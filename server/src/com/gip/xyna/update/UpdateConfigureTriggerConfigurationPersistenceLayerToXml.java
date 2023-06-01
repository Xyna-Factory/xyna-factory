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

package com.gip.xyna.update;

import java.util.Collection;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.XynaActivation;
import com.gip.xyna.xact.trigger.TriggerConfigurationStorable;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;


public class UpdateConfigureTriggerConfigurationPersistenceLayerToXml extends Update{

  private final Version allowedForUpdate;
  private final Version afterUpdate;
  private final boolean mustRegenerateJava;
  
  
  public UpdateConfigureTriggerConfigurationPersistenceLayerToXml(Version allowedForUpdate, Version afterUpdate,
                                                                   boolean mustRegenerateJava) {
    this.allowedForUpdate = allowedForUpdate;
    this.afterUpdate = afterUpdate;
    this.mustRegenerateJava = mustRegenerateJava;
  }

  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(TriggerConfigurationStorable.class);
    
    //Überprüfen, ob bereits persistent konfiguriert
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      Collection<TriggerConfigurationStorable> collection = con.loadCollection(TriggerConfigurationStorable.class);
      if (collection != null && collection.size() > 0) {
        if (logger.isDebugEnabled()) {
          logger.debug("TriggerConfiguration is already configured persistent.");
        }
        return;
      }
    } finally {
      con.closeConnection();
    }
    
    //TriggerConfiguration auf die XMLPersistenceLayerInstanz für XynaActivation konfigurieren
    //(wird in UpdateTriggerFilterUseNewPersistence für Trigger und Filter angelegt)
    Long targetXmlPlID = null;
    for (PersistenceLayerInstanceBean bean : ods.getPersistenceLayerInstances()) {
      if (bean.getConnectionTypeEnum() == ODSConnectionType.DEFAULT
          && bean.getPersistenceLayerID() == ods.getXmlPersistenceLayerID()
          && bean.getDepartment().equals(XynaActivation.DEFAULT_NAME)
          && bean.getConnectionParameter().equals("XynaActivation")) {
        targetXmlPlID = bean.getPersistenceLayerInstanceID();
        break;
      }
    }

    if (targetXmlPlID == null) {
      //XMLPersistenceLayerInstanz für XynaActivation nicht gefunden, daher neu anlegen
      targetXmlPlID = ods.instantiatePersistenceLayerInstance(ods.getXmlPersistenceLayerID(),
                                                              XynaActivation.DEFAULT_NAME, ODSConnectionType.DEFAULT,
                                                              new String[] {"XynaActivation"});
    }

    ods.setPersistenceLayerForTable(targetXmlPlID, TriggerConfigurationStorable.TABLE_NAME, null);
    ods.unregisterStorable(TriggerConfigurationStorable.class);
  }

  @Override
  protected Version getAllowedVersionForUpdate() {
    return this.allowedForUpdate;
  }

  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return this.afterUpdate;
  }

  @Override
  public boolean mustUpdateGeneratedClasses() {
    return this.mustRegenerateJava;
  }

}
