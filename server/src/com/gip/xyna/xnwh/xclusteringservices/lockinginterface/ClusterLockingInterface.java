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

package com.gip.xyna.xnwh.xclusteringservices.lockinginterface;



import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xods.configuration.ClusteredConfiguration;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



/**
 * ClusterLockingInterface erzeugt mit {@link createLockIfNonexistent} eine DatabaseLock-Instanz.
 * 
 * Diese DatabaseLock erfüllt das {@link java.util.concurrent.locks.Lock}-Interface, jedoch kann 
 * im Konstruktor, bei setUseConnection() und bei lock() eine RuntimeException LockFailedException
 * geworfen werden, unlock() loggt dagegen Fehler nur.
 * In den Methoden setUseConnection() und lock() wird in Fehlerfällen blockiert und Retries probiert,
 * bis entweder die Operation geklappt hat oder der ClusterState nach DISCONNECTED_SLAVE wechselt. 
 * In letzterem Fall wird dann die LockFailedException geworfen.
 *
 */
public class ClusterLockingInterface extends FunctionGroup {


  public static final String DEFAULT_NAME = "Cluster Locking Interface";

  private final ConcurrentMap<String, DatabaseLock> locks = new ConcurrentHashMap<String, DatabaseLock>();
  
  private ODS ods;
  private ClusterContext clusterContext;


  public static enum DatabaseLockType {
    InternalConnection {
      @Override
      public DatabaseLock createInstance(ODS ods, ClusterContext clusterContext, String name)
          throws PersistenceLayerException, LockFailedException {
        return new DatabaseLockInternalConnection(ods, clusterContext, name);
      }
    },
    ExternalConnection {
      @Override
      public DatabaseLock createInstance(ODS ods, ClusterContext clusterContext, String name)
          throws PersistenceLayerException, LockFailedException {
        return new DatabaseLockExternalConnection(ods, clusterContext, name);
      }
    };


    protected abstract DatabaseLock createInstance(ODS ods, ClusterContext clusterContext, String name)
        throws PersistenceLayerException, LockFailedException;
  }


  public ClusterLockingInterface() throws XynaException {
    super();
  }
  

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask(ClusterLockingInterface.class, ClusterLockingInterface.class.getSimpleName())
         .before(XynaClusteringServicesManagement.class)
         .execAsync(this::initCluster);
  }
  
  
  private void initCluster() {
    try {
      ods = ODSImpl.getInstance();
      ods.registerStorable(ClusteringServicesLockStorable.class);
  
      clusterContext = new ClusterContext(ClusteringServicesLockStorable.class, ODSConnectionType.DEFAULT);
  
      clusterContext.addClusterStateChangeHandler(new ClusterlockChangeHandler());
      ods.addClusteredStorableConfigChangeHandler(clusterContext, ODSConnectionType.DEFAULT,
                                                  ClusteringServicesLockStorable.class);
    } catch (XynaException e) {
      throw new RuntimeException("Failed to register " + ClusteringServicesLockStorable.class.getSimpleName() + " as clusterable component.", e);
    }
    
    if (logger.isDebugEnabled()) {
      logger.debug("ClusterLockingInterface.clusterContext: " + clusterContext);
    }
  }


  public static class LockFailedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public LockFailedException(Throwable t) {
      super(t);
    }
    public LockFailedException(String message, Throwable cause) {
      super(message, cause);
    }
    public LockFailedException(String message) {
      super(message);
    }
  }


  public static class AlreadyUnlockedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public AlreadyUnlockedException(Throwable t) {
      super(t);
    }
    public AlreadyUnlockedException(String message, Throwable cause) {
      super(message, cause);
    }
    public AlreadyUnlockedException(String message) {
      super(message);
    }
  }


  private class ClusterlockChangeHandler implements ClusterStateChangeHandler {

    public boolean isReadyForChange(ClusterState newState) {
      return true; //immer bereit
    }


    public void onChange(ClusterState newState) {
      if (logger.isDebugEnabled()) {
        logger.debug("ClusterLockingInterface.onChange(" + newState + ")");
      }
      if (newState == ClusterState.NO_CLUSTER) {
        for (DatabaseLock dbl : locks.values()) {
          dbl.setUseConnection(false);
        }
      } else {
        for (DatabaseLock dbl : locks.values()) {
          dbl.setUseConnection(true);
        }
      }
    }
  }


  @Override
  protected void shutdown() throws XynaException {
  }


  /**
   * Erzeugt eine DatabaseLock-Instanz
   * @throws LockFailedException
   */
  public DatabaseLock createLockIfNonexistent(String name, DatabaseLockType databaseLockType)
      throws LockFailedException {

    if (logger.isDebugEnabled()) {
      logger.debug("ClusterLockingInterface.createLockIfNonexistent(" + name + ")");
    }
    DatabaseLock newLock;
    try {
      newLock = databaseLockType.createInstance(ods, clusterContext, name);
    } catch (PersistenceLayerException e) {
      throw new LockFailedException("Failed to create lock: " + e.getMessage(), e);
    }
    DatabaseLock previousLock = locks.putIfAbsent(name, newLock);
    if (previousLock != null) {
      // Das alte lock muss in dem Fall nicht gelöscht werden, weil die Objekte nur einen
      // String enthalten und das persistObject im Konstruktor dann zu einem update wird.
      return previousLock;
    } else {
      return newLock;
    }

  }


  public DatabaseLock getLock(String name) {
    return locks.get(name);
  }

}
