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
import com.gip.xyna.xmcp.xfcli.generated.Changeschedulingparameter;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.XynaScheduler.ChangeSchedulingParameterStatus;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;



public class ChangeschedulingparameterImpl extends XynaCommandImplementation<Changeschedulingparameter> {

  public void execute(OutputStream statusOutputStream, Changeschedulingparameter payload) throws XynaException {
    
    Long orderId = null;
    try {
      orderId = Long.valueOf(payload.getOrderId());
    } catch (NumberFormatException e) {
      writeLineToCommandLine(statusOutputStream, "Could not parse ID.");
      return;
    }
    
    TimeConstraint timeConstraint = TimeConstraint.valueOf( payload.getTimeconstraint() );
     //TODO weitere
    XynaScheduler xynaScheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler();
    
    ChangeSchedulingParameterStatus status = xynaScheduler.changeSchedulingParameter(orderId,timeConstraint );
        
    switch( status ) {
      case NotFound:
        writeLineToCommandLine(statusOutputStream, "Order "+orderId+" not found in scheduler.");
        break;
      case Unschedulable:
        writeLineToCommandLine(statusOutputStream, "Order "+orderId+" is too new or already scheduled. Try again.");
        break;
      case Success:
        writeLineToCommandLine(statusOutputStream, "Scheduling parameters changed.");
        break;
      default:
        writeLineToCommandLine(statusOutputStream, "Unexpected response "+status+".");
    }
  }
  
}
