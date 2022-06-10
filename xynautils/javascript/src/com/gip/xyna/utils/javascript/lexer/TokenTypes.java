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

/**
 * JavaScript tokens.
 *
 */
public enum TokenTypes {

    KEY_WORD,
    OPERATOR,
    SINGLE_LINE_COMMENT,
    MULTI_LINE_COMMENT,
    NEW_LINE,
    WHITE_SPACE,
    IDENTIFIER,
    STRING,
    NUMBER,
    PARENTHESIS_BEGIN,
    PARENTHESIS_END,
    SCOPE_BEGIN,
    SCOPE_END,
    ARRAY_BEGIN,
    ARRAY_END,
    SEMICOLON,
    REGEXP,
    OTHER; // this indicates a possible error in the JavaScript code.
}
