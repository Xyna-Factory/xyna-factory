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
package com.gip.xyna.xnwh.xclusteringservices.lockinginterface;



import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xnwh.exceptions.XNWH_TooManyDedicatedConnections;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.CentralComponentConnectionCacheException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface.LockFailedException;



public class DatabaseLockInternalConnection extends AbstractDatabaseLock {

  private final String connectionName;


  public DatabaseLockInternalConnection(ODS ods, ClusterContext clusterContext, String name)
      throws PersistenceLayerException, LockFailedException {
    super(ods, clusterContext, name);
    this.connectionName = name + "-lock";
    try {
      CentralComponentConnectionCache.getInstance().openCachedConnection(ODSConnectionType.DEFAULT, connectionName,
                                                                         new StorableClassList(ClusteringServicesLockStorable.class));
    } catch (XNWH_TooManyDedicatedConnections e) {
      throw new RuntimeException("Connection limit exceeded while trying to open dedicated connection for DatabaseLock.", e);
    }
  }


  @Override
  protected void closeConnectionForShutdown() {
    // nothing to be done, the connection is being cached
  }


  @Override
  protected void closeConnectionForUnlock() {
    // nothing to be done, the connection is being cached
  }

  @Override
  protected ODSConnection getConnectionForLock() {
    ODSConnection lockingCon = null;
    try {
      lockingCon = CentralComponentConnectionCache.getInstance().getCachedConnection(connectionName, 1);
    } catch (CentralComponentConnectionCacheException e) {
      logger.warn("Failed to open connection", e);
      throw new LockFailedException(e);
    }
    return lockingCon;
  }


  @Override
  protected ODSConnection getConnectionForRetry(PersistenceLayerException cause) throws LockFailedException {
    return getConnectionForLock();
  }


  @Override
  protected ODSConnection getConnectionForUnlock() {
    return getConnectionForLock();
  }


  public void lock(ODSConnection con) throws LockFailedException {
    throw new UnsupportedOperationException("lock with Connection can not be used to set internal connection");
  }


  public void lock() throws LockFailedException {
    localLock.lock();
    boolean lockSuccessful = false;
    try {
      lockInDB();
      lockSuccessful = true;
    } finally {
      if( ! lockSuccessful ) {
        localLock.unlock();
      }
    }
  }
  
}
