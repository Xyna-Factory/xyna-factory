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
package com.gip.xyna.xfmg.xopctrl.usermanagement;


import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateSessionException;
import com.gip.xyna.xfmg.exceptions.XFMG_NameContainsInvalidCharacter;
import com.gip.xyna.xfmg.exceptions.XFMG_PasswordRestrictionViolation;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_SESSION_AUTHENTICATION_FAILED;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownSessionIDException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class TemporarySessionAuthentication {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(TemporarySessionAuthentication.class);
  
  public final static String TEMPORARY_CLI_USER_NAME = "CLI Access";
  public final static String TEMPORARY_CLI_USER_ROLE = UserManagement.ADMIN_ROLE_NAME;
  private final String username;
  private final String password;
  private final String rolename;
  private final Long revision;
  private final boolean noFactoryUser;
  private final CommandControl.Operation operationLock;
  private SessionCredentials session;
  
  private TemporarySessionAuthentication(String username, String password, String rolename, Long revision, boolean noFactoryUser, CommandControl.Operation operationLock) {
    this.password = password;
    this.rolename = rolename;
    this.noFactoryUser = noFactoryUser;
    this.username = username;
    this.operationLock = operationLock;
    this.revision = revision;
  }
  
  
  public void initiate() throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation, XFMG_NameContainsInvalidCharacter, XFMG_PredefinedXynaObjectException, XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException, XFMG_DuplicateSessionException {
    XynaFactoryBase factory = XynaFactory.getInstance();
    if (operationLock != null) {
      CommandControl.tryLock(operationLock, revision);
    }
    boolean rollbackCommandControl = true;
    try {
      if (noFactoryUser) {
        session = factory.getFactoryManagement().getXynaOperatorControl().getSessionManagement().createSession(new XynaUserCredentials(username, null), Optional.of(rolename), false, true);
        try {
          factory.getFactoryManagement().getXynaOperatorControl().getSessionManagement().authorizeSession(session.getSessionId(), session.getToken(), rolename);
        } catch (XFMG_UnknownSessionIDException e) {
          throw new RuntimeException("Session vanished immediately after creation!",e);
        } catch (XFMG_SESSION_AUTHENTICATION_FAILED e) {
          throw new IllegalStateException("Session failed to authenticate immediately after creation!", e);
        }
      } else {
        User user = factory.getFactoryManagementPortal().authenticate(getUsername(), password);
        if (user == null) {
          throw new XFMG_UserAuthenticationFailedException(getUsername());
        }
      }
      rollbackCommandControl = false;
    } finally {
      if (operationLock != null && rollbackCommandControl) {
        CommandControl.unlock(operationLock, revision);
      }
    }
  }
  
  public void destroy() throws PersistenceLayerException, XFMG_PredefinedXynaObjectException {
    XynaFactoryBase factory = XynaFactory.getInstance();
    try {
      if (session != null) {
        factory.getXynaMultiChannelPortalSecurityLayer().quitSession(session.getSessionId());
      }
    } finally {
      if (operationLock != null) {
        CommandControl.unlock(operationLock, revision);
      }
    }
  }
  
  
  public String getUsername() {
    return username;
  }
  
  
  public String getSessionId() {
    if (session == null) {
      throw new IllegalStateException("TempAuth is either not started or was not succesfull!");
    } else {
      return session.getSessionId();
    }
  }
  
  
  public static TemporarySessionAuthentication tempAuthWithTempUser(String username, String password, Long revision) {
    return tempAuthWithTempUser(username, password, revision, true);
  }
  
  public static TemporarySessionAuthentication tempAuthWithTempUser(String username, String password, Long revision, boolean appendRevisionSuffixOnUsername) {
    return tempAuthWithTempUser(username, password, UserManagement.ADMIN_ROLE_NAME, revision, appendRevisionSuffixOnUsername);
  }
  
  public static TemporarySessionAuthentication tempAuthWithTempUser(String username, String password, String rolename, Long revision, boolean appendRevisionSuffixOnUsername) {
    return tempAuthWithTempUserAndOperationLock(username, password, rolename, revision, appendRevisionSuffixOnUsername, null);
  }
  
  public static TemporarySessionAuthentication tempAuthWithTempUserAndOperationLock(String username, String password, String rolename, Long revision, boolean appendRevisionSuffixOnUsername, CommandControl.Operation operationLock) {
    if (appendRevisionSuffixOnUsername) {
      username = username + revision;
    }    
    return new TemporarySessionAuthentication(username, password, rolename, revision, true, operationLock);
  }


  public static TemporarySessionAuthentication tempAuthWithUniqueUser(String userNamePrefix, String rolename) {
    return tempAuthWithUniqueUserAndOperationLock(userNamePrefix, rolename, null, null);
  }

  public static TemporarySessionAuthentication tempAuthWithUniqueUserAndOperationLock(String userNamePrefix, String rolename, Long revision,
                                                                                      CommandControl.Operation operationLock) {
    return new TemporarySessionAuthentication(userNamePrefix, null, rolename, revision, true, operationLock);
  }


  public static TemporarySessionAuthentication tempAuthWithExistingUser(String username, String password, Long revision) {
    return tempAuthWithExistingUserAndOperationLock(username, password, revision, null);
  }
  
  public static TemporarySessionAuthentication tempAuthWithExistingUserAndOperationLock(String username, String password, Long revision, CommandControl.Operation operationLock) {
    return new TemporarySessionAuthentication(username, password, null, revision, true, operationLock);
  }

}
