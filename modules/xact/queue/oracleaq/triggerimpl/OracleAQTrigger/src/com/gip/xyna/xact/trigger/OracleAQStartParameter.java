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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xact.trigger.oracleaq.exception.ORACLEAQTRIGGER_WrongQueueTypeException;
import com.gip.xyna.xact.trigger.oracleaq.shared.DequeueOptions;
import com.gip.xyna.xact.trigger.oracleaq.shared.EnqueueOptions;
import com.gip.xyna.xact.trigger.oracleaq.shared.PLSQLBuilder;
import com.gip.xyna.xact.trigger.oracleaq.shared.QueueData;
import com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.OracleAQConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueManagement;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


public class OracleAQStartParameter extends EnhancedStartParameter {

  private static Logger logger = CentralFactoryLogging.getLogger(OracleAQStartParameter.class);
  
  private static final Integer DEQUEUE_TIMEOUT = 10;

  private QueueData requestQueue = null;
  private QueueData responseQueue = null;
  private boolean usePLSQLPackage = false;
  private boolean autoReconnect = false;
  private boolean transactionSafe = true;
  private int connectionPoolSize = 0;
  private int responseConnectionPoolSize = 0;
  private String packageName;
  private DequeueOptions dequeueOptions;
  private EnqueueOptions enqueueOptions;
  private long autoReconnectRetryInterval;
  private String dequeueSQL;
  private String enqueueSQL;
  private boolean explicitResponseQueue;
  
  public static final StringParameter<String> QUEUE_NAME = 
      StringParameter.typeString("queueName").
      description("unique queuename from queue management (where request messages are read from)").
      mandatory().build();
  public static final StringParameter<String> DEQUEUE_CONDITION = 
      StringParameter.typeString("dequeueCondition").
      description("condition that messages must satisfy to be dequeued "+
                  "(for example: \"corrId LIKE 'myPrefix%'\")").
      build();
  public static final StringParameter<String> CONSUMER_NAME = 
      StringParameter.typeString("consumerName").
      description("consumer name for multi-consumer queues").
      build();
  public static final StringParameter<String> DEQUEUE_ADDITIONAL = 
      StringParameter.typeString("dequeueAdditional").
      description("comma separated list of additional message parameters. "+
                  "('"+PLSQLBuilder.MSG_ID+"' for the unique message id; "+
                  "'"+PLSQLBuilder.ENQUEUE_TIME+"' for the enqueue timestamp; "+
                  "any other for string properties. Example 'msgId,enqueueTime,msg_version')").
      build(); //TODO auch nicht String-Properties

  public static final StringParameter<String> RESPONSE_QUEUE_NAME = 
      StringParameter.typeString("responseQueueName").
      description("unique queuename from queue management (where response messages are sent to)").
      build();
  public static final StringParameter<String> RESPONSE_QUEUE_CONSUMER_NAME = 
      StringParameter.typeString("responseQueueConsumerName").
      description("consumer name for response queue").
      build();
  public static final StringParameter<String> ENQUEUE_ADDITIONAL = 
      StringParameter.typeString("enqueueAdditional").
      description("comma separated list of additional message parameters. "+
                  "(example 'msg_version')").
      build(); //TODO auch nicht String-Properties

  public static final StringParameter<Boolean> TRANSACTION_SAFE = 
      StringParameter.typeBoolean("transactionSafe").
      description("ensures that messages aren't removed from the queue before a triggered order is backuped").
      defaultValue(true).build();

  public static final StringParameter<Boolean> USE_PL_SQL_PACKAGE = 
      StringParameter.typeBoolean("usePlsqlPackage").
      description("should PL/SQL package be created in schema of trigger queue (will be used for dequeue and enqueue)").
      defaultValue(false).build();
  public static final StringParameter<String> PACKAGE_NAME = 
      StringParameter.typeString("packageName").
      description("name of PL/SQL package for dequeue and enqueue").
      defaultValue("XYNA_ORACLEAQ_TRIGGER").
      build();
  
  public static final StringParameter<Integer> CONNECTION_POOLSIZE = 
      StringParameter.typeInteger("connectionPoolSize").
      description("number of concurrently opened database connections for requests").
      defaultValue(2).
      build();
  public static final StringParameter<Integer> RESPONSE_CONNECTION_POOLSIZE = 
      StringParameter.typeInteger("responseConnectionPoolSize").
      description("number of concurrently opened database connections for responses").
      defaultValue(2).
      build();
  public static final StringParameter<Boolean> AUTORECONNECT = 
      StringParameter.typeBoolean("autoReconnect").
      description("should the trigger automatically retry in case of exceptions while dequeueing").
      defaultValue(true).build();
  public static final StringParameter<Integer> AUTO_RECONNECT_RETRY_INTERVAL = 
      StringParameter.typeInteger("autoReconnectRetryInterval").
      description("number of seconds between retries in case of exceptions while dequeueing").
      defaultValue(60).
      build();
  
  
  
  public static final List<StringParameter<?>> allParameters = 
      StringParameter.asList( QUEUE_NAME, DEQUEUE_CONDITION, CONSUMER_NAME, DEQUEUE_ADDITIONAL,
                              RESPONSE_QUEUE_NAME, RESPONSE_QUEUE_CONSUMER_NAME, ENQUEUE_ADDITIONAL,
                              TRANSACTION_SAFE,
                              USE_PL_SQL_PACKAGE, PACKAGE_NAME,
                              CONNECTION_POOLSIZE, RESPONSE_CONNECTION_POOLSIZE, 
                              AUTORECONNECT, AUTO_RECONNECT_RETRY_INTERVAL
                            );
  
  @Override
  public List<StringParameter<?>> getAllStringParameters() {
    return allParameters;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter#build(java.util.Map)
   */
  @Override
  public StartParameter build(Map<String, Object> paramMap) throws XACT_InvalidTriggerStartParameterValueException {
    
    OracleAQStartParameter oaqStartParameter = new OracleAQStartParameter();
    oaqStartParameter.requestQueue = oaqStartParameter.initQueueData(QUEUE_NAME.getFromMap(paramMap), QUEUE_NAME.getName(), CONSUMER_NAME.getFromMap(paramMap));
    oaqStartParameter.connectionPoolSize = CONNECTION_POOLSIZE.getFromMap(paramMap);
    oaqStartParameter.dequeueOptions = DequeueOptions.newDequeueOptions().
        queueName(oaqStartParameter.requestQueue.getQueueName()).
        dequeueCondition(DEQUEUE_CONDITION.getFromMap(paramMap)).
        timeout(DEQUEUE_TIMEOUT).
        consumerName(CONSUMER_NAME.getFromMap(paramMap)).
        additional(splitAdditional( DEQUEUE_ADDITIONAL.getFromMap(paramMap) )).
        build();
     
    oaqStartParameter.responseQueue = oaqStartParameter.initQueueData( RESPONSE_QUEUE_NAME.getFromMap(paramMap), RESPONSE_QUEUE_NAME.getName(), RESPONSE_QUEUE_CONSUMER_NAME.getFromMap(paramMap) );
    oaqStartParameter.explicitResponseQueue = true;
        if( oaqStartParameter.responseQueue == null ) {
      oaqStartParameter.responseQueue = oaqStartParameter.requestQueue;
      oaqStartParameter.explicitResponseQueue = false;
    }
    oaqStartParameter.responseConnectionPoolSize = RESPONSE_CONNECTION_POOLSIZE.getFromMap(paramMap);
    oaqStartParameter.enqueueOptions = EnqueueOptions.newEnqueueOptions().
        queueName(oaqStartParameter.responseQueue.getQueueName()).
        consumerName(CONSUMER_NAME.getFromMap(paramMap)).
        additional(splitAdditional( ENQUEUE_ADDITIONAL.getFromMap(paramMap) )).
        build();
    
    oaqStartParameter.transactionSafe = TRANSACTION_SAFE.getFromMap(paramMap);
    
    oaqStartParameter.usePLSQLPackage = USE_PL_SQL_PACKAGE.getFromMap(paramMap);
    oaqStartParameter.packageName = PACKAGE_NAME.getFromMap(paramMap);
    
    oaqStartParameter.autoReconnect = AUTORECONNECT.getFromMap(paramMap);
    oaqStartParameter.autoReconnectRetryInterval = 1000L * AUTO_RECONNECT_RETRY_INTERVAL.getFromMap(paramMap);
     
    if( oaqStartParameter.usePLSQLPackage ) {
      oaqStartParameter.dequeueSQL = PLSQLBuilder.buildDequeueWithPackage(oaqStartParameter.packageName, oaqStartParameter.dequeueOptions);
      oaqStartParameter.enqueueSQL = PLSQLBuilder.buildEnqueueWithPackage(oaqStartParameter.packageName, oaqStartParameter.enqueueOptions);
    } else {
      oaqStartParameter.dequeueSQL = PLSQLBuilder.buildDequeueBlock(oaqStartParameter.dequeueOptions);
      oaqStartParameter.enqueueSQL = PLSQLBuilder.buildEnqueueBlock(oaqStartParameter.enqueueOptions);
    }
    
    return oaqStartParameter;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter#convertToNewParameters(java.util.List)
   */
  @Override
  public List<String> convertToNewParameters(List<String> params) throws XACT_InvalidStartParameterCountException,
      XACT_InvalidTriggerStartParameterValueException {
     
    List<String> startParams = new ArrayList<String>();
    switch( params.size() ) {
      case 1:
        startParams.add( QUEUE_NAME.toNamedParameter(params.get(0) ) );
        break;
      case 2:
        startParams.add( QUEUE_NAME.toNamedParameter(params.get(0) ) );
        addDEQUEUE_CONDITION( startParams, params.get(1) );
        break;
      case 3:
        startParams.add( QUEUE_NAME.toNamedParameter(params.get(0) ) );
        if( isInteger( params.get(2) ) ) {
          startParams.add( TRANSACTION_SAFE.toNamedParameter(params.get(1) ) );
          startParams.add( CONNECTION_POOLSIZE.toNamedParameter(params.get(2) ) );
        } else {
          addDEQUEUE_CONDITION( startParams, params.get(1) );
          startParams.add( USE_PL_SQL_PACKAGE.toNamedParameter(params.get(2) ) );
        }
        break;
      case 4:
        startParams.add( QUEUE_NAME.toNamedParameter(params.get(0) ) );
        addDEQUEUE_CONDITION( startParams, params.get(1) );
        if( isInteger( params.get(3) ) ) {
          startParams.add( TRANSACTION_SAFE.toNamedParameter(params.get(2) ) );
          startParams.add( CONNECTION_POOLSIZE.toNamedParameter(params.get(3) ) );
        } else {
          startParams.add( USE_PL_SQL_PACKAGE.toNamedParameter(params.get(2) ) );
          startParams.add( AUTORECONNECT.toNamedParameter(params.get(3) ) );
        }
        break;
      case 5:
        startParams.add( QUEUE_NAME.toNamedParameter(params.get(0) ) );
        addDEQUEUE_CONDITION( startParams, params.get(1) );
        startParams.add( USE_PL_SQL_PACKAGE.toNamedParameter(params.get(2) ) );
        if( isInteger( params.get(4) ) ) {
          startParams.add( TRANSACTION_SAFE.toNamedParameter(params.get(3) ) );
          startParams.add( CONNECTION_POOLSIZE.toNamedParameter(params.get(4) ) );
        } else {
          startParams.add( AUTORECONNECT.toNamedParameter(params.get(3) ) );
          startParams.add( RESPONSE_QUEUE_NAME.toNamedParameter(params.get(4) ) );
        }
        break;
      case 6:
        startParams.add( QUEUE_NAME.toNamedParameter(params.get(0) ) );
        addDEQUEUE_CONDITION( startParams, params.get(1) );
        startParams.add( USE_PL_SQL_PACKAGE.toNamedParameter(params.get(2) ) );
        startParams.add( AUTORECONNECT.toNamedParameter(params.get(3) ) );
        startParams.add( TRANSACTION_SAFE.toNamedParameter(params.get(4) ) );
        startParams.add( CONNECTION_POOLSIZE.toNamedParameter(params.get(5) ) );
        break;
      case 7:
        startParams.add( QUEUE_NAME.toNamedParameter(params.get(0) ) );
        addDEQUEUE_CONDITION( startParams, params.get(1) );
        startParams.add( USE_PL_SQL_PACKAGE.toNamedParameter(params.get(2) ) );
        startParams.add( AUTORECONNECT.toNamedParameter(params.get(3) ) );
        startParams.add( RESPONSE_QUEUE_NAME.toNamedParameter(params.get(4) ) );
        startParams.add( TRANSACTION_SAFE.toNamedParameter(params.get(5) ) );
        startParams.add( CONNECTION_POOLSIZE.toNamedParameter(params.get(6) ) );
        break;
      default:
        throw new XACT_InvalidStartParameterCountException();
    }
    return startParams;
  }
  
  private void addDEQUEUE_CONDITION(List<String> startParams, String dequeueCondition) {
    if( ! "null".equalsIgnoreCase(dequeueCondition) ) {
      startParams.add( DEQUEUE_CONDITION.toNamedParameter(dequeueCondition) );
    }
  }

  private boolean isInteger(String string) {
    try {
      Integer.parseInt(string);
      return true;
    } catch( NumberFormatException e ) {
      return false;
    }
  }

  public boolean isUsePLSQLPackage() {
    return usePLSQLPackage;
  }

  public QueueData getRequestQueue() {
    return requestQueue;
  }
  
  public QueueData getResponseQueue() {
    return responseQueue;
  }

  public boolean isExplicitResponseQueue() {
    return explicitResponseQueue;
  }
  
  public boolean isAutoReconnect() {
    return autoReconnect;
  }
  
  public boolean isTransactionSafe() {
    return transactionSafe;
  }

  public String getPackageName() {
    return packageName;
  }

  public int getConnectionPoolSize() {
    return connectionPoolSize;
  }
  
  public int getResponseConnectionPoolSize() {
    return responseConnectionPoolSize;
  }

  public DequeueOptions getDequeueOptions() {
    return dequeueOptions;
  }
  
  public EnqueueOptions getEnqueueOptions() {
    return enqueueOptions;
  }

  public long getAutoReconnectRetryInterval() {
    return autoReconnectRetryInterval;
  }

  public String getDequeueSQL() {
    return dequeueSQL;
  }
  
  public String getEnqueueSQL() {
    return enqueueSQL;
  }

  private QueueData initQueueData(String uniqueName, String paramName, String consumerName) throws XACT_InvalidTriggerStartParameterValueException {
    if( uniqueName == null ) {
      return null; //Queue existiert nicht
    }
    Queue queue = null;
    try {
      QueueManagement mgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getQueueManagement();
      queue = mgmt.getQueue(uniqueName);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XACT_InvalidTriggerStartParameterValueException(paramName, e);
    } catch (Exception e) {
      throw new XACT_InvalidTriggerStartParameterValueException(paramName, e);
    }  
    
    if (logger.isDebugEnabled()) {
      logger.debug("Got Stored Queue: " + queue.toString());
    }
    if (queue.getQueueType() != QueueType.ORACLE_AQ) {
      throw new XACT_InvalidTriggerStartParameterValueException(paramName,
                                                                new ORACLEAQTRIGGER_WrongQueueTypeException(uniqueName));
    }
    QueueConnectData connData = queue.getConnectData();
    if( connData instanceof OracleAQConnectData ) {
      return new QueueData( queue.getExternalName(), (OracleAQConnectData) connData, consumerName);
    } else {
      throw new XACT_InvalidTriggerStartParameterValueException(paramName,
                                                                new ORACLEAQTRIGGER_WrongQueueTypeException(uniqueName));
    }
  }

  private List<String> splitAdditional(String additionalString) {
    if( additionalString == null || additionalString.length() == 0 ) {
      return Collections.emptyList();
    }
    List<String> additional = new ArrayList<String>();
    for( String a : additionalString.split(",") ) {
      a = a.trim();
      if( a.length() != 0 ) {
        additional.add(a);
      }
    }
    return Collections.unmodifiableList(additional);
  }



}
