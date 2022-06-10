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

package com.gip.xyna.xprc.xprcods.capacitymapping;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCapacityCardinality;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;


/**
 * Stores correlations between order types and capacities
 *
 */
public class CapacityMappingDatabase extends FunctionGroup {

  static {
    addDependencies(CapacityMappingDatabase.class,
                    new ArrayList<XynaFactoryPath>(Arrays
                        .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaFactoryManagement.class,
                                                                           XynaFactoryControl.class,
                                                                           DependencyRegister.class)})));
  }

  public static final String DEFAULT_NAME = "CapacityMappingDatabase";

  private HashMap<DestinationKey, CapacityMappingStorable> mapping;
  private Lock readLock;
  private Lock writeLock;
  private boolean initialized;

  private ODS ods;


  public CapacityMappingDatabase() throws XynaException {
    super();
  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  public void init() throws XynaException {

    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    readLock = lock.readLock();
    writeLock = lock.writeLock();

    ods = ODSImpl.getInstance();
    ods.registerStorable(CapacityMappingStorable.class);

    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(CapacityMappingDatabase.class,"CapacityMappingDatabase.loadCapacityMappings").
      after(RevisionManagement.class).
      execAsync(new Runnable() { public void run() { loadCapacityMappings(); }});

    XynaFactory
        .getInstance()
        .getFactoryManagementPortal()
        .getXynaFactoryControl()
        .getDependencyRegister()
        .addDependency(DependencySourceType.XYNAPROPERTY, XynaProperty.CAPACITIES_DIRECTPERSISTENCE,
                       DependencySourceType.XYNAFACTORY, DEFAULT_NAME);
  }

  @Override
  public void shutdown() throws XynaException {
    updateArchive(true);
  }


  private void loadCapacityMappings(){
    RevisionManagement revisionManagment = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    writeLock.lock();
    try {
      mapping = new HashMap<DestinationKey, CapacityMappingStorable>();
      ODSConnection hisConnection = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        Collection<CapacityMappingStorable> loadedContent = hisConnection.loadCollection(CapacityMappingStorable.class);
        for (CapacityMappingStorable next : loadedContent) {
          RuntimeContext runtimeContext;
          try {
            runtimeContext = revisionManagment.getRuntimeContext(next.getRevision());
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            logger.warn("Can't get runtimeContext for revision " + next.getRevision(), e);
            continue;
          }
          mapping.put(new DestinationKey(next.getOrderType(), runtimeContext), next);
        }
      } finally {
        hisConnection.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Failed to initialize " + DEFAULT_NAME, e);
    } finally {
      writeLock.unlock();
    }
    initialized = true;
  }
  
  private void updateArchive(boolean force) throws PersistenceLayerException {
    if (initialized && (isPersistenceDirect() || force)) {

      writeLock.lock();
      try {
        ODSConnection hisConnection = ods.openConnection(ODSConnectionType.HISTORY);
        try {
          hisConnection.deleteAll(CapacityMappingStorable.class);
          hisConnection.persistCollection(mapping.values());
          hisConnection.commit();
        } finally {
          hisConnection.closeConnection();
        }
      } finally {
        writeLock.unlock();
      }

    }
  }


  private boolean isPersistenceDirect() {
    String persistence =
        XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.CAPACITIES_DIRECTPERSISTENCE);
    if (persistence != null) {
      try {
        return Boolean.parseBoolean(persistence);
      } catch (Throwable e) {
        return true;
      }
    } else {
      return true;
    }
  }


  /**
   * Requires a capacity for a destination key, i.e., a order type, etc. If the provided capacity is already required by
   * this destination, the old cardinality is overwritten.
   */
  public boolean addCapacity(DestinationKey key, Capacity capacity) throws PersistenceLayerException,
      XFMG_InvalidCapacityCardinality {

    if (capacity.getCardinality() < 0) {
      throw new XFMG_InvalidCapacityCardinality(capacity.getCardinality() + "");
    }

    writeLock.lock();
    try {
      boolean modified = false;
      
      boolean foundExistingCapacityRequirement = false;

      CapacityMappingStorable storable = mapping.get(key);
      if (storable == null) {
        RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
        try {
          revision = revisionManagement.getRevision(key.getRuntimeContext());
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new RuntimeException(key.getRuntimeContext() + " unknown", e);
        }
        storable = new CapacityMappingStorable(key.getOrderType(), revision);
        mapping.put(key, storable);
        modified = true;
      } else {
        for (Capacity cap : storable.getRequiredCapacities()) {
          if (cap.getCapName().equals(capacity.getCapName())) {
            modified = capacity.getCardinality() != cap.getCardinality(); //nur dann Änderung
            cap.setCardinality(capacity.getCardinality());
            foundExistingCapacityRequirement = true;
            break;
          }
        }
      }

      if (!foundExistingCapacityRequirement) {
        storable.getRequiredCapacities().add(capacity);
        modified = true;
      }

      updateArchive(false);

      callCapacityMappingChangedListeners(key);
      return modified;

    } finally {
      writeLock.unlock();
      try {
        XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.warn("Failed to notify scheduler", t);
      }
    }

  }


  public boolean removeCapacity(DestinationKey key, String capacityName) throws PersistenceLayerException {

    writeLock.lock();
    try {

      boolean modified = false;
      
      CapacityMappingStorable storable = mapping.get(key);
      if (storable == null) {
        modified = false;
      } else if (storable.getRequiredCapacities().size() == 0) {
        mapping.remove(key);
        modified = false;
      } else {
        Iterator<Capacity> iter = storable.getRequiredCapacities().iterator();
        while(iter.hasNext()) {
          Capacity cap = iter.next();
          if (cap.getCapName().equals(capacityName)) {
            iter.remove();
            modified = true;
          }
        }
        if (storable.getRequiredCapacities().size() == 0) {
          mapping.remove(key);
        }
      }

      updateArchive(false);

      callCapacityMappingChangedListeners(key);
      
      return modified;

    }
    finally {
      writeLock.unlock();
      try {
        XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.warn("Failed to notify scheduler", t);
      }
    }

  }


  public void removeAllCapacities(DestinationKey key) throws PersistenceLayerException {

    writeLock.lock();
    try {
      if (mapping.remove(key) != null) {
        updateArchive(false);
      }
      callCapacityMappingChangedListeners(key);
    } finally {
      writeLock.unlock();
      try {
        XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.warn("Failed to notify scheduler", t);
      }
    }

  }


  /**
   * Returns a copy of the capacities associated with the provided DestinationKey. The result is not supposed to be
   * modified since it is only a copy of the cache
   */
  
  public List<Capacity> getCapacities(String orderType) {
    return getCapacities(new DestinationKey(orderType));
  }
  
  public List<Capacity> getCapacities(DestinationKey key) {

    readLock.lock();
    try {
      CapacityMappingStorable storable = mapping.get(key);
       
      if (storable != null) {
        List<Capacity> list = new ArrayList<Capacity>();
        for(Capacity cap : storable.getRequiredCapacities()) {
          list.add(cap.clone());        
        }
        return list;
      } else {
        return new ArrayList<Capacity>();
      }
    } finally {
      readLock.unlock();
    }

  }


  /**
   * Returns a list of all DestinationKeys. The result is not supposed to be modified since it is only a copy.
   */
  public Set<DestinationKey> getDestinationKeys() {

    readLock.lock();
    try {
      Set<DestinationKey> result = Collections.unmodifiableSet(mapping.keySet());
      return result;
    } finally {
      readLock.unlock();
    }

  }

  public List<CapacityMappingStorable> getAllCapacityMappings() {

    readLock.lock();
    try {
      List<CapacityMappingStorable> capacityMappings = new ArrayList<CapacityMappingStorable>();
      for (Entry<DestinationKey, CapacityMappingStorable> e : mapping.entrySet()) {
        CapacityMappingStorable newCapacityMappingStorable =
            new CapacityMappingStorable(e.getValue().getOrderType(), e.getValue().getRevision(), e.getValue().getRequiredCapacities(), e
                .getValue().getId());
        capacityMappings.add(newCapacityMappingStorable);
      }
      return capacityMappings;
    } finally {
      readLock.unlock();
    }

  }


  private Map<CapacityMappingChangeListener, Boolean> capacityMappingChangedListeners =
      new WeakHashMap<CapacityMappingChangeListener, Boolean>();


  /**
   * {@link CapacityMappingChangeListener} wird nur weakly gespeichert, damit objekte nicht mehr entfernt werden müssen
   * @param capacityMappingChangedListener
   */
  public void registerCapacityMappingChangedListener(CapacityMappingChangeListener capacityMappingChangedListener) {
    synchronized (capacityMappingChangedListeners) {
      capacityMappingChangedListeners.put(capacityMappingChangedListener, Boolean.FALSE);
    }
  }


  private void callCapacityMappingChangedListeners(DestinationKey key) {
    Set<CapacityMappingChangeListener> listeners;
    synchronized (capacityMappingChangedListeners) {
      listeners = new HashSet<CapacityMappingChangeListener>(capacityMappingChangedListeners.keySet());
    }
    for (CapacityMappingChangeListener l : listeners) {
      l.capacityMappingChanged(key);
    }
  }


}
