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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gip.xyna.utils.javascript.lexer.JavaScriptTokenReader;
import com.gip.xyna.utils.javascript.lexer.Token;
import com.gip.xyna.utils.javascript.lexer.TokenTypes;

/**
 * Tests the JavaScript token reader.
 *
 */
public final class JavaScriptTokenReaderTest {

    @Test
    public void testSimpleRead() throws IOException {
        String code = "// displays a testing alert.\nfunction test() {\n  alert(\"testing.\");\n}\n";
        StringReader reader = new StringReader(code);
        JavaScriptTokenReader tokenReader = new JavaScriptTokenReader(reader);
        List<Token> tokens = new ArrayList<Token>();
        Token token = null;
        while ((token = tokenReader.read()) != null) {
            tokens.add(token);
        }
        assertEquals(TokenTypes.SINGLE_LINE_COMMENT, tokens.get(0).getTokenType());
        assertEquals(TokenTypes.NEW_LINE, tokens.get(1).getTokenType());
        assertEquals(TokenTypes.KEY_WORD, tokens.get(2).getTokenType());
        assertEquals(TokenTypes.WHITE_SPACE, tokens.get(3).getTokenType());
        assertEquals(TokenTypes.IDENTIFIER, tokens.get(4).getTokenType());
        assertEquals(TokenTypes.PARENTHESIS_BEGIN, tokens.get(5).getTokenType());
        assertEquals(TokenTypes.PARENTHESIS_END, tokens.get(6).getTokenType());
        assertEquals(TokenTypes.WHITE_SPACE, tokens.get(7).getTokenType());
        assertEquals(TokenTypes.SCOPE_BEGIN, tokens.get(8).getTokenType());
        assertEquals(TokenTypes.NEW_LINE, tokens.get(9).getTokenType());
        assertEquals(TokenTypes.WHITE_SPACE, tokens.get(10).getTokenType());
        assertEquals(TokenTypes.IDENTIFIER, tokens.get(11).getTokenType());
        assertEquals(TokenTypes.PARENTHESIS_BEGIN, tokens.get(12).getTokenType());
        assertEquals(TokenTypes.STRING, tokens.get(13).getTokenType());
        assertEquals(TokenTypes.PARENTHESIS_END, tokens.get(14).getTokenType());
        assertEquals(TokenTypes.SEMICOLON, tokens.get(15).getTokenType());
        assertEquals(TokenTypes.NEW_LINE, tokens.get(16).getTokenType());
        assertEquals(TokenTypes.SCOPE_END, tokens.get(17).getTokenType());
        assertEquals(TokenTypes.NEW_LINE, tokens.get(18).getTokenType());
        assertEquals(19, tokens.size());
    }

    @Test
    public void testHandlingOfRegularExpression() throws IOException {
        String code = "function escapeQuotes(str) {\n  return str.replace(/\"/gi, '&quot;');\n}\n";
        StringReader reader = new StringReader(code);
        JavaScriptTokenReader tokenReader = new JavaScriptTokenReader(reader);
        List<Token> tokens = new ArrayList<Token>();
        Token token = null;
        while ((token = tokenReader.read()) != null) {
            tokens.add(token);
        }
        assertEquals(TokenTypes.KEY_WORD, tokens.get(0).getTokenType());
        assertEquals(TokenTypes.WHITE_SPACE, tokens.get(1).getTokenType());
        assertEquals(TokenTypes.IDENTIFIER, tokens.get(2).getTokenType());
        assertEquals(TokenTypes.PARENTHESIS_BEGIN, tokens.get(3).getTokenType());
        assertEquals(TokenTypes.IDENTIFIER, tokens.get(4).getTokenType());
        assertEquals(TokenTypes.PARENTHESIS_END, tokens.get(5).getTokenType());
        assertEquals(TokenTypes.WHITE_SPACE, tokens.get(6).getTokenType());
        assertEquals(TokenTypes.SCOPE_BEGIN, tokens.get(7).getTokenType());
        assertEquals(TokenTypes.NEW_LINE, tokens.get(8).getTokenType());
        assertEquals(TokenTypes.WHITE_SPACE, tokens.get(9).getTokenType());
        assertEquals(TokenTypes.KEY_WORD, tokens.get(10).getTokenType());
        assertEquals(TokenTypes.WHITE_SPACE, tokens.get(11).getTokenType());
        assertEquals(TokenTypes.IDENTIFIER, tokens.get(12).getTokenType());
        assertEquals(TokenTypes.OPERATOR, tokens.get(13).getTokenType());
        assertEquals(TokenTypes.IDENTIFIER, tokens.get(14).getTokenType());
        assertEquals(TokenTypes.PARENTHESIS_BEGIN, tokens.get(15).getTokenType());
        assertEquals(TokenTypes.REGEXP, tokens.get(16).getTokenType());
        assertEquals(TokenTypes.OPERATOR, tokens.get(17).getTokenType());
        assertEquals(TokenTypes.WHITE_SPACE, tokens.get(18).getTokenType());
        assertEquals(TokenTypes.STRING, tokens.get(19).getTokenType());
        assertEquals(TokenTypes.PARENTHESIS_END, tokens.get(20).getTokenType());
        assertEquals(TokenTypes.SEMICOLON, tokens.get(21).getTokenType());
        assertEquals(TokenTypes.NEW_LINE, tokens.get(22).getTokenType());
        assertEquals(TokenTypes.SCOPE_END, tokens.get(23).getTokenType());
        assertEquals(TokenTypes.NEW_LINE, tokens.get(24).getTokenType());
        assertEquals(25, tokens.size());
    }
}
