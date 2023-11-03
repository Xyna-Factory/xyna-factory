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
package com.gip.xyna.xact.filter.actions.auth;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.H5XdevFilter;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateSessionException;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.PasswordCreationUtils;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.PasswordCreationUtils.EncryptionPhase;
import com.gip.xyna.xmcp.RMIChannelImpl;

import xmcp.auth.LoginRequest;


public class LoginAction implements FilterAction {


  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith("/" + PathElements.AUTH + "/" + PathElements.LOGIN) && Method.POST == method;
  }
  
  
  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
 
    
    String payload = AuthUtils.insertFqnIfNeeded(tc.getPayload(), "xmcp.auth.LoginRequest");
    
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    LoginRequest loginRequest = (LoginRequest) Utils.convertJsonToGeneralXynaObjectUsingGuiHttp(payload);
    
    // TODO see Jira PMOD-161 - force
    
    String passwordHashed = PasswordCreationUtils.generatePassword(loginRequest.getPassword(), EncryptionPhase.LOGIN);
    String path = loginRequest.getPath();
    boolean force = loginRequest.getForce() != null ? loginRequest.getForce() : true;
    SessionCredentials creds;
    try {
      creds = RMIChannelImpl.staticCreateSession(loginRequest.getUsername(), passwordHashed, force);
    } catch (RemoteException e) {
      if (e.getMessage().contains("Duplicate Session")) {
        AuthUtils.replyError(tc, jfai, Status.unauthorized, new XFMG_DuplicateSessionException(loginRequest.getUsername()));
        return jfai;
      } else {
        AuthUtils.replyError(tc, jfai, Status.unauthorized, e);
        return jfai;
      }
    }
    

    return createLoginResponse(jfai, tc, creds, path);
  }


  public static FilterActionInstance createLoginResponse(JsonFilterActionInstance jfai, HTTPTriggerConnection tc, SessionCredentials creds, String path)
      throws XynaException {

    String sdj = AuthUtils.getSessionDetailsJson(creds.getSessionId(), creds.getToken());
    String sessionId = H5XdevFilter.STRICT_TRANSPORT_SECURITY.get() ? AuthUtils.COOKIE_FIELD_SESSION_ID_STS : AuthUtils.COOKIE_FIELD_SESSION_ID;
    
    List<String> list = new ArrayList<>();
    list.add(AuthUtils.generateCookie(sessionId, creds.getSessionId(), path, tc, true));
    if(!AuthUtils.USE_CSRF_TOKEN.get()) {
      list.add(AuthUtils.generateCookie(AuthUtils.COOKIE_FIELD_TOKEN, creds.getToken(), path, tc, true));
    }
    jfai.setProperty("Set-Cookie", list); //Liste wird dann spaeter (in httptriggerconnection) umgewandelt in mehrere Set-Cookie Headerzeilen
    jfai.sendJson(tc, sdj);
    return jfai;
  }



  public void appendIndexPage(HTMLPart arg0) {
  }


  public String getTitle() {
    return "Login";
  }


  public boolean hasIndexPageChanged() {
    return false;
  }


  
  

}
