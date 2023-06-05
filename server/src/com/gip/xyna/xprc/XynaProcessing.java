/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xprc;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaRuntimeException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCapacityCardinality;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.ResultController;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
import com.gip.xyna.xprc.exceptions.XPRC_CancelFailedException;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderStorageException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.exceptions.XPRC_CronRemovalException;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCronLikeOrderParametersException;
import com.gip.xyna.xprc.exceptions.XPRC_SuspendFailedException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.remotecallserialization.XynaXmomSerialization;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSearchResult;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSelectImpl;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xfqctrl.XynaFrequencyControl;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xpce.InterruptableExecutionProcessor;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.WorkflowEngine;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement.ManualInteractionProcessingRejectionState;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringDispatcher;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderStartupAndMigrationManagement;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowInformation;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.ClusteredScheduler;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.gip.xyna.xprc.xsched.vetos.AdministrativeVeto;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;



public class XynaProcessing extends XynaProcessingBase {

  public static final String DEFAULT_NAME = "Xyna Processing";

  /**
   * @deprecated use FUTUREEXECUTIONID_ORDER_EXECUTION
   */
  @Deprecated
  public static final int FUTUREEXECUTIONID_STARTPERSISTEDORDERS = XynaFactory.getInstance().getFutureExecution().nextId();
  
  //TODO wäre sinnvoll? //public static final String FUTUREEXECUTIONID_ORDER_ENTRANCE = "OrderEntrance";
  public static final String FUTUREEXECUTIONID_ORDER_EXECUTION = "OrderExecution\npossible";
  
  private WorkflowEngine workflowEngine;
  private XynaProcessCtrlExecution xpce;
  private XynaXmomSerialization remotecallSerialization;
  private XynaScheduler scheduler;
  private XynaProcessingODS xprcods;
  private OrderStatus orderStatus;
  private XynaFrequencyControl frequencyControl;
  private BatchProcessManagement batchProcessManagement;


  public XynaProcessing() throws XynaException {
    super();
  }


  /**
   * General Xyna Section stuff
   */
  @Override
  public void init() throws XynaException {

    // deploy sections
    xpce = new XynaProcessCtrlExecution();
    deploySection(xpce);
    scheduler = new ClusteredScheduler();
    deploySection(scheduler);
    frequencyControl = new XynaFrequencyControl();
    deploySection(frequencyControl);
    batchProcessManagement = new BatchProcessManagement();
    deploySection(batchProcessManagement);

    // create a workflow engine and deploy it
    workflowEngine = new XynaFractalWorkflowEngine();
    try {
      deploySection((Section) workflowEngine);
    } catch (ClassCastException e) {
      throw new RuntimeException("workflow engine invalid.", e);
    }

    // deploy Xyna Processing ODS
    // this has to be done after the workflow engine has been deployed because
    // previously stored workflows are deployed here
    // TODO load the workflows with a separate call so that the deployment becomes independent
    xprcods = new XynaProcessingODS();
    deploySection(xprcods);

    orderStatus = new OrderStatus();
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addMetaTask(FUTUREEXECUTIONID_ORDER_EXECUTION, FUTUREEXECUTIONID_ORDER_EXECUTION).
      after(XynaClusteringServicesManagement.class).
      after( WorkflowDatabase.FUTURE_EXECUTION_ID ).
      execAsync();
    
    fExec.addTask(XynaExecutor.class, XynaExecutor.class.getSimpleName()).
      after(XynaProperty.class).
      execAsync(new Runnable() { public void run() {XynaExecutor.getInstance(); }});//initialisierung
    
    fExec.addTask(OrderStartupAndMigrationManagement.class, "OrderStartupAndMigrationManagement").
      after(XynaClusteringServicesManagement.class).
      execAsync(new Runnable() {public void run() { initOrderStartupAndMigrationManagement(); }});
  
    fExec.addTask("startPersistedOrders", "startPersistedOrders" ).
      after(FUTUREEXECUTIONID_ORDER_EXECUTION).
      after(OrderStartupAndMigrationManagement.class).
      execAsync(new Runnable() {public void run() { startPersistedOrders(); }});

    fExec.addTask(FUTUREEXECUTIONID_STARTPERSISTEDORDERS, "deprecated TaskId" ).
      deprecated().
      after(FUTUREEXECUTIONID_ORDER_EXECUTION).
      execAsync();
    
    fExec.addTask("XynaXmomSerialization", "serialization").
      after(OrderStartupAndMigrationManagement.class).
      execAsync(new Runnable() {public void run() {
        try {
          remotecallSerialization = new XynaXmomSerialization();
        } catch (XynaException e) {
          throw new RuntimeException(e);
        }
        deploySection(remotecallSerialization);
      }});
    


  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  /**
   * @return the currently used workflow engine
   */
  @Override
  public WorkflowEngine getWorkflowEngine() {
    return workflowEngine;
  }


  @Override
  public OrderStatus getOrderStatus() {
    return orderStatus;
  }


  @Override
  public XynaProcessCtrlExecution getXynaProcessCtrlExecution() {
    return xpce;
  }


  @Override
  public XynaScheduler getXynaScheduler() {
    return scheduler;
  }


  @Override
  public XynaProcessingODS getXynaProcessingODS() {
    return xprcods;
  }


  @Override
  public XynaFrequencyControl getFrequencyControl() {
    return frequencyControl;
  }


  @Override
  public BatchProcessManagement getBatchProcessManagement() {
    return batchProcessManagement;
  }
  

  @Override
  public XynaXmomSerialization getXmomSerialization() {
    return remotecallSerialization;
  }


  /**
   * remote access to remove a cron like order
   * @throws XPRC_CronRemovalException
   * @throws XPRC_CronLikeOrderStorageException
   */
  public boolean removeCronLikeOrder(Long id) throws XPRC_CronLikeOrderStorageException, XPRC_CronRemovalException {
    logger.debug("remote access to XynaProcessing.removeCronLikeOrder");
    return ((CronLikeScheduler) getSection(XynaScheduler.DEFAULT_NAME).getFunctionGroup(CronLikeScheduler.DEFAULT_NAME))
        .removeCronLikeOrderWithRetryIfConnectionIsBroken(id);
  }


  /**
   * remote access to start a cron like order
   * @throws XPRC_CronLikeSchedulerException
   */
  public CronLikeOrder startCronLikeOrder(CronLikeOrderCreationParameter clocp) throws XPRC_CronLikeSchedulerException {
    logger.debug("remote access to XynaProcessing.startCronLikeOrder");
    try {
      return getXynaScheduler().getCronLikeScheduler().createCronLikeOrder(clocp, null);
    } catch (XNWH_RetryTransactionException e) {
      throw new XPRC_CronLikeSchedulerException(e);
    }
  }


  /**
   * remote access to modify a cron like order
   * @throws XPRC_CronLikeSchedulerException
   * @throws XPRC_InvalidCronLikeOrderParametersException 
   */
  @Deprecated
  public CronLikeOrder modifyCronLikeOrder(Long id, String label, String ordertype, GeneralXynaObject payload,
                                           Long firstStartupTime, Long interval, Boolean enabled, OnErrorAction onError)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    return modifyCronLikeOrder(id, label, new DestinationKey(ordertype), payload, firstStartupTime, Constants.DEFAULT_TIMEZONE, interval,
                               false, enabled, onError, null, null, null, null);
  }
  
  public CronLikeOrder modifyCronLikeOrder(Long id, String label, DestinationKey destination, GeneralXynaObject payload,
                                           Long firstStartupTime, String timeZoneID, Long interval, Boolean useDST,
                                           Boolean enabled, OnErrorAction onError, String cloCustom0,
                                           String cloCustom1, String cloCustom2, String cloCustom3)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    logger.debug("remote access to XynaProcessing.modifyCronLikeOrder");
    return getXynaScheduler().getCronLikeScheduler().modifyCronLikeOrder(id, label, destination, payload,
                                                                         firstStartupTime, timeZoneID, interval,
                                                                         useDST, enabled, onError, cloCustom0,
                                                                         cloCustom1, cloCustom2, cloCustom3);
  }

  public CronLikeOrder modifyTimeControlledOrder(Long id, CronLikeOrderCreationParameter clocp)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    return getXynaScheduler().getCronLikeScheduler().modifyTimeControlledOrder(id, clocp);
    
  }

  public CronLikeOrder modifyCronLikeOrder(Long id, String label, DestinationKey destination, GeneralXynaObject payload,
                                           Calendar firstStartupTimeWithTimeZone, Long interval, Boolean useDST,
                                           Boolean enabled, OnErrorAction onError, String cloCustom0,
                                           String cloCustom1, String cloCustom2, String cloCustom3)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    return modifyCronLikeOrder(id, label, destination, payload, firstStartupTimeWithTimeZone.getTimeInMillis(),
                               firstStartupTimeWithTimeZone.getTimeZone().getID(), interval, useDST, enabled, onError,
                               cloCustom0, cloCustom1, cloCustom2, cloCustom3);
  }

  
  /**
   * remote access to start an order
   * @throws XynaRuntimeException falls der auftrag nicht gestartet werden konnte
   */
  public Long startOrder(XynaOrderCreationParameter xocp) {
    try {
      return getXynaProcessCtrlExecution().startOrder(xocp);
    } catch (XynaException e) {
      //wegen abwärtskompatibilität wird eine derartige exception in eine runtimeexception gewrapped
      throw new XynaRuntimeException("Could not start order", null, Arrays.asList(new XynaException[] {e}));
    }
  }


  public GeneralXynaObject startOrderSynchronously(XynaOrderCreationParameter xocp) throws XynaException {
    return getXynaProcessCtrlExecution().startOrderSynchronously(xocp);
  }


  public XynaOrderServerExtension startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp)
      throws XynaException {
    return getXynaProcessCtrlExecution().startOrderSynchronouslyAndReturnOrder(xocp);
  }


  public OrderExecutionResponse startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp, ResultController resultController) {
    return getXynaProcessCtrlExecution().startOrderSynchronouslyAndReturnOrder(xocp, resultController);
  }


  /**
   * Does not wait for the timeout to pass
   */
  public CancelBean cancelOrder(Long id, Long timeout) throws XPRC_CancelFailedException {
    return cancelOrder(id, timeout, false);
  }


  /**
   * Cancel an order
   * @return The cancel bean that was returned by the cancel order
   */
  @Override
  public CancelBean cancelOrder(Long id, Long timeout, boolean waitForTimeout) throws XPRC_CancelFailedException {

    // falls länger warten als die uhr zählen kann, warte für bis 5 sek vor uhr-ende
    if (Long.MAX_VALUE - System.currentTimeMillis() < timeout) {
      timeout = Long.MAX_VALUE - System.currentTimeMillis() - 5000;
    }

    final CancelBean bean = new CancelBean();
    bean.setIdToBeCanceled(id);
    bean.setRelativeTimeout(timeout);
    bean.setWaitForTimeout(waitForTimeout);

    try {
      XynaOrderCreationParameter xocp =
          new XynaOrderCreationParameter(XynaDispatcher.DESTINATION_KEY_CANCEL.getOrderType(), Thread.MAX_PRIORITY,
                                         bean);
      xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
      return (CancelBean) getXynaProcessCtrlExecution().startOrderSynchronously(xocp);
    } catch (XynaException e) {
      throw new XPRC_CancelFailedException(Long.toString(id));
    }
  }


  public Collection<CapacityInformation> listCapacityInformation() {
    return getXynaScheduler().getCapacityManagement().listCapacities();
  }


  /**
   * @deprecated use Collection&lt;VetoInformation&gt; listVetoExtendedInformation() 
   */
  @Deprecated
  public Collection<VetoInformationStorable> listVetoInformation() throws PersistenceLayerException {
    return getXynaScheduler().getVetoManagement().listVetosAsStorables();
  }
  
  public Collection<VetoInformation> listVetoExtendedInformation() {
    return getXynaScheduler().getVetoManagement().listVetos();
  }


  public ExtendedCapacityUsageInformation listExtendedCapacityInformation() {
    return getXynaScheduler().getCapacityManagement().getExtendedCapacityUsageInformation();
  }

  public Map<ApplicationEntryType, Map<String, DeploymentStatus>> listDeploymentStatuses(Long revision) {
    return getXynaProcessingODS().getWorkflowDatabase().getAllDeploymentStatuses(revision);
  }

  public List<WorkflowInformation> listWorkflows() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return getXynaProcessingODS().getWorkflowDatabase().listWorkflows();
  }

  @Deprecated
  public void registerSavedWorkflow(String fqNameFromXml) {
    registerSavedWorkflow(fqNameFromXml, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  public void registerSavedWorkflow(String fqNameFromXml, Long revision) {
    getXynaProcessingODS().getWorkflowDatabase().addSaved(fqNameFromXml, revision);
  }


  public void unregisterSavedWorkflow(String fqNameFromXml) {
    getXynaProcessingODS().getWorkflowDatabase().removeSaved(fqNameFromXml, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public boolean addCapacity(String name, int cardinality, CapacityManagement.State state)
      throws XPRC_CAPACITY_ALREADY_DEFINED, PersistenceLayerException {
    return getXynaScheduler().getCapacityManagement().addCapacity(name, cardinality, state);
  }


  public boolean removeCapacity(String name) throws PersistenceLayerException {
    return getXynaScheduler().getCapacityManagement().removeCapacity(name);
  }


  public boolean changeCapacityName(String name, String newName) throws PersistenceLayerException {
    return getXynaScheduler().getCapacityManagement().changeCapacityName(name, newName);
  }


  public boolean changeCapacityState(String name, State newState) throws PersistenceLayerException {
    return getXynaScheduler().getCapacityManagement().changeState(name, newState);
  }


  public boolean changeCapacityCardinality(String name, int cardinality) throws PersistenceLayerException,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    return getXynaScheduler().getCapacityManagement().changeCardinality(name, cardinality);
  }


  @Deprecated
  public boolean requireCapacityForWorkflow(String workflowOriginalFQName, String capacityName, int capacityCardinality)
      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
    List<String> orderTypes = listOrderTypesForWorkflow(workflowOriginalFQName);
    for (String orderType : orderTypes) {
      if (!requireCapacityForOrderType(orderType, capacityName, capacityCardinality)) {
        return false;
      }
    }
    return true;
  }


  @Deprecated
  public boolean removeCapacityForWorkflow(String workflowOriginalFQName, String capacityName)
      throws PersistenceLayerException {
    List<String> orderTypes = listOrderTypesForWorkflow(workflowOriginalFQName);
    for (String orderType : orderTypes) {
      if (!removeCapacityForOrderType(orderType, capacityName)) {
        return false;
      }
    }
    return true;
  }


  public List<String> listOrderTypesForWorkflow(String workflowOriginalFQName) {
    List<String> orderTypes = new ArrayList<String>();
    Map<DestinationKey, DestinationValue> destinations =
        XynaFactory.getInstance().getProcessing().getDestinations(DispatcherIdentification.Execution);

    for (Entry<DestinationKey, DestinationValue> e : destinations.entrySet()) {
      if (e.getValue().getFQName().equals(workflowOriginalFQName)) {
        orderTypes.add(e.getKey().getOrderType());
      }
    }
    return orderTypes;
  }


  public boolean requireCapacityForOrderType(String orderType, String capacityName, int capacityCardinality)
      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
    Capacity newCap = new Capacity(capacityName, capacityCardinality);
    return getXynaProcessingODS().getCapacityMappingDatabase().addCapacity(new DestinationKey(orderType), newCap);
  }
  
  public boolean requireCapacityForOrderType(String orderType, String capacityName, int capacityCardinality, String applicationName, String versionName)
      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
    Capacity newCap = new Capacity(capacityName, capacityCardinality);
    return getXynaProcessingODS().getCapacityMappingDatabase().addCapacity(new DestinationKey(orderType, applicationName, versionName), newCap);
  }

  public boolean requireCapacityForOrderType(String orderType, String capacityName, int capacityCardinality, RuntimeContext runtimeContext)
                  throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
    Capacity newCap = new Capacity(capacityName, capacityCardinality);
    return getXynaProcessingODS().getCapacityMappingDatabase().addCapacity(new DestinationKey(orderType, runtimeContext), newCap);
  }


  public boolean removeCapacityForOrderType(String orderType, String capacityName) throws PersistenceLayerException {
    return getXynaProcessingODS().getCapacityMappingDatabase().removeCapacity(new DestinationKey(orderType),
                                                                              capacityName);
  }
  
  public boolean removeCapacityForOrderType(String orderType, String capacityName, String applicationName, String versionName) throws PersistenceLayerException {
    return getXynaProcessingODS().getCapacityMappingDatabase().removeCapacity(new DestinationKey(orderType, applicationName, versionName),
                                                                              capacityName);
  }

  public boolean removeCapacityForOrderType(String orderType, String capacityName, RuntimeContext runtimeContext) throws PersistenceLayerException {
    return getXynaProcessingODS().getCapacityMappingDatabase().removeCapacity(new DestinationKey(orderType, runtimeContext),
                                                                              capacityName);
  }


  public List<Capacity> listCapacitiesForOrderType(DestinationKey destination) {
    return getXynaProcessingODS().getCapacityMappingDatabase().getCapacities(destination);
  }

  private void initOrderStartupAndMigrationManagement() {
    int ownBinding = getXynaProcessingODS().getOrderArchive().getOwnBinding();
    OrderStartupAndMigrationManagement.getInstance(ownBinding);
  }
  
  private void startPersistedOrders() {
    // lade gespeicherte+pausierte Aufträge im eigenen Thread
    ODS ods = getXynaProcessingODS().getODS();
    ClusterState clusterState = getClusterState(ods);
    OrderStartupAndMigrationManagement.getInstance().startLoadingAtStartup(clusterState);
    if(clusterState == ClusterState.DISCONNECTED_MASTER) {
      OrderStartupAndMigrationManagement.getInstance().startMigrating(clusterState, 0);
    }
  }
  


  /**
   * @param ods
   * @return
   * @throws XFMG_UnknownClusterInstanceIDException
   */
  private ClusterState getClusterState(ODS ods) {
    long clusterInstanceId = ods.getClusterInstanceId(ODSConnectionType.DEFAULT, OrderInstanceBackup.class);
    XynaClusteringServicesManagementInterface clusterMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    try {
      return clusterMgmt.getClusterInstance(clusterInstanceId).getState();
    } catch (XFMG_UnknownClusterInstanceIDException e) {
      return ClusterState.NO_CLUSTER;
    }
  }


  /**
   * 1. cronls anhalten 2. trigger, rmi anhalten 3. mi bearbeitung deaktivieren 4. scheduler anhalten (pause-aufträge
   * müssen noch laufen dürfen!) 5. aufträge die bald einen timeout haben mit fehler beantworten 6. 7. pause signal an
   * laufende aufträge senden (timeout) 8. warten bis keine aufträge mehr laufen 9. warten bis von triggern keine
   * aufträge mehr im planning sind oder dahin kommen können 10. überprüfen, dass responselistener-anzahl = wartender +
   * pausierte aufträge 11. responselistener deregistrieren 12. pausierte und wartende aufträge persistieren 13.
   * orderseriesmanagement persistieren
   */
  @Override
  public void stopGracefully() {

    try {
      getXynaProcessCtrlExecution().stopTriggers();
    } catch (XACT_TriggerCouldNotBeStoppedException e1) {
      logger.warn("could not stop trigger while shutting down", e1);
    }

    //RMI
    ((XynaMultiChannelPortal)XynaFactory.getInstance().getXynaMultiChannelPortal()).unregisterRMIchannel();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement().unregisterRmiInterfaces();

    if (getWorkflowEngine().getExecutionProcessor() instanceof InterruptableExecutionProcessor) {
      ((InterruptableExecutionProcessor) getWorkflowEngine().getExecutionProcessor()).stopAcceptingNewOrders();
    } else {
      logger.warn("ExecutionProcessor does not support suspension, cannot stop accepting new orders");
    }

    // 1. stop CronLS
    if (getXynaScheduler() != null && getXynaScheduler().getCronLikeScheduler() != null) {
      getXynaScheduler().getCronLikeScheduler().stopScheduling();
    }

    getXynaScheduler().pauseScheduling(true, true);

    // MI sperren
    if (getXynaProcessingODS() != null && getXynaProcessingODS().getManualInteractionManagement() != null) {
      getXynaProcessingODS().getManualInteractionManagement()
          .setProcessingRejectionState(ManualInteractionProcessingRejectionState.SHUTDOWN);
    }

    
    // mögliche Migration beenden!
    OrderStartupAndMigrationManagement.getInstance().stopMigrating();
    OrderStartupAndMigrationManagement.getInstance().pauseLoadingAtStartup();
    
    // 3. pause signal an laufende aufträge senden und auf timeouts warten
    try {
      getXynaProcessCtrlExecution().getSuspendResumeManagement().suspendAllOrders(true);
    } catch (XPRC_SuspendFailedException e1) {
      logger.warn("Failed to suspend orders, trying to continue graceful shutdown anyway", e1);
    }
    // interrupt/stop ServiceDestination executions
    terminateAllServiceExecutions();

    // warten bis von triggern keine aufträge mehr im planning sind oder dahin kommen können
    waitForActiveThreads( false, XynaProperty.TIMEOUT_SHUTDOWN_ORDERS_IN_PLANNING, "planning");

    // warten bis keine Aufträge mehr in Execution oder Planning sind
    // passiert zB. wenn ein Auftrag erfolgreich unterbrochen wurde und noch dabei ist auszulaufen,
    // in diesem Fall wollen wir noch nicht die ResponseListener entfernen
    waitForActiveThreads( true, XynaProperty.TIMEOUT_SHUTDOWN_ORDERS_IN_CLEANUP, "cleanup");

    // aufträge speichern
    // 1) get all root suspension entries and their xyna orders
    // 2) identify those child entries which are still required
    // 3) for these entries: store the entry within the xynaorder
    // 4) remove all information but the one that results from manual suspension
    final List<XynaOrderServerExtension> ordersForPersistence =
        getXynaScheduler().getAllOrdersList().getAllNotBackupedOrders();
   
    if (logger.isDebugEnabled()) {
      logger.debug("Found " + ordersForPersistence.size() + " waiting orders.");
    }

    boolean connectionToClusterSeemsBroken = false;
    final AtomicInteger backupsCurrentlyInTransaction = new AtomicInteger();
    final AtomicInteger commitedBackups = new AtomicInteger();
    if (ordersForPersistence.size() > 0) {
      WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {

        public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
          ListIterator<XynaOrderServerExtension> iter = ordersForPersistence.listIterator(commitedBackups.get());
          backupsCurrentlyInTransaction.set(0);
          while (iter.hasNext()) {
            XynaOrderServerExtension xo = iter.next();
            try {
              getXynaProcessingODS().getOrderArchive().backup(xo, BackupCause.SHUTDOWN, con);
            } catch (XNWH_RetryTransactionException e) {
              throw e;
            } catch (PersistenceLayerException e) {
              logger.warn("Could not save XynaOrder <" + xo + ">. Continuing saving of suspended orders.", e);
            } finally {
              backupsCurrentlyInTransaction.incrementAndGet();
            }
            if (backupsCurrentlyInTransaction.get() % 100 == 0) {
              con.commit();
              commitedBackups.set(backupsCurrentlyInTransaction.get());
            }
          }
          con.commit();
          commitedBackups.set(backupsCurrentlyInTransaction.get());
        }
        
      };
      
      try {
        WarehouseRetryExecutor.buildCriticalExecutor().
                               storable(OrderInstanceBackup.class).storable(OrderInstance.class).
                               execute(wre);
      } catch (XNWH_RetryTransactionException e) {
        connectionToClusterSeemsBroken = true;
        ListIterator<XynaOrderServerExtension> iter = ordersForPersistence.listIterator(commitedBackups.get());
        StringBuilder sb = new StringBuilder();
        while (iter.hasNext()) {
          XynaOrderServerExtension warnXo = iter.next();
          if (!warnXo.hasParentOrder()) {
            sb.append(", <").append(warnXo.toString()).append(">");
          }
          // FIXME counter hochzählen und alle 1000 (?) Aufträge die IDs rausschreiben, sonst wird der StringBuilder zu groß
        }
        logger.warn("Could not save XynaOrders for the following orders and their child orders: " + sb.toString(), e);
      } catch (PersistenceLayerException e) {
        //are all caught inside FIXME not true!
        connectionToClusterSeemsBroken = true;
        logger.error(null, e);
      }
      
    }

    // backup für orderinstancedetails erstellen
    if (!connectionToClusterSeemsBroken) {
      try {
        getXynaProcessingODS().getOrderArchive().backupOrderInstanceDetailsOnShutdown();
      } catch (PersistenceLayerException e) {
        logger.error("Failed to backup orderarchive.", e);
      }
    }

  }


  private void waitForActiveThreads(boolean prioritized, XynaPropertyDuration timeout, String msg) {
    if( ! XynaExecutor.getInstance().hasOpenThreads(prioritized) ) {
      return; //nichts zu tun
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Waiting for " + timeout.getMillis() + " ms at the most for orders currently being in "+msg+" state..");
    }
    long start = System.currentTimeMillis();
    long end = start + timeout.getMillis();
    long now = start;
    try {
      while (now <= end && XynaExecutor.getInstance().hasOpenThreads(prioritized) ) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          throw new RuntimeException("thread has been interrupted unexpectedly", e);
        }
        now = System.currentTimeMillis();
      }
    } finally {
      if ( XynaExecutor.getInstance().hasOpenThreads(prioritized) ) {
        logger.warn("Server has waited for " + (now-start)
            + " ms for orders currently being in "+msg+" state. Giving up now and continuing shutdown.");
      } else {
        if( logger.isDebugEnabled() ) {
          logger.debug("Server has waited for " + (now-start) + " ms for orders currently being in "+msg+" state.");
        }
      }
    }
  }

  
  private void removeBadResponseListeners(XynaOrderServerExtension xo) {
    for (XynaOrderServerExtension order : xo.getOrderAndChildrenRecursively()) {
      if (order.hasParentOrder()) {
        order.setResponseListener(null);
      }
    }
  }


  private void reinitializeChildren(XynaOrderServerExtension xo) {
    for (XynaOrderServerExtension order : xo.getOrderAndChildrenRecursively()) {
      if (order.getExecutionProcessInstance() != null) {
        order.getExecutionProcessInstance().tryReinitialize();
      }
    }
  }


  private void terminateAllServiceExecutions() {
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
        .getExecutionEngineDispatcher().terminateAllThreadsOfRunningServiceExecution(false);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      ; // just continue the shutdown
    }
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
        .getExecutionEngineDispatcher().terminateAllThreadsOfRunningServiceExecution(true);
  }



  public FactoryWarehouseCursor<OrderInstanceBackup> listSuspendedOrders(ODSConnection defaultConnetion)
      throws PersistenceLayerException {
    return getXynaProcessingODS().getOrderArchive().getAllSuspendedNoDetails(null, defaultConnetion);
  }


  public Map<DestinationKey, DestinationValue> getDestinations(DispatcherIdentification dispatcherId) {
    switch (dispatcherId) {
      case Cleanup :
        return getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher().getDestinations();
      case Execution :
        return getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher().getDestinations();
      case Planning :
        return getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher().getDestinations();
    }
    return null;
  }


  public List<DispatcherEntry> listDestinations(DispatcherIdentification dispatcherId) {
    XynaDispatcher dispatcher = getDispatcher(dispatcherId);
    Map<DestinationKey, DestinationValue> map = dispatcher.getDestinations();
    List<DispatcherEntry> result = new ArrayList<DispatcherEntry>();
    if (map != null) {
      for (Entry<DestinationKey, DestinationValue> dispatcherEntry : map.entrySet()) {
        result.add(new DispatcherEntry(dispatcherEntry, dispatcher.isCustom(dispatcherEntry.getKey())));
      }
    }
    return result;
  }


  private XynaDispatcher getDispatcher(DispatcherIdentification dispatcherId) {
    switch (dispatcherId) {
      case Cleanup :
        return getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher();
      case Execution :
        return getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher();
      case Planning :
        return getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher();
      default :
        throw new RuntimeException();
    }
  }


  public DispatcherEntry getDestination(DispatcherIdentification dispatcherId, DestinationKey dk)
      throws XPRC_DESTINATION_NOT_FOUND {
    XynaDispatcher dispatcher = getDispatcher(dispatcherId);
    return new DispatcherEntry(dk, dispatcher.getDestination(dk), dispatcher.isCustom(dk));
  }
  
  
  public DispatcherEntry getDestination(DispatcherIdentification dispatcherId, DestinationKey dk,
                                        boolean followRuntimeContextDependencies)
                  throws XPRC_DESTINATION_NOT_FOUND {
    XynaDispatcher dispatcher = getDispatcher(dispatcherId);
    return new DispatcherEntry(dk, dispatcher.getDestination(dk, followRuntimeContextDependencies), dispatcher.isCustom(dk));
  }



  public void removeDestination(DispatcherIdentification dispatcherId, DestinationKey dk)
      throws PersistenceLayerException {
    DestinationValue dv;
    
    try {
      switch (dispatcherId) {
        case Cleanup :
          dv = getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher().getDestination(dk);
          getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher().removeCustomDestination(dk, dv);
          break;

        case Execution :
          dv = getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher().getDestination(dk);
          getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher()
              .removeCustomDestination(dk, dv);
          break;
        case Planning :
          dv = getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher().getDestination(dk);
          getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher().removeCustomDestination(dk, dv);
          break;
      }
    } catch (XPRC_DESTINATION_NOT_FOUND e) {
      // we supress any hint that the destination did not exist
    }
    
    if (dk.getApplicationName() == null) {
      boolean cleanupFound = true;
      boolean planningFound = true;
      boolean executionFound = true;

      try {
        getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher().getDestination(dk);
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        cleanupFound = false;
      }

      try {
        getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher().getDestination(dk);
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        planningFound = false;
      }

      try {
        getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher().getDestination(dk);
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        executionFound = false;
      }

      if (!cleanupFound && !planningFound && !executionFound) {
        Integer[] priorities = DeploymentHandling.allPriorities;
        for (int i = priorities.length - 1; i >= 0; i--) {
          try {
            XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                .executeUndeploymentHandler(priorities[i], dk);
          } catch (XPRC_UnDeploymentHandlerException e) {
            logger.warn("Call of undeployment handler failed.", e);
          }
        }
      }
    }
    
  }


  public void setDestination(DispatcherIdentification dispatcherId, DestinationKey dk, DestinationValue dv)
      throws PersistenceLayerException {
    switch (dispatcherId) {
      case Cleanup :
        getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher().setCustomDestination(dk, dv);
        break;
      case Execution :
        getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher().setCustomDestination(dk, dv);
        break;
      case Planning :
        getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher().setCustomDestination(dk, dv);
        break;
    }

  }


  public KillStuckProcessBean killStuckProcess(Long orderId, boolean forceKill, AbortionCause reason) throws XynaException {
    KillStuckProcessBean bean = new KillStuckProcessBean(orderId, forceKill, reason);
    killStuckProcess(bean, true, null);
    return bean;
  }
  
  public void killStuckProcess(KillStuckProcessBean bean, boolean synchronously,  ResponseListener rl) throws XynaException {
    XynaOrderCreationParameter xocp =
        new XynaOrderCreationParameter(XynaDispatcher.DESTINATION_KEY_KILL_STUCK_PROC.getOrderType(),
                                       Thread.MAX_PRIORITY, bean);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    if (synchronously) {
      boolean gotException = true;
      try {
        KillStuckProcessBean b = (KillStuckProcessBean) getXynaProcessCtrlExecution().startOrderSynchronously(xocp);
        gotException = false;
      } finally {
        if (gotException) {
          //damit die logmeldungen im bean nicht verloren gehen
          if (logger.isDebugEnabled()) {
            logger.debug("killing order " + bean.getOrderIdToBeKilled() + " failed. log:");
            logger.debug(bean.getResultMessage());
          }
        }
      }
    } else {
      if (rl == null) {
        getXynaProcessCtrlExecution().startOrder(xocp);
      } else {
        getXynaProcessCtrlExecution().startOrder(xocp, rl);
      }
    }
  }
  
  public KillStuckProcessBean killStuckProcess(KillStuckProcessBean bean) throws XynaException {
    XynaOrderCreationParameter xocp =
        new XynaOrderCreationParameter(XynaDispatcher.DESTINATION_KEY_KILL_STUCK_PROC.getOrderType(),
                                       Thread.MAX_PRIORITY, bean);
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    return (KillStuckProcessBean) getXynaProcessCtrlExecution().startOrderSynchronously(xocp);
  }


  public boolean configureOrderContextMappingForDestinationKey(DestinationKey dk, boolean createMapping)
      throws PersistenceLayerException {
    return getXynaProcessingODS().getOrderContextConfiguration().configureDestinationKey(dk, createMapping);
  }


  public Collection<DestinationKey> getAllDestinationKeysForWhichAnOrderContextMappingIsCreated() {
    return getXynaProcessingODS().getOrderContextConfiguration()
        .getAllDestinationKeysForWhichAnOrderContextMappingIsCreated();
  }


  public static OrderContext getOrderContext() {
    return XynaFactory.getInstance().getProcessing().getWorkflowEngine().getOrderContext();
  }


  public long startFrequencyControlledTask(FrequencyControlledTaskCreationParameter creationParameter)
      throws XynaException {
    return frequencyControl.startFrequencyControlledTask(creationParameter);
  }


  public boolean cancelFrequencyControlledTask(long taskId) throws XynaException {
    return frequencyControl.cancelFrequencyControlledTask(taskId, false);
  }


  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(long taskId) throws XynaException {
    return frequencyControl.getFrequencyControlledTaskInformation(taskId, new String[0]);
  }


  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(long taskId,
                                                                                  String[] selectedStatistics)
      throws XynaException {
    return frequencyControl.getFrequencyControlledTaskInformation(taskId, selectedStatistics);
  }


  public List<CapacityMappingStorable> getAllCapacityMappings() {
    return getXynaProcessingODS().getCapacityMappingDatabase().getAllCapacityMappings();
  }


  public CapacityInformation getCapacityInformation(String capacityName) {
    return getXynaScheduler().getCapacityManagement().getCapacityInformation(capacityName);
  }


  public static class DispatcherEntry implements Serializable {

    private static final long serialVersionUID = -4147774422278748428L;
    private final DestinationKey dk;
    private final SerializableDestinationValue dv;
    private final boolean custom;

    public DispatcherEntry() {
      dk = null;
      dv = null;
      custom = false;
    }


    DispatcherEntry(DestinationKey dk, DestinationValue dv, boolean custom) {
      this.dk = dk;
      this.dv = new SerializableDestinationValue(dv);
      this.custom = custom;
    }


    DispatcherEntry(Entry<DestinationKey, DestinationValue> entry, boolean custom) {
      this.dk = entry.getKey();
      this.dv = new SerializableDestinationValue(entry.getValue());
      this.custom = custom;
    }


    public DestinationKey getKey() {
      return dk;
    }


    public SerializableDestinationValue getValue() {
      return dv;
    }
    
    public boolean isCustom() {
      return custom;
    }
  }


  public static class SerializableDestinationValue implements Serializable {

    private static final long serialVersionUID = 5104007385540790315L;
    private final String destinationType;
    private final String fqName;


    public SerializableDestinationValue() {
      this.destinationType = null;
      this.fqName = null;
    }


    SerializableDestinationValue(DestinationValue dv) {
      this.destinationType = dv.getDestinationType().getTypeAsString();
      this.fqName = dv.getFQName();
    }


    public String getDestinationType() {
      return destinationType;
    }


    public String getFqName() {
      return fqName;
    }

  }


  public void restartCronLikeTimerThread() {
    getXynaScheduler().getCronLikeScheduler().startTimerThread();
  }


  public SchedulerInformationBean listSchedulerInformation(SchedulerInformationBean.Mode mode) {
    return getXynaScheduler().getInformationBean(mode);
  }

  public OrderSeriesManagementInformation listOrderSeriesManagementInformation(OrderSeriesManagementInformation.Mode mode) {
    return getXynaScheduler().getOrderSeriesManagement().getOrderSeriesManagementInformation(mode);
  }
  
  public RescheduleSeriesOrderInformation rescheduleSeriesOrder(long orderId, boolean force) {
    return getXynaScheduler().getOrderSeriesManagement().rescheduleSeriesOrder(orderId, force);
  }
    
  public void allocateAdministrativeVeto(String vetoName, String documentation) throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
    getXynaScheduler().getVetoManagement().allocateAdministrativeVeto(new AdministrativeVeto(vetoName, documentation));
  }


  public void freeAdministrativeVeto(String vetoName) throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException {
    getXynaScheduler().getVetoManagement().freeAdministrativeVeto(new AdministrativeVeto(vetoName,null));
  }
  
  
  public void setDocumentationOfAdministrativeVeto(String vetoName, String documentation) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    getXynaScheduler().getVetoManagement().setDocumentationOfAdministrativeVeto(new AdministrativeVeto(vetoName, documentation));
  }


  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    return getXynaScheduler().getVetoManagement().searchVetos(select, maxRows);
  }


  public Long startBatchProcess(BatchProcessInput input) throws XynaException{
    return getBatchProcessManagement().startBatchProcess(input);
  }

  public BatchProcessInformation startBatchProcessSynchronous(BatchProcessInput input) throws XynaException {
    return getBatchProcessManagement().startBatchProcessSynchronous(input);
  }

  public BatchProcessInformation getBatchProcessInformation(Long batchProcessId) throws XynaException {
    return getBatchProcessManagement().getBatchProcessInformation(batchProcessId);
  }

  public BatchProcessSearchResult searchBatchProcesses(BatchProcessSelectImpl select, int maxRows) throws PersistenceLayerException {
    return getBatchProcessManagement().searchBatchProcesses(select,maxRows);
  }
  
  public boolean cancelBatchProcess(Long batchProcessId, CancelMode cancelMode) throws PersistenceLayerException{
    return getBatchProcessManagement().cancelBatchProcess(batchProcessId, cancelMode, -1L);
  }

  public boolean pauseBatchProcess(Long batchProcessId) throws PersistenceLayerException{
    return getBatchProcessManagement().pauseBatchProcess(batchProcessId);
  }

  public boolean continueBatchProcess(Long batchProcessId) throws PersistenceLayerException{
    return getBatchProcessManagement().continueBatchProcess(batchProcessId);
  }

  public boolean modifyBatchProcess(Long batchProcessId, BatchProcessInput input) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY{
    return getBatchProcessManagement().modifyBatchProcess(batchProcessId, input);
  }



}
