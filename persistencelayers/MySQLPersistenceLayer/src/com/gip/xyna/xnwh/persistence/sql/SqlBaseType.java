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
package com.gip.xyna.xnwh.persistence.sql;

public interface SqlBaseType<T extends Enum<T> & SqlBaseType<T>> {
    /**
     * gibt zurück, ob der übergebene typ größergleich ist. OTHER.isCompatibleTo(TIME) = false
     * NUMBER.isCompatibleTo(TEXT_ENCODED) = true
     * @param otherType
     * @return
     */
    public boolean isCompatibleTo(T otherType);

    public default T[] getValues() {
        return getEnumClass().getEnumConstants();
    }

    public Class<T> getEnumClass();

    default T valueFor(String name) {
        return Enum.valueOf(getEnumClass(), name);
    }

}
