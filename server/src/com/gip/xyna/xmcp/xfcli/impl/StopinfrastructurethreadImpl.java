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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmStateChangeResult;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.InfrastructureAlgorithmExecutionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Stopinfrastructurethread;



public class StopinfrastructurethreadImpl extends XynaCommandImplementation<Stopinfrastructurethread> {

  public void execute(OutputStream statusOutputStream, Stopinfrastructurethread payload) throws XynaException {
    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    AlgorithmStateChangeResult result = tm.stopAlgorithm(payload.getName());
    switch (result) {
      case ALREADY_IN_STATE :
        writeLineToCommandLine(statusOutputStream, "The infrastructure thread " + payload.getName() + " is already stopped.");
        break;
      case FAILED :
        writeLineToCommandLine(statusOutputStream, "The infrastructure thread " + payload.getName() + " could not be stopped.");
        break;
      case NOT_REGISTERED :
        writeLineToCommandLine(statusOutputStream, "The infrastructure thread " + payload.getName() + " not registered.");
        break;
      case SUCCESS :
        writeLineToCommandLine(statusOutputStream, "The infrastructure thread " + payload.getName() + " has been stopped.");
        break;
      default :
        writeLineToCommandLine(statusOutputStream, "Unknown response: " + result);
        break;
    }
  }

}
