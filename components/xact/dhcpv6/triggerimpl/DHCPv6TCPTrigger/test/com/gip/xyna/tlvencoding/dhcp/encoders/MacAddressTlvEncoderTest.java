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
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.MacAddressTlvEncoder;




/**
 * Tests MAC address TLV encoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class MacAddressTlvEncoderTest {

    @Test
    public void testNormalUse() throws IOException {
        MacAddressTlvEncoder encoder = new MacAddressTlvEncoder(255);
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);
        TypeWithValueNode node = new TypeWithValueNode("MAC address", "AB:8D:12:09:EF:46");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(255);
        expectedResult.write(0);
        expectedResult.write(6);
        expectedResult.write(171);
        expectedResult.write(141);
        expectedResult.write(18);
        expectedResult.write(9);
        expectedResult.write(239);
        expectedResult.write(70);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        encoder = new MacAddressTlvEncoder(0);
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);
        node = new TypeWithValueNode("MAC address", "34:0F:78:56:11:CF");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(6);
        expectedResult.write(52);
        expectedResult.write(15);
        expectedResult.write(120);
        expectedResult.write(86);
        expectedResult.write(17);
        expectedResult.write(207);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        encoder = new MacAddressTlvEncoder(247);
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);
        node = new TypeWithValueNode("MAC address", "FF:FF:FF:FF:FF:FF");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(247);
        expectedResult.write(0);
        expectedResult.write(6);
        expectedResult.write(255);
        expectedResult.write(255);
        expectedResult.write(255);
        expectedResult.write(255);
        expectedResult.write(255);
        expectedResult.write(255);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        encoder = new MacAddressTlvEncoder(111);
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);
        node = new TypeWithValueNode("MAC address", "00:00:00:00:00:00");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(111);
        expectedResult.write(0);
        expectedResult.write(6);
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithCompletelyWrongMacAddressValue() throws IOException {
        MacAddressTlvEncoder encoder = new MacAddressTlvEncoder(255);
        TypeWithValueNode node = new TypeWithValueNode("MAC address", "foo");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithMacAddressContainingBadElement() throws IOException {
        MacAddressTlvEncoder encoder = new MacAddressTlvEncoder(255);
        TypeWithValueNode node = new TypeWithValueNode("MAC address", "AB:8D:12:gg:EF:46");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithTooManyMacAddressElements() throws IOException {
        MacAddressTlvEncoder encoder = new MacAddressTlvEncoder(255);
        TypeWithValueNode node = new TypeWithValueNode("MAC address", "AB:8D:12:09:EF:46:42");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithTooFewMacAddressElements() throws IOException {
        MacAddressTlvEncoder encoder = new MacAddressTlvEncoder(255);
        TypeWithValueNode node = new TypeWithValueNode("MAC address", "AB:8D:12:09:EF");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }
}
