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


package com.gip.juno.ws.db.tables.dhcp;

import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.InputFormat;
import com.gip.juno.ws.enums.InputType;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.TableHandlerBasic;

public class PoolHandler extends TableHandlerBasic implements TableHandler {
  
  public final static String POOLID_COLUMN_NAME = "PoolID";
  public final static String SUBNETID_COLUMN_NAME = "SubnetID";
  public final static String TARGETSTATE_COLUMN_NAME = "TargetState";
  public final static String RANGESTART_COLUMN_NAME = "RangeStart";
  public final static String RANGESTOP_COLUMN_NAME = "RangeStop";
  public final static String ISDEPLOYED_COLUMN_NAME = "IsDeployed";
  
  public PoolHandler() {
    super("Pool", true);
  }
  
  public DBTableInfo initDBTableInfo() {
    DBTableInfo table = new DBTableInfo("pool", "dhcp");

    table.addColumn(new ColInfo(POOLID_COLUMN_NAME).setType(ColType.integer).setVisible(false).setPk().setAutoIncrement());
    table.addColumn(new ColInfo("Subnet").setType(ColType.string).setVisible(true).setUpdates(true)
        .setParentTable("Subnet").setParentCol("SubnetId").setLookupCol("Subnet")
        .setVirtual().setDBName("SubnetID"));
    table.addColumn(new ColInfo(SUBNETID_COLUMN_NAME).setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("PoolTypeID").setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("PoolType").setType(ColType.string).setVisible(true).setUpdates(false)
        .setParentTable("pooltype").setParentCol("PoolTypeId").setLookupCol("Name")
        .setVirtual().setDBName("PoolTypeID"));
    table.addColumn(new ColInfo(RANGESTART_COLUMN_NAME).setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo(RANGESTOP_COLUMN_NAME).setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo(TARGETSTATE_COLUMN_NAME).setType(ColType.string).setVisible(false).setUpdates(true).setOptional().setInputType(InputType.BOOLEAN));
    table.addColumn(new ColInfo(ISDEPLOYED_COLUMN_NAME).setType(ColType.string).setVisible(true).setUpdates(false).setOptional().setInputType(InputType.IMAGE));
    table.addColumn(new ColInfo("UseForStatistics").setType(ColType.string).setVisible(true).setUpdates(true).setOptional().setInputType(InputType.BOOLEAN));
    table.addColumn(new ColInfo("Exclusions").setType(ColType.string).setVisible(true).setUpdates(true).setInputType(InputType.LONGTEXT).setInputFormat(InputFormat.IPv4EXCLUSIONS));
    table.addColumn(new ColInfo("MigrationState").setType(ColType.string).setVisible(false).setUpdates(true).setOptional());
    return table;

  }

}
