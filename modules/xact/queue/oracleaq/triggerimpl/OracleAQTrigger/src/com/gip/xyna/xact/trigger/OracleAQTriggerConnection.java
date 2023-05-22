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

package com.gip.xyna.xact.trigger;


import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xact.trigger.oracleaq.ResponseConnection;
import com.gip.xyna.xact.trigger.oracleaq.ResponseMessage;
import com.gip.xyna.xact.trigger.oracleaq.ResponseMessageCachedConnection;
import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_EnqueueResponseException;
import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_ResponseQueueNotConfiguredException;
import com.gip.xyna.xact.trigger.oracleaq.shared.OracleAQMessage;
import com.gip.xyna.xact.trigger.oracleaq.shared.QueueData;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;


public class OracleAQTriggerConnection extends TriggerConnection {

  private static final long serialVersionUID = -7920313265380831750L;
  
  private OracleAQMessage msg = null;
  private QueueData responseQueue = null;
  private long dequeueTime = 0L;
  protected transient SQLUtils sqlUtils; //aktuelle Transaktion
  private ResponseConnection responseConnection = null;
  /**
   * reference to responseConnection of trigger, so that the connection can
   * be closed by the trigger when the trigger is stopped
   */
  /*
   Fr�her wurde responseQueue im Konstruktor gesetzt, responseConnection in einem Teil der Konstruktoren.
   Dies ist nun nicht mehr n�tig, die beiden Variablen werden immer null bleiben.
   Die ben�tigten Information k�nnen nach Deserialisierung wieder aus dem Trigger entnommen werden,
   der nun neu in der TriggerConnection wiederhergestellt wird.
  
   Einziger Zweck von responseConnection und responseQueue ist nun noch die Abw�rtskompatibilit�t: 
   Bereits serialisierte OracleAQTriggerConnections finden ihren Trigger nicht!
   */
  
  /* (non-Javadoc)
   * @see com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection#getTrigger()
   */
  @Override
  public OracleAQTrigger getTrigger() {
    Object el = super.getTrigger();
    if( el instanceof OracleAQTrigger ) {
      return (OracleAQTrigger)el;
    } else if( el == null ){
      return null;
    } else {
      throw new IllegalStateException("EventListener is no OracleAQTrigger but "+el.getClass() );
    }
  }
  
  public OracleAQTriggerConnection(OracleAQMessage msg, SQLUtils sqlUtils ) {
    this.msg = msg;
    this.sqlUtils = sqlUtils;
  }

  public OracleAQMessage getMessage() {
    return msg;
  }

  /**
   * @param sqlUtils the sqlUtils to set
   */
  public void setSqlUtils(SQLUtils sqlUtils) {
    this.sqlUtils = sqlUtils;
  }
 
  /**
   * @return the sqlUtils
   */
  public SQLUtils getSqlUtils() {
    return sqlUtils;
  }
  
  
  /**
   * builds message with given correlationID and text,
   * that internally has references to the access data of the response queue configured in the
   * triggerConnection;
   * that means the message can be sent to that queue by calling the sentToResponseQueue() method
   * of the ResponseMessage object;
   * additional properties (delay, expiration, priority) can be also set in that object
   * @deprecated use tc.sendResponse( correlationId, response );
   */
  public ResponseMessage buildResponseMessage(String corrID, String text)
                         throws ORACLEAQTRIGGER_ResponseQueueNotConfiguredException {
    
    OracleAQTrigger trigger = getTrigger();
    QueueData respQueue = trigger!=null ? trigger.getResponseQueue() : responseQueue; 
    if (respQueue == null) {
      throw new ORACLEAQTRIGGER_ResponseQueueNotConfiguredException();
    }
    return new ResponseMessage(new OracleAQMessage(corrID, text), respQueue);
  }


  /**
   * builds response message with correlation id = null
   * @deprecated use tc.sendResponse( correlationId, response );
   */
  public ResponseMessage buildResponseMessage(String text)
                         throws ORACLEAQTRIGGER_ResponseQueueNotConfiguredException {
    return buildResponseMessage(null, text);
  }



  /**
   * builds message with given correlationID and text,
   * that internally has references to a cached connection to the response queue configured in the
   * triggerConnection;
   * that means the message can be sent to that queue by calling the sentToResponseQueue() method
   * of the ResponseMessageCachedConnection object;
   * additional properties (delay, expiration, priority) can be also set in that object
   * @deprecated use tc.sendResponse( correlationId, response );
   */
  public ResponseMessageCachedConnection buildResponseMessageCachedConnection(String corrID, String text)
                         throws ORACLEAQTRIGGER_ResponseQueueNotConfiguredException {
    if (responseQueue == null) {
      throw new ORACLEAQTRIGGER_ResponseQueueNotConfiguredException();
    }
    if (responseConnection == null) {
      throw new ORACLEAQTRIGGER_ResponseQueueNotConfiguredException();
    }
    return new ResponseMessageCachedConnection(new OracleAQMessage(corrID, text), responseConnection);
  }


  /**
   * @return
   * @deprecated
   */
  public QueueData getResponseQueueData() {
    return responseQueue;
  }


  /**
   * true, if configuration data for the response queue was provided;
   * otherwise false (in that case, it is not possible to send messages to a
   * response queue)
   * @deprecated
   */
  public boolean isResponseQueueConfigured() {
    return (responseQueue != null);
  }


  /**
   * returns cached connection to response queue;
   * closing that connection will close it also for the trigger and all other trigger connections!
   * @deprecated use tc.sendResponse( correlationId, response );
   */
  public ResponseConnection getResponseConnection() {
    OracleAQTrigger trigger = getTrigger();
    if( trigger != null ) {
      return new ResponseConnection(trigger);
    } else {
      return responseConnection;
    }
  }

  
  /**
   * Sets the dequeue time stamp.
   * @param t   currentTimeMills of dequeuing
   */
  public void setDequeueTime( long t ) {
    this.dequeueTime = t;
  }
  
  
  /**
   * Returns the time the message was dequeued.
   * @return  currentTimeMills of dequeuing
   */
  public long getDequeueTime() {
    return dequeueTime;
  }

  @Override
  public synchronized void close() {
    commitOpenedSQLUtilsAndClose();
    super.close();
  }
  
  @Override
  public void rollback() {
    if (sqlUtils != null) {
      sqlUtils.rollback();
    }
  }

  /**
   * 
   */
  public void commit() {
    if (sqlUtils != null) {
      sqlUtils.commit();
    }
  }
  


  protected synchronized void commitOpenedSQLUtilsAndClose() {
    if (sqlUtils != null) {
      try {
        sqlUtils.commit();
      } finally {
        closeConnection();
      }
    }
  }


  protected synchronized void closeConnection() {
    sqlUtils.closeConnection();
    sqlUtils = null;
  }
  
  /**
   * @param correlationId
   * @param message
   * @throws ORACLEAQTRIGGER_ResponseQueueNotConfiguredException
   * @throws ORACLEAQTRIGGER_EnqueueResponseException
   */
  public void sendResponse(String correlationId, String message) throws ORACLEAQTRIGGER_ResponseQueueNotConfiguredException, ORACLEAQTRIGGER_EnqueueResponseException {
    sendResponse(message, correlationId, null);    
  }

  /**
   * @param exceptionsToString
   * @param correlationId
   * @throws ORACLEAQTRIGGER_ResponseQueueNotConfiguredException 
   * @throws ORACLEAQTRIGGER_EnqueueResponseException 
   */
  public void sendResponse(String message, String correlationId, Integer priority) throws ORACLEAQTRIGGER_ResponseQueueNotConfiguredException, ORACLEAQTRIGGER_EnqueueResponseException {
    OracleAQTrigger trigger = getTrigger();
    if( trigger == null ) {
      ResponseMessage rm = buildResponseMessage(correlationId, message);
      getResponseConnection().enqueue(rm);
    } else {
      trigger.sendResponse( new OracleAQMessage(correlationId, message, priority) );
    }
  }

  /**
   * @return
   */
  public boolean isResponseConfigured() {
    OracleAQTrigger trigger = getTrigger();
    if( trigger == null ) {
      return responseQueue != null; //Abw�rtskompatinbilit�t!
    } else {
      return trigger.isResponseConfigured();
    }
  }

   
  
}
