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
import java.util.List;

import org.junit.Test;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.AbstractDHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.DUIDDHCPv6TlvDecoder;



/**
 * Tests container DHCP TLV decoder.
 */
public class DUIDDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulDecodeWithSubTlvDecoders() {
        List<DHCPv6TlvDecoder> subTlvDecoders = new ArrayList<DHCPv6TlvDecoder>();
        subTlvDecoders.add(new DUIDDHCPv6TlvDecoder(1, "A", new ArrayList<DHCPv6TlvDecoder>()));
        subTlvDecoders.add(new DUIDDHCPv6TlvDecoder(2, "B", new ArrayList<DHCPv6TlvDecoder>()));
        subTlvDecoders.add(new DUIDDHCPv6TlvDecoder(3, "C", new ArrayList<DHCPv6TlvDecoder>()));
        AbstractDHCPv6TlvDecoder decoder = new DUIDDHCPv6TlvDecoder(0, "DUID", subTlvDecoders);

        StringBuilder expectedResult = new StringBuilder();
        expectedResult.append("DUID\n");
        expectedResult.append("  A\n");
        expectedResult.append("    Tlv:1:OctetString:0x03");
        assertEquals(expectedResult.toString(), decoder.decode(new Tlv(0, TestHelper.toByteList(
                "0x0001000103"))));

        expectedResult = new StringBuilder();
        expectedResult.append("DUID\n");
        expectedResult.append("  B\n");
        expectedResult.append("    Tlv:1:OctetString:0x03");
        assertEquals(expectedResult.toString(), decoder.decode(new Tlv(0, TestHelper.toByteList(
                "0x0002000103"))));

        expectedResult = new StringBuilder();
        expectedResult.append("DUID\n");
        expectedResult.append("  C\n");
        expectedResult.append("    Tlv:1:OctetString:0x03");
        assertEquals(expectedResult.toString(), decoder.decode(new Tlv(0, TestHelper.toByteList(
                "0x0003000103"))));

    
    }
// Kein padding mehr
//    @Test
//    public void testSuccessfulDecodeOfPadding() {
//        List<DHCPv6TlvDecoder> subTlvDecoders = new ArrayList<DHCPv6TlvDecoder>();
//        subTlvDecoders.add(new PaddingDHCPv6TlvDecoder(123, "Foo"));
//        subTlvDecoders.add(new MockDHCPTlvDecoder(124, "Bar"));
//        subTlvDecoders.add(new DUIDDHCPv6TlvDecoder(125, "Baz", new ArrayList<DHCPv6TlvDecoder>()));
//        AbstractDHCPv6TlvDecoder decoder = new DUIDDHCPv6TlvDecoder(0, "DUID", subTlvDecoders);
//
//        StringBuilder expectedResult = new StringBuilder();
//        expectedResult.append("DUID\n");
//        expectedResult.append("  Bar:1\n");
//        expectedResult.append("  Foo\n");
//        expectedResult.append("  Baz\n");
//        expectedResult.append("    Tlv:1:OctetString:0x03");
//        assertEquals(expectedResult.toString(), decoder.decode(new Tlv(0, TestHelper.toByteList(
//                "0x007C000101007B007D00050001000103"))));
//    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsConstructorWithSubTlvDecodersNull() {
        new DUIDDHCPv6TlvDecoder(0, "DUID", null);
    }


}
