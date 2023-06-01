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
package com.gip.xyna.xfmg.xfctrl.appmgmt;


import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;


public class ApplicationDefinitionInformation extends ApplicationInformation {

  private static final long serialVersionUID = 1L;

  private Workspace parentWorkspace;
  
  public ApplicationDefinitionInformation(String name, String version, Workspace parentWorkspace, String comment) {
    super(name, version, ApplicationState.OK, comment);
    this.parentWorkspace = parentWorkspace;
  }
  
  
  public Workspace getParentWorkspace() {
    return parentWorkspace;
  }
  
  public RuntimeDependencyContextType getRuntimeDependencyContextType() {
    return RuntimeDependencyContextType.ApplicationDefinition;
  }
  
  public RuntimeContext asRuntimeContext() {
    return getParentWorkspace();
  }
  
}
