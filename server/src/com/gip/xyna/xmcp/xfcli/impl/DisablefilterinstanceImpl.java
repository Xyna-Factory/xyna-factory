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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.utils.exceptions.XynaException;
import java.io.OutputStream;
import com.gip.xyna.xmcp.xfcli.generated.Disablefilterinstance;



public class DisablefilterinstanceImpl extends XynaCommandImplementation<Disablefilterinstance> {

  public void execute(OutputStream statusOutputStream, Disablefilterinstance payload) throws XynaException {
    long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                    .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();

    CommandControl.tryLock(CommandControl.Operation.FILTER_INSTANCE_DISABLE, revision);
    try {
      if (xat.disableFilterInstance(payload.getFilterinstancename(), revision)) {
        writeLineToCommandLine(statusOutputStream, "Successfully disabled the filter instance.");
        
        FilterInstanceInformation filterInstance =
            xat.getFilterInstanceInformation(payload.getFilterinstancename(), revision);
        TriggerInstanceInformation triggerInstance =
            xat.getTriggerInstanceInformation(filterInstance.getTriggerInstanceName(), revision, true);
        if (payload.getVerbose()) {
          if (triggerInstance != null) {
            StringBuilder sb = new StringBuilder();
            xat.appendTriggerState(sb, triggerInstance.getTriggerName(), revision, payload.getVerbose());
            writeLineToCommandLine(statusOutputStream, sb);
          } else {
            writeLineToCommandLine(statusOutputStream, "Trigger instance " + filterInstance.getTriggerInstanceName() + " not found.");
          }
        }
      } else {
        writeLineToCommandLine(statusOutputStream, "Can not disable an already disabled filterInstance.");
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.FILTER_INSTANCE_DISABLE, revision);
    }
  }

}
