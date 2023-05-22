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
package com.gip.xyna.xsor.indices.helper;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xsor.indices.HashIndexKey;
import com.gip.xyna.xsor.indices.definitions.HashIndexDefinition;
import com.gip.xyna.xsor.indices.definitions.IndexedColumnDefinition;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.HashIndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchValue;
import com.gip.xyna.xsor.indices.searchvaluekeys.SearchValueBasedHashIndexKey;


public class HashTestIndexDefinition extends HashIndexDefinition<TestObject> {

  
  public HashTestIndexDefinition(String tableName, String[] indexedColumns) {
    super(tableName, indexedColumns);
  }


  @Override
  public HashIndexKey createIndexKey(TestObject e) {
    return new SearchValueBasedHashIndexKey(e.generateSearchValuesFromColumnNames(getIndexedColumnsInOrder()));
  }


  @Override
  public HashIndexSearchCriterion createIndexSearchCriterion(SearchCriterion criterion, SearchParameter parameter) {
    List<SearchValue> values = new ArrayList<SearchValue>();
    for (IndexedColumnDefinition column : getIndexedColumnsInOrder()) {
      if (column.isDefinedAsColumnFunction()) {
        SearchValue searchValue = column.getColumnFunction().executeFunction(criterion, parameter);
        if (searchValue != null) {
          values.add(searchValue);
        }
      } else {
        List<ColumnCriterion> list = criterion.getColumnCriterionByName(column.getColumnName());
        if (list != null && list.size() > 0) {
          ColumnCriterion columnCriterion = list.get(0); // more then 1 value does not make sense for DNF & Unique/Hash index (as long as we don't implement !=)
          values.add(parameter.getSearchValue(columnCriterion.getMappingToSearchParameter()));
        }
      }
    }
    return new HashIndexSearchCriterion(new SearchValueBasedHashIndexKey(values.toArray(new SearchValue[values.size()])).hashCode());
  }

}
