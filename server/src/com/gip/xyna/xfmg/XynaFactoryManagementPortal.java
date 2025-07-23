/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateSessionException;
import com.gip.xyna.xfmg.exceptions.XFMG_ErrorScanningLogFile;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToAddObjectToApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCapacityCardinality;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCreationOfExistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidModificationOfUnexistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidXynaOrderPriority;
import com.gip.xyna.xfmg.exceptions.XFMG_NameContainsInvalidCharacter;
import com.gip.xyna.xfmg.exceptions.XFMG_NamingConventionException;
import com.gip.xyna.xfmg.exceptions.XFMG_PasswordRestrictionViolation;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RightDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_SESSION_AUTHENTICATION_FAILED;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownSessionIDException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.statistics.XynaStatistics;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueFacade;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfmon.XynaFactoryMonitoring;
import com.gip.xyna.xfmg.xfmon.processmonitoring.ProcessMonitoring;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.PropertyMap;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.SearchOrdertypeParameter;
import com.gip.xyna.xfmg.xods.priority.PrioritySetting;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
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


public interface XynaFactoryManagementPortal {

  public String getProperty(String key);

  public XynaPropertyWithDefaultValue getPropertyWithDefaultValue(String key);

  public PropertyMap<String, String> getPropertiesReadOnly();
  
  
  public  Collection<XynaPropertyWithDefaultValue> getPropertiesWithDefaultValuesReadOnly();
  
  public void setProperty(String key, String value) throws PersistenceLayerException;

  public void setProperty(XynaPropertyWithDefaultValue property) throws PersistenceLayerException;

  public void removeProperty(String key) throws PersistenceLayerException;

  
  public XynaStatistics getXynaStatistics();
  
  
  @Deprecated
  public XynaStatisticsLegacy getXynaStatisticsLegacy();
  

  public Map<Long, OrderInstance> getAllRunningProcesses(long offset, int count) throws PersistenceLayerException;


  public OrderInstanceDetails getRunningProcessDetails(Long id) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


  public Map<Long, CronLikeOrderInformation> getAllCronLikeOrders(long maxRows) throws XPRC_CronLikeSchedulerException;


  public XynaFactoryControl getXynaFactoryControl();


  public XynaFactoryMonitoring getXynaFactoryMonitoring();


  public XynaFactoryManagementODS getXynaFactoryManagementODS();


  public ProcessMonitoring getProcessMonitoring();

  @Deprecated
  public SessionCredentials getNewSession(User user, boolean force) throws PersistenceLayerException, XFMG_DuplicateSessionException;


  public SessionCredentials createSession(XynaUserCredentials credentials, Optional<String> roleName, boolean force) throws PersistenceLayerException, XFMG_DuplicateSessionException;

  
  public boolean authorizeSession(String sessionId, String token, String roleName) throws PersistenceLayerException, XFMG_UnknownSessionIDException, XFMG_SESSION_AUTHENTICATION_FAILED, XFMG_DuplicateSessionException;
  

  public void quitSession(String sessionId) throws PersistenceLayerException;


  public boolean requestSessionPriviliges(String sessionId, ASessionPrivilege privilige)
      throws PersistenceLayerException;


  public boolean releaseAllSessionPriviliges(String sessionId) throws PersistenceLayerException;


  public boolean releaseSessionPrivilige(String sessionId, ASessionPrivilege privilige)
      throws PersistenceLayerException;


  public boolean keepSessionAlive(String sessionId) throws PersistenceLayerException;


  public SessionDetails getSessionDetails(String sessionId) throws PersistenceLayerException,
      XFMG_UnknownSessionIDException;


  public Role authenticateSession(String sessionId, String token) throws PersistenceLayerException,
      XFMG_UnknownSessionIDException, XFMG_SESSION_AUTHENTICATION_FAILED;


  public boolean createUser(String id, String roleName, String password, boolean isPassHashed)
      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation, XFMG_NameContainsInvalidCharacter;


  public boolean createUser(String id, String roleName, String password, boolean isPassHashed, List<String> domains)
      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation, XFMG_NameContainsInvalidCharacter;


  public boolean importUser(String id, String roleName, String passwordhash) throws PersistenceLayerException, XFMG_NameContainsInvalidCharacter;


  public boolean deleteUser(String id) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException;


  public String listUsers() throws PersistenceLayerException;


  public boolean changePassword(String id, String oldPassword, String newPassword, boolean isNewPasswordHashed) throws PersistenceLayerException,
      XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException, XFMG_UserDoesNotExistException, XFMG_PasswordRestrictionViolation;


  public User authenticate(String id, String password) throws XFMG_UserAuthenticationFailedException,
      XFMG_UserIsLockedException, PersistenceLayerException;


  public User authenticateHashed(String id, String password) throws XFMG_UserAuthenticationFailedException,
      XFMG_UserIsLockedException, PersistenceLayerException;


  public boolean usersExists(String id) throws PersistenceLayerException;


  public boolean resetPassword(String id, String newPassword) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_PasswordRestrictionViolation;


  public boolean setPassword(String id, String password) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_PasswordRestrictionViolation;


  public boolean setPasswordHash(String id, String passwordhash) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException;


  public boolean hasRight(String methodName, Role role) throws PersistenceLayerException;


  public boolean hasRight(String methodName, String role) throws PersistenceLayerException;


  public String resolveFunctionToRight(String methodName);


  public boolean changeRole(String id, String name) throws PersistenceLayerException,
      XFMG_PredefinedXynaObjectException, XFMG_UserDoesNotExistException, XFMG_RoleDoesNotExistException;


  public boolean createRole(String name, String domain) throws PersistenceLayerException,
      XFMG_DomainDoesNotExistException, XFMG_NameContainsInvalidCharacter;


  public boolean deleteRole(String name, String domain) throws PersistenceLayerException,
      XFMG_PredefinedXynaObjectException, XFMG_RoleIsAssignedException;


  public boolean grantRightToRole(String roleName, String right) throws PersistenceLayerException,
      XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException;


  public boolean revokeRightFromRole(String roleName, String right) throws PersistenceLayerException,
      XFMG_RoleDoesNotExistException, XFMG_RightDoesNotExistException;


  public boolean createRight(String rightName) throws PersistenceLayerException, XFMG_NamingConventionException, XFMG_NameContainsInvalidCharacter;


  public boolean deleteRight(String rightName) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException,
      XFMG_RightDoesNotExistException;


  public Collection<Right> getRights(String language) throws PersistenceLayerException;
  
  
  public Collection<RightScope> getRightScopes(String language) throws PersistenceLayerException;


  public Collection<Role> getRoles() throws PersistenceLayerException;


  public Collection<User> getUser() throws PersistenceLayerException;


  public boolean isPredefined(PredefinedCategories category, String id);


  public void listenToMdmModifications(String sessionId, ANotificationConnection con) throws PersistenceLayerException;


  public void listenToProcessProgress(String sessionId, ANotificationConnection con, Long orderId)
      throws PersistenceLayerException;


  public String[] scanLogForLinesOfOrder(long orderId, int lineOffset, int maxNumberOfLines, String... excludes)
      throws XFMG_ErrorScanningLogFile;


  public String retrieveLogForOrder(long orderId, int lineOffset, int maxNumberOfLines, String... excludes)
      throws XFMG_ErrorScanningLogFile;


  public User getUser(String useridentifier) throws PersistenceLayerException;


  public Role getRole(String rolename, String domainname) throws PersistenceLayerException;


  public Right getRight(String rightidentifier, String language) throws PersistenceLayerException;


  public Domain getDomain(String domainidentifier) throws PersistenceLayerException;


  public boolean setLockedStateOfUser(String useridentifier, boolean newState) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_PredefinedXynaObjectException;


  public boolean setDomainsOfUser(String useridentifier, List<String> domains) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_DomainDoesNotExistException;


  public boolean setDescriptionOfRole(String roleidentifier, String domainname, String newDescription)
      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException;


  public boolean setAliasOfRole(String rolename, String domainname, String newAlias) throws PersistenceLayerException,
      XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException;


  public UserSearchResult searchUsers(UserSelect selection, int maxRows) throws PersistenceLayerException;


  public Collection<Domain> getDomains() throws PersistenceLayerException;

  public PasswordExpiration getPasswordExpiration(String userName) throws PersistenceLayerException;

  public boolean createDomain(String domainidentifier, DomainType type, int maxRetries, int connectionTimeout)
      throws PersistenceLayerException, XFMG_NameContainsInvalidCharacter;


  public boolean setDomainSpecificDataOfDomain(String domainidentifier, DomainTypeSpecificData specificData)
      throws PersistenceLayerException, XFMG_DomainDoesNotExistException;


  public boolean setDescriptionOfDomain(String domainidentifier, String description) throws PersistenceLayerException,
      XFMG_DomainDoesNotExistException, XFMG_PredefinedXynaObjectException;


  public boolean setDescriptionOfRight(String rightidentifier, String description, String language) throws XynaException;


  public boolean setMaxRetriesOfDomain(String domainidentifier, int maxRetries) throws PersistenceLayerException,
      XFMG_DomainDoesNotExistException;


  public boolean setConnectionTimeoutOfDomain(String domainidentifier, int connectionTimeout)
      throws PersistenceLayerException, XFMG_DomainDoesNotExistException;


  public boolean deleteDomain(String domainidentifier) throws PersistenceLayerException,
      XFMG_PredefinedXynaObjectException, XFMG_DomainDoesNotExistException, XFMG_DomainIsAssignedException;


  public String listDomains() throws PersistenceLayerException;


  public List<Domain> getDomainsForUser(String useridentifier) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_DomainDoesNotExistException;


  public void createOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
      XFMG_InvalidCreationOfExistingOrdertype, XFMG_FailedToAddObjectToApplication;


  public void modifyOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
      XFMG_InvalidModificationOfUnexistingOrdertype, XFMG_InvalidCapacityCardinality;


  public void deleteOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException;


  public List<OrdertypeParameter> listOrdertypes(RuntimeContext runtimeContext) throws PersistenceLayerException;
  
  
  public List<OrdertypeParameter> listOrdertypes(SearchOrdertypeParameter sop) throws PersistenceLayerException;


  public void registerQueue(String uniqueName, String externalName, QueueType queueType, QueueConnectData connectData)
      throws PersistenceLayerException;


  public void deregisterQueue(String uniqueName) throws PersistenceLayerException;


  public Collection<QueueFacade> listQueues() throws PersistenceLayerException;
  
  
  public boolean isSessionAlive(String sessionId) throws PersistenceLayerException;
  
  
  public Collection<PrioritySetting> listPriorities() throws PersistenceLayerException;
  
  
  public void removePriority(String orderType) throws PersistenceLayerException;
  

  public void setPriority(String orderType, int priority) throws XFMG_InvalidXynaOrderPriority,
      PersistenceLayerException;


  public void setPriority(String orderType, int priority, Long revision) throws XFMG_InvalidXynaOrderPriority,
      PersistenceLayerException;


  public void discoverPriority(XynaOrderServerExtension xo);
  
  
  public Integer getPriority(String orderType) throws PersistenceLayerException;

}
