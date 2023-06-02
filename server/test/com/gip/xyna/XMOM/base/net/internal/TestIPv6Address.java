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
import com.gip.xyna.XMOM.base.net.exception.IPv6FormatException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;

public class TestIPv6Address {

  private static Logger _logger = Logger.getLogger(TestIPv6Address.class);
  

  @Test
  public void test1() throws FormatException, ValidationException {
    IPv6Address addr = new IPv6Address("1:2:3:4:5:6:7:8");
    _logger.info(addr.toFullHexRepresentation());
    assertTrue("0001:0002:0003:0004:0005:0006:0007:0008".equals(addr.toFullHexRepresentation()));
    _logger.info(addr.asBigInteger().toString(16));
    assertTrue(addr.asBigInteger().equals(new BigInteger("10002000300040005000600070008", 16)));
    _logger.info(addr.toShortHexRepresentation());
    assertTrue("1:2:3:4:5:6:7:8".equals(addr.toShortHexRepresentation()));
  }
    
  @Test
  public void test2() throws FormatException, ValidationException {
    IPv6Address addr = new IPv6Address("01:2::006:7:0008");
    _logger.info("test 2: " + addr.toFullHexRepresentation());
    assertTrue("0001:0002:0000:0000:0000:0006:0007:0008".equals(addr.toFullHexRepresentation()));
    _logger.info(addr.asBigInteger().toString(16));
    assertTrue(addr.asBigInteger().equals(new BigInteger("10002000000000000000600070008", 16)));
    _logger.info(addr.toShortHexRepresentation());
    assertTrue("1:2::6:7:8".equals(addr.toShortHexRepresentation()));
  }
   
  
  @Test
  public void test3() throws FormatException, ValidationException {
    IPv6Address addr = new IPv6Address("::000:6A:70B:8C00");
    _logger.info("test 3: " + addr.toFullHexRepresentation());
    assertTrue("0000:0000:0000:0000:0000:006a:070b:8c00".equals(addr.toFullHexRepresentation()));
    _logger.info(addr.asBigInteger().toString(16));
    assertTrue(addr.asBigInteger().equals(new BigInteger("6a070b8c00", 16)));
    _logger.info(addr.toShortHexRepresentation());
    assertTrue("::6a:70b:8c00".equals(addr.toShortHexRepresentation()));
  }
  
  
  @Test
  public void test4() throws FormatException, ValidationException {
    IPv6Address addr = new IPv6Address("1:000:3A:40B:5C00::");
    _logger.info("test 4: " + addr.toFullHexRepresentation());
    assertTrue("0001:0000:003a:040b:5c00:0000:0000:0000".equals(addr.toFullHexRepresentation()));
    _logger.info(addr.asBigInteger().toString(16));
    assertTrue(addr.asBigInteger().equals(new BigInteger("10000003a040b5c00000000000000", 16)));
    _logger.info(addr.toShortHexRepresentation());
    assertTrue("1:0:3a:40b:5c00::".equals(addr.toShortHexRepresentation()));
  }
  
  
  @Test
  public void test5() throws FormatException, ValidationException {
    BigInteger pow = BigInteger.ONE.shiftLeft(4);
    String binaryStr = pow.toString(2);
    _logger.info("test 5: " +  binaryStr);
    IPv6Address addr = new IPv6Address(binaryStr);
    _logger.info(addr.toShortHexRepresentation());
    assertTrue("::10".equals(addr.toShortHexRepresentation()));    
  }
  
  
  @Test
  public void test6() throws FormatException, ValidationException {
    BigInteger pow = BigInteger.ONE.shiftLeft(127);
    String binaryStr = pow.toString(2);
    IPv6Address addr = new IPv6Address(binaryStr);
    _logger.info("test 6: " + addr.toShortHexRepresentation());
    assertTrue("8000::".equals(addr.toShortHexRepresentation()));
  }
  
  
  @Test
  public void test7() throws FormatException, ValidationException {
    BigInteger pow = BigInteger.ONE.shiftLeft(128);
    String binaryStr = pow.toString(2);
    _logger.info("test 7: " +  binaryStr);
    try {
      new IPv6Address(binaryStr);
    }
    catch (IPv6FormatException e) {
      return;
    }
    fail("Expected exception");
  }
  
  
  @Test
  public void test8() throws FormatException, ValidationException {    
    IPv6NetmaskData mask = new IPv6NetmaskData(112);
      
    IPv6Address addr2 = new IPv6Address("::0000:0001:0000");
    assertTrue(addr2.isNetworkAddress(mask));
    
    IPv6Address addr3 = new IPv6Address("::0000:0000:1000");
    assertFalse(addr3.isNetworkAddress(mask));
  }
  
  
  @Test
  public void test9() throws FormatException, ValidationException {    
    IPv6NetmaskData mask = new IPv6NetmaskData(112);
      
    IPv6Address addr2 = new IPv6Address("::0000:ffff");
    assertTrue(addr2.isBroadcastAddress(mask));
    
    IPv6Address addr3 = new IPv6Address("::0000:0000:7fff");
    assertFalse(addr3.isBroadcastAddress(mask));
    
    IPv6Address addr4 = new IPv6Address("::0000:fffe");
    assertFalse(addr4.isBroadcastAddress(mask));
  }
  
  
  @Test
  public void test10() throws FormatException, ValidationException {    
    IPv6NetmaskData mask = new IPv6NetmaskData(112);
      
    IPv6Address addr2 = new IPv6Address("::0000:0001:0000");
    assertFalse(addr2.isGatewayAddress(mask));
    
    IPv6Address addr3 = new IPv6Address("::0000:0001");
    assertTrue(addr3.isGatewayAddress(mask));
    
    IPv6Address addr4 = new IPv6Address("::0000:0002");
    assertFalse(addr4.isGatewayAddress(mask));
  }
  
  
  @Test
  public void test11() throws FormatException, ValidationException {
    IPv6Address addr2 = new IPv6Address("::0000:0001:0010");
    IPv6Address addr3 = new IPv6Address("::0001:10");
    assertTrue(addr2.equals(addr2));
    assertTrue(addr2.equals(addr3));
  }
  
  @Test
  public void test12() throws FormatException, ValidationException {
    IPv6Address addr = new IPv6Address("::0000");
    assertTrue(addr.isUnspecifiedAddress());
    
    IPv6Address addr2 = new IPv6Address("::");
    IPv6Address addr3 = new IPv6Address(BigInteger.ZERO);
    assertTrue(addr.equals(addr2));
    assertTrue(addr3.equals(addr2));
  }
  
  @Test
  public void test13() throws FormatException, ValidationException {
    IPv6Address addr = new IPv6Address("::0001");
    assertTrue(addr.isLocalLoopback());
  }
  
  
  @Test
  public void test14() throws FormatException, ValidationException {
    IPv6Address addr = new IPv6Address("2002:0102:0304::");
    _logger.info("test 14: " + addr.convertToV4Address().toDotDecimalString());
    assertTrue("1.2.3.4".equals(addr.convertToV4Address().toDotDecimalString()));
  }
  
  
  @Test
  public void test15() throws FormatException, ValidationException {
    IPv6Address addr = new IPv6Address("2002:ff0f:f0f1::");
    _logger.info("test 15: " + addr.convertToV4Address().toDotDecimalString());
    assertTrue("255.15.240.241".equals(addr.convertToV4Address().toDotDecimalString()));
  }
  
  
  @Test
  public void test16() throws FormatException, ValidationException {
    IPv4Address addrV4 = new IPv4Address("255.15.240.241");
    IPv6Address addr = IPv6Address.fromV4Address(addrV4);
    _logger.info("test 16: " + addr.toShortHexRepresentation());
    assertTrue("2002:ff0f:f0f1::".equals(addr.toShortHexRepresentation()));
  }
  
  @Test
  public void test17() throws FormatException, ValidationException {
    IPv4Address addrV4 = new IPv4Address("4.3.2.5");
    IPv6Address addr = IPv6Address.fromV4Address(addrV4);
    _logger.info("test 17: " + addr.toShortHexRepresentation());
    assertTrue("2002:403:205::".equals(addr.toShortHexRepresentation()));
  }
  
}
