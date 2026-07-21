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



import java.rmi.RemoteException;
import java.security.cert.CertificateException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.ConfigurableFilterAction;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.H5XdevFilterParameter;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.H5XdevFilterParameter;
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
public class ExternalUserLoginAction implements ConfigurableFilterAction {

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


  public static enum ExternalAuthType {
    CLIENT_CERT, JSON_WEB_TOKEN;
  }


  public ExternalUserLoginAction(XMOMGui xmomgui) {
    this.xmomgui = xmomgui;
  }


  @Override
  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith("/auth/externalUserLogin") && Method.POST == method;
  }


  public String getTitle() {
    return "External User Login";
  }


  /**
   * Config-aware entry point called by H5XdevFilter.
   * Casts the config to H5XdevFilterParameter and delegates to the actual logic.
   */
  @Override
  public FilterActionInstance actWithConfig(URLPath url, HTTPTriggerConnection tc, H5XdevFilterParameter config) throws XynaException {
    return actInternal(url, tc, config);
  }


  /**
   * Legacy entry point - called when no config is passed (backward compatibility).
   * Uses parameter defaults.
   */
  @Override
  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    return actInternal(url, tc, H5XdevFilterParameter.createDefaultConfig());
  }


  private FilterActionInstance actInternal(URLPath url, HTTPTriggerConnection tc, H5XdevFilterParameter config) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    String payload = AuthUtils.insertFqnIfNeeded(tc.getPayload(), "xmcp.auth.ExternalUserLoginRequest");
    Pair<Boolean, ExternalUserInfo> p = getExternalUserInfoOrFail(jfai, tc, config);
    if (p.getFirst()) {
      return jfai;
    }
    ExternalUserInfo eui = p.getSecond();
    if (eui == null) {
      AuthUtils.replyError(tc, jfai, Status.unauthorized, noUserInfoException);
      return jfai;
    }

    // parse request
    ExternalUserLoginRequest request = (ExternalUserLoginRequest) Utils.convertJsonToGeneralXynaObjectUsingGuiHttp(payload);

    // create session
    boolean force = request.getForce() != null ? request.getForce() : true;
    String domainName = request.getDomain();
    SessionCredentials creds = XynaFactory.getInstance().getFactoryManagement()
        .createSession(new XynaUserCredentials(eui.externalUserName, ""), Optional.<String> empty(), force);

    // encode optional selectedRole into password: "selectedRole\0jwtToken"
    // \0 is safe as JWT tokens are base64url-encoded and never contain this character
    String selectedRole = request.getSelectedRole();
    if (logger.isDebugEnabled()) {
      logger.debug("externalUserLogin: selectedRole from request='" + selectedRole + "'");
    }

    String externalUserPasswordWithRole =
        (selectedRole != null && !selectedRole.isEmpty()) ? selectedRole + "\0" + eui.externalUserPassword : eui.externalUserPassword;

    // authorize session through external domain
    try {
      if (!new RMIChannelImpl().authorizeSession(new XynaUserCredentials(eui.externalUserName, externalUserPasswordWithRole), domainName,
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


  static Pair<Boolean, ExternalUserInfo> getExternalUserInfoOrFail(JsonFilterActionInstance jfai, HTTPTriggerConnection tc, H5XdevFilterParameter config)
      throws XynaException {
    String headerName = config.getExternalAuthHeader().toLowerCase();
    ExternalAuthType loginType = config.getExternalAuthType();
    String header = tc.getHeader().getProperty(headerName);
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("getting user info for " + loginType + " from " + headerName + " with value " + header);
      }
      ExternalUserInfo eui;
      switch (loginType) {
        case JSON_WEB_TOKEN:
          String jwtHeader = header == null ? null : header.replaceFirst("Bearer\\s+", "");
          eui = ExternalUserInfo.createFromJWT(jwtHeader);
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
