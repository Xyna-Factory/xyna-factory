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
package com.gip.xyna.idgeneration;



import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.idgeneration.IdDistributor.IDSource;
import com.gip.xyna.utils.timing.SleepCounter;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;



public class IdDistributorTest extends TestCase {

  public void test1() {
    IdDistributor idd = new IdDistributor(new IDSource() {

      private final AtomicLong next = new AtomicLong(0);


      public long getNextBlockStart(IdDistributor iddistr, long time) {
        //bl�cke fangen immer bei durch 5 teilbaren zahlen an, und enden bei der n�chsten durch 3 teilbaren zahl (weil blocksize=3)
        return next.getAndAdd(5);
      }
    }, 3L);
    
    try {
      Field field = IdDistributor.class.getDeclaredField("initializedFromPrefetcher");
      field.setAccessible(true);
      field.set(idd, false);
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    
    List<Integer> ids = new ArrayList<>();
    for (int i = 0; i < 14; i++) {
      ids.add((int) idd.getNext());
    }
    assertEquals(new ArrayList<Integer>(Arrays.asList(new Integer[] {0, 1, 2, 5, 10, 11, 15, 16, 17, 20, 25, 26, 30, 31})), ids);
  }


  public void testParallel() {
    final boolean testExceptions = false;
    final boolean testSlow = false;
    ExecutorService tpe = Executors.newFixedThreadPool(5);
    SleepCounter sc = new SleepCounter(1, 50, 3, TimeUnit.MILLISECONDS, true);
    final int blocksize = 10;
    final int blocksizesmall = 5;
    Set<Integer> set2 = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    int n = 10000;
    for (int i = 0; i < 2 * n / blocksizesmall; i++) {
      for (int l = 0; l < blocksizesmall; l++) {
        set2.add(i * blocksize + l);
      }
    }
    try {
      for (int k = 0; k < 100; k++) {
        System.out.println("k = " + k);
        final IdDistributor idd = new IdDistributor(new IDSource() {

          private final AtomicLong next = new AtomicLong(0);
          private final Random r = new Random();


          public long getNextBlockStart(IdDistributor iddistr, long time) {
            if (testSlow && r.nextInt(100) == 1) {
              try {
                Thread.sleep(r.nextInt(100) + 5);
              } catch (InterruptedException e) {
              }
            } else if (testExceptions && r.nextInt(100) < 10) {
              throw new RuntimeException();
            }
            // System.out.println("next block");
            return next.addAndGet(blocksize);
          }
        }, blocksizesmall);
        
        try {
          Field field = IdDistributor.class.getDeclaredField("initializedFromPrefetcher");
          field.setAccessible(true);
          field.set(idd, false);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
          e.printStackTrace();
          fail(e.getMessage());
        }
        
        final Set<Integer> set = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());

        try {
          Runnable r = new Runnable() {

            @Override
            public void run() {
              while (true) {
                try {
                  set.add((int) idd.getNext());
                  break;
                } catch (RuntimeException e) {
                  //ok
                }
              }
            }

          };
          for (int i = 0; i < n; i++) {
            try {
              tpe.execute(r);
            } catch (RejectedExecutionException e) {
              r.run();
            }
          }

          while (set.size() < n) {
            sc.sleep();
          }
          sc.reset();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

        try {
          assertTrue(set.size() == n);
          assertTrue(set2.containsAll(set));
        } catch (AssertionFailedError e) {
          System.out.println(set);
          System.out.println(set2);
          throw e;
        }
      }
    } finally {
      tpe.shutdown();
    }
  }


  public void testPerformance() {
    ExecutorService tpe = Executors.newFixedThreadPool(10);
    SleepCounter sc = new SleepCounter(1, 50, 3, TimeUnit.MILLISECONDS, true);
    final int blocksize = 10;
    final int blocksizesmall = 5;
    int n = 100000;
    try {
      for (int k = 0; k < 10; k++) {
        System.out.println("k = " + k);
        final IdDistributor idd = new IdDistributor(new IDSource() {

          private final AtomicLong next = new AtomicLong(0);


          public long getNextBlockStart(IdDistributor iddistr, long time) {
            return next.getAndAdd(blocksize);
          }
        }, blocksizesmall);
        
        try {
          Field field = IdDistributor.class.getDeclaredField("initializedFromPrefetcher");
          field.setAccessible(true);
          field.set(idd, false);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
          e.printStackTrace();
          fail(e.getMessage());
        }
        
        final AtomicLong sum = new AtomicLong(0);
        final AtomicLong cnt = new AtomicLong(0);
        try {
          Runnable r = new Runnable() {

            @Override
            public void run() {
              sum.addAndGet(idd.getNext());
              cnt.incrementAndGet();
            }

          };
          for (int i = 0; i < n; i++) {
            try {
              tpe.execute(r);
            } catch (RejectedExecutionException e) {
              r.run();
            }
          }

          while (cnt.get() < n) {
            sc.sleep();
          }
          System.out.println(sum.get());
          sc.reset();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

      }
    } finally {
      tpe.shutdown();
    }
  }


  public void testInitialization() {
    final int start = 1232;
    final int blocksize = 3;
    IdDistributor idd = new IdDistributor(new IDSource() {

      private final AtomicLong next = new AtomicLong(start);


      public long getNextBlockStart(IdDistributor iddistr, long time) {
        long n = next.get(); //1232
        if (n % 5 != 0) {
          next.set(n - (n % blocksize) + 5); //1235
        } else {
          next.addAndGet(5);
        }
        System.out.println("returning " + n);
        return n;
      }
    }, blocksize);
    
    try {
      Field field = IdDistributor.class.getDeclaredField("initializedFromPrefetcher");
      field.setAccessible(true);
      field.set(idd, false);
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    
    List<Integer> ids = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      ids.add((int) idd.getNext());
    }
    assertEquals(new ArrayList<Integer>(Arrays.asList(new Integer[] {1232, 1235, /* 1236 ist blockende (durch 3 teilbar) */ 1240, 1241,
        /* 1242 ist blockende */ 1245, 1246, 1247})), ids);
  }
  

  public void testCurrent() {
    final IdDistributor idd = new IdDistributor(new IDSource() {

      @Override
      public long getNextBlockStart(IdDistributor iddistr, long time) {
        return iddistr.getCurrent();
      }

    }, 10);
    
    try {
      Field field = IdDistributor.class.getDeclaredField("initializedFromPrefetcher");
      field.setAccessible(true);
      field.set(idd, false);
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    
    for (int i = 0; i < 30; i++) {
      assertEquals(i + 1, idd.getNext());
    }
  }


}
