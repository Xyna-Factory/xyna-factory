/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xmcp.gitintegration.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;

import xmcp.gitintegration.RepositoryManagement;
import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentDifferences;
import xmcp.gitintegration.WorkspaceXmlCreationConfig;
import xmcp.gitintegration.impl.WorkspaceConfigSplit;
import xmcp.gitintegration.impl.WorkspaceContentCreator;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;
import xmcp.gitintegration.impl.xml.WorkspaceContentXmlConverter;
import xmcp.gitintegration.repository.RepositoryConnection;
import xmcp.gitintegration.storage.WorkspaceDifferenceListStorage;
import xprc.xpce.Workspace;


public class CreateWorkspaceXmlTools {

  
  private static enum SplitModeChange {
    NOT_CHANGED, CHANGED
  }
  
  public void execute(String workspaceName) {
    try {
      RepositoryConnection repositoryConnection = RepositoryManagement.getRepositoryConnection(new Workspace(workspaceName));
      WorkspaceXmlCreationConfig conf = new WorkspaceXmlCreationConfig();
      conf.unversionedSetWorkspaceName(workspaceName);
      conf.unversionedSetForce(false);
      conf.unversionedSetSplitResult(repositoryConnection.getSplitted());
      executeImpl(conf, Optional.ofNullable(repositoryConnection), SplitModeChange.NOT_CHANGED);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  
  public void execute(WorkspaceXmlCreationConfig conf) throws XynaException {
    String workspaceName = conf.getWorkspaceName();
    RepositoryConnection repositoryConnection = RepositoryManagement.getRepositoryConnection(new Workspace(workspaceName));
    Optional<RepositoryConnection> optRepoConn = Optional.ofNullable(repositoryConnection);
    SplitModeChange splitModeChange = SplitModeChange.NOT_CHANGED;
    Optional<WorkspaceConfigSplit> newSplitConfig = WorkspaceConfigSplit.fromId(conf.getSplitResult());

    if(optRepoConn.isEmpty() && !conf.getForce()) {
      throw new RuntimeException("No repository connection found for '" + conf.getWorkspaceName() + "'. Use force to create workspace xml anyway.");
    }

    if(optRepoConn.isPresent() && newSplitConfig.isPresent()) {
      splitModeChange = Objects.equals(repositoryConnection.getSplitted(), conf.getSplitResult()) ? SplitModeChange.CHANGED : SplitModeChange.NOT_CHANGED;
      repositoryConnection.setSplitted(conf.getSplitResult());
      if (splitModeChange == SplitModeChange.CHANGED) {
        if (!conf.getForce()) {
          throw new RuntimeException("Use force to change the configuration between single file and splitted");
        }
        RepositoryManagement.updateRepositoryConnection(repositoryConnection);
      }
    }
    executeImpl(conf, optRepoConn, splitModeChange);
  }
  

  private void executeImpl(WorkspaceXmlCreationConfig conf, Optional<RepositoryConnection> optRepConn, SplitModeChange splitModeChange)
                             throws XynaException {
    String workspaceName = conf.getWorkspaceName();
    Optional<WorkspaceConfigSplit> optionalWorkspaceConfigSplit = WorkspaceConfigSplit.fromId(conf.getSplitResult());
    if(optionalWorkspaceConfigSplit.isEmpty()) {
      throw new RuntimeException("Invalid WorkspaceConfigSplit: '" + conf.getSplitResult() + "'");
    }
    boolean isSplintTypeNone = optionalWorkspaceConfigSplit.get() == WorkspaceConfigSplit.NONE;
    WorkspaceContentCreator contentCreator = new WorkspaceContentCreator();
    WorkspaceContent content = contentCreator.createWorkspaceContentForWorkspace(workspaceName);
    content.unversionedSetSplit(conf.getSplitResult());
    String xml = isSplintTypeNone ? null : new WorkspaceContentXmlConverter().convertToXml(content);
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = rm.getRevision(null, null, workspaceName);
    String path = RevisionManagement.getPathForRevision(PathType.ROOT, revision);
    try {
      removeObsoleteFilesAndDirs(path);
      if(optRepConn.isPresent()) {
        RepositoryConnection repositoryConnection = optRepConn.get();
        closeOpenDifferenceLists(repositoryConnection);
        deleteWsXmlComplete(path, repositoryConnection);
        deleteConfigDirComplete(path, repositoryConnection);
        
        if(!isSplintTypeNone) {
          writeSplit(content, path, repositoryConnection);
        } else {
          writeWsXml(path, repositoryConnection, xml);
        }
      } else {
        if(!isSplintTypeNone) {
          writeSplitFiles(content, new File(path, WorkspaceContentCreator.WORKSPACE_XML_SPLITNAME));
        } else {
          FileUtils.writeStringToFile(xml, new File(path, WorkspaceContentCreator.WORKSPACE_XML_FILENAME));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  public String createWorkspaceXmlString(String workspaceName) {
    WorkspaceContentCreator contentCreator = new WorkspaceContentCreator();
    WorkspaceContent content = contentCreator.createWorkspaceContentForWorkspace(workspaceName);
    WorkspaceContentXmlConverter converter = new WorkspaceContentXmlConverter();
    return converter.convertToXml(content);
  }
  
  private void closeOpenDifferenceLists(RepositoryConnection repconn) {
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    WorkspaceDifferenceListStorage storage = new WorkspaceDifferenceListStorage();
    List<? extends WorkspaceContentDifferences> difflist = storage.loadDifferencesLists(repconn.getWorkspaceName(), false);
    for (WorkspaceContentDifferences diff : difflist) {    
      portal.closeDifferenceList(diff.getListId());
    }
  }
  
  private void writeWsXml(String path, RepositoryConnection repositoryConnection, String xml) throws Exception {
    deleteWsXmlLink(path);
    File workspaceXmlFile = getWsXmlFilePath(repositoryConnection).toFile();
    FileUtils.writeStringToFile(xml, workspaceXmlFile);
    createWsXmlLinkIfNotExists(path, repositoryConnection);
  }
  
  
  private void removeExistingFiles(String path) {
    if (Files.exists(Path.of(path, WorkspaceContentCreator.WORKSPACE_XML_SPLITNAME))) {
      try (Stream<Path> files = Files.list(Path.of(path, WorkspaceContentCreator.WORKSPACE_XML_SPLITNAME))) {
        files.forEach(x -> FileUtils.deleteFileWithRetries(x.toFile()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
  

  private void writeSplit(WorkspaceContent content, String revisionPathStr, RepositoryConnection repconn) throws Exception {
    Path configFolder = createOrGetConfigDir(repconn);
    createConfigDirLinkIfNotExists(revisionPathStr, configFolder);
    deleteConfigDirContent(repconn);
    File configFolderFile = configFolder.toFile(); 
    writeSplitFiles(content, configFolderFile);
  }

  
  private void writeSplitFiles(WorkspaceContent content, File configFolder) throws Exception {
    WorkspaceContentXmlConverter converter = new WorkspaceContentXmlConverter();
    List<Pair<String, String>> data = converter.splitContent(content);
    for (Pair<String, String> entry : data) {
      File fi = new File(configFolder, entry.getFirst());
      FileUtils.writeStringToFile(entry.getSecond(), fi);
    }
  }
  
  private void removeObsoleteFilesAndDirs(String revisionPathStr) throws IOException {
    Path rootPath = Paths.get(revisionPathStr);
    Path wsxml = rootPath.resolve(WorkspaceContentCreator.WORKSPACE_XML_FILENAME);
    if (Files.exists(wsxml) && !Files.isSymbolicLink(wsxml)) {
      Files.delete(wsxml);
    }
    Path configDir = rootPath.resolve(WorkspaceContentCreator.WORKSPACE_XML_SPLITNAME);
    if (Files.exists(configDir) && !Files.isSymbolicLink(configDir)) {
      removeExistingFiles(revisionPathStr);
      Files.delete(configDir);
    }
  }
  
  
  private void deleteWsXmlComplete(String revisionPathStr, RepositoryConnection repconn) throws IOException {
    deleteWsXmlLink(revisionPathStr);
    deleteWsXmlFile(repconn);
  }
  
  private void deleteWsXmlLink(String revisionPathStr) throws IOException {
    Path wsxml = Paths.get(revisionPathStr, WorkspaceContentCreator.WORKSPACE_XML_FILENAME);
    if (Files.exists(wsxml) && Files.isSymbolicLink(wsxml)) {
      Files.delete(wsxml);
    }
  }
  
  private void createWsXmlLinkIfNotExists(String revisionPathStr, RepositoryConnection repconn) throws IOException {
    Path wsxml = getWsXmlFilePath(repconn);
    Path wsxmlLink = Paths.get(revisionPathStr, WorkspaceContentCreator.WORKSPACE_XML_FILENAME);
    if (!Files.exists(wsxmlLink)) {
      Files.createSymbolicLink(wsxmlLink, wsxml);
    }
  }
  
  private void deleteWsXmlFile(RepositoryConnection repconn) throws IOException {
    Path wsxml = getWsXmlFilePath(repconn);
    if (Files.exists(wsxml)) {
      Files.delete(wsxml);
    }
  }
  
  
  private Path getWsXmlFilePath(RepositoryConnection repconn) throws IOException {
    Path rootPath = getWorkspacePath(repconn);
    return rootPath.resolve(WorkspaceContentCreator.WORKSPACE_XML_FILENAME);
  }
  
  private Path getWorkspacePath(RepositoryConnection repconn) throws IOException {
    return Paths.get(repconn.getPath(), repconn.getSubpath());
  }
  
  
  private void deleteConfigDirComplete(String revisionPathStr, RepositoryConnection repconn) throws IOException {
    deleteConfigDirLink(revisionPathStr);
    deleteConfigDir(repconn);
  }
  
  private void deleteConfigDirLink(String revisionPathStr) throws IOException {
    Path configDir = getPathOfConfigDirLink(revisionPathStr);
    if (Files.exists(configDir) && Files.isSymbolicLink(configDir)) {
      Files.delete(configDir);
    }
  }
  
  private void deleteConfigDir(RepositoryConnection repconn) throws IOException {
    Path configDir = getPathOfConfigDir(repconn);
    if (Files.exists(configDir)) {
      FileUtils.deleteDirectory(configDir.toFile());
    }
  }
  
  private void deleteConfigDirContent(RepositoryConnection repconn) throws IOException {
    Path configDir = getPathOfConfigDir(repconn);
    if (Files.exists(configDir)) {
      try (Stream<Path> files = Files.list(configDir)) {
        files.forEach(x -> FileUtils.deleteFileWithRetries(x.toFile()));
      }
    }
  }
  
  private Path createOrGetConfigDir(RepositoryConnection repconn) throws IOException {
    Path configDir = getPathOfConfigDir(repconn);
    if (!Files.exists(configDir)) {
      Files.createDirectory(configDir);
    }
    return configDir;
  }
  
  private void createConfigDirLinkIfNotExists(String revisionPathStr, Path configDir) throws IOException {
    Path configDirLink = getPathOfConfigDirLink(revisionPathStr);
    if (!Files.exists(configDirLink)) {
      Files.createSymbolicLink(configDirLink, configDir);
    }
  }
  
  private Path getPathOfConfigDir(RepositoryConnection repconn) throws IOException {
    Path rootPath = getWorkspacePath(repconn);
    return rootPath.resolve(WorkspaceContentCreator.WORKSPACE_XML_SPLITNAME);
  }
  
  private Path getPathOfConfigDirLink(String revisionPathStr) throws IOException {
    return Paths.get(revisionPathStr, WorkspaceContentCreator.WORKSPACE_XML_SPLITNAME);
  }
  
}
