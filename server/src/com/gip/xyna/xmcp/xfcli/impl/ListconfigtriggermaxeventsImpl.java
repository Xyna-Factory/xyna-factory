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
import java.util.Collections;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.ReceiveControlAlgorithm;
import com.gip.xyna.xact.trigger.TriggerInformation;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.TriggerInstanceIdentification;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listconfigtriggermaxevents;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class ListconfigtriggermaxeventsImpl extends XynaCommandImplementation<Listconfigtriggermaxevents> {

  public void execute(OutputStream statusOutputStream, Listconfigtriggermaxevents payload) throws XynaException {

    //TODO ändern in getTriggerInstanceStatistics() oder sowas?
    
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    boolean foundTriggers = true;
    
    List<Long> revisions = revisionManagement.getAllRevisions();
    Collections.sort(revisions);
    for(Long revision : revisions) {
      foundTriggers &= writeConfigurationForRevision(revision, statusOutputStream);
    }
    
    if(!foundTriggers) {
      writeLineToCommandLine(statusOutputStream, "No triggers registered at server");
    }

  }
  
  
  private boolean writeConfigurationForRevision(Long revision, OutputStream statusOutputStream) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY  {
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
   List<TriggerInformation> triggerInformation = xat.listTriggerInformation();

    if (triggerInformation == null || triggerInformation.size() == 0 ) {
      return false;
    }
    RuntimeContext runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
              .getRuntimeContext(revision);

    for (TriggerInformation ti : triggerInformation) {
      for (TriggerInstanceInformation tii : ti.getTriggerInstances()) {
        if (runtimeContext.equals(tii.getRuntimeContext())) {
          
          Long maxReceivesInParallel;
          Long currentActiveEvents;
          Boolean rejected;
          try {
            EventListener<?, ?> instance = xat.getTriggerInstance(new TriggerInstanceIdentification(tii.getTriggerName(), tii.getRevision(), tii.getTriggerInstanceName()));
            ReceiveControlAlgorithm rca = instance.getReceiveControlAlgorithm();
            maxReceivesInParallel = rca.getMaxReceivesInParallel();
            currentActiveEvents = rca.getCurrentActiveEvents();
            rejected = rca.isRejectRequestsAfterMaxReceives();
          } catch (XACT_TriggerNotFound e) {
            //disabled?
            Pair<Long, Boolean> triggerConfiguration = xat.getTriggerConfiguration(tii);
            maxReceivesInParallel = triggerConfiguration.getFirst();
            if (maxReceivesInParallel == null) {
              maxReceivesInParallel = -1L;
            }
            rejected = triggerConfiguration.getSecond();
            currentActiveEvents = null;
          }
          StringBuilder str = new StringBuilder(tii.getTriggerInstanceName());
          if (runtimeContext instanceof Application) {
            str.append(" (Applicationname: ").append(runtimeContext.getName()).append(", Versionname: ")
                .append(((Application) runtimeContext).getVersionName()).append(")");
          }
          if (runtimeContext instanceof Workspace && !runtimeContext.equals(RevisionManagement.DEFAULT_WORKSPACE)) {
            str.append(" (Workspacenname: ").append(runtimeContext.getName()).append(")");
          }
          str.append(": max events ");
          if (maxReceivesInParallel == -1) {
            str.append("not configured.");
          } else {
            str.append("= ").append(maxReceivesInParallel);
            if (currentActiveEvents != null) {
              str.append(", current active = ").append(currentActiveEvents);
            }
            str.append(" autoReject = ").append(rejected);
            if (currentActiveEvents == null) {
              str.append(", DISABLED");
            }
          }
          writeLineToCommandLine(statusOutputStream, str.toString());
        }
      }
    }

    return true;
  }

}
