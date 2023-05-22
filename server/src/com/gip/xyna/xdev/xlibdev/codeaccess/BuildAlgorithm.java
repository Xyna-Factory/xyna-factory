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
package com.gip.xyna.xdev.xlibdev.codeaccess;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.XMOMChangeEvent;
import com.gip.xyna.xdev.exceptions.XDEV_RepositoryAccessException;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.ComponentType;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.FileUpdate;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryItemModification;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryItemModification.RepositoryModificationType;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xsched.Algorithm;


public class BuildAlgorithm implements Algorithm {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(BuildAlgorithm.class);

  private CodeAccess codeAccess;
  
  private List<ComponentCodeChange> changedComponents = new ArrayList<ComponentCodeChange>(); //�ber svn ge�nderte Komponenten
  private List<XMOMChangeEvent> xmomChangeEvents = new ArrayList<XMOMChangeEvent>(); //neu gespeicherte Objekte (XMOM)
  private AtomicBoolean rebuildAll = new AtomicBoolean(false); //alle ServiceGroups neu bauen (z.B. wegen RuntimeContextDependency-�nderung)
  private Set<String> rebuildMDMJarTriggers = new HashSet<String>();
  
  public BuildAlgorithm(CodeAccess codeAccess) {
    this.codeAccess = codeAccess;
  }
  
  public void exec() {
    if (logger.isDebugEnabled()) {
      logger.debug("code access build started");
    }
    boolean locked = false;
    
    GenerationBaseCache gbCache = new GenerationBaseCache();

    Set<String> services = new HashSet<String>();
    Set<String> filter = new HashSet<String>();
    List<ComponentCodeChange> components = new ArrayList<ComponentCodeChange>();

    Set<String> localCopy = null;
    synchronized (rebuildMDMJarTriggers) {
      if (!rebuildMDMJarTriggers.isEmpty()) {
        localCopy = new HashSet<String>(rebuildMDMJarTriggers);
        rebuildMDMJarTriggers.clear();
      }      
    }
    if (localCopy != null) {
      //mdm.jar neu bauen
      try {
        String commitMsg = "Runtime Context Dependency Modification triggered a mdm.jar rebuild.";
        String identityString;
        if (localCopy.size() == 1) {
          identityString = localCopy.iterator().next();
        } else {
          identityString = localCopy.toString();
        }
        codeAccess.rebuildAndCommitMDMJar(identityString, commitMsg);
      } catch (XDEV_RepositoryAccessException e) {
        logger.warn("Problem accessing repository during mdmjar rebuild", e);
      }
    }
    
    try {
      //Komponenten, die neu gebaut werden m�ssen, einsammeln (so lange bis
      //bis keine neuen �nderungen mehr registriert werden)
      do {
        if (locked) {
          //RepostioryLock freigeben, falls im vorhergehenden Schleifendurchlauf geholt
          codeAccess.unlockRepository();
          locked = false;
        }
        
        //Listen in lokale Listen umtragen, damit die globalen wieder neu gef�llt werden k�nnen
        synchronized (changedComponents) {
          components.addAll(changedComponents);
          changedComponents = new ArrayList<ComponentCodeChange>();
        }
        
        List<XMOMChangeEvent> events;
        synchronized (xmomChangeEvents) {
          events = xmomChangeEvents;
          xmomChangeEvents = new ArrayList<XMOMChangeEvent>();
        }
        
        //f�r die XMOMChangeEvents m�ssen alle abh�ngigen ServiceGroups und Filter ermittelt werden
        if (!rebuildAll.get()) {
          //zun�chst alle Dependencies ermitteln
          Set<DependencyNode> dependencies = new HashSet<DependencyNode>();
          for (XMOMChangeEvent event : events) {
            dependencies.addAll(collectDependencies(event)); //(eigenes Objekt ist in Dependencies enthalten)
          }
          
          //Ergebnis auf ServiceGroups und Filter der eigenen Revision reduzieren
          for (DependencyNode dependency : dependencies) {
            if (dependency.getRevision().equals(codeAccess.getRevision())) {
              if (dependency.getType() == DependencySourceType.FILTER) {
                filter.add(dependency.getUniqueName());
              } else if (dependency.getType() == DependencySourceType.DATATYPE) {
                if (createImplProjectForServiceGroup(dependency.getUniqueName(), gbCache)) {
                  services.add(dependency.getUniqueName());
                }
              }
            }
          }
        }

        //RepositoryLock holen, damit keine neuen �nderungen mehr dazukommen
        codeAccess.lockRepository();
        locked = true;
      } while (!isChangedComponentsEmpty() || !isXmomChangeEventsEmpty());
      
      
      if (rebuildAll.getAndSet(false)) {
        //alle vorhanden ServiceGroups suchen
        services = searchAllServices(gbCache);
        filter = searchAllFilter();
      }
      
      Map<ComponentKey, ComponentCodeChange> buildMap = new HashMap<ComponentKey, ComponentCodeChange>();

      //f�r alle ServiceGroups ein ComponentCodeChange erstellen
      for (String service : services) {
        ComponentKey key = new ComponentKey(service, ComponentType.CODED_SERVICE);
        ComponentCodeChange value = codeAccess.createComponentCodeChangeForNewService(service);
        buildMap.put(key, value);
      }
      
      //f�r alle Filter ein ComponentCodeChange erstellen
      for (String f : filter) {
        ComponentKey key = new ComponentKey(f, ComponentType.FILTER);
        ComponentCodeChange value = codeAccess.createComponentCodeChangeForFilter(f);
        buildMap.put(key, value);
      }
      
      //deleted JavaFiles und changed jars/subcomponents der changedComponents hinzuf�gen
      for (ComponentCodeChange ccc : components) {
        ComponentKey key = new ComponentKey(ccc.getComponentOriginalName(), ccc.getComponentType());
        ComponentCodeChange existingChange = buildMap.get(key);
        if (existingChange != null) {
          for (RepositoryItemModification modifiedJavaFile : ccc.getModifiedJavaFiles()) {
            if (modifiedJavaFile.getModification() == RepositoryModificationType.Deleted) {
              if (!new File(modifiedJavaFile.getFile()).exists()) {
                existingChange.addModifiedJavaFiles(modifiedJavaFile);
              } else {
                //nichts zu tun, File wurde inzwischen wieder angelegt
              }
            }
          }
          for (FileUpdate fileupdate : ccc.getModifiedJars()) {
            existingChange.addModifiedJars(fileupdate);
          }
          for (ComponentCodeChange subComponent : ccc.getSubComponentChanges()) {
            existingChange.addChangedSubComponent(subComponent);
          }
        } else {
          buildMap.put(key, ccc);
        }
      }

      if (!buildMap.isEmpty()) {
        List<ComponentCodeChange> deployList = codeAccess.rebuild(buildMap.values(), "rebuild-" + Constants.defaultUTCSimpleDateFormat().format(new Date()));
        codeAccess.deploy(deployList);
      }
    } finally {
      if (locked) {
        codeAccess.unlockRepository();
        locked = false;
      }
      if (logger.isDebugEnabled()) {
        logger.debug("code access build finished");
      }
    }
  }

  
  public void addChangedComponent(ComponentCodeChange ccc) {
    synchronized (changedComponents) {
      changedComponents.add(ccc);
    }
  }

  public void addChangeEvents(Collection<XMOMChangeEvent> events) {
    synchronized (xmomChangeEvents) {
      xmomChangeEvents.addAll(events);
    }
  }

  public void rebuildAll() {
    rebuildAll.set(true);
  }
  
  private boolean isChangedComponentsEmpty() {
    synchronized (changedComponents) {
      return changedComponents.isEmpty();
    }
  }

  private boolean isXmomChangeEventsEmpty() {
    synchronized (xmomChangeEvents) {
      return xmomChangeEvents.isEmpty();
    }
  }
  
  /**
   * Sucht alle abh�ngigen Objekte �ber das DependencyRegister.
   * TODO besser XMOMDatabase verwenden
   * @param event
   * @return
   */
  private Set<DependencyNode> collectDependencies(XMOMChangeEvent event) {
    DependencySourceType type;
    if (event.getXMOMType() == XMOMType.DATATYPE) {
      type = DependencySourceType.DATATYPE;
    } else if (event.getXMOMType() == XMOMType.EXCEPTION) {
      type = DependencySourceType.XYNAEXCEPTION;
    } else {
      return Collections.emptySet();
    }
    
    RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Long revision = rcdm.getRevisionDefiningXMOMObjectOrParent(event.getObjectIdentifier(), codeAccess.getRevision());

    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister()
            .getDependencies(event.getObjectIdentifier(), type, revision, true);
  }
  
  /**
   * Sucht alle ServiceGroups �ber XMOMDatabase.
   */
  private Set<String> searchAllServices(GenerationBaseCache gbCache) {
    Set<String> services = new HashSet<String>();
    XMOMDatabase db = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();

    try {
      List<XMOMDatabaseSelect> selects = new ArrayList<XMOMDatabaseSelect>();
      XMOMDatabaseSelect select = new XMOMDatabaseSelect();
      select.addDesiredResultTypes(XMOMDatabaseType.SERVICEGROUP);
      select.whereNumber(XMOMDatabaseEntryColumn.REVISION).isEqual(codeAccess.getRevision());
      selects.add(select);
      
      XMOMDatabaseSearchResult resp = db.searchXMOMDatabase(selects, -1, codeAccess.getRevision());
      for (XMOMDatabaseSearchResultEntry result : resp.getResult()) {
        String fqOriginalName = XMOMDatabase.getFqOriginalNameFromFqServiceName(result.getFqName());
        if (createImplProjectForServiceGroup(fqOriginalName, gbCache)) { //nur die Services bauen, die mind. eine Operation haben, die nicht als CodeSnippet implementiert ist.
          services.add(fqOriginalName); 
        }
      }
    } catch (PersistenceLayerException e) {
      logger.warn("Could not get servicegroups", e);
    } catch (XNWH_InvalidSelectStatementException e) {
      logger.warn("Could not get servicegroups", e);
    } 
    
    return services;
  }
  
  
  /**
   * �berpr�ft, ob eine ServiceGroup mindestens eine Operation hat
   * die nicht als CodeSnippet implementiert ist.
   * @param fqOriginalName
   * @param gbCache
   * @return
   */
  private boolean createImplProjectForServiceGroup(String fqOriginalName, GenerationBaseCache gbCache) {
    try {
      DOM dom = DOM.getOrCreateInstance(fqOriginalName, gbCache, codeAccess.getRevision());
      if (dom.getRevision() != codeAccess.getRevision()) {
        return false;
      }
      dom.parseGeneration(false, false); 
      if (CodeAccessManagement.globalCodeAccessFilter.get().accept(dom)
          && (codeAccess.hasNonAbstractJavaOperationNotImplementedAsCodeSnippet(dom) || dom.libraryExists())) {
        return true;
      }
    } catch (XynaException e) {
      logger.warn("Could not rebuild " + fqOriginalName, e);
    }
    
    return false;
  }
  
  
  /**
   * Sucht alle Filter �ber XynaActivationTrigger
   */
  private Set<String> searchAllFilter() {
    Set<String> filter = new HashSet<String>();
    try {
      Filter[] allFilter = XynaFactory.getInstance().getActivation().getActivationTrigger().getFilters(codeAccess.getRevision());
      for (Filter f : allFilter) {
        filter.add(f.getName());
      }
    } catch (PersistenceLayerException e) {
      logger.warn("Could not get filter", e);
    }
    return filter;
  }

  public void rebuildMDMJar(String identityTriggeringRebuild) {    
    synchronized (rebuildMDMJarTriggers) {
      rebuildMDMJarTriggers.add(identityTriggeringRebuild);
    }
  }
  
}