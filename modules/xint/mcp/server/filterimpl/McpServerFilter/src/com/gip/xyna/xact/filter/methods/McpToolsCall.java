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
package com.gip.xyna.xact.filter.methods;



import java.util.List;
import java.util.Objects;

import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xact.filter.McpPrimitivesData;
import com.gip.xyna.xact.filter.McpServerFilter;
import com.gip.xyna.xact.filter.serialization.ContentSerializer;
import com.gip.xyna.xact.filter.serialization.JsonObjectSerializer;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;

import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;
import xint.mcp.schema.Content;
import xint.mcp.schema.Tool;
import xint.mcp.schema.ToolCallResult;



public class McpToolsCall implements McpMethodHandler {

  @Override
  public void process(McpRequestData data) {
    JsonBuilder jb = new JsonBuilder();
    String toolName = data.getPayload().getMember("params").getObjectValue().getMember("name").getStringOrNumberValue();
    JSONValue arsValue = McpServerFilter.getNestedValue(data.getPayload(), "params", "arguments");    
    JSONObject arguments = arsValue == null ? new JSONObject.Builder().instance() : arsValue.getObjectValue();
    McpPrimitivesData primitives = data.getPrimitivesData();

    List<Tool> tools = primitives.getTools();
    ToolCallResult result = null;
    for (Tool tool : tools) {
      if (Objects.equals(tool.getName(), toolName)) {
        try {
          result = tool.call(arguments);
          break;
        } catch (Exception e) {
          jb.startObject();
          jb.addStringAttribute("jsonrpc", "2.0");
          McpServerFilter.addIdToBuilder(jb, data.getPayload().getMember("id"));
          jb.addObjectAttribute("error");
          jb.addNumberAttribute("code", -32603);
          jb.addStringAttribute("message", "Internal error");
          jb.addObjectAttribute("data");
          jb.addStringAttribute("reason", "Unhandled exception while executing tool");
          jb.addStringAttribute("tool", tool.getName());
          McpServerFilter.addIdToBuilder(jb, data.getPayload().getMember("id"), "requestId");
          jb.endObject();
          jb.endObject();
          jb.endObject();
          McpServerFilter.send(data.getTc(), HTTPTriggerConnection.HTTP_OK, MIME_JSON, null, jb.toString());
        }
      }
    }

    if (result == null) {
      jb.startObject();
      jb.addStringAttribute("jsonrpc", "2.0");
      McpServerFilter.addIdToBuilder(jb, data.getPayload().getMember("id"));
      jb.addObjectAttribute("error");
      jb.addNumberAttribute("code", -32602);
      jb.addStringAttribute("message", "Invalid params: tool not found");
      jb.addObjectAttribute("data");
      jb.addStringAttribute("name", "nonexistent_tool");
      jb.endObject();
      jb.endObject();
      jb.endObject();
      McpServerFilter.send(data.getTc(), HTTPTriggerConnection.HTTP_OK, MIME_JSON, null, jb.toString());
    }

    jb.startObject();
    jb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(jb, data.getPayload().getMember("id"));
    jb.addObjectAttribute("result");
    jb.addListAttribute("content");
    if (result.getContent() != null) {
      for (Content content : result.getContent()) {
        jb.addObjectListElement(new ContentSerializer(content));
      }
    }
    jb.endList();
    if (result.getStructuredContent() != null) {
      jb.addObjectAttribute("structuredContent", new JsonObjectSerializer(result.getStructuredContent()));
    }
    jb.addBooleanAttribute("isError", result.getIsError());
    jb.endObject();
    jb.endObject();
    McpServerFilter.send(data.getTc(), HTTPTriggerConnection.HTTP_OK, MIME_JSON, null, jb.toString());
  }

}
