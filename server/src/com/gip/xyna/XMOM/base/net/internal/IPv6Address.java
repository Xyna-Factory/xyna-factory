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
import java.math.BigInteger;
import java.util.regex.Pattern;

import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.IPv4ValidationException;
import com.gip.xyna.XMOM.base.net.exception.IPv6FormatException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;


public class IPv6Address implements Serializable {

  private static final long serialVersionUID = 1L;


  public static class ZeroBlockData {
    int startGroupIndex = -1;
    int numGroupsInBlock = 0;
  }

  public static class Constant {
    public static final BigInteger MAX_IPV6_IP = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
    public static final Pattern PATTERN_FOR_BINARY = Pattern.compile("[01]{1,128}");
  }



  private final int[] _groups = new int[8];
  private final BigInteger _value;


  public IPv6Address(BigInteger valIn) throws FormatException {
    _value = valIn;
    setGroupsFromBigInt(valIn, _groups);
  }


  protected IPv6Address(int[] groups) throws IPv6FormatException {
    if (groups.length != 8) {
      throw new IPv6FormatException("Invalid array length, expected 8: " + groups.length);
    }
    for (int i = 0; i < 8; i++) {
      _groups[i]= groups[i];
    }
    _value = calculateBigIntegerValue(_groups);
  }


  public IPv6Address(String input) throws IPv6FormatException {
    if (input == null) {
      throw new IPv6FormatException("Invalid string: null.");
    }
    if (Constant.PATTERN_FOR_BINARY.matcher(input).matches()) {
      BigInteger fromBinary = new BigInteger(input, 2);
      _value = fromBinary;
      setGroupsFromBigInt(fromBinary, _groups);
      return;
    }

    int doubleColonIndex = input.indexOf("::");
    if (doubleColonIndex != input.lastIndexOf("::")) {
      throw new IPv6FormatException("Multiple double-colons found: " + input);
    }
    String[] splitByDoubleColon = input.split("::");
    if (splitByDoubleColon.length > 2) {
      throw new IPv6FormatException("Multiple double-colons found: " + input);
    }
    else if (splitByDoubleColon.length == 0) {
      _value = BigInteger.ZERO;
      return;
    }

    String[] beforeDoubleColon = splitByDoubleColon[0].split(":");
    if ((doubleColonIndex < 0) && (beforeDoubleColon.length != 8)) {
      throw new IPv6FormatException("Unexpected number of colons: " + input);
    }
    handleGroupsBeforeDoubleColon(beforeDoubleColon, _groups, input);

    if (splitByDoubleColon.length > 1) {
      handleGroupsAfterDoubleColon(beforeDoubleColon.length, splitByDoubleColon[1], _groups, input);
    }
    _value = calculateBigIntegerValue(_groups);
  }


  private static void handleGroupsBeforeDoubleColon(String[] beforeDoubleColon, int[] groups, String input)
                  throws IPv6FormatException {
    if (beforeDoubleColon.length == 1) {
      if ((beforeDoubleColon[0] == null) || (beforeDoubleColon[0].trim().length() < 1)) {
        return;
      }
    }
    for (int i = 0; i < beforeDoubleColon.length; i++) {
      String newPartsStr = beforeDoubleColon[i];
      if ((newPartsStr == null) || (newPartsStr.trim().length() < 1)) {
        throw new IPv6FormatException("Could not parse ipv6 address: " + input);
      }
      groups[i] = Integer.parseInt(newPartsStr, 16);
    }
  }


  private static void handleGroupsAfterDoubleColon(int numGroupsBeforeDoubleColon, String afterDoubleColonStr,
                                            int[] groups, String input) throws IPv6FormatException {
    if ((afterDoubleColonStr == null) || (afterDoubleColonStr.trim().length() < 1)) {
      return;
    }
    String[] afterDoubleColon = afterDoubleColonStr.split(":");
    int missingParts = 8 - numGroupsBeforeDoubleColon - afterDoubleColon.length;

    if (missingParts < 0) {
      throw new IPv6FormatException("Unexpected number of colons: " + input);
    }
    int firstPartIndexAfterDoubleColon = numGroupsBeforeDoubleColon + missingParts;

    for (int i = 0; i < afterDoubleColon.length; i++) {
      String newPartsStr = afterDoubleColon[i];
      if ((newPartsStr == null) || (newPartsStr.trim().length() < 1)) {
        throw new IPv6FormatException("Could not parse ipv6 address: " + input);
      }
      int groupIndex = firstPartIndexAfterDoubleColon + i;
      groups[groupIndex] = Integer.parseInt(newPartsStr, 16);
    }
  }


  private static BigInteger calculateBigIntegerValue(int[] groups) throws IPv6FormatException {
    if (groups[0] < 0 || groups[0] > 0xffff) {
      throw new IPv6FormatException("Each group must be between 0 and 0xFFFF but was: " + groups[0] + "(=0x" + Integer.toHexString(groups[0]).toUpperCase() + ")");
    }
    BigInteger tmp = BigInteger.valueOf(groups[0]);
    for (int i = 1; i < 8; i++) {
      tmp = tmp.shiftLeft(16);
      if (groups[i] < 0 || groups[i] > 0xffff) {
        throw new IPv6FormatException("Each group must be between 0 and 0xFFFF but was: " + groups[i] + "(=0x" + Integer.toHexString(groups[i]).toUpperCase() + ")");
      }
      tmp = tmp.add(BigInteger.valueOf(groups[i]));
    }
    return tmp;
  }


  private static void setGroupsFromBigInt(BigInteger valIn, int[] groups) throws IPv6FormatException {
    BigInteger val = valIn;
    BigInteger div = BigInteger.valueOf(0x10000);

    for (int p = 7; p >= 0; p--) {
      BigInteger[] dar = val.divideAndRemainder(div);
      val = dar[0];
      groups[p] = dar[1].intValue();
    }
    if (!val.equals(BigInteger.ZERO)) {
      throw new IPv6FormatException("Value is too large.");
    }
  }


  public BigInteger asBigInteger() {
    return _value;
  }


  public String toFullHexRepresentation() {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < _groups.length; i++) {
      if (i > 0) {
        s.append(":");
      }
      String groupStr = Integer.toHexString(_groups[i]);
      for (int k = groupStr.length(); k < 4; k++) {
        s.append("0");
      }
      s.append(groupStr);
    }
    return s.toString();
  }


  public String toShortHexRepresentation() {
    ZeroBlockData largestZeroBlock = findLargestZeroBlock();
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < _groups.length; i++) {
      if (i == largestZeroBlock.startGroupIndex) {
        s.append("::");
        continue;
      }
      else if ((i > largestZeroBlock.startGroupIndex) &&
               (i < largestZeroBlock.startGroupIndex + largestZeroBlock.numGroupsInBlock)) {
        continue;
      }
      else if (i > 0) {
        if (i != largestZeroBlock.startGroupIndex + largestZeroBlock.numGroupsInBlock) {
          s.append(":");
        }
      }
      String groupStr = Integer.toHexString(_groups[i]);
      s.append(groupStr);
    }
    return s.toString();
  }


  private ZeroBlockData findLargestZeroBlock() {
    ZeroBlockData largestSoFar = new ZeroBlockData();
    ZeroBlockData current = null;
    for (int i = 0; i < 8; i++) {
      if ((_groups[i] != 0) && (current != null)) {
        current = null;
        continue;
      }
      if (_groups[i] == 0) {
        if (current == null) {
          current = new ZeroBlockData();
          current.startGroupIndex = i;
          current.numGroupsInBlock = 1;
        }
        else {
          current.numGroupsInBlock++;
        }
        if (current.numGroupsInBlock > largestSoFar.numGroupsInBlock) {
          largestSoFar = current;
        }
      }
    }
    return largestSoFar;
  }


  public boolean isNetworkAddress(IPv6NetmaskData maskIn) {
    BigInteger maskInverted = maskIn.getIPInverted();
    BigInteger and = maskInverted.and(this.asBigInteger());
    if (and.equals(BigInteger.ZERO)) {
      return true;
    }
    return false;
  }


  public boolean isBroadcastAddress(IPv6NetmaskData maskIn) {
    BigInteger maskInverted = maskIn.getIPInverted();
    BigInteger and = maskInverted.and(this.asBigInteger());
    if (and.equals(maskInverted)) {
      return true;
    }
    return false;
  }


  public boolean isGatewayAddress(IPv6NetmaskData maskIn) {
    BigInteger maskInverted = maskIn.getIPInverted();
    BigInteger and = maskInverted.and(this.asBigInteger());
    if (and.equals(BigInteger.ONE)) {
      return true;
    }
    return false;
  }


  public boolean isLinkLocal() throws ValidationException, FormatException {
    return isPartOfSubnet("fe80::", 10);
  }

  public boolean isMulticast() throws ValidationException, FormatException {
    return isPartOfSubnet("ff00::", 8);
  }

  public boolean isUniqueLocalUnicast() throws ValidationException, FormatException {
    return isPartOfSubnet("fc00::", 7);
  }

  public boolean isUniqueLocalAddress() throws ValidationException, FormatException {
    return isPartOfSubnet("fd00::", 8);
  }

  public boolean isLocalLoopback() throws ValidationException, FormatException {
    return isPartOfSubnet("::1", 128);
  }

  public boolean isUnspecifiedAddress() throws ValidationException, FormatException {
    return isPartOfSubnet("::", 128);
  }



  private boolean isPartOfSubnet(String ipStr, int maskLength) throws ValidationException, FormatException {
    IPv6Address ip = new IPv6Address(ipStr);
    IPv6NetmaskData mask = new IPv6NetmaskData(maskLength);
    IPv6SubnetData sub = new IPv6SubnetData(ip, mask);
    return sub.ipWithinSubnet(this);
  }


  public IPv6Address inc() throws ValidationException, FormatException {
    return new IPv6Address(this.asBigInteger().add(BigInteger.ONE));
  }


  public IPv6Address getWithOffset(IPv6Address addr) throws FormatException, ValidationException {
    if (addr == null) { return new IPv6Address(this.asBigInteger()); }
    BigInteger sum = this.asBigInteger().add(addr.asBigInteger());
    return new IPv6Address(sum);
  }


  public boolean is6To4Address() throws ValidationException, FormatException {
    return isPartOfSubnet("2002::", 16);
  }


  public IPv4Address convertToV4Address() throws ValidationException, FormatException {
    if (!this.is6To4Address()) {
      throw new IPv4ValidationException("IPv6 address cannot be converted to IPv4: " +
                                        this.toShortHexRepresentation());
    }
    long val = _groups[1] * 256L * 256L + _groups[2];
    return new IPv4Address(val);
  }


  public static IPv6Address fromV4Address(IPv4Address addr) throws IPv6FormatException {
    short[] array = addr.getAsArray();
    int[] groups = new int[8];
    groups[0] = 0x2002;
    groups[1] = 256 * array[0] + array[1];
    groups[2] = 256 * array[2] + array[3];
    return new IPv6Address(groups);
  }

  @Override
  public int hashCode() {
    return asBigInteger().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (o instanceof IPv6Address) {
      return (this.asBigInteger().equals(((IPv6Address) o).asBigInteger()));
    }
    return false;
  }


  public IPv6Address toNetworkAddressOfNetmask(IPv6NetmaskData iPv6NetmaskData) throws FormatException {
    BigInteger and = iPv6NetmaskData.asBigIntegerIp().and(this.asBigInteger());
    return new IPv6Address(and);
  }

}
