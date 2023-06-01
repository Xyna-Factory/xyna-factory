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
package com.gip.xyna.xdev.xlibdev.codeaccess;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.parallel.ParallelExecutor;
import com.gip.xyna.utils.parallel.ParallelTask;
import com.gip.xyna.utils.parallel.SimpleXynaRunnableTaskConsumerPreparator;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.BasicProjectCreationOrChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EmptyRepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EventType;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.FilterCreationEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.ProjectCreationOrChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.XMOMChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.XMOMDeleteEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.XMOMMovementEvent;
import com.gip.xyna.xdev.XynaDevelopment;
import com.gip.xyna.xdev.exceptions.XDEV_InvalidProjectTemplateParametersException;
import com.gip.xyna.xdev.exceptions.XDEV_PathNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_RepositoryAccessException;
import com.gip.xyna.xdev.exceptions.XDEV_TimeoutException;
import com.gip.xyna.xdev.exceptions.XDEV_UnversionedParentException;
import com.gip.xyna.xdev.xlibdev.codeaccess.ComponentCodeChange.ComponentNotRegistered;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess.RecursionDepth;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess.RepositoryRevision;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess.RepositoryTransaction;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess.RevisionChangeListener;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryItemModification;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryItemModification.RepositoryModificationType;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.Support4Eclipse;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.base.FilterImplementationTemplate;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.base.ImplementationTemplate;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.base.ServiceImplementationTemplate;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.base.TriggerImplementationTemplate;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.classloading.SharedLibDeploymentAlgorithm;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDomDatabaseEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMExceptionDatabaseEntry;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.XynaMultiChannelPortal.Identity;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaRunnable;
import com.gip.xyna.xprc.XynaThreadPoolExecutor;
import com.gip.xyna.xprc.exceptions.XPRC_CompileError;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.CodeBuffer;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.XynaExceptionCodeGenerator;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectCodeGenerator;
import com.gip.xyna.xprc.xfractwfe.generation.serviceimpl.JavaServiceImplementation;
import com.gip.xyna.xprc.xsched.Algorithm;
import com.gip.xyna.xprc.xsched.LazyAlgorithmExecutor;



public class CodeAccess implements RevisionChangeListener {

  private static final Logger logger = CentralFactoryLogging.getLogger(CodeAccess.class);
  
  public final static String CLI_VERBOSE_IDENTIFIER = "verbose";
  public final static String CLI_REPOSITORY_IDENTIFIER = "repository";
  

  private String name;
  /**
   * zu welcher (application-)revision gehört das svn? (entspricht projekt-, user- oder default workingset)
   */
  private long revision;
  /**
   * die lokale Arbeitskopie (Arbeit = compile) des Repositories, ergibt sich aus Konstanten + id 
   */
  private String projectFolder;
  /**
   * parsen und aggregieren des outputs vom svn bei client-operationen
   */
  private RepositoryOutputParser parser;
  /**
   * wenn ein svn-update/checkout nicht funktioniert hat, gibt es hier einen entsprechenden fehler.
   */
  private XDEV_RepositoryAccessException repositoryDownstreamFailure;
  /**
   * wenn ein svn-commit nicht funktioniert hat, gibt es hier einen entsprechenden fehler.
   */
  private XDEV_RepositoryAccessException repositoryUpstreamFailure;
  /**
   * implementierung zuständig für builds (compile+jar-erstellung)
   */
  private XynaComponentBuilder builder;
  /**
   * Zugang zum Repository
   */
  private RepositoryAccess repositoryAccess;
  //semaphores anstatt von locks, weil die zuständigkeit nicht threadlocal ist (T1 lockt und T2 unlockt)
  /**
   * jede componente hat ihr eigenes lock
   */
  private final ConcurrentMap<String, Semaphore> buildLocks = new ConcurrentHashMap<String, Semaphore>();
  private final Semaphore repositoryLock = new Semaphore(1); //TODO feingranulareres Lock (auf Fileebene) verwenden
  /**
   * locks für die eclipse template erstellung
   */
  private final Semaphore[] templateLocks = new Semaphore[ComponentType.values().length];
  {
    for (int i = 0; i<templateLocks.length; i++) {
      templateLocks[i] = new Semaphore(1);
    }
  }
  /**
   * gleichzeitige updates auf das gleiche file sollten nicht passieren
   */
  private final Semaphore mdmLock = new Semaphore(1);
  /**
   * fehlgeschlagene builds und zugehörige fehlerinformation
   */
  private ConcurrentMap<String, BuildFailure> buildFailures = new ConcurrentHashMap<String, BuildFailure>();

  private final XynaThreadPoolExecutor threadpoolForBuildAndDeploy =
      new XynaThreadPoolExecutor(1, 20, 5, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {

        private AtomicLong cnt = new AtomicLong(0);


        public Thread newThread(Runnable r) {
          return new Thread(r, "BuildAndDeployExecutorThread-" + cnt.incrementAndGet());
        }

      }, "BuildAndDeployExecutorThreadPool");
  
  private final XynaThreadPoolExecutor threadpoolForFactoryChanges =
      new XynaThreadPoolExecutor(5, 5, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {

        private AtomicLong cnt = new AtomicLong(0);


        public Thread newThread(Runnable r) {
          return new Thread(r, "CodeAccessSVNFactoryChangesThread-" + cnt.incrementAndGet());
        }

      }, "CodeAccessSVNFactoryChangesThreadPool");


  private BuildAlgorithm buildAlgorithm = new BuildAlgorithm(this);
  private LazyAlgorithmExecutor<Algorithm> buildExecutor =
                  new LazyAlgorithmExecutor<Algorithm>("BuildExecutor");
  

  
  

  public static enum ComponentType {

    GLOBAL_LIB(0) {

      @Override
      public String getProjectSubFolder() {
        return Support4Eclipse.COMMON_FOLDER + Constants.FILE_SEPARATOR + "lib";
      }
    },
    USER_LIB(1) {

      @Override
      public String getProjectSubFolder() {
        return Support4Eclipse.COMMON_FOLDER + Constants.FILE_SEPARATOR + "userlib";
      }
    },
    SHARED_LIB(2) {

      @Override
      public String getProjectSubFolder() {
        return "sharedlibs";
      }
    },
    TRIGGER(3) {

      @Override
      public String getProjectSubFolder() {
        return Constants.SUBDIR_TRIGGER;
      }
    },
    FILTER(6) {

      @Override
      public String getProjectSubFolder() {
        return Constants.SUBDIR_FILTER;
      }
    },
    CODED_SERVICE(7) {

      @Override
      public String getProjectSubFolder() {
        return "servicegroups";
      }
    };

    private final int dependencyOrder;


    private ComponentType(int dependencyOrder) {
      this.dependencyOrder = dependencyOrder;
    }


    /**
     * niedriger wert =&gt; muss zuerst gebaut werden
     */
    public int getDependencyOrder() {
      return dependencyOrder;
    }
  
  /**
   * svn sub folder, does not need to correlate with factory folders
   */
    public abstract String getProjectSubFolder();
  }

  
  public static class BuildFailure implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final ComponentCodeChange component;
    private transient Throwable e;
    private String msg;


    public BuildFailure(ComponentCodeChange component, Throwable e) {
      this.component = component;
      this.e = e;
    }
    
    public ComponentCodeChange getComponent() {
      return component;
    }
    
    protected void prepareForSerialization() {
      if (e != null) {
        this.msg = e.getMessage();  
      }
      this.e = null;
    }
    
    

  }
  
  public static class FileUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    private final File file;
    private final ModificationType updateType;


    public FileUpdate(File file, ModificationType updateType) {
      this.file = file;
      this.updateType = updateType;
    }


    public File getFile() {
      return file;
    }


    public ModificationType getUpdateType() {
      return updateType;
    }
  }

  public static enum ModificationType {
    Modified, Deleted;
  }


  public CodeAccess(String name, Long revision, RepositoryAccess repositoryAccess) {
    this.name = name;
    this.revision = revision;
    this.repositoryAccess = repositoryAccess;
    
    // TODO most easy way to handle rejected executions, do we want a different handling
    threadpoolForBuildAndDeploy.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    threadpoolForFactoryChanges.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    buildExecutor.startNewThread(buildAlgorithm);
    
    projectFolder = repositoryAccess.getLocalRepository();
    String compileTargetVersion = null; //keine targetversion angeben
    builder = new XynaComponentBuilder(this, compileTargetVersion);
    parser = new RepositoryOutputParser(this);
    repositoryAccess.registerListener(this);
    try {
      newVersion(repositoryAccess.getHeadVersion());
    } catch (RuntimeException e) {
      //nicht failen
      logger.warn("Failed to update from repository", e);
    }

    try {
      for (BuildFailure failure : BuildFailures.restore(name, revision)) {
        buildFailures.put(failure.getComponent().getComponentOriginalName(), failure);
      }
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to restore BuildFailures", e);
    }
    
    ensureCommonLibs();
    
    if (CodeAccessManagement.updateRegeneratedClasses) {
      try {
        String commitMsg = "Regenerated classes triggered a mdm.jar rebuild.";
        rebuildAndCommitMDMJar("UpdateGeneratedClasses", commitMsg);
      } catch (XDEV_RepositoryAccessException e) {
        logger.warn(null, e);
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.warn(null, t);
      }
    }
    
  }
  

  /**
   * auschecken oder updaten, ggfs deployment anstossen, etc
   */
  public void newVersion(final RepositoryRevision version) {
    final List<ComponentCodeChange> deployList = new Vector<ComponentCodeChange>();

    //FIXME thread freigeben, wenn svn gelockt - der thread, der das lock hält, kann dann die aufgabe übernehmen, wenn er fertig ist

    boolean success = false;
    lockRepository(); //TODO partielles locking bzgl der änderungen, die ausgecheckt werden müssen
    //solange man beim build ist, keine weiteren svn updates durchführen
    try {
      final RepositoryTransaction transaction = repositoryAccess.beginTransaction("HookNotification");
      try {
        List<RepositoryItemModification> modifiedFiles = refreshFromRepository(transaction);
        if (logger.isDebugEnabled()) {
          logger.debug("Refresh from Repository found " + (modifiedFiles == null ? 0 : modifiedFiles.size()) + " modified files:");
          if (modifiedFiles != null) {
            for (RepositoryItemModification modified : modifiedFiles) {
              logger.debug("- " + modified.getFile() + " " + modified.getModification().name());
            }
          }
        }
        if (modifiedFiles == null || modifiedFiles.size() <= 0) {
          success = true;
          return;
        }
  
        List<ComponentCodeChange> modifiedXynaComponents = parser.parseModifiedComponents(modifiedFiles);
        SortedMap<Integer, List<ComponentCodeChange>> modifiedXynaComponentsSorted =
            sortComponentsForBuild(modifiedXynaComponents);
  
        //build: erst shared libs bauen, dann trigger, dann filter, etc
        // FIXME reihenfolge der builds ist wichtig, wenn es abhängigkeiten untereinander gibt.
        //  dies ist insbesondere der fall bei von einander abgeleiteten datentypen mit instanzmethoden
        Iterator<List<ComponentCodeChange>> iterator = modifiedXynaComponentsSorted.values().iterator();
        while (iterator.hasNext()) {
          List<ComponentCodeChange> modifiedComponents = iterator.next();
          List<ComponentCodeChange> buildDirectly = new ArrayList<ComponentCodeChange>();
          
          //builds gleichen typs parallelisieren, weil keine gegenseitige abhängigkeit
          for (final ComponentCodeChange modifiedComponent : modifiedComponents) {
            if (modifiedComponent.getComponentType() == ComponentType.CODED_SERVICE) {
              //ServiceGroups über LazyAlgorithmExecutor bauen und deployen
              buildAlgorithm.addChangedComponent(modifiedComponent);
              buildExecutor.requestExecution();
            } else {
              //Rest direkt bauen
              buildDirectly.add(modifiedComponent);
            }
          }
          
          if (!buildDirectly.isEmpty()) {
            deployList.addAll(buildParallel(buildDirectly, transaction, version.getStringRepresentation()));
          }
        }
        success = true;
      } finally {
        endTransaction(transaction, !success);
      }
    } finally {
      unlockRepository();
    }

    deploy(deployList);
  }

  /**
   * DeploymentItemStateManagement über build Ende informieren
   * @param modifiedComponent
   */
  private void notifyDeploymentItemMgmt(ComponentCodeChange modifiedComponent) {
    DeploymentItemStateManagement dism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    if (dism != null) {
      Optional<Throwable> exception = Optional.empty();
      BuildFailure failure = buildFailures.get(modifiedComponent.getComponentOriginalName());
      if (failure != null) {
        exception = Optional.of(failure.e);
      }
      dism.buildFinished(modifiedComponent.getComponentOriginalName(), exception, revision);
    }
  }
  
  // TODO bulk commit at end instead on a single reference?
  //      retrieve jar-destinations from XynaObjectBuilder as getDeploymentArtifacts(ComponentCodeChange) ?
  /*
   * filter eclipse projekte haben eingechecktes trigger-jar. dieses wird hier geupdated
   */
  protected void updateReferences(ComponentCodeChange modifiedComponent, RepositoryTransaction transaction) throws PersistenceLayerException {
    if (modifiedComponent.getComponentType() == ComponentType.TRIGGER) {
      try {
        Trigger registeredTrigger = modifiedComponent.getTrigger(revision);
        String newTriggerJar = getProjectDir() + File.separatorChar + modifiedComponent.getBasePath() + File.separatorChar + "deploy" +
                                 File.separatorChar + modifiedComponent.getComponentOriginalName() + ".jar";
        File newTriggerJarFile = new File(newTriggerJar);
        if (newTriggerJarFile.exists()) {
          Filter[] filtersToUpdate = XynaFactory.getInstance().getActivation().getActivationTrigger().getFilters(registeredTrigger.getTriggerName());
          for (Filter filter : filtersToUpdate) {
            String triggerJarBasePath = ComponentType.FILTER.getProjectSubFolder() + File.separatorChar + filter.getName() + File.separatorChar + "lib" +
                                          File.separatorChar + "xyna" + File.separatorChar + modifiedComponent.getComponentOriginalName() + ".jar";
            String triggerJar = getProjectDir() + File.separatorChar + triggerJarBasePath;
            File triggerJarFile = new File(triggerJar);
            if (triggerJarFile.exists()) {
              triggerJarFile.delete();
              try {
                triggerJarFile.createNewFile();
                FileUtils.copyFile(newTriggerJarFile, triggerJarFile);
                transaction.commit(new String[] {triggerJarBasePath}, "Updated trigger definition", RecursionDepth.TARGET_ONLY);
              } catch (Exception e) {
                logger.warn("Failed to update trigger library for filter " + filter.getName(), e);
              }
            }
          }
        }
      } catch (ComponentNotRegistered e) {
        //ntbd, no filter project if not added
      }
    }
    
  }


  /**
   * gibt null zurück, falls ein erwarteter fehler passiert. dieser wird dann in {@list #svnFailure} gespeichert.
   * fehlert bei fehlkonfiguration oder ähnlichem
   */
  private List<RepositoryItemModification> refreshFromRepository(RepositoryTransaction transaction) {
    List<RepositoryItemModification> modifiedFiles = null;
    if (new File(getProjectDir()).exists()) {
      try {        
        modifiedFiles = transaction.update(new String[] {Support4Eclipse.COMMON_FOLDER, ComponentType.TRIGGER.getProjectSubFolder(), ComponentType.FILTER.getProjectSubFolder(),
                                                         ComponentType.SHARED_LIB.getProjectSubFolder(), ComponentType.CODED_SERVICE.getProjectSubFolder()},
                                                         repositoryAccess.getHeadVersion(), RecursionDepth.FULL_RECURSION);
      } catch (XDEV_TimeoutException e) {
        try {
          transaction.rollback();
        } catch (XDEV_RepositoryAccessException e1) {
          logger.warn("Error during rollback of repository refresh", e);
        }
        repositoryDownstreamFailure = e;
        logger.warn("Error during repository refresh", e);
        return null;
      } catch (XDEV_RepositoryAccessException e) {
        repositoryDownstreamFailure = e;
        logger.warn("Error during repository refresh", e);
        return null;
      }
    } else {
      try {
        modifiedFiles = transaction.checkout(new String[] {""}, repositoryAccess.getHeadVersion());
      } catch (XDEV_PathNotFoundException e) {
        //ok, dann muss das im svn erst angelegt werden.
        logger.info("Failed to checkout project head", e);
        return null;
      } catch (XDEV_RepositoryAccessException e) {
        repositoryDownstreamFailure = e;
        logger.warn("Error during repository refresh", e);
        return null;
      }
    }
    repositoryDownstreamFailure = null;
    return modifiedFiles;
  }


  private SortedMap<Integer, List<ComponentCodeChange>> sortComponentsForBuild(List<ComponentCodeChange> modifiedXynaComponents) {
    SortedMap<Integer, List<ComponentCodeChange>> result = new TreeMap<Integer, List<ComponentCodeChange>>();
    for (ComponentCodeChange c : modifiedXynaComponents) {
      int prio = c.getComponentType().getDependencyOrder();
      List<ComponentCodeChange> list = result.get(prio);
      if (list == null) {
        list = new ArrayList<ComponentCodeChange>();
        result.put(prio, list);
      }
      list.add(c);
    }
    return result;
  }


  /**
   * alle jars der komponente
   */
  private File[] getAllJarsForDeployment(ComponentCodeChange component) {
    File savedDir = builder.getSavedDir(component);
    return savedDir.listFiles(XynaComponentBuilder.jarFilter);
  }


  private void unlockForBuild(ComponentCodeChange modifiedComponent) {
    Semaphore s = buildLocks.get(modifiedComponent.getComponentOriginalName());
    boolean otherWaiting = s.hasQueuedThreads();
    if (!otherWaiting) {
      //aufräumen
      buildLocks.remove(modifiedComponent.getComponentOriginalName());
      //man könnte das so jedes mal machen.
      //aus performancegründen nur dann, wenn man den verdacht hat, dass die semaphore nicht mehr benötigt wird.

      //schlimmstenfalls kam gerade einer und hat die semaphore doch noch gesehen.
      //dann sieht er nach dem acquire, dass sie nicht mehr in der map ist.
    }
    s.release();
  }


  private void lockForBuild(ComponentCodeChange modifiedComponent) {
    while (true) {
      Semaphore s = buildLocks.get(modifiedComponent.getComponentOriginalName());
      if (s != null) {
        try {
          s.acquire();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        //wurde es bereits aus der map entnommen und ist damit ungültig?
        if (buildLocks.get(modifiedComponent.getComponentOriginalName()) == s) {
          //man hat das offizielle lock -> erfolg!
          return;
        }
        s.release();
      } else {
        s = new Semaphore(1);
        try {
          s.acquire();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        if (null == buildLocks.putIfAbsent(modifiedComponent.getComponentOriginalName(), s)) {
          //man hat das offizielle lock -> erfolg!
          return;
        }
        //else retry. release nicht notwendig, weil die semaphore keiner sieht
      }
    }
  }


  void lockRepository() {
    try {
      repositoryLock.acquire();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }


  void unlockRepository() {
    repositoryLock.release();
  }


  /**
   * baut alle jars, die zu dieser komponente gehören und legt sie in den zugehörigen saved-ordner.
   * die jars, die sich nicht geändert haben, müssen auch im saved-ordner liegen, können aber von einem vorherigen 
   * build-vorgang übernommen oder vom deployed-ordner kopiert werden.
   * 
   * auch bei shared_libs wird 
   */
  private void build(ComponentCodeChange modifiedComponent, String version) throws XynaException {
    builder.build(modifiedComponent, version);
  }
  
  
  private boolean build(ComponentCodeChange modifiedComponent, final boolean deploy) {
    if (logger.isDebugEnabled()) {
      logger.debug("building component " + modifiedComponent.getComponentOriginalName());
    }
    boolean success = false;
    lockForBuild(modifiedComponent);
    try {
      builder.build(modifiedComponent, null);
      buildFailures.remove(modifiedComponent.getComponentOriginalName());
      if (deploy) {
        threadpoolForBuildAndDeploy.execute(new DeployModifiedComponentsRunnable(this, Collections.singletonList(modifiedComponent)));
      }
      success = true;
    } catch (Throwable t) {
      buildFailures.put(modifiedComponent.getComponentOriginalName(), new BuildFailure(modifiedComponent, t));
    } finally {
      if (!success || !deploy) {
        unlockForBuild(modifiedComponent);
      }
    }
    
    notifyDeploymentItemMgmt(modifiedComponent);
    return success;
  }

  List<ComponentCodeChange> rebuild(Collection<ComponentCodeChange> collection, final String version) {
    final RepositoryTransaction transaction = repositoryAccess.beginTransaction("Rebuild");
    boolean success = false;
    try {
      List<ComponentCodeChange> deployList = buildParallel(collection, transaction, version);
      success = true;
      return deployList;
    } finally {
      endTransaction(transaction, !success);
    }
  }
  
  
  private List<ComponentCodeChange> buildParallel(Collection<ComponentCodeChange> collection, final RepositoryTransaction transaction, final String version) {
    final List<ComponentCodeChange> deployList = new Vector<ComponentCodeChange>();

    ParallelExecutor executor = new ParallelExecutor(threadpoolForBuildAndDeploy);
    executor.setTaskConsumerPreparator(new SimpleXynaRunnableTaskConsumerPreparator(false));
    
    //builds gleichen typs parallelisieren, weil keine gegenseitige abhängigkeit
    for (final ComponentCodeChange modifiedComponent : collection) {
      executor.addTask(new ParallelTask() {
        
        public int getPriority() {
          return 1;
        }
        
        public void execute() {
          if (logger.isDebugEnabled()) {
            logger.debug("building " + modifiedComponent.getComponentOriginalName() + " " + modifiedComponent.getModificationType().name());
          }
          try {
            switch (modifiedComponent.getModType()) {
              case Deleted :
                //TODO undeploy
                buildFailures.put(modifiedComponent.getComponentOriginalName(),
                                  new BuildFailure(modifiedComponent, new RuntimeException("unsupported")));
              case Modified :
                boolean success = false;
                lockForBuild(modifiedComponent);
                try {
                  build(modifiedComponent, version);
                  deployList.add(modifiedComponent);
                  updateReferences(modifiedComponent, transaction);
                  success = true;
                  buildFailures.remove(modifiedComponent.getComponentOriginalName());
                } catch (Exception e) {
                  buildFailures.put(modifiedComponent.getComponentOriginalName(), new BuildFailure(modifiedComponent, e));
                  //nächste komponente bauen
                  success = true;
                  unlockForBuild(modifiedComponent);
                } finally {
                  if (!success) {
                    buildFailures.put(modifiedComponent.getComponentOriginalName(), null);
                    unlockForBuild(modifiedComponent);
                  }
                }
                break;
              default :
                throw new RuntimeException("unsupported: " + modifiedComponent.getModificationType());
            }
            
            notifyDeploymentItemMgmt(modifiedComponent);
          } catch (Throwable t) {
            Department.handleThrowable(t);
            logger.warn(null, t);
          }
        }
      });
    }

    //warten, dass build fertig ist
    try {
      executor.executeAndAwait();
    } catch (InterruptedException e) {
      //TODO direkt return und aufräumen?
    }

    return deployList;
  }

  void deploy(List<ComponentCodeChange> deployList) {
    //deployment
    //TODO undeployment
    //     beim undeployment auch die verzeichnisse ganz löschen, damit keine nicht-eingecheckten bin oder deploy-verzeichnisse überbleiben
    threadpoolForBuildAndDeploy.execute(new DeployModifiedComponentsRunnable(this, deployList));
  }
  
  public Map<String, BuildFailure> getBuildFailures() {
    return buildFailures;
  }


  /*
   * TODO methoden für die ermittlung von
   * - was für svn updates laufen gerade
   * - was ist der status der thread pools
   */


  private static class DeployModifiedComponentsRunnable extends XynaRunnable {

    private final CodeAccess codeAccess;
    private final List<ComponentCodeChange> deployList;


    public DeployModifiedComponentsRunnable(CodeAccess codeAccess, List<ComponentCodeChange> deployList) {
      this.codeAccess = codeAccess;
      this.deployList = deployList;
    }


    public void run() {
      GenerationBaseCache generationCache = new GenerationBaseCache();
      
      long revisionCopy = codeAccess.getRevision();
      try {
        try {
          List<GenerationBase> allDatatypes = new ArrayList<GenerationBase>();
          List<ComponentCodeChange> allFilters = new ArrayList<ComponentCodeChange>();
          
          //xmomobjekte benötigen ggf aktuelle sharedlibs, dann müssen ggf auch filter/trigger erneut deployed werden
          //deployment hat eigenes locking
          //TODO asynchron? -> dann das unlock im callbackhandler wenn deployment fertig ist
          for (ComponentCodeChange component : deployList) {
            switch (component.getComponentType()) {
              case CODED_SERVICE :
                allDatatypes.add(component.getDOM(revisionCopy, generationCache));
                break;
              case FILTER :
                allFilters.add(component);
                break;
              case GLOBAL_LIB :
                //FIXME
                throw new RuntimeException("unsupported");
              case SHARED_LIB :
                try {
                  SharedLibDeploymentAlgorithm.deploySharedLib(component.getComponentOriginalName(), revisionCopy, new EmptyRepositoryEvent());
                } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
                  logger.info("shared lib not used?: " + component.getComponentOriginalName());
                  //ok
                }
                break;
              case TRIGGER :
                Trigger t = component.getTrigger(revisionCopy);
                File[] jars = codeAccess.getAllJarsForDeployment(component);
                String[] sharedLibs = t.getSharedLibs();
                XynaFactory
                    .getInstance()
                    .getActivation()
                    .getActivationTrigger()
                    .addTrigger(component.getComponentOriginalName(), jars, t.getFQTriggerClassName(), sharedLibs,
                                revisionCopy, new EmptyRepositoryEvent());
                break;
              case USER_LIB :
                //FIXME
                throw new RuntimeException("unsupported");
              default :
                throw new RuntimeException("unsupported component type: " + component.getComponentType());
            }
          }

          //gemeinsames deployment für Datentypen
          for (GenerationBase gb : allDatatypes) {
            gb.setDeploymentComment("CodeAccess");
          }
          GenerationBase.deploy(allDatatypes, DeploymentMode.codeChanged, false, WorkflowProtectionMode.FORCE_DEPLOYMENT);

          //als letztes die Filter deployen
          for (ComponentCodeChange component : allFilters) {
            Filter f = component.getFilter(revisionCopy);
            File[] jars = codeAccess.getAllJarsForDeployment(component);
            String[] sharedLibs = f.getSharedLibs();
            XynaFactory
                .getInstance()
                .getActivation()
                .getActivationTrigger()
                .addFilter(component.getComponentOriginalName(), jars, f.getFQFilterClassName(),
                          f.getTriggerName(), sharedLibs, f.getDescription(), revisionCopy,
                           new EmptyRepositoryEvent());
          }
        } finally {
          for (ComponentCodeChange component : deployList) {
            codeAccess.unlockForBuild(component);
          }
        }
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error(null, t);
      }
    }

  }




  public void shutdown() {
    threadpoolForBuildAndDeploy.shutdown();
    buildExecutor.stopThread();
  }


  public long getRevision() {
    return revision;
  }


  public String getName() {
    return name;
  }


  /**
   * spezialinformationen abfragen, über interne datenhaltung etc 
   */
  public Reader getExtendedInformation(final String[] args) {
    PipedReader reader = new PipedReader();
    final PipedWriter writer;
    try {
      writer = new PipedWriter(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    threadpoolForFactoryChanges.execute(new XynaRunnable() {

      public void run() {
        boolean verbose = false;
        boolean repositoryInformation = false;
        for (String arg : args) {
          if (arg.equals(CLI_VERBOSE_IDENTIFIER)) {
            verbose = true;
          } else if (arg.equals(CLI_REPOSITORY_IDENTIFIER)) {
            repositoryInformation = true;
          }
        }
        
        BufferedWriter w = new BufferedWriter(writer);
        try {
          Set<String> lockKeys = buildLocks.keySet();
          if (lockKeys.size() > 0) {
            w.write("Currently building:\n");
            Iterator<String> keyIter = lockKeys.iterator();
            while (keyIter.hasNext()) {
              w.write(keyIter.next() + "\n");
            }
          } else {
            w.write("Currently no build in progress.\n");
          }
          XDEV_RepositoryAccessException e = repositoryDownstreamFailure;
          if (e != null) {
            w.write("Repository downstream Failure:\n");
            w.write(e.getRepositoryMessage() + "\n");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            w.write(sw.toString() + "\n");
          }
          e = repositoryUpstreamFailure;
          if (e != null) {
            w.write("Repository upstream Failure:\n");
            w.write(e.getRepositoryMessage() + "\n");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            w.write(sw.toString() + "\n");
          }
          if (buildFailures.entrySet().size() > 0) {
            w.write("Build Failures:\n");
          }
          if (verbose) {
            for (Entry<String, BuildFailure> bf : buildFailures.entrySet()) {
              StringWriter sw = new StringWriter();
              if (bf.getValue().e == null) {
                sw.write(bf.getValue().msg);
              } else {
                bf.getValue().e.printStackTrace(new PrintWriter(sw));
              }
              w.write(bf.getKey() + ": " + sw.toString() + "\n");
            }
          } else {
            for (String bf : buildFailures.keySet()) {
              w.write(bf + "\n");
            }
          }
          if (repositoryInformation) {
            repositoryAccess.writeExtendedInformation(args, w);
          }
          w.flush();
        } catch (Throwable t) {
          logger.error(null, t);
        } finally {
          try {
            w.close();
          } catch (IOException e) {
            logger.warn(null, e);
          }
        }
      }

    });
    return reader;
  }


  public String getProjectDir() {
    return projectFolder;
  }

  public RepositoryAccess getRepositoryAccess() {
    return repositoryAccess;
  }
  
  /**
   * trigger klassen erstellen, zum svn adden und einchecken. triggerName endet nicht auf Trigger, dies wird intern hinzugefügt.
   */
  private void createTrigger(String triggerName) {
    if (triggerName.endsWith("Trigger")) {
      triggerName = triggerName.substring(0, triggerName.lastIndexOf("Trigger"));
    }
    final ComponentCodeChange componentForBuild = new ComponentCodeChange(triggerName + "Trigger", ComponentType.TRIGGER);
    final TriggerImplementationTemplate template = new TriggerImplementationTemplate(triggerName, revision);
    FactoryAdditionRunnable factoryAddition = new FactoryAdditionRunnable() {
      
      public void addToFactory(File[] jars) {
        if (jars != null && jars.length > 0) {
          try {
            XynaFactory
                .getInstance()
                .getActivation()
                .getActivationTrigger()
                .addTrigger(componentForBuild.getComponentOriginalName(), jars, template.getTriggerFQClassName(), new String[0], revision, new EmptyRepositoryEvent());
          } catch (XynaException e) {
            throw new RuntimeException(e);
          }
        } else {
          try { // add empty
            XynaFactory
                .getInstance()
                .getActivation()
                .getActivationTrigger()
                .addTrigger(componentForBuild.getComponentOriginalName(), new File[0], template.getTriggerFQClassName(), new String[0], revision, true, true, new EmptyRepositoryEvent());
          } catch (XynaException e) {
            throw new RuntimeException(e);
          }
        }
      }
      
    };
    createTriggerOrFilter(componentForBuild, template, factoryAddition);
  }
  
  
  private void createTriggerOrFilter(ComponentCodeChange newComponent, ImplementationTemplate template, FactoryAdditionRunnable factoryAddition) {
    String absoluteDirPath = getProjectDir() + Constants.FILE_SEPARATOR + newComponent.getBasePath();
    File f = new File(absoluteDirPath);
    if (f.exists()) {
      throw new RuntimeException(newComponent.getComponentType() + " does already exist");
    }

    boolean success = false;
    try {
      lockTemplate(ComponentType.TRIGGER);
      try {
        Support4Eclipse.buildProjectTemplate(f.getParentFile(), template, false);
        success = true;
      } catch (XDEV_InvalidProjectTemplateParametersException e) {
        throw new RuntimeException(e);
      } catch (Ex_FileAccessException e) {
        throw new RuntimeException(e);
      } catch (XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH e) {
        throw new RuntimeException(e);
      } catch (XPRC_InvalidPackageNameException e) {
        throw new RuntimeException(e);
      } catch (XPRC_XmlParsingException e) {
        throw new RuntimeException(e);
      } finally {
        unlockTemplate(ComponentType.TRIGGER);
      }
    } finally {
      if (!success) {
        //bei fehlern aufräumen
        FileUtils.deleteDirectoryRecursively(f);
      }
    }
    
    success = false;
    lockRepository();
    try {
      RepositoryTransaction transaction = repositoryAccess.beginTransaction(getIdentityString());
      try {
        ensurePathExistsInRepository(f.getParentFile(), newComponent.getComponentType().getProjectSubFolder(), transaction);
        transaction.add(new String[] {newComponent.getBasePath()}, RecursionDepth.FULL_RECURSION);
        transaction.commit(new String[] {newComponent.getBasePath()},
                           newComponent.getComponentType() + " '" + newComponent.getComponentOriginalName() + "' created.",
                           RecursionDepth.FULL_RECURSION);
        success = true;
        //TODO fehlerbehandlung verbessern
      } catch (XDEV_RepositoryAccessException e) {
        throw new RuntimeException(e);
      } finally { 
        endTransaction(transaction, !success);
      }
    } finally {
      unlockRepository();
    }

    fillComponentCodeChangeForNewComponent(newComponent);
    success = false;
    if (build(newComponent, false)) {
      File[] jars = getAllJarsForDeployment(newComponent);
      try {
        factoryAddition.addToFactory(jars);
        success = true;
      } catch (RuntimeException e) {
        // ntbd
      }
    }
    if (!success) {
      factoryAddition.addToFactory(new File[0]);
    }
  }


  private void ensurePathExistsInRepository(File parentFile, String projectSubFolder, RepositoryTransaction transaction) {
    if (!parentFile.exists()) {
      //trigger verzeichnis noch nicht vorhanden
      if (!parentFile.mkdirs()) {
        //vielleicht jemand anderes das verzeichnis geaddet?
        if (!parentFile.exists()) {
          throw new RuntimeException("could not create dir + " + parentFile.getAbsolutePath());
        }
      } else {
        //zum repository adden (falls noch nicht versioniert)
        if (isUnversioned(transaction, projectSubFolder)) {
          try {
            transaction.add(new String[]{projectSubFolder}, RecursionDepth.TARGET_ONLY);
            transaction.commit(new String[]{projectSubFolder}, "adding directory '" + projectSubFolder + "'", RecursionDepth.TARGET_ONLY);
          } catch (XDEV_RepositoryAccessException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }
  
  private boolean isUnversioned(RepositoryTransaction transaction, String file) {
    try {
      List<RepositoryItemModification> statusList = transaction.status(new String[] {file});
      if (statusList == null || statusList.isEmpty()) {
        return true;
      }
      
      for (RepositoryItemModification status : statusList) {
        if (status.getFile().endsWith(file)) { //nur den Status des übergebenen Verzeichnisses überprüfen (Unterverzeichnisse dürfen unversioned sein)
          return status.getModification().equals(RepositoryModificationType.Unversioned);
        }
      }
      return false;
    } catch (XDEV_RepositoryAccessException e) {
      //svn status schlägt fehl, wenn das Parent-Verzeichnis noch nicht versioniert ist
      return true;
    }
  }
  
  private void createFilter(String filterName, final String triggerName) {
    if (filterName.endsWith("Filter")) {
      filterName = filterName.substring(0, filterName.lastIndexOf("Filter"));
    }
    final ComponentCodeChange componentForBuild = new ComponentCodeChange(filterName + "Filter", ComponentType.FILTER);
    final FilterImplementationTemplate template = new FilterImplementationTemplate(filterName, triggerName, revision);
    FactoryAdditionRunnable factoryAddition = new FactoryAdditionRunnable() {
      
      public void addToFactory(File[] jars) {
        if (jars != null && jars.length > 0) {
          try {
            XynaFactory
              .getInstance()
              .getActivation()
              .getActivationTrigger()
              .addFilter(componentForBuild.getComponentOriginalName(), jars, template.getFqFilterClassName(),
                         triggerName, new String[0], "", revision, new EmptyRepositoryEvent());
          } catch (XynaException e) {
            throw new RuntimeException(e);
          }
        } else {
          try { // add empty
            XynaFactory
              .getInstance()
              .getActivation()
              .getActivationTrigger()
              .addFilter(componentForBuild.getComponentOriginalName(), null, template.getFqFilterClassName(), triggerName, new String[0], "",
                         revision, true, true, new EmptyRepositoryEvent());
          } catch (XynaException e) {
            throw new RuntimeException(e);
          }
        }
      }
      
    };
    createTriggerOrFilter(componentForBuild, template, factoryAddition);
  }


  private void unlockTemplate(ComponentType codedService) {
    templateLocks[codedService.ordinal()].release();
  }

  /**
   * beim erstellen von eclipse projekten wird temporär ein verzeichnis "TemplateImplName" angelegt.
   * beim gleichzeitgen erstellen von eclipse projekten des gleichen typs kann man sich dadurch
   * gegenseitig in die quere kommen. -> locking! 
   */
  private void lockTemplate(ComponentType codedService) {
    try {
      templateLocks[codedService.ordinal()].acquire();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }


  protected String getRelativePathOfMdmJar() {
    return ComponentType.GLOBAL_LIB.getProjectSubFolder() + Constants.FILE_SEPARATOR
        + "mdm.jar";
  }
  
  
  private String getJavaGenerationFolder() {
    return getProjectDir() + Constants.fileSeparator + "gen";
  }
  
  
  private String getJavaClassFolder() {
    return getProjectDir() + Constants.fileSeparator + "xmomclasses";
  }

  private void rebuildMdmJarAndPrepareCodedServices(Collection<? extends XMOMChangeEvent> events, Map<String, Triple<XMOMChangeEvent, DOM, ComponentCodeChange>> components) {
    GenerationBaseCache cache = new GenerationBaseCache();

    boolean success = false;
    List<String> dirsToCommit = new ArrayList<String>();
    StringBuilder commitMsg = new StringBuilder();
    
    lockRepository();
    try {
      RepositoryTransaction transaction = repositoryAccess.beginTransaction(getIdentityString());
      try {
        transaction.setTransactionProperty("force", Boolean.TRUE);
        List<GenerationBase> gbToAdd = new ArrayList<>();
        List<Triple<String, XMOMType, Long>> toRemove = new ArrayList<>();
        for (XMOMChangeEvent event : events) {
          if (event.getType() == EventType.XMOM_DELETE) {
            XMOMDeleteEvent del = (XMOMDeleteEvent) event;
            toRemove.add(Triple.of(del.getObjectIdentifier(), del.getXMOMType(), del.getRevision()));
          } else {
            try {
              GenerationBase gb;
              switch (event.getXMOMType()) {
                case DATATYPE :
                  gb = DOM.getOrCreateInstance(event.getObjectIdentifier(), cache, revision);
                  break;
                case EXCEPTION :
                  gb = ExceptionGeneration.getOrCreateInstance(event.getObjectIdentifier(), cache, revision);
                  break;
                case FORM :
                case WORKFLOW :
                default :
                  // ntbd
                  continue;
              }
              gb.parseGeneration(false, false); 
              
              //für die eigene revision die ServiceGroups ermitteln
              if (gb.getRevision().equals(revision)) {
                if (event.getXMOMType() == XMOMType.DATATYPE) {
                  DOM dom = (DOM) gb;
                  if (hasNonAbstractJavaOperationNotImplementedAsCodeSnippet(dom) || dom.libraryExists()) {
                    prepareLocalRepository(event, transaction, dirsToCommit, commitMsg);
                    if (CodeAccessManagement.globalCodeAccessFilter.get().accept(dom)) {
                      components.put(dom.getOriginalFqName(), Triple.<XMOMChangeEvent,DOM,ComponentCodeChange>of(event, dom, null));
                    } else {
                      logger.debug("skipped component " + dom.getOriginalFqName() + " because of filter " + CodeAccessManagement.globalCodeAccessFilter.getPropertyName());
                    }
                  }
                }
              }

              gbToAdd.add(gb);

              if (event.getType() == EventType.XMOM_MOVE) {
                XMOMMovementEvent mv = (XMOMMovementEvent) event;
                toRemove.add(Triple.of(mv.getOldFqName(), mv.getXMOMType(), mv.getRevision()));
              }
            } catch (Exception e) {
              logger.warn("Failed to update MDM.jar.", e);
            }
          }
        }
        
        //batch update mdm.jar
        if (!gbToAdd.isEmpty()) {
          updateMdmJar(gbToAdd, transaction);
        }
        if (!toRemove.isEmpty()) {
          removeFromMdmJar(toRemove, cache, transaction);
        }
        
        success = true;
      } finally {
        if (success) {
          commitMsg.append("mdm.jar updated.");
          dirsToCommit.add(getRelativePathOfMdmJar());
          try {
            transaction.commit(dirsToCommit.toArray(new String[dirsToCommit.size()]), commitMsg.toString(), RecursionDepth.FULL_RECURSION);
          } catch (XDEV_RepositoryAccessException e) {
            logger.warn("Failed to commit transaction",e);
          }
        }

        endTransaction(transaction, !success);
      }
    } finally {
      unlockRepository();
    }
  }


  boolean hasNonAbstractJavaOperationNotImplementedAsCodeSnippet (DOM dom) {
    List<Operation> operations = dom.getOperations();
    for (Operation operation : operations) {
      if (operation instanceof JavaOperation &&
          !operation.isAbstract() &&
          (((JavaOperation)operation).hasEmptyImpl() || operation.implementedInJavaLib())) {
        return true;
      }
    }
    
    return false;
  }


  private final XynaPropertyBoolean compressMDMJar = new XynaPropertyBoolean("xdev.xlibdev.codeaccess.mdmjar.compress", false)
      .setDefaultDocumentation(DocumentationLanguage.EN, "If CodeAccess is used, this flag decides if the checked in mdm.jar will be compressed or not. To deactivate compression may be helpful for less memory consumption in a repository type that stores each change (e.g. SVN).");


  private void updateMdmJar(List<GenerationBase> gbList, RepositoryTransaction transaction) {
    String mdmJar = getRelativePathOfMdmJar();
    File mdmJarFile = new File(getProjectDir() + Constants.FILE_SEPARATOR + mdmJar);
    if (!mdmJarFile.exists()) {
      createMdmJar(mdmJarFile);
      try {
        transaction.add(new String[] {mdmJar}, RecursionDepth.TARGET_ONLY);
      } catch (XDEV_RepositoryAccessException e) {
        throw new RuntimeException(e);
      }
    } else {
      List<String> filesToCompile = new ArrayList<String>();
      final List<Pair<String, String>> names = new ArrayList<Pair<String, String>>(); //Liste mit Paaren (simpleName, pathName)
      
      for (GenerationBase gb : gbList) {
        String fileName = generateJavaStub(gb);
        if (fileName != null) {
          filesToCompile.add(fileName);
          names.add(Pair.of(gb.getSimpleClassName(), GenerationBase.getPackageNameFromFQName(gb.getFqClassName()).replaceAll("\\.", Constants.fileSeparator)));
        }
      }
      
      compileJavaStubs(filesToCompile.toArray(new String[filesToCompile.size()]));
      
      try {
        FileUtils.substituteOrAddFilesInZipFile(mdmJarFile, new File(getJavaClassFolder()), new FilenameFilter() {
  
          public boolean accept(File path, String name) {
            if (new File(path, name).isDirectory()) {
              return true;
            }
            
            for (Pair<String, String> pair : names) {
              if (name.endsWith(".class") && name.startsWith(pair.getFirst()) && path.getAbsolutePath().endsWith(pair.getSecond())) {
                //soweit so gut. nun könnte es aber noch eine klasse sein, die mit dem gleichen namen anfängt (IP vs IPv6).
                //also sicherstellen, dass danach entweder ein ".", oder ein "$" kommt.
                
                char c = name.charAt(pair.getFirst().length());
                if (c == '.' || c == '$') {
                  return true;
                }
              }
            }
            return false;
          }
        }, compressMDMJar.get());
      } catch (Ex_FileAccessException e) {
        throw new RuntimeException(e);
      } finally {
        FileUtils.deleteDirectoryRecursively(new File(getJavaClassFolder()));
        FileUtils.deleteDirectoryRecursively(new File(getJavaGenerationFolder()));
      }
    }
  }

  
  private void removeFromMdmJar(List<Triple<String, XMOMType, Long>> toRemove, GenerationBaseCache cache, RepositoryTransaction transaction) {
    String mdmJar = getRelativePathOfMdmJar();    
    File mdmJarFile = new File(getProjectDir() + Constants.FILE_SEPARATOR + mdmJar);
    if (mdmJarFile.exists()) {
      RuntimeContextDependencyManagement dependencyManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
      .getRuntimeContextDependencyManagement();
      try {
        final List<String> fqPathNameList = new ArrayList<>();
        List<GenerationBase> gbListToAdd = new ArrayList<>();
        List<Long> usedRevisionsSorted = null;
        for (Triple<String, XMOMType, Long> removed : toRemove) {
          String fqXmlName = removed.getFirst();
          XMOMType type = removed.getSecond();
          Set<Long> allRevisionsDefiningXMOMObject = dependencyManagement.getAllRevisionsDefiningXMOMObject(fqXmlName, removed.getThird());
          String fqClassName = GenerationBase.transformNameForJava(fqXmlName);
          fqPathNameList.add(fqClassName.replaceAll("\\.", Constants.fileSeparator));
          if (!allRevisionsDefiningXMOMObject.isEmpty()) {
            /*
             * falls das objekt ein duplikat war, sicherstellen, dass das andere objekt im mdmjar ist
             */
            if (usedRevisionsSorted == null) {
              usedRevisionsSorted = getUsedRevisionsSorted(dependencyManagement);
            }
            long rev = getLowestRevision(usedRevisionsSorted, allRevisionsDefiningXMOMObject);
            if (type == XMOMType.DATATYPE) {
              gbListToAdd.add(DOM.getOrCreateInstance(fqXmlName, cache, rev));
            } else if (type == XMOMType.EXCEPTION) {
              gbListToAdd.add(ExceptionGeneration.getOrCreateInstance(fqXmlName, cache, rev));
            } else {
              throw new RuntimeException();
            }
          }
        }
        
        FileUtils.removeFromZipFile(mdmJarFile, new FileFilter() {
          
          public boolean accept(File pathname) {
            String path = pathname.getPath();
            for (String fqPathName : fqPathNameList) {
              if (path.endsWith(".class") && path.startsWith(fqPathName)) {
                //soweit so gut. nun könnte es aber noch eine klasse sein, die mit dem gleichen namen anfängt (IP vs IPv6).
                //also sicherstellen, dass danach entweder ein ".", oder ein "$" kommt.
                
                char c = path.charAt(fqPathName.length());
                if (c == '.' || c == '$') {
                  return true;
                }
              }
            }
            return false;
          }
        }, compressMDMJar.get());
        
        if (!gbListToAdd.isEmpty()) {
          for (GenerationBase gb : gbListToAdd) {
            try {
              gb.parseGeneration(false, false);
            } catch (XynaException e) {
              logger.warn("Could not parse " + gb.getOriginalFqName(), e);
            }
          }
          updateMdmJar(gbListToAdd, transaction);
        }
      } catch (Ex_FileAccessException e) {
        throw new RuntimeException(e);
      } catch (XPRC_InvalidPackageNameException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  /**
   * required revisions so sortieren, dass zuerst die direkten kinder kommen, und ganz am ende die kinder, die am weitesten weg sind.
   * dabei sind keine doppelten in der liste. falls doppelte vorhanden wären, werden die hintersten kopien bevorzugt.
   */
  private List<Long> getUsedRevisionsSorted(RuntimeContextDependencyManagement dependencyManagement) {
    List<Long> usedByCodeAccess = new ArrayList<>();
    Set<Long> findChildrenOfThese = new HashSet<>();
    findChildrenOfThese.add(revision);
    while (!findChildrenOfThese.isEmpty()) {
      List<Long> tmp = new ArrayList<>(findChildrenOfThese);
      findChildrenOfThese.clear();
      for (long r : tmp) {
        Set<Long> deps = dependencyManagement.getDependencies(r);
        findChildrenOfThese.addAll(deps);
      }
      for (long r : findChildrenOfThese) {
        //doppelte vorne entfernen
        usedByCodeAccess.remove((Object) r);
      }
      usedByCodeAccess.addAll(findChildrenOfThese);
    }
    return usedByCodeAccess;
  }


  /**
   * gibt die revision der @someRevisionsUsedByCodeAccessRevision zurück, die am weitesten weg ist von der codeaccess-revision 
   */
  private Long getLowestRevision(List<Long> usedRevisionsSorted, Set<Long> someRevisionsUsedByCodeAccessRevision) {
    int highestIndex = -2;
    long lowestRevision = -5;
    for (long rev : someRevisionsUsedByCodeAccessRevision) {
      int idx = usedRevisionsSorted.indexOf(rev);
      if (idx > highestIndex) {
        highestIndex = idx;
        lowestRevision = rev;
      }
    }
    return lowestRevision;
  }


  private void createMdmJar(File mdmJarFile) {
    RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();

    //in das mdm.jar müssen auch die XMOMClasses aus allen (rekursiv) verwendeten
    //RuntimeContexten eingepackt werden
    Set<Long> allRelevantRevisions = new HashSet<Long>();
    rcdm.getDependenciesRecursivly(revision, allRelevantRevisions);
    allRelevantRevisions.add(revision);
    
    File javaClassFolder = new File(getJavaClassFolder());
    try {
      //XMOMClasses kompilieren bzw. einsammeln
      for (long rev : allRelevantRevisions) {
        if (revMgmt.isApplicationRevision(rev)) {
          //für Applications das xmomclasses-Verzeichnis kopieren (da in Applications immer alles kompiliert sein sollte)
          String xmomClassDir = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, rev);
          FileUtils.copyRecursivelyWithFolderStructure(new File(xmomClassDir), javaClassFolder);
        } else {
          //für Workspaces neu kompilieren
          compileMdm(rev);
        }
      }
      
      //jar bauen
      Support4Eclipse.createJarFile(builder.createManifest(null), mdmJarFile, new File(getJavaClassFolder()), compressMDMJar.get());
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException(e);
    } finally {
      FileUtils.deleteDirectoryRecursively(new File(getJavaClassFolder()));
      FileUtils.deleteDirectoryRecursively(new File(getJavaGenerationFolder()));
    }
  }

  private void compileMdm(long revision) {
    GenerationBaseCache generationCache = new GenerationBaseCache();
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<XMOMDomDatabaseEntry> allDatatypes = con.loadCollection(XMOMDomDatabaseEntry.class);
      for (XMOMDomDatabaseEntry domEntry : allDatatypes) {
        if (domEntry.getRevision().equals(revision)) {
          DOM.getOrCreateInstance(domEntry.getFqname(), generationCache, revision);
        }
      }
      Collection<XMOMExceptionDatabaseEntry> allExceptions = con.loadCollection(XMOMExceptionDatabaseEntry.class);
      for (XMOMExceptionDatabaseEntry exceptionEntry : allExceptions) {
        if (exceptionEntry.getRevision().equals(revision)) {
          ExceptionGeneration.getOrCreateInstance(exceptionEntry.getFqname(), generationCache, revision);
        }
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        
      }
    }
    
    for (GenerationBase gb : generationCache.values(revision)) {
      try {
        gb.parseGeneration(false, false);
      } catch (XPRC_InheritedConcurrentDeploymentException e) {
        throw new RuntimeException(e);
      } catch (AssumedDeadlockException e) {
        throw new RuntimeException(e);
      } catch (XPRC_MDMDeploymentException e) {
        logger.warn("could not parse " + gb.getOriginalFqName(), e);
      }
    }

    List<String> filesToCompile = new ArrayList<String>();
    for (GenerationBase gb : generationCache.values(revision)) {
      if (gb instanceof DOM || gb instanceof ExceptionGeneration) {
        String fileName = generateJavaStub(gb);
        if (fileName != null) {
          filesToCompile.add(fileName);
        }
      }
    }
    
    compileJavaStubs(filesToCompile.toArray(new String[filesToCompile.size()]));
  }
  
  
  private String generateJavaStub(GenerationBase gb) {
    if (!gb.isReservedServerObject() && gb.exists()) {
      CodeBuffer cb = new CodeBuffer(XynaDevelopment.DEFAULT_NAME);
      try {
        if (gb instanceof DOM) {
          DOM dom = ((DOM) gb);
          XynaObjectCodeGenerator xocg = new XynaObjectCodeGenerator(dom);
          xocg.generateJavaStub(cb);
        } else if (gb instanceof ExceptionGeneration) {
          XynaExceptionCodeGenerator xecg = new XynaExceptionCodeGenerator((ExceptionGeneration) gb);
          xecg.generateJava(cb);
        } else {
          throw new RuntimeException("unexpected type: " + gb);
        }
      } catch (Throwable t) {
        logger.debug("Exception creating Java Stub for " + gb.getFqClassName(), t);
        return null;
      }
      StringBuilder sb = new StringBuilder(getJavaGenerationFolder());
      sb.append(Constants.fileSeparator)
        .append(gb.getFqClassName().replaceAll("\\.", Constants.fileSeparator)).append(".java");
      try {
        GenerationBase.save(new String[] {cb.toString()}, new File(sb.toString()));
      } catch (Ex_FileAccessException e) {
        throw new RuntimeException(e);
      }
      return sb.toString();
    } else {
      return null;
    }
  }
  
  

  private void compileJavaStubs(String[] javaFiles) {
    new File(getJavaClassFolder()).mkdirs();
    StringBuilder classPath = new StringBuilder();
    builder.appendJarsServerLib(classPath);
    builder.appendJarsUserLib(classPath);
    String runtimecontextDependencies = "";
    Set<Long> deps = new HashSet<Long>();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(revision, deps);
    if (deps.size() > 0) {
      for (Long dep : deps) {
        runtimecontextDependencies += Constants.PATH_SEPARATOR + RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, dep);
      }
    }
    classPath.append(Constants.PATH_SEPARATOR).append(getProjectDir() + Constants.fileSeparator + getRelativePathOfMdmJar()
                                                          + runtimecontextDependencies); // does it matter that we ourself might already be contained in that jar...    
    try {
      builder.compile("mdm.jar", javaFiles, classPath.toString(), getJavaClassFolder(), getJavaGenerationFolder());
    } catch (XPRC_CompileError e) {
      //einzeln kompilieren versuchen
      for (String javaFile : javaFiles) {
        try {
          builder.compile("mdm.jar", new String[] {javaFile}, classPath.toString(), getJavaClassFolder(), getJavaGenerationFolder());
        } catch (XPRC_CompileError e2) {
          logger.debug("compileerror compiling javastub for mdmjar", e2);
        }
      }
    }
  }
  
  
  private void unlockMdmJar() {
    mdmLock.release();
  }

  private void lockMdmJar() {
    try {
      mdmLock.acquire();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  
  public void rebuildAndCommitMDMJar(String clientIdentifier, String commitMessage) throws XDEV_RepositoryAccessException {
    final String mdmjar = getRelativePathOfMdmJar();
    lockMdmJar();
    try {
      boolean success = false;
      RepositoryTransaction transaction = repositoryAccess.beginTransaction(clientIdentifier);
      try {
        String mdmJarPath = getProjectDir() + Constants.FILE_SEPARATOR + getRelativePathOfMdmJar();
        File mdmJarFile = new File(mdmJarPath);
        boolean firstCreation = !mdmJarFile.exists();
        createMdmJar(mdmJarFile);
        if (firstCreation) {
          transaction.setTransactionProperty("force", Boolean.TRUE);
          transaction.add(new String[] {mdmJarPath}, RecursionDepth.TARGET_ONLY);
        }
        try {
          transaction.commit(new String[] {mdmjar}, commitMessage, RecursionDepth.FULL_RECURSION);
        } catch (XDEV_UnversionedParentException e) {
          transaction.commit(new String[] {Support4Eclipse.COMMON_FOLDER}, commitMessage, RecursionDepth.FULL_RECURSION);
        }
        success = true;
      } finally {
        endTransaction(transaction, !success);
      }
    } finally {
      unlockMdmJar();
    }
  }


  public void handleProjectEvents(Collection<? extends ProjectCreationOrChangeEvent> events) {
    Collection<XMOMChangeEvent> domOrGenerationEvents = new ArrayList<XMOMChangeEvent>(); 
    Collection<XMOMDeleteEvent> xmomDeleteEvents = new ArrayList<XMOMDeleteEvent>(); 
    for (ProjectCreationOrChangeEvent event : events) {
      switch (event.getType()) {
        case FILTER_CREATION :
          FilterCreationEvent fce = (FilterCreationEvent) event;
          createFilter(fce.getObjectIdentifier(), fce.getTriggerName());
          break;
        case TRIGGER_CREATION :
          createTrigger(((BasicProjectCreationOrChangeEvent)event).getObjectIdentifier());
          break;
        case XMOM_MODIFICATION :
        case XMOM_MOVE :
          XMOMChangeEvent xmomEvent = (XMOMChangeEvent) event;
          if (xmomEvent.getXMOMType() == XMOMType.DATATYPE ||
              xmomEvent.getXMOMType() == XMOMType.EXCEPTION) {
            
            domOrGenerationEvents.add(xmomEvent);
          }
          break;
        case XMOM_DELETE :
          XMOMDeleteEvent xmomDeleteEvent = (XMOMDeleteEvent) event;
          if (xmomDeleteEvent.getXMOMType() == XMOMType.DATATYPE ||
              xmomDeleteEvent.getXMOMType() == XMOMType.EXCEPTION) {
            xmomDeleteEvents.add(xmomDeleteEvent);
          }
          break;
        case RUNTIMECONTEXT_DEPENDENCY_MODIFICATION:
          buildAlgorithm.rebuildMDMJar(getIdentityString());
          buildAlgorithm.rebuildAll();
          buildExecutor.requestExecution();
          break;
        default :
          break;
      }
    }
    if (domOrGenerationEvents.size() > 0) {
      bulkRebuildServices(domOrGenerationEvents);
      buildAlgorithm.addChangeEvents(domOrGenerationEvents);
      buildExecutor.requestExecution();
    }
    if (xmomDeleteEvents.size() > 0) {
      deleteXmom(xmomDeleteEvents);
      buildAlgorithm.rebuildAll(); //da von gelöschten Objekten keine Dependencies mehr ermittelt werden können, muss alles neu gebaut werden
      buildExecutor.requestExecution();
    }
  }


  private void bulkRebuildServices(Collection<XMOMChangeEvent> events) {
    
    Map<String, Triple<XMOMChangeEvent, DOM, ComponentCodeChange>> components =
                    new HashMap<String, Triple<XMOMChangeEvent, DOM, ComponentCodeChange>>();
    
    rebuildMdmJarAndPrepareCodedServices(events, components);
    
    if (!components.isEmpty()) {
      updateServices(components);
    }
  }
  
  private void deleteXmom(Collection<XMOMDeleteEvent> events) {
    
    //mdm.jar updaten
    rebuildMdmJarAndPrepareCodedServices(events, null);
    
    //ServiceGroups der eigenen Revision löschen
    List<String> servicesToDelete = new ArrayList<String>();
    for (XMOMDeleteEvent event : events) {
      if (event.getXMOMType() == XMOMType.DATATYPE && event.getRevision() == revision) {
        servicesToDelete.add(event.getObjectIdentifier());
      }
    }
    
    if (!servicesToDelete.isEmpty()) {
      deleteServices(servicesToDelete);
    }
  }

  private void updateServices(final Map<String, Triple<XMOMChangeEvent, DOM, ComponentCodeChange>> components) {
    Iterator<String> componentKeyIterator = components.keySet().iterator();
    while (componentKeyIterator.hasNext()) {
      Triple<XMOMChangeEvent, DOM, ComponentCodeChange> component = components.get(componentKeyIterator.next());
      try {
        ComponentCodeChange ccc = prepareSource(component.getSecond());
        if (ccc != null) {
          component.setThird(ccc);
        } else {
          componentKeyIterator.remove();
        }
      } catch (Exception e) {
        logger.warn("Failed to prepare source for " + component.getFirst().getObjectIdentifier(), e);
        return;
      }
    }
    
    final String clientIdentifier = getIdentityString();
    
    threadpoolForFactoryChanges.execute(new XynaRunnable() {
    
      public void run() {
        boolean success = false;
        lockRepository();
        try {
          RepositoryTransaction transaction = repositoryAccess.beginTransaction(clientIdentifier);
          try {
            List<String> dirsToCommit = new ArrayList<String>();

            StringBuilder commitMsg = new StringBuilder();
            for (Entry<String, Triple<XMOMChangeEvent, DOM, ComponentCodeChange>> entry : components.entrySet()) {
              //originalFqName statt fqClassName verwenden, da ansonsten beim Auschecken nicht mehr der originalFqName fürs Deployment ermittelt werden kann
              String originalFqNameOfDatatype = entry.getKey();
              
              String serviceSubDir = ComponentType.CODED_SERVICE.getProjectSubFolder() + Constants.FILE_SEPARATOR + originalFqNameOfDatatype;
              String serviceAbsoluteDirPath = getProjectDir() + Constants.FILE_SEPARATOR + serviceSubDir;
              File f = new File(serviceAbsoluteDirPath);
              
              ensurePathExistsInRepository(f.getParentFile(), ComponentType.CODED_SERVICE.getProjectSubFolder(), transaction);
              boolean firstCreationOfService = isUnversioned(transaction, serviceSubDir);
              try {
                if (firstCreationOfService) {
                  transaction.add(new String[] {serviceSubDir}, RecursionDepth.TARGET_AND_DIRECT_CHILDREN);
                  transaction.add(new String[] {serviceSubDir + Constants.FILE_SEPARATOR + "lib",
                                                serviceSubDir + Constants.FILE_SEPARATOR + "src"}, RecursionDepth.FULL_RECURSION);
                  dirsToCommit.add(serviceSubDir);
                } else {
                  transaction.setTransactionProperty("force", true);
                  try {
                    //ok dann sind die dateien modified und werden einfach nur committed.
                    //die impl klasse und die buildfiles etc will man nicht erneut einchecken - nur die interfaces! 
                    //den rest reverten, damit es bei svn updates nicht zu problemen kommt
                    transaction.revert(new String[] {serviceSubDir + Constants.FILE_SEPARATOR + "src",
                        serviceSubDir + Constants.FILE_SEPARATOR + "Exceptions.xml",
                        serviceSubDir + Constants.FILE_SEPARATOR + ".project",
                        serviceSubDir + Constants.FILE_SEPARATOR + "build.xml",
                        serviceSubDir + Constants.FILE_SEPARATOR + ".classpath"}, RecursionDepth.FULL_RECURSION);
                    
                    //evtl wurden aber noch neue files hinzugefügt, die verschwinden beim revert nicht.
                    transaction.add(new String[] {serviceSubDir + Constants.FILE_SEPARATOR + "src",
                                                  serviceSubDir + Constants.FILE_SEPARATOR + "lib"}, RecursionDepth.FULL_RECURSION);
                    
                    dirsToCommit.add(serviceSubDir);
                  } finally {
                    transaction.setTransactionProperty("force", false);
                  }
                }
                commitMsg.append("ServiceGroup '")
                  .append(originalFqNameOfDatatype)
                  .append("' ")
                  .append(firstCreationOfService ? "created" : "modified")
                  .append(".")
                  .append(Constants.LINE_SEPARATOR);
              } catch (XDEV_RepositoryAccessException e) {
                repositoryUpstreamFailure = e;
                logger.warn("Error during svn transaction", e);
                return;
              }
            }
            if (dirsToCommit.size() > 0) {
              try {
                transaction.commit(dirsToCommit.toArray(new String[dirsToCommit.size()]),
                                   commitMsg.toString(), RecursionDepth.FULL_RECURSION);
              } catch (XDEV_RepositoryAccessException e) {
                repositoryUpstreamFailure = e;
                logger.warn("Error during svn transaction", e);
                return;
              }
            }
            success = true;
          } finally {
            endTransaction(transaction, !success);
          }
        } finally {
          unlockRepository();
        }
      }
    });
  }

  private void deleteServices(final List<String> originalFqNameOfDatatypes) {
    final String clientIdentifier = getIdentityString();
    
    threadpoolForFactoryChanges.execute(new XynaRunnable() {
      
      public void run() {
        boolean success = false;
        lockRepository();
        try {
          RepositoryTransaction transaction = repositoryAccess.beginTransaction(clientIdentifier);
          try {
            List<String> dirsToCommit = new ArrayList<String>();
            
            StringBuilder commitMsg = new StringBuilder();
            transaction.setTransactionProperty("force", true); //force, damit auch gelöscht wird, falls unversionierte Elemente enthalten sind
            try {
              for (String originalFqNameOfDatatype : originalFqNameOfDatatypes) {
                String serviceSubDir = ComponentType.CODED_SERVICE.getProjectSubFolder() + Constants.fileSeparator + originalFqNameOfDatatype;
                String serviceAbsoluteDirPath = getProjectDir() + Constants.FILE_SEPARATOR + serviceSubDir;
                File f = new File(serviceAbsoluteDirPath);
                
                try {
                  if (f.exists()) {
                    transaction.delete(new String[] {serviceSubDir});
                    
                    commitMsg.append("ServiceGroup '")
                      .append(originalFqNameOfDatatype)
                      .append("' deleted.")
                      .append(Constants.LINE_SEPARATOR);
                    
                    dirsToCommit.add(serviceSubDir);
                  }
                
                } catch (XDEV_RepositoryAccessException e) {
                  repositoryUpstreamFailure = e;
                  logger.warn("Error during svn transaction", e);
                  return;
                }
              }
            } finally {
              transaction.setTransactionProperty("force", false);
            }
            
            if (dirsToCommit.size() > 0) {
              try {
                transaction.commit(dirsToCommit.toArray(new String[dirsToCommit.size()]),
                                   commitMsg.toString(), RecursionDepth.FULL_RECURSION);
              } catch (XDEV_RepositoryAccessException e) {
                repositoryUpstreamFailure = e;
                logger.warn("Error during svn transaction", e);
                return;
              }
            }
            success = true;
          } finally {
            endTransaction(transaction, !success);
          }
        } finally {
          unlockRepository();
        }
      }
    });
  }

  // insert transaction from outside and commit batched
  private void prepareLocalRepository(XMOMChangeEvent event, RepositoryTransaction transaction, List<String> dirsToCommit, StringBuilder commitMsg) throws XDEV_RepositoryAccessException, XPRC_InvalidPackageNameException, Ex_FileWriteException {
    if (event.getType() == EventType.XMOM_MOVE) {
      XMOMMovementEvent dme = (XMOMMovementEvent) event;
      String oldFqClassNameOfDatatype = GenerationBase.transformNameForJava(dme.getOldFqName());
      String oldServiceSubDir = ComponentType.CODED_SERVICE.getProjectSubFolder() + Constants.fileSeparator + dme.getOldFqName();
      
      String newFqClassNameOfDatatype = GenerationBase.transformNameForJava(dme.getObjectIdentifier());
      String newServiceSubDir = ComponentType.CODED_SERVICE.getProjectSubFolder() + Constants.fileSeparator + dme.getObjectIdentifier();
      
      transaction.move(new String[] {oldServiceSubDir}, newServiceSubDir);
      
      String oldPackageName = GenerationBase.getPackageNameFromFQName(oldFqClassNameOfDatatype).replaceAll("\\.", Constants.fileSeparator);
      String oldSrcPart = "src" + Constants.fileSeparator + oldPackageName;
      String newPackageName = GenerationBase.getPackageNameFromFQName(newFqClassNameOfDatatype).replaceAll("\\.", Constants.fileSeparator);
      String newSrcPart = "src" + Constants.fileSeparator + newPackageName;
      if (!oldSrcPart.equals(newSrcPart)) {
        String oldImplDir = newServiceSubDir + Constants.fileSeparator + oldSrcPart;
        String newImplDir = newServiceSubDir + Constants.fileSeparator + newSrcPart;
        transaction.move(new String[] {oldImplDir}, newImplDir);
      }
      String oldSimpleName = GenerationBase.getSimpleNameFromFQName(oldFqClassNameOfDatatype);
      String newSimpleName = GenerationBase.getSimpleNameFromFQName(newFqClassNameOfDatatype);
      if (!oldSimpleName.equals(newSimpleName)) {
        String oldServiceOperationPath = newServiceSubDir + Constants.fileSeparator + newSrcPart + Constants.fileSeparator + "impl" + Constants.fileSeparator + oldSimpleName + JavaServiceImplementation.STATIC_OPERATION_IMPL_SUFFIX + ".java";
        File oldServiceOperationFile = new File(getProjectDir() + Constants.fileSeparator + oldServiceOperationPath);
        if (oldServiceOperationFile.exists()) {
          String newServiceOperationPath = newServiceSubDir + Constants.fileSeparator + newSrcPart + Constants.fileSeparator + "impl" + Constants.fileSeparator + newSimpleName + JavaServiceImplementation.STATIC_OPERATION_IMPL_SUFFIX + ".java";
          transaction.move(new String[] {oldServiceOperationPath}, newServiceOperationPath);
        }
        
        String oldInstanceOperationPath = newServiceSubDir + Constants.fileSeparator + newSrcPart + Constants.fileSeparator + "impl" + Constants.fileSeparator + oldSimpleName + JavaServiceImplementation.NONSTATIC_OPERATION_IMPL_SUFFIX + ".java";
        File oldInstanceOperationFile = new File(getProjectDir() + Constants.fileSeparator + oldInstanceOperationPath);
        if (oldInstanceOperationFile.exists()) {
          String newInstanceOperationPath = newServiceSubDir + Constants.fileSeparator + newSrcPart + Constants.fileSeparator + "impl" + Constants.fileSeparator + newSimpleName + JavaServiceImplementation.NONSTATIC_OPERATION_IMPL_SUFFIX + ".java";
          transaction.move(new String[] {oldInstanceOperationPath}, newInstanceOperationPath);
        }
      }
      
      String projectDefinitionPath = newServiceSubDir + Constants.fileSeparator + ".project";
      File projectDefinitionFile = new File(getProjectDir() + Constants.fileSeparator + projectDefinitionPath);
      if (projectDefinitionFile.exists()) {
        String projectFile = FileUtils.readFileAsString(projectDefinitionFile);
        projectFile = projectFile.replaceAll("<name>" + oldSimpleName + "Impl</name>", "<name>" + newSimpleName + "Impl</name>");
        FileUtils.writeStringToFile(projectFile, projectDefinitionFile);
      }
      
      String classpathPath = newServiceSubDir + Constants.fileSeparator + ".classpath";
      File classpathFile = new File(getProjectDir() + Constants.fileSeparator + classpathPath);
      if (classpathFile.exists()) {
        String classpath = FileUtils.readFileAsString(classpathFile);
        classpath = classpath.replaceAll("/resource/" + oldSimpleName + "Impl", "/resource/" + newSimpleName + "Impl");
        FileUtils.writeStringToFile(classpath, classpathFile);
      }
      
      
      commitMsg.append("ServiceGroup '")
        .append(dme.getOldFqName())
        .append("' moved to ")
        .append(dme.getObjectIdentifier())
        .append(".")
        .append(Constants.LINE_SEPARATOR);
      dirsToCommit.add(oldServiceSubDir);
      dirsToCommit.add(newServiceSubDir);
    }
    
  }
  
  

  
  private ComponentCodeChange prepareSource(DOM dom) throws Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidPackageNameException, XDEV_InvalidProjectTemplateParametersException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH {
    final String serviceSubDir = ComponentType.CODED_SERVICE.getProjectSubFolder() + Constants.FILE_SEPARATOR + dom.getOriginalFqName();
    String serviceAbsoluteDirPath = getProjectDir() + Constants.FILE_SEPARATOR + serviceSubDir;
    final File f = new File(serviceAbsoluteDirPath);
                    
    File xmlSourceFile = new File(GenerationBase.getFileLocationOfXmlNameForSaving(dom.getOriginalFqName(), dom.getRevision()) + ".xml");
    Document doc = XMLUtils.parse(xmlSourceFile);
    DOM.addLibraryTagAndCodeSnippetInXML(doc, dom, false, true);
    
    ServiceImplementationTemplate template = new ServiceImplementationTemplate(dom.getOriginalFqName(), revision);
    lockTemplate(ComponentType.CODED_SERVICE);
    try {
      Support4Eclipse.buildProjectTemplate(f.getParentFile(), template, false);                       
    } finally {
      unlockTemplate(ComponentType.CODED_SERVICE);
    }
      
    File xmlFileTarget =
      new File(serviceAbsoluteDirPath, "xmldefinition" + File.separator
          + GenerationBase.getSimpleNameFromFQName(dom.getOriginalFqName()) + ".xml");
    XMLUtils.saveDom(xmlSourceFile, doc);
    FileUtils.copyFile(xmlSourceFile, xmlFileTarget);
    
    return createComponentCodeChangeForNewService(dom.getOriginalFqName());
  }

  ComponentCodeChange createComponentCodeChangeForNewService(String originalFqName) {
    String serviceSubDir = ComponentType.CODED_SERVICE.getProjectSubFolder() + Constants.FILE_SEPARATOR + originalFqName;
    String serviceAbsoluteDirPath = getProjectDir() + Constants.FILE_SEPARATOR + serviceSubDir;
    File f = new File(serviceAbsoluteDirPath);
    List<File> allJavaFiles = new ArrayList<File>();
    FileUtils.findFilesRecursively(f, allJavaFiles, XynaComponentBuilder.javaFilter);
    ComponentCodeChange ccc = new ComponentCodeChange(originalFqName, ComponentType.CODED_SERVICE, ModificationType.Modified);
    for (File javaFile : allJavaFiles) {
      ccc.addModifiedJavaFiles(new RepositoryItemModification(javaFile.getAbsolutePath(), RepositoryModificationType.Updated));
    }
    return ccc;
  }

  ComponentCodeChange createComponentCodeChangeForFilter(String filterName) {
    String filterSubDir = ComponentType.FILTER.getProjectSubFolder() + Constants.FILE_SEPARATOR + filterName;
    String filterAbsoluteDirPath = getProjectDir() + Constants.FILE_SEPARATOR + filterSubDir;
    File f = new File(filterAbsoluteDirPath);
    List<File> allJavaFiles = new ArrayList<File>();
    FileUtils.findFilesRecursively(f, allJavaFiles, XynaComponentBuilder.javaFilter);
    ComponentCodeChange ccc = new ComponentCodeChange(filterName, ComponentType.FILTER, ModificationType.Modified);
    for (File javaFile : allJavaFiles) {
      ccc.addModifiedJavaFiles(new RepositoryItemModification(javaFile.getAbsolutePath(), RepositoryModificationType.Updated));
    }
    return ccc;
  }
  
  
  private void fillComponentCodeChangeForNewComponent(ComponentCodeChange ccc) {
    String path = getProjectDir() + File.separatorChar + ccc.getBasePath();
    File f = new File(path);
    List<File> allJavaFiles = new ArrayList<File>();
    FileUtils.findFilesRecursively(f, allJavaFiles, XynaComponentBuilder.javaFilter);
    for (File javaFile : allJavaFiles) {
      ccc.addModifiedJavaFiles(new RepositoryItemModification(javaFile.getAbsolutePath(), RepositoryModificationType.Added));
    }
  }
  

  
  private void endTransaction(RepositoryTransaction transaction, boolean rollback) {
    //TODO Es wäre schöner, wenn das Rollback für nicht commitete Anteile der Transaction in
    //transaction.endTransaction() ausgeführt wird. Dann wird der Rollback-Teil hier
    //nicht mehr benötigt. Da bei svn updates aber kein svn commit ausgeführt wird,
    //muss dann noch eine neue commit-Methode ergänzt werden, die die uncommited-Liste leert.
    //In XMOMAccess muss die endTransaction-Methode dann ebenfalls angepasst werden.
    try {
      if (rollback) {
        try {
          transaction.rollback();
        } catch (XDEV_RepositoryAccessException e) {
          throw new RuntimeException(e);
        }
      }
    } finally {
      try {
        transaction.endTransaction();
      } catch (XDEV_RepositoryAccessException e) {
        logger.warn("Failed to end transaction", e);
      }
    }
  }


  public boolean retryBuild(String failedComponentName, boolean deploy) {
    BuildFailure failure = buildFailures.get(failedComponentName);
    if (failure == null) {
      throw new RuntimeException("Component '" + failedComponentName + "' does not appear to have failed a build");
    } else {
      return build(failure.getComponent(), deploy);
    }
  }


  public Set<String> retryAllBuilds(boolean deploy) {
    Set<String> rebuild = new HashSet<String>();
    for (String key : buildFailures.keySet()) {
      if (retryBuild(key, deploy)) {
        rebuild.add(key);
      }
    }
    return rebuild;
  }
  

  @Override
  public String toString() {
    return name + " repositoryAccess=" + repositoryAccess.getName(); 
  }


  public void restoreBuildFailures(Collection<BuildFailure> failures) {
    for (BuildFailure buildFailure : failures) {
      buildFailures.put(buildFailure.component.getComponentOriginalName(), buildFailure);
    }
  }
  
  
  private String getIdentityString() {
    Identity identity = XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.get();
    if (identity == null) {
      return "CLI access";
    } else {
      return identity.toString();
    }
  }
  
  
  private void ensureCommonLibs() {
    String commonLibPath = getProjectDir() + File.separatorChar + ComponentType.GLOBAL_LIB.getProjectSubFolder();
    File commonLibFolder = new File(commonLibPath);
    if (!commonLibFolder.exists()) {
      commonLibFolder.mkdirs();
      try {
        FileUtils.copyRecursively(builder.getSavedDir(new ComponentCodeChange("", ComponentType.GLOBAL_LIB)), commonLibFolder);
        FileUtils.copyRecursively(builder.getSavedDir(new ComponentCodeChange("", ComponentType.USER_LIB)), commonLibFolder);
      } catch (Ex_FileAccessException e) {
        logger.error("Failed to transfer factory libs to local repository",e);
      }
      for (ComponentType type : new ComponentType[] {ComponentType.TRIGGER, ComponentType.FILTER, ComponentType.SHARED_LIB, ComponentType.CODED_SERVICE}) {
        File subFolder = new File(getProjectDir() + File.separatorChar + type.getProjectSubFolder());
        subFolder.mkdirs();
      }
      String projectDefinition = createCommonLibProjectDefinition();
      try {
        File projectFile = new File(getProjectDir() + File.separatorChar + Support4Eclipse.COMMON_FOLDER + File.separatorChar + ".project");
        if (!projectFile.exists()) {
          projectFile.createNewFile();
        }
        FileUtils.writeStringToFile(projectDefinition, projectFile);
      } catch (Exception e) {
        logger.error("Failed to create CommonLibs project definition",e);
      }
      boolean success = false;
      RepositoryTransaction transaction = repositoryAccess.beginTransaction("CodeAcces initialization");
      try {
        transaction.setTransactionProperty("force", true);
        String[] paths = new String[] {Support4Eclipse.COMMON_FOLDER, ComponentType.TRIGGER.getProjectSubFolder(), ComponentType.FILTER.getProjectSubFolder(),
                                                 ComponentType.SHARED_LIB.getProjectSubFolder(), ComponentType.CODED_SERVICE.getProjectSubFolder()};
        transaction.add(paths, RecursionDepth.FULL_RECURSION);
        transaction.commit(paths, "Initial import of commonLibs", RecursionDepth.FULL_RECURSION);
        success = true;
      } catch (XDEV_RepositoryAccessException e) {
        repositoryUpstreamFailure = e;
        logger.error("Failed to add factory libs to repository", e);
      } finally {
        endTransaction(transaction, !success);
      }
    }
  }


  // TODO rather would like this to be lying around somewhere instead of being hard coded
  private String createCommonLibProjectDefinition() {
    String projectDefinition = 
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + Constants.LINE_SEPARATOR +
      "<projectDescription>" + Constants.LINE_SEPARATOR +
      "  <name>CommonLibs</name>" + Constants.LINE_SEPARATOR +
      "  <comment>Generated from the factory lib and userlib folder</comment>" + Constants.LINE_SEPARATOR +
      "  <projects>" + Constants.LINE_SEPARATOR +
      "  </projects>" + Constants.LINE_SEPARATOR +
      "  <buildSpec>" + Constants.LINE_SEPARATOR +
      "    <buildCommand>" + Constants.LINE_SEPARATOR +
      "      <name>org.eclipse.jdt.core.javabuilder</name>" + Constants.LINE_SEPARATOR +
      "      <arguments/>" + Constants.LINE_SEPARATOR +
      "    </buildCommand>" + Constants.LINE_SEPARATOR +
      "  </buildSpec>" + Constants.LINE_SEPARATOR +
      "  <natures>" + Constants.LINE_SEPARATOR +
      "    <nature>org.eclipse.jdt.core.javanature</nature>" + Constants.LINE_SEPARATOR +
      "  </natures>" + Constants.LINE_SEPARATOR +
      "</projectDescription>";
    return projectDefinition;
  }
  
  
  public boolean componentAlreadyExists(String componentName, ComponentType componentType) {
    if (componentType == ComponentType.TRIGGER && !componentName.endsWith("Trigger")) {
      componentName = componentName + "Trigger";
    }
    if (componentType == ComponentType.FILTER && !componentName.endsWith("Filter")) {
      componentName = componentName + "Filter";
    }
    
    ComponentCodeChange component = new ComponentCodeChange(componentName, componentType);

    String absoluteDirPath = getProjectDir() + Constants.FILE_SEPARATOR + component.getBasePath();
    File f = new File(absoluteDirPath);
    if (f.exists()) {
      return true;
    }
    
    return false;
  }
  
  
  public static interface FactoryAdditionRunnable {
    
    public void addToFactory(File[] jars); 
    
  }
  
}
