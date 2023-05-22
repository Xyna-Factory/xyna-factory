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
package com.gip.xyna.tlvencoding.dhcp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoderFactoryProvider;




/**
 * Tests TLV encoder factory provider.
 */
public final class TlvEncoderFactoryProviderTest {

    @Test
    public void testNormalUse() {
        assertNull(TlvEncoderFactoryProvider.get(null));
        assertNull(TlvEncoderFactoryProvider.get(""));
        assertNotNull(TlvEncoderFactoryProvider.get("Container"));
        assertNotNull(TlvEncoderFactoryProvider.get("IpV4Address"));
        assertNotNull(TlvEncoderFactoryProvider.get("IpV4AddressList"));
        assertNotNull(TlvEncoderFactoryProvider.get("UnsignedInteger"));
        assertNotNull(TlvEncoderFactoryProvider.get("MacAddress"));
        assertNotNull(TlvEncoderFactoryProvider.get("OctetString"));
        assertNotNull(TlvEncoderFactoryProvider.get("Disallowed"));
        assertNotNull(TlvEncoderFactoryProvider.get("Padding"));
        assertNotNull(TlvEncoderFactoryProvider.get("EndOfDataMarker"));
    }
}
