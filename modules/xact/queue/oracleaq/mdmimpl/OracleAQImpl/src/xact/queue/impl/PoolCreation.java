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
package xact.queue.impl;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import xact.queue.admin.DBConnectionData;
import xact.queue.admin.OracleAQConfig;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.db.ConnectionFactory;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionPoolParameter;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.db.IConnectionFactory;
import com.gip.xyna.xact.trigger.oracleaq.shared.SQLUtilsCreator.NoConnectionAvailableReasonDetectorImpl;


/**
 *
 */
public class PoolCreation {

  private static Logger logger = CentralFactoryLogging.getLogger(PoolCreation.class);
  public static int SOCKET_TIMEOUT_SECONDS = 15;
  public static int CONNECTION_TIMEOUT_SECONDS = 15;
  
  
  public static Pair<ConnectionPool, String> getOrCreatePool(String uniqueQueueName, OracleAQConfig oac) {
    String conPoolName = oac.getConnectionPoolName();
    if( conPoolName == null ) {
      conPoolName = "AQ Adapter "+uniqueQueueName;
    }
    
    ConnectionPool pool = null;
    //TODO schöner!
    for (ConnectionPool p : ConnectionPool.getAllRegisteredConnectionPools()) {
      if (p.getId().equals(conPoolName)) {
        pool = p;
      }
    }
    if( pool == null ) {
      DBConnectionData connData = oac.getDBConnectionData();
      try {
        pool = ConnectionPool.getInstance( getConnectionPoolParameter(conPoolName, connData) );
      } catch (NoConnectionAvailableException e) {
        throw new RuntimeException(e);
      }
    }
    //TODO schöneres Target!  username.queuename@db
    //dafür muss ConnectionPool Daten ausgeben!
    //pool.getSchema()+"."+oac.getName_externalQueue()+"@"+pool.getDB();
    
    return Pair.of(pool, conPoolName);
  }

  private static ConnectionPoolParameter getConnectionPoolParameter(String poolName, final DBConnectionData connData) {
    
    final com.gip.xyna.utils.db.DBConnectionData dbd = com.gip.xyna.utils.db.DBConnectionData.newDBConnectionData()
        .user(connData.getUsername())
        .password(connData.getPassword())
        .url(connData.getJdbc_URL())
        .connectTimeoutInSeconds(CONNECTION_TIMEOUT_SECONDS)
        .socketTimeoutInSeconds(SOCKET_TIMEOUT_SECONDS)
        .classLoaderToLoadDriver(PoolCreation.class.getClassLoader())
        .build();
    
    ConnectionPoolParameter conPoolParams = new ConnectionPoolParameter(new IConnectionFactory() {

      public void markConnection(Connection con, String clientInfo) {
        ConnectionFactory.markConnection(con, clientInfo);
      }


      public Connection createNewConnection() {
        if (logger.isInfoEnabled()) {
          logger.info("Creating database connection for schema " + connData.getUsername() + ", jdbc url = "
              + connData.getJdbc_URL());
        }
       
        Connection con;
        try {
          con = dbd.createConnection();
        } catch (SQLException e) {
          throw new ConnectionCreationException(e);
        } catch (Exception e) {
          throw new ConnectionCreationException(e);
        }
        if (con == null) {
          throw new ConnectionCreationException("Creating SQLUtils has failed for schema " + connData.getUsername()
              + ", jdbc url = " + connData.getJdbc_URL());
        }
        return con;
      }
    }, poolName, 3, new NoConnectionAvailableReasonDetectorImpl(), 1, 1000);
    
    return conPoolParams;
  }
  
  public static class ConnectionCreationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConnectionCreationException(String message) {
      super(message);
    }

    public ConnectionCreationException(Throwable cause) {
      super(cause);
    }
    
  }


}
