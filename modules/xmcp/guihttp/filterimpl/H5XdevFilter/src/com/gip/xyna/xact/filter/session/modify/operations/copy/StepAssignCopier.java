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

package com.gip.xyna.xact.filter.session.modify.operations.copy;



import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.Dataflow.InputConnection;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepChoice;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;



/*package*/ class StepAssignCopier implements IStepCopier<StepAssign> {

  @Override
  public StepAssign copyStep(StepAssign sourceStep, ScopeStep parentScope, GenerationBase creator, CopyData cpyData) {
    StepAssign targetStep = new StepAssign(parentScope, creator);
    Map<String, String> idMap = cpyData.getVariableIdMap();
    InputConnections input = new InputConnections(sourceStep.getInputConnections().length());
    String[] outputVarIds = new String[sourceStep.getOutputVarIds().length];

    for (int i = 0; i < input.length(); i++) {
      String inputId = idMap.get(sourceStep.getInputVarIds()[i]);
      input.getVarIds()[i] = inputId;
    }

    for (int i = 0; i < outputVarIds.length; i++) {
      String outputId = idMap.get(sourceStep.getOutputVarIds()[i]);
      outputVarIds[i] = outputId;
    }

    targetStep.replaceVars(input, outputVarIds);

    copyConnections(sourceStep, targetStep, cpyData);

    cpyData.getTargetStepMap().addStep(targetStep);
    
    return targetStep;
  }


  private void copyConnections(StepAssign sourceStep, StepAssign targetStep, CopyData cpyData) {

    Step parentAboveSerial = sourceStep.getParentStep().getParentStep();
    Step provider = null;
    if (parentAboveSerial instanceof StepChoice) {
      if (firstTypeChoiceAssign(sourceStep)) {
        copyTypeChoiceConnection(sourceStep, targetStep, cpyData);
        return;
      }
      provider = parentAboveSerial;
    } else if (parentAboveSerial.getChildSteps().get(0) instanceof StepFunction) {
      provider = parentAboveSerial.getChildSteps().get(0);
    } else {
      return;
    }

    copyConnectionsFromProvider(sourceStep, targetStep, provider, cpyData);
  }


  private void copyTypeChoiceConnection(StepAssign sourceStep, StepAssign targetStep, CopyData cpyData) {

    if (sourceStep.getOutputVarIds().length == 0) {
      return; //not connected
    }

    Dataflow sourceDataflow = cpyData.getSourceDataflow();
    AVariableIdentification orgVarIdent = null;
    AVariable orgAVar = sourceStep.getOutputVars().get(0);

    IdentifiedVariables orgIdentVars = sourceDataflow.identifyVariables(sourceStep.getParentStep().getParentStep());
    IdentifiedVariablesStepChoice orgVarIdentChoice = (IdentifiedVariablesStepChoice) orgIdentVars;

    Collection<AVariableIdentification> orgCreatedVaraibles = orgVarIdentChoice.getCreatedVariables().values();
    for (AVariableIdentification candidate : orgCreatedVaraibles) {
      if (candidate.getIdentifiedVariable().equals(orgAVar)) {
        orgVarIdent = candidate;
        break;
      }
    }

    copySingleConnection(orgVarIdent, targetStep, cpyData);
  }


  private boolean firstTypeChoiceAssign(StepAssign sourceStep) {
    StepSerial parentSerial = (StepSerial) sourceStep.getParentStep();
    StepChoice parentChoice = (StepChoice) parentSerial.getParentStep();

    if (parentChoice.getDistinctionType() != DistinctionType.TypeChoice) {
      return false;
    }
    return parentSerial.getChildSteps().indexOf(sourceStep) == 0;
  }


  private void copyConnectionsFromProvider(StepAssign sourceStep, StepAssign targetStep, Step provider, CopyData cpyData) {
    Dataflow sourceDataflow = cpyData.getSourceDataflow();

    List<AVariableIdentification> sourceInputs = sourceDataflow.identifyVariables(provider).getListAdapter(VarUsageType.output);
    for (int i = 0; i < sourceInputs.size(); i++) {
      AVariableIdentification orgVarIdent = sourceInputs.get(i);
      copySingleConnection(orgVarIdent, targetStep, cpyData);
    }
  }


  private void copySingleConnection(AVariableIdentification orgVarIdent, StepAssign targetStep, CopyData cpyData) {
    Dataflow sourceDataflow = cpyData.getSourceDataflow();
    Map<AVariableIdentification, AVariableIdentification> varMap = cpyData.getVariableIdentCopies();

    Dataflow targetDataflow = cpyData.getTargetDataflow();
    StepSerial targetGlobalStepSerial = targetStep.getParentWFObject().getWfAsStep().getChildStep();

    AVariableIdentification copyVar = varMap.get(orgVarIdent);
    InputConnection connection = sourceDataflow.createCopyOfConnection(orgVarIdent, copyVar, varMap, targetGlobalStepSerial);
    targetDataflow.getInputConnections().put(copyVar, connection);
  }
}
