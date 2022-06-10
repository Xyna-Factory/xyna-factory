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
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigTree;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;




/**
 * Tests text config tree.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class TextConfigTreeTest {

    @Test
    public void testNormalUse() {
        TextConfigTree tree = new TextConfigTree(Collections.unmodifiableList(new ArrayList<Node>()));
        assertEquals(new ArrayList<Node>(), tree.getNodes());

        tree = new TextConfigTree(createNodeList());
        assertEquals(createNodeList(), tree.getNodes());
        assertEquals(createFooNodes(), tree.getNodes("foo"));
        assertEquals(createBarNodes(), tree.getNodes("bar"));
    }

    private static List<Node> createNodeList() {
        List<Node> nodes = new ArrayList<Node>();
        nodes.addAll(createFooNodes());
        nodes.addAll(createBarNodes());
        return Collections.unmodifiableList(nodes);
    }

    private static List<Node> createFooNodes() {
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(new TypeOnlyNode("foo", new ArrayList<Node>()));
        return Collections.unmodifiableList(nodes);
    }

    private static List<Node> createBarNodes() {
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(new TypeWithValueNode("bar", "baz"));
        nodes.add(new TypeWithValueNode("bar", "qux"));
        return Collections.unmodifiableList(nodes);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithNodeListNull() {
        new TextConfigTree(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithNullValueInNodeList() {
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(new TypeOnlyNode("foo", new ArrayList<Node>()));
        nodes.add(null);
        nodes.add(new TypeWithValueNode("bar", "baz"));
        new TextConfigTree(Collections.unmodifiableList(nodes));
    }

    @Test
    public void testEquals() {
        assertEquals(new TextConfigTree(Collections.unmodifiableList(new ArrayList<Node>())),
                new TextConfigTree(Collections.unmodifiableList(new ArrayList<Node>())));
        assertEquals(new TextConfigTree(createNodeList()), new TextConfigTree(createNodeList()));
        assertFalse(new TextConfigTree(Collections.unmodifiableList(new ArrayList<Node>())).equals(
                new TextConfigTree(createNodeList())));
        assertFalse(new TextConfigTree(Collections.unmodifiableList(new ArrayList<Node>())).equals(null));
        assertFalse(new TextConfigTree(Collections.unmodifiableList(new ArrayList<Node>())).equals("foo"));
    }

    @Test
    public void testHashCode() {
        assertEquals(new TextConfigTree(Collections.unmodifiableList(new ArrayList<Node>())).hashCode(),
                new TextConfigTree(Collections.unmodifiableList(new ArrayList<Node>())).hashCode());
        assertEquals(new TextConfigTree(createNodeList()).hashCode(), new TextConfigTree(createNodeList()).hashCode());
        assertFalse(new TextConfigTree(Collections.unmodifiableList(new ArrayList<Node>())).hashCode() == 
                new TextConfigTree(createNodeList()).hashCode());
        assertFalse(new TextConfigTree(Collections.unmodifiableList(new ArrayList<Node>())).hashCode() ==
                "foo".hashCode());
    }
}
