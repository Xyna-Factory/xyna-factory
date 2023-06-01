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
package com.gip.xyna.xprc.xpce.planning;



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement.OptionalOISGenerateMetaInformation;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.OrderStatus;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ProcessingStage;
import com.gip.xyna.xprc.XynaOrderServerExtension.TransientFlags;
import com.gip.xyna.xprc.exceptions.XPRC_UNEXPECTED_ERROR_PROCESS;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xpce.ResponseListenerWithOrderDeathSupport;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.execution.MasterWorkflowPostScheduler;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringDispatcher;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderStartupMode;



/**
 * Klasse gibt die Reihenfolge und die zugehörige Fehlerbehandlung für alle Einzel Schritte des Masterworkflows im
 * Processing vor dem Scheduling an. Im Fehlerfall wird das Cleanup aufgerufen.
 */
public class MasterWorkflowPreScheduler {

  private static final Logger logger = CentralFactoryLogging.getLogger(MasterWorkflowPreScheduler.class);

  private XynaOrderServerExtension xo;
  private XynaProcessCtrlExecution xpce;
  private OrderArchive orderArchive = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();


  public MasterWorkflowPreScheduler(XynaOrderServerExtension xo, XynaProcessCtrlExecution xpce) {
    this.xo = xo;
    this.xpce = xpce;
  }


  private void setThreadPriority() {
    // set the priority if it is different from the currently set one
    // the priority may be between 1 and 10 (including 1 and 10)
    Thread current = Thread.currentThread();
    if (current.getPriority() != xo.getPriority()) {
      //FIXME achtung, funktioniert nur zusammen mit -XX:ThreadPriorityPolicy
      current.setPriority(xo.getPriority());
    }

  }


  private void discoverMonitoringLevel() {
    try {
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher().dispatch(xo);
    } catch (XynaException e) {
      // was not able to set the monitoring level, do something? this will result in errors later
      logger.warn("Could not determine monitoring code for id " + xo.getId() + ", using error monitoring. Cause: "
          + e.getMessage());
      logger.debug("", e);
      xo.setMonitoringLevel(MonitoringDispatcher.DEFAULT_MONITORING_LEVEL);
    }
  }
  
  
  private void discoverPriority() {
    XynaFactory.getInstance().getFactoryManagement().discoverPriority(xo);
  }


  private void insertIntoOrderArchive() {
    // store the instance in the overall workflow instance database
    try {
      orderArchive.insert(xo);
    } catch (PersistenceLayerException e1) {
      // TODO evtl konfigurieren, ob aufträge bei fehlern in der persistierung abgebrochen werden?
      logger.warn("could not persist instance of " + xo + ". this does not interrupt execution of order.", e1);
    }
  }


  private void xynaPlanning() throws XynaException {

    if (logger.isInfoEnabled()) {
      logger.info("starting " + xo.getId() + ", ordertype = " + xo.getDestinationKey().getOrderType()
          + " with Parameters " + xo.getInputPayload());
    }

    discoverPriority();
    setThreadPriority();
    discoverMonitoringLevel();
    insertIntoOrderArchive();
    
    changeMasterWorkflowStatus(OrderInstanceStatus.RUNNING_PLANNING);
 
    // validate
    xpce.getXynaPlanning().validate(xo);

    prepareOrderInputSources();
    
    OrderStartupMode orderStartupMode = XynaProperty.XYNA_ORDER_STARTUP_MODE.get();
    xo.setOrderStartupMode(orderStartupMode);
  }


  private void prepareOrderInputSources() throws XynaException {
    if (!xo.areOrderInputSourcesPrepared()) {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement()
          .prepareOrderInputs(xo, new OptionalOISGenerateMetaInformation());
    }
  }


  private void changeMasterWorkflowStatus(OrderInstanceStatus orderInstanceStatus) {
    try {
      OrderStatus orderStatus = XynaFactory.getInstance().getProcessing().getOrderStatus();
      orderStatus.changeMasterWorkflowStatus(xo, orderInstanceStatus, null);
    } catch (Throwable t) {
      Department.handleThrowable(t);
      //TODO Exception nicht loggen, sondern als Warnung an XynaOrder anhängen
      logger.warn("Could not write orderinstance status "+orderInstanceStatus+" for "+xo, t);
    }
  }


  public void startOrder() {
    CentralFactoryLogging.logOrderTiming(xo.getId(), "start planning");
    
    try {
      RevisionManagement revisionManagement =
                      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      
      if (xo.getDestinationKey().getApplicationName() != null) {
        if (xo.getDestinationKey().getVersionName() == null) {
          Long revision = revisionManagement.getRevision(xo.getDestinationKey().getRuntimeContext());
          // aktuellen VersionName ermitteln und setzen
          RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(revision);
          xo.getDestinationKey().setRuntimeContext(runtimeContext);
        }
      }

      try {
        if (!OrdertypeManagement.internalOrdertypes.contains(xo.getDestinationKey().getOrderType())) { //achtung, interne destinationkeys überschreiben teilweise methoden, vgl. XynaDispatcher.<clinit>
          //wenn die revision nicht gesetzt ist, die aus dem destinationkey übernehmen
          if (xo.getRevision() == null || xo.getRevision().equals(RevisionManagement.REVISION_DEFAULT_WORKSPACE)) {
            xo.setRevision(revisionManagement.getRevision(xo.getDestinationKey().getRuntimeContext()));
          }

          //in welcher revision ist der ordertype definiert? => entsprechenden RTC auch für den destinationkey verwenden
          DestinationKey dk = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement().resolveDestinationKey(xo.getDestinationKey().getOrderType(), xo.getRevision());
          xo.setDestinationKey(dk);
        }
        

        xynaPlanning();
        // dispatcht auftrag zu planningworkflow und setzt schedulerBean von XynaOrder      

        final boolean addedOrderContext;
        OrderContext parentOrderContext = null;
        try {

          if (XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderContextConfiguration()
              .isDestinationKeyConfiguredForOrderContextMapping(xo.getDestinationKey(), false)) {
            if (xo.getOrderContext() == null) {
              xo.setNewOrderContext();
            }
            parentOrderContext = XynaFactory.getInstance().getProcessing().getWorkflowEngine().setOrderContext(xo.getOrderContext());
            addedOrderContext = true;
          } else {
            addedOrderContext = false;
          }

          try {
            try {
              xpce.getXynaPlanning().dispatch(xo);
              xo.updateInputPayload();
            } finally {
              changeMasterWorkflowStatus(OrderInstanceStatus.FINISHED_PLANNING);
            }
          } finally {
            // the purpose of the second part of this if statement is only performance: if there is
            // a parent OrderContext, the code below will set the reference to that object anyway
            if (addedOrderContext && parentOrderContext == null) {
              XynaFactory.getInstance().getProcessing().getWorkflowEngine().removeOrderContext();
            }
          }

        } finally {
          if (parentOrderContext != null) {
            XynaFactory.getInstance().getProcessing().getWorkflowEngine().setOrderContext((OrderContextServerExtension) parentOrderContext);
          }
        }

        // schedule
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getPreScheduler().preschedule(xo);

      } catch (XNWH_RetryTransactionException e) {
        if( xo.isTransientFlagSet(TransientFlags.WasKnownToScheduler) ) {
          //Auftrag ist dem Scheduler bekannt, daher darf Auftrag nicht mehr abgebrochen werden
          logger.warn("Unexpected exception after adding order to scheduler", e);
        } else {
          // FIXME implement a better check that the factory is shutting down due to a crash
          if (new OrderInstanceBackup().getClusterState(ODSConnectionType.DEFAULT) == ClusterState.DISCONNECTED_SLAVE) {
            throw new OrderDeathException(e);
          } else {
            xo.addException(e, ProcessingStage.INITIALIZATION);
            new MasterWorkflowPostScheduler(xo).cleanupBeforeOrderHasBeenScheduled(false);
          }
        }
      }

    } catch (XynaException e) {
      if( xo.isTransientFlagSet(TransientFlags.WasKnownToScheduler) ) {
        //Auftrag ist dem Scheduler bekannt, daher darf Auftrag nicht mehr abgebrochen werden
        logger.warn("Unexpected exception after adding order to scheduler", e);
      } else {
        xo.addException(e, ProcessingStage.INITIALIZATION);
        handleAcknowledgableObjectAtError(xo, e);
        new MasterWorkflowPostScheduler(xo).cleanupBeforeOrderHasBeenScheduled(false);
      }
    } catch (OrderDeathException ode) {
      if( xo.isTransientFlagSet(TransientFlags.WasKnownToScheduler) ) {
        //Auftrag ist dem Scheduler bekannt, daher darf Auftrag nicht mehr abgebrochen werden
        logger.warn("Unexpected exception after adding order to scheduler", ode);
      } else {
        logger.warn("Order cannot be processed any longer", ode);
        xo.addException(new XPRC_UNEXPECTED_ERROR_PROCESS(xo.getDestinationKey().getOrderType(), ode.getClass()
                                                          .getSimpleName() + " " + ode.getMessage(), ode), ProcessingStage.INITIALIZATION);
        handleOrderDeath(this, ode);
      }
    } catch (Throwable t) {
      try {
        if( xo.isTransientFlagSet(TransientFlags.WasKnownToScheduler) ) {
          //Auftrag ist dem Scheduler bekannt, daher darf Auftrag nicht mehr abgebrochen werden
          logger.warn("Unexpected exception after adding order to scheduler", t);
        } else {
          xo.addException(new XPRC_UNEXPECTED_ERROR_PROCESS(xo.getDestinationKey().getOrderType(), t.getClass()
            .getSimpleName() + " " + t.getMessage(), t), ProcessingStage.INITIALIZATION);
          if (logger.isTraceEnabled()) {
            logger.trace(null, t);
          }
          handleAcknowledgableObjectAtError(xo, t);
          new MasterWorkflowPostScheduler(xo).cleanupBeforeOrderHasBeenScheduled(false);
        }
      } finally {
        Department.handleThrowable(t);
      }
    }

  }
  
  private void handleAcknowledgableObjectAtError(XynaOrderServerExtension xose, Throwable throwable) {
    try {
      if(!xo.isAcknowledgeSuccessfullyExecuted() && xo.getOrderContext().getAcknowledgableObject() != null) {
        xo.getOrderContext().getAcknowledgableObject().handleErrorAtPlanning(xo, throwable);
      }
    } catch (Throwable e) {
      logger.error("Error while calling handleErrorAtPlanning at acknowledgable object.", e);
    }
  }


  private void handleOrderDeath(MasterWorkflowPreScheduler mw, OrderDeathException ode) {
    ResponseListener rl = mw.xo.getResponseListener();
    if (rl instanceof ResponseListenerWithOrderDeathSupport) {
      ((ResponseListenerWithOrderDeathSupport) rl).onOrderDeath(ode);
    }
  }

}
