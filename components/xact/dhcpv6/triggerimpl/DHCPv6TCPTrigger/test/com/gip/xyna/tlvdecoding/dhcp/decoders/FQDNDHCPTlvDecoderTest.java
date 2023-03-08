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

import org.junit.Test;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.AbstractDHCPv6TlvDecoder;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoders.FQDNDHCPv6TlvDecoder;




/**
 * Tests octet string DHCP TLV decoder.
 */
public final class FQDNDHCPTlvDecoderTest {

    @Test
    public void testSuccessfulNormalUse() {
        AbstractDHCPv6TlvDecoder decoder = new FQDNDHCPv6TlvDecoder(12,"Test");
        assertEquals("Test:http://www.testeinenlangentextzwischenpunkten.fertig", decoder.decode(new Tlv(12,
                TestHelper.toByteList("0x0A687474703A2F2F777777227465737465696E656E6C616E67656E746578747A7769736368656E70756E6B74656E0666657274696700"))));
    }
}
