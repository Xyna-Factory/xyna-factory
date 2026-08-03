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



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xact.filter.McpServerFilter;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;



public class McpServerDiscover implements McpMethodHandler {


  private final String version;

  private static Logger logger = CentralFactoryLogging.getLogger(McpServerDiscover.class);


  public McpServerDiscover() {
    version = McpServerFilter.getRtcVersion(getClass().getClassLoader());
  }


  @Override
  public void process(McpRequestData data) {
    HTTPTriggerConnection tc = data.getTc();
    JsonBuilder jb = new JsonBuilder();


    if (data.getEra() == Era.LEGACY) {
      if (logger.isDebugEnabled()) {
        logger.debug("[Legacy] mcp-client called [Modern] server/discovery method " + data.getPayload());
      }
    }

    jb.startObject();
    jb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(jb, data.getPayload().getMember("id"));
    jb.addObjectAttribute("result");
    jb.addStringAttribute("resultType", "complete");
    jb.addStringListAttribute("supportedVersions", McpServerFilter.SUPPORTED_VERSIONS);
    jb.addObjectAttribute("capabilities");

    jb.addObjectAttribute("tools");
    jb.endObject();

    jb.addObjectAttribute("resources");
    jb.endObject();

    jb.addObjectAttribute("prompts");
    jb.endObject();

    jb.endObject();

    jb.addNumberAttribute("ttlMs", 3_600_000);
    jb.addStringAttribute("cacheScope", "public");

    jb.addObjectAttribute("_meta");
    jb.addObjectAttribute("io.modelcontextprotocol/serverInfo");
    jb.addStringAttribute("name", "XynaMcpServer");
    jb.addStringAttribute("version", version);
    jb.endObject();
    jb.endObject();

    jb.endObject();
    jb.endObject();

    McpServerFilter.send(tc, HTTPTriggerConnection.HTTP_OK, McpMethodHandler.MIME_JSON, null, jb.toString());
  }

}
