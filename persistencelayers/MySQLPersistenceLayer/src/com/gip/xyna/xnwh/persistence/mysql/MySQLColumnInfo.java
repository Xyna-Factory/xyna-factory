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

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.sql.SqlColumnInfo;

class MySQLColumnInfo extends SqlColumnInfo<MySqlType, MySqlBaseType> {

    static final Logger logger = CentralFactoryLogging.getLogger(MySQLColumnInfo.class);

    final static String SELECT_COLUMN_NAME_AND_DATATYPE_SQL = "select column_name, data_type, character_maximum_length, column_key from information_schema.columns"
            + " where table_schema = database() and table_name= ?";
    static final String DOES_TABLE_EXIST_SQL = "select count(*) from information_schema.tables where table_schema = ? and table_name = ?";
    static final String DOES_INDEX_EXIST_FOR_TABLE_SQL = "SELECT count(*) FROM information_schema.statistics WHERE index_name = ? AND table_name = ?";

    MySQLColumnInfo next; // verkettete Liste, wenn mehrere Einträge zu einer Tabellenspalte existieren

    @Override
    public String toString() {
        return "MySQLColumnInfo(clazzloader=" + (getStorableClass() == null ? "?" : getStorableClass().getClassLoader()) + ", type=" + getType()
                + ",indexType=" + getIndexType() + ",next=" + next + ")";
    }

    private static IndexType getIndexTypeByString(String column_key) {
        if (column_key == null || column_key.equals("")) {
            return IndexType.NONE;
        } else if (column_key.equals("PRI")) {
            return IndexType.PRIMARY;
        } else if (column_key.equals("MUL")) {
            return IndexType.MULTIPLE;
        } else if (column_key.equals("UNI")) {
            return IndexType.UNIQUE;
        } else {
            throw new RuntimeException("Invalid response from database");
        }
    }

    /**
     * @param tableName
     * @return
     */
    public static ResultSetReader<MySQLColumnInfo> getResultSetReader(final String tableName) {
        return new ResultSetReader<MySQLColumnInfo>() {

            public MySQLColumnInfo read(ResultSet rs) throws SQLException {
                MySQLColumnInfo i = new MySQLColumnInfo();
                i.setName(rs.getString("column_name"));
                try {
                    i.setType(MySqlType.valueOf(rs.getString("data_type").toUpperCase()));
                } catch (IllegalArgumentException e) {
                    i.setType(MySqlType.UNKNOWN);
                    logger.warn("unknown datatype: " + rs.getString("data_type") + " in column "
                            + i.getName() + " of table "
                            + tableName + ".");
                }
                i.setCharLength(rs.getLong("character_maximum_length"));
                i.setIndexType(getIndexTypeByString(rs.getString("column_key")));
                return i;
            }

        };
    }

}