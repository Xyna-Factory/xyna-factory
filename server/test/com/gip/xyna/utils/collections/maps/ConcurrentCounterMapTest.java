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
package com.gip.xyna.utils.collections.maps;



import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;



public class ConcurrentCounterMapTest extends TestCase {

  public void test1() {
    ConcurrentCounterMap<String> m = new ConcurrentCounterMap<>();
    assertEquals(1, m.increment("a"));
    assertEquals(1, m.get("a"));
    assertEquals(0, m.get("b"));
    assertEquals(2, m.increment("a"));
    assertEquals(2, m.get("a"));
    assertEquals(1, m.decrement("a"));
    assertEquals(0, m.decrement("a"));
    assertEquals(0, m.get("a"));
    try {
      m.decrement("a");
      fail();
    } catch (RuntimeException e) {
      //ok
    }
  }


  public void testConcurrently() {
    final ConcurrentCounterMap<Integer> m = new ConcurrentCounterMap<>();
    int n = 6;
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(3, n, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    CountDownLatch cdl = new CountDownLatch(n);
    try {
      for (int i = 0; i < n; i++) {
        tpe.execute(new Runnable() {

          @Override
          public void run() {
            Random r = new Random();
            try {
              for (int j = 0; j < 100000; j++) {
                int c = r.nextInt(10);
                for (int k = 0; k < c; k++) {
                  m.increment(123);
                }
                for (int k = 0; k < c; k++) {
                  m.decrement(123);
                }
              }
            } catch (RuntimeException e) {
              m.increment(124);
            } finally {
              cdl.countDown();
            }
          }
        });
      }
      try {
        cdl.await(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        fail("");
      }
      assertEquals(0, m.get(123));
      assertEquals(0, m.get(124));
    } finally {
      tpe.shutdown();
    }
  }

}
