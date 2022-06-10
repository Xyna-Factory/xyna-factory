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
package com.gip.xyna.tlvencoding.dhcp.encoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.AbstractTypeWithValueTlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.IpV4AddressTlvEncoder;



/**
 * Tests IPv4 address tlv encoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class IpV4AddressTlvEncoderTest {

    @Test
    public void testNormalUse() throws IOException {
        IpV4AddressTlvEncoder encoder = new IpV4AddressTlvEncoder(42);
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);
        TypeWithValueNode node = new TypeWithValueNode("Ip address", "234.56.7.89");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(42);
        expectedResult.write(0);
        expectedResult.write(4);
        expectedResult.write(234);
        expectedResult.write(56);
        expectedResult.write(7);
        expectedResult.write(89);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        encoder = new IpV4AddressTlvEncoder(255);
        node = new TypeWithValueNode("Ip address", "255.255.255.255");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(255);
        expectedResult.write(0);
        expectedResult.write(4);
        expectedResult.write(255);
        expectedResult.write(255);
        expectedResult.write(255);
        expectedResult.write(255);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        encoder = new IpV4AddressTlvEncoder(0);
        node = new TypeWithValueNode("Ip address", "0.0.0.0");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(4);
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithBadIpAddressValue() throws IOException {
        IpV4AddressTlvEncoder encoder = new IpV4AddressTlvEncoder(42);
        TypeWithValueNode node = new TypeWithValueNode("Ip address", "foo");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithIpAddressContainingOutOfRangeValue() throws IOException {
        IpV4AddressTlvEncoder encoder = new IpV4AddressTlvEncoder(42);
        TypeWithValueNode node = new TypeWithValueNode("Ip address", "234.56.256.89");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithIpAddressContainingTooManyValues() throws IOException {
        IpV4AddressTlvEncoder encoder = new IpV4AddressTlvEncoder(42);
        TypeWithValueNode node = new TypeWithValueNode("Ip address", "234.56.7.89.1");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithIpAddressContainingTooFewValues() throws IOException {
        IpV4AddressTlvEncoder encoder = new IpV4AddressTlvEncoder(42);
        TypeWithValueNode node = new TypeWithValueNode("Ip address", "234.56.7");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithIpAddressContainingJustSeparators() throws IOException {
        IpV4AddressTlvEncoder encoder = new IpV4AddressTlvEncoder(42);
        TypeWithValueNode node = new TypeWithValueNode("Ip address", "...");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }
}
