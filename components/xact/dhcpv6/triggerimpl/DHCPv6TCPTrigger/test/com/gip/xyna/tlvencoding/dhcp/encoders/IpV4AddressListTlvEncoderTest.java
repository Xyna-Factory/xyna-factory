/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.IpV4AddressListTlvEncoder;




/**
 * Tests IPv4 address list tlv encoder.
 */
public final class IpV4AddressListTlvEncoderTest {

    @Test
    public void testNormalUse() throws IOException {
        IpV4AddressListTlvEncoder encoder = new IpV4AddressListTlvEncoder(234);
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);
        TypeWithValueNode node = new TypeWithValueNode("Ip addresses", "234.56.7.89,123.45.0.255,1.2.3.148");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(234);
        expectedResult.write(0);
        expectedResult.write(12);
        expectedResult.write(234);
        expectedResult.write(56);
        expectedResult.write(7);
        expectedResult.write(89);
        expectedResult.write(123);
        expectedResult.write(45);
        expectedResult.write(0);
        expectedResult.write(255);
        expectedResult.write(1);
        expectedResult.write(2);
        expectedResult.write(3);
        expectedResult.write(148);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        node = new TypeWithValueNode("Ip addresses", "1.0.255.148");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(234);
        expectedResult.write(0);
        expectedResult.write(4);
        expectedResult.write(1);
        expectedResult.write(0);
        expectedResult.write(255);
        expectedResult.write(148);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        node = new TypeWithValueNode("Ip addresses", "");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(234);
        expectedResult.write(0);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));        
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWriteWithInvalidValue() throws IOException {
        IpV4AddressListTlvEncoder encoder = new IpV4AddressListTlvEncoder(42);
        TypeWithValueNode node = new TypeWithValueNode("Ip addresses", "foo");
        encoder.write(node, new ByteArrayOutputStream());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWriteWithIpAddressContainingOutOfRangeValue() throws IOException {
        IpV4AddressListTlvEncoder encoder = new IpV4AddressListTlvEncoder(42);
        TypeWithValueNode node = new TypeWithValueNode("Ip addresses", "234.56.256.89");
        encoder.write(node, new ByteArrayOutputStream());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWriteWithIpAddressContainingTooManyValues() throws IOException {
        IpV4AddressListTlvEncoder encoder = new IpV4AddressListTlvEncoder(42);
        TypeWithValueNode node = new TypeWithValueNode("Ip addresses", "234.56.7.89.1");
        encoder.write(node, new ByteArrayOutputStream());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWriteWithIpAddressContainingTooFewValues() throws IOException {
        IpV4AddressListTlvEncoder encoder = new IpV4AddressListTlvEncoder(42);
        TypeWithValueNode node = new TypeWithValueNode("Ip addresses", "234.56.7");
        encoder.write(node, new ByteArrayOutputStream());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWriteWithIpAddressContainingJustElementSeparators() throws IOException {
        IpV4AddressListTlvEncoder encoder = new IpV4AddressListTlvEncoder(42);
        TypeWithValueNode node = new TypeWithValueNode("Ip addresses", "...");
        encoder.write(node, new ByteArrayOutputStream());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWriteWithIpAddressListContainingJustSeparators() throws IOException {
        IpV4AddressListTlvEncoder encoder = new IpV4AddressListTlvEncoder(42);
        TypeWithValueNode node = new TypeWithValueNode("Ip addresses", ",,,");
        encoder.write(node, new ByteArrayOutputStream());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFailsWriteWithIpAddressListContainingMissingValues() throws IOException {
        IpV4AddressListTlvEncoder encoder = new IpV4AddressListTlvEncoder(42);
        TypeWithValueNode node = new TypeWithValueNode("Ip addresses", "234.56.7.89,,1.2.3.148");
        encoder.write(node, new ByteArrayOutputStream());
    }
}
