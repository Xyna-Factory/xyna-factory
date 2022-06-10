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
package com.gip.xyna.utils.javascript.checkstyle.rules;

import com.gip.xyna.utils.javascript.checkstyle.Rule;
import com.gip.xyna.utils.javascript.checkstyle.TokenBuffer;
import com.gip.xyna.utils.javascript.checkstyle.ValidationFailureException;
import com.gip.xyna.utils.javascript.lexer.Token;
import com.gip.xyna.utils.javascript.lexer.TokenTypes;

/**
 * Checks for occurrence of a lone starting bracket, i.e. '{', '[' or '(', on a line.
 *
 */
public final class NoLoneStartingBracketOnALineRule implements Rule {

    public void checkForRuleViolations(final TokenBuffer buffer) throws ValidationFailureException {
        if (buffer.sizeFiltered() < 3) {
            return;
        }
        Token token = buffer.getFiltered(1);
        if (token.isType(TokenTypes.SCOPE_BEGIN) || token.isType(TokenTypes.ARRAY_BEGIN)
                || token.isType(TokenTypes.PARENTHESIS_BEGIN)) {
            if (buffer.getFiltered(0).isType(TokenTypes.NEW_LINE)
                    && buffer.getFiltered(2).isType(TokenTypes.NEW_LINE)) {
                throw new ValidationFailureException("Found a lone <" + token.getCode() + "> at line <"
                        + token.getLineNumber()  + ">.");
            }
        }
    }
}
