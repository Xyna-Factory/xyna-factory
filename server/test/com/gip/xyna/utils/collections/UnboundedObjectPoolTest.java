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
package com.gip.xyna.utils.collections;



import java.io.Closeable;
import java.io.IOException;

import junit.framework.TestCase;



public class UnboundedObjectPoolTest extends TestCase {

  private static class Obj implements Closeable {

    private boolean closed = false;
    private int id;


    public Obj(int id) {
      this.id = id;
    }


    @Override
    public void close() throws IOException {
      System.out.println("closing " + this);
      if (closed) {
        throw new RuntimeException();
      }
      closed = true;
    }


    public String toString() {
      return "" + id;
    }

  }


  public void testMinObjects() throws InterruptedException {
    UnboundedObjectPool<Obj> pool = new UnboundedObjectPool<>(300, 100, 3);
    for (int i = 0; i < 3; i++) {
      Obj obj = new Obj(i);
      assertTrue(pool.addIfNeeded(obj));
    }
    assertTrue(pool.addIfNeeded(new Obj(3)));
    Thread.sleep(110);
    assertFalse(pool.addIfNeeded(new Obj(4)));
  }


  public void testAutoClose() throws InterruptedException {
    UnboundedObjectPool<Obj> pool = new UnboundedObjectPool<>(100, 100, 0);
    Obj obj = new Obj(1);
    assertTrue(pool.addIfNeeded(obj));
    Thread.sleep(250);
    assertEquals(0, pool.size());
    assertTrue(obj.closed);
    assertNull(pool.get());
  }


  public void testSize() throws InterruptedException {
    UnboundedObjectPool<Obj> pool = new UnboundedObjectPool<>(100, 100, 0);
    assertTrue(pool.addIfNeeded(new Obj(1)));
    assertEquals(1, pool.size());
    assertTrue(pool.addIfNeeded(new Obj(2)));
    assertEquals(2, pool.size());
    assertTrue(pool.addIfNeeded(new Obj(3)));
    assertEquals(3, pool.size());
    Thread.sleep(250);
    assertTrue(pool.addIfNeeded(new Obj(4)));
    assertEquals(1, pool.size());
  }


  public void testGet() throws InterruptedException {
    UnboundedObjectPool<Obj> pool = new UnboundedObjectPool<>(100, 100, 0);
    assertNull(pool.get());
    Obj obj = new Obj(1);
    assertTrue(pool.addIfNeeded(obj));
    assertEquals(obj, pool.get());
    assertEquals(0, pool.size());
    Obj obj2 = new Obj(2);
    assertTrue(pool.addIfNeeded(obj2));
    Obj obj3 = new Obj(3);
    assertTrue(pool.addIfNeeded(obj3));
    assertEquals(obj2, pool.get());
    assertEquals(obj3, pool.get());
  }


  public void testErrorWhenSameObjectIsAddedTwice() {
    UnboundedObjectPool<Obj> pool = new UnboundedObjectPool<>(100, 100, 0);
    Obj obj = new Obj(1);
    assertTrue(pool.addIfNeeded(obj));
    try {
      pool.addIfNeeded(obj);
      fail();
    } catch (IllegalArgumentException e) {
      //ok
    }
  }


  public void testIsClosed() {
    UnboundedObjectPool<Obj> pool = new UnboundedObjectPool<>(100, 100, 0);
    assertFalse(pool.isClosed());
    pool.close();
    assertTrue(pool.isClosed());
  }


  public void testCanNotInteractWithClosed() {
    UnboundedObjectPool<Obj> pool = new UnboundedObjectPool<>(100, 100, 0);
    pool.close();
    try {
      pool.get();
      fail();
    } catch (RuntimeException e) {

    }
    try {
      pool.addIfNeeded(new Obj(1));
      fail();
    } catch (RuntimeException e) {

    }
    try {
      pool.size();
      fail();
    } catch (RuntimeException e) {

    }
  }


  public void testObjectsAreClosed() throws InterruptedException {
    UnboundedObjectPool<Obj> pool = new UnboundedObjectPool<>(100, 100, 0);
    Obj obj1 = new Obj(1);
    Obj obj2 = new Obj(2);
    pool.addIfNeeded(obj1);
    pool.addIfNeeded(obj2);
    pool.close();
    assertTrue(obj1.closed);
    assertTrue(obj2.closed);
    Thread.sleep(300);
  }


  public void testTimeoutIsTimely() throws InterruptedException {
    UnboundedObjectPool<Obj> pool = new UnboundedObjectPool<>(1000, 1000, 0);
    for (int i = 0; i < 10; i++) {
      Obj obj1 = new Obj(1);
      pool.addIfNeeded(obj1);
      if (i < 9) {
        Thread.sleep(100);
      }
    }
    Thread.sleep(10);
    for (int i = 10; i >= 0; i--) {
      assertEquals(i, pool.size());
      Thread.sleep(100);
    }
  }

}
