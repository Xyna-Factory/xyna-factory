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
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Getmonitoringlevel;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class GetmonitoringlevelImpl extends XynaCommandImplementation<Getmonitoringlevel> {

  public void execute(OutputStream statusOutputStream, Getmonitoringlevel payload) throws XynaException {
    
    // Prüfung, ob Application/Version existiert
    RuntimeContext runtimeContext = RevisionManagement.getRuntimeContext(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    if (payload.getOrderType() == null) {
      Map<DestinationKey, Integer> codes = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher().getAllMonitoringLevels();
      SortedSet<DestinationKey> sortedRelevantKeys = new TreeSet<DestinationKey>(new Comparator<DestinationKey>() {
        public int compare(DestinationKey o1, DestinationKey o2) {
          return o1.getOrderType().compareTo(o2.getOrderType());
        }
        
      });
      for (DestinationKey key : codes.keySet()) {
        if (key.getRuntimeContext().equals(runtimeContext)) {
          sortedRelevantKeys.add(key);
        }
      }
      writeLineToCommandLine(statusOutputStream, "Listing " + sortedRelevantKeys.size() + " configured monitoring codes for " + runtimeContext);
      String format = " %-" + 60 + "s %-" + 15 + "s";
      writeLineToCommandLine(statusOutputStream, String.format(format, "OrderType", "MonitoringLevel"));
      for (DestinationKey destinationKey : sortedRelevantKeys) {
        writeLineToCommandLine(statusOutputStream, String.format(format, destinationKey.getOrderType(), codes.get(destinationKey)));
      }
    } else {
      Integer code = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher()
          .getMonitoringLevel(new DestinationKey(payload.getOrderType(), runtimeContext));
      if (code != null) {
        writeLineToCommandLine(statusOutputStream, "Monitoring code for orderType ", payload.getOrderType(), ": " + code);
      } else {
        writeLineToCommandLine(statusOutputStream, "No monitoring code configured for orderType");
      }
    }
  }

}
