/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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



import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.utils.RepeatedExceptionCheck;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;
import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_CouldNotBeStoppedException;
import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_DBConnectionCreationException;
import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_DBPackageCreationException;
import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_EnqueueResponseException;
import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_ResponseQueueNotConfiguredException;
import com.gip.xyna.xact.trigger.oracleaq.shared.OracleAQMessage;
import com.gip.xyna.xact.trigger.oracleaq.shared.PLSQLExecUtils;
import com.gip.xyna.xact.trigger.oracleaq.shared.QueueData;
import com.gip.xyna.xact.trigger.oracleaq.shared.SQLUtilsCreator;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyLong;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;



/**
 * trigger for messages sent to an oracle AQ database queue
 */
public class OracleAQTrigger extends EventListener<OracleAQTriggerConnection, OracleAQStartParameter> {

  private static Logger logger = CentralFactoryLogging.getLogger(OracleAQTrigger.class);
  
  private AtomicBoolean running = new AtomicBoolean(false);
  private OracleAQStartParameter startParameter = null;
  private RepeatedExceptionCheck receiveRepeatedExceptionCheck = new RepeatedExceptionCheck();
  private SQLUtilsCreator requestConnectionPool;
  private SQLUtilsCreator responseConnectionPool;
  private OracleAQTriggerStatistics statistics;
  
  private static final XynaPropertyLong SHUTDOWN_TIMEOUT = 
      new XynaPropertyLong("xyna.aqtrigger.poolshutdown.timeout", 10L);

  
  
  public String getClassDescription() {
    return "trigger for messages sent to an oracle AQ database queue";
  }

  @Override
  protected void onNoFilterFound(OracleAQTriggerConnection tc) {
    if( logger.isDebugEnabled() ) {
      logger.debug("No filter found for "+tc.getMessage());
    }
    tc.rollback();
  } 

  @Override
  protected void onProcessingRejected(String cause, OracleAQTriggerConnection tc) {
    if( logger.isDebugEnabled() ) {
      logger.debug("Rejected due to \""+cause+"\": "+tc.getMessage() );
    }
    statistics.rejectHappened();
    tc.rollback();
  }
 
  @Override
  protected void onFilterFound(XynaOrderServerExtension xo, OrderContextServerExtension ctx, OracleAQTriggerConnection tc) {
    if( logger.isTraceEnabled() ) {
      logger.trace("onFilterFound: "+tc.getMessage() );
    }
    //Behandlung des Dequeue-Commits:
    if (startParameter.isTransactionSafe()) {
      //Umtragen der DB-Connection, auf der das Dequeue-Commit erfolgen soll ins AcknowledgeableObject
      OracleAQTriggerAcknowledgeableObject ack = new OracleAQTriggerAcknowledgeableObject(tc,tc.getSqlUtils());
      ctx.set(OrderContextServerExtension.ACKNOWLEDGABLE_OBJECT_KEY, ack );
      tc.setSqlUtils(null); //werden von nun an nicht mehr in der OracleAQTriggerConnection benötigt
    } else {
      //Dequeue-Commit kann hier erfolgen, Rückgabe der Connection an den Pool
      try {
        tc.commit();
      } finally {
        tc.closeConnection();
      }
    }
    super.onFilterFound(xo, ctx, tc); // currently only dummy implementation, let's call it anyway
  }

  /**
   * ermöglicht es dem Trigger zu reagieren, wenn Filter verantwortlich ist, aber keine XynaOrder startet
   * nicht abstract aufgrund von Abwärtskompatibilität
   * @param tc
   */
  @Override
  protected void onFilterFoundWithoutXynaOrder(OracleAQTriggerConnection tc) {
    if( logger.isTraceEnabled() ) {
      logger.trace("onFilterFoundWithoutXynaOrder: "+tc.getMessage() );
    }
    tc.commit();
  }

  /**
   * ermöglicht es dem Trigger zu reagieren, wenn Filter mit unerwarteten Exceptions fehlschlägt
   * nicht abstract aufgrund von Abwärtskompatibilität
   * @param tc
   * @param connectionFilter
   * @param cause
   */
  @Override
  protected void onFilterFailed(OracleAQTriggerConnection tc, ConnectionFilter<?> connectionFilter, Throwable cause) {
    if( logger.isDebugEnabled() ) {
      logger.debug("onFilterFailed "+connectionFilter.getClassDescription()+" failed for "+tc.getMessage()+" with following exception", cause );
    }
    tc.rollback();
  }
  

  @Override
  protected OracleAQTriggerConnection receive() {
    while (running.get()) {
      try {
        OracleAQTriggerConnection ret = receiveInternal();
        if( ret != null ) {
          return ret;
        }
      } catch (Exception e) {
        handleReceiveException(e);
      }
    }
    return null;
  }


  private OracleAQTriggerConnection receiveInternal() {
    SQLUtils sqlUtils = requestConnectionPool.getSQLUtils("dequeue", logger, Level.TRACE);
    
    boolean rollbackAndCloseSQLUtils = true;
    try {
      OracleAQMessage msg = PLSQLExecUtils.dequeue(sqlUtils,
                                                   startParameter.getDequeueSQL(),
                                                   startParameter.getDequeueOptions(), 
                                                   true, 
                                                   startParameter.isUsePLSQLPackage());      
      if( msg != null ) {
        OracleAQTriggerConnection tc = generateTriggerConnection(msg, sqlUtils);
        rollbackAndCloseSQLUtils = false;
        return tc;
      } else {
        return null;
      }
    } finally {
      if( rollbackAndCloseSQLUtils ) {
        finallyClose(sqlUtils, true);
      }
    }
  }


  /**
   * @param sqlUtils
   */
  private void finallyClose(SQLUtils sqlUtils, boolean rollback) {
    if( sqlUtils == null ) {
      return; //nichts zu tun
    }
    try {
      if( rollback ) {
        sqlUtils.rollback();
      }
    } catch( Exception e ) {
      logger.warn("Could not rollback sqlUtils", e);
    } finally {
      try {
        sqlUtils.closeConnection(); //FIXME ConnectionPool sollte Connection checken!
      } catch( Exception e ) {
        logger.warn("Could not close sqlUtils", e);
      } 
    }
  }

  private OracleAQTriggerConnection generateTriggerConnection(OracleAQMessage msg, SQLUtils sqlUtils) {
    OracleAQTriggerConnection tc = new OracleAQTriggerConnection(msg, sqlUtils);
    long now = System.currentTimeMillis();
    statistics.dequeueHappened(now);
    tc.setDequeueTime(now);
    return tc;
  }


  private void handleReceiveException(Exception exception) {
    boolean isRepeated = receiveRepeatedExceptionCheck.checkRepeated(exception);
    
    if( ! isRepeated ) {
      //Exception trat zum ersten Mal auf
      if ( running.get() ) {
        logger.error("Exception while dequeueing.", exception );
        return; //nach dem ersten Fehler direkt nochmal probieren...
      } else {
        logger.info("Dequeue failed but AQ trigger instance has already been stopped."
            + " Exception is probably due to stop.", exception);
        return; //Receive wird gestoppt werden
      }
    } else {
      //wiederholte Exception, daher AutoReconnect auswerten
      logger.error("Exception while dequeueing. " + receiveRepeatedExceptionCheck );
      
      if (startParameter.isAutoReconnect()) {
        //Etwas warten und dann erneut probieren
        try {
          Thread.sleep(startParameter.getAutoReconnectRetryInterval());
        } catch (InterruptedException e1) {
          //dann halt kürzer warten
        }
      } else {
        disableTrigger();
      }
    }
  }

  private void disableTrigger() {
    logger.error("This instance as been configured not to autoreconnect if a connection loss occurs."
        + " The instance is now being disabled!");
    try {
      XynaFactory.getInstance().getActivation().getActivationTrigger().disableTriggerInstance(this);
    } catch (Exception ex) {
      logger.warn("Failed to disable trigger instance after exception. Trying to stop anyway...", ex);
      try {
        stop();
      } catch (XACT_TriggerCouldNotBeStoppedException e1) {
        logger.warn("Failed to stop trigger");
      }
    }
  }

  /**
   * @return
   */
  public boolean isResponseConfigured() {
    return responseConnectionPool != null;
  }


  /**
   * @param message
   * @param correlationId
   * @param prio
   * @throws ORACLEAQTRIGGER_EnqueueResponseException 
   * @throws ORACLEAQTRIGGER_ResponseQueueNotConfiguredException 
   */
  public void sendResponse(OracleAQMessage message) throws ORACLEAQTRIGGER_EnqueueResponseException, ORACLEAQTRIGGER_ResponseQueueNotConfiguredException {
    if( startParameter.getResponseQueue() == null || responseConnectionPool == null ) {
      throw new ORACLEAQTRIGGER_ResponseQueueNotConfiguredException();
    }
    
    SQLUtils sqlUtils = responseConnectionPool.getSQLUtils("enqueue", logger, Level.TRACE);
    boolean rollbackSQLUtils = true;
    try {
      
      PLSQLExecUtils.enqueue(sqlUtils, 
                             startParameter.getEnqueueSQL(),
                             message,
                             startParameter.getEnqueueOptions(), 
                             startParameter.isUsePLSQLPackage());      
      sqlUtils.commit();
      rollbackSQLUtils = false;
    } catch( Exception e ) {
      throw new ORACLEAQTRIGGER_EnqueueResponseException(startParameter.getResponseQueue().getQueueName(), 
                                                         responseConnectionPool.getSchema(), responseConnectionPool.getJdbcUrl(), e);
    } finally {
      finallyClose(sqlUtils,rollbackSQLUtils);
    }
  }

  


  public void start(OracleAQStartParameter input) throws XACT_TriggerCouldNotBeStartedException {
    startParameter = input;
    String instanceName = getTriggerInstanceIdentification().getInstanceName();
    
    
    statistics = new OracleAQTriggerStatistics(instanceName, startParameter.getRequestQueue().getQueueName());
    
    boolean success = false;
    try {
      String conPoolId = OracleAQTrigger.class.getSimpleName() + "_" + instanceName;
      requestConnectionPool = createPool(conPoolId, true );
      statistics.setRequestConnectionPool(requestConnectionPool);

      if (startParameter.isExplicitResponseQueue() ) {
        responseConnectionPool = createPool(conPoolId+"_response", false );
        if( responseConnectionPool != null ) {
          statistics.setResponseConnectionPool(responseConnectionPool);
        }
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Trigger class loader: " + OracleAQTriggerConnection.class.getClassLoader().toString());
      }

      if (startParameter.isUsePLSQLPackage()) {
        checkPackage();
      }

      //addStatistics
      statistics.register(this);
      running.set(true);
      
      success = true;
    } finally {
      if( !success ) {
        running.set(false);
        if( requestConnectionPool != null ) {
          requestConnectionPool.close();
        }
        if( responseConnectionPool != null ) {
          responseConnectionPool.close();
        }
      }
    }
    
  }
  
  private SQLUtilsCreator createPool(String conPoolId, boolean request) throws ORACLEAQTRIGGER_DBConnectionCreationException {
    QueueData queueData = request ? startParameter.getRequestQueue() : startParameter.getResponseQueue();
    int poolSize = request ? startParameter.getConnectionPoolSize() : startParameter.getResponseConnectionPoolSize();
    if( poolSize == 0 ) {
      return null; //Pool wird nicht gebraucht
    }
    try {
      return new SQLUtilsCreator(conPoolId, queueData, poolSize, null, SHUTDOWN_TIMEOUT.get() );
    } catch( NoConnectionAvailableException e ) {
      throw new ORACLEAQTRIGGER_DBConnectionCreationException(queueData.getDbSchema(), queueData.getJdbcUrl(), e );
    }
  }

  /**
   * @throws ORACLEAQTRIGGER_DBPackageCreationException 
   * 
   */
  private void checkPackage() throws ORACLEAQTRIGGER_DBPackageCreationException {
    try {
      SQLUtils sqlUtils = requestConnectionPool.getSQLUtils("createPackage", logger, Level.DEBUG);
      try {
        if( ! PLSQLExecUtils.existsPackage(sqlUtils, startParameter.getPackageName() )) {
          PLSQLExecUtils.createPackage(sqlUtils, 
                                       startParameter.getPackageName(),
                                       startParameter.getDequeueOptions(),
                                       startParameter.getEnqueueOptions()
                                       );
        }
      } finally {
        finallyClose(sqlUtils, true);
      }

    } catch (Exception e) {
      logger.error("Error starting trigger", e);
      throw new ORACLEAQTRIGGER_DBPackageCreationException(startParameter.getRequestQueue().getDbSchema(),
                                                           startParameter.getRequestQueue().getJdbcUrl(), e);
    }
  }

  public void stop() throws XACT_TriggerCouldNotBeStoppedException {
    if (running.compareAndSet(true, false)) {
      QueueData queueData = startParameter.getRequestQueue();
      try {
        if (logger.isInfoEnabled()) {
          logger.info("Going to close database connection for schema = " + queueData.getDbSchema()
                      + ", jdbc url = " + queueData.getJdbcUrl());
        }
        //Pools schließen
        requestConnectionPool.close();
        if( responseConnectionPool != null ) {
          responseConnectionPool.close();
        }
        
        //removeStatistics
        statistics.unregister(this);
      } catch (Exception e) {
        logger.error("Error closing database connection", e);

        throw new ORACLEAQTRIGGER_CouldNotBeStoppedException(queueData.getQueueName(), queueData.getDbSchema(),
                                                             queueData.getJdbcUrl(), e);
      }
    }
  }


  /**
   * @return
   */
  public QueueData getResponseQueue() {
    return startParameter.getResponseQueue();
  }

}
