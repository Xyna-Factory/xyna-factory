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
package com.gip.xyna.utils.parallel;

import java.util.Random;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xsched.XynaThreadFactory;


/**
 *
 */
public class ParallelExecutorTest extends TestCase {
  
  public void testExecuteTwoTasks()  {

    ThreadPoolExecutor threadpool = getThreadPool(5);

    final AtomicBoolean executedFirst = new AtomicBoolean(false);
    final AtomicBoolean executedSecond = new AtomicBoolean(false);
    ParallelExecutor executor = new ParallelExecutor(threadpool);

    executor.addTask( new WaitAndSetBooleanTrue(100, executedFirst) );
    executor.addTask( new WaitAndSetBooleanTrue(100, executedSecond) );

    executeAndAwait(executor);

    assertTrue(executedFirst.get());
    assertTrue(executedSecond.get());

  }
  

  public void testExecuteTwoTasksWithPrio()  {

    ThreadPoolExecutor threadpool = getThreadPool(5);

    final AtomicBoolean executedFirst = new AtomicBoolean(false);
    final AtomicBoolean executedSecond = new AtomicBoolean(false);
    ParallelExecutor executor = new ParallelExecutor(threadpool);
   
    executor.addTask( new WaitAndSetBooleanTrue(100, executedFirst, 1) );
    executor.addTask( new WaitAndSetBooleanTrue(100, executedSecond, 100) );

    assertEquals( 2, executor.size() );
    
    executor.setPriorityThreshold( 10 ); //nur Task 2 ist höher
    executeAndAwait(executor);
    assertEquals( 1, executor.size() );
    assertFalse(executedFirst.get()); //nicht ausgeführt!
    assertTrue(executedSecond.get());
    
    executor.setPriorityThreshold( 0 );
    executeAndAwait(executor);
    assertEquals( 0, executor.size() );
    assertTrue(executedFirst.get());
    assertTrue(executedSecond.get());

  }

  public void testExecuteMultipleTasksWithFewThreads() throws XynaException {

    final ThreadPoolExecutor threadpool = getThreadPool(5);

    final ParallelExecutor executor = new ParallelExecutor(threadpool);

    int length = 100;
    final AtomicBoolean executedFlags[] = new AtomicBoolean[length];
    final ParallelTask tasks[] = new ParallelTask[length];
    for (int i=0; i<length; i++) {
      executedFlags[i] = new AtomicBoolean(false);
      tasks[i] = new WaitAndSetBooleanTrue(new Random().nextInt(100), executedFlags[i] ); 
      executor.addTask(tasks[i]);
    }
    
    assertEquals( 100, executor.size() );
    UsageMeasurement usage = new UsageMeasurement(threadpool,executor);
    usage.start(50);
  
    executeAndAwait(executor);
    
    assertEquals( 6, usage.getCurrentlyExecutingTasks() ); //ein Thread mehr als im ThreadPool ... 
    assertEquals( 5, usage.getActiveCount() );             //...vorhanden führt Tasks aus 
    
    assertEquals( 0, executor.size() );
    for (int i = 0; i < length; i++) {
      assertTrue(executedFlags[i].get());
    }

  }
  
  private static class UsageMeasurement implements Runnable {

    private ThreadPoolExecutor threadPool;
    private ParallelExecutor executor;
    private int currentlyExecutingTasks;
    private int activeCount;
    private int wait;

    public UsageMeasurement(ThreadPoolExecutor threadpool, ParallelExecutor executor) {
      this.threadPool = threadpool;
      this.executor = executor;
     }

    public void start(int wait) {
      this.wait = wait;
      new Thread(this).start();
    }

    public void run() {
      try {
        Thread.sleep(wait);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      currentlyExecutingTasks = executor.currentlyExecutingTasks();
      activeCount = threadPool.getActiveCount();
    }
    
    public int getCurrentlyExecutingTasks() {
      return currentlyExecutingTasks;
    }
    
    public int getActiveCount() {
      return activeCount;
    }
  }
  

  public void testExecuteMultipleTasksWithUsedThreads() throws XynaException {

    ThreadPoolExecutor threadpool = getThreadPool(5);

    ParallelExecutor executor = new ParallelExecutor(threadpool);

    int length = 100;
    final AtomicBoolean executedFlags[] = new AtomicBoolean[length];
    final ParallelTask tasks[] = new ParallelTask[length];
    for (int i=0; i<length; i++) {
      executedFlags[i] = new AtomicBoolean(false);
      tasks[i] = new WaitAndSetBooleanTrue(10, executedFlags[i] ); 
      executor.addTask(tasks[i]);
    }
    
    for( int i=0; i< 5; ++i ) {
      threadpool.execute( new ThreadPoolBlocker((i+1)*200) );  //100-> 410; 200 -> 540; 300 -> 640; 400 -> 710; 500 -> 760
    } 
    
    assertEquals( 100, executor.size() );
    long start = System.currentTimeMillis();
    executeAndAwait(executor);
    long end = System.currentTimeMillis();
    assertEquals( 0, executor.size() );
    for (int i = 0; i < length; i++) {
      assertTrue(executedFlags[i].get());
    }
    //System.out.println( "duration "+ (end-start) );
    assertDuration( 540, end-start, 30 );

  }

  public void testExecuteMultipleTasksWithLimitation() throws XynaException {

    ThreadPoolExecutor threadpool = getThreadPool(5);

    ParallelExecutor executor = new ParallelExecutor(threadpool, 3); //1 -> 1000; 2 -> 500; 3 -> 340; 4 -> 250; 
                                                                     //5 -> 200; 6 -> 170; 7 -> 170, 8 -> 170...
    int length = 100;
    final AtomicBoolean executedFlags[] = new AtomicBoolean[length];
    final ParallelTask tasks[] = new ParallelTask[length];
    for (int i=0; i<length; i++) {
      executedFlags[i] = new AtomicBoolean(false);
      tasks[i] = new WaitAndSetBooleanTrue(10, executedFlags[i] ); 
      executor.addTask(tasks[i]);
    }
    
    
    assertEquals( 100, executor.size() );
    long start = System.currentTimeMillis();
    executeAndAwait(executor);
    long end = System.currentTimeMillis();
    assertEquals( 0, executor.size() );
    for (int i = 0; i < length; i++) {
      assertTrue(executedFlags[i].get());
    }
    //System.out.println( "duration "+ (end-start) );
    assertDuration( 340, end-start, 30 );

  }
  
  public void testExecuteMultipleTasksWithChangingLimitation() throws XynaException {

    ThreadPoolExecutor threadpool = getThreadPool(5);
 
    ParallelExecutor executor = new ParallelExecutor(threadpool, 3); 
    int length = 100;
    final AtomicBoolean executedFlags[] = new AtomicBoolean[length];
    final ParallelTask tasks[] = new ParallelTask[length];
    for (int i=0; i<length; i++) {
      executedFlags[i] = new AtomicBoolean(false);
      tasks[i] = new WaitAndSetBooleanTrue(10, executedFlags[i] ); 
      executor.addTask(tasks[i]);
    }
    
    assertEquals( 100, executor.size() );
    executor.execute();
    sleep(20);
    assertEquals( 3, executor.currentlyExecutingTasks() );
    assertEquals( 3, threadpool.getActiveCount() );
    
    executor.setThreadLimit(5);
    sleep(20);
    assertEquals( 5, executor.currentlyExecutingTasks() );
    assertEquals( 5, threadpool.getActiveCount() );
    
    executor.setThreadLimit(0);
    sleep(30);
    assertEquals( 0, executor.currentlyExecutingTasks() );
    assertEquals( 0, threadpool.getActiveCount() );
    
    executor.setThreadLimit(5);
    executor.execute();
    sleep(20);
    assertEquals( 5, executor.currentlyExecutingTasks() );
    assertEquals( 5, threadpool.getActiveCount() );
    
    try {
      executor.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    assertEquals( 0, executor.currentlyExecutingTasks() );
    
    assertEquals( 0, executor.size() );
    for (int i = 0; i < length; i++) {
      assertTrue(executedFlags[i].get());
    }

  }

  public void testExecuteMultipleTasksWithChangingLimitationAndAwait() throws XynaException {

    ThreadPoolExecutor threadpool = getThreadPool(5);
 
    ParallelExecutor executor = new ParallelExecutor(threadpool, 3); 
    int length = 100;
    final AtomicBoolean executedFlags[] = new AtomicBoolean[length];
    final ParallelTask tasks[] = new ParallelTask[length];
    for (int i=0; i<length; i++) {
      executedFlags[i] = new AtomicBoolean(false);
      tasks[i] = new WaitAndSetBooleanTrue(10, executedFlags[i] ); 
      executor.addTask(tasks[i]);
    }
    
    assertEquals( 100, executor.size() );
    ParallelExecutorStarter pes = new ParallelExecutorStarter(executor);
    threadpool.execute( pes );
    try {
      sleep(20);
      assertEquals( 3, executor.currentlyExecutingTasks() );
      assertEquals( 3, threadpool.getActiveCount() );

      executor.setThreadLimit(5);
      sleep(20);
      assertEquals( 5, executor.currentlyExecutingTasks() );
      assertEquals( 5, threadpool.getActiveCount() );

      executor.setThreadLimit(0);
      sleep(30);
      assertEquals( 0, executor.currentlyExecutingTasks() );
      assertEquals( 1, threadpool.getActiveCount() ); //wegen wartendem Main-Thread

      executor.setThreadLimit(5);
      sleep(20);
      assertEquals( 5, executor.currentlyExecutingTasks() );
      assertEquals( 5, threadpool.getActiveCount() );

      try {
        executor.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      assertEquals( 0, executor.currentlyExecutingTasks() );

      assertEquals( 0, executor.size() );
      for (int i = 0; i < length; i++) {
        assertTrue(executedFlags[i].get());
      }
    } finally {
      pes.running = false;
    }

  }

  private static class ParallelExecutorStarter implements Runnable {

    private ParallelExecutor executor;

    public ParallelExecutorStarter(ParallelExecutor executor) {
      this.executor = executor;
    }
    public volatile boolean running = true; 
    

    public void run() {
      while( running && executor.hasExecutableTasks() ) {
        try {
          executor.executeAndAwait();
        } catch (InterruptedException e) {
          //weiter warten
        }
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          //weiterwarten
        }
      }
    }
    
  }

  
  
  
  
  
  

  private void sleep(long duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


  private static class ThreadPoolBlocker implements Runnable {
    private long duration;
    public ThreadPoolBlocker(long duration) {
      this.duration = duration;
    }

    public void run() {
      try {
        Thread.sleep(duration);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    
  }
  
  
  
  
  
  
  
  

  private ThreadPoolExecutor getThreadPool(int numberOfThreads) {
    return new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 100, TimeUnit.SECONDS,
                           new SynchronousQueue<Runnable>(), new XynaThreadFactory(1));
  }

  private static class WaitParallelExecutable implements ParallelTask {
    private long wait;
    private int taskId;
    private int prio;
    
    public WaitParallelExecutable(long wait) {
      this.wait = wait;
    }
    public WaitParallelExecutable(long wait, int prio) {
      this.wait = wait;
      this.prio = prio;
    }

    public void execute() {
      try {
        Thread.sleep(wait);
      } catch (InterruptedException e) {
        // wait a little to test whether the execute method blocks until its done
      }
    }

    public int getTaskId() {
      return taskId;
    }

    public int getPriority() {
      return prio;
    }
    
  }
  
  private static class WaitAndSetBooleanTrue extends WaitParallelExecutable {
    private AtomicBoolean bool;

    public WaitAndSetBooleanTrue(long wait, AtomicBoolean bool) {
      super(wait);
      this.bool = bool;
    }
    
    public WaitAndSetBooleanTrue(long wait, AtomicBoolean bool, int prio) {
      super(wait,prio);
      this.bool = bool;
    }

    public void execute() {
      super.execute();
      bool.set(true);
    }
    
  }
  /**
   * @param executor
   */
  private void executeAndAwait(ParallelExecutor executor) {
    try {
      executor.executeAndAwait();
    } catch (InterruptedException e) {
      e.printStackTrace(); 
      fail("Unexpected InterruptedException");
    }
  }

  private void assertDuration(long expected, long actual, long maxDelay) {
    if( actual < expected ) {
      fail("duration "+actual+" < expected "+expected);
    }
    if( actual > expected+ maxDelay) {
      fail("duration "+actual+" > expected +delay "+(expected+maxDelay));
    }
  }

  
}
