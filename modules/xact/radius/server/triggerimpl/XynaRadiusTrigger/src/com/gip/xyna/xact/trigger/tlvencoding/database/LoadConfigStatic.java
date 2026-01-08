/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xact.trigger.tlvencoding.database;



import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.trigger.tlvencoding.radius.RadiusEncoding;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer;



public class LoadConfigStatic {

  private static final Logger logger = CentralFactoryLogging.getLogger(LoadConfigStatic.class);


  private ODS ods;


  public void setUp() {
    ods = ODSImpl.getInstance(true);
    try {
      ods.registerPersistenceLayer(3, XMLPersistenceLayer.class);
      long iniId = ods.instantiatePersistenceLayerInstance(3l, "aghfksajgf", ODSConnectionType.HISTORY, new String[] {"defaultHISTORY"});
      ods.setDefaultPersistenceLayer(ODSConnectionType.HISTORY, iniId);
      //ods.setPersistenceLayerForTable()
      ods.registerStorable(RadiusEncoding.class);
    } catch (Exception e) {
      logger.warn("Failed to register " + RadiusEncoding.class.getSimpleName() + " storable: " + e.getMessage(), e);
    }
    /*
     * try { ods.registerStorable(DHCPEncoding.class); ods.registerPersistenceLayer(123, XMLPersistenceLayer.class);
     * long id = ods.instantiatePersistenceLayerInstance(ods.getXmlPersistenceLayerID(), "test",
     * ODSConnectionType.HISTORY, new String[]{"test"}); ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
     * } catch (XynaException e) { logger.error("", e); //fail(e.getMessage()); }
     */
  }


  public void createListOfRadiusEncodingEntry(List<RadiusEncoding> liste) throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      connection.persistCollection(liste);
      connection.commit();
    } finally {
      connection.closeConnection();
    }
  }


  public Collection<RadiusEncoding> loadRadiusEntries() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(RadiusEncoding.class);
    } finally {
      connection.closeConnection();
    }
  }

}
