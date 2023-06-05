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
package com.gip.xyna.utils.javascript.checkstyle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.utils.javascript.checkstyle.rules.NoLoneStartingBracketOnALineRule;
import com.gip.xyna.utils.javascript.checkstyle.rules.NoNewLineBeforeElseRule;
import com.gip.xyna.utils.javascript.checkstyle.rules.NoOtherTokensRule;
import com.gip.xyna.utils.javascript.checkstyle.rules.WhiteSpaceAfterSomeKeyWordsRule;
import com.gip.xyna.utils.javascript.checkstyle.rules.WhiteSpaceBeforeSingleLineCommentRule;
import com.gip.xyna.utils.javascript.checkstyle.rules.WhiteSpaceBeforeSomeOperatorsRule;
import com.gip.xyna.utils.javascript.checkstyle.rules.WhiteSpaceOrNewLineAfterSomeOperatorsRule;
import com.gip.xyna.utils.javascript.lexer.JavaScriptTokenReader;
import com.gip.xyna.utils.javascript.lexer.Token;
import com.gip.xyna.utils.javascript.lexer.TokenTypes;

/**
 * JavaScript code style checker. 
 *
 */
public final class JavaScriptCheckStyle {

    private static final Set<Rule> RULES = createRules();

    private static Set<Rule> createRules() {
        Set<Rule> rules = new HashSet<Rule>();
        rules.add(new WhiteSpaceAfterSomeKeyWordsRule());
        rules.add(new NoOtherTokensRule());
        rules.add(new NoLoneStartingBracketOnALineRule());
        rules.add(new WhiteSpaceBeforeSomeOperatorsRule());
        rules.add(new WhiteSpaceOrNewLineAfterSomeOperatorsRule());
        rules.add(new WhiteSpaceBeforeSingleLineCommentRule());
        rules.add(new NoNewLineBeforeElseRule());
        return Collections.unmodifiableSet(rules);
    }

    private final int maxLineLength;
    private final String charSet;

    public JavaScriptCheckStyle(int lineLength, String charSet) {
        if (lineLength < 1) {
            throw new IllegalArgumentException("Expected line length larger than zero, but was: <" + lineLength + ">");
        } else if (charSet == null) {
            throw new IllegalArgumentException("Character set may not be null.");
        }
        this.maxLineLength = lineLength;
        this.charSet = charSet;
    }

    public void validateFile(final File file) throws ValidationFailureException {
        doSimpleFileValidation(file);
        doAdvancedFileValidation(file);
    }

    // TODO: think about removing this once the advanced validator can handle everything that this method does.
    private void doSimpleFileValidation(final File file) throws ValidationFailureException {
        LineNumberReader lineReader;
        try {
            lineReader = new LineNumberReader(new InputStreamReader(new FileInputStream(file), charSet));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File not found: <" + file + ">.", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("File encoding not supported.", e);
        }
        try {
            String line;
            while ((line = lineReader.readLine()) != null) {
                validateLine(line, lineReader.getLineNumber());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading file <" + file + ">.", e);
        } finally {
            try {
                lineReader.close();
            } catch (IOException e) {
                throw new IllegalStateException("Error while closing file input stream.", e);
            }
        }
    }

    private void validateLine(final String line, final int lineNumber)
            throws ValidationFailureException {
        if (line.length() > this.maxLineLength) {
            throw new ValidationFailureException("Line <" + lineNumber
                    + "> is too long, expected value less or equal to <" + this.maxLineLength
                    + ">, but was <" + line.length() + ">.");
        } else if (line.contains("\t")) {
            throw new ValidationFailureException("Line <" + lineNumber
                    + "> contains one or more tab characters.");            
        } else if (line.matches(".*\\s+")) {
            throw new ValidationFailureException("Line <" + lineNumber
                    + "> contains unneeded white space at the end of the line.");
        }
    }

    private void doAdvancedFileValidation(final File file) throws ValidationFailureException {
        Reader reader;
        try {
            reader = new InputStreamReader(new FileInputStream(file), this.charSet);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File not found: <" + file + ">.", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("File encoding not supported.", e);
        }
        try {
            JavaScriptTokenReader tokenReader = new JavaScriptTokenReader(reader);
            TokenBuffer buffer = new TokenBuffer();
            Token token = null;
            while ((token = tokenReader.read()) != null) {
                buffer.push(token);
                validateTokenBuffer(buffer);
            }
            if (buffer.size() > 1) {
                // TODO: think about a better way to handle rules at the end of the file.
                if (buffer.get(0).getTokenType() != TokenTypes.NEW_LINE) {
                    throw new ValidationFailureException("File does not end with a new line.");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading file <" + file + ">.", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new IllegalStateException("Error while closing file input stream.", e);
            }
        }
    }

    private void validateTokenBuffer(TokenBuffer buffer) throws ValidationFailureException {
        for (Rule rule : RULES) {
            rule.checkForRuleViolations(buffer);
        }
    }
}
