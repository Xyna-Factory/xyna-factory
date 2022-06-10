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
package com.gip.xyna.xact.filter.xmom.workflows.json;



import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.session.View;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;



public class StepCatchJson implements HasXoRepresentation {

  private final View view;
  private final StepFunction tryStep;
  private final CompensationJson compensationJson;
  private final DistinctionJson exceptionHandlingJson;


  public StepCatchJson(View view, StepCatch stepCatch) {
    this.view = view;
    this.tryStep = (StepFunction)stepCatch.getStepInTryBlock(); // TODO: Is this always a StepFunction?
    this.compensationJson = new CompensationJson(view, tryStep.getCompensateStep());
    this.exceptionHandlingJson = new DistinctionJson(view, ObjectId.createExceptionHandlingId(tryStep), "", DistinctionType.ExceptionHandling,
                                                     stepCatch.getBranchesForGUI(), stepCatch.getHandledCases(), stepCatch.getUnhandledCases(true));
  }
  
  @Override
  public GeneralXynaObject getXoRepresentation() {
    StepFunctionJson tryStepJson = new StepFunctionJson(view, tryStep, exceptionHandlingJson, compensationJson);
    return tryStepJson.getXoRepresentation();
  }

}
