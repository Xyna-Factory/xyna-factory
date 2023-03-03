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
package com.gip.xyna.tlvdecoding.util;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

import com.gip.xyna.tlvdecoding.decodetest.TestHelper;
import com.gip.xyna.xact.triggerv6.tlvdecoding.utilv6.OctetStringUtil;



/**
 * Tests octet string util.
 */
public final class OctetStringUtilTest {

    @Test
    public void testSuccessfulNormalUse() throws UnsupportedEncodingException {
        assertEquals("0x1F", OctetStringUtil.toString(TestHelper.toByteArray("0x1F")));
        assertEquals(" ", OctetStringUtil.toString(TestHelper.toByteArray("0x20")));
        assertEquals("~", OctetStringUtil.toString(TestHelper.toByteArray("0x7E")));
        assertEquals("0x7F", OctetStringUtil.toString(TestHelper.toByteArray("0x7F")));

        assertEquals("0x0123456789ABCDEF", OctetStringUtil.toString(TestHelper.toByteArray("0x0123456789ABCDEF")));
        assertEquals(" !\"#0A\\~", OctetStringUtil.toString(TestHelper.toByteArray("0x2021222330415C7E")));
        assertEquals("foobar", OctetStringUtil.toString("foobar".getBytes("UTF-8")));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToStringFailsWithNull() {
        OctetStringUtil.toString(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToStringFailsWithEmptyByteArray() {
        OctetStringUtil.toString(new byte[0]);
    }
}
