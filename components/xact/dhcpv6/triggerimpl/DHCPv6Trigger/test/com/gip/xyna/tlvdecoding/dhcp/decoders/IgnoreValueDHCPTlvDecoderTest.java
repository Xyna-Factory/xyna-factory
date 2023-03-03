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
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.IgnoreValueDHCPv6TlvDecoder;



/**
 * Tests DOCSIS TLV decoder that only outputs type name.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class IgnoreValueDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulNormalUse() {
        AbstractDHCPv6TlvDecoder decoder = new IgnoreValueDHCPv6TlvDecoder(123, "Foo");
        assertEquals("Foo", decoder.decode(new Tlv(123, new ArrayList<Byte>())));
        assertEquals("Foo", decoder.decode(new Tlv(123, TestHelper.toByteList("0x0123456789ABCDEF"))));
    }
}
