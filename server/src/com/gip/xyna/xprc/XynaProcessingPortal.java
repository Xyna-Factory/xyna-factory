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

package com.gip.xyna.xprc;



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
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
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
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSearchResult;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSelectImpl;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowInformation;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;



public interface XynaProcessingPortal {

  public final int DEFAULT_PRIORITY = 3;


  public enum DispatcherIdentification {
    Planning, Execution, Cleanup;

    public static DispatcherIdentification valueOfIgnoreCase(String dispatcherName) {
      if (Planning.toString().equalsIgnoreCase(dispatcherName)) {
        return Planning;
      } else if (Execution.toString().equalsIgnoreCase(dispatcherName)) {
        return Execution;
      } else if (Cleanup.toString().equalsIgnoreCase(dispatcherName)) {
        return Cleanup;
      }
      throw new IllegalArgumentException(dispatcherName);
    }
  };


  public Long startOrder(XynaOrderCreationParameter xocp);


  public GeneralXynaObject startOrderSynchronously(XynaOrderCreationParameter xocp) throws XynaException;


  public CancelBean cancelOrder(Long id, Long timeout, boolean waitForTimeout) throws XPRC_CancelFailedException;


  public CancelBean cancelOrder(Long id, Long timeout) throws XPRC_CancelFailedException;


  public CronLikeOrder startCronLikeOrder(CronLikeOrderCreationParameter clocp) throws XPRC_CronLikeSchedulerException;
  
  
  public void restartCronLikeTimerThread();


  @Deprecated
  public CronLikeOrder modifyCronLikeOrder(Long id, String label, String ordertype, GeneralXynaObject payload,
                                           Long firstStartupTime, Long interval, Boolean enabled, OnErrorAction onError)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException;


  public CronLikeOrder modifyTimeControlledOrder(Long id, CronLikeOrderCreationParameter clocp)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException;



  public CronLikeOrder modifyCronLikeOrder(Long id, String label, DestinationKey destination, GeneralXynaObject payload,
                                           Long firstStartupTime, String timeZoneID, Long interval, Boolean useDST,
                                           Boolean enabled, OnErrorAction onError, String cloCustom0,
                                           String cloCustom1, String cloCustom2, String cloCustom3)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException;


  public CronLikeOrder modifyCronLikeOrder(Long id, String label, DestinationKey destination, GeneralXynaObject payload,
                                           Calendar firstStartupTimeWithTimeZone, Long interval, Boolean useDST,
                                           Boolean enabled, OnErrorAction onError, String cloCustom0,
                                           String cloCustom1, String cloCustom2, String cloCustom3)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException;


  public XynaOrderServerExtension startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp)
                  throws XynaException;

  public OrderExecutionResponse startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp, ResultController resultController);

  public Long startBatchProcess(BatchProcessInput input) throws XynaException;

  public BatchProcessInformation startBatchProcessSynchronous(BatchProcessInput input) throws XynaException;

  public BatchProcessInformation getBatchProcessInformation(Long batchProcessId) throws XynaException;

  public BatchProcessSearchResult searchBatchProcesses(BatchProcessSelectImpl select, int maxRows) throws PersistenceLayerException;
  
  public boolean cancelBatchProcess(Long batchProcessId, CancelMode cancelMode) throws PersistenceLayerException;

  public boolean pauseBatchProcess(Long batchProcessId) throws PersistenceLayerException;

  public boolean continueBatchProcess(Long batchProcessId) throws PersistenceLayerException;

  public boolean modifyBatchProcess(Long batchProcessId, BatchProcessInput input) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
  
  public boolean removeCronLikeOrder(Long id) throws XPRC_CronLikeOrderStorageException, XPRC_CronRemovalException;


  public boolean addCapacity(String string, int cardinality, State enumState) throws XPRC_CAPACITY_ALREADY_DEFINED,
                  PersistenceLayerException;


  public Collection<CapacityInformation> listCapacityInformation();

  
  public SchedulerInformationBean listSchedulerInformation(SchedulerInformationBean.Mode mode);

  public OrderSeriesManagementInformation listOrderSeriesManagementInformation(OrderSeriesManagementInformation.Mode mode);
  
  public RescheduleSeriesOrderInformation rescheduleSeriesOrder(long orderId, boolean force);
  
  public Collection<VetoInformationStorable> listVetoInformation() throws PersistenceLayerException;


  public ExtendedCapacityUsageInformation listExtendedCapacityInformation();

  /**
   * Lists all saved and deployed workflows in default workspace
   * 
   * @return HashMap&lt;String, DeploymentStatus&gt; - The mapping fqClassName -&gt; deployment status
   */
  public Map<ApplicationEntryType, Map<String, DeploymentStatus>> listDeploymentStatuses(Long revision);

  public List<WorkflowInformation> listWorkflows() throws XynaException;
  
  @Deprecated
  public boolean requireCapacityForWorkflow(String workflowName, String capName, int cardinality)
      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality;

  
  @Deprecated
  public boolean removeCapacityForWorkflow(String wfName, String capacityName) throws PersistenceLayerException;


  public boolean requireCapacityForOrderType(String orderType, String capName, int cardinality)
      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality;
  
  public boolean requireCapacityForOrderType(String orderType, String capName, int cardinality, String applicationName, String versionName)
      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality;

  public boolean requireCapacityForOrderType(String orderType, String capName, int cardinality, RuntimeContext runtimeContext)
                  throws PersistenceLayerException, XFMG_InvalidCapacityCardinality;


  public boolean removeCapacityForOrderType(String orderType, String capacityName) throws PersistenceLayerException;
  
  public boolean removeCapacityForOrderType(String orderType, String capacityName, String applicationName, String versionName) throws PersistenceLayerException;

  public boolean removeCapacityForOrderType(String orderType, String capacityName, RuntimeContext runtimeContext) throws PersistenceLayerException;


  public List<String> listOrderTypesForWorkflow(String workflowOriginalFQName);


  public List<CapacityMappingStorable> getAllCapacityMappings();


  public CapacityInformation getCapacityInformation(String capacityName);


  public boolean removeCapacity(String capacityName) throws PersistenceLayerException;


  public boolean changeCapacityName(String capacityName, String newName) throws PersistenceLayerException;


  public boolean changeCapacityCardinality(String capacityName, int newCardinality) throws PersistenceLayerException, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;


  public boolean changeCapacityState(String capacityName, State newState) throws PersistenceLayerException;


  public List<Capacity> listCapacitiesForOrderType(DestinationKey destination);


  @Deprecated
  public void registerSavedWorkflow(String fqNameFromXml);
  
  /**
   * This is supposed to be called when saving a workflow to register it internally and make it available for GUI
   * purposes.
   * 
   * @param fqNameFromXml
   * @param revision
   */
  public void registerSavedWorkflow(String fqNameFromXml, Long revision);


  public void unregisterSavedWorkflow(String fqNameFromXml);


  public KillStuckProcessBean killStuckProcess(Long orderId, boolean forceKill, AbortionCause reason) throws XynaException;
  

  public KillStuckProcessBean killStuckProcess(KillStuckProcessBean bean) throws XynaException;


  public FactoryWarehouseCursor<OrderInstanceBackup> listSuspendedOrders(ODSConnection defaultConnection)
      throws PersistenceLayerException;


  public void setDestination(DispatcherIdentification dispatcherId, DestinationKey dk, DestinationValue dv)
                  throws PersistenceLayerException;


  public void removeDestination(DispatcherIdentification dispatcherId, DestinationKey dk)
                  throws PersistenceLayerException;


  public Map<DestinationKey, DestinationValue> getDestinations(DispatcherIdentification dispatcherId);


  public List<DispatcherEntry> listDestinations(DispatcherIdentification dispatcherId);


  public DispatcherEntry getDestination(DispatcherIdentification dispatcherId, DestinationKey dk)
                  throws XPRC_DESTINATION_NOT_FOUND;
  
  public DispatcherEntry getDestination(DispatcherIdentification dispatcherId, DestinationKey dk, boolean followRuntimeContextDependencies)
                  throws XPRC_DESTINATION_NOT_FOUND;


  public boolean configureOrderContextMappingForDestinationKey(DestinationKey dk, boolean createMapping)
                  throws PersistenceLayerException;


  public Collection<DestinationKey> getAllDestinationKeysForWhichAnOrderContextMappingIsCreated();


  public long startFrequencyControlledTask(FrequencyControlledTaskCreationParameter creationParameter)
                  throws XynaException;


  /**
   * Obtain information on a frequency controlled task. Does not return any statistics information.
   */
  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(long taskId) throws XynaException;


  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(long taskId,
                                                                                  String[] selectedStatistics)
                  throws XynaException;


  public boolean cancelFrequencyControlledTask(long taskId) throws XynaException;
  
  
  public void allocateAdministrativeVeto(String vetoName, String documentation) throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException;
  
  public void freeAdministrativeVeto(String vetoName) throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException;
  
  public void setDocumentationOfAdministrativeVeto(String vetoName, String documentation) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException;
  
}
