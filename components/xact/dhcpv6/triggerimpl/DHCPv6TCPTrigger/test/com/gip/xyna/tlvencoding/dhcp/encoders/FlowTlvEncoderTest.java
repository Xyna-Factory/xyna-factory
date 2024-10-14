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

import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.AbstractTypeWithValueTlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.FlowTlvEncoder;



/**
 * Tests octet string TLV encoder.
 */
public final class FlowTlvEncoderTest {

    @Test
    public void testEncodeOfValidDomainValue() throws IOException {
        FlowTlvEncoder encoder = new FlowTlvEncoder(6);
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);
        TypeWithValueNode node = new TypeWithValueNode("Flow", "BASIC.1");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();

        /* Wireshark
         06 0a 00 05 42 41 53 49 43 01 31 00
        */
        expectedResult.write((byte)0);
        expectedResult.write((byte)6);
        expectedResult.write((byte)0);
        expectedResult.write((byte)10);
        expectedResult.write((byte)0);
        expectedResult.write((byte)5);
        expectedResult.write((byte)66);
        expectedResult.write((byte)65);
        expectedResult.write((byte)83);
        expectedResult.write((byte)73);
        expectedResult.write((byte)67);
        expectedResult.write((byte)1);
        expectedResult.write((byte)49);
        expectedResult.write((byte)0);

        
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }

    // Noch unbekannt wie Optionen aufgebaut sein können
    
    /*
    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithBadValue() throws IOException {
        FlowTlvEncoder encoder = new FlowTlvEncoder(6);
        TypeWithValueNode node = new TypeWithValueNode("Flow", "0x3000");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    
    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithIncompleteDomainname() throws IOException {
        FlowTlvEncoder encoder = new FlowTlvEncoder(6);
        TypeWithValueNode node = new TypeWithValueNode("ProvServer", "gip.");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithBadDomainname() throws IOException {
        FlowTlvEncoder encoder = new FlowTlvEncoder(12);
        TypeWithValueNode node = new TypeWithValueNode("ProvServer", "gip");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithAnotherBadDomainname() throws IOException {
        ProvServerTlvEncoder encoder = new ProvServerTlvEncoder(12);
        TypeWithValueNode node = new TypeWithValueNode("ProvServer", "333.gip.com");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }
*/

}
