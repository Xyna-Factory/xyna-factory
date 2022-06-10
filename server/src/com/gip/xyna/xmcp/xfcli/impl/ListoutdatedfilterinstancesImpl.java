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
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.FilterInstanceVersions;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listoutdatedfilterinstances;



public class ListoutdatedfilterinstancesImpl extends XynaCommandImplementation<Listoutdatedfilterinstances> {

  public void execute(OutputStream statusOutputStream, Listoutdatedfilterinstances payload) throws XynaException {
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
    Collection<FilterInstanceVersions> outdatedFilters = xat.listOutdatedFilterInstances();
    Map<String, ArrayList<FilterInstanceVersions>> groupedByApplicationName = new TreeMap<String, ArrayList<FilterInstanceVersions>>();
    CollectionUtils.group(outdatedFilters, FilterInstanceVersions.transformationGetApplicationName(), groupedByApplicationName);
    
    for (String application : groupedByApplicationName.keySet()) {
      Map<Triple<String, String, String>, ArrayList<FilterInstanceVersions>> groupedByFilterInstance = CollectionUtils.group(groupedByApplicationName.get(application), FilterInstanceVersions.transformationGetFilterInstance());

      writeLineToCommandLine(statusOutputStream, "Application '"+application+"' has " + groupedByFilterInstance.keySet().size() + " filterinstance" + (groupedByFilterInstance.keySet().size() > 1 ? "s":""));

      for (Triple<String, String, String> fi: groupedByFilterInstance.keySet()) {
        writeLineToCommandLine(statusOutputStream, "  * '"+fi.getFirst()+"' (filter: '" +fi.getSecond() + "', triggerinstance: '" + fi.getThird() + "') in " + groupedByFilterInstance.get(fi).size() + " version" + (groupedByFilterInstance.get(fi).size() > 1 ? "s" : ""));
        
        for (FilterInstanceVersions fiv: groupedByFilterInstance.get(fi)) {
          StringBuilder sb = new StringBuilder();
          sb.append("    - '").append(fiv.getVersionName());
          switch (fiv.getFilterInstance().getStateAsEnum()) {
            case ENABLED:
              int outSize = fiv.getOutdatedVersions().size();
              if (outSize == 0) {
                sb.append("' has no outdated versions");
              } else {
                sb.append("' has " + outSize + " outdated version" + (outSize > 1 ? "s":"") + ":");
                for (int i=0; i<outSize; i++){
                  if (i > 0) {
                    sb.append(", ");
                  }
                  sb.append(" '").append(fiv.getOutdatedVersions().get(i)).append("'");
                }
              }
              break;
            case DISABLED:
              sb.append("' is disabled");
              break;
            case ERROR:
              sb.append("' has an error");
              break;
          }
        writeLineToCommandLine(statusOutputStream, sb.toString());
        }
      }
    }
  }

}
