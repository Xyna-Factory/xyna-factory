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
package com.gip.xyna.xfmg.xfctrl.classloading;



import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.Graph;
import com.gip.xyna.utils.collections.Graph.HasUniqueStringIdentifier;
import com.gip.xyna.utils.collections.Graph.Node;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exception.MultipleExceptions;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_ClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClassLoaderRedeploymentException;
import com.gip.xyna.xfmg.exceptions.XFMG_ExceptionClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_FilterClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_JarFolderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_MDMObjectClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.exceptions.XFMG_SharedLibClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_TriggerClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_WFClassLoaderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl.OrderEntryInterfacesCouldNotBeClosedException;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoaderIdRevisionRef;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoaderSwitcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoadingDependencySource;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ReplaceableClassLoader;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.SupertypeInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeOfUsage;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureRecursionFilter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureVisitor;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableVariableType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_JarFileForServiceImplNotFoundException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer.AdditionalDependencyType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;



/**
 * schnittstelle für alles was mit classloading zu tun hat verwaltet classloaders für diverse objekte: - mdm datentypen
 * - workflows - filter - trigger - sharedlibraries - rmi
 */
public class ClassLoaderDispatcher extends FunctionGroup {

  private static final String DEFAULT_NAME = ClassLoaderDispatcher.class.getSimpleName();
  private static Logger logger = CentralFactoryLogging.getLogger(ClassLoaderDispatcher.class);

  private static final String NAME_OF_EMPTY_SHARED_LIB = "empty";

  static {
    try {
      addDependencies(ClassLoaderDispatcher.class, new ArrayList<XynaFactoryPath>(Arrays
                      .asList(new XynaFactoryPath[] {
                                      new XynaFactoryPath(XynaProcessing.class, XynaFractalWorkflowEngine.class,
                                                          DeploymentHandling.class),
                                      new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryControl.class,
                                                          AutomaticUnDeploymentHandlerManager.class)})));

      //WorkflowDatabase darf erst nach der klasse geladen werden die einen deploymenthandler definiert! siehe WorkflowDatabase
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("", t);
    }
  }

  private EnumMap<ClassLoaderType, ClassLoaderMap> classLoaderMap = new EnumMap<ClassLoaderType, ClassLoaderMap>(ClassLoaderType.class);

  //Outdated Filter in einer eigenen Map halten, da diese zusätzlich eine parentRevision haben
  private OutdatedFilterClassLoaderMap outdatedFilterClassLoader = new OutdatedFilterClassLoaderMap();


  private static class OutdatedFilterRevision {
    private Long revision; //revision des Filters
    private Long parentRevision; //revision des Triggers, der den Filter als OutdatedFilter verwendet

    private OutdatedFilterRevision(Long revision, Long parentRevision) {
      this.revision = revision;
      this.parentRevision = parentRevision;
    }

    public Long getRevision() {
      return revision;
    }

    public Long getParentRevision() {
      return parentRevision;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((parentRevision == null) ? 0 : parentRevision.hashCode());
      result = prime * result + ((revision == null) ? 0 : revision.hashCode());
      return result;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      OutdatedFilterRevision other = (OutdatedFilterRevision) obj;
      if (parentRevision == null) {
        if (other.parentRevision != null)
          return false;
      } else if (!parentRevision.equals(other.parentRevision))
        return false;
      if (revision == null) {
        if (other.revision != null)
          return false;
      } else if (!revision.equals(other.revision))
        return false;
      return true;
    }
  }
  
  
  private static class MapWrapper<K,V> extends ObjectWithRemovalSupport {
    private ConcurrentMap<K,V> map = new ConcurrentHashMap<K, V>();
    
    public boolean shouldBeDeleted() {
      return map.isEmpty();
    }
  }
  
  
  private static class OutdatedFilterClassLoaderMap extends ConcurrentMapWithObjectRemovalSupport<OutdatedFilterRevision, MapWrapper<String, FilterClassLoader>> {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    public MapWrapper<String, FilterClassLoader> createValue(OutdatedFilterRevision key) {
      return new MapWrapper<String, FilterClassLoader>();
    }
  }
  
  
  private static class ClassLoaderMap extends ConcurrentMapWithObjectRemovalSupport<Long, MapWrapper<String, ClassLoaderBase>> {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    public MapWrapper<String, ClassLoaderBase> createValue(Long key) {
      return new MapWrapper<String, ClassLoaderBase>();
    }
  }
  
  private ReentrantLock wfLock = new ReentrantLock();
  private ReentrantLock mdmLock = new ReentrantLock();
  private ReentrantLock exceptionLock = new ReentrantLock();
  private ReentrantLock filterLock = new ReentrantLock();
  private ReentrantLock outdatedFilterLock = new ReentrantLock();
  private ReentrantLock triggerLock = new ReentrantLock();
  private ReentrantLock sharedLibLock = new ReentrantLock();

  private ReentrantLock persistenceLayerLock = new ReentrantLock();
  private ConcurrentHashMap<String, PersistenceLayerClassLoader> persistenceLayerClassLoader = new ConcurrentHashMap<String, PersistenceLayerClassLoader>();

  private Lock[] allLocksAsWriteLockInOrderForDeadLockSecurity = new Lock[] {mdmLock,
      exceptionLock, filterLock, triggerLock, persistenceLayerLock, wfLock, sharedLibLock};

  public static CommandControl.Operation[] operationsToLockForReloadSharedLib = new CommandControl.Operation[] {
      CommandControl.Operation.SHAREDLIB_RELOAD, CommandControl.Operation.TRIGGER_ADD, CommandControl.Operation.TRIGGER_DEPLOY,
      CommandControl.Operation.TRIGGER_INSTANCE_ENABLE, CommandControl.Operation.FILTER_ADD,
      CommandControl.Operation.FILTER_DEPLOY, CommandControl.Operation.FILTER_INSTANCE_ENABLE};
  
  static Map<ClassLoaderType, ReferenceQueue<ClassLoader>> queuesForPhantomReferences =
      new HashMap<ClassLoaderType, ReferenceQueue<ClassLoader>>();
  static Map<ClassLoaderType, Map<PhantomReference<ClassLoader>, String>> phantomClassLoaderInformationMap =
      new HashMap<ClassLoaderType, Map<PhantomReference<ClassLoader>, String>>();

  
  public ClassLoaderDispatcher() throws XynaException {
    super();
    if (logger.isDebugEnabled()) {
      logger.debug("creating " + getClass().getClassLoader());
    }
  }


  public ClassLoaderDispatcher(String name) {
    super(name);
    SerializableClassloadedObject.cld = this;   
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  public void init() throws XynaException {

    classLoaderMap.put(ClassLoaderType.Filter, new ClassLoaderMap());
    classLoaderMap.put(ClassLoaderType.MDM, new ClassLoaderMap());
    classLoaderMap.put(ClassLoaderType.WF, new ClassLoaderMap());
    classLoaderMap.put(ClassLoaderType.Exception, new ClassLoaderMap());
    classLoaderMap.put(ClassLoaderType.SharedLib, new ClassLoaderMap());
    classLoaderMap.put(ClassLoaderType.Trigger, new ClassLoaderMap());
    classLoaderMap.put(ClassLoaderType.DataModelType, new ClassLoaderMap());
    classLoaderMap.put(ClassLoaderType.RMI, new ClassLoaderMap());

    DeploymentHandling dh = XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling();
    dh.addDeploymentHandler(DeploymentHandling.PRIORITY_CLASS_LOADER_UNDEPLOY_OLD,
                            new ClassLoaderDispatcherDeploymentHandler_UndeployOld());
    dh.addDeploymentHandler(DeploymentHandling.PRIORITY_CLASS_LOADER_RECREATE_CLASSLOADERS,
                            new ClassLoaderDispatcherDeploymentHandler_ReloadClassLoaders());
    dh.addDeploymentHandler(DeploymentHandling.PRIORITY_CLASS_LOADER_DEPLOY_NEW,
                            new ClassLoaderDispatcherDeploymentHandler_DeployNew());
    dh.addUndeploymentHandler(DeploymentHandling.PRIORITY_CLASS_LOADER_UNDEPLOY_OLD,
                              new ClassLoaderDispatcherUndeploymentHandler());
    SerializableClassloadedObject.cld = this;

  }


  @SuppressWarnings("unchecked")
  @Override
  public void shutdown() throws XynaException {
    mdmLock.lock();
    try {
      ClassLoaderMap mdmCls = classLoaderMap.get(ClassLoaderType.MDM);
      for (MapWrapper<String, ClassLoaderBase> mdmClassLoaderForRevision : mdmCls.values()) {
        for (Entry<String, ClassLoaderBase> e : mdmClassLoaderForRevision.map.entrySet()) {
          String fqClassname = e.getKey();
          if (e.getValue().hasBeenDeployed()) {
            try {
              XynaObject.undeploy((Class<? extends XynaObject>) e.getValue().loadClass(fqClassname));
            } catch (ClassNotFoundException e1) {
              logger.warn("Failed to call undeployment handlers for type " + fqClassname, e1);
            }
          }
        }
      }
    } finally {
      mdmLock.unlock();
    }
  }

  /**
   * sucht auch in abhängigen anderen revisions, falls nicht in dieser gefunden 
   */
  public Class<XynaProcess> loadWFClass(String fqClassName, Long revision) throws ClassNotFoundException {
    Class<XynaProcess> c = loadWFClassOwnRevision(fqClassName, revision);

    if (c != null) {
      return c;
    }
    
    Set<Long> requirements = new HashSet<Long>();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(revision, requirements);
    for (long req : requirements) {
      c = loadWFClassOwnRevision(fqClassName, req);
      if (c != null) {
        return c;
      }
    }
    
    throw new ClassNotFoundException("class " + fqClassName + " could not be loaded in revision " + revision);
  }

  @SuppressWarnings("unchecked")
  private Class<XynaProcess> loadWFClassOwnRevision(String fqClassName, Long revision) throws ClassNotFoundException {
    ClassLoaderBase wfClassLoader = getClassLoaderByType(ClassLoaderType.WF, fqClassName, revision);
    if (wfClassLoader != null) {
      return (Class<XynaProcess>) wfClassLoader.loadClass(fqClassName);
    }
    
    return null;
  }


  public Class<XynaObject> loadMDMClass(String name, String originalXmlPath, String originalXmlName, Long revision)
                  throws ClassNotFoundException {
    return loadMDMClass(name, true, originalXmlPath, originalXmlName, revision);
  }

  /**
   * sucht auch in abhängigen anderen revisions, falls nicht in dieser gefunden 
   */
  public Class<XynaObject> loadMDMClass(String name, boolean createClassLoaderIfNotExisting, String originalXmlPath,
                                        String originalXmlName, Long revision) throws ClassNotFoundException {
    Class<XynaObject> c = loadMDMClassOwnRevision(name, createClassLoaderIfNotExisting, originalXmlPath, originalXmlName, revision);
    
    if (c != null) {
      return c;
    }
    
    //FIXME performance: cache verwenden, der alle klassen aus dem dependencies-teilbaum schneller durchsuchbar hat
    //      eine map name->classloader wäre z.b. gut
    Set<Long> requirements = new HashSet<Long>();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(revision, requirements);
    for (long req : requirements) {
      c = loadMDMClassOwnRevision(name, createClassLoaderIfNotExisting, originalXmlPath, originalXmlName, req);
      if (c != null) {
        return c;
      }
    }
    
    Long rootRevision = XynaOrderServerExtension.getThreadLocalRootRevision();
    if (rootRevision != null) {
      //es läuft gerade eine deserialisierung, und es ist zusätzlicher revision kontext bekannt
      if (requirements.contains(rootRevision)) {
        return null; //bereits getestet
      }
      if (rootRevision.equals(revision)) {
        return null; //bereits getestet
      }

      if (logger.isDebugEnabled()) {
        logger.debug("trying to load " + name + " from rootrevision " + rootRevision);
      }

      c = loadMDMClassOwnRevision(name, createClassLoaderIfNotExisting, originalXmlPath, originalXmlName, rootRevision);
      if (c != null) {
        return c;
      }

      Set<Long> requirementsRootRev = new HashSet<Long>();
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
          .getDependenciesRecursivly(rootRevision, requirementsRootRev);
      requirementsRootRev.removeAll(requirements); //bereits getestet
      requirementsRootRev.remove(revision); //bereits getestet
      for (long req : requirementsRootRev) {
        c = loadMDMClassOwnRevision(name, createClassLoaderIfNotExisting, originalXmlPath, originalXmlName, req);
        if (c != null) {
          return c;
        }
      }
    }
    
    return null;
  }

  @SuppressWarnings("unchecked")
  private Class<XynaObject> loadMDMClassOwnRevision(String name, boolean createClassLoaderIfNotExisting, String originalXmlPath,
                                        String originalXmlName, Long revision) throws ClassNotFoundException {
    
    boolean existsBefore;
    MapWrapper<String, ClassLoaderBase> mdmClassLoaderForRevision = classLoaderMap.get(ClassLoaderType.MDM).lazyCreateGet(revision);
    try {
      String potentialCLName = ClassLoaderBase.getBaseClassName(name);
      
      ClassLoaderBase mdmLoader = mdmClassLoaderForRevision.map.get(potentialCLName);
      existsBefore = mdmLoader != null;
      
      
      if (createClassLoaderIfNotExisting) {
        MDMClassLoader mcl;
        try {
          //FIXME macht das sinn hier keine shared libs anzugeben??
          mcl = getMDMClassLoaderLazyCreate(potentialCLName, new String[0], originalXmlPath, originalXmlName, revision);
        } catch (XFMG_SHARED_LIB_NOT_FOUND e1) {
          throw new RuntimeException(e1);
        }
        try {
          return (Class<XynaObject>) mcl.loadClass(name);
        } catch (ClassNotFoundException e) {
          if (!existsBefore) {
            mdmClassLoaderForRevision.map.remove(potentialCLName).closeFiles();
          }
        }
      } else if (existsBefore) {
        return (Class<XynaObject>) mdmLoader.loadClass(name);
      }
      return null;
    } finally {
      classLoaderMap.get(ClassLoaderType.MDM).cleanup(revision);
    }
  }


  @SuppressWarnings("unchecked")
  public Class<ConnectionFilter<TriggerConnection>> loadFilterClass(String fqFilterName, String fqClassNameToLoad,
                                                                    String fqTriggerClassName, String[] sharedLibs,
                                                                    Long revision, Long parentRevision)
                  throws ClassNotFoundException, XFMG_TriggerClassLoaderNotFoundException,
                  XFMG_SHARED_LIB_NOT_FOUND {

    return (Class<ConnectionFilter<TriggerConnection>>) getOutdatedFilterClassLoaderLazyCreate(fqFilterName,
                                                                                               fqTriggerClassName,
                                                                                               sharedLibs, revision,
                                                                                               parentRevision)
                    .loadClass(fqClassNameToLoad);
  }

  
  @SuppressWarnings("unchecked")
  public Class<ConnectionFilter<TriggerConnection>> loadFilterClass(String fqFilterName, String fqClassNameToLoad,
                                                                    String fqTriggerClassName, String[] sharedLibs, Long revision)
                  throws ClassNotFoundException, XFMG_TriggerClassLoaderNotFoundException,
                  XFMG_SHARED_LIB_NOT_FOUND {
    return (Class<ConnectionFilter<TriggerConnection>>) getFilterClassLoaderLazyCreate(fqFilterName,
                                                                                       fqTriggerClassName, sharedLibs,
                                                                                       revision)
                    .loadClass(fqClassNameToLoad);
  }


  public Class<?> loadTriggerClass(String fqTriggerClassName, String fqClassName, Long revision) throws ClassNotFoundException,
                  XynaException {
    return getTriggerClassLoaderLazyCreate(fqTriggerClassName, null, revision).loadClass(fqClassName);
  }


  /**
   * erstellt classloader falls nicht existent
   */
  public MDMClassLoader getMDMClassLoaderLazyCreate(String fqClassName, String[] sharedLibs, String originalXmlPath,
                                                    String originalXmlName, Long revision) throws XFMG_SHARED_LIB_NOT_FOUND {

    mdmLock.lock(); // lock, um zuverhindern, dass ClassLoader zweimal von unterschiedlichen Threads angelegt wird.
    try {
      MapWrapper<String, ClassLoaderBase> mdmClassLoaderForRevision = classLoaderMap.get(ClassLoaderType.MDM).lazyCreateGet(revision);
      try {
        MDMClassLoader existingResult = (MDMClassLoader) mdmClassLoaderForRevision.map.get(fqClassName);
        if (existingResult != null) {
          return existingResult;
        }
  
        List<String> tmp = new ArrayList<String>();
        for (int i = 0; i < sharedLibs.length; i++) {
          if(!sharedLibs[i].equals("")) {
            tmp.add(sharedLibs[i]);
          }
        }
        SharedLibClassLoader []sharedLibCLs = new SharedLibClassLoader[tmp.size() == 0 ? 1 : tmp.size()];
        RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        for(int i = 0; i < tmp.size(); i++) {
          Long rev = rcdm.getRevisionDefiningSharedLib(tmp.get(i), revision);
          if (rev == null) {
            throw new XFMG_SHARED_LIB_NOT_FOUND(tmp.get(i));
          }
          sharedLibCLs[i] = getSharedLibClassLoaderLazyCreate(tmp.get(i), rev);
        }
        if(tmp.size() == 0) {
          sharedLibCLs[0] = getOrCreateEmptySharedLibClassLoader(revision);
        }
        
        if (originalXmlName == null || originalXmlPath == null) {
          throw new RuntimeException("Cannot lazily create an MDM classloader for unknown original XML name and/or path");
        }
  
        MDMClassLoader mcl = new MDMClassLoader(fqClassName, sharedLibCLs, tmp.toArray(new String[0]), originalXmlPath, originalXmlName, revision);
        mdmClassLoaderForRevision.map.put(fqClassName, mcl);
        return mcl;
      } finally {
        classLoaderMap.get(ClassLoaderType.MDM).cleanup(revision);
      }
    } finally {
      mdmLock.unlock();
    }
  }
  
  /**
   * wirft fehler, wenn CL nicht existiert
   */
  public MDMClassLoader getMDMClassLoader(String fqClassName, Long revision, boolean allowDelegation) throws XFMG_MDMObjectClassLoaderNotFoundException {
    MDMClassLoader cl = findClassLoaderByType(fqClassName, revision, ClassLoaderType.MDM, allowDelegation);
    if (cl == null) {
      throw new XFMG_MDMObjectClassLoaderNotFoundException(fqClassName);
    }
    return cl;
  }
  
  /**
   * sucht classloader in der entsprechenden map oder falls nicht gefunden über die revision-dependencies 
   */
  public <T extends ClassLoaderBase> T findClassLoaderByType(String fqClassName, Long revision, ClassLoaderType type, boolean allowDelegation) {
    return findClassLoaderByType(fqClassName, revision, null, type, allowDelegation);
  }
  
  /**
   * sucht classloader in der entsprechenden map oder falls nicht gefunden über die revision-dependencies 
   */
  public <T extends ClassLoaderBase> T findClassLoaderByType(String fqClassName, Long revision, Long parentRevision, ClassLoaderType type, boolean allowDelegation) {
    if (allowDelegation && RuntimeContextDependencyManagement.getThreadLocalValueForDeserializationWithNewRevisions()) {
      T t = loadByThreadLocalRootOrderRevision(fqClassName, type, Collections.<Long>emptySet(), parentRevision);
      if (t != null) {
        return t;
      }
    }
    T mcl = (T) getClassLoaderByType(type, fqClassName, revision, parentRevision);
    if (mcl == null && allowDelegation) {
      Set<Long> requirements = new HashSet<Long>();
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
          .getDependenciesRecursivly(revision, requirements);
      for (long dep : requirements) {
         mcl = (T) getClassLoaderByType(type, fqClassName, dep, parentRevision);
         if (mcl != null) {
           return mcl;
         }
      }
      
      requirements.add(revision);
      return loadByThreadLocalRootOrderRevision(fqClassName,type, requirements, parentRevision);
    } else {
      return mcl;
    }
  }

  
  private  <T extends ClassLoaderBase> T loadByThreadLocalRootOrderRevision(String fqClassName, ClassLoaderType type, Set<Long> revisionsAlreadyTested, Long parentRevision) {
    Long rootRevision = XynaOrderServerExtension.getThreadLocalRootRevision();
    if (rootRevision != null) {
      //es läuft gerade eine deserialisierung, und es ist zusätzlicher revision kontext bekannt
      if (revisionsAlreadyTested.contains(rootRevision)) {
        return null; //bereits getestet
      }

      if (logger.isDebugEnabled()) {
        logger.debug("trying to find classloader " + fqClassName + " from rootrevision " + rootRevision);
      }

      T mcl = (T) getClassLoaderByType(type, fqClassName, rootRevision, parentRevision);
      if (mcl != null) {
        return mcl;
      }

      Set<Long> requirementsRootRev = new HashSet<Long>();
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
          .getDependenciesRecursivly(rootRevision, requirementsRootRev);
      requirementsRootRev.removeAll(revisionsAlreadyTested); //bereits getestet
      for (long req : requirementsRootRev) {
        mcl = (T) getClassLoaderByType(type, fqClassName, req, parentRevision);
        if (mcl != null) {
          return mcl;
        }
      }
    }
    return null;
  }


  /**
  * wirft fehler, wenn CL nicht existiert
  */
  public SharedLibClassLoader getSharedLibClassLoader(String name, Long revision, boolean allowDelegation) throws XFMG_SharedLibClassLoaderNotFoundException {
    SharedLibClassLoader cl = findClassLoaderByType(name, revision, ClassLoaderType.SharedLib, allowDelegation);
    if (cl == null) {
      throw new XFMG_SharedLibClassLoaderNotFoundException(name);
    }
    return cl;
  }

 
  public SharedLibClassLoader getSharedLibClassLoaderLazyCreate(String name, Long revision) throws XFMG_SHARED_LIB_NOT_FOUND {
    sharedLibLock.lock();
    try {
      if("".equals(name)) {
        return getOrCreateEmptySharedLibClassLoader(revision);
      }

      MapWrapper<String, ClassLoaderBase> sharedLibClassLoaderForRevision =
                      classLoaderMap.get(ClassLoaderType.SharedLib).lazyCreateGet(revision);
      try {
        SharedLibClassLoader scl = (SharedLibClassLoader) sharedLibClassLoaderForRevision.map.get(name);
        if (scl == null) {
          scl = new SharedLibClassLoader(name, revision);
          sharedLibClassLoaderForRevision.map.put(name, scl);
        }
        return scl;
      } finally {
        classLoaderMap.get(ClassLoaderType.SharedLib).cleanup(revision);
      }
    } finally {
      sharedLibLock.unlock();
    }
  }


  /**
   * wirft fehler, wenn CL nicht existiert
   */
  public WFClassLoader getWFClassLoader(String fqClassName, Long revision, boolean allowDelegation) throws XFMG_WFClassLoaderNotFoundException {
    WFClassLoader wcl = findClassLoaderByType(fqClassName, revision, ClassLoaderType.WF, allowDelegation);
    if (wcl == null) {
      throw new XFMG_WFClassLoaderNotFoundException(fqClassName);
    }
    return wcl;
  }


  public WFClassLoader getWFClassLoaderLazyCreate(String fqClassName, String originalXmlPath, String originalXmlName, Long revision){
    wfLock.lock();
    try {
      MapWrapper<String, ClassLoaderBase> wfClassLoaderForRevision = classLoaderMap.get(ClassLoaderType.WF).lazyCreateGet(revision);
      try {
        WFClassLoader wcl = (WFClassLoader) wfClassLoaderForRevision.map.get(fqClassName);
        if(wcl == null) {
          wcl = new WFClassLoader(fqClassName, originalXmlPath, originalXmlName, revision);
          wfClassLoaderForRevision.map.put(fqClassName, wcl);
        }
        return wcl;
      } finally {
        classLoaderMap.get(ClassLoaderType.WF).cleanup(revision);
      }
    } finally {
      wfLock.unlock();
    }
  }


  public void removeMDMClassLoader(String fqClassName, Long revision) {
    ClassLoaderBase clb = removeClassLoaderFromMap(revision, fqClassName, ClassLoaderType.MDM);
    notifyAutomaticUndeploymentHandlers(fqClassName, revision);
    if (clb != null) {
      clb.closeFiles();
    }
  }


  public void removeExceptionClassLoader(String fqClassName, Long revision) {
    ClassLoaderBase clb = removeClassLoaderFromMap(revision, fqClassName, ClassLoaderType.Exception);
    notifyAutomaticUndeploymentHandlers(fqClassName, revision);
    if (clb != null) {
      clb.closeFiles();
    }
  }


  public void removeWFClassLoader(String fqClassName, Long revision) {
    ClassLoaderBase wfcl = removeClassLoaderFromMap(revision, fqClassName, ClassLoaderType.WF);
    if (wfcl != null && wfcl.hasBeenUsed()) {
      //FIXME wieso nur wenn != null und wenn used? 
      notifyAutomaticUndeploymentHandlers(fqClassName, revision);
    }
    if (wfcl != null) {
      wfcl.closeFiles();
    }
  }


  public void removeFilterClassLoader(String fqFilterClassName, Long revision) {
    ClassLoaderBase filterClassLoader = removeClassLoaderFromMap(revision, fqFilterClassName, ClassLoaderType.Filter);
    if (filterClassLoader != null) {
      removeFromAllDependenciesToReloadInOtherClassLoaders(filterClassLoader.getClassLoaderID(), filterClassLoader.getType(), filterClassLoader.getRevision(), true);
    }
    notifyAutomaticUndeploymentHandlers(fqFilterClassName, revision);
    if (filterClassLoader != null) {
      filterClassLoader.closeFiles();
    }
  }


  public void removeOutdatedFilterClassLoaders(String fqFilterClassName, Long revision) {
    for (OutdatedFilterRevision rev : outdatedFilterClassLoader.keySet()) {
      FilterClassLoader cl = null;
      if (revision.equals(rev.getRevision()) || revision.equals(rev.getParentRevision())) {
        cl = removeOutdatedFilterClassLoaderFromMap(rev, fqFilterClassName);
        
        if (cl != null) {
          removeFromAllDependenciesToReloadInOtherClassLoaders(cl.getClassLoaderID(), cl.getType(), cl.getRevision(), cl.getParentRevision(), true);
        }
        
        notifyAutomaticUndeploymentHandlers(fqFilterClassName, revision);
        if (cl != null) {
          cl.closeFiles();
        }
      }
    }
  }


  public void removeTriggerClassLoader(String fqTriggerClassName, Long revision) {
    ClassLoaderBase triggerClassLoader = removeClassLoaderFromMap(revision, fqTriggerClassName, ClassLoaderType.Trigger);
    if (logger.isDebugEnabled()) {
      logger.debug("removed trigger classloader " + triggerClassLoader);
    }
    if (triggerClassLoader != null) {
      removeFromAllDependenciesToReloadInOtherClassLoaders(triggerClassLoader.getClassLoaderID(), triggerClassLoader.getType(), triggerClassLoader.getRevision(), true);
    }
    notifyAutomaticUndeploymentHandlers(fqTriggerClassName, revision);
    if (triggerClassLoader != null) {
      triggerClassLoader.closeFiles();
    }
  }


  public void removeSharedLibClassLoader(String sharedLibName, Long revision) {
    if (sharedLibName == null) {
      sharedLibName = NAME_OF_EMPTY_SHARED_LIB;
    }
    ClassLoaderBase sharedLibClassLoader = removeClassLoaderFromMap(revision, sharedLibName, ClassLoaderType.SharedLib);
    if (sharedLibClassLoader != null) {
      removeFromAllDependenciesToReloadInOtherClassLoaders(sharedLibClassLoader.getClassLoaderID(),
                                                           sharedLibClassLoader.getType(),
                                                           sharedLibClassLoader.getRevision(),
                                                           true);
    }
    notifyAutomaticUndeploymentHandlers(sharedLibName, revision);
    if (sharedLibClassLoader != null) {
      sharedLibClassLoader.closeFiles();
    }
  }

  /**
   * wirft fehler, wenn CL nicht existiert
   */
  public FilterClassLoader getFilterClassLoader(String fqFilterClassName, Long revision, boolean allowDelegation)
      throws XFMG_FilterClassLoaderNotFoundException {
    FilterClassLoader cl = findClassLoaderByType(fqFilterClassName, revision, ClassLoaderType.Filter, allowDelegation);
    if (cl == null) {
      throw new XFMG_FilterClassLoaderNotFoundException(fqFilterClassName);
    }
    return cl;
  }

  private FilterClassLoader getOutdatedFilterClassLoader(String fqFilterClassName, Long revision, Long parentRevision) {
    OutdatedFilterRevision rev = new OutdatedFilterRevision(revision, parentRevision);
    MapWrapper<String, FilterClassLoader> classLoaderForRevision = outdatedFilterClassLoader.lazyCreateGet(rev);
    try {
      return classLoaderForRevision.map.get(fqFilterClassName);
    } finally {
      outdatedFilterClassLoader.cleanup(rev);
    }
  }

  private ClassLoaderBase getClassLoaderFromMap(String fqClassName, Long revision, ClassLoaderType clt) {
    MapWrapper<String, ClassLoaderBase> classLoaderForRevision = classLoaderMap.get(clt).lazyCreateGet(revision);
    try {
      return classLoaderForRevision.map.get(fqClassName);
    } finally {
      classLoaderMap.get(clt).cleanup(revision);
    }
  }


  private FilterClassLoader removeOutdatedFilterClassLoaderFromMap(OutdatedFilterRevision rev, String fqFilterClassName) {
    MapWrapper<String, FilterClassLoader> classLoaderForRevision = outdatedFilterClassLoader.lazyCreateGet(rev);
    try {
      return classLoaderForRevision.map.remove(fqFilterClassName);
    } finally {
      outdatedFilterClassLoader.cleanup(rev);
    }
  }
  
  private ClassLoaderBase removeClassLoaderFromMap(Long revision, String fqClassName, ClassLoaderType clt) {
    MapWrapper<String, ClassLoaderBase> classLoaderForRevision = classLoaderMap.get(clt).lazyCreateGet(revision);
    try {
      return classLoaderForRevision.map.remove(fqClassName);
    } finally {
      classLoaderMap.get(clt).cleanup(revision);
    }
  }

  
  public FilterClassLoader getOutdatedFilterClassLoaderLazyCreate(String fqFilterClassName, String fqTriggerClassName,
                                                          String[] sharedLibs, Long revision, Long parentRevision) throws XFMG_TriggerClassLoaderNotFoundException, XFMG_SHARED_LIB_NOT_FOUND {

    outdatedFilterLock.lock(); // lock um gleichzeitig Erstellung von FilterClassLoadern von unterschiedlichen Threads zuverhindern
    try {
      OutdatedFilterRevision rev = new OutdatedFilterRevision(revision, parentRevision);
      MapWrapper<String, FilterClassLoader> classLoaderForRevision = outdatedFilterClassLoader.lazyCreateGet(rev);
      try {
        FilterClassLoader fcl = classLoaderForRevision.map.get(fqFilterClassName);
        if(fcl != null) {
          return fcl;
        }
       
        TriggerClassLoader tcl;
        tcl = getTriggerClassLoader(fqTriggerClassName, parentRevision, true);
        
        SharedLibClassLoader[] scls = new SharedLibClassLoader[sharedLibs.length];
        for (int i = 0; i < scls.length; i++) {
          scls[i] = getSharedLibClassLoaderLazyCreate(sharedLibs[i], revision);
        }
        
        fcl = new FilterClassLoader(fqFilterClassName, tcl, scls, fqTriggerClassName, revision);
        
        classLoaderForRevision.map.put(fqFilterClassName, fcl);
        return fcl;
      } finally {
        outdatedFilterClassLoader.cleanup(rev);
      }
    } finally {
      outdatedFilterLock.unlock();
    }
  }
  
  
  
  public FilterClassLoader getFilterClassLoaderLazyCreate(String fqFilterClassName, String fqTriggerClassName,
                                                          String[] sharedLibs, Long revision) throws XFMG_TriggerClassLoaderNotFoundException, XFMG_SHARED_LIB_NOT_FOUND {

    filterLock.lock(); // lock um gleichzeitig Erstellung von FilterClassLoadern von unterschiedlichen Threads zuverhindern
    try {
      MapWrapper<String, ClassLoaderBase> filterClassLoaderForRevision = classLoaderMap.get(ClassLoaderType.Filter).lazyCreateGet(revision);
      try {
        FilterClassLoader fcl = (FilterClassLoader) filterClassLoaderForRevision.map.get(fqFilterClassName);
        if(fcl != null) {
          return fcl;
        }
        
        TriggerClassLoader tcl = getTriggerClassLoader(fqTriggerClassName, revision, true);
        
        List<SharedLibClassLoader> scls = new ArrayList<SharedLibClassLoader>();
        RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        for (int i = 0; i < sharedLibs.length; i++) {
          if(!sharedLibs[i].equals("")) {
            Long rev = rcdm.getRevisionDefiningSharedLib(sharedLibs[i], revision);
            if (rev == null) {
              throw new XFMG_SHARED_LIB_NOT_FOUND(sharedLibs[i]);
            }
            scls.add(getSharedLibClassLoaderLazyCreate(sharedLibs[i], rev));
          }
        }
        
        fcl = new FilterClassLoader(fqFilterClassName, tcl, scls.toArray(new SharedLibClassLoader[0]), fqTriggerClassName, revision);
        
        filterClassLoaderForRevision.map.put(fqFilterClassName, fcl);
        return fcl;
      } finally {
        classLoaderMap.get(ClassLoaderType.Filter).cleanup(revision);
      }
    } finally {
      filterLock.unlock();
    }
  }

  public void removeDataModelTypeClassLoader(String datamodeltype) {
    MapWrapper<String, ClassLoaderBase> dataModelTypeClassLoaderForRevision =
        classLoaderMap.get(ClassLoaderType.DataModelType).lazyCreateGet(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    try {
      dataModelTypeClassLoaderForRevision.map.remove(datamodeltype);
    } finally {
      classLoaderMap.get(ClassLoaderType.DataModelType).cleanup(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    }
  }

  public DataModelTypeClassLoader getDataModelTypeClassLoaderLazyCreate(String datamodeltype) throws XFMG_JarFolderNotFoundException {
    MapWrapper<String, ClassLoaderBase> dataModelTypeClassLoaderForRevision =
        classLoaderMap.get(ClassLoaderType.DataModelType).lazyCreateGet(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    try {
      DataModelTypeClassLoader dmtcl = (DataModelTypeClassLoader) dataModelTypeClassLoaderForRevision.map.get(datamodeltype);
      if (dmtcl != null) {
        return dmtcl;
      }
      dmtcl = new DataModelTypeClassLoader(datamodeltype);

      dataModelTypeClassLoaderForRevision.map.put(datamodeltype, dmtcl);

      return dmtcl;
    } finally {
      classLoaderMap.get(ClassLoaderType.DataModelType).cleanup(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    }
  }
  
  private void notifyAutomaticUndeploymentHandlers(String className, Long revision) {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getAutomaticUnDeploymentHandlerManager()
                    .notifyUndeployment(className, revision);
  }


  public void removeRemoteDestinationTypeClassLoader(String remotedestinationtype) {
    MapWrapper<String, ClassLoaderBase> remoteDestinationTypeClassLoaderForRevision =
        classLoaderMap.get(ClassLoaderType.RemoteDestinationType).lazyCreateGet(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    try {
      remoteDestinationTypeClassLoaderForRevision.map.remove(remotedestinationtype);
    } finally {
      classLoaderMap.get(ClassLoaderType.RemoteDestinationType).cleanup(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    }
  }

  public RemoteDestinationTypeClassLoader getRemoteDestinationTypeClassLoaderLazyCreate(String remotedestinationtype) throws XFMG_JarFolderNotFoundException {
    MapWrapper<String, ClassLoaderBase> remoteDestinationTypeClassLoaderForRevision =
        classLoaderMap.get(ClassLoaderType.RemoteDestinationType).lazyCreateGet(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    try {
      RemoteDestinationTypeClassLoader dmtcl = (RemoteDestinationTypeClassLoader) remoteDestinationTypeClassLoaderForRevision.map.get(remotedestinationtype);
      if (dmtcl != null) {
        return dmtcl;
      }
      dmtcl = new RemoteDestinationTypeClassLoader(remotedestinationtype);

      remoteDestinationTypeClassLoaderForRevision.map.put(remotedestinationtype, dmtcl);

      return dmtcl;
    } finally {
      classLoaderMap.get(ClassLoaderType.RemoteDestinationType).cleanup(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    }
  }
  
  
  /**
   * wirft fehler, wenn CL nicht existiert
   */
  public TriggerClassLoader getTriggerClassLoader(String fqTriggerClassName, Long revision, boolean allowDelegation) throws XFMG_TriggerClassLoaderNotFoundException {
    TriggerClassLoader cl = findClassLoaderByType(fqTriggerClassName, revision, ClassLoaderType.Trigger, allowDelegation);
    if (cl == null) {
      throw new XFMG_TriggerClassLoaderNotFoundException(fqTriggerClassName);
    }
    return cl;
  }


  public TriggerClassLoader getTriggerClassLoaderLazyCreate(String fqTriggerClassName, String[] sharedLibs,
                                                            Long revision) throws XFMG_SHARED_LIB_NOT_FOUND {

    triggerLock.lock();
    try {
      MapWrapper<String, ClassLoaderBase> classLoaderForRevision = classLoaderMap.get(ClassLoaderType.Trigger).lazyCreateGet(revision);
      try {
        TriggerClassLoader tcl = (TriggerClassLoader) classLoaderForRevision.map.get(fqTriggerClassName);
        if (tcl != null) {
          return tcl;
        }

        if (sharedLibs == null) {
          sharedLibs = new String[0];
        }
        List<String> tmp = new ArrayList<String>();
        for (int i = 0; i < sharedLibs.length; i++) {
          if (!sharedLibs[i].equals("")) {
            tmp.add(sharedLibs[i]);
          }
        }
        RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        SharedLibClassLoader[] sharedLibCLs = new SharedLibClassLoader[tmp.size() == 0 ? 1 : tmp.size()];
        for(int i = 0; i < tmp.size(); i++) {
          Long rev = rcdm.getRevisionDefiningSharedLib(tmp.get(i), revision);
          if (rev == null) {
            throw new XFMG_SHARED_LIB_NOT_FOUND(tmp.get(i));
          }
          sharedLibCLs[i] = getSharedLibClassLoaderLazyCreate(tmp.get(i), rev);
        }
        if (tmp.size() == 0) {
          sharedLibCLs[0] = getOrCreateEmptySharedLibClassLoader(revision);
        }

        tcl = new TriggerClassLoader(fqTriggerClassName, sharedLibCLs, tmp.toArray(new String[0]), revision);
        if (logger.isDebugEnabled()) {
          logger.debug("created trigger classloader " + tcl);
        }
        classLoaderForRevision.map.put(fqTriggerClassName, tcl);

        return tcl;
      } finally {
        classLoaderMap.get(ClassLoaderType.Trigger).cleanup(revision);
      }
    } finally {
      triggerLock.unlock();
    }
  }


  /**
   * in dieser methode werden classloader (falls vorhanden) durch neue ersetzt. es sollten keine
   * anderen classloader ausser dem übergebenen berührt werden (zb implizit durch deployment/instanziierung
   * von zugehörigen mdmobjekten oder sowas).
   * die parent-classloader, registrierte urls und registrierte dependencies bleiben erhalten.
   */
  public void reloadClassLoaderByType(ClassLoaderType clt, String className, Long revision) throws XFMG_ClassLoaderNotFoundException {
    reloadClassLoaderByType(clt, className, revision, null);
  }
  
  public void reloadClassLoaderByType(ClassLoaderType clt, String className, Long revision, Long parentRevision) throws XFMG_ClassLoaderNotFoundException {
    // hier braucht man nichts locken, weil das vom reload aufgerufen wird, und darum wird alles gelockt

    if (logger.isDebugEnabled()) {
      logger.debug("reloading classloader by type ### " + className + " ###  type: " + clt + " ### in rev " + revision);
    }

    ClassLoaderBase xclOld = null;
    ClassLoaderBase xclNew = null;
    
    if (clt == ClassLoaderType.MDM) {
      xclOld = removeClassLoaderFromMap(revision, className, clt);
      if (xclOld != null) {
        MDMClassLoader mdmClOld = (MDMClassLoader) xclOld;
        try {
          xclNew = getMDMClassLoaderLazyCreate(className, mdmClOld.getSharedLibs(), mdmClOld.getOriginalXmlPath(),
                                               mdmClOld.getOriginalXmlName(), revision);
        } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
          throw new RuntimeException(e);
        }
      }
    } else if (clt == ClassLoaderType.Exception) {
      xclOld = removeClassLoaderFromMap(revision, className, clt);
      if (xclOld != null) {
        xclNew = getExceptionClassLoaderLazyCreate(className, ((ExceptionClassLoader) xclOld).getOriginalXmlPath(),
                                                 ((ExceptionClassLoader) xclOld).getOriginalXmlName(), revision);
      }
    } else if (clt == ClassLoaderType.Trigger) {
      xclOld = removeClassLoaderFromMap(revision, className, clt);
      if (xclOld != null) {
        try {
          xclNew = getTriggerClassLoaderLazyCreate(className, ((TriggerClassLoader) xclOld).getSharedLibs(), revision);
        } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
          throw new RuntimeException(e);
        }
      }
    } else if (clt == ClassLoaderType.Filter) {
      xclOld = removeClassLoaderFromMap(revision, className, clt);
      if (xclOld != null) {
        FilterClassLoader fclOld = (FilterClassLoader) xclOld;
        try {
          xclNew = getFilterClassLoaderLazyCreate(className, fclOld.getFQTriggerClass(), fclOld.getSharedLibs(), revision);
        } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
          throw new RuntimeException(e);
        }
      }
    } else if(clt == ClassLoaderType.OutdatedFilter) {
      OutdatedFilterRevision rev = new OutdatedFilterRevision(revision, parentRevision);
      xclOld = removeOutdatedFilterClassLoaderFromMap(rev, className);
      if (xclOld != null) {
        try {
          FilterClassLoader fclOld = (FilterClassLoader) xclOld;
          xclNew = getOutdatedFilterClassLoaderLazyCreate(className, fclOld.getFQTriggerClass(),
                                                          fclOld.getSharedLibs(), revision, fclOld.getParentRevision());
        } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
          throw new RuntimeException(e);
        }
      }
    } else if (clt == ClassLoaderType.SharedLib) {
      xclOld = removeClassLoaderFromMap(revision, className, clt);
      try {
        xclNew = getSharedLibClassLoaderLazyCreate(className, revision);
      } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
        throw new RuntimeException(e);
      }
    } else if (clt == ClassLoaderType.WF) {
      xclOld = removeClassLoaderFromMap(revision, className, clt);
      if (xclOld != null) {
        WFClassLoader wfxclOld = (WFClassLoader) xclOld;
        // wf neu deployen
        xclNew = getWFClassLoaderLazyCreate(className, wfxclOld.getOriginalXmlPath(), wfxclOld.getOriginalXmlName(), revision);
      }
    }
    if (xclOld == null || xclNew == null) {
      if (xclOld == null) {
        if (clt == ClassLoaderType.MDM) {
          throw new XFMG_MDMObjectClassLoaderNotFoundException(className);
        } else if (clt == ClassLoaderType.WF) {
          throw new XFMG_WFClassLoaderNotFoundException(className);
        } else if (clt == ClassLoaderType.Exception) {
          throw new XFMG_ExceptionClassLoaderNotFoundException(className);
        } else if (clt == ClassLoaderType.SharedLib) {
          throw new XFMG_SharedLibClassLoaderNotFoundException(className);
        } else if (clt == ClassLoaderType.Trigger) {
          throw new XFMG_TriggerClassLoaderNotFoundException(className);
        } else if (clt == ClassLoaderType.Filter || clt == ClassLoaderType.OutdatedFilter) {
          throw new XFMG_FilterClassLoaderNotFoundException(className);
        }
      } 
      throw new RuntimeException("new classloader has not been created for object " + className + " of type " + clt.toString());      
    }
    URL[] urls = xclOld.getURLs();
    xclNew.addURLs(urls);
    // dependencies erhalten
    xclNew.copyDependencies(xclOld, false);
    xclOld.closeFiles();

    if (logger.isDebugEnabled()) {
      logger.debug("reloaded " + clt + " >>>>" + className + "<<<<");
    }

  }
  
  
  /**
   * Über ClassLoaderBuilder kann ein neuer ClassLoader angelegt werden, falls er noch nicht existiert
   */
  public interface ClassLoaderBuilder {
    
    String getId();

    ClassLoaderBase createClassLoader();
  }

  public ClassLoaderBase getOrCreateClassLoaderByType(ClassLoaderType clt, Long revision, ClassLoaderBuilder clb) {
    ClassLoaderBase cl = getClassLoaderByType(clt, clb.getId(), revision, null);
    if( cl == null ) {
      cl = createClassLoader(clt, revision, clb);
    }
    return cl;
  }

  private ClassLoaderBase createClassLoader(ClassLoaderType clt, Long revision, ClassLoaderBuilder clb) {
    ConcurrentMap<String, ClassLoaderBase> map = classLoaderMap.get(clt).lazyCreateGet(revision).map;
    try {
      map.putIfAbsent(clb.getId(), clb.createClassLoader());
      return map.get(clb.getId()); 
    } finally {
      classLoaderMap.get(clt).cleanup(revision);
    }
  }


  public ClassLoaderBase getClassLoaderByType(ClassLoaderType clt, String classLoaderName, Long revision) {
    return getClassLoaderByType(clt, classLoaderName, revision, null);
  }

  /**
   * gibt classloader aus entsprechender map zurück 
   */
  public ClassLoaderBase getClassLoaderByType(ClassLoaderType clt, String classLoaderName, Long revision,
                                              Long parentRevision) {
    ClassLoaderBase xcl = null;
    switch (clt) {
      case MDM :
      case Trigger :
      case Filter :
      case SharedLib :
      case WF :
      case Exception :
        xcl = getClassLoaderFromMap(classLoaderName, revision, clt);
        break;
      case OutdatedFilter:
        xcl = getOutdatedFilterClassLoader(classLoaderName, revision, parentRevision);
        break;
      case PersistenceLayer :
        xcl = persistenceLayerClassLoader.get(classLoaderName);
        break;
      case RMI : 
        xcl = getClassLoaderFromMap(classLoaderName, revision, clt);
        break;
      case ClusterProvider ://fall through
      case XYNA :
        throw new IllegalArgumentException("Cannot process " + clt + " classloader");
      default :
        throw new IllegalArgumentException("Unknown ClassLoaderType: " + clt);
    }
    return xcl;
  }
  
  public Class<?> loadClassWithClassLoader(ClassLoaderType clt, String classLoaderName, String fqClassName, Long revision) throws ClassNotFoundException {
    return getClassLoaderByType(clt, classLoaderName, revision).loadClass(fqClassName);
  }


  private void redeployRMIImpls() {
    ConcurrentMap<String, ClassLoaderBase> map = classLoaderMap.get(ClassLoaderType.RMI).lazyCreateGet(VersionManagement.REVISION_WORKINGSET).map;
    try {
      for( Map.Entry<String, ClassLoaderBase> entry : map.entrySet() ) {
        ClassLoaderBase cl = entry.getValue();
        if( cl instanceof ReplaceableClassLoader ) {
          map.replace( entry.getKey(), cl, ((ReplaceableClassLoader)cl).replace() );
        }
      }
    } finally {
      classLoaderMap.get(ClassLoaderType.RMI).cleanup(VersionManagement.REVISION_WORKINGSET);
    }
    
    // rmi könnte eine beliebige mdm klasse erzeugen => immer redeploy!
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement().redeployRMIImpls();
    } catch (XMCP_RMI_BINDING_ERROR e) {
      throw new RuntimeException(e);
    }
  }


  public void reloadAllSharedLibs(Long revision) throws XFMG_ClassLoaderRedeploymentException {

    MapWrapper<String, ClassLoaderBase> sharedLibClassLoaderForRevision = classLoaderMap.get(ClassLoaderType.SharedLib).lazyCreateGet(revision);
    try {
      Set<String> allNames = new HashSet<String>(sharedLibClassLoaderForRevision.map.keySet());
      for (String name : allNames) {
        if (!NAME_OF_EMPTY_SHARED_LIB.equals(name)) {
          try {
            reloadSharedLibInternally(name, false, sharedLibClassLoaderForRevision, revision, null);
          } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
            throw new RuntimeException("Shared lib got lost during reload of all shared libs", e);
          } catch (OrderEntryInterfacesCouldNotBeClosedException e) {
            throw new RuntimeException("Could not disable Trigger", e);
          }
        }
      }
      redeployRMIImpls();
    } finally {
      classLoaderMap.get(ClassLoaderType.SharedLib).cleanup(revision);
    }
  }

  
  public void reloadSharedLib(String name, Long revision) throws XFMG_ClassLoaderRedeploymentException,
      XFMG_SHARED_LIB_NOT_FOUND, OrderEntryInterfacesCouldNotBeClosedException {
    reloadSharedLib(name, revision, null);
  }
    
  public void reloadSharedLib(String name, Long revision, ClassLoaderSwitcher cls) throws XFMG_ClassLoaderRedeploymentException,
    XFMG_SHARED_LIB_NOT_FOUND, OrderEntryInterfacesCouldNotBeClosedException {
    MapWrapper<String, ClassLoaderBase> sharedLibClassLoaderForRevision = classLoaderMap.get(ClassLoaderType.SharedLib).lazyCreateGet(revision);
    try {
      reloadSharedLibInternally(name, true, sharedLibClassLoaderForRevision, revision, cls);
    } finally {
      classLoaderMap.get(ClassLoaderType.SharedLib).cleanup(revision);
    }
  }


  private void reloadSharedLibInternally(final String name, boolean reloadRMI,
                                         final MapWrapper<String, ClassLoaderBase> sharedLibClassLoaderForRevision,
                                         final Long revision, ClassLoaderSwitcher cls) throws XFMG_ClassLoaderRedeploymentException, XFMG_SHARED_LIB_NOT_FOUND, OrderEntryInterfacesCouldNotBeClosedException {
    //während einem reloadSharedLib dürfen die Trigger und Filter nicht enabled werden
    //daher hier die entsprechenden Methoden sperren
    Pair<Operation, Operation> failure =
                    CommandControl.wlock(CommandControl.Operation.SHAREDLIB_RELOAD,
                                         operationsToLockForReloadSharedLib, revision);
    if (failure != null) {
      throw new RuntimeException(failure.getFirst() + " could not be locked because it is locked by another process of type "
                 + failure.getSecond() + ".");
    }
    
    try {
      SharedLibClassLoader scl = (SharedLibClassLoader) sharedLibClassLoaderForRevision.map.get(name);
      if (scl != null) {
        XynaFactory.getInstance().getActivation().getActivationTrigger().lockManagement();
        try {
          //Trigger und Filter disablen
          RevisionOrderControl roc = new RevisionOrderControl(revision);
          roc.disableTriggerInstances(null, true);

          //eigentliches reload
          for (Lock lock : allLocksAsWriteLockInOrderForDeadLockSecurity) {
            lock.lock();
          }
          try {
            ClassLoaderSwitcher switcher;
            if (cls == null) {
              switcher = ClassLoaderBase.NOP_CLASSLOADERSWITCHER;
            } else {
              switcher = cls;
            }
            try {
              scl.reloadDependencies(switcher);
            } catch (MultipleExceptions e) {
              throw new XFMG_ClassLoaderRedeploymentException(ClassLoaderType.SharedLib.name(), name, e);
            }
          } finally {
            for (int i = allLocksAsWriteLockInOrderForDeadLockSecurity.length - 1; i >= 0; i--) {
              allLocksAsWriteLockInOrderForDeadLockSecurity[i].unlock();
            }
          }

          //Trigger und Filter wieder enablen
          roc.enablePreviouslyDisabledTriggerInstances();

          if (reloadRMI) {
            redeployRMIImpls();
          }
        } finally {
          XynaFactory.getInstance().getActivation().getActivationTrigger().unlockManagement();
        }
      } else {
        throw new XFMG_SHARED_LIB_NOT_FOUND(name);
      }
    } finally {
      CommandControl.wunlock(operationsToLockForReloadSharedLib, revision);
    }
  }
  
  
  /**
   * läd abhängige filter classloader auch neu
   */
  public void reloadTrigger(final String fqTriggerClassName, final String[] sharedLibs, final Long revision) throws XFMG_ClassLoaderRedeploymentException {
    
    ClassLoaderBase tcl = getClassLoaderFromMap(fqTriggerClassName, revision, ClassLoaderType.Trigger);
    
    if (tcl != null) {
      
      // alles locken während des reloads 
      for (Lock lock : allLocksAsWriteLockInOrderForDeadLockSecurity) {
        lock.lock();
      }
      try {
        tcl.reloadDependencies(new ClassLoaderSwitcher() {

          public void switchClassLoader() throws XynaException {
            ClassLoaderBase tclRemoved = removeClassLoaderFromMap(revision, fqTriggerClassName, ClassLoaderType.Trigger);
            // neue shared libs setzen, klasse wird bei reloaddependencies evtl neu gebaut.
            TriggerClassLoader newTcl = getTriggerClassLoaderLazyCreate(fqTriggerClassName, sharedLibs, revision);
            if (logger.isDebugEnabled()) {
              logger.debug("new trigger classloader = " + newTcl + ", old trigger classloader = " + tclRemoved);
            }
            // dependencies nach "oben" ändern sich nicht
            newTcl.copyDependencies(tclRemoved, true);
            tclRemoved.closeFiles();
          }
          
        });
      } catch (MultipleExceptions e) {
        throw new XFMG_ClassLoaderRedeploymentException(ClassLoaderType.Trigger.name(), fqTriggerClassName, e);
      } finally {
        for (int i = allLocksAsWriteLockInOrderForDeadLockSecurity.length - 1; i >= 0; i--) {
          allLocksAsWriteLockInOrderForDeadLockSecurity[i].unlock();
        }
      }
    }

    notifyAutomaticUndeploymentHandlers(fqTriggerClassName, revision);

  }

  
  
  private void traceClassLoaderState() {
    if (logger.isTraceEnabled()) {
      logger.trace("--------------- classloader state:");
      StringBuilder sb = new StringBuilder();
      buildClassloaderTrace(sb);
      logger.trace(sb.toString());
    }
  }
  

  private void buildClassloaderTrace(StringBuilder buf) {
    for (Entry<ClassLoaderType, ClassLoaderMap> t : classLoaderMap.entrySet()) {
      if (t.getKey() == ClassLoaderType.Filter || t.getKey() == ClassLoaderType.Trigger
                      || t.getKey() == ClassLoaderType.MDM || t.getKey() == ClassLoaderType.WF
                      || t.getKey() == ClassLoaderType.Exception || t.getKey() == ClassLoaderType.SharedLib
                      || t.getKey() == ClassLoaderType.RMI ) {
        for (Long revision : new TreeSet<Long>(t.getValue().keySet())) {
          traceClassLoaderState(t.getValue().get(revision).map, t.getKey().toString() + "s", buf);
        }
      }
    }
    
    //OutdatedFilter
    for (OutdatedFilterRevision revision : outdatedFilterClassLoader.keySet()) {
      MapWrapper<String, FilterClassLoader> classLoaderForRevision = outdatedFilterClassLoader.lazyCreateGet(revision);
      try {
        traceClassLoaderState(classLoaderForRevision.map, ClassLoaderType.OutdatedFilter.toString() + "s", buf);
      } finally {
        outdatedFilterClassLoader.cleanup(revision);
      }
    }
    
    ClassLoaderBase.traceBackuppedDependencies(buf);
  }


  public StringBuffer getClassLoaderTrace() {
    StringBuilder sb = new StringBuilder();
    buildClassloaderTrace(sb);
    return new StringBuffer(sb);
  }

  private void buildClassloaderTrace(StringBuilder buf, ClassLoaderType type) {
    for (Entry<ClassLoaderType, ClassLoaderMap> t : classLoaderMap.entrySet()) {
      if (t.getKey() == type ) {
        for (Long revision : new TreeSet<Long>(t.getValue().keySet())) {
          traceClassLoaderState(t.getValue().get(revision).map, t.getKey().toString() + "s", buf);
        }
      }
    }
  }
  
  public StringBuilder getClassLoaderTrace(ClassLoaderType type) {
    StringBuilder sb = new StringBuilder();
    buildClassloaderTrace(sb, type);
    return sb;
  }
  
  private static final Comparator<Entry<String, ? extends ClassLoaderBase>> COMPARATOR_CLASSLOADER_ENTRY = new Comparator<Entry<String, ? extends ClassLoaderBase>>() {

    @Override
    public int compare(Entry<String, ? extends ClassLoaderBase> o1, Entry<String, ? extends ClassLoaderBase> o2) {
      //erst revision, dann name
      int c = Long.compare(o1.getValue().getRevision(), o2.getValue().getRevision());
      if (c == 0) {
        return o1.getKey().compareTo(o2.getKey());
      }
      return c;
    }
    
  };


  private static void traceClassLoaderState(Map<String, ? extends ClassLoaderBase> cls, String info,
                                            StringBuilder buf) {
    buf.append("").append(info).append("\n");

    List<Entry<String, ? extends ClassLoaderBase>> l = new ArrayList<Entry<String, ? extends ClassLoaderBase>>(cls.entrySet());
    Collections.sort(l, COMPARATOR_CLASSLOADER_ENTRY);
    for (Entry<String, ? extends ClassLoaderBase> entry : l) {
      String name = entry.getKey();
      ClassLoaderBase clb = entry.getValue();
      buf.append(" o ").append(name).append(" id=\"").append(clb.getClassLoaderID()).append("\" cl=\"").append(clb)
                      .append("\" revision=\"").append(clb.getRevision());
      if (clb.getParentRevision() != null) {
        buf.append("\" parentRevision=\"").append(clb.getParentRevision());
      }
      buf.append("\"\n");
      buf.append("   + parent=").append(clb.getParent()).append("\n");
      buf.append("   + parents:\n");
      for (ClassLoaderBase parentCL : clb.getParents()) {
        buf.append("     - ").append(parentCL).append("\n");
      }
      buf.append("   + urls:\n");
      for (URL url : clb.getURLs()) {
        buf.append("     - ").append(url).append("\n");
      }
      clb.traceClassLoadingDependencies(buf);
    }
  }

  /**
   * in anderen classloader, die den übergebenen classloader als dependencyToReloadIfClassLoaderChanges eingetragen haben,
   * den übergebenen classloader als dependency entfernen.
   * d.h. anpassung der classloader von objekten, die von dem übergebenen classloader verwendet werden.
   */
  private void removeFromAllDependenciesToReloadInOtherClassLoaders(String fqClassName, ClassLoaderType type,
                                                                    Long revision, boolean removeFromBackuppedDeps) {
    removeFromAllDependenciesToReloadInOtherClassLoaders(fqClassName, type, revision, null, removeFromBackuppedDeps);
  }
  
  
  private void removeFromAllDependenciesToReloadInOtherClassLoaders(String fqClassName, ClassLoaderType type,
                                                                    Long revision, Long parentRevision, boolean removeFromBackuppedDeps) {
    RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();

    ClassLoaderIdRevisionRef clId = new ClassLoaderIdRevisionRef(fqClassName, revision, parentRevision);
    Set<Long> deps = new HashSet<Long>();
    rcdMgmt.getDependenciesRecursivly(revision, deps);
    for (ClassLoaderType t : classLoaderMap.keySet()) {
      for (MapWrapper<String, ClassLoaderBase> map : classLoaderMap.get(t).values()) {
        for (ClassLoaderBase clb : map.map.values()) {
          if (clb.getRevision().equals(revision)
               || deps.contains(clb.getRevision()) //abhängige Revisions
               || (parentRevision != null && parentRevision.equals(clb.getRevision()))) { //OutdatedFilter
            clb.removeDependencyToReloadIfThisClassLoaderIsRecreated(clId, type);
          }
        }
      }
    }
    
    //TODO die OutdatedFilter müssten wahrscheinlich gar nicht durchsucht werden, da sie von niemandem
    //verwendet werden und daher keine dependencies haben
    for (MapWrapper<String, FilterClassLoader> map : outdatedFilterClassLoader.values()) {
      for (FilterClassLoader clb : map.map.values()) {
        if (clb.getRevision().equals(revision) 
             || deps.contains(clb.getRevision()) //abhängige Revisions
             || (parentRevision != null && parentRevision.equals(clb.getRevision()))) {
          clb.removeDependencyToReloadIfThisClassLoaderIsRecreated(clId, type);
        }
      }
    }
    
    if (removeFromBackuppedDeps) {
      ClassLoaderBase.removeFromBackuppedDependencies(fqClassName, type, revision);
    }
  }


  private class ClassLoaderDispatcherDeploymentHandler_UndeployOld implements DeploymentHandler {
    
    public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
    //  traceClassLoaderState();

      if(object.isReservedServerObject()) {
        return;
      }
      
      if (logger.isDebugEnabled()) {
        logger.debug("Undeploying classloaders for " + object.getFqClassName() + " revision " + object.getRevision());
      }
      ClassLoaderBase objectClassLoader = null;
      boolean firstClassLoaderCreation = false;
      if (object instanceof WF) {
        try {
          objectClassLoader = getWFClassLoader(object.getFqClassName(), object.getRevision(), false);
        } catch (XFMG_WFClassLoaderNotFoundException e) {
          firstClassLoaderCreation = true;
          // erstes deployment... ok!
        }
      } else if (object instanceof DOM) {
        try {
          objectClassLoader = getMDMClassLoader(object.getFqClassName(), object.getRevision(), false);
        } catch (XFMG_MDMObjectClassLoaderNotFoundException e) {
          firstClassLoaderCreation = true;
          // noch nicht deployed gewesen
        }
      } else if (object instanceof ExceptionGeneration) {
        try {
          objectClassLoader = getExceptionClassLoader(object.getFqClassName(), object.getRevision(), false);
        } catch (XFMG_ExceptionClassLoaderNotFoundException e) {
          firstClassLoaderCreation = true;
          // noch nicht deployed gewesen
        }
      }
      if (logger.isDebugEnabled()) {
        logger.debug("classloader was already there: " + !firstClassLoaderCreation);
      }

      if (!firstClassLoaderCreation && objectClassLoader.hasBeenDeployed()) {
        //get dependencies, undeploy all
        try {
          objectClassLoader.undeployDependencies();
        } catch (XFMG_ClassLoaderRedeploymentException | MultipleExceptions e) {
          throw new XPRC_DeploymentHandlerException(object.getFqClassName(), DEPLOYMENTHANDLERNAME_UNDEPLOYOLD, e);
        }
      }
           
      logger.debug("classloading 'undeploy old' finished ...");

    }

    public void finish(boolean success) throws XPRC_DeploymentHandlerException {
    }

    @Override
    public void begin() throws XPRC_DeploymentHandlerException {
      //FilterClassLoader wollen sich das Lock holen, was zu Deadlocks führen kann. Deshalb bereits hier holen
      XynaFactory.getInstance().getActivation().getActivationTrigger().lockManagement();
    }

  }
  
  private static final String DEPLOYMENTHANDLERNAME_RELOADCL = "reloadClassloaders";
  private static final String DEPLOYMENTHANDLERNAME_UNDEPLOYOLD = "removeOldClassloaders";
  private static final String DEPLOYMENTHANDLERNAME_DEPLOYNEW = "addNewClassLoaders";


  private class ClassLoaderDispatcherDeploymentHandler_ReloadClassLoaders implements DeploymentHandler {

    public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {

      if (object.isReservedServerObject()) {
        return;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Reloading classloaders for " + object.getFqClassName() + " revision " + object.getRevision());
      }
      if (object instanceof WF) {
        WFClassLoader oldWfCl = null;
        boolean firstClassLoaderCreation = false;
        try {
          oldWfCl = getWFClassLoader(object.getFqClassName(), object.getRevision(), false);
        } catch (XFMG_WFClassLoaderNotFoundException e) {
          firstClassLoaderCreation = true;
          // erstes deployment... ok!
        }
        if (logger.isDebugEnabled()) {
          logger.debug("classloader was already there: " + !firstClassLoaderCreation);
        }

        WFClassLoader newWfCl = oldWfCl;
        if (firstClassLoaderCreation) {
          if (logger.isDebugEnabled()) {
            logger.debug("creating new classloader " + object.getFqClassName() + "...");
          }
          newWfCl = getWFClassLoaderLazyCreate(object.getFqClassName(), object.getOriginalPath(), object.getOriginalSimpleName(), object.getRevision());
        }

        classloaderDependencyHandling(newWfCl, object, firstClassLoaderCreation, !firstClassLoaderCreation);
      } else if (object instanceof DOM) {
        DOM dom = (DOM) object;
        MDMClassLoader existingClassloader = null;
        boolean firstClassLoaderCreation = false;
        MDMClassLoader newMDMClassloader = null;
        try {
          existingClassloader = getMDMClassLoader(object.getFqClassName(), object.getRevision(), false);
        } catch (XFMG_MDMObjectClassLoaderNotFoundException e) {
          firstClassLoaderCreation = true;
          // noch nicht deployed gewesen
        }
        if (logger.isDebugEnabled()) {
          logger.debug("classloader was already there: " + !firstClassLoaderCreation);
        }

        HashSet<String> jars = new HashSet<String>();
        try {
          dom.getDependentJarsWithoutRecursion(jars, false, false);
        } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
          throw new XPRC_DeploymentHandlerException(object.getFqClassName(), DEPLOYMENTHANDLERNAME_RELOADCL, e);
        } catch (XPRC_JarFileForServiceImplNotFoundException e) {
          throw new XPRC_DeploymentHandlerException(object.getFqClassName(), DEPLOYMENTHANDLERNAME_RELOADCL, e);
        }
        
        boolean jarsOrSharedLibsDiffer = true; //TODO performance: ist nicht immer true!

        boolean createNewClassLoader = firstClassLoaderCreation || jarsOrSharedLibsDiffer;
        if (createNewClassLoader) {
          logger.debug("creating new classloader ...");
          if (!firstClassLoaderCreation) {
            //alten classloader entfernen, weil der neue evtl andere shared libs oder jars hat.
            existingClassloader =
                (MDMClassLoader) removeClassLoaderFromMap(object.getRevision(), object.getFqClassName(), ClassLoaderType.MDM);
            existingClassloader.closeFiles();
          }
          try {
            newMDMClassloader = getMDMClassLoaderLazyCreate(object.getFqClassName(), dom.getSharedLibs(), dom.getOriginalPath(),
                                                            dom.getOriginalSimpleName(), dom.getRevision());
            try {
              for (String j : jars) {
                newMDMClassloader.addJarFile(j);
              }
            } catch (Ex_FileAccessException e) {
              throw new XPRC_DeploymentHandlerException(object.getFqClassName(), DEPLOYMENTHANDLERNAME_RELOADCL, e);
            }


          } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
            throw new XPRC_DeploymentHandlerException(object.getFqClassName(), DEPLOYMENTHANDLERNAME_RELOADCL, e);
          }

          if (!firstClassLoaderCreation) {
            // dependencies nach "oben" ändern sich nicht
            //ist nur für datentypen notwendig, weil der classloader oben entfernt und neu angelegt wurde
            newMDMClassloader.copyDependencies(existingClassloader, true);
          }
        } else {
          newMDMClassloader = existingClassloader;
        }

        classloaderDependencyHandling(newMDMClassloader, object, firstClassLoaderCreation, !createNewClassLoader);
      } else if (object instanceof ExceptionGeneration) {
        boolean firstClassLoaderCreation = false;
        ExceptionClassLoader ecl = null;
        try {
          ecl = getExceptionClassLoader(object.getFqClassName(), object.getRevision(), false);
        } catch (XFMG_ExceptionClassLoaderNotFoundException e) {
          firstClassLoaderCreation = true;
          // noch nicht deployed gewesen
        }
        if (logger.isDebugEnabled()) {
          logger.debug("classloader was already there: " + !firstClassLoaderCreation);
        }

        if (firstClassLoaderCreation) {
          if (logger.isDebugEnabled()) {
            logger.debug("creating new classloader " + object.getFqClassName() + "...");
          }
          ecl =
              getExceptionClassLoaderLazyCreate(object.getFqClassName(), object.getOriginalPath(), object.getOriginalSimpleName(),
                                                object.getRevision());
        }

        classloaderDependencyHandling(ecl, object, firstClassLoaderCreation, !firstClassLoaderCreation);
      }

      logger.debug("classloading 'reload classloaders' finished ...");

    }


    private void classloaderDependencyHandling(ClassLoaderBase newClassloader, GenerationBase object, boolean firstClassLoaderCreation,
                                               boolean recreateSelf) {
      boolean backupDepsRestored = false;
      if (newClassloader.restoreBackuppedClassloadingDependencies()) {
        object.setBackupExisted(true);
        backupDepsRestored = true;
      }
      if (!firstClassLoaderCreation || backupDepsRestored) {
        recreateDependencies(newClassloader, recreateSelf); //kopiert insbesondere auch den newclassloader, nicht nur die deps
      }
      //dependencies entfernen, die beim "deployNew" wieder hinzugefügt werden. 
      removeFromAllDependenciesToReloadInOtherClassLoaders(object.getFqClassName(), newClassloader.getType(), object.getRevision(), false);
    }


    private void recreateDependencies(ClassLoaderBase cl, boolean includeSelf) {
      if (logger.isDebugEnabled()) {
        logger.debug("removing old classloader and recreating all dependent classloaders.");
      }
      for (Lock lock : allLocksAsWriteLockInOrderForDeadLockSecurity) {
        lock.lock();
      }
      try {
        cl.recreateDependencies(includeSelf); //alle classloader in hierarchie ersetzen.
      } finally {
        for (int i = allLocksAsWriteLockInOrderForDeadLockSecurity.length - 1; i >= 0; i--) {
          allLocksAsWriteLockInOrderForDeadLockSecurity[i].unlock();
        }
      }

    }


    public void finish(boolean success) throws XPRC_DeploymentHandlerException {
    }


    @Override
    public void begin() throws XPRC_DeploymentHandlerException {
    }

  }
  
  private static final XynaPropertyBoolean useReducedAmountOfHardClassloadingDependencies = new XynaPropertyBoolean("xfmg.xfctrl.classloading.dependencies.generationbase.reduce", true);
  

  private class ClassLoaderDispatcherDeploymentHandler_DeployNew implements DeploymentHandler {

    public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {

      if (object.isReservedServerObject()) {
        return;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Deploying classloaders for " + object.getFqClassName() + " revision " + object.getRevision());
      }
      ClassLoaderBase objectClassLoader;
      if (object instanceof WF) {
        // by now the classloader has to exist
        try {
          objectClassLoader = getWFClassLoader(object.getFqClassName(), object.getRevision(), false);
        } catch (XFMG_WFClassLoaderNotFoundException e) {
          throw new RuntimeException(e);
        }

      } else if (object instanceof DOM) {
        // by now the classloader has to exist
        try {
          objectClassLoader = getMDMClassLoader(object.getFqClassName(), object.getRevision(), false);
        } catch (XFMG_MDMObjectClassLoaderNotFoundException e) {
          throw new RuntimeException(e);
        }

      } else if (object instanceof ExceptionGeneration) {
        // by now the classloader has to exist
        try {
          objectClassLoader = getExceptionClassLoader(object.getFqClassName(), object.getRevision(), false);
        } catch (XFMG_ExceptionClassLoaderNotFoundException e) {
          throw new RuntimeException(e);
        }

      } else {
        throw new RuntimeException("unexpected generation base object: " + object);
      }

      if (!objectClassLoader.hasBeenDeployed()) {
        try {
          objectClassLoader.deployDependencies(mode);
        } catch (XFMG_ClassLoaderRedeploymentException e) {
          throw new XPRC_DeploymentHandlerException(object.getFqClassName(), DEPLOYMENTHANDLERNAME_DEPLOYNEW, e);
        }
      }

      /*
       * dependencies jetzt erst setzen, damit nicht zuviele redeployments stattfinden.
       * evtl sollte man das hier in einem weiteren deploymenthandler machen, der als viertes nach den 
       * anderen 3 classloading-deploymenthandlern passiert.
       */
      ClassLoaderType type;
      if (object instanceof WF) {
        type = ClassLoaderType.WF;
      } else if (object instanceof DOM) {
        type = ClassLoaderType.MDM;
        
        //shared lib deps werden bei "addDependencies" nicht gesetzt
        DOM dom = (DOM) object;
        for (String sharedLib : dom.getSharedLibs()) {
          try {
            getSharedLibClassLoader(sharedLib, dom.getRevision(), true)
                .addDependencyToReloadIfThisClassLoaderIsRecreated(dom.getFqClassName(), dom.getRevision(), ClassLoaderType.MDM,
                                                                   ClassLoadingDependencySource.GenerationBase);
          } catch (XFMG_SharedLibClassLoaderNotFoundException e) {
            throw new RuntimeException("Shared Lib Classloader missing", e);
          }
        }
      } else if (object instanceof ExceptionGeneration) {
        type = ClassLoaderType.Exception;
      } else {
        throw new RuntimeException("unsupported type " + object.getClass().getName());
      }
      
      addDependencies(object, type);

      if (logger.isDebugEnabled()) {
        logger.debug("classloading 'deploy new' finished ...");
      }
    }


    public void finish(boolean success) throws XPRC_DeploymentHandlerException {
      XynaFactory.getInstance().getActivation().getActivationTrigger().unlockManagement();
    }


    @Override
    public void begin() throws XPRC_DeploymentHandlerException {
    }
    

  }
  
  /*
   * 
   * dependencies werden erst entfernt, und dann nach dem deployment wieder hinzugefügt, damit
   * es während des deployments keine unnötigen classloader abhängigkeiten gibt, die eh durch das deployment
   * bereits ausgetauscht+deployed werden.
   * 
   * das entfernen passiert über removeFromAllDependenciesToReloadInOtherClassLoaders
   * 
   * usecase: deploying B. 
   * (pfeil bedeutet "x benutzt y")
   * abhängigkeitsbaum vorher               nachher
   * 
   *     A                                    A
   *     |                                    |
   *     v                                    v
   *     B                                    B
   *    /|\                                  /|
   *  v  v  v                               v v
   *  E  C  F                              E  C  F
   *   \ | /                                  | /
   *     v                                    v
   *     D                                    D
   *  
   * => dependency F->D  muss bestehen bleiben, damit F beim deployment von B neu geladen wird.
   *    dependency E->D  sollte entfernt werden
   */
  private void addDependencies(GenerationBase o, ClassLoaderType type) throws XPRC_DeploymentHandlerException {
    for (GenerationBase gb : getDependentObjectsUsedByClassLoader(o)) {
      if (gb.isReservedServerObject()) {
        continue;
      }
      if (gb instanceof WF) {
        // keine wf abhängigkeit, weil der wf übers processing aufgerufen wird
      } else if (gb instanceof DOM) {
        DOM gbd = (DOM) gb;
        try {
          MDMClassLoader cl = findClassLoaderByType(gbd.getFqClassName(), gbd.getRevision(), ClassLoaderType.MDM, false);
          if (cl == null && 
              !gbd.hasError()) {
            cl = getMDMClassLoaderLazyCreate(gbd.getFqClassName(), gbd.getSharedLibs(), gbd.getOriginalPath(), gbd.getOriginalSimpleName(),
                                             gbd.getRevision());
          }
          if (cl != null) {                
            cl.addDependencyToReloadIfThisClassLoaderIsRecreated(o.getFqClassName(), o.getRevision(), type, ClassLoadingDependencySource.GenerationBase);
          }
        } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
          throw new XPRC_DeploymentHandlerException(gbd.getFqClassName(), DEPLOYMENTHANDLERNAME_RELOADCL, e);
        }
      } else if (gb instanceof ExceptionGeneration) {
        ExceptionGeneration exceptionGen = (ExceptionGeneration) gb;
        ExceptionClassLoader cl = findClassLoaderByType(exceptionGen.getFqClassName(), exceptionGen.getRevision(), ClassLoaderType.Exception, false);
        if (cl == null && 
            !exceptionGen.hasError()) {
            cl = getExceptionClassLoaderLazyCreate(exceptionGen.getFqClassName(), exceptionGen.getOriginalPath(),
                                                   exceptionGen.getOriginalSimpleName(), gb.getRevision());
        }
        if (cl != null) {
          cl.addDependencyToReloadIfThisClassLoaderIsRecreated(o.getFqClassName(), o.getRevision(), type, ClassLoadingDependencySource.GenerationBase);
        }
      }
    }
  }

  private Set<GenerationBase> getDependentObjectsUsedByClassLoader(GenerationBase o) {
    if (!useReducedAmountOfHardClassloadingDependencies.get()) {
      return o.getDependenciesRecursively().getDependencies(false);
    }
    /*
     * eigtl müssten die dependencies genügen, die beim classloading hinzugefügt werden.
     * 
     * leider genügen die nicht, sondern müssen um folgende dependencies erweitert werden:
     * für jede methoden signatur (instanzmethode oder statische methode)
     *   für alle inputs und outputs
     *     abhängigkeit setzen, dass datentyp mit den methoden diese typen verwendet.
     * 
     * 
     * grund: beim verify von methode x mit argument (oder output) y passiert nicht automatisch ein classloading von y (mit classloader von x)
     *        wenn das passieren würde, wäre alles gut.
     *        
     *        dadurch weiß der server nicht, dass y von x benutzt wird (classloading-technisch) und es passiert kein class-reloading von x
     *        nachdem y neu erzeugt wurde. das wäre erstmal nicht kritisch - aber das verify ist offenbar dumm, und sieht dann zwei verschiedene
     *        y, die vom gleichen x benutzt werden könnten und signalisiert deshalb einen verifyerror
     *        
     *        => es gibt verschiedene möglichkeiten dem server beizubringen, dass y von x benutzt wird:
     *        1) wie hier: explizit abhängigkeiten setzen
     *        2) dafür sorgen, dass classloader x das loadclass auf y aufruft
     *           dazu muss im bytecode von x irgendwo eine methode von y aufgerufen werden (oder konstruktor)
     *           
     *  vgl https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-5.html
     */
    Set<GenerationBase> ret = new HashSet<>();
    if (o instanceof DOM) {
      for (com.gip.xyna.xprc.xfractwfe.generation.Operation op : ((DOM) o).getOperations()) {
        for (AVariable input : op.getInputVars()) {
          if (input.getDomOrExceptionObject() != null) {
            ret.add(input.getDomOrExceptionObject());
          }
        }
        for (AVariable output : op.getOutputVars()) {
          if (output.getDomOrExceptionObject() != null) {
            ret.add(output.getDomOrExceptionObject());
          }
        }
        for (AVariable ex : op.getThrownExceptions()) {
          if (ex.getDomOrExceptionObject() != null) {
            ret.add(ex.getDomOrExceptionObject());
          }
        }
      }
    }
    return ret;
    //holzhammer methode von früher erzeugt viel zu viele abhängigkeiten: return o.getDependenciesRecursively().getDependencies(false);
  }

  private class ClassLoaderDispatcherUndeploymentHandler implements UndeploymentHandler {

    public void exec(GenerationBase object) throws XPRC_UnDeploymentHandlerException {

      if(object.isReservedServerObject()) {
        return;
      }
      
      //undeploy auf dem object aufrufen, falls vorher deployed gewesen
      //dependencies auf dieses object aus den anderen classloadern entfernen
      //classloader entfernen
      //falls es kein classloader gibt, ist das auch ok. evtl war nur xml in deploymentverzeichnis und objekt nicht benutzt.

      if (object instanceof WF) {
        removeWFClassLoader(object.getFqClassName(), object.getRevision());
        removeFromAllDependenciesToReloadInOtherClassLoaders(object.getFqClassName(), ClassLoaderType.WF,
                                                             object.getRevision(), true);
      } else if (object instanceof DOM) {
        MDMClassLoader mdmcl = null;
        try {
          mdmcl = getMDMClassLoader(object.getFqClassName(), object.getRevision(), false);
        } catch (XFMG_MDMObjectClassLoaderNotFoundException e) {
          // gibt kein classloader
          return;
        }
        
        try {
          if (mdmcl.hasBeenDeployed()) {
            try {
              mdmcl.undeployWhenReload(object.getFqClassName());
            } catch (XFMG_ClassLoaderRedeploymentException | RuntimeException e) {
              throw new XPRC_UnDeploymentHandlerException(object.getFqClassName(), "removeClassLoaders", e);
            }
          }
        } finally {
          removeMDMClassLoader(object.getFqClassName(), object.getRevision());
          removeFromAllDependenciesToReloadInOtherClassLoaders(object.getFqClassName(), ClassLoaderType.MDM,
                                                                    object.getRevision(), true);
        }

      } else if (object instanceof ExceptionGeneration) {
        try {
          // do we need to obtain this one here?
          getExceptionClassLoader(object.getFqClassName(), object.getRevision(), false);
        } catch (XFMG_ExceptionClassLoaderNotFoundException e) {
          // no classloader found
          return;
        }
        removeExceptionClassLoader(object.getFqClassName(), object.getRevision());
        removeFromAllDependenciesToReloadInOtherClassLoaders(object.getFqClassName(), ClassLoaderType.Exception, object.getRevision(), true);
      } else {
        throw new IllegalArgumentException("Unknown " + GenerationBase.class.getSimpleName() + " instance: "
                        + object.getClass().getName());
      }

    } // end exec

    public void exec(FilterInstanceStorable object) {
    }

    public void exec(TriggerInstanceStorable object) {
    }

    public void exec(Capacity object) {
    }

    public void exec(DestinationKey object) {
    }

    public void finish() throws XPRC_UnDeploymentHandlerException {
    }

    public boolean executeForReservedServerObjects(){
      return false;
    }

    public void exec(FilterStorable object) {
    }

    public void exec(TriggerStorable object) {
    }
  }

  /**
   * sucht auch in abhängigen anderen revisions, falls nicht in dieser gefunden 
   */
  public ExceptionClassLoader getExceptionClassLoader(String fqClassName, Long revision, boolean allowDelegation)
                  throws XFMG_ExceptionClassLoaderNotFoundException {
    ExceptionClassLoader ecl = findClassLoaderByType(fqClassName, revision, ClassLoaderType.Exception, allowDelegation);
    if (ecl != null) {
      return ecl;
    } else {
      throw new XFMG_ExceptionClassLoaderNotFoundException(fqClassName);
    }
  }


  public ExceptionClassLoader getExceptionClassLoaderLazyCreate(String fqClassName, String originalXmlPath,
                                                                String originalXmlName, Long revision) {

    exceptionLock.lock();
    try {
      MapWrapper<String, ClassLoaderBase> exceptionClassLoaderForRevision = classLoaderMap.get(ClassLoaderType.Exception).lazyCreateGet(revision);
      try {
        ExceptionClassLoader ecl = (ExceptionClassLoader) exceptionClassLoaderForRevision.map.get(fqClassName);
        if (ecl != null) {
          return ecl;
        }
        
        if (originalXmlPath == null || originalXmlName == null) {
          throw new RuntimeException("Cannot create exception classloader without xml name and path specified.");
        }
        ecl = new ExceptionClassLoader(fqClassName, originalXmlPath, originalXmlName, revision);
        exceptionClassLoaderForRevision.map.put(fqClassName, ecl);
        
        return ecl;
      } finally {
        classLoaderMap.get(ClassLoaderType.Exception).cleanup(revision);
      }
    } finally {
      exceptionLock.unlock();
    }
  }

  public Class<XynaExceptionBase> loadExceptionClass(String name, String originalXmlPath, String originalXmlName, Long revision)
                  throws ClassNotFoundException {
    return loadExceptionClass(name, true, originalXmlPath, originalXmlName, revision);
  }


  public Class<XynaExceptionBase> loadExceptionClass(String name, boolean createClassLoaderIfNotExisting,
                                                     String originalXmlPath, String originalXmlName, Long revision)
                  throws ClassNotFoundException {

    Class<XynaExceptionBase> c = loadExceptionClassOwnRevision(name, createClassLoaderIfNotExisting, originalXmlPath, originalXmlName, revision);
    
    if (c != null) {
      return c;
    }
    
    Set<Long> requirements = new HashSet<Long>();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(revision, requirements);
    for (long req : requirements) {
      c = loadExceptionClassOwnRevision(name, createClassLoaderIfNotExisting, originalXmlPath, originalXmlName, req);
      if (c != null) {
        return c;
      }
    }

    Long rootRevision = XynaOrderServerExtension.getThreadLocalRootRevision();
    if (rootRevision != null) {
      //es läuft gerade eine deserialisierung, und es ist zusätzlicher revision kontext bekannt
      if (requirements.contains(rootRevision)) {
        return null; //bereits getestet
      }
      if (rootRevision.equals(revision)) {
        return null; //bereits getestet
      }

      if (logger.isDebugEnabled()) {
        logger.debug("trying to load " + name + " from rootrevision " + rootRevision);
      }

      c = loadExceptionClassOwnRevision(name, createClassLoaderIfNotExisting, originalXmlPath, originalXmlName, rootRevision);
      if (c != null) {
        return c;
      }

      Set<Long> requirementsRootRev = new HashSet<Long>();
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
          .getDependenciesRecursivly(rootRevision, requirementsRootRev);
      requirementsRootRev.removeAll(requirements); //bereits getestet
      requirementsRootRev.remove(revision); //bereits getestet
      for (long req : requirementsRootRev) {
        c = loadExceptionClassOwnRevision(name, createClassLoaderIfNotExisting, originalXmlPath, originalXmlName, req);
        if (c != null) {
          return c;
        }
      }
    }
    
    return null;
  }

  @SuppressWarnings("unchecked")
  private Class<XynaExceptionBase> loadExceptionClassOwnRevision(String name, boolean createClassLoaderIfNotExisting,
                                                                 String originalXmlPath, String originalXmlName, Long revision)
                   throws ClassNotFoundException {

    String potentialCLName = ClassLoaderBase.getBaseClassName(name);
    
    ClassLoaderBase ecl = getClassLoaderFromMap(potentialCLName, revision, ClassLoaderType.Exception);
    boolean existsBefore = ecl != null;

    if (createClassLoaderIfNotExisting) {
      ExceptionClassLoader mcl = getExceptionClassLoaderLazyCreate(potentialCLName, originalXmlPath, originalXmlName, revision);
      try {
        return (Class<XynaExceptionBase>) mcl.loadClass(name);
      } catch (ClassNotFoundException e) {
        if (!existsBefore) {
          removeExceptionClassLoader(potentialCLName, revision);
        }
      }
    } else if (existsBefore) {
      return (Class<XynaExceptionBase>) ecl.loadClass(name);
    }
    return null;
  }

  
  public String resolveAndListPhantoms(boolean listPresentClassLoaders) {
    synchronized (ClassLoaderDispatcher.class) {
    Reference<? extends ClassLoader> reference;
    StringBuilder output = new StringBuilder();
    output.append("Polling ReferenceQueues for phantom reachable ClassLoaders:\n");
    for (Entry<ClassLoaderType, ReferenceQueue<ClassLoader>> entry : queuesForPhantomReferences.entrySet()) {
      while ((reference = entry.getValue().poll()) != null) {
        String classLoaderInformation = phantomClassLoaderInformationMap.get(entry.getKey()).remove(reference);
        output.append("\t").append(classLoaderInformation).append("\n");
      }
    }
    output.append("\n");
    if (listPresentClassLoaders) {
      output.append("Following ClassLoaders are more then phantom reachable:\n");
      for (Map<PhantomReference<ClassLoader>, String> types : phantomClassLoaderInformationMap.values()) {
        for (String classLoaderInformation : types.values()) {
          output.append("\t").append(classLoaderInformation).append("\n");
        }
      }
      output.append("\n");
    }
    
    output.append("Listing sizes of ClassLoader-Phantom References : Amount of registered ClassLoaders for that type (if appropriate)\n");
    output.append("MDM:\t");
    output.append(phantomClassLoaderInformationMap.get(ClassLoaderType.MDM).size());
    output.append(" : ");
    output.append(calculateSum(classLoaderMap.get(ClassLoaderType.MDM)));
    output.append("\n");
    output.append("Exception:\t");
    output.append(phantomClassLoaderInformationMap.get(ClassLoaderType.Exception).size());
    output.append(" : ");
    output.append(calculateSum(classLoaderMap.get(ClassLoaderType.Exception)));
    output.append("\n");
    output.append("Workflow:\t");
    output.append(phantomClassLoaderInformationMap.get(ClassLoaderType.WF).size());
    output.append(" : ");
    output.append(calculateSum(classLoaderMap.get(ClassLoaderType.WF)));
    output.append("\n");
    output.append("SharedLib:\t");
    output.append(phantomClassLoaderInformationMap.get(ClassLoaderType.SharedLib).size());
    output.append(" : ");
    output.append(calculateSum(classLoaderMap.get(ClassLoaderType.SharedLib)));
    output.append("\n");
    output.append("Trigger:\t");
    output.append(phantomClassLoaderInformationMap.get(ClassLoaderType.Trigger).size());
    output.append(" : ");
    output.append(calculateSum(classLoaderMap.get(ClassLoaderType.Trigger)));
    output.append("\n").append("Filter:\t");
    output.append(phantomClassLoaderInformationMap.get(ClassLoaderType.Filter).size());
    output.append(" : ");
    output.append(calculateSum(classLoaderMap.get(ClassLoaderType.Filter)));
    output.append("\n");
    output.append("Persist:\t");
    output.append(phantomClassLoaderInformationMap.get(ClassLoaderType.PersistenceLayer).size());
    output.append("\n");
    output.append("Xyna:\t");
    output.append(phantomClassLoaderInformationMap.get(ClassLoaderType.XYNA).size());
    output.append("\n");
    output.append("RMI:\t");
    output.append(phantomClassLoaderInformationMap.get(ClassLoaderType.RMI).size());
    output.append("\n");
    
    return output.toString();
    }
  }


  private int calculateSum(ClassLoaderMap mapOfMap) {
    int countSize = 0;
      for(MapWrapper<String, ClassLoaderBase> mapEntry : mapOfMap.values()) {
        countSize += mapEntry.map.size();
      }
    return countSize;
  }
  
  
  synchronized static void addPhantomReference(ClassLoader loader, ClassLoaderType type, String classLoaderInformation) {
    Map<PhantomReference<ClassLoader>, String> typeMap = phantomClassLoaderInformationMap.get(type);
    if (typeMap == null) {
      typeMap = new HashMap<PhantomReference<ClassLoader>, String>();
    }
    ReferenceQueue<ClassLoader> correspondingQueue = queuesForPhantomReferences.get(type);
    if (correspondingQueue == null) {
      correspondingQueue = new ReferenceQueue<ClassLoader>();
      queuesForPhantomReferences.put(type, correspondingQueue);
    }
    typeMap.put(new PhantomReference<ClassLoader>(loader, correspondingQueue), classLoaderInformation);
    phantomClassLoaderInformationMap.put(type, typeMap);
  }


  public void registerPersistenceLayerClassLoader(String fqPersistencelayerName,
                                                  PersistenceLayerClassLoader persLayerClassloader) {

    if (fqPersistencelayerName == null) {
      throw new IllegalArgumentException();
    }
    if (persLayerClassloader == null) {
      throw new IllegalArgumentException();
    }

    persistenceLayerClassLoader.put(fqPersistencelayerName, persLayerClassloader);
  }


  public void unregisterPersistenceLayerClassLoader(String name) {

    if (name == null) {
      throw new IllegalArgumentException();
    }

    if (persistenceLayerClassLoader.remove(name) == null) {
      logger.warn("Could not remove persistencelayer classloader <" + name + ">, it does not exist.");
    }
  }
  
  
  private SharedLibClassLoader getOrCreateEmptySharedLibClassLoader(Long revision) throws XFMG_SHARED_LIB_NOT_FOUND {
    sharedLibLock.lock();
    try {
      MapWrapper<String, ClassLoaderBase> sharedLibClassLoaderForRevision = classLoaderMap.get(ClassLoaderType.SharedLib).lazyCreateGet(revision);
      try {
        SharedLibClassLoader empty = (SharedLibClassLoader) sharedLibClassLoaderForRevision.map.get(NAME_OF_EMPTY_SHARED_LIB);
        if(empty == null) {
          empty = new SharedLibClassLoader(SharedLibClassLoader.EMPTYSHAREDLIB, revision);
          sharedLibClassLoaderForRevision.map.put(NAME_OF_EMPTY_SHARED_LIB, empty);
        }
        return empty;
      } finally {
        classLoaderMap.get(ClassLoaderType.SharedLib).cleanup(revision);
      }
    } finally {
      sharedLibLock.unlock();
    }
  }


  /**
   * wenn ein classloader temporär entfernt wird (usecase: deploymentproblem), muss man sich
   * für das nächste deployment die classloadingdeps merken, weil die verwendenden objekte
   * nicht neu deployed werden.
   *  
   * falls es den classloader nicht gibt, wird kein fehler geworfen
   */
  public void backupClassloadingDeps(GenerationBase gb) {
    String fqClassName = gb.getFqClassName();
    long revision = gb.getRevision();
    ClassLoaderBase cl;
    if (gb instanceof DOM){
      try {
        cl = getMDMClassLoader(fqClassName, revision, false);
      } catch (XFMG_MDMObjectClassLoaderNotFoundException e) {
        return;
      }
    } else if (gb instanceof WF) {
      try {
        cl = getWFClassLoader(fqClassName, revision, false);
      } catch (XFMG_WFClassLoaderNotFoundException e) {
        return;
      }
    } else if (gb instanceof ExceptionGeneration) {
      try {
        cl = getExceptionClassLoader(fqClassName, revision, false);
      } catch (XFMG_ExceptionClassLoaderNotFoundException e) {
        return;
      }
    } else {
      throw new RuntimeException();
    }
    
    cl.backupClassloadingDependencies();
  }

  public void cleanClassLoaderMapsForRevision(long revision) {
    if (classLoaderMap.get(ClassLoaderType.Filter).get(revision) != null) {
      logger.warn("FilterClassloaders for revision " + revision + " remaining, cannot cleanup.");
    }
    if (classLoaderMap.get(ClassLoaderType.Trigger).get(revision) != null) {
      logger.warn("TriggerClassloaders for revision " + revision + " remaining, cannot cleanup.");
    }
    if (classLoaderMap.get(ClassLoaderType.WF).get(revision) != null) {
      logger.warn("WFClassloaders for revision " + revision + " remaining, cannot cleanup.");
    }
    if (classLoaderMap.get(ClassLoaderType.MDM).get(revision) != null) {
      logger.warn("MDMClassloaders for revision " + revision + " remaining, cannot cleanup.");
    }
    if (classLoaderMap.get(ClassLoaderType.SharedLib).get(revision) != null) {
      logger.warn("SharedLibClassloaders for revision " + revision + " remaining, cannot cleanup.");
    }
    if (classLoaderMap.get(ClassLoaderType.Exception).get(revision) != null) {
      logger.warn("ExceptionClassloaders for revision " + revision + " remaining, cannot cleanup.");
    }
    if (outdatedFilterClassLoader.get(revision) != null) {
      logger.warn("OutdatedFilterClassloaders for revision " + revision + " remaining, cannot cleanup.");
    }
  }
  
  private static class ClassLoaderID implements HasUniqueStringIdentifier {
    
    private final String id;
    private final ClassLoaderIdRevisionRef classloaderref;
    private final ClassLoaderType clt;
    
    public ClassLoaderID(ClassLoaderType clt, ClassLoaderIdRevisionRef id) {
      this.id = clt.name() + "??" + id.classLoaderId + "??" + id.revision + "??" + id.parentRevision;
      this.classloaderref = id;
      this.clt = clt;
    }

    public String getId() {
      return id;
    }
    
    
  }
  
  /*
   * in every revisionDependenciesToRemove
   *  search for reloadIfThisChanges from a revisionsToRecreate
   *  remove that dependency and collect that classloader
   *  for all collected classloaders (and all their parentclassloaders (recursively)) do in the correct order (like during deployment):
   *   - call onUndeployment
   *   - switch classloader
   *   - return classloader, so that onDeployment can be called later
   */
  public List<ClassLoaderBase> recreateClassLoadersWithoutRevisionDependencies(Set<Long> revisionsToRecreate, Set<Long> revisionDependenciesToRemove) {
    for (Lock lock : allLocksAsWriteLockInOrderForDeadLockSecurity) {
      lock.lock();
    }
    if (logger.isTraceEnabled()) {
      logger.trace("reloading classloaders in revisions " + revisionsToRecreate + " because revisions " + revisionDependenciesToRemove + " are not connected any more.");
    }
    try {
      Map<ClassLoaderType, Set<ClassLoaderIdRevisionRef>> classLoadersToRecreate = new HashMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>();
      for (ClassLoaderType aClassLoaderType : ClassLoaderType.values()) {
        ClassLoaderMap typedSubMap = classLoaderMap.get(aClassLoaderType);
        if (typedSubMap != null) {
          for (Long revisionDependencyToRemove : revisionDependenciesToRemove) {
            MapWrapper<String, ClassLoaderBase> revisionDependencyToRemoveSubMap = typedSubMap.get(revisionDependencyToRemove);
            if (revisionDependencyToRemoveSubMap != null) {
              for (ClassLoaderBase classLoaderInRemovedRevision : revisionDependencyToRemoveSubMap.map.values()) {
                Map<ClassLoaderType, ConcurrentMap<ClassLoaderIdRevisionRef, ClassLoadingDependencySource>> revisionDependencyEntryReloadSet = classLoaderInRemovedRevision.getClassLoadersToReloadIfThisClassLoaderIsRecreated();
                Map<ClassLoaderType, Set<ClassLoaderIdRevisionRef>> foreignRevisionClassLoaderToRemoveMap = new HashMap<ClassLoaderType, Set<ClassLoaderIdRevisionRef>>();
                for (ClassLoaderType bClassLoaderType : ClassLoaderType.values()) {
                  Set<ClassLoaderIdRevisionRef> foreignRevisionClassLoaderToRemoveSubSet = new HashSet<ClassLoaderIdRevisionRef>();
                  foreignRevisionClassLoaderToRemoveMap.put(bClassLoaderType, foreignRevisionClassLoaderToRemoveSubSet);
                  
                  //classloader dependencies eines classloaders in nicht mehr erreichbarer revision
                  ConcurrentMap<ClassLoaderIdRevisionRef, ClassLoadingDependencySource> toReloadMap =
                      revisionDependencyEntryReloadSet.get(bClassLoaderType);
                  if (toReloadMap != null) {
                    Set<ClassLoaderIdRevisionRef> toReloadSet = toReloadMap.keySet();
                    for (ClassLoaderIdRevisionRef revisionDependencyEntryReload : toReloadSet) {
                      if (revisionsToRecreate.contains(revisionDependencyEntryReload.revision)) {
                        //diese classloader dependency ist nicht mehr erreichbar und wird unten entfernt
                        foreignRevisionClassLoaderToRemoveSubSet.add(revisionDependencyEntryReload);

                        Set<ClassLoaderIdRevisionRef> list = classLoadersToRecreate.get(bClassLoaderType);
                        if (list == null) {
                          list = new HashSet<ClassLoaderIdRevisionRef>();
                          classLoadersToRecreate.put(bClassLoaderType, list);
                        }
                        list.add(revisionDependencyEntryReload);
                      }
                    }
                  }
                }
                for (ClassLoaderType bClassLoaderType : ClassLoaderType.values()) {
                  for (ClassLoaderIdRevisionRef foreignRevisionClassLoaderToRemove : foreignRevisionClassLoaderToRemoveMap.get(bClassLoaderType)) {
                    if (logger.isTraceEnabled()) {
                      logger.trace("removing dependency in classloader " + classLoaderInRemovedRevision + " to not reload " + foreignRevisionClassLoaderToRemove + ".");
                    }
                    classLoaderInRemovedRevision.removeDependencyToReloadIfThisClassLoaderIsRecreated(foreignRevisionClassLoaderToRemove, bClassLoaderType);
                  }
                }
              }
            }
          }
        }
      }
      
      List<ClassLoaderBase> result = new ArrayList<ClassLoaderBase>();
      
      Map<String, Node<ClassLoaderID>> nodeMap = new HashMap<String, Node<ClassLoaderID>>();
      for (Entry<ClassLoaderType, Set<ClassLoaderIdRevisionRef>> classLoaderToRecreateSubSet : classLoadersToRecreate.entrySet()) {
        ClassLoaderType clt = classLoaderToRecreateSubSet.getKey();
        for (ClassLoaderIdRevisionRef classLoaderToRecreate : classLoaderToRecreateSubSet.getValue()) {
          addToNodeMap(nodeMap, classLoaderToRecreate, clt);
        }
      }
      //nun die menge der classloader die neu erstellt werden reduzieren, um duplikate aufrufe zu vermeiden und in der richtigen reihenfolge unzudeployen
      //plan: falls ein classloader A in den dependenciesToReload von classloader B enthalten ist, kann A aus der liste gestrichen werden,
      //weil er beim handling der dependencies automatisch mit berücksichtigt wird
      Graph<ClassLoaderID> g = new Graph<ClassLoaderID>(nodeMap.values());
      List<Node<ClassLoaderID>> roots = g.getRoots();

      for (Node<ClassLoaderID> root : roots) {
        if (logger.isTraceEnabled()) {
          logger.trace("removing classloader and dependencies of " + root.getContent().classloaderref);
        }
        ClassLoaderBase clb =
            getClassLoaderByType(root.getContent().clt, root.getContent().classloaderref.classLoaderId,
                                 root.getContent().classloaderref.revision);
        if (clb == null) {
          logger.warn("classloader " + root.getContent().classloaderref.classLoaderId + "@" + root.getContent().classloaderref.revision
              + " not found.");
          continue;
        }
        result.add(clb);
        try {
          clb.undeployDependencies();
          clb.recreateDependencies();
        } catch (XFMG_ClassLoaderRedeploymentException | MultipleExceptions e) {
          // should not happen, we just collected it (as long as we guard ourself against current mods)
          RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
          String rc;
          try {
            rc = rm.getRuntimeContext(root.getContent().classloaderref.revision).toString();
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
            rc = "unknown";
          }
          logger.info("Could not reload classloader " + clb.getClassLoaderID() + " " + root.getContent().classloaderref.classLoaderId
              + " in revision " + root.getContent().classloaderref.revision + " (" + rc + ").", e);
        }
      }
   
      return result;
    } finally {
      for (int i = allLocksAsWriteLockInOrderForDeadLockSecurity.length - 1; i >= 0; i--) {
        allLocksAsWriteLockInOrderForDeadLockSecurity[i].unlock();
      }
    }    
  }

  
  public void deployClassLoaders(List<ClassLoaderBase> classloaders) {
    XynaFactory.getInstance().getActivation().getActivationTrigger().lockManagement();
    try {
      for (Lock lock : allLocksAsWriteLockInOrderForDeadLockSecurity) {
        lock.lock();
      }
      try {
        for (ClassLoaderBase clb : classloaders) {
          if (logger.isTraceEnabled()) {
            logger.trace("deploying classloader and dependencies of " + clb);
          }
          try {
            clb.deployDependencies(DeploymentMode.deployBackup); //backup, damit exceptions nicht als runtimeexceptions/error durchschlagen
          } catch (XFMG_ClassLoaderRedeploymentException e) {
            //erwartet, wenn man runtimecontextdependencies entfernt hat. ist hier nicht weiter zu behandeln, 
            //weil später deswegen ein redeployment mit neuer codegenerierung etc durchgeführt wird.
            if (logger.isDebugEnabled()) {
              RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
              String rc;
              try {
                rc = rm.getRuntimeContext(clb.getRevision()).toString();
              } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
                rc = "unknown";
              }
              logger.debug("Could not reload classloader " + clb.getClassLoaderID() + " in revision " + clb.getRevision() + " (" + rc + ").",e);
            }
          }
          try {
            GenerationBase gb = null;
            switch (clb.getType()) {
              case MDM :
                gb = DOM.generateUncachedInstance(clb.getClassLoaderID(), true, clb.getRevision());
                break;
              case Exception :
                gb = ExceptionGeneration.generateUncachedInstance(clb.getClassLoaderID(), true, clb.getRevision());
                break;
              case WF :
                gb = WF.generateUncachedInstance(clb.getClassLoaderID(), true, clb.getRevision());
                break;
              default:
                break;
            }
            if (gb != null) {
              addDependencies(gb, clb.getType());
            }
          } catch (XPRC_DeploymentHandlerException | XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | 
                   AssumedDeadlockException | XPRC_MDMDeploymentException e) {
            logger.debug("Error during addDependencies for: " + clb.getClassLoaderID() + " @ " + clb.getRevision(), e);
          }
        }
      } finally {
        for (int i = allLocksAsWriteLockInOrderForDeadLockSecurity.length - 1; i >= 0; i--) {
          allLocksAsWriteLockInOrderForDeadLockSecurity[i].unlock();
        }
      }
    } finally {
      XynaFactory.getInstance().getActivation().getActivationTrigger().unlockManagement();
    }
  }


  private Node<ClassLoaderID> addToNodeMap(Map<String, Node<ClassLoaderID>> nodeMap, ClassLoaderIdRevisionRef classLoaderToRecreate, ClassLoaderType clt) {
    ClassLoaderID clid = new ClassLoaderID(clt, classLoaderToRecreate);
    Node<ClassLoaderID> n = nodeMap.get(clid.getId());
    if (n == null) {
      n = new Node<ClassLoaderID>(clid);
      nodeMap.put(clid.getId(), n);
      ClassLoaderBase clb = getClassLoaderByType(clt, classLoaderToRecreate.classLoaderId, classLoaderToRecreate.revision);
      if (clb == null) {
        logger.warn("Failed to find classloader while trying to recreate: " + classLoaderToRecreate.classLoaderId + "@" + classLoaderToRecreate.revision);
      } else {
        ConcurrentMap<ClassLoaderType, ConcurrentMap<ClassLoaderIdRevisionRef, ClassLoadingDependencySource>> classLoadersToReloadIfThisClassLoaderIsRecreated = clb.getClassLoadersToReloadIfThisClassLoaderIsRecreated();
        for (Entry<ClassLoaderType, ConcurrentMap<ClassLoaderIdRevisionRef, ClassLoadingDependencySource>> e : classLoadersToReloadIfThisClassLoaderIsRecreated.entrySet()) {
          ClassLoaderType childType = e.getKey();
          for (ClassLoaderIdRevisionRef childCL : e.getValue().keySet()) {
            Node<ClassLoaderID> child = addToNodeMap(nodeMap, childCL, childType);
            n.addDependency(child);
          }
        }
      }
    }
    return n;
  }


  //aus abwärtskompatibilitätsgründen: xtf verwendet das
  public MDMClassLoader getMDMClassLoader(String fqClassName, Long revision) throws XFMG_MDMObjectClassLoaderNotFoundException {
    return getMDMClassLoader(fqClassName, revision, true);
  }


  public void validateClassLoaders(CommandLineWriter clw) {
    ValidationCount cnt = new ValidationCount();
    DeploymentItemStateManagement dism =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();

    for (Entry<ClassLoaderType, ClassLoaderMap> t : classLoaderMap.entrySet()) {
      for (Long revision : new TreeSet<Long>(t.getValue().keySet())) {
        MapWrapper<String, ClassLoaderBase> typespecificMap = t.getValue().get(revision);
        if (typespecificMap != null) {
          cnt.clCntAll += typespecificMap.map.values().size();
          for (ClassLoaderBase clb : typespecificMap.map.values()) {
            validateClassLoader(dism, clw, clb, cnt);
          }
        }
      }
    }
    clw.writeLineToCommandLine("checked " + cnt.clCntAll + " class loaders by comparing " + cnt.cntLoadedDeps + " of " + cnt.cntAll()
        + " potential classes.");
    clw.writeLineToCommandLine("found " + cnt.cntInvalidClassLoader + " class loader problems!");
    clw.writeLineToCommandLine("classloaderstats: erroneous=" + cnt.clCntError + ", closed=" + cnt.clCntClosed);
    clw.writeLineToCommandLine("loaded classes stats: ok=" + cnt.cntOK + ", notloaded=" + cnt.cntNotLoadedDeps + ", notclassloaderbase="
        + cnt.cntLoadedButNotBaseClassloader + ", erroneous=" + cnt.cntError + ", obsoleteXMOMStructureCache=" + cnt.cntXMOMStructureCacheRelic);
    //TODO missing classloaders: also für wieviele objekte im deploymentitemmgmt gilt, dass sie deployed sind, es aber keinen classloader gibt
  }


  private void validateClassLoader(DeploymentItemStateManagement dism, CommandLineWriter clw, ClassLoaderBase clb, ValidationCount cnt) {
    if (clb.isClosed()) {
      clw.writeLineToCommandLine("Registered classloader is closed: " + clb.getExtendedDescription(true));
      cnt.clCntClosed++;
    } else {
      switch (clb.type) {
        case MDM :
        case Exception :
          MDMClassLoaderXMLBase base = (MDMClassLoaderXMLBase) clb;
          validateXMOMClassLoader(dism, clw, clb, base.getOriginalXmlPath() + "." + base.getOriginalXmlName(), cnt);
          return;
        case WF :
          validateXMOMClassLoader(dism, clw, clb, GenerationBase.lookupXMLNameByJavaClassName(clb.getClassLoaderID(), clb.getRevision(), false), cnt);
          return;
        case Filter :
          try {
            AdditionalDependencyContainer additionalDependencies = getFilterByFilterClassName(clb.getClassLoaderID(), clb.getRevision());
            if (additionalDependencies == null) {
              return; //ntbd
            }
            for (AdditionalDependencyType type : AdditionalDependencyType.xmomTypes()) {
              Set<String> deps = additionalDependencies.getAdditionalDependencies(type);
              validateLoadedClassesFor(clw, clb, deps, cnt);
            }
            return;
          } catch (PersistenceLayerException | Ex_FileAccessException | XPRC_XmlParsingException
              | XPRC_InvalidXmlMissingRequiredElementException | XACT_FilterNotFound e) {
            logger.info("Could not get filter information for " + clb.getClassLoaderID() + " in revision " + clb.getRevision(), e);
            cnt.clCntError++;
            return;
          }
        default : //ntbd
          return;
      }
    }
  }


  private AdditionalDependencyContainer getFilterByFilterClassName(String fqClassName, Long revision)
      throws PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException, XACT_FilterNotFound {
    for (Filter f : XynaFactory.getInstance().getActivation().getActivationTrigger().getFilters(revision)) {
      if (f.getFQFilterClassName().equals(fqClassName)) {
        return f.getAdditionalDependencies();
      }
    }
    throw new XACT_FilterNotFound(fqClassName);
  }


  private static class ValidationCount {

    //classloader
    private int clCntError;
    private int clCntClosed;
    private int clCntAll;

    //loadedClasses:
    private int cntNotLoadedDeps;
    private int cntLoadedDeps;
    private int cntLoadedButNotBaseClassloader;
    private int cntInvalidClassLoader;
    private int cntOK;
    private int cntError;
    private int cntXMOMStructureCacheRelic;


    public int cntAll() {
      return cntNotLoadedDeps + cntLoadedDeps + cntLoadedButNotBaseClassloader + cntInvalidClassLoader + cntOK + cntError + cntXMOMStructureCacheRelic;
    }
  }


  private void validateLoadedClassesFor(CommandLineWriter clw, ClassLoaderBase clb, Set<String> deps, ValidationCount cnt) {
    for (String fqXmlName : deps) {
      String fqClassName;
      try {
        fqClassName = GenerationBase.transformNameForJava(fqXmlName);
      } catch (XPRC_InvalidPackageNameException e) {
        logger.info("Classloader " + clb + " can not load dep " + fqXmlName);
        cnt.cntError++;
        continue;
      }
      Class<?> c = clb.findLoadedClass2(fqClassName);
      if (c == null) {
        cnt.cntNotLoadedDeps++;
        continue; //kein falscher classloader, wenn es noch nicht geladen ist
      }
      cnt.cntLoadedDeps++;
      ClassLoader cl = c.getClassLoader();
      if (cl instanceof ClassLoaderBase) {
        ClassLoaderBase current = getClassLoaderByType(((ClassLoaderBase) cl).getType(), fqClassName, ((ClassLoaderBase) cl).getRevision());
        if (current != cl) {
          clw.writeLineToCommandLine("Classloader " + clb.toString() + " loads wrong class for " + fqClassName);
          cnt.cntInvalidClassLoader++;
        } else {
          cnt.cntOK++;
        }
      } else {
        //ok, internes objekt?
        cnt.cntLoadedButNotBaseClassloader++;
      }
    }
  }


  private void validateXMOMClassLoader(DeploymentItemStateManagement dism, CommandLineWriter clw, ClassLoaderBase clb,
                                       String xmomFqXmlName, ValidationCount cnt) {
    DeploymentItemState deploymentItemState = dism.get(xmomFqXmlName, clb.getRevision());
    if (deploymentItemState == null) {
      clw.writeLineToCommandLine(xmomFqXmlName + ", rev=" + clb.getRevision() + " not found in DeploymentItemStateManagement.");
      cnt.clCntError++;
      return;
    }

    Set<DeploymentItemInterface> interfaceEmployments =
        ((DeploymentItemStateImpl) deploymentItemState).getInterfaceEmployments(DeploymentLocation.DEPLOYED);

    Set<String> deps = new HashSet<>();
    for (DeploymentItemInterface intf : interfaceEmployments) {
      if (intf instanceof SupertypeInterface) {
        SupertypeInterface si = (SupertypeInterface) intf;
        validateXMOMStorableStructureCache(clw, clb, xmomFqXmlName, cnt, si.getName());

        addToDeps(deps, si.getType(), si.getName());
      } else if (intf instanceof InterfaceEmployment) {
        InterfaceEmployment ie = (InterfaceEmployment) intf;
        addToDeps(deps, ie.getProvider().getType(), ie.getProvider().getName());
      } else if (intf instanceof TypeInterface) {
        TypeInterface ti = (TypeInterface) intf;
        addToDeps(deps, ti.getType(), ti.getName());
        if (ti.getTypesOfUsage().contains(TypeOfUsage.SUPERTYPE)) {
          validateXMOMStorableStructureCache(clw, clb, xmomFqXmlName, cnt, ti.getName());
        }
      }

    }
    validateLoadedClassesFor(clw, clb, deps, cnt);
  }
  
  private void validateXMOMStorableStructureCache(CommandLineWriter clw, ClassLoaderBase clb,
                        String xmomFqXmlName, ValidationCount cnt, String superTypeName) {
    //classloader-referenzen in xmomstructure cache checken
    if (superTypeName.equals(XMOMPersistenceManagement.STORABLE_BASE_CLASS)) { //TODO auch für tiefere ableitungshierarchien checken
      XMOMStorableStructureInformation structuralInformation;
      try {
        structuralInformation = XMOMStorableStructureCache.getInstance(clb.getRevision()).getStructuralInformation(GenerationBase.transformNameForJava(xmomFqXmlName));
      } catch (XPRC_InvalidPackageNameException e) {
        return;
      }
      Class<?> rootClazz = null;
      try {
        rootClazz = structuralInformation.getStorableClass();
      } catch (RuntimeException e) {
      }
      
      if (structuralInformation != null) {
        final Set<StorableStructureInformation> sis = new HashSet<>();
        sis.add(structuralInformation);
        structuralInformation.traverse(new StorableStructureVisitor() {

          @Override
          public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
            sis.add(current);
          }


          @Override
          public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) {

          }


          @Override
          public StorableStructureRecursionFilter getRecursionFilter() {
            return new StorableStructureRecursionFilter() {

              public boolean accept(StorableColumnInformation columnLink) {
                return columnLink.getStorableVariableType() == StorableVariableType.EXPANSION;
              }

              public boolean acceptHierarchy(StorableStructureInformation declaredType) {
                return false;
              }
            };
          }

        });
        for (StorableStructureInformation ssi : sis) {
          try {
            ssi.getStorableClass();
          } catch (RuntimeException e) {
            clw.writeLineToCommandLine("Object " + xmomFqXmlName + " in revision " + clb.getRevision() + " failed to load it's StorableClass");
            cnt.cntError++;
          }
          if (rootClazz != null && 
              ssi.getClassLoaderForStorable() != null) {
            try {
              Class<?> clazz = ssi.getClassLoaderForStorable().loadClass(rootClazz.getName());
              if (!Objects.equals(clazz, rootClazz)) {
                clw.writeLineToCommandLine("Object " + xmomFqXmlName + " in revision " + clb.getRevision() + " loaded  XMOMStorableRootClass " + clazz + " does not equal " + rootClazz);
                cnt.cntXMOMStructureCacheRelic++;
              }
            } catch (ClassNotFoundException e) {
              clw.writeLineToCommandLine("Object " + xmomFqXmlName + " in revision " + clb.getRevision() + " failed to load XMOMStorableRootClass");
              cnt.cntXMOMStructureCacheRelic++;
            }
          }
        }
      } else {
        clw.writeLineToCommandLine("Missing StorableStructureInformation for " + xmomFqXmlName + " in revision " + clb.getRevision());
        cnt.cntError++;
      }
    }
  }


  private void addToDeps(Set<String> deps, XMOMType type, String name) {
    if (type == null) {
      deps.add(name);
    } else {
      switch (type) {
        case DATATYPE :
        case EXCEPTION :
        case WORKFLOW :
          deps.add(name);
          break;
        default ://ntbd
      }
    }
  }


}
