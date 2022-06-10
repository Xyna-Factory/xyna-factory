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
package com.gip.xyna.xnwh.persistence.memory.index;

import java.util.PriorityQueue;


public abstract class AtomicBulkUpdateBase<E extends Comparable<E>, F> implements AtomicBulkUpdate<E, F> {

  protected abstract class AtomicBulkAction implements Comparable<AtomicBulkAction> {

    public abstract E getKey();


    /*
     * adds gehen immer auf den darauffolgenden knoten => reihenfolge muss immer so sein, dass adds bei gleichen keys später kommen.
     * das gleiche gilt für update-adds.
     * => reihenfolge:
     * 'remove a' < 'add a' < 'remove b' < 'add b'
     */
    public int compareTo(AtomicBulkAction o) {
      if (getKey() == null) {
        if (o.getKey() == null) {
          return 0;
        }
        return -1;
      }
      if (o.getKey() == null) {
        return 1;
      }
      int result = getKey().compareTo(o.getKey());
      if (result == 0) {
        int thisIsAddAction = isAddAction() ? 1 : 0;
        int oIsAddAction = o.isAddAction() ? 1 : 0;
        return thisIsAddAction - oIsAddAction;
      }
      return result;
    }


    public boolean isAddAction() {
      return this instanceof AtomicBulkUpdateBase.AtomicAddAction
          || (this instanceof AtomicBulkUpdateBase.AtomicUpdateAction && ((AtomicUpdateAction) this)
              .nextKeyIsForAdd());
    }
    
  }

  protected class AtomicAddAction extends AtomicBulkAction {

    public E e;
    public F f;


    public AtomicAddAction(E e, F f) {
      this.e = e;
      this.f = f;
    }


    @Override
    public E getKey() {
      return e;
    }
    
    @Override
    public String toString() {
      return "A" + e;
    }

  }

  protected class AtomicRemoveAction extends AtomicBulkAction {

    public E e;
    public F f;


    public AtomicRemoveAction(E e, F f) {
      this.e = e;
      this.f = f;
    }


    @Override
    public E getKey() {
      return e;
    }
    
    @Override
    public String toString() {
      return "R" + e;
    }

  }

  protected class AtomicUpdateAction extends AtomicBulkAction {

    public E oldKey;
    public E newKey;
    public F value;
    public E currentKey;
    public boolean firstHandle = true;


    public AtomicUpdateAction(E oldKey, E newKey, F value) {
      this.oldKey = oldKey;
      this.newKey = newKey;
      this.value = value;
      if (oldKey.compareTo(newKey) < 0) {
        currentKey = oldKey;
      } else {
        currentKey = newKey;
      }
    }


    public boolean nextKeyIsForAdd() {
      return currentKey == newKey;
    }


    @Override
    public E getKey() {
      return currentKey;
    }
    
    @Override
    public String toString() {
      return "U" + oldKey + "->" + newKey;
    }

  }


  //sortierte liste von actions. sortiert nach keys - duplizierte keys sind erlaubt => priorityqueue ist am besten geeignet
  protected PriorityQueue<AtomicBulkAction> actions;
  protected ResultHandler<F> handler;
  
  public AtomicBulkUpdateBase(ResultHandler<F> handler) {
    this.handler = handler;
    actions = new PriorityQueue<AtomicBulkAction>();
  }

  public void add(E e, F f) {
    actions.add(new AtomicAddAction(e, f));
  }


  //TODO F-Finder anstatt f direkt? ggfs kann man f nicht über die object-identity finden?
  public void remove(E e, F f) {
    actions.add(new AtomicRemoveAction(e, f));
  }


  public void update(E oldKey, E newKey, F value) {
    if (oldKey.compareTo(newKey) == 0) {
      return;
    }
    //braucht man atomicUpdateAction?
   /* add(newKey, value);
    remove(oldKey, value);*/
    actions.add(new AtomicUpdateAction(oldKey, newKey, value));
  }
  
  public int size() {
    return actions.size();
  }
  
  @Override
  public String toString() {
    return actions.toString();
  }

}
