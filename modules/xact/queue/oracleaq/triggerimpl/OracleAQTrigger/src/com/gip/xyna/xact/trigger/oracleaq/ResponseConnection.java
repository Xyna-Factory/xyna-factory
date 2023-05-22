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

import java.io.Serializable;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xact.trigger.OracleAQTrigger;
import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_EnqueueResponseException;
import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_ResponseQueueNotConfiguredException;
import com.gip.xyna.xact.trigger.oracleaq.shared.EnqueueableOracleAQMessage;
import com.gip.xyna.xact.trigger.oracleaq.shared.QueueData;
import com.gip.xyna.xact.trigger.oracleaq.shared.SQLUtilsCreator.SQLLogger;
import com.gip.xyna.xact.trigger.oracleaq.shared.SQLUtilsCreator.SQLRuntimeException;


/**
 * contains a cached connection to the database schema of the response queue;
 * the connection will be kept open,
 * and automatically re-opened (if possible) after errors
 *
 * @deprecated wird allerdings noch verwendet in Filtern:
 * ResponseMessage rm = tc.buildResponseMessage(correlationId, response);
 * tc.getResponseConnection().enqueue(rm);
 * sollte abgel�st werden durch tc.sendResponse( correlationId, response );
 */
public class ResponseConnection implements Serializable {

  private static final long serialVersionUID = 1L;

  private static Logger _logger = CentralFactoryLogging.getLogger(ResponseConnection.class);

  private QueueData queueData;
  private transient SQLUtils _sqlUtils = null;

  private transient boolean _isConnOpen = false;
  private boolean _neverOpened = true;

  // used for synchronization for single instance
  private transient Boolean _lock = true;

  private SQLLogger sqlLogger;

  private transient OracleAQTrigger trigger;
  
  public ResponseConnection(QueueData queueData, SQLLogger sqlLogger) {
    if (queueData == null) {
      throw new IllegalArgumentException("queueData must not be null");
    }
    if (sqlLogger == null) {
      throw new IllegalArgumentException("sqlLogger must not be null");
    }
    this.queueData = queueData;
    this.sqlLogger = sqlLogger;
  }


  /**
   * @param trigger
   */
  public ResponseConnection(OracleAQTrigger trigger) {
    this.trigger = trigger;
  }


  private void open() {
    _neverOpened = false;
    _sqlUtils = queueData.getDBConnectionData(null).createSQLUtils(sqlLogger);
    if( _sqlUtils == null ) {
      throw new SQLRuntimeException(new Exception("sqlUtils is null") );
    }
    _isConnOpen = true;
  }


  public void closeConnection() {
    _logger.info("Going to close database connection of response connection, schema = " +
                 queueData.getDbSchema() + ", jdbc url = " + queueData.getJdbcUrl());
    synchronized (_lock) {
      close();
    }
  }


  private void close() {
    _isConnOpen = false;
    try {
      if(_sqlUtils != null) {
        // wenn sqlUtils null, ist connection eh verloren ...
        getSQLUtilsReopened().closeConnection();
      }
    }
    catch (Exception e) {
      _logger.debug("Error trying to close connection", e);
    }
  }


  public void enqueue(EnqueueableOracleAQMessage msg)
         throws ORACLEAQTRIGGER_EnqueueResponseException, ORACLEAQTRIGGER_ResponseQueueNotConfiguredException {
    if( trigger != null ) {
      trigger.sendResponse(msg);
    } else {
    
    synchronized (_lock) {
      try {
        if (!_isConnOpen) {
          open();
        }
        enqueueImpl(msg);
      }
      catch (Exception e) {
        _logger.error("Error trying to enqueue response message.", e);
        throw new ORACLEAQTRIGGER_EnqueueResponseException(queueData.getQueueName(), queueData.getDbSchema(),
                                                           queueData.getJdbcUrl(), e);
      }
    }
    }
  }


  private void enqueueImpl(EnqueueableOracleAQMessage msg) {
    SQLUtils sqlUtils = getSQLUtilsReopened();
    msg.sendToQueue(queueData, sqlUtils );
  }


  public boolean isNeverOpened() {
    return _neverOpened;
  }


  public QueueData getQueueData() {
    return queueData;
  }
  
  private SQLUtils getSQLUtilsReopened() {
    if(_sqlUtils == null) {
      synchronized (_lock) {
        open();
      }
    }
    return _sqlUtils;
  }


}
