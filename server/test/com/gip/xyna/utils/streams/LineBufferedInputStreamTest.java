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
package com.gip.xyna.utils.streams;



import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Semaphore;

import junit.framework.TestCase;

import com.gip.xyna.utils.streams.LineBufferedInputStream.LineMarker;



/**
 *
 */
public class LineBufferedInputStreamTest extends TestCase {


  public void test1() throws IOException {
    byte[] bytes = "Dies ist\nein langer\nString mit\nZeilenumbrüchen.".getBytes();

    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

    BufferedReader reader1 = new BufferedReader(new InputStreamReader(bais));

    assertEquals("Dies ist#ein langer", reader1.readLine() + "#" + reader1.readLine());

    bais.reset();

    BufferedReader reader2 = new BufferedReader(new InputStreamReader(bais));

    assertEquals("Dies ist#ein langer", reader2.readLine() + "#" + reader2.readLine());

    bais.reset();

    LineBufferedInputStream lbis = new LineBufferedInputStream(bais, LineMarker.LF);

    assertEquals("Dies ist#ein langer", lbis.readLine() + "#" + lbis.readLine());

    assertEquals("String mit", lbis.readLine());
    assertEquals("Zeilenumbrüchen.", lbis.readLine());
    assertNull(lbis.readLine());

  }


  public void test2() throws IOException {
    byte[] bytes = "123a456aa78".getBytes();
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

    LineBufferedInputStream lbis = new LineBufferedInputStream(bais, new byte[] {'a'});

    assertEquals("123", lbis.readLine());
    assertEquals("456", lbis.readLine());
    assertEquals("", lbis.readLine());
    assertEquals("78", lbis.readLine());
    assertNull(lbis.readLine());
  }


  public void test3() throws IOException {
    byte[] bytes = "123abc456aababc78b89".getBytes();
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

    LineBufferedInputStream lbis = new LineBufferedInputStream(bais, new byte[] {'a', 'b', 'c'});

    assertEquals("123", lbis.readLine());
    assertEquals("456aab", lbis.readLine());
    assertEquals("78b89", lbis.readLine());
    assertNull(lbis.readLine());
  }


  private static class MyException extends RuntimeException {

  }


  public void test4() throws IOException {
    byte[] bytes = "abc123aabcabc456aabaacbaabc78b89abcdacd".getBytes();
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

    LineBufferedInputStream lbis = new LineBufferedInputStream(bais, new byte[] {'a', 'a', 'b', 'c'});

    assertEquals("abc123", lbis.readLine());
    assertEquals("abc456aabaacb", lbis.readLine());
    assertEquals("78b89abcdacd", lbis.readLine());
    assertNull(lbis.readLine());
  }


  public void testHangingReadLine() throws Exception {
    byte[] buf = "1\r\n2\r\n3\r\n".getBytes();
    final Semaphore s = new Semaphore(6); //6 reads für die ersten beiden zeilen
    ByteArrayInputStream bais = new ByteArrayInputStream(buf) {


      @Override
      public synchronized int read() {
        if (!s.tryAcquire(1)) {
          throw new MyException();
        }
        return super.read();

      }


      @Override
      public synchronized int read(byte[] b, int off, int len) {
        if (!s.tryAcquire(len)) {
          throw new MyException();
        }
        return super.read(b, off, len);
      }

    };
    LineBufferedInputStream lbis = new LineBufferedInputStream(bais, LineMarker.CRLF);
    boolean myex = false;
    try {
      String line = lbis.readLine();
    } catch (MyException e) {
      myex = true;
    }
    assertTrue(myex);
  }


  public void testNotHangingReadLine() throws Exception {
    byte[] buf = "1\r\n2\r\n3\r\n".getBytes();
    final Semaphore s = new Semaphore(6); //6 reads für die ersten beiden zeilen
    ByteArrayInputStream bais = new ByteArrayInputStream(buf) {

      @Override
      public synchronized int read() {
        if (!s.tryAcquire(1)) {
          throw new MyException();
        }
        return super.read();
      }


      @Override
      public synchronized int read(byte[] b, int off, int len) {
        if (!s.tryAcquire(len)) {
          throw new MyException();
        }
        return super.read(b, off, len);
      }

    };
    LineBufferedInputStream lbis = new LineBufferedInputStream(bais, LineMarker.CRLF.getBytes(), "ASCII", false);
    assertEquals("1", lbis.readLine());
    assertEquals("2", lbis.readLine());
  }


}
