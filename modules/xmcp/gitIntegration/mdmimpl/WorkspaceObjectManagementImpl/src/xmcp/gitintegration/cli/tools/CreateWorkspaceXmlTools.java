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

package xmcp.gitintegration.cli.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
import xmcp.gitintegration.impl.WorkspaceContentCreator;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;
import xmcp.gitintegration.impl.xml.WorkspaceContentXmlConverter;
import xmcp.gitintegration.repository.RepositoryConnection;
import xmcp.gitintegration.storage.WorkspaceDifferenceListStorage;
import xprc.xpce.Workspace;


public class CreateWorkspaceXmlTools {
  
  public static enum XmlCreationMode {
    ONLY_CREATE_STRING, WRITE_FILE
  }
  
  public static enum SplitModeChange {
    NOT_CHANGED, CHANGED
  }
  
  public void execute(String workspaceName) {
    try {
      RepositoryConnection repositoryConnection = RepositoryManagement.getRepositoryConnection(new Workspace(workspaceName));
      WorkspaceXmlCreationConfig conf = new WorkspaceXmlCreationConfig();
      conf.unversionedSetWorkspaceName(workspaceName);
      conf.unversionedSetForce(false);
      conf.unversionedSetSplitResult(repositoryConnection.getSplitted());
      executeImpl(conf, XmlCreationMode.WRITE_FILE, Optional.ofNullable(repositoryConnection), SplitModeChange.NOT_CHANGED);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  
  public String execute(WorkspaceXmlCreationConfig conf, XmlCreationMode mode) throws XynaException {
    if (mode == XmlCreationMode.ONLY_CREATE_STRING) {
      return executeImpl(conf, mode, Optional.empty(), SplitModeChange.NOT_CHANGED);
    }
    String workspaceName = conf.getWorkspaceName();
    RepositoryConnection repositoryConnection = RepositoryManagement.getRepositoryConnection(new Workspace(workspaceName));
    SplitModeChange splitModeChange = SplitModeChange.NOT_CHANGED;
    if (repositoryConnection.getSplitted() != conf.getSplitResult()) {
      if (!conf.getForce()) {
        throw new RuntimeException("Use force to change the configuration between single file and splitted");
      }
      splitModeChange = SplitModeChange.CHANGED;
    }
    repositoryConnection.setSplitted(conf.getSplitResult());
    RepositoryManagement.updateRepositoryConnection(repositoryConnection);
    return executeImpl(conf, mode, Optional.ofNullable(repositoryConnection), splitModeChange);
  }
  
  
  private String executeImpl(WorkspaceXmlCreationConfig conf, XmlCreationMode mode, 
                             Optional<RepositoryConnection> optRepConn, SplitModeChange splitModeChange) 
                             throws XynaException {
    String workspaceName = conf.getWorkspaceName();
    WorkspaceContentCreator contentCreator = new WorkspaceContentCreator();
    WorkspaceContent content = contentCreator.createWorkspaceContentForWorkspace(workspaceName);
    WorkspaceContentXmlConverter converter = new WorkspaceContentXmlConverter();
    String xml = converter.convertToXml(content);
    if (mode == XmlCreationMode.ONLY_CREATE_STRING) {
      return xml;
    }
    if (!optRepConn.isPresent()) {
      throw new IllegalArgumentException("Parameter RepositoryConnection is empty");
    }
    RepositoryConnection repositoryConnection = optRepConn.get();
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = rm.getRevision(null, null, workspaceName);
    String path = RevisionManagement.getPathForRevision(PathType.ROOT, revision);
    closeOpenDifferenceLists(repositoryConnection);
    try {
      removeObsoleteFilesAndDirs(path);
      if (conf.getSplitResult()) {
        if (splitModeChange == SplitModeChange.CHANGED) {
          deleteWsXmlComplete(path, repositoryConnection);
        }
        writeSplit(content, path, repositoryConnection);
      } else {
        if (splitModeChange == SplitModeChange.CHANGED) {
          deleteConfigDirComplete(path, repositoryConnection);
        }
        writeWsXml(path, repositoryConnection, xml);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return xml;
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
    WorkspaceContentXmlConverter converter = new WorkspaceContentXmlConverter();
    Path configFolder = createOrGetConfigDir(repconn);
    createConfigDirLinkIfNotExists(revisionPathStr, configFolder);
    
    List<Pair<String, String>> data = converter.split(content);
    deleteConfigDirContent(repconn);
    File configFolderFile = configFolder.toFile(); 
    for (Pair<String, String> entry : data) {
      File fi = new File(configFolderFile, entry.getFirst());
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
