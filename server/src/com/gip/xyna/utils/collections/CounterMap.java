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
package com.gip.xyna.utils.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;



/**
 *
 */
public class CounterMap<K> extends WrappedMap<K, AtomicInteger> {
    
  public CounterMap() {
    super( new HashMap<K,AtomicInteger>() );
  }
  
  public AtomicInteger put(K key, int count) {
    return put(key, new AtomicInteger(count) );
  }
  
  public int add(K key, int add) {
    AtomicInteger counter = get(key);
    if( counter == null ) {
      counter = new AtomicInteger();
      put(key,counter);
    }
    return counter.addAndGet(add);
  }
 
  public int increment(K key) {
    AtomicInteger counter = get(key);
    if( counter == null ) {
      counter = new AtomicInteger();
      put(key,counter);
    }
    return counter.incrementAndGet();
  }
  
  public int getCount(K key) throws IllegalArgumentException {
    AtomicInteger counter = get(key);
    if( counter == null ) {
      throw new IllegalArgumentException("key "+key+" does not exist");
    }
    return counter.get();
  }

  public void add(CounterMap<K> counterMap) {
    for( Map.Entry<K, AtomicInteger> entry : counterMap.entrySet() ) {
      add( entry.getKey(), entry.getValue().get() );
    }
  }
  
  public int getTotalCount() {
    int sum = 0;
    for( Map.Entry<K, AtomicInteger> entry : entrySet() ) {
      sum += entry.getValue().get();
    }
    return sum;
  }
  
  /**
   * Gibt analog zu entrySet() alle Einträge der Map aus, nun aber in einer Liste sortiert nach dem Count.
   * Über die zurückgegebene Liste kann der Zählerstand geändert werden! 
   * @param descending
   * @return
   */
  public List<Map.Entry<K, AtomicInteger>> entryListSortedByCount(boolean descending) {
    List<Map.Entry<K, AtomicInteger>> list = new ArrayList<Map.Entry<K, AtomicInteger>>(entrySet());
    Collections.sort(list, new CountComparator(descending) );
    return list;
  }
  
  private static class CountComparator implements Comparator<Map.Entry<?, AtomicInteger>> {

    private boolean descending;

    public CountComparator(boolean descending) {
      this.descending = descending;
    }

    public int compare(java.util.Map.Entry<?, AtomicInteger> o1, java.util.Map.Entry<?, AtomicInteger> o2) {
      int c1 = o1.getValue().get();
      int c2 = o2.getValue().get();
      if( descending ) {
        return c2 - c1;
      } else {
        return c1 - c2;
      }
    }
    
  }

}
