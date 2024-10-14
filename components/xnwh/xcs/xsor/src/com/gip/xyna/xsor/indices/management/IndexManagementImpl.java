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
package com.gip.xyna.xsor.indices.management;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.xsor.indices.CompositeIndex;
import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.XSORPayloadPrimaryKeyIndex;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchRequest;
import com.gip.xyna.xsor.protocol.XSORPayload;


public class IndexManagementImpl implements IndexManagement {
  
  private static final Logger logger = Logger.getLogger(IndexManagement.class);
  
  private final IndexFactory indexFactory;
  protected final Map<String, Map<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>, CompositeIndex>> compositeIndicesPerTable = 
                  new HashMap<String, Map<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>, CompositeIndex>>();
  protected final Map<String, XSORPayloadPrimaryKeyIndex> primaryKeyIndexPerTable = new HashMap<String, XSORPayloadPrimaryKeyIndex>();

  
  public IndexManagementImpl(IndexFactory indexFactory) {
    this.indexFactory = indexFactory;
  }
  

  public synchronized void createIndex(IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion> indexDefinition) {
    String tablename = indexDefinition.getTableName();
    Map<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>, CompositeIndex> indices = compositeIndicesPerTable.get(tablename);
    if (indices == null) {
      indices = new HashMap<IndexDefinition<?,? extends IndexKey,? extends IndexSearchCriterion>, CompositeIndex>();
    }
    if (indices.containsKey(indexDefinition)) {
      return;
    } else {
      CompositeIndex index = indexFactory.createIndex(indexDefinition);
      if (index != null) {
        indices.put(indexDefinition, index);
        compositeIndicesPerTable.put(tablename, indices);
      }
    }
  }
  
  
  public void createXSORPayloadPrimaryKeyIndex(String tableName) {
    XSORPayloadPrimaryKeyIndex xsorPayloadPrimaryKeyIndex = indexFactory.createXSORPayloadPrimaryKeyIndex();
    primaryKeyIndexPerTable.put(tableName, xsorPayloadPrimaryKeyIndex);
    compositeIndicesPerTable.put(tableName, new HashMap<IndexDefinition<?,? extends IndexKey,? extends IndexSearchCriterion>, CompositeIndex>());
  }


  public XSORPayloadPrimaryKeyIndex getXSORPayloadPrimaryKeyIndex(String tableName) {
    return primaryKeyIndexPerTable.get(tableName);
  }

  public boolean put(XSORPayload obj, int internalId) {
    String tablename = obj.getTableName();
    Collection<CompositeIndex> indices = compositeIndicesPerTable.get(tablename).values();
    boolean allSucceeded = true;
    if (indices != null) {
      for (CompositeIndex compositeIndex : indices) {
        if (!compositeIndex.put(obj, internalId)) {
          allSucceeded = false;
        }
      }
    }
    return allSucceeded;
  }


  public void delete(XSORPayload obj, int internalId) {
    String tablename = obj.getTableName();
    Collection<CompositeIndex> indices = compositeIndicesPerTable.get(tablename).values();
    if (indices != null) {
      for (CompositeIndex compositeIndex : indices) {
        compositeIndex.delete(obj, internalId);
      }
    }
  }


  public void update(XSORPayload outdatedEntry, XSORPayload updatedEntry, int outdatedId, int updatedId) {
    String tablename;
    if (outdatedEntry == null && updatedEntry == null) {
      throw new RuntimeException("dont send double nulls when updating indices");
    }
    if (outdatedEntry != null) {
      tablename = outdatedEntry.getTableName();
    } else {
      tablename = updatedEntry.getTableName();
    }
    Collection<CompositeIndex> indices = compositeIndicesPerTable.get(tablename).values();
    if (indices != null) {
      for (CompositeIndex compositeIndex : indices) {
        boolean success = false;
        try {
          compositeIndex.update(outdatedEntry, updatedEntry, outdatedId, updatedId);
          success = true;
        } finally {
          if (!success) {
            String pk;
            if (outdatedEntry != null) {
              pk = String.valueOf(outdatedEntry.getPrimaryKey());
            } else {
              pk = String.valueOf(updatedEntry.getPrimaryKey());
            }
            logger.warn("indices in order : " + indices + " problem in "
                            + Arrays.toString(compositeIndex.getIndexDefintion().getIndexedColumnNamesInOrder()) + ", " + pk,
                        new Exception());
          }
        }
      }
    }
  }


  public void clear(String tablename) {
    Collection<CompositeIndex> indices = compositeIndicesPerTable.get(tablename).values();
    if (indices != null) {
      for (CompositeIndex compositeIndex : indices) {
        compositeIndex.clear();
      }
    }
    getXSORPayloadPrimaryKeyIndex(tablename).clear();
  }
  
  
  public IndexSearchResult search(SearchRequest searchRequest, SearchParameter searchParameter, int maxRows) {
    IndexSearchResult result = null;
    List<? extends SearchCriterion> criteria = searchRequest.getSearchCriterion();
    Map<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>, CompositeIndex> indexMapForRequest = compositeIndicesPerTable.get(searchRequest.getTablename());
    if (criteria == null || criteria.size() == 0) { // no criteria = fullTableScan
      XSORPayloadPrimaryKeyIndex index = getXSORPayloadPrimaryKeyIndex(searchRequest.getTablename());
      if (logger.isDebugEnabled()) {
        logger.debug("No SearchCriteria given for request on " + searchRequest.getTablename() + " returning all contained ids.");
      }
      return IndexSearchResult.createIndexSearchResultAsFullTableScan(index);
    } else {
      for (SearchCriterion criterion : searchRequest.getSearchCriterion()) {
        IndexSearchResult searchRequestResult = null;
        Collection<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> appropriateIndexDefinitions = criterion.getAppropriateIndexDefinitions();
        if (criterion.containsPrimaryKeyInEqualComparision()) {
          ColumnCriterion columnCriterion = criterion.getPrimaryKeyInEqualComparisionCriterion();
          XSORPayloadPrimaryKeyIndex index = getXSORPayloadPrimaryKeyIndex(searchRequest.getTablename());
          int pkResult = index.getUniqueValueForKey(searchParameter.getSearchValue(columnCriterion.getMappingToSearchParameter()).getValue());
          if (pkResult < 0) {
            if (logger.isDebugEnabled()) {
              logger.debug("SearchRequest for " + searchRequest.getTablename() + " contained primaryKey in equals and a corresponding object was not contained.");
            }
            return IndexSearchResult.EMPTY_INDEX_SEARCH_RESULT;
          } else {
            if (logger.isDebugEnabled()) {
              logger.debug("SearchRequest for " + searchRequest.getTablename() +
                           " contained primaryKey in equals and a corresponding object was contained, returning id: " + pkResult);
            }
            return IndexSearchResult.createIndexSearchResultFromInteger(pkResult);
          }
        } else if (appropriateIndexDefinitions == null || appropriateIndexDefinitions.size() == 0) {
          XSORPayloadPrimaryKeyIndex pkIndex = getXSORPayloadPrimaryKeyIndex(searchRequest.getTablename());
          return IndexSearchResult.createIndexSearchResultAsFullTableScan(pkIndex);
        } else {
          IndexSearchResult indexSearchCriterionResult = null;
          for (IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion> indexDefinition : appropriateIndexDefinitions) {
            CompositeIndex index = indexMapForRequest.get(indexDefinition);
            if (index == null) {
              // there is no index covering this request => fullTableScan
              XSORPayloadPrimaryKeyIndex pkIndex = getXSORPayloadPrimaryKeyIndex(searchRequest.getTablename());
              if (logger.isDebugEnabled()) {
                logger.debug("An appropriate index could not be found for " + searchRequest.getTablename() + " returning all contained ids.");
              }
              return IndexSearchResult.createIndexSearchResultAsFullTableScan(pkIndex);
            } else {
              indexSearchCriterionResult = index.search(criterion, searchParameter, maxRows).intersect(indexSearchCriterionResult);
              if (logger.isDebugEnabled()) {
                logger.debug("mostAppropriateIndex.search for maxResults " + maxRows + " returned " + indexSearchCriterionResult.getInternalIds().length + " search exhausted: " + indexSearchCriterionResult.isExhaustiveSearch());
              }
            }
          }
          if (indexSearchCriterionResult == null) {
            searchRequestResult = IndexSearchResult.EMPTY_INDEX_SEARCH_RESULT;
          } else {
            searchRequestResult = indexSearchCriterionResult;
          }
        }
        result = searchRequestResult.union(result);
      }
    }
    return result;
  }


  public void remove(String tableName) {
    clear(tableName);
    compositeIndicesPerTable.remove(tableName);
    primaryKeyIndexPerTable.remove(tableName);
  }




}
