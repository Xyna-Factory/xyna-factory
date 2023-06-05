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
package com.gip.xyna.xprc.xfractwfe.base.parallel;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.parallel.ParallelExecutor;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.parallel.FractalWorkflowParallelExecutor.ParallelismLimitation;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;


/**
 * Thread-Beschränkung mit blockierten Tasks: Solange der Task suspendiert ist, steht dem ParallelExecutor ein 
 * Thread weniger zu Verfügung.
 * 
 * Dies ist beispielsweise sinnvoll, wenn zuwenig Capacities zu Verfügung stehen, so dass weitere Threads 
 * mit ihrem synchronen Subworkflow-Aufruf durch das Warten auf die Capacity blockiert würden. Dann sollen diese 
 * Threads besser erst gar nicht nicht gestartet werden.
 * 
 * Über {@link #awaitLimitationNotZero()} wartet der FractalWorkflowParallelExecutor blockierend solange, bis 
 * ein Thread ausgeführt werden darf.
 * 
 */
public abstract class AbstractBlockingParallelismLimitation implements ParallelismLimitation<FractalProcessStep<?>> {

  private static Logger logger = CentralFactoryLogging.getLogger(AbstractBlockingParallelismLimitation.class);
  
  private ParallelExecutor parallelExecutor;
  private volatile CountDownLatch awaitNotZero;
  private volatile HashSet<String> blockingLaneIds;
  protected AtomicInteger maxThreadLimit = new AtomicInteger(); //ThreadLimit, welches von der Subklasse bestimmt wird
  protected AtomicInteger threadLimit = new AtomicInteger(); //tatsächliches ThreadLimit unter Berücksichtigung der blockierten Lanes
  
  public void setParallelExecutor(ParallelExecutor parallelExecutor) {
    this.parallelExecutor = parallelExecutor;
    setMaxThreadLimit( getMaxThreadLimit() );
  }

  public abstract int getMaxThreadLimit();
  
  public abstract boolean shouldLaneBeBlocked(ProcessSuspendedException suspendedException);
  
  public void handleProcessSuspendedException(SuspendableParallelTask<FractalProcessStep<?>> suspendableParallelTask,
                                              ProcessSuspendedException suspendedException) {
    if( shouldLaneBeBlocked(suspendedException) ) {
      changeThreadLimit(-1); //ThreadLimit verringern
      addBlockingLaneId(suspendableParallelTask.getLaneId());
      if (logger.isDebugEnabled()) {
        logger.debug("Suspended blocking lane " + suspendableParallelTask.getLaneId() + ".");
      }
    }
  }


  public void addTaskToParallelExecutor(SuspendableParallelTask<FractalProcessStep<?>> taskToResume) {
    boolean isBlocking = removeBlockingLaneId(taskToResume.getLaneId());
    if (isBlocking) {
      taskToResume.setPriority(SuspendableParallelTask.PRIORITY_RESUME_BLOCKED);
      parallelExecutor.addTask(taskToResume);
      changeThreadLimit(1); //ThreadLimit wieder erhöhen
      if (logger.isDebugEnabled()) {
        logger.debug("Resuming blocking lane " + taskToResume.getLaneId() + ". threadlimit=" + parallelExecutor.getThreadLimit()
            + ", taskconsumerCnt=" + parallelExecutor.currentlyExecutingTasks());
      }
    } else {
      taskToResume.setPriority(SuspendableParallelTask.PRIORITY_RESUME);
      parallelExecutor.addTask(taskToResume);
    }
  }

  protected void setMaxThreadLimit(int newMaxTL ) {
    int oldMTL = 0;
    int delta = 0;
    do {
      oldMTL = maxThreadLimit.get();
      delta = newMaxTL - oldMTL;
    } while( ! maxThreadLimit.compareAndSet(oldMTL, newMaxTL) );
    changeThreadLimit(delta);
  }
 
  private void changeThreadLimit(int delta) {
    threadLimit.addAndGet(delta);
    if( awaitNotZero != null ) {
      if( threadLimit.get() > 0 ) {
        if( logger.isDebugEnabled() ) {
          logger.debug("notify limitation not 0");
        }
        awaitNotZero.countDown();
      }
    }
    synchronized (this) { 
      //synchronized, damit zwischen dem lesen und schreiben nichts passieren kann. ansonstne könnte die reihenfolge sein
      //thread1 liest threadlimit
      //thread2 ändert threadlimit
      //thread2 schreibt threadlimit in PE
      //thread1 schreibt falsches threadlimit in PE
      parallelExecutor.setThreadLimit(threadLimit.get());
    }
  }

  private synchronized void addBlockingLaneId(String laneId) {
    if( blockingLaneIds == null ) {
      blockingLaneIds = new HashSet<String>();
    }
    blockingLaneIds.add(laneId);
  }

  private synchronized boolean removeBlockingLaneId(String laneId) {
    if( blockingLaneIds == null ) {
      return false;
    }
    return blockingLaneIds.remove(laneId);
  }

  public boolean awaitLimitationNotZero() {
    if( threadLimit.get() > 0 ) {
      changeThreadLimit(0); //sollte nicht vorkommen, sicherheitshalber nochmal ThreadLimit im ParallelExecutor setzen
      return true;
    } else {
      //TODO wenn alle slots blockiert sind, sollte man hier nicht ewig warten, sondern der fractalworkflowparallelexecutor sollte sich beenden/suspendieren.
      //TODO später: wenn z.b. die kardinalität der kapazität vergrößert wird = maxthreadlimit vergrößert => suspendierten auftrag aufwecken!
      awaitNotZero = new CountDownLatch(1);
      try {
        if( logger.isDebugEnabled() ) {
          logger.debug("awaiting limitation not 0");
        }
        awaitNotZero.await();
      } catch (InterruptedException e) {
        //dann halt nicht mehr warten
        return false;
      } finally {
        awaitNotZero = null;
      }
      return true;
    }
  }

  
  
}
