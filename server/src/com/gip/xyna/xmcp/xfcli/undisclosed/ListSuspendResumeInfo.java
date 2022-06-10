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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;


public class ListSuspendResumeInfo implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
    
    SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
    if( allArgs.getArgCount() == 0 ) {
      Map<Long,String> map = srm.getRunningOrders();
      ArrayList<String> orders = new ArrayList<String>();
      for( Map.Entry<Long,String> entry : map.entrySet() ) {
        orders.add( entry.getKey()+": "+entry.getValue() );
      }
      if( orders.size() == 0 ) {
        clw.writeLineToCommandLine( "No informations found." );
      } else {
        Collections.sort(orders);
        for( String order : orders ) {
          clw.writeLineToCommandLine( order );
        }
      }
      Set<Long> unresumeableOrders = srm.getUnresumeableOrders();
      if( ! unresumeableOrders.isEmpty() ) {
        clw.writeLineToCommandLine( "orderIds locked: "+unresumeableOrders );
      }
      
    } else if( allArgs.getArgCount() == 2 ) {
      String command = allArgs.getArg(0).toLowerCase();
      Long id = null;
      if( "id".equals(command) ) {
        id = Long.valueOf(allArgs.getArg(1));
      } else {
        throw new RuntimeException("invalid parameter " + command + ". expected <id>.");
      }
      clw.writeLineToCommandLine( srm.getSRInformation(id) );
    } else {
      clw.writeLineToCommandLine("Usage: listsuspendresumeinfo [id <id>]");
    }
  }

}
