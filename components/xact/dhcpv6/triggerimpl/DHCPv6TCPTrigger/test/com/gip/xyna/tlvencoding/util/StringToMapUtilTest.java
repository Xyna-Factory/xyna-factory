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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.StringToMapUtil;

/**
 * Tests string to map util.
 */
public final class StringToMapUtilTest {

    @Test
    public void testSuccessfulUseOfToMapOfSets() {
        Map<String, Set<String>> result = StringToMapUtil.toMapOfSets(
                "\"foo\"=\"qux\",\"foo\"=\"bar\",\"baz\"=\"1\\\",2\\\",3\\\"\\\\\"");
        Map<String, Set<String>> expectedResult = new HashMap<String, Set<String>>();
        expectedResult.put("foo", createSet("qux", "bar"));
        expectedResult.put("baz", createSet("1\",2\",3\"\\"));
        assertEquals(expectedResult, result);

        result = StringToMapUtil.toMapOfSets("\"foo\"=\"123\",\"bar\"=\"456\",\"baz\"=\"789\"");
        expectedResult = new HashMap<String, Set<String>>();
        expectedResult.put("foo", createSet("123"));
        expectedResult.put("bar", createSet("456"));
        expectedResult.put("baz", createSet("789"));
        assertEquals(expectedResult, result);
    }

    private static Set<String> createSet(String ... values) {
        Set<String> result = new HashSet<String>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }

    @Test
    public void testSuccessfulUseOfMapOfSetsWithEmptyString() {
        assertEquals(new HashMap<String, String>(), StringToMapUtil.toMapOfSets(""));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapOfSetsFailsWithNull() {
        StringToMapUtil.toMapOfSets(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapOfSetsFailsWithWronglyEscapedString() {
        StringToMapUtil.toMapOfSets("\"foo\"=\"\\bar\"");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapOfSetsFailsWithStringNotStartingWithQuote() {
        StringToMapUtil.toMapOfSets("baz\"foo\"=\"\bar\"");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapOfSetsFailsWithPrematurelyTerminatedString() {
        StringToMapUtil.toMapOfSets("\"foo\"=\"bar");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapOfSetsFailsWithKeyDefinedMoreThanOnceWithSameValue() {
        StringToMapUtil.toMapOfSets("\"foo\"=\"bar\",\"foo\"=\"bar\"");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapOfSetsFailsWithWrongSeparator() {
        StringToMapUtil.toMapOfSets("\"foo\"=\"123\";\"bar\"=\"456\",\"baz\"=\"789\"");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapOfSetsFailsWithWrongAssignmentOperator() {
        StringToMapUtil.toMapOfSets("\"foo\":\"123\",\"bar\"=\"456\",\"baz\"=\"789\"");
    }

    @Test
    public void testSuccessfulUseOfToMap() {
        Map<String, String> result = StringToMapUtil.toMap("\"foo\"=\"bar\",\"baz\"=\"1\\\",2\\\",3\\\"\\\\\"");
        Map<String, String> expectedResult = new HashMap<String, String>();
        expectedResult.put("foo", "bar");
        expectedResult.put("baz", "1\",2\",3\"\\");
        assertEquals(expectedResult, result);

        result = StringToMapUtil.toMap("\"foo\"=\"123\",\"bar\"=\"456\",\"baz\"=\"789\"");
        expectedResult = new HashMap<String, String>();
        expectedResult.put("foo", "123");
        expectedResult.put("bar", "456");
        expectedResult.put("baz", "789");
        assertEquals(expectedResult, result);
    }

    @Test
    public void testSuccessfulUseOfToMapWithEmptyString() {
        assertEquals(new HashMap<String, String>(), StringToMapUtil.toMap(""));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapFailsWithNull() {
        StringToMapUtil.toMap(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapFailsWithWronglyEscapedString() {
        StringToMapUtil.toMap("\"foo\"=\"\\bar\"");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapFailsWithStringNotStartingWithQuote() {
        StringToMapUtil.toMap("baz\"foo\"=\"\bar\"");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapFailsWithPrematurelyTerminatedString() {
        StringToMapUtil.toMap("\"foo\"=\"bar");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapFailsWithDuplicateKey() {
        StringToMapUtil.toMap("\"foo\"=\"bar\",\"foo\"=\"baz\"");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapFailsWithKeyDefinedMoreThanOnceWithSameValue() {
        StringToMapUtil.toMap("\"foo\"=\"bar\",\"foo\"=\"bar\"");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapFailsWithWrongSeparator() {
        StringToMapUtil.toMap("\"foo\"=\"123\";\"bar\"=\"456\",\"baz\"=\"789\"");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testToMapFailsWithWrongAssignmentOperator() {
        StringToMapUtil.toMap("\"foo\":\"123\",\"bar\"=\"456\",\"baz\"=\"789\"");
    }
}
