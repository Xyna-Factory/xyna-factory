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

import java.util.ArrayList;

import org.junit.Test;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.AbstractDHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.MacAddressDHCPv6TlvDecoder;




/**
 * Test MAC address DHCP TLV decoder.
 */
public final class MacAddressDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulNormalUse() {
        AbstractDHCPv6TlvDecoder decoder = new MacAddressDHCPv6TlvDecoder(234, "Foo");
        assertEquals("Foo", decoder.decode(new Tlv(234, new ArrayList<Byte>())));
        assertEquals("Foo:00:00:00:00:00:00", decoder.decode(new Tlv(234, TestHelper.toByteList("0x000000000000"))));
        assertEquals("Foo:12:34:67:9A:BC:DE", decoder.decode(new Tlv(234, TestHelper.toByteList("0x1234679ABCDE"))));
        assertEquals("Foo:FF:FF:FF:FF:FF:FF", decoder.decode(new Tlv(234, TestHelper.toByteList("0xFFFFFFFFFFFF"))));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testDecodeFailsWithTooFewBytes() {
        new MacAddressDHCPv6TlvDecoder(234, "Foo").decode(new Tlv(234, TestHelper.toByteList("0x0000000000")));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testDecodeFailsWithTooManyBytes() {
        new MacAddressDHCPv6TlvDecoder(234, "Foo").decode(new Tlv(234, TestHelper.toByteList("0x00000000000000")));
    }
}
