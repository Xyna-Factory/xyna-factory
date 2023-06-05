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
package com.gip.xyna.xfmg.xfctrl.deploystate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentTransition;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;


public class DeploymentItemStateRegistry implements DeploymentItemRegistry {

  private final static Logger logger = CentralFactoryLogging.getLogger(DeploymentItemStateRegistry.class);
  
  private final static String REVISION_RESTORATION_QUERY_SQL = 
    "select * from " + DeploymentItemStateStorable.TABLENAME + " where " + DeploymentItemStateStorable.COL_REVISION + " = ?";
  private static PreparedQuery<DeploymentItemStateStorable> REVISION_RESTORATION_QUERY;
  
  private final long managedRevision;
  private final ConcurrentMap<String, DeploymentItemState> registered;
  
  
  public DeploymentItemStateRegistry(long managedRevision) {
    registered = new ConcurrentHashMap<String, DeploymentItemState>();
    this.managedRevision = managedRevision;
  }
  
  
  public DeploymentItemStateRegistry() {
    registered = new ConcurrentHashMap<String, DeploymentItemState>();
    this.managedRevision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
  }


  public DeploymentItemState get(String fqName) {
    return registered.get(fqName);
  }
  
  public long getManagedRevision() {
    return managedRevision;
  }

  public Set<DeploymentItemState> list() {
    return new HashSet<DeploymentItemState>(registered.values());
  }
  
  public void save(DeploymentItem di) {
    DeploymentItemStateImpl dis = new DeploymentItemStateImpl(di, this);
    DeploymentItemState disNow = addIfAbsent(dis);
    if (disNow == dis) {
      dis.validate(DeploymentLocation.SAVED);
    } else {
      disNow.save(di);
    }
    store((DeploymentItemStateImpl)disNow);
  }
  

  public void save(String fqName) {
    try {
      Optional<DeploymentItem> build = DeploymentItemBuilder.build(fqName, Optional.<XMOMType>empty(), true, managedRevision);
      if (build.isPresent()) {
        save(build.get());
      } else {
        throw new RuntimeException("No build");
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }
  
  
  public void delete(String fqName, DeploymentContext ctx) {
    /*
     * FIXME evtl gibt es ein duplikat in einer kind-revision: dann dort die callsites hinmigrieren
     * 
     * falls es ein duplikat in einer parent-revision gibt, ist es schwieriger:
     *   lokale callsites können im parent nicht immer aufgelöst werden.
     *    - es kann aber welche geben, die dort aufgelöst werden können und deshalb dort migriert werden sollten.
     */          
    DeploymentItemState dis = registered.get(fqName);
    if (dis == null) {
      logger.warn(fqName + ".delete, but its not registered");
    } else {
      dis.delete(ctx);
      delete((DeploymentItemStateImpl)dis);
    }
  }


  public void collectUsingObjectsInContext(String fqName, DeploymentContext ctx) {
    DeploymentItemState dis = registered.get(fqName);
    if (dis == null) {
      logger.warn(fqName + ".collectUsingObjectsInContext, but its not registered");
    } else {
      dis.collectUsingObjectsInContext(ctx);
    }
  }


  public void undeploy(String fqName, DeploymentContext ctx) {
    DeploymentItemState dis = registered.get(fqName);
    if (dis == null) {
      logger.warn(fqName + ".undeploy, but its not registered");
    } else {
      dis.undeploy(ctx);
    }
  }


  public void deployFinished(String fqName, DeploymentTransition transition, boolean copiedXMLFromSaved,
                             Optional<? extends Throwable> deploymentException) {
    DeploymentItemState dis = registered.get(fqName);
    if (dis == null) {
      logger.warn(fqName + ".deploy, but its not registered");
    } else {
      Optional<List<Throwable>> exceptionCauses = collectExceptionCauses(deploymentException);
      try {
        boolean changed = dis.deploymentTransition(transition, copiedXMLFromSaved, deploymentException);
        if (changed) {
          store((DeploymentItemStateImpl) dis);
        }
      } finally {
        //beim speichern der exception werden exception causes aus java serialisierungsgründen in "deploymentTransition" genullt. 
        //das deployment soll die causes aber noch sehen können
        restoreExceptionCauses(deploymentException, exceptionCauses);
      }
    }
  }

  private void restoreExceptionCauses(Optional<? extends Throwable> ex, Optional<List<Throwable>> causes) {
    if (ex.isPresent()) {
      Throwable t = ex.get();
      List<Throwable> cs = causes.get();
      for (int i = 1; i < cs.size(); i++) {
        Throwable c = cs.get(i);
        if (t.getCause() == null) {
          t.initCause(c);
        } else if (t.getCause() == c) {
          //ok
        } else {
          throw new RuntimeException("Cxception cause changed unexpectedly");
        }
        t = c;
      }
    }
  }


  private Optional<List<Throwable>> collectExceptionCauses(Optional<? extends Throwable> ex) {
    if (ex.isPresent()) {
      List<Throwable> l = new ArrayList<>();
      Throwable t = ex.get();
      while (t != null) {
        l.add(t);
        t = t.getCause();
      }
      return Optional.of(l);
    } else {
      return Optional.empty();
    }
  }


  public void buildFinished(String fqName, Optional<? extends Throwable> buildException) {
    DeploymentItemState dis = registered.get(fqName);
    if (dis == null) {
      logger.warn(fqName + ".build, but its not registered");
    } else {
      dis.setBuildError(buildException);
      store((DeploymentItemStateImpl)dis);
    }
  }


  public void init(Set<DeploymentItem> allItems) {
    if (logger.isDebugEnabled()) {
      logger.debug("init with #" + allItems.size() + " items in " + managedRevision);
    }
    RuntimeContextDependencyManagement rcdm =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    DeploymentItemStateManagement dism =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    for (DeploymentItem di : allItems) {
      saveInMapAndInheritFromPhantoms(di, rcdm, dism);
    }
    Map<String, DeploymentItemStateStorable> restoration = restore();
    for (DeploymentItem di : allItems) {
      DeploymentItemStateImpl disi = (DeploymentItemStateImpl) registered.get(di.getName());
      if (disi != null && disi.exists()) {
        if (restoration.containsKey(disi.getName())) {
          disi.restore(restoration.get(disi.getName()));
        }
        if (!di.isApplicationItem()) {
          disi.validate(DeploymentLocation.SAVED);
        }
        disi.validate(DeploymentLocation.DEPLOYED);
      }
    }
  }


  /**
   * @return stored deploymentitemstate (new or previous)
   */
  public synchronized DeploymentItemState addIfAbsent(DeploymentItemState dis) {
    DeploymentItemState previous = registered.putIfAbsent(dis.getName(), dis);
    if (previous != null) {
      return previous;
    } else {
      RuntimeContextDependencyManagement rcdm =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      DeploymentItemStateManagement dism =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
      //gibt es ein phantom in einer parentrevision?
      Set<Long> revisions = new HashSet<Long>();
      rcdm.getParentRevisionsRecursivly(getManagedRevision(), revisions);
      revisions.remove(getManagedRevision());
      List<DeploymentItemState> phantoms = new ArrayList<DeploymentItemState>(1);
      for (long r : revisions) {
        DeploymentItemState phantomCandidate = dism.get(dis.getName(), r);
        if (phantomCandidate != null && !phantomCandidate.exists()) {
          phantoms.add(phantomCandidate);
          DeploymentItemStateRegistry disr = (DeploymentItemStateRegistry) dism.getRegistry(r);
          disr.registered.remove(dis.getName());
        }
      }

      //lokal anlegen
      registered.put(dis.getName(), dis);
      for (DeploymentItemState phantom : phantoms) {
        Set<DeploymentItemState> callSites = phantom.getInvocationSites(DeploymentLocation.SAVED);
        for (DeploymentItemState callSite : callSites) {
          dis.addInvocationSite(callSite, DeploymentLocation.SAVED);
        }
        callSites = phantom.getInvocationSites(DeploymentLocation.DEPLOYED);
        for (DeploymentItemState callSite : callSites) {
          dis.addInvocationSite(callSite, DeploymentLocation.DEPLOYED);
        }
      }
    }
    return dis;
  }

  
  private DeploymentItemState saveInMapAndInheritFromPhantoms(DeploymentItem di, RuntimeContextDependencyManagement rcdm,
                                               DeploymentItemStateManagement dism) {
    DeploymentItemState previous = registered.get(di.getName());
    if (previous != null) {
      //lokales phantom ergänzen
      if (previous.exists()) {
        logger.warn(di.getName() + " exists in " + getManagedRevision());
      }
      
      Set<DeploymentLocation> locations = new HashSet<DeploymentLocation>();
      if (!di.isApplicationItem()) {
        locations.add(DeploymentLocation.SAVED);
      }
      locations.add(DeploymentLocation.DEPLOYED);
      previous.update(di, locations);
    } else {
      //gibt es ein phantom in einer parentrevision?
      Set<Long> revisions = new HashSet<Long>();
      rcdm.getParentRevisionsRecursivly(getManagedRevision(), revisions);
      revisions.remove(getManagedRevision());
      List<DeploymentItemState> phantoms = new ArrayList<DeploymentItemState>(1);
      for (long r : revisions) {
        DeploymentItemState phantomCandidate = dism.get(di.getName(), r);
        if (phantomCandidate != null && !phantomCandidate.exists()) {
          phantoms.add(phantomCandidate);
          DeploymentItemStateRegistry disr = (DeploymentItemStateRegistry) dism.getRegistry(r);
          disr.registered.remove(di.getName());
        }
      }

      //lokal anlegen
      DeploymentItemStateImpl newDis = new DeploymentItemStateImpl(di, this);
      registered.put(di.getName(), newDis);
      for (DeploymentItemState phantom : phantoms) {
        Set<DeploymentItemState> callSites = phantom.getInvocationSites(DeploymentLocation.SAVED);
        for (DeploymentItemState callSite : callSites) {
          newDis.addInvocationSite(callSite, DeploymentLocation.SAVED);
        }
        callSites = phantom.getInvocationSites(DeploymentLocation.DEPLOYED);
        for (DeploymentItemState callSite : callSites) {
          newDis.addInvocationSite(callSite, DeploymentLocation.DEPLOYED);
        }
      }
    }
    return previous;
  }


  private Map<String, DeploymentItemStateStorable> restore() {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      PreparedQuery<DeploymentItemStateStorable> revisionRestorationQuery;
      if (REVISION_RESTORATION_QUERY == null) {
        try {
          revisionRestorationQuery = con.prepareQuery(new Query<DeploymentItemStateStorable>(REVISION_RESTORATION_QUERY_SQL, DeploymentItemStateStorable.reader));
          REVISION_RESTORATION_QUERY = revisionRestorationQuery;
        } catch (PersistenceLayerException e) {
          logger.debug("Failed to prepare revision restoration query '" + REVISION_RESTORATION_QUERY_SQL + "'", e);
          return Collections.emptyMap();
        }
      } else {
        revisionRestorationQuery = REVISION_RESTORATION_QUERY;
      }
      try {
        List<DeploymentItemStateStorable> result = con.query(revisionRestorationQuery, new Parameter(managedRevision), -1);
        Map<String, DeploymentItemStateStorable> restoration = new HashMap<String, DeploymentItemStateStorable>();
        for (DeploymentItemStateStorable diss : result) {
          restoration.put(diss.getFqName(), diss);
        }
        return restoration;
      } catch (PersistenceLayerException e) {
        logger.debug("Failed to execute revision restoration query '" + REVISION_RESTORATION_QUERY + "'", e);
        return Collections.emptyMap();
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.debug("Failed to close read only connection for DeploymentItemState restoration", e);
      }
    }
  }
  
  
  
  private void store(DeploymentItemStateImpl disi) {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      try {
        con.persistObject(new DeploymentItemStateStorable(disi, managedRevision));
        con.commit();
      } catch (PersistenceLayerException e) {
        logger.debug("Failed to persist '" + disi.getName() + "' into backing store", e);
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.debug("Failed to close read only connection for DeploymentItemState restoration", e);
      }
    }
  }



  private void delete(DeploymentItemStateImpl disi) {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      try {
        con.deleteOneRow(new DeploymentItemStateStorable(disi, managedRevision));
        con.commit();
      } catch (PersistenceLayerException e) {
        logger.debug("Failed to delete '" + disi.getName() + "' from backing store on object deletion", e);
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.debug("Failed to close read only connection for DeploymentItemState restoration", e);
      }
    }
  }


  public void update(DeploymentItem di, Set<DeploymentLocation> locations) {
    DeploymentItemState dis = registered.get(di.getName());
    if (dis == null) {
      dis = addIfAbsent(new DeploymentItemStateImpl(di, this));
    }
    dis.update(di, locations);
  }


  @Override
  public void invalidateCallSites() {
    for (DeploymentItemState dis : registered.values()) {
      ((DeploymentItemStateImpl) dis).invalidateCallSites();
    }
  }


  public void check() {
    for (DeploymentItemState dis : registered.values()) {
      ((DeploymentItemStateImpl) dis).check();
    }
  }

}
