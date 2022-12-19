/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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


  public File determineWorkspaceXMLFile(String workspaceName) {
    Long revision = getRevision(workspaceName);
    String path = RevisionManagement.getPathForRevision(PathType.ROOT, revision);
    File file = new File(path, "workspace.xml");
    return file;
  }


  public WorkspaceContent createWorkspaceContent(String workspaceName) {
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


  public WorkspaceContent createWorkspaceContentFromFile(File file) {
    try {
      String xml = Files.readString(file.toPath());
      WorkspaceContent result = createWorkspaceContentFromText(xml);
      return result;
    } catch (IOException e) {
      throw new RuntimeException("Could not read file " + file.getAbsolutePath(), e);
    }
  }


  public WorkspaceContent createWorkspaceContentFromText(String xml) {
    WorkspaceContentXmlConverter converter = new WorkspaceContentXmlConverter();
    WorkspaceContent result = converter.convertFromXml(xml);
    return result;
  }
}
