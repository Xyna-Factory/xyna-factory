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
package com.gip.xyna.xfmg.xfctrl.xmomdatabase;



import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport.ValueProcessor;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.concurrent.DistributedWorkWithTasks;
import com.gip.xyna.utils.concurrent.DistributedWorkWithTasks.Task;
import com.gip.xyna.utils.concurrent.FutureCollection;
import com.gip.xyna.utils.concurrent.HashParallelReentrantLock;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMCommonValueParser.CommonValues;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerInstanceIdUnknownException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.XynaExecutor;
import com.gip.xyna.xprc.XynaRunnable;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;



public class XMOMDatabase extends FunctionGroup {

  
  public final static String DEFAULT_NAME = "XMOMDatabase";
  private final static Logger logger = CentralFactoryLogging.getLogger(XMOMDatabase.class);
  private static final PreparedQueryCache cache = new PreparedQueryCache();
  
  //Wert auf den die XynaProperty 'xyna.xfmg.xfctrl.xmomdatabase.discovery_on_startup' nach erfolgreichem shutdown gesetzt wird
  private boolean discoveryOnStartupValueAfterSchutdown = true;
  
  public static enum XMOMType {
    DATATYPE(EL.DATATYPE, ApplicationEntryType.DATATYPE,  "DataType"),
    WORKFLOW(EL.SERVICE, ApplicationEntryType.WORKFLOW, "Workflow"),
    EXCEPTION(EL.EXCEPTIONSTORAGE, ApplicationEntryType.EXCEPTION, "ExceptionType"),
    FORM(EL.FORMDEFINITION, ApplicationEntryType.FORMDEFINITION, "FormDefinition"),
    ORDERINPUTSOURCE(EL.ORDER_INPUT_SOURCE, ApplicationEntryType.ORDERINPUTSOURCE, "OrderInputSource");

    private final String xmlTag;
    private final String niceName;
    private final ApplicationEntryType appEntryType;
    private static Map<String, XMOMType> mapByName = new HashMap<String, XMOMDatabase.XMOMType>();
    private static Map<String, XMOMType> mapByString = new HashMap<String, XMOMDatabase.XMOMType>();
    static {
      for (XMOMType type: values()) {
        mapByName.put(type.xmlTag, type);
        mapByString.put(type.toString().toLowerCase(), type);
      }
    }


    private XMOMType(String xmlTag, ApplicationEntryType appEntryType, String niceName) {
      this.xmlTag = xmlTag;
      this.niceName = niceName;
      this.appEntryType = appEntryType;
    }


    public static XMOMType getXMOMTypeByRootTag(String rootTag) {
      return mapByName.get(rootTag);
    }
    
    public static XMOMType getXMOMTypeByString(String stringRepresentation) {
      XMOMType result = mapByString.get(stringRepresentation.toLowerCase());
      if (result == null) {
        throw new IllegalArgumentException("Unknown XMOMType: " + stringRepresentation);
      }
      return result;
    }
    
    
    public ApplicationEntryType getApplicationEntryRepresentation() {
      return appEntryType;
    }
    
    
    public static XMOMType deriveXMOMType(ApplicationEntryType aet) {
      for (XMOMType type : values()) {
        if (type.appEntryType == aet) {
          return type;
        }
      }
      throw new IllegalArgumentException("XMOMType not deriveable from " + aet.toString());
    }
    
    
    public static XMOMType getXMOMTypeByGenerationInstance(GenerationBase gb) {
      if (gb instanceof DOM) {
        return XMOMType.DATATYPE;
      } else if (gb instanceof ExceptionGeneration) {
        return XMOMType.EXCEPTION;
      } else if (gb instanceof WF) {
        return XMOMType.WORKFLOW;
      } else {
        throw new IllegalArgumentException("Unidentified GenerationBase type " + gb.getClass().getName()); 
      }
    }
    
    public String getNiceName() {
      return niceName;
    }
    
    public String getRootTag() {
      return xmlTag;
    }

    public static XMOMType getXMOMTypeByDependencySourceType(DependencySourceType type) {
      switch (type) {
        case DATATYPE :
          return DATATYPE;
        case XYNAEXCEPTION :
          return EXCEPTION;
        case WORKFLOW :
          return WORKFLOW;
        default :
          throw new IllegalArgumentException(type + " is not a XMOMType");
      }
    }


    public static XMOMType getXMOMTypeByFile(File file) {
      try (FileInputStream fis = new FileInputStream(file)) {
        return XMOMType.getXMOMTypeByRootTag(XMLUtils.getRootElementName(fis));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }


  public static String getFqOriginalNameFromFqServiceOperationName(String fqServiceOperationName) {
    int lastDotIndex = fqServiceOperationName.lastIndexOf(".");
    if (lastDotIndex < 0) {
      throw new IllegalArgumentException("'" + fqServiceOperationName + "' is not a valid service operation name");
    }
    String fqServiceName = fqServiceOperationName.substring(0, lastDotIndex);
    lastDotIndex = fqServiceName.lastIndexOf(".");
    if (lastDotIndex < 0) {
      throw new IllegalArgumentException("'" + fqServiceOperationName + "' is not a valid service operation name");
    }
    return fqServiceName.substring(0, lastDotIndex);
  }

  
  public static String getFqOriginalNameFromFqServiceName(String fqServiceName) {
    int lastDotIndex = fqServiceName.lastIndexOf(".");
    if (lastDotIndex < 0) {
      throw new IllegalArgumentException("'" + fqServiceName + "' is not a valid service name");
    }
    return fqServiceName.substring(0, lastDotIndex);
  }


  private static enum StorageAction {
    create,  //neuen Entry anlegen
    update,  //alten Entry updaten
    discard; //aktuell betrachteten Entry verwerfen, da vorhandener Entry neuer ist
  }
  
  public static enum XMOMState {
    missing_xml(-1L),
    missing_xml_but_backward_relations(0L);
    
    Long timestamp;

    private XMOMState(Long timestamp) {
      this.timestamp = timestamp;
    }
    
    
    public Long getTimestamp() {
      return timestamp;
    }
  }
  
  private final static Comparator<XMOMDatabaseSearchResultEntry> improvedSearchResultComparator = new Comparator<XMOMDatabaseSearchResultEntry>() {

    public int compare(XMOMDatabaseSearchResultEntry o1, XMOMDatabaseSearchResultEntry o2) {
      if (o1.getWeigth() == o2.getWeigth()) { //weights first
        if (o1.getSimplename() == null && o2.getSimplename() == null) {
          return o1.getFqName().compareTo(o2.getFqName());
        }
        if(o1.getSimplename() == null) {
          return 1;
        }
        if(o2.getSimplename() == null) {
          return -1;
        }
        if (o1.getSimplename().equals(o2.getSimplename())) { //simplenames second
          return compareStringsNotFavoringLongStrings2(o1.getSimplepath(), o2.getSimplepath()); //path has to differ then! /pout
        } else {
          return compareStringsNotFavoringLongStrings2(o1.getSimplename(), o2.getSimplename());
        }
      } else if (o1.getWeigth() > o2.getWeigth()) {
        return -1;
      } else {
        return 1;
      }
    }
  };
  
  
  // ab == a -> ac == a -> ab != ac : this might violate the comperator contract
  private static final int compareStringsNotFavoringLongStrings(String first, String second) {
    if (first == null && second == null) {
      return 0;
    }
    if(first == null) {
      return 1;
    }
    if(second == null) {
      return -1;
    }
    if (first.length() == second.length()) {
      return first.compareTo(second);
    } else {
      int shortLength = Math.min(first.length(), second.length());
      return first.substring(0, shortLength).compareToIgnoreCase(second.substring(0, shortLength));
    }
  }
  
  
  private static final int compareStringsNotFavoringLongStrings2(String first, String second) {
    if (first == null && second == null) {
      return 0;
    }
    if(first == null) {
      return 1;
    }
    if(second == null) {
      return -1;
    }
    return -first.compareTo(second);
  }
  
  public static final List<XMOMDatabaseSearchResultEntry> sortResultList(List<XMOMDatabaseSearchResultEntry> unsortedResults) {
      Collections.sort(unsortedResults, improvedSearchResultComparator);
    return unsortedResults;    
  }


  private ODS ods;
  private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
  private ThreadPoolExecutor searchRequestPool = new ThreadPoolExecutor(5, 50, 300, TimeUnit.SECONDS,
                                                                        new LinkedBlockingQueue<Runnable>(), new ArchiveRequestThreadFactory());
  
  
  

  public XMOMDatabase() throws XynaException {
  }
  
  public XMOMDatabase(String cause) throws XynaException {
    super(cause);
  }

  
  public static XMOMDatabase getXMOMDatabasePreInit(ODS ods, String cause) throws XynaException {
    if (!XynaFactory.getInstance().isStartingUp()) {
      throw new XynaException("PreInitXMOMDatabase can only be retrieved during Factory startup.");
    }
    XMOMDatabase xmomDatabase = new XMOMDatabase(cause);
    xmomDatabase.ods = ods;
    ods.registerStorable(XMOMDomDatabaseEntry.class);
    ods.registerStorable(XMOMExceptionDatabaseEntry.class);
    ods.registerStorable(XMOMOperationDatabaseEntry.class);
    ods.registerStorable(XMOMWorkflowDatabaseEntry.class);
    ods.registerStorable(XMOMServiceGroupDatabaseEntry.class);
    ods.registerStorable(XMOMDataModelDatabaseEntry.class);
    ods.registerStorable(XMOMFormDefinitionDatabaseEntry.class);
    return xmomDatabase;
  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  protected static class XMOMObjectSet extends ObjectWithRemovalSupport {
    
    //das Pair setzt sich aus dem originalFqName und dem rootType zusammen;
    //Boolean hat keine Bedeutung (es gibt kein ConcurrentSet)
    private final ConcurrentHashMap<Pair<String,String>, Boolean> xmomObjects = new ConcurrentHashMap<Pair<String,String>, Boolean>();
    
    @Override
    protected boolean shouldBeDeleted() {
      return xmomObjects.isEmpty();
    }
    
    public void add(String originalFqName, String rootTag) {
      xmomObjects.put(Pair.of(originalFqName, rootTag), true);
    }
    
    public void remove(String originalFqName, String rootTag) {
      xmomObjects.remove(Pair.of(originalFqName, rootTag));
    }
    
    protected boolean isEmpty() {
      return xmomObjects.isEmpty();
    }
    
    public Set<Pair<String, String>> getXMOMObjects() {
      return xmomObjects.keySet();
    }
  }


  @Override
  protected void init() throws XynaException {

    ods = ODSImpl.getInstance();

    List<Task<XynaException>> tasks = new ArrayList<Task<XynaException>>();
    tasks.add(createInitTask(XMOMDomDatabaseEntry.class, XMOMDomDatabaseEntry.TABLENAME));
    tasks.add(createInitTask(XMOMExceptionDatabaseEntry.class, XMOMExceptionDatabaseEntry.TABLENAME));
    tasks.add(createInitTask(XMOMOperationDatabaseEntry.class, XMOMOperationDatabaseEntry.TABLENAME));
    tasks.add(createInitTask(XMOMWorkflowDatabaseEntry.class, XMOMWorkflowDatabaseEntry.TABLENAME));
    tasks.add(createInitTask(XMOMServiceGroupDatabaseEntry.class, XMOMServiceGroupDatabaseEntry.TABLENAME));
    tasks.add(createInitTask(XMOMDataModelDatabaseEntry.class, XMOMDataModelDatabaseEntry.TABLENAME));
    tasks.add(createInitTask(XMOMFormDefinitionDatabaseEntry.class, XMOMFormDefinitionDatabaseEntry.TABLENAME));
    executeTasks(tasks, 4);

    XynaProperty.XMOMDISCOVERY_ON_STARTUP.registerDependency(DEFAULT_NAME);

    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    //Achtung: Diese Deploymenthandler müssen beim Serverstart nicht registriert sein bevor die WFDatabase geladen wird,       
    //         weil die Objekte alle vorher irgendwann manuell deployed wurden und deshalb keine Informationen verloren gehen.
    //         Deshalb wären die Deploymenthandler beim Serverstart-Deployment sogar redundant und schlecht für die Performance.
    //Deshalb ist hier eine Abhängigkeit auf WFDatabase ok.
    fExec.addTask(XMOMDatabase.class, "XMOMDatabase").
         after(WorkflowDatabase.FUTURE_EXECUTION_ID).
         after(XynaProperty.class).
         execAsync(new Runnable() { public void run() {
           XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
               .addDeploymentHandler(DeploymentHandling.PRIORITY_WORKFLOW_DATABASE, new DeploymentHandler() {

                 //zum GenerationBase-Objekt in Beziehung stehende XMOMObjekte, die nicht in der XMOMDatabase gefunden werden,
                 //sammeln und im finish registrieren
                 ConcurrentMapWithObjectRemovalSupport<Long, XMOMObjectSet> xmomObjectsToFinishLater = new ConcurrentMapWithObjectRemovalSupport<Long, XMOMObjectSet>() {

                   private static final long serialVersionUID = 1L;

                   @Override
                   public XMOMObjectSet createValue(Long key) {
                     return new XMOMObjectSet();
                   }
                 };
                 
                 //das registrieren der XMOMObjekte soll immer nur ein Thread pro Revision machen, da ein Cache für die GenerationBase-Objekte verwendet wird
                 HashParallelReentrantLock<Long> finishLock = new HashParallelReentrantLock<Long>(5);

                 public void exec(final GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
                   if (mode.shouldCopyXMLFromSavedToDeployed() || mode == DeploymentMode.codeUnchanged
                                    || mode == DeploymentMode.reloadWithXMOMDatabaseUpdate) {
                     xmomObjectsToFinishLater.process(object.getRevision(), new ValueProcessor<XMOMObjectSet, Boolean>() {

                       public Boolean exec(XMOMObjectSet finishLater) {
                         finishLater.add(object.getOriginalFqName(), XMOMType.getXMOMTypeByGenerationInstance(object).getRootTag());
                         return true;
                       }
                     });
                   }
                 }

                 public void finish(boolean success) throws XPRC_DeploymentHandlerException {
                   final GenerationBaseCache gbcache = new GenerationBaseCache();
                   for (final Long revision : xmomObjectsToFinishLater.keySet()) {
                     finishLock.lock(revision);
                     try {
                       xmomObjectsToFinishLater.process(revision, new ValueProcessor<XMOMObjectSet, Boolean>() {

                         public Boolean exec(XMOMObjectSet xmomObjectsToRegister) {
                           try {
                             //alle eingesammelten Objekte nun registrieren
                             registerMOMObjects(xmomObjectsToRegister, revision, gbcache);
                           } catch (PersistenceLayerException ple) {
                             logger.error("Unable to register xmom objects", ple);
                           } catch (AssumedDeadlockException ade) {
                             logger.error("Unable to register xmom objects", ade);
                           }

                           return true;
                         }
                       });
                     } finally {
                       finishLock.unlock(revision);
                     }
                   }
                 }

                @Override
                public void begin() throws XPRC_DeploymentHandlerException {
                }
               });
           
           
           if (XynaProperty.XMOMDISCOVERY_ON_STARTUP.get()) {
             //XMOMDiscovery ausführen
             XynaExecutor.getInstance(false)
               .executeRunnableWithUnprioritizedPlanningThreadpool(new XynaRunnable() {
                 
                 public void run() {
                   try {
                     discovery();
                     discoveryOnStartupValueAfterSchutdown = false; //Discovery war erfolgreich, daher muss es beim nächsten Server-Start nicht wiederholt werden
                   } catch (XynaException e) {
                     logger.warn("XMOM-Discovery failed, the command 'xmomdiscovery' can be used to manually trigger a discovery.",e);
                   }
                 }
               });
           } else {
             //beim nächsten Server-Start soll kein Discovery ausgeführt werden, falls das Shutdown durchgeführt wurde
             discoveryOnStartupValueAfterSchutdown = false;
           }
           
           try {
             XynaProperty.XMOMDISCOVERY_ON_STARTUP.set(true);
           } catch (PersistenceLayerException e) {
             logger.warn("Failed to set XynaProperty 'xyna.xfmg.xfctrl.xmomdatabase.discovery_on_startup'.",e);
           }
         }
       });
  }


  private Task<XynaException> createInitTask(final Class<? extends Storable<?>> clazz, final String tableName) {
    return new Task<XynaException>() {
      public void run() throws XynaException {
        ods.registerStorable(clazz);
      }
    };
  }


  @Override
  protected void shutdown() throws XynaException {
    XynaProperty.XMOMDISCOVERY_ON_STARTUP.set(discoveryOnStartupValueAfterSchutdown);
  }


  private void executeTasks(List<Task<XynaException>> tasks, int numberOfExtraThreads) throws XynaException {

    if (numberOfExtraThreads < 0) {
      numberOfExtraThreads = 0;
    } else if (numberOfExtraThreads > tasks.size() - 1) {
      numberOfExtraThreads = tasks.size();
    }

    // Execute all the tasks with some threads
    final DistributedWorkWithTasks<XynaException> workWithTasks = new DistributedWorkWithTasks<XynaException>(tasks);
    final ConcurrentLinkedQueue<XynaException> exceptions = new ConcurrentLinkedQueue<XynaException>();
    for (int i=0; i<numberOfExtraThreads; i++) {
      Runnable r = new Runnable() {
        public void run() {
          try {
            workWithTasks.executeAndWaitForCompletion();
          } catch (InterruptedException e) {
            // does not matter because the main thread is still working and will proceed with the shutdown.
            // the interrupt is still worth logging since it is not expected and may signal other issues
            logger.info("Got interrupted while shutting down XMOM database", e);
          } catch (XynaException e) {
            exceptions.add(e);
          }
        }
      };
      Thread t = new Thread(r);
      t.start();
    }
    try {
      workWithTasks.executeAndWaitForCompletion();
    } catch (InterruptedException e) {
      logger.warn("Got interrupted while shutting down XMOM database", e);
    }

    // Handle exceptions that occurred in other threads. If there is only one, throw it, if there are more, log all but the one that is finally thrown
    if (exceptions.size() > 0) {
      if (exceptions.size() == 1) {
        throw exceptions.poll();
      } else {
        XynaException e = null;
        while (exceptions.size() > 0) {
          e = exceptions.poll();
          if (exceptions.size() > 0) {
            logger.warn("Exception during XMOM database shutdown: " + e.getMessage(), e);
          }
        }
        if (e != null) {
          throw e;
        }
      }
    }

  }


  public void registerMOMObject(String originalFqName, Long revision) throws PersistenceLayerException, AssumedDeadlockException {
    registerMOMObject(originalFqName, revision, new GenerationBaseCache());
  }
  
  
  private void registerMOMObject(String originalFqName, Long revision, GenerationBaseCache generationBaseCache) throws PersistenceLayerException, AssumedDeadlockException {
    try { // There might be invalid saved xmls, they can't be parsed so we don't cache them
      registerMOMObject(originalFqName, GenerationBase.retrieveRootTag(originalFqName, revision, false, true), revision, generationBaseCache);
    } catch (Ex_FileAccessException e) {
      logger.debug("Error during registerMOMObject", e);
      return;
    } catch (XPRC_XmlParsingException e) {
      logger.debug("Error during registerMOMObject", e);
      return;
    }
  }

  
  public XMOMDatabaseEntry registerGenerationBaseObject(GenerationBase object, XMOMObjectSet xmomObjectsToFinishLater) throws PersistenceLayerException, AssumedDeadlockException {
    XMOMDatabaseEntry entry = null;
    String rootTag;
    if (object instanceof WF) {
      rootTag = XMOMType.WORKFLOW.getRootTag();
      entry = registerWorkflow((WF)object, xmomObjectsToFinishLater);
    } else if (object instanceof ExceptionGeneration) {
      rootTag = XMOMType.EXCEPTION.getRootTag();
      entry = registerException((ExceptionGeneration)object, xmomObjectsToFinishLater);
    } else if (object instanceof DOM) {
      rootTag = XMOMType.DATATYPE.getRootTag();
      entry = registerDatatypeAndServiceGroup((DOM)object, xmomObjectsToFinishLater);
    } else {
      logger.error("Trying to register GenerationBaseObject of unknown Type");
      return null;
    }
    
    xmomObjectsToFinishLater.remove(object.getOriginalFqName(), rootTag);
    return entry;
  }


  public void registerMOMObject(String originalFqName, String rootTag, Long revision) throws PersistenceLayerException,
                  AssumedDeadlockException {
    registerMOMObject(originalFqName, rootTag, revision, new GenerationBaseCache());
  }
  
  private void registerMOMObject(String originalFqName, String rootTag, Long revision, GenerationBaseCache generationBaseCache) throws PersistenceLayerException,
    AssumedDeadlockException {
    XMOMObjectSet xmomObjectsToRegister = new XMOMObjectSet();
    xmomObjectsToRegister.add(originalFqName, rootTag);
    
    registerMOMObjects(xmomObjectsToRegister, revision, generationBaseCache);
  }

  /**
   * Registriert die übergebenen XMOMObjects. Falls ein hierzu in Beziehung stehendes Objekt noch
   * nicht in der XMOMDatabase vorhanden ist, wird es ebenfalls vollständig registriert.
   */
  private void registerMOMObjects(XMOMObjectSet xmomObjectsToRegister, Long revision, GenerationBaseCache generationBaseCache) throws PersistenceLayerException, AssumedDeadlockException {
    while (!xmomObjectsToRegister.isEmpty()) {
      for (Pair<String,String> xmomObject : xmomObjectsToRegister.getXMOMObjects()) {
        String originalFqName = xmomObject.getFirst();
        XMOMType objectType = XMOMType.getXMOMTypeByRootTag(xmomObject.getSecond());
        switch (objectType) {
          case DATATYPE :
            registerDatatypeAndServiceGroup(originalFqName, revision, generationBaseCache, xmomObjectsToRegister);
            break;
          case EXCEPTION :
            registerException(originalFqName, revision, generationBaseCache, xmomObjectsToRegister);
            break;
          case WORKFLOW :
            registerWorkflow(originalFqName, revision, generationBaseCache, xmomObjectsToRegister);
            break;
          case FORM :
            registerForm(originalFqName, revision);
            break;
          default :
            throw new RuntimeException("unexpected type " + objectType);
        }
        xmomObjectsToRegister.remove(originalFqName, xmomObject.getSecond());
      }
    }
  }


  private void registerDatatypeAndServiceGroup(String originalFqName, Long revision, GenerationBaseCache generationBaseCache, XMOMObjectSet xmomObjectsToFinishLater) throws PersistenceLayerException,
                  AssumedDeadlockException {
    if (logger.isDebugEnabled()) {
      logger.debug("registerDatatypeAndOrServiceGroup: " + originalFqName + " (revision " + revision + ")");
    }
    DOM dom;
    try {
      dom = DOM.getOrCreateInstance(originalFqName, generationBaseCache, revision);
    } catch (XPRC_InvalidPackageNameException e) {
      logger.debug("Error during getOrCreateInstance on registerDatatypeAndServiceGroup", e);
      return;
    }
    
    try {
      dom.parseGeneration(false, false, false);
    } catch (XPRC_MDMDeploymentException e) {
      tryRegisterWithCommonValues(originalFqName, XMOMDatabaseType.DATATYPE, revision, dom.getParsingTimestamp());
      return;
    } catch (XPRC_InheritedConcurrentDeploymentException e) {
      logger.debug("Error during getOrCreateInstance on registerDatatypeAndServiceGroup", e);
      return;
    }

    registerDatatypeAndServiceGroup(dom, xmomObjectsToFinishLater);
  }


  private XMOMDatabaseEntry registerDatatypeAndServiceGroup(DOM dom, XMOMObjectSet xmomObjectsToFinishLater) throws PersistenceLayerException, AssumedDeadlockException {
    Map<String, XMOMOperationDatabaseEntry> previousOperations = new HashMap<String, XMOMOperationDatabaseEntry>();
    XMOMDatabaseEntry returnvalue = registerDatatype(dom, previousOperations, xmomObjectsToFinishLater);
    
    Map<String, List<Operation>> serviceToOperationMap = dom.getServiceNameToOperationMap();
    for (Entry<String, List<Operation>> entry : serviceToOperationMap.entrySet()) {
      String serviceName = entry.getKey();
      registerServiceGroup(dom, serviceName);
      List<Operation> operations = entry.getValue();
      if (operations != null) {
        registerOperations(dom, serviceName, operations, previousOperations, xmomObjectsToFinishLater);
      }
    }
    return returnvalue;
  }


  private XMOMDatabaseEntry registerDatatype(DOM dom, Map<String, XMOMOperationDatabaseEntry> previousOperations, XMOMObjectSet xmomObjectsToFinishLater) throws PersistenceLayerException, AssumedDeadlockException {
    if (logger.isDebugEnabled()) {
      logger.debug("registerDatatype: " + dom.getOriginalFqName() + " (revision " + dom.getRevision() + ")");
    }
    
    XMOMDomDatabaseEntry entry = new XMOMDomDatabaseEntry(dom);

    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      StorageAction action;
      try {
        action = examineExistingEntry(con, entry);
        if (action == StorageAction.discard) {
          return entry;
        }
      } finally {
        con.closeConnection();
      }


      if (action == StorageAction.update) {
        unregisterDatatype(dom, previousOperations);
      }


      con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.persistObject(entry);
        con.commit();

        RelationManagementMethods.insertPossessedByRelations.execute(con, entry, xmomObjectsToFinishLater);
        con.commit();
        RelationManagementMethods.insertExtendedByRelations.execute(con, entry, xmomObjectsToFinishLater);
        con.commit();
        RelationManagementMethods.insertInstanceServiceReferenceOfRelations.execute(con, entry, xmomObjectsToFinishLater);
        con.commit();
      } finally {
        con.closeConnection();
      }
    } finally {
      writeUnlockCache();
    }

    return entry;
  }


  private void registerException(String originalFqName, Long revision, GenerationBaseCache generationBaseCache, XMOMObjectSet xmomObjectsToFinishLater) throws AssumedDeadlockException, PersistenceLayerException {
    ExceptionGeneration excep;
    try {
      excep = ExceptionGeneration.getOrCreateInstance(originalFqName, generationBaseCache, revision);
    } catch (XPRC_InvalidPackageNameException e) {
      logger.debug("Error during getOrCreateInstance on registerException", e);
      return;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("registerException: " + originalFqName + " (revision " + revision + ")");
    }
    
    try {
      excep.parseGeneration(false, false, false);
      // There might be invalid saved xmls, they can't be parsed so we don't cache them
    } catch (XPRC_MDMDeploymentException e) {
      tryRegisterWithCommonValues(originalFqName, XMOMDatabaseType.EXCEPTION, revision, excep.getParsingTimestamp());
      return;
    } catch (XPRC_InheritedConcurrentDeploymentException e) {
      logger.debug("Error during getOrCreateInstance on registerException", e);
      return;
    }

    registerException(excep, xmomObjectsToFinishLater);
  }


  private XMOMDatabaseEntry registerException(ExceptionGeneration excep, XMOMObjectSet xmomObjectsToFinishLater) throws AssumedDeadlockException, PersistenceLayerException {
    XMOMExceptionDatabaseEntry entry = new XMOMExceptionDatabaseEntry(excep);

    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      StorageAction action;
      try {
        action = examineExistingEntry(con, entry);
        if (action == StorageAction.discard) {
          return entry;
        }
      } finally {
        con.closeConnection();
      }

      if (action == StorageAction.update) {
        unregisterException(excep);
      }

      con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.persistObject(entry);
        con.commit();

        RelationManagementMethods.insertPossessedByRelations.execute(con, entry, xmomObjectsToFinishLater);
        con.commit();
        RelationManagementMethods.insertExtendedByRelations.execute(con, entry, xmomObjectsToFinishLater);
        con.commit();
      } finally {
        con.closeConnection();
      }
    } finally {
      writeUnlockCache();
    }
    
    return entry;
  }


  private StorageAction examineExistingEntry(ODSConnection con, XMOMDatabaseEntry newEntry)
                  throws PersistenceLayerException {
    XMOMDatabaseEntry previousEntry;
    try {
      previousEntry = newEntry.clone();
      try {
        con.queryOneRow(previousEntry);
        
        if (previousEntry.getTimestamp() != null 
              && previousEntry.getTimestamp().compareTo(newEntry.getTimestamp()) >= 0) {
          //vorhandener Eintrag ist aktueller
          return StorageAction.discard;
        }
        
        //alte Backward-Relations in neuen Eintrag übernehmen
        retrievePreviousBackwardRelations(previousEntry, newEntry);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        //noch kein Eintrag vorhanden
        return StorageAction.create;
      }
    } catch (CloneNotSupportedException e1) {
      // is supported for all XMOMCacheEntries
    }
    
    return StorageAction.update;
  }

  
  private void retrievePreviousBackwardRelations(XMOMDatabaseEntry previousEntry, XMOMDatabaseEntry newEntry)
                  throws PersistenceLayerException {
    switch (newEntry.getXMOMDatabaseType()) {
      case DATATYPE :
        ((XMOMDomDatabaseEntry) newEntry).setExtendedBy(((XMOMDomDatabaseEntry) previousEntry).getExtendedBy());
        ((XMOMDomDatabaseEntry) newEntry).setPossessedBy(((XMOMDomDatabaseEntry) previousEntry).getPossessedBy());
        ((XMOMDomDatabaseEntry) newEntry).setNeededBy(((XMOMDomDatabaseEntry) previousEntry).getNeededBy());
        ((XMOMDomDatabaseEntry) newEntry).setProducedBy(((XMOMDomDatabaseEntry) previousEntry).getProducedBy());
        ((XMOMDomDatabaseEntry) newEntry).setInstancesUsedBy(((XMOMDomDatabaseEntry) previousEntry).getInstancesUsedBy());
        ((XMOMDomDatabaseEntry) newEntry).setUsedInImplOf(((XMOMDomDatabaseEntry) previousEntry).getUsedInImplOf());
        break;
      case EXCEPTION :
        ((XMOMExceptionDatabaseEntry) newEntry).setExtendedBy(((XMOMExceptionDatabaseEntry) previousEntry).getExtendedBy());
        ((XMOMExceptionDatabaseEntry) newEntry).setPossessedBy(((XMOMExceptionDatabaseEntry) previousEntry).getPossessedBy());
        ((XMOMExceptionDatabaseEntry) newEntry).setNeededBy(((XMOMExceptionDatabaseEntry) previousEntry).getNeededBy());
        ((XMOMExceptionDatabaseEntry) newEntry).setProducedBy(((XMOMExceptionDatabaseEntry) previousEntry).getProducedBy());
        ((XMOMExceptionDatabaseEntry) newEntry).setThrownBy(((XMOMExceptionDatabaseEntry) previousEntry).getThrownBy());
        ((XMOMExceptionDatabaseEntry) newEntry).setInstancesUsedBy(((XMOMExceptionDatabaseEntry) previousEntry).getInstancesUsedBy());
        ((XMOMExceptionDatabaseEntry) newEntry).setUsedInImplOf(((XMOMExceptionDatabaseEntry) previousEntry).getUsedInImplOf());
        break;
      case WORKFLOW:
        ((XMOMWorkflowDatabaseEntry) newEntry).setInstanceServiceReferenceOf(((XMOMWorkflowDatabaseEntry) previousEntry).getInstanceServiceReferenceOf());
        // fall through
      case OPERATION:
      case SERVICE :
        ((XMOMServiceDatabaseEntry) newEntry).setCalledBy(((XMOMServiceDatabaseEntry) previousEntry).getCalledBy());
        ((XMOMServiceDatabaseEntry) newEntry).setGroupedBy(((XMOMServiceDatabaseEntry) previousEntry).getGroupedBy());
        break;
      default :
        break;
    }
  }



  private void registerServiceGroup(DOM dom, String serviceName) throws PersistenceLayerException {
    if (logger.isDebugEnabled()) {
      logger.debug("registerServiceGroup: " + dom.getOriginalFqName());
    }
    XMOMServiceGroupDatabaseEntry entry = new XMOMServiceGroupDatabaseEntry(dom, serviceName);

    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        StorageAction action = examineExistingEntry(con, entry);
        if (action == StorageAction.discard) {
          return;
        }
        
        con.persistObject(entry);
        con.commit();
      } finally {
        con.closeConnection();
      }
    } finally {
      writeUnlockCache();
    }
  }


  private void registerOperations(DOM dom, String serviceName, Collection<Operation> operations, Map<String, XMOMOperationDatabaseEntry> previousEntries, XMOMObjectSet xmomObjectsToFinishLater)
                  throws PersistenceLayerException, AssumedDeadlockException {
    if (logger.isDebugEnabled()) {
      logger.debug("registerOperations: " + dom.getOriginalFqName());
    }
    List<XMOMOperationDatabaseEntry> entries = new ArrayList<XMOMOperationDatabaseEntry>();
    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        for (Operation operation : operations) {
          if (logger.isDebugEnabled()) {
            logger.debug("operation: " + operation.getName());
          }
          XMOMOperationDatabaseEntry entry = new XMOMOperationDatabaseEntry(dom, serviceName, operation);
          
          StorageAction action = examineExistingEntry(con, entry);
          if (action == StorageAction.discard) {
            continue;
          }
          
          if (action == StorageAction.create) {
            XMOMOperationDatabaseEntry previousEntry = previousEntries.get(entry.getId());
            if (previousEntry != null) {
              //Entry wurde durch registerDatatypeAndServiceGroup entfernt -> alte Rückwärtsbeziehungen müssen erhalten bleiben
              retrievePreviousBackwardRelations(previousEntry, entry);
            }
          }
          entry.extendInstantiations(con);
          entries.add(entry);
        }
      } finally {
        con.closeConnection();
      }
    } finally {
      writeUnlockCache();
    }

    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.persistCollection(entries);
        con.commit();

        for (XMOMOperationDatabaseEntry xmomOperationCacheEntry : entries) {
          RelationManagementMethods.insertNeededByRelations.execute(con, xmomOperationCacheEntry, xmomObjectsToFinishLater);
          con.commit();
          RelationManagementMethods.insertProducedByRelations.execute(con, xmomOperationCacheEntry, xmomObjectsToFinishLater);
          con.commit();
          RelationManagementMethods.insertThrownByRelations.execute(con, xmomOperationCacheEntry, xmomObjectsToFinishLater);
          con.commit();
          RelationManagementMethods.insertInstancesUsedByRelations.execute(con, xmomOperationCacheEntry, xmomObjectsToFinishLater);
          con.commit();
        }
      } finally {
        con.closeConnection();
      }
    } finally {
      writeUnlockCache();
    }
  }


  private void registerWorkflow(String originalFqName, Long revision, GenerationBaseCache generationBaseCache, XMOMObjectSet xmomObjectsToFinishLater) throws PersistenceLayerException, AssumedDeadlockException {
    if (logger.isDebugEnabled()) {
      logger.debug("registerWorkflow: " + originalFqName);
    }
    WF wf;
    try {
      wf = WF.getOrCreateInstance(originalFqName, generationBaseCache, revision);
    } catch (XPRC_InvalidPackageNameException e) {
      logger.debug("Error during getOrCreateInstance on registerWorkflow", e);
      return;
    }
    
    try {
      wf.parseGeneration(false, false, false);
      // There might be invalid saved xmls, they can't be parsed so we don't cache them
    } catch (XPRC_MDMDeploymentException e) {
      tryRegisterWithCommonValues(originalFqName, XMOMDatabaseType.WORKFLOW, revision, wf.getParsingTimestamp());
      return;
    } catch (XPRC_InheritedConcurrentDeploymentException e) {
      logger.debug("Error during getOrCreateInstance on registerWorkflow", e);
      return;
    } catch (RuntimeException e) {
      tryRegisterWithCommonValues(originalFqName, XMOMDatabaseType.WORKFLOW, revision, wf.getParsingTimestamp());
      logger.debug("Error during parseGeneration on registerWorkflow, registered with common values", e);
      return;
    }
    registerWorkflow(wf, xmomObjectsToFinishLater);
  }


  private XMOMDatabaseEntry registerWorkflow(WF wf, XMOMObjectSet xmomObjectsToFinishLater) throws PersistenceLayerException, AssumedDeadlockException {
    XMOMWorkflowDatabaseEntry entry = new XMOMWorkflowDatabaseEntry(wf);

    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      StorageAction action;
      try {
        entry.extendInstantiations(con);
        action = examineExistingEntry(con, entry);
        if (action == StorageAction.discard) {
          return entry;
        }
      } finally {
        con.closeConnection();
      }

      if (action == StorageAction.update) {
        unregisterWorkflow(wf);
      }

      con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.persistObject(entry);
        con.commit();

        RelationManagementMethods.insertNeededByRelations.execute(con, entry, xmomObjectsToFinishLater);
        con.commit();
        RelationManagementMethods.insertProducedByRelations.execute(con, entry, xmomObjectsToFinishLater);
        con.commit();
        RelationManagementMethods.insertThrownByRelations.execute(con, entry, xmomObjectsToFinishLater);
        con.commit();
        RelationManagementMethods.insertCalledByRelations.execute(con, entry, xmomObjectsToFinishLater);
        con.commit();
        RelationManagementMethods.insertInstancesUsedByRelations.execute(con, entry, xmomObjectsToFinishLater);
        con.commit();
        RelationManagementMethods.insertDataModelsUsedByRelations.execute(con, entry, xmomObjectsToFinishLater);
        con.commit();
        RelationManagementMethods.insertUsedInImplOfRelations.execute(con, entry, xmomObjectsToFinishLater);
        con.commit();
      } finally {
        con.closeConnection();
      }
    } finally {
      writeUnlockCache();
    }
    
    return entry;
  }
  
  
  private void registerForm(String originalFqName, Long revision) throws PersistenceLayerException {
    // TODO needs to be expanded once Form-Relations are to be extracted
    tryRegisterWithCommonValues(originalFqName, XMOMDatabaseType.FORMDEFINITION, revision, System.currentTimeMillis());
  }
  
  
  private void tryRegisterWithCommonValues(String originalFqName, XMOMDatabaseType type, Long revision, Long timestamp) throws PersistenceLayerException {
    RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Long revisionForEntry = rcdm.getRevisionDefiningXMOMObjectOrParent(originalFqName, revision);
    CommonValues values = XMOMCommonValueParser.tryParsingCommonValues(originalFqName, type, revisionForEntry);
    XMOMDatabaseEntry entry = type.generateInstanceOfArchiveStorableWithPrimaryKey(values.fqname, revisionForEntry);
    entry.setLabel(values.label);
    entry.setName(values.name);
    entry.setPath(values.path);
    entry.setTimestamp(timestamp);
    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        StorageAction action = examineExistingEntry(con, entry);
        
        if (action == StorageAction.discard) {
          return;
        }
        
        // we don't unregister in this registration because we're missing all values for forward relations
        con.persistObject(entry);
        con.commit();
      } finally {
        con.closeConnection();
      }
    } finally {
      writeUnlockCache();
    }
  }


  public void unregisterMOMObject(String originalFqName, Long revision) throws PersistenceLayerException, AssumedDeadlockException {
    try {
      unregisterMOMObject(originalFqName, GenerationBase.retrieveRootTag(originalFqName, revision, false, true), revision);
      // we could try to delete that entry anyway
      // We wouldn't be able to delete the backward relations though
    } catch (Ex_FileAccessException e) {
      logger.debug("Error during unregisterMOMObject", e);
      return;
    } catch (XPRC_XmlParsingException e) {
      logger.debug("Error during unregisterMOMObject", e);
      return;
    }
  }


  public void unregisterMOMObject(String originalFqName, String rootTag, Long revision) throws PersistenceLayerException,
                  AssumedDeadlockException {
    XMOMType objectType = XMOMType.getXMOMTypeByRootTag(rootTag);

    switch (objectType) {
      case DATATYPE :
        unregisterDatatype(originalFqName, revision, null);
        break;
      case EXCEPTION :
        unregisterException(originalFqName, revision);
        break;
      case WORKFLOW :
        unregisterWorkflow(originalFqName, revision);
        break;
      case FORM :
        unregisterForm(originalFqName, revision);
        break;
    }
  }

  public void unregisterXMOMObjects(Long revision) {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    boolean deployed = revisionManagement.isWorkspaceRevision(revision) ? false : true;
    String mdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, revision, deployed);
    List<File> files = FileUtils.getMDMFiles(new File(mdmDir), new ArrayList<File>());

    for (File f : files) {
      Document doc;
      try {
        doc = XMLUtils.parse(f);
      } catch (XynaException e) {
        logger.warn("could not parse xml: " + f.getAbsolutePath(), e);
        continue;
      }

      String originalFqName = GenerationBase.getFqXMLName(doc);
      String rootTagName = doc.getDocumentElement().getTagName();
      try {
        unregisterMOMObject(originalFqName, rootTagName, revision);
      } catch (XynaException e) {
        logger.warn("Error while trying to unregister xmom object " + originalFqName, e);
      }
    }
  }

  private void unregisterDatatype(String originalFqName, Long revision, Map<String, XMOMOperationDatabaseEntry> previousOperations) throws AssumedDeadlockException,
                  PersistenceLayerException {
    if (logger.isDebugEnabled()) {
      logger.debug("unregisterDatatype: " + originalFqName + " (revision " + revision + ")");
    }
    XMOMDomDatabaseEntry entry = new XMOMDomDatabaseEntry(originalFqName, revision);

    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        try {
          con.queryOneRow(entry);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.warn("Could not find entry for dom " + entry.getFqname() + ", some relations might not be properly removed");
          return;
        }
        con.delete(Arrays.asList(new XMOMDomDatabaseEntry[] {entry}));
        con.commit();

        RelationManagementMethods.removePossessedByRelations.execute(con, entry, null);
        con.commit();
        RelationManagementMethods.removeExtendedByRelations.execute(con, entry, null);
        con.commit();
        RelationManagementMethods.removeInstanceServiceReferenceOfRelations.execute(con, entry, null);
        con.commit();
      } finally {
        con.closeConnection();
      }

      if (entry.getWraps() != null) {
        String[] wrappedServiceGroup = entry.getWraps().split(XMOMDatabaseEntry.SEPERATION_MARKER);
        for (String serviceGroupName : wrappedServiceGroup) {
          if (XMOMDatabaseEntry.isValidFQName(serviceGroupName)) {
            XMOMServiceGroupDatabaseEntry serviceGroupEntry = new XMOMServiceGroupDatabaseEntry(serviceGroupName, entry.getRevision());
    
            con = ods.openConnection(ODSConnectionType.HISTORY);
            try {
    
              con.queryOneRow(serviceGroupEntry);
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              logger.warn("ServiceGroup " + serviceGroupName + " could not be found.");
            } finally {
              con.closeConnection();
            }
    
            unregisterServiceGroupAndOperations(serviceGroupEntry, previousOperations);
          }
        }
      }
    } finally {
      writeUnlockCache();
    }
  }



  public void unregisterDatatype(DOM dom) throws PersistenceLayerException, AssumedDeadlockException {
    unregisterDatatype(dom.getOriginalFqName(), dom.getRevision(), null);
  }

  public void unregisterDatatype(DOM dom, Map<String, XMOMOperationDatabaseEntry> previousOperations) throws PersistenceLayerException, AssumedDeadlockException {
    unregisterDatatype(dom.getOriginalFqName(), dom.getRevision(), previousOperations);
  }


  private void unregisterServiceGroupAndOperations(XMOMServiceGroupDatabaseEntry entry, Map<String, XMOMOperationDatabaseEntry> previousOperations) throws PersistenceLayerException,
                  AssumedDeadlockException {
    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.delete(Arrays.asList(new XMOMServiceGroupDatabaseEntry[] {entry}));
        con.commit();
      } finally {
        con.closeConnection();
      }

      if (entry.getGroups() != null) {
        String[] containedOperations = entry.getGroups().split(XMOMDatabaseEntry.SEPERATION_MARKER);
        if (containedOperations != null && containedOperations.length > 0) {
          for (String fqname : containedOperations) {
            if (XMOMDatabaseEntry.isValidFQName(fqname)) {
              logger.debug("unregistering operation: " + fqname + " (revision " + entry.getRevision() + ")");
              XMOMOperationDatabaseEntry operationEntry = new XMOMOperationDatabaseEntry(fqname, entry.getRevision());
              unregisterOperation(operationEntry, previousOperations);
            }
          }
        }
      }
    } finally {
      writeUnlockCache();
    }
  }

  private void unregisterOperation(XMOMOperationDatabaseEntry entry, Map<String, XMOMOperationDatabaseEntry> previousOperations) throws PersistenceLayerException,
                  AssumedDeadlockException {
    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.queryOneRow(entry);
        if (previousOperations != null) {
          previousOperations.put(entry.getId(), entry);
        }
        logger.debug("adding operation to list");
        con.deleteOneRow(entry);
        con.commit();

        RelationManagementMethods.removeNeededByRelations.execute(con, entry, null);
        con.commit();
        RelationManagementMethods.removeProducedByRelations.execute(con, entry, null);
        con.commit();
        RelationManagementMethods.removeThrownByRelations.execute(con, entry, null);
        con.commit();
        RelationManagementMethods.removeInstancesUsedByRelations.execute(con, entry, null);
        con.commit();
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.debug("Could not find entry for operation: " + entry.getFqname()
                     + " some usedBy relations might not be properly removed");
        return;
      } finally {
        con.closeConnection();
      }
      
    } finally {
      writeUnlockCache();
    }
  }


  private void unregisterException(String originalFqName, Long revision) throws AssumedDeadlockException, PersistenceLayerException {
    if (logger.isDebugEnabled()) {
      logger.debug("unregisterException: " + originalFqName + " (revision " + revision + ")");
    }
    XMOMExceptionDatabaseEntry entry = new XMOMExceptionDatabaseEntry(originalFqName, revision);

    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.queryOneRow(entry);
        con.delete(Arrays.asList(new XMOMExceptionDatabaseEntry[] {entry}));
        con.commit();

        RelationManagementMethods.removePossessedByRelations.execute(con, entry, null);
        con.commit();
        RelationManagementMethods.removeExtendedByRelations.execute(con, entry, null);
        con.commit();
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("Could not find entry for exception " + originalFqName
                        + ", some relations might not be properly removed");
      } finally {
        con.closeConnection();
      }
    } finally {
      writeUnlockCache();
    }

  }


  public void unregisterException(ExceptionGeneration excep) throws AssumedDeadlockException, PersistenceLayerException {
    unregisterException(excep.getOriginalFqName(), excep.getRevision());
  }


  private void unregisterWorkflow(String originalFqName, Long revision) throws PersistenceLayerException, AssumedDeadlockException {
    if (logger.isDebugEnabled()) {
      logger.debug("unregisterWorkflow: " + originalFqName + " (revision " + revision + ")");
    }
    XMOMWorkflowDatabaseEntry entry = new XMOMWorkflowDatabaseEntry(originalFqName, revision);

    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.queryOneRow(entry);
        con.delete(Arrays.asList(new XMOMWorkflowDatabaseEntry[] {entry}));
        con.commit();

        RelationManagementMethods.removeCalledByRelations.execute(con, entry, null);
        con.commit();
        RelationManagementMethods.removeNeededByRelations.execute(con, entry, null);
        con.commit();
        RelationManagementMethods.removeProducedByRelations.execute(con, entry, null);
        con.commit();
        RelationManagementMethods.removeThrownByRelations.execute(con, entry, null);
        con.commit();
        RelationManagementMethods.removeInstancesUsedByRelations.execute(con, entry, null);
        con.commit();
        RelationManagementMethods.removeDataModelsUsedByRelations.execute(con, entry, null);
        con.commit();
        RelationManagementMethods.removeUsedInImplOfRelations.execute(con, entry, null);
        con.commit();
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("Could not find entry for workflow " + originalFqName
                        + ", some relations might not be properly removed");
      } finally {
        con.closeConnection();
      }
    } finally {
      writeUnlockCache();
    }
  }


  public void unregisterGenerationBaseObject(GenerationBase object) throws PersistenceLayerException, AssumedDeadlockException {
    if (object instanceof WF) {
      unregisterWorkflow((WF)object);
    } else if (object instanceof ExceptionGeneration) {
      unregisterException((ExceptionGeneration)object);
    } else if (object instanceof DOM) {
      unregisterDatatype((DOM)object);
    } else {
      logger.error("Trying to unregister GenerationBaseObject of unknown Type");
    }
  }

  
  public void unregisterWorkflow(WF wf) throws PersistenceLayerException, AssumedDeadlockException {
    unregisterWorkflow(wf.getOriginalFqName(), wf.getRevision());
  }

  private void unregisterForm(String originalFqName, Long revision) throws PersistenceLayerException, AssumedDeadlockException {
    if (logger.isDebugEnabled()) {
      logger.debug("unregisterForm: " + originalFqName + " (revision " + revision + ")");
    }
    XMOMFormDefinitionDatabaseEntry entry = new XMOMFormDefinitionDatabaseEntry(originalFqName, revision);

    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.deleteOneRow(entry);
        con.commit();
      } finally {
        con.closeConnection();
      }
    } finally {
      writeUnlockCache();
    }
  }

  public void unregisterDataModel(String dataModelName) throws PersistenceLayerException {
    if (logger.isDebugEnabled()) {
      logger.debug("unregisterDataModel: " + dataModelName);
    }
    XMOMDataModelDatabaseEntry entry = new XMOMDataModelDatabaseEntry(dataModelName);

    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.deleteOneRow(entry);
        con.commit();
      } finally {
        con.closeConnection();
      }
    } finally {
      writeUnlockCache();
    }
  }
  
  
  /**
   * Discovery für alle Workspaces.
   */
  public void discovery() throws PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
      XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException,
      XNWH_NoPersistenceLayerConfiguredForTableException,
      XNWH_PersistenceLayerInstanceIdUnknownException {
    discovery(false);
  }

  /**
   * Alle XMOMEntries werden neu geparst und in der XMOMDatabase aktualisiert.
   * Alle Einträge, die nicht existieren und nur Backward-Relations enthalten auf Objekte, die auch nicht existieren (auf fileebene),
   * werden entfernt.
   * Falls sie nicht existieren, aber auch nicht gelöscht werden können, wird das Label auf null gesetzt
   * 
   * @param all gibt an, ob nur workspaces (all=false) oder auch applications (all=true) aktualisiert werden
   */
  public void discovery(boolean all) throws PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException,
  XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
  XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException,
  XNWH_NoPersistenceLayerConfiguredForTableException,
  XNWH_PersistenceLayerInstanceIdUnknownException {
    //Zunächst alle Zeitstempel auf -1 setzen
    updateAllTimestamps(XMOMState.missing_xml.getTimestamp(), all);
    
    //Dann das Discovery durchführen. Dabei werden alle Entries, deren xml noch existiert,
    //mit einem aktuellen Zeitstempel eingetragen.
    //Für alle Entries die nicht existieren, aber in die eine Backward-Relation eingetragen wird,
    //wird der Zeitstempel auf 0 gesetzt
    RevisionManagement revisionManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();

    for (Long revision : revisionManagement.getAllWorkspaceRevisions()) {
      GenerationBaseCache generationBaseCache = new GenerationBaseCache();
      rediscover(revision, generationBaseCache, false);
    }

    if (all) {
      for (Long revision : revisionManagement.getAllApplicationRevisions()) {
        GenerationBaseCache generationBaseCache = new GenerationBaseCache();
        rediscover(revision, generationBaseCache, true);
      }
    }
    
    //Nun können alle Einträge mit Zeitstempel -1 deregistriert werden (dabei werden auch die entsprechenden Backward-Relations entfernt)
    //und für alle Einträge mit Zeitstempel 0 das Label auf null gesetzt werden
    handleNonExistingEntries();    
  }
  
  
  public void rediscover(Long revision, GenerationBaseCache generationBaseCache, boolean deployed) throws PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException,
                  XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                  XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException,
                  XNWH_NoPersistenceLayerConfiguredForTableException, XNWH_PersistenceLayerInstanceIdUnknownException {
    if (logger.isDebugEnabled()) {
      logger.debug("Discovery started (revision " + revision + ")");
    }
    
    Set<String> fqNames = discoverAllXMOMFqNames(revision, deployed);
    for (String fqName : fqNames) {
      try {
        registerMOMObject(fqName, revision, generationBaseCache);
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.debug("Failed to register object", t);
      }
    }
  }
  
  public static Set<String> discoverAllXMOMFqNames(long revision, boolean deployed) {
    String mdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, revision, deployed);
    Set<String> originalNames = new HashSet<String>();
    List<File> files = FileUtils.getMDMFiles(new File(mdmDir), new ArrayList<File>());
    for (File file : files) {
      String originalFqName = file.getPath();
      if (originalFqName.startsWith(mdmDir + "/")) {
        originalFqName = originalFqName.substring(mdmDir.length() + 1);
      }
      if (originalFqName.endsWith(".xml")) {
        originalFqName = originalFqName.substring(0, originalFqName.length() - ".xml".length());
      }
      originalFqName = originalFqName.replaceAll("/", ".");
      if (logger.isTraceEnabled()) {
        logger.trace("Transformed filename to originalName: " + originalFqName);
      }
      originalNames.add(originalFqName);
    }
    return originalNames;
  }
  
  private Collection<XMOMDatabaseEntry> getAllXMOMEntries(ODSConnection con) throws PersistenceLayerException {
    Collection<XMOMDatabaseEntry> allEntries = new ArrayList<XMOMDatabaseEntry>();
    allEntries.addAll(con.loadCollection(XMOMDomDatabaseEntry.class));
    allEntries.addAll(con.loadCollection(XMOMExceptionDatabaseEntry.class));
    allEntries.addAll(con.loadCollection(XMOMWorkflowDatabaseEntry.class));
    allEntries.addAll(con.loadCollection(XMOMServiceGroupDatabaseEntry.class));
    allEntries.addAll(con.loadCollection(XMOMOperationDatabaseEntry.class));
    allEntries.addAll(con.loadCollection(XMOMFormDefinitionDatabaseEntry.class));
    
    return allEntries;
  }
  
  
  private void updateAllTimestamps(Long newTimestamp, boolean all) throws PersistenceLayerException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    writeLockCache();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        Collection<XMOMDatabaseEntry> allEntries = getAllXMOMEntries(con);
        for (XMOMDatabaseEntry entry : allEntries) {
          if (all || revisionManagement.isWorkspaceRevision(entry.getRevision())) {
            entry.setTimestamp(newTimestamp);
            con.persistObject(entry);
          }
        }
        con.commit();
      } finally {
        con.closeConnection();
      }
    } finally {
      writeUnlockCache();
    }
  }
  
  
  private void handleNonExistingEntries() throws PersistenceLayerException, AssumedDeadlockException {
    writeLockCache();
    try {
      Collection<XMOMDatabaseEntry> allEntries = new ArrayList<XMOMDatabaseEntry>();
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        
        allEntries = getAllXMOMEntries(con);
        
        for (XMOMDatabaseEntry entry : allEntries) {
          if (entry.getTimestamp() == null) {
            continue; //nicht existierender Eintrag
          }
          if (entry.getTimestamp().equals(XMOMState.missing_xml_but_backward_relations.getTimestamp())) {
            entry.setLabel(null);
            con.persistObject(entry);
          }
        }
        
        con.commit();
      } finally {
        con.closeConnection();
      }
      
      for (XMOMDatabaseEntry entry : allEntries) {
        if (entry.getTimestamp() == null) {
          continue; //nicht existierender Eintrag
        }
        if (entry.getTimestamp().equals(XMOMState.missing_xml.getTimestamp())) {
          if (entry instanceof XMOMDomDatabaseEntry) {
            unregisterDatatype(entry.getFqname(), entry.getRevision(), null);
          }
          if (entry instanceof XMOMExceptionDatabaseEntry) {
            unregisterException(entry.getFqname(), entry.getRevision());
          }
          if (entry instanceof XMOMWorkflowDatabaseEntry) {
            unregisterWorkflow(entry.getFqname(), entry.getRevision());
          }
          if (entry instanceof XMOMServiceGroupDatabaseEntry) {
            unregisterServiceGroupAndOperations((XMOMServiceGroupDatabaseEntry)entry, null);
          }
          if (entry instanceof XMOMOperationDatabaseEntry) {
            unregisterOperation((XMOMOperationDatabaseEntry)entry, null);
          }
          if (entry instanceof XMOMFormDefinitionDatabaseEntry) {
            unregisterForm(entry.getFqname(), entry.getRevision());
          }
        }
      }
    } finally {
      writeUnlockCache();
    }
  }
  
  
  public static class PreparedXMOMDatabaseSelect {

    public final List<Collection<XMOMDatabaseType>> archiveTypesPerSelect;
    
    private PreparedXMOMDatabaseSelect(List<Collection<XMOMDatabaseType>> archiveTypesPerSelect) {
      this.archiveTypesPerSelect = archiveTypesPerSelect;
    }
    
  }

  
  /**
   * suche für mehrfache wiederverwendung vorbereiten. achtung: dabei sind die whereclauses der selects irrelevant.
   * wichtig sind nur die 
   * - desired result types
   * - selektierte spalten
   */
  public PreparedXMOMDatabaseSelect prepareSearch(List<XMOMDatabaseSelect> selects) {
    Iterator<XMOMDatabaseSelect> selectIter = selects.iterator();
    List<Collection<XMOMDatabaseType>> archiveTypesPerSelect = new ArrayList<Collection<XMOMDatabaseType>>();
    while (selectIter.hasNext()) {
      XMOMDatabaseSelect actSelect = selectIter.next();

      Collection<XMOMDatabaseType> contextTypes = actSelect.getDesiredResultTypes();
      if (contextTypes.size() == 0) {
        selectIter.remove();
        continue;
      }
      contextTypes = XMOMDatabaseType.resolveXMOMDatabaseTypes(contextTypes);

      Collection<XMOMDatabaseType> typesOfRequest = retrieveArchiveTypesForRequest(actSelect);
      if (typesOfRequest.size() < 1) {
        selectIter.remove();
      } else {
        boolean atLeastOneValidArchive = false;
        Iterator<XMOMDatabaseType> arcIter = typesOfRequest.iterator();
        while (arcIter.hasNext()) {
          XMOMDatabaseType archiveType = arcIter.next();
          if (!contextTypes.contains(archiveType)) {
            arcIter.remove();
          } else {
            atLeastOneValidArchive = true;
          }
        }
        if (atLeastOneValidArchive) {
          archiveTypesPerSelect.add(typesOfRequest);
        } else {
          selectIter.remove();
        }
      }
    }
    return new PreparedXMOMDatabaseSelect(archiveTypesPerSelect);
  }

  public XMOMDatabaseSearchResult executePreparedSelect(PreparedXMOMDatabaseSelect prepared, List<XMOMDatabaseSelect> selects, int maxRows, Long revision) {
    return executePreparedSelect(prepared, selects, maxRows, revision, false);
  }
  
  private XMOMDatabaseSearchResult executePreparedSelect(PreparedXMOMDatabaseSelect prepared, List<XMOMDatabaseSelect> selects, int maxRows, Long revision, boolean searchDependentRevisions) {
    Map<XMOMDatabaseType, Integer> counts = new HashMap<XMOMDatabaseType, Integer>();
    try {
      List<Collection<XMOMDatabaseType>> archiveTypesPerSelect = prepared.archiveTypesPerSelect;
      
      List<XMOMDatabaseSearchResultEntry> results = new ArrayList<XMOMDatabaseSearchResultEntry>();
      int countAll = 0;
      readLockCache();
      try {
        Set<Long> allRelevantRevisions = new HashSet<Long>();
        if (searchDependentRevisions) {
          RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
          rcdm.getDependenciesRecursivly(revision, allRelevantRevisions);
        }
        allRelevantRevisions.add(revision);

        XMOMDatabaseSelect currentSelect;
        FutureCollection<List<XMOMDatabaseSearchResultEntry>> futureResults = new FutureCollection<List<XMOMDatabaseSearchResultEntry>>(); 
        for (int i = 0; i < archiveTypesPerSelect.size(); i++) {
          currentSelect = selects.get(i);
          for (XMOMDatabaseType archiveType : archiveTypesPerSelect.get(i)) {
            //mit maxRows = -1 suchen, damit Ergebnisse gezählt werden können
            //TODO zur Performance-Verbesserung wieder mit maxrows suchen und mit "select count(*)" zählen,
            //dabei muss aber die Selection dann aus den einzelnen selects richtig kombiniert werden
            for (Long relevantRevision : allRelevantRevisions) {
              ArchiveSearchTask request = new ArchiveSearchTask(currentSelect, archiveType, ods, -1, relevantRevision, searchDependentRevisions);
              futureResults.add(searchRequestPool.submit(request));
            }
          }
        }
        for (List<XMOMDatabaseSearchResultEntry> result : futureResults.get()) {
          results.addAll(result);
        }

        // sort & prune (we could do this faster if we don't add weights)
        List<XMOMDatabaseSearchResultEntry> prunedResults = new ArrayList<XMOMDatabaseSearchResultEntry>();
        XMOMDatabaseSearchResultEntry currentEntry;
        for (int i = 0; i < results.size(); i++) {
          currentEntry = results.get(i);
          int indexOfAlreadyContainedObject = prunedResults.indexOf(currentEntry);
          if (indexOfAlreadyContainedObject > -1) {
            prunedResults.get(indexOfAlreadyContainedObject).setWeigth(prunedResults.get(indexOfAlreadyContainedObject)
                                                                           .getWeigth() + currentEntry.getWeigth());
          } else {
            prunedResults.add(currentEntry);
            
            //neues Objekt zählen
            XMOMDatabaseType type = currentEntry.getType();
            //analog zu XMOMSearchDispatcher.searchDeploymentItemManagement
            if (type == XMOMDatabaseType.WORKFLOW || type == XMOMDatabaseType.OPERATION) {
              type = XMOMDatabaseType.SERVICE;
            }
            Integer count = counts.get(type);
            if (count == null) {
              count = 0;
            }
            counts.put(type, count + 1);
            countAll++;
          }
        }
        
        //Ergebnis auf maxRows begrenzen
        if (prunedResults.size() >= maxRows && maxRows > 0) {
          prunedResults = new ArrayList<XMOMDatabaseSearchResultEntry>(prunedResults.subList(0, maxRows));
        }
        results = sortResultList(prunedResults);
        
      } finally {
        readUnlockCache();
      }
      return new XMOMDatabaseSearchResult(results, countAll, counts);
    } catch (InterruptedException e) {
      logger.debug("Error during search, returning empty result", e);
      return new XMOMDatabaseSearchResult(new ArrayList<XMOMDatabaseSearchResultEntry>(), 0, counts);
    } catch (ExecutionException e) {
      logger.debug("Error during search, returning empty result", e);
      return new XMOMDatabaseSearchResult(new ArrayList<XMOMDatabaseSearchResultEntry>(), 0, counts);
    } catch (RuntimeException t) {
      logger.debug("Error during search, returning empty result", t);
      return new XMOMDatabaseSearchResult(new ArrayList<XMOMDatabaseSearchResultEntry>(), 0, counts);
    } catch (Throwable t) {
      logger.debug("Error during search, returning empty result", t);
      return new XMOMDatabaseSearchResult(new ArrayList<XMOMDatabaseSearchResultEntry>(), 0, counts);
    }
  }
  
  
  public XMOMDatabaseSearchResult searchXMOMDatabase(List<XMOMDatabaseSelect> selects, int maxRows, Long revision)
      throws XNWH_InvalidSelectStatementException, PersistenceLayerException {

    //objekte, die nicht existieren, haben zwar relationen, aber kein label - und sollen nicht gefunden werden
    for (XMOMDatabaseSelect s : selects) {
      //FIXME das and unterstützt hier keine sinnvolle klammerung - derzeit aber auch nicht unbedingt notwendig, weil wir keine entsprechenden statements verwenden
      s.and(s.newWhereClause().whereNumber(XMOMDatabaseEntryColumn.REVISION).isEqual(revision));
      s.and(s.newWhereClause().whereNot(s.newWhereClause().where(XMOMDatabaseEntryColumn.LABEL).isNull()));
    }
    PreparedXMOMDatabaseSelect prepared = prepareSearch(selects);
    return executePreparedSelect(prepared, selects, maxRows, revision, true);
  }


  private static class ArchiveSearchTask implements Callable<List<XMOMDatabaseSearchResultEntry>> {

    private final XMOMDatabaseSelect select;
    private final XMOMDatabaseType archive;
    private final ODS ods;
    private final int maxRows;
    private List<XMOMDatabaseSearchResultEntry> results = new ArrayList<XMOMDatabaseSearchResultEntry>();
    private Long revision;
    private final boolean substituteRevisionParameter;
    
    ArchiveSearchTask(XMOMDatabaseSelect select, XMOMDatabaseType archive, ODS ods, int maxRows, Long revision, boolean substituteRevisionParameter) {
      this.select = select;
      this.archive = archive;
      this.ods = ods;
      this.maxRows = maxRows;
      this.revision = revision;
      this.substituteRevisionParameter = substituteRevisionParameter;
    }
    
    public List<XMOMDatabaseSearchResultEntry> call() throws Exception {

      List<? extends XMOMDatabaseEntry> searchResult = new ArrayList<XMOMDatabaseEntry>();
      String selectString = select.getSelectString().replace(XMOMDatabaseSelect.ARCHIVEPLACEHOLDER,
                                                      archive.getArchiveIdentifier());
      ResultSetReader<? extends XMOMDatabaseEntry> reader = getReaderForArchive(archive, select.getSelection());
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        PreparedQuery<? extends XMOMDatabaseEntry> query = cache.getQueryFromCache(selectString, con, reader, archive.getArchiveIdentifier());
        Parameter paras;
        if (substituteRevisionParameter) {
          //letzter parameter ist revision und muss ausgetauscht werden, damit in allen runtimecontexten gesucht wird
          paras = substituteRevisionParameter(select.getParameter());
        } else {
          paras = select.getParameter();
        }
        searchResult = con.query(query, paras, maxRows);
        for (XMOMDatabaseEntry xmomCacheEntryBase : retrieveEntriesForSelection(con, select.getSelection(), searchResult)) {
          if (!excludeFromSearchResult(xmomCacheEntryBase)) {
            XMOMDatabaseSearchResultEntry entry = new XMOMDatabaseSearchResultEntry(xmomCacheEntryBase, 1);
            entry.setRuntimeContext(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(xmomCacheEntryBase.getRevision()));
            entry.setLabel( xmomCacheEntryBase.getCaselabel() );
            results.add(entry);
          }
        }
      } finally {
        con.closeConnection();
      }
      return results;
    }

    private Parameter substituteRevisionParameter(Parameter oldParas) {
      Parameter paras = select.getParameter();
      Object[] parasAsObject = new Object[paras.size()];
      for (int i = 0; i<parasAsObject.length; i++) {
        parasAsObject[i] = paras.get(i);
      }
      parasAsObject[parasAsObject.length - 1] = revision;
      paras = new Parameter(parasAsObject);
      return paras;
    }
  }
  
  
  private static boolean excludeFromSearchResult(XMOMDatabaseEntry entry) {
    if (entry.getFqname() == null) {
      return true;
    }
    if (entry.getXMOMDatabaseType() == XMOMDatabaseType.DATATYPE &&
                    entry.getMetadata() != null) {
      return entry.getMetadata().contains("<IsServiceGroupOnly>true</IsServiceGroupOnly>");
    } else {
      return false;
    }
  }
    
  
  private static List<? extends XMOMDatabaseEntry> retrieveEntriesForSelection(ODSConnection con, 
                                                                     Collection<XMOMDatabaseEntryColumn> selection,
                                                                     Collection<? extends XMOMDatabaseEntry> searchResult) throws PersistenceLayerException {
    List<XMOMDatabaseEntry> retrievedSelection = new ArrayList<XMOMDatabaseEntry>();
    for (XMOMDatabaseEntry xmomCacheEntry : searchResult) {
      boolean foundRelation = false;
      for (XMOMDatabaseEntryColumn column : selection) {
        if (column.hasReversedColumn()) { //nur dann ist es eine spalte, zu der man die referenz auf das andere/die anderen objekt auflösen will
          foundRelation = true;
          String value = null;
          try {
            value = xmomCacheEntry.getValueByColumn(column);
          } catch (IllegalArgumentException e) {
            ; //nothing to do
          }
          if (value != null && !value.equals("")) {
            String[] seperatedList = value.split(XMOMDatabaseEntry.SEPERATION_MARKER);
            pks : for (String fqName : seperatedList) {
              if (XMOMDatabaseEntry.isValidFQName(fqName)) {
                RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
                Long revisionForEntry = rcdm.getRevisionDefiningXMOMObjectOrParent(fqName, xmomCacheEntry.getRevision());
                
                //for each entry generate appropriate XMOMCacheEntry with that primaryKey
                for (XMOMDatabaseType archiveType : XMOMDatabaseType.getArchiveTypesForColumnLookup(column, true)) {
                  XMOMDatabaseEntry entry = archiveType.generateInstanceOfArchiveStorableWithPrimaryKey(fqName, revisionForEntry);
                  try {
                    //query that object
                    con.queryOneRow(entry);
                    retrievedSelection.add(entry);
                    continue pks;
                  } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
                    ; //no entry, no result
                  }
                }
              }
            }
          }
        }
      }
      if (!foundRelation) {
        retrievedSelection.add(xmomCacheEntry);
      }
    }
    
    return retrievedSelection;
  }
  

  private static Collection<XMOMDatabaseType> retrieveArchiveTypesForRequest(XMOMDatabaseSelect select) {
    Set<XMOMDatabaseType> typesFromWhereClauses = new HashSet<XMOMDatabaseType>();
    archives : for (XMOMDatabaseType type : XMOMDatabaseType.values()) {
      for (XMOMDatabaseEntryColumn column : select.getColumnsWithinWhereClauses()) {
        if (type.getAllowedColumns().contains(column)) {
          typesFromWhereClauses.add(type);
          continue archives;
        }
      }
    }
    
    if (XMOMDatabaseType.areArchiveTypesInvalid(typesFromWhereClauses)) {
      typesFromWhereClauses.clear();
      return typesFromWhereClauses;
    } else {
      typesFromWhereClauses = XMOMDatabaseType.transformArchives(typesFromWhereClauses);
    }

    Set<XMOMDatabaseType> typesFromSelectionClauses = new HashSet<XMOMDatabaseType>();
    archives : for (XMOMDatabaseType type : XMOMDatabaseType.values()) {
      for (XMOMDatabaseEntryColumn column : select.getSelection()) {
        if (type.getAllowedColumns().contains(column)) {
          typesFromSelectionClauses.add(type);
          continue archives;
        }
      }
    }

    typesFromSelectionClauses = XMOMDatabaseType.transformArchives(typesFromSelectionClauses);
    if (typesFromWhereClauses.size() > 0) {
      typesFromSelectionClauses.retainAll(typesFromWhereClauses);
    }
    
    return typesFromSelectionClauses;
  }


  


  private static ResultSetReader<? extends XMOMDatabaseEntry> getReaderForArchive(XMOMDatabaseType type,
                                                                               Set<XMOMDatabaseEntryColumn> selected) {
    switch (type) {
      case DATATYPE :
        return new XMOMDomDatabaseEntry.DynamicXMOMCacheReader(selected);
      case EXCEPTION :
        return new XMOMExceptionDatabaseEntry.DynamicXMOMCacheReader(selected);
      case OPERATION :
        return new XMOMOperationDatabaseEntry.DynamicXMOMCacheReader(selected);
      case SERVICEGROUP :
        return new XMOMServiceGroupDatabaseEntry.DynamicXMOMCacheReader(selected);
      case WORKFLOW :
        return new XMOMWorkflowDatabaseEntry.DynamicXMOMCacheReader(selected);
      case FORMDEFINITION :
        // TODO dynamic reader once fields
        return XMOMFormDefinitionDatabaseEntry.reader;
      default: 
        throw new RuntimeException("Readers for abstract archives '" +type.toString()+ "' can not be retrieved!");
    }
  }


  private void readLockCache() {
    cacheLock.readLock().lock();
  }


  private void readUnlockCache() {
    cacheLock.readLock().unlock();
  }


  private void writeLockCache() {
    if (!cacheLock.isWriteLockedByCurrentThread()) {
      cacheLock.writeLock().lock();
    }
  }


  private void writeUnlockCache() {
    while (cacheLock.isWriteLockedByCurrentThread()) {
      cacheLock.writeLock().unlock();
    }
  }
  
  
  private static class ArchiveRequestThreadFactory implements ThreadFactory {

    public Thread newThread(Runnable r) {
      return new Thread(r, "ArchiveSearchRequest");
    }
    
  }

  
  public Collection<XMOMDatabaseEntry> getAllXMOMEntriesFromSingleUnknown(String fqName, boolean forwardRelations,
                                                                          boolean backwardRelations, Long revision)
      throws PersistenceLayerException {

    Collection<XMOMDatabaseEntry> result = new ArrayList<XMOMDatabaseEntry>();
    if(fqName.contains("*")) {
      List<XMOMDatabaseSelect> selectList = new ArrayList<XMOMDatabaseSelect>();
      try {
        XMOMDatabaseSelect select = new XMOMDatabaseSelect();
        select.where(XMOMDatabaseEntryColumn.FQNAME).isLike(fqName.replace('*', '%'));
        select.addDesiredResultTypes(XMOMDatabaseType.GENERIC);
        selectList.add(select);
        XMOMDatabaseSearchResult searchResult = searchXMOMDatabase(selectList, Integer.MAX_VALUE, revision);
        for(XMOMDatabaseSearchResultEntry entry : searchResult.getResult()) {
          RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
          Long revisionForLookup = rcdm.getRevisionDefiningXMOMObjectOrParent(entry.getFqName(), revision);
          result.add(entry.getType().generateInstanceOfArchiveStorableWithPrimaryKey(entry.getFqName(), revisionForLookup));
        }
      } catch (XynaException e) {
        throw new XNWH_GeneralPersistenceLayerException("Failed to load xmomobjects", e);
      }
    }
    
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      if(!fqName.contains("*")) {
        XMOMDatabaseEntry entry;
        try {
          entry = new XMOMWorkflowDatabaseEntry(fqName, revision);
          con.queryOneRow(entry);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          try {
            entry = new XMOMDomDatabaseEntry(fqName, revision);
            con.queryOneRow(entry);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
            try {
              entry = new XMOMExceptionDatabaseEntry(fqName, revision);
              con.queryOneRow(entry);
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e2) {
              return Collections.emptyList();
            }
          }
        }
        result.add(entry);
      }
      
      Collection<XMOMDatabaseEntryColumn> selection = buildSelectionFromFlags(forwardRelations, backwardRelations);
      if (selection == null) {
        return result;
      } else {
        return followAllEntries(result, selection, con);
      }
    } finally {
      con.closeConnection();
    }
  }
  
  
  private Collection<XMOMDatabaseEntryColumn> buildSelectionFromFlags(boolean forwardRelations, boolean backwardRelations) {
    if (!forwardRelations && !backwardRelations) {
      return null;
    } else {
      Collection<XMOMDatabaseEntryColumn> selection = new ArrayList<XMOMDatabaseEntryColumn>();
      if (forwardRelations) {
        selection.addAll(XMOMDatabaseEntryColumn.ALL_FORWARD_RELATIONS);
      }
      if (backwardRelations) {
        selection.addAll(XMOMDatabaseEntryColumn.ALL_BACKWARD_RELATIONS);
      }
      return selection;
    }
  }
  
  
  private Collection<XMOMDatabaseEntry> followAllEntries(Collection<XMOMDatabaseEntry> entries, 
                                                         Collection<XMOMDatabaseEntryColumn> selectionToFollow,
                                                         ODSConnection con) throws PersistenceLayerException {
    Collection<XMOMDatabaseEntry> result = new ArrayList<XMOMDatabaseEntry>();
    result.addAll(entries);
    Collection<XMOMDatabaseEntry> toFollow = new ArrayList<XMOMDatabaseEntry>(entries);
    Set<String> alreadyFollowed = new HashSet<String>();
    while (true) {
      List<XMOMDatabaseEntry> objectsFromRelations = (List<XMOMDatabaseEntry>) retrieveEntriesForSelection(con, selectionToFollow, toFollow);
      for (XMOMDatabaseEntry xmomDatabaseEntry : toFollow) {
        alreadyFollowed.add(xmomDatabaseEntry.getFqname());
      }
      Iterator<XMOMDatabaseEntry> relationIterator = objectsFromRelations.iterator();
      while (relationIterator.hasNext()) {
        XMOMDatabaseEntry current = relationIterator.next();
        if (alreadyFollowed.contains(current.getFqname())) {
          relationIterator.remove();
        } else {
          result.add(current);
        }
      }
      toFollow = objectsFromRelations;
      if (toFollow.size() <= 0) {
        break;
      }
    }
    return result;
  }
  
  
  public String getDataModelUsedBy (String dataModelName) throws PersistenceLayerException {
    XMOMDataModelDatabaseEntry entry = new XMOMDataModelDatabaseEntry(dataModelName);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.queryOneRow(entry);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      //ok, DataModel wird es nicht verwendet
      return null;
    }
    
    return entry.getUsedBy();
  }
}
