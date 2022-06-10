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
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.OctetStringTlvEncoder;



/**
 * Tests octet string TLV encoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class OctetStringTlvEncoderTest {

    @Test
    public void testEncodeOfHexadecimalValue() throws IOException {
        OctetStringTlvEncoder encoder = new OctetStringTlvEncoder(12, 8, 8);
        assertEquals(8, encoder.getMinLength());
        assertEquals(8, encoder.getMaxLength());
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);
        TypeWithValueNode node = new TypeWithValueNode("Bytes", "0x0123456789ABCDEF");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(12);
        expectedResult.write(0);
        expectedResult.write(8);
        expectedResult.write(1);
        expectedResult.write(35);
        expectedResult.write(69);
        expectedResult.write(103);
        expectedResult.write(137);
        expectedResult.write(171);
        expectedResult.write(205);
        expectedResult.write(239);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        encoder = new OctetStringTlvEncoder(56, 1, 254);
        assertEquals(1, encoder.getMinLength());
        assertEquals(254, encoder.getMaxLength());
        node = new TypeWithValueNode("Bytes", "0x00");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(56);
        expectedResult.write(0);
        expectedResult.write(1);
        expectedResult.write(0);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        node = new TypeWithValueNode("Bytes", "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(56);
        expectedResult.write(0);
        expectedResult.write(28);
        for (int i = 0; i < 28; ++i) {
            expectedResult.write(255);
        }
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test
    public void testEncodeOfStringValue() throws IOException {
        OctetStringTlvEncoder encoder = new OctetStringTlvEncoder(12, 1, 10);
        assertEquals(1, encoder.getMinLength());
        assertEquals(10, encoder.getMaxLength());
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);

        TypeWithValueNode node = new TypeWithValueNode("StringValue", "\"foo\"");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        assertEquals(TestHelper.createIntegerList(0,12, 0, 5, 34, 102, 111, 111,34), TestHelper.toUnsignedIntList(target));

        node = new TypeWithValueNode("StringValue", "\"\\\"\\\\\\\"\"");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        assertEquals(TestHelper.createIntegerList(0,12, 0, 8, 34, 92,34,92,92,92,34, 34), TestHelper.toUnsignedIntList(target));

        node = new TypeWithValueNode("StringValue", "\"צüוה\"");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        assertEquals(TestHelper.createIntegerList(0, 12, 0, 10, 34, 0xC3, 0xB6, 0xC3, 0xBC, 0xC3, 0xA5, 0xC3, 0xA4,34),
                TestHelper.toUnsignedIntList(target));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorWithMinLengthTooLow() {
        new OctetStringTlvEncoder(12, 0, 8);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorWithMaxLengthTooLarge() {
        new OctetStringTlvEncoder(12, 8, 70000);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorWithMinLengthLargerThanMaxLength() {
        new OctetStringTlvEncoder(12, 9, 8);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithBadValue() throws IOException {
        OctetStringTlvEncoder encoder = new OctetStringTlvEncoder(12, 8, 8);
        TypeWithValueNode node = new TypeWithValueNode("Bytes", "1234");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithValueLengthTooLarge() throws IOException {
        OctetStringTlvEncoder encoder = new OctetStringTlvEncoder(12, 8, 8);
        TypeWithValueNode node = new TypeWithValueNode("Bytes", "0x0123456789ABCDEFFF");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithValueLengthTooSmall() throws IOException {
        OctetStringTlvEncoder encoder = new OctetStringTlvEncoder(12, 8, 8);
        TypeWithValueNode node = new TypeWithValueNode("Bytes", "0x0123456789ABCD");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }
}
