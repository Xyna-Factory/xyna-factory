/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
import java.util.Map;
import java.util.Set;


/**
 * Map-Implementierung, die alle Aufrufe an eine im Konstruktor �bergebene Map weiterreicht.
 * Dies vereinfacht das Implementieren von Dekoratoren. 
 */
public class WrappedMap<K,V> implements Map<K,V> {

  protected final Map<K,V> wrapped;
  
  protected WrappedMap(Map<K,V> map) {
    wrapped = map;
  }

  public void clear() {
    wrapped.clear();
  }

  public boolean containsKey(Object key) {
    return wrapped.containsKey(key);
  }

  public boolean containsValue(Object value) {
    return wrapped.containsValue(value);
  }

  public Set<java.util.Map.Entry<K, V>> entrySet() {
    return wrapped.entrySet();
  }

  public V get(Object key) {
    return wrapped.get(key);
  }

  public boolean isEmpty() {
    return wrapped.isEmpty();
  }

  public Set<K> keySet() {
    return wrapped.keySet();
  }

  public V put(K key, V value) {
    return wrapped.put(key, value);
  }

  public void putAll(Map<? extends K, ? extends V> t) {
    wrapped.putAll(t);
  }

  public V remove(Object key) {
    return wrapped.remove(key);
  }

  public int size() {
    return wrapped.size();
  }

  public Collection<V> values() {
    return wrapped.values();
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
