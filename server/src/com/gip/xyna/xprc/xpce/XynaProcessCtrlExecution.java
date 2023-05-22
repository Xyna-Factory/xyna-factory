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

package com.gip.xyna.xprc.xpce;



import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.utils.concurrent.ExceptionGatheringFutureCollection;
import com.gip.xyna.utils.concurrent.ExceptionGatheringFutureCollection.GatheredException;
import com.gip.xyna.utils.concurrent.ExecutionWrapper;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.ResultController;
import com.gip.xyna.xmcp.SynchronousSuccesfullOrderExecutionResponse;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.ResponseListener.ResponseListenerResponse;
import com.gip.xyna.xprc.ResponseListenerWithSuspensionSupport;
import com.gip.xyna.xprc.XynaExecutor;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ProcessingStage;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_HANGING_PROCESS;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_ABORTED_EXCEPTION;
import com.gip.xyna.xprc.exceptions.XPRC_UNEXPECTED_ERROR_PROCESS;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xfractwfe.ProcessAbortedException;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xpce.cleanup.XynaCleanup;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.execution.MasterWorkflowPostScheduler;
import com.gip.xyna.xprc.xpce.execution.XynaExecution;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringDispatcher;
import com.gip.xyna.xprc.xpce.orderexecutiontimeoutmanagement.OrderExecutionTimeoutManagement;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xpce.planning.MasterWorkflowPreScheduler;
import com.gip.xyna.xprc.xpce.planning.XynaPlanning;
import com.gip.xyna.xprc.xpce.startup.Startup;
import com.gip.xyna.xprc.xpce.statustracking.StatusChangeProvider;
import com.gip.xyna.xprc.xpce.transaction.TransactionManagement;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.TwoConnectionBean;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.SchedulerBean;
import com.gip.xyna.xprc.xsched.XynaScheduler;



public class XynaProcessCtrlExecution extends Section {

  public static final String DEFAULT_NAME = "Xyna Process, Control & Execution";
  public static final Logger logger = CentralFactoryLogging.getLogger(XynaProcessCtrlExecution.class);

  static {
    try {
      addDependencies(XynaProcessCtrlExecution.class, new ArrayList<XynaFactoryPath>(Arrays
                      .asList(new XynaFactoryPath[] {
                                      new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryManagementODS.class,
                                                          Configuration.class),
                                      new XynaFactoryPath(XynaProcessing.class, XynaFractalWorkflowEngine.class,
                                                          DeploymentHandling.class)})));
      // WorkflowDatabase darf erst nach der klasse geladen werden die einen deploymenthandler definiert! siehe
      // WorkflowDatabase
    }
    catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("", t);
    }
  }


  public XynaProcessCtrlExecution() throws XynaException {
    super();
  }


  private XynaPlanning xynaPlanning;
  private XynaScheduler xynaScheduler;
  private XynaExecution xynaExecution;
  private XynaCleanup xynaCleanup;
  private StatusChangeProvider statusChangeProvider;
  private OrderExecutionTimeoutManagement orderExecutionTimeoutManagement;
  private MonitoringDispatcher monitoringDispatcher;
  private SuspendResumeManagement suspendResumeManagement;
  private OrderInputSourceManagement oigm;
  private ParameterInheritanceManagement parameterInheritanceManagement;
  private TransactionManagement transactionManagement;

  @Override
  public void init() throws XynaException {

    statusChangeProvider = new StatusChangeProvider();
    deployFunctionGroup(statusChangeProvider);

    // Deploy the standard master workflow components
    xynaPlanning = new XynaPlanning();
    deployFunctionGroup(xynaPlanning);

    xynaExecution = new XynaExecution();
    deployFunctionGroup(xynaExecution);

    xynaCleanup = new XynaCleanup();
    deployFunctionGroup(xynaCleanup);

    // Deploy the startup functionality
    deployFunctionGroup(new Startup());
    
    suspendResumeManagement = new SuspendResumeManagement();
    //deployFunctionGroup(suspendResumeManagement); FIXME FunctionGroup?

    // TODO: monitoring function group?
    monitoringDispatcher = new MonitoringDispatcher();
    deployFunctionGroup(monitoringDispatcher);
    
    orderExecutionTimeoutManagement = new OrderExecutionTimeoutManagement();
    deployFunctionGroup(orderExecutionTimeoutManagement);
    
    parameterInheritanceManagement = new ParameterInheritanceManagement();
    deployFunctionGroup(parameterInheritanceManagement);
    
    transactionManagement = new TransactionManagement();
    deployFunctionGroup(transactionManagement);

    XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.TIMEOUT_SUSPENSION.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.ORDERABORTION_COMPENSATE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.XYNA_ORDER_STARTUP_MODE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    triggerStopTimeout.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask(XynaProcessCtrlExecution.class,"XynaProcessCtrlExecution.init").
      after(DeploymentHandling.class).
      execAsync(new Runnable() { public void run() { initDeploymentHandler(); }});
    
    
    fExec.addTask("XynaProcessCtrlExecution.initVar", "XynaProcessCtrlExecution.initVar").
      after(OrderInputSourceManagement.class, XynaProcessCtrlExecution.class).
      before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
      execAsync(new Runnable() { public void run() {
        oigm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
        }});
  }
  
  private void initDeploymentHandler() {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                    .addDeploymentHandler(DeploymentHandling.PRIORITY_XPRC, new DeploymentHandler() {

                      public void exec(GenerationBase object, DeploymentMode mode) {
                        if (object instanceof WF) {
                          WF wf = (WF) object;
                          // default destinations einrichten
                          DestinationValue dv = new FractalWorkflowDestination(object.getFqClassName());
                          
                          RuntimeContext runtimeContext;
                          try {
                            runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                                          .getRevisionManagement().getRuntimeContext(object.getRevision());
                          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
                            logger.warn("Could not find application name and version name for revision " + object.getRevision(), e1);
                            return;
                          }
                          DestinationKey dk = new DestinationKey(GenerationBase.getDefaultOrdertype(object), runtimeContext);

                          // destinations m�ssen nicht in file gespeichert werden, da beim serverneustart
                          // deployment erneut aufgerufen wird.
                          if (Objects.equals(wf.getOutputTypeFullyQualified(), SchedulerBean.class.getName())) {

                            getXynaPlanning().getPlanningDispatcher().setDestination(dk, dv, true);
                            getXynaExecution().getExecutionEngineDispatcher()
                                            .setDestination(dk, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW, false);
                          }
                          else {
                            getXynaPlanning().getPlanningDispatcher()
                                              .setDestination(dk, XynaDispatcher.DESTINATION_DEFAULT_PLANNING, false);
                            getXynaExecution().getExecutionEngineDispatcher().setDestination(dk, dv, true);
                          }
                          getXynaCleanup().getCleanupEngineDispatcher()
                                          .setDestination(dk, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW, false);
                          
                          //bestehende customdestinations m�ssen wieder aktiviert werden
                          getXynaPlanning().getPlanningDispatcher().activateCustomDestinations(dv);
                          getXynaExecution().getExecutionEngineDispatcher().activateCustomDestinations(dv);
                          getXynaCleanup().getCleanupEngineDispatcher().activateCustomDestinations(dv);
                        }
                      }

                      public void finish(boolean success) throws XPRC_DeploymentHandlerException {
                      }

                      @Override
                      public void begin() throws XPRC_DeploymentHandlerException {
                      }


                    });
    
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
    .addUndeploymentHandler(DeploymentHandling.PRIORITY_EXCHANGE_ADDITIONAL_LIBS,
                            new UndeploymentHandler() {
      
                              public void exec(GenerationBase object) {
                                if (object instanceof WF) {
                                  RuntimeContext runtimeContext;
                                  try {
                                    runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                                                    .getRevisionManagement().getRuntimeContext(object.getRevision());
                                  } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
                                    logger.warn("Could not find application name and version name for revision " + object.getRevision(), e1);
                                    return;
                                  }
                                  DestinationKey dk = new DestinationKey(GenerationBase.getDefaultOrdertype(object), runtimeContext);
                                  try {
                                    for (XynaDispatcher disp : new XynaDispatcher[]{getXynaExecution().getExecutionEngineDispatcher(),
                                                                                    getXynaPlanning().getPlanningDispatcher(), 
                                                                                    getXynaCleanup().getCleanupEngineDispatcher()}) {
                                      DestinationValue dv = disp.getDestination(dk);
                                      if (dv.resolveRevision(dk).equals(object.getRevision()) && dv.getFQName().equals(object.getFqClassName())) {
                                        //nicht umkonfigurierten default-ordertype entfernen
                                        disp.removeCustomDestination(dk, dv);
                                      }
                                    }
                                  } catch (XPRC_DESTINATION_NOT_FOUND e) {
                                    //ok dann ist nichts zu tun
                                  } catch (PersistenceLayerException e) {
                                    logger.warn("Could not remove destination for ordertype " + GenerationBase.getDefaultOrdertype(object), e);
                                  }
                                }
                              }

                              public void exec(FilterInstanceStorable object) {
                              }

                              public void exec(TriggerInstanceStorable object) {
                              }

                              public void exec(Capacity object) {
                              }

                              public void exec(DestinationKey object) {
                              }

                              public void finish() throws XPRC_UnDeploymentHandlerException {
                              }

                              public boolean executeForReservedServerObjects(){
                                return false;
                              }

                              public void exec(FilterStorable object) {
                              }

                              public void exec(TriggerStorable object) {
                              }
                            });
  }


  @Override
  public void shutdown() throws XynaException {
    stopTriggers();
    super.shutdown();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public XynaPlanning getXynaPlanning() {
    return xynaPlanning;
  }


  public XynaScheduler getXynaScheduler() {
    if ( xynaScheduler == null ) {
      xynaScheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler();
    }
    
    return xynaScheduler;
  }


  public XynaExecution getXynaExecution() {
    return xynaExecution;
  }


  public XynaCleanup getXynaCleanup() {
    return xynaCleanup;
  }


  public StatusChangeProvider getStatusChangeProvider() {
    return statusChangeProvider;
  }


  public MonitoringDispatcher getMonitoringDispatcher() {
    return monitoringDispatcher;
  }
  
  
  public OrderExecutionTimeoutManagement getOrderExecutionTimeoutManagement() {
    return orderExecutionTimeoutManagement;
  }
  
  
  public SuspendResumeManagement getSuspendResumeManagement() {
    return suspendResumeManagement;
  }

  
  public ParameterInheritanceManagement getParameterInheritanceManagement() {
    return parameterInheritanceManagement;
  }
  
  
  public TransactionManagement getTransactionManagement() {
    return transactionManagement;
  }
  
  /**
   * The "real" startOrder function
   */
  private void startOrder(XynaOrderServerExtension xo) {
    final boolean createLoggingContextForThis = logXynaOrderId(xo);
    DeploymentManagement deploymentMgmt = DeploymentManagement.getInstance();
    try {
      deploymentMgmt.countOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());      
      //ACHTUNG: hier darf man kein einfaches try-finally verwenden, um den counter wieder runterzuz�hlen, falls der auftrag (asynchron) gescheduled wird!
      //ansonsten kann es passieren, dass der executionthread bereits l�uft und dann im orderfilter die deploymentid umgesetzt wird, bevor hier 
      //finally ausgef�hrt wird. f�r die f�lle, wo das scheduling nicht passiert, braucht man das finally aber.
      xo.setDeploymentCounterMustBeCountDown();
      try {
        new MasterWorkflowPreScheduler(xo, this).startOrder();
      } finally {
        if (!xo.isDeploymentCounterCountDownDone()) {
          deploymentMgmt.countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
          xo.setDeploymentCounterCountDownDone();
        } //else: bereits runtergez�hlt. siehe XynaScheduler.addOrderIntoAllOrdersEtc und XynaOrderExecutor.cancelOrder      
        
      }
    } finally {
      if (createLoggingContextForThis) {
        NDC.pop();
      }
    }
  }


  /**
   * Asynchronously inserts an order without registering a custom response listener, that is
   * "start it and forget about it"
   * @throws XynaException falls inputsource behandlung fehler wirft oder auftrag fehler an responselistener meldet (in planning oder execution)
   */
  public Long startOrder(XynaOrderCreationParameter xocp) throws XynaException {
    CountDownLatch latch = new CountDownLatch(1);
    SynchronousResponseListener rl = new SynchronousResponseListener(latch, null);
    Long id = startOrder(xocp, rl);
    
    //checken, ob es exceptions gab
    rl.handleAfterAwait();
    XynaException[] exs = rl.getXynaExceptions();
    if (exs != null && exs.length > 0) {
      if (exs.length == 1) {
        throw exs[0];
      } else {
        throw new XPRC_UNEXPECTED_ERROR_PROCESS(xocp.getDestinationKey().getOrderType(), "Multiple errors ocurred.").initCauses(exs);
      }
    }
    
    return id;
  }

  
  private void notifyInputSource(XynaOrderCreationParameter xocp, XynaOrderServerExtension xo) throws XynaException {
    long inputGenId = xocp.getOrderInputSourceId();
    if (inputGenId > 0) {
      oigm.notifyInputSource(inputGenId, xo);
    }
  }

  /**
   * erstellt ordercontext (der die order enth�lt) und notifiziert inputsource (falls n�tig)
   * @param orderId optional: -1 =&gt; es wird neue id generiert.
   */
  public OrderContextServerExtension createAndPrepareOrderAndContext(XynaOrderCreationParameter xocp, long orderId) throws XynaException {
    XynaOrderServerExtension xo;
    if (orderId == -1) {
      xo = new XynaOrderServerExtension(xocp);
    } else {
      xo = new XynaOrderServerExtension(xocp, orderId);
    }
    notifyInputSource(xocp, xo);

    OrderContextServerExtension ctx = new OrderContextServerExtension(xo);
    if (xocp.getAcknowledgableObject() != null) {
      ctx.set(OrderContextServerExtension.ACKNOWLEDGABLE_OBJECT_KEY, xocp.getAcknowledgableObject());
    }
    if (xocp.getTransientCreationRole() != null) {
      ctx.set(OrderContextServerExtension.CREATION_ROLE_KEY, xocp.getTransientCreationRole());
    }

    return ctx;
  }

  /**
   * Asynchronously inserts an order with registering a custom response listener
   * @throws XynaException falls inputsource behandlung fehler wirft
   */
  public Long startOrder(XynaOrderCreationParameter xocp, ResponseListener responseListener) throws XynaException {
    OrderContextServerExtension ctx = createAndPrepareOrderAndContext(xocp, -1);
    XynaOrderServerExtension xo = ctx.xo;
    startOrder(xo, responseListener, ctx);

    return xo.getId();
  }


  public static class EmptyResponseListener extends ResponseListener {

    private static final long serialVersionUID = 366878255392869200L;


    public void onError(XynaException[] e, OrderContext ctx) {
    }


    public void onResponse(GeneralXynaObject response, OrderContext ctx) {
    }

  }


  /**
   * Asynchronously inserts an order, registering a customized ResponseListener achtung: Es wird kein eigener thread
   * aufgemacht, d.h. dieser thread kann u.U. zu einem scheduler- thread mutieren oder solange laufen bis dieser auftrag
   * fertig ist.
   */
  public void startOrder(XynaOrderServerExtension xo, ResponseListener rl, OrderContext ctx) {
    xo.setOrderContext(ctx);
    xo.setResponseListener(rl);
    startOrder(xo);
  }


  /**
   * Synchronously inserts an order, thereby building a XynaOrder and calling startOrderSynchronous(XynaOrder xo)
   */
  public GeneralXynaObject startOrderSynchronously(XynaOrderCreationParameter xocp) throws XynaException {
    return startOrderSynchronouslyAndReturnOrder(xocp).getOutputPayload();
  }

  public XynaOrderServerExtension startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp)
                  throws XynaException {
    OrderContextServerExtension ctx = createAndPrepareOrderAndContext(xocp, -1);
    XynaOrderServerExtension xo = ctx.xo;
    xo.setOrderContext(ctx);
    return startOrderSynchronous(xo, false, true);
  }

  // WebService is using this to return an orderId & OutputPayload
  public OrderExecutionResponse startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp, ResultController resultController) {
    long orderId =  XynaFactory.getInstance().getIDGenerator().getUniqueId();
    try {
      OrderContextServerExtension ctx = createAndPrepareOrderAndContext(xocp, orderId);
      XynaOrderServerExtension xo = ctx.xo;
      xo.setOrderContext(ctx);
      startOrderSynchronous(xo, false, true);
      
      return new SynchronousSuccesfullOrderExecutionResponse(xo.getOutputPayload(), xo.getId(), resultController);
    } catch (Throwable t) {
      ErroneousOrderExecutionResponse errResp = new ErroneousOrderExecutionResponse(t, resultController);
      errResp.setOrderId(orderId);
      return errResp;
    }
  }


  /**
   * Synchronously inserts an order, thereby building a XynaOrder and calling startOrderSynchronous(XynaOrder xo)
   */
  public XynaOrderServerExtension startOrderSynchronous(final XynaOrderServerExtension xo) throws XynaException {
    return startOrderSynchronous(xo, false, true);
  }

  /**
   * Synchronously "really" inserts an order based on a XynaOrder. is called by generated workflow-code
   * @param xo
   * @param onlyPushToScheduler Wenn true: direkt an Scheduler unter Umgehung des PreScheduler
   * @return
   * @throws XynaException
   */
  public XynaOrderServerExtension startOrderSynchronous(final XynaOrderServerExtension xo, boolean onlyPushToScheduler)
                  throws XynaException {
    return startOrderSynchronous(xo, onlyPushToScheduler, true);
  }
  
  /**
   * wird vom generierten code aus aufgerufen
   */
  public XynaOrderServerExtension compensateOrderSynchronously(final XynaOrderServerExtension xo) throws XynaException {
    xo.getDestinationKey().setCompensate(true);
    return startOrderSynchronous(xo, true, false);
  }

  public interface SynchronousResponseListenerForXpce {

    /**
     * 
     */
    void handleInterruptionWhileWaitingForLatch();

    /**
     */
    void handleAfterAwait();

    /**
     * @return
     */
    GeneralXynaObject getResponse();

    /**
     * @return
     */
    XynaException[] getXynaExceptions();

    
    /**
     * Wurde ein onResponse mit einem Null-Result aufgerufen?
     * @return
     */
    boolean hasNullResponse();
    
  }
  
  /**
   * Synchronously "really" inserts an order based on a XynaOrder. 
   * Called from {@link #startOrderSynchronous(XynaOrderServerExtension, boolean)}
   * or  {@link #compensateOrderSynchronously(XynaOrderServerExtension)}
   * @param xo
   * @param onlyPushToScheduler Wenn true: direkt an Scheduler unter Umgehung des PreScheduler
   * @param setMonitoringLevel Wenn true und onlyPushToScheduler true: MonitoringLevel setzen 
   * @return
   * @throws XynaException
   * @throws ProcessSuspendedException
   */
  private XynaOrderServerExtension startOrderSynchronous(XynaOrderServerExtension xo, boolean onlyPushToScheduler, boolean setMonitoringLevel)
                  throws XynaException, ProcessSuspendedException {
    
    //Latch, um auf Ende der Ausf�hrung zu warten
    final CountDownLatch latch = new CountDownLatch(1);
   
    //ResponseListener-Implementierung unterschiedlich je nach Grund der synchronen Ausf�hrung 
    SynchronousResponseListenerForXpce xpceRL = null;
    if( xo.hasParentOrder() ) { //wird zur Unterscheidung "Subworkflow"/"extern synchron eingestellt" verwendet
      SubworkflowResponseListener rl = new SubworkflowResponseListener(latch, xo);
      xo.setResponseListener(rl);
      xpceRL = rl;
    } else {
      SynchronousResponseListener rl = new SynchronousResponseListener(latch, xo);
      xo.setResponseListener(rl);
      xpceRL = rl;
    }
    
    //Ausf�hrung
    executeOrder(xo, onlyPushToScheduler, setMonitoringLevel);

    //Warten auf Beendigung
    try {
      latch.await();
    } catch (InterruptedException e) {
      if (logger.isDebugEnabled()) {
        logger.debug(Thread.currentThread() + " got interrupted while waiting for countdown latch.");
      }
      xpceRL.handleInterruptionWhileWaitingForLatch();
      errorHandlingWhenInterruptedException(e, xo, xpceRL.getXynaExceptions() );
    }
    
    //Spezialbehandlung des ResponseListeners wirft evtl. Exceptions
    xpceRL.handleAfterAwait();
    
    //R�ckgabe der erfolgreichen XynaOrder ...
    GeneralXynaObject response = xpceRL.getResponse();
    if( response != null ) {
      xo.setOutputPayload(response);
      return xo;
    } else {
      if( xpceRL.hasNullResponse() ) {
        return xo;
      }
    }
    
    //... oder Werfen der Exceptions, falls Aufruf nicht erfolgreich war
    XynaException[] exceptions = xpceRL.getXynaExceptions();
    if( exceptions == null || exceptions.length == 0 ) {
      throw new XPRC_HANGING_PROCESS(Long.toString(xo.getId()));
    } else {
      if (exceptions.length == 1) {
        throw exceptions[0];
      } else {
        throw new XPRC_UNEXPECTED_ERROR_PROCESS(xo.getDestinationKey().getOrderType(), "Multiple errors ocurred.").initCauses(exceptions);
      }
    }
  }

  private void errorHandlingWhenInterruptedException(Exception e, XynaOrderServerExtension xo, XynaException[] xynaExceptions) {
    if (xynaExceptions != null && xynaExceptions.length > 0) {
      Throwable[] ts = new Throwable[xynaExceptions.length + 1];
      for (int i = 0; i < xynaExceptions.length; i++) {
        ts[i] = xynaExceptions[i];
      }
      ts[ts.length - 1] = e;
      throw new RuntimeException(new XPRC_UNEXPECTED_ERROR_PROCESS(xo.getDestinationKey().getOrderType(), e
                                                                   .getClass().getSimpleName() + " " + e.getMessage()).initCauses(ts));
    }
    else {
      // FIXME: fehlermeldung sollte klarmachen, dass hier kein fehler im auftrag vorliegt, sondern dieser noch munter
      // am laufen sein kann
      throw new RuntimeException(new XPRC_UNEXPECTED_ERROR_PROCESS(xo.getDestinationKey().getOrderType(), e
                                                                   .getClass().getSimpleName() + " " + e.getMessage(), e));
    }
  }

  

  /**
   * Stellt Auftrag in Scheduler ein, evtl. unter Umgehung des PreScheduler
   * @param xo
   * @param onlyPushToScheduler Wenn true: direkt an Scheduler unter Umgehung des PreScheduler
   * @param setMonitoringLevel Wenn true und onlyPushToScheduler true: MonitoringLevel setzen 
   * @throws XPRC_OrderEntryCouldNotBeAcknowledgedException 
   * @throws XNWH_RetryTransactionException 
   */
  private void executeOrder(XynaOrderServerExtension xo, boolean onlyPushToScheduler, boolean setMonitoringLevel) throws XNWH_RetryTransactionException, XPRC_OrderEntryCouldNotBeAcknowledgedException {
    if ( onlyPushToScheduler ) {
      final boolean createLoggingContextForThis = logXynaOrderId(xo);
      try {
        if( setMonitoringLevel ) {
          try {
            getMonitoringDispatcher().dispatch(xo);
          } catch (XynaException e) {
            // was not able to set the monitoring level, do something? this will result in errors later
            logger.warn("Could not determine monitoring code for id " + xo.getId() + ", using error monitoring", e);
            xo.setMonitoringLevel(MonitoringDispatcher.DEFAULT_MONITORING_LEVEL);
          }
        }
        getXynaScheduler().addOrder(xo, null, false);
      } finally {
        if (createLoggingContextForThis) {
          NDC.pop();
        }
      }
    } else {
      startOrder(xo);
    }
  }

  /**
   * Workflow wird gestartet. Falls gew�nscht, wird die XynaOrderId als 
   * LoggingDiagnosisContext geloggt. 
   * Im Falle des Starts eines Subworkflows besteht der LoggingDiagnosisContext anschlie�end
   * aus zwei Teilen: dem LoggingDiagnosisContext des Parents und der XynaOrderId des Subworkflows.
   * Detached gestartete Workflows werden hier wie Subworkflows behandelt.
   * @param xo
   * @return
   */
  private boolean logXynaOrderId(XynaOrderServerExtension xo) {
    if( ! XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT.get() ) {
      return false;
    }
    NDC.push( String.valueOf(xo.getId()));
    return true;
  }


/**
 * @deprecated Ist durch {@link SynchronousResponseListener} und {@link SubworkflowResponseListener} ersetzt, soll nur 
 * noch zum Deserialisieren existierender Auftr�ge verwendet werden 
 */
  @Deprecated
  private static class CountDownLatchResponseListenerWithSuspensionSupport
      extends
        ResponseListenerWithSuspensionSupport {

    private static final long serialVersionUID = -7324139499947034302L;
    private transient CountDownLatch latch;
    private List<GeneralXynaObject> resp;
    private List<XynaException> err;
    private boolean gotAborted;
    private OrderDeathException orderDeathException = null;
    private boolean needToAbortParentOrder;
    private final XynaOrderServerExtension xo;
    private ProcessAbortedException abortionException;
    private ProcessSuspendedException suspendedException;
    private boolean isSubWf;


    public CountDownLatchResponseListenerWithSuspensionSupport(CountDownLatch latch, XynaOrderServerExtension xo, boolean isSubWf) {
      this.latch = latch;
      this.resp = new ArrayList<GeneralXynaObject>();
      this.err = new ArrayList<XynaException>();
      this.gotAborted = false;
      this.xo = xo;
      this.needToAbortParentOrder = false;
      this.isSubWf = isSubWf;
    }

    public void onResponse(GeneralXynaObject response, OrderContext ctx) {
      resp.add(response);
      latch.countDown();
    }


    public void onError(XynaException[] e, OrderContext ctx) {
      synchronized (err) {
        for (XynaException xe : e) {
          err.add(xe);
        }
      }
      latch.countDown();
    }

    @Override
    public void onSuspended(ProcessSuspendedException suspendedException) {
      if( isSubWf ) {
        this.suspendedException = suspendedException;
        latch.countDown();
      }
    }

    public void onOrderAbortion(ProcessAbortedException e) {
      if (xo == null) {
        //deserialisiert und aborted, bevor wieder der thread gestartet wurde
        //TODO in welchen f�llen kann das genau passieren?
        logger.warn("Order has been aborted. ParentOrder can not be notified.", e);
        return;
      }
      boolean needToAbortParentOrder = e.needsToAbortParentOrderEvenIfNotAborted();
      abortionException = e;

      if (xo.hasParentOrder()) {
        gotAborted = true;
        this.needToAbortParentOrder = needToAbortParentOrder;
        latch.countDown();
      } else {
        onError(new XynaException[] {new XPRC_PROCESS_ABORTED_EXCEPTION(xo.getId(), e.getAbortionCause()
            .getAbortionCauseString(), (Throwable) e)}, xo.getOrderContext());
      }
    }

    /*
     * This ResponseListener (although serializable) shouldn't be persisted. empty read- and write-Methods ensure a
     * nulled Response-Listener for orders using this class This approach is kind of dirty, a deep clone of the order
     * (all childs and selfReferences) that get's these Listeners removed would be preferable
     */
    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
      // s.defaultWriteObject();
    }


    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
      // s.defaultReadObject();
      this.latch = new CountDownLatch(1);
      resp = new ArrayList<GeneralXynaObject>();
      err = new ArrayList<XynaException>();
    }


    @Override
    public void onOrderDeath(OrderDeathException e) {
      this.orderDeathException = e;
      this.latch.countDown();
    }


  }

  
  public void callResponseListener(XynaOrderServerExtension xo, TwoConnectionBean cons) throws XNWH_RetryTransactionException {
    if (logger.isTraceEnabled()) {
      // in tracemodus exceptions gesammelt ausgeben
      if (xo.getErrors() != null && xo.getErrors().length > 0) {
        if (xo.getErrors().length == 1) {
          logger.trace("order " + xo.getId() + " had exception:", xo.getErrors()[0]);
        } else {
          logger.trace("order " + xo.getId() + " had " + xo.getErrors().length + " exceptions:");
          for (int i = 0; i < xo.getErrors().length; i++) {
            logger.trace("Exception " + i + ":");
            logger.trace(null, xo.getErrors()[i]);
          }
        }
      } else {
        logger.trace("order " + xo.getId() + " had no errors.");
      }
    }
    ResponseListener rl = xo.getResponseListener();
    final boolean createLoggingContextForThis = XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT.get();
    
    if (createLoggingContextForThis) {
      NDC.push(Long.toString(xo.getId())); //FIXME doppelt?
    }
    
    boolean usingConnection = false;
    
    CentralFactoryLogging.logOrderTiming(xo.getId(), "call responseListener");
    
    try {
      if (cons != null && cons.getDefaultConnection() != null) {
        usingConnection = true;
        rl.setConnections(cons);
      } else {
        usingConnection = false;
      }

      ResponseListenerResponse response = ResponseListenerResponse.finish();
      if (xo.isAborted()) {
        if (rl instanceof SubworkflowResponseListener) {
          ((SubworkflowResponseListener) rl).onOrderAbortion(xo.getAbortionException());
        } else {
          response = rl.internal_onErrorWithReply(xo);
        }
      } else if (xo.hasError()) {
        response = rl.internal_onErrorWithReply(xo);
      } else {
        response = rl.internal_onResponseWithReply(xo);
      }
      
      switch (response.getHandling()) {
        case CONTINUE:
          if (usingConnection) {
            cons.commitAndCloseIfOpened();
          }
          break;
        case ABORT_ORDER:
          if (usingConnection) {
            cons.closeUncommited();
          }
          if (logger.isDebugEnabled()) {
            logger.debug("Aborted execution of order " + xo, response.getCause());
          }
          break;
        case ARCHIVE_AS_FAILED:
          if (usingConnection) {
  
            if (logger.isDebugEnabled()) {
              logger.debug("ResponseListener ordered order " + xo.getId() + " to be archived as failed", response.getCause());
            }
  
            if (response.getCause().getCause() != null) {
              if (!xo.getErrorsFrom(ProcessingStage.ARCHIVING).contains(response.getCause().getCause())) {
                MasterWorkflowPostScheduler.addThrowableAsXynaException(xo, response.getCause().getCause(), ProcessingStage.OTHER);
              }
            } else {
              MasterWorkflowPostScheduler.addThrowableAsXynaException(xo, response.getCause(), ProcessingStage.OTHER);
            }
  
            try {
              xo.setMonitoringLevel(0); //nicht nach orderarchive default schreiben, nur andere sachen updaten
              XynaFactory.getInstance().getProcessing().getOrderStatus().changeErrorStatus(xo, OrderInstanceStatus.XYNA_ERROR);
            } catch (Throwable t) {
              Department.handleThrowable(t);
              logger.error("Uncaught exception during status change of order " + xo, t);
            }
            
            try {
              //status updaten
              OrderInstanceDetails oid = cons.getRootOID();
              oid.setStatus(OrderInstanceStatus.XYNA_ERROR);
              oid.setLastUpdate(System.currentTimeMillis());
              oid.setExceptions(xo.getErrors());
    
              cons.getHistoryConnection().persistObject(oid);
            } catch (Throwable tt) {
              Department.handleThrowable(tt);
              logger.warn("Could not update order " + xo.getId() + " in OrderArchive HISTORY.", tt);
            }
            try {
              cons.commitAndCloseIfOpened();
              if (logger.isDebugEnabled()) {
                logger.debug("Archived order " + xo + " as failed.");
              }
            } catch (Throwable tt) {
              Department.handleThrowable(tt);
              cons.closeUncommited();
              logger.error("Uncaught exception during archiving of order " + xo, tt);
            }
          } else {
            logger.error("Aborted execution of order " + xo, response.getCause());
          }
        break;
      }
    } catch (XNWH_RetryTransactionException ctcbe) {
      if (usingConnection) {
        cons.closeUncommited();
        throw ctcbe; //retry oben dr�ber
      }
    } catch (Throwable t) {
      if (usingConnection) {
        try {
          cons.commitAndCloseIfOpened();
        } catch (XNWH_RetryTransactionException e) {
          cons.closeUncommited();
          throw e;
        } catch (PersistenceLayerException ple) {
          cons.closeUncommited();
          logger.error("Was unable to commit!", ple);
        }
      }
      Department.handleThrowable(t);
      logger.error("Uncaught exception during response listener notification for order " + xo, t);
    } finally {
      try {
        xo.cleanup();
      } finally {
        try {
          if (createLoggingContextForThis) {
            NDC.pop();
          }
        } catch (Throwable t) {
          Department.handleThrowable(t);
          logger.warn("Failed to clear NDC context", t);
        }
      }
    }
  }


  // ======================================= EVENTLISTENER HANDLING ======================

  public static final int FUTUREEXECUTIONID_TRIGGERSTART = XynaFactory.getInstance().getFutureExecution().nextId();

  
  public void registerEventListener(EventListenerInstance eli) throws XACT_TriggerCouldNotBeStartedException {
    registerEventListener(eli, -1);
  }
  
  public void registerEventListener(EventListenerInstance eli, final int processingLimit) throws XACT_TriggerCouldNotBeStartedException {

    long revision = eli.getRevision();
    // if there is an entry and the thread is not running, then remove it
    if (getOrCreateEventListenerThreadMap(revision).containsKey(eli.getInstanceName())
        && !(getOrCreateEventListenerThreadMap(revision).get(eli.getInstanceName()).isRunning())) {
      removeEventListenerThread(revision, eli.getInstanceName());
    }

    // if there is no entry
    if (!(getOrCreateEventListenerThreadMap(revision).containsKey(eli.getInstanceName()))) {
      eli.getEL().start(eli.getStartParameter());
      // neuen Thread aufmachen, der hier auf Events horcht.
      final EventListenerThread t = new EventListenerThread(eli, processingLimit);

      FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
      
      fExec.addTask("XPCE_"+eli.getInstanceName(), "XynaProcessCtrlExecution.registerEventListener-"+eli.getInstanceName()+"-"+eli.getRevision()).
            after(XynaActivationTrigger.FUTUREEXECUTION_DEPLOYTRIGGER_ID).
            execNowOrAsync( new Runnable(){ public void run() { t.start(); }} );
      
      addEventListenerThread(revision, eli.getInstanceName(), t);
    } else {
      logger.warn("Tried to start a new event listener thread for a trigger instance that already has a thread attached to it!");
    }
  }

  
  public void registerEventListenerNoThreadHandling(EventListener el, StartParameter sp)
      throws XACT_TriggerCouldNotBeStartedException {
    el.start(sp);
  }


  private static class RemovalMap<T,S> extends ObjectWithRemovalSupport {
    
    private final Map<T, S> map = new ConcurrentHashMap<T, S>();
    
    @Override
    protected boolean shouldBeDeleted() {
      return map.isEmpty();
    }
  }

  private static class EventListenerThreadMap<K> extends ConcurrentMapWithObjectRemovalSupport<K, RemovalMap<String, EventListenerThread>> {

    private static final long serialVersionUID = 1L;

    @Override
    public RemovalMap<String, EventListenerThread> createValue(K key) {
      return new RemovalMap<String, EventListenerThread>();
    }

  }
  
  
  private ConcurrentMapWithObjectRemovalSupport<Long, RemovalMap<String, EventListenerThread>> eventListenerThreads = new EventListenerThreadMap<Long>();

  private Map<String, EventListenerThread> getOrCreateEventListenerThreadMap(Long revision) {
    RemovalMap<String, EventListenerThread> result = eventListenerThreads.lazyCreateGet(revision);
    eventListenerThreads.cleanup(revision);

    return result.map;
  }
  
  private void addEventListenerThread(Long revision, String instanceName, EventListenerThread elt) {
    RemovalMap<String, EventListenerThread> result = eventListenerThreads.lazyCreateGet(revision);
    try{
      result.map.put(instanceName, elt);
    } finally {
      eventListenerThreads.cleanup(revision);
    }
  }

  private EventListenerThread removeEventListenerThread(Long revision, String instanceName) {
    RemovalMap<String, EventListenerThread> result = eventListenerThreads.lazyCreateGet(revision);
    try{
      return result.map.remove(instanceName);
    } finally {
      eventListenerThreads.cleanup(revision);
    }
  }

  private static final XynaPropertyDuration triggerStopTimeout = new XynaPropertyDuration("xact.trigger.stop.timeout", new Duration(25000L));
  static {
    triggerStopTimeout.setDefaultDocumentation(DocumentationLanguage.EN, "Maximum time to wait for each triggerinstance that is to be stopped.");
  }
  
  public void unregisterEventListener(final String name, final Long revision) {
    final EventListenerThread elt = removeEventListenerThread(revision, name);
    if (elt != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("stopping trigger " + name);
      }

      elt.setRunning(false);

      ExecutionWrapper<XynaException> wrapper = new ExecutionWrapper<XynaException>("stop trigger " + name) {

        @Override
        public void execute() {
          try {
            elt.getEventListener().getEL().stop();
            if (logger.isDebugEnabled()) {
              logger.debug("event listener thread has been stopped");
            }
          } catch (Throwable t) {
            Department.handleThrowable(t);
            String runtimeContext = "";
            if (!RevisionManagement.REVISION_DEFAULT_WORKSPACE.equals(revision)) {
              RuntimeContext rc;
              try {
                rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                        .getRuntimeContext(revision);
              } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
                rc = new Application("unknown", "rev=" + revision);
              }
              runtimeContext = " in " + rc;
            }
            logger.error("trigger instance " + name + " could not be stopped successfully" + runtimeContext + ".", t);
          }
        }

      };
      //TODO wie beim service-deployment den trigger �ber timeout und reaktion bei timeout entscheiden lassen
      try {
        XynaExecutor.getInstance().executeRunnableWithUnDeploymentThreadpool(wrapper);
        if (!wrapper.await(triggerStopTimeout.get().getDurationInMillis())) {
          Thread t = wrapper.getThread();
          if (t != null) {
            Exception e = new Exception("Thread " + t.getName() + " hangs.");
            e.setStackTrace(t.getStackTrace());
            logger.warn("Timeout waiting for triggerinstance " + name + ".", e);
            ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
            long blockerId = tbean.getThreadInfo(t.getId()).getLockOwnerId();
            if (blockerId != -1) {
              ThreadInfo ti = tbean.getThreadInfo(blockerId);
              if (ti != null) {
                e = new Exception("Thread " + ti.getThreadName() + " blocks thread " + t.getName());
                e.setStackTrace(ti.getStackTrace());
                logger.warn("Blocking thread", e);
              }
            }
          }
        }
      } catch (RejectedExecutionException e) {
        logger.warn("could not stop trigger " + name + ", no thread available.", e);
      }
    }
  }


  public List<String> getAllEventListenerNames(Long revision) {
    return new ArrayList<String>(getOrCreateEventListenerThreadMap(revision).keySet());
  }


  /**
   * falls es keine EL instanz mit diesem namen gibt, wird null zur�ckgegeben.
   * @param nameOfTriggerInstance
   */
  public EventListenerInstance getEventListenerByName(String nameOfTriggerInstance, Long revision) {
    Map<String, EventListenerThread> mapELT = getOrCreateEventListenerThreadMap(revision);
    EventListenerThread elt = mapELT.get(nameOfTriggerInstance);
    if (elt != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Found EventListenerInstance " + elt.getEventListener() + " by thread for revision <" + revision
            + ">");
      }
      return elt.getEventListener();
    } else {
      logger.debug("No EventListenerInstance found for trigger instance name <" + nameOfTriggerInstance
          + "> in revision <" + revision + ">");
      return null;
    }
  }


  /**
   * Stoppt alle trigger
   */
  public void stopTriggers() throws XACT_TriggerCouldNotBeStoppedException {
    ExecutorService triggerStopExecutor = Executors.newFixedThreadPool(10);
    ((ThreadPoolExecutor)triggerStopExecutor).setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    try {
      ExceptionGatheringFutureCollection<Void> triggerStopResults = new ExceptionGatheringFutureCollection<Void>();
      for (final Long revision : eventListenerThreads.keySet()) {
        for (final String name : getAllEventListenerNames(revision)) {
          triggerStopResults.add(triggerStopExecutor.submit(new Callable<Void>() {
            public Void call() throws Exception {
              try {
                EventListenerThread elt = eventListenerThreads.get(revision).map.get(name);
                if (elt.isProcessingLimited()) {
                  XynaFactory.getInstance().getActivation().getActivationTrigger().disableTriggerInstance(name, revision, true);
                }
              } finally {
                unregisterEventListener(name, revision);
              }
              return null;
            }
          }));
        }
      }
      try {
        triggerStopResults.getWithExceptionGathering();
      } catch (GatheredException e) {
        XACT_TriggerCouldNotBeStoppedException exception = new XACT_TriggerCouldNotBeStoppedException(new String[] {"Not all triggers could be succesfully stopped."}, e) {};
        exception.initCauses(e.getCauses().toArray(new Throwable[e.getCauses().size()]));
        throw exception;
      }
    } finally {
      triggerStopExecutor.shutdownNow();
    }
  }
  
  
  //will rollback the archive of the order to let another node take over from last checkpoint
 public static class ResponseOfOrderFailedAbortOrder extends RuntimeException {
   
   private static final long serialVersionUID = -2769967427772347950L;
   
   public ResponseOfOrderFailedAbortOrder(String message) {
     super(message);
   }

   public ResponseOfOrderFailedAbortOrder(Throwable cause) {
     super(cause);
   }
   
 }
 
 
 //will append this exception or it's cause to Order and persist it as failed
 public static class ResponseOfOrderFailedArchiveAsFailed extends RuntimeException {
   
   private static final long serialVersionUID = -7107913524462112826L;

   public ResponseOfOrderFailedArchiveAsFailed(String message) {
     super(message);
   }
   
   public ResponseOfOrderFailedArchiveAsFailed(Throwable cause) {
     super(cause);
   }
   
 }
  
}
