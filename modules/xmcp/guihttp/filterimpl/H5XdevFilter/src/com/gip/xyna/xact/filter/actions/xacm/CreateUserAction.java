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
package com.gip.xyna.xact.filter.actions.xacm;



import java.rmi.RemoteException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.PasswordCreationUtils;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.PasswordCreationUtils.EncryptionPhase;

import xmcp.xacm.usermanagement.datatypes.CreateUserRequest;



public class CreateUserAction implements FilterAction {
  
  private static final XynaFactoryManagement factoryManagement =  (XynaFactoryManagement) XynaFactory.getInstance().getFactoryManagement();

  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith("/" + PathElements.XACM + "/" + PathElements.CREATE_USER) && Method.POST == method;
  }


  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {

    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    // Login-Check
    XynaPlainSessionCredentials xpsc = AuthUtils.readCredentialsFromCookies(tc);
    try {
      Role role = AuthUtils.authenticate(xpsc);
      if(!XynaFactory.getInstance().getFactoryManagementPortal().hasRight(Rights.USER_MANAGEMENT.name(), role)) {
        AuthUtils.replyError(tc, jfai, Status.forbidden, new XFMG_ACCESS_VIOLATION(Rights.USER_MANAGEMENT.name(), role.getName()));
        return jfai;
      }
    } catch (RemoteException e) {
      AuthUtils.replyError(tc, jfai, Status.unauthorized, e);
      return jfai;
    }
    
    CreateUserRequest createUserRequest = (CreateUserRequest) com.gip.xyna.xact.filter.util.Utils.convertJsonToGeneralXynaObject(
                     tc.getPayload(), 
                     com.gip.xyna.xact.filter.util.Utils.getRtcRevision(
                              com.gip.xyna.xact.filter.util.Utils.getGuiHttpApplication()
                     ));
    String passwordHashed = PasswordCreationUtils.generatePassword(createUserRequest.getPassword(), EncryptionPhase.LOGIN);
    boolean success = factoryManagement.createUser(createUserRequest.getUsername(), createUserRequest.getRole(), passwordHashed, true, createUserRequest.getDomains());
    jfai.sendJson(tc, "{\"success\": " + (success ? "true" : "false") + "}");
    return jfai;
  }


  @Override
  public void appendIndexPage(HTMLPart part) {

  }


  @Override
  public String getTitle() {
    return "Change Password";
  }


  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }


}
