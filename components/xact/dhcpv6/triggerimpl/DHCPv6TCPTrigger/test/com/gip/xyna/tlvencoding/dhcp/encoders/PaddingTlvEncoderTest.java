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
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.AbstractTypeOnlyTlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.PaddingTlvEncoder;




/**
 * Tests padding TLV encoder.
 */
public final class PaddingTlvEncoderTest {

    @Test
    public void testNormalUse() throws IOException {
        PaddingTlvEncoder encoder = new PaddingTlvEncoder(0);
        assertTrue(encoder instanceof AbstractTypeOnlyTlvEncoder);
        Node node = new TypeOnlyNode("Padding", new ArrayList<Node>());
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        expectedResult.write(0);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        encoder = new PaddingTlvEncoder(255);
        node = new TypeOnlyNode("Padding", new ArrayList<Node>());
        target = new ByteArrayOutputStream();
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        expectedResult.write(255);
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithSubNodes() throws IOException {
        PaddingTlvEncoder encoder = new PaddingTlvEncoder(0);
        assertTrue(encoder instanceof AbstractTypeOnlyTlvEncoder);
        List<Node> subNodes = new ArrayList<Node>();
        subNodes.add(new TypeOnlyNode("Foo", new ArrayList<Node>()));
        Node node = new TypeOnlyNode("Padding", subNodes);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }
}
