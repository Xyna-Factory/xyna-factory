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
package com.gip.xyna.xsor.indices.definitions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;


public abstract class IndexDefinition<E, K extends IndexKey, R extends IndexSearchCriterion> {

  private final String tableName;
  private final IndexedColumnDefinition[] indexedColumns;
  private final String[] indexedColumnNamesInOrder;
  
  
  public IndexDefinition(String tableName, String[] indexedColumns) {
    this.tableName = tableName;
    this.indexedColumns = new IndexedColumnDefinition[indexedColumns.length];
    for (int i = 0; i<indexedColumns.length; i++) {
      this.indexedColumns[i] = IndexedColumnDefinition.generateIndexColumnDefinition(indexedColumns[i]);
    }
    List<String> basicColumnNames = new ArrayList<String>();
    for (IndexedColumnDefinition indexedColumn : this.indexedColumns) {
      if (indexedColumn.isDefinedAsColumnFunction()) {
        basicColumnNames.addAll(Arrays.asList(indexedColumn.getColumnFunction().getColumns()));
      } else {
        basicColumnNames.add(indexedColumn.getColumnName());
      }
    }
    indexedColumnNamesInOrder = basicColumnNames.toArray(new String[basicColumnNames.size()]);
  }
  
  public abstract K createIndexKey(E e);
  
  public abstract R createIndexSearchCriterion(SearchCriterion criterion, SearchParameter parameter);
  
  
  public IndexedColumnDefinition[] getIndexedColumnsInOrder() {
    return indexedColumns;
  }
  
  
  public String[] getIndexedColumnNamesInOrder() {
    return indexedColumnNamesInOrder;
  }
  
  
  public String getTableName() {
    return tableName;
  }
  
  
  @Override
  public boolean equals(Object obj) {
    if (this.getClass().isInstance(obj)) {
      IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion> otherDefinition = (IndexDefinition)obj;
      if (getTableName().equals(otherDefinition.getTableName())) {
        return Arrays.equals(getIndexedColumnsInOrder(), otherDefinition.getIndexedColumnsInOrder());
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  
  private Integer hashcode;
  
  @Override
  public int hashCode() {
    if (hashcode == null) {
      int hash = getTableName().hashCode();
      hash = 31 * hash + Arrays.hashCode(getIndexedColumnsInOrder());
      hashcode = new Integer(hash);
    }
    return hashcode.intValue();
  }
  
  
  public abstract float coverage(SearchCriterion searchCriterion);
  
}
