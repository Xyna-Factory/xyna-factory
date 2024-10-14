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
package com.gip.xyna.utils.collections.maps;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.queues.SimpleDelayQueue;

/**
 * TimeoutMap speichert wie eine Map, verliert die Einträge jedoch nach einem Timeout, 
 * welches beim Eintragen gesetzt wird.
 * 
 * TODO Zugriffe auf timeouts und map sind nicht atomar, daher nicht richtig threadsafe!
 * 
 *
 */
public class TimeoutMap<K,V> implements Map<K,V> {
  
  private SimpleDelayQueue<K> timeouts = new SimpleDelayQueue<K>();
  private ConcurrentHashMap<K, V> map = new ConcurrentHashMap<K, V>();
  
  public TimeoutMap() {
  }
  
  /**
   * wird bei jeder Map-Operation gerufen, um alte Einträge zu entfernen
   */
  private void removeOldEntries() {
    K key = timeouts.poll();
    while( key != null ) {
      map.remove(key);
      key = timeouts.poll();
    }
  }
  
  public List<Pair<K, Long>> listAllKeysOrdered( long baseTimeMillis ) {
    return timeouts.listAllEntriesOrdered(baseTimeMillis);
  }
  
  public boolean refresh(K key) {
    return refresh( key, timeouts.getDefaultDelay() );
  }
  
  public boolean refresh(K key, long timeToLive) {
    boolean removed = timeouts.remove(key);
    if( ! removed ) {
      return false;
    }
    while( removed ) { //Mehrfacheinträge entfernen
      removed = timeouts.remove(key);
    }
    timeouts.add(key, timeToLive);
    return true;
  }
  
  @Override
  public V put(K key, V value) {
    removeOldEntries();
    timeouts.add(key);
    return map.put(key, value);
  }

  public V put(K key, V value, long timeToLive) {
    removeOldEntries();
    timeouts.add(key, timeToLive);
    return map.put(key, value);
  }
  
  /**Stellt sicher, dass Eintrag eingetragen ist und Timeout aktuell ist
   * TODO nicht ganz richtig: anderer Thread 
   * könnte währenddessen Timeout sehen und map anschließend leeren...
   * @param key
   * @param value
   * @param timeToLive
   * @return
   */
  public V replace(K key, V value, long timeToLive) {
    removeOldEntries();
    if( ! refresh(key, timeToLive) ) {
      timeouts.add(key, timeToLive);
    }
    return map.put(key, value);
  }  

  public V replace2(K key, V value, long timeToLive) { //if V==Long, then replace is ambiguous
    return replace(key, value, timeToLive);
  }
  
  public void setDefaultDelay(long defaultDelay) {
    timeouts.setDefaultDelay(defaultDelay);
  }
  
  public long getDefaultDelay() {
    return timeouts.getDefaultDelay();
  }
  
  @Override
  public String toString() {
    removeOldEntries();
    return map.toString();
  }


  @Override
  public V remove(Object key) {
    removeOldEntries();
    timeouts.remove(key);
    return map.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    removeOldEntries();
    for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
      timeouts.add(e.getKey());
      map.put(e.getKey(), e.getValue());
    }
  }

  public void clear() {
    timeouts.clear();
    map.clear();
  }
  
  public boolean containsKey(Object key) {
    removeOldEntries();
    return map.containsKey(key);
  }

  public boolean containsValue(Object value) {
    removeOldEntries();
    return map.containsValue(value);
  }

  public Set<java.util.Map.Entry<K, V>> entrySet() {
    removeOldEntries();
    return map.entrySet();
  }

  public V get(Object key) {
    removeOldEntries();
    return map.get(key);
  }

  public boolean isEmpty() {
    removeOldEntries();
    return map.isEmpty();
  }

  public Set<K> keySet() {
    removeOldEntries();
    return map.keySet();
  }

  public int size() {
    removeOldEntries();
    return map.size();
  }

  public Collection<V> values() {
    removeOldEntries();
    return map.values();
  }
 
  @Override
  public int hashCode() {
    removeOldEntries();
    return map.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    removeOldEntries();
    return map.equals(obj);
  }

  
  
}
