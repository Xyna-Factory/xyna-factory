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
package xfmg.xopctrl.impl;



import java.util.ArrayList;
import java.util.List;

import xfmg.xopctrl.CouldNotAccessSessionException;
import xfmg.xopctrl.InvalidCredentials;
import xfmg.xopctrl.InvalidSession;
import xfmg.xopctrl.SessionManagementServiceOperation;
import xfmg.xopctrl.UserAuthenticationRight;
import xfmg.xopctrl.UserAuthenticationRole;
import xfmg.xopctrl.XynaUserSession;
import base.Credentials;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
//import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateSessionException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionDetails;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;



public class SessionManagementServiceOperationImpl implements ExtendedDeploymentTask, SessionManagementServiceOperation {

  private static SessionManagement sMgmt;
  private static UserManagement uMgmt;


  public void onDeployment() throws XynaException {
  }


  public void onUndeployment() throws XynaException {
  }


  public Long getOnUnDeploymentTimeout() {
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    return null;
  }


  private static void init() {
    if (sMgmt == null) {
      sMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
    }
    if (uMgmt == null) {
      uMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement();
    }
  }


  public XynaUserSession login(Credentials credentials) throws InvalidCredentials {
    init();
    User user;
    try {
      user = uMgmt.authenticate(credentials.getUsername(), credentials.getPassword());
    } catch (XFMG_UserAuthenticationFailedException e) {
      throw new InvalidCredentials(e); // FIXME
    } catch (XFMG_UserIsLockedException e) {
      throw new InvalidCredentials(e); // FIXME
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    try {
      SessionCredentials sCreds = sMgmt.getNewSession(user, true);
      String sessionString = sCreds.getSessionId();
      XynaUserSession userSessionID = new XynaUserSession(sessionString);
      return userSessionID;
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  
  public void logout(XynaUserSession xynaUserSession) {
    init();
    try {
      if (sMgmt.isSessionAlive(xynaUserSession.getSessionID())) {
        sMgmt.quitSession(xynaUserSession.getSessionID());
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void validateSession(XynaUserSession xynaUserSession) throws InvalidSession {
    init();
    try {
      if (!sMgmt.isSessionAlive(xynaUserSession.getSessionID())) {
        throw new InvalidSession();
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public List<? extends UserAuthenticationRight> getUserAuthenticationRights(XynaUserSession xynaUserSession) 
                  throws CouldNotAccessSessionException {
    init();
    List<UserAuthenticationRight> ret = new ArrayList<UserAuthenticationRight>();
    try {
      SessionDetails details = sMgmt.getSessionDetails(xynaUserSession.getSessionID());
      for (String s : details.getRights()) {
        UserAuthenticationRight right = new UserAuthenticationRight();
        right.setRight(s);
        ret.add(right);
      }
    } catch (Exception e) {
      throw new CouldNotAccessSessionException(e);
    }
    return ret;
  }


  public UserAuthenticationRole getUserAuthenticationRole(XynaUserSession xynaUserSession) 
                  throws CouldNotAccessSessionException {
    init();
    try {
      SessionDetails details = sMgmt.getSessionDetails(xynaUserSession.getSessionID());
      String role = details.getRole();
      UserAuthenticationRole ret = new UserAuthenticationRole();
      ret.setRole(role);
      return ret;
    } catch (Exception e) {
      throw new CouldNotAccessSessionException(e);
    }
  }


  public XynaUserSession getCurrentXynaUserSession(XynaOrderServerExtension xo) 
                  throws CouldNotAccessSessionException {
    init();
    try {
      SessionDetails details = sMgmt.getSessionDetails(xo.getSessionId());
      XynaUserSession ret = new XynaUserSession();
      ret.setSessionID(details.getSessionId());
      return ret;
    } catch (Exception e) {
      throw new CouldNotAccessSessionException(e);
    }
  }
  
}
