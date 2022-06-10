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

package com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationManagement.CorrelationIdLock;

import junit.framework.TestCase;


public class SynchronizationManagementTest extends TestCase {

  public void testlocks() throws InterruptedException {

    final ReadWriteLock globalLock = new ReentrantReadWriteLock();
    ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<String, ReentrantLock>();

    final AtomicBoolean failed = new AtomicBoolean();
    final AtomicBoolean exclusive = new AtomicBoolean();
    final AtomicLong executions = new AtomicLong();
    final long maxExecutions = 10000000;

    final CorrelationIdLock lock = new CorrelationIdLock(globalLock, locks, "blabla");

    Thread threads[] = new Thread[5];
    for (int i = 0; i < threads.length; i++) {
      Runnable r = new Runnable() {

        public void run() {
          while (executions.getAndIncrement() < maxExecutions && !failed.get()) {
            lock.lock();
            try {
              if (!exclusive.compareAndSet(false, true)) {
                failed.set(true);
                return;
              }
//              try {
//                Thread.sleep(1);
//              } catch (InterruptedException e) {
//                failed.set(true);
//                return;
//              }
              int x = 0;
              for (int i=0; i<1000;i++) {
                x++;
              }
              exclusive.set(false);
            } catch (Throwable t) {
              t.printStackTrace();
              failed.set(true);
            } finally {
              lock.unlock();
            }
          }
        }
      };
      threads[i] = new Thread(r);
      threads[i].start();
    }

    while (executions.get() < maxExecutions) {
      if (failed.get()) {
        fail("Lock is not exclusive.");
      }
      Thread.sleep(100);
    }

  }

}
