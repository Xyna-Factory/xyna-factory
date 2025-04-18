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
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Undeploytrigger;



public class UndeploytriggerImpl extends XynaCommandImplementation<Undeploytrigger> {

  public void execute(OutputStream statusOutputStream, Undeploytrigger payload) throws XynaException {
    
    Long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                    .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    
    CommandControl.tryLock(CommandControl.Operation.TRIGGER_UNDEPLOY, revision);
    try {
      XynaFactory.getInstance().getActivation().getActivationTrigger().undeployTrigger(payload.getTriggerName(), payload.getTriggerInstanceName(), revision);
      
      StringBuilder sb = new StringBuilder();
      XynaFactory.getInstance().getActivation().getActivationTrigger().appendTriggerState(sb, payload.getTriggerName(), revision, payload.getVerbose());
      writeLineToCommandLine(statusOutputStream, sb);
    } finally {
      CommandControl.unlock(CommandControl.Operation.TRIGGER_UNDEPLOY, revision);
    }
  }

}
