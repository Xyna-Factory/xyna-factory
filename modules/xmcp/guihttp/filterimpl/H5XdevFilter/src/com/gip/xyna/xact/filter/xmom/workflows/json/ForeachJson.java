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



import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.session.View;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.xmom.MetaXmomContainers;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.ForEachScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;

import xmcp.processmodeller.datatypes.Foreach;


public class ForeachJson extends XMOMGuiJson implements HasXoRepresentation {

  private View view;
  private StepForeach stepForeach;
  private ObjectId foreachId;
  private IdentifiedVariables identifiedVariables;

  public ForeachJson() {
    
  }

  public ForeachJson(View view, StepForeach stepForeach) {
    this.view = view;
    this.stepForeach = stepForeach;
    this.foreachId = ObjectId.createStepId(stepForeach);
    this.identifiedVariables = view.getGenerationBaseObject().identifyVariables(foreachId); // Problem: fuer diese Id bekommt man einen StepSerial anstatt einem StepForeach
  }
  
  @Override
  public GeneralXynaObject getXoRepresentation() {
    Foreach f = new Foreach();
    f.setId(foreachId.getObjectId());
    f.setParallelExecution(stepForeach.getParallelExecution());
    f.setDeletable(true);
    String[] itemTypes = new String[] { MetaXmomContainers.DATA_FQN, MetaXmomContainers.EXCEPTION_FQN };
    f.addToAreas(ServiceUtils.createVariableArea(
                        view.getGenerationBaseObject(), foreachId, VarUsageType.input, identifiedVariables, Tags.FOREACH_INPUT, itemTypes, true));
    
    ForEachScopeStep scopeStep = (ForEachScopeStep)stepForeach.getChildSteps().get(0);
    Step stepToLoop = scopeStep.getChildStep();
    WorkflowStepVisitor workflowStepVisitor = new WorkflowStepVisitor(view, stepToLoop);
    f.addToAreas(ServiceUtils.createContentArea(workflowStepVisitor.createWorkflowSteps(), ObjectId.createStepId(stepToLoop).getObjectId(), Tags.SERVICE_CONTENT, true));
    f.addToAreas(ServiceUtils.createVariableArea(
                        view.getGenerationBaseObject(), foreachId, VarUsageType.output, identifiedVariables, Tags.FOREACH_OUTPUT, itemTypes, true));
    return f;
  }

  public static class ForeachJsonVisitor extends EmptyJsonVisitor<ForeachJson>{
    ForeachJson fj = new ForeachJson();

    @Override
    public ForeachJson get() {
      return fj;
    }

    @Override
    public ForeachJson getAndReset() {
      ForeachJson ret = fj;
      fj = new ForeachJson();
      return ret;
    }
  }
}
