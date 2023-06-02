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
package com.gip.xyna.xprc.xpce.transaction;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.xpce.transaction.parameter.TransactionOperation;

class TransactionWrapper<T extends TypedTransaction> implements ManagedTransaction {

  private final T wrappedTransaction;
  private final DisposalStrategy guard; 
  
  TransactionWrapper(T wrappedTransaction, DisposalStrategy guard) {
    this.wrappedTransaction = wrappedTransaction;
    this.guard = guard;
  }
  
  public void commit() throws Exception {
    try {
      wrappedTransaction.commit();
    } finally {
      guard.notifyOfOperation(TransactionOperation.COMMIT);
    }
  }

  public void rollback() throws Exception {
    try {
      wrappedTransaction.rollback();
    } finally {
      guard.notifyOfOperation(TransactionOperation.ROLLBACK);
    }
  }

  public void end() throws Exception {
    try {
      wrappedTransaction.end();
    } finally {
      guard.notifyOfOperation(TransactionOperation.END);
    }
  }
  

  @Override
  public <O extends TypedTransaction> O getTypedTransaction(Class<O> clazz) {
    try {
      if (clazz.isInstance(wrappedTransaction)) {
        return clazz.cast(wrappedTransaction);
      } else {
        return null;
      }
    } finally {
      guard.notifyOfOperation(TransactionOperation.ACCESS);
    }
  }

  @Override
  public <O extends TypedTransaction> List<O> getTypedTransactions(Class<O> clazz) {
    List<O> list = new ArrayList<>();
    try {
      if (clazz.isInstance(wrappedTransaction)) {
        list.add(clazz.cast(wrappedTransaction));
      }
      return list;
    } finally {
      guard.notifyOfOperation(TransactionOperation.ACCESS);
    }
  }
  
}