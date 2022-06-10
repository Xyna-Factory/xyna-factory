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

package com.gip.xyna.XMOM.base.net.internal;

import java.io.Serializable;

import com.gip.xyna.XMOM.base.net.exception.AddressNoNetmaskException;
import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.IllegalNetmaskLengthException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;


public class IPv4NetmaskData implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * mask length in number of bits
   */
  private final int _length;

  /**
   * netmask as ip address
   */
  private final IPv4Address _ip;


  public IPv4NetmaskData(int length) throws ValidationException, FormatException {
    _ip = getIPFromLength(length);
    _length = length;
  }

  
  public IPv4NetmaskData(String input) throws ValidationException, FormatException {
    if (IPv4Address.Constant.IP_STR_PATTERN.matcher(input).matches()) {
      _ip = new IPv4Address(input);
      _length = getNetmaskLength(_ip);
      return;
    }
    _length = parseLengthString(input);
    _ip = getIPFromLength(_length);
  }
  
  
  private static int parseLengthString(String lengthIn) throws ValidationException, FormatException {
    String lengthStr = lengthIn;
    if (lengthIn.startsWith("/")) {
      lengthStr = lengthIn.substring(1);
    }
    try {
      return Integer.parseInt(lengthStr);
    }
    catch (Exception e) {
      throw new IllegalNetmaskLengthException(lengthIn);
    }
  }
  
  
  public IPv4NetmaskData(IPv4Address ip) throws ValidationException, FormatException {
    _length = getNetmaskLength(ip);
    _ip = ip;
  }

  public IPv4Address getIPv4Address() {
    return _ip;
  }

  public int getLength() {
    return _length;
  }

  private static int getNetmaskLength(IPv4Address ipIn) throws ValidationException {
    long ip = ipIn.getAsLong();
    int setBitCounter = 0;
    boolean foundUnsetBit = false;

    for (int i = 1; i <= 32; i++) {
      long tmp = powBase2(32 - i);
      long and = ip & tmp;
      if (and == 0) {
        if (!foundUnsetBit) {
          foundUnsetBit = true;
        }
      } else {
        //after first zero-bit, no more one-bits are allowed
        if (foundUnsetBit) {
          throw new AddressNoNetmaskException(ipIn.toBinaryString());
        }
        setBitCounter++;
      }
    }
    return setBitCounter;
  }


  public static IPv4Address getIPFromLength(int length) throws ValidationException, FormatException {
    if (length > 32) {
      throw new IllegalNetmaskLengthException("" + length);
    }
    long tmp = powBase2(32 - length) - 1;
    long ip = invertIPAsLong(tmp);
    return new IPv4Address(ip);
  }

  public long getIPInvertedAsLong() {
    return invertIPAsLong(this.getIPv4Address().getAsLong());
  }

  public static long invertIPAsLong(long ip) {
    //long max = 0xFFFFFFFF;
    long max = IPv4Address.Constant.MAX_IP;
    return (max ^ ip);
  }

  public static long powBase2(int n) {
    if (n < 0) {
      throw new RuntimeException("No negative powers of two supported.");
    }
    return (1L << n);
  }

}
