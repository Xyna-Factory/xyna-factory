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
package com.gip.xyna.xact.filter.actions.auth;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;

import xmcp.auth.LogoutRequest;


public class LogoutAction implements FilterAction {

  
  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith("/" + PathElements.AUTH + "/" + PathElements.LOGOUT) && Method.POST == method;
  }
  
  
  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    String payload = AuthUtils.insertFqnIfNeeded(tc.getPayload(), "xmcp.auth.LogoutRequest");
    XynaPlainSessionCredentials xpsc = AuthUtils.readCredentialsFromCookies(tc);
    
    try {
      AuthUtils.authenticate(xpsc);
    } catch (RemoteException e) {
      AuthUtils.replyError(tc, jfai, Status.unauthorized, e);
      return jfai;
    }
    
    XynaFactory.getInstance().getFactoryManagementPortal().quitSession(xpsc.getSessionId());
    LogoutRequest request = (LogoutRequest) Utils.convertJsonToGeneralXynaObjectUsingGuiHttp(payload);
    
    List<String> list = new ArrayList<>();
    list.add(AuthUtils.generateCookie(AuthUtils.COOKIE_FIELD_SESSION_ID, "-", request.getPath(), tc, false) + "; " + AuthUtils.COOKIE_MARKER_EXPIRED);
    list.add(AuthUtils.generateCookie(AuthUtils.COOKIE_FIELD_TOKEN, "-", request.getPath(), tc, false) + "; " + AuthUtils.COOKIE_MARKER_EXPIRED);
    jfai.setProperty("Set-Cookie", list); //Liste wird dann spaeter (in httptriggerconnection) umgewandelt in mehrere Set-Cookie Headerzeilen
    jfai.sendJson(tc, "");
    return jfai;
  }
  
  

  public void appendIndexPage(HTMLPart arg0) {
  }


  public String getTitle() {
    return "Logout";
  }


  public boolean hasIndexPageChanged() {
    return false;
  }

}
