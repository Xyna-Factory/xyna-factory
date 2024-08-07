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

package com.gip.xyna.xact.filter.actions;

import java.rmi.RemoteException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

public abstract class H5xFilterAction  implements FilterAction{

  protected boolean isLoggedIn(HTTPTriggerConnection tc) {
    return isLoggedIn(AuthUtils.readCredentialsFromRequest(tc));
  }
  
  protected boolean isLoggedIn(XynaPlainSessionCredentials creds) {
    try {
      return AuthUtils.authenticate(creds) != null;
    } catch (RemoteException e) {
      return false;
    }
  }
  
  protected boolean hasModellerRight(HTTPTriggerConnection tc) {
    return hasRight(tc, GuiRight.PROCESS_MODELLER.getKey());
  }
  
  protected boolean hasRight(HTTPTriggerConnection tc, String right) {
    return hasRight(AuthUtils.readCredentialsFromRequest(tc), right);
  }
  
  protected boolean hasRight(XynaPlainSessionCredentials creds, String right) {
    try {
      return XynaFactory.getInstance().getFactoryManagementPortal().hasRight(right, getRole(creds));
    } catch (PersistenceLayerException ex) {
      return false;
    }
  }


  protected Role getRole(HTTPTriggerConnection tc) {
    return getRole(AuthUtils.readCredentialsFromRequest(tc));
  }
  
  protected Role getRole(XynaPlainSessionCredentials creds) {
    try {
      return AuthUtils.authenticate(creds);
    } catch (RemoteException ex) {
      return null;
    }
  }
  
  
  //returns true, if user is logged in and has all passed rights
  protected boolean checkLoginAndRights(HTTPTriggerConnection tc, JsonFilterActionInstance jfai, String... rights)  throws XynaException {
   
    if(!isLoggedIn(tc)) {
      AuthUtils.replyLoginRequiredError(tc, jfai);
      return false;
    }
    
    if(rights == null) {
      return true;
    }
    
    for(String right : rights) {
      if (!hasRight(tc, right)) {
        Role role = getRole(tc);
        AuthUtils.replyError(tc, jfai, Status.forbidden, new XFMG_ACCESS_VIOLATION(right, role != null ? role.getName() : ""));
        return false;
      }
    }
    
    return true;
  }
  
  protected boolean checkLoginAndRights(XynaPlainSessionCredentials creds, String... rights) {
    if(!isLoggedIn(creds)) {
      return false;
    }
    
    if(rights == null) {
      return true;
    }
    
    for(String right : rights) {
      if (!hasRight(creds, right)) {
        getRole(creds);
        return false;
      }
    }
    
    return true;
  }

}

