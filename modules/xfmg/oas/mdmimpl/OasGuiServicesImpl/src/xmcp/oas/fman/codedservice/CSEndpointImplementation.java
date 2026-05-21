/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xmcp.oas.fman.codedservice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.CreateWorkspaceResult;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.CreateWorkspaceResult.Result;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;

import xmcp.oas.fman.codedservice.parameter.CreateImplWfParameter;
import xmcp.oas.fman.codedservice.parameter.CreateImplWfParameter.SignatureVariable;
import xmcp.oas.fman.datatypes.EndpointImplementationCreationData;

public class CSEndpointImplementation {

  public void execute(XynaOrderServerExtension order, EndpointImplementationCreationData data) {
    if(data == null) { throw new IllegalArgumentException("data null"); }
    if(data.getGeneratedRtcRevision() == null)  { throw new IllegalArgumentException("Generated Rtc revision null"); }
    switch(data.getAction()) {
      case "SetImplementationWorkspace": configureImplementationWorkspace(data); break;
      case "CreateEndpointDatatype": createEndpointDatatype(order, data); break;
      case "CreateEndpointWorkflow": createEndpointWorkflow(order, data); break;
      case "LinkEndpointWorkflow": linkEndpointWorkflow(order, data); break;
      default: throw new RuntimeException("Unexpected Action " + data.getAction());
    }
  }
  
  private void linkEndpointWorkflow(XynaOrderServerExtension order, EndpointImplementationCreationData data) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String opName = data.getServiceName();
    String dtName = data.getImplementationDatatypeFqn().substring(data.getImplementationDatatypeFqn().lastIndexOf(".")+1);
    String dtPath = data.getImplementationDatatypeFqn().substring(0, data.getImplementationDatatypeFqn().lastIndexOf("."));
    String workflowFqname = data.getImplementationWorkflowFqn();
    Long revision = data.getImplementationRtcRevision();
    try {
      FilterCallbackInteractionUtils.LinkWorkflow(runnable, opName, dtName, dtPath, workflowFqname, revision);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void configureImplementationWorkspace(EndpointImplementationCreationData data) {
    String workspaceName = data.getImplementationRtcName();
    if(workspaceName == null || workspaceName.isBlank()) { throw new IllegalArgumentException("Invalid workspace Name " + workspaceName); }
    workspaceName = workspaceName.trim();
    Long workspaceRevision = findOrCreateWorkspaceRevision(workspaceName);
    manageWorkspaceDependencies(workspaceRevision, data.getGeneratedRtcRevision());
  }
  
  private void manageWorkspaceDependencies(Long workspaceRevision, Long dependencyRevision) {
    RuntimeContextDependencyManagement rtcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();

    Set<Long> dependencies = new HashSet<>();
    rtcdMgmt.getDependenciesRecursivly(workspaceRevision, dependencies);
    if(!dependencies.contains(dependencyRevision)) {
      try {
        RuntimeDependencyContext owner = RuntimeContextDependencyManagement.asRuntimeDependencyContext(revMgmt.getRuntimeContext(workspaceRevision));
        RuntimeDependencyContext dependency = RuntimeContextDependencyManagement.asRuntimeDependencyContext(revMgmt.getRuntimeContext(dependencyRevision));
        rtcdMgmt.addDependency(owner, dependency, "OAS_Base", false);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  private Long findOrCreateWorkspaceRevision(String workspaceName) {
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Map<Long, Workspace> workspaces = revMgmt.getWorkspaces();
    for(Long revision : workspaces.keySet()) {
      Workspace candidate = workspaces.get(revision);
      if(candidate.getName().equals(workspaceName)) {
        return revision;
      }
    }
    
    //create workspace
    WorkspaceManagement workspaceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    Workspace workspace = new Workspace(workspaceName);
    try {
      CreateWorkspaceResult result = workspaceManagement.createWorkspace(workspace);
      if(result.getResult() != Result.Success) {
        throw new RuntimeException("Problem creating workspace.");
      }
      return revMgmt.getRevision(null, null, workspaceName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private void createEndpointDatatype(XynaOrderServerExtension order, EndpointImplementationCreationData data) {
    Long implRev = data.getImplementationRtcRevision();
    String label = data.getImplementationDatatypeLabel();
    String path = data.getImplementationDatatypePath();
    String apiDtFqn = data.getAPIDatatype();
    if(implRev == null) { throw new IllegalArgumentException("Implementation revision null"); }
    if(label == null || label.isBlank()) { throw new IllegalArgumentException("Implementation DT Label invalid"); }
    if(path == null || path.isBlank()) { throw new IllegalArgumentException("Implementation DT Path invalid"); }
    if(apiDtFqn == null || apiDtFqn.isBlank()) { throw new IllegalArgumentException("Api DT invalid"); }
    
    try {
      FilterCallbackInteractionUtils.createImplDt(order, label, path, apiDtFqn, implRev);
    } catch (Exception e) {
      throw new RuntimeException("could not create Datatype", e);
    }
  }

  
  private void createEndpointWorkflow(XynaOrderServerExtension order, EndpointImplementationCreationData data) {
    Long implRev = data.getImplementationRtcRevision();
    String wfLabel = data.getImplementationWorkflowLabel();
    String wfPath = data.getImplementationWorkflowPath();
    String implDtFqn = data.getImplementationDatatypeFqn();
    String serviceName = data.getServiceName();
    if (implRev == null) { throw new IllegalArgumentException("Implementation revision null"); }
    if (implDtFqn == null || implDtFqn.isBlank()) { throw new IllegalArgumentException("Api DT invalid"); }

    try {
      List<SignatureVariable> inputs = new ArrayList<>();
      List<SignatureVariable> outputs = new ArrayList<>();
      loadOperation(data, inputs, outputs);
      CreateImplWfParameter parameter = new CreateImplWfParameter(order, wfLabel, wfPath, implDtFqn, implRev, serviceName, inputs, outputs);

      FilterCallbackInteractionUtils.createImplWf(parameter);
    } catch (Exception e) {
      throw new RuntimeException("could not create Datatype", e);
    }
  }
  
  private void loadOperation(EndpointImplementationCreationData data, List<SignatureVariable> inputs, List<SignatureVariable> outputs) throws Exception{
    DOM dom = DOM.getOrCreateInstance(data.getImplementationDatatypeFqn(), new GenerationBaseCache(), data.getImplementationRtcRevision());
    dom.parseGeneration(true, false);
    Operation op = dom.getOperationByName(data.getServiceName(), true);
    op.getOutputVars();
    inputs.add(new SignatureVariable(data.getImplementationDatatypeFqn(), dom.getLabel()));
    for(AVariable input : op.getInputVars()) {
      inputs.add(new SignatureVariable(input.getFQClassName(), input.getLabel()));
    }
    for(AVariable output : op.getOutputVars()) {
      outputs.add(new SignatureVariable(output.getFQClassName(), output.getLabel()));
    }
  }
}
