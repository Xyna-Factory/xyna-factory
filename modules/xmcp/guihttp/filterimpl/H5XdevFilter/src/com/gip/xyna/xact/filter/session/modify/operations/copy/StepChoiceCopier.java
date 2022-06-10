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



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.StepMap;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepChoice;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.UseAVariable;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.GlobalChoiceVarIdentification;
import com.gip.xyna.xact.filter.util.ReferencedVarIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.BranchInfo;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.CaseInfo;



public class StepChoiceCopier implements IStepCopier<StepChoice> {

  private static StepSerialCopier stepSerialCopier = new StepSerialCopier();


  @Override
  public StepChoice copyStep(StepChoice sourceStep, ScopeStep parentScope, GenerationBase creator, CopyData cpyData) {
    StepMap targetStepMap = cpyData.getTargetStepMap();
    StepChoice targetStep = new StepChoice(parentScope, creator);

    try {
      targetStep.create(sourceStep.getOuterCondition(), sourceStep.getDistinctionType());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    targetStep.replaceOuterCondition(sourceStep.getOuterCondition());

    addInputVars(sourceStep, targetStep, cpyData);
    targetStepMap.addStep(targetStep);
    addInputConnections(sourceStep, targetStep, cpyData);
    copyOutputVars(sourceStep, targetStep, cpyData);
    addOutputInIdentifiedVariables(sourceStep, targetStep, cpyData);
    copyChildren(sourceStep, targetStep, cpyData);

    return targetStep;
  }


  private void addOutputInIdentifiedVariables(StepChoice sourceStep, StepChoice targetStep, CopyData cpyData) {
    Dataflow sourceDataflow = cpyData.getSourceDataflow();
    Dataflow targetDataflow = cpyData.getTargetDataflow();
    Map<AVariable, AVariable> varMap = cpyData.getVariableCopies();
    Map<AVariableIdentification, AVariableIdentification> identMap = cpyData.getVariableIdentCopies();
    List<AVariableIdentification> sourceOutput = sourceDataflow.identifyVariables(sourceStep).getListAdapter(VarUsageType.output);
    List<AVariableIdentification> targetOutput = new ArrayList<AVariableIdentification>();

    for (AVariableIdentification sourceOutputVarIdent : sourceOutput) {
      AVariable copyAvar = varMap.get(sourceOutputVarIdent.getIdentifiedVariable());
      AVariableIdentification cpyIdent = GlobalChoiceVarIdentification.of(copyAvar, targetStep);
      cpyIdent.idprovider = new UseAVariable(cpyIdent);
      targetOutput.add(cpyIdent);
      identMap.put(sourceOutputVarIdent, cpyIdent);
    }
    ((IdentifiedVariablesStepChoice) targetDataflow.identifyVariables(targetStep)).setOutputVarIdentification(targetOutput);
  }


  private void copyOutputVars(StepChoice sourceStep, StepChoice targetStep, CopyData cpyData) {
    List<AVariable> orgOutputVars = sourceStep.getOutputVars();
    List<AVariable> cpyOutputVars = new ArrayList<AVariable>();
    WF parentWFObject = targetStep.getParentWFObject();
    for (AVariable orgOutputVar : orgOutputVars) {
      AVariable cpyOutputVar = StepCopier.copyVariable(orgOutputVar, parentWFObject, cpyData);
      cpyOutputVars.add(cpyOutputVar);
    }
    targetStep.setCalculatedOutput(cpyOutputVars);
  }


  private void copyChildren(StepChoice sourceStep, StepChoice targetStep, CopyData cpyData) {
    switch (sourceStep.getDistinctionType()) {
      case ConditionalChoice :
        copyChildrenChoice(sourceStep, targetStep, cpyData);
        break;
      case ConditionalBranch :
        copyChildrenBranch(sourceStep, targetStep, cpyData);
        break;
      case TypeChoice :
        copyChildrenType(sourceStep, targetStep, cpyData);
        break;
      default : //Exception Handling => not part of StepChoice
        break;
    }
  }


  private void copyChildrenType(StepChoice source, StepChoice target, CopyData cpyData) {
    copyCreatedVariables(source, target, cpyData);
    fillBranches(source, target, cpyData);
    removeExtraBranches(source, target);
  }


  private void removeExtraBranches(StepChoice source, StepChoice target) {
    List<BranchInfo> sourceBranchInfos = source.getBranchesForGUI();
    List<BranchInfo> targetBranchInfos = target.getBranchesForGUI();
    Iterator<BranchInfo> it = targetBranchInfos.iterator();
    int deleteIndex = 0;
    while (it.hasNext()) {
      BranchInfo targetBranchInfo = it.next();
      boolean found = false;
      for (BranchInfo sourceBranchInfo : sourceBranchInfos) {
        if (targetBranchInfo.getMainCase().getComplexName().equals(sourceBranchInfo.getMainCase().getComplexName())) {
          found = true;
          break;
        }
      }
      if (!found) {
        it.remove();
        redistributeCases(source, target, targetBranchInfo);
        target.removeBranch(deleteIndex);
      } else {
        deleteIndex++;
      }
    }
  }

  
  private void redistributeCases(StepChoice source, StepChoice target, BranchInfo rmvBranch) {
    List<CaseInfo> cases = rmvBranch.getCases();
    
    for(CaseInfo caseInfo: cases) {
      String complexName = caseInfo.getComplexName();
      String targetAlias = findTargetAlias(source, complexName);
      caseInfo.setAlias(targetAlias);
    }
    
  }

  
  private String findTargetAlias(StepChoice source, String complexName) {
    List<BranchInfo> branches = source.getBranchesForGUI();
    
    for(BranchInfo branch : branches) {
      List<CaseInfo> cases = branch.getCases();
      for(CaseInfo caseInfo : cases) {
        if(caseInfo.getComplexName().equals(complexName)) {
          return caseInfo.getAlias();
        }
      }
    }
    
    //case should be removed
    return null;
  }


  private void fillBranches(StepChoice source, StepChoice target, CopyData cpyData) {
    List<BranchInfo> sourceBranchInfos = source.getBranchesForGUI();
    List<BranchInfo> targetBranchInfos = target.getBranchesForGUI();
    StepMap stepMap = cpyData.getTargetStepMap();
    for (BranchInfo sourceBranchInfo : sourceBranchInfos) {
      CaseInfo mainCase = sourceBranchInfo.getMainCase();
      boolean found = false;
      int targetBranchIndex = 0;
      for (BranchInfo targetBranchInfo : targetBranchInfos) {
        if (targetBranchInfo.getMainCase().getComplexName().equals(mainCase.getComplexName())) {
          found = true;
          break;
        }
        targetBranchIndex++;
      }
      if (found) {
        Step sourceExecutedStep = sourceBranchInfo.getMainStep();
        Step targetExecutedStep = targetBranchInfos.get(targetBranchIndex).getMainStep();
        stepMap.addStep(targetExecutedStep);
        targetExecutedStep.getChildSteps().clear();

        stepSerialCopier.fillStepSerial((StepSerial) sourceExecutedStep, (StepSerial) targetExecutedStep, target.getParentScope(), cpyData);
      }
    }
  }


  private void copyCreatedVariables(StepChoice source, StepChoice target, CopyData cpyData) {
    Dataflow sourceDataflow = cpyData.getSourceDataflow();
    Dataflow targetDataflow = cpyData.getTargetDataflow();
    WF parentWFObject = target.getParentWFObject();
    Map<AVariableIdentification, AVariableIdentification> varIdentMap = cpyData.getVariableIdentCopies();

    IdentifiedVariablesStepChoice orgIdentVars = (IdentifiedVariablesStepChoice) sourceDataflow.identifyVariables(source);
    IdentifiedVariablesStepChoice cpyIdentVars = (IdentifiedVariablesStepChoice) targetDataflow.identifyVariables(target);

    Map<Integer, AVariableIdentification> orgCreatedVars = orgIdentVars.getCreatedVariables();
    Map<Integer, AVariableIdentification> cpyCreatedVars = cpyIdentVars.getCreatedVariables();

    for (Integer i : orgCreatedVars.keySet()) {
      AVariableIdentification orgVarIdent = orgCreatedVars.get(i);
      AVariable orgAVar = orgVarIdent.getIdentifiedVariable();
      AVariable cpyAVar = StepCopier.copyVariable(orgAVar, parentWFObject, cpyData);

      String orgGuiId = ObjectId.createVariableId(ObjectId.createStepId(source).getBaseId(), VarUsageType.input, i + 1);
      orgVarIdent.internalGuiId = () -> orgGuiId;

      AVariableIdentification cpyVarIdent = null;
      cpyVarIdent = ReferencedVarIdentification.of(cpyAVar);
      final String id = cpyAVar.getId();
      cpyVarIdent.idprovider = () -> id;
      String guiId = ObjectId.createVariableId(ObjectId.createStepId(target).getBaseId(), VarUsageType.input, i + 1);
      cpyVarIdent.internalGuiId = () -> guiId;

      cpyCreatedVars.put(i, cpyVarIdent);
      varIdentMap.put(orgVarIdent, cpyVarIdent);
    }
  }


  private void copyChildrenChoice(StepChoice sourceStep, StepChoice targetStep, CopyData cpyData) {
    int branchIndex = 0;

    //no alias allowed for conditional choice!
    Map<String, CaseInfo> cases = targetStep.getCaseInfos();
    for (CaseInfo caseInfo : cases.values()) {
      caseInfo.setAlias("");
    }

    List<BranchInfo> sourceBranchInfos = sourceStep.getBranchesForGUI();
    List<BranchInfo> targetBranchInfos = targetStep.getBranchesForGUI();
    for (BranchInfo sourceBranchInfo : sourceBranchInfos) {
      Step sourceExecutedStep = sourceBranchInfo.getMainStep();
      Step targetExecutedStep = targetBranchInfos.get(branchIndex).getMainStep();
      StepSerial targetStepSerial = (StepSerial) targetExecutedStep;
      targetStepSerial.getChildSteps().clear(); //remove existing steps (-> assigns)
      stepSerialCopier.fillStepSerial((StepSerial) sourceExecutedStep, targetStepSerial, targetStep.getParentScope(), cpyData);
      branchIndex++;
    }
  }


  private void copyChildrenBranch(StepChoice sourceStep, StepChoice targetStep, CopyData cpyData) {
    int branchIndex = 0;
    List<BranchInfo> sourceBranchInfos = sourceStep.getBranchesForGUI();
    List<BranchInfo> targetBranchInfos = targetStep.getBranchesForGUI();

    for (BranchInfo sourceBranchInfo : sourceBranchInfos) {
      List<CaseInfo> sourceCaseInfos = sourceBranchInfo.getCases();
  
      if (!isDefaultBranch(sourceBranchInfo)) {
        CaseInfo mainCase = sourceBranchInfo.getMainCase();
        targetStep.addBranch(branchIndex, mainCase.getComplexName(), mainCase.getName());
        targetBranchInfos = targetStep.getBranchesForGUI();
      }
      
      if (sourceCaseInfos.size() > 1) {
        int caseIndex = 0;
        for (CaseInfo caseInfo : sourceCaseInfos) {
          if (caseInfo.isMainCaseOfItsBranch()) {
            targetStep.getChildSteps().get(caseIndex).getChildSteps().clear();
          } else {
            targetStep.addCase(branchIndex, caseIndex, caseInfo.getComplexName(), caseInfo.getName());
          }
          caseIndex++;
        }
      }

      Step sourceExecutedStep = sourceBranchInfo.getMainStep();
      Step targetExecutedStep = targetBranchInfos.get(branchIndex).getMainStep();
      StepSerial targetStepSerial = (StepSerial) targetExecutedStep;
      targetStepSerial.getChildSteps().clear(); //remove existing steps (-> assigns)

      stepSerialCopier.fillStepSerial((StepSerial) sourceExecutedStep, targetStepSerial, targetStep.getParentScope(), cpyData);

      branchIndex++;
    }
  }
  
  
  private boolean isDefaultBranch(BranchInfo branchInfo) {
    List<CaseInfo> sourceCaseInfos = branchInfo.getCases();
    boolean isDefault = false;
    for (CaseInfo caseInfo : sourceCaseInfos) {
      if (caseInfo.isDefault()) {
        isDefault = true;
        break;
      }
    }
    return isDefault;
  }


  private void addInputConnections(StepChoice sourceStep, StepChoice targetStep, CopyData cpyData) {
    List<AVariable> orgInputVars = sourceStep.getInputVars();
    List<AVariable> cpyInputVars = targetStep.getInputVars();
    StepSerial targetGlobalStepSerial = targetStep.getParentWFObject().getWfAsStep().getChildStep();

    for (int i = 0; i < orgInputVars.size(); i++) {
      AVariable orgVar = orgInputVars.get(i);
      AVariable cpyVar = cpyInputVars.get(i);

      CopyDataflowConnectionData conCpyData = new CopyDataflowConnectionData();
      conCpyData.setDataflowToUpdate(cpyData.getTargetDataflow());
      conCpyData.setOriginalDataflow(cpyData.getSourceDataflow());
      conCpyData.setSourceVar(orgVar);
      conCpyData.setVariableToConnect(cpyVar);
      conCpyData.setToUpdateGlobalStepSerial(targetGlobalStepSerial);
      conCpyData.setVarCopies(cpyData.getVariableIdentCopies());

      StepCopier.copyConnection(conCpyData);
    }
  }


  private void addInputVars(StepChoice sourceStep, StepChoice targetStep, CopyData cpyData) {
    if (sourceStep.getDistinctionType() == DistinctionType.TypeChoice) {
      AVariable inputVar = sourceStep.getInputVars().get(0);
      AVariable copyVar = StepCopier.copyVariable(inputVar, targetStep.getParentWFObject(), cpyData);
      targetStep.setTypeChoiceVar(copyVar);
      return;
    }

    List<AVariable> inputVars = sourceStep.getInputVars();
    for (int i = 0; i < inputVars.size(); i++) {
      AVariable inputVar = inputVars.get(i);
      AVariable copyVar = StepCopier.copyVariable(inputVar, targetStep.getParentWFObject(), cpyData);
      targetStep.addInputVar(i, copyVar);
      targetStep.getInputConnections().addInputConnection(i);
      String orgVarId = sourceStep.getInputVarIds()[i];
      String cpyVarId = StepCopier.getCopyVarId(orgVarId, targetStep.getParentWFObject(), cpyData.getVariableIdMap());
      targetStep.getInputConnections().getVarIds()[i] = cpyVarId;
    }
  }
}
