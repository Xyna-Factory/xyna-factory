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

package com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons;

import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;


public enum CronLikeOrderColumn {
  ID(CronLikeOrder.COL_ID),
  LABEL(CronLikeOrder.COL_LABEL),
  ORDERTYPE(CronLikeOrder.COL_ORDERTYPE),
  STARTTIME(CronLikeOrder.COL_STARTTIME),
  NEXTEXECUTION(CronLikeOrder.COL_NEXT_EXEC_TIME),
  INTERVAL(CronLikeOrder.COL_INTERVAL),
  STATUS(CronLikeOrder.COL_STATUS),
  ONERROR(CronLikeOrder.COL_ONERROR),
  APPLICATIONNAME(CronLikeOrder.COL_APPLICATIONNAME),
  VERSIONNAME(CronLikeOrder.COL_VERSIONNAME),
  WORKSPACENAME(CronLikeOrder.COL_WORKSPACENAME),
  ENABLED(CronLikeOrder.COL_ENABLED),
  CREATIONPARAMETER(CronLikeOrder.COL_CREATION_PARAMTER),
  TIMEZONEID(CronLikeOrder.COL_TIME_ZONE_ID),
  CONSIDERDAYLIGHTSAVING(CronLikeOrder.COL_CONSIDER_DAYLIGHT_SAVING),
  CUSTOM0(CronLikeOrder.COL_CRON_LIKE_ORDER_CUSTOM0),
  CUSTOM1(CronLikeOrder.COL_CRON_LIKE_ORDER_CUSTOM1),
  CUSTOM2(CronLikeOrder.COL_CRON_LIKE_ORDER_CUSTOM2),
  CUSTOM3(CronLikeOrder.COL_CRON_LIKE_ORDER_CUSTOM3),
  ERROR_MSG(CronLikeOrder.COL_ERROR_MSG),
  ROOT_ORDER_ID(CronLikeOrder.COL_ASSIGNED_ROOT_ORDER_ID),
  EXEC_COUNT(CronLikeOrder.COL_EXEC_COUNT),
  REMOVE_ON_SHUTDOWN(CronLikeOrder.COL_REMOVE_ON_SHUTDOWN),
  REVISION(CronLikeOrder.COL_REVISION);

  
  private String columnName;
  
  private CronLikeOrderColumn(String columnName) {
    this.columnName = columnName;
  }
  
  public String getColumnName() {
    return columnName;
  }
  
  
  public static CronLikeOrderColumn getCLOColumnByName(String columnName) {
    for (CronLikeOrderColumn type : values()) {
      if (type.columnName.equals(columnName)) {
        return type;
      }
    }
    throw new IllegalArgumentException(columnName);
  }
}
