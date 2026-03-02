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



import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.BatchRepositoryEvent;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildNewWorkspace;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveWorkspace;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.RemoveWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;

import xmcp.gitintegration.ListId;
import xmcp.gitintegration.Reference;
import xmcp.gitintegration.ReferenceData;
import xmcp.gitintegration.ReferenceManagement;
import xmcp.gitintegration.ResolveWorkspaceContentDifferencesResult;
import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.WorkspaceContentDifferences;
import xmcp.gitintegration.WorkspaceContentDifferencesResolution;
import xmcp.gitintegration.WorkspaceObjectManagement;
import xmcp.gitintegration.impl.RepositoryManagementImpl.AddRepositoryConnectionResult.Success;
import xmcp.gitintegration.repository.Repository;
import xmcp.gitintegration.repository.RepositoryConnection;
import xmcp.gitintegration.repository.RepositoryConnectionGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;



public class RepositoryManagementImpl {

  private static Logger _logger = Logger.getLogger(RepositoryManagementImpl.class); 

  private static PreparedQueryCache queryCache = new PreparedQueryCache();

  public static final String CONFIG = "config";
  public static final String WORKSPACE_XML = "workspace.xml";
  private static final String SAVED = "saved";
  private static final String XMOM = "XMOM";


  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(RepositoryConnectionStorable.class);
    queryCache = new PreparedQueryCache();
  }


  public static void shutdown() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.unregisterStorable(RepositoryConnectionStorable.class);
  }


  private static String replaceSymbolicLink(Path linkPath) {
    // check symbolic link and its target
    Path targetPath;
    try {
      targetPath = linkPath.toRealPath();
      if (!Files.isDirectory(targetPath)) {
        return "Error: Could not follow symbolic link '" + linkPath + "'!";
      }
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return "Error: Could not find symbolic link '" + linkPath + "'!";
    }
    // delete symbolic link
    try {
      Files.delete(linkPath);
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return "Error: Could not delete symbolic link '" + linkPath + "'!";
    }
    // create new directory at link path
    try {
      Files.createDirectory(linkPath);
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return "Error: Could not create directory '" + linkPath + "'!";
    }
    // copy content from target path to newly created link path
    if (!copyDirectoryContent(targetPath, linkPath)) {
      return "Error: Could not copy files from '" + targetPath + "' to '" + linkPath + "!";
    }
    return null;
  }


  private static boolean copyDirectoryContent(Path source, Path target) {
    try {
      Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          Files.createDirectories(target.resolve(source.relativize(dir).toString()));
          return FileVisitResult.CONTINUE;
        }


        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.copy(file, target.resolve(source.relativize(file).toString()));
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return false;
    }
    return true;
  }


  private static boolean deleteDirectoryContent(Path path) {
    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }


        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return false;
    }
    return true;
  }


  private static boolean deleteDirectory(Path path) {
    if (!deleteDirectoryContent(path)) {
      return false;
    }
    if(!path.toFile().exists()) {
      return true;
    }
    try {
      Files.delete(path);
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return false;
    }
    return true;
  }


  private static Long getRevision(String workspaceName) {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try {
      return rm.getRevision(null, null, workspaceName);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    }
  }


  private static Long createWorkspace(String workspaceName) {
    WorkspaceManagement wm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    try {
      wm.createWorkspace(new Workspace(workspaceName));
      return getRevision(workspaceName);
    } catch (XFMG_CouldNotBuildNewWorkspace e) {
      return null;
    }
  }


  private static Long deleteWorkspace(String workspaceName) {
    WorkspaceManagement wm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    Long revision = getRevision(workspaceName);
    if (revision == null) {
      return null;
    }
    try {
      RemoveWorkspaceParameters params = new RemoveWorkspaceParameters();
      params.setForce(true);
      params.setCleanupXmls(true);
      wm.removeWorkspace(new Workspace(workspaceName), params);
      return revision;
    } catch (XFMG_CouldNotRemoveWorkspace e) {
      return null;
    }
  }
  private static boolean matchWsFile(Path filePath, BasicFileAttributes fileAttr) {
    return fileAttr.isRegularFile() && filePath.endsWith(WORKSPACE_XML);
  }

  
  public static AddRepositoryConnectionResult addRepositoryConnection(String repoPath, String workspace, boolean full, boolean setup) {
    List<RepositoryConnectionStorable> workspaces;
    try {
      workspaces = findRelevantWorkspacesToConnect(repoPath, workspace, full);
    } catch (Exception e) {
      return new AddRepositoryConnectionResult(Success.NONE, Collections.emptyList(), e.getMessage());
    }
    if (workspaces.isEmpty()) {
      String msg = full ? "Could not find any workspaces in path!" : "Could not find given workspace in path!";
      return new AddRepositoryConnectionResult(Success.NONE, Collections.emptyList(), msg);
    }

    List<String> errors = new ArrayList<>();
    List<String> actionsPerformed = new ArrayList<>();
    int successfulConnections = 0;
    for (RepositoryConnectionStorable storable : workspaces) {
      boolean success = createWorkspaceWithSymlinks(storable, actionsPerformed, errors);
      successfulConnections += success ? 1 : 0;
      if(success) {
        persistRepositoryConnectionStorable(storable);
        actionsPerformed.add("registered repository connection for " + storable.getWorkspacename());
      }
    }
    //if setup is requested, setup each relevant workspace (in correct order)
    if(setup && errors.isEmpty()) {
      try {
        setupWorkspaces(workspaces, actionsPerformed, errors);
      } catch(Exception e) {
        errors.add("Error setting up workspace: " + e.getMessage());
      }
    } else if(setup) {
      errors.add("Skipping setup because of previous errors");
    }
    Success success = successfulConnections == 0 ? Success.NONE : errors.isEmpty() ? Success.FULL : Success.PARTIAL;
    return new AddRepositoryConnectionResult(success, actionsPerformed, String.join("\n", errors));
  }

  
  private static WorkspaceContentDifferences createWorkspaceContentDifferences(RepositoryConnectionStorable storable) {
    WorkspaceContent wsContent = WorkspaceObjectManagement.createWorkspaceContent(new xprc.xpce.Workspace(storable.getWorkspacename()));
    Path pathToWorkspaceXml = Paths.get(storable.getPath(), storable.getSubpath()).toAbsolutePath().normalize();
    boolean split = WorkspaceConfigSplit.fromId(storable.getSplittype()).get() != WorkspaceConfigSplit.NONE;
    pathToWorkspaceXml = pathToWorkspaceXml.resolve(split ? CONFIG : WORKSPACE_XML);
    WorkspaceContent repoContent = WorkspaceObjectManagement.createWorkspaceContentFromFile(new base.File(pathToWorkspaceXml.toString()));
    return WorkspaceObjectManagement.compareWorkspaceContent(wsContent, repoContent);
  }
  
  
  private static Long resolveRuntimeContextDependencyDiffs(RepositoryConnectionStorable storable, WorkspaceContentDifferences diffs, List<String> actionsPerformed, List<String> errors) {
    ListId listId = new ListId.Builder().listId(diffs.getListId()).instance();
    List<? extends WorkspaceContentDifference> entries = new ArrayList<>(diffs.getDifferences());
    boolean remainingChanges = false;
    for(WorkspaceContentDifference entry : entries) {
      if(!(entry.getContentType().equals("runtimecontextdependency"))) {
        continue;
      }
      boolean success = resolveWorkspaceDifference(listId, entry, actionsPerformed, errors);
      remainingChanges |= !success;
      diffs.getDifferences().remove(entry);
    }

    if(remainingChanges) {
      errors.add("Could not resolve all rtc dependencies of workspace " + storable.getWorkspacename() + " setup of this workspace will be skipped.");
      return null;
    } else {
      actionsPerformed.add("Successfully resolved all rtc dependencies of workspace " + storable.getWorkspacename());
      return getRevision(storable.getWorkspacename());
    }
  }
  
  
  private static void resolveNonAppDefDiffs(Map<String, WorkspaceContentDifferences> workspaceDiffsByWorkspace, String workspaceName, List<String> actionsPerformed, List<String> errors) {
    WorkspaceContentDifferences diffs = workspaceDiffsByWorkspace.get(workspaceName);
    ListId listId = new ListId.Builder().listId(diffs.getListId()).instance();
    for(WorkspaceContentDifference entry: new ArrayList<>(diffs.getDifferences())) {
      if(entry.getContentType().equals("applicationdefinition")) {
        continue;
      }
      resolveWorkspaceDifference(listId, entry, actionsPerformed, errors);
      diffs.getDifferences().remove(entry);
    }
  }
  
  private static void setupWorkspaces(List<RepositoryConnectionStorable> storables, List<String> actionsPerformed, List<String> errors) {
    Map<String, WorkspaceContentDifferences> workspaceDiffsByWorkspace = new HashMap<>();
    List<Long> revisions = new ArrayList<>();
    Map<Long, String> revisionToWsNameMap = new HashMap<>();
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    
    for(RepositoryConnectionStorable storable : storables) {
      WorkspaceContentDifferences diffs = createWorkspaceContentDifferences(storable);
      actionsPerformed.add("There are " + diffs.getDifferences().size() + " pieces of configuration to apply for workspace "  + storable.getWorkspacename());
      workspaceDiffsByWorkspace.put(storable.getWorkspacename(), diffs);
      
      revisions.add(resolveRuntimeContextDependencyDiffs(storable, diffs, actionsPerformed, errors));
    }

    List<Long> sortedRevisions = sortWorkspaces(revisions);
    for(Long revision : new ArrayList<>(sortedRevisions)) {
      try {
        revisionToWsNameMap.put(revision, rm.getWorkspace(revision).getName());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        errors.add("Could not find workspace for revision " + revision);
        sortedRevisions.remove(revision);
      }
    }
    
    for (Long revision : sortedRevisions) {
      refreshworkspace(revision, false, actionsPerformed, errors);
    }
    
    for(Long revision : sortedRevisions) {
      String workspaceName = revisionToWsNameMap.get(revision);
      resolveNonAppDefDiffs(workspaceDiffsByWorkspace, workspaceName, actionsPerformed, errors);
      triggerDatatypeReferences(workspaceName, revision, actionsPerformed, errors);
    }
    
    for (Long revision : sortedRevisions) {
      refreshworkspace(revision, true, actionsPerformed, errors);
    }
    
    for(Long revision : sortedRevisions) {
      WorkspaceContentDifferences diffs = workspaceDiffsByWorkspace.get(revisionToWsNameMap.get(revision));
      ListId listId = new ListId.Builder().listId(diffs.getListId()).instance();
      for(WorkspaceContentDifference entry: diffs.getDifferences()) {
        resolveWorkspaceDifference(listId, entry, actionsPerformed, errors);
      }
    }
  }


  private static void triggerDatatypeReferences(String workspaceName, Long revision, List<String> actionsPerformed, List<String> errors) {
    List<? extends ReferenceData> references = ReferenceManagement.listReferences(new xprc.xpce.Workspace(workspaceName));
    if (references == null || references.isEmpty()) {
      return;
    }
    references = references.stream().filter(x -> x.getObjectType().equals("DATATYPE")).collect(Collectors.toList());
    if(references.isEmpty()) {
      return;
    }
    List<Reference> refsToTrigger = new ArrayList<>();
    for(ReferenceData refData : references) {
      Reference.Builder builder = new Reference.Builder();
      builder.path(refData.getPath());
      builder.type(refData.getReferenceType());
      refsToTrigger.add(builder.instance());
    }
    try {
      actionsPerformed.add("Trigger references for datatypes in " + workspaceName);
      ReferenceManagement.triggerReferences(refsToTrigger, null, revision);
      actionsPerformed.add("Triggered references for datatypes in " + workspaceName);
    } catch(Exception e) {
      errors.add("Could not trigger references for datatypes in " + workspaceName);
    }
  }


  private static boolean resolveWorkspaceDifference(ListId listId, WorkspaceContentDifference entry, List<String> actionsPerformed, List<String> errors) {
    WorkspaceContentDifferencesResolution.Builder builder = new WorkspaceContentDifferencesResolution.Builder();
    builder.entryId(entry.getEntryId());
    builder.resolution(entry.getDifferenceType().getClass().getSimpleName());
    String action = WorkspaceObjectManagement.createDifferenceString(entry);
    action = action.endsWith("\n") ? action.substring(0, action.length() - 1) : action;
    actionsPerformed.add("Trying to resolve "+ action);
    try {
      List<? extends ResolveWorkspaceContentDifferencesResult> result;
      result= WorkspaceObjectManagement.resolveWorkspaceDifferences(listId, List.of(builder.instance()));
      if (result == null || result.isEmpty()) {
        actionsPerformed.add("Successfully resolved difference: " + action);
        return true;
      }
      if (result.size() == 1) {
        if (result.get(0).getSuccess()) {
          actionsPerformed.add("Successfully resolved difference: " + action);
          return true;
        } else {
          errors.add("Failed to resolve difference: " + action + ". Reason: " + result.get(0).getMessage());
        }
      }
    } catch (Exception e) {
      errors.add("Error resolving workspace difference " + e.getMessage());
    }
    return false;
  }
  
  
  private static void refreshworkspace(Long revision, boolean deploy, List<String> actionsPerformed, List<String> errors) {
    XynaMultiChannelPortal portal = (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();
    TemporarySessionAuthentication tsa =
    TemporarySessionAuthentication.tempAuthWithUniqueUserAndOperationLock("RefreshWS", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE, revision,
                                                                          CommandControl.Operation.XMOM_SAVE);
    try {
      tsa.initiate();
      Collection<String> allObjectNames = collectObjectNames(revision);
      BatchRepositoryEvent repositoryEvent = new BatchRepositoryEvent(revision);
      if(!deploy) {
        actionsPerformed.add("Register objects of revision " + revision);
        for (String objectName : allObjectNames) {
          File file = new File(GenerationBase.getFileLocationOfXmlNameForSaving(objectName, revision) + ".xml");
          if (file.exists()) {
            String xml = FileUtils.readFileAsString(file);
            try {
              portal.saveMDM(xml, true, tsa.getUsername(), tsa.getSessionId(), revision, repositoryEvent, true, true);
            } catch (Exception e) {
              errors.add("exception during registration of " + objectName + " in revision " + revision);
            }
          }
        }
        actionsPerformed.add("Registered objects of revision " + revision);
      } else {
        List<GenerationBase> toDeploy = new ArrayList<>();
        for (String objectName : allObjectNames) {
          File file = new File(GenerationBase.getFileLocationOfXmlNameForSaving(objectName, revision) + ".xml");
          XMOMType type = XMOMType.getXMOMTypeByFile(file);
          toDeploy.add(GenerationBase.getInstance(type, objectName, revision));
        }
        actionsPerformed.add("Deploying objects of revision " + revision);
        GenerationBase.deploy(toDeploy, DeploymentMode.codeChanged, false, WorkflowProtectionMode.BREAK_ON_USAGE);
        actionsPerformed.add("Deployed objects of revision " + revision);
      }
    } catch(Exception e) {
      errors.add("Error during " + (deploy ? "deployment" : "registration") + " of revision " + revision + ": " + e.getMessage());
    } finally {
      try {
        tsa.destroy();
      } catch (Exception e) {
        errors.add("Error during destruction of temporary session for revision " + revision + ": " + e.getMessage());
      }
    }
  }

  
  private static Collection<String> collectObjectNames(Long revision) {
    final String savedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, revision, false);
    List<File> files = FileUtils.getMDMFiles(new File(savedMdmDir), new ArrayList<File>());
    return CollectionUtils.transformAndSkipNull(files, new Transformation<File, String>() {
      public String transform(File from) {
        String xmlName = from.getPath().substring(savedMdmDir.length() + 1).replaceAll(Constants.FILE_SEPARATOR, ".");
        xmlName = xmlName.substring(0, xmlName.length() - ".xml".length());
        if (GenerationBase.isReservedServerObjectByFqOriginalName(xmlName)) {
          return null;
        } else {
          return xmlName;
        }
      }
    });
  }
  
  private static List<Long> sortWorkspaces(List<Long> revisions) {
    List<Long> result = new ArrayList<>();
    RuntimeContextDependencyManagement rcdm;
    rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    for(Long revision : revisions) {
      if(result.contains(revision)) {
        continue;
      }
      addRevisionToResult(result, revision, revisions, rcdm);
    }
    return result;
  }
  
  private static void addRevisionToResult(List<Long> result, Long revision, List<Long> revisions, RuntimeContextDependencyManagement rcdm) {
    Set<Long> dependencies = new HashSet<>();
    rcdm.getDependenciesRecursivly(revision, dependencies);
    for(Long dependency : dependencies) {
      if(revisions.contains(dependency) && !result.contains(dependency)) {
        addRevisionToResult(result, dependency, revisions, rcdm);
      }
    }
    
    result.add(revision);
  }

  private static boolean createWorkspaceWithSymlinks(RepositoryConnectionStorable storable, List<String> actions, List<String> errors) {
    String workspaceName = storable.getWorkspacename();
    if (getRevision(workspaceName) != null) {
      errors.add("Workspace '" + storable.getWorkspacename() + "' already exists.");
      return false;
    }
    Path subPath = Paths.get(storable.getPath(), storable.getSubpath());
    if (!subPath.resolve(SAVED).resolve(XMOM).toFile().isDirectory() && !subPath.resolve(XMOM).toFile().isDirectory()) {
      errors.add("Sub path of workspace.xml for workspace '" + workspaceName + "' does not contain the " + SAVED + "/" + XMOM + " or "
          + XMOM + " directory! Subpath: " + storable.getSubpath());
      return false;
    }

    Long revision = createWorkspace(workspaceName);
    if (revision == null) {
      errors.add("Could not create workspace '" + workspaceName + "' within the factory!");
      return false;
    } else {
      actions.add("Created workspace for " + workspaceName + " (" + revision + ")");
    }

    if (!createSymlinks(storable, actions, errors)) {
      actions.add("Tried to delete workspace " + workspaceName + " (" + revision + ")");
      Long rev = deleteWorkspace(workspaceName);
      if (rev == null) {
        errors.add("Deletion of workspace " + workspaceName + " was NOT successfull.");
      } else {
        actions.add("Deletion of workspace " + workspaceName + " was successfull.");
      }
      return false;
    }

    return true;
  }


  private static boolean createSymlinks(RepositoryConnectionStorable storable, List<String> actions, List<String> errors) {
    Long rev = getRevision(storable.getWorkspacename());
    Path revisionPath = Path.of(Constants.BASEDIR, Constants.REVISION_PATH, Constants.PREFIX_REVISION + rev).toAbsolutePath().normalize();
    if (storable.getSavedinrepo()) {
      if (!deleteDirectory(revisionPath)) {
        errors.add("Could not delete directory '" + revisionPath + "' within the factory!");
        return false;
      }
      if (!createSymbolicLink(revisionPath, Paths.get(storable.getPath(), storable.getSubpath()))) {
        errors.add("Could not create symbolic link '" + revisionPath + "' within the factory!");
        return false;
      } else {
        actions.add("Created symbolic link from " + revisionPath + " to " + Paths.get(storable.getPath(), storable.getSubpath()));
      }
      return true;
    }

    Path workspacePathAbs = Path.of(storable.getPath(), storable.getSubpath()).toAbsolutePath().normalize();
    try {
      Files.createDirectory(revisionPath.resolve(SAVED));
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      errors.add("Could not create directory '" + revisionPath.resolve(SAVED) + "' within the factory!");
      return false;
    }
    actions.add("Created directory " + revisionPath.resolve(SAVED));

    if (!createSymbolicLink(revisionPath.resolve(SAVED).resolve(XMOM), workspacePathAbs.resolve(XMOM))) {
      errors.add("Could not create symbolic link '" + revisionPath.resolve(SAVED).resolve(XMOM) + "' within the factory!");
      return false;
    } else {
      actions.add("Created symbolic link from " + revisionPath.resolve(SAVED).resolve(XMOM) + " to " + workspacePathAbs.resolve(XMOM));
    }
    // create symlink for config directory, if any
    Optional<WorkspaceConfigSplit> split = WorkspaceConfigSplit.fromId(storable.getSplittype());
    if (split.isPresent() && split.get() != WorkspaceConfigSplit.NONE) {
      if (!createSymbolicLink(revisionPath.resolve(CONFIG), workspacePathAbs.resolve(CONFIG))) {
        errors.add("Could not create symbolic link '" + revisionPath.resolve(CONFIG) + "' within the factory!");
        return false;
      } else {
        actions.add("Created symbolic link from " + revisionPath.resolve(CONFIG) + " to " + workspacePathAbs.resolve(CONFIG));
      }
    } else {
      Path workspaceXmlPath = Paths.get(workspacePathAbs.toString(), WORKSPACE_XML);
      Path filename = workspaceXmlPath.getFileName();
      if (!createSymbolicLink(revisionPath.resolve(filename), workspaceXmlPath)) {
        errors.add("Could not create symbolic link '" + revisionPath.resolve(filename) + "' within the factory!");
        return false;
      } else {
        actions.add("Created symbolic link from " + revisionPath.resolve(filename) + " to " + workspaceXmlPath);
      }
    }


    return true;
  }


  private static boolean createSymbolicLink(Path from, Path to) {
    try {
      Files.createSymbolicLink(from, to);
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return false;
    }
    return true;
  }


  private static List<RepositoryConnectionStorable> findRelevantWorkspacesToConnect(String repoPath, String workspace, boolean full) {
    List<RepositoryConnectionStorable> result = new ArrayList<>();
    if (!new File(repoPath).isDirectory()) {
      throw new RuntimeException("Error: Path '" + repoPath + "' is not a directory!");
    }
    Path basePath = Paths.get(repoPath).toAbsolutePath();
    String basePathStr = basePath.toString();
    Map<String, Path> workspaceXmlPathMap;
    try {
      workspaceXmlPathMap = createWorkspaceXmlPathMap(basePath, full, workspace);
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      throw new RuntimeException("Error: Exception occured while searching for workspace.xml files!");
    }
    for (Entry<String, Path> entry : workspaceXmlPathMap.entrySet()) {
      String workspaceName = entry.getKey();
      Path pathToWorkspaceXml = entry.getValue();
      boolean savedInRepo = pathToWorkspaceXml.getParent().resolve(SAVED).resolve(XMOM).toFile().isDirectory();
      boolean isSplit = pathToWorkspaceXml.getParent().endsWith(CONFIG);
      Path subPath = isSplit ? pathToWorkspaceXml.getParent() : pathToWorkspaceXml;
      String subPathString = subPath.getParent().toAbsolutePath().toString().substring(basePathStr.length() + 1); //+1 for "/"
      String workspaceXmlBasePath = pathToWorkspaceXml.toAbsolutePath().normalize().toString();
      String splitStr = determineSplitType(workspaceXmlBasePath);
      RepositoryConnectionStorable storable;
      storable = new RepositoryConnectionStorable(workspaceName, Path.of(basePathStr).normalize().toString(), subPathString, savedInRepo, splitStr);
      result.add(storable);
    }
    return result;
  }


  private static String determineSplitType(String filePath) {
    WorkspaceContent content = WorkspaceObjectManagement.createWorkspaceContentFromFile(new base.File(filePath));
    String splitStr = Path.of(filePath).endsWith(CONFIG) ? WorkspaceConfigSplit.BYTYPE.getId() : WorkspaceConfigSplit.NONE.getId();
    if(content.getSplit() != null) {
      Optional<WorkspaceConfigSplit> optional = WorkspaceConfigSplit.fromId(content.getSplit());
      if(optional.isPresent()) {
        return content.getSplit();
      } else {
        if(_logger.isWarnEnabled()) {
          _logger.warn("invalid split type '" + content.getSplit() + " in " + filePath + " assuming " + splitStr);
        }
      }
    }
    return splitStr;
  }

  
  private static Map<String, Path> createWorkspaceXmlPathMap(Path basePath, boolean full, String workspace) throws IOException {
    Map<String, Path> workspaceXmlPathMap = new HashMap<>();
    List<Path> paths = Files.find(basePath, Integer.MAX_VALUE, RepositoryManagementImpl::matchWsFile).collect(Collectors.toList());

    for (Path workspaceXmlPath : paths) {
      WorkspaceContent content = WorkspaceObjectManagement.createWorkspaceContentFromFile(new base.File(workspaceXmlPath.toString()));
      String workspaceName = content.getWorkspaceName();
      if (workspaceName != null && (full || workspaceName.equals(workspace))) {
        workspaceXmlPathMap.put(workspaceName, workspaceXmlPath);
      }
    }

    return workspaceXmlPathMap;
  }

  public static List<RepositoryConnection> listRepositoryConnections() {
    List<RepositoryConnection> result = new ArrayList<>();
    List<RepositoryConnectionStorable> storables = loadRepositoryConnections();
    for(RepositoryConnectionStorable storable : storables) {
      result.add(convert(storable));
    }
    return result;
  }

  private static RepositoryConnection convert(RepositoryConnectionStorable storable) {
    RepositoryConnection.Builder result = new RepositoryConnection.Builder();
    result.path(storable.getPath())
        .savedinrepo(storable.getSavedinrepo())
        .splittype(storable.getSplittype())
        .subpath(storable.getSubpath())
        .workspaceName(storable.getWorkspacename());
    return result.instance();
  }

  public static String removeRepositoryConnection(String workspace, boolean full, boolean delete) {
    List<? extends RepositoryConnectionStorable> storables = loadRepositoryConnections();
    if (!full) {
      storables.removeIf(storable -> !storable.getWorkspacename().equals(workspace));
    }
    int count = 0;
    for (RepositoryConnectionStorable storable : storables) {
      String workspaceName = storable.getWorkspacename();
      Long revision = getRevision(workspaceName);
      if (revision == null) {
        return "Error: Workspace '" + workspaceName + "' does not exist within the factory!";
      }
      Path revisionPath = Path.of(Constants.BASEDIR, Constants.REVISION_PATH, Constants.PREFIX_REVISION + revision);
      String error = replaceSymbolicLinks(revisionPath, storable);
      if(error != null) {
        return error;
      }

      deleteRepositoryConnectionStorable(workspaceName);
      if (delete) {
        deleteWorkspace(workspaceName);
      }
      count++;
    }
    return "Successfully removed " + count + " workspace(s) from the repository.";
  }


  private static String replaceSymbolicLinks(Path revisionPath, RepositoryConnectionStorable storable) {
    String error = null;
    List<Path> toReplace = new ArrayList<>();
    toReplace.add(storable.getSavedinrepo() ? revisionPath : revisionPath.resolve(SAVED).resolve(XMOM));
    Optional<WorkspaceConfigSplit> configSplit = WorkspaceConfigSplit.fromId(storable.getSplittype());
    boolean oldConfigMightBeSplit = configSplit.isEmpty() || configSplit.get() != WorkspaceConfigSplit.NONE;
    if (oldConfigMightBeSplit && Files.isSymbolicLink(revisionPath.resolve(CONFIG))) {
      toReplace.add(revisionPath.resolve(CONFIG));
    }
    for (Path pathToReplace : toReplace) {
      error = replaceSymbolicLink(pathToReplace);
      if (error != null) {
        return error;
      }
    }
    return error;
  }

  private static class LoadRepositoryConnections implements WarehouseRetryExecutableNoException<List<RepositoryConnectionStorable>> {

    @Override
    public List<RepositoryConnectionStorable> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      return new ArrayList<>(con.loadCollection(RepositoryConnectionStorable.class));
    }
  }


  public static List<RepositoryConnectionStorable> loadRepositoryConnections() {
    List<RepositoryConnectionStorable> result;
    try {
      result = WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(RepositoryConnectionStorable.class)).execute(new LoadRepositoryConnections());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    return result;
  }


  private static class LoadConnectionsForSingleRepository implements WarehouseRetryExecutableNoException<List<RepositoryConnectionStorable>> {

    private String repo;


    public LoadConnectionsForSingleRepository(String repo) {
      this.repo = repo;
    }


    @Override
    public List<RepositoryConnectionStorable> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      ResultSetReader<? extends RepositoryConnectionStorable> reader = new RepositoryConnectionStorable().getReader();
      PreparedQuery<? extends RepositoryConnectionStorable> query = queryCache.getQueryFromCache(QUERY_ENTRIES_FOR_LIST, con, reader);
      List<? extends RepositoryConnectionStorable> result = con.query(query, new Parameter(repo), -1);
      return new ArrayList<RepositoryConnectionStorable>(result);
    }


    private static final String QUERY_ENTRIES_FOR_LIST =
        "select * from " + RepositoryConnectionStorable.TABLE_NAME + " where " + RepositoryConnectionStorable.COL_PATH + "=?";

  }


  public static List<? extends RepositoryConnectionStorable> loadConnectionsForSingleRepository(String repo) {
    List<RepositoryConnectionStorable> result;
    try {
      result = WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(RepositoryConnectionStorable.class)).execute(new LoadConnectionsForSingleRepository(repo));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    return result;
  }


  private static class LoadRepositoryConnectionForWorkspace implements WarehouseRetryExecutableNoException<Optional<RepositoryConnectionStorable>> {

    private String workspace;


    public LoadRepositoryConnectionForWorkspace(String workspace) {
      this.workspace = workspace;
    }


    @Override
    public Optional<RepositoryConnectionStorable> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      try {
        RepositoryConnectionStorable result = new RepositoryConnectionStorable(workspace);
        con.queryOneRow(result);
        return Optional.of(result);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return Optional.empty();
      }
    }
  }


  public static Optional<RepositoryConnectionStorable> loadRepositoryConnectionForWorkspace(String workspace) {
    Optional<RepositoryConnectionStorable> result;
    try {
      result = WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(RepositoryConnectionStorable.class))
          .execute(new LoadRepositoryConnectionForWorkspace(workspace));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    return result;
  }


  private static class PersistRepositoryConnectionStorable implements WarehouseRetryExecutableNoResult {

    private RepositoryConnectionStorable content;


    public PersistRepositoryConnectionStorable(RepositoryConnectionStorable content) {
      this.content = content;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      con.persistObject(content);
    }
  }


  public static void persistRepositoryConnectionStorable(RepositoryConnectionStorable storable) {
    try {
      WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(RepositoryConnectionStorable.class)).execute(new PersistRepositoryConnectionStorable(storable));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  private static class DeleteRepositoryConnectionStorable implements WarehouseRetryExecutableNoResult {

    private String workspaceName;


    public DeleteRepositoryConnectionStorable(String workspaceName) {
      this.workspaceName = workspaceName;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      con.deleteOneRow(new RepositoryConnectionStorable(workspaceName));
    }
  }


  public static void deleteRepositoryConnectionStorable(String workspaceName) {
    try {
      WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(RepositoryConnectionStorable.class))
          .execute(new DeleteRepositoryConnectionStorable(workspaceName));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  private static class DeleteAllRepositoryConnectionStorables implements WarehouseRetryExecutableNoResult {

    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      con.deleteAll(RepositoryConnectionStorable.class);
    }
  }


  public static void deleteAllRepositoryConnectionStorables() {
    try {
      WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(RepositoryConnectionStorable.class)).execute(new DeleteAllRepositoryConnectionStorables());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static RepositoryConnection getRepositoryConnection(String workspaceName) {
    Optional<RepositoryConnectionStorable> opt = loadRepositoryConnectionForWorkspace(workspaceName);
    if(opt.isEmpty()) {
      return null;
    }
    RepositoryConnection repositoryConnection = new RepositoryConnection();
    repositoryConnection.setWorkspaceName(workspaceName);
    repositoryConnection.setPath(opt.get().getPath());
    repositoryConnection.setSubpath(opt.get().getSubpath());
    repositoryConnection.setSavedinrepo(opt.get().getSavedinrepo());
    repositoryConnection.setSplittype(opt.get().getSplittype());
    return repositoryConnection;
  }
  
  public static void updatetRepositoryConnection(RepositoryConnection repositoryConnection) {
    Optional<RepositoryConnectionStorable> opt = loadRepositoryConnectionForWorkspace(repositoryConnection.getWorkspaceName());
    if(opt.isEmpty()) {
      throw new RuntimeException("No RepositoryConnection found (Workspace: " + repositoryConnection.getWorkspaceName() + ")");
    }
    // Update Attributes
    opt.get().setPath(repositoryConnection.getPath());
    opt.get().setSubpath(repositoryConnection.getSubpath());
    opt.get().setSavedinrepo(repositoryConnection.getSavedinrepo());
    opt.get().setSplittype(repositoryConnection.getSplittype());
    persistRepositoryConnectionStorable(opt.get());
  }


  public static List<? extends RepositoryConnectionGroup> listRepositoryConnectionGroups() {
    List<RepositoryConnection> connections = RepositoryManagementImpl.listRepositoryConnections();
    connections.sort((a, b) -> a.getWorkspaceName().compareTo(b.getWorkspaceName()));
    List<RepositoryConnectionGroup> result = new ArrayList<>();
    Map<String, List<RepositoryConnection>> groups = new HashMap<>();
    for(RepositoryConnection connection: connections) {
      groups.putIfAbsent(connection.getPath(), new ArrayList<>());
      groups.get(connection.getPath()).add(connection);
    }
    for(String repoGroup : groups.keySet()) {
      Repository repo = new Repository.Builder().path(repoGroup).instance();
      List<RepositoryConnection> conns = groups.get(repoGroup);
      RepositoryConnectionGroup group = new RepositoryConnectionGroup.Builder().repository(repo).repositoryConnection(conns).instance();
      result.add(group);
    }
    return result;
  }
  
  public static class AddRepositoryConnectionResult {
    private final Success success;
    private final List<String> actionsPerformed;
    private final String errorMsg;
    
    public AddRepositoryConnectionResult(Success success, List<String> actionsPerformed, String errorMsg) {
      this.success = success;
      this.actionsPerformed = actionsPerformed;
      this.errorMsg = errorMsg;
    }
    
    public Success getSuccess() {
      return success;
    }
    
    public List<String> getActionsPerformed() {
      return actionsPerformed;
    }
    
    public String getErrorMsg() {
      return errorMsg;
    }
    
    public static enum Success {
      NONE, PARTIAL, FULL
    }
  }
  
}
