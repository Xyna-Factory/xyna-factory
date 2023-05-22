/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xpce.ordersuspension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.WarehouseRetryExecutorBuilder;
import com.gip.xyna.xprc.Redirection;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.xfractwfe.base.ParallelExecutionStep;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringDispatcher;
import com.gip.xyna.xprc.xpce.ordersuspension.RootOrderSuspension.ManualInteractionData;
import com.gip.xyna.xprc.xpce.ordersuspension.RootOrderSuspension.State;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.DoResume;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.ResumeResult;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.Step;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceSuspensionStatus;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderStartupAndMigrationManagement;
import com.gip.xyna.xprc.xsched.AllOrdersList;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;


/**
 *
 */
public class SuspendResumeAdapterImpl implements SuspendResumeAdapter<ODSConnection,XynaOrderServerExtension> {

  private static Logger logger = CentralFactoryLogging.getLogger(SuspendResumeAdapterImpl.class);
  private SuspendResumeAlgorithm<ODSConnection, XynaOrderServerExtension> suspendResumeAlgorithm;
  
  public SuspendResumeAdapterImpl() {
  }
  
  public void setSuspendResumeAlgorithm(SuspendResumeAlgorithm<ODSConnection,XynaOrderServerExtension> suspendResumeAlgorithm) {
    this.suspendResumeAlgorithm = suspendResumeAlgorithm;
  }

  public void startOrder(RootSRInformation<XynaOrderServerExtension> rootSRInformation, ODSConnection con) throws PersistenceLayerException {
    XynaOrderServerExtension order = SRHelper.getRootOrder(rootSRInformation);
    SRHelper.updateMonitoring(order);
    SRHelper.setSuspendResumeStatus(order, false, con, rootSRInformation.getSuspensionCause());
    
    SuspensionBackupMode backupMode = rootSRInformation.getSuspensionCause() == null ?
                    SuspensionBackupMode.BACKUP : rootSRInformation.getSuspensionCause().getOrderBackupMode();
    
    if( con != null && backupMode.doBackup()) {
        con.executeAfterCommit( new OrderReferenceConversionToWeak(rootSRInformation), Thread.MIN_PRIORITY);
    }
    
    try {
      XynaFactory.getInstance().getProcessing().getXynaScheduler().addOrder(order, con, false);
    } catch (XPRC_OrderEntryCouldNotBeAcknowledgedException e) {
      throw new RuntimeException("A resuming order should never be acknowledged", e);
    }
    if( con == null && backupMode.doBackup()) {
      rootSRInformation.convertOrderReferenceToWeak();
    }
  }
  
  
  private static class OrderReferenceConversionToWeak implements Runnable {

    private RootSRInformation<XynaOrderServerExtension> rootSRInformation;

    public OrderReferenceConversionToWeak(RootSRInformation<XynaOrderServerExtension> rootSRInformation) {
      this.rootSRInformation = rootSRInformation;
    }

    public void run() {
      rootSRInformation.convertOrderReferenceToWeak();
    }
    
  }
  

  public void rescheduleOrder(XynaOrderServerExtension order) {
    if (logger.isDebugEnabled()) {
      logger.debug("Immediately re-adding order <" + order.getId()
                   + "> during suspension because it has already been resumed");
    }
    try {
      XynaFactory.getInstance().getProcessing().getXynaScheduler().addOrder(order, null, false);
    } catch (XPRC_OrderEntryCouldNotBeAcknowledgedException e) {
      throw new RuntimeException("A suspended order should never be acknowledged", e);
    }
  }

  
  public void suspendOrder(XynaOrderServerExtension order, RootOrderSuspension rootOrderSuspension, 
                           SuspensionCause suspensionCause, boolean backup ) throws PersistenceLayerException {
   
    SuspendOrder so = new SuspendOrder(order,rootOrderSuspension,suspensionCause, backup);
    WarehouseRetryExecutor.buildMinorExecutor().
        storable(OrderInstanceBackup.class).storable(OrderInstance.class).
        execute(so);
  }
  
  private static class SuspendOrder implements WarehouseRetryExecutableNoResult {
    
    private XynaOrderServerExtension order;
    private RootOrderSuspension rootOrderSuspension;
    private SuspensionCause suspensionCause;
    private boolean backup;
 
    public SuspendOrder(XynaOrderServerExtension order, RootOrderSuspension rootOrderSuspension,
        SuspensionCause suspensionCause, boolean backup) {
      this.order = order;
      this.rootOrderSuspension = rootOrderSuspension;
      this.suspensionCause = suspensionCause;
      this.backup = backup;
    }

    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      suspendOrder(con);
      con.commit();
    }

    private void suspendOrder( ODSConnection con ) throws PersistenceLayerException {
      SRHelper.setSuspendResumeStatus(order, true, con, suspensionCause);
      
      boolean isRootOrder = ! order.hasParentOrder();
      
      if( isRootOrder && rootOrderSuspension != null && rootOrderSuspension.isMINecessary() ) {
        SRHelper.injectMI(order, rootOrderSuspension, con);
      }
      
      if( isRootOrder ) {
        if( backup) { 
          //Auftrag ist Hauptauftrag: Er und seine Kinder m�ssen nun gebackupt werden
          SRHelper.backupXynaOrder(con, order);
        }
      } else {
        //Auftrag hat einen Parent, d.h. es muss kein Backup erstellt werden, 
        //da die Order durch den Parent weiterhin aktiv im Speicher gehalten wird
      }
    }

  }


  @Override
  public void abortOrderSuspension(RootSRInformation<XynaOrderServerExtension> rootSRInformation, 
      long orderId, Throwable cause) {
    
    XynaOrderServerExtension order = rootSRInformation.getOrder();
    order.abortResumingOrder(true,cause);
    
    //alle Resume-Quellen vernichten, damit kein weiteres Resume erfolgt
    try {
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().
      cleanupOrderFamily(rootSRInformation.getOrder());
    } catch( PersistenceLayerException e ) {
      //dann sind halt nicht alle Resume-Quellen vernichtet, das soll aber Abort nicht aufhalten
      logger.warn("Could not remove resumes-sources for "+rootSRInformation.getRootId(), e );
    }
    rescheduleOrder(order);
  }
  
  
  public XynaOrderServerExtension readOrder(Long orderId, ODSConnection con) throws PersistenceLayerException, OrderBackupNotFoundException, OrderBackupNotAccessibleException {
    ODSConnection defaultCon = con != null ? con : ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      return SRHelper.getArchivedXynaOrder(orderId, defaultCon);
    } finally {
      if( con == null ) {
        defaultCon.closeConnection();
      }
    }
  }
  
  public void writeOrders(ODSConnection con, Collection<XynaOrderServerExtension> orders) throws PersistenceLayerException {
    ODSConnection defaultCon = con != null ? con : ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      for (XynaOrderServerExtension xose : orders) {
        for (XynaOrderServerExtension familyMember : xose.getOrderAndChildrenRecursively()) {
          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().backup(familyMember, BackupCause.SHUTDOWN, con);
        }
      }
    } finally {
      if( con == null ) {
        defaultCon.closeConnection();
      }
    }
  }

  public Long getOrderId(XynaOrderServerExtension order) {
    return order.getId();
  }


  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#fillOrderData(com.gip.xyna.xprc.xpce.ordersuspension.SRInformation, java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  public void fillOrderData(SRInformation srInformation, XynaOrderServerExtension order) {
    if( order == null ) {
      return; //nichts zu tun
    }
    srInformation.setRootId(order.getRootOrder().getId());
    if( order.getParentOrder() != null ) {
      srInformation.setParentLaneId(order.getParentLaneId());
      srInformation.setParentId(order.getParentOrder().getId());
    } else {
      if( srInformation instanceof RootSRInformation ) {
        RootSRInformation<XynaOrderServerExtension> rootSRInformation =
            (RootSRInformation<XynaOrderServerExtension>)srInformation;
        rootSRInformation.setOrder(order);
        rootSRInformation.setOrderHasRedirection( order.getRedirection() != null );
      }
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#getRootId(java.lang.Long, java.lang.Object)
   */
  public Long getRootId(Long orderId) throws PersistenceLayerException, OrderBackupNotFoundException {
    return SRHelper.readRootOrderIdFromOrderBackup(orderId);
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#getRootOrderId(java.lang.Object)
   */
  public Long getRootOrderId(XynaOrderServerExtension order) {
    return order.getRootOrder().getId();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#getRootOrder(java.lang.Object)
   */
  public XynaOrderServerExtension getRootOrder(XynaOrderServerExtension order) {
    return order.getRootOrder();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#extractOrder(java.lang.Long, java.lang.Object)
   */
  public XynaOrderServerExtension extractOrder(Long orderId, RootSRInformation<XynaOrderServerExtension> rootSRInformation) throws NoSuchChildException {
    long id = orderId.longValue();
    XynaOrderServerExtension xo = SRHelper.getRootOrder(rootSRInformation).getOrderInFamilyById(id);
    if (xo == null) {
      throw new NoSuchChildException(rootSRInformation.getRootId(),orderId);
    }
    return xo;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#addAllParallelExecutors(com.gip.xyna.xprc.xpce.ordersuspension.SRInformation, java.lang.Object)
   */
  public void addAllParallelExecutors(SRInformation srInformation, Step step) {
    Step parentStep = step.getParentStep();
    
    if( !( parentStep instanceof ParallelExecutionStep) ) {
      throw new IllegalStateException("Parent of step should be a ParallelExecutionStep but "+parentStep.getClass().getName() );
    }
    
    boolean added = true;
    while( added && parentStep != null ) {
      if( parentStep instanceof ParallelExecutionStep ) {
        ParallelExecutionStep<?> parent = (ParallelExecutionStep<?>) parentStep;
        added = srInformation.addParallelExecutor( parent.getFractalWorkflowParallelExecutor() );
        //wenn added == false, wurden die PEs bereits hinzugef�gt, muss nicht doppelt gemacht werden
      }
      parentStep = parentStep.getParentStep();
    }
  }

  public void cleanupOrderFamily(RootSRInformation<XynaOrderServerExtension> rootSRInformation, Long orderId, ODSConnection con) throws PersistenceLayerException {
    HashSet<Long> orderIds = new HashSet<Long>();
    try {
      XynaOrderServerExtension cleanupOrder = extractOrder(orderId,rootSRInformation);
      for( XynaOrderServerExtension xo : cleanupOrder.getOrderAndChildrenRecursively() ) {
        orderIds.add(xo.getId());
      }
    } catch (NoSuchChildException e) {
      throw new RuntimeException(e); //unerwartet!
    }
    if( logger.isDebugEnabled() ) {
      logger.debug("cleanupOrderFamily for "+orderIds);
    }
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().
    cleanupOrderFamily(rootSRInformation.getRootId(), orderIds, con);
  }

  public boolean suspendInExecution(RootSRInformation<XynaOrderServerExtension> rootSRInformation, RootOrderSuspension rootOrderSuspension) {
    XynaProcess rootProcess = SRHelper.getRootOrder(rootSRInformation).getExecutionProcessInstance();
    if( rootProcess == null ) {
      //Auftrag ist gerade erst gestartet, konnte XynaProcess noch nicht eintragen
      return false;
    } else {
      rootProcess.getRootProcessData().setRootOrderSuspension( rootOrderSuspension );
      return true;
    }
  }
      
  public boolean suspendInScheduler(RootSRInformation<XynaOrderServerExtension> rootSRInformation, RootOrderSuspension rootOrderSuspension) {
    AllOrdersList allOrders = XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList();
    for( XynaOrderServerExtension order : SRHelper.getRootOrder(rootSRInformation).getOrderAndChildrenRecursively() ) {
      if( ! order.hasParentOrder() ) {
        continue; //RootAuftr�ge sollen nicht aus Scheduler geworfen werden
      }
      //TODO hier kann nicht erkannt werden, wo sich die XynaOrder befindet. transient volatile OrderInstanceStatus 
      //     in XynaOrderServerExtension w�re gut!
      //Order befindet sich in evtl. Scheduler, TimeConstraintManagement etc. 
      SchedulingOrder so = allOrders.getSchedulingOrder(order.getId());
      if( so == null ) {
        continue; //Ist wohl doch schon in der Execution oder noch im Planning. 
        //Execution: wird dort suspendiert; Planning: muss sp�ter erneut probiert werden 
      }
      boolean removed = false;
      synchronized (so) {
        so.waitIfLocked();
        removed = so.markAsRemoved();
      }
      if( ! removed ) {
        continue; //Auftrag ist zwar noch in AllOrdersList, aber bereits in der Ausf�hrung, wird dort suspendiert
      }
      allOrders.removeOrder(order.getId());
      //Auftrag wurde aus Scheduler entfernt. Nun muss daf�r ein ResumeTarget angelegt werden
      ResumeTarget resumeTarget = new ResumeTarget(rootSRInformation.getRootId(), order.getParentOrder().getId(), order.getParentLaneId() );
      
      
      SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
      List<ResumeTarget> resumeTargets = srm.handleSuspensionEvent(rootOrderSuspension.suspend(resumeTarget), order, false);
      rootOrderSuspension.addResumeTargets(resumeTargets);
      
    }
    return true;
  }
    
  public List<Triple<RootOrderSuspension, String, PersistenceLayerException>> resumeRootOrdersWithRetries(List<RootOrderSuspension> rootOrderSuspensions) {
    
    WarehouseRetryExecutorBuilder wre = WarehouseRetryExecutor.buildCriticalExecutor().
      storable(OrderInstanceBackup.class).storable(OrderInstance.class);
    
    
    UndoSuspension undoSuspension = new UndoSuspension(rootOrderSuspensions,true);
    try {
      wre.execute(undoSuspension);
    } catch (PersistenceLayerException e) {
      //Unerwartet, da UndoSuspension intern alle PersistenceLayerException f�ngt. Probleme mit dem �ffnen der Con?
      logger.warn("Unexpected PersistenceLayerException in undoSuspensions", e);
      
      //FIXME was nun? alles als failed zur�ckgeben? noch nicht behandelte + aktuell behandelten
      //->Retry
    }
    
    //nun noch Retries:
    if( undoSuspension.numberToRetry() > 0 ) {
      List<RootOrderSuspension> resumesToRetry = undoSuspension.getResumesToRetry();
      while( ! resumesToRetry.isEmpty() ) {
        //n�chster Retry:
        UndoSuspension retry = new UndoSuspension(resumesToRetry,false);
        try {
          wre.execute(retry);
        } catch (PersistenceLayerException e1) {
          //FIXME siehe oben!
          logger.warn(null, e1);
        }
        //auswerten
        if( resumesToRetry.size() == retry.numberToRetry() ) {
          //Anzahl der Retries wird nicht weniger
          logger.warn("undo Suspensions failed, number of resumes to retry "+resumesToRetry.size()+" does not decrease");
          //Die Retries haben nicht geholfen, daher als Failed z�hlen
          undoSuspension.addRetriesAsFailed( retry );
          break;
        } else {
          //Fehler zur GesamtListe hinzuf�gen, n�chste ResumesToRetry setzen und kurz warten
          undoSuspension.addFailed( retry );
          resumesToRetry = retry.getResumesToRetry();
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            //dann halt k�rzer warten
          }
        }
      }
    }
    return undoSuspension.getFailedResumes();
  }
  
  public void interruptProcess(RootSRInformation<XynaOrderServerExtension> rootSRInformation, RootOrderSuspension rootOrderSuspension, boolean stopForcefully) {
    XynaProcess rootProcess = SRHelper.getRootOrder(rootSRInformation).getExecutionProcessInstance();
    if( rootProcess == null ) {
      //das darf nicht sein!
      logger.warn("Could not interrupt Process if process is missing");
    } else {
      int terminated = rootProcess.terminateThreadsOfRunningJavaServiceCalls(stopForcefully);
      rootOrderSuspension.setTerminateThreadCount( stopForcefully, terminated );
    }
  }
  
  public void injectMI(XynaOrderServerExtension order, RootOrderSuspension rootOrderSuspension, ODSConnection con) {
    if( order.hasParentOrder() ) {
      return; //nur f�r RootOrder relevant!
    }
    logger.debug("Inject MI to order "+order);
    ManualInteractionData manualInteractionData = rootOrderSuspension.getManualInteractionData();
    
    boolean stopForcefully = manualInteractionData.isStopForcefully();
    String message = manualInteractionData.getMIMessage();
    order.setRedirection(new Redirection(message, stopForcefully) );
    try {
      Long miOrderId = order.getRedirection().redirectOrder(order, con);
      manualInteractionData.setMIOrderId(miOrderId);
    } catch (PersistenceLayerException e) {
      logger.warn("Could not inject MI for Order "+order, e);
    }
  }
  
  public int waitUntilRootOrderIsAccessible(int retry, Long rootId) {
    boolean accessible = false;
    try {
      accessible = 
          OrderStartupAndMigrationManagement.getInstance().waitUntilRootOrderIsAccessible(rootId);
    } catch (Exception e) {
      //LoadingAbortedWithErrorException, MigrationAbortedWithErrorException, InterruptedException
      //PersistenceLayerException
      //Fehler werfen -> Resume geht kaputt
      throw new RuntimeException(e);
    }
    if( accessible ) {
      //OrderBackup sollte nun zug�nglich sein, daher normaler retry
      return retry;
    } else {
      //OrderBackup k�nnte schon l�ngst wieder erreichbar sein, das wurde allerdings nicht festgestellt.
      //daher �berwachter Retry
      if( retry >= 3 ) {
        throw new RuntimeException("OrderBackup with different BootCountID, Retry Limit 3 reached.");
      } else {
        return retry+1;
      }
    }
  }

  
  private class UndoSuspension implements WarehouseRetryExecutableNoResult {
    
    private List<Triple<RootOrderSuspension,String,PersistenceLayerException>> failedResumes = 
        new ArrayList<Triple<RootOrderSuspension,String,PersistenceLayerException>>();
    private List<Triple<RootOrderSuspension,String,PersistenceLayerException>> resumesToRetry = 
        new ArrayList<Triple<RootOrderSuspension,String,PersistenceLayerException>>();
    private List<RootOrderSuspension> suspendingOrders;
    private boolean firstCall;

    public UndoSuspension(List<RootOrderSuspension> suspendingOrders, boolean firstCall) {
      this.suspendingOrders = suspendingOrders;
      this.firstCall = firstCall;
    }

    /* (non-Javadoc)
     * @see com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult#executeAndCommit(com.gip.xyna.xnwh.persistence.ODSConnection)
     */
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      if( firstCall ) {
        for( RootOrderSuspension ros : suspendingOrders ) {
          State state = ros.undoSuspension();
          try {
            if( state == State.Suspended || state == State.Resuming ) {
              undoSuspension( ros, con );
            } else {
              //sollte nicht vorkommen, Warnung bereits geloggt.
              failedResumes.add( Triple.of(ros,"state="+state, (PersistenceLayerException)null) );
            }
          } finally {
            ros.continueResume();
          }
          con.commit();
        }
      } else {
        for( RootOrderSuspension ros : suspendingOrders ) {
          undoSuspension( ros, con );
          con.commit();
        }
      }
      con.commit();
    }
    
    public void addRetriesAsFailed(UndoSuspension retry) {
      failedResumes.addAll(retry.resumesToRetry);
    }

    public void addFailed(UndoSuspension retry) {
      failedResumes.addAll(retry.failedResumes);
    }

    public int numberToRetry() {
      return resumesToRetry.size();
    }

    public List<Triple<RootOrderSuspension,String,PersistenceLayerException>> getFailedResumes() {
      return failedResumes;
    }

    public List<RootOrderSuspension> getResumesToRetry() {
      List<RootOrderSuspension> retryOrders = new ArrayList<RootOrderSuspension>();
      for( Triple<RootOrderSuspension,String,PersistenceLayerException> triple : resumesToRetry ) {
        retryOrders.add(triple.getFirst());
      }
      return retryOrders;
    }
    
    private void undoSuspension(RootOrderSuspension ros, ODSConnection con) {
      try {
        Pair<ResumeResult,String> pair = suspendResumeAlgorithm.resumeRootOrder( ros.getRootOrderId(), ros.getResumeTargets(), con );
        switch( pair.getFirst() ) {
          case Resumed:
            break;
          case Failed:
            failedResumes.add( Triple.of(ros, pair.getSecond(), (PersistenceLayerException)null ));
            break;
          case Unresumeable:
            //darf nicht resumt werden, deswegen ab jetzt ignorieren: kein Retry, kein Fehler
            break;
          default:
            logger.warn("Unexpected ResumeResult "+pair.getFirst() );
            failedResumes.add( Triple.of(ros, pair.getSecond(), (PersistenceLayerException)null ));
        }
      } catch (PersistenceLayerException e) {
        resumesToRetry.add( Triple.of(ros, (String)null, e )); 
      }
    }

  }
  

  /**
   * Weitergehende Anpassungen an die Factory, die �ber das hinausgehen, was der SRAdapterImpl macht
   *
   */
  private static class SRHelper {

    public static XynaOrderServerExtension getArchivedXynaOrder(Long rootOrderId, ODSConnection con) throws PersistenceLayerException, OrderBackupNotFoundException, OrderBackupNotAccessibleException {
      OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();

      OrderInstanceBackup oib = oa.getBackedUpRootOrder(rootOrderId, con);
      if( oib == null ) {
        logger.warn( "Failed to resume order <" + rootOrderId + ">, no corresponding order found in orderbackup.");
        throw new OrderBackupNotFoundException(rootOrderId);
      }

      if( oib.getBootCntId() != XynaFactory.getInstance().getBootCntId()) {
        //unerwartete BootCntId, so darf Auftrag nicht resumt werden, da Auftrag noch nicht vollst�ndig 
        //migriert ist durch OrderMigration im Cluster oder beim Startup.
        throw new OrderBackupNotAccessibleException();
      }

      if( oib.getBackupCauseAsEnum() != BackupCause.SUSPENSION ) {
        logger.warn( "Failed to resume order <" + rootOrderId + ">, orderbackup entry has wrong BackupCause "+oib.getBackupCauseAsEnum()+".");
        throw new OrderBackupNotFoundException(rootOrderId);
      }

      XynaOrderServerExtension order = oib.getXynaorder();
      if (order == null) {
        logger.warn( "Failed to resume order <" + rootOrderId + ">, no corresponding order found in orderbackup.");
        throw new OrderBackupNotFoundException(rootOrderId);
      }

      //ResponseListener und OrderContext wiederherstellen
      oa.restoreTransientOrderParts(order, rootOrderId);
      return order;
    }
    
    public static XynaOrderServerExtension getRootOrder(RootSRInformation<XynaOrderServerExtension> rootSRInformation) {
      XynaOrderServerExtension rootOrder = rootSRInformation.getOrder();
      if( rootOrder == null ) {
        logger.info("RootSRInformation has no reference to order");
        AllOrdersList allOrders = XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList();
        SchedulingOrder so = allOrders.getSchedulingOrder(rootSRInformation.getRootId());
        if( so != null ) {
          rootOrder = allOrders.getXynaOrder(so);
        }
        if( rootOrder == null ) {
          //Unerwartet: Solange der Auftrag in der Execution ist kann die rootOrder nichgt aus 
          //            der RootSRInformation verschwinden
          //            Wenn der Auftrag im Scheduler ist, kann die rootOrder wegen OOM-Protection
          //            verschwinden. Gleichzeitig muss jedoch auch der Auftrag in AllOrders 
          //            auffindbar sein.
          // Evtl. Race? Aus allOrders bereits verschwunden, aber noch nicht wieder in der Execution?
          try {
            Thread.sleep(50);
          } catch( InterruptedException e ) {
            //dann halt nicht warten
          }
          rootOrder = rootSRInformation.getOrder();
          if( rootOrder == null ) {
            logger.warn("RootSRInformation without order and no entry in allOrders found");
            throw new RuntimeException("RootSRInformation without order and no entry in allOrders found");
          }
        }
      }
      return rootOrder;
    }

    public static Long readRootOrderIdFromOrderBackup(Long orderId) throws PersistenceLayerException, OrderBackupNotFoundException {
      try {
        return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
            .readRootOrderIdFromOrderBackup(null, orderId);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new OrderBackupNotFoundException(orderId, e);
      }
    }


    /**
     * @param xo
     */
    public static void updateMonitoring(XynaOrderServerExtension xo) {
      try {
        if (logger.isTraceEnabled()) {
          logger.trace("Updating monitoring settings for resumed order <" + xo.getId() + ">");
        }
        for (XynaOrderServerExtension order : xo.getOrderAndChildrenRecursively()) {
          if (order.getExecutionProcessInstance() != null) {
            order.getExecutionProcessInstance().prepareForResume();
          }
        }
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher().dispatch(xo);
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        logger.warn("Could not set monitoring level for resumed order: " + e.getMessage());
        logger.debug("", e);
        xo.setMonitoringLevel(MonitoringDispatcher.DEFAULT_MONITORING_LEVEL);
      }
    }

    public static void setSuspendResumeStatus(XynaOrderServerExtension order, boolean suspendOrResume, ODSConnection con, SuspensionCause suspensionCause ) throws XNWH_RetryTransactionException {
      OrderInstanceSuspensionStatus suspensionStatus = suspendOrResume ? OrderInstanceSuspensionStatus.SUSPENDED : OrderInstanceSuspensionStatus.NOT_SUSPENDED;
      try {
        XynaFactory.getInstance().getProcessing().getOrderStatus().suspendResumeStatus(order, suspensionStatus, con, suspensionCause );
      } catch (XNWH_RetryTransactionException e) {
        throw e;
      } catch (PersistenceLayerException e) {
        logger.error("Unexpected error while setting suspension state of order "+order.getId()+" to \""+suspensionStatus+"\"", e);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.error("Unexpected error while setting suspension state of order "+order.getId()+" to \""+suspensionStatus+"\"", e);
      }
    }

    public static void backupXynaOrder(ODSConnection con, XynaOrderServerExtension rootOrder) throws PersistenceLayerException {
      //Backup komplett schreiben, also auch f�r alle Kinder, die berites fertig gelaufen sind
      List<XynaOrderServerExtension> allOrders = rootOrder.getOrderAndChildrenRecursively();
      for ( XynaOrderServerExtension xose : allOrders ) {
        if (xose.needsToBeBackupedOnSuspensionOfParent()) {
          backupXynaOrder( con, BackupCause.FINISHED_SUBWF, xose );
        }
      }
      logger.info( "backup " + rootOrder);
      backupXynaOrder(con, BackupCause.SUSPENSION, rootOrder);
    }
    
    public static void backupXynaOrder(ODSConnection con, BackupCause backupCause, XynaOrderServerExtension xose) throws PersistenceLayerException {
      XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().backup(xose, backupCause, con);
    }
    
    public static void injectMI(XynaOrderServerExtension order, RootOrderSuspension rootOrderSuspension, ODSConnection con) throws PersistenceLayerException {
      logger.debug("Inject MI to order "+order);
      ManualInteractionData manualInteractionData = rootOrderSuspension.getManualInteractionData();

      boolean stopForcefully = manualInteractionData.isStopForcefully();
      String message = manualInteractionData.getMIMessage();
      order.setRedirection(new Redirection(message, stopForcefully) );
      Long miOrderId = order.getRedirection().redirectOrder(order, con);
      manualInteractionData.setMIOrderId(miOrderId);
    }

  }


  @Override
  public Pair<ResumeResult, String> abortSuspendedOrder(DoResume<ODSConnection> doResume,
                                                        RootSRInformation<XynaOrderServerExtension> rootSRInformation, long orderId,
                                                        boolean ignoreResourcesWhenResuming) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(); 
    try {
      cleanupOrderFamily(rootSRInformation, orderId, con);

      //Auftrag wird nun resumt und damit wieder in den Scheduler eingestellt. Dort wird er dann abgebrochen.
      XynaFactory.getInstance().getProcessing().getXynaScheduler().abortOrder(orderId, 10 * 60 * 1000, ignoreResourcesWhenResuming); //FIXME timeout konfigurierbar
      //TODO cause �bergeben

      Pair<ResumeResult, String> result = doResume.resume(con);
      con.commit();
      return result;
    } finally {
      con.closeConnection();
    }
  }

}
