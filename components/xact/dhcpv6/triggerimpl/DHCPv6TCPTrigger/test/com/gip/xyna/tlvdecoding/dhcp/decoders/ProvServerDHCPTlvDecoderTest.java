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
package com.gip.xyna.tlvdecoding.dhcp.decoders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.AbstractDHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.ProvServerDHCPv6TlvDecoder;




/**
 * Tests octet string DHCP TLV decoder.
 */
public final class ProvServerDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulNormalUse() {
        AbstractDHCPv6TlvDecoder decoder = new ProvServerDHCPv6TlvDecoder(123, "Test");
        assertEquals("Test:c.ef", decoder.decode(new Tlv(123,
                TestHelper.toByteList("0x00016302656600"))));
    }


    @Test (expected = IllegalArgumentException.class)
    public void testFailsWithMissingZeroBeginning() {
      AbstractDHCPv6TlvDecoder decoder = new ProvServerDHCPv6TlvDecoder(123, "Test");
      assertEquals("Test:c.ef", decoder.decode(new Tlv(123,
              TestHelper.toByteList("0x016302656600"))));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWithMissingZeroAtEnd() {
      AbstractDHCPv6TlvDecoder decoder = new ProvServerDHCPv6TlvDecoder(123, "Test");
      assertEquals("Test:c.ef", decoder.decode(new Tlv(123,
              TestHelper.toByteList("0x000163026566"))));
    }

    
    @Test (expected = IllegalArgumentException.class)
    public void testFailsWithInsaneLengthValue() {
      AbstractDHCPv6TlvDecoder decoder = new ProvServerDHCPv6TlvDecoder(123, "Test");
      assertEquals("Test:c.ef", decoder.decode(new Tlv(123,
              TestHelper.toByteList("0x00FF6302656600"))));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWithSlightlyOffLengthValues() {
      AbstractDHCPv6TlvDecoder decoder = new ProvServerDHCPv6TlvDecoder(123, "Test");
      assertEquals("Test:c.ef", decoder.decode(new Tlv(123,
              TestHelper.toByteList("0x00026301656600"))));
    }


    @Test (expected = IllegalArgumentException.class)
    public void testFailsWithAnotherSlightlyOffLengthValue() {
      AbstractDHCPv6TlvDecoder decoder = new ProvServerDHCPv6TlvDecoder(123, "Test");
      assertEquals("Test:c.ef", decoder.decode(new Tlv(123,
              TestHelper.toByteList("0x00016301650100"))));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWithUpperCaseChars() {
      AbstractDHCPv6TlvDecoder decoder = new ProvServerDHCPv6TlvDecoder(123, "Test");
      assertEquals("Test:C.EF", decoder.decode(new Tlv(123,
              TestHelper.toByteList("0x00014302454600"))));
    }

}
