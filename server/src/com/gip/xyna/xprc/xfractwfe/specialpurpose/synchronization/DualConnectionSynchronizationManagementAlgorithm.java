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
package com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization;



import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CronRemovalException;
import com.gip.xyna.xprc.exceptions.XPRC_DUPLICATE_CORRELATIONID;
import com.gip.xyna.xprc.exceptions.XPRC_TIMEOUT_DURING_SYNCHRONIZATION;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationManagement.TimeoutAlgorithm;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationManagement.TimeoutResult;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;



public class DualConnectionSynchronizationManagementAlgorithm implements SynchronizationManagementAlgorithm {

  private ODS ods;
  private static final Logger logger = CentralFactoryLogging.getLogger(DualConnectionSynchronizationManagementAlgorithm.class);

  private AtomicLong dirtyCounter;


  DualConnectionSynchronizationManagementAlgorithm(ODS ods, AtomicLong dirtyCounter) {
    this.ods = ods;
    this.dirtyCounter = dirtyCounter;
  }


  public String awaitNotification(String correlationId, int timeoutInSeconds, Integer internalStepId,
                                  Long firstExecutionTimeInMilliSeconds, XynaOrderServerExtension xo,
                                  boolean needsToFreeCapacitiesAndVetos, ODSConnection defaultCon)
      throws XPRC_TIMEOUT_DURING_SYNCHRONIZATION, XPRC_DUPLICATE_CORRELATIONID, PersistenceLayerException {
    SynchronizationEntry possiblyExistingEntry = new SynchronizationEntry(correlationId, null, timeoutInSeconds);


    // open a history connection since this is expected to be persistent
    ODSConnection historyConnection = ods.openConnection(ODSConnectionType.HISTORY);
    try {

      boolean existedInHistory = true;
      try {
        historyConnection.queryOneRow(possiblyExistingEntry);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        existedInHistory = false;
      }

      if (existedInHistory) {

        // only notified entries are expected to be contained within the history persistence layer
        if (!possiblyExistingEntry.gotNotified()) {
          throw new RuntimeException("Entry existed in history persistence layer but has not been notified.");
        }

        if (logger.isDebugEnabled()) {
          logger.debug("Thread '" + Thread.currentThread().getName() + "' got his answer without waiting.");
        }
        historyConnection.deleteOneRow(possiblyExistingEntry);
        historyConnection.commit();
        return possiblyExistingEntry.getAnswer();

      } else {

        boolean existsInDefault = true;
        try {
          defaultCon.queryOneRow(possiblyExistingEntry);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          existsInDefault = false;
        }
        if (existsInDefault) {
          // the entry exists, this is either because the default persistence layer is configured as persistent and
          // this
          // is the order arriving again after a factory restart or crash; or this is a regular resume
          if (!possiblyExistingEntry.getInternalXynaStepId().equals(internalStepId)
              || !possiblyExistingEntry.getOrderId().equals(xo.getId())) {
            throw new XPRC_DUPLICATE_CORRELATIONID(correlationId);
          }
          if (possiblyExistingEntry.isTimedOut()) {
            defaultCon.deleteOneRow(possiblyExistingEntry);
            defaultCon.commit();
            throw new XPRC_TIMEOUT_DURING_SYNCHRONIZATION(correlationId);
          }
          if (possiblyExistingEntry.gotNotified()) {
            // this should not happen, because notified entries are only supposed to be present within the history
            // persistence layer
            logger.warn("Found a notified synchronization entry within default persistence layer.");
            return possiblyExistingEntry.getAnswer();
          }
        } else {
          long now = System.currentTimeMillis();
          // convert timeout to long to avoid integer overflow
          if (firstExecutionTimeInMilliSeconds + new Long(timeoutInSeconds) * 1000 < now) {
            // the entry may be gone due to a server restart or crash, but the parameter firstExecutionTime is
            // passed
            // by the workflow engine
            throw new XPRC_TIMEOUT_DURING_SYNCHRONIZATION(correlationId);
          }
          possiblyExistingEntry.setTimestamp(firstExecutionTimeInMilliSeconds);
          possiblyExistingEntry.setOrderId(xo.getId());
          possiblyExistingEntry.setInternalXynaStepId(internalStepId);
          defaultCon.persistObject(possiblyExistingEntry);
          defaultCon.commit();
        }

      }
    } finally {
      historyConnection.closeConnection();
    }
    SuspensionCause suspensionCause = new SuspensionCause_Await(needsToFreeCapacitiesAndVetos);    
    throw new ProcessSuspendedException(suspensionCause); 
  }


  public SynchronizationEntry notifyEntryAndDeleteCronJob(String correlationId, String answer,
                                                            Integer internalStepId, XynaOrderServerExtension xo,
                                                            ODSConnection defaultCon)
      throws XPRC_DUPLICATE_CORRELATIONID, PersistenceLayerException {
    SynchronizationEntry entryToUseForResume = null;

    boolean orderIsReadyForResume = false;

    SynchronizationEntry possiblyWaitingEntry =
        new SynchronizationEntry(correlationId, answer, SynchronizationManagement.getAnswerTimeout());
    boolean entryExistingInDefault = true;
    try {
      defaultCon.queryOneRow(possiblyWaitingEntry);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      entryExistingInDefault = false;
    }

    if (entryExistingInDefault) {
      orderIsReadyForResume = possiblyWaitingEntry.isReadyToResume();
      if (logger.isDebugEnabled()) {
        logger.debug(Thread.currentThread().getName() + " is delivering the answer '" + answer
            + "' for correlationId: " + correlationId);
      }
      possiblyWaitingEntry.setAnswer(answer);
      possiblyWaitingEntry.setNotified();
      // notify the entry and resume the order
      if (possiblyWaitingEntry.isReadyToResume()) {
        entryToUseForResume = possiblyWaitingEntry;
        entryToUseForResume.setOrderResumed(true);
        defaultCon.deleteOneRow(entryToUseForResume);

        ODSConnection historyConnection = ods.openConnection(ODSConnectionType.HISTORY);
        try {
          historyConnection.persistObject(entryToUseForResume);
          historyConnection.commit();
        } finally {
          historyConnection.closeConnection();
        }

        // TODO duplicate code, see SingleConnectionSynchronizationManagement
        try {
          if (logger.isDebugEnabled()) {
            boolean removedCron =
                XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
                    .removeCronLikeOrder(defaultCon, possiblyWaitingEntry.getCorrespondingResumeOrderId());
            if (removedCron) {
              logger.debug("Successfully removed obsolete resume/timeout cron job for correlation id '" + correlationId
                  + "', cron id '" + possiblyWaitingEntry.getCorrespondingResumeOrderId() + "'");
            } else {
              logger.debug("Could not remove obsolete resume/timeout cron job for correlation id '" + correlationId
                  + "', cron id '" + possiblyWaitingEntry.getCorrespondingResumeOrderId() + "'");
            }
          } else {
            XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
                .removeCronLikeOrder(defaultCon, possiblyWaitingEntry.getCorrespondingResumeOrderId());
          }
        } catch (XPRC_CronRemovalException e) {
          logger.warn("Failed to remove obsolete resume/timeout cron like order for correlation id <" + correlationId
              + ">, cron id <" + possiblyWaitingEntry.getCorrespondingResumeOrderId() + ">", e);
        }

      } else {
        // if the entry is not ready to be resumed because the suspension exception has not been handled,
        // the suspension handling will recognize the answer and will resume the waiting order itself so in
        // this case we do not have to resume here. just save the information that it got notified.
        defaultCon.persistObject(possiblyWaitingEntry);
      }
    } else {

      ODSConnection historyConnection = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        boolean existsInHistory = true;
        try {
          historyConnection.queryOneRow(possiblyWaitingEntry);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          existsInHistory = false;
        }
        if (existsInHistory) {
          if (!possiblyWaitingEntry.getInternalXynaStepId().equals(internalStepId)
              || !possiblyWaitingEntry.getOrderId().equals(xo.getId())) {
            throw new XPRC_DUPLICATE_CORRELATIONID(correlationId);
          }
          logger.warn("The same synchronization notification has been performed twice,"
              + " if this cannot be due to a server crash this might lead to inconsistencies.");
        } else {
          possiblyWaitingEntry.setNotified();
          // it's rather irritating to save the ids of the notification
          // possiblyWaitingEntry.setOrderId(xo.getId());
          // possiblyWaitingEntry.setInternalXynaStepId(internalStepId);
          historyConnection.persistObject(possiblyWaitingEntry);
          historyConnection.commit();
        }
      } finally {
        historyConnection.closeConnection();
      }

    }

    if (orderIsReadyForResume) {
      return entryToUseForResume;
    } else if (logger.isDebugEnabled()) {
      logger.debug("Got notified for a correlation ID for which the corresponding order has already been resumed.");
    }
    return null;

  }


  public Collection<SynchronizationEntry> listCurrentSynchronizationEntries(ODSConnection con) throws PersistenceLayerException {
    Collection<SynchronizationEntry> allOrders;
    allOrders = con.loadCollection(SynchronizationEntry.class);
    
    ODSConnection conHis = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      allOrders.addAll(conHis.loadCollection(SynchronizationEntry.class));
    } finally {
      conHis.closeConnection();
    }

    return Collections.unmodifiableCollection(allOrders);
  }




  public TimeoutAlgorithm getTimeoutAlgorithm() {
    return new DualConnectionCleanUpAlgorithm();
  }


  private static class DualConnectionCleanUpAlgorithm implements TimeoutAlgorithm {

    private PreparedCommand preparedDeleteCommand;
    private static final String deleteSqlString = "delete from " + SynchronizationEntry.TABLE_NAME + " where" + " ? > "
        + SynchronizationEntry.COL_TIMESTAMP + " + " + SynchronizationEntry.COL_TIMEOUT + " * 1000";

    private boolean useFallbackMemoryDelete = false;


    DualConnectionCleanUpAlgorithm() {
    }


    public void call(ODSConnection historyConnection) throws PersistenceLayerException {

      if (preparedDeleteCommand == null && !useFallbackMemoryDelete) {
        try {
          preparedDeleteCommand = historyConnection.prepareCommand(new Command(deleteSqlString));
        } catch (PersistenceLayerException e) {
          useFallbackMemoryDelete = true;
          logger.warn("Could not create prepared statement for synchronization managament cleanup thread,"
              + " using memory fallback solution."
              + " For large numbers of synchronization entries this might result in memory problems.");
        } catch (IllegalArgumentException e) {
          useFallbackMemoryDelete = true;
          logger.warn("Could not create prepared statement for synchronization managament cleanup thread,"
              + " using memory fallback solution."
              + " For large numbers of synchronization entries this might result in memory problems.");
        }
      }

      if (preparedDeleteCommand != null) {
        int deleted = historyConnection.executeDML(preparedDeleteCommand, new Parameter(System.currentTimeMillis()));
        historyConnection.commit();
        if (logger.isDebugEnabled()) {
          if (deleted > 0) {
            logger
                .debug("Deleted " + deleted + " synchroniatzion item" + (deleted > 1 ? "s" : "") + " due to timeout.");
          } else {
            logger.trace("No synchronization entry to be removed due to timeout.");
          }
        }
      } else {
        Collection<SynchronizationEntry> notifiedEntries = historyConnection.loadCollection(SynchronizationEntry.class);
        Collection<SynchronizationEntry> toBeDeleted = null;
        long now = System.currentTimeMillis();
        for (SynchronizationEntry entry : notifiedEntries) {
          if (entry.isTimedOut(now)) {
            if (toBeDeleted == null) {
              toBeDeleted = new HashSet<SynchronizationEntry>();
            }
            toBeDeleted.add(entry);
          }
        }
        if (toBeDeleted != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("Removing " + toBeDeleted.size() + " outdated notified synchronization entries.");
          }
          historyConnection.delete(toBeDeleted);
          historyConnection.commit();
        }
      }

    }


    public ODSConnectionType getConnectionTypeForCleanup() {
      return ODSConnectionType.HISTORY;
    }

  }


  public TimeoutResult returnTimedoutEntry(String correlationId, ODSConnection defaultConnection)
      throws PersistenceLayerException {

    SynchronizationEntry historyEntry = new SynchronizationEntry(correlationId);
    ODSConnection historyConnection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      historyConnection.queryOneRow(historyEntry);
      if (historyEntry.isOrderResumed()) {
        logger.debug("Received timeout for correlation ID for which the corresponding order has already been resumed.");
        return null;
      }
      historyEntry.setOrderResumed(true);
      historyConnection.persistObject(historyEntry);
      historyConnection.commit();
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // this is the usually expected case: the entry just exists in DEFAULT since that is where an "await"
      // is stored
      historyEntry = null;
    } finally {
      historyConnection.closeConnection();
    }

    SynchronizationEntry defaultEntry = new SynchronizationEntry(correlationId);

    try {
      defaultConnection.queryOneRow(defaultEntry);
      if (defaultEntry.isOrderResumed()) {
        logger.debug("Received timeout for correlation ID for which the corresponding order has already been resumed.");
        return null;
      }
      defaultEntry.setOrderResumed(true);
      defaultConnection.persistObject(defaultEntry);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // this is not necessarily bad as it can happen occasionally if the timed out entry
      // has just been notified => do nothing
      logger.debug("Received timeout for correlation ID that has already been removed.");
      defaultEntry = null;
    }

    if (historyEntry != null) {
      TimeoutResult result = new TimeoutResult();
      result.historyEntry = historyEntry;
      return result;
    } else if (defaultEntry != null) {
      TimeoutResult result = new TimeoutResult();
      result.defaultEntry = defaultEntry;
      return result;
    }
    return null;
  }


  public ODSConnectionType getConnectionTypeForFastAwait() {
    return ODSConnectionType.HISTORY;
  }


  public boolean cleanupOrderFamily(Long rootOrderId, Set<Long> suspendedOrderIds, ODSConnection con)
                  throws PersistenceLayerException {
    // TODO noch zu implementieren, wenn DualConnectionSynchronizationManagementAlgorithm mal wieder verwendet werden soll ...
    return false;
  }


  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationManagementAlgorithm#awaitNotification(com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationEntry, java.lang.Long, com.gip.xyna.xnwh.persistence.ODSConnection)
   */
  public String awaitNotification(SynchronizationEntry synchronizationEntry, Long rootOrderId, SuspensionCause suspensionCause,
                                  ODSConnection defaultConnection) 
                                      throws XPRC_DUPLICATE_CORRELATIONID, PersistenceLayerException {
    // TODO Auto-generated method stub
    return null;
  }

}
