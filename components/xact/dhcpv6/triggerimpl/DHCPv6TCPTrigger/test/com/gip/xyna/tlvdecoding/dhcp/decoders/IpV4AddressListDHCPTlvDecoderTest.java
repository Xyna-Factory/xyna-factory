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
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.IpV4AddressListDHCPv6TlvDecoder;




/**
 * Tests IPv4 address list DHCP TLV decoder.
 */
public final class IpV4AddressListDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulNormalUse() {
        AbstractDHCPv6TlvDecoder decoder = new IpV4AddressListDHCPv6TlvDecoder(234, "Foo");
        assertEquals("Foo", decoder.decode(new Tlv(234, new ArrayList<Byte>())));
        assertEquals("Foo:0.0.0.0", decoder.decode(new Tlv(234, TestHelper.toByteList("0x00000000"))));
        assertEquals("Foo:192.168.1.10", decoder.decode(new Tlv(234, TestHelper.toByteList("0xC0A8010A"))));
        assertEquals("Foo:255.255.255.255", decoder.decode(new Tlv(234, TestHelper.toByteList("0xFFFFFFFF"))));
        assertEquals("Foo:255.255.255.255,0.0.0.0,170.170.170.170", decoder.decode(new Tlv(234,
                TestHelper.toByteList("0xFFFFFFFF00000000AAAAAAAA"))));
        assertEquals("Foo", decoder.decode(new Tlv(234, new ArrayList<Byte>())));        
    }

    @Test (expected = IllegalArgumentException.class)
    public void testDecodeFailsWithMoreThanZeroAndLessThanFourBytes() {
        new IpV4AddressListDHCPv6TlvDecoder(234, "Foo").decode(new Tlv(234, TestHelper.toByteList("0x000000")));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testDecodeFailsWithIncorrectNumberOfBytesOverFour() {
        new IpV4AddressListDHCPv6TlvDecoder(234, "Foo").decode(new Tlv(234,
                TestHelper.toByteList("0x000000000000000000")));
    }
}
