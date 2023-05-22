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
package com.gip.xyna.xprc.xpce;


import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xsched.DefaultBackupAcknowledgableObject;



public abstract class AbstractBackupAck extends AbstractConnectionAwareAck {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(DefaultBackupAcknowledgableObject.class);
  private volatile PersistenceLayerException plException = null;
  private volatile XNWH_RetryTransactionException rtException = null;


  public AbstractBackupAck(ODSConnection con) {
    super(con);
  }

  public final void acknowledgeSchedulerEntry(XynaOrderServerExtension xose)
      throws XPRC_OrderEntryCouldNotBeAcknowledgedException, XNWH_RetryTransactionException {
    try {
      backupPreFlight(xose);
      backup(xose);
      xose.setHasBeenBackuppedInScheduler(true);
      backupPostFlight(xose);
    } catch (XNWH_RetryTransactionException rte) {
      logger.warn("Failed to create backup for XynaOrder <" + xose.getId() + ">", rte);
      rtException = rte;
      throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(rte);
    } catch (PersistenceLayerException ple) {
      logger.warn("Failed to create backup for XynaOrder <" + xose.getId() + ">", ple);
      plException = ple;
      throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(ple);
    }
  }


  protected void backupPreFlight(XynaOrderServerExtension xose) throws PersistenceLayerException {
  }


  protected void backupPostFlight(XynaOrderServerExtension xose) throws PersistenceLayerException {
  }


  protected abstract BackupCause getBackupCause();

  
  protected final void backup(final XynaOrderServerExtension xose) throws PersistenceLayerException {

    WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {
      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
            .backup(xose, getBackupCause(), con);
      }
    };
    
    WarehouseRetryExecutor.buildCriticalExecutor().
      connection(getConnection()).
      storables(backupStorables()).
      execute(wre);

  }


  public PersistenceLayerException getPersistenceLayerException() {
    return plException;
  }


  public XNWH_RetryTransactionException getRetryTransactionException() {
    return rtException;
  }
}
