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
package com.gip.xyna.tlvencoding.dhcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;




/**
 * Tests type only node.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class TypeOnlyNodeTest {

    @Test
    public void testNormalUse() {
        TypeOnlyNode node = new TypeOnlyNode("foo", new ArrayList<Node>());
        assertEquals("foo", node.getTypeName());
        assertEquals(new ArrayList<Node>(), node.getSubNodes());
        
        node = new TypeOnlyNode("bar", createNodeList());
        assertEquals("bar", node.getTypeName());
        assertEquals(createNodeList(), node.getSubNodes());
    }

    private static List<Node> createNodeList() {
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(new TypeOnlyNode("foo", new ArrayList<Node>()));
        nodes.add(new TypeWithValueNode("baz", "123"));
        return Collections.unmodifiableList(nodes);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithTypeNameNull() {
        new TypeOnlyNode(null, new ArrayList<Node>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithTypeNameAsEmptyString() {
        new TypeOnlyNode("", new ArrayList<Node>());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithSubNodesNull() {
        new TypeOnlyNode("foo", null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithNullElementInSubNodes() {
        List<Node> subNodes = new ArrayList<Node>();
        subNodes.add(new TypeOnlyNode("foo", new ArrayList<Node>()));
        subNodes.add(null);
        subNodes.add(new TypeWithValueNode("baz", "123"));
        new TypeOnlyNode("bar", Collections.unmodifiableList(subNodes));
    }

    @Test
    public void testEquals() {
        assertEquals(new TypeOnlyNode("foo", new ArrayList<Node>()), new TypeOnlyNode("foo", new ArrayList<Node>()));
        assertFalse(new TypeOnlyNode("foo", new ArrayList<Node>()).equals(
                new TypeOnlyNode("bar", new ArrayList<Node>())));
        assertFalse(new TypeOnlyNode("foo", new ArrayList<Node>()).equals(null));
        assertFalse(new TypeOnlyNode("foo", new ArrayList<Node>()).equals("foo"));
        assertEquals(new TypeOnlyNode("bar", createNodeList()), new TypeOnlyNode("bar", createNodeList()));
        assertFalse(new TypeOnlyNode("bar", createNodeList()).equals(new TypeOnlyNode("foo", createNodeList())));
        assertFalse(new TypeOnlyNode("bar", createNodeList()).equals(new TypeOnlyNode("bar", new ArrayList<Node>())));
    }

    @Test
    public void testHashCode() {
        assertEquals(new TypeOnlyNode("foo", new ArrayList<Node>()).hashCode(),
                new TypeOnlyNode("foo", new ArrayList<Node>()).hashCode());
        assertFalse(new TypeOnlyNode("foo", new ArrayList<Node>()).hashCode() ==
                new TypeOnlyNode("bar", new ArrayList<Node>()).hashCode());
        assertFalse(new TypeOnlyNode("foo", new ArrayList<Node>()).hashCode() == "foo".hashCode());
        assertEquals(new TypeOnlyNode("bar", createNodeList()).hashCode(),
                new TypeOnlyNode("bar", createNodeList()).hashCode());
        assertFalse(new TypeOnlyNode("bar", createNodeList()).hashCode() ==
                new TypeOnlyNode("foo", createNodeList()).hashCode());
        assertFalse(new TypeOnlyNode("bar", createNodeList()).hashCode() ==
                new TypeOnlyNode("bar", new ArrayList<Node>()).hashCode());
    }
}
