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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listwfs;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;



public class ListwfsImpl extends XynaCommandImplementation<Listwfs> {

  public void execute(OutputStream statusOutputStream, Listwfs payload) throws XynaException {
    
    Long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                    .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    
    Map<String, DeploymentStatus> deploymentStatusesUnsorted =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase().getAllDeploymentStatuses(revision).get(ApplicationEntryType.WORKFLOW);

    if (deploymentStatusesUnsorted == null) {
      writeLineToCommandLine(statusOutputStream, "No deployment status information available.");
      return;
    }

    SortedMap<String, DeploymentStatus> deploymentStatuses =
        new TreeMap<String, DeploymentStatus>(deploymentStatusesUnsorted);

    writeLineToCommandLine(statusOutputStream, "Listing deployment status information for " + deploymentStatuses.size()
        + " elements...");

    Iterator<Entry<String, DeploymentStatus>> iter = deploymentStatuses.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<String, DeploymentStatus> e = iter.next();
      String message = "\tName: " + e.getKey() + ", deployment status: " + e.getValue().toString();
      writeLineToCommandLine(statusOutputStream, message);
    }
  }

}
