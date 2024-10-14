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
package com.gip.xyna.utils.javascript.checkstyle.rules;

import com.gip.xyna.utils.javascript.checkstyle.Rule;
import com.gip.xyna.utils.javascript.checkstyle.TokenBuffer;
import com.gip.xyna.utils.javascript.checkstyle.ValidationFailureException;
import com.gip.xyna.utils.javascript.lexer.Token;
import com.gip.xyna.utils.javascript.lexer.TokenTypes;

/**
 * Checks that 'else' is not preceded by a new line, i.e. the ending bracket - '}' - of the preceding 'if' should be on
 * the same line.
 *
 */
public final class NoNewLineBeforeElseRule implements Rule {

    public void checkForRuleViolations(final TokenBuffer buffer) throws ValidationFailureException {
        if (buffer.sizeFiltered() < 2) {
            return;
        }
        Token token = buffer.getFiltered(0);
        if (token.isType(TokenTypes.KEY_WORD) && token.getCode().equals("else")) {
            if (buffer.getFiltered(1).isType(TokenTypes.NEW_LINE)) {
                throw new ValidationFailureException("Key word <else> on line <" + token.getLineNumber()
                        + "> should not be preceded by a new line.");
            }
        }
    }
}
