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

import static org.junit.Assert.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.log4j.Logger;
import org.junit.Test;

import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;
import com.gip.xyna.XMOM.base.net.internal.IPv4Address;
import com.gip.xyna.XMOM.base.net.internal.IPv4NetmaskData;



public class TestIPv4Address {

  private static Logger _logger = LogManager.getLogger(TestIPv4Address.class);


  @Test
  public void test1() throws FormatException, ValidationException {
    IPv4Address ip = new IPv4Address("1.02.33.004");
    assertTrue(ip.getAsArray()[0] == 1);
    assertTrue(ip.getAsArray()[1] == 2);
    assertTrue(ip.getAsArray()[2] == 33);
    assertTrue(ip.getAsArray()[3] == 4);
    assertTrue(ip.getAsLong() == (1 * 256L * 256L * 256L +
                                  2 * 256L * 256L +
                                  33 * 256L +
                                  4));
  }

  @Test
  public void test2() throws FormatException, ValidationException {
    IPv4Address ip = new IPv4Address("255.255.255.255");
    _logger.debug(ip.getAsLong());
    assertTrue(ip.getAsLong() == 4294967295L);
    assertTrue("11111111111111111111111111111111".equals(ip.toBinaryString()));
  }

  @Test
  public void test3() throws FormatException {
    try {
      IPv4Address ip = new IPv4Address("0.0.0.256");
    }
    catch (Exception e) {
      assertTrue(e instanceof ValidationException);
      //assertTrue(e.getMessage().startsWith("Illegal byte value in IP address:"));
      return;
    }
    fail();
  }

  @Test
  public void test4() throws FormatException {
    try {
      new IPv4Address("-1.0.0.0");
    }
    catch (Exception e) {
      assertTrue(e instanceof ValidationException);
      return;
    }
    fail();
  }

  @Test
  public void test5() throws FormatException, ValidationException {
    IPv4Address ip = new IPv4Address(256);
    _logger.debug("test5: " + ip.toDotDecimalString());
    assertTrue("0.0.1.0".equals(ip.toDotDecimalString()));
    assertTrue(ip.getAsLong() == 256);
    assertTrue(ip.getAsArray()[0] == 0);
    assertTrue(ip.getAsArray()[1] == 0);
    assertTrue(ip.getAsArray()[2] == 1);
    assertTrue(ip.getAsArray()[3] == 0);
  }


  @Test
  public void test6() throws FormatException, ValidationException {
    short[] arr = new short[] { 1, 222, 33, 45 };
    IPv4Address ip = new IPv4Address(arr);
    assertTrue(ip.getAsArray()[0] == 1);
    assertTrue(ip.getAsArray()[1] == 222);
    assertTrue(ip.getAsArray()[2] == 33);
    assertTrue(ip.getAsArray()[3] == 45);
    assertTrue(ip.getAsLong() == (1 * 256L * 256L * 256L +
                                  222 * 256L * 256L +
                                  33 * 256L +
                                  45));
    _logger.debug("test6: " + ip.toZeroPaddedDotDecimalString());
    assertTrue("001.222.033.045".equals(ip.toZeroPaddedDotDecimalString()));
  }


  @Test
  public void test7() throws FormatException, ValidationException {
    IPv4Address ip = IPv4Address.fromBinaryString("11111111.00000000.00000000.11111111");
    long ip2Val = 0xFF0000FFL;
    IPv4Address ip2 = new IPv4Address(ip2Val);
    _logger.debug("test7: " + ip.getAsLong());
    _logger.debug("test7: " + ip2.getAsLong());
    assertTrue(ip.getAsLong() == ip2.getAsLong());
    assertTrue("11111111000000000000000011111111".equals(ip2.toBinaryString()));
  }

  @Test
  public void test8() throws FormatException {
    try {
      IPv4Address.fromBinaryString("1110.1.1.2");
    }
    catch (Exception e) {
      assertTrue(e instanceof FormatException);
      return;
    }
    fail();
  }


  @Test
  public void test9() throws FormatException, ValidationException {
    IPv4Address ipMask = new IPv4Address(0xFFFFFF00L);
    IPv4NetmaskData mask = new IPv4NetmaskData(ipMask);
    IPv4Address ip = new IPv4Address(0x00FF0000L);
    assertTrue(ip.isNetworkAddress(mask));
    IPv4Address ip2 = new IPv4Address(0x00FF0010L);
    assertFalse(ip2.isNetworkAddress(mask));
  }

  @Test
  public void test10() throws FormatException, ValidationException {
    IPv4Address ipMask = new IPv4Address(0xFFFFFF00L);
    IPv4NetmaskData mask = new IPv4NetmaskData(ipMask);
    IPv4Address ip = new IPv4Address(0x00FF0001L);
    assertTrue(ip.isGatewayAddress(mask));
    IPv4Address ip2 = new IPv4Address(0x00FF0010L);
    assertFalse(ip2.isGatewayAddress(mask));
  }

  @Test
  public void test11() throws FormatException, ValidationException {
    IPv4Address ipMask = new IPv4Address(0xFFFFFF00L);
    IPv4NetmaskData mask = new IPv4NetmaskData(ipMask);
    IPv4Address ip = new IPv4Address(0x00FF00FFL);
    assertTrue(ip.isBroadcastAddress(mask));
    IPv4Address ip2 = new IPv4Address(0x00FF0010L);
    assertFalse(ip2.isBroadcastAddress(mask));
  }

  @Test
  public void test12() throws FormatException, ValidationException {
    IPv4Address ip = new IPv4Address("192-168-5-127");
    _logger.debug("test12: " + ip.toDotDecimalString());
    assertTrue(ip.getAsArray()[0] == 192);
    assertTrue(ip.getAsArray()[1] == 168);
    assertTrue(ip.getAsArray()[2] == 5);
    assertTrue(ip.getAsArray()[3] == 127);
  }

  @Test
  public void test13() throws FormatException {
    try {
      new IPv4Address("192.168-5-127");
    }
    catch (Exception e) {
      assertTrue(e instanceof FormatException);
      return;
    }
    fail();
  }

  @Test
  public void test14() throws FormatException {
    try {
      new IPv4Address("192-168-5-127-");
    }
    catch (Exception e) {
      assertTrue(e instanceof FormatException);
      return;
    }
    fail();
  }

  @Test
  public void test15() throws FormatException, ValidationException {
    IPv4Address ip = new IPv4Address("192_168_15_129");
    _logger.debug("test15: " + ip.toDotDecimalString());
    assertTrue(ip.getAsArray()[0] == 192);
    assertTrue(ip.getAsArray()[1] == 168);
    assertTrue(ip.getAsArray()[2] == 15);
    assertTrue(ip.getAsArray()[3] == 129);
  }

  @Test
  public void test16() throws FormatException, ValidationException {
    IPv4Address ip = new IPv4Address("172.19.3.1");
    _logger.debug("test16: ip=" + ip.toDotDecimalString());
    _logger.debug("test16: ip as long=" + ip.getAsLong());
    assertTrue(ip.getAsLong() == 2886927105L);

    IPv4NetmaskData mask = new IPv4NetmaskData("255.255.255.0");
    _logger.debug("test16: mask=" + mask.getIPv4Address().toDotDecimalString());

    long networkIp = mask.getIPv4Address().getAsLong() & ip.getAsLong();
    IPv4Address networkAddr = new IPv4Address(networkIp);
    IPv4SubnetData subnet = new IPv4SubnetData(networkAddr, mask);
    _logger.debug("test16: subnet=" + subnet.getNetworkAddress().toDotDecimalString());
    _logger.debug("test16: subnet as long=" + subnet.getNetworkAddress().getAsLong());
    assertTrue(subnet.getNetworkAddress().getAsLong() == 2886927104L);
  }

  @Test
  public void test17() throws FormatException, ValidationException {
    IPv4Address ip = new IPv4Address("172.19.3.1");
    _logger.debug("test17: ip=" + ip.toDotDecimalString());
    _logger.debug("test17: ip as long=" + ip.getAsLong());
    assertTrue(ip.getAsLong() == 2886927105L);

    IPv4NetmaskData mask = new IPv4NetmaskData("255.255.255.0");
    IPv4Address networkAddr = ip.toNetworkAddressOfNetmask(mask);
    IPv4SubnetData subnet = new IPv4SubnetData(networkAddr, mask);
    _logger.debug("test17: subnet=" + subnet.getNetworkAddress().toDotDecimalString());
    _logger.debug("test17: subnet as long=" + subnet.getNetworkAddress().getAsLong());
    assertTrue(subnet.getNetworkAddress().getAsLong() == 2886927104L);
  }

}
