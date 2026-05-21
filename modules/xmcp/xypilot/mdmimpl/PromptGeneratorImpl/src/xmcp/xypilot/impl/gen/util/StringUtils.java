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


public class StringUtils {

    // literals are numbers and booleans
    public static final String LITERAL_REGEX = "[\\+-]?(?:\\d+\\.?\\d*|\\d*\\.?\\d+)[dDfFlL]?|true|false";

    // a string literal is a string that is surrounded by double quotes
    // group1: the string without the quotes
    public static final String STRING_REGEX = "\"((?:[^\"\\\\]|\\\\.)*)\"";

    /**
     * Capitalizes the first letter of the string. Returns the empty string if the input is empty.
     * @param s
     * @return
     */
    public static String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }


    /**
     * Decapitalizes the first letter of the string. Returns the empty string if the input is empty.
     * @param s
     * @return
     */
    public static String decapitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toLowerCase() + s.substring(1);
    }


    /**
     * Escapes quotes in the string.
     * @param s
     * @return
     */
    public static String escapeQuotes(String s) {
        return s.replace("\"", "\\\"");
    }


    /**
     * Removes all empty lines from the given string.
     * @param s
     * @return
     */
    public static String removeEmptyLines(String s) {
        return s.replaceAll("(?m)^[ \t]*\r?\n", "");
    }


    /**
     * Trims the given label and replaces all non-word characters with an underscore.
     * The first character is set to lower case.
     */
    public static String toIdentifier(String label) {
        return decapitalize(label.trim()).replaceAll("\\W+", "_");
    }

}
