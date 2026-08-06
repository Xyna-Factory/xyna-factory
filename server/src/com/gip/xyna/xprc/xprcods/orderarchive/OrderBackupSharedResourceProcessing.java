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
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.sharedresources.KryoSerializedSharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceRequestResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_ShutDown;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;



public class OrderBackupSharedResourceProcessing {

  public static final SharedResourceDefinition<SharedResourceOrderBackupId> XYNA_ORDER_BACKUP_SR_DEF =
      new KryoSerializedSharedResourceDefinition<>(OrderBackupManagement.XYNA_ORDERBACKUP_SR, SharedResourceOrderBackupId.class);

  // all results with BackupCause 'SHUTDOWN' can be resumed
  // results with BackupCause 'SUSPENSION' can be resumed, if suspensionCause in details is 'shutdown'
  private static final String QUERY_STRING = "SELECT " + OrderInstanceBackup.COL_ROOT_ID + ", " + OrderInstanceBackup.COL_DETAILS + ", "
      + OrderInstanceBackup.COL_BACKUP_CAUSE //
      + " FROM " + OrderInstanceBackup.TABLE_NAME + " WHERE (" //
      + OrderInstanceBackup.COL_ID + " = " + OrderInstanceBackup.COL_ROOT_ID + " AND " //
      + OrderInstanceBackup.COL_BOOTCNTID + " != ? ) AND (" + OrderInstanceBackup.COL_BACKUP_CAUSE + " = '" + BackupCause.SHUTDOWN + "' OR " //
      + OrderInstanceBackup.COL_BACKUP_CAUSE + " = '" + BackupCause.SUSPENSION + "')";

  private final Query<OrderInstanceBackup> query;

  private static final String NEXT_ID_KEY = "nextId";
  private static final long HEARTBEAT_INTERVAL_MS = 60_000;
  private static final long STALE_THRESHOLD_MS = HEARTBEAT_INTERVAL_MS * 5;
  private static final long WORK_PER_ROUND_MS = HEARTBEAT_INTERVAL_MS * 2;

  private static final Logger logger = CentralFactoryLogging.getLogger(OrderBackupSharedResourceProcessing.class);

  private SharedResourceManagement srm;
  private long ourId;
  private boolean running;
  private Thread processingThread;


  public OrderBackupSharedResourceProcessing() {
    ourId = -1l;
    running = false;
    try {
      query = new Query<>(QUERY_STRING, OrderInstanceBackup.getSelectiveReader(), OrderInstanceBackup.TABLE_NAME);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not create query", e);
    }
  }


  private void process() {

    updateEntry();

    while (running) {
      try {
        if (isOurTurnToWork()) {
          List<Work> work = queryWork();
          int numberOfProcessedItems = 0;
          if (!work.isEmpty()) {
            long now = System.currentTimeMillis();
            if (logger.isDebugEnabled()) {
              logger.debug("Found " + work.size() + " work items to process. Starting now: " + now);
            }
            for (Work workItem : work) {
              if (System.currentTimeMillis() - now > WORK_PER_ROUND_MS) {
                if (logger.isDebugEnabled()) {
                  logger.debug("Work this round exceeded limit, leaving remaining work items for the next round");
                }
                break;
              }
              workItem.execute();
              numberOfProcessedItems++;
              if (logger.isTraceEnabled()) {
                logger.trace("Finished work item " + workItem + " - total work time " + (System.currentTimeMillis() - now) + "ms");
              }
              if (System.currentTimeMillis() - now > HEARTBEAT_INTERVAL_MS) {
                refreshEntry();
              }
            }
            if (logger.isDebugEnabled()) {
              logger.debug("Updating entry after processing " + numberOfProcessedItems + " work items.");
            }
            updateEntry();
          } else {
            if (logger.isDebugEnabled()) {
              logger.debug("No work to do.");
            }
          }
        }
        Thread.sleep(HEARTBEAT_INTERVAL_MS);
        refreshEntry();
      } catch (InterruptedException e) {

      }
    }
  }


  private void refreshEntry() {
    if (ourId == -1l) {
      updateEntry();
      return;
    }
    long now = System.currentTimeMillis();
    SharedResourceRequestResult<SharedResourceOrderBackupId> result =
        getSrm().update(XYNA_ORDER_BACKUP_SR_DEF, List.of(String.valueOf(ourId)), x -> {
          x.getValue().refreshTimestsamp = now;
          return new SharedResourceInstance<>(x.getId(), now, x.getValue());
        });
    if (!result.isSuccess()) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not refresh entry", result.getException());
      }
      ourId = -1l;
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Successfully refreshed entry (" + ourId + ") to " + now);
      }
    }
  }


  private void createNextIdEntry() {
    long now = System.currentTimeMillis();
    SharedResourceRequestResult<SharedResourceOrderBackupId> result = getSrm()
        .create(XYNA_ORDER_BACKUP_SR_DEF, List.of(new SharedResourceInstance<>(NEXT_ID_KEY, now, new SharedResourceOrderBackupId(0))));
    if (!result.isSuccess()) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not create initial NextId entry.", result.getException());
      }
      return;
    }
    ourId = 0;
    if (logger.isDebugEnabled()) {
      logger.debug("Initialized NextId entry. Using id: " + ourId);
    }
  }


  private void updateEntry() {
    long oldId = ourId;
    IdContainer container = new IdContainer();
    SharedResourceRequestResult<SharedResourceOrderBackupId> result =
        getSrm().update(XYNA_ORDER_BACKUP_SR_DEF, List.of(NEXT_ID_KEY), (x) -> {
          container.id = x.getValue().refreshTimestsamp + 1;
          x.getValue().refreshTimestsamp = container.id;
          return x;
        });

    if (!result.isSuccess() || container.id == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Could not get id from nextId entry. Creating it.", result.getException());
      }
      createNextIdEntry();
    } else {
      ourId = container.id;
      if (logger.isDebugEnabled()) {
        logger.debug("Got new Id for OrderBackupWork: " + ourId);
      }
    }
    long now = System.currentTimeMillis();
    if (ourId != -1l) {
      getSrm().create(XYNA_ORDER_BACKUP_SR_DEF,
                      List.of(new SharedResourceInstance<>(String.valueOf(ourId), now, new SharedResourceOrderBackupId(now))));
    }

    removeEntry(oldId);
  }


  private void removeEntry(long oldId) {
    if (oldId == -1l) {
      if (logger.isDebugEnabled()) {
        logger.debug("No Entry to remove. oldId is not set");
      }
      return;
    }
    SharedResourceRequestResult<SharedResourceOrderBackupId> result = srm.delete(XYNA_ORDER_BACKUP_SR_DEF, List.of(String.valueOf(oldId)));
    if (!result.isSuccess()) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not delete our entry with id " + oldId, result.getException());
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Successfully deleted our entry with id " + oldId);
      }
    }

  }


  private boolean isOurTurnToWork() {
    if (ourId == -1l) {
      if (logger.isDebugEnabled()) {
        logger.debug("OurId is not set. It cannot be our turn to work");
      }
      return false;
    }
    long now = System.currentTimeMillis();
    SharedResourceRequestResult<SharedResourceOrderBackupId> result = getSrm().readAll(XYNA_ORDER_BACKUP_SR_DEF);
    if (!result.isSuccess() || result.getResources() == null) {
      return false;
    }
    boolean ourEntryFound = false;
    List<SharedResourceInstance<SharedResourceOrderBackupId>> resources = result.getResources();
    for (SharedResourceInstance<SharedResourceOrderBackupId> resource : resources) {
      if (Objects.equals(NEXT_ID_KEY, resource.getId())) {
        continue;
      }
      long id = Long.valueOf(resource.getId());
      if (id < ourId && resource.getValue().refreshTimestsamp + STALE_THRESHOLD_MS > now) {
        if (logger.isDebugEnabled()) {
          logger.debug("Not out turn to work. Active lower id found: " + id);
        }
        return false;
      }
      if (id == ourId) {
        ourEntryFound = true;
      }
    }

    if (!ourEntryFound) {
      if (logger.isWarnEnabled()) {
        logger.warn("No other working node found with a lower id than us (" + ourId + ") but our entry is missing.");
      }
      ourId = -1l;
      updateEntry();
      return false;
    }

    //ourId is the smallest, it is our turn to work
    if (logger.isDebugEnabled()) {
      logger.debug("Our turn to work");
    }
    return true;
  }


  private List<Work> queryWork() {
    List<Work> result = new ArrayList<>();
    result.addAll(queryStaleEntryWork());
    result.addAll(queryResumeOrderWork());
    return result;
  }


  private List<DeleteStaleEntryWork> queryStaleEntryWork() {
    long now = System.currentTimeMillis();
    SharedResourceRequestResult<SharedResourceOrderBackupId> resources = getSrm().readAll(XYNA_ORDER_BACKUP_SR_DEF);
    if (!resources.isSuccess() || resources.getResources() == null) {
      return Collections.emptyList();
    }
    List<DeleteStaleEntryWork> result = new ArrayList<>();
    for (SharedResourceInstance<SharedResourceOrderBackupId> resource : resources.getResources()) {
      if (Objects.equals(NEXT_ID_KEY, resource.getId())) {
        continue;
      }
      long lastUpdate = resource.getValue().refreshTimestsamp;
      if (lastUpdate + STALE_THRESHOLD_MS < now) {
        if (logger.isDebugEnabled()) {
          logger.debug("Identified Stale Entry Deletion work for entry with id " + resource.getId() + " lastUpdate: " + lastUpdate);
        }
        result.add(new DeleteStaleEntryWork(resource.getId()));
      }
    }
    return result;
  }


  private List<ResumeOrderWork> queryResumeOrderWork() {
    List<ResumeOrderWork> result = new ArrayList<>();
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

        List<OrderInstanceBackup> candidates = con.query(con.prepareQuery(query), new Parameter(bootCntId), -1);
        List<Long> result = new ArrayList<>();
        for (OrderInstanceBackup candidate : candidates) {
          String suspensionCause = candidate.getDetails().getSuspensionCause();
          if (candidate.getBackupCauseAsEnum() == BackupCause.SHUTDOWN || Objects.equals(suspensionCause, SuspensionCause_ShutDown.name)) {
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


  public void start() {
    running = true;
    processingThread = new Thread(this::process, "OrderBackupSharedResourceProcessing");
    processingThread.setDaemon(true);
    processingThread.start();
  }


  public void stop() {
    running = false;
    processingThread.interrupt();
    try {
      processingThread.join();
    } catch (InterruptedException e) {
    }
    removeEntry(ourId);
  }


  private SharedResourceManagement getSrm() {
    if (srm != null) {
      return srm;
    }
    synchronized (this) {
      if (srm != null) {
        return srm;
      }
      srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
    }
    return srm;
  }


  private abstract static class Work {

    public abstract void execute();
  }

  private static class ResumeOrderWork extends Work {

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

  private static class DeleteStaleEntryWork extends Work {

    private String id;


    public DeleteStaleEntryWork(String id) {
      this.id = id;
    }


    @Override
    public void execute() {
      if (logger.isDebugEnabled()) {
        logger.debug("Deleting State entry with id " + id);
      }
      SharedResourceRequestResult<SharedResourceOrderBackupId> result =
          XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement().delete(XYNA_ORDER_BACKUP_SR_DEF, List.of(id));
      if (!result.isSuccess()) {
        if (logger.isWarnEnabled()) {
          logger.warn("Failed to delete stale entry with id " + id, result.getException());
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Sucessfully deleted stale entry with id " + id);
        }
      }
    }


    @Override
    public String toString() {
      return String.format("[DeleteStaleEntry %s]", id);
    }
  }

  private static class SharedResourceOrderBackupId {

    private long refreshTimestsamp;


    //required for kryo serialization
    @SuppressWarnings("unused")
    public SharedResourceOrderBackupId() {
    }


    public SharedResourceOrderBackupId(long refreshTimestsamp) {
      this.refreshTimestsamp = refreshTimestsamp;
    }
  }

  private static class IdContainer {

    private Long id = null;
  }
}
