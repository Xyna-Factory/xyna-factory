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
package com.gip.xyna.xact.trigger.oracleaq.shared;

import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;

import oracle.sql.CLOB;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.db.OutputParam;
import com.gip.xyna.utils.db.OutputParamFactory;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.SQLUtils;


/**
 * Hilfsfunktionen zum Dequeue/Enqueue, zum Einrichten einer Queue und zum Bau eines Package.
 * Alle Funktionen machen kein Transactionhandling.
 * Auch das Auswerten von SQLExceptions muss extern durchgef�hrt werden. 
 *
 */
public class PLSQLExecUtils {  
 
  private static Logger logger = CentralFactoryLogging.getLogger(PLSQLExecUtils.class);
  
  private static class OutputParamCLOB implements OutputParam<String> {

    String data;
    
    public String get() {
      return data;
    }

    public int getSQLType() {
      return Types.CLOB;
    }

    public void set(Object arg0) {
      if( arg0 instanceof Clob ) {
        Clob clob = ((Clob)arg0);
        try {
          data = clob.getSubString(1,(int)clob.length() );
        } catch (SQLException e) {
          Logger.getLogger(OutputParamCLOB.class).warn( "Could not read CLOB ", e );
        } finally {
          if( clob instanceof CLOB ) {
            CLOB cl = (CLOB)clob;
            try {
              if( cl.isTemporary() ) {
                cl.freeTemporary();
              }
            } catch( SQLException e) {
              Logger.getLogger(OutputParamCLOB.class).warn( "Could not free temp CLOB ", e );
            }
          }
        }
      } else {
        Logger.getLogger(OutputParamCLOB.class).info("Could not read CLOB: unexpected Type: "+ arg0 );
      }
    }
    
    public String toString() {
      return "OutputParam<Clob>";
    }
    
  }

  public static void enqueueDirect(SQLUtils sqlUtils, OracleAQMessage message, EnqueueOptions enqueueOptions ) {
    enqueue(sqlUtils, PLSQLBuilder.buildEnqueueBlock(enqueueOptions), message, enqueueOptions, false);
  }
  
  public static void enqueueWithPackage(SQLUtils sqlUtils, String packageName, OracleAQMessage message, EnqueueOptions enqueueOptions) {
    enqueue(sqlUtils, PLSQLBuilder.buildEnqueueWithPackage(packageName,enqueueOptions), message, enqueueOptions, true);
  }
  
  public static void enqueue(SQLUtils sqlUtils, String sql, 
                             OracleAQMessage message, EnqueueOptions enqueueOptions,
                             boolean usePLSQLPackage) {
    Parameter param = new Parameter();
    if( usePLSQLPackage ) {
      //leider stimmt Reihenfolge im Package nicht mit direkt verwendbarer Reihenfolge �berein.
      //Package-Deklaration kann wegen Abw�rtskompatibilit�t nicht ge�ndert werden
      param.addParameter(enqueueOptions.getQueueName());
      param.addParameter(message.getCorrelationID());
      param.addParameter(message.getText());
      param.addParameter(enqueueOptions.getPriority( message.getPriority() ));
    } else {
      param.addParameter(enqueueOptions.getQueueName());
      param.addParameter(message.getText());
      param.addParameter(message.getCorrelationID());
      param.addParameter(enqueueOptions.getPriority( message.getPriority() ));
      param.addParameter(enqueueOptions.getDelay());
      param.addParameter(enqueueOptions.getExpiration());
    }
    for( String key : enqueueOptions.getAdditional() ) {
      param.addParameter( message.getProperty( key ) );
    }
    sqlUtils.executeBlock(sql, param);
  }

  public static OracleAQMessage dequeueDirect(SQLUtils sqlUtils, DequeueOptions dequeueOptions, boolean catchDequeueTimeout ) {
    String sql = PLSQLBuilder.buildDequeueBlock(dequeueOptions);
    return dequeue(sqlUtils, sql, dequeueOptions, catchDequeueTimeout, false);
  }
  public static OracleAQMessage dequeueWithPackage( SQLUtils sqlUtils, String packageName,
                                                    DequeueOptions dequeueOptions, boolean catchDequeueTimeout ) {
    String sql = PLSQLBuilder.buildDequeueWithPackage(packageName, dequeueOptions);
    return dequeue(sqlUtils, sql, dequeueOptions, catchDequeueTimeout, true);
  }

  public static OracleAQMessage dequeue( SQLUtils sqlUtils, String sql, DequeueOptions dequeueOptions, 
                                         boolean catchDequeueTimeout, boolean usePLSQLPackage ) {
    OutputParam<String> output = new OutputParamCLOB();
    OutputParam<String> corrId = OutputParamFactory.createString();
    OutputParam<Integer> priority = OutputParamFactory.createInteger();
    Parameter param = new Parameter();
    if( usePLSQLPackage ) {
      //leider stimmt Reihenfolge im Package nicht mit direkt verwendbarer Reihenfolge �berein.
      //Package-Deklaration kann wegen Abw�rtskompatibilit�t nicht ge�ndert werden
      param.addParameter(dequeueOptions.getQueueName());
      param.addParameter(dequeueOptions.getTimeout());
      param.addParameter(corrId);
      param.addParameter(output);
      param.addParameter(dequeueOptions.getDequeueCondition());
      param.addParameter(priority);
    } else {
      param.addParameter(dequeueOptions.getTimeout());
      param.addParameter(dequeueOptions.getDequeueCondition());
      param.addParameter(dequeueOptions.getQueueName());
      param.addParameter(corrId);
      param.addParameter(priority);
      param.addParameter(output);
    }
    int additionalSize = dequeueOptions.getAdditional().size();
    OutputParam<?>[] additional = new OutputParam[additionalSize];
    for( int i=0; i<additionalSize; ++i ) {
      OutputParam<String> add = OutputParamFactory.createString();
      additional[i] = add;
      param.addParameter(add);
    }
    boolean success = false;
    if( catchDequeueTimeout ) {
      success = executeBlockAndCatchDequeueTimeout( sqlUtils, sql, param );
    } else {
      success = sqlUtils.executeBlock(sql, param);
    }
    if( success ) {
      OracleAQMessage.Builder message = OracleAQMessage.newOracleAQMessage();
      message.corrID(corrId.get());
      message.text(output.get());
      message.priority(priority.get());
      for( int i=0; i<additionalSize; ++i ) {
        String value = String.valueOf( additional[i].get() );
        String key = dequeueOptions.getAdditional().get(i);
        if( PLSQLBuilder.MSG_ID.equals(key) ) {
          message.msgId(value);
        } else if( PLSQLBuilder.ENQUEUE_TIME.equals(key) ) {
          synchronized (PLSQLBuilder.ENQUEUE_TIME_FORMAT) { //SimpleDateFormat ist nicht threadsafe!
            try {
              message.enqueueTime( PLSQLBuilder.ENQUEUE_TIME_FORMAT.parse(value).getTime() );
            } catch (ParseException e) {
              logger.warn("Could not parse enqueueTime \""+value+"\"" );
              //TODO wie Zeit setzen?
            }
          }
        } else {
          message.addProperty( key, value );
        }
      }
      return message.build();
    } else {
      return null;
    }
  }

  
  private static boolean executeBlockAndCatchDequeueTimeout(SQLUtils sqlUtils, String sql, Parameter param) {
    SQLException lastException = null;
    boolean success = false;
    try {
      sqlUtils.setLogException(false); //verhindert das Loggen/Umsetzen in SQLRuntimeException
      success = sqlUtils.executeBlock(sql, param);
      lastException = sqlUtils.getLastException();
    } finally {
      sqlUtils.setLogException(true);
    }
    if( lastException != null ) {
      if( lastException.getErrorCode() == 25228 ) { //timeout or end-of-fetch during message dequeue from ... 
        //keine Message zu dequeuen
      } else {
        sqlUtils.logLastException(); //doch noch loggen/werfen als SQLRuntimeException
      }
    }
    return success;
  }
  
  public static void createQueue(SQLUtils sqlUtils, QueueData queueData) {
    createQueue(sqlUtils, queueData.getQueueName(), queueData.getConsumerName() );
  }

  public static void createQueue(SQLUtils sqlUtils, String queueName) {
    createQueue(sqlUtils, queueName, null);
  }
  public static void createQueue(SQLUtils sqlUtils, String queueName, String consumerName) {
    String sql = PLSQLBuilder.buildCreateQueue(consumerName!=null);
    Parameter params = new Parameter(queueName, queueName+"_t", consumerName);
    sqlUtils.executeDDL(sql,params);
  }
  
  public static void dropQueue(SQLUtils sqlUtils, QueueData queueData) {
    dropQueue(sqlUtils, queueData.getQueueName(), queueData.getConsumerName() );
  }

  public static void dropQueue(SQLUtils sqlUtils, String queueName, String consumerName) {
    String sql = PLSQLBuilder.buildDropQueue(consumerName!=null);
    Parameter params = new Parameter(queueName, queueName+"_t", consumerName);
    sqlUtils.executeDDL(sql,params);
  }

  public static void createPackage(SQLUtils sqlUtils, String packageName) {
    Pair<String,String> pair = PLSQLBuilder.buildCreatePackage(packageName, DequeueOptions.newDequeueOptions().build(), EnqueueOptions.newEnqueueOptions().build()  );
    sqlUtils.executeDDL( pair.getFirst(), null);
    sqlUtils.executeDDL( pair.getSecond(), null);
  }
  
  public static void createPackage(SQLUtils sqlUtils, String packageName,
                                   DequeueOptions dequeueOptions, EnqueueOptions enqueueOptions) {
    Pair<String,String> pair = PLSQLBuilder.buildCreatePackage( packageName, dequeueOptions, enqueueOptions );
    sqlUtils.executeDDL( pair.getFirst(), null);
    sqlUtils.executeDDL( pair.getSecond(), null);
  }

  public static void dropPackage(SQLUtils sqlUtils, String packageName) {
    sqlUtils.executeDDL( PLSQLBuilder.buildDropPackage(packageName), null);
  }
  
  public static boolean existsPackage(SQLUtils sqlUtils, String packageName) {
    String sql = "SELECT count(*) FROM User_Objects WHERE object_type='PACKAGE' AND object_name = upper(?)";
    int cnt = sqlUtils.queryInt(sql, new Parameter(packageName) );
    return cnt == 1;
  }
  
  public static boolean existsQueue(SQLUtils sqlUtils, String queueName) {
    String sql = "SELECT count(*) FROM User_Objects WHERE object_type='QUEUE' AND object_name = upper(?)";
    int cnt = sqlUtils.queryInt(sql, new Parameter(queueName) );
    return cnt == 1;
  }

  
  
}
