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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.selectdatamodel;

import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelStorable;

public enum DataModelColumn {
  
  FQNAME(DataModelStorable.COL_FQNAME),
  LABEL(DataModelStorable.COL_LABEL),
  BASETYPEFQNAME(DataModelStorable.COL_BASETYPEFQNAME),
  BASETYPELABEL(DataModelStorable.COL_BASETYPELABEL),
  DATAMODELTYPE(DataModelStorable.COL_DATAMODELTYPE),
  DATAMODELPREFIX(DataModelStorable.COL_DATAMODELPREFIX),
  VERSION(DataModelStorable.COL_VERSION),
  DOCUMENTATION(DataModelStorable.COL_DOCUMENTATION),
  PARAMETER("parameter"),
  XMOMTYPECOUNT(DataModelStorable.COL_XMOMTYPECOUNT),
  DEPLOYABLE(DataModelStorable.COL_DEPLOYABLE)
  ;

  
  private String columnName;
  
  private DataModelColumn(String columnName) {
    this.columnName = columnName;
  }
  
  public String getColumnName() {
    return columnName;
  }
  
  public static DataModelColumn getColumnByName(String columnName) {
    for (DataModelColumn type : values()) {
      if (type.columnName.equals(columnName)) {
        return type;
      }
    }
    throw new IllegalArgumentException(columnName);
  }
}
