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
package com.gip.xyna.update;


import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xnwh.persistence.TableConfiguration;

/**
 * updated das orderarchive:
 * 1. alte WorkflowInstanceObjekte werden umgestellt auf OrderInstanceObjekte
 * 2. fertige auftr�ge wandern ins archiv
 */
public class UpdateCorrectConfigurationMemoryPersistenceLayerInstance extends UpdateJustVersion {


  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateCorrectConfigurationMemoryPersistenceLayerInstance.class);



  public UpdateCorrectConfigurationMemoryPersistenceLayerInstance(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }

  
  @Override
  protected void update() throws XynaException {

    ODS ods = ODSImpl.getInstance();

    long targetDefaultId = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(),
                                                                   XynaFactoryManagement.DEFAULT_NAME,
                                                                   ODSConnectionType.DEFAULT, new String[0]);
    
    Long instanceId = null; 
    boolean isDefault = false;
    
    // suchen der Memory-PersistenceLayerInstance, welche ConnectionParameter == "Configuration"
    PersistenceLayerInstanceBean layerInstances[] = ods.getPersistenceLayerInstances();
    if(layerInstances != null) {
      for(PersistenceLayerInstanceBean bean : layerInstances) {
        if (bean.getConnectionTypeEnum() == ODSConnectionType.DEFAULT
            && bean.getConnectionParameter().contains("Configuration")
            && bean.getPersistenceLayerID() == ods.getMemoryPersistenceLayerID()) {
          logger.debug("Found MemoryPersistenceLayerInstance with id " + bean.getPersistenceLayerInstanceID() + " and wrong connection parameter.");
          instanceId = bean.getPersistenceLayerInstanceID();
          isDefault = bean.getIsDefault();
          break;
        }
      }
    }
    if(isDefault) {
      ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, targetDefaultId);
    }
    
    if(instanceId != null) {
      // alle Tabellen finden, die diese PersistenceLayerInstance haben
      TableConfiguration []tc = ods.getTableConfigurations();
      if(tc != null) {
        for(TableConfiguration tableConfiguration : tc) {
          if(tableConfiguration.getPersistenceLayerInstanceID() == instanceId) {
            logger.debug("Found table " + tableConfiguration.getTable() + " configured on persistence layer instance with id " + instanceId
                         + ". Reconfigure the table on new persistence layer instance with id " + targetDefaultId);
            ods.setPersistenceLayerForTable(targetDefaultId, tableConfiguration.getTable(), null);
          }
        }
      }
      logger.debug("Remove persistence layer instance with id " + instanceId);
      ods.removePersistenceLayerInstance(instanceId);
    }
    
  }

}
