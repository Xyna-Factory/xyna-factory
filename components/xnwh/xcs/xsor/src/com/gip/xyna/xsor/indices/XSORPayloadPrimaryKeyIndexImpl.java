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
package com.gip.xyna.xsor.indices;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.debug.Debugger;
import com.gip.xyna.xsor.common.IntegrityAssertion;
import com.gip.xyna.xsor.common.XSORUtil;

import org.apache.log4j.Logger;



public class XSORPayloadPrimaryKeyIndexImpl implements XSORPayloadPrimaryKeyIndex {

  private final static Logger logger = Logger.getLogger(XSORPayloadPrimaryKeyIndex.class);
  private ConcurrentMap<XSORPayloadPrimaryKeyIndexKey, Integer> map;

  private Debugger debugger = Debugger.getInstance();
  
  public XSORPayloadPrimaryKeyIndexImpl(int initialCapacity, float loadFactor, int concurrencyLevel) {
    this.map = new ConcurrentHashMap<XSORPayloadPrimaryKeyIndexKey, Integer>(initialCapacity, loadFactor, concurrencyLevel);
  }
  
    

  public boolean put(Object obj, int internalId) {
    boolean success = map.putIfAbsent(new XSORPayloadPrimaryKeyIndexKey(obj), internalId) == null;
    if (logger.isTraceEnabled()) {
      logger.trace("putIfAbsent of " + internalId + " success: " + success);
    }
    return success;
  }
  
  private static String pkAsString(Object obj) {
    String os;
    if (obj instanceof byte[]) {
      os = Arrays.toString((byte[])obj);
    } else {
      os = String.valueOf(obj);
    }
    return os;
  }

  
  public void delete(final Object obj, int internalId) {
    debugger.trace(new Object() {
      public String toString() {
        return "deletePKIdx " + pkAsString(obj);
      }
    });
    @IntegrityAssertion
    boolean success = map.remove(new XSORPayloadPrimaryKeyIndexKey(obj), internalId);
    assert success :  "remove of " + internalId + " with key " + String.valueOf(obj) + " did not succeed";
  }


  public int[] values() {
    return XSORUtil.safeIntegerCollectionToArray(map.values());
  }

  
  public Set<Object> keySet() {
    Set<Object> keySet = new HashSet<Object>();
    for (XSORPayloadPrimaryKeyIndexKey xsorPayloadPrimaryKeySearchKey : map.keySet()) {
      keySet.add(xsorPayloadPrimaryKeySearchKey.getXSORPayloadPrimaryKey());
    }
    return keySet;
  }
  
  
  public int[] get(Object obj) {
    int retrievedId = getUniqueValueForKey(obj);
    if (retrievedId == UniqueKeyValueMappingIndex.NO_VALUE) {
      return new int[0];
    } else {
      return new int[] {retrievedId};
    }
  }
  
  
  public int getUniqueValueForKey(Object obj) {
    Integer value = map.get(new XSORPayloadPrimaryKeyIndexKey(obj));
    if (value == null) {
      return UniqueKeyValueMappingIndex.NO_VALUE;
    } else {
      return value.intValue();
    }
  }

  public void clear() {
    map.clear();
  }

  
  public int replace(Object key, int newValue, int oldValue) {
    @IntegrityAssertion
    Integer previousValue = map.put(new XSORPayloadPrimaryKeyIndexKey(key), newValue);
    if (previousValue == null) {
      return UniqueKeyValueMappingIndex.NO_VALUE;
    } else {
      assert (oldValue == previousValue || previousValue == newValue) :
        "replace in primaryKeyIndex replaced: " + previousValue + " with " + newValue + " for key: " + String.valueOf(key) + " but expected to replace " + oldValue;
      if (oldValue != previousValue && previousValue != newValue) {
        logger.warn("replace in primaryKeyIndex replaced: " + previousValue + " with " + newValue + " for key: " + String.valueOf(key) + " but expected to replace " + oldValue);
      }
      return previousValue.intValue();
    }
  }
  
  
  public void checkIntegrity(BufferedWriter w) {
    Map<Integer, Object> reverseMapping = new HashMap<Integer, Object>();
    Set<Entry<XSORPayloadPrimaryKeyIndexKey, Integer>> entries = map.entrySet();
    try {
      w.write("Checking ");
      w.write(String.valueOf(entries.size()));
      w.write(" index entries: ");
      w.newLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    for (Entry<XSORPayloadPrimaryKeyIndexKey, Integer> entry : entries) {
      if (reverseMapping.containsKey(entry.getValue())) {
        try {
          if (entry.getKey().getXSORPayloadPrimaryKey() instanceof byte[]) {
            w.write(Arrays.toString((byte[])entry.getKey().getXSORPayloadPrimaryKey()));
          } else {
            w.write(String.valueOf(entry.getKey().getXSORPayloadPrimaryKey()));
          }
          w.write(" and ");
          if (reverseMapping.get(entry.getValue()) instanceof byte[]) {
            w.write(Arrays.toString((byte[])reverseMapping.get(entry.getValue())));
          } else {
            w.write(String.valueOf(reverseMapping.get(entry.getValue())));
          }
          w.write(" both map to the same internal id: ");
          w.write(String.valueOf(entry.getValue()));
          w.newLine();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }


  public int getCurrentSize() {
    return map.size();
  }
  
}
