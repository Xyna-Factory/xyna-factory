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

package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;



import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.concurrent.JoinedExecutor;
import com.gip.xyna.utils.db.ConnectionFactory;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionCouldNotBeClosedException;
import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.IConnectionFactory;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.pool.ConnectionPoolParameterImpl;
import com.gip.xyna.utils.db.pool.FastValidationStrategy;
import com.gip.xyna.utils.db.utils.RepeatedExceptionCheck;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterConnectionException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterInitializationException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidConnectionParametersForClusterProviderException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStartParametersForClusterProviderException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterInformation;
import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.ClusterAlgorithmAbstract.ClusterSetupRowsForUpdate;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.DefaultSQLUtilsLogger.DBProblemHandler;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.RemoteInterfaceForClusterStateChangesImplAQ.Change;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



/**
 * Cluster von Knoten, die über eine Oracle-Datenbank kommunizieren.<br>
 * Das createCluster legt eine Datenbank-Tabelle an, über die das Status-Management abgewickelt wird. Alle weiteren
 * hinzugefügten Knoten tragen sich in dieser Tabelle ein.
 */
public class OracleRACClusterProvider implements ClusterProvider, RemoteInterfaceForClusterStateChanges, DBProblemHandler {

  protected static final Logger logger = CentralFactoryLogging.getLogger(OracleRACClusterProvider.class);

  public static final long SHUTDOWN_CONNECTION_POOL_TIMEOUT_SECONDS = 30;

  public static final int INITIAL_BINDING = 1;
  public static final String TYPENAME = OracleRACClusterProvider.class.getSimpleName();
  private static final int CONNECTION_POOL_SIZE = 3;

  private static final String PARAMETER_DESCRIPTION = "4 parameters expected: user, password, "
      + "url ((eg jdbc:oracle:thin:@//10.0.10.1:1521/xyna), timeout(ms).";

  public static final XynaPropertyInt RAC_RETRY_ATTEMPTS = new XynaPropertyInt("xyna.xfmg.xcs.db_retry_attempts", 1);
  public static final XynaPropertyDuration RAC_RETRY_WAIT = new XynaPropertyDuration("xyna.xfmg.xcs.db_retry_wait", "10 s");
  protected String poolName = "OracleRACClusterProvider";
  

  private boolean initialized = false;
  protected OracleRACClusterProviderConfiguration config;
  private ConnectionPool pool;
  private DBConnectionData dbdata;
  protected RemoteInterfaceForClusterStateChangesImplAQ interconnect;

  private volatile ClusterStateChangeHandler changeHandler;
  private volatile ClusterAlgorithm clusterAlgorithm;
  private volatile ClusterState localState;
  private AtomicBoolean shutdown = new AtomicBoolean(false);
  private volatile Connector connector;
  private RepeatedExceptionCheck getStateInternalRepeatedExceptionCheck = new RepeatedExceptionCheck();
  private AtomicBoolean dbNotReachable = new AtomicBoolean(false);
  private GetState getState;
  private AtomicInteger connectionCreationCounter = new AtomicInteger(0);

  public OracleRACClusterProvider() {
  }


  public String getTypeName() {
    return TYPENAME;
  }

  
  
  protected IConnectionFactory createConnectionFactory(final DBConnectionData dbdata) {
    return new IConnectionFactory() {
      public Connection createNewConnection() {
        try {
          connectionCreationCounter.incrementAndGet();
          return dbdata.createConnection();
        } catch (Exception e) {
          throw new RuntimeException("Failed to create connection "+connectionCreationCounter.get()+": "+e.getMessage(), e);
        }
      }

      public void markConnection(Connection con, String clientInfo) {
        ConnectionFactory.markConnection(con, clientInfo);
      }
    };
  }


  private void initInternally() throws DBNotReachableException {

    int tsek = (int) config.getTimeout() / 1000;
    RemoteInterfaceForClusterStateChangesImplAQ.ANSWER_TIMEOUT = tsek;
    RemoteInterfaceForClusterStateChangesImplAQ.DEQUEUE_TIMEOUT = tsek;
    RemoteInterfaceForClusterStateChangesImplAQ.CONNECTGUARD_TIMEOUT = 2 * tsek;
    
    String user = getClass().getSimpleName();
    RAC_RETRY_ATTEMPTS.registerDependency(UserType.XynaFactory, user);
    RAC_RETRY_WAIT.registerDependency(UserType.XynaFactory, user);
    
    dbdata =
        DBConnectionData.newDBConnectionData()
            .user(config.getUser()).password(config.getPassword()).url(config.getUrl())
            .connectTimeoutInSeconds(tsek) 
            .socketTimeoutInSeconds(tsek * 4) //muss größer sein als RemoteInterfaceForClusterStateChangesImplAQ.DEQUEUE_TIMEOUT
            //.classLoaderToLoadDriver(OracleRACClusterProvider.class.getClassLoader()) only works for AppCL == URL_CL, no longer the case for >= Java9
            .build();
    try {
      
      ConnectionPoolParameterImpl.Builder cppBuilder = new ConnectionPoolParameterImpl.Builder();
      cppBuilder.identifiedBy(poolName).
          connectionFactory( createConnectionFactory(dbdata) ).
          size(CONNECTION_POOL_SIZE).
          maxRetries(0). //nicht zuviele Retries, die Connection wiederzuerlangen -> schnellere Erkennung, dass DB nicht erreichbar ist
          noConnectionAvailableReasonDetector( new NoConnectionAvailableReasonDetectorImpl() ).
          validationStrategy(FastValidationStrategy.validateAlwaysWithTimeout_dontRebuildConnectionAfterFailedValidation(config.getTimeout()) ).
          build();
      
      pool = ConnectionPool.getInstance( cppBuilder.build() );
      if (logger.isTraceEnabled()) {
        logger.trace("got pool: " + pool);
      }
      
      getState = new GetState();
      
    } catch( Exception e ) {
      throw new DBNotReachableException(e);
    }
        
    interconnect = new RemoteInterfaceForClusterStateChangesImplAQ(pool, config.getUser(), this, config.getTimeout(), this, this);
  }
  
  private void checkAndCreateTableClusterSetupRow(SQLUtils sqlUtils, boolean lazyCreateTable) {
    Integer rowCount =
      sqlUtils.queryInt("select count(*) from user_tables where table_name=?",
        new Parameter(XynaClusterSetup.CLUSTER_SETUP_TABLE_NAME.toUpperCase()));
    boolean tableExists = rowCount != null && rowCount > 0;
    if (!tableExists) {
      if (!lazyCreateTable) {
        throw new RuntimeException("table <" + XynaClusterSetup.CLUSTER_SETUP_TABLE_NAME + "> does not exist");
      }
      // String nicht extrahiert, weil das fast nie ein Client braucht und der String gespart werden kann
      StringBuilder createTableStatement =
        new StringBuilder("CREATE TABLE ").append(XynaClusterSetup.CLUSTER_SETUP_TABLE_NAME).append(" (\n");
      createTableStatement.append(XynaClusterSetup.COL_BINDING).append(" NUMBER(32) NOT NULL,\n");
      createTableStatement.append(XynaClusterSetup.COL_STATE).append(" VARCHAR2(256) NOT NULL,\n");
      createTableStatement.append(XynaClusterSetup.COL_IS_ONLINE).append(" NUMBER(1) NOT NULL,\n");
      createTableStatement.append("CONSTRAINT binding_pk PRIMARY KEY (" + XynaClusterSetup.COL_BINDING + ")\n");
      createTableStatement.append(")");
      sqlUtils.executeDDL(createTableStatement.toString(), new Parameter());
      sqlUtils.commit();
    }
  }

  private void checkInitialized() {
    if (!initialized) {
      throw new RuntimeException(TYPENAME + " not initialized");
    }
  }

  public String getNodeConnectionParameterInformation() {
    return PARAMETER_DESCRIPTION;
  }

  public int getLocalBinding() {
    return config.getBinding();
  }

  public long createCluster(String[] startParameters) throws XFMG_InvalidStartParametersForClusterProviderException,
      XFMG_ClusterInitializationException {
    try {
      initStorable();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    config = createConfig(startParameters);
     
    SQLUtils sqlUtils;
    try {
      initInternally();
      sqlUtils = interconnect.createSqlUtils("createCluster");
    } catch (DBNotReachableException e) {
      throw new XFMG_ClusterInitializationException(TYPENAME, e);
    }
    try {
      try {
        checkAndCreateTableClusterSetupRow(sqlUtils,true);

        config.setBinding(INITIAL_BINDING);

        clusterAlgorithm = new ClusterAlgorithmSingleNode();
        localState = clusterAlgorithm.createCluster(sqlUtils, interconnect, config.getBinding());

        sqlUtils.commit();
        changeState( localState, "createCluster");
         
      } finally {
        sqlUtils.closeConnection();
      }
    } catch( SQLRuntimeException e ) {
      throw new XFMG_ClusterInitializationException(TYPENAME,e);
    }
    persistConfig();
    initialized = true;
    return config.getId();
  }

  public long joinCluster(String[] startParameters) throws XFMG_InvalidConnectionParametersForClusterProviderException,
  XFMG_ClusterConnectionException {
    try {
      initStorable();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    try {
      config = createConfig(startParameters);
    }
    catch (XFMG_InvalidStartParametersForClusterProviderException e) {
      throw new XFMG_InvalidConnectionParametersForClusterProviderException(TYPENAME,e);
    }
    
    SQLUtils sqlUtils; 
    try {
      initInternally();
      sqlUtils = interconnect.createSqlUtils("joinCluster");
    } catch (DBNotReachableException e) {
      throw new XFMG_ClusterConnectionException(e);
    }
    
    try {
      try {
        checkAndCreateTableClusterSetupRow(sqlUtils,true);

        // es sollte schon Einträge geben (sonst Inkonsistenz):
        
        ClusterSetupRowsForUpdate rows = new ClusterSetupRowsForUpdate(sqlUtils);
        int cnt = rows.size();
        if (cnt == 0 ) {
          throw new IllegalStateException("Central cluster setup table is empty, cannot join.");
        } else if (cnt == 1) {
          clusterAlgorithm = new ClusterAlgorithmTwoNodes();
        } else if (cnt > 1) {
          clusterAlgorithm = new ClusterAlgorithmMultiNodes();
        }
        
        config.setBinding( rows.getMaxBinding() +1 ); //neues Binding erstellen
        rows.setOwnBinding(config.getBinding());
        
        localState = clusterAlgorithm.join(sqlUtils, rows, config.getBinding(), interconnect );
       
        sqlUtils.commit(); //sollte eigentlich schon in clusterAlgorithm.join(...) passiert sein
        changeState( localState, "joinCluster");
       
      } finally {
        sqlUtils.closeConnection();
      }
    } catch( SQLRuntimeException e ) {
      throw new XFMG_ClusterConnectionException(e);
    }
    persistConfig();
    initialized = true;
    return config.getId();

  }


  public void restoreClusterConnect() {
    SQLUtils sqlUtils = null;
    try {
      sqlUtils = interconnect.createSqlUtils("restoreClusterConnect");
      ClusterSetupRowsForUpdate rows = new ClusterSetupRowsForUpdate(sqlUtils, config.getBinding());
      localState = clusterAlgorithm.restoreConnect(sqlUtils, rows, interconnect);
      sqlUtils.commit();
      changeState( localState, "restoreClusterConnect");
    } catch (DBNotReachableException e) {
      dbNotReachable();
    } finally {
      if (sqlUtils != null) {
        sqlUtils.closeConnection();
      }
    }
  }


  public void restoreClusterPrepare(long id) throws XFMG_ClusterInitializationException {
    try {
      initStorable();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    restoreConfig(id);
    
    SQLUtils sqlUtils = null;
    try {
      initInternally();
      sqlUtils = interconnect.createSqlUtils("restoreClusterPrepare");
    } catch (DBNotReachableException e) {
      throw new XFMG_ClusterInitializationException(TYPENAME, e);
    }
    try {
      try {
        checkAndCreateTableClusterSetupRow(sqlUtils,false);
        
        ClusterSetupRowsForUpdate rows = new ClusterSetupRowsForUpdate(sqlUtils,config.getBinding());
        clusterAlgorithm = rows.checkAndGetClusterAlgorithm();
        localState = clusterAlgorithm.restorePrepare(sqlUtils, rows, interconnect);
        
        sqlUtils.commit();
      } finally {
        sqlUtils.closeConnection();
      }
    } catch( SQLRuntimeException e ) {
      //dies hätte nun nicht passieren dürfen: eine unerwartete Exception ist bei der Benutzung der DB aufgetreten.
      throw new XFMG_ClusterInitializationException(TYPENAME, e);
    }
    
    initialized = true;
  }

  //package private für Tests
  void initStorable() throws PersistenceLayerException {
    ODSImpl.getInstance().registerStorable(OracleRACClusterProviderConfiguration.class);
  }


  public void setClusterStateChangeHandler(ClusterStateChangeHandler cscHandler) {
    checkInitialized();
    this.changeHandler = cscHandler;
  }


  public void disconnect() { //ist eigentlich ein shutdown()

    if( logger.isTraceEnabled() ) {
      logger.trace( "disconnect called from ", new Exception() );
    }
    
    checkInitialized();

    if( ! shutdown.compareAndSet( false, true ) ) {
      //disconnect wurde bereits erfolgreich gerufen, daher nichts mehr zu tun
      logger.info( "disconnect was already called, skipping disconnect");
      return;
    }
    
    try {
      if (! dbNotReachable.get() ) {
        SQLUtils sqlUtils;
        try {
          sqlUtils = interconnect.createSqlUtils("disconnect");
        } catch (DBNotReachableException e) {
          //Es kann keine connection hergestellt werden. Das Retry wird innen drin schon durchgeführt.
          //Hier kann nichts mehr getan werden.
          logger.warn("Failed to open connection while trying to shutdown", e);
          dbNotReachable();
          //localState = ClusterState.DISCONNECTED_SLAVE;
          //changeState( localState, "disconnect");
          return;
        }
  
        try {
          ClusterSetupRowsForUpdate rows = new ClusterSetupRowsForUpdate(sqlUtils, config.getBinding());
          clusterAlgorithm = rows.checkAndGetClusterAlgorithm();
          localState = clusterAlgorithm.shutdown(sqlUtils, rows, interconnect);
          sqlUtils.commit();
          
          changeState( localState, "disconnect");
        } catch( SQLRuntimeException e ) {
          logger.warn("Failed to write disconnected state to db", e);
          dbNotReachable();
        } finally {
          sqlUtils.closeConnection();
        }
      }
      interconnect.shutdown("disconnect");//Beenden des Listeners
      
    } finally {  
      // always shut down the use connection pool
      try {
        // timeout is measured in ms
        if (dbNotReachable.get()) {
          try {
            ConnectionPool.removePool(pool, true, -1);
          } catch (ConnectionCouldNotBeClosedException e1) {
            logger.error("Failed to shutdown connection pool forcefully", e1);
          }
        } else {
          ConnectionPool.removePool(pool, false, SHUTDOWN_CONNECTION_POOL_TIMEOUT_SECONDS * 1000);
        }
      } catch (ConnectionCouldNotBeClosedException e) {
        logger.warn("Failed to shutdown forcefully within " + SHUTDOWN_CONNECTION_POOL_TIMEOUT_SECONDS
            + " seconds. Forcing closedown...", e);
        try {
          ConnectionPool.removePool(pool, true, -1);
        } catch (ConnectionCouldNotBeClosedException e1) {
          logger.error("Failed to shutdown connection pool forcefully", e1);
        }
      }
      
      Connector c = connector;
      if( c != null ) {
        c.cancel();
      }
    } 

  }


  public ClusterInformation getInformation() {
    ClusterInformation ci = new ClusterInformation(getLocalBinding(), TYPENAME);
    ci.setOwnNodeInformation("binding "+getLocalBinding());
    ci.setExtendedInformation( ci.getOwnNodeInformation() );
    ci.setClusterState(getState());
    return ci;
  }

  public boolean isConnected() {

    checkInitialized();

    SQLUtils sqlUtils = null;
    try {
      sqlUtils = interconnect.createSqlUtils("isConnected");
      
      ClusterSetupRowsForUpdate rows = new ClusterSetupRowsForUpdate(sqlUtils,config.getBinding());
      if ( rows.getOwn() == null ) {
        throw new RuntimeException("Could not obtain own state from DB.");
      } else {
        return ! rows.getOwn().getState().isDisconnected();
      }
      
    } catch (DBNotReachableException e) {
      logger.warn("Failed to create connection to DB, assuming disconnect", e);
      return false;
    } finally {
      if( sqlUtils != null ) {
        sqlUtils.closeConnection();
      }
    }
  }

  public void changeClusterState(ClusterState newState) {
    if( logger.isTraceEnabled() ) {
      logger.trace( "changeClusterState to "+newState+" called from ", new Exception() );
    }
    
    if( newState == null ) {
      throw new IllegalArgumentException("newState is null");
    }
    
    if( shutdown.get() ) {
      //disconnect wurde bereits gerufen, der ConnectionPool funktioniert nicht mehr
      logger.info( "disconnect was already called, skipping changeClusterState");
      return;
    }
    
    checkInitialized();
    if( localState != newState ) {
      if( dbNotReachable.get() ) {
        logger.info( "could not change clusterState "+localState+"->"+newState+": dbNotReachable");
        return; 
      } else {
        changeClusterStateInternal(newState);
      }
    } else {
      logger.info("omitted clusterState change "+localState+"->"+newState);
    }
    
  }

  private void changeClusterStateInternal(ClusterState newState) {
    
    SQLUtils sqlUtils;
    try {
      sqlUtils = interconnect.createSqlUtils("changeClusterState");
    } catch (DBNotReachableException e) {
      logger.error("Could not get connection to DB", e);
      dbNotReachable();
      return;
    }
    try { 
      ClusterSetupRowsForUpdate rows = new ClusterSetupRowsForUpdate(sqlUtils,config.getBinding());
      clusterAlgorithm = rows.checkAndGetClusterAlgorithm();
      
      localState = clusterAlgorithm.changeClusterState(sqlUtils, rows, interconnect, newState);
      
      sqlUtils.commit();
      changeState( localState, "changeClusterState");
      
    } finally {
      sqlUtils.closeConnection();
    }
  }


  public String getStartParameterInformation() {
    return PARAMETER_DESCRIPTION;
  }
  
  
  
  private class GetState extends JoinedExecutor<ClusterState> {

    //@Override
    protected ClusterState executeInternal() {
      try {
        ClusterState newState = getStateInternal();
        if( newState != localState ) {
          logger.info("getState expected "+localState+", read "+newState);
          localState = newState;
          changeState(localState, "getState");
        }
      } catch (RuntimeException e) {
        localState = ClusterState.DISCONNECTED_SLAVE;
        throw e;
      } catch (Error e) {
        localState = ClusterState.DISCONNECTED_SLAVE;
        throw e;
      }
      return localState;
    }
    
  }
  
  //private RepeatedExceptionCheck repeatedExceptionCheck = new RepeatedExceptionCheck(true);
  public ClusterState getState() {
    if( logger.isTraceEnabled() ) {
      logger.trace( "getState called from ", new Exception() );
    }
    /*
    boolean dup = repeatedExceptionCheck.checkRepeated(new Exception());
    if( dup ) {
      logger.info( "getState called again "+ repeatedExceptionCheck.getRepeationCount()+", localState="+localState );
    } else {
      logger.info( "getState called from ", repeatedExceptionCheck.getLastThrowable() );
    }*/
    
    if( shutdown.get() ) {
      //disconnect wurde bereits gerufen, der ConnectionPool funktioniert nicht mehr
      logger.info( "disconnect was already called, skipping db-read");
      return localState;
    } else if (dbNotReachable.get()) {
      logger.info( "DB is marked as not reachable, skipping db-read");
      return localState;
    }
    
    checkInitialized();

    try {
      ClusterState state = getState.execute();
      //logger.info( "read state "+state+", localState="+localState );
      return state;
    } catch (InterruptedException e) {
      throw new RuntimeException(e); 
    }
    
  }
  
  
  private ClusterState getStateInternal() {
    getStateInternalRepeatedExceptionCheck.clear();
    int maxRetryAttempts = Math.max(0, RAC_RETRY_ATTEMPTS.get() );
    
    Exception lastException = null;
    for( int retry = -1; retry<maxRetryAttempts; ++retry ) {
      if( retry >= 0 ) {
        try {
          Thread.sleep(RAC_RETRY_WAIT.getMillis());
        } catch (InterruptedException e) {
          //dann halt kürzer warten
        }
      }
      
      try {
        return getStateInternalFromDB();
      } catch ( Exception e) {
        lastException = e;
      }
      Pair<String,Exception> log = logLastException( retry, lastException );
      if( log.getSecond() == null ) {
        logger.warn( log.getFirst() );
      } else {
        logger.warn( log.getFirst(), log.getSecond() );
      }
    }
    logger.error("Could not read cluster state after "+maxRetryAttempts+" retries: "+lastException.getMessage() );
    return ClusterState.DISCONNECTED_SLAVE;
  }
  
  
  private Pair<String,Exception> logLastException(int retry, Exception exception) {
    StringBuilder msg = new StringBuilder();
    if( retry >= 0 ) {
      msg.append( "Retry ").append(retry+1).append(": ");
    }
    if( exception instanceof DBNotReachableException ) {
      msg.append("Could not get connection to DB");
    } else {
      msg.append("Severe failure with db connection");
    }
    boolean repeated = getStateInternalRepeatedExceptionCheck.checkRepeated(exception);
    if( repeated ) {
      msg.append(" repeated ").append(getStateInternalRepeatedExceptionCheck.getRepeationCount() ).append(" times");
      return Pair.of(msg.toString(), null);
    } else {
      return Pair.of( msg.toString(), exception);
    }
  }


  private ClusterState getStateInternalFromDB() throws DBNotReachableException {
    SQLUtils sqlUtils = null;
    try {
      sqlUtils = interconnect.createSqlUtils("getState");
      List<XynaClusterSetup> row =
          sqlUtils.query(SQLStrings.SELECT_OWN_BINDING_FOR_UPDATE_SQL, new Parameter(config.getBinding()),
            XynaClusterSetup.reader);
      dbNotReachable.set(false); 
      if (row.size() != 1) {
        throw new RuntimeException("Missing own entry in cluster setup table");
      }
      return row.get(0).getState();
    } finally {
      if( sqlUtils != null ) {
        sqlUtils.closeConnection();
      }
    }
  }
  
  
  

  public void leaveCluster() {

    checkInitialized();
    
    SQLUtils sqlUtils;
    try {
      sqlUtils = interconnect.createSqlUtils("leaveCluster");
    } catch (DBNotReachableException e) {
      throw new RuntimeException("Could not get connection to DB", e);
    }

    try {
      try {
        ClusterSetupRowsForUpdate rows = new ClusterSetupRowsForUpdate(sqlUtils,config.getBinding());
        if (rows.getOwn() == null ) {
          throw new RuntimeException("Missing own binding <" + config.getBinding()
            + "> in central cluster setup table.");
        }
        
        clusterAlgorithm.leaveCluster(sqlUtils, rows);
        
        // FIXME SPS interconnect.leave(config.getBinding()) aufrufen

        sqlUtils.commit();
      } finally {
        sqlUtils.closeConnection();
      }
    } catch (SQLRuntimeException e) {
      logger.error("Severe failure with db connection: "+e.getMessage());
      dbNotReachable();
      throw e;
    }
    
  }

  public List<Integer> getAllBindingsIncludingLocal() throws XNWH_RetryTransactionException {
    SQLUtils sqlUtils;
    try {
      sqlUtils = interconnect.createSqlUtils("getAllBindingsIncludingLocal");
    } catch (DBNotReachableException e) {
      dbNotReachable();
      throw new XNWH_RetryTransactionException(e);
    }
    try {
      try {
        return readBinding(sqlUtils);
      } finally {
        sqlUtils.closeConnection();
      }
    } catch (SQLRuntimeException e) {
      logger.error("Severe failure with db connection: "+e.getMessage());
      try {
        logger.info("retry getAllBindingsIncludingLocal");
        sqlUtils = interconnect.createSqlUtils("getAllBindingsIncludingLocal");
      } catch (DBNotReachableException e2) {
        dbNotReachable();
        throw new XNWH_RetryTransactionException(e2);
      }
      try {
        try {
          return readBinding(sqlUtils);
        } finally {
          sqlUtils.closeConnection();
        }
      } catch (SQLRuntimeException e2) {
        logger.error("Severe failure with db connection: "+e2.getMessage());
        dbNotReachable();
        throw new XNWH_RetryTransactionException(e);
      }
    }
  }

  private List<Integer> readBinding(SQLUtils sqlUtils) {
    List<Integer> result =
      sqlUtils.query(SQLStrings.SELECT_ONLY_BINDINGS_SQL, new Parameter(), new ResultSetReader<Integer>() {
        public Integer read(ResultSet rs) throws SQLException {
          return rs.getInt(XynaClusterSetup.COL_BINDING);
        }
      });
    return result;
  }

  
  
  /*
   * vom anderen factory-knoten aufgerufen werden => diese methoden werden aufgerufen. 
   */
  
  
  
  public void join(SQLUtils sqlUtils, int joinedBinding) {
    if( logger.isTraceEnabled() ) {
      logger.trace("join("+joinedBinding+"), ownBinding="+ config.getBinding()+", changeHandler="+changeHandler);
    }
    //der andere Knoten möchte sich connecten, wird ihm das erlaubt?
    //Diese Entscheidung kann länger dauern, daher in eigenem Thread
    new Thread( new Connector(Change.join), "RAC-Connector-join" ).start();
  }
  public void startup(SQLUtils sqlUtils, int startingBinding) {
    if( logger.isTraceEnabled() ) {
      logger.trace("startup"+startingBinding+"), ownBinding="+ config.getBinding()+", changeHandler="+changeHandler);
    }
    //der andere Knoten möchte sich connecten, wird ihm das erlaubt?
    //Diese Entscheidung kann länger dauern, daher in eigenem Thread
    new Thread( new Connector(Change.startup), "RAC-Connector-startup" ).start();
  }
  public void connect(SQLUtils sqlUtils, int connectingBinding) {
    if( logger.isTraceEnabled() ) {
      logger.trace("connect"+connectingBinding+"), ownBinding="+ config.getBinding()+", changeHandler="+changeHandler);
    }
    refreshStateAccordingToSetupTable(Change.connect);
  }
  public void waiting(SQLUtils sqlUtils, int waitingBinding) {
    if( logger.isTraceEnabled() ) {
      logger.trace("waiting"+waitingBinding+"), ownBinding="+ config.getBinding()+", changeHandler="+changeHandler);
    }
    logger.info( "waiting");
  }
  public void disconnect(SQLUtils sqlUtils, int quittingBinding) {
    if( logger.isTraceEnabled() ) {
      logger.trace("disconnect"+quittingBinding+"), ownBinding="+ config.getBinding()+", changeHandler="+changeHandler);
    }
    refreshStateAccordingToSetupTable(Change.disconnectRequest);
  }
    
  private void refreshStateAccordingToSetupTable(Change change) {
    localState = getStateInternal();
    if (logger.isDebugEnabled()) {
      logger.debug("Got notified of remote " + change + ", stateChange to " + localState + " as requested by other node.");
    }
    if( changeHandler != null ) {
      changeHandler.onChange(localState);
    }
  }

  private class Connector implements Runnable {

    private volatile boolean canceled = false;
    private Change change;
    
    public Connector(Change change) {
      this.change = change;
    }

    public void run() {
      logger.info("Started Connector");
      connector = this;
      SQLUtils sqlUtils = null;
      try {
        sqlUtils = interconnect.createSqlUtils("connector.connect");
        if( changeHandler != null ) {
          waitForReadyToConnect(sqlUtils);
        }
        if( !canceled ) {
          connect(sqlUtils);
        }
      } catch (DBNotReachableException e) {
        dbNotReachable();
      } finally {
        if (sqlUtils != null) {
          sqlUtils.closeConnection();
        }
      }
    }
    
    private void connect(SQLUtils sqlUtils) {
      ClusterSetupRowsForUpdate rows = new ClusterSetupRowsForUpdate(sqlUtils, config.getBinding());
      if( change == Change.join ) {
        clusterAlgorithm = rows.checkAndGetClusterAlgorithm(); 
      }
      localState = clusterAlgorithm.connect(sqlUtils, rows, interconnect);
      sqlUtils.commit();
      changeState( localState, "connect");
    }
 
    private void waitForReadyToConnect(SQLUtils sqlUtils) {

      boolean ready = false;
      while( ! ( ready || canceled) ) {
        ready = changeHandler.isReadyForChange(ClusterState.CONNECTED);
        if( ! ready ) {
          logger.trace( "waiting for isReadyForChange CONNECTED");
          try {
            interconnect.waiting(sqlUtils, config.getBinding());
          } catch( Exception e ) {
            logger.warn("Could not notify other node to wait for isReadyForChange CONNECTED", e);
            if( e instanceof DBNotReachableException ) {
              dbNotReachable();
              canceled = true;
              return;
            } else {
              //weiterprobieren
            }
          }
          try {  
            synchronized ( this ) {
              this.wait(500);
            }
          } catch (InterruptedException e) {
            //Ignorieren, dann ist Wartezeit halt kürzer
          }
        }
      }
      logger.debug( "isReadyForChange CONNECTED");
    }

    /**
     * 
     */
    public void readyForStateChange() {
      synchronized (this) {
        this.notify();
      }
    }
    
    public void cancel() {
      canceled = true;
      synchronized (this) {
        this.notify();
      }
    }

  }
  
  public void readyForStateChange() {
    Connector c = connector;
    if( c != null ) {
      c.readyForStateChange();
    }  
  }
 
  /**
   * @param startParameters
   * @return
   * @throws XFMG_InvalidStartParametersForClusterProviderException 
   */
  private OracleRACClusterProviderConfiguration createConfig(String[] startParameters) throws XFMG_InvalidStartParametersForClusterProviderException {
    if (startParameters.length != 4) {
      throw new IllegalArgumentException(PARAMETER_DESCRIPTION);
    }

    String username = startParameters[0];
    String password = startParameters[1];
    String jdbcConnectString = startParameters[2];
    long timeout;
    try {
      timeout = Integer.valueOf(startParameters[3]);
    } catch (NumberFormatException e) {
      throw new XFMG_InvalidStartParametersForClusterProviderException("Invalid timeout value: '" + startParameters[4]
          + "'");
    }

    OracleRACClusterProviderConfiguration parameter = new OracleRACClusterProviderConfiguration();
    parameter.setUser(username);
    parameter.setPassword(password);
    parameter.setUrl(jdbcConnectString);
    parameter.setTimeout(timeout);
    return parameter;
  }
  
  /**
   * 
   */
  protected void persistConfig() {
    try {
      ODSConnection hisCon = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
      try {
        config.setId(XynaFactory.getInstance().getIDGenerator().getUniqueId());
        hisCon.persistObject(config);
        hisCon.commit();
      } finally {
        hisCon.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not store new cluster information", e);
    }
  }
  
  /**
   * @param id
   * @throws XFMG_ClusterInitializationException 
   */
  protected void restoreConfig(long id) throws XFMG_ClusterInitializationException {
    config = new OracleRACClusterProviderConfiguration(id);
    // first restore the config from local data
    try {
      ODSConnection hisCon = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
      try {
        try {
          hisCon.queryOneRow(config);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          // this is a configuration inconsistency and should not happen
          config = null;
          throw new RuntimeException("Specified id <" + id + "> is unknown.", e);
        }
        if (config.getBinding() < INITIAL_BINDING) {
          throw new IllegalStateException("Local binding is invalid: <" + config.getBinding() + ">");
        }
      } finally {
        hisCon.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      throw new XFMG_ClusterInitializationException(TYPENAME, e);
    }
  }
  
  public void dbNotReachable() {
    ClusterState state = getState();
    if( state == ClusterState.DISCONNECTED_SLAVE ) {
      if( dbNotReachable.compareAndSet(false, true) ) {
        if (initialized) {
          if (logger.isInfoEnabled()) {
            logger.info("DB is not reachable, notifying listeners and assuming own state <"+localState+">");
          }
          if( interconnect != null ) {
            interconnect.shutdown("db not reachable");
          }
          if (this.changeHandler != null) {
            this.changeHandler.onChange(localState);
          }
        } else {
          logger.info("DB is not reachable");
          //ansonsten kann hier nichts weiter getan werden, über die mit setException(...) gesetzte Exception 
          //sollte aber die Initialisierung abgebrochen werden
        }
      }
    }
  }
  
  public void setException(Exception exception) {
    //TODO
  }


  public void shutdown(String cause) {
    throw new RuntimeException("not supported"); //sollte nie aufgerufen werden
  }


  public void checkInterconnect() {
    if( shutdown.get() ) {
      //disconnect wurde bereits gerufen, der ConnectionPool funktioniert nicht mehr
      logger.info( "disconnect was already called, skipping checkInterconnect");
      return;
    }
    
    //getState macht eine DB-Anfrage intern, d.h die DB wird auf jeden Fall gestestet.
    //getState hat hier den Vorteil, dass mehrere Threads gleichzeitig die Anfrage 
    //starten können, ohne dass mehrere Connections gebraucht werden. Außerdem müssen die
    //später (während der Wartezeit des ersten Threads) hinzukommenden Threads nicht mehr 
    //ganz so lange warten.
    getState();
  }
  
 
  
  /**
   * @param clusterState
   */
  protected void changeState(ClusterState clusterState, String method) {
    if( changeHandler != null ) {
      if( logger.isDebugEnabled() ) {
        logger.debug( method +" finished; calling onChange for clusterState="+clusterState);
      }
      changeHandler.onChange(clusterState);
    } else {
      if( logger.isDebugEnabled() ) {
        logger.debug( method +" finished with clusterState = "+clusterState );
      }
    }
  }
  
  @Override
  public String toString() {
    return "OracleRACClusterProvider("
      +localState+", "
      +(clusterAlgorithm==null?"null":clusterAlgorithm.getClass().getSimpleName())
      +")";
  }

  public boolean isShutdown() {
    return shutdown.get();
  }


  public boolean fastCheckIsMediumReachable() {
    return !dbNotReachable.get();
  }

}
