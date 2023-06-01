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

package com.gip.xyna.xact.triggerv6.tlvencoding.databasev6;



import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.tlvencoding.dhcpv6.DHCPv6Encoding;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class LoadConfigv6 {

  private static final Logger logger = CentralFactoryLogging.getLogger(LoadConfigv6.class);


  private ODS ods;


  public void setUp() {
    ods = ODSImpl.getInstance(true);
    try {
      ods.registerStorable(DHCPv6Encoding.class);
    } catch (Exception e) {
      logger.warn("Failed to register " + DHCPv6Encoding.class.getSimpleName() + " storable: " + e.getMessage(), e);
    }
    /*
     * try { ods.registerStorable(DHCPEncoding.class); ods.registerPersistenceLayer(123, XMLPersistenceLayer.class);
     * long id = ods.instantiatePersistenceLayerInstance(ods.getXmlPersistenceLayerID(), "test",
     * ODSConnectionType.HISTORY, new String[]{"test"}); ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
     * } catch (XynaException e) { logger.error("", e); //fail(e.getMessage()); }
     */
  }


  public void createListOfDHCPEncodingEntry(List<DHCPv6Encoding> liste) throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      connection.persistCollection(liste);
      connection.commit();
    } finally {
      connection.closeConnection();
    }
  }


  public Collection<DHCPv6Encoding> loadDHCPEntries() throws PersistenceLayerException {
    ODSConnection connection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return connection.loadCollection(DHCPv6Encoding.class);
    } finally {
      connection.closeConnection();
    }
  }

}
