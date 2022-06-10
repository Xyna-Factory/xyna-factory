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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Resumescheduler;
import com.gip.xyna.xprc.xsched.XynaScheduler;



public class ResumeschedulerImpl extends XynaCommandImplementation<Resumescheduler> {

  public void execute(OutputStream statusOutputStream, Resumescheduler payload) throws XynaException {
    writeToCommandLine(statusOutputStream, "Resuming " + XynaScheduler.DEFAULT_NAME + "\n");
    boolean result = XynaFactory.getInstance().getProcessing().getXynaScheduler().resumeSchedulingManually();
    if (!result) {
      writeLineToCommandLine(statusOutputStream,"The Scheduler is currently paused by an internal application and will be resumed if the application is finished.");
    }
  }

}
