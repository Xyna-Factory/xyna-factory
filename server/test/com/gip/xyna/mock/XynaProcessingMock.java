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
package com.gip.xyna.mock;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCapacityCardinality;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.ResultController;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.OrderStatus;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.XynaProcessingBase;
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
import com.gip.xyna.xprc.xfractwfe.XynaPythonSnippetManagement;
import com.gip.xyna.xprc.xpce.WorkflowEngine;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowInformation;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean.Mode;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;

public class XynaProcessingMock extends XynaProcessingBase {

  private XynaProcessingODS processingODS;
  private XynaScheduler scheduler;
  
  public XynaProcessingMock() throws XynaException {
    processingODS = new XynaProcessingODS();
    scheduler = new XynaScheduler();
  }

  public Long startOrder(XynaOrderCreationParameter xocp) {
    // TODO Auto-generated method stub
    return null;
  }

  public GeneralXynaObject startOrderSynchronously(XynaOrderCreationParameter xocp) throws XynaException {
    // TODO Auto-generated method stub
    return null;
  }

  public CancelBean cancelOrder(Long id, Long timeout) throws XPRC_CancelFailedException {
    // TODO Auto-generated method stub
    return null;
  }

  public CronLikeOrder startCronLikeOrder(CronLikeOrderCreationParameter clocp) throws XPRC_CronLikeSchedulerException {
    // TODO Auto-generated method stub
    return null;
  }

  public void restartCronLikeTimerThread() {
    // TODO Auto-generated method stub
    
  }

  public CronLikeOrder modifyCronLikeOrder(Long id, String label, String ordertype, GeneralXynaObject payload,
      Long firstStartupTime, Long interval, Boolean enabled, OnErrorAction onError)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    // TODO Auto-generated method stub
    return null;
  }

  public CronLikeOrder modifyTimeControlledOrder(Long id, CronLikeOrderCreationParameter clocp)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    // TODO Auto-generated method stub
    return null;
  }

  public CronLikeOrder modifyCronLikeOrder(Long id, String label, DestinationKey destination, GeneralXynaObject payload,
      Long firstStartupTime, String timeZoneID, Long interval, Boolean useDST, Boolean enabled, OnErrorAction onError,
      String cloCustom0, String cloCustom1, String cloCustom2, String cloCustom3)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    // TODO Auto-generated method stub
    return null;
  }

  public CronLikeOrder modifyCronLikeOrder(Long id, String label, DestinationKey destination, GeneralXynaObject payload,
      Calendar firstStartupTimeWithTimeZone, Long interval, Boolean useDST, Boolean enabled, OnErrorAction onError,
      String cloCustom0, String cloCustom1, String cloCustom2, String cloCustom3)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    // TODO Auto-generated method stub
    return null;
  }

  public XynaOrderServerExtension startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp)
      throws XynaException {
    // TODO Auto-generated method stub
    return null;
  }

  public OrderExecutionResponse startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp,
      ResultController resultController) {
    // TODO Auto-generated method stub
    return null;
  }

  public Long startBatchProcess(BatchProcessInput input) throws XynaException {
    // TODO Auto-generated method stub
    return null;
  }

  public BatchProcessInformation startBatchProcessSynchronous(BatchProcessInput input) throws XynaException {
    // TODO Auto-generated method stub
    return null;
  }

  public BatchProcessInformation getBatchProcessInformation(Long batchProcessId) throws XynaException {
    // TODO Auto-generated method stub
    return null;
  }

  public BatchProcessSearchResult searchBatchProcesses(BatchProcessSelectImpl select, int maxRows)
      throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean cancelBatchProcess(Long batchProcessId, CancelMode cancelMode) throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean pauseBatchProcess(Long batchProcessId) throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean continueBatchProcess(Long batchProcessId) throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean modifyBatchProcess(Long batchProcessId, BatchProcessInput input)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean removeCronLikeOrder(Long id) throws XPRC_CronLikeOrderStorageException, XPRC_CronRemovalException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean addCapacity(String string, int cardinality, State enumState)
      throws XPRC_CAPACITY_ALREADY_DEFINED, PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }

  public Collection<CapacityInformation> listCapacityInformation() {
    // TODO Auto-generated method stub
    return null;
  }

  public SchedulerInformationBean listSchedulerInformation(Mode mode) {
    // TODO Auto-generated method stub
    return null;
  }

  public OrderSeriesManagementInformation listOrderSeriesManagementInformation(
      com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation.Mode mode) {
    // TODO Auto-generated method stub
    return null;
  }

  public RescheduleSeriesOrderInformation rescheduleSeriesOrder(long orderId, boolean force) {
    // TODO Auto-generated method stub
    return null;
  }

  public Collection<VetoInformationStorable> listVetoInformation() throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return null;
  }

  public ExtendedCapacityUsageInformation listExtendedCapacityInformation() {
    // TODO Auto-generated method stub
    return null;
  }

  public Map<ApplicationEntryType, Map<String, DeploymentStatus>> listDeploymentStatuses(Long revision) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<WorkflowInformation> listWorkflows() throws XynaException {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean requireCapacityForWorkflow(String workflowName, String capName, int cardinality)
      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean removeCapacityForWorkflow(String wfName, String capacityName) throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean requireCapacityForOrderType(String orderType, String capName, int cardinality)
      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean requireCapacityForOrderType(String orderType, String capName, int cardinality, String applicationName,
      String versionName) throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean requireCapacityForOrderType(String orderType, String capName, int cardinality,
      RuntimeContext runtimeContext) throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean removeCapacityForOrderType(String orderType, String capacityName) throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean removeCapacityForOrderType(String orderType, String capacityName, String applicationName,
      String versionName) throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean removeCapacityForOrderType(String orderType, String capacityName, RuntimeContext runtimeContext)
      throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }

  public List<String> listOrderTypesForWorkflow(String workflowOriginalFQName) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<CapacityMappingStorable> getAllCapacityMappings() {
    // TODO Auto-generated method stub
    return null;
  }

  public CapacityInformation getCapacityInformation(String capacityName) {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean removeCapacity(String capacityName) throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean changeCapacityName(String capacityName, String newName) throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean changeCapacityCardinality(String capacityName, int newCardinality)
      throws PersistenceLayerException, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean changeCapacityState(String capacityName, State newState) throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }

  public List<Capacity> listCapacitiesForOrderType(DestinationKey destination) {
    // TODO Auto-generated method stub
    return null;
  }

  public void registerSavedWorkflow(String fqNameFromXml) {
    // TODO Auto-generated method stub
    
  }

  public void registerSavedWorkflow(String fqNameFromXml, Long revision) {
    // TODO Auto-generated method stub
    
  }

  public void unregisterSavedWorkflow(String fqNameFromXml) {
    // TODO Auto-generated method stub
    
  }

  public KillStuckProcessBean killStuckProcess(Long orderId, boolean forceKill, AbortionCause reason)
      throws XynaException {
    // TODO Auto-generated method stub
    return null;
  }

  public KillStuckProcessBean killStuckProcess(KillStuckProcessBean bean) throws XynaException {
    // TODO Auto-generated method stub
    return null;
  }

  public FactoryWarehouseCursor<OrderInstanceBackup> listSuspendedOrders(ODSConnection defaultConnection)
      throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return null;
  }

  public void setDestination(DispatcherIdentification dispatcherId, DestinationKey dk, DestinationValue dv)
      throws PersistenceLayerException {
    // TODO Auto-generated method stub
    
  }

  public void removeDestination(DispatcherIdentification dispatcherId, DestinationKey dk)
      throws PersistenceLayerException {
    // TODO Auto-generated method stub
    
  }

  public Map<DestinationKey, DestinationValue> getDestinations(DispatcherIdentification dispatcherId) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<DispatcherEntry> listDestinations(DispatcherIdentification dispatcherId) {
    // TODO Auto-generated method stub
    return null;
  }

  public DispatcherEntry getDestination(DispatcherIdentification dispatcherId, DestinationKey dk)
      throws XPRC_DESTINATION_NOT_FOUND {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean configureOrderContextMappingForDestinationKey(DestinationKey dk, boolean createMapping)
      throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }

  public Collection<DestinationKey> getAllDestinationKeysForWhichAnOrderContextMappingIsCreated() {
    // TODO Auto-generated method stub
    return null;
  }

  public long startFrequencyControlledTask(FrequencyControlledTaskCreationParameter creationParameter)
      throws XynaException {
    // TODO Auto-generated method stub
    return 0;
  }

  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(long taskId) throws XynaException {
    // TODO Auto-generated method stub
    return null;
  }

  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(long taskId,
      String[] selectedStatistics) throws XynaException {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean cancelFrequencyControlledTask(long taskId) throws XynaException {
    // TODO Auto-generated method stub
    return false;
  }

  public void allocateAdministrativeVeto(String vetoName, String documentation)
      throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
    // TODO Auto-generated method stub
    
  }

  public void freeAdministrativeVeto(String vetoName)
      throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException {
    // TODO Auto-generated method stub
    
  }

  public void setDocumentationOfAdministrativeVeto(String vetoName, String documentation)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    // TODO Auto-generated method stub
    
  }

  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public XynaScheduler getXynaScheduler() {
    return scheduler;
  }

  @Override
  public XynaProcessCtrlExecution getXynaProcessCtrlExecution() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public WorkflowEngine getWorkflowEngine() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public XynaProcessingODS getXynaProcessingODS() {
     return processingODS;
  }

  @Override
  public XynaFrequencyControl getFrequencyControl() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BatchProcessManagement getBatchProcessManagement() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CancelBean cancelOrder(Long id, Long timeout, boolean waitForTimeout) throws XPRC_CancelFailedException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void stopGracefully() throws XynaException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public OrderStatus getOrderStatus() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getDefaultName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected void init() throws XynaException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public DispatcherEntry getDestination(DispatcherIdentification dispatcherId, DestinationKey dk,
                                        boolean followRuntimeContextDependencies)
                  throws XPRC_DESTINATION_NOT_FOUND {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public XynaXmomSerialization getXmomSerialization() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public XynaPythonSnippetManagement getXynaPythonSnippetManagement() {
    // TODO Auto-generated method stub
    return null;
  }

}
