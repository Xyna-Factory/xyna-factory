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



import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import junit.framework.TestCase;

import com.gip.xyna.utils.concurrent.QueueProcessor.ElementProcessor;



public class QueueProcessorTest extends TestCase {


  public void test1() throws InterruptedException {
    int threadCnt = 1;
    final CountDownLatch l = new CountDownLatch(threadCnt);
    for (int i = 0; i < threadCnt; i++) {
      final int c = i;
      new Thread(new Runnable() {

        public void run() {
          try {
            test(c);
          } catch (InterruptedException e) {
          } finally {
            l.countDown();
          }
        }

      }).start();
    }
    l.await();
    System.out.println("all threads finished");
  }


  public void test(int count) throws InterruptedException {

    final Random r = new Random();
    for (int k = 0; k < 5; k++) {
      int threadCnt = 5;
      final CountDownLatch l = new CountDownLatch(threadCnt + 1);
      final AtomicLong sum = new AtomicLong();
      final QueueProcessor<Integer> q = new QueueProcessor<Integer>(20000, new ElementProcessor<Integer>() {

        public void process(Integer o) {
          sum.addAndGet(o);
        }

      });
      new Thread(new Runnable() {

        public void run() {
          q.run();
          l.countDown();
        }

      }, "queuerunner").start();
      long t = System.currentTimeMillis();
      final long n = 5000000;
      for (int i = 0; i < threadCnt; i++) {
        new Thread(new Runnable() {

          public void run() {
            for (int i = 0; i < n; i++) {
              /*while (!q.offer(i)) {

              }*/
                q.put(i);
            }
            l.countDown();
          }

        }, "client " + i).start();
      }
      while (sum.get() != (threadCnt * n * (n - 1) / 2)) {
        LockSupport.parkNanos(1000);
      }
      q.stop();
      l.await();
      System.out.println("[" + count + "] finished " + (System.currentTimeMillis() - t) + "ms.");
      System.out.println(q.cntL1.get() + " / " + q.cntL2.get() + " / " + q.cntL3.get() + " / " + q.cntA.get() + " / " + q.cntB.get());
    }
  }
  
  
  public void testShutdownWhenFull() throws InterruptedException {

    final int threadCnt = 100;
    final CountDownLatch startProcessing = new CountDownLatch(1);
    final CountDownLatch finishProcessing = new CountDownLatch(1);
    final AtomicLong sum = new AtomicLong();
    final QueueProcessor<TestElement> q = new QueueProcessor<TestElement>(10, new ElementProcessor<TestElement>() {

      public void process(TestElement o) {
        try {
          startProcessing.await();
        } catch (InterruptedException e) {
          fail(e.getMessage());
        }
        o.process(sum);
      }

    });
    Thread queuerunner = 
    new Thread(new Runnable() {

      public void run() {
        q.run();
      }

    }, "queuerunner");
    queuerunner.start();
    Thread[] clients = new Thread[threadCnt];
    for (int i = 0; i < threadCnt; i++) {
      clients[i] = new Thread(new Runnable() {

        public void run() {
          q.put(new TestElement());
        }

      }, "client " + i);
      clients[i].start();
    }
    Thread.sleep(1000);
    startProcessing.countDown();
    Thread.sleep(1000);
    q.stop();
    finishProcessing.countDown();
    queuerunner.join(30000);
    Thread.sleep(1000);
    assertEquals("querunner should have finished by now", false, queuerunner.isAlive());
    assertEquals("All events should have been processed", threadCnt, sum.get());
    int alive = 0;
    int dead = 0;
    for (Thread client : clients) {
      if (client.isAlive()) {
        alive++;
      } else {
        dead++;
      }
    }
    assertEquals("All clients should be dead: " + alive + "/" + dead, threadCnt, dead);
    
  }
  
  
  private class TestElement {
    
    TestElement() {
    }
    
    public void process(AtomicLong process) {
      process.incrementAndGet();
    }
    
  }


}
