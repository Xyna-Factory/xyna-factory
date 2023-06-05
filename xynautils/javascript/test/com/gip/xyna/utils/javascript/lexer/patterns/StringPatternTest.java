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
package com.gip.xyna.utils.javascript.lexer.patterns;

import static org.junit.Assert.*;

import org.junit.Test;

import com.gip.xyna.utils.javascript.lexer.patterns.StringPattern;

/**
 * String pattern test.
 *
 */
public final class StringPatternTest {

    private StringPattern pattern = new StringPattern();

    @Test
    public void testNormalUse() {
        assertTrue(pattern.isMatchingPrefix(""));
        assertTrue(pattern.isMatchingPrefix("\""));
        assertTrue(pattern.isMatchingPrefix("\"foo"));
        assertTrue(pattern.isMatchingPrefix("\"foo\\\""));
        assertTrue(pattern.isMatchingPrefix("\"foo\""));
        assertTrue(pattern.isMatchingPrefix("\"foo\\\"\""));
        assertTrue(pattern.isMatchingPrefix("\"\""));
        assertTrue(pattern.isMatchingPrefix("'"));
        assertTrue(pattern.isMatchingPrefix("'foo"));
        assertTrue(pattern.isMatchingPrefix("'foo\'"));
        assertTrue(pattern.isMatchingPrefix("'foo'"));
        assertTrue(pattern.isMatchingPrefix("'foo\\''"));
        assertTrue(pattern.isMatchingPrefix("''"));
        assertFalse(pattern.isMatchingPrefix("foo"));
        assertFalse(pattern.isMatchingPrefix("\"\"\""));
        assertFalse(pattern.isMatchingPrefix("'''"));
        assertTrue(pattern.isCompleteMatch("\"\""));
        assertTrue(pattern.isCompleteMatch("\"foo\""));
        assertTrue(pattern.isCompleteMatch("\"foo\\\"\""));
        assertTrue(pattern.isCompleteMatch("\"\""));
        assertTrue(pattern.isCompleteMatch("'foo'"));
        assertTrue(pattern.isCompleteMatch("'foo\\''"));
        assertTrue(pattern.isCompleteMatch("''"));
    }

    @Test
    public void testHandlingOfNewLineCharacters() {
        assertFalse(pattern.isMatchingPrefix("\"\n"));
        assertFalse(pattern.isMatchingPrefix("\"\r"));
        assertFalse(pattern.isMatchingPrefix("\"\u0085"));
        assertFalse(pattern.isMatchingPrefix("\"\u2028"));
        assertFalse(pattern.isMatchingPrefix("\"\u2029"));
        assertFalse(pattern.isMatchingPrefix("'\n"));
        assertFalse(pattern.isMatchingPrefix("'\r"));
        assertFalse(pattern.isMatchingPrefix("'\u0085"));
        assertFalse(pattern.isMatchingPrefix("'\u2028"));
        assertFalse(pattern.isMatchingPrefix("'\u2029"));
        assertFalse(pattern.isCompleteMatch("\"\n\""));
        assertFalse(pattern.isCompleteMatch("\"\r\""));
        assertFalse(pattern.isCompleteMatch("\"\u0085\""));
        assertFalse(pattern.isCompleteMatch("\"\u2028\""));
        assertFalse(pattern.isCompleteMatch("\"\u2029\""));
        assertFalse(pattern.isCompleteMatch("'\n'"));
        assertFalse(pattern.isCompleteMatch("'\r'"));
        assertFalse(pattern.isCompleteMatch("'\u0085'"));
        assertFalse(pattern.isCompleteMatch("'\u2028'"));
        assertFalse(pattern.isCompleteMatch("'\u2029'"));
    }

    @Test (expected = NullPointerException.class)
    public void testIsMatchingPrefixWithNull() {
        pattern.isMatchingPrefix(null);
    }

    @Test (expected = NullPointerException.class)
    public void testIsCompleteMatchWithNull() {
        pattern.isCompleteMatch(null);
    }
}
