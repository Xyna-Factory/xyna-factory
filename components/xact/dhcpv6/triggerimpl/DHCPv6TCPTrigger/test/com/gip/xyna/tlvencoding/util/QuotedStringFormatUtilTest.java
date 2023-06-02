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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.QuotedStringFormatUtil;

/**
 * Tests quoted string format util.
 */
public final class QuotedStringFormatUtilTest {

    @Test
    public void testIsQuoteFormatNormally() {
        assertTrue(QuotedStringFormatUtil.isQuoteFormat("\"\""));
        assertTrue(QuotedStringFormatUtil.isQuoteFormat("\"foo\""));
        assertTrue(QuotedStringFormatUtil.isQuoteFormat("\"\\\\fo\\\"o\""));
        assertFalse(QuotedStringFormatUtil.isQuoteFormat(""));
        assertFalse(QuotedStringFormatUtil.isQuoteFormat("foo"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testIsQuoteFormatWithNull() {
        QuotedStringFormatUtil.isQuoteFormat(null);
    }

    @Test
    public void testUnquoteNormally() {
        assertEquals("", QuotedStringFormatUtil.unquote("\"\""));
        assertEquals("foo", QuotedStringFormatUtil.unquote("\"foo\""));
        assertEquals("\\fo\"o", QuotedStringFormatUtil.unquote("\"\\\\fo\\\"o\""));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testUnquoteWithNull() {
        QuotedStringFormatUtil.unquote(null);
    }
}
