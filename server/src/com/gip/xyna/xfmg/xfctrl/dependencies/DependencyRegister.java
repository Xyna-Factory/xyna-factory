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

package com.gip.xyna.xfmg.xfctrl.dependencies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.FilterChangedListener;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.TriggerChangedListener;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer.AdditionalDependencyType;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.execution.ExecutionDispatcher;



public class DependencyRegister extends FunctionGroup {

  public static final String DEFAULT_NAME = "DependencyRegister";
  public static int ID_FUTURE_EXECUTION = XynaFactory.getInstance().getFutureExecution().nextId();

  
  private static class RemovalConcurrentMap<T,S> extends ObjectWithRemovalSupport {

    private final ConcurrentMap<T, S> map = new ConcurrentHashMap<T, S>(4, 0.75f, 2);

    @Override
    protected boolean shouldBeDeleted() {
      return map.isEmpty();
    }
  }
  
  private static class SetOfAllNodes<K> extends ConcurrentMapWithObjectRemovalSupport<K, RemovalConcurrentMap<DependencyNode, DependencyNode>> {

    private static final long serialVersionUID = 1L;

    @Override
    public RemovalConcurrentMap<DependencyNode, DependencyNode> createValue(K key) {
      return new RemovalConcurrentMap<DependencyNode, DependencyNode>();
    }
  }
  
  /*
   * eine dependencynode kennt immer alle ihre direkten dependencies (wer benutzt mich?) und usednodes (wen benutze ich?)
   * die darin enthaltenen DependencyNodes sind die gleichen Objektinstanzen wie die in der Map, d.h. man kann sich
   * an den Objekten entlanghangeln, wenn man eine DependencyNode hat und muss nicht immer wieder im DependenyRegister nachfragen.
   * 
   * die keys sind leere objekte ohne sets von benutzen/abhängigen objekten
   */
  private ConcurrentMapWithObjectRemovalSupport<Long, RemovalConcurrentMap<DependencyNode, DependencyNode>> setOfAllNodes;
  {
    //init methode kann die variable bereits initialisiert haben
    //andere komponenten greifen auf dependencyregister zu, init() wurde aber noch nicht ausgeführt wegen abhängigkeiten
    if (setOfAllNodes == null) {
      setOfAllNodes = new SetOfAllNodes<Long>();
    }
  }


  private UndeploymentHandler generationBaseUndeploymentHandler;
  private DeploymentHandler generationBaseDeploymentHandler;

  private TriggerChangedListener triggerChangedListener;
  private FilterChangedListener filterChangedListener;

  public static final String ORDERTYPE_DEPENDENCY_NAME = "Ordertype";
  public static final String DATATYPE_DEPENDENCY_NAME = "Datatype";
  public static final String WORKFLOW_DEPENDENCY_NAME = "Workflow";
  public static final String TRIGGER_DEPENDENCY_NAME = "Trigger";
  public static final String FILTER_DEPENDENCY_NAME = "Filter";
  public static final String XYNAPROPERTY_DEPENDENCY_NAME = "XynaProperty";
  public static final String XYNAFACTORY_DEPENDENCY_NAME = "XynaFactory";
  public static final String SHAREDLIB_DEPENDENCY_NAME = "SharedLib";
  public static final String XYNAEXCEPTION_DEPENDENCY_NAME = "XynaException";


  public enum DependencySourceType {

    ORDERTYPE(ORDERTYPE_DEPENDENCY_NAME),
    DATATYPE(DATATYPE_DEPENDENCY_NAME),
    WORKFLOW(WORKFLOW_DEPENDENCY_NAME),
    TRIGGER(TRIGGER_DEPENDENCY_NAME),
    FILTER(FILTER_DEPENDENCY_NAME),
    XYNAPROPERTY(XYNAPROPERTY_DEPENDENCY_NAME),
    XYNAFACTORY(XYNAFACTORY_DEPENDENCY_NAME),
    SHAREDLIB(SHAREDLIB_DEPENDENCY_NAME),
    XYNAEXCEPTION(XYNAEXCEPTION_DEPENDENCY_NAME);


    private String name;

    private DependencySourceType(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }


    public static DependencySourceType getByName(String name, boolean ignoreCase) {
      for (DependencySourceType t : DependencySourceType.values()) {
        if (t.getName().equals(name) || (ignoreCase && t.getName().equalsIgnoreCase(name))) {
          return t;
        }
      }
      throw new IllegalArgumentException("Unknown " + DependencySourceType.class.getSimpleName() + ": '" + name + "'");
    }


    public static String getAllValidNamesAsSingleString() {
      StringBuilder sb = new StringBuilder();
      DependencySourceType[] values = DependencySourceType.values();
      for (int i = 0; i < values.length; i++) {
        sb.append(values[i].getName());
        if (i < values.length - 1) {
          sb.append(", ");
        }
      }
      return sb.toString();
    }


    public static DependencySourceType from(XMOMType type) {
      switch (type) {
        case DATATYPE :
          return DATATYPE;
        case EXCEPTION :
          return XYNAEXCEPTION;
        case WORKFLOW :
          return WORKFLOW;
        default :
          throw new RuntimeException("unsupported transformation: " + type);
      }
    }

  };

  static {
    try {
      // alle klassen die einen deploymenthandler definieren müssen hier rein! schliesslich sollen diese auch schon
      // aufgerufen werden, wenn die workflows der wfdb geladen werden. d.h. beim serverstart müssen die deployment
      // handler bereits definiert worden sein, bevor die workflows deployed werden.
      addDependencies(DependencyRegister.class, new ArrayList<XynaFactoryPath>(Arrays.asList(new XynaFactoryPath[] {
                      new XynaFactoryPath(XynaProcessing.class, XynaFractalWorkflowEngine.class,
                                          DeploymentHandling.class)})));

    } catch (Throwable t) {
      Department.handleThrowable(t);
      CentralFactoryLogging.getLogger(DependencyRegister.class).error("", t);
    }
  }


  public DependencyRegister() throws XynaException {
    super();
  }


  public DependencyRegister(String unused) throws XynaException {
    // this exists for testing and serverstartup purposes only!
    super(unused);
    setOfAllNodes = new SetOfAllNodes<Long>();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(DependencyRegister.class, "DependencyRegister").
      after(DeploymentHandling.class).
      execAsync(new Runnable() { public void run() { initAll(); } });
  }
  
  private void initAll() {
    if (setOfAllNodes == null) {
      setOfAllNodes = new SetOfAllNodes<Long>();
    }
    createDeploymentHandlers();
    createUndeploymentHandlers();
    createTriggerAndFilterChangedHandlers();

    // Dependency from XynaFractalWorkflowEngine
    addDependency(DependencySourceType.XYNAPROPERTY, XynaProperty.XYNA_DISABLE_XSD_VALIDATION,
                                   DependencySourceType.XYNAFACTORY, XynaFractalWorkflowEngine.DEFAULT_NAME);

  }


  @Override
  protected void shutdown() throws XynaException {
    removeDeploymentHandler();
    removeUndeploymentHandler();
    removeTriggerAndFilterChangeListeners();
  }


  private ConcurrentMap<DependencyNode, DependencyNode> getSetOfAllNodesLazy(Long revision) {
    RemovalConcurrentMap<DependencyNode, DependencyNode> concurrentMap = setOfAllNodes.lazyCreateGet(revision);
    setOfAllNodes.cleanup(revision);

    return concurrentMap.map;
  }

  
  public Set<DependencyNode> getDependencyNodesByType(DependencySourceType type) {
    return getDependencyNodesByType(type, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public Set<DependencyNode> getDependencyNodesByType(DependencySourceType type, Long revision) {

    if (type == null) {
      throw new IllegalArgumentException("Type may not be null");
    }

    ConcurrentMap<DependencyNode, DependencyNode> setOfAllUsedNodesLazy = getSetOfAllNodesLazy(revision);
    Set<DependencyNode> result = new HashSet<DependencyNode>();
    for (DependencyNode dn : setOfAllUsedNodesLazy.values()) {
      if (dn.getType() == type) {
        result.add(dn);
      }
    }
    return result;
  }

  
  public Set<DependencyNode> getDependencies(String uniqueName, DependencySourceType type) {
    return getDependencies(uniqueName, type, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public Set<DependencyNode> getDependencies(String uniqueName, DependencySourceType type, Long revision) {
    return getDependencies(uniqueName, type, revision, false);
  }
  
  
  public Set<DependencyNode> getDependencies(String uniqueName, DependencySourceType type, Long revision, boolean recursive) {
    DependencyNode dependencyNode = getSetOfAllNodesLazy(revision).get(new DependencyNode(uniqueName, type, revision));
    if (dependencyNode == null) {
      return Collections.emptySet();
    } else if (recursive) {
      Set<DependencyNode> recursiveDependencies = new HashSet<DependencyNode>();
      collectDependenciesRecursively(dependencyNode, recursiveDependencies);
      return recursiveDependencies;
    } else {
      return dependencyNode.getDependentNodes();
    }
  }


  private void collectDependenciesRecursively(DependencyNode dependencyNode, Set<DependencyNode> recursiveDependencies) {
    if (!recursiveDependencies.add(dependencyNode)) {
      return;
    }
    for (DependencyNode n : dependencyNode.getDependentNodes()) {
      collectDependenciesRecursively(n, recursiveDependencies);
    }
  }


  public DependencyNode getDependencyNode(String uniqueName, DependencySourceType type, Long revision) {
    return getDependencyNodeNonLazy(new DependencyNode(uniqueName, type, revision));
  }


  private DependencyNode getDependencyNodeLazy(DependencyNode key) {
    RemovalConcurrentMap<DependencyNode, DependencyNode> map = setOfAllNodes.lazyCreateGet(key.getRevision());
    try{
      DependencyNode dependencyNode = map.map.get(key);
      if (dependencyNode == null) {
        dependencyNode = new DependencyNode(key.getUniqueName(), key.getType(), key.getRevision());
        DependencyNode previous = map.map.putIfAbsent(key, dependencyNode);
        if (previous != null) {
          dependencyNode = previous;
        }
      }
      
      return dependencyNode;
    } finally {
      setOfAllNodes.cleanup(key.getRevision());
    }
  }
  
  private DependencyNode getDependencyNodeNonLazy(DependencyNode key) {
    return getSetOfAllNodesLazy(key.getRevision()).get(key);
  }

  /**
   * usedNode wird von usedBy verwendet 
   * @return true falls dependency nicht bereits existierte
   */
  private boolean addDependency(DependencyNode usedNode, DependencyNode usedBy) {
    DependencyNode dependencyUsedByNode = getDependencyNodeLazy(usedBy);
    DependencyNode dependencyUsedNode = getDependencyNodeLazy(usedNode);
    if (dependencyUsedByNode == dependencyUsedNode) {
      return false;
    }
    
    boolean result = dependencyUsedByNode.addUsedNode(dependencyUsedNode);
    if (result) {
      result = dependencyUsedNode.addNodeThatUsesThis(dependencyUsedByNode);
    }
    return result;
  }


  /**
   * Entfernt eine Abhängikeit zwischen usedNode und noMoreUsedBy.
   * Falls noMoreUsedBy keine weiteren Abhängikeiten hat, wird er aus setOfAllNodes entfernt
   * @param usedNode
   * @param noMoreUsedBy
   * @return
   */
  private boolean removeDependency(DependencyNode usedNode, DependencyNode noMoreUsedBy) {
    DependencyNode dependencyUsedByNode = getDependencyNodeLazy(noMoreUsedBy);
    DependencyNode dependencyUsedNode = getDependencyNodeNonLazy(usedNode); //nicht neu anlegen!
    
    boolean result = false;
    if (dependencyUsedNode != null) {
      result = dependencyUsedByNode.removeUsedNode(dependencyUsedNode);
      if (result) {
        result = dependencyUsedNode.removeDependentNode(dependencyUsedByNode);
      }
    }
    
    RemovalConcurrentMap<DependencyNode, DependencyNode> allNodes = setOfAllNodes.lazyCreateGet(dependencyUsedByNode.getRevision());
    try{
      if(dependencyUsedByNode.getDependentNodes().size() == 0 && dependencyUsedByNode.getUsedNodes().size() == 0) {
        allNodes.map.remove(dependencyUsedByNode);
      }
    } finally {
      setOfAllNodes.cleanup(dependencyUsedByNode.getRevision());
    }

    return result;
  }

  /**
   * Entfernt einen DependencyNode und alle seine Abhängigkeiten
   * @param name
   * @param type
   * @param revision
   */
  public void removeDependencyNode(String name, DependencySourceType type, Long revision) {
    DependencyNode toBeRemoved = new DependencyNode(name, type, revision);
    DependencyNode dependencyNode = getDependencyNodeNonLazy(toBeRemoved); //nicht neu anlegen!!!
    
    if (dependencyNode != null) {
      removeAllDependencies(dependencyNode, revision);
    }
  }

  
  void removeAllDependencies(DependencyNode toBeRemoved, Long revision) {  
    if (toBeRemoved == null) {
      throw new IllegalArgumentException(DependencyNode.class.getSimpleName() + " to be removed may not be null");
    }
    
    RemovalConcurrentMap<DependencyNode, DependencyNode> map = setOfAllNodes.lazyCreateGet(revision);
    try {
      DependencyNode removed = map.map.remove(toBeRemoved);
      if (removed == null) {
        return;
      }
      
      //vorwärts und rückwärts referenzen aus dep-register entfernen
      for (DependencyNode dependent : removed.getDependentNodes()) {
        dependent.removeUsedNode(removed);
      }
      for (DependencyNode used : removed.getUsedNodes()) {
        used.removeDependentNode(removed);
      }
    } finally {
      setOfAllNodes.cleanup(revision);
    }
  }

  /**
   * Entfernt oldNode und überträgt alle Abhängigkeiten auf newNode.
   * @param oldNode
   * @param newNode
   * @param revision
   */
  private void exchangeAllDependencies(DependencyNode oldNode, DependencyNode newNode, Long revision) {  
    if (oldNode == null || newNode == null) {
      throw new IllegalArgumentException(DependencyNode.class.getSimpleName() + " to be exchanged may not be null");
    }
    
    RemovalConcurrentMap<DependencyNode, DependencyNode> map = setOfAllNodes.lazyCreateGet(revision);
    try{
      //alten DependencyNode entfernen
      DependencyNode removed = map.map.remove(oldNode);
      if (removed == null) {
        return;
      }
      
      //neuen Node eintragen, falls nicht vorhanden
      DependencyNode added = getDependencyNodeLazy(newNode);
      
      //vorwärts und rückwärts referenzen aus dep-register übertragen
      for (DependencyNode dependent : removed.getDependentNodes()) {
        dependent.removeUsedNode(removed);
        dependent.addUsedNode(added);
        added.addNodeThatUsesThis(dependent);
      }
      for (DependencyNode used : removed.getUsedNodes()) {
        used.removeDependentNode(removed);
        used.addNodeThatUsesThis(added);
        added.addUsedNode(used);
      }
      
    } finally {
      setOfAllNodes.cleanup(revision);
    }
  }
  
  
  /**
   * Tauscht den DependencyNode des fqClassNames mit dem für den xmlName aus
   * @param fqClassName
   * @param xmlName
   * @param revision
   */
  public void exchangeFqClassNameForXmlName (String fqClassName, String xmlName, Long revision) {
    DependencyNode oldNode = new DependencyNode(fqClassName, DependencySourceType.WORKFLOW, revision);
    DependencyNode newNode = new DependencyNode(xmlName, DependencySourceType.WORKFLOW, revision);
    
    exchangeAllDependencies(oldNode, newNode, revision);
  }
  
  /**
   * wenn sich vorwärtsreferenzen (wen benutze ich) ändern, will man die alten ändern und die neuen eintragen.
   * die rückwärtsreferenzen ändern sich dadurch nicht (wer benutzt mich).
   */
  void removeMyUsedObjects(DependencyNode toBeRemoved) {
    if (toBeRemoved == null) {
      throw new IllegalArgumentException(DependencyNode.class.getSimpleName() + " to be removed may not be null");
    }
    
    DependencyNode node = getDependencyNodeLazy(toBeRemoved);
    Set<DependencyNode> usedNodes = node.getUsedNodes();
    for (DependencyNode used : usedNodes) {
      used.removeDependentNode(node);
      node.removeUsedNode(used);
    }
  }

  /**
   * nicht empfohlen, weil man leicht fehler macht, indem man nur eine revision angibt.
   * @deprecated use {@link #removeDependency(DependencySourceType, String, Long, DependencySourceType, String, Long)}
   */
  @Deprecated
  public boolean removeDependency(DependencySourceType usedNodeType, String usedNodeUniqueName, DependencySourceType noMoreUsedByType,
                                  String noMoreUsedByUniqueName, Long revision) {
    return removeDependency(new DependencyNode(usedNodeUniqueName, usedNodeType, revision),
                            new DependencyNode(noMoreUsedByUniqueName, noMoreUsedByType, revision));
  }

  public boolean removeDependency(DependencySourceType usedNodeType, String usedNodeUniqueName, Long usedRevision,
                                     DependencySourceType noMoreUsedByType, String noMoreUsedByUniqueName, Long usedByRevision) {
    return removeDependency(new DependencyNode(usedNodeUniqueName, usedNodeType, usedRevision),
                            new DependencyNode(noMoreUsedByUniqueName, noMoreUsedByType, usedByRevision));
  }

  /**
   * nicht empfohlen, weil man leicht fehler macht, indem man nur eine revision angibt.
   * @deprecated use {@link #addDependency(DependencySourceType, String, Long, DependencySourceType, String, Long)}
   */
  @Deprecated
  public boolean addDependency(DependencySourceType usedNodeType, String usedNodeUniqueName,
                               DependencySourceType usedByType, String usedByUniqueName, Long revision) {
    return addDependency(new DependencyNode(usedNodeUniqueName, usedNodeType, revision),
                         new DependencyNode(usedByUniqueName, usedByType,revision));
  }
  
  public boolean addDependency(DependencySourceType usedNodeType, String usedNodeUniqueName, Long usedNodeRevision,
                               DependencySourceType usedByType, String usedByUniqueName, Long usedByRevision) {
      return addDependency(new DependencyNode(usedNodeUniqueName, usedNodeType, usedNodeRevision),
                           new DependencyNode(usedByUniqueName, usedByType,usedByRevision));
  }
  
  public boolean addDependency(DependencySourceType usedNodeType, String usedNodeUniqueName,
                               DependencySourceType usedByType, String usedByUniqueName) {
    return addDependency(usedNodeType, usedNodeUniqueName, usedByType, usedByUniqueName, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  private void createDeploymentHandlers() {
    generationBaseDeploymentHandler = new DependencyRegisterDeploymentHandler(this);
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                    .addDeploymentHandler(DeploymentHandling.PRIORITY_DEPENDENCY_CREATION,
                                          generationBaseDeploymentHandler);
  }


  private void createUndeploymentHandlers() {
    generationBaseUndeploymentHandler = new DependencyRegisterUndeploymentHandler(this);
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                    .addUndeploymentHandler(DeploymentHandling.PRIORITY_DEPENDENCY_CREATION,
                                            generationBaseUndeploymentHandler);
  }


  private void removeDeploymentHandler() {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                    .removeDeploymentHandler(generationBaseDeploymentHandler);
  }


  private void removeUndeploymentHandler() {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                    .removeUndeploymentHandler(generationBaseUndeploymentHandler);
  }


  private void createTriggerAndFilterChangedHandlers() {

    triggerChangedListener = new TriggerChangedListener() {

      public void triggerAdded(Trigger t) throws Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException {
        createEmptyNodeIfDoesNotExist(t.getTriggerName(), DependencySourceType.TRIGGER, t.getRevision());
        AdditionalDependencyContainer container = t.getAdditionalDependencies();
        if (container != null) {
          addDependenciesForAdditionalDependencies(container, DependencySourceType.TRIGGER, t.getTriggerName(), t.getRevision());
        }
        addDependenciesToSharedLibs(DependencySourceType.TRIGGER, t.getTriggerName(), t.getRevision(), t.getSharedLibs());
      }


      public void triggerRemoved(Trigger t) {
        DependencyNode toBeRemoved = new DependencyNode(t.getTriggerName(), DependencySourceType.TRIGGER, t.getRevision());
        removeAllDependencies(toBeRemoved, t.getRevision());
      }

    };

    filterChangedListener = new FilterChangedListener() {

      public void filterAdded(Filter f)
          throws Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException {
        createEmptyNodeIfDoesNotExist(f.getName(), DependencySourceType.FILTER, f.getRevision());
        AdditionalDependencyContainer container = f.getAdditionalDependencies();
        if (container != null) {
          addDependenciesForAdditionalDependencies(container, DependencySourceType.FILTER, f.getName(), f.getRevision());
        }
        addDependenciesToSharedLibs(DependencySourceType.FILTER, f.getName(), f.getRevision(), f.getSharedLibs());
        addDependency(DependencySourceType.TRIGGER, f.getTriggerName(), f.getRevision(), DependencySourceType.FILTER, f.getName(),
                      f.getTrigger().getRevision());
      }


      public void filterRemoved(Filter f) {
        DependencyNode toBeRemoved = new DependencyNode(f.getName(), DependencySourceType.FILTER, f.getRevision());
        removeAllDependencies(toBeRemoved, f.getRevision());
      }

    };

    XynaFactory.getInstance().getActivation().getActivationTrigger().addTriggerChangeListener(triggerChangedListener);
    XynaFactory.getInstance().getActivation().getActivationTrigger().addFilterChangeListener(filterChangedListener);

  }


  void addDependenciesToSharedLibs(DependencySourceType usingtype, String usingName, Long revision, String[] sharedLibs) {
    if (sharedLibs == null) {
      return;
    }
    RuntimeContextDependencyManagement rcdm =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    for (String s : sharedLibs) {
      if (s != null && !"".equals(s)) {
        Long rev = rcdm.getRevisionDefiningSharedLib(s, revision);
        if (rev == null) {
          throw new RuntimeException("SharedLib " + s + " not found");
        }
        addDependency(DependencySourceType.SHAREDLIB, s, rev, usingtype, usingName, revision);
      }
    }
  }



  /**
   * dependencies aus additional dependencies zu using node hinzufügen  
   */
  void addDependenciesForAdditionalDependencies(AdditionalDependencyContainer container, DependencySourceType usingNodeType,
                                                String usingNodeName, Long revision) {
    // TODO create mapping from AdditionalDependencyType to DependencySourceType and use an automatic loop
    RuntimeContextDependencyManagement rcdm =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    ExecutionDispatcher ed =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher();
    for (String s : container.getAdditionalDependencies(AdditionalDependencyType.ORDERTYPE)) {
      long revisionOfOrderType;
      try {
        revisionOfOrderType = rm.getRevision(ed.getRuntimeContextDefiningOrderType(new DestinationKey(s, rm.getRuntimeContext(revision))));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        if (logger.isInfoEnabled()) {
          logger.info("OrderType defined as additional dependency '" + s + "' not found.");
        }
        revisionOfOrderType = revision;
      }
      addDependency(DependencySourceType.ORDERTYPE, s, revisionOfOrderType, usingNodeType, usingNodeName, revision);
    }
    for (String s : container.getAdditionalDependencies(AdditionalDependencyType.DATATYPE)) {
      addDependency(DependencySourceType.DATATYPE, s, rcdm.getRevisionDefiningXMOMObjectOrParent(s, revision), usingNodeType, usingNodeName, revision);
    }
    for (String s : container.getAdditionalDependencies(AdditionalDependencyType.XYNA_PROPERTY)) {
      addDependency(DependencySourceType.XYNAPROPERTY, s, usingNodeType, usingNodeName, revision); //FIXME xynaproperty ist nicht revisionspezifisch?!
    }
    for (String s : container.getAdditionalDependencies(AdditionalDependencyType.EXCEPTION)) {
      addDependency(DependencySourceType.XYNAEXCEPTION, s, rcdm.getRevisionDefiningXMOMObjectOrParent(s, revision), usingNodeType, usingNodeName, revision);
    }
    for (String s : container.getAdditionalDependencies(AdditionalDependencyType.WORKFLOW)) {
      addDependency(DependencySourceType.WORKFLOW, s, rcdm.getRevisionDefiningXMOMObjectOrParent(s, revision), usingNodeType, usingNodeName, revision);
    }
  }


  private void removeTriggerAndFilterChangeListeners() {
    XynaFactory.getInstance().getActivation().getActivationTrigger().removeTriggerChangeListener(triggerChangedListener);
    XynaFactory.getInstance().getActivation().getActivationTrigger().removeFilterChangeListener(filterChangedListener);
  }


  private Set<DependencyNode> getAllUsedNodes(DependencyNode usingNode, boolean recurse, boolean includeHead) {
    DependencyNode node = getDependencyNodeNonLazy(usingNode);
    if (node == null) {
      //TODO includeHead?
      return new HashSet<DependencyNode>();
    }
        
    Set<DependencyNode> result = new HashSet<DependencyNode>();
    getAllUsedNodes(node, recurse, result);
    if (includeHead) {
      if (result.size() > 0 || existsNode(node)) {
        //> 0 => offenbar existiert eine entsprechende dependencynode
        result.add(usingNode);
      }
    } else {
      result.remove(node);
    }
    return result;
  }


  private boolean existsNode(DependencyNode usingNode) {
    DependencyNode node = getDependencyNodeNonLazy(usingNode);
    return node != null;
  }


  private void getAllUsedNodes(DependencyNode node, boolean recurse, Set<DependencyNode> result) {
    if (result.contains(node)) {
      return;
    }
    result.add(node);
    if (recurse) {
      for (DependencyNode used : node.getUsedNodes()) {
        getAllUsedNodes(used, recurse, result);
      }
    } else {
      for (DependencyNode used : node.getUsedNodes()) {
        result.add(used);
      }
    }
  }


  public boolean hasUsedNodesByUsedNodeType(String usingObjectUniqueName, DependencySourceType usingType,
                                            DependencySourceType usedType, Long revision) {
    DependencyNode node = getDependencyNodeNonLazy(new DependencyNode(usingObjectUniqueName, usingType, revision));
    if (node == null) {
      return false;
    }
    for (DependencyNode used : node.getUsedNodes()) {
      if (used.getType() == usedType) {
        return true;
      }
    }
    return false;
  }


  public Set<DependencyNode> getAllUsedNodes(String usingObjectUniqueName, DependencySourceType type, boolean recurse) {
    return getAllUsedNodes(usingObjectUniqueName, type, recurse, false);
  }


  public Set<DependencyNode> getAllUsedNodes(String usingObjectUniqueName, DependencySourceType type, boolean recurse,
                                             boolean includeHead, Long revision) {
    if (usingObjectUniqueName == null) {
      throw new IllegalArgumentException("Unique name may not be null");
    }
    if (type == null) {
      throw new IllegalArgumentException("Type may not be null");
    }
    return Collections.unmodifiableSet(getAllUsedNodes(new DependencyNode(usingObjectUniqueName, type, revision), recurse,
                                                       includeHead));
  }
  
  
  public Set<DependencyNode> getAllUsedNodes(String usingObjectUniqueName, DependencySourceType type, boolean recurse,
                                             boolean includeHead) {
    return getAllUsedNodes(usingObjectUniqueName, type, recurse, includeHead, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  public Set<DependencyNode> getAllUsedNodesSameRevision(String usingObjectUniqueName, DependencySourceType type, boolean recurse,
                                                         boolean includeHead, Long revision) {
    DependencyNodeRevisionFilter filter = new DependencyNodeRevisionFilter(revision);
    Set<DependencyNode> allUsedNodes = getAllUsedNodes(usingObjectUniqueName, type, recurse, includeHead, revision);
    List<DependencyNode> allUsedNodesSameRevision = CollectionUtils.filter(allUsedNodes, filter);
    return new HashSet<DependencyNode>(allUsedNodesSameRevision);
  }
  

  private static class DependencyNodeRevisionFilter implements com.gip.xyna.utils.collections.CollectionUtils.Filter<DependencyNode> {
    private Long revision;

    public DependencyNodeRevisionFilter(Long revision) {
      this.revision = revision;
    }

    public boolean accept(DependencyNode dn) {
      return dn.getRevision().equals(revision);
    }
  }
  
  /**
   * @return true if the node existed before and false otherwise
   */
  protected boolean createEmptyNodeIfDoesNotExist(String uniqueName, DependencySourceType type, Long revision) {
    DependencyNode key = new DependencyNode(uniqueName, type, revision);
    DependencyNode previous = getDependencyNodeNonLazy(key);
    getDependencyNodeLazy(key);
    return previous != null;
  }


  /**
   * aus den used nodes der übergebenen node werden alle nodes entfernt,
   * die in einer der entfernten revisions liegen und ersetzt durch existiertende nodes in der richtigen revision.
   */
  public void updateRevisionInUsedNodes(DependencyNode dependencyNode, Set<Long> removedRevisions) {
    RuntimeContextDependencyManagement rcdm =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    ExecutionDispatcher eed =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher();
    for (DependencyNode dn : dependencyNode.getUsedNodes()) {
      if (removedRevisions.contains(dn.getRevision())) {
        Long newRev = null;
        switch (dn.getType()) {
          case DATATYPE :
          case WORKFLOW :
          case XYNAEXCEPTION :
            newRev = rcdm.getRevisionDefiningXMOMObjectOrParent(dn.getUniqueName(), dependencyNode.getRevision());
            break;
          case FILTER :
            try {
              Filter filter = xat.getFilter(dependencyNode.getRevision(), dn.getUniqueName(), true);
              newRev = filter.getRevision();
            } catch (PersistenceLayerException e) {
              logger.debug(null, e);
            } catch (XACT_FilterNotFound e) {
              //ntbd
            }
            break;
          case TRIGGER :
            try {
              Trigger trigger = xat.getTrigger(dependencyNode.getRevision(), dn.getUniqueName(), true);
              newRev = trigger.getRevision();
            } catch (PersistenceLayerException e) {
              logger.info(null, e);
            } catch (XACT_TriggerNotFound e) {
              //ntbd
            }
            break;
          case ORDERTYPE :
            DestinationKey dk;
            try {
              dk = new DestinationKey(dn.getUniqueName(), rm.getRuntimeContext(dependencyNode.getRevision()));
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              throw new RuntimeException(e);
            }
            try {
              newRev = rm.getRevision(eed.getRuntimeContextDefiningOrderType(dk));
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              throw new RuntimeException(e);
            } catch (XPRC_DESTINATION_NOT_FOUND e) {
              //ntbd
            }
            break;
          case SHAREDLIB :
            newRev = rcdm.getRevisionDefiningSharedLib(dn.getUniqueName(), dependencyNode.getRevision());
            break;
          default :
            continue;
        }
        if (!dn.getRevision().equals(newRev)) { //notwendig, weil parentRevision nicht die revision sein muss, in der sich abhängigkeiten geändert haben (sondern ein parent davon)
          if (dependencyNode.removeUsedNode(dn)) {
            dn.removeDependentNode(dependencyNode);

            if (newRev == null) {
              //unerwartet, hätte eigtl vom aufrufer bereits erkannt werden müssen. jetzt kann man aber schlecht nen rollback durchführen. also nur loggen
              logger.warn("Could not replace dependency of " + dependencyNode.getUniqueName() + " to " + dn.getUniqueName()
                  + " with changed revision (" + dn.getRevision() + " -> ?)");
              continue;
            }
            DependencyNode dnNewRev = getDependencyNode(dn.getUniqueName(), dn.getType(), newRev);
            if (dnNewRev == null) {
              //unerwartet, hätte eigtl vom aufrufer bereits erkannt werden müssen. jetzt kann man aber schlecht nen rollback durchführen. also nur loggen
              logger.warn("Could not replace dependency of " + dependencyNode.getUniqueName() + " to " + dn.getUniqueName()
                  + " with changed revision (" + dn.getRevision() + " -> " + newRev + ")");
              continue;
            }
            if (dependencyNode.addUsedNode(dnNewRev)) {
              dnNewRev.addNodeThatUsesThis(dependencyNode);
            }
          }
        }
      }
    }
  }

}
