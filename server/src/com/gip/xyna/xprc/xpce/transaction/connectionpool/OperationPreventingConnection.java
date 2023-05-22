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
package com.gip.xyna.xprc.xpce.transaction.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import com.gip.xyna.utils.db.WrappedConnection;
import com.gip.xyna.xprc.xpce.transaction.parameter.OperationPrevention;
import com.gip.xyna.xprc.xpce.transaction.parameter.TransactionOperation;

public class OperationPreventingConnection extends WrappedConnection {

  private final OperationPrevention operationPrevention;
  
  public OperationPreventingConnection(Connection con, OperationPrevention operationPrevention) {
    super(con);
    this.operationPrevention = operationPrevention;
  }
  
  @Override
  public void commit() throws SQLException {
    if (operationPrevention.doPrevent(TransactionOperation.COMMIT)) {
      ConnectionPoolTransactionType.logger.debug("Preventing operation '" + TransactionOperation.COMMIT.toString() +"'");
    } else {
      super.commit();
    }
  }
  
  @Override
  public void rollback() throws SQLException {
    if (operationPrevention.doPrevent(TransactionOperation.ROLLBACK)) {
      ConnectionPoolTransactionType.logger.debug("Preventing operation '" + TransactionOperation.ROLLBACK.toString() +"'");
    } else {
      super.rollback();
    }
  }
  
  @Override
  public void rollback(Savepoint savepoint) throws SQLException {
    if (operationPrevention.doPrevent(TransactionOperation.ROLLBACK)) {
      ConnectionPoolTransactionType.logger.debug("Preventing operation '" + TransactionOperation.ROLLBACK.toString() +"'");
    } else {
      super.rollback(savepoint);
    }
  }
  
  @Override
  public void close() throws SQLException {
    if (operationPrevention.doPrevent(TransactionOperation.END)) {
      ConnectionPoolTransactionType.logger.debug("Preventing operation '" + TransactionOperation.END.toString() +"'");
    } else {
      super.close();
    }
  }
  
}