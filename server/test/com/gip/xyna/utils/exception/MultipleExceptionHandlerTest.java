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
package com.gip.xyna.utils.exception;



import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.gip.xyna.utils.exception.MultipleExceptionHandler.ExceptionCauseOrdering;

import junit.framework.TestCase;



public class MultipleExceptionHandlerTest extends TestCase {

  public static class MyException1 extends Exception {

    public MyException1() {
      super("message1");
    }

  }
  public static class MyException2 extends Exception {

    public MyException2() {
    }

  }


  public void testEmpty1() {
    MultipleExceptionHandler<MyException1> h = new MultipleExceptionHandler<>();
    try {
      h.rethrow();
    } catch (Throwable t) {
      fail();
    }
  }


  public void testEmpty2() {
    MultipleExceptionHandler<MyException1> h = new MultipleExceptionHandler<>();
    MultipleExceptionHandler<MyException2> h2 = new MultipleExceptionHandler<>();
    try {
      h.rethrow(h2);
    } catch (Throwable t) {
      fail();
    }
  }


  public void testEmpty3() {
    MultipleExceptionHandler<MyException1> h = new MultipleExceptionHandler<>();
    MultipleExceptionHandler<MyException2> h2 = new MultipleExceptionHandler<>();
    MultipleExceptionHandler<RuntimeException> h3 = new MultipleExceptionHandler<>();
    try {
      h.rethrow(h2, h3);
    } catch (Throwable t) {
      fail();
    }
  }


  public void testSingle1() {
    MultipleExceptionHandler<MyException1> h = new MultipleExceptionHandler<>();
    h.addException(new MyException1());
    try {
      h.rethrow();
      fail();
    } catch (Throwable t) {
      assertTrue(t instanceof MyException1);
    }
  }


  public void testSingle3() {
    MultipleExceptionHandler<MyException1> h = new MultipleExceptionHandler<>();
    MultipleExceptionHandler<MyException2> h2 = new MultipleExceptionHandler<>();
    MultipleExceptionHandler<RuntimeException> h3 = new MultipleExceptionHandler<>();
    h.addException(new MyException1());
    try {
      h.rethrow(h2, h3);
      fail();
    } catch (Throwable t) {
      assertTrue(t instanceof MyException1);
    }
    try {
      h2.rethrow(h, h3);
      fail();
    } catch (Throwable t) {
      assertTrue(t instanceof MyException1);
    }
    try {
      h3.rethrow(h, h2);
      fail();
    } catch (Throwable t) {
      assertTrue(t instanceof MyException1);
    }
  }


  public void testDouble1() {
    MultipleExceptionHandler<MyException1> h = new MultipleExceptionHandler<>();
    MyException1 e1 = new MyException1();
    MyException1 e2 = new MyException1();
    h.addException(e1);
    h.addException(e2);
    try {
      h.rethrow();
      fail();
    } catch (Throwable t) {
      assertTrue(t instanceof MultipleExceptions);
      MultipleExceptions m = (MultipleExceptions) t;
      assertEquals(2, m.getCauses().size());
      assertTrue(m.getCauses().contains(e1));
      assertTrue(m.getCauses().contains(e2));
    }
  }


  public void testDouble3SameType() {
    MultipleExceptionHandler<MyException1> h = new MultipleExceptionHandler<>();
    MultipleExceptionHandler<MyException2> h2 = new MultipleExceptionHandler<>(ExceptionCauseOrdering.TIME);
    MultipleExceptionHandler<RuntimeException> h3 = new MultipleExceptionHandler<>();
    MyException1 e1 = new MyException1();
    MyException1 e2 = new MyException1();
    MyException1 e3 = new MyException1();
    h.addException(e1);
    h.addException(e2);
    h.addException(e3);
    try {
      h2.rethrow(h, h3);
      fail();
    } catch (Throwable t) {
      assertTrue(t instanceof MultipleExceptions);
      MultipleExceptions m = (MultipleExceptions) t;
      assertEquals(3, m.getCauses().size());
      assertEquals(e1, m.getCauses().get(0));
      assertEquals(e2, m.getCauses().get(1));
      assertEquals(e3, m.getCauses().get(2));
    }
  }


  public void testDouble3DifferentTypeOrderedByTime() {
    MultipleExceptionHandler<MyException1> h = new MultipleExceptionHandler<>();
    MultipleExceptionHandler<MyException2> h2 = new MultipleExceptionHandler<>(ExceptionCauseOrdering.TIME);
    MultipleExceptionHandler<RuntimeException> h3 = new MultipleExceptionHandler<>();
    Random r = new Random();
    List<Throwable> t = new ArrayList<>();
    int n = 100;
    for (int i = 0; i < n; i++) {
      switch (r.nextInt(3)) {
        case 0 :
          MyException1 m = new MyException1();
          h.addException(m);
          t.add(m);
          break;
        case 1 :
          MyException2 m2 = new MyException2();
          h2.addException(m2);
          t.add(m2);
          break;
        case 2 :
          RuntimeException m3 = new RuntimeException();
          h3.addException(m3);
          t.add(m3);
          break;
      }
    }
    try {
      h2.rethrow(h, h3);
      fail();
    } catch (Throwable th) {
      assertTrue(th instanceof MultipleExceptions);
      MultipleExceptions m = (MultipleExceptions) th;
      assertEquals(n, m.getCauses().size());
      for (int i = 0; i < n; i++) {
        assertEquals(t.get(i), m.getCauses().get(i));
      }
    }
  }


  public void testDouble3DifferentTypeOrderedByType() {
    MultipleExceptionHandler<MyException1> h = new MultipleExceptionHandler<>();
    MultipleExceptionHandler<MyException2> h2 = new MultipleExceptionHandler<>(ExceptionCauseOrdering.TYPE);
    MultipleExceptionHandler<RuntimeException> h3 = new MultipleExceptionHandler<>();
    Random r = new Random();
    List<Throwable> t = new ArrayList<>();
    int n = 100;
    for (int i = 0; i < n; i++) {
      switch (r.nextInt(3)) {
        case 0 :
          MyException1 m = new MyException1();
          h.addException(m);
          t.add(m);
          break;
        case 1 :
          MyException2 m2 = new MyException2();
          h2.addException(m2);
          t.add(m2);
          break;
        case 2 :
          RuntimeException m3 = new RuntimeException();
          h3.addException(m3);
          t.add(m3);
          break;
      }
    }
    try {
      h2.rethrow(h, h3);
      fail();
    } catch (Throwable th) {
      assertTrue(th instanceof MultipleExceptions);
      MultipleExceptions m = (MultipleExceptions) th;
      assertEquals(n, m.getCauses().size());
      for (int i = 0; i < n; i++) {
        //h2 -> h -> h3
        Throwable c = m.getCauses().get(i);
        if (h2.count() > i) {
          assertTrue(c instanceof MyException2);
        } else if (h2.count() + h.count() > i) {
          assertTrue(c instanceof MyException1);
        } else {
          assertTrue(c instanceof RuntimeException);
        }
      }
    }
  }
}
