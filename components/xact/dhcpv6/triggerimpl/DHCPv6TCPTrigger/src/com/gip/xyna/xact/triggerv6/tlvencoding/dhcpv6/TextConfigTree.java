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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Text config tree.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class TextConfigTree {

    private final List<Node> nodes;
    private final Map<String, List<Node>> nodesLookUpTable;

    public TextConfigTree(final List<Node> nodes) {
        if (nodes == null) {
            throw new IllegalArgumentException("Nodes may not be null.");
        }
        this.nodes = validateAndMakeUnmodifiable(nodes);
        this.nodesLookUpTable = createNodesLookUpTable();
    }

    private static List<Node> validateAndMakeUnmodifiable(final List<Node> nodeList) {
        List<Node> copy = new ArrayList<Node>();
        for (Node node : nodeList) {
            if (node == null) {
                throw new IllegalArgumentException("Found a node that is null: <" + nodeList + ">.");
            }
            copy.add(node);
        }
        return Collections.unmodifiableList(copy);
    }

    private Map<String, List<Node>> createNodesLookUpTable() {
        Map<String, List<Node>> lut = new HashMap<String, List<Node>>();
        for (Node node : nodes) {
            String typeName = node.getTypeName();
            if (!lut.containsKey(typeName)) {
                lut.put(typeName, new ArrayList<Node>());
            }
            lut.get(typeName).add(node);
        }
        for (Map.Entry<String, List<Node>> entry : lut.entrySet()) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));            
        }
        return Collections.unmodifiableMap(lut);
    }

    public List<Node> getNodes() {
        return this.nodes;
    }

    public List<Node> getNodes(final String typeName) {
        List<Node> nodes = this.nodesLookUpTable.get(typeName);
        if (nodes == null) {
            return Collections.unmodifiableList(new ArrayList<Node>());
        }
        return nodes;
    }

    @Override
    public String toString() {
        return this.nodes.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TextConfigTree other = (TextConfigTree) obj;
        if (nodes == null) {
            if (other.nodes != null) {
                return false;
            }
        } else if (!nodes.equals(other.nodes)) {
            return false;
        }
        return true;
    }
}
