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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.PaddingTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.PaddingTlvEncoder;




/**
 * Padding TLV encoder factory.
 */
public final class PaddingTlvEncoderFactoryTest {

    @Test
    public void testNormalUse() throws IOException {
        PaddingTlvEncoderFactory factory = new PaddingTlvEncoderFactory();
        assertTrue(factory instanceof PaddingTlvEncoderFactory);
        TlvEncoder encoder = factory.create(111, new HashMap<String, String>(), new HashMap<String, TlvEncoder>());
        assertTrue(encoder instanceof PaddingTlvEncoder);
        TypeOnlyNode node = new TypeOnlyNode("Padding", new ArrayList<Node>());
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(111);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithArguments() {
        PaddingTlvEncoderFactory factory = new PaddingTlvEncoderFactory();
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("foo", "bar");
        factory.create(111, arguments, new HashMap<String, TlvEncoder>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithSubNodes() {
        PaddingTlvEncoderFactory factory = new PaddingTlvEncoderFactory();
        Map<String, TlvEncoder> encodings = new HashMap<String, TlvEncoder>();
        encodings.put("Baz", new PaddingTlvEncoder(123));
        factory.create(111, new HashMap<String, String>(), encodings);
    }
}
