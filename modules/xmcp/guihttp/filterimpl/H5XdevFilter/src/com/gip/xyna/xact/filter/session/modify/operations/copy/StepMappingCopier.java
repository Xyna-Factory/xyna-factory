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

import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;



/*package*/ class StepMappingCopier implements IStepCopier<StepMapping> {

  public StepMapping copyStep(StepMapping source, ScopeStep targetParentScope, GenerationBase creator, CopyData cpyData) {
    StepMapping target = new StepMapping(targetParentScope, creator);

    if (!source.isTemplateMapping()) {
      target.createEmpty(source.getLabel());
      target.setIsConditionMapping(source.isConditionMapping());
    } else {
      try {
        target.createTemplate();
      } catch (XPRC_InvalidPackageNameException e) {
        throw new RuntimeException(e);
      }
    }

    if (target.isTemplateMapping() && target.getFormulaCount() == 1) {
      target.removeFormula(0); // remove initial formula
    }

    for (int i = 0; i < source.getFormulaCount(); i++) {
      target.addFormula(i, source.getFormula(i));
    }

    target.setDocumentation(source.getDocumentation());

    cpyData.getTargetStepMap().addStep(target);
    copyVars(source, target, cpyData);
    
    return target;
  }

  private void copyVars(StepMapping sourceStep, StepMapping targetStep, CopyData cpyData) {
    copyInputVariables(sourceStep, targetStep, cpyData);
    copyOutputVariables(sourceStep, targetStep, cpyData);
    StepCopier.copyVarIdentification(cpyData, sourceStep, targetStep);
  }


  private void copyOutputVariables(StepMapping sourceStep, StepMapping targetStep, CopyData cpyData) {
    Map<String, String> idMap = cpyData.getVariableIdMap();
    
    //remove existing output (-> template)
    targetStep.getOutputVars().clear();
    for (int i = 0; i < targetStep.getOutputVarIds().length; i++) {
      targetStep.removeOutputVarId(0);
    }

    for (int i = 0; i < sourceStep.getOutputVarIds().length; i++) {
      //copy variable in step
      AVariable sourceVar = sourceStep.getOutputVars().get(i);
      AVariable copy = StepCopier.copyVariable(sourceVar, targetStep.getParentWFObject(), cpyData);
      targetStep.getOutputVars().add(copy);

      //copy variableId -> creates Target RefId
      String copyId = StepCopier.getCopyVarId(sourceStep.getOutputVarIds()[i], targetStep.getParentWFObject(), idMap);
      targetStep.addOutputVarId(i, copyId);
    }
  }


  private void copyInputVariables(StepMapping sourceStep, StepMapping targetStep, CopyData cpyData) {
    Map<String, String> idMap = cpyData.getVariableIdMap();
    StepSerial targetGlobalStepSerial = targetStep.getParentWFObject().getWfAsStep().getChildStep();
    InputConnections sourceInputConnections = sourceStep.getInputConnections();
    InputConnections targetInputConnections = targetStep.getInputConnections();

    for (int i = 0; i < sourceInputConnections.length(); i++) {
      //copy id
      String sourceId = sourceInputConnections.getVarIds()[i];
      boolean isUserConnected = sourceInputConnections.isUserConnected(sourceId);
      boolean isConstant = sourceInputConnections.isConstantConnected(sourceId);
      String varId = StepCopier.getCopyVarId(sourceId, targetStep.getParentWFObject(), idMap);
      targetInputConnections.addInputConnection(i, varId, null, isUserConnected, isConstant, null);

      //copy variable
      AVariable sourceVar = sourceStep.getInputVars().get(i);
      AVariable copyVar = StepCopier.copyVariable(sourceVar, targetStep.getParentWFObject(), cpyData);
      targetStep.getInputVars().add(copyVar);

      //copy DataFlow connection
      CopyDataflowConnectionData conCpyData = new CopyDataflowConnectionData();
      conCpyData.setDataflowToUpdate(cpyData.getTargetDataflow());
      conCpyData.setOriginalDataflow(cpyData.getSourceDataflow());
      conCpyData.setToUpdateGlobalStepSerial(targetGlobalStepSerial);
      conCpyData.setVariableToConnect(copyVar);
      conCpyData.setSourceVar(sourceVar);
      conCpyData.setVarCopies(cpyData.getVariableIdentCopies());

      StepCopier.copyConnection(conCpyData);
    }
  }
}
