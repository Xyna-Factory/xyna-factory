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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listordertypescapacitystatus;
import com.gip.xyna.xprc.XynaProcessingPortal.DispatcherIdentification;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.CapacityInformation;



public class ListordertypescapacitystatusImpl extends XynaCommandImplementation<Listordertypescapacitystatus> {

  private static final String STATUS = " status:";
  
  @Override
  public void execute(OutputStream statusOutputStream, Listordertypescapacitystatus payload) throws XynaException {
    String output = "";
    String outputOrderType = "";

    boolean noCaps = false;
    Collection<CapacityInformation> capDetails = factory.getXynaMultiChannelPortalPortal().listCapacityInformation();
    if (capDetails == null || capDetails.size() == 0) {
      noCaps = true;
    }

    Map<DestinationKey, DestinationValue> destinations = XynaFactory.getPortalInstance().getProcessingPortal()
                    .getDestinations(DispatcherIdentification.Execution);

    writeLineToCommandLine(statusOutputStream, "Listing capacity status information for order types ...");

    // TODO this should be sorted but the code needs some cleanup to do that
    for (DestinationKey key : destinations.keySet()) {
      outputOrderType += "Order Type: " + key.getOrderType();
      if (key.getApplicationName() != null && key.getApplicationName().length() > 0) {
        outputOrderType += " App: " + key.getApplicationName() + ", Version: " + key.getVersionName();
      } else if (!key.getRuntimeContext().equals(RevisionManagement.DEFAULT_WORKSPACE)) {
        outputOrderType += " (" + key.getRuntimeContext() + ")";
      }
      
      outputOrderType += "," + STATUS + " ";
      List<Capacity> caps =
          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase()
              .getCapacities(key);
      if (caps == null || caps.size() == 0) {
        outputOrderType += "READY\n";
        output += outputOrderType;
        outputOrderType = "";
        continue;
      }
      if (noCaps) { // at this point caps is > 0
        outputOrderType += "NOT READY ";
      }
      capLoop : for (Capacity cap : caps) {
        if (noCaps) {
          outputOrderType += "(capacity '" + cap.getCapName() + "' is NONEXISTENT) ";
          continue;
        }
        if (capDetails != null) {
          for (CapacityInformation capInfo : capDetails) {
            if (cap.getCapName().equals(capInfo.getName())) {
              if (capInfo.getState() != com.gip.xyna.xprc.xsched.CapacityManagement.State.ACTIVE) {
                outputOrderType += "(capacity '" + cap.getCapName() + "' is DISABLED) ";
                continue capLoop;
              }
              else {
                continue capLoop;
              }
            }
          }
        }
        outputOrderType += "(capacity '" + cap.getCapName() + "' is NONEXISTENT) ";
      }
      if (!outputOrderType.contains("READY")) {
        int insertIndex = outputOrderType.lastIndexOf(STATUS) + STATUS.length();
        output += outputOrderType.substring(0, insertIndex);
        if (outputOrderType.contains("DISABLED") || outputOrderType.contains("NONEXISTENT")) {
          output += " NOT READY ";
          output += outputOrderType.substring(insertIndex + 1) + "\n";
        }
        else {
          output += " READY\n";
        }
      }
      else {
        output += outputOrderType + "\n";
      }
      outputOrderType = "";
    }
    writeToCommandLine(statusOutputStream, output);

  }

}
