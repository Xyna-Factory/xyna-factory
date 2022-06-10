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

import java.util.ArrayList;

import org.junit.Test;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.AbstractDHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.IpV6AddressDHCPv6TlvDecoder;



/**
 * Tests IPv4 address DHCP TLV decoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class IpV6AddressDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulNormalUse() {
        AbstractDHCPv6TlvDecoder decoder = new IpV6AddressDHCPv6TlvDecoder(234, "Foo");
        assertEquals("Foo", decoder.decode(new Tlv(234, new ArrayList<Byte>())));
        assertEquals("Foo:0:0:0:0:0:0:0:0", decoder.decode(new Tlv(234, TestHelper.toByteList("0x00000000000000000000000000000000"))));
        assertEquals("Foo:1:0:0:0:0:0:0:2", decoder.decode(new Tlv(234, TestHelper.toByteList("0x00010000000000000000000000000002"))));
        assertEquals("Foo:ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", decoder.decode(new Tlv(234, TestHelper.toByteList("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"))));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testDecodeFailsWithTooFewBytes() {
        new IpV6AddressDHCPv6TlvDecoder(234, "Foo").decode(new Tlv(234, TestHelper.toByteList("0x000000")));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testDecodeFailsWithTooManyBytes() {
        new IpV6AddressDHCPv6TlvDecoder(234, "Foo").decode(new Tlv(234, TestHelper.toByteList("0x0000000000")));
    }


}
