/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.persistence.mocks;

import java.util.Collection;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.TransactionProperty;


/**
 *
 */
public class ODSConnectionMockBase implements ODSConnection {

  public void commit() throws PersistenceLayerException {}

  public void rollback() throws PersistenceLayerException {}

  public void closeConnection() throws PersistenceLayerException {}

  public <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException {
    return false;
  }
  public PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException {
    return null;
  }

  public <E> PreparedQuery<E> prepareQuery( final Query<E> query) throws PersistenceLayerException {
    return new PreparedQuery<E>() {

      public String getTable() {
        return query.getTable();
      }

      public ResultSetReader<? extends E> getReader() {
        return query.getReader();
      }
    };
  }

  public int executeDML(PreparedCommand cmd, Parameter paras) throws PersistenceLayerException {
    return 0;
  }

  public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows) throws PersistenceLayerException {
    return null;
  }
  
  public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows, ResultSetReader<? extends E> reader) throws PersistenceLayerException {
    return null;
  }

  public <T extends Storable> void queryOneRow(T storable) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
  }

  public <T extends Storable> void queryOneRowForUpdate(T storable) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
  }

  public <E> E queryOneRow(PreparedQuery<E> query, Parameter parameter) throws PersistenceLayerException {
    return null;
  }

  public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
    return false;
  }

  public <T extends Storable> void persistCollection(Collection<T> storableCollection)
                  throws PersistenceLayerException {
  }

  public <T extends Storable> Collection<T> loadCollection(Class<T> klass) throws PersistenceLayerException {
    return null;
  }

  public <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException {
  }

  public <T extends Storable> void deleteOneRow(T toBeDeleted) throws PersistenceLayerException {
  }

  public <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException {
  }
  public void setTransactionProperty(TransactionProperty property) {
  }

  public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> storableClazz)
                  throws PersistenceLayerException {
  }

  public PreparedCommand prepareCommand(Command cmd, boolean listenToPersistenceLayerChanges)
                  throws PersistenceLayerException {
    return null;
  }

  public <E> PreparedQuery<E> prepareQuery(Query<E> query, boolean listenToPersistenceLayerChanges)
                  throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return null;
  }

  public <T extends Storable<?>> FactoryWarehouseCursor<T> getCursor(String sqlQuery, Parameter parameters,
                                                                     ResultSetReader<T> rsr, int cacheSize)
                  throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isOpen() { return false; }

  public ODSConnectionType getConnectionType() { return null; }

  public void executeAfterCommit(Runnable runnable) {}

  public void executeAfterRollback(Runnable runnable) {}

  public void executeAfterClose(Runnable runnable) {}

  public boolean isInTransaction() {
    return false;
  }
  
  public void ensurePersistenceLayerConnectivity(List<Class<? extends Storable<?>>> storableClazz)
                  throws PersistenceLayerException {
  }

  public void executeAfterCommitFails(Runnable runnable) {    
  }

  public void executeAfterCommit(Runnable runnable, int priority) {
    // TODO Auto-generated method stub
    
  }

  public void executeAfterCommitFails(Runnable runnable, int priority) {
    // TODO Auto-generated method stub
    
  }

  public void executeAfterRollback(Runnable runnable, int priority) {
    // TODO Auto-generated method stub
    
  }

  public void executeAfterClose(Runnable runnable, int priority) {
    // TODO Auto-generated method stub
    
  }

  public <T extends Storable<?>> FactoryWarehouseCursor<T> getCursor(String sqlQuery, Parameter parameters,
                                                                     ResultSetReader<T> rsr, int cacheSize,
                                                                     PreparedQueryCache cache)
      throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return null;
  }

  public void shareConnectionPools(ODSConnection con) {
    // TODO Auto-generated method stub
    
  }
  
}
