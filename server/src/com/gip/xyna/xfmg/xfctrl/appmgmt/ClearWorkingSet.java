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
package com.gip.xyna.xfmg.xfctrl.appmgmt;



import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.utils.InvalidXMLException;
import com.gip.xyna.utils.exceptions.utils.XMLUtils;
import com.gip.xyna.xact.exceptions.XACT_FilterMayNotBeRemovedIsDeployedException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerMayNotBeRemovedIsDeployedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.BasicProjectCreationOrChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.BatchRepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EventType;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.RepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.XMOMDeleteEvent;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilterInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xfmg.exceptions.XFMG_ClearWorkingSetFailedBecauseOfRunningOrders;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveApplication;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.ApplicationName;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.PreparedXMOMDatabaseSelect;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;
import com.gip.xyna.xprc.exceptions.XPRC_MDMUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeoutWhileWaitingForUnaccessibleOrderException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DependentObjectMode;
import com.gip.xyna.xprc.xpce.dispatcher.ServiceDestination;



public class ClearWorkingSet extends RevisionOrderControl {

  private static final Logger logger = CentralFactoryLogging.getLogger(ClearWorkingSet.class);


  /**
   * @param revision revision des workingsets (z.b. {@link RevisionManagement#REVISION_DEFAULT_WORKSPACE}) 
   */
  public ClearWorkingSet(long revision) {
    super(revision);
  }


  //TODO beim berücksichtigen laufender aufträge die blacklist beachten:
  //     evtl verwenden laufende aufträge nur objekte aus der blacklist und dürfen deshalb weiterhin laufen
  //TODO clear verhindern, wenn es noch "ungespeicherte" (was auch immer das bedeutet?) änderungen im workingset gibt,
  //     die durch das clear gelöscht würden.


  public interface RevisionContentBlackWhiteList {

    public Collection<String> getTriggerInstanceNames();


    public Collection<String> getFilterInstanceNames();


    /**
     * workflows, datatypes, exceptions
     */
    public Collection<String> getXMOMObjects();


    public Collection<String> getSharedLibs();


    public Collection<String> getTriggersNames();


    public Collection<String> getFilterNames();


    /**
     * objekte, die im workingset in einer application definiert sind
     */
    public Collection<ApplicationName> getApplications();
  }

  public static class RevisionContentBlackWhiteListBean implements RevisionContentBlackWhiteList {

    private final Set<String> triggerInstanceNames = new HashSet<String>();
    private final Set<String> filterInstanceNames = new HashSet<String>();
    private final Set<String> xmomObjects = new HashSet<String>();
    private final Set<String> sharedLibs = new HashSet<String>();
    private final Set<String> triggerNames = new HashSet<String>();
    private final Set<String> filterNames = new HashSet<String>();
    private final Set<ApplicationName> applications = new HashSet<ApplicationName>();


    public void addTriggerInstance(String triggerInstanceName) {
      triggerInstanceNames.add(triggerInstanceName);
    }


    public void addFilterInstance(String filterInstanceName) {
      filterInstanceNames.add(filterInstanceName);
    }


    public void addXMOMObject(String fqName) {
      xmomObjects.add(fqName);
    }


    public void addSharedLib(String sharedLibName) {
      sharedLibs.add(sharedLibName);
    }


    public void addTrigger(String triggerName) {
      triggerNames.add(triggerName);
    }


    public void addFilter(String filterName) {
      filterNames.add(filterName);
    }


    public void addApplication(String appName, String appVersion) {
      applications.add(new ApplicationName(appName, appVersion));
    }


    public Collection<String> getTriggerInstanceNames() {
      return triggerInstanceNames;
    }


    public Collection<String> getFilterInstanceNames() {
      return filterInstanceNames;
    }


    public Collection<String> getXMOMObjects() {
      return xmomObjects;
    }


    public Collection<String> getSharedLibs() {
      return sharedLibs;
    }


    public Collection<String> getTriggersNames() {
      return triggerNames;
    }


    public Collection<String> getFilterNames() {
      return filterNames;
    }


    public Collection<ApplicationName> getApplications() {
      return applications;
    }

  }


  private final DependencyRegister depReg = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
      .getDependencyRegister();
  private final XMOMDatabase xmomDatabase = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
  private RevisionContentBlackWhiteList blackList = new RevisionContentBlackWhiteListBean(); //objekte, die nicht entfernt werden sollen
  private RevisionContentBlackWhiteListBean blackListWithApplicationContent;
  
  private Set<String> allXMOMToDelete;
  private Set<String> allXMOMToKeep;

  /**
   * objekte definieren, die nicht beim clear entfernt werden sollen. 
   * default blacklist = {}.
   * xmom factory komponenten werden nie entfernt, sind also additiv zur blacklist zu sehen
   */
  public void setBlackList(RevisionContentBlackWhiteList blackList) {
    if (blackList == null) {
      blackList = new RevisionContentBlackWhiteListBean();
    }
    this.blackList = blackList;
  }


  /**
   * objekte definieren, die beim clear entfernt werden sollen. andere objekte werden dann nicht entfernt!
   * d.h. es wird immer {whitelist} \ {blacklist} entfernt.
   * default whitelist = alles.
   */
  public void setWhiteList(RevisionContentBlackWhiteList whiteList) {
    throw new RuntimeException("unsupported");
  }


  /**
   * entfernt alle objekte aus dem workingset ausser denen, die über {@link #setBlackList(RevisionContentBlackWhiteList)} 
   * angegeben wurden.
   * Die Subtypen von Output-Parametern von Java-Services von geblacklisteten Applications werden nicht
   * entfernt, außer sie sind in 'removeSubtypes' enthalten.
   * @throws XFMG_ClearWorkingSetFailedBecauseOfRunningOrders falls noch aufträge am laufen sind (force = false)
   * @throws OrderEntryInterfacesCouldNotBeClosedException falls auftragseingangsschnittstellen nicht geschlossen werden konnten
   * @throws XPRC_TimeoutWhileWaitingForUnaccessibleOrderException wenn aufträge unerwartet hängen
   * @throws PersistenceLayerException wenn beim entfernen der objekte ein persistencelayerproblem auftritt
   */
  public void clear(boolean forceKillRunningOrders, List<String> removeSubtypesOf) throws XFMG_ClearWorkingSetFailedBecauseOfRunningOrders, OrderEntryInterfacesCouldNotBeClosedException,
      PersistenceLayerException, XPRC_TimeoutWhileWaitingForUnaccessibleOrderException {
    Set<Long> set = new HashSet<>();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getParentRevisionsRecursivly(revision, set);
    if (set.size() > 0) {
      throw new RuntimeException("The workspace can not be cleared, because it is still referenced from another runtime context.");
    }
    /*
     * davor schützen, dass nicht gleichzeitig andere clearworkingsets aufgerufen werden, oder deployments durchgeführt werden, etc
     * CommandControl.wlock und closeorderentryinterfaces haben ähnliche funktionalität.
     * 
     * CommandControl.wlock betrifft:
     *  - andere clearworkingsets
     *  - saveMDM
     *  - refactoring
     *  - build filter/trigger/service project, buildmdmjar, buildservicedefinitionjar
     *  - codeaccess
     *  - ordertype bezogene befehle
     *    - dokumentation
     *    - destinations
     *    - priority
     *    - monitoringlevel
     *    - ordercontextmapping
     *  - deployment von xmom objekten, triggern, filtern
     *    - triggerkonfiguration (disable, enable, maxtriggerevents)
     *    - undeployment/remove
     *  - applicationbezogene befehle (build, addObjectTo, etc)
     *  - packagebezogene befehle
     *  - anlegen und ändern von crons
     *  - anlegen von frequencycontrolled tasks
     *  - xmomstorable config
     *  
     * closeorderentryinterfaces betrifft:
     *  - startorder
     *  - bestehende filter und trigger instanzen
     */
    Pair<Operation, Operation> failure =
        CommandControl.wlock(CommandControl.Operation.APPLICATION_CLEAR_WORKINGSET, CommandControl.Operation.all(), revision);
    if (failure != null) {
      throw new RuntimeException(failure.getFirst() + " could not be locked because it is locked by another process of type "
          + failure.getSecond() + ".");
    }
    try {
      closeOrderEntryInterfaces(blackList, true); //TODO hier könnte man auch die blacklist verwenden, die weiter unten erstellt wird
      try {
        int timeout = 0;
        while (true) {
          if (waitForRunningOrders(timeout, TimeUnit.MILLISECONDS)) {
            break;
          }
          if (!forceKillRunningOrders) {
            throw new XFMG_ClearWorkingSetFailedBecauseOfRunningOrders();
          }
          if (forceKillRunningOrders) {
            //TODO wenn ein auftrag nach dem kill in der compensation länger braucht, wird er darin erneut gekillt.
            killRunningOrders();
          }
          timeout = 1000;
          logger.debug("waiting for running orders to finish ...");
        }

        BatchRepositoryEvent repositoryEvent = new BatchRepositoryEvent(revision);
        Map<ApplicationName, List<ApplicationEntryStorable>> applicationContent = clearApplications(removeSubtypesOf, repositoryEvent);
        createBlackListWithAppContent(applicationContent);
        clearFilterInstances();
        clearTriggerInstances();
        clearFilter(repositoryEvent);
        clearTrigger(repositoryEvent);
        clearXMOMObjects(repositoryEvent);
        clearSharedLibs(repositoryEvent);
        clearOrdertypes();
        
        repositoryEvent.execute("clear workspace");
      } finally {
        openPreviouslyClosedOrderEntryInterfaces();
      }
    } finally {
      CommandControl.wunlock(CommandControl.Operation.all(), revision);
    }
  }
  

  private void clearOrdertypes() throws PersistenceLayerException {
    /*
     * alle ordertypes (inkl zugehöriger konfig) wegräumen, deren executiondestination auf ein nicht (mehr) vorhandenes xmom objekt (wf/service) zeigt.
     * falls executiondestination nicht gelöscht wurde, aber planning/cleanup, bleibt der ordertype bestehen.
     */
    List<OrdertypeParameter> listOrdertypes =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement()
            .listOrdertypes(revision);

    List<OrdertypeParameter> toDelete = new ArrayList<OrdertypeParameter>();
    for (OrdertypeParameter op : listOrdertypes) {
      if (op.getExecutionDestinationValue() != null) {
        switch (op.getExecutionDestinationValue().getDestinationTypeEnum()) {
          case JAVA_DESTINATION :
          case UNKOWN :
            break;
          case SERVICE_DESTINATION :
            if (!allXMOMToKeep.contains(ServiceDestination.splitFqName(op.getExecutionDestinationValue().getFullQualifiedName())[0])) {
              if (logger.isDebugEnabled()) {
                logger.debug("deleting ordertype config " + op.getOrdertypeName());
              }
              toDelete.add(op);
            }
            break;
          case XYNA_FRACTAL_WORKFLOW :
            if (!allXMOMToKeep.contains(op.getExecutionDestinationValue().getFullQualifiedName())) {
              if (logger.isDebugEnabled()) {
                logger.debug("deleting ordertype config " + op.getOrdertypeName());
              }
              toDelete.add(op);
            }
            break;
          default :
            throw new RuntimeException("unsupported executiontype : " + op.getPlanningDestinationValue().getDestinationType());
        }
      }
    }

    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement().deleteOrdertypes(toDelete);
  }


  private void createBlackListWithAppContent(Map<ApplicationName, List<ApplicationEntryStorable>> applicationContent) {
    blackListWithApplicationContent = new RevisionContentBlackWhiteListBean();
    for (String xmomName : blackList.getXMOMObjects()) {
      blackListWithApplicationContent.addXMOMObject(xmomName);
    }
    for (String trigger : blackList.getTriggersNames()) {
      blackListWithApplicationContent.addTrigger(trigger);
    }
    for (String triggerInstance : blackList.getTriggerInstanceNames()) {
      blackListWithApplicationContent.addTriggerInstance(triggerInstance);
    }
    for (String filter : blackList.getFilterNames()) {
      blackListWithApplicationContent.addFilter(filter);
    }
    for (String filterInstance : blackList.getFilterInstanceNames()) {
      blackListWithApplicationContent.addFilterInstance(filterInstance);
    }
    for (String sharedLib : blackList.getSharedLibs()) {
      blackListWithApplicationContent.addSharedLib(sharedLib);
    }
    for (ApplicationName app : blackList.getApplications()) {
      blackListWithApplicationContent.addApplication(app.getName(), app.getVersionName());
    }

    for (List<ApplicationEntryStorable> l : applicationContent.values()) {
      for (ApplicationEntryStorable aes : l) {
        switch (aes.getTypeAsEnum()) {
          case DATATYPE :
          case EXCEPTION :
          case WORKFLOW :
            blackListWithApplicationContent.addXMOMObject(aes.getName());
            break;
          case FILTER :
            blackListWithApplicationContent.addFilter(aes.getName());
            break;
          case FILTERINSTANCE :
            blackListWithApplicationContent.addFilterInstance(aes.getName());
            break;
          case SHAREDLIB :
            blackListWithApplicationContent.addSharedLib(aes.getName());
            break;
          case TRIGGER :
            blackListWithApplicationContent.addTrigger(aes.getName());
            break;
          case TRIGGERINSTANCE :
            blackListWithApplicationContent.addTriggerInstance(aes.getName());
            break;
          case ORDERTYPE :
          case XYNAPROPERTY :
          case FORMDEFINITION :
          case CAPACITY :
            break;
          default :
            throw new RuntimeException("unknown type : " + aes.getTypeAsEnum());
        }
      }
    }

  }


  private Map<ApplicationName, List<ApplicationEntryStorable>> clearApplications(List<String> removeSubtypesOf, RepositoryEvent repositoryEvent) throws PersistenceLayerException {
    Map<ApplicationName, List<ApplicationEntryStorable>> applicationContent =
        new HashMap<ApplicationName, List<ApplicationEntryStorable>>();
    ApplicationManagement am = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    List<ApplicationDefinitionInformation> apps = am.listApplicationDefinitions(revision);
    Collection<ApplicationName> blacklistedApplications = blackList.getApplications();
    int last = apps.size() + 1;
    while (apps.size() > 0 && apps.size() < last) {
      last = apps.size();
      Iterator<ApplicationDefinitionInformation> it = apps.iterator();
      while (it.hasNext()) {
        ApplicationDefinitionInformation app = it.next();
        if (blacklistedApplications.contains(new ApplicationName(app.getName(), app.getVersion()))) {
          if (logger.isDebugEnabled()) {
            logger.debug("skipping application definition '" + app.getName() + "' / '" + app.getVersion()
                + "' because it is contained in blacklist.");
          }
          List<ApplicationEntryStorable> listApplicationDetails =
              ((ApplicationManagementImpl) am).listApplicationDetails(app.getName(), app.getVersion(), true, removeSubtypesOf, revision);
          applicationContent.put(new ApplicationName(app.getName(), app.getVersion()), listApplicationDetails);
          it.remove();
        } else {
          try {
            RemoveApplicationParameters params = new RemoveApplicationParameters();
            params.setParentWorkspace(app.getParentWorkspace());
            am.removeApplicationVersion(app.getName(), app.getVersion(), params, repositoryEvent);
            it.remove();
          } catch (XFMG_CouldNotRemoveApplication e) {
            //evtl existiert eine abhängigkeit auf eine nicht zu entfernende appdef
          }
        }
      }
    }
    for (ApplicationDefinitionInformation app : apps) {
      logger.warn("Could not remove application definition " + app.getName() + ". It and its content will be kept.");
      List<ApplicationEntryStorable> listApplicationDetails =
          ((ApplicationManagementImpl) am).listApplicationDetails(app.getName(), app.getVersion(), true, removeSubtypesOf, revision);
      applicationContent.put(new ApplicationName(app.getName(), app.getVersion()), listApplicationDetails);
    }
    return applicationContent;
  }


  private void clearSharedLibs(RepositoryEvent repositoryEvent) {
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    File sharedLibsDir = new File(RevisionManagement.getPathForRevision(PathType.SHAREDLIB, revision));
    File[] dirs = sharedLibsDir.listFiles();
    if (dirs == null) {
      return;
    }
    for (File dir : dirs) {
      if (dir.isDirectory()) {
        if (!blackListWithApplicationContent.getSharedLibs().contains(dir.getName())) {
          ClassLoaderBase sharedLibClassLoader = cld.getClassLoaderByType(ClassLoaderType.SharedLib, dir.getName(), revision);
          if (sharedLibClassLoader != null) {
            if (sharedLibClassLoader.hasDependencies()) {
              if (logger.isDebugEnabled()) {
                logger.debug("skipping shared lib " + dir.getName() + " because it is still in use.");
              }
              continue;
            }
            cld.removeSharedLibClassLoader(dir.getName(), revision);
          }
          if (logger.isDebugEnabled()) {
            logger.debug("removing shared lib " + dir.getName());
          }
          FileUtils.deleteDirectoryRecursively(dir);
          
          repositoryEvent.addEvent(new BasicProjectCreationOrChangeEvent(EventType.SHAREDLIB_REMOVE, dir.getName()));
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("skipping shared lib " + dir.getName() + " because it is contained in blacklist.");
          }
        }
      }
    }
  }


  private void clearXMOMObjects(RepositoryEvent repositoryEvent) {
    RuntimeContext runtimeContext;
    try {
      runtimeContext =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    List<File> files;
    if (runtimeContext instanceof Application) {
      files = FileUtils.getMDMFiles(runtimeContext.getName(), ((Application) runtimeContext).getVersionName());
    }
    else {
      String savedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, revision, false);
      files = FileUtils.getMDMFiles(new File(savedMdmDir), new ArrayList<File>());
    }

    Map<String, XMOMType> toDelete = new HashMap<String, XMOMType>();
    allXMOMToKeep = new HashSet<String>();
    
    for (File f : files) {
      Document doc;
      try {
        doc = XMLUtils.parse(f);
      } catch (InvalidXMLException e) {
        logger.warn("invalid xml: " + f.getAbsolutePath(), e);
        continue;
      }

      String fqXmlName = GenerationBase.getFqXMLName(doc);
      if (fqXmlName == null || fqXmlName.trim().length() <= 1) {
        logger.warn("skipping invalid xml: " + f.getAbsolutePath());
        continue;
      }

      if (blackListWithApplicationContent.getXMOMObjects().contains(fqXmlName)) {
        if (logger.isDebugEnabled()) {
          logger.debug("skipping xmom object " + fqXmlName + " because it is contained in blacklist.");
        }
        allXMOMToKeep.add(fqXmlName);
        continue;
      }

      XMOMType type = XMOMType.getXMOMTypeByRootTag(doc.getDocumentElement().getTagName());
      toDelete.put(fqXmlName, type);
    }

    /*
     * Beim Entfernen von XMOM Objekten müssen die Abhängigkeiten beachtet werden.
     * Es dürfen nur XMOM Objekte entfernt werden, bei denen alle Abhängigkeiten auch entfernt werden dürfen.
     * 
     * In erster Linie geht es um die Abhängigkeiten im Saved-Ordner. Manche dieser Objekte können aber auch
     * deployed sein, und müssen dann undeployed werden. Im Deployed-Stand können die Objekte aber
     * anders definiert sein als im Saved-Stand und deshalb muss man für alle diese Objekte
     * die Deployed-Dependencies nochmal separat untersuchen.
     */
    allXMOMToDelete = new HashSet<String>(toDelete.keySet());
    XMOMDBSearchResultCache cache = new XMOMDBSearchResultCache();
    if (toDelete.size() > 0) {
      try {
        Iterator<Entry<String, XMOMType>> it = toDelete.entrySet().iterator();
        while (it.hasNext()) {
          Entry<String, XMOMType> entry = it.next();
          String fqXmlName = entry.getKey();
          XMOMType type = entry.getValue();

          switch (type) {
            case DATATYPE :
            case EXCEPTION :
            case WORKFLOW :
              if (isUsedByBlacklistedObject(fqXmlName, type, cache)) {
                it.remove();
                allXMOMToDelete.remove(fqXmlName);
                allXMOMToKeep.add(fqXmlName);
              }
              break;
            case FORM :
              GenerationBase.deleteMDMObjectFromSavedFolder(fqXmlName, revision);
              it.remove();
              allXMOMToDelete.remove(fqXmlName);
              repositoryEvent.addEvent(new XMOMDeleteEvent(fqXmlName, type, revision));
              break;
            default :
              throw new RuntimeException("invalid type " + type);
          }
        }    
        cache = null;
        undeployAndDelete(toDelete, true, repositoryEvent);
      } finally {
        GenerationBase.finishUndeploymentHandler();
      }
    }
  }


  private boolean isUsedByBlacklistedObject(String fqXmlName, XMOMType type, XMOMDBSearchResultCache cache) {
    //check saved
    try {
      getSavedDependencies(fqXmlName, false, cache);
    } catch (PersistenceLayerException e) {
      return true;
    } catch (XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT e) {
      return true;
    }
    
    //check deployed
    try {
      checkUndeploy(fqXmlName, type, cache);
    } catch (XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT e) {
      return true;
    }
    return false;
  }

  private void undeployAndDelete(Map<String, XMOMType> toDelete, boolean disableChecksForRunningOrders, RepositoryEvent event) {
    Map<XMOMType, List<String>> objects = new HashMap<XMOMType, List<String>>();
    for (Entry<String, XMOMType> entry : toDelete.entrySet()) {
      List<String> list = objects.get(entry.getValue());
      if (list == null) {
        list = new ArrayList<String>();
        objects.put(entry.getValue(), list);
      }
      list.add(entry.getKey());
    }
    try {
      GenerationBase.undeployAndDelete(objects, disableChecksForRunningOrders, DependentObjectMode.INVALIDATE, true, revision, event,
                                       false);
    } catch (XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT e) {
      //sollte nicht passieren. wir haben ja extra nur objekte gewählt, die nicht mehr von anderen nicht-zu-löschenden objekten verwendet werden
      //ausserdem wird die exception wohl nur geworfen, wenn der DependentObjectMode auf "Protect" steht
      logger.warn("Could not delete all objects", e);
    }
  }



  /**
   * wirft fehler, wenn objekte in den dependencies gefunden werden, die nicht entfernt werden dürfen
   */
  private Set<XMOMDatabaseSearchResultEntry> getSavedDependencies(String fullXmlName, boolean deleteDependencies, XMOMDBSearchResultCache cache)
      throws PersistenceLayerException, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {

    Set<XMOMDatabaseSearchResultEntry> result = new HashSet<XMOMDatabaseSearchResultEntry>();
    try {
      searchSavedDepsRecursively(fullXmlName, result, deleteDependencies, cache);
    } catch (XNWH_InvalidSelectStatementException e) {
      throw new RuntimeException(e);
    }
    return result;
  }


  //enthält wraps und groups, weil dadurch keine falschen haupttypen (datentypen, exceptions, workflows) ermittelt werden,
  //sondern nur falsche zwischentypen.
  private static XMOMDatabaseEntryColumn[][] columns = {
      //exception
      {XMOMDatabaseEntryColumn.EXTENDEDBY, XMOMDatabaseEntryColumn.POSSESSEDBY, XMOMDatabaseEntryColumn.NEEDEDBY,
          XMOMDatabaseEntryColumn.PRODUCEDBY, XMOMDatabaseEntryColumn.THROWNBY, XMOMDatabaseEntryColumn.INSTANCESUSEDBY

      },
      //datatype
      {XMOMDatabaseEntryColumn.EXTENDEDBY, XMOMDatabaseEntryColumn.POSSESSEDBY, XMOMDatabaseEntryColumn.NEEDEDBY,
          XMOMDatabaseEntryColumn.PRODUCEDBY, XMOMDatabaseEntryColumn.WRAPS, XMOMDatabaseEntryColumn.INSTANCESUSEDBY},
      //service und workflow
      {XMOMDatabaseEntryColumn.CALLEDBY},
      //operation
      {XMOMDatabaseEntryColumn.GROUPEDBY},
      //servicegroup
      {XMOMDatabaseEntryColumn.GROUPS}};

  
  private static PreparedXMOMDatabaseSelect preparedSearch;
  
  
  private PreparedXMOMDatabaseSelect prepareSearch(List<XMOMDatabaseSelect> selects) {
    //das prepare cachen: dafür ist es egal, was für werte in den whereclauses stecken
    if (preparedSearch == null) {
      preparedSearch = xmomDatabase.prepareSearch(selects);
    }
    return preparedSearch;
  }
  
  private static class XMOMDBSearchResultCache {
    private Map<String, XMOMDatabaseSearchResult> cache = new HashMap<>();
    Set<String> checkedForUndeployment = new HashSet<>();
  }


  private void searchSavedDepsRecursively(String fullXmlName, Set<XMOMDatabaseSearchResultEntry> allFoundDeps, boolean deleteDependencies, XMOMDBSearchResultCache cache)
      throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT, XNWH_InvalidSelectStatementException, PersistenceLayerException {
    XMOMDatabaseSearchResult xmomDbDeps = cache.cache.get(fullXmlName);
    if (xmomDbDeps == null) {
      List<XMOMDatabaseSelect> selects = new ArrayList<XMOMDatabaseSelect>();

      for (XMOMDatabaseEntryColumn[] columnsOfType : columns) {
        XMOMDatabaseSelect s = new XMOMDatabaseSelect();
        s.addDesiredResultTypes(XMOMDatabaseType.GENERIC);
        for (XMOMDatabaseEntryColumn col : columnsOfType) { //liefert im resultset auch die objekte, die im "fullxmlname"-objekt in der beziehung eingetragen sind
          s.select(col);
        }
        s.where(XMOMDatabaseEntryColumn.FQNAME).isEqual(fullXmlName).and().whereNumber(XMOMDatabaseEntryColumn.REVISION).isEqual(revision);
        selects.add(s);
      }
      
      PreparedXMOMDatabaseSelect prepared = prepareSearch(selects);
      xmomDbDeps = xmomDatabase.executePreparedSelect(prepared, selects, Integer.MAX_VALUE, revision);
      cache.cache.put(fullXmlName, xmomDbDeps);
    }
    
    for (XMOMDatabaseSearchResultEntry e : xmomDbDeps.getResult()) {
      if (allFoundDeps.contains(e)) {
        continue;
      }

      if (deleteDependencies || existingSavedDependencyMustBeDeleted(fullXmlName, e)) {
        allFoundDeps.add(e);
        searchSavedDepsRecursively(e.getFqName(), allFoundDeps, deleteDependencies, cache);
      } else {
        //dependency gefunden, aber dependencies sollen nicht gelöscht werden.dependency ist offenbar blacklisted
        throw new XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT(fullXmlName, new RuntimeException(fullXmlName + " is used by " + e.getFqName() + "."));
      }
    }

  }


  /**
   * exception, falls objekt nicht undeployed werden darf, weil es abhängigkeiten besitzt, die nicht in toDelete enthalten sind.
   * abhängigkeiten werden zu allFoundDependencies hinzugefügt.
   * das objekt selbst wird zu toUndeploy hinzugefügt.
   * @param fullXmlName
   * @param type
   * @throws XPRC_MDMUndeploymentException
   */
  private void checkUndeploy(String fullXmlName, XMOMType type, XMOMDBSearchResultCache cache)
      throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    if (cache.checkedForUndeployment.contains(fullXmlName)) {
      return;
    }
    DependencySourceType depType;
    switch (type) {
      case DATATYPE :
        depType = DependencySourceType.DATATYPE;
        break;
      case EXCEPTION :
        depType = DependencySourceType.XYNAEXCEPTION;
        break;
      case FORM :
        return;
      case WORKFLOW :
        depType = DependencySourceType.WORKFLOW;
        break;
      default :
        throw new RuntimeException("unsupported type: " + type);
    }
    DependencyNode node = depReg.getDependencyNode(fullXmlName, depType, revision);
    if (node != null) { //deployed
      Set<String> dependencies = retrieveDependencyRegisterDependencies(node);
      boolean undeploy =
          dependencies.size() == 0 || existingDeployedDependenciesMustBeDeleted(dependencies);

      if (undeploy) {
        //alle dependencies wurden damit überprüft und müssen nicht nochmal überprüft werden
        cache.checkedForUndeployment.addAll(dependencies);
        cache.checkedForUndeployment.add(fullXmlName);
      } else {
        throw new XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT(fullXmlName, new RuntimeException(fullXmlName + " is used by other objects."));
      }
    }

  }


  /**
   * @return true falls das xmom objekte auch gelöscht werden muss. false sonst 
   */
  private boolean existingSavedDependencyMustBeDeleted(String parent, XMOMDatabaseSearchResultEntry e) {
    switch (e.getType()) {
      case DATATYPE :
      case EXCEPTION :
      case WORKFLOW :
        break;
      case OPERATION :
      case SERVICE :
      case SERVICEGROUP :
        return true;
      case GENERIC :
      default :
        throw new RuntimeException("unsupported type " + e.getType());
    }
    if (!allXMOMToDelete.contains(e.getFqName())) {
      if (allXMOMToKeep.contains(e.getFqName())) {
        return false;
      }
      //vermutlich ein relikt von dependency im xmomdatabase.
      logger.info("ignoring saved dependency " + e.getFqName() + " -> " + parent);
    }
    return true;
  }


  /**
   * @return true falls alle dependencies auch gelöscht werden müssen, false sonst
   */
  private boolean existingDeployedDependenciesMustBeDeleted(Set<String> dependencies) {
    for (String dep : dependencies) {
      if (!allXMOMToDelete.contains(dep)) {
        return false;
      }
    }
    return true;
  }


  private Set<String> retrieveDependencyRegisterDependencies(DependencyNode node) {
    Set<DependencyNode> nodes =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister()
            .getDependencies(node.getUniqueName(), node.getType(), revision, true);
    Set<String> gbDeps = new HashSet<String>();
    for (DependencyNode dn : nodes) {
      if (!dn.getRevision().equals(revision) || (dn.getUniqueName().equals(node.getUniqueName()) && dn.getType() == node.getType())) {
        //eigenes objekt nicht zurückgeben, andere revision nicht zurückgeben
        continue;
      }
      switch (dn.getType()) {
        case DATATYPE :
        case WORKFLOW :
        case XYNAEXCEPTION :
          gbDeps.add(dn.getUniqueName());
          break;

        default :
          break;
      }
    }
    return gbDeps;
  }


  private void clearFilterInstances() throws PersistenceLayerException {
    for (Filter f : xt.getFilters(revision)) {
      for (FilterInstanceStorable fi : xt.getFilterInstancesForFilter(f.getName(), revision, false)) {
        if (!blackListWithApplicationContent.getFilterInstanceNames().contains(fi.getFilterInstanceName())) {
          if (logger.isDebugEnabled()) {
            logger.debug("removing filterinstance " + fi.getFilterInstanceName() + ".");
          }
          try {
            ConnectionFilterInstance<?> cfi = xt.undeployFilter(fi.getFilterInstanceName(), revision);
            disabledFilterInstances.remove(cfi);
          } catch (XACT_FilterNotFound e) {
            throw new RuntimeException(e);
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("skipping filterinstance " + fi.getFilterInstanceName() + " because it is contained in blacklist.");
          }
        }
      }
    }
  }


  private void clearTriggerInstances() throws PersistenceLayerException {
    for (Trigger t : xt.getTriggers(revision)) {
      try {
        for (TriggerInstanceStorable ti : xt.getTriggerInstancesForTrigger(t.getTriggerName(), revision, false)) {
          if (!blackListWithApplicationContent.getTriggerInstanceNames().contains(ti.getTriggerInstanceName())) {
            if (xt.getFilterInstancesForTriggerInstance(ti.getTriggerInstanceName(), revision, true).size() == 0) {
              if (logger.isDebugEnabled()) {
                logger.debug("removing triggerinstance " + ti.getTriggerInstanceName() + ".");
              }
              try {
                EventListenerInstance<?, ?> eli = xt.undeployTrigger(t.getTriggerName(), ti.getTriggerInstanceName(), revision);
                stoppedTriggerInstances.remove(eli);
              } catch (XACT_TriggerInstanceNotFound e) {
                throw new RuntimeException(e);
              }
            } else {
              if (logger.isDebugEnabled()) {
                logger.debug("skipping triggerinstance " + ti.getTriggerInstanceName() + " because it still contains filterinstances.");
              }
            }
          } else {
            if (logger.isDebugEnabled()) {
              logger.debug("skipping triggerinstance " + ti.getTriggerInstanceName() + " because it is contained in blacklist.");
            }
          }
        }
      } catch (XACT_TriggerNotFound e) {
        throw new RuntimeException(e);
      }
    }
  }


  private void clearFilter(RepositoryEvent repositoryEvent) throws PersistenceLayerException {
    for (Filter f : xt.getFilters(revision)) {
      if (!blackListWithApplicationContent.getFilterNames().contains(f.getName())) {
        if (xt.getFilterInstancesForFilter(f.getName(), revision, true).size() == 0) {
          if (logger.isDebugEnabled()) {
            logger.debug("removing filter " + f.getName() + ".");
          }
          try {
            xt.removeFilter(f.getName(), revision, repositoryEvent, false);
          } catch (XACT_FilterNotFound e) {
            throw new RuntimeException(e);
          } catch (XACT_FilterMayNotBeRemovedIsDeployedException e) {
            throw new RuntimeException(e);
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("skipping filter " + f.getName() + " because it is used by filterinstances.");
          }
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("skipping filter " + f.getName() + " because it is contained in blacklist.");
        }
      }
    }
  }


  private void clearTrigger(RepositoryEvent repositoryEvent) throws PersistenceLayerException {
    //alle trigger entfernen, die nicht in der blacklist stehen und die keine existierenden abhängigen objekte besitzen (filter, triggerinstanzen)
    for (Trigger t : xt.getTriggers(revision)) {
      if (!blackListWithApplicationContent.getTriggersNames().contains(t.getTriggerName())) {
        try {
          if (xt.getTriggerInstancesForTrigger(t.getTriggerName(), revision, true).size() == 0) {
            if (xt.getFilters(t.getTriggerName(), revision, true).length == 0) {
              if (logger.isDebugEnabled()) {
                logger.debug("removing trigger " + t.getTriggerName() + ".");
              }
              try {
                xt.removeTrigger(t.getTriggerName(), revision, repositoryEvent);
              } catch (XACT_TriggerMayNotBeRemovedIsDeployedException e) {
                //oben geprüft, dass dem nicht so ist
                throw new RuntimeException(e);
              }
            } else {
              if (logger.isDebugEnabled()) {
                logger.debug("skipping trigger " + t.getTriggerName() + " because it is used by filters.");
              }
            }
          } else {
            if (logger.isDebugEnabled()) {
              logger.debug("skipping trigger " + t.getTriggerName() + " because it is used by triggerinstances.");
            }
          }
        } catch (XACT_TriggerNotFound e) {
          throw new RuntimeException(e);
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("skipping trigger " + t.getTriggerName() + " because it is contained in blacklist.");
        }
      }
    }
  }


}
