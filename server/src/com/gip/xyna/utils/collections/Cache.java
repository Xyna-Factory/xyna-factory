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

import java.util.concurrent.ConcurrentHashMap;



/**
 * einfacher threadsicherer cache.
 *
 */
public class Cache<U, V> {

  /*
   * problem: ein thread entfernt eines oder mehrere objekte aus dem cache, und gleichzeitig läuft die
   * cache-entry-creation. es muss sichergestellt werden, dass bemerkt wird, dass das objekt zu alt ist
   * und das objekt dann erneut erstellt wird.
   * es darf nie passieren, dass der cache ein objekt enthält, welches begonnen wurde zu erstellen, bevor es
   * das letzte mal aus dem cache entfernt wurde.
   * 
   */
  
  public interface CacheEntryCreation<U, V> {
    public V create(U key);
  }
  
  private final ConcurrentHashMap<U, V> map = new ConcurrentHashMap<U, V>();
  private final CacheEntryCreation<U, V> creation;
  private final static Object PLACE_HOLDER = new Object();
  
  public Cache(CacheEntryCreation<U, V> creation) {
    this.creation = creation;
  }

  public V getOrCreate(final U key) {
    V value = map.get(key);
    while (value == null || value == PLACE_HOLDER) {
      while (value == PLACE_HOLDER) {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
        }
        value = map.get(key);
      }
      if (value == null) {
        //cache befüllen
        if (((ConcurrentHashMap)map).putIfAbsent(key, PLACE_HOLDER) == null) {
          //place holder, damit man bemerkt, wenn der cache geleert wird, während man den value erstellt
          boolean success = false;
          try {
            value = creation.create(key);
            success = true;
          } finally {
            if (!success) {
              map.remove(key);
            }
          }
          if (!((ConcurrentHashMap)map).replace(key, PLACE_HOLDER, value)) {
            //sollte nicht passieren können
            value = map.get(key);
          }
        } else {
          value = map.get(key);
        }
      }
    }
    return value;
  }
  
  public void clearCache() {
    map.clear();
  }
  
  public void removeFromCache(U key) {
    map.remove(key);
  }
  
}
