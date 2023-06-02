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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;


/**
 * Text config tree reader.
 */
public final class TextConfigTreeReader {

    private final TextConfigTokenReader reader;
    private TextConfigToken pendingToken = null;
    private boolean isTreeReadAlready = false;

    public TextConfigTreeReader(final Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("Reader may not be null.");
        }
        this.reader = new TextConfigTokenReader(reader);
    }

    public TextConfigTree read() throws ConfigFileReadException {
        if (this.isTreeReadAlready) {
            throw new UnsupportedOperationException("Read has already been done.");
        }
        this.isTreeReadAlready = true;
        List<Node> nodes = new ArrayList<Node>();
        while (true) {
            Node node = readRootNode();
            if (node == null) {
                return new TextConfigTree(nodes);
            }
            nodes.add(node);
        }
    }

    public void close() throws IOException {
        reader.close();
    }

    private Node readRootNode() throws ConfigFileReadException {
        TextConfigToken token = readToken();
        if (token == null) {
            return null;
        } else if (token.getLevel() != 0) {
            throw new ConfigFileReadException("Found token at wrong level: <" + token + ">");
        }
        return transformToNode(token);
    }

    private TextConfigToken readToken() throws ConfigFileReadException {
        if (this.pendingToken != null) {
            TextConfigToken token = this.pendingToken;
            this.pendingToken = null;
            return token;
        }
        return reader.read();
    }

    private void putBackToken(final TextConfigToken token) {
        this.pendingToken = token;
    }

    private Node transformToNode(final TextConfigToken token) throws ConfigFileReadException {
        if (token.getValue() == null) {
            List<Node> subNodes = new ArrayList<Node>();
            while (true) {
                TextConfigToken otherToken = readToken();
                if (otherToken == null) {
                    break;
                } else if (otherToken.getLevel() == token.getLevel() + 1) {
                    subNodes.add(transformToNode(otherToken));
                } else {
                    putBackToken(otherToken);
                    break;
                }
            }
            return new TypeOnlyNode(token.getKey(), subNodes);
        } else {
            return new TypeWithValueNode(token.getKey(), token.getValue());
        }
    }
}
