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
package com.gip.xyna.xprc.xpce;



import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable.TriggerInstanceState;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xprc.XynaExecutor;
import com.gip.xyna.xprc.XynaRunnable;



public class EventListenerThread<J extends StartParameter, TC extends TriggerConnection, I extends EventListener<TC, J>> extends Thread {

  private static final Logger logger = CentralFactoryLogging.getLogger(EventListenerThread.class);

  private final EventListenerInstance<J, I> el;
  private volatile boolean running = true;
  private final AtomicInteger processingLimit;


  public EventListenerThread(EventListenerInstance<J, I> el) {
    this(el, -1);
  }
  
  public EventListenerThread(EventListenerInstance<J, I> el, int processingLimit) {

    if (logger.isDebugEnabled()) {
      logger.debug("Creating trigger for " + el.getInstanceName() + "(" + el + ")");
    }
    this.processingLimit = new AtomicInteger(processingLimit);
    
    this.el = el;
    setDaemon(true);
    setPriority(MAX_PRIORITY); // TODO konfigurierbar, am besten auch alle anderen nicht gethreadpoolten threads
    // (scheduler, timeouthandler-thread, cronls, etc)
    setName("EventListener Thread (" + el.getInstanceName() + ")");

  }


  public void run() {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Executing " + EventListenerThread.class.getSimpleName() + " for " + el.getInstanceName());
      }

      while (running) {
        try {
          receiveNext();
          if (processingLimit.get() > 0) {
            if (processingLimit.decrementAndGet() == 0) {
              try {
                logger.info("OrderEntranceLimit reached, terminating processing for " + el.getInstanceName());
                XynaFactory.getInstance().getActivation().getActivationTrigger().disableTriggerInstance(el.getInstanceName(),
                                                                                                        el.getRevision(), false);
              } finally {
                setRunning(false);
              }
            }
          }
        } catch (OutOfMemoryError t) {
          Department.handleThrowable(t);
        }
      }
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("unexpected error during execution of trigger. trigger will be shut down", t);      
      try {
        XynaFactory.getInstance().getActivation().getActivationTrigger().setTriggerInstanceState(
                                        el.getInstanceName(), 
                                        el.getRevision(), 
                                        TriggerInstanceState.ERROR);
        XynaFactory.getInstance().getActivation().getActivationTrigger().setTriggerInstanceError(
                                        el.getInstanceName(), 
                                        el.getRevision(),
                                        t);
      } catch (Throwable t1) {
        Department.handleThrowable(t1);
        logger.error("unexpected error while changing trigger state.", t1);
      }
      try {
        el.getEL().stop();
      } catch (Throwable t1) {
        Department.handleThrowable(t1);
        logger.error("unexpected error during shut down of trigger.", t1);
      }
    }
  }

  private int cntNull = 0;

  private void receiveNext() {    
    TC tc = el.getEL().receiveNext();
    if (tc != null) {
      cntNull = 0;
      XynaRunnable child = new FilterProcessingRunnable(el.getEL(), tc);
      // at this point we don't know the priority yet, so draw from a mixed pool and set
      // the priority later if required
      if (!running) {
        //trigger wurde bereits gestopped
        child.rejected(); //TODO cause übergeben?
      } else {
        try {
          XynaExecutor.getInstance().executeRunnableWithUnprioritizedPlanningThreadpool(child);
        } catch (RejectedExecutionException e) {
          child.rejected();
        }
      }
    } else {
      cntNull++;
      //kein Event erhalten: dies ist nicht normal, da in receiveNext() solange gewartet wird, 
      //bis ein Event erhalten wurde. Entweder wird der EventListener gestoppt oder er muss gedrosselt 
      //werden, damit er nicht zuviele Events annimmt
      if (running && cntNull > 10) {
        // => warten, dann wieder receiveNext() rufen
        int sleepTime = Math.min(100, Math.max(0, (cntNull - 10) * 5));
        try {
          Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }


  public EventListenerInstance getEventListener() {
    return el;
  }


  public void setRunning(boolean r) {
    running = r;
  }


  public boolean isRunning() {
    return running;
  }
  
  
  public boolean isProcessingLimited() {
    return processingLimit.get() >= 0;
  }

}
