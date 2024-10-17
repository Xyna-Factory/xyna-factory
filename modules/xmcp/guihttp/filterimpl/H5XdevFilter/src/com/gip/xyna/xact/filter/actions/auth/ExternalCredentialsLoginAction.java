/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateSessionException;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaUserCredentials;
import com.gip.xyna.xmcp.RMIChannelImpl;

import xmcp.auth.ExternalCredentialsLoginRequest;

/**
 * vgl ExternalUserLoginInformationAction
 * request:
 * {
 * "username":"<username>""
 * "password":"<password>"
 * "force":"true"
 * "domain":"<domainname>"
 * }
 * 
 * antwort:
 * so wie beim login, nur dass die sessionerzeugung �ber die externe domain
 * passiert
 * 
 */
public class ExternalCredentialsLoginAction implements FilterAction {

  // private static final Logger logger =
  // CentralFactoryLogging.getLogger(ExternalUserLoginAction.class);

  private static final Exception notAuthorizedException = new Exception("Session could not be authorized.");
  // private static final Exception internalServerError = new Exception("Internal
  // Server Error");
  static {
    notAuthorizedException.setStackTrace(new StackTraceElement[0]);
    // internalServerError.setStackTrace(new StackTraceElement[0]);
  }

  private XMOMGui xmomgui;

  public ExternalCredentialsLoginAction(XMOMGui xmomgui) {
    this.xmomgui = xmomgui;
  }

  @Override
  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith("/auth/externalCredentialsLogin") && Method.POST == method;
  }

  public String getTitle() {
    return "External Credentials Login";
  }

  @Override
  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    String payload = AuthUtils.insertFqnIfNeeded(tc.getPayload(), "xmcp.auth.ExternalCredentialsLoginRequest");

    // parsing
    ExternalCredentialsLoginRequest request = (ExternalCredentialsLoginRequest) Utils
        .convertJsonToGeneralXynaObjectUsingGuiHttp(payload);

    // session erzeugen
    String username = request.getUsername();
    String password = request.getPassword();
    boolean force = request.getForce() != null ? request.getForce() : true;
    String domainName = request.getDomain();
    // password can be found in the correlated Xyna Order
    XynaUserCredentials userCredentials = new XynaUserCredentials(username, password);
    SessionCredentials creds = XynaFactory.getInstance().getFactoryManagement()
        .createSession(userCredentials, Optional.<String>empty(), force);

    // session fremd-authorisieren
    try {
      // logger.info("ExternalCredentialsLogin with: " + username + ", " + password +
      // ", " + domainName);

      /*
       * TODO
       * neue Domain in Zeta-Auth-Login bekannt machen: Domain anlegen mit WG
       */
      if (!new RMIChannelImpl().authorizeSession(userCredentials, domainName,
          new XynaPlainSessionCredentials(creds.getSessionId(), creds.getToken()))) {
        return error(creds, tc, jfai);
      }
    } catch (RemoteException e) {
      if (e.getMessage().contains("XYNA-04049")) {
        return error(creds, tc, jfai, new XFMG_DuplicateSessionException(username));
      } else {
        return error(creds, tc, jfai, e);
      }
    }

    return LoginAction.createLoginResponse(jfai, tc, creds, request.getPath(), xmomgui);
  }

  private FilterActionInstance error(SessionCredentials creds, HTTPTriggerConnection tc, JsonFilterActionInstance jfai)
      throws SocketNotAvailableException {
    return error(creds, tc, jfai, notAuthorizedException);
  }

  private FilterActionInstance error(SessionCredentials creds, HTTPTriggerConnection tc, JsonFilterActionInstance jfai,
      Throwable e)
      throws SocketNotAvailableException {
    AuthUtils.replyError(tc, jfai, Status.unauthorized, e);
    return jfai;
  }

  @Override
  public void appendIndexPage(HTMLPart body) {

  }

  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }
}
