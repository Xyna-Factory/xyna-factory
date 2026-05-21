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
package xmcp.xypilot.impl.gen.parse.json;

import java.util.Optional;

public class JsonLineParser {

    private static void parseLine(String line, JsonLineHandler handler) {
        // preprocess line
        line = line.trim();
        if (line.endsWith(",")) {
            line = line.substring(0, line.length() - 1);
        }

        // empty line or comment
        if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) {
            return;
        }

        // end of object
        if (line.startsWith("}")) {
            handler.objectEnd();
            return;
        }

        // start of object
        if (line.startsWith("{")) {
            handler.objectStart(Optional.empty());
            return;
        }

        // start of array
        if (line.startsWith("[")) {
            handler.arrayStart(Optional.empty());
            return;
        }

        // end of array
        if (line.startsWith("]")) {
            handler.arrayEnd();
            return;
        }

        // object, array or primitive property
        String[] parts = line.split(":", 2);
        if (parts.length == 2) {
            String key = parts[0].trim();
            String value = parts[1].trim();

            if (key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }

            if (value.equals("[")) {
                handler.arrayStart(Optional.of(key));
            } else if (value.equals("{")) {
                handler.objectStart(Optional.of(key));
            } else {
                handler.property(key, value);
            }
        }
    }

    public static void parse(String json, JsonLineHandler handler) {
        String[] lines = json.split("\n");

        handler.startParsing();
        for (String line : lines) {
            parseLine(line, handler);
        }
        handler.endParsing();
    }

}
