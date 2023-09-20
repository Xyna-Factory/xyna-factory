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
package com.gip.xyna.utils.misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import com.gip.xyna.xfmg.Constants;


/**
 * Wrapper für sun.misc.BASE64Encoder und BASE64Decoder, da diese deprecated sind und Compiler-Warnungne produzieren.
 * Hier tritt die Warnung nur einmal auf. Außerdem könnte hier eine alternative Implementierung eingebunden werden.
 * 
 */
public class Base64 {

  
  public static String encode(byte[] bytes) {
    String multiLine;
    try {
      multiLine = new String(java.util.Base64.getMimeEncoder().encode(bytes), Constants.DEFAULT_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return multiLine.replace("\r\n", "");
  }


  public static void encode(InputStream is, OutputStream os) throws IOException {
    byte[] buffer = new byte[2 << 14];
    byte[] destBuffer = new byte[buffer.length * 2];
    int len;
    while (0 < (len = is.read(buffer))) {
      byte[] src;
      if (len < buffer.length) {
        src = new byte[len];
        System.arraycopy(buffer, 0, src, 0, len);
      } else {
        src = buffer;
      }
      int destLen = java.util.Base64.getMimeEncoder().encode(src, destBuffer);
      os.write(destBuffer, 0, destLen);
    }
  }
  
  public static byte[] decode(String string) throws IOException {
    return java.util.Base64.getMimeDecoder().decode(string.getBytes(Constants.DEFAULT_ENCODING));
  }

}
