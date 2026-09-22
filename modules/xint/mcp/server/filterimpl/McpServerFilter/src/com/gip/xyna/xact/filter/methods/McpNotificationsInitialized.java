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
import com.gip.xyna.xact.filter.McpServerFilter;
import com.gip.xyna.xact.filter.McpLegacySession;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;



public class McpNotificationsInitialized implements McpMethodHandler {

  private static Logger logger = CentralFactoryLogging.getLogger(McpNotificationsInitialized.class);


  @Override
  public void process(McpRequestData data) {
    HTTPTriggerConnection tc = data.getTc();
    McpLegacySession session = data.getSession();

    try {
      McpServerFilter.send(tc, "202 Accepted", HTTPTriggerConnection.MIME_PLAINTEXT, null, "");
    } catch (Exception e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not send response to client!", e);
      }
    }

    session.setInitialized(true);

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Initialized Session %s", session.getSessionId()));
    }
  }

}
