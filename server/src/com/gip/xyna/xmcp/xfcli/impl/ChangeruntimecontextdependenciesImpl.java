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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Changeruntimecontextdependencies;



public class ChangeruntimecontextdependenciesImpl extends XynaCommandImplementation<Changeruntimecontextdependencies> {
  
  private final static Pattern CHANGE_SEPERATION_PATTERN = Pattern.compile("([ar]):(.+)");
  

  public void execute(OutputStream statusOutputStream, Changeruntimecontextdependencies payload) throws XynaException {
    RuntimeDependencyContext owner = null;
    if (payload.getOwnerApplicationName() != null && payload.getOwnerWorkspaceName() != null) {
      owner = new ApplicationDefinition(payload.getOwnerApplicationName(), new Workspace(payload.getOwnerWorkspaceName()));
    } else if (payload.getOwnerApplicationName() != null || payload.getOwnerWorkspaceName() != null){
      owner = from(RevisionManagement.getRuntimeContext(payload.getOwnerApplicationName(), payload.getOwnerVersionName(), payload.getOwnerWorkspaceName()));
    }
    
    if (owner == null) {
      throw new IllegalArgumentException("no owner runtime context specified");
    }
    
    Set<RuntimeDependencyContext> additions = new HashSet<>();
    Set<RuntimeDependencyContext> removals = new HashSet<>();
    for (String change : payload.getChanges()) {
      Matcher changeMatcher = CHANGE_SEPERATION_PATTERN.matcher(change);
      if (changeMatcher.matches()) {
        RuntimeDependencyContext rcd;
        String rdcDescription = changeMatcher.group(2);
        if (rdcDescription.contains("//")) {
          rcd = ApplicationDefinition.deserializeFromString(rdcDescription);
          if (rcd == null) {
            throw new IllegalArgumentException(rdcDescription + " is no valid RuntimeDependencyContext");
          }
        } else {
          RuntimeContext rc = RuntimeContext.valueOf(changeMatcher.group(2));
          if (rc == null) {
            throw new IllegalArgumentException(rdcDescription + " is no valid RuntimeDependencyContext");
          }
          rcd = from(rc);
        }
        switch (changeMatcher.group(1)) {
          case "a" :
            additions.add(rcd);
            break;
          case "r" :
            removals.add(rcd);
            break;
          default :
            throw new IllegalArgumentException("Invalid change specified, please verify you're using the correct format: '([ar]):(.*)'");
        }
      } else {
        throw new IllegalArgumentException("Invalid change specified, please verify you're using the correct format: '([ar]):(.*)'");
      }
    }
    
    
    TemporarySessionAuthentication tsa =
                    TemporarySessionAuthentication.tempAuthWithUniqueUser("AddRuntimeContextDependency", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE);
    tsa.initiate();
    try {
      RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      Set<RuntimeDependencyContext> newDeps = new HashSet<>(rcdMgmt.getDependencies(owner));
      for (RuntimeDependencyContext removal : removals) {
        boolean removed = newDeps.remove(removal);
        if (!removed) {
          throw new IllegalArgumentException(removal.toString() + " could not be removed from current dependencies: " + newDeps);
        }
      }
      for (RuntimeDependencyContext addition : additions) {
        boolean added = newDeps.add(addition);
        if (!added) {
          throw new IllegalArgumentException(addition.toString() + " could not be added to dependencies: " + newDeps);
        }
      }
      rcdMgmt.modifyDependencies(owner, newDeps, tsa.getUsername(), payload.getForce(), true);
      writeLineToCommandLine(statusOutputStream, "Successfully changed runtime context dependencies.");
    } finally {
      tsa.destroy();
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
