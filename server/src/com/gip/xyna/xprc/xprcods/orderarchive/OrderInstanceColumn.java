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

package com.gip.xyna.xprc.xprcods.orderarchive;



import com.gip.xyna.xnwh.selection.parsing.PrimitiveColumnType;



public enum OrderInstanceColumn {

  C_ID(OrderInstance.COL_ID, PrimitiveColumnType.NUMBER),
  C_PARENT_ID(OrderInstance.COL_PARENT_ID, PrimitiveColumnType.NUMBER),
  C_EXECUTION_TYPE(OrderInstance.COL_EXECUTION_TYPE, PrimitiveColumnType.STRING),
  C_PRIORITY(OrderInstance.COL_PRIORITY, PrimitiveColumnType.NUMBER),
  C_STATUS(OrderInstance.COL_STATUS, PrimitiveColumnType.STRING),
  C_STATUS_COMPENSATE(OrderInstance.COL_STATUS_COMPENSATE, PrimitiveColumnType.STRING),
  C_SUSPENSION_STATUS(OrderInstance.COL_SUSPENSION_STATUS, PrimitiveColumnType.STRING),
  C_SUSPENSION_CAUSE(OrderInstance.COL_SUSPENSION_CAUSE, PrimitiveColumnType.STRING),
  C_START_TIME(OrderInstance.COL_START_TIME, PrimitiveColumnType.NUMBER),
  C_STOP_TIME(OrderInstance.COL_STOP_TIME, PrimitiveColumnType.NUMBER),
  C_LAST_UPDATE(OrderInstance.COL_LAST_UPDATE, PrimitiveColumnType.NUMBER),
  C_ORDER_TYPE(OrderInstance.COL_ORDERTYPE, PrimitiveColumnType.STRING),
  C_MONITORING_LEVEL(OrderInstance.COL_MONITORING_LEVEL, PrimitiveColumnType.NUMBER),
  C_CUSTOM0(OrderInstance.COL_CUSTOM0, PrimitiveColumnType.STRING),
  C_CUSTOM1(OrderInstance.COL_CUSTOM1, PrimitiveColumnType.STRING),
  C_CUSTOM2(OrderInstance.COL_CUSTOM2, PrimitiveColumnType.STRING),
  C_CUSTOM3(OrderInstance.COL_CUSTOM3, PrimitiveColumnType.STRING),
  C_SESSION_ID(OrderInstance.COL_SESSION_ID, PrimitiveColumnType.STRING),
  C_EXCEPTIONS(OrderInstance.COL_EXCEPTIONS),
  C_INTERNAL_ORDER(OrderInstance.COL_INTERNAL_ORDER, PrimitiveColumnType.BOOLEAN),
  C_AUDIT_DATA_XML(OrderInstanceDetails.COL_AUDIT_DATA_AS_XML),
  C_AUDIT_DATA_XML_B(OrderInstanceDetails.COL_AUDIT_DATA_AS_XML_B),
  C_AUDIT_DATA_AS_JAVA_OBJECT(OrderInstanceDetails.COL_AUDIT_DATA_AS_JAVA_OBJECT),
  C_ROOT_ID(OrderInstance.COL_ROOT_ID, PrimitiveColumnType.NUMBER),
  C_APPLICATIONNAME(OrderInstance.COL_APPLICATIONNAME, PrimitiveColumnType.STRING),
  C_VERISONNAME(OrderInstance.COL_VERISONNAME, PrimitiveColumnType.STRING),
  C_WORKSPACENAME(OrderInstance.COL_WORKSPACENAME, PrimitiveColumnType.STRING),
  C_BATCH_PROCESS_ID(OrderInstance.COL_BATCH_PROCESS_ID, PrimitiveColumnType.NUMBER);


  private String columnName;
  private PrimitiveColumnType columnType;


  private OrderInstanceColumn(String s, PrimitiveColumnType t) {
    columnName = s;
    columnType = t;
  }
  
  private OrderInstanceColumn(String s) {
    this(s,PrimitiveColumnType.COMPLEX);
  }


  public String getColumnName() {
    return columnName;
  }
  
  
  public PrimitiveColumnType getColumnType() {
    return columnType;
  }
  
  
  public static OrderInstanceColumn getByColumnName(String columnName) {
    for (OrderInstanceColumn next : values()) {
      if (next.getColumnName().equals(columnName)) {
        return next;
      }
    }
    throw new IllegalArgumentException(new StringBuilder().append("Unknown ").append(OrderInstanceColumn.class.getSimpleName())
                                       .append(" name: <").append(columnName).append(">").toString());
  }
  

}
