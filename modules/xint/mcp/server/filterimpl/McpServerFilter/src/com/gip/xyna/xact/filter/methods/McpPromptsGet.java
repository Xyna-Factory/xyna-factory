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
import com.gip.xyna.xact.filter.ErrorMessages;
import com.gip.xyna.xact.filter.McpPrimitivesData;
import com.gip.xyna.xact.filter.McpServerFilter;
import com.gip.xyna.xact.filter.serialization.PromptMessageSerializer;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;

import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;
import xint.mcp.schema.GetPromptResult;
import xint.mcp.schema.Prompt;
import xint.mcp.schema.PromptMessage;



public class McpPromptsGet implements McpMethodHandler {

  @Override
  public void process(McpRequestData data) {
    JsonBuilder jb = new JsonBuilder();
    McpPrimitivesData primitives = data.getPrimitivesData();
    String name = McpServerFilter.getNestedValue(data.getPayload(), "params", "name").getStringOrNumberValue();
    JSONValue argumentsValue = McpServerFilter.getNestedValue(data.getPayload(), "params", "arguments");
    JSONObject arguments = null;
    if (argumentsValue != null && Objects.equals("OBJECT", argumentsValue.getType())) {
      arguments = argumentsValue.getObjectValue();
    } else {
      arguments = new JSONObject.Builder().instance();
    }
    GetPromptResult result = null;
    List<Prompt> prompts = primitives.getPrompts();
    for (Prompt prompt : prompts) {
      if (Objects.equals(name, prompt.getName())) {
        result = prompt.get(arguments);
      }
    }

    if (result == null) {
      ErrorMessages.sendPrimitiveNotFound(data.getTc(), data.getPayload().getMember("id"), "prompt", name);
      return;
    }

    jb.startObject();
    jb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(jb, data.getPayload().getMember("id"));
    jb.addObjectAttribute("result");
    jb.addStringAttribute("resultType", "complete");
    jb.addStringAttribute("description", result.getDescription());
    jb.addListAttribute("messages");
    if (result.getMessages() != null) {
      for (PromptMessage message : result.getMessages()) {
        jb.addObjectListElement(new PromptMessageSerializer(message));
      }
    }
    jb.endList();
    jb.endObject();
    jb.endObject();

    McpServerFilter.send(data.getTc(), HTTPTriggerConnection.HTTP_OK, MIME_JSON, null, jb.toString());
  }

}
