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
package com.gip.xyna.xsor;



import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;



public class TransactionContext {

  private static final Logger logger = Logger.getLogger(TransactionContext.class.getName());

  private final static AtomicInteger idGenerator = new AtomicInteger(0);

  private final int transactionId;
  private final XSORMemoryRepository xsorMemoryRepository;
  private final Map<String, Set<Integer>> lockedInternalIds;
  private final Map<String, Set<Integer>> grabbedIds;
  private final Map<String, Map<Integer, Integer>> holdCount;


  private TransactionContext(int transactionId, XSORMemoryRepository xsorMemoryRepository) {
    this.transactionId = transactionId;
    this.xsorMemoryRepository = xsorMemoryRepository;
    lockedInternalIds = new HashMap<String, Set<Integer>>();
    holdCount = new HashMap<String, Map<Integer, Integer>>();
    grabbedIds = new HashMap<>();
  }


  public int getTransactionId() {
    return transactionId;
  }


  boolean isLockedByTransaction(int internalId, String tableName) {
    Table correspondingTable = xsorMemoryRepository.getByTableName(tableName);
    return correspondingTable.getLockTransactionId(internalId) == transactionId;
  }


  void releaseTransactionLocks() {
    for (Entry<String, Set<Integer>> entry : lockedInternalIds.entrySet()) {
      String tableName = entry.getKey();
      Table correspondingTable = xsorMemoryRepository.getByTableName(tableName);
      for (int lockedInternalId : entry.getValue()) {
        correspondingTable.unlockObjectForTransaction(lockedInternalId, transactionId);
      }
    }
    lockedInternalIds.clear();
    holdCount.clear();
  }


  Map<String, Set<Integer>> getInternalIdsForAllGrabbedObjects() {
    return grabbedIds;
  }

  public void grabbed(int internalId, String tableName) {
    Set<Integer> grabbed = grabbedIds.get(tableName);
    if (grabbed == null) {
      grabbed = new HashSet<>();
      grabbedIds.put(tableName, grabbed);
    }
    grabbed.add(internalId);
  }
  
  public void released(int internalId, String tableName) {
    Set<Integer> grabbed = grabbedIds.get(tableName);
    if (grabbed != null) {
      grabbed.remove(internalId);
    }
  }

  void lockForTransaction(int internalId, String tableName) {
    Set<Integer> locked = lockedInternalIds.get(tableName);
    if (locked == null) {
      locked = new HashSet<Integer>();
      lockedInternalIds.put(tableName, locked);
    }
    if (!locked.add(internalId)) {
      increaseHoldCount(internalId, tableName);
    } else {
      boolean success = false;
      try {
        Table correspondingTable = xsorMemoryRepository.getByTableName(tableName);
        correspondingTable.lockObjectForTransaction(internalId, transactionId);
        success = true;
      } finally {
        if (!success) {
          lockedInternalIds.remove(internalId);
        }
      }
    }
  }
  
  
  boolean tryLockForTransaction(int internalId, String tableName) {
    Table correspondingTable = xsorMemoryRepository.getByTableName(tableName);
    Set<Integer> locked = lockedInternalIds.get(tableName);
    if (locked == null) {
      locked = new HashSet<Integer>();
      lockedInternalIds.put(tableName, locked);
    }
    if (locked.contains(internalId)) {
      increaseHoldCount(internalId, tableName);
      return true;
    } else {
      if (correspondingTable.tryLockObjectForTransaction(internalId, transactionId)) {
        locked.add(internalId);
        return true;
      } else {
        return false;
      }
    }
  }
  
  
  private synchronized void increaseHoldCount(int internalId, String tableName) {
    Map<Integer, Integer> tableHoldCount = holdCount.get(tableName);
    if (tableHoldCount == null) {
      tableHoldCount = new HashMap<Integer, Integer>();
      holdCount.put(tableName, tableHoldCount);
    }
    Integer holdCount = tableHoldCount.get(internalId);
    if (holdCount == null) {
      holdCount = 1;
    }
    tableHoldCount.put(internalId, ++holdCount);
  }
  
  private synchronized int decreaseHoldCount(int internalId, String tableName) {
    Map<Integer, Integer> tableHoldCount = holdCount.get(tableName);
    if (tableHoldCount == null) {
      return 0;
    }
    Integer holdCount = tableHoldCount.get(internalId);
    if (holdCount == null) {
      return 0;
    } else {
      return holdCount--;
    }
    
  }


  void unlockFromTransaction(int internalId, String tableName) {
    int holdCount = decreaseHoldCount(internalId, tableName);
    if (holdCount <= 0) {
      if (lockedInternalIds.get(tableName) != null) {
        lockedInternalIds.get(tableName).remove(internalId);
      }
      Table correspondingTable = xsorMemoryRepository.getByTableName(tableName);
      correspondingTable.unlockObjectForTransaction(internalId, transactionId);
    }
  }


  // factory method
  public static TransactionContext newTransactionContext(XSORMemoryRepository xsorMemoryRepository) {
    int newTransactionId = idGenerator.incrementAndGet();
    while (newTransactionId < 0) {
      if (newTransactionId == Integer.MIN_VALUE) {
        idGenerator.set(0);
        return new TransactionContext(0, xsorMemoryRepository);
      }
      Thread.yield();
      newTransactionId = idGenerator.incrementAndGet();
    }
    return new TransactionContext(newTransactionId, xsorMemoryRepository);
  }


  public void releasedAll() {
    grabbedIds.clear();
  }

}
