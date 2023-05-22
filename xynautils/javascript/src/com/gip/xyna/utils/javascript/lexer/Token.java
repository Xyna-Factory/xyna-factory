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
package com.gip.xyna.utils.javascript.lexer;

/**
 * JavaScript code token.
 *
 */
public final class Token {

    private final TokenTypes tokenType;
    private final int lineNumber;
    private final String code;

    public Token(final TokenTypes tokenType, final int lineNumber, final String code) {
        if (tokenType == null) {
            throw new IllegalArgumentException("Token type may not be null.");
        } else if (code == null) {
            throw new IllegalArgumentException("Code may not be null.");
        } else if (lineNumber < 1) {
            throw new IllegalArgumentException("Expected line number larger than one, but was: <"
                    + lineNumber + ">.");
        }
        this.tokenType = tokenType;
        this.code = code;
        this.lineNumber = lineNumber;
    }

    public TokenTypes getTokenType() {
        return this.tokenType;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public String getCode() {
        return this.code;
    }

    public boolean isType(final TokenTypes tokenType) {
        return this.tokenType.equals(tokenType);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.tokenType);
        sb.append(":");
        sb.append(this.lineNumber);
        sb.append(":<");
        sb.append(this.code);
        sb.append(">");
        return sb.toString();
    }
}
