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

package com.gip.xyna.xact.filter.session.repair;



import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.ExpressionUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.TokenType;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.XFLLexem;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.BranchInfo;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.CaseInfo;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;

import xmcp.processmodeller.datatypes.RepairEntry;



public class WorkflowRepair implements RepairInterface {

  @Override
  public List<RepairEntry> getRepairEntries(GenerationBaseObject obj) {
    return repair(obj, false);
  }


  @Override
  public List<RepairEntry> repair(GenerationBaseObject obj) {
    return repair(obj, true);
  }


  @Override
  public boolean responsible(GenerationBaseObject obj) {
    return obj.getGenerationBase() instanceof WF;
  }


  public List<RepairEntry> repairSteps(List<Step> steps, GenerationBaseObject obj) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    RecursiveStepRepairVisitor visitor = createWorkflowVisitor(true, obj);

    for (Step s : steps) {
      s.visit(visitor);
    }

    result = visitor.getResult();
    String resourceName = obj.getWFStep().getWF().getFqClassName();
    for (RepairEntry e : result) {
      e.setResource(resourceName);
    }

    return result;
  }


  public List<RepairEntry> repairAuditWorkflow(WF workflow) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    RecursiveStepRepairVisitor visitor = new RecursiveStepRepairVisitor(true);
    WFStep wf = (WFStep) workflow.getWfAsStep();
    Set<Step> steps = wf.getAllStepsRecursively();

    visitor.addStepMappingRepair(this::mergeLocals);
    visitor.addStepChoiceRepair(this::fillMissingCaseIds);

    for (Step s : steps) {
      s.visit(visitor);
    }

    result = visitor.getResult();
    for (RepairEntry e : result) {
      e.setResource(workflow.getFqClassName());
    }

    return result;
  }
  
  
  private RecursiveStepRepairVisitor createWorkflowVisitor(boolean apply, GenerationBaseObject obj) {
    RecursiveStepRepairVisitor visitor = new RecursiveStepRepairVisitor(apply);
    StepFunctionRepair stepFunctionRepair = new StepFunctionRepair();
    BiFunction<StepChoice, Boolean, List<RepairEntry>> removeTypeChoiceFun;
    removeTypeChoiceFun = createRemoveTypeChoiceFunction(obj);

    //set Repair Functions in visitor
    visitor.addStepChoiceRepair(removeTypeChoiceFun);
    visitor.addStepChoiceRepair(this::convertStepChoiceInputToPrototype);
    visitor.addStepChoiceRepair(this::removeTypeChoiceLanes);
    visitor.addStepChoiceRepair(this::removeChoiceOutputConstant);
    visitor.addStepChoiceRepair(this::fillMissingCaseIds);
    visitor.addStepChoiceRepair(this::removeUnusedInputs);

    visitor.addStepThrowRepair(this::changeThrowToXynaExceptionBase);

    visitor.addWFStepRepair(this::convertStepInputToPrototype);
    visitor.addWFStepRepair(this::convertStepOutputToPrototype);

    stepFunctionRepair.registerFunctions(visitor::addStepFunctionRepair, obj);

    visitor.addStepMappingRepair(this::convertStepInputToPrototype);
    visitor.addStepMappingRepair(this::convertStepOutputToPrototype);
    visitor.addStepMappingRepair(this::removeConstantFromMappingInput);
    visitor.addStepMappingRepair(this::mergeLocals);

    visitor.addStepCatchRepair(this::removeCatchLanes);
    visitor.addStepCatchRepair(this::removeStepCatchOutputConstant);

    visitor.addStepParallelRepair(removeObsoleteStepParallel(obj));
    
    visitor.addStepForeachRepair(this::removeForeachWithoutconnection);
    
    return visitor;
  }

  
  private List<RepairEntry> repair(GenerationBaseObject obj, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    WFStep wf = obj.getWFStep();
    Set<Step> steps = wf.getAllStepsRecursively();
    RecursiveStepRepairVisitor visitor = createWorkflowVisitor(apply, obj);

    for (Step s : steps) {
      s.visit(visitor);
    }

    result = visitor.getResult();
    for (RepairEntry e : result) {
      e.setResource(obj.getFQName().getFqName());
    }

    return result;
  }


  private List<RepairEntry> removeChoiceOutputConstant(StepChoice step, boolean apply) {
    List<Step> childSteps = step.getChildSteps();
    return callRemoveConstantFromStepAssign(step, childSteps, apply);
  }
  
  
  private List<RepairEntry> removeUnusedInputs(StepChoice step, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();

    if (ExpressionUtils.calculateVariableIndicesToRemove(step).size() > 0) {
      RepairEntry entry = new RepairEntry();
      entry.setDescription("Unused Input variable");
      entry.setId(ObjectId.createStepId(step).getObjectId());
      entry.setLocation("StepChoice");
      entry.setType("Input variable removal");
      result.add(entry);

      if (apply) {
        ExpressionUtils.cleanUpChoiceInputsAndConditions(step);
      }
    }

    return result;
  }


  private List<RepairEntry> fillMissingCaseIds(StepChoice step, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    WF wf = step.getParentWFObject();
    
    List<CaseInfo> cases = step.getHandledCases();
    for(CaseInfo caseInfo : cases) {
      if (caseInfo.getId() == null || caseInfo.getId().length() == 0) {
        RepairEntry entry = new RepairEntry();
        entry.setDescription("Choice case has no id");
        entry.setId(step.getStepId());
        entry.setLocation("Choice step with Expression: " + step.getOuterConditionForGUI());
        entry.setType("Id generation");
        result.add(entry);

        if (apply) {
          step.getCaseInfos().remove(caseInfo.getId());
          int id = wf.getNextXmlId();
          caseInfo.setId(String.valueOf(id));
          step.getCaseInfos().put(String.valueOf(id), caseInfo);
          
          //if this is a main case we also have to update id of StepSerial
          Optional<Step> stepSerial = findStepSerialToCase(caseInfo, step);
          if(stepSerial.isPresent()) {
            stepSerial.get().setXmlId(id);
          }
          
        }
      }
    }

    return result;
  }



  private Optional<Step> findStepSerialToCase(CaseInfo caseInfo, StepChoice step) {
    if(!caseInfo.isMainCaseOfItsBranch()) {
      return Optional.empty();
    }
    
    //number of main cases before us
    int branchIndex = 0;
    int numberOfCases = step.getHandledCases().size();
    for(int i=0; i<numberOfCases; i++) {
      CaseInfo comp = step.getHandledCases().get(i);
      if(comp == caseInfo) {
        break;
      }
      if(comp.isMainCaseOfItsBranch()) {
        branchIndex++;
      }
    }
    
    return Optional.of(step.getChildSteps().get(branchIndex));
  }


  private List<RepairEntry> removeStepCatchOutputConstant(StepCatch step, boolean apply) {
    List<Step> childSteps = new ArrayList<Step>(step.getChildSteps()); //includes output of Step in try block
    return callRemoveConstantFromStepAssign(step, childSteps, apply);
  }


  private BiFunction<StepParallel, Boolean, List<RepairEntry>> removeObsoleteStepParallel(GenerationBaseObject gbo) {
    return (s, a) -> {
      return callRemoveObsoleteStepParallel(s, gbo, a);
    };
  }

  
  private List<String> getInputVarsOfStepInForeach(StepForeach step){
    List<String> result;
    
    Step s = step.getChildScope().getChildStep().getChildSteps().get(0);
    if(s instanceof StepForeach) {
      return getInputVarsOfStepInForeach((StepForeach)s);
    }
    
    result = Arrays.asList(s.getInputVarIds());
    return result;
  }

  private List<RepairEntry> removeForeachWithoutconnection(StepForeach step, boolean apply){
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    List<String> childInputVars = getInputVarsOfStepInForeach(step);
      

    for (AVariable sfvar : step.getInputVarsSingle()) {
      if(childInputVars.contains(sfvar.getId())) {
        return Collections.emptyList(); //if someThing is still in use, we do not remove the ForEach
      }
    }
    
    
    RepairEntry entry = new RepairEntry();
    entry.setDescription("Unconnected ForEach (id: " + step.getStepId() + ")");
    entry.setId(ObjectId.createStepId(step).getObjectId());
    entry.setLocation(createLocation(step));
    entry.setType("Remove Foreach");
    result.add(entry);

    if (apply) {
      //we need to remove this ForEach! -- similar to DataFlow
      AVariable[] varsToUpdate = step.getOutputVarsSingle();

      //pass private scopeStep variables to workflow
      for (AVariable var : step.getChildScope().getPrivateVars()) {
        step.getParentWFObject().getWfAsStep().getChildStep().addVar(var);
      }

      //if this step is under a ForEach, but there is another ForEach around that, and we have to remove the outer ForEach, 
      //we do not want to replace it with currentStep, but with our parent-ForEach.
      Step parentStep = step.getParentStep();
      Step replacement = Dataflow.getSurroundingStep(step.getChildSteps().get(0), step);
      if (replacement == step) {
        replacement = step.getChildScope().getChildStep().getChildSteps().get(0);
      }

      parentStep.getChildSteps().set(parentStep.getChildSteps().indexOf(step), replacement);
      replacement.setParentScope(parentStep.getParentScope());
      Utils.updateScopeOfSubSteps(replacement);

      //these variables were removed from their StepSerial when they were added to StepForeach output
      for (int i = 0; i < varsToUpdate.length; i++) {
        step.getParentWFObject().getWfAsStep().getChildStep().addVar(varsToUpdate[i]);
      }
    }
    
    
    return result;
  }
  

  private List<RepairEntry> callRemoveConstantFromStepAssign(Step parent, List<Step> paths, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    for (Step s : paths) {
      if (s instanceof StepSerial) {
        result.addAll(removeConstantFromStepAssign(parent, (StepSerial) s, apply));
      }
    }

    return result;
  }


  private List<RepairEntry> removeConstantFromStepAssign(Step parent, StepSerial serial, boolean apply) {
    StepAssign assign = findLastStepAssign(serial);

    if (assign == null) {
      return Collections.emptyList(); // should not happen!
    }

    String[] ids = assign.getInputVarIds();
    InputConnections connections = assign.getInputConnections();
    List<RepairEntry> result = removeConstant(assign, ids, connections, apply);
    String objectId = ObjectId.createStepId(parent).getObjectId();
    String location = createLocation(parent);

    //update result -> should say we repaired workflow output and not some invisible StepAssign
    for (RepairEntry entry : result) {
      entry.setId(objectId);
      entry.setLocation(location);
    }

    return result;
  }


  private StepAssign findLastStepAssign(StepSerial step) {
    List<Step> steps = new ArrayList<Step>(step.getChildSteps());
    Collections.reverse(steps);
    for (Step s : steps) {
      if (s instanceof StepAssign) {
        return (StepAssign) s;
      }
    }

    return null;
  }


  private List<RepairEntry> removeConstantFromMappingInput(StepMapping step, boolean apply) {
    String[] ids = step.getInputVarIds();
    InputConnections connections = step.getInputConnections();
    return removeConstant(step, ids, connections, apply);
  }


  private void applyMergeLocals(StepMapping step) {
    String[] localIds = step.getLocalVarIds();
    int decrement = localIds.length;
    int numberOfInputs = step.getInputVarIds().length;
    int numberOfExpressions = step.getRawExpressions().size();
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < numberOfExpressions; i++) {
      for (int j = 0; j < decrement; j++) {
        String expression = step.getRawExpressions().get(i); // get expression in current state;
        sb.setLength(0);

        String value = "";
        try {
          value = step.getParentScope().identifyVariable(localIds[j]).getVariable().getValue();
        } catch (XPRC_InvalidVariableIdException e) {
          throw new RuntimeException("could not merge Locals");
        }

        List<XFLLexem> lexems = XFLLexer.lex(expression, true);
        for (XFLLexem lexem : lexems) {
          if (TokenType.VARIABLE == lexem.getType()) {
            if (!lexem.getToken().equals("%" + (numberOfInputs + j) + "%")) {
              // leave the variable index as it is
              sb.append(lexem.getToken());
            } else {
              //replace with value
              sb.append("\"");
              sb.append(value);
              sb.append("\"");
            }
          } else if (TokenType.ARGUMENT_SEPERATOR == lexem.getType()) {
            // add a blank after argument separator
            sb.append(lexem.getToken()).append(" ");
          } else {
            // leave the token as it is
            sb.append(lexem.getToken());
          }
        }
        step.replaceFormula(i, sb.toString());
      }
    }

    //add dummy variables (so inputVarRemoved decreases all relevant items)
    for (int i = 0; i < decrement; i++) {
      step.getInputVars().add(0, null);
    }

    for (int i = 0; i < decrement; i++) {
      step.inputVarRemoved(numberOfInputs); //locals are like inputs after regular inputs => decrease everything after it %X%
    }

    //remove dummy variables
    for (int i = 0; i < decrement; i++) {
      step.getInputVars().remove(0);
    }

    //clear arrays
    System.arraycopy(step.getLocalVarIds(), 0, step.getLocalVarIds(), 0, 0);
    System.arraycopy(step.getLocalVarPaths(), 0, step.getLocalVarPaths(), 0, 0);
  }
  
  private List<RepairEntry> mergeLocals(StepMapping step, boolean apply) {
    String[] localIds = step.getLocalVarIds();

    if (localIds == null || localIds.length == 0) {
      return Collections.emptyList();
    }

    List<RepairEntry> result = new ArrayList<RepairEntry>();
    RepairEntry entry = new RepairEntry();
    entry.setDescription("Mapping has local tags.");
    entry.setId(ObjectId.createStepId(step).getObjectId());
    entry.setLocation(createLocation(step));
    entry.setType("Merge local tags");
    result.add(entry);

    if (apply) {
      applyMergeLocals(step);
    }


    return result;
  }


  private List<RepairEntry> removeCatchLanes(StepCatch step, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    List<BranchInfo> lanes = step.getBranchesForGUI();
    String location = createLocation(step.getStepInTryBlock());

    for (int i = 0; i < lanes.size(); i++) {
      BranchInfo s = lanes.get(i);
      for (CaseInfo caseInfo : s.getCases()) {
        if (catchCaseNeedsToBeRemoved(step, caseInfo)) {
          RepairEntry entry = new RepairEntry();
          entry.setDescription("Catch Lane is invalid.");
          entry.setId(ObjectId.createCaseId(ObjectId.createStepId(step).getObjectId(), String.valueOf(i)));
          entry.setLocation(location);
          entry.setType("Catch Lane Removal.");
          result.add(entry);

          if (apply) {
            step.removeBranch(i);
          }
          break;
        }
      }
    }

    return result;
  }


  private boolean catchCaseNeedsToBeRemoved(StepCatch stepCatch, CaseInfo info) {
    String fqn = info.getComplexName();
    Long revision = stepCatch.getParentWFObject().getRevision();

    //type does not exist anymore
    if (!typeExists(fqn, revision)) {
      return true;
    }

    GenerationBase doe = null;

    try {
      doe = DomOrExceptionGenerationBase.getOrCreateInstance(fqn, new GenerationBaseCache(), revision);
    } catch (XPRC_InvalidPackageNameException | Ex_FileAccessException | XPRC_XmlParsingException e) {
      return false;
    }

    //type is no longer an exception, abstract or invalid
    if (!(doe instanceof ExceptionGeneration) || doe.isAbstract() || !XMOMRepair.canRetrieveRootTag((ExceptionGeneration) doe)) {
      return true;
    }

    return false;
  }


  private List<RepairEntry> removeTypeChoiceLanes(StepChoice step, boolean apply) {
    if (step.getDistinctionType() != DistinctionType.TypeChoice) {
      return Collections.emptyList();
    }

    AVariable parentType = step.getInputVars().get(0);
    String id = ObjectId.createStepId(step).getObjectId();

    //entire TypeChoice will be removed (but not by this method)
    if (XMOMRepair.variableHasToBeConverted(parentType)) {
      return Collections.emptyList();
    }

    List<RepairEntry> result = new ArrayList<RepairEntry>();

    for (int i = step.getChildSteps().size() - 1; i >= 0; i--) {
      AVariable typeOfCase = step.createInputVariableForBranch(i);

      if (invalidType(typeOfCase, parentType)) {
        RepairEntry entry = new RepairEntry();

        entry.setDescription("Type Choice lane invalid");
        entry.setId(id);
        entry.setType("Type Choice lane removal");
        entry.setLocation(parentType.getFQClassName());
        result.add(entry);

        if (apply) {
          //remove this child
          step.removeCase(i);
        }
      }
    }

    return result;
  }


  private boolean invalidType(AVariable avar, AVariable supposedParent) {

    DomOrExceptionGenerationBase avarDoe = avar.getDomOrExceptionObject();
    DomOrExceptionGenerationBase sParentDoe = supposedParent.getDomOrExceptionObject();

    if (avarDoe == null || !avarDoe.exists() || sParentDoe == null) {
      return true;
    }

    return !DomOrExceptionGenerationBase.isSuperClass(sParentDoe, avarDoe);
  }


  private BiFunction<StepChoice, Boolean, List<RepairEntry>> createRemoveTypeChoiceFunction(GenerationBaseObject gbo) {
    return (s, a) -> {
      return removeTypeChoice(s, a, gbo);
    };
  }


  private List<RepairEntry> removeTypeChoice(StepChoice step, boolean apply, GenerationBaseObject gbo) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();

    if (step.getDistinctionType() != DistinctionType.TypeChoice) {
      return result;
    }

    AVariable v = step.getInputVars().get(0);

    if (!XMOMRepair.variableHasToBeConverted(v)) {
      return result;
    }

    Step parent = step.getParentStep();
    RepairEntry entry = new RepairEntry();

    entry.setDescription("TypeChoice variable does not exist");
    entry.setId(ObjectId.createStepId(step.getParentStep()).getObjectId());
    entry.setLocation(v.getLabel());
    entry.setType("Step Removal");
    result.add(entry);

    if (apply) {
      parent.getChildSteps().remove(step);
      if (parent instanceof StepSerial) {
        Utils.removeWrapperWhenObsolete(gbo, (StepSerial) parent);
      }
    }

    return result;
  }


  private List<RepairEntry> changeThrowToXynaExceptionBase(StepThrow step, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    AVariable v = null;
    boolean changeRequired = false;

    if (step.getInputVars() == null || step.getInputVars().size() == 0) {
      String exceptionFqn = step.getExceptionTypeFqn();
      Long revision = step.getParentWFObject().getRevision();
      ExceptionGeneration eg = null;
      try {
        eg = ExceptionGeneration.getInstance(exceptionFqn, revision);
        if (!eg.exists()) {
          changeRequired = true;
        }

        if (!eg.isReservedServerObject()) {
          DomOrExceptionGenerationBase.retrieveRootTag(eg.getFqClassName(), eg.getRevision());
        }

      } catch (XPRC_InvalidPackageNameException | Ex_FileAccessException | XPRC_XmlParsingException e) {
        changeRequired = true;
      }

    } else {
      v = step.getInputVars().get(0);
      changeRequired = v.isPrototype() || !v.getDomOrExceptionObject().exists();
    }

    if (changeRequired) {
      RepairEntry entry = new RepairEntry();

      entry.setDescription("Invalid throw input");
      entry.setId(ObjectId.createStepId(step).getObjectId());
      entry.setLocation(step.getLabel());
      entry.setType("Convert to XynaExceptionBase");

      result.add(entry);


      if (apply) {
        String stepId = step.getStepId();
        //update preferedExceptionType in StepThrow
        step.create("core.exception.XynaExceptionBase", step.getLabel());
        try {
          if (stepId != null) {
            step.setXmlId(Integer.valueOf(stepId));
          } else {
            step.setXmlId(null);
          }
        } catch (NullPointerException e) {

        }

        //create dummy variable, update Exception variable
        step.setExceptionID(null); //remove ExceptionID to change type of dummy
        ExceptionVariable dummy = step.createDummyVar();
        Integer createdVarId = step.getCreator().getNextXmlId();
        dummy.setId(String.valueOf(createdVarId));
        dummy.setLabel("XynaExceptionBase");
        ExceptionGeneration eg = ExceptionGenerationRepair.createXynaExceptionBaseExceptionGeneration(step.getCreator().getRevision());
        dummy.replaceDomOrException(eg, "XynaExceptionBase");
        step.getParentScope().addPrivateVariable(dummy);
        step.setExceptionID(dummy.getId());
        step.getInputVars();
        step.setExceptionID(null);
      }
    }

    return result;
  }


  private List<RepairEntry> convertStepChoiceInputToPrototype(StepChoice step, boolean apply) {

    //if it is a typeChoice that will be removed, don't try to convert input
    if (step.getDistinctionType() == DistinctionType.TypeChoice) {
      AVariable v = step.getInputVars().get(0);
      if (XMOMRepair.variableHasToBeConverted(v)) {
        return Collections.emptyList();
      }
    }

    return convertStepInputToPrototype(step, apply);
  }


  private List<RepairEntry> convertStepInputToPrototype(Step step, boolean apply) {
    List<AVariable> avars = step.getInputVars();
    Function<Integer, String> idGenerator = createVariableIDGenerator(step, VarUsageType.input);
    String location = createLocation(step);

    return XMOMRepair.convertAVariableList(location, avars, idGenerator, apply);
  }


  private List<RepairEntry> convertStepOutputToPrototype(Step step, boolean apply) {
    Function<Integer, String> idGenerator = createVariableIDGenerator(step, VarUsageType.output);
    String location = createLocation(step);
    List<AVariable> avars = null;

    try {
      avars = step.getOutputVars();
    } catch (RuntimeException e) {
      return new ArrayList<RepairEntry>();
    }

    return XMOMRepair.convertAVariableList(location, avars, idGenerator, apply);
  }


  private List<RepairEntry> callRemoveObsoleteStepParallel(StepParallel step, GenerationBaseObject gbo, boolean apply) {
    boolean removeRequired = false;
    if (step.getChildSteps() == null) {
      removeRequired = true;
    } else if (step.getChildSteps().size() == 0 || step.getChildSteps().size() == 1) {
      removeRequired = true;
    }

    if (!removeRequired) {
      return Collections.emptyList();
    }

    List<RepairEntry> result = new ArrayList<RepairEntry>();
    RepairEntry entry = new RepairEntry();
    entry.setDescription("Invalid StepParallel");
    entry.setId(ObjectId.createStepId(step).getObjectId());
    entry.setLocation("StepParallel"); //TODO: location?
    entry.setType("Step Parallel removal");
    result.add(entry);

    if (apply) {
      if (step.getChildSteps() == null || step.getChildSteps().size() == 0) {
        //remove
        step.getParentStep().getChildSteps().remove(step);
      } else {
        Utils.removeWrapperIfSingleLane(gbo.getStepMap(), step);
      }
    }

    return result;
  }


  /*package*/ static String createLocation(Step step) {
    String location = null;

    try {
      location = step.getLabel();
    } catch (UnsupportedOperationException e) {
      if (step instanceof WFStep) {
        location = "workflow";
      } else if (step instanceof StepSerial && step.getParentStep() instanceof WFStep) {
        location = "workflow";
      } else if (step instanceof StepAssign && isWorkflowOutputAssign(step)) {
        location = "workflow";
      } else if (step instanceof StepCatch && isWorkflowCatch((StepCatch) step)) {
        location = "workflow";
      } else {
        location = ObjectId.createStepId(step).getObjectId();
      }
    }

    return location;
  }


  private static boolean isWorkflowCatch(StepCatch step) {
    return step.getStepInTryBlock() == step.getParentWFObject().getWfAsStep().getChildStep();
  }


  private static boolean isWorkflowOutputAssign(Step step) {
    List<Step> stepList = step.getParentWFObject().getWfAsStep().getChildStep().getChildSteps();
    if (stepList.size() == 0) {
      return false;
    }

    return stepList.get(stepList.size() - 1) == step;
  }


  /*package*/ static boolean typeExists(String fqn, Long revision) {
    if (DatatypeVariable.ANY_TYPE.equals(fqn)) {
      return true;
    }
    try {
      DomOrExceptionGenerationBase.getOrCreateInstance(fqn, new GenerationBaseCache(), revision);
    } catch (XPRC_InvalidPackageNameException | Ex_FileAccessException | XPRC_XmlParsingException e) {
      return false;
    }

    return true;
  }


  /*package*/ static Function<Integer, String> createVariableIDGenerator(Step step, VarUsageType usage) {
    return (idx) -> ObjectId.createVariableId(step.getStepId(), usage, idx);
  }


  /*package*/ static List<RepairEntry> removeConstant(Step step, String[] ids, InputConnections connections, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    Boolean[] constants = connections.getConstantConnected();
    boolean repairRequired;
    String description = "";
    for (int i = 0; i < ids.length; i++) {
      if (constants[i] == null || constants[i] == false || ids[i] == null) {
        continue;
      }
      repairRequired = false;

      AVariable var = null;
      try {
        var = step.getParentScope().identifyVariable(ids[i]).getVariable();
      } catch (Exception e) {
        repairRequired = true;
        description = "Constant variable missing (id: " + ids[i] + ").";
      }

      if (var != null) {
        repairRequired |= XMOMRepair.variableHasToBeConverted(var);
        description = "Constant variable has invalid type.";
      }

      if (repairRequired) {
        RepairEntry entry = new RepairEntry();

        entry.setDescription(description);
        entry.setId(ObjectId.createStepId(step).getObjectId());
        entry.setLocation(createLocation(step));
        entry.setType("Remove constant");

        result.add(entry);

        if (apply) {
          constants[i] = false;
        }
      } else { //variable was not converted - we have to check members
        result.addAll(handleInvalidMembersOfConstant(var, step, apply));
      }
    }

    return result;
  }
  

  private static List<RepairEntry> handleInvalidMembersOfConstant(AVariable var, Step step, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    List<VariableToRemove> invalidMembers = collectInvalidMembers(var);
    if (invalidMembers.size() > 0) {
      RepairEntry entry = new RepairEntry();
      StringBuilder sb = new StringBuilder();
      sb.append("Constant variable has ").append(invalidMembers.size());
      sb.append(" invalid member").append(invalidMembers.size() > 1 ? "s" : "");
      sb.append(": [");
      for (VariableToRemove av : invalidMembers) {
        sb.append(av.getVarToRemove().getVarName()).append(", ");
      }
      sb.setLength(sb.length() - 2); //remove ", "
      sb.append("]");
      entry.setDescription(sb.toString());
      entry.setId(ObjectId.createStepId(step).getObjectId());
      entry.setLocation(createLocation(step));
      entry.setType("Remove invalid member in constant");

      result.add(entry);

      if (apply) {
        for (VariableToRemove toRemove : invalidMembers) {
          ArrayList<AVariable> lst = new ArrayList<AVariable>();
          lst.add(toRemove.getVarToRemove());
          toRemove.getParent().removeChildren(lst);
        }
      }
    }
    return result;
  }


  private static List<VariableToRemove> collectInvalidMembers(AVariable v) {

    if (v.getDomOrExceptionObject() == null || v.getDomOrExceptionObject().isReservedServerObject()) {
      return Collections.emptyList();
    }

    List<VariableToRemove> result = new ArrayList<VariableToRemove>();
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    Class<?> constantClass = null;
    ClassLoaderType clt = v instanceof ExceptionVariable ? ClassLoaderType.Exception : ClassLoaderType.MDM;
    try {
      constantClass = cld.loadClassWithClassLoader(clt, v.getFQClassName(), v.getFQClassName(), v.getDomOrExceptionObject().getRevision());
    } catch (ClassNotFoundException e) {
      return result;
    }

    if (v.isList()) {
      for (AVariable entry : v.getChildren()) {
        result.addAll(collectInvalidMembers(entry));
      }
    } else {
      for (AVariable candidate : v.getChildren()) {
        Field field = findFieldInHierarchy(candidate.getVarName(), constantClass);
        if (field == null || !isConstantMemberContentValid(field, candidate)) {
          VariableToRemove resultEntry = new VariableToRemove(v, candidate);
          result.add(resultEntry); //field not found or invalid
        }
      }
    }

    return result;
  }


  private static Field findFieldInHierarchy(String fieldName, Class<?> clazz) {
    Field result = null;
    while (clazz != null) {
      try {
        result = clazz.getDeclaredField(fieldName);
        break;
      } catch (NoSuchFieldException | SecurityException e) {
        clazz = clazz.getSuperclass();
      }
    }

    return result;
  }


  private static boolean isConstantMemberContentValid(Field field, AVariable candidate) {
    //list/single
    if (field.getType().equals(java.util.List.class)) {
      if (!candidate.isList()) {
        return false;
      } else {
        return true; //no additional checks for the 'list' element (not an entry in the list)
      }
    }

    //type name
    String fieldType = null;
    String candidateType = null;
    if (field.getType().equals(java.util.List.class)) {
      fieldType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0].getTypeName();
    } else {
      fieldType = field.getType().getTypeName();
    }

    candidateType = (candidate.getFQClassName() != null) ? candidate.getFQClassName() : candidate.getJavaTypeEnum().getFqName();

    //if candidate is JavaType, match equals
    if (candidate.getFQClassName() == null) {
      if (!fieldType.equals(candidateType)) {
        return false;
      }
    } else { //if candidate is a modeled XMOM object, check subTypes as well
      GenerationBase candidateDoe = null;
      GenerationBase fieldDoe = null;
      
      //convert reservedServerTypes
      if(GenerationBase.isReservedServerObjectByFqClassName(fieldType)) {
        fieldType = GenerationBase.getXmlNameForReservedClass(field.getType());
      }

      //checkAbstract
      try {
        fieldDoe = DomOrExceptionGenerationBase.getOrCreateInstance(fieldType, new GenerationBaseCache(), candidate.getRevision());
      } catch (XPRC_InvalidPackageNameException | Ex_FileAccessException | XPRC_XmlParsingException e) {
        return false;
      }
      
      candidateDoe = candidate.getDomOrExceptionObject();
      
      //field should always be DOM or EXCEPTION
      if (!(fieldDoe instanceof DomOrExceptionGenerationBase) || !(candidateDoe instanceof DomOrExceptionGenerationBase)) {
        return false;
      }

      try {
        candidateDoe.parse(true); //required for isSuperClass
      } catch (XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException | XPRC_MDMDeploymentException e) {
        return false;
      }

      if (candidateDoe.isAbstract()) { //might need to parse candidateDoe first
        return false;
      }

      if (!DomOrExceptionGenerationBase.isSuperClass((DomOrExceptionGenerationBase) fieldDoe,
                                                     (DomOrExceptionGenerationBase) candidateDoe)) {
        return false; //cannot assign
      }
    }

    return true;
  }


  private static class VariableToRemove {

    private AVariable parent;
    private AVariable varToRemove;


    public VariableToRemove(AVariable parent, AVariable varToRemove) {
      this.parent = parent;
      this.varToRemove = varToRemove;
    }


    public AVariable getParent() {
      return parent;
    }


    public AVariable getVarToRemove() {
      return varToRemove;
    }
  }
}
