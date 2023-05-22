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
package com.gip.xyna.utils.javascript.lexer.patterns;

import com.gip.xyna.utils.javascript.lexer.Pattern;

/**
 * String literal token pattern.
 *
 */
public final class StringLiteralPattern implements Pattern {

    private final String stringLiteral;

    public StringLiteralPattern(final String stringLiteral) {
        if (stringLiteral == null) {
            throw new IllegalArgumentException("String literal may not be null.");
        }
        this.stringLiteral = stringLiteral;
    }

    public boolean isMatchingPrefix(final String tokenString) {
        return stringLiteral.startsWith(tokenString);
    }

    public boolean isCompleteMatch(final String tokenString) {
        return stringLiteral.equals(tokenString);
    }
}
