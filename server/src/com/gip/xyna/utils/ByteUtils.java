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
package com.gip.xyna.utils;



import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.regex.Pattern;



/**
 * Methoden-Sammlung:
 * 1) um daten in bytearrays zu schreiben und daraus zu lesen
 * 2) bytes in hexadezimal-geschriebene strings umzuwandeln (und umgekehrt)
 * 
 * TODO Refactoring: Klare Benennung der Funktionen 
 * Verwendet vom Modul DHcpClient und SSHServer
 */
public class ByteUtils {


  private final static char[] hexchars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
  private final static char[] hexcharsUpper = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  private final static String[] hexvals = new String[256];
  private final static String[] hexvalsLower = new String[256];
  private final static byte[] singleDigits = new byte[103];
  private final static byte[][] doubleDigits = new byte[103][];
  static {
    for (int i = 0; i < 256; i++) {
      String hex = Integer.toHexString(i & 0xFF);
      if (hex.length() < 2) {
        String s = "0" + hex.toUpperCase();
        hexvals[i] = s;
        hexvalsLower[i] = s.toLowerCase();
        singleDigits[Character.toUpperCase(hex.charAt(0))] = (byte) (i & 0xFF);
        singleDigits[Character.toLowerCase(hex.charAt(0))] = (byte) (i & 0xFF);
        hex = s;
      } else {
        String s = hex.toUpperCase();
        hexvals[i] = s;
        hexvalsLower[i] = s.toLowerCase();

      }

      byte[] baLower = doubleDigits[Character.toLowerCase(hex.charAt(0))];
      if (baLower == null) {
        baLower = new byte[103];
        doubleDigits[Character.toLowerCase(hex.charAt(0))] = baLower;
      }
      byte[] baUpper = doubleDigits[Character.toUpperCase(hex.charAt(0))];
      if (baUpper == null) {
        baUpper = new byte[103];
        doubleDigits[Character.toUpperCase(hex.charAt(0))] = baUpper;
      }
      baLower[Character.toLowerCase(hex.charAt(1))] = (byte) (i & 0xFF);
      baUpper[Character.toUpperCase(hex.charAt(1))] = (byte) (i & 0xFF);
    }
  }


  public static void writeInt(final byte[] ba, final int intValue, final int offset) {
    ba[offset] = (byte) ((intValue >> 24) & 0x000000FF);
    ba[offset + 1] = (byte) ((intValue >> 16) & 0x000000FF);
    ba[offset + 2] = (byte) ((intValue >> 8) & 0x000000FF);
    ba[offset + 3] = (byte) (intValue & 0x000000FF);
  }


  public static void writeLong(final byte[] ba, final long longValue, final int offset) {
    ba[offset] = (byte) ((longValue >> 56) & 0x000000FF);
    ba[offset + 1] = (byte) ((longValue >> 48) & 0x000000FF);
    ba[offset + 2] = (byte) ((longValue >> 40) & 0x000000FF);
    ba[offset + 3] = (byte) ((longValue >> 32) & 0x000000FF);
    ba[offset + 4] = (byte) ((longValue >> 24) & 0x000000FF);
    ba[offset + 5] = (byte) ((longValue >> 16) & 0x000000FF);
    ba[offset + 6] = (byte) ((longValue >> 8) & 0x000000FF);
    ba[offset + 7] = (byte) (longValue & 0x000000FF);
  }


  public static void writeLong(final byte[] ba, final long longValue, final int offset, int nrOfBytes) {
    for (int i = 0; i < nrOfBytes; i++) {
      ba[offset + i] = (byte) ((longValue >> 8 * (nrOfBytes - 1 - i)) & 0x000000FF);
    }
  }


  public static String toHexString(byte b, boolean upperCase) {
    if (upperCase) {
      return hexvals[b & 0xFF];
    } else {
      return hexvalsLower[b & 0xFF];
    }
  }
  
  public static String toHexString(byte[] bs, boolean upperCase, String separator) {
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for( byte b : bs ) {
      sb.append(sep).append( upperCase ? hexvals[b & 0xFF] : hexvalsLower[b & 0xFF] );
      if( separator != null ) {
        sep = separator;
      }
    }
    return sb.toString();
  }
  
  public static String toHexString(byte[] bs, boolean upperCase, String separator, boolean startsWith0x ) {
    StringBuilder sb = new StringBuilder();
    if( startsWith0x ) {
      sb.append("0x");
    }
    String sep = "";
    for( byte b : bs ) {
      sb.append(sep).append( upperCase ? hexvals[b & 0xFF] : hexvalsLower[b & 0xFF] );
      if( separator != null ) {
        sep = separator;
      }
    }
    return sb.toString();
  }


  /**
   * string darf nicht mit 0x anfangen
   */
  public static String readHexValue(byte[] ba, int offset, int len, boolean uppercase) {
    char[] hexc;
    if (uppercase) {
      hexc = hexcharsUpper;
    } else {
      hexc = hexchars;
    }
    char[] chars = new char[len * 2];
    for (int i = 0; i < len; i++) {
      int v = ba[i + offset] & 0xff;
      int w = v >> 4;
      chars[i * 2] = hexc[w];
      w = v & 0xf;
      chars[i * 2 + 1] = hexc[w];
    }
    return String.valueOf(chars);
  }

  /**
   * schreibt nicht "0x" zu beginn
   */
  public static void writeHexValue(byte[] ba, int offset, String hexVal) {
    int l = hexVal.length();
    if (l % 2 != 0) {
      throw new IllegalArgumentException("Hex string must have even length.");
    }
    for (int i = 0; i < l / 2; i++) {
      String s = hexVal.substring(2 * i, 2 * i + 2);
      ba[offset + i] = fromHexString(s);
    }
  }


  /**
   * s muss l�nge 1 oder 2 haben
   */
  public static byte fromHexString(String s) {
    int len = s.length();
    if (len == 1) {
      return singleDigits[s.charAt(0)];
    } else if (len == 2) {
      return doubleDigits[s.charAt(0)][s.charAt(1)];
    } else {
      throw new IllegalArgumentException("Hex string must have length <= 2.");
    }
  }


  /**
   * schreibt erst die l�nge des strings in das bytearray (immer 2 bytes), und danach den eigentlichen string
   * ACHTUNG: mit utf8 funktioniert das nicht korrekt, weil keine 1:1 beziehung zwischen characters und bytes besteht
   */
  public static void writeString(byte[] data, String value, int offset, int maxLength, Charset charset) {
    if (value == null) {
      throw new RuntimeException("null value not supported in setString");
    }
    //iso 8859-15 ist eine 8bit codierung, damit kann man gefahrlos bytes abschneiden
    ByteBuffer b = charset.encode(CharBuffer.wrap(value, 0, Math.min(value.length(), maxLength)));
    int len = Math.min(b.remaining(), maxLength);
    b.get(data, offset + 2, len);
    data[offset] = (byte) (len >>> 8);
    data[offset + 1] = (byte) (len);
  }


  public static String readString(byte[] data, int offset, Charset charset) {
    int length = ((data[offset] & 0x000000FF) << 8) + ((data[offset + 1] & 0x000000FF));
    CharBuffer buffer = charset.decode(ByteBuffer.wrap(data, offset + 2, length));
    return buffer.toString();
  }


  public static long readLong(byte[] data, int offset) {
    long s = 0;
    byte d = data[offset];
    if (d != 0) {
      s += (d & 0x00000000000000FFl) << 56;
    }
    d = data[offset + 1];
    if (d != 0) {
      s += (d & 0x00000000000000FFl) << 48;
    }
    d = data[offset + 2];
    if (d != 0) {
      s += (d & 0x00000000000000FFl) << 40;
    }
    d = data[offset + 3];
    if (d != 0) {
      s += (d & 0x00000000000000FFl) << 32;
    }
    d = data[offset + 4];
    if (d != 0) {
      s += (d & 0x00000000000000FFl) << 24;
    }
    d = data[offset + 5];
    if (d != 0) {
      s += (d & 0x00000000000000FFl) << 16;
    }
    d = data[offset + 6];
    if (d != 0) {
      s += (d & 0x00000000000000FFl) << 8;
    }
    d = data[offset + 7];
    if (d != 0) {
      s += d & 0x00000000000000FFl;
    }
    return s;
  }


  public static int readInt(byte[] data, int offset) {
    return ((data[offset] & 0x000000FF) << 24) + ((data[offset + 1] & 0x000000FF) << 16) + ((data[offset + 2] & 0x000000FF) << 8)
        + (data[offset + 3] & 0x000000FF);
  }
  
  public static short readShort(byte[] data, int offset) {
    return (short) ( ((data[offset] & 0x000000FF) << 8) +  (data[offset + 1] & 0x000000FF) );
  }


  public static byte[] intToByteArray(int value) {
    byte[] b = new byte[4];
    writeInt(b, value, 0);
    return b;
  }


  public static byte[] longToByteArray(long value, int nrOfBytes) {
    byte[] b = new byte[nrOfBytes];
    writeLong(b, value, 0, nrOfBytes);
    return b;
  }


  /**
   * Parsen eines Byte-Array aus Hex-String.
   * Beispiel: 
   * assertEquals("ab:cd:00:00", ByteUtils.toHexString( ByteUtils.fromHexString("abcd", 4, true), false, ":"));
   * assertEquals("ab:cd:e0:00", ByteUtils.toHexString( ByteUtils.fromHexString("abcde", 4, true), false, ":"));
   * assertEquals("00:00:ab:cd", ByteUtils.toHexString( ByteUtils.fromHexString("abcd", 4, false), false, ":"));
   * assertEquals("00:0a:bc:de", ByteUtils.toHexString( ByteUtils.fromHexString("abcde", 4, false), false, ":"));
  
   * @param hex
   * @param length
   * @param fromLeft falls hex.length() &lt; length*2, wird mit 0 aufgef�llt, von links oder rechts
   * @return byte[length]
   */
  public static byte[] fromHexString(String hex, int length, boolean fromLeft) {
    byte[] bytes = new byte[length];
    
    int max = hex.length();
    
    if( fromLeft ) {
      for( int i=0; i<length; ++i ) {
        if( i*2+2 > max) {
          if( i*2+1 == max ) {
            bytes[i] = (byte)(Integer.parseInt(hex.substring(i*2,i*2+1), 16)*16);
          }
          break;
        }
        bytes[i] = (byte)Integer.parseInt(hex.substring(i*2,i*2+2), 16);
      }
    } else {
      for( int i=0; i<length; ++i ) {
        if( max-i*2-2 < 0 ) {
          if( max-i*2-1 == 0 ) {
            bytes[length-i-1] = (byte)Integer.parseInt(hex.substring(0,1), 16);
          }
          break;
        }
        bytes[length-i-1] = (byte)Integer.parseInt(hex.substring(max-i*2-2,max-i*2), 16);
      }
    }
    return bytes;
  }

  
  
  private static final Pattern hexValuePattern = Pattern.compile("0x([0-9A-Fa-f]{2})+");

  public static byte[] fromHexStringWithLeading0x(final String hexValue) {
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



}
