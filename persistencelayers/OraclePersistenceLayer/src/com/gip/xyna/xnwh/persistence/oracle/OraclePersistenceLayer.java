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
package com.gip.xyna.xnwh.persistence.oracle;



import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionCouldNotBeClosedException;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.db.ExtendedParameter;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.ResultSetReaderFunction;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.WrappedConnection;
import com.gip.xyna.utils.db.exception.UnexpectedParameterException;
import com.gip.xyna.utils.db.types.BLOB;
import com.gip.xyna.utils.db.types.BooleanWrapper;
import com.gip.xyna.utils.db.types.CLOBString;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_ConnectionClosedException;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
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
import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabasePersistenceLayerConnectionWithAlterTableSupport;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabasePersistenceLayerWithAlterTableSupportHelper;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;
import com.gip.xyna.xnwh.pools.OraclePoolType;
import com.gip.xyna.xnwh.pools.PoolDefinition;
import com.gip.xyna.xnwh.pools.TypedConnectionPoolParameter;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser.EscapeParameters;
import com.gip.xyna.xnwh.utils.SQLErrorHandling;
import com.gip.xyna.xnwh.utils.SQLErrorHandlingLogger;
import com.gip.xyna.xnwh.utils.SQLErrorHandlingLogger.ErrorCodeHandlingElement;
import com.gip.xyna.xnwh.utils.SQLErrorHandlingLogger.SQLErrorHandlingLoggerBuilder;


public class OraclePersistenceLayer implements PersistenceLayer, Clustered {

  private final static Logger logger = CentralFactoryLogging.getLogger(OraclePersistenceLayer.class);
  private final static Pattern NUMBERS_PATTERN = Pattern.compile("^\\d+$");
  
  private static Set<String> instanceIdentifiers = new HashSet<String>();

  private static final int MAX_INSERT_RETRY_COUNTER = 3; 


  /*
   * konfigurierbare Settings
   */
  private boolean automaticColumnTypeWidening = false;
  private static final String KEY_AUTOMATIC_COLUMN_TYPE_WIDENING = "automaticColumnTypeWidening";

  private String nameSuffix;
  private static final String KEY_NAME_SUFFIX = "nameSuffix";
  private static final String KEY_CONNECT_TIMEOUT = "connectTimeout";
  private static final String KEY_SOCKET_TIMEOUT = "socketTimeout";
  private static final String KEY_CONNECTION_CREATION_RETRIES = "connectionCreationRetries";
  
  private static final String KEY_ZIPPED_BLOBS = "zippedBlobs";
  private static final String KEY_USE_PL_LOGGER = "usePersistenceLayerLogger";
  private static final String KEY_USE_SCHEMA = "schema";
  
  /**
   * falls true, werden alle sql-debugs mit {@link #logger} gemacht, ansonsten mit einem dynamischen logger, der sich
   * aus der caller-class ergibt und dann den namen "xyna.sql.&lt;callerclass&gt;" bekommt.
   */
  private boolean usePersistenceLayerLogger = false;
  private String schema;

  private String username;
  private String url;
  private int timeout;
  private ConnectionPool pool;
  private ConnectionPool dedicatedPool;

  private long clusterProviderId = -1;
  private ClusterProvider clusterInstance;
  private boolean zippedBlobs = false;
  private Long pliID;
  
  private OraclePLClusterStateChangeHandler clusterStateChangeHandler = new OraclePLClusterStateChangeHandler();

  public PersistenceLayerConnection getConnection() throws PersistenceLayerException {
    return new OraclePersistenceLayerConnection();
  }
  
  public PersistenceLayerConnection getDedicatedConnection() throws PersistenceLayerException {
    return new OraclePersistenceLayerConnection(true);
  }

  private enum UpdateInsert {
    update, insert, done;
  }
  
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
  
  public String[] getParameterInformation() {
    return new String[] {
      "poolname (see listconnectionpools)",
      "timeout(ms)",
      "additional optional parameters are key value pairs (key=value). supported keys are: \n"
          + KEY_AUTOMATIC_COLUMN_TYPE_WIDENING + " (true/false), \n"
          + KEY_NAME_SUFFIX + " (required for clustering), \n"
          + KEY_ZIPPED_BLOBS + " (true/false), \n"
          + KEY_USE_PL_LOGGER + " (true=>better performance, false=>better configuration possibilities), \n"
          + KEY_USE_SCHEMA + " (name of the db schema the check for table/view existence is done against)."};
  }


  private static enum OracleSqlBaseType {
    NUMBER, FLOAT, TIME, TEXT_ENCODED, BINARY, OTHER;

    /**
     * gibt zurück, ob der übergebene typ größergleich ist. OTHER.isCompatibleTo(TIME) = false
     * NUMBER.isCompatibleTo(TEXT_ENCODED) = true
     * @param otherType
     * @return
     */
    public boolean isCompatibleTo(OracleSqlBaseType otherType) {
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

  private static enum OracleSqlType {
    
    CHAR(2000, OracleSqlBaseType.TEXT_ENCODED, true),
    VARCHAR(4000, OracleSqlBaseType.TEXT_ENCODED, true),
    VARCHAR2(4000, OracleSqlBaseType.TEXT_ENCODED, true),
    NCHAR(2000, OracleSqlBaseType.TEXT_ENCODED, true),
    NVARCHAR(4000, OracleSqlBaseType.TEXT_ENCODED, true),
    LONG(2*1024*1024*1024, OracleSqlBaseType.TEXT_ENCODED),
    
    NUMBER(38, OracleSqlBaseType.NUMBER),
    BINARYFLOAT(4, OracleSqlBaseType.BINARY),
    BINARYDOUBLE(8, OracleSqlBaseType.BINARY),

    DATE(3, OracleSqlBaseType.TIME),
    TIMESTAMP(4, OracleSqlBaseType.TIME),
    TIMESTAMP_WITH_TIME_ZONE(4, OracleSqlBaseType.TIME),
    TIMESTAMP_WITH_LOCAL_TIME_ZONE(4, OracleSqlBaseType.TIME), // TODO check these

    BLOB(128L*1024L*1024L*1024L*1024L, OracleSqlBaseType.BINARY), // can contain up to 128 TB of data
    CLOB(128L*1024L*1024L*1024L*1024L, OracleSqlBaseType.TEXT_ENCODED),
    NCLOB(128L*1024L*1024L*1024L*1024L, OracleSqlBaseType.TEXT_ENCODED),
    BFILE(2000, OracleSqlBaseType.BINARY),

    RAW(4000, OracleSqlBaseType.BINARY),
    LONG_RAW(128L*1024L*1024L*1024L*1024L, OracleSqlBaseType.BINARY), // this is deprecated, use the LOB types instead

    UNKNOWN(0, null);

    private boolean dependentOnSizeSpecification;
    private long size;
    private OracleSqlBaseType baseType;


    private OracleSqlType(long size, OracleSqlBaseType baseType) {
      this(size, baseType, false);
    }


    private OracleSqlType(long size, OracleSqlBaseType baseType, boolean dependentOnSizeSpecification) {
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


    protected OracleSqlBaseType getBaseType() {
      return baseType;
    }


    /**
     * gibt zu einem typ alle damit kompatiblen typen zurück (die "größer" sind). beispiel: BLOB.getCompatibleTypes =>
     * BLOB, MEDIUMBLOB, LONGBLOB
     * @return
     */
    protected List<OracleSqlType> getCompatibleTypes() {
      List<OracleSqlType> types = new ArrayList<OracleSqlType>();
      for (OracleSqlType type : OracleSqlType.values()) {
        if (baseType.isCompatibleTo(type.baseType)) {
          if (size <= type.size) {
            types.add(type);
          }
        }
      }
      return types;
    }
  }
  
  private int getColumnSize(Column col) {
    OracleSQLColumnInfo colInfo = columnMap.get(col);
    if (colInfo == null) {
      if (col.size() > 0) {
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
        return (int)l;
      } else {
        long l = colInfo.type.size;
        if (logger.isTraceEnabled()) {
          logger.trace("size for col " + col.name() + " = " + l);
        }
        if (l > Integer.MAX_VALUE) {
          return Integer.MAX_VALUE;
        }
        return (int)l;
      }
    }
  }
  
  private OracleSqlType getColumnTypeOfStringCol(Column col, String value) {
    OracleSQLColumnInfo colInfo = columnMap.get(col);
    if (colInfo == null) {
      if (value.length() > OracleSqlType.VARCHAR2.size) {
        return OracleSqlType.CLOB;
      }
      if (value.length() <= OracleSqlType.VARCHAR2.size / 2) {
        return OracleSqlType.VARCHAR2;
      }
      try {
        //TODO konfigurierbares encoding? ist das hier überhaupt das encoding der datenbank?
        int lbytes = value.getBytes(Constants.DEFAULT_ENCODING).length;
        if (lbytes > OracleSqlType.VARCHAR2.size) {
          return OracleSqlType.CLOB;
        }
        return OracleSqlType.VARCHAR2;
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    return colInfo.type;
  }

  public void init(Long pliID, String... args) throws PersistenceLayerException {
    this.pliID = pliID;
    if (args == null || args.length < 2) {
      StringBuilder sb = new StringBuilder();
      String parts[] = getParameterInformation();
      for (int i = 0; i < parts.length; i++) {
        sb.append(parts[i]);
        if (i < parts.length - 1) {
          sb.append(", ");
        }
      }
      throw new IllegalArgumentException("at least 2 parameters expected: " + sb.toString());
    }
    
    ConnectionPoolManagement poolMgmt = ConnectionPoolManagement.getInstance();
    
    PoolDefinition regularPoolDefinition;
    PoolDefinition dedicatedPoolDefinition;
    String poolName;
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
      poolName = getPoolName(pliID);
      regularPoolDefinition = poolMgmt.getConnectionPoolDefinition(poolName);
      dedicatedPoolDefinition = poolMgmt.getConnectionPoolDefinition(getDedicatedPoolName(pliID));
    } else {
      poolName = handleParams(args);
      regularPoolDefinition = poolMgmt.getConnectionPoolDefinition(poolName);

      if (regularPoolDefinition == null) {
        throw new XNWH_GeneralPersistenceLayerException("Connection pool <" + poolName + "> not found!");
      }
      if (!regularPoolDefinition.getType().equals(OraclePoolType.POOLTYPE_IDENTIFIER)) {
        // only warn as it might be a custom poolType made for oracle-PL usage
        logger.warn("The connection pool configured for persistence layer instance " + pliID + " is of type "
            + regularPoolDefinition.getType() + ". It must be of type " + OraclePoolType.POOLTYPE_IDENTIFIER + ".");
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
      throw new XNWH_GeneralPersistenceLayerException("Connection pool <" + poolName + "> not found!");
    }
    
    url = regularPoolDefinition.getConnectstring();
    username = regularPoolDefinition.getUser();
      
    // catch any RuntimeException or Error to be able to rollback the insertion of the instanceidentifier
    // into the static instanceIdentifiers map
    try {
      try {
        pool = poolMgmt.getConnectionPool(regularPoolDefinition.getName());
        dedicatedPool = poolMgmt.getConnectionPool(dedicatedPoolDefinition.getName());
        clusterStateChangeHandler.setId(getName());
        clusterStateChangeHandler.addPool(pool);
        clusterStateChangeHandler.addPool(dedicatedPool);
        
      } catch( NoConnectionAvailableException e ) {
        //FIXME besser behandeln!
        throw new XNWH_RetryTransactionException(e);
      }
      
      if (logger.isTraceEnabled()) {
        logger.trace("got pool: " + pool);
        logger.trace("got dedicated pool: " + dedicatedPool);
      }

      if (nameSuffix != null) {
        try {
          XynaClusteringServicesManagement.getInstance().registerClusterableComponent(this);
        } catch (XFMG_ClusterComponentConfigurationException e) {
          throw new RuntimeException(e);
        }
      }
      
      if (usePersistenceLayerLogger) {
        sqlUtilsLoggerInfo = buildSQLLogger(logger, Level.INFO);
        sqlUtilsLoggerDebug = buildSQLLogger(logger, Level.DEBUG);
        sqlUtilsLoggerTrace = buildSQLLogger(logger, Level.TRACE);
      } else {
        sqlUtilsLoggerInfo = buildSQLLogger(Level.INFO);
        sqlUtilsLoggerDebug = buildSQLLogger(Level.DEBUG);
        sqlUtilsLoggerTrace = buildSQLLogger(Level.TRACE);
      }

    } catch (RuntimeException e) {
      if (nameSuffix != null) {
        synchronized (instanceIdentifiers) {
          instanceIdentifiers.remove(nameSuffix);
        }
      }
      throw e;
    } catch (Error e) {
      if (nameSuffix != null) {
        synchronized (instanceIdentifiers) {
          instanceIdentifiers.remove(nameSuffix);
        }
      }
      throw e;
    }

  }
  
  
  private String handleParams(String[] args) throws XNWH_GeneralPersistenceLayerException {
    for (int i = 2; i < args.length; i++) {
      String[] keyValue = args[i].split("=");
      if (keyValue.length != 2) {
        throw new XNWH_GeneralPersistenceLayerException("invalid key value pair provided: " + args[i]);
      }
      String key = keyValue[0];
      if (key.equals(KEY_AUTOMATIC_COLUMN_TYPE_WIDENING)) {
        automaticColumnTypeWidening = Boolean.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_AUTOMATIC_COLUMN_TYPE_WIDENING + " to " + automaticColumnTypeWidening);
        }
      } else if (key.equals(KEY_NAME_SUFFIX)) {
        nameSuffix = keyValue[1].trim();
        synchronized (instanceIdentifiers) {
          if (instanceIdentifiers.contains(nameSuffix)) {
            throw new XNWH_GeneralPersistenceLayerException("Instance identifier <" + nameSuffix + "> is not unique!");
          }
          instanceIdentifiers.add(nameSuffix);
        }
      } else if (key.equals(KEY_ZIPPED_BLOBS)) {
        zippedBlobs = Boolean.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_ZIPPED_BLOBS + " to " + zippedBlobs);
        }
      } else if (KEY_USE_PL_LOGGER.equals(key)) {
        usePersistenceLayerLogger = Boolean.valueOf(keyValue[1]);
      } else if (KEY_USE_SCHEMA.equals(key)) {
        schema = keyValue[1];
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
    boolean useDurableStatementCache = false;
    int connectionCreationRetries = 0;
    
    for (int i = 5; i < args.length; i++) {
      String[] keyValue = args[i].split("=");
      if (keyValue.length != 2) {
        throw new XNWH_GeneralPersistenceLayerException("invalid key value pair provided: " + args[i]);
      }
      String key = keyValue[0];
      if (key.equals(KEY_AUTOMATIC_COLUMN_TYPE_WIDENING)) {
        automaticColumnTypeWidening = Boolean.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_AUTOMATIC_COLUMN_TYPE_WIDENING + " to " + automaticColumnTypeWidening);
        }
      } else if (key.equals(KEY_NAME_SUFFIX)) {
        nameSuffix = keyValue[1].trim();
        synchronized (instanceIdentifiers) {
          if (instanceIdentifiers.contains(nameSuffix)) {
            throw new XNWH_GeneralPersistenceLayerException("Instance identifier <" + nameSuffix + "> is not unique!");
          }
          instanceIdentifiers.add(nameSuffix);
        }
      } else if (key.equals(KEY_CONNECT_TIMEOUT)) {
        connectTimeout = Integer.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_CONNECT_TIMEOUT + " to " + connectTimeout);
        }
      } else if (key.equals(KEY_SOCKET_TIMEOUT)) {
        socketTimeout = Integer.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_SOCKET_TIMEOUT + " to " + socketTimeout);
        }
      } else if (key.equals(KEY_CONNECTION_CREATION_RETRIES)) {
        connectionCreationRetries = Integer.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_CONNECTION_CREATION_RETRIES + " to " + connectionCreationRetries);
        }
      } else if (key.equals(KEY_ZIPPED_BLOBS)) {
        zippedBlobs = Boolean.valueOf(keyValue[1]);
        if (logger.isDebugEnabled()) {
          logger.debug("set " + KEY_ZIPPED_BLOBS + " to " + zippedBlobs);
        }
      } else if (KEY_USE_PL_LOGGER.equals(key)) {
        usePersistenceLayerLogger = Boolean.valueOf(keyValue[1]);
      } else if (KEY_USE_SCHEMA.equals(key)) {
        schema = keyValue[1];
      } else {
        throw new XNWH_GeneralPersistenceLayerException("unknown key: " + key);
      }
    }
    String username = args[0];
    String password = args[1];
    String url = args[2];
    
    timeout = Integer.valueOf(args[4]);

    String prefixForId = getPoolName(pliID);
    
    TypedConnectionPoolParameter tcpp = new TypedConnectionPoolParameter(new OraclePoolType().getName());
    tcpp.name(prefixForId).connectString(url).user(username).password(password).size(Integer.valueOf(args[3]));
    tcpp.additionalParams( StringParameter.buildObjectMap()
                           .put(OraclePoolType.CONNECT_TIMEOUT, new Duration(connectTimeout, TimeUnit.SECONDS) )
                           .put(OraclePoolType.SOCKET_TIMEOUT, new Duration(socketTimeout, TimeUnit.SECONDS) )
                           .build() );
    return tcpp;
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

  
  private String getPoolName(Long pliID) {
    return OraclePersistenceLayer.class.getSimpleName() + "_" + pliID;
  }
  
  
  private String getDedicatedPoolName(Long pliID) {
    return getPoolName(pliID) + "_dedicated";
  }
  

  
  private static interface ErrorlessCallable<T> {
    
    public T call();
    
  }
  

  private static class SQLUtilsWithRetryOperationSupport extends SQLUtils {

    private int retryCnt = 5;

    public SQLUtilsWithRetryOperationSupport(Connection connection, SQLErrorHandlingLogger sqlUtilsLogger) {
      super(connection, sqlUtilsLogger);
    }


    private <T> T exec(final ErrorlessCallable<T> callable) {
      for (int i = 1; i <= retryCnt; i++) {
        try {
          return callable.call();
        } catch (com.gip.xyna.xnwh.exception.SQLRetryOperationRuntimeException e) {
          if (i == retryCnt) {
            throw new com.gip.xyna.xnwh.exception.SQLRetryOperationRuntimeException(e);
          }
        }
      }
      throw new RuntimeException(); //unreachable
    }


    @Override
    public boolean closeConnection() {
      return exec(new ErrorlessCallable<Boolean>() {

        public Boolean call() {
          return SQLUtilsWithRetryOperationSupport.super.closeConnection();
        }

      });
    }


    public boolean commit() {
      return exec(new ErrorlessCallable<Boolean>() {

        public Boolean call() {
          return SQLUtilsWithRetryOperationSupport.super.commit();
        }

      });
    }


    @Override
    public boolean executeCall(final String arg0, final com.gip.xyna.utils.db.Parameter arg1) {
      return exec(new ErrorlessCallable<Boolean>() {

        public Boolean call() {
          return SQLUtilsWithRetryOperationSupport.super.executeCall(arg0, arg1);
        }

      });
    }


    @Override
    public boolean executeDDL(final String arg0, final com.gip.xyna.utils.db.Parameter arg1) {
      return exec(new ErrorlessCallable<Boolean>() {

        public Boolean call() {
          return SQLUtilsWithRetryOperationSupport.super.executeDDL(arg0, arg1);
        }

      });
    }


    @Override
    public int executeDML(final String arg0, final com.gip.xyna.utils.db.Parameter arg1) {
      return exec(new ErrorlessCallable<Integer>() {

        public Integer call() {
          return SQLUtilsWithRetryOperationSupport.super.executeDML(arg0, arg1);
        }

      });
    }

    public <T> ArrayList<T> queryNoRetryOperation(final String arg0, final com.gip.xyna.utils.db.Parameter arg1,
                                  final ResultSetReader<T> arg2) {
      return SQLUtilsWithRetryOperationSupport.super.query(arg0, arg1, arg2);
    }
    

    @Override
    public <T> ArrayList<T> query(final String arg0, final com.gip.xyna.utils.db.Parameter arg1,
                                  final ResultSetReader<T> arg2) {
      return exec(new ErrorlessCallable<ArrayList<T>>() {

        public ArrayList<T> call() {
          return SQLUtilsWithRetryOperationSupport.super.query(arg0, arg1, arg2);
        }

      });
    }


    @Override
    public Integer query(final String arg0, final com.gip.xyna.utils.db.Parameter arg1,
                         final ResultSetReaderFunction arg2) {
      return exec(new ErrorlessCallable<Integer>() {

        public Integer call() {
          return SQLUtilsWithRetryOperationSupport.super.query(arg0, arg1, arg2);
        }

      });
    }


    @Override
    public Integer queryInt(final String arg0, final com.gip.xyna.utils.db.Parameter arg1) {
      return exec(new ErrorlessCallable<Integer>() {

        public Integer call() {
          return SQLUtilsWithRetryOperationSupport.super.queryInt(arg0, arg1);
        }

      });
    }


    @Override
    public <T> T queryOneRow(final String arg0, final com.gip.xyna.utils.db.Parameter arg1,
                             final ResultSetReader<T> arg2) {
      return exec(new ErrorlessCallable<T>() {

        public T call() {
          return SQLUtilsWithRetryOperationSupport.super.queryOneRow(arg0, arg1, arg2);
        }

      });
    }


    @Override
    public Boolean queryOneRow(final String arg0, final com.gip.xyna.utils.db.Parameter arg1,
                               final ResultSetReaderFunction arg2) {
      return exec(new ErrorlessCallable<Boolean>() {

        public Boolean call() {
          return SQLUtilsWithRetryOperationSupport.super.queryOneRow(arg0, arg1, arg2);
        }

      });
    }


    @Override
    public boolean rollback() {
      return exec(new ErrorlessCallable<Boolean>() {

        public Boolean call() {
          return SQLUtilsWithRetryOperationSupport.super.rollback();
        }

      });
    }

  }

  private SQLErrorHandlingLogger sqlUtilsLoggerInfo;
  private SQLErrorHandlingLogger sqlUtilsLoggerDebug;
  private SQLErrorHandlingLogger sqlUtilsLoggerTrace;
  
  
  private SQLUtilsWithRetryOperationSupport createSQLUtils(boolean isDedicated) throws PersistenceLayerException {
    ConnectionPool cp = pool;
    String clientInfo = "xyna oraclepersistencelayer "+pliID +" "+getName();
    
    if (isDedicated) {
      cp = dedicatedPool;
      clientInfo += " dedicated";
    }

    try {
      WrappedConnection con = cp.getConnection(timeout, clientInfo);
      SQLUtilsWithRetryOperationSupport sqlUtils = new SQLUtilsWithRetryOperationSupport(con, sqlUtilsLoggerDebug) {

        @Override
        public boolean closeConnection() {
          setLogger(sqlUtilsLoggerInfo); //damit das CLOSE auf info geloggt wird
          return super.closeConnection();
        }
        
      };
      sqlUtils.setIncludeResultSetReaderNullElements(false);
      sqlUtilsLoggerInfo.logSQL("OPEN");
      
      return sqlUtils;
     
    } catch ( NoConnectionAvailableException e) {
      switch( e.getReason() ) {
        case PoolExhausted:
          //Retry ist in jedem Fall sinnvoll
          throw new XNWH_RetryTransactionException(e);
        case ConnectionRefused:
        case Other:  //Ursache nicht entscheidbar
        case NetworkUnreachable:
        case Timeout:
          //Retries könnten erfolgreich sein
          //Interconnect checken, da dieser von diesen Fehlern betroffen sein könnte
          ClusterProvider clusterInstance = getClusterInstance();
          if (clusterInstance != null) {
            clusterInstance.checkInterconnect();
          }
          throw new XNWH_RetryTransactionException(e); //Retries werden evtl. von Disconnect beendet
        case PoolClosed:
        case URLInvalid:
        case UserOrPasswordInvalid:
          //Retries sind nicht sinnvoll, hat auch nichts mit Interconnect zu tun
          throw new XNWH_GeneralPersistenceLayerException("could not get DB Connection from " + cp.getId(), e);
        case PoolBroken:
          throw new XNWH_GeneralPersistenceLayerException("could not get DB Connection from " + cp.getId(), e);
        default:
          throw new XNWH_GeneralPersistenceLayerException("could not get DB Connection from " + cp.getId(), e);
      }
    } catch( Exception e ) {
      //unerwartete andere Exceptions
      throw new XNWH_GeneralPersistenceLayerException("could not get DB Connection from " + cp.getId(), e);
    }
  }


  private static void closeSQLUtils(SQLUtils sqlUtils) throws PersistenceLayerException {
    try {
      sqlUtils.closeConnection();
    } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
      throw new XNWH_RetryTransactionException(e);
    } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
      throw new XNWH_GeneralPersistenceLayerException("problem closing connection", e);
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


  private String createInsertStatement(Column[] columns, String tableName) throws PersistenceLayerException {

    StringBuilder cols = new StringBuilder();
    StringBuilder vals = new StringBuilder();
    if (columns.length == 0) {
      throw new XNWH_GeneralPersistenceLayerException("no columns found to persist");
    }
    for (int i = 0; i < columns.length - 1; i++) {
      cols.append(escape(columns[i].name())).append(", ");
      vals.append("?, ");
    }
    cols.append(escape(columns[columns.length - 1].name()));
    vals.append("?");

    return new StringBuilder().append("insert into ").append(escape(tableName)).append(" (").append(cols.toString())
                    .append(") values (").append(vals.toString()).append(")").toString();

  }


  //die clobstring klasse wird in den database-utils unterstützt, benutzt aber die oracle-jars. weil die utils in ihrem classloader die oracle-jars nicht kennen, ist dies der workaround.
  private static class ExtendedCLOBString extends CLOBString {

    public ExtendedCLOBString(String string) {
      super(string);
    }


    public void setCLOB(PreparedStatement stmt, int i) throws SQLException {
      try {
        if (stmt instanceof oracle.jdbc.OraclePreparedStatement) {
          //oracle.jdbc.OraclePreparedStatement
          //nicht oracle.jdbc.driver.OraclePreparedStatement; -> ClassCastException
          ((oracle.jdbc.OraclePreparedStatement) stmt).setStringForClob(i, get());
        } else {
          stmt.setString(i, get());
        }
      } catch (RuntimeException e) {
        //keine Oracle-Umgebung
        //TODO bessere Kontrolle der Umgebung
        logger.warn("Could not set clob", e);
        stmt.setString(i, get());
      }
    }

  }
  
  private static class ConfigurableGZIPOutputStream extends GZIPOutputStream {

    public ConfigurableGZIPOutputStream(OutputStream out, int compressionLevel, int buffersize) throws IOException {
      super(out, buffersize);
      def.setLevel(compressionLevel);
    }
    
  }

  private static class ZippedBlob extends BLOB {
    
    public ZippedBlob() {
      super(BLOB.ZIPPED);
    }


    @Override
    protected DeflaterOutputStream createZippedOutputStream(OutputStream os) throws IOException {
      return new ConfigurableGZIPOutputStream(os, Deflater.BEST_SPEED, 512);
    }


    public String toString() {
      String logString = super.toString();
      if (logString == null) {
        return null;
      } else {
        // TODO actually this should be done within the superclass but the consequences are not completely clear
        return logString.replaceFirst(Pattern.quote(getClass().getName()),
                                      Matcher.quoteReplacement(getClass().getSimpleName()));
      }
    }

  }

  /*private static class NoConnectionAvailableReasonDetectorImpl implements ConnectionPool.NoConnectionAvailableReasonDetector {

    public Reason detect(SQLException sqlException) {
      
      
      int error = sqlException.getErrorCode();
      switch(error) {
      case 1017:
        return Reason.UserOrPasswordInvalid;
      case 28000:
        return Reason.UserOrPasswordInvalid;
      }
      
      String message = sqlException.getMessage();
      if( message == null ) {
        return Reason.Other;
      }
      if( message.contains ("The Network Adapter could not establish the connection")) {
        return Reason.NetworkUnreachable;
      }
      if( message.contains ("Oracle-URL")) {
        return Reason.URLInvalid;
      }
      if( message.contains ("Listener refused the connection")) {
        return Reason.ConnectionRefused;
      }
      
      return Reason.Other;
    }
    
  }*/


  private Map<String, Class<? extends Storable>> tables = new HashMap<String, Class<? extends Storable>>();

  private static class OracleSQLColumnInfo extends DatabaseColumnInfo {

    private static final String selectColumnNameAndDatatypeSQL =
                    "select "
                    + "utc.column_name as column_name , utc.data_type as data_type, " +
                        "utc.data_length as data_length, ui.index_name as index_name, ui.uniqueness as uniqueness "
                    + "from "
                    + "USER_TAB_COLS utc "
                    + "left outer join USER_IND_COLUMNS uic ON utc.table_name = uic.table_name and utc.column_name = uic.column_name "
                    + "left outer join USER_INDEXES ui ON  ui.index_name = uic.index_name "
                    + "where "
                    + "utc.table_name=?";

    private OracleSqlType type; //uppercase
    private IndexType indexType;
    private String indexName;
    private OracleSQLColumnInfo next; //verkettete Liste, wenn mehrere Einträge zu einer Tabellenspalte existieren
    
    @Override
    public String getTypeAsString() {
      return type.toString();
    }


    @Override
    public boolean isTypeDependentOnSizeSpecification() {
      return type.isDependentOnSizeSpecification();
    }
    
    public static IndexType getIndexTypeByString(String uniqueNess) {
      if ("unique".equalsIgnoreCase(uniqueNess)) {
        return IndexType.UNIQUE;
      } else if ("nonunique".equalsIgnoreCase(uniqueNess)) {
        return IndexType.MULTIPLE;
      } else if (uniqueNess == null || uniqueNess.trim().length() == 0) {
        return IndexType.NONE;
      }
      throw new IllegalArgumentException("Unknown index type: <" + uniqueNess + ">");
    }
    
    public static ResultSetReader<OracleSQLColumnInfo> getResultSetReader(final String tableName) {
      return new ResultSetReader<OracleSQLColumnInfo>() {

        public OracleSQLColumnInfo read(ResultSet rs) throws SQLException {
          OracleSQLColumnInfo i = new OracleSQLColumnInfo();
          i.setName(rs.getString("column_name"));
          try {
            i.type = OracleSqlType.valueOf(rs.getString("data_type").toUpperCase());
          } catch (IllegalArgumentException e) {
            i.type = OracleSqlType.UNKNOWN;
            logger.warn("unknown datatype: " + rs.getString("data_type") + " in column "
                + i.getName() + " of table " + tableName + ".");
          }
          String indexType = rs.getString("uniqueness");
          try {
            i.indexType = OracleSQLColumnInfo.getIndexTypeByString(indexType);
          } catch (IllegalArgumentException e) {
            i.indexType = IndexType.NONE;
            logger.warn("unknown index type: <" + indexType + "> in column "
                        + i.getName() + " of table " + tableName + ".");
          }
          i.indexName = rs.getString("index_name");
          i.setCharLength(rs.getLong("data_length"));
          return i;
        }

      };
    }
    
  }

  private Map<Column, OracleSQLColumnInfo> columnMap = Collections.synchronizedMap(new IdentityHashMap<Column, OracleSQLColumnInfo>());


  // unterstützt nicht mehrere threads die die gleiche connection benutzen
  private class OraclePersistenceLayerConnection
      implements
        PersistenceLayerConnection,
        DatabasePersistenceLayerConnectionWithAlterTableSupport {

    public OraclePersistenceLayerConnection() throws PersistenceLayerException {
      this(false);
    }    
    
    public OraclePersistenceLayerConnection(boolean isDedicated) throws PersistenceLayerException {
      sqlUtils = createSQLUtils(isDedicated);
      
      if (isDedicated) {
        connectionPool = dedicatedPool;
      } else {
        connectionPool = pool;
      }
      sharedConnections = new ArrayList<OraclePersistenceLayerConnection>();
      sharedConnections.add(this);
    }

    public OraclePersistenceLayerConnection(OraclePersistenceLayerConnection shareConnectionPool) {
      sqlUtils = shareConnectionPool.sqlUtils;
      connectionPool = pool;
      sharedConnections = shareConnectionPool.sharedConnections;
      sharedConnections.add(this);
    }

    private final ConnectionPool connectionPool;
    private final SQLUtilsWithRetryOperationSupport sqlUtils;
    private boolean closed = false;
    private final List<OraclePersistenceLayerConnection> sharedConnections; //vgl MysqlPL


    public <T extends Storable> void addTable(Class<T> klass, boolean forceWidening, Properties props) throws PersistenceLayerException {
      String tableName = Storable.getPersistable(klass).tableName();
      try {
        if (forceWidening) {
          boolean previousSetting = automaticColumnTypeWidening;
          automaticColumnTypeWidening = true;
          DatabasePersistenceLayerWithAlterTableSupportHelper.addTable(this, klass);
          automaticColumnTypeWidening = previousSetting;
        } else {
          DatabasePersistenceLayerWithAlterTableSupportHelper.addTable(this, klass);
        }
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        throw new XNWH_GeneralPersistenceLayerException("AddTable " + tableName + " failed.", e);
      }
      
      tables.put(tableName, klass);
    }


    public <T extends Storable> void alterColumns(Set<DatabaseIndexCollision> collisions) throws PersistenceLayerException {
      for (DatabaseIndexCollision collision : collisions) {
        String tableName = collision.getPersi().tableName();
        

        boolean isPk = collision.getColumn().name().equals(collision.getPersi().primaryKey());
        IndexType javaIndexType = isPk? IndexType.UNIQUE : collision.getColumn().index();
        
        OracleSQLColumnInfo colInfo = columnMap.get(collision.getColumn());
        
        //es gibt mindestens einen Index für diese Spalte, dieser hat einen Namen
        String indexName = createIndexName(tableName, collision.getColumn().name(), isPk );
        
        //nun über alle vorgefundenen Indexe zu dieser Spalte iterieren
        boolean foundIndex = false;
        for( OracleSQLColumnInfo ci = colInfo; ci != null; ci = ci.next ) {
          if( alterIndex(collision.getColumn(), ci, javaIndexType, indexName, tableName, collision.getIndexModification()) ) {
            foundIndex = true;
          }
        }
        if( ! foundIndex ) {
          if( javaIndexType != IndexType.NONE ) {
            if (collision.getIndexModification() != IndexModification.CREATE) {
              logger.warn("Detected index creation on column " + collision.getColumn().name() + " in table " + tableName + " does not match the desired modifcation " + collision.getIndexModification());
            } else {
              createIndex( indexName, javaIndexType, tableName, collision.getColumn().name() );
            }
          }
        }
      }
    }


    public <T extends Storable> Set<DatabaseIndexCollision> checkColumns(Persistable persistable, Class<T> klass, Column[] cols)
                    throws PersistenceLayerException {
      String tableName = persistable.tableName();
      String sql = OracleSQLColumnInfo.selectColumnNameAndDatatypeSQL;
      com.gip.xyna.utils.db.Parameter paras = new com.gip.xyna.utils.db.Parameter(tableName.toUpperCase());
      if (schema != null) {
        sql += " AND utc.owner = ?";
        sql = sql.replaceAll("USER_", "ALL_");
        paras.addParameter(schema.toUpperCase());
      }
      List<OracleSQLColumnInfo> colInfos =
          sqlUtils.query(sql, paras,
            OracleSQLColumnInfo.getResultSetReader(tableName) );
      StringBuilder addColumnString = new StringBuilder();
      
      for (Column col : cols) {
        boolean foundCol = false;
        for (OracleSQLColumnInfo colInfo : colInfos) {
          if (col.name().equalsIgnoreCase(colInfo.getName())) {
            DatabasePersistenceLayerWithAlterTableSupportHelper.checkColumn(this, colInfo, col, klass,
                                                                            tableName,
                                                                            automaticColumnTypeWidening);
            foundCol = true;
          }
        }
        if (!foundCol) {
          //spalte erstellen
          if (addColumnString.length() > 0) {
            addColumnString.append(",");
          }
          addColumnString.append("").append(escape(col.name())).append(" ").append(getDefaultColumnTypeString(col, klass));
          if (col.index() == IndexType.NONE || col.index() == IndexType.MULTIPLE) {
            addColumnString.append(" NULL");
          } else {
            addColumnString.append(" NOT NULL");
          }
        }
      }
      
      // Updating columnMap as some modifications might have occured.
      colInfos = sqlUtils.query(sql, paras,
          OracleSQLColumnInfo.getResultSetReader(tableName) );
      
      for (Column col : cols) {
        for (OracleSQLColumnInfo colInfo : colInfos) {
          if (col.name().equalsIgnoreCase(colInfo.getName())) {
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
      
      if (addColumnString.length() > 0) {
        sqlUtils.executeDDL("ALTER TABLE " + escape(tableName) + " \n ADD (" + addColumnString.toString() + ")", null);
      }

      Set<DatabaseIndexCollision> collisions = new HashSet<DatabaseIndexCollision>();
      for (Column column : cols) {
        boolean isPk = column.name().equals(persistable.primaryKey());
        IndexType javaIndexType = isPk? IndexType.UNIQUE : column.index();
        
        OracleSQLColumnInfo colInfo = columnMap.get(column);
        if( colInfo == null ) {
          //keine Daten für den Index bislang, deswegen evtl. neu bauen
          if( javaIndexType != IndexType.NONE ) {
            String indexName = createIndexName(tableName, column.name(), isPk );
            createIndex( indexName, javaIndexType, tableName, column.name() );
          }
          continue;
        }
        
        if( javaIndexType == IndexType.NONE && colInfo.indexType == IndexType.NONE ) {
          continue; //kein Index
        }
        //es gibt mindestens einen Index für diese Spalte, dieser hat einen Namen
        String indexName = createIndexName(tableName, column.name(), isPk );
        
        //nun über alle vorgefundenen Indexe zu dieser Spalte iterieren
        Pair<Boolean, IndexModification> modification = Pair.of(false, null);
        boolean foundIndex = false;
        for( OracleSQLColumnInfo ci = colInfo; ci != null; ci = ci.next ) {
          modification = checkIndex(column, ci, javaIndexType, indexName, tableName); 
          if( modification.getFirst() ) {
            foundIndex = true;
          }
        }
        if( ! foundIndex ) {
          if( javaIndexType != IndexType.NONE ) {
            //createIndex( indexName, javaIndexType, tableName, column.name() );
            modification = Pair.of(false, IndexModification.CREATE);
          }
        }
        if (modification.getSecond() != null) {
          collisions.add(new DatabaseIndexCollision(persistable, column, klass, modification.getSecond()));
        }
      }
      return collisions;
    }
    
    
    
    private Pair<Boolean, IndexModification> checkIndex(Column column, OracleSQLColumnInfo colInfo, IndexType javaIndexType, String indexName, String tableName ) {
      if( javaIndexType == IndexType.NONE ) {
        //deleteIndex(colInfo.indexName, javaIndexType, tableName, column.name() );
        return Pair.of(Boolean.FALSE, IndexModification.DELETE);
      }
      if( indexName.equalsIgnoreCase(colInfo.indexName) ) {
        if( javaIndexType == colInfo.indexType ) {
          //Index passt
          return Pair.of(Boolean.TRUE, null);
        } else {
          //logger.info( "Index-Modification "+colInfo.indexName+" ("+ colInfo.indexType+") -> "+ indexName+" ("+javaIndexType+") for "+tableName+ "."+column.name()+" is unsupported!" );
          return Pair.of(Boolean.TRUE, IndexModification.MODIFY);
        }
      } else {
        if( javaIndexType == colInfo.indexType ) {
          logger.info( "Index with unexpected name "+colInfo.indexName+" (expected: "+indexName+") for "+tableName+ "."+column.name() );
          return Pair.of(Boolean.TRUE, IndexModification.CREATE);
        } else {
          logger.info( "Index "+colInfo.indexName+" ("+ colInfo.indexType+") for "+tableName+ "."+column.name()+ " was unexpected!" );
          return Pair.of(Boolean.FALSE,  null);
        }
      }
    }
    
    
    private boolean alterIndex(Column column, OracleSQLColumnInfo colInfo, IndexType javaIndexType, String indexName, String tableName, IndexModification desiredModification ) {
      if( javaIndexType == IndexType.NONE ) {
        if (desiredModification != IndexModification.DELETE) {
          logger.warn("Detected index deletion on column " + column.name() + " in table " + tableName + " does not match the desired modifcation " + desiredModification);
          return false;
        } else {
          deleteIndex(colInfo.indexName, javaIndexType, tableName, column.name() );
          return false;
        }
      }
      if( indexName.equalsIgnoreCase(colInfo.indexName) ) {
        if( javaIndexType == colInfo.indexType ) {
          //Index passt
        } else {
          if (desiredModification != IndexModification.MODIFY) {
            logger.warn("Detected index modification on column " + column.name() + " in table " + tableName + " does not match the desired modifcation " + desiredModification);
          } else {
            logger.info( "Index-Modification "+colInfo.indexName+" ("+ colInfo.indexType+") -> "+ indexName+" ("+javaIndexType+") for "+tableName+ "."+column.name()+" is unsupported!" );
          }
        }
        return true;
      } else {
        return false;
      }
    }

    private String createIndexName(String tableName, String columnName, boolean pk) {
      // Oracle Database prior to version 12.2 limit identifier names, such as table names, column names, and primary key names, to 30 characters. 
      // Oracle Database 12.2 and higher have a default limit of 128 characters.
      // Workaround for "ORA-00972: identifier is too long" with "CONSTRAINT fqctrltaskinformation_taskid_pk PRIMARY KEY(taskid)"
      // Identifiers have to be <= 128 signs 
      final int MAX_IDENTIFIER_LENGTH = 128;
      String indexName = tableName.replace('.', '_')+"_"+columnName+(pk? "_pk" : "_idx");
      if( indexName.length() > MAX_IDENTIFIER_LENGTH ) {
        int endOfTableName = tableName.length();
        int beginOfCut = tableName.length() - (indexName.length() - MAX_IDENTIFIER_LENGTH);
        indexName = indexName.substring(0, beginOfCut) +indexName.substring(endOfTableName);
        //TODO  evtl. reicht es nicht aus, nur den Tabellennamen zu kürzen
      }
      return indexName;
    }

    private void createIndex(String indexName, IndexType indexType, String tableName, String columnName) {
      String createIndexStatement = null;
      String escTableName = escape(tableName);
      String escColName = escape(columnName);
      switch( indexType ) {
        case UNIQUE:
          createIndexStatement = "CREATE UNIQUE INDEX "+indexName+" ON "+escTableName+"("+escColName+")";
          break;
        case MULTIPLE:
          createIndexStatement = "CREATE INDEX "+indexName+" ON "+escTableName+"("+escColName+")";
          break;
        default:
          logger.info( "Index-Creation "+indexName+" of type "+ indexType+" for "+escTableName+ "."+escColName+" is unsupported!" );
          return;
      }
      logger.info( "Index-Creation "+indexName+" of type "+ indexType+" for "+escTableName+ "."+escColName );
      try {
        executeDDL(createIndexStatement);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        logger.warn("Could not create index '" + indexName + "' on table '" + escTableName + "' for column '" + escColName + "'", e);
      }
    }
    
    private void executeDDL(String ddl) {
      //Ausführen des Statements ddl und Warn-Log, falls dies nicht erfolgreich war
      boolean created = false;
      try {
        sqlUtils.executeDDL(ddl, null);
        created = true;
      } finally {
        if( !created ) {
          logger.warn("Statement was "+ ddl );
        }
      }
    }

    private void deleteIndex(String indexName, IndexType indexType, String tableName, String columnName) {
      logger.info( "Index-Deletion "+indexName+" ("+indexType+") for "+tableName+ "."+columnName+" is unsupported!" );
    }

    public <T extends Storable> void createTable(Persistable persistable, Class<T> klass, Column[] cols) {
      String tableName = persistable.tableName();
      StringBuilder createTableStatement =
          new StringBuilder("CREATE TABLE ").append( escape(tableName)).append(" (\n");
      for (Column col : cols) {
        String typeAsString = getDefaultColumnTypeString(col, klass);
        createTableStatement.append("  ").append( escape(col.name())).append(" ").append(typeAsString);
        //default wert
        if (col.name().equals(persistable.primaryKey())) {
          createTableStatement.append(" NOT");
        }
        createTableStatement.append(" NULL,\n");
      }
      
      String pkIndexName = createIndexName(tableName, persistable.primaryKey(),true);
      createTableStatement.append("  CONSTRAINT ").append(pkIndexName).append(" PRIMARY KEY(")
          .append(persistable.primaryKey());
      for (Column column : cols) {
        if (column.index() == IndexType.PRIMARY && !column.name().equals(persistable.primaryKey())) {
          createTableStatement.append(", ").append(escape(column.name()));
        }
      }
      createTableStatement.append(")\n");

      createTableStatement.append(")");
      executeDDL(createTableStatement.toString());
      
      for (Column column : cols) {
        if (column.index() == IndexType.MULTIPLE || column.index() == IndexType.UNIQUE) {
          if (column.name().equals(persistable.primaryKey())) {
            continue;
          }
          String indexName = createIndexName(tableName, column.name(), false );
          createIndex(indexName, column.index(), tableName, column.name() );
        }
      }

    }


    public boolean doesTableExist(Persistable persistable) {
      //TODO all_objects spart eine abfrage
      //TODO synonyme beachten
      if (schema != null) {
        Integer rowCount =
            sqlUtils.queryInt("select count(*) from all_tables where table_name=? and owner=?",
                              new com.gip.xyna.utils.db.Parameter(persistable.tableName().toUpperCase(), schema.toUpperCase()));
        if (rowCount == 0) {
          rowCount =
              sqlUtils.queryInt("select count(*) from all_views where view_name=? and owner=?",
                                new com.gip.xyna.utils.db.Parameter(persistable.tableName().toUpperCase(), schema.toUpperCase()));
        }
        if (rowCount == null) {
          return false;
        }
        return rowCount > 0;
      }
      //kein spezielles schema gesetzt
      
      Integer rowCount =
          sqlUtils.queryInt("select count(*) from user_tables where table_name=?",
                            new com.gip.xyna.utils.db.Parameter(persistable.tableName().toUpperCase()));
      if (rowCount == 0) {
        rowCount =
            sqlUtils.queryInt("select count(*) from user_views where view_name=?",
                              new com.gip.xyna.utils.db.Parameter(persistable.tableName().toUpperCase()));
      }
      if (rowCount == null) {
        return false;
      }
      return rowCount > 0;
    }


    public <T extends Storable> String getDefaultColumnTypeString(Column col, Class<T> klass) {
      OracleSqlType colType = getDefaultOracleSQLColTypeForStorableColumn(col, klass);
      String typeAsString = colType.toString();
      if (colType.isDependentOnSizeSpecification()) {
        typeAsString += "(" + getColumnSize(col) + ")";
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
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      ensureOpen();
      try {
        sqlUtils.commit();
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        throw new XNWH_GeneralPersistenceLayerException("could not commit transaction", e);
      }
    }


    public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      String select =
          "select count(*) from " +  escape(storable.getTableName()) + " where "
              +  escape(Storable.getPersistable(storable.getClass()).primaryKey()) + " = ?";
      com.gip.xyna.utils.db.Parameter paras = new com.gip.xyna.utils.db.Parameter(storable.getPrimaryKey());
      try {
        int cnt = sqlUtils.queryInt(select, paras);
        return cnt > 0;
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        throw new XNWH_GeneralPersistenceLayerException("could not execute count query.", e);
      }
    }


    public <T extends Storable> void deleteOneRow(T storable) throws PersistenceLayerException {
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      String delete =
          new StringBuilder().append("delete from ").append(escape(storable.getTableName())).append(" where ")
              .append( escape(Storable.getPersistable(storable.getClass()).primaryKey())).append(" = ?").toString();
      try {
        deleteSingleElement(storable, delete);
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        throw new XNWH_GeneralPersistenceLayerException("could not delete row " + storable.getPrimaryKey(), e);
      }
    }


    public <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException {
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      try {
        if (storableCollection != null && storableCollection.size() > 0) {
          Iterator<T> it = storableCollection.iterator();
          T a = it.next();
          String delete =
              new StringBuilder().append("delete from ").append(escape(a.getTableName())).append(" where ")
                  .append(escape(Storable.getPersistable(a.getClass()).primaryKey())).append(" = ?").toString();
          deleteSingleElement(a, delete);
          while (it.hasNext()) {
            a = it.next();
            deleteSingleElement(a, delete);
          }
        }
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        throw new XNWH_GeneralPersistenceLayerException("could not delete collection", e);
      }
    }


    private <T extends Storable> void deleteSingleElement(T storable, String deleteString) {
      com.gip.xyna.utils.db.Parameter paras = new com.gip.xyna.utils.db.Parameter(storable.getPrimaryKey());
      sqlUtils.executeDML(deleteString, paras);
    }


    public <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException {
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      String delete = "truncate table " + escape(Storable.getPersistable(klass).tableName());
      try {
        sqlUtils.executeDML(delete, null);
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        throw new XNWH_GeneralPersistenceLayerException("could not execute dml", e);
      }
    }


    public int executeDML(PreparedCommand cmdInterface, Parameter paras) throws PersistenceLayerException {
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      if (!(cmdInterface instanceof OraclePreparedCommand)) {
        throw new XNWH_IncompatiblePreparedObjectException(PreparedCommand.class.getSimpleName());
      }
      OraclePreparedCommand cmd = (OraclePreparedCommand) cmdInterface;
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
            sqlUtilsParas.addParameter(paras.get(i));
          }
        }
      }
      try {
        return sqlUtils.executeDML(cmd.getSqlString(), sqlUtilsParas);
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        throw new XNWH_GeneralPersistenceLayerException("problem executing sql command: \"" + cmd.getSqlString()
            + "\" [" + sqlUtilsParas + "]", e);
      }
    }


    public <T extends Storable> Collection<T> loadCollection(final Class<T> klass) throws PersistenceLayerException {
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      ensureOpen();
      final com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T> reader = getReader(klass);
      try {
        ArrayList<T> list = sqlUtils.queryNoRetryOperation("select * from " + escape(Storable.getPersistable(klass).tableName()), null,
                                                           new ResultSetReaderWrapper<T>(reader, zippedBlobs, klass));
        return list;
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        throw new XNWH_GeneralPersistenceLayerException("problem loading collection", e);
      }
    }


    public <T extends Storable> void persistCollection(Collection<T> storableCollection)
                    throws PersistenceLayerException {
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      Iterator<T> it = storableCollection.iterator();
      while (it.hasNext()) {
        persistObject(it.next());
      }
    }


    private <T extends Storable> com.gip.xyna.utils.db.Parameter createParasForInsertAndUpdate(Column[] columns, T storable)
                    throws PersistenceLayerException {
      com.gip.xyna.utils.db.ExtendedParameter paras = new com.gip.xyna.utils.db.ExtendedParameter();
      for (Column col : columns) {
        Object val = storable.getValueByColName(col);
        if (col.type() == ColumnType.INHERIT_FROM_JAVA) {
          if (val instanceof String) {
            String valAsString = (String) val;
            if (valAsString != null) {
              int colSize = getColumnSize(col);
              if (valAsString.length() > colSize) {
                logger.warn("Provided value for column '" + col.name() + "' in table '" + storable.getTableName()
                    + "' was too long. It will be shortened to fit into column of size " + colSize + ". value = "
                    + valAsString);
                valAsString = valAsString.substring(0, colSize);
              }
              if (getColumnTypeOfStringCol(col, valAsString) == OracleSqlType.CLOB) {
                //oracle kann nur strings bis zu einer gewissen länge, danach müssen es in der datenbank clobs sein
                ExtendedCLOBString clob = new ExtendedCLOBString(valAsString);
                paras.addParameter(clob);
                continue;
              }
            }
            paras.addParameter(valAsString);
          } else {
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
              storable.serializeByColName(col.name(),val,blob.getOutputStream());
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
          throw new XNWH_GeneralPersistenceLayerException("unsupported column type " + col.type().toString());
        }
      }
      return paras;
    }


    private <T extends Storable> String createUpdateStatement(Column[] columns, T storable)
                    throws PersistenceLayerException {
      StringBuilder setter = new StringBuilder();
      if (columns.length == 0) {
        throw new XNWH_GeneralPersistenceLayerException("no columns found to persist");
      }
      setter.append(" set ");
      for (int i = 0; i < columns.length - 1; i++) {
        setter.append(escape(columns[i].name())).append(" = ?, ");
      }
      setter.append(escape(columns[columns.length - 1].name())).append(" = ? ");
      String tableName = storable.getTableName();
      StringBuilder stmt = new StringBuilder();
      stmt.append("update ").append(escape(tableName)).append(setter).append(" where ")
          .append(escape(Storable.getPersistable(storable.getClass()).primaryKey())).append(" = ?");
      return stmt.toString();
    }


    public <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException {
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      ensureOpen();
      //überprüfen, ob objekt bereits in db ist
      String sqlString =
          new StringBuilder().append("select count(*) from ").append(escape(storable.getTableName())).append(" where ")
              .append(escape(Storable.getPersistable(storable.getClass()).primaryKey())).append(" = ?").toString();
      boolean existedBefore = false;
      try {

        // determine whether the object exists. use a trace logger for this since the following statement (update or
        // insert will contain at least the same information)
        int cnt;
        sqlUtils.setLogger(sqlUtilsLoggerTrace);
        try {
          cnt = sqlUtils.queryInt(sqlString, new com.gip.xyna.utils.db.Parameter(storable.getPrimaryKey()));
        } finally {
          sqlUtils.setLogger(sqlUtilsLoggerDebug);
        }

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

            String insert = createInsertStatement(columns, storable.getTableName());
            com.gip.xyna.utils.db.Parameter paras = createParasForInsertAndUpdate(columns, storable);

            try {
              sqlUtils.executeDML(insert, paras);
            } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
              if (e.getCause() instanceof SQLException) {
                SQLException sqlEx = (SQLException)e.getCause();
                if (sqlEx.getErrorCode() == 1) {
                  //  ORA-00001: unique constraint (...) violated
                  cnt = sqlUtils.queryInt(sqlString, new com.gip.xyna.utils.db.Parameter(storable.getPrimaryKey()));
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
            paras.addParameter(storable.getPrimaryKey());

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


    public PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException {
      return new OraclePreparedCommand(cmd);
    }


    public <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException {
      return new OraclePreparedQuery<E>(query);
    }

    public <E> List<E> query(PreparedQuery<E> queryInterface, Parameter parameter, int maxRows)
    throws PersistenceLayerException {
      OraclePreparedQuery<E> query = (OraclePreparedQuery<E>) queryInterface;
      return this.query(queryInterface, parameter, maxRows, query.getReader());
    }
    
    
    public <E> List<E> query(PreparedQuery<E> queryInterface, Parameter parameter, int maxRows,  final com.gip.xyna.xnwh.persistence.ResultSetReader<? extends E> reader)
        throws PersistenceLayerException {
      if (!(queryInterface instanceof OraclePreparedQuery)) {
        throw new XNWH_IncompatiblePreparedObjectException(PreparedQuery.class.getSimpleName());
      }
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      OraclePreparedQuery<E> query = (OraclePreparedQuery<E>) queryInterface;
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
            paras.addParameter((String) SelectionParser.escapeParams((String)parameter.get(i), isLike[i], new EscapeForOracle()));
          } else if (parameter.get(i) instanceof Boolean) {
            paras.addParameter(new BooleanWrapper((Boolean) parameter.get(i)));
          } else {
            paras.addParameter(parameter.get(i));
          }
        }
      }
      String sqlQuery = query.getQuery().getSqlString();
      if (maxRows > -1 && maxRows < Integer.MAX_VALUE) {
        //beschränkung des ergebnisses mit oracle hilfsmitteln
        sqlQuery = query.getTransformedQueryToUseWithMaxRows(getStorable(query.getQuery().getTable()));
        if (paras == null) {
          paras = new com.gip.xyna.utils.db.Parameter();
        }
        paras.addParameter(maxRows);
      }
      //für Oracle muss bei Like-Anfragen (mit escapten Zeichen) das verwendete Escape-Zeichen
      //angegeben werden
      sqlQuery = sqlQuery.replaceAll("\\s+(?i)LIKE\\s+\\?\\s+", " LIKE ? ESCAPE '\\\\' ");
      try {
        ArrayList<E> list = sqlUtils.queryNoRetryOperation(sqlQuery, paras, new ResultSetReaderWrapper<E>(reader, zippedBlobs, getStorable(query.getQuery().getTable())));
        return list;
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        throw new XNWH_GeneralPersistenceLayerException("query \"" + sqlQuery + "\" [" + paras
            + "] could not be executed.", e);
      }
    }


    private Class<? extends Storable> getStorable(String table) {
      return tables.get(table);
    }


    public <T extends Storable> void queryOneRowForUpdate(final T storable) throws PersistenceLayerException,
                    XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      queryOneRowInternally(storable, true);
    }


    public <T extends Storable> void queryOneRow(final T storable) throws PersistenceLayerException,
                    XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      queryOneRowInternally(storable, false);
    }
    
    public <T extends Storable> void queryOneRowInternally(final T storable, boolean forUpdate)
                    throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      ensureOpen();
      StringBuilder selectString = new StringBuilder().append("select * from ").append(escape(storable.getTableName()))
                      .append(" where ").append(escape(Storable.getPersistable(storable.getClass()).primaryKey()))
                      .append(" = ?");
      if (forUpdate) {
        selectString.append(" for update");
      }
      try {
        T result = sqlUtils.queryOneRow(selectString.toString(),
                                        new com.gip.xyna.utils.db.Parameter(storable.getPrimaryKey()),
                                        new ResultSetReaderWrapper<T>(storable.getReader(), zippedBlobs, storable.getClass()));
        if (result == null) {
          throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(String.valueOf(storable.getPrimaryKey()),
                                                          storable.getTableName());
        }
        storable.setAllFieldsFromData(result);
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
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
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      ensureOpen();
      try {
        sqlUtils.rollback();
      } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
        throw new XNWH_RetryTransactionException(e);
      } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
        throw new XNWH_GeneralPersistenceLayerException("could not rollback transaction", e);
      }
    }


    public <T extends Storable> boolean areBaseTypesCompatible(Column col, Class<T> klass, DatabaseColumnInfo colInfo) {
      OracleSqlType recommendedType = getDefaultOracleSQLColTypeForStorableColumn(col, klass);
      return ((OracleSQLColumnInfo) colInfo).type.getBaseType().isCompatibleTo(recommendedType.getBaseType());
    }


    public <T extends Storable> boolean areColumnsCompatible(Column col, Class<T> klass, DatabaseColumnInfo colInfo) {
      OracleSqlType recommendedType = getDefaultOracleSQLColTypeForStorableColumn(col, klass);
      List<OracleSqlType> compatibleTypes = recommendedType.getCompatibleTypes();
      return compatibleTypes.contains(((OracleSQLColumnInfo) colInfo).type);
    }


    public <T extends Storable> String getCompatibleColumnTypesAsString(Column col, Class<T> klass) {
      OracleSqlType recommendedType = getDefaultOracleSQLColTypeForStorableColumn(col, klass);
      List<OracleSqlType> compatibleTypes = recommendedType.getCompatibleTypes();
      StringBuilder compatibleTypesStringBuilder = new StringBuilder();
      for (int i = 0; i < compatibleTypes.size(); i++) {
        if (i > 0) {
          compatibleTypesStringBuilder.append(", ");
        }
        OracleSqlType next = compatibleTypes.get(i);
        compatibleTypesStringBuilder.append(next.toString());
        if (next.isDependentOnSizeSpecification()) {
          compatibleTypesStringBuilder.append("(?)");
        }
      }
      return compatibleTypesStringBuilder.toString();
    }


    public <T extends Storable> String getTypeAsString(Column col, Class<T> klass) {
      return getDefaultOracleSQLColTypeForStorableColumn(col, klass).toString();
    }


    public <T extends Storable> boolean isTypeDependentOnSizeSpecification(Column col, Class<T> klass) {
      return getDefaultOracleSQLColTypeForStorableColumn(col, klass).isDependentOnSizeSpecification();
    }


    public <T extends Storable> void modifyColumnsCompatible(Column col, Class<T> klass, String tableName) {
      OracleSqlType recommendedType = getDefaultOracleSQLColTypeForStorableColumn(col, klass);
      String sql =
          new StringBuilder("ALTER TABLE ").append(escape(tableName)).append(" MODIFY ").append(escape(col.name())).append(" ")
              .append(recommendedType).append("(").append(getColumnSize(col)).append(")").toString();
      sqlUtils.executeDDL(sql, null);
    }


    public <T extends Storable> void widenColumnsCompatible(Column col, Class<T> klass, String tableName) {
      OracleSqlType recommendedType = getDefaultOracleSQLColTypeForStorableColumn(col, klass);
      StringBuilder sql =
          new StringBuilder("ALTER TABLE ").append(escape(tableName)).append(" MODIFY ").append(escape(col.name())).append(" ").append(recommendedType);
      if (recommendedType.isDependentOnSizeSpecification()) {
        sql.append("(").append(getColumnSize(col)).append(")");
      }
      sqlUtils.executeDDL(sql.toString(), null);
    }


    public void setTransactionProperty(TransactionProperty property) {
      //nicht unterstützt
    }

    
    public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> arg0) throws PersistenceLayerException {
      OraclePersistenceLayer.this.throwIfDBNotReachable();
      try {
        connectionPool.ensureConnectivity(sqlUtils.getConnection());
      } catch (SQLException e) {
        throw new XNWH_RetryTransactionException(e);
      }
    }

    
    public boolean isOpen() {
      try {
        return !closed && !sqlUtils.getConnection().isClosed();
      } catch (SQLException e) {
        return false;
      }
    }

    public <T extends Storable> void removeTable(Class<T> arg0, Properties arg1) throws PersistenceLayerException {
      for (Column col : Storable.getColumns(arg0)) {
        columnMap.remove(col);
      }
      tables.remove(Storable.getPersistable(arg0).tableName());
    }


    public long getPersistenceLayerInstanceId() {
      return OraclePersistenceLayer.this.pliID;
    }

  }


  public String getInformation() {
    //user pw connectstring), poolsize, pool timeout(ms) + weitere connection parameter
    return "Oracle (" + username + "@" + url + ", pooltimeout=" + timeout + ")";
  }


  public boolean describesSamePhysicalTables(PersistenceLayer plc) {
    if (plc instanceof OraclePersistenceLayer) {
      OraclePersistenceLayer mplc = (OraclePersistenceLayer) plc;
      if (url.equals(mplc.url)) {
        return true;
      }
    }
    return false;
  }


  public void shutdown() throws PersistenceLayerException {
    try {
      ConnectionPool.removePool(pool, true, 0);
      ConnectionPool.removePool(dedicatedPool, true, 0);
    } catch (ConnectionCouldNotBeClosedException e) {
      throw new XNWH_GeneralPersistenceLayerException("Shutdown of " + OraclePersistenceLayer.class.getName()
          + " not successful. Some connections could not be closed.", e);
    }
  }


  private static Map<Class<?>, OracleSqlType> javaTypeToMySQLType = new HashMap<Class<?>, OracleSqlType>();

  static {
    javaTypeToMySQLType.put(Boolean.class, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Boolean.TYPE, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Byte.class, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Byte.TYPE, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Short.class, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Short.TYPE, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Integer.class, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Integer.TYPE, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Long.class, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Long.TYPE, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Float.class, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Float.TYPE, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Double.class, OracleSqlType.NUMBER);
    javaTypeToMySQLType.put(Double.TYPE, OracleSqlType.NUMBER);

    javaTypeToMySQLType.put(String.class, OracleSqlType.VARCHAR2);
    javaTypeToMySQLType.put(StringSerializable.class, OracleSqlType.VARCHAR2);
  }

  private static <T extends Storable> OracleSqlType getDefaultOracleSQLColTypeForStorableColumn(Column col, Class<T> klass) {
    ColumnType colType = col.type();
    if (colType == ColumnType.BLOBBED_JAVAOBJECT) {
      return OracleSqlType.BLOB;
    } else if (colType == ColumnType.BYTEARRAY) {
      return OracleSqlType.BLOB;
    } else if (colType == ColumnType.INHERIT_FROM_JAVA) {
      Field f = Storable.getColumn(col, klass);
      Class<?> fieldType = f.getType();
      OracleSqlType type = javaTypeToMySQLType.get(fieldType);
      if (type == null) {
        //Iteration über die Einträge in javaTypeToMySQLType: evtl. ist Storable-Column von einem bekannten Typ abgeleitet
        for( Class<?> clazz : javaTypeToMySQLType.keySet() ) {
          if( clazz.isAssignableFrom(fieldType) ) {
            type = javaTypeToMySQLType.get(clazz);
            break;
          }
        }
      }
      if( type == null ) {
        return OracleSqlType.BLOB;
      } else if (type == OracleSqlType.VARCHAR2) {
        if (col.size() > type.getSize()) {
          return OracleSqlType.CLOB;
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


  public Reader getExtendedInformation(String[] arg0) {
    return null;
  }


  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {
    this.clusterProviderId = clusterInstanceId;
    ODSImpl.getInstance().changeClustering(this, true, clusterInstanceId);
    
    XynaClusteringServicesManagementInterface clusterMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    this.clusterInstance = clusterMgmt.getClusterInstance(clusterInstanceId);
    
    this.clusterStateChangeHandler.setClusterInstanceId(clusterInstanceId);
    
    clusterMgmt.addClusterStateChangeHandler(clusterInstanceId, this.clusterStateChangeHandler);
  }


  public long getClusterInstanceId() {
    return clusterProviderId;
  }

 
  public String getName() {
    return getClass().getSimpleName() + "_" + nameSuffix;
  }


  public boolean isClustered() {
    return clusterProviderId > -1;
  }


  public void disableClustering() {
    ODSImpl.getInstance().changeClustering(this, false, clusterProviderId);
  }

  
  public ClusterProvider getClusterInstance() {
    return clusterInstance;
  }
  
  private String name;


  public String toString() {
    if (name == null) {
      String s = super.toString();
      name =
          s.substring(OraclePersistenceLayer.class.getName().length()
              - OraclePersistenceLayer.class.getSimpleName().length());
    }
    return name;
  }

  
  public void throwIfDBNotReachable() throws XNWH_RetryTransactionException {
    if (isClustered() && clusterInstance != null && !clusterInstance.fastCheckIsMediumReachable()) {
      throw new XNWH_RetryTransactionException();
    }
  }
  
  
  
  private SQLErrorHandlingLogger buildSQLLogger(Level level) {
    SQLErrorHandlingLoggerBuilder builder = buildSQLLogger();
    return builder.build(null, level, ODSImpl.CONNECTIONCLASSNAME);
  }
  
  
  private SQLErrorHandlingLogger buildSQLLogger(Logger logger, Level level) {
    SQLErrorHandlingLoggerBuilder builder = buildSQLLogger();
    return builder.build(logger, level, ODSImpl.CONNECTIONCLASSNAME);
  }
  
  
  private SQLErrorHandlingLoggerBuilder buildSQLLogger() {
    SQLErrorHandlingLoggerBuilder builder = SQLErrorHandlingLogger.builder();
    builder.handleCode(
                0, // not an ORA error, e.g. when thrown within Xyna
                1, // ORA-00001: unique constraint (...) violated
              904, // ORA-00904: (...): invalid identifier
              906, // ORA-00906: missing left parenthesis
              955, // ORA-00955: name is already used by an existing object
             1400, // ORA-01400: cannot insert NULL into ....  You tried to insert a NULL value into a column that does not accept NULL values.
             1430, // ORA-01430: column being added already exists in table
             1722, // ORA-01722: invalid number
             1735, // ORA-01735: invalid ALTER TABLE option
            25228  // ORA-25228: timeout or end-of-fetch during message dequeue from <schema>.<queueName>
          ).with(SQLErrorHandling.ERROR)
           .handleCode(
             1013  // ORA-01013: user requested cancel of current operation
          ).with(SQLErrorHandling.RETRY_OPERATION)
           .arbitraryCheck(new ErrorCodeHandlingElement() {
              public void check(SQLException e) throws com.gip.xyna.xnwh.exception.SQLRuntimeException {
                ClusterProvider clusterInstance = getClusterInstance();
                if (clusterInstance != null) {
                  clusterInstance.checkInterconnect();
                }
              }
          })
          // 3135  ORA-03135: connection lost contact
          // 25402 ORA-25402: transaction must roll back
          // 25408 ORA-25408: can not safely replay call
          .otherwise(SQLErrorHandling.RETRY_TRANSACTION);
    return builder;
  }
  
  
  //SQLRuntimeException(2) are only kept around in case they are contained in a serialized StackTrace
  private static class SQLRuntimeException extends RuntimeException {

    public SQLRuntimeException(Exception e) {
      super(e);
    }


    private static final long serialVersionUID = 4495095171180268007L;

  }
  
  private static class SQLRuntimeException2 extends RuntimeException {

    public SQLRuntimeException2(Exception e) {
      super(e);
    }

    
    private static final long serialVersionUID = -1896005211780911371L;

  }

  
  private static class SQLRetryOperationRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SQLRetryOperationRuntimeException(Exception e) {
      super(e);
    }

  }
  
  private static class EscapeForOracle implements EscapeParameters {

    public String escapeForLike(String toEscape) {
      if (toEscape == null) {
        return toEscape;
      }
      
      toEscape = toEscape.replaceAll("%", "\\\\%");
      toEscape = toEscape.replaceAll("_", "\\\\_");
      return toEscape;
    }

    @Override
    public String getMultiCharacterWildcard() {
      return "%";
    }

    @Override
    public String getSingleCharacterWildcard() {
      return "_";
    }
    
  }


  public PersistenceLayerConnection getConnection(PersistenceLayerConnection shareConnectionPool) throws PersistenceLayerException {
    if (shareConnectionPool instanceof OraclePersistenceLayerConnection) {
      return new OraclePersistenceLayerConnection((OraclePersistenceLayerConnection) shareConnectionPool);
    } else {
      throw new IllegalArgumentException();
    }
  }


  public boolean usesSameConnectionPool(PersistenceLayer plc) {
    if (plc instanceof OraclePersistenceLayer) {
      OraclePersistenceLayer oplc = (OraclePersistenceLayer) plc;
      return oplc.pool == pool;
    } else {
      return false;
    }
  }

  @Override
  public QueryGenerator getQueryGenerator() {
    return new QueryGenerator(OraclePersistenceLayer::escape);
  }


  public static String escape(String toEscape) {
    if(XynaProperty.QUERY_ESCAPE.get()) {
      return String.format("\"%s\"", toEscape.toUpperCase());
    }
    return toEscape;
  }
}
