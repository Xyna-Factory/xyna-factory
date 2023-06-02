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
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.ContainerDHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.DUIDLLTDHCPv6TlvDecoder;



/**
 * Tests container DHCP TLV decoder.
 */
public class DUIDLLTDHCPTlvDecoderTest {


    @Test
    public void testSuccessfulDecodeWithSubTlvDecoders() {
        List<DHCPv6TlvDecoder> subTlvDecoders = new ArrayList<DHCPv6TlvDecoder>();
        subTlvDecoders.add(new MockDHCPTlvDecoder(1, "Foo"));
        subTlvDecoders.add(new MockDHCPTlvDecoder(2, "Bar"));
        subTlvDecoders.add(new MockDHCPTlvDecoder(3, "Baz"));
        //subTlvDecoders.add(new MockDHCPTlvDecoder(1, "Ban"));

        subTlvDecoders.add(new ContainerDHCPv6TlvDecoder(125, "Par", new ArrayList<DHCPv6TlvDecoder>()));
        
        
        AbstractDHCPv6TlvDecoder decoder = new DUIDLLTDHCPv6TlvDecoder(0, "DUIDLLT", subTlvDecoders);

        StringBuilder expectedResult = new StringBuilder();
        expectedResult.append("DUIDLLT\n");
        expectedResult.append("  Foo:2\n");
        expectedResult.append("  Bar:4\n");
        expectedResult.append("  Baz:4");

        assertEquals(expectedResult.toString(), decoder.decode(new Tlv(0, TestHelper.toByteList(
                "0x00001111111122222222"))));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsConstructorWithSubTlvDecodersNull() {
        new DUIDLLTDHCPv6TlvDecoder(0, "DUIDLLT", null);
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
