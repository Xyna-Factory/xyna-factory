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
package com.gip.xyna.utils.concurrent;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;


public class ParallelLockTestHelper {

  
  public static void testLockExclusive(ParallelLock<Integer> pl ) {
    Integer lock = new Integer(1);

    long start = System.currentTimeMillis();
    lockInOtherThread( pl, lock, 500 );

    pl.lock( lock );
    try {
      long end = System.currentTimeMillis();

      System.err.println( (end-start) );

      Assert.assertTrue( "Lock not exclusive", (end-start) >= 500 );
    } finally {
      pl.unlock( lock );
    }
  }
  
  public static void testLockParallel(ParallelLock<Integer> pl) {    
    Integer lock1 = new Integer(1);
    Integer lock2 = new Integer(2);
    
    long start = System.currentTimeMillis();
    lockInOtherThread( pl, lock1, 500 );
    
    pl.lock( lock2 );
    try {
      long end = System.currentTimeMillis();
      
      System.err.println( (end-start) );
      
      Assert.assertTrue( "Lock is exclusive", (end-start) < 500 ); // ungefähr 50, durch Sicherung im lockInOtherThread
    } finally {
      pl.unlock( lock2 );
    }
  }
  

  public static void assertSimilar(long a, long b, long maxdiff) {
    long ad = Math.abs(a-b);
    if( ad > maxdiff ) {
      Assert.assertEquals("", "Difference |"+a+" - "+b+"| > "+maxdiff);
    }
  }

  public static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static <T> void lockInOtherThread( final ParallelLock<T> pl, final T lock, final int duration) {
    new Thread(){
      public void run() {
        pl.lock( lock );
        try {
          Thread.sleep(duration);
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
          pl.unlock( lock );
        }
      }
    }.start();
    try {
      Thread.sleep(duration/10); //Sicherung, dass Thread wirklich gestartet ist
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


  
  public static <T> AtomicLong measureLockTimeInOtherThread(final ParallelLock<T> pl, final T lock,
                                                            final int duration) {
    final AtomicLong lockTime = new AtomicLong();
    new Thread(){
      public void run() {
        pl.lock( lock );
        try {
          lockTime.set( System.currentTimeMillis() );
          Thread.sleep(duration);
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
          pl.unlock( lock );
        }
      }
    }.start();
    return lockTime;
  }

  
}
