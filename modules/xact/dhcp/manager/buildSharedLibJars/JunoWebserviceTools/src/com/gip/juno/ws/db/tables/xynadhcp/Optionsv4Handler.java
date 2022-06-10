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

package com.gip.juno.ws.db.tables.xynadhcp;
  
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.InputFormat;
import com.gip.juno.ws.enums.InputType;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.TableHandlerBasic;
import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBTableInfo;

public class Optionsv4Handler extends TableHandlerBasic implements TableHandler {
  
  public Optionsv4Handler() {
      super("Optionsv4", true);
    }

  
  public DBTableInfo initDBTableInfo() {
      DBTableInfo table = new DBTableInfo("optionsv4", "xynadhcp");

      table.addColumn(new ColInfo("Id").setGuiname("ID").setType(ColType.integer).setVisible(true).setPk().setUpdates(false).setAutoIncrement());
      table.addColumn(new ColInfo("ParentId").setGuiname("Parent-ID").setType(ColType.integer).setVisible(true).setUpdates(true).setOptional().setInputFormat(InputFormat.NUMBER));
      table.addColumn(new ColInfo("TypeName").setGuiname("Name").setType(ColType.string).setVisible(true).setUpdates(true));
      table.addColumn(new ColInfo("TypeEncoding").setGuiname("Nummer der Option").setType(ColType.number).setVisible(true).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("EnterpriseNr").setGuiname("Enterprise-Nummer").setType(ColType.number).setVisible(true).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("ValueDataTypeName").setGuiname("Encoding-Typ").setType(ColType.string).setVisible(true).setUpdates(true).setOptional().setInputType(InputType.DROPDOWN));
      table.addColumn(new ColInfo("ValueDataTypeArgumentsString").setGuiname("zusätzl. Argumente").setType(ColType.string).setVisible(true).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("ReadOnly").setType(ColType.string).setVisible(true).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("Status").setType(ColType.string).setVisible(false).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("GuiName").setGuiname("Kurzname der Option").setType(ColType.string).setVisible(true).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("GuiAttributeId").setType(ColType.integer).setVisible(false).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("GuiAttribute").setGuiname("Name in GUI für Optionen").setType(ColType.string).setVisible(true).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("GuiAttributeWerteBereich").setGuiname("Wertebereich in GUI für Optionen").setType(ColType.string).setVisible(true).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("GuiFixedAttributeId").setType(ColType.integer).setVisible(false).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("GuiFixedAttribute").setGuiname("Name in GUI für globale Optionen").setType(ColType.string).setVisible(true).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("GuiFixedAttributeValue").setGuiname("Value in GUI für globale Optionen").setType(ColType.string).setVisible(true).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("GuiParameterId").setType(ColType.integer).setVisible(false).setUpdates(true).setOptional());
      table.addColumn(new ColInfo("GuiParameter").setGuiname("Name in GUI für Conditions").setType(ColType.string).setVisible(true).setUpdates(true).setOptional());

      return table;
    }
}
