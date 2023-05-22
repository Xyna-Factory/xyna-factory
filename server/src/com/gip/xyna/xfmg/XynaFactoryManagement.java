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
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateSessionException;
import com.gip.xyna.xfmg.exceptions.XFMG_ErrorScanningLogFile;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToAddObjectToApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_IllegalPropertyValueException;
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
import com.gip.xyna.xfmg.xods.configuration.Configuration;
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
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.AChangeNotificationListener;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.ANotificationConnection;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.MdmModificationChangeListener;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.ProcessProgressChangeListener;
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



public class XynaFactoryManagement extends XynaFactoryManagementBase {

  public static final String DEFAULT_NAME = "Xyna Factory Management";

  private XynaClusteringServicesManagement clusterManagement;
  private XynaExtendedStatusManagement extendedStatusManagement;
  private XynaFactoryControl factoryControl;

  public XynaFactoryManagement() throws XynaException {
    super();
  }


  public Components getComponents() {
    return (Components) getSection(XynaFactoryControl.DEFAULT_NAME).getFunctionGroup(Components.DEFAULT_NAME);
  }


  public void init() throws XynaException {
    deploySection(new XynaFactoryManagementODS());
    deploySection(new XynaOperatorControl());
    factoryControl = new XynaFactoryControl();
    deploySection(factoryControl);
    deploySection(new XynaFactoryMonitoring());
    clusterManagement = XynaClusteringServicesManagement.getInstance();
    deploySection(clusterManagement);
    extendedStatusManagement = new XynaExtendedStatusManagement();
    deploySection(extendedStatusManagement);
  }


  public void setProperty(String key, String value) throws PersistenceLayerException {
    try {
      ((Configuration) getSection(XynaFactoryManagementODS.DEFAULT_NAME).getFunctionGroup(Configuration.DEFAULT_NAME))
                      .setProperty(key, value);
    } catch (XFMG_IllegalPropertyValueException e) {
      throw new RuntimeException(e); //TODO Abw�rtskompatibilit�t, besser deklarieren!
    }
  }


  public void setProperty(XynaPropertyWithDefaultValue property) throws PersistenceLayerException {
    try {
      ((Configuration) getSection(XynaFactoryManagementODS.DEFAULT_NAME).getFunctionGroup(Configuration.DEFAULT_NAME))
                      .setProperty(property);
    } catch (XFMG_IllegalPropertyValueException e) {
      throw new RuntimeException(e); //TODO Abw�rtskompatibilit�t, besser deklarieren!
    }
  }


  public String getProperty(String key) {
    return ((Configuration) getSection(XynaFactoryManagementODS.DEFAULT_NAME)
                    .getFunctionGroup(Configuration.DEFAULT_NAME)).getProperty(key);
  }

  public XynaPropertyWithDefaultValue getPropertyWithDefaultValue(String key) {
    return ((Configuration) getSection(XynaFactoryManagementODS.DEFAULT_NAME)
                    .getFunctionGroup(Configuration.DEFAULT_NAME)).getPropertyWithDefaultValue(key);
  }

  

  public void removeProperty(String key) throws PersistenceLayerException {
    ((Configuration) getSection(XynaFactoryManagementODS.DEFAULT_NAME).getFunctionGroup(Configuration.DEFAULT_NAME))
                    .removeProperty(key);
  }


  public PropertyMap<String, String> getPropertiesReadOnly() {
    return ((Configuration) getSection(XynaFactoryManagementODS.DEFAULT_NAME)
        .getFunctionGroup(Configuration.DEFAULT_NAME)).getPropertiesReadOnly();
  }

  
  public Collection<XynaPropertyWithDefaultValue> getPropertiesWithDefaultValuesReadOnly() {
    return ((Configuration) getSection(XynaFactoryManagementODS.DEFAULT_NAME)
        .getFunctionGroup(Configuration.DEFAULT_NAME)).getPropertiesWithDefaultValuesReadOnly();
  }

  public XynaStatistics getXynaStatistics() {
    return ((XynaStatistics) getSection(XynaFactoryManagementODS.DEFAULT_NAME)
        .getFunctionGroup(XynaStatistics.DEFAULT_NAME));
  }

  
  public XynaStatisticsLegacy getXynaStatisticsLegacy() {
    return ((XynaStatisticsLegacy) getSection(XynaFactoryManagementODS.DEFAULT_NAME)
        .getFunctionGroup(XynaStatisticsLegacy.DEFAULT_NAME));
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public XynaFactoryMonitoring getXynaFactoryMonitoring() {
    XynaFactoryMonitoring xfacmon = (XynaFactoryMonitoring) getSection(XynaFactoryMonitoring.DEFAULT_NAME);
    if (xfacmon == null)
      logger.debug("tried to access undeployed Section " + XynaFactoryMonitoring.DEFAULT_NAME);
    return xfacmon;
  }


  public XynaFactoryManagementODS getXynaFactoryManagementODS() {
    XynaFactoryManagementODS xfacmonODS = (XynaFactoryManagementODS) getSection(XynaFactoryManagementODS.DEFAULT_NAME);
    if (xfacmonODS == null)
      logger.debug("tried to access undeployed Section " + XynaFactoryManagementODS.DEFAULT_NAME);
    return xfacmonODS;
  }


  public Map<Long, CronLikeOrderInformation> getAllCronLikeOrders(long maxRows) throws XPRC_CronLikeSchedulerException {
    return ((ProcessMonitoring) getXynaFactoryMonitoring().getFunctionGroup(ProcessMonitoring.DEFAULT_NAME))
                    .getAllCronLikeOrders(maxRows);
  }


  public Map<Long, OrderInstance> getAllRunningProcesses(long offset, int count) throws PersistenceLayerException {
    return ((ProcessMonitoring) getXynaFactoryMonitoring().getFunctionGroup(ProcessMonitoring.DEFAULT_NAME))
                    .getAllRunningProcesses(offset, count);
  }


  public OrderInstanceDetails getRunningProcessDetails(Long id) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return ((ProcessMonitoring) getXynaFactoryMonitoring().getFunctionGroup(ProcessMonitoring.DEFAULT_NAME))
                    .getRunningProcessDetails(id);
  }


  public ProcessMonitoring getProcessMonitoring() {
    return (ProcessMonitoring) getSection(XynaFactoryMonitoring.DEFAULT_NAME)
                    .getFunctionGroup(ProcessMonitoring.DEFAULT_NAME);
  }


  public XynaFactoryControl getXynaFactoryControl() {
    return factoryControl;
  }


  /*
   * SessionManagment
   */
  @Deprecated
  public SessionCredentials getNewSession(User user, boolean force) throws PersistenceLayerException, XFMG_DuplicateSessionException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement()
                    .getNewSession(user, force);
  }
  
  
  public SessionCredentials createSession(XynaUserCredentials credentials, Optional<String> roleName, boolean force)
                  throws PersistenceLayerException, XFMG_DuplicateSessionException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement()
                    .createSession(credentials, roleName, force);
  }
  
  
  public boolean authorizeSession(String sessionId, String token, String roleName) throws PersistenceLayerException,
                  XFMG_UnknownSessionIDException, XFMG_SESSION_AUTHENTICATION_FAILED, XFMG_DuplicateSessionException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement()
                    .authorizeSession(sessionId, token, roleName);
  }


  public boolean releaseAllSessionPriviliges(String sessionId) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement()
                    .releaseSessionPriviliges(sessionId);
  }


  public boolean releaseSessionPrivilige(String sessionId, ASessionPrivilege privilige)
                  throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement()
                    .releaseSessionPriviliges(sessionId, privilige);
  }


  public boolean requestSessionPriviliges(String sessionId, ASessionPrivilege privilige)
                  throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement()
                    .requestSessionPriviliges(sessionId, privilige);
  }


  public boolean keepSessionAlive(String sessionId) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement()
                    .keepAlive(sessionId);
  }
  
  
  public boolean isSessionAlive(String sessionId) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement()
                    .isSessionAlive(sessionId);
  }


  public Role authenticateSession(String sessionId, String token) throws PersistenceLayerException,
                  XFMG_UnknownSessionIDException, XFMG_SESSION_AUTHENTICATION_FAILED {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement()
                    .authenticateSession(sessionId, token);
  }


  public SessionDetails getSessionDetails(String sessionId) throws PersistenceLayerException,
                  XFMG_UnknownSessionIDException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement()
                    .getSessionDetails(sessionId);
  }


  /*
   * UserManagement
   */
  public boolean createUser(String id, String roleName, String password, boolean isPassHashed)
                  throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation, XFMG_NameContainsInvalidCharacter {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .createUser(id, roleName, password, isPassHashed);
  }


  public boolean createUser(String id, String roleName, String password, boolean isPassHashed, List<String> domains)
                  throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation, XFMG_NameContainsInvalidCharacter {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .createUser(id, roleName, password, isPassHashed, domains);
  }


  public boolean importUser(String id, String roleName, String passwordhash) throws PersistenceLayerException, XFMG_NameContainsInvalidCharacter {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .importUser(id, roleName, passwordhash);
  }


  public boolean deleteUser(String id) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement().deleteUser(id);
  }


  public String listUsers() throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement().listUsers();
  }


  public boolean changePassword(String id, String oldPassword, String newPassword, boolean isNewPasswordHashed) throws PersistenceLayerException,
                  XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException, XFMG_UserDoesNotExistException, XFMG_PasswordRestrictionViolation {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .changePassword(id, oldPassword, newPassword, isNewPasswordHashed);
  }


  public User authenticate(String id, String password) throws XFMG_UserAuthenticationFailedException,
                  XFMG_UserIsLockedException, PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .authenticate(id, password);
  }


  public boolean usersExists(String id) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement().usersExists(id);
  }


  public User authenticateHashed(String id, String password) throws XFMG_UserAuthenticationFailedException,
                  XFMG_UserIsLockedException, PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .authenticateHashed(id, password);
  }


  public boolean resetPassword(String id, String newPassword) throws PersistenceLayerException,
                  XFMG_UserDoesNotExistException, XFMG_PasswordRestrictionViolation {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .resetPassword(id, newPassword);
  }


  public boolean setPassword(String id, String newPassword) throws PersistenceLayerException,
                  XFMG_UserDoesNotExistException, XFMG_PasswordRestrictionViolation {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .setPassword(id, newPassword);
  }


  public boolean setPasswordHash(String id, String newPassword) throws PersistenceLayerException,
                  XFMG_UserDoesNotExistException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .setPasswordHash(id, newPassword);
  }


  public boolean hasRight(String methodName, Role role) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .hasRight(methodName, role);
  }


  public boolean hasRight(String methodName, String role) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .hasRight(methodName, role);
  }


  public String resolveFunctionToRight(String methodName) {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .resolveFunctionToRight(methodName);
  }


  public boolean changeRole(String id, String name) throws PersistenceLayerException,
                  XFMG_PredefinedXynaObjectException, XFMG_UserDoesNotExistException, XFMG_RoleDoesNotExistException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .changeRole(id, name);
  }


  public boolean createRole(String name, String domain) throws PersistenceLayerException,
                  XFMG_DomainDoesNotExistException, XFMG_NameContainsInvalidCharacter {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement().createRole(name,
                                                                                                               domain);
  }


  public boolean deleteRole(String name, String domain) throws PersistenceLayerException,
                  XFMG_PredefinedXynaObjectException, XFMG_RoleIsAssignedException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement().deleteRole(name,
                                                                                                               domain);
  }


  public boolean grantRightToRole(String roleName, String right) throws PersistenceLayerException,
                  XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .grantRightToRole(roleName, right);
  }


  public boolean revokeRightFromRole(String roleName, String right) throws PersistenceLayerException,
                  XFMG_RoleDoesNotExistException, XFMG_RightDoesNotExistException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .revokeRightFromRole(roleName, right);
  }


  public boolean createRight(String rightName) throws PersistenceLayerException, XFMG_NamingConventionException, XFMG_NameContainsInvalidCharacter {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .createRight(rightName);
  }


  public boolean deleteRight(String rightName) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException,
                  XFMG_RightDoesNotExistException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .deleteRight(rightName);
  }


  public XynaOperatorControl getXynaOperatorControl() {
    return (XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME);
  }


  public void listenToMdmModifications(String sessionId, ANotificationConnection con) throws PersistenceLayerException {
    AChangeNotificationListener listener = new MdmModificationChangeListener(con);
    ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement()
                    .signupSessionForNotification(sessionId, listener);
  }


  public void listenToProcessProgress(String sessionId, ANotificationConnection con, Long orderId)
                  throws PersistenceLayerException {
    AChangeNotificationListener listener = new ProcessProgressChangeListener(con, orderId);
    ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement()
                    .signupSessionForNotification(sessionId, listener);
  }


  public void quitSession(String sessionId) throws PersistenceLayerException {
    ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getSessionManagement().quitSession(sessionId);
  }


  public Collection<Right> getRights(String language) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement().getRights(language);
  }


  public Collection<Role> getRoles() throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement().getRoles();
  }


  public Collection<User> getUser() throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement().getUsers();
  }


  public boolean isPredefined(PredefinedCategories category, String id) {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .isPredefined(category, id);
  }


  public String[] scanLogForLinesOfOrder(long orderId, int lineOffset, int maxNumberOfLines, String... excludes)
                  throws XFMG_ErrorScanningLogFile {
    return ((ProcessMonitoring) getXynaFactoryMonitoring().getFunctionGroup(ProcessMonitoring.DEFAULT_NAME))
                    .scanLogForLinesOfOrder(orderId, lineOffset, maxNumberOfLines, excludes);
  }


  public String retrieveLogForOrder(long orderId, int lineOffset, int maxNumberOfLines, String... excludes)
                  throws XFMG_ErrorScanningLogFile {
    return ((ProcessMonitoring) getXynaFactoryMonitoring().getFunctionGroup(ProcessMonitoring.DEFAULT_NAME))
                    .retrieveLogForOrder(orderId, lineOffset, maxNumberOfLines, excludes);
  }


  public boolean createDomain(String domainidentifier, DomainType type, int maxRetries, int connectionTimeout)
                  throws PersistenceLayerException, XFMG_NameContainsInvalidCharacter {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .createDomain(domainidentifier, type, maxRetries, connectionTimeout);
  }


  public Domain getDomain(String domainidentifier) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .getDomain(domainidentifier);
  }


  public Collection<Domain> getDomains() throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement().getDomains();
  }


  public Right getRight(String rightidentifier, String language) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .getRight(rightidentifier, language);
  }


  public Role getRole(String rolename, String domainname) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement().getRole(rolename,
                                                                                                            domainname);
  }


  public User getUser(String useridentifier) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .getUser(useridentifier);
  }


  public UserSearchResult searchUsers(UserSelect selection, int maxRows) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .searchUsers(selection, maxRows);
  }


  public boolean setAliasOfRole(String rolename, String domainname, String newAlias) throws PersistenceLayerException,
                  XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .modifyRoleFieldAlias(rolename, domainname, newAlias);
  }


  public boolean setConnectionTimeoutOfDomain(String domainidentifier, int connectionTimeout)
                  throws PersistenceLayerException, XFMG_DomainDoesNotExistException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .modifyDomainFieldConnectionTimeout(domainidentifier, connectionTimeout);
  }


  public boolean setDescriptionOfRight(String rightidentifier, String description, String language) throws XynaException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .modifyRightFieldDescription(rightidentifier, description, language);
  }


  public boolean setDescriptionOfRole(String rolename, String domainname, String newDescription)
                  throws PersistenceLayerException, XFMG_PredefinedXynaObjectException, XFMG_RoleDoesNotExistException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .modifyRoleFieldDescription(rolename, domainname, newDescription);
  }


  public boolean setDomainSpecificDataOfDomain(String domainidentifier, DomainTypeSpecificData specificData)
                  throws PersistenceLayerException, XFMG_DomainDoesNotExistException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .modifyDomainFieldDomainTypeSpecificData(domainidentifier, specificData);
  }


  public boolean setDescriptionOfDomain(String domainidentifier, String newValue) throws PersistenceLayerException,
                  XFMG_DomainDoesNotExistException, XFMG_PredefinedXynaObjectException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .modifyDomainFieldDescription(domainidentifier, newValue);
  }


  public boolean setDomainsOfUser(String useridentifier, List<String> domains) throws PersistenceLayerException,
                  XFMG_UserDoesNotExistException, XFMG_DomainDoesNotExistException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .modifyUserFieldDomains(useridentifier, domains);
  }


  public boolean setLockedStateOfUser(String useridentifier, boolean newState) throws PersistenceLayerException,
                  XFMG_UserDoesNotExistException, XFMG_PredefinedXynaObjectException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .modifyUserFieldLocked(useridentifier, newState);
  }


  public boolean setMaxRetriesOfDomain(String domainidentifier, int maxRetries) throws PersistenceLayerException,
                  XFMG_DomainDoesNotExistException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .modifyDomainFieldMaxRetries(domainidentifier, maxRetries);
  }


  public boolean deleteDomain(String domainidentifier) throws PersistenceLayerException,
                  XFMG_DomainDoesNotExistException, XFMG_PredefinedXynaObjectException, XFMG_DomainIsAssignedException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .deleteDomain(domainidentifier);
  }


  public String listDomains() throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement().listDomains();
  }


  public List<Domain> getDomainsForUser(String useridentifier) throws PersistenceLayerException,
                  XFMG_UserDoesNotExistException, XFMG_DomainDoesNotExistException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .getDomainsForUser(useridentifier);
  }

  public PasswordExpiration getPasswordExpiration(String userName) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement()
                    .getPasswordExpiration(userName);
  }
  
  
  public XynaClusteringServicesManagement getXynaClusteringServicesManagement() {
    return clusterManagement;
  }
  
  public XynaExtendedStatusManagement getXynaExtendedStatusManagement() {
    return extendedStatusManagement;
  }


  public void createOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
      XFMG_InvalidCreationOfExistingOrdertype, XFMG_FailedToAddObjectToApplication {
    getXynaFactoryManagementODS().getOrderTypeManagement().createOrdertype(ordertypeParameter);
  }


  public void modifyOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
                  XFMG_InvalidModificationOfUnexistingOrdertype, XFMG_InvalidCapacityCardinality {
    getXynaFactoryManagementODS().getOrderTypeManagement().modifyOrdertype(ordertypeParameter);
  }


  public void deleteOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException {
    getXynaFactoryManagementODS().getOrderTypeManagement().deleteOrdertype(ordertypeParameter);
  }


  public List<OrdertypeParameter> listOrdertypes(RuntimeContext runtimeContext) throws PersistenceLayerException {
    return getXynaFactoryManagementODS().getOrderTypeManagement().listOrdertypes(runtimeContext);
  }
  
  
  public List<OrdertypeParameter> listOrdertypes(SearchOrdertypeParameter sop) throws PersistenceLayerException {
    return getXynaFactoryManagementODS().getOrderTypeManagement().listOrdertypes(sop);
  }
  

  /**
   * QueueManagement
   */
  public void registerQueue(String uniqueName, String externalName, QueueType queueType, QueueConnectData connectData)
                  throws PersistenceLayerException {
    getXynaFactoryControl().getQueueManagement().registerQueue(uniqueName, externalName, queueType, connectData);
  }


  public void deregisterQueue(String uniqueName) throws PersistenceLayerException {
    getXynaFactoryControl().getQueueManagement().deregisterQueue(uniqueName);
  }


  public Collection<Queue> listQueues() throws PersistenceLayerException {
    return getXynaFactoryControl().getQueueManagement().listQueues();
  }


  public Collection<PrioritySetting> listPriorities() throws PersistenceLayerException {
    return getXynaFactoryManagementODS().getPriorityManagement().listPriorities();
  }


  public void removePriority(String orderType) throws PersistenceLayerException {
    getXynaFactoryManagementODS().getPriorityManagement().removePriority(orderType);
  }


  public void setPriority(String orderType, int priority) throws XFMG_InvalidXynaOrderPriority,
      PersistenceLayerException {
    getXynaFactoryManagementODS().getPriorityManagement().setPriority(orderType, priority);
  }


  public void setPriority(String orderType, int priority, Long revision) throws XFMG_InvalidXynaOrderPriority,
      PersistenceLayerException {
    getXynaFactoryManagementODS().getPriorityManagement().setPriority(orderType, priority, revision);
  }


  public void discoverPriority(XynaOrderServerExtension xo) {
    getXynaFactoryManagementODS().getPriorityManagement().discoverPriority(xo);
  }


  public Integer getPriority(String orderType) throws PersistenceLayerException {
    return getXynaFactoryManagementODS().getPriorityManagement().getPriority(orderType);
  }


  public Collection<RightScope> getRightScopes(String language) throws PersistenceLayerException {
    return ((XynaOperatorControl) getSection(XynaOperatorControl.DEFAULT_NAME)).getUserManagement().getRightScopes(language);
  }




}
