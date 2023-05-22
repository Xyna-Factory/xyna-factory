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
package com.gip.xyna.tlvencoding.dhcp.encoderfactories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.AbstractTypeWithValueTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.IpV6AddressListTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.IpV6AddressListTlvEncoder;




/**
 * Tests IPv4 address list tlv encoder factory.
 */
public final class IpV6AddressListTlvEncoderFactoryTest {

    @Test
    public void testNormalUse() throws IOException {
        IpV6AddressListTlvEncoderFactory factory = new IpV6AddressListTlvEncoderFactory();
        assertTrue(factory instanceof AbstractTypeWithValueTlvEncoderFactory);
        TlvEncoder encoder = factory.create(234, new HashMap<String, String>(), new HashMap<String, TlvEncoder>());
        assertTrue(encoder instanceof IpV6AddressListTlvEncoder);
        TypeWithValueNode node = new TypeWithValueNode("Ips", "1:2:3:4:5:6:7:8,1:2:3:4:5:6:7:1");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(234);
        expectedResult.write(0);
        expectedResult.write(32);
        expectedResult.write(0);
        expectedResult.write(1);
        expectedResult.write(0);
        expectedResult.write(2);
        expectedResult.write(0);
        expectedResult.write(3);
        expectedResult.write(0);
        expectedResult.write(4);
        expectedResult.write(0);
        expectedResult.write(5);
        expectedResult.write(0);
        expectedResult.write(6);
        expectedResult.write(0);
        expectedResult.write(7);
        expectedResult.write(0);
        expectedResult.write(8);
        expectedResult.write(0);
        expectedResult.write(1);
        expectedResult.write(0);
        expectedResult.write(2);
        expectedResult.write(0);
        expectedResult.write(3);
        expectedResult.write(0);
        expectedResult.write(4);
        expectedResult.write(0);
        expectedResult.write(5);
        expectedResult.write(0);
        expectedResult.write(6);
        expectedResult.write(0);
        expectedResult.write(7);
        expectedResult.write(0);
        expectedResult.write(1);

        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithArgumentsNotEmpty() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("foo", "bar");
        new IpV6AddressListTlvEncoderFactory().create(123, arguments, new HashMap<String, TlvEncoder>());       
    }
}
