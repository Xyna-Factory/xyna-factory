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

import java.io.IOException;
import java.io.OutputStream;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Reloadxml;
import com.gip.xyna.xmcp.xfcli.undisclosed.ShowPersistenceLayerDetails;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;



public class ReloadxmlImpl extends XynaCommandImplementation<Reloadxml> {

  public void execute(OutputStream statusOutputStream, Reloadxml payload) throws XynaException {
    CommandLineWriter clw = CommandLineWriter.createCommandLineWriter(statusOutputStream);
    PersistenceLayerInstanceBean[] persistenceLayerInstances = ODSImpl.getInstance().getPersistenceLayerInstances();
    for (PersistenceLayerInstanceBean plib : persistenceLayerInstances) {
      if (plib.getPersistenceLayerID() == ODSImpl.getInstance().getXmlPersistenceLayerID()) {
        long plId = plib.getPersistenceLayerInstanceID();
        try {
          AllArgs aa = new AllArgs();
          aa.addArg(String.valueOf(plId));
          aa.addArg("reload");
          new ShowPersistenceLayerDetails().execute(aa, clw);
        } catch (IOException e) {
          clw.writeLineToCommandLine("Exception reloading XML PersistenceLayer " + plib.getPersistenceLayerInstanceName() + ":" +  e.getMessage());
        }
      }
    }
    clw.writeLineToCommandLine("Reloading finished. Check logfile for warnings.");
  }

}
