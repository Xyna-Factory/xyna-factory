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
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xbatchmgmt.storables.RuntimeInformationUpdater;
import com.gip.xyna.xprc.xpce.AbstractBackupAck;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;


/**
 * AcknowledgableObject das dafür sorgt, dass die Anzahl der laufenden Aufträge in der
 * BatchProcessRuntimeInformation nur hochgezählt und der Input als "used" markiert werden,
 * wenn der Slave gebackupt wurde.
 *
 */
public class SlaveBackupAck extends AbstractBackupAck {

  private static final long serialVersionUID = 1L;
  
  private static final transient Logger logger = CentralFactoryLogging.getLogger(SlaveBackupAck.class);

  private transient RuntimeInformationUpdater updater; //transient, SlaveBackupAck wird nicht serialisiert, deshalb muss updater nicht wiederhergestellt werden
  private String currentInputId;

  public SlaveBackupAck(ODSConnection con, RuntimeInformationUpdater updater, String currentInputId) {
    super(con);
    this.updater = updater;
    this.currentInputId = currentInputId;
  }

  @Override
  protected void backupPostFlight(XynaOrderServerExtension xose) throws PersistenceLayerException {
    try{
      updater.acknowledgeSlave(getConnection(), currentInputId);
      updater.update();
      if( getConnection() != null ) {
        getConnection().commit();
      }
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

  
  /**
   * Connection bei Fehler im Planning schließen
   */
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
