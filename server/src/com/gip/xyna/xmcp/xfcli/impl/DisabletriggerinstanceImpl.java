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
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Disabletriggerinstance;



public class DisabletriggerinstanceImpl extends XynaCommandImplementation<Disabletriggerinstance> {

  public void execute(OutputStream statusOutputStream, Disabletriggerinstance payload) throws XynaException {

    XynaActivationTrigger xynaActivationTrigger = XynaFactory.getInstance().getActivation().getActivationTrigger();

    long revision =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
            .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());

    CommandControl.tryLock(CommandControl.Operation.TRIGGER_INSTANCE_DISABLE, revision);
    try {
      if (xynaActivationTrigger.disableTriggerInstance(payload.getTriggerinstancename(), revision,
                                                       payload.getFilterInstanceDependencies())) {
        writeLineToCommandLine(statusOutputStream, "Successfully disabled the trigger instance.");

        StringBuilder sb = new StringBuilder();
        TriggerInstanceInformation ti =
            xynaActivationTrigger.getTriggerInstanceInformation(payload.getTriggerinstancename(), revision);
        xynaActivationTrigger.appendTriggerState(sb, ti.getTriggerName(), revision, payload.getVerbose());
        writeLineToCommandLine(statusOutputStream, sb);

      } else {
        writeLineToCommandLine(statusOutputStream, "Can not disable an already disabled triggerInstance.");
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.TRIGGER_INSTANCE_DISABLE, revision);
    }

  }

}