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
package com.gip.xyna.utils.javascript.lexer.patterns;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gip.xyna.utils.javascript.lexer.patterns.JavaScriptRegularExpressionPattern;

/**
 * JavaScript regular expression pattern test.
 *
 */
public final class JavaScriptRegularExpressionPatternTest {

    private JavaScriptRegularExpressionPattern pattern = new JavaScriptRegularExpressionPattern();

    @Test
    public void testIsMatchingPrefix() {
        assertTrue(pattern.isMatchingPrefix(""));
        assertTrue(pattern.isMatchingPrefix("/"));
        assertFalse(pattern.isMatchingPrefix("//"));
        assertTrue(pattern.isMatchingPrefix("/foo/"));
        assertTrue(pattern.isMatchingPrefix("/foo/g"));
        assertTrue(pattern.isMatchingPrefix("/foo/gi"));
        assertTrue(pattern.isMatchingPrefix("/foo\\/bar/gi"));
        assertFalse(pattern.isMatchingPrefix("/foo/gi,"));
        assertFalse(pattern.isMatchingPrefix("/foo/gi/"));
    }

    @Test
    public void testIsCompleteMatch() {
        assertFalse(pattern.isCompleteMatch(""));
        assertFalse(pattern.isCompleteMatch("foo"));
        assertFalse(pattern.isCompleteMatch("// foo"));
        assertFalse(pattern.isCompleteMatch("/foo//"));
        assertTrue(pattern.isCompleteMatch("/foo/gi"));
        assertTrue(pattern.isCompleteMatch("/foo/"));
        assertTrue(pattern.isCompleteMatch("/foo\\/bar\"/"));
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
