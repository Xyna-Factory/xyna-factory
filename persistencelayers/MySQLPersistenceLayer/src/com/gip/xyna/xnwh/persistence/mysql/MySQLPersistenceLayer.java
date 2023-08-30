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
package com.gip.xyna.xnwh.persistence.mysql;



import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionCouldNotBeClosedException;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.db.ExtendedParameter;
import com.gip.xyna.utils.db.InList;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.StatementCache;
import com.gip.xyna.utils.db.WrappedConnection;
import com.gip.xyna.utils.db.exception.UnexpectedParameterException;
import com.gip.xyna.utils.db.types.BLOB;
import com.gip.xyna.utils.db.types.BooleanWrapper;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xnwh.exceptions.XNWH_ConnectionClosedException;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.TransactionProperty;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseColumnInfo;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseIndexCollision;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseIndexCollision.IndexModification;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabasePersistenceLayerConnectionWithAlterTableSupport;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabasePersistenceLayerWithAlterTableSupportHelper;
import com.gip.xyna.xnwh.persistence.xmom.PersistenceExpressionVisitors;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;
import com.gip.xyna.xnwh.pools.MySQLPoolType;
import com.gip.xyna.xnwh.pools.PoolDefinition;
import com.gip.xyna.xnwh.pools.TypedConnectionPoolParameter;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser.EscapeParams;
import com.gip.xyna.xnwh.utils.SQLErrorHandling;
import com.gip.xyna.xnwh.utils.SQLErrorHandlingLogger;
import com.gip.xyna.xnwh.utils.SQLErrorHandlingLogger.SQLErrorHandlingLoggerBuilder;



public class MySQLPersistenceLayer implements PersistenceLayer {

  private static final Logger logger = CentralFactoryLogging.getLogger(MySQLPersistenceLayer.class);
  private static final Pattern NUMBERS_PATTERN = Pattern.compile("^\\d+$");
  private static final Map<String, Class<? extends Storable>> tableToClassMap = new HashMap<String, Class<? extends Storable>>();

  /**
   * falls true, werden alle sql-debugs mit {@link #logger} gemacht, ansonsten mit einem dynamischen logger, der sich
   * aus der caller-class ergibt und dann den namen "xyna.sql.&lt;callerclass&gt;" bekommt.
   */
  private boolean usePersistenceLayerLogger = false;


  private static enum MySqlBaseType {
    NUMBER, FLOAT, TIME, TEXT_ENCODED, BINARY, OTHER;

    /**
     * gibt zurück, ob der übergebene typ größergleich ist. OTHER.isCompatibleTo(TIME) = false
     * NUMBER.isCompatibleTo(TEXT_ENCODED) = true
     * @param otherType
     * @return
     */
    public boolean isCompatibleTo(MySqlBaseType otherType) {
      if (this == otherType) {
        return true;
      }
      if (this == NUMBER) {
        return otherType == FLOAT || otherType == TEXT_ENCODED || otherType == BINARY;
      } else if (this == FLOAT) {
        return otherType == TEXT_ENCODED || otherType == BINARY;
      } else if (this == TIME) {
        return otherType == TEXT_ENCODED || otherType == BINARY;
      } else if (this == TEXT_ENCODED) {
        return otherType == BINARY;
      } else if (this == BINARY) {
        return otherType == TEXT_ENCODED;
      } else
        return false;
    }
  }

  private enum UpdateInsert {
    update, insert, done;
  }


  private static final int MAX_INSERT_RETRY_COUNTER = 3;


  private static enum MySqlType {
    BOOL(1, MySqlBaseType.NUMBER), BOOLEAN(1, MySqlBaseType.NUMBER), TINYINT(1, MySqlBaseType.NUMBER), SMALLINT(2,
        MySqlBaseType.NUMBER), MEDIUMINT(3, MySqlBaseType.NUMBER), INT(4, MySqlBaseType.NUMBER), INTEGER(4,
        MySqlBaseType.NUMBER), BIGINT(8, MySqlBaseType.NUMBER), SERIAL(8, MySqlBaseType.NUMBER), BIT(8,
        MySqlBaseType.NUMBER), FLOAT(4, MySqlBaseType.FLOAT), DOUBLE(8, MySqlBaseType.FLOAT), REAL(8,
        MySqlBaseType.FLOAT), DECIMAL(4, MySqlBaseType.FLOAT), DEC(4, MySqlBaseType.FLOAT), NUMERIC(8,
        MySqlBaseType.FLOAT), DATE(3, MySqlBaseType.TIME), DATETIME(8, MySqlBaseType.TIME), TIMESTAMP(4,
        MySqlBaseType.TIME), TIME(3, MySqlBaseType.TIME), YEAR(1, MySqlBaseType.TIME), CHAR(255,
        MySqlBaseType.TEXT_ENCODED, true), NCHAR(255, MySqlBaseType.TEXT_ENCODED, true), VARCHAR(65535,
        MySqlBaseType.TEXT_ENCODED, true), TINYTEXT(255, MySqlBaseType.TEXT_ENCODED), TEXT(65535,
        MySqlBaseType.TEXT_ENCODED), MEDIUMTEXT(16777215, MySqlBaseType.TEXT_ENCODED), LONGTEXT(4294967295L,
        MySqlBaseType.TEXT_ENCODED), BINARY(255, MySqlBaseType.BINARY, true), VARBINARY(65535, MySqlBaseType.BINARY,
        true), TINYBLOB(255, MySqlBaseType.BINARY), BLOB(65535, MySqlBaseType.BINARY), MEDIUMBLOB(16777215,
        MySqlBaseType.BINARY), LONGBLOB(4294967295L, MySqlBaseType.BINARY), ENUM(65635, MySqlBaseType.OTHER), SET(64,
        MySqlBaseType.OTHER), UNKNOWN(0, null);

    private boolean dependentOnSizeSpecification;
    private long size;
    private MySqlBaseType baseType;


    private MySqlType(long size, MySqlBaseType baseType) {
      this(size, baseType, false);
    }


    private MySqlType(long size, MySqlBaseType baseType, boolean dependentOnSizeSpecification) {
      this.size = size;
      this.baseType = baseType;
      this.dependentOnSizeSpecification = dependentOnSizeSpecification;
    }


    protected boolean isDependentOnSizeSpecification() {
      return dependentOnSizeSpecification;
    }


    protected long getSize() {
      return size;
    }


    protected MySqlBaseType getBaseType() {
      return baseType;
    }


    /**
     * gibt zu einem typ alle damit kompatiblen typen zurück (die "größer" sind). beispiel: BLOB.getCompatibleTypes =>
     * BLOB, MEDIUMBLOB, LONGBLOB
     * @return
     */
    protected List<MySqlType> getCompatibleTypes() {
      List<MySqlType> types = new ArrayList<MySqlType>();
      for (MySqlType type : MySqlType.values()) {
        if (baseType.isCompatibleTo(type.baseType)) {
          if (size <= type.size) {
            types.add(type);
          }
        }
      }
      return types;
    }
  }


  private static Map<Class<?>, MySqlType> javaTypeToMySQLType = new HashMap<Class<?>, MySqlType>();


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


  public PersistenceLayerConnection getConnection() throws PersistenceLayerException {
    return new MySQLPersistenceLayerConnection();
  }


  public PersistenceLayerConnection getDedicatedConnection() throws PersistenceLayerException {
    return new MySQLPersistenceLayerConnection(true);
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


  public String[] getParameterInformation() {
    return new String[] {
        "poolname (see listconnectionpools)",
        "timeout(ms)",
        "additional optional parameters are key value pairs (key=value). supported keys are: " + "\n"
            + KEY_AUTOMATIC_COLUMN_TYPE_WIDENING + " (true/false)" + "\n" + KEY_USE_PL_LOGGER
            + " (true=>better performance, false=>better configuration possibilities)" + "\n" 
            + KEY_DURABLE_STATEMENT_CACHE + " (true/false)" + "\n" + KEY_ZIPPED_BLOBS + " (true/false)"};
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
      if (!regularPoolDefinition.getType().equals(MySQLPoolType.POOLTYPE_IDENTIFIER)) {
        // only warn as it might be a custom poolType made for MySQL-PL usage
        logger.warn("PoolType does not match the expected identifier!");
      }
      TypedConnectionPoolParameter tcpp = regularPoolDefinition.toCreationParameter();
      tcpp.size(0).name(tcpp.getName() + "_dedicated");
      try {
        poolMgmt.startAndAddConnectionPool(tcpp);
      } catch (NoConnectionAvailableException e) {
        // mimics behavior of @deprecated getInstance call
        throw new RuntimeException(e);
      }
      dedicatedPoolDefinition = poolMgmt.getConnectionPoolDefinition(tcpp.getName());
    }
    
    if (regularPoolDefinition == null) {
      throw new XNWH_GeneralPersistenceLayerException("Pool does not exist!");
    }
    
    
    
    url = regularPoolDefinition.getConnectstring();
    username = regularPoolDefinition.getUser();
    
    // TODO echtes Pattern für den connect string benutzen
    int i = url.lastIndexOf("/");
    if (i < 0 || i + 1 == url.length()) {
      throw new XNWH_GeneralPersistenceLayerException("Connect string must contain a schema name.");
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
        automaticColumnTypeWidening = Boolean.valueOf(keyValue[1]);
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
  
  //basiert auf den inneren (echten) connections. cache räumt sich automatisch auf, wenn die connections nicht mehr verwendet werden
  //auf die pooledconnection kann man den cache nicht basieren, weil dieser innen seine connection austauschen kann
  //achtung: feature ist für ORACLE nicht einfach zu kopieren.
  private WeakHashMap<Connection, StatementCache> statementCaches = new WeakHashMap<Connection, StatementCache>();
  private boolean useDurableStatementCache = false;
  private boolean zippedBlobs = false;
  
  //wenn dedizierte connections den cache verwenden, muss darauf reagiert werden, dass die innere connection evtl ausgetauscht wurde
  private static class MySQLPLStatementCache extends StatementCache {

    private WeakReference<Connection> wr;
    
    @Override
    public PreparedStatement getPreparedStatement(String sql) {
      PreparedStatement ps = super.getPreparedStatement(sql);
      if (ps == null) {
        return null;
      }
      try {
        Connection innerCon = ps.getConnection();
        if (innerCon instanceof WrappedConnection) {
          innerCon = ((WrappedConnection) innerCon).getWrappedConnection();
        }
        if (wr != null) {
          if (innerCon == null || innerCon.isClosed() || wr.get() != innerCon) {
            close();
            ps = null;
            wr = new WeakReference<Connection>(innerCon);
          }
        } else if (innerCon != null) { 
          //erstes mal belegen
          wr = new WeakReference<Connection>(innerCon);
        }
      } catch (SQLException e) {
        close();
        ps = null;
      }
      return ps;
    }
    
  }
  
  private SQLUtils createSQLUtils(boolean isDedicated) throws PersistenceLayerException {
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


  private void closeSQLUtils(SQLUtils sqlUtils) throws PersistenceLayerException {
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


  private static class MySQLColumnInfo extends DatabaseColumnInfo {

    private final static String SELECT_COLUMN_NAME_AND_DATATYPE_SQL =
        "select column_name, data_type, character_maximum_length, column_key from information_schema.columns"
            + " where table_schema = database() and table_name= ?";
    static final String DOES_TABLE_EXIST_SQL =
        "select count(*) from information_schema.tables where table_schema = ? and table_name = ?";
    static final String DOES_INDEX_EXIST_FOR_TABLE_SQL =
        "SELECT count(*) FROM information_schema.statistics WHERE index_name = ? AND table_name = ?";


    private MySqlType type; //uppercase
    private IndexType indexType;
    private MySQLColumnInfo next; //verkettete Liste, wenn mehrere Einträge zu einer Tabellenspalte existieren
    private Class<?> clazz;

    @Override
    public String getTypeAsString() {
      return type != null ? type.toString() : "<null>";
    }

    public void setStorableClass(Class<?> clazz) {
      this.clazz = clazz;
    }

    @Override
    public boolean isTypeDependentOnSizeSpecification() {
      if (type == null) {
        throw new RuntimeException("Type has not been set.");
      }
      return type.isDependentOnSizeSpecification();
    }


    @Override
    public String toString() {
      return "MySQLColumnInfo(clazzloader=" + (clazz == null ? "?" : clazz.getClassLoader()) + ", type=" + type + ",indexType=" + indexType + ",next=" + next + ")";
    }


    protected static IndexType getIndexTypeByString(String column_key) {
      if (column_key == null || column_key.equals("")) {
        return IndexType.NONE;
      } else if (column_key.equals("PRI")) {
        return IndexType.PRIMARY;
      } else if (column_key.equals("MUL")) {
        return IndexType.MULTIPLE;
      } else if (column_key.equals("UNI")) {
        return IndexType.UNIQUE;
      } else {
        throw new RuntimeException("Invalid response from database");
      }
    }


    /**
     * @param tableName
     * @return
     */
    public static ResultSetReader<MySQLColumnInfo> getResultSetReader(final String tableName) {
      return new ResultSetReader<MySQLColumnInfo>() {

        public MySQLColumnInfo read(ResultSet rs) throws SQLException {
          MySQLColumnInfo i = new MySQLColumnInfo();
          i.setName(rs.getString("column_name"));
          try {
            i.type = MySqlType.valueOf(rs.getString("data_type").toUpperCase());
          } catch (IllegalArgumentException e) {
            i.type = MySqlType.UNKNOWN;
            logger.warn("unknown datatype: " + rs.getString("data_type") + " in column " + i.getName() + " of table "
                + tableName + ".");
          }
          i.setCharLength(rs.getLong("character_maximum_length"));
          i.indexType = getIndexTypeByString(rs.getString("column_key"));
          return i;
        }

      };
    }

  }


  private static <T extends Storable> MySqlType getDefaultMySQLColTypeForStorableColumn(Column col, Class<T> klass) {
    ColumnType colType = col.type();
    if (colType == ColumnType.BLOBBED_JAVAOBJECT) {
      return MySqlType.LONGBLOB;
    } else if (colType == ColumnType.BYTEARRAY) {
      return MySqlType.LONGBLOB;
    } else if (colType == ColumnType.INHERIT_FROM_JAVA) {
      Field f = Storable.getColumn(col, klass);
      Class<?> fieldType = f.getType();
      MySqlType type = javaTypeToMySQLType.get(fieldType);
      if (type == null) {
        //Iteration über die Einträge in javaTypeToMySQLType: evtl. ist Storable-Column von einem bekannten Typ abgeleitet
        for (Class<?> clazz : javaTypeToMySQLType.keySet()) {
          if (clazz.isAssignableFrom(fieldType)) {
            type = javaTypeToMySQLType.get(clazz);
            break;
          }
        }
      }
      if (type == null) {
        return MySqlType.LONGBLOB;
      } else if (type == MySqlType.VARCHAR) {
        if (col.size() > type.getSize()) {
          return MySqlType.LONGTEXT;
        } else {
          return type;
        }
      } else {
        return type;
      }
    } else {
      throw new RuntimeException("unsupported columntype: " + colType);
    }
  }


  private static <T extends Storable> com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T> getReader(Class<T> klass)
      throws PersistenceLayerException {
    T i;
    try {
      i = klass.newInstance();
    } catch (InstantiationException e) {
      // TODO exception type
      throw new XNWH_GeneralPersistenceLayerException("Storable " + klass.getName()
          + " must have a valid no arguments constructor.", e);
    } catch (IllegalAccessException e) {
      throw new XNWH_GeneralPersistenceLayerException("Storable " + klass.getName()
          + " must have a valid no arguments constructor.", e);
    }
    return (com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T>) i.getReader();
  }


  private static String createInsertStatement(Column[] columns, String tableName) throws PersistenceLayerException {
    StringBuilder cols = new StringBuilder();
    StringBuilder vals = new StringBuilder();
    if (columns.length == 0) {
      throw new XNWH_GeneralPersistenceLayerException("no columns found to persist");
    }
    for (int i = 0; i < columns.length - 1; i++) {
      cols.append(columns[i].name()).append(", ");
      vals.append("?, ");
    }
    cols.append(columns[columns.length - 1].name());
    vals.append("?");

    return "insert into " + tableName + " (" + cols + ") values (" + vals + ")";
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


  // unterstützt nicht mehrere threads die die gleiche connection benutzen
  private class MySQLPersistenceLayerConnection
      implements
        PersistenceLayerConnection,
        DatabasePersistenceLayerConnectionWithAlterTableSupport {

    private final ConnectionPool connectionPool;
    private final SQLUtils sqlUtils;
    private boolean closed = false;
    //falls db connection über mehrere mysql-pl cons geteilt wird, stehen hier alle beteiligten aktiven mysqlPL-connections drin.
    //beim close schliesst nur der letzte aus der liste die zugrundeliegende db-connection
    private final List<MySQLPersistenceLayerConnection> sharedConnections;

    public MySQLPersistenceLayerConnection() throws PersistenceLayerException {
      this(false);
    }


    public MySQLPersistenceLayerConnection(boolean isDedicated) throws PersistenceLayerException {
      sqlUtils = createSQLUtils(isDedicated);
      try {
        if (sqlUtils.getConnection() != null) {
          sqlUtils.getConnection().setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }
      } catch (SQLException e) {
        logger
            .warn("Unable to set TransactionIsolationLevel, this might lead to strange behaviour if the global IsolationLevel differs.");
      }
      if (isDedicated) {
        connectionPool = dedicatedPool;
      } else {
        connectionPool = pool;
      }
      sharedConnections = new ArrayList<MySQLPersistenceLayerConnection>();
      sharedConnections.add(this);
    }
  

    public MySQLPersistenceLayerConnection(MySQLPersistenceLayerConnection shareConnectionPool) {
      sqlUtils = shareConnectionPool.sqlUtils;
      connectionPool = pool;
      sharedConnections = shareConnectionPool.sharedConnections;
      sharedConnections.add(this);
    }


    public boolean isOpen() {
      try {
        return !closed && !sqlUtils.getConnection().isClosed();
      } catch (SQLException e) {
        return false;
      }
    }


    /**
     * überprüfung ob tabelle existiert. falls nicht wird sie erstellt. falls ja, wird validiert, ob spalten fehlen oder
     * geändert werden müssen. fehlende spalten werden hinzugefügt. vorhandene spalten werden geupdated, falls das ohne
     * datenverlust möglich ist. (ansonsten wird ein fehler geworfen.)
     */
    public <T extends Storable> void addTable(Class<T> klass, boolean forceWidening, Properties props)
        throws PersistenceLayerException {
      try {
        if (forceWidening) {
          boolean previousSetting = automaticColumnTypeWidening;
          automaticColumnTypeWidening = true;
          DatabasePersistenceLayerWithAlterTableSupportHelper.addTable(this, klass);
          automaticColumnTypeWidening = previousSetting;
        } else {
          DatabasePersistenceLayerWithAlterTableSupportHelper.addTable(this, klass);
        }
        tableToClassMap.put(Storable.getPersistable(klass).tableName().toLowerCase(), klass);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        validateConnection();
        throw new XNWH_GeneralPersistenceLayerException("Storable " + Storable.getPersistable(klass).tableName() + " could not be registered in " + MySQLPersistenceLayer.class.getSimpleName(), e);
      }
    }


    public <T extends Storable> void createTable(Persistable persistable, Class<T> klass, Column[] cols) {
      String tableName = persistable.tableName().toLowerCase();
      StringBuilder createTableStatement = new StringBuilder("CREATE TABLE ").append(tableName).append(" (\n");
      for (Column col : cols) {
        String typeAsString = getDefaultColumnTypeString(col, klass);
        createTableStatement.append("  ").append(col.name()).append(" ").append(typeAsString);
        //default wert
        if (col.name().equals(persistable.primaryKey())) {
          createTableStatement.append(" NOT");
        }
        createTableStatement.append(" NULL,\n");
      }
      createTableStatement.append("  PRIMARY KEY(").append(persistable.primaryKey());
      for (Column column : cols) {
        // the following check is a bit nasty: dont add the "one" primary key twice. in theory it would be
        // possible to use only this loop without the check but that would require that all tables have set
        // the IndexType correctly.
        if (column.index() == IndexType.PRIMARY && !column.name().equals(persistable.primaryKey())) {
          createTableStatement.append(", ").append(column.name());
        }
      }
      createTableStatement.append(")\n");
      createTableStatement.append(")\n ENGINE=InnoDB");
      sqlUtils.setLogger(sqlUtilsLoggerCreateTable);
      try {
        sqlUtils.executeDDL(createTableStatement.toString(), null);
        if (!isView(tableName)) {
          
          for (Column column : cols) {
            if (column.name().equals(persistable.primaryKey())) {
              continue;
            }
            switch (column.index()) {
              case NONE :
                //nichts zu tun
                break;
              case PRIMARY :
                //nichts zu tun: Ist bereits beim CREATE TABLE gesetzt worden
                break;
              default : //UNIQUE und MULTIPLE
                String indexName = createIndexName(tableName, column.name(), false);
                createIndex(indexName, column.index(), tableName, column.name());
            }
          }
        }
      } finally {
        sqlUtils.setLogger(sqlUtilsLoggerDebug);
      }
    }


    private <T extends Storable> boolean isView(String tableName) {
      Boolean queryResult = sqlUtils.queryOneRow("SELECT TABLE_TYPE FROM information_schema.TABLES WHERE TABLE_NAME = ? AND TABLE_SCHEMA = ?",
                                                 new com.gip.xyna.utils.db.Parameter(tableName, schemaName), new ResultSetReader<Boolean>() {

                                                   public Boolean read(ResultSet rs) throws SQLException {
                                                     String tableType = rs.getString("TABLE_TYPE");
                                                     return tableType != null && tableType.equalsIgnoreCase("VIEW");
                                                   }
                                                 });
      return Boolean.TRUE.equals(queryResult);
    }


//  Kürzungsregeln:
//  (Bsp. Ziel: 62)
//  abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz_012345678901234567890123456789012345678901234567890123456789_idx  ==> 117 Zeichen 
//
//      abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz_0123456789012345678901234567890123456789012345678901_idx  ==> 109 Zeichen
//      1. Kürzen bis beide gleich lang (Falls es währenddessen schon passt: aufhören)
//
//      abcdefghijklmnopqrstuvwxyzab_01234567890123456789012345678_idx  ==> 62 Zeichen
//      2. Beide gleich kürzen bis es passt
    
  //Maximale Länge des Hashs, bei dem es noch Sinn macht zu modulon. Alles darüber übertrifft Integer.MAX_VALUE und ergibt somit keinen Sinn mehr
  private final int MAX_HASH = (int)(Math.log(Integer.MAX_VALUE)/Math.log(36));

  private String createIndexName(String tableName, String columnName, boolean pk) {
    final boolean useDatabaseQueryForDuplicates = databaseQueryToAvoidDuplicateIndexes.get();
    final int HASH_LENGTH = hashCharacterCountToAvoidDuplicateIndexes.get();
    final int MAXLENGHTH = maximumIndexNameLenght.get();
    
    String tablePart = tableName;
    String columnPart = columnName;
    String indexName = tablePart.replace('.', '_') + "_" + columnPart + (pk ? "_pk" : "_idx");
  
    int toShorten =indexName.length() - MAXLENGHTH;
    if(toShorten > 0) {
      toShorten += (useDatabaseQueryForDuplicates ? 2:HASH_LENGTH);
      int toShortenTableName;
      int toShortenColumnName;
      if(tablePart.length() > columnPart.length()) {
        toShortenTableName = Math.min(toShorten, tablePart.length() - columnPart.length()); //Auf gleiche Länge
        toShortenTableName += (toShorten - toShortenTableName) / 2; //Rest
        toShortenColumnName = toShorten - toShortenTableName;
      } else {
        toShortenColumnName = Math.min(toShorten, columnPart.length() - tablePart.length()); //Auf gleiche Länge
        toShortenColumnName += (toShorten - toShortenColumnName) / 2; //Rest
        toShortenTableName = toShorten - toShortenColumnName;
      }
      tablePart = tablePart.substring(0, tablePart.length() - toShortenTableName);
      columnPart = columnPart.substring(0, columnPart.length() - toShortenColumnName);
      indexName = tablePart.replace('.', '_') + "_" + columnPart + (pk ? "_pk" : "_idx");
      
      if (useDatabaseQueryForDuplicates) {
        int suffix = 0;
        indexName = tablePart.replace('.', '_') + "_" + columnPart + (suffix != 0 ? suffix : "")
            + (pk ? "_pk" : "_idx");
        while (doesIndexExistForTable(indexName, tableName)) {
          suffix++;
          indexName = tablePart.replace('.', '_') + "_" + columnPart + (suffix != 0 ? suffix : "")
              + (pk ? "_pk" : "_idx");
        }
      } else { 
        String hash = "";
        if(HASH_LENGTH <= MAX_HASH) { //Falls HASH_LENGTH Integer.MAX_VALUE übertrifft, gibte es keinen Sinn mehr zu modulon
          int mod = (int) Math.pow(36, HASH_LENGTH);
          hash = numberToStringUsingAllChars(columnName.hashCode() % mod);
        } else {
          hash = numberToStringUsingAllChars(columnName.hashCode());
        }
        indexName = tablePart.replace('.', '_') + "_" + columnPart + hash
            + (pk ? "_pk" : "_idx");
      }
    }
    return indexName;
  }
  
  private String numberToStringUsingAllChars(int num) {
    StringBuilder back = new StringBuilder();
    num = Math.abs(num);
    while(num!=0) {
      int charValue = Math.abs(num % 36);
      num /=36;
      if(charValue <= 9) {
        back.append(charValue);
      } else {
        back.append((char)('a' + charValue - 10));
      }
    }
    return back.toString();
  }




    private void createIndex(String indexName, IndexType indexType, String tableName, String columnName) {
      String createIndexStatement = null;
      switch (indexType) {
        case UNIQUE :
          createIndexStatement = "CREATE UNIQUE INDEX " + indexName + " ON " + tableName + "(" + columnName + ")";
          break;
        case MULTIPLE :
          createIndexStatement = "CREATE INDEX " + indexName + " ON " + tableName + "(" + columnName + ")";
          break;
        default :
          logger.info("Index-Creation " + indexName + " of type " + indexType + " for " + tableName + "." + columnName
              + " is unsupported!");
          return;
      }
      logger.info("Index-Creation " + indexName + " of type " + indexType + " for " + tableName + "." + columnName);
      executeDDL(createIndexStatement);
    }


    private void executeDDL(String ddl) {
      //Ausführen des Statements ddl und Warn-Log, falls dies nicht erfolgreich war
      boolean created = false;
      try {
        sqlUtils.executeDDL(ddl, null);
        created = true;
      } finally {
        if (!created) {
          logger.warn("Statement was " + ddl);
        }
      }
    }


    public <T extends Storable> void modifyColumnsCompatible(Column col, Class<T> klass, String tableName) {
      if (!isView(tableName)) {
        MySqlType recommendedType = getDefaultMySQLColTypeForStorableColumn(col, klass);
        String sql =
            new StringBuffer("ALTER TABLE ").append(tableName).append(" CHANGE ").append(col.name()).append(" ")
                .append(col.name()).append(" ").append(recommendedType).append("(").append(getColumnSize(col, klass))
                .append(")").toString();
        sqlUtils.setLogger(sqlUtilsLoggerCreateTable);
        try {
          sqlUtils.executeDDL(sql, null);
        } finally {
          sqlUtils.setLogger(sqlUtilsLoggerDebug);
        }
      }
    }


    public <T extends Storable> String getCompatibleColumnTypesAsString(Column col, Class<T> klass) {
      MySqlType recommendedType = getDefaultMySQLColTypeForStorableColumn(col, klass);
      List<MySqlType> compatibleTypes = recommendedType.getCompatibleTypes();
      StringBuilder compatibleTypesStringBuilder = new StringBuilder();
      for (int i = 0; i < compatibleTypes.size(); i++) {
        if (i > 0) {
          compatibleTypesStringBuilder.append(", ");
        }
        MySqlType next = compatibleTypes.get(i);
        compatibleTypesStringBuilder.append(next.toString());
        if (next.isDependentOnSizeSpecification()) {
          compatibleTypesStringBuilder.append("(?)");
        }
      }
      return compatibleTypesStringBuilder.toString();
    }


    public <T extends Storable> boolean areBaseTypesCompatible(Column col, Class<T> klass, DatabaseColumnInfo colInfo) {
      MySqlType recommendedType = getDefaultMySQLColTypeForStorableColumn(col, klass);
      return ((MySQLColumnInfo) colInfo).type.getBaseType().isCompatibleTo(recommendedType.getBaseType());
    }


    public <T extends Storable> void widenColumnsCompatible(Column col, Class<T> klass, String tableName) {
      if (!isView(tableName)) {
        MySqlType recommendedType = getDefaultMySQLColTypeForStorableColumn(col, klass);
        StringBuffer sql =
            new StringBuffer("ALTER TABLE ").append(tableName).append(" CHANGE ").append(col.name()).append(" ")
                .append(col.name()).append(" ").append(recommendedType);
        if (recommendedType.isDependentOnSizeSpecification()) {
          sql.append("(").append(getColumnSize(col, klass)).append(")");
        }
        sqlUtils.executeDDL(sql.toString(), null);
      }
    }


    public <T extends Storable> boolean areColumnsCompatible(Column col, Class<T> klass, DatabaseColumnInfo colInfo) {
      MySqlType recommendedType = getDefaultMySQLColTypeForStorableColumn(col, klass);
      List<MySqlType> compatibleTypes = recommendedType.getCompatibleTypes();
      return compatibleTypes.contains(((MySQLColumnInfo) colInfo).type);
    }


    public <T extends Storable> Set<DatabaseIndexCollision> checkColumns(Persistable persistable, Class<T> klass, Column[] cols)
                    throws PersistenceLayerException {
      final String tableNameWithSchemaPrefix = persistable.tableName().toLowerCase();
      final String tableNameWithoutSchemaPrefix;
      if (tableNameWithSchemaPrefix.contains(".")) {
        String tableNameParts[] = tableNameWithSchemaPrefix.split("\\.");
        tableNameWithoutSchemaPrefix = tableNameParts[tableNameParts.length - 1];
      } else {
        tableNameWithoutSchemaPrefix = tableNameWithSchemaPrefix;
      }

      sqlUtils.setLogger(sqlUtilsLoggerCreateTable);
      try {
        List<MySQLColumnInfo> colInfos =
            sqlUtils.query(MySQLColumnInfo.SELECT_COLUMN_NAME_AND_DATATYPE_SQL,
                           new com.gip.xyna.utils.db.Parameter(tableNameWithoutSchemaPrefix),
                           MySQLColumnInfo.getResultSetReader(tableNameWithoutSchemaPrefix));

        StringBuffer addColumnString = new StringBuffer();
        for (Column col : cols) {
          boolean foundCol = false;
          for (MySQLColumnInfo colInfo : colInfos) {
            if (col.name().equalsIgnoreCase(colInfo.getName())) {
              DatabasePersistenceLayerWithAlterTableSupportHelper.checkColumn(this, colInfo, col, klass,
                                                                              tableNameWithSchemaPrefix,
                                                                              automaticColumnTypeWidening);
              foundCol = true;
            }
          }
          if (!foundCol) {
            //spalte erstellen
            if (addColumnString.length() > 0) {
              addColumnString.append(",");
            }
            addColumnString.append("\n  ADD COLUMN ").append(col.name()).append(" ")
                .append(getDefaultColumnTypeString(col, klass)).append(" NULL");
          }
        }

        // Updating columnMap as some modifications might have occured.
        colInfos =
            sqlUtils.query(MySQLColumnInfo.SELECT_COLUMN_NAME_AND_DATATYPE_SQL,
                           new com.gip.xyna.utils.db.Parameter(tableNameWithoutSchemaPrefix),
                           MySQLColumnInfo.getResultSetReader(tableNameWithoutSchemaPrefix));

        for (Column col : cols) {
          for (MySQLColumnInfo colInfo : colInfos) {
            if (col.name().equalsIgnoreCase(colInfo.getName())) {
              colInfo.setStorableClass(klass);
              if (columnMap.get(col) == null ||
                  columnMap.get(col).indexType != colInfo.indexType ||
                  columnMap.get(col).type != colInfo.type) {
                colInfo.next = columnMap.get(col); //evtl. vorherige Einträge aufheben: es kann mehrere Einträge ...
                //... in "colInfos" zu einem Eintrag in "cols" geben 
                columnMap.put(col, colInfo);
              }
            }
          }
        }

        Set<DatabaseIndexCollision> collisions = new HashSet<DatabaseIndexCollision>();
        if (!isView(tableNameWithSchemaPrefix)) {
          if (addColumnString.length() > 0) {
            sqlUtils.executeDDL("ALTER TABLE " + tableNameWithSchemaPrefix + "\n" + addColumnString.toString(), null);
          }
          
          //indizes überprüfen
          for (Column column : cols) {
            boolean isPk = column.name().equals(persistable.primaryKey());
            IndexType javaIndexType = isPk ? IndexType.UNIQUE : column.index();

            MySQLColumnInfo colInfo = columnMap.get(column);
            if (colInfo == null) {
              //keine Daten für den Index bislang, deswegen evtl. neu bauen
              if (javaIndexType != IndexType.NONE) {
                String indexName = createIndexName(tableNameWithSchemaPrefix, column.name(), isPk);
                try {
                  createIndex(indexName, javaIndexType, tableNameWithSchemaPrefix, column.name());
                } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
                  validateConnection();
                  logger.warn("Could not create index " + indexName + " on table " + tableNameWithSchemaPrefix
                      + " for column " + column.name() + " of type " + javaIndexType + ".", e);
                }
              }
              continue;
            }

            if (javaIndexType == IndexType.NONE && colInfo.indexType == IndexType.NONE) {
              continue; //kein Index
            }

            if (colInfo.indexType == IndexType.PRIMARY && colInfo.getName().equalsIgnoreCase(persistable.primaryKey())) {
              //primarykey index ok...
              continue;
            }

            //es gibt mindestens einen Index für diese Spalte, dieser hat einen Namen
            String indexName = createIndexName(tableNameWithSchemaPrefix, column.name(), isPk);
            try {
              IndexModification mod = checkIndex(column, colInfo, javaIndexType, indexName, tableNameWithSchemaPrefix);
              if (mod != null) {
                collisions.add(new DatabaseIndexCollision(persistable, column, klass, mod));
              }
            } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
              validateConnection();
              logger.warn("Could not create index " + indexName + " on table " + tableNameWithSchemaPrefix
                  + " for column " + column.name() + " of type " + javaIndexType + ".", e);
            }

          }
        }
        return collisions;
      } finally {
        sqlUtils.setLogger(sqlUtilsLoggerDebug);
      }
    }
    
    
    public <T extends Storable> void alterColumns(Set<DatabaseIndexCollision> collisions) throws PersistenceLayerException {
      for (DatabaseIndexCollision collision : collisions) {
        final String tableNameWithSchemaPrefix = collision.getPersi().tableName().toLowerCase();
        final String tableNameWithoutSchemaPrefix;
        if (tableNameWithSchemaPrefix.contains(".")) {
          String tableNameParts[] = tableNameWithSchemaPrefix.split("\\.");
          tableNameWithoutSchemaPrefix = tableNameParts[tableNameParts.length - 1];
        } else {
          tableNameWithoutSchemaPrefix = tableNameWithSchemaPrefix;
        }

        sqlUtils.setLogger(sqlUtilsLoggerCreateTable);
        try {
          Column col = collision.getColumn();

          if (!isView(tableNameWithSchemaPrefix)) {
            //indizes überprüfen
              boolean isPk = col.name().equals(collision.getPersi().primaryKey());
              IndexType javaIndexType = isPk ? IndexType.UNIQUE : col.index();

              MySQLColumnInfo colInfo = columnMap.get(col);
              if (colInfo == null) {
                //keine Daten für den Index bislang, deswegen evtl. neu bauen
                if (javaIndexType != IndexType.NONE) {
                  String indexName = createIndexName(tableNameWithSchemaPrefix, col.name(), isPk);
                  try {
                    createIndex(indexName, javaIndexType, tableNameWithSchemaPrefix, col.name());
                  } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
                    validateConnection();
                    logger.warn("Could not create index " + indexName + " on table " + tableNameWithSchemaPrefix
                        + " for column " + col.name() + " of type " + javaIndexType + ".", e);
                  }
                }
                continue;
              }

              if (javaIndexType == IndexType.NONE && colInfo.indexType == IndexType.NONE) {
                continue; //kein Index
              }

              if (colInfo.indexType == IndexType.PRIMARY && colInfo.getName().equalsIgnoreCase(collision.getPersi().primaryKey())) {
                //primarykey index ok...
                continue;
              }

              //es gibt mindestens einen Index für diese Spalte, dieser hat einen Namen
              String indexName = createIndexName(tableNameWithSchemaPrefix, col.name(), isPk);
              try {
                alterIndex(col, colInfo, javaIndexType, indexName, tableNameWithSchemaPrefix, collision.getIndexModification());
              } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
                validateConnection();
                logger.warn("Could not create index " + indexName + " on table " + tableNameWithSchemaPrefix
                    + " for column " + col.name() + " of type " + javaIndexType + ".", e);
              }

          }
        } finally {
          sqlUtils.setLogger(sqlUtilsLoggerDebug);
        }
      }
    }


    public long getPersistenceLayerInstanceId() {
      return MySQLPersistenceLayer.this.pliID;
    }
    
    
    private IndexModification checkIndex(Column column, MySQLColumnInfo colInfo, IndexType javaIndexType, String indexName,
                            String tableName) {
      //TODO anders als in OraclePersistenceLayer. Ist angleichen sinnvoll?
      //MySQLColumnInfo muss noch erweitert werden, damit Name des bestehenden Index bekannt ist
      if (colInfo.next != null) {
        logger.warn("Unimplemented check of multiple indexes on column " + column.name() + " in table " + tableName);
        return null; 
      }
      if (colInfo.indexType == javaIndexType) {
        // this is OK as long as we cannot verify the name
        return null;
      }
      if (javaIndexType == IndexType.NONE) {
        return IndexModification.DELETE;
      }
      if (colInfo.indexType == IndexType.NONE) {
        return IndexModification.CREATE;
      }
      
      return IndexModification.MODIFY;

    }
    
    
    private void alterIndex(Column column, MySQLColumnInfo colInfo, IndexType javaIndexType, String indexName,
                                              String tableName, IndexModification desiredModification) {
      //TODO anders als in OraclePersistenceLayer. Ist angleichen sinnvoll?
      //MySQLColumnInfo muss noch erweitert werden, damit Name des bestehenden Index bekannt ist
      if (colInfo.next != null) {
        logger.warn("Unimplemented check of multiple indexes on column " + column.name() + " in table " + tableName);
        return;
      }
      if (colInfo.indexType == javaIndexType) {
        // this is OK as long as we cannot verify the name
        return;
      }
      if (javaIndexType == IndexType.NONE) {
        if (desiredModification != IndexModification.DELETE) {
          logger.warn("Detected index deletion on column " + column.name() + " in table " + tableName + " does not match the desired modifcation " + desiredModification);
          return;
        } else {
          logger.warn("Unimplemented delete index on column " + column.name() + " in table " + tableName);
          return;
        }
      }
      if (colInfo.indexType == IndexType.NONE) {
        if (desiredModification != IndexModification.CREATE) {
          logger.warn("Detected index creation on column " + column.name() + " in table " + tableName + " does not match the desired modifcation " + desiredModification);
          return;
        } else {
          createIndex(indexName, javaIndexType, tableName, column.name());
          return;
        }
      }
      if (desiredModification != IndexModification.MODIFY) {
        logger.warn("Detected index modification on column " + column.name() + " in table " + tableName + " does not match the desired modifcation " + desiredModification);
      } else {
        logger.warn("Unimplemented change index on column " + column.name() + " in table " + tableName + ". Found: "
            + colInfo.indexType + ", expected: " + javaIndexType.name());
      }
    }


    public <T extends Storable> String getTypeAsString(Column col, Class<T> klass) {
      MySqlType colType = getDefaultMySQLColTypeForStorableColumn(col, klass);
      return colType.toString();
    }


    public <T extends Storable> boolean isTypeDependentOnSizeSpecification(Column col, Class<T> klass) {
      MySqlType colType = getDefaultMySQLColTypeForStorableColumn(col, klass);
      return colType.isDependentOnSizeSpecification();
    }


    public boolean doesTableExist(Persistable persistable) {
      String tableNameWithoutSchemaPrefix = persistable.tableName().toLowerCase();
      if (tableNameWithoutSchemaPrefix.contains(".")) {
        String tableNameParts[] = tableNameWithoutSchemaPrefix.split("\\.");
        tableNameWithoutSchemaPrefix = tableNameParts[tableNameParts.length - 1];
      }
      return sqlUtils.queryInt(MySQLColumnInfo.DOES_TABLE_EXIST_SQL,
                               new com.gip.xyna.utils.db.Parameter(schemaName, tableNameWithoutSchemaPrefix)) > 0;
    }
    
    public boolean doesIndexExistForTable(String index, String table) {
      return sqlUtils.queryInt(MySQLColumnInfo.DOES_INDEX_EXIST_FOR_TABLE_SQL,
          new com.gip.xyna.utils.db.Parameter(index, table)) > 0;
    }
    
    
    public <T extends Storable> String getDefaultColumnTypeString(Column col, Class<T> klass) {
      MySqlType colType = getDefaultMySQLColTypeForStorableColumn(col, klass);
      String typeAsString = colType.toString();
      if (colType.isDependentOnSizeSpecification()) {
        typeAsString += "(" + getColumnSize(col, klass) + ")";
      }
      return typeAsString;
    }


    public void closeConnection() throws PersistenceLayerException {
      if (closed) {
        return;
      }
      sharedConnections.remove(this);
      if (sharedConnections.isEmpty()) {
        closeSQLUtils(sqlUtils);
      }
      closed = true;
    }


    private void ensureOpen() throws PersistenceLayerException {
      if (closed) {
        throw new XNWH_ConnectionClosedException();
      }
    }


    public void commit() throws PersistenceLayerException {
      ensureOpen();
      try {
        sqlUtils.commit();
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        validateConnection();
        throw new XNWH_GeneralPersistenceLayerException("could not commit transaction.", e);
      }
    }


    public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
      String select =
          "select count(*) from " + storable.getTableName().toLowerCase() + " where "
              + Storable.getPersistable(storable.getClass()).primaryKey() + " = ?";
      com.gip.xyna.utils.db.Parameter paras = new com.gip.xyna.utils.db.ExtendedParameter();
      Column colPK = getColumnForPrimaryKey(storable);
      addToParameter(paras, colPK, storable.getPrimaryKey(), storable);
      try {
        sqlUtils.cacheStatement(select);
        int cnt = sqlUtils.queryInt(select, paras);
        return cnt > 0;
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        validateConnection();
        throw new XNWH_GeneralPersistenceLayerException("could not check for object.", e);
      }
    }


    public <T extends Storable> void deleteOneRow(T storable) throws PersistenceLayerException {
      String delete =
          "delete from " + storable.getTableName().toLowerCase() + " where "
              + Storable.getPersistable(storable.getClass()).primaryKey() + " = ?";
      deleteSingleElement(storable, delete);
    }


    public <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException {
      if (storableCollection != null && storableCollection.size() > 0) {
        Iterator<T> it = storableCollection.iterator();
        T a = it.next();
        String delete =
            "delete from " + a.getTableName().toLowerCase() + " where "
                + Storable.getPersistable(a.getClass()).primaryKey() + " = ?";
        deleteSingleElement(a, delete);
        while (it.hasNext()) {
          a = it.next();
          deleteSingleElement(a, delete);
        }
      }
    }


    private <T extends Storable> void deleteSingleElement(T storable, String deleteString)
        throws PersistenceLayerException {
      com.gip.xyna.utils.db.Parameter paras = new com.gip.xyna.utils.db.ExtendedParameter();
      Column colPK = getColumnForPrimaryKey(storable);
      addToParameter(paras, colPK, storable.getPrimaryKey(), storable);
      try {
        sqlUtils.cacheStatement(deleteString);
        sqlUtils.executeDML(deleteString, paras);
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);  
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        validateConnection();
        throw new XNWH_GeneralPersistenceLayerException(null, e);
      }
    }


    public <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException {
      String delete = "truncate table " + Storable.getPersistable(klass).tableName().toLowerCase();
      try {
        sqlUtils.executeDML(delete, null);
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        validateConnection();
        throw new XNWH_GeneralPersistenceLayerException(null, e);
      }
    }


    public int executeDML(PreparedCommand cmdInterface, Parameter paras) throws PersistenceLayerException {
      if (!(cmdInterface instanceof MySQLPreparedCommand)) {
        throw new XNWH_IncompatiblePreparedObjectException(PreparedCommand.class.getSimpleName());
      }
      MySQLPreparedCommand cmd = (MySQLPreparedCommand) cmdInterface;
      ensureOpen();
      com.gip.xyna.utils.db.Parameter sqlUtilsParas = null;
      if (paras != null) {
        for (int i = 0; i < paras.size(); i++) {
          if (paras.get(i) instanceof Boolean) {
            sqlUtilsParas = new ExtendedParameter();
            break;
          }
        }
        if (sqlUtilsParas == null) {
          sqlUtilsParas = new com.gip.xyna.utils.db.Parameter();
        }
        for (int i = 0; i < paras.size(); i++) {
          if (paras.get(i) instanceof Boolean) {
            sqlUtilsParas.addParameter(new BooleanWrapper((Boolean) paras.get(i)));
          } else {
            addToParameterWithArraySupportAsStrings(sqlUtilsParas, paras.get(i));
          }
        }
      }
      try {
        sqlUtils.cacheStatement(cmd.getSqlString());
        return sqlUtils.executeDML(cmd.getSqlString(), sqlUtilsParas);
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        validateConnection();
        throw new XNWH_GeneralPersistenceLayerException("problem executing sql command: \"" + cmd.getSqlString()
            + "\" [" + sqlUtilsParas + "]", e);
      }
    }


    public <T extends Storable> Collection<T> loadCollection(final Class<T> klass) throws PersistenceLayerException {
      ensureOpen();
      final com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T> reader = getReader(klass);
      try {
        ArrayList<T> list =
            sqlUtils.query("select * from " + Storable.getPersistable(klass).tableName().toLowerCase(), null,
                           new ResultSetReaderWrapper<T>(reader, zippedBlobs, klass));
        return list;
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        validateConnection();
        throw new XNWH_GeneralPersistenceLayerException(null, e);
      }
    }


    public <T extends Storable> void persistCollection(Collection<T> storableCollection)
        throws PersistenceLayerException {
      int size = storableCollection.size();
      if (size == 0) {
        return;
      }
      if (size == 1) {
        persistObject(storableCollection.iterator().next());
        return;
      }
      ensureOpen();
      Iterator<T> it = storableCollection.iterator();
      //erstmal ein select for update auf alle angegebenen objekte. dann schauen, ob sie da sind,
      //und daran entscheiden, ob man update oder insert machen muss
      //jeweils als batch-operation.
      //TODO merging von objekten mit dem gleichen pk
      
      final T firstElement = storableCollection.iterator().next();
      //gibt bytearray als string zurück
      ResultSetReader<Object> resultSetReaderForPK = getResultSetReaderForPrimaryKey(firstElement.getPrimaryKey());
      Column[] columns = firstElement.getColumns();
      final Column colPK = getColumnForPrimaryKey(firstElement);


      Persistable persistable = Storable.getPersistable(firstElement.getClass());
      StringBuilder start =
          new StringBuilder("select ").append(persistable.primaryKey()).append(" from ")
              .append(persistable.tableName().toLowerCase()).append(" where ");
      int remainingSize = size;
      Set<Object> existingPKs = new HashSet<Object>();
      while (remainingSize > 0) {
        StringBuilder selectSql = new StringBuilder(start);
        Object[] pks = new Object[Math.min(100, remainingSize)];
        for (int i = 0; i<pks.length; i++) {
          T s = it.next();
          pks[i] = s.getPrimaryKey();
        }
        InList inList;
        try {
          inList = new InList(pks, 100) {

            @Override
            protected void addParameter(com.gip.xyna.utils.db.Parameter parameter, Object p) {
              try {
                addToParameter(parameter, colPK, p, firstElement);
              } catch (PersistenceLayerException e) {
                throw new RuntimeException(e);
              }
            }


            @Override
            protected com.gip.xyna.utils.db.Parameter createParameter() {
              return new ExtendedParameter();
            }

          };
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        selectSql.append(inList.getSQL(persistable.primaryKey()));
        selectSql.append(" for update");
        try {
          List<Object> result = sqlUtils.query(selectSql.toString(), inList.getParams(), resultSetReaderForPK);
          existingPKs.addAll(result);
        } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
          throw new XNWH_RetryTransactionException(e);          
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
          validateConnection();
          throw new XNWH_GeneralPersistenceLayerException("could not persist collection.", e);
        }

        remainingSize -= pks.length;
      }

      it = storableCollection.iterator();

      String insert = createInsertStatement(columns, persistable.tableName().toLowerCase());
      String update = createUpdateStatement(columns, firstElement);
      List<com.gip.xyna.utils.db.Parameter> insertParas = new ArrayList<com.gip.xyna.utils.db.Parameter>();
      List<com.gip.xyna.utils.db.Parameter> updateParas = new ArrayList<com.gip.xyna.utils.db.Parameter>();
      while (it.hasNext()) {
        T s = it.next();
        //transformation des pks, die im set existingPKs verwendet wird. 
        //(TODO etwas umständlich, das könnte man refactorn. oder man könnte die unformatierten pks 
        //vergleichen, dann müsste man im bytearray fall den comparator korrekt überschreiben)
        com.gip.xyna.utils.db.Parameter tempPara = new com.gip.xyna.utils.db.Parameter();
        addToParameter(tempPara, colPK, s.getPrimaryKey(), s);
        Object transformedPk = tempPara.getParameter(1);
        if (existingPKs.contains(transformedPk)) {
          //update
          com.gip.xyna.utils.db.Parameter paras = createParasForInsertAndUpdate(columns, s);
          //parameter für whereclause adden
          addToParameter(paras, colPK, s.getPrimaryKey(), s);
          updateParas.add(paras);
        } else {
          com.gip.xyna.utils.db.Parameter paras = createParasForInsertAndUpdate(columns, s);
          
          insertParas.add(paras);
          existingPKs.add(transformedPk); //damit nicht später in der gleichen collection erneut insert versucht wird
        }
      }
      try {
        if (insertParas.size() > 0) {
          sqlUtils
              .executeDMLBatch(insert, insertParas.toArray(new com.gip.xyna.utils.db.Parameter[insertParas.size()]));
        }
        if (updateParas.size() > 0) {
          sqlUtils
              .executeDMLBatch(update, updateParas.toArray(new com.gip.xyna.utils.db.Parameter[updateParas.size()]));
        }
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        validateConnection();
        throw new XNWH_RetryTransactionException(e);        
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        throw new XNWH_GeneralPersistenceLayerException("could not persist collection.", e);
      }
    }


    private void addToParameterWithArraySupportAsStrings(com.gip.xyna.utils.db.Parameter paras, Object val) {
      try {
        paras.addParameter(val); //erkennt strings, zahlen etc
      } catch (UnexpectedParameterException e) {
        //toString oder analoge repräsentation verwenden
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
    

    private ResultSetReader<Object> getResultSetReaderForPrimaryKey(Object val) {
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

   /*       private byte[] getBytesFromArrayString(String string) {
            String part[] = string.replaceAll("\\[", "").replaceAll("\\]", "").split(",");
            byte[] ret = new byte[part.length];
            for (int i = 0; i < ret.length; i++) {
              ret[i] = (byte) (Integer.parseInt(part[i].trim()));
            }
            return ret;
          }*/


          public Object read(ResultSet rs) throws SQLException {
            return rs.getString(1);
          }
        };
      } else {
        throw new RuntimeException("unsupported " + clazz);
      }
    }


    private <T extends Storable> void addToParameter(com.gip.xyna.utils.db.Parameter paras, Column col, Object val,
                                                     T storable) throws PersistenceLayerException {
      if (col.type() == ColumnType.INHERIT_FROM_JAVA) {
        if (val instanceof String) {
          String valAsString = (String) val;
          if (valAsString != null) {
            int colSize = getColumnSize(col, storable.getClass());
            if (valAsString.length() > colSize) {
              logger.warn("Provided value for column '" + col.name() + "' in table '" + storable.getTableName()
                  + "' was too long (" + valAsString.length() + "). It will be shortened to fit into column of size " + colSize + ". value = "
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
          if (zippedBlobs) {
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
          throw new XNWH_GeneralPersistenceLayerException("could not write bytes to blob in column " + col.name(), e);
        }
        paras.addParameter(blob);
      } else {
        throw new XNWH_UnsupportedPersistenceLayerFeatureException("column type " + col.type().toString());
      }
    }


    private <T extends Storable> com.gip.xyna.utils.db.Parameter createParasForInsertAndUpdate(Column[] columns,
                                                                                               T storable)
        throws PersistenceLayerException {
      com.gip.xyna.utils.db.ExtendedParameter paras = new com.gip.xyna.utils.db.ExtendedParameter();
      for (Column col : columns) {
        Object val = storable.getValueByColName(col);
        addToParameter(paras, col, val, storable);
      }
      return paras;
    }


    private <T extends Storable> String createUpdateStatement(Column[] columns, T storable)
        throws PersistenceLayerException {
      if (columns.length == 0) {
        throw new XNWH_GeneralPersistenceLayerException("no columns found to persist");
      }
      StringBuilder setter = new StringBuilder();
      setter.append(" set ");
      for (int i = 0; i < columns.length - 1; i++) {
        setter.append(columns[i].name()).append(" = ?, ");
      }
      setter.append(columns[columns.length - 1].name()).append(" = ? ");
      String tableName = storable.getTableName().toLowerCase();
      StringBuilder whereClause =
          new StringBuilder().append(" where ").append(Storable.getPersistable(storable.getClass()).primaryKey())
              .append(" = ?");
      return new StringBuilder().append("update ").append(tableName).append(setter).append(whereClause).toString();
    }


    private Column getColumnForPrimaryKey(Storable s) {
      Column[] cols = Storable.getColumns(s.getClass());
      Persistable p = Storable.getPersistable(s.getClass());
      for (Column col : cols) {
        if (col.name().equals(p.primaryKey())) {
          return col;
        }
      }
      throw new RuntimeException("pk col not found for storable " + s.getClass());
    }


    public <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException {

      //FIXME dieser Code ist aus OraclePersistenceLayer kopiert. Aufgrund der Ähnlichkeiten sollte er
      //aus beiden extrahiert werden
      ensureOpen();
      //überprüfen, ob objekt bereits in db ist
      String sqlString =
          "select count(*) from " + storable.getTableName().toLowerCase() + " where "
              + Storable.getPersistable(storable.getClass()).primaryKey() + " = ?";
      boolean existedBefore = false;
      try {

        // determine whether the object exists. use a trace logger for this since the following statement (update or
        // insert will contain at least the same information)
        com.gip.xyna.utils.db.Parameter parasForCountQuery = new com.gip.xyna.utils.db.ExtendedParameter();
        Column colPK = getColumnForPrimaryKey(storable);
        addToParameter(parasForCountQuery, colPK, storable.getPrimaryKey(), storable);
        sqlUtils.cacheStatement(sqlString);
        int cnt = sqlUtils.queryInt(sqlString, parasForCountQuery);

        UpdateInsert updateOrInsert;
        if (cnt == 0) {
          updateOrInsert = UpdateInsert.insert;
        } else if (cnt == 1) {
          updateOrInsert = UpdateInsert.update;
        } else {
          // this should never happen without malconfiguration
          throw new XNWH_GeneralPersistenceLayerException("result of count-statement \"" + sqlString + "\" ("
              + storable.getPrimaryKey() + ") was " + cnt + ". only 0 or 1 are valid results.");
        }
        int insertRetryCounter = 0;
        int endlessLoopCounter = 0;
        while (updateOrInsert != UpdateInsert.done) {
          if (endlessLoopCounter++ > 100) {
            throw new RuntimeException("unexpectedly long loop"); //sollte nicht passieren
          }
          //solange versuchen, bis insert oder update erfolgreich ist.
          if (updateOrInsert == UpdateInsert.insert) {
            //TODO performance: hier kann man die erstellten parameter und das statement cachen, wenn die whileschleife hier mehrfach vorbei kommt.
            //                  das passiert aber nicht oft, dass hier die while schleife mehrfach den insert-fall durchläuft.
            //insert
            Column[] columns = storable.getColumns();

            String insert = createInsertStatement(columns, storable.getTableName().toLowerCase());
            com.gip.xyna.utils.db.Parameter paras = createParasForInsertAndUpdate(columns, storable);

            try {
              sqlUtils.cacheStatement(insert);
              sqlUtils.executeDML(insert, paras);
            } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
              if (e.getCause() instanceof SQLException) {
                SQLException sqlEx = (SQLException) e.getCause();
                validateConnection();

                //logger.debug(" UniqueConstraintViolation? " + sqlEx.getClass().getSimpleName() + sqlEx.getMessage() );
                //unter Java 6: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException: 
                //Duplicate entry 'corrT_SingleS1_eda1c56f-31' for key 'seriesinformation_correlationId_idx'


                //Leider werden wegen Einführung von jdbc4 unterschiedliche Exceptions geworfen,
                //je nachdem, ob Java 5 oder 6 verwendet wird.
                //Java5: com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException
                //Java6: com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
                //Da hier mit Java 5 kompiliert werden soll, kann leider kein instanceof verwendet 
                //werden, da es sonst die Fehlermeldung "The type java.sql.SQLIntegrityConstraintViolationException 
                //cannot be resolved. It is indirectly referenced from required .class files" gibt.
                //Daher nun Test über den Classname
                String className = sqlEx.getClass().getSimpleName();

                boolean uniqueConstraintViolated = className.contains("MySQLIntegrityConstraintViolationException");
                if (uniqueConstraintViolated) {
                  cnt = sqlUtils.queryInt(sqlString, parasForCountQuery);
                  if (cnt == 0) {
                    //entweder bereits wieder gelöscht (unwahrscheinlich) oder die uniqueconstraintverletzung ist von einer anderen spalte bedingt
                    //updateOrInsert weiterhin auf insert
                    if (++insertRetryCounter > MAX_INSERT_RETRY_COUNTER) {
                      throw e;
                    }
                  } else {
                    //ok update probieren
                    updateOrInsert = UpdateInsert.update;
                    insertRetryCounter = 0;
                  }
                  continue;
                }
              }
              throw e; //falls nicht continue
            }
            existedBefore = false;
          } else if (updateOrInsert == UpdateInsert.update) {
            //update
            Column[] columns = storable.getColumns();

            String updateStmt = createUpdateStatement(columns, storable);
            com.gip.xyna.utils.db.Parameter paras = createParasForInsertAndUpdate(columns, storable);
            //parameter für whereclause adden
            addToParameter(paras, colPK, storable.getPrimaryKey(), storable);

            sqlUtils.cacheStatement(updateStmt);
            int modified = sqlUtils.executeDML(updateStmt, paras);
            if (modified == 0) {
              updateOrInsert = UpdateInsert.insert;
              continue;
            }
            existedBefore = true;
          }
          updateOrInsert = UpdateInsert.done;
        }
        
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        throw new XNWH_GeneralPersistenceLayerException("could not persist storable " + storable, e);
      }

      return existedBefore;

    }


    private void validateConnection() throws PersistenceLayerException {
      try {
        if (sqlUtils != null && sqlUtils.getConnection() != null && !sqlUtils.getConnection().isValid(0)) {
          Connection c = sqlUtils.getConnection();
          closeConnection();  // connection is no longer valid and needs to be closed.
          if (!c.isClosed()) {// closeConnection() keeps the connection open if it is shared
            c.close();        // but since the connection is no longer valid, it has to be closed
          }
        }
      } catch (SQLException e1) {
        logger.warn("Could not validate connection.", e1);
      }
    }


    public PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException {
      return new MySQLPreparedCommand(cmd);
    }


    public <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException {
      return new MySQLPreparedQuery<E>(query);
    }


    public <E> List<E> query(PreparedQuery<E> queryInterface, Parameter parameter, int maxRows)
        throws PersistenceLayerException {
      MySQLPreparedQuery<E> query = (MySQLPreparedQuery<E>) queryInterface;
      return this.query(queryInterface, parameter, maxRows, query.getReader());
    }


    public <E> List<E> query(PreparedQuery<E> queryInterface, Parameter parameter, int maxRows,
                             final com.gip.xyna.xnwh.persistence.ResultSetReader<? extends E> resultSetReader)
        throws PersistenceLayerException {
      if (!(queryInterface instanceof MySQLPreparedQuery)) {
        throw new XNWH_IncompatiblePreparedObjectException(PreparedQuery.class.getSimpleName());
      }
      MySQLPreparedQuery<E> query = (MySQLPreparedQuery<E>) queryInterface;
      ensureOpen();
      com.gip.xyna.utils.db.Parameter paras = null;

      boolean[] isLike = query.getQuery().getLikeParameters();

      if (parameter != null) {
        for (int i = 0; i < parameter.size(); i++) {
          if (!isLike[i]) {
            if (parameter.get(i) instanceof Boolean) {
              paras = new ExtendedParameter();
              break;
            }
          }
        }
        if (paras == null) {
          paras = new com.gip.xyna.utils.db.Parameter();
        }
        for (int i = 0; i < parameter.size(); i++) {
          if (parameter.get(i) instanceof String) {
            paras.addParameter(SelectionParser.escapeParams((String)parameter.get(i), isLike[i], new EscapeForMySQL()));
          } else if (parameter.get(i) instanceof Boolean) {
            paras.addParameter(new BooleanWrapper((Boolean) parameter.get(i)));
          } else {
            addToParameterWithArraySupportAsStrings(paras, parameter.get(i));
          }
        }
      }

      String sqlQuery = query.getQuery().getSqlString();

      //Umwandlung zu rlike, da MariaDB regexp_like() nicht unterstützt
      sqlQuery = modifyFunction(sqlQuery, PersistenceExpressionVisitors.QueryFunctionStore.REGEXP_LIKE_SQL_FUNCTION, "%Column% RLIKE (%Params%)" );

      //TODO cachen
      if (maxRows == 1 && transactionProperties != null
          && transactionProperties.contains(TransactionProperty.selectRandomElement())) {
        if (!sqlQuery.toLowerCase().contains("order by")) { //else altes order by beibehalten
          if (sqlQuery.toLowerCase().endsWith("for update")) { //vor das "for update" einfügen
            sqlQuery =
                sqlQuery.substring(0, sqlQuery.length() - "for update".length())
                    + " order by rand() limit 0, 1 for update";
          } else {
            sqlQuery += " order by rand() limit 0, 1";
          }
        }
      } else if (maxRows > -1 && maxRows < Integer.MAX_VALUE) {
        //beschränkung des ergebnisses. das ist das einzige mysql spezifische
        //limit ist vor "for update", aber nach allem anderen.
        //http://dev.mysql.com/doc/refman/5.0/en/select.html
        sqlQuery = sqlQuery.trim();
        if (sqlQuery.toLowerCase().endsWith("for update")) {
          sqlQuery =
              sqlQuery.substring(0, sqlQuery.length() - "for update".length()) + " limit 0, " + maxRows + " for update";
        } else {
          sqlQuery += " limit 0, " + maxRows;
        }
      }
      try {
        sqlUtils.cacheStatement(sqlQuery);
        ArrayList<E> list = sqlUtils.query(sqlQuery, paras, new ResultSetReaderWrapper<E>(resultSetReader, zippedBlobs, tableToClassMap.get(query.getTable().toLowerCase())));
        return list;
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);        
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        validateConnection();
        throw new XNWH_GeneralPersistenceLayerException("query \"" + sqlQuery + "\" [" + paras
            + "] could not be executed.", e);
      }
    }


    /**
     * Passt die Schreibweise einer SQL-Funktion (z.B. regexp_like) an MySQL an.
     */
    private String modifyFunction(String sqlQuery, String sqlFunction, String replacement) {
      if (sqlQuery.contains(sqlFunction)) {
        String preExpr = "([\\s\\(]+)"; //Leerzeichen oder Klammer stehen am Anfang
        String params = "([^,]*),([^)]*)"; //Parameter der SQL-Funktion
        Pattern pattern = Pattern.compile(preExpr +"\\Q" + sqlFunction + "\\E" +"\\s*\\(" + params + "\\)",Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sqlQuery);
          if (matcher.find()) {
            replacement = replacement.replace("%Column%", "$2").replace("%Params%", "$3");
            sqlQuery = matcher.replaceAll("$1" + replacement); //eigentliche Ersetzung
        }
      }
      return sqlQuery;
    }


    /**
     * Note that this only does any locking if autocommit is disabled
     */
    public <T extends Storable> void queryOneRowForUpdate(final T storable) throws PersistenceLayerException,
        XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      queryOneRowInternally(storable, true);
    }


    public <T extends Storable> void queryOneRow(final T storable) throws PersistenceLayerException,
        XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      queryOneRowInternally(storable, false);
    }


    private <T extends Storable> void queryOneRowInternally(final T storable, boolean forUpdate)
        throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      ensureOpen();
      StringBuilder selectString =
          new StringBuilder().append("select * from ").append(storable.getTableName().toLowerCase()).append(" where ")
              .append(Storable.getPersistable(storable.getClass()).primaryKey()).append(" = ?");
      if (forUpdate) {
        selectString.append(" for update");
      }
      try {
        com.gip.xyna.utils.db.Parameter paras = new com.gip.xyna.utils.db.ExtendedParameter();
        Column colPK = getColumnForPrimaryKey(storable);
        addToParameter(paras, colPK, storable.getPrimaryKey(), storable);
        sqlUtils.cacheStatement(selectString.toString());
        T result = (T) sqlUtils.queryOneRow(selectString.toString(), paras, new ResultSetReaderWrapper(storable.getReader(), zippedBlobs, storable.getClass()));
        if (result == null) {
          throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(String.valueOf(storable.getPrimaryKey()),
                                                          storable.getTableName());
        }
        storable.setAllFieldsFromData(result);
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);        
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        validateConnection();
        throw new XNWH_GeneralPersistenceLayerException("query \"" + selectString + "\" [" + storable.getPrimaryKey()
            + "] could not be executed.", e);
      }
    }


    public <E> E queryOneRow(PreparedQuery<E> query, Parameter parameter) throws PersistenceLayerException {
      List<E> l = query(query, parameter, 1);
      if (l.size() > 0) {
        return l.get(0);
      } else {
        return null;
      }
    }


    public void rollback() throws PersistenceLayerException {
      ensureOpen();
      try {
        sqlUtils.rollback();
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        validateConnection();
        throw new XNWH_GeneralPersistenceLayerException("could not rollback transaction.", e);
      }
    }


    private Set<TransactionProperty> transactionProperties;


    public void setTransactionProperty(TransactionProperty property) {
      if (transactionProperties == null) {
        transactionProperties = new HashSet<TransactionProperty>();
      }
      transactionProperties.add(property);
    }


    public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> arg0) throws PersistenceLayerException {
      try {
        connectionPool.ensureConnectivity(sqlUtils.getConnection());
      } catch (SQLException e) {
        throw new XNWH_RetryTransactionException(e);
      }
    }


    public <T extends Storable> void removeTable(Class<T> arg0, Properties arg1) throws PersistenceLayerException {
      tableToClassMap.remove(Storable.getPersistable(arg0).tableName().toLowerCase());
      for (Column col : Storable.getColumns(arg0)) {
        columnMap.remove(col);
      }
    }

  }


  public String getInformation() {
    //user pw jdbc:mysql://x.x.x.x/schema), poolsize, timeout(ms)
    return toString() + " (" + username + "@" + url + " timeout=" + timeout + ")";
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


  private <T extends Storable<?>> int getColumnSize(Column col, Class<T> clazz) {
    MySQLColumnInfo colInfo = columnMap.get(col);
    if (colInfo == null) {
      //TODO cache befüllen oder sicherstellen, dass dieser fall nicht unerwartet auftritt.
      if (logger.isTraceEnabled()) {
        logger.trace("Column " + col.name() + " of " + Storable.getPersistable(clazz).tableName() + " not found in colInfo cache. (classloader=" + clazz.getClassLoader() + ")");
      }
      if (col.size() > 0) {
        if (col.type() == ColumnType.INHERIT_FROM_JAVA && Storable.getColumn(col, clazz).getType() == byte[].class) {
          return col.size() * 6 +1; //wenn nach varchar geschrieben werden soll, ist das relevant. darstellung ist dann [b, b2, b3], maximaler verbrauch pro byte ist ", -123"
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
        long l = colInfo.type.size;
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


  // FIXME duplicated class from OraclePL
  private static class ConfigurableGZIPOutputStream extends GZIPOutputStream {

    public ConfigurableGZIPOutputStream(OutputStream out, int compressionLevel, int buffersize) throws IOException {
      super(out, buffersize);
      def.setLevel(compressionLevel);
    }
    
  }

  
  //FIXME duplicated class from OraclePL
  private static class ZippedBlob extends BLOB {
    
    public ZippedBlob() {
      super(BLOB.ZIPPED);
    }

    @Override
    protected DeflaterOutputStream createZippedOutputStream(OutputStream os) throws IOException {
      return new ConfigurableGZIPOutputStream(os, Deflater.BEST_SPEED, 512);
    }
    
    public String toString() {
      return super.toString();
    }
    
  }
  
  
  //FIXME duplicated class from OraclePL
  private static class ResultSetReaderWrapper<T> implements ResultSetReader<T> {

    private final com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T> innerReader;
    private final boolean zippedBlobs; 
    @SuppressWarnings("rawtypes")
    private Class<? extends Storable> storableClass = null;
    
    public ResultSetReaderWrapper(com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T> reader, boolean zippedBlobs) {
      this.innerReader = reader;
      this.zippedBlobs = zippedBlobs;
    }
    
    public ResultSetReaderWrapper(com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T> reader, boolean zippedBlobs, Class<? extends Storable> storableClass) {
      this(reader, zippedBlobs);
      this.storableClass = storableClass;
    }
    
    public T read(ResultSet rs) throws SQLException {
      if (zippedBlobs) {
        return innerReader.read(new ResultSetWrapperReadingZippedBlobs(rs, storableClass));
      } else {
        return innerReader.read(rs);
      }
    }
    
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
  
  private static class EscapeForMySQL implements EscapeParams {

    public String escapeForLike(String toEscape) {
      if (toEscape == null) {
        return toEscape;
      }
      
      toEscape = toEscape.replaceAll("%", "\\\\%");
      toEscape = toEscape.replaceAll("_", "\\\\_");
      return toEscape;
    }

    public String getWildcard() {
      return "%";
    }
    
  }


  public PersistenceLayerConnection getConnection(PersistenceLayerConnection shareConnectionPool) throws PersistenceLayerException {
    if (shareConnectionPool instanceof MySQLPersistenceLayerConnection) {
      return new MySQLPersistenceLayerConnection((MySQLPersistenceLayerConnection) shareConnectionPool);
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

}
