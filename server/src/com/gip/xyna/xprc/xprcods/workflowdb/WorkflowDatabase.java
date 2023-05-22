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
package com.gip.xyna.xprc.xprcods.workflowdb;



import java.io.File;
import java.io.FileFilter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.BijectiveMap;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelStorage;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Connection;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;



public class WorkflowDatabase extends FunctionGroup {

  public static final String DEFAULT_NAME = "Workflow Database";
  public static final Logger logger = CentralFactoryLogging.getLogger(WorkflowDatabase.class);

  public static final int FUTURE_EXECUTION_ID = XynaFactory.getInstance().getFutureExecution().nextId();
  
  /*
   * listen der fq original names, nicht der class names
   * 
   * F�r die deployedProcessOriginalFQs werden die fqOriginalNames und die classNames
   * in einer BijectiveMap (fqOriginalNames -> classNames) gespeichert
   */
  private HashMap<Long, Set<String>> deployedDatatypesFQs;
  private HashMap<Long, Set<String>> deployedExceptionsFQs;
  private HashMap<Long, BijectiveMap<String, String>> deployedProcessOriginalFQs;
  private HashMap<Long, Set<String>> savedProcessOriginalFQs;

  private ReentrantLock lock = new ReentrantLock();

  public enum DeploymentStatus {
    DEPLOYED, SAVED, DEPLOYMENT_ERROR, DEPLOYED_STOPPED
  }

  public WorkflowDatabase() throws XynaException {
    super();
  }


  private WorkflowDatabase(String cause) throws XynaException {
    super(cause);
    deployedProcessOriginalFQs = new HashMap<Long, BijectiveMap<String, String>>();
    savedProcessOriginalFQs = new HashMap<Long, Set<String>>();
    deployedDatatypesFQs = new HashMap<Long, Set<String>>();
    deployedExceptionsFQs = new HashMap<Long, Set<String>>();
    try {
      ODS ods = ODSImpl.getInstance();
      ods.registerStorable(DeployedProcessOriginalFQsStorable.class);
      ods.registerStorable(DeployedDatatypesAndExceptionsStorable.class);
      
      loadDeployedProcessOriginalFQs();
    } catch (PersistenceLayerException e) {
      logger.error("", e);
    }
  }


  public static WorkflowDatabase getWorkflowDatabasePreInit() throws XynaException {
    return new WorkflowDatabase("preInit");
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public void shutdown() throws XynaException {
  }

  
  private Set<String> getOrCreateSetForRevision(Long revision, HashMap<Long, Set<String>> mapOfSets) {
    Set<String> set = mapOfSets.get(revision);
    if(set == null) {
      synchronized (mapOfSets) {
        set = mapOfSets.get(revision);
        if(set == null) {
          set = new HashSet<String>();
          mapOfSets.put(revision, set);
        }
      }
    }
    return set;
  }
  
  private Map<String, String> getOrCreateMapForRevision(Long revision, HashMap<Long, BijectiveMap<String, String>> mapOfMaps) {
    BijectiveMap<String, String> map = mapOfMaps.get(revision);
    if(map == null) {
      synchronized (mapOfMaps) {
        map = mapOfMaps.get(revision);
        if(map == null) {
          map = new BijectiveMap<String, String>();
          mapOfMaps.put(revision, map);
        }
      }
    }
    return map;
  }
  
  
  public void init() throws XynaException {
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(FUTURE_EXECUTION_ID, "WorkflowDatabase").
       after(XynaFractalWorkflowEngine.FUTUREEXECUTION_ID).
       after(XynaProcessCtrlExecution.class).
       after(DependencyRegister.ID_FUTURE_EXECUTION).
       after(XynaProcessingODS.FUTUREEXECUTION_ID).
       after(XynaClusteringServicesManagement.class).
       after(RevisionManagement.class).
       after(DataModelStorage.class).  //darf bestimmen, ob Datenmodel-Typen deployt werden d�rfen
       after(RuntimeContextDependencyManagement.class).
       execAsync( new Runnable(){ public void run() { 
         try { initLater(); } catch( XynaException xe ) { throw new RuntimeException(xe); } } } ); 
    
  }


  public void initLater() throws XynaException {
    long t1 = System.currentTimeMillis();

    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(DeployedProcessOriginalFQsStorable.class);
    ods.registerStorable(DeployedDatatypesAndExceptionsStorable.class);

    // FIXME potential connection pool deadlock when configured to a limited database connection pool
    lock.lock();
    try {
      reloadSharedLibs();      
      
      deployedProcessOriginalFQs = new HashMap<Long, BijectiveMap<String, String>>();
      savedProcessOriginalFQs = new HashMap<Long, Set<String>>();
      deployedDatatypesFQs = new HashMap<Long, Set<String>>();
      deployedExceptionsFQs = new HashMap<Long, Set<String>>();
      try {
        loadDeployedProcessOriginalFQs();
      } catch (PersistenceLayerException e) {
        logger.error("error loading wf: " + e.getMessage(), e);
      }

      updateSavedProcessFQs();

      Set<Long> revisions = new HashSet<Long>();
      revisions.addAll(deployedProcessOriginalFQs.keySet());
      revisions.addAll(deployedDatatypesFQs.keySet());
      revisions.addAll(deployedExceptionsFQs.keySet());
      
      List<Long> orderedRevisions = orderRevisions(revisions);

      boolean removedRevision = false;
      boolean singleBatchDeploy = XynaProperty.WORKFLOW_DB_SINGLE_BATCH_DEPLOY.get();
      List<GenerationBase> allObjects = new ArrayList<GenerationBase>();
      for (Long revision : orderedRevisions) {
        if (!(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getAllRevisions()
            .contains(revision))) {
          logger.info("Removing workflowdatabase entries for nonexistent revision " + revision);
          deployedDatatypesFQs.remove(revision);
          deployedExceptionsFQs.remove(revision);
          deployedProcessOriginalFQs.remove(revision);
          removedRevision = true;
        }
        Set<String> deployedDatatypesSet = getOrCreateSetForRevision(revision, deployedDatatypesFQs);
        Set<String> deployedExceptionsSet = getOrCreateSetForRevision(revision, deployedExceptionsFQs);
        Map<String, String> deployedWfsMap = getOrCreateMapForRevision(revision, deployedProcessOriginalFQs);

        List<GenerationBase> objects = new ArrayList<GenerationBase>();
        for (String deployException : deployedExceptionsSet) {
          objects.add(ExceptionGeneration.getInstance(deployException, revision));
        }
        for (String deployDatatype : deployedDatatypesSet) {
          objects.add(DOM.getInstance(deployDatatype, revision));
        }
        for (String deployWf : deployedWfsMap.keySet()) {
          objects.add(WF.getInstance(deployWf, revision));
        }
        
        if (singleBatchDeploy) {
          allObjects.addAll(objects);
        } else {
          lock.unlock(); //w�hrend des deployments das lockfreigeben
          try {
            for (GenerationBase gb : objects) {
              gb.setDeploymentComment("Serverstart: reload");
            }
            
            GenerationBase.deploy(objects, DeploymentMode.reload, false, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);
          } catch (Throwable t) {
            if (t instanceof MDMParallelDeploymentException) {
              MDMParallelDeploymentException ex = (MDMParallelDeploymentException) t;
              logger.error("Could not load " + ex.getNumberOfFailedObjects() + " xmomobjects, continuing serverstart.");
              for (GenerationBase object : ex.getFailedObjects()) {
                logger.error("Could not load xmomobject " + object.getOriginalFqName() + " from revision " + object.getRevision(), object.getExceptionCause());
                if (object.getExceptionWhileOnError() != null) {
                  logger.error("Errors occurred during cleanup of xmomobject " + object.getOriginalFqName(), object.getExceptionWhileOnError());
                }
                XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,"Could not deploy xmomobject " + object.getOriginalFqName() + " @rev_" + object.getRevision());
              }
            } else {
              logger.error("Could not load xmomobjects, continuing serverstart.", t);
              XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,"Could not deploy xmomobjects in revision " + revision);
            }
            Department.handleThrowable(t);
          } finally {
            lock.lock();
          }
        }
      }
      
      if (singleBatchDeploy) {
        lock.unlock(); //w�hrend des deployments das lockfreigeben
        try {
          for (GenerationBase gb : allObjects) {
            gb.setDeploymentComment("Serverstart: reload");
          }
          
          GenerationBase.deploy(allObjects, DeploymentMode.reload, false, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);
        } catch (Throwable t) {
          if (t instanceof MDMParallelDeploymentException) {
            MDMParallelDeploymentException ex = (MDMParallelDeploymentException) t;
            logger.error("Could not load " + ex.getNumberOfFailedObjects() + " xmomobjects, continuing serverstart.");
            for (GenerationBase object : ex.getFailedObjects()) {
              logger.error("Could not load xmomobject " + object.getOriginalFqName() + " from revision " + object.getRevision(), object.getExceptionCause());
              if (object.getExceptionWhileOnError() != null) {
                logger.error("Errors occurred during cleanup of xmomobject " + object.getOriginalFqName(), object.getExceptionWhileOnError());
              }
              XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,"Could not deploy xmomobject " + object.getOriginalFqName() + " @rev_" + object.getRevision());
            }
          } else {
            logger.error("Could not load xmomobjects, continuing serverstart.", t);
            XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,"Could not deploy xmomobjects in revision ");
          }
          Department.handleThrowable(t);
        } finally {
          lock.lock();
        }
      }
      
      if (removedRevision) {
        persistDeployedObjects();
      }
    } finally {
      lock.unlock();
    }
    if (logger.isDebugEnabled()) {
      logger.debug("workflow deployment took " + (System.currentTimeMillis() - t1) + "ms.");
    }

    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .addDeploymentHandler(DeploymentHandling.PRIORITY_WORKFLOW_DATABASE, new DeploymentHandler() {

          private boolean deployedWFsChanged = false;
          private boolean deployedDatatypesOrExceptionsChanged = false;

          public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
            lock.lock();
            try {
              if (object instanceof WF) {
                if (!getOrCreateMapForRevision(object.getRevision(), deployedProcessOriginalFQs).containsKey(object.getOriginalFqName())) {
                  getOrCreateMapForRevision(object.getRevision(), deployedProcessOriginalFQs).put(object.getOriginalFqName(), object.getFqClassName());
                  removeSaved(object.getOriginalFqName(), object.getRevision());
                  if (mustPersistChanges(mode)) {
                    deployedWFsChanged = true;
                  }
                }
              } else if (object instanceof DOM) {
                if (getOrCreateSetForRevision(object.getRevision(), deployedDatatypesFQs).add(object.getOriginalFqName())) {
                  if (mustPersistChanges(mode)) {
                    deployedDatatypesOrExceptionsChanged = true;
                  }
                }
              } else if (object instanceof ExceptionGeneration) {
                if (getOrCreateSetForRevision(object.getRevision(), deployedExceptionsFQs).add(object.getOriginalFqName())) {
                  if (mustPersistChanges(mode)) {
                    deployedDatatypesOrExceptionsChanged = true;
                  }
                }
              }
            } finally {
              lock.unlock();
            }
          }


          private boolean mustPersistChanges(DeploymentMode mode) {
            //TODO eigtl nur codeNew?
            return mode == DeploymentMode.codeChanged || mode == DeploymentMode.codeNew || mode == DeploymentMode.deployBackup;
          }


          public void finish(boolean success) throws XPRC_DeploymentHandlerException {
            /*
             * TODO verbesserungsideen:
             * - falls nicht success, nicht speichern? oder muss dann generationbase in onError undeploymenthandler ausf�hren?
             * - falls auf DB statt auf XML konfiguriert, kann man feingranularer speichern
             */
            lock.lock(); //booleans atomar umsetzen, wenn mehrere deploymentvorg�nge parallel am laufen sind
            try {
              if (deployedWFsChanged) {
                persistDeployedProcessOriginalFQs();
              }
              if (deployedDatatypesOrExceptionsChanged) {
                persistDeployedDatatypesAndExceptions();
              }
              deployedWFsChanged = false;
              deployedDatatypesOrExceptionsChanged = false;
            } catch (PersistenceLayerException e) {
              throw new XPRC_DeploymentHandlerException("unknown", "workflowdatabase", e);
            } finally {
              lock.unlock();
            }
          }


          @Override
          public void begin() throws XPRC_DeploymentHandlerException {
          }

        });

    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .addUndeploymentHandler(DeploymentHandling.PRIORITY_WORKFLOW_DATABASE, new UndeploymentHandler() {
          
          private boolean deployedWFsChanged = false;
          private boolean deployedDatatypesOrExceptionsChanged = false;


          public void exec(GenerationBase object) throws XPRC_UnDeploymentHandlerException {
            lock.lock();
            try {
              if (object instanceof WF) {
                if (getOrCreateMapForRevision(object.getRevision(), deployedProcessOriginalFQs).remove(object.getOriginalFqName()) != null) {
                  addSaved(object.getOriginalFqName(), object.getRevision());
                  deployedWFsChanged = true;
                }
              } else if (object instanceof DOM) {
                if (getOrCreateSetForRevision(object.getRevision(), deployedDatatypesFQs).remove(object.getOriginalFqName())) {
                  deployedDatatypesOrExceptionsChanged = true;
                }
              } else if (object instanceof ExceptionGeneration) {
                if (getOrCreateSetForRevision(object.getRevision(), deployedExceptionsFQs).remove(object.getOriginalFqName())) {
                  deployedDatatypesOrExceptionsChanged = true;
                }
              }
            } finally {
              lock.unlock();
            }
          }

          public void finish() throws XPRC_UnDeploymentHandlerException {
            lock.lock(); //booleans atomar umsetzen, wenn mehrere deploymentvorg�nge parallel am laufen sind
            try {
              if (deployedWFsChanged) {
                persistDeployedProcessOriginalFQs();
              }
              if (deployedDatatypesOrExceptionsChanged) {
                persistDeployedDatatypesAndExceptions();
              }
              deployedWFsChanged = false;
              deployedDatatypesOrExceptionsChanged = false;
            } catch (PersistenceLayerException e) {
              throw new XPRC_UnDeploymentHandlerException("unknown", "workflowdatabase", e);
            } finally {
              lock.unlock();
            }
          }

          public void exec(FilterInstanceStorable object) {
          }

          public void exec(TriggerInstanceStorable object) {
          }

          public void exec(Capacity object) {
          }

          public void exec(DestinationKey object) {
          }

          public boolean executeForReservedServerObjects(){
            return false;
          }

          public void exec(FilterStorable object) {
          }

          public void exec(TriggerStorable object) {
          }
        });

  }
  
  
  private static void reloadSharedLibs() {
    Map<Long, Set<String>> deployedSharedLibs = discoverDeployedSharedLibs();
    for (Long revision : deployedSharedLibs.keySet()) {
      for (String sharedLibName : deployedSharedLibs.get(revision)) {
        try {
          reloadSharedLib(sharedLibName, revision);
        } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
          logger.warn("Error during sharedLib deployment, continuing server start", e);
        }
      }
    }
  }
  
  
  public static void reloadSharedLib(String sharedLibName, Long revision) throws XFMG_SHARED_LIB_NOT_FOUND {
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getClassLoaderDispatcher();
    cld.getSharedLibClassLoaderLazyCreate(sharedLibName, revision);
  }


  private static Map<Long, Set<String>> discoverDeployedSharedLibs() {
    Map<Long, Set<String>> sharedLibs = new HashMap<Long, Set<String>>();
    List<Long> allRevisions = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getAllRevisions();
    for (Long aRevision : allRevisions) {
      String sharedLibPath = RevisionManagement.getPathForRevision(PathType.SHAREDLIB, aRevision, true);
      File sharedLibFile = new File(sharedLibPath);
      if (sharedLibFile.exists()) {
        File[] sharedLibFolders = sharedLibFile.listFiles(new FileFilter() {
          public boolean accept(File pathname) {
            return pathname.isDirectory();
          }
        });
        if (sharedLibFolders != null &&
            sharedLibFolders.length > 0) {
          Set<String> localSharedLibs = new HashSet<String>();
          for (File sharedLibFolder : sharedLibFolders) {
            localSharedLibs.add(sharedLibFolder.getName());
          }
          sharedLibs.put(aRevision, localSharedLibs);
        }
      }
    }
    return sharedLibs;
  }


  private List<Long> orderRevisions(Set<Long> revisions) {
    List<Long> l = new ArrayList<>(revisions);
    Collections.sort(l, new Comparator<Long>() {

      RuntimeContextDependencyManagement rtCtxMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();

      @Override
      public int compare(Long o1, Long o2) {
        return Integer.compare(numberOfDependencies(o1), numberOfDependencies(o2));
      }

      private int numberOfDependencies(Long r) {
        Set<Long> set = new HashSet<>();
        rtCtxMgmt.getDependenciesRecursivly(r, set);
        return set.size();
      }
     
    });
    return l;
  }


  public void addSaved(String xpOriginalFQName, Long revision) {
    lock.lock();
    try {
      if (!getOrCreateMapForRevision(revision, deployedProcessOriginalFQs).containsKey(xpOriginalFQName))
       getOrCreateSetForRevision(revision,  savedProcessOriginalFQs).add(xpOriginalFQName);
    } finally {
      lock.unlock();
    }
  }


  public void removeSaved(String xpOriginalFQName, Long revision) {
    lock.lock();
    try {
      getOrCreateSetForRevision(revision,  savedProcessOriginalFQs).remove(xpOriginalFQName);
    } finally {
      lock.unlock();
    }
  }


  public boolean isRegisteredByFQ(String xpOriginalFQName, Long revision) {
    return this.isRegisteredByFQ(XMOMType.WORKFLOW, xpOriginalFQName, revision);
  }
  
  
  public boolean isRegisteredByFQ(XMOMType type, String originalFQName, Long revision) {
    lock.lock();
    try {
      switch (type) {
        case DATATYPE :
          return getOrCreateSetForRevision(revision, deployedDatatypesFQs).contains(originalFQName); 
        case EXCEPTION :
          return getOrCreateSetForRevision(revision, deployedExceptionsFQs).contains(originalFQName);
        case WORKFLOW :
          return getOrCreateMapForRevision(revision, deployedProcessOriginalFQs).containsKey(originalFQName);
        //case FORM :
        default :
          return false;
      }
    } finally {
      lock.unlock();
    }
  }


  public HashMap<Long, List<String>> getDeployedWfs() {
    lock.lock();
    try {
      HashMap<Long, List<String>> result = new HashMap<Long, List<String>>();
      for(Entry<Long, BijectiveMap<String, String>> entry : deployedProcessOriginalFQs.entrySet()) {
        result.put(entry.getKey(), new ArrayList<String>(entry.getValue().keySet()));
      }
      return result;
    } finally {
      lock.unlock();
    }
  }
  
  public HashMap<Long, List<String>> getDeployedDatatypes() {
    lock.lock();
    try {
      HashMap<Long, List<String>> result = new HashMap<Long, List<String>>();
      for(Entry<Long, Set<String>> entry : deployedDatatypesFQs.entrySet()) {
        result.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
      }
      return result;
    } finally {
      lock.unlock();
    }
  }
  
  public HashMap<Long, List<String>> getDeployedExceptions() {
    lock.lock();
    try {
      HashMap<Long, List<String>> result = new HashMap<Long, List<String>>();
      for(Entry<Long, Set<String>> entry : deployedExceptionsFQs.entrySet()) {
        result.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
      }
      return result;
    } finally {
      lock.unlock();
    }
  }



  public Collection<String> getSavedWfs() {
    lock.lock();
    try {
      return new ArrayList<String>(getOrCreateSetForRevision(RevisionManagement.REVISION_DEFAULT_WORKSPACE,  savedProcessOriginalFQs));
    } finally {
      lock.unlock();
    }
  }


  /**
   * @return {@link HashMap}&lt;{@link String}, {@link DeploymentStatus}&gt; - A mapping from fqOriginalName to its deployment
   *         status
   */
  public Map<ApplicationEntryType, Map<String, DeploymentStatus>> getAllDeploymentStatuses(Long revision) {

    Map<ApplicationEntryType, Map<String, DeploymentStatus>> result = new HashMap<ApplicationEntryType, Map<String, DeploymentStatus>>();

    DeploymentStatus deployedStatus = DeploymentStatus.DEPLOYED;
    Boolean rmiClosed = RevisionOrderControl.getRmiCliClosed(revision, OrderEntranceType.RMI);
    if (rmiClosed != null && rmiClosed) {
      deployedStatus = DeploymentStatus.DEPLOYED_STOPPED;
    }
    
    lock.lock();
    try {
      Map<String, DeploymentStatus> map = new HashMap<String, WorkflowDatabase.DeploymentStatus>();
      // updating this from disk on every request is not a good idea since that is very slow
      // it is not expected that anyone writes to disk without using the UI, so the list should be up to date anyway
      // updateSavedProcessFQs();
      for (String s : getOrCreateSetForRevision(revision,  savedProcessOriginalFQs) ) {
        map.put(s, DeploymentStatus.SAVED);
      }
      
      for (String s : getOrCreateMapForRevision(revision, deployedProcessOriginalFQs).keySet()) {
        map.put(s, deployedStatus);
      }
      result.put(ApplicationEntryType.WORKFLOW, map);
      
      map = new HashMap<String, WorkflowDatabase.DeploymentStatus>();   
      for (String s : getOrCreateSetForRevision(revision, deployedDatatypesFQs)) {
        map.put(s, deployedStatus);
      }
      result.put(ApplicationEntryType.DATATYPE, map);
      
      map = new HashMap<String, WorkflowDatabase.DeploymentStatus>();   
      for (String s : getOrCreateSetForRevision(revision, deployedExceptionsFQs)) {
        map.put(s, deployedStatus);
      }
      result.put(ApplicationEntryType.EXCEPTION, map);
    } finally {
      lock.unlock();
    }

    return result;

  }

  /**
   * Liefert DeplyomentStatuses f�r Default-Workspace
   * @return
   */
  public Map<ApplicationEntryType, Map<String, DeploymentStatus>> getAllDeploymentStatuses() {
    return getAllDeploymentStatuses(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  
  public List<WorkflowInformation> listWorkflows() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    List<WorkflowInformation> allWorkflows = new ArrayList<WorkflowInformation>();
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getRevisionManagement();
    for (Long revision : revisionManagement.getAllRevisions()) {
      RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(revision);
      
      Map<String, DeploymentStatus> deploymentStatuses = getAllDeploymentStatuses(revision).get(ApplicationEntryType.WORKFLOW);
      for (String fqClassName : deploymentStatuses.keySet()) {
        allWorkflows.add(new WorkflowInformation(fqClassName, runtimeContext, deploymentStatuses.get(fqClassName)));
      }
    }
    
    return allWorkflows;
  }
  
  
  /**
   * Liefert den xmlName zum fqClassName f�r einen deployten Workflow
   * @param fqClassName
   * @param revision
   * @return
   */
  public String getXmlName(String fqClassName, Long revision) {
    if (deployedProcessOriginalFQs == null || deployedProcessOriginalFQs.get(revision) == null) {
      return null;
    }
    
    return deployedProcessOriginalFQs.get(revision).getInverse(fqClassName);
  }
  
  
  public void updateSavedProcessFQs() {

    lock.lock();
    try {
      savedProcessOriginalFQs.clear();
      
      RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getRevisionManagement();
      for (Long revision : revisionManagement.getAllWorkspaceRevisions()) {
        String savedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, revision, false);
        File baseDir = new File(savedMdmDir);
        if (!baseDir.exists()) {
          //Workspaces die nur noch f�r Audits vorhanden sind, haben kein saved-Verzeichnis mehr
          continue;
        }
        ArrayList<File> files = getMDMFiles(baseDir, new ArrayList<File>());
        for (File file : files) {
          try {

            String savedLocation = file.getPath();

            Document d = XMLUtils.parse(savedLocation);
            Element rootElement = d.getDocumentElement();
            if (rootElement.getTagName().equals(EL.SERVICE)) {
              String packagename = rootElement.getAttribute(GenerationBase.ATT.TYPEPATH);
              String classname = rootElement.getAttribute(GenerationBase.ATT.TYPENAME);
              if (packagename != null && classname != null) {
                String name = packagename + "." + classname;
                if (!getOrCreateMapForRevision(revision, deployedProcessOriginalFQs).containsKey(name))
                  getOrCreateSetForRevision(revision, savedProcessOriginalFQs).add(name);
              }
            }

          } catch (XynaException e) {
            logger.info("Found invalid XML while getting information on saved workflows");
          }
        }
      }
    } finally {
      lock.unlock();
    }

  }


  public static ArrayList<File> getMDMFiles(File basedir, ArrayList<File> list) {
    File[] files = basedir.listFiles();
    if (files == null) {
      throw new RuntimeException("directory " + basedir + " not found");
    }
    for (File f : files) {
      if (f.isDirectory()) {
        getMDMFiles(f, list);
      } else {
        if (f.getName().endsWith(".xml")) {
          list.add(f);
        }
      }
    }
    return list;
  }

  public void persistDeployedObjects() throws PersistenceLayerException {
    lock.lock();
    try {
      persistDeployedProcessOriginalFQs();
      persistDeployedDatatypesAndExceptions();
    } finally {
      lock.unlock();
    }
  }

  private void persistDeployedDatatypesAndExceptions() throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();

        
    List<DeployedDatatypesAndExceptionsStorable> persListDatatypesExceptions = new ArrayList<DeployedDatatypesAndExceptionsStorable>();
    for (Entry<Long, Set<String>> entry : deployedExceptionsFQs.entrySet()) {
      for(String string : entry.getValue()) {
        persListDatatypesExceptions.add(DeployedDatatypesAndExceptionsStorable.exception(string, entry.getKey()));
      }
    }
    for (Entry<Long, Set<String>> entry : deployedDatatypesFQs.entrySet()) {
      for(String string : entry.getValue()) {
        persListDatatypesExceptions.add(DeployedDatatypesAndExceptionsStorable.datatype(string, entry.getKey()));
      }
    }

    Connection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<DeployedDatatypesAndExceptionsStorable> previousListDatatypesExceptions =
          con.loadCollection(DeployedDatatypesAndExceptionsStorable.class);

      List<DeployedDatatypesAndExceptionsStorable> toBeDeleted =
          new ArrayList<WorkflowDatabase.DeployedDatatypesAndExceptionsStorable>();
      for (DeployedDatatypesAndExceptionsStorable prevEntry : previousListDatatypesExceptions) {
        if (prevEntry.isDatatype()) {
          if (!getOrCreateSetForRevision(prevEntry.getRevision(), deployedDatatypesFQs).contains(prevEntry.getFqName())) {
            toBeDeleted.add(prevEntry);
          }
        } else if (prevEntry.isException()) {
          if (!getOrCreateSetForRevision(prevEntry.getRevision(), deployedExceptionsFQs)
              .contains(prevEntry.getFqName())) {
            toBeDeleted.add(prevEntry);
          }
        }
      }
      con.delete(toBeDeleted);

      con.persistCollection(persListDatatypesExceptions);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }
  
  
  private void persistDeployedProcessOriginalFQs() throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();

    List<DeployedProcessOriginalFQsStorable> persList = new ArrayList<DeployedProcessOriginalFQsStorable>();
    for (Entry<Long, BijectiveMap<String, String>> entry : deployedProcessOriginalFQs.entrySet()) {
      for(String string : entry.getValue().keySet()) {
        persList.add(new DeployedProcessOriginalFQsStorable(string, entry.getKey()));
      }
    }

    Connection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<DeployedProcessOriginalFQsStorable> previousList =
          con.loadCollection(DeployedProcessOriginalFQsStorable.class);

      List<DeployedProcessOriginalFQsStorable> toBeDeleted = new ArrayList<DeployedProcessOriginalFQsStorable>();
      for (DeployedProcessOriginalFQsStorable dpofq : previousList) {
        if (!getOrCreateMapForRevision(dpofq.getRevision(), deployedProcessOriginalFQs)
            .containsKey(dpofq.getDeployedProcessOriginalFQ())) {
          toBeDeleted.add(dpofq);
        }
      }
      if (toBeDeleted.size() > 0) {
        con.delete(toBeDeleted);
      }
      
      con.persistCollection(persList);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  private void loadDeployedProcessOriginalFQs() throws PersistenceLayerException {

    ODS ods = ODSImpl.getInstance();
    Collection<DeployedProcessOriginalFQsStorable> persList;
    Collection<DeployedDatatypesAndExceptionsStorable> persListDatatypesExceptions;

    Connection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      persList = con.loadCollection(DeployedProcessOriginalFQsStorable.class);
      persListDatatypesExceptions = con.loadCollection(DeployedDatatypesAndExceptionsStorable.class);
    } finally {
      con.closeConnection();
    }

    deployedProcessOriginalFQs.clear();
    for (DeployedProcessOriginalFQsStorable storable : persList) {
      String fqClassName = null;
      try {
        fqClassName = GenerationBase.transformNameForJava(storable.getDeployedProcessOriginalFQ());
      } catch (XPRC_InvalidPackageNameException e) {
        // sollte eigentlich nicht vorkommen, da der Workflow schon einmal erfolgreich deployed wurde,
        // wenn er im DeployedProcessOriginalFQsStorable gefunden wird
        logger.warn("Could not get fqClassName for '" + storable.getDeployedProcessOriginalFQ() + "'", e);
      }
      getOrCreateMapForRevision(storable.getRevision(), deployedProcessOriginalFQs).put(storable.getDeployedProcessOriginalFQ(), fqClassName);
    }
    
    refreshDeployedDatatypesAndExceptions(persListDatatypesExceptions);
  }
  
  
  // called via reflection from UpdateInitDeployedDatatypesAndExceptions
  private void refreshDeployedDatatypesAndExceptions(Collection<DeployedDatatypesAndExceptionsStorable> persListDatatypesExceptions) {
    deployedDatatypesFQs.clear();
    deployedExceptionsFQs.clear();
    for (DeployedDatatypesAndExceptionsStorable storable : persListDatatypesExceptions) {
      if(storable.isDatatype()) {
        getOrCreateSetForRevision(storable.getRevision(), deployedDatatypesFQs).add(storable.getFqName());
      } else if(storable.isException()) {
        getOrCreateSetForRevision(storable.getRevision(), deployedExceptionsFQs).add(storable.getFqName());
      }
    }
  }
  
  
  @Persistable(primaryKey = DeployedProcessOriginalFQsStorable.ID, tableName = DeployedProcessOriginalFQsStorable.TABLENAME)
  public static class DeployedProcessOriginalFQsStorable extends Storable<DeployedProcessOriginalFQsStorable> {

    private static final long serialVersionUID = 7738061005776931544L;

    public final static String ID = "id";
    public final static String TABLENAME = "deployedWorkflows";
    public final static String FQNAME = "deployedProcessOriginalFQ";
    public final static String REVISION = "revision";

    @Column(name = ID)
    private String id;
    
    @Column(name = REVISION)
    private Long revision;
    
    @Column(name = FQNAME)
    private String deployedProcessOriginalFQ;


    public DeployedProcessOriginalFQsStorable() {
    }


    public DeployedProcessOriginalFQsStorable(String deployedProcessOriginalFQ, Long revision) {
      this.deployedProcessOriginalFQ = deployedProcessOriginalFQ;
      this.revision = revision;
      this.id = deployedProcessOriginalFQ + "#" + revision;
    }


    public String getDeployedProcessOriginalFQ() {
      return deployedProcessOriginalFQ;
    }

    public Long getRevision() {
      return revision;
    }

    public void setDeployedProcessOriginalFQ(String deployedProcessOriginalFQ) {
      this.deployedProcessOriginalFQ = deployedProcessOriginalFQ;
    }


    @Override
    public ResultSetReader<? extends DeployedProcessOriginalFQsStorable> getReader() {
      return new ResultSetReader<DeployedProcessOriginalFQsStorable>() {

        public DeployedProcessOriginalFQsStorable read(ResultSet rs) throws SQLException {
          DeployedProcessOriginalFQsStorable result = new DeployedProcessOriginalFQsStorable();
          result.id = rs.getString(ID);
          result.deployedProcessOriginalFQ = rs.getString(FQNAME);
          result.revision = rs.getLong(REVISION);
          return result;
        }
      };
    }


    @Override
    public Object getPrimaryKey() {
      return id;
    }

    
    public String getId() {
      return id;
    }


    @Override
    public <U extends DeployedProcessOriginalFQsStorable> void setAllFieldsFromData(U data) {
      DeployedProcessOriginalFQsStorable cast = data;
      deployedProcessOriginalFQ = cast.deployedProcessOriginalFQ;
      id = cast.id;
      revision = cast.revision;
    }
    
  }
  
  @Persistable(primaryKey = DeployedDatatypesAndExceptionsStorable.ID, tableName = DeployedDatatypesAndExceptionsStorable.TABLENAME)
  public static class DeployedDatatypesAndExceptionsStorable extends Storable<DeployedDatatypesAndExceptionsStorable> {

    private static final long serialVersionUID = 7738061005776931544L;
    
    private static final String TYPE_EXCETPION = "exception";
    private static final String TYPE_DATATYPE = "datatype";

    public final static String ID = "id";
    public final static String TABLENAME = "deployedDatatypesAndExceptions";
    public final static String FQNAME = "fqName";
    public final static String REVISION = "revision";
    public final static String TYPE = "type";

    @Column(name = ID)
    private String id;
    
    @Column(name = REVISION)
    private Long revision;
    
    @Column(name = FQNAME)
    private String fqName;
    
    @Column(name = TYPE)
    private String type;


    public DeployedDatatypesAndExceptionsStorable() {
    }


    public static DeployedDatatypesAndExceptionsStorable datatype(String fqName, Long revision) {
      DeployedDatatypesAndExceptionsStorable dd = new DeployedDatatypesAndExceptionsStorable();
      dd.fqName = fqName;
      dd.revision = revision;
      dd.id = fqName + "#" + revision;
      dd.type = TYPE_DATATYPE;
      return dd;
    }
    
    public static DeployedDatatypesAndExceptionsStorable exception(String fqName, Long revision) {
      DeployedDatatypesAndExceptionsStorable dd = new DeployedDatatypesAndExceptionsStorable();
      dd.fqName = fqName;
      dd.revision = revision;
      dd.id = fqName + "#" + revision;
      dd.type = TYPE_EXCETPION;
      return dd;
    }


    public String getFqName() {
      return fqName;
    }

    public Long getRevision() {
      return revision;
    }

    public String getType() {
      return type;
    }


    @Override
    public ResultSetReader<? extends DeployedDatatypesAndExceptionsStorable> getReader() {
      return new ResultSetReader<DeployedDatatypesAndExceptionsStorable>() {

        public DeployedDatatypesAndExceptionsStorable read(ResultSet rs) throws SQLException {
          DeployedDatatypesAndExceptionsStorable result = new DeployedDatatypesAndExceptionsStorable();
          result.id = rs.getString(ID);
          result.fqName = rs.getString(FQNAME);
          result.revision = rs.getLong(REVISION);
          result.type = rs.getString(TYPE);
          return result;
        }
      };
    }


    @Override
    public Object getPrimaryKey() {
      return id;
    }

    
    public String getId() {
      return id;
    }


    @Override
    public <U extends DeployedDatatypesAndExceptionsStorable> void setAllFieldsFromData(U data) {
      DeployedDatatypesAndExceptionsStorable cast = data;
      fqName = cast.fqName;
      id = cast.id;
      revision = cast.revision;
      type = cast.type;
    }
    
    
    public boolean isException() {
      return TYPE_EXCETPION.equals(type);
    }
    
    public boolean isDatatype() {
      return TYPE_DATATYPE.equals(type);
    }
    
  }
  
}
