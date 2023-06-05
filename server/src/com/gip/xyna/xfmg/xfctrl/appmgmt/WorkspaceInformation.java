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
package com.gip.xyna.xfmg.xfctrl.appmgmt;



import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.RuntimeDependencyContextInformation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceState;
import com.gip.xyna.xmcp.PluginInformation;



public class WorkspaceInformation implements Serializable, RuntimeDependencyContextInformation {

  private static final long serialVersionUID = 1L;

  private final Workspace workspace;
  private WorkspaceState state;
  private PluginInformation repositoryAccess;
  private Collection<RuntimeDependencyContext> requirements;
  private Collection<RuntimeContextProblem> problems;
  private Map<OrderEntrance, SerializablePair<Boolean, String>> orderEntranceStates;


  public WorkspaceInformation(Workspace ws) {
    this.workspace = ws;
  }


  public Workspace getWorkspace() {
    return workspace;
  }


  public WorkspaceState getState() {
    return state;
  }
  
  public void setState(WorkspaceState state) {
    this.state = state;
  }

  
  public PluginInformation getRepositoryAccess() {
    return repositoryAccess;
  }


  public void setRepositoryAccess(PluginInformation repositoryAccess) {
    this.repositoryAccess = repositoryAccess;
  }


  public Collection<RuntimeDependencyContext> getRequirements() {
    return requirements;
  }
  
  
  public void setRequirements(Collection<RuntimeDependencyContext> collection) {
    this.requirements = collection;
  }


  public Collection<RuntimeContextProblem> getProblems() {
    return problems;
  }


  public void setProblems(Collection<RuntimeContextProblem> problems) {
    this.problems = problems;
  }


  public RuntimeDependencyContextType getRuntimeDependencyContextType() {
    return RuntimeDependencyContextType.Workspace;
  }

  
  public Map<OrderEntrance, SerializablePair<Boolean, String>> getOrderEntrances() {
    return orderEntranceStates;
  }
  
  public void setOrderEntrances(Map<OrderEntrance, SerializablePair<Boolean, String>> orderEntranceStates) {
    this.orderEntranceStates = orderEntranceStates;
  }

  public RuntimeContext asRuntimeContext() {
    return getWorkspace();
  }


}
