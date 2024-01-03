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
package com.gip.xyna.xact.filter.actions.auth.utils;



import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocket;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.update.Updater;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xact.filter.H5XdevFilter;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.trigger.HTTPStartParameter;
import com.gip.xyna.xact.trigger.HTTPTrigger;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownSessionIDException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyEnum;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionDetails;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xmcp.RMIChannelImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;



public class AuthUtils {

  public final static String COOKIE_FIELD_SESSION_ID = "sessionId";
  public final static String COOKIE_FIELD_SESSION_ID_STS = "__Secure-sessionId";
  
  /**
   * @deprecated
   * deprecated with v9.0.3.0
   * XynaProperty: xmcp.guihttp.csrf
   */
  @Deprecated
  public final static String COOKIE_FIELD_TOKEN = "token";
  public final static String HEADER_FILED_CSRF_TOKEN = "xyna-csrf-token";
  public final static String COOKIE_MARKER_SECURE = "Secure";
  public final static String COOKIE_MARKER_HTTP_ONLY = "HttpOnly";
  public final static String COOKIE_MARKER_EXPIRED = "Expires=Thu, 01 Jan 1970 00:00:00 GMT";
  public final static String COOKIE_MARKER__PATH = "path=";
  public final static String COOKIE_MARKER_MAX_AGE = "max-age=";

  private static final Logger logger = CentralFactoryLogging.getLogger(AuthUtils.class);
  private static String factoryVersion;
  private static Object factoryVersionlock = new Object();
  
  
  public static final XynaPropertyBoolean USE_CSRF_TOKEN = new XynaPropertyBoolean("xmcp.guihttp.csrf", false)
      .setDefaultDocumentation(DocumentationLanguage.EN, "Add csrf token to login response and validate " + HEADER_FILED_CSRF_TOKEN + " header.")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Füge csrf token zur login response hinzu und validiere " + HEADER_FILED_CSRF_TOKEN + " heder.");


  public static void replyModellerLoginRequiredError(HTTPTriggerConnection tc, JsonFilterActionInstance jfai) throws SocketNotAvailableException {
    Role role = null;
    try {
      role = AuthUtils.authenticate(AuthUtils.readCredentialsFromRequest(tc));
    } catch (RemoteException e) {
      // user not logged in -> unauthorized
      replyLoginRequiredError(tc, jfai);
    }
    
    //logged in, but missing PROCESS_MODELLER right
    replyError(tc, jfai, Status.forbidden, new XFMG_ACCESS_VIOLATION(GuiRight.PROCESS_MODELLER.name(), role != null ? role.getName() : ""));
  }
  
  public static void replyError(HTTPTriggerConnection tc, JsonFilterActionInstance jfai, Exception e) throws SocketNotAvailableException {
    replyError(tc, jfai, Status.badRequest, e);
  }

  public static void replyLoginRequiredError(HTTPTriggerConnection tc, JsonFilterActionInstance jfai) throws SocketNotAvailableException{
    replyError(tc, jfai, Status.unauthorized, new RuntimeException("Login Required"));
  }

  public static void replyError(HTTPTriggerConnection tc, JsonFilterActionInstance jfai, Status status, Exception e)
      throws SocketNotAvailableException {
    jfai.sendJson(tc, status.getHttpStatus(), com.gip.xyna.xact.filter.util.Utils.xoToJson(com.gip.xyna.xact.filter.util.Utils.error(e)));
  }


  public static void replyError(HTTPTriggerConnection tc, JsonFilterActionInstance jfai, Status status, Throwable t)
      throws SocketNotAvailableException {
    jfai.sendJson(tc, status.getHttpStatus(), com.gip.xyna.xact.filter.util.Utils.xoToJson(com.gip.xyna.xact.filter.util.Utils.error(t)));
  }


  public static String getSessionDetailsJson(String sessionId, String token) throws PersistenceLayerException, XFMG_UnknownSessionIDException {
    SessionDetails details = XynaFactory.getInstance().getFactoryManagementPortal().getSessionDetails(sessionId);
    return writeSessionDetailsJson(details, token);
  }


  private static String writeSessionDetailsJson(SessionDetails details, String token) {
    JsonBuilder jb = new JsonBuilder();
    
    String user = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement()
    .resolveSessionToUser(details.getSessionId());
    
    jb.startObject();
    {
      jb.addStringAttribute("username", user);
      jb.addStringAttribute("sessionId", details.getSessionId());
      jb.addStringAttribute("role", details.getRole());
      jb.addStringListAttribute("rights", details.getRights());
      jb.addNumberAttribute("startTime", details.getStartTime());
      jb.addNumberAttribute("lastInteraction", details.getLastInteraction());
      jb.addNumberAttribute("serverTime", System.currentTimeMillis());
      jb.addNumberAttribute("serverId", XynaFactory.getInstance().hashCode());
      if(USE_CSRF_TOKEN.get()) {
        jb.addStringAttribute("sessionToken", token);
      }
      String xynaVersion;

      if (factoryVersion != null) {
        xynaVersion = factoryVersion;
      } else {
        synchronized (factoryVersionlock) {
          try {
            xynaVersion = Updater.getInstance().getVersionOfLastSuccessfulUpdate().getString();
            factoryVersion = xynaVersion;
          } catch (XPRC_VERSION_DETECTION_PROBLEM | PersistenceLayerException e) {
            logger.error("Could not determine xyna version", e);
            xynaVersion = "unknown";
          }
        }
      }


      jb.addStringAttribute("xynaVersion", xynaVersion);
    }
    jb.endObject();
    return jb.toString();
  }


  public static Map<String, String> readCookies(HTTPTriggerConnection tc) {
    Map<String, String> map = new HashMap<>();
    String value = tc.getHeader().getProperty("cookie");
    if (value != null) {
      String[] cookies;
      if (value.contains(";")) {
        cookies = value.split(";");
      } else {
        cookies = new String[] {value};
      }
      for (String cookie : cookies) {
        if (cookie.contains("=")) {
          String[] keyValue = cookie.split("=");
          map.put(keyValue[0].trim(), keyValue[1].trim());
        }
      }
    }
    return map;
  }


  public static XynaPlainSessionCredentials readCredentialsFromRequest(HTTPTriggerConnection tc) {
    Map<String, String> map = readCookies(tc);
    String sessionId = H5XdevFilter.STRICT_TRANSPORT_SECURITY.get() ? COOKIE_FIELD_SESSION_ID_STS : COOKIE_FIELD_SESSION_ID;
    if (USE_CSRF_TOKEN.get()) {
      String token = (String) tc.getHeader().get(HEADER_FILED_CSRF_TOKEN);
      return new XynaPlainSessionCredentials(map.get(sessionId), token);
    } else {
      return new XynaPlainSessionCredentials(map.get(sessionId), map.get(COOKIE_FIELD_TOKEN));
    }
  }


  public static Role authenticate(XynaPlainSessionCredentials xpsc) throws RemoteException {
    return RMIChannelImpl.authenticate(xpsc);
  }

  private static final XynaPropertyDuration maxageproperty =
      new XynaPropertyDuration("xmcp.guihttp.filter.h5xdev.cookie.maxage", new Duration(10, TimeUnit.HOURS));
  
  private static enum SameSite {
    
    STRICT("Strict"), 
    LAX("Lax"), 
    NONE("None");
    
    private final static String SAME_SITE_PREFIX = "SameSite=";
    
    private final String stringValue;
    
    SameSite(String cookieValue) {
      this.stringValue = cookieValue;
    }
    
    public void append(StringBuilder sb) {
      if (stringValue != null) {
        sb.append("; ").append(SAME_SITE_PREFIX).append(stringValue);
      }
    }
    
  }
  
  private static final XynaPropertyEnum<SameSite> samesiteproperty = //nicht erlauben, dass cookie für requests von anderen origins benutzt werden darf
                  new XynaPropertyEnum<SameSite>("xmcp.guihttp.filter.h5xdev.cookie.samesite", SameSite.class, SameSite.STRICT);
  


  public static String generateCookie(String key, String value, String path, HTTPTriggerConnection tc, boolean setMaxAge) {
    StringBuilder sb = new StringBuilder();
    sb.append(key).append('=').append(value).append("; ").append(COOKIE_MARKER_HTTP_ONLY);
    if (tc.getSocket() instanceof SSLSocket) {
      HTTPStartParameter sp = ((HTTPTrigger) tc.getTrigger()).getStartParameter();
      if (sp.useHTTPs()) {
        sb.append("; ").append(COOKIE_MARKER_SECURE);
      }
    }
    sb.append("; ").append(COOKIE_MARKER__PATH);
    if (path == null) {
      sb.append("/");
    } else {
      sb.append(path);
    }
    if (setMaxAge && maxageproperty.get().getDuration(TimeUnit.SECONDS) != 0) {
      sb.append("; ").append(COOKIE_MARKER_MAX_AGE).append(maxageproperty.get().getDuration(TimeUnit.SECONDS));
    }
    samesiteproperty.get().append(sb);
    return sb.toString();
  }


  //TODO: ZETA-71
  public static String insertFqnIfNeeded(String payload, String fqn) {

    if (payload == null || payload.length() == 0) {
      return "{ \"$meta\":{\"fqn\": \"" + fqn + "\"}}";
    }

    if (payload.contains("$meta")) {
      return payload;
    }

    return "{ \"$meta\":{\"fqn\": \"" + fqn + "\"}" + payload.substring(1);
  }

}
