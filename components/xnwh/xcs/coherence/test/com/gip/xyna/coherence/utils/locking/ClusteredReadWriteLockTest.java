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
package com.gip.xyna.coherence.utils.locking;



import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;

import junit.framework.TestCase;

import com.gip.xyna.coherence.CacheController;
import com.gip.xyna.coherence.CacheControllerFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCalleeProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.NodeConnectionProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.java.InterconnectCalleeJava;



public class ClusteredReadWriteLockTest extends TestCase {

  public void test1() {
    Random random = new Random();
    final int controllerCount = 4;
    final CacheController[] controllers = new CacheController[controllerCount];
    final CacheController ccID0 = CacheControllerFactory.newCacheController();
    ccID0.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    ccID0.setupNewCluster(); //neues cluster

    controllers[0] = ccID0;
    for (int i = 1; i < controllerCount; i++) {
      controllers[i] = CacheControllerFactory.newCacheController();
      controllers[i].addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
      controllers[i].connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
          .getJavaProvider((InterconnectCalleeJava) controllers[random.nextInt(i)].getCallees().get(0)));
    }

    final ClusteredReadWriteLock[] locks = new ClusteredReadWriteLock[controllerCount];
    locks[0] = new ClusteredReadWriteLock(controllers[0]);
    for (int i = 1; i < locks.length; i++) {
      locks[i] =
          new ClusteredReadWriteLock(controllers[i], locks[0].getIdForExclusiveLockObject(), locks[0]
              .getIdForNumberOfNodesWithSharedLocksObject());
    }

    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);
    final CountDownLatch latch3 = new CountDownLatch(1);
    final CountDownLatch latch4 = new CountDownLatch(1);
    final CountDownLatch latchEnd = new CountDownLatch(1);
    final AtomicInteger cnt = new AtomicInteger(0);
    Thread t0 = new Thread(new Runnable() {

      public void run() {
        try {
          locks[0].readLock().lock();
          //warte auf anderen thread, dass er readlock hat. => zwei threads k�nnen readlock haben.
          latch.await();
          locks[0].readLock().unlock();
          long t0 = System.currentTimeMillis();
          locks[0].writeLock().lock();
          t0 = System.currentTimeMillis() - t0;
          assertTrue(t0 > 3000); // => writelock kann nicht gleichzeitig mit readlock geholt werden.
          latch2.countDown();
          Thread.sleep(4000);
          locks[0].writeLock().lock(); //reentrancy
          locks[0].writeLock().unlock();
          Thread.sleep(1000);
          assertEquals(0, cnt.get());
          locks[0].writeLock().unlock();
          latch3.await();
          assertEquals(1, cnt.get());
          locks[0].readLock().lock();
          latch4.countDown();

          t0 = System.currentTimeMillis();
          if (locks[0].writeLock().tryLock(2, TimeUnit.SECONDS)) {
            fail("got Lock");
          }
          long diff = System.currentTimeMillis() - t0;
          System.out.println(diff);
          assertTrue("locktimeout did not work", diff >= 2000);

        } catch (InterruptedException e) {
        }
      }

    });
    t0.setDaemon(true);
    t0.start();
    Thread t1 = new Thread(new Runnable() {

      public void run() {
        try {
          locks[1].readLock().lock();
          latch.countDown();
          locks[1].readLock().lock(); //reentrancy
          Thread.sleep(4000);
          locks[1].readLock().unlock();
          locks[1].readLock().unlock();
          //warte, dass andere thread writelock hat
          latch2.await();
          long t0 = System.currentTimeMillis();
          locks[1].writeLock().lock();
          t0 = System.currentTimeMillis() - t0;
          assertTrue(t0 > 3000); // => writelock kann nicht gleichzeitig mit writelock geholt werden.
          cnt.incrementAndGet();
          latch3.countDown();
          locks[1].readLock().lock(); //downgrade
          locks[1].writeLock().unlock();
          latch4.await(); //nur noch readlock �brig          
          Thread.sleep(4000);

          latchEnd.countDown();
        } catch (InterruptedException e) {
        }
      }

    });
    t1.setDaemon(true);
    t1.start();
    try {
      if (!latchEnd.await(20000, TimeUnit.MILLISECONDS)) {
        fail("deadlock?");
      }
    } catch (InterruptedException e) {
    }
  }


  public void testForDeadlocks() throws InterruptedException {
    final Random random = new Random();
    final int controllerCount = 4;
    final CacheController[] controllers = new CacheController[controllerCount];
    final CacheController ccID0 = CacheControllerFactory.newCacheController();
    ccID0.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    ccID0.setupNewCluster(); //neues cluster

    controllers[0] = ccID0;
    for (int i = 1; i < controllerCount; i++) {
      controllers[i] = CacheControllerFactory.newCacheController();
      controllers[i].addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
      controllers[i].connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
          .getJavaProvider((InterconnectCalleeJava) controllers[random.nextInt(i)].getCallees().get(0)));
    }

    final ReadWriteLock[] locks = new ReadWriteLock[controllerCount];
    /*//zum vergleich mit normalen reentrantreadwritelocks: 
     locks[0] = new ReentrantReadWriteLock();
     for (int i = 1; i<locks.length; i++) {
       locks[i] = locks[0];
     }*/
    locks[0] = new ClusteredReadWriteLock(controllers[0]);
    for (int i = 1; i < locks.length; i++) {
      ClusteredReadWriteLock l = (ClusteredReadWriteLock) locks[0];
      locks[i] =
          new ClusteredReadWriteLock(controllers[i], l.getIdForExclusiveLockObject(), l
              .getIdForNumberOfNodesWithSharedLocksObject());
    }

    int numberOfThreads = 5;
    final AtomicInteger cnt = new AtomicInteger(0);
    final CountDownLatch latch = new CountDownLatch(numberOfThreads);
    final StringBuffer sb = new StringBuffer();
    for (int i = 0; i < numberOfThreads; i++) {
      Thread t = new Thread(new Runnable() {

        public void run() {
          while (cnt.incrementAndGet() < 1000) {
            int node = random.nextInt(controllerCount);
            int wlCnt = random.nextInt(2);
            int readCnt = random.nextInt(5) + 1;
            int readCnt2 = random.nextInt(5) + 1;
            for (int i = 0; i < wlCnt; i++) {
              locks[node].writeLock().lock();
            }
            for (int i = 0; i < readCnt; i++) {
              locks[node].readLock().lock();
            }
            for (int i = 0; i < readCnt2; i++) {
              locks[node].readLock().lock();
            }
             sb.append(wlCnt+"a");
            try {
              Thread.sleep(1);
            } catch (InterruptedException e) {
            }
            sb.append(wlCnt + "b");
            for (int i = 0; i < readCnt2; i++) {
              locks[node].readLock().unlock();
            }
            for (int i = 0; i < wlCnt; i++) {
              locks[node].writeLock().unlock();
            }
            for (int i = 0; i < readCnt; i++) {
              locks[node].readLock().unlock();
            }
          }
          latch.countDown();
        }

      });
      t.setDaemon(true);
      t.start();
    }
    if (!latch.await(120000, TimeUnit.MILLISECONDS)) {
      fail("test took too long");
    }
    String[] parts = sb.toString().split("1a1b");
    for (int i = 0; i < parts.length; i++) {
      assertFalse(parts[i].contains("1"));

      assertEquals(parts[i], parts[i].split("0a", -1).length, parts[i].split("0b", -1).length);
    }
  }


}
