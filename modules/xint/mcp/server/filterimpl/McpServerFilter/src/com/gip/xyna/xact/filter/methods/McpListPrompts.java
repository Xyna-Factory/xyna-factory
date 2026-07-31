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

import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xact.filter.McpPrimitivesData;
import com.gip.xyna.xact.filter.McpServerFilter;
import com.gip.xyna.xact.filter.serialization.PromptSerializer;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;

import xint.mcp.schema.Prompt;

public class McpListPrompts implements McpMethodHandler  {

  @Override
  public void process(McpRequestData data) {
    JsonBuilder sb = new JsonBuilder();
    McpPrimitivesData primitives = data.getPrimitivesData();
    sb.startObject();
    sb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(sb, data.getPayload().getMember("id"));
    sb.addObjectAttribute("result");
    if (data.getEra() == Era.MODERN) {
      sb.addStringAttribute("resultType", "complete");
    }
    sb.addListAttribute("prompts");
    
    for(Prompt prompt : primitives.getPrompts()) {
      sb.addObjectListElement(new PromptSerializer(prompt));
    }
    
    sb.endList();
    sb.addStringAttribute("nextCursor", null);
    if (data.getEra() == Era.MODERN) {
      sb.addNumberAttribute("ttlMs", 300_000);
      sb.addStringAttribute("cacheScope", "public");
    }
    sb.endObject();
    sb.endObject();

    McpServerFilter.send(data.getTc(), HTTPTriggerConnection.HTTP_OK, MIME_JSON, null, sb.toString());
  }

}
