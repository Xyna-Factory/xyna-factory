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
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listcapacitiesforordertype;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;



public class ListcapacitiesforordertypeImpl extends XynaCommandImplementation<Listcapacitiesforordertype> {

  @Override
  public void execute(OutputStream statusOutputStream, Listcapacitiesforordertype payload) throws XynaException {
    String orderType = payload.getOrderType();

    // Prüfung, ob Application/Version bzw. Workspace existiert
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    
    RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(revision);
    
    List<Capacity> capacities =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase()
            .getCapacities(new DestinationKey(orderType, runtimeContext));
    
    if (capacities == null || capacities.size() == 0) {
       writeLineToCommandLine(statusOutputStream, "Currently no capacities are required to execute order type '"
          + orderType + "'.");
    } else {
      writeLineToCommandLine(statusOutputStream, "Listing required capacities for order type '" + orderType + "'");
      String output = "";
      for (Capacity cap : capacities) {
        output = " * Name: '" + cap.getCapName() + "', cardinality: " + cap.getCardinality();
        writeLineToCommandLine(statusOutputStream, output);
      }
    }
  }

}
