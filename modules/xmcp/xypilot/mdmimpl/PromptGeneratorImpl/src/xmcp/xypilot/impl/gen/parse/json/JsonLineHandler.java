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

/**
 * Interface for a handler that is called by the JsonLineParser for each line of a json string.
 */
public interface JsonLineHandler {
    public default void startParsing() {}
    public default void property(String key, String value) {}
    public default void objectStart(Optional<String> key) {}
    public default void objectEnd() {}
    public default void arrayStart(Optional<String> key) {}
    public default void arrayEnd() {}
    public default void endParsing() {}
}
