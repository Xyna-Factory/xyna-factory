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


package com.gip.juno.ws.db.tables.snmptrap;

import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.IfNoVal;
import com.gip.juno.ws.enums.InputFormat;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.TableHandlerBasic;

public class PoolUsagev6Handler extends TableHandlerBasic implements TableHandler {
  
  public PoolUsagev6Handler() {
    super("PoolUsagev6", false);
  }
  
  public static final String POOLID_COLUMN_NAME = "PoolID";
  public static final String USEDFRACTION_COLUMN_NAME = "UsedFraction";
  
  public DBTableInfo initDBTableInfo() {
    DBTableInfo table = new DBTableInfo("poolusage", "snmptrapv6");

    table.addColumn(new ColInfo(POOLID_COLUMN_NAME).setType(ColType.integer).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("SharedNetworkID").setType(ColType.integer).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("PoolTypeID").setType(ColType.integer).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("StandortGruppeID").setType(ColType.integer).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("Size").setType(ColType.integer).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("Used").setType(ColType.integer).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo(USEDFRACTION_COLUMN_NAME).setType(ColType.number).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("Prefixlength").setType(ColType.integer).setOptional().setVisible(true).setUpdates(true)
                    .setIfNoVal(IfNoVal.Null).setInputFormat(InputFormat.NUMBER));
    return table;

  }

}
