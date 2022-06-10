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

public class CmHandler extends TableHandlerBasic implements TableHandler {
  
  public CmHandler() {
    super("Cm", LocationSchema.service, true);
  }

  public DBTableInfo initDBTableInfo() {
    final DBTableInfo table = new DBTableInfo("cm", "service");

    table.addColumn(new ColInfo("Standort").setType(ColType.string).setVisible(false).setUpdates(false)
        .setVirtual());
    table.addColumn(new ColInfo("Mac").setType(ColType.binaryhex).setVisible(true).setPk().setInputType("mac"));
    table.addColumn(new ColInfo("Mode").setType(ColType.string).setVisible(true).setUpdates(true).setDefaultValue("NORMAL"));
    table.addColumn(new ColInfo("IpMode").setType(ColType.string).setVisible(true).setUpdates(true)
                    .setIfNoVal(IfNoVal.EmptyString).setDoTrim(true).setOptional());
    table.addColumn(new ColInfo("Ds").setType(ColType.integer).setVisible(true).setUpdates(true).setDefaultValue("0"));
    table.addColumn(new ColInfo("Us").setType(ColType.integer).setVisible(true).setUpdates(true).setDefaultValue("0"));
    table.addColumn(new ColInfo("NumberOfCpes").setType(ColType.integer).setVisible(true).setUpdates(true).setDefaultValue("2"));
    table.addColumn(new ColInfo("MtaEnable").setType(ColType.string).setVisible(true).setUpdates(true)
                    .setIfNoVal(IfNoVal.EmptyString).setDoTrim(true).setOptional());
    table.addColumn(new ColInfo("CpeIps").setType(ColType.string).setVisible(true).setUpdates(false)
            .setIfNoVal(IfNoVal.EmptyString).setDoTrim(true).setOptional());
    table.addColumn(new ColInfo("CpeIpsv6").setType(ColType.string).setVisible(true).setUpdates(false)
                    .setIfNoVal(IfNoVal.EmptyString).setDoTrim(true).setOptional());
    table.addColumn(new ColInfo("Xml").setType(ColType.string).setVisible(true).setUpdates(true)
                    .setIfNoVal(IfNoVal.EmptyString).setDoTrim(true).setOptional());
    table.addColumn(new ColInfo("ConfigFile").setType(ColType.string).setVisible(true).setUpdates(true)
        .setIfNoVal(IfNoVal.Null).setDoTrim(true).setOptional());
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
        propagationTable.setTablename("cm_propagation");
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
