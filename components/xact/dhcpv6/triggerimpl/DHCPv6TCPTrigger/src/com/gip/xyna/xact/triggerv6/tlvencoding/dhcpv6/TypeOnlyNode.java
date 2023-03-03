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
import java.util.List;

/**
 * Node with type and no value, but possibly sub nodes.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class TypeOnlyNode extends Node {

    private final List<Node> subNodes;

    public TypeOnlyNode(final String typeName, final List<Node> subNodes) {
        super(typeName);
        if (subNodes == null) {
            throw new IllegalArgumentException("List of sub nodes may not be null.");
        }
        this.subNodes = validateAndMakeUnmodifiable(subNodes);
    }

    private static List<Node> validateAndMakeUnmodifiable(final List<Node> nodeList) {
        List<Node> copy = new ArrayList<Node>();
        for (Node node : nodeList) {
            if (node == null) {
                throw new IllegalArgumentException("Found a sub node that is null: <" + nodeList + ">.");
            }
            copy.add(node);
        }
        return Collections.unmodifiableList(copy);
    }

    public List<Node> getSubNodes() {
        return subNodes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{typeName:<");
        sb.append(this.getTypeName());
        sb.append(">,subNodes:<");
        sb.append(this.subNodes);
        sb.append(">}");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((subNodes == null) ? 0 : subNodes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        TypeOnlyNode other = (TypeOnlyNode) obj;
        if (subNodes == null) {
            if (other.subNodes != null)
                return false;
        } else if (!subNodes.equals(other.subNodes))
            return false;
        return true;
    }
}
