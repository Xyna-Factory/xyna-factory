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
package com.gip.xyna.demon.worker;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.persistency.PersistableCounter;
import com.gip.xyna.demon.worker.SlaveFactory.SlaveThread;


/**
 * Pool 
 *
 * @param <Tool> Werkzeug, das jeder Slave zum Arbeiten benötigt (z.B. DB-Connection)
 * @param <Work> der konkrete Arbeitsauftrag
 */
public class SlavePool<Tool,Work> {
  static Logger logger = Logger.getLogger(SlavePool.class.getName());

  protected ThreadPoolExecutor threadPoolExecutor;
  protected PersistableCounter pcRequested;
  protected PersistableCounter pcRejected;
  private PersistableCounter pcSucceeded;
  private PersistableCounter pcFailed;  
  private volatile boolean active;
  private SlaveInitializer<Tool> slaveInitializer;
  private int corePoolSize;
  private int maxPoolSize;
 
  public SlavePool( SlaveInitializer<Tool> slaveInitializer, int corePoolSize, int maxPoolSize ) {
    this.slaveInitializer = slaveInitializer;
    this.corePoolSize = corePoolSize;
    this.maxPoolSize = maxPoolSize;
    
    pcRequested = new PersistableCounter("slavePool.requested");
    pcRejected = new PersistableCounter("slavePool.rejected");
    pcSucceeded = new PersistableCounter("slavePool.succeeded");
    pcFailed = new PersistableCounter("slavePool.failed");
    
    if( this.maxPoolSize < this.corePoolSize ) {
      logger.error("MaxPoolSize < CorePoolSize, setting MaxPoolSize = CorePoolSize" );
      this.maxPoolSize = this.corePoolSize;
    }
    startThreadPoolExecutor();
  }
  
  public void startThreadPoolExecutor() {
    threadPoolExecutor = new ThreadPoolExecutor( 
        corePoolSize, maxPoolSize,           //corePoolSize Threads sind dauerhaft vorhanden, anwachsend bis maxPoolSize
        1000L, TimeUnit.MILLISECONDS,        //überzählige Threads werden nach 1 s wieder beendet
        new SynchronousQueue<Runnable>(),    //direkte Übergabe an Thread, kein Queueing
        new ThreadPoolExecutor.AbortPolicy() //sofortiges Werfen der RuntimeException RejectedExecutionException, falls kein Thread frei ist
        );
    threadPoolExecutor.setThreadFactory( new SlaveFactory<Tool>(slaveInitializer) );
    active = true;
  }

  /**
   * @param sw
   */
  public void execute( SlaveWork<Tool,Work> sw ) {
    pcRequested.increment();
    try {
      threadPoolExecutor.execute( new RunnableSlaveWork<Tool,Work>( this, sw ) );
    } catch( RejectedExecutionException e ) {
      pcRejected.increment();
    }
  }
 
  public static enum CounterData {
    REQUESTED,
    REJECTED,
    SUCCEEDED,
    FAILED,
  }
    
  /**
   * @param counterData
   * @return
   */
  public PersistableCounter getPersistableCounter(CounterData counterData) {
    switch( counterData ) {
    case REQUESTED: return pcRequested;
    case REJECTED: return pcRejected;
    case SUCCEEDED: return pcSucceeded;
    case FAILED: return pcFailed;
    default:
      throw new IllegalArgumentException();
    }
  }

  
  public static class RunnableSlaveWork<Tool,Work> implements Runnable {

    private SlaveWork<Tool,Work> slaveWork;
    private SlavePool<Tool,Work> slavePool;
    
    public RunnableSlaveWork(SlavePool<Tool,Work> slavePool, SlaveWork<Tool,Work> sw) {
      this.slavePool = slavePool;
      this.slaveWork = sw;
    }

    @SuppressWarnings("unchecked")
    public void run() {
      Thread ct = Thread.currentThread();
      if( ct instanceof SlaveThread ) {
        SlaveThread<Tool> st = (SlaveThread<Tool>) ct;
        boolean success;
        try {
          success = slaveWork.work( st.getTool() );
        } catch( RuntimeException e ) {
          //Schützt dem Thread vor dem Abbruch durch ungefangene RuntimeExceptions
          logger.error("Catched RuntimeException from SlaveWork",e);
          success = false;
        }
        slavePool.countSuccess(success);
      } else {
        logger.error( "CurrentThread is no SlaveThread, instead "+ct.getClass().getName() );
        slavePool.countSuccess(false);
      }
    }
    
  }

  public void terminate() {
    active = false;
    threadPoolExecutor.shutdown();
  }

  /**
   * package private!
   * @param success
   */
  synchronized void countSuccess(boolean success) {
    if( success ) {
      pcSucceeded.increment();
    } else {
      pcFailed.increment();
    }
  }
  
  public void logStatus(Logger statusLogger) {
    if( active ) {
      statusLogger.info( "SlavePool is working with "+threadPoolExecutor.getActiveCount()+" active threads ("+threadPoolExecutor.getPoolSize()+" threads)" );
    } else {
      statusLogger.info( "SlavePool is terminated");
    }
  }
  
  public boolean isActive() {
    return active;
  }
  
}
