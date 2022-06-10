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

import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.IfNoVal;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.juno.ws.handler.PropagationHandler;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.TableHandlerBasic;

public class Mta_Sip_PortHandler extends TableHandlerBasic implements TableHandler {
  
  public Mta_Sip_PortHandler() {
    super("Mta_Sip_Port", LocationSchema.service, true);
  }
  
  public DBTableInfo initDBTableInfo() {
    final DBTableInfo table = new DBTableInfo("mta_sip_port", "service");

    table.addColumn(new ColInfo("Standort").setType(ColType.string).setVisible(false).setUpdates(false)
        .setVirtual());
    table.addColumn(new ColInfo("Mac").setType(ColType.binaryhex).setVisible(true).setPk());
    table.addColumn(new ColInfo("Port").setType(ColType.integer).setVisible(true).setPk());
    table.addColumn(new ColInfo("Dn").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("LocalNumber").setType(ColType.string).setVisible(true).setUpdates(true).setIfNoVal(IfNoVal.EmptyString).setDoTrim(true).setOptional());
    table.addColumn(new ColInfo("AreaCode").setType(ColType.string).setVisible(true).setUpdates(true).setIfNoVal(IfNoVal.EmptyString).setDoTrim(true).setOptional());
    table.addColumn(new ColInfo("Username").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("Password").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("RegistrarServer").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("OutboundProxy").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("Xml").setType(ColType.string).setVisible(true).setUpdates(true)
            .setIfNoVal(IfNoVal.EmptyString).setDoTrim(true).setOptional());
    table.addColumn(new ColInfo("ConfigDescr").setType(ColType.string).setVisible(true).setUpdates(true)
                    .setIfNoVal(IfNoVal.EmptyString).setDoTrim(true).setOptional());
    
    table.addPropagationHandler(new PropagationHandler() {
      
      private ColInfo propagationColumn = new ColInfo("propagationAction").setType(ColType.string).setUpdates(true).setPk();
      private ColInfo rollbackFailureColumn = new ColInfo("rollbackFailure").setType(ColType.string).setUpdates(true);
      
      @Override
      public String getPropagationActionColumnName() {
        return propagationColumn.name;
      }
      @Override
      public DBTableInfo getPropagationDBTableInfo() {
        DBTableInfo propagationTable = table.clone();
        propagationTable.setTablename("mta_sip_port_propagation");
        propagationTable.addColumn(propagationColumn);
        propagationTable.addColumn(rollbackFailureColumn);
        return propagationTable;
      }
      @Override
      public String getRollbackFailureColumnName() {
        return rollbackFailureColumn.name;
      }
    });
    
    return table;

  }

}
