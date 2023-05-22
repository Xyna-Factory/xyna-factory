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
package com.gip.xyna.xact.trigger;



import java.sql.SQLException;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.ResultSetReaderFactory;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.xpce.AbstractConnectionAwareAck;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;



public class OracleAQTriggerAcknowledgeableObject extends AbstractConnectionAwareAck {

  private static final long serialVersionUID = -8541142579931147256L;

  private final transient OracleAQTriggerConnection triggerConnection;
  private transient SQLUtils sqlUtils;
  private boolean rollbackIssued;
  
  OracleAQTriggerAcknowledgeableObject(OracleAQTriggerConnection triggerConnection, SQLUtils sqlUtils) {
    super(null);
    this.triggerConnection = triggerConnection;
    this.sqlUtils = sqlUtils;
  }

  public void handleErrorAtPlanning(XynaOrderServerExtension xose, Throwable throwable) {
    if (triggerConnection != null) {
      if(getConnection() != null) {
        instrumentConForTriggerCon(getConnection());
      } else {
        commitOpenedSQLUtilsAndClose();
      }
    }
  }
  

  public void acknowledgeSchedulerEntry(XynaOrderServerExtension xose)
      throws XPRC_OrderEntryCouldNotBeAcknowledgedException {
    if (triggerConnection == null) {
      //sollte nur auftreten, wenn OracleAQTriggerAcknowledgeableObject deserialisiert wurde und die 
      //transiente TriggerConnection nun fehlt. Backup sollte in diesem Fall eigentlich nicht n�tig sein,
      //schadet aber auch nicht au�er etwas Mehraufwand f�r die DB.
      try {
        backup(xose);
      } catch (PersistenceLayerException e) {
        throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(e);
      }
      return;
    }

    try {
      checkAQConnection();
      if( getConnection() != null) {
        instrumentConForTriggerCon(getConnection());
        backup(xose);
        //Das Commit der TriggerCon geschieht nachtr�glich, wenn die getConnection() committed wird.
      } else {
        try {
          backup(xose);
          commitOpenedSQLUtilsAndClose();
        } catch (XNWH_RetryTransactionException e) {
          //retry once - can't use WarehouseRetryExecutor here as we are operating on a final triggerconnection
          checkAQConnection();
          backup(xose);
          commitOpenedSQLUtilsAndClose();
        }
      }
    } catch (PersistenceLayerException e) {
      rollbackOpenedSQLUtilsAndClose();
      throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(e);
    } catch (SQLException e) {
      rollbackOpenedSQLUtilsAndClose();
      throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(e);
    }

  }

  protected void checkAQConnection() throws SQLException {
    if (rollbackIssued) {
      throw new RuntimeException("Connection rollbacked and closed before order reached Scheduler.");
    }
    if (sqlUtils != null) {
      List<Long> result =
          sqlUtils.query("select 1 from dual", new Parameter(), ResultSetReaderFactory.getLongReader());
      if (result == null) {
        throw new SQLException("Failed to verify opened AQConnection");
      }
    }
  }
  
  protected synchronized void commitOpenedSQLUtilsAndClose() {
    if (sqlUtils != null) {
      try {
        sqlUtils.commit();
      } finally {
        closeConnection();
      }
    }
  }
  
  // synchronized to not contend with a possibly call from the trigger 
  protected synchronized void rollbackOpenedSQLUtilsAndClose() {
    if (sqlUtils != null) {
      try {
        sqlUtils.rollback();
        rollbackIssued = true;
      } finally {
        closeConnection();
      }
    }
  }

  protected synchronized void closeConnection() {
    sqlUtils.closeConnection();
    sqlUtils = null;
  }

  /**
   * Backup der XynaOrderServerExtension
   * @param xose
   * @throws PersistenceLayerException 
   */
  private void backup(XynaOrderServerExtension xose) throws PersistenceLayerException {
    XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
      .backup(xose, BackupCause.ACKNOWLEDGED, getConnection());
    xose.setHasBeenBackuppedInScheduler(true);
  }

  /**
   * �bertragen des Commit/Rollback/Close von der ODSConnection auf die TriggerConnection
   * @param odsConnection 
   * @param finishedTriggerConnection
   */
  private void instrumentConForTriggerCon(ODSConnection odsConnection) {
    odsConnection.executeAfterCommit(new Runnable() {
      public void run() {
        commitOpenedSQLUtilsAndClose();
      }
    });
    odsConnection.executeAfterRollback(new Runnable() {
      public void run() {
        rollbackOpenedSQLUtilsAndClose();
      }
    });
    odsConnection.executeAfterClose(new Runnable() {
      public void run() {
        rollbackOpenedSQLUtilsAndClose();
      }
    });
  }
  
}
