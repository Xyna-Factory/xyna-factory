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
package com.gip.xyna.persistence.xsor.indices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import com.gip.xyna.xsor.indices.OrderedIndexKey;
import com.gip.xyna.xsor.indices.definitions.ColumnFunction;
import com.gip.xyna.xsor.indices.definitions.IndexedColumnDefinition;
import com.gip.xyna.xsor.indices.definitions.OrderedIndexDefinition;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.OrderedIndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchValue;
import com.gip.xyna.xsor.indices.searchvaluekeys.SearchValueBasedOrderedIndexKey;
import com.gip.xyna.xsor.indices.searchvaluekeys.SearchValueBasedOrderedIndexKey.KeyType;
import com.gip.xyna.xnwh.persistence.Storable;


public class StorableBasedOrderedIndexDefinition<E extends Storable>  extends OrderedIndexDefinition<E> {

  
  public StorableBasedOrderedIndexDefinition(String tableName, String[] indexedColumns) {
    super(tableName, indexedColumns);
  }
  

  @Override
  public OrderedIndexKey createIndexKey(E e) {
    return new SearchValueBasedOrderedIndexKey(StorableBasedIndexHelper.createSearchValuesForColumnsFromStorable(getIndexedColumnsInOrder(), e));
  }

  @Override
  public OrderedIndexSearchCriterion createIndexSearchCriterion(SearchCriterion criterion, SearchParameter parameter) {    
    List<SearchValue> startKeyValues = new ArrayList<SearchValue>();
    List<SearchValue> stopKeyValues = new ArrayList<SearchValue>();
    boolean stopInclusive = true;
    boolean startInclusive = true;
    
    IndexedColumnDefinition[] indexedColumns = getIndexedColumnsInOrder();
    for (int i = 0; i < indexedColumns.length; i++) {
      IndexedColumnDefinition column = indexedColumns[i];
      if (column.isDefinedAsColumnFunction()) {
        ColumnFunction columnFunction = column.getColumnFunction();
        SearchValue value = columnFunction.executeFunction(criterion, parameter, SearchColumnOperator.GREATER);
        if (value != null) {
          addIfRightIndex(value, startKeyValues, i);
          startInclusive = false;
        } else {
          value = columnFunction.executeFunction(criterion, parameter, SearchColumnOperator.GREATER_EQUALS);
          if (value != null) {
            addIfRightIndex(value, startKeyValues, i);
            startInclusive = true;
          }
        }
        value = columnFunction.executeFunction(criterion, parameter, SearchColumnOperator.SMALLER);
        if (value != null) {
          addIfRightIndex(value, stopKeyValues, i);
          stopInclusive = false;
        } else {
          value = columnFunction.executeFunction(criterion, parameter, SearchColumnOperator.SMALLER_EQUALS);
          if (value != null) {
            addIfRightIndex(value, stopKeyValues, i);
            stopInclusive = true;
          }
        }
        value = columnFunction.executeFunction(criterion, parameter, SearchColumnOperator.EQUALS);
        if (value != null) {
          addIfRightIndex(value, startKeyValues, i);
          addIfRightIndex(value, stopKeyValues, i);
          stopInclusive = true;
          startInclusive = true;
        }
      } else {
        List<ColumnCriterion> columnCriteria = criterion.getColumnCriterionByName(column.getColumnName());
        for (ColumnCriterion columnCriterion : columnCriteria) {
          SearchValue value = parameter.getSearchValue(columnCriterion.getMappingToSearchParameter());
          switch (columnCriterion.getOperator()) {
            case EQUALS :
              addIfRightIndex(value, startKeyValues, i);
              addIfRightIndex(value, stopKeyValues, i);
              stopInclusive = true;
              startInclusive = true;
              break;
            case GREATER :
              addIfRightIndex(value, startKeyValues, i);
              startInclusive = false;
              break;
            case GREATER_EQUALS :
              addIfRightIndex(value, startKeyValues, i);
              startInclusive = true;
              break;
            case SMALLER :
              addIfRightIndex(value, stopKeyValues, i);
              stopInclusive = false;
              break;
            case SMALLER_EQUALS :
              addIfRightIndex(value, stopKeyValues, i);
              stopInclusive = true;
              break;
            default : // IN & LIKE
              throw new UnsupportedOperationException("IN or LIKE in ordered");
          }
        }
      }
    }
     
    OrderedIndexKey rangeStart = new SearchValueBasedOrderedIndexKey(startKeyValues.toArray(new SearchValue[startKeyValues.size()]), KeyType.START, startInclusive,
                                                                     startKeyValues.size() == getIndexedColumnsInOrder().length ? false : true);
    OrderedIndexKey rangeStop = new SearchValueBasedOrderedIndexKey(stopKeyValues.toArray(new SearchValue[stopKeyValues.size()]), KeyType.STOP, stopInclusive,
                                                                    stopKeyValues.size() == getIndexedColumnsInOrder().length ? false : true);
    return new OrderedIndexSearchCriterion(rangeStart, rangeStop);
  }

  
  //we'll only add SearchValues if all indexed columns before have been added
  private void addIfRightIndex(SearchValue value, List<SearchValue> toAddTo, int index) {
    if (index == toAddTo.size()) {
      toAddTo.add(value);
    }
  }
  
  
}
