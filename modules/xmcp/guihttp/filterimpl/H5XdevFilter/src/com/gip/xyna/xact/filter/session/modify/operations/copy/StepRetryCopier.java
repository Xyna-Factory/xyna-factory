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
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;



/*package*/ class StepRetryCopier implements IStepCopier<StepRetry> {

  @Override
  public StepRetry copyStep(StepRetry sourceStep, ScopeStep parentScope, GenerationBase creator, CopyData cpyData) {
    StepRetry result = new StepRetry(parentScope, creator);
    result.create(sourceStep.getLabel(), sourceStep.getDocumentation());

    cpyData.getTargetStepMap().addStep(result);
    copyVars(sourceStep, result, cpyData);

    return result;
  }


  private void copyVars(StepRetry sourceStep, StepRetry targetStep, CopyData cpyData) {
    StepSerial targetGlobalStepSerial = targetStep.getParentWFObject().getWfAsStep().getChildStep();
    Dataflow sourceDataflow = cpyData.getSourceDataflow();
    Dataflow targetDataflow = cpyData.getTargetDataflow();
    Map<AVariableIdentification, AVariableIdentification> cloneMap = cpyData.getVariableIdentCopies();

    //copy variable
    AVariableIdentification from = sourceDataflow.identifyVariables(sourceStep).getListAdapter(VarUsageType.input).get(0);
    AVariableIdentification to = targetDataflow.identifyVariables(targetStep).getListAdapter(VarUsageType.input).get(0);

    InputConnection connection = sourceDataflow.createCopyOfConnection(from, to, cloneMap, targetGlobalStepSerial);
    targetDataflow.getInputConnections().put(to, connection);
  }

}
