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
package com.gip.xyna.tlvdecoding.dhcp.decoderfactories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.AbstractDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.NonTLVIpV6AddressDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.NonTLVIpV6AddressDHCPv6TlvDecoder;




/**
 * Tests IPv4 address DHCP TLV decoder factory.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class NonTLVIpV6AddressDHCPTlvDecoderFactoryTest {

    @Test
    public void testSuccessfulNormalUse() {
        AbstractDHCPv6TlvDecoderFactory decoderFactory = new NonTLVIpV6AddressDHCPv6TlvDecoderFactory();
        assertEquals("NonTLVIpV6Address", decoderFactory.getDataTypeName());
        assertEquals(decoderFactory.getDataTypeName(), NonTLVIpV6AddressDHCPv6TlvDecoderFactory.DATA_TYPE_NAME);
        DHCPv6TlvDecoder decoder = decoderFactory.create(123, "Foo", new ArrayList<DHCPv6TlvDecoder>(),null);
        assertTrue(decoder instanceof NonTLVIpV6AddressDHCPv6TlvDecoder);
        assertEquals(123, decoder.getTypeEncoding());
        assertEquals("Foo", decoder.getTypeName());

        decoder = decoderFactory.create(234, "Bar", new ArrayList<DHCPv6TlvDecoder>(),null);
        assertEquals(234, decoder.getTypeEncoding());
        assertEquals("Bar", decoder.getTypeName());
    }
}
