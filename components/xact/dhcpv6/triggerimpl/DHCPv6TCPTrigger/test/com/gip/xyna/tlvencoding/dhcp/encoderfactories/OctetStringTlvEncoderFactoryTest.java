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
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.AbstractTypeWithValueTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.OctetStringTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.OctetStringTlvEncoder;



/**
 * Tests octet string TLV encoder factory.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class OctetStringTlvEncoderFactoryTest {

    @Test
    public void testNormalUse() throws IOException {
        TlvEncoderFactory factory = new OctetStringTlvEncoderFactory();
        assertTrue(factory instanceof AbstractTypeWithValueTlvEncoderFactory);
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("minLength", "1");
        arguments.put("maxLength", "254");
        TlvEncoder encoder = factory.create(29, arguments, new HashMap<String, TlvEncoder>());
        assertTrue(encoder instanceof OctetStringTlvEncoder);
        assertEquals(1, ((OctetStringTlvEncoder) encoder).getMinLength());
        assertEquals(254, ((OctetStringTlvEncoder) encoder).getMaxLength());
        TypeWithValueNode node = new TypeWithValueNode("Bytes", "0xF00BAA");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(29);
        expectedResult.write(0);
        expectedResult.write(3);
        expectedResult.write(240);
        expectedResult.write(11);
        expectedResult.write(170);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        arguments = new HashMap<String, String>();
        arguments.put("minLength", "5");
        arguments.put("maxLength", "30");
        encoder = factory.create(253, arguments, new HashMap<String, TlvEncoder>());
        assertEquals(5, ((OctetStringTlvEncoder) encoder).getMinLength());
        assertEquals(30, ((OctetStringTlvEncoder) encoder).getMaxLength());
        node = new TypeWithValueNode("Bytes", "0x1234C0FFEE");
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(253);
        expectedResult.write(0);
        expectedResult.write(5);
        expectedResult.write(18);
        expectedResult.write(52);
        expectedResult.write(192);
        expectedResult.write(255);
        expectedResult.write(238);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test
    public void testCreateWithMinLengthMissing() {
        TlvEncoderFactory factory = new OctetStringTlvEncoderFactory();
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("maxLength", "123");
        OctetStringTlvEncoder encoder = (OctetStringTlvEncoder) factory.create(29, arguments,
                new HashMap<String, TlvEncoder>());
        assertEquals(1, encoder.getMinLength());
        assertEquals(123, encoder.getMaxLength());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithBadMinLengthValue() {
        TlvEncoderFactory factory = new OctetStringTlvEncoderFactory();
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("minLength", "foo");
        arguments.put("maxLength", "254");
        factory.create(29, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithTooSmallMinLengthValue() {
        TlvEncoderFactory factory = new OctetStringTlvEncoderFactory();
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("minLength", "0");
        arguments.put("maxLength", "254");
        factory.create(29, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithTooLargeMinLengthValue() {
        TlvEncoderFactory factory = new OctetStringTlvEncoderFactory();
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("minLength", "255");
        arguments.put("maxLength", "254");
        factory.create(29, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test
    public void testCreateWithMaxLengthMissing() {
        TlvEncoderFactory factory = new OctetStringTlvEncoderFactory();
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("minLength", "1");
        OctetStringTlvEncoder encoder = (OctetStringTlvEncoder) factory.create(29, arguments,
                new HashMap<String, TlvEncoder>());
        assertEquals(1, encoder.getMinLength());
        assertEquals(256*256, encoder.getMaxLength());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithBadMaxLengthValue() {
        TlvEncoderFactory factory = new OctetStringTlvEncoderFactory();
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("minLength", "1");
        arguments.put("maxLength", "bar");
        factory.create(29, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithTooSmallMaxLengthValue() {
        TlvEncoderFactory factory = new OctetStringTlvEncoderFactory();
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("minLength", "1");
        arguments.put("maxLength", "0");
        factory.create(29, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithTooLargeMaxLengthValue() {
        TlvEncoderFactory factory = new OctetStringTlvEncoderFactory();
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("minLength", "1");
        arguments.put("maxLength", "70000");
        factory.create(29, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithUnknownArgument() {
        TlvEncoderFactory factory = new OctetStringTlvEncoderFactory();
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("minLength", "1");
        arguments.put("maxLength", "254");
        arguments.put("foo", "bar");
        factory.create(29, arguments, new HashMap<String, TlvEncoder>());
    }
}
