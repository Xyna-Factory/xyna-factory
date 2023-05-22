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

package com.gip.xyna.xact.filter.session.modify.operations.copy;



import java.util.List;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;



/*package*/ class StepSerialCopier implements IStepCopier<StepSerial> {

  @Override
  public StepSerial copyStep(StepSerial sourceSerial, ScopeStep parentScope, GenerationBase creator, CopyData cpyData) {
    StepSerial targetSerial = new StepSerial(parentScope, creator);
    fillStepSerial(sourceSerial, targetSerial, parentScope, cpyData);

    cpyData.getTargetStepMap().addStep(targetSerial);
    copyVars(sourceSerial, targetSerial, cpyData);

    return targetSerial;
  }


  public void fillStepSerial(StepSerial sourceSerial, StepSerial targetSerial, ScopeStep parentScope, CopyData cpyData) {
    //we need variables first!
    StepSerial sourceStepSerial = sourceSerial;
    StepSerial targetStepSerial = targetSerial;
    List<AVariable> vars = sourceStepSerial.getVariablesAndExceptions();
    WF parentWFObject = (parentScope instanceof WFStep) ? ((WFStep) parentScope).getWF() : parentScope.getParentWFObject();

    for (AVariable var : vars) {
      AVariable copy = StepCopier.copyVariable(var, parentWFObject, cpyData);
      targetStepSerial.addVar(copy);
    }

    for (int i = 0; i < sourceSerial.getChildSteps().size(); i++) {
      Step sourceChild = sourceSerial.getChildSteps().get(i);
      Step targetChild = StepCopier.copyStep(sourceChild, targetSerial.getParentScope(), cpyData);
      targetSerial.addChild(i, targetChild);
    }
  }


  private void copyVars(StepSerial sourceStep, StepSerial targetStep, CopyData cpyData) {
    StepSerial sourceStepSerial = (StepSerial) sourceStep;
    StepSerial targetStepSerial = (StepSerial) targetStep;
    List<AVariable> vars = sourceStepSerial.getVariablesAndExceptions();
    for (AVariable var : vars) {
      AVariable copy = StepCopier.copyVariable(var, targetStep.getParentWFObject(), cpyData);
      targetStepSerial.addVar(copy);
    }
  }

}
