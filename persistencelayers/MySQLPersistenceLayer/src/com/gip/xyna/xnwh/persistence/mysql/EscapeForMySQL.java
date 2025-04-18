/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.persistence.mysql;

import com.gip.xyna.xnwh.selection.parsing.SelectionParser.EscapeParameters;

class EscapeForMySQL implements EscapeParameters {

    public String escapeForLike(String toEscape) {
        if (toEscape == null) {
            return toEscape;
        }

        toEscape = toEscape.replaceAll("%", "\\\\%");
        toEscape = toEscape.replaceAll("_", "\\\\_");
        return toEscape;
    }

    @Override
    public String getMultiCharacterWildcard() {
        return "%";
    }

    @Override
    public String getSingleCharacterWildcard() {
        return "_";
    }

}