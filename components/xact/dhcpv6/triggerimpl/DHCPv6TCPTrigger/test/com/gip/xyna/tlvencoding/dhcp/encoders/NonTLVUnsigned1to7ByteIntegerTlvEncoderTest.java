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
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.NonTLVUnsigned1to7ByteIntegerTlvEncoder;




/**
 * Tests unsigned 1 to 7 byte integer TLV encoder.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class NonTLVUnsigned1to7ByteIntegerTlvEncoderTest {

    @Test
    public void test4ByteUnsignedInteger() throws IOException {
        NonTLVUnsigned1to7ByteIntegerTlvEncoder encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(16, 4, null, null, null);
        assertEquals(16, encoder.getTypeEncoding());
        assertEquals(4, encoder.getNrBytes());
        assertEquals(0L, encoder.getMinValue());
        assertEquals(0xFFFFFFFFL, encoder.getMaxValue());
        assertEquals(1L, encoder.getMultipleOf());
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);
        TypeWithValueNode node = new TypeWithValueNode("Integer value", "123456");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(1);
        expectedResult.write(226);
        expectedResult.write(64);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(0, 4, 1L, 20L, 2L);
        assertEquals(0, encoder.getTypeEncoding());
        assertEquals(4, encoder.getNrBytes());
        assertEquals(1L, encoder.getMinValue());
        assertEquals(20L, encoder.getMaxValue());
        assertEquals(2L, encoder.getMultipleOf());
        node = new TypeWithValueNode("Integer value", "10");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(10);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(255, 4, 0L, 0L, 1L);
        assertEquals(255, encoder.getTypeEncoding());
        assertEquals(4, encoder.getNrBytes());
        assertEquals(0L, encoder.getMinValue());
        assertEquals(0L, encoder.getMaxValue());
        assertEquals(1L, encoder.getMultipleOf());
        node = new TypeWithValueNode("Integer value", "0");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(123, 4, 0xFFFFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFL);
        assertEquals(123, encoder.getTypeEncoding());
        assertEquals(4, encoder.getNrBytes());
        assertEquals(0xFFFFFFFFL, encoder.getMinValue());
        assertEquals(0xFFFFFFFFL, encoder.getMaxValue());
        assertEquals(0xFFFFFFFFL, encoder.getMultipleOf());
        node = new TypeWithValueNode("Integer value", Long.toString(0xFFFFFFFFL));
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(255);
        expectedResult.write(255);
        expectedResult.write(255);
        expectedResult.write(255);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test
    public void test1ByteUnsignedInteger() throws IOException {
        NonTLVUnsigned1to7ByteIntegerTlvEncoder encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(123, 1, null, null, null);
        assertEquals(123, encoder.getTypeEncoding());
        assertEquals(1, encoder.getNrBytes());
        assertEquals(0L, encoder.getMinValue());
        assertEquals(255L, encoder.getMaxValue());
        assertEquals(1L, encoder.getMultipleOf());
        TypeWithValueNode node = new TypeWithValueNode("Integer value", "234");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(234);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test
    public void test7ByteUnsignedInteger() throws IOException {
        NonTLVUnsigned1to7ByteIntegerTlvEncoder encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(145, 7, null, null, null);
        assertEquals(145, encoder.getTypeEncoding());
        assertEquals(7, encoder.getNrBytes());
        assertEquals(0L, encoder.getMinValue());
        assertEquals(0xFFFFFFFFFFFFFFL, encoder.getMaxValue());
        assertEquals(1L, encoder.getMultipleOf());
        TypeWithValueNode node = new TypeWithValueNode("Integer value", "234234123215212");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0x00);
        expectedResult.write(0xD5);
        expectedResult.write(0x08);
        expectedResult.write(0xE1);
        expectedResult.write(0xB5);
        expectedResult.write(0x61);
        expectedResult.write(0x6C);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test  (expected = IllegalArgumentException.class)
    public void testCreateWithNrBytesTooLow() {
        new NonTLVUnsigned1to7ByteIntegerTlvEncoder(16, 0, null, null, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithNrBytesTooHigh() {
        new NonTLVUnsigned1to7ByteIntegerTlvEncoder(16, 8, null, null, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithMinValueTooHigh() {
        new NonTLVUnsigned1to7ByteIntegerTlvEncoder(16, 4, (0xFFFFFFFFL + 1L), null, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithMinValueTooLow() {
        new NonTLVUnsigned1to7ByteIntegerTlvEncoder(16, 4, -1L, null, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithMaxValueTooHigh() {
        new NonTLVUnsigned1to7ByteIntegerTlvEncoder(16, 4, null, (0xFFFFFFFFL + 1L), null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithMaxValueTooLow() {
        new NonTLVUnsigned1to7ByteIntegerTlvEncoder(16, 4, null, -1L, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithMultipleOfTooHigh() {
        new NonTLVUnsigned1to7ByteIntegerTlvEncoder(16, 4, null, null, (0xFFFFFFFFL + 1L));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithMultipleOfTooLow() {
        new NonTLVUnsigned1to7ByteIntegerTlvEncoder(16, 4, null, null, 0L);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testParseOfNonIntegerValue() throws IOException {
        NonTLVUnsigned1to7ByteIntegerTlvEncoder encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(0, 4, 1L, 20L, 2L);
        TypeWithValueNode node = new TypeWithValueNode("Integer value", "foo");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testParseOfTooLowIntegerValue() throws IOException {
        NonTLVUnsigned1to7ByteIntegerTlvEncoder encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(16, 4, null, null, null);
        TypeWithValueNode node = new TypeWithValueNode("Integer value", "-1");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);    
    }

    @Test (expected = IllegalArgumentException.class)
    public void testParseOfTooHighIntegerValue() throws IOException {
        NonTLVUnsigned1to7ByteIntegerTlvEncoder encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(16, 4, null, null, null);
        TypeWithValueNode node = new TypeWithValueNode("Integer value", Long.toString(0xFFFFFFFFL + 1L));
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);        
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMinValueConstraint() throws IOException {
        NonTLVUnsigned1to7ByteIntegerTlvEncoder encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(0, 4, 1L, 20L, 2L);
        TypeWithValueNode node = new TypeWithValueNode("Integer value", "0");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMaxValueConstraint() throws IOException {
        NonTLVUnsigned1to7ByteIntegerTlvEncoder encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(0, 4, 1L, 20L, 2L);
        TypeWithValueNode node = new TypeWithValueNode("Integer value", "21");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMultipleOfConstraint() throws IOException {
        NonTLVUnsigned1to7ByteIntegerTlvEncoder encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(0, 4, 1L, 20L, 2L);
        TypeWithValueNode node = new TypeWithValueNode("Integer value", "3");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithOutOfImplementationRangeValue() throws IOException {
        NonTLVUnsigned1to7ByteIntegerTlvEncoder encoder = new NonTLVUnsigned1to7ByteIntegerTlvEncoder(145, 7, null, null, null);
        TypeWithValueNode node = new TypeWithValueNode("Integer value", "9223372036854775808");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }
}
