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
package com.gip.xyna.update;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInfoStorable;

public class UpdateToStandAloneOrderInstance extends UpdateJustVersion {

  public UpdateToStandAloneOrderInstance(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
  }
  
  private final static String ALL_BASE_COLUMNS =
    new StringBuilder()
      .append(OrderInstance.COL_ID)
      .append(", ")
      .append(OrderInstance.COL_PARENT_ID)
      .append(", ")
      .append(OrderInstance.COL_EXECUTION_TYPE)
      .append(", ")
      .append(OrderInstance.COL_PRIORITY)
      .append(", ")
      .append(OrderInstance.COL_STATUS)
      .append(", ")
      .append(OrderInstance.COL_STATUS_COMPENSATE)
      .append(", ")
      .append(OrderInstance.COL_SUSPENSION_STATUS)
      .append(", ")
      .append(OrderInstance.COL_SUSPENSION_CAUSE)
      .append(", ")
      .append(OrderInstance.COL_START_TIME)
      .append(", ")
      .append(OrderInstance.COL_LAST_UPDATE)
      .append(", ")
      .append(OrderInstance.COL_STOP_TIME)
      .append(", ")
      .append(OrderInstance.COL_ORDERTYPE)
      .append(", ")
      .append(OrderInstance.COL_MONITORING_LEVEL)
      .append(", ")
      .append(OrderInstance.COL_CUSTOM0)
      .append(", ")
      .append(OrderInstance.COL_CUSTOM1)
      .append(", ")
      .append(OrderInstance.COL_CUSTOM2)
      .append(", ")
      .append(OrderInstance.COL_CUSTOM3)
      .append(", ")
      .append(OrderInstance.COL_SESSION_ID)
      .append(", ")
      .append(OrderInstance.COL_INTERNAL_ORDER)
      .append(", ")
      .append(OrderInstance.COL_ROOT_ID)
      .append(", ")
      .append(OrderInstance.COL_APPLICATIONNAME)
      .append(", ")
      .append(OrderInstance.COL_VERISONNAME)
      .append(", ")
      .append(OrderInstance.COL_WORKSPACENAME)
      .append(", ")
      .append(OrderInstance.COL_BATCH_PROCESS_ID)
      .append(", ")
      .append(OrderInstance.COL_EXCEPTIONS)
      .toString(); 
  
  private final static String UPDATE_SQL = 
    "INSERT INTO " + OrderInfoStorable.TABLE_NAME + " (" + ALL_BASE_COLUMNS + ") SELECT " + ALL_BASE_COLUMNS + " FROM orderarchive";
  
  private final static String COUNT_SQL = "SELECT COUNT(*) FROM " + OrderInfoStorable.TABLE_NAME;
  private final static String COUNT_OLD_SQL = "SELECT COUNT(*) FROM " + OrderInstanceDetails.TABLE_NAME; 
  
  @Override
  protected void update() throws XynaException {
    super.update();
    
    ODS ods = ODSImpl.getInstance();
    
    ods.registerStorable(OrderInstanceDetails.class);
    
    long orderarchiveDefaultPlId = ods.getPersistenceLayerInstanceId(ODSConnectionType.DEFAULT, OrderInstanceDetails.class);
    ods.setPersistenceLayerForTable(orderarchiveDefaultPlId, OrderInfoStorable.TABLE_NAME, null);
    
    long orderarchiveHistoryPlId = ods.getPersistenceLayerInstanceId(ODSConnectionType.HISTORY, OrderInstanceDetails.class);
    ods.setPersistenceLayerForTable(orderarchiveHistoryPlId, OrderInfoStorable.TABLE_NAME, null);
    
    ods.registerStorable(OrderInfoStorable.class);
    
    ODSConnection conHis = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      ResultSetReader<Long> longReader = new ResultSetReader<Long>() {

        @Override
        public Long read(ResultSet rs) throws SQLException {
          return rs.getLong(1);
        }
        
      };
      
      Query<Long> countOld = new Query<>(COUNT_OLD_SQL, longReader);
      PreparedQuery<Long> pCountOld = conHis.prepareQuery(countOld);
      
      Long resultOld = conHis.queryOneRow(pCountOld, new Parameter());
      if (resultOld != null && resultOld > 0) {
        Query<Long> count = new Query<>(COUNT_SQL, longReader);
        PreparedQuery<Long> pCount = conHis.prepareQuery(count);
        
        Long result = conHis.queryOneRow(pCount, new Parameter());
        if (result == null || result <= 0) {
          Command cmd = new Command(UPDATE_SQL, OrderInstanceDetails.TABLE_NAME);
          PreparedCommand prepCmd = conHis.prepareCommand(cmd);
          conHis.executeDML(prepCmd, new Parameter());
          conHis.commit();
        }
      }
    } finally {
      conHis.closeConnection();
      ods.unregisterStorable(OrderInstanceDetails.class);
      ods.unregisterStorable(OrderInfoStorable.class);
    }
    
  }

}
