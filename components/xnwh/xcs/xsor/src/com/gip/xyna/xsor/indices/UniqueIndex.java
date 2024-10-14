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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.xsor.common.IntegrityAssertion;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.definitions.UniqueIndexDefinition;
import com.gip.xyna.xsor.indices.management.IndexSearchResult;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.tools.AtomicitySupport;
import com.gip.xyna.xsor.protocol.XSORPayload;



public class UniqueIndex implements CompositeIndex, UniqueKeyValueMappingIndex<XSORPayload> /* too bad nobody cares atm */ {

  private final ConcurrentMap<UniqueIndexKey, Integer> map;
  private final UniqueIndexDefinition definition;
  private final AtomicitySupport atomicitySupport;


  public UniqueIndex(UniqueIndexDefinition definition, AtomicitySupport atomicitySupport, int initialCapacity, float loadFactor, int concurrencyLevel) {
    this.map = new ConcurrentHashMap<UniqueIndexKey, Integer>(initialCapacity, loadFactor, concurrencyLevel);
    this.definition = definition;
    this.atomicitySupport = atomicitySupport;
  }


  public boolean put(XSORPayload obj, int internalId) {
    atomicitySupport.protectAgainstNonAtomicOperations();
    try {
      UniqueIndexKey key = definition.createIndexKey(obj);
      return map.putIfAbsent(key, internalId) == null;
    } finally {
      atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
    }
  }


  public IndexSearchResult search(SearchCriterion searchCriterion, SearchParameter parameter, int maxRows) {
    atomicitySupport.protectAgainstNonAtomicOperations();
    try {
      UniqueIndexKey key = definition.createIndexSearchCriterion(searchCriterion, parameter).getSearchKeyUniqueHash();
      Integer result = map.get(key);
      if (result == null) {
        return IndexSearchResult.EMPTY_INDEX_SEARCH_RESULT;
      } else {
        return IndexSearchResult.createIndexSearchResultFromInteger(result);
      }
    } finally {
      atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
    }
  }


  public void delete(XSORPayload obj, int internalId) {
    atomicitySupport.protectAgainstNonAtomicOperations();
    try {
      UniqueIndexKey key = definition.createIndexKey(obj);
      @IntegrityAssertion
      boolean success = map.remove(key, internalId);
      assert success : "delete on uniqueIndex for id " + internalId + " did not succedd";
    } finally {
      atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
    }
  }


  public void update(XSORPayload outdatedEntry, XSORPayload updatedEntry, int outdatedId, int updatedId) {
    if (outdatedEntry == null) {
      put(updatedEntry, updatedId);
      // TODO treat result?
    } else if (updatedEntry == null) {
      delete(outdatedEntry, outdatedId);
    } else {
      UniqueIndexKey outdatedKey = definition.createIndexKey(outdatedEntry);
      UniqueIndexKey updatedKey = definition.createIndexKey(updatedEntry);
      if (outdatedKey.equals(updatedKey)) {
        atomicitySupport.protectAgainstNonAtomicOperations();
        try {
          map.replace(outdatedKey, outdatedId, updatedId);
        } finally {
          atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
        }
      } else {
        atomicitySupport.initiateNonAtomicOperation();
        try {
          map.putIfAbsent(updatedKey, updatedId); // TODO treat failure?
          map.remove(outdatedKey, outdatedId);
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
    atomicitySupport.protectAgainstNonAtomicOperations();
    try {
      List<Integer> values = new ArrayList<Integer>(map.values());
      int[] result = new int[values.size()];
      for (int i = 0; i < result.length; i++) {
        result[i] = values.get(i).intValue();
      }
      return result;
    } finally {
      atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
    }
  }


  public int[] get(XSORPayload obj) {
    int retrievedId = getUniqueValueForKey(obj);
    if (retrievedId == UniqueKeyValueMappingIndex.NO_VALUE) {
      return new int[0];
    } else {
      return new int[] {retrievedId};
    }
  }
  
  
  public int getUniqueValueForKey(XSORPayload obj) {
    atomicitySupport.protectAgainstNonAtomicOperations();
    try {
      Integer result = map.get(definition.createIndexKey(obj));
      if (result == null) {
        return UniqueKeyValueMappingIndex.NO_VALUE;
      } else {
        return result.intValue();
      }
    } finally {
      atomicitySupport.releaseProtectionAgainstNonAtomicOperations();
    }
  }


  public void clear() {
    map.clear();
  }


  
}
