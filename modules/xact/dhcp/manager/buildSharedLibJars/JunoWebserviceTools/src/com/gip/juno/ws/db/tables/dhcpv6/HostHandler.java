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
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.ColInfo.LOOKUP_ON;
import com.gip.juno.ws.tools.ColInfo.LookupQuery;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.IfNoVal;
import com.gip.juno.ws.enums.InputFormat;
import com.gip.juno.ws.enums.InputType;
import com.gip.juno.ws.handler.TableHandlerBasic;

public class HostHandler extends TableHandlerBasic {

  public final static String CMTSIP_COLUMN_NAME = "Cmtsip";
  public final static String ASSIGNED_POOL_ID_COLUMN_NAME = "AssignedPoolID";
  public static final LookupQuery CMTSIP_LOOKUP_BY_ASSIGNEDPOOLID_QUERY = new LookupQuery(new SQLCommand("SELECT linkAddresses FROM dhcpv6.sharednetwork WHERE sharedNetworkID = (SELECT sharedNetworkID FROM dhcpv6.subnet WHERE subnetID = (SELECT subnetID FROM dhcpv6.pool WHERE poolID = ?))"),
                                                                                          new String[] {ASSIGNED_POOL_ID_COLUMN_NAME}, IfNoVal.ConstraintViolation, LOOKUP_ON.INSERTION, LOOKUP_ON.MODIFICATION);
  
  public HostHandler() {
    super("Host", true);
  }
 
  public DBTableInfo initDBTableInfo() {
    DBTableInfo table = new DBTableInfo("host", "dhcpv6");
    
    table.addColumn(new ColInfo("HostID").setType(ColType.integer).setVisible(false).setUpdates(false).setPk().setAutoIncrement());
    table.addColumn(new ColInfo("Mac").setType(ColType.string).setVisible(true).setUpdates(true).setInputFormat(InputFormat.MAC));
    table.addColumn(new ColInfo("HostName").setType(ColType.string).setVisible(true).setUpdates(true).setIfNoVal(IfNoVal.EmptyString).setInputFormat(InputFormat.NO_SPECIAL));
    table.addColumn(new ColInfo("AgentRemoteId").setType(ColType.string).setVisible(true).setUpdates(true).setInputFormat(InputFormat.MAC));
    table.addColumn(new ColInfo("AssignedIp").setType(ColType.string).setVisible(true).setUpdates(true).setInputFormat(InputFormat.IPv6));
    table.addColumn(new ColInfo("Prefixlength").setType(ColType.string).setVisible(true).setOptional().setUpdates(true).setIfNoVal(IfNoVal.EmptyString).setInputFormat(InputFormat.NUMBER));
    table.addColumn(new ColInfo(ASSIGNED_POOL_ID_COLUMN_NAME).setType(ColType.integer).setVisible(false).setUpdates(true));
    table.addColumn(new ColInfo("Pool").setType(ColType.string).setVisible(true).setUpdates(true)
                    .setParentTable("pool").setParentCol("PoolID").setLookupCol("rangeStart")
                    .setVirtual().setDBName(ASSIGNED_POOL_ID_COLUMN_NAME).setInputType(InputType.DROPDOWN));
    table.addColumn(new ColInfo("SubnetOfPool").setType(ColType.integer).setVisible(false).setUpdates(false));
    /*table.addColumn(new ColInfo("Subnet").setType(ColType.string).setVisible(true).setUpdates(true)
                    .setParentTable("subnet").setParentCol("SubnetID").setLookupCol("subnet")
                    .setVirtual().setDBName("SubnetOfPool"));*/
    table.addColumn(new ColInfo("DesiredPoolType").setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("PoolType").setType(ColType.integer).setVisible(true).setUpdates(true)
                    .setParentTable("pooltype").setParentCol("PoolTypeID").setLookupCol("name")
                    .setVirtual().setDBName("DesiredPoolType").setInputType(InputType.DROPDOWN));
    table.addColumn(new ColInfo("DynamicDnsActive").setGuiname("DNS Eintrag").setType(ColType.string).setVisible(false).setUpdates(true).setInputType(InputType.BOOLEAN).setOptional());
    table.addColumn(new ColInfo("DeploymentState").setType(ColType.string).setVisible(true).setUpdates(true).setOptional().setInputType(InputType.IMAGE)); //updates=true because of ReadOnly-InputType.IMAGE
    table.addColumn(new ColInfo("ConfigDescr").setType(ColType.string).setVisible(true).setOptional().setUpdates(true).setInputType(InputType.LONGTEXT));
    table.addColumn(new ColInfo(CMTSIP_COLUMN_NAME).setType(ColType.string).setVisible(false).setOptional().setUpdates(true)
                    .setLookupQuery(CMTSIP_LOOKUP_BY_ASSIGNEDPOOLID_QUERY));
    return table;

  }

}
