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
 * Checks that single line comments are preceded by white space or new line.
 *
 */
public class WhiteSpaceBeforeSingleLineCommentRule implements Rule  {

    public void checkForRuleViolations(final TokenBuffer buffer) throws ValidationFailureException {
        if (buffer.size() < 2) {
            return;
        }
        Token token = buffer.get(0);
        if (token.getTokenType() != TokenTypes.SINGLE_LINE_COMMENT) {
            return;
        }
        if (buffer.get(1).getTokenType() != TokenTypes.WHITE_SPACE
                && buffer.get(1).getTokenType() != TokenTypes.NEW_LINE) {
            throw new ValidationFailureException("Expected white space, or new line, before comment at line <"
                    + token.getLineNumber() + ">.");
        }
    }
}
