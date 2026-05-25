/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xfractwfe.generation.xmom;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class IdMapping {

  private Map<String, IdValue> map = new HashMap<>();
  
  
  public IdValue getOrCreateIdValue(String value) {
    IdValue id = map.get(value);
    if (id == null) {
      id = new IdValue(value);
      map.put(value, id);
    } else {
      id.incRefCount();
    }
    return id;
  }
  
  
  public void renumber() {
    IdRenumbering idr = new IdRenumbering();
    for (String val : map.keySet()) {
      idr.add(val);
    }
    Map<String, Integer> renumbered = idr.renumber();
    Map<String, IdValue> newMap = new HashMap<>();
    for (Entry<String, IdValue> entry : map.entrySet()) {
      Integer newVal = renumbered.get(entry.getKey());
      if (newVal == null) {
        throw new RuntimeException("Error in id renumbering: Missing id " + entry.getKey());
      }
      String str = String.valueOf(newVal);
      entry.getValue().changeValue(str);
      newMap.put(str, entry.getValue());
    }
    map = newMap;
  }
  
}
