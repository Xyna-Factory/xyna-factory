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

package com.gip.xyna.coherence.ha;

import java.io.Serializable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;



public class BulkActionContainer implements Serializable {


  private static final long serialVersionUID = 1L;


  private static Iterator<Long> emptyLongIterator = new EmptyLongIterator();
  private static Iterator<BulkActionElement> emptyElementsIterator = new EmptyElementIterator();


  private SortedMap<Long, BulkActionElement> elements;
  private SortedSet<Long> locks;
  private SortedMap<Long, Long> explicitUnlocks;


  public BulkActionContainer() {
  }


  public void addElement(long order, BulkActionElement element) {
    if (elements == null) {
      elements = new TreeMap<Long, BulkActionElement>();
    }
    elements.put(order, element);
  }


  public Iterator<BulkActionElement> orderedElementIterator() {
    if (elements != null && elements.size() > 0) {
      return elements.values().iterator();
    } else {
      return emptyElementsIterator;
    }
  }


  public void addLock(long order, long objectId) {
    if (locks == null) {
      locks = new TreeSet<Long>();
    }
    locks.add(objectId);
  }


  public Iterator<Long> orderedLockIterator() {
    if (locks != null && locks.size() > 0) {
      return locks.iterator();
    } else {
      return emptyLongIterator;
    }
  }


  public void addExplicitUnlock(long order, long objectId) {
    if (explicitUnlocks == null) {
      explicitUnlocks = new TreeMap<Long, Long>();
    }
    explicitUnlocks.put(order, objectId);
  }


  public Iterator<Long> orderedUnlockIterator() {
    if (explicitUnlocks != null && explicitUnlocks.size() > 0) {
      return explicitUnlocks.values().iterator();
    } else {
      return emptyLongIterator;
    }
  }


  private static class EmptyLongIterator implements Iterator<Long> {

    public boolean hasNext() {
      return false;
    }


    public Long next() {
      return null;
    }


    public void remove() {
      throw new RuntimeException("unsupported");
    }

  }


  private static class EmptyElementIterator implements Iterator<BulkActionElement> {

    public boolean hasNext() {
      return false;
    }


    public BulkActionElement next() {
      return null;
    }


    public void remove() {
      throw new RuntimeException("unsupported");
    }

  }

}
