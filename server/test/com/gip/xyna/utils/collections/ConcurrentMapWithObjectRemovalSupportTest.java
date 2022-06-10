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
package com.gip.xyna.utils.collections;



import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;



public class ConcurrentMapWithObjectRemovalSupportTest extends TestCase {

  private static class TestVal extends ObjectWithRemovalSupport {

    private final Vector<String> l = new Vector<String>();


    @Override
    protected boolean shouldBeDeleted() {
      return l.isEmpty();
    }

  }

  private static class TestMap<K> extends ConcurrentMapWithObjectRemovalSupport<K, TestVal> {

    private static final long serialVersionUID = -5879346063975910063L;

    private Vector<TestVal> allValues = new Vector<TestVal>();


    @Override
    public TestVal createValue(K key) {
      TestVal v = new TestVal();
      allValues.add(v);
      return v;
    }

  }


  public void test1() {
    TestMap<Long> map = new TestMap<Long>();
    long key = 3;
    TestVal v = map.lazyCreateGet(key);
    v.l.add("a");
    map.cleanup(key);
    assertTrue(map.containsKey(key));
    v = map.lazyCreateGet(key);
    v.l.clear();
    map.cleanup(key);
    assertEquals(0, map.size());
  }


  public void test2() throws InterruptedException {
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(10, 10, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    for (int run = 0; run < 100; run++) {
      final TestMap<Long> map = new TestMap<Long>();
      final Random r = new Random();
      final CountDownLatch latch = new CountDownLatch(10);
      for (int i = 0; i < 10; i++) {
        tpe.execute(new Runnable() {

          public void run() {
            try {
              for (int i = 0; i < 100; i++) {
                Long key = (long) r.nextInt(10);
                TestVal v = map.lazyCreateGet(key);
                try {
                  if (r.nextBoolean()) {
                    v.l.add("bla");
                  } else {
                    try {
                      v.l.remove(0);
                    } catch (ArrayIndexOutOfBoundsException e) {
                      //ignore
                    }
                  }
                } finally {
                  map.cleanup(key);
                }
              }
            } finally {
              latch.countDown();
            }
          }
        });
      }
      latch.await();
      for (Entry<Long, TestVal> e : map.entrySet()) {
        assertTrue(e.getValue().l.size() > 0);
      }
      System.out.println(map.allValues.size());
      for (TestVal v : map.allValues) {
        if (v.l.size() > 0) {
          assertTrue(map.values().contains(v));
        }
      }
    }
  }

}
