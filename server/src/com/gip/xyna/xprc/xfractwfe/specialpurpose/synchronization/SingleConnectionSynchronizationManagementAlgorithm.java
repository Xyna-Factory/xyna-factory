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
package com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.exceptions.XPRC_CronRemovalException;
import com.gip.xyna.xprc.exceptions.XPRC_DUPLICATE_CORRELATIONID;
import com.gip.xyna.xprc.exceptions.XPRC_TIMEOUT_DURING_SYNCHRONIZATION;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationManagement.TimeoutAlgorithm;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationManagement.TimeoutResult;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;



public class SingleConnectionSynchronizationManagementAlgorithm implements SynchronizationManagementAlgorithm {

  private ODS ods;
  private static final Logger logger = CentralFactoryLogging.getLogger(SingleConnectionSynchronizationManagementAlgorithm.class);

  private AtomicLong dirtyCounter;

  SingleConnectionSynchronizationManagementAlgorithm(ODS ods, AtomicLong dirtyCounter) {
    this.ods = ods;
    this.dirtyCounter = dirtyCounter;
  }

  public String awaitNotification(SynchronizationEntry synchronizationEntry, Long rootOrderId, SuspensionCause suspensionCause,
                                  ODSConnection defaultConnection ) throws PersistenceLayerException, XPRC_DUPLICATE_CORRELATIONID, XPRC_TIMEOUT_DURING_SYNCHRONIZATION {
    do {
      try {
        return awaitNotificationInternal(synchronizationEntry, rootOrderId, suspensionCause, defaultConnection);
      } catch( RetryException e) {
      }
    } while(true);
  }
 
  private static class RetryException extends Exception {
    private static final long serialVersionUID = 1L;
  }
  
  public String awaitNotificationInternal(SynchronizationEntry synchronizationEntry, Long rootOrderId, SuspensionCause suspensionCause,
                                  ODSConnection defaultConnection ) 
      throws PersistenceLayerException, XPRC_DUPLICATE_CORRELATIONID, XPRC_TIMEOUT_DURING_SYNCHRONIZATION, RetryException {
    //Existiert bereits ein Eintrag?
    SynchronizationEntry existingEntry = new SynchronizationEntry(synchronizationEntry.getCorrelationId());
    boolean doInsert = false; //soll ein Insert oder Update gemacht werden?
    try {
      defaultConnection.queryOneRowForUpdate(existingEntry);
      //Eintrag existiert, daher auswerten
      
      //ist es der richtige Eintrag oder stimmt nur die CorrelationId?
      boolean ownEntry = false;
      if( synchronizationEntry.getInternalXynaStepId().equals(existingEntry.getInternalXynaStepId()) 
          && existingEntry.getOrderId().equals(synchronizationEntry.getOrderId() ) ) {
        //richtiger Eintrag ist gefunden
        ownEntry = true;
      } else {
        if (existingEntry.getNotified() ) {
          //InternalXynaStepId ist leer, wenn dies ein Notify vor dem Await war.
          ownEntry = true; //FIXME ist nicht immer korrekt! kann z.B ein fremder Auftrag sein, der gerade genotified wurde...
        } else {
          ownEntry = false;
        }
      }
      if( ! ownEntry ) {
        //Eintrag zu fremdem Auftrag gefunden, daher abbrechen
        throw new XPRC_DUPLICATE_CORRELATIONID(synchronizationEntry.getCorrelationId());
      }
      
      
      //Steht Antwort oder Abbruch bereits fest? 
      if (existingEntry.getNotified() || existingEntry.receivedTimeout() ) {
        //Eintrag wird nicht länger benötigt
        defaultConnection.deleteOneRow(existingEntry);
        defaultConnection.commit();
        if( existingEntry.getNotified() ) {
          return existingEntry.getAnswer();
        }
        if (existingEntry.receivedTimeout()) {
          throw new XPRC_TIMEOUT_DURING_SYNCHRONIZATION(existingEntry.getCorrelationId());
        }
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      //Eintrag muss neu angelegt werden
      doInsert = true;
      existingEntry = synchronizationEntry;
      
      //evtl. ist der SynchronizationEntry nur verloren gegangen, warum auch immer... (z.b. memory-konfiguration, nach neustart)
      //deshalb Timeout hier überwachen
      long firstExecutionTimeInMilliSeconds = existingEntry.getTimeStamp();
      long timeoutInSeconds = existingEntry.getTimeout();
      long now = System.currentTimeMillis();
      // convert timeout to long to avoid integer overflow
      if (firstExecutionTimeInMilliSeconds + timeoutInSeconds * 1000 < now) {
         throw new XPRC_TIMEOUT_DURING_SYNCHRONIZATION(existingEntry.getCorrelationId());
      }
    }
    
    //Nun die CronLikeOrder erzeugen, die das Await abbricht, falls kein Notify kommt
    prepareResume(existingEntry,rootOrderId,defaultConnection);
    
    //SynchronizationEntry persistieren
    boolean updated = defaultConnection.persistObject(existingEntry);
    if( updated == doInsert ) {
      //Update trotz erwartetem Insert: eine andere Transaktion ist dazwischengekommen
      //umgekehrt Insert trotz erwartetem Update sollte nicht auftreten, da existierende Zeile für Update gelockt ist
      defaultConnection.rollback();
      throw new RetryException();
    }
    defaultConnection.commit(); //persistiert auch CLO, schaltet CLO aktiv
    
    //Suspendierung anstoßen
    throw new ProcessSuspendedException(suspensionCause); 
  }
  

  private void prepareResume(SynchronizationEntry synchronizationEntry, Long rootOrderId, ODSConnection defaultConnection) throws PersistenceLayerException, XPRC_TIMEOUT_DURING_SYNCHRONIZATION {
    Long cronId = synchronizationEntry.getCorrespondingResumeOrderId();
    if (cronId != null && cronId != 0) {
      //es wurde bereits ein cron erstellt. nach dem serverstart existiert dieser aber u.u. nicht mehr => existenz überprüfen
      CronLikeOrder clo = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().getCronLikeOrder(defaultConnection, cronId);
      if (clo != null) {
        //cron existiert noch
        return;
      } else if (synchronizationEntry.isTimedOut()) {
        //cron existiert nicht mehr, aber timeout ist bereits überschritten
        defaultConnection.deleteOneRow(synchronizationEntry);
        defaultConnection.commit();
        throw new XPRC_TIMEOUT_DURING_SYNCHRONIZATION(synchronizationEntry.getCorrelationId());
      }
    }
    
    //es wurde noch kein cron angelegt oder er existiert nicht mehr -> neuen anlegen
    long resumeTime = synchronizationEntry.getTimeStamp() + synchronizationEntry.getTimeout();
    try {
      cronId =
          SynchronizationManagement.createTimeoutCronLikeOrder(synchronizationEntry.getCorrelationId(), resumeTime, rootOrderId,
                                                               defaultConnection);
    } catch (XPRC_CronLikeSchedulerException e) {
      //throw new XPRC_FeatureRelatedExceptionDuringSuspensionHandling
      throw new RuntimeException("Failed to create cron like order: " + e.getMessage(), e); //FIXME bessere Exception!
    }
    synchronizationEntry.setReadyForNotify();
    synchronizationEntry.setCorrespondingResumeOrderId(cronId);    
  }

  public SynchronizationEntry notifyEntryAndDeleteCronJob(String correlationId, String answer, Integer internalStepId,
                                                          XynaOrderServerExtension xo, ODSConnection defaultConnection)
      throws XPRC_DUPLICATE_CORRELATIONID, PersistenceLayerException {
    SleepCounter errorBackoff = new SleepCounter(100, 6000, 4);
    do {
      try {
        return notifyEntryAndDeleteCronJobInternal(correlationId, answer, internalStepId, xo, defaultConnection);
      } catch( RetryException e) {
        try {
          errorBackoff.sleep();
        } catch (InterruptedException e1) {
          if(logger.isErrorEnabled()) {
            logger.error("Notify interrupted for correlationId '" + correlationId + "' - notify order id: " + xo.getId() + "'. Notify failed " + errorBackoff.iterationCount() + " times.");
            throw new RuntimeException("Notify interrupted for correlationId '" + correlationId + "' - notify order id: " + xo.getId());
          }
        }
      }
    } while(true);
  }
    
  public SynchronizationEntry notifyEntryAndDeleteCronJobInternal(String correlationId, String answer, Integer internalStepId,
                                                          XynaOrderServerExtension xo, ODSConnection defaultConnection)
      throws XPRC_DUPLICATE_CORRELATIONID, PersistenceLayerException, RetryException {

    boolean orderReadyForResume = true;

    SynchronizationEntry possiblyWaitingEntry =
        new SynchronizationEntry(correlationId, answer, SynchronizationManagement.getAnswerTimeout());
    try {
      defaultConnection.queryOneRowForUpdate(possiblyWaitingEntry);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      possiblyWaitingEntry.setNotified();
      boolean updated = false;
      try {
        updated = defaultConnection.persistObject(possiblyWaitingEntry);
      } catch(RuntimeException ex) {
        if(logger.isErrorEnabled()) {
          logger.error("Error updating notified status for synchronization with correlationId '" + correlationId + "' - orderId: " + xo.getId(), ex);
        }
        defaultConnection.rollback();
        throw new RetryException();
      }
      if( updated ) { //Konkurrierend ist nun doch bereits ein Eintrag in der DB, deswegen Abbruch und Retry
        defaultConnection.rollback();
        throw new RetryException();
      }
      return null;
    }

    // check whether the entry has already been resumed
    if (hasEntryBeenNotifiedBefore(possiblyWaitingEntry)) {
      throw new XPRC_DUPLICATE_CORRELATIONID(correlationId);
    } else if (possiblyWaitingEntry.isOrderResumed()) {
      if (possiblyWaitingEntry.receivedTimeout()) {
        logger.debug("Received notify for correlationId <" + correlationId
            + "> shortly after timeout, delivering response anyway.");
        possiblyWaitingEntry.setAnswer(answer);
        possiblyWaitingEntry.setNotified();
        defaultConnection.persistObject(possiblyWaitingEntry);
        return null;
      } else {
        throw new XPRC_DUPLICATE_CORRELATIONID(correlationId);
      }
    }

    if (logger.isDebugEnabled()) {
      // No need to log the content of the answer again since it has already been logged when it arrived
      logger.debug(Thread.currentThread().getName() + " is delivering the answer for correlationId: " + correlationId);
    }
    possiblyWaitingEntry.setAnswer(answer);
    possiblyWaitingEntry.setNotified();
    if (possiblyWaitingEntry.isReadyToResume()) {
      possiblyWaitingEntry.setOrderResumed(true);
    } else {
      orderReadyForResume = false;
    }
    defaultConnection.persistObject(possiblyWaitingEntry);

    if (possiblyWaitingEntry.isReadyToResume()) {
      try {
        boolean removedCron =
            XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
                .removeCronLikeOrder(defaultConnection, possiblyWaitingEntry.getCorrespondingResumeOrderId());
        if (logger.isDebugEnabled()) {
          if (removedCron) {
            logger.debug("Successfully removed obsolete resume/timeout cron job for correlation id '" + correlationId + "', cron id '"
                + possiblyWaitingEntry.getCorrespondingResumeOrderId() + "'");
          } else {
            logger.debug("Could not remove obsolete resume/timeout cron job for correlation id '" + correlationId + "', cron id '"
                + possiblyWaitingEntry.getCorrespondingResumeOrderId() + "'");
          }
        }
      } catch (XPRC_CronRemovalException e) {
        logger.warn("Failed to remove obsolete resume/timeout cron like order for correlation id <" + correlationId
            + ">, cron id <" + possiblyWaitingEntry.getCorrespondingResumeOrderId() + ">", e);
      }
    }

    if (orderReadyForResume) {
      return possiblyWaitingEntry;
    } else if (logger.isDebugEnabled()) {
      logger.debug("Got notified for a correlation ID for which the corresponding order will be resumed automatically.");
    }
    return null;

  }


  public Collection<SynchronizationEntry> listCurrentSynchronizationEntries(ODSConnection con) throws PersistenceLayerException {
    Collection<SynchronizationEntry> allOrders;
    allOrders = con.loadCollection(SynchronizationEntry.class);
    return Collections.unmodifiableCollection(allOrders);
  }




  public TimeoutAlgorithm getTimeoutAlgorithm() {
    return new SingleConnectionCleanUpAlgorithm();
  }




  private static class SingleConnectionCleanUpAlgorithm implements TimeoutAlgorithm {

    private PreparedCommand preparedDeleteCommand;
    private static final String deleteSqlString = "delete from " + SynchronizationEntry.TABLE_NAME + " where" + " ? > "
        + SynchronizationEntry.COL_TIMESTAMP + " + " + SynchronizationEntry.COL_TIMEOUT + " * 1000 AND "
        + SynchronizationEntry.COL_NOTIFIED + " = ? AND " + SynchronizationEntry.COL_ORDER_ID_2_RESUME + " is null";

    private boolean useFallbackMemoryDelete = false;


    SingleConnectionCleanUpAlgorithm() {
    }


    public void call(ODSConnection cleanupConnection) throws PersistenceLayerException {

      // try to use a prepared statement to be more efficient
      if (preparedDeleteCommand == null && !useFallbackMemoryDelete) {
        try {
          preparedDeleteCommand = cleanupConnection.prepareCommand(new Command(deleteSqlString));
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
        } catch (RuntimeException e) {
          useFallbackMemoryDelete = true;
          logger.warn("Could not create prepared statement for synchronization managament cleanup thread,"
              + " using memory fallback solution."
              + " For large numbers of synchronization entries this might result in memory problems.", e);
        }
      }

      if (preparedDeleteCommand != null) {
        int deleted =
            cleanupConnection.executeDML(preparedDeleteCommand, new Parameter(System.currentTimeMillis(), true));
        cleanupConnection.commit();
        if (logger.isDebugEnabled()) {
          if (deleted > 0) {
            logger
                .debug("Deleted " + deleted + " synchronization item" + (deleted > 1 ? "s" : "") + " due to timeout.");
          } else {
            logger.trace("No synchronization entry to be removed due to timeout.");
          }
        }
      } else {
        Collection<SynchronizationEntry> notifiedEntries = cleanupConnection.loadCollection(SynchronizationEntry.class);
        Collection<SynchronizationEntry> toBeDeleted = null;
        long now = System.currentTimeMillis();
        for (SynchronizationEntry entry : notifiedEntries) {
          boolean needToDelete = // only deleted entries that...
              entry.isTimedOut(now) // ...are overdue
                  && hasEntryBeenNotifiedBefore(entry) // ...have been notified 
                  && !entry.receivedTimeout(); // ...did not just receive the timeout signal concurrently with the notify 
          if (needToDelete) {
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
          cleanupConnection.delete(toBeDeleted);
          cleanupConnection.commit();
        }
      }
    }


    public ODSConnectionType getConnectionTypeForCleanup() {
      return ODSConnectionType.DEFAULT;
    }

  }


  private static boolean hasEntryBeenNotifiedBefore(SynchronizationEntry possiblyWaitingEntry) {
    return possiblyWaitingEntry.getNotified();
  }


  public TimeoutResult returnTimedoutEntry(String correlationId, ODSConnection con) throws PersistenceLayerException {
    SynchronizationEntry entry = new SynchronizationEntry(correlationId);
    try {
      con.queryOneRowForUpdate(entry);
      if (entry.isOrderResumed()) {
        logger.debug("Received timeout for correlation ID for which the corresponding order has already been resumed.");
        return null;
      }
      entry.setOrderResumed(true);
      entry.setReceivedTimeout(true);
      con.persistObject(entry);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      logger.debug("Received timeout for correlation ID that has already been removed.");
      return null;
    }

    TimeoutResult result = new TimeoutResult();
    result.defaultEntry = entry;
    return result;

  }


  public ODSConnectionType getConnectionTypeForFastAwait() {
    return ODSConnectionType.DEFAULT;
  }


  public boolean cleanupOrderFamily(Long rootOrderId, Set<Long> suspendedOrderIds, ODSConnection con)
                  throws PersistenceLayerException {
    
    if(suspendedOrderIds == null || suspendedOrderIds.isEmpty()) {
      return false;
    }
    List<SynchronizationEntry> entries;
    StringBuilder sql = new StringBuilder();
    sql.append("select * from ").append(SynchronizationEntry.TABLE_NAME).append(" where ");
    Iterator<Long> iter = suspendedOrderIds.iterator();
    while(iter.hasNext()) {
      sql.append(SynchronizationEntry.COL_ORDER_ID_2_RESUME).append(" = ? ");
      iter.next();
      if(iter.hasNext()) {
        sql.append(" or ");
      }
    }
    PreparedQuery<SynchronizationEntry> preparedQuery = null;
    try {
      preparedQuery = con.prepareQuery(new Query<SynchronizationEntry>(sql.toString(), new SynchronizationEntry().getReader(), SynchronizationEntry.TABLE_NAME));
      entries = con.query(preparedQuery, new Parameter(suspendedOrderIds.toArray()), -1);
    } catch (PersistenceLayerException e) {
      // PersistenceLayer kann offensichtlich keine PreparedQueries ... also Fallback und alles laden
      entries = new ArrayList<SynchronizationEntry>();
      Collection<SynchronizationEntry> allSyncEntries = con.loadCollection(SynchronizationEntry.class);
      for(SynchronizationEntry entry : allSyncEntries) {
        if(suspendedOrderIds.contains(entry.getCorrespondingresumeorder())) {
          entries.add(entry);
        }
      }
    }
    
    if(!entries.isEmpty()) {
      CronLikeScheduler cronLikeScheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler();
      for(SynchronizationEntry entry : entries) {
        if(entry.getCorrespondingResumeOrderId() != null) {
          try {
            cronLikeScheduler.removeCronLikeOrder(con, entry.getCorrespondingResumeOrderId());
          } catch (XPRC_CronRemovalException e) {
            logger.error("Failed to remove cron like order with id " + entry.getCorrespondingResumeOrderId(), e);
          }
        }
      }
      con.delete(entries);
    }
    
    return !entries.isEmpty();
  }

}
