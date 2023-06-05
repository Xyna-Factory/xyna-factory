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
package com.gip.xyna.xnwh.persistence.memory.index.map;



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;

import com.gip.xyna.xnwh.persistence.memory.index.AtomicBulkUpdate;
import com.gip.xyna.xnwh.persistence.memory.index.AtomicBulkUpdateBase;
import com.gip.xyna.xnwh.persistence.memory.index.Condition;
import com.gip.xyna.xnwh.persistence.memory.index.ConditionType;
import com.gip.xyna.xnwh.persistence.memory.index.Index;
import com.gip.xyna.xnwh.persistence.memory.index.ResultHandler;



public class IndexImplMap<E extends Comparable<E>, F> implements Index<E, F> {

  private NavigableMap<E, List<F>> map = new TreeMap<E, List<F>>();
  private ReadWriteLock lock;

  
  public IndexImplMap(ReadWriteLock lock) {
    this.lock = lock;
  }

  public void add(E e, F f) {
    lock.readLock().lock();
    try {
      addUnlocked(e, f);
    } finally {
      lock.readLock().unlock();
    }
  }


  private void addUnlocked(E e, F f) {
    List<F> l = map.get(e);
    if (l == null) {
      l = new ArrayList<F>();
      map.put(e, l);
    }
    l.add(f);
  }
  
  private void removeUnlocked(E e, F f) {
    List<F> l = map.get(e);
    if (l == null) {
      throw new RuntimeException();
    }
    if (l.size() == 0) {
      throw new RuntimeException();
    }
    if (l.size() == 1) {
      map.remove(e);
    } else {
      l.remove(f);
    }
  }


  public void readOnly(ResultHandler<F> handler, Condition<E> condition, boolean reverse) {
    lock.readLock().lock();
    try {
      NavigableMap<E, List<F>> partialMap;
      
      switch (condition.getType()) {
        case BIGGER :
        case BIGGER_OR_EQUAL :
          partialMap = map.tailMap(condition.getLookupValue(), condition.getType()==ConditionType.BIGGER_OR_EQUAL);
          if (reverse) {
            partialMap = partialMap.descendingMap();
          }
          Iterator<Entry<E, List<F>>> it = partialMap.entrySet().iterator();
          while (it.hasNext()) {
            Entry<E, List<F>> entry = it.next();
            E key = entry.getKey();
            if (key.compareTo(condition.getLookupValue()) == 0) {
              continue;
            }
            List<F> value = entry.getValue();
            if (!handler.handle(value)) {
              break;
            }
          }
          break;
        case SMALLER :
        case SMALLER_OR_EQUAL :
          partialMap = map.headMap(condition.getLookupValue(), condition.getType() == ConditionType.SMALLER_OR_EQUAL);
          if (reverse) {
            partialMap = partialMap.descendingMap();
          }
          it = partialMap.entrySet().iterator();
          while (it.hasNext()) {
            Entry<E, List<F>> entry = it.next();
            List<F> value = entry.getValue();
            if (!handler.handle(value)) {
              break;
            }
          }
          break;
        case EQUALS :
          List<F> value = map.get(condition.getLookupValue());
          if (value != null) {
            handler.handle(value);
          }
          break;
        default :
          throw new RuntimeException();
      }
    } finally {
      lock.readLock().unlock();
    }
  }


  public void rebalance() {

  }


  private class AtomicBulkUpdateHashMap extends AtomicBulkUpdateBase<E, F> {


    public AtomicBulkUpdateHashMap(ResultHandler<F> handler) {
      super(handler);
    }


    public void commit() {
      //in der richtigen reihenfolge abarbeiten

      AtomicBulkAction currentAction = actions.poll();
      lock.writeLock().lock();
      try {
        while (currentAction != null) {
          if (currentAction instanceof AtomicBulkUpdateBase.AtomicAddAction) {
            AtomicAddAction addAction = (AtomicAddAction) currentAction;
            addUnlocked(addAction.e, addAction.f);
          } else if (currentAction instanceof AtomicBulkUpdateBase.AtomicRemoveAction) {
            AtomicRemoveAction removeAction = (AtomicRemoveAction) currentAction;
            removeUnlocked(removeAction.e, removeAction.f);
          } else {
            //updateaction
            
            AtomicUpdateAction updateAction = (AtomicUpdateAction) currentAction;
            if (updateAction.nextKeyIsForAdd()) {
              addUnlocked(updateAction.newKey, updateAction.value);
            } else {
              removeUnlocked(updateAction.oldKey, updateAction.value);
            }
            
            if (updateAction.firstHandle) {
              updateAction.firstHandle = false;
              //key tauschen.
              updateAction.currentKey =
                  updateAction.nextKeyIsForAdd() ? updateAction.oldKey : updateAction.newKey;
              actions.add(updateAction);
            }
          }

          //nächstes element
          currentAction = actions.poll();
        }
      } finally {
        lock.writeLock().unlock();
      }
    }


  }


  public AtomicBulkUpdate<E, F> startBulkUpdate(ResultHandler<F> handler) {
    return new AtomicBulkUpdateHashMap(handler);
  }
  
  @Override
  public String toString() {
    return map.toString();
  }


}
