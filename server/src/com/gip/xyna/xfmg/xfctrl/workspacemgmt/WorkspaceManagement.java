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
package com.gip.xyna.xfmg.xfctrl.workspacemgmt;

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
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EmptyRepositoryEvent;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccessManagement;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccessManagement;
import com.gip.xyna.xdev.xlibdev.xmomaccess.XMOMAccess;
import com.gip.xyna.xdev.xlibdev.xmomaccess.XMOMAccessManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_ClearWorkingSetFailedBecauseOfRunningOrders;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildNewWorkspace;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveWorkspace;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.BasicApplicationName;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ClearWorkingSet;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ClearWorkingSet.RevisionContentBlackWhiteList;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RemoveApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl.OrderEntryInterfacesCouldNotBeClosedException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkingSetBlackListXynaProperties;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.SharedLibDeploymentAlgorithm;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarkerManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.rtctxmgmt.RuntimeContextManagement;
import com.gip.xyna.xfmg.xfctrl.rtctxmgmt.RuntimeContextChangeHandler;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.ApplicationName;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.CreateWorkspaceResult.Result;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.ClearWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.RemoveWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xmcp.PluginInformation;
import com.gip.xyna.xmcp.xguisupport.messagebus.Publisher;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XFMG_RuntimeContextStillReferencedException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_TimeoutWhileWaitingForUnaccessibleOrderException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.OrdersInUse;
import com.gip.xyna.xprc.xfractwfe.OrdersInUse.FillingMode;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;


public class WorkspaceManagement extends FunctionGroup{

  public static final String DEFAULT_NAME = "WorkspaceManagement";
  
  private static final Logger logger = CentralFactoryLogging.getLogger(WorkspaceManagement.class);

  private static Map<Long, WorkspaceBlackListXynaProperties> clearWorkspaceBlackListProperties;
  
  //Namen, die nicht für Workspaces verwendet werden dürfen:
  // "workingset" verboten wegen XynaProperty "xact.snmp.service.engineid.currentvalue.<workingset>.readonly"
  private static Set<String> illegalWorkspaceNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"workingset"})));


  public WorkspaceManagement() throws XynaException {
    super();
  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(WorkspaceManagement.class, "WorkspaceManagement.initAll").
          after(RevisionManagement.class).
          after(WorkflowDatabase.FUTURE_EXECUTION_ID). //für createWorkspace nötig
          execAsync( new Runnable() { public void run() { initAll();} });
  }
  
  @Override
  protected void shutdown() throws XynaException {
  }


  private void initAll() {
    WorkingSetBlackListXynaProperties.registerPropertiesDependency(DEFAULT_NAME);
    
    clearWorkspaceBlackListProperties = new ConcurrentHashMap<Long, WorkspaceBlackListXynaProperties>();
    
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Map<Long, Workspace> workspaces = revisionManagement.getWorkspaces();
    for (Long revision : workspaces.keySet()) {
      if (!revision.equals(RevisionManagement.REVISION_DEFAULT_WORKSPACE)) {
        WorkspaceBlackListXynaProperties blackList = new WorkspaceBlackListXynaProperties(workspaces.get(revision).getName());
        blackList.registerPropertiesDependency(DEFAULT_NAME);
        clearWorkspaceBlackListProperties.put(revision, blackList);
      }
    }
  }
  
  
  /**
   * Liefert alle Workspaces mit Informationen zum RepositoryAccess
   * @return
   */
  public List<WorkspaceInformation> listWorkspaces(boolean includeProblems) {
    List<WorkspaceInformation> result = new ArrayList<WorkspaceInformation>();
    
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Map<Long, Workspace> workspaces = revisionManagement.getWorkspaces();
    
    for (Workspace workspace : workspaces.values()) {
      WorkspaceInformation wsi;
      try {
        wsi = getWorkspaceDetails(workspace, includeProblems);
        result.add(wsi);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // ok, Workspace existiert nicht mehr
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
    }
    
    Collections.sort(result, new Comparator<WorkspaceInformation>() {

      public int compare(WorkspaceInformation o1, WorkspaceInformation o2) {
        return o1.getWorkspace().compareTo(o2.getWorkspace());
      }
      
    });
    
    return result;
  }
  
  
  /**
   * Liefert alle Workspaces mit Informationen zum RepositoryAccess
   * @return
   */
  public Map<Workspace, PluginInformation> listWorkspaces() {
    Map<Workspace, PluginInformation> ret = new TreeMap<Workspace, PluginInformation>();
    List<WorkspaceInformation> wsList = listWorkspaces(false);
    
    for (WorkspaceInformation wsi : wsList) {
      ret.put(wsi.getWorkspace(), wsi.getRepositoryAccess());
    }
    
    return ret;
  }
  
  
  public WorkspaceInformation getWorkspaceDetails(Workspace workspace, boolean includeProblems) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    WorkspaceInformation wsi = new WorkspaceInformation(workspace);
    
    //RepositoryAccess über CodeAccess bzw. XMOMAccess bestimmen
    Long revision = revisionManagement.getRevision(workspace);
    RepositoryAccess repositoryAccess = null;
    CodeAccess codeAccess = XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getCodeAccessManagement().getCodeAccessInstance(revision);
    if (codeAccess != null) {
      repositoryAccess = codeAccess.getRepositoryAccess();
    } else {
      XMOMAccess xmomAccess = XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getXMOMAccessManagement().getXMOMAccessInstance(revision);
      if (xmomAccess != null) {
        repositoryAccess = xmomAccess.getRepositoryAccess();
      }
    }
    
    PluginInformation pluginInformation = new PluginInformation();
    if (repositoryAccess != null) {
      pluginInformation.setName(repositoryAccess.getTypename());
      pluginInformation.setParamMap(repositoryAccess.getParamMap());
    }
    
    wsi.setRepositoryAccess(pluginInformation);
    
    //RuntimeContext Requirements und Problems
    Collection<RuntimeDependencyContext> requirements = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRequirements(workspace);
    wsi.setRequirements(requirements);

    WorkspaceState state;
    if (includeProblems) {
      List<RuntimeContextProblem> problems = new ArrayList<RuntimeContextProblem>(revisionManagement.getRuntimeContextProblems(workspace));
      wsi.setProblems(problems);
      //Zustand
      if (problems.isEmpty()) {
        state = WorkspaceState.OK;
      } else {
        state = WorkspaceState.WARNING;
        
        for (RuntimeContextProblem problem : problems) {
          if (problem.causeErrorStatus()) {
            //es gibt mindestens ein Problem, das zum Zustand ERROR führt
            state = WorkspaceState.ERROR;
            break;
          }
        }
      }
    } else {
      //wenn die gui den state wissen will, fragt sie auch immer die problems mit an
      //TODO für andere ist das nicht transparent
      state = WorkspaceState.OK; //eigtl unknown
    }
    
    wsi.setState(state);
    
    return wsi;
  }
  
  
  /**
   * Legt einen neuen Workspace an und kopiert dabei die Factory Komponenten aus dem
   * Default-Workspace
   * @throws XFMG_CouldNotBuildNewWorkspace
   */
  public CreateWorkspaceResult createWorkspace(Workspace workspace) throws XFMG_CouldNotBuildNewWorkspace{
    return createWorkspace(workspace, null);
  }
  
  /**
   * Legt einen neuen Workspace an und kopiert dabei die Factory Komponenten aus dem
   * Default-Workspace. Falls user ungleich null, wird ein Multi-User-Event geschickt.
   * @throws XFMG_CouldNotBuildNewWorkspace
   */
  public CreateWorkspaceResult createWorkspace(Workspace workspace, String user) throws XFMG_CouldNotBuildNewWorkspace{
    CreateWorkspaceResult result = new CreateWorkspaceResult();
    
    if (illegalWorkspaceNames.contains(workspace.getName())) {
      throw new IllegalArgumentException("'" + workspace.getName() + "' not allowed as workspaceName");
    }
    if (workspace.getName().trim().length() == 0) {
      throw new IllegalArgumentException("'" + workspace.getName() + "' not allowed as workspaceName");
    }
    
    if (workspace.getName().contains("\"")) {
      throw new IllegalArgumentException("workspaceName must not contain \"");
    }
    
    Long revision = null;
    try {
      //neue Revision anfordern
      RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      revision = revisionManagement.buildNewRevisionForNewWorkspace(workspace);
      
      //neues Revision-Verzeichnis anlegen
      RevisionManagement.createNewRevisionDirectory(revision);
      
      //Basis-Applications als Requirements hinzufügen,
      //falls keine Basis-Application existiert, werden die Factory Komponenten aus dem
      //Default-Workspace kopiert
      ApplicationManagementImpl applicationManagement =
                      (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
      Application basicApp = applicationManagement.getBasicApplication(BasicApplicationName.Processing);
      
      if (basicApp == null) {
        copyFactoryComponents(revision, result);
      } else {
        RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        rcdMgmt.modifyDependencies(workspace, new ArrayList<RuntimeDependencyContext>(Collections.singletonList(basicApp)), null, true, true);
      }
      
      //Workspace-abhängige XynaProperties anlegen
      createWorkspaceDependentXynaProperties(workspace, revision);
      
      if (result.getResult() == null) {
        result.setResult(Result.Success);
      }
      
      //Multi-User-Event für RuntimeContext Änderung
      Publisher publisher = new Publisher(user);
      publisher.publishRuntimeContextCreate(workspace);
    } catch (Throwable e) {
      Department.handleThrowable(e);
      result.setResult(Result.Failed);
      if (revision != null) {
        try {
          RemoveWorkspaceParameters params = new RemoveWorkspaceParameters();
          params.setCleanupXmls(true);
          removeWorkspace(workspace, params);
          logger.info("Removed workspace <" + workspace.getName() + "> because it could not be created successfully.");
        } catch (Exception e1) {
          logger.warn("Rollback failed. Workspace could not be deleted.", e1);
        }
      }
      if (e instanceof XFMG_CouldNotBuildNewWorkspace) {
        throw (XFMG_CouldNotBuildNewWorkspace) e;
      } else {
        throw new XFMG_CouldNotBuildNewWorkspace(workspace.getName(), e);
      }
    }
    
    for (RuntimeContextChangeHandler rdcch : getRtCtxMgmt().getHandlers()) {
      try {
        rdcch.creation(workspace);
      } catch (Throwable t) {
        logger.error("Could not execute RuntimeContextChangeHandler " + rdcch, t);
      }
    }
    
    return result;
  }
  
  
  /**
   * Kopiert die Factory Komponenten aus dem saved-Verzeichnis des Default-Workspaces in das
   * saved-Verzeichnis der angegeben Revision.
   * Außerdem werden die SharedLibs deployt.
   * @param revision
   * @throws XynaException
   * @throws IOException
   * @throws OrderEntryInterfacesCouldNotBeClosedException
   */
  private void copyFactoryComponents(Long revision, CreateWorkspaceResult result) {
    ArrayList<File> filesToCopy = new ArrayList<File>(); //xmls, die kopiert werden
    Map<String, String> xmoms = new HashMap<String, String>(); //xmoms (mit type), die in XMOMDatabase registriert werden müssen
    Set<String> sharedLibs = new HashSet<String>(); //SharedLibs, die von Factory Komponenten verwendet werden und daher kopiert werden müssen
    List<String> services = new ArrayList<String>(); //Services, die zusätzliche jars verwenden

    //alle MDM Files suchen (im saved-Verzeichnis)
    String savedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DEFAULT_WORKSPACE, false);
    List<File> files = FileUtils.getMDMFiles(new File(savedMdmDir), new ArrayList<File>());
    
    for (File file : files) {
      Document doc;
      try {
        doc = XMLUtils.parse(file.getPath());
      } catch (XynaException e) {
        result.handleWarning("Could not parse XMOMObject " + file.getPath(), e);
        continue;
      }
      
      Element rootElement = doc.getDocumentElement();
      String originalFqName = GenerationBase.getFqXMLName(doc);
      String rootTagName = rootElement.getTagName();
      
      Element rootMetaElement = GenerationBase.getRootMetaElement(doc.getDocumentElement());
      if (rootMetaElement != null) {
        Element isFactoryComponentElement = XMLUtils.getChildElementByName(rootMetaElement, GenerationBase.EL.ISXYNACOMPONENT);
        if (isFactoryComponentElement == null) {
          continue; //nur Factory Komponenten kopieren
        }
        
        filesToCopy.add(file);
        xmoms.put(originalFqName, rootTagName);
        
        //für Services müssen evtl. noch Jars und SharedLibs kopiert werden
        if (rootTagName.equals(GenerationBase.EL.DATATYPE)) {
          try {
            DOM dom = DOM.generateUncachedInstance(originalFqName, false, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
            
            if (dom.getAdditionalLibraries().size() > 0) {
              services.add(dom.getFqClassName());
            }
            
            for (String s : dom.getSharedLibs()) {
              sharedLibs.add(s);
            }
          } catch (XynaException e) {
            result.handleWarning("Could not get additional libraries for " + originalFqName, e);
          }
        }
        
        //Wfs als saved registrieren
        if (rootTagName.equals(GenerationBase.EL.SERVICE)) {
          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase().addSaved(originalFqName, revision);
        }
      }
    }
    
    
    //xmls kopieren
    copyXmls(filesToCopy, revision, result);
    
    //in XMOMDatabase eintragen
    registerXMOMs(xmoms, revision, result);
    
    //ServiceJars kopieren
    copyServiceLibraries(services, revision, result);

    //SharesLibs kopieren
    copySharedLibraries(sharedLibs, revision, result);
    
    //Objekte im DeploymentItemStateManagement registrieren
    DeploymentItemStateManagement dism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    dism.discoverItems(revision);
  }
  
  
  private void copyXmls(ArrayList<File> filesToCopy, Long targetRevision, CreateWorkspaceResult result) {
    String sourceDir = RevisionManagement.getPathForRevision(PathType.ROOT, RevisionManagement.REVISION_DEFAULT_WORKSPACE, false);
    String targetDir = RevisionManagement.getPathForRevision(PathType.ROOT, targetRevision, false);

    try {
      FileUtils.copyFiles(filesToCopy, new File(sourceDir), new File(targetDir));
    } catch (Ex_FileAccessException e1) {
      result.handleWarning("Could not copy factory components : " + e1.getMessage(), e1);
    } catch (IOException e1) {
      result.handleWarning("Could not copy factory components : " + e1.getMessage(), e1);
    }
  }
  
  private void registerXMOMs(Map<String, String> xmoms, Long targetRevision, CreateWorkspaceResult result) {
    for (String originalFqName : xmoms.keySet()) {
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase()
                        .registerMOMObject(originalFqName, xmoms.get(originalFqName), targetRevision);
      } catch (XynaException e) {
        result.handleWarning("Could not register XMOMObject",e);
      }
    }
  }
  
  private void copyServiceLibraries(List<String> services, Long targetRevision, CreateWorkspaceResult result) {
    for (String s : services) {
      String sourceDir = GenerationBase.getFileLocationOfServiceLibsForSaving(s, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
      String targetDir = GenerationBase.getFileLocationOfServiceLibsForSaving(s, targetRevision);
      try {
        FileUtils.copyRecursivelyWithFolderStructure(new File(sourceDir), new File(targetDir));
      } catch (Ex_FileAccessException e) {
        result.handleWarning("Could not copy serviceLib " + s, e);
      }
    }
  }
  
  private void copySharedLibraries(Set<String> sharedLibs, Long targetRevision, CreateWorkspaceResult result) {
    for (String s : sharedLibs) {
      String sourceDir = RevisionManagement.getPathForRevision(PathType.SHAREDLIB, RevisionManagement.REVISION_DEFAULT_WORKSPACE, false);
      String targetDir = RevisionManagement.getPathForRevision(PathType.SHAREDLIB, targetRevision, false);
      
      try {
        FileUtils.copyRecursivelyWithFolderStructure(new File(sourceDir + s), new File(targetDir + s));
      } catch (Ex_FileAccessException e) {
        result.handleWarning("Could not copy sharedLib " + s, e);
        continue;
      }
      
      //für SharedLibs gibt es kein automatisches Deployment, daher hier manuell deployen
      try {
        SharedLibDeploymentAlgorithm.deploySharedLib(s, targetRevision, new EmptyRepositoryEvent());
      } catch (XynaException e) {
        result.handleWarning("Could not deploy sharedLib " + s, e);
      } catch (OrderEntryInterfacesCouldNotBeClosedException e) {
        result.handleWarning("Could not deploy sharedLib " + s, e);
      }
    }
  }
  
  /**
   * Legt die workspace-abhängigen XynaProperties an
   * und kopiert die entsprechenden Werte aus dem Default-Workspace
   */
  private void createWorkspaceDependentXynaProperties(Workspace workspace, Long revision) throws PersistenceLayerException{
    //BlackList-XynaProperties für ClearWorkspace
    WorkspaceBlackListXynaProperties blackList = new WorkspaceBlackListXynaProperties(workspace.getName());
    blackList.registerPropertiesDependency(DEFAULT_NAME);
    clearWorkspaceBlackListProperties.put(revision, blackList);
    
    //excludedSubtypesOf-Properties für buildApplicationVersion
    ApplicationManagementImpl applicationManagement =
                    (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                        .getApplicationManagement();
    applicationManagement.addExcludedSubtypesOfProperty(workspace.getName(), revision);
  }
  
  
  /**
   * Entfernt einen Workspace.
   */
  public void removeWorkspace(Workspace workspace, RemoveWorkspaceParameters params) throws XFMG_CouldNotRemoveWorkspace {
    try {
      RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision = revisionManagement.getRevision(workspace);
      if( revisionManagement.isSpecialRevision(revision) ) {
        throw new IllegalArgumentException(workspace + " may not be deleted");
      }
      
      Pair<Operation, Operation> failure =
          CommandControl.wlock(CommandControl.Operation.WORKSPACE_REMOVE, CommandControl.Operation.all(), revision);
      if (failure != null) {
        throw new RuntimeException(failure.getFirst() + " could not be locked because it is locked by another process of type "
            + failure.getSecond() + ".");
      }
      
      try {
        //Überprüfung, dass der Workspace nicht von einem anderen RuntimeContext referenziert wird
        RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        if (rcdMgmt.isReferenced(workspace)) {
          throw new XFMG_RuntimeContextStillReferencedException(workspace.toString());
        }
        
        for (RuntimeContextChangeHandler rdcch : getRtCtxMgmt().getHandlers()) {
          try {
            rdcch.removal(workspace);
          } catch (Throwable t) {
            logger.error("Could not execute RuntimeContextChangeHandler " + rdcch, t);
          }
        }
        
        //Behandlung laufender Aufträge
        revisionManagement.handleRunningOrders(revision, params.isForce());
        
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement().removeWorkspace(workspace);
        
        XynaProcess.instanceMethodTypes.remove(revision);
        
        //zuerst RepositoryAccess entfernen, da die Dateien im Repository erhalten bleiben sollen
        removeRepositoryAccess(revision);
        
        //Trigger und Filter entfernen
        ApplicationManagementImpl applicationManagement =
            (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                .getApplicationManagement();
        applicationManagement.removeFilterInstances(revision, false, null);
        applicationManagement.removeTriggerInstances(revision, false, null);
        applicationManagement.removeFilters(revision, false, null);
        applicationManagement.removeTriggers(revision, false, null);
        
        //Undeployment der XMOM Objekte
        undeployXMOMObjects(revision);
        ClassLoaderBase.cleanupBackuppedDependencies(revision);
        
        //ordertype configs entfernen
        List<OrdertypeParameter> ordertypes =
                        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement()
                            .listOrdertypes(revision);
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement().deleteOrdertypes(ordertypes);

        //orderContextMappings entfernen
        revisionManagement.removeOrderContextMapping(workspace);
        
        //aus XMOMDatabase deregistrieren
        XMOMDatabase xmomDatabase = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
        xmomDatabase.unregisterXMOMObjects(revision);
        
        //aus DeploymentItemStateManagement deregistrieren
        DeploymentItemStateManagement dism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
        dism.removeRegistry(revision);
        
        //alle DeploymentMarker entfernen
        DeploymentMarkerManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentMarkerManagement();
        dmm.deleteDeploymentMarkerForRevision(revision);
        
        //SharedLibs entfernen
        removeSharedLibs(revision);
        
        //Application-Definitions entfernen
        removeApplicationDefinitions(workspace, revision, params.getUser());
        
        //OrderInputSources entfernen
        removeOrderInputSources(revision);
        
        //workspace-abhängige XynaProperties deregistrieren
        unregisterWorkspaceDependentXynaProperties(revision);
        
        //RuntimeContext Dependencies löschen
        rcdMgmt.deleteRuntimeContext(workspace);
        
        //Revision-Verzeichnis (teilweise) löschen
        RevisionManagement.removeRevisionFolder(revision, params.keepForAudits());
        
        //XMOMVersion-Eintrag löschen
        if (!params.keepForAudits()) {
          if (logger.isDebugEnabled()) {
            logger.debug("Delete xmomversion for revision " + revision);
          }
          revisionManagement.deleteRevision(revision);
        }
        
        //Multi-User-Event für RuntimeContext Änderung
        Publisher publisher = new Publisher(params.getUser());
        publisher.publishRuntimeContextDelete(workspace);
      } finally {
        CommandControl.wunlock(CommandControl.Operation.all(), revision);
      }
    } catch (XynaException e) {
      throw new XFMG_CouldNotRemoveWorkspace(workspace.getName(), e);
    }
  }
  
  
  private RuntimeContextManagement getRtCtxMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextManagement();
  }
  
  
  private void undeployXMOMObjects(Long revision) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    WorkflowDatabase wdb = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase();
    List<String> datatypes = wdb.getDeployedDatatypes().get(revision);
    List<String> exceptions = wdb.getDeployedExceptions().get(revision);
    List<String> wfs = wdb.getDeployedWfs().get(revision);

    // call undeployment handler and remove from cache
    try {
      if (datatypes != null) {
        for (String fqName : datatypes) {
          DOM dom = DOM.getInstance(fqName, revision);
          if (dom.getRevision() != null && dom.getRevision().equals(revision)) {
            dom.undeployRudimentarily(false);
          }
        }
      }
      if (exceptions != null) {
        for (String fqName : exceptions) {
          ExceptionGeneration ex = ExceptionGeneration.getInstance(fqName, revision);
          if (ex.getRevision() != null && ex.getRevision().equals(revision)) {
            ex.undeployRudimentarily(false);
          }
        }
      }
      if (wfs != null) {
        for (String fqName : wfs) {
          WF wf = WF.getInstance(fqName, revision);
          if (wf.getRevision() != null && wf.getRevision().equals(revision)) {
            wf.undeployRudimentarily(false);
          }
        }
      }
    } finally {
      GenerationBase.finishUndeploymentHandler();
    }
  }
  
  
  private void removeSharedLibs(Long revision) {
    File sharedLibsDir = new File(RevisionManagement.getPathForRevision(PathType.SHAREDLIB, revision));
    File[] dirs = sharedLibsDir.listFiles();
    if (dirs == null) {
      return;
    }
    for (File dir : dirs) {
      if (dir.isDirectory()) {
        RevisionManagement.removeSharedLib(dir.getName(), revision);
      }
    }
    RevisionManagement.removeSharedLib(null, revision); //empty shared lib
  }
  
  private void removeApplicationDefinitions(Workspace parentWorkspace, Long revision, String user) throws XFMG_CouldNotRemoveApplication {
    ApplicationManagementImpl applicationManagement =
                    (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                        .getApplicationManagement();
    
    List<ApplicationDefinitionInformation> appDefinitions = applicationManagement.listApplicationDefinitions(revision);
    
    RemoveApplicationParameters params = new RemoveApplicationParameters();
    params.setParentWorkspace(parentWorkspace);
    params.setUser(user);
    
    for (ApplicationDefinitionInformation app : appDefinitions) {
      applicationManagement.removeApplicationVersion(new ApplicationName(app.getName(), app.getVersion()), params, false, null, new EmptyRepositoryEvent(), true);
    }
  }

  private void removeOrderInputSources(Long revision) throws PersistenceLayerException {
    OrderInputSourceManagement oism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
    oism.deleteOrderInputSourcesForRevision(revision);
  }
  
  private void removeRepositoryAccess(Long revision) throws PersistenceLayerException {
    String repositoryAccessInstance = null;
    CodeAccessManagement cam = XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getCodeAccessManagement();
    CodeAccess codeAccess = cam.removeCodeAccessInstance(revision);

    if (codeAccess != null) {
      repositoryAccessInstance = codeAccess.getRepositoryAccess().getName();
    }
    
    XMOMAccessManagement xam = XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getXMOMAccessManagement();
    XMOMAccess xmomAccess = xam.removeXMOMAccessInstance(revision);

    if (xmomAccess != null) {
      repositoryAccessInstance = xmomAccess.getRepositoryAccess().getName();
    }
    
    if (repositoryAccessInstance != null) {
      RepositoryAccessManagement ram = XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryAccessManagement();
      ram.removeRepositoryAccessInstance(repositoryAccessInstance);
    }
  }
  
  /**
   * Deregistriert die workspace-abhängigen XynaProperties (clearWorkspace-BlackList und excludeSubTypesOf für buildApplicationVersion)
   */
  private void unregisterWorkspaceDependentXynaProperties(Long revision) throws PersistenceLayerException{
    //BlackList-XynaProperties für ClearWorkspace
    WorkspaceBlackListXynaProperties removed = clearWorkspaceBlackListProperties.remove(revision);
    if (removed != null) {
      removed.unregisterProperties();
    }
    
    //excludeSubtypesOf-Properties für buildApplicationVersion
    ApplicationManagementImpl applicationManagement =
                    (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                        .getApplicationManagement();
    applicationManagement.removeExcludedSubtypesOfProperty(revision);
  }
  
  
  /**
   * Bestimmt alle aktiven (d.h. unbeendeten) XynaOrders, die TimeControlledOrders (Batch Prozesse
   * und Crons) und die Frequency-Controlled Tasks in einem Workspace
   * @param workspaceName
   * @param verbose
   * @param global
   * @return
   * @throws XynaException
   */
  public OrdersInUse listActiveOrders(String workspaceName, boolean verbose, boolean global) throws XynaException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RuntimeContext runtimeContext = RevisionManagement.DEFAULT_WORKSPACE;
    if (workspaceName != null) {
      runtimeContext = new Workspace(workspaceName);
    }
    long revision = revisionManagement.getRevision(runtimeContext);

    FillingMode mode = verbose ? FillingMode.Complete : FillingMode.EasyInfos;
    OrdersInUse activeOrders = DeploymentManagement.getInstance().getInUse(revision, mode);

    //TODO Cluster

    return activeOrders;
  }

  
  public void clearWorkspace(Workspace workspace, ClearWorkspaceParameters params) throws XFMG_ClearWorkingSetFailedBecauseOfRunningOrders, PersistenceLayerException, XPRC_TimeoutWhileWaitingForUnaccessibleOrderException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(workspace);
    
    ClearWorkingSet cws = new ClearWorkingSet(revision);
    RevisionContentBlackWhiteList blackList;
    if (revision.equals(RevisionManagement.REVISION_DEFAULT_WORKSPACE)) {
      blackList = new WorkingSetBlackListXynaProperties();
    } else {
      blackList = clearWorkspaceBlackListProperties.get(revision);
    }
    cws.setBlackList(blackList);
    
    try {
      cws.clear(params.isIgnoreRunningOrders(), params.getRemoveSubtypesOf());
    } catch (OrderEntryInterfacesCouldNotBeClosedException e) {
      throw new RuntimeException(e);
    }
  }
}
