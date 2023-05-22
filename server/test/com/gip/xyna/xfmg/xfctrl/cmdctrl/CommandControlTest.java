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
package com.gip.xyna.xfmg.xfctrl.cmdctrl;



import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;



public class CommandControlTest extends TestCase {

  public void test1() throws InterruptedException, XynaException {
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(10, 10, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    final Vector<Exception> ex = new Vector<Exception>();
    int cnt = 10000;
    final CountDownLatch l = new CountDownLatch(cnt);
    Runnable r = new Runnable() {

      public void run() {
        try {
          CommandControl.tryLock(Operation.APPLICATION_ADDOBJECT);
          CommandControl.unlock(Operation.APPLICATION_ADDOBJECT);
          Pair<Operation, Operation> wlock = CommandControl.wlock(Operation.APPLICATION_ADDOBJECT, Operation.all(), -1);
          if (wlock != null) {
            return;
          }
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
          }
          CommandControl.wunlock(Operation.all(), -1);
        } catch (IllegalMonitorStateException e) {
          e.printStackTrace();
          ex.add(e);
        } catch (RuntimeException e) {

        } finally {
          l.countDown();
        }
      }

    };
    for (int i = 0; i < cnt; i++) {
      try {
        tpe.execute(r);
      } catch (RejectedExecutionException e) {
        i--;
      }
    }

    l.await();
    assertEquals(0, ex.size());
  }
  
  public void test2() {
    Pair<Operation, Operation> result = CommandControl.wlock(Operation.APPLICATION_ADDOBJECT, new Operation[]{Operation.APPLICATION_BUILD}, -1);
    assertTrue(result == null);
    final CountDownLatch l = new CountDownLatch(1);
    Thread t  = new Thread(new Runnable() {

      public void run() {
        try {
          Pair<Operation, Operation> result = CommandControl.wlock(Operation.APPLICATION_CLEAR_WORKINGSET, new Operation[]{Operation.APPLICATION_BUILD}, -1);
          assertTrue(result != null);
        } finally {
          l.countDown();
        }
      }
      
    });
    t.start();
    try {
      assertTrue(l.await(2, TimeUnit.SECONDS));
    } catch (InterruptedException e) {
    }
    CommandControl.wunlock(new Operation[]{Operation.APPLICATION_BUILD}, -1);
  }

}
