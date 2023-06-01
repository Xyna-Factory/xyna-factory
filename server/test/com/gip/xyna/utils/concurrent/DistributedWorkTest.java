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




import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

import org.junit.Test;



public class DistributedWorkTest extends TestCase {

  @Test
  public void test1() throws InterruptedException {
    int workSize = 2000;
    final DistributedWork work = new DistributedWork(workSize);
    final Random r = new Random();
    int tcount = 10;
    final CountDownLatch l = new CountDownLatch(tcount);
    final SortedSet<Integer> worked = new TreeSet<Integer>();

    for (int i = 0; i < tcount; i++) {
      Thread t = new Thread(new Runnable() {

        private void innerRun() {
          long tid = Thread.currentThread().getId();
          int nextWorkIdx;
          while (-1 != (nextWorkIdx = work.getAndLockNextOpenTaskIdx())) {
            //bit wurde gesetzt, weil ansosnten continue, d.h. jetzt kann dieser thread in ruhe das addTable durchführen
            try {
              System.out.println(tid + "- " + System.currentTimeMillis() + " working " + nextWorkIdx);
              synchronized (worked) {
                worked.add(nextWorkIdx);
              }
              /*  try {
                  Thread.sleep(r.nextInt(1000));
                } catch (InterruptedException e) {
                }*/
              if (r.nextDouble() < 0.1) {
                System.out.println(tid + "- " + System.currentTimeMillis() + " recursion");
                innerRun();
              }
            } finally {
              work.taskDone(); //auch bei einem fehler sollen die threads nicht warten müssen.
            }
          }
          try {
            work.waitForCompletion();
          } catch (InterruptedException e) {
          }
          System.out.println(tid + "- " + System.currentTimeMillis() + " finished");
        }


        public void run() {
          innerRun();
          l.countDown();
        }

      });
      t.start();
    }

    l.await();
    assertEquals(2000, worked.size());
    assertEquals(Integer.valueOf(1999), worked.last());
    assertEquals(Integer.valueOf(0), worked.first());
  }

}
