/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.FilterAction;
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
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownSessionIDException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;



public class InfoAction implements FilterAction {

  private static Logger logger = CentralFactoryLogging.getLogger(InfoAction.class);

  private final XMOMGui xmomgui;


  public InfoAction(XMOMGui xmomgui) {
    this.xmomgui = xmomgui;
  }


  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith("/" + PathElements.AUTH + "/" + PathElements.INFO) && Method.GET == method;
  }


  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    XynaPlainSessionCredentials creds = AuthUtils.readCredentialsFromRequest(tc);
    String sessionId = creds.getSessionId();

    if (sessionId == null) {
      AuthUtils.replyError(tc, jfai, Status.unauthorized, new RuntimeException());
      return jfai;
    }


    SessionBasedData data = xmomgui.getSessionBasedData(sessionId);
    if (data != null) {
      logger.debug("returning token for local session");
      String sdj = AuthUtils.getSessionDetailsJson(sessionId, data.getSession().getToken());
      jfai.sendJson(tc, sdj);
      return jfai;
    }

    logger.debug("session not found locally, checking session database");

    try {
      String sdj = AuthUtils.getSessionDetailsJson(sessionId);
      jfai.sendJson(tc, sdj);
      return jfai;
    } catch (XFMG_UnknownSessionIDException e) {
      AuthUtils.replyError(tc, jfai, Status.unauthorized, new RuntimeException());
      return jfai;
    }
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
