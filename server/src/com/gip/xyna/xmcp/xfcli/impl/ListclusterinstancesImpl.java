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
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterInformation;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listclusterinstances;



public class ListclusterinstancesImpl extends XynaCommandImplementation<Listclusterinstances> {

  public void execute(OutputStream statusOutputStream, Listclusterinstances payload) throws XynaException {
    Map<Long, ClusterInformation> infos =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement()
            .getClusterInstancesInformation();
    if (infos != null && infos.size() > 0) {
      List<Long> keys = new ArrayList<Long>(infos.keySet());
      Collections.sort(keys);
      writeLineToCommandLine(statusOutputStream, "Found " + infos.size() + " cluster instances:");
      for (Long key : keys) {
        ClusterInformation value = infos.get(key);
        writeToCommandLine(statusOutputStream, "\t" + key + ": ");
        StringBuilder sb =
            new StringBuilder().append(value.getDescription()).append(" (").append(value.getClusterState())
                .append(")\n");
        writeToCommandLine(statusOutputStream, sb.toString());
        String extInfo = value.getExtendedInformation();
        if (extInfo != null) {
          for (String s : extInfo.split("\\n")) {
            writeLineToCommandLine(statusOutputStream, "\t\t" + s);
          }
        } else {
          writeLineToCommandLine(statusOutputStream, "\t\tNo extended information available.");
        }
      }
    } else {
      writeLineToCommandLine(statusOutputStream, "No Cluster instances found.");
    }
  }

}
