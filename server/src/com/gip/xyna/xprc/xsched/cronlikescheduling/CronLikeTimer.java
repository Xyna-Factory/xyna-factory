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

package com.gip.xyna.xprc.xsched.cronlikescheduling;



import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.util.ManagedPlainThreadFromPausableRunnable;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.exceptions.XNWH_TooManyDedicatedConnections;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.DedicatedConnection;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.XynaExecutor;



public class CronLikeTimer extends ManagedPlainThreadFromPausableRunnable {

  public static final String CRON_LIKE_TIMER = "CronLikeTimer";
  public final static String CRONLIKETIMER_THREAD_NAME = "CronLikeTimer Thread";
  private static final Logger logger = CentralFactoryLogging.getLogger(CronLikeTimer.class);
  
  
  private final CronLikeOrderQueue cronQueue;
  private final CronLikeScheduler cronScheduler;
  
  
  public CronLikeTimer(CronLikeScheduler cronLikeScheduler) {
    super(CRONLIKETIMER_THREAD_NAME);
    cronQueue = new CronLikeOrderQueue(XynaProperty.CRON_LIKE_TIMER_UPPER_BOUND.get(), new CronLikeOrderComparator());
    cronScheduler = cronLikeScheduler;
    runToggle = false;
    
    XynaFactory
    .getInstance()
    .getFactoryManagementPortal()
    .getXynaFactoryControl()
    .getDependencyRegister()
    .addDependency(DependencySourceType.XYNAPROPERTY, XynaProperty.CRON_LIKE_TIMER_UPPER_BOUND.getPropertyName(),
                   DependencySourceType.XYNAFACTORY, CRON_LIKE_TIMER);
    
    try {
      CentralComponentConnectionCache.getInstance().openCachedConnection(ODSConnectionType.DEFAULT,
                                                                         DedicatedConnection.CronLikeScheduler,
                                                                         new StorableClassList(CronLikeOrder.class));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e.getMessage(), e);
    } catch (XNWH_TooManyDedicatedConnections e) {
      throw new RuntimeException("Connection limit exceeded while trying to open dedicated connection for CronLikeTimer.", e);
    }

  }
  
  /**
   * Do work
   */
  protected void runOnce() throws Exception {
    boolean empty;
    synchronized (cronQueue) {
      empty = cronQueue.isEmpty();
      if (empty) {
        //readAllFromDBFlag kann hier auf true gesetzt werden, weil gleich alle (bzw. so viele
        //wie maximal in die Queue passen) Crons aus der Datenbank ausgelesen werden.
        //Werden zwischenzeitlich von anderen Threads neue Crons in die Queue eingestellt,
        //so werden diese wieder verdrängt, wenn nicht genügend Platz ist und der
        //Ausführungszeitpunkt zu weit in der Zukunft liegt. Kann ein Cron nicht eingefügt werden,
        //wird das Flag auf false gesetzt.
        setReadAllFromDBFlag(true);
      }
    }
    
    if (empty) {
      if(logger.isTraceEnabled()) {
        logger.trace("Load cron like ordes from database.");
      }
      // load as many orders from persistence layer as fit in queue
      WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {

        public void executeAndCommit(ODSConnection internalConnection) throws PersistenceLayerException {

          if (logger.isTraceEnabled()) {
            logger.trace( "cronQueue is empty, trying to load " + ( XynaProperty.CRON_LIKE_TIMER_UPPER_BOUND.get()) + " items" );
          }
          
          cronScheduler.readNextFromPersistenceLayer(internalConnection, XynaProperty.CRON_LIKE_TIMER_UPPER_BOUND.get());
          
          internalConnection.commit();
        } //warehouseretryexecutable.execute

      };

      try {
        WarehouseRetryExecutor.buildCriticalExecutor().
          connectionDedicated(DedicatedConnection.CronLikeScheduler).
          storable(CronLikeOrder.class).
          execute(wre);
      } catch (XNWH_RetryTransactionException rte) {
        throw new IllegalStateException("DB not reachable", rte); // DB not reachable, interrupt normal operation
      } catch (PersistenceLayerException ple) {
        logger.debug("Failed to read new CronLikeOrders from Queue.", ple);
        throw ple;
      }
    }
    
    synchronized (cronQueue) {
      if (!runToggle) {
        return; //CronLikeTimer soll beendet werden
      }
      // wait for new orders if there is nothing to do
      if (cronQueue.isEmpty()) {
        logger.debug("No cron like orders pending, waiting for input...");
        try {
          cronQueue.wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      
      // woken up: check next order
      CronLikeOrder next = cronQueue.peek();

      if (next != null) {   
        if (logger.isTraceEnabled()) {
          logger.trace("Retrieved valid cron like order with id <" + next.getId() + "> from queue.");
        }

        if (next.isExecutionTimeReached()) {
          logger.trace("Execution time is reached.");
          // Verhindern, dass diese CLO wieder aus der DB geladen wird.
          // Das unmark findet in DefaultCronLikeOrderStartUnderlyingOrderAlgorithm nach der DB-Transaktion statt. 
          markAsNotToSchedule(cronQueue.poll().getId());
          try {
            startUnderlyingOrder(next);
          } catch (XNWH_RetryTransactionException rte) {
            throw new IllegalStateException("DB not reachable", rte); // DB not reachable, interrupt normal operation
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        } else {
          long untilNextExecution = next.getNextExecution() - System.currentTimeMillis();
          if (logger.isDebugEnabled()) {
            logger.debug("Waiting " + untilNextExecution + "ms for cron like order " + next.getId());
          }
          if (untilNextExecution > 0) {
            try {
              cronQueue.wait(untilNextExecution);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
          if (logger.isDebugEnabled()) {
            //auftrag ist aber nicht mehr notwendigerweise in der queue - evtl wurde er bereits gelöscht.
            //das sieht man dann daran, ob er danach gestartet wird
            logger.debug("Got notified while waiting or finished waiting for cron like order " + next.getId());
          }
        }
      }
    } // synchronized (queue)
  }
  
  
  @Override
  public boolean stop() {
    if (!runToggle) {
      return false;
    }
    runToggle = false;
    synchronized (cronQueue) {
      cronQueue.notifyAll();
    }
    return waitForTermination();
  }
  
  
  protected ErrorHandling handle(Throwable t) {
    if (t instanceof PersistenceLayerException) {
      return ErrorHandling.BACKOFF;
    }
    return ErrorHandling.ABORT;
  }
  
  
  /**
   * Start cron like order execution.
   */
  private void startUnderlyingOrder(final CronLikeOrder next) throws InterruptedException, XNWH_RetryTransactionException {

    if (XynaFactory.getInstance().isShuttingDown()) {
      if (logger.isInfoEnabled()) {
        logger.info("Aborting " + getClass().getSimpleName() + " execution because factory is shutting down");
      }
      return;
    }
    
    if(logger.isTraceEnabled()) {
      logger.trace("startUnderlyingOrder for cron like order with id <" + next.getId() + ">");
    }
    if (next.isSingleExecution()) {
      next.setStatus(CronLikeOrderStatus.RUNNING_SINGLE_EXECUTION);
    } else {
      next.setStatus(CronLikeOrderStatus.RUNNING);
    }

    boolean started = false;
    int cnt = 0;
    while (!started && runToggle) {
      try {
        XynaExecutor.getInstance().executeRunnableWithUnprioritizedPlanningThreadpool(next.getPlanningRunnable());
        started = true;
      } catch (final RejectedExecutionException e) {
        //was anderes als retries macht hier keinen sinn, weil ansonsten der cron-timer gar keinen sinn mehr macht
        if (cnt % 100 == 0) {
          //alle 10 sek loggen
          logger.warn("Could not start cronlike order because threadpool is busy. Will retry shortly.");
          if (logger.isDebugEnabled()) {
            logger.debug(null, e);
          }
        }
        cnt++;
        cronQueue.wait(100); //gibt das lock frei, damit andere threads nicht warten müssen
      }
    }
  }
  
  
  /**
   * Order wird nicht zur Scheduling-Queue hinzugefügt. Wenn sie schon drin sind, wird sie dort entfernt. 
   */
  public void markAsNotToScheduleAndRemoveFromQueue(Long orderId) {
    synchronized (cronQueue) {
      if(logger.isTraceEnabled()) {
        logger.trace("markAsNotToScheduleAndRemoveFromQueue for cron like order with id <" + orderId + ">");
      }
      cronQueue.markCronLikeOrderAsNotToSchedule(orderId);
      cronQueue.removeFromQueue(orderId);
      
      if(cronQueue.size() == 0) {
        cronQueue.notify();
      }
    }
  }
  
  /**
   * Queue komplett leeren und neu aus DB befüllen ...
   */
  public void recreateQueue() {
    synchronized (cronQueue) {
      logger.trace("Clear memory queue and refill it from database.");
      cronQueue.clear();
      cronQueue.notify();
    }
  }
  
  /**
   * 
   * @param binding, Binding, welches von der Queue verarbeitet werden soll. Wenn NULL, wird Binding ignoriert.
   */
  public void changeToBinding(Integer binding) {
    synchronized (cronQueue) {
      if(logger.isDebugEnabled()) {
        logger.debug("Change the CronLikeTimer to schedule only cron like orders with binding <" + binding + ">");
      }
      cronQueue.changeToBinding(binding);
      cronQueue.notify();
    }
  }
  
  /**
   * Order wird nicht zur Scheduling-Queue hinzugefügt. Wenn sie schon drin sind, ist es zu spät und sie werden dennoch ausgeführt. 
   */
  public void markAsNotToSchedule(Long orderId) {
    synchronized (cronQueue) {
      if(logger.isTraceEnabled()) {
        logger.trace("markAsNotToSchedule for cron like order with id <" + orderId + ">");
      }
      cronQueue.markCronLikeOrderAsNotToSchedule(orderId);
      
    }
  }
  
  public void unmarkAsNotToSchedule(Long orderId) {
    synchronized (cronQueue) {
      if(logger.isTraceEnabled()) {
        logger.trace("unmarkAsNotToSchedule for cron like order with id <" + orderId + ">");
      }
      cronQueue.unmarkCronLikeOrderAsNotToSchedule(orderId);
    }
  }
  
  public void tryAddNewOrders(Collection<CronLikeOrder> orders) {
    synchronized (cronQueue) { // stopps the execution to add a new CLO into the queue. otherwise the CLO may be running
      // while it is not yet stored in the DB via writeMemoryEntriesToPersistenceLayerIfNecessary
      
      Iterator<CronLikeOrder> iter = orders.iterator();
      int added = 0;
      int maxQueueLengthUpperBound = XynaProperty.CRON_LIKE_TIMER_UPPER_BOUND.get();
      while(iter.hasNext()) {
        CronLikeOrder order = iter.next();
        cronQueue.unmarkCronLikeOrderAsNotToSchedule(order.getId());
        if (order.getNextExecution() <= cronQueue.getLatestExecutionTimeInQueue()) {
          //muss früher gestartet werden als der auftrag mit der längsten wartezeit in der queue
          cronQueue.addCronLikeOrderToQueue(order);
          added++;
        } else if(cronQueue.size() < maxQueueLengthUpperBound) {
          cronQueue.addCronLikeOrderToQueue(order);
          added++;
        }
      }      

      if(logger.isDebugEnabled()) {
        logger.debug("Add " + added + " cron like orders to memory queue.");
      }
      
      if(cronQueue.size() > maxQueueLengthUpperBound) {
        // Queue kürzen ... letztes Element löschen
        cronQueue.shrink(maxQueueLengthUpperBound);
        setReadAllFromDBFlag(false);
      }
      
      cronQueue.notify();
    }
  }
  
  public boolean tryAddNewOrderWithoutUnmarkOrder(CronLikeOrder order, boolean addFromPersistenceLayer) {
    synchronized (cronQueue) { // stopps the execution to add a new CLO into the queue. otherwise the CLO may be running
      // while it is not yet stored in the DB via writeMemoryEntriesToPersistenceLayerIfNecessary
      
      if(logger.isTraceEnabled()) {
        logger.trace("tryAddNewToQueue cron like order with id <" + order.getId() + ">");
      }
      
      boolean added;
      int maxQueueLengthUpperBound = XynaProperty.CRON_LIKE_TIMER_UPPER_BOUND.get();
      if (order.getNextExecution() <= cronQueue.getLatestExecutionTimeInQueue()) {
        //muss früher gestartet werden als der auftrag mit der längsten wartezeit in der queue
        added = cronQueue.addCronLikeOrderToQueue(order, addFromPersistenceLayer);
      } else if(cronQueue.size() < maxQueueLengthUpperBound) {
        added = cronQueue.addCronLikeOrderToQueue(order, addFromPersistenceLayer);
      } else {
        added = false;
      }
      
      if ( logger.isTraceEnabled() ) {
        logger.trace( "queue filled " + cronQueue.size() + " (" + ((added) ? 1 : 0) + ")" );
      }
      
      if(added) {
        logger.trace("Adding cron like orders to memory queue.");
        
        if(cronQueue.size() > maxQueueLengthUpperBound) {
          // Queue kürzen ... letztes Element löschen
          logger.trace("Shrink Queue to fit the correct size.");
          cronQueue.shrink(maxQueueLengthUpperBound);
          setReadAllFromDBFlag(false);
        }
        
        cronQueue.notify();
      } else {
        //wenn die Queue leer ist, müssen neue Crons aus der DB gelesen werden
        //dazu den CronLikeTimer aufwecken
        if (cronQueue.isEmpty()) {
          cronQueue.notify();
        }
      }
      
      return added;
    }
  }
  
  
  public boolean tryAddNewOrder(CronLikeOrder order) {
    

    if(logger.isTraceEnabled()) {
      logger.trace("tryAddNewOrder (with unmarkCronLikeOrderAsNotToSchedule) cron like order with id <" + order.getId() + ">");
    }
    synchronized (cronQueue) { // stopps the execution to add a new CLO into the queue. otherwise the CLO may be running
      // while it is not yet stored in the DB via writeMemoryEntriesToPersistenceLayerIfNecessary
      
      cronQueue.unmarkCronLikeOrderAsNotToSchedule(order.getId());
      if (!order.isEnabled()) {
        if (logger.isTraceEnabled()) {
          logger.trace("Cron like order not added to memory queue, order is disabled.");
        }
        return false;
      }
      boolean added = tryAddNewOrderWithoutUnmarkOrder(order, false);
      if(!added) {
        setReadAllFromDBFlag(false);
      }
      return added;
    }
  }
  
  
  public List<CronLikeOrder> getQueueList() {
    synchronized (cronQueue) {
      return cronQueue.getQueue();
    }
  }
  
  public void stopWorking() {
    
    synchronized (cronQueue) { // synchrnonizes on runToggle to provide the state change
      logger.trace("Stop working CronLikeTimer.");
      runToggle = false;
      cronQueue.notify();
    }
  }
  
  public Object getBlockingObject() {
    return cronQueue;
  }
  
  protected void setReadAllFromDBFlag(boolean readAllFromDBFlag) {
    if(logger.isTraceEnabled()) {
      logger.trace("Set readAllFromDBFlag to " + readAllFromDBFlag); 
    }
    cronQueue.setReadAllFromDBFlag(readAllFromDBFlag);    
  }
  
  
  /**
   * Startet das Einsammeln der Crons, die bereits aus der DB gelöscht / geändert wurden
   * und setzt cleared auf false.
   */
  public void prepareReadNext() {
    cronQueue.prepareReadNext();
  }

  /**
   * Stoppt das Einsammeln der Crons, die bereits aus der DB gelöscht /geändert wurden.
   */
  public void finishReadNext() {
    cronQueue.finishReadNext();
  }
  
  
  public boolean isCleared() {
    return cronQueue.isCleared();
  }

}
