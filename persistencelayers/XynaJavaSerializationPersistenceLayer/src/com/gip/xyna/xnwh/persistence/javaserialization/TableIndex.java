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

package com.gip.xyna.xnwh.persistence.javaserialization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;



public class TableIndex extends ArrayList<RowObject> implements Serializable {

  private static final long serialVersionUID = 823962302633743100L;

  public enum Status {SINGLE, BLOB};
    
  private Status status = Status.SINGLE;
  private transient ReentrantLock lock = new ReentrantLock(true);
  

  public TableIndex() {
    super();
  }
  
  public void init() {
    lock = new ReentrantLock(true);
  }
  
  public void getLock() {
    if (!lock.isHeldByCurrentThread()) {
      lock.lock();
    }
  }
  
  
  public void releaseLock() {
    if (lock.isHeldByCurrentThread()) {
      lock.unlock();
    }
  }
  
  public Status getStatus() {
    return status;
  }

  
  public void setStatus(Status status) {
    this.status = status;
  }


  @Override
  public boolean contains(Object elem) {
    for (RowObject row : this) {
      // simply comparing the hashcodes is not enough since these may collide without
      // the objects being equal while equality according to 'equals' is expected to
      // mean that the objects are really identical
      if (row.getPrimaryKey().equals(elem)) {
        return true;
      }
    }
    return false;
  }
  
  
  @Override
  public boolean containsAll(Collection<?> elems) {
    for (Object obj : elems) {
      if (!(obj instanceof Storable)) {
        // throw new XynaException("Not storable object send to the persistence layer");
        return false;
      }
      if (!this.contains(((Storable) obj).getPrimaryKey())) {
        return false;
      }
    }
    return true;
  }
    
  
  // calculates the offset in bytes to the beginning of the indexed object
  public long calculateOffset(RowObject rowToBeCalculated) throws PersistenceLayerException {

    long offset = 0;
    for (RowObject row : this) {
      if (row.getPrimaryKey().equals(rowToBeCalculated.getPrimaryKey())) {
        return offset;
      }
      for (ColumnObject column : row) {
        offset += column.getSize();
      }
    }
    throw new XNWH_GeneralPersistenceLayerException("specified row was not indexed");
  }

  
  //will be needed later on
  //calculates the offset to the named field of the indexed object
  public long calculateOffset(int index, String fieldName) {
    return 1l;
  }
}
