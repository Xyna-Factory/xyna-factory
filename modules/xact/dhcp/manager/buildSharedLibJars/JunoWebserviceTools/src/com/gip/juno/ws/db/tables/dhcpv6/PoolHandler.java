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
import com.gip.juno.ws.enums.InputFormat;
import com.gip.juno.ws.enums.InputType;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.TableHandlerBasic;

public class PoolHandler extends TableHandlerBasic implements TableHandler {
  
  public PoolHandler() {
    super("Pool", true);
  }
  
  public DBTableInfo initDBTableInfo() {
    DBTableInfo table = new DBTableInfo("pool", "dhcpv6");

    table.addColumn(new ColInfo("PoolID").setType(ColType.integer).setVisible(false).setPk().setAutoIncrement());
    table.addColumn(new ColInfo("Subnet").setType(ColType.string).setVisible(true).setUpdates(true)
        .setParentTable("Subnet").setParentCol("SubnetId").setLookupCol("Subnet")
        .setVirtual().setDBName("SubnetID"));
    table.addColumn(new ColInfo("SubnetID").setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("PoolTypeID").setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("PoolType").setType(ColType.string).setVisible(true).setUpdates(false)
        .setParentTable("pooltype").setParentCol("PoolTypeId").setLookupCol("Name")
        .setVirtual().setDBName("PoolTypeID"));
    table.addColumn(new ColInfo("RangeStart").setType(ColType.string).setVisible(true).setUpdates(true).setInputFormat(InputFormat.IPv6));
    table.addColumn(new ColInfo("RangeStop").setType(ColType.string).setVisible(true).setUpdates(true).setInputFormat(InputFormat.IPv6));
    table.addColumn(new ColInfo("Prefixlength").setType(ColType.string).setOptional().setVisible(true).setUpdates(true)
                    .setIfNoVal(IfNoVal.EmptyString).setInputFormat(InputFormat.NUMBER));
    table.addColumn(new ColInfo("TargetState").setType(ColType.string).setVisible(true).setUpdates(true).setOptional().setInputType(InputType.BOOLEAN));
    table.addColumn(new ColInfo("IsDeployed").setType(ColType.string).setVisible(true).setUpdates(false).setOptional().setInputType(InputType.IMAGE));
    table.addColumn(new ColInfo("UseForStatistics").setType(ColType.string).setVisible(false).setUpdates(true).setOptional().setInputType(InputType.BOOLEAN));
    table.addColumn(new ColInfo("Exclusions").setType(ColType.string).setVisible(true).setUpdates(true).setInputType(InputType.LONGTEXT).setInputFormat(InputFormat.IPv6EXCLUSIONS));
    return table;

  }

}
