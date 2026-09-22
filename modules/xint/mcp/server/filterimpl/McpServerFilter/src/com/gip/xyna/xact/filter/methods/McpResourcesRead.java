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
import com.gip.xyna.xact.filter.serialization.ResourceContentSerializer;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;

import xint.mcp.schema.ReadResourceRequestParams;
import xint.mcp.schema.ReadResourceResult;
import xint.mcp.schema.Resource;
import xint.mcp.schema.ResourceContent;



public class McpResourcesRead implements McpMethodHandler {

  @Override
  public void process(McpRequestData data) {
    JsonBuilder jb = new JsonBuilder();
    String uri = data.getPayload().getMember("params").getObjectValue().getMember("uri").getStringOrNumberValue();
    McpPrimitivesData primitives = data.getPrimitivesData();

    List<Resource> resources = primitives.getResources();
    ReadResourceRequestParams request = null;
    ReadResourceResult result = null;
    for (Resource resource : resources) {
      if (Objects.equals(uri, resource.getUri())) {
        result = resource.read(request);
        break;
      }
    }

    if (result == null) {
      ErrorMessages.sendPrimitiveNotFound(data.getTc(), data.getPayload().getMember("id"), "resource", uri);
      return;
    }

    jb.startObject();
    jb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(jb, data.getPayload().getMember("id"));
    jb.addObjectAttribute("result");
    if (data.getEra() == Era.MODERN) {
      jb.addStringAttribute("responseType", "complete");
    }
    jb.addListAttribute("contents");
    if (result.getContents() != null) {
      for (ResourceContent content : result.getContents()) {
        jb.addObjectListElement(new ResourceContentSerializer(content));
      }
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
