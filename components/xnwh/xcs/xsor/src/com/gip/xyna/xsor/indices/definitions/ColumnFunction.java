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
package com.gip.xyna.xsor.indices.definitions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchValue;


public class ColumnFunction {

  public enum ColumnFunctionType {
    MAX
  }
  
  public static Pattern functionPattern = Pattern.compile("^(.+)\\((.+)\\)$");
  
  private ColumnFunctionType type;
  private String[] columns;
  
  
  public ColumnFunction(String function) {
    Matcher matcher = functionPattern.matcher(function);
    if (matcher.matches()) {
      this.type = ColumnFunctionType.valueOf(matcher.group(1));
      this.columns = matcher.group(2).split(",");
      for(int i=0; i<columns.length; i++) {
        columns[i] = columns[i].trim();
      }
    } else {
      throw new RuntimeException(function + " is no ColumnFunction");
    }
  }
  
  
  public ColumnFunctionType getType() {
    return type;
  }
  
  public String[] getColumns() {
    return columns;
  }
  
  public void setType(ColumnFunctionType type) {
    this.type = type;
  }
  
  public void setColumns(String[] columns) {
    this.columns = columns;
  }
  
  
  public SearchValue executeFunction(SearchCriterion criterion, SearchParameter searchParameter, SearchColumnOperator operatorRestriction) {
    List<Object> values = new ArrayList<Object>();
    for (String columnName : columns) {
      List<ColumnCriterion> columnCriteria = criterion.getColumnCriterionByName(columnName);
      if (columnCriteria != null && columnCriteria.size() > 0) {
        if (operatorRestriction == null) {
          values.add(searchParameter.getSearchValue(columnCriteria.get(0).getMappingToSearchParameter()).getValue());
        } else {
          for (ColumnCriterion columnCriterion : columnCriteria) {
            if (columnCriterion.getOperator() == operatorRestriction) { // several of the same operator should not happen...as long as we do not implement an != ...
              values.add(searchParameter.getSearchValue(columnCriterion.getMappingToSearchParameter()).getValue());
            }
          }
        }
      }
    }
    if (values.size() == 0) {
      return null;
    }
    switch (type) {
      case MAX :
        Object value = executeMax(values);
        return new SearchValue(value);

      default :
        throw new UnsupportedOperationException(type.toString());
    }
  }
  
  public SearchValue executeFunction(SearchCriterion criterion, SearchParameter searchParameter) {
    return executeFunction(criterion, searchParameter, null);
  }
  
  
  public SearchValue executeFunction(List<Object> values) {
    switch (type) {
      case MAX :
        Object value = executeMax(values);
        return new SearchValue(value);

      default :
        throw new UnsupportedOperationException(type.toString());
    }
  }
  
  
  private Object executeMax(List<Object> values) {
    // let's assume they are comparable
    if (values.size() == 0) {
      return null;
    }
    if (values.size() == 1) {
      return values.get(0);
    }
    Comparable biggest = (Comparable) values.get(0);
    for (int i=1; i < values.size(); i++) {
      if (biggest == null) {
        biggest = (Comparable) values.get(i);;
      } else {
        Comparable contender = (Comparable) values.get(i);
        if (contender != null) {
          int result = biggest.compareTo(contender);
          if (result < 0) {
            biggest = contender;
          }
        }
      }
    }
    return biggest;
  }
  
  
}
