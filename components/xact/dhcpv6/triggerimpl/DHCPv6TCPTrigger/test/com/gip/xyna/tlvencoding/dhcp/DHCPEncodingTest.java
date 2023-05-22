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
package com.gip.xyna.tlvencoding.dhcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6Encoding;





public final class DHCPEncodingTest {

    @Test
    public void testNormalUse() {
        DHCPv6Encoding encoding = new DHCPv6Encoding(1, null, "foo", 101, null,"UnsignedInteger",
                new HashMap<String, String>());
        assertEquals(1, encoding.getId());
        assertNull(encoding.getParentId());
        assertEquals("foo", encoding.getTypeName());
        assertEquals(101, encoding.getTypeEncoding());
        assertEquals("UnsignedInteger", encoding.getValueDataTypeName());
        assertEquals(new HashMap<String, String>(), encoding.getValueDataTypeArguments());

        encoding = new DHCPv6Encoding(123, 456,"baz", 0, null, "UnsignedInteger", new HashMap<String, String>());
        assertEquals(123, encoding.getId());
        assertTrue(456 == encoding.getParentId());
        assertEquals("baz", encoding.getTypeName());
        assertEquals(0, encoding.getTypeEncoding());
        assertEquals("UnsignedInteger", encoding.getValueDataTypeName());
        assertEquals(new HashMap<String, String>(), encoding.getValueDataTypeArguments());

        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("foo", "bar");
        arguments.put("baz", "qux");
        encoding = new DHCPv6Encoding(2, null, "bar", 255, null, "BarDataType", Collections.unmodifiableMap(arguments));
        assertEquals(2, encoding.getId());
        assertNull(encoding.getParentId());
        assertEquals("bar", encoding.getTypeName());
        assertEquals(255, encoding.getTypeEncoding());
        assertEquals("BarDataType", encoding.getValueDataTypeName());
        assertEquals(arguments, encoding.getValueDataTypeArguments());

        //hinzugefuegt
        encoding = new DHCPv6Encoding(123, 456,"baz", 0, 4491, "UnsignedInteger", new HashMap<String, String>());
        assertEquals(123, encoding.getId());
        assertTrue(456 == encoding.getParentId());
        assertEquals("baz", encoding.getTypeName());
        assertEquals(0, encoding.getTypeEncoding());
        assertEquals("UnsignedInteger", encoding.getValueDataTypeName());
        assertEquals(new Integer(4491),encoding.getEnterpriseNr());
        assertEquals(new HashMap<String, String>(), encoding.getValueDataTypeArguments());

    
    
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithIdEqualToParentId() {
        new DHCPv6Encoding(1, 1, "foo", 101, null, "UnsignedInteger", new HashMap<String, String>());
    }



    @Test (expected = IllegalArgumentException.class)
    public void testWithTypeNameNull() {
        new DHCPv6Encoding(1, null, null, 0, 101, "UnsignedInteger", new HashMap<String, String>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithTypeNameAsEmptyString() {
        new DHCPv6Encoding(1, null, "", 101, null,"UnsignedInteger", new HashMap<String, String>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithTypeEncodingValueTooLow() {
        new DHCPv6Encoding(1, null, "foo", -1, null, "UnsignedInteger", new HashMap<String, String>());
    }

//    @Test (expected = IllegalArgumentException.class)
//    public void testWithTypeEncodingValueTooHigh() {
//        new DHCPv6Encoding(1, null, "foo", 70000, null, "UnsignedInteger", new HashMap<String, String>());
//    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithValueDataTypeNameNull() {
        new DHCPv6Encoding(1, null,"foo", 101, null,null, new HashMap<String, String>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithValueDataTypeNameAsEmptyString() {
        new DHCPv6Encoding(1, null, "foo", 101, null, "", new HashMap<String, String>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithInvalidValueDataTypeName() {
        new DHCPv6Encoding(1, null, "foo", 101, null, "123", new HashMap<String, String>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithValuDataTypeArgumentsNull() {
        new DHCPv6Encoding(1, null, "foo", 101, null,"UnsignedInteger", null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithValuDataTypeArgumentsKeyNull() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("foo", "bar");
        arguments.put(null, "qux");
        new DHCPv6Encoding(2, 1, "bar", 255, null,"BarDataType", Collections.unmodifiableMap(arguments));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithValuDataTypeArgumentsKeyValueNull() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("foo", "bar");
        arguments.put("baz", null);
        new DHCPv6Encoding(2, 1, "bar", 255, null, "BarDataType", Collections.unmodifiableMap(arguments));
    }
    
    //hinzugefuegt
    @Test (expected = IllegalArgumentException.class)
    public void testWithInvalidEnterpriseNr() {
        Map<String, String> arguments = new HashMap<String, String>();
        arguments.put("foo", "bar");
        arguments.put("baz", null);
        new DHCPv6Encoding(2, 1, "bar", 255, 4400, "BarDataType", Collections.unmodifiableMap(arguments));
    }

    
}
