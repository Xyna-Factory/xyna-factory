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


package com.gip.xyna.XMOM.base.net.internal;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.NetworkNotMatchesNetmaskException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;


public class TestIPv6Subnet {

  private static Logger _logger = Logger.getLogger(TestIPv6Netmask.class);
  

  @Test
  public void test1() throws FormatException, ValidationException {
    IPv6Address addr = new IPv6Address("0001::");
    IPv6NetmaskData mask = new IPv6NetmaskData(15);
    try {
      new IPv6SubnetData(addr, mask);
    }
    catch (NetworkNotMatchesNetmaskException e) {
      return;
    }
    fail("Expected exception");    
  }
  
  
  @Test
  public void test2() throws FormatException, ValidationException {
    IPv6Address addr = new IPv6Address("0001::");
    IPv6NetmaskData mask = new IPv6NetmaskData(16);
    IPv6SubnetData subnet = new IPv6SubnetData(addr, mask);
    assertTrue(subnet.getNetmask().getLength() == 16);
  }
  
  
  @Test
  public void test3() throws FormatException, ValidationException {
    IPv6Address addr1 = new IPv6Address("0001::");
    IPv6NetmaskData mask = new IPv6NetmaskData(16);
    IPv6SubnetData subnet = new IPv6SubnetData(addr1, mask);
    
    IPv6Address addr2 = new IPv6Address("0001:1000::");
    assertTrue(subnet.ipWithinSubnet(addr2));
    
    IPv6Address addr3 = new IPv6Address("0002:0000::");
    assertFalse(subnet.ipWithinSubnet(addr3));
    
    IPv6Address addr4 = new IPv6Address("0001:f000::");
    assertTrue(subnet.ipWithinSubnet(addr4));
    
    IPv6Address addr5 = new IPv6Address("0000:ffff::");
    assertFalse(subnet.ipWithinSubnet(addr5));
  }
  
  
  @Test
  public void test4() throws FormatException, ValidationException {    
    IPv6NetmaskData mask = new IPv6NetmaskData(112);
    IPv6Address addr = new IPv6Address("::1001:0000");
      
    IPv6SubnetData subnet = new IPv6SubnetData(addr, mask);
    IPv6Address gateway = subnet.getGatewayAddress();
    _logger.info("test 4: " + gateway.toShortHexRepresentation());
    assertTrue("::1001:1".equals(gateway.toShortHexRepresentation()));
    
    IPv6Address addr2 = new IPv6Address("::0000:1001:0001");
    _logger.info(addr2.toShortHexRepresentation());
    assertTrue(addr2.equals(gateway));    
  }
  
  
  @Test
  public void test5() throws FormatException, ValidationException {    
    IPv6NetmaskData mask = new IPv6NetmaskData(112);
    IPv6Address addr = new IPv6Address("::1001:0000");
      
    IPv6SubnetData subnet = new IPv6SubnetData(addr, mask);
    IPv6Address broadcast = subnet.getBroadcastAddress();
    _logger.info("test 5: " + broadcast.toShortHexRepresentation());
    assertTrue("::1001:ffff".equals(broadcast.toShortHexRepresentation()));
    
    IPv6Address addr2 = new IPv6Address("::0000:1001:ffff");
    _logger.info(addr2.toShortHexRepresentation());
    assertTrue(addr2.equals(broadcast));
  }
  
  
}
