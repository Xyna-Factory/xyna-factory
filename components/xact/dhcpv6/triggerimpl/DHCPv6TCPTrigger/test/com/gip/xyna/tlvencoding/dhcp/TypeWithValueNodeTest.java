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
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;




/**
 * Tests type with value node.
 */
public final class TypeWithValueNodeTest {

    @Test
    public void testNormalUse() {
        TypeWithValueNode node = new TypeWithValueNode("foo", "bar");
        assertEquals("foo", node.getTypeName());
        assertEquals("bar", node.getValue());
        node = new TypeWithValueNode("baz", "");
        assertEquals("baz", node.getTypeName());
        assertEquals("", node.getValue());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithTypeNameAsNull() {
        new TypeWithValueNode(null, "bar");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithTypeNameAsEmptyString() {
        new TypeWithValueNode("", "bar");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithValueNull() {
        new TypeWithValueNode("foo", null);
    }

    @Test
    public void testEquals() {
        assertEquals(new TypeWithValueNode("foo", "bar"), new TypeWithValueNode("foo", "bar"));
        assertEquals(new TypeWithValueNode("baz", ""), new TypeWithValueNode("baz", ""));
        assertFalse(new TypeWithValueNode("foo", "bar").equals(new TypeWithValueNode("baz", "bar")));
        assertFalse(new TypeWithValueNode("foo", "bar").equals(new TypeWithValueNode("foo", "baz")));
        assertFalse(new TypeWithValueNode("foo", "bar").equals(null));
        assertFalse(new TypeWithValueNode("foo", "bar").equals("foo"));
    }

    @Test
    public void testHashCode() {
        assertEquals(new TypeWithValueNode("foo", "bar").hashCode(), new TypeWithValueNode("foo", "bar").hashCode());
        assertEquals(new TypeWithValueNode("baz", "").hashCode(), new TypeWithValueNode("baz", "").hashCode());
        assertFalse(new TypeWithValueNode("foo", "bar").hashCode() == new TypeWithValueNode("baz", "bar").hashCode());
        assertFalse(new TypeWithValueNode("foo", "bar").hashCode() == new TypeWithValueNode("foo", "baz").hashCode());
        assertFalse(new TypeWithValueNode("foo", "bar").hashCode() == "foo".hashCode());
    }
}
