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

package com.gip.xyna.update;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionCouldNotBeClosedException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer.TransactionMode;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;
import com.gip.xyna.xnwh.pools.PoolDefinition;



public class UpdatePoolDefinitionPasswords extends Update{


  private Version allowedForUpdate;
  private Version afterUpdate;


  public UpdatePoolDefinitionPasswords(Version allowedForUpdate, Version afterUpdate) {
    this.allowedForUpdate = allowedForUpdate;
    this.afterUpdate = afterUpdate;
  }

  @SuppressWarnings("unchecked")
  protected void update() throws XynaException {
    Logger logger = CentralFactoryLogging.getLogger(Update.class);
    PersistenceLayerConnection con = getXMLConnection();
    try{
      Collection<PoolDefinition> oldStorables = con.loadCollection(PoolDefinition.class);
      List<PoolDefinition> newStorables = CollectionUtils.transformAndSkipNull(oldStorables, new TransformAdditionalParams());
      con.deleteAll(PoolDefinition.class);
      con.persistCollection(newStorables);
      con.commit();
    } finally {      
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
    ConnectionPool[] pools = ConnectionPool.getAllRegisteredConnectionPools();
    for (ConnectionPool connectionPool : pools) {
      try {
        ConnectionPool.removePool(connectionPool, true, 20000);
      } catch (ConnectionCouldNotBeClosedException e) {
        throw new RuntimeException(e);
      }
    }
    ODSImpl ods = ODSImpl.getInstance();
    Map<String, Map<Long, Class<? extends Storable<?>>>> storables;
    try {
      Field field = ODSImpl.class.getDeclaredField("storables");
      field.setAccessible(true);
      storables = (Map<String, Map<Long, Class<? extends Storable<?>>>>) field.get(ods);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    ODSImpl.getInstance().shutdown();
    ODSImpl.clearInstances();
    logger.debug("Cleared instance");
    logger.debug(storables);
    ods = ODSImpl.getInstance(true);
    ConnectionPoolManagement poolMgmt = ConnectionPoolManagement.getInstance();
    try {
      Method reinit = ConnectionPoolManagement.class.getDeclaredMethod("reinit");
      reinit.setAccessible(true);
      reinit.invoke(poolMgmt);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    try {
      Method reinit = ODSImpl.class.getDeclaredMethod("reinit");
      reinit.setAccessible(true);
      reinit.invoke(ods);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (storables != null && storables.size() > 0) {
      for (Map<Long, Class<? extends Storable<?>>> subMap : storables.values()) {
        for (Class<? extends Storable<?>> clazz : subMap.values()) {
          logger.debug("reregistering: " + clazz);
          ods.registerStorable(clazz);
        }
      }
    }
    
    Updater.getInstance().refreshPLCache();
  }
  
  private PersistenceLayerConnection getXMLConnection() throws PersistenceLayerException {
    XMLPersistenceLayer xmlpers = new XMLPersistenceLayer();
    xmlpers.init(null, "default" + ODSConnectionType.HISTORY.toString(), TransactionMode.FULL_TRANSACTION.name(), "false");
    return xmlpers.getConnection();
  }
  
  protected Version getAllowedVersionForUpdate() {
    return this.allowedForUpdate;
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return this.afterUpdate;
  }


  @Override
  public boolean mustUpdateGeneratedClasses() {
    return false;
  }
  
  private static class TransformAdditionalParams implements Transformation<PoolDefinition, PoolDefinition> {

    public PoolDefinition transform(PoolDefinition from) {
      PoolDefinition transformed = new PoolDefinition();
      transformed.setConnectstring(from.getConnectstring());
      transformed.setName(from.getName());
      transformed.setRetries(from.getRetries());
      transformed.setSize(from.getSize());
      transformed.setType(from.getType());
      transformed.setUser(from.getUser());
      transformed.setValidationinterval(from.getValidationinterval());
      transformed.setParams(from.getParams());
      transformed.setUuid(from.getUuid());
      
      //from.getPassword is clear
      //calling transformed.setPassword encodes it
      transformed.setPassword(from.getPassword());
      
      return transformed;
    }
    
    
  }

}
