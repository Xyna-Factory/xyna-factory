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
package com.gip.xyna.xdnc.xnwh;


public enum VelocityTemplateColumn {
  ID(VelocityTemplateStorable.COL_ID),
  APPLICATION(VelocityTemplateStorable.COL_APPLICATION),
  SCOPE(VelocityTemplateStorable.COL_SCOPE),
  TYPE(VelocityTemplateStorable.COL_TYPE),
  PART(VelocityTemplateStorable.COL_PART),
  CONSTRAINTSET(VelocityTemplateStorable.COL_CONSTRAINTSET),
  SCORE(VelocityTemplateStorable.COL_SCORE),
  CONTENT(VelocityTemplateStorable.COL_CONTENT);
  
  private final String columnName;
  
  private VelocityTemplateColumn(String columnumName) {
    this.columnName = columnumName;
  }
  
  public String getColumnName() {
    return columnName;
  }
  
  
  public static VelocityTemplateColumn getVelocityTemplateColumnByColumnName(String columnName) {
    for (VelocityTemplateColumn column : values()) {
      if (column.getColumnName().equalsIgnoreCase(columnName)) {
        return column;
      }
    }
    throw new IllegalArgumentException("Specified columnName '" +columnName + "' is not a valid column of VelocityTemplate");
  }
  
}
