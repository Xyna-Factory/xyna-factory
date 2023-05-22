/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_EnqueueResponseException;
import com.gip.xyna.xact.trigger.oracleaq.shared.EnqueueableOracleAQMessage;
import com.gip.xyna.xact.trigger.oracleaq.shared.OracleAQMessage;
import com.gip.xyna.xact.trigger.oracleaq.shared.QueueData;
import com.gip.xyna.xact.trigger.oracleaq.shared.SQLUtilsCreator.SQLLogger;


/**
 * Message intended to be sent to the response queue of the trigger;
 * when the sendToResponseQueue() method is called, a new database connection
 * will be opened and closed just for that enqueue operation
 *
 * @deprecated wird allerdings noch verwendet in Filtern:
 * ResponseMessage rm = tc.buildResponseMessage(correlationId, response);
 * tc.getResponseConnection().enqueue(rm);
 * sollte abgel�st werden durch tc.sendResponse( correlationId, response );
 */
public class ResponseMessage extends EnqueueableOracleAQMessage {

  private static final long serialVersionUID = 1L;

  private static Logger _logger = CentralFactoryLogging.getLogger(ResponseMessage.class);

  protected QueueData queueData = null;

  public ResponseMessage(OracleAQMessage msg, QueueData qdata) {
    super(msg);
    this.queueData = qdata;
  }

  public ResponseMessage(String corrID, String text, Integer priority, QueueData qdata) {
    super(corrID, text, priority);
    this.queueData = qdata;
  }

  public void sendToResponseQueue() throws ORACLEAQTRIGGER_EnqueueResponseException {
    SQLUtils sqlUtils = null;
    try {
      sqlUtils = queueData.getDBConnectionData(null).createSQLUtils( new SQLLogger(_logger) );
      sendToQueue(queueData, sqlUtils);
    } catch( Exception e ) {
      throw new ORACLEAQTRIGGER_EnqueueResponseException(queueData.getQueueName(), queueData.getDbSchema(),
                                                         queueData.getJdbcUrl(), e);
    } finally {
      try {
        sqlUtils.closeConnection();
        _logger.info("Closed database connection for schema " + queueData.getDbSchema() +
                     ", jdbc url = " + queueData.getJdbcUrl());
      }
      catch (Exception e) {
        _logger.debug("", e);
      }
    }
  }

}
