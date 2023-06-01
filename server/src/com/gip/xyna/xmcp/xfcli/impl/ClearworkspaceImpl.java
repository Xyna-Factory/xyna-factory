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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.util.Arrays;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.ClearWorkspaceParameters;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Clearworkspace;



public class ClearworkspaceImpl extends XynaCommandImplementation<Clearworkspace> {

  public void execute(OutputStream statusOutputStream, Clearworkspace payload) throws XynaException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(null, null, payload.getWorkspaceName());
    
    WorkspaceManagement workspaceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();

    ClearWorkspaceParameters params = new ClearWorkspaceParameters();
    params.setIgnoreRunningOrders(payload.getForce());
    if (payload.getRemoveSubtypesOf() != null) {
      params.setRemoveSubtypesOf(Arrays.asList(payload.getRemoveSubtypesOf()));
    }
    
    CommandControl.tryLock(Operation.APPLICATION_CLEAR_WORKINGSET, revision);
    CommandControl.unlock(Operation.APPLICATION_CLEAR_WORKINGSET, revision); //kurz danach wird writelock geholt, das kann nicht upgegraded werden
    workspaceManagement.clearWorkspace(new Workspace(payload.getWorkspaceName()), params);
  }

}
