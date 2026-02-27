/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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



import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.TransactionProperty;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseColumnInfo;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseIndexCollision;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabasePersistenceLayerConnectionWithAlterTableSupport;



// unterstützt nicht mehrere threads die die gleiche connection benutzen
class MySQLPersistenceLayerConnection implements PersistenceLayerConnection, DatabasePersistenceLayerConnectionWithAlterTableSupport {

  enum UpdateInsert {
    update, insert, done;
  }


  static final Logger logger = CentralFactoryLogging.getLogger(MySQLPersistenceLayerConnection.class);
  private final MySQLPersistenceLayerRegularConnection regularConnection;
  private final SQLUtils sqlUtils;
  private final List<MySQLPersistenceLayerRegularConnection> sharedConnections;


  public MySQLPersistenceLayerConnection(MySQLPersistenceLayer mySQLPersistenceLayer) throws PersistenceLayerException {
    this(mySQLPersistenceLayer, false);
  }


  public MySQLPersistenceLayerConnection(MySQLPersistenceLayer mySQLPersistenceLayer, boolean isDedicated)
      throws PersistenceLayerException {
    sqlUtils = mySQLPersistenceLayer.createSQLUtils(isDedicated);
    try {
      if (sqlUtils.getConnection() != null) {
        sqlUtils.getConnection().setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
      }
    } catch (SQLException e) {
      logger.warn("Unable to set TransactionIsolationLevel, this might lead to strange behaviour if the global IsolationLevel differs.");
    }
    ConnectionPool connectionPool;
    if (isDedicated) {
      connectionPool = mySQLPersistenceLayer.getDedicatedConnectionPool();
    } else {
      connectionPool = mySQLPersistenceLayer.getConnectionPool();
    }
    sharedConnections = new ArrayList<>();
    regularConnection = new MySQLPersistenceLayerRegularConnection(mySQLPersistenceLayer, connectionPool, sqlUtils, sharedConnections);
  }


  public MySQLPersistenceLayerConnection(MySQLPersistenceLayer mySQLPersistenceLayer, MySQLPersistenceLayerConnection shareConnectionPool) {
    sqlUtils = shareConnectionPool.sqlUtils;
    ConnectionPool connectionPool = mySQLPersistenceLayer.getConnectionPool();
    sharedConnections = shareConnectionPool.sharedConnections;
    regularConnection = new MySQLPersistenceLayerRegularConnection(mySQLPersistenceLayer, connectionPool, sqlUtils, sharedConnections);
  }


  @Override
  public boolean doesTableExist(Persistable persistable) {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      return alterTableConn.doesTableExist(persistable);
    }
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void createTable(Persistable persistable, Class<T> klass, Column[] cols) {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      alterTableConn.createTable(persistable, klass, cols);
    }
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> Set<DatabaseIndexCollision> checkColumns(Persistable persistable, Class<T> klass, Column[] columns)
      throws PersistenceLayerException {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      return alterTableConn.checkColumns(persistable, klass, columns);
    }
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void alterColumns(Set<DatabaseIndexCollision> columns) throws PersistenceLayerException {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      alterTableConn.alterColumns(columns);
    }
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> String getDefaultColumnTypeString(Column col, Class<T> klass) {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      return alterTableConn.getDefaultColumnTypeString(col, klass);
    }
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> boolean areColumnsCompatible(Column col, Class<T> klass, DatabaseColumnInfo colInfo) {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      return alterTableConn.areColumnsCompatible(col, klass, colInfo);
    }
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> boolean areBaseTypesCompatible(Column col, Class<T> klass, DatabaseColumnInfo colInfo) {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      return alterTableConn.areBaseTypesCompatible(col, klass, colInfo);
    }
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> boolean isTypeDependentOnSizeSpecification(Column col, Class<T> klass) {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      return alterTableConn.isTypeDependentOnSizeSpecification(col, klass);
    }
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void modifyColumnsCompatible(Column col, Class<T> klass, String tableName) {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      alterTableConn.modifyColumnsCompatible(col, klass, tableName);
    }
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void widenColumnsCompatible(Column col, Class<T> klass, String tableName) {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      alterTableConn.widenColumnsCompatible(col, klass, tableName);
    }
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> String getCompatibleColumnTypesAsString(Column col, Class<T> klass) {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      return alterTableConn.getCompatibleColumnTypesAsString(col, klass);
    }
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> String getTypeAsString(Column col, Class<T> klass) {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      return alterTableConn.getTypeAsString(col, klass);
    }
  }


  @Override
  public long getPersistenceLayerInstanceId() {
    try (MySQLPersistenceLayerAlterTableConnection alterTableConn = regularConnection.createAlterTableConnection()) {
      return alterTableConn.getPersistenceLayerInstanceId();
    }
  }


  @Override
  public void commit() throws PersistenceLayerException {
    regularConnection.commit();
  }


  @Override
  public void rollback() throws PersistenceLayerException {
    regularConnection.rollback();
  }


  @Override
  public void closeConnection() throws PersistenceLayerException {
    regularConnection.closeConnection();
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException {
    return regularConnection.persistObject(storable);
  }


  @Override
  public PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException {
    return regularConnection.prepareCommand(cmd);
  }


  @Override
  public <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException {
    return regularConnection.prepareQuery(query);
  }


  @Override
  public int executeDML(PreparedCommand cmd, Parameter paras) throws PersistenceLayerException {
    return regularConnection.executeDML(cmd, paras);
  }


  @Override
  public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows) throws PersistenceLayerException {
    return regularConnection.query(query, parameter, maxRows);
  }


  @Override
  public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows,
                           ResultSetReader<? extends E> reader)
      throws PersistenceLayerException {
    return regularConnection.query(query, parameter, maxRows, reader);
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void queryOneRow(T storable) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    regularConnection.queryOneRow(storable);
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void queryOneRowForUpdate(T storable)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    regularConnection.queryOneRowForUpdate(storable);
  }


  @Override
  public <E> E queryOneRow(PreparedQuery<E> query, Parameter parameter) throws PersistenceLayerException {
    return regularConnection.queryOneRow(query, parameter);
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
    return regularConnection.containsObject(storable);
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void persistCollection(Collection<T> storableCollection) throws PersistenceLayerException {
    regularConnection.persistCollection(storableCollection);
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> Collection<T> loadCollection(Class<T> klass) throws PersistenceLayerException {
    return regularConnection.loadCollection(klass);
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException {
    regularConnection.delete(storableCollection);
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void deleteOneRow(T toBeDeleted) throws PersistenceLayerException {
    regularConnection.deleteOneRow(toBeDeleted);
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException {
    regularConnection.deleteAll(klass);
  }


  @Override
  public void setTransactionProperty(TransactionProperty property) {
    regularConnection.setTransactionProperty(property);
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> storableClazz) throws PersistenceLayerException {
    regularConnection.ensurePersistenceLayerConnectivity(storableClazz);
  }


  @Override
  public boolean isOpen() {
    return regularConnection.isOpen();
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void addTable(Class<T> klass, boolean forceWidening, Properties properties) throws PersistenceLayerException {
    regularConnection.addTable(klass, forceWidening, properties);
  }


  @Override
  @SuppressWarnings("rawtypes")
  public <T extends Storable> void removeTable(Class<T> klass, Properties properties) throws PersistenceLayerException {
    regularConnection.removeTable(klass, properties);
  }
}