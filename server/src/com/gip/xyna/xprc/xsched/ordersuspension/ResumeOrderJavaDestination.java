/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */

package com.gip.xyna.xprc.xsched.ordersuspension;



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_INPUT_PARAMETER_TYPE;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xpce.dispatcher.JavaDestination;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.ResumeResult;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderStartupAndMigrationManagement;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;



public class ResumeOrderJavaDestination extends JavaDestination {

  private static final long serialVersionUID = -2858253968667283277L;

  public static final String RESUME_DESTINATION = "com.gip.xyna.ResumeOrder";

  private static final Logger logger = CentralFactoryLogging.getLogger(ResumeOrderJavaDestination.class);


  public ResumeOrderJavaDestination() {
    super(RESUME_DESTINATION);
  }

  /*
   * reihenfolge von gleichzeitig gehaltenen ressourcen (deadlockvermeidung)
   * - auf OrderMigrationFinished(orderid) warten (kein echtes lock, aber blockierendes warten ist nicht soviel anders)
   * - SRInformationLock
   * - OrderBackup-Lock (HashParallelLock)
   * - DBConnection
   *
   * optimierung von
   * - gleichzeitiges halten von ressourcen möglichst kurz
   * - und möglichst die ressourcen kürzer, die viel contention haben
   * 
   * die notwendigkeit, mehrere datenbank-operationen in der gleichen transaktion durchzuführen, ist hier nicht gegeben.
   * es muss nur schutz vor folgenden problemen gegeben werden:
   * a) falls resume fehlschlägt, muss resume retry machen
   * b) falls resume erfolgreich, muss resumter auftrag sicheren ort erreichen, damit der resumeauftrag ansich verschwinden kann
   * beides muss nicht gekoppelt werden - zuviele resumes sind nicht schädlich 
   * 
   */

  @Override
  public GeneralXynaObject exec(final XynaOrderServerExtension xose, GeneralXynaObject input)
      throws XPRC_INVALID_INPUT_PARAMETER_TYPE, PersistenceLayerException {

    if (!(input instanceof ResumeOrderBean)) {
      throw new XPRC_INVALID_INPUT_PARAMETER_TYPE("1", ResumeOrderBean.class.getName(), input.getClass().getName());
    }

    // although we're not reachable we're not protection worthy and if we try to resume a locked order we'll hang and the Deployment does so with us
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xose.getIdOfLatestDeploymentFromOrder());
    try {
      waitForOrderStartupAndMigrationManagement();

      ResumeOrderBean bean = (ResumeOrderBean) input;
      if (bean.isResumed()) {
        logger.debug("Skipping resume as it has been already performed earlier");
        return bean;
      }

      SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();

      /*
       * Binding des zu resumenden Auftrags ermitteln.
       * Es gibt folgende Fälle: 
       * A) (Root-)Auftrag befindet sich im OrderBackup => Dann findet man leicht das Binding heraus
       * B) Auftrag ist nicht im Backup
       *    B1) Er ist suspendiert mit konfiguriertem SuspensionBackupMode => No Backup
       *       B1a) Auftrag auf diesem Knoten
       *       B1b) Auftrag auf anderem Knoten
       *    B2) Er ist noch nicht suspendiert, weil das suspend parallel am laufen ist (Racecondition)
       *       B2a) Es gibt auf diesem Knoten SRInformation
       *       B2b) Es gibt auf anderem Knoten SRInformation
       * Wenn also das Binding über das OrderBackup nicht ermittelt werden kann, sollte man noch nach einem SRInformation-Eintrag oder einem Memory-Backup schauen.
       * Falls beides nicht gefunden wird, dann auf dem anderen Knoten delegieren.
       * Flag übergeben, so dass auf dem anderen Knoten nicht erneut zurückdelegiert werden kann (Nur für den Fall eines Bugs)
       * Es gibt dabei noch die Möglichkeit einer Racecondition, wenn das Suspend gleichzeitig mit den Checks passiert. 
       * => Deshalb beim Check nach SRInformation und Memory-Backup aufpassen.
       * 
       * => Der Check wird dadurch durchgeführt, dass man einfach lokal das Resume probiert.
       * Wenn es fehlschlägt, weil der Auftrag nicht im Backup gefunden wird (dies inkludiert B1a und B2a), dann auf den anderen Knoten delegieren
       */
      Integer foreignBinding;
      try {
        foreignBinding = getForeignBinding(bean);
      } catch (PersistenceLayerException e) {
        srm.resumeOrderAsynchronouslyDelayed(bean.getTarget(), bean.getRetryCount() + 1, null, bean.mayDelegateToOtherNodeIfOrderIsNotFound());
        bean.setRequestSucceeded(true);
        bean.setResumed(false);
        return bean;
      }
      Pair<ResumeResult, String> result = null;
      if (foreignBinding == null) {
        result = srm.resumeOrder(bean.getTarget());
      } else {
        result = srm.resumeOrderRemote(foreignBinding, bean.getTarget());
      }


      ResumeOrder resumeOrder = new ResumeOrder(bean);
      resumeOrder.checkResultAndRetry(result, srm);

      bean.setRequestSucceeded(resumeOrder.isSucceeded());
      bean.setResumed(resumeOrder.isResumed());

      return bean;
    } finally {
      DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(xose.getIdOfLatestDeploymentFromOrder());
    }
  }


  private Integer getForeignBinding(final ResumeOrderBean bean) throws PersistenceLayerException {
    //TODO fastpath für "nicht im cluster, muss nichts überprüfen". das kann dann gecached werden. achtung: wann cacherefresh?

    //TODO besser Anfrage ans OrderStartupAndMigrationManagement: getBindingOfOrder(Long orderId)
    return WarehouseRetryExecutor.buildCriticalExecutor().connection(ODSConnectionType.DEFAULT).storable(OrderInstanceBackup.class)
        .execute(new WarehouseRetryExecutableNoException<Integer>() {

          @Override
          public Integer executeAndCommit(ODSConnection con) throws PersistenceLayerException {
            lazilyCreatePreparedQuery(con);
            OrderInstanceBackup oib = con.queryOneRow(backupRootOrderIdQuery, new Parameter(bean.getTarget().getRootId()));
            if (oib == null) {
              oib = con.queryOneRow(backupRootOrderIdQuery, new Parameter(bean.getTarget().getOrderId()));
            }
            if (oib == null) {
              return null;
            } else {
              int ownBinding = oib.getLocalBinding(ODSConnectionType.DEFAULT);
              if (oib.getBinding() == ownBinding) {
                return null;
              } else {
                try {
                  boolean migrated = OrderStartupAndMigrationManagement.getInstance().waitUntilRootOrderIsAccessible(oib.getRootId());
                  if (migrated) {
                    //auf eigenes Binding migriert
                    return null;
                  } else {
                    //es bleibt beim fremden Binding
                    return oib.getBinding();
                  }
                } catch (Exception e) {
                  //LoadingAbortedWithErrorException, MigrationAbortedWithErrorException, InterruptedException
                  logger.warn("Waiting for migration failed, aborting resume.", e);
                  throw new OrderDeathException(e); //resumeorder bleibt im orderbackup und wird beim nächsten serverstart erneut ausgeführt
                }
              }
            }
          }
        });

  }


  private static final Object backupRootOrderIdQueryLock = new Object();
  private static volatile PreparedQuery<OrderInstanceBackup> backupRootOrderIdQuery = null;


  private void lazilyCreatePreparedQuery(ODSConnection con) throws PersistenceLayerException {
    if (backupRootOrderIdQuery == null) {
      synchronized (backupRootOrderIdQueryLock) {
        if (backupRootOrderIdQuery == null) {
          StringBuilder sb = new StringBuilder();
          sb.append("select ").append(OrderInstanceBackup.COL_ROOT_ID).append(",").append(OrderInstanceBackup.COL_ID).append(",")
              .append(OrderInstanceBackup.COL_BINDING).append(" from ").append(OrderInstanceBackup.TABLE_NAME).append(" where ")
              .append(OrderInstanceBackup.COL_ID).append("=?");
          Query<OrderInstanceBackup> query = new Query<OrderInstanceBackup>(sb.toString(), OrderInstanceBackup.getSelectiveReader());
          backupRootOrderIdQuery = con.prepareQuery(query, true);
        }
      }
    }
  }


  /**
   * Falls eine Migration gerade läuft, wird geprüft, ob diese schon eine DB-Connection hat. Zur Vermeidung von Deadlocks
   * darf ResumeOrderJavaDestination erst eine Connection hollen, wenn der Migrationsthread eine hat
   */
  private void waitForOrderStartupAndMigrationManagement() {
    boolean waitAgain = true;
    do {
      try {
        waitAgain = OrderStartupAndMigrationManagement.getInstance().needMigrationThreadOrLoadingThreadConnections();
      } catch (RuntimeException re) {
        if (re.getMessage().contains("not correctly configured")) {
          //OrderStartupAndMigrationManagement ist noch nicht initialisiert
          waitAgain = true;
        } else {
          throw re;
        }
      }
      if (waitAgain) {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          // die ignorieren wir mal ... ansonsten würde der Resumeauftrag verloren gehen und der zugehörige Auftrag wär für immer verloren        
        }
      }
    } while (waitAgain);
  }


  private static class ResumeOrder {

    private ResumeOrderBean bean;
    private boolean resumed = false;
    private boolean succeeded = false;


    public ResumeOrder(ResumeOrderBean bean) {
      this.bean = bean;
    }


    public boolean isSucceeded() {
      return succeeded;
    }


    public boolean isResumed() {
      return resumed;
    }


    public void checkResultAndRetry(Pair<ResumeResult, String> result, SuspendResumeManagement srm) throws PersistenceLayerException {
      int retryCnt = checkResult(result);
      switch (retryCnt) {
        case NO_RETRY :
          return;
        case RETRY_OTHER_NODE :
          OrderInstanceBackup oib = new OrderInstanceBackup();
          ClusterProvider clusterInstance =  oib.getClusterInstance(ODSConnectionType.DEFAULT);
          int localBinding = clusterInstance.getLocalBinding();
          for (int binding : clusterInstance.getAllBindingsIncludingLocal()) {
            if (binding != localBinding) {
              srm.resumeOrderRemote(binding, bean.getTarget());
              //result kann hier nicht weiter verarbeitet werden. anderer knoten macht ggf retries
              break;
            }
          }
          break;
        default :
          retryResume(retryCnt, result);
      }
    }
    
    private static final int NO_RETRY = -1;
    private static final int RETRY_OTHER_NODE = -2;
    private static final XynaPropertyBoolean killOrderWithoutOrderBackup =
        new XynaPropertyBoolean("xprc.xsched.ordersuspension.resume.nobackup.kill", true)
            .setDefaultDocumentation(DocumentationLanguage.EN,
                                     "If this is set to true: When a Xyna Order can not be resumed because its orderbackup entry is missing, the order will automatically be aborted.");


    private int checkResult(Pair<ResumeResult, String> result) {
      int retryCount = bean.getRetryCount() + 1;
      switch (result.getFirst()) {
        case Resumed :
          resumed = true;
          succeeded = true;
          return NO_RETRY;
        case Failed :
          if (SuspendResumeManagement.FAILED_ORDERBACKUP_NOT_FOUND.equals(result.getSecond())) {
            if (bean.mayDelegateToOtherNodeIfOrderIsNotFound() && new OrderInstanceBackup().isClustered(ODSConnectionType.DEFAULT)) {
              //vermutlich racecondition: auf anderem knoten ist das orderbackup beim suspend noch nicht passiert
              if (logger.isDebugEnabled()) {
                logger.debug("Order " + bean.getTarget() + " not found in orderbackup. Delegating Resume to other node.");
              }
              return RETRY_OTHER_NODE;
            } else {
              //unerwarteter fall. hier kann man den auftrag killen/aufräumen, er kann ja nicht wieder resumed werden. 
              if (killOrderWithoutOrderBackup.get()) {
                KillStuckProcessBean kspb = new KillStuckProcessBean(bean.getTarget().getRootId(), true, AbortionCause.NOBACKUP);
                try {
                  kspb = XynaFactory.getInstance().getProcessing().killStuckProcess(kspb);
                  logger.warn("Unresumable order " + bean.getTarget() + " has been killed: " + kspb.getResultMessage());
                } catch (XynaException e) {
                  logger.warn("Order " + bean.getTarget() + " could not be resumed and not be killed.", e);
                }
              } else {
                logger.warn("Could not resume " + bean.getTarget() + ": " + result.getSecond() + ". RetryCount=" + retryCount);
              }
              return NO_RETRY;
            }
          } //else: bei anderen fehlern retry 
          break;
        case Unresumeable :
          if (SuspendResumeManagement.UNRESUMABLE_LOCKED.equals(result.getSecond())) {
            retryCount = 0; //permanente Retries, bis Lock aufgehoben wird
          }
          break;
        default :
          logger.error("Unexpected ResumeResult " + result.getFirst() + " " + result.getSecond());
          resumed = false;
          return NO_RETRY;
      }
      if (logger.isInfoEnabled()) {
        logger.info("Could not resume " + bean.getTarget() + ": " + result.getSecond() + ". RetryCount=" + retryCount);
      }
      return retryCount;
    }


    /**
     * Resume konnte nicht ausgeführt werden, daher mit etwas Verzögerung nochmal probieren, 
     * indem hier nun ein neuer ResumeOrder-Auftrag angelegt wird. 
     * @throws PersistenceLayerException 
     */
    private void retryResume(int retryCount, Pair<ResumeResult, String> result) throws PersistenceLayerException {
      if (retryCount > 5) {
        logger.warn("Giving up retries to resume " + bean.getTarget() + " after 5 retries, last result: " + result);
        throw new RuntimeException("Giving up retries to resume " + bean.getTarget() + " after 5 retries, last result: " + result);
      }

      SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
      srm.resumeOrderAsynchronouslyDelayed(bean.getTarget(), retryCount, null, bean.mayDelegateToOtherNodeIfOrderIsNotFound());
      succeeded = true;
    }


  }


}
