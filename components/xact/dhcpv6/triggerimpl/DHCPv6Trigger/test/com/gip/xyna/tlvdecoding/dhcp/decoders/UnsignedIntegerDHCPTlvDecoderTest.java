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
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.UnsignedIntegerDHCPv6TlvDecoder;




/**
 * Tests unsigned integer DHCP TLV decoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class UnsignedIntegerDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulNormalUse() {
        AbstractDHCPv6TlvDecoder decoder = new UnsignedIntegerDHCPv6TlvDecoder(234, "Foo");
        assertEquals("Foo", decoder.decode(new Tlv(234, new ArrayList<Byte>())));
        assertEquals("Foo:0", decoder.decode(new Tlv(234, TestHelper.toByteList("0x00"))));
        assertEquals("Foo:0", decoder.decode(new Tlv(234, TestHelper.toByteList("0x00000000"))));
        assertEquals("Foo:255", decoder.decode(new Tlv(234, TestHelper.toByteList("0xFF"))));
        assertEquals("Foo:65535", decoder.decode(new Tlv(234, TestHelper.toByteList("0xFFFF"))));
        assertEquals("Foo:16777215", decoder.decode(new Tlv(234, TestHelper.toByteList("0xFFFFFF"))));
        assertEquals("Foo:4294967295", decoder.decode(new Tlv(234, TestHelper.toByteList("0xFFFFFFFF"))));
        assertEquals("Foo:1099511627775", decoder.decode(new Tlv(234, TestHelper.toByteList("0xFFFFFFFFFF"))));
        assertEquals("Foo:281474976710655", decoder.decode(new Tlv(234, TestHelper.toByteList("0xFFFFFFFFFFFF"))));
        assertEquals("Foo:72057594037927935", decoder.decode(new Tlv(234, TestHelper.toByteList("0xFFFFFFFFFFFFFF"))));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsDecodeFailsWithValueWithTooManyBytes() {
        new UnsignedIntegerDHCPv6TlvDecoder(234, "Foo").decode(new Tlv(234,
                TestHelper.toByteList("0x00FFFFFFFFFFFFFF")));
    }
}
