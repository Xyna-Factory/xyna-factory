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
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintManagement;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintManagementInformation;


/**
 *
 */
public class ListTimeConstraintManagementInfo implements CommandExecution {

   public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
     TimeConstraintManagement tcm = XynaFactory.getInstance().getProcessing().getXynaScheduler().getTimeConstraintManagement();
     
     TimeConstraintManagementInformation tcmi = tcm.getTimeConstraintManagementInformation();
     
     clw.writeString("waiting for start time: "+tcmi.numStartTime+"\n");
     clw.writeString("checking scheduling timeout: "+tcmi.numSchedulingTimeout+"\n");
     clw.writeString("total tasks: "+tcmi.numTimedTasks+"\n");
     clw.writeString("TimeWindows:\n");
     for( String tw : tcmi.timeWindows ) {
       clw.writeString("  "+tw+"\n");
     }
     
  }

  
  
  
}
