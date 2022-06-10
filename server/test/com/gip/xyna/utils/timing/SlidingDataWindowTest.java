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
package com.gip.xyna.utils.timing;



import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;



public class SlidingDataWindowTest extends TestCase {

  public void test1() {
    int n = 7;
    int width = 10;
    SlidingDataWindow w = new SlidingDataWindow(n, width);
    assertEquals(0, (int) w.size());
    w.increment(52131);
    assertEquals(1, (int) w.size());
    w.increment(52131 + width - 1);
    assertEquals(2, (int) w.size());
    assertEquals(2, (int) w.get(52131));
    w.put(52131 + width * 6 - 5, 10); //5. schublade
    assertEquals(12, (int) w.size());
    assertTrue(w.put(52131 - 1, 1)); //-1. schublade
    assertEquals(13, (int) w.size());
    assertFalse(w.put(52131 - 2 * width - 1, 1)); //-2. schublade
    assertEquals(13, (int) w.size());
  }


  public void testSlide() {
    SlidingDataWindow w = new SlidingDataWindow(2, 5);
    w.increment(10);
    w.increment(15);
    assertEquals(2, (int) w.size());

    w = new SlidingDataWindow(2, 5);
    w.increment(10);
    w.increment(20);
    assertEquals(1, (int) w.size());
  }


  public void atest3() {
    final SlidingDataWindow w = new SlidingDataWindow(30, 20);
    final AtomicBoolean b = new AtomicBoolean(true);
    final CountDownLatch l = new CountDownLatch(1);
    Thread t = new Thread(new Runnable() {

      public void run() {
        for (int i = 0; i < 200; i++) {
          System.out.println(w.size(System.currentTimeMillis()) + ", " + w.get(System.currentTimeMillis() - 10 * 10));
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
          }
        }
        b.set(false);
        System.out.println("stopped");
        for (int i = 0; i < 200; i++) {
          System.out.println(w.size(System.currentTimeMillis()) + ", " + w.get(System.currentTimeMillis() - 10 * 10));
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
          }
        }
        l.countDown();
      }

    });
    t.start();
    while (b.get()) {
      w.increment(System.currentTimeMillis());
    }
    try {
      l.await();
    } catch (InterruptedException e) {
    }
  }


  public void testRange() {
    SlidingDataWindow w = new SlidingDataWindow(20, 5);
    w.addInRange(17, 80, 64);
    assertEquals(64, (int) w.size());

    int width = 5;
    double val = 65.5;
    int len = 64;
    w = new SlidingDataWindow(13, width);
    testSize(w);
    w.addInRange(17, 17+len-1, val);
    testSize(w);
    w.addInRange(20, 20+len-1, val);
    testSize(w);
    w.addInRange(23, 23+len-1, val);
    testSize(w);
    w.addInRange(26, 26+len-1, val);
    testSize(w);

    for (int i = 35; i < 75; i += width) {
      assertEquals(4 * val / len * width, w.get(i));
    }
  }


  private void testSize(SlidingDataWindow w) {
    double size = 0;
    for (long i = w.interval()[0]; i < w.interval()[1]; i += 5) {
      double v =  w.get(i);
      if (v != Double.MIN_VALUE) {
        size += v;
      }
    }
    assertEquals(w.size(), size);
  }

}
