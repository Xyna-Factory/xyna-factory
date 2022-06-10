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

public class SharedNetworkHandler extends TableHandlerBasic implements TableHandler {
  
  public final static String STANDORTID_COLUMN_NAME = "StandortID";
  public final static String SHAREDNETWORKID_COLUMN_NAME = "SharedNetworkID";
  
  public SharedNetworkHandler() {
    super("SharedNetwork", true);
  }
  
  public DBTableInfo initDBTableInfo() {
    DBTableInfo table = new DBTableInfo("sharednetwork", "dhcp");

    table.addColumn(new ColInfo(SHAREDNETWORKID_COLUMN_NAME).setType(ColType.integer).setVisible(false).setPk()
        .setAutoIncrement());
    table.addColumn(new ColInfo("SharedNetwork").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo(STANDORTID_COLUMN_NAME).setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("Standort").setType(ColType.string).setVisible(true).setUpdates(false)
        .setParentTable("standort").setParentCol("StandortID").setLookupCol("Name")
        .setVirtual().setDBName("StandortID"));
    table.addColumn(new ColInfo("CpeDnsID").setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("CpeDns").setType(ColType.string).setVisible(true).setUpdates(false)
        .setParentTable("cpedns").setParentCol("CpeDnsID").setLookupCol("CpeDns")
        .setVirtual().setDBName("CpeDnsID"));
    table.addColumn(new ColInfo("LinkAddresses").setType(ColType.string).setVisible(true).setUpdates(true)
                    .setInputType(InputType.LONGTEXT).setInputFormat(InputFormat.IPv4LIST));
    table.addColumn(new ColInfo("MigrationState").setType(ColType.string).setVisible(false).setUpdates(true).setOptional());
    return table;
  }

}
