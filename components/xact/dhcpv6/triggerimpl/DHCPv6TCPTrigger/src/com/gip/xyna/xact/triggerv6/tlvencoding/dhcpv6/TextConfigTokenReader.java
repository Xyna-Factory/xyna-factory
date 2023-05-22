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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

/**
 * Text config reader.
 */
public final class TextConfigTokenReader {

    private final LineNumberReader reader;

    public TextConfigTokenReader(final Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("Reader may not be null.");
        }
        this.reader = new LineNumberReader(reader);
    }

    public TextConfigToken read() throws ConfigFileReadException {
        while (true) {
            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                throw new ConfigFileReadException("Failed to read line.", e);
            }
            if (line == null) {
                return null;
            }
            return createToken(line, reader.getLineNumber());
        }
    }

    private static TextConfigToken createToken(final String line, final int lineNumber)
            throws ConfigFileReadException {
        String value = null;
        String indentedKey = line;
        if (line.contains(":")) {
            String[] parts = line.split(":", 2);
            indentedKey = parts[0];
            value = parts[1];
        }
        int spacesCount = countBeginningSpaces(indentedKey);
        if (spacesCount % 2 == 1) {
            throw new ConfigFileReadException("Expected an even amount of spaces at the beginning of line <"
                    + lineNumber + ">: <" + line + ">");
        }
        int level = spacesCount / 2;
        String key = indentedKey.substring(spacesCount);
        if ("".equals(key)) {
            throw new ConfigFileReadException("Expected key to not be an empty string at line <" + lineNumber + ">: <"
                    + line + ">");
        }
        return new TextConfigToken(level, key, value);
    }

    private static int countBeginningSpaces(final String str) {
        int count = 0;
        for (int i = 0; i < str.length(); ++i) {
            if (str.charAt(i) == ' ') {
                ++count;
            } else {
                break;
            }
        }
        return count;
    }

    public void close() throws IOException {
        reader.close();
    }
}
