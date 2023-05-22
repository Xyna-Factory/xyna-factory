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

import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.AbstractDHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.ContainerDHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.EContainerDHCPv6TlvDecoder;



/**
 * Tests container DHCP TLV decoder.
 */
public class EContainerDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulDecodeWithoutSubTlvDecoders() {
        AbstractDHCPv6TlvDecoder decoder = new EContainerDHCPv6TlvDecoder(17, "EContainer",
                new ArrayList<DHCPv6TlvDecoder>());
        
        List<Byte> content = new ArrayList<Byte>();
        assertEquals("EContainer", decoder.decode(new Tlv(17, new ArrayList<Byte>())));
        
        content.add((byte)0); // enterprisenr: 4491
        content.add((byte)0);
        content.add((byte)17);
        content.add((byte)139);
        
        content.add((byte)0); // 0 TLV
        content.add((byte)1); // 1
        content.add((byte)0); // 0 Laenge
        content.add((byte)1); // 1
        //content.add((byte)48); // 0 Wert
        content.add((byte)1); // 1
        
        assertEquals("EContainer\n  Tlv:1:OctetString:0x01", decoder.decode(new Tlv(17,
                content)));

        content.add((byte)0); 
        content.add((byte)2); // 2
        content.add((byte)0); // 3
        content.add((byte)3); // 3
        content.add((byte)102); // f
        content.add((byte)111); // o
        content.add((byte)111); // o

        
        assertEquals("EContainer\n  Tlv:1:OctetString:0x01\n  Tlv:2:OctetString:foo", decoder.decode(new Tlv(17,
                content)));
                
    }

    
    @Test
    public void testSuccessfulDecodeWithSubTlvDecoders() {
        List<DHCPv6TlvDecoder> subTlvDecoders = new ArrayList<DHCPv6TlvDecoder>();
        subTlvDecoders.add(new MockDHCPTlvDecoder(123, "Foo"));
        subTlvDecoders.add(new MockDHCPTlvDecoder(124, "Bar"));
        subTlvDecoders.add(new EContainerDHCPv6TlvDecoder(17, "Baz", new ArrayList<DHCPv6TlvDecoder>()));
        AbstractDHCPv6TlvDecoder decoder = new ContainerDHCPv6TlvDecoder(0, "Container", subTlvDecoders);

        StringBuilder expectedResult = new StringBuilder();
        expectedResult.append("Container\n");
        expectedResult.append("  Bar:1\n");
        expectedResult.append("  Foo:2\n");
        expectedResult.append("  Baz\n");
        expectedResult.append("    Tlv:1:OctetString:0x03");

        List<Byte> content = new ArrayList<Byte>();
        
        //0x7C01017B02AAAA7D 03010103 "0x7C01017B7D03010103

        content.add((byte)0);
        content.add((byte)124);
        content.add((byte)0);
        content.add((byte)1);
        content.add((byte)1);
        
        content.add((byte)0);
        content.add((byte)123);
        content.add((byte)0);
        content.add((byte)2);
        content.add((byte)170);
        content.add((byte)170);
        
        
        content.add((byte)0);
        content.add((byte)17);
        content.add((byte)0);
        content.add((byte)9);
        
        
                
        content.add((byte)0); // enterprisenr: 4491
        content.add((byte)0);
        content.add((byte)17);
        content.add((byte)139);
        
        content.add((byte)0); // 1
        content.add((byte)1); // 1
        content.add((byte)0); // 1
        content.add((byte)1); // 1
        content.add((byte)3); // 3

        
        
        assertEquals(expectedResult.toString(), decoder.decode(new Tlv(0, content)));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsConstructorWithSubTlvDecodersNull() {
        new EContainerDHCPv6TlvDecoder(0, "Container", null);
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
