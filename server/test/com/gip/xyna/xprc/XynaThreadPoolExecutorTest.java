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
package com.gip.xyna.xprc;



import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import com.gip.xyna.xprc.XynaThreadPoolExecutor.EagerThreadPoolSizeStrategy;
import com.gip.xyna.xprc.XynaThreadPoolExecutor.LazyThreadPoolSizeStrategy;
import com.gip.xyna.xprc.XynaThreadPoolExecutor.ThreadPoolUsageStrategy;
import com.gip.xyna.xprc.xsched.XynaDaemonFactory;



public class XynaThreadPoolExecutorTest extends TestCase {

  
  
  private static class EmptyRunnable extends XynaRunnable {
    private AtomicInteger execCnt;
    private long sleep;

    public EmptyRunnable(AtomicInteger execCnt, long sleep) {
      this.execCnt = execCnt;
      this.sleep = sleep;
    }

    public void run() {
      try {
        Thread.sleep(sleep);
        execCnt.incrementAndGet();
        //System.err.println( System.currentTimeMillis() );
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    
  }
  
  private static class RejectableRunnable extends EmptyRunnable {
    private RejectedExecutionHandlerImpl rehi;

    public RejectableRunnable(AtomicInteger execCnt, long sleep, RejectedExecutionHandlerImpl rehi) {
      super(execCnt,sleep);
      this.rehi = rehi;
    }
    
    @Override
    public boolean isRejectable() {
      return true;
    }
    
    @Override
    public void rejected() {
      rehi.rejectedExecution(this, null);
    }
    
  }
  
  
  private static class Starter implements Runnable {

    private XynaThreadPoolExecutor tpe;
    private AtomicInteger cnt;
    private Runnable runnable;

    public Starter(XynaThreadPoolExecutor tpe, AtomicInteger cnt, Runnable runnable) {
      this.tpe = tpe;
      this.cnt = cnt;
      this.runnable = runnable;
    }

    public void run() {
      tpe.execute(runnable);
      cnt.incrementAndGet();
    } 
    
  }
  
  private static class CountDownLatchRunnable extends XynaRunnable {
    private CountDownLatch cdl;
    private boolean await;

    public CountDownLatchRunnable(boolean await ) {
      this.await = await;
      this.cdl = new CountDownLatch(1);
    }
    
    public void run() {
      try {
        if( await ) {
          cdl.await();
        } else {
          cdl.countDown();
        }
      } catch (InterruptedException e) {
      }
    }

    public void countDown() {
      if( await ) {
        cdl.countDown();
      } else {
        throw new RuntimeException("countDown not allowed for CountDownLatchRunnable(false)");
      }
    }
    
    public void await() throws InterruptedException {
      if( await ) {
        throw new RuntimeException("countDown not allowed for CountDownLatchRunnable(true)");
      } else {
        cdl.await();
      }
    }
    
  }
  
  public static class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {
    private AtomicInteger rejectedCnt = new AtomicInteger();
    private ArrayList<Runnable> rejectedList;
    private boolean fillList;
    
    public RejectedExecutionHandlerImpl(boolean fillList) {
      this.fillList = fillList;
      if( fillList ) {
        this.rejectedList = new ArrayList<Runnable>();
      }
    }

    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
      rejectedCnt.incrementAndGet();
      if( fillList ) {
        rejectedList.add(r);
      }
    }

  }
  
  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  
  
  private String countsToString(XynaThreadPoolExecutor tpe) {
    return "C"+tpe.getCorePoolSize()+" A"+tpe.getActiveCount()+" W"+tpe.getWaitingCount()+" R"+tpe.getRejectedTasks();
  }



  public void testReject_Rejectable() throws InterruptedException {
    XynaThreadPoolExecutor tpe =
        new XynaThreadPoolExecutor(1, 1, 50, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(3), Executors
            .defaultThreadFactory(), "bla", true);
    RejectedExecutionHandlerImpl rehi_tpe = new RejectedExecutionHandlerImpl(false);
    
    tpe.setRejectedExecutionHandler(rehi_tpe);

    RejectedExecutionHandlerImpl rehi_run = new RejectedExecutionHandlerImpl(true);
    
    
    final AtomicInteger execCnt = new AtomicInteger();
    final Runnable r1 = new RejectableRunnable(execCnt,10,rehi_run);
    final Runnable r2 = new RejectableRunnable(execCnt,20,rehi_run);
    final Runnable r3 = new RejectableRunnable(execCnt,30,rehi_run);
    final Runnable r4 = new RejectableRunnable(execCnt,40,rehi_run);
    
    CountDownLatchRunnable cdlr = new CountDownLatchRunnable(true);
   
    tpe.execute(cdlr);
    tpe.execute(r1);
    tpe.execute(r2);
    tpe.execute(r3);
    tpe.execute(r4);
    
    cdlr.countDown();
    tpe.awaitTermination(150, TimeUnit.MILLISECONDS);
    
    assertEquals( 0, rehi_tpe.rejectedCnt.get() ); //RejectedExecutionHandler hat nichts zu tun, ...
    assertEquals( 1, rehi_run.rejectedCnt.get() );  //... da RejectableRunnable sich um reject kümmern muss
    assertEquals( 3, execCnt.get() );
    assertEquals( r1, rehi_run.rejectedList.get(0) ); //erstes (ältestes) Runnable wird verworfen
    
  }

  public void testReject_NotRejectable() throws InterruptedException {
    XynaThreadPoolExecutor tpe =
        new XynaThreadPoolExecutor(1, 1, 50, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(3), Executors
            .defaultThreadFactory(), "bla", true);
    RejectedExecutionHandlerImpl rehi = new RejectedExecutionHandlerImpl(true);
    tpe.setRejectedExecutionHandler(rehi);

    final AtomicInteger execCnt = new AtomicInteger();
    final Runnable r1 = new EmptyRunnable(execCnt,10);
    final Runnable r2 = new EmptyRunnable(execCnt,20);
    final Runnable r3 = new EmptyRunnable(execCnt,30);
    final Runnable r4 = new EmptyRunnable(execCnt,40);
   
    CountDownLatchRunnable cdlr = new CountDownLatchRunnable(true);
   
    tpe.execute(cdlr);
    tpe.execute(r1);
    tpe.execute(r2);
    tpe.execute(r3);
    tpe.execute(r4);
    
    cdlr.countDown();
    tpe.awaitTermination(100, TimeUnit.MILLISECONDS);
    
    assertEquals( 1, rehi.rejectedCnt.get() ); //RejectedExecutionHandler hat nichts zu tun, ...
    assertEquals( 3, execCnt.get() );
    assertEquals( r4, rehi.rejectedList.get(0) ); //neuestes Runnable wird verworfen
  
  }


  public void atestLastRingBuffer() throws InterruptedException {
    XynaThreadPoolExecutor tpe =
        new XynaThreadPoolExecutor(1, 1, 50, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000), Executors
            .defaultThreadFactory(), "bla", true);
    ThreadPoolExecutor starterThread = new ThreadPoolExecutor(20, 20, 50, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));
    AtomicInteger execCnt = new AtomicInteger();
    //Runnable r = new EmptyRunnable(execCnt,10);
    Runnable r = new RejectableRunnable(execCnt,10, null);
    RejectedExecutionHandlerImpl rehi = new RejectedExecutionHandlerImpl(false);
    tpe.setRejectedExecutionHandler(rehi);
    
    AtomicInteger cnt2 = new AtomicInteger();
    Starter starter = new Starter(tpe, cnt2, r );
    
    int numberOfRequests = 50000; //0;  //00;
    for (int i = 0; i < numberOfRequests; i++) {
      while (true) {
        try {
          starterThread.execute(starter);
          break;
        } catch (RejectedExecutionException e) {

        }
      }
    }
        
    System.out.println("cnt2 "+cnt2.get());
    
    System.out.println(rehi.rejectedCnt.get());
    System.out.println(execCnt.get());
    System.out.println((execCnt.get() + rehi.rejectedCnt.get() ));
    
    tpe.awaitTermination(11, TimeUnit.SECONDS);
    
    System.out.println(rehi.rejectedCnt.get());
    System.out.println(execCnt.get());
    System.out.println((execCnt.get() + rehi.rejectedCnt.get() ));
    
    
  }
  
  
  public void testWithQueue_Default() {
    XynaThreadPoolExecutor tpe =
        new XynaThreadPoolExecutor(1, 10, 50, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(50), Executors
            .defaultThreadFactory(), "bla", true);
    RejectedExecutionHandlerImpl rehi = new RejectedExecutionHandlerImpl(false);
    tpe.setRejectedExecutionHandler(rehi);

    AtomicInteger execCnt = new AtomicInteger();
    
    for( int i=0; i< 10; ++i ) {
      tpe.execute( new EmptyRunnable(execCnt, 1000) );
    }
    sleep(10);
    assertEquals("C1 A1 W9 R0", countsToString(tpe) );
    
    for( int i=10; i< 50; ++i ) {
      tpe.execute( new EmptyRunnable(execCnt, 1000) );
    }
    sleep(10);
    assertEquals("C1 A1 W49 R0", countsToString(tpe) );
   
    for( int i=50; i< 55; ++i ) {
      tpe.execute( new EmptyRunnable(execCnt, 1000) );
    }
    sleep(10);
    assertEquals("C1 A5 W50 R0", countsToString(tpe) );
   
    for( int i=55; i< 60; ++i ) {
      tpe.execute( new EmptyRunnable(execCnt, 1000) );
    }
    sleep(10);
    assertEquals("C1 A10 W50 R0", countsToString(tpe) );
    
    tpe.execute( new EmptyRunnable(execCnt, 1000) );
    sleep(10);
    assertEquals("C1 A10 W50 R1", countsToString(tpe) );
    
    tpe.execute( new EmptyRunnable(execCnt, 1000) );
    sleep(10);
    assertEquals("C1 A10 W50 R2", countsToString(tpe) );

  }


  public void testWithQueue_Eager() {
    XynaThreadPoolExecutor tpe =
        new XynaThreadPoolExecutor(1, 10, 50, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(50), Executors
            .defaultThreadFactory(), "bla", true);
    RejectedExecutionHandlerImpl rehi = new RejectedExecutionHandlerImpl(false);
    tpe.setRejectedExecutionHandler(rehi);
    tpe.setThreadPoolSizeStrategy( new EagerThreadPoolSizeStrategy(tpe) ); 
    
    AtomicInteger execCnt = new AtomicInteger();
    
    for( int i=0; i< 10; ++i ) {
      tpe.execute( new EmptyRunnable(execCnt, 1000) );
    }
    sleep(10);
    assertEquals("C1 A10 W0 R0", countsToString(tpe) );
    
    for( int i=10; i< 15; ++i ) {
      tpe.execute( new EmptyRunnable(execCnt, 1000) );
    }
    sleep(10);
    assertEquals("C1 A10 W5 R0", countsToString(tpe) );
   
    listThreads("while:");
    
    sleep(1000);
    assertEquals("C1 A5 W0 R0", countsToString(tpe) );
    
    sleep(1000);
    assertEquals("C1 A0 W0 R0", countsToString(tpe) );
    
  }
  
  public void testEager2() {
    XynaThreadPoolExecutor tpe =  new XynaThreadPoolExecutor(10, 100, 1, TimeUnit.SECONDS,
                               1000, new XynaDaemonFactory(), "Planning", false);
    //tpe.setThreadPoolUsageStrategy( new ThreadPoolUsageStrategyDependingOnCaller(tpe) );
    tpe.setThreadPoolSizeStrategy( new EagerThreadPoolSizeStrategy(tpe) ); 
    
    AtomicInteger execCnt = new AtomicInteger();
    
    for( int i=0; i<5; ++i ) {
      tpe.execute( new EmptyRunnable(execCnt, 1000) );
    }
    sleep(10);
    assertEquals("C10 A5 W0 R0", countsToString(tpe) );
  }

  public void testWithQueue_Lazy() {
    XynaThreadPoolExecutor tpe =
        new XynaThreadPoolExecutor(1, 10, 50, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(50), Executors
            .defaultThreadFactory(), "bla", true);
    RejectedExecutionHandlerImpl rehi = new RejectedExecutionHandlerImpl(false);
    tpe.setRejectedExecutionHandler(rehi);
    tpe.setThreadPoolSizeStrategy( new LazyThreadPoolSizeStrategy(tpe,3) ); 
    
    AtomicInteger execCnt = new AtomicInteger();
    
    for( int i=0; i< 10; ++i ) {
      tpe.execute( new EmptyRunnable(execCnt, 1000) );
      //System.err.println( i + ": "+ countsToString(tpe) );
    }
    sleep(10);
    assertEquals("C1 A3 W7 R0", countsToString(tpe) );
    
    for( int i=10; i< 25; ++i ) {
      tpe.execute( new EmptyRunnable(execCnt, 1000) );
      //System.err.println( i + ": "+ countsToString(tpe) );
    }
    sleep(10);
    assertEquals("C1 A7 W18 R0", countsToString(tpe) );
   
    for( int i=25; i< 40; ++i ) {
      tpe.execute( new EmptyRunnable(execCnt, 1000) );
      //System.err.println( i + ": "+ countsToString(tpe) );
    }
    sleep(10);
    assertEquals("C1 A10 W30 R0", countsToString(tpe) );

  }


  public void testTakeWaits() {
    ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
    long cputime1 = sumCPUTimeOfThreads(tbean);
    XynaThreadPoolExecutor tpe =
        new XynaThreadPoolExecutor(1, 1, 20, TimeUnit.SECONDS, 500, Executors.defaultThreadFactory(), "Test", true);
    AtomicInteger execCnt = new AtomicInteger();
    for (int i = 0; i < 10; i++) {
      tpe.execute(new EmptyRunnable(execCnt, 100));
    }
    sleep(10000);
    long cputime2 = sumCPUTimeOfThreads(tbean);
    assertTrue("took " + (cputime2 - cputime1) + "ms", cputime2 - cputime1 < 1000);
  }


  private long sumCPUTimeOfThreads(ThreadMXBean tbean) {
    long cputime = 0;
    for (long t : tbean.getAllThreadIds()) {
      cputime += tbean.getThreadCpuTime(t)/1000000;
    }
    return cputime;
  }


  private void listThreads(String msg) {
    System.err.println( msg );
    Thread[] threads = new Thread[20];
    Thread.enumerate(threads);
    for( int t =0; t < 20; ++t ) {
      if( threads[t] != null ) {
        System.err.println( threads[t].getName() );
      }
    }
    System.err.println();
  }
  

}
