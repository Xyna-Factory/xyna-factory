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
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement.ChangeResult;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Removeruntimecontextdependency;



public class RemoveruntimecontextdependencyImpl extends XynaCommandImplementation<Removeruntimecontextdependency> {

  public void execute(OutputStream statusOutputStream, Removeruntimecontextdependency payload) throws XynaException {
    RuntimeDependencyContext owner = RuntimeContextDependencyManagement.getRuntimeDependencyContext(payload.getOwnerApplicationName(), payload.getOwnerVersionName(), payload.getOwnerWorkspaceName());

    if (owner == null) {
      throw new IllegalArgumentException("no owner runtime context specified");
    }
    
    RuntimeDependencyContext requirement = RuntimeContextDependencyManagement.getRuntimeDependencyContext(payload.getRequirementApplicationName(), payload.getRequirementVersionName(), payload.getRequirementWorkspaceName());
    
    if (requirement == null) {
      throw new IllegalArgumentException("no dependent runtime context specified");
    }
    
    ChangeResult result;
    RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    result = rcdMgmt.removeDependency(owner, requirement, TemporarySessionAuthentication.TEMPORARY_CLI_USER_NAME);
    
    switch(result) {
      case Succeeded:
        writeLineToCommandLine(statusOutputStream, "Successfully removed runtime context dependency.");
        break;
      case NoChange:
        writeLineToCommandLine(statusOutputStream, "Runtime context dependency does not exist.");
        break;
      case Failed:
        writeLineToCommandLine(statusOutputStream, "Could not remove runtime context dependency.");
    }
  }

}
