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
package xmcp.gitintegration.cli.impl;


import java.io.OutputStream;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xmcp.gitintegration.WorkspaceXmlCreationConfig;
import xmcp.gitintegration.cli.generated.Createworkspacexml;
import xmcp.gitintegration.cli.tools.CreateWorkspaceXmlTools;
import xmcp.gitintegration.cli.tools.CreateWorkspaceXmlTools.XmlCreationMode;


public class CreateworkspacexmlImpl extends XynaCommandImplementation<Createworkspacexml> {

  public void execute(OutputStream statusOutputStream, Createworkspacexml payload) throws XynaException {
    CreateWorkspaceXmlTools tools = new CreateWorkspaceXmlTools();
    WorkspaceXmlCreationConfig conf = new WorkspaceXmlCreationConfig();
    conf.setWorkspaceName(payload.getWorkspaceName());
    conf.setSplitResult(payload.getSplitResult());
    conf.setForce(payload.getForce());
    
    XmlCreationMode mode = payload.getPrintResult() ? XmlCreationMode.ONLY_CREATE_STRING : XmlCreationMode.WRITE_FILE; 
    String xml = tools.execute(conf, mode);
    if (payload.getPrintResult()) {
      writeLineToCommandLine(statusOutputStream, xml);
    }
  }
  
}
