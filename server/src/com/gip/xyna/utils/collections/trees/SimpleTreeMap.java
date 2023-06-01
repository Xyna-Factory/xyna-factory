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
package com.gip.xyna.utils.collections.trees;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.collections.WrappedMap;


public class SimpleTreeMap<K extends SimpleTreeSet.TreeElement<K>, V> extends WrappedMap<K, V> {

  private SimpleTreeSet<K> tree;
  
  public SimpleTreeMap() {
    super(new HashMap<K,V>());
    tree = new SimpleTreeSet<K>();
  }
  
  public SimpleTreeMap(Map<K, V> m) {
    super(new HashMap<K,V>());
    tree = new SimpleTreeSet<K>();
    putAll(m);
  }

  public List<K> keyList() {
    return tree.toUnmodifiableList();
  }  

  public V put(K key, V value) {
    if( value == null ) {
      throw new IllegalArgumentException("SimpleTreeMap cannot contain null values");
    }
    V previous = wrapped.put(key, value);
    if( previous == null ) {
      tree.add(key);
    }
    return previous;
  }

  public void putAll(Map<? extends K, ? extends V> t) {
    for( Map.Entry<? extends K, ? extends V> entry : t.entrySet() ) {
      put(entry.getKey(), entry.getValue());
    }
  }

  public V remove(K key) {
    boolean removed = tree.remove(key);
    if( removed ) {
      return wrapped.remove(key);
    } else {
      return null;
    }
  }
 
  @Override
  public String toString() {
    return wrapped.toString();
  }

  public void sort() {
    tree.sort();
  }
  public void sort(Comparator<K> comparator) {
    tree.sort(comparator);
  }
  
  public K getParent(K key) {
    return tree.getParent(key);
  }
  
  public List<K> getChildren(K key, boolean recursively) {
    return tree.getChildren( key, recursively );
  }

}
