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
package com.gip.xyna.xact.triggerv6.tlvencoding.utilv6;

/**
 * Quoted string format util.
 */
public final class QuotedStringFormatUtil {

    private QuotedStringFormatUtil() {
    }

    public static boolean isQuoteFormat(final String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value may not be null.");
        }
        return value.matches("\"((\\\\[\"\\\\])|[^\"\\\\])*\"");
    }

    public static String unquote(final String value) {
        if (!isQuoteFormat(value)) {
            throw new IllegalArgumentException("Expected quoted string value, but got: <" + value + ">.");
        }
        String result = value.substring(1, value.length() - 1);
        result = result.replace("\\\\", "\\b");
        result = result.replace("\\\"", "\"");
        return result.replace("\\b", "\\");
    }
}
