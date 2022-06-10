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

package com.gip.juno.ws.db.tables.service;

import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.InputType;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.TableHandlerBasic;
import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBTableInfo;


public class Docsis_EncodingHandler extends TableHandlerBasic implements TableHandler {

  public Docsis_EncodingHandler() {
    super("Docsis_Encoding", true);
  }

  public DBTableInfo initDBTableInfo() {
    DBTableInfo table = new DBTableInfo("docsis_encoding", "service");

    table.addColumn(new ColInfo("Id").setGuiname("ID").setType(ColType.integer).setVisible(true).setPk().setAutoIncrement());
    table.addColumn(new ColInfo("Type_Encoding").setGuiname("TLV-Nummer").setType(ColType.integer).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("Parent_id").setGuiname("ID des Parent-TLV").setType(ColType.integer).setVisible(true).setUpdates(true).setOptional());
    table.addColumn(new ColInfo("Cmts_Mic_Order").setGuiname("Stelle in MIC").setType(ColType.integer).setVisible(true).setUpdates(true).setOptional());
    table.addColumn(new ColInfo("Type_Name").setGuiname("Name").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("Value_Data_Type_Name").setGuiname("Encoding-Typ").setType(ColType.string).setInputType(InputType.DROPDOWN).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("Value_Data_Type_Arguments").setGuiname("zusätzl. Argumente").setType(ColType.string).setVisible(true).setUpdates(true).setOptional());
    return table;
  }

}
