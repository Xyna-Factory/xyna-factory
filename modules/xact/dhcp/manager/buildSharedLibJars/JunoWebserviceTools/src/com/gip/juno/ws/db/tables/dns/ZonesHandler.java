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


package com.gip.juno.ws.db.tables.dns;

import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.InputFormat;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.TableHandlerBasic;

public class ZonesHandler extends TableHandlerBasic implements TableHandler {

  public ZonesHandler() {
    super("Zones", true);
  }

  public DBTableInfo initDBTableInfo() {
    DBTableInfo table = new DBTableInfo("zones", "dns");

    table.addColumn(new ColInfo("Zone").setType(ColType.string).setVisible(true).setPk().setUpdates(false));
    table.addColumn(new ColInfo("Mx_Priority").setType(ColType.integer).setVisible(true).setUpdates(true).setOptional().setInputFormat(InputFormat.NUMBER));
    table.addColumn(new ColInfo("Primary_Ns").setType(ColType.string).setVisible(true).setUpdates(true).setOptional());
    table.addColumn(new ColInfo("Resp_Contact").setType(ColType.string).setVisible(true).setUpdates(true).setOptional());
    table.addColumn(new ColInfo("Serial").setType(ColType.integer).setVisible(true).setUpdates(true).setInputFormat(InputFormat.NUMBER));
    table.addColumn(new ColInfo("Refresh").setType(ColType.integer).setVisible(true).setUpdates(true).setInputFormat(InputFormat.NUMBER));
    table.addColumn(new ColInfo("Retry").setType(ColType.integer).setVisible(true).setUpdates(true).setInputFormat(InputFormat.NUMBER));
    table.addColumn(new ColInfo("Expire").setType(ColType.integer).setVisible(true).setUpdates(true).setInputFormat(InputFormat.NUMBER));
    table.addColumn(new ColInfo("Minimum").setType(ColType.integer).setVisible(true).setUpdates(true).setInputFormat(InputFormat.NUMBER));
    table.addColumn(new ColInfo("DppInstance").setType(ColType.string).setVisible(false).setUpdates(true));
    return table;
  }

}
