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

import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseColumnInfo;

public abstract class SqlColumnInfo<T extends Enum<T> & SqlType<T, B>, B extends Enum<B> & SqlBaseType<B>> extends DatabaseColumnInfo {

    private IndexType indexType;
    private Class<?> clazz;

    public Class<?> getStorableClass() {
        return clazz;
    }

    private T type; // uppercase

    public void setIndexType(IndexType indexType) {
        this.indexType = indexType;
    }

    public void setType(T type) {
        this.type = type;
    }

    public T getType() {
        return type;
    }

    @Override
    public String getTypeAsString() {
        return type != null ? type.toString() : "<null>";
    }

    public IndexType getIndexType() {
        return indexType;
    }

    public void setStorableClass(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean isTypeDependentOnSizeSpecification() {
        if (type == null) {
            throw new RuntimeException("Type has not been set.");
        }
        return type.isDependentOnSizeSpecification();
    }

    //public abstract ResultSetReader<? extends SqlColumnInfo<T, B>> getResultSetReader(final String tableName);

}