/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.ResumeResult;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;


/**
 *
 */
public class ResumeOrder implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {

    SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();

    ResumeTarget target = getResumeTarget(clw, allArgs);
    if( target != null ) {
      Pair<ResumeResult, String> pair = srm.resumeOrder(target);
      switch( pair.getFirst() ) {
        case Resumed:
          clw.writeLineToCommandLine( "Resumed "+ target );
          break;
        case Failed:
          clw.writeLineToCommandLine( "Resume "+ target + " failed: "+pair.getSecond());
          break;
       case Unresumeable:
          clw.writeLineToCommandLine( "Cannot resume "+ target + ": "+pair.getSecond());
          break;
        default:
          clw.writeLineToCommandLine( "Tried to resume "+ target + " and got unexpected Result "+pair.getFirst()+":"+pair.getSecond()+". See log for further information"); 
      }
    }
  }

  private ResumeTarget getResumeTarget(CommandLineWriter clw, AllArgs allArgs) {

    Long orderId;
    switch( allArgs.getArgCount() ) {
      case 1:
        if( allArgs.getArg(0).startsWith("ResumeTarget") ) {
          return ResumeTarget.valueOf(allArgs.getArg(0));
        }
        orderId = Long.parseLong(allArgs.getArg(0));
        return new ResumeTarget(orderId, orderId);
      case 2:
        try {
          return new ResumeTarget( Long.parseLong(allArgs.getArg(0)), Long.parseLong(allArgs.getArg(1)) );
        } catch( NumberFormatException e ) {
          orderId = Long.parseLong(allArgs.getArg(0));
          return new ResumeTarget( orderId, orderId, allArgs.getArg(1) );
        }
      case 3:
        return new ResumeTarget( Long.parseLong(allArgs.getArg(0)), Long.parseLong(allArgs.getArg(1)), allArgs.getArg(2) );
      default:
        clw.writeLineToCommandLine( "parameters are <rootId> [<orderId>] [<laneId>]\n" );
        return null ;
    }
  }

}
