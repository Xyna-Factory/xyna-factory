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

package com.gip.xyna.xfmg.xods.configuration;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * erweiterung einer normalen map um einen modified-counter und eine methode um eine nicht modifizierbare version der map zu bekommen 
 *
 */
public class PropertyMap<K, V> extends ConcurrentHashMap<K, V> {
  
  private static final long serialVersionUID = 1L;
  private AtomicInteger modifiedCount = new AtomicInteger(0);
  
  public PropertyMap() {
    super();
  }
  
  private void incModCnt() {
    if (modifiedCount != null) {
      modifiedCount.incrementAndGet();
    }
  }
  
  PropertyMap(Map<K, V> map, int modifiedCount) {
    super(map);
    this.modifiedCount = new AtomicInteger(modifiedCount);
  }

  /**
   * erstellt eine nicht modifizierbare kopie der map und übernimmt den modifiedcount
   */
  public PropertyMap<K, V> getAsUnmodifiablePropertyMap() {
    Map<K, V> newMap = Collections.unmodifiableMap(this);
    return new PropertyMap<K, V>(newMap, getModifiedCount());
  }
  
  public int getModifiedCount() {
    return modifiedCount.get();
  }

  @Override
  public void clear() {
    super.clear();
    incModCnt();
  }
  
  public V put(K key, V value) {
    V ret = super.put(key, value);
    incModCnt();
    return ret;
  };
  
  public V putIfAbsent(K key, V value) {
    V ret = super.putIfAbsent(key, value);
    incModCnt();
    return ret;
  };

  @Override
  public V remove(Object key) {
    V ret = super.remove(key);
    incModCnt();
    return ret;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> t) {
    super.putAll(t);
    incModCnt();
  }

  @Override
  public boolean remove(Object key, Object value) {
    boolean ret = super.remove(key, value);
    incModCnt();
    return ret;
  }
  
  public boolean replace(K key, V oldValue, V newValue) {
    boolean ret = super.replace(key, oldValue, newValue);
    incModCnt();
    return ret;
  };
  
  public V replace(K key, V value) {
    V ret = super.replace(key, value);
    incModCnt();
    return ret;
  };
  
  
}
