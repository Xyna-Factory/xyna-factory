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

package com.gip.xyna.xprc.xprcods.ordercontextconfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;



public class OrderContextConfiguration extends FunctionGroup {

  public static final String DEFAULT_NAME = "OrderContextConfiguration";

  static {
    addDependencies(OrderContextConfiguration.class, new ArrayList<XynaFactoryPath>(Arrays
                    .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaFactoryManagement.class,
                                                                       XynaFactoryManagementODS.class,
                                                                       Configuration.class)})));
  }
  
  private final static String QUERY_BY_ORDERTYPE = "select * from " + OrderContextConfigStorable.TABLE_NAME + " where "
                                                                    + OrderContextConfigStorable.COLUMN_ORDER_TYPE + " = ?";
  private static PreparedQuery<OrderContextConfigStorable> queryByOrdertype;

  private ODS ods;
  private final ConcurrentLinkedQueue<DestinationKey> cache = new ConcurrentLinkedQueue<DestinationKey>();
  private final ReentrantReadWriteLock cacheReadWriteLock = new ReentrantReadWriteLock();
  private final Lock readLock = cacheReadWriteLock.readLock();
  private final Lock writeLock = cacheReadWriteLock.writeLock();

  private volatile boolean isInitialized = false;

  public OrderContextConfiguration() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    
    
    ods = ((XynaProcessingODS) getParentSection()).getODS();
    ods.registerStorable(OrderContextConfigStorable.class);
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(OrderContextConfiguration.class,"OrderContextConfiguration.loadOrderContextConfigs").
      after(RevisionManagement.class).
      execAsync(new Runnable() { public void run() { loadOrderContextConfigs(); }});
    
    XynaProperty.XYNA_GLOBAL_ORDER_CONTEXT_SETTINGS.registerDependency(DEFAULT_NAME);
  }


  @Override
  protected void shutdown() throws XynaException {
    //nothing else to be done, changes are saved to disk immediately
  }

  private void loadOrderContextConfigs(){
    RevisionManagement revisionManagment = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      writeLock.lock();
      try {
        Collection<OrderContextConfigStorable> configStorables = con.loadCollection(OrderContextConfigStorable.class);
        for (OrderContextConfigStorable storable : configStorables) {
          RuntimeContext runtimeContext;
          try {
            runtimeContext = revisionManagment.getRuntimeContext(storable.getRevision());
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            logger.warn("Can't get runtimeContext for revision " + storable.getRevision(), e);
            continue;
          }
          cache.add(new DestinationKey(storable.getOrderType(), runtimeContext));
        }
        try {
          queryByOrdertype = con.prepareQuery(new Query<OrderContextConfigStorable>(QUERY_BY_ORDERTYPE, OrderContextConfigStorable.reader));
        } catch (XNWH_UnsupportedPersistenceLayerFeatureException e) {
          queryByOrdertype = null;
        }
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("Failed to initialize " + DEFAULT_NAME, e);
      } finally {
        writeLock.unlock();
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Could not close connection!",e);
      }
    }
    
    isInitialized = true;
  }


  public boolean isDestinationKeyConfiguredForOrderContextMapping(DestinationKey dk, boolean ignoreGlobalProperty) {

    if (!isInitialized) {
      throw new IllegalStateException(getDefaultName() + " has not been initialized.");
    }

    if (!ignoreGlobalProperty && XynaProperty.XYNA_GLOBAL_ORDER_CONTEXT_SETTINGS.get()) {
      return true;
    } else {
      readLock.lock();
      try {
        return cache.contains(dk);
      } finally {
        readLock.unlock();
      }
    }
  }


  /**
   * Configures the order context mapping for the given destination key.
   * @return true if something was changed and false otherwise
   */
  public boolean configureDestinationKey(DestinationKey dk, boolean createOrderContextMapping)
      throws PersistenceLayerException {

    if (!isInitialized) {
      throw new IllegalStateException(getDefaultName() + " has not been initialized.");
    }

    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision;
    try {
      revision = revisionManagement.getRevision(dk.getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(dk.getRuntimeContext() + " unknown", e);
    }
    
    boolean somethingChanged;
    writeLock.lock();
    try {

      if (createOrderContextMapping && cache.contains(dk)) {
        somethingChanged = false;
      } else if (!createOrderContextMapping && !cache.contains(dk)) {
        somethingChanged = false;
      } else {
        somethingChanged = true;
        if (createOrderContextMapping) {
          cache.add(dk);
        } else {
          cache.remove(dk);
        }
      }

    } finally {
      writeLock.unlock();
    }

    if (somethingChanged) {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        OrderContextConfigStorable storable = new OrderContextConfigStorable(dk.getOrderType(), revision);
        if (createOrderContextMapping) {
          con.persistObject(storable);
        } else {
          deleteOneRow(dk, revision, con);
        }
        con.commit();
      } finally {
        con.closeConnection();
      }
    }

    return somethingChanged;
  }

  
  private void deleteOneRow(DestinationKey key, Long revision, ODSConnection con) throws PersistenceLayerException {
    Collection<OrderContextConfigStorable> possibleStorables;
    if (queryByOrdertype == null) {
      possibleStorables = con.loadCollection(OrderContextConfigStorable.class);
    } else {
      possibleStorables = con.query(queryByOrdertype, new Parameter(key.getOrderType()), -1);
    }
    for (OrderContextConfigStorable occs : possibleStorables) {
      if (occs.getOrderType().equals(key.getOrderType()) &&
          ((occs.getRevision() == null && revision == null) ||
           (occs.getRevision() != null && occs.getRevision().equals(revision)))) {
        con.deleteOneRow(occs);
        con.commit();
        return;
      }
    }
  }

  public Collection<DestinationKey> getAllDestinationKeysForWhichAnOrderContextMappingIsCreated() {
    readLock.lock();
    try {
      return Collections.unmodifiableCollection(cache);
    } finally {
      readLock.unlock();
    }
  }

}
