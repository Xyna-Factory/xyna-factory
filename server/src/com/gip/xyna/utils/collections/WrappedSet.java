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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


/**
 * Set-Implementierung, die alle Aufrufe an ein im Konstruktor übergebenes Set weiterreicht.
 * Dies vereinfacht das Implementieren von Dekoratoren. 
 */
public class WrappedSet<E> implements Set<E> {
  
  protected Set<E> wrapped;
  
  protected  WrappedSet( Set<E> set) {
    this.wrapped = set;
  }

  public int size() {
    return wrapped.size();
  }

  public boolean isEmpty() {
    return wrapped.isEmpty();
  }

  public boolean contains(Object o) {
    return wrapped.contains(o);
  }

  public Iterator<E> iterator() {
    return wrapped.iterator();
  }

  public Object[] toArray() {
    return wrapped.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return wrapped.toArray(a);
  }

  public boolean add(E o) {
    return wrapped.add(o);
  }

  public boolean remove(Object o) {
    return wrapped.remove(o);
  }

  public boolean containsAll(Collection<?> c) {
    return wrapped.containsAll(c);
  }

  public boolean addAll(Collection<? extends E> c) {
    return wrapped.addAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return wrapped.retainAll(c);
  }

  public boolean removeAll(Collection<?> c) {
    return wrapped.removeAll(c);
  }

  public void clear() {
    wrapped.clear();
  }
  
  @Override
  public String toString() {
    return wrapped.toString();
  }
  
  @Override
  public int hashCode() {
    return wrapped.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    return wrapped.equals(obj);
  }

}
