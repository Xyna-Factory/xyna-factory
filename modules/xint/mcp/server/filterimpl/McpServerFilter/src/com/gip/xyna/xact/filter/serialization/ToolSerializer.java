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
package com.gip.xyna.xact.filter.serialization;

import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonSerializable;

import xint.mcp.schema.Icon;
import xint.mcp.schema.Tool;
import xint.mcp.schema.ToolAnnotations;

public class ToolSerializer implements JsonSerializable {

  private final Tool tool;
  
  public ToolSerializer(Tool tool) {
    this.tool = tool;
  }
  
  @Override
  public void toJson(JsonBuilder jb) {
    jb.addStringAttribute("name", tool.getName());
    jb.addStringAttribute("title", tool.getTitel());
    jb.addStringAttribute("description", tool.getDescription());
    jb.addObjectAttribute("inputSchema", new JsonObjectSerializer(tool.getInputSchema()));
    if(tool.getOutputSchema() != null) {
      jb.addObjectAttribute("outputSchema", new JsonObjectSerializer(tool.getOutputSchema()));
    }
    if(tool.getIcons() != null && !tool.getIcons().isEmpty()) {
      jb.addListAttribute("icons");
      for(Icon icon : tool.getIcons()) {
        jb.addObjectListElement(new IconSerializer(icon));
      }
      jb.endList();
    }
    if(tool.getAnnotations() != null) {
      ToolAnnotations annotations = tool.getAnnotations();
      jb.addObjectAttribute("annotations");
      jb.addStringAttribute("title", annotations.getTitle());
      jb.addBooleanAttribute("readOnlyHint", annotations.getReadOnlyHint());
      jb.addBooleanAttribute("destructiveHint", annotations.getDestructiveHint());
      jb.addBooleanAttribute("idempotentHint", annotations.getIdempotentHint());
      jb.addBooleanAttribute("openWorldHint", annotations.getOpenWorldHint());
      jb.endObject();
    }
  }
  
}
