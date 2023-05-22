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
package com.gip.xyna.utils.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;


/**
 *
 */
public class IncrementableCountDownLatchTest extends TestCase {

  private static class CountDown extends Thread {

    private CountDownLatch cdl;
    private long millis;
    private String id;
    private IncrementableCountDownLatch icdl;

    public CountDown(CountDownLatch cdl, long millis, String id) {
      this.cdl = cdl;
      this.millis = millis;
      this.id = id;
    }
    public CountDown(IncrementableCountDownLatch icdl, long millis, String id) {
      this.icdl = icdl;
      this.millis = millis;
      this.id = id;
    }
   
    @Override
    public void run() {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      //System.out.println(id + " countDown");
      if( cdl != null ) cdl.countDown();
      if( icdl != null ) icdl.countDown();
    }
    
  }
  
  private static class Increment extends Thread {

    private long millis;
    private String id;
    private IncrementableCountDownLatch icdl;

    public Increment(IncrementableCountDownLatch icdl, long millis, String id) {
      this.icdl = icdl;
      this.millis = millis;
      this.id = id;
    }
   
    @Override
    public void run() {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      boolean res = icdl.increment();
      System.out.println(id + " increment -> " + res );
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

  
  
  
  
  
  
  
  public void test_2CountDown_N() {
    CountDownLatch cdl = new CountDownLatch(2);
    
    long start = System.currentTimeMillis();
    new CountDown(cdl,100, "CD1").start();
    new CountDown(cdl,50, "CD2").start();
    
    try {
      cdl.await();
      long duration = System.currentTimeMillis()-start;
      System.out.println( "await returned after "+duration+" ms" );
      assertDuration( 100, duration, 5 );
    } catch (InterruptedException e) {
      fail(" Unexpected interruption "+ e.getMessage() );
    }
  }
  
  public void test_2CountDown_I() {
    IncrementableCountDownLatch cdl = new IncrementableCountDownLatch(2);
    
    long start = System.currentTimeMillis();
    new CountDown(cdl,100, "CD1").start();
    new CountDown(cdl,50, "CD2").start();
    
    try {
      cdl.await();
      long duration = System.currentTimeMillis()-start;
      System.out.println( "await returned after "+duration+" ms" );
      assertDuration( 100, duration, 5 );
    } catch (InterruptedException e) {
      fail(" Unexpected interruption "+ e.getMessage() );
    }
  }
 

  public void test_CountDown0_N() {
    CountDownLatch cdl = new CountDownLatch(0);
    
    long start = System.currentTimeMillis();
    new CountDown(cdl,50, "CD1").start();
    
    try {
      cdl.await();
      long duration = System.currentTimeMillis()-start;
      System.out.println( "await returned after "+duration+" ms" );
      assertDuration( 0, duration, 5 );
    } catch (InterruptedException e) {
      fail(" Unexpected interruption "+ e.getMessage() );
    }
  }

  public void test_CountDown0_I() {
    IncrementableCountDownLatch cdl = new IncrementableCountDownLatch(0);
    
    long start = System.currentTimeMillis();
    new CountDown(cdl,50, "CD1").start();
    
    try {
      cdl.await();
      long duration = System.currentTimeMillis()-start;
      System.out.println( "await returned after "+duration+" ms" );
      assertDuration( 0, duration, 5 );
    } catch (InterruptedException e) {
      fail(" Unexpected interruption "+ e.getMessage() );
    }
  }

  public void test_2CountDown3_N() {
    CountDownLatch cdl = new CountDownLatch(2);
    
    long start = System.currentTimeMillis();
    new CountDown(cdl,100, "CD1").start();
    new CountDown(cdl,50, "CD2").start();
    new CountDown(cdl,20, "CD3").start();
    
    try {
      cdl.await();
      long duration = System.currentTimeMillis()-start;
      System.out.println( "await returned after "+duration+" ms" );
      assertDuration( 50, duration, 5 );
    } catch (InterruptedException e) {
      fail(" Unexpected interruption "+ e.getMessage() );
    }
  }
  
  public void test_2CountDown3_I() {
    IncrementableCountDownLatch cdl = new IncrementableCountDownLatch(2);
    
    long start = System.currentTimeMillis();
    new CountDown(cdl,100, "CD1").start();
    new CountDown(cdl,50, "CD2").start();
    new CountDown(cdl,20, "CD3").start();
    
    try {
      cdl.await();
      long duration = System.currentTimeMillis()-start;
      System.out.println( "await returned after "+duration+" ms" );
      assertDuration( 50, duration, 5 );
    } catch (InterruptedException e) {
      fail(" Unexpected interruption "+ e.getMessage() );
    }
  }
  
  public void test_Increment() {
    IncrementableCountDownLatch cdl = new IncrementableCountDownLatch(2);
    
    long start = System.currentTimeMillis();
    new CountDown(cdl,100, "CD1").start();
    new CountDown(cdl,50, "CD2").start();
    new CountDown(cdl,20, "CD3").start();
    new Increment(cdl,30, "INC1").start();
    
    try {
      cdl.await();
      long duration = System.currentTimeMillis()-start;
      System.out.println( "await returned after "+duration+" ms" );
      assertDuration( 100, duration, 5 );
    } catch (InterruptedException e) {
      fail(" Unexpected interruption "+ e.getMessage() );
    }
  }
  
  public void test_Increment0() {
    IncrementableCountDownLatch cdl = new IncrementableCountDownLatch(0);
    long start = System.currentTimeMillis();
    new Increment(cdl,10, "INC1").start();
    new Increment(cdl,20, "INC2").start();
    new CountDown(cdl,30, "CD3").start();
    new Increment(cdl,50, "INC3").start();
    new CountDown(cdl,60, "CD2").start();
    new CountDown(cdl,100, "CD1").start();
    
    try {
      Thread.sleep(30);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
    try {
      cdl.await();
      long duration = System.currentTimeMillis()-start;
      System.out.println( "await returned after "+duration+" ms" );
      assertDuration( 100, duration, 5 );
    } catch (InterruptedException e) {
      fail(" Unexpected interruption "+ e.getMessage() );
    }
  }
  
  public void test_Increment_AfterAwait() {
    IncrementableCountDownLatch cdl = new IncrementableCountDownLatch(2);
    
    assertEquals( 2, cdl.getCount() );
    assertTrue( cdl.countDown() );
    assertEquals( 1, cdl.getCount() );
    assertTrue( cdl.increment() );
    assertEquals( 2, cdl.getCount() );
    assertTrue( cdl.countDown() );
    assertTrue( cdl.countDown() );
    assertEquals( 0, cdl.getCount() );
    assertFalse( cdl.countDown() );
    assertEquals( -1, cdl.getCount() );
    assertFalse( cdl.increment() );
    assertEquals( 0, cdl.getCount() );
    assertFalse( cdl.increment() );
    assertEquals( 1, cdl.getCount() );
    assertFalse( cdl.countDown() );
    assertEquals( 0, cdl.getCount() );
  }

  //testen, dass bei gleichzeitigem runterz�hlen auf 0  und hochz�hlen auf 1 korrekt erkannt wird, dass cdl nicht hochgez�hlt werden darf
  public void test_Race() throws InterruptedException {
    int n = 2;
    final IncrementableCountDownLatch cdl = new IncrementableCountDownLatch(n);
    final AtomicBoolean finished = new AtomicBoolean(false);
    final AtomicInteger cnt = new AtomicInteger(0);
    final AtomicBoolean failed = new AtomicBoolean(false);
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(n, n, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    for (int i = 0; i<n; i++) {
      tpe.execute(new Runnable() {

        public void run() {
          cdl.countDown();
          for (int i = 0; i<1000; i++) {
            if (!cdl.increment()) {
              continue;
            }
            if (finished.get()) {
              failed.set(true);
              return;
            }
            cnt.incrementAndGet();
            cdl.countDown();
          }
        }
        
      });
    }
    assertTrue(cdl.await(3, TimeUnit.SECONDS));
    finished.set(true);
    System.out.println("cnt=" + cnt.get());
    assertTrue(failed.get());
  }
 
}
