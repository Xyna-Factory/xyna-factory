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
package com.gip.xyna.xprc.xprcods.orderarchive;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderBackupHelperProcessAbstract;



public class SynchronousOrderBackupResumer {

  private static final Logger logger = CentralFactoryLogging.getLogger(SynchronousOrderBackupResumer.class);


  public boolean loadAndResume(final long rootOrderId) throws PersistenceLayerException {
    return WarehouseRetryExecutor.buildCriticalExecutor().storable(OrderInstanceBackup.class).storable(OrderInstanceDetails.class)
        .execute(new WarehouseRetryExecutableNoException<Boolean>() {

          @Override
          public Boolean executeAndCommit(ODSConnection con) throws PersistenceLayerException {
            SynchronousResumer syncResumer = new SynchronousResumer();

            List<OrderInstanceBackup> backupOrders = syncResumer.loadBackups(con, rootOrderId);
            if (backupOrders.isEmpty()) {
              if (logger.isDebugEnabled()) {
                logger.debug("No order backup entries found for rootOrderId=" + rootOrderId);
              }
              return false;
            }

            PrepareResult prepared = syncResumer.prepareForCurrentBoot(backupOrders, con);
            syncResumer.abortAndResumePrepared(con, prepared);
            con.commit();

            return true;
          }
        });
  }


  private static final class SynchronousResumer extends OrderBackupHelperProcessAbstract {

    private SynchronousResumer() {
      super(Collections.<PrioritizedRootId> emptyList(), 0);
    }


    @Override
    protected WarehouseRetryExecutableNoResult getWarehouseRetryExecutable() {
      throw new UnsupportedOperationException("Not used for synchronous single-root resume.");
    }


    private List<OrderInstanceBackup> loadBackups(ODSConnection con, long rootOrderId) throws PersistenceLayerException {
      return getBackupItems(Collections.singletonList(rootOrderId), con);
    }


    private PrepareResult prepareForCurrentBoot(List<OrderInstanceBackup> backupOrders, ODSConnection con)
        throws PersistenceLayerException {

      List<OrderInstanceBackup> readyToResume = new ArrayList<OrderInstanceBackup>();
      List<OrderInstanceBackup> failed = new ArrayList<OrderInstanceBackup>();
      List<OrderInstanceDetails> detailsToPersist = new ArrayList<OrderInstanceDetails>();
      long currentBootCountId = XynaFactory.getInstance().getBootCntId();

      for (OrderInstanceBackup backup : backupOrders) {
        backup.setBootCntId(currentBootCountId);

        if (backup.getDetails() != null) {
          if (shouldPreserveShutdownRootWithoutOrder(backup)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Preserving SHUTDOWN backup entry for order " + backup.getId() + " (root without deserialized order).");
            }
            detailsToPersist.add(backup.getDetails());
            readyToResume.add(backup);
            continue;
          }
          if (checkOrderBackupInstanceForRemoval(backup, con, false)) {
            failed.add(backup);
            continue;
          }
          detailsToPersist.add(backup.getDetails());
        }
        readyToResume.add(backup);
      }

      con.persistCollection(detailsToPersist);
      con.persistCollection(readyToResume);

      return new PrepareResult(readyToResume, failed);
    }


    private boolean shouldPreserveShutdownRootWithoutOrder(OrderInstanceBackup backup) {
      OrderInstanceDetails details = backup.getDetails();
      if (details == null) {
        return false;
      }
      return backup.getBackupCauseAsEnum() == OrderInstanceBackup.BackupCause.SHUTDOWN && backup.getXynaorder() == null
          && details.getParentId() == -1;
    }


    private void abortAndResumePrepared(ODSConnection con, PrepareResult prepared) throws PersistenceLayerException {
      abortBackups(prepared.failed, con);
      resumeOrdersFromBackup(con, prepared.readyToResume);
    }
  }


  private static final class PrepareResult {

    private final List<OrderInstanceBackup> readyToResume;
    private final List<OrderInstanceBackup> failed;


    private PrepareResult(List<OrderInstanceBackup> readyToResume, List<OrderInstanceBackup> failed) {
      this.readyToResume = readyToResume;
      this.failed = failed;
    }
  }
}
