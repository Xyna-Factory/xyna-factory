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
package com.gip.xyna.xfmg.xfctrl.proxymgmt.storables;

import java.util.Collection;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

public class ProxyStorage {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(ProxyStorage.class);
  
  private ODSImpl ods;
  
  public ProxyStorage() throws PersistenceLayerException {
    ods = ODSImpl.getInstance();
    ods.registerStorable(ProxyStorable.class);
  }

  public void persist(ProxyStorable proxy) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      logger.info( "Persist "+proxy);
      con.persistObject(proxy);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }
  
  private static void finallyClose(ODSConnection con) {
    if( con != null ) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }

  public Collection<ProxyStorable> listProxies() throws PersistenceLayerException {
    
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(ProxyStorable.class);
    } finally {
      finallyClose(con);
    }
  }

  public void remove(ProxyStorable proxy) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      logger.info( "Delete "+proxy);
      con.deleteOneRow(proxy);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }

  
}
