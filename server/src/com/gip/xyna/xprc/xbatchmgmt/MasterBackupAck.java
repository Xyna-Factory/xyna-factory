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
package com.gip.xyna.xprc.xbatchmgmt;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.AbstractBackupAck;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;


/**
 * AcknowledgableObject das dafür sorgt, dass die BatchProcessRuntimeInformation
 * und BatchProcessArchive erst persistiert werden, wenn der Master gebackupt wurde.
 *
 */
public class MasterBackupAck extends AbstractBackupAck {
  
  private static final long serialVersionUID = 1L;
  
  private static final transient Logger logger = CentralFactoryLogging.getLogger(MasterBackupAck.class);
  
  private transient BatchProcess batchProcess; //transient, MasterBackupAck wird nicht serialisiert, deshalb muss batchProcess nicht wiederhergestellt werden
    
  public MasterBackupAck(BatchProcess batchProcess) {
    super( ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT) );
    this.batchProcess = batchProcess;
  }

  @Override
  protected void backupPostFlight(XynaOrderServerExtension xose) throws PersistenceLayerException {
    try {
      batchProcess.persistStorables(getConnection());
      getConnection().commit();
      getConnection().closeConnection();
      batchProcess.acknowledgeMasterBackup();
    } finally {
      if( getConnection() != null ) {
        try {
          getConnection().closeConnection();
        } catch (PersistenceLayerException e) {
          logger.warn("Failed to close connection", e);
        }
      }
    }
  }
  
  
  @Override
  protected BackupCause getBackupCause() {
    return BackupCause.ACKNOWLEDGED;
  }
  
    
  @Override
  public void handleErrorAtPlanning(XynaOrderServerExtension xose, Throwable throwable) {
    if (getConnection() != null) {
      try {
        getConnection().closeConnection();
      }
      catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }
}
