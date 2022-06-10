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
package com.gip.xyna.utils.javascript.lexer.patterns;

import com.gip.xyna.utils.javascript.lexer.Pattern;

/**
 * Regular expression matching token pattern.
 *
 */
public class RegExpPattern implements Pattern {

    private final String regexp;

    public RegExpPattern(final String regexp) {
        if (regexp == null) {
            throw new IllegalArgumentException("Regular-expression may not be null.");
        }
        this.regexp = regexp;
    }

    public boolean isMatchingPrefix(final String tokenString) {
        return tokenString.matches(this.regexp) || tokenString.equals("");
    }

    public boolean isCompleteMatch(final String tokenString) {
        return tokenString.matches(this.regexp);
    }
}
