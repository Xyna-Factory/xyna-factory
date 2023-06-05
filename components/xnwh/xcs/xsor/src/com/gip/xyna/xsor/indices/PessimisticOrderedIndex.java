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
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.gip.xyna.xsor.common.IntegrityAssertion;
import com.gip.xyna.xsor.common.XSORUtil;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.definitions.OrderedIndexDefinition;
import com.gip.xyna.xsor.indices.management.IndexSearchResult;
import com.gip.xyna.xsor.indices.search.OrderedIndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.tools.IntValueWrapper;
import com.gip.xyna.xsor.indices.tools.SingleIntValueWrapper;
import com.gip.xyna.xsor.protocol.XSORPayload;


public class PessimisticOrderedIndex implements CompositeIndex {
  
  public static final float MAX_COVERAGE_FOR_PARTIALCOVERAGE = 0.95f;

  private final NavigableMap<OrderedIndexKey, IntValueWrapper> map;
  private final OrderedIndexDefinition definition;
  
  public PessimisticOrderedIndex(OrderedIndexDefinition definition) {
    map = new TreeMap<OrderedIndexKey, IntValueWrapper>();
    this.definition = definition;
  }
  
  private ReadWriteLock lock = new ReentrantReadWriteLock();
  
  public boolean put(XSORPayload obj, int internalId) {
    OrderedIndexKey key = definition.createIndexKey(obj);
    lock.writeLock().lock();
    try {
      IntValueWrapper intValues = map.get(key);
      if (intValues == null) {
        map.put(key, new SingleIntValueWrapper(internalId));
      } else {
        IntValueWrapper update = intValues.addValue(internalId);
        map.put(key, update);
      }
    } finally {
      lock.writeLock().unlock();  
    }
    return true;
  }

  public IndexSearchResult search(SearchCriterion searchCriterion, SearchParameter parameter, int maxResults) {
    OrderedIndexSearchCriterion criterion = definition.createIndexSearchCriterion(searchCriterion, parameter);
    OrderedIndexKey rangeStart = criterion.getRangeStart();
    OrderedIndexKey rangeStop = criterion.getRangeStop();
    lock.readLock().lock();
    try {
      SortedMap<OrderedIndexKey, IntValueWrapper> subMap;

      if (rangeStop == null) {
        rangeStop = map.lastKey();
      }
      if (rangeStart == null) {
        rangeStart = map.firstKey();
      }
      int rangeComparison = rangeStart.compareTo(rangeStop);
      if (rangeComparison == 0 &&
          (rangeStart.isInclusive() || rangeStop.isInclusive())) {
        OrderedIndexKey keyForLookup = null;
        if (rangeStart.isPartiallyOpen() && rangeStop.isPartiallyOpen()) { 
          // if both keys are equal but partially open a subMap might still return more then a single value 
          subMap = map.subMap(rangeStart, rangeStart.isInclusive(), rangeStop, rangeStop.isInclusive());
        } else {
          if (rangeStart.isPartiallyOpen()) { // use the closed key
            keyForLookup = rangeStop;
          } else {
            keyForLookup = rangeStart;
          }
          IntValueWrapper wrapper = map.get(keyForLookup);
          if (wrapper == null) {
            return IndexSearchResult.EMPTY_INDEX_SEARCH_RESULT;
          } else {
            return IndexSearchResult.createIndexSearchResultFromIntValueWrapper(wrapper);
          }
        }
      } else if (rangeComparison > 0) {
        return IndexSearchResult.EMPTY_INDEX_SEARCH_RESULT;
      } else {
        subMap = map.subMap(rangeStart, rangeStart.isInclusive(), rangeStop, rangeStop.isInclusive());
      }
      
      if (maxResults < 0) {
        return XSORUtil.intValueWrapperValuesToIndexSearchResult(subMap.values());
      } else {
        return XSORUtil.intValueWrapperValuesToIndexSearchResult(subMap.values(), maxResults);
      }
      
    } finally {
      lock.readLock().unlock();
    }
  }

  public void delete(XSORPayload obj, int internalId) {
    OrderedIndexKey key = definition.createIndexKey(obj);
    lock.writeLock().lock();
    try {
      IntValueWrapper intValues = map.get(key);
      if (intValues == null) {
        return;
      } else {
        IntValueWrapper update = intValues.removeValue(internalId);
        if (update == null) {
          map.remove(key);
        } else {
          map.put(key, update);
        }
      }
    } finally {
      lock.writeLock().unlock();  
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
      lock.writeLock().lock();
      try {
        @IntegrityAssertion
        IntValueWrapper intValues = map.get(outdatedKey);
        assert intValues != null : "no outdated values could be found when trying to update " +outdatedId + " to " + updatedId;
        if (outdatedKey.equals(updatedKey)) {
          IntValueWrapper updatedValues = intValues.replaceValue(outdatedId, updatedId);
          map.put(updatedKey, updatedValues);
        } else {
          IntValueWrapper lessIntValues = intValues.removeValue(outdatedId);
          if (lessIntValues == null) {
            map.remove(outdatedKey);
          } else {
            map.put(outdatedKey, lessIntValues);
          }
          IntValueWrapper otherIntValues = map.get(updatedKey);
          if (otherIntValues == null) {
            map.put(updatedKey, new SingleIntValueWrapper(updatedId));
          } else {
            IntValueWrapper moreValues = otherIntValues.addValue(updatedId);
            map.put(updatedKey, moreValues);
          }
        }
      } finally {
        lock.writeLock().unlock();  
      }
    }
  }

  public IndexDefinition getIndexDefintion() {
    return definition;
  }

  
  public int[] values() {
    lock.readLock().lock();
    try {
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
    } finally {
      lock.readLock().unlock();
    }
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
