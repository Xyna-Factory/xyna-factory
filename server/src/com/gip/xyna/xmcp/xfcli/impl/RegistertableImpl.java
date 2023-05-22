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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Registertable;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xnwh.persistence.Storable;



public class RegistertableImpl extends XynaCommandImplementation<Registertable> {

  private static final Logger logger = CentralFactoryLogging.getLogger(RegistertableImpl.class);
  
  public void execute(OutputStream statusOutputStream, Registertable payload) throws XynaException {
    
    CommandLineWriter clw = CommandLineWriter.createCommandLineWriter(statusOutputStream);
    
    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    
    //Es muss entweder die PersistenceLayerInstanceID oder der PersistenceLayerInstanceName angegeben sein.
    long id;
    if (payload.getPersistenceLayerInstanceID() != null) {
      if (payload.getPersistenceLayerInstanceName() != null) {
        clw.writeLineToCommandLine("It may only be set the 'persistenceLayerInstanceId' or 'persistenceLayerInstanceName'.");
        return;
      }
      id = Long.valueOf(payload.getPersistenceLayerInstanceID());
    } else {
      //Ist der Name angegeben, so muss hieraus die ID bestimmt werden.
      if (payload.getPersistenceLayerInstanceName() != null) {
        id = ods.getPersistenceLayerInstanceId(payload.getPersistenceLayerInstanceName());
      } else {
        clw.writeLineToCommandLine("Either 'persistenceLayerInstanceId' or 'persistenceLayerInstanceName' must be set.");
        return;
      }
    }
    
    if( payload.getCopy() ) {
      //clw.writeLineToCommandLine("Trying to copy data from table "+ payload.getTableName()+".");
      CopyTable copyTable = new CopyTable(ods, payload.getTableName());
      copyTable.init(clw,id);
      
      copyTable.load(clw);
      
      ods.setPersistenceLayerForTable(id, payload.getTableName(), payload.getProperties());
      
      copyTable.persist(clw);
    } else {
      ods.setPersistenceLayerForTable(id, payload.getTableName(), payload.getProperties());
    }
  }

  private class CopyTable {

    private ODS ods;
    private String tableName;
    private Class<Storable<?>> storableClass;
    private ODSConnectionType connectionType;
    private PersistenceLayerInstanceBean fromPL;
    private PersistenceLayerInstanceBean toPL;
    
    
    private Collection<Storable<?>> data;
    
    public CopyTable(ODS ods, String tableName) {
      this.ods = ods;
      this.tableName = tableName;
    }

    public void init(CommandLineWriter clw, long toId) {
      storableClass = ods.getStorableByTableName(tableName);
      if (storableClass == null) {
        clw.writeLineToCommandLine( "Table '" + tableName + "' is not registered: Cannot copy data.");
        return;
      }
      
      toPL = getPersistenceLayerInstance(toId);
      if( toPL == null ) {
        clw.writeLineToCommandLine( "Could not find persistence layer instance: Cannot copy data.");
        storableClass = null;
        return;
      }
      
      connectionType = toPL.getConnectionTypeEnum();
      
      fromPL = getPersistenceLayerInstance(ods.getPersistenceLayerInstanceId(connectionType, storableClass));

      if( toPL.equals(fromPL) ) {
        clw.writeLineToCommandLine( "Table is already configured on persistence layer instance "+fromPL.getPersistenceLayerInstanceName() +": Will not copy data.");
        storableClass = null;
        return;
      }
    }

    
    private PersistenceLayerInstanceBean getPersistenceLayerInstance(long id) {
      for( PersistenceLayerInstanceBean plib : ods.getPersistenceLayerInstances() ) {
        if( plib.getPersistenceLayerInstanceID() == id ) {
          return plib;
        }
      }
      return null;
    }

    public void load(CommandLineWriter clw) {
      if( storableClass == null ) {
        return; //nichts zu tun
      }
      ODSConnection con = null;
      try {
        ods.registerStorable(storableClass);
        con = ods.openConnection(connectionType);
        data = con.loadCollection(storableClass);
        clw.writeLineToCommandLine("Read "+data.size()+" entries from "+tableName+" in persistence layer " + fromPL.getPersistenceLayerInstanceName()+".");
      } catch( PersistenceLayerException e ) {
        clw.writeLineToCommandLine("Could not load "+storableClass.getSimpleName()+": "+e.getMessage());
        logger.info("Could not load "+storableClass.getSimpleName(), e);
      } finally {
        finallyClose(con);
      }
      
    }
    public void persist(CommandLineWriter clw) {
      if( data == null || data.isEmpty() ) {
        return; //nichts zu tun
      }
      ODSConnection con = null;
      try {
        ods.registerStorable(storableClass);
        con = ods.openConnection(connectionType);
        
        con.persistCollection(data);
        con.commit();
        clw.writeLineToCommandLine("Copied "+data.size()+" entries to "+tableName+" in persistence layer " + toPL.getPersistenceLayerInstanceName()+".");
      } catch( PersistenceLayerException e ) {
        clw.writeLineToCommandLine("Could not persist "+storableClass.getSimpleName()+": "+e.getMessage());
        logger.info("Could not persist "+storableClass.getSimpleName(), e);
      } finally {
        finallyClose(con);
      }
    }
    
    private void finallyClose(ODSConnection con) {
      if (con != null) {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.warn("Could not close connection", e);
        }
      }
    }
    
  }

}
