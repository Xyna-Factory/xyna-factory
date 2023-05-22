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
package com.gip.xyna.xact.filter.actions.auth;



import java.rmi.RemoteException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.json.ChangePasswordRequestJson;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.PasswordCreationUtils;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.PasswordCreationUtils.EncryptionPhase;



public class ChangePasswordAction implements FilterAction {


  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith("/" + PathElements.AUTH + "/" + PathElements.CHANGE_PASSWORD) && Method.POST == method;
  }


  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {

    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    // Login-Check
    XynaPlainSessionCredentials xpsc = AuthUtils.readCredentialsFromCookies(tc);
    try {
      Role role = AuthUtils.authenticate(xpsc);
      if(!XynaFactory.getInstance().getFactoryManagementPortal().hasRight(Rights.USER_MANAGEMENT_EDIT_OWN.name(), role)) {
        AuthUtils.replyError(tc, jfai, Status.forbidden, new XFMG_ACCESS_VIOLATION(Rights.USER_MANAGEMENT_EDIT_OWN.name(), role.getName()));
        return jfai;
      }
    } catch (RemoteException e) {
      AuthUtils.replyError(tc, jfai, Status.unauthorized, e);
      return jfai;
    }

    JsonParser jp = new JsonParser();
    ChangePasswordRequestJson cprj;
    try {
      cprj = jp.parse(tc.getPayload(), ChangePasswordRequestJson.getJsonVisitor());
    } catch (InvalidJSONException | UnexpectedJSONContentException e) {
      AuthUtils.replyError(tc, jfai, e);
      return jfai;
    }

    String user = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement()
        .resolveSessionToUser(xpsc.getSessionId());
    String oldPasswordHashed = PasswordCreationUtils.generatePassword(cprj.getOldPassword(), EncryptionPhase.LOGIN);
    String newPasswordHashed = PasswordCreationUtils.generatePassword(cprj.getNewPassword(), EncryptionPhase.LOGIN);
    
    try {
      boolean result =
          XynaFactory.getInstance().getFactoryManagement().changePassword(user, oldPasswordHashed, newPasswordHashed, true);
      jfai.sendJson(tc, "{\"success\": " + (result ? "true" : "false") + "}");
    } catch (XFMG_UserAuthenticationFailedException ex) {
      AuthUtils.replyError(tc, jfai, Status.forbidden, ex);
      return jfai;
    }
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
