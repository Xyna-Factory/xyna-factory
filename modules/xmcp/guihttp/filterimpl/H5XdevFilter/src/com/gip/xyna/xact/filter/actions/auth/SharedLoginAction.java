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



import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;

import xmcp.auth.SharedLoginRequest;



public class SharedLoginAction extends LoginAction {


  public SharedLoginAction(XMOMGui xmomgui) {
    super(xmomgui);
  }


  @Override
  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith("/" + PathElements.AUTH + "/" + PathElements.SHARED_LOGIN) && Method.POST == method;
  }


  @Override
  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    String payload = AuthUtils.insertFqnIfNeeded(tc.getPayload(), "xmcp.auth.SharedLoginRequest");
    SharedLoginRequest request = (SharedLoginRequest) Utils.convertJsonToGeneralXynaObjectUsingGuiHttp(payload);


    SessionCredentials creds = new SessionCredentials(request.getSessionId(), request.getToken());
    return createLoginResponse(jfai, tc, creds, request.getPath(), xmomgui);
  }


}
