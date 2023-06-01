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
package xfmg.xopctrl.impl;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import xfmg.xopctrl.ActionFailed;
import xfmg.xopctrl.DomainDoesNotExist;
import xfmg.xopctrl.NamingConventionViolated;
import xfmg.xopctrl.NameContainsInvalidCharacter;
import xfmg.xopctrl.PasswordRestrictionViolation;
import xfmg.xopctrl.PredefinedXynaObject;
import xfmg.xopctrl.RightDoesNotExist;
import xfmg.xopctrl.RoleDoesNotExist;
import xfmg.xopctrl.RoleIsAssigned;
import xfmg.xopctrl.UserAuthenticationFailed;
import xfmg.xopctrl.UserAuthenticationRight;
import xfmg.xopctrl.UserAuthenticationRightDescription;
import xfmg.xopctrl.UserAuthenticationRole;
import xfmg.xopctrl.UserAuthenticationRoleDescription;
import xfmg.xopctrl.UserChangeResult;
import xfmg.xopctrl.UserDoesNotExist;
import xfmg.xopctrl.UserIsLocked;
import xfmg.xopctrl.UserRoleManagementServiceOperation;
import xfmg.xopctrl.exceptions.UserAlreadyExists;
import base.Credentials;
import base.LockState;
import base.Password;
import base.locale.De_DEGerman;
import base.locale.En_USEnglish;
import base.locale.Locale;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_NameContainsInvalidCharacter;
import com.gip.xyna.xfmg.exceptions.XFMG_NamingConventionException;
import com.gip.xyna.xfmg.exceptions.XFMG_PasswordRestrictionViolation;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RightDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainName;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserName;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;


public class UserRoleManagementServiceOperationImpl implements ExtendedDeploymentTask, UserRoleManagementServiceOperation {

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }
  
  
  public void createRight(UserAuthenticationRight right, base.Text description_EN, base.Text description_DE) throws NamingConventionViolated, NameContainsInvalidCharacter, ActionFailed {
    try {
        boolean success = XynaFactory.getInstance().getXynaMultiChannelPortal().createRight(right.getRight());
        if (!success) {
            throw new ActionFailed("Could not create Right '" + right.getRight() + "'. Possibly this Right already exists.");
        }
        
        // set English description if available
        if ((description_EN != null) && (description_EN.getText() != null) && (description_EN.getText().length() > 0)) {
            try {
                XynaFactory.getInstance().getXynaMultiChannelPortal().setDescriptionOfRight(right.getRight(), description_EN.getText(), "EN");
            } catch (XynaException e) {
                throw new RuntimeException(e);
            }
        }
        // set German description if available
        if ((description_DE != null) && (description_DE.getText() != null) && (description_DE.getText().length() > 0)) {
            try {
                XynaFactory.getInstance().getXynaMultiChannelPortal().setDescriptionOfRight(right.getRight(), description_DE.getText(), "DE");
            } catch (XynaException e) {
                throw new RuntimeException(e);
            }
        }
    } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
    } catch (XFMG_NamingConventionException e) {
        throw new NamingConventionViolated(e);
    } catch (XFMG_NameContainsInvalidCharacter e) {
        throw new NameContainsInvalidCharacter(e.getNameType(), e);
    }
  }
  
  public void deleteRight(UserAuthenticationRight right) throws RightDoesNotExist, ActionFailed {
    try {
      Right fr = XynaFactory.getInstance().getXynaMultiChannelPortal().getRight(right.getRight(), "EN");
      if(fr == null) {
        throw new RightDoesNotExist(right.getRight());
      }
      List<String> rolesWithThatRight = getRolesWithGrantedRight(right.getRight());
      if(!rolesWithThatRight.isEmpty()) {
        StringBuilder message = new StringBuilder("The Right is used by the ");
        if(rolesWithThatRight.size() > 1) {
          message.append("Roles ");
        } else {
          message.append("Role ");
        }
        for (int i = 0; i < rolesWithThatRight.size(); i++) {
          message.append(rolesWithThatRight.get(i));
          if(i < rolesWithThatRight.size() - 1) {
            message.append(", ");
          }
        }
        message.append(".");
        throw new ActionFailed(message.toString());
      }
      boolean success = XynaFactory.getInstance().getXynaMultiChannelPortal().deleteRight(right.getRight());
      if (!success) {
          throw new ActionFailed("Could not delete Right '" + right.getRight() + "'. Possibly this Right does not exist.");
      }
    } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
    } catch (XFMG_PredefinedXynaObjectException e) {
        throw new RuntimeException(e);
    } catch (XFMG_RightDoesNotExistException e) {
        throw new RightDoesNotExist(e.getRightId(), e);
    }
  }
  
  private List<String> getRolesWithGrantedRight(String right) throws PersistenceLayerException{
    List<String> result = new ArrayList<>();
    if(right == null) {
      return result;
    }
    boolean isScopedRight = right.contains(":");
    Collection<Role> roles = XynaFactory.getInstance().getXynaMultiChannelPortal().getRoles();
    for (Role role : roles) {
      if(isScopedRight) {
        String rightName = right.split(":")[0];
        for (String grantedRight : role.getScopedRights()) {
          String grantedRightName = grantedRight.split(":")[0];
          if(rightName.equals(grantedRightName)) {
            result.add(role.getName());
          }
        }
      } else {
        if(role.getRightsAsList().contains(right)) {
          result.add(role.getName());
        }
      }
    }
    return result;
  }
  
  public void createRole(UserAuthenticationRole userAuthenticationRole, DomainName domainName, base.Text description) throws DomainDoesNotExist, NameContainsInvalidCharacter, ActionFailed {
    try {
        boolean success = XynaFactory.getInstance().getXynaMultiChannelPortal().createRole(userAuthenticationRole.getRole(), domainName.getName());
        if (!success) {
            throw new ActionFailed("Could not create Role '" + userAuthenticationRole.getRole() + "'. Possibly this Role already exists.");
        }
        if (!description.isNullOrEmpty()) {
            try {
              XynaFactory.getInstance().getXynaMultiChannelPortal().setDescriptionOfRole(userAuthenticationRole.getRole(), domainName.getName(), description.getText());
            } catch (XFMG_RoleDoesNotExistException e) {
                throw new RuntimeException(e);
            } catch (XFMG_PredefinedXynaObjectException e) {
                throw new RuntimeException(e);
            }
        }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XFMG_DomainDoesNotExistException e) {
      throw new DomainDoesNotExist(e.getDomainName(), e);
    } catch (XFMG_NameContainsInvalidCharacter e) {
      throw new NameContainsInvalidCharacter(e.getNameType(), e);
    }
  }

  public void deleteRole(UserAuthenticationRole userAuthenticationRole, DomainName domainName) throws PredefinedXynaObject, RoleIsAssigned, ActionFailed {
    try {
      Role role = XynaFactory.getInstance().getXynaMultiChannelPortal().getRole(userAuthenticationRole.getRole(), domainName.getName());
      if(role == null) {
        throw new ActionFailed("Role doesn't exist.");
      }
      boolean success = XynaFactory.getInstance().getXynaMultiChannelPortal().deleteRole(userAuthenticationRole.getRole(), domainName.getName());
      if (!success) {
          throw new ActionFailed("Could not delete Role '" + userAuthenticationRole.getRole() + "'. Possibly this Role does not exist.");
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XFMG_PredefinedXynaObjectException e) {
      throw new PredefinedXynaObject(e.getId(), e.getType(), e);
    } catch (XFMG_RoleIsAssignedException e) {
      throw new RoleIsAssigned(e.getId(), e);
    }
  }

  public void grantRightToRole(UserAuthenticationRole userAuthenticationRole, UserAuthenticationRight userAuthenticationRight) throws RightDoesNotExist, RoleDoesNotExist, ActionFailed {
    try {
      boolean success = XynaFactory.getInstance().getXynaMultiChannelPortal().grantRightToRole(userAuthenticationRole.getRole(), userAuthenticationRight.getRight());
      if (!success) {
          throw new ActionFailed("Could not grant Right '" + userAuthenticationRight.getRight() + "' to role '" + userAuthenticationRole.getRole() + "'.");
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XFMG_RightDoesNotExistException e) {
      throw new RightDoesNotExist(e.getRightId(), e);
    } catch (XFMG_RoleDoesNotExistException e) {
      throw new RoleDoesNotExist(e.getRoleId(), e);
    }
  }

  public void revokeRightFromRole(UserAuthenticationRole userAuthenticationRole, UserAuthenticationRight userAuthenticationRight) throws RoleDoesNotExist, RightDoesNotExist, ActionFailed {
    try {
      boolean success = XynaFactory.getInstance().getXynaMultiChannelPortal().revokeRightFromRole(userAuthenticationRole.getRole(), userAuthenticationRight.getRight());
      if (!success) {
          throw new ActionFailed("Could not revoke Right '" + userAuthenticationRight.getRight() + "' from role '" + userAuthenticationRole.getRole() + "'.");
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XFMG_RoleDoesNotExistException e) {
      throw new RoleDoesNotExist(e.getRoleId(), e);
    } catch (XFMG_RightDoesNotExistException e) {
      throw new RightDoesNotExist(e.getRightId(), e);
    }
  }

  public UserChangeResult createUser(Credentials credentials, UserAuthenticationRole userAuthenticationRole) throws RoleDoesNotExist, PasswordRestrictionViolation, NameContainsInvalidCharacter, UserAlreadyExists {
    boolean success;
    try {
      success = XynaFactory.getInstance().getXynaMultiChannelPortal().createUser(credentials.getUsername(), userAuthenticationRole.getRole(), credentials.getPassword(), false);
      if (!success) {
        //throw new ActionFailed("Could not create User '" + credentials.getUsername() + "'. Possibly this User already exists.");
        throw new UserAlreadyExists();
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XFMG_RoleDoesNotExistException e) {
      throw new RoleDoesNotExist(e.getRoleId(), e);
    } catch (XFMG_PasswordRestrictionViolation e) {
      throw new PasswordRestrictionViolation(e.getRestriction(), e);
    } catch (XFMG_NameContainsInvalidCharacter e) {
      throw new NameContainsInvalidCharacter(e.getNameType(), e);
    }
    
    return new UserChangeResult(success);
  }

  public UserChangeResult deleteUser(UserName userName) throws PredefinedXynaObject {
    boolean success;
    try {
      success = XynaFactory.getInstance().getXynaMultiChannelPortal().deleteUser(userName.getName());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XFMG_PredefinedXynaObjectException e) {
      throw new PredefinedXynaObject(e.getId(), "user", e);
    }
    
    return new UserChangeResult(success);
  }
  
  public LockState getLockState(UserName userName) throws UserDoesNotExist{
    User user;
    try {
      user = XynaFactory.getInstance().getXynaMultiChannelPortal().getUser(userName.getName());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    
    if (user == null) {
      throw new UserDoesNotExist(userName.getName());
    }
    
    return new LockState(user.isLocked());
  }

  public UserChangeResult setLockState(UserName userName, LockState lockState) throws UserDoesNotExist, PredefinedXynaObject {
    boolean success;
    try {
      success = XynaFactory.getInstance().getXynaMultiChannelPortal().setLockedStateOfUser(userName.getName(), lockState.getLocked());
    } catch (XFMG_UserDoesNotExistException ex) {
      throw new UserDoesNotExist(ex.getUserId(), ex);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XFMG_PredefinedXynaObjectException e) {
      throw new PredefinedXynaObject(e.getId(), "user", e);
    }
    return new UserChangeResult(success);
  }

  public UserChangeResult changePassword(UserName userName, Password oldPassword, Password newPassword) throws UserDoesNotExist, PasswordRestrictionViolation, UserIsLocked, UserAuthenticationFailed {
    String oldPasswordHash = generateHashedPassword(oldPassword.getPassword());
    boolean success;
    try {
      success = XynaFactory.getInstance().getXynaMultiChannelPortal().changePassword(userName.getName(), oldPasswordHash, newPassword.getPassword(), false);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XFMG_UserAuthenticationFailedException e) {
      throw new UserAuthenticationFailed(e.getUserId(), e);
    } catch (XFMG_UserIsLockedException e) {
      throw new UserIsLocked(e.getUserId(), e);
    } catch (XFMG_UserDoesNotExistException e) {
      throw new UserDoesNotExist(e.getUserId(), e);
    } catch (XFMG_PasswordRestrictionViolation e) {
      throw new PasswordRestrictionViolation(e.getRestriction(), e);
    }
    
    return new UserChangeResult(success);
  }

  public UserChangeResult setPassword(UserName userName, Password newPassword) throws UserDoesNotExist, PasswordRestrictionViolation {
    boolean success;
    try {
      success = XynaFactory.getInstance().getXynaMultiChannelPortal().setPassword(userName.getName(), newPassword.getPassword());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XFMG_UserDoesNotExistException e) {
      throw new UserDoesNotExist(e.getUserId(), e);
    } catch (XFMG_PasswordRestrictionViolation e) {
      throw new PasswordRestrictionViolation(e.getRestriction(), e);
    }
    
    return new UserChangeResult(success);
  }
  
  public UserName getCurrentUser(XynaOrderServerExtension xo) {
    SessionManagement sMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
    String name = sMgmt.resolveSessionToUser(xo.getSessionId());
    
    return new UserName(name);
  }

  public UserAuthenticationRole getCurrentRole(XynaOrderServerExtension xo) {
    if (xo != null && xo.getCreationRole() != null) {
      return new UserAuthenticationRole(xo.getCreationRole().getName());
    } else {
      return new UserAuthenticationRole(null);
    }
  }

  @Override
  public List<? extends UserAuthenticationRight> getAllRightsFromRole(UserAuthenticationRole role, DomainName domain) throws RoleDoesNotExist {
    List<UserAuthenticationRight> rights = new ArrayList<>();
    try {
      Role r = XynaFactory.getInstance().getXynaMultiChannelPortal().getRole(role.getRole(), domain.getName());
      if (r == null) {
          throw new RoleDoesNotExist(role.getRole());
      }
      // all rights without parameters
      for(String s: r.getRightsAsSet()) {
          rights.add(new UserAuthenticationRight(s));
      }
      // all rights with parameters
      for(String s: r.getScopedRights()) {
          rights.add(new UserAuthenticationRight(s));
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    return rights;
  }

  @Override
  public List<? extends UserAuthenticationRole> getAllRoles() {
    List<UserAuthenticationRole> result = new ArrayList<>();
    Collection<Role> roles;
    try {
      roles = XynaFactory.getInstance().getXynaMultiChannelPortal().getRoles();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    for (Role r: roles) {
      result.add(new UserAuthenticationRole(r.getName()));
    }
    return result;
  }
  
  @Override
  public List<? extends UserAuthenticationRight> getAllRights(Locale locale) {
    Collection<Right> all_rights;
    Collection<RightScope> all_right_scopes;
    try {
        String lang = get_language_string_from_Locale(locale);
        all_rights = XynaFactory.getInstance().getXynaMultiChannelPortal().getRights(lang);
        all_right_scopes = XynaFactory.getInstance().getXynaMultiChannelPortal().getRightScopes(lang);
    } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
    }
    List<UserAuthenticationRight> result = new ArrayList<>();
    for (Right right : all_rights) {
        result.add(new UserAuthenticationRight(right.getName()));
    }
    for (RightScope right_scope : all_right_scopes) {
        result.add(new UserAuthenticationRight(right_scope.getDefinition()));
    }
    return result;
  }
  
  public UserAuthenticationRole getRoleFromUser(UserName user_name) throws UserDoesNotExist {
    try {
        Collection<User> all_users = XynaFactory.getInstance().getXynaMultiChannelPortal().getUser();
        for (User user : all_users) {
            if (user.getName().equals(user_name.getName())) {
                return new UserAuthenticationRole(user.getRole());
            }
        }
    } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
    }
    throw new UserDoesNotExist(user_name.getName());
  }

  @Override
  public UserAuthenticationRoleDescription getDescriptionOfRole(UserAuthenticationRole role, DomainName domain) throws RoleDoesNotExist {
    try {
      Role r = XynaFactory.getInstance().getXynaMultiChannelPortal().getRole(role.getRole(), domain.getName());
      if (r == null) {
          throw new RoleDoesNotExist(role.getRole());
      }
      return new UserAuthenticationRoleDescription(r.getDescription());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setDescriptionOfRole(UserAuthenticationRole role, DomainName domain, UserAuthenticationRoleDescription description) throws RoleDoesNotExist {
    try {
      XynaFactory.getInstance().getXynaMultiChannelPortal().setDescriptionOfRole(role.getRole(), domain.getName(), description.getDescription());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public UserAuthenticationRightDescription getDescriptionOfRight(UserAuthenticationRight right, Locale locale) throws RightDoesNotExist {
    try {
      String lang = get_language_string_from_Locale(locale);
      Right r = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getRight(right.getRight(), lang);
      if (r == null) {
          throw new RightDoesNotExist(right.getRight());
      }
      return new UserAuthenticationRightDescription(r.getDescription());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }
  
  //================================================================================================
  // private helper methods
  //================================================================================================
  
  private static String generateHashedPassword(String password) {
      StringBuilder passwordBuilder = new StringBuilder();
      try {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update((password).getBytes());
        byte[] result = md5.digest();    

        for (int i = 0; i < result.length; i++) {
          int halfbyte = (result[i] >>> 4) & 0x0F;
          int two_halfs = 0;
          do {
            if ((0 <= halfbyte) && (halfbyte <= 9))
              passwordBuilder.append((char) ('0' + halfbyte));
            else
              passwordBuilder.append((char) ('a' + (halfbyte - 10)));
            halfbyte = result[i] & 0x0F;
          } while (two_halfs++ < 1);
        }
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("Error during hash generation", e);
      }
      return passwordBuilder.toString();
  }
  
  private static String get_language_string_from_Locale(Locale locale) {
    String lang;
    if (locale instanceof En_USEnglish) {
      lang = "EN";
    } else if (locale instanceof De_DEGerman) {
      lang = "DE";
    } else {
      lang = locale.getLanguage();
    }
    return lang;
  }
}
