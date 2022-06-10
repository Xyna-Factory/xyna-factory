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


public class ColumnCriterion {
  
  private String columnName;
  private SearchColumnOperator operator;
  private int mappingToSearchParameter;
  private boolean primaryKeyInEqualComparison;
  
  public ColumnCriterion(String columnName, SearchColumnOperator operator, int mappingToSearchParameter) {
    this(columnName, operator, mappingToSearchParameter, false);
  }
  
  
  public ColumnCriterion(String columnName, SearchColumnOperator operator, int mappingToSearchParameter, boolean primaryKeyInEqualComparison) {
    this.columnName = columnName;
    this.operator = operator;
    this.mappingToSearchParameter = mappingToSearchParameter;
    this.primaryKeyInEqualComparison = primaryKeyInEqualComparison;
  }

  
  public String getColumnName() {
    return columnName;
  }

  
  public SearchColumnOperator getOperator() {
    return operator;
  }

  
  public int getMappingToSearchParameter() {
    return mappingToSearchParameter;
  }

  
  public boolean hasPrimaryKeyInEqualComparison() {
    return primaryKeyInEqualComparison;
  }
  
  
  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  
  public void setOperator(SearchColumnOperator operator) {
    this.operator = operator;
  }

  
  public void setMappingToSearchParameter(int mappingToSearchParameter) {
    this.mappingToSearchParameter = mappingToSearchParameter;
  }
  
  public void setPrimaryKeyInEqualComparison(boolean primaryKeyInEqualComparison) {
    this.primaryKeyInEqualComparison = primaryKeyInEqualComparison;
  }
  
}
