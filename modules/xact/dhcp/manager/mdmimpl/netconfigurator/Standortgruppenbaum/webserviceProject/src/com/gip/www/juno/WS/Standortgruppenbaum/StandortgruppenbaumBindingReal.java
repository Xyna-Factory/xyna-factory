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


package com.gip.www.juno.WS.Standortgruppenbaum;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.juno.ws.db.tables.dhcp.CpednsHandler;
import com.gip.juno.ws.db.tables.dhcp.PoolHandler;
import com.gip.juno.ws.db.tables.dhcp.PooltypeHandler;
import com.gip.juno.ws.db.tables.dhcp.SharedNetworkHandler;
import com.gip.juno.ws.db.tables.dhcp.StandortHandler;
import com.gip.juno.ws.db.tables.dhcp.StandortgruppeHandler;
import com.gip.juno.ws.db.tables.dhcp.StaticHostHandler;
import com.gip.juno.ws.db.tables.dhcp.SubnetHandler;
import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.handler.AuthenticationTools;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.multiuser.MultiUserTools;
import com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype;
import com.gip.www.juno.WS.Standortgruppenbaum.Messages.*; 

public class StandortgruppenbaumBindingReal {
  

  static Logger logger = Logger.getLogger("Standortgruppenbaum");
  private final static LocationTreeSelector locationTreeSelector = new LocationTreeSelector(logger);

  private final static List<TableHandler> affectedTables = Arrays.asList(new TableHandler[] {new CpednsHandler(), 
                                                                                             new PoolHandler(),
                                                                                             new PooltypeHandler(),
                                                                                             new SharedNetworkHandler(),
                                                                                             new StandortgruppeHandler(),
                                                                                             new StandortHandler(),
                                                                                             new StaticHostHandler(),
                                                                                             new SubnetHandler()});
  
  
  private TableHandler staticHostHandler = new StaticHostHandler();
  
  public java.lang.String getTreeString(GetTreeStringRequest_ctype getTreeStringRequest) throws java.rmi.RemoteException {
    InputHeaderContent_ctype inputHeader = getTreeStringRequest.getInputHeader();
    //AuthenticationTools.authenticate(inputHeader.getUsername(), inputHeader.getPassword(), logger);
    //AuthenticationTools.checkPermissions(inputHeader.getUsername(), "standortgruppenbaum", "*", logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new AuthenticationTools.WebServiceInvocationIdentifier(null);
    AuthenticationTools.authenticateAndAuthorize(inputHeader.getUsername(), inputHeader.getPassword(),
                                                 "standortgruppenbaum", wsInvocationId, logger);
    SQLCommand retrievalUpdate = MultiUserTools.generateTableRetrievalTimestampUpdateForMultipleColumns(affectedTables, inputHeader.getUsername());
    DBCommands.executeDML(DBSchema.aaa, retrievalUpdate, logger);    
    List<LocationTreeQueryLine> queriedLines = locationTreeSelector.query();
    List<LocationTreeQueryLine> queriedUnpoolHosts = locationTreeSelector.queryUnpooledHosts();
    TreeStringBuilder builder = new TreeStringBuilder();
    String locationTreeString = builder.buildTreeStringWithUnpooledHosts(queriedLines, queriedUnpoolHosts);
    logger.info(locationTreeString);
    
    return locationTreeString;    
  }

  
}
