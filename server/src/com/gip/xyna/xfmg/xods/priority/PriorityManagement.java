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
package com.gip.xyna.xfmg.xods.priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.concurrent.HashParallelReentrantReadWriteLocks;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidXynaOrderPriority;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement.OrderTypeUpdates;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;


public class PriorityManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "PriorityManagement";

  private static final String VALID_THREAD_PRIORITY_BOUNDS = "[" + Thread.MIN_PRIORITY + "-" + Thread.MAX_PRIORITY + "]";
  public static final int HARDCODED_DEFAULT_PRIORITY = 7; 
  
  static {
    addDependencies(PriorityManagement.class,
                    new ArrayList<XynaFactoryPath>(Arrays
                        .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaFactoryManagement.class,
                                                                           XynaFactoryControl.class,
                                                                           DependencyRegister.class)})));
  }
  
  private ODS ods;
  private Map<Long, Map<String, Integer>> priorityMap;
  private HashParallelReentrantReadWriteLocks priorityLock = new HashParallelReentrantReadWriteLocks();
  
  
  public PriorityManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  
  private Map<String, Integer> getOrCreatePriorityMap(Long revision) {
    Map<String, Integer> result = priorityMap.get(revision);
    if(result == null){
      synchronized (priorityMap) {
        result = priorityMap.get(revision);
        if(result == null){
          result = new HashMap<String, Integer>();
          priorityMap.put(revision, result);
        }
      }
    }    
    return result;
  }

  @Override
  protected void init() throws XynaException {
    ods = ODSImpl.getInstance();
    ods.registerStorable(PrioritySetting.class);
    priorityMap = new HashMap<Long, Map<String, Integer>>();
    
    FutureExecution fexc = XynaFactory.getInstance().getFutureExecution();
    fexc.addTask(PriorityManagement.class, "PriorityManagement").execAsync(new Runnable() {

      @Override
      public void run() {
        try {
          ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
          try {
            Collection<PrioritySetting> prios = con.loadCollection(PrioritySetting.class);
            for (PrioritySetting prio : prios) {
              Map<String, Integer> tmpMap = getOrCreatePriorityMap(prio.getRevision());
              tmpMap.put(prio.getOrderType(), prio.getPriority());
            }
          } finally { // catch PersistenceLayerExceptions and log you're only going to give away DefaultPrio?
            con.closeConnection();
          }
        } catch (PersistenceLayerException e) {
          throw new RuntimeException(e);
        }
      }
      
    });
 
    XynaProperty.CONFIGURABLE_DEFAULT_XYNAORDER_PRIORITY.registerDependency(DEFAULT_NAME);
  }


  @Override
  protected void shutdown() throws XynaException {

  }
  
  /**
   * Mehrstufige Suche nach der Priority:
   * 1. Suche nach Prio für (OrderType,Revision)
   * 2. Übernahme der Priority aus copyPriorityFrom
   * 3. XynaProperty.CONFIGURABLE_DEFAULT_XYNAORDER_PRIORITY
   * 4. HARDCODED_DEFAULT_PRIORITY
   * @param orderType darf null sein
   * @param revision darf null sein
   * @param copyPriorityFrom darf null sein
   */
  public int determinePriority(String orderType, Long revision, XynaOrderServerExtension copyPriorityFrom) {
    if (orderType != null ) {
      Integer prio = revision != null ? getPriority(orderType, revision) : getPriority(orderType, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
      if( prio != null ) {
        int priority = prio.intValue();
        if( inAllowedPriorityRange( priority ) ) {
          return priority;
        }
      }
    }
    
    if ( copyPriorityFrom != null ) {
      int priority = copyPriorityFrom.getPriority();
      if( inAllowedPriorityRange( priority ) ) {
        return priority;
      }
    }
    
    int priority = XynaProperty.CONFIGURABLE_DEFAULT_XYNAORDER_PRIORITY.get();
    if( inAllowedPriorityRange( priority ) ) {
      return priority; 
    } else {
      return HARDCODED_DEFAULT_PRIORITY;
    }
  }
  
  /**
   * Setzt die Priority über {@link #determinePriority(String, Long, XynaOrderServerExtension)} mit 
   * Übername der Priority aus der Parent-XynaOrder
   * @param xo
   */
  public void discoverPriority(XynaOrderServerExtension xo) {
    int priority = xo.getPriority();
    
    if( ! inAllowedPriorityRange( priority ) ) {
      String orderType = xo.getDestinationKey().getOrderType();
      Long revision;
      try {
        revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(xo.getDestinationKey().getRuntimeContext());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      priority = determinePriority( orderType, revision, xo.getParentOrder() );
      xo.setPriority(priority);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("XynaOrder discovered Priority: " + xo.getPriority());
    }
  }


  private boolean inAllowedPriorityRange(int priority) {
    return priority >= Thread.MIN_PRIORITY && priority <= Thread.MAX_PRIORITY;
  }


  public void setPriority(String orderType, int priority) throws XFMG_InvalidXynaOrderPriority,
      PersistenceLayerException {
    setPriority(orderType, priority, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public void setPriority(String orderType, int priority, Long revision) throws XFMG_InvalidXynaOrderPriority,
      PersistenceLayerException {

    if (isValidPriority(priority)) {
      priorityLock.writeLock(orderType);
      try {
        getOrCreatePriorityMap(revision).put(orderType, priority);
        PrioritySetting prio = new PrioritySetting(orderType, priority, revision); 
        ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
        try {
          con.persistObject(prio);
          con.commit();
        } finally {
          con.closeConnection();
        }
      } finally {
        priorityLock.writeUnlock(orderType);
      }
    } else {
      throw new XFMG_InvalidXynaOrderPriority(priority, VALID_THREAD_PRIORITY_BOUNDS);
    }
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement()
        .updateInCache(revision, orderType, OrderTypeUpdates.setPriority(priority));
  }
  
  public Integer getPriority(String orderType) throws PersistenceLayerException {
    return getPriority(orderType, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  /**
   * @return konfigurierte priority oder null, falls nicht konfiguriert für den ordertype 
   */
  public Integer getPriority(String orderType, Long revision) {
    priorityLock.readLock(orderType);
    try {
      Integer priority = getOrCreatePriorityMap(revision).get(orderType);
      return priority;
    } finally {
      priorityLock.readUnlock(orderType);
    }
  }
  
  public void removePriority(String orderType) throws PersistenceLayerException {
    removePriority(orderType, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  
  public void removePriority(String orderType, Long revision) throws PersistenceLayerException {
    priorityLock.writeLock(orderType);
    try {
      if (getOrCreatePriorityMap(revision).remove(orderType) != null) {
        PrioritySetting prio = new PrioritySetting(orderType, revision);
        ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
        try {
          con.deleteOneRow(prio);
          con.commit();
        } finally {
          con.closeConnection();
        }
      }
    } finally {
      priorityLock.writeUnlock(orderType);
    }
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement()
        .updateInCache(revision, orderType, OrderTypeUpdates.setPriority(null));
  }
  
  
  public Collection<PrioritySetting> listPriorities() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(PrioritySetting.class);
    } finally {
      con.closeConnection();
    }
  }


  public static boolean isValidPriority(int priority) {
    return ((priority >= Thread.MIN_PRIORITY) && (priority <= Thread.MAX_PRIORITY));
  }
  
  
  public static int restrictPriorityToThreadPriorityBounds(int priority) {
    if (priority < Thread.MIN_PRIORITY) {
      return Thread.MIN_PRIORITY;
    } else if (priority > Thread.MAX_PRIORITY) {
      return Thread.MAX_PRIORITY;
    } else {
      return priority;
    }
  }
  
  public List<String> getAllPrioritiesForRevision(Long revision) {
    List<String> result = new ArrayList<String>();
    Map<String, Integer> tmpMap = getOrCreatePriorityMap(revision);
    for(String value : tmpMap.keySet()) {
      result.add(value);
    }
    return result;
  }
  
}
