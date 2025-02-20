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

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

//FIXME duplicated class from OraclePL
class ResultSetReaderWrapper<T> implements ResultSetReader<T> {

    private final com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T> innerReader;
    private final boolean zippedBlobs;
    @SuppressWarnings("rawtypes")
    private Class<? extends Storable> storableClass = null;

    public ResultSetReaderWrapper(com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T> reader,
            boolean zippedBlobs) {
        this.innerReader = reader;
        this.zippedBlobs = zippedBlobs;
    }

    public ResultSetReaderWrapper(com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T> reader,
            boolean zippedBlobs, @SuppressWarnings("rawtypes") Class<? extends Storable> storableClass) {
        this(reader, zippedBlobs);
        this.storableClass = storableClass;
    }

    public T read(ResultSet rs) throws SQLException {
        if (zippedBlobs) {
            return innerReader.read(new ResultSetWrapperReadingZippedBlobs(rs, storableClass));
        } else {
            return innerReader.read(rs);
        }
    }

}