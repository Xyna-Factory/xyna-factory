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
package com.gip.xyna.xact.trigger.oracleaq.shared;

import java.util.Arrays;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.db.SQLUtils;


/**
 *
 */
public class QueueTest {

  private static Logger logger = Logger.getLogger(QueueTest.class);
  /*
  public static class SQLLogger implements SQLUtilsLogger {
    Logger logger;
    public SQLLogger(Logger newLogger) {
      logger = newLogger;
    }
    public void logException(Exception e) {
      throw new SQLRuntimeException(e);
    }
    public void logSQL(String sql) {
      if( logger.isDebugEnabled() ) {
        logger.debug("SQL= " + sql);
      }
    }
  }
  */
  
  public static class SQLRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SQLRuntimeException(Exception e) {
      super(e);
    }
    
  }
  
  
  /**
   * @param args
   * @throws NoConnectionAvailableException 
   */
  public static void main(String[] args) throws NoConnectionAvailableException {
    Logger rootLogger = Logger.getRootLogger();
    rootLogger.setLevel(Level.DEBUG);
    rootLogger.removeAllAppenders();
    rootLogger.addAppender(new ConsoleAppender(new PatternLayout("XYNA %-5p [%t] (%C:%M:%L) - [%x] %m%n")));
  
    QueueData queueData = new QueueData("testAQ", "", "", "", "TestConsumer");
    
    //DBConnectionData dbd = queueData.getDBConnectionData(null);
    
    DequeueOptions dequeueOptions = DequeueOptions.newDequeueOptions().
        additional(Arrays.asList(PLSQLBuilder.MSG_ID,PLSQLBuilder.ENQUEUE_TIME,"interface_version")).
        consumerName(queueData.getConsumerName()).
        //dequeueCondition("%").
        queueName(queueData.getQueueName()).
        build();
    
    EnqueueOptions enqueueOptions = EnqueueOptions.newEnqueueOptions().
        additional(Arrays.asList("interface_version")).
        consumerName(queueData.getConsumerName()).
        queueName(queueData.getQueueName()).
        build();
    
    SQLUtilsCreator sqlUtilsCreator = new SQLUtilsCreator("pool", queueData, 2);
    SQLUtils sqlUtils = sqlUtilsCreator.getSQLUtils("ci", logger, Level.TRACE );
    //SQLUtils sqlUtils = dbd.createSQLUtils(new SQLLogger(logger) );
    try {
      //String queueName = "testAQ";
      String packageName = "testPack";
      String queueName = "testQueue";
      String responseQueueName = "responseQueue";
      
      //String message = "RuntimeException";
      String message = "XynaException";
      //String message = "NotResponsible";
      //String message = "Hiho";
      
      
      existsQueue(sqlUtils, queueData);
      //createQueue(sqlUtils, queueData);
      
      //existsQueue(sqlUtils, queueData);
      //dropQueue(sqlUtils, queueData);
      //existsQueue(sqlUtils, queueData);
      
      //enqueue(sqlUtils, queueData, message);
      //dequeue(sqlUtils, queueData);
     
      
      createPackage(sqlUtils, packageName, dequeueOptions, enqueueOptions);
      existsPackage(sqlUtils, packageName);
      //dropPackage(sqlUtils, packageName);
      
      loadTest(sqlUtils, packageName, enqueueOptions, dequeueOptions, 50000);
      
      /*
      enqueuePackage(sqlUtils, packageName, enqueueOptions, "Hallo");
      OracleAQMessage msg = dequeuePackage(sqlUtils, packageName, dequeueOptions);
      System.err.println( "msgId = "+msg.getMsgId() );
      System.err.println( "text = "+msg.getText() );
      System.err.println( "corrID = "+msg.getCorrelationID() );
      System.err.println( "enqueueTime = "+msg.getEnqueueTime()+ ", diff="+(System.currentTimeMillis()-msg.getEnqueueTime()) );
      System.err.println( "interface_version =" + msg.getProperty("interface_version") );
      */
      
      
      //enqueueDequeue(sqlUtils, queueName, message, responseQueueName );
      sqlUtils.commit();
      
    } finally {
      sqlUtils.closeConnection();
    }
  }




  private static void loadTest(SQLUtils sqlUtils, String packageName, 
                               EnqueueOptions enqueueOptions, DequeueOptions dequeueOptions,
                               int size) {
    long start = System.currentTimeMillis();
    try {
      long t1 = start;
      for( int i=1; i<= size; ++i ) {
        enqueuePackage(sqlUtils, packageName, enqueueOptions, "Hallo", i );
        sqlUtils.commit();
        OracleAQMessage msg = dequeuePackage(sqlUtils, packageName, dequeueOptions);
        //System.out.println( msg.getMsgId() +"  "+ msg.getCorrelationID() );
        sqlUtils.commit();

        if( i % 1000 == 0 ) {
          long t2 = System.currentTimeMillis();
          System.out.println( i/1000 +": 1000 Enqueue/Dequeues in "+(t2-t1)+" ms");
          t1 = t2;
        }
      }
    } finally {
      long end = System.currentTimeMillis();
      System.out.println( ((end-start)/1000) + " seconds for "+size+ " Enqueue/Dequeues");
    }
  }

  /**
   * @param sqlUtils
   * @param string
   * @param message
   * @param string2
   */
  private static void enqueueDequeue(SQLUtils sqlUtils, String queueName, String message, String responseQueueName) {
    String corrId = "corr-"+System.currentTimeMillis();
    OracleAQMessage aqMessage = new OracleAQMessage(corrId, message );
    EnqueueOptions enqueueOptions = new EnqueueOptions(queueName);
    PLSQLExecUtils.enqueueDirect(sqlUtils, aqMessage, enqueueOptions );
    sqlUtils.commit();
    
    DequeueOptions dequeueOptions = DequeueOptions.newDequeueOptions().
        queueName(responseQueueName).
        dequeueCondition_CorrId(corrId).
        build();
    
    OracleAQMessage responseMessage = PLSQLExecUtils.dequeueDirect(sqlUtils, dequeueOptions, true );
    System.err.println( responseMessage );
  }







  private static void existsQueue(SQLUtils sqlUtils, QueueData queueData) {
    String queueName = queueData.getQueueName();
    boolean exists = PLSQLExecUtils.existsQueue(sqlUtils, queueName);
    System.err.println( "Queue "+queueName+(exists?" exists":" does not exist") );
  }

  private static void createQueue(SQLUtils sqlUtils, QueueData queueData) {
    PLSQLExecUtils.createQueue(sqlUtils,queueData);
  }
  
  private static void dropQueue(SQLUtils sqlUtils, QueueData queueData) {
    PLSQLExecUtils.dropQueue(sqlUtils,queueData);
  }
  
  private static void enqueue(SQLUtils sqlUtils, QueueData queueData, String message) {
    OracleAQMessage aqMessage = new OracleAQMessage("corr-123", message );
    EnqueueOptions enqueueOptions = new EnqueueOptions(queueData.getQueueName());
    PLSQLExecUtils.enqueueDirect(sqlUtils, aqMessage, enqueueOptions );
  }

  private static void enqueuePackage(SQLUtils sqlUtils, String packageName, EnqueueOptions enqueueOptions, String message, int i) {
    OracleAQMessage aqMessage = OracleAQMessage.newOracleAQMessage().
        corrID("corr-"+i).text(message).
        addProperty("interface_version", "5.5").
        build();
    PLSQLExecUtils.enqueueWithPackage(sqlUtils, packageName, aqMessage, enqueueOptions );
  }

  private static void dequeue(SQLUtils sqlUtils, QueueData queueData) {
    DequeueOptions dequeueOptions = new DequeueOptions(queueData.getQueueName(),queueData.getConsumerName());
    OracleAQMessage aqMessage = PLSQLExecUtils.dequeueDirect(sqlUtils, dequeueOptions, true );
    System.err.println( aqMessage );
  }
  
  private static OracleAQMessage dequeuePackage(SQLUtils sqlUtils, String packageName, DequeueOptions dequeueOptions) {
    return PLSQLExecUtils.dequeueWithPackage(sqlUtils, packageName, dequeueOptions, true );
  }  

  private static void createPackage(SQLUtils sqlUtils, String packageName, DequeueOptions dequeueOptions, EnqueueOptions enqueueOptions) {
    PLSQLExecUtils.createPackage(sqlUtils, packageName, dequeueOptions, enqueueOptions);
  }
  
  private static void existsPackage(SQLUtils sqlUtils, String packageName) {
    boolean exists = PLSQLExecUtils.existsPackage(sqlUtils, packageName);
    System.err.println( "existsPackage \""+packageName+"\"? "+exists );
  }
  
  private static void dropPackage(SQLUtils sqlUtils, String packageName) {
    PLSQLExecUtils.dropPackage(sqlUtils, packageName);
  }


}
