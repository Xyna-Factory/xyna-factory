/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;



import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.SQLUtilsLogger;
import com.gip.xyna.xact.trigger.oracleaq.shared.EnqueueableOracleAQMessage;
import com.gip.xyna.xact.trigger.oracleaq.shared.OracleAQMessage;
import com.gip.xyna.xact.trigger.oracleaq.shared.PLSQLBuilder;
import com.gip.xyna.xact.trigger.oracleaq.shared.QueueData;
import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.DefaultSQLUtilsLogger.DBProblemHandler;



public class RemoteInterfaceForClusterStateChangesImplAQ implements RemoteInterfaceForClusterStateChanges, Runnable {

  private static final Logger logger = CentralFactoryLogging
      .getLogger(RemoteInterfaceForClusterStateChangesImplAQ.class);
  
  public static enum Change {
    join("rj"),
    startup("rt"),
    connect("rc"),
    disconnectRequest("rs"),
    waiting("rw");
    //e-<binding> sind antworten, und hier in diesem enum stehen nur requests
    
    private String text;
    private Change(String text) {
      this.text = text;
    }
   
    public static Change fromText( String text) {
      for( Change ch : values() ) {
        if( text.startsWith( ch.text ) ) {
          return ch;
        }
      }
      return null;
    }

    public String createMsgText(int binding) {
      return  text + "-" + binding;
    }
    
  }
  
  /**
   * wartezeit auf antworten, falls über aq kommuniziert wird
   */
  public static int ANSWER_TIMEOUT = 5;
  /**
   * wartezeit des listeners, bevor er erneut versucht. wirkt sich auf die dauer des shutdowns aus
   */
  public static int DEQUEUE_TIMEOUT = 10;
  /**
   * wartezeit auf das asynchrone connect des anderen knoten
   */
  public static int CONNECTGUARD_TIMEOUT = 10;

  private final Thread listenerThread;
  private final QueueData queue;
  private final ConnectionPool connectionPool;
  private SQLUtils sqlUtilsForDequeue;
  private CallableStatement callableStatement;
  private volatile boolean running;
  private final RemoteInterfaceForClusterStateChanges impl;
  private int ownBinding;
  private final DBProblemHandler dbProblemHandler;
  private final ClusterProvider clusterProvider;

  private AtomicLong cnt = new AtomicLong(0);
  private volatile boolean listenerInitialized;
  private volatile SQLException listenerStartException;
  private long connectTimeout;
  public volatile ConnectGuard connectGuard;


  public RemoteInterfaceForClusterStateChangesImplAQ(ConnectionPool connectionPool,
                                                     String dbUserName,
                                                     RemoteInterfaceForClusterStateChanges propagateIncomingRequestsToThis,
                                                     long connectTimeout, DBProblemHandler dbProblemHandler, ClusterProvider clusterProvider) {
    String queueName = "interconnectq";
    this.dbProblemHandler = dbProblemHandler;
    this.clusterProvider = clusterProvider;
    this.connectionPool = connectionPool;
    this.connectTimeout = connectTimeout;
    this.listenerThread = new Thread(this, "XOracleInterconnectListenerThread");
    this.listenerThread.setDaemon(true);
    this.impl = propagateIncomingRequestsToThis;
    this.queue = new QueueData(queueName, dbUserName, null, null, null);
  }


  /**
   * @throws DBNotReachableException
   */
  public void start(int binding) throws DBNotReachableException {
    this.ownBinding = binding;
    if (!running) {
      running = true;
      listenerInitialized = false;
      listenerStartException = null;
      listenerThread.start();

      //Warten, dass listenerThread korrekt initialisiert werden konnte
      while (!listenerInitialized) {
        try {
          Thread.sleep(20);
        } catch (InterruptedException e) {
          logger.info("ignored exception " + e.getMessage(), e);
        }
      }
      if (listenerStartException != null) {
        throw new DBNotReachableException(listenerStartException);
      }
    }
  }


  /**
   * richtet die Queue ein, falls diese noch nicht existiert. Achtung: dies wird commitet
   * @param sqlUtils
   * @param queueName
   * @param dbUserName
   */
  private void initQueue(SQLUtils sqlUtils, String queueName, String dbUserName) {
    Integer cnt =
        sqlUtils.queryInt("select count(*) from all_queues where owner = ? and name = ?",
                          new Parameter(dbUserName.toUpperCase(), queueName.toUpperCase()));
    if (cnt == null) {
      throw new RuntimeException("could not select queue");
    } else if (cnt == 0) {
      //queue erstellen, weil nicht vorhanden
      String queueTableName = queueName + "_t";
      StringBuilder sb = new StringBuilder(300);
      sb.append("{call BEGIN\n");
      sb.append("  DBMS_AQADM.CREATE_QUEUE_TABLE (queue_table => '")
          .append(queueTableName)
          .append("',queue_payload_type => 'SYS.AQ$_JMS_TEXT_MESSAGE', multiple_consumers =>FALSE, sort_list => 'priority,enq_time');\n");
      sb.append("  DBMS_AQADM.CREATE_QUEUE (queue_name => '").append(queueName).append("', queue_table =>'")
          .append(queueTableName).append("');\n");
      sb.append("  DBMS_AQADM.START_QUEUE (queue_name => '").append(queueName).append("');\n");
      sb.append("END\n}");

      sqlUtils.executeCall(sb.toString(), new Parameter());
      sqlUtils.commit();
    }
  }


  /**
   * @return
   * @throws DBNotReachableException
   */
  public SQLUtils createSqlUtils(final String info) throws DBNotReachableException {
    long start = System.currentTimeMillis();
    boolean success = false;
    try {
      Logger localLogger = logger;
      if (true) { //TODO konfigurierbar, ob logger oder null übergeben wird, um loghierarchy steuern zu können
        localLogger = null;
      }
      final SQLUtilsLogger infoLogger = new DefaultSQLUtilsLogger(localLogger, dbProblemHandler, Level.INFO);
      SQLUtilsLogger debugLogger = new DefaultSQLUtilsLogger(localLogger, dbProblemHandler, Level.DEBUG);
      SQLUtils sqlUtils = new SQLUtils( connectionPool.getConnection(connectTimeout, "xynaClusterProviderInterconnect-" + info),
        debugLogger ) {
        
          @Override
          public boolean closeConnection() {
            setLogger(infoLogger); //damit das CLOSE auf info geloggt wird
            return super.closeConnection();
          }
        
      };
      infoLogger.logSQL("OPEN"); //transaktion sichtbar machen FIXME: zeigt verursachende zeile mit (?:?:?) an
      success = true;
      return sqlUtils;
    } catch (Exception e) {
      throw new DBNotReachableException(e);
    } finally {
      long delay = System.currentTimeMillis()-start;
      if( delay > 10 ) {
        logger.warn("Got "+(success?"validated":"no")+" connection after "+delay+" ms for \""+info+"\"");
      }
    }
  }

  private void createSQLUtilsForDequeue() throws SQLException {
    sqlUtilsForDequeue = connectionPool.getSQLUtils(connectTimeout, "xynaClusterProviderInterconnect-dequeue", logger);
    //connectionPool sollte eine valide Connection oder eine Exception liefern
    callableStatement = createCallableStatement(sqlUtilsForDequeue);
  }


  private CallableStatement createCallableStatement(SQLUtils sqlUtils) throws SQLException {
    return sqlUtils.prepareCall(PLSQLBuilder.buildDequeueBlock());
  }


  public void run() {
    //Initialisierung
    try {
      createSQLUtilsForDequeue();
      initQueue(sqlUtilsForDequeue, queue.getQueueName(), queue.getDbSchema());
    } catch (SQLException e) {
      running = false;
      listenerStartException = e;
    }
    listenerInitialized = true;

    int failureCount = 0;
    try {
      while (running) {
        try {
          listen();
        } catch (Throwable t) {
          ++failureCount;
          if (logger.isInfoEnabled()) {
            logger.info("run("+failureCount+"): " + t.getMessage());
          }
          if (t instanceof SQLException) {
            if (((SQLException) t).getErrorCode() == 1013) {
              //ORA-01013: user requested cancel of current operation
            } else {
              logger.error("run("+failureCount+"): got unexpected sql exception: " + t.getMessage(), t);
            }
          } else {
            logger.error("run("+failureCount+"): Unexpected exception in listener thread" + (running ? ", trying to keep thread running" : "")
                + ": " + t.getMessage(), t);
          }
          if (!running) {
            continue; //Thread soll beendet werden, daher zurück zum while
          }
          //connection neu aufmachen
          while( running && !reopenConnection() ) {
            logger.error("run("+failureCount+"): could not reopen connection, checking dbNotReachable");
            dbProblemHandler.dbNotReachable();
          }
        }
      }
    } finally {
      if (running) {
        logger.error("run("+failureCount+"): interconnect thread shut down and should be running");
      }
      //runtimeexceptions etc: trotzdem connection schliessen
      finallyClose(sqlUtilsForDequeue, callableStatement);
    }
  }


  /**
   * @param retryCnt
   */
  private boolean reopenConnection() {
    try {
      finallyClose(sqlUtilsForDequeue, callableStatement);
      if (running) {
        logger.info("trying reconnect.");
        createSQLUtilsForDequeue();
        return true;
      }
    } catch (Exception e) {
      //die Connections kommen aus einem Pool, der sie vor der Ausgabe testet und mehrfach versucht, 
      //neue valide Connections zu erhalten. Daher bring ein weiterer Retry hier keinen Mehrwert, daher Abbruch 
      logger.error("error while reopenConnection: " + e.getMessage(), e);
      return false;
    }
    return false;
  }


  private void finallyClose(SQLUtils sqlUtils, CallableStatement cs) {
    sqlUtils.finallyClose(null, cs);
    try {
      if (!sqlUtils.getConnection().isClosed()) {
        sqlUtils.closeConnection();
      }
    } catch (SQLException e) {
      logger.warn("connection could not be closed", e);
    }
  }


  private void listen() throws SQLException {
    //weil eigene requests mit der correlationid mit dem eigenen binding eingestellt werden, bekommt man
    //so nur die requests von anderen knoten.
    OracleAQMessage msg;
    try {
      msg =
          getNextMessage(sqlUtilsForDequeue, DEQUEUE_TIMEOUT, "corrId LIKE 'REQ-%' AND corrId NOT LIKE 'REQ-"
              + ownBinding + "-%'", callableStatement);
    } catch (TimeoutException e) {
      return;
    }
    if( impl.isShutdown() ) {
      return; //nicht mehr antworten
    }
    
    if (msg != null) {
      String text = msg.getText();
      if (logger.isTraceEnabled()) {
        logger.trace("got message[corrId = " + msg.getCorrelationID() + "]: " + msg.getText() + "ownBinding="
            + ownBinding);
      }
      if (msg.getCorrelationID() == null || msg.getText() == null) {
        logger.warn("got null message or null correlationid");
        return;
      }
      boolean invalidMessageInQueue = false;
      if (text.length() < 3) {
        invalidMessageInQueue = true;
      } else {
        int remoteBinding = -1;
        try {
          int idx = text.indexOf('-');
          remoteBinding = Integer.valueOf(text.substring(idx + 1));
          Change change = Change.fromText(text);
          if( change == null ) {
            invalidMessageInQueue = true;
          } else {
            try {
              ConnectGuard cg = connectGuard;
              switch(change) {
                case join:
                  impl.join(sqlUtilsForDequeue, remoteBinding);
                  break;
                case startup:
                  impl.startup(sqlUtilsForDequeue, remoteBinding);
                  break;
                case connect:
                  if( cg != null ) {
                    cg.cancel();
                  } else {
                    logger.warn("Unexpected connect: ConnectGuard is null!");
                  }
                  impl.connect(sqlUtilsForDequeue, remoteBinding);
                  break;
                case disconnectRequest:
                  impl.disconnect(sqlUtilsForDequeue, remoteBinding);
                  break;
                case waiting:
                  if( cg != null ) {
                    cg.keepWaiting();
                  } else {
                    logger.info("Unexpected waiting: ConnectGuard is null!");
                  }
                  impl.waiting(sqlUtilsForDequeue, remoteBinding);
                  break;
              }
            } catch( Throwable t ) {
              Department.handleThrowable(t);
              logger.error("Failed to react to external request", t);
            }
          }
        } catch (NumberFormatException e) {
          invalidMessageInQueue = true;
        }
        if (invalidMessageInQueue) {
          logger.warn("ignoring invalid msg in queue: " + text + " corrid = " + msg.getCorrelationID());
        } else {
          EnqueueableOracleAQMessage response =
              new EnqueueableOracleAQMessage(createResponseCorrelationId(msg.getCorrelationID()), "e-" + ownBinding);
          response.sendToQueue(queue, sqlUtilsForDequeue);
        }
        sqlUtilsForDequeue.commit();
      }
    }
  }


  private static class TimeoutException extends Exception {

    public TimeoutException(SQLException e) {
      super(e);
    }


    private static final long serialVersionUID = 1L;

  }


  private OracleAQMessage getNextMessage(SQLUtils sqlUtils, int timeout, String dequeueCondition, CallableStatement cs)
      throws SQLException, TimeoutException {
    cs.setString(1, Integer.toString(timeout));
    cs.setString(2, dequeueCondition);
    cs.setString(3, queue.getQueueName());

    cs.registerOutParameter(4, java.sql.Types.VARCHAR);
    cs.registerOutParameter(5, java.sql.Types.VARCHAR);
    cs.registerOutParameter(6, java.sql.Types.VARCHAR);

    try {
      sqlUtils.excuteUpdate(cs);
    } catch (SQLException e) {
      // ORA-25228: timeout or end-of-fetch during message dequeue from <schema>.<queueName>
      if (e.getErrorCode() == 25228) {
        throw new TimeoutException(e);
      }
      throw e;
    }

    String corrId = cs.getString(4);
    int priority = cs.getInt(5);
    String text = cs.getString(6);
    sqlUtils.commit();
    return new OracleAQMessage(corrId, text, priority);
  }


  private void callOtherNodeOverAQ(SQLUtils sqlUtils, Change change, int binding) throws DBNotReachableException,
      InterFactoryConnectionDoesNotWorkException {
    //nachricht nach AQ schreiben, auf antwort warten    

    String corrIdReq = createRequestCorrelationId(binding);
    String corrIdRes = createResponseCorrelationId(corrIdReq);
    EnqueueableOracleAQMessage msg = new EnqueueableOracleAQMessage(corrIdReq, change.createMsgText(binding));

    CallableStatement cs = null;
    try {
      cs = createCallableStatement(sqlUtils);

      //evtl. schon vorhandene, veraltete Antwort entfernen
      try {
        OracleAQMessage oldMsg = getNextMessage(sqlUtils, 0, "corrId = '" + corrIdRes + "'", cs);
        if (logger.isInfoEnabled()) {
          logger.info("old message found: " + oldMsg.getText());
        }
      } catch (TimeoutException e) {
        //Ok, erwartet; keine veraltete Antwort gefunden
      }

      //Nachricht einstellen

      msg.sendToQueue(queue, sqlUtils);

      //auf Antwort warten
      OracleAQMessage answerMsg = null;
      try {
        answerMsg = getNextMessage(sqlUtils, ANSWER_TIMEOUT, "corrId = '" + corrIdRes + "'", cs);
      } catch (TimeoutException e1) {
        //keine Antwort erhalten. Zur Sicherheit eigene Anfrage beseitigen
        try {
          getNextMessage(sqlUtils, 0, "corrId = '" + corrIdReq + "'", cs);
          if (logger.isInfoEnabled()) {
            logger.info("removed own " + change + "-request");
          }
          //nun Abbruch
          throw new InterFactoryConnectionDoesNotWorkException();
        } catch (TimeoutException e2) {
          //Oh, die Nachricht wurde doch schon entfernt!

          //ist die Antwort doch noch verspätet eingetroffen?
          try {
            answerMsg = getNextMessage(sqlUtils, 0, "corrId = '" + corrIdRes + "'", cs);
            //OK, Antwort erhalten
          } catch (TimeoutException e3) {
            logger.error("Inconsistent state: other node dequeued the request, but doesn't answer");
            throw new InterFactoryConnectionDoesNotWorkException();
          }
        }
      }
      if (logger.isTraceEnabled()) {
        logger.trace("got answer " + answerMsg.getText());
      }

    } catch (SQLException e) {
      throw new DBNotReachableException(e);
    } finally {
      sqlUtils.finallyClose(null, cs);
    }
  }


  private String createRequestCorrelationId(int binding) {
    return "REQ-" + binding + "-" + cnt.incrementAndGet();
  }


  private String createResponseCorrelationId(String corrId) {
    return "RES-" + corrId.substring(4); //REQ durch RES ersetzen
  }


  public void join(SQLUtils sqlUtils, int joinedBinding) throws InterFactoryConnectionDoesNotWorkException, DBNotReachableException {
    callOtherNodeOverAQWithConnectGuard(sqlUtils, joinedBinding,  "RAC-ConnectGuard-join", Change.join);
  }

  public void startup(SQLUtils sqlUtils, int startingBinding) throws InterFactoryConnectionDoesNotWorkException, DBNotReachableException {
    callOtherNodeOverAQWithConnectGuard(sqlUtils, startingBinding,  "RAC-ConnectGuard-startup", Change.startup);
  }
  
  private void callOtherNodeOverAQWithConnectGuard(SQLUtils sqlUtils, int binding, String cgThreadName, Change change)
      throws InterFactoryConnectionDoesNotWorkException, DBNotReachableException {
    new Thread(new ConnectGuard(CONNECTGUARD_TIMEOUT * 1000), cgThreadName).start();
    boolean calledOtherNodeSuccessfully = false;
    try {
      callOtherNodeOverAQ(sqlUtils, change, binding);
      calledOtherNodeSuccessfully = true;
    } finally {
      if (!calledOtherNodeSuccessfully) {
        //connectguard canceln, weil anderer knoten nicht da ist oder ein anderer fehler passiert ist
        ConnectGuard cg = connectGuard;
        if (cg != null) {
          cg.cancel();
        } else {
          logger.warn("RAC ConnectGuard could not be cancelled.");
        }
      }
    }
  }

  public void connect(SQLUtils sqlUtils, int connectingBinding) throws InterFactoryConnectionDoesNotWorkException, DBNotReachableException {
    callOtherNodeOverAQ(sqlUtils, Change.connect, connectingBinding);
  }
  
  public void waiting(SQLUtils sqlUtils, int waitingBinding) throws InterFactoryConnectionDoesNotWorkException, DBNotReachableException {
    callOtherNodeOverAQ(sqlUtils, Change.waiting, waitingBinding);
  }

  public void disconnect(SQLUtils sqlUtils, int quittingBinding) {
    try {
      callOtherNodeOverAQ(sqlUtils, Change.disconnectRequest, quittingBinding);
    } catch (DBNotReachableException e) {
      logger.warn("disconnect could not be communicated over aq", e);
    } catch (InterFactoryConnectionDoesNotWorkException e) {
      logger.warn("disconnect could not be communicated over aq", e);
    }
  }

  public void shutdown(String cause) {
    running = false;
    if( logger.isInfoEnabled() ) {
      logger.info("Shutdown of "+listenerThread.getName()+" due to "+cause);
    }
    try {
      if (callableStatement != null) {
        logger.info("cancel callableStatement");
        callableStatement.cancel();
      }
    } catch (SQLException e) {
      if (logger.isInfoEnabled()) {
        logger.info("Ignored exception " + e.getMessage(), e);
      }
    }

    try {
      listenerThread.join(DEQUEUE_TIMEOUT * 1000);
    } catch (InterruptedException e) {
      logger.info("shutdown interrupted ", e);
    }

  }

  public boolean isShutdown() {
    return ! running;
  }

  private class ConnectGuard implements Runnable {

    private volatile long waitingRefreshed;
    private long timeout;
    private volatile boolean canceled;

    public ConnectGuard(long timeout) {
      this.timeout = timeout;
      waitingRefreshed = System.currentTimeMillis();
      canceled = false;
      connectGuard = this;
    }

    public void cancel() {
      logger.info("ConnectGuard canceled");
      this.canceled = true;
      synchronized( this ) {
        this.notify();
      }
    }

    public void run() {
      logger.info("Started ConnectGuard");
      long now = System.currentTimeMillis();
      while (!canceled && waitingRefreshed + timeout > now) {
        try {
          synchronized (this) {
            if (!canceled) { //kann in der zwischenzeit gecanceled worden sein
              this.wait(waitingRefreshed + timeout - now);
            }
          }
        }
        catch (InterruptedException e) {
          //Exception ingorieren: dann halt kürzer warten
        }
        now = System.currentTimeMillis();
      }
      if( ! canceled ) {
        //Der erwartete CONNECT kam nun nach dem Timeout nicht. 
        //Es kann davon ausgegangen werden, dass der andere Knoten verstorben ist, 
        //daher wird nun ein Übergang nach DISCONNECTED_MASTER versucht.

        if (logger.isInfoEnabled()) {
          logger.info("No connect with other node after " + timeout
              + " ms timeout. Assuming other node is dead and switching to DISCONNECTED_MASTER");
        }
        clusterProvider.changeClusterState(ClusterState.DISCONNECTED_MASTER);
      }
      connectGuard = null;
      logger.info("ConnectGuard finished");
    }
    
    public void keepWaiting() {
      waitingRefreshed = System.currentTimeMillis();
    }
    
  }
  
  
}
