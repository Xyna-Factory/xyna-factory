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



import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.gip.xyna.xact.filter.methods.McpMethodHandler;
import com.gip.xyna.xact.filter.methods.McpMethodHandler.Era;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;

import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;

import static com.gip.xyna.xact.filter.ErrorMessages.ErrorCodes;



public class RequestValidation {

  /**
   * functions are called in order until one returns false
   * if all return true, the request is valid.
   * functions send an appropriate response, if they return false
   */
  private static final List<Function<RequestValidationData, Boolean>> modernValidationFunctions =
      List.of(RequestValidation::validateJsonRpcAndId, //
              RequestValidation::validateMetaAndProtocolVersion, //
              RequestValidation::validateMirroredHeaders);


  public boolean validateRequest(HTTPTriggerConnection tc, JSONObject obj, Optional<McpLegacySession> sessionOpt, Era era) {
    if (era == Era.LEGACY) {
      return validateLegacyRequest(tc, obj, sessionOpt);
    } else {
      return validateModernRequest(tc, obj);
    }
  }


  private boolean validateLegacyRequest(HTTPTriggerConnection tc, JSONObject obj, Optional<McpLegacySession> sessionOpt) {
    String method = obj.getMember("method").getStringOrNumberValue();
    if (!"initialize".equals(method)) {
      if (sessionOpt == null) {
        ErrorMessages.sendMissingSessionIdResponse(tc);
        return false;
      }
      if (sessionOpt.isEmpty()) {
        ErrorMessages.sendUnknownSessionIdResponse(tc);
        return false;
      }
    } else {
      JSONValue id = obj.getMember("id");
      JSONValue val = McpServerFilter.getNestedValue(obj, "params", "protocolVersion");
      String protocolVersionString = val == null ? null : val.getStringOrNumberValue();
      if (!McpServerFilter.SUPPORTED_VERSIONS.contains(protocolVersionString)) {
        ErrorMessages.sendUnsupportedProtocolVersion(tc, id, protocolVersionString);
        return false;
      }

    }
    return true;
  }


  private static boolean validateJsonRpcAndId(RequestValidationData data) {
    HTTPTriggerConnection tc = data.getTc();
    JSONObject obj = data.getObj();
    JSONValue id = obj.getMember("id");
    JSONValue jsonRpc = obj.getMember("jsonrpc");
    if (jsonRpc == null) {
      ErrorMessages.sendBadRequestResponse(tc, id, ErrorCodes.JSON_RPC_INVALID_REQUEST, "Invalid params: missing required jsonRpc field");
      return false;
    }
    if (!Objects.equals("2.0", jsonRpc.getStringOrNumberValue())) {
      ErrorMessages.sendBadRequestResponse(tc, id, ErrorCodes.JSON_RPC_INVALID_REQUEST, "Invalid params: jsonrpc is not 2.0");
      return false;
    }

    if (id == null || id.getStringOrNumberValue() == null || id.getStringOrNumberValue().isEmpty()) {
      ErrorMessages.sendBadRequestResponse(tc, id, ErrorCodes.JSON_RPC_INVALID_REQUEST, "Invalid params: id not set");
      return false;
    }

    return true;
  }


  private static boolean validateMetaAndProtocolVersion(RequestValidationData data) {
    HTTPTriggerConnection tc = data.getTc();
    JSONObject obj = data.getObj();
    JSONValue id = obj.getMember("id");
    JSONValue _meta = McpServerFilter.getNestedValue(obj, "params", "_meta");
    if (_meta == null || _meta.getObjectValue() == null) {
      ErrorMessages.sendBadRequestResponse(tc, id, ErrorCodes.JSON_RPC_INVALID_PARAMS, "Invalid params: params/_meta missing");
      return false;
    }
    JSONValue protocolVersion = _meta.getObjectValue().getMember("io.modelcontextprotocol/protocolVersion");
    if (protocolVersion == null) {
      ErrorMessages.sendBadRequestResponse(tc, id, ErrorCodes.JSON_RPC_INVALID_PARAMS,
                                           "Invalid params: io.modelcontextprotocol/protocolVersion missing in _meta");
      return false;
    }
    String protocolVersionString = protocolVersion.getStringOrNumberValue();
    if (protocolVersionString == null || !McpServerFilter.SUPPORTED_VERSIONS.contains(protocolVersionString)) {
      ErrorMessages.sendUnsupportedProtocolVersion(tc, id, protocolVersionString);
      return false;
    }

    JSONValue clientCaps = _meta.getObjectValue().getMember("io.modelcontextprotocol/clientCapabilities");
    if (clientCaps == null) {
      ErrorMessages.sendBadRequestResponse(tc, id, ErrorCodes.JSON_RPC_INVALID_PARAMS,
                                           "Invalid params: io.modelcontextprotocol/clientCapabilities missing in _meta");
      return false;
    }
    if (!Objects.equals("OBJECT", clientCaps.getType())) {
      ErrorMessages.sendBadRequestResponse(tc, id, ErrorCodes.JSON_RPC_INVALID_PARAMS,
                                           "Invalid params: io.modelcontextprotocol/clientCapabilities not an object");
      return false;
    }
    return true;
  }


  private static boolean validateMirroredHeaders(RequestValidationData data) {
    JSONObject obj = data.getObj();
    JSONValue id = data.getObj().getMember("id");
    JSONValue val;

    String protocolVersionHeader = data.getTc().getHeader().getProperty(McpMethodHandler.PROTOCOL_VERSION_HEADER.toLowerCase());
    val = McpServerFilter.getNestedValue(obj, "params", "_meta", "io.modelcontextprotocol/protocolVersion");
    String version = val == null ? null : val.getStringOrNumberValue();
    if (!Objects.equals(protocolVersionHeader, version)) {
      ErrorMessages.sendBadRequestResponse(data.getTc(), id, ErrorCodes.MCP_HEADER_MISMATCH,
                                           "Header Mismatch: " + McpMethodHandler.PROTOCOL_VERSION_HEADER);
      return false;
    }

    String methodHeader = data.getTc().getHeader().getProperty(McpMethodHandler.MCP_METHOD_HEADER.toLowerCase());
    val = obj.getMember("method");
    String method = val == null ? null : val.getStringOrNumberValue();
    if (!Objects.equals(methodHeader, method)) {
      ErrorMessages.sendBadRequestResponse(data.getTc(), id, ErrorCodes.MCP_HEADER_MISMATCH,
                                           "Header Mismatch: " + McpMethodHandler.MCP_METHOD_HEADER);
      return false;
    }

    if (method.equals("tools/call") || method.equals("resources/read") || method.equals("prompts/get")) {
      String nameHeader = data.getTc().getHeader().getProperty(McpMethodHandler.MCP_NAME_HEADER.toLowerCase());
      val = McpServerFilter.getNestedValue(obj, "params", "name");
      String paramName = val == null ? null : val.getStringOrNumberValue();
      val = McpServerFilter.getNestedValue(obj, "params", "uri");
      String paramUri = val == null ? null : val.getStringOrNumberValue();
      if ((paramName != null && !Objects.equals(nameHeader, paramName)) || (paramUri != null && !Objects.equals(nameHeader, paramUri))) {
        ErrorMessages.sendBadRequestResponse(data.getTc(), id, ErrorCodes.MCP_HEADER_MISMATCH,
                                             "Header Mismatch: " + McpMethodHandler.MCP_NAME_HEADER);
        return false;
      }
    }
    return true;
  }


  private boolean validateModernRequest(HTTPTriggerConnection tc, JSONObject obj) {
    RequestValidationData data = new RequestValidationData(tc, obj);
    for (Function<RequestValidationData, Boolean> validation : modernValidationFunctions) {
      if (!validation.apply(data)) {
        return false;
      }
    }
    return true;
  }


  private static class RequestValidationData {

    private final HTTPTriggerConnection tc;
    private final JSONObject obj;


    public RequestValidationData(HTTPTriggerConnection tc, JSONObject obj) {
      this.tc = tc;
      this.obj = obj;
    }


    public HTTPTriggerConnection getTc() {
      return tc;
    }


    public JSONObject getObj() {
      return obj;
    }


  }
}
