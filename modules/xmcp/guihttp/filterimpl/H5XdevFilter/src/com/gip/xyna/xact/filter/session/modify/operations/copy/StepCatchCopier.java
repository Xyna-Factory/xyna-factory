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

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.util.DirectVarIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.UseAVariable;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.BranchInfo;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.CaseInfo;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;



/*package*/ class StepCatchCopier implements IStepCopier<StepCatch> {


  private static StepSerialCopier stepSerialCopier = new StepSerialCopier();


  @Override
  public StepCatch copyStep(StepCatch sourceStep, ScopeStep parentScope, GenerationBase creator, CopyData cpyData) {

    Step cpyOfStepInTryBlock = StepCopier.copyStep(sourceStep.getStepInTryBlock(), parentScope, cpyData);

    StepCatch result = new StepCatch(parentScope, cpyOfStepInTryBlock, creator);
    result.createEmpty();

    if (cpyOfStepInTryBlock instanceof StepFunction) {
      ((StepFunction) cpyOfStepInTryBlock).setCatchStep(result);
    }
    cpyData.getTargetStepMap().addStep(result);
    copyBranchesOfStepCatch(sourceStep, result, cpyData);

    return result;
  }


  private void copyBranchesOfStepCatch(StepCatch source, StepCatch target, CopyData cpyData) {
    //initialize exceptionVariableIdsToCase
    target.getBranchesForGUI();

    List<BranchInfo> sourceBranchInfos = source.getBranchesForGUI();
    if (sourceBranchInfos == null) {
      return;
    }

    int numberOfInputsExecutedStep = source.getStepInTryBlock().getInputVarIds().length;
    String orgBaseId = ObjectId.createStepId(source.getStepInTryBlock()).getBaseId();
    String cpyBaseId = ObjectId.createStepId(target.getStepInTryBlock()).getBaseId();

    StepSerial targetGlobalStepSerial = target.getParentWFObject().getWfAsStep().getChildStep();
    WF targetWF = targetGlobalStepSerial.getParentWFObject();

    for (int i = 0; i < sourceBranchInfos.size(); i++) {
      BranchInfo sourceBranchInfo = sourceBranchInfos.get(i);
      CaseInfo mainCase = sourceBranchInfo.getMainCase();
      String fqn = mainCase.getComplexName();
      ExceptionVariable exceptionToCatch = getorCreateTargetExceptionVarFromFqn(fqn, source, targetWF, cpyData);
      AVariable orgExceptionVar = getSourceExceptionVarFromFqn(fqn, source, cpyData);

      //add variable pair to map
      cpyData.getVariableCopies().put(orgExceptionVar, exceptionToCatch);
      DirectVarIdentification orgVarIdent = DirectVarIdentification.of(orgExceptionVar);
      DirectVarIdentification cpyVarIdent = DirectVarIdentification.of(exceptionToCatch);

      final String orgGuiId = ObjectId.createVariableId(orgBaseId, VarUsageType.input, i + numberOfInputsExecutedStep);
      orgVarIdent.internalGuiId = () -> orgGuiId;
      orgVarIdent.idprovider = new UseAVariable(orgVarIdent);

      final String cpyGuiId = ObjectId.createVariableId(cpyBaseId, VarUsageType.input, i + numberOfInputsExecutedStep);
      cpyVarIdent.internalGuiId = () -> cpyGuiId;
      cpyVarIdent.idprovider = new UseAVariable(cpyVarIdent);


      cpyData.getVariableIdentCopies().put(orgVarIdent, cpyVarIdent);
      target.addCaughtException(exceptionToCatch);
      targetGlobalStepSerial.removeVar(exceptionToCatch); //was added another time by addCoughtException
      copyBranches(sourceBranchInfo, fqn, target, cpyData);
    }
  }
  
  


  private void copyBranches(BranchInfo sourceBranchInfo, String fqn, StepCatch target, CopyData cpyData) {
    List<BranchInfo> targetBranchInfos = target.getBranchesForGUI();
    for (BranchInfo targetBranchInfo : targetBranchInfos) {
      if (fqn.equals(targetBranchInfo.getMainCase().getComplexName())) {
        Step sourceExecutedStep = sourceBranchInfo.getMainStep();
        Step targetExecutedStep = targetBranchInfo.getMainStep();
        targetExecutedStep.getChildSteps().clear(); //remove assign
        cpyData.getTargetStepMap().addStep(targetExecutedStep);
        StepSerial sourceSerial = (StepSerial) sourceExecutedStep;
        StepSerial targetSerial = (StepSerial) targetExecutedStep;
        stepSerialCopier.fillStepSerial(sourceSerial, targetSerial, target.getParentScope(), cpyData);
        break;
      }
    }
  }


  private AVariable getSourceExceptionVarFromFqn(String fqn, StepCatch step, CopyData cpyData) {
    AVariable sourceVar = null;
    for (AVariable candidate : step.getCaughtExceptionVars()) {
      String sourceFqn = candidate.getOriginalPath() + "." + candidate.getOriginalName();
      if (sourceFqn.equals(fqn)) {
        sourceVar = candidate;
        break;
      }
    }
    if (sourceVar == null) {
      throw new RuntimeException("Could not determine original exception. Fqn:" + fqn);
    }

    return sourceVar;
  }


  private ExceptionVariable getorCreateTargetExceptionVarFromFqn(String fqn, StepCatch step, WF targetWF, CopyData cpyData) {
    Map<AVariable, AVariable> varMap = cpyData.getVariableCopies();
    AVariable sourceVar = getSourceExceptionVarFromFqn(fqn, step, cpyData);
    ExceptionVariable result = (ExceptionVariable) varMap.get(sourceVar);
    if (result == null) {
      result = (ExceptionVariable) StepCopier.copyVariable(sourceVar, targetWF, cpyData);
    }
    return result;
  }
}
