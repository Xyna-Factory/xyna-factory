/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xnwh.persistence.devnull;

import java.io.Reader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import com.gip.xyna.utils.db.UnsupportingResultSet;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.TransactionProperty;


/**
 * frisst alles, merkt sich nichts.
 */
public class XynaDevNullPersistenceLayer implements PersistenceLayer {

  public PersistenceLayerConnection getConnection() throws PersistenceLayerException {
    return new DevNullPersistenceLayerConnection();
  }
  
  
  public PersistenceLayerConnection getDedicatedConnection() throws PersistenceLayerException {
    return new DevNullPersistenceLayerConnection();
  }
  

  public void init(Long pliID, String... args) throws PersistenceLayerException {
  }
  
  private static class DevNullPersistenceLayerConnection implements PersistenceLayerConnection {

    public <T extends Storable> void addTable(Class<T> klass, boolean forceWidening, Properties props) throws PersistenceLayerException {
    }
    
    public <T extends Storable> void removeTable(Class<T> klass, Properties props) throws PersistenceLayerException {
    }


    public void closeConnection() throws PersistenceLayerException {
    }


    public void commit() throws PersistenceLayerException {
    }


    public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
      return true;
    }


    public <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException {
    }


    public <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException {
    }


    public int executeDML(PreparedCommand cmd, Parameter paras) throws PersistenceLayerException {
      return 0;
    }


    public <T extends Storable> Collection<T> loadCollection(Class<T> klass) throws PersistenceLayerException {
      return new ArrayList<T>();
    }


    public <T extends Storable> void persistCollection(Collection<T> storableCollection)
                    throws PersistenceLayerException {
    }


    public <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException {
      return false;
    }


    public PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException {
      return new DevNullPreparedCommand(cmd);
    }


    public <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException {
      return new DevNullPreparedQuery<E>(query);
    }


    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows) throws PersistenceLayerException {
      return new ArrayList<E>();
    }
    
    
    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows, ResultSetReader<? extends E> reader)
    throws PersistenceLayerException {
      return new ArrayList<E>();
    }


    public <T extends Storable> void queryOneRow(T storable) throws PersistenceLayerException,
                    XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(String.valueOf(storable.getPrimaryKey()), storable.getTableName());
    }


    public <T extends Storable> void queryOneRowForUpdate(T storable) throws PersistenceLayerException,
                    XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(String.valueOf(storable.getPrimaryKey()), storable.getTableName());
    }


    private static class UnsupportingResultSetForCount extends UnsupportingResultSet {

      @Override
      public int getInt(int columnIndex) throws SQLException {
        return 0;
      }


      @Override
      public int getInt(String columnName) throws SQLException {
        return 0;
      }

    }

    public <E> E queryOneRow(PreparedQuery<E> query, Parameter parameter) throws PersistenceLayerException {
      if (query instanceof DevNullPreparedQuery) {
        DevNullPreparedQuery<E> dnpq = (DevNullPreparedQuery<E>)query;
        if (dnpq.getQuery().getSqlString().startsWith("select count(*)")) {
          try {
            return query.getReader().read(new UnsupportingResultSetForCount());
          } catch (SQLException e) {
            throw new XNWH_GeneralPersistenceLayerException("could not create resultset", e);
          }
        } else {
          return null;
        }
      } else {
        throw new XNWH_GeneralPersistenceLayerException("invalid prepared query: " + query);
      }
    }

    public void rollback() throws PersistenceLayerException {     
    }


    public <T extends Storable> void deleteOneRow(T arg0) throws PersistenceLayerException {
    }


    public void setTransactionProperty(TransactionProperty arg0) {
      //nicht unterstützt
    }


    public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> arg0) throws PersistenceLayerException {
    }


    public boolean isOpen() {
      return true;
    }
    
  }
  
  private static class DevNullPreparedQuery<E> implements PreparedQuery<E> {

    private String table;
    private ResultSetReader<? extends E> reader;
    private Query<E> query;
    
    public DevNullPreparedQuery(Query<E> q) {
      this.table = q.getTable();
      this.reader = q.getReader();
      this.query = q;
    }
    
    public ResultSetReader<? extends E> getReader() {
      return reader;
    }

    public String getTable() {
      return table;
    }
    
    public Query<E> getQuery() {
      return query;
    }
    
  }
  
  private static class DevNullPreparedCommand implements PreparedCommand {

    private String table;
    
    public DevNullPreparedCommand(Command cmd) {
      this.table = cmd.getTable();
    }
    
    public String getTable() {
      return table;
    }
    
  }

  public String getInformation() {
    return "DevNull";
  }

  public boolean describesSamePhysicalTables(PersistenceLayer pl) {
    return pl instanceof XynaDevNullPersistenceLayer;
  }

  public String[] getParameterInformation() {
    return new String[0];
  }

  public void shutdown() throws PersistenceLayerException {
  }

  public Reader getExtendedInformation(String[] arg0) {
    return null;
  }


  public PersistenceLayerConnection getConnection(PersistenceLayerConnection shareConnectionPool) throws PersistenceLayerException {
    return new DevNullPersistenceLayerConnection();
  }


  public boolean usesSameConnectionPool(PersistenceLayer plc) {
    if (plc instanceof XynaDevNullPersistenceLayer) {
      return true;
    }
    return false;
  }
  
  
}
