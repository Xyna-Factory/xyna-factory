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
package com.gip.xyna.xfmg.xfctrl.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;


public class RuntimeContextDependencyStorage {

  private static Logger logger = CentralFactoryLogging.getLogger(RuntimeContextDependencyStorage.class);

  
  private ODSImpl ods;
  private PreparedQueryCache queryCache;
  
  private ReentrantLock lock = new ReentrantLock();
  
  private static final String QUERY_GET_BY_OWNER = 
                  "select * from "+RuntimeContextDependencyStorable.TABLENAME
                  +" where " + RuntimeContextDependencyStorable.COL_TYPE + "=? and "
                  + RuntimeContextDependencyStorable.COL_NAME + "=?";

  private static final String QUERY_GET_BY_OWNER_WITH_ADDITION = 
                  "select * from "+RuntimeContextDependencyStorable.TABLENAME
                  +" where " + RuntimeContextDependencyStorable.COL_TYPE + "=? and "
                  + RuntimeContextDependencyStorable.COL_NAME + "=? and "
                  + RuntimeContextDependencyStorable.COL_ADDITION + "=?";
  
  
  public RuntimeContextDependencyStorage() throws PersistenceLayerException {
    ods = ODSImpl.getInstance();
    ods.registerStorable(RuntimeContextDependencyStorable.class);
    queryCache = new PreparedQueryCache();
  }


  public Collection<RuntimeContextDependencyStorable> getAllDependencies() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(RuntimeContextDependencyStorable.class);
    } finally {
      finallyClose(con);
    }
  }

  
  public void modifyDependencies(RuntimeDependencyContext owner, Collection<RuntimeDependencyContext> newDependencies) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      lock.lock();
      try {
        List<RuntimeContextDependencyStorable> existingDependencies = queryDependencies(con, owner);
        
        List<RuntimeContextDependencyStorable> toDelete = new ArrayList<RuntimeContextDependencyStorable>();
        List<RuntimeDependencyContext> toKeep = new ArrayList<RuntimeDependencyContext>();
        for(RuntimeContextDependencyStorable old : existingDependencies) {
          if (!newDependencies.contains(old.getDependency())) {
            //in neuen Dependencies nicht enthalten, daher l�schen
            toDelete.add(old);
          } else {
            //in den neuen Dependencies enthalten, daher behalten
            toKeep.add(old.getDependency());
          }
        }
        
        con.delete(toDelete);
        
        //neue Dependencies anlegen, falls nicht bereits vorhanden
        List<RuntimeContextDependencyStorable> toCreate = new ArrayList<RuntimeContextDependencyStorable>();
        for (RuntimeDependencyContext newDep : newDependencies) {
          if (!toKeep.contains(newDep)) {
            toCreate.add(new RuntimeContextDependencyStorable(owner, newDep));
          }
        }
        
        con.persistCollection(toCreate);
        
        con.commit();
      } finally {
        lock.unlock();
      }
    } finally {
      finallyClose(con);
    }
  }
  

  private List<RuntimeContextDependencyStorable> queryDependencies(ODSConnection con, RuntimeDependencyContext owner) throws PersistenceLayerException {
    String addition = null;
    if (owner instanceof Application) {
      addition = ((Application) owner).getVersionName();
    }
    if (owner instanceof ApplicationDefinition) {
      addition = ((ApplicationDefinition) owner).getParentWorkspace().getName();
    }
    
    String queryString;
    Parameter parameter;
    if (addition == null) {
      queryString = QUERY_GET_BY_OWNER;
      parameter = new Parameter(owner.getRuntimeDependencyContextType().name(), owner.getName());
    } else {
      queryString = QUERY_GET_BY_OWNER_WITH_ADDITION;
      parameter = new Parameter(owner.getRuntimeDependencyContextType().name(), owner.getName(), addition);
    }
    
    PreparedQuery<RuntimeContextDependencyStorable> query = 
                    queryCache.getQueryFromCache(queryString, con, RuntimeContextDependencyStorable.reader);
    return con.query(query, parameter, -1);
  }
  
  
  public void deleteRuntimeContext(RuntimeDependencyContext owner) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      lock.lock();
      try {
        List<RuntimeContextDependencyStorable> toDelete = queryDependencies(con, owner);
        
        con.delete(toDelete);
        con.commit();
      } finally {
        lock.unlock();
      }
    } finally {
      finallyClose(con);
    }
  }
  
  private static void finallyClose(ODSConnection con) {
    if( con != null ) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }
}
