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
package com.gip.xyna.xsor.indices.definitions;

import java.util.List;

import com.gip.xyna.xsor.indices.OrderedIndexKey;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.OrderedIndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;


public abstract class OrderedIndexDefinition<E> extends IndexDefinition<E, OrderedIndexKey, OrderedIndexSearchCriterion> {
  
  private static final float MAX_COVERAGE_FOR_PARTIALCOVERAGE = 0.95f;

  public OrderedIndexDefinition(String tableName, String[] indexedColumns) {
    super(tableName, indexedColumns);
  }

  public abstract OrderedIndexKey createIndexKey(E e);
  
  public abstract OrderedIndexSearchCriterion createIndexSearchCriterion(SearchCriterion criterion, SearchParameter parameter);

  public float coverage(SearchCriterion searchCriterion) {
    int coveredColumns = 0;
    boolean allStartKeyColumnsGiven = true;
    boolean allStopKeyColumnsGiven = true;
    String[] indexedColumns = getIndexedColumnNamesInOrder();
    for (String indexedColumn : indexedColumns) {
      List<ColumnCriterion> criteria = searchCriterion.getColumnCriterionByName(indexedColumn);
      if (criteria == null || criteria.size() == 0) {
        allStartKeyColumnsGiven = false;
        allStopKeyColumnsGiven = false;
        break;
      }
      boolean startKeyColumnsContainedForThisName = false;
      boolean stopKeyColumnsContainedForThisName = false;
      for (ColumnCriterion columnCriterion : criteria) {
        switch (columnCriterion.getOperator()) {
          case GREATER :
          case GREATER_EQUALS:
            if (allStartKeyColumnsGiven) {
              coveredColumns++;
              startKeyColumnsContainedForThisName = true;
            }
            break;
          case SMALLER:
          case SMALLER_EQUALS:
            if (allStopKeyColumnsGiven) {
              coveredColumns++;
              stopKeyColumnsContainedForThisName = true;
            }
            break;
          case EQUALS:
            if (allStartKeyColumnsGiven) {
              startKeyColumnsContainedForThisName = true;
            }
            if (allStopKeyColumnsGiven) {
              stopKeyColumnsContainedForThisName = true;
            }
            if (allStartKeyColumnsGiven || allStopKeyColumnsGiven) {
              coveredColumns++;
            }
            break;
          case IN:
          case LIKE:
            return 0.0f;
        }
      }
      allStartKeyColumnsGiven = startKeyColumnsContainedForThisName;
      allStopKeyColumnsGiven = stopKeyColumnsContainedForThisName;
      if (!allStartKeyColumnsGiven && !allStopKeyColumnsGiven) {
        break;
      }
    }
    
    float maxValueCoverage;
    if (!allStartKeyColumnsGiven && !allStopKeyColumnsGiven) { // TODO && or || ?
      maxValueCoverage = MAX_COVERAGE_FOR_PARTIALCOVERAGE;
    } else {
      maxValueCoverage = 1.0f;
    }
    float singleCover = maxValueCoverage / searchCriterion.getColumns().size();
    return singleCover * coveredColumns;
  }
  
}
