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
package com.gip.xyna.utils.db;

import java.lang.Thread.State;
import java.lang.ref.PhantomReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException.Reason;
import com.gip.xyna.utils.db.pool.ConnectionBuildStrategy;
import com.gip.xyna.utils.db.pool.ConnectionPoolParameterImpl;
import com.gip.xyna.utils.db.pool.ConnectionPoolParameterImpl.Builder;
import com.gip.xyna.utils.db.pool.PoolEntryConnection;
import com.gip.xyna.utils.db.pool.RetryStrategy;
import com.gip.xyna.utils.db.pool.ValidationStrategy;
import com.gip.xyna.utils.db.utils.RepeatedExceptionCheck;

/**
 * TODO: 
 * - in den connectioninformations sichtbar machen, dass eine connection gerade in der validierung befindlich ist
 * - statt "synchronized (object)", mit einem lock arbeiten, um timeouts zu gew�hrleisten. "synchronized (object)"
 *   hat in lasttests sehr lange auf das lock gewartet, evtl l�nger als der gew�nschte timeout.
 * - priorit�ten f�r getConnection?
 * - keepalive-thread besser als anderweitig regelm�ssiges connection-verify?
 * - connections erst aufmachen, wenn ben�tigt
 *  * dietrich ist �brigens der ansicht, dass connectionpools mit dynamischer gr��e gar nicht so gut sind.
    weil es dann ganz schnell passiert, dass man w�hrend der tests keine probleme hat, aber dann zur laufzeit ganz unvorbereitet (jaja) engp�sse mit den connections.
    ich denke trotzdem, dass etwas mehr configurationsm�glichkeiten bei den connectionpools schon gut w�ren...
 * - connections schliessen, wenn sie lange nicht ben�tigt wurden
 * - connections zur�ckholen, wenn sie lange inaktiv sind, aber in benutzung (zb vergessenes close) => daraus resultierend: 
 *      wie geht man damit um, dass dann mehrere threads das gleiche connection objekt kennen?
 *      eventuell an threads binden. falls anderer thread zugreifen will => fehler.
 * - statistiken: 
 *    - wie ist die zeitliche ausnutzung der connections in benutzung? (zb 90% der zeit zwischen get und commit wurde die connection eigtl nicht ben�tigt)
 *    - wie lange haben threads auf connection gewartet
 *    - wie lange warten gerade wartende threads? (zb maxwert)
 *    - maximale anzahl von threads, die warten mussten
 *    - durchschnitt wartender threads in den letzten x minuten
 *    - statistken �ber die zeit zwischen holen der connection und zur�ckgeben
 * - methode public static ConnectionPool getInstance(final String sqlConnectString, int poolSize) rauswerfen
 * - methode zum freigeben von connections
 * - {@link PhantomReference} nutzen: http://www.javalobby.org/java/forums/t16520.html
 * - keine operationen auf connection erlauben, falls sie dem pool geh�rt
 *  
 * Bemerkung:
 * Durch die Verwendung von ConnectionFactory besteht derzeit eine Abh�ngigkeit von Oracle (nur noch wegen 
 * Abw�rtskompatibilit�t in oben genannter zu entfernender Methode)
 * 
 * Verwendung ohne SQLUtils: 
 * ConnectionPool pool = ConnectionPool.getInstance(...); //lazy initialization
 * Connection con = pool.getConnection("clientInfo"); //holt connection setzt client info
 * try {
 *   ...
 *   con.commit();
 * } finally {
 *   con.close(); //zur�ckgeben an pool
 * }

 * Verwendung mit SQLUtils: 
 * ConnectionPool pool = ConnectionPool.getInstance(...); //lazy initialization
 * SQLUtils sqlUtils = pool.getConnection("clientInfo", logger); //holt connection, setzt clientinfo, setzt default sqlutilslogger
 * try {
 *   ...
 *   sqlUtils.commit(); 
 * } finally {
 *   sqlUtils.closeConnection(); //zur�ckgeben an pool
 * }
 */
public class ConnectionPool {

  private final LinkedList<PoolEntryConnection> listFree = new LinkedList<PoolEntryConnection>();
  private final LinkedList<PoolEntryConnection> listUsed = new LinkedList<PoolEntryConnection>();
  private final LinkedList<Thread> waitingThreads = new LinkedList<Thread>();
  private static final Logger logger = Logger.getLogger(ConnectionPool.class);
  private final Object listLock;
  private volatile boolean closingDown = false;
  private static final HashMap<String, ConnectionPool> instances = new HashMap<String, ConnectionPool>();
  private static final long DEFAULT_TIMEOUT = 60000; //1 minute
  private volatile boolean collectStatistics = true;

  private int maxSizeForSQLStatistics = DEFAULT_MAXSIZE_SQLSTATISTICS;
  public static final int DEFAULT_MAXSIZE_SQLSTATISTICS = 1000;
  private ConcurrentMap<String, Integer> storedSQLs = new ConcurrentHashMap<String, Integer>();
  private final RepeatedExceptionCheck pooledConInitializeForUseRepeatedExceptionCheck = new RepeatedExceptionCheck();
  private final static RepeatedExceptionCheck globalPooledConInitializeForUseRepeatedExceptionCheck = new RepeatedExceptionCheck();
  
  private volatile com.gip.xyna.utils.db.pool.ConnectionPoolParameter connectionPoolParameter;
  private volatile boolean checkThreadUsage = false;
  private volatile PoolState state = PoolState.Initializing;
  
  private AtomicLong configurationCount = new AtomicLong(0);
  
  
  
  public enum PoolState {
    Initializing, Running, Closing, Closed, Broken;
  }
  
  //Wrapper, um die Ausf�hrungsweise des Cleanups zu steuern
  public interface CleanupWrapper {
    public void registerCleanup(Runnable cleanup);
  }

  //Z�hler f�r registrierte CleanupWrapper, damit immer nur der neueste ausgef�hrt wird
  private static final AtomicInteger marker = new AtomicInteger(0);

  //als Default wird ein "normaler" Shutdown-Hook verwendet, um das Cleanup durchzuf�hren
  static {
    setCleanupWrapper(new CleanupWrapper() {
      public void registerCleanup(Runnable hook) {
        Runtime.getRuntime().addShutdownHook(new Thread(hook));
      }
    }); 
  }

  /**
   * Setzt einen neuen CleanupWrapper, der dann das Schlie�en der ConnectionPools ausf�hrt.
   * @param newCleanup
   */
  public static void setCleanupWrapper(CleanupWrapper newCleanup) {
    final int markerCopy = marker.incrementAndGet();
    newCleanup.registerCleanup(new Runnable() {

      public void run() {
         if (markerCopy != ConnectionPool.marker.get()) {
           return; //damit werden alte wrapper invalidiert
         }
         
         //connections schliessen
         logger.debug("shutting down all pools");
         synchronized (instances) {
           for (ConnectionPool pool : instances.values()) {
             try {
               pool.closedown(true, -1);
             } catch (ConnectionCouldNotBeClosedException e) {
               logger.error("error shutting down ConnectionPool "+pool.getId(), e);
             }
           }
         }
      }
    });
  } 
  
  
  private ConnectionPool( com.gip.xyna.utils.db.pool.ConnectionPoolParameter connectionPoolParameter ) throws NoConnectionAvailableException {
    this.connectionPoolParameter = connectionPoolParameter;
    
    logger.debug("Initializing Connection Pool...");
    listLock = new Object();
    
    boolean succeeded = false;
    try {
      synchronized (listLock) {
        for (int i = 0; i < connectionPoolParameter.getSize(); i++) {
          if (logger.isDebugEnabled()) {
            logger.debug("[ConnectionPool] Opening Connection " + i);
          }
          listFree.add(new PoolEntryConnection(createConnectionWithRetries(), this));
        }
      }
      logger.debug("Connection Pool ready...");
      succeeded = true;
    } finally {
      if( ! succeeded ) {
        try {
          closedown(true, -1);
        } catch (ConnectionCouldNotBeClosedException e) {
          logger.warn("ConnectionPool "+connectionPoolParameter.getName()+" could not be closed after failed initialization");
        }
      } else {
        state = PoolState.Running;
      }
    }
    
  }
  
  private Connection createConnectionWithRetries() throws NoConnectionAvailableException {
    Connection con = connectionPoolParameter.getRetryStrategy().createConnectionWithRetries(
        connectionPoolParameter.getConnectionBuildStrategy(),
        connectionPoolParameter.getValidationStrategy(),
        connectionPoolParameter.getNoConnectionAvailableReasonDetector()
        );
    return con;
  }

  /**
   * f�r abw�rtskompatibilt�t
   * @param sqlConnectString
   * @param poolSize
   * @return
   * @deprecated
   */
  public static ConnectionPool getInstance(final String sqlConnectString, int poolSize) {
    String conPoolId = sqlConnectString;
    synchronized (instances) {
      if (!instances.containsKey(conPoolId)) {
        try {
          ConnectionPoolParameterImpl cppi = new Builder().identifiedBy(conPoolId).
              connectionFactory(new IConnectionFactory() {

                public Connection createNewConnection() {
                  return ConnectionFactory.getConnection(sqlConnectString, "");
                }

                public void markConnection(Connection con, String clientInfo) {
                  ConnectionFactory.markConnection(con, clientInfo);
                }
                
              }).
              size(poolSize).build();
          instances.put(conPoolId, new ConnectionPool(cppi) );
        } catch (NoConnectionAvailableException e) {
          throw new RuntimeException(e);
        }
      }
      return instances.get(conPoolId);
    }
  }

  /**
   * f�r abw�rtskompatibilt�t
   * @param cf
   * @param conPoolId
   * @param poolSize
   * @return
   * @deprecated
   */
  public static ConnectionPool getInstance(IConnectionFactory cf, String conPoolId, int poolSize) {
    synchronized (instances) {
      if (!instances.containsKey(conPoolId)) {
        try {
          ConnectionPoolParameterImpl cppi = new Builder().identifiedBy(conPoolId).
              connectionFactory(cf).
              size(poolSize).build();
          instances.put(conPoolId, new ConnectionPool(cppi) );
        } catch (NoConnectionAvailableException e) {
          throw new RuntimeException(e);
        }
      }
      return instances.get(conPoolId);
    }
  }
  
   
  public static ConnectionPool getInstance(com.gip.xyna.utils.db.pool.ConnectionPoolParameter connectionPoolParameter ) throws NoConnectionAvailableException {
    synchronized (instances) {
      String conPoolId = connectionPoolParameter.getName();
      if (!instances.containsKey( conPoolId )) {
        instances.put(conPoolId, new ConnectionPool(connectionPoolParameter));
      }
      return instances.get(conPoolId);
    }
  }

  /**
   * @deprecated use com.gip.xyna.utils.db.pool.ConnectionPoolParameterImpl or own implementation of use com.gip.xyna.utils.db.pool.ConnectionPoolParameter
   */
  public static class ConnectionPoolParameter extends com.gip.xyna.utils.db.pool.ConnectionPoolParameterImpl {
    
    public ConnectionPoolParameter(IConnectionFactory cf, String name, int poolSize, 
        NoConnectionAvailableReasonDetector reasonDetector,
        int maxRetries, long checkInterval) {
      super( new Builder().connectionFactory(cf).identifiedBy(name).size(poolSize).
          noConnectionAvailableReasonDetector(reasonDetector).maxRetries(maxRetries).
          validationInterval(checkInterval).
          build() );
    }
    public ConnectionPoolParameter(com.gip.xyna.utils.db.pool.ConnectionPoolParameterImpl cpp) {
      super(cpp);
    }
    public static ConnectionPoolParameterBuilder create(String conPoolId) {
      ConnectionPoolParameterBuilder cppb = new ConnectionPoolParameterBuilder();
      cppb.identifiedBy(conPoolId);
      return cppb;
    }
  }
  /**
   * @deprecated use com.gip.xyna.utils.db.pool.ConnectionPoolParameterImpl.Builder
   */
  public static class ConnectionPoolParameterBuilder extends ConnectionPoolParameter.AbstractBuilder<ConnectionPoolParameter> { 

    @Override
    protected ConnectionPoolParameter buildInternal() {
      return new ConnectionPoolParameter(buildConnectionPoolParameterImpl());
    }
  }
  

  /**
   * pooled sqlUtils mit default SQLUtilsLogger, der als verursachende Zeile der SQL Statements
   * die Klasse loggt, in der sqlUtils.query(...) o.�. aufgerufen wird.
   * Zum Zur�ckgeben der Connection muss nur sqlUtils.closeConnection() aufgerufen werden
   * @param clientInfo
   * @return
   * @throws SQLException
   */
  public SQLUtils getSQLUtils(long timeout, String clientInfo,
    final Logger clientLogger) throws SQLException {
    SQLUtilsLogger logger = null;
    if( clientLogger != null ) {
      logger = new SQLUtilsLogger() {
        public void logSQL(String sql) {
          internalLog(Level.DEBUG, sql, null);
        }

        public void logException(Exception e) {
          internalLog(Level.WARN, e.getMessage(), e);
        }

        /**
         * Sorgt auf interessante Weise f�r das richtige Setzen des aufrufenden Zeile: (keine Zeile dieser Datei, sondern der Aufrufer)
         * siehe http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/Category.html
         * http://marc.info/?l=log4j-user&m=99859247618691&w=2
         * That's why you need to provide the fully-qualified classname. If you
        use the wrapper I showed, you will not have this problem. That is, the
        class and method name of the wrapper caller, not the wrapper itself,
        will be logged. The code which determines the logging method looks one
        past the fully-qualified classname in the callstack.
         * @param level
         * @param str
         * @param t
         */
        private void internalLog(Level level, String str, Throwable t) {
          clientLogger.log(SQLUtils.class.getName(), level, str, t);
        }
      };
    }
    
    SQLUtils sqlUtils = new SQLUtils(getConnection(timeout, clientInfo), logger );
    sqlUtils.setName(clientInfo);
    return sqlUtils;
  }
  
  /**
   * default timeout = 1 minute
   * @param clientInfo
   * @param clientLogger
   * @return
   * @throws SQLException
   */
  public SQLUtils getSQLUtils(String clientInfo, final Logger clientLogger) throws SQLException {
    return getSQLUtils(DEFAULT_TIMEOUT, clientInfo, clientLogger);
  }

  /**
   * default timeout = 1 minute
   * @return
   * @throws SQLException
   */
  public PooledConnection getConnection(String clientInfo) throws SQLException {
    return getConnection(DEFAULT_TIMEOUT, clientInfo);
  }

  /**
   * versucht innerhalb des timeouts (in millisekunden) eine connection aus dem pool zu bekommen.
   * 
   * danach wird die connection validiert und die clientinfo gesetzt. die daf�r ben�tigte zeit (oder falls das lange dauert,
   * weil es netzwerkprobleme gibt), ist nicht im timeout enthalten.
   */
  public PooledConnection getConnection(long timeout, String clientInfo) throws NoConnectionAvailableException {
    if (closingDown) {
      throw new NoConnectionAvailableException(Reason.PoolClosed);
    }
    if (state == PoolState.Broken) {
      throw new NoConnectionAvailableException(Reason.PoolBroken);
    }
    if (logger.isDebugEnabled()) {
      StringBuilder sb =
          new StringBuilder("getCachedConnection(): listFree.size() = ").append(listFree.size())
              .append(", waiting thread count: " + waitingThreads.size());
      logger.debug(sb);
    }
    
    Thread currentThread = Thread.currentThread();
   
    PoolEntryConnection peCon = getConnection(timeout, currentThread);
    
    if (logger.isDebugEnabled()) {
      logger.debug("got connection " + peCon+" now validating and initializing");
    }
    
    try {
      long currentTime = System.currentTimeMillis();
      if( peCon.getInnerConnection() != null ) {
        boolean validated = validateCon(peCon, currentTime);
        if( ! validated ) {
          peCon.rebuild( createConnectionWithRetries() );
        }
      } else {
        peCon.rebuild( createConnectionWithRetries() );
      }
      
      return peCon.initializeForUse(currentThread, currentTime, clientInfo);
    } catch (NoConnectionAvailableException e) {
      returnConnection(peCon);
      throw e;
    }
  }
    
  private PoolEntryConnection getConnection(long timeout, Thread currentThread) throws NoConnectionAvailableException {
    PoolEntryConnection con = null;
    
    // Ist eine Connection frei? Dann nimm sie!
    synchronized (listLock) {
      tryToSaturatePool();
      if (listFree.size() > 0) {
        con = getFreeConnection();
      } else if (isDynamic()) {
        if (logger.isDebugEnabled()) {
          logger.debug("[ConnectionPool] Opening Connection Dynamically");
        }
        try {
          con = new PoolEntryConnection(connectionPoolParameter.getConnectionBuildStrategy().createNewConnection(), this);
        } catch( Exception e ) {
          throw new NoConnectionAvailableException(e, connectionPoolParameter.getNoConnectionAvailableReasonDetector() );
        }
        listUsed.add(con);
      }
    }
    if (con != null) {
      return con; 
    }

    // Keine Connection frei? Dann warte!
    long endTime = System.currentTimeMillis() + timeout;

    //TODO besser w�re es, wenn jeder thread sein eigenes objekt hat, auf dem er wartet. was passiert dann, wenn ein thread mehrere connections will?
    synchronized (waitingThreads) {
      waitingThreads.addLast(currentThread);
    }
    try {
      boolean loggedGotNoConnection = false;
      while (con == null) {
        if (closingDown) {
          throw new NoConnectionAvailableException(Reason.PoolClosed);
        }
        if (!loggedGotNoConnection) {
          loggedGotNoConnection = true;
          if (logger.isInfoEnabled()) {
            logger.info("No free connection in pool "+getId()+", waiting. Waiting thread count: " + waitingThreads.size());
          }
        }
        synchronized (waitingThreads) {
          //inzwischen kann bereits eine connection freigeworden sein
          //deshalb hier nochmal checken, bevor man beginnt zu warten
          if (listFree.size() == 0) {
            long waitTime = endTime - System.currentTimeMillis();
            if (waitTime <= 0) {
              throw new NoConnectionAvailableException(Reason.PoolExhausted,timeout);
            }
            try {
              waitingThreads.wait(waitTime);
            } catch (InterruptedException e1) {
              throw new NoConnectionAvailableException(Reason.PoolExhausted);
            }
            //nur der erste thread darf versuchen sich eine connection zu holen. die anderen m�ssen warten
            if (waitingThreads.indexOf(currentThread) > 0) {
              continue;
            }
          }
        }

        // Aufgeweckt? Dann k�nnte nun eine Connection frei sein... .
        // Falls jemand schneller war: con bleibt null => wieder warten.          
        synchronized (listLock) {
          if (listFree.size() > 0) {
            con = getFreeConnection();
          }
        }
      }
    } finally {
      synchronized (waitingThreads) {
        waitingThreads.remove(currentThread);
        waitingThreads.notifyAll();
      }
    }
    return con;
  }

  /**
   * Validiert die PoolEntryConnection
   * @param peCon
   * @param currentTime
   * @return true, wenn Con nicht validiert oder erfolgreich validert wurde (Con also verwendet werden kann),
   *         false, wenn Validierung fehlgeschlagen ist
   * @throws NoConnectionAvailableException
   */
  private boolean validateCon(PoolEntryConnection peCon, long currentTime) throws NoConnectionAvailableException {
    ValidationStrategy validationStrategy = connectionPoolParameter.getValidationStrategy();
    if( ! validationStrategy.isValidationNecessary(currentTime, peCon.getLastCheck() ) ) {
      return true;
    }
    
    boolean lastCheckOk = peCon.getLastCheckOk();
    
    Exception exception = validationStrategy.validate(peCon.getInnerConnection());
    peCon.setLastCheck( currentTime );

    if( exception == null ) {
      pooledConInitializeForUseRepeatedExceptionCheck.clear();
      globalPooledConInitializeForUseRepeatedExceptionCheck.clear();
      return true;
    } else {
      //check nicht erfolgreich
      StringBuilder sb = new StringBuilder();
      boolean logWithoutException = logValidationException(exception, peCon, sb);
      if( logWithoutException ) {
        logger.warn(sb.toString());
      } else {
        logger.warn(sb.toString(), exception);
      }
      
      peCon.setLastCheckResult(false);
      if( ! validationStrategy.rebuildConnectionAfterFailedValidation() ) {
        if( !lastCheckOk) {
          //Letzter Check war auch schon nicht erfolgreich. Wahrscheinlich sind mittlerweile alle 
          //freien Connections schon einmal als fehlerhaft validiert worden und kommen nun 
          //zum zweiten Mal in die Validierung.
          //Damit dies nicht ewig so weitergeht, m�ssen die Connections neugebaut werden und nicht 
          //mit dem throw einfach nur �bersprungen werden. 
          return false;
        }
        throw new NoConnectionAvailableException( exception, connectionPoolParameter.getNoConnectionAvailableReasonDetector() );
      }
      return false;
    }
  }
  
  private boolean logValidationException(Exception exception, PoolEntryConnection peCon, StringBuilder sb) {
    sb.append("connection ").append(peCon).append(" could not be validated.");
    sb.append(" Recreating connection in pool " ).append(getId()).append(".");
    int repCount = pooledConInitializeForUseRepeatedExceptionCheck.checkRepeationCount( exception );
    int globalRepCount = globalPooledConInitializeForUseRepeatedExceptionCheck.checkRepeationCount( exception );
    
    sb.append(" This happened for the ");
    if( globalRepCount > 0 ) {
      sb.append(globalRepCount+1).append(" time consecutively globally and for the ").append(repCount+1).append(" time in this pool");
    } else if( repCount > 0 ) {
      sb.append(repCount+1).append(" time consecutively in this pool");
    } else {
      sb.append("first time");
    }
    sb.append(" due to \"").append(exception.getMessage()).append("\".");
    return repCount > 0 || globalRepCount > 0;
  }

 

  private PoolEntryConnection getFreeConnection() {
    PoolEntryConnection con = listFree.remove(0);
    listUsed.add(con);
    return con;
  }

  /**
   * wird beim close der pooledconnection aufgerufen
   */
  public void returnConnection(PoolEntryConnection peCon) {
    synchronized (listLock) {
      int usedBefore = listUsed.size();
      int freeBefore = listFree.size();
      if (listUsed.contains(peCon)) {
        listUsed.remove(peCon);
        
        boolean returnConnection = !(isDynamic() || 
                                     listFree.size() + listUsed.size() + 1 > connectionPoolParameter.getSize() ||
                                     peCon.getConfigurationCount() < configurationCount.get());
        if (returnConnection) {
          listFree.add(peCon);
        } else {
          try {
            Connection innerConnection = peCon.getInnerConnection();

            if (innerConnection != null && !peCon.getInnerConnection().isClosed()) {
              innerConnection.close();
              peCon.reset();

              if (logger.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder(60);
                sb.append("closed connection from pool ").append(connectionPoolParameter.getName()).append(". there are ");
                sb.append(listUsed.size()).append(" open connections left in the pool.");
                logger.debug(sb.toString());
              }
            }
          } catch (SQLException e) {
            logger.warn("could not close connection from pool "+getId()+" " + peCon, e);
            //This method does not throw anything so we will keep that this way
            //throw new ConnectionCouldNotBeClosedException(1);
          }
        }
        tryToSaturatePool();
      } else {
        //der pool wurde heruntergefahren oder die connection wurde anderweitig daraus entfernt (zb in recreateConnectionPool)
        logger.info("Connection " + peCon + " could not be returned to pool " + getId() + ".");
        return;
      }

      if (logger.isTraceEnabled()) {
        StringBuilder sb = new StringBuilder(200);
        sb.append("Returned connection to pool: ");
        sb.append(peCon).append(". Counts before: used: ").append(usedBefore);
        sb.append(", free: ").append(freeBefore).append(". Counts after: used: ").append(listUsed.size());
        sb.append(", free: ").append(listFree.size()).append(". Waiting Threads: ").append(waitingThreads.size());
        logger.trace(sb.toString());
      } else if (logger.isDebugEnabled()) {
        logger.debug("Returned connection to pool.");
      }
    }

    synchronized (waitingThreads) {
      if (waitingThreads.size() > 0) {
        if (logger.isTraceEnabled()) {
          logger.trace("A thread is waiting - notifying all threads. WaitingThreads.size(): " + waitingThreads.size());
        }
        waitingThreads.notifyAll();
      }
    }
  }

  public void markAsBroken() {
    state = PoolState.Broken;
  }
  
  public PoolState getState() {
    return state;
  }
  
  /**
   * Siehe closedownPool
   * @param force
   * @param timeout
   * @throws ConnectionCouldNotBeClosedException
   */
  public void close(boolean force, long timeout) throws ConnectionCouldNotBeClosedException {
    if ( closingDown) {
      return;
    }
    closedown(force, timeout);
  }
  
  /**
   * schliesst alle connections des zugeh�rigen pools. auf connections wartende threads werden
   * mit einem fehler beantwortet. entfernt den pool nicht, damit ein erneuter aufruf von
   * ConnectionPool.getInstance(...) keinen neuen Pool erstellt. um den Pool zu entfernen, muss
   * removePool() aufgerufen werden.
   * @param pool
   * @param force falls true, werden auch connections, die gerade in benutzung sind geschlossen.
   * ansonsten wird mit dem �bergebenen timeout versucht auf die connections in benutzung zu
   * warten. sind sie bis dahin nicht zur�ckgegeben, werden sie hart geschlossen.
   * @param timeout milliseconds. will only be used if force=false. cumulative!
   * @throws ConnectionCouldNotBeClosedException falls eine oder mehrere der connections nicht
   * geschlossen werden konnten
   */
  public static void closedownPool(ConnectionPool pool, boolean force,
    long timeout) throws ConnectionCouldNotBeClosedException {
    if (pool == null || pool.closingDown) {
      return;
    }
    pool.closedown(force, timeout);
  }
  
  
  private void closedown(boolean force, long timeout) throws ConnectionCouldNotBeClosedException {
    if( state == PoolState.Running ) {
      state = PoolState.Closing;
    }
    closingDown = true;
    long t = System.currentTimeMillis();
    synchronized (waitingThreads) { //wartende threads unterbrechen
      for (Thread th: waitingThreads) {
        th.interrupt();
      }
      waitingThreads.clear(); //damit man sich sp�ter selbst sorgenfrei hier reinh�ngen kann
    }
    int cntErroneous = 0;
    int lfs = 0;
    int lus = 0;
    synchronized (listLock) {
      lfs = listFree.size();
      lus = listUsed.size();
    }
    PoolEntryConnection con = null;

    int maxThreads = Math.min(20,connectionPoolParameter.getSize()); //maximal 20 Threads verwenden
    maxThreads = Math.max(1, maxThreads); // mindestens einen Thread verwenden
    ExecutorService connectionCloserService = Executors.newFixedThreadPool(maxThreads);

    try {
      ((ThreadPoolExecutor)connectionCloserService).setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
      List<Future<Void>> closings = new ArrayList<Future<Void>>(lfs + lus);
      //connections schliessen
      while (lfs > 0 || lus > 0) {
        if (lfs > 0) {
          synchronized (listLock) {
            con = listFree.removeFirst(); //kann nicht null sein, da kein anderer thread mehr freie connections bekommen kann (closingDown = true)
          }
          logger.debug("removed free connection");
        } else if (force || System.currentTimeMillis() - t > timeout) {

          synchronized (listLock) {
            if (listUsed.size() > 0) {
              con = listUsed.removeFirst();
              logger.debug("removed used connection");
            }
          }

        }
        if (con != null && con.getInnerConnection() != null) {
          closings.add(connectionCloserService.submit( new ConnectionCloser(con.getInnerConnection(), getId()) ) );
          if (logger.isDebugEnabled()) {
            synchronized (listLock) {
              lfs = listFree.size();
              lus = listUsed.size();
            }
            if (logger.isDebugEnabled()) {
              StringBuilder sb = new StringBuilder(60);
              sb.append("closed connection from pool ").append(connectionPoolParameter.getName()).append(". there are ");
              sb.append(lfs + lus).append(" open connections left in the pool.");
              logger.debug(sb.toString());
            }
          }
        } else {
          //lfs == 0, lus > 0, force=false und timeout noch nicht �berschritten => warten
          //es k�nnte allerdings sein, dass seit beginn der whileschleife eine connection frei
          //geworden ist.
          synchronized (listLock) {
            lfs = listFree.size();
          }
          if (lfs == 0) {
            long sleepTime = Math.max(timeout - (System.currentTimeMillis() - t), 1);
            if (sleepTime > 0) {
              synchronized (waitingThreads) { 
                waitingThreads.add(Thread.currentThread());
                try {
                  //bei spurious wakeup wird die whileschleife wiederholt. ok.
                  waitingThreads.wait(sleepTime);
                } catch (InterruptedException e) {
                  //ntbd
                }
              }
            }
          }
        }
        synchronized (listLock) {
          lfs = listFree.size();
          lus = listUsed.size();
        }
      }

      for (Future<Void> future : closings) {
        long maxWait = 200;
        long maxWaitTime = Math.max(timeout - (System.currentTimeMillis() - t), maxWait);
        try {
          future.get(maxWaitTime, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
          if (maxWaitTime > maxWait) {
            logger.warn("could not close connection from pool "+getId()+" in " + maxWaitTime + "ms.");
            cntErroneous++;
          } else {
            logger.warn("could not close connection from pool "+getId()+" in " + maxWaitTime + "ms.");
            //nicht als fehler bewerten, z.b. wenn timeout = -1 ist
          }
        } catch (Throwable closingError) {
          logger.warn("exception while waiting on connections from pool "+getId()+" to close", closingError);
          cntErroneous++;
        }
      }
      synchronized (waitingThreads) { 
        Iterator<Thread> iter = waitingThreads.iterator();
        while (iter.hasNext()) {
          Thread current = iter.next();
          if (current == Thread.currentThread()) {
            iter.remove();
          }
        }
      }
      if (cntErroneous > 0) {
        throw new ConnectionCouldNotBeClosedException(cntErroneous);
      } else {
        if( state == PoolState.Closing ) {
          state = PoolState.Closed;
        }
      }
    } finally {
      connectionCloserService.shutdown();
    }
  }
  
  private static class ConnectionCloser implements Callable<Void> {
    Connection connectionToClose;
    String poolId;
    private static RepeatedExceptionCheck recRollback = new RepeatedExceptionCheck();
    private static RepeatedExceptionCheck recClose = new RepeatedExceptionCheck();
    
    
    public ConnectionCloser(Connection connectionToClose, String poolId) {
      this.connectionToClose = connectionToClose;
      this.poolId = poolId;
    }
    
    public Void call() throws Exception {
      try {
        if ( connectionToClose.isClosed()) {
          return null; //nichts zu tun
        }
        try {
          connectionToClose.rollback();
        } catch( SQLException e ) {
          closeConnectionLog(e, "rollback", recRollback);
        }
        connectionToClose.close();
      } catch (SQLException e) {
        closeConnectionLog(e, "close", recClose);
        throw e;
      }
      return null;
    }

    private void closeConnectionLog(SQLException e, String action, RepeatedExceptionCheck rec) {
      int repCount = rec.checkRepeationCount(e);
      if( repCount >0 ) {
        logger.warn("could not "+action+" connection from pool "+poolId +" again for the "+(repCount)+" time, message: "+e.getMessage() );
      } else {
        logger.warn("could not "+action+" connection from pool "+poolId, e);
      }
    }
    
  }
  
  public boolean restart(boolean force) {
    if (closingDown) {
      closingDown = false;
      recreateAllConnections(force);
      return true;
    } else {
      return false;
    }
  }

  /**
   * falls closedown noch nicht aufgerufen wird, passiert das intern. anschliessend wird der
   * Pool entfernt, so dass ein erneuter Aufruf von ConnectionPool.getInstance(...) einen neuen
   * Pool erzeugt.
   * @param pool
   * @param force
   * @param timeout
   * @throws ConnectionCouldNotBeClosedException siehe closedownPool. bei einem fehler wird der pool trotzdem
   *   entfernt.
   */
  public static void removePool(ConnectionPool pool, boolean force,
    long timeout) throws ConnectionCouldNotBeClosedException {
    if (pool == null) {
      return;
    }
    try {
      if (!pool.closingDown) {
        pool.closedown(force, timeout);
      }
    } finally { //falls closedown einen fehler wirft
      synchronized (instances) {
        instances.remove(pool.connectionPoolParameter.getName());
      }
    }
  }
  
  // access to connectionPoolParameter is synchronized to listlock in all relevant places
  public synchronized void adjustPoolParameter(com.gip.xyna.utils.db.pool.ConnectionPoolParameter newParams,
                                                boolean invalidatesOldConnections, boolean recreateCons) {
    synchronized (listLock) {
      this.connectionPoolParameter = newParams;
      if (invalidatesOldConnections) {
        configurationCount.incrementAndGet();
        while (listFree.size() > 0) { // clear freeList
          PoolEntryConnection con = listFree.poll();
          try {
            con.closeInner();
          } catch (SQLException e) {
            logger.debug("Failed to close connection on modification",e);
          }
        }
      } else {
        while (listFree.size() > 0 &&
               listUsed.size() + listFree.size() > connectionPoolParameter.getSize()) {  // trim freeList
          PoolEntryConnection con = listFree.poll();
          try {
            con.closeInner();
          } catch (SQLException e) {
            logger.debug("Failed to close connection on modification",e);
          }
        }
      }
      if (recreateCons) {
        recreateAllConnections(true);
      } else {
        tryToSaturatePool();
      }
    }
  }
  
  
  private void tryToSaturatePool() {
    while (listFree.size() < connectionPoolParameter.getSize() - listUsed.size()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Opening Connection in " + getId());
      }
      try {
        listFree.add(new PoolEntryConnection(createConnectionWithRetries(), this));
      } catch (NoConnectionAvailableException e) {
        logger.warn("Connection could not be opened in pool " + getId(),e);
        break;
      }
    }
  }
  
  /**
   * unterscheidet sich von einer normalen connection dadurch, dass sie beim close
   * an den pool zur�ckgegeben wird.
   */
  public static class PooledConnection extends WrappedConnection {

    public PooledConnection(PoolEntryConnection con) {
      super(con);
    }

    /**
     * Gibt die Connection zur�ck an den Pool (innere Con ist eine PoolEntryConnection)
     * Eine geschlossene Connection sollte von niemandem weiterverwendet werden, daher wird 
     * die innere Connection genullt. Die danach auftreteneden NPEs sollen eine starke Warnung sein!
     */
    public void close() throws SQLException {
      con.close();
      con = null; //jetzt ist die PooledConnection kaputt. 
    }

    public Connection getInnerConnection() {
      return ((PoolEntryConnection)con).getInnerConnection();
    }
    
    public ConnectionPool getConnectionPool() {
      return ((PoolEntryConnection)con).getPool();
    }
    
    private int lastlevel = -1;

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
      if (level != lastlevel) {
        super.setTransactionIsolation(level);
        lastlevel = level;
      }
    }

    @Override
    public boolean isClosed() throws SQLException {
      if( con == null ) {
        return true;
      }
      return super.isClosed();
    }

    @Override
    public String toString() {
      return "PooledConnection("+con+")";
    }

  }

  public interface NoConnectionAvailableReasonDetector {

    Reason detect(SQLException sqlException);
    
  }
  
  public static class NoConnectionAvailableException extends SQLException {
    private static final long serialVersionUID = 1L;

    public static enum Reason {
      PoolClosed("ConnectionPool was closed down"),
      PoolExhausted("There was no connection availabe after timeout"),
      Timeout("Timeout while openening connection"),
      NetworkUnreachable("Network unreachable"),
      UserOrPasswordInvalid("User/Password invalid"),
      URLInvalid("URL invalid"),
      ConnectionRefused("Connection refused"),
      Other("Other"),
      PoolBroken("Pool is marked as broken");
      
      
      private String message;

      private Reason(String message) {
        this.message = message;
      }
      public String getMessage() {
        return message;
      }
      
    }
    
    private Reason reason;
    
    public NoConnectionAvailableException(Reason reason, long timeout) {
      super( reason.getMessage() +" ("+timeout+" ms)");
      this.reason = reason;
     
    }

    public NoConnectionAvailableException(Reason reason) {
      super( reason.getMessage() );
      this.reason = reason;
    }

    public NoConnectionAvailableException(Reason reason, Exception cause ) {
      super( reason == Reason.Other ? cause.getMessage() : reason.getMessage() );
      this.reason = reason;
      initCause(cause);
    }

    public NoConnectionAvailableException(Exception cause, NoConnectionAvailableReasonDetector detector) {
      this( examineReason(cause, detector ), cause );
    }   

    private static Reason examineReason(Exception cause, NoConnectionAvailableReasonDetector detector ) {
      if( detector == null ) {
        return Reason.Other;
      }
      
      SQLException sqlCause = getSqlCause( cause );
      if( sqlCause == null ) {
        return Reason.Other;
      }
      
      Reason reason = detector.detect( sqlCause );
      if( reason == null ) {
        return Reason.Other; 
      } else {
        return reason;
      }
    }

    private static SQLException getSqlCause(Throwable cause) {
      if( cause == null ) {
        return null;
      }
      if( cause instanceof SQLException ) {
        return (SQLException) cause;
      }
      return getSqlCause( cause.getCause() );
    }

    public Reason getReason() {
      if( reason == null ) {
        reason = Reason.Other; //nur f�r Abw�rts 
      }
      return reason;
    }
    
  }
  
  public static class ConnectionNotInPoolException extends SQLException {
    private static final long serialVersionUID = 1L;

    public ConnectionNotInPoolException() {
      super("Connection does not belong to pool.");
    }

  }
  
  public static class DiscardedConnectionException extends SQLException {
    private static final long serialVersionUID = 1L;

    public DiscardedConnectionException() {
      super("Connection has been discarded.");
    }

  }
  
  public static class ConnectionCouldNotBeClosedException extends SQLException {
    private static final long serialVersionUID = 1L;

    public ConnectionCouldNotBeClosedException(int countOpenConnections) {
      super("Some (" + countOpenConnections + ") Connections could not be closed");
    }
  }

  /**
   * in millisekunden: nach welcher zeit der nicht-benutzung im pool wird eine connection erneut �berpr�ft, ob sie noch g�ltig ist.
   * falls <=0 : �berpr�fung jedes mal
   * @deprecated 
   */
  public void setCheckInterval(long checkInterval) {
    this.connectionPoolParameter.getValidationStrategy().setValidationInterval(checkInterval);
  }
 
  public void storeSQL(String sql) {
    if (storedSQLs != null) {
      if (sql == null) {
        sql = "null";
      }
      //TODO eher als ringbuffer implementieren statt zu clearen. �lteste statistiken fliegen raus
      if (storedSQLs.size() > maxSizeForSQLStatistics) {
        storedSQLs.clear();
      }
      boolean replaced = false;
      do {
        Integer oldVal = storedSQLs.get(sql);
        if (oldVal == null) {
          replaced = (null == storedSQLs.putIfAbsent(sql, 1));
        } else {
          replaced = storedSQLs.replace(sql, oldVal, oldVal + 1);
        }
      } while (!replaced);
    }
  }

  
  /**
   * RetryStrategy legt fest, wieviele Retries gemacht werden sollen, wenn keine valide Connection zur DB neu aufgebuat werden kann
   * @return
   */
  public RetryStrategy getRetryStrategy() {
    return connectionPoolParameter.getRetryStrategy();
  }
  
  public ValidationStrategy getValidationStrategy() {
    return connectionPoolParameter.getValidationStrategy();
  }
  
  /**
   * per default angeschaltet. alle teuren operationen werden damit ausgeschaltet.
   * derzeit beinhaltet das:
   * - lastrollback zeitstempel
   * - lastcommit zeitstempel
   * 
   */
  public void setCollectStatistics(boolean collectStatistics) {
    this.collectStatistics = collectStatistics;
    if (collectStatistics) {
      if (storedSQLs == null) {
        storedSQLs = new ConcurrentHashMap<String, Integer>();
      }
    } else {
      storedSQLs = null;
    }
  }
  
  /**
   * maximale anzahl der f�r statistics gespeicherten sqls.
   * wird diese gr��e �berschritten, werden alle gespeicherten sqls gel�scht.
   * per default auf {@link #DEFAULT_MAXSIZE_SQLSTATISTICS} 
   */
  public void setMaxSizeForSQLStatistics(int maxSize) {
    this.maxSizeForSQLStatistics = maxSize;
  }
  
  public static class ConnectionInformation {
    private long lastCheck;
    private boolean lastCheckOk;
    private String lastSQL;
    private boolean inUse;
    private long aquiredLast;
    private long lastCommit;
    private long lastRollback;
    private long cntUsed; //wie oft benutzt
    private ThreadInformation currentThread;
    private StackTraceElement[] stackTraceWhenThreadGotConnection;

    public ConnectionInformation(long lastCheck, boolean inUse, long aquiredLast, long lastCommit, long lastRollback, long cntUsed,
                                  boolean lastCheckOk, String lastSQL, Thread currentThread,
                                  StackTraceElement[] stackTraceWhenThreadGotConnection) {
      if (currentThread != null) {
        this.currentThread = new ThreadInformation(currentThread);
      }
      this.lastCheck = lastCheck;
      this.inUse = inUse;
      this.aquiredLast = aquiredLast;
      this.lastCommit = lastCommit;
      this.lastRollback = lastRollback;
      this.cntUsed = cntUsed;
      this.lastCheckOk = lastCheckOk;
      this.lastSQL = lastSQL;
      this.stackTraceWhenThreadGotConnection = stackTraceWhenThreadGotConnection;
    }
    
    public StackTraceElement[] getStackTraceWhenThreadGotConnection() {
      return stackTraceWhenThreadGotConnection;
    }
    
    public ThreadInformation getCurrentThread() {
      return currentThread;
    }
    
    public long getLastCheck() {
      return lastCheck;
    }
    
    public boolean isLastCheckOk() {
      return lastCheckOk;
    }
    
    public String getLastSQL() {
      return lastSQL;
    }
    
    public boolean isInUse() {
      return inUse;
    }

    
    public long getAquiredLast() {
      return aquiredLast;
    }

    
    public long getLastCommit() {
      return lastCommit;
    }

    
    public long getLastRollback() {
      return lastRollback;
    }
    
    public long getCntUsed() {
      return cntUsed;
    }
    
  }
  
  /**
   * ist ein snapshot zu der zeit der abfrage ohne synchronisierung, kann deshalb auch informationen
   * anzeigen, die schon nichts mehr mit einern connection zu tun haben.
   */
  public static class ThreadInformation {
    private String name;
    private StackTraceElement[] stacktrace;
    private State state;
    private int priority;
    private long id;
    
    private ThreadInformation(Thread t) {
      stacktrace = t.getStackTrace();
      name = t.getName();
      state = t.getState();
      priority = t.getPriority();
      id = t.getId();
    }
    
    public StackTraceElement[] getStackTrace() {
      return stacktrace;
    }    
    
    public String getName() {
      return name;
    }
    
    public State getState() {
      return state;
    }
    
    public int getPriority() {
      return priority;
    }
    
    public long getId() {
      return id;
    }
    
  }
  
  /**
   * diese informationen sind unter umst�nden nicht konsistent. zb kann es passieren, 
   * dass zwischen dem ermitteln eines attributs einer connection und einem anderen 
   * sich die daten drastisch �ndern. zb ist der aktive thread ggfs schon an dem close 
   * vorbei, trotzdem ist die connection laut connectioninfo noch nicht closed.
   */
  public ConnectionInformation[] getConnectionStatistics() {
    List<PoolEntryConnection> freeConnections;
    List<PoolEntryConnection> usedConnections;
    synchronized (listLock) {
      freeConnections = new ArrayList<PoolEntryConnection>(listFree);
      usedConnections = new ArrayList<PoolEntryConnection>(listUsed);
    }
    ConnectionInformation[] conInfos = new ConnectionInformation[freeConnections.size() + usedConnections.size()];
    for (int i = 0; i < freeConnections.size(); i++) {
      conInfos[i] = freeConnections.get(i).getConnectionInformation();
    }
    for (int i = 0; i < usedConnections.size(); i++) {
      conInfos[i + freeConnections.size()] = usedConnections.get(i).getConnectionInformation();
    }
    return conInfos;
  }


  public ThreadInformation[] getWaitingThreadInformation() {
    List<Thread> threads;
    synchronized (waitingThreads) {
      threads = new ArrayList<Thread>(waitingThreads);
    }
    ThreadInformation[] ti = new ThreadInformation[threads.size()];
    for (int i = 0; i<threads.size(); i++) {
      ti[i] = new ThreadInformation(threads.get(i));
    }
    return ti;
  }
  
  /**
   * statistiken, wie oft sqls ausgef�hrt wurden.
   */
  public Map<String, Integer> getSQLStatistics() {
    Map<String, Integer> m = storedSQLs;
    if (m == null) {
      return null;
    }
    return new HashMap<String, Integer>(m);
  }
  
  /**
   * falls @param closeConnectionsInUseImmediately = true, werden:
   * - alle freien und alle aktiven connection geschlossen 
   * - alle connections im pool durch neue connections ersetzt
   * das f�hrt dann dazu, dass jeder zuk�nftig anfragende (oder derzeit wartende) thread eine neue funktionierende connection bekommt. die bestehenden
   * threads die eine aktive connection haben, werden vermutlich in einen fehler laufen, ausser sie m�ssen nur noch ein close() durchf�hren, dann
   * gibt es keinen fehler.
   * - wenn fehler beim schliessen der connections auftreten, werden trotzdem alle connections neu erstellt.
   * 
   * falls @param closeConnectionsInUseImmediately = false, werden:
   * - alle freien connections geschlossen
   * - alle (freien und aktiven) connections aus dem pool entfernt und durch neue ersetzt
   * - aktive connections erst geschlossen, wenn der jeweilige thread das close() aufruft.
   * vorr�bergehend sind damit maximal 2*connectionPoolSize offene connections vorhanden.
   * 
   * fehler beim schliessen bestehender connections werden nur geloggt, fehler beim erstellen neuer connections f�hren zum abbruch und
   * werden weitergeworfen.
   */
  public void recreateAllConnections(boolean closeConnectionsInUseImmediately) {
    //TODO ein modus w�re noch sch�n, der nur immediately die connections schliesst, wenn sie bereits l�ngere zeit h�ngen, bzw, wenn der thread, der die
    //     connection h�lt, innerhalb eines timeouts nicht zur�ckkommt.
    //     usecase: man will die connections eines pools neu erstellen, w�hrend er noch in "normaler" verwendung ist, ohne fehler bei den benutzern
    //     zu erzeugen, die durch das bisherige closeImmediately die connection w�hrend der verwendung geschlossen bekommen k�nnen.
    synchronized (listLock) {
      boolean success = false;

      try {
        //1. behandlung der connections in use
        
        //kopie erstellen, weil die liste in der schleife modifziert wird
        ArrayList<PoolEntryConnection> copyOfListUsed = new ArrayList<PoolEntryConnection>(listUsed);
        for (PoolEntryConnection con : copyOfListUsed) {
          if (closeConnectionsInUseImmediately) {
            if (isDynamic()) {
              try {
                Connection innerConnection = con.getInnerConnection();
                
                if (innerConnection != null) {
                  innerConnection.close();
                }
                
                con.reset();
              } catch (SQLException e) {
                if (logger.isDebugEnabled()) {
                  logger.debug("could not close connection " + con, e);
                }
              }
            } else {
              try {
                con.close();
              } catch (SQLException e) {
                if (logger.isDebugEnabled()) {
                  logger.debug("could not close connection " + con, e);
                }
              }
            }
            //jetzt ist die connection wieder in listfree
          } else {
            //connection markieren, damit beim close() auch die innere connection geschlossen wird
            if (!isDynamic()) {
              con.markToClose();
            }
          }
        }
        
        //2. schliessen von innerconnections
        for (PoolEntryConnection con : listFree) {
          try {
            Connection innerConnection = con.getInnerConnection();
            if (innerConnection != null) {
              innerConnection.close();
            }
          } catch (SQLException e) {
            if (logger.isDebugEnabled()) {
              logger.debug("could not close connection " + con, e);
            }
          }
        }
        success = true;
      } finally {
        if (success || closeConnectionsInUseImmediately) {
          //3. alle connections aus listfree und listused entfernen und durch neue connections ersetzen

          if (!isDynamic()) {
            listUsed.clear();
          }
          
          listFree.clear();
          tryToSaturatePool();
        }
      }
     
    }
    synchronized (waitingThreads) {
      if (waitingThreads.size() > 0) {
        if (logger.isTraceEnabled()) {
          logger.trace("A thread is waiting - notifying all threads. WaitingThreads.size(): " + waitingThreads.size());
        }
        waitingThreads.notifyAll();
      } 
    }
  }
  
  public static ConnectionPool[] getAllRegisteredConnectionPools() {
    synchronized (instances) {
      return instances.values().toArray(new ConnectionPool[]{});
    }
  }
  
  public String getId() {
    return connectionPoolParameter.getName();
  }
  
  
  /**
   * stellt sicher, dass die connection funktionst�chtig ist, indem erstens die innere connection getestet wird, und
   * falls sie geschlossen oder nicht funktionst�chtig ist, neu aufgemacht wird.
   * @param con pooledconnection, die gepr�ft werden soll
   * @throws SQLException
   * @throws ConnectionNotInPoolException falls connection nicht zum pool geh�rt
   * @throws DiscardedConnectionException falls connection von {@link #recreateAllConnections(boolean)} ung�ltig gemacht
   *           wurde
   */
  public void ensureConnectivity(Connection con) throws SQLException {
    if (!(con instanceof PooledConnection)) {
      throw new ConnectionNotInPoolException();
    }
    PooledConnection poolCon = (PooledConnection) con;
    if ( poolCon.con == null ) { //Connection ist wahrscheinlich schon geschlossen worden; Pool kann nicht mehr bestimmt werden
      throw new ConnectionNotInPoolException();
    }
    if ( poolCon.getConnectionPool() != this) {
      throw new ConnectionNotInPoolException();
    }
    
    PoolEntryConnection peCon = (PoolEntryConnection) poolCon.con;
    
    synchronized (listLock) {
      if (!listUsed.contains(peCon)) {
        throw new SQLException("can only refresh connections in use");
      }
    }

    //synchronized, weil das close auch beim {@link ConnectionPool.#recreateAllConnections(boolean)} aufgerufen wird
    synchronized (peCon) {

      if (peCon.isClosed() ) {
        throw new SQLException("Connection is already closed."); //TODO ist das evtl eine runtimeexception?
      }
      if (peCon.markedAsClosed() ) {
        //wurde von recreateAllConnections geschlossen
        throw new DiscardedConnectionException();
      }

      if (peCon.getInnerConnection().isClosed()) {
        peCon.rebuild( createConnectionWithRetries() );
      } else {
        ValidationStrategy validationStrategy = connectionPoolParameter.getValidationStrategy();
        Exception e = validationStrategy.validate(peCon.getInnerConnection() );
        if (e != null) {
          logger.trace("connection could not be validated ", e);
          //alte connection aufr�umen
          try {
            peCon.getInnerConnection().rollback();
          } catch (SQLException ee) {
            if (logger.isTraceEnabled()) {
              logger.trace("Failed to rollback broken connection from pool "+getId()+" during refresh", ee);
            }
          }
          peCon.rebuild( createConnectionWithRetries() );
        }
      }
    }
  }
  
  
  public boolean isClosed() {
    return closingDown;
  }
  
  public boolean isDynamic() {
    return connectionPoolParameter.canDynamicGrow();
  }

  public long getConfigurationCount() {
    return configurationCount.get();
  }

  public ConnectionBuildStrategy getConnectionBuildStrategy() {
    return connectionPoolParameter.getConnectionBuildStrategy();
  }
  
  public boolean isCheckThreadUsage() {
    return checkThreadUsage;
  }

  public boolean isCollectStatistics() {
    return collectStatistics;
  }

  public String getInfo() {
    StringBuilder sb = new StringBuilder();
    sb.append("ConnectionPool(").append(getId()).append(",state=").append(getState()).append(": ");
    sb.append("size=").append(connectionPoolParameter.getSize()).append(",used=").append(listUsed.size()).append(",free=").append(listFree.size());
    sb.append(")");
    return sb.toString();
  }
  
}
