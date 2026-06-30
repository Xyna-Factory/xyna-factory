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
import java.security.cert.CertificateException;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.Pair;
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

import xmcp.auth.ExternalUserLoginRequest;



/**
 * vgl ExternalUserLoginInformationAction
 * request:
 * {
 * "force":"true"
 * "domain":"<domainname>"
 * }
 * 
 * antwort:
 * so wie beim login, nur dass die sessionerzeugung über die externe domain passiert
 * 
 */
public class ExternalUserLoginAction implements FilterAction {

  private static final Logger logger = CentralFactoryLogging.getLogger(ExternalUserLoginAction.class);

  private static final String SSL_CLIENT_CERT = "SSL_CLIENT_CERT".toLowerCase();

  private static final Exception noUserInfoException = new Exception("No user info provided");
  private static final Exception notAuthorizedException = new Exception("Session could not be authorized.");
  private static final Exception internalServerError = new Exception("Internal Server Error");
  static {
    noUserInfoException.setStackTrace(new StackTraceElement[0]);
    notAuthorizedException.setStackTrace(new StackTraceElement[0]);
    internalServerError.setStackTrace(new StackTraceElement[0]);
  }
  
  private XMOMGui xmomgui;
  private static String headerName = SSL_CLIENT_CERT;
  private static ExternalAuthType loginType = ExternalAuthType.CLIENT_CERT;

  public static enum ExternalAuthType {
    CLIENT_CERT, JSON_WEB_TOKEN;
  }
  
  public ExternalUserLoginAction(XMOMGui xmomgui) {
    this.xmomgui = xmomgui;
  }

  public static void setExternalAuthType(ExternalAuthType type) {
    loginType = type;
  }

  public static void setAuthTokenHeaderName(String value) {
    headerName = value.toLowerCase();
  }

  @Override
  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith("/auth/externalUserLogin") && Method.POST == method;
  }


  public String getTitle() {
    return "External User Login";
  }



  public static Pair<Boolean, ExternalUserInfo> getExternalUserInfoOrFail(JsonFilterActionInstance jfai,
      HTTPTriggerConnection tc)
      throws XynaException {
    String header = tc.getHeader().getProperty(headerName);
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("getting user info for " + loginType + " from " + headerName + " with value " + header);
      }
      ExternalUserInfo eui;
      switch (loginType) {
        case JSON_WEB_TOKEN:
          eui = ExternalUserInfo.createFromJWT(header.replaceFirst("Bearer\\s+", ""));
          break;
        case CLIENT_CERT:
        default:
          eui = ExternalUserInfo.createFromClientCertificate(header);
      }
      return Pair.of(false, eui);
    } catch (RuntimeException e) {
      Utils.logError("Unexpected failure", e);
      AuthUtils.replyError(tc, jfai, Status.failed, internalServerError);
      return Pair.of(true, null);
    } catch (CertificateException e) {
      logger.trace(null, e);
      Exception ex = new Exception(e.getMessage());
      ex.setStackTrace(new StackTraceElement[0]);
      AuthUtils.replyError(tc, jfai, Status.badRequest, ex);
      return Pair.of(true, null);
    }
  }


  @Override
  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    String payload = AuthUtils.insertFqnIfNeeded(tc.getPayload(), "xmcp.auth.ExternalUserLoginRequest");
    Pair<Boolean, ExternalUserInfo> p = getExternalUserInfoOrFail(jfai, tc);
    if (p.getFirst()) {
      return jfai;
    }
    ExternalUserInfo eui = p.getSecond();
    if (eui == null) {
      AuthUtils.replyError(tc, jfai, Status.unauthorized, noUserInfoException);
      return jfai;
    }

    //parsing
    ExternalUserLoginRequest request = (ExternalUserLoginRequest) Utils.convertJsonToGeneralXynaObjectUsingGuiHttp(payload);

    //session erzeugen
    boolean force = request.getForce() != null ? request.getForce() : true;
    String domainName = request.getDomain();
    SessionCredentials creds = XynaFactory.getInstance().getFactoryManagement()
        .createSession(new XynaUserCredentials(eui.externalUserName, ""), Optional.<String> empty(), force);

    //session fremd-authorisieren
    try {
      if (!new RMIChannelImpl().authorizeSession(new XynaUserCredentials(eui.externalUserName, eui.externalUserPassword), domainName,
                                                 new XynaPlainSessionCredentials(creds.getSessionId(), creds.getToken()))) {
        return error(creds, tc, jfai);
      }
    } catch (RemoteException e) {
      if (e.getMessage().contains("XYNA-04049")) {
        return error(creds, tc, jfai, new XFMG_DuplicateSessionException(eui.externalUserName));
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

  
  private FilterActionInstance error(SessionCredentials creds, HTTPTriggerConnection tc, JsonFilterActionInstance jfai, Throwable e)
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
