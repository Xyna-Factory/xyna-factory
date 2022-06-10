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
package com.gip.xyna.xact.filter.xmom.workflows.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonParserUtils;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.session.View;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectId.ObjectPart;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.xmom.MetaXmomContainers;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.BranchInfo;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.CaseInfo;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;

import xmcp.processmodeller.datatypes.ContentArea;
import xmcp.processmodeller.datatypes.Data;
import xmcp.processmodeller.datatypes.Formula;
import xmcp.processmodeller.datatypes.FormulaArea;
import xmcp.processmodeller.datatypes.Item;
import xmcp.processmodeller.datatypes.ItemBarArea;
import xmcp.processmodeller.datatypes.Variable;
import xmcp.processmodeller.datatypes.distinction.Branch;
import xmcp.processmodeller.datatypes.distinction.Case;
import xmcp.processmodeller.datatypes.distinction.CaseArea;
import xmcp.processmodeller.datatypes.distinction.ConditionalBranching;
import xmcp.processmodeller.datatypes.distinction.ConditionalChoice;
import xmcp.processmodeller.datatypes.distinction.Distinction;
import xmcp.processmodeller.datatypes.distinction.ExceptionHandling;
import xmcp.processmodeller.datatypes.distinction.TypeChoice;
import xmcp.processmodeller.datatypes.exception.Exception;

public class DistinctionJson extends XMOMGuiJson implements HasXoRepresentation {

  private View view;
  private ObjectId distinctionId;
  private IdentifiedVariables identifiedVariables;
  private boolean request;
  private String condition;
  private DistinctionType distinctionType;
  private List<BranchJson> branches;
  private List<BranchInfo> branchInfos;
  private List<CaseInfo> handledCases;
  private List<CaseInfo> unhandledCases;


  public DistinctionJson() {
    this.request = true;
  }

  public DistinctionJson(View view, StepChoice stepChoice) {
    this(view, ObjectId.createStepId(stepChoice), stepChoice.getOuterConditionForGUI(), stepChoice.getDistinctionType(),
         stepChoice.getBranchesForGUI(), stepChoice.getHandledCases(), stepChoice.getUnhandledCases(true));
  }

  public DistinctionJson(View view, ObjectId distinctionId, String condition, DistinctionType distinctionType, List<BranchInfo> branchInfos, List<CaseInfo> handledCases, List<CaseInfo> unhandledCases) {
    this.request = false;
    this.view = view;
    this.distinctionId = distinctionId;
    this.identifiedVariables = view.getGenerationBaseObject().identifyVariables(distinctionId);
    this.distinctionType = distinctionType;
    this.branchInfos = branchInfos;
    this.handledCases = handledCases;
    this.unhandledCases = unhandledCases;
    this.branches = createBranches();

    if (distinctionType == DistinctionType.TypeChoice) {
      this.condition = Tags.EXPRESSION_PARAMETER_0;
    } else {
      this.condition = condition;
    }
  }

  public DistinctionType getDistinctionType() {
    return distinctionType;
  }
  
  public void setDistinctionType(DistinctionType distinctionType) {
    this.distinctionType = distinctionType;
  }
  
  
  public void setView(View view) {
    this.view = view;
  }


  private List<BranchJson> createBranches() {
    List<BranchJson> cs = new ArrayList<>();
    for (int branchNo = 0; branchNo < branchInfos.size(); branchNo++) {
      BranchInfo branch = branchInfos.get(branchNo);
      cs.add(new BranchJson(view, distinctionId, distinctionType, branchNo, branch.getMainStep(), branch.getCases(), handledCases, identifiedVariables));
    }

    return cs;
  }
  
  @Override
  public GeneralXynaObject getXoRepresentation() {
    Distinction distinction;
    switch(distinctionType) {
      case ConditionalChoice:
        distinction = new ConditionalChoice();
        break;
      case ConditionalBranch:
        distinction = new ConditionalBranching();
        break;
      case TypeChoice:
        distinction = new TypeChoice();
        break;
      default:
        distinction = new ExceptionHandling();
        break;
    }
    if(!request) { // TODO: Was ist das?
      distinction.setId(distinctionId.getObjectId());
    }

    if(distinctionType != DistinctionType.ExceptionHandling) {
      String[] additionalItemTypes = new String[] {};
      if(distinctionType == DistinctionType.TypeChoice) {
        additionalItemTypes = new String[] {MetaXmomContainers.DATA_FQN, MetaXmomContainers.EXCEPTION_FQN};
      }

      boolean isFormulasReadonly = (distinctionType == DistinctionType.TypeChoice);
      distinction.addToAreas(ServiceUtils.createFormulaArea(view.getGenerationBaseObject(), distinctionId, Arrays.asList(condition),
                                                            Tags.CHOICE_INPUT, identifiedVariables, false, isFormulasReadonly,
                                                            Arrays.asList(VarUsageType.input), additionalItemTypes));
    }
    ContentArea a = new ContentArea();
    a.setId(ObjectId.createId(ObjectType.branchArea, distinctionId.getBaseId()));
    a.setName(Tags.CONTENT);
    a.setReadonly(true);
//    a.addToItemTypes(MetaXmomContainers.BRANCH_FQN);
    if(branches != null && !branches.isEmpty()) {
      for (BranchJson branchJson : branches) {
        a.addToItems((Item) branchJson.getXoRepresentation());
      }
    }  else {
      a.setItems(Collections.emptyList());
    }
    distinction.addToAreas(a);
    if(distinctionType == DistinctionType.ExceptionHandling) {
      // currently only for exception handling (later maybe also for type choices)
      distinction.setDeletable(false);
      ItemBarArea area = new ItemBarArea();
      area.addToItemTypes(MetaXmomContainers.DATA_FQN);
      area.addToItemTypes(MetaXmomContainers.EXCEPTION_FQN);
      area.setName(Tags.UNHANDLED_EXCEPTIONS);
      for (CaseInfo unhandledCase : unhandledCases) {
        Exception ex = new Exception();
        ex.setLabel(unhandledCase.getName());
        ex.setFqn(unhandledCase.getComplexName());
        area.addToItems(ex);
      }
      if(area.getItems() == null) {
        area.setItems(Collections.emptyList());
      }
      distinction.addToAreas(area);
    } else if(!request) {
      distinction.setDeletable(true);
      boolean readonly = distinctionType != DistinctionType.ConditionalBranch;
      distinction.addToAreas(ServiceUtils.createVariableArea(
                       view.getGenerationBaseObject(), ObjectId.createId(ObjectType.step, distinctionId.getBaseId(), ObjectPart.output), VarUsageType.output, 
                       identifiedVariables, Tags.CASE_OUTPUT, 
                       new String[] { MetaXmomContainers.DATA_FQN, MetaXmomContainers.EXCEPTION_FQN }, readonly));
    }

    return distinction;
  }
  
  public String getCondition() {
    return condition;
  }
  
  private static class BranchJson implements HasXoRepresentation {

    private View view;
    private DistinctionType distinctionType;
    private WorkflowStepVisitor workflowStepVisitor;
    private List<CaseInfo> casesInBranchInfo;
    private List<CaseInfo> caseInfos;
    private IdentifiedVariables identifiedVariables;
    private String branchNo;
    private ObjectId distinctionId;

    public BranchJson(View view, ObjectId distinctionId, DistinctionType distinctionType, int branchNo, Step executedStep, List<CaseInfo> casesInBranchInfo, List<CaseInfo> caseInfos, IdentifiedVariables identifiedVariables) {
      this.view = view;
      this.distinctionType = distinctionType;
      this.workflowStepVisitor = new WorkflowStepVisitor(view, executedStep);
      this.casesInBranchInfo = casesInBranchInfo;
      this.caseInfos = caseInfos;
      this.identifiedVariables = identifiedVariables;
      this.branchNo = String.valueOf(branchNo);
      this.distinctionId = distinctionId;
    }
    
    @Override
    public GeneralXynaObject getXoRepresentation() {
      Branch branch = new Branch();
      branch.setId(ObjectId.createBranchId(distinctionId.getBaseId(), branchNo));
      branch.addToAreas(createCaseArea());
      
      if (distinctionType == DistinctionType.ExceptionHandling) {
        branch.addToAreas(ServiceUtils.createContentArea(workflowStepVisitor.createWorkflowSteps(), ObjectId.createStepId(workflowStepVisitor.getStep()).getObjectId(), Tags.CONTENT, false, MetaXmomContainers.RETRY_FQN));
        branch.setDeletable(true);
      } else {
        branch.addToAreas(ServiceUtils.createContentArea(workflowStepVisitor.createWorkflowSteps(), ObjectId.createStepId(workflowStepVisitor.getStep()).getObjectId(), Tags.CONTENT, false));
        
        if (distinctionType == DistinctionType.ConditionalChoice) {
          branch.setDeletable(false);
        } else if (distinctionType == DistinctionType.TypeChoice) {
          branch.setDeletable(true);
        } else {
          // conditional branching
          if (hasDefaultCase()) {
            branch.setDeletable(false);
          } else {
            branch.setDeletable(true);
          }
        }
      }
      return branch;
    }
    
    private boolean hasDefaultCase() {
      if (caseInfos == null) {
        return false;
      }
      
      for (CaseInfo caseInfo : casesInBranchInfo) {
        if (caseInfo.isDefault()) {
          return true;
        }
      }
      
      return false;
    }
    
    private CaseArea createCaseArea() {
      CaseArea ca = new CaseArea();
      ca.setName(Tags.CASE_INPUT);
      ca.addToItemTypes(MetaXmomContainers.CASE_FQN);
      ca.setId(ObjectId.createCaseAreaId(distinctionId.getBaseId(), branchNo));
      
      for(int i=0; i< casesInBranchInfo.size(); i++) {
        CaseInfo caseInfo = casesInBranchInfo.get(i);
        int caseNoInt = caseInfos.indexOf(caseInfo);
        
        if(caseNoInt == -1) {
          caseNoInt = i;
        }
        
        ca.addToItems(createCase(caseInfo, caseNoInt));
      }
      
      if(ca.getItems() == null) {
        ca.setItems(Collections.emptyList());
      }
      return ca;
    }
    
    private Case createCase(CaseInfo caseInfo, int caseNo) {
      Case distinctionCase = new Case();
      String caseNoStr = String.valueOf(caseNo);
      String caseId = ObjectId.createCaseId(distinctionId.getBaseId(), caseNoStr);
      distinctionCase.setId(caseId);

      Formula condition = new Formula();
      condition.setId(caseId);
      FormulaArea formulaArea = new FormulaArea(null, true, Tags.CASE_FORMULA_AREA, new ArrayList<>(), new ArrayList<>());
      formulaArea.addToItems(condition);
      distinctionCase.addToAreas(formulaArea);

      int inputsCount = identifiedVariables.getVariables(VarUsageType.input).size();
      String fakeVarId = ObjectId.createVariableId(distinctionId.getBaseId(), VarUsageType.input, caseNo+inputsCount);

      if (distinctionType == DistinctionType.ConditionalChoice) {
        condition.setExpression(Tags.EXPRESSION_PARAMETER_0);
        condition.addToInput(createFakeVariableData(fakeVarId, caseInfo.getName(), Tags.BOOLEAN));
        distinctionCase.setReadonly(true);
        condition.setReadonly(true);
      } else if (distinctionType == DistinctionType.ConditionalBranch) {
        distinctionCase.setLabel(caseInfo.getName());
        distinctionCase.setIsDefault(caseInfo.isDefault());
        if (!caseInfo.isDefault()) {
          distinctionCase.setMergeGroup(distinctionId.getObjectId());
        }

        if (!caseInfo.isDefault()) {
          condition.setExpression(caseInfo.getGuiName());
        }

        List<VariableJson> variables = VariableJson.toList(identifiedVariables, VarUsageType.input, view.getGenerationBaseObject(), null);
        for (VariableJson variableJson : variables) {
          condition.addToInput((Variable) variableJson.getXoRepresentation());
        }
      } else if (distinctionType == DistinctionType.TypeChoice) {
        condition.setExpression(Tags.EXPRESSION_PARAMETER_0);
        distinctionCase.setReadonly(true);
        condition.setReadonly(true);
        distinctionCase.setMergeGroup(distinctionId.getObjectId());

        if (identifiedVariables.getVariable(VarUsageType.input, 0).getIdentifiedVariable() instanceof ExceptionVariable) {
          condition.addToInput(createFakeVariableException(fakeVarId, caseInfo.getName(), caseInfo.getComplexName()));
        } else {
          condition.addToInput(createFakeVariableData(fakeVarId, caseInfo.getName(), caseInfo.getComplexName()));
        }
      } else { // exception handling
        condition.setExpression(Tags.EXPRESSION_PARAMETER_0);
        condition.addToInput(createFakeVariableException(fakeVarId, caseInfo.getName(), caseInfo.getComplexName()));
        distinctionCase.setReadonly(true);
        condition.setReadonly(true);
      }

      if (condition.getInput() == null) {
        condition.setInput(Collections.emptyList());
      }

      return distinctionCase;
    }
    
    private Variable createFakeVariableData(String id, String label, String fqn) {
      Data d = new Data();
      d.setId(id);
      d.setLabel(label);
      d.setFqn(fqn);
      d.setDeletable(false);
      d.setReadonly(true);

      return d;
    }
    
    private Variable createFakeVariableException(String id, String label, String fqn) {
      Exception e = new Exception();
      e.setId(id);
      e.setLabel(label);
      e.setFqn(fqn);
      e.setDeletable(false);
      e.setReadonly(true);

      return e;
    }

  }

  public static class DistinctionJsonVisitor extends EmptyJsonVisitor<DistinctionJson> {
    private static List<String> usedTags = Arrays.asList(Tags.TYPE, Tags.VARIABLES, Tags.CONTENT, Tags.AREAS, Tags.AREA_TYPE, Tags.CHOICE_INPUT, Tags.CHOICE_CASES, Tags.CASES);
    DistinctionJson cj = new DistinctionJson();

    @Override
    public DistinctionJson get() {
      return cj;
    }
    @Override
    public DistinctionJson getAndReset() {
      DistinctionJson ret = cj;
      cj = new DistinctionJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if( label.equals(Tags.TYPE) ) {
        if (value.equals(Tags.CONDITIONAL_CHOICE)) {
          cj.distinctionType = DistinctionType.ConditionalChoice;
        } else if (value.equals(Tags.CONDITIONAL_BRANCHING)) {
          cj.distinctionType = DistinctionType.ConditionalBranch;
        } else if (value.equals(Tags.TYPE_CHOICE)) {
          cj.distinctionType = DistinctionType.TypeChoice;
        } else {
          throw new UnexpectedJSONContentException(label + ": " + value + ", expected: " + Tags.CONDITIONAL_CHOICE + " or " + Tags.CONDITIONAL_BRANCHING);
        }
      }

      if( label.equals(Tags.READONLY) ) {
        //ignore
        return;
      }

      if( label.equals(Tags.CHOICE_CONDITION) ) {
        cj.condition = value;//ignore
        return;
      }

      JsonParserUtils.checkAllowedLabels(usedTags, label);
    }

    @Override
    public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
      JsonParserUtils.checkAllowedLabels(usedTags, label);
      return null;
    }
    
    @Override
    public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {
      JsonParserUtils.checkAllowedLabels(usedTags, label);
    }
  }

}
