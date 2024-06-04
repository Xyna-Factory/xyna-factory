/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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



import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.H5XdevFilter;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.session.SessionBasedData;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;



public class InfoAction implements FilterAction {

  private final XMOMGui xmomgui;

  public InfoAction(XMOMGui xmomGui) {
    this.xmomgui = xmomGui;
  }


  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith("/" + PathElements.AUTH + "/" + PathElements.INFO) && Method.GET == method;
  }


  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    Map<String, String> map = AuthUtils.readCookies(tc);
    String sessionIdKey = H5XdevFilter.STRICT_TRANSPORT_SECURITY.get() ? AuthUtils.COOKIE_FIELD_SESSION_ID_STS : AuthUtils.COOKIE_FIELD_SESSION_ID;
    String sessionId = map.get(sessionIdKey);
    SessionBasedData sbd = xmomgui.getSessionBasedData(sessionId);
   
    if(sbd == null) {
      AuthUtils.replyError(tc, jfai, Status.unauthorized, new RuntimeException());
      return jfai;
    }
    String sdj = AuthUtils.getSessionDetailsJson(sbd.getSession().getId(), sbd.getSession().getToken());

    jfai.sendJson(tc, sdj);
    return jfai;
  }


  public void appendIndexPage(HTMLPart arg0) {
  }


  public String getTitle() {
    return "Info";
  }


  public boolean hasIndexPageChanged() {
    return false;
  }


}
