/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.local.LocalRowData.SimpleLockWithUnderlyingData;
import com.gip.xyna.xnwh.persistence.memory.DataInterface;
import com.gip.xyna.xnwh.persistence.memory.MemoryRowLock;
import com.gip.xyna.xnwh.persistence.memory.TableObject;



public class LocalTableObject<T extends Storable> extends TableObject<T, LocalRowData<T>>
    implements
      DataInterface<T, LocalRowData<T>> {

  private final ReentrantReadWriteLock tableLock = new ReentrantReadWriteLock();
  private Map<Object, LocalRowData<T>> data;
  private Map<Object, LocalRowData<T>> uncommittedData;


  public LocalTableObject(Class<T> storableClass) throws PersistenceLayerException {
    super(storableClass);
    //TODO auf concurrenthashmap umstellen und damit einiges an lockingnotwendigkeit in tableobject-klasse unnötig machen -> benötigt aber noch anpassungen und zb verwendung von CAS operations auf der map...
    data = new HashMap<Object, LocalRowData<T>>();
    uncommittedData = new HashMap<Object, LocalRowData<T>>(); 
  }


  @Override
  public DataInterface<T, LocalRowData<T>> getDataInterface() {
    return this;
  }


  @Override
  public ReadWriteLock getTableLock() {
    return tableLock;
  }


  public LocalRowData<T> createRowData(PersistenceLayer pl, T content) throws PersistenceLayerException {
    LocalRowData<T> uncommitedExisting = uncommittedData.get(content.getPrimaryKey());
    if (uncommitedExisting != null) {
      return uncommitedExisting;
    }
    
    LocalRowData<T> result = createRowDataInternally(pl, content, true);
    result.setData(pl, content);
    return result;
  }


  private LocalRowData<T> createRowDataInternally(PersistenceLayer pl, T content, boolean committed) {
    return new LocalRowData<T>(this, content, committed);
  }


  public int dataSize(PersistenceLayer pl) {
    return data.size();
  }


  public Collection<LocalRowData<T>> values(PersistenceLayer pl) {
    Collection<LocalRowData<T>> result = data.values();
    return new ArrayList<LocalRowData<T>>(result); // return a copy
  }


  public LocalRowData<T> remove(PersistenceLayer pl, Object pk) {
    LocalRowData<T> removed = data.remove(pk);
    if (removed == null) {
      return null;
    }
    ((SimpleLockWithUnderlyingData) removed.getLock(pl).sustainedLock()).deleted();
    return removed;
  }


  public LocalRowData<T> get(PersistenceLayer pl, Object primaryKey) {
    return data.get(primaryKey);
  }


  public void put(PersistenceLayer pl, Object pk, LocalRowData<T> content) {
    data.put(pk, content);
  }


  public boolean containsKey(PersistenceLayer pl, Object pk) {
    return data.containsKey(pk);
  }


  public void dataClear(PersistenceLayer pl) {
    data.clear();
  }

  public void removeTemporaryObject(PersistenceLayer arg0, LocalRowData<T> arg1) {
    // nothing to be done for memory implementation, objects will be gc'ed
  }


  public MemoryRowLock putUncommitted(PersistenceLayer pl, T content) throws PersistenceLayerException {
    LocalRowData<T> result = uncommittedData.get(content.getPrimaryKey());
    if (result == null) {
      result = createRowDataInternally(pl, content, false);
      uncommittedData.put(content.getPrimaryKey(), result);
    }
    return result.getLock(pl);
  }


  public void moveUncommittedToCommitted(PersistenceLayer pl, T target) {
    LocalRowData<T> existingUncommittedRowData = uncommittedData.remove(target.getPrimaryKey());
    if (existingUncommittedRowData == null) {
      throw new RuntimeException("Cannot move uncommitted data, target row data does not exist in uncommitted storage: " + target.getPrimaryKey());
    }
    existingUncommittedRowData.setIsCommitted(true);
    if (data.put(target.getPrimaryKey(), existingUncommittedRowData) != null) {
      throw new RuntimeException("Uncommitted data may not replace committed data");
    }
  }

}
