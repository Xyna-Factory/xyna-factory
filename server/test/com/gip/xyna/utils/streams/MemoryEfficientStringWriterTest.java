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
package com.gip.xyna.utils.streams;

import java.io.IOException;
import java.util.Random;

import junit.framework.TestCase;


public class MemoryEfficientStringWriterTest extends TestCase {
  
  public void test1() throws IOException {
    MemoryEfficientStringWriter mesw = new MemoryEfficientStringWriter();
    mesw.append("Hello").append(" World!");
    assertEquals("Hello World!", mesw.getString());
  }
  
  public void test2() throws IOException  {
    MemoryEfficientStringWriter mesw = new MemoryEfficientStringWriter();
    mesw.append('a');
    mesw.append("test");
    mesw.append("b\u7121");
    mesw.append('c');
    mesw.append("test", 2, 3);
    mesw.write(new char[]{'x', 'x'});
    mesw.write('y');
    mesw.write("test");
    mesw.write(new char[]{'a', 'b', 'c', 'd'}, 1, 2);
    mesw.write("1234567", 2, 2);
    assertEquals("atestb\u7121csxxytestbc34", mesw.getString());
  }
  
  public void test3() throws IOException, InterruptedException { //beim manuellen testen wait hochstellen und dann bei den check-stellen mit jstatc speicherverbrauch checken
    int wait = 0;
    Thread.sleep(wait);
    boolean sw = true;
    MemoryEfficientStringWriter mesw = new MemoryEfficientStringWriter();
    StringBuilder sb = new StringBuilder();

    Random r = new Random();
    int len = 0;
    for (int i = 0; i < 1000; i++) {
      char[] c = new char[500 + r.nextInt(50)];
      len += c.length;
      for (int j = 0; j < c.length; j++) {
        c[j] = (char) ('a' + r.nextInt(30));
      }
      if (sw) {
        mesw.write(c);
      } else {
        sb.append(new String(c));
      }
    }
    System.out.println("check1 (" + len + ")");
    System.gc();
    Thread.sleep(wait);
    String s;
    if (sw) {
      long t = System.currentTimeMillis();
      s = mesw.getString();
      System.out.println((System.currentTimeMillis() - t) + "ms");
    } else {
      s = sb.toString();
      sb.setLength(0);
    }
    Thread.sleep(1000);

    System.out.println("check2");
    System.gc();
    Thread.sleep(1000);
    assertEquals(len, s.length());
    String t = mesw.getString();
    assertEquals("", t);
  }

}
