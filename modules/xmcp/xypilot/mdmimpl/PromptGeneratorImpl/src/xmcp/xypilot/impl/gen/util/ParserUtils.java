/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xmcp.xypilot.impl.gen.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserUtils {

    public static final String whitespace = " ";
    public static final String indent = whitespace.repeat(2);

    /**
     * Represents regexp that can be used to extract quoted text, that might be followed by a comma
     */
    public final static Pattern quotedPattern = Pattern.compile("\"(.*)\",?");

    /**
     * Extracts the contents within the quotation.
     *
     * Example  - "Hallo" -> Hallo
     * @param text
     * @return Enquoted text without surrounding quotations; null if input doesn't matches pattern
     */
    public static String getQuoted(String text) {
        Matcher m = quotedPattern.matcher(text);
        if (!m.matches() || m.groupCount() < 1) {
            return null;
        }

        return m.group(1);
    }

    /**
     * Reverts the escaping done by JsonUtils.escapeString
     * @param text
     * @return
     */
    public static String unescapeString(String text) {
        return text
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\\\", "\\");
    }
}
