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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.util.Collections;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmStartParameter;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.InfrastructureAlgorithmExecutionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Startschedulerthread;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.XynaScheduler.SCHEDULERTHREAD_RESTART_OPTIONS;



public class StartschedulerthreadImpl extends XynaCommandImplementation<Startschedulerthread> {

  public void execute(OutputStream statusOutputStream, Startschedulerthread payload) throws XynaException {
    SCHEDULERTHREAD_RESTART_OPTIONS restartOption;
    if (payload.getCheck()) {
      restartOption = SCHEDULERTHREAD_RESTART_OPTIONS.CHECK;
    } else if (payload.getRepair()) {
      restartOption = SCHEDULERTHREAD_RESTART_OPTIONS.REPAIR;
    } else {
      restartOption = SCHEDULERTHREAD_RESTART_OPTIONS.NONE;
    }
    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    try {
      tm.startAlgorithm(XynaScheduler.SCHEDULER_ALGORITHM_THREAD_NAME,
                        new AlgorithmStartParameter(true, Collections.singletonList(XynaScheduler.actionParameter.toNamedParameterObject(restartOption))),
                        statusOutputStream);
    } catch (StringParameterParsingException e) {
      // parameter is used to build it's string representation
      throw new RuntimeException(e);
    }
  }

}
