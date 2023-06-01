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
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.ProvServerTlvEncoder;



/**
 * Tests octet string TLV encoder.
 */
public final class ProvServerTlvEncoderTest {

    @Test
    public void testEncodeOfValidDomainValue() throws IOException {
        ProvServerTlvEncoder encoder = new ProvServerTlvEncoder(3);
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);
        TypeWithValueNode node = new TypeWithValueNode("ProvServer", "epprov.gip.com");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();

        /* Wireshark
        03 11 00 06 65 70 70 72 6f 76 03 67 69 70 03 63 6f 6d 00
        */
        expectedResult.write((byte)3);
        expectedResult.write((byte)17);
        expectedResult.write((byte)0);
        expectedResult.write((byte)6);
        expectedResult.write((byte)101);
        expectedResult.write((byte)112);
        expectedResult.write((byte)112);
        expectedResult.write((byte)114);
        expectedResult.write((byte)111);
        expectedResult.write((byte)118);
        expectedResult.write((byte)3);
        expectedResult.write((byte)103);
        expectedResult.write((byte)105);
        expectedResult.write((byte)112);
        expectedResult.write((byte)3);
        expectedResult.write((byte)99);
        expectedResult.write((byte)111);
        expectedResult.write((byte)109);
        expectedResult.write((byte)0);

        
        assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));
    }



    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithBadValue() throws IOException {
        ProvServerTlvEncoder encoder = new ProvServerTlvEncoder(12);
        TypeWithValueNode node = new TypeWithValueNode("ProvServer", "0x3000");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithIncompleteDomainname() throws IOException {
        ProvServerTlvEncoder encoder = new ProvServerTlvEncoder(12);
        TypeWithValueNode node = new TypeWithValueNode("ProvServer", "gip.");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWriteWithBadDomainname() throws IOException {
        ProvServerTlvEncoder encoder = new ProvServerTlvEncoder(12);
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


}
