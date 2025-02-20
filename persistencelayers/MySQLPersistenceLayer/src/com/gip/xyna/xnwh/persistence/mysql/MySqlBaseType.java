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
package com.gip.xyna.xnwh.persistence.mysql;

enum MySqlBaseType {
    NUMBER, FLOAT, TIME, TEXT_ENCODED, BINARY, OTHER;

    /**
     * gibt zurück, ob der übergebene typ größergleich ist.
     * OTHER.isCompatibleTo(TIME) = false
     * NUMBER.isCompatibleTo(TEXT_ENCODED) = true
     * 
     * @param otherType
     * @return
     */
    public boolean isCompatibleTo(MySqlBaseType otherType) {
        if (this == otherType) {
            return true;
        }
        if (this == NUMBER) {
            return otherType == FLOAT || otherType == TEXT_ENCODED || otherType == BINARY;
        } else if (this == FLOAT) {
            return otherType == TEXT_ENCODED || otherType == BINARY;
        } else if (this == TIME) {
            return otherType == TEXT_ENCODED || otherType == BINARY;
        } else if (this == TEXT_ENCODED) {
            return otherType == BINARY;
        } else if (this == BINARY) {
            return otherType == TEXT_ENCODED;
        } else
            return false;
    }
}