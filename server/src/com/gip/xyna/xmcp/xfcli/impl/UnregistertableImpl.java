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
package com.gip.xyna.xmcp.xfcli.impl;

import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.generated.Registertable;
import com.gip.xyna.xmcp.xfcli.generated.Unregistertable;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xnwh.persistence.Storable;



public class UnregistertableImpl extends XynaCommandImplementation<Unregistertable> {

  private static final Logger logger = CentralFactoryLogging.getLogger(RegistertableImpl.class);
  
  
  public void execute(OutputStream statusOutputStream, Unregistertable payload) throws XynaException {
    
    CommandLineWriter clw = CommandLineWriter.createCommandLineWriter(statusOutputStream);
    
    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    
    long id = ods.getPersistenceLayerInstanceId(ODSConnectionType.valueOf(payload.getConnectionType()), payload.getTableName());
    
    if (payload.getDelete()) {
      deleteTable(payload.getTableName(), ods, id, clw);
    }
    ods.removeTableFromPersistenceLayer(id, payload.getTableName());
  }
  
  private void deleteTable(String tableName, ODS ods, long id, CommandLineWriter clw) {
    Class<Storable<?>> storableClass = ods.getStorableByTableName(tableName);
    if (storableClass == null) {
      clw.writeLineToCommandLine( "Table '" + tableName + "' is not registered: Cannot delete data.");
      return;
    }
    
    PersistenceLayerInstanceBean pl = getPersistenceLayerInstance(id, ods);
    if( pl == null ) {
      clw.writeLineToCommandLine( "Could not find persistence layer instance: Cannot delete data.");
      storableClass = null;
      return;
    }
    
    ODSConnection con = ods.openConnection(pl.getConnectionTypeEnum());
    try {
      con.deleteAll(storableClass);
      con.commit();
    } catch( PersistenceLayerException e ) {
      clw.writeLineToCommandLine("Could not delete "+storableClass.getSimpleName()+": "+e.getMessage());
      logger.info("Could not delete "+storableClass.getSimpleName(), e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Could not close connection", e);
      }
    }
  }
  
  
  private PersistenceLayerInstanceBean getPersistenceLayerInstance(long id, ODS ods) {
    for( PersistenceLayerInstanceBean plib : ods.getPersistenceLayerInstances() ) {
      if( plib.getPersistenceLayerInstanceID() == id ) {
        return plib;
      }
    }
    return null;
  }

}
