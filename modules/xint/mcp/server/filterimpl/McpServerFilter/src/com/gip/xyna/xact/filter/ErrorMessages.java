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



import java.nio.charset.Charset;

import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xact.filter.methods.McpMethodHandler;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;

import xfmg.xfctrl.datamodel.json.JSONValue;



public class ErrorMessages {


  public interface ErrorCodes {

    public static final int MCP_HEADER_MISMATCH = -32020;
    public static final int MCP_UNSUPPORTED_PROTOCOL_VERSION = -32022;

    public static final int JSON_RPC_INVALID_REQUEST = -32600;
    public static final int JSON_RPC_METHOD_NOT_FOUND = -32601;
    public static final int JSON_RPC_INVALID_PARAMS = -32602;
    public static final int JSON_RPC_INTERNAL_ERROR = -32603;
    public static final int JSON_RPC_PARSE_ERROR = -32700;
  }


  private static final byte[] UNKNOWN_SESSIONID_RESPONSE = createInvalidRequestResponse("Invalid Request: unknown or expired session");
  private static final long UNKNOWN_SESSIONID_REPONSE_SIZE = UNKNOWN_SESSIONID_RESPONSE.length;


  private static final byte[] MISSING_SESSIONID_RESPONSE = createInvalidRequestResponse("Invalid Request: missing Mcp-session-id header");
  private static final long MISSING_SESSIONID_REPONSE_SIZE = MISSING_SESSIONID_RESPONSE.length;


  public static void sendUnknownSessionIdResponse(HTTPTriggerConnection tc) {
    McpServerFilter.send(tc, //
                         HTTPTriggerConnection.HTTP_UNAUTHORIZED, //
                         McpMethodHandler.MIME_JSON, //
                         null, // 
                         UNKNOWN_SESSIONID_RESPONSE, //
                         UNKNOWN_SESSIONID_REPONSE_SIZE);
  }


  public static void sendMissingSessionIdResponse(HTTPTriggerConnection tc) {
    McpServerFilter.send(tc, //
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
    McpServerFilter.addIdToBuilder(sb, id);
    sb.addObjectAttribute("error");
    sb.addNumberAttribute("code", code);
    sb.addStringAttribute(message, message);
    sb.endObject();
    sb.endObject();
    McpServerFilter.send(tc, HTTPTriggerConnection.HTTP_BADREQUEST, McpMethodHandler.MIME_JSON, null, sb.toString());
  }


  public static void sendInternalError(HTTPTriggerConnection tc, JSONValue id, String action) {
    JsonBuilder jb = new JsonBuilder();
    jb.startObject();
    jb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(jb, id);
    jb.addObjectAttribute("error");
    jb.addNumberAttribute("code", ErrorCodes.JSON_RPC_INTERNAL_ERROR);
    jb.addStringAttribute("message", "Internal error");
    jb.addObjectAttribute("data");
    jb.addStringAttribute("reason", String.format("Unhandled exception while %s", action));
    jb.endObject();
    jb.endObject();
    McpServerFilter.send(tc, HTTPTriggerConnection.HTTP_OK, McpMethodHandler.MIME_JSON, null, jb.toString());
  }


  public static void sendPrimitiveNotFound(HTTPTriggerConnection tc, JSONValue id, String type, String requestedPrimitive) {
    JsonBuilder jb = new JsonBuilder();
    jb.startObject();
    jb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(jb, id);
    jb.addObjectAttribute("error");
    jb.addNumberAttribute("code", ErrorCodes.JSON_RPC_INVALID_PARAMS);
    jb.addStringAttribute("message", String.format("Invalid params: %s '%s' not found", type, requestedPrimitive));
    jb.addObjectAttribute("data");
    jb.addStringAttribute("name", String.format("nonexistent_%s", type));
    jb.endObject();
    jb.endObject();
    jb.endObject();
    McpServerFilter.send(tc, HTTPTriggerConnection.HTTP_OK, McpMethodHandler.MIME_JSON, null, jb.toString());
  }


  public static void sendUnsupportedProtocolVersion(HTTPTriggerConnection tc, JSONValue id, String unsupportedVersion) {
    JsonBuilder jb = new JsonBuilder();
    jb.startObject();
    jb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(jb, id);
    jb.addObjectAttribute("error");
    jb.addNumberAttribute("code", ErrorCodes.JSON_RPC_INVALID_PARAMS);
    jb.addStringAttribute("message", "Unsupported protocol version");
    jb.addObjectAttribute("data");
    jb.addStringListAttribute("supported", McpServerFilter.SUPPORTED_VERSIONS);
    jb.addStringAttribute("requested", unsupportedVersion);
    jb.endObject();
    jb.endObject();
    jb.endObject();
    McpServerFilter.send(tc, HTTPTriggerConnection.HTTP_BADREQUEST, McpMethodHandler.MIME_JSON, null, jb.toString());
  }


  public static void sendMethodNotFoundResponse(HTTPTriggerConnection tc, JSONValue id, String method) {
    JsonBuilder sb = new JsonBuilder();
    sb.startObject();
    sb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(sb, id);
    sb.addObjectAttribute("error");
    sb.addNumberAttribute("code", ErrorCodes.JSON_RPC_METHOD_NOT_FOUND);
    sb.addStringAttribute("message", "Method not found");
    sb.addObjectAttribute("data");
    sb.addStringAttribute("method", method);
    sb.endObject();
    sb.endObject();
    sb.endObject();
    McpServerFilter.send(tc, HTTPTriggerConnection.HTTP_OK, McpMethodHandler.MIME_JSON, null, sb.toString());
  }


  private static byte[] createInvalidRequestResponse(String msg) {
    JsonBuilder sb = new JsonBuilder();
    sb.startObject();
    sb.addStringAttribute("jsonrpc", "2.0");
    sb.addObjectAttribute("error");
    sb.addNumberAttribute("code", ErrorCodes.JSON_RPC_INVALID_REQUEST);
    sb.addStringAttribute("message", msg);
    sb.endObject();
    sb.endObject();
    return sb.toString().getBytes(Charset.forName("UTF8"));
  }

}
