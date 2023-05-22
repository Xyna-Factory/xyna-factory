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
package com.gip.xyna.xsor.indices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.xsor.common.IntegrityAssertion;
import com.gip.xyna.xsor.indices.definitions.HashIndexDefinition;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.management.IndexSearchResult;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.tools.AtomicitySupport;
import com.gip.xyna.xsor.indices.tools.IntValueWrapper;
import com.gip.xyna.xsor.indices.tools.SingleIntValueWrapper;
import com.gip.xyna.xsor.protocol.XSORPayload;



public class HashIndex implements CompositeIndex {

  private final ConcurrentMap<Integer, IntValueWrapper> map;
  private final HashIndexDefinition definition;
  private final AtomicitySupport atomicitySupport;
  private final int optimisticRetries;
  
  public HashIndex(HashIndexDefinition definition, AtomicitySupport atomicitySupport, int initialCapacity, float loadFactor, int concurrencyLevel, int optimisticRetries) {
    map = new ConcurrentHashMap<Integer, IntValueWrapper>(initialCapacity, loadFactor, concurrencyLevel);
    this.definition = definition;
    this.atomicitySupport = atomicitySupport;
    this.optimisticRetries = optimisticRetries;
  }
  
  
  public boolean put(XSORPayload obj, int internalId) {
    Integer key = definition.createIndexKey(obj).hashCode();
    boolean success = false;
    int retries = 0;
    while (!success && retries != optimisticRetries) {
      atomicitySupport.protectAgainstNonAtomicOperations();
      try {
        IntValueWrapper intValues = map.get(key);
        if (intValues == null) {
          intValues = new SingleIntValueWrapper(internalId);
          if (map.putIfAbsent(key, intValues) == null) {
            success = true;
          }
        } else {
          IntValueWrapper update = intValues.addValue(internalId);
          if (map.replace(key, intValues, update)) {
            success = true;
          }
        }
        if (optimisticRetries > 0) {
          retries++;
        }
      } finally {
        atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
        if (!success) {
          Thread.yield();
        }
      }
    }
    // FIXME checked?
    if (!success) {
      throw new RuntimeException("OptimisticLocking did not succeed");
    }
    return success;
  }

  
  public IndexSearchResult search(SearchCriterion searchCriterion, SearchParameter parameter, int maxResults) {
    Integer key = definition.createIndexSearchCriterion(searchCriterion, parameter).getSearchHash();
    atomicitySupport.protectAgainstNonAtomicOperations();
    try {
      IntValueWrapper intValues = map.get(key);
      if (intValues == null) {
        return IndexSearchResult.EMPTY_INDEX_SEARCH_RESULT;
      } else {
        return IndexSearchResult.createIndexSearchResultFromIntValueWrapper(intValues);
      }
    } finally {
      atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
    }
  }

  
  public void delete(XSORPayload obj, int internalId) { 
    Integer key = definition.createIndexKey(obj).hashCode();
    boolean success = false;
    int retries = 0;
    while (!success && retries != optimisticRetries) {
      atomicitySupport.protectAgainstNonAtomicOperations();
      try {
        @IntegrityAssertion
        IntValueWrapper intValues = map.get(key);
        assert intValues != null : "no values found for deletion when trying to delete " + internalId;
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
        if (optimisticRetries > 0) {
          retries++;
        }
      } finally {
        atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
        if (!success) {
          Thread.yield();
        }
      }
    }
    // FIXME checked?
    if (!success) {
      throw new RuntimeException("OptimisticLocking did not succeed");
    }
  }

  
  public void update(XSORPayload outdatedEntry, XSORPayload updatedEntry, int outdatedId, int updatedId) {
    if (outdatedEntry == null) {
      put(updatedEntry, updatedId);
      // TODO treat result?
    } else if (updatedEntry == null) {
      delete(outdatedEntry, outdatedId);
    } else {
      Integer outdatedKey = definition.createIndexKey(outdatedEntry).hashCode();
      Integer updatedKey = definition.createIndexKey(updatedEntry).hashCode();
      if (outdatedKey.hashCode() == updatedKey.hashCode()) {
        boolean success = false;
        int retries = 0;
        while (!success && retries != optimisticRetries) {
          if (outdatedId == updatedId) {
            success = true;
          } else {
            atomicitySupport.protectAgainstNonAtomicOperations();
            try {
              @IntegrityAssertion
              IntValueWrapper outdatedValues = map.get(outdatedKey);
              assert (outdatedValues != null) : "No values found for key when trying to update " + outdatedId + " to " + updatedId ;
              IntValueWrapper updatedValues = outdatedValues.replaceValue(outdatedId, updatedId);
              if (map.replace(outdatedKey, outdatedValues, updatedValues)) {
                success = true;
              }
              if (optimisticRetries > 0) {
                retries++;
              }
            } finally {
              atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
              if (!success) {
                Thread.yield();
              }
            }
          }
        }
        // FIXME checked?
        if (!success) {
          throw new RuntimeException("OptimisticLocking did not succeed");
        }
      } else {
        atomicitySupport.initiateNonAtomicOperation();
        try {
          // because we hold the write lock we'll have less checking to do as in other cases
          @IntegrityAssertion
          IntValueWrapper outdatedValuesOfOutdatedKey = map.get(outdatedKey);
          assert (outdatedValuesOfOutdatedKey != null) : "No values found for key when trying to update " + outdatedId + " to " + updatedId ;
          IntValueWrapper updateValuesOutdatedKey = outdatedValuesOfOutdatedKey.removeValue(outdatedId);
          if (updateValuesOutdatedKey == null) {
            map.remove(outdatedKey);
          } else {
            map.replace(outdatedKey, updateValuesOutdatedKey);
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
    Integer key = definition.createIndexKey(obj).hashCode();
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
