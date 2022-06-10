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
package com.gip.xyna.utils;



import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;



public class ByteUtilsTest extends TestCase {

  public void testSingleBytes() {
    Random r = new Random();
    for (byte b = Byte.MIN_VALUE; b < Byte.MAX_VALUE; b++) {
      String s = ByteUtils.toHexString(b, r.nextBoolean());
      byte b2 = ByteUtils.fromHexString(s);
      assertEquals(b, b2);
    }
  }


  public void testInt() {
    Random r = new Random();
    int s = 513;
    int step = 4096;
    byte[] ba = new byte[s];
    for (int i = Integer.MIN_VALUE; i < Integer.MAX_VALUE - step; i += r.nextInt(step)) {
      int offset = r.nextInt(s - 4);
      ByteUtils.writeInt(ba, i, offset);
      assertEquals(i, ByteUtils.readInt(ba, offset));
    }
  }


  public void testLong() {
    Random r = new Random();
    int s = 513;
    long step = 12 * 1024;
    byte[] ba = new byte[s];
    for (long l = Long.MIN_VALUE; l < Long.MAX_VALUE - (step + 1L) * Integer.MAX_VALUE; l +=
        r.nextInt(Integer.MAX_VALUE) * step + r.nextInt(Integer.MAX_VALUE)) {
      int offset = r.nextInt(s - 8);
      ByteUtils.writeLong(ba, l, offset);
      assertEquals(l, ByteUtils.readLong(ba, offset));
    }
  }


  public void testString() {
    Charset charset = Charset.forName("ISO-8859-1");
    Random r = new Random();
    int s = 4411;
    byte[] ba = new byte[s];

    for (int k = 0; k < 10000; k++) {
      StringBuilder sb = new StringBuilder();
      int l = r.nextInt(400) + 1;
      for (int i = 0; i < l; i++) {
        sb.append((char) (r.nextInt(150) + 30));
      }
      String str = sb.toString();
      int offset = r.nextInt(s - 401);
      int maxlen = r.nextInt(l);
      ByteUtils.writeString(ba, str, offset, maxlen, charset);
      String strRead = ByteUtils.readString(ba, offset, charset);
      assertTrue(str.startsWith(strRead));
      assertEquals(Math.min(maxlen, str.length()), strRead.length());
    }
  }


  public void testHexStrings() {
    Random r = new Random();
    int s = 3211;
    byte[] ba = new byte[s];

    for (int i = 0; i < 30000; i++) {
      int offset = r.nextInt(s - 20);
      int len = r.nextInt(20);
      byte[] bytes = new byte[len];
      r.nextBytes(bytes);
      boolean uppercase = r.nextBoolean();
      String hexVal = ByteUtils.readHexValue(bytes, 0, bytes.length, uppercase);

      byte[] bytes2 = new byte[len];
      ByteUtils.writeHexValue(bytes2, 0, hexVal);
      assertEquals(Arrays.toString(bytes), Arrays.toString(bytes2));

      ByteUtils.writeHexValue(ba, offset, hexVal);
      String read = ByteUtils.readHexValue(ba, offset, len, uppercase);
      assertEquals(read, hexVal);
    }
  }

  
  public void testHexStrings2() {
    assertEquals("ab:cd:00:00", ByteUtils.toHexString( ByteUtils.fromHexString("abcd", 4, true), false, ":"));
    assertEquals("ab:cd:e0:00", ByteUtils.toHexString( ByteUtils.fromHexString("abcde", 4, true), false, ":"));
  
    assertEquals("00:00:ab:cd", ByteUtils.toHexString( ByteUtils.fromHexString("abcd", 4, false), false, ":"));
    assertEquals("00:0a:bc:de", ByteUtils.toHexString( ByteUtils.fromHexString("abcde", 4, false), false, ":"));
    
    
  }
  
  
  
}
