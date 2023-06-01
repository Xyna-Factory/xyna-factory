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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listcapacitydependencies;
import com.gip.xyna.xprc.XynaProcessingPortal.DispatcherIdentification;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.planning.Capacity;



public class ListcapacitydependenciesImpl extends XynaCommandImplementation<Listcapacitydependencies> {

  @Override
  public void execute(OutputStream statusOutputStream, Listcapacitydependencies payload) throws XynaException {

    StringBuilder output = new StringBuilder();

    writeLineToCommandLine(statusOutputStream, "Listing capacities with requiring order types ...");

    Map<String, TreeMap<RuntimeContext, Set<DestinationKey>>> capToOrderTypeMap = new TreeMap<String, TreeMap<RuntimeContext, Set<DestinationKey>>>();

    Map<DestinationKey, DestinationValue> destinations =
        XynaFactory.getPortalInstance().getProcessingPortal().getDestinations(DispatcherIdentification.Execution);

    for (DestinationKey key : destinations.keySet()) {
      List<Capacity> caps =
          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase()
              .getCapacities(key);
      if (caps == null || caps.size() == 0) {
        continue;
      }
      for (Capacity cap : caps) {
        TreeMap<RuntimeContext, Set<DestinationKey>> runtimeContextToOrderTypeMap = capToOrderTypeMap.get(cap.getCapName());
        if (runtimeContextToOrderTypeMap == null) {
          runtimeContextToOrderTypeMap = new TreeMap<RuntimeContext, Set<DestinationKey>>();
          capToOrderTypeMap.put(cap.getCapName(), runtimeContextToOrderTypeMap);
        }
        Set<DestinationKey> ordertypesForRuntimeContext = runtimeContextToOrderTypeMap.get(key.getRuntimeContext());
        if(ordertypesForRuntimeContext == null) {
          ordertypesForRuntimeContext = new HashSet<DestinationKey>();
          runtimeContextToOrderTypeMap.put(key.getRuntimeContext(), ordertypesForRuntimeContext);
        }
        ordertypesForRuntimeContext.add(key);
      }
    }

    Set<String> capNames = capToOrderTypeMap.keySet();
    for (String cap : capNames) {
      output.append("Capacity '").append(cap).append("' is required for ordertypes:\n");
      Set<DestinationKey> ordertypes = capToOrderTypeMap.get(cap).get(RevisionManagement.DEFAULT_WORKSPACE);
      if(ordertypes != null) {
        output.append("  Default Workspace:\n");
        writeOrdertypes(output, ordertypes);
      }
      for(Map.Entry<RuntimeContext, Set<DestinationKey>> entrySet : capToOrderTypeMap.get(cap).entrySet()) {
        if(!RevisionManagement.DEFAULT_WORKSPACE.equals(entrySet.getKey())) {
          String runtimeContext = entrySet.getKey().toString();
          if (entrySet.getKey() instanceof Application) {
            runtimeContext = "Applicationname: " + entrySet.getKey().getName() + " Versionname: " + ((Application) entrySet.getKey()).getVersionName();
          }
          output.append("  ").append(runtimeContext).append(":\n");
          writeOrdertypes(output, entrySet.getValue());
        }
      }
    }

    if (output != null && output.length() != 0) {
      writeToCommandLine(statusOutputStream, output.toString());
    }
    else {
      writeLineToCommandLine(statusOutputStream, "No capacity allocation defined on server.");
    }

  }
  
  private void writeOrdertypes(StringBuilder output, Collection<DestinationKey> ordertypes) {
    Iterator<DestinationKey> iter = ordertypes.iterator();
    while(iter.hasNext()) {
      DestinationKey key = iter.next();
      output.append("    ").append(key.getOrderType()).append("\n");      
    }
  } 

}
