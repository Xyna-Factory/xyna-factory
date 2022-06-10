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
import com.gip.juno.ws.tools.ColInfo.LOOKUP_ON;
import com.gip.juno.ws.tools.ColInfo.LookupQuery;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.IfNoVal;
import com.gip.juno.ws.enums.InputFormat;
import com.gip.juno.ws.enums.InputType;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.handler.TableHandlerBasic;

public class StaticHostHandler extends TableHandlerBasic implements TableHandler {

  public static final String DNS_COLNAME = "Dns";  
  public static final String STATICHOSTID_COLNAME = "StaticHostID";
  public static final String SUBNETID_COLNAME = "SubnetID";
  public static final String ASSIGNEDPOOLID_COLNAME = "AssignedPoolID";
  public static final String DESIREDPOOLTYPE_COLNAME = "DesiredPoolType";
  public final static String CMTSIP_COLUMN_NAME = "Cmtsip";
  public final static String IP_COLUMN_NAME = "Ip";
  
  public final static LookupQuery cmtsIpLookupByAssignedPoolIdQuery = new LookupQuery(new SQLCommand("SELECT linkAddresses FROM dhcp.sharednetwork WHERE sharedNetworkID = (SELECT sharedNetworkID FROM dhcp.subnet WHERE subnetID = (SELECT subnetID FROM dhcp.pool WHERE poolID = ?))"),
                                                                                       new String[] {ASSIGNEDPOOLID_COLNAME}, IfNoVal.ConstraintViolation, LOOKUP_ON.INSERTION, LOOKUP_ON.MODIFICATION);
  
  public StaticHostHandler() {
    super("StaticHost", true);
  }
  
  public DBTableInfo initDBTableInfo() {
    DBTableInfo table = new DBTableInfo("statichost", "dhcp");

    table.addColumn(new ColInfo(STATICHOSTID_COLNAME).setType(ColType.integer).setVisible(false).setPk().setAutoIncrement());
    table.addColumn(new ColInfo("Cpe_mac").setGuiname("Mac").setType(ColType.string).setVisible(true).setUpdates(true)
                    .setCheckUnique().setInputFormat(InputFormat.MAC));
    table.addColumn(new ColInfo("Hostname").setType(ColType.string).setVisible(true).setUpdates(true));
    table.addColumn(new ColInfo("RemoteId").setType(ColType.string).setVisible(true).setUpdates(true).setInputFormat(InputFormat.MAC));
    table.addColumn(new ColInfo(IP_COLUMN_NAME).setType(ColType.string).setVisible(true).setUpdates(true).setInputFormat(InputFormat.IPv4));
    table.addColumn(new ColInfo(ASSIGNEDPOOLID_COLNAME).setType(ColType.integer).setVisible(false).setUpdates(true));
    table.addColumn(new ColInfo(DESIREDPOOLTYPE_COLNAME).setType(ColType.integer).setVisible(false).setUpdates(false).setOptional());
    table.addColumn(new ColInfo("SubnetID").setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("Subnet").setType(ColType.string).setVisible(false).setUpdates(false)
        .setParentTable("subnet").setParentCol("SubnetID").setLookupCol("subnet")
        .setVirtual().setDBName(SUBNETID_COLNAME));
    table.addColumn(new ColInfo(DNS_COLNAME).setType(ColType.string).setVisible(false).setUpdates(true).setOptional());
    table.addColumn(new ColInfo("DynamicDnsActive").setGuiname("DNS Eintrag").setType(ColType.string).setVisible(false).setUpdates(true).setInputType(InputType.BOOLEAN).setOptional());
    table.addColumn(new ColInfo("Deployed1").setGuiname("Deployed").setType(ColType.string).setVisible(true).setUpdates(false).setInputType(InputType.IMAGE));
    table.addColumn(new ColInfo("Deployed2").setType(ColType.string).setVisible(false).setUpdates(false).setInputType(InputType.IMAGE));
    table.addColumn(new ColInfo("ConfigDescr").setType(ColType.string).setOptional().setVisible(true).setUpdates(true).setInputType(InputType.LONGTEXT));
    table.addColumn(new ColInfo(CMTSIP_COLUMN_NAME).setType(ColType.string).setVisible(false).setOptional().setUpdates(true)
                    .setLookupQuery(cmtsIpLookupByAssignedPoolIdQuery));
    return table;

  }

}
