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



import com.gip.xyna.XynaFactory;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.utils.exceptions.XynaException;
import java.io.OutputStream;
import com.gip.xyna.xmcp.xfcli.generated.Discoverabandonedorders;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrdersManagement.DiscoveredAbandonedOrdersBean;



public class DiscoverabandonedordersImpl extends XynaCommandImplementation<Discoverabandonedorders> {

  public void execute(OutputStream statusOutputStream, Discoverabandonedorders payload) throws XynaException {
    DiscoveredAbandonedOrdersBean discoveryResults =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getAbandonedOrdersManagement()
            .discoverAbandonedOrders(payload.getDeep());
    if (discoveryResults.getFoundCount() == 0) {
      writeLineToCommandLine(statusOutputStream, "Did not find any abandoned orders.");
    } else if (discoveryResults.getFoundCount() == 1) {
      writeLineToCommandLine(statusOutputStream, "Found 1 abandoned order.");
    } else {
      writeLineToCommandLine(statusOutputStream, "Found " + discoveryResults.getFoundCount() + " abandoned orders.");
    }
    
    if (discoveryResults.getErrorCount() > 1) {
      writeLineToCommandLine(statusOutputStream, "There have been " + discoveryResults.getErrorCount() + " errors during the detection.");
    } else if (discoveryResults.getErrorCount() == 1) {
      writeLineToCommandLine(statusOutputStream, "There has been one error during the detection.");
    }
  }

}
