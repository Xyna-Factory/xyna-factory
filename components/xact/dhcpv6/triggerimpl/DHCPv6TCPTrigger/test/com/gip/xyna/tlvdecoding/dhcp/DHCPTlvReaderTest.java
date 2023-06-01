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
package com.gip.xyna.tlvdecoding.dhcp;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6TlvReader;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.TlvReader;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.TlvReaderException;

import static org.junit.Assert.*;



/**
 * Tests DHCP TLV reader.
 */
public final class DHCPTlvReaderTest {

    @Test
    public void testSuccessfulNormalUse() throws TlvReaderException {
        StringBuilder input = new StringBuilder();
        input.append("0x");
        input.append("00000000");
        input.append("0001000101");
        input.append("000200020202");
        input.append("00030003030303");
        input.append("0004000404040404");
        input.append("000500050505050505");

        TlvReader reader = new DHCPv6TlvReader(new ByteArrayInputStream(TestHelper.toByteArray(input.toString())),
                new HashSet<Long>());

        for (int i = 0; i < 6; ++i) {
            Tlv tlv = reader.read();
            assertEquals(i, tlv.getTypeEncoding());
            assertEquals(i, tlv.getValue().size());
            if (tlv.getValue().size() > 0) {
                for (Byte b : tlv.getValue()) {
                    assertEquals(i, (int) b & 0xFF);
                }
            }
        }
        assertNull(reader.read());
    }


    @Test (expected = TlvReaderException.class)
    public void testReadFailsWithLengthTooHigh() throws TlvReaderException {
        StringBuilder input = new StringBuilder();
        input.append("0x01FF");
        for (int i = 0; i < 255; ++i) {
            input.append("AA");
        }
        DHCPv6TlvReader reader = new DHCPv6TlvReader(new ByteArrayInputStream(TestHelper.toByteArray(input.toString())),
                new HashSet<Long>());
        reader.read();
    }

    @Test (expected = TlvReaderException.class)
    public void testReadFailsWhenEndOfStreamIsReachedInTheMiddleOfATlv() throws TlvReaderException {
        DHCPv6TlvReader reader = new DHCPv6TlvReader(new ByteArrayInputStream(TestHelper.toByteArray("0x0123AAAA")),
                new HashSet<Long>());
        reader.read();
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateFailsWithInputStreamNull() {
        new DHCPv6TlvReader(null, new HashSet<Long>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateFailsWithPaddingTypeEncodingsNull() {
        new DHCPv6TlvReader(new ByteArrayInputStream(TestHelper.toByteArray("0x0000")), null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateFailsWithNullValueInPaddingTypeEncodings() {
        Set<Long> typeEncodings = new HashSet<Long>();
        typeEncodings.add(0L);
        typeEncodings.add(null);
        new DHCPv6TlvReader(new ByteArrayInputStream(TestHelper.toByteArray("0x0000")), typeEncodings);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateFailsWithTooLowlValueInPaddingTypeEncodings() {
        Set<Long> typeEncodings = new HashSet<Long>();
        typeEncodings.add(0L);
        typeEncodings.add(-1L);
        new DHCPv6TlvReader(new ByteArrayInputStream(TestHelper.toByteArray("0x0000")), typeEncodings);
    }

//    @Test (expected = IllegalArgumentException.class)
//    public void testCreateFailsWithTooHighValueInPaddingTypeEncodings() {
//        Set<Long> typeEncodings = new HashSet<Long>();
//        typeEncodings.add(0L);
//        typeEncodings.add(70000L);
//        new DHCPv6TlvReader(new ByteArrayInputStream(TestHelper.toByteArray("0x0000")), typeEncodings);
//    }
}
