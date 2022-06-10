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
import java.util.Arrays;
import java.util.Comparator;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listtableconfig;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xnwh.persistence.TableConfiguration;



public class ListtableconfigImpl extends XynaCommandImplementation<Listtableconfig> {

  public void execute(OutputStream statusOutputStream, Listtableconfig payload) throws XynaException {

    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    TableConfiguration[] tcs = ods.getTableConfigurations();
    Arrays.sort(tcs, new Comparator<TableConfiguration>() {

      public int compare(TableConfiguration o1, TableConfiguration o2) {
        return o1.getTable().toLowerCase().compareTo(o2.getTable().toLowerCase());
      }

    });

    PersistenceLayerInstanceBean[] plis = ods.getPersistenceLayerInstances();
    Arrays.sort(plis, new Comparator<PersistenceLayerInstanceBean>() {

      public int compare(PersistenceLayerInstanceBean o1, PersistenceLayerInstanceBean o2) {
        return o1.getConnectionType().compareTo(o2.getConnectionType());
      }

    });

    writeToCommandLine(statusOutputStream, tcs.length + " configured Tables (Name, PL Instance): \n");
    
    int maxLengthTable = 10;
    for (TableConfiguration tc : tcs) {
      if (tc.getTable().length() > maxLengthTable) {
        maxLengthTable = tc.getTable().length();
      }
    }
    
    int maxLengthPli = 10;
    for (PersistenceLayerInstanceBean pli : plis) {
      if (pli.getPersistenceLayerInstanceName().length() > maxLengthPli) {
        maxLengthPli = pli.getPersistenceLayerInstanceName().length();
      }
    }
    
    String formatHeader = "%-" + maxLengthTable + "s  %-15s  %6s  %-" + maxLengthPli + "s  %-80s  %-70s";
    String formatLine = "%-" + maxLengthTable + "s  %-15s  %6d  %-" + maxLengthPli + "s  %-80s  %-70s";
    String formatNotConfigured = "%-" + maxLengthTable + "s  %-50s";
    
    String header = String.format(formatHeader, "Table name", "Connection type", "PLI ID", "PLI name", "PLI parameters", "Properties");
    writeLineToCommandLine(statusOutputStream, header);
    
    for (TableConfiguration tc : tcs) {
      boolean found = false;
      for (PersistenceLayerInstanceBean pli : plis) {
        if (pli.getPersistenceLayerInstanceID() == tc.getPersistenceLayerInstanceID()) {
          String properties = tc.getProperties() != null ? tc.getProperties() : "";
          String s = String.format(formatLine, tc.getTable(), pli.getConnectionType(), tc.getPersistenceLayerInstanceID(),
                                   pli.getPersistenceLayerInstanceName(), pli.getPersistenceLayerInstance().getInformation(),
                                   properties);
          
          writeLineToCommandLine(statusOutputStream, s);
          found = true;
          break;
        }
      }
      if (!found) {
        String s = String.format(formatNotConfigured, tc.getTable(), "not configured - using default persistence layer.");
        writeLineToCommandLine(statusOutputStream, s);
      }
    }

  }

}
