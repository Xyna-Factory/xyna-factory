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
package com.gip.xyna.utils.collections.maps;



import java.util.concurrent.ConcurrentHashMap;



public class ConcurrentCounterMap<T> {

  private final ConcurrentHashMap<T, Integer> map = new ConcurrentHashMap<>();


  /**
   * erhöht wert um 1 und gibt erhöhten wert zurück
   */
  public int increment(T key) {
    while (true) {
      Integer val = map.get(key);
      if (val == null) {
        if (null == map.putIfAbsent(key, 1)) {
          return 1;
        } //else retry
      } else if (map.replace(key, val, val + 1)) {
        return val + 1;
      } //else retry
    }
  }


  public int decrement(T key) {
    while (true) {
      Integer val = map.get(key);
      if (val == null) {
        throw new RuntimeException();
      }
      if (val == 1) {
        if (map.remove(key, val)) {
          return 0;
        } else {
          continue;
        }
      } else if (map.replace(key, val, val - 1)) {
        return val - 1;
      } //else retry
    }
  }


  public int get(T key) {
    Integer val = map.get(key);
    if (val == null) {
      return 0;
    }
    return val;
  }


}
