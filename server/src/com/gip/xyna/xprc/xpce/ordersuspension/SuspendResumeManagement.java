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
package com.gip.xyna.xprc.xpce.ordersuspension;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyEnum;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.exceptions.XPRC_ResumeFailedException;
import com.gip.xyna.xprc.exceptions.XPRC_SuspendFailedException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xpce.AbstractBackupAck;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension.AcknowledgableObject;
import com.gip.xyna.xprc.xpce.SubworkflowResponseListener;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.ordersuspension.SRInformation.SRState;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.AbortionOfSuspendedOrderResult;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.ResumeResult;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.ResumableParallelExecutor;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.Step;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_Manual;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_ShutDown;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderStartupAndMigrationManagement;
import com.gip.xyna.xprc.xsched.ClusteredScheduler;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.orderabortion.SuspendedOrderAbortionSupportListenerInterface;
import com.gip.xyna.xprc.xsched.ordersuspension.ResumeMultipleOrdersBean;
import com.gip.xyna.xprc.xsched.ordersuspension.ResumeOrderBean;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendAllOrdersBean;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendOrderBean;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendOrdertypeBean;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendRevisionsBean;
import com.gip.xyna.xprc.xsched.scheduling.ClusteredSchedulerRemoteInterface;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;


/**
 * SuspendResumeManagement soll sich um alles kümmern, was mit Suspend/Resume in Zusammenhang steht.
 * TODO 
 * 
 * 
 */
public class SuspendResumeManagement {

  private final static String DEFAULT_NAME = "SuspendResumeManagement";
  public static final String UNRESUMABLE_LOCKED = "Locked";
  public static final String UNRESUMABLE_MI_REDIRECTION = "Order has MI-Redirection";
  public static final String FAILED_ORDERBACKUP_NOT_FOUND = "OrderBackup not found";

  private static Logger logger = CentralFactoryLogging.getLogger(SuspendResumeManagement.class);
  
  public static XynaPropertyEnum<BackupFailedAction> SUSPEND_RESUME_BACKUP_FAILED_ACTION = 
      new XynaPropertyEnum<BackupFailedAction>("xyna.xprc.xpce.ordersuspension.backup_failed_action", BackupFailedAction.class, BackupFailedAction.KeepRunning).
      setDefaultDocumentation(DocumentationLanguage.EN, "Action when backup for suspension can not be written: KeepRunning, Abort").
      setDefaultDocumentation(DocumentationLanguage.DE, "Aktion, wenn das Backup für die Suspendierung nicht geschrieben werden kann: KeepRunning, Abort");
      
  
  
  public static enum BackupFailedAction {
    KeepRunning,
    Abort;
  }
  
  
  private SuspendResumeAlgorithm<ODSConnection,XynaOrderServerExtension> suspendResumeAlgorithm;
  
  private List<SuspendedOrderAbortionSupportListenerInterface> suspendedOrderAbortionSupportListeners;

  private XynaScheduler scheduler; 

  public SuspendResumeManagement() {
    suspendedOrderAbortionSupportListeners = new ArrayList<SuspendedOrderAbortionSupportListenerInterface>();
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(SuspendResumeManagement.class,DEFAULT_NAME+".init").
      after(XynaProcessCtrlExecution.class, XynaScheduler.class, OrderStartupAndMigrationManagement.class).
      before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
      execAsync(new Runnable() { public void run() { init(); } });
  }
   
  protected void init() {
    scheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler();
    SuspendResumeAdapterImpl suspendResumeAdapterImpl = new SuspendResumeAdapterImpl();
    suspendResumeAlgorithm = new SuspendResumeAlgorithm<ODSConnection,XynaOrderServerExtension>( suspendResumeAdapterImpl );

    XynaProperty.RESUME_RETRY_DELAY.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.SUSPEND_RESUME_SHOW_SRINFORMATION_LOCK_INFO.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    SUSPEND_RESUME_BACKUP_FAILED_ACTION.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
  }
  
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  public void shutdown() throws XynaException {
    suspendResumeAlgorithm.shutdown();
  }
  
  public void addListener( SuspendedOrderAbortionSupportListenerInterface soasl ) {
    if( soasl != null ) {
      suspendedOrderAbortionSupportListeners.add(soasl);
    }
  }

  public void removeListener(SuspendedOrderAbortionSupportListenerInterface soasl) {
    if( suspendedOrderAbortionSupportListeners != null ) { //NPE bei ShutDown während fehlerhaftem FactoryStart verhindern
      if( soasl != null ) {
        suspendedOrderAbortionSupportListeners.remove(soasl);
      }
    }
  }
 

  public Pair<ResumeResult, String> resumeOrderRemote(int binding, ResumeTarget target) {
    if( scheduler instanceof ClusteredScheduler ) {
      Pair<ResumeResult, String> result = null;
      try {
        ResumeOrderRemotelyRMIRunnable resumeOrderRemotely = new ResumeOrderRemotelyRMIRunnable(binding,target);

        boolean res = ((ClusteredScheduler)scheduler).resumeOrderRemotely(resumeOrderRemotely);
        if( res ) {
          result = Pair.of(ResumeResult.Resumed, "");
        } else {
          result = Pair.of(ResumeResult.Failed, "Remote resume failed");
        }
      } finally {
        if (logger.isDebugEnabled()) {
          logger.debug("resumeOrderRemote for "+target+" and binding " + binding +" with result "+result);
        }
      }
      return result;
    } else {
      //TODO: kann derzeit nicht auftreten, sollte umgebaut werden auf geclustertes SuspendResumeManagement
      throw new UnsupportedOperationException("scheduler is no instance of ClusteredScheduler"); 
    }
  }
  
  public static class ResumeOrderRemotelyRMIRunnable implements RMIRunnableNoException<Boolean, ClusteredSchedulerRemoteInterface> {

    private int binding;
    private ResumeTarget target;
    
    public ResumeOrderRemotelyRMIRunnable(int binding, ResumeTarget target) {
      this.binding = binding;
      this.target = target;
    }

    public Boolean execute(ClusteredSchedulerRemoteInterface clusteredInterface) throws RemoteException {
      return clusteredInterface.resumeOrderRemotely(binding, target);
    }
    
  }

  public void resumeOrderAsynchronously(ResumeTarget target, AcknowledgableObject ack) throws XPRC_OrderEntryCouldNotBeAcknowledgedException, XPRC_ResumeFailedException {
    resumeOrderInternally(target, false, 0, 0, ack, true);
  }
    
  public Long resumeOrderAsynchronouslyDelayed(ResumeTarget target, int retryCount, ODSConnection con, boolean mayDelegateToOtherNodeIfOrderIsNotFound) throws PersistenceLayerException {
    try {
      long delay = XynaProperty.RESUME_RETRY_DELAY.getMillis();
      Pair<ResumeOrderBean, Long> pair = resumeOrderInternally(target, false, delay, retryCount, new DelayedResumeAck(con), mayDelegateToOtherNodeIfOrderIsNotFound );
      return pair.getSecond(); //resumeOrderId;
    } catch(XPRC_ResumeFailedException e) {
      //Fehler sollte nicht auftreten, da nur bei synchronem Aufruf geworfen
      throw new RuntimeException(e);
    } catch (XPRC_OrderEntryCouldNotBeAcknowledgedException e) {
      logger.warn( "Delayed ResumeOrder could not be acknowledged", e );
      if( e.getCause() instanceof PersistenceLayerException ) {
        throw (PersistenceLayerException) e.getCause();
      } else {
        throw new RuntimeException(e);
      }
    }    
  }

  private static class DelayedResumeAck extends AbstractBackupAck {
    private static final long serialVersionUID = 1L;

    public DelayedResumeAck(ODSConnection con) {
      super(con);
    }

    @Override
    protected BackupCause getBackupCause() {
      return BackupCause.ACKNOWLEDGED;
    }

  }

  
 
  /**
   * TODO eigentlich nur benötigt, weil direktes Resume nicht mit Cluster umgehen kann!
   * Ansonsten bringt die extra ResumeOrder nichts
   * @param target
   * @param sync
   * @param delay
   * @param retryCount
   * @param ack
   * @return
   * @throws XPRC_ResumeFailedException
   * @throws XPRC_OrderEntryCouldNotBeAcknowledgedException
   */
  private Pair<ResumeOrderBean,Long> resumeOrderInternally(ResumeTarget target, boolean sync, long delay, int retryCount, AcknowledgableObject ack, boolean mayDelegateToOtherNodeIfOrderIsNotFound) throws XPRC_ResumeFailedException,
      XPRC_OrderEntryCouldNotBeAcknowledgedException {
    ResumeOrderBean rob = new ResumeOrderBean(target,retryCount);
    rob.setMayDelegateToOtherNodeIfOrderIsNotFound(mayDelegateToOtherNodeIfOrderIsNotFound);    
    XynaOrderCreationParameter xocp = xynaOrderCreationParameter(XynaDispatcher.DESTINATION_KEY_RESUME, 
                                                                 rob );
    try {
      if( delay != 0 ) {
        xocp.setTimeConstraint(TimeConstraint.delayed(delay));
      }
      if (ack != null) {
        xocp.setAcknowledgableObject(ack);
      }
      if (sync) {
        rob = (ResumeOrderBean) getXynaProcessCtrlExecution().startOrderSynchronously(xocp);
        return Pair.of( rob, null);
      } else {
        return Pair.of( null, getXynaProcessCtrlExecution().startOrder(xocp) );
      }
    } catch (XPRC_OrderEntryCouldNotBeAcknowledgedException e) {
      throw e;
    } catch (XynaException e) {
      throw new XPRC_ResumeFailedException(target.toString(), e);
    }
  }

  /**
   * @return
   */
  private XynaProcessCtrlExecution getXynaProcessCtrlExecution() {
    return XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution();
  }

  
  public ResumeMultipleOrdersBean resumeMultipleOrders(List<ResumeTarget> targets, boolean sync) throws XPRC_ResumeFailedException {
    if (targets == null)
      throw new IllegalArgumentException("List of targets to be resumed may not be null");
    try {
      if (sync) {
        return (ResumeMultipleOrdersBean) startOrderSynchronously(XynaDispatcher.DESTINATION_KEY_RESUME_MULTIPLE, 
                                                                 new ResumeMultipleOrdersBean(targets) ); 
      } else {
        getXynaProcessCtrlExecution().startOrder(new XynaOrderCreationParameter(XynaDispatcher.DESTINATION_KEY_RESUME_MULTIPLE, 
                                                                                new ResumeMultipleOrdersBean(targets)));
        return null;
      }
    } catch (XynaException e) {
      throw new XPRC_ResumeFailedException(targets.toString(), e);
    }
  }


  public SuspendOrderBean suspendOrder(Long orderId) throws XPRC_SuspendFailedException {
    try {
      return (SuspendOrderBean) startOrderSynchronously( XynaDispatcher.DESTINATION_KEY_SUSPEND, 
                                                         new SuspendOrderBean(orderId) );
    } catch (XynaException e) {
      throw new XPRC_SuspendFailedException(String.valueOf(orderId), e);
    }
  }

  public SuspendAllOrdersBean suspendAllOrders(boolean suspendForShutdown) throws XPRC_SuspendFailedException {
    try {
      return (SuspendAllOrdersBean) startOrderSynchronously( XynaDispatcher.DESTINATION_KEY_SUSPEND_ALL, 
                                                             new SuspendAllOrdersBean(suspendForShutdown) );
    } catch (XynaException e) {
      throw new XPRC_SuspendFailedException("all", e);
    }
  }

  public SuspendOrdertypeBean suspendOrdertype(String ordertypeToSuspend, boolean killStuckOrders, Long revision, boolean keepUnresumeable) throws XPRC_SuspendFailedException {
    try {
      SuspendOrdertypeBean request = new SuspendOrdertypeBean(ordertypeToSuspend, killStuckOrders, revision);
      request.setKeepUnresumeable(keepUnresumeable);
      return (SuspendOrdertypeBean) startOrderSynchronously( XynaDispatcher.DESTINATION_KEY_SUSPEND_ORDERTYPE, request);
    } catch (XynaException e) {
      throw new XPRC_SuspendFailedException(ordertypeToSuspend, e);
    }
  }
  
  
  public SuspendRevisionsBean suspendRevisions(Set<Long> revisionsToSuspend, boolean killStuckOrders, boolean keepUnresumeable) throws XPRC_SuspendFailedException {
    try {
      SuspendRevisionsBean request = new SuspendRevisionsBean(revisionsToSuspend, killStuckOrders);
      request.setKeepUnresumeable(keepUnresumeable);
      return (SuspendRevisionsBean) startOrderSynchronously( XynaDispatcher.DESTINATION_KEY_SUSPEND_REVISIONS, request);
    } catch (XynaException e) {
      throw new XPRC_SuspendFailedException(revisionsToSuspend.toString(), e);
    }
  }

  /**
   * @param destinationKey
   * @param payload
   * @throws XynaException 
   */
  private GeneralXynaObject startOrderSynchronously(DestinationKey destinationKey, GeneralXynaObject payload) throws XynaException {
    return getXynaProcessCtrlExecution().startOrderSynchronously( xynaOrderCreationParameter(destinationKey, payload) );                                                       
  }

  /**
   * @param destinationKey
   * @param payload
   * @return
   */
  private XynaOrderCreationParameter xynaOrderCreationParameter(DestinationKey destinationKey, GeneralXynaObject payload) {
    XynaOrderCreationParameter xocp =
      new XynaOrderCreationParameter(destinationKey, payload);
    xocp.setPriority(Thread.MAX_PRIORITY);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return xocp;
  }
     
  public Pair<ResumeResult, String> resumeOrder(ResumeTarget target) throws PersistenceLayerException {
    return suspendResumeAlgorithm.resume(target);
  }
  
  public Pair<ResumeResult, String> resumeOrder(XynaOrderServerExtension order, ODSConnection con) throws PersistenceLayerException {
    if( logger.isDebugEnabled() ) {
      logger.debug( "Resuming order "+order+" direct");
    }
    return suspendResumeAlgorithm.resume(order, con);
  }

  public Set<String> getLaneIdsToResume(Long orderId, ResumableParallelExecutor parallelExecutor) {
    Set<String> laneIdsToResume = suspendResumeAlgorithm.getLaneIdsToResume(orderId,parallelExecutor);
    if (logger.isDebugEnabled()) {
      logger.debug( "resume laneIdsToResume " + laneIdsToResume +" for " + parallelExecutor.getParallelExecutorId() );
    }
    return laneIdsToResume;
  }
  
  public void addStartedOrder(Long orderId, XynaOrderServerExtension order) {
    suspendResumeAlgorithm.addStartedOrder(orderId, order);
    SRInformation srInfo = suspendResumeAlgorithm.getSRInformationCache().getOrCreateLocked(orderId, order, SRState.Running);
    srInfo.setState(SRState.Running);
    srInfo.unlock();
  }

  public void addParallelExecutor(Long orderId, XynaOrderServerExtension order, ResumableParallelExecutor parallelExecutor) {
    suspendResumeAlgorithm.addParallelExecutor(orderId, order, parallelExecutor);
  }

  public List<ResumeTarget> handleSuspensionEvent(ProcessSuspendedException suspendedException, 
      XynaOrderServerExtension order, boolean resumeAllowed) {
    
    if (logger.isDebugEnabled()) {
      logger.debug("Order " + order.getId() + " was suspended");
    }
    //jetzt capacities freigeben, damit das flag in der order gesetzt wird, bevor das backup im suspendstatus-update geschieht
    SuspensionCause suspensionCause = suspendedException.getSuspensionCause();
    scheduler.freeCapacitiesAndVetos(order, suspensionCause.needToFreeCapacities(), suspensionCause.needToFreeVetos());
    
    Pair<Boolean, List<ResumeTarget>> pair = suspendResumeAlgorithm.handleSuspensionEvent(suspendedException, order.getId(), order, resumeAllowed);
    
    if( pair.getFirst() ) { // ist suspendiert
      ResponseListener rl = order.getResponseListener();
      if( order.getResponseListener() instanceof SubworkflowResponseListener ) {
        ((SubworkflowResponseListener)rl).onSuspended(suspendedException);
      }
    }
    return pair.getSecond(); //ResumeTargets
  }
  
  public void handleSuspensionEventInParallelStep(ProcessSuspendedException suspendedException, long orderId, Step step) {
    suspendResumeAlgorithm.handleSuspensionEventInParallelStep(suspendedException, orderId, step);
  }

  public void cleanupSuspensionEntries(long orderId) {
    suspendResumeAlgorithm.cleanupSuspensionData(orderId);
  }

  public AbortionOfSuspendedOrderResult abortSuspendedWorkflow(XynaOrderServerExtension xo, Long rootOrderId, boolean ignoreResourcesWhenResuming, boolean forceResume) throws PersistenceLayerException {
    return suspendResumeAlgorithm.abortSuspendedWorkflow(xo, rootOrderId, ignoreResourcesWhenResuming, forceResume);
  }
  
  public void cleanupOrderFamily(XynaOrderServerExtension rootOrder) throws PersistenceLayerException {
    
    HashSet<Long> orderIds = new HashSet<Long>();
    for( XynaOrderServerExtension xo : rootOrder.getOrderAndChildrenRecursively() ) {
      orderIds.add(xo.getId());
    }
    if( logger.isDebugEnabled() ) {
      logger.debug("cleanupOrderFamily for "+orderIds);
    }
    CleanupOrderFamily cof = new CleanupOrderFamily(this, rootOrder.getId(), orderIds);
    WarehouseRetryExecutor.buildMinorExecutor().
        storable(OrderInstanceBackup.class). //FIXME dieses Storable wird nicht benötigt
        execute(cof);
    
    //Korrekt wäre die Ermittlung der Storables über 
    //for (SuspendedOrderAbortionSupportListenerInterface component : suspendedOrderAbortionSupportListeners ) {
      //storableList.adAll( component.getStorableClassList() );
    //}
    
  }

  private static class CleanupOrderFamily implements WarehouseRetryExecutableNoResult {
    private SuspendResumeManagement suspendResumeManagement;
    private long rootId;
    private HashSet<Long> orderIds;
    
    public CleanupOrderFamily(SuspendResumeManagement suspendResumeManagement,
        long rootId, HashSet<Long> orderIds) {
      this.suspendResumeManagement = suspendResumeManagement;
      this.rootId = rootId;
      this.orderIds = orderIds;
    }

    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      suspendResumeManagement.cleanupOrderFamily(rootId, orderIds, con);
      con.commit();
    }
    
  }

  public void cleanupOrderFamily(long rootOrderId, Set<Long> orderIds, ODSConnection con) throws PersistenceLayerException {
    for (SuspendedOrderAbortionSupportListenerInterface component : suspendedOrderAbortionSupportListeners ) {
      component.cleanupOrderFamily(rootOrderId, orderIds, con);
    }
  }

  /**
   * Convenience-Methode zur Ermittlung des SuspensionCause
   */
  public static ProcessSuspendedException suspendManualOrShutDown(Long orderId, String laneId) {
    SuspensionCause suspensionCause = null;
    if (XynaFactory.getInstance().isShuttingDown()) {
      suspensionCause = new SuspensionCause_ShutDown();
    } else {
      suspensionCause = new SuspensionCause_Manual();
    }
    suspensionCause.setLaneId(laneId);
    return new ProcessSuspendedException(suspensionCause);
  }
  
  
  /**
   * Nur für Ausgabe über ListSuspendResumeInfo
   * @return
   */
  public Map<Long, String> getRunningOrders() {
    return suspendResumeAlgorithm.getSRInformationCache().getRunningOrders();
  }

  /**
   * Nur für Ausgabe über ListSuspendResumeInfo
   * @param id
   * @return
   */
  public String getSRInformation(Long id) {
    return suspendResumeAlgorithm.getSRInformationCache().getSRInformationAsString(id);
  }

  /**
   * Locken der Aufträge, damit diese kein Resume beginnen können
   * @param orderIds
   */
  public void addUnresumeableOrders(Collection<Long> orderIds) {
    suspendResumeAlgorithm.getSRInformationCache().addUnresumeableOrders(orderIds);
  }
  
  public void removeUnresumableOrders(Collection<Long> orderIds) {
    suspendResumeAlgorithm.getSRInformationCache().removeUnresumableOrders(orderIds);
  }
    
  public Set<Long> getUnresumeableOrders() {
    return suspendResumeAlgorithm.getSRInformationCache().getUnresumeableOrders();
  }

  
  public SuspendRootOrderData suspendRootOrders( SuspendRootOrderData suspendRootOrderData) {
    return suspendResumeAlgorithm.suspendRootOrders(suspendRootOrderData);
  }
  
  public Pair<ResumeResult,String> resumeRootOrder( Long rootOrderId, List<ResumeTarget> targets, ODSConnection con) throws PersistenceLayerException {
    return suspendResumeAlgorithm.resumeRootOrder(rootOrderId, targets, con);
  }

  public Map<Long,Pair<String, PersistenceLayerException>> resumeRootOrdersWithRetries(Map<Long, ArrayList<ResumeTarget>> rootOrderTargets) {
    return suspendResumeAlgorithm.resumeRootOrdersWithRetries(rootOrderTargets);
  }

  public void handleParallelExecutorFinished(Long orderId, ResumableParallelExecutor parallelExecutor) {
    suspendResumeAlgorithm.handleParallelExecutorFinished(orderId, parallelExecutor.getParallelExecutorId());
  }

  public Long getRootIdIfSuspendedInMemory(long orderId) {
    List<XynaOrderServerExtension> list = suspendResumeAlgorithm.getSRInformationCache().getSuspendedOrdersInMemory();
    for (XynaOrderServerExtension root : list) {
      if (root.getId() == orderId) {
        return orderId;
      }
      XynaOrderServerExtension xo = root.getOrderInFamilyById(orderId);
      if (xo != null && xo.getId() == orderId) {
        return root.getId();
      }
    }
    return null;
  }

}
