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

import java.util.LinkedHashMap;


/**
 * LruCache ist ein Cache, der eine vorgegebenen Anzahl von Cache-Eintr�gen 
 * aufbewahrt. Falls weitere Eintr�ge �ber put() eingetragen werden, werden die 
 * am l�ngsten nicht verwendeten (LRU) Eintr�ge entfernt. Als Verwendung z�hlt
 * dabei die Abfrage �ber get() und das Eintragen oder �ndern �ber put().
 * <br><br>
 * Achtung: dieser Cache ist nicht threadsafe, daher extern absichern!
 */
public class LruCache<K,V> extends LinkedHashMap<K, V> {
  private static final long serialVersionUID = 1L;
  
  private final int maxEntries;
  
  public LruCache(int maxEntries) {
    super(maxEntries + 1, 1.0f, true);
    this.maxEntries = maxEntries;
  }

  /* (non-Javadoc)
   * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
   */
  @Override
  protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
    return super.size() > maxEntries;
  }
    
}
