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

package com.gip.xyna.xprc;



import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.XynaThreadPoolExecutor.DefaultThreadPoolSizeStrategy;
import com.gip.xyna.xprc.XynaThreadPoolExecutor.EagerThreadPoolSizeStrategy;
import com.gip.xyna.xprc.XynaThreadPoolExecutor.LazyThreadPoolSizeStrategy;
import com.gip.xyna.xprc.xpce.execution.MasterWorkflowPostScheduler;
import com.gip.xyna.xprc.xsched.XynaDaemonFactory;
import com.gip.xyna.xprc.xsched.XynaThreadFactory;



public class XynaExecutor {

  public static class ExecutionThreadPoolExecutorWithDecreasingPrio implements Executor {

    private final int prio;

    public ExecutionThreadPoolExecutorWithDecreasingPrio(int prio) {
      this.prio = prio;
    }

    public void execute(Runnable r) {
      if (r instanceof XynaRunnable) {
        int p = prio;
        while (p >= Thread.MIN_PRIORITY) {
          try {
            XynaExecutor.getInstance().executeRunnableWithExecutionThreadpool((XynaRunnable) r, p);
            break; //success
          } catch (RejectedExecutionException e) {
            p--;
            if (p < Thread.MIN_PRIORITY) {
              throw e;
            }
          }
        }
      } else {
        throw new RuntimeException("unsupported runnable type: " + r.getClass());
      }
    }

  }

  private static final Logger logger = CentralFactoryLogging.getLogger(XynaExecutor.class);

  // executes the task using a thread pool
  private XynaThreadPoolExecutor[] prioritizedExecServices;
  private XynaThreadPoolExecutor unprioritizedPlanningExecService;
  private XynaThreadPoolExecutor unprioritizedCleanupExecService;
  private XynaThreadPoolExecutor unprioritizedUnDeploymentService;

  private volatile static XynaExecutor _instance = null;
  private volatile boolean isShutdown = false;

  private int queueSizePlanning;
  private int queueSizeCleanup;

  
  // This dummy sub-class is used to start threads the same way while the XynaFactory is not yet ready
  // The default class requires properties and other parts of the XynaFactory to be initialized.
  // That is not the case on startup and shutdown. So with this those calls are redirected to use
  private static class XynaExecutorDummy extends XynaExecutor {

    public XynaExecutorDummy() {
      super(false);
    }


    public void executeRunnableWithExecutionThreadpool(XynaRunnable r, int priority) {
      Thread commandLineThread = new Thread(r);
      commandLineThread.setName(XynaExecutor.class.getSimpleName() + " Spawning Thread");
      commandLineThread.setPriority(priority);
      commandLineThread.start();
    }

    
    public void executeRunnableWithUnprioritizedPlanningThreadpool(XynaRunnable r) {
      Thread commandLineThread = new Thread(r);
      commandLineThread.setName(XynaExecutor.class.getSimpleName() + " Spawning Thread");
      commandLineThread.setPriority(Thread.MAX_PRIORITY);
      commandLineThread.start();
    }


    public void executeRunnableWithCleanupThreadpool(XynaRunnable r) {
      Thread commandLineThread = new Thread(r);
      commandLineThread.setName(XynaExecutor.class.getSimpleName() + " Spawning Thread");
      commandLineThread.setPriority(Thread.MAX_PRIORITY);
      commandLineThread.start();
    }
    
    public void executeRunnableWithUnDeploymentThreadpool(XynaRunnable r) {
      Thread commandLineThread = new Thread(r);
      commandLineThread.setName(XynaExecutor.class.getSimpleName() + " Spawning Thread");
      commandLineThread.setPriority(Thread.MAX_PRIORITY);
      commandLineThread.start();
    }

  };


  /*
   * Gets a new instance more defensively if Threads are requested on startup and the XynaFactory is not yet ready.
   * If there is no instane and it is not desired to create on then the dummy instance is returned.
   * Otherwise we get the instance created by the default getInstance method.
   */
  public static XynaExecutor getInstance(boolean initializeIfNecessary) {
    if (_instance == null && !initializeIfNecessary) {
      return new XynaExecutorDummy();
    }

    return getInstance();
  }


  /*
   * If there is no such Instance, create one statically and pass it back.
   * Otherwise if we are already shutting down, then we use the XynaExecutorDummy to be able to dispatch threads even
   * some parts of the XynaFactory are already down.
   */
  public static XynaExecutor getInstance() {
    if (_instance == null) {
      synchronized (XynaExecutor.class) {
        if (_instance == null) {
          _instance = new XynaExecutor();
        }
      }
      return _instance;
    } else {
      if (_instance.isShutdown()) {
        return new XynaExecutorDummy();
      } else {
        return _instance;
      }
    }
  }


  public static void setInstance(XynaExecutor executor) {
    _instance = executor;
  }


  private XynaExecutor(boolean dummy) {
  }


  private XynaExecutor() {
    int minThreadsPlanning = XynaProperty.THREADPOOL_PLANNING_MINTHREADS.get();
    int maxThreadsPlanning = XynaProperty.THREADPOOL_PLANNING_MAXTHREADS.get();
    int keepAlivePlanning = (int) XynaProperty.THREADPOOL_PLANNING_KEEP_ALIVE.get().getDuration(TimeUnit.SECONDS);
    queueSizePlanning = XynaProperty.THREADPOOL_PLANNING_QUEUE_SIZE.get();
    int minThreadsCleanup = XynaProperty.THREADPOOL_CLEANUP_MINTHREADS.get();
    int maxThreadsCleanup = XynaProperty.THREADPOOL_CLEANUP_MAXTHREADS.get();
    int keepAliveCleanup = (int) XynaProperty.THREADPOOL_CLEANUP_KEEP_ALIVE.get().getDuration(TimeUnit.SECONDS);
    queueSizeCleanup = XynaProperty.THREADPOOL_CLEANUP_QUEUE_SIZE.get();
    int minThreadsExecution = XynaProperty.THREADPOOL_EXECUTION_MINTHREADS.get();
    int maxThreadsExecution = XynaProperty.THREADPOOL_EXECUTION_MAXTHREADS.get();
    int keepAliveExecution = (int) XynaProperty.THREADPOOL_EXECUTION_KEEP_ALIVE.get().getDuration(TimeUnit.SECONDS);
    boolean useRingBufferImplementation = XynaProperty.THREADPOOL_PLANNING_USE_RINGBUFFER.get();
    int maxThreadsDeployment = XynaProperty.THREADPOOL_DEPLOYMENT_MAXTHREADS.get();
    int keepAliveDeployment = XynaProperty.THREADPOOL_DEPLOYMENT_KEEP_ALIVE.get();
    
    
    XynaProperty.THREADPOOL_PLANNING_MINTHREADS.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_PLANNING_MINTHREADS.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_PLANNING_MAXTHREADS.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_PLANNING_KEEP_ALIVE.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_PLANNING_QUEUE_SIZE.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_PLANNING_POOL_SIZE_STRATEGY.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_CLEANUP_MINTHREADS.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_CLEANUP_MAXTHREADS.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_CLEANUP_KEEP_ALIVE.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_CLEANUP_QUEUE_SIZE.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_EXECUTION_MINTHREADS.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_EXECUTION_MAXTHREADS.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_EXECUTION_KEEP_ALIVE.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_PLANNING_USE_RINGBUFFER.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_DEPLOYMENT_MAXTHREADS.registerDependency("XynaExecutor");
    XynaProperty.THREADPOOL_DEPLOYMENT_KEEP_ALIVE.registerDependency("XynaExecutor");
    
    prioritizedExecServices = new XynaThreadPoolExecutor[11];
    for (int i = 1; i < 11; i++) {
      // keine (bzw leere) queue, weil ansonsten subaufträge nie gestartet werden könnten, deren parentaufträge die
      // threads blockieren
      // grund dafür ist, dass die threadpoolexecutoren die logik besitzen, dass neue threads oberhalb von der
      // corepoolsize
      // erst erstellt werden, wenn die queue voll ist.
      prioritizedExecServices[i] = new XynaThreadPoolExecutor(minThreadsExecution, maxThreadsExecution,
                                                              keepAliveExecution, TimeUnit.SECONDS,
                                                              new SynchronousQueue<Runnable>(),
                                                              new XynaThreadFactory(i), "ExecutionPrio" + i);
    }

    // this pool is used before scheduling, so the number of threads has to be limited
    // every request beyond that limit is queued
    unprioritizedPlanningExecService =
          new XynaThreadPoolExecutor(minThreadsPlanning, maxThreadsPlanning, keepAlivePlanning, TimeUnit.SECONDS,
                                     queueSizePlanning, new XynaDaemonFactory(), "Planning", useRingBufferImplementation);
    unprioritizedPlanningExecService.setThreadPoolUsageStrategy( new ThreadPoolUsageStrategyDependingOnCaller(unprioritizedPlanningExecService) );
    
    String planningThreadPoolSizeStrategy = XynaProperty.THREADPOOL_PLANNING_POOL_SIZE_STRATEGY.readOnlyOnce();
    try {
      if( "eager".equalsIgnoreCase(planningThreadPoolSizeStrategy) ) {
        unprioritizedPlanningExecService.setThreadPoolSizeStrategy( new EagerThreadPoolSizeStrategy(unprioritizedPlanningExecService) ); 
      } else if( "default".equalsIgnoreCase(planningThreadPoolSizeStrategy) ) {
        unprioritizedPlanningExecService.setThreadPoolSizeStrategy( new DefaultThreadPoolSizeStrategy() ); 
      } else {
        String[] parts = planningThreadPoolSizeStrategy.split("_");
        if( "lazy".equalsIgnoreCase(parts[0]) ) {
          int lazyness = Integer.parseInt(parts[1]);
          unprioritizedPlanningExecService.setThreadPoolSizeStrategy( new LazyThreadPoolSizeStrategy(unprioritizedPlanningExecService,lazyness) ); 
        } else {
          throw new Exception("unexpected part "+ parts[0]);
        }
      }
    } catch( Exception e ) {
      logger.warn("Failed to use ThreadPoolSizeStrategy \""+planningThreadPoolSizeStrategy+"\"", e);
      unprioritizedPlanningExecService.setThreadPoolSizeStrategy( new EagerThreadPoolSizeStrategy(unprioritizedPlanningExecService) ); 
    }
    logger.info( "unprioritizedPlanningExecService = "+unprioritizedPlanningExecService+ ", useRingBufferImplementation="+useRingBufferImplementation+", threadPoolSizeStrategy="+ unprioritizedPlanningExecService.getThreadPoolSizeStrategy());

    // this pool is used for orders which are marked as erroneous by the scheduler (e.g. timeout)
    BlockingQueue<Runnable> queue = null;
    if (queueSizeCleanup <= 0) {
      queue = new SynchronousQueue<Runnable>();
    }
    else {
      queue = new LinkedBlockingQueue<Runnable>(queueSizeCleanup);
    }
    unprioritizedCleanupExecService = new XynaThreadPoolExecutor(minThreadsCleanup, maxThreadsCleanup,
                                                                 keepAliveCleanup, TimeUnit.SECONDS, queue,
                                                                 new XynaDaemonFactory(), "Cleanup");
    
    unprioritizedUnDeploymentService = new XynaThreadPoolExecutor(0, maxThreadsDeployment, keepAliveDeployment,
                                                                 TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                                                                 new XynaDaemonFactory(), "UnDeploymentHandler");

  }


  public ThreadPoolStatistics[] getThreadPoolStatistics() {
    ThreadPoolStatistics[] ret = new ThreadPoolStatistics[13];
    for (int i = 0; i < 10; i++) {
      ret[i] = new ThreadPoolStatistics("Execution Prio " + (i + 1), prioritizedExecServices[i + 1], 0);
    }
    ret[10] = new ThreadPoolStatistics("Planning", unprioritizedPlanningExecService, queueSizePlanning);
    ret[11] = new ThreadPoolStatistics("Cleanup", unprioritizedCleanupExecService, queueSizeCleanup);
    ret[12] = new ThreadPoolStatistics("(Un-)Deployment-Handler", unprioritizedUnDeploymentService, 0);
    return ret;
  }


  public void executeRunnableWithExecutionThreadpool(XynaRunnable r, int priority) {
    if (isShutdown) {
      throw new IllegalStateException(getClass().getSimpleName() + " is shutdown, cannot execute runnable!");
    }
    priority = checkPriority(r, priority);
    prioritizedExecServices[priority].execute(r);
  }


  private int checkPriority(XynaRunnable r, int priority) {
    //es gibt nicht prio 0
    if (priority < 1 || priority >= prioritizedExecServices.length) {
      if (r instanceof MasterWorkflowPostScheduler) {
        MasterWorkflowPostScheduler mwps = (MasterWorkflowPostScheduler) r;
        logger.warn("invalid priority: " + priority + " (" + mwps.getOrder() + ")");
      } else {
        logger.warn("invalid priority: " + priority);
      }
      priority = 1;
    }
    return priority;
  }


  public void executeRunnableWithUnprioritizedPlanningThreadpool(XynaRunnable r) {
    if (isShutdown) {
      throw new IllegalStateException(getClass().getSimpleName() + " is shutdown, cannot execute runnable!");
    }
    if( logger.isTraceEnabled() ) {
      logger.trace("execute("+r+") with queue "+unprioritizedPlanningExecService.getQueue() );
    }
    unprioritizedPlanningExecService.execute(r);
  }


  public ThreadPoolExecutor getExecutionThreadPool(int priority) {
    priority = checkPriority(null, priority);
    return prioritizedExecServices[priority];
  }


  public boolean hasOpenUnprioritizedThreads() {
    return unprioritizedPlanningExecService.getQueue().size() > 0 || unprioritizedPlanningExecService.getActiveCount() > 0;
  }


  public boolean hasOpenPrioritizedThreads() {
    for (ThreadPoolExecutor execServ : prioritizedExecServices) {
      if (execServ == null) {
        continue;
      }
      if (execServ.getQueue().size() > 0) {
        return true;
      }
      if (execServ.getActiveCount() > 0) {
        return true;
      }
    }
    return false;
  }

  public boolean hasOpenThreads(boolean prioritized) {
    if( prioritized ) {
      return hasOpenPrioritizedThreads();
    } else {
      return hasOpenUnprioritizedThreads();
    }
  }


  /**
   * Sends shutdown signals to all active {@link ExecutorService} instances.
   * 
   * @throws IllegalStateException if already shutdown
   */
  public synchronized void shutdown() {

    if (isShutdown) {
      throw new IllegalStateException(getClass().getSimpleName() + " is already shutdown, cannot shutdown!");
    }
    isShutdown = true;

    boolean sentShutdownSignal = false;
    if (prioritizedExecServices != null) {
      logger.debug("Sending shutdown signal to prioritized thread pools...");
      for (ExecutorService execService : prioritizedExecServices) {
        if (execService != null) {
          execService.shutdown();
          sentShutdownSignal = true;
        }
      }
    }
    if (unprioritizedPlanningExecService != null) {
      logger.debug("Sending shutdown signal to unprioritized thread pool...");
      unprioritizedPlanningExecService.shutdown();
      sentShutdownSignal = true;
    }
    if (unprioritizedCleanupExecService != null) {
      logger.debug("Sending shutdown signal to thread pool for timeout handling...");
      unprioritizedCleanupExecService.shutdown();
      sentShutdownSignal = true;
    }
    if (unprioritizedUnDeploymentService != null) {
      logger.debug("Sending shutdown signal to thread pool for UnDeployment handler...");
      unprioritizedUnDeploymentService.shutdown();
      sentShutdownSignal = true;
    }
    

    if (logger.isDebugEnabled()) {
      if (sentShutdownSignal == true) {
        logger.debug("Sent shutdown signal to thread pools.");
      } else {
        logger.debug("No thread pool to shutdown.");
      }
    }

  }


  public boolean isShutdown() {
    return isShutdown;
  }


  public boolean isTerminated() {

    if (prioritizedExecServices != null) {
      for (ExecutorService execService : prioritizedExecServices) {
        if (execService != null) {
          if (!execService.isTerminated()) {
            return false;
          }
        }
      }
    }

    if (unprioritizedPlanningExecService != null) {
      if (!unprioritizedPlanningExecService.isTerminated()) {
        return false;
      }
    }

    if (unprioritizedCleanupExecService != null) {
      if (!unprioritizedCleanupExecService.isTerminated()) {
        return false;
      }
    }
    return true;
  }

 
  public void executeRunnableWithCleanupThreadpool(XynaRunnable r) {
    if (isShutdown) {
      throw new IllegalStateException(getClass().getSimpleName() + " is shutdown, cannot execute runnable!");
    }
    unprioritizedCleanupExecService.execute(r);
  }
  
  
  public void executeRunnableWithUnDeploymentThreadpool(XynaRunnable r) {
    if (isShutdown) {
      throw new IllegalStateException(getClass().getSimpleName() + " is shutdown, cannot execute runnable!");
    }
    unprioritizedUnDeploymentService.execute(r);
  }
 
}

