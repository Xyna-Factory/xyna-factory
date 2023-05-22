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
package com.gip.xyna.utils.collections;



import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;



public class MtoNMappingTest extends TestCase {

  public void test1() {
    MtoNMapping<String, Integer> map = new MtoNMapping<String, Integer>();
    map.add("test", 54);
    assertEquals(new HashSet<String>(Arrays.asList("test")), map.getAllKeys());
    assertFalse(new HashSet<String>(Arrays.asList("test2")).equals(map.getAllKeys()));
    assertEquals(new HashSet<Integer>(Arrays.asList(54)), map.getAllValues());
    assertFalse(new HashSet<Integer>(Arrays.asList(55)).equals(map.getAllValues()));

    map.add("test", 56);
    assertEquals(new HashSet<String>(Arrays.asList("test")), map.getKeys(54));
    assertNull(map.getKeys(55));
    assertEquals(new HashSet<String>(Arrays.asList("test")), map.getKeys(56));

    assertEquals(new HashSet<Integer>(Arrays.asList(54, 56)), map.getValues("test"));
    assertNull(map.getValues("test2"));
  }


  public void testConcurrency() throws InterruptedException {
    ThreadPoolExecutor tp = new ThreadPoolExecutor(10, 10, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    try {
      for (int t = 0; t < 100; t++) {
        System.out.println(t);
        final MtoNMapping<String, Integer> map = new MtoNMapping<String, Integer>();
        final CountDownLatch latch = new CountDownLatch(10);
        final Random random = new Random();
        for (int i = 0; i < 10; i++) {
          tp.execute(new Runnable() {

            public void run() {
              try {
                for (int i = 0; i < 100; i++) {
                  int j = random.nextInt(3);
                  if (j == 0) {
                    //delete
                    if (random.nextBoolean()) {
                      //key
                      Set<String> allKeys = map.getAllKeys();
                      Iterator<String> iterator = allKeys.iterator();
                      if (iterator.hasNext()) {
                        String s = iterator.next();
                        map.removeKey(s);
                      }
                    } else {
                      //val
                      Set<Integer> allVals = map.getAllValues();
                      Iterator<Integer> iterator = allVals.iterator();
                      if (iterator.hasNext()) {
                        int v = iterator.next();
                        map.removeValue(v);
                      }
                    }
                  } else if (j == 1) {
                    //add
                    String k = "" + random.nextInt(10);
                    Integer v = random.nextInt(10);
                    map.add(k, v);
                  } else {
                    Set<String> allKeys = map.getAllKeys();
                    Iterator<String> iterator = allKeys.iterator();
                    if (iterator.hasNext()) {
                      String s = iterator.next();
                      Set<Integer> vals = map.getValues(s);
                      if (vals != null) {
                        Iterator<Integer> it2 = vals.iterator();
                        if (it2.hasNext()) {
                          map.removeMapping(s, it2.next());
                        }
                      }
                    }
                  }
                }
              } finally {
                latch.countDown();
              }
            }

          });
        }
        latch.await();
        try {
          for (String s : map.getAllKeys()) {
            Set<Integer> values = map.getValues(s);
            //checken, dass andersrum auch mapping besteht
            //npe nicht zu �berpr�fen, weil values darf nicht null sein!
            for (Integer v : values) {
              assertTrue(map.getKeys(v).contains(s));
            }
          }
          for (Integer i : map.getAllValues()) {
            Set<String> keys = map.getKeys(i);
            //checken, dass andersrum auch mapping besteht
            //npe nicht zu �berpr�fen, weil values darf nicht null sein!
            for (String k : keys) {
              assertTrue(map.getValues(k).contains(i));
            }
          }
        } finally {
         // System.out.println(map);
        }
      }
    } finally {
      tp.shutdown();
    }
  }

}
