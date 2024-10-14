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
package com.gip.xyna.tlvencoding.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gip.xyna.tlvencoding.encodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;



/**
 * Tests byte util.
 */
public final class ByteUtilTest {

    @Test
    public void testIntToByteArray() {
        assertEquals(createIntegerListForIntParse(0x00, 0x00, 0x00, 0x00),
                TestHelper.toUnsignedIntList(ByteUtil.toByteArray(0x00000000)));
        assertEquals(createIntegerListForIntParse(0x01, 0x02, 0x03, 0x04),
                TestHelper.toUnsignedIntList(ByteUtil.toByteArray(0x01020304)));
        assertEquals(createIntegerListForIntParse(0xFE, 0xDC, 0xBA, 0x98),
                TestHelper.toUnsignedIntList(ByteUtil.toByteArray(0xFEDCBA98)));
        assertEquals(createIntegerListForIntParse(0xFF, 0xFF, 0xFF, 0xFF),
                TestHelper.toUnsignedIntList(ByteUtil.toByteArray(0xFFFFFFFF)));
    }

    @Test
    public void testLongToByteArray() {
        assertEquals(createIntegerListForLongParse(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
                TestHelper.toUnsignedIntList(ByteUtil.toByteArray(0x0000000000000000L)));
        assertEquals(createIntegerListForLongParse(0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF),
                TestHelper.toUnsignedIntList(ByteUtil.toByteArray(0x0123456789ABCDEFL)));
        assertEquals(createIntegerListForLongParse(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF),
                TestHelper.toUnsignedIntList(ByteUtil.toByteArray(0xFFFFFFFFFFFFFFFFL)));
    }

    @Test
    public void testStringToByteArray() {
        assertEquals(createIntegerList(0x0F), TestHelper.toUnsignedIntList(ByteUtil.toByteArray("0x0F")));
        assertEquals(createIntegerListForIntParse(0x01, 0x23, 0xCD, 0xEF), TestHelper.toUnsignedIntList(
                ByteUtil.toByteArray("0x0123CDEF")));
        assertEquals(createIntegerListForLongParse(0x00, 0x45, 0x67, 0x89, 0xAB, 0x3B, 0xCD, 0xEF),
                TestHelper.toUnsignedIntList(ByteUtil.toByteArray("0x00456789AB3BCDEF")));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testStringToByteArrayWithNull() {
        ByteUtil.toByteArray(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testStringToByteArrayWithBadValue() {
        ByteUtil.toByteArray("0x");
    }

    private List<Integer> createIntegerList(final int value) {
        List<Integer> list = new ArrayList<Integer>();
        list.add(value);
        return list;
    }

    private List<Integer> createIntegerListForIntParse(int a, int b, int c, int d) {
        List<Integer> list = new ArrayList<Integer>();
        list.add(a);
        list.add(b);
        list.add(c);
        list.add(d);
        return list;
    }

    private List<Integer> createIntegerListForLongParse(int a, int b, int c, int d, int e, int f, int g, int h) {
        List<Integer> list = new ArrayList<Integer>();
        list.add(a);
        list.add(b);
        list.add(c);
        list.add(d);
        list.add(e);
        list.add(f);
        list.add(g);
        list.add(h);
        return list;
    }

    @Test
    public void testToHexValue() {
        final String hexValue = "0x1234567890ABCDEF";
        assertEquals(hexValue, ByteUtil.toHexValue(ByteUtil.toByteArray(hexValue)));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToHexValueWithNull() {
        ByteUtil.toHexValue(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToHexValueWithEmptyByteArray() {
        ByteUtil.toHexValue(new byte[0]);
    }
}
