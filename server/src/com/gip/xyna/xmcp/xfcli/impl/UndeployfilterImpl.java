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
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Undeployfilter;



public class UndeployfilterImpl extends XynaCommandImplementation<Undeployfilter> {

  public void execute(OutputStream statusOutputStream, Undeployfilter payload) throws XynaException {
    
    Long revisionOfFilterInstance = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                    .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();

    CommandControl.tryLock(CommandControl.Operation.FILTER_UNDEPLOY, revisionOfFilterInstance);
    try {
      //TriggerInstanz f�r sp�tere Statusausgabe suchen
      FilterInstanceInformation filterInstance = xat.getFilterInstanceInformation(payload.getFilterInstanceName(), revisionOfFilterInstance);
      TriggerInstanceInformation triggerInstance = null;
      if (filterInstance != null) {
        triggerInstance = xat.getTriggerInstanceInformation(filterInstance.getTriggerInstanceName(), revisionOfFilterInstance, true);
      }

      //Undeployment
      xat.undeployFilter(payload.getFilterInstanceName(), revisionOfFilterInstance);
      
      //Statusausgabe
      if (triggerInstance != null) {
        StringBuilder sb = new StringBuilder();
        xat.appendTriggerState(sb, triggerInstance.getTriggerName(), revisionOfFilterInstance, payload.getVerbose());
        writeLineToCommandLine(statusOutputStream, sb);
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.FILTER_UNDEPLOY, revisionOfFilterInstance);
    }
  }

}
