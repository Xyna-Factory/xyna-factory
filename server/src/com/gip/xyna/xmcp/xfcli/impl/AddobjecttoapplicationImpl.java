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
import java.io.PrintStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Addobjecttoapplication;



public class AddobjecttoapplicationImpl extends XynaCommandImplementation<Addobjecttoapplication> {

  public void execute(OutputStream statusOutputStream, Addobjecttoapplication payload) throws XynaException {

    ApplicationManagementImpl applicationManagement =
        (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long parentRevision = revisionManagement.getRevision(null, null, payload.getParentWorkspace());
    
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_ADDOBJECT, parentRevision);
    try {
      if (payload.getType() == null) {
        applicationManagement.addXMOMObjectToApplication(payload.getObjectName(), payload.getApplicationName(), parentRevision,
                                                         new SingleRepositoryEvent(parentRevision), payload.getVerbose(), new PrintStream(statusOutputStream));
      } else if (payload.getType().equalsIgnoreCase("trigger")) {
        applicationManagement.addTriggerToApplication(payload.getObjectName(), payload.getApplicationName(), parentRevision, payload.getVerbose(),
                                                              new PrintStream(statusOutputStream));
      } else if (payload.getType().equalsIgnoreCase("filter")) {
        applicationManagement.addFilterToApplication(payload.getObjectName(), payload.getApplicationName(), parentRevision, payload.getVerbose(),
                                                              new PrintStream(statusOutputStream));
      } else if (payload.getType().equalsIgnoreCase("triggerInstanceName")) {
        applicationManagement.addTriggerInstanceToApplication(payload.getObjectName(), payload.getApplicationName(), parentRevision, payload.getVerbose(),
                                                              new PrintStream(statusOutputStream));
      } else if (payload.getType().equalsIgnoreCase("filterInstanceName")) {
        applicationManagement.addFilterInstanceToApplication(payload.getObjectName(), payload.getApplicationName(), parentRevision, payload.getVerbose(),
                                                             new PrintStream(statusOutputStream));
      } else if (payload.getType().equalsIgnoreCase("xynaProperty")) {
        applicationManagement.addNonModelledObjectToApplication(payload.getObjectName(), payload.getApplicationName(), null,
                                                     ApplicationEntryType.XYNAPROPERTY, parentRevision, payload.getVerbose(),
                                                     new PrintStream(statusOutputStream));
      } else if (payload.getType().equalsIgnoreCase("capacityName")) {
        applicationManagement.addNonModelledObjectToApplication(payload.getObjectName(), payload.getApplicationName(), null,
                                                     ApplicationEntryType.CAPACITY, parentRevision, payload.getVerbose(),
                                                     new PrintStream(statusOutputStream));
      } else if (payload.getType().equalsIgnoreCase("ordertype")) {
        applicationManagement.addNonModelledObjectToApplication(payload.getObjectName(), payload.getApplicationName(), null,
                                                     ApplicationEntryType.ORDERTYPE, parentRevision, payload.getVerbose(),
                                                     new PrintStream(statusOutputStream));
      } else if (payload.getType().equalsIgnoreCase("formdefinition")) {
        applicationManagement.addNonModelledObjectToApplication(payload.getObjectName(), payload.getApplicationName(), null,
                                                     ApplicationEntryType.FORMDEFINITION, parentRevision, payload.getVerbose(),
                                                     new PrintStream(statusOutputStream));
      } else if (payload.getType().equalsIgnoreCase("orderinputsource")) {
        applicationManagement.addNonModelledObjectToApplication(payload.getObjectName(), payload.getApplicationName(), null,
                                                     ApplicationEntryType.ORDERINPUTSOURCE, parentRevision, payload.getVerbose(),
                                                     new PrintStream(statusOutputStream));
      } else {
        writeLineToCommandLine(statusOutputStream, "Unknown type");
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_ADDOBJECT, parentRevision);
    }

  }

}
