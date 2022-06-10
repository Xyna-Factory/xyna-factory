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

import com.gip.xyna.idgeneration.GeneratedIDsStorable;
import com.gip.xyna.update.Updater.VersionStorable;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.XynaActivation;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerConfigurationStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyStorable;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.securestorage.SecuredStorable;
import com.gip.xyna.xnwh.xwarehousejobs.WarehouseJob;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.ordercontextconfiguration.OrderContextConfigStorable;
import com.gip.xyna.xprc.xsched.CapacityStorable;
import com.gip.xyna.xprc.xsched.OrderSeriesManagementStorable;

public class UpdateInitialize extends UpdateJustVersion {

  public UpdateInitialize(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
  }
  
  @Override
  protected void update() throws XynaException {
    super.update();
    
    ODS ods = ODSImpl.getInstance();
    
    final long memoryPersistenceLayerId = ods.getMemoryPersistenceLayerID();
    if (memoryPersistenceLayerId >= 0) {
      long memInstanceId = ods.instantiatePersistenceLayerInstance(memoryPersistenceLayerId, "xprc",
                                                                   ODSConnectionType.DEFAULT, new String[0]);
      ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, memInstanceId);
    }
    
    
    final long devNullPersistenceLayerId = ods.getDevNullPersistenceLayerID();
    if (devNullPersistenceLayerId >= 0) {
      long historyDevNullInstanceId = ods.instantiatePersistenceLayerInstance(devNullPersistenceLayerId, XynaProcessing.DEFAULT_NAME,
                                                                              ODSConnectionType.HISTORY, new String[0]);
      
      ods.registerStorable(OrderInstanceDetails.class);
      ods.setPersistenceLayerForTable(historyDevNullInstanceId, OrderInstanceDetails.TABLE_NAME, null);
      
      ods.registerStorable(FrequencyControlledTaskInformation.class);
      ods.setPersistenceLayerForTable(historyDevNullInstanceId, FrequencyControlledTaskInformation.TABLE_NAME, null);
    }
    
    {
      long configurationXmlHistoryInstanceId = ods.instantiatePersistenceLayerInstance(ods.getXmlPersistenceLayerID(), XynaFactoryManagement.DEFAULT_NAME,
                                                                                       ODSConnectionType.HISTORY, new String[] {"Configuration"});
      
      ods.registerStorable(XynaPropertyStorable.class);
      ods.setPersistenceLayerForTable(configurationXmlHistoryInstanceId, XynaPropertyStorable.TABLE_NAME, null);
    }
    
    {
      long xactXmlDefaultInstanceId = ods.instantiatePersistenceLayerInstance(ods.getXmlPersistenceLayerID(), XynaActivation.DEFAULT_NAME,
                                                                              ODSConnectionType.DEFAULT, new String[] {"XynaActivation"});
      
      ods.registerStorable(TriggerStorable.class);
      ods.setPersistenceLayerForTable(xactXmlDefaultInstanceId, TriggerStorable.TABLE_NAME, null);
      
      ods.registerStorable(TriggerInstanceStorable.class);
      ods.setPersistenceLayerForTable(xactXmlDefaultInstanceId, TriggerInstanceStorable.TABLE_NAME, null);
      
      ods.registerStorable(FilterStorable.class);
      ods.setPersistenceLayerForTable(xactXmlDefaultInstanceId, FilterStorable.TABLE_NAME, null);
      
      ods.registerStorable(FilterInstanceStorable.class);
      ods.setPersistenceLayerForTable(xactXmlDefaultInstanceId, FilterInstanceStorable.TABLE_NAME, null);
      
      ods.registerStorable(TriggerConfigurationStorable.class);
      ods.setPersistenceLayerForTable(xactXmlDefaultInstanceId, TriggerConfigurationStorable.TABLE_NAME, null);
    }
    
    final long xmlPersistenceLayerId = ods.getXmlPersistenceLayerID();
    long defaultXMLInstanceId = ods.instantiatePersistenceLayerInstance(xmlPersistenceLayerId, XynaProcessing.DEFAULT_NAME,
                                                                        ODSConnectionType.DEFAULT, new String[] {"default" + ODSConnectionType.DEFAULT.toString()});
    
    ods.registerStorable(OrderSeriesManagementStorable.class);
    ods.setPersistenceLayerForTable(defaultXMLInstanceId, OrderSeriesManagementStorable.TABLE_NAME, null);
    
    ods.registerStorable(SecuredStorable.class);
    ods.setPersistenceLayerForTable(defaultXMLInstanceId, SecuredStorable.TABLE_NAME, null);
    
    ods.registerStorable(GeneratedIDsStorable.class);
    ods.setPersistenceLayerForTable(defaultXMLInstanceId, GeneratedIDsStorable.TABLE_NAME, null);
    
    ods.registerStorable(OrderContextConfigStorable.class);
    ods.setPersistenceLayerForTable(defaultXMLInstanceId, OrderContextConfigStorable.TABLE_NAME, null);
    
    ods.registerStorable(WarehouseJob.class);
    ods.setPersistenceLayerForTable(defaultXMLInstanceId, WarehouseJob.TABLE_NAME, null);
    
    ods.registerStorable(CapacityStorable.class);
    ods.setPersistenceLayerForTable(defaultXMLInstanceId, CapacityStorable.TABLE_NAME, null);
    
    ods.registerStorable(VersionStorable.class);
    ods.setPersistenceLayerForTable(defaultXMLInstanceId, VersionStorable.TABLE_NAME, null);
    

  }
  
  

}
