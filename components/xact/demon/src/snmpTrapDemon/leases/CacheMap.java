/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package snmpTrapDemon.leases;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * CacheMap (LRU-Cache) cachet eine bestimmte, konfiguriere Anzahl an Daten.
 *
 * Die Cachegröße kann über setCacheSize() oder den entrsprechenden 
 * Konstruktor gesetzt werden.
 * Falls über put ein weiterer Eintrag in die CacheMap vorgenommen 
 * wird und die Cache schon gefüllt ist, wird der am längsten nicht 
 * abgerufene Eintrag aus der Map entfernt. 
 *
 */
public class CacheMap<K,V> extends AbstractMap<K,V> {
  
  private static class UsedValue<V> {
    V value;
    long lastUsed;

    public UsedValue(V value) {
      this.value = value;
      lastUsed = System.currentTimeMillis();
    }

    public V getValue() {
      lastUsed = System.currentTimeMillis();
      return value;
      //Für Debugzwecke ganz interessant
      //String ret = (String)value;
      //ret += "_"+lastUsed;
      //lastUsed = System.currentTimeMillis();
      //return (V)ret;
    }

   public long getLastUsed() {
      return lastUsed;
    }
  }
  
  private HashMap<K,UsedValue<V>> dataMap = new HashMap<K,UsedValue<V>>();
  private EntrySet entrySet = new EntrySet();
  private int cacheSize;
 
  public CacheMap() {
    cacheSize = 10;
  }
  
  public CacheMap(int cacheSize) {
    this.cacheSize = cacheSize;
  }
  
  private class EntrySet extends AbstractSet<java.util.Map.Entry<K, V>> {

    @Override
    public Iterator<java.util.Map.Entry<K, V>> iterator() {
      return new Iterator<java.util.Map.Entry<K, V>>() {
        private Iterator<Entry<K,UsedValue<V>>> i = CacheMap.this.dataMap.entrySet().iterator();

        public boolean hasNext() {
          return i.hasNext();
        }

        public java.util.Map.Entry<K, V> next() {
          final Entry<K,UsedValue<V>> baseEntry; { baseEntry = i.next(); }
          return new java.util.Map.Entry<K, V>() {
            public K getKey() {
              return baseEntry.getKey();
            }
            public V getValue() {
              return baseEntry.getValue().getValue();
            }
            public V setValue(V value) {
              baseEntry.setValue( new UsedValue<V>(value) ); 
              return null;
            }};
        }

        public void remove() {
          i.remove();
        }
      };
    }

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#size()
     */
    @Override
    public int size() {
      return CacheMap.this.dataMap.size();
    }
    
  }
  
  
  
  /* (non-Javadoc)
   * @see java.util.AbstractMap#entrySet()
   */
  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    return entrySet;
  }
    
  /* (non-Javadoc)
   * @see java.util.AbstractMap#put(java.lang.Object, java.lang.Object)
   */
  @Override
  public V put(K key, V value) {   
    UsedValue<V> uv = dataMap.put( key, new UsedValue<V>(value) );
    if( uv != null ) {
      return uv.getValue(); //nur Update eines älteren Eintrags
    }
    //neuer Eintrag, daher Cache-Größe überwachen
    removeOldest();
    return null;
  }
  
  /**
   * 
   */
  private void removeOldest() {
    while( dataMap.size() > cacheSize ) {
      long lastUsed = System.currentTimeMillis();
      K oldest = null;
      for( Entry<K,UsedValue<V>> e : dataMap.entrySet() ) {
        if( e.getValue().getLastUsed() < lastUsed ) {
          lastUsed = e.getValue().getLastUsed();
          oldest = e.getKey();
        }
      }
      dataMap.remove(oldest);
    }
  }

  /**
   * @param cacheSize the cacheSize to set
   */
  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;
  }

  /**
   * @return the cacheSize
   */
  public int getCacheSize() {
    return cacheSize;
  }
  
}
