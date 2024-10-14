/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.utils.exceptions.XynaException;

public class OOMManagement extends FunctionGroup {
  
  public static final String DEFAULT_NAME = "OutOfMemoryManagement";

  private List<Thread> suspendedThreads;
  private MemoryReserveReclaimer reclaimer;
  private final static Logger logger = CentralFactoryLogging.getLogger(OOMManagement.class);

  private int totalSuspensions;
  private int totalResumes;
  private int oomOnLog;
  private int otherErrorsOnLog;
  private int suspensionErrors; //threads that could not be queued
  
  public OOMManagement() throws XynaException {
    super();
  }
  
  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  protected void init() throws XynaException {
    suspendedThreads = new ArrayList<Thread>(50);
    reclaimer = new MemoryReserveReclaimer(this::onMemoryAllocated);
    reclaimer.start();
  }

  @Override
  protected void shutdown() throws XynaException {
    if(logger.isDebugEnabled()) {
      logger.debug("Shutting down " + DEFAULT_NAME + ". Calling interrupt on " + suspendedThreads.size() + " suspended threads.");
    }
    if(reclaimer != null) {
      reclaimer.running = false;
      reclaimer.interrupt();
    }
    
    for(Thread t : suspendedThreads) {
      t.interrupt();
    }
  }
  
  /**
   * Called by an outside thread
   */
  public void handleThrowable(Throwable t) {
    // http://java.sun.com/j2se/1.4.2/docs/api/java/lang/Error.html
    if (t instanceof ThreadDeath) { // ThreadDeath fehler müssen weitergeworfen werden!
      throw (ThreadDeath) t;
    }
    
    if (t instanceof OutOfMemoryError) {
      Thread thread = Thread.currentThread();
      try {
        synchronized (suspendedThreads) {
          suspendedThreads.add(thread);
          totalSuspensions++;
        }
        reclaimer.freeMemoryReserve();
      } catch(OutOfMemoryError e) {
        suspensionErrors++;
        try {
          Thread.sleep(3000l);
        } catch (InterruptedException e1) {
        }
      }
      try {
        logger.fatal("Out of memory", t);
        if (logger.isDebugEnabled()) {
          logger.debug("suspended");
          logger.debug(thread.getName());
        }
      } catch (Throwable e) {
        countErrorOnLog(e);
      }

      while (checkContains(suspendedThreads, thread)) {
        try {
          Thread.sleep(30000l); //interrupt comes from OOMManagement::onMemoryAllocated
        } catch (InterruptedException e) {
          try {
            totalResumes++;
            if (logger.isDebugEnabled()) {
              logger.debug("Resumed " + thread.getName());
            }
          } catch (Throwable th) {
            countErrorOnLog(th);
          }
        }
      }
    }
  }
  
  private boolean checkContains(List<Thread> threads, Thread thread) {
    synchronized (threads) {
      return threads.contains(thread);
    }
  }
  
  private void countErrorOnLog(Throwable t) {
    if(t instanceof OutOfMemoryError) {
      oomOnLog++;
    } else {
      otherErrorsOnLog++;
    }
  }
  
  private void onMemoryAllocated() {
    synchronized (suspendedThreads) {
     if(!suspendedThreads.isEmpty()) {
       Thread resumeThread = suspendedThreads.get(0);
       suspendedThreads.remove(0);
       reclaimer.freeMemoryReserve();
       try {
         if (logger.isDebugEnabled()) {
           logger.debug("Resuming " + resumeThread.getName());
         }
       } catch (Throwable t) {
         countErrorOnLog(t);
       }
       resumeThread.interrupt();
     }
    }
  }
  


  /**
   * Manages a memoryReserve. If it is freed (by someone callingfreeMemoryReserve),
   * it tries to reallocate it (allocateMemoryReserve). After successful reallocation,
   * it calls the reservedMemCallback runnable.
   * 
   * During normal operation, memoryReserve is set and the Thread is waiting.
   */
  private static class MemoryReserveReclaimer extends Thread {

    private boolean running;
    private int[] memoryReserve;
    private Runnable reservedMemCallback;
    
    public MemoryReserveReclaimer(Runnable reservedMemoryCallback) {
      running = true;
      this.reservedMemCallback = reservedMemoryCallback;
      this.setName("MemoryReserveClaimer");
    }
    
    
    public synchronized void freeMemoryReserve() {
        memoryReserve = null;
    }
    
    private synchronized boolean allocateMemoryReserve() {
      if(memoryReserve == null) {
        memoryReserve = new int[256 * 1024];
        return true;
      }
      return false;
    }
    
    @Override
    public void run() {
      boolean allocatedMemoryThisRound = false;
      while(running) {
        try {
          allocatedMemoryThisRound = allocateMemoryReserve();
          if(allocatedMemoryThisRound) {
            reservedMemCallback.run();
          }
        } catch(OutOfMemoryError e) {
        }
        try {
          Thread.sleep(5000l);
        } catch (InterruptedException e1) {
        }
      }
    }
  }
  
  public int getTotalSuspensions() {
    return totalSuspensions;
  }

  
  public int getTotalResumes() {
    return totalResumes;
  }

  
  public int getOomOnLog() {
    return oomOnLog;
  }
  
  public int getOtherErrorsOnLog() {
    return otherErrorsOnLog;
  }
  
  public int getSuspensionErrors() {
    return suspensionErrors;
  }
}
