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
package com.gip.xyna.xmcp;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyAccess;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyRight;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionBasedUserContextValue;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionDetails;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.PasswordExpiration;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaUserCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSearchResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSelect;


public interface XynaOperatorControlRMI extends Remote {
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean hasRight(String user, String password, String rightName) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean createUser(String user, String password, String id, String roleName, String newPassword, boolean isPassHashed) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean deleteUser(String user, String password, String id) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean changePassword(String user, String password, String id, String oldPassword, String newPassword, boolean isNewPasswordHashed) throws RemoteException;
    
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean setPassword(String user, String password, String id, String newPassword) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean changeRole(String user, String password, String id, String name) throws RemoteException;  
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean createRole(String user, String password, String name, String domain) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean deleteRole(String user, String password, String name, String domain) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean grantRightToRole(String user, String password, String roleName, String right) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean revokeRightFromRole(String user, String password, String roleName, String right) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean createRight(String user, String password, String rightName) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean deleteRight(String user, String password, String rightName) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Collection<Right> getRights(String user, String password) throws RemoteException;
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Collection<Right> getRights(String user, String password, String language) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Collection<RightScope> getRightScopes(XynaCredentials credentials) throws RemoteException;
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Collection<RightScope> getRightScopes(XynaCredentials credentials, String language) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Collection<Role> getRoles(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Collection<User> getUser(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Role getMyRole(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public SessionCredentials createSession(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public SessionCredentials createSession(String user, String password, boolean force) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public SessionCredentials createUnauthorizedSession(String user, String password, String sessionUser, boolean force) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean authorizeSession(XynaUserCredentials user, String domainOverride, XynaPlainSessionCredentials session) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public void destroySession(String sessionId, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public void pingSession(String sessionId, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public SessionDetails getSessionDetails(String sessionId, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public User getUser(String user, String password, String useridentifier) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Role getRole(String user, String password, String rolename, String domainname) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Right getRight(String user, String password, String rightidentifier) throws RemoteException;
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Right getRight(String user, String password, String rightidentifier, String language) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Domain getDomain(String user, String password, String domainidentifier) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean setLockedStateOfUser(String user, String password, String useridentifier, boolean newState) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean setDomainsOfUser(String user, String password, String useridentifier, List<String> domains) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean setDescriptionOfRole(String user, String password, String rolename, String domainname, String newDescription) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean setAliasOfRole(String user, String password, String rolename, String domainname, String newAlias) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public UserSearchResult searchUsers(String user, String password, UserSelect selection, int maxRows) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public Collection<Domain> getDomains(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean createDomain(String user, String password, String domainidentifier, DomainType type, int maxRetries, int connectionTimeout) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean setDomainSpecificDataOfDomain(String user, String password, String domainname, DomainTypeSpecificData specificData) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean setDescriptionOfRight(String user, String password, String rightidentifier, String description) throws RemoteException;
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean setDescriptionOfRight(String user, String password, String rightidentifier, String description, String language) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean setMaxRetriesOfDomain(String user, String password, String domainidentifier, int maxRetries) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean setConnectionTimeoutOfDomain(String user, String password, String domainidentifier, int connectionTimeout) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean setDescriptionOfDomain(String user, String password, String domainidentifier, String description) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean deleteDomain(String user, String password, String domainidentifier) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public boolean isSessionAlive(String user, String password, String sessionId) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public void terminateSession(XynaCredentials credentials) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public PasswordExpiration getPasswordExpiration(XynaCredentials credentials, String userName) throws RemoteException;


  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public List<SessionBasedUserContextValue> getUserContextValues(XynaCredentials credentials) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public void setUserContextValue(XynaCredentials credentials, String key, String value) throws RemoteException;

  @ProxyAccess(right = ProxyRight.USER_MANAGEMENT)
  public void resetUserContextValues(XynaCredentials credentials) throws RemoteException;

}
