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

package com.gip.xyna.xnwh.persistence.memory;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;



public class TestTableObject<T extends Storable> extends TableObject<T, TestTableObject.TestMemoryRowData<T>>
    implements
      DataInterface<T, TestTableObject.TestMemoryRowData<T>> {

  private Map<Object, TestMemoryRowData<T>> data;
  private ReentrantReadWriteLock lock;

  private Map<Object, TestMemoryRowData<T>> uncommittedData;


  public TestTableObject(Class<T> storableClass) throws PersistenceLayerException {
    super(storableClass);
    data = new HashMap<Object, TestMemoryRowData<T>>();
    uncommittedData = new HashMap<Object, TestMemoryRowData<T>>();
    lock = new ReentrantReadWriteLock();
  }


  @Override
  public DataInterface<T, TestTableObject.TestMemoryRowData<T>> getDataInterface() {
    return this;
  }


  @Override
  public ReadWriteLock getTableLock() {
    return lock;
  }


  private TestMemoryRowData<T> createRowDataInternally(PersistenceLayer pl, T content, boolean committed) {
    return new TestMemoryRowData<T>(this, content, committed);
  }


  public TestMemoryRowData<T> createRowData(PersistenceLayer pl, T content) throws PersistenceLayerException {
    synchronized (uncommittedDataLock) {
      TestMemoryRowData<T> uncommitedExisting = uncommittedData.get(content.getPrimaryKey());
      if (uncommitedExisting == null) {
        throw new RuntimeException("Missing uncommitted data on insert");
      }
      return uncommitedExisting;
//      return createRowDataInternally(pl, content, true);
    }
  }


  public int dataSize(PersistenceLayer pl) {
    return data.size();
  }


  public List<TestTableObject.TestMemoryRowData<T>> values(PersistenceLayer pl) {
    Collection<TestTableObject.TestMemoryRowData<T>> result = data.values();
    if (result instanceof List) {
      return (List<TestTableObject.TestMemoryRowData<T>>) result;
    } else {
      return new ArrayList<TestTableObject.TestMemoryRowData<T>>(result);
    }
  }


  public TestMemoryRowData<T> remove(PersistenceLayer pl, Object pk) {
    //data entfernen und lock invalidieren
    TestMemoryRowData<T> deletedValue = data.remove(pk);;
    ((SimpleLockWithUnderlyingData) deletedValue.getLock(pl).sustainedLock()).dataDeleted = true;
    return deletedValue;
  }


  public TestTableObject.TestMemoryRowData<T> get(PersistenceLayer pl, Object primaryKey) {
    return data.get(primaryKey);
  }


  public void put(PersistenceLayer pl, Object pk, TestTableObject.TestMemoryRowData<T> content) {
    data.put(pk, content);
  }


  public boolean containsKey(PersistenceLayer pl, Object pk) {
    return data.containsKey(pk);
  }


  public void dataClear(PersistenceLayer pl) {
    data.clear();
  }


  public static class TestMemoryRowData<T extends Storable> extends MemoryRowData<T> {

    private MemoryRowLock lock = new MemoryRowLock(new ReentrantReadWriteLock(), new SimpleLockWithUnderlyingData());
    private T data;

    private final long uniqueId;


    private static AtomicLong uniqueIdGenerator = new AtomicLong();


    public TestMemoryRowData(TableObject<T, ? extends MemoryRowData<T>> parent, T data, boolean committed) {
      super(parent, committed);
      this.data = data;
      this.uniqueId = uniqueIdGenerator.incrementAndGet();
    }


    @Override
    public MemoryRowLock getLock(PersistenceLayer pl) {
      return lock;
    }


    @Override
    public T getData(PersistenceLayer pl) {
      return data;
    }


    @Override
    protected T setDataInternally(PersistenceLayer pl, T storable) {
      T previousValue = this.data;
      this.data = storable;
      return previousValue;
    }


    @Override
    public long getUniqueID() {
      return uniqueId;
    }
    
    @Override
    public String toString() {
      return data.toString();
    }

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
    
  }


  public void removeTemporaryObject(PersistenceLayer pl, TestTableObject.TestMemoryRowData<T> data) {
    // nothing to be done for memory implementation
  }


  private Object uncommittedDataLock = new Object();


  public MemoryRowLock putUncommitted(PersistenceLayer pl, T content) throws PersistenceLayerException {
    synchronized (uncommittedDataLock) {
      TestMemoryRowData<T> result = uncommittedData.get(content.getPrimaryKey());
      if (result == null) {
        result = createRowDataInternally(pl, content, false);
        uncommittedData.put(content.getPrimaryKey(), result);
      }
      return result.getLock(pl);
    }
  }


  public void moveUncommittedToCommitted(PersistenceLayer pl, T target) {
    synchronized (uncommittedDataLock) {
      TestMemoryRowData<T> existingUncommittedRowData = uncommittedData.remove(target.getPrimaryKey());
      if (existingUncommittedRowData == null) {
        throw new RuntimeException(
                                   "Cannot move uncommitted data, target row data does not exist in uncommitted storage");
      }
      existingUncommittedRowData.setIsCommitted(true);
      if (data.put(target.getPrimaryKey(), existingUncommittedRowData) != null) {
        throw new RuntimeException("Uncommitted data may not replace committed data");
      }
    }
  }

}
