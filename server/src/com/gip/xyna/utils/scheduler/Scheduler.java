/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.utils.scheduler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.TaggedOrderedCollection;
import com.gip.xyna.utils.scheduler.SchedulerInformationBuilder.Timestamp;
import com.gip.xyna.utils.scheduler.UrgencyOrderList.Urgency;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xprc.xsched.Algorithm;


/**
 * 
 * Aus Auftragsdaten eines Auftrags (üblicherweise Priority und Wartezeit) wird eine einzige Zahl 
 * "Urgency" berechnet. 
 * Mit dieser Urgency ist die Reihenfolge festgelegt, in der versucht wird, die Auftraege
 * zu schedulen. Der Vorteil dieser Urgency gegenueber einer Sortierung durch den 
 * konfigurierbaren Comparator wie bei den anderen SchedulerAlgorithm-Implementierungen ist,
 * dass dieses Kriterium (als Zahl ausgedrueckt) auf andere Schedulerknoten transferiert und
 * dort einsortiert werden kann.
 */
public class Scheduler<O,I> implements Algorithm {

  
  static Logger logger = CentralFactoryLogging.getLogger(Scheduler.class);
  
  private SchedulerCustomisation<O,I> customisation; 
  private BlockingQueue<Urgency<O>> entranceQueue;
  private BlockingQueue<Urgency<O>> reorderQueue;
  
  private ReentrantLock schedulingLock = new ReentrantLock();
  private volatile boolean currentlyScheduling = false;
  private UrgencyOrderList<O> urgencyOrders = new UrgencyOrderList<O>();
  private I lastInformation;
  private SchedulerInformationBuilder<I> informationBuilder;
  private long totalSchedulerRuns;
  private int loopFailedCounter;
  private final SleepCounter sleepCounter = new SleepCounter(2, 500, 10, TimeUnit.MILLISECONDS, true);
  
  public Scheduler( SchedulerCustomisation<O,I> customisation) {
    this.customisation = customisation;
    Pair<BlockingQueue<Urgency<O>>, BlockingQueue<Urgency<O>>> queues = 
        customisation.createQueues();
    this.entranceQueue = queues.getFirst();
    this.reorderQueue = queues.getSecond();
    this.customisation.setScheduler(this);
    this.informationBuilder = customisation.getInformationBuilder();
    this.totalSchedulerRuns = 0;
  }
  
  public void putOrder(O order) throws InterruptedException {
    entranceQueue.put(createUrgency(order));
  }
  
  public boolean offerOrder(O order) {
    return entranceQueue.offer(createUrgency(order));
  }
  
  public void putReorderOrder(O order) throws InterruptedException {
    reorderQueue.put(createUrgency(order));
  }
  
  public boolean offerReorderOrder(O order) {
    return reorderQueue.offer(createUrgency(order));
  }
  
  public I getSchedulerInformation() {
    return lastInformation;
  }
  
  public UrgencyOrderList<O> getUrgencyOrderList() { //TODO sollte intern bleiben
    return urgencyOrders;
  }
  
  public boolean isCurrentlyScheduling() {
    return currentlyScheduling;
  }
  
  /**
   * Runnable wird ausgeführt, während Scheduler pausiert: konsistente Daten 
   * @param r
   */
  public void executeExclusively(Runnable r) {
    schedulingLock.lock();
    try {
      r.run();
    } finally {
      schedulingLock.unlock();
    }
  }
  
  public void exec() {
    do {
      //solange schedulen, bis Queues leer sind
      trySchedule();
    } while( ! ( entranceQueue.isEmpty() && reorderQueue.isEmpty() ) );
    //Queues sind leer: nun warten, bis Scheduler von außen geweckt wird
  }
  
  
  public void trySchedule() {
    informationBuilder.timestamp(Timestamp.Start);
    totalSchedulerRuns++;
    informationBuilder.schedulerRunNumber(totalSchedulerRuns);
    customisation.beginScheduling();
    try {
      schedulingLock.lock();
      try {
        currentlyScheduling = true;
        tryScheduleLocked();
        currentlyScheduling = false;
      } finally {
        schedulingLock.unlock();
      }
      informationBuilder.timestamp(Timestamp.Finished);
      customisation.endScheduling();
      lastInformation = informationBuilder.build();
    } catch( Throwable t ) {
      Department.handleThrowable(t);
      customisation.handleThrowable(t);
      lastInformation = informationBuilder.build();
    }
  }

  private Urgency<O> createUrgency(O order) {
    long urgency = customisation.calculateUrgency(order);
    long orderd = customisation.getOrderId(order);
    return new Urgency<O>(orderd, order, urgency);
  }
  
  private void tryScheduleLocked() {
    try {
      //Sammeln der neuen Aufträge
      entranceQueue.drainTo(urgencyOrders);

      //Wiedereinstellen umzuordnender Aufträge
      if( ! reorderQueue.isEmpty() ) {
        urgencyOrders.reorder(reorderQueue);
      }

      customisation.preparation();
      informationBuilder.timestamp(Timestamp.PreparationFinished);

      //Schleife über alle zu schedulenden Aufträge
      boolean loopEndedRegularily = loopOverUrgencyOrders();
      informationBuilder.loopEndedRegularily(loopEndedRegularily);

      informationBuilder.timestamp(Timestamp.SchedulingFinished);

      //leere TagListen entfernen, damit diese nicht Performance kosten
      urgencyOrders.removeEmptyTagLists();

      customisation.postparation();
      if (!loopEndedRegularily) {
        //endlosschleife von nicht erfolgreichen scheduler-läufen verlangsamen, damit sich system besser erholen kann
        loopFailedCounter ++;
        if (loopFailedCounter == 10) {
          sleepCounter.reset();
        }
        if (loopFailedCounter > 10) {
          try {
            sleepCounter.sleep();
          } catch (InterruptedException e) {
          }
        }
      } else {
        loopFailedCounter = 0;
      }
    } finally {
      informationBuilder.waitingForTags(urgencyOrders.getWaitingForTags() );
      informationBuilder.waitingForTagNull(urgencyOrders.getWaitingForTag(null) );
      informationBuilder.waitingForUnknown(urgencyOrders.getUntaggedSize() ); 
    }
  }

  private boolean loopOverUrgencyOrders() {
    TaggedOrderedCollection<Urgency<O>>.Iterator iter = urgencyOrders.iterator();
    int iteratedCounter = 0;
    int scheduled = 0;
    Urgency<O> uo = null;
    try {
      while (iter.hasNext() ) {
        //neue UrgencyOrder holen
        uo = iter.next();
        ++iteratedCounter;
        
        customisation.beforeScheduling( uo.getUrgency() );
       
        //Versuch, aktuelle Order zu schedulen
        ScheduleResult scheduleResult = customisation.trySchedule(uo);
        if (logger.isTraceEnabled()) {
          logger.trace("Scheduling result for <" + uo + "> is <" + scheduleResult + ">");
        }
        if (scheduleResult == null) {
          logger.warn("Unexpected scheduling result null for order " + customisation.getOrderId(uo.getOrder()));
          //scheduleResult = ScheduleResult.Error;//FIXME besser?
          iter.remove(); //einfach ignorieren ist eigentlich falsch
          continue;
        }
        
        boolean remove = scheduleResult.getType().hasToRemove();
        try {
          switch( scheduleResult.getType() ) {
            case Scheduled:
              ++scheduled;
              break;
            case Tag:
              iter.tag(scheduleResult.getTag());
              if( scheduleResult.isHide() ) {
                //es werden keine weiteren Urgency<O>s versucht zu schedulen, 
                //die dieses Tag ebenfalls benötigen. (Urgency<O>s müssen bereits getaggt sein) 
                iter.hide(scheduleResult.getTag());
              }
              break;
            case BreakLoop:
              logger.info("Scheduling result for <" + uo + "> is <BreakLoop>");
              return false;
            case Reorder:
              //Remove wird gleich gemacht, daher direkt neu in entranceQueue einstellen, nicht in reorderQueue
              uo.setUrgency( customisation.calculateUrgency(uo.getOrder() ) );
              if( ! entranceQueue.offer(uo) ) {
                logger.warn( "Could not reorder "+uo.getOrder()+": offer failed");
                remove = false; //offer hat nicht geklappt, daher falsch einsortiert lassen
              }
              break;
            case Continue :
              break;
            case Remove :
              break;
          }
        } finally {
          if( remove ) {
            iter.remove();
          }
        }
      }
      return true; //Schleife regulär durchlaufen
    } finally {
      informationBuilder.iteratedOrders(iteratedCounter);
      informationBuilder.scheduledOrders(scheduled);
    }
  }

}
