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

import com.gip.xyna.xsor.indices.UniqueIndexKey;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.UniqueIndexSearchCriterion;




public abstract class UniqueIndexDefinition<E> extends IndexDefinition<E, UniqueIndexKey, UniqueIndexSearchCriterion> {
  
  public UniqueIndexDefinition(String tableName, String[] indexedColumns) {
    super(tableName, indexedColumns);
  }

  public abstract UniqueIndexKey createIndexKey(E e);
  
  public abstract UniqueIndexSearchCriterion createIndexSearchCriterion(SearchCriterion criterion, SearchParameter parameter);

  public float coverage(SearchCriterion searchCriterion) {
    String[] indexColumns = getIndexedColumnNamesInOrder();
    for (String string : indexColumns) {
      List<ColumnCriterion> critera = searchCriterion.getColumnCriterionByName(string);
      if (critera == null || critera.size() == 0) {
        return 0.0f;
      }
      ColumnCriterion criterion = critera.get(0); // multiple criteria for Unique (& not being range) is senseless
      if (criterion.getOperator() != SearchColumnOperator.EQUALS) {
        return 0.0f;
      }
    }
    return 1.0f;
  }
  
}
