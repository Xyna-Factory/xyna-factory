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
import com.gip.xyna.xact.filter.serialization.ToolSerializer;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;

import xint.mcp.schema.Tool;



public class McpToolsList implements McpMethodHandler {

  @Override
  public void process(McpRequestData data) {
    JsonBuilder jb = new JsonBuilder();
    McpPrimitivesData primitives = data.getPrimitivesData();
    jb.startObject();
    jb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(jb, data.getPayload().getMember("id"));
    jb.addObjectAttribute("result");
    if (data.getEra() == Era.MODERN) {
      jb.addStringAttribute("resultType", "complete");
    }
    jb.addListAttribute("tools");

    for (Tool tool : primitives.getTools()) {
      jb.addObjectListElement(new ToolSerializer(tool, data.getEra()));
    }

    jb.endList();
    if (data.getEra() == Era.MODERN) {
      jb.addNumberAttribute("ttlMs", 300_000);
      jb.addStringAttribute("cacheScope", "public");
    }
    jb.endObject();
    jb.endObject();

    McpServerFilter.send(data.getTc(), HTTPTriggerConnection.HTTP_OK, MIME_JSON, null, jb.toString());
  }
}
