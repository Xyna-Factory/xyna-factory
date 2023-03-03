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
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.AbstractTypeWithValueTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.MacAddressTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.MacAddressTlvEncoder;




/**
 * Tests MAC address TLV encoder factory.
 */
public final class MacAddressTlvEncoderFactoryTest {

    @Test
    public void testNormalUse() throws IOException {
        MacAddressTlvEncoderFactory factory = new MacAddressTlvEncoderFactory();
        assertTrue(factory instanceof AbstractTypeWithValueTlvEncoderFactory);
        TlvEncoder encoder = factory.create(222, new HashMap<String, String>(), new HashMap<String, TlvEncoder>());
        assertTrue(encoder instanceof MacAddressTlvEncoder);
        TypeWithValueNode node = new TypeWithValueNode("MAC address", "12:34:56:78:90:AF");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(222);
        expectedResult.write(0);
        expectedResult.write(6);
        expectedResult.write(0x12);
        expectedResult.write(0x34);
        expectedResult.write(0x56);
        expectedResult.write(0x78);
        expectedResult.write(0x90);
        expectedResult.write(0xAF);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithArgumentsNotEmpty() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("foo", "bar");
        new MacAddressTlvEncoderFactory().create(123, arguments, new HashMap<String, TlvEncoder>());       
    }
}
