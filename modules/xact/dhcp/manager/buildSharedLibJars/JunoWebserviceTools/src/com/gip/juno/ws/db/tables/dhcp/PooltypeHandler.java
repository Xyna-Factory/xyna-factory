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
import com.gip.juno.ws.enums.InputType;
import com.gip.juno.ws.enums.LookupStyle;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.TableHandlerBasic;

public class PooltypeHandler extends TableHandlerBasic implements TableHandler {
  
  public PooltypeHandler() {
    super("Pooltype", true);
  }
  
  public DBTableInfo initDBTableInfo() {
    DBTableInfo table = new DBTableInfo("pooltype", "dhcp");

    table.addColumn(new ColInfo("PoolTypeID").setType(ColType.integer).setVisible(false).setPk().setAutoIncrement());
    table.addColumn(new ColInfo("Name").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("Negation").setType(ColType.string).setVisible(true).setUpdates(true).setInputType(InputType.DROPDOWN));
    table.addColumn(new ColInfo("Classes").setType(ColType.string).setVisible(true).setUpdates(false)
        .setParentTable("class").setParentCol("ClassID").setLookupCol("Name").setLookupStyle(LookupStyle.csv)
        .setDBName("ClassIDs").setVirtual());
    table.addColumn(new ColInfo("ClassIDs").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("Attributes").setType(ColType.string).setVisible(true).setUpdates(true)
        .setIfNoVal(IfNoVal.Null).setRemoveSpaces().setCheckAttributeSyntax()
        .setAdditionalChecks(new AdditionalCheck[] {AdditionalCheck.checkAttributeExistence}));
    table.addColumn(new ColInfo("FixedAttributes").setType(ColType.string).setVisible(true).setUpdates(true)
        .setIfNoVal(IfNoVal.Null).setRemoveSpaces()
        .setAdditionalChecks(new AdditionalCheck[] {AdditionalCheck.checkFixedAttributeExistence}));
    table.addColumn(new ColInfo("IsDefault").setType(ColType.string).setVisible(true).setUpdates(true).setInputType(InputType.BOOLEAN));
    return table;

  }

}
