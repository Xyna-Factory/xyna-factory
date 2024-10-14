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
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.RemoveWorkspaceParameters;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Removeworkspace;



public class RemoveworkspaceImpl extends XynaCommandImplementation<Removeworkspace> {

  public void execute(OutputStream statusOutputStream, Removeworkspace payload) throws XynaException {
    Workspace workspace = new Workspace(payload.getWorkspaceName());
    
    Long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                    .getRevision(workspace);

    RemoveWorkspaceParameters params = new RemoveWorkspaceParameters();
    params.setForce(payload.getForce());
    params.setCleanupXmls(payload.getCleanupXmls());
    
    CommandControl.tryLock(Operation.WORKSPACE_REMOVE, revision);
    CommandControl.unlock(Operation.WORKSPACE_REMOVE, revision); //kurz danach wird writelock geholt, das kann nicht upgegraded werden

    TemporarySessionAuthentication tsa =
                    TemporarySessionAuthentication.tempAuthWithUniqueUser("RemoveWorkspace", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE);
    tsa.initiate();
    try {
      params.setUser(tsa.getUsername());
      WorkspaceManagement workspaceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
      workspaceManagement.removeWorkspace(workspace, params);
    } finally {
      tsa.destroy();
    }
    
    if (payload.getCleanupXmls()) {
      writeLineToCommandLine(statusOutputStream, "Successfully removed complete workspace.");
    } else {
      writeLineToCommandLine(statusOutputStream, "Successfully removed workspace, but parts for audits have been preserved. Use '-c' to remove completely.");
    }
  }
}
