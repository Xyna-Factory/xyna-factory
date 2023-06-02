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

package com.gip.xyna.XMOM.base.net.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.IPv4FormatException;
import com.gip.xyna.XMOM.base.net.exception.IPv4ValidationException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;


/**
 * helper class to handle ipv4 technical matters;
 */
public class IPv4Address implements Serializable {

  private static final long serialVersionUID = 1L;


  public static class Constant {
    //public static final long MAX_IP = 4294967295L;
    public static final long _256_TO_SECOND = 256L * 256L;
    public static final long _256_TO_THIRD = 256L * 256L * 256L;
    public static final long _256_TO_FOURTH = 256L * 256L * 256L * 256L;
    public static final long MAX_IP = _256_TO_FOURTH - 1;

    public static final Pattern BINARY_STR_PATTERN = Pattern.compile("[01\\.]+");
    public static final Pattern IP_STR_PATTERN = Pattern.compile("^\\d{1,3}(\\D)\\d{1,3}(\\D)\\d{1,3}(\\D)\\d{1,3}$");
  }

  private final long _longVal;
  private final short[] _bytes = new short[4];


  /**
   * Expects string in decimal-notation,
   * with leading zeroes optional,
   * e.g. "001.2.33.04";
   * separator character can be either '.' or any other non-digit character (\D)
   * @throws ValidationException
   */
  public IPv4Address(String val) throws FormatException, ValidationException {
    String[] parts = val.split("\\.");

    if (parts.length != 4) {
      parts = getIPStrParts(val);
    }
    for (int i = 0; i < 4; i++) {
      short shortVal = Short.parseShort(parts[i]);
      checkByteVal(shortVal);
      _bytes[i] = shortVal;
    }
    _longVal = getLongFromArray(_bytes);
  }


  public IPv4Address(long val) throws FormatException, ValidationException {
    if ((val > Constant.MAX_IP) || (val < 0)) {
      throw new IPv4ValidationException("'" + val + "' is not a valid IP address (between 0 and " + Constant.MAX_IP + ").");
    }
    _longVal = val;
    _bytes[0] = (short) ((val >>> 24) & 255);
    _bytes[1] = (short) ((val >>> 16) & 255);
    _bytes[2] = (short) ((val >>> 8) & 255);
    _bytes[3] = (short) (val & 255);
  }


  public IPv4Address(short[] bytes) throws FormatException, ValidationException {
    if (bytes.length != 4) {
      throw new IPv4FormatException("Expected four bytes, got " + bytes.length + " - " + Arrays.toString(bytes));
    }
    for (int i = 0; i < 4; i++) {
      short shortVal = bytes[i];
      checkByteVal(shortVal);
      _bytes[i] = shortVal;
    }
    _longVal = getLongFromArray(_bytes);
  }


  private String[] getIPStrParts(String ip) throws FormatException {
    Matcher matcher = Constant.IP_STR_PATTERN.matcher(ip);
    if (!matcher.matches()) {
      throw new IPv4FormatException(ip + " does not match ip pattern. Expected four groups consisting of one to three digits separated by a non-digit.");
    }
    boolean couldParse = true;
    String separator = matcher.group(1);
    if (separator == null) {
      couldParse = false;
    } else if (!separator.equals(matcher.group(2))) {
      couldParse = false;
    } else if (!separator.equals(matcher.group(3))) {
      couldParse = false;
    }

    if (!couldParse) {
      throw new IPv4FormatException(ip + " does not match ip pattern. Expected separators to be consistent. Got: '" + separator + "', '" + matcher.group(2) + ", '" + matcher.group(3) + "'.");
    }
    String[] ret = ip.split(separator);
    if (ret.length != 4) {
      throw new IPv4FormatException(ip + " does not match ip pattern. After splitting at separator '" + separator + "', there are '" + ret.length + "' values instead of the expected 4.");
    }
    return ret;
  }


  public static IPv4Address fromBinaryString(String addr) throws FormatException, ValidationException {
    if (!Constant.BINARY_STR_PATTERN.matcher(addr).matches()) {
      throw new IPv4FormatException(addr + ". Expected binary string (0s and 1s).");
    }
    long p = 1;
    long sum = 0;
    for (int l = addr.length() - 1; l >= 0; l--) {
      if (addr.charAt(l) == '1') {
        sum += p;
      }
      if (addr.charAt(l) != '.') {
        p *= 2;
      }
    }
    return new IPv4Address(sum);
  }


  private static long getLongFromArray(short[] bytes) {
    return bytes[0] * Constant._256_TO_THIRD +
           bytes[1] * Constant._256_TO_SECOND +
           bytes[2] * 256L +
           bytes[3];
  }

  public long getAsLong() {
    return _longVal;
  }

  public short[] getAsArray() {
    return _bytes;
  }


  public String toBinaryString() {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < 4; i++) {
      s.append(byteValTo8DigitBinaryString(_bytes[i]));
    }
    return s.toString();
  }

  public String toDotDecimalString() {
    StringBuilder s = new StringBuilder();
    s.append(_bytes[0]);
    for (int i = 1; i < 4; i++) {
      s.append(".");
      s.append(_bytes[i]);
    }
    return s.toString();
  }

  public String toZeroPaddedDotDecimalString() {
    StringBuilder s = new StringBuilder();
    boolean first = true;
    for (int i = 0; i < 4; i++) {
      if (first) {
        first = false;
      }
      else {
        s.append(".");
      }
      if (_bytes[i] < 10) {
        s.append("00");
      }
      else if (_bytes[i] < 100) {
        s.append("0");
      }
      s.append(_bytes[i]);
    }
    return s.toString();
  }


  private void checkByteVal(short val) throws FormatException, ValidationException {
    if ((val < 0) || (val > 255)) {
      throw new IPv4ValidationException("'" + val + "' is not between 0 and 255.");
    }
  }

  private String byteValTo8DigitBinaryString(short val) {
    return Integer.toBinaryString(256 + val).substring(1);
  }


  public boolean isBroadcastAddress(IPv4NetmaskData maskIn) {
    long maskInverted = maskIn.getIPInvertedAsLong();
    long and = maskInverted & this.getAsLong();
    if (and == maskInverted) {
      return true;
    }
    return false;
  }

  public boolean isNetworkAddress(IPv4NetmaskData maskIn) {
    long maskInverted = maskIn.getIPInvertedAsLong();
    long and = maskInverted & this.getAsLong();
    if (and == 0) {
      return true;
    }
    return false;
  }

  public boolean isGatewayAddress(IPv4NetmaskData maskIn) {
    long maskInverted = maskIn.getIPInvertedAsLong();
    long and = maskInverted & this.getAsLong();
    if (and == 1) {
      return true;
    }
    return false;
  }


  public boolean isMulticast() throws ValidationException, FormatException {
    return isPartOfSubnet("224.0.0.0", 4);
  }

  public boolean isPrivate() throws ValidationException, FormatException {
    boolean ret = (isPartOfSubnet("10.0.0.0", 8) || isPartOfSubnet("172.16.0.0", 12) ||
                   isPartOfSubnet("192.168.0.0", 16));
    return ret;
  }

  public boolean isLocalLoopback() throws ValidationException, FormatException {
    return isPartOfSubnet("127.0.0.0", 8);
  }

  public boolean isLinkLocal() throws ValidationException, FormatException {
    return isPartOfSubnet("169.254.0.0", 16);
  }


  private boolean isPartOfSubnet(String ipStr, int maskLength) throws ValidationException, FormatException {
    IPv4Address ip = new IPv4Address(ipStr);
    IPv4NetmaskData mask = new IPv4NetmaskData(maskLength);
    IPv4SubnetData sub = new IPv4SubnetData(ip, mask);
    return sub.ipWithinSubnet(this);
  }


  public IPv4Address inc() throws FormatException, ValidationException {
    return new IPv4Address(this.getAsLong() + 1);
  }

  public IPv4Address dec() throws FormatException, ValidationException {
    return new IPv4Address(this.getAsLong() - 1);
  }


  public IPv4Address toNetworkAddressOfNetmask(IPv4NetmaskData mask) throws FormatException, ValidationException {
    long networkIp = mask.getIPv4Address().getAsLong() & this.getAsLong();
    return new IPv4Address(networkIp);
  }


  public IPv4Address getWithOffset(IPv4Address addr) throws FormatException, ValidationException {
    if (addr == null) { return new IPv4Address(this.getAsLong()); }
    long sum = this.getAsLong() + addr.getAsLong();
    return new IPv4Address(sum);
  }


  @Override
  public int hashCode() {
    return new Long(getAsLong()).hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (o instanceof IPv4Address) {
      return (this.getAsLong() == ((IPv4Address) o).getAsLong());
    }
    return false;
  }

}
