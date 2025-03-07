/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.persistence.mysql;



import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionCouldNotBeClosedException;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.StatementCache;
import com.gip.xyna.utils.db.WrappedConnection;
import com.gip.xyna.utils.db.exception.UnexpectedParameterException;
import com.gip.xyna.utils.db.types.BLOB;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.sql.ZippedBlob;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;
import com.gip.xyna.xnwh.pools.MySQLPoolType;
import com.gip.xyna.xnwh.pools.PoolDefinition;
import com.gip.xyna.xnwh.pools.TypedConnectionPoolParameter;
import com.gip.xyna.xnwh.utils.SQLErrorHandling;
import com.gip.xyna.xnwh.utils.SQLErrorHandlingLogger;
import com.gip.xyna.xnwh.utils.SQLErrorHandlingLogger.SQLErrorHandlingLoggerBuilder;



public class MySQLPersistenceLayer implements PersistenceLayer {

  private static final Logger logger = CentralFactoryLogging.getLogger(MySQLPersistenceLayer.class);
  private static final Pattern NUMBERS_PATTERN = Pattern.compile("^\\d+$");
  @SuppressWarnings("rawtypes")
  private static final Map<String, Class<? extends Storable>> tableToClassMap = new HashMap<String, Class<? extends Storable>>();

  /**
   * falls true, werden alle sql-debugs mit {@link #logger} gemacht, ansonsten mit einem dynamischen logger, der sich
   * aus der caller-class ergibt und dann den namen "xyna.sql.&lt;callerclass&gt;" bekommt.
   */
  private boolean usePersistenceLayerLogger = false;


  static final int MAX_INSERT_RETRY_COUNTER = 3;


  static Map<Class<?>, MySqlType> javaTypeToMySQLType = new HashMap<Class<?>, MySqlType>();


  static {
    javaTypeToMySQLType.put(Boolean.class, MySqlType.BOOLEAN);
    javaTypeToMySQLType.put(Boolean.TYPE, MySqlType.BOOLEAN);
    javaTypeToMySQLType.put(Byte.class, MySqlType.INT);
    javaTypeToMySQLType.put(Byte.TYPE, MySqlType.INT);
    javaTypeToMySQLType.put(byte[].class, MySqlType.VARCHAR);
    javaTypeToMySQLType.put(Short.class, MySqlType.INT);
    javaTypeToMySQLType.put(Short.TYPE, MySqlType.INT);
    javaTypeToMySQLType.put(Integer.class, MySqlType.INT);
    javaTypeToMySQLType.put(Integer.TYPE, MySqlType.INT);
    javaTypeToMySQLType.put(Long.class, MySqlType.BIGINT);
    javaTypeToMySQLType.put(Long.TYPE, MySqlType.BIGINT);
    javaTypeToMySQLType.put(Float.class, MySqlType.FLOAT);
    javaTypeToMySQLType.put(Float.TYPE, MySqlType.FLOAT);
    javaTypeToMySQLType.put(Double.class, MySqlType.DOUBLE);
    javaTypeToMySQLType.put(Double.TYPE, MySqlType.DOUBLE);

    javaTypeToMySQLType.put(String.class, MySqlType.VARCHAR);
    javaTypeToMySQLType.put(StringSerializable.class, MySqlType.VARCHAR);
  }

  private boolean automaticColumnTypeWidening = false;
  private static final String KEY_AUTOMATIC_COLUMN_TYPE_WIDENING = "automaticColumnTypeWidening";
  private static final String KEY_USE_PL_LOGGER = "usePersistenceLayerLogger";
  private static final String KEY_CONNECT_TIMEOUT = "connectTimeout";
  private static final String KEY_SOCKET_TIMEOUT = "socketTimeout";
  private static final String KEY_DURABLE_STATEMENT_CACHE = "durableStatementCache";
  private static final String KEY_ZIPPED_BLOBS = "zippedBlobs";
  private static final String KEY_ENABLE_SCHEMA_LOCKING = "schemaLocking";
  private static final String KEY_SCHEMA_LOCKING_TIMEOUT = "schemaLockingTimeout";
  private static final String KEY_ACCESS_MODE = "accessMode";


  public PersistenceLayerConnection getConnection() throws PersistenceLayerException {
    return new MySQLPersistenceLayerConnection(this);
  }


  public PersistenceLayerConnection getDedicatedConnection() throws PersistenceLayerException {
    return new MySQLPersistenceLayerConnection(this, true);
  }


  public boolean describesSamePhysicalTables(PersistenceLayer plc) {
    if (plc instanceof MySQLPersistenceLayer) {
      MySQLPersistenceLayer mplc = (MySQLPersistenceLayer) plc;
      if (url.equals(mplc.url)) {
        return true;
      }
    }
    return false;
  }


  private String username;
  private String schemaName;
  private String url;
  private int timeout;
  private ConnectionPool pool;
  private ConnectionPool dedicatedPool;
  private Long pliID;

  String getSchemaName() {
    return schemaName;
  }

  ConnectionPool getConnectionPool() {
    return pool;
  }

  ConnectionPool getDedicatedConnectionPool() {
    return dedicatedPool;
  }

  boolean useAutomaticColumnTypeWidening() {
    return automaticColumnTypeWidening;
  }

  void enableAutomaticColumnTypeWidening() {
    automaticColumnTypeWidening = true;
  }

  void setAutomaticColumnTypeWidening(boolean b) {
    automaticColumnTypeWidening = b;
  }

  Long getPersistenceLayerInstanceID() {
    return this.pliID;
  }

  public String[] getParameterInformation() {
    return new String[] {
        "poolname (see listconnectionpools)",
        "timeout(ms)",
        "additional optional parameters are key value pairs (key=value). supported keys are: " + "\n"
            + KEY_AUTOMATIC_COLUMN_TYPE_WIDENING + " (true/false), default: false" + "\n" 
            + KEY_USE_PL_LOGGER+ " (true=>better performance, false=>better configuration possibilities), default: false" + "\n"
            + KEY_DURABLE_STATEMENT_CACHE + " (true/false), default: false" + "\n"
            + KEY_ZIPPED_BLOBS + " (true/false), default: false" + "\n"
            + KEY_ENABLE_SCHEMA_LOCKING + " (true/false), default: false" + "\n"
            + KEY_SCHEMA_LOCKING_TIMEOUT + " (integer), default: " + String.valueOf(Integer.MAX_VALUE) + "\n"
            + KEY_ACCESS_MODE + " (read-data/read-only/read-write), default: read-write"
    };
  }


  public void init(Long pliID, String... args) throws PersistenceLayerException {
    
    if (args == null || args.length < 2) {
      StringBuilder errMsg = new StringBuilder();
      String errMsgs[] = getParameterInformation();
      for (int i = 0; i < errMsgs.length; i++) {
        errMsg.append(errMsgs[i]);
        if (i < errMsgs.length - 1) {
          errMsg.append(", ");
        }
      }
      throw new IllegalArgumentException("At least 3 parameters expected:" + errMsg.toString());
    }
    
    this.pliID = pliID;
    
    ConnectionPoolManagement poolMgmt = ConnectionPoolManagement.getInstance();
    
    PoolDefinition regularPoolDefinition;
    PoolDefinition dedicatedPoolDefinition;
    if (isLegacyCreation(args)) {
      TypedConnectionPoolParameter tcpp = handleLegacyParams(pliID, args);
      try {
        poolMgmt.startAndAddConnectionPool(tcpp);
        tcpp.size(0).name(getDedicatedPoolName(pliID));
        poolMgmt.startAndAddConnectionPool(tcpp);
      } catch (NoConnectionAvailableException e) {
        // mimics behavior of @deprecated getInstance call
        throw new RuntimeException(e);
      }
      regularPoolDefinition = poolMgmt.getConnectionPoolDefinition(getPoolName(pliID));
      dedicatedPoolDefinition = poolMgmt.getConnectionPoolDefinition(getDedicatedPoolName(pliID));
    } else {
      String poolname = handleParams(args);
      regularPoolDefinition = poolMgmt.getConnectionPoolDefinition(poolname);
      if (regularPoolDefinition == null) {
        throw new XNWH_GeneralPersistenceLayerException("Pool '" + poolname + "' for pliID " + String.valueOf(this.pliID) + " does not exist!");
      }
      if (!MySQLPoolType.POOLTYPE_IDENTIFIER.equals(regularPoolDefinition.getType())) {
        // only warn as it might be a custom poolType made for MySQL-PL usage
        logger.warn("PoolType '" + regularPoolDefinition.getType() + "' for pool '" + poolname + "' does not match the expected identifier '" + MySQLPoolType.POOLTYPE_IDENTIFIER + "'!");
      }
      TypedConnectionPoolParameter tcpp = regularPoolDefinition.toCreationParameter();
      tcpp.size(0).name(tcpp.getName() + "_dedicated");
      try {
        poolMgmt.startAndAddConnectionPool(tcpp);
      } catch (NoConnectionAvailableException e) {
        // mimics behavior of @deprecated getInstance call
        throw new RuntimeException("No connection for pool '" + poolname + "' available.", e);
      }
      dedicatedPoolDefinition = poolMgmt.getConnectionPoolDefinition(tcpp.getName());
    }
    
    if (regularPoolDefinition == null) {
      throw new XNWH_GeneralPersistenceLayerException("Pool for pliID " + String.valueOf(this.pliID) + " does not exist!");
    }
    
    url = regularPoolDefinition.getConnectstring();
    username = regularPoolDefinition.getUser();
    
    // TODO echtes Pattern für den connect string benutzen
    int i = url.lastIndexOf("/");
    if (i < 0 || i + 1 == url.length()) {
      throw new XNWH_GeneralPersistenceLayerException("Connect string '" + url + "' for pliID " + String.valueOf(this.pliID) + " must contain a schema name.");
    }
    schemaName = url.substring(i + 1);
    if (schemaName.contains("?")) {
      schemaName = schemaName.substring(0, schemaName.indexOf('?'));
    }

    if (usePersistenceLayerLogger) {
      sqlUtilsLoggerInfo = buildMySQLLogger(logger, Level.INFO);
      sqlUtilsLoggerDebug = buildMySQLLogger(logger, Level.DEBUG);
      sqlUtilsLoggerCreateTable = buildMySQLLogger(logger, Level.DEBUG, ODSImpl.class.getName());
    } else {
      sqlUtilsLoggerInfo = buildMySQLLogger(Level.INFO);
      sqlUtilsLoggerDebug = buildMySQLLogger(Level.DEBUG);
      sqlUtilsLoggerCreateTable = buildMySQLLogger(Level.DEBUG, ODSImpl.class.getName());
    }

    try {
      pool = poolMgmt.getConnectionPool(regularPoolDefinition.getName());
      dedicatedPool = poolMgmt.getConnectionPool(dedicatedPoolDefinition.getName());
    } catch (NoConnectionAvailableException e) {
      // mimics behavior of @deprecated getInstance call
      throw new RuntimeException(e);
    }

    if (logger.isTraceEnabled()) {
      logger.trace("got pool: " + pool);
      logger.trace("got dedicated pool: " + dedicatedPool);
    }

  }


  private String handleParams(String[] args) throws XNWH_GeneralPersistenceLayerException {
    for (int i = 2; i < args.length; i++) {
      String[] keyValue = args[i].split("=");
      if (keyValue.length != 2) {
        throw new XNWH_GeneralPersistenceLayerException("invalid key value pair provided: " + args[i]);
      }
      String key = keyValue[0];
      if (KEY_AUTOMATIC_COLUMN_TYPE_WIDENING.equals(key)) {
        automaticColumnTypeWidening = Boolean.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_AUTOMATIC_COLUMN_TYPE_WIDENING + " to " + automaticColumnTypeWidening);
        }
      } else if (KEY_USE_PL_LOGGER.equals(key)) {
        usePersistenceLayerLogger = Boolean.valueOf(keyValue[1]);
      } else if (key.equals(KEY_DURABLE_STATEMENT_CACHE)) {        
        useDurableStatementCache = Boolean.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_DURABLE_STATEMENT_CACHE + " to " + useDurableStatementCache);
        }
      } else if (key.equals(KEY_ZIPPED_BLOBS)) {
        zippedBlobs = Boolean.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_ZIPPED_BLOBS + " to " + zippedBlobs);
        }
      } else if (key.equals(KEY_ENABLE_SCHEMA_LOCKING)) {
        enableSchmeaLocking = Boolean.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_ENABLE_SCHEMA_LOCKING + " to " + enableSchmeaLocking);
        }
      } else if (key.equals(KEY_SCHEMA_LOCKING_TIMEOUT)) {
        schemaLockingTimeout = Integer.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_SCHEMA_LOCKING_TIMEOUT + " to " + String.valueOf(schemaLockingTimeout));
        }
      } else if (key.equals(KEY_ACCESS_MODE)) {
        accessMode = AccessMode.fromString(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_ACCESS_MODE + " to " + accessMode.toString());
        }
      } else {
        throw new XNWH_GeneralPersistenceLayerException("unknown key: " + key);
      }
    }
    
    timeout = Integer.valueOf(args[1]);
    return args[0];
  }


  
  private TypedConnectionPoolParameter handleLegacyParams(Long pliID, String[] args) throws XNWH_GeneralPersistenceLayerException {
    int connectTimeout = 3;
    int socketTimeout = 0;
    
    for (int i = 5; i < args.length; i++) {
      String[] keyValue = args[i].split("=");
      if (keyValue.length != 2) {
        throw new XNWH_GeneralPersistenceLayerException("invalid key value pair provided: " + args[i]);
      }
      String key = keyValue[0];
      if (KEY_AUTOMATIC_COLUMN_TYPE_WIDENING.equals(key)) {
        setAutomaticColumnTypeWidening(Boolean.valueOf(keyValue[1]));
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_AUTOMATIC_COLUMN_TYPE_WIDENING + " to " + automaticColumnTypeWidening);
        }
      } else if (KEY_USE_PL_LOGGER.equals(key)) {
        usePersistenceLayerLogger = Boolean.valueOf(keyValue[1]);
      } else if (key.equals(KEY_CONNECT_TIMEOUT)) {
        connectTimeout = Integer.valueOf(keyValue[1]);
        if (connectTimeout <= 0) {
          connectTimeout = 60 * 60 * 24 * 365; //1 jahr. besser als sonderbehandlung für 0 unten
        }
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_CONNECT_TIMEOUT + " to " + connectTimeout);
        }
      } else if (key.equals(KEY_SOCKET_TIMEOUT)) {
        socketTimeout = Integer.valueOf(keyValue[1]);
        if (socketTimeout < 0) {
          socketTimeout = 0;
        }
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_SOCKET_TIMEOUT + " to " + socketTimeout);
        }
      } else if (key.equals(KEY_DURABLE_STATEMENT_CACHE)) {        
        useDurableStatementCache = Boolean.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_DURABLE_STATEMENT_CACHE + " to " + useDurableStatementCache);
        }
      } else if (key.equals(KEY_ZIPPED_BLOBS)) {        
        zippedBlobs = Boolean.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_ZIPPED_BLOBS + " to " + zippedBlobs);
        }
      } else {
        throw new XNWH_GeneralPersistenceLayerException("unknown key: " + key);
      }
    }
    
    String username = args[0];
    String password = args[1];
    String url = args[2];
    
    timeout = Integer.valueOf(args[4]);

    String prefixForId = getPoolName(pliID);
    
    TypedConnectionPoolParameter tcpp = new TypedConnectionPoolParameter(MySQLPoolType.POOLTYPE_IDENTIFIER);
    tcpp.name(prefixForId).connectString(url).user(username).password(password).size(Integer.valueOf(args[3]));  
    tcpp.additionalParams( StringParameter.buildObjectMap()
                           .put(MySQLPoolType.CONNECT_TIMEOUT, new Duration(connectTimeout, TimeUnit.SECONDS) )
                           .put(MySQLPoolType.SOCKET_TIMEOUT, new Duration(socketTimeout, TimeUnit.SECONDS) )
                           .build() );
                           
    return tcpp;
  }


  private String getPoolName(Long pliID) {
    return MySQLPersistenceLayer.class.getSimpleName() + "_" + pliID;
  }
  
  
  private String getDedicatedPoolName(Long pliID) {
    return getPoolName(pliID) + "_dedicated";
  }


  private boolean isLegacyCreation(String[] args) {
    if (args.length < 5) {
      return false;
    } else if (NUMBERS_PATTERN.matcher(args[3]).matches() && 
               NUMBERS_PATTERN.matcher(args[4]).matches()) {
      return true;
    } else {
      return false;
    }
  }
  
  private SQLErrorHandlingLogger sqlUtilsLoggerDebug;
  private SQLErrorHandlingLogger sqlUtilsLoggerInfo;
  private SQLErrorHandlingLogger sqlUtilsLoggerCreateTable;

  SQLErrorHandlingLogger getDebugLogger() {
    return sqlUtilsLoggerDebug;
  }
  
  SQLErrorHandlingLogger getCreateTableLogger() {
    return sqlUtilsLoggerCreateTable;
  }

  protected enum AccessMode {
    READ_DATA("read-data"), READ_WRITE("read-write"), READ_ONLY("read-only");

    private final String name;

    AccessMode(final String name) {
      this.name = name;
    }

    public String toString() {
      return this.name;
    };

    public static AccessMode fromString(final String val) {
      if (val == null || val.isEmpty())
        throw new IllegalArgumentException("value can not be empty");

      switch (val) {
        case "read-only":
          return READ_ONLY;
        case "read-write":
          return READ_WRITE;
        case "read-data":
          return READ_DATA;
        default:
          throw new IllegalArgumentException("invalid value: " + val);
      }

    }
  };
  
  //basiert auf den inneren (echten) connections. cache räumt sich automatisch auf, wenn die connections nicht mehr verwendet werden
  //auf die pooledconnection kann man den cache nicht basieren, weil dieser innen seine connection austauschen kann
  //achtung: feature ist für ORACLE nicht einfach zu kopieren.
  private WeakHashMap<Connection, StatementCache> statementCaches = new WeakHashMap<Connection, StatementCache>();
  private boolean useDurableStatementCache = false;
  private boolean zippedBlobs = false;
  private boolean enableSchmeaLocking = false;
  private AccessMode accessMode = AccessMode.READ_WRITE;
  private int schemaLockingTimeout = Integer.MAX_VALUE;
  
  boolean useZippedBlobs() {
    return zippedBlobs;
  }

  boolean useSchemaLocking() {
    return enableSchmeaLocking;
  }

  int getSchemaLockingTimeout() {
    return schemaLockingTimeout;
  }

  AccessMode getAccessMode() {
    return accessMode;
  }

  SQLUtils createSQLUtils(boolean isDedicated) throws PersistenceLayerException {
    try {
      String clientInfo = "xyna mysqlpersistencelayer";
      ConnectionPool cp;

      if (isDedicated) {
        cp = dedicatedPool;
        clientInfo += " dedicated";
      } else {
        cp = pool;
      }

      //FIXME woher kommt die clientinfo? => nur fuer oracle wichtig
      WrappedConnection con = cp.getConnection(timeout, clientInfo);
      Connection innerCon = con.getWrappedConnection();
      SQLUtils sqlUtils = new SQLUtils(con, sqlUtilsLoggerDebug) {

        @SuppressWarnings("deprecation")
        @Override
        protected boolean closeConnection(boolean markConnection) {
          setLogger(sqlUtilsLoggerInfo);
          return super.closeConnection(false);
        }

      };
      sqlUtils.setIncludeResultSetReaderNullElements(false);
      sqlUtilsLoggerInfo.logSQL("OPEN");

      StatementCache sc;
      if (useDurableStatementCache) {
        sc = statementCaches.get(innerCon);

        if (sc == null) {
          synchronized (statementCaches) {
            sc = statementCaches.get(innerCon);
            if (sc == null) {
              sc = new MySQLPLStatementCache();
              statementCaches.put(innerCon, sc);
            }
          }
        }
      } else {
        sc = new MySQLPLStatementCache();
      }
      sqlUtils.setStatementCache(sc);
      return sqlUtils;
    } catch ( NoConnectionAvailableException e) {
      switch( e.getReason() ) {
        case PoolExhausted:
          //Retry ist in jedem Fall sinnvoll
          throw new XNWH_RetryTransactionException(e);
        case ConnectionRefused:
        case Other:  //Ursache nicht entscheidbar //FIXME derzeit immer PoolExhausted oder Other
        case NetworkUnreachable:
        case Timeout:
          //Retries könnten erfolgreich sein
          throw new XNWH_RetryTransactionException(e);
        case PoolClosed:
        case URLInvalid:
        case UserOrPasswordInvalid:
          //Retries sind nicht sinnvoll
          throw new XNWH_GeneralPersistenceLayerException("could not get DB Connection from " + pool.getId(), e);
        default:
          throw new XNWH_GeneralPersistenceLayerException("could not get DB Connection from " + pool.getId(), e);
      }
    } catch( Exception e ) {
      //unerwartete andere Exceptions
      throw new XNWH_GeneralPersistenceLayerException("could not get DB Connection from " + pool.getId(), e);
    }
  }

  private static final XynaPropertyInt maximumIndexNameLenght =
      new XynaPropertyInt("xnwh.persistence.maximumindexnamelength", 64)
            .setDefaultDocumentation(DocumentationLanguage.EN,
              "Maximum amount of characters a indexname may not exceed.");

  private static final XynaPropertyBoolean databaseQueryToAvoidDuplicateIndexes =
      new XynaPropertyBoolean("xnwh.persistence.databasequerytoavoidduplicateindexes", false)
            .setDefaultDocumentation(DocumentationLanguage.EN,
              "Looks in Database tables if the Index is used. If not set a hash value will be appended to all index names that are too long.");
  
  private static final XynaPropertyInt hashCharacterCountToAvoidDuplicateIndexes =
      new XynaPropertyInt("xnwh.persistence.hashcharactercounttoavoidduplicateindexes", 5)
            .setDefaultDocumentation(DocumentationLanguage.EN,
              "If Query to avoid duplicate Indexes is not set this determines how many charactes of a hash value should be appended to avoid duplicate Indexes");

  int getMaximumIndexNameLenght() {
    return maximumIndexNameLenght.get();
  }
  
  boolean useDatabaseQueryToAvoidDuplicateIndexes() {
    return databaseQueryToAvoidDuplicateIndexes.get();
  }
  
  int getHashCharacterCountToAvoidDuplicateIndexes() {
    return hashCharacterCountToAvoidDuplicateIndexes.get();
  }
  
  void closeSQLUtils(SQLUtils sqlUtils) throws PersistenceLayerException {
    if (useDurableStatementCache) {
      sqlUtils.setStatementCache(null); //cache soll überleben (sqlUtils clearen den cache bei closeConnection())
    }
    try {
      sqlUtils.closeConnection();
    } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
      throw new XNWH_RetryTransactionException(e);
    } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
      throw new XNWH_GeneralPersistenceLayerException("problem closing connection", e);
    }
  }

  public String getInformation() {
    return String.format("MySQL Persistence (%s@%s timeout=%s)", username, url, timeout);
  }


  public void shutdown() throws PersistenceLayerException {
    try {
      ConnectionPool.removePool(pool, true, 0);
      ConnectionPool.removePool(dedicatedPool, true, 0);
    } catch (ConnectionCouldNotBeClosedException e) {
      throw new XNWH_GeneralPersistenceLayerException("shutdown of " + MySQLPersistenceLayer.class.getName()
          + " not successful. Some connections could not be closed.", e);
    }
  }


  public Reader getExtendedInformation(String[] arg0) {
    return null;
  }


  private Map<Column, MySQLColumnInfo> columnMap = Collections
      .synchronizedMap(new IdentityHashMap<Column, MySQLColumnInfo>());

  Map<Column, MySQLColumnInfo> getColumnMap() {
        return columnMap;
  }

  @SuppressWarnings("rawtypes")
  static Map<String, Class<? extends Storable>> getTableClassMap() {
    return tableToClassMap; 
  }

  private SQLErrorHandlingLogger buildMySQLLogger(Level level) {
    SQLErrorHandlingLoggerBuilder builder = buildMySQLLogger();
    return builder.build(null, level);
  }
  
  
  private SQLErrorHandlingLogger buildMySQLLogger(Level level, String name) {
    SQLErrorHandlingLoggerBuilder builder = buildMySQLLogger();
    return builder.build(null, level, name);
  }
  
  
  private SQLErrorHandlingLogger buildMySQLLogger(Logger logger, Level level) {
    SQLErrorHandlingLoggerBuilder builder = buildMySQLLogger();
    return builder.build(logger, level);
  }
  
  
  private SQLErrorHandlingLogger buildMySQLLogger(Logger logger, Level level, String name) {
    SQLErrorHandlingLoggerBuilder builder = buildMySQLLogger();
    return builder.build(logger, level, name);
  }
  
  
  private SQLErrorHandlingLoggerBuilder buildMySQLLogger() {
    SQLErrorHandlingLoggerBuilder builder = SQLErrorHandlingLogger.builder();
    builder.handleCode(
          1099, // Table '%s' was locked with a READ lock and can't be updated 
          1156, //Got packets out of order
          1157, //Couldn't uncompress communication packet
          1158, //Got an error reading communication packets
          1159, //Got timeout reading communication packets
          1160, //Got an error writing communication packets
          1161, //Got timeout writing communication packets 
          1192, //Can't execute the given command because you have active locked tables or an active transaction 
          1205, //Lock wait timeout exceeded; try restarting transaction 
          1213, //Deadlock found when trying to get lock; try restarting transaction 
          1223, //Can't execute the query because you have a conflicting read lock 
          1479  //Transaction branch was rolled back: deadlock was detected 
          ).with(SQLErrorHandling.RETRY_TRANSACTION)
           .otherwise(SQLErrorHandling.ERROR);
    return builder;
  }
  
  
  // SQLRuntimeException(2) are only keept around in case they are contained in a serialized StackTrace
  private static class SQLRuntimeException extends RuntimeException {

    public SQLRuntimeException(Exception e) {
      super(e);
    }


    public SQLRuntimeException(String msg, Throwable t) {
      super(msg, t);
    }


    public SQLRuntimeException(String msg) {
      super(msg);
    }


    private static final long serialVersionUID = 4495095171180268007L;

  }
  
  
  @SuppressWarnings("unused")
  private static class SQLRuntimeException2 extends SQLRuntimeException {

    public SQLRuntimeException2(Exception e) {
      super(e);
    }


    public SQLRuntimeException2(String msg, Throwable t) {
      super(msg, t);
    }


    public SQLRuntimeException2(String msg) {
      super(msg);
    }


    private static final long serialVersionUID = 1L;

  }
  
  public PersistenceLayerConnection getConnection(PersistenceLayerConnection shareConnectionPool) throws PersistenceLayerException {
    if (shareConnectionPool instanceof MySQLPersistenceLayerConnection) {
      return new MySQLPersistenceLayerConnection(this, (MySQLPersistenceLayerConnection) shareConnectionPool);
    } else {
      throw new IllegalArgumentException();
    }
  }


  public boolean usesSameConnectionPool(PersistenceLayer plc) {
    if (!(plc instanceof MySQLPersistenceLayer)) {
      return false;
    }
    MySQLPersistenceLayer other = (MySQLPersistenceLayer) plc;
    return other.pool == pool;
  }

  static Column getColumnForPrimaryKey(@SuppressWarnings("rawtypes") Storable s) {
    Column[] cols = Storable.getColumns(s.getClass());
    Persistable p = Storable.getPersistable(s.getClass());
    for (Column col : cols) {
        if (col.name().equals(p.primaryKey())) {
            return col;
        }
    }
    throw new RuntimeException("pk col not found for storable " + s.getClass());
  }  

  <T extends Storable<?>> int getColumnSize(Column col, Class<T> clazz) {
      MySQLColumnInfo colInfo = getColumnMap().get(col);
      if (colInfo == null) {
          // TODO cache befüllen oder sicherstellen, dass dieser fall nicht unerwartet
          // auftritt.
          if (logger.isTraceEnabled()) {
              logger.trace("Column " + col.name() + " of " + Storable.getPersistable(clazz).tableName()
                      + " not found in colInfo cache. (classloader=" + clazz.getClassLoader() + ")");
          }
          if (col.size() > 0) {
              if (col.type() == ColumnType.INHERIT_FROM_JAVA
                      && Storable.getColumn(col, clazz).getType() == byte[].class) {
                  return col.size() * 6 + 1; // wenn nach varchar geschrieben werden soll, ist das relevant.
                                             // darstellung ist dann [b, b2, b3], maximaler verbrauch pro byte ist ",
                                             // -123"
              }
              return col.size();
          } else {
              return XynaProperty.DEFAULT_SIZE_COLUMN_TYPE.get();
          }
      } else {
          if (colInfo.isTypeDependentOnSizeSpecification()) {
              long l = colInfo.getCharLength();
              if (logger.isTraceEnabled()) {
                  logger.trace("size for col " + col.name() + " = " + l);
              }
              if (l > Integer.MAX_VALUE) {
                  return Integer.MAX_VALUE;
              }
              return (int) l;
          } else {
              long l = colInfo.getType().getSize();
              if (logger.isTraceEnabled()) {
                  logger.trace("size for col " + col.name() + " = " + l);
              }
              if (l > Integer.MAX_VALUE) {
                  return Integer.MAX_VALUE;
              }
              return (int) l;
          }
      }
  }

  @SuppressWarnings("rawtypes")
  <T extends Storable> void addToParameter(com.gip.xyna.utils.db.Parameter paras, Column col, Object val,
          T storable) throws PersistenceLayerException {
      if (col.type() == ColumnType.INHERIT_FROM_JAVA) {
          if (val instanceof String) {
              String valAsString = (String) val;
              if (valAsString != null) {
                  int colSize = getColumnSize(col, storable.getClass());
                  if (valAsString.length() > colSize) {
                      logger.warn(
                              "Provided value for column '" + col.name() + "' in table '" + storable.getTableName()
                                      + "' was too long (" + valAsString.length()
                                      + "). It will be shortened to fit into column of size " + colSize + ". value = "
                                      + valAsString);
                      valAsString = valAsString.substring(0, colSize);
                  }
                  paras.addParameter(valAsString);
              }
          } else {
              addToParameterWithArraySupportAsStrings(paras, val);
          }
      } else if (col.type() == ColumnType.BLOBBED_JAVAOBJECT) {
          if (val == null) {
              paras.addParameter(null);
          } else {
              BLOB blob;
              if (useZippedBlobs()) {
                  blob = new ZippedBlob();
              } else {
                  blob = new BLOB(BLOB.UNZIPPED);
              }
              try {
                  storable.serializeByColName(col.name(), val, blob.getOutputStream());
              } catch (IOException e) {
                  throw new XNWH_GeneralPersistenceLayerException("could not serialize object to blob in column "
                          + col.name(), e);
              }
              paras.addParameter(blob);
          }
      } else if (col.type() == ColumnType.BYTEARRAY) {
          BLOB blob = new BLOB(BLOB.UNZIPPED);
          try {
              OutputStream os = blob.getOutputStream();
              os.write((byte[]) val);
              os.flush();
          } catch (IOException e) {
              throw new XNWH_GeneralPersistenceLayerException("could not write bytes to blob in column " + col.name(),
                      e);
          }
          paras.addParameter(blob);
      } else {
          throw new XNWH_UnsupportedPersistenceLayerFeatureException("column type " + col.type().toString());
      }
  }

  @SuppressWarnings("rawtypes")
  <T extends Storable> com.gip.xyna.utils.db.Parameter createParasForInsertAndUpdate(Column[] columns,
          T storable)
          throws PersistenceLayerException {
      com.gip.xyna.utils.db.ExtendedParameter paras = new com.gip.xyna.utils.db.ExtendedParameter();
      for (Column col : columns) {
          Object val = storable.getValueByColName(col);
          addToParameter(paras, col, val, storable);
      }
      return paras;
  }

  static void addToParameterWithArraySupportAsStrings(com.gip.xyna.utils.db.Parameter paras, Object val) {
      try {
          paras.addParameter(val); // erkennt strings, zahlen etc
      } catch (UnexpectedParameterException e) {
          // toString oder analoge repräsentation verwenden
          if (val.getClass().isArray()) {
              Class<?> componentType = val.getClass().getComponentType();
              if (componentType == byte.class) {
                  paras.addParameter(Arrays.toString((byte[]) val));
              } else if (componentType == short.class) {
                  paras.addParameter(Arrays.toString((short[]) val));
              } else if (componentType == int.class) {
                  paras.addParameter(Arrays.toString((int[]) val));
              } else if (componentType == long.class) {
                  paras.addParameter(Arrays.toString((long[]) val));
              } else if (componentType == char.class) {
                  paras.addParameter(Arrays.toString((char[]) val));
              } else if (componentType == float.class) {
                  paras.addParameter(Arrays.toString((float[]) val));
              } else if (componentType == double.class) {
                  paras.addParameter(Arrays.toString((double[]) val));
              } else if (componentType == boolean.class) {
                  paras.addParameter(Arrays.toString((boolean[]) val));
              } else {
                  paras.addParameter(Arrays.toString((Object[]) val));
              }
          } else {
              paras.addParameter(String.valueOf(val));
          }
      }
  }
  
  static ResultSetReader<Object> getResultSetReaderForPrimaryKey(Object val) {
      Class<?> clazz = val.getClass();
      if (clazz == String.class) {
          return new ResultSetReader<Object>() {
              public Object read(ResultSet rs) throws SQLException {
                  return rs.getString(1);
              }
          };
      } else if (clazz == Integer.class) {
          return new ResultSetReader<Object>() {
              public Object read(ResultSet rs) throws SQLException {
                  return rs.getInt(1);
              }
          };
      } else if (clazz == Long.class) {
          return new ResultSetReader<Object>() {
              public Object read(ResultSet rs) throws SQLException {
                  return rs.getLong(1);
              }
          };
      } else if (clazz == Byte.class) {
          return new ResultSetReader<Object>() {
              public Object read(ResultSet rs) throws SQLException {
                  return rs.getByte(1);
              }
          };
      } else if (clazz == Double.class) {
          return new ResultSetReader<Object>() {
              public Object read(ResultSet rs) throws SQLException {
                  return rs.getDouble(1);
              }
          };
      } else if (clazz == Float.class) {
          return new ResultSetReader<Object>() {
              public Object read(ResultSet rs) throws SQLException {
                  return rs.getFloat(1);
              }
          };
      } else if (clazz == byte[].class) {
          return new ResultSetReader<Object>() {
              /*
               * private byte[] getBytesFromArrayString(String string) {
               * String part[] = string.replaceAll("\\[", "").replaceAll("\\]",
               * "").split(",");
               * byte[] ret = new byte[part.length];
               * for (int i = 0; i < ret.length; i++) {
               * ret[i] = (byte) (Integer.parseInt(part[i].trim()));
               * }
               * return ret;
               * }
               */
              public Object read(ResultSet rs) throws SQLException {
                  return rs.getString(1);
              }
          };
      } else {
          throw new RuntimeException("unsupported " + clazz);
      }
  }

}
