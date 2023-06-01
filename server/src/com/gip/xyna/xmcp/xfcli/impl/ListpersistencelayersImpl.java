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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listpersistencelayers;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.PersistenceLayerBeanMemoryCache;



public class ListpersistencelayersImpl extends XynaCommandImplementation<Listpersistencelayers> {

  private static final Logger logger = CentralFactoryLogging.getLogger(ListpersistencelayersImpl.class);


  public void execute(OutputStream statusOutputStream, Listpersistencelayers payload) throws XynaException {

    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    PersistenceLayerBeanMemoryCache[] pls = ods.getPersistenceLayers();
    Arrays.sort(pls, new Comparator<PersistenceLayerBeanMemoryCache>() {

      public int compare(PersistenceLayerBeanMemoryCache o1, PersistenceLayerBeanMemoryCache o2) {
        return (int) (o1.getPersistenceLayerID() - o2.getPersistenceLayerID());
      }
    });
    writeLineToCommandLine(statusOutputStream, pls.length + " PersistenceLayers: ");
    for (PersistenceLayerBeanMemoryCache pl : pls) {
      StringBuffer sb = new StringBuffer();
      String[] paraInfo;
      try {
        paraInfo = pl.getPersistenceLayerClass().getConstructor().newInstance().getParameterInformation();
        if (paraInfo == null || paraInfo.length == 0) {
          sb.append("     none");
        } else {
          for (int i = 0; i < paraInfo.length; i++) {
            if (i > 0) {
              sb.append("\n");
            }
            sb.append("     o ").append(paraInfo[i]);
          }
        }
      } catch (Throwable e) {
        sb.append("    !!! error occurred retrieving startparameter information. see logfile.");
        logger.error("could not retrieve startparameter information for persistencelayerid="
                         + pl.getPersistenceLayerID() + " class=" + pl.getPersistenceLayerClass().getName(), e);
      }
      writeLineToCommandLine(statusOutputStream, " - " + pl.getPersistenceLayerID() + "  "
          + pl.getPersistenceLayerClass().getName() + "  " + pl.getPersistenceLayerName());
      writeLineToCommandLine(statusOutputStream, "     startparameter: ");
      writeLineToCommandLine(statusOutputStream, sb.toString());
    }

  }

}
