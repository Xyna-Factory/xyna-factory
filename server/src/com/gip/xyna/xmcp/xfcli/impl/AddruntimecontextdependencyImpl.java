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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement.ChangeResult;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Addruntimecontextdependency;



public class AddruntimecontextdependencyImpl extends XynaCommandImplementation<Addruntimecontextdependency> {

  public void execute(OutputStream statusOutputStream, Addruntimecontextdependency payload) throws XynaException {
    RuntimeDependencyContext owner = null;
    if (payload.getOwnerApplicationName() != null && payload.getOwnerWorkspaceName() != null) {
      owner = new ApplicationDefinition(payload.getOwnerApplicationName(), new Workspace(payload.getOwnerWorkspaceName()));
    } else if (payload.getOwnerApplicationName() != null || payload.getOwnerWorkspaceName() != null){
      owner = from(RevisionManagement.getRuntimeContext(payload.getOwnerApplicationName(), payload.getOwnerVersionName(), payload.getOwnerWorkspaceName()));
    }
    
    if (owner == null) {
      throw new IllegalArgumentException("no owner runtime context specified");
    }
    
    RuntimeDependencyContext requirement = null;
    if (payload.getRequirementApplicationName() != null && payload.getRequirementWorkspaceName() != null) {
      requirement = new ApplicationDefinition(payload.getRequirementApplicationName(), new Workspace(payload.getRequirementWorkspaceName()));
    } else if (payload.getRequirementApplicationName() != null || payload.getRequirementWorkspaceName() != null){
      requirement = from(RevisionManagement.getRuntimeContext(payload.getRequirementApplicationName(), payload.getRequirementVersionName(), payload.getRequirementWorkspaceName()));
    }
    
    if (requirement == null) {
      throw new IllegalArgumentException("no dependent runtime context specified");
    }
    
    ChangeResult result;
    RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    result = rcdMgmt.addDependency(owner, requirement, "CLI Access", payload.getForce());
    
    switch(result) {
      case Succeeded:
        writeLineToCommandLine(statusOutputStream, "Successfully added runtime context dependency.");
        break;
      case NoChange:
        writeLineToCommandLine(statusOutputStream, "Runtime context dependency already exists.");
        break;
      case Failed:
        writeLineToCommandLine(statusOutputStream, "Could not add runtime context dependency.");
    }
  }
  
  private static RuntimeDependencyContext from(RuntimeContext rc) {
    switch(rc.getType()) {
      case Application:
        return (Application)rc;
      case Workspace:
        return (Workspace)rc;
      default: 
        throw new IllegalArgumentException("Not a RuntimeDependencyContext: " + rc);
    }
  }

}
