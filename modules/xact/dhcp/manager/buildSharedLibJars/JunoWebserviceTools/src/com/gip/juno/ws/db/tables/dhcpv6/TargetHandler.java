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


package com.gip.juno.ws.db.tables.dhcpv6;

import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.IfNoVal;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.TableHandlerBasic;

public class TargetHandler extends TableHandlerBasic implements TableHandler {
  
  public TargetHandler() {
    super("Target", false);
  }
  
  public DBTableInfo initDBTableInfo() {
    DBTableInfo table = new DBTableInfo("target", "dhcpv6");

    table.addColumn(new ColInfo("TargetID").setType(ColType.integer).setVisible(false).setPk().setAutoIncrement());
    table.addColumn(new ColInfo("Name").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("DppFixedAttributeId").setType(ColType.integer).setVisible(false).setUpdates(false)
        .setIfNoVal(IfNoVal.Null));
    table.addColumn(new ColInfo("DppFixedAttribute").setType(ColType.string).setVisible(true).setUpdates(false)
        .setParentTable("dppfixedattribute").setParentCol("dppFixedAttributeID").setLookupCol("name")
        .setVirtual().setDBName("DppFixedAttributeId"));
    table.addColumn(new ColInfo("StandortGruppeID").setType(ColType.integer).setVisible(false).setUpdates(false)
        .setIfNoVal(IfNoVal.Null));
    table.addColumn(new ColInfo("StandortGruppe").setType(ColType.string).setVisible(true).setUpdates(false)
        .setParentTable("standortgruppe").setParentCol("standortGruppeID").setLookupCol("name")
        .setVirtual().setDBName("StandortGruppeID"));
    table.addColumn(new ColInfo("ConnectDataID").setType(ColType.integer).setVisible(false).setUpdates(true)
        .setIfNoVal(IfNoVal.Null));
    table.addColumn(new ColInfo("Clustergroup").setType(ColType.integer).setVisible(true).setUpdates(true)
        .setIfNoVal(IfNoVal.Null));
    table.addColumn(new ColInfo("Nodetype").setType(ColType.integer).setVisible(true).setUpdates(true)
        .setIfNoVal(IfNoVal.Null));
    table.addColumn(new ColInfo("Dienst").setType(ColType.string).setVisible(true).setUpdates(true)
        .setIfNoVal(IfNoVal.Null));
    table.addColumn(new ColInfo("Payload").setType(ColType.string).setVisible(true).setUpdates(true)
        .setIfNoVal(IfNoVal.Null));
    return table;
  }

}
