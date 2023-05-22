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
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNeedsEnabledTriggerException;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable.TriggerInstanceState;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Enabletriggerinstance;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class EnabletriggerinstanceImpl extends XynaCommandImplementation<Enabletriggerinstance> {

  public void execute(OutputStream statusOutputStream, Enabletriggerinstance payload) throws XynaException {

    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RuntimeContext runtimeContext = RevisionManagement.getRuntimeContext(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    long revision = revisionManagement.getRevision(runtimeContext);

    XynaActivationTrigger xynaActivationTrigger = XynaFactory.getInstance().getActivation().getActivationTrigger();

    int processingLimit = -1;
    if (payload.getOrderentrancelimit() != null) {
      processingLimit = Integer.parseInt(payload.getOrderentrancelimit());
    }

    CommandControl.tryLock(CommandControl.Operation.TRIGGER_INSTANCE_ENABLE, revision);
    try {
      if (xynaActivationTrigger.enableTriggerInstance(payload.getTriggerinstancename(), revision, true,
                                                      processingLimit,  payload.getFilterInstanceDependencies())) {
        writeLineToCommandLine(statusOutputStream, "Successfully enabled the trigger instance.");
        appendState(payload.getTriggerinstancename(), revision, statusOutputStream, payload.getVerbose());
      } else {
        writeLineToCommandLine(statusOutputStream, "Can not enable an already enabled triggerInstance.");
      }
    } catch (XACT_TriggerInstanceNeedsEnabledTriggerException e) {
      //wenn der Trigger nicht richtig geadded werden konnte, dann nur den Status auf ENABLED setzen
      xynaActivationTrigger.setTriggerInstanceState(payload.getTriggerinstancename(), revision, TriggerInstanceState.ENABLED);
      writeLineToCommandLine(statusOutputStream, "Successfully enabled the trigger instance.");
      if (payload.getVerbose()) {
        appendState(payload.getTriggerinstancename(), revision, statusOutputStream, payload.getVerbose());
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.TRIGGER_INSTANCE_ENABLE, revision);
    }

  }


  private void appendState(String triggerInstanceName, Long revision, OutputStream statusOutputStream, boolean verbose)
      throws PersistenceLayerException, XACT_TriggerNotFound {
    XynaActivationTrigger xynaActivationTrigger = XynaFactory.getInstance().getActivation().getActivationTrigger();
    TriggerInstanceInformation ti = xynaActivationTrigger.getTriggerInstanceInformation(triggerInstanceName, revision);
    StringBuilder sb = new StringBuilder();
    xynaActivationTrigger.appendTriggerState(sb, ti.getTriggerName(), revision, verbose);
    writeLineToCommandLine(statusOutputStream, sb);
  }

}
