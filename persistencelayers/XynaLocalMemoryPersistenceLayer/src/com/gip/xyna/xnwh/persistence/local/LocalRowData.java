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

package com.gip.xyna.xnwh.persistence.local;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.memory.LockWithUnderlyingData;
import com.gip.xyna.xnwh.persistence.memory.MemoryRowData;
import com.gip.xyna.xnwh.persistence.memory.MemoryRowLock;
import com.gip.xyna.xnwh.persistence.memory.UnderlyingDataNotFoundException;


public class LocalRowData<T extends Storable> extends MemoryRowData<T> {


  private static AtomicLong uniqueIdGenerator = new AtomicLong();

  private final long uniqueId;


  T rowData; //pk ist erstes feld
  private MemoryRowLock rowLock;


  public LocalRowData(LocalTableObject<T> parent, T content, boolean committed) {
    super(parent, committed);
    uniqueId = uniqueIdGenerator.getAndIncrement();
    this.rowData = content;
  }


  @Override
  public T getData(PersistenceLayer pl) {
    return rowData;
  }


  @Override
  protected T setDataInternally(PersistenceLayer pl, T storable) {
    T previousValue = this.rowData;
    this.rowData = storable;
    return previousValue;
  }


  @Override
  public MemoryRowLock getLock(PersistenceLayer pl) {
    if (rowLock == null) {
      synchronized (this) {
        if (rowLock == null) {
          rowLock = new MemoryRowLock(new ReentrantReadWriteLock(), new SimpleLockWithUnderlyingData());
        }
      }
    }
    return rowLock;
  }


  public static class SimpleLockWithUnderlyingData implements LockWithUnderlyingData {

    private boolean dataDeleted = false;
    
    private Lock lock = new ReentrantLock();

    public void lock() throws UnderlyingDataNotFoundException {
      if (dataDeleted) {
        throw new UnderlyingDataNotFoundException(null);
      }
      lock.lock();
      if (dataDeleted) {
        lock.unlock();
        throw new UnderlyingDataNotFoundException(null);
      }
    }

    public void unlock() {
      lock.unlock();
    }

    public void deleted() {
      dataDeleted = true;
    }

  }


  @Override
  public long getUniqueID() {
    return uniqueId;
  }

}
