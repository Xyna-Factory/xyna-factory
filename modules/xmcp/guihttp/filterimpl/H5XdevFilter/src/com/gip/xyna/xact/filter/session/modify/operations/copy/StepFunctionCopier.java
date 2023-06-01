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
import java.util.Optional;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.Dataflow.InputConnection;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.modify.operations.ModifyOperationBase;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.Service;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;



/*package*/ class StepFunctionCopier implements IStepCopier<StepFunction> {

  private static StepSerialCopier stepSerialCopier = new StepSerialCopier();

  @Override
  public StepFunction copyStep(StepFunction source, ScopeStep parentScope, GenerationBase creator, CopyData cpyData) {
    StepFunction target = null;
    Service service = source.getService();
    GenerationBase implGb = service.getDom() == null ? service.getWF() : service.getDom();

    target = ModifyOperationBase.createStepFunction(parentScope, source.getOperationName(), implGb);
    if (service.isPrototype()) {
      setPrototypeVariables(service, target.getService(), target.getParentWFObject(), cpyData);
      //add input connections - they are filled later
      for (int i = 0; i < source.getInputConnections().length(); i++) {
        target.getInputConnections().addInputConnection(0);
      }
    }

    target.setDocumentation(source.getDocumentation());
    target.setLabel(source.getLabel());
    target.setQueryFilterConditions(source.getQueryFilterConditions());

    cpyData.getTargetStepMap().addStep(target);
    copyVars(source, target, cpyData);

    copyCompensation(source, target, cpyData);

    return target;
  }


  private void copyCompensation(StepFunction source, StepFunction target, CopyData cpyData) {
    Step sourceCompensationStep = source.getCompensateStep();
    if (sourceCompensationStep == null) {
      return;
    }

    target.setOverrideCompensation(true);

    StepSerial sourceComSerial = (StepSerial) source.getCompensateStep();
    StepSerial targetComSerial = (StepSerial) target.getCompensateStep();

    stepSerialCopier.fillStepSerial(sourceComSerial, targetComSerial, target.getParentScope(), cpyData);
  }


  private void setPrototypeVariables(Service from, Service to, WF parentWF, CopyData cpyData) {
    List<AVariable> serviceInputs = to.getInputVars();

    for (AVariable input : from.getInputVars()) {
      AVariable cpy = StepCopier.copyVariable(input, parentWF, cpyData);
      serviceInputs.add(cpy);
    }

    List<AVariable> serviceOutputs = to.getOutputVars();
    for (AVariable output : from.getOutputVars()) {
      if (output == null) {
        continue;
      }
      AVariable cpy = StepCopier.copyVariable(output, parentWF, cpyData);
      serviceOutputs.add(cpy);
    }
  }


  //TODO: remote destination
  private void copyVars(StepFunction sourceStep, StepFunction targetStep, CopyData cpyData) {

    copyInputVariables(sourceStep, targetStep, cpyData);

    copyOutputVariables(sourceStep, targetStep, cpyData);
    
    //has to happen after copy of Output Variables!
    copyInputConnections(sourceStep, targetStep, cpyData);

    StepCopier.copyOutputVarIdents(sourceStep, targetStep, cpyData);
  }


  private void copyInputConnections(StepFunction sourceStep, StepFunction targetStep, CopyData cpyData) {
    InputConnections sourceInputConnections = sourceStep.getInputConnections();

    for (int i = 0; i < sourceInputConnections.length(); i++) {
      String sourceId = sourceInputConnections.getVarIds()[i];
      //determine connected variable (stepFunction does not create Variables for Inputs!)
      Optional<AVariable> sourceVar = tryGetVariable(sourceStep, sourceId);
      if (sourceVar.isEmpty()) {
        //input not connected or connected to variable outside copy scope. nothing to be done.
        continue;
      }

      copyConnection(sourceStep, targetStep, i, cpyData);
    }
  }


  private void copyInputVariables(StepFunction sourceStep, StepFunction targetStep, CopyData cpyData) {
    Map<String, String> idMap = cpyData.getVariableIdMap();
    InputConnections sourceInputConnections = sourceStep.getInputConnections();
    InputConnections targetInputConnections = targetStep.getInputConnections();

    for (int i = 0; i < sourceInputConnections.length(); i++) {
      //copy id
      String sourceId = sourceInputConnections.getVarIds()[i];
      boolean isUserConnected = sourceInputConnections.isUserConnected(sourceId);
      boolean isConstant = sourceInputConnections.isConstantConnected(sourceId);
      String varId = StepCopier.getCopyVarId(sourceId, targetStep.getParentWFObject(), idMap);
      String expectedType = sourceInputConnections.getExpectedTypes()[i];
      targetInputConnections.getVarIds()[i] = varId;
      targetInputConnections.getPaths()[i] = null;
      targetInputConnections.getUserConnected()[i] = isUserConnected;
      targetInputConnections.getConstantConnected()[i] = isConstant;
      targetInputConnections.getExpectedTypes()[i] = expectedType;
    }
  }


  private Optional<AVariable> tryGetVariable(StepFunction sourceStep, String varId) {
    try {
      VariableIdentification ident = sourceStep.getParentScope().identifyVariable(varId);
      AVariable variable = ident.getVariable();
      return Optional.of(variable);
    } catch (XPRC_InvalidVariableIdException e) {
      return Optional.empty();
    }
  }


  private void copyConnection(StepFunction sourceStep, StepFunction targetStep, int index, CopyData cpyData) {
    StepSerial targetGlobalStepSerial = targetStep.getParentWFObject().getWfAsStep().getChildStep();
    Map<AVariableIdentification, AVariableIdentification> varIdentMap = cpyData.getVariableIdentCopies();
    Dataflow sourceDataflow = cpyData.getSourceDataflow();
    Dataflow targetDataflow = cpyData.getTargetDataflow();
    IdentifiedVariables sourceIdentVars = sourceDataflow.identifyVariables(sourceStep);
    AVariableIdentification orgVarIdent = sourceIdentVars.getVariable(VarUsageType.input, index);

    if (orgVarIdent == null) {
      //input is connected, but source is not part of copy -> connection will be lost
      return;
    }

    IdentifiedVariables copyIdentVars = targetDataflow.identifyVariables(targetStep);
    AVariableIdentification copyVar = copyIdentVars.getVariable(VarUsageType.input, index);

    InputConnection connection = sourceDataflow.createCopyOfConnection(orgVarIdent, copyVar, varIdentMap, targetGlobalStepSerial);
    targetDataflow.getInputConnections().put(copyVar, connection);
  }


  private void copyOutputVariables(StepFunction sourceStep, StepFunction targetStep, CopyData cpyData) {
    Map<String, String> idMap = cpyData.getVariableIdMap();
    WF targetWF = targetStep.getParentWFObject();
    String[] sourceOutputVarIds = sourceStep.getOutputVarIds();
    boolean addedMissingSourceVar = false;
    Service sourceService = sourceStep.getService();
    List<AVariable> serviceOutputs = sourceService.getOutputVars();

    clearOutput(targetStep);

    for (int i = 0; i < sourceOutputVarIds.length; i++) {
      addedMissingSourceVar = false;
      //copy variable in step
      AVariable sourceVar = sourceStep.getOutputVars().get(i);
      String sourceId = sourceOutputVarIds[i];
      if (sourceVar == null) {
        Optional<AVariable> optSource = createMissingOutputSourceVar(targetWF, sourceStep, i);
        if(optSource.isEmpty()) {
          continue;
        }
        sourceVar = optSource.get();
        sourceId = sourceVar.getId();
        addedMissingSourceVar = true;
      }
      AVariable copy = StepCopier.copyVariable(sourceVar, targetWF, cpyData);

      if (addedMissingSourceVar) {
        //add to workflow
        StepSerial targetStepSerial = targetWF.getWfAsStep().getChildStep();
        targetStepSerial.addVar(copy);
        
        //add to service if service is a prototype
        //but only if source Output is null
        Service targetService = targetStep.getService();
        if (targetService.isPrototype() && serviceOutputs.get(i) == null) {
          targetService.getOutputVars().add(i, copy);
        }
      }

      //copy variableId -> creates Target RefId
      String copyId = StepCopier.getCopyVarId(sourceId, targetWF, idMap);
      targetStep.addOutputVarId(i, copyId);
    }

    copyOutputCasts(sourceStep, targetStep);
  }


  private void copyOutputCasts(StepFunction sourceStep, StepFunction targetStep) {
    String[] sourceCastOutput = sourceStep.getReceiveVarCastToType();
    String[] destCastOutput = targetStep.getReceiveVarCastToType();

    for (int i = 0; i < destCastOutput.length; i++) {
      destCastOutput[i] = sourceCastOutput[i];
    }
  }


  private void clearOutput(StepFunction step) {
    int outputCount = step.getOutputVarIds().length;
    for (int i = 0; i < outputCount; i++) {
      step.removeOutputVarId(0);
    }
    step.getOutputVars().clear();
  }


  //if copied step is inside a ForEach and the ForEach is not copied as well
  //we are missing the output variable. It was captured by the ForEach in the 
  //original workflow.
  private Optional<AVariable> createMissingOutputSourceVar(WF targetWF, StepFunction sourceStep, int i) {
    AVariable source = Utils.getServiceOutputVariables(sourceStep).get(i);
    CopyData tmpCpyData = new CopyData(null, null, null); //new object, since we the variable we create should be in the source workflow
    String castFqn = sourceStep.getReceiveVarCastToType()[i];

    
    if (source == null) {
      //can't reconstruct single/list
      if(sourceStep.isPrototype()) {
        AVariable prototype =  AVariable.createAnyType(targetWF, false);
        prototype.createPrototype("AnyType");
        return Optional.of(prototype);
      }
      
      return Optional.empty();
    }

    if (castFqn != null) {
      AVariable newOutputVar = StepCopier.copyVariable(source, targetWF, tmpCpyData);
      String label = source.getLabel();
      DomOrExceptionGenerationBase doe;
      long revision = targetWF.getRevision();
      try {
        doe = (DomOrExceptionGenerationBase) DomOrExceptionGenerationBase.getOrCreateInstance(castFqn, new GenerationBaseCache(), revision);
      } catch (XPRC_InvalidPackageNameException | Ex_FileAccessException | XPRC_XmlParsingException e) {
        throw new RuntimeException(e);
      }
      newOutputVar.replaceDomOrException(doe, label);
      return Optional.of(newOutputVar);
    }

    AVariable result = StepCopier.copyVariable(source, targetWF, tmpCpyData);
    return Optional.of(result);
  }

}
