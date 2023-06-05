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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.AbstractTypeOnlyTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.EContainerTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoderfactories.UnsignedIntegerTlvEncoderFactory;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.EContainerTlvEncoder;



/**
 * Tests container tvl encoder factory.
 */
public final class EContainerTlvEncoderFactoryTest {

    @Test
    public void testNormalUse() throws IOException {
        EContainerTlvEncoderFactory factory = new EContainerTlvEncoderFactory();
        assertTrue(factory instanceof AbstractTypeOnlyTlvEncoderFactory);
        
        HashMap<String,String> arguments = new HashMap<String, String>();
        arguments.put("enterprisenr", "4491");
        
        TlvEncoder encoder = factory.create(134, arguments, new HashMap<String, TlvEncoder>());
        assertTrue(encoder instanceof EContainerTlvEncoder);
        TypeOnlyNode node = new TypeOnlyNode("Foo", new ArrayList<Node>());
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();

        expectedResult.write(0); 
        expectedResult.write(134);
        expectedResult.write(0); 
        expectedResult.write(4); // length
        expectedResult.write(0); // 0
        expectedResult.write(0); // 0
        expectedResult.write(17);  // 17*256 +
        expectedResult.write(139); // 139      = 4491
        
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        
        UnsignedIntegerTlvEncoderFactory subfactory = new UnsignedIntegerTlvEncoderFactory();
        List<Node> subNodes = new ArrayList<Node>();
        Node subNode = new TypeWithValueNode("Bar", "2");
        subNodes.add(subNode);
        node = new TypeOnlyNode("Baz",subNodes);
        Map<String, TlvEncoder> subNodeEncodings = new HashMap<String, TlvEncoder>();
        
        HashMap<String,String> subarguments = new HashMap<String, String>();
        subarguments.put("nrBytes", "1");
        
        TlvEncoder subNodeEncoder = subfactory.create(1, subarguments,
                new HashMap<String, TlvEncoder>());
        subNodeEncodings.put("Bar", subNodeEncoder);
        target = new ByteArrayOutputStream();
        encoder = factory.create(243, arguments, subNodeEncodings);
        encoder.write(node, target);
        expectedResult = new ByteArrayOutputStream();
        
        expectedResult.write(0);
        expectedResult.write(243); // Mainnode
        expectedResult.write(0);
        expectedResult.write(9); // length
        expectedResult.write(0); // 0
        expectedResult.write(0); // 0
        expectedResult.write(17);  // 17*256 +
        expectedResult.write(139); // 139      = 4491
        
        expectedResult.write(0); // T
        expectedResult.write(1); // 
        expectedResult.write(0); // L
        expectedResult.write(1); // 
        expectedResult.write(2); // V
        
        
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithArgumentsEmpty() {
        Map<String, String> arguments = new HashMap<String, String>();
        new EContainerTlvEncoderFactory().create(123, arguments, new HashMap<String, TlvEncoder>());       
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithInvalidArguments() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("blibla","blub");
        new EContainerTlvEncoderFactory().create(123, arguments, new HashMap<String, TlvEncoder>());       
    }


}
