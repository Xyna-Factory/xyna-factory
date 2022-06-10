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
package com.gip.xyna.xsor.indices;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.gip.xyna.xsor.common.XSORUtil;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.definitions.OrderedIndexDefinition;
import com.gip.xyna.xsor.indices.management.IndexSearchResult;
import com.gip.xyna.xsor.indices.search.OrderedIndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.tools.AtomicitySupport;
import com.gip.xyna.xsor.indices.tools.IntValueWrapper;
import com.gip.xyna.xsor.indices.tools.SingleIntValueWrapper;
import com.gip.xyna.xsor.protocol.XSORPayload;

@Deprecated // not tested beyond initial tests after implementation
public class OptimisticOrderedIndex implements CompositeIndex {

  private final ConcurrentSkipListMap<OrderedIndexKey, IntValueWrapper> map;
  private final OrderedIndexDefinition definition;
  private final AtomicitySupport atomicitySupport;
  
  public OptimisticOrderedIndex(OrderedIndexDefinition definition, AtomicitySupport atomicitySupport) {
    map = new ConcurrentSkipListMap<OrderedIndexKey, IntValueWrapper>();
    this.definition = definition;
    this.atomicitySupport = atomicitySupport;
  }
  
   
  public boolean put(XSORPayload obj, int internalId) {
    OrderedIndexKey key = definition.createIndexKey(obj);
    boolean success = false;
    atomicitySupport.protectAgainstNonAtomicOperations();
    try {
      while (!success) { // TODO retry a few times?
        IntValueWrapper intValues = map.get(key);
        if (intValues == null) {
          intValues = new SingleIntValueWrapper(internalId);
          if (map.putIfAbsent(key, intValues) == null) {
            success = true;
          }
        } else {
          IntValueWrapper update = intValues.addValue(internalId);
          if (update == null) {
            success = true;
          } else if (map.replace(key, intValues, update)) {
            success = true;
          }
        }
      }
    } finally {
      atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
      if (!success) {
        Thread.yield();
      }
    }
    return success;
  }

  public IndexSearchResult search(SearchCriterion searchCriterion, SearchParameter parameter, int maxResults) {
    OrderedIndexSearchCriterion criterion = definition.createIndexSearchCriterion(searchCriterion, parameter);
    OrderedIndexKey rangeStart = criterion.getRangeStart();
    OrderedIndexKey rangeStop = criterion.getRangeStop();
    atomicitySupport.protectAgainstNonAtomicOperations();
    try {
      SortedMap<OrderedIndexKey, IntValueWrapper> subMap;

      if (rangeStop == null) {
        rangeStop = map.lastKey();
      }
      if (rangeStart == null) {
        rangeStart = map.firstKey();
      }
      subMap = map.subMap(rangeStart, rangeStop);
      
      return XSORUtil.intValueWrapperValuesToIndexSearchResult(subMap.values(), maxResults);
    } finally {
      atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
    }
  }

  public void delete(XSORPayload obj, int internalId) {
    OrderedIndexKey key = definition.createIndexKey(obj);
    boolean success = false;
    while (!success) {
      atomicitySupport.protectAgainstNonAtomicOperations();
      try {
        IntValueWrapper intValues = map.get(key);
        IntValueWrapper update = intValues.removeValue(internalId);
        if (update == null) {
          if (map.remove(key, intValues)) {
            success = true;
          }
        } else {
          if (map.replace(key, intValues, update)) {
            success = true;
          }
        }
      } finally {
        atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
        if (!success) {
          Thread.yield();
        }
      }
    }
  }

  public void update(XSORPayload outdatedEntry, XSORPayload updatedEntry, int outdatedId, int updatedId) {
    if (outdatedEntry == null) {
      put(updatedEntry, updatedId);
      // TODO treat result?
    } else if (updatedEntry == null) {
      delete(outdatedEntry, outdatedId);
    } else {
      OrderedIndexKey outdatedKey = definition.createIndexKey(outdatedEntry);
      OrderedIndexKey updatedKey = definition.createIndexKey(updatedEntry);
      if (outdatedKey.hashCode() == updatedKey.hashCode()) {
        boolean success = false;
        while (!success) {
          if (outdatedId == updatedId) {
            success = true;
          } else {
            atomicitySupport.protectAgainstNonAtomicOperations();
            try {
              IntValueWrapper outdatedValues = map.get(outdatedKey);
              if (outdatedValues == null) {
                throw new RuntimeException("No values found for key");
              }
              IntValueWrapper updatedValues = outdatedValues.replaceValue(outdatedId, updatedId);
              if (map.replace(outdatedKey, outdatedValues, updatedValues)) {
                success = true;
              }
            } finally {
              atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
              if (!success) {
                Thread.yield();
              }
            }
          }
        }
      } else {
        atomicitySupport.initiateNonAtomicOperation();
        try { 
          // because we hold the write lock we'll have less checking to do as in other cases
          IntValueWrapper outdatedValuesOfOutdatedKey = map.get(outdatedKey);
          if (outdatedValuesOfOutdatedKey == null) {
            throw new RuntimeException("No values found for key");
          }
          IntValueWrapper updateValuesOutdatedKey = outdatedValuesOfOutdatedKey.removeValue(outdatedId);
          if (updateValuesOutdatedKey == null) {
            map.remove(outdatedKey);
          }
          IntValueWrapper outdatedValuesOfUpdatedKey = map.get(updatedKey);
          if (outdatedValuesOfUpdatedKey == null) {
            map.put(updatedKey, new SingleIntValueWrapper(updatedId));
          } else {
            IntValueWrapper updatedValuesOfUpdatedKey = outdatedValuesOfUpdatedKey.addValue(updatedId);
            if (updatedValuesOfUpdatedKey != null) {
              map.replace(updatedKey, updatedValuesOfUpdatedKey);
            }
          }
        } finally {
          atomicitySupport.finishNonAtomicOperation();
        }
      }
    }
  }
  
  
  public IndexDefinition getIndexDefintion() {
    return definition;
  }

  
  public int[] values() {
    List<Integer> values = new ArrayList<Integer>();
    for (IntValueWrapper value : map.values()) {
      for (int i : value.getValues()) {
        values.add(i);
      }
    }
    int[] result = new int[values.size()];
    for (int i=0; i<result.length; i++) {
      result[i] = values.get(i).intValue();
    }
    return result;
  }

  public int[] get(XSORPayload obj) {
    OrderedIndexKey key = definition.createIndexKey(obj);
    IntValueWrapper values = map.get(key);
    if (values == null) {
      return new int[0];
    } else {
      return values.getValues();
    }
  }


  public void clear() {
    map.clear();
  }

}
