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
package com.gip.xyna.tlvdecoding.dhcp.decoders;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.AbstractDHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.ContainerDHCPv6TlvDecoder;



/**
 * Tests container DHCP TLV decoder.
 */
public class ContainerDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulDecodeWithoutSubTlvDecoders() {
        AbstractDHCPv6TlvDecoder decoder = new ContainerDHCPv6TlvDecoder(123, "Container",
                new ArrayList<DHCPv6TlvDecoder>());
        assertEquals("Container", decoder.decode(new Tlv(123, new ArrayList<Byte>())));
        assertEquals("Container\n  Tlv:1:OctetString:0x01", decoder.decode(new Tlv(123,
                TestHelper.toByteList("0x0001000101"))));
        assertEquals("Container\n  Tlv:1:OctetString:0x01\n  Tlv:2:OctetString:foo", decoder.decode(new Tlv(123,
                TestHelper.toByteList("0x000100010100020003666F6F"))));
    }

    @Test
    public void testSuccessfulDecodeWithSubTlvDecoders() {
        List<DHCPv6TlvDecoder> subTlvDecoders = new ArrayList<DHCPv6TlvDecoder>();
        subTlvDecoders.add(new MockDHCPTlvDecoder(123, "Foo"));
        subTlvDecoders.add(new MockDHCPTlvDecoder(124, "Bar"));
        subTlvDecoders.add(new ContainerDHCPv6TlvDecoder(125, "Baz", new ArrayList<DHCPv6TlvDecoder>()));
        AbstractDHCPv6TlvDecoder decoder = new ContainerDHCPv6TlvDecoder(0, "Container", subTlvDecoders);

        StringBuilder expectedResult = new StringBuilder();
        expectedResult.append("Container\n");
        expectedResult.append("  Bar:1\n");
        expectedResult.append("  Foo:2\n");
        expectedResult.append("  Baz\n");
        expectedResult.append("    Tlv:1:OctetString:0x03");
        assertEquals(expectedResult.toString(), decoder.decode(new Tlv(0, TestHelper.toByteList(
                "0x007C000101007B0002AAAA007D00050001000103"))));
    }
// Kein padding mehr
//    @Test
//    public void testSuccessfulDecodeOfPadding() {
//        List<DHCPv6TlvDecoder> subTlvDecoders = new ArrayList<DHCPv6TlvDecoder>();
//        subTlvDecoders.add(new PaddingDHCPv6TlvDecoder(123, "Foo"));
//        subTlvDecoders.add(new MockDHCPTlvDecoder(124, "Bar"));
//        subTlvDecoders.add(new ContainerDHCPv6TlvDecoder(125, "Baz", new ArrayList<DHCPv6TlvDecoder>()));
//        AbstractDHCPv6TlvDecoder decoder = new ContainerDHCPv6TlvDecoder(0, "Container", subTlvDecoders);
//
//        StringBuilder expectedResult = new StringBuilder();
//        expectedResult.append("Container\n");
//        expectedResult.append("  Bar:1\n");
//        expectedResult.append("  Foo\n");
//        expectedResult.append("  Baz\n");
//        expectedResult.append("    Tlv:1:OctetString:0x03");
//        assertEquals(expectedResult.toString(), decoder.decode(new Tlv(0, TestHelper.toByteList(
//                "0x007C000101007B007D00050001000103"))));
//    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsConstructorWithSubTlvDecodersNull() {
        new ContainerDHCPv6TlvDecoder(0, "Container", null);
    }

    private final class MockDHCPTlvDecoder extends AbstractDHCPv6TlvDecoder {

        public MockDHCPTlvDecoder(int typeEncoding, String typeName) {
            super(typeEncoding, typeName);
        }

        @Override
        protected String decodeTlvValue(byte[] value) {
            return Integer.toString(value.length);
        }
    }
}
