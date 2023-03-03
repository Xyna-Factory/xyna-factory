package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6;
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

/**
 * Node.
 */
public abstract class Node {

    private final String typeName;

    Node(final String typeName) {
        if (typeName == null) {
            throw new IllegalArgumentException("Type name may not be null.");
        } else if ("".equals(typeName)) {
            throw new IllegalArgumentException("Type name may not be empty string.");
        }
        this.typeName = typeName;
    }

    public final String getTypeName() {
        return this.typeName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((typeName == null) ? 0 : typeName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Node other = (Node) obj;
        if (typeName == null) {
            if (other.typeName != null)
                return false;
        } else if (!typeName.equals(other.typeName))
            return false;
        return true;
    }
}
