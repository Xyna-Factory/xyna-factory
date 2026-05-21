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

import com.gip.xyna.xnwh.persistence.sql.SqlType;

enum MySqlType implements SqlType<MySqlType, MySqlBaseType> {
        BOOL(1, MySqlBaseType.NUMBER), BOOLEAN(1, MySqlBaseType.NUMBER), TINYINT(1, MySqlBaseType.NUMBER), SMALLINT(2,
                        MySqlBaseType.NUMBER),
        MEDIUMINT(3, MySqlBaseType.NUMBER), INT(4, MySqlBaseType.NUMBER), INTEGER(4,
                        MySqlBaseType.NUMBER),
        BIGINT(8, MySqlBaseType.NUMBER), SERIAL(8, MySqlBaseType.NUMBER), BIT(8,
                        MySqlBaseType.NUMBER),
        FLOAT(4, MySqlBaseType.FLOAT), DOUBLE(8, MySqlBaseType.FLOAT), REAL(8,
                        MySqlBaseType.FLOAT),
        DECIMAL(4, MySqlBaseType.FLOAT), DEC(4, MySqlBaseType.FLOAT), NUMERIC(8,
                        MySqlBaseType.FLOAT),
        DATE(3, MySqlBaseType.TIME), DATETIME(8, MySqlBaseType.TIME), TIMESTAMP(4,
                        MySqlBaseType.TIME),
        TIME(3, MySqlBaseType.TIME), YEAR(1, MySqlBaseType.TIME), CHAR(255,
                        MySqlBaseType.TEXT_ENCODED, true),
        NCHAR(255, MySqlBaseType.TEXT_ENCODED, true), VARCHAR(65535,
                        MySqlBaseType.TEXT_ENCODED, true),
        TINYTEXT(255, MySqlBaseType.TEXT_ENCODED), TEXT(65535,
                        MySqlBaseType.TEXT_ENCODED),
        MEDIUMTEXT(16777215, MySqlBaseType.TEXT_ENCODED), LONGTEXT(4294967295L,
                        MySqlBaseType.TEXT_ENCODED),
        BINARY(255, MySqlBaseType.BINARY, true), VARBINARY(65535, MySqlBaseType.BINARY,
                        true),
        TINYBLOB(255, MySqlBaseType.BINARY), BLOB(65535, MySqlBaseType.BINARY), MEDIUMBLOB(16777215,
                        MySqlBaseType.BINARY),
        LONGBLOB(4294967295L, MySqlBaseType.BINARY), ENUM(65635, MySqlBaseType.OTHER), SET(64,
                        MySqlBaseType.OTHER),
        UNKNOWN(0, null);

        private boolean dependentOnSizeSpecification;
        private long size;
        private MySqlBaseType baseType;

        private MySqlType(long size, MySqlBaseType baseType) {
                this(size, baseType, false);
        }

        private MySqlType(long size, MySqlBaseType baseType, boolean dependentOnSizeSpecification) {
                this.size = size;
                this.baseType = baseType;
                this.dependentOnSizeSpecification = dependentOnSizeSpecification;
        }

        public boolean isDependentOnSizeSpecification() {
                return dependentOnSizeSpecification;
        }

        public long getSize() {
                return size;
        }

        public MySqlBaseType getBaseType() {
                return baseType;
        }

        @Override
        public Class<MySqlType> getEnumClass() {
            return MySqlType.class;
        }
}