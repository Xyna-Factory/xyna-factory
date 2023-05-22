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

import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.AbstractTypeWithValueTlvEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders.FQDNTlvEncoder;



/**
 * Tests octet string TLV encoder.
 */
public final class FQDNTlvEncoderTest {

    @Test
    public void testEncodeOfHexadecimalValue() throws IOException {
        FQDNTlvEncoder encoder = new FQDNTlvEncoder(13);
        assertEquals(13, encoder.getTypeEncoding());
        assertTrue(encoder instanceof AbstractTypeWithValueTlvEncoder);
    TypeWithValueNode node = new TypeWithValueNode("FQDN", "0x6162636430");
    ByteArrayOutputStream target = new ByteArrayOutputStream();
    encoder.write(node, target);
    ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
    expectedResult.write(0);
    expectedResult.write(13);
    expectedResult.write(0);
    expectedResult.write(7);
    expectedResult.write(5);
    expectedResult.write(97);
    expectedResult.write(98);
    expectedResult.write(99);
    expectedResult.write(100);
    expectedResult.write(48);
    expectedResult.write(00);
    assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));


    node = new TypeWithValueNode("FQDN",
                                 "0x687474703A2F2F7777772E7465737465696E656E6C616E67656E746578747A7769736368656E70756E6B74656E2E666572746967");
    target = new ByteArrayOutputStream();
    encoder.write(node, target);
    expectedResult = new ByteArrayOutputStream();

    expectedResult.write(0); // encoding
    expectedResult.write(13);

    expectedResult.write(0); // Gesamtlaenge
    expectedResult.write(54);

    expectedResult.write(10); // Laenge bis Punkt
    expectedResult.write(104);
    expectedResult.write(116);
    expectedResult.write(116);
    expectedResult.write(112);
    expectedResult.write(58);
    expectedResult.write(47);
    expectedResult.write(47);
    expectedResult.write(119);
    expectedResult.write(119);
    expectedResult.write(119);

    expectedResult.write(34); // Laenge bis Punkt
    expectedResult.write(116);
    expectedResult.write(101);
    expectedResult.write(115);
    expectedResult.write(116);
    expectedResult.write(101);
    expectedResult.write(105);
    expectedResult.write(110);
    expectedResult.write(101);
    expectedResult.write(110);
    expectedResult.write(108);
    expectedResult.write(97);
    expectedResult.write(110);
    expectedResult.write(103);
    expectedResult.write(101);
    expectedResult.write(110);
    expectedResult.write(116);
    expectedResult.write(101);
    expectedResult.write(120);
    expectedResult.write(116);
    expectedResult.write(122);
    expectedResult.write(119);
    expectedResult.write(105);
    expectedResult.write(115);
    expectedResult.write(99);
    expectedResult.write(104);
    expectedResult.write(101);
    expectedResult.write(110);
    expectedResult.write(112);
    expectedResult.write(117);
    expectedResult.write(110);
    expectedResult.write(107);
    expectedResult.write(116);
    expectedResult.write(101);
    expectedResult.write(110);

    expectedResult.write(6); // Laenge bis Punkt
    expectedResult.write(102);
    expectedResult.write(101);
    expectedResult.write(114);
    expectedResult.write(116);
    expectedResult.write(105);
    expectedResult.write(103);
    expectedResult.write(00);
    assertEquals(TestHelper.toUnsignedIntList(expectedResult), TestHelper.toUnsignedIntList(target));

        
    }

    @Test (expected = IllegalArgumentException.class)
    public void testNonOctetString() throws IOException {
        FQDNTlvEncoder encoder = new FQDNTlvEncoder(13);
        TypeWithValueNode node = new TypeWithValueNode("FQDN", "Test");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGrossesX() throws IOException {
        FQDNTlvEncoder encoder = new FQDNTlvEncoder(13);
        TypeWithValueNode node = new TypeWithValueNode("FQDN", "0XAFFE");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testOhneHeader() throws IOException {
        FQDNTlvEncoder encoder = new FQDNTlvEncoder(13);
        TypeWithValueNode node = new TypeWithValueNode("FQDN", "AFFE");
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        encoder.write(node, target);
    }


}
