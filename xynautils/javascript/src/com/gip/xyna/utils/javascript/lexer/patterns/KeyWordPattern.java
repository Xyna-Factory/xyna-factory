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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.utils.javascript.lexer.Pattern;

/**
 * Key word pattern.
 *
 */
public class KeyWordPattern implements Pattern {

    static final Set<String> KEY_WORDS = KeyWordPattern.createKeyWords();

    private static Set<String> createKeyWords() {
        Set<String> keyWords = new HashSet<String>();
        keyWords.add("break");
        keyWords.add("case");
        keyWords.add("catch");
        keyWords.add("continue");
        keyWords.add("default");
        keyWords.add("delete");
        keyWords.add("do");
        keyWords.add("else");
        keyWords.add("finally");
        keyWords.add("for");
        keyWords.add("function");
        keyWords.add("if");
        keyWords.add("in");
        keyWords.add("instanceof");
        keyWords.add("new");
        keyWords.add("return");
        keyWords.add("switch");
        keyWords.add("this");
        keyWords.add("throw");
        keyWords.add("try");
        keyWords.add("typeof");
        keyWords.add("var");
        keyWords.add("void");
        keyWords.add("while");
        keyWords.add("with");
        return Collections.unmodifiableSet(keyWords);
    }

    public boolean isMatchingPrefix(final String prefix) {
        for (String keyWord : KEY_WORDS) {
            if (keyWord.startsWith(prefix)) {
                return true;
            }
        }
        return prefix.equals("");
    }

    public boolean isCompleteMatch(final String tokenString) {
        return KEY_WORDS.contains(tokenString);
    }
}
