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



import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xclusteringservices.ClusterInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyAccess;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyRight;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xfmg.xods.configuration.PropertyMap;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSearchResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSelect;
import com.gip.xyna.xprc.CustomStringContainer;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSearchResult;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSelect;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ExtendedManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionSelect;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.SearchMode;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceResult;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSearchResult;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSelectImpl;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.healthmarketscience.rmiio.RemoteInputStream;



public interface XynaRMIChannel extends XynaRMIChannelBase {

  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public Long sessionStartOrder(String sessionId, String token, GeneralXynaObject payload, String orderType, int prio,
                                String custom0, String custom1, String custom2, String custom3) throws RemoteException;


  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public Long sessionStartOrder(String sessionId, String token, GeneralXynaObject payload, String orderType, int prio)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public GeneralXynaObject sessionStartOrderSynchronously(String sessionId, String token, GeneralXynaObject payload,
                                                          String orderType, int prio, String custom0, String custom1,
                                                          String custom2, String custom3) throws RemoteException;


  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public GeneralXynaObject sessionStartOrderSynchronously(String sessionId, String token, GeneralXynaObject payload,
                                                          String orderType, int prio) throws RemoteException;

  @ProxyAccess(right = ProxyRight.TRIGGER_FILTER_MANAGEMENT)
  public void sessionAddTrigger(String sessionId, String token, String name, RemoteInputStream jarFiles,
                                String fqTriggerClassName, String[] sharedLibs, String description,
                                String startParameterDocumentation, long revision) throws RemoteException;


  @ProxyAccess(right = ProxyRight.TRIGGER_FILTER_MANAGEMENT)
  public void sessionDeployTrigger(String sessionId, String token, String nameOfTrigger, String nameOfTriggerInstance,
                                   String[] startParameter, String description, long revision) throws RemoteException;


  @ProxyAccess(right = ProxyRight.TRIGGER_FILTER_MANAGEMENT)
  public void sessionAddFilter(String sessionId, String token, String filterName, RemoteInputStream jarFiles,
                               String fqFilterClassName, String triggerName, String[] sharedLibs, String description, long revision)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.TRIGGER_FILTER_MANAGEMENT)
  public void sessionDeployFilter(String sessionId, String token, String filtername, String nameOfFilterInstance,
                                  String nameOfTriggerInstance, String description, long revision) throws RemoteException;


  @Deprecated
  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.insert, checks=4)
  public Long sessionStartCronLikeOrder(String sessionId, String token, String label, String payload, String orderType,
                                        Long startTime, Long interval, boolean enabled, String onError)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.insert, checks=4)
  public Long sessionStartCronLikeOrder(String sessionId, String token, String label, String payload, String orderType,
                                        Long startTime, String timeZoneId, Long interval, boolean useDST,
                                        boolean enabled, String onError, String cloCustom0, String cloCustom1,
                                        String cloCustom2, String cloCustom3) throws RemoteException;


  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.insert, checks=4)
  public Long sessionStartCronLikeOrder(String sessionId, String token, String label, String payload, String orderType,
                                        Calendar startTimeWithTimeZone, Long interval, boolean useDST, boolean enabled,
                                        String onError, String cloCustom0, String cloCustom1, String cloCustom2,
                                        String cloCustom3) throws RemoteException;
  

  @Deprecated
  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.insert, checks=3)
  public CronLikeOrderInformation sessionStartCronLikeOrder(String sessionId, String token, GeneralXynaObject payload,
                                                            String orderType, Long startTime, Long interval)
      throws RemoteException, XynaException;


  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.insert, checks=3)
  public CronLikeOrderInformation sessionStartCronLikeOrder(String sessionId, String token, GeneralXynaObject payload,
                                                            String orderType, Long startTime, String timeZoneID,
                                                            Long interval, boolean useDST, String cloCustom0,
                                                            String cloCustom1, String cloCustom2, String cloCustom3)
      throws RemoteException, XynaException;


  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.insert, checks=3)
  public CronLikeOrderInformation sessionStartCronLikeOrder(String sessionId, String token, GeneralXynaObject payload,
                                                            String orderType, Calendar startTimeWithTimeZone,
                                                            Long interval, boolean useDST, String cloCustom0,
                                                            String cloCustom1, String cloCustom2, String cloCustom3)
      throws RemoteException, XynaException;


  @Deprecated
  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.write, checks=5)
  public CronLikeOrderInformation sessionModifyCronLikeOrder(String sessionId, String token, Long id, String label,
                                                             String payload, String orderType, Long startTime,
                                                             Long interval, boolean enabled, String onError)
      throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.write, checks=5)
  public CronLikeOrderInformation sessionModifyCronLikeOrder(String sessionId, String token, Long id, String label,
                                                             String payload, String orderType, Long startTime,
                                                             String timeZoneID, Long interval, boolean useDST,
                                                             boolean enabled, String onError, String cloCustom0,
                                                             String cloCustom1, String cloCustom2, String cloCustom3)
      throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.write, checks={5,16,17})
  public CronLikeOrderInformation sessionModifyCronLikeOrder(String sessionId, String token, Long id, String label,
                                                             String payload, String orderType, Long startTime,
                                                             String timeZoneID, Long interval, boolean useDST,
                                                             boolean enabled, String onError, String cloCustom0,
                                                             String cloCustom1, String cloCustom2, String cloCustom3,
                                                             String applicationName, String versionName)
      throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.write, checks=5)
  public CronLikeOrderInformation sessionModifyCronLikeOrder(String sessionId, String token, Long id, String label,
                                                             String payload, String orderType,
                                                             Calendar startTimeWithTimeZone, Long interval,
                                                             boolean useDST, boolean enabled, String onError,
                                                             String cloCustom0, String cloCustom1, String cloCustom2,
                                                             String cloCustom3) throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.delete, checks=2)
  public boolean sessionRemoveCronLikeOrder(String sessionId, String token, Long id) throws RemoteException;


  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.read, nochecks=true)
  public Map<Long, CronLikeOrderInformation> sessionListCronLikeOrders(String sessionId, String token, int maxRows)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.VIEW_MANUAL_INTERACTION)
  public List<ManualInteractionEntry> sessionListManualInteractionEntries(String sessionId, String token)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.PROCESS_MANUAL_INTERACTION)
  public void sessionProcessManualInteractionEntry(String sessionId, String token, Long id, String response)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public boolean sessionHasRight(String sessionId, String token, String rightName) throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDERARCHIVE_VIEW)
  public OrderInstanceResult sessionSearch(String sessionId, String token, OrderInstanceSelect select, int maxRows)
      throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.read, checks=2)
  public CronLikeOrderSearchResult sessionSearchCronLikeOrders(String sessionId, String token, CronLikeOrderSelectImpl selectCron, int maxRows) throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDERARCHIVE_VIEW)
  public OrderInstanceResult sessionSearchOrderInstances(String sessionId, String token, OrderInstanceSelect select,
                                                         int maxRows, SearchMode searchMode) throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDERARCHIVE_DETAILS)
  public String sessionGetCompleteOrder(String sessionId, String token, long id) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Triple<String, String, String> sessionGetAuditWithApplicationAndVersion(String sessionId, String token, long id) throws RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public List<String> sessionGetMDMs(String sessionId, String token) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public List<String> sessionGetMDMs(String sessionId, String token, String application, String version) throws RemoteException;

  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT_EDIT_OWN)
  public boolean sessionChangePassword(String sessionId, String token, String id, String oldPassword, String newPassword, boolean isNewPasswordHashed)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionChangeRole(String sessionId, String token, String id, String name) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionCreateRight(String sessionId, String token, String rightName) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionCreateRole(String sessionId, String token, String name, String domain) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionCreateUser(String sessionId, String token, String id, String roleName, String newPassword,
                                   boolean isPassHashed) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
 public boolean sessionDeleteRight(String sessionId, String token, String rightName) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionDeleteRole(String sessionId, String token, String name, String domain) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionDeleteUser(String sessionId, String token, String id) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Collection<Right> sessionGetRights(String sessionId, String token) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Collection<Right> sessionGetRights(String sessionId, String token, String language) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Collection<Role> sessionGetRoles(String sessionId, String token) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Collection<User> sessionGetUser(String sessionId, String token) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionGrantRightToRole(String sessionId, String token, String roleName, String right)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionSetPassword(String sessionId, String token, String id, String newPassword)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionRevokeRightFromRole(String sessionId, String token, String roleName, String right)
      throws RemoteException;

  @Deprecated
  //ServiceDestinations are not deserializable outside the factory
  @ProxyAccess(right = ProxyRight.DISPATCHER_MANAGEMENT)
  public Map<DestinationKey, DestinationValue> sessionGetDestinations(String sessionId, String token, String dispatcher)
      throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DISPATCHER_MANAGEMENT)
  public List<DispatcherEntry> sessionListDestinations(String sessionId, String token, String dispatcher)
      throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DISPATCHER_MANAGEMENT)
  public void sessionRemoveDestination(String sessionId, String token, String dispatcher, String dk)
      throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DISPATCHER_MANAGEMENT)
  public void sessionSetDestination(String sessionId, String token, String dispatcher, String dk, String dv)
      throws XynaException, RemoteException;

  @Deprecated
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void sessionDeployDatatype(String sessionId, String token, String xml, Map<String, byte[]> libraries)
      throws XynaException, RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void sessionDeployException(String sessionId, String token, String xml) throws XynaException, RemoteException;

  @Deprecated
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void sessionDeployWorkflow(String sessionId, String token, String fqClassName) throws XynaException,
      RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public HashMap<String, DeploymentStatus> sessionListDeploymentStatuses(String sessionId, String token)
      throws XynaException, RemoteException;

  @Deprecated
  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public String sessionSaveMDM(String sessionId, String token, String xml) throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.VIEW_MANUAL_INTERACTION)
  public ManualInteractionResult sessionSearchManualInteractions(String sessionId, String token,
                                                                 ManualInteractionSelect selectMI, int maxRows)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.VIEW_MANUAL_INTERACTION)
  public ExtendedManualInteractionResult sessionSearchExtendedManualInteractions(String sessionId, String token,
                                                                                 ManualInteractionSelect selectMI,
                                                                                 int maxRows) throws RemoteException;


  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public Long sessionStartOrder(String sessionId, String token, String payload, String orderType, int prio,
                                Long relativeTimeout, CustomStringContainer customs) throws XynaException,
      RemoteException;

  @ProxyAccess(right = ProxyRight.START_ORDER, checks=3)
  public List<String> sessionStartOrderSynchronously(String sessionId, String token, String payload, String orderType,
                                                     int prio, Long relativeTimeout, CustomStringContainer customs)
      throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.PUBLIC)
  public OrderInstanceDetails sessionGetOrderInstanceDetails(String sessionid, String token, long id)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public String[] sessionScanLogForLinesOfOrder(String sessionid, String token, long orderId, int lineOffset,
                                                int maxNumberOfLines, String... excludes) throws XynaException,
      RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public String sessionRetrieveLogForOrder(String sessionid, String token, long orderId, int lineOffset,
                                           int maxNumberOfLines, String... excludes) throws XynaException,
      RemoteException;

  @ProxyAccess(right = ProxyRight.FREQUENCY_CONTROL_MANAGEMENT)
  public long sessionStartFrequencyControlledTask(String sessionid, String token,
                                                  FrequencyControlledTaskCreationParameter creationParameter)
      throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.FREQUENCY_CONTROL_MANAGEMENT)
  public boolean sessionCancelFrequencyControlledTask(String sessionid, String token, long taskId)
      throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.FREQUENCY_CONTROL_VIEW)
  public FrequencyControlledTaskInformation sessionGetFrequencyControlledTaskInformation(String sessionid,
                                                                                         String token, long taskId)
      throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.FREQUENCY_CONTROL_VIEW)
  public FrequencyControlledTaskInformation sessionGetFrequencyControlledTaskInformation(String sessionid,
                                                                                         String token, long taskId,
                                                                                         String[] selectedStatistics)
      throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public User sessionGetUser(String sessionid, String token, String useridentifier) throws RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Role sessionGetRole(String sessionid, String token, String rolename, String domainname) throws RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Right sessionGetRight(String sessionid, String token, String rightidentifier) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Right sessionGetRight(String sessionid, String token, String rightidentifier, String language) throws RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Domain sessionGetDomain(String sessionid, String password, String domainidentifier) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionSetLockedStateOfUser(String sessionid, String token, String useridentifier, boolean newState)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionSetDomainsOfUser(String sessionid, String token, String useridentifier, List<String> domains)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionSetDescriptionOfRole(String sessionid, String token, String rolename, String domainname,
                                             String newDescription) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionSetAliasOfRole(String sessionid, String token, String rolename, String domainname,
                                       String newAlias) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public UserSearchResult sessionSearchUsers(String sessionid, String token, UserSelect selection, int maxRows)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.FREQUENCY_CONTROL_VIEW)
  public FrequencyControlledTaskSearchResult sessionSearchFrequencyControlledTasks(String sessionid,
                                                                                   String token,
                                                                                   FrequencyControlledTaskSelect selection,
                                                                                   int maxRows) throws RemoteException;


  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Collection<Domain> sessionGetDomains(String sessionid, String token) throws RemoteException;


  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionCreateDomain(String sessionid, String token, String domainidentifier, DomainType type,
                                     int maxRetries, int connectionTimeout) throws RemoteException;


  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionSetDomainSpecificDataOfDomain(String sessionid, String token, String domainidentifier,
                                                      DomainTypeSpecificData specificData) throws RemoteException;


  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionSetDescriptionOfRight(String sessionid, String token, String rightidentifier, String description)
      throws RemoteException;
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionSetDescriptionOfRight(String sessionid, String token, String rightidentifier, String description, String language)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionSetMaxRetriesOfDomain(String sessionid, String token, String domainidentifier, int maxRetries)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionSetConnectionTimeoutOfDomain(String sessionid, String token, String domainidentifier,
                                                     int connectionTimeout) throws RemoteException;


  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionSetDescriptionOfDomain(String sessionid, String token, String domainidentifier,
                                               String description) throws RemoteException;


  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean sessionDeleteDomain(String sessionid, String token, String domainidentifier) throws RemoteException;


  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void sessionUndeployDatatype(String sessionid, String token, String originalFqName, boolean recursivly)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void sessionUndeployException(String sessionid, String token, String originalFqName, boolean recursivly)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MDM)
  public void sessionUndeployWorkflow(String sessionid, String token, String originalFqName, boolean recursivly)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void sessionDeleteDatatype(String sessionid, String token, String originalFqName, boolean recursivlyUndeploy,
                                    boolean recursivlyDelete) throws RemoteException;


  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void sessionDeleteException(String sessionid, String token, String originalFqName, boolean recursivlyUndeploy,
                                     boolean recursivlyDelete) throws RemoteException;


  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void sessionDeleteWorkflow(String sessionid, String token, String originalFqName, boolean recursivlyUndeploy,
                                    boolean recursivlyDelete) throws RemoteException;


  @ProxyAccess(right = ProxyRight.PUBLIC)
  public XMOMDatabaseSearchResult sessionSearchXMOMDatabase(String sessionid, String token,
                                                            List<XMOMDatabaseSelect> selects, int maxRows)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.PUBLIC)
  public ExtendedCapacityUsageInformation listExtendedCapacityInformation(String sessionid, String token)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.read, checks=2)
  public CapacityInformation sessionGetCapacityInformation(String sessionid, String token, String capacityName)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.insert, checks=2)
  public boolean sessionAddCapacity(String sessionid, String token, String name, int cardinality,
                                    CapacityManagement.State state) throws RemoteException;


  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.delete, checks=2)
  public boolean sessionRemoveCapacity(String sessionid, String token, String name) throws RemoteException;


  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.write, checks=2)
  public boolean sessionChangeCapacityName(String sessionid, String token, String name, String newName)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.write, checks=2)
  public boolean sessionChangeCapacityCardinality(String sessionid, String token, String name, int cardinality)
      throws RemoteException;


  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.write, checks=2)
  public boolean sessionChangeCapacityState(String sessionid, String token, String name, CapacityManagement.State state)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.write, checks=2)
  public boolean sessionRequireCapacityForWorkflow(String sessionid, String token, String workflowFqName,
                                                   String capacityName, int capacityCardinality) throws RemoteException;


  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.write, checks=2)
  public boolean sessionRemoveCapacityForWorkflow(String sessionid, String token, String workflowFqName,
                                                  String capacityName) throws RemoteException;


  @ProxyAccess(right = ProxyRight.PUBLIC)
  public List<CapacityMappingStorable> sessionGetAllCapacityMappings(String sessionid, String token)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.CAPACITY, action=Action.read, nochecks=true)
  public ExtendedCapacityUsageInformation sessionListExtendedCapacityInformation(String sessionid, String token)
      throws RemoteException;

  
  @ProxyAccess(right = ProxyRight.VETO, action=Action.read, nochecks=true)
  public Collection<VetoInformationStorable> sessionListVetoInformation(String sessionid, String token)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.DISPATCHER_MANAGEMENT)
  public DispatcherEntry sessionGetDestination(String sessionid, String token, String dispatcher, String destinationkey)
      throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.read, checks=2)
  public String sessionGetProperty(String sessionid, String token, String key) throws RemoteException;

  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.read, nochecks=true)
  public PropertyMap<String, String> sessionGetProperties(String sessionid, String token) throws RemoteException;

  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.read, nochecks=true)
  public Collection<XynaPropertyWithDefaultValue> sessionGetPropertiesWithDefaultValuesReadOnly(String sessionid, String token) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.write, checks=2)
  public void sessionSetProperty(String sessionid, String token, String key, String value) throws RemoteException;

  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.write, checks=2)
  public void sessionSetProperty(String sessionid, String token, XynaPropertyWithDefaultValue property) throws RemoteException;

  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.delete, checks=2)
  public void sessionRemoveProperty(String sessionid, String token, String key) throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.insert, checks=2)
  public void sessionCreateOrdertype(String sessionid, String token, OrdertypeParameter ordertypeParameter)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.write, checks=2)
  public void sessionModifyOrdertype(String sessionid, String token, OrdertypeParameter ordertypeParameter)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.delete, checks=2)
  public void sessionDeleteOrdertype(String sessionid, String token, OrdertypeParameter ordertypeParameter)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.read, nochecks=true)
  public List<OrdertypeParameter> sessionListOrdertypes(String sessionid, String token) throws RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public boolean sessionIsSessionAlive(String sessionid, String token, String otherSessionId) throws RemoteException;

  @ProxyAccess(right = ProxyRight.VETO, action=Action.insert, checks=2)
  public void sessionAllocateAdministrativeVeto(String sessionid, String token, String vetoName, String documentation)
      throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.VETO, action=Action.delete, checks=2)
  public void sessionFreeAdministrativeVeto(String sessionid, String token, String vetoName) throws XynaException,
      RemoteException;

  @ProxyAccess(right = ProxyRight.VETO, action=Action.write, checks=2)
  public void sessionSetDocumentationOfAdministrativeVeto(String sessionid, String token, String vetoName, String documentation) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.VETO, action=Action.read, nochecks=true)
  public VetoSearchResult sessionSearchVetos(String sessionid, String token, VetoSelectImpl select, int maxRows) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Map<Long, ClusterInformation> listClusterInstances() throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Collection<ApplicationInformation> sessionListApplications(String sessionid, String token) throws RemoteException;
  

  @Deprecated
  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.insert, checks={4,9,10})
  public Long sessionStartCronLikeOrder(String sessionid, String token, String label, String payload, String orderType,
                                        Long startTime, Long interval, boolean enabled, String onError,
                                        String application, String version) throws RemoteException;


  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.insert, checks={4,15,16})
  public Long sessionStartCronLikeOrder(String sessionid, String token, String label, String payload, String orderType,
                                        Long startTime, String timeZoneID, Long interval, boolean useDST,
                                        boolean enabled, String onError, String cloCustom0, String cloCustom1,
                                        String cloCustom2, String cloCustom3, String application, String version)
      throws RemoteException;

  @ProxyAccess(right = ProxyRight.CRON_LIKE_ORDER, action=Action.insert, checks={4,14,15})
  public Long sessionStartCronLikeOrder(String sessionid, String token, String label, String payload, String orderType,
                                        Calendar startTimeWithTimeZone, Long interval, boolean useDST, boolean enabled,
                                        String onError, String cloCustom0, String cloCustom1, String cloCustom2,
                                        String cloCustom3, String application, String version) throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.read, checks=2)
  public List<Capacity> sessionListCapacitiesForOrdertype(String sessionid, String token, String ordertype) throws RemoteException;


}
