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



import java.util.Random;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import com.gip.xyna.ObjectStringRepresentation;



public class ExpiringMapTest extends TestCase {

  public void testExpire() throws InterruptedException {
    ExpiringMap<String, String> map = new ExpiringMap<String, String>(300, TimeUnit.MILLISECONDS);
    map.put("a", "b");
    Thread.sleep(200);
    assertEquals("b", map.get("a"));
    Thread.sleep(200);
    //erst anderer zugriff führt zum expire
    assertEquals("b", map.get("a"));
    assertNull(map.get("a"));
  }


  public void testRemoveBeforeTimeout() throws InterruptedException {
    ExpiringMap<String, String> map = new ExpiringMap<String, String>(300, TimeUnit.MILLISECONDS);
    map.put("a", "b");
    Thread.sleep(200);
    assertEquals("b", map.get("a"));
    assertEquals("b", map.remove("a"));
    assertNull(map.get("a"));
    Thread.sleep(200);
    assertNull(map.get("a"));
  }


  public void testRemoveAfterTimeout() throws InterruptedException {
    ExpiringMap<String, String> map = new ExpiringMap<String, String>(300, TimeUnit.MILLISECONDS);
    map.put("a", "b");
    Thread.sleep(200);
    assertEquals("b", map.get("a"));
    Thread.sleep(200);
    assertEquals("b", map.remove("a"));
    assertNull(map.get("a"));
  }


  public void testSameTime() throws InterruptedException {
    ExpiringMap<Integer, Integer> map = new ExpiringMap<Integer, Integer>(300, TimeUnit.MILLISECONDS);
    for (int i = 0; i < 100; i++) {
      map.put(i, i);
    }
    Thread.sleep(200);
    assertEquals(42, (int) map.get(42));
    assertEquals(88, (int) map.remove(88));
    Thread.sleep(200);
    assertEquals(56, (int) map.get(56));
    for (int i = 0; i < 100; i++) {
      assertNull(map.get(i));
    }
  }


  public void testTiming() throws InterruptedException {
    ExpiringMap<Integer, Integer> map = new ExpiringMap<Integer, Integer>(300, TimeUnit.MILLISECONDS);
    map.put(3, 5);
    long t = System.currentTimeMillis();
    Thread.sleep(270);
    Integer v = 1;
    while (v != null) {
      v = map.get(3);
    }
    long diff = Math.abs(System.currentTimeMillis() - t) - 300;
    assertTrue(diff >= 0);
    assertTrue(diff < 20);
  }
  
  
  public void testReadUpdatesTime() throws Exception {
    ExpiringMap<Integer, Integer> map = new ExpiringMap<Integer, Integer>(300, TimeUnit.MILLISECONDS, true);
    map.put(3, 5);
    Thread.sleep(270);
    assertEquals(5, (int) map.get(3));
    Thread.sleep(100);
    assertEquals(5, (int) map.get(3));
    Thread.sleep(370);
    assertEquals(5, (int) map.get(3));
    Thread.sleep(370);
    //gets aktualisieren cache - deshalb auf anderes element zugreifen
    map.put(4, 6);
    assertNull(map.get(3));
  }
  
  public void testMemoryConsumption() throws Exception {
    ExpiringMap<Integer, Integer> map = new ExpiringMap<Integer, Integer>(5000, TimeUnit.MILLISECONDS, true);
    Random r = new Random();
    for (int i = 0; i<10; i++) {
      map.put(i, 1444);
    }

    StringBuilder sb = new StringBuilder();
    ObjectStringRepresentation.createStringRepOfObject(sb, map);
    int oldLen = sb.length();
    
    for (int i = 0; i<100000; i++) {
      map.get(r.nextInt(10));
    }
    
    sb = new StringBuilder();
    ObjectStringRepresentation.createStringRepOfObject(sb, map);
    System.out.println("old=" + oldLen + ", new=" + sb.length());
    assertTrue(sb.length() < oldLen * 1.1);
    try {
      assertEquals(1444, (int)map.get(3));
    } finally {
      map.remove(3);
    }
  }
  
}
