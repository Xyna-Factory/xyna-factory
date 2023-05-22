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
package com.gip.xyna.xact.filter.xmom.workflows.json;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xact.filter.session.View;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.StepVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;

public class WorkflowStepVisitor extends XMOMGuiJson implements StepVisitor {

  private final View view;
  private final Step step;
  private List<GeneralXynaObject> workflowSteps = new ArrayList<>();
  
  public WorkflowStepVisitor(View view, Step step) {
    this.view = view;
    this.step = step;
  }
  
  
  public List<GeneralXynaObject> createWorkflowSteps() {
    workflowSteps.clear();
    step.visit(this);
    return workflowSteps;
  }


  @Override
  public void visit(Step step) {
    // nothing
  }
  
  @Override
  public void visitStepSerial(StepSerial step) {
    List<Step> childSteps = step.getChildSteps();
    for (Step child : childSteps) {
      child.visit(WorkflowStepVisitor.this);
    }
   }
  
  @Override
  public void visitStepParallel(StepParallel step) {
    ParallelismJson parallelismJson = new ParallelismJson(view, step, this);
    addChildStep(parallelismJson.getXoRepresentation());
  }
  
  @Override
  public void visitStepMapping(StepMapping step) {
    MappingJson mappingJson = new MappingJson(view, step);
    addChildStep(mappingJson.getXoRepresentation());
  }

  @Override
  public void visitStepFunction(StepFunction step) {
    StepFunctionJson stepFunctionJson = new StepFunctionJson(view, step);
    addChildStep(stepFunctionJson.getXoRepresentation());
  }

  @Override
  public void visitStepCatch(StepCatch step) {
    StepCatchJson stepCatchJson = new StepCatchJson(view, step);
    addChildStep(stepCatchJson.getXoRepresentation());
  }

  @Override
  public void visitStepAssign(StepAssign step) {
    //nichts zu tun
  }

  @Override
  public void visitStepForeach(StepForeach step) {
    ForeachJson foreachJson = new ForeachJson(view, step);
    addChildStep(foreachJson.getXoRepresentation());
  }
  
  @Override
  public void visitStepChoice(StepChoice step) {
    DistinctionJson distinctionJson = new DistinctionJson(view, step);
    addChildStep(distinctionJson.getXoRepresentation());
  }

  @Override
  public void visitStepThrow(StepThrow step) {
    ThrowJson throwJson = new ThrowJson(view, step);
    addChildStep(throwJson.getXoRepresentation());
  }

  @Override
  public void visitScopeStep(ScopeStep step) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visitStepRetry(StepRetry step) {
    RetryJson retryJson = new RetryJson(view, step);
    addChildStep(retryJson.getXoRepresentation());
  }

  public Step getStep() {
    return step;
  }
  
  private void addChildStep(GeneralXynaObject child) {
    if(child != null) {
      workflowSteps.add(child);
    }
  }
}
