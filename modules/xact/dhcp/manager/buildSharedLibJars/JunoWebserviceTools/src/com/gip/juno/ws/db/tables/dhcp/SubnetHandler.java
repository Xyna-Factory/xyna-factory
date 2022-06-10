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

import com.gip.juno.ws.tools.AdditionalCheck;
import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.IfNoVal;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.TableHandlerBasic;

public class SubnetHandler extends TableHandlerBasic implements TableHandler {
  
  public final static String SHAREDNETOWRKID_COLUMN_NAME = "SharedNetworkID";
  public final static String SUBNETID_COLUMN_NAME = "SubnetID";
  public final static String SUBNET_COLUMN_NAME = "Subnet";
  public final static String SUBNETMASK_COLUMN_NAME = "Mask";
  
  public SubnetHandler() {
    super("Subnet", true);
  }
  
  public DBTableInfo initDBTableInfo() {
    DBTableInfo table = new DBTableInfo("subnet", "dhcp");

    table.addColumn(new ColInfo(SUBNETID_COLUMN_NAME).setType(ColType.integer).setVisible(false).setPk().setAutoIncrement());
    table.addColumn(new ColInfo(SUBNET_COLUMN_NAME).setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo(SHAREDNETOWRKID_COLUMN_NAME).setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("SharedNetwork").setType(ColType.string).setVisible(true).setUpdates(true)
        .setParentTable("sharednetwork").setParentCol("SharedNetworkID").setLookupCol("sharedNetwork")
        .setVirtual().setDBName("SharedNetworkID"));
    table.addColumn(new ColInfo(SUBNETMASK_COLUMN_NAME).setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("FixedAttributes").setType(ColType.string).setVisible(true).setUpdates(true).setRemoveSpaces()
        .setAdditionalChecks(new AdditionalCheck[] {AdditionalCheck.checkFixedAttributeExistence}));
    table.addColumn(new ColInfo("Attributes").setType(ColType.string).setVisible(true).setUpdates(true)
                    .setIfNoVal(IfNoVal.Null).setRemoveSpaces().setCheckAttributeSyntax()
                    .setAdditionalChecks(new AdditionalCheck[] {AdditionalCheck.checkAttributeExistence}));
    table.addColumn(new ColInfo("MigrationState").setType(ColType.string).setVisible(false).setUpdates(true).setOptional());
    return table;

  }

}
