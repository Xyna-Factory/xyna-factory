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

package com.gip.xyna.xmcp;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNodeStorable;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyAccess;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyRight;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xprc.CustomStringContainer;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.XynaProcessingPortal;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSearchResult;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSelectImpl;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSearchResult;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSelect;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DependentObjectMode;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension.AcknowledgableObject;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowInformation;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSearchResult;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSelectImpl;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;



public interface XynaProcessingRMI extends Remote {

  public final int DEFAULT_PRIORITY = XynaProcessingPortal.DEFAULT_PRIORITY;


  /**
   * @return the order id of the started order
   */
  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public Long startOrder(String user, String password, GeneralXynaObject payload, String orderType, int prio)
                  throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public Long startOrder(String user, String password, GeneralXynaObject payload, String orderType, int prio,
                         String custom1, String custom2, String custom3, String custom4) throws XynaException,
                  RemoteException;
  
  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public Long startOrder(String user, String password, GeneralXynaObject payload, String orderType, int prio,
                         String custom1, String custom2, String custom3, String custom4, AcknowledgableObject acknowledgableObject) throws XynaException,
                  RemoteException;


  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public GeneralXynaObject startOrderSynchronously(String user, String password, GeneralXynaObject payload,
                                                   String orderType, int prio) throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public GeneralXynaObject startOrderSynchronously(String user, String password, GeneralXynaObject payload,
                                                   String orderType, int prio, String custom1, String custom2,
                                                   String custom3, String custom4) throws XynaException,RemoteException;


  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.insert, checks=4)
  public Long startCronLikeOrder(String user, String password, String label, String payload, String orderType,
                                 Long startTime, Long interval, boolean enabled, String onError) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.insert, checks=4)
  public Long startCronLikeOrder(String user, String password, String label, String payload, String orderType,
                                 Long startTime, Long interval, boolean enabled, String onError, String application, String version) throws RemoteException;

  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.insert, checks=1)
  public Long startCronLikeOrder(XynaCredentials credentials, RemoteCronLikeOrderCreationParameter clocp) throws RemoteException;


  @Deprecated
  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.write, checks=4)
  public CronLikeOrderInformation modifyCronLikeOrder(String user, String password, Long id, String label,
                                                      String orderType, String payload, Long firstStartupTime,
                                                      Long interval, Boolean enabled, String onError)
      throws XynaException, RemoteException;
  
  
  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.write, checks=4)
  public CronLikeOrderInformation modifyCronLikeOrder(String user, String password, Long id, String label,
                                                      String orderType, String payload, Long firstStartupTime,
                                                      String timeZoneID, Long interval, Boolean useDST,
                                                      Boolean enabled, String onError, String cloCustom0,
                                                      String cloCustom1, String cloCustom2, String cloCustom3)
      throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.write, checks={4,16,17})
  public CronLikeOrderInformation modifyCronLikeOrder(String user, String password, Long id, String label,
                                                      String orderType, String payload, Long firstStartupTime,
                                                      String timeZoneID, Long interval, Boolean useDST,
                                                      Boolean enabled, String onError, String cloCustom0,
                                                      String cloCustom1, String cloCustom2, String cloCustom3,
                                                      String applicationName, String versionName)
                                                          throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.write, checks=4)
  public CronLikeOrderInformation modifyCronLikeOrder(String user, String password, Long id, String label,
                                                      String orderType, String payload,
                                                      Calendar firstStartupTimeWithTimeZone, Long interval,
                                                      Boolean useDST, Boolean enabled, String onError,
                                                      String cloCustom0, String cloCustom1, String cloCustom2,
                                                      String cloCustom3) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.write, checks=2)
  public CronLikeOrderInformation modifyTimeControlledOrder(XynaCredentials credentials, Long id, RemoteCronLikeOrderCreationParameter clocp) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.delete, checks=2)
  public boolean removeCronLikeOrder(String user, String password, Long id) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.read, nochecks=true)
  public Map<Long, CronLikeOrderInformation> listCronLikeOrders(String user, String password, int maxRows) throws RemoteException;
 
  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.read, checks=2)
  public CronLikeOrderSearchResult searchCronLikeOrders(String user, String password, CronLikeOrderSelectImpl selectCron, int maxRows) throws RemoteException;
 
  
  @Deprecated //ServiceDestinations are not deserializable outside the factory
  @ProxyAccess(right = ProxyRight.DISPATCHER_MANAGEMENT)
  public Map<DestinationKey, DestinationValue> getDestinations(String user, String password, String dispatcher) 
                  throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.DISPATCHER_MANAGEMENT)
  public List<DispatcherEntry> listDestinations(String user, String password, String dispatcher)
                  throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DISPATCHER_MANAGEMENT)
  public DispatcherEntry getDestination(String user, String password, String dispatcher, String destinationkey)
                  throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DISPATCHER_MANAGEMENT)
  public void setDestination(String user, String password, String dispatcher, String dk, String dv)
                  throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DISPATCHER_MANAGEMENT)
  public void removeDestination(String user, String password, String dispatcher, String dk) throws XynaException,
                  RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Map<String, DeploymentStatus> listDeploymentStatuses(String user, String password) throws XynaException,
                  RemoteException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Map<ApplicationEntryType, Map<String, DeploymentStatus>> listDeploymentStatuses(XynaCredentials credentials,
                                                                                         RuntimeContext runtimeContext)
      throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public List<WorkflowInformation> listWorkflows(XynaCredentials credentials) throws XynaException, RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public XMOMDatabaseSearchResult searchXMOMDatabase(String user, String password, List<XMOMDatabaseSelect> selects, int maxRows) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public XMOMDatabaseSearchResult searchXMOMDatabase(XynaCredentials credentials, List<XMOMDatabaseSelect> selects, int maxRows, RuntimeContext runtimeContext) throws XynaException, RemoteException;

  @Deprecated
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void deployWorkflow(String user, String password, String fqClassName) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void deployWorkflow(XynaCredentials credentials, String fqClassName, RuntimeContext runtimeContext) throws XynaException, RemoteException;

  @Deprecated
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void deployDatatype(String user, String password, String xml, Map<String, byte[]> libraries) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void deployDatatype(XynaCredentials credentials, String xml, Map<String, byte[]> libraries, RuntimeContext runtimeContext) throws XynaException, RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void deployDatatype(XynaCredentials credentials, String xml, Map<String, byte[]> libraries, boolean override, String user) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void deployDatatype(XynaCredentials credentials, String xml, Map<String, byte[]> libraries, boolean override, String user, RuntimeContext runtimeContext) throws XynaException, RemoteException;

  @Deprecated
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void deployException(String user, String password, String xml) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void deployException(XynaCredentials credentials, String xml, RuntimeContext runtimeContext) throws XynaException, RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void deployException(XynaCredentials credentials, String xml, boolean override, String user) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void deployException(XynaCredentials credentials, String xml, boolean override, String user, RuntimeContext runtimeContext) throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void deployMultiple(XynaCredentials credentials, Map<XMOMType, List<String>> deploymentItems, RuntimeContext runtimeContext, String creator) throws XynaException, RemoteException;

  @Deprecated
  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public String saveMDM(String user, String password, String xml) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public String saveMDM(XynaCredentials credentials, String xml, RuntimeContext runtimeContext) throws XynaException, RemoteException;
  
  
  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public Long startOrder(String user, String password, String payload, String orderType, int prio, Long timeout, CustomStringContainer customs) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public Long startOrder(String user, String password, String payload, String orderType, int prio, Long timeout, CustomStringContainer customs, AcknowledgableObject acknowledgableObject) throws XynaException, RemoteException;

  
  /**
   * Returns immediately.
   * 
   * @param id - the order id to be canceled
   * @param timeout - the time to wait until the cancellation fails
   */
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public CancelBean cancelOrder(Long id, Long timeout) throws RemoteException;

  /**
   * If <b>waitForTimeout</b> is set to <b>true</b>, this blocks until either the order is canceled or the timeout is reached.
   * 
   * @param id - the order id to be canceled
   * @param timeout - the time to wait until the cancellation fails
   * @param waitForTimeout - specifies whether the order is supposed to wait for the cancellation result
   */
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public CancelBean cancelOrder(Long id, Long timeout, boolean waitForTimeout) throws RemoteException;

  
  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public List<String> startOrderSynchronously(String user, String password, String payload, String orderType, int prio, Long timeout, CustomStringContainer customs) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public List<String> startOrderSynchronously(String user, String password, String payload, String orderType, int prio, Long timeout, CustomStringContainer customs, AcknowledgableObject acknowledgableObject) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.FREQUENCY_CONTROL_MANAGEMENT)
  public long startFrequencyControlledTask(String user, String password, FrequencyControlledTaskCreationParameter creationParameter) throws XynaException, RemoteException;

  /**
   * @param user
   * @param password
   * @param taskId
   */
  @ProxyAccess(right = ProxyRight.FREQUENCY_CONTROL_MANAGEMENT)
  public boolean cancelFrequencyControlledTask(String user, String password, long taskId) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.FREQUENCY_CONTROL_VIEW)
  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(String user, String password, long taskId, String[] selectedStatistics) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.FREQUENCY_CONTROL_VIEW)
  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(String user, String password, long taskId) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.FREQUENCY_CONTROL_VIEW)
  public FrequencyControlledTaskSearchResult searchFrequencyControlledTask(String user, String password, FrequencyControlledTaskSelect select, int maxRows) throws RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void undeployDatatype(String user, String password, String originalFqName, boolean recursivly) throws RemoteException;

  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void undeployDatatype(XynaCredentials credentials, String originalFqName, boolean recursivly, RuntimeContext runtimeContext) throws RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void undeployException(String user, String password, String originalFqName, boolean recursivly) throws RemoteException;

  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void undeployException(XynaCredentials credentials, String originalFqName, boolean recursivly, RuntimeContext runtimeContext) throws RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void undeployWorkflow(String user, String password, String originalFqName, boolean recursivly) throws RemoteException;

  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void undeployWorkflow(XynaCredentials credentials, String originalFqName, boolean recursivly, RuntimeContext runtimeContext) throws RemoteException;

  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void undeployXMOMObject(XynaCredentials credentials, String originalFqName, XMOMType type, DependentObjectMode dependentObjectMode, RuntimeContext runtimeContext) throws RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void deleteDatatype(String user, String password, String originalFqName, boolean recursivlyUndeploy, boolean recursivlyDelete) throws RemoteException;

  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void deleteDatatype(XynaCredentials credentials, String originalFqName, boolean recursivlyUndeploy, boolean recursivlyDelete, RuntimeContext runtimeContext) throws RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void deleteException(String user, String password, String originalFqName, boolean recursivlyUndeploy, boolean recursivlyDelete) throws RemoteException;

  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void deleteException(XynaCredentials credentials, String originalFqName, boolean recursivlyUndeploy, boolean recursivlyDelete, RuntimeContext runtimeContext) throws RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void deleteWorkflow(String user, String password, String originalFqName, boolean recursivlyUndeploy, boolean recursivlyDelete) throws RemoteException;

  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void deleteWorkflow(XynaCredentials credentials, String originalFqName, boolean recursivlyUndeploy, boolean recursivlyDelete, RuntimeContext runtimeContext) throws RemoteException;
  
  
  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.read, nochecks=true)
  public ExtendedCapacityUsageInformation listExtendedCapacityInformation(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.read, nochecks=true)
  public Collection<CapacityInformation> listCapacityInformation(XynaCredentials credentials) throws RemoteException;

  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.insert, checks=2)
  public boolean addCapacity(String user, String password, String name, int cardinality, CapacityManagement.State state) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.read, checks=2)
  public CapacityInformation getCapacityInformation(String user, String password, String capacityName) throws RemoteException;

  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.delete, checks=2)
  public boolean removeCapacity(String user, String password, String name) throws RemoteException;

  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.write, checks=2)
  public boolean changeCapacityName(String user, String password, String name, String newName) throws RemoteException;

  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.write, checks=2)
  public boolean changeCapacityCardinality(String user, String password, String name, int cardinality) throws RemoteException;

  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.write, checks=2)
  public boolean changeCapacityState(String user, String password, String name, CapacityManagement.State state) throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.write, checks=2)
  public boolean requireCapacityForWorkflow(String user, String password, String workflowFqName, String capacityName, int capacityCardinality) throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.write, checks=2)
  public boolean requireCapacityForOrderType(String user, String password, String orderType, String capacityName, int capacityCardinality) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.write, checks=2)
  public boolean removeCapacityForWorkflow(String user, String password, String workflowFqName, String capacityName) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public List<CapacityMappingStorable> getAllCapacityMappings(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.VETO, action=Action.read, nochecks=true)
  public Collection<VetoInformationStorable> listVetoInformation(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.VETO, action=Action.insert, checks=2)
  public void allocateAdministrativeVeto(String user, String password, String vetoName, String documentation) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.VETO, action=Action.delete, checks=2)
  public void freeAdministrativeVeto(String user, String password, String vetoName) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.VETO, action=Action.write, checks=2)
  public void setDocumentationOfAdministrativeVeto(String user, String password, String vetoName, String documentation) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.VETO, action=Action.read, checks=2)
  public VetoSearchResult searchVetos(String user, String password, VetoSelectImpl select, int maxRows) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.read, checks=2)
  public List<Capacity> listCapacitiesForOrdertype(String user, String password, String ordertype) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.read, checks=1)
  public List<Capacity> listCapacitiesForDestination(XynaCredentials creds, DestinationKey destination) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.KILL_STUCK_PROCESS)
  public String killStuckProcess(XynaCredentials credentials, long orderId) throws XynaException, RemoteException;

  
  @ProxyAccess(right = ProxyRight.START_ORDER, checks=1)
  public OrderExecutionResponse startOrder(XynaCredentials credentials, RemoteXynaOrderCreationParameter rxocp) throws RemoteException;
  
  /**
   * gibt im gutfall die xmldarstellung der xynaobjekte zur�ck
   * im exceptionfall wird nur die simple darstellung der exceptions zur�ckgegeben 
   * Es gibt mehrere Subklassen von OrderExecutionResponse. Im Fehlerfall wird {@link ErroneousOrderExecutionResponse},
   * im asynchronen Gutfall nur {@link SuccesfullOrderExecutionResponse} und im synchronen Gutfall
   * {@link SynchronousSuccesfullOrderExecutionResponse} zur�ckgegeben.
   */
  @ProxyAccess(right = ProxyRight.START_ORDER, checks=1)
  public OrderExecutionResponse startOrderSynchronously(XynaCredentials credentials, RemoteXynaOrderCreationParameter rxocp) throws RemoteException;

  @ProxyAccess(right = ProxyRight.START_ORDER, checks=1)
  public OrderExecutionResponse startOrderSynchronously(XynaCredentials credentials,
                                                        RemoteXynaOrderCreationParameter rxocp,
                                                        ResultController controller) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.TIME_CONTROLLED_ORDER, action=Action.insert, checks=1)
  public Long startBatchProcess(XynaCredentials credentials, BatchProcessInput input) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.TIME_CONTROLLED_ORDER, action=Action.insert, checks=1)
  public BatchProcessInformation startBatchProcessSynchronous(XynaCredentials credentials, BatchProcessInput input) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.TIME_CONTROLLED_ORDER, action=Action.read, checks=1)
  public BatchProcessInformation getBatchProcessInformation(XynaCredentials credentials, Long batchProcessId) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.TIME_CONTROLLED_ORDER, action=Action.read, checks=1)
  public BatchProcessSearchResult searchBatchProcesses(XynaCredentials credentials, BatchProcessSelectImpl select, int maxRows) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.TIME_CONTROLLED_ORDER, action=Action.kill, checks=1)
  public boolean cancelBatchProcess(XynaCredentials credentials, Long batchProcessId, CancelMode cancelMode) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.TIME_CONTROLLED_ORDER, action=Action.disable, checks=1)
  public boolean pauseBatchProcess(XynaCredentials credentials, Long batchProcessId) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.TIME_CONTROLLED_ORDER, action=Action.enable, checks=1)
  public boolean continueBatchProcess(XynaCredentials credentials, Long batchProcessId) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.TIME_CONTROLLED_ORDER, action=Action.write, checks={1,2})
  public boolean modifyBatchProcess(XynaCredentials credentials, Long batchProcessId, BatchProcessInput input) throws XynaException, RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public String saveMDM(XynaCredentials credentials, String xml, boolean override, String user) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public String saveMDM(XynaCredentials credentials, String xml, boolean override, String user, RuntimeContext runtimeContext) throws XynaException, RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void deleteXMOMObject(XynaCredentials credentials, XMOMType type, String originalFqName, boolean recursivlyUndeploy, boolean recursivlyDelete, String user) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void deleteXMOMObject(XynaCredentials credentials, XMOMType type, String originalFqName, boolean recursivlyUndeploy, boolean recursivlyDelete, String user, RuntimeContext runtimeContext) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void deleteXMOMObject(XynaCredentials credentials, XMOMType type, String originalFqName, DependentObjectMode dependentObjectMode, String user, RuntimeContext runtimeContext) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public void addTimeWindow(XynaCredentials credentials, TimeConstraintWindowDefinition definition) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public void removeTimeWindow(XynaCredentials credentials, String name, boolean force) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public void changeTimeWindow(XynaCredentials credentials, TimeConstraintWindowDefinition definition) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public List<FactoryNodeStorable> getAllFactoryNodes(XynaCredentials credentials) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Collection<SharedLib> listAllSharedLibs(XynaCredentials credentials) throws RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Collection<SharedLib> listSharedLibs(XynaCredentials credentials, RuntimeContext runtimeContext, boolean withContent) throws RemoteException;
}
