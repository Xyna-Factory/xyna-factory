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
package com.gip.xyna.xprc.xpce.execution;



import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaRuntimeException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.OrderStatus;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ProcessingStage;
import com.gip.xyna.xprc.XynaRunnable;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_ABORTED_EXCEPTION;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_CANCELLED;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_SCHEDULING_TIMEOUT;
import com.gip.xyna.xprc.exceptions.XPRC_UNEXPECTED_ERROR_PROCESS;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xfractwfe.ProcessAbortedException;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess.XynaProcessState;
import com.gip.xyna.xprc.xpce.ResponseListenerWithOrderDeathSupport;
import com.gip.xyna.xprc.xpce.cleanup.XynaCleanup;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.TwoConnectionBean;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceCompensationStatus;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.AllOrdersList;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintManagement;



/**
 * Klasse gibt die Reihenfolge und die zugehörige Fehlerbehandlung für alle Einzel Schritte des Masterworkflows im
 * Processing nach dem Scheduling an. Alle Fehler vor der Ausführung der Execution-Destination führen zur Ausführung des
 * Cleanups. Alle Fehler während des Cleanups werden in der XynaOrder geloggt und dann der nächste Schritt im Cleanup
 * ausgeführt. technisch: Die Fehlerbehandlung von Einzelschritten ist in der Methode
 * executeAndCatchEverythingExceptSuspension() gekapselt.
 */
public class MasterWorkflowPostScheduler extends XynaRunnable {

  private static final Logger logger = CentralFactoryLogging.getLogger(MasterWorkflowPostScheduler.class);

  private final SchedulingOrder so;
  private XynaOrderServerExtension xo;
  private XynaScheduler scheduler;
  private Mode mode;
  
  public static enum Mode {
    Normal, Terminate, TerminateAndIgnoreCapacitiesAndVetos;
  }
  
  public MasterWorkflowPostScheduler(XynaOrderServerExtension xo, Mode mode) {
    this.so = null;
    if (xo == null) {
      throw new RuntimeException(XynaOrder.class.getSimpleName() + " may not be null.");
    }
    this.xo = xo;
    this.mode = mode;
    this.scheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler();
    if (this.scheduler == null) {
      throw new RuntimeException("Failed to determine " + XynaScheduler.DEFAULT_NAME);
    }
  }

  public MasterWorkflowPostScheduler(SchedulingOrder so, Mode mode) {
    if (so == null) {
      throw new RuntimeException(SchedulingOrder.class.getSimpleName() + " may not be null.");
    }
    this.so = so;
    this.scheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler();
    if (this.scheduler == null) {
      throw new RuntimeException("Failed to determine " + XynaScheduler.DEFAULT_NAME);
    }
    this.mode = mode;
  }

  public MasterWorkflowPostScheduler(XynaOrderServerExtension xo) {
    this.so = null;
    if (xo == null) {
      throw new RuntimeException(XynaOrder.class.getSimpleName() + " may not be null.");
    }
    this.xo = xo;
    this.mode = Mode.Normal;
    this.scheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler();
    if (this.scheduler == null) {
      throw new RuntimeException("Failed to determine " + XynaScheduler.DEFAULT_NAME);
    }
  }


  private interface Executable {
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException;
  }
  
  private static final Executable execution_dispatch_updateStatus_RunningExecution =
      new ChangeMasterWorkflowStatusExecutable(OrderInstanceStatus.RUNNING_EXECUTION, true);

  private static final Executable execution_dispatch_updateCompensationStatus_Running =
      new ChangeCompensationStatusExecutable(OrderInstanceCompensationStatus.RUNNING);

  private static final Executable execution_dispatch = new Executable() {
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      CentralFactoryLogging.logOrderTiming(mw.xo.getId(), "call exec destination");
      //UpdateStatus in dispatch 
      XynaExecution xe = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution();
      
      DestinationValue dv = xe.getExecutionDestination(mw.xo.getDestinationKey());
      mw.xo.setExecutionDestination(dv);
      mw.xo.setExecutionType(dv.getDestinationType());

      if (mw.xo.getDestinationKey().isCompensate() ) {
        // The pre compensate step handler also sets this status. This is better place to set the status, but
        // if an error occurs within the workflow itself the order does not pass a dispatcher again.
        
        mw.executeAndCatchEverythingToAddAsWarning( execution_dispatch_updateCompensationStatus_Running, ProcessingStage.COMPENSATION);
        
      } else {
        mw.executeAndCatchEverythingToAddAsWarning( execution_dispatch_updateStatus_RunningExecution, ProcessingStage.EXECUTION);
      }
      
      xe.dispatch(mw.xo);
      CentralFactoryLogging.logOrderTiming(mw.xo.getId(), "finished exec destination");
    }
  };
  
  private static final Executable execution_status_finished = 
      new ChangeMasterWorkflowStatusExecutable(OrderInstanceStatus.FINISHED_EXECUTION, true);
  
  
  private static final class ChangeMasterWorkflowStatusExecutable implements Executable {

    private OrderInstanceStatus orderInstanceStatus;
    private boolean success;

    public ChangeMasterWorkflowStatusExecutable(OrderInstanceStatus orderInstanceStatus, boolean success) {
      this.orderInstanceStatus = orderInstanceStatus;
      this.success = success;
    }
    
    @Override
    public String toString() {
      return "ChangeMasterWorkflowStatus "+orderInstanceStatus;
    }

    @Override
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      OrderStatus orderStatus = XynaFactory.getInstance().getProcessing().getOrderStatus();
      if( success ) {
        orderStatus.changeMasterWorkflowStatus(mw.xo, orderInstanceStatus, null);
      } else {
        orderStatus.changeErrorStatus(mw.xo, orderInstanceStatus);
      }
    }
    
  }
  
  private static final class ChangeCompensationStatusExecutable implements Executable {

    private OrderInstanceCompensationStatus compensationStatus;

    public ChangeCompensationStatusExecutable(OrderInstanceCompensationStatus compensationStatus) {
      this.compensationStatus = compensationStatus;
    }
    
    @Override
    public String toString() {
      return "ChangeCompensationStatus "+compensationStatus;
    }

    @Override
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      OrderStatus orderStatus = XynaFactory.getInstance().getProcessing().getOrderStatus();
      orderStatus.compensationStatus(mw.xo, compensationStatus);
    }
    
  }

   

  private static final Executable execution = new Executable() {

    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      XynaOrderServerExtension xo = mw.xo;
      if (!xo.getSchedulingData().isHasAcquiredCapacities()) {
        throw new RuntimeException("order is expected to have aquired its capacities after scheduling.");
      }
      //fehler vor execution führen zu cleanup
      //status update geschieht in dispatcher
      mw.executeAndCatchEverythingExceptSuspension(execution_dispatch, ProcessingStage.EXECUTION);
      // this is just for consistency (und wird von frequencycontrolled tasks verwendet)
      
      mw.executeAndCatchEverythingToAddAsWarning(execution_status_finished, ProcessingStage.EXECUTION);
      //hangeMasterWorkflowStatus(mw.xo, OrderInstanceStatus.FINISHED_EXECUTION, true);
    }

  };

  private static final Executable cleanup_updateStatus_RunningCleanup = 
      new ChangeMasterWorkflowStatusExecutable(OrderInstanceStatus.RUNNING_CLEANUP, true);
  
  private static final Executable cleanup_batch = new Executable() {
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      //Falls der Auftrag ein BatchProcess-Master ist, geht der BatchProcess jetzt in den Status Cleanup über
      if(mw.xo.getBatchProcessMarker() != null && mw.xo.getBatchProcessMarker().isBatchProcessMaster()){
        BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
        try {
          bpm.masterInCleanup(mw.xo.getBatchProcessMarker().getBatchProcessId());
        } catch( Throwable t ) {
          handleThrowable(mw.xo, t, ProcessingStage.OTHER);
        }
      }
    }
  };
  
  private static final Executable cleanup_xynacleanup = new Executable() {
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      XynaCleanup.cleanup(mw.xo);
    }
  };
  
  private static final Executable finish_xynacleanupfinally = new Executable() {
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      XynaCleanup.cleanupFinally(mw.xo);
    }
  };
  
  private static final Executable finish_updateCompensationStatus_Finished = 
      new ChangeCompensationStatusExecutable(OrderInstanceCompensationStatus.FINISHED);

  
  private static final Executable finish_updateCompensationStatus_Error = 
      new ChangeCompensationStatusExecutable(OrderInstanceCompensationStatus.ERROR);
  
  private static final Executable cleanup_dispatch = new Executable() {
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup().dispatch(mw.xo);
    }
  };

  private static final Executable cleanup_updateStatus_FinishCleanup = 
      new ChangeMasterWorkflowStatusExecutable(OrderInstanceStatus.FINISHED_CLEANUP, true);


  private static final Executable cleanup_handleSeries = new Executable() {
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      if( mw.xo.isInOrderSeries() ) {
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getOrderSeriesManagement().finishOrder(mw.xo);
      }
    }
  };
  
  private static final Executable cleanup_handleSuspendResume = new Executable() {
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().cleanupSuspensionEntries(mw.xo.getId());
    }
  };

  private static final Executable cleanup = new Executable() {
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      ProcessingStage stage = ProcessingStage.CLEANUP;
      mw.executeAndCatchEverythingToAddAsWarning(cleanup_updateStatus_RunningCleanup, stage);
      
      if(mw.xo.getBatchProcessMarker() != null && mw.xo.getBatchProcessMarker().isBatchProcessMaster()) {
        mw.executeAndCatchEverythingExceptSuspension(cleanup_batch, stage);
      }
      
      if( XynaProperty.CLEANUP_WORKFLOW_BEFORE_FREEING_CAP_VETO.get() ) {
        mw.executeAndCatchEverythingExceptSuspension(cleanup_dispatch, stage);
        mw.executeAndCatchEverythingExceptSuspension(cleanup_xynacleanup, stage);
      } else {
        mw.executeAndCatchEverythingExceptSuspension(cleanup_xynacleanup, stage);
        mw.executeAndCatchEverythingExceptSuspension(cleanup_dispatch, stage);
      }
      mw.executeAndCatchEverythingExceptSuspension(cleanup_handleSeries, stage);
      mw.executeAndCatchEverythingExceptSuspension(cleanup_handleSuspendResume, stage);
      mw.executeAndCatchEverythingToAddAsWarning(cleanup_updateStatus_FinishCleanup, stage);
    }
  };
  
  private static final Executable finish_compensation_compensation_finished = 
      new ChangeCompensationStatusExecutable(OrderInstanceCompensationStatus.FINISHED );
  private static final Executable finish_compensation_compensation_error = 
      new ChangeCompensationStatusExecutable(OrderInstanceCompensationStatus.ERROR );
  
  private static final Executable finish_compensation = new Executable() {
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      if (mw.xo.getExecutionProcessInstance() != null) {
        if (mw.xo.getExecutionProcessInstance().hasCompensatedSuccesfully()) {
          mw.executeAndCatchEverythingToAddAsWarning(finish_compensation_compensation_finished, ProcessingStage.COMPENSATION);
        } else {
          mw.executeAndCatchEverythingToAddAsWarning(finish_compensation_compensation_error, ProcessingStage.COMPENSATION);
        }
      }
    }
  };
  
  private static final Executable finish_updateStatus_Error =
      new ChangeMasterWorkflowStatusExecutable(OrderInstanceStatus.XYNA_ERROR, false);
 
  private static final Executable finish_updateStatus_Canceled =
      new ChangeMasterWorkflowStatusExecutable(OrderInstanceStatus.CANCELED, false);
 
  private static final Executable finish_updateStatus_Timeout =
      new ChangeMasterWorkflowStatusExecutable(OrderInstanceStatus.SCHEDULING_TIME_OUT, false );
 
  private static final Executable finish_updateStatus_Finish =
      new ChangeMasterWorkflowStatusExecutable(OrderInstanceStatus.FINISHED, true );
  
  

  
  private TwoConnectionBean archivingBean = null;
  
  private static final Executable finish_archive = new Executable() {

    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      CentralFactoryLogging.logOrderTiming(mw.xo.getId(), "archiving");
      mw.archivingBean = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().archive(mw.xo);
    }

  };


  private static final Executable finish = new Executable() {
    
    public void execute(MasterWorkflowPostScheduler mw) throws XynaException {
      ProcessingStage stage = ProcessingStage.OTHER;
      OrderInstanceStatus status = getWfStatus(mw.xo);
      if( status == OrderInstanceStatus.FINISHED ) {
        mw.executeAndCatchEverythingToAddAsWarning(finish_updateStatus_Finish, stage);
      } else {
        if( status == OrderInstanceStatus.CANCELED ) {
          mw.executeAndCatchEverythingToAddAsWarning(finish_updateStatus_Canceled, stage);
        } else if( status == OrderInstanceStatus.SCHEDULING_TIME_OUT ) {
          mw.executeAndCatchEverythingToAddAsWarning(finish_updateStatus_Timeout, stage);
        } else {
          mw.executeAndCatchEverythingToAddAsWarning(finish_updateStatus_Error, stage);
        }
        mw.executeAndCatchEverythingExceptSuspension(finish_compensation, stage);
      }

      int retryCounter = 0;

      while (true) {
        mw.executeAndCatchEverythingToAddAsWarning(finish_archive, ProcessingStage.ARCHIVING);
        try {
          //später: wenn die connections bereits vorher zugemacht werden, dann braucht man in BatchProcess.terminateMaster keinen eigenen thread.
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().callResponseListener(mw.xo, mw.archivingBean);
          break;
        } catch (XNWH_RetryTransactionException ctcbe) {
          //FIXME es ist fragwürdig, ob das wirklich eine so gute idee ist, hier retries zu machen.
          //wenn der auftrag das commit auf die DEFAULT connection (insbesondre orderarchive) bereits durchführen konnte,
          //dann funktioniert der erneute aufruf von finish_archive nicht so gut. es können dann keine auditdaten aus DEFAULT geholt werden.
          //es sollte eigtl auch wie im archive() ein fehler dazu führen, dass auf ALTERNATIVE ausgewichen wird, damit die daten nicht verloren gehen.
          
          // Cannot use warehouseRetryExecutor here because we need two simultanous connections
          // exception kann nur von responselistener kommen
          retryCounter++;

          if (retryCounter > Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES) { //TODO Konstantenname!
            mw.archivingBean.closeUncommited();
            logger.error("Was unable to finish scheduling for order " + mw.xo.getId(), ctcbe);
            break;
          } else {
            logger.info("Connection to cluster broken. Retrying...", ctcbe); //TODO Cluster?
          }
        }
      }
      mw.executeAndCatchEverythingExceptSuspension(finish_xynacleanupfinally, stage); 
    }
    
    private OrderInstanceStatus getWfStatus(XynaOrder xo) {
      if( ! xo.hasError() && !xo.isCancelled() && !xo.isTimedOut() ) {
        return OrderInstanceStatus.FINISHED;
      }
      XynaException[] errors = xo.getErrors();
      if( errors == null || errors.length == 0 ) {
        if( xo.isTimedOut() ) {
          return OrderInstanceStatus.SCHEDULING_TIME_OUT;
        } else if( xo.isCancelled() ) {
          logger.warn( "finish.getWfStatus: isCanceled without exception XPRC_PROCESS_CANCELLED" );
          return OrderInstanceStatus.CANCELED;
        } else {
          logger.warn( "finish.getWfStatus: no errors attached "+ xo.hasError() );
          return OrderInstanceStatus.XYNA_ERROR; //TODO
        }
      } else if( errors.length == 1 ) {
        XynaException xe = errors[0];
        if( xe == null ) {
          logger.warn( "finish.getWfStatus: attached error is null" );
          return OrderInstanceStatus.XYNA_ERROR; //TODO
        } else if( xe instanceof XPRC_PROCESS_CANCELLED ) {
          if( ! xo.isCancelled() ) {
            logger.warn( "finish.getWfStatus: has exception XPRC_PROCESS_CANCELLED but is not canceled" );
          }
          return OrderInstanceStatus.CANCELED;
        } else if( xe instanceof XPRC_PROCESS_SCHEDULING_TIMEOUT ) {
          if( ! xo.isTimedOut() ) {
            logger.warn( "finish.getWfStatus: has exception XPRC_PROCESS_SCHEDULING_TIMEOUT but is not timed out" );
          }
          return OrderInstanceStatus.SCHEDULING_TIME_OUT;
        } else {
          return OrderInstanceStatus.XYNA_ERROR;
        }
      } else {
        //zuviele XynaExceptions, dies kann kein Cancel oder SchedulingTimeout sein
        return OrderInstanceStatus.XYNA_ERROR;
      }
    }
    
  };
  
  
  private static final Executable reinitialize_monitoringhandler = new Executable() {

    public void execute(MasterWorkflowPostScheduler mw) {
      try {
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher().dispatch(mw.xo);
      } catch (XynaException e) {
        // was not able to set the monitoring level, might result in missing compensation information
        logger.warn("Could not determine monitoring code for id " + mw.xo.getId() + " for compensation. Cause: "
            + e.getMessage());
        logger.debug("", e);
      }
    }

  };


  private void handleOrderDeath(MasterWorkflowPostScheduler mw, OrderDeathException ode) {
    ResponseListener rl = mw.xo.getResponseListener();
    if (rl instanceof ResponseListenerWithOrderDeathSupport) {
      ((ResponseListenerWithOrderDeathSupport) rl).onOrderDeath(ode);
    }
  }

  private void handleAbortion(final ProcessAbortedException abortedException) {
    if (logger.isDebugEnabled()) {
      logger.debug("Order " + xo.getId() + " was aborted");
    }
    abortedException.setOriginId(xo.getId());

    XPRC_PROCESS_ABORTED_EXCEPTION xpae = 
        new XPRC_PROCESS_ABORTED_EXCEPTION(xo.getId(), 
            abortedException.getAbortionCause().getAbortionCauseString(), 
            abortedException);
    
    XynaProcess xp = xo.getExecutionProcessInstance();
    if( xp != null ) {
      Throwable cause = xp.getRootProcessData().getAbortionCause();
      if( cause != null ) {
        Throwable c = xpae;
        while( c.getCause() != null ) {
          c = c.getCause();
        }
        c.initCause(cause);
      }
    }
    
    xo.addException(xpae, ProcessingStage.OTHER);
    xo.setAbortionException(abortedException);
  }


  /**
   * Lässt nur {@link ProcessSuspendedException} und die von Department.handleThrowable weitergeworfenen Errors durch,
   * Alles andere wird gefangen und als Fehler in der XynaOrder geloggt.
   * @param r
   */
  private void executeAndCatchEverythingExceptSuspension(Executable r, ProcessingStage stage) {
    try {
      r.execute(this);
    } catch (XynaException xe) {
      xo.addException(xe, stage);
    } catch (ProcessSuspendedException e) {
      throw e;
    } catch (ProcessAbortedException e) {
      handleAbortion(e);
    } catch (OrderDeathException ode) {
      logger.error("Connection to database appears to be broken, stopping execution", ode);
      throw ode;
    } catch (RuntimeException re) {
      addThrowableAsXynaException(xo, re, stage);
    } catch (Throwable t) {
      Department.handleThrowable(t);
      addThrowableAsXynaException(xo, t, stage);
    }
  }
  
  /**
   * Lässt nur die von Department.handleThrowable weitergeworfenen Errors durch,
   * Alles andere wird gefangen und soll als Warnung an die XynaOrder angehängt werden (TODO)
   * und wird bis dahin nur geloggt.
   * @param r
   */
  private void executeAndCatchEverythingToAddAsWarning(Executable r, ProcessingStage stage) {
    try {
      r.execute(this);
    } catch (XynaException xe) {
      //xo.addException(xe, stage);
      logger.warn("Could not execute "+r.toString(), xe);
    } catch (RuntimeException re) {
      //addThrowableAsXynaException(xo, re, stage);
      logger.warn("Could not execute "+r.toString(), re);
    } catch (Throwable t) {
      Department.handleThrowable(t);
      //addThrowableAsXynaException(xo, t, stage);
      logger.warn("Could not execute "+r.toString(), t);
    }
     //FIXME Warnung schreiben! 
    
  }

  private static void handleThrowable( XynaOrder xo, Throwable t, ProcessingStage stage) {
    if( t == null ) {
      logger.warn("Throwable is null");
    } else if( t instanceof XynaException ) {
      if (xo instanceof XynaOrderServerExtension) {
        ((XynaOrderServerExtension)xo).addException((XynaException)t, stage);
      } else {
        xo.addException((XynaException)t);
      }
    } else if( t instanceof ProcessSuspendedException ) {
      throw (ProcessSuspendedException)t;
    } else if( t instanceof ProcessAbortedException ) {
      throw (ProcessAbortedException)t;
    } else if( t instanceof OrderDeathException ) {
      throw (OrderDeathException)t;
    } else if( t instanceof RuntimeException ) {
      addThrowableAsXynaException(xo, t, stage);
    } else {
      Department.handleThrowable(t);
      addThrowableAsXynaException(xo, t, stage);
    }
  }
  

  public void run() {
    try {
      if( so != null ) { //häufigste Ausführung hat SchedulingOrder gesetzt
        extractXynaOrder();
      }
      if( xo == null ) {
        if( so != null ) {
          scheduler.getAllOrdersList().removeOrder(so);
          XynaFactory.getInstance().getProcessing().killStuckProcess(so.getOrderId(), true, AbortionCause.UNKNOWN);
        } else {
          //keine Möglichkeit, Auftrag abbzubrechen
        }
        return;
      }
      runInternallyWithXynaOrder();
    } catch (OrderDeathException ode) {
      logger.warn("Connection to database appears to be broken, stopping execution",ode);
      handleOrderDeath(this, ode);
      return;
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error(null, t);
    }
  }
  
  private void extractXynaOrder() {
    //XynaOrder aus SchedulingOrder extrahieren
    xo = so.getXynaOrderOrNull();
    AllOrdersList allOrders = scheduler.getAllOrdersList();
    try {
      if( xo == null ) {
        xo = allOrders.getXynaOrder(so);
        if( xo == null ) {
          logger.warn("Could not read XynaOrder for SchedulingOrder "+so.getOrderId());
          return; //Fehler
        }
      }
      
      if( so.getSchedulingExceptions() != null ) {
        for( XynaException xe : so.getSchedulingExceptions() ) {
          xo.addException( xe, ProcessingStage.SCHEDULING );
        }
      }
      
      if( mode == Mode.Terminate ) {
        if( ! xo.hasError() ) {
          if( so.isMarkedAsCanceled() ) {
            xo.setCancelled(true);
            xo.addException(new XPRC_PROCESS_CANCELLED(xo.getId()), ProcessingStage.SCHEDULING);
          } else if( so.isMarkedAsTimedout() ) {
            xo.setTimedOut(true);
            xo.addException(TimeConstraintManagement.buildSchedulingTimeoutException(so.getOrderId(),so, xo), ProcessingStage.SCHEDULING);
          } else {
            logger.warn(so + " has no error -> aborted" );
            xo.addException(new XPRC_UNEXPECTED_ERROR_PROCESS(xo.getDestinationKey().getOrderType(),
                                                              so+" should be terminated but has no error" ),
                                                              ProcessingStage.SCHEDULING);
          }
        }
      }
    } finally {
      //SchedulingOrder aus AllOrdersList entfernen
      so.setOrderStatus(OrderInstanceStatus.RUNNING);
    }
  }
  
  private void runInternallyWithXynaOrder() {
    CentralFactoryLogging.logOrderTiming(xo.getId(), "executionthread start");
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
    try {
      if (xo.mustDeploymentCounterBeCountDown()) {
        //xynaprocess.cleanupAbortFailedThreads zählt hoch
        DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
        xo.setDeploymentCounterCountDownDone();
      }
      if (so != null) {
        scheduler.getAllOrdersList().removeOrder(so);
      }
      //ausgeführte XynaOrder dem SuspendResumeManagement bekanntmachen, damit dort notwendige Daten zur 
      //Suspendierung bereit sind 
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().
      getSuspendResumeManagement().addStartedOrder(xo.getId(), xo);

      switch( mode ) {
        case Normal:
          runNormal();
          break;
        case Terminate:
          //fall through
        case TerminateAndIgnoreCapacitiesAndVetos :
          runTerminate();
          break;
      }
    } finally {
      DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
      CentralFactoryLogging.logOrderTiming(xo.getId(), "executionthread finished");
    }
  }

  private void runNormal() {
    // overwrite possible old settings concerning whether the order needs rescheduling
    // wird nur bei entsprechenden suspension-exceptions auf false gestellt
    SchedulingData schedulingData = xo.getSchedulingData();
    schedulingData.setNeedsToAcquireCapacitiesOnNextScheduling(true);
    schedulingData.setNeedsToAcquireVetosOnNextScheduling(true);
    schedulingData.setNeedsToCheckTimeConstraintOnNextScheduling(false);
    
    if (xo.getBatchProcessMarker() != null && xo.getBatchProcessMarker().isBatchProcessMaster()) {
      try {
        XynaFactory.getInstance().getProcessing().getBatchProcessManagement().updateMasterAfterScheduling(xo.getBatchProcessMarker());
      } catch (PersistenceLayerException e) {
        logger.warn("BatchProcess master " + xo.getId() + " could not be updated after scheduling.", e);
        xo.addException(e, ProcessingStage.EXECUTION);
      }
    }
    
    if (!xo.hasParentOrder() && XynaProperty.XYNA_ORDER_STARTUP_MODE.get().isSafeMode()) {
      try {
        xo.setHasBeenBackuppedAfterChange(false);
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().backup(xo, BackupCause.AFTER_SCHEDULING);
      } catch (PersistenceLayerException e) {
        logger.warn("Can not backup order with id <" + xo.getId() + "> and backup cause AFTER_SCHEDULING.", e);
      }
    }

    final boolean addedOrderContextMapping;
    if (XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderContextConfiguration()
                    .isDestinationKeyConfiguredForOrderContextMapping(xo.getDestinationKey(), false)) {
      if (xo.getOrderContext() == null) {
        xo.setNewOrderContext();
      }
      XynaFactory.getInstance().getProcessing().getWorkflowEngine().setOrderContext(xo.getOrderContext());
      addedOrderContextMapping = true;
    } else {
      addedOrderContextMapping = false;
    }
    try {
      if (xo.getDestinationKey().isCompensate()) {
        compensateMasterWorkflow();
      } else {
        normalMasterWorkflow();
      }
    } finally {
      if (addedOrderContextMapping) {
        // if the factory is shutting down, this is not important any longer. if the order had been killed,
        // it might even result in NullPointerExceptionS because the killed thread may return a little later
        // than the time the factory components have started shutting down.
        if (!XynaFactory.getInstance().isShuttingDown()) {
          XynaFactory.getInstance().getProcessing().getWorkflowEngine().removeOrderContext();
        }
      }
    }
  }


  private void runTerminate() {
    final String ndc = xo.getLoggingDiagnosisContext(XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT.get());
    final boolean createLoggingContextForThis = ndc != null;
    if (createLoggingContextForThis) {
      NDC.push(ndc);
    }
    try {
      if (xo.getExecutionProcessInstance() != null) {
        if (xo.getExecutionProcessInstance().getState() == XynaProcessState.SUSPENDED
            || xo.getExecutionProcessInstance().getState() == XynaProcessState.SUSPENDED_AFTER_ABORTING) {
          //resume: auftrag ist nicht mehr im scheduler!
          xo.abortResumingOrder(mode == Mode.TerminateAndIgnoreCapacitiesAndVetos, null); //TODO Cause?
          runNormal();
        } else if (xo.getDestinationKey().isCompensate()) {
          //compensate
          xo.setAbortionException(new ProcessAbortedException(AbortionCause.UNKNOWN)); //FIXME abortionCause und orginOrderId von ursprünglichem abort erben
          cleanupCompensateBeforeOrderHasBeenScheduled(false);
        } else {
          //ungültiger zustand?
          logger.error("order " + xo
              + " with execution process instance was cancelled, but is neither resuming nor compensating. process state = "
              + xo.getExecutionProcessInstance().getState());
        }
      } else {
        //vor der execution phase
        cleanupBeforeOrderHasBeenScheduled(false);
      }
    } finally {
      if (createLoggingContextForThis) {
        NDC.pop();
      }
    }
  }

  
  private void normalMasterWorkflow() {
    final String ndc = xo.getLoggingDiagnosisContext( XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT.get() );
    final boolean createLoggingContextForThis = ndc != null;
    if( createLoggingContextForThis ) {
      NDC.push(ndc);
    }
    
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Executing order " + xo.getId() + " with thread '" + Thread.currentThread().getName() + "'");
      }
      //execution phase
      try {
        executeAndCatchEverythingExceptSuspension(execution, ProcessingStage.EXECUTION);
      } catch( ProcessSuspendedException pse ) {
        if( handleSuspension(pse) ) {
          return;
        }
      }
      
      //cleanup
      executeAndCatchEverythingExceptSuspension(cleanup, ProcessingStage.CLEANUP);
      //finish
      executeAndCatchEverythingExceptSuspension(finish, ProcessingStage.OTHER);
    } catch( ProcessSuspendedException pse ) {
      logger.warn("Unexpected ProcessSuspendedException in cleanup oder finish", pse);
      //FIXME was nun?
    } catch (OrderDeathException ode) {
      // if the exception is thrown from handleSuspension (which can happen quite easily) it will be caught here
      logger.warn("Connection to database appears to be broken, stopping execution", ode);
      handleOrderDeath(this, ode);
      return;
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("unexpected error occurred during execution of order " + xo, t);
    } finally {
      if (createLoggingContextForThis) {
        NDC.pop();
      }
      archivingBean = null;
    }
    //evtl hat scheduler wegen threadpool-engpass angehalten.
    scheduler.notifyScheduler();
  }

  private void compensateMasterWorkflow() {
    //compensate wird nur aufgerufen, wenn auftrag fehlerfrei war.
    //exceptions die an der xyna order hängen, sind also eindeutig der compensation zuordbar.
    final String ndc = xo.getLoggingDiagnosisContext( XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT.get() );
    final boolean createLoggingContextForThis = ndc != null;
    if( createLoggingContextForThis ) {
      NDC.push(ndc);
    }
    ProcessingStage stage = ProcessingStage.COMPENSATION;
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Compensating order " + xo.getId() + " with thread '" + Thread.currentThread().getName() + "'");
      }

      try {
        executeAndCatchEverythingExceptSuspension(reinitialize_monitoringhandler, stage);
        //execution phase
        executeAndCatchEverythingExceptSuspension(execution_dispatch, stage);
      } catch( ProcessSuspendedException pse ) {
        if( handleSuspension(pse) ) {
          return;
        }
      }

      //caps freigeben
      executeAndCatchEverythingExceptSuspension(cleanup_xynacleanup, stage);
      executeAndCatchEverythingExceptSuspension(finish_xynacleanupfinally, stage);
      executeAndCatchEverythingExceptSuspension(cleanup_handleSuspendResume, stage);
   
      if( xo.hasError() ) {
        executeAndCatchEverythingToAddAsWarning(finish_updateCompensationStatus_Error, stage);
      } else {
        executeAndCatchEverythingToAddAsWarning(finish_updateCompensationStatus_Finished, stage);
      }
      
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().callResponseListener(xo, null);
    } catch (OrderDeathException ode) {
      logger.warn("Connection to database appears to be broken, stopping execution", ode);
      handleOrderDeath(this, ode);
      return;
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("Unexpected error occurred during execution of order " + xo, t);
    } finally {
      if (createLoggingContextForThis) {
        NDC.pop();
      }
    }

    //evtl hat scheduler wegen threadpool-engpass angehalten.
    scheduler.notifyScheduler();

  }


  /**
   * @param processSuspendedException
   * @return true, wenn keine weitere Bearbeitung erforderlich ist
   */
  private boolean handleSuspension(ProcessSuspendedException processSuspendedException) {
    try {
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().
      handleSuspensionEvent(processSuspendedException, xo, true);
      return true;
      //TODO sollte man hier im fehlerfall die kindaufträge die schon suspendiert sind killern?
      // Hier gibt es unterschiedliche Fehlerfälle:
      // 1) DB komplett nicht mehr erreichbar. In diesem Fall wirft die Methode eine OrderDeathException. Es ist nichts
      //    weiter zu tun, weil die Factory in diesem Fall sowieso herunterfährt.
      // 2) Eine sonstige PersistenceLayerException: Außer bei der internen Entwicklung sollte das nur passieren, wenn
      //    es echte Probleme mit der Datenbank gibt und der Knoten im Single-Betrieb ist (dann wird kein OrderDeath
      //    geworfen).
      // 3) XPRC_UNEXPECTED_ERROR_PROCESS ist eigentlich eine RuntimeException. Wird mit Throwable behandelt.
      // 4) XPRC_ErrorDuringSuspensionHandling: Inhaltlicher Fehler bei funktionierender Warehouse-Verbindung. In diesem
      //    Fall Cleanup für die Subaufträge nachholen und 
    } catch (OrderDeathException ode) {
      logger.warn("Connection to database appears to be broken, stopping execution", ode);
      handleOrderDeath(this, ode);
      return true;
    } catch (Throwable t) {
      // FIXME wie FeatureRelatedExceptionDuringSuspensionHandling behandeln? Warehouse-Connectivity ist ja potentiell gegeben
      Department.handleThrowable(t);
      addThrowableAsXynaException(xo, t, ProcessingStage.EXECUTION);
      return false;
    }
  }

  /**
   * cleanup + finish
   */
  //ACHTUNG methodenname in orderanddeploymentcounter referenziert
  public void cleanupBeforeOrderHasBeenScheduled(boolean countDeploymentCounter) {
    if (countDeploymentCounter) {
      DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
    }
    try {
      executeAndCatchEverythingExceptSuspension(cleanup, ProcessingStage.CLEANUP);
      executeAndCatchEverythingExceptSuspension(finish, ProcessingStage.OTHER);
    } finally {
      if (countDeploymentCounter) {
        DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
      }
    }
  }

  
  public static void addThrowableAsXynaException(XynaOrder xo, Throwable t) {
    addThrowableAsXynaException(xo, t, ProcessingStage.OTHER);
  }
  
  
  public static void addThrowableAsXynaException(XynaOrder xo, Throwable t, ProcessingStage stage) {
    String message;
    if (t instanceof XynaRuntimeException) {
      message = t.toString();
    } else {
      message = t.getMessage();
    }
    if (logger.isDebugEnabled()) {
      logger.debug("unexpected exception ('" + message + "') caught during execution", t);
    }
    XynaException newE = new XPRC_UNEXPECTED_ERROR_PROCESS((xo.getDestinationKey() != null ? xo
                    .getDestinationKey().getOrderType() : "unknown"), t.getClass().getSimpleName() + " "
                    + message, t);
    if (xo instanceof XynaOrderServerExtension) {
      ((XynaOrderServerExtension)xo).addException(newE, stage);
    } else {
      xo.addException(newE);
    }
  }

  //ACHTUNG methodenname in orderanddeploymentcounter referenziert
  public void cleanupCompensateBeforeOrderHasBeenScheduled(boolean countDeploymentCounter) {
    final String ndc = xo.getLoggingDiagnosisContext(XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT.get());
    final boolean createLoggingContextForThis = ndc != null;
    if (createLoggingContextForThis) {
      NDC.push(ndc);
    }
    
    if (countDeploymentCounter) {
      DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
    }
    try {
      executeAndCatchEverythingExceptSuspension(finish_updateCompensationStatus_Error, ProcessingStage.COMPENSATION);
      
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().callResponseListener(xo, null);
      scheduler.notifyScheduler();
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("Unexpected error occurred during execution of order " + xo, t);
    } finally {
      if (countDeploymentCounter) {
        DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
      }
      if (createLoggingContextForThis) {
        NDC.pop();
      }
    }
  }

  public XynaOrderServerExtension getOrder() {
    return xo;
  }
  
}
