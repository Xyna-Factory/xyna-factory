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
package xmcp.gitintegration.impl;


import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.SharedLib;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import xmcp.gitintegration.Reference;
import xmcp.gitintegration.ReferenceData;
import xmcp.gitintegration.RemoveReferenceData;
import xmcp.gitintegration.RepositoryManagement;
import xmcp.gitintegration.cli.generated.OverallInformationProvider;
import xmcp.gitintegration.impl.processing.ReferenceSupport;
import xmcp.gitintegration.impl.references.InternalReference;
import xmcp.gitintegration.impl.references.ReferenceType;
import xmcp.gitintegration.repository.RepositoryConnection;
import xmcp.gitintegration.storage.ReferenceStorable;
import xmcp.gitintegration.storage.ReferenceStorage;
import xmcp.gitintegration.ui.IndexedObjectReference;
import xmcp.gitintegration.ui.IndexedReference;
import xprc.xpce.Workspace;
import xmcp.gitintegration.ReferenceManagementServiceOperation;
import xmcp.gitintegration.ReferenceMethodType;


public class ReferenceManagementServiceOperationImpl implements ExtendedDeploymentTask, ReferenceManagementServiceOperation {

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    ReferenceStorage.init();
    OverallInformationProvider.onDeployment();
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    OverallInformationProvider.onUndeployment();
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public void addReference(ReferenceData referenceData3) {
    ReferenceInteraction impl = new ReferenceInteraction();
    Long workspaceRevision = getRevision(referenceData3.getWorkspaceName());
    impl.create(referenceData3.getPath(), referenceData3.getObjectType(), referenceData3.getReferenceType(), workspaceRevision,
                   referenceData3.getObjectName());
  }


  public List<? extends ReferenceData> listReferences(Workspace workspace2) {
    ReferenceStorage storage = new ReferenceStorage();
    String workspaceName = workspace2.getName();
    List<ReferenceStorable> references = null;
    if (workspaceName == null) {
      references = storage.getAllReferences();
    } else {
      Long revision = getRevision(workspaceName);
      references = storage.getAllReferencesForWorkspace(revision);
    }
    return references.stream().map(this::convert).collect(Collectors.toList());
  }
  
  private ReferenceData convert(ReferenceStorable storable) {
    ReferenceData.Builder builder = new ReferenceData.Builder();
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    builder.objectName(storable.getObjectName());
    builder.objectType(storable.getObjecttype());
    builder.path(storable.getPath());
    builder.referenceType(storable.getReftype());
    try {
      builder.workspaceName(revMgmt.getWorkspace(storable.getWorkspace()).getName());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return builder.instance();
  }
  
  private Reference convertToReference(ReferenceStorable storable) {
    Reference.Builder builder = new Reference.Builder();
    builder.path(storable.getPath());
    builder.type(storable.getReftype());
    return builder.instance();
  }

  public void removeReference(RemoveReferenceData removeReferenceData5) {
    ReferenceInteraction impl = new ReferenceInteraction();
    Long workspaceRevision = getRevision(removeReferenceData5.getWorkspaceName());
    impl.delete(removeReferenceData5.getPath(), workspaceRevision, removeReferenceData5.getObjectName());
  }
  
  private Long getRevision(String workspaceName) {
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try {
      return revMgmt.getRevision(new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace(workspaceName));
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void triggerReferences(List<? extends Reference> refs, List<String> jarNames, Long revision) {
    ReferenceSupport impl = new ReferenceSupport();
    List<InternalReference> references = convert(refs, revision);
    impl.triggerReferences(references, revision);
  }
  
  private List<InternalReference> convert(List<? extends Reference> arg0, Long revision) {
    List<InternalReference> references = new ArrayList<>();
    Workspace workspace = getWorkspace(revision);
    RepositoryConnection connection = RepositoryManagement.getRepositoryConnection(workspace);
    for(Reference ref : arg0) {
      InternalReference internal = new InternalReference();
      internal.setPath(ref.getPath());
      internal.setType(ref.getType());
      internal.setPathToRepo(connection.getPath());
      references.add(internal);
    }
    return references;
  }
  
  private Workspace getWorkspace(Long revision) {
    try {
      return new Workspace(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getWorkspace(revision).getName());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }
  
  private List<ObjectWithReferences> loadReferenceObjectNames(Long revision) {
    List<ObjectWithReferences> result = new ArrayList<>();
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
    Trigger[] triggers = xat.getTriggers(revision);
    for(int i=0; i< triggers.length; i++) {
      result.add(new ObjectWithReferences(triggers[i].getTriggerName(), "TRIGGER"));
    }
    //Filter
    try {
      Filter[] filters = xat.getFilters(revision);
      for (int i = 0; i < filters.length; i++) {
        result.add(new ObjectWithReferences(filters[i].getName(), "FILTER"));
      }
    } catch (PersistenceLayerException e) {

    }
    
    //SharedLibraries
    Collection<SharedLib> libs = XynaFactory.getInstance().getXynaMultiChannelPortalPortal().listSharedLibs(revision);
    for(SharedLib lib : libs) {
      result.add(new ObjectWithReferences(lib.getName(), "SHARED_LIBRARY"));
    }

    //DataTypes that have at least one referenced library
    Set<DependencyNode> doms = XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister()
        .getDependencyNodesByType(DependencySourceType.DATATYPE, revision);
    GenerationBaseCache cache = new GenerationBaseCache();
    for(DependencyNode dom : doms) {
      String fqn = dom.getUniqueName();
      try {
        DOM d = DOM.getOrCreateInstance(fqn, cache, revision);
        d.parse(false);
        Set<String> add = new HashSet<>(d.getAdditionalLibraries());
        add.addAll(d.getPythonLibraries());
        if(!add.isEmpty()) {
          result.add(new ObjectWithReferences(fqn, "DATATYPE"));
        }
      } catch(Exception e) {
        
      }
    }
    
    return result;
  }

  @Override
  public List<? extends IndexedObjectReference> loadReferencesForGui(RepositoryConnection connection) {
    List<IndexedObjectReference> result = new ArrayList<>();
    ReferenceStorage storage = new ReferenceStorage();
    Long revision = getRevision(connection.getWorkspaceName());
    List<ReferenceStorable> references = storage.getAllReferencesForWorkspace(revision);
    Map<String, List<ReferenceStorable>> referenceMap = new HashMap<>();
    for(ReferenceStorable storable : references) {
      referenceMap.putIfAbsent(storable.getObjectName(), new ArrayList<>());
      referenceMap.get(storable.getObjectName()).add(storable);
    }
    List<ObjectWithReferences> objects = loadReferenceObjectNames(revision);
    objects.sort((x,y) -> x.name.compareTo(y.name));
    int idx = 0;
    for(ObjectWithReferences obj : objects) {
      IndexedObjectReference.Builder builder = new IndexedObjectReference.Builder();
      builder.index(idx++);
      builder.name(obj.name);
      builder.objectType(obj.type);
      List<ReferenceStorable> refs = referenceMap.getOrDefault(obj.name, Collections.emptyList());
      builder.numberOfReferences(refs.size());
      List<Reference> referenceList = refs.stream().map(this::convertToReference).collect(Collectors.toList());
      builder.reference(referenceList);
      builder.objectValid(true);
      builder.workspaceName(connection.getWorkspaceName());
      result.add(builder.instance());
      referenceMap.remove(obj.name);
    }
    
    for(Entry<String, List<ReferenceStorable>> entry : referenceMap.entrySet()) {
      IndexedObjectReference.Builder builder = new IndexedObjectReference.Builder();
      builder.index(idx++);
      builder.name(entry.getKey());
      builder.objectType(entry.getValue().get(0).getObjecttype()); //TODO: check if types are consistent
      List<ReferenceStorable> refs = referenceMap.getOrDefault(entry.getKey(), Collections.emptyList());
      builder.numberOfReferences(refs.size());
      List<Reference> referenceList = refs.stream().map(this::convertToReference).collect(Collectors.toList());
      builder.reference(referenceList);
      builder.objectValid(false);
      builder.workspaceName(connection.getWorkspaceName());
      result.add(builder.instance());
    }
    
    return result;
  }
  
  
  private static class ObjectWithReferences {
    private String name;
    private String type;
    
    public ObjectWithReferences(String name, String type) {
      this.name = name;
      this.type = type;
    }
  }


  @Override
  public List<? extends IndexedReference> loadReferencesOfObject(IndexedObjectReference indexedReference) {
    List<IndexedReference> result = new ArrayList<>();
    ReferenceStorage storage = new ReferenceStorage();
    Long revision = getRevision(indexedReference.getWorkspaceName());
    List<ReferenceStorable> references = storage.getAllReferencesForWorkspace(revision);
    int idx = 0;
    for(ReferenceStorable reference : references) {
      if(!reference.getObjectName().equals(indexedReference.getName())) {
        continue;
      }
      IndexedReference.Builder builder = new IndexedReference.Builder();
      builder.index(idx++);
      Reference.Builder refBuilder = new Reference.Builder();
      refBuilder.path(reference.getPath());
      refBuilder.type(reference.getReftype());
      builder.reference(refBuilder.instance());
      result.add(builder.instance());
    }
    return result;
  }


  @Override
  public List<? extends ReferenceMethodType> listReferenceMethodTypes() {
    ReferenceType[] values = ReferenceType.values();
    List<ReferenceMethodType> result = new ArrayList<>();
    for (int i = 0; i < values.length; i++) {
      result.add(new ReferenceMethodType.Builder().name(values[i].toString()).instance());
    }
    return result;
  }
}
