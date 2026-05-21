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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.CreateWorkspaceResult;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Createworkspace;



public class CreateworkspaceImpl extends XynaCommandImplementation<Createworkspace> {

  public void execute(OutputStream statusOutputStream, Createworkspace payload) throws XynaException {
    TemporarySessionAuthentication tsa =
                    TemporarySessionAuthentication.tempAuthWithUniqueUserAndOperationLock("CreateWorkspace", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE, RevisionManagement.REVISION_DEFAULT_WORKSPACE,
                                                                                          CommandControl.Operation.WORKSPACE_CREATE);
    tsa.initiate();
    try {
      WorkspaceManagement workspaceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
      Workspace workspace = new Workspace(payload.getWorkspaceName());

      Long preferredRevision = null;
      if (payload.getRevision() != null) {
        try {
          preferredRevision = Long.parseLong(payload.getRevision());
        }
        catch (Exception e) {
          throw new IllegalArgumentException("Revision is no integer number", e);
        }
      }
      CreateWorkspaceResult result = workspaceManagement.createWorkspace(workspace, tsa.getUsername(), preferredRevision);
      
      switch (result.getResult()) {
        case Success:
          writeLineToCommandLine(statusOutputStream, "Workspace successfully created.");
          break;
        case Warnings:
          writeLineToCommandLine(statusOutputStream, "Workspace created, but found " + result.getWarnings().size() + " warnings:");
          for (String warn : result.getWarnings()) {
            writeLineToCommandLine(statusOutputStream, warn);
          }
          break;
        case Failed:
          writeLineToCommandLine(statusOutputStream, "Could not create workspace.");
          break;
        default:
          writeLineToCommandLine(statusOutputStream, "Unexpected response "+result.getResult()+".");
      }
    } finally {
      tsa.destroy();
    }
  }

}
