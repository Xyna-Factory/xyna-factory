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
import java.util.Objects;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.sharedresources.KryoSerializedSharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceWorkManagementThread;
import com.gip.xyna.xnwh.sharedresources.SharedResourceWorkManagementThread.ShareResourceEntryManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceWorkManagementThread.SharedResourceWork;
import com.gip.xyna.xnwh.sharedresources.SharedResourceWorkManagementThread.SharedResourceWorkManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceWorkManagementThread.SharedResourceWorkManagementThreadConfig;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_ShutDown;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;



public class OrderBackupSharedResourceProcessing {

  public static final SharedResourceDefinition<SharedResourceOrderBackupId> XYNA_ORDER_BACKUP_SR_DEF =
      new KryoSerializedSharedResourceDefinition<>(OrderBackupManagement.XYNA_ORDERBACKUP_SR, SharedResourceOrderBackupId.class);

  private static final Logger logger = CentralFactoryLogging.getLogger(OrderBackupSharedResourceProcessing.class);
  private SharedResourceWorkManagementThread<SharedResourceDefinition<SharedResourceOrderBackupId>, SharedResourceOrderBackupId> processingThread;


  public void start() {
    SharedResourceWorkManagementThreadConfig<SharedResourceDefinition<SharedResourceOrderBackupId>, SharedResourceOrderBackupId> config;
    ShareResourceEntryManagement<SharedResourceOrderBackupId> nextProcessor = new OrderbackupEntryMgmt();
    SharedResourceWorkManagement workMgmt = new OrderbackupWorkManagement();
    config = new SharedResourceWorkManagementThreadConfig<>("OrderBackupSharedResourceProcessing", XYNA_ORDER_BACKUP_SR_DEF, nextProcessor,
                                                            workMgmt, 60_000);
    processingThread = new SharedResourceWorkManagementThread<>(config);
    processingThread.start();
  }


  public void stop() {
    processingThread.end();
    processingThread.interrupt();
    try {
      processingThread.join();
    } catch (InterruptedException e) {
    }
    processingThread.removeOurEntry();
  }


  private static class OrderbackupWorkManagement implements SharedResourceWorkManagement {

    // all results with BackupCause 'SHUTDOWN' can be resumed
    // results with BackupCause 'SUSPENSION' can be resumed, if suspensionCause in details is 'shutdown'
    private static final String sql =
        "SELECT " + OrderInstanceBackup.COL_ROOT_ID + ", " + OrderInstanceBackup.COL_DETAILS + ", " + OrderInstanceBackup.COL_BACKUP_CAUSE //
            + " FROM " + OrderInstanceBackup.TABLE_NAME + " WHERE (" //
            + OrderInstanceBackup.COL_ID + " = " + OrderInstanceBackup.COL_ROOT_ID + " AND " //
            + OrderInstanceBackup.COL_BOOTCNTID + " != ? ) AND (" + OrderInstanceBackup.COL_BACKUP_CAUSE + " = '" + BackupCause.SHUTDOWN
            + "' OR " //
            + OrderInstanceBackup.COL_BACKUP_CAUSE + " = '" + BackupCause.SUSPENSION + "')";


    private final PreparedQueryCache cache;
    private final ResultSetReader<? extends OrderInstanceBackup> reader;


    public OrderbackupWorkManagement() {
      cache = new PreparedQueryCache();
      reader = OrderInstanceBackup.getSelectiveReader();
    }


    @Override
    public List<SharedResourceWork> queryWork(long ourId) {
      List<SharedResourceWork> result = new ArrayList<>();
      for (long rootOrderId : queryResumeOrderIdsShutdown()) {
        result.add(new ResumeOrderWork(rootOrderId));
      }
      return result;
    }


    private List<Long> queryResumeOrderIdsShutdown() {
      long bootCntId = XynaFactory.getInstance().getBootCntId();
      WarehouseRetryExecutableNoException<List<Long>> wre = new WarehouseRetryExecutableNoException<List<Long>>() {

        @Override
        public List<Long> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
          PreparedQuery<? extends OrderInstanceBackup> query = cache.getQueryFromCache(sql, con, reader, OrderInstanceBackup.TABLE_NAME);
          List<? extends OrderInstanceBackup> candidates = con.query(query, new Parameter(bootCntId), 1000);
          List<Long> result = new ArrayList<>();
          for (OrderInstanceBackup candidate : candidates) {
            if (candidate.getDetails() == null) {
              continue;
            }
            String suspensionCause = candidate.getDetails().getSuspensionCause();
            if (candidate.getBackupCauseAsEnum() == BackupCause.SHUTDOWN
                || Objects.equals(suspensionCause, SuspensionCause_ShutDown.name)) {
              result.add(candidate.getRootId());
            }
          }
          return result;
        }
      };
      try {
        return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.DEFAULT).storable(OrderInstanceBackup.class)
            .execute(wre);
      } catch (PersistenceLayerException e) {
        if (logger.isWarnEnabled()) {
          logger.warn("Could not query OrderInstanceBackup", e);
        }
        return Collections.emptyList();
      }
    }

  }

  private static class OrderbackupEntryMgmt implements ShareResourceEntryManagement<SharedResourceOrderBackupId> {

    @Override
    public long readId(SharedResourceOrderBackupId nextEntry) {
      return nextEntry.id;
    }


    @Override
    public SharedResourceOrderBackupId createNextIdEntry(long value) {
      return new SharedResourceOrderBackupId(value);
    }


    @Override
    public SharedResourceOrderBackupId createNewEntry() {
      return new SharedResourceOrderBackupId();
    }


    @Override
    public void updateNextIdEntry(SharedResourceOrderBackupId nextIdEntry, long value) {
      nextIdEntry.id = value;
    }


  }


  private static class ResumeOrderWork implements SharedResourceWork {

    private final long rootOrderId;


    public ResumeOrderWork(long rootOrderId) {
      this.rootOrderId = rootOrderId;
    }


    @Override
    public void execute() {
      if (logger.isDebugEnabled()) {
        logger.debug("Processing ResumeOrderWork for rootOrderId " + rootOrderId);
      }
      SynchronousOrderBackupResumer resumer = new SynchronousOrderBackupResumer();
      try {
        resumer.loadAndResume(rootOrderId);
      } catch (PersistenceLayerException e) {
        if (logger.isWarnEnabled()) {
          logger.warn("Could not resume oder with rootId " + rootOrderId, e);
        }
      }
    }


    @Override
    public String toString() {
      return String.format("[ResumeOrder %d]", rootOrderId);
    }
  }


  private static class SharedResourceOrderBackupId {

    private long id;


    public SharedResourceOrderBackupId() {
    }


    public SharedResourceOrderBackupId(long id) {
      this.id = id;
    }
  }
}
