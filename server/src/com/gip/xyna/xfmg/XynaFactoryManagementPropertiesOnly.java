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

package com.gip.xyna.xfmg;



import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_ErrorScanningLogFile;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCreationOfExistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidModificationOfUnexistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidXynaOrderPriority;
import com.gip.xyna.xfmg.exceptions.XFMG_NamingConventionException;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RightDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_SESSION_AUTHENTICATION_FAILED;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownSessionIDException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.statistics.XynaStatistics;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfmon.XynaFactoryMonitoring;
import com.gip.xyna.xfmg.xfmon.processmonitoring.ProcessMonitoring;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.components.Components;
import com.gip.xyna.xfmg.xods.configuration.PropertyMap;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.SearchOrdertypeParameter;
import com.gip.xyna.xfmg.xods.priority.PrioritySetting;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.XynaOperatorControl;
import com.gip.xyna.xfmg.xopctrl.managedsessions.ASessionPrivilege;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionDetails;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.ANotificationConnection;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.PasswordExpiration;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.PredefinedCategories;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaUserCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSearchResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSelect;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;



public class XynaFactoryManagementPropertiesOnly extends XynaFactoryManagementBase {


  private static final String ILLEGAL_STATE_MESSAGE = "No Xyna Factory is running. Only getProperty(), setProperty() and removeProperty() may be invoked.";

  private final Map<String, String> propertyMap;


  public XynaFactoryManagementPropertiesOnly() throws XynaException {
    this(new HashMap<String, String>());
  }


  public XynaFactoryManagementPropertiesOnly(Map<String, String> propertyMap) throws XynaException {
    this.propertyMap = propertyMap;
  }


  public String getProperty(String key) {
    return propertyMap.get(key);
  }


  public PropertyMap<String, String> getPropertiesReadOnly() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }
  
  public  Collection<XynaPropertyWithDefaultValue> getPropertiesWithDefaultValuesReadOnly() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void setProperty(String key, String value) throws PersistenceLayerException {
    this.propertyMap.put(key, value);
  }

  /**
   * Setzt Key und Value einer Property. Die Documentation wird nicht uebernommen.
   */
  public void setProperty(XynaPropertyWithDefaultValue property) throws PersistenceLayerException {
    this.propertyMap.put(property.getName(), property.getValue());
  }


  public void removeProperty(String key) throws PersistenceLayerException {
    this.propertyMap.remove(key);
  }


  public XynaStatistics getXynaStatistics() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }
  
  
  public XynaStatisticsLegacy getXynaStatisticsLegacy() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Map<Long, OrderInstance> getAllRunningProcesses(long offset, int count) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public OrderInstanceDetails getRunningProcessDetails(Long id) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Map<Long, CronLikeOrderInformation> getAllCronLikeOrders(long maxRows) throws XPRC_CronLikeSchedulerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaFactoryMonitoring getXynaFactoryMonitoring() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaFactoryManagementODS getXynaFactoryManagementODS() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public ProcessMonitoring getProcessMonitoring() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void quitSession(String sessionId) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean requestSessionPriviliges(String sessionId, ASessionPrivilege privilige)
                  throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean releaseAllSessionPriviliges(String sessionId) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean releaseSessionPrivilige(String sessionId, ASessionPrivilege privilige)
                  throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean keepSessionAlive(String sessionId) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public SessionDetails getSessionDetails(String sessionId) throws PersistenceLayerException,
                  XFMG_UnknownSessionIDException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Role authenticateSession(String sessionId, String token) throws PersistenceLayerException,
                  XFMG_UnknownSessionIDException, XFMG_SESSION_AUTHENTICATION_FAILED {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean createUser(String id, String roleName, String password, boolean isPassHashed)
                  throws PersistenceLayerException, XFMG_RoleDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean importUser(String id, String roleName, String passwordhash) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean deleteUser(String id) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public String listUsers() throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean changePassword(String id, String oldPassword, String newPassword, boolean isNewPasswordHashed) throws PersistenceLayerException,
                  XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException, XFMG_UserDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public User authenticate(String id, String password) throws XFMG_UserAuthenticationFailedException,
                  XFMG_UserIsLockedException, PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public User authenticateHashed(String id, String password) throws XFMG_UserAuthenticationFailedException,
                  XFMG_UserIsLockedException, PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean usersExists(String id) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean resetPassword(String id, String newPassword) throws PersistenceLayerException,
                  XFMG_UserDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean setPassword(String id, String password) throws PersistenceLayerException,
                  XFMG_UserDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean setPasswordHash(String id, String passwordhash) throws PersistenceLayerException,
                  XFMG_UserDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean hasRight(String methodName, Role role) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean hasRight(String methodName, String role) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public String resolveFunctionToRight(String methodName) {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean changeRole(String id, String name) throws PersistenceLayerException,
                  XFMG_PredefinedXynaObjectException, XFMG_UserDoesNotExistException, XFMG_RoleDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean createRole(String name, String domain) throws PersistenceLayerException,
                  XFMG_DomainDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean deleteRole(String name, String domain) throws PersistenceLayerException,
                  XFMG_PredefinedXynaObjectException, XFMG_RoleIsAssignedException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public String listRoles() throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean grantRightToRole(String roleName, String right) throws PersistenceLayerException,
                  XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean revokeRightFromRole(String roleName, String right) throws PersistenceLayerException,
                  XFMG_RoleDoesNotExistException, XFMG_RightDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean createRight(String rightName) throws PersistenceLayerException, XFMG_NamingConventionException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean deleteRight(String rightName) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException,
                  XFMG_RightDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Collection<Right> getRights(String language) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Collection<Role> getRoles() throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Collection<User> getUser() throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }

  
  public PasswordExpiration getPasswordExpiration(String userName) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean isPredefined(PredefinedCategories category, String id) {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void listenToMdmModifications(String sessionId, ANotificationConnection con) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void listenToProcessProgress(String sessionId, ANotificationConnection con, Long orderId)
                  throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public String[] scanLogForLinesOfOrder(long orderId, int lineOffset, int maxNumberOfLines, String... excludes)
                  throws XFMG_ErrorScanningLogFile {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);

  }


  public String retrieveLogForOrder(long orderId, int lineOffset, int maxNumberOfLines, String... excludes)
                  throws XFMG_ErrorScanningLogFile {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public User getUser(String useridentifier) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Role getRole(String rolename, String domainname) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Right getRight(String rightidentifier, String language) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Domain getDomain(String domainidentifier) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean setLockedStateOfUser(String useridentifier, boolean newState) throws PersistenceLayerException,
                  XFMG_UserDoesNotExistException, XFMG_PredefinedXynaObjectException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean setDomainsOfUser(String useridentifier, List<String> domains) throws PersistenceLayerException,
                  XFMG_UserDoesNotExistException, XFMG_DomainDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean setDescriptionOfRole(String roleidentifier, String domainname, String newDescription)
                  throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean setAliasOfRole(String rolename, String domainname, String newAlias) throws PersistenceLayerException,
                  XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public UserSearchResult searchUsers(UserSelect selection, int maxRows) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Collection<Domain> getDomains() throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean createDomain(String domainidentifier, DomainType type, int maxRetries, int connectionTimeout)
                  throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean setDomainSpecificDataOfDomain(String domainidentifier, DomainTypeSpecificData specificData)
                  throws PersistenceLayerException, XFMG_DomainDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean setDescriptionOfDomain(String domainidentifier, String description) throws PersistenceLayerException,
                  XFMG_DomainDoesNotExistException, XFMG_PredefinedXynaObjectException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean setDescriptionOfRight(String rightidentifier, String description, String language) throws XynaException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean setMaxRetriesOfDomain(String domainidentifier, int maxRetries) throws PersistenceLayerException,
                  XFMG_DomainDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean setConnectionTimeoutOfDomain(String domainidentifier, int connectionTimeout)
                  throws PersistenceLayerException, XFMG_DomainDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean deleteDomain(String domainidentifier) throws PersistenceLayerException,
                  XFMG_PredefinedXynaObjectException, XFMG_DomainDoesNotExistException, XFMG_DomainIsAssignedException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public String listDomains() throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public List<Domain> getDomainsForUser(String useridentifier) throws PersistenceLayerException,
                  XFMG_UserDoesNotExistException, XFMG_DomainDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Components getComponents() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaFactoryControl getXynaFactoryControl() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaOperatorControl getXynaOperatorControl() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public String getDefaultName() {
    return XynaFactoryManagement.DEFAULT_NAME;
  }


  protected void init() throws XynaException {
  }


  public boolean createUser(String id, String roleName, String password, boolean isPassHashed, List<String> domains)
                  throws PersistenceLayerException, XFMG_RoleDoesNotExistException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaClusteringServicesManagement getXynaClusteringServicesManagement() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void createOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
                  XFMG_InvalidCreationOfExistingOrdertype {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void modifyOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
                  XFMG_InvalidModificationOfUnexistingOrdertype {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void deleteOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public List<OrdertypeParameter> listOrdertypes() throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void registerQueue(String uniqueName, String externalName, QueueType queueType, QueueConnectData connectData)
                  throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void deregisterQueue(String uniqueName) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Collection<Queue> listQueues() throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean isSessionAlive(String sessionId) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaExtendedStatusManagement getXynaExtendedStatusManagement() {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Collection<PrioritySetting> listPriorities() throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void removePriority(String orderType) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void setPriority(String orderType, int priority) throws XFMG_InvalidXynaOrderPriority,
                  PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void discoverPriority(XynaOrderServerExtension xo) {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }

  public Integer getPriority(String orderType) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public void setPriority(String orderType, int priority, Long revision) throws XFMG_InvalidXynaOrderPriority,
      PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public Collection<RightScope> getRightScopes(String language) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public XynaPropertyWithDefaultValue getPropertyWithDefaultValue(String key) {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public SessionCredentials getNewSession(User user, boolean force) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public SessionCredentials createSession(XynaUserCredentials credentials, Optional<String> roleName, boolean force)
                  throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public boolean authorizeSession(String sessionId, String token, String roleName) throws PersistenceLayerException,
                  XFMG_UnknownSessionIDException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public List<OrdertypeParameter> listOrdertypes(RuntimeContext runtimeContext) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }


  public List<OrdertypeParameter> listOrdertypes(SearchOrdertypeParameter sop) throws PersistenceLayerException {
    throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
  }
}
