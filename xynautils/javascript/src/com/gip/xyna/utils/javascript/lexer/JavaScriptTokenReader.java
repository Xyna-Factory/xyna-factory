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
package com.gip.xyna.utils.javascript.lexer;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.utils.javascript.lexer.patterns.IdentifierPattern;
import com.gip.xyna.utils.javascript.lexer.patterns.JavaScriptRegularExpressionPattern;
import com.gip.xyna.utils.javascript.lexer.patterns.KeyWordPattern;
import com.gip.xyna.utils.javascript.lexer.patterns.MultiLineCommentPattern;
import com.gip.xyna.utils.javascript.lexer.patterns.NumberPattern;
import com.gip.xyna.utils.javascript.lexer.patterns.OperatorPattern;
import com.gip.xyna.utils.javascript.lexer.patterns.RegExpPattern;
import com.gip.xyna.utils.javascript.lexer.patterns.SingleLineCommentPattern;
import com.gip.xyna.utils.javascript.lexer.patterns.StringLiteralPattern;
import com.gip.xyna.utils.javascript.lexer.patterns.StringPattern;

/**
 * JavaScript token reader. Handling of JavaScript regular expressions is problematic and in this
 * implementation they are handled by looking at the previous token - this covers most but not all
 * cases. Note also that this lexer is not implemented with performance in mind. JavaCC and similar
 * solutions should give a lot better performance.
 *
 */
public final class JavaScriptTokenReader {

    public static final String NEW_LINE_REGEXP = "(\\n\\r?)|[\\r\\u0085\\u2028\\u2029]";

    private static final Map<TokenTypes, Pattern> PATTERNS = createPatterns();
    private static final Set<TokenTypes> INSIGNIFICANT_TOKEN_TYPES
        = createSetOfInsignificantTokenTypes();
    private static final Pattern REGULAR_EXPRESSION = new JavaScriptRegularExpressionPattern();

    private static Map<TokenTypes, Pattern> createPatterns() {
        Map<TokenTypes, Pattern> map = new HashMap<TokenTypes, Pattern>();
        map.put(TokenTypes.KEY_WORD, new KeyWordPattern());
        map.put(TokenTypes.OPERATOR, new OperatorPattern());
        map.put(TokenTypes.SINGLE_LINE_COMMENT, new SingleLineCommentPattern());
        map.put(TokenTypes.MULTI_LINE_COMMENT, new MultiLineCommentPattern());
        map.put(TokenTypes.NEW_LINE, new RegExpPattern(NEW_LINE_REGEXP));
        map.put(TokenTypes.WHITE_SPACE, new RegExpPattern("[ \\t\\x0B\\f]+"));
        map.put(TokenTypes.IDENTIFIER, new IdentifierPattern());
        map.put(TokenTypes.STRING, new StringPattern());
        map.put(TokenTypes.NUMBER, new NumberPattern());
        map.put(TokenTypes.PARENTHESIS_BEGIN, new StringLiteralPattern("("));
        map.put(TokenTypes.PARENTHESIS_END, new StringLiteralPattern(")"));
        map.put(TokenTypes.SCOPE_BEGIN, new StringLiteralPattern("{"));
        map.put(TokenTypes.SCOPE_END, new StringLiteralPattern("}"));
        map.put(TokenTypes.ARRAY_BEGIN, new StringLiteralPattern("["));
        map.put(TokenTypes.ARRAY_END, new StringLiteralPattern("]"));
        map.put(TokenTypes.SEMICOLON, new StringLiteralPattern(";"));
        return Collections.unmodifiableMap(map);
    }

    private static Set<TokenTypes> createSetOfInsignificantTokenTypes() {
        Set<TokenTypes> types = new HashSet<TokenTypes>();
        types.add(TokenTypes.WHITE_SPACE);
        types.add(TokenTypes.SINGLE_LINE_COMMENT);
        types.add(TokenTypes.MULTI_LINE_COMMENT);
        // Ignored: JS sometimes allows a complete line of code not to be ended with semicolon
        types.add(TokenTypes.NEW_LINE);
        return types;
    }

    private final LineNumberReader reader;
    private StringBuffer currentToken = new StringBuffer();
    private int currentTokenStartLine = -1;
    private Token lastSignificantTokenRead = null;

    public JavaScriptTokenReader(final Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("Reader may not be null.");
        }
        this.reader = new LineNumberReader(reader);
    }

    public Token read() throws IOException {
        while (true) {
            int charStartLine = reader.getLineNumber() + 1;  
            int characterValue = reader.read();
            if (characterValue < 0) {
                if (currentToken.length() < 1) {
                    return null;
                } else {
                    String code = currentToken.toString();
                    currentToken = new StringBuffer();
                    return createToken(code, currentTokenStartLine);
                }
            }
            if (currentToken.length() < 1) {
                this.currentTokenStartLine = charStartLine;
            }
            char character = (char) characterValue;
            currentToken.append(character);
            if (findPrefixMatches(currentToken.toString()).size() < 1) {
                int lineNumber;
                String code;
                if (currentToken.length() > 1) {
                    code = currentToken.deleteCharAt(currentToken.length() - 1).toString();
                    lineNumber = this.currentTokenStartLine;
                    this.currentTokenStartLine = charStartLine;
                    currentToken = new StringBuffer();
                    currentToken.append(character);
                } else {
                    code = currentToken.toString();
                    currentToken = new StringBuffer();
                    lineNumber = this.currentTokenStartLine;
                }
                return createToken(code, lineNumber);
            }
        }
    }

    private Token createToken(final String code, final int lineNumber) {
        Token token = new Token(findCompleteMatch(code), lineNumber, code);
        if (!INSIGNIFICANT_TOKEN_TYPES.contains(token.getTokenType())) {
            lastSignificantTokenRead = token;
        }
        return token;
    }

    private Set<TokenTypes> findPrefixMatches(final String prefix) {
        Set<TokenTypes> matches = new HashSet<TokenTypes>();
        for (Map.Entry<TokenTypes, Pattern> entry : PATTERNS.entrySet()) {
            if (entry.getValue().isMatchingPrefix(prefix)) {
                matches.add(entry.getKey());
            }
        }
        // JS regular expressions are designed in a very nasty way - this code handles most cases
        if (prefix.startsWith("/")) {
            if (lastSignificantTokenMayBeFollowedByRegularExpression()) {
                if (REGULAR_EXPRESSION.isMatchingPrefix(prefix)) {
                    matches.add(TokenTypes.REGEXP);
                }
            }
        }
        return matches;
    }

    private boolean lastSignificantTokenMayBeFollowedByRegularExpression() {
        if (lastSignificantTokenRead == null) {
            return true;
        }
        TokenTypes type = lastSignificantTokenRead.getTokenType();
        if (type.equals(TokenTypes.IDENTIFIER)) {
            return false;
        } else if (type.equals(TokenTypes.NUMBER)) {
            return false;
        } else if (type.equals(TokenTypes.REGEXP)) {
            return false;
        } else if (type.equals(TokenTypes.STRING)) {
            return false;
        } else if (type.equals(TokenTypes.ARRAY_END)) {
            return false;
        } else if (type.equals(TokenTypes.PARENTHESIS_END)) {
            return false;
        } else if (type.equals(TokenTypes.SCOPE_END)) {
            return false;
        } else if (type.equals(TokenTypes.OPERATOR)) {
            String code = lastSignificantTokenRead.getCode();
            if (code.equals("++") || code.equals("--")) {
                return false;
            }
        } else if (type.equals(TokenTypes.KEY_WORD)) {
            String code = lastSignificantTokenRead.getCode();
            if (code.equals("true") || code.equals("false") || code.equals("null") || code.equals("this")) {
                return false;
            }
        }
        return true;
    }

    public static TokenTypes findCompleteMatch(final String code) {
        TokenTypes tokenType = TokenTypes.OTHER; 
        for (Map.Entry<TokenTypes, Pattern> entry : PATTERNS.entrySet()) {
            if (entry.getValue().isCompleteMatch(code)) {
                if (tokenType != TokenTypes.OTHER) {
                    throw new IllegalStateException("Found more than one match for <" + code + ">: "
                        + tokenType + " and " + entry.getKey() + ".");
                }
                tokenType = entry.getKey();
            }
        }
        if (tokenType == TokenTypes.OTHER) {
            if (REGULAR_EXPRESSION.isCompleteMatch(code)) {
                tokenType = TokenTypes.REGEXP;
            }
        }
        return tokenType;
    }
}
