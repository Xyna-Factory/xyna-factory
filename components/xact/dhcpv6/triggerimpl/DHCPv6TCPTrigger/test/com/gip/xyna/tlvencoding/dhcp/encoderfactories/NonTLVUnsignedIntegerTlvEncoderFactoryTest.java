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
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.NonTLVUnsignedIntegerTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.NonTLVUnsigned1to7ByteIntegerTlvEncoder;



/**
 * Tests unsigned integer TLV encoder factory.
 */
public final class NonTLVUnsignedIntegerTlvEncoderFactoryTest {

    @Test
    public void testCreationOf4ByteNonTLVUnsignedInteger() throws IOException {
        NonTLVUnsignedIntegerTlvEncoderFactory factory = new NonTLVUnsignedIntegerTlvEncoderFactory();
        assertTrue(factory instanceof AbstractTypeWithValueTlvEncoderFactory);
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("nrBytes", "4");
        TlvEncoder encoder = factory.create(123, arguments, new HashMap<String, TlvEncoder>());
        assertTrue(encoder instanceof NonTLVUnsigned1to7ByteIntegerTlvEncoder);
        assertEquals(123, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getTypeEncoding());
        assertEquals(4, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getNrBytes());
        assertEquals(0L, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getMinValue());
        assertEquals(0xFFFFFFFFL, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getMaxValue());
        assertEquals(1L, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getMultipleOf());
        TypeWithValueNode node = new TypeWithValueNode("Unsigned integer", "10794");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(42);
        expectedResult.write(42);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        arguments = new HashMap<String, String>();
        arguments.put("nrBytes", "4");
        arguments.put("minValue", "5");
        arguments.put("maxValue", "11");
        arguments.put("multipleOf", "2");
        encoder = factory.create(245, arguments, new HashMap<String, TlvEncoder>());
        assertTrue(encoder instanceof NonTLVUnsigned1to7ByteIntegerTlvEncoder);
        assertEquals(245, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getTypeEncoding());
        assertEquals(4, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getNrBytes());
        assertEquals(5L, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getMinValue());
        assertEquals(11L, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getMaxValue());
        assertEquals(2L, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getMultipleOf());
        node = new TypeWithValueNode("Unsigned integer", "8");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(8);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test
    public void testCreationOf1ByteNonTLVUnsignedInteger() throws IOException {
        NonTLVUnsignedIntegerTlvEncoderFactory factory = new NonTLVUnsignedIntegerTlvEncoderFactory();
        assertTrue(factory instanceof AbstractTypeWithValueTlvEncoderFactory);
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("nrBytes", "1");
        TlvEncoder encoder = factory.create(42, arguments, new HashMap<String, TlvEncoder>());
        assertTrue(encoder instanceof NonTLVUnsigned1to7ByteIntegerTlvEncoder);
        assertEquals(42, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getTypeEncoding());
        assertEquals(1, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getNrBytes());
        assertEquals(0L, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getMinValue());
        assertEquals(0xFFL, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getMaxValue());
        assertEquals(1L, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getMultipleOf());
        TypeWithValueNode node = new TypeWithValueNode("Unsigned integer", "135");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(135);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test
    public void testCreationOf7ByteNonTLVUnsignedInteger() throws IOException {
        NonTLVUnsignedIntegerTlvEncoderFactory factory = new NonTLVUnsignedIntegerTlvEncoderFactory();
        assertTrue(factory instanceof AbstractTypeWithValueTlvEncoderFactory);
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("nrBytes", "7");
        arguments.put("minValue", "42");
        arguments.put("maxValue", "4242424242424242");
        TlvEncoder encoder = factory.create(91, arguments, new HashMap<String, TlvEncoder>());
        assertTrue(encoder instanceof NonTLVUnsigned1to7ByteIntegerTlvEncoder);
        assertEquals(91, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getTypeEncoding());
        assertEquals(7, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getNrBytes());
        assertEquals(42L, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getMinValue());
        assertEquals(4242424242424242L, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getMaxValue());
        assertEquals(1L, ((NonTLVUnsigned1to7ByteIntegerTlvEncoder) encoder).getMultipleOf());
        TypeWithValueNode node = new TypeWithValueNode("Unsigned integer", "9876543210");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0x02);
        expectedResult.write(0x4C);
        expectedResult.write(0xB0);
        expectedResult.write(0x16);
        expectedResult.write(0xEA);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithNoArguments() {
        new NonTLVUnsignedIntegerTlvEncoderFactory().create(123, new HashMap<String, String>(), new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithUnsupportedNrBytesArgument() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("nrBytes", "42");
        new NonTLVUnsignedIntegerTlvEncoderFactory().create(123, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithBadNrBytesArgument() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("nrBytes", "foo");
        new NonTLVUnsignedIntegerTlvEncoderFactory().create(123, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithBadMinValueArgument() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("nrBytes", "4");
        arguments.put("minValue", "foo");
        new NonTLVUnsignedIntegerTlvEncoderFactory().create(123, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithBadMaxValueArgument() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("nrBytes", "4");
        arguments.put("maxValue", "foo");
        new NonTLVUnsignedIntegerTlvEncoderFactory().create(123, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithBadMultipleOfArgument() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("nrBytes", "4");
        arguments.put("multipleOf", "foo");
        new NonTLVUnsignedIntegerTlvEncoderFactory().create(123, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithUnknownArgument() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("nrBytes", "4");
        arguments.put("foo", "bar");
        new NonTLVUnsignedIntegerTlvEncoderFactory().create(123, arguments, new HashMap<String, TlvEncoder>());
    }
}
