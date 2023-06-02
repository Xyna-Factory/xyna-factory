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
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listcapacities;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.capacities.CapacityUsageSlotInformation;



public class ListcapacitiesImpl extends XynaCommandImplementation<Listcapacities> {

  public void execute(OutputStream statusOutputStream, Listcapacities payload) throws XynaException {

    if (!payload.getVerbose()) {
      Collection<CapacityInformation> capacities = factory.getXynaMultiChannelPortalPortal().listCapacityInformation();

      TreeSet<String> sortedOutput = new TreeSet<String>();
      if (capacities.size() > 0) {
        writeLineToCommandLine(statusOutputStream, "Listing capacities...");
        String output = "";
        for (CapacityInformation ci : capacities) {
          output = "* Capacity '" + ci.getName() + "': ";
          output += "usage " + ci.getInuse() + "/" + ci.getCardinality() + ", ";
          output += "state " + ci.getState();
          sortedOutput.add(output);
        }
        for (String outputLine : sortedOutput) {
          writeLineToCommandLine(statusOutputStream, outputLine);
        }
      } else {
        writeLineToCommandLine(statusOutputStream, "Currently there are no capacities defined.");
      }
    } else {

      ExtendedCapacityUsageInformation extCapUsageInfo =
          factory.getXynaMultiChannelPortalPortal().listExtendedCapacityInformation();

      if (extCapUsageInfo.getSlotInformation().size() == 0) {
        writeLineToCommandLine(statusOutputStream, "Currently there are no capacities defined.");
        return;
      }

      writeLineToCommandLine(statusOutputStream, "Listing extended information on "
          + extCapUsageInfo.getSlotInformation().size() + " capacities:");

      TreeSet<String> sortedOutput = new TreeSet<String>();

      Iterator<HashSet<CapacityUsageSlotInformation>> iter = extCapUsageInfo.getSlotInformation().values().iterator();
      while (iter.hasNext()) {
        Iterator<CapacityUsageSlotInformation> iter2 = iter.next().iterator();
        while (iter2.hasNext()) {
          CapacityUsageSlotInformation next = iter2.next();
          StringBuilder nextLine = new StringBuilder();
          nextLine.append("[Capacity ").
            append(next.getPrivateCapacityIndex()).append(": ").append(next.getCapacityName());
          nextLine.append(", Slot ").
            append(getNumberWithLeadingZeroes(next.getSlotIndex() + 1, next.getMaxSlotIndex() + 1)).
            append("/").
            append(next.getMaxSlotIndex() + 1);
          if (next.getBinding() != 0) {
            nextLine.append(", Binding ").append(next.getBinding());
          }
          nextLine.append("] ");
          if (next.isOccupied()) {
            nextLine.append("Used by: [").
              append(next.getUsingOrderId()).
              append(": ").
              append(next.getUsingOrderType());
            if( next.isTransferable() ) {
              nextLine.append(", transferable");
            }
            nextLine.append("]");
          } else {
            if( next.getUsingOrderId() == ExtendedCapacityUsageInformation.ORDER_ID_FOR_DISABLED_CAPACITY) {
              nextLine.append("Disabled");
            } else if( next.getUsingOrderId() == ExtendedCapacityUsageInformation.ORDER_ID_FOR_RESERVED_CAPACITY) {
              nextLine.append("Reserved for binding ").append(next.getUsingOrderType());
            } else {
              nextLine.append("Unused");
            }
          }
          sortedOutput.add(nextLine.toString());
        }
      }
      for (String nextLine : sortedOutput) {
        writeLineToCommandLine(statusOutputStream, nextLine);
      }

    }

  }


  //this adds leading zeroes to get the correct order
  private String getNumberWithLeadingZeroes(int toBeAdjusted, int intToBeComparedTo) {
    int length = String.valueOf(intToBeComparedTo).length();
    String result = String.valueOf(toBeAdjusted);
    
    while ( result.length() < length ) {
      result = "0" + result;
    }
    
    return result;
  }

}
