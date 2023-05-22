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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class LruCacheWithTimingInformation<K, V> {

  
  private final LruCache<AddedTime<K>, V> innerCache;
  private final AtomicLong lastEvicted = new AtomicLong(0);

  public LruCacheWithTimingInformation(int maxEntries) {
    innerCache = new LruCache<AddedTime<K>, V>(maxEntries) {

      private static final long serialVersionUID = 1L;

      @Override
      protected boolean removeEldestEntry(java.util.Map.Entry<AddedTime<K>, V> eldest) {
        if (super.removeEldestEntry(eldest)) {
          lastEvicted.set(eldest.getKey().time);
          return true;
        }
        return false;
      }
    };
  }

  static class AddedTime<K> {
    public final K k;
    public final long time;
    public AddedTime(K k, long time) {
      this.k = k;
      this.time = time;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((k == null) ? 0 : k.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      AddedTime<?> other = (AddedTime<?>) obj;
      if (k == null) {
        if (other.k != null)
          return false;
      } else if (!k.equals(other.k))
        return false;
      return true;
    }
  }
  
  public long creationTimeOfLastEvictedKey() {
    return lastEvicted.get();
  }
  
  public V put(K key, V value) {
    return innerCache.put(new AddedTime<K>(key, System.currentTimeMillis()), value);
  }

  public int size() {
    return innerCache.size();
  }

  public V get(K key) {
    return innerCache.get(new AddedTime<K>(key, -1));
  }

  public void remove(K key) {
    innerCache.remove(new AddedTime<K>(key, -1));
  }

  public Set<K> keySet() {
    Set<K> s = new HashSet<>();
    for (AddedTime<K> key : innerCache.keySet()) {
      s.add(key.k);
    }
    return s;
  }
  
  
}
