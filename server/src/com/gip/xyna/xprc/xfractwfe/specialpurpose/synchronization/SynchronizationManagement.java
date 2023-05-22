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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.utils.RepeatedExceptionCheck;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutable;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableOneException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CronCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderStorageException;
import com.gip.xyna.xprc.exceptions.XPRC_DUPLICATE_CORRELATIONID;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCronLikeOrderParametersException;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.exceptions.XPRC_ResumeFailedException;
import com.gip.xyna.xprc.exceptions.XPRC_TIMEOUT_DURING_SYNCHRONIZATION;
import com.gip.xyna.xprc.exceptions.XPRC_UNEXPECTED_THREAD_INTERRUPTION;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xpce.AbstractBackupAck;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler.CronLikeOrderPersistenceOption;
import com.gip.xyna.xprc.xsched.orderabortion.SuspendedOrderAbortionSupportListenerInterface;



// TODO externalize message broker parts, see bugz 13164
public class SynchronizationManagement extends FunctionGroup
    implements SuspendedOrderAbortionSupportListenerInterface {


  public static final String DEFAULT_NAME = "SynchronizationManagement";
  private static final Logger logger = CentralFactoryLogging.getLogger(SynchronizationManagement.class);
  
  
  public static class TimeoutResult {
    public SynchronizationEntry historyEntry;
    public SynchronizationEntry defaultEntry;
  }


  public static interface TimeoutAlgorithm {

    public void call(ODSConnection defaultConnection) throws PersistenceLayerException;
    public ODSConnectionType getConnectionTypeForCleanup();
  }
  

  /**
   * in sekunden
   */
  public static int getAnswerTimeout() {
    return (int) XynaProperty.XPRC_SYNC_ANSWER_TIMEOUT_SECONDS.get();
  }

  private static final String RETRY = "no_answer, retry (internally used string)_432x193";
  

  // init at value 1 to timeout old entries in the beginning
  private AtomicLong dirtyCounter;

  private ODS ods;
  private CleanupRunnable cleanupRunnable;

  private SynchronizationManagementAlgorithm algorithm;
  
  private ConcurrentMap<String, ReentrantLock> correlationLockMap;
  private ReadWriteLock globalCorrelationLockMapLock; 
  
  /*
   * Schnittstelle des Synchronization Service zur Synchronisierung asynchroner Aufrufe in Workflows
   */
  public SynchronizationManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    dirtyCounter = new AtomicLong(1);
    correlationLockMap = new ConcurrentHashMap<String, ReentrantLock>();
    globalCorrelationLockMapLock = new ReentrantReadWriteLock();
    
    ods = ODSImpl.getInstance();
    ods.registerStorable(SynchronizationEntry.class);
    
    XynaProperty.XPRC_SYNCHRONIZATION_WAIT_ACTIVELY_FOR_RESPONSE_TIMEOUT_MILLISECONDS.registerDependency(DEFAULT_NAME);
    XynaProperty.XPRC_SYNC_ANSWER_TIMEOUT_SECONDS.registerDependency(DEFAULT_NAME);
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(SynchronizationManagement.class,"SynchronizationManagement.init").
      after(SuspendResumeManagement.class).
      execAsync(
      new Runnable() { public void run() {
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().
        addListener(SynchronizationManagement.this);
      } }
    );
    
    if (ods.isSamePhysicalTable(SynchronizationEntry.TABLE_NAME, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY) || true) {
      //algorithm = new SingleConnectionSynchronizationManagementAlgorithm(ods, dirtyCounter);
      algorithm = new SingleConnectionSynchronizationManagementAlgorithm(ods, dirtyCounter);
    } else {
      //algorithm = new DualConnectionSynchronizationManagementAlgorithm(ods, dirtyCounter);
      algorithm = new DualConnectionSynchronizationManagementAlgorithm(ods, dirtyCounter);
    }
    cleanupRunnable = new CleanupRunnable(algorithm.getTimeoutAlgorithm());
    Thread t = new Thread(cleanupRunnable, "SynchronizationCleanupThread");
    t.start();

  }


  @Override
  protected void shutdown() throws XynaException {
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().removeListener(this);
    if (cleanupRunnable != null) {
      cleanupRunnable.stopRunning();
    }
  }

  /**
   * Does not block. TODO wieso? h�ngt doch von XPRC_SYNCHRONIZATION_WAIT_ACTIVELY_FOR_RESPONSE_TIMEOUT_MILLISECONDS ab!
   * @param correlationId - id to synchronize on
   * @param timeoutInSeconds - a resume order will be started after this time, thereby finding out that no answer has
   *          been created.
   * @return - the returning String will be wrapped in a SynchronizationAnswer and will be the return of the service
   *         call
   */
  public String awaitNotification(final String correlationId, final int timeoutInSeconds, final Integer internalStepId,
                                  final Long firstExecutionTimeInMilliSeconds, final XynaOrderServerExtension xo,
                                  final boolean needsToFreeCapacitiesAndVetos, final String laneId)
      throws XPRC_TIMEOUT_DURING_SYNCHRONIZATION, XPRC_UNEXPECTED_THREAD_INTERRUPTION, XPRC_DUPLICATE_CORRELATIONID,
      PersistenceLayerException {
    if (correlationId == null || correlationId.length() == 0) {
      throw new IllegalArgumentException("Correlation Id must not be null");
    }

    dirtyCounter.incrementAndGet();

    if (timeoutInSeconds < 1) {
      throw new RuntimeException("Synchronization Service operation 'await notification' was called with an illegal timeout value: "
                                     + timeoutInSeconds + "s");
    }

    if (logger.isDebugEnabled()) {
      logger.debug(xo+" is waiting "+timeoutInSeconds+" second"+(timeoutInSeconds==1?"":"s")
                   +" for an answer with correlationId <"+correlationId+">");
    }

    final Lock corrLock = new CorrelationIdLock(globalCorrelationLockMapLock, correlationLockMap, correlationId);

    long activeWaitMaxSleepTime = XynaProperty.XPRC_SYNCHRONIZATION_WAIT_ACTIVELY_FOR_RESPONSE_TIMEOUT_MILLISECONDS.getMillis();
   
    //ohne suspension auf notify warten, falls so konfiguriert
    if( activeWaitMaxSleepTime > 0 ) {
      long endTimeForUserTimeout = firstExecutionTimeInMilliSeconds + timeoutInSeconds * 1000;
      long endTimeUntilSuspension = System.currentTimeMillis() + activeWaitMaxSleepTime;
      long endTimeForLoop = Math.min(endTimeForUserTimeout, endTimeUntilSuspension);
      
      String result = waitWithoutSuspension(correlationId, corrLock, endTimeForLoop, activeWaitMaxSleepTime);
      if (!RETRY.equals(result)) {
        return result;
      } 
    }
    
    SynchronizationEntry synchronizationEntry = new SynchronizationEntry(correlationId);
    synchronizationEntry.setTimestamp(firstExecutionTimeInMilliSeconds);
    synchronizationEntry.setOrderId(xo.getId());
    synchronizationEntry.setRootId(xo.getRootOrder().getId());
    synchronizationEntry.setInternalXynaStepId(internalStepId);
    synchronizationEntry.setLaneId(laneId);
    synchronizationEntry.setTimeoutInSeconds(timeoutInSeconds);
    
    SuspensionCause suspensionCause = new SuspensionCause_Await(needsToFreeCapacitiesAndVetos);
    suspensionCause.setLaneId(laneId);
    
    AwaitNotification aw = new AwaitNotification(algorithm, synchronizationEntry, xo.getRootOrder().getId(), suspensionCause);
    return WarehouseRetryExecutor.executeWithRetries(aw, ODSConnectionType.DEFAULT,
                                                     Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                                     Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                                     aw.getStorableClassList() );

  }


  private static class AwaitNotification implements WarehouseRetryExecutable<String, XPRC_DUPLICATE_CORRELATIONID, XPRC_TIMEOUT_DURING_SYNCHRONIZATION> {

    private final SynchronizationManagementAlgorithm algorithm;
    private final SynchronizationEntry synchronizationEntry;
    private final Long rootOrderId;
    private final SuspensionCause suspensionCause;

    public AwaitNotification(SynchronizationManagementAlgorithm algorithm, SynchronizationEntry synchronizationEntry,
                             Long rootOrderId, SuspensionCause suspensionCause) {
      this.algorithm = algorithm;
      this.synchronizationEntry = synchronizationEntry;
      this.rootOrderId = rootOrderId;
      this.suspensionCause = suspensionCause;
    }

    public String executeAndCommit(ODSConnection con) throws PersistenceLayerException, XPRC_DUPLICATE_CORRELATIONID, XPRC_TIMEOUT_DURING_SYNCHRONIZATION {
      return algorithm.awaitNotification(synchronizationEntry, rootOrderId, suspensionCause, con);
    }
    
    public StorableClassList getStorableClassList() {
      return new StorableClassList(SynchronizationEntry.class,CronLikeOrder.class); 
    }
  }
  
  
  /**
   * ohne suspension auf notify warten, falls so konfiguriert
   * @param correlationId 
   * @param corrLock 
   * @param endTimeForLoop
   * @param activeWaitMaxSleepTime 
   * @throws XPRC_TIMEOUT_DURING_SYNCHRONIZATION 
   * @throws PersistenceLayerException 
   */
  private String waitWithoutSuspension(final String correlationId, final Lock corrLock, long endTimeForLoop, long activeWaitMaxSleepTime) throws XPRC_TIMEOUT_DURING_SYNCHRONIZATION, PersistenceLayerException {
    
    long waitTimePerLoop = Math.min(1000, activeWaitMaxSleepTime / 2);
    
    while (System.currentTimeMillis() < endTimeForLoop) {
      try {

        WarehouseRetryExecutable<String, XPRC_TIMEOUT_DURING_SYNCHRONIZATION, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY> wre =
            new WarehouseRetryExecutable<String, XPRC_TIMEOUT_DURING_SYNCHRONIZATION, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY>() {

              public String executeAndCommit(ODSConnection connectionForAwait) throws PersistenceLayerException,
                  XPRC_TIMEOUT_DURING_SYNCHRONIZATION, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
                corrLock.lock();
                try {
                  SynchronizationEntry possiblyNotifiedEntry = new SynchronizationEntry(correlationId);
                  connectionForAwait.queryOneRowForUpdate(possiblyNotifiedEntry);
                  if (possiblyNotifiedEntry.gotNotified()) {
                    connectionForAwait.deleteOneRow(possiblyNotifiedEntry);
                    connectionForAwait.commit();
                    return possiblyNotifiedEntry.getAnswer();
                  } else if (possiblyNotifiedEntry.isTimedOut()) {
                    connectionForAwait.deleteOneRow(possiblyNotifiedEntry);
                    connectionForAwait.commit();
                    throw new XPRC_TIMEOUT_DURING_SYNCHRONIZATION(correlationId);
                  }
                  // if none of the cases apply but the object exists, just retry
                  return RETRY;
                } finally {
                  corrLock.unlock();
                }
              }

            };

        String result =
            WarehouseRetryExecutor.executeWithRetries(wre, algorithm.getConnectionTypeForFastAwait(),
                                                      Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                                      Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                                      new StorableClassList(SynchronizationEntry.class));

        if (!RETRY.equals(result)) {
          return result;
        }

      } catch (XNWH_RetryTransactionException e) {
        // using OrderInstanceBackup as it is expected to be clustered
        if (new OrderInstanceBackup().getClusterState(ODSConnectionType.DEFAULT) == ClusterState.DISCONNECTED_SLAVE) {
          throw new OrderDeathException(e);
        } else {
          throw new XNWH_GeneralPersistenceLayerException("Was unable to retry", e);
        }
      } catch (PersistenceLayerException e) {
        throw e;
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // expected, just retry
      }
      
      try {
        Thread.sleep(waitTimePerLoop);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;// continue with regular execution, this may happen e.g. on shutdown
      }
    }
    return RETRY;
  }


  /**
   * Leave an answer and free any waiting threads, answer will get invalid in ANSWER_TIMEOUT seconds. The method is
   * synchronized because two connections are opened in the opposite order compared to awaitNotification.
   */
  public void notifyWaiting(final String correlationId, final String answer, final Integer internalStepId,
                            final XynaOrderServerExtension xo) throws XPRC_DUPLICATE_CORRELATIONID,
      PersistenceLayerException {
    if (correlationId == null) {
      throw new NullPointerException("CorrelationId must not be null.");
    }
    
    dirtyCounter.incrementAndGet();

    if (logger.isDebugEnabled()) {
      StringBuilder answerForLogFile = new StringBuilder();
      answerForLogFile.append("Got notification for correlationId: '").append(correlationId).append("', answer: ");
      if (answer != null && answer.length() > 100) {
        answerForLogFile.append("'").append(answer.substring(0, 100)).append("...' [length=").append(answer.length()).append("]");
      } else {
        answerForLogFile.append("'").append(answer).append("'");
      }
      logger.debug(answerForLogFile.toString());
    }

    // FIXME for the dualconnectionSynchronization this wont really work this way since it requires two connections
    WarehouseRetryExecutableOneException<SynchronizationEntry, XPRC_DUPLICATE_CORRELATIONID> wre =
        new WarehouseRetryExecutableOneException<SynchronizationEntry, XPRC_DUPLICATE_CORRELATIONID>() {

          public SynchronizationEntry executeAndCommit(ODSConnection defaultCon) throws PersistenceLayerException,
              XPRC_DUPLICATE_CORRELATIONID {
            // get the connection before locking the whole correlation id and getting the global readlock
            final Lock corrLock =
                new CorrelationIdLock(globalCorrelationLockMapLock, SynchronizationManagement.this.correlationLockMap,
                                      correlationId);
            corrLock.lock();
            try {
              SynchronizationEntry result =
                  algorithm.notifyEntryAndDeleteCronJob(correlationId, answer, internalStepId, xo, defaultCon);
              if (result != null) {
                try {
                  resumeWaitingOrder(result, defaultCon);
                } catch (XPRC_ResumeFailedException e) {
                  //fehler weiterwerfen -> dann kann der workflow erneut ausgef�hrt werden, falls die voraussetzungen f�r ein resume besser sind.
                  //nachteil: die sync-answer geht dadurch verloren. die k�nnte man eigtl auch speichern.
                  //          -> problem dabei: dann kann man halt nicht mehr einfach den workflow erneut ausf�hren, 
                  //             weil der synchronization-entry bereits als notified gilt.
                  //eine l�sung w�re: die einfache m�glichkeit zu bieten, das resume erneut zu triggern
                  throw new RuntimeException(e); //TODO exception deklarieren?
                }
              }
              return result;
            } finally {
              corrLock.unlock();
            }
          }
        };

    WarehouseRetryExecutor.executeWithRetriesOneException(wre, ODSConnectionType.DEFAULT,
                                                          Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                                          Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                                          new StorableClassList(SynchronizationEntry.class, OrderInstanceBackup.class, CronLikeOrder.class, OrderInstance.class));
  }

  
  private static class SynchronizationAck extends AbstractBackupAck {

    private static final long serialVersionUID = 1L;


    public SynchronizationAck(ODSConnection con) {
      super(con);
    }


    @Override
    protected BackupCause getBackupCause() {
      return BackupCause.WAITING_FOR_CAPACITY;
    }


  }


  public static void resumeWaitingOrder(SynchronizationEntry e, final ODSConnection defaultConnection) throws XNWH_RetryTransactionException, XPRC_ResumeFailedException {
    if (e.getOrderId() == null) {
      throw new RuntimeException("Cannot resume order with order id <null>");
    }

    SynchronizationAck ackObject = new SynchronizationAck(defaultConnection);
    try {
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement()
          .resumeOrderAsynchronously(e.getResumeTarget(), ackObject);
    } catch (XPRC_ResumeFailedException e1) {
      throw new RuntimeException("Failed to resume waiting order after notify event. ResumeTarget=" + e.getResumeTarget(), e1);
    } catch (XPRC_OrderEntryCouldNotBeAcknowledgedException e2) {
      if (e2.getCause() instanceof XNWH_RetryTransactionException) {
        throw (XNWH_RetryTransactionException) e2.getCause();
      } else {
        throw new XPRC_ResumeFailedException(e.getResumeTarget().toString(), e2);
      }
    }

  }

  public Collection<SynchronizationEntry> listCurrentSynchronizationEntries() throws PersistenceLayerException {
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
    defCon.ensurePersistenceLayerConnectivity(SynchronizationEntry.class);
    globalCorrelationLockMapLock.writeLock().lock();
    try {
      return algorithm.listCurrentSynchronizationEntries(defCon);
    } finally {
      globalCorrelationLockMapLock.writeLock().unlock();
    }
    } finally {
      try {
        defCon.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection when listing SynchronizationEntries.",e);
      }
    }
  }


  public static long createTimeoutCronLikeOrder(String correlationId, long resumeTime, Long rootOrderId, ODSConnection defaultConnection)
      throws XNWH_RetryTransactionException, XPRC_CronLikeOrderStorageException, XPRC_CronCreationException, XPRC_InvalidCronLikeOrderParametersException {

    TimeoutSynchronizationInput payload = new TimeoutSynchronizationInput(correlationId);
    CronLikeOrderCreationParameter cronParameters =
      new CronLikeOrderCreationParameter(XynaDispatcher.DESTINATION_KEY_TIMEOUT_SYNCHRONIZATION,
                                         resumeTime, null, payload);
        cronParameters.setRootOrderId(rootOrderId);
       
    CronLikeOrder resultingOrder =
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
            .createCronLikeOrder(cronParameters, CronLikeOrderPersistenceOption.REMOVE_ON_SHUTDOWN_IF_NO_CLUSTER, defaultConnection);
    return resultingOrder.getId();
  }


  private class CleanupRunnable implements Runnable {

    private volatile boolean runToggle = false;
    private volatile boolean currentlyRunning = false;
    private volatile CountDownLatch stopLatch;

    private RepeatedExceptionCheck repeatedExceptionCheck = new RepeatedExceptionCheck();
    
    private final Object runToggleMonitor = new Object(); 

    private TimeoutAlgorithm cleanupAlgorithm;

    CleanupRunnable(TimeoutAlgorithm cleanupAlgorithm) {
      this.cleanupAlgorithm = cleanupAlgorithm;
    }
    
    
    public void run() {

      try {

        WarehouseRetryExecutableNoResult cleanupExecutable = new WarehouseRetryExecutableNoResult() {
          public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
            con.ensurePersistenceLayerConnectivity(SynchronizationEntry.class);
            globalCorrelationLockMapLock.writeLock().lock();
            try {
              cleanupAlgorithm.call(con);
            } finally {
              globalCorrelationLockMapLock.writeLock().unlock();
            }
          }
        };

        runToggle = true;
        while (runToggle) {

          synchronized (runToggleMonitor) {
            try {
              runToggleMonitor.wait(10000);
            } catch (InterruptedException e) {
              logger.error("Synchronization manager cleanup thread got interrupted, stopping.", e);
              runToggle = false;
              break;
            }
            currentlyRunning = true;
          }

          try {

            if (!runToggle) {
              break;
            }

            if (dirtyCounter.get() == 0) {
              //              continue;
              // FIXME dont check this here but rather within the actual algorithm. only set to zero if
              //       it is evident that there are no entries left to be watched.
            } else {
              dirtyCounter.set(0);
            }

            if (logger.isTraceEnabled()) {
              logger.trace("Looking for outdated synchronization entries...");
            }
            try {
              WarehouseRetryExecutor
                  .executeWithRetriesNoException(cleanupExecutable, cleanupAlgorithm.getConnectionTypeForCleanup(),
                                                 Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                                 Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                                 new StorableClassList(SynchronizationEntry.class));
              repeatedExceptionCheck.clear();
            } catch (Throwable e) {
              Department.handleThrowable(e);
              boolean repeated = repeatedExceptionCheck.checkRepeated(e);
              if( repeated ) {
                logger.warn( "Warehouse access failed, retrying: "+repeatedExceptionCheck );
              } else {
                logger.warn( "Warehouse access failed, retrying: "+repeatedExceptionCheck, e);
              }
            }

          } finally {

            synchronized (runToggleMonitor) {
              currentlyRunning = false;
            }

          }

        }

        synchronized (runToggleMonitor) {
          if (stopLatch != null) {
            stopLatch.countDown();
          }
        }

      } catch (Throwable e) {
        Department.handleThrowable(e);
        logger.error("Unexpected error in synchronization cleanup thread.", e);
      }

      logger.info("Synchronization Cleanup Thread stopped.");

    }


    public void stopRunning() {
      logger.trace("shutting down SynchronizationCleanupThread");
      this.runToggle = false;
      synchronized (runToggleMonitor) {
        runToggleMonitor.notify();
        if (currentlyRunning) {
          stopLatch = new CountDownLatch(1);
        }
      }
      if (stopLatch != null) {
        try {
          stopLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
      }
    }

  }


  public void timeout(final String correlationId) throws PersistenceLayerException {

    dirtyCounter.incrementAndGet();

    WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {

      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        TimeoutResult result;
        con.ensurePersistenceLayerConnectivity(SynchronizationEntry.class);
        final Lock corrLock =
            new CorrelationIdLock(globalCorrelationLockMapLock, SynchronizationManagement.this.correlationLockMap,
                                  correlationId);
        corrLock.lock();
        try {
          result = algorithm.returnTimedoutEntry(correlationId, con);
        } finally {
          corrLock.unlock();
        }
        if (result != null) {
          try {
            if (result.historyEntry != null) {
              resumeWaitingOrder(result.historyEntry, con);
            } else if (result.defaultEntry != null) {
              resumeWaitingOrder(result.defaultEntry, con);
            }
          } catch (XPRC_ResumeFailedException e) {
            throw new RuntimeException(e);
          }
        }
      }
    };
    WarehouseRetryExecutor.buildCriticalExecutor().
      storable(SynchronizationEntry.class).storable(OrderInstanceBackup.class).storable(OrderInstance.class).
      execute(wre);

  }


  static class CorrelationIdLock implements Lock {

    private final String id;
    private final ReadWriteLock globalCorrelationLockMapLock;
    private final ConcurrentMap<String, ReentrantLock> correlationLockMap;


    public CorrelationIdLock(ReadWriteLock globalLock, ConcurrentMap<String, ReentrantLock> correlationLockMap,
                             String id) {
      this.id = id;
      this.globalCorrelationLockMapLock = globalLock;
      this.correlationLockMap = correlationLockMap;
    }


    public void lock() {
      getCorrelationLock(id);
    }

    public void unlock() {
      releaseCorrelationLock(id);
    }


    private void getCorrelationLock(String correlationId) {
      globalCorrelationLockMapLock.readLock().lock();
      try {
        ReentrantLock newLock;
        boolean alreadyLocked = false;
        synchronized (correlationLockMap) {
          if (logger.isTraceEnabled())
            logger
                .trace("CorrelationLock locking: " + correlationId + " with total locks:" + correlationLockMap.size());

          newLock = correlationLockMap.get(correlationId);

          if (newLock == null) {
            newLock = new ReentrantLock();
            newLock.lock();
            correlationLockMap.put(correlationId, newLock);
            alreadyLocked = true;
          }

        }

        if (!alreadyLocked) {
          boolean locked = false;
          while (!locked) {
            newLock.lock();
            synchronized (correlationLockMap) {
              ReentrantLock potentiallyNewLock = correlationLockMap.get(correlationId);
              if (potentiallyNewLock == newLock) {
                locked = true;
                // fertig
              } else {
                if (potentiallyNewLock == null) {
                  correlationLockMap.put(correlationId, newLock);
                  locked = true;
                } else {
                  newLock.unlock();
                  newLock = potentiallyNewLock;
                }
              }
            }
          }
        }

        if (logger.isDebugEnabled()) {
          logger.trace("Locked root order <" + correlationId + ">");
        }
      } catch (RuntimeException e) {
        globalCorrelationLockMapLock.readLock().unlock();
        throw e;
      }

    }


    private void releaseCorrelationLock(String correlationId) {
      try {
        synchronized (correlationLockMap) {
          if (logger.isTraceEnabled())
            logger.trace("CorrelationLock unlocking: " + correlationId + " with total locks:"
                + correlationLockMap.size());

          // FIXME: potential weak-spot as the first call releases all locks!
          if (!correlationLockMap.containsKey(correlationId)) {
            return;
          }

          boolean releasedAtLeastOne = false;
          while (correlationLockMap.get(correlationId).isHeldByCurrentThread()) {
            releasedAtLeastOne = true;
            correlationLockMap.get(correlationId).unlock();
          }

          // the thread owned the lock before calling unlock. still this is not secure since the lock() method is
          // called outside the synchronized
          if (releasedAtLeastOne) {
            correlationLockMap.remove(correlationId);
          }

          if (logger.isTraceEnabled()) {
            logger.trace("CorrelationLock unlocked: " + correlationId + " with total locks:"
                + correlationLockMap.size());
          }

        }
      } finally {
        globalCorrelationLockMapLock.readLock().unlock();
      }

    }


    public void lockInterruptibly() throws InterruptedException {
      throw new RuntimeException("unsupported");
    }
    public boolean tryLock() {
      throw new RuntimeException("unsupported");
    }
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      throw new RuntimeException("unsupported");
    }


    public Condition newCondition() {
      throw new RuntimeException("unsupported");
    }
  }


  public boolean cleanupOrderFamily(Long rootOrderId, Set<Long> suspendedOrderIds, ODSConnection con)
                  throws PersistenceLayerException {
    return algorithm.cleanupOrderFamily(rootOrderId, suspendedOrderIds, con);
  }

}
