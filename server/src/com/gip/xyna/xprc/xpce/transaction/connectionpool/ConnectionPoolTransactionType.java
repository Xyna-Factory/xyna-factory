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
package com.gip.xyna.xprc.xpce.transaction.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;
import com.gip.xyna.xprc.exceptions.XPRC_FailedToOpenTransaction;
import com.gip.xyna.xprc.xpce.transaction.TransactionType;
import com.gip.xyna.xprc.xpce.transaction.parameter.OperationPrevention;


public class ConnectionPoolTransactionType implements TransactionType<ConnectionPoolTransaction> {
  
  final static Logger logger = CentralFactoryLogging.getLogger(ConnectionPoolTransactionType.class);

  public static final String TRANSACTION_TYPE_NAME = "ConnectionPoolTransactionType";
  private static final String DEFAULT_CLIENT_INFO = TRANSACTION_TYPE_NAME;
  private static final Long DEFAULT_TIMEOUT = 1800_000L; //30 min
  
  //namen der keys für die parameter
  public final static String KEY_CONNECTION_POOLS = "connectionPools";
  public final static String KEY_CONNECTION_TIMEOUT = "conTimeout";
  public final static String KEY_CLIENT_INFO = "clientInfo";
  
  
  
  public ConnectionPoolTransaction openTransaction(OperationPrevention operationPrevention, Map<String, String> params) throws XPRC_FailedToOpenTransaction {
    String conPools = params.get(KEY_CONNECTION_POOLS);
    List<String> conPoolList = new ArrayList<>();
    if (conPools.contains(",")) {
      for (String conPool : conPools.split(",")) {
        conPoolList.add(conPool.trim());
      }
    } else {
      conPoolList.add(conPools.trim());
    }
    Long timeout = DEFAULT_TIMEOUT;
    if (params.containsKey(KEY_CONNECTION_TIMEOUT)) {
      timeout = Long.parseLong(params.get(KEY_CONNECTION_TIMEOUT));
    }
    String clientInfo = DEFAULT_CLIENT_INFO;
    if (params.containsKey(KEY_CLIENT_INFO)) {
      clientInfo = params.get(KEY_CLIENT_INFO);
    }

    ConnectionPoolManagement cpm = XynaFactory.getInstance().getXynaNetworkWarehouse().getConnectionPoolManagement();
    ConnectionBundle bundle = new ConnectionBundle();
    for (String conPoolName : conPoolList) {
      try {
        ConnectionPool pool = cpm.getConnectionPool(conPoolName);
        if (pool == null) { // current cpm impl would throw RuntimeException
          // throw to let the outer catch handle it
          throw new RuntimeException("Failed to look up pool '" + conPoolName + "' in PoolMgmt.");
        }
        bundle.add(conPoolName, wrap(pool.getConnection(timeout, clientInfo), operationPrevention));
      } catch (Throwable e) {
        Department.handleThrowable(e);
        for (Connection alreadyOpenedCon : bundle) {
          try {
            unwrap(alreadyOpenedCon).close();
          } catch (SQLException e1) {
            logger.warn("Failed to close already opened connection on rollback of openTransaction", e1);
          }
        }
        throw new XPRC_FailedToOpenTransaction(TRANSACTION_TYPE_NAME + "/" + conPoolName, e);
      }
    }
    return new ConnectionPoolTransaction(bundle);
    
  }


  private Connection unwrap(Connection con) {
    if (con instanceof OperationPreventingConnection) {
      return ((OperationPreventingConnection) con).getWrappedConnection();
    } else {
      return con;
    }
  }


  private static Connection wrap(Connection con, OperationPrevention operationPrevention) {
    if (operationPrevention != null &&
        operationPrevention.getOperations() != null &&
        operationPrevention.getOperations().size() > 0) {
      return new OperationPreventingConnection(con, operationPrevention);
    } else {
      return con;
    }
  }
  

}
