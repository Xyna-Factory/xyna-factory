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
import com.gip.juno.ws.enums.IfNoVal;
import com.gip.juno.ws.enums.InputType;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.TableHandlerBasic;

public class ConditionHandler extends TableHandlerBasic implements TableHandler {
  
  public ConditionHandler() {
    super("Condition", true);
  }
  
  public DBTableInfo initDBTableInfo() {
    DBTableInfo table = new DBTableInfo("`condition`", "dhcp");

    table.addColumn(new ColInfo("ConditionID").setType(ColType.integer).setVisible(false).setPk()
        .setAutoIncrement());
    table.addColumn(new ColInfo("Parameter").setType(ColType.string).setVisible(true).setUpdates(true)
        .setIfNoVal(IfNoVal.ConstraintViolation).setInputType(InputType.DROPDOWN));
    table.addColumn(new ColInfo("Operator").setType(ColType.string).setVisible(true).setUpdates(true)
        .setIfNoVal(IfNoVal.ConstraintViolation).setInputType(InputType.DROPDOWN));
    table.addColumn(new ColInfo("Value").setType(ColType.string).setVisible(true).setUpdates(true)
        .setIfNoVal(IfNoVal.EmptyString));
    table.addColumn(new ColInfo("Name").setType(ColType.string).setVisible(true).setUpdates(true)
        .setIfNoVal(IfNoVal.ConstraintViolation));
    return table;
  }

}
