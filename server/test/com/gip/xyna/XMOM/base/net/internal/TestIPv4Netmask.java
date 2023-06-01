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

import org.apache.log4j.Logger;
import org.junit.Test;

import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;
import com.gip.xyna.XMOM.base.net.internal.IPv4Address;
import com.gip.xyna.XMOM.base.net.internal.IPv4NetmaskData;



public class TestIPv4Netmask {

  private static Logger _logger = Logger.getLogger(TestIPv4Netmask.class);


  @Test
  public void test1() throws FormatException, ValidationException {
    assertTrue(1 == IPv4NetmaskData.powBase2(0));
    assertTrue(2 == IPv4NetmaskData.powBase2(1));
    assertTrue(8 == IPv4NetmaskData.powBase2(3));
    assertTrue(1024 == IPv4NetmaskData.powBase2(10));
    assertTrue((long) Math.pow(2, 10) == IPv4NetmaskData.powBase2(10));
    assertTrue((long) Math.pow(2, 31) == IPv4NetmaskData.powBase2(31));
    assertTrue((long) Math.pow(2, 32) == IPv4NetmaskData.powBase2(32));
  }


  @Test
  public void test2() throws FormatException, ValidationException {
    long tmp = IPv4NetmaskData.powBase2(31) + IPv4NetmaskData.powBase2(30) + 
                    IPv4NetmaskData.powBase2(29) + IPv4NetmaskData.powBase2(28);
    _logger.debug("2^31 + 2^30 + 2^29 + 2^28 = " + tmp);
    IPv4NetmaskData mask = new IPv4NetmaskData(4);
    _logger.debug("netmask len 1 = " + mask.getIPv4Address().getAsLong());
    assertTrue(mask.getIPv4Address().getAsLong() == tmp);
  }

  @Test
  public void test3() throws FormatException, ValidationException {
    IPv4NetmaskData mask = new IPv4NetmaskData(32);
    _logger.debug("netmask len 32 = " + mask.getIPv4Address().getAsLong());
    assertTrue(mask.getIPv4Address().getAsLong() == IPv4Address.Constant.MAX_IP);
  }

  @Test
  public void test4() throws FormatException, ValidationException {
    IPv4NetmaskData mask = new IPv4NetmaskData(30);
    _logger.debug("netmask len 30 = " + mask.getIPv4Address().getAsLong());
    assertTrue(mask.getIPv4Address().getAsLong() == IPv4Address.Constant.MAX_IP - 3);

    long inverted = mask.invertIPAsLong(mask.getIPv4Address().getAsLong());
    assertTrue(inverted == 3);
  }

  @Test
  public void test5() throws FormatException, ValidationException {
    IPv4Address ip = new IPv4Address(IPv4Address.Constant.MAX_IP);
    IPv4NetmaskData mask = new IPv4NetmaskData(ip);
    assertTrue(32 == mask.getLength());
  }

  @Test
  public void test6() throws FormatException, ValidationException {
    IPv4Address ip = new IPv4Address(IPv4Address.Constant.MAX_IP - 7);
    IPv4NetmaskData mask = new IPv4NetmaskData(ip);
    assertTrue(29 == mask.getLength());
  }

  @Test
  public void test7() throws FormatException, ValidationException {
    IPv4Address ip = new IPv4Address(IPv4NetmaskData.powBase2(31) + IPv4NetmaskData.powBase2(30));
    IPv4NetmaskData mask = new IPv4NetmaskData(ip);
    _logger.debug("test7: netmask len = " +  mask.getLength());
    assertTrue(2 == mask.getLength());
  }


  @Test
  public void test8() throws FormatException, ValidationException {
    IPv4Address ip = new IPv4Address(IPv4NetmaskData.powBase2(31) + IPv4NetmaskData.powBase2(10));
    try {
      new IPv4NetmaskData(ip);
    }
    catch (Exception e) {
      assertTrue(e instanceof ValidationException);
      _logger.debug("test8: " + e.getMessage());
      return;
    }
    fail();
  }

  
  @Test
  public void test9() throws FormatException, ValidationException {
    IPv4NetmaskData mask = new IPv4NetmaskData("/29");
    _logger.debug("test 9: netmask len 29 = " + mask.getIPv4Address().toDotDecimalString());
    _logger.debug("test 9: netmask len = " + mask.getLength());
    assertTrue(mask.getIPv4Address().getAsLong() == IPv4Address.Constant.MAX_IP - 7);
    assertTrue(mask.getLength() == 29);
    
    long inverted = mask.invertIPAsLong(mask.getIPv4Address().getAsLong());
    assertTrue(inverted == 7);
  }
  
  
  @Test
  public void test10() throws FormatException, ValidationException {
    IPv4NetmaskData mask = new IPv4NetmaskData("29");
    _logger.debug("test 10: netmask len 29 = " + mask.getIPv4Address().toZeroPaddedDotDecimalString());
    _logger.debug("test 10: netmask len = " + mask.getLength());
    assertTrue(mask.getIPv4Address().getAsLong() == IPv4Address.Constant.MAX_IP - 7);
    assertTrue(mask.getLength() == 29);
    
    long inverted = mask.invertIPAsLong(mask.getIPv4Address().getAsLong());
    assertTrue(inverted == 7);
  }
  
  @Test
  public void test11() throws FormatException, ValidationException {
    IPv4NetmaskData mask = new IPv4NetmaskData("255.255.255.254");
    _logger.debug("test 11: netmask len 31 = " + mask.getIPv4Address().toZeroPaddedDotDecimalString());
    _logger.debug("test 11: netmask len = " + mask.getLength());
    assertTrue(mask.getIPv4Address().getAsLong() == IPv4Address.Constant.MAX_IP - 1);
    assertTrue(mask.getLength() == 31);
    
    long inverted = mask.invertIPAsLong(mask.getIPv4Address().getAsLong());
    assertTrue(inverted == 1);
  }
  
}
