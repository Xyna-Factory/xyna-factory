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
package com.gip.xyna.xsor.indices.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.management.SearchCriterionIndexDefinitionDeterminator;


public abstract class SearchCriterion {
  
  protected Map<String, List<ColumnCriterion>> columnsByName; //as this is in DNF we don't care for the order they are in
  private ColumnCriterion primaryKeyInEqualComparisionCriterion;
  private List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> appropriateIndexDefinitions;
  
  
  public SearchCriterion(List<ColumnCriterion> columns) {
    resetColumns(columns);
  }
  
  
  private void resetColumns(List<ColumnCriterion> columns) {
    columnsByName = new HashMap<String, List<ColumnCriterion>>();
    for (ColumnCriterion columnCriterion : columns) {
      if (columnCriterion.hasPrimaryKeyInEqualComparison()) {
        primaryKeyInEqualComparisionCriterion = columnCriterion;
      }
      String colName = columnCriterion.getColumnName();
      if (columnsByName.containsKey(colName)) {
        List<ColumnCriterion> list = columnsByName.get(colName);
        list.add(columnCriterion);
        columnsByName.put(colName, list);
      } else {
        List<ColumnCriterion> criterions = new ArrayList<ColumnCriterion>();
        criterions.add(columnCriterion);
        columnsByName.put(colName, criterions);
      }
    }
  }

  
  public List<ColumnCriterion> getColumns() {
    List<ColumnCriterion> columns = new ArrayList<ColumnCriterion>();
    for (List<ColumnCriterion> list : columnsByName.values()) {
      columns.addAll(list);
    }
    return columns;
  }

  
  public void setColumns(List<ColumnCriterion> columns) {
    resetColumns(columns);
  }
  
  
  public List<ColumnCriterion> getColumnCriterionByName(String name) {
    return columnsByName.get(name);
  }
  
  
  public boolean containsPrimaryKeyInEqualComparision() {
    return primaryKeyInEqualComparisionCriterion != null;
  }
  
  
  public ColumnCriterion getPrimaryKeyInEqualComparisionCriterion() {
    return primaryKeyInEqualComparisionCriterion;
  }
  
  
  public void establishAppropriateIndexDefinitions(List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions, SearchCriterionIndexDefinitionDeterminator determinator) {
    appropriateIndexDefinitions = determinator.determineAppropriateIndexDefinitions(indexDefinitions, this);
  }
  
  
  public List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> getAppropriateIndexDefinitions() {
    return appropriateIndexDefinitions;
  }

  
  public abstract boolean fits(Object candidate, SearchParameter parameter);
  
  
  
}
