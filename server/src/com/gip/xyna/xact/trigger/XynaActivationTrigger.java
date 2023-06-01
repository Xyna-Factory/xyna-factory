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

package com.gip.xyna.xact.trigger;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xact.exceptions.XACT_AdditionalDependencyDeploymentException;
import com.gip.xyna.xact.exceptions.XACT_DuplicateFilterDefinitionException;
import com.gip.xyna.xact.exceptions.XACT_DuplicateTriggerDefinitionException;
import com.gip.xyna.xact.exceptions.XACT_ErrorDuringFilterAdditionRollback;
import com.gip.xyna.xact.exceptions.XACT_ErrorDuringTriggerAdditionRollback;
import com.gip.xyna.xact.exceptions.XACT_FilterImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_FilterInstanceNeedsEnabledFilterException;
import com.gip.xyna.xact.exceptions.XACT_FilterMayNotBeRemovedIsDeployedException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_IncompatibleFilterImplException;
import com.gip.xyna.xact.exceptions.XACT_IncompatibleTriggerImplException;
import com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterException;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xact.exceptions.XACT_LibOfFilterImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_LibOfTriggerImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_OldFilterVersionInstantiationException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerHasFilterInstanceInErrorStateException;
import com.gip.xyna.xact.exceptions.XACT_TriggerImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNeedsEnabledTriggerException;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerMayNotBeRemovedIsDeployedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.exceptions.XACT_WrongTriggerException;
import com.gip.xyna.xact.trigger.CommandWithFolderBackup.InternalException;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.FilterInstanceStorable.FilterInstanceState;
import com.gip.xyna.xact.trigger.FilterStorable.FilterState;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable.TriggerInstanceState;
import com.gip.xyna.xact.trigger.TriggerStorable.TriggerState;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EmptyRepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EventType;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.RepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilterInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_ExceptionClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_MDMObjectClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.exceptions.XFMG_TriggerClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_WFClassLoaderNotFoundException;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.statistics.XynaStatistics;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase.ClassLoadingDependencySource;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer.AdditionalDependencyType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DeploymentLocks;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.statustracking.StatusChangeProvider;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;


@SuppressWarnings("rawtypes")
public class XynaActivationTrigger extends Section implements TriggerManagement {

  public static final int FUTUREEXECUTION_DEPLOYTRIGGER_ID = XynaFactory.getInstance().getFutureExecution().nextId();
  public static final int FUTUREEXECUTION_ADDTRIGGER_ID = XynaFactory.getInstance().getFutureExecution().nextId();
  public static final String DEFAULT_NAME = "Xyna Trigger and Filter Management";
  public static final Logger logger = CentralFactoryLogging.getLogger(XynaActivationTrigger.class);

  static {
    XynaFactoryPath path = new XynaFactoryPath(XynaProcessing.class, XynaProcessCtrlExecution.class,
                                               StatusChangeProvider.class);
    XynaFactoryPath path2 = new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryControl.class,
                                                DependencyRegister.class);
    ArrayList<XynaFactoryPath> pathes = new ArrayList<XynaFactoryPath>();
    pathes.add(path);
    pathes.add(path2);
    addDependencies(XynaActivationTrigger.class, pathes);
  }

  private TriggerStorage storage;
  private final ReentrantLock managementLock = new ReentrantLock();
  
  // verwaltung von triggern und filtern. methoden um von aussen filter und trigger zu instantiieren
  // und sie vorher zu registrieren (validieren)
  // usecase: httptrigger adden, httptrigger an 3 verschiedenen ports (verschiedene startparameter)
  // instantiieren, dann verschiedene filter (evtl mehrfach an unterschiedlichen triggern)
  // registrieren.

  
  public static interface XmlElements {

    public static final String TRIGGER = "Trigger";
    public static final String FILTER = "Filter";
  }
  

  private static final Comparator<TriggerInstanceStorable> triggerInstanceComparator = new Comparator<TriggerInstanceStorable>() {

    public int compare(TriggerInstanceStorable o1, TriggerInstanceStorable o2) {
      if (o1.getTriggerInstanceName() == null) {
        return -1;
      }
      if (o2.getTriggerInstanceName() == null) {
        return 1;
      }
      int ret = o1.getTriggerInstanceName().compareToIgnoreCase(o2.getTriggerInstanceName());
      if (ret == 0 && o1.getRevision() != null && o2.getRevision() != null) {
        return o1.getRevision().compareTo(o2.getRevision());
      }
      return ret;
    }
  };


  public XynaActivationTrigger() throws XynaException {
    super();
  }

  //enthält alle enabled Triggerinstanzen, der Thread muss nicht laufen (z.B. falls eine Filterinstanz einen Fehler hat)
  //alle laufenden Trigger sind in XynaProcessCtrlExecution.eventListenerThreads zu finden
  private Map<Long, Map<String, EventListenerInstance<?, ?>>> eventListenerInstancesByName =
      new ConcurrentHashMap<Long, Map<String, EventListenerInstance<?, ?>>>();

  @Override
  public void init() throws XynaException {
    storage = new TriggerStorage();
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask(FUTUREEXECUTION_ADDTRIGGER_ID, "XynaActivationTrigger.futureExecutionRestoreTriggersAndFiltersAtStartup").
      after(DependencyRegister.ID_FUTURE_EXECUTION,WorkflowDatabase.FUTURE_EXECUTION_ID).
      before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
      
      execAsync(new Runnable() { public void run() { restoreTriggersAndFiltersAtStartup(); }});
    
    fExec.addTask(FUTUREEXECUTION_DEPLOYTRIGGER_ID,"XynaActivationTrigger.futureExecutionDeploying").
      after(XynaStatistics.FUTUREEXECUTION_ID).
      after(IDGenerator.class).
      after(FUTUREEXECUTION_ADDTRIGGER_ID).
      execAsync(new Runnable() { public void run() { futureExecutionDeploying(); }}); 
  
  }
  
  
  public static Collection<AdditionalDependencyContainer> getAdditionalFilterDependenciesPreInit() throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(FilterStorable.class);
    
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    Collection<FilterStorable> filters = con.loadCollection(FilterStorable.class);
    Collection<AdditionalDependencyContainer> adcs = new ArrayList<AdditionalDependencyContainer>();
    for (FilterStorable fi : filters) {
      File xmlDefinition = new File(getFilterXmlLocationByFqFilterClassName(fi.getFqFilterClassName(), fi.getRevision()));
      if (xmlDefinition.exists()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Found information on additional dependencies for filter " + fi.getFqFilterClassName() + " revision " + fi.getRevision());
        }
        try {
          Document d = XMLUtils.parse(xmlDefinition.getAbsolutePath());
          if (d.getDocumentElement().getTagName().equals(XmlElements.FILTER)) {
            adcs.add(new AdditionalDependencyContainer(d.getDocumentElement(), fi.getRevision()));
          }
        } catch (Ex_FileAccessException e) {
          logger.debug(null, e);
        } catch (XPRC_XmlParsingException e) {
          logger.debug(null, e);
        }
      }
    }
    
    return adcs;
  }
  

  private void restoreTriggersAndFiltersAtStartup() {

    Collection<TriggerStorable> triggerStorables = new HashSet<TriggerStorable>();
    try {
      triggerStorables = storage.loadCollection(TriggerStorable.class);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not load stored triggers.", e);
    }

    for (TriggerStorable triggerStorable : triggerStorables) {
      try {
        if (triggerStorable.getStateAsEnum() != TriggerState.EMPTY) {
          addTrigger(triggerStorable);
        }
      } catch (Exception e) {
        logger.warn("Trigger " + triggerStorable.getTriggerName() + " could not be registered and has been disabled.",
                    e);
        XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,"Failed to register trigger "
            + triggerStorable.getTriggerName() + getApplicationInfo(triggerStorable.getRevision()));

        storage.setTriggerError(triggerStorable.getTriggerName(), triggerStorable.getRevision(), e);
      }
    }

    Collection<FilterStorable> filterStorables = new HashSet<FilterStorable>();
    try {
      filterStorables = storage.loadCollection(FilterStorable.class);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not load stored filters.", e);
    }

    for (FilterStorable filterStorable : filterStorables) {
      try {
        if (filterStorable.getStateAsEnum() != FilterState.EMPTY) {
          addFilter(filterStorable);
        }
      } catch (Exception e) {
        logger.warn("Filter " + filterStorable.getFilterName() + " could not be registered and has been disabled.", e);
        XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,"Failed to register filter "
            + filterStorable.getFilterName() + getApplicationInfo(filterStorable.getRevision()));
        
        storage.setFilterError(filterStorable.getFilterName(), filterStorable.getRevision(), e);
      }
    }
  }


  private String getApplicationInfo(Long revision) {
    String runtimeContext = "";
    if (!RevisionManagement.REVISION_DEFAULT_WORKSPACE.equals(revision)) {
      RuntimeContext rc;
      try {
        rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                .getRuntimeContext(revision);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        rc = new Application("unknown", String.valueOf(revision));
      }
      runtimeContext = " in " + rc;
    }
    
    return runtimeContext;
  }

  public void futureExecutionDeploying() {
      Collection<TriggerInstanceStorable> triggerInstanceStorables = new HashSet<TriggerInstanceStorable>();
      try {
        triggerInstanceStorables = storage.loadCollection(TriggerInstanceStorable.class);
      } catch (PersistenceLayerException e) {
        logger.error("Could not load stored trigger instances.", e);
        return;
      }

      Map<Long, List<String>> disabledTriggerInstances = new HashMap<Long, List<String>>();
      for (TriggerInstanceStorable triggerInstanceStorable : triggerInstanceStorables) {
        boolean disabled = false;
        if (triggerInstanceStorable.getStateAsEnum() == TriggerInstanceState.DISABLED) {
          disabled = true;
        } else {
          try {
            boolean startThread = true; //der Thread wird erst in einem FutureExecutionTask nach dem Deployment der Filter gestartet
            deployTrigger(triggerInstanceStorable, startThread, -1);
          } catch (XACT_TriggerInstanceNeedsEnabledTriggerException e) {
            XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,"Trigger instance "
                            + triggerInstanceStorable.getTriggerInstanceName()
                            + " could not be deployed because trigger could not be registered.");
            disabled = true;
          } catch (Throwable t) {
            Department.handleThrowable(t);
            XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,"Failed to deploy trigger instance "
                + triggerInstanceStorable.getTriggerInstanceName());
            logger.warn("Could not deploy stored trigger instances.", t);
            disabled = true;
          }
        }
        
        if (disabled) {
          List<String> disabledTriggerInstancesForRevision = disabledTriggerInstances.get(triggerInstanceStorable.getRevision());
          if(disabledTriggerInstancesForRevision == null) {
            disabledTriggerInstancesForRevision = new ArrayList<String>();
            disabledTriggerInstances.put(triggerInstanceStorable.getRevision(), disabledTriggerInstancesForRevision);
          }
          disabledTriggerInstancesForRevision.add(triggerInstanceStorable.getTriggerInstanceName());
        }
      }

      // filter deployen
      Collection<FilterInstanceStorable> filterInstanceStorables = new HashSet<FilterInstanceStorable>();
      try {
        filterInstanceStorables = storage.loadCollection(FilterInstanceStorable.class);
      } catch (PersistenceLayerException e) {
        logger.error("Could not load stored filter instances.", e);
        return;
      }

      //sort filter instances
      List<FilterInstanceStorable> filterInstanceStorablesSorted = new ArrayList<FilterInstanceStorable>(filterInstanceStorables);
      Collections.sort(filterInstanceStorablesSorted, (x, y) -> x.getId() != null ? (y.getId() != null ? x.getId().compareTo(y.getId()) : -1) : 1);

      for (FilterInstanceStorable filterInstanceStorable : filterInstanceStorablesSorted) {
        if (filterInstanceStorable.getStateAsEnum() != FilterInstanceState.DISABLED) {
          List<String> disabledTriggerInstancesForRevision =
              disabledTriggerInstances.get(filterInstanceStorable.getRevision());
          if (disabledTriggerInstancesForRevision != null && disabledTriggerInstancesForRevision.contains(filterInstanceStorable.getTriggerInstanceName())) {
            XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,"Filter instance " + filterInstanceStorable.getFilterInstanceName() 
                                                                        + " could not be deployed because trigger instance "
                                                                        + filterInstanceStorable.getTriggerInstanceName() + " is disabled.");
          } else {
            try {
              deployFilterInternally(filterInstanceStorable, false, true);
            } catch (Throwable t) {
              logger.warn("Could not deploy stored filter instances.", t);
              XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,"Failed to deploy filter instance "
                  + filterInstanceStorable.getFilterInstanceName());
            }
          }
        }
      }
    }
  
  

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }
  
  private void addTrigger(TriggerStorable triggerStorable) throws  XynaException {
    
    String triggerName = triggerStorable.getTriggerName();
    File[] jarFilesAsArray = triggerStorable.getJarFilesAsArray();
    String fqTriggerClassName = triggerStorable.getFqTriggerClassName();
    String[] sharedLibs = triggerStorable.getSharedLibsAsArray();
    
    addNewTriggerInternally(triggerName, triggerStorable.getRevision(), jarFilesAsArray, fqTriggerClassName, sharedLibs, true, false, false, "at startup", true, new EmptyRepositoryEvent());
  }


  
  /*
   *  Aufruf von außerhalb (ChannelPortal) 
   */
  public void addTrigger(String triggerName, ZipInputStream jarFiles, String fqTriggerClassName, String[] sharedLibs,
                         String description, String startParameterDocumentation, long revision) throws XynaException {

    // save jarfiles
    File tempfolder = new File("temp_" + System.currentTimeMillis());
    File[] files = FileUtils.saveZipToDir(jarFiles, tempfolder);
    try {
      addTriggerInternally(triggerName, revision, files, fqTriggerClassName, sharedLibs, false, false, false, new SingleRepositoryEvent(revision));
    } finally {
      if (files != null && files.length > 0) {
        for (File file : files) {
          FileUtils.deleteFileWithRetries(file);
        }
      }
      tempfolder.delete();
    }
  }


  /*
   * wird von außen aufgerufen (ChannelPortal und CLI-Command)
   */
  public void removeTrigger(String nameOfTrigger) throws XACT_TriggerNotFound,
                  XACT_TriggerMayNotBeRemovedIsDeployedException, PersistenceLayerException {

    removeTrigger(nameOfTrigger, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  public void removeTrigger(String nameOfTrigger, Long revision) throws XACT_TriggerNotFound,
                  XACT_TriggerMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    removeTrigger(nameOfTrigger, revision, new SingleRepositoryEvent(revision));
  }
  
  public void removeTrigger(String nameOfTrigger, Long revision, RepositoryEvent repositoryEvent) throws XACT_TriggerNotFound,
                  XACT_TriggerMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    managementLock.lock();
    try {
      Trigger trigger = getTrigger(revision, nameOfTrigger, false);

      List<TriggerInstanceStorable> triggerInstancesByTriggerName = storage.getTriggerInstancesByTriggerName(trigger.getTriggerName(), revision, true);
      if (triggerInstancesByTriggerName.size() > 0) {
        throw new XACT_TriggerMayNotBeRemovedIsDeployedException(nameOfTrigger);
      }

      //filters are not allowed to exist without their trigger
      List<FilterStorable> filtersOnTrigger = storage.getFiltersByTriggerName(nameOfTrigger, revision, true); 
      if (filtersOnTrigger.size() > 0) {
        throw new RuntimeException("Filters using this trigger must be removed first");
      }

      // delete from list of known triggers
      if (logger.isDebugEnabled()) {
        logger.debug("Removing trigger " + nameOfTrigger);
      }

      trigger.removeClassLoader();

      removeJarsOfTriggerOrFilter(trigger.getJarFiles());

      triggerRemoved(trigger);

      storage.deleteTrigger(nameOfTrigger, revision);

      repositoryEvent.addEvent(new ProjectCreationOrChangeProvider.TriggerChangeEvent(EventType.TRIGGER_REMOVE, nameOfTrigger, getSimpleClassName(trigger.getFQTriggerClassName())));
    } finally {
      managementLock.unlock();
    }
  }

  private void removeJarsOfTriggerOrFilter(File[] jars) {
    if (jars.length > 0) {
      File parentDir = jars[0].getParentFile();
      if (FileUtils.deleteDirectoryRecursively(parentDir)) {
        if (logger.isDebugEnabled()) {
          logger.debug("Deleted directory " + parentDir.getAbsolutePath());
        }
      } else {
        if (logger.isInfoEnabled()) {
          logger.info("Could not delete directory " + parentDir.getAbsolutePath());
        }
      }
    }
  }


  private void deployTrigger(TriggerInstanceStorable triggerInstanceStorable, boolean startThread, int processingLimit)
      throws XACT_TriggerNotFound, XACT_IncompatibleTriggerImplException, XACT_TriggerImplClassNotFoundException,
      XACT_InvalidStartParameterException, PersistenceLayerException, XFMG_SHARED_LIB_NOT_FOUND,
      XACT_LibOfTriggerImplNotFoundException, 
      XACT_TriggerCouldNotBeStartedException, XACT_AdditionalDependencyDeploymentException,
      XACT_TriggerInstanceNeedsEnabledTriggerException {

    String[] paras = triggerInstanceStorable.getStartParameterArray();
    String nameOfTrigger = triggerInstanceStorable.getTriggerName();
    String nameOfTriggerInstance = triggerInstanceStorable.getTriggerInstanceName();
    String desc = triggerInstanceStorable.getDescription();

    deployTrigger(nameOfTrigger, nameOfTriggerInstance, paras, desc, triggerInstanceStorable.getRevision(), startThread, processingLimit);
  }


  public void deployTrigger(String nameOfTrigger, String nameOfTriggerInstance, String[] startParameter, String description, Long revision,
                            boolean startThread) throws XACT_TriggerImplClassNotFoundException, XACT_IncompatibleTriggerImplException,
      XACT_TriggerNotFound, XACT_TriggerInstanceNeedsEnabledTriggerException, XACT_InvalidStartParameterException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, PersistenceLayerException,
      XACT_AdditionalDependencyDeploymentException, XACT_TriggerCouldNotBeStartedException {
    deployTrigger(nameOfTrigger, nameOfTriggerInstance, startParameter, description, revision, startThread, -1);
  }

  public void deployTrigger(String nameOfTrigger, String nameOfTriggerInstance, String[] startParameter, String description, Long revision,
                            boolean startThread, int processingLimit) throws XACT_TriggerImplClassNotFoundException, XACT_IncompatibleTriggerImplException,
                            XACT_TriggerNotFound, XACT_TriggerInstanceNeedsEnabledTriggerException, XACT_InvalidStartParameterException,
                            XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, PersistenceLayerException,
                            XACT_AdditionalDependencyDeploymentException, XACT_TriggerCouldNotBeStartedException {
    deployTriggerInternally(nameOfTrigger, nameOfTriggerInstance, startParameter, description, revision, false, startThread, processingLimit, false);
  }


  public void deployTriggerDisabled(String nameOfTrigger, String nameOfTriggerInstance, String[] startParameter, String description,
                                    Long revision) throws XACT_TriggerNotFound, PersistenceLayerException {
    boolean deployDisabled = true;
    boolean startThread = false;
    try {
      deployTriggerInternally(nameOfTrigger, nameOfTriggerInstance, startParameter, description, revision, deployDisabled, startThread, -1, false);
    } catch (XACT_TriggerInstanceNeedsEnabledTriggerException e) {
      throw new RuntimeException(e); //kann bei deploydisabled nicht passieren
    } catch (XACT_AdditionalDependencyDeploymentException e) {
      throw new RuntimeException(e); //kann bei deploydisabled nicht passieren
    } catch (XACT_TriggerCouldNotBeStartedException e) {
      throw new RuntimeException(e); //kann bei deploydisabled nicht passieren
    } catch (XACT_TriggerImplClassNotFoundException e) {
      throw new RuntimeException(e); //kann bei deploydisabled nicht passieren
    } catch (XACT_IncompatibleTriggerImplException e) {
      throw new RuntimeException(e); //kann bei deploydisabled nicht passieren
    } catch (XACT_InvalidStartParameterException e) {
      throw new RuntimeException(e); //kann bei deploydisabled nicht passieren
    } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
      throw new RuntimeException(e); //kann bei deploydisabled nicht passieren
    } catch (XACT_LibOfTriggerImplNotFoundException e) {
      throw new RuntimeException(e); //kann bei deploydisabled nicht passieren
    }
  }

  /**
   * Undeployment einer Triggerinstanz mit ihren Filterinstanzen. 
   * @param nameOfTrigger
   * @param nameOfTriggerInstance
   * @param revision
   * @return EventListenerInstance, falls die Triggerinstanz enabled war, sonst null
   * @throws XACT_TriggerNotFound
   * @throws PersistenceLayerException
   * @throws XACT_TriggerInstanceNotFound
   */
  public EventListenerInstance undeployTrigger(String nameOfTrigger, String nameOfTriggerInstance, Long revision)
                  throws XACT_TriggerNotFound, PersistenceLayerException, XACT_TriggerInstanceNotFound {
    return undeployTriggerInternally(nameOfTrigger, nameOfTriggerInstance, revision);
  }


  /*
   * wird von außen aufgerufen (ChannelPortal)
   */
  public void addFilter(String filterName, ZipInputStream jarFiles, String fqFilterClassName, String triggerName, String[] sharedLibs,
                        String description, long revision) throws XPRC_ExclusiveDeploymentInProgress, XACT_FilterImplClassNotFoundException,
      XACT_TriggerNotFound, PersistenceLayerException, Ex_FileAccessException,
      XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException, XACT_AdditionalDependencyDeploymentException,
      XACT_JarFileUnzipProblem, XACT_LibOfFilterImplNotFoundException, XFMG_SHARED_LIB_NOT_FOUND {
    // save jarFiles 
    File tempfolder = new File("temp_" + System.currentTimeMillis());
    File[] files = FileUtils.saveZipToDir(jarFiles, tempfolder);
    try {
      addFilterInternally(filterName, revision, files, fqFilterClassName, triggerName, sharedLibs,
                          description, false, false, false, new SingleRepositoryEvent(revision));
    } finally {
      if (files != null && files.length > 0) {
        for (File file : files) {
          FileUtils.deleteFileWithRetries(file);
        }
      }
      tempfolder.delete();
    }
  }


  /*
   * wird von außen aufgerufen (ChannelPortal und CLI-Command)
   */
  public void removeFilter(String nameOfFilter) throws XACT_FilterNotFound,
                  XACT_FilterMayNotBeRemovedIsDeployedException, PersistenceLayerException {

    removeFilter(nameOfFilter, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  /**
   * entfernt den filter in der revision überall dort, wo er als outdaten filter verwendet wird
   */
  private void removeOutdatedFilterFromOtherTriggers(String filterInstanceName, String triggerInstanceName, long revisionOfFilterInstance) {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                           .getRevisionManagement();
    List<Long> allRevisions = rm.getAllApplicationRevisions();
    for(Long otherRevision : allRevisions) {
      Set<String> allEli = getAllEventListenerInstanceNames(otherRevision);
      for (String instName : allEli) {
        //outdated filter sind nur an triggerinstanzen mit dem gleichen namen gebunden
        if (instName.equals(triggerInstanceName)) {
          EventListenerInstance eli = getEventListenerInstanceByName(instName, otherRevision, false);          
          eli.getEL().removeOutdatedFilter(filterInstanceName, revisionOfFilterInstance);
        }
      }
    }
  }

  private void callUndeploymentOfOutdatedFilterInstances(String filterInstanceName, String triggerInstanceName, long revisionOfFilterInstance) {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
.getRevisionManagement();
    List<Long> allRevisions = rm.getAllApplicationRevisions();
    for (Long revisionOfTriggerInstance : allRevisions) {
      Set<String> allEli = getAllEventListenerInstanceNames(revisionOfTriggerInstance);
      for (String instName : allEli) {
        //outdated filter sind nur an triggerinstanzen mit dem gleichen namen gebunden
        if (instName.equals(triggerInstanceName)) {
          EventListenerInstance<?, ?> eli = getEventListenerInstanceByName(instName, revisionOfTriggerInstance, false);
          for (ConnectionFilterInstance<?> cfi : eli.getEL().getAllOutdatedFilters()) {
            if (filterInstanceName.equals(cfi.getInstanceName())) {
              long revisionOfOutdatedFilter = ((ClassLoaderBase) cfi.getCF().getClass().getClassLoader()).getRevision();
              if (revisionOfFilterInstance == revisionOfOutdatedFilter) {
                callUndeploymentOfFilterInstance(cfi.getCF(), eli.getEL());
              }
            }
          }
        }
      }
    }
  }
  
  public void removeFilter(String nameOfFilter, Long revision) throws XACT_FilterNotFound,
                  XACT_FilterMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    removeFilter(nameOfFilter, revision, new SingleRepositoryEvent(revision), false);
  }
  
  public void removeFilter(String nameOfFilter, Long revision, RepositoryEvent repositoryEvent) throws XACT_FilterNotFound,
                  XACT_FilterMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    removeFilter(nameOfFilter, revision, repositoryEvent, false);
  }

  public void removeFilter(String nameOfFilter, Long revisionOfFilter, RepositoryEvent repositoryEvent,
                           boolean undeployingInstances) throws XACT_FilterNotFound,
                  XACT_FilterMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    managementLock.lock();
    try {
      Filter f = getFilter(revisionOfFilter, nameOfFilter, false);
      
      Collection<FilterInstanceStorable> fis = getFilterInstancesForFilter(nameOfFilter, revisionOfFilter, true);

      for (FilterInstanceStorable fi : fis) {
        if (fi.getStateAsEnum() == FilterInstanceState.ENABLED) {
          throw new XACT_FilterMayNotBeRemovedIsDeployedException(nameOfFilter);
        }
      }

      if (undeployingInstances) {
        for (FilterInstanceStorable fi : fis) {
          undeployFilterInternally(fi.getFilterInstanceName(), fi.getRevision(), false);
        }
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Removing filter " + nameOfFilter);
      }
      f.removeClassLoader();

      removeJarsOfTriggerOrFilter(f.getJarFiles());

      filterRemoved(f);
      
      storage.deleteFilter(nameOfFilter, revisionOfFilter);

      repositoryEvent.addEvent(new ProjectCreationOrChangeProvider.FilterChangeEvent(EventType.FILTER_REMOVE, nameOfFilter, getSimpleClassName(f.getFQFilterClassName())));
      
      //falls ein Trigger Thread wegen einem Fehler bei diesem Filter nicht laufen konnte
      //kann er nun evtl. gestartet werden
      for (FilterInstanceStorable fi : fis) {
        TriggerInstanceStorable tis;
        try {
          tis = storage.getTriggerInstanceByName(fi.getTriggerInstanceName(), revisionOfFilter, true);
        } catch (XACT_TriggerInstanceNotFound e) {
          throw new RuntimeException(e);
        }
        restartEventListener(fi.getTriggerInstanceName(), tis.getRevision());
      }
    } finally {
      managementLock.unlock();
    }
  }
  
  /**
   * Startet den Trigger Thread, falls der Trigger enabled ist, aber der Thread gestoppt ist
   * und keine fehlerhaften Filterinstanzen vorhanden sind
   */
  private void restartEventListener(String triggerInstanceName, Long revisionOfTriggerInstanceOrParentRevision) {
    EventListenerInstance eli = getEventListenerInstanceByName(triggerInstanceName, revisionOfTriggerInstanceOrParentRevision, true);
    if (eli != null) {
      boolean running =
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution()
              .getEventListenerByName(triggerInstanceName, eli.getRevision()) != null;
      if (!running) {
        try {
          startThreadForTriggerInstance(triggerInstanceName, eli.getRevision());
        } catch (XACT_TriggerHasFilterInstanceInErrorStateException e) {
          //ok, eine andere Filterinstanz ist fehlerhaft
        } catch (Exception e) {
          logger.warn("could not start trigger thread " + triggerInstanceName, e);
        }
      }
    }
  }
  
  /*
   * wird von außen aufgerufen (ChannelPortal und CLI-Command)
   */
  public void removeFilterWithUndeployingInstances(String nameOfFilter) throws XACT_FilterNotFound,
                  PersistenceLayerException {
    removeFilterWithUndeployingInstances(nameOfFilter, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  
  public void removeFilterWithUndeployingInstances(String nameOfFilter, Long revision) throws XACT_FilterNotFound,
                  PersistenceLayerException {
    try {
      removeFilter(nameOfFilter, revision, new SingleRepositoryEvent(revision), true);
    } catch (XACT_FilterMayNotBeRemovedIsDeployedException e) {
      throw new RuntimeException(e); //tritt wegen undeployingInstances nicht auf
    }
  }

  /**
   * Undeployment einer Filterinstanz. 
   * @param filterInstanceName
   * @param revision
   * @return ConnectionFilterInstance, falls die Filterinstanz enabled war, sonst null
   * @throws XACT_FilterNotFound
   * @throws PersistenceLayerException
   */
  public ConnectionFilterInstance undeployFilter(String filterInstanceName, Long revision) throws XACT_FilterNotFound, PersistenceLayerException {
    managementLock.lock();
    try {
      return undeployFilterInternally(filterInstanceName, revision, true);
    } finally {
      managementLock.unlock();
    }
  }
  
  public void undeployFilter(String filterInstanceName) throws XACT_FilterNotFound, PersistenceLayerException {
    undeployFilter(filterInstanceName, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public void addTrigger(String name, File[] jarFiles, String fqTriggerClassName, String[] sharedLibs, Long revision, RepositoryEvent repositoryEvent)
      throws XPRC_ExclusiveDeploymentInProgress, XACT_IncompatibleTriggerImplException, XACT_TriggerImplClassNotFoundException,
      Ex_FileAccessException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, PersistenceLayerException,
      XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException, XACT_JarFileUnzipProblem,
      XACT_ErrorDuringTriggerAdditionRollback, XACT_DuplicateTriggerDefinitionException {
    addTriggerInternally(name, revision, jarFiles, fqTriggerClassName, sharedLibs, false, false, false, repositoryEvent);
  }


  public void addTrigger(String name, File[] jarFiles, String fqTriggerClassName, String[] sharedLibs, Long revision, boolean addEmpty, boolean disableFailedInstances, RepositoryEvent repositoryEvent)
      throws XPRC_ExclusiveDeploymentInProgress, XACT_IncompatibleTriggerImplException, XACT_TriggerImplClassNotFoundException,
      Ex_FileAccessException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, PersistenceLayerException,
      XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException, XACT_JarFileUnzipProblem,
      XACT_DuplicateTriggerDefinitionException {
    addTriggerInternally(name, revision, jarFiles, fqTriggerClassName, sharedLibs, false, addEmpty, disableFailedInstances, repositoryEvent);
  }


  public void addTrigger(String name, File[] jarFiles, String fqTriggerClassName, String[] sharedLibs) throws  XynaException  {
    Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    addTriggerInternally(name, revision, jarFiles, fqTriggerClassName, sharedLibs, false, false, false, new SingleRepositoryEvent(revision));
  }
  

  private String repairPath(String path, String lastPathEntry) {
    // 1. suche nach vorkommen von <lastPathEntry>
    // 2. lösche alles vor <lastPathEntry> inkl. <lastPathEntry>/
    // Bsp. ./filter/DHCPFilter/DHCPFilter.jar --> DHCPFilter/DHCPFilter.jar
    int index = path.indexOf(lastPathEntry);
    if(index < 0) {
      logger.debug("Can't repair path of <" + path + "> with lastPathEntry = " + lastPathEntry);
      return path;
    }
    if(path.length() < lastPathEntry.length()) {
      logger.error("Can't repair path of <" + path + "> with lastPathEntry = " + lastPathEntry + ". Length of path is to small.");
      return path;
    }
    return path.substring(index + lastPathEntry.length());
  }


  /**
   * Falls der Trigger bereits existiert, wird er ueberschrieben. Diese methode updated den Trigger Code. Alle
   * bisherigen Triggerinstanzen dieses Triggers werden redeployed, damit der neue Code wirksam wird.
   */
  private void addTriggerInternally(String name, final Long revision, File[] jarFiles, final String fqTriggerClassName,
                                    String[] sharedLibs, boolean isStarting, boolean addEmpty, boolean disableFailedInstances,
                                    RepositoryEvent repositoryEvent)
      throws XPRC_ExclusiveDeploymentInProgress, Ex_FileAccessException, XACT_IncompatibleTriggerImplException,
      XACT_TriggerImplClassNotFoundException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, PersistenceLayerException,
      XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException, XACT_JarFileUnzipProblem,
      XACT_DuplicateTriggerDefinitionException {

    if (name.contains("#")) {
      // Zeichen wird für Primary-Key in Tabelle verwendet ... verboten!
      throw new RuntimeException("Illegal character '#' in name " + name);
    }

    managementLock.lock();
    try {
      TriggerStorable trigger = storage.getTriggerByName(name, revision, false);
      if(trigger.getStateAsEnum() == TriggerState.EMPTY) {
        //für vorher mittels addEmpty hinzugefügte trigger
        addNewTriggerInternally(name, revision, jarFiles, fqTriggerClassName, sharedLibs, 
                                isStarting, addEmpty, disableFailedInstances, "jars to existing trigger", false, repositoryEvent);
      } else {
        //bereits existierenden Trigger ändern
        addExistingTrigger(name, revision, jarFiles, fqTriggerClassName, sharedLibs, isStarting, disableFailedInstances, repositoryEvent);
      }
    } catch (XACT_TriggerNotFound e) {
      //neuen Trigger hinzufügen
      addNewTriggerInternally(name, revision, jarFiles, fqTriggerClassName, sharedLibs, isStarting, addEmpty, disableFailedInstances, "new trigger", true, repositoryEvent);
    } finally {
      managementLock.unlock();
    }
  }


  private String guessSourceFolderByJarFileLocation(String fqClassName, File[] jarFiles) {
    if (jarFiles == null || jarFiles.length == 0) {
      throw new RuntimeException("At least one jar file expected");
    }
    File oneJar = jarFiles[0];
    File parent = oneJar.getParentFile();
    while (!parent.getName().equals(getSimpleClassName(fqClassName))) {
      parent = parent.getParentFile();
      if (parent == null) {
        throw new RuntimeException();
      }
    }
    return parent.getAbsolutePath();
  }

    
  private void addNewTriggerInternally(String name, final Long revision, File[] jarFiles, final String fqTriggerClassName,
                                       String[] sharedLibs, boolean isStarting, boolean addEmpty, boolean disableFailedInstances, 
                                       String addCause, boolean validate, RepositoryEvent repositoryEvent) 
        throws PersistenceLayerException, XPRC_ExclusiveDeploymentInProgress, Ex_FileAccessException, 
               XACT_IncompatibleTriggerImplException, XACT_TriggerImplClassNotFoundException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, 
               XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException, XACT_DuplicateTriggerDefinitionException {
    if (logger.isDebugEnabled()) {
      logger.debug("adding "+addCause+" " + name + " of class " + fqTriggerClassName);
    }
    
    DeploymentLocks.writeLock(fqTriggerClassName, DependencySourceType.TRIGGER, "AddTrigger", revision);
    try {
      if (!isStarting && !addEmpty) {
        String deploymentPath = getTriggerDeploymentFolderByTriggerFqClassName(fqTriggerClassName, revision);
        String savedPath = guessSourceFolderByJarFileLocation(fqTriggerClassName, jarFiles);
        jarFiles = copySavedToDeployed(savedPath, deploymentPath, jarFiles).toArray(new File[1]);
      }

      //Überprüfen, dass nicht bereits ein Trigger mit anderem Namen, aber gleichem ClassLoader existiert
      if (validate) {
        try {
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                          .getTriggerClassLoader(fqTriggerClassName, revision, true);
          throw new XACT_DuplicateTriggerDefinitionException(fqTriggerClassName);
        } catch (XFMG_TriggerClassLoaderNotFoundException e) {
          //ok, ClassLoader existiert noch nicht
        }
      }
      
      if (addEmpty) {
        //nur TriggerStorable mit State EMPTY anlegen
        TriggerStorable newTriggerStorable =
                        new TriggerStorable(name, revision, new String[0],
                                            fqTriggerClassName, sharedLibs, TriggerState.EMPTY);
        storage.persistObject(newTriggerStorable);
        return;
      } else {
        Trigger t = new Trigger(name, revision, jarFiles, fqTriggerClassName, sharedLibs);
        addNewTriggerInternally(t);
      }
    } finally {
      DeploymentLocks.writeUnlock(fqTriggerClassName, DependencySourceType.TRIGGER, revision);
    }
    
    repositoryEvent.addEvent(new ProjectCreationOrChangeProvider.TriggerChangeEvent(EventType.TRIGGER_ADD, name, getSimpleClassName(fqTriggerClassName)));
  }


  private void addNewTriggerInternally(Trigger t) throws Ex_FileAccessException, XPRC_XmlParsingException, PersistenceLayerException, XACT_IncompatibleTriggerImplException, XACT_TriggerImplClassNotFoundException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, XPRC_InvalidXmlMissingRequiredElementException {        
    t.resetClassLoader();
    boolean successAfterNewClassloader = false;
    try {
      //Storable anlegen
      List<String> jarStrings = new ArrayList<String>();
      for (File jar : t.getJarFiles()) {
        jarStrings.add(repairPath(jar.getPath(), "trigger/"));
      }

      TriggerStorable newTriggerStorable =
        new TriggerStorable(t.getTriggerName(), t.getRevision(),
                            jarStrings.toArray(new String[1]),
                            t.getFQTriggerClassName(), t.getSharedLibs(), TriggerState.OK);
      storage.persistObject(newTriggerStorable);
      
      //TriggerChangedListener benachrichtigen
      triggerAdded(t);
      
      successAfterNewClassloader = true;
    } finally {
      if (!successAfterNewClassloader) {
        removeDependentFilterClassLoader(t.getTriggerName(), t.getRevision());
        t.removeClassLoader();
      }
    }
  }

  private void removeDependentFilterClassLoader(String triggerName, Long revision) {
    try {
      Filter[] filters = getFilters(triggerName, revision, true);
      for (Filter f : filters) {
        f.removeClassLoader();
      }
    } catch (Exception e) {
      logger.warn("Failed to remove filter classloader", e);
    }
  }
  
  private class AddTriggerCommand extends CommandWithFolderBackup {
    private String name;
    private String fqTriggerClassName;
    private File[] jarFiles;
    private String[] sharedLibs;
    private Long revision;
    private boolean isStarting;
    private boolean disableFailedInstances;
    private RepositoryEvent repositoryEvent;
    private Trigger oldTrigger;
    private List<File> copiedLibs;
    
    
    private AddTriggerCommand(String name, String fqTriggerClassName, File[] jarFiles,
                              String[] sharedLibs, Long revision, boolean isStarting, boolean disableFailedInstances,
                              RepositoryEvent repositoryEvent) {
      super();
      this.name = name;
      this.fqTriggerClassName = fqTriggerClassName;
      this.jarFiles = jarFiles;
      this.sharedLibs = sharedLibs;
      this.revision = revision;
      this.isStarting = isStarting;
      this.disableFailedInstances = disableFailedInstances;
      this.repositoryEvent = repositoryEvent;
    }
    
    @Override
    protected File backup() throws Ex_FileAccessException, XACT_JarFileUnzipProblem, InternalException {
      try {
        oldTrigger = getTrigger(revision, name, false);
      } catch (XACT_TriggerNotFound e) {
        throw new RuntimeException(e); //Trigger war schon als existierend bekannt -> sollte nicht vorkommen
      } catch (PersistenceLayerException e) {
        throw new InternalException(e);
      }
      
      return super.backup();
    }
    
    @Override
    protected void rollbackFailureTreatment(Throwable t) throws InternalException {
      //Trigger-Status auf ERROR setzen
      storage.setTriggerError(name, revision, new XACT_ErrorDuringTriggerAdditionRollback(name, t));
      
      throw new InternalException(new XACT_ErrorDuringTriggerAdditionRollback(name, t));
    }

  
    @Override
    protected void rollbackAndExecute() throws XACT_JarFileUnzipProblem, Ex_FileAccessException, InternalException {
      //rollback passiert in der mitte des executes
      try {
        executeInternally();
      } catch (InternalException e) {
        if (e.getCause() instanceof XACT_JarFileUnzipProblem) {
          throw (XACT_JarFileUnzipProblem) e.getCause();
        } else if (e.getCause() instanceof Ex_FileAccessException) {
          throw (Ex_FileAccessException) e.getCause();
        }
        throw e;
      }
    }

    @Override
    protected void executeInternally() throws InternalException {
      //try to remove old or new instances
      List<TriggerInstanceStorable> tiss;
      try {
        tiss = storage.getTriggerInstancesByTriggerName(name, revision, true);
      } catch (PersistenceLayerException e1) {
        throw new RuntimeException(e1);
      }
      for (TriggerInstanceStorable tis : tiss) {
        if (tis.getStateAsEnum() == TriggerInstanceState.ENABLED) {
          EventListenerInstance eli = getEventListenerInstanceByName(tis.getTriggerInstanceName(), tis.getRevision(), false);
          if (eli != null) {
            try {
              disableTriggerInstanceInternally(eli.getInstanceName(), eli.getRevision(), true, false);
            } catch (PersistenceLayerException e) {
              throw new RuntimeException(e); //sollte wegen persist=false nicht auftreten
            } catch (XACT_TriggerInstanceNotFound e) {
              throw new RuntimeException(e); //bestehende trigger instanz: konnte ja auch deployed werden
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              throw new RuntimeException(e); //bestehende trigger instanz: konnte ja auch deployed werden
            }
          }
        }
      }


      //jars kopieren: erstnachdem alle triggerinstanzen aus sind.
      //FIXME hier werden die jars kopiert, aber innerhalb von addTriggerInternally auch nochmal. das ist zuviel. das gleiche bei addFilter
      if (getCurrentPhase() == ExecutionPhase.EXECUTION) {
        try {
          String deploymentPath = getTriggerDeploymentFolderByTriggerFqClassName(fqTriggerClassName, revision);
          String savedPath = guessSourceFolderByJarFileLocation(fqTriggerClassName, jarFiles);
          copiedLibs = copySavedToDeployed(savedPath, deploymentPath, jarFiles);
        } catch (Ex_FileAccessException e) {
          throw new InternalException(e);
        }
      } else if (getCurrentPhase() == ExecutionPhase.ROLLBACK) {
        try {
          rollback();
        } catch (XACT_JarFileUnzipProblem e) {
          throw new InternalException(e);
        } catch (Ex_FileAccessException e) {
          throw new InternalException(e);
        }
      } else {
        throw new RuntimeException("unexpected state of " + CommandWithFolderBackup.class.getSimpleName() + ": " + getCurrentPhase());
      }
      
      try {
        //keine trigger instanzen mehr aktiv
        //ClassLoader existiert noch, daher addTriggerInternally mit validate = false aufrufen
        if (getCurrentPhase() != ExecutionPhase.ROLLBACK) {
          addNewTriggerInternally(name, revision, copiedLibs.toArray(new File[copiedLibs.size()]), fqTriggerClassName, sharedLibs, 
                                  isStarting, false, false, "existing trigger", false, repositoryEvent);
        } else {
          addNewTriggerInternally(name, revision, oldTrigger.getJarFiles(), oldTrigger.getFQTriggerClassName(), oldTrigger.getSharedLibs(), 
                                  isStarting, false, false, "existing trigger", false, repositoryEvent);
       }
      } catch (XPRC_ExclusiveDeploymentInProgress e) {
        throw new InternalException(e);
      } catch (XACT_IncompatibleTriggerImplException e) {
        throw new InternalException(e);
      } catch (XACT_TriggerImplClassNotFoundException e) {
        throw new InternalException(e);
      } catch (Ex_FileAccessException e) {
        throw new InternalException(e);
      } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
        throw new InternalException(e);
      } catch (XACT_LibOfTriggerImplNotFoundException e) {
        throw new InternalException(e);
      } catch (PersistenceLayerException e) {
        throw new InternalException(e);
      } catch (XPRC_XmlParsingException e) {
        throw new InternalException(e);
      } catch (XPRC_InvalidXmlMissingRequiredElementException e) {
        throw new InternalException(e);
      } catch (XACT_DuplicateTriggerDefinitionException e) {
        throw new RuntimeException(e); //sollte nicht vorkommen, da addTriggerInternally mit validate = false aufgerufen wird
      }

      //alte triggerinstanzen an den neuen trigger hängen (oder beim rollback an den alten trigger)
      Collection<TriggerInstanceStorable> triggerInstanceStorables;
      try {
        triggerInstanceStorables = storage.getTriggerInstancesByTriggerName(name, revision, true);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }

      for (TriggerInstanceStorable ti : triggerInstanceStorables) {
        if (ti.getStateAsEnum() == TriggerInstanceState.DISABLED) {
          continue;
        }

        try {
          deployTriggerInternally(name, ti.getTriggerInstanceName(), ti.getStartParameterArray(), ti.getDescription(), ti.getRevision(),
                                  false, true, -1, disableFailedInstances);
        } catch (Exception e) {
          logger.warn("could not redeploy trigger instance " + ti.getTriggerInstanceName(), e);
        }
      }
    }
  }
  
  private void addExistingTrigger(String name, Long revision, File[] jarFiles, String fqTriggerClassName,
                                  String[] sharedLibs, boolean isStarting, boolean disableFailedInstances,
                                  RepositoryEvent repositoryEvent) throws Ex_FileAccessException,
      XACT_JarFileUnzipProblem, XACT_TriggerImplClassNotFoundException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException,
      XACT_IncompatibleTriggerImplException, XPRC_ExclusiveDeploymentInProgress,
      PersistenceLayerException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException {

    String deploymentDirPath = getTriggerDeploymentFolderByTriggerFqClassName(fqTriggerClassName, revision);

    /*
     * TODO exceptionhandling verbessern
     * wie? ideen:
     * - exceptions gruppieren mittels gemeinsamer oberklasse oder mittels wrappen in gemeinsame exception
     *   - leider habe ich nicht viele sinnvolle gruppen gefunden bzw das würde keine nennenswerte verbesserung des codes ergeben.
     *   - auf java 7 warten, wo man exceptionhandling besser gruppieren kann
     */
    CommandWithFolderBackup addTriggerWithRollback = new AddTriggerCommand(name, fqTriggerClassName,
                                                                           jarFiles, sharedLibs, revision, isStarting,
                                                                           disableFailedInstances, repositoryEvent);
    
    try {
      addTriggerWithRollback.execute(new File(deploymentDirPath));
    } catch (InternalException e) {
      Throwable cause = e.getCause();
      if (e.getCause() instanceof XACT_ErrorDuringTriggerAdditionRollback) {
        cause = cause.getCause();
      }
      
      if (cause instanceof XACT_TriggerImplClassNotFoundException) {
        throw (XACT_TriggerImplClassNotFoundException) cause;
      } else if (cause instanceof XFMG_SHARED_LIB_NOT_FOUND) {
        throw (XFMG_SHARED_LIB_NOT_FOUND) cause;
      } else if (cause instanceof XACT_LibOfTriggerImplNotFoundException) {
        throw (XACT_LibOfTriggerImplNotFoundException) cause;
      } else if (cause instanceof Ex_FileAccessException) {
        throw (Ex_FileAccessException) cause;
      } else if (cause instanceof XACT_IncompatibleTriggerImplException) {
        throw (XACT_IncompatibleTriggerImplException) cause;
      } else if (cause instanceof XPRC_ExclusiveDeploymentInProgress) {
        throw (XPRC_ExclusiveDeploymentInProgress) cause;
      } else if (cause instanceof PersistenceLayerException) {
        throw (PersistenceLayerException) cause;
      } else if (cause instanceof XPRC_XmlParsingException) {
        throw (XPRC_XmlParsingException) cause;
      } else if (cause instanceof XPRC_InvalidXmlMissingRequiredElementException) {
        throw (XPRC_InvalidXmlMissingRequiredElementException) cause;
      } else {
        throw new RuntimeException(e.getCause());
      }
    }
  }


  /**
   * Deployed alle enabled Filterinstanzen einer Triggerinstanz
   * @param triggerInstanceName
   * @throws PersistenceLayerException 
   * @throws XFMG_SHARED_LIB_NOT_FOUND 
   */
  private void redeployFilterInstancesForTriggerInstance(EventListenerInstance eli, boolean disableFailedInstances) {
    String triggerInstanceName = eli.getInstanceName();
    long revisionOfTriggerInstance = eli.getRevision();
    List<FilterInstanceStorable> filterInstances;
    try {
      filterInstances = storage.getFilterInstancesByTriggerInstanceName(triggerInstanceName, revisionOfTriggerInstance, true);
    } catch (PersistenceLayerException e) {
      logger.warn("could not redeploy filter instances on trigger instance "
                      + triggerInstanceName + ".", e);
      return;
    }
    
    for (FilterInstanceStorable fi : filterInstances) {
      if (fi.getStateAsEnum() == FilterInstanceState.DISABLED) {
        continue;
      }
      
      try {
        instantiateConnectionFilter(fi, eli, true);
        if (fi.getStateAsEnum() == FilterInstanceState.ERROR) {
          //FilterInstance ist nun im Zustand ENABLED
          storage.setFilterInstanceState(fi.getFilterInstanceName(), fi.getRevision(), FilterInstanceState.ENABLED);
        }
      } catch (Exception e) {
        logger.warn("could not redeploy filter instance " + fi.getFilterInstanceName() + " on trigger instance "
                        + triggerInstanceName + ".", e);
        
        if (disableFailedInstances && e instanceof XACT_IncompatibleFilterImplException) {
          try {
            deployFilterInternallyDisabled(fi);
          } catch (PersistenceLayerException e1) {
            logger.warn("could not disable filter instance + " + fi.getFilterInstanceName(), e1);
          } catch (XACT_FilterNotFound e1) {
            throw new RuntimeException(e1);
          }
        }
      }
    }
  }

  public Set<String> getAllEventListenerInstanceNames(Long revision) {
    Map<String, EventListenerInstance<?, ?>> map = eventListenerInstancesByName.get(revision);
    if (map == null) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(map.keySet());
  }

  private EventListener redeployEventListener(Trigger trigger,
                                              EventListenerInstance<?, ?>  oldEl, String[] startParameter,
                                              String description, boolean persistChanges)
      throws XACT_IncompatibleTriggerImplException, XACT_TriggerImplClassNotFoundException,
      XACT_InvalidStartParameterException, XACT_LibOfTriggerImplNotFoundException,
      XACT_TriggerCouldNotBeStartedException,
      XACT_AdditionalDependencyDeploymentException, XFMG_SHARED_LIB_NOT_FOUND,
      XACT_TriggerInstanceNeedsEnabledTriggerException, PersistenceLayerException {
    
    if (logger.isDebugEnabled()) {
      logger.debug("redeployment of triggerinstance " + oldEl.getInstanceName() + " of trigger " + trigger.getTriggerName() + " ...");
    }

    //undeploy
    try {
      disableTriggerInstanceInternally(oldEl.getInstanceName(), oldEl.getRevision(), true, false);
    } catch (XACT_TriggerInstanceNotFound e) {
      throw new RuntimeException(e); //bestehende trigger instanz: konnte ja auch deployed werden
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e); //bestehende trigger instanz: konnte ja auch deployed werden
    }

    //neu deployen (Filter werden dabei wieder angehängt)
    return deployTriggerInternally(trigger, oldEl.getInstanceName(), startParameter, description, false,
                                   true, -1, false, oldEl.getRevision());
  }


  /**
   * Erstellt eine Instanz eines Triggers mit Hilfe der Startparameter. Gibt es schon eine Instanz mit dem gleichen
   * Namen, wird diese redeployed und die angehängten Filter bleiben erhalten.
   * -- bei deploydisabled kann passieren:
   * @throws XACT_TriggerNotFound 
   * @throws PersistenceLayerException 
   * -- bei deployenabled kann zusätzlich passieren:
   * @throws XACT_TriggerInstanceNeedsEnabledTriggerException
   * @throws XACT_TriggerCouldNotBeStartedException 
   * @throws XACT_AdditionalDependencyDeploymentException 
   * @throws XACT_LibOfTriggerImplNotFoundException 
   * @throws XFMG_SHARED_LIB_NOT_FOUND 
   * @throws XACT_InvalidStartParameterException 
   * @throws XACT_IncompatibleTriggerImplException 
   * @throws XACT_TriggerImplClassNotFoundException 
   */
  private EventListener deployTriggerInternally(String nameOfTrigger, String nameOfTriggerInstance, String[] startParameter,
                                                String description, Long revisionOfTriggerInstance, boolean deployedDisabled, boolean startThread,
                                                int processingLimit, boolean disableFailedInstances)
      throws XACT_TriggerInstanceNeedsEnabledTriggerException, XACT_TriggerNotFound, PersistenceLayerException,
      XACT_TriggerImplClassNotFoundException, XACT_IncompatibleTriggerImplException, XACT_InvalidStartParameterException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, XACT_AdditionalDependencyDeploymentException,
       XACT_TriggerCouldNotBeStartedException {

    if (nameOfTriggerInstance.contains("#")) {
      // Zeichen wird für Primary-Key in Tabelle verwendet ... verboten!
      throw new RuntimeException("Illegal character '#' in name " + nameOfTriggerInstance);
    }
    
    if (logger.isDebugEnabled()) {
      logger.debug("adding triggerinstance " + nameOfTriggerInstance + " of trigger " + nameOfTrigger + " ...");
    }
    
    if (getTriggerState(nameOfTrigger, revisionOfTriggerInstance, true) == TriggerState.ERROR && !deployedDisabled) {
      throw new XACT_TriggerInstanceNeedsEnabledTriggerException(nameOfTrigger);
    }
    
    managementLock.lock();
    try {
      Trigger trigger = getTrigger(revisionOfTriggerInstance, nameOfTrigger, true);
      
      EventListenerInstance oldeli =
                      getEventListenerInstanceByName(nameOfTriggerInstance, revisionOfTriggerInstance, false); //undeployment der Filter muss auch für nicht laufende Trigger ausgeführt werden
      
      if (oldeli != null) {
        if (deployedDisabled) {
          try {
            disableTriggerInstance(nameOfTriggerInstance, revisionOfTriggerInstance, false);
            return null;
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            throw new RuntimeException(e);
          } catch (XACT_TriggerNotFound e) {
            throw new RuntimeException(e);
          } catch (XACT_TriggerInstanceNotFound e) {
            throw new RuntimeException(e);
          }
        }
        // redeploy
        logger.debug("Found existing EventListenerInstance " + oldeli + ", redeploying");
        return redeployEventListener(trigger, oldeli, startParameter, description, true);
      }
      
      return deployTriggerInternally(trigger, nameOfTriggerInstance, startParameter, description, deployedDisabled, startThread, processingLimit, disableFailedInstances, revisionOfTriggerInstance);
    } finally {
      managementLock.unlock();
    }
  }

  private EventListener deployTriggerInternally(Trigger trigger, String triggerInstanceName, String[] startParameter, String description,
                                                boolean deployedDisabled, boolean startThread, int processingLimit, boolean disableFailedInstances, long revisionOfTriggerInstance) throws PersistenceLayerException,
      XACT_TriggerImplClassNotFoundException, XACT_IncompatibleTriggerImplException, XACT_InvalidStartParameterException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, XACT_AdditionalDependencyDeploymentException,
      XACT_TriggerCouldNotBeStartedException, XACT_TriggerInstanceNeedsEnabledTriggerException {
    EventListener el = null;

    TriggerInstanceStorable triggerInstanceStorable =
                    new TriggerInstanceStorable(triggerInstanceName, revisionOfTriggerInstance, trigger.getTriggerName(), startParameter, description,
                                                !deployedDisabled);
    
    EventListenerInstance eli;
    if (!deployedDisabled) {
      try {
        eli = trigger.instantiateEventListenerInstance(triggerInstanceName, startParameter, description, revisionOfTriggerInstance);

        // deploy additional dependencies
        deployAdditionalDependencies(trigger, eli.getInstanceName());

        // deployment
        el = deployEventListener(eli);
      } catch (Throwable t) {
        Department.handleThrowable(t);

        if (disableFailedInstances) {
          logger.warn("could not redeploy trigger instance" + triggerInstanceName + ".", t);
          try {
            deployTriggerDisabled(trigger.getTriggerName(), triggerInstanceName, startParameter,
                                  description, revisionOfTriggerInstance);
            return null;
          } catch (XACT_TriggerNotFound e) {
            throw new RuntimeException(e); //Trigger sollte noch vorhanden sein
          }
        } else {
          triggerInstanceStorable.setError(t);
          try {
            //erst checken, ob triggerinstance bereits gespeichert ist. soll nicht persistiert werden, falls nicht.
            storage.getTriggerInstanceByName(triggerInstanceName, revisionOfTriggerInstance, false);
            storage.persistObject(triggerInstanceStorable);
          } catch (XACT_TriggerInstanceNotFound e) {
            //ok, wirf fehler
          }
          
          if (t instanceof XACT_TriggerCouldNotBeStartedException) {
            throw (XACT_TriggerCouldNotBeStartedException) t;
          } else if (t instanceof PersistenceLayerException) {
            throw (PersistenceLayerException) t;
          } else if (t instanceof XACT_IncompatibleTriggerImplException) {
            throw (XACT_IncompatibleTriggerImplException) t;
          } else if (t instanceof XACT_TriggerImplClassNotFoundException) {
            throw (XACT_TriggerImplClassNotFoundException) t;
          } else if (t instanceof XACT_InvalidStartParameterException) {
            throw (XACT_InvalidStartParameterException) t;
          } else if (t instanceof XACT_LibOfTriggerImplNotFoundException) {
            throw (XACT_LibOfTriggerImplNotFoundException) t;
          } else if (t instanceof XACT_AdditionalDependencyDeploymentException) {
            throw (XACT_AdditionalDependencyDeploymentException) t;
          } else if (t instanceof XACT_TriggerInstanceNeedsEnabledTriggerException) {
            throw (XACT_TriggerInstanceNeedsEnabledTriggerException) t;
          } else if (t instanceof XFMG_SHARED_LIB_NOT_FOUND) {
            throw (XFMG_SHARED_LIB_NOT_FOUND) t;
          } else {
            throw new RuntimeException(t);
          }
        }
      }
    } else {
      eli = null;
    }
    
    // speichern
    storage.persistObject(triggerInstanceStorable);
    
    if (!deployedDisabled) {
      //evtl. bereits vorhandene filterinstanzen wieder herstellen
      redeployFilterInstancesForTriggerInstance(eli, disableFailedInstances);
    
      //Thread starten
      if (startThread) {
        startThreadForTriggerInstance(eli, processingLimit, false);
      }
    }
    
    //Application auf RUNNING setzen
    if (triggerInstanceStorable.getStateAsEnum() == TriggerInstanceState.ENABLED) {
      RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      if (triggerInstanceStorable.getRevision() != null 
            && !revisionManagement.isWorkspaceRevision(triggerInstanceStorable.getRevision())) {
        ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                        .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
        applicationManagement.changeApplicationState(triggerInstanceStorable.getRevision(), ApplicationState.RUNNING);
      }
    }
    
    return el;
  }
  
  private EventListener deployEventListener(EventListenerInstance eli) throws PersistenceLayerException {
    String nameOfTriggerInstance = eli.getInstanceName();
    Long revision = eli.getRevision();
    
    try {
      TriggerConfigurationStorable tcs = storage.getTriggerConfigurationByTriggerInstanceName(nameOfTriggerInstance, revision);
      configureTriggerMaxEvents(eli.getEL(), tcs.getMaxReceives(), tcs.getAutoReject());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      if (logger.isTraceEnabled()) {
        logger.trace("triggerinstance " + nameOfTriggerInstance + " has no separate configuration.");
      }
      //nicht jeder trigger muss konfiguriert sein...
    }

    Map<String, EventListenerInstance<?, ?>> relevantMap = eventListenerInstancesByName.get(revision);
    if (relevantMap == null) {
      relevantMap = new HashMap<String, EventListenerInstance<?,?>>();
      eventListenerInstancesByName.put(revision, relevantMap);
    }
    relevantMap.put(nameOfTriggerInstance, eli);
    if (logger.isDebugEnabled()) {
      logger.debug("Added EventListenerInstance " + eli + " to cache for revision " + revision);
    }
    return eli.getEL();
  }


  private void deployAdditionalDependencies(Trigger trigger, String nameOfTriggerInstance) throws XACT_AdditionalDependencyDeploymentException {
    // deploy additional dependencies
    try {
      if (trigger.getAdditionalDependencies() != null) {
        Long revision = trigger.getRevision();
        logger.debug("deploying additional dependencies to exceptions");
        if (trigger.getAdditionalDependencies().getAdditionalDependencies(AdditionalDependencyType.EXCEPTION).size() > 0) {          
          for (String ex : trigger.getAdditionalDependencies().getAdditionalDependencies(AdditionalDependencyType.EXCEPTION)) {
            // BUG 11121: 'AdditionalDependencies' need only to be deployed if there isn't already a classloader for them
            try {
              XynaFactory.getInstance()
                              .getFactoryManagement()
                              .getXynaFactoryControl()
                              .getClassLoaderDispatcher()
                              .getExceptionClassLoader(GenerationBase.transformNameForJava(ex),
                                                       revision, true);
            } catch (XFMG_ExceptionClassLoaderNotFoundException e) {
              ExceptionGeneration exception = ExceptionGeneration.getInstance(ex, revision);
              exception.setDeploymentComment("Additional dependencies of trigger " + trigger.getTriggerName());
              RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
              if(revisionManagement.isWorkspaceRevision(revision)) {
                exception.deploy(DeploymentMode.codeChanged, WorkflowProtectionMode.FORCE_DEPLOYMENT);
              } else {
                exception.deploy(DeploymentMode.reload, WorkflowProtectionMode.FORCE_DEPLOYMENT);
              }
            }
          }
        }
      }
    } catch (XynaException e) {
      throw new XACT_AdditionalDependencyDeploymentException(nameOfTriggerInstance, e);
    }
  }

  private void deployAdditionalDependencies(AdditionalDependencyContainer additionalDependencies, String nameOfObjectWhichHasAdditionalDependencies,
                                            String classLoaderIdToBeReloaded, Long revision, boolean isStarting) throws XACT_AdditionalDependencyDeploymentException {
    if (additionalDependencies != null) {
      // deploy additional dependencies
      try {
        RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        Set<String> datatypeDependencies =
            additionalDependencies.getAdditionalDependencies(AdditionalDependencyType.DATATYPE);
        if (datatypeDependencies.size() > 0) {
          if (logger.isDebugEnabled()) {
            logger.debug("Deploying additional dependencies to datatypes for filter " + nameOfObjectWhichHasAdditionalDependencies + " in rev " + revision);
          }
          for (String datatype : datatypeDependencies) {
            // BUG 11121: 'AdditionalDependencies' need only to be deployed if there isn't already a classloader for them
            ClassLoaderBase clb;
            try {
              clb =
                  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                      .getMDMClassLoader(GenerationBase.transformNameForJava(datatype), revision, true);
            } catch (XFMG_MDMObjectClassLoaderNotFoundException e) {
              DOM dom = DOM.getInstance(datatype, revision);
              dom.setDeploymentComment("Deploy additional dependencies of Filter " + nameOfObjectWhichHasAdditionalDependencies);
              if (isStarting || !revisionManagement.isWorkspaceRevision(revision)) {
                dom.deploy(DeploymentMode.reload, WorkflowProtectionMode.FORCE_DEPLOYMENT);
              } else {
                dom.deploy(DeploymentMode.codeUnchanged, WorkflowProtectionMode.FORCE_DEPLOYMENT);
              }
              clb =
                  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                      .getMDMClassLoader(GenerationBase.transformNameForJava(datatype), revision, true);
            }
            clb.addDependencyToReloadIfThisClassLoaderIsRecreated(classLoaderIdToBeReloaded, revision, ClassLoaderType.Filter, ClassLoadingDependencySource.TriggerFilterAdditionalDependencies);
          }
        }

        Set<String> exceptionDependencies =
                        additionalDependencies.getAdditionalDependencies(AdditionalDependencyType.EXCEPTION);
        if (exceptionDependencies.size() > 0) {
          logger.debug("deploying additional dependencies to exceptions");
          for (String ex : exceptionDependencies) {
            // BUG 11121: 'AdditionalDependencies' need only to be deployed if there isn't already a classloader for them
            ClassLoaderBase clb;
            try {
              clb =
                  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                      .getExceptionClassLoader(GenerationBase.transformNameForJava(ex), revision, true);
            } catch (XFMG_ExceptionClassLoaderNotFoundException e) {
              ExceptionGeneration exception = ExceptionGeneration.getInstance(ex, revision);
              exception.setDeploymentComment("Deploy additional dependencies of Filter " + nameOfObjectWhichHasAdditionalDependencies);
              if (isStarting || !revisionManagement.isWorkspaceRevision(revision)) {
                exception.deploy(DeploymentMode.reload, WorkflowProtectionMode.FORCE_DEPLOYMENT);
              } else {
                exception.deploy(DeploymentMode.codeUnchanged, WorkflowProtectionMode.FORCE_DEPLOYMENT);
              }
              clb =
                  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                      .getExceptionClassLoader(GenerationBase.transformNameForJava(ex), revision, true);
            }
            clb.addDependencyToReloadIfThisClassLoaderIsRecreated(classLoaderIdToBeReloaded, revision, ClassLoaderType.Filter, ClassLoadingDependencySource.TriggerFilterAdditionalDependencies);
          }
        }

        Set<String> workflowDependencies =
                        additionalDependencies.getAdditionalDependencies(AdditionalDependencyType.WORKFLOW);
        if (workflowDependencies.size() > 0) {
          logger.debug("deploying additional dependencies to workflows");
          for (String wf : workflowDependencies) {
            // BUG 11121: 'AdditionalDependencies' need only to be deployed if there isn't already a classloader for them
            ClassLoaderBase clb;
            try {
              clb =
                  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                      .getWFClassLoader(GenerationBase.transformNameForJava(wf), revision, true);
            } catch (XFMG_WFClassLoaderNotFoundException e) {
              WF workflow = WF.getInstance(wf, revision);
              workflow.setDeploymentComment("Deploy additional dependencies of Filter " + nameOfObjectWhichHasAdditionalDependencies);
              if (isStarting || !revisionManagement.isWorkspaceRevision(revision)) {
                workflow.deploy(DeploymentMode.reload, WorkflowProtectionMode.FORCE_DEPLOYMENT);
              } else {
                workflow.deploy(DeploymentMode.codeUnchanged, WorkflowProtectionMode.FORCE_DEPLOYMENT);
              }
              clb =
                  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                      .getWFClassLoader(GenerationBase.transformNameForJava(wf), revision, true);
            }
            clb.addDependencyToReloadIfThisClassLoaderIsRecreated(classLoaderIdToBeReloaded, revision, ClassLoaderType.Filter, ClassLoadingDependencySource.TriggerFilterAdditionalDependencies);
          }
        }

      } catch (XynaException e) {
        throw new XACT_AdditionalDependencyDeploymentException(nameOfObjectWhichHasAdditionalDependencies, e);
      }
    }
  }

  public void startThreadForTriggerInstance(String nameOfTriggerInstance, Long revision)
                  throws XACT_TriggerInstanceNotFound, XACT_TriggerCouldNotBeStartedException {
    startThreadForTriggerInstance(nameOfTriggerInstance, revision, -1);
  }
  
  public void startThreadForTriggerInstance(String nameOfTriggerInstance, Long revision, int processingLimit)
      throws XACT_TriggerInstanceNotFound, XACT_TriggerCouldNotBeStartedException {
    managementLock.lock();
    try {
      EventListenerInstance eli = getEventListenerInstanceByName(nameOfTriggerInstance, revision, false);
      if (eli == null) {
        throw new XACT_TriggerInstanceNotFound(nameOfTriggerInstance);
      }
      
      startThreadForTriggerInstance(eli, processingLimit, true);
    } finally {
      managementLock.unlock();
    }
  }

  private void startThreadForTriggerInstance(EventListenerInstance eli, int processingLimit, boolean throwExceptionIfNoEnabledFilterFound)
                  throws XACT_TriggerCouldNotBeStartedException {
    try {
      checkFiltersHaveNoError(eli, throwExceptionIfNoEnabledFilterFound);
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().registerEventListener(eli, processingLimit);
    } catch (Throwable t) {
      Department.handleThrowable(t);
      
      if (t instanceof XACT_TriggerHasFilterInstanceInErrorStateException) {
        //Thread konnte nicht gestartet werden, weil eine Filterinstanz fehlerhaft ist,
        //Triggerinstanz soll enabled bleiben
        throw (XACT_TriggerHasFilterInstanceInErrorStateException) t;
      }
      
      //Fehler persistieren
      storage.setTriggerInstanceError(eli.getInstanceName(), eli.getRevision(), t);
      
      if (t instanceof XACT_TriggerCouldNotBeStartedException) {
        throw (XACT_TriggerCouldNotBeStartedException) t;
      } else {
        throw new RuntimeException(t);
      }
    }
  }
  
  private void checkFiltersHaveNoError(EventListenerInstance eli, boolean throwExceptionIfNoEnabledFilterFound) throws XACT_TriggerHasFilterInstanceInErrorStateException, PersistenceLayerException {
    List<FilterInstanceStorable> fis = storage.getFilterInstancesByTriggerInstanceName(eli.getInstanceName(), eli.getRevision(), true);
    boolean enabledFilterFound = false; 
    for (FilterInstanceStorable fi : fis) {
      if (fi.isOptional()) {
        try {
          if (fi.getStateAsEnum() == FilterInstanceState.ENABLED && getFilterState(fi.getFilterName(), fi.getRevision(), true) != FilterState.ERROR) {
            enabledFilterFound = true;
          }
        } catch (XACT_FilterNotFound e) {
          //egal, ist optional
        }
        continue; //Filterinstanz wird für den Trigger nicht unbedingt benötigt
      }
      switch (fi.getStateAsEnum()) {
        case DISABLED:
          continue; //disabled Filterinstanzen nicht beachten
        case ERROR:
          throw new XACT_TriggerHasFilterInstanceInErrorStateException(fi.getFilterInstanceName());
        case ENABLED:
          //evtl. hat der Filter einen Fehler
          FilterState state;
          try {
            state = getFilterState(fi.getFilterName(), fi.getRevision(), true);
          } catch (XACT_FilterNotFound e) {
            throw new XACT_TriggerHasFilterInstanceInErrorStateException(fi.getFilterInstanceName(), e);
          }
          if (state == FilterState.ERROR) {
            throw new XACT_TriggerHasFilterInstanceInErrorStateException(fi.getFilterInstanceName());
          }
          enabledFilterFound = true;
          break;
        default:
          logger.error("Unexpected FilterInstanceState " + fi.getStateAsEnum());
      }
    }
    if (!enabledFilterFound && fis.size() > 0 && throwExceptionIfNoEnabledFilterFound) {
      throw new XACT_TriggerHasFilterInstanceInErrorStateException("No filter enabled.");
    }
  }


  private void disableTriggerInstanceInternally(String nameOfTriggerInstance, Long revisionOfTriggerInstance, boolean disableFilterInstances, boolean persist)
      throws PersistenceLayerException, XACT_TriggerInstanceNotFound,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    
    if (logger.isDebugEnabled()) {
      logger.debug("Disabling trigger instance " + nameOfTriggerInstance + " for revision <" + revisionOfTriggerInstance + ">");
    }

    EventListenerInstance<?, ?> eli = getEventListenerInstanceByName(nameOfTriggerInstance, revisionOfTriggerInstance, false);

    if (eli == null) {
      if (persist) {
        logger.debug("trigger instance has no running thread");
        storage.setTriggerInstanceState(nameOfTriggerInstance, revisionOfTriggerInstance, TriggerInstanceState.DISABLED);
        return;
      } else {
        throw new XACT_TriggerInstanceNotFound(nameOfTriggerInstance);
      }
    }
    
    //Thread stoppen
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution()
        .unregisterEventListener(nameOfTriggerInstance, revisionOfTriggerInstance);
    
    //für alle vorhandenen ConnectionFilterInstances onUndeployment aufrufen
    ConnectionFilterInstance[] filterInstances = eli.getEL().getAllFilters();
    if (filterInstances != null) {
      for (ConnectionFilterInstance filterInstance : filterInstances) {
        disableFilterInstance(filterInstance, false);
      }
    }
    
    if (disableFilterInstances && persist) {
      //Filterinstanzen sollen auch den Status DISABLED erhalten
      Collection<FilterInstanceStorable> fis = storage.getFilterInstancesByTriggerInstanceName(nameOfTriggerInstance, revisionOfTriggerInstance, true);
      for (FilterInstanceStorable fi : fis) {
        if (fi.getStateAsEnum() != FilterInstanceState.DISABLED) {
          storage.setFilterInstanceState(fi.getFilterInstanceName(), fi.getRevision(), FilterInstanceState.DISABLED);
        }
      }
    }
    
    //aus EventListenerMap entfernen
    removeEventListenerInstanceFromMap(nameOfTriggerInstance, revisionOfTriggerInstance);
    
    if (persist) {
      storage.setTriggerInstanceState(nameOfTriggerInstance, revisionOfTriggerInstance, TriggerInstanceState.DISABLED);
    }
  }

  
  private EventListenerInstance<?, ?> removeEventListenerInstanceFromMap(String nameOfTriggerInstance, Long revision) {
    EventListenerInstance<?, ?> removedInstanceFromMap = null;
    Map<String, EventListenerInstance<?, ?>> relevantMap = eventListenerInstancesByName.get(revision);
    if (relevantMap != null) {
      removedInstanceFromMap = relevantMap.remove(nameOfTriggerInstance);
      if (relevantMap.isEmpty()) {
        eventListenerInstancesByName.remove(revision);
      }
    } else {
      logger.warn("EventListenerInstance map does not exist for revision <" + revision + ">");
    }

    if (removedInstanceFromMap == null) {
      logger.warn("No EventListenerInstance found in cache for trigger instance <" + nameOfTriggerInstance + ">");
    }
    
    return removedInstanceFromMap;
  }

  /**
   * Filterinstanz wird als disabled gespeichert.
   * am eigenen trigger wird nichts gemacht. (TODO unschön!) 
   */
  public boolean disableFilterInstanceDontTouchTrigger(ConnectionFilterInstance cfi) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    managementLock.lock();
    try {
      EventListenerInstance eli = getEventListenerInstanceByName(cfi.getTriggerInstanceName(), cfi.getRevision(), true);
      
      if (eli != null) {
        callUndeploymentOfFilterInstance(cfi.getCF(), eli.getEL());
      }
      
      FilterInstanceStorable fis = storage.getFilterInstanceByName(cfi.getInstanceName(), cfi.getRevision(), false);
      if (fis.getStateAsEnum() == FilterInstanceState.DISABLED) {
        return false;
      } else {
        storage.setFilterInstanceState(cfi.getInstanceName(), cfi.getRevision(), FilterInstanceState.DISABLED);
        return true;
      }
    } finally {
      managementLock.unlock();
    }
  }
  

  /**
   * Filterinstanz wird als disabled gespeichert und von der Triggerinstanz entfernt.
   */
  public boolean disableFilterInstance(String filterInstanceName, Long revision) throws PersistenceLayerException, XACT_TriggerInstanceNotFound, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    managementLock.lock();
    try {
      FilterInstanceStorable fis = storage.getFilterInstanceByName(filterInstanceName, revision, true);
      if (fis.getStateAsEnum() == FilterInstanceState.DISABLED) {
        //bereits disabled
        return false;
      } else {
        //ConnectionFilterInstance disablen, falls vorhanden
        EventListenerInstance eli = getEventListenerInstanceByName(fis.getTriggerInstanceName(), fis.getRevision(), true);
        if (eli != null) {
          ConnectionFilterInstance[] foundFilters = eli.getEL().getAllFilters();
          for (ConnectionFilterInstance cfi : foundFilters) {
            if (cfi.getInstanceName().equals(filterInstanceName) && cfi.getRevision() == fis.getRevision()) {
              disableFilterInstance(cfi, false);
              break;
            }
          }
        }
        
        //State im Storable umsetzen
        storage.setFilterInstanceState(filterInstanceName, fis.getRevision(), FilterInstanceState.DISABLED);

        if (eli != null) {
          restartEventListener(eli.getInstanceName(), eli.getRevision());
        }
        return true;
      }
    } finally {
      managementLock.unlock();
    }
  }
  
  
  @SuppressWarnings("unchecked")
  public void disableFilterInstance(ConnectionFilterInstance cfi, boolean persist) throws XACT_TriggerInstanceNotFound, PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    managementLock.lock();
    try {
      EventListenerInstance<?, ?> eli = getEventListenerInstanceByName(cfi.getTriggerInstanceName(), cfi.getRevision(), true);
      
      if (eli == null) {
        throw new XACT_TriggerInstanceNotFound(cfi.getTriggerInstanceName());
      }
      
      //Undeployment der Filterinstanz
      callUndeploymentOfFilterInstance(cfi.getCF(), eli.getEL());
      
      // Undeployment der OutdatedFilter
      callUndeploymentOfOutdatedFilterInstances(cfi.getInstanceName(), cfi.getTriggerInstanceName(), cfi.getRevision());

      //Entfernen der Filterinstanz mit ihren OutdatedFilterinstanzen vom EventListener
      eli.getEL().removeFilter(cfi);
      
      if (persist) {
        storage.setFilterInstanceState(cfi.getInstanceName(), cfi.getRevision(), FilterInstanceState.DISABLED);
      }
    } finally {
      managementLock.unlock();
    }
  }
  
  
  public static void callUndeploymentOfFilterInstance(ConnectionFilter filterInstance, EventListener triggerInstance) {
    try {
      filterInstance.onUndeployment(triggerInstance);
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.warn("Exception during onundeployment of filter instance " + filterInstance, t);
    }
  }

  
  
  /**
   * Undeployment of Triggers and their Filters.
   */
  private EventListenerInstance undeployTriggerInternally(String nameOfTrigger, String nameOfTriggerInstance, Long revisionOfTriggerInstance)
                  throws XACT_TriggerNotFound, PersistenceLayerException, XACT_TriggerInstanceNotFound {

    TriggerInstanceStorable triggerInstanceStorable = null;
    managementLock.lock();
    try {
      //Thread stoppen
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution()
          .unregisterEventListener(nameOfTriggerInstance, revisionOfTriggerInstance);

      //undeploy all filters
      for (FilterInstanceStorable f : getFilterInstancesForTriggerInstance(nameOfTriggerInstance, revisionOfTriggerInstance, true)) {
        try {
          undeployFilterInternally(f.getFilterInstanceName(), f.getRevision(), false);
        } catch (XACT_FilterNotFound e) {
          // Filter nicht mehr gefunden -> mit den anderen weiter machen
        }
      }

      //aus eventListenerMap entfernen
      EventListenerInstance eli = removeEventListenerInstanceFromMap(nameOfTriggerInstance, revisionOfTriggerInstance);

      //TiggerInstanceStorable und TriggerConfigurationStorable löschen
      triggerInstanceStorable = storage.getTriggerInstanceByName(nameOfTriggerInstance, revisionOfTriggerInstance, false);
      storage.deleteTriggerInstance(nameOfTriggerInstance, revisionOfTriggerInstance);
      return eli;
    } finally {
      managementLock.unlock();
      if (triggerInstanceStorable != null) {
        executeUndeploymentHandlers(triggerInstanceStorable);
      }
    }
  }


  private void executeUndeploymentHandlers(TriggerInstanceStorable triggerInstanceStorable) {
    Integer[] priorities = DeploymentHandling.allPriorities;
    for (int i = priorities.length - 1; i >= 0; i--) {
      try {
        XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
            .executeUndeploymentHandler(priorities[i], triggerInstanceStorable);
      } catch (XPRC_UnDeploymentHandlerException e) {
        logger.warn("Call of undeployment handler failed.", e);
      }
    }
  }
  
  
  private void addFilter(FilterStorable filterStorable) throws XPRC_ExclusiveDeploymentInProgress, XACT_FilterImplClassNotFoundException,
      XACT_IncompatibleFilterImplException, XACT_TriggerNotFound, PersistenceLayerException, Ex_FileAccessException,
      XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException, XACT_AdditionalDependencyDeploymentException,
      XACT_JarFileUnzipProblem, XACT_LibOfFilterImplNotFoundException, XACT_OldFilterVersionInstantiationException,
      XFMG_SHARED_LIB_NOT_FOUND {

    Trigger trigger = getTrigger(filterStorable.getRevision(), filterStorable.getTriggerName(), true);
    addNewFilterInternally(filterStorable.getFilterName(), filterStorable.getFqFilterClassName(), filterStorable.getJarFilesAsArray(),
                           filterStorable.getSharedLibsArray(), filterStorable.getDescription(),
                           trigger, filterStorable.getRevision(), true, false, new EmptyRepositoryEvent());
  }


  public void addFilter(String filterName, File[] jarFiles, String fqFilterClassName, String triggerName, String[] sharedLibs,
                        String description, Long revision, RepositoryEvent repositoryEvent) throws XPRC_ExclusiveDeploymentInProgress,
      XACT_FilterImplClassNotFoundException, XACT_TriggerNotFound, PersistenceLayerException,
      Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException,
      XACT_AdditionalDependencyDeploymentException, XACT_JarFileUnzipProblem, XACT_LibOfFilterImplNotFoundException,
      XFMG_SHARED_LIB_NOT_FOUND {

    addFilterInternally(filterName, revision, jarFiles, fqFilterClassName, triggerName, sharedLibs, description, false, false, false, repositoryEvent);
  }


  public void addFilter(String filterName, File[] jarFiles, String fqFilterClassName, String triggerName, String[] sharedLibs,
                        String description, Long revision, boolean addEmpty, boolean disableFailedInstances, RepositoryEvent repositoryEvent) throws XPRC_ExclusiveDeploymentInProgress,
      XACT_FilterImplClassNotFoundException, XACT_TriggerNotFound, PersistenceLayerException,
      Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException,
      XACT_AdditionalDependencyDeploymentException, XACT_JarFileUnzipProblem, XACT_LibOfFilterImplNotFoundException,
      XFMG_SHARED_LIB_NOT_FOUND {

    addFilterInternally(filterName, revision, jarFiles, fqFilterClassName, triggerName, sharedLibs, description, false, addEmpty, disableFailedInstances, repositoryEvent);
  }


  public void addFilter(String filterName, File[] jarFiles, String fqFilterClassName, String triggerName, String[] sharedLibs,
                        String description, long revision) throws XPRC_ExclusiveDeploymentInProgress, XACT_FilterImplClassNotFoundException,
      XACT_TriggerNotFound, PersistenceLayerException, Ex_FileAccessException,
      XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException, XACT_AdditionalDependencyDeploymentException,
      XACT_JarFileUnzipProblem, XACT_LibOfFilterImplNotFoundException, XFMG_SHARED_LIB_NOT_FOUND {

    addFilterInternally(filterName, revision, jarFiles, fqFilterClassName, triggerName, sharedLibs,
                        description, false, false, false, new SingleRepositoryEvent(revision));
  }


  /**
   * Merkt sich dass dieser Filter verfuegbar ist. Durch das Update der JarFiles werden automatisch alle vorhanden
   * Filterinstanzen mit dem neuen Code ausgefuehrt.
   */
  private void addFilterInternally(String filterName, final Long revision, File[] jarFiles, final String fqFilterClassName,
                                   String triggerName, String[] sharedLibs, String description, boolean isStarting, boolean addEmpty,
                                   boolean disableFailedInstances, RepositoryEvent repositoryEvent) throws XACT_TriggerNotFound, XPRC_ExclusiveDeploymentInProgress,
      PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException,
      XACT_AdditionalDependencyDeploymentException, XACT_FilterImplClassNotFoundException,
      XACT_JarFileUnzipProblem, XACT_LibOfFilterImplNotFoundException,
      XFMG_SHARED_LIB_NOT_FOUND {

    if (filterName.contains("#")) {
      // Zeichen wird für Primary-Key in Tabelle verwendetet ... verboten!
      throw new RuntimeException("Illegal character '#' in name " + filterName);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("adding filter " + filterName + " of filterclass " + fqFilterClassName + " for trigger " + triggerName + ".");
    }

    managementLock.lock();
    try {
      Trigger trigger = getTrigger(revision, triggerName, true);

      if (addEmpty) {
        jarFiles = new File[0];
      }

      FilterStorable oldFilter = null;
      try {
        oldFilter = storage.getFilterByName(filterName, revision, false);
      } catch (XACT_FilterNotFound e1) {
        //neuen Filter anlegen
      }
      
      if (oldFilter != null && oldFilter.getStateAsEnum() != FilterState.EMPTY) {
        addExistingFilter(filterName, revision, jarFiles, fqFilterClassName, triggerName, sharedLibs, description, isStarting,
                          disableFailedInstances, repositoryEvent);
      } else {
        addNewFilterInternally(filterName, fqFilterClassName, jarFiles, sharedLibs, description,
                               trigger, revision, isStarting, addEmpty, repositoryEvent);
      }
    } finally {
      managementLock.unlock();
    }
  }


  /**
   * Copies the whole saved folder and returns the new locations of the provided jar files
   */
  private List<File> copySavedToDeployed(String savedPathFolder, String deployedPath, File[] jarFiles) throws Ex_FileAccessException {
    File savedPathFile = new File(savedPathFolder);
    List<File> allFilesExceptForJars = new ArrayList<File>();
    addSubdirectoryFilesExceptJars(allFilesExceptForJars, savedPathFile, jarFiles);
    copyFilesToTargetFolder(deployedPath, allFilesExceptForJars.toArray(new File[allFilesExceptForJars.size()]));
    return copyFilesToTargetFolder(deployedPath, jarFiles);
  }


  private void addSubdirectoryFilesExceptJars(List<File> allFiles, File directory, File[] jarFiles) throws Ex_FileAccessException {
    String[] allFileNames = directory.list();
    if (allFileNames == null) {
      throw new Ex_FileAccessException(directory.getAbsolutePath());
    } else if (allFileNames.length == 0) {
      return;
    }
    outer: for (String oneFileName : allFileNames) {
      File oneFile = new File(directory.getAbsolutePath() + Constants.FILE_SEPARATOR + oneFileName);
      String oneFileAbsolutePath;
      try {
        oneFileAbsolutePath = oneFile.getCanonicalPath();
      } catch (IOException e) {
        oneFileAbsolutePath = oneFile.getAbsolutePath();
      }
      for (File jarFile: jarFiles) {
        if (oneFileAbsolutePath.equals(jarFile.getAbsolutePath())) {
          continue outer; // this will be copied in the subsequent step that takes care only of the jar files
        }
      }
      allFiles.add(oneFile);
    }
  }


  private void addNewFilterInternally(String filterName, String fqFilterClassName, File[] jarFiles, String[] sharedLibs,
                                      String description, Trigger trigger, Long revisionOfFilter, boolean isStarting,
                                      boolean addEmpty,  RepositoryEvent repositoryEvent) throws PersistenceLayerException,
        XPRC_ExclusiveDeploymentInProgress, Ex_FileAccessException, XACT_FilterImplClassNotFoundException,
        XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException, XPRC_XmlParsingException,
        XPRC_InvalidXmlMissingRequiredElementException, XACT_AdditionalDependencyDeploymentException {
    
    DeploymentLocks.writeLock(fqFilterClassName, DependencySourceType.FILTER, "AddFilter", revisionOfFilter);
    try {
      if (!isStarting && !addEmpty) {
        String deploymentPath = getFilterDeploymentFolderByFilterFqClassName(fqFilterClassName, revisionOfFilter);
        String savedPath = guessSourceFolderByJarFileLocation(fqFilterClassName, jarFiles);
        jarFiles = copySavedToDeployed(savedPath, deploymentPath, jarFiles).toArray(new File[1]);
      }

      Filter fi = new Filter(filterName, revisionOfFilter, jarFiles, fqFilterClassName, trigger, trigger.getTriggerName(), sharedLibs, description);

      if (addEmpty) {
        //nur FilterStorable mit state EMPTY anlegen
        FilterStorable toBeStored =
                        new FilterStorable(filterName, revisionOfFilter, new String[0], fqFilterClassName, trigger.getTriggerName(), sharedLibs,
                                           description, FilterState.EMPTY);
        storage.persistObject(toBeStored);
        return;
      }

      // alten classloader entfernen, damit ein neuer erstellt wird, sobald das naechste mal ein filter instantiiert wird.
      // siehe auch klasse eventlistener
      fi.removeClassLoader();
      boolean successAfterNewClassLoader = false;
      
      try {
        try {
          fi.validate(null);
        } catch (XACT_DuplicateFilterDefinitionException e) {
          throw new RuntimeException(e);
        } catch (XACT_WrongTriggerException e) {
          //FIXME
          throw new RuntimeException(e);
        }

        List<String> jarStrings = new ArrayList<String>();
        for (File jar : jarFiles) {
          jarStrings.add(repairPath(jar.getPath(), "filter/"));
        }

        //additional dependencies
        AdditionalDependencyContainer additionalDependencies = fi.getAdditionalDependencies();
        if (additionalDependencies != null) {
          /*
           * TODO notwendig, weil es sonst zu deadlocks kommt, weil das deployment intern das managementlock holt. vgl. ClassloaderDispatcher
           * so ist das aber nur ein workaround, weil jetzt theoretisch ein weiteres addfilter loslaufen könnte, bevor man hier persistiert hat
           * das ist natürlich nicht gut. das passiert aber in der praxis so selten, dass das so erst mal viel besser als ein deadlock ist
           */
          boolean heldByThread = managementLock.isHeldByCurrentThread();
          if (heldByThread) {
            managementLock.unlock();
          }
          try {
          deployAdditionalDependencies(additionalDependencies, fi.getName(), fi.getFQFilterClassName(), revisionOfFilter, isStarting);
          } finally {
            if (heldByThread) {
              managementLock.lock();
            }
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("No additional dependency information found in file " 
                         + XynaActivationTrigger.getFilterXmlLocationByFqFilterClassName(fqFilterClassName, revisionOfFilter) + " (file does not exist)");
          }
        }

        //Storable speichern
        FilterStorable toBeStored =
                        new FilterStorable(filterName, revisionOfFilter, jarStrings.toArray(new String[1]), fqFilterClassName, trigger.getTriggerName(), sharedLibs,
                                           description, FilterState.OK);
        storage.persistObject(toBeStored);
        
        //FilterChangeListener benachrichtigen
        filterAdded(fi);
        successAfterNewClassLoader = true;
      } finally {
        if (!successAfterNewClassLoader) {
          fi.removeClassLoader();
        }
      }
    } finally {
      DeploymentLocks.writeUnlock(fqFilterClassName, DependencySourceType.FILTER, revisionOfFilter);
    }
    
    repositoryEvent.addEvent(new ProjectCreationOrChangeProvider.FilterChangeEvent(EventType.FILTER_ADD, filterName, getSimpleClassName(fqFilterClassName)));
  }
  
  
  private class AddFilterCommand extends CommandWithFolderBackup {
    private String filterName;
    private String fqFilterClassName;
    private Trigger trigger;
    private File[] jarFiles;
    private String[] sharedLibs;
    private String description;
    private Long revision;
    private boolean isStarting;
    private boolean disableFailedInstances;
    private RepositoryEvent repositoryEvent;
    private Filter oldFilter;
    private List<File> copiedLibs;

    private AddFilterCommand(String filterName, String fqFilterClassName, Trigger trigger,
                             File[] jarFiles, String[] sharedLibs, String description, Long revision,
                             boolean isStarting, boolean disableFailedInstances, RepositoryEvent repositoryEvent) {
      super();
      this.filterName = filterName;
      this.fqFilterClassName = fqFilterClassName;
      this.trigger = trigger;
      this.jarFiles = jarFiles;
      this.sharedLibs = sharedLibs;
      this.description = description;
      this.revision = revision;
      this.isStarting = isStarting;
      this.disableFailedInstances = disableFailedInstances;
      this.repositoryEvent = repositoryEvent;
    }

    @Override
    protected File backup() throws Ex_FileAccessException, XACT_JarFileUnzipProblem, InternalException {
      try {
        oldFilter = getFilter(revision, filterName, false);
      } catch (XACT_FilterNotFound e) {
        throw new RuntimeException(e); //Filter war schon als existierend bekannt -> sollte nicht vorkommen
      } catch (PersistenceLayerException e) {
        throw new InternalException(e);
      }
      
      return super.backup();
    }
    
    @Override
    protected void rollbackFailureTreatment(Throwable t) throws InternalException {
      //Filter-Status auf ERROR setzen
      storage.setFilterError(filterName, revision, new XACT_ErrorDuringTriggerAdditionRollback(filterName, t));
      
      //Trigger Threads dürfen für optionale Instanzen trotz Filter-Fehler wieder starten
      try {
        for (FilterInstanceStorable fi : storage.getFilterInstancesByFilterName(filterName, revision, true)) {
          restartEventListener(fi.getTriggerInstanceName(), revision);
        }
      } catch (PersistenceLayerException e) {
        logger.warn("Could not restart trigger threads", e);
      }
      
      throw new InternalException(new XACT_ErrorDuringFilterAdditionRollback(filterName, t));
    }
    
    @Override
    protected void rollbackAndExecute() throws XACT_JarFileUnzipProblem, Ex_FileAccessException, InternalException {
      //rollback passiert in der mitte des executes
      try {
        executeInternally();
      } catch (InternalException e) {
        if (e.getCause() instanceof XACT_JarFileUnzipProblem) {
          throw (XACT_JarFileUnzipProblem) e.getCause();
        } else if (e.getCause() instanceof Ex_FileAccessException) {
          throw (Ex_FileAccessException) e.getCause();
        }
        throw e;
      }
    }

    @Override
    protected void executeInternally() throws InternalException {
      //alte filterinstanzen undeployen, cache leeren und Trigger Thread stoppen
      Trigger trigger = oldFilter.getTrigger();
      EventListenerInstance[] triggerInstances;
      try {
        triggerInstances = getTriggerInstances(trigger.getTriggerName(), trigger.getRevision());
      } catch (XACT_TriggerNotFound e1) {
        throw new RuntimeException(e1);
      }
      for (EventListenerInstance eli : triggerInstances) {
        eli.getEL().resetFilterCache();
        
        ConnectionFilterInstance[] foundFilters = eli.getEL().getAllFilters();
        for (ConnectionFilterInstance cfi : foundFilters) {
          if (cfi.getFilterName().equals(oldFilter.getName())) {
            try {
              disableFilterInstance(cfi, false);
              XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().unregisterEventListener(eli.getInstanceName(), eli.getRevision());
              break;
            } catch (XACT_TriggerInstanceNotFound e) {
              throw new RuntimeException(e); //Triggerinstanz war schon da
            } catch (PersistenceLayerException e) {
              throw new RuntimeException(e); //tritt wegen persist=false nicht auf
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              throw new RuntimeException(e); //Filterinstanz war schon da
            }
          }
        }
      }
      
      if (getCurrentPhase() == ExecutionPhase.EXECUTION) {
        //FIXME sicherstellen, dass nicht noch aktive filter instanzen unterwegs sind, die evtl klassen mit dem alten classloader
        //      nachladen wollen. das gleiche gilt für trigger-instanzen bei der entsprechenden stelle wo trigger-jars ausgetauscht werden
        try {
          String deploymentPath = getFilterDeploymentFolderByFilterFqClassName(fqFilterClassName, revision);
          String savedPath = guessSourceFolderByJarFileLocation(fqFilterClassName, jarFiles);
          copiedLibs = copySavedToDeployed(savedPath, deploymentPath, jarFiles);
        } catch (Ex_FileAccessException e) {
          throw new InternalException(e);
        }
      } else if (getCurrentPhase() == ExecutionPhase.ROLLBACK) {
        try {
          rollback();
        } catch (XACT_JarFileUnzipProblem e) {
          throw new InternalException(e);
        } catch (Ex_FileAccessException e) {
          throw new InternalException(e);
        }
      } else {
        throw new RuntimeException("unexpected state of " + CommandWithFolderBackup.class.getSimpleName() + ": " + getCurrentPhase());
      }

      try {
        if (getCurrentPhase() != ExecutionPhase.ROLLBACK) {
          addNewFilterInternally(filterName, fqFilterClassName, copiedLibs.toArray(new File[copiedLibs.size()]), sharedLibs, description,
                                 trigger, revision, isStarting, false, repositoryEvent);
        } else {
          addNewFilterInternally(filterName, oldFilter.getFQFilterClassName(), oldFilter.getJarFiles(), oldFilter.getSharedLibs(),
                                 oldFilter.getDescription(), oldFilter.getTrigger(), revision, isStarting, false, repositoryEvent);
        }
      } catch (XPRC_ExclusiveDeploymentInProgress e) {
        throw new InternalException(e);
      } catch (PersistenceLayerException e) {
        throw new InternalException(e);
      } catch (Ex_FileAccessException e) {
        throw new InternalException(e);
      } catch (XPRC_XmlParsingException e) {
        throw new InternalException(e);
      } catch (XPRC_InvalidXmlMissingRequiredElementException e) {
        throw new InternalException(e);
      } catch (XACT_AdditionalDependencyDeploymentException e) {
        throw new InternalException(e);
      } catch (XACT_FilterImplClassNotFoundException e) {
        throw new RuntimeException(e); //kommt nur wenn addExistingFilter aufgerufen wird
      } catch (XACT_LibOfFilterImplNotFoundException e) {
        throw new RuntimeException(e); //kommt nur wenn addExistingFilter aufgerufen wird
      } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
        throw new RuntimeException(e); //kommt nur wenn addExistingFilter aufgerufen wird
      }
      
      //für alle enabled filterinstanzen die diesen filter bereits benutzten: ondeployment aufrufen.
      List<FilterInstanceStorable> filterInstances;
      try {
        filterInstances = storage.getFilterInstancesByFilterName(filterName, revision, true);
      } catch (PersistenceLayerException e) {
        throw new InternalException(e);
      }
      
      for (FilterInstanceStorable fi : filterInstances) {
        if (fi.getStateAsEnum() == FilterInstanceState.DISABLED) {
          continue;
        }
        
        Exception ex = null;
        try {
          EventListenerInstance eli = getEventListenerInstanceByName(fi.getTriggerInstanceName(), fi.getRevision(), true);
          if (eli != null) {
            instantiateConnectionFilter(fi, eli, true);
            
            if (fi.getStateAsEnum() == FilterInstanceState.ERROR) {
              //FilterInstance ist nun im Zustand ENABLED
              storage.setFilterInstanceState(fi.getFilterInstanceName(), fi.getRevision(), FilterInstanceState.ENABLED);
            }
          }
        } catch (XACT_FilterImplClassNotFoundException e) {
          ex = e;
        } catch (XACT_IncompatibleFilterImplException e) {
          ex = e;
        } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
          ex = e;
        } catch (XACT_LibOfFilterImplNotFoundException e) {
          ex = e;
        } catch (Exception e) {
          logger.warn("could not redeploy filter instance " + fi.getFilterInstanceName(), e);
        }

        if (ex != null) {
          if (disableFailedInstances) {
            logger.warn("could not redeploy filter instance " + fi.getFilterInstanceName() + "filter instance will be disabled.", ex);
            try {
              deployFilterInternallyDisabled(fi);
            } catch (XACT_FilterNotFound e) {
              throw new RuntimeException(e);
            } catch (PersistenceLayerException e) {
              logger.warn("could not disable filter instance " + fi.getFilterInstanceName(), ex);
            }
          }
        }
      }

      //Trigger Threads wieder starten
      for (FilterInstanceStorable fi : filterInstances) {
        restartEventListener(fi.getTriggerInstanceName(), revision);
      }
    }
  }
  
  
  public void addExistingFilter(String filterName, Long revision, File[] jarFiles, String fqFilterClassName,
                                String triggerName, String[] sharedLibs, String description, boolean isStarting,
                                boolean disableFailedInstances, RepositoryEvent repositoryEvent) throws Ex_FileAccessException, XACT_JarFileUnzipProblem,
      PersistenceLayerException, XPRC_ExclusiveDeploymentInProgress, XACT_TriggerNotFound, XPRC_XmlParsingException,
      XACT_AdditionalDependencyDeploymentException, XPRC_InvalidXmlMissingRequiredElementException {

    String deploymentDirPath = getFilterDeploymentFolderByFilterFqClassName(fqFilterClassName, revision);
    
    managementLock.lock();
    try {
     Trigger trigger = getTrigger(revision, triggerName, true);

      CommandWithFolderBackup addFilterWithRollback = new AddFilterCommand(filterName, fqFilterClassName,
                                                                           trigger, jarFiles, sharedLibs, description,
                                                                           revision, isStarting, disableFailedInstances,
                                                                           repositoryEvent);

      addFilterWithRollback.execute(new File(deploymentDirPath));
    } catch (InternalException e) {
      Throwable cause = e.getCause();
      if (e.getCause() instanceof XACT_ErrorDuringTriggerAdditionRollback) {
        cause = cause.getCause();
      }

      if (cause instanceof PersistenceLayerException) {
        throw (PersistenceLayerException) cause;
      } else if (cause instanceof Ex_FileAccessException) {
        throw (Ex_FileAccessException) cause;
      } else if (cause instanceof XACT_JarFileUnzipProblem) {
        throw (XACT_JarFileUnzipProblem) cause;
      } else if (cause instanceof XPRC_ExclusiveDeploymentInProgress) {
        throw (XPRC_ExclusiveDeploymentInProgress) cause;
      } else if (cause instanceof XACT_TriggerNotFound) {
        throw (XACT_TriggerNotFound) cause;
      } else if (cause instanceof XPRC_XmlParsingException) {
        throw (XPRC_XmlParsingException) cause;
      } else if (cause instanceof XPRC_InvalidXmlMissingRequiredElementException) {
        throw (XPRC_InvalidXmlMissingRequiredElementException) cause;
      } else if (cause instanceof XACT_AdditionalDependencyDeploymentException) {
        throw (XACT_AdditionalDependencyDeploymentException) cause;
      } else {
        throw new RuntimeException(e.getCause());
      }
    } finally {
      managementLock.unlock();
    }
  }


  public EventListenerInstance getEventListenerInstanceByName(String triggerInstanceName, Long revision,
                                                              boolean followRuntimeContextDependencies) {
    Map<String, EventListenerInstance<?, ?>> relevantMap = eventListenerInstancesByName.get(revision);
    EventListenerInstance ret = null;
    if (relevantMap != null) {
      ret = relevantMap.get(triggerInstanceName);
    }

    if (ret == null && followRuntimeContextDependencies) {
      Set<Long> deps = new HashSet<Long>();
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
          .getDependenciesRecursivly(revision, deps);
      for (long dep : deps) {
        relevantMap = eventListenerInstancesByName.get(dep);
        if (relevantMap != null) {
          ret = relevantMap.get(triggerInstanceName);
          if (ret != null) {
            break;
          }
        }
      }
    }

    return ret;
  }

  @Deprecated
  public void deployFilter(String filtername, String nameOfFilterInstance, String nameOfTriggerInstance, String description, long revision)
      throws XACT_FilterImplClassNotFoundException, XACT_IncompatibleFilterImplException, XACT_FilterNotFound, PersistenceLayerException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException {
    try {
      deployFilter( new DeployFilterParameter.Builder().filterName(filtername).instanceName(nameOfFilterInstance).
          triggerInstanceName(nameOfTriggerInstance).description(description).revision(revision).optional(false).build()
          );
    } catch (XACT_InvalidFilterConfigurationParameterValueException e) {
      //heir unerwartet, da nicht über Parameter konfigurierbar
      throw new RuntimeException("Filter configuration failed", e);
    }
  }

  public void deployFilter(DeployFilterParameter deployFilterParameter) throws XACT_FilterNotFound,
      PersistenceLayerException, XACT_FilterImplClassNotFoundException, XACT_IncompatibleFilterImplException, XFMG_SHARED_LIB_NOT_FOUND,
      XACT_LibOfFilterImplNotFoundException, XACT_InvalidFilterConfigurationParameterValueException {
    FilterInstanceStorable filterInstanceStorable = new FilterInstanceStorable(deployFilterParameter);
    deployFilterInternally( filterInstanceStorable, false, false);
  }
  
  public void deployFilterDisabled(DeployFilterParameter deployFilterParameter) throws XACT_FilterNotFound, PersistenceLayerException {
    FilterInstanceStorable filterInstanceStorable = new FilterInstanceStorable(deployFilterParameter);
    deployFilterInternallyDisabled( filterInstanceStorable );
  }
  
  
  private void deployFilterInternallyDisabled(FilterInstanceStorable filterInstanceStorable) throws XACT_FilterNotFound, PersistenceLayerException {
    try {
      deployFilterInternally(filterInstanceStorable, true, true);
    } catch ( XACT_FilterImplClassNotFoundException |
              XACT_IncompatibleFilterImplException | 
              XFMG_SHARED_LIB_NOT_FOUND |
              XACT_LibOfFilterImplNotFoundException | 
              XACT_InvalidFilterConfigurationParameterValueException 
             e) {
      throw new RuntimeException(e); //kann bei deploydisabled nicht passieren
    }
  }
  
  /**
   * deployed filter. falls bereits ein filter mit dem gleichen namen am gleichen trigger deployed ist, wird er ersetzt.
   * es kann filterinstanzen mit gleichem namen an unterschiedlichen triggerinstanzen geben.
   * -- bei deploydisabled kann geworfen werden:
   * @throws XACT_FilterNotFound 
   * @throws PersistenceLayerException 
   * -- bei deployenabled zusätzlich:
   * @throws XACT_LibOfFilterImplNotFoundException 
   * @throws XFMG_SHARED_LIB_NOT_FOUND 
   * @throws XACT_IncompatibleFilterImplException 
   * @throws XACT_FilterImplClassNotFoundException 
   * @throws StringParameterParsingException 
   * @throws XACT_InvalidFilterConfigurationParameterValueException 
   */
  private void deployFilterInternally(FilterInstanceStorable filterInstanceStorable, boolean deployDisabled, boolean redeploy) 
      throws XACT_FilterNotFound, PersistenceLayerException, XACT_FilterImplClassNotFoundException, 
      XACT_IncompatibleFilterImplException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException, XACT_InvalidFilterConfigurationParameterValueException {
    
    if (logger.isDebugEnabled()) {
      logger.debug("Deploy filter instance '"+filterInstanceStorable.getFilterInstanceName()
        +"' of filter '"+filterInstanceStorable.getFilterName()
        +"' to trigger '"+filterInstanceStorable.getTriggerInstanceName()+"'");
    }
    
    filterInstanceStorable.setEnabled(!deployDisabled);
    
    managementLock.lock();
    try {
     
      boolean persistOnly = deployDisabled;
      EventListenerInstance eli = getEventListenerInstanceByName(filterInstanceStorable.getTriggerInstanceName(), filterInstanceStorable.getRevision(), true);
      if (eli == null) {
        //falls die Triggerinstanz nicht deployed ist, nur den Zustand im FilterInstanceStorable ändern
        persistOnly = true;
        if (!deployDisabled) {
          filterInstanceStorable.setError(new XACT_TriggerInstanceNotFound(filterInstanceStorable.getTriggerInstanceName()));
        }
      }
      
      if (getFilterState(filterInstanceStorable.getFilterName(), filterInstanceStorable.getRevision(), true) == FilterState.ERROR) {
        //Filterinstanz kann auch in den Zustand enabled gehen, wenn der Filter im Zustand ERROR ist
        persistOnly = true; 

        if (filterInstanceStorable.isOptional()) {
          //optional-Flag hat sich evtl. geändert -> überprüfen ob Trigger Thread laufen darf
          restartEventListener(filterInstanceStorable.getTriggerInstanceName(), filterInstanceStorable.getRevision());
        } else if (eli != null) {
          //Trigger Thread darf nicht ohne diese Filterinstanz laufen -> Thread stoppen
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().unregisterEventListener(eli.getInstanceName(), eli.getRevision());
        }
      }

      boolean success = false;
      try {
        if (!persistOnly) {
          //eigentliches Deployment
          instantiateConnectionFilter(filterInstanceStorable, eli, redeploy);
        }
        
        storage.persistObject(filterInstanceStorable);
        success = true;
      } finally {
        if (success || filterInstanceStorable.isOptional() ) {
          restartEventListener(filterInstanceStorable.getTriggerInstanceName(), filterInstanceStorable.getRevision());
        }
      }
    } finally {
      managementLock.unlock();
    }
  }
  

  @SuppressWarnings("unchecked")
  private void instantiateConnectionFilter(FilterInstanceStorable filterInstance, EventListenerInstance eli, boolean redeploy) throws XACT_FilterNotFound,
      PersistenceLayerException, XACT_FilterImplClassNotFoundException, XACT_IncompatibleFilterImplException, XFMG_SHARED_LIB_NOT_FOUND,
      XACT_LibOfFilterImplNotFoundException, XACT_InvalidFilterConfigurationParameterValueException {
    
    String nameOfFilterInstance = filterInstance.getFilterInstanceName();
    Long revisionOfFilterInstance = filterInstance.getRevision();
    Filter filter = getFilter(revisionOfFilterInstance, filterInstance.getFilterName(), true);

    ConnectionFilter cf = null;
    boolean filterHasBeenDeployed = false;
    ConnectionFilterInstance cfi = null;
    try {
      //Undeployment alter Instanzen
      ConnectionFilterInstance[] foundFilters = eli.getEL().getAllFilters();
      for (ConnectionFilterInstance oldCfi : foundFilters) {
        if (oldCfi.getInstanceName().equals(nameOfFilterInstance) && filterInstance.getRevision() == oldCfi.getRevision()) {
          callUndeploymentOfFilterInstance(oldCfi.getCF(), eli.getEL());
          break;
        }
      }
      callUndeploymentOfOutdatedFilterInstances(nameOfFilterInstance, eli.getInstanceName(), revisionOfFilterInstance);

      // Deploy filter
      cf = filter.instantiateFilter(revisionOfFilterInstance);
      cf.onDeployment(eli.getEL());
      filterHasBeenDeployed = true;
      cfi = new ConnectionFilterInstance(cf, filterInstance, filter.getName(), eli.getInstanceName(), filter.getSharedLibs() );
      eli.getEL().addFilter(cfi);
    } catch(
        XACT_FilterImplClassNotFoundException | 
        XACT_IncompatibleFilterImplException | 
        XFMG_SHARED_LIB_NOT_FOUND |
        XACT_LibOfFilterImplNotFoundException |
        XACT_InvalidFilterConfigurationParameterValueException e
        ) {
      handleFilterFailed(filterHasBeenDeployed, cf, eli, filterInstance, redeploy, e);
      throw e;
    } catch (Throwable t) {
      Department.handleThrowable(t);
      handleFilterFailed(filterHasBeenDeployed, cf, eli, filterInstance, redeploy, t);
      throw new RuntimeException(t);
    }
    try {
      //OutdatedFilter zum EventListener hinzufügen
      addOutdatedFilter(cfi, eli);
    } catch (Throwable e) {
      //FIXME fehler werfen? aber dann in den aufrufern ggfs auch nur dann weiterwerfen, wenn alle anderen aufgaben fertig sind?!
      Department.handleThrowable(e); 
      logger.warn("Deployment of filter instance " + filterInstance + " successful, but could not add outdated filter to old application versions.", e);
    }
  }
  
  private void handleFilterFailed(boolean filterHasBeenDeployed, ConnectionFilter cf, 
      EventListenerInstance eli, FilterInstanceStorable filterInstance, boolean redeploy, Throwable t) {
    try {
      if (filterHasBeenDeployed) {
        cf.onUndeployment(eli.getEL());
      }
      filterInstance.setError(t);
      if( redeploy ) {
        //Deployment geschieht automatisch, d.h. ein Fehler hier ist unerwarte und sollte daher gespeichert werden
        storage.persistObject(filterInstance);
      } else {
        //Der Fehler ist während einer initialen Konfigurierung oder einer Umkonfigurierung aufgetreten. 
        //Dieser Vorgang wird mit einer Exception beantwortet, daher sollte der fehlerhafte Zustand 
        //nicht persistiert werden.
      }
      try {
        //trigger anhalten, falls dies kein optionaler filter war, oder der letzte "enabled-te" filter
        checkFiltersHaveNoError(eli, true);
      } catch (XACT_TriggerHasFilterInstanceInErrorStateException e) {
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().unregisterEventListener(eli.getInstanceName(), eli.getRevision());
      }
    } catch( PersistenceLayerException e ) {
      logger.warn("Failed to handle failed filter "+filterInstance.getFilterInstanceName(), e);
    }
  }

  @SuppressWarnings("unchecked")
  private void addOutdatedFilter(ConnectionFilterInstance cfi, EventListenerInstance eli) throws PersistenceLayerException {
    String filtername = cfi.getFilterName();
    String nameOfFilterInstance = cfi.getInstanceName();
    String nameOfTriggerInstance = cfi.getTriggerInstanceName();
    
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    long filterInstanceRevision = cfi.getRevision();
    if (!revisionManagement.isWorkspaceRevision(filterInstanceRevision)) {
      //suche gleich benannte filter in einer älteren version der gleichen applikation
      String currentApplicationName;
      try {
        currentApplicationName = revisionManagement.getApplication(filterInstanceRevision).getName();
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      List<FilterInformation> filterInformations = listFilterInformation();
      for (FilterInformation filterInformation : filterInformations) {
        if (filterInformation.getFilterName().equals(filtername)) { //anderer filter hat den gleichen namen
          for (FilterInstanceInformation filterInstanceInformation : filterInformation.getFilterInstances()) {
            if (filterInstanceInformation.getFilterInstanceName().equals(nameOfFilterInstance)
                            && filterInstanceInformation.getTriggerInstanceName().equals(nameOfTriggerInstance)) { //andere filter instance hat den gleichen namen und ist an einer trigger instanz mit dem gleichen namen deployed 
              if (currentApplicationName.equals(filterInformation.getApplicationName())) { //anderer filter ist in der gleichen application deployed
                long revisionOldFilter;
                try {
                  revisionOldFilter =
                                  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                                  .getRevision(filterInformation.getRuntimeContext());
                } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
                  continue;
                }
                if (!revisionManagement.isWorkspaceRevision(revisionOldFilter)) {
                  if (filterInstanceRevision < revisionOldFilter) { //es existiert bereits eine neuere Version des Filters -> den aktuellen dort als outdated hinzufügen
                    try {
                      EventListenerInstance[] elis = getTriggerInstances(filterInformation.getTriggerName(), revisionOldFilter);
                      for (EventListenerInstance parentEli : elis) {
                        if (parentEli.getInstanceName().equals(nameOfTriggerInstance)) {
                          parentEli.getEL().addOutdatedFilterVersion(cfi);
                        }
                      }
                    } catch (XACT_TriggerNotFound e) {
                      // Trigger nicht mehr gefunden -> Filter muss nicht als outdated hinugefügt werden
                    }
                  }
                  
                  if (revisionOldFilter < filterInstanceRevision) { //anderer filter ist in älterer revision der gleichen application deployed
                    try {
                      Filter oldFilter = getFilter(revisionOldFilter, filtername, false);
                      // puuhhh, geschafft. Jetzt haben wir einen Filter mit einer älteren Revision, der auf die gleiche
                      // Filterinstanz und Triggerinstanz deployt ist/war

                      //nun wird die passende filterinstanz einer alten revision an den aktuellen trigger gebunden,
                      //damit weiterhin aufträge in der alten revision gestartet werden können.

                      try {
                        ConnectionFilter cfiOldFilter = oldFilter.instantiateFilter(filterInstanceRevision, eli.getRevision());
                        cfiOldFilter.onDeployment(eli.getEL());
                        oldFilter.setHasBeenDeployed(eli.getRevision());
                        eli.getEL().addOutdatedFilterVersion(new ConnectionFilterInstance(cfiOldFilter, filtername,
                                                                                          nameOfTriggerInstance,
                                                                                          nameOfFilterInstance,
                                                                                          oldFilter.getSharedLibs(),
                                                                                          oldFilter.getDescription(),
                                                                                          revisionOldFilter));
                      } catch (Exception e) {
                        //trotzdem weitermachen mit nächstem. FIXME diese fehler sollten zu einer zustandsänderung des filters führen, die man sich anschauen kann
                        logger.warn("Outdated filter could not be added to old application version.",
                                    createOldFilterVersionInstantiationException(nameOfFilterInstance, oldFilter.getRevision(), filterInstanceRevision,
                                                                                 nameOfTriggerInstance, e));
                      }
                    } catch (XACT_FilterNotFound e) {
                      //ok, kein älterer Filter vorhanden
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private XACT_OldFilterVersionInstantiationException createOldFilterVersionInstantiationException(String nameOfFilterInstance,
                                                                                                   Long revOld,
                                                                                                   Long revNew,
                                                                                                   String nameOfTriggerInstance,
                                                                                                   Exception e) {
    RevisionManagement rm =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Application applicationOld;
    try {
      applicationOld = rm.getApplication(revOld);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      throw new RuntimeException("unknown revision " + revOld, e);
    }
    Application applicationNew;
    try {
      applicationNew = rm.getApplication(revNew);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      throw new RuntimeException("unknown revision " + revNew, e);
    }
    String vOld = applicationOld.getName() + "/" + applicationOld.getVersionName();
    String vNew = applicationNew.getName() + "/" + applicationNew.getVersionName();
    return new XACT_OldFilterVersionInstantiationException(nameOfFilterInstance, vOld, nameOfTriggerInstance, vNew);
  }


  /**
   * Undeployed filter facade to get the filterInstance
   */
  private ConnectionFilterInstance undeployFilterInternally(String filterInstanceName, Long revisionOfFilterInstance, boolean restartTriggerThread) throws XACT_FilterNotFound,
                  PersistenceLayerException {

    FilterInstanceStorable filterInstanceStorable;
    try {
      filterInstanceStorable = storage.getFilterInstanceByName(filterInstanceName, revisionOfFilterInstance, false);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XACT_FilterNotFound(filterInstanceName);
    }
    
    //outdated filter handling:
    //eigene filter instanz überall entfernen
    removeOutdatedFilterFromOtherTriggers(filterInstanceName, filterInstanceStorable.getTriggerInstanceName(), revisionOfFilterInstance);

    //Undeployment
    ConnectionFilterInstance cfi =
        getFilterInstance(filterInstanceName, filterInstanceStorable.getTriggerInstanceName(), revisionOfFilterInstance);
    if (cfi != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("undeploying filterinstance " + cfi.getInstanceName() + " of filter " + cfi.getFilterName() + " to trigger "
            + filterInstanceStorable.getTriggerInstanceName());
      }
      try {
        undeployFilterInternally(cfi);
      } catch (XACT_TriggerInstanceNotFound e) {
        throw new RuntimeException(e);
      }
      return cfi;
    }

    //falls keine ConnectionFilterInstance gefunden wurde, nur das Storable löschen
    //und ggf. den Trigger Thread wieder starten
    storage.deleteFilterInstance(filterInstanceName, revisionOfFilterInstance);
    executeUndeploymentHandlers(filterInstanceStorable);
    
    if (restartTriggerThread) {
      restartEventListener(filterInstanceStorable.getTriggerInstanceName(), revisionOfFilterInstance);
    }
    
    return null;
  }


  /**
   * Real undeploy filter.
   */
  private void undeployFilterInternally(ConnectionFilterInstance cfi)
                  throws XACT_TriggerInstanceNotFound, PersistenceLayerException {
    try {
      disableFilterInstance(cfi, false);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e); //tritt wegen persist = false nicht auf
    }
    
    FilterInstanceStorable toBeDeleted = new FilterInstanceStorable(cfi.getInstanceName(), cfi.getRevision());
    try {
      storage.deleteFilterInstance(cfi.getInstanceName(), cfi.getRevision());
    } finally {
      executeUndeploymentHandlers(toBeDeleted);
    }
  }


  private void executeUndeploymentHandlers(FilterInstanceStorable fis) {
    managementLock.unlock(); //lock nicht halten, während die undeploymenthandler ausgeführt werden
    try {
      Integer[] priorities = DeploymentHandling.allPriorities;
      for (int i = priorities.length - 1; i >= 0; i--) {
        try {
          XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
              .executeUndeploymentHandler(priorities[i], fis);
        } catch (XPRC_UnDeploymentHandlerException e) {
          logger.warn("Call of undeployment handler failed.", e);
        }
      }
    } finally {
      managementLock.lock();
    }
  }


  public void redeployTriggerInstancesOfTriggerWithSameParas(String fqTriggerClassName, Long revisionOfTrigger) {
    TriggerStorable triggerStorable = null;
    managementLock.lock();
    try {
      triggerStorable = storage.getTriggerByFqClassName(fqTriggerClassName, revisionOfTrigger);
      List<TriggerInstanceStorable> tiss = storage.getTriggerInstancesByTriggerName(triggerStorable.getTriggerName(), triggerStorable.getRevision(), true);
      for (TriggerInstanceStorable tis : tiss) {
        EventListenerInstance el = getEventListenerInstanceByName(tis.getTriggerInstanceName(), tis.getRevision(), false);
        if (el == null) {
          logger.warn("trigger instance " + tis.getTriggerInstanceName() + " not found in revision " + tis.getRevision());
          continue;
        }
        EventListenerInstance newEl = new EventListenerInstance(el); 
        try {
          redeployEventListener(new Trigger(triggerStorable), el, newEl.getStartParameterAsStringArray(), newEl.getDescription(), false);
        } catch (Throwable t) {
          Department.handleThrowable(t);
          
          //Status der Triggerinstanz wurde bereits auf ERROR gesetzt.
          logger.warn("could not redeploy trigger instance " + el.getInstanceName(), t);
        }
      }
    } catch (Throwable t) {
      if (triggerStorable != null) {
        //Trigger-Status auf ERROR setzen
        storage.setTriggerError(triggerStorable.getTriggerName(), revisionOfTrigger, t);
      }
      
      logger.warn("could not redeploy triggerinstances of trigger " + triggerStorable.getTriggerName(), t);
      return;
    } finally {
      managementLock.unlock();
    }
  }

  /**
   * gibt nur laufende filter, keine disabled filter zurück. dafür {@link #getFilterInstancesForFilter(String, Long)} verwenden
   */
  public ConnectionFilterInstance[] getFilterInstances(String filterName, Long revisionOfFilter) {
    List<ConnectionFilterInstance> ret = new ArrayList<ConnectionFilterInstance>();
    managementLock.lock();
    try {
      List<FilterInstanceStorable> fiss;
      try {
        fiss = storage.getFilterInstancesByFilterName(filterName, revisionOfFilter, true);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
      Set<EventListenerInstance> elis = new HashSet<EventListenerInstance>();
      for (FilterInstanceStorable fis : fiss) {
        EventListenerInstance eli = getEventListenerInstanceByName(fis.getTriggerInstanceName(), revisionOfFilter, true);
        if (eli != null) {
          elis.add(eli);
        }
      }
      for (EventListenerInstance eli : elis) {
        ConnectionFilterInstance[] filters = eli.getEL().getAllFilters();
        for (ConnectionFilterInstance cfi : filters) {
          if (cfi.getFilterName().equals(filterName)) {
            ret.add(cfi);
          }
        }
      }
      return ret.toArray(new ConnectionFilterInstance[ret.size()]);
    } finally {
      managementLock.unlock();
    }
  }

  
  /**
   * gibt die OutdatedFilter mit Namen filterName zurück, die Aufträge in revision revisionOfFilter 
   * starten und am Trigger von revision revisionOfTriggerWhereOutdatedFilterInstanceIsRegisteredAt hängen.
   */
  public ConnectionFilterInstance[] getOutdatedFilterInstances(String filterName, Long revisionOfFilter,
                                                               Long revisionOfTriggerWhereOutdatedFilterInstanceIsRegisteredAt) {
    List<ConnectionFilterInstance> ret = new ArrayList<ConnectionFilterInstance>();
    managementLock.lock();
    try {
      List<TriggerInstanceStorable> tiss;
      try {
        FilterStorable fs = storage.getFilterByName(filterName, revisionOfFilter, false);
        TriggerStorable ts =
            storage.getTriggerByName(fs.getTriggerName(), revisionOfTriggerWhereOutdatedFilterInstanceIsRegisteredAt, false);
        tiss =
            storage.getTriggerInstancesByTriggerName(ts.getTriggerName(), revisionOfTriggerWhereOutdatedFilterInstanceIsRegisteredAt, true);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      } catch (XACT_FilterNotFound e) {
        throw new RuntimeException(e);
      } catch (XACT_TriggerNotFound e) {
        throw new RuntimeException(e);
      }
      //checken, ob diese triggerinstanzen nun einen entsprechenden filter haben
      for (TriggerInstanceStorable tis : tiss) {
        if (tis.getStateAsEnum() == TriggerInstanceState.ENABLED) {
          EventListenerInstance<?, ?> eli = getEventListenerInstanceByName(tis.getTriggerInstanceName(), tis.getRevision(), false);
          if (eli != null) {
            List<ConnectionFilterInstance<?>> filters = eli.getEL().getAllOutdatedFilters();
            for (ConnectionFilterInstance cfi : filters) {
              long revisionOfOutdatedFilter = ((ClassLoaderBase) cfi.getCF().getClass().getClassLoader()).getRevision();
              if (cfi.getFilterName().equals(filterName) && revisionOfFilter.equals(revisionOfOutdatedFilter)) {
                ret.add(cfi);
              }
            }
          }
        }
      }

      return ret.toArray(new ConnectionFilterInstance[ret.size()]);
    } finally {
      managementLock.unlock();
    }
  }


  public Filter[] getFilters(long revision) throws PersistenceLayerException {
    managementLock.lock();
    try {
      List<FilterStorable> filterStorables = storage.getFiltersByRevision(revision);
      List<Filter> filters = new ArrayList<Filter>();
      for (FilterStorable storable : filterStorables) {
        Trigger trigger = null;
        try {
          trigger = getTrigger(revision, storable.getTriggerName(), true);
        } catch (XACT_TriggerNotFound e) {
          //trigger nicht vorhanden, also filter invalide, ok.
        }
        filters.add(new Filter(storable, trigger, storable.getTriggerName()));
      }
      
      return filters.toArray(new Filter[filters.size()]);
    } finally {
      managementLock.unlock();
    }
  }


  public Filter[] getFilters(String triggerName, Long revision, boolean followRuntimeContextBackwardDependencies) {
    managementLock.lock();
    try {
      List<FilterStorable> filterStorables =
          storage.getFiltersByTriggerName(triggerName, revision, followRuntimeContextBackwardDependencies);

      if (filterStorables.size() == 0) {
        return new Filter[0];
      }
      Trigger trigger = null;
      try {
        trigger = getTrigger(revision, triggerName, false);
      } catch (XACT_TriggerNotFound e) {
        //ok
      }

      List<Filter> filters = new ArrayList<Filter>();
      for (FilterStorable storable : filterStorables) {
        filters.add(new Filter(storable, trigger, triggerName));
      }

      return filters.toArray(new Filter[filters.size()]);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } finally {
      managementLock.unlock();
    }
  }

  public String getFqFilterClassName(String filterName, Long revision) throws PersistenceLayerException {
    try {
      return storage.getFilterByName(filterName, revision, false).getFqFilterClassName();
    } catch (XACT_FilterNotFound e) {
      return null;
    }
  }
  
  /**
   * gibt nur enabled trigger zurück. Für alle Triggerinstanzen {@link #getTriggerInstancesForTrigger(String, Long, boolean)} verwenden
   */
  public EventListenerInstance[] getTriggerInstances(String triggerName, Long revisionOfTriggerOrParent) throws XACT_TriggerNotFound {
    managementLock.lock();
    try {
      TriggerStorable trigger = storage.getTriggerByName(triggerName, revisionOfTriggerOrParent, true);
      List<TriggerInstanceStorable> tiss = storage.getTriggerInstancesByTriggerName(triggerName, trigger.getRevision(), true);
      List<EventListenerInstance> ret = new ArrayList<EventListenerInstance>();
      
      for (TriggerInstanceStorable tis : tiss) {
        if (tis.getStateAsEnum() == TriggerInstanceState.ENABLED) {
          EventListenerInstance eli = getEventListenerInstanceByName(tis.getTriggerInstanceName(), tis.getRevision(), false);
          if (eli != null) {
            ret.add(eli);
          }
        }
      }
      
      return ret.toArray(new EventListenerInstance[ret.size()]);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } finally {
      managementLock.unlock();
    }
  }


  public Trigger[] getTriggers(Long revision) {
    managementLock.lock();
    try {
      List<TriggerStorable> triggerStorables = storage.getTriggersByRevision(revision);
      
      List<Trigger> triggers = new ArrayList<Trigger>();
      for (TriggerStorable storable : triggerStorables) {
        if (storable.getRevision().equals(revision)) {
          triggers.add(new Trigger(storable));
        }
      }
      
      return triggers.toArray(new Trigger[triggers.size()]);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } finally {
      managementLock.unlock();
    }
  }


  public static String getTriggerXmlLocationByTriggerFqClassName(String triggerFqClassName, Long revision) {
    String triggerName = getSimpleClassName(triggerFqClassName);
    return RevisionManagement.getPathForRevision(PathType.TRIGGER, revision) + Constants.fileSeparator + triggerName
                    + Constants.fileSeparator + triggerName + ".xml";
  }
  
  
  private static String getTriggerDeploymentFolderByTriggerFqClassName(String triggerFqClassName, Long revision) {
    return getTriggerFolderbyTriggerFqClassname(triggerFqClassName, revision, true);
  }

  public static String getTriggerSavedFolderByTriggerFqClassName(String triggerFqClassName, Long revision) {
    return getTriggerFolderbyTriggerFqClassname(triggerFqClassName, revision, false);
  }

  //without ending fileSeperator
  private static String getTriggerFolderbyTriggerFqClassname(String triggerFqClassName, Long revision, boolean deployed) {
    String triggerName = getSimpleClassName(triggerFqClassName);
    return RevisionManagement.getPathForRevision(PathType.TRIGGER, revision, deployed) + Constants.fileSeparator + triggerName;
  }
  
  public static String getSimpleClassName(String fqClassName) {
    String simpleClassName;
    if (fqClassName.contains(".")) {
      simpleClassName = fqClassName.substring(fqClassName.lastIndexOf(".") + 1);
    } else {
      simpleClassName = fqClassName;
    }
    return simpleClassName;
  }


  //without ending fileSeperator
  public static String getFilterDeploymentFolderByFilterFqClassName(String filterFqClassName, Long revision) {
    return getFilterFolderByFilterFqClassName(filterFqClassName, revision, true);
  }

  public static String getFilterSavedFolderByFilterFqClassName(String filterFqClassName, Long revision) {
    return getFilterFolderByFilterFqClassName(filterFqClassName, revision, false);
  }

  private static String getFilterFolderByFilterFqClassName(String filterFqClassName, Long revision, boolean deployed) {
    //FIXME kollision wenn zwei filter den gleichen classname verwenden? achtung: beim refactoring wird aus basis des filenames auf den filter geschlossen
    //eigtl sollte der verzeichnisname dem filternamen entsprechen, nicht dem simpleclassname.
    String filterName = getSimpleClassName(filterFqClassName);
    return RevisionManagement.getPathForRevision(PathType.FILTER, revision, deployed) + Constants.fileSeparator + filterName;
  }


  public static String getFilterXmlLocationByFqFilterClassName(String filterFqClassName, Long revision) {
    String filterName = getSimpleClassName(filterFqClassName);
    return RevisionManagement.getPathForRevision(PathType.FILTER, revision) + Constants.fileSeparator + filterName
        + Constants.fileSeparator + filterName + ".xml";
  }


  private ReentrantLock listenerLock = new ReentrantLock();
  private ArrayList<TriggerChangedListener> triggerChangeListener = new ArrayList<TriggerChangedListener>();
  private ArrayList<FilterChangedListener> filterChangeListener = new ArrayList<FilterChangedListener>();


  public void addTriggerChangeListener(TriggerChangedListener listener) {
    listenerLock.lock();
    try {
      triggerChangeListener.add(listener);
    } finally {
      listenerLock.unlock();
    }
  }


  public void removeTriggerChangeListener(TriggerChangedListener listener) {
    listenerLock.lock();
    try {
      triggerChangeListener.remove(listener);
    } finally {
      listenerLock.unlock();
    }
  }


  public void addFilterChangeListener(FilterChangedListener listener) {
    listenerLock.lock();
    try {
      filterChangeListener.add(listener);
    } finally {
      listenerLock.unlock();
    }
  }


  public void removeFilterChangeListener(FilterChangedListener listener) {
    listenerLock.lock();
    try {
      filterChangeListener.remove(listener);
    } finally {
      listenerLock.unlock();
    }
  }


  private void triggerAdded(Trigger t) throws Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException {
    listenerLock.lock();
    try {
      for (TriggerChangedListener listener : triggerChangeListener) {
        listener.triggerAdded(t);
      }
    } finally {
      listenerLock.unlock();
    }
  }


  private void triggerRemoved(Trigger t) throws PersistenceLayerException, XACT_TriggerNotFound {
    listenerLock.lock();
    try {
      for (TriggerChangedListener listener : triggerChangeListener) {
        listener.triggerRemoved(t);
      }
    } finally {
      listenerLock.unlock();
    }
    TriggerStorable ts = storage.getTriggerByName(t.getTriggerName(), t.getRevision(), false);
    Integer[] priorities = DeploymentHandling.allPriorities;
    for (int i = priorities.length - 1; i >= 0; i--) {
      try {
        XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
            .executeUndeploymentHandler(priorities[i], ts);
      } catch (XPRC_UnDeploymentHandlerException e) {
        logger.warn("Call of undeployment handler failed.", e);
      }
    }
  }


  private void filterAdded(Filter f) throws Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException {
    listenerLock.lock();
    try {
      for (FilterChangedListener listener : filterChangeListener) {
        listener.filterAdded(f);
      }
    } finally {
      listenerLock.unlock();
    }
  }


  private void filterRemoved(Filter f) throws PersistenceLayerException, XACT_FilterNotFound {
    listenerLock.lock();
    try {
      for (FilterChangedListener listener : filterChangeListener) {
        listener.filterRemoved(f);
      }
    } finally {
      listenerLock.unlock();
    }
    FilterStorable fs = storage.getFilterByName(f.getName(), f.getRevision(), false);
    Integer[] priorities = DeploymentHandling.allPriorities;
    for (int i = priorities.length - 1; i >= 0; i--) {
      try {
        XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
            .executeUndeploymentHandler(priorities[i], fs);
      } catch (XPRC_UnDeploymentHandlerException e) {
        logger.warn("Call of undeployment handler failed.", e);
      }
    }
  }


  
  public void configureTriggerMaxEvents(String triggerInstanceName, long maxNumberEvents, boolean autoReject, Long revision)
      throws XACT_TriggerInstanceNotFound, PersistenceLayerException {
    managementLock.lock();
    try {
      EventListenerInstance triggerInstance = getEventListenerInstanceByName(triggerInstanceName, revision, false);
      if (triggerInstance == null) {
        throw new XACT_TriggerInstanceNotFound(triggerInstanceName);
      }

      configureTriggerMaxEvents(triggerInstance.getEL(), maxNumberEvents, autoReject);

      createConfigureTriggerMaxEventsSetting(triggerInstanceName, maxNumberEvents, autoReject, revision);
    } finally {
      managementLock.unlock();
    }
  }
  

  public void createConfigureTriggerMaxEventsSetting(String triggerInstanceName, long maxNumberEvents, boolean autoReject, Long revision)
      throws PersistenceLayerException {
    TriggerConfigurationStorable tcs = new TriggerConfigurationStorable(triggerInstanceName, maxNumberEvents, autoReject, revision);
    storage.persistObject(tcs);
  }


  private void configureTriggerMaxEvents(EventListener el, long maxNumberEvents, boolean autoReject) {
    el.getReceiveControlAlgorithm().setMaxReceivesInParallel(maxNumberEvents);
    el.getReceiveControlAlgorithm().setRejectRequestsAfterMaxReceives(autoReject);
  }
  
  /**
   * Liefert die Configuration der TriggerInstance.
   * @param triggerinstance
   * @return Pair mit first = maxReceives, second = autoReject
   * @throws PersistenceLayerException
   */
  public Pair<Long, Boolean> getTriggerConfiguration(TriggerInstanceInformation triggerinstance) throws PersistenceLayerException {
    Long maxReceives = null;
    Boolean autoReject = null;
    try {
      //Versuche maxReceives aus der TriggerInstance zu bestimmen
      EventListener<?, ?> el = getTriggerInstance(new TriggerInstanceIdentification(triggerinstance.getTriggerName(),
                                                                                    triggerinstance.getRevision(), triggerinstance.getTriggerInstanceName()));
      if (el.getReceiveControlAlgorithm().getMaxReceivesInParallel() >= 0) {
        maxReceives = el.getReceiveControlAlgorithm().getMaxReceivesInParallel();
        autoReject = el.getReceiveControlAlgorithm().isRejectRequestsAfterMaxReceives();
      }
    } catch (XACT_TriggerNotFound e) {
      if (logger.isTraceEnabled()) {
        logger.trace("Could not retrieve trigger instance for maxEvent configuration export.", e);
      }
      //TriggerInstance ist disabled -> maxReceives aus TriggerConfigurationStorable auslesen
      try {
        TriggerConfigurationStorable tcs = storage.getTriggerConfigurationByTriggerInstanceName(triggerinstance.getTriggerInstanceName(), triggerinstance.getRevision());
        maxReceives = tcs.getMaxReceives();
        autoReject = tcs.getAutoReject();
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY ex) {
        //Trigger ist nicht konfiguriert
        if (logger.isDebugEnabled()) {
          logger.debug("triggerinstance " + triggerinstance.getTriggerInstanceName() + " has no separate configuration.");
        }
      }
    }
    
    return Pair.of(maxReceives, autoReject);
  }
  
  
  public static List<File> copyFilesToTargetFolder(String targetDirPath, File[] sourceFiles) throws Ex_FileAccessException {
    List<File> targetFiles = new ArrayList<File>();
    if (sourceFiles == null || sourceFiles.length == 0) {
      return targetFiles;
    }
    File targetDir = new File(targetDirPath);
    for (File sourceFile : sourceFiles) {
      // no need to copy if already inside targetFolder
      String sourceFilePath;
      String absoluteTargetDirPath;
      try {
        sourceFilePath = sourceFile.getCanonicalPath();
        absoluteTargetDirPath = targetDir.getCanonicalPath();
      } catch (IOException e) {
        sourceFilePath = sourceFile.getAbsolutePath();
        absoluteTargetDirPath = targetDir.getAbsolutePath();
      }
      if (!sourceFilePath.startsWith(absoluteTargetDirPath)) {
        StringBuilder fileDestination = new StringBuilder();
        fileDestination.append(absoluteTargetDirPath);
        fileDestination.append(Constants.fileSeparator);
        fileDestination.append(sourceFile.getName());
        File newDestinationFile = new File(fileDestination.toString());
        if (newDestinationFile.exists() && !newDestinationFile.isDirectory()) {
          // delete single files but no target directories
          newDestinationFile.delete();
        }
        File parent = new File(newDestinationFile.getParent());
        parent.mkdirs();
        if (sourceFile.isDirectory()) {
          newDestinationFile.mkdir();
          String[] subFileNames = sourceFile.list();
          if (subFileNames == null) {
            throw new Ex_FileAccessException(sourceFile.getAbsolutePath());
          }
          List<File> subFiles = new ArrayList<File>();
          for (String subFileName : subFileNames) {
            subFiles.add(new File(sourceFile.getAbsolutePath() + Constants.FILE_SEPARATOR + subFileName));
          }
          targetFiles.addAll(copyFilesToTargetFolder(newDestinationFile.getAbsolutePath(), subFiles.toArray(new File[subFiles.size()])));
        } else {
          try {
            newDestinationFile.createNewFile();
          } catch (IOException e) {
            throw new Ex_FileAccessException(newDestinationFile.getPath(), e);
          }
          FileUtils.copyFile(sourceFile, newDestinationFile);
          targetFiles.add(newDestinationFile);
        }
      } else {
        targetFiles.add(sourceFile);
      }
    }
    return targetFiles;
  }


  public boolean enableTriggerInstance(String triggerInstanceName) throws PersistenceLayerException,
    XACT_TriggerInstanceNotFound, XACT_IncompatibleTriggerImplException,
      XACT_TriggerImplClassNotFoundException, XACT_TriggerNotFound, XACT_InvalidStartParameterException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, 
      XACT_TriggerCouldNotBeStartedException, XACT_AdditionalDependencyDeploymentException,
      XACT_TriggerInstanceNeedsEnabledTriggerException {

    return enableTriggerInstance(triggerInstanceName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, true, -1, false);
  }


  public boolean enableTriggerInstance(String triggerInstanceName, Long revision, boolean startThread)
      throws PersistenceLayerException, XACT_TriggerInstanceNotFound, XACT_IncompatibleTriggerImplException,
      XACT_TriggerImplClassNotFoundException, XACT_TriggerNotFound, XACT_InvalidStartParameterException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, 
      XACT_TriggerCouldNotBeStartedException, XACT_AdditionalDependencyDeploymentException,
      XACT_TriggerInstanceNeedsEnabledTriggerException {
    return enableTriggerInstance(triggerInstanceName, revision, startThread, -1, false);
  }


  public boolean enableTriggerInstance(String triggerInstanceName, Long revisionOfTriggerInstance, boolean startThread,
                                       int processingLimit, boolean enableFilterInstances)
      throws PersistenceLayerException, XACT_TriggerInstanceNotFound, XACT_IncompatibleTriggerImplException,
      XACT_TriggerImplClassNotFoundException, XACT_TriggerNotFound, XACT_InvalidStartParameterException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, 
      XACT_TriggerCouldNotBeStartedException, XACT_AdditionalDependencyDeploymentException,
      XACT_TriggerInstanceNeedsEnabledTriggerException {

    managementLock.lock();
    try {
      TriggerInstanceStorable tis = storage.getTriggerInstanceByName(triggerInstanceName, revisionOfTriggerInstance, false);
      if (tis.getStateAsEnum() == TriggerInstanceState.ENABLED) {
        //bereits enabled
        return false;
      } else {
        if (enableFilterInstances) {
          List<FilterInstanceStorable> fis = storage.getFilterInstancesByTriggerInstanceName(triggerInstanceName, revisionOfTriggerInstance, true);
          for (FilterInstanceStorable filterInstance : fis) {
            try {
              enableFilterInstance(filterInstance.getFilterInstanceName(), filterInstance.getRevision());
            } catch (XynaException e) {
              logger.error("Failed to enable filterinstance " + filterInstance.getFilterInstanceName(), e);
            }
          }
        }

        deployTrigger(tis, startThread, processingLimit);
        return true;
      }
    } finally {
      managementLock.unlock();
    }
  }

  
  // undeploy but keep persisted deploymentParameters
  public boolean disableTriggerInstance(String triggerInstanceName) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XACT_TriggerNotFound, XACT_TriggerInstanceNotFound {
    
    return disableTriggerInstance(triggerInstanceName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, false);
  }

  
  // undeploy but keep persisted deploymentParameters
  public boolean disableTriggerInstance(EventListener<?, ?> triggerInstance) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XACT_TriggerNotFound, XACT_TriggerInstanceNotFound {
    TriggerInstanceIdentification tii = triggerInstance.getTriggerInstanceIdentification();
    return disableTriggerInstance(tii.getInstanceName(), tii.getRevision(), false);
  }
  
  /**
   * disabled die Triggerinstanz mit ihren zugehörigen Filterinstanzen
   * @param triggerInstanceName
   * @param revision
   * @return
   * @throws PersistenceLayerException
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY
   * @throws XACT_TriggerNotFound
   * @throws XACT_TriggerInstanceNotFound
   */
  public boolean disableTriggerInstance(String triggerInstanceName, Long revision) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XACT_TriggerNotFound, XACT_TriggerInstanceNotFound {
    return disableTriggerInstance(triggerInstanceName, revision, true, false);
  }

  public boolean disableTriggerInstance(String triggerInstanceName, Long revision, boolean disableFilterInstances) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XACT_TriggerNotFound, XACT_TriggerInstanceNotFound {
    return disableTriggerInstance(triggerInstanceName, revision, disableFilterInstances, false);
  }
  
  
  public boolean disableTriggerInstance(String triggerInstanceName, Long revisionOfTriggerInstance, boolean disableFilterInstances, boolean storableOnly) throws PersistenceLayerException,
    XACT_TriggerNotFound, XACT_TriggerInstanceNotFound, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    managementLock.lock();
    try {
      TriggerInstanceStorable tis = storage.getTriggerInstanceByName(triggerInstanceName, revisionOfTriggerInstance, false);
      if (tis.getStateAsEnum() == TriggerInstanceState.DISABLED) {
        //bereits disabled
        return false;
      } else {
        if (!storableOnly && tis.getStateAsEnum() == TriggerInstanceState.ENABLED) {
          disableTriggerInstanceInternally(triggerInstanceName, revisionOfTriggerInstance, disableFilterInstances, true);
        } else {
          storage.setTriggerInstanceState(triggerInstanceName, revisionOfTriggerInstance, TriggerInstanceState.DISABLED);
        }
        return true;
      }
    } finally {
      managementLock.unlock();
    }
  }

  @Override
  public boolean enableFilterInstance(String filterInstanceName) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XACT_FilterImplClassNotFoundException,
      XACT_IncompatibleFilterImplException, XACT_FilterNotFound, XACT_TriggerInstanceNotFound,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException, 
      XACT_FilterInstanceNeedsEnabledFilterException {

    try {
      return enableFilterInstance(filterInstanceName, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    } catch (XACT_OldFilterVersionInstantiationException e) {
      throw new RuntimeException(e); //workingset hat keine kompatibilität mit anderen versionen
    } catch (XACT_InvalidFilterConfigurationParameterValueException e) {
      throw new RuntimeException(e); //ist ein Fehler beim Deploy, hier unerwartet und wegen 
       //Schnittstellenänderung nicht in Signatur übernommen
    }
  }
  
  
  public boolean enableFilterInstance(String filterInstanceName, Long revisionOfFilterInstance) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XACT_FilterImplClassNotFoundException,
      XACT_IncompatibleFilterImplException, XACT_FilterNotFound, XACT_TriggerInstanceNotFound,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException, 
      XACT_OldFilterVersionInstantiationException, XACT_FilterInstanceNeedsEnabledFilterException, XACT_InvalidFilterConfigurationParameterValueException {
    managementLock.lock();
    try {
      FilterInstanceStorable fis = storage.getFilterInstanceByName(filterInstanceName, revisionOfFilterInstance, false);
      if (fis.getStateAsEnum() == FilterInstanceState.ENABLED) {
        //bereits enabled
        return false;
      } else {
        deployFilterInternally(fis,false, true);
        return true;
      }
    } finally {
      managementLock.unlock();
    }
  }
  
  
  public FilterInstanceInformation getFilterInstanceInformation(String filterInstanceName, long revisionOfFilterInstance) throws PersistenceLayerException {
    try {
      FilterInstanceStorable filterInstance = storage.getFilterInstanceByName(filterInstanceName, revisionOfFilterInstance, false);
      return new FilterInstanceInformation(filterInstance);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    }
  }
  
  
  public Collection<FilterInstanceInformation> getFilterInstanceInformations(long revision) throws PersistenceLayerException {
    Collection<FilterInstanceInformation> fiis = new ArrayList<FilterInstanceInformation>();
    Collection<FilterInstanceStorable> filterInstances = storage.getFilterInstancesByRevision(revision);
    for (FilterInstanceStorable filterInstanceStorable : filterInstances) {
      fiis.add(new FilterInstanceInformation(filterInstanceStorable));
    }
    return fiis;
  }


  public TriggerInstanceInformation getTriggerInstanceInformation(String triggerInstanceName, long revisionOfTriggerInstance)
      throws PersistenceLayerException {
    return getTriggerInstanceInformation(triggerInstanceName, revisionOfTriggerInstance, false);
  }
  
  public TriggerInstanceInformation getTriggerInstanceInformation(String triggerInstanceName, long revisionOfTriggerInstanceOrParent, boolean followRuntimeContextDependencies)
      throws PersistenceLayerException {

    try {
      TriggerInstanceStorable triggerInstance = storage.getTriggerInstanceByName(triggerInstanceName, revisionOfTriggerInstanceOrParent, followRuntimeContextDependencies);
      return new TriggerInstanceInformation(triggerInstance.getTriggerInstanceName(), 
                                            triggerInstance.getTriggerName(),
                                            triggerInstance.getDescription(), 
                                            triggerInstance.getStateAsEnum(),
                                            Arrays.asList(triggerInstance.getStartParameterArray()), 
                                            triggerInstance.getStartParameter(),
                                            triggerInstance.getErrorCause(), 
                                            triggerInstance.getRevision());
    } catch (XACT_TriggerInstanceNotFound e) {
      return null;
    }
  }

  
  public Collection<TriggerInstanceInformation> getTriggerInstanceInformation(long revision) throws PersistenceLayerException {
    Collection<TriggerInstanceStorable> triggerInstances = storage.getTriggerInstancesByRevision(revision);
    Collection<TriggerInstanceInformation> tiis = new ArrayList<TriggerInformation.TriggerInstanceInformation>();
    for (TriggerInstanceStorable triggerInstance : triggerInstances) {
      tiis.add(new TriggerInstanceInformation(triggerInstance.getTriggerInstanceName(), 
                                              triggerInstance.getTriggerName(),
                                              triggerInstance.getDescription(), 
                                              triggerInstance.getStateAsEnum(),
                                              Arrays.asList(triggerInstance.getStartParameterArray()),
                                              triggerInstance.getStartParameter(), 
                                              triggerInstance.getErrorCause(), 
                                              triggerInstance.getRevision()));
    }
    return tiis;
  }
  
  
  public List<FilterInformation> listFilterInformation() throws PersistenceLayerException {
    Collection<FilterStorable> filters = storage.loadCollection(FilterStorable.class);
    Collection<FilterInstanceStorable> allFilterInstances = storage.loadCollection(FilterInstanceStorable.class);
    
    List<FilterInformation> filterInfo = new ArrayList<>();
    for (FilterStorable filterStorable : filters) {
      Set<Long> revisions = new HashSet<Long>(); //TODO wofür revisions filtern?
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
          .getParentRevisionsRecursivly(filterStorable.getRevision(), revisions);
      revisions.add(filterStorable.getRevision());

      List<FilterInstanceStorable> filterInstances = new ArrayList<>();
      for (FilterInstanceStorable filterInstanceStorable : allFilterInstances) {
        if (filterInstanceStorable.getFilterName().equals(filterStorable.getFilterName()) && revisions.contains(filterInstanceStorable.getRevision())) {
          filterInstances.add(filterInstanceStorable);
        }
      }
      filterInfo.add(Filter.getFilterInformation(filterStorable, filterInstances,this));
    }
    return filterInfo;
  }



  /**
   * Liefert alle Filterinstanzen zu einer Triggerinstanz.
   */
  public Collection<FilterInstanceStorable> getFilterInstancesForTriggerInstance(String triggerInstanceName, Long revision, boolean followRuntimeContextBackwardDependencies) throws PersistenceLayerException {
    return storage.getFilterInstancesByTriggerInstanceName(triggerInstanceName, revision, followRuntimeContextBackwardDependencies);
  }
  
  public Collection<FilterInstanceStorable> getFilterInstancesForFilter(String filterName, Long revision) throws PersistenceLayerException {
    return getFilterInstancesForFilter(filterName, revision, true);
  }
  
  /**
   * Liefert alle Filterinstanzen eines Filters.
   */
  public Collection<FilterInstanceStorable> getFilterInstancesForFilter(String filterName, Long revision, boolean followRuntimeContextBackwardDependencies) throws PersistenceLayerException {
    return storage.getFilterInstancesByFilterName(filterName, revision, followRuntimeContextBackwardDependencies);
  }


  /**
   * Liefert alle Triggerinstanzen zu einem Trigger.
   */
  public Collection<TriggerInstanceStorable> getTriggerInstancesForTrigger(String triggerName, Long revision, boolean followRuntimeContextBackwardDependencies) throws PersistenceLayerException {
    return storage.getTriggerInstancesByTriggerName(triggerName, revision, followRuntimeContextBackwardDependencies);
  }
  
  
  public List<TriggerInformation> listTriggerInformation() throws PersistenceLayerException {
    Collection<TriggerStorable> allTriggers = storage.loadCollection(TriggerStorable.class);
    Collection<TriggerInstanceStorable> triggerInstances = storage.loadCollection(TriggerInstanceStorable.class);

    List<TriggerInformation> triggerInfo = new ArrayList<TriggerInformation>();
    for (TriggerStorable triggerStorable : allTriggers) {
      Trigger trigger = new Trigger(triggerStorable);
      List<TriggerInstanceStorable> triggerInstancesSortedList = new ArrayList<TriggerInstanceStorable>(triggerInstances);
      Collections.sort(triggerInstancesSortedList, triggerInstanceComparator);
      triggerInfo.add( Trigger.getTriggerInfo( trigger, triggerStorable, triggerInstancesSortedList) );
    }
    
    return triggerInfo;
  } 


  public void appendTriggerState(StringBuilder sb, String triggerName, Long revisionOfTriggerOrParent, boolean verbose)
      throws PersistenceLayerException, XACT_TriggerNotFound {

    TriggerStorable trigger = storage.getTriggerByName(triggerName, revisionOfTriggerOrParent, true);
    Collection<TriggerInstanceStorable> triggerInstances =
        storage.getTriggerInstancesByTriggerName(triggerName, trigger.getRevision(), true);

    sb.append("Trigger ").append(triggerName).append(": ").append(trigger.getStateAsEnum());
    if (triggerInstances != null && triggerInstances.size() > 0) {
      sb.append("\n has ").append(triggerInstances.size()).append(" instance")
          .append(triggerInstances.size() > 1 ? "s" : "");
      if (verbose) {
        sb.append(":");
      }

      int numberEnabled = 0;
      int numberUnexpectedlyNotRunning = 0;
      for (TriggerInstanceStorable ti : triggerInstances) {
        if (verbose) {
          sb.append("\n  - ").append(ti.getTriggerInstanceName()).append(": ").append(ti.getStateAsEnum());
        }

        if (ti.getStateAsEnum() == TriggerInstanceState.ENABLED) {
          numberEnabled++;
          //Überprüfung, ob der Thread läuft
          EventListenerInstance eli =
              XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution()
                  .getEventListenerByName(ti.getTriggerInstanceName(), ti.getRevision());
          if (eli == null) {
            if (verbose) {
              sb.append(" (NOT RUNNING)");
            }
            numberUnexpectedlyNotRunning++;
          }
        }
      }

      if (!verbose) {
        sb.append(" (" + (numberEnabled-numberUnexpectedlyNotRunning) + " enabled and running");
        if (numberUnexpectedlyNotRunning > 0) {
          sb.append(", " + numberUnexpectedlyNotRunning + " enabled but not running, check verbose output)");
        } else {
          sb.append(")");
        }
      }

    }
    
    Collection<FilterStorable> filters = storage.getFiltersByTriggerName(triggerName, trigger.getRevision(), true);
    if (filters.size() > 0) {
      sb.append("\n and ").append(filters.size()).append(" filter").append(filters.size() > 1 ? "s" : "");
    }
    if (verbose) {
      sb.append(":");
      for (FilterStorable filter : filters) {
        sb.append("\n  - ").append(filter.getFilterName()).append(": ").append(filter.getStateAsEnum());

        Collection<FilterInstanceStorable> filterInstances = storage.getFilterInstancesByFilterName(filter.getFilterName(), filter.getRevision(), true);
        if (filterInstances != null && filterInstances.size() > 0) {
          sb.append("\n     with ").append(filterInstances.size()).append(" instance").append(filterInstances.size() > 1 ? "s:" : ":");
          for (FilterInstanceStorable fi : filterInstances) {
            sb.append("\n").append("      - ").append(fi.getFilterInstanceName()).append(" on ").append(fi.getTriggerInstanceName()).append(": ").append(fi.getStateAsEnum());
            if (fi.isOptional()) {
              sb.append(" (optional)");
            }
          }
        }
      }
    }
  }

  
  /**
   * @deprecated use el.getTriggerInstanceIdentification()
   */
  @Deprecated
  public String getTriggerInstanceName(EventListener el) {
    return el.getTriggerInstanceIdentification().getInstanceName();
  }
  

  /**
   * @param el
   * @return
   * @deprecated use el.getTriggerInstanceIdentification()
   */
  @Deprecated
  public Pair<String, Long> getTriggerInstanceNameAndRevision(EventListener el) {
    TriggerInstanceIdentification tii = el.getTriggerInstanceIdentification();
    return Pair.of( tii.getInstanceName(), tii.getRevision() );
  }


  public Trigger getTrigger(Long revision, String triggerName, boolean followRuntimeContextDependencies) throws PersistenceLayerException, XACT_TriggerNotFound {
    TriggerStorable storable = storage.getTriggerByName(triggerName, revision, followRuntimeContextDependencies);
    return new Trigger(storable);
  }


  public Filter getFilter(Long revision, String filterName, boolean followRuntimeContextDependencies) throws PersistenceLayerException,
      XACT_FilterNotFound {
    managementLock.lock();
    try {
      FilterStorable storable = storage.getFilterByName(filterName, revision, followRuntimeContextDependencies);
      Trigger trigger = null;
      try {
        trigger = getTrigger(revision, storable.getTriggerName(), true);
      } catch (XACT_TriggerNotFound e) {
        //ok
      }
      return new Filter(storable, trigger, storable.getTriggerName());
    } finally {
      managementLock.unlock();
    }
  }
  


  public void deployTrigger(String triggerName, String nameOfTriggerInstance, String[] startParameter,
                            String description, long revision) throws XACT_TriggerNotFound, XACT_IncompatibleTriggerImplException,
      XACT_TriggerImplClassNotFoundException, XACT_InvalidStartParameterException, PersistenceLayerException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, 
      XACT_TriggerCouldNotBeStartedException, XACT_AdditionalDependencyDeploymentException,
      XACT_TriggerInstanceNeedsEnabledTriggerException {
    boolean startThread = true;
    deployTrigger(triggerName, nameOfTriggerInstance, startParameter, description,
                  revision, startThread);
  }


  public void undeployTrigger(String nameOfTrigger, String nameOfTriggerInstance) throws XACT_TriggerNotFound,
                  PersistenceLayerException, XACT_TriggerInstanceNotFound {
    undeployTrigger(nameOfTrigger, nameOfTriggerInstance, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public Filter[] getFilters(String triggerName) {
    return getFilters(triggerName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, false);
  }


  public EventListenerInstance<?, ?>[] getTriggerInstances(String triggerName) throws XACT_TriggerNotFound {
    return getTriggerInstances(triggerName, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public Trigger[] getTriggers() {
    return getTriggers(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public ConnectionFilterInstance<?>[] getFilterInstances(String filterName) {
    return getFilterInstances(filterName, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public void configureTriggerMaxEvents(String triggerInstanceName, long maxNumberEvents, boolean autoReject)
                  throws XACT_TriggerInstanceNotFound, PersistenceLayerException {
    configureTriggerMaxEvents(triggerInstanceName, maxNumberEvents, autoReject, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public EventListener<?, ?> getTriggerInstance(TriggerInstanceIdentification triggerInstanceId) throws XACT_TriggerNotFound {
    EventListenerInstance el = getEventListenerInstanceByName(triggerInstanceId.getInstanceName(), triggerInstanceId.getRevision(), false);
    if (el == null) {
      throw new XACT_TriggerNotFound(triggerInstanceId.getName(), new Exception("Instance \"" + triggerInstanceId.getInstanceName()
          + "\" does not exist"));
    }
    return el.getEL();
  }


  /**
   * addFilter mit den bestehenden parametern.
   * nur im workingset. wird z.b. von refactoring verwendet, nachdem sich das filter-xml geändert hat
   */
  public void reAddExistingFilterWithExistingParameters(String filterSimpleClassName, Long revision) throws XPRC_ExclusiveDeploymentInProgress,
      XACT_FilterImplClassNotFoundException, XACT_TriggerNotFound, PersistenceLayerException,
      Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException,
      XACT_AdditionalDependencyDeploymentException, XACT_JarFileUnzipProblem, XACT_LibOfFilterImplNotFoundException,
      XFMG_SHARED_LIB_NOT_FOUND {
    List<FilterStorable> filters = storage.getFiltersByRevision(revision);
    for (FilterStorable filter : filters) {
      if (getSimpleClassName(filter.getFqFilterClassName()).equals(filterSimpleClassName)) {
        addFilterInternally(filter.getFilterName(), filter.getRevision(), filter.getJarFilesAsArray(), filter.getFqFilterClassName(), filter
            .getTriggerName(), filter.getSharedLibsArray(), filter.getDescription(), false, false, false, new SingleRepositoryEvent(filter.getRevision()));
        break;
      }
    }
  }
  
  
  /**
   * Liefert alle Filterinstanzen mit ihren outdated versions
   */
  public Collection<FilterInstanceVersions> listOutdatedFilterInstances() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException, XACT_TriggerNotFound {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                    .getRevisionManagement();
    List<Long> allRevisions = rm.getAllApplicationRevisions();
    
    Map<Pair<Long, String>, FilterInstanceVersions> filterInstances = new HashMap<Pair<Long, String>, FilterInstanceVersions>();
    
    for (Long revision : allRevisions) {
      String applicationName = rm.getApplication(revision).getName();
      String versionName = rm.getApplication(revision).getVersionName();
      
      managementLock.lock();
      try {
        //alle Filterinstanzen suchen
        for (Filter filter: getFilters(revision)) {
          for (FilterInstanceStorable fis : getFilterInstancesForFilter(filter.getName(), filter.getRevision(), true)) {
            //der Filterinstancename ist innerhalb einer Revision eindeutig
            Pair<Long, String> key = Pair.of(fis.getRevision(), fis.getFilterInstanceName());
            FilterInstanceVersions fiv = new FilterInstanceVersions(applicationName, versionName, fis);
            filterInstances.put(key, fiv);
          }
        }
        
        //die zugehörigen outdated versions suchen
        for (Trigger trigger : getTriggers(revision)) {
          for (EventListenerInstance<?, ?> eli : getTriggerInstances(trigger.getTriggerName(), trigger.getRevision())) {
            List<ConnectionFilterInstance<?>> outdatedFilters = eli.getEL().getAllOutdatedFilters();

            if (outdatedFilters != null && outdatedFilters.size() > 0) {
              for (ConnectionFilterInstance outdated : outdatedFilters) {
                String outdatedVersion = rm.getApplication(outdated.getRevision()).getVersionName();
                Pair<Long, String> key = Pair.of(outdated.getRevision(), outdated.getInstanceName());
                filterInstances.get(key).addOutdatedVersion(outdatedVersion);
              }
            }
          }
        }
      } finally {
        managementLock.unlock();
      }
    }
    
    return filterInstances.values();
  }

  
  public void setTriggerInstanceError(String triggerInstanceName, Long revision, Throwable t) {
    storage.setTriggerInstanceError(triggerInstanceName, revision, t);
  }

  public void setTriggerInstanceState(String triggerInstanceName, Long revision, TriggerInstanceState state) throws PersistenceLayerException, XACT_TriggerInstanceNotFound {
    storage.setTriggerInstanceState(triggerInstanceName, revision, state);
  }


  private TriggerState getTriggerState(String triggerName, Long revision, boolean followRuntimeContextDependencies)
      throws PersistenceLayerException, XACT_TriggerNotFound {
    TriggerStorable storable = storage.getTriggerByName(triggerName, revision, followRuntimeContextDependencies);
    if (storable.getStateAsEnum() == null) {
      return TriggerState.OK; //Default ist OK
    }
    return storable.getStateAsEnum();
  }


  private FilterState getFilterState(String filterName, Long revision, boolean followRuntimeContextDependencies)
      throws PersistenceLayerException, XACT_FilterNotFound {
    FilterStorable storable = storage.getFilterByName(filterName, revision, followRuntimeContextDependencies);
    if (storable.getStateAsEnum() == null) {
      return FilterState.OK; //Default ist OK
    }
    return storable.getStateAsEnum();
  }


  public FilterInformation getFilterInformation(String filterName, long revision, boolean followRuntimeContextDependenciesToFindFilter)
      throws PersistenceLayerException, XACT_FilterNotFound {
    FilterStorable filter = storage.getFilterByName(filterName, revision, followRuntimeContextDependenciesToFindFilter);
    return Filter.getFilterInformation(filter, storage.getFilterInstancesByFilterName(filterName, filter.getRevision(), true), this );
  }


  public TriggerInformation getTriggerInformation(String triggerName, long revision, boolean followRuntimeContextDependenciesToFindTrigger)
      throws PersistenceLayerException, XACT_TriggerNotFound {
    TriggerStorable triggerStorable = storage.getTriggerByName(triggerName, revision, followRuntimeContextDependenciesToFindTrigger);
    Trigger trigger = new Trigger(triggerStorable);

    List<TriggerInstanceStorable> triggerInstances =
        storage.getTriggerInstancesByTriggerName(triggerName, triggerStorable.getRevision(), true);
    List<TriggerInstanceStorable> triggerInstancesSortedList = new ArrayList<TriggerInstanceStorable>(triggerInstances);
    Collections.sort(triggerInstancesSortedList, triggerInstanceComparator);
    return Trigger.getTriggerInfo(trigger, triggerStorable, triggerInstancesSortedList);
  }


  public ConnectionFilterInstance<?> getFilterInstance(String filterInstanceName, String triggerInstanceName, long filterInstanceRevision) {
    EventListenerInstance eli = getEventListenerInstanceByName(triggerInstanceName, filterInstanceRevision, true);
    if (eli == null) {
      return null;
    }
    for (ConnectionFilterInstance<?> cfi : eli.getEL().getAllFilters()) {
      if (cfi.getRevision() == filterInstanceRevision) {
        if (cfi.getInstanceName().equals(filterInstanceName)) {
          return cfi;
        }
      }
    }
    return null;
  }


  public void unlockManagement() {
    managementLock.unlock();
  }

  public void lockManagement() {
    managementLock.lock();
  }


}
