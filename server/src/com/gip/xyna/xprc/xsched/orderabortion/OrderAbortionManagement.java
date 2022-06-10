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


package com.gip.xyna.xprc.xsched.orderabortion;



import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.concurrent.HashParallelReentrantLock;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnable;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIImplFactory;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_ABORTION_FAILED;
import com.gip.xyna.xprc.xfractwfe.base.EngineSpecificProcess;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess.XynaProcessState;
import com.gip.xyna.xprc.xpce.WorkflowEngine;
import com.gip.xyna.xprc.xpce.execution.ExecutionDispatcher;
import com.gip.xyna.xprc.xpce.execution.MasterWorkflowPostScheduler;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.AbortionOfSuspendedOrderResult;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;



public class OrderAbortionManagement extends FunctionGroup
    implements
      Clustered,
      ClusterStateChangeHandler,
      RMIImplFactory<OrderAbortionRemoteInterfaceImpl> {

  public static final String DEFAULT_NAME = "OrderAbortionManagement";
  public static final Logger logger = CentralFactoryLogging.getLogger(OrderAbortionManagement.class);


  public int FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID = XynaFactory.getInstance().getFutureExecution().nextId();

  private volatile ClusterState rmiClusterState = ClusterState.NO_CLUSTER;
  private volatile boolean isReadyForChange = true;

  private boolean rmiIsClustered = false;
  private long rmiClusterInstanceId = 0;
  private RMIClusterProvider rmiClusterInstance = null;
  private long clusteredOrderAbortionManagementInterfaceID = 0;
  private ClusterState currentState;

  private static PreparedQueryCache cache = new PreparedQueryCache();


  private ClusterStateChangeHandler clusterStateChangeHandler = new ClusterStateChangeHandler() {

    public boolean isReadyForChange(ClusterState newState) {
      return true; //immer bereit
    }


    public void onChange(ClusterState newState) {
      rmiClusterState = newState;
    }

  };


  public OrderAbortionManagement() throws XynaException {
    super();
  }


  public boolean processAbortion(final KillStuckProcessBean bean) throws XPRC_PROCESS_ABORTION_FAILED {
    boolean success = processAbortionLocally(bean, false);

    // we were not successful locally, so we try the abortion on the remote machine
    if (!success && rmiIsClustered && (rmiClusterState == ClusterState.CONNECTED)) {
      List<Boolean> remoteNodeshandled = null;

      try {
        remoteNodeshandled =
            RMIClusterProviderTools
                .executeAndCumulate(rmiClusterInstance,
                                    clusteredOrderAbortionManagementInterfaceID,
                                    new RMIRunnable<Boolean, ClusteredOrderAbortionManagementInterface, XynaException>() {

                                      public Boolean execute(ClusteredOrderAbortionManagementInterface clusteredInterface)
                                          throws RemoteException, XynaException {
                                        return clusteredInterface.processAbortion(bean);
                                      }
                                    }, null);
      } catch (InvalidIDException e) {
        throw new RuntimeException(e); // sollte nicht passieren, weil kein removeRmi aufgerufen wird
      } catch (XynaException e) {
        throw new XPRC_PROCESS_ABORTION_FAILED(bean.getOrderIdToBeKilled(), "Error while aborting order remotely", e);
      }

      for (Boolean handled : remoteNodeshandled) {
        if (handled == true) {
          success = true;
          break;
        }
      }
    }

    if (!success) {
      long rootOrderId = findRootOrderId(bean);
      try {
        cleanupOrderRelicsForFamily(rootOrderId, bean);
      } catch (PersistenceLayerException e) {
        logger.warn("Could not access persistencelayer during cleanup of order family " + rootOrderId, e);
        return false;
      }
      throw new XPRC_PROCESS_ABORTION_FAILED(bean.getOrderIdToBeKilled(), "Order not found but cleanup was successful.");
    }

    return success;
  }


  /**
   * @param xo ist nicht null bei rekursionsaufruf, ansonsten null
   * @return true if order was found
   */
  public boolean abortMasterWorkflow(XynaOrderServerExtension xo, long orderId, KillStuckProcessBean bean,
                                     boolean killJavaCalls) {
    int retryCnt = 0;
    AtomicBoolean abortedPlanningWF = new AtomicBoolean(false);
    AtomicBoolean abortedExecutionWF = new AtomicBoolean(false);
    AtomicBoolean abortedCleanupWF = new AtomicBoolean(false);
    while (retryCnt++ < 4) {
      if (logger.isInfoEnabled()) {
        logger.info("aborting " + orderId + ".");
        if (logger.isDebugEnabled()) {
          logger.debug(" killJavaCalls=" + killJavaCalls + ", " + bean);
        }
      }
      //1. abort Planning WF
      WorkflowEngine wfe = XynaFactory.getInstance().getProcessing().getWorkflowEngine();
      EngineSpecificProcess processInstance = wfe.getPlanningProcessor().getRunningProcessById(orderId);
      if (processInstance != null) {
        /*  if (resultMessage != null) {
            resultMessage.append("  * Found process instance in execution phase\n");
          }*/
        XynaProcess xp = (XynaProcess) processInstance;
        switch (xp.abortRunningWF(killJavaCalls, bean, abortedPlanningWF)) {
          case SUCCESS :
            return true;
          case UNSUCCESSFUL :
            //retry
            continue;
          default :
            throw new RuntimeException();
        }
      }

      //2. abort in scheduler
      boolean successfullyCancelled =
          XynaFactory.getInstance().getProcessing().getXynaScheduler()
              .abortOrder(orderId, 0, bean.isIgnoreResourcesWhenResuming());
      if (successfullyCancelled) {
        bean.getResultMessageStringBuilder().append("  * Successfully canceled order ").append(orderId)
            .append(" that was waiting to be scheduled\n");
        return true;
      }

      //3. abort Execution Workflow
      processInstance = wfe.getExecutionProcessor().getRunningProcessById(orderId);
      if (processInstance != null) {
        /*  if (resultMessage != null) {
            resultMessage.append("  * Found process instance in execution phase\n");
          }*/
        XynaProcess xp = (XynaProcess) processInstance;
        switch (xp.abortRunningWF(killJavaCalls, bean, abortedExecutionWF)) {
          case SUCCESS :
            return true;
          case UNSUCCESSFUL :
            //retry
            continue;
          default :
            throw new RuntimeException();
        }
      }

      //4. abort Service Destination
      ExecutionDispatcher execDispatcher =
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
              .getExecutionEngineDispatcher();
      // Suche nach Aufträge die über callService aufgerufen wurden.
      if (execDispatcher.containsServiceExecutionsFromOrder(orderId)) {
        if (logger.isDebugEnabled()) {
          logger.debug("aborting order " + orderId + " as service destination");
        }
        //FIXME auf timeout warten
        execDispatcher.terminateThreadOfRunningServiceExecution(orderId, bean.forceKill(), bean.getTerminationReason());
        return true;
      }

      //5. abort cleanup workflow
      processInstance = wfe.getCleanupProcessor().getRunningProcessById(orderId);
      if (processInstance != null) {
        /*  if (resultMessage != null) {
            resultMessage.append("  * Found process instance in execution phase\n");
          }*/
        XynaProcess xp = (XynaProcess) processInstance;
        switch (xp.abortRunningWF(killJavaCalls, bean, abortedCleanupWF)) {
          case SUCCESS :
            return true;
          case UNSUCCESSFUL :
            //retry
            continue;
          default :
            throw new RuntimeException();
        }
      }

      //6a. wir haben das auftragsobjekt
      //könnte jetzt bereits suspended sein.
      if (xo != null) {
        //nur bei rekursion der fall
        if (xo.getExecutionProcessInstance() != null) {
          if (xo.getExecutionProcessInstance().getState() != XynaProcessState.FINISHED) {
            switch (xo.getExecutionProcessInstance().abortRunningWF(killJavaCalls, bean, abortedExecutionWF)) {
              case SUCCESS :
                return true;
              case UNSUCCESSFUL :
                //retry
                continue;
              default :
                throw new RuntimeException();
            }
          } else {
            //racecondition sollte hier eigtl keine auftreten, weil beim parent-wf bereits "ABORTING" gesetzt ist
            return true;
          }
        } else {
          // -> 7. retry
        }
      }

      //6b. root auftrag suspended?
      AbortionOfSuspendedOrderResult suspensionResult = abortSuspendedRootOrder(orderId, bean.isIgnoreResourcesWhenResuming());
      switch (suspensionResult) {
        case SUCCESS :
          return true;
        case RESUME_FAILED_WRONG_BINDING :
          return false;
        default :
          //else: ntbd - retry
      }

      //7. order irgendwo dazwischen? -> kurz warten, dann nochmal probieren
      if (logger.isDebugEnabled()) {
        logger.debug("did not find order " + orderId + ". trying again (#" + retryCnt +")...");
      }
      Thread.yield();
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
      }
    }

    //8. order nicht abbrechen können -> aufräumen, aber erst beim anderen clusterknoten schauen falls vorhanden.
    if (xo == null) {
      return false;
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("could not find order " + xo + " running. starting cleanup process ...");
      }
      new MasterWorkflowPostScheduler(xo).cleanupBeforeOrderHasBeenScheduled(true);
      return true;
    }
  }
  
  private PreparedQuery<OrderInstanceDetails> queryFamily;


  public boolean cleanupOrderRelicsForFamily(long rootOrderId, KillStuckProcessBean bean)
      throws PersistenceLayerException {
    //TODO für bessere meldung in orderarchive history schauen: evtl war der auftrag schon archiviert?
    if (logger.isDebugEnabled()) {
      logger.debug("order " + rootOrderId + " not found in factory. trying to cleanup corresponding data.");
    }
    boolean foundOrder = false;
    Set<Long> ordersInFamily = new HashSet<Long>();
    ordersInFamily.add(rootOrderId);
    ordersInFamily.add(bean.getOrderIdToBeKilled());
    ODSConnection con = ODSImpl.getInstance().openConnection();
    try {
      //checken, ob auftrag jung ist und orderarchive default aufräumen
      if (queryFamily == null) {
        synchronized (this) { //doublecheckedpattern so (ohne volatile variable) ok, weil query mehrfach prepared werden darf
          if (queryFamily == null) {
            queryFamily =
                con.prepareQuery(new Query<OrderInstanceDetails>("select * from " + OrderInstanceDetails.TABLE_NAME + " where "
                    + OrderInstanceDetails.COL_ROOT_ID + " = ?", new OrderInstanceDetails().getReader()));
          }
        }
      }
      List<OrderInstanceDetails> family = con.query(queryFamily, new Parameter(rootOrderId), Integer.MAX_VALUE);
      if (family.size() > 0) {
        Collections.sort(family, new Comparator<OrderInstanceDetails>() {

          public int compare(OrderInstanceDetails o1, OrderInstanceDetails o2) {
            return new Long(o1.getLastUpdate()).compareTo(o2.getLastUpdate());
          }

        });
        if (System.currentTimeMillis() - 5 * 60 * 1000 < family.get(family.size() - 1).getLastUpdate()) {
          bean.getResultMessageStringBuilder().append("  * XynaOrder ").append(family.get(family.size() - 1).getId())
              .append(" belonging to the order family of ").append(rootOrderId)
              .append(" could not be found but has been updated in the last 5 minutes. Try again in 5 minutes.\n");
          //auftrag zu jung und nicht gefunden
          return false;
        } else {
          foundOrder = true;
          for (OrderInstanceDetails oid : family) {
            ordersInFamily.add(oid.getId());
          }
          if (logger.isDebugEnabled()) {
            logger.debug("cleaning orderarchive DEFAULT for family: " + ordersInFamily);
          }
          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
              .cleanup(con, family, bean.getTerminationReason().getAbortionCauseString());
          bean.getResultMessageStringBuilder().append("  * Deleted ").append(family.size())
              .append(" orders from orderarchive default belonging to the order family of ").append(rootOrderId)
              .append(".\n");
          con.commit();
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("order not found in orderarchive DEFAULT");
        }
      }

      //suspension causes aufräumen
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().
        cleanupOrderFamily(rootOrderId, ordersInFamily, con);
      con.commit();
      
      //orderbackup aufräumen
      PreparedQuery<OrderInstanceBackup> queryBackups =
          con.prepareQuery(new Query<OrderInstanceBackup>("select "+OrderInstanceBackup.COL_ID+" from " + OrderInstanceBackup.TABLE_NAME + " where "
              + OrderInstanceBackup.COL_ROOT_ID + " = ?", OrderInstanceBackup.getSelectiveReader()));
      List<OrderInstanceBackup> backups = con.query(queryBackups, new Parameter(rootOrderId), Integer.MAX_VALUE);
      if (backups.size() > 0) {
        foundOrder = true;
        for (OrderInstanceBackup oib : backups) {
          ordersInFamily.add(oib.getId());
        }
        con.delete(backups);
        con.commit();
        if (logger.isDebugEnabled()) {
          logger.debug("deleted + " + backups.size() + " order backups.");
        }
      }
    } finally {
      con.closeConnection();
    }

    for (Long orderInFamily : ordersInFamily) {
      XynaFactory.getInstance().getProcessing().getXynaScheduler().getOrderSeriesManagement().abortOrder(orderInFamily);
      boolean freedCaps = freeCapacities(bean, orderInFamily);
      if (freedCaps) {
        if (logger.isDebugEnabled()) {
          logger.debug("freed capacities and vetos for order " + orderInFamily);
        }
        foundOrder = true;
        bean.setHasFreedCapacities();
      } else {
        if (XynaFactory.getInstance().getProcessing().getXynaScheduler()
            .abortOrder(orderInFamily, 15 * 60 * 1000, bean.isIgnoreResourcesWhenResuming())) {
          //FIXME konfigurierbar
          foundOrder = true;
          bean.getResultMessageStringBuilder().append("  * Canceled XynaOrder in scheduler.\n");
        } else if (!foundOrder) {
          bean.getResultMessageStringBuilder().append("  * Could not find order ").append(orderInFamily).append(".\n");
        } else {
          bean.getResultMessageStringBuilder().append("  * Could not find XynaOrder for order id ")
              .append(orderInFamily)
              .append(" within the workflow engine or the scheduler but deleted some entries to its root order ")
              .append(rootOrderId).append(".\n");
        }
      }
    }

    return foundOrder;
  }


  private PreparedQuery<Pair<Long, Integer>> queryRootOrderIdInOrderBackup;
  private PreparedQuery<String> queryBackupCause;


  public AbortionOfSuspendedOrderResult abortSuspendedWorkflow(XynaOrderServerExtension xo,
                                        boolean ignoreResourcesWhenResuming, boolean forceResume)
      throws PersistenceLayerException {
    return abortSuspendedWorkflowInternal(xo.getRootOrder().getId(), xo, ignoreResourcesWhenResuming, forceResume);
  }

  private AbortionOfSuspendedOrderResult abortSuspendedWorkflowInternal(Long rootOrderId, XynaOrderServerExtension xo,
                                                 boolean ignoreResourcesWhenResuming, boolean forceResume)
      throws PersistenceLayerException {
    return XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().
        abortSuspendedWorkflow(xo, rootOrderId, ignoreResourcesWhenResuming, forceResume);
  }


  private PreparedQuery<String> getQueryBackupCause(ODSConnection con) throws PersistenceLayerException {
    if (queryBackupCause == null) {
      queryBackupCause =
          con.prepareQuery(new Query<String>("select " + OrderInstanceBackup.COL_BACKUP_CAUSE + " from "
              + OrderInstanceBackup.TABLE_NAME + " where " + OrderInstanceBackup.COL_ID + " = ? and " + OrderInstanceBackup.COL_XYNAORDER + " is not null",
                                             new ResultSetReader<String>() {

                                               public String read(ResultSet rs) throws SQLException {
                                                 return rs.getString(OrderInstanceBackup.COL_BACKUP_CAUSE);
                                               }

                                             }));
    }
    return queryBackupCause;
  }

  private int localBinding = -2;

  private AbortionOfSuspendedOrderResult abortSuspendedRootOrder(long orderId, boolean ignoreResourcesWhenResuming) {
    //fast check: ist auftrag suspendiert, aber SuspensionBackupMode=NO_BACKUP. findet man den auftrag nicht im backup und kann deshalb weder
    //            binding noch rootid herausfinden 
    Long rootId = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().getRootIdIfSuspendedInMemory(orderId);
    AbortionOfSuspendedOrderResult suspended = AbortionOfSuspendedOrderResult.RESUME_FAILED;
    try {
      if (rootId != null) {
        return abortSuspendedWorkflowInternal(rootId, null, ignoreResourcesWhenResuming, false);
      }

      //check über backup
      ODSConnection con = ODSImpl.getInstance().openConnection();
      try {
        if (queryRootOrderIdInOrderBackup == null) {
          queryRootOrderIdInOrderBackup = con.prepareQuery(new Query<Pair<Long, Integer>>("select " + OrderInstanceBackup.COL_ROOT_ID + ", "
              + OrderInstanceBackup.COL_BINDING + " from " + OrderInstanceBackup.TABLE_NAME + " where " + OrderInstanceBackup.COL_ID
              + " = ?", new ResultSetReader<Pair<Long, Integer>>() {

                public Pair<Long, Integer> read(ResultSet rs) throws SQLException {
                  return new Pair<>(rs.getLong(OrderInstanceBackup.COL_ROOT_ID), rs.getInt(OrderInstanceBackup.COL_BINDING));
                }
              }));
          if (localBinding == -2) {
            localBinding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);
          }
        }
        Pair<Long, Integer> result = con.queryOneRow(queryRootOrderIdInOrderBackup, new Parameter(orderId));
        if (result != null && result.getSecond() == localBinding) {
          rootId = result.getFirst();
          String backupCause = con.queryOneRow(getQueryBackupCause(con), new Parameter(rootId));
          if (backupCause != null && backupCause.equals(BackupCause.SUSPENSION.name())) {
            //abortSuspendedWorkflow holt intern neue Connection. Übergeben der Connection ist wegen 
            //"Lock-reihenfolge" nicht gut, vgl Kommentar in ResumeOrderJavaDestination
            con.closeConnection();
            suspended = abortSuspendedWorkflowInternal(rootId, null, ignoreResourcesWhenResuming, false);
          }
        } else if (result == null) {
          if (logger.isDebugEnabled()) {
            logger.debug("order " + orderId + " is not suspended.");
          }
          suspended = AbortionOfSuspendedOrderResult.RESUME_FAILED;
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("order " + orderId + " belongs to other binding.");
          }
          suspended = AbortionOfSuspendedOrderResult.RESUME_FAILED_WRONG_BINDING;
        }
      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      logger.warn("Could not query order backup", e);
    }

    return suspended;
  }


  private final HashParallelReentrantLock<Long> abortionLock = new HashParallelReentrantLock<Long>(128);


  /**
   * @return true, falls der auftrag gefunden wurde
   */
  public boolean processAbortionLocally(KillStuckProcessBean bean, boolean cleanupIfNotFound) {
    long rootOrderId = findRootOrderId(bean);
    if (logger.isDebugEnabled()) {
      logger.debug("found root order id " + rootOrderId + " for order " + bean.getOrderIdToBeKilled());
    }
    abortionLock.lock(rootOrderId); //locken, damit nicht zwei kills auf den gleichen auftrag gleichzeitig laufen
    try {
      boolean success = abortMasterWorkflow(null, rootOrderId, bean, true);
      if (!success && cleanupIfNotFound) {
        try {
          cleanupOrderRelicsForFamily(rootOrderId, bean);
        } catch (PersistenceLayerException e) {
          logger.warn("Could not access persistencelayer during cleanup of order family " + rootOrderId, e);
        }
      }
      return success;
    } finally {
      abortionLock.unlock(rootOrderId);
    }
  }


  private long findRootOrderId(KillStuckProcessBean bean) {
    long orderIdToBeKilled = bean.getOrderIdToBeKilled();
    try {
      ODSConnection con = ODSImpl.getInstance().openConnection();
      try {
        //1. orderarchive default

        OrderInstanceDetails oid = new OrderInstanceDetails(orderIdToBeKilled);
        try {
          con.queryOneRow(oid);
          return oid.getRootId();
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          //ok
        }

        //2. orderbackup
        return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
              .readRootOrderIdFromOrderBackup(con, orderIdToBeKilled);
        
      } finally {
        con.closeConnection();
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      //ok
    } catch (PersistenceLayerException e) {
      logger.warn("Could not query suspensionentry " + orderIdToBeKilled + ".", e);
    }

    //4. planning
    WorkflowEngine wfe = XynaFactory.getInstance().getProcessing().getWorkflowEngine();
    EngineSpecificProcess processInstance = wfe.getPlanningProcessor().getRunningProcessById(orderIdToBeKilled);
    if (processInstance != null) {
      return processInstance.getCorrelatedXynaOrder().getRootOrder().getId();
    }

    //5. scheduler
    SchedulingOrder so =
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList()
            .getSchedulingOrder(orderIdToBeKilled);
    if (so != null) {
      XynaOrderServerExtension xo =
          XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList().getXynaOrder(so);
      if (xo != null) {
        return xo.getRootOrder().getId();
      }
    }

    //6. execution
    processInstance = wfe.getExecutionProcessor().getRunningProcessById(orderIdToBeKilled);
    if (processInstance != null) {
      return processInstance.getCorrelatedXynaOrder().getRootOrder().getId();
    }

    //7. cleanup
    processInstance = wfe.getCleanupProcessor().getRunningProcessById(orderIdToBeKilled);
    if (processInstance != null) {
      return processInstance.getCorrelatedXynaOrder().getRootOrder().getId();
    }

    bean.getResultMessageStringBuilder().append("  * Could not find order ").append(orderIdToBeKilled)
        .append(" anywhere. Continuing with the assumption that it is a root order.\n");
    return orderIdToBeKilled;
  }


  private boolean freeCapacities(KillStuckProcessBean bean, long orderId) {
    boolean freedCaps;
    boolean freedVetos;
    XynaScheduler xSched = XynaFactory.getInstance().getProcessing().getXynaScheduler();

    freedCaps = xSched.getCapacityManagement().forceFreeCapacities(orderId);
    freedVetos = xSched.getVetoManagement().forceFreeVetos(orderId);

    if (freedVetos) {
      bean.getResultMessageStringBuilder().append("  * Successfully freed vetos for order id " + orderId + "\n");
    } else {
      bean.getResultMessageStringBuilder().append("  * Could not free vetos of order " + orderId + " or order not found.\n");
    }
    if (freedCaps) {
      bean.getResultMessageStringBuilder().append("  * Successfully freed capacities for order id " + orderId + "\n");
    } else {
      bean.getResultMessageStringBuilder().append("  * Could not free capacities of order " + orderId + " or order not found.\n");
    }

    xSched.notifyScheduler();
    return freedCaps;
  }


  public boolean isClustered() {
    return rmiIsClustered;
  }


  public long getClusterInstanceId() {
    if (!rmiIsClustered) {
      throw new IllegalStateException("Component is not clustered.");
    }

    return rmiClusterInstanceId;
  }


  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {
    this.rmiClusterInstanceId = clusterInstanceId;
    XynaClusteringServicesManagementInterface clusterMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    rmiClusterInstance = (RMIClusterProvider) clusterMgmt.getClusterInstance(clusterInstanceId);

    if (rmiClusterInstance == null) {
      throw new IllegalArgumentException("Did not find Clusterinstance with id " + clusterInstanceId);
    }

    try {
      clusteredOrderAbortionManagementInterfaceID =
          rmiClusterInstance.addRMIInterfaceWithClassReloading("RemoteOrderAbortionManagment", this);
    } catch (XMCP_RMI_BINDING_ERROR e) {
      throw new XFMG_ClusterComponentConfigurationException(getName(), clusterInstanceId, e);
    }

    rmiIsClustered = true;

    clusterMgmt.addClusterStateChangeHandler(clusterInstanceId, clusterStateChangeHandler);

    rmiClusterState = rmiClusterInstance.getState();
  }


  public void disableClustering() {
    rmiIsClustered = false;
    rmiClusterState = ClusterState.NO_CLUSTER;
    rmiClusterInstance = null;
    clusteredOrderAbortionManagementInterfaceID = 0;
    rmiClusterInstanceId = 0;
    XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement()
        .removeClusterStateChangeHandler(rmiClusterInstanceId, clusterStateChangeHandler);
  }


  public String getName() {
    return getDefaultName();
  }


  public boolean isReadyForChange(ClusterState newState) {
    return isReadyForChange;
    // TODO : zwischen kill aufrufen auf false setzen, damit nicht alle auf einmal laufen.
  }


  public void onChange(final ClusterState newState) {
    if (logger.isDebugEnabled()) {
      logger.debug("Got notified of state transition " + currentState + " -> " + newState);
    }
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask(OrderAbortionManagement.class, OrderAbortionManagement.class.getSimpleName()).
       before(XynaClusteringServicesManagement.class).
       execAsync(this::initCluster);
  }


  private void initCluster() {
    try {
      XynaClusteringServicesManagement.getInstance().registerClusterableComponent(OrderAbortionManagement.this);
    } catch (XFMG_ClusterComponentConfigurationException e) {
      throw new RuntimeException("Failed to register " + OrderAbortionManagement.class.getSimpleName() + " as clusterable component.", e);
    }
  }


  @Override
  protected void shutdown() throws XynaException {
  }


  public void init(InitializableRemoteInterface rmiImpl) {
    rmiImpl.init(this);
  }


  public String getFQClassName() {
    return OrderAbortionRemoteInterfaceImpl.class.getName();
  }


  public void shutdown(InitializableRemoteInterface rmiImpl) {
  }

}
