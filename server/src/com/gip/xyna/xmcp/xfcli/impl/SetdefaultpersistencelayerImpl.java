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
import com.gip.xyna.xmcp.xfcli.generated.Setdefaultpersistencelayer;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;



public class SetdefaultpersistencelayerImpl extends XynaCommandImplementation<Setdefaultpersistencelayer> {

  public void execute(OutputStream statusOutputStream, Setdefaultpersistencelayer payload) throws XynaException {
    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    
    //Es muss entweder die PersistenceLayerInstanceID oder der PersistenceLayerInstanceName angegeben sein.
    long id;
    if (payload.getPersistenceLayerInstanceID() != null) {
      if (payload.getPersistenceLayerInstanceName() != null) {
        writeLineToCommandLine(statusOutputStream, "It may only be set the 'persistenceLayerInstanceId' or 'persistenceLayerInstanceName'.");
        return;
      }
      id = Long.valueOf(payload.getPersistenceLayerInstanceID());
    } else {
      //Ist der Name angegeben, so muss hieraus die ID bestimmt werden.
      if (payload.getPersistenceLayerInstanceName() != null) {
        id = ods.getPersistenceLayerInstanceId(payload.getPersistenceLayerInstanceName());
      } else {
        writeLineToCommandLine(statusOutputStream, "Either 'persistenceLayerInstanceId' or 'persistenceLayerInstanceName' must be set.");
        return;
      }
    }
    
    ods.setDefaultPersistenceLayer(ODSConnectionType.valueOf(payload.getConnectionType()),
                                   id);
  }

}
