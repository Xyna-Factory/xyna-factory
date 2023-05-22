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
package com.gip.xyna.xact.triggerv6.tlvencoding.utilv6;

import java.util.regex.Pattern;

public final class ByteUtil {

  private ByteUtil() {
  }


  public static byte[] toByteArray(final int intValue) {
    byte[] result = new byte[4];
    result[0] = (byte) ((intValue >> 24) & 0x000000FF);
    result[1] = (byte) ((intValue >> 16) & 0x000000FF);
    result[2] = (byte) ((intValue >> 8) & 0x000000FF);
    result[3] = (byte) (intValue & 0x000000FF);
    return result;
  }


  public static byte[] toByteArray(final long longValue) {
    byte[] result = new byte[8];
    result[0] = (byte) ((longValue >> 56) & 0x000000FF);
    result[1] = (byte) ((longValue >> 48) & 0x000000FF);
    result[2] = (byte) ((longValue >> 40) & 0x000000FF);
    result[3] = (byte) ((longValue >> 32) & 0x000000FF);
    result[4] = (byte) ((longValue >> 24) & 0x000000FF);
    result[5] = (byte) ((longValue >> 16) & 0x000000FF);
    result[6] = (byte) ((longValue >> 8) & 0x000000FF);
    result[7] = (byte) (longValue & 0x000000FF);
    return result;
  }


  public static byte[] toByteArray(final long longValue, int nrOfBytes) {
    byte[] result = new byte[nrOfBytes];
    for (int i=0; i<nrOfBytes; i++) {
      result[i] = (byte) ((longValue >> 8*(nrOfBytes-1-i)) & 0x000000FF);
    }
    return result;
  }


  private static final Pattern hexValuePattern = Pattern.compile("0x([0-9A-F]{2})+");

  public static byte[] toByteArray(final String hexValue) {
    if (hexValue == null) {
      throw new IllegalArgumentException("Hex value may not be null.");
    } else if (!hexValuePattern.matcher(hexValue).matches()) {
      throw new IllegalArgumentException("Expected hex value, but got: <" + hexValue + ">");
    }
    String hexString = hexValue.split("x")[1];
    int byteCount = hexString.length() / 2;
    byte[] bytes = new byte[byteCount];
    for (int i = 0; i < byteCount; ++i) {
      int index = i * 2;
      String element = hexString.substring(index, index + 2);
      bytes[i] = (byte) Integer.parseInt(element, 16);
    }
    return bytes;
  }


  public static String toHexValue(final byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("Byte array may not be null.");
    } else if (bytes.length < 1) {
      throw new IllegalArgumentException("Empty byte array provided.");
    }
    StringBuilder sb = new StringBuilder();
    sb.append("0x");
    for (int i = 0; i < bytes.length; ++i) {
      byte b = bytes[i];
      String hex = Integer.toHexString(((int) b) & 0xFF);
      if (hex.length() < 2) {
        sb.append("0");
      }
      sb.append(hex.toUpperCase());
    }
    return sb.toString();
  }

}
