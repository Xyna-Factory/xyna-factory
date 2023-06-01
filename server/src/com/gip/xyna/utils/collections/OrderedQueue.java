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

package com.gip.xyna.utils.collections;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;


/**
 * OrderedQueue ist eine Queue-Implementierung, bei der alle Einträge durch die 
 * natürliche Ordnung durch Comparable&lt;E&gt; angeordnet sind.
 * Dadurch haben offer(E o) und add(E o) ein O(N)-Verhalten.
 * Über refresh() kann der erste Eintrag wieder neu einsortiert werden, wenn  
 * sein Ordnungskriterium geändert wurde.
 */
public class OrderedQueue<E extends Comparable<E> > extends AbstractQueue<E> {

  private LinkedList<E> data;
  
  public OrderedQueue() {
    data = new LinkedList<E>();
  }
  
  @Override
  public boolean addAll(Collection<? extends E> c) {
    if( data.addAll(c) ) {
      Collections.sort(data);
      return true;
    }
    return false;
  }
  
  
  @Override
  public Iterator<E> iterator() {
    return data.iterator();
  }

  @Override
  public int size() {
    return data.size();
  }

  public boolean offer(E o) {
    for( ListIterator<E> iter = data.listIterator(0); iter.hasNext();  ) {
      if( iter.next().compareTo(o) > 0 ) {
        iter.previous();
        iter.add(o);
        return true;
      }
    }
    data.add(o);
    return true;
  }

  public E peek() {
    if( data.isEmpty() ) {
      return null;
    }
    return data.getFirst();
  }

  public E poll() {
    if( data.isEmpty() ) {
      return null;
    }
    return data.removeFirst();
  }
  
  public boolean refresh() {
    return offer( poll() );  
  }
  
  @Override
  public void clear() {
    data.clear();
  }
  
}
