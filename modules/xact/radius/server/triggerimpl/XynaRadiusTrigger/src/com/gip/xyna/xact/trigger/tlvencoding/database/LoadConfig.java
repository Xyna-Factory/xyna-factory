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



public class LoadConfig {

  private ODS ods;
  private static final Logger logger = CentralFactoryLogging.getLogger(LoadConfig.class);


  public void setUp() {
    ods = ODSImpl.getInstance(true);
    try {
      ods.registerStorable(RadiusEncoding.class);
    } catch (Exception e) {
      logger.debug("getInstance fehlgeschlagen");
    }
    /*
     * try { ods.registerStorable(DHCPEncoding.class); ods.registerPersistenceLayer(123, XMLPersistenceLayer.class);
     * long id = ods.instantiatePersistenceLayerInstance(ods.getXmlPersistenceLayerID(), "test",
     * ODSConnectionType.HISTORY, new String[]{"test"}); ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
     * } catch (XynaException e) { logger.error("", e); //fail(e.getMessage()); }
     */
  }


  //  public void createSingleRadiusEncodingEntry() throws PersistenceLayerException {
  //
  //    Map<String, String> valuearguments = new HashMap<String, String>();
  //
  //    // Anfang
  //    RadiusEncoding encoding = new RadiusEncoding(0, null, "Pad", 0, null, "Padding", valuearguments);
  //
  //    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
  //    try {
  //      connection.persistObject(encoding);
  //      connection.commit();
  //
  //
  //    }
  //    finally {
  //      connection.closeConnection();
  //    }
  //
  //  }


  public void createListOfDHCPEncodingEntry(List<RadiusEncoding> liste) throws PersistenceLayerException {
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
