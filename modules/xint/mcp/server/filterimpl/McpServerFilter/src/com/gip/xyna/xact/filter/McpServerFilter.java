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
package com.gip.xyna.xact.filter;



import com.gip.xyna.xact.filter.methods.McpInitialize;
import com.gip.xyna.xact.filter.methods.McpListPrompts;
import com.gip.xyna.xact.filter.methods.McpMethodHandler;
import com.gip.xyna.xact.filter.methods.McpMethodHandler.Era;
import com.gip.xyna.xact.filter.methods.McpMethodHandler.McpRequestData;
import com.gip.xyna.xact.filter.methods.McpNotificationsInitialized;
import com.gip.xyna.xact.filter.methods.McpPing;
import com.gip.xyna.xact.filter.methods.McpPromptsGet;
import com.gip.xyna.xact.filter.methods.McpResourcesList;
import com.gip.xyna.xact.filter.methods.McpResourcesRead;
import com.gip.xyna.xact.filter.methods.McpServerDiscover;
import com.gip.xyna.xact.filter.methods.McpToolsCall;
import com.gip.xyna.xact.filter.methods.McpToolsList;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.RevisionChangeUnDeploymentHandler;
import xact.templates.Document;
import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;



public class McpServerFilter extends ConnectionFilter<HTTPTriggerConnection> {

  private static final long serialVersionUID = 1L;

  private static Logger logger = CentralFactoryLogging.getLogger(McpServerFilter.class);

  public static final List<String> SUPPORTED_VERSIONS = List.of("2026-07-28", "2025-11-25");

  private static final byte[] UNKNOWN_SESSIONID_RESPONSE = createUnknownSessionIdResponse();
  private static final long UNKNOWN_SESSIONID_REPONSE_SIZE = UNKNOWN_SESSIONID_RESPONSE.length;


  private static final byte[] MISSING_SESSIONID_RESPONSE = createMissingSessionIdResponse();
  private static final long MISSING_SESSIONID_REPONSE_SIZE = MISSING_SESSIONID_RESPONSE.length;


  private static final McpServerDiscover MRTHOD_SERVER_DISCOVER = new McpServerDiscover();
  private static final McpInitialize METHOD_INITIALIZE = new McpInitialize();
  private static final McpNotificationsInitialized METHOD_NOTIFICATIONS_INITIALIZED = new McpNotificationsInitialized();
  private static final McpPing METHOD_PING = new McpPing();
  private static final McpToolsList METHOD_TOOLS_LIST = new McpToolsList();
  private static final McpToolsCall METHOD_TOOLS_CALL = new McpToolsCall();
  private static final McpResourcesList METHOD_RESOURCES_LIST = new McpResourcesList();
  private static final McpResourcesRead METHOD_RESOURCES_READ = new McpResourcesRead();
  private static final McpListPrompts METHOD_PROMPTS_LIST = new McpListPrompts();
  private static final McpPromptsGet METHOD_PROMPTS_GET = new McpPromptsGet();


  private static final RequestValidation REQUEST_VALIDATION = new RequestValidation();

  private static final Map<String, McpLegacySession> legacySessions = new ConcurrentHashMap<>();
  private static final Map<Integer, McpPrimitivesData> infoPerTrigger = new ConcurrentHashMap<>();


  /**
   * Called to create a configuration template to parse configuration and show configuration options.
   * @return McpServerConfigurationParameter template
   */
  @Override
  public FilterConfigurationParameter createFilterConfigurationTemplate() {
    return new McpServerConfigurationParameter();
  }


  /**
   * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection.
   * This method returns a FilterResponse object, which includes the XynaOrder if the filter is responsible for the request.
   * # If this filter is not responsible the returned object must be: FilterResponse.notResponsible()
   * # If this filter is responsible the returned object must be: FilterResponse.responsible(XynaOrder order)
   * # If this filter is responsible but the request is handled without creating a XynaOrder the 
   *   returned object must be: FilterResponse.responsibleWithoutXynaorder()
   * # If this filter is responsible but the request should be handled by an older version of the filter in another application version, the returned
   *    object must be: FilterResponse.responsibleButTooNew().
   * @param tc
   * @return FilterResponse object
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error.
   *         Results in onError() being called by Xyna Processing.
   */
  @Override
  public FilterResponse createXynaOrder(HTTPTriggerConnection tc, FilterConfigurationParameter baseConfig) throws XynaException {
    McpServerConfigurationParameter config = (McpServerConfigurationParameter) baseConfig;
    try {
      tc.readHeader();
      tc.readPayload();
    } catch (Exception e) {
      return FilterResponse.notResponsible();
    }

    String url = tc.getUri();

    if (!url.endsWith(config.getEndpoint())) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("This MCP Filter is not responsible for %s. Endpoint: %s", url, config.getEndpoint()));
      }
      return FilterResponse.notResponsible();
    }

    String payload = tc.getPayload();
    if (logger.isTraceEnabled()) {
      logger.trace(String.format("This MCP Filter is responsible for %s. Payload: %s", payload));
    }

    if (tc.getMethodEnum() == Method.DELETE) {
      deleteSession(tc);
      return FilterResponse.responsibleWithoutXynaorder();
    }

    if (tc.getMethodEnum() == Method.GET) {
      sendNoAsync(tc, payload);
      return FilterResponse.responsibleWithoutXynaorder();
    }

    JSONObject obj;
    try {
      obj = JSONObject.fromJson(new Document.Builder().text(payload).instance());
    } catch (Exception e) {
      tc.sendErrorResponse("400 Bad Request", "Invalid Json"); //TODO: check specification
      return FilterResponse.responsibleWithoutXynaorder();
    }

    processRequest(tc, obj);

    return FilterResponse.responsibleWithoutXynaorder();
  }


  private void sendNoAsync(HTTPTriggerConnection tc, String payload) {
    Properties properties = new Properties();
    properties.setProperty("Allow", "POST");
    send(tc, "405 Method Not Allowed", McpMethodHandler.MIME_JSON, properties, "");
  }


  private void deleteSession(HTTPTriggerConnection tc) {
    String sessionId = tc.getHeader().getProperty(McpMethodHandler.SESSIONID_HEADER.toLowerCase(), null);

    if (sessionId == null) {
      sendUnknownSessionIdResponse(tc);
    }

    if (legacySessions.remove(sessionId) != null) {
      try {
        tc.sendResponse("");
      } catch (SocketNotAvailableException e) {

      }
    } else {
      sendUnknownSessionIdResponse(tc);
    }
  }


  private void processRequest(HTTPTriggerConnection tc, JSONObject obj) {
    String method = obj.getMember("method").getStringOrNumberValue();
    Optional<McpLegacySession> sessionOpt = getSessionFromConnection(tc);;
    McpLegacySession session = sessionOpt == null ? null : sessionOpt.isEmpty() ? null : sessionOpt.get();
    Era era = Era.determineEra(obj);
    if (!REQUEST_VALIDATION.validateRequest(tc, obj, sessionOpt, era)) {
      return;
    }

    infoPerTrigger.computeIfAbsent(tc.getTrigger().hashCode(), (x) -> new McpPrimitivesData(getRevision()));
    McpPrimitivesData primitivesData = infoPerTrigger.get(tc.getTrigger().hashCode());

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("processing request %s, [%s]", method, era));
    }

    McpRequestData data = new McpRequestData(tc, obj, legacySessions, session, primitivesData, era);
    switch (method) {
      case "initialize" :
        METHOD_INITIALIZE.process(data);
        break;
      case "notifications/initialized" :
        METHOD_NOTIFICATIONS_INITIALIZED.process(data);
        break;
      case "ping" :
        METHOD_PING.process(data);
        break;
      case "tools/list" :
        METHOD_TOOLS_LIST.process(data);
        break;
      case "tools/call" :
        METHOD_TOOLS_CALL.process(data);
        break;
      case "resources/list" :
        METHOD_RESOURCES_LIST.process(data);
        break;
      case "resources/read" :
        METHOD_RESOURCES_READ.process(data);
      case "prompts/list" :
        METHOD_PROMPTS_LIST.process(data);
        break;
      case "prompts/get" :
        METHOD_PROMPTS_GET.process(data);
        break;
      case "server/discover" :
        MRTHOD_SERVER_DISCOVER.process(data);
      default :
        sendMethodNotFoundResponse(tc, obj.getMember("id"), method);
        break;
    }
  }


  private Optional<McpLegacySession> getSessionFromConnection(HTTPTriggerConnection tc) {
    String sessionId = tc.getHeader().getProperty(McpMethodHandler.SESSIONID_HEADER.toLowerCase());
    if (sessionId == null) {
      return null;
    }
    return Optional.ofNullable(legacySessions.get(sessionId));
  }


  /**
   * Called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned GeneralXynaObject
   * @param tc corresponding triggerconnection
   */
  @Override
  public void onResponse(GeneralXynaObject response, HTTPTriggerConnection tc) {
    //TODO implementation
    //TODO update dependency xml file
  }


  /**
   * Called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, HTTPTriggerConnection tc) {
    //TODO implementation
    //TODO update dependency xml file
  }


  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    //TODO implementation
    //TODO update dependency xml file
    return null;
  }


  /**
   * Called once for each filter instance when it is deployed and again on each classloader change (e.g. when changing corresponding implementation jars).
   * @param triggerInstance trigger instance this filter instance is registered to
   */
  @SuppressWarnings("rawtypes")
  @Override
  public void onDeployment(EventListener triggerInstance) {
    super.onDeployment(triggerInstance);

    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .addDeploymentHandler(DeploymentHandling.PRIORITY_REMOTESERIALIZATION,
                              new RevisionChangeUnDeploymentHandler(McpServerFilter::invalidateRevisions));

    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .addUndeploymentHandler(DeploymentHandling.PRIORITY_REMOTESERIALIZATION,
                                new RevisionChangeUnDeploymentHandler(McpServerFilter::invalidateRevisions));

  }


  /**
   * Called once for each filter instance when it is undeployed and again on each classloader change (e.g. when changing corresponding implementation jars).
   * @param triggerInstance trigger instance this filter instance is registered to
   */
  @SuppressWarnings("rawtypes")
  @Override
  public void onUndeployment(EventListener triggerInstance) {
    super.onUndeployment(triggerInstance);

    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .removeDeploymentHandler(new RevisionChangeUnDeploymentHandler(McpServerFilter::invalidateRevisions));

    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .removeDeploymentHandler(new RevisionChangeUnDeploymentHandler(McpServerFilter::invalidateRevisions));

  }


  public static boolean send(HTTPTriggerConnection tc, String status, String mime, Properties responseHeader, String body) {
    try {
      byte[] msgBytes = body.getBytes(Charset.forName("UTF8"));
      send(tc, status, mime, responseHeader, msgBytes, Long.valueOf(msgBytes.length));
    } catch (Exception e) {
      return false;
    }
    return true;
  }


  public static boolean send(HTTPTriggerConnection tc, String status, String mime, Properties responseHeader, byte[] msgBytes, long size) {
    try {
      tc.sendResponse(status, mime, responseHeader, new ByteArrayInputStream(msgBytes), size);
    } catch (Exception e) {
      return false;
    }
    return true;
  }


  private static byte[] createUnknownSessionIdResponse() {
    JsonBuilder sb = new JsonBuilder();
    sb.startObject();
    sb.addStringAttribute("jsonrpc", "2.0");
    sb.addObjectAttribute("error");
    sb.addIntegerAttribute("code", -32600);
    sb.addStringAttribute("message", "Invalid Request: unknown or expired session");
    sb.endObject();
    sb.endObject();
    return sb.toString().getBytes(Charset.forName("UTF8"));
  }


  public static void sendUnknownSessionIdResponse(HTTPTriggerConnection tc) {
    send(tc, //
         HTTPTriggerConnection.HTTP_UNAUTHORIZED, //
         McpMethodHandler.MIME_JSON, //
         null, // 
         UNKNOWN_SESSIONID_RESPONSE, //
         UNKNOWN_SESSIONID_REPONSE_SIZE);
  }


  public static void sendMissingSessionIdResponse(HTTPTriggerConnection tc) {
    send(tc, //
         HTTPTriggerConnection.HTTP_UNAUTHORIZED, //
         McpMethodHandler.MIME_JSON, //
         null, // 
         MISSING_SESSIONID_RESPONSE, //
         MISSING_SESSIONID_REPONSE_SIZE);
  }


  public static void sendBadRequestResponse(HTTPTriggerConnection tc, JSONValue id, int code, String message) {
    JsonBuilder sb = new JsonBuilder();
    sb.startObject();
    sb.addStringAttribute("jsonrpc", "2.0");
    addIdToBuilder(sb, id);
    sb.addObjectAttribute("error");
    sb.addNumberAttribute("code", code);
    sb.addStringAttribute(message, message);
    sb.endObject();
    sb.endObject();
    send(tc, HTTPTriggerConnection.HTTP_BADREQUEST, McpMethodHandler.MIME_JSON, null, sb.toString());
  }


  private void sendMethodNotFoundResponse(HTTPTriggerConnection tc, JSONValue id, String method) {
    JsonBuilder sb = new JsonBuilder();
    sb.startObject();
    sb.addStringAttribute("jsonrpc", "2.0");
    addIdToBuilder(sb, id);
    sb.addObjectAttribute("error");
    sb.addNumberAttribute("code", -32601);
    sb.addStringAttribute("message", "Method not found");
    sb.addObjectAttribute("data");
    sb.addStringAttribute("method", method);
    sb.endObject();
    sb.endObject();
    sb.endObject();
    send(tc, HTTPTriggerConnection.HTTP_OK, McpMethodHandler.MIME_JSON, null, sb.toString());
  }


  private static byte[] createMissingSessionIdResponse() {
    JsonBuilder sb = new JsonBuilder();
    sb.startObject();
    sb.addStringAttribute("jsonrpc", "2.0");
    sb.addObjectAttribute("error");
    sb.addIntegerAttribute("code", -32600);
    sb.addStringAttribute("message", "Invalid Request: missing Mcp-session-id header");
    sb.endObject();
    sb.endObject();
    return sb.toString().getBytes(Charset.forName("UTF8"));
  }


  private static void invalidateRevisions(Collection<Long> revisions) {
    if (logger.isDebugEnabled()) {
      logger.debug("Invalidating " + revisions);
    }
    infoPerTrigger.clear();
  }


  public static void addIdToBuilder(JsonBuilder jb, JSONValue idValue) {
    if (idValue == null) {
      return;
    }
    if ("NUMBER".equals(idValue.getType())) {
      String idAsString = idValue.getStringOrNumberValue();
      if (idAsString.contains(".")) {
        jb.addNumberAttribute("id", Double.valueOf(idAsString));
      } else {
        jb.addNumberAttribute("id", Long.valueOf(idAsString));
      }
    } else {
      jb.addStringAttribute("id", idValue.getStringOrNumberValue());
    }
  }


  public static JSONValue getNestedValue(JSONObject obj, String... path) {
    JSONValue result = null;
    for (String p : path) {
      result = obj.getMember(p);
      if (result == null) {
        return null;
      }
      obj = result.getObjectValue();
    }
    return result;
  }


  public static String getRtcVersion(ClassLoader cb) {
    String result = "unknown";

    ClassLoaderBase clb = (ClassLoaderBase) cb;
    Long rev = clb.getRevision();
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try {
      RuntimeContext rc = rm.getRuntimeContext(rev);
      if (rc instanceof Workspace) {
        result = "workingset";
      } else if (rc instanceof Application) {
        result = ((Application) rc).getVersionName();
      }
    } catch (Exception e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not determine RuntimeContext.", e);
      }
    }

    return result;
  }
}
