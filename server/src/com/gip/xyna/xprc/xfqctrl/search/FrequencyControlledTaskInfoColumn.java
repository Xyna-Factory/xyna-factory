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

package com.gip.xyna.xprc.xfqctrl.search;

import com.gip.xyna.xnwh.selection.parsing.PrimitiveColumnType;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;



public enum FrequencyControlledTaskInfoColumn {

                  ID(FrequencyControlledTaskInformation.COL_TASKID, PrimitiveColumnType.NUMBER),
                  TASK_LABEL(FrequencyControlledTaskInformation.COL_TASKLABEL, PrimitiveColumnType.STRING),
                  STATUS(FrequencyControlledTaskInformation.COL_TASK_STATUS, PrimitiveColumnType.STRING), 
                  EVENT_COUNT(FrequencyControlledTaskInformation.COL_EVENT_COUNT, PrimitiveColumnType.NUMBER), 
                  FINISHED_EVENT(FrequencyControlledTaskInformation.COL_FINISHED_EVENTS, PrimitiveColumnType.NUMBER), 
                  FAILED_EVENTS(FrequencyControlledTaskInformation.COL_FAILED_EVENTS, PrimitiveColumnType.NUMBER), 
                  MAX_EVENTS(FrequencyControlledTaskInformation.COL_MAX_EVENTS, PrimitiveColumnType.NUMBER), 
                  EVENT_CREATION_INFO(FrequencyControlledTaskInformation.COL_EVENT_CREATION_INFORMATION, PrimitiveColumnType.STRING), 
                  STATISTICS(FrequencyControlledTaskInformation.COL_STATISTICS_INFORMATION), 
                  START_TIME(FrequencyControlledTaskInformation.COL_START_TIME, PrimitiveColumnType.NUMBER), 
                  STOP_TIME(FrequencyControlledTaskInformation.COL_STOP_TIME, PrimitiveColumnType.NUMBER),
                  APPLICATIONNAME(FrequencyControlledTaskInformation.COL_APPLICATIONNAME, PrimitiveColumnType.STRING, new String[] {"application"}),
                  VERSIONNAME(FrequencyControlledTaskInformation.COL_VERSIONNAME, PrimitiveColumnType.STRING, new String[] {"version"}),
                  WORKSPACENAME(FrequencyControlledTaskInformation.COL_WORKSPACENAME, PrimitiveColumnType.STRING, new String[] {"workspace"});

  private String columnName;
  private PrimitiveColumnType columnType;
  private String[] aliases;

  private FrequencyControlledTaskInfoColumn(String s, PrimitiveColumnType t) {
    this(s, t, new String[0]);
  }

  private FrequencyControlledTaskInfoColumn(String s, PrimitiveColumnType t, String[] aliases) {
    this.columnName = s;
    this.columnType = t;
    this.aliases = aliases;
  }
  
  private FrequencyControlledTaskInfoColumn(String s) {
    this(s, PrimitiveColumnType.COMPLEX);
  }


  public String getColumnName() {
    return this.columnName;
  }

  
  public PrimitiveColumnType getColumnType() {
    return columnType;
  }
  
  
  public String[] getAliases() {
    return aliases;
  }

  public static FrequencyControlledTaskInfoColumn getByColumnName(String columnName) {
    for (FrequencyControlledTaskInfoColumn next : values()) {
      if (next.getColumnName().equals(columnName)) {
        return next;
      } else {
        for (String alias : next.getAliases()) {
          if (alias.equals(columnName)) {
            return next;
          }
        }
      }
    }
    
    throw new IllegalArgumentException("Unknown " + FrequencyControlledTaskInfoColumn.class.getSimpleName()
                    + " name: <" + columnName + ">");
  }

}
