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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import java.io.OutputStream;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.generated.Listworkspacedetails;



public class ListworkspacedetailsImpl extends XynaCommandImplementation<Listworkspacedetails> {

  public void execute(OutputStream statusOutputStream, Listworkspacedetails payload) throws XynaException {
    CommandLineWriter clw = CommandLineWriter.createCommandLineWriter(statusOutputStream);

    WorkspaceManagement workspaceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    Workspace ws = new Workspace(payload.getWorkspaceName());
    WorkspaceInformation wsInfo = workspaceManagement.getWorkspaceDetails(ws, true);
    
    if (wsInfo == null) {
      clw.writeLineToCommandLine("No information found for "+ws+".");
      return;
    }
    
    //Ausgabe
    showWorkspaceName(clw, wsInfo);
    showWorkspaceInfo(clw, wsInfo);
    showWorkspaceProblems(clw, wsInfo);
  }

  private void showWorkspaceName(CommandLineWriter clw, WorkspaceInformation wsInfo) {
    clw.writeLineToCommandLine(wsInfo.getWorkspace().getName());
  }
  
  private void showWorkspaceInfo(CommandLineWriter clw, WorkspaceInformation wsInfo) {
    StringBuilder header = new StringBuilder();
    
    header.append("\n state ").append(wsInfo.getState());
    if (wsInfo.getRequirements() != null) {
      switch (wsInfo.getRequirements().size()) {
        case 0: 
          break;
        case 1: header.append("\n requires ").append(wsInfo.getRequirements().iterator().next());
          break;
        default:
          header.append("\n requires: ");
          for (RuntimeDependencyContext rc : wsInfo.getRequirements()) {
            header.append("\n  ").append(rc);
          }
      }
    }
    clw.writeLineToCommandLine(header);
  }
  
  private void showWorkspaceProblems(CommandLineWriter clw, WorkspaceInformation wsInfo) {
    StringBuilder header = new StringBuilder();
    if (wsInfo.getProblems() != null) {
      switch (wsInfo.getProblems().size()) {
        case 0: 
          break;
        case 1:
          header.append("\n has problem ")
                .append(wsInfo.getProblems().iterator().next().getMessage());
          break;
        default:
          header.append("\n has problems: ");
          for(RuntimeContextProblem p : wsInfo.getProblems()) {
            header.append("\n  ").append(p.getMessage());
          }
      }
      clw.writeLineToCommandLine(header);
    }
  }
}
