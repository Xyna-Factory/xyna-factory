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

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


/**
 * LinkedList, die selbst überwacht, ob sie geordnet ist. Über isOrdered() kann abgefragt werden, ob sie 
 * noch geordnet ist und mit order() wieder sortiert werden.
 */
public class OrderedLinkedList<E extends Comparable<E> > extends AbstractList<E> {

  private LinkedList<E> data;
  private boolean ordered;
  
  public OrderedLinkedList() {
    data = new LinkedList<E>();
    ordered = true;
  }
  
  public OrderedLinkedList(Collection<? extends E> c) {
    data = new LinkedList<E>(c);
    ordered = false;
  }
  
  public boolean add(E o) {
    ordered = false;
    return data.add(o);
  };
  
  @Override
  public boolean addAll(Collection<? extends E> c) {
    ordered = false;
    return data.addAll(c);
  }
  
  @Override
  public boolean remove(Object o) {
    return data.remove(o);
  }
  
  @Override
  public E remove(int index) {
    return data.remove(index);
  }
  
  @Override
  public Iterator<E> iterator() {
    return data.iterator();
  }
  
  @Override
  public ListIterator<E> listIterator() {
    return data.listIterator();
  }
  
  @Override
  public ListIterator<E> listIterator(int index) {
    return data.listIterator(index);
  }
 
  /**
   * @return the ordered
   */
  public boolean isOrdered() {
    return ordered;
  }
  
  /**
   * Sortieren der Daten
   */
  public void order() {
    Collections.sort(data);
    ordered = true;
  }
  
  
  @Override
  public E get(int index) {
    return data.get(index);
  }

  @Override
  public int size() {
    return data.size();
  }
  
  List<E> getData() {
    return Collections.unmodifiableList(data);
  }
  
}
