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
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IgnoreValueDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.IgnoreValueDHCPv6TlvDecoder;




/**
 * Tests factory for DHCP TLV decoder that ignores value.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class IgnoreValueDHCPTlvDecoderFactoryTest {

    @Test
    public void testSuccessfulNormalUse() {
        AbstractDHCPv6TlvDecoderFactory decoderFactory = new IgnoreValueDHCPv6TlvDecoderFactory("Foo");
        assertEquals("Foo", decoderFactory.getDataTypeName());
        decoderFactory = new IgnoreValueDHCPv6TlvDecoderFactory("Bar");
        assertEquals("Bar", decoderFactory.getDataTypeName());

        DHCPv6TlvDecoder decoder = decoderFactory.create(123, "Baz", new ArrayList<DHCPv6TlvDecoder>(),null);
        assertTrue(decoder instanceof IgnoreValueDHCPv6TlvDecoder);
        assertEquals(123, decoder.getTypeEncoding());
        assertEquals("Baz", decoder.getTypeName());

        decoder = decoderFactory.create(234, "Qux", new ArrayList<DHCPv6TlvDecoder>(),null);
        assertEquals(234, decoder.getTypeEncoding());
        assertEquals("Qux", decoder.getTypeName());
    }
}
