/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.update;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;

/**
 * durch einen bug ging das default persistence layer ggfs verloren. hiermit wird es wieder hergestellt
 */
public class UpdateVerifyDefaultPersistenceLayer extends UpdateJustVersion {

  public UpdateVerifyDefaultPersistenceLayer(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
  }


  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    PersistenceLayerInstanceBean b = ods.getDefaultPersistenceLayerInstance(ODSConnectionType.DEFAULT);
    if (b == null) {
      logger.debug("Found that there is no default persistencelayer set for connection type "
          + ODSConnectionType.DEFAULT);
      boolean foundMem = false;
      final long memoryPersistenceLayerId = ods.getMemoryPersistenceLayerID();
      for (PersistenceLayerInstanceBean pli : ods.getPersistenceLayerInstances()) {
        if (pli.getPersistenceLayerID() == memoryPersistenceLayerId
            && pli.getConnectionTypeEnum() == ODSConnectionType.DEFAULT) {
          logger.debug("setting default persistencelayer to id = " + pli.getPersistenceLayerInstanceID());
          foundMem = true;
          ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, pli.getPersistenceLayerInstanceID());
          break;
        }
      }
      if (!foundMem) {
        logger
            .debug("There was no persistencelayer instance of type memory. Creating one and registering it as default...");
        long memInstanceId =
            ods.instantiatePersistenceLayerInstance(memoryPersistenceLayerId, "xprc", ODSConnectionType.DEFAULT,
                                                    new String[0]);
        ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, memInstanceId);
      }
    }
  }

}
