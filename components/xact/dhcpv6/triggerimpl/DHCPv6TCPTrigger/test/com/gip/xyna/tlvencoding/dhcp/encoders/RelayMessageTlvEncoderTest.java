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
package com.gip.xyna.tlvencoding.dhcp.encoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.AbstractTypeOnlyTlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.RelayMessageTlvEncoder;




/**
 * Tests container tlv encoder.
 */
public final class RelayMessageTlvEncoderTest {

    @Test
    public void testNormalUse() throws IOException {
        RelayMessageTlvEncoder encoder = new RelayMessageTlvEncoder(234, new HashMap<String, TlvEncoder>());
        assertTrue(encoder instanceof AbstractTypeOnlyTlvEncoder);
        TypeOnlyNode node = new TypeOnlyNode("Foo", new ArrayList<Node>());
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(234);
        expectedResult.write(0);
        expectedResult.write(0);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        Map<String, TlvEncoder> subNodeEncodings = new HashMap<String, TlvEncoder>();
        TlvEncoder tlvEncoder = EasyMock.createMock(TlvEncoder.class);
        EasyMock.replay(tlvEncoder);
        subNodeEncodings.put("Bar", tlvEncoder);
        encoder = new RelayMessageTlvEncoder(255, subNodeEncodings);
        node = new TypeOnlyNode("Baz", new ArrayList<Node>());
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(255);
        expectedResult.write(0);
        expectedResult.write(0);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        subNodeEncodings = new HashMap<String, TlvEncoder>();
        Node subNode = new TypeWithValueNode("Bar", "Qux");
        tlvEncoder = new MockTlvEncoderImpl();
        subNodeEncodings.put("Bar", tlvEncoder);
        encoder = new RelayMessageTlvEncoder(0, subNodeEncodings);
        List<Node> subNodes = new ArrayList<Node>();
        subNodes.add(subNode);
        subNodes.add(subNode);
        node = new TypeOnlyNode("Baz", subNodes);
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(0);
        expectedResult.write(8);
        expectedResult.write(42);
        expectedResult.write(2);
        expectedResult.write(4);
        expectedResult.write(2);
        expectedResult.write(42);
        expectedResult.write(2);
        expectedResult.write(4);
        expectedResult.write(2);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test
    public void testEncodeWithMaxContent() throws IOException {
        Map<String, TlvEncoder> subNodeEncodings = new HashMap<String, TlvEncoder>();
        Node subNode = new TypeWithValueNode("Bar", "Qux");
        Node padNode = new TypeOnlyNode("Pad", new ArrayList<Node>());
        subNodeEncodings.put("Bar", new MockTlvEncoderImpl());
        subNodeEncodings.put("Pad", new MockTlvEncoderImpl(true));
        RelayMessageTlvEncoder encoder = new RelayMessageTlvEncoder(0, subNodeEncodings);
        List<Node> subNodes = new ArrayList<Node>();
        for (int i = 0; i < 63; ++i) {
            subNodes.add(subNode);
        }
        subNodes.add(padNode);
        subNodes.add(padNode);
        TypeOnlyNode node = new TypeOnlyNode("Baz", subNodes);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        assertEquals(258, target.size());
    }

    private final class MockTlvEncoderImpl implements TlvEncoder {

        private final boolean isPadding;

        public MockTlvEncoderImpl() {
            isPadding = false;
        }

        public MockTlvEncoderImpl(final boolean isPadding) {
            this.isPadding = isPadding;
        }

        public void write(final Node node, final OutputStream target) throws IOException {
            if (isPadding) {
                assertTrue(node instanceof TypeOnlyNode);
                target.write(0);
            } else {
                assertTrue(node instanceof TypeWithValueNode);
                TypeWithValueNode n = (TypeWithValueNode) node;
                assertEquals("Bar", n.getTypeName());
                assertEquals("Qux", n.getValue());
                target.write(42);
                target.write(2);
                target.write(4);
                target.write(2);
            }
        }

        public int getTypeEncoding() {
            return 0;
        }
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithSubNodeEncodingsNull() {
        new RelayMessageTlvEncoder(234, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithSubNodeEncodingsContainingNullDataTypeName() {
        Map<String, TlvEncoder> subNodeEncodings = new HashMap<String, TlvEncoder>();
        TlvEncoder tlvEncoder = EasyMock.createMock(TlvEncoder.class);
        EasyMock.replay(tlvEncoder);
        subNodeEncodings.put(null, tlvEncoder);
        new RelayMessageTlvEncoder(255, subNodeEncodings);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithSubNodeEncodingsContainingEmptyStringDataTypeName() {
        Map<String, TlvEncoder> subNodeEncodings = new HashMap<String, TlvEncoder>();
        TlvEncoder tlvEncoder = EasyMock.createMock(TlvEncoder.class);
        EasyMock.replay(tlvEncoder);
        subNodeEncodings.put("", tlvEncoder);
        new RelayMessageTlvEncoder(255, subNodeEncodings);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithSubNodeEncodingsContainingNullEncoder() {
        Map<String, TlvEncoder> subNodeEncodings = new HashMap<String, TlvEncoder>();
        subNodeEncodings.put("Bar", null);
        new RelayMessageTlvEncoder(255, subNodeEncodings);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testEncodeWithUnknownSubType() throws IOException {
        RelayMessageTlvEncoder encoder = new RelayMessageTlvEncoder(234, new HashMap<String, TlvEncoder>());
        List<Node> subNodes = new ArrayList<Node>();
        subNodes.add(new TypeWithValueNode("Bar", "Qux"));
        Node node = new TypeOnlyNode("Baz", subNodes);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testEncodeWithTooMuchSubNodeData() throws IOException {
        Map<String, TlvEncoder> subNodeEncodings = new HashMap<String, TlvEncoder>();
        Node subNode = new TypeWithValueNode("Bar", "Qux");
        Node padNode = new TypeOnlyNode("Pad", new ArrayList<Node>());
        subNodeEncodings.put("Bar", new MockTlvEncoderImpl());
        subNodeEncodings.put("Pad", new MockTlvEncoderImpl(true));
        RelayMessageTlvEncoder encoder = new RelayMessageTlvEncoder(0, subNodeEncodings);
        List<Node> subNodes = new ArrayList<Node>();
        for (int i = 0; i < 60000; ++i) {
            subNodes.add(subNode);
        }
        subNodes.add(padNode);
        subNodes.add(padNode);
        subNodes.add(padNode);
        TypeOnlyNode node = new TypeOnlyNode("Baz", subNodes);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }
}
