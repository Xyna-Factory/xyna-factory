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



import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;



public class ReentrantLock2Test extends TestCase {

  public void testLockUnlock() {
    ReentrantLock2 lock = new ReentrantLock2();
    assertFalse(lock.isLocked());
    assertEquals(0, lock.getHoldCount());
    assertFalse(lock.isHeldByCurrentThread());
    lock.lock();
    assertTrue(lock.isLocked());
    assertEquals(1, lock.getHoldCount());
    assertTrue(lock.isHeldByCurrentThread());
    lock.unlock();
    assertFalse(lock.isLocked());
    assertEquals(0, lock.getHoldCount());
    assertFalse(lock.isHeldByCurrentThread());
  }


  public void testReentrancy() {
    ReentrantLock2 lock = new ReentrantLock2();
    assertFalse(lock.isLocked());
    assertEquals(0, lock.getHoldCount());
    lock.lock();
    assertTrue(lock.isLocked());
    assertEquals(1, lock.getHoldCount());
    lock.lock();
    assertTrue(lock.isLocked());
    assertEquals(2, lock.getHoldCount());
    lock.unlock();
    assertTrue(lock.isLocked());
    assertEquals(1, lock.getHoldCount());
    lock.unlock();
    assertFalse(lock.isLocked());
    assertEquals(0, lock.getHoldCount());
  }


  public void testOtherThreadUnlock() throws InterruptedException {
    final ReentrantLock2 lock = new ReentrantLock2();
    assertFalse(lock.isLocked());
    assertEquals(0, lock.getHoldCount());
    lock.lock();
    assertTrue(lock.isLocked());
    assertEquals(1, lock.getHoldCount());

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean gotIllegalMonitorStateException = new AtomicBoolean(false);
    Thread t2 = new Thread(new Runnable() {

      public void run() {
        try {
          lock.unlock();
        } catch (IllegalMonitorStateException e) {
          gotIllegalMonitorStateException.set(true);
        } finally {
          latch.countDown();
        }
      }

    });
    t2.start();
    latch.await();
    assertTrue(gotIllegalMonitorStateException.get());
  }


  public void testOtherThreadWaitsForLock() throws InterruptedException {
    final ReentrantLock2 lock = new ReentrantLock2();
    assertFalse(lock.isLocked());
    assertEquals(0, lock.getHoldCount());
    lock.lock();
    assertTrue(lock.isHeldByCurrentThread());
    assertTrue(lock.isLocked());
    assertEquals(1, lock.getHoldCount());

    final CountDownLatch latch0 = new CountDownLatch(1);
    final CountDownLatch latch1 = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);
    final CountDownLatch latch3 = new CountDownLatch(1);
    assertFalse(lock.hasQueuedThreads());

    Thread t2 = new Thread(new Runnable() {

      public void run() {
        try {
          try {
            assertFalse(lock.isHeldByCurrentThread());
            assertTrue(lock.isLocked());
            assertEquals(0, lock.getHoldCount());
            latch0.countDown();
            lock.lock();
            assertTrue(lock.isLocked());
            assertEquals(1, lock.getHoldCount());
            assertTrue(lock.isHeldByCurrentThread());
          } catch (Throwable t) {
            t.printStackTrace();
            fail(t.getMessage());
          } finally {
            latch1.countDown();
            try {
              latch2.await();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
        } finally {
          try {
            lock.unlock();
          } finally {
            latch3.countDown();
          }
        }

      }

    });
    t2.start();
    latch0.await(); //anderer thread hat �berpr�ft, dass er lock nicht hat
    Thread.sleep(300);
    assertTrue(lock.hasQueuedThreads());
    lock.unlock();
    latch1.await(); //anderer thread hat lock geholt
    assertFalse(lock.isHeldByCurrentThread());
    assertTrue(lock.isLocked());
    latch2.countDown(); //anderer thread wei� bescheid, dass assertions ausgef�hrt wurden
    latch3.await(); //anderer thread hat lock freigegeben
    assertFalse(lock.isLocked());
  }

  public void testLockUnlockSeveralThreads() throws InterruptedException {
    final ReentrantLock2 lock = new ReentrantLock2();
    final Random random = new Random();
    final int threadNumber = 20;
    final int loops = 10;
    final AtomicInteger numberOfRemainingLoops = new AtomicInteger(threadNumber*loops);
    final CountDownLatch latch = new CountDownLatch(threadNumber);
    for (int i = 0; i<threadNumber; i++) {
      Thread t = new Thread(new Runnable() {

        private int holdCount;
        
        private void internal() {
          lock.lock();
          holdCount ++;
          try {
            try {
              Thread.sleep(random.nextInt(30) + 30);
            } catch (InterruptedException e) {
            }
            assertTrue(lock.isHeldByCurrentThread());
            assertTrue(lock.isLocked());
            if (numberOfRemainingLoops.get() > 0 && numberOfRemainingLoops.get() > threadNumber*(loops-1)) {
              assertTrue(lock.hasQueuedThreads()); //am ende des tests ist evtl keiner mehr am warten. am anfang ist evtl noch keiner am warten.
            }
            assertEquals(holdCount, lock.getHoldCount());
            if (random.nextBoolean()) {
              internal();
            }
          } finally {
            lock.unlock();
          }
        }
        
        public void run() {          
          try {
            for (int i = 0; i<loops; i++) {
              holdCount = 0;
              numberOfRemainingLoops.decrementAndGet();
              internal();
            }
          } finally {
            latch.countDown();
          }
          
        }
        
      });
      t.start();
    }
    latch.await();
    assertFalse(lock.isLocked());
    assertFalse(lock.isHeldByCurrentThread());
    assertFalse(lock.hasQueuedThreads());
  }


  public void testPerformance() throws InterruptedException {

    final ReentrantLock2 lock = new ReentrantLock2();
    final AtomicBoolean locked = new AtomicBoolean();
    final long executions = 1000000;
    final int numberOfThreads = 10;

    List<LockRunnable> lockRunnables = new ArrayList<ReentrantLock2Test.LockRunnable>();
    for (int i = 0; i < numberOfThreads; i++) {
      LockRunnable lockRunnable = new LockRunnable(lock, locked, executions);
      lockRunnables.add(lockRunnable);
      new Thread(lockRunnable).start();
    }

    long before = System.nanoTime();
    outer : while (true) {
      for (LockRunnable lr : lockRunnables) {
        if (lr.isFailed()) {
          fail("Thread failed");
        }
        if (!lr.isFinished()) {
          Thread.sleep(200);
          continue outer;
        }
      }
      break;
    }

    long duration = System.nanoTime() - before;
    double durationSeconds = duration / (1E9);
    long totalExecutions = (long) numberOfThreads * (long) executions;
    double rate = totalExecutions / durationSeconds;
    System.out.println("took " + durationSeconds + "s for " + totalExecutions + " executions => " + rate + "Hz");

  }


  private static class LockRunnable implements Runnable {

    private final ReentrantLock2 lock;
    private final AtomicBoolean locked;
    private final long executions;

    private boolean failed;
    private boolean finished;


    public LockRunnable(ReentrantLock2 lock, AtomicBoolean locked, long executions) {
      this.lock = lock;
      this.locked = locked;
      this.executions = executions;
    }


    public void run() {
      long cnt = 0;
      while (cnt < executions) {
        cnt++;
        lock.lock();
        try {
          boolean check1 = locked.compareAndSet(false, true);
          boolean check2 = locked.compareAndSet(true, false);
          if (!(check1 && check2)) {
            throw new RuntimeException("lock is not secure");
          }
        } catch (Throwable t) {
          failed = true;
          t.printStackTrace();
        } finally {
          lock.unlock();
        }
      }
      finished = true;
    }


    public boolean isFinished() {
      return finished;
    }


    public boolean isFailed() {
      return failed;
    }

  }
}
