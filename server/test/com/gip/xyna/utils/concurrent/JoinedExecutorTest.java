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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.text.html.HTMLDocument.RunElement;

import junit.framework.TestCase;

import com.gip.xyna.utils.collections.WrappedMap;


/**
 *
 */
public class JoinedExecutorTest extends TestCase {

  private static void assertApproxEquals(long expected, long actual, long approx) {
    if( Math.abs(expected-actual) > approx ) {
      fail("difference "+Math.abs(expected-actual)+" between expected "+expected+" and actual "+actual+" is greater than allowed "+approx);
    }
  }
  
  private static class JoinedExecutorCounter extends JoinedExecutor<Integer> {
    AtomicInteger ai;
    public JoinedExecutorCounter(int start) {
      ai = new AtomicInteger(start);
    }
    public Integer executeInternal() {
      return ai.getAndIncrement();
    }
  }
  
  private static class JoinedExecutorCounterWithLatch extends JoinedExecutor<Integer> {
    AtomicInteger ai;
    private CountDownLatch cdl;
    public JoinedExecutorCounterWithLatch(int start, CountDownLatch cdl) {
      ai = new AtomicInteger(start);
      this.cdl = cdl;
    }
    public Integer executeInternal() {
      try {
        cdl.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return ai.getAndIncrement();
    }
  }
  
  private static class JoinedExecutorCounterWithSleep extends JoinedExecutor<Integer> {
    AtomicInteger ai;
    private long millis;
    public JoinedExecutorCounterWithSleep(int start,long millis) {
      ai = new AtomicInteger(start);
      this.millis= millis;
    }
    public Integer executeInternal() {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return ai.getAndIncrement();
    }
  }

  
  
  public static void testA() {
    JoinedExecutorCounter jec = new JoinedExecutorCounter(12);
    try {
      assertEquals( new Integer(12), jec.execute() );  
      assertEquals( new Integer(13), jec.execute() );
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail("no exception expected "+ e.getMessage());
    }
  }
  
  public static void testB1() {
    CountDownLatch cdl = new CountDownLatch(0);
    
    JoinedExecutorCounterWithLatch jecwl = new JoinedExecutorCounterWithLatch(23,cdl);
    
    Measurement m1 = new Measurement(jecwl);
    Measurement m2 = new Measurement(jecwl);
    
    m1.start();
    sleep(10);
    m2.start();
   
    sleep(10); //wichtig, damit beide Thread laufen und in execute h�ngen
    
    cdl.countDown();
    
    sleep(10);
    assertEquals( new Integer(23), m1.getResult() ); 
    assertEquals( new Integer(24), m2.getResult() ); 
    assertApproxEquals( 10, m2.getTimestamp()-m1.getTimestamp(), 1); //Ergebnis in etwa 10 ms Abstand erhalten    
  }

  
  public static void testB2() {
    CountDownLatch cdl = new CountDownLatch(1);
    
    JoinedExecutorCounterWithLatch jecwl = new JoinedExecutorCounterWithLatch(23,cdl);
    
    Measurement m1 = new Measurement(jecwl);
    Measurement m2 = new Measurement(jecwl);
    
    m1.start();
    sleep(10);
    m2.start();
   
    sleep(10); //wichtig, damit beide Thread laufen und in execute h�ngen
    
    cdl.countDown();
    
    sleep(10);
    assertEquals( new Integer(23), m1.getResult() ); 
    assertEquals( new Integer(23), m2.getResult() ); 
    assertApproxEquals( 0, m2.getTimestamp()-m1.getTimestamp(), 1); //Ergebnis in gleicher Millisekunde erhalten    
  }
  
  
  public static void testC1() {
    JoinedExecutorCounterWithSleep jecws = new JoinedExecutorCounterWithSleep(23,5);
    
    Measurement m1 = new Measurement(jecws);
    Measurement m2 = new Measurement(jecws);
    
    m1.start();
    sleep(10);
    m2.start();
   
    sleep(10); //wichtig, damit beide Threads fertig werden konnten
    
    assertEquals( new Integer(23), m1.getResult() ); 
    assertEquals( new Integer(24), m2.getResult() );
    assertApproxEquals( 10, m2.getTimestamp()-m1.getTimestamp(), 1); //Ergebnis in etwa 10 ms Abstand erhalten    
  }


  public static void testC2() {
    JoinedExecutorCounterWithSleep jecws = new JoinedExecutorCounterWithSleep(23,20);
    
    Measurement m1 = new Measurement(jecws);
    Measurement m2 = new Measurement(jecws);
    
    m1.start();
    sleep(10);
    m2.start();
   
    sleep(15); //wichtig, damit beide Threads fertig werden konnten
    
    assertEquals( new Integer(23), m1.getResult() ); 
    assertEquals( new Integer(23), m2.getResult() ); 
    assertApproxEquals( 0, m2.getTimestamp()-m1.getTimestamp(), 1); //Ergebnis in gleicher Millisekunde erhalten    
   
  }
  
  
  public static void testD1() {
    JoinedExecutorCounterWithSleep jecws = new JoinedExecutorCounterWithSleep(0,3);
    ConcurrentCounterMap counter = new ConcurrentCounterMap();
    LoadMeasurement lm = new LoadMeasurement(200,jecws,0,counter);
    
    lm.start();
    sleep(250);
    int next = 0;
    try {
      next = jecws.execute().intValue();
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail("no exception expected "+ e.getMessage());
    }
    
    //int expected = 200/3; //64 -66, schwankt leider 
    //deswegen nicht pr�fen assertEquals( expected, next);
    
    assertEquals( next, lm.getNumResults() );
    System.out.println( counter);
  }  
  
  public static void testD2() {
    JoinedExecutorCounterWithSleep jecws = new JoinedExecutorCounterWithSleep(0,3);
    ConcurrentCounterMap counter = new ConcurrentCounterMap();
    int size = 10;
    LoadMeasurement[] lms = new LoadMeasurement[size];
    for( int i=0; i<size; ++i ) {
      lms[i] = new LoadMeasurement(200,jecws,7,counter);
    }
    for( int i=0; i<size; ++i ) {
      sleep(1);
      lms[i].start();
    }
    
    sleep(250);
    int next = 0;
    try {
      next = jecws.execute().intValue();
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail("no exception expected "+ e.getMessage());
    }
    
    System.out.println(next );
    
    for( int i=0; i<size; ++i ) {
      System.out.println( lms[i].getNumResults() );
    }
    System.out.println( counter);
    int sum = 0;
    for( int i=0; i< next; ++i ) {
      int count = counter.get(i).get();
      System.out.println("Count("+i+") = "+ count );
      sum += count;
    }
    System.out.println(" sum = "+sum);
  }  

  
  private static class JoinedExecutorThrowsWithSleep extends JoinedExecutor<Integer> {
    private long millis;
    private RuntimeException runtimeException;
    private Error error;
    boolean firstExecution = true;
    public JoinedExecutorThrowsWithSleep(RuntimeException runtimeException, long millis) {
      this.runtimeException = runtimeException;
      this.millis= millis;
    }

    public JoinedExecutorThrowsWithSleep(Error error, int millis) {
      this.error = error;
      this.millis= millis;
    }
    
    public Integer executeInternal() {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if( firstExecution ) {
        firstExecution = false;
        if( runtimeException != null ) {
          throw runtimeException;
        } else if( error != null ) {
          throw error;
        } else {
          return 54321;
        }
      } else {
        return 12345;
      }
    }
  }
  
  public static void testE1() {
    String msg = "test";
    JoinedExecutorThrowsWithSleep jetws = new JoinedExecutorThrowsWithSleep(new RuntimeException(msg), 10);
    try {
      Integer dummy = jetws.execute();
      fail( "Exception expected");
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail("no InterruptedException expected "+ e.getMessage());
    } catch( RuntimeException e ) {
      assertEquals(msg, e.getMessage());
    }
  }
  
  public static void testE2() {
    String msg = "test";
    JoinedExecutorThrowsWithSleep jetws = new JoinedExecutorThrowsWithSleep(new RuntimeException(msg), 20);
    
    Measurement m1 = new Measurement(jetws);
    Measurement m2 = new Measurement(jetws);
    
    m1.start();
    sleep(10);
    m2.start();
   
    sleep(15); //wichtig, damit beide Threads fertig werden konnten
    
    assertNull( m1.getResult() ); 
    assertNull( m2.getResult() ); 
    
    assertApproxEquals( 0, m2.getTimestamp()-m1.getTimestamp(), 1); //Ergebnis in gleicher Millisekunde erhalten    
    assertEquals( "java.lang.RuntimeException: "+msg, String.valueOf(m1.getThrowable()) );
    assertEquals( "java.lang.RuntimeException: "+msg, String.valueOf(m2.getThrowable()) );
    
  }

  public static void testE3() {
    String msg = "test";
    JoinedExecutorThrowsWithSleep jetws = new JoinedExecutorThrowsWithSleep(new Error(msg), 20);
    
    Measurement m1 = new Measurement(jetws);
    Measurement m2 = new Measurement(jetws);
    
    m1.start();
    sleep(10);
    m2.start();
   
    sleep(15); //wichtig, damit beide Threads fertig werden konnten
    assertFalse( m2.isReady() );
    sleep(20);
    assertTrue( m2.isReady() );
    
    
    assertNull( m1.getResult() ); 
    assertEquals( new Integer(12345), m2.getResult() ); 
    assertApproxEquals( 20, m2.getTimestamp()-m1.getTimestamp(), 1); //Ergebnis nach Retry erhalten    
    assertEquals( "java.lang.Error: "+msg, String.valueOf(m1.getThrowable()) );
    assertEquals( "null", String.valueOf(m2.getThrowable()) );
    
  }


  
  
  
  
  
  
  
  
  private static class ConcurrentCounterMap extends WrappedMap<Integer, AtomicInteger> {

    public ConcurrentCounterMap() {
      super( new ConcurrentHashMap<Integer, AtomicInteger>() );
    }
    
    ConcurrentHashMap<Integer, AtomicInteger> getConcurrentHashMap() {
      return (ConcurrentHashMap<Integer, AtomicInteger>)wrapped;
    }
    
    public void count(Integer key) {
      AtomicInteger counter = get(key);
      if( counter == null ) {
        getConcurrentHashMap().putIfAbsent(key, new AtomicInteger(0) );
        counter = get(key);
      }
      counter.getAndIncrement();
    }
    
  }
  
  
  
  private static class LoadMeasurement extends Thread {
    
    long duration;
    private JoinedExecutor<Integer> joinedExecutor;
    int numResults = 0;
    private long millis;
    private ConcurrentCounterMap counter;
    
    public LoadMeasurement(long duration, JoinedExecutor<Integer> joinedExecutor, long millis, ConcurrentCounterMap counter) {
      this.duration = duration;
      this.joinedExecutor = joinedExecutor;
      this.millis = millis;
      this.counter = counter;
    }
    public void run() {
      long start = System.currentTimeMillis();
      while ( start+duration > System.currentTimeMillis() ) {
        if( millis > 0 ) {
          try {
            Thread.sleep(millis);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        Integer result;
        try {
          result = joinedExecutor.execute();
        } catch (InterruptedException e) {
          e.printStackTrace();
          result = new Integer(Integer.MIN_VALUE);
        }
        counter.count(result);
        ++numResults;
      }
    }
    
    public int getNumResults() {
      return numResults;
    }
    
  }
  
  
  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static class Measurement extends Thread {

    private JoinedExecutor<Integer> joinedExecutor;
    private Integer result;
    private long timestamp;
    private Throwable throwable;
    private boolean isReady;

    public Measurement(JoinedExecutor<Integer> joinedExecutor) {
      this.joinedExecutor = joinedExecutor;
    }
    
    @Override
    public void run() {
      try {
        result = joinedExecutor.execute();
      } catch (Throwable t) {
        this.throwable = t;
      }
      timestamp = System.currentTimeMillis();
      isReady = true;
    }
    
    public Integer getResult() {
      return result;
    }
    
    public long getTimestamp() {
      return timestamp;
    }
    
    public Throwable getThrowable() {
      return throwable;
    }
    
    public boolean isReady() {
      return isReady;
    }
  }
  
  
  
}
