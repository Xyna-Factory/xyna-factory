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

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ExtendedParameter;
import com.gip.xyna.utils.db.InList;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.types.BooleanWrapper;
import com.gip.xyna.xnwh.exceptions.XNWH_ConnectionClosedException;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.TransactionProperty;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabasePersistenceLayerWithAlterTableSupportHelper;
import com.gip.xyna.xnwh.persistence.xmom.PersistenceExpressionVisitors;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;

// unterstützt nicht mehrere threads die die gleiche connection benutzen
class MySQLPersistenceLayerConnection implements PersistenceLayerConnection {

    enum UpdateInsert {
        update, insert, done;
    }

    static final Logger logger = CentralFactoryLogging.getLogger(MySQLPersistenceLayerConnection.class);

    /**
     *
     */
    private final MySQLPersistenceLayer mySQLPersistenceLayer;
    private final ConnectionPool connectionPool;
    private final SQLUtils sqlUtils;
    private boolean closed = false;

    // falls db connection über mehrere mysql-pl cons geteilt wird, stehen hier alle
    // beteiligten aktiven mysqlPL-connections drin.
    // beim close schliesst nur der letzte aus der liste die zugrundeliegende
    // db-connection
    private final List<MySQLPersistenceLayerConnection> sharedConnections;

    public MySQLPersistenceLayerConnection(MySQLPersistenceLayer mySQLPersistenceLayer)
            throws PersistenceLayerException {
        this(mySQLPersistenceLayer, false);
    }

    public MySQLPersistenceLayerConnection(MySQLPersistenceLayer mySQLPersistenceLayer, boolean isDedicated)
            throws PersistenceLayerException {
        this.mySQLPersistenceLayer = mySQLPersistenceLayer;
        sqlUtils = this.mySQLPersistenceLayer.createSQLUtils(isDedicated);
        try {
            if (sqlUtils.getConnection() != null) {
                sqlUtils.getConnection().setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            }
        } catch (SQLException e) {
            logger.warn(
                    "Unable to set TransactionIsolationLevel, this might lead to strange behaviour if the global IsolationLevel differs.");
        }
        if (isDedicated) {
            connectionPool = this.mySQLPersistenceLayer.getDedicatedConnectionPool();
        } else {
            connectionPool = this.mySQLPersistenceLayer.getConnectionPool();
        }
        sharedConnections = new ArrayList<MySQLPersistenceLayerConnection>();
        sharedConnections.add(this);
    }

    public MySQLPersistenceLayerConnection(MySQLPersistenceLayer mySQLPersistenceLayer,
            MySQLPersistenceLayerConnection shareConnectionPool) {
        this.mySQLPersistenceLayer = mySQLPersistenceLayer;
        sqlUtils = shareConnectionPool.sqlUtils;
        connectionPool = this.mySQLPersistenceLayer.getConnectionPool();
        sharedConnections = shareConnectionPool.sharedConnections;
        sharedConnections.add(this);
    }

    public boolean isOpen() {
        try {
            return !closed && !sqlUtils.getConnection().isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * überprüfung ob tabelle existiert. falls nicht wird sie erstellt. falls ja,
     * wird validiert, ob spalten fehlen oder
     * geändert werden müssen. fehlende spalten werden hinzugefügt. vorhandene
     * spalten werden geupdated, falls das ohne
     * datenverlust möglich ist. (ansonsten wird ein fehler geworfen.)
     */
    @SuppressWarnings("rawtypes")
    public <T extends Storable> void addTable(Class<T> klass, boolean forceWidening, Properties props)
            throws PersistenceLayerException {
        try {
            MySQLPersistenceLayerAlterTableConnection alterTableConn = new MySQLPersistenceLayerAlterTableConnection(
                    mySQLPersistenceLayer, this, sqlUtils);
            if (forceWidening) {
                boolean previousSetting = this.mySQLPersistenceLayer.useAutomaticColumnTypeWidening();
                this.mySQLPersistenceLayer.enableAutomaticColumnTypeWidening();
                DatabasePersistenceLayerWithAlterTableSupportHelper.addTable(alterTableConn, klass);
                this.mySQLPersistenceLayer.setAutomaticColumnTypeWidening(previousSetting);
            } else {
                DatabasePersistenceLayerWithAlterTableSupportHelper.addTable(alterTableConn, klass);
            }
            MySQLPersistenceLayer.getTableClassMap().put(Storable.getPersistable(klass).tableName().toLowerCase(),
                    klass);
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
            validateConnection();
            throw new XNWH_GeneralPersistenceLayerException("Storable " + Storable.getPersistable(klass).tableName()
                    + " could not be registered in " + MySQLPersistenceLayer.class.getSimpleName(), e);
        }
    }

    public void closeConnection() throws PersistenceLayerException {
        if (closed) {
            return;
        }
        sharedConnections.remove(this);
        if (sharedConnections.isEmpty()) {
            this.mySQLPersistenceLayer.closeSQLUtils(sqlUtils);
        }
        closed = true;
    }

    private void ensureOpen() throws PersistenceLayerException {
        if (closed) {
            throw new XNWH_ConnectionClosedException();
        }
    }

    public void commit() throws PersistenceLayerException {
        ensureOpen();
        try {
            sqlUtils.commit();
        } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
            throw new XNWH_RetryTransactionException(e);
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
            validateConnection();
            throw new XNWH_GeneralPersistenceLayerException("could not commit transaction.", e);
        }
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
        String select = "select count(*) from " + storable.getTableName().toLowerCase() + " where "
                + Storable.getPersistable(storable.getClass()).primaryKey() + " = ?";
        com.gip.xyna.utils.db.Parameter paras = new com.gip.xyna.utils.db.ExtendedParameter();
        Column colPK = MySQLPersistenceLayer.getColumnForPrimaryKey(storable);
        this.mySQLPersistenceLayer.addToParameter(paras, colPK, storable.getPrimaryKey(), storable);
        try {
            sqlUtils.cacheStatement(select);
            int cnt = sqlUtils.queryInt(select, paras);
            return cnt > 0;
        } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
            throw new XNWH_RetryTransactionException(e);
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
            validateConnection();
            throw new XNWH_GeneralPersistenceLayerException("could not check for object.", e);
        }
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> void deleteOneRow(T storable) throws PersistenceLayerException {
        String delete = "delete from " + storable.getTableName().toLowerCase() + " where "
                + Storable.getPersistable(storable.getClass()).primaryKey() + " = ?";
        deleteSingleElement(storable, delete);
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException {
        if (storableCollection != null && storableCollection.size() > 0) {
            Iterator<T> it = storableCollection.iterator();
            T a = it.next();
            String delete = "delete from " + a.getTableName().toLowerCase() + " where "
                    + Storable.getPersistable(a.getClass()).primaryKey() + " = ?";
            deleteSingleElement(a, delete);
            while (it.hasNext()) {
                a = it.next();
                deleteSingleElement(a, delete);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private <T extends Storable> void deleteSingleElement(T storable, String deleteString)
            throws PersistenceLayerException {
        com.gip.xyna.utils.db.Parameter paras = new com.gip.xyna.utils.db.ExtendedParameter();
        Column colPK = MySQLPersistenceLayer.getColumnForPrimaryKey(storable);
        this.mySQLPersistenceLayer.addToParameter(paras, colPK, storable.getPrimaryKey(), storable);
        try {
            sqlUtils.cacheStatement(deleteString);
            sqlUtils.executeDML(deleteString, paras);
        } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
            throw new XNWH_RetryTransactionException(e);
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
            validateConnection();
            throw new XNWH_GeneralPersistenceLayerException(null, e);
        }
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException {
        String delete = "truncate table " + Storable.getPersistable(klass).tableName().toLowerCase();
        try {
            sqlUtils.executeDML(delete, null);
        } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
            throw new XNWH_RetryTransactionException(e);
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
            validateConnection();
            throw new XNWH_GeneralPersistenceLayerException(null, e);
        }
    }

    public int executeDML(PreparedCommand cmdInterface, Parameter paras) throws PersistenceLayerException {
        if (!(cmdInterface instanceof MySQLPreparedCommand)) {
            throw new XNWH_IncompatiblePreparedObjectException(PreparedCommand.class.getSimpleName());
        }
        MySQLPreparedCommand cmd = (MySQLPreparedCommand) cmdInterface;
        ensureOpen();
        com.gip.xyna.utils.db.Parameter sqlUtilsParas = null;
        if (paras != null) {
            for (int i = 0; i < paras.size(); i++) {
                if (paras.get(i) instanceof Boolean) {
                    sqlUtilsParas = new ExtendedParameter();
                    break;
                }
            }
            if (sqlUtilsParas == null) {
                sqlUtilsParas = new com.gip.xyna.utils.db.Parameter();
            }
            for (int i = 0; i < paras.size(); i++) {
                if (paras.get(i) instanceof Boolean) {
                    sqlUtilsParas.addParameter(new BooleanWrapper((Boolean) paras.get(i)));
                } else {
                    MySQLPersistenceLayer.addToParameterWithArraySupportAsStrings(sqlUtilsParas, paras.get(i));
                }
            }
        }
        try {
            sqlUtils.cacheStatement(cmd.getSqlString());
            return sqlUtils.executeDML(cmd.getSqlString(), sqlUtilsParas);
        } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
            throw new XNWH_RetryTransactionException(e);
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
            validateConnection();
            throw new XNWH_GeneralPersistenceLayerException("problem executing sql command: \"" + cmd.getSqlString()
                    + "\" [" + sqlUtilsParas + "]", e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T extends Storable> com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T> getReader(
            Class<T> klass)
            throws PersistenceLayerException {
        T i;
        try {
            i = klass.getDeclaredConstructor().newInstance();
        } catch (IllegalArgumentException e) {
            throw new XNWH_GeneralPersistenceLayerException("Storable " + klass.getName()
                    + " must have a valid no arguments constructor.", e);
        } catch (InvocationTargetException e) {
            throw new XNWH_GeneralPersistenceLayerException("Storable " + klass.getName()
                    + " must have a valid no arguments constructor.", e);
        } catch (NoSuchMethodException e) {
            throw new XNWH_GeneralPersistenceLayerException("Storable " + klass.getName()
                    + " must have a valid no arguments constructor.", e);
        } catch (SecurityException e) {
            throw new XNWH_GeneralPersistenceLayerException("Storable " + klass.getName()
                    + " must have a valid no arguments constructor.", e);
        } catch (InstantiationException e) {
            throw new XNWH_GeneralPersistenceLayerException("Storable " + klass.getName()
                    + " must have a valid no arguments constructor.", e);
        } catch (IllegalAccessException e) {
            throw new XNWH_GeneralPersistenceLayerException("Storable " + klass.getName()
                    + " must have a valid no arguments constructor.", e);
        }
        return (com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T>) i.getReader();
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> Collection<T> loadCollection(final Class<T> klass) throws PersistenceLayerException {
        ensureOpen();
        final com.gip.xyna.xnwh.persistence.ResultSetReader<? extends T> reader = getReader(klass);
        try {
            ArrayList<T> list = sqlUtils.query(
                    "select * from " + Storable.getPersistable(klass).tableName().toLowerCase(), null,
                    new ResultSetReaderWrapper<T>(reader, this.mySQLPersistenceLayer.useZippedBlobs(), klass));
            return list;
        } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
            throw new XNWH_RetryTransactionException(e);
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
            validateConnection();
            throw new XNWH_GeneralPersistenceLayerException(null, e);
        }
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> void persistCollection(Collection<T> storableCollection)
            throws PersistenceLayerException {
        int size = storableCollection.size();
        if (size == 0) {
            return;
        }
        if (size == 1) {
            persistObject(storableCollection.iterator().next());
            return;
        }
        ensureOpen();
        Iterator<T> it = storableCollection.iterator();
        // erstmal ein select for update auf alle angegebenen objekte. dann schauen, ob
        // sie da sind,
        // und daran entscheiden, ob man update oder insert machen muss
        // jeweils als batch-operation.
        // TODO merging von objekten mit dem gleichen pk

        final T firstElement = storableCollection.iterator().next();
        // gibt bytearray als string zurück
        ResultSetReader<Object> resultSetReaderForPK = MySQLPersistenceLayer
                .getResultSetReaderForPrimaryKey(firstElement.getPrimaryKey());
        Column[] columns = firstElement.getColumns();
        final Column colPK = MySQLPersistenceLayer.getColumnForPrimaryKey(firstElement);

        Persistable persistable = Storable.getPersistable(firstElement.getClass());
        StringBuilder start = new StringBuilder("select ").append(persistable.primaryKey()).append(" from ")
                .append(persistable.tableName().toLowerCase()).append(" where ");
        int remainingSize = size;
        Set<Object> existingPKs = new HashSet<Object>();
        while (remainingSize > 0) {
            StringBuilder selectSql = new StringBuilder(start);
            Object[] pks = new Object[Math.min(100, remainingSize)];
            for (int i = 0; i < pks.length; i++) {
                T s = it.next();
                pks[i] = s.getPrimaryKey();
            }
            InList inList;
            try {
                inList = new InList(pks, 100) {

                    @Override
                    protected void addParameter(com.gip.xyna.utils.db.Parameter parameter, Object p) {
                        try {
                            mySQLPersistenceLayer.addToParameter(parameter, colPK, p, firstElement);
                        } catch (PersistenceLayerException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    protected com.gip.xyna.utils.db.Parameter createParameter() {
                        return new ExtendedParameter();
                    }

                };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            selectSql.append(inList.getSQL(persistable.primaryKey()));
            selectSql.append(" for update");
            try {
                List<Object> result = sqlUtils.query(selectSql.toString(), inList.getParams(), resultSetReaderForPK);
                existingPKs.addAll(result);
            } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
                throw new XNWH_RetryTransactionException(e);
            } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
                validateConnection();
                throw new XNWH_GeneralPersistenceLayerException("could not persist collection.", e);
            }

            remainingSize -= pks.length;
        }

        it = storableCollection.iterator();

        String insert = createInsertStatement(columns, persistable.tableName().toLowerCase());
        String update = createUpdateStatement(columns, firstElement);
        List<com.gip.xyna.utils.db.Parameter> insertParas = new ArrayList<com.gip.xyna.utils.db.Parameter>();
        List<com.gip.xyna.utils.db.Parameter> updateParas = new ArrayList<com.gip.xyna.utils.db.Parameter>();
        while (it.hasNext()) {
            T s = it.next();
            // transformation des pks, die im set existingPKs verwendet wird.
            // (TODO etwas umständlich, das könnte man refactorn. oder man könnte die
            // unformatierten pks
            // vergleichen, dann müsste man im bytearray fall den comparator korrekt
            // überschreiben)
            com.gip.xyna.utils.db.Parameter tempPara = new com.gip.xyna.utils.db.Parameter();
            this.mySQLPersistenceLayer.addToParameter(tempPara, colPK, s.getPrimaryKey(), s);
            Object transformedPk = tempPara.getParameter(1);
            if (existingPKs.contains(transformedPk)) {
                // update
                com.gip.xyna.utils.db.Parameter paras = this.mySQLPersistenceLayer
                        .createParasForInsertAndUpdate(columns, s);
                // parameter für whereclause adden
                this.mySQLPersistenceLayer.addToParameter(paras, colPK, s.getPrimaryKey(), s);
                updateParas.add(paras);
            } else {
                com.gip.xyna.utils.db.Parameter paras = this.mySQLPersistenceLayer
                        .createParasForInsertAndUpdate(columns, s);

                insertParas.add(paras);
                existingPKs.add(transformedPk); // damit nicht später in der gleichen collection erneut insert versucht
                                                // wird
            }
        }
        try {
            if (insertParas.size() > 0) {
                sqlUtils
                        .executeDMLBatch(insert,
                                insertParas.toArray(new com.gip.xyna.utils.db.Parameter[insertParas.size()]));
            }
            if (updateParas.size() > 0) {
                sqlUtils
                        .executeDMLBatch(update,
                                updateParas.toArray(new com.gip.xyna.utils.db.Parameter[updateParas.size()]));
            }
        } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
            validateConnection();
            throw new XNWH_RetryTransactionException(e);
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
            throw new XNWH_GeneralPersistenceLayerException("could not persist collection.", e);
        }
    }

    private static String createInsertStatement(Column[] columns, String tableName) throws PersistenceLayerException {
        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        if (columns.length == 0) {
            throw new XNWH_GeneralPersistenceLayerException("no columns found to persist");
        }
        for (int i = 0; i < columns.length - 1; i++) {
            cols.append(columns[i].name()).append(", ");
            vals.append("?, ");
        }
        cols.append(columns[columns.length - 1].name());
        vals.append("?");

        return "insert into " + tableName + " (" + cols + ") values (" + vals + ")";
    }

    @SuppressWarnings("rawtypes")
    private static <T extends Storable> String createUpdateStatement(Column[] columns, T storable)
            throws PersistenceLayerException {
        if (columns.length == 0) {
            throw new XNWH_GeneralPersistenceLayerException("no columns found to persist");
        }
        StringBuilder setter = new StringBuilder();
        setter.append(" set ");
        for (int i = 0; i < columns.length - 1; i++) {
            setter.append(columns[i].name()).append(" = ?, ");
        }
        setter.append(columns[columns.length - 1].name()).append(" = ? ");
        String tableName = storable.getTableName().toLowerCase();
        StringBuilder whereClause = new StringBuilder().append(" where ")
                .append(Storable.getPersistable(storable.getClass()).primaryKey())
                .append(" = ?");
        return new StringBuilder().append("update ").append(tableName).append(setter).append(whereClause).toString();
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException {

        // FIXME dieser Code ist aus OraclePersistenceLayer kopiert. Aufgrund der
        // Ähnlichkeiten sollte er
        // aus beiden extrahiert werden
        ensureOpen();
        // überprüfen, ob objekt bereits in db ist
        String sqlString = "select count(*) from " + storable.getTableName().toLowerCase() + " where "
                + Storable.getPersistable(storable.getClass()).primaryKey() + " = ?";
        boolean existedBefore = false;
        try {

            // determine whether the object exists. use a trace logger for this since the
            // following statement (update or
            // insert will contain at least the same information)
            com.gip.xyna.utils.db.Parameter parasForCountQuery = new com.gip.xyna.utils.db.ExtendedParameter();
            Column colPK = MySQLPersistenceLayer.getColumnForPrimaryKey(storable);
            this.mySQLPersistenceLayer.addToParameter(parasForCountQuery, colPK, storable.getPrimaryKey(), storable);
            sqlUtils.cacheStatement(sqlString);
            int cnt = sqlUtils.queryInt(sqlString, parasForCountQuery);

            MySQLPersistenceLayerConnection.UpdateInsert updateOrInsert;
            if (cnt == 0) {
                updateOrInsert = MySQLPersistenceLayerConnection.UpdateInsert.insert;
            } else if (cnt == 1) {
                updateOrInsert = MySQLPersistenceLayerConnection.UpdateInsert.update;
            } else {
                // this should never happen without malconfiguration
                throw new XNWH_GeneralPersistenceLayerException("result of count-statement \"" + sqlString + "\" ("
                        + storable.getPrimaryKey() + ") was " + cnt + ". only 0 or 1 are valid results.");
            }
            int insertRetryCounter = 0;
            int endlessLoopCounter = 0;
            while (updateOrInsert != MySQLPersistenceLayerConnection.UpdateInsert.done) {
                if (endlessLoopCounter++ > 100) {
                    throw new RuntimeException("unexpectedly long loop"); // sollte nicht passieren
                }
                // solange versuchen, bis insert oder update erfolgreich ist.
                if (updateOrInsert == MySQLPersistenceLayerConnection.UpdateInsert.insert) {
                    // TODO performance: hier kann man die erstellten parameter und das statement
                    // cachen, wenn die whileschleife hier mehrfach vorbei kommt.
                    // das passiert aber nicht oft, dass hier die while schleife mehrfach den
                    // insert-fall durchläuft.
                    // insert
                    Column[] columns = storable.getColumns();

                    String insert = createInsertStatement(columns,
                            storable.getTableName().toLowerCase());
                    com.gip.xyna.utils.db.Parameter paras = this.mySQLPersistenceLayer
                            .createParasForInsertAndUpdate(columns, storable);

                    try {
                        sqlUtils.cacheStatement(insert);
                        sqlUtils.executeDML(insert, paras);
                    } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
                        if (e.getCause() instanceof SQLException) {
                            SQLException sqlEx = (SQLException) e.getCause();
                            validateConnection();

                            // logger.debug(" UniqueConstraintViolation? " +
                            // sqlEx.getClass().getSimpleName() + sqlEx.getMessage() );
                            // unter Java 6:
                            // com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException:
                            // Duplicate entry 'corrT_SingleS1_eda1c56f-31' for key
                            // 'seriesinformation_correlationId_idx'

                            // Leider werden wegen Einführung von jdbc4 unterschiedliche Exceptions
                            // geworfen,
                            // je nachdem, ob Java 5 oder 6 verwendet wird.
                            // Java5: com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException
                            // Java6:
                            // com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
                            // Da hier mit Java 5 kompiliert werden soll, kann leider kein instanceof
                            // verwendet
                            // werden, da es sonst die Fehlermeldung "The type
                            // java.sql.SQLIntegrityConstraintViolationException
                            // cannot be resolved. It is indirectly referenced from required .class files"
                            // gibt.
                            // Daher nun Test über den Classname
                            String className = sqlEx.getClass().getSimpleName();

                            boolean uniqueConstraintViolated = className
                                    .contains("MySQLIntegrityConstraintViolationException");
                            if (uniqueConstraintViolated) {
                                cnt = sqlUtils.queryInt(sqlString, parasForCountQuery);
                                if (cnt == 0) {
                                    // entweder bereits wieder gelöscht (unwahrscheinlich) oder die
                                    // uniqueconstraintverletzung ist von einer anderen spalte bedingt
                                    // updateOrInsert weiterhin auf insert
                                    if (++insertRetryCounter > MySQLPersistenceLayer.MAX_INSERT_RETRY_COUNTER) {
                                        throw e;
                                    }
                                } else {
                                    // ok update probieren
                                    updateOrInsert = MySQLPersistenceLayerConnection.UpdateInsert.update;
                                    insertRetryCounter = 0;
                                }
                                continue;
                            }
                        }
                        throw e; // falls nicht continue
                    }
                    existedBefore = false;
                } else if (updateOrInsert == MySQLPersistenceLayerConnection.UpdateInsert.update) {
                    // update
                    Column[] columns = storable.getColumns();

                    String updateStmt = createUpdateStatement(columns, storable);
                    com.gip.xyna.utils.db.Parameter paras = this.mySQLPersistenceLayer
                            .createParasForInsertAndUpdate(columns, storable);
                    // parameter für whereclause adden
                    this.mySQLPersistenceLayer.addToParameter(paras, colPK, storable.getPrimaryKey(), storable);

                    sqlUtils.cacheStatement(updateStmt);
                    int modified = sqlUtils.executeDML(updateStmt, paras);
                    if (modified == 0) {
                        updateOrInsert = MySQLPersistenceLayerConnection.UpdateInsert.insert;
                        continue;
                    }
                    existedBefore = true;
                }
                updateOrInsert = MySQLPersistenceLayerConnection.UpdateInsert.done;
            }

        } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
            throw new XNWH_RetryTransactionException(e);
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
            throw new XNWH_GeneralPersistenceLayerException("could not persist storable " + storable, e);
        }

        return existedBefore;

    }

    void validateConnection() throws PersistenceLayerException {
        try {
            if (sqlUtils != null && sqlUtils.getConnection() != null && !sqlUtils.getConnection().isValid(0)) {
                Connection c = sqlUtils.getConnection();
                closeConnection(); // connection is no longer valid and needs to be closed.
                if (!c.isClosed()) {// closeConnection() keeps the connection open if it is shared
                    c.close(); // but since the connection is no longer valid, it has to be closed
                }
            }
        } catch (SQLException e1) {
            logger.warn("Could not validate connection.", e1);
        }
    }

    public PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException {
        return new MySQLPreparedCommand(cmd);
    }

    public <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException {
        return new MySQLPreparedQuery<E>(query);
    }

    public <E> List<E> query(PreparedQuery<E> queryInterface, Parameter parameter, int maxRows)
            throws PersistenceLayerException {
        MySQLPreparedQuery<E> query = (MySQLPreparedQuery<E>) queryInterface;
        return this.query(queryInterface, parameter, maxRows, query.getReader());
    }

    public <E> List<E> query(PreparedQuery<E> queryInterface, Parameter parameter, int maxRows,
            final com.gip.xyna.xnwh.persistence.ResultSetReader<? extends E> resultSetReader)
            throws PersistenceLayerException {
        if (!(queryInterface instanceof MySQLPreparedQuery)) {
            throw new XNWH_IncompatiblePreparedObjectException(PreparedQuery.class.getSimpleName());
        }
        MySQLPreparedQuery<E> query = (MySQLPreparedQuery<E>) queryInterface;
        ensureOpen();
        com.gip.xyna.utils.db.Parameter paras = null;

        boolean[] isLike = query.getQuery().getLikeParameters();

        if (parameter != null) {
            for (int i = 0; i < parameter.size(); i++) {
                if (!isLike[i]) {
                    if (parameter.get(i) instanceof Boolean) {
                        paras = new ExtendedParameter();
                        break;
                    }
                }
            }
            if (paras == null) {
                paras = new com.gip.xyna.utils.db.Parameter();
            }
            for (int i = 0; i < parameter.size(); i++) {
                if (parameter.get(i) instanceof String) {
                    paras.addParameter(
                            SelectionParser.escapeParams((String) parameter.get(i), isLike[i], new EscapeForMySQL()));
                } else if (parameter.get(i) instanceof Boolean) {
                    paras.addParameter(new BooleanWrapper((Boolean) parameter.get(i)));
                } else {
                    MySQLPersistenceLayer.addToParameterWithArraySupportAsStrings(paras, parameter.get(i));
                }
            }
        }

        String sqlQuery = query.getQuery().getSqlString();

        // Umwandlung zu rlike, da MariaDB regexp_like() nicht unterstützt
        sqlQuery = modifyFunction(sqlQuery, PersistenceExpressionVisitors.QueryFunctionStore.REGEXP_LIKE_SQL_FUNCTION,
                "%Column% RLIKE (%Params%)");

        // TODO cachen
        if (maxRows == 1 && transactionProperties != null
                && transactionProperties.contains(TransactionProperty.selectRandomElement())) {
            if (!sqlQuery.toLowerCase().contains("order by")) { // else altes order by beibehalten
                if (sqlQuery.toLowerCase().endsWith("for update")) { // vor das "for update" einfügen
                    sqlQuery = sqlQuery.substring(0, sqlQuery.length() - "for update".length())
                            + " order by rand() limit 0, 1 for update";
                } else {
                    sqlQuery += " order by rand() limit 0, 1";
                }
            }
        } else if (maxRows > -1 && maxRows < Integer.MAX_VALUE) {
            // beschränkung des ergebnisses. das ist das einzige mysql spezifische
            // limit ist vor "for update", aber nach allem anderen.
            // http://dev.mysql.com/doc/refman/5.0/en/select.html
            sqlQuery = sqlQuery.trim();
            if (sqlQuery.toLowerCase().endsWith("for update")) {
                sqlQuery = sqlQuery.substring(0, sqlQuery.length() - "for update".length()) + " limit 0, " + maxRows
                        + " for update";
            } else {
                sqlQuery += " limit 0, " + maxRows;
            }
        }
        try {
            sqlUtils.cacheStatement(sqlQuery);
            ArrayList<E> list = sqlUtils.query(sqlQuery, paras,
                    new ResultSetReaderWrapper<E>(resultSetReader, this.mySQLPersistenceLayer.useZippedBlobs(),
                            MySQLPersistenceLayer.getTableClassMap().get(query.getTable().toLowerCase())));
            return list;
        } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
            throw new XNWH_RetryTransactionException(e);
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
            validateConnection();
            throw new XNWH_GeneralPersistenceLayerException("query \"" + sqlQuery + "\" [" + paras
                    + "] could not be executed.", e);
        }
    }

    /**
     * Passt die Schreibweise einer SQL-Funktion (z.B. regexp_like) an MySQL an.
     */
    private String modifyFunction(String sqlQuery, String sqlFunction, String replacement) {
        if (sqlQuery.contains(sqlFunction)) {
            String preExpr = "([\\s\\(]+)"; // Leerzeichen oder Klammer stehen am Anfang
            String params = "([^,]*),([^)]*)"; // Parameter der SQL-Funktion
            Pattern pattern = Pattern.compile(preExpr + "\\Q" + sqlFunction + "\\E" + "\\s*\\(" + params + "\\)",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(sqlQuery);
            if (matcher.find()) {
                replacement = replacement.replace("%Column%", "$2").replace("%Params%", "$3");
                sqlQuery = matcher.replaceAll("$1" + replacement); // eigentliche Ersetzung
            }
        }
        return sqlQuery;
    }

    /**
     * Note that this only does any locking if autocommit is disabled
     */
    @SuppressWarnings("rawtypes")
    public <T extends Storable> void queryOneRowForUpdate(final T storable) throws PersistenceLayerException,
            XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
        queryOneRowInternally(storable, true);
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> void queryOneRow(final T storable) throws PersistenceLayerException,
            XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
        queryOneRowInternally(storable, false);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T extends Storable> void queryOneRowInternally(final T storable, boolean forUpdate)
            throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
        ensureOpen();
        StringBuilder selectString = new StringBuilder().append("select * from ")
                .append(storable.getTableName().toLowerCase()).append(" where ")
                .append(Storable.getPersistable(storable.getClass()).primaryKey()).append(" = ?");
        if (forUpdate) {
            selectString.append(" for update");
        }
        try {
            com.gip.xyna.utils.db.Parameter paras = new com.gip.xyna.utils.db.ExtendedParameter();
            Column colPK = MySQLPersistenceLayer.getColumnForPrimaryKey(storable);
            this.mySQLPersistenceLayer.addToParameter(paras, colPK, storable.getPrimaryKey(), storable);
            sqlUtils.cacheStatement(selectString.toString());
            T result = (T) sqlUtils.queryOneRow(selectString.toString(), paras, new ResultSetReaderWrapper(
                    storable.getReader(), this.mySQLPersistenceLayer.useZippedBlobs(), storable.getClass()));
            if (result == null) {
                throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(String.valueOf(storable.getPrimaryKey()),
                        storable.getTableName());
            }
            storable.setAllFieldsFromData(result);
        } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
            throw new XNWH_RetryTransactionException(e);
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
            validateConnection();
            throw new XNWH_GeneralPersistenceLayerException(
                    "query \"" + selectString + "\" [" + storable.getPrimaryKey()
                            + "] could not be executed.",
                    e);
        }
    }

    public <E> E queryOneRow(PreparedQuery<E> query, Parameter parameter) throws PersistenceLayerException {
        List<E> l = query(query, parameter, 1);
        if (l.size() > 0) {
            return l.get(0);
        } else {
            return null;
        }
    }

    public void rollback() throws PersistenceLayerException {
        ensureOpen();
        try {
            sqlUtils.rollback();
        } catch (com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException e) {
            throw new XNWH_RetryTransactionException(e);
        } catch (com.gip.xyna.xnwh.exception.SQLRuntimeException e) {
            validateConnection();
            throw new XNWH_GeneralPersistenceLayerException("could not rollback transaction.", e);
        }
    }

    private Set<TransactionProperty> transactionProperties;

    public void setTransactionProperty(TransactionProperty property) {
        if (transactionProperties == null) {
            transactionProperties = new HashSet<TransactionProperty>();
        }
        transactionProperties.add(property);
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> arg0)
            throws PersistenceLayerException {
        try {
            connectionPool.ensureConnectivity(sqlUtils.getConnection());
        } catch (SQLException e) {
            throw new XNWH_RetryTransactionException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    public <T extends Storable> void removeTable(Class<T> arg0, Properties arg1) throws PersistenceLayerException {
        MySQLPersistenceLayer.getTableClassMap().remove(Storable.getPersistable(arg0).tableName().toLowerCase());
        for (Column col : Storable.getColumns(arg0)) {
            this.mySQLPersistenceLayer.getColumnMap().remove(col);
        }
    }

}