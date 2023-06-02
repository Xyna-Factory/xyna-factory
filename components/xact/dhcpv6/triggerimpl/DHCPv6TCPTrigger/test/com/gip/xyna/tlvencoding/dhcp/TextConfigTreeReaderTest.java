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
package com.gip.xyna.tlvencoding.dhcp;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.ConfigFileReadException;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigTree;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigTreeReader;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;

/**
 * Tests text config tree reader.
 */
public final class TextConfigTreeReaderTest {

    @Test
    public void testNormalUse() throws ConfigFileReadException {
        String config = "foo\n  bar:123\n  baz:456\n";
        TextConfigTreeReader reader = new TextConfigTreeReader(new StringReader(config));
        assertEquals(createFirstTree(), reader.read());

        StringBuilder sb = new StringBuilder();
        sb.append("Downstream Packet Classification Encoding\n");
        sb.append("  Classifier Reference:4\n");
        sb.append("  Service Flow Reference:4\n");
        sb.append("  Rule Priority:98\n");
        sb.append("  Classifier Activation State:on\n");
        sb.append("  IP Packet Classification Encodings\n");
        sb.append("    IP Protocol:17\n");
        sb.append("    IP Destination Address:10.0.0.0\n");
        sb.append("    IP Destination Mask:255.192.0.0\n");
        sb.append("    TCP/UDP Source Port Start:5060\n");
        sb.append("    TCP/UDP Source Port End:5060\n");
        sb.append("    TCP/UDP Destination Port Start:5060\n");
        sb.append("    TCP/UDP Destination Port End:5060\n");
        sb.append("Privacy Enable:on\n");
        sb.append("SNMP MIB Object:(docsDevFilterIpDefault)1.3.6.1.2.1.69.1.6.3.0, Integer, 2\n");
        reader = new TextConfigTreeReader(new StringReader(sb.toString()));
        assertEquals(createSecondTree(), reader.read());
    }

    private static TextConfigTree createFirstTree() {
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(new TypeWithValueNode("bar", "123"));
        nodes.add(new TypeWithValueNode("baz", "456"));
        List<Node> treeNodes = new ArrayList<Node>();
        treeNodes.add(new TypeOnlyNode("foo", nodes));
        return new TextConfigTree(treeNodes);
    }

    private static TextConfigTree createSecondTree() {
        List<Node> level2 = new ArrayList<Node>();
        level2.add(new TypeWithValueNode("IP Protocol", "17"));
        level2.add(new TypeWithValueNode("IP Destination Address", "10.0.0.0"));
        level2.add(new TypeWithValueNode("IP Destination Mask", "255.192.0.0"));
        level2.add(new TypeWithValueNode("TCP/UDP Source Port Start", "5060"));
        level2.add(new TypeWithValueNode("TCP/UDP Source Port End", "5060"));
        level2.add(new TypeWithValueNode("TCP/UDP Destination Port Start", "5060"));
        level2.add(new TypeWithValueNode("TCP/UDP Destination Port End", "5060"));
        List<Node> level1 = new ArrayList<Node>();
        level1.add(new TypeWithValueNode("Classifier Reference", "4"));
        level1.add(new TypeWithValueNode("Service Flow Reference", "4"));
        level1.add(new TypeWithValueNode("Rule Priority", "98"));
        level1.add(new TypeWithValueNode("Classifier Activation State", "on"));
        level1.add(new TypeOnlyNode("IP Packet Classification Encodings", level2));
        List<Node> level0 = new ArrayList<Node>();
        level0.add(new TypeOnlyNode("Downstream Packet Classification Encoding", level1));
        level0.add(new TypeWithValueNode("Privacy Enable", "on"));
        level0.add(new TypeWithValueNode("SNMP MIB Object", "(docsDevFilterIpDefault)1.3.6.1.2.1.69.1.6.3.0, Integer, 2"));
        return new TextConfigTree(level0);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreateWithNull() {
        new TextConfigTreeReader(null);
    }

    @Test (expected = UnsupportedOperationException.class)
    public void testReadTwice() throws ConfigFileReadException {
        String config = "foo\n  bar:123\n  baz:456\n";
        TextConfigTreeReader reader = new TextConfigTreeReader(new StringReader(config));
        reader.read();
        reader.read();
    }

    @Test (expected = ConfigFileReadException.class)
    public void testReadOfWronglyIndentedConfig() throws ConfigFileReadException {
        String config = "  foo\n  bar:123\n  baz:456\n";
        new TextConfigTreeReader(new StringReader(config)).read();
    }

    @Test (expected = ConfigFileReadException.class)
    public void testReadOfAnotherWronglyIndentedConfig() throws ConfigFileReadException {
        String config = "foo\n  bar:123\n    baz:456\n";
        new TextConfigTreeReader(new StringReader(config)).read();
    }
}
