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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall;



import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectTimeoutException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.NodeManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationType.DispatchingTarget;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationType.ErrorHandling;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationType.ErrorHandlingLocation;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationTypeInstance;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.NotificationProcessor.RemoteCallNotificationStatus;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications.AwaitApplicationAvailableNotification;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications.AwaitConnectivityNotification;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications.AwaitOrderNotification;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications.GetResultNotification;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications.RemoteCallNotification;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications.StartOrderNotification;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.RemoteCallXynaOrderCreationParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_OrderCouldNotBeStartedException;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspensionBackupMode;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_ShutDown;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_Standard;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;

public class RemoteCallHelper {
  
  static final Logger logger = CentralFactoryLogging.getLogger(RemoteCallHelper.class);
  
  
  private XynaOrderServerExtension correlatedXynaOrder;
  private String laneId;
  private String orderType;
  private RuntimeContext stubContext;
  private GeneralXynaObject dispatchingParameter;
  private RemoteDestinationTypeInstance rdti;
  private volatile AwaitOrderNotification awaitOrder;
  private volatile boolean isCanceled = false;
  private volatile transient DispatchingTarget dispTarget;
  private volatile transient ScheduledFuture<Void> remoteExecutionTimeout;
  
  private static final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
  
  RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  
  public RemoteCallHelper(XynaOrderServerExtension correlatedXynaOrder,
      String laneId) {
    this.correlatedXynaOrder = correlatedXynaOrder;
    this.laneId = laneId;
  }
  
  public void setRemoteParameter(String remoteDestination,
      String orderType, RuntimeContext stubContext, GeneralXynaObject dispatchingParameter) {
    this.orderType = orderType;
    this.stubContext = stubContext;
    this.dispatchingParameter = dispatchingParameter;
    
    RemoteDestinationManagement rdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement();
    this.rdti = rdm.getRemoteDestinationTypeInstance(remoteDestination);
  }


  public DispatchingTarget getDispatchingTarget(String factoryNode) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RuntimeContext ownContext = revMgmt.getRuntimeContext(correlatedXynaOrder.getRevision());
    
    DispatchingTarget target = rdti.dispatch(ownContext, stubContext, dispatchingParameter);
    
    if( factoryNode != null ) {
      target.factoryNodeName = factoryNode;
    }
    return target;
  }


  private static FactoryNodeCaller getFactoryNodeCaller(DispatchingTarget target) {
    NodeManagement nm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
    FactoryNodeCaller fnc = nm.getFactoryNodeCaller(target.factoryNodeName);
    return fnc;
  }
  
  public ResumeTarget createResumeTarget() {
    ResumeTarget rt = new ResumeTarget(correlatedXynaOrder, laneId);
    return rt;
  }

  /**
   * Starten des Auftrags auf Remote-Seite.
   * Entweder Warten bis zur Notification oder bei Nichterreichbarkeit bis zum Resume.
   * Nach Resume muss ein weiterer startRemoteOrder ausgeführt werden.
   * @param resumed
   * @param startTimeStamp
   * @param target
   * @param input
   * @return
   * @throws XynaException
   */
  public Pair<Long,DispatchingTarget> startRemoteOrder(
      boolean resumed,
      long startTimeStamp, 
      DispatchingTarget target,
      GeneralXynaObject input) throws XynaException {
    
    if( resumed ) {
      //Prüfen, ob Auftrag nicht zulange gewartet hat und daher beendet werden muss
      //TODO ersteinmal nicht vorgesehen, da keine generischen Timeouts definiert sind
    }
    
    for( int retry = 0; retry < 100; ++retry ) {
      FactoryNodeCaller fnc = getFactoryNodeCaller(target);
      RemoteCallXynaOrderCreationParameter xocp = new RemoteCallXynaOrderCreationParameter(new DestinationKey(orderType, target.context), input);
      StartOrderNotification startOrder = new StartOrderNotification(xocp);
      fnc.enqueue( startOrder );
      if( logger.isDebugEnabled() ) {
        logger.debug("RCH: startOrder "+correlatedXynaOrder.getId()  + " System time: " + System.currentTimeMillis());
      }
      
      try {
        startOrder.await(); //warte, dass startordernotification verarbeitet wurde, d.h., dass versucht wurde, den auftrag remote zu starten
      } catch (InterruptedException e ) {
        handleInterruptedException(startOrder, e);
      }
      
      if( startOrder.isSucceeded() ) {
        if( logger.isDebugEnabled() ) {
          logger.debug("RCH: startOrder "+correlatedXynaOrder.getId() +" -> " + startOrder.getRemoteOrderId() + " System time: " + System.currentTimeMillis());
        }
        return Pair.of(startOrder.getRemoteOrderId(), target);
      } else {
        Throwable ex = startOrder.parseSerializedException(orderType, correlatedXynaOrder.getRootOrder().getRevision());
        if (ex instanceof XPRC_OrderCouldNotBeStartedException) {
          if (logger.isDebugEnabled()) {
            logger.debug("RCH: startOrder failed: " + ex.getMessage()
                + (ex.getCause() != null ? "cause: " + ex.getCause().getMessage() : ""));
            if (logger.isTraceEnabled()) {
              logger.trace(null, ex);
            }
          }
          target = handleRemoteApplicationProblem((XPRC_OrderCouldNotBeStartedException)ex, fnc, target, startTimeStamp, retry);
        } else if( startOrder.getNodeConnectException() != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("RCH: could not connect to node: " + startOrder.getNodeConnectException().getMessage());
            if (logger.isTraceEnabled()) {
              logger.trace(null, startOrder.getNodeConnectException());
            }
          }
          target = handleNodeConnectException(startOrder.getNodeConnectException(), true,
              startTimeStamp, fnc, target, retry );
        } else {
          XynaException xe = startOrder.parseSerializedException(orderType, correlatedXynaOrder.getRootOrder().getRevision());
          if( xe != null ) {
            if (logger.isDebugEnabled()) {
              logger.debug("RCH: startOrder failed on node: " + xe.getMessage());
              if (logger.isTraceEnabled()) {
                logger.trace(null, xe);
              }
            }
            throw xe;
          } else {
            throw new IllegalStateException("Not reachable, neither success nor failure");
          }
        }
      }
    }
    throw new IllegalStateException("Not reachable, retry exceeded");
  }


  /**
   * Warten, dass Auftrag auf Remote-Seite bearbeitet wird:
   * Entweder bis Notification eintrifft oder ein Suspend ausgeführt wird.
   * Nach Resume kann ein weiterer awaitOrder ausgeführt werden.
   * @param target
   * @param startTimeStamp
   * @param remoteOrderId
   * @throws XFMG_NodeConnectException
   * @throws PersistenceLayerException 
   */
  public void awaitOrder(DispatchingTarget target, long startTimeStamp, Long remoteOrderId) throws XFMG_NodeConnectException, PersistenceLayerException {

    boolean suspend = false; //TODO rdti.canSuspend();
    long suspensionTimeout = 600000L; //TODO 
    
    if( ! suspend ) {
      //wenn nicht gleich eine Suspendierung erfolgt ist nun ein Backup nötig, 
      //damit RemoteOrderId, StartTimeStamp und FactoryNode nicht verloren gehen können
      
      InheritanceRule r = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getParameterInheritanceManagement()
          .getPreferredBackupWhenRemoteCallRule(correlatedXynaOrder);
      SuspensionBackupMode sbm = SuspensionBackupMode.valueOf(r.getValueAsString());
      if (sbm == SuspensionBackupMode.BACKUP) {
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().backup(correlatedXynaOrder.getRootOrder(),
                                                                                                  BackupCause.AFTER_SCHEDULING);
      }
    }
    synchronized (this) {
      checkCancelation();
      awaitOrder = null;
      if( suspend ) {
        awaitOrder = new AwaitOrderNotification(remoteOrderId, startTimeStamp+suspensionTimeout, createResumeTarget() );
      } else {
        awaitOrder = new AwaitOrderNotification(remoteOrderId);
      }
      dispTarget = target;
    }
    if (target.executionTimeout > 0) {
      long offset = System.currentTimeMillis() - startTimeStamp;
      RemoteExecutionTimeoutCallable  remoteExecutionTimeoutCallable = new RemoteExecutionTimeoutCallable();
      remoteExecutionTimeout = timeoutExecutor.schedule(remoteExecutionTimeoutCallable, target.executionTimeout - offset, TimeUnit.MILLISECONDS);
    }
    FactoryNodeCaller fnc = getFactoryNodeCaller(dispTarget);
    fnc.enqueue(awaitOrder);
    
    logger.debug("RCH: awaitOrder "+correlatedXynaOrder.getId()+ (suspend? " suspend" : "")  +  " System time: " + System.currentTimeMillis());

    try {
      awaitOrder.await();
    } catch (InterruptedException e ) {
      handleInterruptedException(awaitOrder, e);
    }
    synchronized (this) {
      checkCancelation();
    }

    if( awaitOrder.isSucceeded() ) {
      logger.debug("RCH: awaitOrder succeeded "+correlatedXynaOrder.getId() +  " System time: " + System.currentTimeMillis());
      return;
    } else if( suspend && awaitOrder.isParked() ) {
      logger.debug("RCH: awaitOrder -> suspend "+correlatedXynaOrder.getId()  +  " System time: " + System.currentTimeMillis());
      throw new ProcessSuspendedException( new SuspensionCause_Standard() );
    } else if( awaitOrder.isAborted()) {
      //order was aborted because remote node was removed. 
      //Exception is always set if aborted is true
      throw awaitOrder.getNodeConnectException();
    } else if( awaitOrder.getNodeConnectException() != null ) {
      if (logger.isDebugEnabled()) {
        logger.debug("RCH: could not connect to node: " + awaitOrder.getNodeConnectException().getMessage());
        if (logger.isTraceEnabled()) {
          logger.trace(null, awaitOrder.getNodeConnectException());
        }
      }
      fnc.enqueue( new AwaitConnectivityNotification( startTimeStamp+suspensionTimeout, createResumeTarget() ) );
      throw new ProcessSuspendedException( new SuspensionCause_Standard() );
    } else {
      throw new IllegalStateException("Not reachable");
    }
  }
  
  
  private class RemoteExecutionTimeoutCallable implements Callable<Void> {

    public Void call() throws Exception {
      cancel();
      return null;
    }
    
  }
  
  /**
   * Nach erfolgreichem awaitOrder kann nun das Ergebnis abgeholt werden, entweder 
   * XynaObject oder XFMG_NodeRemoteException.
   * @param target
   * @param remoteOrderId
   * @return
   * @throws XynaException 
   */
  public GeneralXynaObject getResult(DispatchingTarget target, Long remoteOrderId)
      throws XynaException {
    GetResultNotification getResult = new GetResultNotification(remoteOrderId);
    FactoryNodeCaller fnc = getFactoryNodeCaller(target);
    fnc.enqueue(getResult);
    try {
      getResult.await();
    } catch (InterruptedException e ) {
      handleInterruptedException(awaitOrder, e);
    }
    
    try {
      if( getResult.isSucceeded() ) {
        if( logger.isDebugEnabled() ) {
          logger.debug("RCH: getResult succeeded "+correlatedXynaOrder.getId()  +  " System time: " + System.currentTimeMillis());
        }
        Long remoteCallAppRevision = revMgmt.getRevision(stubContext);
        return getResult.getXynaObject(correlatedXynaOrder, remoteCallAppRevision);
      } else {
        XynaException xe = getResult.parseSerializedException(orderType, correlatedXynaOrder.getRootOrder().getRevision());
        if( xe != null ) {
          if( logger.isDebugEnabled() ) {
            logger.debug("RCH: getResult failed "+xe.getMessage() );
          }
          throw xe;
        } else {
          throw new IllegalStateException("Not reachable, neither success nor failure");
        }
      }
    } finally {
      if (remoteExecutionTimeout != null &&
          !remoteExecutionTimeout.isDone()) {
        remoteExecutionTimeout.cancel(false);
      }
    }
  }
  
  private void checkCancelation() {
    if (isCanceled) {
      if (XynaFactory.getInstance().isShuttingDown()) {
        throw new ProcessSuspendedException( new SuspensionCause_ShutDown() );
      } else {
        String reason;
        if (remoteExecutionTimeout != null &&
            remoteExecutionTimeout.isDone()) {
          reason = "Remote execution timed out";
        } else {
          reason = "cancelled";
        }
        throw new RuntimeException(reason);
      }
    }
  }

  private void handleInterruptedException(RemoteCallNotification notification, InterruptedException e) {
    //getResult abbrechen
    boolean removed = notification.remove();
    if (removed) {
      checkCancelation();
      throw new RuntimeException("interrupted", e);
    } else {
      //Auftrag ist nicht mehr abbrechbar, evtl. schon beendet
      if (notification.isExecuting()) {
        checkCancelation();
        throw new RuntimeException("interrupted", e);
      } else {
        //await ist mittlerweile doch gerufen worden, daher fortfahren
      }
    }
  }


  private DispatchingTarget handleRemoteApplicationProblem(XPRC_OrderCouldNotBeStartedException ex, FactoryNodeCaller fnc,
                                                           DispatchingTarget target, long startTimeStamp, int retry)
      throws XFMG_NodeConnectTimeoutException, XPRC_OrderCouldNotBeStartedException {
    ErrorHandlingLocation ehl = ErrorHandlingLocation.PHASE1;
    ErrorHandling handling = rdti.handleConnectionError(stubContext, ehl, target, ex, retry, dispatchingParameter);
    switch (handling.type) {
      case QUEUE :
        long now = System.currentTimeMillis();
        if (now > startTimeStamp + handling.timeout) {
          throw new XFMG_NodeConnectTimeoutException(fnc.getNodeName());
        } else {
          fnc.enqueue(new AwaitApplicationAvailableNotification(startTimeStamp + handling.timeout, createResumeTarget(), ex.getApplicationName()));
          throw new ProcessSuspendedException(new SuspensionCause_Standard());
        }
      case THROW :
        throw ex;
      case TRY_NEXT :
        return handling.target;
      default :
        throw new UnsupportedOperationException(handling.type + " currently not supported");
    }
  }
  
  private DispatchingTarget handleNodeConnectException(
      XFMG_NodeConnectException nodeConnectException, 
      boolean start,
      long startTimeStamp,
      FactoryNodeCaller fnc, DispatchingTarget target, int retry
      ) throws XFMG_NodeConnectException {
    
    ErrorHandlingLocation ehl = start ? ErrorHandlingLocation.PHASE1 : ErrorHandlingLocation.PHASE2;
    ErrorHandling handling = 
        rdti.handleConnectionError(stubContext, ehl, target, nodeConnectException, 
                                   retry, dispatchingParameter);
    
    switch (handling.type) {
    case THROW :
      if( handling.error instanceof XFMG_NodeConnectException ) {
        throw (XFMG_NodeConnectException)handling.error;
      } else {
        throw new XFMG_NodeConnectException(fnc.getNodeName(), handling.error);
      }
    case TRY_NEXT :
      //Retry mit anderem Target
      return handling.target;
    case QUEUE :
      long now = System.currentTimeMillis();
      if( now > startTimeStamp+handling.timeout ) {
        throw new XFMG_NodeConnectTimeoutException(fnc.getNodeName());
      } else {
        fnc.enqueue( new AwaitConnectivityNotification( startTimeStamp+handling.timeout, createResumeTarget() ) );
        throw new ProcessSuspendedException( new SuspensionCause_Standard() );
      }
    default :
      throw new UnsupportedOperationException( handling.type +" currently not supported");
    }
  }

  public void cancel() {
    synchronized (this) {
      if (!isCanceled) {
        isCanceled = true;
        if (awaitOrder != null) {
          awaitOrder.setStatusAndNotify(RemoteCallNotificationStatus.Succeeded);
          FactoryNodeCaller fnc = getFactoryNodeCaller(dispTarget);
          fnc.cancel(awaitOrder);
        }
      }
    }
  }

  
}