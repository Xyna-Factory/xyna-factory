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
package com.gip.xyna.xfmg.xods.orderinputsourcemgmt.selectorderinputsource;

import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;


public enum OrderInputSourceColumn {

  ID(OrderInputSourceStorable.COL_ID),
  NAME(OrderInputSourceStorable.COL_NAME),
  TYPE(OrderInputSourceStorable.COL_TYPE),
  ORDERTYPE(OrderInputSourceStorable.COL_ORDERTYPE),
  APPLICATIONNAME(OrderInputSourceStorable.COL_APPLICATIONNAME, new String[] {"application"}),
  VERSIONNAME(OrderInputSourceStorable.COL_VERSIONNAME, new String[] {"version"}),
  WORKSPACENAME(OrderInputSourceStorable.COL_WORKSPACENAME, new String[] {"workspace"}),
  DOCUMENTATION(OrderInputSourceStorable.COL_DOCUMENTATION),
  PARAMETER("parameters"),
  REFERENCE_COUNT("refcnt"), 
  STATE("state");

  
  private String columnName;
  private String[] aliases;
  
  private OrderInputSourceColumn(String columnName) {
    this(columnName, new String[0]);
  }

  private OrderInputSourceColumn(String columnName, String[] aliases) {
    this.columnName = columnName;
    this.aliases = aliases;
  }
  
  
  public String getColumnName() {
    return columnName;
  }
  
  public String[] getAliases() {
    return aliases;
  }
  
  public static OrderInputSourceColumn getColumnByName(String columnName) {
    for (OrderInputSourceColumn type : values()) {
      if (type.columnName.equals(columnName)) {
        return type;
      } else {
        for (String alias : type.getAliases()) {
          if (alias.equals(columnName)) {
            return type;
          }
        }
      }
    }
    throw new IllegalArgumentException(columnName);
  }
}
