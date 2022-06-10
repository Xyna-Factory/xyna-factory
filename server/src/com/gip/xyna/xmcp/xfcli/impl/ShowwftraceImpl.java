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
import java.util.ArrayList;
import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Showwftrace;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;



public class ShowwftraceImpl extends XynaCommandImplementation<Showwftrace> {

  public void execute(OutputStream statusOutputStream, Showwftrace payload) throws XynaException {
    Long id;
    try {
      id = new Long(payload.getId());
    } catch (NumberFormatException nfe) {
      writeToCommandLine(statusOutputStream, "Invalid ID (" + payload.getId() + ").");
      return;
    }

    // TODO support more than just the manual interaction entries

    Map<Long, ManualInteractionEntry> entries =
        factory.getXynaMultiChannelPortalPortal().listManualInteractionEntries();

    if (entries.containsKey(id)) {
      ManualInteractionEntry entry = entries.get(id);
      writeLineToCommandLine(statusOutputStream, "Found workflow trace for ID " + id + ":");

      ArrayList<String> wfTrace = entry.getWfTrace().getEntries();
      for (String traceElement : wfTrace) {
        writeLineToCommandLine(statusOutputStream, "### " + traceElement + ".");
      }

    } else {
      writeToCommandLine(statusOutputStream, "No entry found for ID " + id + ".");
    }
  }

}
