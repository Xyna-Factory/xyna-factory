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

package com.gip.xyna.xact.triggerv6.databasescrpt;



import java.util.Collection;
import java.util.List;

import com.gip.xyna.xact.dhcpv6.db.storables.Condition;
import com.gip.xyna.xact.dhcpv6.db.storables.DeviceClass;
import com.gip.xyna.xact.dhcpv6.db.storables.GuiAttribute;
import com.gip.xyna.xact.dhcpv6.db.storables.GuiFixedAttribute;
import com.gip.xyna.xact.dhcpv6.db.storables.GuiParameter;
import com.gip.xyna.xact.dhcpv6.db.storables.PoolType;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class LoadGuiLists {

  private ODS ods;


  public void setUp() {
    ods = ODSImpl.getInstance(true);
    try {
      ods.registerStorable(GuiAttribute.class);
      ods.registerStorable(GuiFixedAttribute.class);
      ods.registerStorable(GuiParameter.class);
    
      ods.registerStorable(DeviceClass.class);
      ods.registerStorable(Condition.class);
      ods.registerStorable(PoolType.class);
    
    }
    catch (Exception e) {
      System.out.println("LoadConfigv6: getInstance fehlgeschlagen");
    }

    /*
     * try { ods.registerStorable(DHCPv6Encoding.class); ods.registerPersistenceLayer(123, XmlPersistenceLayer.class);
     * long id = ods.instantiatePersistenceLayerInstance(ods.getXmlPersistenceLayerID(), "test",
     * ODSConnectionType.HISTORY, new String[]{"test"}); ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
     * } catch (XynaException e) { }
     */
  }


  public void createListOfGuiAttributeEntries(Collection<GuiAttribute> liste) throws PersistenceLayerException {


    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);


    try {
      connection.deleteAll(GuiAttribute.class);
      connection.persistCollection(liste);
      connection.commit();
    }
    finally {
      connection.closeConnection();
    }
  }

  public void createListOfGuiFixedAttributeEntries(Collection<GuiFixedAttribute> liste) throws PersistenceLayerException {


    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);


    try {
      connection.deleteAll(GuiFixedAttribute.class);
      connection.persistCollection(liste);
      connection.commit();
    }
    finally {
      connection.closeConnection();
    }
  }

  public void createListOfGuiParameterEntries(Collection<GuiParameter> liste) throws PersistenceLayerException {


    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);


    try {
      connection.deleteAll(GuiParameter.class);
      connection.persistCollection(liste);
      connection.commit();
    }
    finally {
      connection.closeConnection();
    }
  }


  public Collection<GuiAttribute> loadGuiAttribute() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(GuiAttribute.class);
    }
    finally {
      connection.closeConnection();
    }
  }

  public Collection<GuiFixedAttribute> loadGuiFixedAttribute() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(GuiFixedAttribute.class);
    }
    finally {
      connection.closeConnection();
    }
  }

  public Collection<GuiParameter> loadGuiParameter() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(GuiParameter.class);
    }
    finally {
      connection.closeConnection();
    }
  }

  public Collection<DeviceClass> loadDeviceClass() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(DeviceClass.class);
    }
    finally {
      connection.closeConnection();
    }
  }

  public Collection<Condition> loadCondition() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(Condition.class);
    }
    finally {
      connection.closeConnection();
    }
  }

  public Collection<PoolType> loadPoolType() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(PoolType.class);
    }
    finally {
      connection.closeConnection();
    }
  }

  
}
