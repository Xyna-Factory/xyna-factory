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
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.InfrastructureAlgorithmExecutionManagement;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmStartParameter;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmStateChangeResult;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Startcronlikeschedulerthread;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeTimer;

// replaced by generic startinfrastructurethread
@Deprecated
public class StartcronlikeschedulerthreadImpl extends XynaCommandImplementation<Startcronlikeschedulerthread> {

  public void execute(OutputStream statusOutputStream, Startcronlikeschedulerthread payload) throws XynaException {
    writeLineToCommandLine(statusOutputStream, "Trying to restart CronLikeScheduler");
    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    AlgorithmStateChangeResult result;
    try {
      result = tm.startAlgorithm(CronLikeTimer.CRONLIKETIMER_THREAD_NAME, new AlgorithmStartParameter(false));
    } catch (StringParameterParsingException e) {
      throw new RuntimeException(e); // no parameters, should not happen
    }
    writeLineToCommandLine(statusOutputStream, "CronLikeTime start request result: " + result);
  }

}
