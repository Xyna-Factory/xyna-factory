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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonUtils {

    @SuppressWarnings("unchecked")
    public static String toJSON(Object value) {
        if (value instanceof String) {
            return toJSONValue((String) value);
        } else if (value instanceof Number) {
            return toJSONValue((Number) value);
        } else if (value instanceof List) {
            return toJSONArray((List<Object>) value);
        } else if (value instanceof Map) {
            return toJSONObject((Map<String, Object>) value);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + value.getClass().getName());
        }
    }


    public static String toJSONArray(List<Object> values) {
        StringBuilder ret = new StringBuilder("[");

        ret.append(
            values.stream()
            .map(JsonUtils::toJSON)
            .collect(Collectors.joining(","))
        );

        ret.append("]");
        return ret.toString();
    }


    public static String toJSONValue(String value) {
        return "\"" + escapeString(value) + "\"";
    }


    public static String toJSONValue(Number value) {
        return value.toString();
    }


    public static String toJSONObject(Map<String, Object> values) {
        StringBuilder ret = new StringBuilder("{");

        ret.append(
            values.entrySet()
            .stream()
            .map((entry) -> "\"" + entry.getKey() + "\":" + toJSON(entry.getValue()))
            .collect(Collectors.joining(","))
        );

        ret.append("}");
        return ret.toString();
    }


    /**
     * Escapes newlines and double-quotes so the given text can be used in JSON
     * @param text
     * @return
     */
    public static String escapeString(String text) {
        if (text == null) {
          return "";
        }
        return text
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
            .replace("\"", "\\\"");
    }

}
