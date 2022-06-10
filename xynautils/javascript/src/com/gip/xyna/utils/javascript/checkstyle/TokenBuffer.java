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
package com.gip.xyna.utils.javascript.checkstyle;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.javascript.lexer.Token;
import com.gip.xyna.utils.javascript.lexer.TokenTypes;

/**
 * Token buffer.
 *
 */
public final class TokenBuffer {

    private static final int MAX_CAPACITY = 10;

    private final List<Token> buffer = new ArrayList<Token>(MAX_CAPACITY);
    private final List<Token> filteredBuffer = new ArrayList<Token>(MAX_CAPACITY);

    /**
     * Adds a token to the buffer at index 0 and shifts all other elements one step.
     */
    public void push(final Token token) {
        if (token == null) {
            throw new IllegalArgumentException("Token may not be null.");
        }
        appendBuffer(buffer, token);
        if (token.isType(TokenTypes.MULTI_LINE_COMMENT)
                || token.isType(TokenTypes.SINGLE_LINE_COMMENT)) {
            appendFilteredBuffer(new Token(TokenTypes.WHITE_SPACE, token.getLineNumber(), token.getCode()));
        } else {
            appendFilteredBuffer(token);
        }
    }

    private static void appendBuffer(final List<Token> buffer, final Token token) {
        if (buffer.size() == MAX_CAPACITY) {
            buffer.remove(MAX_CAPACITY - 1);
        }
        buffer.add(0, token);
    }

    private void appendFilteredBuffer(final Token token) {
        if (filteredBuffer.size() > 0) {
            if (token.isType(TokenTypes.WHITE_SPACE)) {
                Token oldToken = filteredBuffer.get(0);
                if (oldToken.isType(TokenTypes.WHITE_SPACE)
                        || oldToken.isType(TokenTypes.NEW_LINE)) {
                    Token newToken = new Token(oldToken.getTokenType(), oldToken.getLineNumber(),
                            oldToken.getCode() + token.getCode());
                    filteredBuffer.set(0, newToken);
                    return;
                }
            } else if (token.isType(TokenTypes.NEW_LINE)) {
                Token oldToken = filteredBuffer.get(0);
                if (oldToken.isType(TokenTypes.WHITE_SPACE)
                        || oldToken.isType(TokenTypes.NEW_LINE)) {
                    Token newToken = new Token(token.getTokenType(), oldToken.getLineNumber(),
                            oldToken.getCode() + token.getCode());
                    filteredBuffer.set(0, newToken);
                    return;
                }
            }
        }
        appendBuffer(filteredBuffer, token);
    }

    /**
     * Gets the token at the specified index.
     * @param index index of token.
     * @return requested token.
     */
    public Token get(final int index) {
        return buffer.get(index);
    }

    /**
     * Gets the token at the specified index. Note that comments, new lines, and white space tokens are merged into new
     * lines and white space tokens. If a new line token is merged with any other token it becomes a new new line token
     * in all other cases white space tokens are the result.
     * @param index
     * @return requested token.
     */
    public Token getFiltered(final int index) {
        return filteredBuffer.get(index);
    }

    public int size() {
        return buffer.size();
    }

    public int sizeFiltered() {
        return filteredBuffer.size();
    }
}
