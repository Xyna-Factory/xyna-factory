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



import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerNotRegisteredException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerBeanMemoryCache;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xsched.CapacityStorable;



public class UpdateXMLShellPersistenceLayerConfig extends UpdateJustVersion {


  public UpdateXMLShellPersistenceLayerConfig(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }


  @Override
  protected void update() throws XynaException {

    ODS ods = ODSImpl.getInstance();

    PersistenceLayerBeanMemoryCache[] layers = ods.getPersistenceLayers();
    long persistenceLayerId = -1;
    for (PersistenceLayerBeanMemoryCache layer : layers) {
      if (layer.getPersistenceLayerClass().getName().equals(ODSImpl.XYNA_XMLSHELL_PERSISTENCE_LAYER_FQ_CLASSNAME)) {
        persistenceLayerId = layer.getPersistenceLayerID();
      }
    }
    if (persistenceLayerId == -1) {
      // wieso ist der XMLPersistenceLayerWithShellQueries nicht da?
      throw new XNWH_PersistenceLayerNotRegisteredException(ODSImpl.XYNA_XMLSHELL_PERSISTENCE_LAYER_FQ_CLASSNAME);
    }

    long xmlshellInstanceIdDefault =
        ods.instantiatePersistenceLayerInstance(persistenceLayerId, XynaProcessing.DEFAULT_NAME,
                                                ODSConnectionType.DEFAULT, new String[0]);
    long xmlshellInstanceIdHistory =
        ods.instantiatePersistenceLayerInstance(persistenceLayerId, XynaProcessing.DEFAULT_NAME,
                                                ODSConnectionType.HISTORY, new String[0]);

    // CapacityStorable DEFAULT und HISTORY
    ods.registerStorable(CapacityStorable.class);
    String tableName = new CapacityStorable().getTableName();
    if (!ods.isTableRegistered(ODSConnectionType.DEFAULT, tableName)) {
      ods.setPersistenceLayerForTable(xmlshellInstanceIdDefault, tableName, null);
    }
    ods.unregisterStorable(CapacityStorable.class);
    
    // FrequencyControlledTaskInformation HISTORY
    ods.registerStorable(FrequencyControlledTaskInformation.class);
    tableName = new FrequencyControlledTaskInformation().getTableName();
    if (!ods.isTableRegistered(ODSConnectionType.HISTORY, tableName)) {
      ods.setPersistenceLayerForTable(xmlshellInstanceIdHistory, tableName, null);
    }
    ods.unregisterStorable(FrequencyControlledTaskInformation.class);

    // OrderArchive HISTORY
    ods.registerStorable(OrderInstanceDetails.class);
    tableName = new OrderInstance().getTableName();
    if (!ods.isTableRegistered(ODSConnectionType.HISTORY, tableName)) {
      ods.setPersistenceLayerForTable(xmlshellInstanceIdHistory, new OrderInstance().getTableName(), null);
    }
    ods.unregisterStorable(OrderInstanceDetails.class);

  }

}
