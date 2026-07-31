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



import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xact.filter.McpServerFilter;
import com.gip.xyna.xact.filter.McpLegacySession;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import xfmg.xfctrl.datamodel.json.JSONObject;



public class McpInitialize implements McpMethodHandler {

  private static Logger logger = CentralFactoryLogging.getLogger(McpInitialize.class);

  private final String version;
  
  public McpInitialize() {
    version = McpServerFilter.getRtcVersion(getClass().getClassLoader());
  }

  @Override
  public void process(McpRequestData data) {
    JSONObject obj = data.getPayload();
    HTTPTriggerConnection tc = data.getTc();
    JSONObject clientInfo = obj.getMember("params").getObjectValue().getMember("clientInfo").getObjectValue();
    String clientInfoName = clientInfo.getMember("name").getStringOrNumberValue();
    String clientInfoVersion = clientInfo.getMember("version").getStringOrNumberValue();
    JsonBuilder sb = new JsonBuilder();
    sb.startObject();
    sb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(sb, obj.getMember("id"));
    sb.addObjectAttribute("result");
    sb.addStringAttribute("protocolVersion", "2025-03-26");
    sb.addObjectAttribute("capabilities");

    sb.addObjectAttribute("prompts");
    sb.endObject();

    sb.addObjectAttribute("resources");
    sb.endObject();
    sb.addObjectAttribute("tools");
    sb.endObject();

    sb.endObject();
    
    sb.addObjectAttribute("serverInfo");
    sb.addStringAttribute("name", "XynaMcpServer");
    sb.addStringAttribute("version", version);
    sb.endObject();
    
    //sb.addStringAttribute("instructions", "");
    sb.endObject();
    sb.endObject();

    String sessionId = UUID.randomUUID().toString();
    McpLegacySession session = new McpLegacySession(sessionId, false, clientInfoName, clientInfoVersion);

    try {
      byte[] msgBytes = sb.toString().getBytes(Charset.forName("UTF8"));
      long size = Long.valueOf(msgBytes.length);
      Properties header = new Properties();
      header.setProperty(McpMethodHandler.SESSIONID_HEADER, sessionId);
      tc.sendResponse(HTTPTriggerConnection.HTTP_OK, McpMethodHandler.MIME_JSON, header, new ByteArrayInputStream(msgBytes), size);
    } catch (SocketNotAvailableException e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not send response to client!", e);
      }
    }
    data.getSessions().put(sessionId, session);
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Started initializing Session %s for client '%s'/'%s'", sessionId, clientInfoName, clientInfoVersion));
    }
  }


}
