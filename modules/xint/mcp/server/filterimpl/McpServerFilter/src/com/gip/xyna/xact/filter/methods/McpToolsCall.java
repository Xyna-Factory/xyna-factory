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
import xint.mcp.schema.Content;
import xint.mcp.schema.Tool;
import xint.mcp.schema.ToolCallResult;



public class McpToolsCall implements McpMethodHandler {

  @Override
  public void process(McpRequestData data) {
    JsonBuilder sb = new JsonBuilder();
    String toolName = data.getPayload().getMember("params").getObjectValue().getMember("name").getStringOrNumberValue();
    JSONObject arguments = data.getPayload().getMember("params").getObjectValue().getMember("arguments").getObjectValue();
    McpPrimitivesData primitives = data.getPrimitivesData();

    List<Tool> tools = primitives.getTools();
    ToolCallResult result = null;
    for (Tool tool : tools) {
      if (Objects.equals(tool.getName(), toolName)) {
        //TODO: validate input against schema
        try {
          result = tool.call(arguments);
          break;
        } catch (Exception e) {
          //TODO: error result
        }
      }
    }
    
    if(result == null) {
      //ToDO: no tool found
    }
    sb.startObject();
    sb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(sb, data.getPayload().getMember("id"));
    sb.addObjectAttribute("result");
    sb.addListAttribute("content");
    if(result.getContent() != null) {
      for(Content content : result.getContent()) {
        sb.addObjectListElement(new ContentSerializer(content));
      }
    }
    sb.endList();
    if(result.getStructuredContent() != null) {
      sb.addObjectAttribute("structuredContent", new JsonObjectSerializer(result.getStructuredContent()));
    }
    sb.addBooleanAttribute("isError", result.getIsError());
    sb.endObject();
    sb.endObject();
    McpServerFilter.send(data.getTc(), HTTPTriggerConnection.HTTP_OK, MIME_JSON, null, sb.toString());
  }

}
