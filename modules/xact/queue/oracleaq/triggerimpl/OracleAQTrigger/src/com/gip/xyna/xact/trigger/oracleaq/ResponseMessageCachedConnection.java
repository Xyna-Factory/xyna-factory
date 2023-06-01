/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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


package com.gip.xyna.xact.trigger.oracleaq;

import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_EnqueueResponseException;
import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_ResponseQueueNotConfiguredException;
import com.gip.xyna.xact.trigger.oracleaq.shared.EnqueueableOracleAQMessage;
import com.gip.xyna.xact.trigger.oracleaq.shared.OracleAQMessage;


/**
 * Message intended to be sent to the response queue of the trigger;
 * when the sendToResponseQueue() method is called, a cached database connection will
 * be used that is kept open;
 * that database connection is provided by the ResponseConnection parameter of the constructor
 *
 * @deprecated wird allerdings noch verwendet in Filtern:
 * ResponseMessage rm = tc.buildResponseMessage(correlationId, response);
 * tc.getResponseConnection().enqueue(rm);
 * sollte abgelöst werden durch tc.sendResponse( correlationId, response );
 */
public class ResponseMessageCachedConnection extends EnqueueableOracleAQMessage {

  protected ResponseConnection responseConnection = null;

  public ResponseMessageCachedConnection(OracleAQMessage msg, ResponseConnection respConn) {
    super(msg);
    responseConnection = respConn;
  }

  public ResponseMessageCachedConnection(String corrID, String text, Integer priority,
                                         ResponseConnection respConn) {
    super(corrID, text, priority);
    responseConnection = respConn;
  }


  public void sendToResponseQueue() throws ORACLEAQTRIGGER_EnqueueResponseException,
                                           ORACLEAQTRIGGER_ResponseQueueNotConfiguredException {
    responseConnection.enqueue(this);
  }

}
