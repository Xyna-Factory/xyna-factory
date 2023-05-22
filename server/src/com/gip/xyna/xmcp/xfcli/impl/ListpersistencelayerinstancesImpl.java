/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
import java.util.Arrays;
import java.util.Comparator;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listpersistencelayerinstances;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;



public class ListpersistencelayerinstancesImpl extends XynaCommandImplementation<Listpersistencelayerinstances> {

  public void execute(OutputStream statusOutputStream, Listpersistencelayerinstances payload) throws XynaException {

    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    PersistenceLayerInstanceBean[] plis = ods.getPersistenceLayerInstances();
    Arrays.sort(plis, new Comparator<PersistenceLayerInstanceBean>() {

      public int compare(PersistenceLayerInstanceBean o1, PersistenceLayerInstanceBean o2) {
        return (int) (o1.getPersistenceLayerInstanceID() - o2.getPersistenceLayerInstanceID());
      }

    });
    writeLineToCommandLine(statusOutputStream, plis.length + " PersistenceLayerInstances found:");
    
    int maxLengthName = 10;
    for (PersistenceLayerInstanceBean pli : plis) {
      if (pli.getPersistenceLayerInstanceName().length() > maxLengthName) {
        maxLengthName = pli.getPersistenceLayerInstanceName().length();
      }
    }
    
    String formatHeader = " - %5s  %-" + maxLengthName +"s %-20s  %8s  %14s  %-70s";
    String formatLine = " - %5d  %-" + maxLengthName +"s %-20s  %8s  %14s  %-70s";

    String bla = String.format(formatHeader, "ID", "name", "PL name", "default", "ConnectionType", "PL Instance");
    writeLineToCommandLine(statusOutputStream, bla);
    for (PersistenceLayerInstanceBean pli : plis) {
      String info = pli.getPersistenceLayerInstance().getInformation();
      String plName = ods.getPersistenceLayer(pli.getPersistenceLayerID()).getPersistenceLayerName();

      String s =
          String.format(formatLine, pli.getPersistenceLayerInstanceID(), pli.getPersistenceLayerInstanceName(),
                        plName, (pli.getIsDefault() ? "+++" : ""), pli.getConnectionType(), info);
      //          " - " + pli.getPersistenceLayerInstanceID() + "  " + pli.getPersistenceLayerID()
      //              + (pli.getIsDefault() ? " +++" : "    ") + pli.getConnectionType() + "  " + name;
      writeLineToCommandLine(statusOutputStream, s);
    }

  }

}
