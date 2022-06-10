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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.priority.PrioritySetting;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listpriorities;



public class ListprioritiesImpl extends XynaCommandImplementation<Listpriorities> {

  public void execute(OutputStream statusOutputStream, Listpriorities payload) throws XynaException {
    StringBuilder output = new StringBuilder();

    Collection<PrioritySetting> priorities = factory.getXynaMultiChannelPortalPortal().listPriorities();
    if (priorities == null || priorities.size() == 0) {
      writeToCommandLine(statusOutputStream, "No Priorities configured.\n");
      return;
    }

    boolean moreThanOne = priorities.size() > 1;
    writeToCommandLine(statusOutputStream, "Listing information for " + priorities.size()
        + (moreThanOne ? " Priorities..." : "Priority...") + "\n");
    //output.append(String.format("%100s  %10s\n", "orderType", "priority"));
    
    Map<RuntimeContext, Set<PrioritySetting>> prioritiesForRuntimeContexts = new HashMap<RuntimeContext, Set<PrioritySetting>>();
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    
    for(PrioritySetting prioritySetting : priorities) {
      RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(prioritySetting.getRevision());
      Set<PrioritySetting> setOfPriorities = prioritiesForRuntimeContexts.get(runtimeContext);
      if(setOfPriorities == null) {
        setOfPriorities = new HashSet<PrioritySetting>();
        prioritiesForRuntimeContexts.put(runtimeContext, setOfPriorities);
      }
      setOfPriorities.add(prioritySetting);
    }
    
    
    Set<PrioritySetting> setOfPriorities = prioritiesForRuntimeContexts.get(RevisionManagement.DEFAULT_WORKSPACE);
    if (setOfPriorities != null) {
      outputSet(output, setOfPriorities);
    }

    for(Entry<RuntimeContext, Set<PrioritySetting>>  entry : prioritiesForRuntimeContexts.entrySet()) {
      if(!RevisionManagement.DEFAULT_WORKSPACE.equals(entry.getKey())) {
        String runtimeContext = entry.getKey().toString();
        if (entry.getKey() instanceof Application) {
          runtimeContext = "Applicationname: " + entry.getKey().getName() + " Versionname: " + ((Application) entry.getKey()).getVersionName();
        }
        output.append(runtimeContext).append("\n");
        outputSet(output, entry.getValue());
      }
    }

    writeToCommandLine(statusOutputStream, output.toString());

  }

  private void outputSet(StringBuilder output, Set<PrioritySetting> setOfPriorities) {
    SortedMap<String, PrioritySetting> sortedSetOfPrios = new TreeMap<String, PrioritySetting>();
    for (PrioritySetting prioritySetting : setOfPriorities) {
      sortedSetOfPrios.put(prioritySetting.getOrderType(), prioritySetting);
    }
    for (Entry<String, PrioritySetting> prioritySetting : sortedSetOfPrios.entrySet()) {
      //output.append(String.format("%100s  %10d\n", prioritySetting.getOrderType(), prioritySetting.getPriority()));
      output.append("  ").append(prioritySetting.getKey()).append(": ")
          .append(prioritySetting.getValue().getPriority()).append("\n");
    }
  }

}
