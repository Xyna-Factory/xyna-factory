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

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.NoFreeIPFoundException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;
import com.gip.xyna.XMOM.base.net.internal.IPv4Address;
import com.gip.xyna.XMOM.base.net.internal.IPv4NetmaskData;
import com.gip.xyna.XMOM.base.net.internal.IPv4SubnetData;


public class TestIPv4Subnet {

  private static Logger _logger = Logger.getLogger(TestIPv4Subnet.class);

  @Test
  public void test1() throws FormatException, ValidationException {
    try {
      new IPv4SubnetData(new IPv4Address(0x12345601L), new IPv4NetmaskData(24));
    }
    catch (Exception e) {
      //_logger.debug("test1: " + e.getMessage());
      assertTrue(e instanceof ValidationException);
      return;
    }
    fail();
  }


  @Test
  public void test2() throws FormatException, ValidationException {
    IPv4SubnetData sub = new IPv4SubnetData(new IPv4Address(0x12345600), new IPv4NetmaskData(24));
    assertFalse(sub.ipWithinSubnet(new IPv4Address(0x123455FFL)));
    assertTrue(sub.ipWithinSubnet(new IPv4Address(0x12345600L)));
    assertTrue(sub.ipWithinSubnet(new IPv4Address(0x12345601L)));
    assertTrue(sub.ipWithinSubnet(new IPv4Address(0x123456FFL)));
    assertFalse(sub.ipWithinSubnet(new IPv4Address(0x12345700L)));
  }

  @Test
  public void test3() throws FormatException, ValidationException {
    IPv4SubnetData sub = new IPv4SubnetData(new IPv4Address("192.168.5.0"), new IPv4NetmaskData(24));
    assertTrue(sub.ipWithinSubnet(new IPv4Address("192.168.5.130")));
  }

  @Test
  public void test4() throws FormatException, ValidationException {
    IPv4SubnetData sub = new IPv4SubnetData(new IPv4Address("192.168.5.0"), new IPv4NetmaskData(24));
    IPv4Address bc = sub.getBroadcastAddress();
    _logger.debug("broadcast address of 192.168.5.0/24: " + bc.toDotDecimalString());
    assertTrue(bc.getAsLong() == new IPv4Address("192.168.5.255").getAsLong());
  }

  @Test
  public void test5() throws FormatException, ValidationException {
    IPv4SubnetData sub = new IPv4SubnetData(new IPv4Address("192.168.5.0"), new IPv4NetmaskData(24));
    IPv4Address gw = sub.getGatewayAddress();
    _logger.debug("gateway address of 192.168.5.0: " + gw.toDotDecimalString());
    assertTrue(gw.getAsLong() == new IPv4Address("192.168.5.1").getAsLong());
  }

  
  @Test
  public void test6() throws FormatException, ValidationException {
    IPv4SubnetData sub = new IPv4SubnetData(new IPv4Address("192.168.5.0"), new IPv4NetmaskData(29));
    IPv4Address bc = sub.getBroadcastAddress();
    _logger.debug("broadcast address of 192.168.5.0/29: " + bc.toDotDecimalString());
    
    List<IPv4Address> list = sub.getAllIPsInSubnet();
    for (IPv4Address ip : list) {
      _logger.debug("test 6 list item: " + ip.toDotDecimalString());
    }
    assertTrue(list.size() == 6);
    assertTrue(list.get(0).equals(new IPv4Address("192.168.5.1")));
    assertTrue(list.get(5).equals(new IPv4Address("192.168.5.6")));
  }
  
  
  @Test
  public void test7() throws FormatException, ValidationException {
    IPv4SubnetData sub1 = new IPv4SubnetData(new IPv4Address("192.168.5.0"), new IPv4NetmaskData(29));
    IPv4SubnetData sub2 = new IPv4SubnetData(new IPv4Address("192.168.5.0"), new IPv4NetmaskData(28));
    
    List<IPv4Address> list = sub1.getAllIPsInSubnet();
    list.remove(3);
    IPv4Address freeIp = sub2.getNextFreeIP(list);
    _logger.debug("test 7: " + freeIp.toDotDecimalString());
    assertTrue(freeIp.equals(new IPv4Address("192.168.5.4")));
  }
  
  
  @Test
  public void test8() throws FormatException, ValidationException {
    IPv4SubnetData sub1 = new IPv4SubnetData(new IPv4Address("192.168.5.0"), new IPv4NetmaskData(29));
    IPv4SubnetData sub2 = new IPv4SubnetData(new IPv4Address("192.168.5.0"), new IPv4NetmaskData(28));
    
    List<IPv4Address> list = sub1.getAllIPsInSubnet();
    IPv4Address freeIp = sub2.getNextFreeIP(list);
    _logger.debug("test 8: " + freeIp.toDotDecimalString());
    assertTrue(freeIp.equals(new IPv4Address("192.168.5.7")));
  }
  
  
  @Test
  public void test9() throws FormatException, ValidationException {
    IPv4SubnetData sub1 = new IPv4SubnetData(new IPv4Address("192.168.5.0"), new IPv4NetmaskData(29));
    IPv4SubnetData sub2 = new IPv4SubnetData(new IPv4Address("192.168.5.0"), new IPv4NetmaskData(29));
    
    List<IPv4Address> list = sub1.getAllIPsInSubnet();
    try {
      sub2.getNextFreeIP(list);
    }
    catch (NoFreeIPFoundException e) {
      return;
    }
    fail("Expected exception");      
  }
  
  
}
