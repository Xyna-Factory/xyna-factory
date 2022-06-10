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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listclusterablecomponents;



public class ListclusterablecomponentsImpl extends XynaCommandImplementation<Listclusterablecomponents> {

  public void execute(OutputStream statusOutputStream, Listclusterablecomponents payload) throws XynaException {
    Set<Clustered> clusterableComponents =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement()
            .listClusterableComponents();
    //immer in gleicher reihenfolge ausgeben
    List<Clustered> clusterableComponentsList = new ArrayList<Clustered>(clusterableComponents);
    Collections.sort(clusterableComponentsList, new Comparator<Clustered>() {

      public int compare(Clustered o1, Clustered o2) {
        return o1.getName().compareTo(o2.getName());
      }
      
    });
    StringBuilder sb = new StringBuilder();
    sb.append("The following components may be configured to be clustered;\n");
    for (Clustered clustered : clusterableComponentsList) {
      sb.append("  o '").append(clustered.getName()).append("'");
      if (clustered.isClustered()) {
        sb.append(" is already clustered on cluster '").append(clustered.getClusterInstanceId()).append("'.");
      } else {
        sb.append(" is currently not clustered.");
      }
      sb.append("\n");
    }
    writeLineToCommandLine(statusOutputStream, sb.toString());
  }
}
