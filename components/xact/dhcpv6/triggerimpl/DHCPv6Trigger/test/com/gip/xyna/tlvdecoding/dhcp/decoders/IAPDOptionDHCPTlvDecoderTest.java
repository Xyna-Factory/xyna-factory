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
import java.util.List;

import org.junit.Test;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.AbstractDHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.IAPDOptionDHCPv6TlvDecoder;



/**
 * Tests container DHCP TLV decoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public class IAPDOptionDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulDecodeWithoutSubTlvDecoders() {
        List<DHCPv6TlvDecoder> subTlvDecoders = new ArrayList<DHCPv6TlvDecoder>();
        subTlvDecoders.add(new MockDHCPTlvDecoder(100001, "Foo"));
        subTlvDecoders.add(new MockDHCPTlvDecoder(100002, "Bar"));
        subTlvDecoders.add(new MockDHCPTlvDecoder(100003, "Baz"));
        subTlvDecoders.add(new MockDHCPTlvDecoder(100004, "Bat"));

        //subTlvDecoders.add(new MockDHCPTlvDecoder(1, "Ban"));

        
        
        AbstractDHCPv6TlvDecoder decoder = new IAPDOptionDHCPv6TlvDecoder(0, "IAPDOption", subTlvDecoders);

        StringBuilder expectedResult = new StringBuilder();
        expectedResult.append("IAPDOption\n");
        expectedResult.append("  Foo:4\n");
        expectedResult.append("  Bar:4\n");
        expectedResult.append("  Baz:1\n");
        expectedResult.append("  Bat:16");
        assertEquals(expectedResult.toString(), decoder.decode(new Tlv(0, TestHelper.toByteList(
                "0x00000001000000020300000000000000000000000000000004"))));
    }


    @Test (expected = IllegalArgumentException.class)
    public void testFailsConstructorWithSubTlvDecodersNull() {
        new IAPDOptionDHCPv6TlvDecoder(0, "IAPDOption", null);
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
