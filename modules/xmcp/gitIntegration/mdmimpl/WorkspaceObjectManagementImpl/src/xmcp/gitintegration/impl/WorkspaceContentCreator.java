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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentItem;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;
import xmcp.gitintegration.impl.xml.WorkspaceContentXmlConverter;



public class WorkspaceContentCreator {
  
  public static final String WORKSPACE_XML_FILENAME = "workspace.xml";
  public static final String WORKSPACE_XML_SPLITNAME = "config";


  public File determineWorkspaceXMLFile(String workspaceName) {
    Long revision = getRevision(workspaceName);
    String path = RevisionManagement.getPathForRevision(PathType.ROOT, revision);
    File file = new File(path, WORKSPACE_XML_FILENAME);
    if(!file.exists()) {
      file = new File(file.getParentFile(), WORKSPACE_XML_SPLITNAME);
      if(!file.exists()) {
        throw new RuntimeException("workspace.xml does not exist for '" + workspaceName + "' at " + file.getParent());
      }
    }
    return file;
  }


  public WorkspaceContent createWorkspaceContentForWorkspace(String workspaceName) {
    WorkspaceContent result = new WorkspaceContent();
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    Long revision = getRevision(workspaceName);
    List<WorkspaceContentItem> items = portal.createItems(revision);

    result.setWorkspaceName(workspaceName);
    result.setWorkspaceContentItems(items);

    return result;
  }


  private Long getRevision(String workspaceName) {
    try {
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      return rm.getRevision(null, null, workspaceName);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException("No workspace '" + workspaceName + "' found.", e);
    }
  }


  /**
   * File is either a workspace.xml or a configuration-folder containing
   * files named after workspaceContentItem subclasses  
   */
  public WorkspaceContent createWorkspaceContentFromFile(File file) {
    WorkspaceContent result = null;
    try {
      result = file.isFile() ? createWorkspaceContentFromText(Files.readString(file.toPath())) : createWorkspaceContentFromDirectory(file);
    } catch (IOException e) {
      throw new RuntimeException("Could not read WorkspaceContent from " + file.getAbsolutePath(), e);
    }
    return result;
  }


  private WorkspaceContent createWorkspaceContentFromDirectory(File file) throws IOException {
    WorkspaceContentXmlConverter converter = new WorkspaceContentXmlConverter();
    WorkspaceContent result = new WorkspaceContent();
    for (File f : file.listFiles()) {
      String input = Files.readString(f.toPath());
      if(f.getName().equals(WORKSPACE_XML_FILENAME)) {
        WorkspaceContent c = converter.convertFromXml(input);
        result.setWorkspaceName(c.getWorkspaceName());
        continue;
      }
      converter.addToWorkspaceContent(input, result);
    }
    return result;
  }


  public WorkspaceContent createWorkspaceContentFromText(String xml) {
    WorkspaceContentXmlConverter converter = new WorkspaceContentXmlConverter();
    WorkspaceContent result = converter.convertFromXml(xml);
    return result;
  }

  
  public WorkspaceContent createWorkspaceContentFromText(List<String> list) {
    WorkspaceContentXmlConverter converter = new WorkspaceContentXmlConverter();
    List<WorkspaceContentItem> items = new ArrayList<>();
    String name = null;
    for (String input : list) {
      WorkspaceContent tmp = converter.convertFromXml(input);
      if (tmp.getWorkspaceName() != null) {
        name = tmp.getWorkspaceName();
      }
      items.addAll(tmp.getWorkspaceContentItems());
    }
    WorkspaceContent result = new WorkspaceContent();
    result.setWorkspaceName(name);
    result.setWorkspaceContentItems(items);
    return result;
  }
  
}
