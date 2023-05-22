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

import java.util.regex.Matcher;


public class IndexedColumnDefinition {
  
  private final String columnName;
  private final ColumnFunction columnFunction;
  
  private IndexedColumnDefinition(String columnName) {
    this(columnName, null);
  }
  
  private IndexedColumnDefinition(String columnName, ColumnFunction columnFunction) {
    this.columnName = columnName;
    this.columnFunction = columnFunction;
  }
  
  
  public boolean isDefinedAsColumnFunction() {
    return columnFunction != null;
  }
  
  
  public String getColumnName() {
    return columnName;
  }
  
  
  public ColumnFunction getColumnFunction() {
    return columnFunction;
  }
  
  
  
  public static IndexedColumnDefinition generateIndexColumnDefinition(String columnName) {
    Matcher columnFunctionDetector = ColumnFunction.functionPattern.matcher(columnName);
    if (columnFunctionDetector.matches()) {
      return new IndexedColumnDefinition(columnName, new ColumnFunction(columnName)); 
    } else {
      return new IndexedColumnDefinition(columnName); 
    }
  }
  
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof IndexedColumnDefinition) {
      IndexedColumnDefinition otherColumnDefinition = (IndexedColumnDefinition) obj;
      return getColumnName().equals(otherColumnDefinition.getColumnName());
    } else {
      return false;
    }
  }
  
  
  @Override
  public int hashCode() {
    return getColumnName().hashCode();
  }


}
