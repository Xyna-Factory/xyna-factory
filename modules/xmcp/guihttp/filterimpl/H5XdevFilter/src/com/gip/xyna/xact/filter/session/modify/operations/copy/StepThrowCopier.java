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

package com.gip.xyna.xact.filter.session.modify.operations.copy;



import java.util.Map;

import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.Dataflow.InputConnection;
import com.gip.xyna.xact.filter.session.Dataflow.SimpleConnection;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;



/*package*/ class StepThrowCopier implements IStepCopier<StepThrow> {

  @Override
  public StepThrow copyStep(StepThrow sourceStep, ScopeStep parentScope, GenerationBase creator, CopyData cpyData) {
    StepThrow result = new StepThrow(parentScope, creator);
    String fqn = sourceStep.getPrefedExceptionType();

    if (fqn == null) {
      fqn = sourceStep.getExceptionTypeFqn();
    }

    if (fqn != null && fqn.equals("java.lang.Exception")) {
      fqn = "core.exception.Exception";
    }

    result.create(fqn, sourceStep.getLabel());

    cpyData.getTargetStepMap().addStep(result);
    copyVars(sourceStep, result, cpyData);

    return result;
  }


  private void copyVars(StepThrow sourceStep, StepThrow targetStep, CopyData cpyData) {
    StepSerial targetGlobalStepSerial = targetStep.getParentWFObject().getWfAsStep().getChildStep();
    Dataflow sourceDataflow = cpyData.getSourceDataflow();
    Dataflow targetDataflow = cpyData.getTargetDataflow();
    Map<AVariableIdentification, AVariableIdentification> cloneMap = cpyData.getVariableIdentCopies();

    // nothing to be done
    AVariableIdentification from = sourceDataflow.identifyVariables(sourceStep).getVariable(VarUsageType.input, 0);
    AVariableIdentification to = targetDataflow.identifyVariables(targetStep).getVariable(VarUsageType.input, 0);


    InputConnection copyCon = sourceDataflow.createCopyOfConnection(from, to, cloneMap, targetGlobalStepSerial);
    targetDataflow.getInputConnections().put(to, copyCon);

    SimpleConnection svc = copyCon.getConnectionForLane(0);
    if (svc.getInputVars().size() == 1) {
      targetStep.setExceptionID(svc.getInputVar(0).getIdentifiedVariable().getId());
    }

  }

}
