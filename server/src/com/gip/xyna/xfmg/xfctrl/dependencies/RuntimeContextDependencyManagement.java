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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.MtoNMapping;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.FilterInformation;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.FilterInstanceStorable.FilterInstanceState;
import com.gip.xyna.xact.trigger.TriggerInformation;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable.TriggerInstanceState;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.RepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotStartApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotStopApplication;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ClearWorkingSet.RevisionContentBlackWhiteListBean;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl.OrderEntryInterfacesCouldNotBeClosedException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcherFactory;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagementImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagementImpl.StateTransition;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.Inconsistency;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.CrossRevisionResolver;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.RevisionBasedCrossResolver;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement.RuntimeContextProblemParameter;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.rtctxmgmt.RuntimeContextChangeHandler;
import com.gip.xyna.xfmg.xfctrl.rtctxmgmt.RuntimeContextManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyEnum;
import com.gip.xyna.xmcp.xguisupport.messagebus.Publisher;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement.StructureCacheRegistrator;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureIdentifier;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureRecursionFilter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureVisitor;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.exceptions.XFMG_CouldNotModifyRuntimeContextDependenciesException;
import com.gip.xyna.xprc.exceptions.XFMG_DependencyStillUsedByApplicationDefinitionException;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_WorkflowProtectionModeViolationException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.WorkflowRevision;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;


public class RuntimeContextDependencyManagement extends FunctionGroup {
  
  public static final String DEFAULT_NAME = "RuntimeContextDependencyManagement";

  // TODO cache duplication is not pretty, remove RuntimeContext-cache and only keeps revs?
  //      requests containing RuntimeContexts should be external and are therefore not as privileged 
  private Map<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> dependencies = new ConcurrentHashMap<RuntimeDependencyContext, Collection<RuntimeDependencyContext>>();
  private MtoNMapping<Long, Long> revDependencies = new MtoNMapping<Long, Long>();
  
  private RuntimeContextDependencyStorage storage = new RuntimeContextDependencyStorage();

  public RuntimeContextDependencyManagement() throws XynaException {
    super();
  }
  
  public RuntimeContextDependencyManagement(String cause) throws XynaException  {
    super(cause);
    initRuntimeContextDependencyStorage();
    initRuntimeContextDependencies();
  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(RuntimeContextDependencyStorage.class,"RuntimeContextDependencyManagement.initRuntimeContextDependencyStorage").
      after(PersistenceLayerInstances.class).
      execAsync(new Runnable() { public void run() { initRuntimeContextDependencyStorage(); }});
    
    fExec.addTask(RuntimeContextDependencyManagement.class,"RuntimeContextDependencyManagement.initRuntimeContextDependencies").
      after(RuntimeContextDependencyStorage.class, RevisionManagement.class).
      execAsync(new Runnable() { public void run() { initRuntimeContextDependencies(); }}); 
    
    fExec.addTask("RuntimeContextDependencyManagement.deploymentItemStateManagement-Cache", "RuntimeContextDependencyManagement.deploymentItemStateManagement-Cache").
      after(DeploymentItemStateManagementImpl.class, RuntimeContextDependencyManagement.class).
      execAsync(new Runnable() { public void run() { 
        dism =  //nur Caching
            (DeploymentItemStateManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                .getDeploymentItemStateManagement();
        }});
  }


  private void initRuntimeContextDependencyStorage() {
    try {
      storage = new RuntimeContextDependencyStorage();
    } catch( PersistenceLayerException e ) {
      logger.warn("Could not initialize RuntimeContextDependencyManagement", e);
      throw new RuntimeException(e);
    }
  }
  
  private void initRuntimeContextDependencies() {
    try {
      for (RuntimeContextDependencyStorable rcds : storage.getAllDependencies()) {
        Collection<RuntimeDependencyContext> deps = dependencies.get(rcds.getOwner());
        if (deps == null) {
          deps = new ArrayList<RuntimeDependencyContext>();
          dependencies.put(rcds.getOwner(), deps);
        }
        deps.add(rcds.getDependency());
      }
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      for (Entry<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> entry : dependencies.entrySet()) {
        if (hasRevision(entry.getKey())) { //ApplicationDefinitions haben keine revision
          Long ownerRev = revMgmt.getRevision((RuntimeContext)entry.getKey());
          for (RuntimeDependencyContext dep : entry.getValue()) {
            long depRev  = revMgmt.getRevision((RuntimeContext)dep);
            revDependencies.add(ownerRev, depRev);
          }
        }
      }
    } catch ( XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      logger.warn("Could not initialize RuntimeContextDependencyManagement", e);
      throw new RuntimeException(e);
    } catch( PersistenceLayerException e ) {
      logger.warn("Could not initialize RuntimeContextDependencyManagement", e);
      throw new RuntimeException(e);
    }
  }
  

  @Override
  protected void shutdown() throws XynaException {
    // TODO Auto-generated method stub
    
  }

  /*
   * TODO alle verwender sollten aus performancegründen evtl besser auf getDependenciesRecursively umgestellt werden, weil sie sonst selbst
   * eine behandlung dafür benötigen, die dependencies nicht doppelt zu überprüfen.
   */
  public Set<Long> getDependencies(long revision) {
    Set<Long> deps = revDependencies.getValuesUnsafe(revision);
    if (deps != null) {
      return deps;
    } else {
      return Collections.emptySet();
    }
  }
  
  /**
   * Welche RuntimeContexte benötigt owner?
   * @param owner
   * @return
   */
  public Collection<RuntimeDependencyContext> getRequirements(RuntimeDependencyContext owner) {
    if (dependencies.containsKey(owner)) {
      return Collections.unmodifiableCollection(dependencies.get(owner));
    } else {
      return Collections.emptyList();
    }
  }
  
  
  public void getRequirementsRecursivly(RuntimeDependencyContext owner, Set<RuntimeDependencyContext> requirements) {
    Collection<RuntimeDependencyContext> reqs = getRequirements(owner);
    for (RuntimeDependencyContext requirement : reqs) {
      if (requirements.add(requirement)) {
        getRequirementsRecursivly(requirement, requirements);
      }
    }
  }
  
  
  public Collection<RuntimeDependencyContext> getDependencies(RuntimeDependencyContext owner) {
    return getRequirements(owner);
  }

  public Map<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> getAllDependencies() {
    return Collections.unmodifiableMap(dependencies);
  }
  
  
  public void getDependenciesRecursivly(Long revision, Set<Long> dependencies) {
    Set<Long> newDeps = getDependencies(revision);
    for (Long newDep : newDeps) {
      if (dependencies.add(newDep)) {
        getDependenciesRecursivly(newDep, dependencies);
      }
    }
  }
  
  //dependencies sammeln von revision aus. ergebnis in dependencies schreiben. simulation: revisionWithChanges hat nicht die normalen deps, sondern die changedDependencies
  private void getDependenciesRecursivlyWithSimulatedChanges(Long revision, Set<Long> dependencies, Long revisionWithChanges, Set<Long> changedDependencies) {
    Set<Long> newDeps;
    if (revision.equals(revisionWithChanges)) {
      newDeps = changedDependencies;
    } else {
      newDeps = getDependencies(revision);
    }
    for (Long newDep : newDeps) {
      if (dependencies.add(newDep)) {
        getDependenciesRecursivlyWithSimulatedChanges(newDep, dependencies, revisionWithChanges, changedDependencies);
      }
    }
  }
  
  
  private Set<Long> getParentRevisions(Long revision) {
    Set<Long> parents = revDependencies.getKeys(revision);
    if (parents != null) {
      return parents;
    } else {
      return new HashSet<Long>();
    }
  }
  
  private Set<Long> getParentRevisionsRecursivly(Long revision) {
    Set<Long> parents = new HashSet<Long>();
    getParentRevisionsRecursivly(revision, parents);
    return parents;
  }
  
  
  public void getParentRevisionsRecursivly(Long revision, Set<Long> parents) {
    Set<Long> newParents = getParentRevisions(revision);
    newParents.removeAll(parents);
    parents.addAll(newParents);
    for (Long newParent : newParents) {
      getParentRevisionsRecursivly(newParent, parents);
    }
  }
  
  
  public Set<RuntimeDependencyContext> getParentRuntimeContexts(RuntimeDependencyContext requirement) {
    Set<RuntimeDependencyContext> parents = new HashSet<RuntimeDependencyContext>();
    for (Entry<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> entry : dependencies.entrySet()) {
      if (entry.getValue().contains(requirement)) {
        parents.add(entry.getKey());
      }
    }
    return parents;
  }


  public List<RuntimeContext> getParentRuntimeContextsSorted(RuntimeDependencyContext rc) {
    final RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    List<RuntimeContext> runtimeContextsToPublishAsXMOMUpdate = new ArrayList<RuntimeContext>();
    runtimeContextsToPublishAsXMOMUpdate.add(rc.asCorrespondingRuntimeContext());
    try {
      for (Long parentRevision : getParentRevisionsRecursivly(rm.getRevision(rc.asCorrespondingRuntimeContext()))) {
        RuntimeContext parentRuntimeContext;
        parentRuntimeContext = rm.getRuntimeContext(parentRevision);
        runtimeContextsToPublishAsXMOMUpdate.add(parentRuntimeContext);
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    //für die gui so sortieren, dass immer erst die kinder, dann die parents benachrichtigt werden
    sortRuntimeContextsForGUI(runtimeContextsToPublishAsXMOMUpdate);
    return runtimeContextsToPublishAsXMOMUpdate;
  }
  
  public List<RuntimeContext> getParentRuntimeContextsSorted(Collection<RuntimeContextDependencyChange> changes) {
    final RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    List<RuntimeContext> runtimeContextsToPublishAsXMOMUpdate = new ArrayList<RuntimeContext>();
    for (RuntimeContextDependencyChange change : changes) {
      runtimeContextsToPublishAsXMOMUpdate.add(change.getOwner().asCorrespondingRuntimeContext());
      try {
        for (Long parentRevision : getParentRevisionsRecursivly(rm.getRevision(change.getOwner().asCorrespondingRuntimeContext()))) {
          RuntimeContext parentRuntimeContext;
          parentRuntimeContext = rm.getRuntimeContext(parentRevision);
          runtimeContextsToPublishAsXMOMUpdate.add(parentRuntimeContext);
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
    }
    //für die gui so sortieren, dass immer erst die kinder, dann die parents benachrichtigt werden
    sortRuntimeContextsForGUI(runtimeContextsToPublishAsXMOMUpdate);
    return runtimeContextsToPublishAsXMOMUpdate;
  }
  
  public void sortRuntimeContextsForGUI(List<RuntimeContext> runtimeContextsToPublishAsXMOMUpdate) {
    final RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Collections.sort(runtimeContextsToPublishAsXMOMUpdate, new Comparator<RuntimeContext>() {

      public int compare(RuntimeContext o1, RuntimeContext o2) {
        if (o1.equals(o2)) {
          return 0;
        }
        int cnt1 = getCntOfDeps(o1);
        int cnt2 = getCntOfDeps(o2);
        return Integer.compare(cnt1, cnt2);
      }
      
      private final Map<RuntimeContext, Integer> cache = new HashMap<>();

      private int getCntOfDeps(RuntimeContext o) {
        Integer cnt = cache.get(o);
        if (cnt == null) {
          Set<Long> deps = new HashSet<>();
          try {
            getDependenciesRecursivly(rm.getRevision(o), deps);
            cnt = deps.size();
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            cnt = 0;
          }
          cache.put(o, cnt);
        }
        return cnt;
      }

    });
  }


  /**
   * Überprüft (rekursiv), ob die childRevision in der Abhängigkeitshierarchie der parentRevision enthalten ist
   */
  public boolean isDependency(long parentRevision, long childRevision) {
    Set<Long> requirements = new HashSet<Long>();
    getDependenciesRecursivly(parentRevision, requirements);
    return requirements.contains(childRevision);
  }
  
  private DeploymentItemStateManagementImpl dism;
  
  private Long getRevisionDefiningXMOMObjectDISM(String originalXmlName, Long parentRevision, boolean checkDependencies) {
    DeploymentItemState dis = dism.get(originalXmlName, parentRevision);
    if (dis != null && dis.exists()) {
      return parentRevision;
    } else if (checkDependencies) {
      Set<Long> deps = new HashSet<Long>();
      getDependenciesRecursivly(parentRevision, deps);
      for (Long dep : deps) {
        Long revision = getRevisionDefiningXMOMObjectDISM(originalXmlName, dep, false);
        if (revision != null) {
          return revision;
        }
      }
    }
    return null;
  }


  private static class FilesInRevision {

    private final Set<String> fqXmlNames;


    private FilesInRevision(Long rev) {
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      boolean isWorkspace = revMgmt.isWorkspaceRevision(rev);
      fqXmlNames = XMOMDatabase.discoverAllXMOMFqNames(rev, !isWorkspace);
    }

  }


  private ConcurrentMap<Long, FilesInRevision> cacheForFileSystem = new ConcurrentHashMap<Long, FilesInRevision>();


  private Long getRevisionDefiningXMOMObjectFileSystem(String originalXmlName, Long parentRevision, boolean checkDependencies) {
    FilesInRevision fir = cacheForFileSystem.get(parentRevision);
    if (fir == null) {
      fir = new FilesInRevision(parentRevision);
      cacheForFileSystem.put(parentRevision, fir);
    }
    if (fir.fqXmlNames.contains(originalXmlName)) {
      return parentRevision;
    } else if (checkDependencies) {
      Set<Long> deps = new HashSet<Long>();
      getDependenciesRecursivly(parentRevision, deps);
      for (Long dep : deps) {
        Long revision = getRevisionDefiningXMOMObjectFileSystem(originalXmlName, dep, false);
        if (revision != null) {
          return revision;
        }
      }
    }
    return null;
  }

  public Long getRevisionDefiningSharedLib(final String sharedLibName, Long parentRevision) {
    return getRevisionDefiningSharedLib(sharedLibName, parentRevision, true);
  }
  
  private Long getRevisionDefiningSharedLib(final String sharedLibName, Long parentRevision, boolean checkDependencies) {
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    boolean isWorkspace = revMgmt.isWorkspaceRevision(parentRevision);
    File sharedLibRoot = new File(RevisionManagement.getPathForRevision(PathType.SHAREDLIB, parentRevision, !isWorkspace));
    Long definingRevision = null;
    if (sharedLibRoot.exists()) {
      File[] sharedLibs = sharedLibRoot.listFiles(new FileFilter() {
        
        public boolean accept(File file) {
          return file.isDirectory() && file.getName().equals(sharedLibName);
        }
      });
      if (sharedLibs != null &&
          sharedLibs.length > 0) {
        definingRevision = parentRevision;
      }
    }
    if (definingRevision != null) {
      return definingRevision;
    } else if (checkDependencies) {
      Set<Long> deps = new HashSet<Long>();
      getDependenciesRecursivly(parentRevision, deps);
      for (Long dep : deps) {
        Long revision = getRevisionDefiningSharedLib(sharedLibName, dep, false);
        if (revision != null) {
          return revision;
        }
      }
    }
    return null;
  }
  
  
  /**
   * gibt die revision zurück, in der originalXmlName definiert ist, abhängig von den aktuellen dependencies von parentRevision 
   * 
   * oder null, falls keine revision gefunden, in der das objekt definiert ist.
   */
  // TODO eigenen cache aufbauen statt dism?
  public Long getRevisionDefiningXMOMObject(String originalXmlName, long parentRevision) {
    if (XynaFactory.getInstance().isStartingUp()) {
      return getRevisionDefiningXMOMObjectFileSystem(originalXmlName, parentRevision, true);
    } else {
      return getRevisionDefiningXMOMObjectDISM(originalXmlName, parentRevision, true);
    }
  }
  
  
  public long getRevisionDefiningXMOMObjectOrParent(String originalXmlName, long parentRevision) {
    Long l = getRevisionDefiningXMOMObject(originalXmlName, parentRevision);
    if (l == null) {
      return parentRevision;
    }
    return l;
  }
  
  
  public Set<Long> getAllRevisionsDefiningXMOMObject(String originalXmlName, long parentRevision) {
    if (XynaFactory.getInstance().isStartingUp()) {
      return getAllRevisionsDefiningXMOMObjectFileSystem(originalXmlName, parentRevision, true);
    } else {
      return getAllRevisionsDefiningXMOMObjectDISM(originalXmlName, parentRevision, true);
    }
  }
  
  
  private Set<Long> getAllRevisionsDefiningXMOMObjectFileSystem(String originalXmlName, Long parentRevision, boolean checkDependencies) {
    Set<Long> allRevisions = new HashSet<Long>();
    FilesInRevision fir = cacheForFileSystem.get(parentRevision);
    if (fir == null) {
      fir = new FilesInRevision(parentRevision);
      cacheForFileSystem.put(parentRevision, fir);
    }
    if (fir.fqXmlNames.contains(originalXmlName)) {
      allRevisions.add(parentRevision);
    }
    if (checkDependencies) {
      Set<Long> deps = new HashSet<Long>();
      getDependenciesRecursivly(parentRevision, deps);
      for (Long dep : deps) {
        Set<Long> revisions = getAllRevisionsDefiningXMOMObjectFileSystem(originalXmlName, dep, false);
        allRevisions.addAll(revisions);
      }
    }
    return allRevisions;
  }

  private Set<Long> getAllRevisionsDefiningXMOMObjectDISM(String originalXmlName, Long parentRevision, boolean checkDependencies) {
    Set<Long> allRevisions = new HashSet<Long>();
    DeploymentItemState dis = dism.get(originalXmlName, parentRevision);
    if (dis != null && dis.exists()) {
      allRevisions.add(parentRevision);
    }
    if (checkDependencies) {
      Set<Long> deps = new HashSet<Long>();
      getDependenciesRecursivly(parentRevision, deps);
      for (Long dep : deps) {
        allRevisions.addAll(getAllRevisionsDefiningXMOMObjectDISM(originalXmlName, dep, false));
      }
    }
    return allRevisions;
  }

  public boolean isReferenced(RuntimeDependencyContext runtimeContext) {
    for (Collection<RuntimeDependencyContext> depList : dependencies.values()) {
      if (depList.contains(runtimeContext)) {
        return true;
      }
    }
    
    return false;
  }
  
  public ChangeResult addDependency(RuntimeDependencyContext owner, RuntimeDependencyContext dependency, String user, boolean force) throws PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException {
    List<RuntimeDependencyContext> newDependencies = new ArrayList<RuntimeDependencyContext>(getDependencies(owner));
    if (newDependencies.contains(dependency)) {
      return ChangeResult.NoChange;
    }
    
    newDependencies.add(dependency);
    modifyDependencies(owner, newDependencies, user, force, true);
    
    return ChangeResult.Succeeded;
  }

  public ChangeResult removeDependency(RuntimeDependencyContext owner, RuntimeDependencyContext dependency, String user, boolean force) throws PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException {
    List<RuntimeDependencyContext> newDependencies = new ArrayList<RuntimeDependencyContext>(getDependencies(owner));
    if (!newDependencies.contains(dependency)) {
      return ChangeResult.NoChange;
    }
    
    newDependencies.remove(dependency);
    modifyDependencies(owner, newDependencies, user, force, true);
    
    return ChangeResult.Succeeded;
  }
  
  public ChangeResult removeDependency(RuntimeDependencyContext owner, RuntimeDependencyContext dependency, String user) throws PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException {
    return removeDependency(owner, dependency, user, false);
  }
  
  
  public void modifyDependencies(RuntimeDependencyContext owner, List<RuntimeDependencyContext> newDependencies, String user) throws PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException {
    modifyDependencies(owner, newDependencies, user, false, true);
  }


  public synchronized void modifyDependencies(RuntimeDependencyContext owner, Collection<RuntimeDependencyContext> newDependencies, String user, boolean force, boolean publishChanges) throws PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException {
    final RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try { // TODO duplication from below for lock constructor
      IRuntimeDependencyLock lock = null;
      if (hasRevision(owner)) {
        Long ownerRev = revisionManagement.getRevision(owner.asCorrespondingRuntimeContext());
        Set<Long> deps = new HashSet<Long>();
        
        ownerRev = revisionManagement.getRevision(owner.asCorrespondingRuntimeContext());
        for (RuntimeDependencyContext newDependency : newDependencies) {
          deps.add(revisionManagement.getRevision(newDependency.asCorrespondingRuntimeContext()));
        }
        Set<Long> previousDeps = revDependencies.getValues(ownerRev);
        if (previousDeps != null && previousDeps.equals(deps)) {
          //ntbd
          return;
        }
        Set<Long> removedRevisions = new HashSet<Long>();
        if (previousDeps != null) {
          for (Long previousDep : previousDeps) {
            if (!deps.contains(previousDep)) {
              removedRevisions.add(previousDep);
            }
          }
        }
        lock = new RuntimeDependencyLock(ownerRev, removedRevisions);
      }
      modifyDependencies(owner, newDependencies, user, force, publishChanges, lock);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_CouldNotModifyRuntimeContextDependenciesException(owner.toString(), e);
    }
  }
   
  
  // TODO handle modfiy with empty or null list as removal
  // synchronized to guard vs other modifies, rather use a lock?
  public synchronized void modifyDependencies(RuntimeDependencyContext owner, Collection<RuntimeDependencyContext> newDependencies, String user, boolean force, boolean publishChanges, IRuntimeDependencyLock lock) throws PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException {
    validateRequestedChanges(owner, newDependencies);
    if (logger.isDebugEnabled()) {
      logger.debug("modifydependencies for " + owner + ": new deps=" + newDependencies);
    }
    try {
      List<Exception> exceptionsDuringModification = new ArrayList<Exception>();
      List<RuntimeContext> runtimeContextsToPublishAsXMOMUpdate = null;
      if (hasRevision(owner)) {
        final RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        Long ownerRev = null;
        Set<Long> deps = new HashSet<Long>();
        
        ownerRev = revisionManagement.getRevision(owner.asCorrespondingRuntimeContext());
        for (RuntimeDependencyContext newDependency : newDependencies) {
          deps.add(revisionManagement.getRevision(newDependency.asCorrespondingRuntimeContext()));
        }
        
        Set<Long> parentRevs = getParentRevisionsRecursivly(ownerRev);
        parentRevs.add(ownerRev);
        Set<Long> previousDeps = revDependencies.getValues(ownerRev);
        if (previousDeps != null && previousDeps.equals(deps)) {
          //ntbd
          return;
        }
        
        ValidationResult validationResult =
            validateRuntimeContextChanges(Collections.singleton(new RuntimeContextDependencyChange(owner, newDependencies)), force)
                .iterator().next();

        Set<Long> removedRevisions = new HashSet<Long>();
        if (previousDeps != null) {
          for (Long previousDep : previousDeps) {
            if (!deps.contains(previousDep)) {
              removedRevisions.add(previousDep);
            }
          }
        }
        Set<Long> addedRevisions = new HashSet<Long>();
        if (previousDeps != null) {
          for (Long newDep : deps) {
            if (!previousDeps.contains(newDep)) {
              addedRevisions.add(newDep);
            }
          }
        } else {
          addedRevisions.addAll(deps);
        }
        
        for (RuntimeContextChangeHandler rdcch : getRtCtxMgmt().getHandlers()) {
          try {
            rdcch.dependencyChanges(owner, dependencies.get(owner), newDependencies);
          } catch (Throwable t) {
            logger.error("Could not execute RuntimeContextChangeHandler " + rdcch, t);
          }
        }
        
        Map<Long, Map<XMOMType, Collection<String>>> toRegenerate = validationResult.toRegenerate();
        
        lock.lock(workflowProtectionNecessary(ownerRev, deps));
        try {
          TriggerAndFilterHandling tfHandling = new TriggerAndFilterHandling(ownerRev, removedRevisions);
          tfHandling.handleTriggerAndFilter();
          try {
            
            List<ClassLoaderBase> classloaders = recreateClassLoaders(removedRevisions, addedRevisions, ownerRev, exceptionsDuringModification);          
            
            modifyDependenciesDirectly(owner, newDependencies);
            revDependencies.removeKey(ownerRev);
            for (Long dep : deps) {
              revDependencies.add(ownerRev, dep);
            }
            //XMOM Updates für den owner und alle Parents
            //(für Application Definitions muss kein XMOM Update geschickt werden)
            runtimeContextsToPublishAsXMOMUpdate = getParentRuntimeContextsSorted(owner);   
            
            updateDependencyRegister(toRegenerate, runtimeContextsToPublishAsXMOMUpdate, revisionManagement, removedRevisions);
            updateOrderTypeDependencies(ownerRev, removedRevisions, runtimeContextsToPublishAsXMOMUpdate);
            updateStorableStructureCache(ownerRev, removedRevisions);
  
            if (classloaders != null) {
              classLoaderDeployment(classloaders);
            }
            
            for (long removedRev : removedRevisions) {
              Set<Long> targetRevisions = new HashSet<>();
              for (long parentRev : parentRevs) {
                if (!isDependency(parentRev, removedRev)) {
                  targetRevisions.add(parentRev);
                }
              }
              if (!targetRevisions.isEmpty()) {
                ClassLoaderBase.cleanupBackuppedDependencies(removedRev, targetRevisions);
              }
            }
          
          } finally {
            tfHandling.unhandleTriggersAndFilters();
          }
       
        } finally {
          lock.unlock();
          exceptionsDuringModification.addAll(lock.getExceptionsAtUnlock());
        }
        
        DeploymentItemStateManagementImpl dism =
            (DeploymentItemStateManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();

        Map<Long, Set<DeploymentItemStateImpl>> callSites = new HashMap<>();
        for (Long parentRev : parentRevs) {
          callSites.put(parentRev, new HashSet<>());
        }
        // callsites invalidieren, deren revision nicht mehr die removed revision erreichen kann
        for (Long removedRev : removedRevisions) {
          DeploymentItemRegistry deploymentItemRegistry = (DeploymentItemRegistry) dism.getRegistry(removedRev);
          for (DeploymentItemState dis : deploymentItemRegistry.list()) {
            Set<DeploymentItemState> cs = new HashSet<>(dis.getInvocationSites(DeploymentLocation.DEPLOYED));
            cs.addAll(dis.getInvocationSites(DeploymentLocation.SAVED));
            for (DeploymentItemState caller : cs) {
              DeploymentItemStateImpl callerImpl = (DeploymentItemStateImpl) caller;
              Set<DeploymentItemStateImpl> set = callSites.get(callerImpl.getRevision());
              if (set != null) {
                set.add(callerImpl);
              }
            }
          }
          deploymentItemRegistry.invalidateCallSites();
        }

        // regenerate der objekte, für die sich der deploymentstatus geändert hat (zum guten oder schlechten)
        // this would deadlock with the lock
        for (Long revisionToRegenerate : toRegenerate.keySet()) {
          DeploymentItemRegistry dir = dism.getRegistry(revisionToRegenerate);
          for (Collection<String> fqNames : toRegenerate.get(revisionToRegenerate).values()) {
            for (String fqName : fqNames) {
              DeploymentItemState dis = dir.get(fqName);
              if (dis != null &&
                  dis instanceof DeploymentItemStateImpl) {
             // TODO regenerateDeployedAllFeatures appears to not execute the necessary handlers for this to be invoked on it's own?
                ((DeploymentItemStateImpl)dis).invalidateCache();
              }
            }
          }
        }
        for (Set<DeploymentItemStateImpl> s : callSites.values()) {
          for (DeploymentItemStateImpl d : s) {
            d.invalidateCache();
          }
        }
        
        StringBuilder comment = new StringBuilder("RuntimeDependencyContext Dependency Change of ");
        comment.append(owner).append(": Added Deps={");
        boolean first = true;
        for (Long added : addedRevisions) {
          if (first) {
            first = false;
          } else {
            comment.append(", ");
          }
          comment.append(revisionManagement.getRuntimeContext(added));
        }
        comment.append("}, Removed Deps={");
        first = true;
        for (Long removed : removedRevisions) {
          if (first) {
            first = false;
          } else {
            comment.append(", ");
          }
          comment.append(revisionManagement.getRuntimeContext(removed));
        }
        comment.append("}");
        
        if (logger.isDebugEnabled()) {
          logger.debug("deploying objects in revisions " + toRegenerate.keySet());
        }
        try {
          GenerationBase.deploy(toRegenerate, DeploymentMode.regenerateDeployedAllFeaturesCollectSpecialDeps, false, WorkflowProtectionMode.FORCE_DEPLOYMENT, comment.toString());
        } catch (MDMParallelDeploymentException e) {
          exceptionsDuringModification.add(e);
        } catch (XPRC_DeploymentDuringUndeploymentException e) {
          exceptionsDuringModification.add(e);
        } catch (XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH e) {
          exceptionsDuringModification.add(e);
        } catch (XPRC_InvalidPackageNameException e) {
          exceptionsDuringModification.add(e);
        }
        
        for (Long revisionToRegenerate : toRegenerate.keySet()) {
          DeploymentItemRegistry dir = dism.getRegistry(revisionToRegenerate);
          
          for (Collection<String> fqNames : toRegenerate.get(revisionToRegenerate).values()) {
            for (String fqName : fqNames) {
              DeploymentItemState dis = dir.get(fqName);
              if (dis != null &&
                  dis instanceof DeploymentItemStateImpl) {
                ((DeploymentItemStateImpl)dis).getStateReport();
              }
            }
          }
        }
        for (Set<DeploymentItemStateImpl> s : callSites.values()) {
          for (DeploymentItemStateImpl d : s) {
            d.getStateReport();
          }
        }
        
        //CodeAccess benachrichtigen, damit mdm.jars aktualisiert werden
        RepositoryEvent repositoryEvent = new SingleRepositoryEvent(ownerRev);
        repositoryEvent.addEvent(new ProjectCreationOrChangeProvider.RuntimeContextDependencyChangeEvent());

        //invalidate serialization for changed revisions (owner + parents)
        XynaFactory.getInstance().getProcessing().getXmomSerialization().invalidateRevisions(parentRevs);
        
        //invalidate pythonCodeSnippetManagement for changed revisions (owner + parents)
        XynaFactory.getInstance().getProcessing().getXynaPythonSnippetManagement().invalidateRevisions(parentRevs);
      } else { 
        if (owner instanceof ApplicationDefinition) {
          addDependenciesToParentWorkspace((ApplicationDefinition)owner, newDependencies, user, force, publishChanges);
        }
        //ApplicationDefinitions haben keine revision
        modifyDependenciesDirectly(owner, newDependencies);
        
        //bei Änderungen an Requirements einer ApplicationDefinition muss der ApplicationDefintion-Cache im ApplicationManagement geleert werden
        ApplicationManagementImpl appMgmt = (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getApplicationManagement();
        appMgmt.handleApplicationDefinitionDependencyChange((ApplicationDefinition)owner);
      }

      if (publishChanges) {
        //Multi-User-Event für Dependency Änderungen am owner
        Publisher publisher = new Publisher(user);
        publisher.publishRuntimeContextUpdate(owner);
        if (runtimeContextsToPublishAsXMOMUpdate != null) {
          for (RuntimeContext rc : runtimeContextsToPublishAsXMOMUpdate) {
            publisher.publishXMOMUpdate(rc.getGUIRepresentation());
          }
        }
      }
      
      if (exceptionsDuringModification.size() > 0) {
        //TODO weiterwerfen? ist ja eigtl nur eine warnung. optimalerweise an irgendeinen listener geben, der es z.b. beim client auf andere art und weise ausgibt
        logger.warn("Caught " + exceptionsDuringModification.size() + " Exceptions during RuntimeContextDependency Change:");
        for (Exception e : exceptionsDuringModification) {
          logger.warn(null, e);
        }
      }

    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_CouldNotModifyRuntimeContextDependenciesException(owner.toString(), e);
    } catch (XFMG_DependencyStillUsedByApplicationDefinitionException e) {
      throw new XFMG_CouldNotModifyRuntimeContextDependenciesException(owner.toString(), e);
    }   
  }
  
  
  public synchronized void modifyDependencies(Collection<RuntimeContextDependencyChange> changes, String user, boolean force, boolean publishChanges, IRuntimeDependencyLock lock) throws PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException {
    final RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    
    for (RuntimeContextDependencyChange change : changes) {
      validateRequestedChanges(change.getOwner(), change.getNewDependencies());
    }
    
    Pair<Collection<RuntimeContextDependencyChange>, Collection<RuntimeContextDependencyChange>> split =
                    seperateInRuntimeContextAndAppDefs(changes);
    Collection<RuntimeContextDependencyChange> runtimeContextChanges = split.getFirst();
    Collection<RuntimeContextDependencyChange> appDefChanges = split.getSecond();
    
    try {
      modifyAppDefDependencies(appDefChanges, user, force, publishChanges, lock);
      
      List<Exception> exceptionsDuringModification = new ArrayList<Exception>();
      
      Collection<ValidationResult> validationResults = validateRuntimeContextChanges(runtimeContextChanges, force);
      Set<Long> removedRevisions = new HashSet<>();
      Set<Long> parentRevisions = new HashSet<>();
      
      for (RuntimeContextDependencyChange runtimeContextChange : runtimeContextChanges) {
        runtimeContextChange.calculateRevStuff();
        removedRevisions.addAll(runtimeContextChange.removedRevisions);
        getParentRevisionsRecursivly(runtimeContextChange.ownerRev, parentRevisions);
        parentRevisions.add(runtimeContextChange.ownerRev);
      }
      
      Map<Long, Map<XMOMType, Collection<String>>> toRegenerate = collectRegenerates(validationResults);
      
      lock.lock(workflowProtectionNecessary(runtimeContextChanges));
      try {
        
        TriggerAndFilterHandling tfHandling = new TriggerAndFilterHandling(runtimeContextChanges);
        tfHandling.handleTriggerAndFilter();
        try {
          
          List<ClassLoaderBase> classloaders = new ArrayList<>();
          for (RuntimeContextDependencyChange runtimeContextChange : runtimeContextChanges) {
            List<ClassLoaderBase> classloadersTmp = recreateClassLoaders(runtimeContextChange.removedRevisions, runtimeContextChange.addedRevisions, runtimeContextChange.ownerRev, exceptionsDuringModification);
            if (classloadersTmp != null) {
              classloaders.addAll(classloadersTmp);
            }
          }
          for (RuntimeContextDependencyChange runtimeContextChange : runtimeContextChanges) {
            modifyDependenciesDirectly(runtimeContextChange.getOwner(), runtimeContextChange.getNewDependencies());
          }
          
          for (RuntimeContextDependencyChange runtimeContextChange : runtimeContextChanges) {
            revDependencies.removeKey(runtimeContextChange.ownerRev);
            for (Long dep : runtimeContextChange.deps) {
              revDependencies.add(runtimeContextChange.ownerRev, dep);
            }
          }
          
          for (RuntimeContextDependencyChange runtimeContextChange : runtimeContextChanges) {
            updateDependencyRegister(toRegenerate, getParentRuntimeContextsSorted(runtimeContextChange.owner), revisionManagement, runtimeContextChange.removedRevisions);
          }
          for (RuntimeContextDependencyChange runtimeContextChange : runtimeContextChanges) {
            updateOrderTypeDependencies(runtimeContextChange.ownerRev, runtimeContextChange.removedRevisions, getParentRuntimeContextsSorted(runtimeContextChange.owner));
          }
          for (RuntimeContextDependencyChange runtimeContextChange : runtimeContextChanges) {
            updateStorableStructureCache(runtimeContextChange.ownerRev, runtimeContextChange.removedRevisions);
          }

          if (classloaders != null) {
            classLoaderDeployment(classloaders);
          }

          for (long removedRev : removedRevisions) {
            Set<Long> targetRevisions = new HashSet<>();
            for (long parentRev : parentRevisions) {
              if (!isDependency(parentRev, removedRev)) {
                targetRevisions.add(parentRev);
              }
            }
            if (!targetRevisions.isEmpty()) {
              ClassLoaderBase.cleanupBackuppedDependencies(removedRev, targetRevisions);
            }
          }
          
        } finally {
          tfHandling.unhandleTriggersAndFilters();
        }
        
      } finally {
        lock.unlock();
        exceptionsDuringModification.addAll(lock.getExceptionsAtUnlock());
      }
      
      
      Map<Long, Set<DeploymentItemStateImpl>> callSites = new HashMap<>();
      for (Long parentRev : parentRevisions) {
        callSites.put(parentRev, new HashSet<>());
      }
      // callsites invalidieren, deren revision nicht mehr die removed revision erreichen kann
      for (Long removedRev : removedRevisions) {
        DeploymentItemRegistry deploymentItemRegistry = (DeploymentItemRegistry) dism.getRegistry(removedRev);
        for (DeploymentItemState dis : deploymentItemRegistry.list()) {
          Set<DeploymentItemState> cs = new HashSet<>(dis.getInvocationSites(DeploymentLocation.DEPLOYED));
          cs.addAll(dis.getInvocationSites(DeploymentLocation.SAVED));
          for (DeploymentItemState caller : cs) {
            DeploymentItemStateImpl callerImpl = (DeploymentItemStateImpl) caller;
            Set<DeploymentItemStateImpl> set = callSites.get(callerImpl.getRevision());
            if (set != null) {
              set.add(callerImpl);
            }
          }
        }
        deploymentItemRegistry.invalidateCallSites();
      }
      
      // regenerate der objekte, für die sich der deploymentstatus geändert hat (zum guten oder schlechten)
      // this would deadlock with the lock
      
      DeploymentItemStateManagementImpl dism =
                      (DeploymentItemStateManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
      for (Long revisionToRegenerate : toRegenerate.keySet()) {
        DeploymentItemRegistry dir = dism.getRegistry(revisionToRegenerate);
        
        for (Collection<String> fqNames : toRegenerate.get(revisionToRegenerate).values()) {
          for (String fqName : fqNames) {
            DeploymentItemState dis = dir.get(fqName);
            if (dis != null &&
                dis instanceof DeploymentItemStateImpl) {
              // TODO regenerateDeployedAllFeatures appears to not execute the necessary handlers for this to be invoked on it's own?
              ((DeploymentItemStateImpl)dis).invalidateCache();
            }
          }
        }
      }
      for (Set<DeploymentItemStateImpl> s : callSites.values()) {
        for (DeploymentItemStateImpl d : s) {
          d.invalidateCache();
        }
      }
      
      StringBuilder comment = new StringBuilder();
      for (RuntimeContextDependencyChange runtimeContextChange : runtimeContextChanges) {
        comment.append("RuntimeDependencyContext Dependency Change of ");
        comment.append(runtimeContextChange.getOwner()).append(": Added Deps={");
        boolean first = true;
        for (Long added : runtimeContextChange.addedRevisions) {
          if (first) {
            first = false;
          } else {
            comment.append(", ");
          }
          comment.append(revisionManagement.getRuntimeContext(added));
        }
        comment.append("}, Removed Deps={");
        first = true;
        for (Long removed : runtimeContextChange.removedRevisions) {
          if (first) {
            first = false;
          } else {
            comment.append(", ");
          }
          comment.append(revisionManagement.getRuntimeContext(removed));
        }
        comment.append("}");
        comment.append(Constants.LINE_SEPARATOR);
      }
      
      if (logger.isDebugEnabled()) {
        logger.debug("deploying objects in revisions " + toRegenerate.keySet());
      }
      try {
        GenerationBase.deploy(toRegenerate, DeploymentMode.regenerateDeployedAllFeaturesCollectSpecialDeps, false, WorkflowProtectionMode.FORCE_DEPLOYMENT, comment.toString());
      } catch (MDMParallelDeploymentException e) {
        exceptionsDuringModification.add(e);
      } catch (XPRC_DeploymentDuringUndeploymentException e) {
        exceptionsDuringModification.add(e);
      } catch (XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH e) {
        exceptionsDuringModification.add(e);
      } catch (XPRC_InvalidPackageNameException e) {
        exceptionsDuringModification.add(e);
      }
      /*
       * callsites in den neu hinzugefügten revision wieder herstellen
       * am einfachsten wäre: in allen parent-revisions statereports neu berechnen
       * das wäre aber auch teuer und macht potentiell viel arbeit die unnötig ist.
       * => wie kann man das reduzieren?
       * nur für alle invalidierten callsites in den removedrevisions den statereport neu erzeugen. (entweder sind diese objekte in der menge der "toRegenerate", oder
       * sie sind in den neuen addedrevisions auflösbar).
       * noch sparsamer wäre: die invalidierten callsites direkt in den addedrevisions eintragen ohne den statereport der betroffenen objekte
       * neu zu berechnen (für die objekte, die nicht in toRegenerate sind, d.h. ihr call muss ja weiterhin funktionieren).
       * aber das wäre fehleranfälliger - vielleicht später, falls von der performance her gewünscht.
       * 
       */
      for (Long revisionToRegenerate : toRegenerate.keySet()) {
        DeploymentItemRegistry dir = dism.getRegistry(revisionToRegenerate);
        
        for (Collection<String> fqNames : toRegenerate.get(revisionToRegenerate).values()) {
          for (String fqName : fqNames) {
            DeploymentItemState dis = dir.get(fqName);
            if (dis != null &&
                dis instanceof DeploymentItemStateImpl) {
              ((DeploymentItemStateImpl)dis).getStateReport();
            }
          }
        }
      }
      for (Set<DeploymentItemStateImpl> s : callSites.values()) {
        for (DeploymentItemStateImpl d : s) {
          d.getStateReport();
        }
      }
      
      for (RuntimeContextDependencyChange runtimeContextChange : runtimeContextChanges) {
        //CodeAccess benachrichtigen, damit mdm.jars aktualisiert werden
        RepositoryEvent repositoryEvent = new SingleRepositoryEvent(runtimeContextChange.ownerRev);
        repositoryEvent.addEvent(new ProjectCreationOrChangeProvider.RuntimeContextDependencyChangeEvent());
      }
      
      //invalidate serialization for changed revisions (owner + parents)
      XynaFactory.getInstance().getProcessing().getXmomSerialization().invalidateRevisions(parentRevisions);
      
      //invalidate pythonCodeSnippetManagement for changed revisions (owner + parents)
      XynaFactory.getInstance().getProcessing().getXynaPythonSnippetManagement().invalidateRevisions(parentRevisions);

        if (publishChanges) {
          for (RuntimeContextDependencyChange change : changes) {
          //Multi-User-Event für Dependency Änderungen am owner
            Publisher publisher = new Publisher(user);
            publisher.publishRuntimeContextUpdate(change.getOwner());
            for (RuntimeContext rc : getParentRuntimeContextsSorted(change.owner)) {
              publisher.publishXMOMUpdate(rc.getGUIRepresentation());
            }
          }
        }
        
        if (exceptionsDuringModification.size() > 0) {
          //TODO weiterwerfen? ist ja eigtl nur eine warnung. optimalerweise an irgendeinen listener geben, der es z.b. beim client auf andere art und weise ausgibt
          logger.warn("Caught " + exceptionsDuringModification.size() + " Exceptions during RuntimeContextDependency Change:");
          for (Exception e : exceptionsDuringModification) {
            logger.warn(null, e);
          }
        }
      
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_CouldNotModifyRuntimeContextDependenciesException("TODO", e);
    } catch (XFMG_DependencyStillUsedByApplicationDefinitionException e) {
      throw new XFMG_CouldNotModifyRuntimeContextDependenciesException("TODO", e);
    }
  }
  
  
  private Map<Long, Map<XMOMType, Collection<String>>> collectRegenerates(Collection<ValidationResult> validationResults) {
    Map<Long, Map<XMOMType, Collection<String>>> toRegenerate = new HashMap<>();
    for (ValidationResult validationResult : validationResults) {
      Map<Long, Map<XMOMType, Collection<String>>> result = validationResult.toRegenerate;
      for (Entry<Long, Map<XMOMType, Collection<String>>> resultEntry : result.entrySet()) {
        if (!toRegenerate.containsKey(resultEntry.getKey())) {
          toRegenerate.put(resultEntry.getKey(), resultEntry.getValue());
        } else {
          Map<XMOMType, Collection<String>> subMap = toRegenerate.get(resultEntry.getKey());
          for (Entry<XMOMType, Collection<String>> subMapEntry : subMap.entrySet()) {
            if (!subMap.containsKey(subMapEntry.getKey())) {
              subMap.put(subMapEntry.getKey(), subMapEntry.getValue());
            } else {
              subMap.get(subMapEntry.getKey()).addAll(subMapEntry.getValue());
            }
          }
          
        }
      }
    }
    return toRegenerate;
  }

  private Collection<ValidationResult> validateRuntimeContextChanges(Collection<RuntimeContextDependencyChange> changes, boolean force) throws XFMG_DependencyStillUsedByApplicationDefinitionException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    Collection<ValidationResult> results = new ArrayList<>();
    for (RuntimeContextDependencyChange change : changes) {
      ValidationResult validationResult = validate(change.getOwner(), change.getNewDependencies());
      if (!validationResult.success(force)) {
        throw new RuntimeContextValidationException(validationResult, force);
      }
      results.add(validationResult);
    }
    return results;
  }

  private void modifyAppDefDependencies(Collection<RuntimeContextDependencyChange> changes, String user, boolean force, boolean publishChanges, IRuntimeDependencyLock lock) throws PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    for (RuntimeContextDependencyChange change : changes) {
      addDependenciesToParentWorkspace((ApplicationDefinition)change.getOwner(), change.getNewDependencies(), user, force, publishChanges);
      
      //ApplicationDefinitions haben keine revision
      modifyDependenciesDirectly(change.getOwner(), change.getNewDependencies());
      
      //bei Änderungen an Requirements einer ApplicationDefinition muss der ApplicationDefintion-Cache im ApplicationManagement geleert werden
      ApplicationManagementImpl appMgmt = (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getApplicationManagement();
      appMgmt.handleApplicationDefinitionDependencyChange((ApplicationDefinition)change.getOwner());      
    }
  }
  
  
  private Pair<Collection<RuntimeContextDependencyChange>, Collection<RuntimeContextDependencyChange>> seperateInRuntimeContextAndAppDefs(Collection<RuntimeContextDependencyChange> changes) {
    Collection<RuntimeContextDependencyChange> appDefOwners = new ArrayList<>();
    Collection<RuntimeContextDependencyChange> others = new ArrayList<>();
    for (RuntimeContextDependencyChange change : changes) {
      if (hasRevision(change.getOwner())) {
        others.add(change);
      } else if (change.getOwner() instanceof ApplicationDefinition) {
        appDefOwners.add(change);
      } else {
        logger.debug(""); // TODO change has no rev & !appDef ...wtf?
      }
    }
    return Pair.of(others, appDefOwners);
  }


  public static class RuntimeContextDependencyChange {
    private final RuntimeDependencyContext owner;
    private final Collection<RuntimeDependencyContext> newDependencies;
    
    //transient nur als marker, dass die variablen temporär sind
    private /*transient*/ Long ownerRev;
    private /*transient*/ Set<Long> deps;
    private /*transient*/ Set<Long> previousDeps;
    private /*transient*/ Set<Long> removedRevisions;
    private /*transient*/ Set<Long> addedRevisions;
    
    public RuntimeContextDependencyChange(RuntimeDependencyContext owner, Collection<RuntimeDependencyContext> newDependencies) {
      this.owner = owner;
      this.newDependencies = newDependencies;
    }

    
    public RuntimeDependencyContext getOwner() {
      return owner;
    }

    
    public Collection<RuntimeDependencyContext> getNewDependencies() {
      return newDependencies;
    }
    
    private void calculateRevStuff() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      final RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      deps = new HashSet<Long>();
      
      ownerRev = revisionManagement.getRevision(owner.asCorrespondingRuntimeContext());
      for (RuntimeDependencyContext newDependency : newDependencies) {
        deps.add(revisionManagement.getRevision(newDependency.asCorrespondingRuntimeContext()));
      }
      
      RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                      .getRuntimeContextDependencyManagement();
      previousDeps = rcdm.getDependencies(ownerRev);
      removedRevisions = new HashSet<Long>();
      addedRevisions = new HashSet<Long>();
      if (previousDeps != null && previousDeps.equals(deps)) {
        //ntbd
        return;
      }
      
      if (previousDeps != null) {
        for (Long previousDep : previousDeps) {
          if (!deps.contains(previousDep)) {
            removedRevisions.add(previousDep);
          }
        }
      }
      if (previousDeps != null) {
        for (Long newDep : deps) {
          if (!previousDeps.contains(newDep)) {
            addedRevisions.add(newDep);
          }
        }
      } else {
        addedRevisions.addAll(deps);
      }
    }
  }
  
  
  private RuntimeContextManagement getRtCtxMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextManagement();
  }
  

  private void updateOrderTypeDependencies(Long ownerRev, Set<Long> removedRevisions,
                                           List<RuntimeContext> ownerAndParents) {
    
    Set<Long> ownerAndHigherRefs = new HashSet<Long>();
    for (RuntimeContext rtCtx : ownerAndParents) {
      try {
        Long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(rtCtx);
        ownerAndHigherRefs.add(rev);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.debug("Error while resolving revision",e);
      }
      
    }
    
    DependencyRegister depReg = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister();
    
    List<XynaDispatcher> dispatchers = new ArrayList<XynaDispatcher>();
    dispatchers.add(XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher());
    dispatchers.add(XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher());
    dispatchers.add(XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher());
    
    for (XynaDispatcher disp : dispatchers) {
      Set<DestinationKey> allCustomKeys = new HashSet<DestinationKey>();
      allCustomKeys.addAll(disp.getAllCustomDestinations());
      for (DestinationKey dk : allCustomKeys) {
        if (ownerAndParents.contains(dk.getRuntimeContext())) {
          DestinationValue dv = null;
          try {
            dv = disp.getDestination(dk);
          } catch (XPRC_DESTINATION_NOT_FOUND e) {
            logger.warn("Error while trying to resolve custom destination: " + dk,e);
            continue;
          }
          if (dv instanceof FractalWorkflowDestination) {
            Long rev = null;
            try {
              rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(dk.getRuntimeContext());
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              throw new RuntimeException(e);
            }
            
            Set<Long> currentlyReachableRevisions = dv.resolveAllRevisions(rev);
            Set<Long> depNodesExistInRevs = new HashSet<Long>();
            
            Set<DependencyNode> currentlyUsed = depReg.getAllUsedNodes(dk.getOrderType(), DependencySourceType.ORDERTYPE, false, false, rev);
            
            if (!disp.isPredefined(dk) && !disp.isPredefined(dv)) {
              for (DependencyNode depNode : currentlyUsed) {
                if (depNode.getUniqueName().equals(dv.getFQName())) {
                  if (!currentlyReachableRevisions.contains(depNode.getRevision())) {
                    depReg.removeDependency(DependencySourceType.WORKFLOW, depNode.getUniqueName(), depNode.getRevision(),
                                            DependencySourceType.ORDERTYPE, dk.getOrderType(), rev);
                  } else {
                    depNodesExistInRevs.add(depNode.getRevision());
                  }
                }
              }
              
              currentlyReachableRevisions.removeAll(depNodesExistInRevs);
              for (Long missingRev : currentlyReachableRevisions) {
                depReg.addDependency(DependencySourceType.WORKFLOW, dv.getFQName(), missingRev,
                                     DependencySourceType.ORDERTYPE, dk.getOrderType(), rev);
              }
            }
          }
        }
      }
    }
  }
  

  private void classLoaderDeployment(List<ClassLoaderBase> classLoaders) {
    ClassLoaderDispatcher dispatcher = ClassLoaderDispatcherFactory.getInstance().getImpl();
    dispatcher.deployClassLoaders(classLoaders);
  }

  private List<ClassLoaderBase> recreateClassLoaders(Set<Long> removedRevisions, Set<Long> addedRevisions, Long ownerRev, List<Exception> exceptionsDuringModification) {   
    if (removedRevisions.size() > 0) {
      Set<Long> removedRevisionsAndChildren = collectRevisionsNotReachableAnyMoreBeforeChange(ownerRev, removedRevisions, addedRevisions);
      logger.debug("recreating classloaders");
      ClassLoaderDispatcher dispatcher = ClassLoaderDispatcherFactory.getInstance().getImpl();
      Set<Long> revisionsToRecreate = getParentRevisionsRecursivly(ownerRev);
      revisionsToRecreate.add(ownerRev);
      try {
        return dispatcher.recreateClassLoadersWithoutRevisionDependencies(revisionsToRecreate, removedRevisionsAndChildren);
      } catch (Exception e) {
        exceptionsDuringModification.add(e);
      }
    }
    return null;
  }


  private void updateDependencyRegister(Map<Long, Map<XMOMType, Collection<String>>> toRegenerate,
                                        List<RuntimeContext> runtimeContextsToPublishAsXMOMUpdate, RevisionManagement revisionManagement,
                                        Set<Long> removedRevisions) {
    //dependencyregister aufräumen, evtl schlägt das unten stehende deployment ja fehl und kommt nicht bis zum deploymenthandler, der das auch tun würde
    DependencyRegister dr = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister();
    for (Entry<Long, Map<XMOMType, Collection<String>>> e1 : toRegenerate.entrySet()) {
      Long rev = e1.getKey();
      for (Entry<XMOMType, Collection<String>> e2 : e1.getValue().entrySet()) {
        XMOMType type = e2.getKey();
        for (String name : e2.getValue()) {
          if (type.equals(XMOMType.DATATYPE) || type.equals(XMOMType.EXCEPTION) || type.equals(XMOMType.WORKFLOW)) {
            DependencyNode dependencyNode = dr.getDependencyNode(name, DependencySourceType.from(type), rev);
            if (dependencyNode != null) {
              dr.removeMyUsedObjects(dependencyNode);
            } else if (logger.isDebugEnabled()) {
              logger.debug("Didnt find " + name + " in revision " + rev);
            }
          }
        }
      }
    }

    if (removedRevisions.size() > 0) {
      //die nicht neu generierten objekte, die dependencyregister einträge haben, müssen diese evtl neu erstellen, weil sie nun auf eine andere revision zeigen
      //es muss nach benutzten dr-einträgen gesucht werden, die auf eine der gelöschten revs zeigen (oder auf eine der children davon, die nicht mehr erreichbar sind)
      for (RuntimeContext parentRC : runtimeContextsToPublishAsXMOMUpdate) {
        Long rev;
        try {
          rev = revisionManagement.getRevision(parentRC);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e2) {
          //ignore, das muss oben drüber sicher gestellt werden, dass das hier nicht passiert
          continue;
        }
        Set<Long> removedRevisionsAndChildren = collectRevisionsNotReachableAnyMore(rev, removedRevisions);

        Map<XMOMType, Collection<String>> regenerated = toRegenerate.get(rev);
        for (Entry<XMOMType, Collection<String>> e : getAllObjects(rev).entrySet()) {
          XMOMType type = e.getKey();
          Collection<String> xmomObjects = e.getValue();
          Collection<String> regeneratedObjects = null;
          if (regenerated != null) {
            regeneratedObjects = regenerated.get(type);
          }
          for (String name : xmomObjects) {
            if (regeneratedObjects != null && regeneratedObjects.contains(name)) {
              //deployment hat die dependencyregistereinträge bereits neu angelegt
              continue;
            }
            if (type.equals(XMOMType.DATATYPE) || type.equals(XMOMType.EXCEPTION) || type.equals(XMOMType.WORKFLOW)) {
              DependencyNode dependencyNode = dr.getDependencyNode(name, DependencySourceType.from(type), rev);
              if (dependencyNode != null) {
                dr.updateRevisionInUsedNodes(dependencyNode, removedRevisionsAndChildren);
              }
            }
          }
        }
      }
    }
  }
  
  
  private void updateStorableStructureCache(Long ownerRev, Set<Long> removedRevisions) {
    if (removedRevisions.size() <= 0) {
      return;
    }
    Map<Long, Set<Long>> unreachableRevs = collectRevisionsNotReachableAnyMoreDetailed(ownerRev, removedRevisions);
    
    // for all storables from ownerRev & parentRevs
    //   check for usage of removedRevisions
    //   if found, add root object for storableStructureCache rebuild
       
    // we also have to fix the storablestructurecache of objects that are not reachable any more: they can 
    // contain references to the objects in ownerRev & parentRevs in the form of subtypeentries.
 
    Set<Long> allRevisions = getParentRevisionsRecursivly(ownerRev);
    allRevisions.add(ownerRev);
    Collection<XMOMStorableStructureInformation> allRootsToRebuild = new ArrayList<>();
    for (Long aRevision : allRevisions) {
      XMOMStorableStructureCache xssc = XMOMStorableStructureCache.getInstance(aRevision);
      Collection<XMOMStorableStructureInformation> infos = xssc.getAllStorableStructureInformation();
      for (XMOMStorableStructureInformation xssi : infos) {
        StructureCacheHandlerForUnreachableRevisions ruc = new StructureCacheHandlerForUnreachableRevisions(unreachableRevs);
        xssi.traverse(ruc); //also removes obsolete subtypeentries
        if (ruc.storableRefersToUnreachableRevisions()) {
          allRootsToRebuild.add(xssi);
        }
      }
    }
    
    try {
      StructureCacheRegistrator scr = new StructureCacheRegistrator();
      scr.begin();
      for (XMOMStorableStructureInformation aRoot : allRootsToRebuild) {
        DOM dom = DOM.generateUncachedInstance(aRoot.getFqXmlName(), true, aRoot.getDefiningRevision());
        scr.exec(dom, DeploymentMode.codeChanged);
      }
      scr.finish(true);
    } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException | 
             XPRC_MDMDeploymentException | XPRC_DeploymentHandlerException e) {
      throw new RuntimeException("Error while trying to update StorableStructureCache", e);
    }
    
 
    
  }
  
  private static class StructureCacheHandlerForUnreachableRevisions implements StorableStructureVisitor {
    
    private boolean atLeastOneHit = false;
    private final Map<Long, Set<Long>> unreachableRevisions;
    
    StructureCacheHandlerForUnreachableRevisions(Map<Long, Set<Long>> unreachableRevisions) {
      this.unreachableRevisions = unreachableRevisions;
    }

    public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
      Set<Long> unreachableFrom = unreachableRevisions.get(current.getRevision());
      if (unreachableFrom != null) {
        atLeastOneHit = true;
        if (current.getSubEntries() != null) {
          Iterator<StorableStructureIdentifier> it = current.getSubEntries().iterator();
          while (it.hasNext()) {
            StorableStructureIdentifier subentry = it.next();
            if (unreachableFrom.contains(subentry.getInfo().getRevision())) {
              it.remove(); //remove subentry that is not reachable any more
            }
          }
        }
      }
    }

    public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) {
    }

    public StorableStructureRecursionFilter getRecursionFilter() {
      return XMOMStorableStructureCache.ALL_RECURSIONS_AND_FULL_HIERARCHY;
    }
    
    public boolean storableRefersToUnreachableRevisions() {
      return atLeastOneHit;
    }
    
  }
  

  /**
   * s.u. gleichnamige methode. aber berechnung passiert, bevor die dependencies geändert worden sind
   */
  private Set<Long> collectRevisionsNotReachableAnyMoreBeforeChange(Long parentRev, Set<Long> removedRevisions, Set<Long> addedRevisions) {
    // returns {removedRevisions} U {children von removedRevisions} \ {children von parentRev nach änderung}
    Set<Long> result = new HashSet<>(removedRevisions);
    for (Long removed : removedRevisions) {
      getDependenciesRecursivly(removed, result); //added deps  
    }
    Set<Long> childrenOfParentRev = new HashSet<>();
    Set<Long> newDirectChildren = new HashSet<Long>(getDependencies(parentRev));
    newDirectChildren.addAll(addedRevisions);
    for (Long childOfParentRev : newDirectChildren) {
      if (!removedRevisions.contains(childOfParentRev)) {
        childrenOfParentRev.add(childOfParentRev);
        getDependenciesRecursivly(childOfParentRev, childrenOfParentRev);
      }
    }
    result.removeAll(childrenOfParentRev);
    return result;
  }
  
  /**
   * removedRevisions waren ursprünglich von parentRev erreichbar, aber jetzt nicht mehr.
   * ermittle alle revisions, die jetzt nicht mehr erreichbar sind. 
   */
  private Set<Long> collectRevisionsNotReachableAnyMore(Long parentRev, Set<Long> removedRevisions) {
    // returns {removedRevisions} U {children von removedRevisions} \ {children von parentRev}
    Set<Long> result = new HashSet<>(removedRevisions);
    for (Long removed : removedRevisions) {
      getDependenciesRecursivly(removed, result); //added deps  
    }
    Set<Long> childrenOfParentRev = new HashSet<>();
    getDependenciesRecursivly(parentRev, childrenOfParentRev);
    result.removeAll(childrenOfParentRev);
    return result;
  }


  /**
   * returns map with entries of sets of revisions (=value) that do not reach the unreachable revision (=key) any more
   *  
   * example:
   *  parentrev1 parentrev2
   *       \        /     \
   *           v            \
   *        ownerRev          \
   *       /          \         \
   *       v           v        |
   *  removedrev1  removedrev2  /
   *       |             \     /
   *       v               \  /
   *    removed2             v
   *                      childrev
   *                      
   * so in the above example the entries would look like this:
   *  removedrev1   -> {ownerRev, parentrev1, parentrev2}
   *  removed2      -> {ownerRev, parentrev1, parentrev2}
   *  removedrev2   -> {ownerRev, parentrev1, parentrev2}
   *  childrev      -> {ownerRev, parentrev1}
   */
  private Map<Long, Set<Long>> collectRevisionsNotReachableAnyMoreDetailed(Long parentRev, Set<Long> removedRevisions) {
    Set<Long> unreachables = new HashSet<>(removedRevisions);
    for (Long removed : removedRevisions) {
      getDependenciesRecursivly(removed, unreachables); //added deps  
    }
    Set<Long> parentRevs = new HashSet<>();
    getParentRevisionsRecursivly(parentRev, parentRevs);
    parentRevs.add(parentRev);
    Map<Long, Set<Long>> depsOfParents = new HashMap<>(); //cache to not calculate that repeatedly below
    for (Long pr : parentRevs) {
      Set<Long> deps = new HashSet<>();
      getDependenciesRecursivly(pr, deps);
      //deps.add(pr) not needed
      depsOfParents.put(pr, deps);
    }
    
    Map<Long, Set<Long>> map = new HashMap<>();
    for (Long unreachable : unreachables) {
      Set<Long> unreachableFrom = new HashSet<>();
      for (Entry<Long, Set<Long>> e : depsOfParents.entrySet()) {
        if (!e.getValue().contains(unreachable)) {
          unreachableFrom.add(e.getKey());
        }
      }
      map.put(unreachable, unreachableFrom);
    }
    return map;
  }
  
  private Map<XMOMType, Collection<String>> getAllObjects(Long rev) {
    DeploymentItemStateManagementImpl dism =
        (DeploymentItemStateManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
            .getDeploymentItemStateManagement();
    Map<XMOMType, Collection<String>> xmomEntries = new HashMap<XMOMType, Collection<String>>();
    DeploymentItemRegistry dir = dism.lazyCreateOrGet(rev);
    for (DeploymentItemState dis : dir.list()) {
      if (dis.exists()) {
        Collection<String> coll = xmomEntries.get(dis.getType());
        if (coll == null) {
          coll = new ArrayList<String>();
          xmomEntries.put(dis.getType(), coll);
        }
        coll.add(dis.getName());
      }
    }
    return xmomEntries;
  }

  private void validateRequestedChanges(RuntimeDependencyContext owner, Collection<RuntimeDependencyContext> newDependencies) {
    if (owner instanceof ApplicationDefinition) {
      for (RuntimeDependencyContext newDependency : newDependencies) {
        if (newDependency instanceof ApplicationDefinition) {
          if (!((ApplicationDefinition)owner).getParentWorkspace().equals(((ApplicationDefinition)newDependency).getParentWorkspace())) {
            // TODO
            throw new RuntimeException("ApplicationDefinitions can only require other ApplicationDefinitions if they are in the same workspace");
          }
        }
      }
    } else {
      for (RuntimeDependencyContext newDependency : newDependencies) {
        if (newDependency instanceof ApplicationDefinition) {
          // TODO
          throw new RuntimeException("Applications or Workspaces can not require ApplicationDefinitions");
        }
      }
    }
  }

  private synchronized void modifyDependenciesDirectly(RuntimeDependencyContext owner, Collection<RuntimeDependencyContext> newDependencies) throws PersistenceLayerException {
    storage.modifyDependencies(owner, newDependencies);
    if (newDependencies.size() > 0) {
      dependencies.put(owner, newDependencies);
    } else {
      dependencies.remove(owner);
    }
  }
  
  
  private void addDependenciesToParentWorkspace(ApplicationDefinition applicationDefinition, Collection<RuntimeDependencyContext> newDependencies, String user, boolean force, boolean publishChanges) throws PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException {
    Workspace parentWorkspace = applicationDefinition.getParentWorkspace();
    
    boolean atLeastOneChange = false;
    //die Abhängigkeiten der ApplicationDefinition zu den Workspace Abhängigkeiten hinzufügen (falls nicht bereits vorhanden)
    Set<RuntimeDependencyContext> newWorkspaceDeps = new HashSet<RuntimeDependencyContext>(getDependencies(parentWorkspace));
    for (RuntimeDependencyContext rc : newDependencies) {
      if (hasRevision(rc)) { //nur Abhängigkeiten zu anderen Workspaces und Applications vererben
        if (newWorkspaceDeps.add(rc)) {
          atLeastOneChange = true;
        }
      }
    }
    
    if (atLeastOneChange) {
      modifyDependencies(parentWorkspace, new ArrayList<RuntimeDependencyContext>(newWorkspaceDeps), user, force, publishChanges);
    }
  }
  

  // additive Änderungen ohne Sperrung zulassen
  private boolean workflowProtectionNecessary(Long ownerRev, Set<Long> newDependencies) {
    Set<Long> oldDeps = revDependencies.getValues(ownerRev);
    if (oldDeps != null) {
      for (Long oldDep : oldDeps) {
        if (!newDependencies.contains(oldDep)) {
          return true;
        }
      }
    }
    return false;
  }
  
  private boolean workflowProtectionNecessary(Collection<RuntimeContextDependencyChange> changes) {
    for (RuntimeContextDependencyChange change : changes) {
      if (workflowProtectionNecessary(change.ownerRev, change.deps)) {
        return true;
      }
    }
    return false;
  }
  
  
  private ValidationResult validate(RuntimeDependencyContext owner, Collection<RuntimeDependencyContext> newDependencies) throws XFMG_DependencyStillUsedByApplicationDefinitionException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    //falls Dependencies von Workspaces entfernt werden, dürfen sie nicht als Dependency bei
    //einer Application Definition definiert sein
    checkApplicationDefinitionDependencies(owner, newDependencies); //FIXME in problem umwandeln

    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();

    Collection<RuntimeContextProblem> oldProblems = revisionManagement.getRuntimeContextProblems(owner);
    RuntimeContextProblemParameter rcpp = RuntimeContextProblemParameter.simulatedChanges(owner, newDependencies);
    Collection<RuntimeContextProblem> changedProblems = revisionManagement.getRuntimeContextProblems(owner, rcpp);
    
    Collection<RuntimeContextProblem> newProblems = new HashSet<RuntimeContextProblem>();
    validateProblems: for (RuntimeContextProblem changedProblem : changedProblems) {
      for (RuntimeContextProblem oldProblem : oldProblems) {
        if (oldProblem.equals(changedProblem)) {
          continue validateProblems;
        }
      }
      newProblems.add(changedProblem);
    }
    
    // TODO gescheit nach draussen kommunizieren
    if (newProblems.size() > 0) {
      StringBuilder sb = new StringBuilder(newProblems.size() + " new problems detected!\n\n");
      for (RuntimeContextProblem newProblem : newProblems) {
        sb.append(newProblem.getMessage());
      }
    }
    
    Long ownerRev = revisionManagement.getRevision(owner.asCorrespondingRuntimeContext());
    Set<Long> newDeps = new HashSet<Long>();
    for (RuntimeDependencyContext newDependency : newDependencies) {
      newDeps.add(revisionManagement.getRevision(newDependency.asCorrespondingRuntimeContext()));
    }
    Map<Long, List<StateTransition>> stateTransitions = validateDeploymentItemStates(ownerRev, newDeps);
    return ValidationResult.from(stateTransitions, newProblems);
  }
  

  private Map<Long, List<StateTransition>> validateDeploymentItemStates(Long rootRev, Set<Long> newDependencies) {
    DeploymentItemStateManagement dism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    Set<Long> rootRevisions = new HashSet<Long>();
    rootRevisions = getParentRevisionsRecursivly(rootRev);
    rootRevisions.add(rootRev);
    Map<Long, List<StateTransition>> stateTransitions = new HashMap<Long, List<StateTransition>>();
    for (Long rootRevision : rootRevisions) {
      CrossRevisionResolver crossResolver = RevisionBasedCrossResolver.runtimeDependencyBackedResolver(rootRevision);
      Set<Long> changedDependencies = new HashSet<Long>();
      getDependenciesRecursivlyWithSimulatedChanges(rootRevision, changedDependencies, rootRev, newDependencies);
      changedDependencies.add(rootRevision);
      CrossRevisionResolver otherCrossResolver = RevisionBasedCrossResolver.customDependencyResolver(changedDependencies);
      List<StateTransition> transitions = dism.collectStateChangesBetweenResolvers(rootRevision, crossResolver, otherCrossResolver);
      stateTransitions.put(rootRevision, transitions);
    }
    return stateTransitions;
  }

/**
 * Überprüft ob eine von einem Workspace zu entfernende Abhängigkeit noch von einer 
 * ApplicationDefinition verwendet wird.
 * @param owner
 * @param newDependencies
 * @throws XFMG_DependencyStillUsedByApplicationDefinitionException falls eine zu entfernende Abhängigkeit noch verwendet wird
 */
  private void checkApplicationDefinitionDependencies(RuntimeDependencyContext owner, Collection<RuntimeDependencyContext> newDependencies) throws XFMG_DependencyStillUsedByApplicationDefinitionException {
    if (owner instanceof Workspace) {
      List<RuntimeDependencyContext> toDelete = new ArrayList<RuntimeDependencyContext>(getDependencies(owner));
      toDelete.removeAll(newDependencies);
      
      for (Entry<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> entry : dependencies.entrySet()) {
        if (entry.getKey() instanceof ApplicationDefinition) {
          if (((ApplicationDefinition)entry.getKey()).getParentWorkspace().equals(owner)) {
            for (RuntimeDependencyContext rc : toDelete) {
              if (entry.getValue().contains(rc)) {
                throw new XFMG_DependencyStillUsedByApplicationDefinitionException(rc.toString(), entry.getKey().toString());
              }
            }
          }
        }
      }
    }
  }

  /**
  * Entfernt den RuntimeDependencyContext mit allen seinen Dependencies.
  * @param runtimeContext
  * @throws PersistenceLayerException
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY 
  */
  public void deleteRuntimeContext(RuntimeDependencyContext runtimeContext) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    storage.deleteRuntimeContext(runtimeContext);
    dependencies.remove(runtimeContext);
    
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    if (hasRevision(runtimeContext)) { //ApplicationDefinitions haben keine revision
      Long revision = revMgmt.getRevision(runtimeContext.asCorrespondingRuntimeContext());
      revDependencies.removeKey(revision);
    }
  }
  
  private boolean hasRevision(RuntimeDependencyContext runtimeContext) {
    return runtimeContext instanceof RuntimeContext;
  }


  private enum HandlingOfRunningOrdersEnum {
    ABORT, MIGRATE, IGNORE;
  }


  private static final XynaPropertyEnum<HandlingOfRunningOrdersEnum> handlingOfRunningOrders =
      new XynaPropertyEnum<HandlingOfRunningOrdersEnum>("xfmg.xfctrl.rcdependencies.change.orders.active.behavior",
                                                        HandlingOfRunningOrdersEnum.class, HandlingOfRunningOrdersEnum.ABORT)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "Handling of active Xyna Orders when Runtime Context dependencies are changed. Possible values are ABORT (Abort change), IGNORE (Continue change), MIGRATE (Try migration of active orders - orders may get corrupted and have to be killed)");


  public interface IRuntimeDependencyLock {
    
    public void lock(boolean workflowProtectionNecessary);
    
    public void unlock();
    
    public List<XFMG_CouldNotStartApplication> getExceptionsAtUnlock();
    
  }
  
  private class RuntimeDependencyLock implements IRuntimeDependencyLock {

    private final Set<Long> revisions;
    private final Set<Long> removedDepsAndChildren;
    private Set<Application> appsToStart;
    
    private List<XFMG_CouldNotStartApplication> unlockExceptions = new ArrayList<XFMG_CouldNotStartApplication>();
    
    RuntimeDependencyLock(Long revision, Set<Long> removedDeps) {
      Set<Long> toLock = getParentRevisionsRecursivly(revision);
      toLock.add(revision);
      this.removedDepsAndChildren = new HashSet<Long>();
      for (Long l : removedDeps) {
        Set<Long> deps = new HashSet<Long>();
        getDependenciesRecursivly(l, deps);
        removedDepsAndChildren.addAll(deps);
        removedDepsAndChildren.add(l);
      }
      this.revisions = toLock;
      appsToStart = new HashSet<Application>();
    }
    
    /**
     * stoppt applications und trigger, um sie nach der dependency-änderung wieder zu starten
     * locking über DeploymentManagement
     */
    public void lock(boolean workflowProtectionNecessary) {
      if (workflowProtectionNecessary) {
        // close all order entrances
        RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        ApplicationManagementImpl appMgmt =
            (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl()
                .getApplicationManagement();
        for (Long revision : revisions) {
          RuntimeContext runtimeContext;
          try {
            runtimeContext = revMgmt.getRuntimeContext(revision);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            throw new RuntimeException(e);
          }
          if (runtimeContext instanceof Application) {
            Application app = (Application) runtimeContext;
            try {
              ApplicationState state;
              try {
                state = appMgmt.getApplicationState(app.getName(), app.getVersionName());
              } catch (PersistenceLayerException e) {
                throw new RuntimeException(e);
              }
              if (state == ApplicationState.RUNNING) {
                // clusterwide?
                appMgmt.stopApplication(app.getName(), app.getVersionName(), true);
                appsToStart.add(app);
              }
            } catch (XFMG_CouldNotStopApplication e) {
              throw new RuntimeException(e); //FIXME vorher bereits gestoppte applications wieder starten!
            }
          } else {
            // lock Workspace globally allthough the current changes could only affect a single applicationDefinition with a couple of filters?
            //   --> do not lock workspace orderentrances atm, we'll lock implicitly with DeploymentMgmt
          }
        }
      }
      
      if (workflowProtectionNecessary) {
        // DeploymentManagement.addDeployment with all workflows from this and all parentRevisions
        Set<WorkflowRevision> affectedWorkflows = new HashSet<WorkflowRevision>();
        Map<Long, List<String>> deployedWFs =
            XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase().getDeployedWfs();
        try {
          for (Long revision : deployedWFs.keySet()) {
            if (revisions.contains(revision)) {
              for (String wfName : deployedWFs.get(revision)) {
                affectedWorkflows.add(new WorkflowRevision(GenerationBase.transformNameForJava(wfName), revision));
              }
            }
          }
        } catch (XPRC_InvalidPackageNameException e) {
          throw new RuntimeException(e);
        }
        HandlingOfRunningOrdersEnum handlingOfRunningOrdersEnum = handlingOfRunningOrders.get();
        if (handlingOfRunningOrdersEnum != HandlingOfRunningOrdersEnum.IGNORE) {
          try {
            DeploymentManagement
                .getInstance()
                .addDeployment(affectedWorkflows,
                               handlingOfRunningOrdersEnum == HandlingOfRunningOrdersEnum.ABORT ? WorkflowProtectionMode.BREAK_ON_USAGE : WorkflowProtectionMode.FORCE_DEPLOYMENT);
          } catch (XPRC_WorkflowProtectionModeViolationException e) {
            throw new RuntimeException(e);
          }
        }
      }
      // protect vs app modifications
    }
    
    
    public void unlock() {
      // orderReloadingFromDeploymentManagementRunningDuringRCChange.set(new Object());
       try {
         DeploymentManagement.getInstance().cleanupIfLast();
       } finally {
       //  orderReloadingFromDeploymentManagementRunningDuringRCChange.remove();
         
         ApplicationManagement appMgmt = XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getApplicationManagement();
         for (Application app : appsToStart) {
           // TODO forced, clusterwide?
           try {
             appMgmt.startApplication(app.getName(), app.getVersionName(), true, true);
           } catch (XFMG_CouldNotStartApplication e) {
             unlockExceptions.add(e);
           }
         }
       }
     }


    public List<XFMG_CouldNotStartApplication> getExceptionsAtUnlock() {
      return unlockExceptions;
    }
    
  }
  
  private class TriggerAndFilterHandling {
    
    private final Set<Long> revisions;
    private final Set<Long> removedDepsAndChildren;
    
    private List<RevisionOrderControl> rocList = new ArrayList<RevisionOrderControl>();
    private List<FilterInstanceInformation> filterInstancessWithoutTriggerInstance = new ArrayList<FilterInstanceInformation>();
    private List<FilterInstanceInformation> filterInstancessWithoutFilter = new ArrayList<FilterInstanceInformation>();
    private List<FilterInformation> filtersWithoutTrigger = new ArrayList<FilterInformation>();
    private List<TriggerInstanceInformation> triggerInstancessWithoutTrigger = new ArrayList<TriggerInstanceInformation>();
    
    private TriggerAndFilterHandling(Long revision, Set<Long> removedDeps) {
      Set<Long> toLock = getParentRevisionsRecursivly(revision);
      toLock.add(revision);
      this.revisions = toLock;
      this.removedDepsAndChildren = new HashSet<Long>();
      for (Long l : removedDeps) {
        Set<Long> deps = new HashSet<Long>();
        getDependenciesRecursivly(l, deps);
        removedDepsAndChildren.addAll(deps);
        removedDepsAndChildren.add(l);
      }
    }
    
    
    private TriggerAndFilterHandling(Collection<RuntimeContextDependencyChange> changes) {
      Set<Long> toLock = new HashSet<>();
      for (RuntimeContextDependencyChange change : changes) {
        toLock.addAll(getParentRevisionsRecursivly(change.ownerRev));
        toLock.add(change.ownerRev);
      }
      this.revisions = toLock;
      this.removedDepsAndChildren = new HashSet<Long>();
      for (RuntimeContextDependencyChange change : changes) {
        for (Long l : change.removedRevisions) {
          Set<Long> deps = new HashSet<Long>();
          getDependenciesRecursivly(l, deps);
          removedDepsAndChildren.addAll(deps);
          removedDepsAndChildren.add(l);
        }
      }
    }
    
    /**
     * sammelt 
     * - trigger instanzen, die in kind-revisionen sind, die wegen dem change neu gestartet werden müssen, damit entweder filter 
     *   entfernt oder hinzugefügt werden.
     *   - diese werden gestopped.
     * - triggerinstanzen, filter, filterinstanzen in den parentrevisionen (und eigene), die derzeit nicht funktionieren, weil 
     *   sie eine abhängigkeit vermissen und nach dem change dann funktionieren könnten.
     *   
     * TODO: objekte, die durch das entfernen von dependencies ungültig werden, sollten nicht nur laufzeitmässig deregistriert 
     *       werden (passiert durch den oberen ersten punkt), sondern auch in den status ERROR wechseln
     * 
     * beim unlock werden die gesammelten objekte dann behandelt.
     */
    private void handleTriggerAndFilter() {
      XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
      try {
        //[0] -> alle objekte (um blacklist zu erzeugen)
        //[1] -> die objekte, die eigtl entfernt werden müssen (also whitelist)
        Map<Long, RevisionContentBlackWhiteListBean[]> objectsToReloadMap = new HashMap<Long, RevisionContentBlackWhiteListBean[]>();
        for (Long rev : revisions) {
          RevisionContentBlackWhiteListBean[] objectsToReload = objectsToReloadMap.get(rev);
          if (objectsToReload == null) {
            objectsToReload =
                new RevisionContentBlackWhiteListBean[] {new RevisionContentBlackWhiteListBean(), new RevisionContentBlackWhiteListBean()};
            objectsToReloadMap.put(rev, objectsToReload);
          }
          for (FilterInstanceInformation fii : xat.getFilterInstanceInformations(rev)) {
            if (fii.getState() == FilterInstanceState.DISABLED) {
              continue;
            }
            TriggerInstanceInformation tii = xat.getTriggerInstanceInformation(fii.getTriggerInstanceName(), fii.getRevision(), true);
            if (tii == null) {
              filterInstancessWithoutTriggerInstance.add(fii);
              continue;
            }
            try {
              xat.getFilterInformation(fii.getFilterName(), fii.getRevision(), true);
            } catch (XACT_FilterNotFound e) {
              filterInstancessWithoutFilter.add(fii);
              continue;
            }
            if (fii.getState() != FilterInstanceState.ENABLED) {
              continue;
            }
            if (!filterInstanceIsDependentOnRemovedRevs(fii)) {
              objectsToReload[0].addFilterInstance(fii.getFilterInstanceName());
              continue;
            }
            if (tii.getState() != TriggerInstanceState.ENABLED) {
              continue;
            }
            //triggerinstanz ist in kindrevision, aber filter in parentrevision
            //triggerinstanz ganz anhalten. achtung, die ist evtl in anderer revision
            RevisionContentBlackWhiteListBean[] objectsToReloadTii = objectsToReloadMap.get(tii.getRevision());
            if (objectsToReloadTii == null) {
              objectsToReloadTii =
                  new RevisionContentBlackWhiteListBean[] {new RevisionContentBlackWhiteListBean(), new RevisionContentBlackWhiteListBean()};
              objectsToReloadMap.put(tii.getRevision(), objectsToReloadTii);
            }
            objectsToReloadTii[1].addTriggerInstance(tii.getTriggerInstanceName());
            objectsToReload[1].addFilterInstance(fii.getFilterInstanceName());
          }

          for (TriggerInstanceInformation tii : xat.getTriggerInstanceInformation(rev)) {
            if (tii.getState() == TriggerInstanceState.DISABLED) {
              continue;
            }
            try {
              xat.getTriggerInformation(tii.getTriggerName(), tii.getRevision(), true);
            } catch (XACT_TriggerNotFound e) {
              triggerInstancessWithoutTrigger.add(tii);
            }
          }
          
          for (Filter filter : xat.getFilters(rev)) {
            FilterInformation fi;
            try {
              fi = xat.getFilterInformation(filter.getName(), rev, false);
            } catch (XACT_FilterNotFound e) {
              continue;
            }
            try {
              xat.getTriggerInformation(fi.getTriggerName(), rev, true);
            } catch (XACT_TriggerNotFound e) {
              filtersWithoutTrigger.add(fi);
            }
          }
        }

        for (Entry<Long, RevisionContentBlackWhiteListBean[]> entry : objectsToReloadMap.entrySet()) {
          RevisionOrderControl roc = new RevisionOrderControl(entry.getKey());
          try {
            RevisionContentBlackWhiteListBean blacklist = entry.getValue()[0];
            for (String triggerInstanceOfWhiteList : entry.getValue()[1].getTriggerInstanceNames()) {
              blacklist.getTriggerInstanceNames().remove(triggerInstanceOfWhiteList);
            }
            for (String filterInstanceOfWhiteList : entry.getValue()[1].getFilterInstanceNames()) {
              blacklist.getFilterInstanceNames().remove(filterInstanceOfWhiteList);
            }
            roc.disableTriggerInstances(blacklist, true);
            rocList.add(roc);
          } catch (OrderEntryInterfacesCouldNotBeClosedException e) {
            logger.warn(null, e); //TODO werfen + compensation?
          }
        }
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
    }
    
    
    private void unhandleTriggersAndFilters() {
      for (RevisionOrderControl roc : rocList) {
        roc.enablePreviouslyDisabledTriggerInstances();
      }

      //eventuell sind durch den dependencychange nun filterinstanzen/triggerinstanzen neu dazugekommen, die vorher nicht gestartet waren (aber ENABLED)
      XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      try {
        for (FilterInformation fi : filtersWithoutTrigger) {
          Long rev;
          try {
            rev = rm.getRevision(fi.getRuntimeContext());
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
            continue;
          }
          try {
            xat.getTriggerInformation(fi.getTriggerName(), rev, true);
            try {
              xat.reAddExistingFilterWithExistingParameters(GenerationBase.getSimpleNameFromFQName(fi.getFqFilterClassName()), rev);
            } catch (Exception e) {
              logger.info("failed to add filter " + fi.getFilterName() + " in " + fi.getRuntimeContext());
            }
          } catch (XACT_TriggerNotFound e) {
            //ok, dann halt nicht
          }
        }
        for (FilterInstanceInformation fii : filterInstancessWithoutFilter) {
          try {
            xat.getFilterInformation(fii.getFilterName(), fii.getRevision(), true);
            tryActivateFilterInstance(fii);
          } catch (XACT_FilterNotFound e) {
            //ok, dann halt nicht
          }
        }
        for (FilterInstanceInformation fii : filterInstancessWithoutTriggerInstance) {
          TriggerInstanceInformation tii = xat.getTriggerInstanceInformation(fii.getTriggerInstanceName(), fii.getRevision(), true);
          if (tii != null) {
            tryActivateFilterInstance(fii);
          }
        }
        for (TriggerInstanceInformation tii : triggerInstancessWithoutTrigger) {
          try {
            xat.getTriggerInformation(tii.getTriggerName(), tii.getRevision(), true);
            tryActivateTriggerInstance(tii);
          } catch (XACT_TriggerNotFound e) {
            //ok, dann halt nicht
          }
        }
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
    }
    
    
    private boolean filterInstanceIsDependentOnRemovedRevs(FilterInstanceInformation fii) throws PersistenceLayerException {
      XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      /*
       * checken, ob zugehöriger trigger in zu entfernenden revisions (inkl dependencies) enthalten ist.
       * es genügt, dies für den trigger zu checken, weil triggerinstanz+filter auch den trigger verwenden.
       * 
       * falls ja, ist die filterinstance betroffen, und muss neu geladen werden (inkl zugehöriger triggerinstance)
       * betroffenheitsarten (austausch oder entfernung. falls die dependency nur entfernt wird, ist es immer richtig, die objekte zu disablen):
       * - trigger austausch -> neue klasse von triggerinstanz notwendig
       * - filter austausch -> neue klasse von filterinstanz notwendig
       * - triggerinstanz austausch -> filterinstanz deregistrieren
       * alle diese fälle sind durch disable+enable gut abgehandelt.
       */

      FilterInformation fi;
      try {
        try {
          fi = xat.getFilterInformation(fii.getFilterName(), fii.getRevision(), true);
        } catch (XACT_FilterNotFound e) {
          //ok, evtl wird das ja durch das ändern der dependencies gefixt.
          return true;
        }

        if (removedDepsAndChildren.contains(rm.getRevision(fi.getRuntimeContext()))) {
          return true;
        }

        TriggerInformation ti;
        try {
          ti = xat.getTriggerInformation(fi.getTriggerName(), fii.getRevision(), true);
        } catch (XACT_TriggerNotFound e) {
          //ok, evtl wird das ja durch das ändern der dependencies gefixt.
          return true;
        }
        if (removedDepsAndChildren.contains(rm.getRevision(ti.getRuntimeContext()))) {
          return true;
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      return false;
    }


    private void tryActivateTriggerInstance(TriggerInstanceInformation tii) {
      XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
      try {
        xat.disableTriggerInstance(tii.getTriggerInstanceName(), tii.getRevision());
        xat.enableTriggerInstance(tii.getTriggerInstanceName(), tii.getRevision(), true);
      } catch (Exception e) {
        RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        String rc;
        try {
          rc = rm.getRuntimeContext(tii.getRevision()).toString();
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
          rc = "" + tii.getRevision();
        }
        logger.info("could not enable trigger instance " + tii.getTriggerInstanceName() + " in " + rc + " with new dependencies");
      }
    }


    private void tryActivateFilterInstance(FilterInstanceInformation fii) {
      XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();

      try {
        xat.disableFilterInstance(fii.getFilterInstanceName(), fii.getRevision());
        xat.enableFilterInstance(fii.getFilterInstanceName(), fii.getRevision());
      } catch (Exception e) {
        RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        String rc;
        try {
          rc = rm.getRuntimeContext(fii.getRevision()).toString();
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
          rc = "" + fii.getRevision();
        }
        logger.info("could not enable filter instance " + fii.getFilterInstanceName() + " in " + rc + " with new dependencies");
      }
    }
  }
  
  //wird derzeit nicht verwendet, soll später bei der migration laufender aufträge helfen
  private static final ThreadLocal<Object> orderReloadingFromDeploymentManagementRunningDuringRCChange = new ThreadLocal<Object>() {
    @Override
    protected Integer initialValue() {
      return null;
    }
  };
  
  
  public static boolean getThreadLocalValueForDeserializationWithNewRevisions() {
    //hässlich, dass man immer threadlocals anlegt und wieder removen muss. threadlocal benötigt einen getter, der readonly ist.
    if (orderReloadingFromDeploymentManagementRunningDuringRCChange.get() == null) {
      orderReloadingFromDeploymentManagementRunningDuringRCChange.remove();
      return false;
    }
    return true;
  }
  
  
  private static class ValidationResult {
    
    private static final Logger logger = CentralFactoryLogging.getLogger(ValidationResult.class);
    private Map<Long, Map<XMOMType, Collection<String>>> toRegenerate;
    private Map<Long, Map<XMOMType, Collection<String>>> invalidAfterChanges;
    private Collection<RuntimeContextProblem> problems;
    
    private ValidationResult(Map<Long, Map<XMOMType, Collection<String>>> toRegenerate,
                             Map<Long, Map<XMOMType, Collection<String>>> invalidAfterChanges,
                             Collection<RuntimeContextProblem> problems) {
      this.toRegenerate = toRegenerate;
      this.invalidAfterChanges = invalidAfterChanges;
      this.problems = problems;
    }
    
    
    //sortiere statetransitions in die, für die der state sich ändert, und in die, wo objekte invalide werden
    public static ValidationResult from(Map<Long, List<StateTransition>> stateTransitions, Collection<RuntimeContextProblem> problems) {
      Map<Long, Map<XMOMType, Collection<String>>> toRegenerate = new HashMap<Long, Map<XMOMType, Collection<String>>>();
      Map<Long, Map<XMOMType, Collection<String>>> invalidAfterChanges = new HashMap<Long, Map<XMOMType, Collection<String>>>();
      for (Entry<Long, List<StateTransition>> revision : stateTransitions.entrySet()) {
        for (StateTransition transition : revision.getValue()) {
          if (transition.turnedInvalid()) {
            addTo(invalidAfterChanges, revision.getKey(), transition.getType(), transition.getFqName());
          }
          if (transition.stateChanged()) {
            if (logger.isDebugEnabled()) {
              logger.debug(transition.getType().getNiceName() + " " + transition.getFqName()
                  + " will change state from " + transition.getFromState() + " to " + transition.getToState());
              if (transition.turnedInvalid()) {
                logger.debug("New inconsistencies:");
                for (Inconsistency i : transition.getInconsistenciesTo()) {
                  logger.debug(i.toFriendlyString());
                }
              } else if (transition.turnedValid()) {
                logger.debug("Removed inconsistencies:");
                for (Inconsistency i : transition.getInconsistenciesFrom()) {
                  logger.debug(i.toFriendlyString());
                }
              }
            }
            addTo(toRegenerate, revision.getKey(), transition.getType(), transition.getFqName());
          }
        }
      }
      return new ValidationResult(toRegenerate, invalidAfterChanges, problems);
    }
    
    private static void addTo(Map<Long, Map<XMOMType, Collection<String>>> map, Long revision, XMOMType type, String fqName) {
      Map<XMOMType, Collection<String>> revisionSubMap = map.get(revision);
      if (revisionSubMap == null) {
        revisionSubMap = new HashMap<XMOMType, Collection<String>>();
        map.put(revision, revisionSubMap);
      }
      Collection<String> xmomSubSet = revisionSubMap.get(type);
      if (xmomSubSet == null) {
        xmomSubSet = new HashSet<String>();
        revisionSubMap.put(type, xmomSubSet);
      }
      xmomSubSet.add(fqName);
    }

    
    public boolean success(boolean force) {
      if (!force && invalidAfterChanges.size() > 0) {
        return false;
      }
      for (RuntimeContextProblem rtCtxProblem : problems) {
        if (rtCtxProblem.causeValidationError()) {
          return false;
        } else if (!force && rtCtxProblem.causeErrorStatus()) {
          return false;
        }
      }
      return true;
      
    }
    
    
    public Map<Long, Map<XMOMType, Collection<String>>> toRegenerate() {
      return toRegenerate;
    }
    
  }

  public static enum ChangeResult {
    Succeeded, NoChange, Failed;
  }

  public static RuntimeDependencyContext asRuntimeDependencyContext(RuntimeContext rc) {
    if (rc instanceof Workspace) {
      return (Workspace)rc;
    } else if (rc instanceof Application) {
      return (Application)rc;
    } else {
      throw new IllegalArgumentException("Not a RuntimeDependencyContext: " + rc);
    }
  }

  public static Collection<RuntimeContext> asRuntimeDependencyContext(Collection<RuntimeDependencyContext> rdcs) {
    Set<RuntimeContext> rcs = new HashSet<RuntimeContext>();
    for (RuntimeDependencyContext rdc : rdcs) {
      rcs.add(rdc.asCorrespondingRuntimeContext());
    }
    return rcs;
  }

  public static RuntimeDependencyContext getRuntimeDependencyContext(String applicationName,
                                                                     String versionName,
                                                                     String workspaceName) {
    if (applicationName != null) {
      if (workspaceName != null) {
        return new ApplicationDefinition(applicationName, new Workspace(workspaceName));
      } else {
        if (versionName != null) {
          return new Application(applicationName, versionName);
        } else {
          throw new RuntimeException("Application version missing for application " + applicationName);
        }
      }
    }
    
    if (workspaceName != null) {
      return new Workspace(workspaceName);
    }
    
    return null;
  }

  
  public static class RuntimeContextValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private ValidationResult validationResult;
    private boolean force;


    public RuntimeContextValidationException(ValidationResult validationResult, boolean force) {
      super(createMessage(validationResult, force, false));
      this.validationResult = validationResult;
      this.force = force;
    }


    private static String createMessage(ValidationResult validationResult, boolean force, boolean extended) {
      StringBuilder sb = new StringBuilder("Validationerror: Could not change dependencies.");
      if (validationResult.invalidAfterChanges.size() > 0 && !force) {
        if (!extended) {
          sb.append("\nThere will be " + validationResult.invalidAfterChanges.size() + " invalid Objects after this change");
        } else {
          sb.append("\nThere will be " + validationResult.invalidAfterChanges.size() + " runtime contexts containing invalid objects after this change");
        }
        if (extended) {
          sb.append(":");
          for (Entry<Long, Map<XMOMType, Collection<String>>> entry : validationResult.invalidAfterChanges.entrySet()) {
            String rtcString;
            try {
              rtcString = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                  .getRuntimeContext(entry.getKey()).getGUIRepresentation();
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              rtcString = "rev_" + entry.getKey();
            }
            int linesThisRTC = 0;
            for (Entry<XMOMType, Collection<String>> xmomentry : entry.getValue().entrySet()) {
              String xmomTypeName = xmomentry.getKey().toString();
              for (String fqn : xmomentry.getValue()) {
                sb.append("\n  ");
                sb.append(rtcString);
                sb.append(" - (");
                sb.append(xmomTypeName);
                sb.append(") ");
                sb.append(fqn);
                linesThisRTC++;
                if (linesThisRTC >= 10) {
                  if (xmomentry.getValue().size() > 10) {
                    sb.append("\n  ... There will be " + xmomentry.getValue().size() + " invalid objects in total.");
                  }
                  break;
                }
              }
            }
          }
          
          
        } else {
          sb.append(".");
        }
      }
      for (RuntimeContextProblem rtCtxProblem : validationResult.problems) {
        if (rtCtxProblem.causeValidationError()) {
          sb.append("\nFatal Error: " + rtCtxProblem.getMessage() + ".");
        } else if (!force && rtCtxProblem.causeErrorStatus()) {
          sb.append("\nProblem: " + rtCtxProblem.getMessage());
        }
      }
      return sb.toString();
    }
    
    public String createExtendedMessage() {
      return createMessage(validationResult, force, true);
    }


    public ValidationResult getValidationResult() {
      return validationResult;
    }

  }


}

