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
package com.gip.xyna.utils.collections.sets;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;


/**
 *
 */
public class WeakHashSet<E> implements Set<E> {
  
  protected static final Object PRESENT = new Object();
  
  protected WeakHashMap<E,Object> baseMap;
  
  public WeakHashSet() {
    baseMap = new WeakHashMap<E,Object>();
  }
  
  public WeakHashSet(int initialCapacity) {
    baseMap = new WeakHashMap<E,Object>(initialCapacity);
  }
  
  public WeakHashSet( Set<E> set) {
    this(set.size());
    addAll(set);
  }
  
  public int size() {
    return baseMap.size();
  }

  public boolean isEmpty() {
    return baseMap.isEmpty();
  }

  public boolean contains(Object o) {
    return baseMap.containsKey(o);
  }

  public Iterator<E> iterator() {
    return baseMap.keySet().iterator();
  }

  public Object[] toArray() {
    return baseMap.keySet().toArray();
  }

  public <T> T[] toArray(T[] a) {
    return baseMap.keySet().toArray(a);
  }

  public boolean add(E o) {
    return baseMap.put(o, PRESENT)==null;
  }

  public boolean remove(Object o) {
    return baseMap.remove(o)==PRESENT;
  }

  public boolean containsAll(Collection<?> c) {
    return baseMap.keySet().containsAll(c);
  }

  public boolean addAll(Collection<? extends E> c) {
    boolean changed = false;
    for( E e : c ) {
      changed |= add(e);
    }
    return changed;
  }

  public boolean retainAll(Collection<?> c) {
    return baseMap.keySet().retainAll(c);
  }

  public boolean removeAll(Collection<?> c) {
    return baseMap.keySet().removeAll(c);
  }

  public void clear() {
    baseMap.clear();
  }
  
  @Override
  public String toString() {
    return baseMap.keySet().toString();
  }
  
  @Override
  public int hashCode() {
    return baseMap.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if( obj == this ) {
      return true;
    }
    if( !(obj instanceof Set) ) {
      return false;
    }
    Set<?> s = (Set<?>) obj;
    if( s.size() != size() ) {
      return false;
    }
    return containsAll(s);
  }

}
