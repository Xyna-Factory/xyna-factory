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
package com.gip.xyna;

import java.util.HashMap;
import java.util.Map;

public class BijectiveMap<K, V> extends HashMap<K, V> {

  private static final long serialVersionUID = 6379176652416679479L;
  private final Map<V, K> inverse = new HashMap<>();


  @Override
  public V put(K key, V val) {
    V oldVal = super.put(key, val);
    inverse.put(val, key);
    return oldVal;
  }


  @Override
  public V remove(Object key) {
    V val = super.remove(key);
    inverse.remove(val);
    return val;
  }


  public K getInverse(V val) {
    return inverse.get(val);
  }
}
