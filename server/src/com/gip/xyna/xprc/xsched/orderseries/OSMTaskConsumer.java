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
package com.gip.xyna.xprc.xsched.orderseries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.CounterMap;
import com.gip.xyna.utils.collections.LruCache;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation.Mode;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation.TaskConsumerState;
import com.gip.xyna.xprc.xsched.orderseries.tasks.OSMTask;


/**
 *
 */
public class OSMTaskConsumer implements Runnable {

  private static Logger logger = CentralFactoryLogging.getLogger(OSMTaskConsumer.class);
  public static final int MAX_FAILED_TASKS = 10; //TODO konfigurierbar
  private static final int MAX_INTERN = 3; //Maximal 3 interne Aufträge, dann wieder ein externer 
  
  private volatile boolean running = false;
  private volatile boolean paused = false;
  private volatile Thread thread;
  private BlockingQueue<OSMTask> externalQueue;
  private Queue<OSMTask> internalQueue;
  private OSMCache osmCache;
  private OSMInterface osm;
  private OSMLocalImpl localOsm;
  private OSMRemoteProxyImpl remoteOsm;
  private PredecessorTrees predecessorTrees;
  private volatile CountDownLatch pauseLatch;
  private volatile CountDownLatch unpauseLatch;
  private ReentrantLock pauseLock;
  private CounterMap<Class<? extends OSMTask>> taskCounter = new CounterMap<Class<? extends OSMTask>>();
  private LruCache<String, OSMTask> failedTasks = new LruCache<String, OSMTask>(MAX_FAILED_TASKS);
  private int internCounter;
  
  public OSMTaskConsumer( BlockingQueue<OSMTask> externalQueue, Queue<OSMTask> internalQueue,
                          OSMCache osmCache, OSMInterface osm, 
                          OSMLocalImpl localOsm, OSMRemoteProxyImpl remoteOsm, 
                          PredecessorTrees predecessorTrees) {
    this.externalQueue = externalQueue;
    this.internalQueue = internalQueue;
    this.osmCache = osmCache;
    this.osm = osm;
    this.localOsm = localOsm;
    this.remoteOsm = remoteOsm;
    this.predecessorTrees = predecessorTrees;
    this.pauseLock = new ReentrantLock();
  }
  
  public void run() {
    thread = Thread.currentThread();
    running = true;
    while(running) {
      if( paused ) {
        pauseLatch.countDown();
        try {
          unpauseLatch.await();
        } catch (InterruptedException e) { 
          //ignorieren: evtl. wurde ja running auf false gesetzt
          continue;
        }
      }
      OSMTask task = null;
      try {
        task = retrieveTask();
      } catch (InterruptedException e) { 
        //ignorieren: evtl. wurde ja running auf false gesetzt 
      }
      execute( task );
    }
    thread = null;
  }

  /**
   * @return
   * @throws InterruptedException 
   */
  public OSMTask retrieveTask() throws InterruptedException {
    OSMTask task = null;
    if( internCounter == MAX_INTERN ) {
      //MAX_INTERN interne wurden bearbeitet, daher erst mal einen externen ausführen
      internCounter = 0;
      task = externalQueue.poll(); //nicht warten, falls keine externen vorliegen 
    }
    if( task == null ) {
      //zuerst interne Queue leeren
      task = internalQueue.poll();
      
      if( task != null ) {
        ++internCounter;
      } else {
        //falls keine internen Tasks vorliegen: auf externe Queue warten 
        task = externalQueue.take();
      }
    }
    return task;
  }

  /**
   * @param task
   */
  public void execute(OSMTask task) {
    if( task == null ) {
      return; //nichts zu tun
    }
    try {
      if( logger.isDebugEnabled() ) {
        logger.debug("execute " + task );
      }
      
      task.execute(osmCache, osm, localOsm, remoteOsm, predecessorTrees);

      taskCounter.increment(task.getClass());
    } catch (Exception e) {
      //FIXME besser behandeln: Jeder Fehler bedeutet, dass Aufträge nicht ausgeführt werden!
      logger.warn("Task "+task+" failed", e);
      failedTasks.put(task.getCorrelationId(), task);
    }
  }

  /**
   * @return
   */
  public boolean isRunning() {
    return running;
  }
 
  /**
   * Anhalten des TaskConsumers, bis unpause() gerufen wird. Diese Methode blockiert, bis der 
   * TaskConsumer sicher pausiert ist. Fall beim Warten der Thread interrupted wird, wird ein
   * unpause() ausgeführt und die InterruptedException weitergeworfen. 
   * @throws InterruptedException falls TaskConsumers wegen InterruptedException nicht pausiert wurde
   */
  public void pause() throws InterruptedException {
    pauseLock.lock();
    paused = true;
    pauseLatch = new CountDownLatch(1);
    unpauseLatch = new CountDownLatch(1);
    Thread t = thread;
    if( t != null ) {
      t.interrupt(); 
    }
    try {
      pauseLatch.await();
    } catch (InterruptedException e) {
      unpause();
      throw e;
    } 
  }

  /**
   * Beenden der TaskConsumer-Pause
   */
  public void unpause() {
    if( pauseLock.isHeldByCurrentThread() ) {
      paused = false;
      unpauseLatch.countDown();
      pauseLock.unlock();
    }
  }

  /**
   * @param osmi
   * @param mode 
   */
  public void fillOrderSeriesManagementInformation(OrderSeriesManagementInformation osmi, Mode mode) {
    TaskConsumerState tcs = null;
    if( running ) {
      tcs = paused ? TaskConsumerState.Paused : TaskConsumerState.Running;
    } else {
      tcs = TaskConsumerState.Finished;
    }
    osmi.setTaskConsumerState( tcs );
    
    if( mode != Mode.Basic ) {
      osmi.setTasksCount( taskCounterList() );
      ArrayList<String> failed = new ArrayList<String>();
      for( OSMTask ft : failedTasks.values() ) {
        failed.add(ft.toString());
      }
      osmi.setFailedTasks( failed );
    }
  }

  /**
   * @return
   */
  private List<Pair<String,Integer>> taskCounterList() {
    List<Pair<String,Integer>> list = new ArrayList<Pair<String,Integer>>();
    for( Map.Entry<Class<? extends OSMTask>,AtomicInteger> entry: taskCounter.entrySet() ) {
      list.add( Pair.of(entry.getKey().getSimpleName(), entry.getValue().get() ));
    }
    return list;
  } 

}
