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
package com.gip.xyna.persistence.xsor;



import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xsor.TransactionContext;
import com.gip.xyna.xsor.common.exceptions.GeneralXSORException;
import com.gip.xyna.xsor.common.exceptions.XSORThrowableCarrier;
import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchRequest;
import com.gip.xyna.xsor.indices.search.SearchValue;
import com.gip.xyna.xsor.persistence.PersistenceException;
import com.gip.xyna.xsor.protocol.XSORPayload;
import com.gip.xyna.persistence.xsor.exceptions.XSOR_Process_Exception;
import com.gip.xyna.persistence.xsor.exceptions.XSOR_Severe_Process_Exception;
import com.gip.xyna.persistence.xsor.helper.ClusterPLLogger;
import com.gip.xyna.persistence.xsor.helper.TypedValuesHelper;
import com.gip.xyna.persistence.xsor.indices.StorableBasedSearchCriterion;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.CompositeIndex;
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
import com.gip.xyna.xnwh.persistence.TransactionProperty.TransactionPropertyType;



public class XynaClusterPersistenceLayerConnection implements PersistenceLayerConnection {


  private static final Logger logger = CentralFactoryLogging.getLogger(XynaClusterPersistenceLayerConnection.class);
  private final ClusterPLLogger sqlLogger;

  private XynaClusterPersistenceLayer pl;
  private boolean strictlyCoherent = true;
  private final TransactionContext transactionContext;


  public XynaClusterPersistenceLayerConnection(XynaClusterPersistenceLayer pl) {
    this.pl = pl;
    transactionContext = pl.getXynaScalableObjectRepository().beginTransaction(strictlyCoherent);
    sqlLogger = pl.sqlLogger;
    sqlLogger.info("OPEN", transactionContext);
  }


  public void closeConnection() throws PersistenceLayerException {
    sqlLogger.info("CLOSE", transactionContext);
    try {
      pl.getXynaScalableObjectRepository().endTransaction(transactionContext, strictlyCoherent);
    } catch (GeneralXSORException e) {
      throw new XSOR_Process_Exception("endTransaction", e);
    } catch (XSORThrowableCarrier carrier) {
      XSOR_Process_Exception multipleCause = new XSOR_Process_Exception("endTransaction");
      multipleCause.initCauses(carrier.getCarried().toArray(new Throwable[carrier.getCarried().size()]));
      throw multipleCause;
    } catch (Throwable t) {
      throw new XSOR_Severe_Process_Exception("endTransaction", t);
    }
    pl = null;
  }


  public void commit() throws PersistenceLayerException {
    sqlLogger.debug("COMMIT", transactionContext);
    try {
      pl.getXynaScalableObjectRepository().endTransaction(transactionContext, strictlyCoherent);
    } catch (GeneralXSORException e) {
      throw new XSOR_Process_Exception("endTransaction", e);
    } catch (XSORThrowableCarrier carrier) {
      XSOR_Process_Exception multipleCause = new XSOR_Process_Exception("endTransaction");
      multipleCause.initCauses(carrier.getCarried().toArray(new Throwable[carrier.getCarried().size()]));
      throw multipleCause;
    } catch (Throwable t) {
      throw new XSOR_Severe_Process_Exception("endTransaction", t);
    }
  }


  public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
    if (storable == null) {
      throw new NullPointerException();
    }
    if (storable.getPrimaryKey() == null) {
      throw new IllegalArgumentException("primary key must not be null.");
    }
    // mit dem pk-index suchen
    SearchRequest request = pl.getRegisteredPrimaryKeyRequestForTable(storable.getTableName());
    SearchParameter parameter = new SearchParameter(new SearchValue(storable.getPrimaryKey()));
    List<XSORPayload> result = null;
    try {
      result = pl.getXynaScalableObjectRepository().search(request, parameter, transactionContext, 1, false,
                                                           strictlyCoherent);
    } catch (GeneralXSORException e) {
      throw new XSOR_Process_Exception("search", e);
    } catch (Throwable t) {
      throw new XSOR_Severe_Process_Exception("search", t);
    }

    if (result.size() > 0) { // TODO would we benefit if we could signal count-Requests to the XC?
      return true;
    }
    return false;
  }


  public <T extends Storable> void delete(Collection<T> storables) throws PersistenceLayerException {
    for (T storable : storables) {
      deleteOneRow(storable);
    }
  }


  public <T extends Storable> void deleteAll(Class<T> storableClazz) throws PersistenceLayerException {
    Persistable persi = Storable.getPersistable(storableClazz);
    // TODO performance, own operation to circumvent full table scan invocation?
    // TODO memory footprint, first loading the whole table and iterating over it can require huge amounts of memSpace
    //      workaround: while (("select * from table" maxResults = 1) != null)
    //                    delete that one
    SearchRequest request = new SearchRequest(persi.tableName(), null) {
      @Override
      public boolean fits(XSORPayload arg0, SearchParameter arg1) {
        return true;
      }
    };
    List<XSORPayload> searchResult = null;
    try {
      searchResult = pl.getXynaScalableObjectRepository().search(request, new SearchParameter(), transactionContext,
                                                                 -1, false, strictlyCoherent);
    } catch (GeneralXSORException e1) {
      throw new XSOR_Process_Exception("search", e1);
    } catch (Throwable t) {
      throw new XSOR_Severe_Process_Exception("search", t);
    }

    for (XSORPayload payload : searchResult) {
      try {
        pl.getXynaScalableObjectRepository().deletePayload(payload, transactionContext, strictlyCoherent);
      } catch (GeneralXSORException e) {
        throw new XSOR_Process_Exception("deletePayload", e);
      } catch (Throwable t) {
        throw new XSOR_Severe_Process_Exception("deletePayload", t);
      }
    }

  }


  public <T extends Storable> void deleteOneRow(T storable) throws PersistenceLayerException {
    try {
      XSORPayload payload = returnAsXSORPayload(storable);
      sqlLogger.buildAndLogDeletion(payload, transactionContext);
      sqlLogger.buildAndLogAllColumns(storable);
      pl.getXynaScalableObjectRepository().deletePayload(payload, transactionContext, strictlyCoherent);
    } catch (GeneralXSORException e) {
      throw new XSOR_Process_Exception("deletePayload", e);
    } catch (Throwable t) {
      throw new XSOR_Severe_Process_Exception("deletePayload", t);
    }
  }


  public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> arg0) throws PersistenceLayerException {
    if (pl.getXynaScalableObjectRepository() == null) {
      throw new UnsupportedOperationException(""); // FIXME could we ask our clusterProvider?
    }
  }


  public int executeDML(PreparedCommand preparedCommand, Parameter parameter) throws PersistenceLayerException {
    // take preparedQuery from PreparedCommand and convert Parameter
    PreparedClusterQuery<?> query = ((PreparedClusterCommand) preparedCommand).getPreparedClusterQuery();
    // fire preparedQuery
    List<?> result = query(query, parameter, -1);
    // call delete for all returned objects
    for (Object object : result) {
      try {
        pl.getXynaScalableObjectRepository().deletePayload(returnAsXSORPayload(object), transactionContext,
                                                           strictlyCoherent);
      } catch (GeneralXSORException e) {
        throw new XSOR_Process_Exception("deletePayload", e);
      } catch (Throwable t) {
        throw new XSOR_Severe_Process_Exception("deletePayload", t);
      }
    }
    return result.size();
  }


  public <T extends Storable> Collection<T> loadCollection(Class<T> storableClazz) throws PersistenceLayerException {
    Persistable persi = Storable.getPersistable(storableClazz);
    // TODO performance, own operation to circumvent full table scan invocation?
    List<SearchCriterion> singleEmptyCriterion = new ArrayList<SearchCriterion>();
    singleEmptyCriterion.add(new SearchCriterion(new ArrayList<ColumnCriterion>()) {

      @Override
      public boolean fits(Object candidate, SearchParameter parameter) {
        return true;
      }
    });
    SearchRequest request = new SearchRequest(persi.tableName(), singleEmptyCriterion);
    List<XSORPayload> searchResult = null;
    try {
      searchResult = pl.getXynaScalableObjectRepository().search(request, new SearchParameter(), transactionContext,
                                                                 -1, false, strictlyCoherent);
    } catch (GeneralXSORException e) {
      throw new XSOR_Process_Exception("search", e);
    } catch (Throwable t) {
      throw new XSOR_Severe_Process_Exception("search", t);
    }

    Collection<T> result = new ArrayList<T>();
    if (searchResult.size() == 0) {
      return Collections.emptyList();
    } else {
      ResultSetReader<T> reader = storableClazz.cast(searchResult.get(0)).getReader();
      XynaClusterResultSet xcrs = new XynaClusterResultSet(getAllColumnNames(storableClazz));
      for (XSORPayload payload : searchResult) {
        xcrs.setBackingStorable((Storable) payload);
        try {
          result.add(reader.read(xcrs));
        } catch (SQLException e) {
          if (logger.isDebugEnabled()) {
            logger.warn("Error reading object, continuing", e);
          }
        }
      }
    }
    return result;
  }


  public <T extends Storable> void persistCollection(Collection<T> storables) throws PersistenceLayerException {
    for (T storable : storables) {
      persistObject(storable);
    }
  }


  public <T extends Storable> boolean persistObject(final T storable) throws PersistenceLayerException {
    XSORPayload payload = returnAsXSORPayload(storable);
    sqlLogger.buildAndLogPersist(payload, transactionContext);
    sqlLogger.buildAndLogAllColumns(storable);
    try {
      return pl.getXynaScalableObjectRepository().persistPayload(payload, transactionContext, strictlyCoherent);
    } catch (GeneralXSORException e) {
      throw new XSOR_Process_Exception("persistPayload", e);
    } catch (Throwable t) {
      throw new XSOR_Severe_Process_Exception("persistPayload", t);
    }
  }


  public PreparedCommand prepareCommand(Command command) throws PersistenceLayerException {
    // TODO what do we wan't to support
    // MemLayer supports only deletes...let's just do the same for now
    sqlLogger.debug("PREPARE DML: " + command.getSqlString(), transactionContext);
    String tableName = command.getTable();
    return new PreparedClusterCommandCreator().prepareCommand(command, pl.getStorableClazzForTable(tableName), pl.getRegisteredIndexDefinitions(tableName));
  }


  public <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException {
    sqlLogger.debug("PREPARE: " + query.getSqlString(), transactionContext);
    PreparedClusterQueryCreator<E> queryCreator = new PreparedClusterQueryCreator<E>();
    String tableName = query.getTable();
    PreparedClusterQuery<E> preparedQuery = queryCreator.prepareQuery(query, pl.getStorableClazzForTable(tableName), pl.getRegisteredIndexDefinitions(tableName));
    return preparedQuery;
  }


  public <E> List<E> query(PreparedQuery<E> pq, Parameter paras, int maxResults) throws PersistenceLayerException {
    if (!(pq instanceof PreparedClusterQuery)) {
      throw new XNWH_GeneralPersistenceLayerException("invalid prepared query: " + pq.getClass().getName());
    }
    return query(pq, paras, maxResults, pq.getReader());
  }


  public <E> List<E> query(PreparedQuery<E> pq, Parameter paras, int maxResults,
                           ResultSetReader<? extends E> resultSetReader) throws PersistenceLayerException {
    if (!(pq instanceof PreparedClusterQuery)) {
      throw new XNWH_GeneralPersistenceLayerException("invalid prepared query: " + pq.getClass().getName());
    }
    // TODO check if we did receive all necessary searchParams?
    PreparedClusterQuery<E> castedPQ = (PreparedClusterQuery<E>) pq;
    if (castedPQ.isCountQuery()) {
      maxResults = -1; // FIXME this is dangerous as we'll receive all the values which could be quite a lot
    }

    SearchParameter parameter = convertPersistenceParameterToSearchParameter(paras, castedPQ.getPrepreparedParameter(),
                                                                             castedPQ.getSearchRequest());
    sqlLogger.debug(castedPQ.getSqlString(), paras, transactionContext);
    sqlLogger.logPrepreparedParameter(castedPQ.getPrepreparedParameter());
    List<XSORPayload> searchResult = null;
    try {
      searchResult = pl.getXynaScalableObjectRepository().search(castedPQ.getSearchRequest(), parameter,
                                                                 transactionContext, maxResults,
                                                                 castedPQ.isForUpdate(), strictlyCoherent);
    } catch (GeneralXSORException e) {
      throw new XSOR_Process_Exception("search", e);
    } catch (Throwable t) {
      throw new XSOR_Severe_Process_Exception("search", t);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("query returned: " + searchResult.size());
    }

    List<E> result = new ArrayList<E>();
    XynaClusterResultSet xcrs = new XynaClusterResultSet(castedPQ.getSelection());
    if (castedPQ.isCountQuery()) {
      xcrs.setBackingStorable(new CountStorable(searchResult.size()));
      try {
        result.add(pq.getReader().read(xcrs));
      } catch (SQLException e) {
        if (logger.isDebugEnabled()) {
          logger.warn("Error reading object, continuing", e);
        }
      }
    } else {
      for (XSORPayload payload : searchResult) {
        xcrs.setBackingStorable((Storable) payload);
        try {
          result.add(pq.getReader().read(xcrs));
        } catch (SQLException e) {
          if (logger.isDebugEnabled()) {
            logger.warn("Error reading object, continuing", e);
          }
        }
      }
    }

    return result;
  }


  public <T extends Storable> void queryOneRow(T storable) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    queryOneRow(storable, false);
  }


  public <T extends Storable> void queryOneRowForUpdate(T storable) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    queryOneRow(storable, true);
  }


  private <T extends Storable> void queryOneRow(T storable, boolean forUpdate) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    SearchRequest pkRequest = pl.getRegisteredPrimaryKeyRequestForTable(storable.getTableName());
    SearchParameter searchParameter = new SearchParameter(
                                                          new SearchValue(returnAsXSORPayload(storable).getPrimaryKey()));
    StringBuilder messageBuilder = new StringBuilder().append("SELECT * FROM ").append(storable.getTableName())
                    .append(" WHERE {PrimaryKey}=").append(storable.getPrimaryKey().toString());
    if (forUpdate) {
      messageBuilder.append(" FOR UPDATE");
    }
    sqlLogger.debug(messageBuilder.toString(), transactionContext);
    List<XSORPayload> searchResult = null;
    try {
      searchResult = pl.getXynaScalableObjectRepository().search(pkRequest, searchParameter, transactionContext, 1,
                                                                 forUpdate, strictlyCoherent);
    } catch (GeneralXSORException e) {
      throw new XSOR_Process_Exception("search", e);
    } catch (Throwable t) {
      throw new XSOR_Severe_Process_Exception("search", t);
    }

    if (searchResult != null && searchResult.size() > 0) {
      storable.setAllFieldsFromData(storable.getClass().cast(searchResult.get(0)));
    } else {
      throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(storable.getPrimaryKey().toString(), storable.getTableName());
    }
  }


  public <E> E queryOneRow(PreparedQuery<E> pq, Parameter paras) throws PersistenceLayerException {
    List<E> result = query(pq, paras, 1);
    if (result.size() == 0) {
      return null;
    }
    return result.get(0);
  }


  public void rollback() throws PersistenceLayerException {
    sqlLogger.debug("ROLLBACK", transactionContext);
    try {
      pl.getXynaScalableObjectRepository().endTransaction(transactionContext, strictlyCoherent);
    } catch (GeneralXSORException e) {
      throw new XSOR_Process_Exception("endTransaction", e);
    } catch (XSORThrowableCarrier carrier) {
      XSOR_Process_Exception multipleCause = new XSOR_Process_Exception("endTransaction");
      multipleCause.initCauses(carrier.getCarried().toArray(new Throwable[carrier.getCarried().size()]));
      throw multipleCause;
    } catch (Throwable t) {
      throw new XSOR_Severe_Process_Exception("endTransaction", t);
    }
  }


  public void setTransactionProperty(TransactionProperty arg0) {
    if (arg0.getPropertyType() == TransactionPropertyType.noSynchronousActiveClusterSynchronizationNeeded) {
      strictlyCoherent = false;
    }
  }


  public <T extends Storable> void addTable(Class<T> storableClass, boolean forceWidening, Properties props)
                  throws PersistenceLayerException {
    if (!XSORPayload.class.isAssignableFrom(storableClass)) {
      throw new XNWH_GeneralPersistenceLayerException("storable must implement " + XSORPayload.class.getName() + ".");
    }

    List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions = 
               new ArrayList<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>>();

    // indices from column annotations
    Column[] cols = Storable.getColumns(storableClass);
    for (Column column : cols) {
      IndexDefinition<T, ? extends IndexKey, ? extends IndexSearchCriterion> index = pl.getIndexDefinitionFactory()
                      .createIndex(storableClass, column);
      if (index != null) {
        indexDefinitions.add(index);
      }
    }

    // composite indices
    CompositeIndex[] indices = Storable.getCompositeIndices(storableClass);
    for (CompositeIndex compositeIndex : indices) {
      IndexDefinition<T, ? extends IndexKey, ? extends IndexSearchCriterion> index = pl.getIndexDefinitionFactory()
                      .createIndex(storableClass, compositeIndex);
      if (index != null) {
        indexDefinitions.add(index);
      }
    }

    Persistable persi = Storable.getPersistable(storableClass);
    String tableName = persi.tableName();
    if (logger.isDebugEnabled()) {
      logger.debug("Sending " + indexDefinitions.size() + " to controller.");
    }
    
    pl.registerIndexDefinitions(tableName, indexDefinitions);
    pl.registerStorable(tableName, storableClass);

    int maxTableSize = 1000000;
    if (props != null) {
      String val = props.getProperty(XynaClusterPersistenceLayer.PROP_MAXTABLESIZE);
      if (val != null) {
        try {
          maxTableSize = Integer.valueOf(val.trim());
        } catch (NumberFormatException e) {
          throw new XNWH_GeneralPersistenceLayerException("property " + XynaClusterPersistenceLayer.PROP_MAXTABLESIZE
                          + " was set to invalid value "
                          + props.getProperty(XynaClusterPersistenceLayer.PROP_MAXTABLESIZE), e);
        }
      }
    }

    ColumnCriterion pkCol = new ColumnCriterion(persi.primaryKey(), SearchColumnOperator.EQUALS, 0, true);
    StorableBasedSearchCriterion criterion = new StorableBasedSearchCriterion(Arrays.asList(pkCol));
    pl.registerPrimaryKeyRequest(tableName, new SearchRequest(tableName, Arrays.asList(criterion)));

    try {
      pl.getXynaScalableObjectRepository().initializeTable(tableName, (Class<? extends XSORPayload>) storableClass, maxTableSize);
    } catch (PersistenceException e) {
      throw new XNWH_GeneralPersistenceLayerException(
                                                      "Could not load objects of type " + storableClass.getName() + ".",
                                                      e);
    }
    pl.registerTableStatistics(tableName);
  }
  
  
  public <T extends Storable> void removeTable(Class<T> klass, Properties props) throws PersistenceLayerException {
    if (XSORPayload.class.isAssignableFrom(klass)) {
      Persistable persi = Storable.getPersistable(klass);
      String tableName = persi.tableName();
      pl.unregisterTable(tableName);
      try {
        pl.getXynaScalableObjectRepository().removeTable(tableName, (Class<? extends XSORPayload>) klass);
      } catch (PersistenceException e) {
        throw new XNWH_GeneralPersistenceLayerException("Failed to unregister table " + tableName + ".", e);
      }
    } else {
      logger.debug("removeTable invocation for unfitting object detected: " + klass.getName());
    }
  }

  
  private SearchParameter convertPersistenceParameterToSearchParameter(Parameter parameter,
                                                                       Map<Integer, SearchValue> prepreparedParams,
                                                                       SearchRequest searchRequest) {
    SearchParameter searchParameter = new SearchParameter();
    if (parameter != null) {
      Map<Integer, ColumnCriterion> inParameter = findInParameter(searchRequest);
      int parameterIndex = 0;
      for (int index = 0; index < parameter.size() + prepreparedParams.size(); index++) {
        if (prepreparedParams.containsKey(index)) {
          searchParameter.addParameter(prepreparedParams.get(index));
        } else {
          Object value = parameter.get(parameterIndex);
          if (inParameter.containsKey(index)) {
            if (value instanceof String && ((String) value).startsWith("(") && ((String) value).endsWith(")")) {
              String unwrapped = ((String) value).substring(1, ((String) value).length() - 1);
              Object splittedAndTypedParams = TypedValuesHelper.generateTypedArrayFromStringArray(inParameter
                              .get(index).getColumnName(), unwrapped.split(","), pl
                              .getStorableClazzForTable(searchRequest.getTablename()));
              searchParameter.addParameter(splittedAndTypedParams);
            } else {
              searchParameter.addParameter(value);
            }
          } else {
            searchParameter.addParameter(value);
          }
          parameterIndex++;
        }
      }
    }
    return searchParameter;
  }


  private Map<Integer, ColumnCriterion> findInParameter(SearchRequest searchRequest) {
    Map<Integer, ColumnCriterion> inParameter = new HashMap<Integer, ColumnCriterion>();
    for (SearchCriterion criterion : searchRequest.getSearchCriterion()) {
      for (ColumnCriterion column : criterion.getColumns()) {
        if (column.getOperator() == SearchColumnOperator.IN) {
          inParameter.put(column.getMappingToSearchParameter(), column);
        }
      }
    }
    return inParameter;
  }


  private XSORPayload returnAsXSORPayload(Object obj) throws PersistenceLayerException {
    if (obj instanceof XSORPayload) {
      return (XSORPayload) obj;
    } else {
      throw new UnsupportedOperationException(XynaClusterPersistenceLayer.class.getSimpleName()
                      + " can only operate on " + XSORPayload.class.getSimpleName());
    }
  }


  private String[] getAllColumnNames(Class<? extends Storable> clazz) {
    Column[] allColumns = Storable.getColumns(clazz);
    String[] allColumnNames = new String[allColumns.length];
    for (int i = 0; i < allColumns.length; i++) {
      allColumnNames[i] = allColumns[i].name();
    }
    return allColumnNames;
  }


  public boolean isOpen() {
    return pl.getXynaScalableObjectRepository() != null;
  }


}
