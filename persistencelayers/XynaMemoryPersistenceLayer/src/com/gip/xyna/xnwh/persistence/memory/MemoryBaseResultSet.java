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



import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.SimpleResultSetMetaData;
import com.gip.xyna.xnwh.persistence.Storable;



/**
 * muss die methoden implementieren, die f�r resultsetreader zur verf�gung stehen sollen. TODO performance: in
 * verbindung mit preparedquerycomponent: erst bei resultset.next() weiteriterieren, und nicht zuerst die gesamte liste
 * durchlaufen.
 */
public abstract class MemoryBaseResultSet extends IMemoryBaseResultSet {

  protected ArrayList<MemoryRowData<?>> data = new ArrayList<MemoryRowData<?>>();
  protected int currentIdx = -1;
  private List<MemoryRowLock> locks;

  private final PersistenceLayer persistencelayer;
  private Storable currentData;
  private final boolean sustainedLocks;

  public MemoryBaseResultSet(PersistenceLayer persistencelayer, boolean writeLocks) {
    this.persistencelayer = persistencelayer;
    this.sustainedLocks = writeLocks;
  }


  @Override
  public void unlockReadLocks() {
    if (locks != null) {
      for (MemoryRowLock l : locks) {
        l.temporaryLock().readLock().unlock();
      }
      if (!sustainedLocks) {
        locks = null;
      }
    }
  }


  public boolean next() throws SQLException {

    currentData = null;
    while (currentData == null) {
      currentIdx++;
      if (data.size() <= currentIdx) {
        return false;
      } else {
        try {
          currentData = data.get(currentIdx).getData(persistencelayer);
        } catch (UnderlyingDataNotFoundException e) {
          // this can happen if no "for update" has been included and the row has been deleted in the meantime. TODO sicher? dazu halten wir doch das readlock
          continue;
        }
      }
    }

    // found another row
    return true;

  }


  public void add(MemoryRowData rd, MemoryRowLock lock) {
    data.add(rd);
    if (locks == null) {
      locks = new ArrayList<MemoryRowLock>();
    }
    locks.add(lock);
  }


  @Override
  public List<MemoryRowLock> getWriteLocks() {
    if (sustainedLocks) {
      return locks;
    }
    return null;
  }


  public void orderByAndTruncateResult(Comparator comparator, int maxRows) {
    if (comparator != null) {
      Collections.sort(data, comparator);
    }
    while (data.size() > maxRows) {
      int idx = data.size() - 1;
      data.remove(idx);
      MemoryRowLock l = locks.remove(idx);
      if (sustainedLocks) {
        l.sustainedLock().unlock();
      }
      l.temporaryLock().readLock().unlock();
    }
  }


  protected void ensureIdx() throws SQLException {
    wasNull = false;
    if (currentIdx >= data.size() || currentIdx < 0) {
      throw new SQLException("can not access data. please use resultSet.next() to ensure, data is accessible.");
    }
  }


  protected Storable getCurrentData() {
    return currentData;
  }


  public int size() {
    return data.size();
  }


  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    return new SimpleResultSetMetaData(currentData);
  }


  public void clear() {
    data.clear();
    unlockReadLocks();
    unlockWriteLocks();
  }


}
