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
import java.util.Arrays;
import java.util.Collections;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmStartParameter;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmStateChangeResult;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.InfrastructureAlgorithmExecutionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Startinfrastructurethread;



public class StartinfrastructurethreadImpl extends XynaCommandImplementation<Startinfrastructurethread> {

  public void execute(OutputStream statusOutputStream, Startinfrastructurethread payload) throws XynaException {
    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    try {
      AlgorithmStateChangeResult result = 
        tm.startAlgorithm(payload.getName(),
                          new AlgorithmStartParameter(payload.getAllowRestart(),
                                                      payload.getParameter() == null ? Collections.emptyList() : Arrays.asList(payload.getParameter())),
                          statusOutputStream);
      switch (result) {
        case ALREADY_IN_STATE :
          writeLineToCommandLine(statusOutputStream, "The infrastructure thread " + payload.getName() + " is already running and a restart is not allowed.");
          break;
        case FAILED :
          writeLineToCommandLine(statusOutputStream, "The infrastructure thread " + payload.getName() + " could not be started.");
          break;
        case NOT_REGISTERED :
          writeLineToCommandLine(statusOutputStream, "The infrastructure thread " + payload.getName() + " not registered.");
          break;
        case SUCCESS :
          writeLineToCommandLine(statusOutputStream, "The infrastructure thread " + payload.getName() + " has been started.");
          break;
        default :
          writeLineToCommandLine(statusOutputStream, "Unknown response: " + result);
          break;
      }
    } catch (StringParameterParsingException e) {
      throw new IllegalArgumentException(e);
    }
  }

}
