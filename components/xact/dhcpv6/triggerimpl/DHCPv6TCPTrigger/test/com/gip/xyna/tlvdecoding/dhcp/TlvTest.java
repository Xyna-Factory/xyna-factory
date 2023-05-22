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
package com.gip.xyna.tlvdecoding.dhcp;

import java.util.ArrayList;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.Tlv;
import com.gip.xyna.xact.triggerv6.tlvdecoding.utilv6.ByteUtil;


/**
 * Tests TLV - type length value object.
 */
public final class TlvTest {

    @Test
    public void testNormalUseSuccess() {
        final String hexValue = "0x0123456789ABCDEF";
        Tlv tlv = new Tlv(255, TestHelper.toByteList(hexValue));
        assertEquals(255, tlv.getTypeEncoding());
        assertEquals(TestHelper.toByteList(hexValue), tlv.getValue());
        assertEquals(hexValue, ByteUtil.toHexValue(tlv.getValueAsByteArray()));
        assertNotNull(tlv.toString());

        tlv = new Tlv(0, new ArrayList<Byte>());
        assertEquals(0, tlv.getTypeEncoding());
        assertEquals(0, tlv.getValue().size());
        assertEquals(0, tlv.getValueAsByteArray().length);
        assertNotNull(tlv.toString());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorFailsWithTypeEncodingTooLow() {
        new Tlv(-1, new ArrayList<Byte>());
    }

//    @Test (expected = IllegalArgumentException.class)
//    public void testConstructorFailsWithTypeEncodingTooHigh() {
//        new Tlv(70000, new ArrayList<Byte>());
//    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorFailsWithValueNull() {
        new Tlv(0, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstrcuctorFailsWithNullInValueByteList() {
        List<Byte> bytes = TestHelper.toByteList("0x0123456789ABCDEF");
        bytes.add(null);
        new Tlv(255, bytes);
    }

    @Test (expected = UnsupportedOperationException.class)
    public void testModifyValueFails() {
        new Tlv(0, new ArrayList<Byte>()).getValue().add((byte) 123);
    }
}
