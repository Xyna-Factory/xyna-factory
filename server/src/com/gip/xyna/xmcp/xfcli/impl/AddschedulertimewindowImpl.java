/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Addschedulertimewindow;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintManagement;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeWindowDefinition;



public class AddschedulertimewindowImpl extends XynaCommandImplementation<Addschedulertimewindow> {

  public void execute(OutputStream statusOutputStream, Addschedulertimewindow payload) throws XynaException {
    
    String[] definitions = payload.getDefinition();
    if( definitions.length == 0 ) {
      writeLineToCommandLine(statusOutputStream, "definition missing");
      return;
    }
   
    TimeConstraintWindowDefinition.Builder builder = TimeConstraintWindowDefinition.construct(payload.getName());
    builder.setDescription(payload.getComment());
    for( int i=0; i< definitions.length; ++i ) {
      builder.addTimeWindowDefinition( TimeWindowDefinition.valueOf( definitions[i] ) );
    }
    
    TimeConstraintManagement tcm = XynaFactory.getInstance().getProcessing().getXynaScheduler().getTimeConstraintManagement();
    tcm.addTimeWindow(builder.construct());
    
  }

}
