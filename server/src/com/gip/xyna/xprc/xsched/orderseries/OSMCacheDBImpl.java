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
package com.gip.xyna.xprc.xsched.orderseries;


import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_TooManyDedicatedConnections;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.DedicatedConnection;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.Rollback;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.WarehouseRetryExecutorBuilder;
import com.gip.xyna.xprc.exceptions.XPRC_DUPLICATE_CORRELATIONID;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;


/**
 *
 */
public class OSMCacheDBImpl implements OSMCache {

  private PreparedQuery<SeriesInformationStorable> loadByCorrelationIdQuery;
  private PreparedQuery<SeriesInformationStorable> loadByOrderIdQuery;
  //private ODS ods;
  private int ownBinding;
  private static Logger logger = CentralFactoryLogging.getLogger(OSMCacheDBImpl.class);
  
  private static final String loadByCorrelationIdQueryString =
      "select * from "+SeriesInformationStorable.TABLE_NAME+" where "+SeriesInformationStorable.COL_CORRELATION_ID+" = ?";
  private static final String loadByOrderIdQueryString =
      "select * from "+SeriesInformationStorable.TABLE_NAME+" where "+SeriesInformationStorable.COL_ID+" = ?";

  public OSMCacheDBImpl() throws PersistenceLayerException {
    init();
  }
  
  public OSMCacheDBImpl(ODS ods, int ownBinding) throws PersistenceLayerException {
    //this.ods = ods;
    this.ownBinding = ownBinding;
    init();
  }
 
  /**
   * @param ownBinding the ownBinding to set
   */
  public void setOwnBinding(int ownBinding) {
    this.ownBinding = ownBinding;
  }
  
  protected void init() throws PersistenceLayerException {
    
    try {
      CentralComponentConnectionCache.getInstance().openCachedConnection(ODSConnectionType.DEFAULT, DedicatedConnection.OSMCache,
                                                                         new StorableClassList(SeriesInformationStorable.class));
    } catch (XNWH_TooManyDedicatedConnections e) {
      throw new RuntimeException("Connection limit exceeded while trying to open dedicated connection for DatabaseLock.", e);
    }
    ODSConnection con = CentralComponentConnectionCache.getConnectionFor(DedicatedConnection.OSMCache);
    
    loadByCorrelationIdQuery = con.prepareQuery(new Query<SeriesInformationStorable>(
                                    loadByCorrelationIdQueryString, 
                                    SeriesInformationStorable.reader),
                                    true);
    loadByOrderIdQuery = con.prepareQuery(new Query<SeriesInformationStorable>(
                    loadByOrderIdQueryString, 
                    SeriesInformationStorable.reader),
                    true);
    con.commit();
  }
  
  
  public synchronized SeriesInformationStorable get(String correlationId) {
    try {
      return warehouseRetryExecutor_SeriesInformationStorable().
          execute(new QueryExecutable(loadByCorrelationIdQuery, correlationId));
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to read SeriesInformationStorable "+correlationId, e );
      return null;
    }
  }

  public synchronized SeriesInformationStorable refresh(String correlationId) {
    return get(correlationId); //hier keint Unterschied zu get: frisch aus DB lesen
  }

  
  public synchronized SearchResult search(long orderId) {
    try {
      SeriesInformationStorable sis = 
          warehouseRetryExecutor_SeriesInformationStorable().execute( new QueryExecutable(loadByOrderIdQuery, orderId) );
      if( sis == null) {
        return SearchResult.notFound();
      } else {
        return SearchResult.found(sis.getId(), sis.getBinding(), sis.getCorrelationId(), ownBinding);
      }
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to read SeriesInformationStorable "+orderId, e );
      return SearchResult.notFound();
    }
  }

  public synchronized void update(SeriesInformationStorable sis) {
    try {
      warehouseRetryExecutor_SeriesInformationStorable().execute( new PersistExecutable(sis) );
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to persist SeriesInformationStorable "+sis.getCorrelationId(), e );
    }
  }

  public synchronized void insert(SeriesInformationStorable sis) throws XPRC_DUPLICATE_CORRELATIONID, XNWH_GeneralPersistenceLayerException {
    try {
      warehouseRetryExecutor_SeriesInformationStorable().execute( new PersistExecutable(sis) );
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to persist SeriesInformationStorable "+sis.getCorrelationId(), e );
      //Evtl. ist dies eine Unique-Contraint-Violation, dies nun testen

      SeriesInformationStorable existingSis = get( sis.getCorrelationId() );
      if( existingSis != null ) {
        if( sis.getId() != existingSis.getId() ) {
          throw new XPRC_DUPLICATE_CORRELATIONID(sis.getCorrelationId());
        }
      }
      //andere Fehlerursache ebenfalls weiterreichen
      if (new SeriesInformationStorable().getClusterState(ODSConnectionType.DEFAULT) == ClusterState.DISCONNECTED_SLAVE) {
        throw new OrderDeathException(e);
      } else {
        throw new XNWH_GeneralPersistenceLayerException("Was unable to insert seriesInformationStorable", e);
      }
    }
  }

  public void lock(String correlationId) {
    throw new UnsupportedOperationException("locking not implemented");
  }

  public void unlock(String correlationId) {
    throw new UnsupportedOperationException("locking not implemented");
  }

  public boolean tryLock(String correlationId) {
    throw new UnsupportedOperationException("locking not implemented");
  }

  @Override
  public String toString() {
    return "OSMCacheDBImpl"; //FIXME
  }

  public void remove(String correlationId) {
    throw new UnsupportedOperationException("remove(correlationId) not implemented");
  }
  
  /**
   * @param id
   * @throws XNWH_GeneralPersistenceLayerException 
   */
  public synchronized void remove(long id) throws XNWH_GeneralPersistenceLayerException {
    if( ! XynaProperty.ORDER_SERIES_CLEAN_DATABASE.get() ) {
      return; //SeriesInformationStorable sollen nicht aus DB entfernt werden
    }
    try {
      warehouseRetryExecutor_SeriesInformationStorable().execute( new DeleteExecutable(id) );
    } catch (PersistenceLayerException e) {
      if (new SeriesInformationStorable().getClusterState(ODSConnectionType.DEFAULT) == ClusterState.DISCONNECTED_SLAVE) {
        throw new OrderDeathException(e);
      } else {
        throw new XNWH_GeneralPersistenceLayerException("Was unable to delete seriesInformationStorable", e);
      }
    }
  }

  
  private WarehouseRetryExecutorBuilder warehouseRetryExecutor_SeriesInformationStorable() {
    return WarehouseRetryExecutor.buildCriticalExecutor().
        connectionDedicated(DedicatedConnection.OSMCache).
        rollback(Rollback.OnError).
        storable(SeriesInformationStorable.class);
  }


  private static class QueryExecutable implements WarehouseRetryExecutableNoException<SeriesInformationStorable> {

    private PreparedQuery<SeriesInformationStorable> query;
    private Parameter parameter;
    
    public QueryExecutable(PreparedQuery<SeriesInformationStorable> loadByCorrelationIdQuery, String correlationId) {
      this.query = loadByCorrelationIdQuery;
      parameter = new Parameter(correlationId);
    }
    public QueryExecutable(PreparedQuery<SeriesInformationStorable> loadByOrderIdQuery, Long orderId) {
      this.query = loadByOrderIdQuery;
      parameter = new Parameter(orderId);
    }

    public SeriesInformationStorable executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      SeriesInformationStorable sis = null;
      sis = con.queryOneRow(query, parameter );
      con.commit();
      return sis;
    }
        
  }
    
  private static class PersistExecutable implements WarehouseRetryExecutableNoResult {
    private SeriesInformationStorable sis;

    public PersistExecutable(SeriesInformationStorable sis) {
      this.sis = sis;
    }

    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      con.persistObject(sis);
      con.commit();
    }
    
  }
  
  private static class DeleteExecutable implements WarehouseRetryExecutableNoResult {
    private SeriesInformationStorable sis;
    
    public DeleteExecutable(long orderId) {
      sis = new SeriesInformationStorable();
      sis.setId(orderId);
    }
    
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      con.deleteOneRow(sis);
      con.commit();
    }
    
  }

}
