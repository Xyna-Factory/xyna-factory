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
import java.math.BigInteger;

import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.IPv6FormatException;
import com.gip.xyna.XMOM.base.net.exception.IllegalNetmaskLengthException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;


public class IPv6NetmaskData implements Serializable {
  
  private static final long serialVersionUID = 1L;

  /**
   * mask length in number of bits
   */
  private final int _length;
    
  /**
   * netmask as ip address (in BigInteger form)
   */
  private final BigInteger _ip;
  
  
  public IPv6NetmaskData(String length) throws ValidationException, FormatException {
    this(parseInputStr(length));
  }
  
  
  public IPv6NetmaskData(int length) throws ValidationException, FormatException {
    if (length > 128) {
      throw new IllegalNetmaskLengthException("IPv6 netmask too long: " + length);
    }
    _length = length;
    _ip = calculateIp(length);
  }
  
  
  private static int parseInputStr(String input) throws IPv6FormatException {
    String str = input;
    if (str.startsWith("/")) {
      str = input.substring(1);
    }
    try {
      return Integer.parseInt(str);
    }
    catch (Exception e) {
      throw new IPv6FormatException("Could not parse netmask: " + input);
    }
  }
  
  private static BigInteger calculateIp(int length) {
    BigInteger sum = BigInteger.ZERO;
    for (int i = 0; i < length; i++) {      
      BigInteger pow = BigInteger.ONE.shiftLeft(127 - i);
      sum = sum.add(pow);
    }
    return sum;
  }
  
  public int getLength() {
    return _length;
  }
  
  public BigInteger asBigIntegerIp() {
    return _ip;
  }
  
  
  public BigInteger getIPInverted() {
    return invertIP(this.asBigIntegerIp());
  }

  
  public static BigInteger invertIP(BigInteger ip) {
    BigInteger max = IPv6Address.Constant.MAX_IPV6_IP;
    return max.xor(ip);
  }
  
}
