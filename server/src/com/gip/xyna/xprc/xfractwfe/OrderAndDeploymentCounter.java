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
package com.gip.xyna.xprc.xfractwfe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.ArrayRingBuffer;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.local.RemoteOrderResponseListener;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xprc.exceptions.XPRC_TimeoutWhileWaitingForUnaccessibleOrderException;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalcleanup.FractalCleanupProcessor;
import com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalexecution.FractalExecutionProcessor;
import com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalplanning.FractalPlanningProcessor;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.execution.ExecutionDispatcher;
import com.gip.xyna.xprc.xpce.execution.MasterWorkflowPostScheduler;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.ordersuspension.ResumeOrderJavaDestination;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendAllOrdersJavaDestination;
import com.gip.xyna.xprc.xsched.scheduling.XynaOrderExecutor;

/**
 * This class keeps track of the number of initiated Deployments as well as the orders that are running in between
 * WorkflowProcessors (Planning-&gt;Execution-&gt;CleanUp) and Scheduler (all those place where they are not accessible)
 */
public class OrderAndDeploymentCounter {
/**
 * This could now mostly be replaced from an aggregated callStatistic
 * for that to work we would need to count orders we find in reachable places though
 * and compare that with an aggregation of called - (finished + errors)
 * Once we collected as much reachable orders as there are known to be on their way we could proceed 
 * We could definitively use that aggregation for an even faster failFast in breakOnInterfaceChanges 
 */
  private static Logger logger = CentralFactoryLogging.getLogger(OrderAndDeploymentCounter.class);

  private final AtomicLong countOfDeploymentsSinceStartup;
  private final AtomicLong ordersKnowingCurrentDeployment;
  private final AtomicLong ordersKnowingOlderDeployment;
  private final ReadWriteLock currentIdLock;
  private final AtomicBoolean someoneToNotify = new AtomicBoolean(false);
  private final Object shelter;


  OrderAndDeploymentCounter() {
    countOfDeploymentsSinceStartup = new AtomicLong(0);
    ordersKnowingCurrentDeployment = new AtomicLong(0);
    ordersKnowingOlderDeployment = new AtomicLong(0);
    currentIdLock = new ReentrantReadWriteLock();
    shelter = new Object();
  }


  public void countUp(long deploymentsKnownToOrder) {
    currentIdLock.readLock().lock();
    try {
      synchronized (ringBuffer) {
        if (ringbuffersize.get() > 1) {
          ringBuffer.add(new DebugEntry(deploymentsKnownToOrder, true, ordersKnowingOlderDeployment.get(), ordersKnowingCurrentDeployment.get()));
        }
      }
      AtomicLong cnt = getCounterFor(deploymentsKnownToOrder);
      long value = cnt.getAndIncrement();
      if (cnt == ordersKnowingOlderDeployment && value == 0) {
        lastTimeCounterWasIncreasedFromZero.set(System.currentTimeMillis());
      }
      traceCounterState("count up");
    } finally {
      currentIdLock.readLock().unlock();
    }
  }
  
  
  // does not get currentIdLock, will need to be done outside
  private AtomicLong getCounterFor(long deploymentsKnownToOrder) {
    if (deploymentsKnownToOrder == getCurrentDeploymentCount()) {
      return ordersKnowingCurrentDeployment;
    } else {
      return ordersKnowingOlderDeployment;
    }
  }
  
  
  private AtomicLong lastTimeCounterWasIncreasedFromZero = new AtomicLong(System.currentTimeMillis());

  private static final int stackelementfordebug = 5;
  
  private enum CallerType {
  
    
    START_ORDER {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(XynaProcessCtrlExecution.class.getCanonicalName());
      }
    },
    MASTER_WF_RUN {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(MasterWorkflowPostScheduler.class.getCanonicalName())
            && stackTrace[stackelementfordebug].getMethodName().equals("runInternallyWithXynaOrder");
      }
    }, 
    PRE_SCHEDULER {
      
      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        //com.gip.xyna.xprc.xsched.XynaScheduler.addOrderIntoAllOrdersEtc(XynaScheduler.java:810)
        return stackTrace[stackelementfordebug].getClassName().equals(XynaScheduler.class.getCanonicalName());
      }
    },
    EXECUTION_PROCESSOR {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(FractalExecutionProcessor.class.getCanonicalName());
      }
    },
    PLANNING_PROCESSOR {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(FractalPlanningProcessor.class.getCanonicalName());
      }
    },
    CLEANUP_PROCESSOR {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(FractalCleanupProcessor.class.getCanonicalName());
      }
    },
    CANCEL {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(XynaOrderExecutor.class.getCanonicalName());
      }
    },
    RESUME {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(ResumeOrderJavaDestination.class.getCanonicalName());
      }
    },
    REMOTE_ORDER_RESP_ERROR {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(RemoteOrderResponseListener.class.getCanonicalName())
            && stackTrace[stackelementfordebug].getMethodName().equals("onError");
      }
    },
    REMOTE_ORDER_RESP_RESP {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(RemoteOrderResponseListener.class.getCanonicalName())
            && stackTrace[stackelementfordebug].getMethodName().equals("onResponse");
      }
    },
    SERVICE_DESTINATION {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(ExecutionDispatcher.class.getCanonicalName());
      }
    },
    ORDER_FILTER {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(OrderFilterAlgorithmsImpl.class.getCanonicalName());
      }
    },
    CLEANUP_ABORT_FAILED {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(XynaProcess.class.getCanonicalName());
      }
    },
    CLEANUP_BEFORE_SCHEDULED {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getMethodName().equals("cleanupBeforeOrderHasBeenScheduled");
      }
    },
    CLEANUP_COMPENSATE {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getMethodName().equals("cleanupCompensateBeforeOrderHasBeenScheduled");
      }
    },
    SUSPEND_ALL_ORDERS {

      @Override
      boolean accept(StackTraceElement[] stackTrace) {
        return stackTrace[stackelementfordebug].getClassName().equals(SuspendAllOrdersJavaDestination.class.getCanonicalName());
      }
    };

    public static CallerType createFromThread(Thread t) {
      StackTraceElement[] st = t.getStackTrace();
      /*
       * 
        [0] = Thread.getStackTrace
        [1] = at com.gip.xyna.xprc.xfractwfe.OrderAndDeploymentCounter$CallerType.createFromThread(OrderAndDeploymentCounter.java:219)
          at com.gip.xyna.xprc.xfractwfe.OrderAndDeploymentCounter$CallerType.createFromThread(OrderAndDeploymentCounter.java:250)
          at com.gip.xyna.xprc.xfractwfe.OrderAndDeploymentCounter$DebugEntry.<init>(OrderAndDeploymentCounter.java:274)
          at com.gip.xyna.xprc.xfractwfe.OrderAndDeploymentCounter.countDown(OrderAndDeploymentCounter.java:290)
          at com.gip.xyna.xprc.xfractwfe.DeploymentManagement.countDownOrderThatKnowsAboutDeployment(DeploymentManagement.java:400)
          at com.gip.xyna.xprc.xpce.execution.MasterWorkflowPostScheduler.runInternallyWithXynaOrder(MasterWorkflowPostScheduler.java:662)
          at com.gip.xyna.xprc.xpce.execution.MasterWorkflowPostScheduler.run(MasterWorkflowPostScheduler.java:582)
       */
      for (CallerType ct : CallerType.values()) {
        if (ct.accept(st)) {
          return ct;
        }
      }
      if (logger.isDebugEnabled()) {
        logger.debug("unknown caller " + st[stackelementfordebug].getClassName(), new Exception());
      }
      return null;
    }


    abstract boolean accept(StackTraceElement[] stackTrace);

  }
  
  private static class DebugEntry {
    private final long time = System.currentTimeMillis();
    private final int deploymentId;
    private final CallerType caller;
    private final boolean countUp;
    private final long cntOld;
    private final long cntNew;
    private final long tid;
    
    private DebugEntry(long deploymentId, boolean countUp, long cntOld, long cntNew) {
      this.deploymentId = (int)deploymentId;
      this.countUp = countUp;
      Thread t = Thread.currentThread();
      tid = t.getId();
      caller = CallerType.createFromThread(t);
      this.cntOld = cntOld;
      this.cntNew = cntNew;
    }
    
  }
    
  private AtomicLong lastZeroTimeOfDebug = new AtomicLong(0);
  private static final XynaPropertyInt ringbuffersize = new XynaPropertyInt("xprc.xfractwfe.deployment.counter.debug.ringbuffer.size", 100000).setHidden(true);
  private final ArrayRingBuffer<DebugEntry> ringBuffer = new ArrayRingBuffer<DebugEntry>(ringbuffersize.get());

  public void countDown(long deploymentsKnownToOrder) {
    currentIdLock.readLock().lock();
    try {
      synchronized (ringBuffer) {
        if (ringbuffersize.get() > 1) {
          ringBuffer.add(new DebugEntry(deploymentsKnownToOrder, false, ordersKnowingOlderDeployment.get(), ordersKnowingCurrentDeployment.get()));
        }
      }
      AtomicLong counter = getCounterFor(deploymentsKnownToOrder);
      long value = counter.decrementAndGet();
      if (value < 0) {
        logger.warn("Counter decremented below zero when trying to count down: deploymentsKnown=" + deploymentsKnownToOrder + "   currentCount=" + getCurrentDeploymentCount());
        counter.set(0);
        value = 0;
      }
      if (value == 0 && ordersKnowingOlderDeployment == counter) {
        if (someoneToNotify.get() && isItSafeToDeploy() && someoneToNotify.compareAndSet(true, false)) {
          synchronized (shelter) {
            shelter.notifyAll();
          }
        }
        lastTimeCounterWasIncreasedFromZero.set(System.currentTimeMillis());
      }
      if (logger.isDebugEnabled() && ordersKnowingOlderDeployment == counter) {
        long diff = System.currentTimeMillis() - lastTimeCounterWasIncreasedFromZero.get();
        String msg =
          "deploymentCounter was 0 last " + diff
              + "ms ago. current count is " + value;
        if (value == 0) {
          logger.trace(msg);
        } else {
          long l = lastZeroTimeOfDebug.get();
          if (diff > 60000 && lastTimeCounterWasIncreasedFromZero.get() != l) {
            if (lastZeroTimeOfDebug.compareAndSet(l, lastTimeCounterWasIncreasedFromZero.get())) {
              String filename = "depcnt_" + XynaFactory.getInstance().getBootCount() + "_" + l + ".txt";
              try {
                FileUtils.writeStreamToFile(getDebugInfo(), new File(filename));
                logger.debug("wrote deployment counter debug file to " + filename);
              } catch (Ex_FileWriteException e) {
                logger.debug("could not write deployment counter debug file to " + filename, e);
              }
            } //else anderer thread war schneller
          }
          logger.debug(msg);
        }
        traceCounterState("count down");
      }
    } finally {
      currentIdLock.readLock().unlock();
    }
  }


  private InputStream getDebugInfo() {
    DebugEntry[] des;
    synchronized (ringBuffer) {
      des = ringBuffer.getOrdered(new DebugEntry[ringBuffer.size()]);
    }
    StringBuilder sb = new StringBuilder();
    for (DebugEntry de : des) {
      sb.append(de.time).append(" ").append(de.tid).append(" ").append(de.deploymentId).append(" ").append(de.caller.name()).append(" ")
          .append(de.countUp ? "+ " : "- ").append(de.cntOld).append("/").append(de.cntNew).append("\n");
    }
    return new ByteArrayInputStream(sb.toString().getBytes());
  }


  public boolean isItSafeToDeploy() {
    currentIdLock.readLock().lock();
    try {
      traceCounterState("isItSafeToDeploy");
      if (ordersKnowingOlderDeployment.get() <= 0) {
        return true;
      } else {
        return false;
      }
    } finally {
      currentIdLock.readLock().unlock();
    }
  }



  public void hideTillItsSafe(long timeout) throws XPRC_TimeoutWhileWaitingForUnaccessibleOrderException {
    boolean locked = true;
    currentIdLock.readLock().lock();
    try {
      if (ordersKnowingOlderDeployment.get() <= 0) {
        return;
      }
      if( timeout <= 0 ) {
        throw new XPRC_TimeoutWhileWaitingForUnaccessibleOrderException();
      }
      try {
        synchronized (shelter) {
          currentIdLock.readLock().unlock();
          locked = false;
          someoneToNotify.set(true);
          shelter.wait(timeout);
          locked = true;
          currentIdLock.readLock().lock();
        }
        if (ordersKnowingOlderDeployment.get() <= 0) {
          return;
        } else {
          throw new XPRC_TimeoutWhileWaitingForUnaccessibleOrderException();
        }
      } catch (InterruptedException e) {
        throw new RuntimeException("Deployment-thread got interrupted while waiting for a deployment-window", e);
      }
    } finally {
      if (locked) {
        currentIdLock.readLock().unlock();
      }
    }
  }


  public long propagateNewDeployment() {
    currentIdLock.writeLock().lock();
    try {
      traceCounterState("pre counter migration");
      long newDeploymentId = countOfDeploymentsSinceStartup.incrementAndGet();
      if (ordersKnowingOlderDeployment.get() == 0 && ordersKnowingCurrentDeployment.get() > 0) {
        lastTimeCounterWasIncreasedFromZero.set(System.currentTimeMillis());
      }
      ordersKnowingOlderDeployment.addAndGet(ordersKnowingCurrentDeployment.getAndSet(0));
      traceCounterState("post counter migration");
      return newDeploymentId;
    } finally {
      currentIdLock.writeLock().unlock();
    }
  }


  public long getCurrentDeploymentCount() {
    return countOfDeploymentsSinceStartup.get();
  }


  public void cleanup(Collection<Long> allDeploymentIds) {
    logger.trace("calling cleanup");
    // no longer necessary to perform any kind of cleanup
  }
  
  
  private void traceCounterState(String action) {
    if (logger.isTraceEnabled()) {
      logger.trace(getCounterState(action));
    }
  }
  
  public String getCounterState(String action) {
    StringBuilder stateBuilder = new StringBuilder();
    stateBuilder.append("\nDeploymentCounter-State for " + action + "\n")
                .append("currentDeploymentCount: " + countOfDeploymentsSinceStartup.get() + "\n")
                .append("unsafe orders: " + ordersKnowingOlderDeployment + "\n")
                .append("safe orders: " + ordersKnowingCurrentDeployment);
    return stateBuilder.toString();
  }
  
}
