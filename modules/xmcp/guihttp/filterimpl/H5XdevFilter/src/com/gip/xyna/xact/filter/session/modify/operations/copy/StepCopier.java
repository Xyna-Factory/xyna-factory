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



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.Dataflow.InputConnection;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.Modification;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.StepMap;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.modify.operations.CopyOperation;
import com.gip.xyna.xact.filter.session.repair.WorkflowRepair;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.DirectVarIdentification;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;

import xmcp.processmodeller.datatypes.RepairEntry;



public class StepCopier {

  private static IStepCopier<StepThrow> throwCopier = new StepThrowCopier();
  private static IStepCopier<StepRetry> retryCopier = new StepRetryCopier();
  private static IStepCopier<StepCatch> catchCopier = new StepCatchCopier();
  private static IStepCopier<StepAssign> assignCopier = new StepAssignCopier();
  private static IStepCopier<StepSerial> serialCopier = new StepSerialCopier();
  private static IStepCopier<StepChoice> choiceCopier = new StepChoiceCopier();
  private static IStepCopier<StepMapping> mappingCopier = new StepMappingCopier();
  private static IStepCopier<StepForeach> foreachCopier = new StepForeachCopier();
  private static IStepCopier<StepParallel> parallelCopier = new StepParallelCopier();
  private static IStepCopier<StepFunction> functionCopier = new StepFunctionCopier();


  public static Step copyStep(Step sourceStep, ScopeStep targetParentScope, CopyData cpyData) {
    Step result;

    if (sourceStep == null) {
      return null;
    }

    //TODO: use visitor to determine IStepCopier and casted step
    //  -> call copyStepInternal only once -> maybe move implementation here
    //determine correct IStepCopier and cast Step accordingly
    if (sourceStep instanceof StepMapping) {
      result = copyStepInternal(mappingCopier, (StepMapping) sourceStep, targetParentScope, cpyData);
    } else if (sourceStep instanceof StepParallel) {
      result = copyStepInternal(parallelCopier, (StepParallel) sourceStep, targetParentScope, cpyData);
    } else if (sourceStep instanceof StepSerial) {
      result = copyStepInternal(serialCopier, (StepSerial) sourceStep, targetParentScope, cpyData);
    } else if (sourceStep instanceof StepFunction) {
      result = copyStepInternal(functionCopier, (StepFunction) sourceStep, targetParentScope, cpyData);
    } else if (sourceStep instanceof StepCatch) {
      result = copyStepInternal(catchCopier, (StepCatch) sourceStep, targetParentScope, cpyData);
    } else if (sourceStep instanceof StepAssign) {
      result = copyStepInternal(assignCopier, (StepAssign) sourceStep, targetParentScope, cpyData);
    } else if (sourceStep instanceof StepRetry) {
      result = copyStepInternal(retryCopier, (StepRetry) sourceStep, targetParentScope, cpyData);
    } else if (sourceStep instanceof StepThrow) {
      result = copyStepInternal(throwCopier, (StepThrow) sourceStep, targetParentScope, cpyData);
    } else if (sourceStep instanceof StepForeach) {
      result = copyStepInternal(foreachCopier, (StepForeach) sourceStep, targetParentScope, cpyData);
    } else if (sourceStep instanceof StepChoice) {
      result = copyStepInternal(choiceCopier, (StepChoice) sourceStep, targetParentScope, cpyData);
    } else {
      throw new RuntimeException("unsupported step type: " + sourceStep.getClass());
    }

    return result;
  }


  private static <T extends Step> T copyStepInternal(IStepCopier<T> copier, T sourceStep, ScopeStep targetParentScope, CopyData cpyData) {
    T targetStep = copier.copyStep(sourceStep, targetParentScope, targetParentScope.getCreator(), cpyData);
    return targetStep;
  }


  //TODO: move to different class
  public static void copyVars(Step sourceStep, Step targetStep, CopyData cpyData) {
    if (sourceStep instanceof StepMapping) {
      copyVarsStepMapping((StepMapping) sourceStep, (StepMapping) targetStep, cpyData);
      return;
    }

    if (sourceStep instanceof StepParallel) {
      return;
    }

    if (sourceStep instanceof StepSerial) {
      StepSerial sourceStepSerial = (StepSerial) sourceStep;
      StepSerial targetStepSerial = (StepSerial) targetStep;
      Set<String> variablesInTarget = targetStepSerial.getServiceVariables().stream().map(x -> x.getId()).collect(Collectors.toSet());
      List<AVariable> vars = sourceStepSerial.getVariablesAndExceptions();
      for (AVariable var : vars) {
        AVariable copy = copyVariable(var, targetStep.getParentWFObject(), cpyData);
        boolean containsVariable = variablesInTarget.contains(copy.getId());
        if (!containsVariable)
          targetStepSerial.addVar(copy);
      }
      return;
    }

    throw new RuntimeException("unsupported Step type:" + sourceStep.getClassName());
  }


  /*package*/ static void copyConnection(CopyDataflowConnectionData conCpyData) {
    AVariable varToConnect = conCpyData.getVariableToConnect();
    Dataflow sourceDataflow = conCpyData.getOriginalDataflow();
    Dataflow destinationDataflow = conCpyData.getDataflowToUpdate();
    StepSerial targetGlobalStepSerial = conCpyData.getToUpdateGlobalStepSerial();
    Map<AVariableIdentification, AVariableIdentification> cloneMap = conCpyData.getVarCopies();
    AVariableIdentification from = readVarIdentFromAVar(sourceDataflow, conCpyData.getSourceVar());
    if (from == null) {
      return;
    }

    AVariableIdentification avar = DirectVarIdentification.of(varToConnect);
    InputConnection connection = sourceDataflow.createCopyOfConnection(from, avar, cloneMap, targetGlobalStepSerial);

    destinationDataflow.getInputConnections().put(avar, connection);
  }


  private static AVariableIdentification readVarIdentFromAVar(Dataflow sourceDataflow, AVariable sourceVar) {
    Set<AVariableIdentification> candidates = sourceDataflow.getInputConnections().keySet();
    for (AVariableIdentification candidate : candidates) {
      if (candidate.getIdentifiedVariable().equals(sourceVar)) {
        return candidate;
      }
    }

    return null;
  }


  private static void copyVarsStepMapping(StepMapping sourceStep, StepMapping targetStep, CopyData cpyData) {
    Map<String, String> idMap = cpyData.getVariableIdMap();
    StepSerial targetGlobalStepSerial = targetStep.getParentWFObject().getWfAsStep().getChildStep();
    InputConnections sourceInputConnections = sourceStep.getInputConnections();
    InputConnections targetInputConnections = targetStep.getInputConnections();

    //copy input variables
    for (int i = 0; i < sourceInputConnections.length(); i++) {
      //copy id
      String sourceId = sourceInputConnections.getVarIds()[i];
      boolean isUserConnected = sourceInputConnections.isUserConnected(sourceId);
      boolean isConstant = sourceInputConnections.isConstantConnected(sourceId);
      String varId = getCopyVarId(sourceId, targetStep.getParentWFObject(), idMap);
      targetInputConnections.addInputConnection(i, varId, null, isUserConnected, isConstant, null);

      //copy variable
      AVariable sourceVar = sourceStep.getInputVars().get(i);
      AVariable copyVar = copyVariable(sourceVar, targetStep.getParentWFObject(), cpyData);
      targetStep.getInputVars().add(copyVar);

      //copy DataFlow connection
      CopyDataflowConnectionData conCpyData = new CopyDataflowConnectionData();
      conCpyData.setDataflowToUpdate(cpyData.getTargetDataflow());
      conCpyData.setOriginalDataflow(cpyData.getSourceDataflow());
      conCpyData.setToUpdateGlobalStepSerial(targetGlobalStepSerial);
      conCpyData.setVariableToConnect(copyVar);
      conCpyData.setSourceVar(sourceVar);
      conCpyData.setVarCopies(cpyData.getVariableIdentCopies());

      copyConnection(conCpyData);
    }

    //copy output variables
    for (int i = 0; i < sourceStep.getOutputVarIds().length; i++) {
      //copy variable in step
      AVariable sourceVar = sourceStep.getOutputVars().get(i);
      AVariable copy = copyVariable(sourceVar, targetStep.getParentWFObject(), cpyData);
      targetStep.getOutputVars().add(copy);

      //copy variableId -> creates Target RefId
      String copyId = getCopyVarId(sourceStep.getOutputVarIds()[i], targetStep.getParentWFObject(), idMap);
      ((StepMapping) targetStep).addOutputVarId(i, copyId);
    }

    copyVarIdentification(cpyData, sourceStep, targetStep);

  }


  /*package*/ static void copyVarIdentification(CopyData cpyData, Step sourceStep, Step targetStep) {
    IdentifiedVariables iv = cpyData.getTargetDataflow().identifyVariables(targetStep);
    IdentifiedVariables orgIv = cpyData.getSourceDataflow().identifyVariables(sourceStep);
    List<AVariableIdentification> targetIdentification;

    targetIdentification = iv.getListAdapter(VarUsageType.input);
    for (int i = 0; i < targetIdentification.size(); i++) {
      AVariableIdentification cloneAvarIdent = iv.getVariable(VarUsageType.input, i);
      AVariableIdentification orgAvarIdent = orgIv.getVariable(VarUsageType.input, i);
      cpyData.getVariableIdentCopies().put(orgAvarIdent, cloneAvarIdent);
    }

    targetIdentification = iv.getListAdapter(VarUsageType.output);
    for (int i = 0; i < targetIdentification.size(); i++) {
      AVariableIdentification cloneAvarIdent = iv.getVariable(VarUsageType.output, i);
      AVariableIdentification orgAvarIdent = orgIv.getVariable(VarUsageType.output, i);
      cpyData.getVariableIdentCopies().put(orgAvarIdent, cloneAvarIdent);
    }
  }


  /*package*/ static AVariable copyVariable(AVariable sourceVar, WF parentWFObject, CopyData cpyData) {
    Map<String, String> idMap = cpyData.getVariableIdMap();
    String idOfCopy = null;
    String idOfOriginal = sourceVar.getId();
    if (idOfOriginal == null) {
      return copyVariableInternal(null, sourceVar, parentWFObject, cpyData);
    }

    idOfCopy = idMap.get(idOfOriginal);
    if (idOfCopy == null) {
      idOfCopy = "" + parentWFObject.getNextXmlId();
      idMap.put(idOfOriginal, idOfCopy);
    }

    return copyVariableInternal(idOfCopy, sourceVar, parentWFObject, cpyData);
  }


  /*package*/ static String getCopyVarId(String sourceId, WF targetWf, Map<String, String> idMap) {
    if (sourceId == null || sourceId.length() == 0) {
      return null;
    }
    String result = idMap.get(sourceId);
    if (result == null) {
      //we need to create a new variable-(id)
      result = "" + targetWf.getNextXmlId();
      idMap.put(sourceId, result);
    }
    return result;
  }


  private static AVariable copyVariableInternal(String id, AVariable sourceVar, WF parentWFObject, CopyData cpyData) {
    AVariable result = null;
    if (sourceVar.getDomOrExceptionObject() != null) {
      result = AVariable.createAVariable(id, sourceVar.getDomOrExceptionObject(), sourceVar.isList());
    } else {
      result = AVariable.createAnyType(parentWFObject, sourceVar.isList());
      if (sourceVar.isPrototype()) {
        result.createPrototype(sourceVar.getLabel());
      }
    }

    cpyData.getVariableCopies().put(sourceVar, result);
    CopyOperation.copyUnknownMetaTags(sourceVar, result);
    result.setValue(sourceVar.getValue());
    result.setValues(sourceVar.getValues());
    result.setLabel(sourceVar.getLabel());

    List<AVariable> sourceChildren = sourceVar.getChildren();
    if (sourceChildren != null) {
      for (AVariable sourceChild : sourceChildren) {
        AVariable cpyChild = copyVariableInternal("" + parentWFObject.getNextXmlId(), sourceChild, parentWFObject, cpyData);
        cpyChild.setVarName(sourceChild.getVarName());
        result.addChild(cpyChild);
      }
    }

    return result;
  }


  /*package*/ static void copyOutputVarIdents(Step sourceStep, Step targetStep, CopyData cpyData) {
    Map<AVariableIdentification, AVariableIdentification> varIdentCopies = cpyData.getVariableIdentCopies();
    Dataflow sourceDataflow = cpyData.getSourceDataflow();
    Dataflow targetDataflow = cpyData.getTargetDataflow();

    List<AVariableIdentification> orgVarIdent = sourceDataflow.identifyVariables(sourceStep).getListAdapter(VarUsageType.output);
    List<AVariableIdentification> cpyVarIdent = targetDataflow.identifyVariables(targetStep).getListAdapter(VarUsageType.output);

    for (int i = 0; i < cpyVarIdent.size(); i++) {
      AVariableIdentification orgVar = orgVarIdent.get(i);
      AVariableIdentification cpyVar = cpyVarIdent.get(i);
      varIdentCopies.put(orgVar, cpyVar);
    }
  }


  public static Step copyStepInto(Step sourceStep, StepSerial targetStepSerial, int index, CopyData data) {

    //add variables from parentStepSerial
    StepSerial sourceStepSerial = getParentStepSerial(sourceStep);
    StepCopier.copyVars(sourceStepSerial, targetStepSerial, data);
    
    //add variables from StepSerial in parentScope
    //if parentScope is WFStep, we already added these variables.
    ScopeStep parentScope = sourceStep.getParentScope();
    if (!(parentScope instanceof WFStep)) {
      sourceStepSerial = parentScope.getChildStep();
      StepCopier.copyVars(sourceStepSerial, targetStepSerial, data);
    }
    
    //add variables from globalStepSerial
    StepSerial sourceGlobalSerial = sourceStep.getParentWFObject().getWfAsStep().getChildStep();
    if (sourceGlobalSerial != sourceStepSerial) {
      StepSerial targetGlobalSerial = targetStepSerial.getParentWFObject().getWfAsStep().getChildStep();
      StepCopier.copyVars(sourceGlobalSerial, targetGlobalSerial, data);
    }
    
    //copy step
    Step result = StepCopier.copyStep(sourceStep, targetStepSerial.getParentScope(), data);
    targetStepSerial.addChild(index, result);
    return result;
  }


  private static StepSerial getParentStepSerial(Step step) {
    step = step.getParentStep();
    while (!(step instanceof StepSerial)) {
      step = step.getParentStep();
    }
    return (StepSerial) step;
  }
  
  public static GBSubObject copyStepIntoGenerationBaseObject(GenerationBaseObject gbo, GBSubObject object) {
    Step sourceStep = getActualStepToCopy(object.getStep());
    StepMap targetStepMap = gbo.getStepMap();
    WF wf = gbo.getWFStep().getWF();
    Dataflow sourceDataflow = object.getRoot().getDataflow();
    CopyData data = new CopyData(targetStepMap, gbo.getDataflow(), sourceDataflow);
    StepSerial targetStepSerial = wf.getWfAsStep().getChildStep();

    if (QueryUtils.isQuery(object)) {
      Step mapping = QueryUtils.findQueryHelperMapping(object);
      StepCopier.copyStepInto(mapping, targetStepSerial, 0, data);
      StepCopier.copyStepInto(sourceStep, targetStepSerial, 1, data);
    } else {
      StepCopier.copyStepInto(sourceStep, targetStepSerial, 0, data);
    }
    gbo.getDataflow().applyDataflowToGB();
    
    GBSubObject result = new GBSubObject(gbo, ObjectId.createStepId(wf.getWfAsStep().getChildStep()), wf.getWfAsStep().getChildStep());

    return result;
  }

  private static Step getActualStepToCopy(Step sourceStep) {
    if ((sourceStep instanceof StepFunction) && sourceStep.getParentStep() instanceof StepCatch) {
      sourceStep = sourceStep.getParentStep();
    }

    if (sourceStep instanceof StepForeach) {
      sourceStep = findChildOfStepForeach(sourceStep);
    }
    return sourceStep;
  }
  
  private static Step findChildOfStepForeach(Step sourceStep) {
    while (sourceStep instanceof StepForeach) {
      sourceStep = sourceStep.getChildSteps().get(0).getChildSteps().get(0).getChildSteps().get(0);
    }
    return sourceStep;
  }
  
  
  public static List<RepairEntry> copyStepInto(GenerationBaseObject toInsert, Integer index, StepSerial targetStepSerial, Modification modification) {
    List<Step> sourceStepsToInsert = toInsert.getWFStep().getChildStep().getChildSteps();
    StepMap targetStepMap = modification.getObject().getStepMap();
    if (index == null || index == -1) {
      index = targetStepSerial.getChildSteps().size();
    }

    Set<Step> stepsToRepair = new HashSet<Step>();
    for (Step s : sourceStepsToInsert) {
      WF.addChildStepsRecursively(stepsToRepair, s);
    }
    stepsToRepair.addAll(sourceStepsToInsert);

    WorkflowRepair repair = new WorkflowRepair();
    List<RepairEntry> repairEntries = repair.repairSteps(new ArrayList<Step>(stepsToRepair), toInsert);

    toInsert.createDataflow();
    CopyData data = new CopyData(targetStepMap, modification.getObject().getDataflow(), toInsert.getDataflow());

    for (Step sourceStep : sourceStepsToInsert) {
      StepCopier.copyStepInto(sourceStep, targetStepSerial, index++, data);
    }

    return repairEntries;
    
  }
}
