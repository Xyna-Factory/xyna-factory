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
package com.gip.xyna.tlvdecoding.dhcp;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvDecoderFactoryProvider;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.ContainerDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IgnoreValueDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IpV4AddressDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.IpV4AddressListDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.MacAddressDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.OctetStringDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.PaddingDHCPv6TlvDecoderFactory;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.decoderfactories.UnsignedIntegerDHCPv6TlvDecoderFactory;



/**
 * Tests DHCP TLV decoder factory provider.
 */
public final class DHCPTlvDecoderFactoryProviderTest {

    @Test
    public void testGetSuccessfullyReturnsCorrectValuesForAnyStringInput() {
        assertTrue(DHCPv6TlvDecoderFactoryProvider.get("OctetString") instanceof OctetStringDHCPv6TlvDecoderFactory);
        assertTrue(DHCPv6TlvDecoderFactoryProvider.get("Container") instanceof ContainerDHCPv6TlvDecoderFactory);
        assertTrue(DHCPv6TlvDecoderFactoryProvider.get("UnsignedInteger")
                instanceof UnsignedIntegerDHCPv6TlvDecoderFactory);
        assertTrue(DHCPv6TlvDecoderFactoryProvider.get("IpV4Address") instanceof IpV4AddressDHCPv6TlvDecoderFactory);
        assertTrue(DHCPv6TlvDecoderFactoryProvider.get("IpV4AddressList")
                instanceof IpV4AddressListDHCPv6TlvDecoderFactory);
        assertTrue(DHCPv6TlvDecoderFactoryProvider.get("CableModemMic") instanceof IgnoreValueDHCPv6TlvDecoderFactory);
        assertTrue(DHCPv6TlvDecoderFactoryProvider.get("CmtsMic") instanceof IgnoreValueDHCPv6TlvDecoderFactory);
        assertTrue(DHCPv6TlvDecoderFactoryProvider.get("MacAddress") instanceof MacAddressDHCPv6TlvDecoderFactory);
        assertTrue(DHCPv6TlvDecoderFactoryProvider.get("Padding") instanceof PaddingDHCPv6TlvDecoderFactory);
        assertTrue(DHCPv6TlvDecoderFactoryProvider.get("EndOfDataMarker") instanceof PaddingDHCPv6TlvDecoderFactory);
        assertTrue(DHCPv6TlvDecoderFactoryProvider.get("Disallowed") instanceof OctetStringDHCPv6TlvDecoderFactory);

        assertNull(DHCPv6TlvDecoderFactoryProvider.get("Unknown"));
        assertNull(DHCPv6TlvDecoderFactoryProvider.get("Foo"));
        assertNull(DHCPv6TlvDecoderFactoryProvider.get(""));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetFailsWithNull() {
        DHCPv6TlvDecoderFactoryProvider.get(null);
    }
}
