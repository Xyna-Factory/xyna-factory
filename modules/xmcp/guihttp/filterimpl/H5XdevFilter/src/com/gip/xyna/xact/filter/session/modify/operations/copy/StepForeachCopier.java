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



import java.util.List;
import java.util.Map;

import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.Dataflow.InputConnection;
import com.gip.xyna.xact.filter.session.gb.StepMap;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;



/*package*/ class StepForeachCopier implements IStepCopier<StepForeach> {

  private static StepSerialCopier stepSerialCopier = new StepSerialCopier();


  @Override
  public StepForeach copyStep(StepForeach sourceStep, ScopeStep parentScope, GenerationBase creator, CopyData cpyData) {
    StepMap stepMap = cpyData.getTargetStepMap();
    StepForeach targetStep = new StepForeach(parentScope, creator);

    targetStep.createEmpty();
    copyInput(sourceStep, targetStep, cpyData);
    stepMap.addStep(targetStep);
    addVarIdentCopiesToMap(sourceStep, targetStep, cpyData);
    copyOutput(sourceStep, targetStep, cpyData);
    copyChildren(sourceStep, targetStep, cpyData);
    copySingleOutputVariables(sourceStep, targetStep, cpyData);
    copyVars(sourceStep, targetStep, cpyData);

    return targetStep;
  }


  //needs to happen after step was added to StepMap!
  private void addVarIdentCopiesToMap(StepForeach sourceStep, StepForeach targetStep, CopyData cpyData) {
    Map<AVariableIdentification, AVariableIdentification> varIdMap = cpyData.getVariableIdentCopies();
    Dataflow sourceDataflow = cpyData.getSourceDataflow();
    Dataflow targetDataflow = cpyData.getTargetDataflow();

    List<AVariableIdentification> orgInputs = sourceDataflow.identifyVariables(sourceStep).getListAdapter(VarUsageType.input);
    List<AVariableIdentification> cpyInputs = targetDataflow.identifyVariables(targetStep).getListAdapter(VarUsageType.input);

    for (int i = 0; i < orgInputs.size(); i++) {
      AVariableIdentification org = orgInputs.get(i);
      AVariableIdentification copy = cpyInputs.get(i);
      varIdMap.put(org, copy);
    }
  }


  private void copyChildren(StepForeach sourceStep, StepForeach targetStep, CopyData cpyData) {
    StepSerial sourceSerial = sourceStep.getChildScope().getChildStep();
    StepSerial targetSerial = targetStep.getChildScope().getChildStep();
    ScopeStep newParentScope = targetStep.getChildScope();
    stepSerialCopier.fillStepSerial(sourceSerial, targetSerial, newParentScope, cpyData);
  }


  private void copyInput(StepForeach sourceStep, StepForeach targetStep, CopyData cpyData) {
    Map<String, String> idMap = cpyData.getVariableIdMap();
    Map<AVariable, AVariable> varMap = cpyData.getVariableCopies();
    String[] inputVarIds = sourceStep.getInputListRefs();

    for (int i = 0; i < inputVarIds.length; i++) {
      String oldId = inputVarIds[i];
      String newId = idMap.get(oldId);
      AVariable copyVar;

      //input is not part of copy, create placeholder
      if (newId == null) {
        newId = "" + targetStep.getParentWFObject().getNextXmlId();
        idMap.put(oldId, newId);
        AVariable orgSingleVar = sourceStep.getInputVarsSingle()[i];
        copyVar = StepCopier.copyVariable(orgSingleVar, targetStep.getParentWFObject(), cpyData);
        targetStep.addInput(copyVar);
        continue;
      }

      AVariable oldListVar = getOldListVar(sourceStep, oldId);
      copyVar = varMap.get(oldListVar);
      targetStep.addInput(copyVar);
      AVariable orgSingleVar = sourceStep.getInputVarsSingle()[i];
      AVariable copySingleVar = targetStep.getInputVarsSingle()[i];
      idMap.put(orgSingleVar.getId(), copySingleVar.getId());
    }
  }


  private AVariable getOldListVar(StepForeach sourceStep, String id) {
    try {
      return sourceStep.getChildScope().identifyVariable(id).getVariable();
    } catch (XPRC_InvalidVariableIdException e) {
      return null;
    }
  }


  private void copyOutput(StepForeach sourceStep, StepForeach targetStep, CopyData cpyData) {
    WF targetWF = targetStep.getParentWFObject();
    Map<String, String> idMap = cpyData.getVariableIdMap();
    String[] oldOutputListRefs = sourceStep.getOutputListRefs();
    String[] newOutputListRefs = new String[oldOutputListRefs.length];

    for (int i = 0; i < oldOutputListRefs.length; i++) {
      String oldOutputListVarId = oldOutputListRefs[i];
      String newOutputListVarId = StepCopier.getCopyVarId(oldOutputListVarId, targetWF, idMap);
      newOutputListRefs[i] = newOutputListVarId;
    }
    targetStep.setOutputListRefs(newOutputListRefs);
  }


  private void copySingleOutputVariables(StepForeach sourceStep, StepForeach targetStep, CopyData cpyData) {
    WF targetWF = targetStep.getParentWFObject();

    //copy single output variables
    //requires childSteps to create the variables first
    AVariable[] oldOutputVarsSingle = sourceStep.getOutputVarsSingle();
    AVariable[] newOutputVarsSingle = new AVariable[oldOutputVarsSingle.length];

    for (int i = 0; i < oldOutputVarsSingle.length; i++) {
      AVariable oldOutput = oldOutputVarsSingle[i];
      AVariable newOutput = StepCopier.copyVariable(oldOutput, targetWF, cpyData);

      newOutputVarsSingle[i] = newOutput;
    }

    targetStep.setOutputVarsSingle(newOutputVarsSingle);
  }


  private void copyVars(StepForeach sourceStep, StepForeach targetStep, CopyData cpyData) {
    StepSerial globalStepSerial = targetStep.getParentWFObject().getWfAsStep().getChildStep();
    Dataflow sourceDataflow = cpyData.getSourceDataflow();
    Dataflow targetDataflow = cpyData.getTargetDataflow();
    Map<AVariableIdentification, AVariableIdentification> variableCloneMap = cpyData.getVariableIdentCopies();

    //copy connection from list input to ForEach
    List<AVariableIdentification> fromVarIdents = sourceDataflow.identifyVariables(sourceStep).getVariables(VarUsageType.input);
    List<AVariableIdentification> toVarIdents = targetDataflow.identifyVariables(targetStep).getVariables(VarUsageType.input);

    for (int i = 0; i < fromVarIdents.size(); i++) {
      AVariableIdentification from = fromVarIdents.get(i);
      AVariableIdentification to = toVarIdents.get(i);

      InputConnection copyCon = sourceDataflow.createCopyOfConnection(from, to, variableCloneMap, globalStepSerial);
      targetDataflow.getInputConnections().put(to, copyCon);
    }
  }

}
