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

import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.xnwh.persistence.Storable;



public class TransactionCacheTable<T extends Storable> {

  private Map<Object, TransactionCacheEntry<T>> updatedTableData;
  private Map<Object, TransactionCacheEntry<T>> deletedTableData;
  private Map<Object, TransactionCacheEntry<T>> insertedTableData;
  private Map<Object, TransactionCacheEntry<T>> lockedRows;

  private String tableName;


  public TransactionCacheTable(String tableName) {
    this.tableName = tableName;
  }


  public String getTableName() {
    return this.tableName;
  }


  public void addUpdatedTableContent(MemoryRowData<T> rowdata, T updatedContent, Integer order) {
    if (updatedTableData == null) {
      updatedTableData = new HashMap<Object, TransactionCacheEntry<T>>();
    }
    updatedTableData.put(updatedContent.getPrimaryKey(), new TransactionCacheEntry<T>(order, rowdata, updatedContent));
    if (deletedTableData != null) {
      deletedTableData.remove(updatedContent.getPrimaryKey());
    }
  }


  public void addInsertedTableContent(MemoryRowData<T> rowdata, T updatedContent, Integer order) {
    if (insertedTableData == null) {
      insertedTableData = new HashMap<Object, TransactionCacheEntry<T>>();
    }
    insertedTableData.put(updatedContent.getPrimaryKey(), new TransactionCacheEntry<T>(order, rowdata, updatedContent));
    if (deletedTableData != null) {
      deletedTableData.remove(updatedContent.getPrimaryKey());
    }
  }


  public void addDeletedTableContent(MemoryRowData<T> rowdata, T deleted, Integer order) {
    if (deletedTableData == null) {
      deletedTableData = new HashMap<Object, TransactionCacheEntry<T>>();
    }
    deletedTableData.put(deleted.getPrimaryKey(), new TransactionCacheEntry<T>(order, rowdata, deleted));
    if (updatedTableData != null) {
      updatedTableData.remove(deleted.getPrimaryKey());
    }
    if (insertedTableData != null) {
      insertedTableData.remove(deleted.getPrimaryKey());
    }
    if (lockedRows != null) {
      lockedRows.remove(deleted.getPrimaryKey());
    }
  }


  public void addLockedRow(MemoryRowData<T> rowdata, T lockedRow, Integer order) {
    if (lockedRows == null) {
      lockedRows = new HashMap<Object, TransactionCacheEntry<T>>();
    }
    if (lockedRows.put(lockedRow.getPrimaryKey(), new TransactionCacheEntry<T>(order, rowdata, lockedRow)) != null) {
      throw new RuntimeException("Write lock added twice for primary key <" + lockedRow.getPrimaryKey()
          + "> in table <" + getTableName() + ">");
    }
  }


  public Map<Object, TransactionCacheEntry<T>> allUpdatedObjects() {
    return allUpdatedObjects(false);
  }


  public Map<Object, TransactionCacheEntry<T>> allUpdatedObjects(boolean lazyCreate) {
    if (lazyCreate) {
      if (updatedTableData == null) {
        updatedTableData = new HashMap<Object, TransactionCacheEntry<T>>();
      }
    }
    return updatedTableData;
  }


  public Map<Object, TransactionCacheEntry<T>> allInsertedObjects() {
    return insertedTableData;
  }


  public Map<Object, TransactionCacheEntry<T>> allDeletedObjects() {
    return deletedTableData;
  }


  public Map<Object, TransactionCacheEntry<T>> allLockedRows() {
    return lockedRows;
  }


  public void clear() {
    if (updatedTableData != null) {
      updatedTableData.clear();
    }
    if (insertedTableData != null) {
      insertedTableData.clear();
    }
    if (deletedTableData != null) {
      deletedTableData.clear();
    }
  }


  public boolean isEmpty() {
    return (updatedTableData == null || updatedTableData.isEmpty())
        && (insertedTableData == null || insertedTableData.isEmpty())
        && (deletedTableData == null || deletedTableData.isEmpty());
  }


}
