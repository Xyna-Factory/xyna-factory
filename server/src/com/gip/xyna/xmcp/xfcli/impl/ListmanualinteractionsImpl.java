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
import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listmanualinteractions;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;


public class ListmanualinteractionsImpl extends XynaCommandImplementation<Listmanualinteractions> {

  public void execute(OutputStream statusOutputStream, Listmanualinteractions payload) throws XynaException {
    Map<Long, ManualInteractionEntry> entries =
        factory.getXynaMultiChannelPortalPortal().listManualInteractionEntries(100);

    if (entries.size() > 0) {
      if (entries.size() == 1) {
        writeLineToCommandLine(statusOutputStream, "Found the following manual interaction entry:");
      } else if (entries.size() >= 100) {
        writeLineToCommandLine(statusOutputStream, "Found the following " + entries.size()
            + " manual interaction entries (there may be more entries):");
      } else {
        writeLineToCommandLine(statusOutputStream, "Found " + entries.size() + " manual interaction entries:");
      }
      for (ManualInteractionEntry entry : entries.values()) {

        String output =
            "### ID " + entry.getID() + " ### reason: '" + entry.getReason() + "', type: '" + entry.getType()
                + "', UserGroup: '" + entry.getUserGroup() + "', Todo: '" + entry.getTodo() + "' ### source: " 
                + entry.getWfTrace().getRootOrderType() + " responsetype: " + entry.getAllowedResponses()  + "\n";

        if (payload.getVerbose()) {
          output += "Workflow trace: \n";
          for (String stackTraceElement : entry.getWfTrace().getEntries()) {
            output += "      * " + stackTraceElement + "\n";
          }
          output += "\n";
        }
        writeToCommandLine(statusOutputStream, output);
      }
    } else {
      writeLineToCommandLine(statusOutputStream, "No manual interaction entries found.");
    }
  }

}
