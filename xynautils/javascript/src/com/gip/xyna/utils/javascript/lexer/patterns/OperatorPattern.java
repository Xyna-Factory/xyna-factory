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
package com.gip.xyna.utils.javascript.lexer.patterns;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.utils.javascript.lexer.Pattern;

/**
 * Operator pattern.
 *
 */
public final class OperatorPattern implements Pattern {

    private static final Set<String> OPERATORS = createOperators();

    private static Set<String> createOperators() {
        Set<String> operators = new HashSet<String>();
        operators.add(".");
        operators.add(",");
        operators.add(":");
        operators.add("+");
        operators.add("++");
        operators.add("-");
        operators.add("--");
        operators.add("*");
        operators.add("/");
        operators.add("%");
        operators.add("!=");
        operators.add("!==");
        operators.add("=");
        operators.add("==");
        operators.add("===");
        operators.add("===");
        operators.add("~");
        operators.add("!");
        operators.add("<<");
        operators.add(">>");
        operators.add(">>>");
        operators.add("<");
        operators.add("<=");
        operators.add(">");
        operators.add(">=");
        operators.add("&");
        operators.add("^");
        operators.add("|");
        operators.add("&&");
        operators.add("||");
        operators.add("?");
        operators.add("+=");
        operators.add("-=");
        operators.add("*=");
        operators.add("/=");
        operators.add("%=");
        operators.add("^=");
        operators.add("|=");
        operators.add("&=");
        return Collections.unmodifiableSet(operators);
    }

    public boolean isMatchingPrefix(final String prefix) {
        for (String operator : OPERATORS) {
            if (operator.startsWith(prefix)) {
                return true;
            }
        }
        return prefix.equals("");
    }

    public boolean isCompleteMatch(final String tokenString) {
        return OPERATORS.contains(tokenString);
    }
}
