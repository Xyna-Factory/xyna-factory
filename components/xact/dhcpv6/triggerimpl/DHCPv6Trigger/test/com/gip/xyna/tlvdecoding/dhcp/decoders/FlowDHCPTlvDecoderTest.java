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
package com.gip.xyna.tlvdecoding.dhcp.decoders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.AbstractDHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.FlowDHCPv6TlvDecoder;




/**
 * Tests octet string DHCP TLV decoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class FlowDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulNormalUse() {
        AbstractDHCPv6TlvDecoder decoder = new FlowDHCPv6TlvDecoder(123, "Test");
        assertEquals("Test:A.BCDEFGHIJKLMNOPQRSTUVWXYZ", decoder.decode(new Tlv(123,
                TestHelper.toByteList("0x0001411942434445464748494A4B4C4D4E4F505152535455565758595A00"))));
    }


    @Test (expected = IllegalArgumentException.class)
    public void testFailsWithMissingZeroBeginning() {
      AbstractDHCPv6TlvDecoder decoder = new FlowDHCPv6TlvDecoder(123, "Test");
      assertEquals("Test:C.EF", decoder.decode(new Tlv(123,
              TestHelper.toByteList("0x014302454600"))));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWithMissingZeroAtEnd() {
      AbstractDHCPv6TlvDecoder decoder = new FlowDHCPv6TlvDecoder(123, "Test");
      assertEquals("Test:c.ef", decoder.decode(new Tlv(123,
              TestHelper.toByteList("0x000143024546"))));
    }

    
    @Test (expected = IllegalArgumentException.class)
    public void testFailsWithInsaneLengthValue() {
      AbstractDHCPv6TlvDecoder decoder = new FlowDHCPv6TlvDecoder(123, "Test");
      assertEquals("Test:C.EF", decoder.decode(new Tlv(123,
              TestHelper.toByteList("0x00FF4302454600"))));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWithSlightlyOffLengthValues() {
      AbstractDHCPv6TlvDecoder decoder = new FlowDHCPv6TlvDecoder(123, "Test");
      assertEquals("Test:C.EF", decoder.decode(new Tlv(123,
              TestHelper.toByteList("0x00024301454600"))));
    }


    @Test (expected = IllegalArgumentException.class)
    public void testFailsWithAnotherSlightlyOffLengthValue() {
      AbstractDHCPv6TlvDecoder decoder = new FlowDHCPv6TlvDecoder(123, "Test");
      assertEquals("Test:C.EF", decoder.decode(new Tlv(123,
              TestHelper.toByteList("0x00014301450100"))));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWithLowerCaseChars() {
      AbstractDHCPv6TlvDecoder decoder = new FlowDHCPv6TlvDecoder(123, "Test");
      assertEquals("Test:c.ef", decoder.decode(new Tlv(123,
              TestHelper.toByteList("0x00016302656600"))));
    }

}
