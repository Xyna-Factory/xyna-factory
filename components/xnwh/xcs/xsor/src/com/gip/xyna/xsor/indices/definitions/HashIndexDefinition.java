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

import com.gip.xyna.xsor.indices.HashIndexKey;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.HashIndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;


public abstract class HashIndexDefinition<E> extends IndexDefinition<E, HashIndexKey, HashIndexSearchCriterion> {

  public HashIndexDefinition(String tableName, String[] indexedColumns) {
    super(tableName, indexedColumns);
  }

  public abstract HashIndexKey createIndexKey(E e);
  
  public abstract HashIndexSearchCriterion createIndexSearchCriterion(SearchCriterion criterion, SearchParameter parameter);
  
  public float coverage(SearchCriterion searchCriterion) {
    float rating = 0.0f;
    float singleCover = 1.0f / searchCriterion.getColumns().size();
    String[] indexedColumns = getIndexedColumnNamesInOrder();
    for (String string : indexedColumns) {
      List<ColumnCriterion> criteria = searchCriterion.getColumnCriterionByName(string);
      if (criteria == null || criteria.size() == 0) {
        return 0.0f;
      }
      ColumnCriterion criterion = criteria.get(0); // again more then one criterion would make no sense
      if (criterion.getOperator() == SearchColumnOperator.EQUALS) {
        rating += singleCover;
      } else {
        return 0.0f;
      }
    }
    return rating;
  }
  
}
