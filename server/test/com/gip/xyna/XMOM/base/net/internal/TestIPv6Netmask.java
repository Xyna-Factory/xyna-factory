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

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;

public class TestIPv6Netmask {

  private static Logger _logger = Logger.getLogger(TestIPv6Netmask.class);
  

  @Test
  public void test1() throws FormatException, ValidationException {
    BigInteger val = BigInteger.valueOf(1).shiftLeft(128);
    val = val.subtract(BigInteger.ONE);
    _logger.info(val.toString(16));
    _logger.info(val.toString(2));
    _logger.info(val.toString(2).length());
    
    IPv6Address addr = new IPv6Address(val);
    _logger.info(addr.toFullHexRepresentation());
    assertTrue("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff".equals(addr.toFullHexRepresentation()));
    
    assertTrue(val.equals(IPv6Address.Constant.MAX_IPV6_IP));
  }
  
  
  @Test
  public void test2() throws FormatException, ValidationException {
    IPv6NetmaskData mask = new IPv6NetmaskData(4);
    IPv6Address addr = new IPv6Address(mask.asBigIntegerIp());
    _logger.info("test 2: " + addr.toShortHexRepresentation());
    assertTrue("f000::".equals(addr.toShortHexRepresentation()));
  }
  
  
  @Test
  public void test3() throws FormatException, ValidationException {    
    IPv6NetmaskData mask = new IPv6NetmaskData(32);
    IPv6Address addr = new IPv6Address(mask.asBigIntegerIp());
    _logger.info("test 3: " + addr.toShortHexRepresentation());
    assertTrue("ffff:ffff::".equals(addr.toShortHexRepresentation()));
    
    IPv6Address inverted = new IPv6Address(mask.getIPInverted());
    _logger.info(inverted.toShortHexRepresentation());
    assertTrue("::ffff:ffff:ffff:ffff:ffff:ffff".equals(inverted.toShortHexRepresentation()));
  }
  
  
  @Test
  public void test4() throws FormatException, ValidationException {
    IPv6NetmaskData mask = new IPv6NetmaskData("/8");
    IPv6Address addr = new IPv6Address(mask.asBigIntegerIp());
    _logger.info("test 4: " + addr.toShortHexRepresentation());
    assertTrue("ff00::".equals(addr.toShortHexRepresentation()));
  }
  
  @Test
  public void test5() throws FormatException, ValidationException {
    IPv6NetmaskData mask = new IPv6NetmaskData("12");
    IPv6Address addr = new IPv6Address(mask.asBigIntegerIp());
    _logger.info("test 5: " + addr.toShortHexRepresentation());
    assertTrue("fff0::".equals(addr.toShortHexRepresentation()));
  }
  
}
