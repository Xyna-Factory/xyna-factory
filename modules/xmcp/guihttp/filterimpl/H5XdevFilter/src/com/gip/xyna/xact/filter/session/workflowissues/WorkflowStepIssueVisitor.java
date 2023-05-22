/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

package com.gip.xyna.xact.filter.session.workflowissues;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.filter.session.XMOMLoader;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.StepMap.CommonStepVisitor;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.ExpressionUtils;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceResolutionContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.xfractwfe.formula.BaseType;
import com.gip.xyna.xprc.xfractwfe.formula.Expression;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression;
import com.gip.xyna.xprc.xfractwfe.formula.Functions;
import com.gip.xyna.xprc.xfractwfe.formula.LiteralExpression;
import com.gip.xyna.xprc.xfractwfe.formula.Not;
import com.gip.xyna.xprc.xfractwfe.formula.Operator;
import com.gip.xyna.xprc.xfractwfe.formula.SupportedFunctionStore;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;
import com.gip.xyna.xprc.xfractwfe.formula.VariableInstanceFunctionIncovation;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.CaseInfo;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.EmptyVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.InferOriginalTypeVisitor;
import xmcp.processmodeller.datatypes.Issue;
import com.gip.xyna.xnwh.persistence.xmom.PersistenceExpressionVisitors.QueryFunctionStore;



public class WorkflowStepIssueVisitor extends CommonStepVisitor {

  private List<Issue> collectedIssues;
  private XMOMLoader loader;

  public WorkflowStepIssueVisitor(XMOMLoader loader) {
    collectedIssues = new ArrayList<Issue>();
    this.loader = loader;
  }


  public List<Issue> getCollectedIssues() {
    return collectedIssues;
  }


  @Override
  public void visit(Step step) {
  }

  @Override
  public void visitStepMapping(StepMapping step) {
    String stepId = ObjectId.createStepId(step).getObjectId();
    boolean conditionalMapping = step.isConditionMapping();

    if (!step.isTemplateMapping()) {
      //INVALID_FORMULA
      checkMappingFormula(step);
    }

    //PROTOTYPEVARIABLE
    checkPrototypeVariable(step, step.getStepId());

    //ABSTRACT_CONSTANT
    checkGeneralStepAbstractConstant(step, step.getInputConnections(), step.getStepId(), VarUsageType.input);

    //OBJECTS_AFTER_BLOCKER
    if (!conditionalMapping) {
      checkObjectsAfterBlocker(step, stepId);
    }
  }

  
  private void checkMappingFormula(StepMapping step) {
    String stepId = ObjectId.createStepId(step).getObjectId();
    if (step.isConditionMapping()) {
      Step stepFunction = getQueryFunctionStep(step);
      stepId = stepFunction.getStepId();
      String outputId = stepFunction.getOutputVarIds()[0];

      //INVALID_FORMULA
      if(step.getRawExpressions() != null && step.getRawExpressions().size() > 0) {
        List<String> formulas = QueryUtils.extractQueryFormulas(step.getRawExpressions().get(0), step.getInputVars().size());
        for (int i = 0; i < formulas.size(); i++) {
          String formula = formulas.get(i);
          formula = ExpressionUtils.unescapeQueryExpression(formula);
          String id = ObjectId.createFilterCriterionId(stepId, VarUsageType.input, i);
          checkValidQueryFormula(step, formula, id, outputId);
        }
      }
    } else {
      //INVALID_FORMULA
      int formulaCount = step.getFormulaCount();
      for (int i = 0; i < formulaCount; i++) {
        String formula = step.getFormula(i);
        String id = ObjectId.createFormulaId(step.getStepId(), VarUsageType.input, i);
        checkInvalidAssignmentFormula(step, formula, id);
      }
    }   
  }


  private Step getQueryFunctionStep(StepMapping step) {
    List<Step> steps = step.getParentStep().getChildSteps();
    StepCatch stepCatch= (StepCatch) steps.get(steps.indexOf(step) + 1); //StepFunction (query) after this step
    Step result = stepCatch.getStepInTryBlock();
    return result;
  }


  @Override
  public void visitStepFunction(StepFunction step) {

    String stepId = step.getStepId();

    //PROTOTYPE_STEP
    if (step.isPrototype()) {
      String id = ObjectId.createStepId(step).getObjectId();
      Issue issue = new Issue.Builder().id(id).messageCode(WorkflowIssueMessageCode.PROTOTYPE_STEP).instance();
      collectedIssues.add(issue);
    }

    //ABSTRACT_CONSTANT
    checkGeneralStepAbstractConstant(step, step.getInputConnections(), step.getStepId(), VarUsageType.input);

    //PROTOTYPEVARIABLE
    if (!step.isPrototype()) {
      List<AVariable> varList = Utils.getServiceInputVars(step);
      checkVarListForPrototypes(varList, VarUsageType.input, stepId);
      varList = Utils.getServiceOutputVariables(step);
      checkVarListForPrototypes(varList, VarUsageType.output, stepId);
    }

    //OBJECTS_AFTER_BLOCKER
    checkObjectsAfterBlocker(step.getProxyForCatch(), stepId);

    //INVALID_ORDER_INPUT_SOURCE
    if (!step.isPrototype()) {
      checkInvalidOrderInputSource(step, ObjectId.createId(ObjectType.orderInputSource, stepId));
    }
  }


  @Override
  public void visitStepRetry(StepRetry step) {

    String stepId = ObjectId.createStepId(step).getObjectId();

    //ABSTRACT_CONSTANT
    checkGeneralStepAbstractConstant(step, step.getInputConnections(), stepId, VarUsageType.input);

    //OBJECTS_AFTER_BLOCKER
    checkObjectsAfterBlocker(step, stepId);

    //RETRY_AT_INVALID_POSITION
    checkRetryAtInvalidPosition(step, stepId);
  }


  @Override
  public void visitStepChoice(StepChoice step) {

    if(step.getDistinctionType().equals(DistinctionType.TypeChoice)) {
      return; //nothing applies to TypeChoice
    }
    
    String stepId = ObjectId.createStepId(step).getObjectId();

    //INVALID_FORMULA
    int formulaCount = step.getFormulaCount();
    for (int i = 0; i < formulaCount; i++) {
      String id = ObjectId.createFormulaId(step.getStepId(), VarUsageType.input, i);
      
      //there is only one outer formula and it is evaluated alongside the cases
      if(step.getDistinctionType().equals(DistinctionType.ConditionalBranch)) {
        //check cases
        for(int j=0; j<step.getChildSteps().size(); j++) {
          CaseInfo ci = step.getCaseInfo(j);
          if(ci.isDefault()) {
            continue;
          }
          id = ObjectId.createCaseId(step.getStepId(), "" + j);
          checkInvalidBooleanFormula(step, ci.getComplexName(), id);
        }
      } else {
        checkInvalidBooleanFormula(step, step.getFormula(i), id);
      }
    }

    if (step.getDistinctionType() != DistinctionType.ConditionalBranch) {
      return;
    }

    //only apply to userOutput in conditional Branchings
    //ABSTRACT_CONSTANT
    List<AVariable> outputs = step.getUserdefinedOutput();
    for (int i = 0; i < outputs.size(); i++) {
      AVariable avar = outputs.get(i);
      String id = ObjectId.createVariableId(stepId, VarUsageType.output, i);
      //ABSTRACT_CONSTANT
      checkAbstractConstant(avar, id);
    }

    //PROTOTYPE_VARIABLE
    checkVarListForPrototypes(outputs, VarUsageType.output, stepId);


  }


  @Override
  public void visitScopeStep(ScopeStep step) {
    if (!(step instanceof WFStep)) {
      return;
    }
    
    String stepId = ""; //-> empty for wf

    //PROTOTYPE_VARIABLE
    checkPrototypeVariable(step, stepId);

    //ABSTRACT_CONSTANT -> in output
    StepAssign assignStep = step.getChildStep().findFirstAssign();
    checkGeneralStepAbstractConstant(assignStep, assignStep.getInputConnections(), stepId, VarUsageType.output);
  }


  @Override
  public void visitStepThrow(StepThrow step) {
    String stepId = step.getStepId();

    //ABSTRACT_CONSTANT
    checkGeneralStepAbstractConstant(step, step.getInputConnections(), stepId, VarUsageType.input);

    //OBJECTS_AFTER_BLOCKER
    checkObjectsAfterBlocker(step, stepId);
  }


  private void checkInvalidOrderInputSource(StepFunction step, String stepId) {
    String orderInputSourceRef = step.getOrderInputSourceRef();
    if (orderInputSourceRef == null || orderInputSourceRef.length() == 0) {
      return;
    }

    try {
      SearchRequestBean searchRequest = new SearchRequestBean();
      searchRequest.setArchiveIdentifier(ArchiveIdentifier.orderInputSource);
      Map<String, String> filterEntries = new HashMap<String, String>();
      
      filterEntries.put("name", orderInputSourceRef);
      
      searchRequest.setFilterEntries(filterEntries);
      searchRequest.setSelection("*");
      searchRequest.setMaxRows(1);
      SearchResult<?> result = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement()
      .searchInputSources(searchRequest);
      
      if(result.getResult().size() != 1){
        Issue issue = new Issue.Builder().id(stepId).messageCode(WorkflowIssueMessageCode.INVALID_ORDER_INPUT_SOURCE).instance();
        collectedIssues.add(issue);
        return;
      }
      
      OrderInputSourceStorable ois = (OrderInputSourceStorable) result.getResult().get(0);
      if (ois != null && ois.getState() != null && ois.getState().equals("INVALID")) {
        Issue issue = new Issue.Builder().id(stepId).messageCode(WorkflowIssueMessageCode.INVALID_ORDER_INPUT_SOURCE).instance();
        collectedIssues.add(issue);
      }
    } catch (PersistenceLayerException | XNWH_SelectParserException | XNWH_InvalidSelectStatementException e) {

    }
  }


  private void checkRetryAtInvalidPosition(StepRetry step, String stepId) {
    boolean validPosition = isInsideStepCatch(step, 0);
    if (validPosition) {
      return;
    }

    Issue issue = new Issue.Builder().id(stepId).messageCode(WorkflowIssueMessageCode.RETRY_AT_INVALID_POSITION).instance();
    collectedIssues.add(issue);
  }


  private void checkGeneralStepAbstractConstant(Step step, InputConnections ic, String stepId, VarUsageType vut) {
    for (int i = 0; i < ic.length(); i++) {
      if (ic.getConstantConnected()[i]) {
        String id = ObjectId.createVariableId(stepId, vut, i);
        AVariable av = null;
        try {
          av = step.getParentScope().identifyVariable(ic.getVarIds()[i]).getVariable();
        } catch (XPRC_InvalidVariableIdException e) {
          continue;
        }
        checkAbstractConstant(av, id);
      }
    }
  }

  
  private void checkValidQueryFormula(Step stepMapping, String formula, String id, String queryOutputId) {
    StepBasedIdentificationNoPath identification = new StepBasedIdentificationNoPath(stepMapping, loader);
    identification.getIds().add(0, queryOutputId); // add Query Output as %0%
    Optional<ModelledExpression> ex = getValidFormulaWithIdentification(stepMapping, formula, false, identification);
    if (ex.isEmpty() || !isBooleanExpression(stepMapping, ex.get())) {
      addInvalidFormularIssue(id);
    }
  }

  //-> Formula that assigns a value
  private void checkInvalidAssignmentFormula(Step step, String formula, String id) {
    Optional<ModelledExpression> ex = getValidFormula(step, formula, true);
    if (ex.isEmpty() || !isAssignmentExpression(step, ex.get())) {
      addInvalidFormularIssue(id);
    }
  }


  private void checkInvalidBooleanFormula(Step step, String formula, String id) {
    Optional<ModelledExpression> ex = getValidFormula(step, formula, false);
    if (ex.isEmpty() || !isBooleanExpression(step, ex.get())) {
      addInvalidFormularIssue(id);
    }
  }
  
  
  private void checkObjectsAfterBlocker(Step step, String stepId) {
    List<Step> steps = getStepsAround(step);
    int index = steps.indexOf(step);
    for (int i = 0; i < index; i++) {
      if (isBlockerStep(steps.get(i))) {
        Issue issue = new Issue.Builder().id(stepId).messageCode(WorkflowIssueMessageCode.OBJECTS_AFTER_BLOCKER).instance();
        collectedIssues.add(issue);
        return;
      }
    }
  }


  private List<Step> getStepsAround(Step step) {
    if (step.getParentStep() == null || !(step.getParentStep() instanceof StepSerial)) {
      return Collections.emptyList();
    }

    return step.getParentStep().getChildSteps();
  }

  private void checkPrototypeVariable(Step step, String stepId) {
    List<AVariable> varList = step.getInputVars();
    checkVarListForPrototypes(varList, VarUsageType.input, stepId);
    varList = step.getOutputVars();
    checkVarListForPrototypes(varList, VarUsageType.output, stepId);
  }


  private void checkVarListForPrototypes(List<AVariable> varList, VarUsageType varUsageType, String stepId) {
    for (int i = 0; i < varList.size(); i++) {
      if (!varList.get(i).isPrototype()) {
        continue;
      }
      String id = ObjectId.createVariableId(stepId, varUsageType, i);
      Issue issue = new Issue.Builder().id(id).messageCode(WorkflowIssueMessageCode.PROTOTYPE_VARIABLE).instance();
      collectedIssues.add(issue);
    }
  }


  private void checkAbstractConstant(AVariable var, String id) {
    if(var.isList()) {
      return; //TODO: check member of List
    }
    if (!var.getDomOrExceptionObject().isAbstract()) {
      return;
    }
    Issue issue = new Issue.Builder().id(id).messageCode(WorkflowIssueMessageCode.ABSTRACT_CONSTANT).instance();
    collectedIssues.add(issue);
  }


  private void addInvalidFormularIssue(String id) {
    Issue issue = new Issue.Builder().id(id).messageCode(WorkflowIssueMessageCode.INVALID_FORMULA).instance();
    collectedIssues.add(issue);
  }


  private DeploymentItemState getOrCreateDeploymentItemstate(Step step) {
    DeploymentItemState dis = null;
    DeploymentItemStateManagement dism =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    DeploymentItemRegistry registry = dism.getRegistry(step.getCreator().getRevision());
    dis = registry.get(step.getCreator().getOriginalFqName());

    if (dis == null) { //workflow not saved
      DeploymentItem di = new DeploymentItem(step.getCreator().getOriginalFqName(), XMOMType.WORKFLOW);
      dis = new DeploymentItemStateImpl(di, registry);
    }

    return dis;
  }


  private Optional<ModelledExpression> getValidFormula(Step step, String formular, boolean assign) {
    return getValidFormulaWithIdentification(step, formular, assign, new StepBasedIdentificationNoPath(step, loader));
  }

  private Optional<ModelledExpression> getValidFormulaWithIdentification(Step step, String formular, boolean assign, VariableContextIdentification identification){
    DeploymentItemState dis = getOrCreateDeploymentItemstate(step);
    try {
      InterfaceResolutionContext.updateCtx(DeploymentLocation.SAVED, dis);
      SupportedFunctionStore functionStore = getFunctionStore(step);
      ModelledExpression me = ModelledExpression.parse(identification, formular, functionStore); 
      ModelledExpressionManagement mem = new ModelledExpressionManagement();
      
      if (!mem.areFunctionsInAssignmentExpressionValid(me, step, identification, assign)) {
        return Optional.empty();
      }

      return Optional.of(me);
    } catch (XPRC_ParsingModelledExpressionException | RuntimeException e) {
      return Optional.empty();
    } finally {
      InterfaceResolutionContext.revertCtx();
    }  
  }


  private SupportedFunctionStore getFunctionStore(Step step) {
    if(step instanceof StepMapping) {
      StepMapping mapping = (StepMapping)step;
      if(mapping.isConditionMapping()) {
        return new QueryFunctionStore();
      }
    }
    return new Functions();
  }


  private boolean isBlockerStep(Step step) {
    return step instanceof StepThrow || step instanceof StepRetry;
  }


  private boolean isInsideStepCatch(Step step, int depth) {
    Step parentStep = step.getParentStep();
    if (parentStep == null) {
      return false;
    }

    if (parentStep instanceof StepCatch) {
      return true;
    }

    if (depth > 1000) {
      throw new RuntimeException("loop detected. Step: " + step.getStepId());
    }

    return isInsideStepCatch(parentStep, depth + 1);
  }


  private boolean isAssignmentExpression(Step step, ModelledExpression exp) {
    GetTypeVisitor gtv = new GetTypeVisitor();
    exp.visitSourceExpression(gtv);    
    return gtv.isVarAssignment();
  }


  private boolean isBooleanExpression(Step step, ModelledExpression exp) {

    if (isAssignmentExpression(step, exp)) {
      return false;
    }

    InferOriginalTypeVisitor visitor = new InferOriginalTypeVisitor();
    exp.visitTargetExpression(visitor);
    try {
      TypeInfo targetType = exp.getTargetType();
      BaseType baseType = targetType.getBaseType();
      return baseType == BaseType.BOOLEAN_PRIMITIVE || baseType == BaseType.BOOLEAN_OBJECT;
    } catch (XPRC_InvalidVariableMemberNameException e) {
      return false;
    }
  }


  private static class GetTypeVisitor extends EmptyVisitor {

    private boolean isVarAssignment = false;


    public boolean isVarAssignment() {
      return isVarAssignment;
    }
    
    public void functionStarts(FunctionExpression fe) { isVarAssignment = true; }

    public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi) { isVarAssignment = true; }
    
    public void instanceFunctionSubExpressionStarts(Expression fe, int parameterIndex) { isVarAssignment = true; }
    
    public void literalExpression(LiteralExpression expression) { isVarAssignment = true; }

    public void notStarts(Not not) { isVarAssignment = true; }
    
    public void operator(Operator operator) { isVarAssignment = true; }

    public void variableStarts(Variable variable) { isVarAssignment = true; }
    
    public void variablePartStarts(VariableAccessPart part) { isVarAssignment = true; }
    
    public void indexDefStarts() {}
  }

}
