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



import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;



//http://stackoverflow.com/questions/3802370/java-time-based-map-cache-with-expiring-keys -> könnte man auch ersetzen
//so ist es aber simpler - aber auch nicht voll implementiert. z.b. würde man annehmen, dass man auch andere methoden verwenden kann...
/**
 * nach der angegebenen zeit verschwinden einträge aus der map.
 * konfigurierbar, ob lesende zugriffe den zeitstempel updaten oder nicht.
 * ACHTUNG: nur {@link ExpiringMap#put(Object, Object)}, {@link ExpiringMap#remove(Object)}, {@link ExpiringMap#get(Object)} 
 * und {@link ExpiringMap#putIfAbsent(Object, Object)} verwenden. rest ist nicht implementiert.
 *
 */
public class ExpiringMap<K, V> extends ConcurrentHashMap<K, V> {

  private static final long serialVersionUID = -1024825109568473349L;

  private final long validityDurationMS;

  private final SortedMap<Long, Set<K>> timeKeys = new TreeMap<Long, Set<K>>();
  private final Map<K, Long> keyTimes = new HashMap<K, Long>(); //letzter zugriff pro key
  private final boolean readUpdatesTime;


  public ExpiringMap(long validityDuration, TimeUnit unit, boolean readUpdatesTime) {
    super();
    validityDurationMS = unit.toMillis(validityDuration);
    this.readUpdatesTime = readUpdatesTime;
  }


  public ExpiringMap(long validityDuration, TimeUnit unit) {
    this(validityDuration, unit, false);
  }


  @Override
  public V put(K key, V value) {
    V v = super.put(key, value);
    updateKey(key);
    check();
    return v;
  }


  private void updateKey(K key) {
    long t = System.currentTimeMillis();
    synchronized (timeKeys) {
      Set<K> list = timeKeys.get(t);
      if (list == null) {
        list = new HashSet<K>();
        timeKeys.put(t, list);
      }
      list.add(key);
      Long oldTime = keyTimes.put(key, t);
      if (oldTime != null) {
        Set<K> keys = timeKeys.get(oldTime);
        if (keys != null) {
          keys.remove(key);
          if (keys.isEmpty()) {
            timeKeys.remove(oldTime);
          }
        }
      }
    }
  }


  @Override
  public V putIfAbsent(K key, V value) {
    V v = super.putIfAbsent(key, value);
    if (v == null || readUpdatesTime) {
      updateKey(key);
    }
    check();
    return v;
  }


  private void check() {
    synchronized (timeKeys) {
      while (!timeKeys.isEmpty()) {
        Long t = timeKeys.firstKey();
        if (System.currentTimeMillis() - t > validityDurationMS) {
          Set<K> removed = timeKeys.remove(t);
          for (K k : removed) {
            Long lastChange = keyTimes.get(k);
            if (lastChange != null && lastChange <= t) { //falls null, ist der key bereits entfernt. falls > t, gibt es einen neueren eintrag in timeKeys und deshalb wird der key noch nicht gelöscht
              remove(k, false); //keine rekursion!
            }
          }
        } else {
          break;
        }
      }
    }
  }


  @Override
  public V get(Object key) {
    V v = super.get(key);
    if (readUpdatesTime) {
      updateKey((K) key);
    }
    check();
    return v;
  }

  /**
   * nicht überschreiben, statt dessen die variante mit boolean check
   */
  @Override
  public final V remove(Object key) {
    return remove(key, true);
  }


  protected V remove(Object key, boolean check) {
    V v = super.remove(key);
    synchronized (timeKeys) {
      Long t = keyTimes.remove(key);
      if (t != null) {
        Set<K> keys = timeKeys.get(t);
        if (keys != null) {
          keys.remove(key);
          if (keys.isEmpty()) {
            timeKeys.remove(t);
          }
        }
      }
    }
    if (check) {
      check();
    }
    return v;
  }


  public void clear() {
    super.clear();
    synchronized (timeKeys) {
      keyTimes.clear();
      timeKeys.clear();
    }
  }


}
