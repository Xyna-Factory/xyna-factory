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
package xmcp.gitintegration.cli.impl;



import java.io.File;
import java.io.OutputStream;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.cli.generated.Createworkspacexml;
import xmcp.gitintegration.impl.WorkspaceContentCreator;
import xmcp.gitintegration.impl.xml.WorkspaceContentXmlConverter;



public class CreateworkspacexmlImpl extends XynaCommandImplementation<Createworkspacexml> {

  public void execute(OutputStream statusOutputStream, Createworkspacexml payload) throws XynaException {
    String workspaceName = payload.getWorkspaceName();
    WorkspaceContentCreator contentCreator = new WorkspaceContentCreator();
    WorkspaceContent content = contentCreator.createWorkspaceContent(workspaceName);
    WorkspaceContentXmlConverter converter = new WorkspaceContentXmlConverter();
    String xml = converter.convertToXml(content);

    if (payload.getPrintResult()) {
      writeLineToCommandLine(statusOutputStream, xml);
    } else {
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision = rm.getRevision(null, null, workspaceName);
      String path = RevisionManagement.getPathForRevision(PathType.ROOT, revision);
      File file = new File(path, "workspace.xml");
      FileUtils.writeStringToFile(xml, file);
    }
  }

}
