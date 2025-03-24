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
package com.gip.xyna.xact.filter.session.modify.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.ModificationNotAllowedException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.exceptions.UnsupportedOperationException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Case;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.DTMetaTag;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.FormulaInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberMethodInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberVarInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.QueryFilterCriterion;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.QuerySelectionMask;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.QuerySortCriterion;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Variable;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepFunction;
import com.gip.xyna.xact.filter.session.workflowwarnings.FormulaChangeNotification;
import com.gip.xyna.xact.filter.session.workflowwarnings.WorkflowWarningsHandler;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.ThrowExceptionIdProvider;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.ExpressionUtils;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.util.xo.Util;
import com.gip.xyna.xact.filter.xmom.workflows.enums.GuiLabels;
import com.gip.xyna.xact.filter.xmom.workflows.json.ChangeJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ServiceUtils;
import com.gip.xyna.xmcp.SharedLib;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.UnsupportedJavaTypeException;
import com.gip.xyna.xprc.xfractwfe.generation.CodeOperation;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.HasDocumentation;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;
import com.gip.xyna.xprc.xfractwfe.generation.PythonOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCallInService;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xnwh.persistence.QueryParameter;
import xnwh.persistence.SelectionMask;
import xnwh.persistence.Storable;

public class ChangeOperation extends ModifyOperationBase<ChangeJson> {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(ChangeOperation.class);
  
  private ChangeJson change;
  
  @Override
  public int parseRequest(String jsonRequest) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();
    change = jp.parse(jsonRequest, ChangeJson.getJsonVisitor());
    
    return change.getRevision();
  }

  @Override
  protected void modifyStep(Step step) {
    String label = change.getText();
    if(label != null ) {
      step.setLabel(label);
    } else if( ( change.getExpression() != null ) && (step instanceof StepChoice) ) {
      StepChoice stepChoice = (StepChoice)step;
      stepChoice.replaceOuterCondition( change.getExpression() );
    }
    if (step instanceof StepFunction) {
      StepFunction stepFunction = (StepFunction)step;
      if(change.getFreeCapacities() != null && stepFunction.isFreeCapacitiesTaggable()) {
        stepFunction.setFreesCapacities(change.getFreeCapacities());
      }
      if(change.getDetachable() != null && stepFunction.isDetachedTaggable()) {
        stepFunction.setExecutionDetached(change.getDetachable());
      }
      if(change.isOverrideCompensation() != null) {
        object.getRoot().getStepMap().removeStep(step);
        stepFunction.setOverrideCompensation(change.isOverrideCompensation());
        object.getRoot().getStepMap().addStep(step);
      }
      if(change.getLimitResults() != null) {
        QueryParameter queryParameter = QueryUtils.getQueryParameter(object);
        if(queryParameter != null) {
          queryParameter.setMaxObjects(change.getLimitResults());
          QueryUtils.saveQueryParamater(object, modification.getObject().getDataflow(), queryParameter);
        }
      }
      if(change.getQueryHistory() != null) {
        QueryParameter queryParameter = QueryUtils.getQueryParameter(object);
        if(queryParameter != null) {
          queryParameter.setQueryHistory(change.getQueryHistory());
          QueryUtils.saveQueryParamater(object, modification.getObject().getDataflow(), queryParameter);
        }
      }
    }
  }

  @Override
  protected void modifyWorkflow(WF workflow) {
    String label = change.getText();
    if( label != null ) {
      workflow.setLabel(label);
    } 
  }

  @Override
  protected void modifyVariable(Variable variable) throws UnsupportedOperationException, XynaException, ModificationNotAllowedException {
    String label = change.getLabel();
    if (label != null) {
      variable.getVariable().setLabel(label);
    }

    handleIsList(variable);

    handleChangeCast(variable);
  }
  
  
  private void handleChangeCast(Variable variable) throws UnsupportedOperationException, XynaException, ModificationNotAllowedException{
    String castToFqn = change.getCastToFqn();
    if( castToFqn == null ) {
      return;
    }
    
    if (isQuery(object)) {
      changeQuerySelectionMask(castToFqn);
      return;
    }
      
    if (!(variable.getIdentifiedVariables() instanceof IdentifiedVariablesStepFunction)) {
      throw new UnsupportedOperationException(UnsupportedOperationException.OPERATION_DYNAMIC_TYPING,
                                              UnsupportedOperationException.DYNAMIC_TYPING_ONLY_FOR_SERVICE_CALLS);
    }
    
    IdentifiedVariablesStepFunction identifiedVariables = (IdentifiedVariablesStepFunction) variable.getIdentifiedVariables();
    if (castToFqn.length() > 0) {
      String superFqn = identifiedVariables.getOriginalVarFqn(variable.getUsage(), variable.getIndex());
      String anyTypeFqn = GenerationBase.ANYTYPE_REFERENCE_PATH + "." + GenerationBase.ANYTYPE_REFERENCE_NAME;
      if (!anyTypeFqn.equals(superFqn)) {
        GenerationBaseObject superGbo = modification.load(FQNameJson.ofPathAndName(superFqn));
        GenerationBaseObject castToGbo = modification.load(FQNameJson.ofPathAndName(castToFqn));
        DomOrExceptionGenerationBase superType = (DomOrExceptionGenerationBase) superGbo.getGenerationBase();
        DomOrExceptionGenerationBase subType = (DomOrExceptionGenerationBase) castToGbo.getGenerationBase();
        if (subType.equals(superType) || (!isAnyType(variable) && !DomOrExceptionGenerationBase.isSuperClass(superType, subType))) {
          throw new UnsupportedOperationException(UnsupportedOperationException.OPERATION_DYNAMIC_TYPING,
                                                  UnsupportedOperationException.DYNAMIC_TYPING_WRONG_TYPE);
        }
      }
    }
    identifiedVariables.setVarCastToFqn(variable.getUsage(), variable.getIndex(), castToFqn.length() > 0 ? castToFqn : null);

    // in case variable is set to constant value it must be removed
    Dataflow dataflow = modification.getObject().getDataflow();
    if (dataflow != null) { // only for workflows
      dataflow.removeConstantValues(object, Optional.empty());
    }

  }
  

  @Override
  protected void modifyFormulaArea(Step step) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    if (step instanceof StepMapping) {
      StepMapping stepMapping = (StepMapping)step;
      stepMapping.sortFormulas();
      
      //update warnings - remove existing and check new
      FQName fqn = modification.getObject().getFQName();
      String objectId = ObjectId.createStepId(step).getObjectId();
      WorkflowWarningsHandler warningsHandler = modification.getSession().getWFWarningsHandler(fqn);
      warningsHandler.deleteAllWarnings(objectId);
      List<String> expressions = stepMapping.getRawExpressions();
      for (int i=0; i<expressions.size(); i++) {
        String expression = expressions.get(i);
        ObjectId formulaId = ObjectId.parse(ObjectId.createFormulaId(step.getStepId(), VarUsageType.input, i));
        FormulaChangeNotification notification = new FormulaChangeNotification(expression, stepMapping);
        modification.getSession().getWFWarningsHandler(fqn).handleChange(formulaId, notification);
      }
      
    }
  }

  private boolean isAnyType(Variable variable) {
    return variable.getVariable().getIdentifiedVariable().isJavaBaseType() &&
        variable.getVariable().getIdentifiedVariable().getJavaTypeEnum() == PrimitiveType.ANYTYPE;
  }
  
  private void handleIsList(Variable variable) throws XynaException {
    Boolean isList = change.isList();
    if( isList != null ) {
      Dataflow dataflow = modification.getObject().getDataflow();
      if (dataflow != null) { // only for workflows
        dataflow.removeConstantValues(object, Optional.empty());
      }

      variable.getVariable().getIdentifiedVariable().setIsList(isList);
      
      //update other variable
      if(!(variable.getVariable().idprovider instanceof ThrowExceptionIdProvider)) {
        String idToFind = variable.getVariable().idprovider.getId();
        
        if(idToFind.equals(variable.getVariable().getIdentifiedVariable().getId()))
          return;
        
        AVariable other = null;
        StepForeach parentSF = StepForeach.getParentStepForeachOrNull(object.getStep());
        //find variable with id == idToFind.
        //it could be in our ParentScope
        //or outputVarsSingle of our parent StepForeach
        if(parentSF == null) {
          try {
            other = object.getStep().getParentScope().identifyVariable(idToFind).getVariable();
          }
          catch(Exception e) {
            throw new RuntimeException("Variable not found");
          }         
        }
        else {
          List<AVariable> candidates = parentSF.getOutputVarsSingle(false);
          for(AVariable candidate : candidates) {
            if(candidate.getId() != null && candidate.getId().equals(idToFind)) {
              other = candidate;
              break;
            }
          }
        }
        
        if(other == null)
          throw new RuntimeException("could not find variable.");
        
        other.setIsList(isList);
      }
    }
  }
  
  
  @Override
  protected void modifyFormula(FormulaInfo formulaInfo) {
    Step step = object.getStep();
    if( step instanceof StepChoice ) {
      StepChoice stepChoice = (StepChoice)step;
      stepChoice.replaceOuterCondition( change.getExpression() );
      ExpressionUtils.cleanUpChoiceInputsAndConditions(stepChoice);

      //update Input/Output variables of Step
      object.getRoot().getVariableMap().refreshIdentifiedVariables(ObjectId.createStepId(step));
    } else if( step instanceof StepMapping ) {
      StepMapping stepMapping = (StepMapping)step;
      stepMapping.replaceFormula( formulaInfo.getIndex(), change.getExpression() );
      
      //if mapping is Template, we might need to update inputs as well
      if(stepMapping.isTemplateMapping()) {
        List<AVariable> inputsToRemove = getUnusedInputs(stepMapping.getInputVars(), change.getExpression());
        IdentifiedVariables idv = object.getRoot().getVariableMap().identifyVariables(ObjectId.createStepId(step));
        for(int i=0; i<inputsToRemove.size(); i++) {
          int index = stepMapping.getInputVars().indexOf(inputsToRemove.get(i));
          idv.getListAdapter(VarUsageType.input).remove(index);

          //update Input/Output variables of Step
          object.getRoot().getVariableMap().refreshIdentifiedVariables(ObjectId.createStepId(step));
        }
      }

      FQName fqn = modification.getObject().getFQName();
      FormulaChangeNotification notification = new FormulaChangeNotification(change.getExpression(), stepMapping);
      modification.getSession().getWFWarningsHandler(fqn).handleChange(object.getId(), notification);
    }
  }
  
  
  private List<AVariable> getUnusedInputs(List<AVariable> inputs, String expression){
    List<AVariable> result = new ArrayList<AVariable>();
    
    for(int i=0; i<inputs.size(); i++) {
      String searchString = String.format("%%%d%%", i);
      if(!expression.contains(searchString)) {
        result.add(inputs.get(i));
      }
    }
    
    
    return result;
  }

  @Override
  protected void modifyCase(Case caseInfo) {
    StepChoice stepChoice = (StepChoice)object.getStep();
    if (change.getExpression() != null) {
      stepChoice.replaceExpression(caseInfo.getIndex(), change.getExpression());
      ExpressionUtils.cleanUpChoiceInputsAndConditions(stepChoice);
    } else {
      stepChoice.decoupleCase(caseInfo.getIndex());
      object.getRoot().getStepMap().updateStep(stepChoice);
    }
  }

  @Override
  protected void modifyBranchArea(Step step) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    Distinction distinction;
    if (step instanceof StepFunction) {
      distinction = (Distinction)( ((StepFunction)step).getProxyForCatch() ); 
    } else if (step instanceof WFStep) {
      StepSerial stepContainer = ((WFStep)step).getChildStep();
      distinction = (Distinction)stepContainer.getProxyForCatch();
      object.getRoot().getStepMap().updateStep((Step)distinction);
    } else {
      distinction = ((Distinction)step);
    }

    int existingBranches = distinction.getBranchesForGUI().size();

    distinction.addMissingBranches();

    int totalBranches = distinction.getBranchesForGUI().size();
    int createdBranches = totalBranches - existingBranches;

    ObjectId parentStepId = ObjectId.createStepId(object.getStep());
    IdentifiedVariables identifiedVariables = modification.getObject().getVariableMap().identifyVariables(parentStepId);
    List<AVariableIdentification> outputVarIdents = identifiedVariables.getVariables(VarUsageType.output);
    for (int i = 0; i < createdBranches; i++) {
      for (AVariableIdentification outputVarIdent : outputVarIdents) {
        GBSubObject varSubObject = modification.getObject().getObject(outputVarIdent.internalGuiId.createId());
        modification.getObject().getDataflow().addConnectionForBranch(varSubObject, step);
      }
    }
    
    
    
    
    
    object.getRoot().getStepMap().updateStep((Step)distinction);
  }

  @Override
  protected void modifyDocumentation(HasDocumentation object) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    String documentation = change.getText();
    if (documentation != null) {
      object.setDocumentation(documentation);
//      addUpdate( new UpdateResponseJson(getObjectId(), new LabelJson(label) ) ); TODO
    }
  }

  @Override
  protected void modifyQueryFilter(QueryFilterCriterion queryFilterCriterion)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    if (object.getStep() instanceof StepFunction && change.getExpression() != null) {
      StepFunction stepFunction = (StepFunction) object.getStep();
      String expression = (change.getExpression() != null) ? change.getExpression() : "";
      stepFunction.getQueryFilterConditions().set(queryFilterCriterion.getIndex(), QueryUtils.escapeExpressionForXML(expression));
      QueryUtils.refreshQueryHelperMappingExpression(object);
    }
  }
  

  @Override
  protected void modifyQuerySorting(QuerySortCriterion querySortCriterion)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    QueryParameter queryParameter = QueryUtils.getQueryParameter(object);
    if (queryParameter != null) {
      try {
        if (queryParameter.getSortCriterion() != null) {
          if (change.getExpression() != null) {
            queryParameter.getSortCriterion().get(querySortCriterion.getIndex()).setCriterion(change.getExpression());
          }
          if (change.getAscending() != null) {
            queryParameter.getSortCriterion().get(querySortCriterion.getIndex()).setReverse(!change.getAscending());
          }
          QueryUtils.saveQueryParamater(object, modification.getObject().getDataflow(), queryParameter);
        }
      } catch (Exception ex) {
        Utils.logError(ex);
      }
    }
  }
  
  @Override
  protected void modifyQuerySelectionMask(QuerySelectionMask querySelectionMask)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    SelectionMask selectionMask = QueryUtils.getSelectionMask(GBSubObject.of(object.getRoot(), ObjectId.createStepId(object.getStep()).getObjectId()));
    if(selectionMask != null) {
      selectionMask.getColumns().remove(querySelectionMask.getIndex());
      selectionMask.getColumns().add(querySelectionMask.getIndex(), change.getExpression());
      QueryUtils.saveSelectionMask(object, object.getRoot().getDataflow(), selectionMask);
    }
  }
  

  private void changeQuerySelectionMask(String castToFqn) throws UnsupportedOperationException, XynaException {
    
    SelectionMask selectionMask = QueryUtils.getSelectionMask(object);
    if(selectionMask != null) {
      if(castToFqn != null && !castToFqn.isEmpty()) {
        GenerationBaseObject castToGbo = modification.load(FQNameJson.ofPathAndName(castToFqn));
        GenerationBaseObject storable = modification.load(FQNameJson.ofPathAndName(Storable.class.getName()));
        if(!DomOrExceptionGenerationBase.isSuperClass(
                         (DomOrExceptionGenerationBase) storable.getGenerationBase(),
                         (DomOrExceptionGenerationBase) castToGbo.getGenerationBase())) {
          throw new UnsupportedOperationException(UnsupportedOperationException.OPERATION_DYNAMIC_TYPING, UnsupportedOperationException.DYNAMIC_TYPING_WRONG_TYPE);
        }
        selectionMask.setRootType(castToFqn);
      } else {
        selectionMask.setRootType(Storable.class.getName());
      }
      
      QueryUtils.saveSelectionMask(object, modification.getObject().getDataflow(), selectionMask);
      QueryUtils.refreshRootType(object, (DOM)modification.load(FQNameJson.ofPathAndName(selectionMask.getRootType())).getGenerationBase());
    }
  }

  @Override
  protected void modifyMemberVar(DomOrExceptionGenerationBase dtOrException, MemberVarInfo memberVarInfo) throws UnsupportedOperationException, XynaException, ModificationNotAllowedException, UnsupportedJavaTypeException {
    List<AVariable> vars = dtOrException.getAllMemberVarsIncludingInherited();
    AVariable var = vars.get(memberVarInfo.getIndex());

    String fqn = change.getFqn();
    String primitiveType = change.getPrimitiveType();
    if ( (fqn != null && !Util.isExcludedType(fqn)) || (primitiveType != null) ) {
      changeType(fqn, primitiveType, dtOrException, var);
    }

    String label = change.getLabel();
    if (label != null) {
      var.setLabel(label);

      List<String> usedNames = new ArrayList<String>();
      for (AVariable curVar : vars) {
        usedNames.add(curVar.getVarName());
      }

      String varName = com.gip.xyna.xprc.xfractwfe.generation.xml.Utils.createUniqueJavaName(usedNames, label, false);
      if(varName == null || varName.isBlank()) {
        varName = com.gip.xyna.xprc.xfractwfe.generation.xml.Utils.createUniqueJavaName(usedNames, "data", false);
      }

      var.setVarName(varName);
    }

    String storableRole = change.getStorableRole();
    if (storableRole != null) {
      if (GuiLabels.DT_LABEL_HISTORIZATION_TIMESTAMP.equals(storableRole)) {
        changeStorableRole(var, PersistenceTypeInformation.HISTORIZATION_TIMESTAMP);
      } else if (GuiLabels.DT_LABEL_UNIQUE_IDENTIFIER.equals(storableRole)) {
        changeStorableRole(var, PersistenceTypeInformation.UNIQUE_IDENTIFIER);
      } else if (GuiLabels.DT_LABEL_CURRENTVERSION_FLAG.equals(storableRole)) {
        changeStorableRole(var, PersistenceTypeInformation.CURRENTVERSION_FLAG);
      } else if (storableRole.length() == 0) {
        changeStorableRole(var, null);
      } else {
        throw new UnsupportedOperationException(UnsupportedOperationException.OPERATION_STORABLE_ROLE,
                                                UnsupportedOperationException.STORABLE_ROLE_NOT_SUPPORTED
                                                    + GuiLabels.DT_LABEL_HISTORIZATION_TIMESTAMP + ", "
                                                    + GuiLabels.DT_LABEL_UNIQUE_IDENTIFIER + ", "
                                                    + GuiLabels.DT_LABEL_CURRENTVERSION_FLAG + ".");
      }
    }

    Boolean isList = change.isList();
    if (isList != null) {
      var.setIsList(isList);
    }
  }

  private void changeType(String fqn, String primitiveType, DomOrExceptionGenerationBase dtOrException, AVariable var) throws UnsupportedOperationException, XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException, UnsupportedJavaTypeException {
    if ( (fqn != null && fqn.length() > 0) && (primitiveType != null && primitiveType.length() > 0) ) {
      throw new UnsupportedOperationException(UnsupportedOperationException.OPERATION_CHANGE_TYPE, UnsupportedOperationException.CHANGE_TYPE_MUTUALLY_EXCLUSIVE);
    }

    if (Util.isExcludedType(fqn)) {
      throw new UnsupportedOperationException(UnsupportedOperationException.OPERATION_CHANGE_TYPE, UnsupportedOperationException.INVALID_TYPE);
    }

    AVariable newVar;
    
    if(fqn == null || !fqn.contains(".")) {
      DatatypeVariable dtVar = new DatatypeVariable(dtOrException);
      dtVar.create(PrimitiveType.create(primitiveType));
      newVar = dtVar;
    } else if (Util.isSubtypeOf(fqn, GenerationBase.CORE_EXCEPTION, dtOrException.getRevision())) {
      ExceptionVariable exceptionVar = new ExceptionVariable(dtOrException);
      String path = fqn.substring(0, fqn.lastIndexOf('.'));
      String name = fqn.substring(Math.min(fqn.lastIndexOf('.')+1, fqn.length()));
      exceptionVar.init(path, name);
      newVar = exceptionVar;
    } else {
      DatatypeVariable dtVar = new DatatypeVariable(dtOrException);
      dtVar.create(fqn);
      dtVar = new DatatypeVariable(dtOrException, dtVar.getDomOrExceptionObject().getRevision());
      dtVar.create(fqn);
      newVar = dtVar;
    }

    newVar.setLabel(var.getLabel());
    newVar.setPersistenceTypes(var.getPersistenceTypes());
    newVar.setIsList(var.isList());
    newVar.setVarName(var.getVarName());
    newVar.setDocumentation(var.getDocumentation());

    dtOrException.replaceMemberVar(var, newVar);
  }

  private void changeStorableRole(AVariable var, PersistenceTypeInformation newRole) {
    Set<PersistenceTypeInformation> persistenceTypes = var.getPersistenceTypes();
    if (persistenceTypes == null) {
      persistenceTypes = new HashSet<PersistenceTypeInformation>();
    }

    if (persistenceTypes.contains(newRole)) {
      // desired new storable role is already assigned
      return;
    }

    // remove existing storable role
    Set<PersistenceTypeInformation> possibleRoles =  new HashSet<PersistenceTypeInformation>(
        Arrays.asList(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP, PersistenceTypeInformation.UNIQUE_IDENTIFIER, PersistenceTypeInformation.CURRENTVERSION_FLAG));
    for (PersistenceTypeInformation persistenceType : persistenceTypes) {
      if (possibleRoles.contains(persistenceType)) {
        persistenceTypes.remove(persistenceType);
        break;
      }
    }

    // add new storable role
    if (newRole != null) {
      persistenceTypes.add(newRole);
    }

    var.setPersistenceTypes(persistenceTypes);
  }

  @Override
  protected void modifyMemberMethod(DOM dom, MemberMethodInfo memberMethodInfo) throws UnsupportedOperationException, XynaException, ModificationNotAllowedException {
    OperationInformation[] operationInformations = dom.collectOperationsOfDOMHierarchy(true);
    OperationInformation operationInformation = operationInformations[memberMethodInfo.getIndex()];
    Operation operation = operationInformation.getOperation();

    String implementationType = change.getImplementationType();
    if (implementationType != null) {
      Operation newOperation = null;
      if (GuiLabels.DT_LABEL_IMPL_TYPE_ABSTRACT.equals(implementationType)) {
        newOperation = new JavaOperation(dom);
        ((JavaOperation) newOperation).setAbstract(true);
      } else if (GuiLabels.DT_LABEL_IMPL_TYPE_CODED_SERVICE.equals(implementationType)) {
        newOperation = new JavaOperation(dom);
        ((JavaOperation) newOperation).setImpl("");
        newOperation.setStatic(operation.isStatic());
      } else if (GuiLabels.DT_LABEL_IMPL_TYPE_CODED_SERVICE_PYTHON.equals(implementationType)) {
        newOperation = new PythonOperation(dom);
        ((PythonOperation) newOperation).setImpl("");
        newOperation.setStatic(operation.isStatic());
      } else if (GuiLabels.DT_LABEL_IMPL_TYPE_REFERENCE.equals(implementationType)) {
        newOperation = new WorkflowCallInService(dom);
        ((WorkflowCallInService)newOperation).setWf(change.getReference(), dom.getRevision());
      } else {
        throw new UnsupportedOperationException(UnsupportedOperationException.OPERATION_IMPLEMENTATION_TYPE,
            UnsupportedOperationException.IMPLEMENTATION_TYPE_NOT_SUPPORTED
                + GuiLabels.DT_LABEL_IMPL_TYPE_ABSTRACT + ", "
                + GuiLabels.DT_LABEL_IMPL_TYPE_CODED_SERVICE + ", "
                + GuiLabels.DT_LABEL_IMPL_TYPE_CODED_SERVICE_PYTHON + ", "
                + GuiLabels.DT_LABEL_IMPL_TYPE_REFERENCE + ".");
      }

      newOperation.setLabel(operation.getLabel());
      newOperation.setName(operation.getName());
      newOperation.takeOverSignature(operation);
      newOperation.setDocumentation(operation.getDocumentation());
      dom.replaceOperation(operation, newOperation);
      operation = newOperation;
      
      object.refreshIdentifiedVariables();
    }

    String label = change.getLabel();
    if (label != null) {
      operation.setLabel(label);

      List<String> usedNames = new ArrayList<>();
      for (OperationInformation curOpInfo : operationInformations) {
        usedNames.add(curOpInfo.getOperation().getName());
      }
      operation.setName(com.gip.xyna.xprc.xfractwfe.generation.xml.Utils.createUniqueJavaName(usedNames, label, false));

      // since changing the label may change the order of the services and their ids are determined by their order, the variable map has to be refreshed for all services
      object.getRoot().resetVariableMap();
    }

    String implementation = change.getImplementation();
    if (implementation != null) {
      if (!(operation instanceof CodeOperation) || ((CodeOperation) operation).isAbstract()) {
        throw new UnsupportedOperationException(UnsupportedOperationException.OPERATION_IMPLEMENTATION,
                                                UnsupportedOperationException.IMPLEMENTATION_NOT_SUPPORTED);
      }
      ((CodeOperation) operation).setImpl(implementation.strip());
    }

    Boolean isAbortable = change.isAbortable();
    if (isAbortable != null) {
      operation.setIsStepEventListener(isAbortable);
    }

    setAbstract(dom, change.isAbstract());
  }

  @Override
  protected void modifyTypeInfoArea(DomOrExceptionGenerationBase dtOrException) throws XynaException, ModificationNotAllowedException {
    String label = change.getLabel();
    if (label != null) {
      dtOrException.setLabel(label);
    }

    String baseTypeFqn = change.getBaseType();
    if (baseTypeFqn != null && Util.isExcludedType(baseTypeFqn)) {
      throw new ModificationNotAllowedException(UnsupportedOperationException.INVALID_BASE_TYPE);
    }

    if ( (baseTypeFqn != null) &&
         (!(dtOrException instanceof ExceptionGeneration) || !baseTypeFqn.isEmpty()) ) { // exception types must always have a base type
      GenerationBase baseType;
      if (baseTypeFqn.length() == 0) {
        baseType = null;
      } else {
        baseType = GenerationBase.getOrCreateInstance(baseTypeFqn, new GenerationBaseCache(), dtOrException.getRevision());
        baseType.parseGeneration(false/*saved*/, false, false);
      }

      if (dtOrException instanceof DOM) {
        ((DOM) dtOrException).replaceParent((DOM) baseType);
      } else  if (baseType != null) {
        ((ExceptionGeneration) dtOrException).replaceParent((ExceptionGeneration) baseType);
      }
      
      List<String> inheritedNames = new ArrayList<>();
      List<String> usedNames = new ArrayList<>();
      List<AVariable> allVars = dtOrException.getAllMemberVarsIncludingInherited();
      for (AVariable v : allVars) {
        if(!v.getCreator().equals(dtOrException)) {
          inheritedNames.add(v.getVarName());
        }
        usedNames.add(v.getVarName());
      } 
      
      List<AVariable> vars = dtOrException.getMemberVars();
      for (AVariable var : vars) {
        if(inheritedNames.contains(var.getVarName())) {
          String varName = com.gip.xyna.xprc.xfractwfe.generation.xml.Utils.createUniqueJavaName(usedNames, var.getLabel(), false);
          var.setVarName(varName);
          usedNames.add(varName);
        }
      }      
    }

    setAbstract(dtOrException, change.isAbstract());
  }
  
  @Override
  protected void modifyMetaTag(DomOrExceptionGenerationBase dtOrException) throws XPRC_XmlParsingException {
    XMLUtils.parseString(change.getTag()); // validate whether tag content is valid XML
    int idx = ObjectId.getMetaTagIdx(object.getId());
    DTMetaTag newTag = GBSubObjectUtils.createDTMetaTag(change.getTag(), idx);
    object.getMetaTagListAdapter().set(idx, newTag);
  }

  @Override
  protected void modifyOrderInputSource(Step step)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    if(change.getName() != null && step instanceof StepFunction) {
      StepFunction stepFunction = (StepFunction)step;
      stepFunction.setOrderInputSourceRef(change.getName());
    }
  }

  @Override
  protected void modifyExceptionMessageArea(DomOrExceptionGenerationBase dtOrException) {
    String exceptionMsgLanguage = change.getExceptionMsgLanguage();
    if (exceptionMsgLanguage != null && dtOrException instanceof ExceptionGeneration) {
      ExceptionGeneration exception = (ExceptionGeneration) dtOrException;
      String exceptionMsgText = change.getExceptionMsgText();
      Map<String, String> exceptionMessages = exception.getExceptionEntry().getMessages();

      if (exceptionMsgText == null || exceptionMsgText.equals("")) {
        // remove message for given language
        exceptionMessages.remove(exceptionMsgLanguage);
      } else {
        // set message
        exceptionMessages.put(exceptionMsgLanguage, exceptionMsgText);
      }
    }
  }

  @Override
  protected void modifyServiceGroupSharedLib(DOM serviceGroup) {
    List<SharedLib> availableSharedLibs = Utils.getSharedLibs(serviceGroup.getRevision());
    SharedLib libToChange = availableSharedLibs.get(object.getLibInfo().getIndex());

    if (change.isUsed()) {
      serviceGroup.addSharedLib(libToChange.getName());
    } else {
      serviceGroup.deleteSharedLib(libToChange.getName());
    }
  }
  
  @Override
  protected void modifyRemoteDestination(StepFunction step) {
    ServiceUtils.setRemoteDestination(step, change.getName());
  }

}
