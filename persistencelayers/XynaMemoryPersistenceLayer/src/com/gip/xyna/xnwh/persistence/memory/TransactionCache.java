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

package com.gip.xyna.xnwh.persistence.memory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.xnwh.persistence.Storable;



public class TransactionCache {

  private Map<String, TransactionCacheTable<?>> tableCaches;


  /**
   * needs to be incremented by two everytime a number is drawn to make sure that there is one free
   * slot after each order
   */
  private AtomicInteger nextOrder = new AtomicInteger(0);
  private static final int ORDER_OFFSET = 3;


  public TransactionCacheTable<?> getUpdatedTableContent(String table) {
    if (tableCaches == null) {
      return null;
    }
    return tableCaches.get(table);
  }


  private TransactionCacheTable<?> checkTableCache(String table) {
    if (tableCaches == null) {
      tableCaches = new HashMap<String, TransactionCacheTable<?>>();
    }
    TransactionCacheTable<?> existingTableCache = tableCaches.get(table);
    if (existingTableCache == null) {
      existingTableCache = new TransactionCacheTable(table);
      tableCaches.put(table, existingTableCache);
    }
    return existingTableCache;
  }


  public <E extends Storable> void addDeletedTableContent(String table, MemoryRowData<E> rd, E deleted) {
    TransactionCacheTable<E> existingTableCache = (TransactionCacheTable<E>) checkTableCache(table);
    existingTableCache.addDeletedTableContent(rd, deleted, nextOrder.getAndAdd(ORDER_OFFSET));
  }


  public <E extends Storable> void addInsertedTableContent(String table, MemoryRowData<E> rd, E updatedContent) {
    TransactionCacheTable<E> existingTableCache = (TransactionCacheTable<E>) checkTableCache(table);
    existingTableCache.addInsertedTableContent(rd, updatedContent, nextOrder.getAndAdd(ORDER_OFFSET));
  }


  public <E extends Storable> void addUpdatedTableContent(String table, MemoryRowData<E> rd, E updatedContent) {
    TransactionCacheTable<E> existingTableCache = (TransactionCacheTable<E>) checkTableCache(table);
    existingTableCache.addUpdatedTableContent(rd, updatedContent, nextOrder.getAndAdd(ORDER_OFFSET));
  }


  public <E extends Storable> void addLockedRow(String table, MemoryRowData<E> rd, E lockedRow) {
    TransactionCacheTable<E> existingTableCache = (TransactionCacheTable<E>) checkTableCache(table);
    existingTableCache.addLockedRow(rd, lockedRow, nextOrder.getAndAdd(ORDER_OFFSET));
  }


  public Collection<TransactionCacheTable<?>> allUpdatedTablesOrNull() {
    if (tableCaches == null || tableCaches.size() == 0) {
      return null;
    }
    return tableCaches.values();
  }


  public void clear() {
    if (tableCaches != null) {
      for (TransactionCacheTable<?> tableCache: tableCaches.values()) {
        tableCache.clear();
      }
    }
  }


  public boolean isEmpty() {
    if (tableCaches == null || tableCaches.isEmpty()) {
      return true;
    }
    for (TransactionCacheTable<?> tableCache: tableCaches.values()) {
      if (!tableCache.isEmpty()) {
        return false;
      }
    }
    return true;
  }

}
