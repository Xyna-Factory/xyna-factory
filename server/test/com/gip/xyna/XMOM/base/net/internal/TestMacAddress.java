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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.MACAddressValidationException;


public class TestMacAddress {
  
  private static Logger _logger = Logger.getLogger(TestMacAddress.class);
  
  
  @Test
  public void test1() throws FormatException {
    assertTrue(MACAddressData.COLON_SEP_PATTERN.matcher("aa:bb:cc:dd:ee:ff").matches());
    assertFalse(MACAddressData.COLON_SEP_PATTERN.matcher("aa:bb:cc:dd:ee:ff1").matches());
    assertTrue(MACAddressData.COLON_SEP_PATTERN.matcher("1a:bb:9c:30:ee:f2").matches());
    assertFalse(MACAddressData.COLON_SEP_PATTERN.matcher("aa:bb:cc:dd:ee:ff:").matches());
    assertFalse(MACAddressData.COLON_SEP_PATTERN.matcher(":aa:bb:cc:dd:ee:ff").matches());
    assertFalse(MACAddressData.COLON_SEP_PATTERN.matcher("00:aa:bb:cc:dd:ee:ff").matches());
    assertFalse(MACAddressData.COLON_SEP_PATTERN.matcher("1aa:bb:cc:dd:ee:ff").matches());
    assertFalse(MACAddressData.COLON_SEP_PATTERN.matcher("aa:bb:cc:dd:ee:gg").matches());
  }
  
  @Test
  public void test2() throws FormatException {
    assertTrue(MACAddressData.HYPHEN_SEP_PATTERN.matcher("aa-bb-cc-dd-ee-ff").matches());
    assertFalse(MACAddressData.HYPHEN_SEP_PATTERN.matcher("aa-bb-cc-dd-ee-ff1").matches());
    assertTrue(MACAddressData.HYPHEN_SEP_PATTERN.matcher("1a-bb-9c-30-ee-f2").matches());
    assertFalse(MACAddressData.HYPHEN_SEP_PATTERN.matcher("aa-bb-cc-dd-ee-ff-").matches());
    assertFalse(MACAddressData.HYPHEN_SEP_PATTERN.matcher("-aa-bb-cc-dd-ee-ff").matches());
    assertFalse(MACAddressData.HYPHEN_SEP_PATTERN.matcher("00-aa-bb-cc-dd-ee-ff").matches());
    assertFalse(MACAddressData.HYPHEN_SEP_PATTERN.matcher("1aa-bb-cc-dd-ee-ff").matches());
    assertFalse(MACAddressData.HYPHEN_SEP_PATTERN.matcher("aa-bb-cc-dd-ee-gg").matches());
  }
  
  @Test
  public void test3() throws FormatException {
    assertTrue(MACAddressData.DOT_SEP_PATTERN.matcher("aabb.ccdd.eeff").matches());
    assertFalse(MACAddressData.DOT_SEP_PATTERN.matcher("aabb.ccdd.eeff1").matches());
    assertFalse(MACAddressData.DOT_SEP_PATTERN.matcher("aabbc.cdd.eeff").matches());
    assertFalse(MACAddressData.DOT_SEP_PATTERN.matcher("aabb.ccdd.ee.ff").matches());
    assertTrue(MACAddressData.DOT_SEP_PATTERN.matcher("1abb.cc22.9ef0").matches());
  }
  
  @Test
  public void test4() throws FormatException, MACAddressValidationException {
    MACAddressData mac = new MACAddressData("aa-bb-cc-dd-ee-ff");
    _logger.info("test 4: " + mac.getColonSeparated());
    assertTrue("aa:bb:cc:dd:ee:ff".equals(mac.getColonSeparated()));
  }
  
  @Test
  public void test5() throws FormatException, MACAddressValidationException {
    MACAddressData mac = new MACAddressData("a1:b2:c3:d4:e5:f6");
    _logger.info("test 5: " + mac.getDotSeparated());
    assertTrue("a1b2.c3d4.e5f6".equals(mac.getDotSeparated()));
  }
  
  @Test
  public void test6() throws FormatException, MACAddressValidationException {
    MACAddressData mac = new MACAddressData("A1:b2:C3:d4:e5:F6");
    _logger.info("test 6: " + mac.getDotSeparated());
    assertTrue("a1b2.c3d4.e5f6".equals(mac.getDotSeparated()));
  }
  
  @Test
  public void test7() throws FormatException, MACAddressValidationException {
    MACAddressData mac = new MACAddressData("A1b2.C3d4.e5F6");
    _logger.info("test 7: " + mac.getHyphenSeparated());
    assertTrue("a1-b2-c3-d4-e5-f6".equals(mac.getHyphenSeparated()));
  }
  
}
