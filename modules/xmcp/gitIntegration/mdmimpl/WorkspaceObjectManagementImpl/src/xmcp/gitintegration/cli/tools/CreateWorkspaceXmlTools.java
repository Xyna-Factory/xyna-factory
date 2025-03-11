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
import java.util.List;
import java.util.stream.Stream;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;

import xmcp.gitintegration.RepositoryManagement;
import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceXmlCreationConfig;
import xmcp.gitintegration.impl.WorkspaceContentCreator;
import xmcp.gitintegration.impl.xml.WorkspaceContentXmlConverter;
import xmcp.gitintegration.repository.RepositoryConnection;
import xprc.xpce.Workspace;


public class CreateWorkspaceXmlTools {
  
  public static enum XmlCreationMode {
    ONLY_CREATE_STRING, WRITE_FILE
  }
  
  public void execute(String workspaceName) {
    try {
      RepositoryConnection repositoryConnection = RepositoryManagement.getRepositoryConnection(new Workspace(workspaceName));
      WorkspaceXmlCreationConfig conf = new WorkspaceXmlCreationConfig();
      conf.unversionedSetWorkspaceName(workspaceName);
      conf.unversionedSetForce(false);
      conf.unversionedSetSplitResult(repositoryConnection.getSplitted());
      executeImpl(conf, XmlCreationMode.WRITE_FILE);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  
  public String execute(WorkspaceXmlCreationConfig conf, XmlCreationMode mode) throws XynaException {
    if (mode == XmlCreationMode.ONLY_CREATE_STRING) {
      return executeImpl(conf, mode);
    }
    String workspaceName = conf.getWorkspaceName();
    RepositoryConnection repositoryConnection = RepositoryManagement.getRepositoryConnection(new Workspace(workspaceName));
    if(repositoryConnection.getSplitted() != conf.getSplitResult() && !conf.getForce()) {
      throw new RuntimeException("Use force to change the configuration between single file and splitted");
    }
    repositoryConnection.setSplitted(conf.getSplitResult());
    RepositoryManagement.updateRepositoryConnection(repositoryConnection);
    return executeImpl(conf, mode);
  }
  
  
  private String executeImpl(WorkspaceXmlCreationConfig conf, XmlCreationMode mode) throws XynaException {
    String workspaceName = conf.getWorkspaceName();
    WorkspaceContentCreator contentCreator = new WorkspaceContentCreator();
    WorkspaceContent content = contentCreator.createWorkspaceContentForWorkspace(workspaceName);
    WorkspaceContentXmlConverter converter = new WorkspaceContentXmlConverter();
    String xml = converter.convertToXml(content);    
    if (mode == XmlCreationMode.ONLY_CREATE_STRING) {
      return xml;
    }
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = rm.getRevision(null, null, workspaceName);
    String path = RevisionManagement.getPathForRevision(PathType.ROOT, revision);
    if (!conf.getSplitResult()) {
      removeExistingFiles(path);
      File workspaceXmlFile = new File(path, WorkspaceContentCreator.WORKSPACE_XML_FILENAME);
      FileUtils.writeStringToFile(xml, workspaceXmlFile);
    } else {
      writeSplit(content, path);
    }
    return xml;
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


  private void writeSplit(WorkspaceContent content, String path) {
    WorkspaceContentXmlConverter converter = new WorkspaceContentXmlConverter();
    File configFolder = new File(path, WorkspaceContentCreator.WORKSPACE_XML_SPLITNAME);
    List<Pair<String, String>> data = converter.split(content);

    removeExistingFiles(path);
    
    try {
      if(!Files.exists(configFolder.toPath())) {
        Files.createDirectories(configFolder.toPath());
      }
      //write new files
      for (Pair<String, String> entry : data) {
        File fi = new File(configFolder, entry.getFirst());
        FileUtils.writeStringToFile(entry.getSecond(), fi);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
