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

import java.util.ArrayList;
import java.util.List;

public interface SqlType<T extends Enum<T> & SqlType<T, B>, B extends Enum<B> & SqlBaseType<B>> {

    public boolean isDependentOnSizeSpecification();

    public long getSize();

    public B getBaseType();

    /**
     * gibt zu einem typ alle damit kompatiblen typen zurück (die "größer" sind).
     * beispiel: BLOB.getCompatibleTypes =>
     * BLOB, MEDIUMBLOB, LONGBLOB
     * 
     */
    public default List<T> getCompatibleTypes() {
        List<T> types = new ArrayList<T>();
        for (T type : getValues()) {
            if (getBaseType().isCompatibleTo(type.getBaseType())) {
                if (getSize() <= type.getSize()) {
                    types.add(type);
                }
            }
        }
        return types;
    }

    public default T[] getValues() {
        return getEnumClass().getEnumConstants();
    }

    public Class<T> getEnumClass();

    default T valueFor(String name) {
        return Enum.valueOf(getEnumClass(), name);
    }
}
