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

package com.gip.xyna.utils.concurrent;

import junit.framework.TestCase;


/**
 *
 */
public class HashParallelReentrantLockTest extends TestCase {
  
  
  public void testLockExclusive() {
    ParallelLockTestHelper.testLockExclusive( new HashParallelReentrantLock<Integer>(32) );
  }
  public void testLockParallel() {
    ParallelLockTestHelper.testLockParallel( new HashParallelReentrantLock<Integer>(32) );
  }
 
  public void testLockReentrant() {
    
    HashParallelReentrantLock<Object> hprl = new HashParallelReentrantLock<Object>(32);
    
    Object lock = new Object();
    
    hprl.lock( lock );
    try {
      hprl.lock( lock );
      try {
        System.err.println( "hoho" ); //FIXME wie kann dieser Test so geschrieben werden, dass ein Verstoß möglich wäre?
        //Im Fhlerfall blockiert der Test hier
      } finally {
        hprl.unlock( lock );
      }
    } finally {
      hprl.unlock( lock );
    }
  }
  
   



  
  
  public static void main(String[] args) throws InterruptedException {
    
    
    HashParallelReentrantLock<Long> hl = new HashParallelReentrantLock<Long>(32);
    
    Long l1 = new Long(1);
    Long l2 = new Long(2);
    Long l3 = new Long(3);
    Long l4 = new Long(4);
     
    hl.lock( l1);
    try {
      Locker locker1 = new Locker(hl, 1);
      
      Thread t1 = new Thread( locker1);
      
      //t1.start();
      
      //Thread.sleep(1000);
      
    } finally {
      hl.unlock( l1);
    }
    
    for( int i=0; i<10; ++i ) {
      testParallel(hl, 16);
    }
  }
  

  
  private static void testParallel(HashParallelReentrantLock<Long> hl, int size) throws InterruptedException {
    Locker[] lockers = new Locker[size];
    Thread[] threads = new Thread[size];
    for( int i=0; i<size; ++i ) {
      lockers[i] = new Locker(hl, i);
      threads[i] = new Thread( lockers[i] );
    }
    long start = System.currentTimeMillis();
    for( int i=0; i<size; ++i ) {
      threads[i].start();
    }
    for( int i=0; i<size; ++i ) {
      threads[i].join();
    }
    long end = System.currentTimeMillis();
    
    System.err.println( "Test finished after "+(end-start)+" ms" );
  }

  private static class Locker implements Runnable {
    HashParallelReentrantLock<Long> hl;
    private int id;
    public Locker(HashParallelReentrantLock<Long> hl, int id) {
      this.hl = hl;
      this.id = id;
    }
    
    public void run() {
      for( int i=0; i<20; ++i ) {
      
        long lock = (int)(Math.random()*256);
        long start = System.currentTimeMillis();
        hl.lock( lock );
        try {
          long end = System.currentTimeMillis();
          //System.err.println( "id "+id+" got lock for "+lock+" after "+(end-start)+" ms" );
          try {
            Thread.sleep(10);
          }
          catch (InterruptedException e) {
            e.printStackTrace();
          }
        } finally {
          hl.unlock( lock );
        }
      }
      
    }
    
    
  }
  

}
