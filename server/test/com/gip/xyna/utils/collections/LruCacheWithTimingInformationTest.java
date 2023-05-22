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

import junit.framework.TestCase;

public class LruCacheWithTimingInformationTest extends TestCase {

  public void testMaxSize() {
    int size = 10;
    LruCacheWithTimingInformation<String, String> c = new LruCacheWithTimingInformation<>(size);
    for (int i = 0; i<100; i++) {
      c.put("" + i, "" + i);
      assertEquals(Math.min(10,  i+1), c.size());
    }
  }
  
  public void testMapObjectLifecycle() {
    int size = 10;
    LruCacheWithTimingInformation<String, String> c = new LruCacheWithTimingInformation<>(size);
    c.put("a", "b");
    String val = c.get("a");
    assertEquals("b", val);
    c.remove("a");
    assertEquals(0, c.size());
    val = c.get("a");
    assertNull(val);
  }
  
  public void testLru() {
    int size = 10;
    LruCacheWithTimingInformation<String, String> c = new LruCacheWithTimingInformation<>(size);
    for (int i = 0; i<100; i++) {
      c.put("" + i, "" + i);
    }
    for (int i = 90; i<100; i++) {
      assertTrue(c.keySet().contains("" + i));
    }
  }
  
  public void testInsertionTime() throws InterruptedException {
    int size = 10;
    LruCacheWithTimingInformation<String, String> c = new LruCacheWithTimingInformation<>(size);
    for (int i = 0; i<100; i++) {
      c.put("" + i, "" + i);
    }
    long t = System.currentTimeMillis();
    Thread.sleep(100);
    c.put("asd", "asd");
    long diff = t - c.creationTimeOfLastEvictedKey();
    System.out.println(diff);
    assertTrue(Math.abs(diff) < 3);
  }
  
}
