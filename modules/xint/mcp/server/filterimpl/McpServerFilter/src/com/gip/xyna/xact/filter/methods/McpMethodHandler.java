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



import java.util.Map;

import com.gip.xyna.xact.filter.McpPrimitivesData;
import com.gip.xyna.xact.filter.McpServerFilter;
import com.gip.xyna.xact.filter.McpLegacySession;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;

import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;



public interface McpMethodHandler {

  public static final String SESSIONID_HEADER = "Mcp-Session-Id";
  public static final String MIME_JSON = "application/json";


  void process(McpRequestData data);


  public static enum Era {

    LEGACY, MODERN;


    public static Era determineEra(JSONObject obj) {
      JSONValue val = McpServerFilter.getNestedValue(obj, "params", "_meta", "io.modelcontextprotocol/protocolVersion");
      if (val == null) {
        return Era.LEGACY;
      }
      return Era.MODERN;
    }
  }

  public static class McpRequestData {

    private final HTTPTriggerConnection tc;
    private final JSONObject payload;
    private final Map<String, McpLegacySession> sessions;
    private final McpLegacySession session;
    private final McpPrimitivesData primitivesData;
    private final Era era;


    public McpRequestData(HTTPTriggerConnection tc, JSONObject payload, Map<String, McpLegacySession> sessions, McpLegacySession session,
                          McpPrimitivesData primitivesData, Era era) {
      this.tc = tc;
      this.payload = payload;
      this.sessions = sessions;
      this.session = session;
      this.primitivesData = primitivesData;
      this.era = era;
    }


    public HTTPTriggerConnection getTc() {
      return tc;
    }


    public JSONObject getPayload() {
      return payload;
    }


    public Map<String, McpLegacySession> getSessions() {
      return sessions;
    }


    public McpLegacySession getSession() {
      return session;
    }


    public McpPrimitivesData getPrimitivesData() {
      return primitivesData;
    }


    public Era getEra() {
      return era;
    }
  }
}
