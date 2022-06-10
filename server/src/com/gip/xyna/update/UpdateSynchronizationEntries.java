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



import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;



public class UpdateSynchronizationEntries extends UpdateJustVersion {

  private static com.gip.xyna.update.outdatedclasses_5_0_0_3.SynchronizationEntry oldSynchronizationEntry
    = new com.gip.xyna.update.outdatedclasses_5_0_0_3.SynchronizationEntry();
  private static com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationEntry newSynchronizationEntry
    = new com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationEntry();

  
  private static final String sqlGetAllSynchronizationEntriesWithLaneId = 
      "select * from "+ oldSynchronizationEntry.TABLE_NAME 
      +" where "+oldSynchronizationEntry.COL_LANE_ID_2_RESUME +" is not null";
  private static final String updateOrderInstanceBackup = 
      "update " + OrderInstanceBackup.TABLE_NAME
      +" set " + OrderInstanceBackup.COL_BACKUP_CAUSE +" = '"+BackupCause.SHUTDOWN.toString()+"'" 
      +" where "+OrderInstanceBackup.COL_ID+"= ?"; 
      
      
  
  public UpdateSynchronizationEntries(Version oldVersion,
      Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }

  @Override
  protected void update() throws XynaException {
    
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(oldSynchronizationEntry.getClass());
    ods.registerStorable(OrderInstanceBackup.class);
    
    ODSConnection conDef = ods.openConnection(ODSConnectionType.DEFAULT);
    
    boolean canUpdateOrderbackup = true; //evtl. kann Orderbackup nicht geupdated werden
    PreparedCommand pcUpdateOrderBackup = null;
    try {
      pcUpdateOrderBackup = conDef.prepareCommand(new Command(updateOrderInstanceBackup));
    } catch( PersistenceLayerException e ) {
      logger.warn("Cannot update OrderBackup. This is normal when using the XynaMemoryPersistenceLayer.", e);
      canUpdateOrderbackup = false;
    }
  
    FactoryWarehouseCursor<? extends com.gip.xyna.update.outdatedclasses_5_0_0_3.SynchronizationEntry> cursor =
        conDef.getCursor( sqlGetAllSynchronizationEntriesWithLaneId, new Parameter(),
                         oldSynchronizationEntry.getReader(), 100);
    try {
      for( com.gip.xyna.update.outdatedclasses_5_0_0_3.SynchronizationEntry se : cursor.separated() ) {
        
        //bisherige LaneId austragen
        se.setLaneId(null);
        conDef.persistObject(se);
        
        if( canUpdateOrderbackup ) {
          //BackupCause auf SHUTDOWN umsetzen, damit Auftrag wieder ausgeführt wird und dabei 
          //die LaneId richtig eingetragen wird
          conDef.executeDML(pcUpdateOrderBackup, new Parameter(se.getOrdertoresume()));
        }
        
      }
      cursor.checkForExceptions();
    } catch( PersistenceLayerException e ) {
      logger.warn( "Failed to update SynchronizationEntry and OrderBackup ", e);
      throw new RuntimeException(e);
    } finally {
      try {
        conDef.commit();
      } finally {
        conDef.closeConnection();
      }
    }
    
    ods.unregisterStorable(oldSynchronizationEntry.getClass());
    
    //neu registrieren, um lanetoresume in String zu konvertieren
    ods.registerStorableForceWidening(newSynchronizationEntry.getClass());
    ods.unregisterStorable(newSynchronizationEntry.getClass());
    ods.unregisterStorable(OrderInstanceBackup.class);
  }



}
