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

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionCouldNotBeClosedException;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionInformation;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionPoolParameter;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException.Reason;
import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.IConnectionFactory;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.SQLUtilsLogger;



public class SQLUtilsCreator implements IConnectionFactory {

  private static Logger logger = CentralFactoryLogging.getLogger(SQLUtilsCreator.class);
  

  private ConnectionPool pool;
  private DBConnectionData dbd;
  private long shutdownTimeout;
  
  public SQLUtilsCreator(String conPoolId, QueueData queueData, int poolSize ) throws NoConnectionAvailableException  {
    this(conPoolId,queueData,poolSize,null, 10);
  }
  
  public SQLUtilsCreator(String conPoolId, QueueData queueData, int poolSize, ClassLoader classLoader, long shutdownTimeout ) throws NoConnectionAvailableException  {
    dbd = queueData.getDBConnectionData(classLoader);
    this.shutdownTimeout = 1000L * shutdownTimeout;
    ConnectionPoolParameter cpp = ConnectionPoolParameter.create(conPoolId).
        connectionFactory(this).
        size(poolSize).
        noConnectionAvailableReasonDetector( new NoConnectionAvailableReasonDetectorImpl() ).
        build();
    this.pool = ConnectionPool.getInstance(cpp);
  }

  public SQLUtils getSQLUtils(String clientInfo, Logger logger, Level level) {
    SQLLogger sqlLogger = new SQLLogger(logger, level);
    
    Connection connection;
    try {
      connection = pool.getConnection(clientInfo);
    } catch (SQLException e) {
      sqlLogger.logException(e);
      return null;
    }
    
    SQLUtils sqlUtils = new SQLUtils(connection, sqlLogger);
    sqlUtils.setName(clientInfo);
    return sqlUtils;
  }
  

  public static class SQLLogger implements SQLUtilsLogger {
    private Logger logger;
    private Level level;
    
    public SQLLogger(Logger logger) {
      this(logger,Level.TRACE);
    }
    public SQLLogger(Logger logger, Level level) {
      this.logger = logger;
      this.level = level;
    }
    
    public void logException(Exception e) {
      throw new SQLRuntimeException(e);
    }
    public void logSQL(String sql) {
      if( logger.isEnabledFor(level) ) {
        logger.log(SQLUtils.class.getName(), level, "SQL= " + sql, null);
      }
    }
  }

  public static class SQLRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SQLRuntimeException(Exception e) {
      super(e);
    }

  }


  public static class NoConnectionAvailableReasonDetectorImpl
      implements
        ConnectionPool.NoConnectionAvailableReasonDetector {

    public Reason detect(SQLException sqlException) {
      int error = sqlException.getErrorCode();
      switch (error) {
        case 1017 :
          return Reason.UserOrPasswordInvalid;
        case 28000 :
          return Reason.UserOrPasswordInvalid;
      }

      String message = sqlException.getMessage();
      if (message == null) {
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

  }


  public int getActiveConnections() {
    int usedCount = 0;
    for (ConnectionInformation ci : pool.getConnectionStatistics()) {
      if (ci.isInUse()) {
        usedCount++;
      }
    }
    return usedCount;
  }


  public Connection createNewConnection() {
    try {
      return dbd.createConnection();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public void markConnection(Connection arg0, String arg1) {
    
  }


  public String getSchema() {
    return dbd.getUser();
  }


  public String getJdbcUrl() {
    return dbd.getUrl();
  }


  public void close() {
    try {
      ConnectionPool.removePool(pool, false, shutdownTimeout );
    } catch (ConnectionCouldNotBeClosedException e) {
      try {
        ConnectionPool.removePool(pool, true, shutdownTimeout);
      } catch (ConnectionCouldNotBeClosedException e2) {
        logger.warn("Could not remove ConnectionPool "+pool.getId(), e );
      }
    }
    
  }

}
