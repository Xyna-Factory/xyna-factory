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
package com.gip.xyna.utils.javascript.checkstyle.rules;

import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.utils.javascript.checkstyle.Rule;
import com.gip.xyna.utils.javascript.checkstyle.TokenBuffer;
import com.gip.xyna.utils.javascript.checkstyle.ValidationFailureException;
import com.gip.xyna.utils.javascript.lexer.Token;
import com.gip.xyna.utils.javascript.lexer.TokenTypes;

/**
 * Checks that white space follows after some operators.
 *
 */
public final class WhiteSpaceOrNewLineAfterSomeOperatorsRule implements Rule {

    private static final Set<String> OPERATORS = createOperators();

    private static Set<String> createOperators() {
        Set<String> operators = new HashSet<String>();
        operators.add(",");
        operators.add(":");
        operators.add("!=");
        operators.add("!==");
        operators.add("=");
        operators.add("==");
        operators.add("===");
        operators.add("===");
        operators.add("<");
        operators.add("<=");
        operators.add(">");
        operators.add(">=");
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
        return operators;
    }

    public void checkForRuleViolations(final TokenBuffer buffer) throws ValidationFailureException {
        if (buffer.size() < 2) {
            return;
        }
        Token token = buffer.get(1);
        if (token.getTokenType() != TokenTypes.OPERATOR) {
            return;
        } else if (!OPERATORS.contains(token.getCode())) {
            return;
        }
        if (buffer.get(0).getTokenType() != TokenTypes.WHITE_SPACE
                && buffer.get(0).getTokenType() != TokenTypes.NEW_LINE) {
            throw new ValidationFailureException("Operator <" + token.getCode() + "> on line <" + token.getLineNumber()
                    + "> should be followed by white space or new line.");
        }
    }
}
