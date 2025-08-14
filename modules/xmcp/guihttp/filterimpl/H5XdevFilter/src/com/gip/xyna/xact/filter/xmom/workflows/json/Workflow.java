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
package com.gip.xyna.xact.filter.xmom.workflows.json;

import java.util.List;

import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.View;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectId.ObjectPart;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.xmom.MetaXmomContainers;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.BranchInfo;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.CaseInfo;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

public class Workflow extends XMOMGuiJson implements HasXoRepresentation {

  private final View view;
  private final WF wf;
  private final ObjectPart part;
  private ObjectId id;
  private IdentifiedVariables identifiedVariables;
  private DistinctionJson exceptionHandlingJson = null;
  private Dataflow dataflow;
  private boolean saveState;
  private boolean readonly = false;

  public static ThreadLocal<Boolean> isAudit = ThreadLocal.withInitial(() -> Boolean.FALSE);

  
  public Workflow(GenerationBaseObject gbo) {
    this(gbo, ObjectPart.all );
  }
  
  public Workflow(GenerationBaseObject gbo, ObjectPart part) {
    this.view = gbo.getView();
    this.wf = gbo.getWorkflow();
    this.part = part;
    this.id = new ObjectId(ObjectType.workflow, null);
    this.identifiedVariables = view.getGenerationBaseObject().identifyVariables(id);
    this.dataflow = gbo.getDataflow();
    this.exceptionHandlingJson = createExceptionHandling();
    this.saveState = gbo.getSaveState();
  }

  @Override
  public GeneralXynaObject getXoRepresentation() {
    VariableJson.dataflowInjector.set(dataflow);
    try {
      xmcp.processmodeller.datatypes.Workflow workflow = new xmcp.processmodeller.datatypes.Workflow();
      workflow.setReadonly(readonly);
      try {
        workflow.setRtc(com.gip.xyna.xact.filter.util.Utils.getModellerRtc(wf.getRevision()));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // nothing
      }
      workflow.setFqn(wf.getFqClassName());
      workflow.setId(id.getObjectId());
      workflow.setLabel(wf.getLabel());
      workflow.setDeletable(false);
      
      workflow.addToAreas(ServiceUtils.createVariableArea(
                       view.getGenerationBaseObject(), id, 
                       VarUsageType.input, identifiedVariables, 
                       ServiceUtils.getServiceTag(VarUsageType.input), 
                       new String[] { MetaXmomContainers.DATA_FQN, MetaXmomContainers.EXCEPTION_FQN}, 
                       identifiedVariables.isReadOnly()));
      workflow.addToAreas(ServiceUtils.createLabelArea(id, wf.getLabel(), wf.getFqClassName(), saveState, false));
      workflow.addToAreas(ServiceUtils.createDocumentationArea(id, wf.getDocumentation()));
      
      WorkflowStepVisitor ws = new WorkflowStepVisitor(view, wf.getWfAsStep().getChildStep());
      workflow.addToAreas(ServiceUtils.createContentArea(ws.createWorkflowSteps(), ObjectId.createStepId(wf.getWfAsStep().getChildStep()).getObjectId(), Tags.SERVICE_CONTENT, false));
      
      workflow.addToAreas(ServiceUtils.createVariableArea(
                        view.getGenerationBaseObject(), id, 
                        VarUsageType.output, identifiedVariables, 
                        ServiceUtils.getServiceTag(VarUsageType.output), 
                        new String[] { MetaXmomContainers.DATA_FQN, MetaXmomContainers.EXCEPTION_FQN}, 
                        identifiedVariables.isReadOnly()));
      workflow.addToAreas(ServiceUtils.createExceptionHandlingArea(exceptionHandlingJson));
      
      //TODO TS Warum wird bei all ServiceUtils.parameterContentToJson(jb, view.getGenerationBaseObject(), id, VarUsageType.thrown, identifiedVariables ); nicht aufgerufen
      //TODO TS Member fehlt: workflow.setDeletable(deletable);
      //TODO TS Member fehlt: workflow.setIsAbstract(isAbstract);
      
      return workflow;
    } finally {
      VariableJson.dataflowInjector.remove();
    }
  }
  
  private DistinctionJson createExceptionHandling() {
    StepSerial mainStep = (StepSerial)wf.getWfAsStep().getChildStep();
    Step catchProxy = mainStep.getProxyForCatch();
    if (!(catchProxy instanceof StepCatch)) {
      return null;
    }

    StepCatch stepCatch = (StepCatch)catchProxy;
    List<BranchInfo> branches = stepCatch.getBranchesForGUI();
    List<CaseInfo> handledCases = stepCatch.getHandledCases();
    List<CaseInfo> unhandledCases = stepCatch.getUnhandledCases(true);
    ObjectId exceptionHandlingId = ObjectId.createExceptionHandlingId(mainStep.getParentWFObject().getWfAsStep());

    return new DistinctionJson(view, exceptionHandlingId, "", DistinctionType.ExceptionHandling, branches, handledCases, unhandledCases);
  }

  
  public boolean isReadonly() {
    return readonly;
  }

  
  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }

}
