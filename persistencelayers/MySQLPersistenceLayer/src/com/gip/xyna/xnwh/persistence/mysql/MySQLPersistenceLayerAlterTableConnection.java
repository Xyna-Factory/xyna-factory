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

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseColumnInfo;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseIndexCollision;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseIndexCollision.IndexModification;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabasePersistenceLayerConnectionWithAlterTableSupport;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabasePersistenceLayerWithAlterTableSupportHelper;

class MySQLPersistenceLayerAlterTableConnection
        implements DatabasePersistenceLayerConnectionWithAlterTableSupport, AutoCloseable {

    static final Logger logger = CentralFactoryLogging.getLogger(MySQLPersistenceLayerAlterTableConnection.class);

    private String lock;

    boolean tryLock(String lock, int plTimeout) {
        if (!this.mySQLPersistenceLayer.useSchemaLocking())
            return false;

        if (isLocked())
            throw new IllegalStateException(
                    "Lock with name " + this.lock + " exists. Can not create new lock for " + lock);

        int timeout = plTimeout < 0 ? -1 : plTimeout;
        if (logger.isDebugEnabled()) {
            logger.debug("locking database for pliID " + pliID + "(" + mySQLPersistenceLayer.getSchemaName() + ") with "
                    + lock + " with timeout " + String.valueOf(timeout));
        }

        long waittime = 0;
        if (logger.isDebugEnabled()) {
            waittime = System.currentTimeMillis();
        }

        Integer result = sqlUtils.queryInt("select get_lock(?, ?)",
                new com.gip.xyna.utils.db.Parameter(lock, timeout));

        if (logger.isDebugEnabled()) {
            waittime = System.currentTimeMillis() - waittime;
            logger.debug("locking database for pliID " + pliID + "(" + mySQLPersistenceLayer.getSchemaName() + ") with "
                    + lock + " took (ms) " + String.valueOf(waittime));
        }

        if (result == null || result == 0) {
            logger.error("locking database for pliID " + pliID + "(" + mySQLPersistenceLayer.getSchemaName() + ") with "
                    + lock + " with timeout " + String.valueOf(timeout) + " failed. result = "
                    + (result != null ? String.valueOf(result) : "NULL"));
            return false;
        }

        this.lock = lock;
        return result > 0;
    }

    boolean isLocked() {
        return this.lock != null && !this.lock.isEmpty();
    }

    void unlock() {
        if (!this.mySQLPersistenceLayer.useSchemaLocking())
            return;

        if (logger.isDebugEnabled()) {
            logger.debug("unlocking database for pliID " + pliID + " (" + mySQLPersistenceLayer.getSchemaName()
                    + ") with lock " + this.lock + ". Database is "
                    + ((isLocked()) ? "locked" : "not locked."));
        }

        if (!isLocked())
            return;

        Integer result = sqlUtils.queryInt("select release_lock(?)",
                new com.gip.xyna.utils.db.Parameter(this.lock));

        if (result == null || result == 0)
            logger.error("unlocking of database for pliID " + pliID + "(" + mySQLPersistenceLayer.getSchemaName()
                    + ") for lock " + this.lock + " failed. result = "
                    + (result != null ? String.valueOf(result) : "NULL"));
        else {
            this.lock = null;
        }
    }

    private static String numberToStringUsingAllChars(int num) {
        StringBuilder back = new StringBuilder();
        num = Math.abs(num);
        while (num != 0) {
            int charValue = Math.abs(num % 36);
            num /= 36;
            if (charValue <= 9) {
                back.append(charValue);
            } else {
                back.append((char) ('a' + charValue - 10));
            }
        }
        return back.toString();
    }

    @SuppressWarnings("rawtypes")
    private static <T extends Storable> MySqlType getDefaultMySQLColTypeForStorableColumn(Column col, Class<T> klass) {
        ColumnType colType = col.type();
        if (colType == ColumnType.BLOBBED_JAVAOBJECT) {
            return MySqlType.LONGBLOB;
        } else if (colType == ColumnType.BYTEARRAY) {
            return MySqlType.LONGBLOB;
        } else if (colType == ColumnType.INHERIT_FROM_JAVA) {
            Field f = Storable.getColumn(col, klass);
            Class<?> fieldType = f.getType();
            MySqlType type = MySQLPersistenceLayer.javaTypeToMySQLType.get(fieldType);
            if (type == null) {
                // Iteration über die Einträge in javaTypeToMySQLType: evtl. ist Storable-Column
                // von einem bekannten Typ abgeleitet
                for (Class<?> clazz : MySQLPersistenceLayer.javaTypeToMySQLType.keySet()) {
                    if (clazz.isAssignableFrom(fieldType)) {
                        type = MySQLPersistenceLayer.javaTypeToMySQLType.get(clazz);
                        break;
                    }
                }
            }
            if (type == null) {
                return MySqlType.LONGBLOB;
            } else if (type == MySqlType.VARCHAR) {
                if (col.size() > type.getSize()) {
                    return MySqlType.LONGTEXT;
                } else {
                    return type;
                }
            } else {
                return type;
            }
        } else {
            throw new RuntimeException("unsupported columntype: " + colType);
        }
    }

    private MySQLPersistenceLayer mySQLPersistenceLayer;

    private MySQLPersistenceLayerConnection mySQLPersistenceLayerConnection;

    private SQLUtils sqlUtils;

    private Long pliID;

    // Maximale Länge des Hashs, bei dem es noch Sinn macht zu modulon. Alles
    // darüber übertrifft Integer.MAX_VALUE und ergibt somit keinen Sinn mehr
    private final int MAX_HASH = (int) (Math.log(Integer.MAX_VALUE) / Math.log(36));

    MySQLPersistenceLayerAlterTableConnection(MySQLPersistenceLayer pl, MySQLPersistenceLayerConnection plCon,
            SQLUtils utils) {
        this.mySQLPersistenceLayerConnection = plCon;
        this.mySQLPersistenceLayer = pl;
        this.sqlUtils = utils;
        this.pliID = this.mySQLPersistenceLayer.getPersistenceLayerInstanceID();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <T extends Storable> void alterColumns(Set<DatabaseIndexCollision> collisions)
            throws PersistenceLayerException {
        for (DatabaseIndexCollision collision : collisions) {
            final String tableNameWithSchemaPrefix = collision.getPersi().tableName().toLowerCase();

            sqlUtils.setLogger(this.mySQLPersistenceLayer.getCreateTableLogger());
            try {
                Column col = collision.getColumn();

                if (!isView(tableNameWithSchemaPrefix)) {
                    // indizes überprüfen
                    boolean isPk = col.name().equals(collision.getPersi().primaryKey());
                    IndexType javaIndexType = isPk ? IndexType.UNIQUE : col.index();

                    MySQLColumnInfo colInfo = this.mySQLPersistenceLayer.getColumnMap().get(col);
                    if (colInfo == null) {
                        // keine Daten für den Index bislang, deswegen evtl. neu bauen
                        if (javaIndexType != IndexType.NONE) {
                            String indexName = createIndexName(tableNameWithSchemaPrefix, col.name(), isPk);
                            try {
                                createIndex(indexName, javaIndexType, tableNameWithSchemaPrefix, col.name());
                            } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
                                this.mySQLPersistenceLayerConnection.validateConnection();
                                logger.warn(
                                        "Could not create index " + indexName + " on table " + tableNameWithSchemaPrefix
                                                + " for column " + col.name() + " of type " + javaIndexType + ".",
                                        e);
                            }
                        }
                        continue;
                    }

                    if (javaIndexType == IndexType.NONE && colInfo.getIndexType() == IndexType.NONE) {
                        continue; // kein Index
                    }

                    if (colInfo.getIndexType() == IndexType.PRIMARY
                            && colInfo.getName().equalsIgnoreCase(collision.getPersi().primaryKey())) {
                        // primarykey index ok...
                        continue;
                    }

                    // es gibt mindestens einen Index für diese Spalte, dieser hat einen Namen
                    String indexName = createIndexName(tableNameWithSchemaPrefix, col.name(), isPk);
                    try {
                        alterIndex(col, colInfo, javaIndexType, indexName, tableNameWithSchemaPrefix,
                                collision.getIndexModification());
                    } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
                        this.mySQLPersistenceLayerConnection.validateConnection();
                        logger.warn("Could not create index " + indexName + " on table " + tableNameWithSchemaPrefix
                                + " for column " + col.name() + " of type " + javaIndexType + ".", e);
                    }

                }
            } finally {
                sqlUtils.setLogger(this.mySQLPersistenceLayer.getDebugLogger());
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> Set<DatabaseIndexCollision> checkColumns(Persistable persistable, Class<T> klass,
            Column[] cols)
            throws PersistenceLayerException {
        final String tableNameWithSchemaPrefix = persistable.tableName().toLowerCase();
        final String tableNameWithoutSchemaPrefix;
        if (tableNameWithSchemaPrefix.contains(".")) {
            String tableNameParts[] = tableNameWithSchemaPrefix.split("\\.");
            tableNameWithoutSchemaPrefix = tableNameParts[tableNameParts.length - 1];
        } else {
            tableNameWithoutSchemaPrefix = tableNameWithSchemaPrefix;
        }

        sqlUtils.setLogger(this.mySQLPersistenceLayer.getCreateTableLogger());
        try {
            List<MySQLColumnInfo> colInfos = sqlUtils.query(MySQLColumnInfo.SELECT_COLUMN_NAME_AND_DATATYPE_SQL,
                    new com.gip.xyna.utils.db.Parameter(tableNameWithoutSchemaPrefix),
                    MySQLColumnInfo.getResultSetReader(tableNameWithoutSchemaPrefix));

            StringBuffer addColumnString = new StringBuffer();
            for (Column col : cols) {
                boolean foundCol = false;
                for (MySQLColumnInfo colInfo : colInfos) {
                    if (col.name().equalsIgnoreCase(colInfo.getName())) {
                        DatabasePersistenceLayerWithAlterTableSupportHelper.checkColumn(this, colInfo, col, klass,
                                tableNameWithSchemaPrefix,
                                this.mySQLPersistenceLayer.useAutomaticColumnTypeWidening());
                        foundCol = true;
                    }
                }
                if (!foundCol) {
                    // spalte erstellen
                    if (addColumnString.length() > 0) {
                        addColumnString.append(",");
                    }
                    addColumnString.append("\n  ADD COLUMN ").append(col.name()).append(" ")
                            .append(getDefaultColumnTypeString(col, klass)).append(" NULL");
                }
            }

            // Updating columnMap as some modifications might have occured.
            colInfos = sqlUtils.query(MySQLColumnInfo.SELECT_COLUMN_NAME_AND_DATATYPE_SQL,
                    new com.gip.xyna.utils.db.Parameter(tableNameWithoutSchemaPrefix),
                    MySQLColumnInfo.getResultSetReader(tableNameWithoutSchemaPrefix));

            for (Column col : cols) {
                for (MySQLColumnInfo colInfo : colInfos) {
                    if (col.name().equalsIgnoreCase(colInfo.getName())) {
                        colInfo.setStorableClass(klass);
                        if (this.mySQLPersistenceLayer.getColumnMap().get(col) == null ||
                                this.mySQLPersistenceLayer.getColumnMap().get(col).getIndexType() != colInfo
                                        .getIndexType()
                                || this.mySQLPersistenceLayer.getColumnMap().get(col).getType() != colInfo.getType()) {
                            colInfo.next = this.mySQLPersistenceLayer.getColumnMap().get(col);
                            // evtl. vorherige Einträge
                            // aufheben: es kann mehrere
                            // Einträge ...
                            // ... in "colInfos" zu einem Eintrag in "cols" geben
                            this.mySQLPersistenceLayer.getColumnMap().put(col, colInfo);
                        }
                    }
                }
            }

            Set<DatabaseIndexCollision> collisions = new HashSet<DatabaseIndexCollision>();
            if (!isView(tableNameWithSchemaPrefix)) {
                if (addColumnString.length() > 0) {
                    String ddl = "ALTER TABLE " + tableNameWithSchemaPrefix + "\n" + addColumnString.toString();
                    validateAccessMode(ddl);
                    sqlUtils.executeDDL(ddl, null);
                }

                // indizes überprüfen
                for (Column column : cols) {
                    boolean isPk = column.name().equals(persistable.primaryKey());
                    IndexType javaIndexType = isPk ? IndexType.UNIQUE : column.index();

                    MySQLColumnInfo colInfo = this.mySQLPersistenceLayer.getColumnMap().get(column);
                    if (colInfo == null) {
                        // keine Daten für den Index bislang, deswegen evtl. neu bauen
                        if (javaIndexType != IndexType.NONE) {
                            String indexName = createIndexName(tableNameWithSchemaPrefix, column.name(), isPk);
                            try {
                                createIndex(indexName, javaIndexType, tableNameWithSchemaPrefix, column.name());
                            } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
                                this.mySQLPersistenceLayerConnection.validateConnection();
                                logger.warn(
                                        "Could not create index " + indexName + " on table " + tableNameWithSchemaPrefix
                                                + " for column " + column.name() + " of type " + javaIndexType + ".",
                                        e);
                            }
                        }
                        continue;
                    }

                    if (javaIndexType == IndexType.NONE && colInfo.getIndexType() == IndexType.NONE) {
                        continue; // kein Index
                    }

                    if (colInfo.getIndexType() == IndexType.PRIMARY
                            && colInfo.getName().equalsIgnoreCase(persistable.primaryKey())) {
                        // primarykey index ok...
                        continue;
                    }

                    // es gibt mindestens einen Index für diese Spalte, dieser hat einen Namen
                    String indexName = createIndexName(tableNameWithSchemaPrefix, column.name(), isPk);
                    try {
                        IndexModification mod = checkIndex(column, colInfo, javaIndexType, indexName,
                                tableNameWithSchemaPrefix);
                        if (mod != null) {
                            collisions.add(new DatabaseIndexCollision(persistable, column, klass, mod));
                        }
                    } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
                        this.mySQLPersistenceLayerConnection.validateConnection();
                        logger
                                .warn("Could not create index " + indexName + " on table " + tableNameWithSchemaPrefix
                                        + " for column " + column.name() + " of type " + javaIndexType + ".", e);
                    }

                }
            }
            return collisions;
        } finally {
            sqlUtils.setLogger(this.mySQLPersistenceLayer.getDebugLogger());
        }
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> void createTable(Persistable persistable, Class<T> klass, Column[] cols) {

        validateAccessMode(klass.getCanonicalName());

        String tableName = persistable.tableName().toLowerCase();
        StringBuilder createTableStatement = new StringBuilder("CREATE TABLE ").append(tableName).append(" (\n");
        for (Column col : cols) {
            String typeAsString = getDefaultColumnTypeString(col, klass);
            createTableStatement.append("  ").append(col.name()).append(" ").append(typeAsString);
            // default wert
            if (col.name().equals(persistable.primaryKey())) {
                createTableStatement.append(" NOT");
            }
            createTableStatement.append(" NULL,\n");
        }
        createTableStatement.append("  PRIMARY KEY(").append(persistable.primaryKey());
        for (Column column : cols) {
            // the following check is a bit nasty: dont add the "one" primary key twice. in
            // theory it would be
            // possible to use only this loop without the check but that would require that
            // all tables have set
            // the IndexType correctly.
            if (column.index() == IndexType.PRIMARY && !column.name().equals(persistable.primaryKey())) {
                createTableStatement.append(", ").append(column.name());
            }
        }
        createTableStatement.append(")\n");
        createTableStatement.append(")\n ENGINE=InnoDB");
        sqlUtils.setLogger(this.mySQLPersistenceLayer.getCreateTableLogger());
        try {
            sqlUtils.executeDDL(createTableStatement.toString(), null);
            if (!isView(tableName)) {

                for (Column column : cols) {
                    if (column.name().equals(persistable.primaryKey())) {
                        continue;
                    }
                    switch (column.index()) {
                        case NONE:
                            // nichts zu tun
                            break;
                        case PRIMARY:
                            // nichts zu tun: Ist bereits beim CREATE TABLE gesetzt worden
                            break;
                        default: // UNIQUE und MULTIPLE
                            String indexName = createIndexName(tableName, column.name(), false);
                            createIndex(indexName, column.index(), tableName, column.name());
                    }
                }
            }
        } finally {
            sqlUtils.setLogger(this.mySQLPersistenceLayer.getDebugLogger());
        }
    }

    public boolean doesTableExist(Persistable persistable) {
        String tableNameWithoutSchemaPrefix = persistable.tableName().toLowerCase();
        if (tableNameWithoutSchemaPrefix.contains(".")) {
            String tableNameParts[] = tableNameWithoutSchemaPrefix.split("\\.");
            tableNameWithoutSchemaPrefix = tableNameParts[tableNameParts.length - 1];
        }
        return sqlUtils.queryInt(MySQLColumnInfo.DOES_TABLE_EXIST_SQL,
                new com.gip.xyna.utils.db.Parameter(this.mySQLPersistenceLayer.getSchemaName(),
                        tableNameWithoutSchemaPrefix)) > 0;
    }

    public boolean doesIndexExistForTable(String index, String table) {
        return sqlUtils.queryInt(MySQLColumnInfo.DOES_INDEX_EXIST_FOR_TABLE_SQL,
                new com.gip.xyna.utils.db.Parameter(index, table)) > 0;
    }

    public long getPersistenceLayerInstanceId() {
        return this.pliID;
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> void modifyColumnsCompatible(Column col, Class<T> klass, String tableName) {
        validateAccessMode(klass.getCanonicalName());
        if (!isView(tableName)) {
            MySqlType recommendedType = getDefaultMySQLColTypeForStorableColumn(col, klass);
            String sql = new StringBuffer("ALTER TABLE ").append(tableName).append(" CHANGE ").append(col.name())
                    .append(" ")
                    .append(col.name()).append(" ").append(recommendedType).append("(")
                    .append(this.mySQLPersistenceLayer.getColumnSize(col, klass))
                    .append(")").toString();
            sqlUtils.setLogger(this.mySQLPersistenceLayer.getCreateTableLogger());
            try {
                sqlUtils.executeDDL(sql, null);
            } finally {
                sqlUtils.setLogger(this.mySQLPersistenceLayer.getDebugLogger());
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> void widenColumnsCompatible(Column col, Class<T> klass, String tableName) {
        validateAccessMode(klass.getCanonicalName());
        if (!isView(tableName)) {
            MySqlType recommendedType = getDefaultMySQLColTypeForStorableColumn(col, klass);
            StringBuffer sql = new StringBuffer("ALTER TABLE ").append(tableName).append(" CHANGE ").append(col.name())
                    .append(" ")
                    .append(col.name()).append(" ").append(recommendedType);
            if (recommendedType.isDependentOnSizeSpecification()) {
                sql.append("(").append(this.mySQLPersistenceLayer.getColumnSize(col, klass)).append(")");
            }
            sqlUtils.executeDDL(sql.toString(), null);
        }
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> String getCompatibleColumnTypesAsString(Column col, Class<T> klass) {
        MySqlType recommendedType = getDefaultMySQLColTypeForStorableColumn(col, klass);
        List<MySqlType> compatibleTypes = recommendedType.getCompatibleTypes();
        StringBuilder compatibleTypesStringBuilder = new StringBuilder();
        for (int i = 0; i < compatibleTypes.size(); i++) {
            if (i > 0) {
                compatibleTypesStringBuilder.append(", ");
            }
            MySqlType next = compatibleTypes.get(i);
            compatibleTypesStringBuilder.append(next.toString());
            if (next.isDependentOnSizeSpecification()) {
                compatibleTypesStringBuilder.append("(?)");
            }
        }
        return compatibleTypesStringBuilder.toString();
    }

    // Kürzungsregeln:
    // (Bsp. Ziel: 62)
    // abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz_012345678901234567890123456789012345678901234567890123456789_idx
    // ==> 117 Zeichen
    //
    // abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz_0123456789012345678901234567890123456789012345678901_idx
    // ==> 109 Zeichen
    // 1. Kürzen bis beide gleich lang (Falls es währenddessen schon passt:
    // aufhören)
    //
    // abcdefghijklmnopqrstuvwxyzab_01234567890123456789012345678_idx ==> 62 Zeichen
    // 2. Beide gleich kürzen bis es passt

    @SuppressWarnings("rawtypes")
    public <T extends Storable> boolean areBaseTypesCompatible(Column col, Class<T> klass, DatabaseColumnInfo colInfo) {
        MySqlType recommendedType = getDefaultMySQLColTypeForStorableColumn(col, klass);
        return ((MySQLColumnInfo) colInfo).getType().getBaseType().isCompatibleTo(recommendedType.getBaseType());
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> boolean areColumnsCompatible(Column col, Class<T> klass, DatabaseColumnInfo colInfo) {
        MySqlType recommendedType = getDefaultMySQLColTypeForStorableColumn(col, klass);
        List<MySqlType> compatibleTypes = recommendedType.getCompatibleTypes();
        return compatibleTypes.contains(((MySQLColumnInfo) colInfo).getType());
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> String getTypeAsString(Column col, Class<T> klass) {
        MySqlType colType = getDefaultMySQLColTypeForStorableColumn(col, klass);
        return colType.toString();
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> boolean isTypeDependentOnSizeSpecification(Column col, Class<T> klass) {
        MySqlType colType = getDefaultMySQLColTypeForStorableColumn(col, klass);
        return colType.isDependentOnSizeSpecification();
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> String getDefaultColumnTypeString(Column col, Class<T> klass) {
        MySqlType colType = getDefaultMySQLColTypeForStorableColumn(col, klass);
        String typeAsString = colType.toString();
        if (colType.isDependentOnSizeSpecification()) {
            typeAsString += "(" + this.mySQLPersistenceLayer.getColumnSize(col, klass) + ")";
        }
        return typeAsString;
    }

    @SuppressWarnings("rawtypes")
    private <T extends Storable> boolean isView(String tableName) {
        Boolean queryResult = sqlUtils.queryOneRow(
                "SELECT TABLE_TYPE FROM information_schema.TABLES WHERE TABLE_NAME = ? AND TABLE_SCHEMA = ?",
                new com.gip.xyna.utils.db.Parameter(tableName, this.mySQLPersistenceLayer.getSchemaName()),
                new ResultSetReader<Boolean>() {

                    public Boolean read(ResultSet rs) throws SQLException {
                        String tableType = rs.getString("TABLE_TYPE");
                        return tableType != null && tableType.equalsIgnoreCase("VIEW");
                    }
                });
        return Boolean.TRUE.equals(queryResult);
    }

    private void alterIndex(Column column, MySQLColumnInfo colInfo, IndexType javaIndexType, String indexName,
            String tableName, IndexModification desiredModification) {
        // TODO anders als in OraclePersistenceLayer. Ist angleichen sinnvoll?
        // MySQLColumnInfo muss noch erweitert werden, damit Name des bestehenden Index
        // bekannt ist
        if (colInfo.next != null) {
            logger.warn(
                    "Unimplemented check of multiple indexes on column " + column.name() + " in table " + tableName);
            return;
        }
        if (colInfo.getIndexType() == javaIndexType) {
            // this is OK as long as we cannot verify the name
            return;
        }
        if (javaIndexType == IndexType.NONE) {
            if (desiredModification != IndexModification.DELETE) {
                logger.warn("Detected index deletion on column " + column.name() + " in table "
                        + tableName + " does not match the desired modifcation " + desiredModification);
                return;
            } else {
                logger
                        .warn("Unimplemented delete index on column " + column.name() + " in table " + tableName);
                return;
            }
        }
        if (colInfo.getIndexType() == IndexType.NONE) {
            if (desiredModification != IndexModification.CREATE) {
                logger.warn("Detected index creation on column " + column.name() + " in table "
                        + tableName + " does not match the desired modifcation " + desiredModification);
                return;
            } else {
                createIndex(indexName, javaIndexType, tableName, column.name());
                return;
            }
        }
        if (desiredModification != IndexModification.MODIFY) {
            logger.warn("Detected index modification on column " + column.name() + " in table "
                    + tableName + " does not match the desired modifcation " + desiredModification);
        } else {
            logger.warn(
                    "Unimplemented change index on column " + column.name() + " in table " + tableName + ". Found: "
                            + colInfo.getIndexType() + ", expected: " + javaIndexType.name());
        }
    }

    private void createIndex(String indexName, IndexType indexType, String tableName, String columnName) {
        String createIndexStatement = null;
        switch (indexType) {
            case UNIQUE:
                createIndexStatement = "CREATE UNIQUE INDEX " + indexName + " ON " + tableName + "(" + columnName + ")";
                break;
            case MULTIPLE:
                createIndexStatement = "CREATE INDEX " + indexName + " ON " + tableName + "(" + columnName + ")";
                break;
            default:
                logger.info(
                        "Index-Creation " + indexName + " of type " + indexType + " for " + tableName + "." + columnName
                                + " is unsupported!");
                return;
        }
        logger
                .info("Index-Creation " + indexName + " of type " + indexType + " for " + tableName + "." + columnName);
        executeDDL(createIndexStatement);
    }

    private void executeDDL(String ddl) {

        validateAccessMode(ddl);

        // Ausführen des Statements ddl und Warn-Log, falls dies nicht erfolgreich war
        boolean created = false;
        try {
            sqlUtils.executeDDL(ddl, null);
            created = true;
        } finally {
            if (!created) {
                logger.warn("Statement not excuted was " + ddl);
            }
        }
    }

    private String createIndexName(String tableName, String columnName, boolean pk) {
        final boolean useDatabaseQueryForDuplicates = this.mySQLPersistenceLayer
                .useDatabaseQueryToAvoidDuplicateIndexes();
        final int HASH_LENGTH = this.mySQLPersistenceLayer.getHashCharacterCountToAvoidDuplicateIndexes();
        final int MAXLENGHTH = this.mySQLPersistenceLayer.getMaximumIndexNameLenght();

        String tablePart = tableName;
        String columnPart = columnName;
        String indexName = tablePart.replace('.', '_') + "_" + columnPart + (pk ? "_pk" : "_idx");

        int toShorten = indexName.length() - MAXLENGHTH;
        if (toShorten > 0) {
            toShorten += (useDatabaseQueryForDuplicates ? 2 : HASH_LENGTH);
            int toShortenTableName;
            int toShortenColumnName;
            if (tablePart.length() > columnPart.length()) {
                toShortenTableName = Math.min(toShorten, tablePart.length() - columnPart.length()); // Auf gleiche Länge
                toShortenTableName += (toShorten - toShortenTableName) / 2; // Rest
                toShortenColumnName = toShorten - toShortenTableName;
            } else {
                toShortenColumnName = Math.min(toShorten, columnPart.length() - tablePart.length()); // Auf gleiche
                                                                                                     // Länge
                toShortenColumnName += (toShorten - toShortenColumnName) / 2; // Rest
                toShortenTableName = toShorten - toShortenColumnName;
            }
            tablePart = tablePart.substring(0, tablePart.length() - toShortenTableName);
            columnPart = columnPart.substring(0, columnPart.length() - toShortenColumnName);
            indexName = tablePart.replace('.', '_') + "_" + columnPart + (pk ? "_pk" : "_idx");

            if (useDatabaseQueryForDuplicates) {
                int suffix = 0;
                indexName = tablePart.replace('.', '_') + "_" + columnPart + (suffix != 0 ? suffix : "")
                        + (pk ? "_pk" : "_idx");
                while (doesIndexExistForTable(indexName, tableName)) {
                    suffix++;
                    indexName = tablePart.replace('.', '_') + "_" + columnPart + (suffix != 0 ? suffix : "")
                            + (pk ? "_pk" : "_idx");
                }
            } else {
                String hash = "";
                if (HASH_LENGTH <= MAX_HASH) { // Falls HASH_LENGTH Integer.MAX_VALUE übertrifft, gibte es keinen Sinn
                                               // mehr zu modulon
                    int mod = (int) Math.pow(36, HASH_LENGTH);
                    hash = numberToStringUsingAllChars(columnName.hashCode() % mod);
                } else {
                    hash = numberToStringUsingAllChars(columnName.hashCode());
                }
                indexName = tablePart.replace('.', '_') + "_" + columnPart + hash
                        + (pk ? "_pk" : "_idx");
            }
        }
        return indexName;
    }

    private IndexModification checkIndex(Column column, MySQLColumnInfo colInfo, IndexType javaIndexType,
            String indexName,
            String tableName) {
        // TODO anders als in OraclePersistenceLayer. Ist angleichen sinnvoll?
        // MySQLColumnInfo muss noch erweitert werden, damit Name des bestehenden Index
        // bekannt ist
        if (colInfo.next != null) {
            logger.warn(
                    "Unimplemented check of multiple indexes on column " + column.name() + " in table " + tableName);
            return null;
        }
        if (colInfo.getIndexType() == javaIndexType) {
            // this is OK as long as we cannot verify the name
            return null;
        }
        if (javaIndexType == IndexType.NONE) {
            return IndexModification.DELETE;
        }
        if (colInfo.getIndexType() == IndexType.NONE) {
            return IndexModification.CREATE;
        }

        return IndexModification.MODIFY;

    }

    @Override
    public void close() throws Exception {
        unlock();
    }

    private void validateAccessMode(final String details) {
        MySQLPersistenceLayer.AccessMode mode = this.mySQLPersistenceLayer.getAccessMode();
        if (!mode.equals(MySQLPersistenceLayer.AccessMode.READ_WRITE)) {
            String msg = "Can not change data because of access mode " + mode + ". " + details;
            throw new IllegalStateException(msg);
        }
    }
}
