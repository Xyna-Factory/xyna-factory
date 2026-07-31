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
import com.gip.xyna.xact.trigger.SocketNotAvailableException;

import xfmg.xfctrl.datamodel.json.JSONObject;



public class McpPing implements McpMethodHandler {
  
  private static Logger logger = CentralFactoryLogging.getLogger(McpPing.class);

  @Override
  public void process(McpRequestData data) {
    JSONObject obj = data.getPayload();
    HTTPTriggerConnection tc = data.getTc();
    JsonBuilder sb = new JsonBuilder();
    sb.startObject();
    sb.addStringAttribute("jsonrpc", "2.0");
    McpServerFilter.addIdToBuilder(sb, obj.getMember("id"));
    sb.addObjectAttribute("result");
    sb.endObject();
    sb.endObject();
    try {
      tc.sendResponse(sb.toString());
    } catch (SocketNotAvailableException e) {
      if(logger.isWarnEnabled()) {
        logger.warn("Could not send response to client!", e);
      }
    }
  }

}
