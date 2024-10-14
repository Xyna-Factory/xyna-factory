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
package com.gip.xyna.persistence.xsor.indices;

import java.util.Map;

import com.gip.xyna.xsor.indices.UniqueIndexKey;
import com.gip.xyna.xsor.indices.definitions.UniqueIndexDefinition;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.UniqueIndexSearchCriterion;
import com.gip.xyna.xsor.indices.searchvaluekeys.SearchValueBasedUniqueIndexKey;
import com.gip.xyna.xnwh.persistence.Storable;


public class StorableBasedUniqueIndexDefinition<E extends Storable> extends UniqueIndexDefinition<E> {
  
  
  public StorableBasedUniqueIndexDefinition(String tableName, String[] indexedColumns) {
    super(tableName, indexedColumns);
  }
  
  
  @Override
  public UniqueIndexKey createIndexKey(E e) {
    return new SearchValueBasedUniqueIndexKey(StorableBasedIndexHelper.createSearchValuesForColumnsFromStorable(getIndexedColumnsInOrder(), e));
  }


  @Override
  public UniqueIndexSearchCriterion createIndexSearchCriterion(SearchCriterion criterion, SearchParameter parameter) {
    return new UniqueIndexSearchCriterion(new SearchValueBasedUniqueIndexKey(StorableBasedIndexHelper.createSearchValuesForColumnsFromSearchRequest(getIndexedColumnsInOrder(),
                                                                                                                                                    criterion,
                                                                                                                                                    parameter)));
  }
  

  
}
