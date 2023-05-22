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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Instantiatepersistencelayer;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;


public class InstantiatepersistencelayerImpl extends XynaCommandImplementation<Instantiatepersistencelayer> {

  public void execute(OutputStream statusOutputStream, Instantiatepersistencelayer payload) throws XynaException {
    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    
    //Es muss entweder die PersistenceLayerID oder der PersistenceLayerName angegeben sein.
    long id;
    if (payload.getPersistenceLayerID() != null) {
      if (payload.getPersistenceLayerName() != null) {
        writeLineToCommandLine(statusOutputStream, "It may only be set the 'persistenceLayerId' or 'persistenceLayerName'.");
        return;
      }
      id = Long.valueOf(payload.getPersistenceLayerID());
    } else {
      //Ist der Name angegeben, so muss hieraus die ID bestimmt werden.
      if (payload.getPersistenceLayerName() != null) {
        id = ods.getPersistenceLayerId(payload.getPersistenceLayerName());
      } else {
        writeLineToCommandLine(statusOutputStream, "Either 'persistenceLayerId' or 'persistenceLayerName' must be set.");
        return;
      }
    }
    
    // TODO cleanup
    long l =
        ods.instantiatePersistenceLayerInstance(payload.getPersistenceLayerInstanceName(), id, payload.getDepartment(),
                                                ODSConnectionType.valueOf(payload.getConnectionType()),
                                                payload.getPersistenceLayerSpecifics());
    
    //ACHTUNG: EIGENSCHAFTEN DIESES TEXTS WERDEN IN SKRIPTEN BENUTZT (awk)
    writeToCommandLine(statusOutputStream, "ID of new PersistenceLayerInstance = " + l + "\n");
  }

}
