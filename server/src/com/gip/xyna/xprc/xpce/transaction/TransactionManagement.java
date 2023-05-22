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
package com.gip.xyna.xprc.xpce.transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.maps.ConcurrentCounterMap;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_FailedToOpenTransaction;
import com.gip.xyna.xprc.exceptions.XPRC_TransactionOperationFailed;
import com.gip.xyna.xprc.exceptions.XPRC_UnknownTransaction;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage.ChildOrderStorageStack;
import com.gip.xyna.xprc.xpce.transaction.connectionpool.ConnectionPoolTransactionType;
import com.gip.xyna.xprc.xpce.transaction.odsconnection.ODSConnectionTransactionType;
import com.gip.xyna.xprc.xpce.transaction.parameter.TransactionParameter;

public class TransactionManagement extends FunctionGroup {

  private final static String DEFAULT_NAME = "TransactionManagement";
  
  public final static String TRANSACTION_GENERIC_CONTEXT_IDENTIFIER = "transaction";
  
  private Map<String, TransactionType<?>> registeredTypes;
  private final Map<Long, ManagedTransaction> managedTransactions = new HashMap<>();
  private IDGenerator generator;
  
  public TransactionManagement() throws XynaException {
    
  }
  
  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    registeredTypes = new HashMap<>();
    generator = IDGenerator.getInstance();
    registeredTypes.put(ConnectionPoolTransactionType.TRANSACTION_TYPE_NAME, new ConnectionPoolTransactionType());
    registeredTypes.put(ODSConnectionTransactionType.TRANSACTION_TYPE_NAME, new ODSConnectionTransactionType());
  }


  @Override
  protected void shutdown() throws XynaException {
    
  }
  
  private final ConcurrentCounterMap<Long> transactionCountPerOrder = new ConcurrentCounterMap<>();
  private final ConcurrentHashMap<Long, Long> transactionIdToRootOrderId = new ConcurrentHashMap<>();
  
  public Long openTransaction(TransactionParameter tp) throws XPRC_FailedToOpenTransaction {
    TransactionType<?> type =  registeredTypes.get(tp.getTransactionType());
    TypedTransaction transaction = type.openTransaction(tp.getSafeguardParameter().getOperationPrevention(), tp.getTransactionTypeSpecifics());
    Long id = generator.getUniqueId("TransactionMgmt");
    DisposalStrategy guard = DisposalStrategy.create(id, tp);
    TransactionWrapper<?> managedTransaction = new TransactionWrapper<>(transaction, guard);
    managedTransactions.put(id, managedTransaction);
    if (logger.isDebugEnabled()) {
      logger.debug("Created transaction " + id + " of type " + tp.getTransactionType() + " with disposalstrategy " + guard.getClass().getSimpleName());
    }
    //anzahl offener transaktionen merken, damit persistenceservices schnell wissen, ob sie transaktion suchen m�ssen.
    ChildOrderStorageStack childOrderStorageStack = ChildOrderStorage.childOrderStorageStack.get();
    if (childOrderStorageStack.getCorrelatedXynaOrder() != null) {
      long rootId = childOrderStorageStack.getCorrelatedXynaOrder().getRootOrder().getId();
      transactionCountPerOrder.increment(rootId);
      transactionIdToRootOrderId.put(id, rootId);
    }
    return id;
  }

  
  public void commit(Long txId) throws XPRC_UnknownTransaction, XPRC_TransactionOperationFailed {
    ManagedTransaction mTx = managedTransactions.get(txId);
    if (mTx == null) {
      throw new XPRC_UnknownTransaction(txId);
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Committing transaction " + txId);
      }
      try {
        mTx.commit();
      } catch (Exception e) {
        throw new XPRC_TransactionOperationFailed("commit", e);
      }
    }
  }
  
  public void rollback(Long txId) throws XPRC_UnknownTransaction, XPRC_TransactionOperationFailed {
    ManagedTransaction mTx = managedTransactions.get(txId);
    if (mTx == null) {
      throw new XPRC_UnknownTransaction(txId);
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Rollbacking transaction " + txId);
      }
      try {
        mTx.rollback();
      } catch (Exception e) {
        throw new XPRC_TransactionOperationFailed("rollback", e);
      }
    }
  }

  public void end(Long txId) throws XPRC_TransactionOperationFailed {
    ManagedTransaction mTx = managedTransactions.get(txId);
    if (mTx == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Transaction " + txId + " already closed");
      }
      return;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Closing transaction " + txId);
    }
    try {
      mTx.end();
    } catch (Exception e) {
      throw new XPRC_TransactionOperationFailed("end", e);
    } finally {
      managedTransactions.remove(txId);
      Long rootId = transactionIdToRootOrderId.remove(txId);
      if (rootId != null) {
        transactionCountPerOrder.decrement(rootId);
      }
    }
  }
  
  public int numberOfOpenTransactions(long rootId) {
    return transactionCountPerOrder.get(rootId);
  }
  
  public boolean isOpen(Long txId) {
    return managedTransactions.get(txId) != null;
  }


  /**
   * Verwendung so:
   * <pre>
   * TransactionAccess access = txMgmt.access(txId);
   * ConnectionPoolTransaction transaction = access.getTypedTransaction(ConnectionPoolTransaction.class);
   * Connection unspecifiedCon = transaction.getConnection();
   * </pre> 
   */
  public TransactionAccess access(Long txId) throws XPRC_UnknownTransaction {
    ManagedTransaction mTx = managedTransactions.get(txId);
    if (mTx == null) {
      throw new XPRC_UnknownTransaction(txId);
    } else {
      return mTx;
    }
  }

  
}
