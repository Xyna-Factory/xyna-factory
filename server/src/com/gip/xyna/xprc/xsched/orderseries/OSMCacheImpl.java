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
package com.gip.xyna.xprc.xsched.orderseries;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.LruCache;
import com.gip.xyna.utils.concurrent.HashParallelReentrantLock;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_DUPLICATE_CORRELATIONID;


/**
 *
 */
public class OSMCacheImpl implements OSMCache {
  private static Logger logger = CentralFactoryLogging.getLogger(OSMCacheImpl.class);
  
  private HashParallelReentrantLock<String> locks;
  private LruCache<String,SeriesInformationStorable> cache;
  private Map<String,SeriesInformationStorable> data;
  private int ownBinding;
  private OSMCache dbBackend;
  
  public OSMCacheImpl(int ownBinding, int lockParallelity, int maxEntries, OSMCache dbBackend) {
    this.locks = new HashParallelReentrantLock<String>(lockParallelity);
    this.cache = new LruCache<String,SeriesInformationStorable>(maxEntries);
    this.data = Collections.synchronizedMap(cache);
    this.ownBinding = ownBinding;
    this.dbBackend = dbBackend;
  }
  
  public SeriesInformationStorable get(String correlationId) {
    SeriesInformationStorable sis = data.get(correlationId);
    if( sis == null ) {
      sis = refresh(correlationId);
    }
    return sis;
  }
  
  public SeriesInformationStorable refresh(String correlationId) {
    SeriesInformationStorable sis = dbBackend.get(correlationId);
    if( sis != null && sis.getBinding() == ownBinding ) {
      //in eigenen Cache aufnehmen
      data.put(correlationId, sis);
    }
    return sis;
  }

  public SearchResult search(long orderId) {
    return dbBackend.search(orderId);
  }

  public void update(SeriesInformationStorable sis) {
    data.put(sis.getCorrelationId(),sis);
    dbBackend.update(sis);
  }

  public void insert(SeriesInformationStorable sis) throws XNWH_GeneralPersistenceLayerException, XPRC_DUPLICATE_CORRELATIONID {
    dbBackend.insert(sis);
    data.put(sis.getCorrelationId(),sis);
  }

  public void lock(String correlationId) {
    locks.lock(correlationId);
  }

  public void unlock(String correlationId) {
    locks.unlock(correlationId);
  }

  public boolean tryLock(String correlationId) {
    return locks.tryLock(correlationId);
  }

  @Override
  public String toString() {
    return data.toString();
  }

  public void remove(String correlationId) {
    SeriesInformationStorable sis = data.remove(correlationId);
    if( sis.getBinding() == ownBinding ) {
      try {
        dbBackend.remove(sis.getId());
      }
      catch (XNWH_GeneralPersistenceLayerException e) {
        logger.warn( "Could not remove SeriesInformationStorable "+correlationId, e);
      }
    }
  }
  
  public void remove(long id) throws XNWH_GeneralPersistenceLayerException {
    throw new UnsupportedOperationException("remove(id) not implemented");
  }

  public int size() { //TODO evtl. ein fillOrderSeriesManagementInformation
    return data.size();
  }

  public void lockAll() {
    locks.lockAll();
  }

  public void unlockAll() {
    locks.unlockAll();
  }
  
  public Collection<SeriesInformationStorable> getCacheCopy() {
    return data.values();
  }

}
