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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xsor.indices.definitions.ColumnFunction;
import com.gip.xyna.xsor.indices.definitions.IndexedColumnDefinition;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchValue;
import com.gip.xyna.xnwh.persistence.Storable;


public class StorableBasedIndexHelper {

  
  public static <S extends Storable> SearchValue[] createSearchValuesForColumnsFromStorable(IndexedColumnDefinition[] columns, S storable) {
    List<SearchValue> values = new ArrayList<SearchValue>();
    for (IndexedColumnDefinition column : columns) {
      if (column.isDefinedAsColumnFunction()) {
        ColumnFunction function = column.getColumnFunction();
        SearchValue searchValue = function.executeFunction(getValuesFormStorable(function.getColumns(), storable));
        if (searchValue != null) {
          values.add(searchValue);
        }
      } else {
        Serializable value = storable.getValueByColString(column.getColumnName());
        values.add(new SearchValue(value));
      }
    }
    return values.toArray(new SearchValue[values.size()]);
  }
    
  
  public static <S extends Storable> SearchValue[] createSearchValuesForColumnsFromSearchRequest(IndexedColumnDefinition[] columns,
                                                                                                 SearchCriterion searchCriterion,
                                                                                                 SearchParameter searchParameter) {
    List<SearchValue> values = new ArrayList<SearchValue>();
    for (IndexedColumnDefinition column : columns) {
      if (column.isDefinedAsColumnFunction()) {
        SearchValue searchValue = column.getColumnFunction().executeFunction(searchCriterion, searchParameter);
        if (searchValue != null) {
          values.add(searchValue);
        }
      } else {
        List<ColumnCriterion> list = searchCriterion.getColumnCriterionByName(column.getColumnName());
        if (list != null && list.size() > 0) {
          ColumnCriterion columnCriterion = list.get(0); // more then 1 value does not make sense for DNF & Unique/Hash index (as long as we don't implement !=)
          values.add(searchParameter.getSearchValue(columnCriterion.getMappingToSearchParameter()));
        }
      }
    }
    return values.toArray(new SearchValue[values.size()]);
  }
  
  
  public static <S extends Storable> List<Object> getValuesFormStorable(String[] columnIdentifiers, S storable) {
    List<Object> values = new ArrayList<Object>();
    for (String columnIdentifier : columnIdentifiers) {
      values.add(storable.getValueByColString(columnIdentifier));
    }
    return values;
  }
  
  /*public static String[] getIndexedColumNamesInOrder(IndexedColumnDefinition[] columns) {
    List<String> allSingleColumns = new ArrayList<String>();
    for (String column : columns) {
      ColumnFunction function = columnFunctions.get(column);
      if (function == null) {
        allSingleColumns.add(column);
      } else {
        for (String columnInFunction : function.getColumns()) {
          allSingleColumns.add(columnInFunction);
        }
      }
    }
    return allSingleColumns.toArray(new String[allSingleColumns.size()]);
  }*/
  
}
