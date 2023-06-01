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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import java.util.Collection;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationEntry;
import com.gip.xyna.xprc.xpce.WorkflowEngine;


/**
 *
 */
public class ListSynchronizationEntries implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) throws PersistenceLayerException {
    WorkflowEngine wfe = XynaFactory.getInstance().getProcessing().getWorkflowEngine();
    if (!(wfe instanceof XynaFractalWorkflowEngine)) {
      clw.writeLineToCommandLine("Command is not available for workflow engine '" + (wfe != null ? wfe.getClass().getName() : "<null>"));
      return;
    }

    XynaFractalWorkflowEngine xfractwfe = (XynaFractalWorkflowEngine) wfe;
    Collection<SynchronizationEntry> entries = xfractwfe.getSynchronizationManagement().listCurrentSynchronizationEntries();

    if (entries != null && entries.size() > 0) {
      clw.writeLineToCommandLine("Found the following " + (entries.size() > 1 ? entries.size() + " entries" : "entry")
                      + ":");
      for (SynchronizationEntry entry : entries) {
        StringBuffer sb = new StringBuffer();
        sb.append("\t* Correlation ID: " + entry.getCorrelationId());
        sb.append(", notified: " + entry.gotNotified());
        sb.append(", answer: " + entry.getAnswer());
        sb.append(", order id: " + entry.getOrderId());
        if (entry.getLaneId() != null) {
          sb.append(", lane id: " + entry.getLaneId());
        }
        clw.writeLineToCommandLine(sb.toString());
      }
    } else {
      clw.writeLineToCommandLine("No synchronization entries found.");
    }
    
  }

}
