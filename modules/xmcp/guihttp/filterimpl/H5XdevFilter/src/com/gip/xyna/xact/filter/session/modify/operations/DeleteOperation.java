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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.session.Clipboard;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.ModificationNotAllowedException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.exceptions.UnsupportedOperationException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Branch;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Case;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.FormulaInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberMethodInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberVarInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.QueryFilterCriterion;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.QuerySelectionMask;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.QuerySortCriterion;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Variable;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.workflowwarnings.ReferenceInvalidatedNotification;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xact.filter.xmom.workflows.json.DeleteJson;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;

import xnwh.persistence.QueryParameter;
import xnwh.persistence.SelectionMask;

public class DeleteOperation extends ModifyOperationBase<DeleteJson> {

  private DeleteJson delete;
  private final Clipboard clipboard; //only set if deleting something from clipBoard
  
  
  public DeleteOperation() {
    clipboard = null;
  }
  
  public DeleteOperation(Clipboard clipboard) {
    this.clipboard = clipboard;
  }
  

  @Override
  public int parseRequest(String jsonRequest) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();
    delete = jp.parse(jsonRequest, DeleteJson.getJsonVisitor());
    
    return delete.getRevision();
  }


  @Override
  protected void modifyStep(Step step) {
    if (clipboard != null) {
      deleteFromClipboard();
      return;
    } else if (isQuery(object)) {
      deleteQuery(step);
    } else {
      deleteStep(step, object.getParent());
    }

    FQName fqName = modification.getObject().getFQName();
    ReferenceInvalidatedNotification notification = new ReferenceInvalidatedNotification(fqName, object.getRoot().getWorkflow());
    modification.getSession().getWFWarningsHandler(fqName).handleChange(object.getId(), notification);
  }
  
  private void deleteFromClipboard() {
    clipboard.removeEntry(object);
  }

  private void deleteStep(Step step, GBSubObject parent) {
    
    if (step.getParentStep() instanceof StepCatch) {
      // step is in a try-block -> delete the whole block from its parent
      step = step.getParentStep();
      parent = parent.getParent();
    }

    parent.getStepListAdapter().remove(step);
    removeWrapperWhenObsolete((StepSerial)parent.getStep());
    updateParentsWhenNeeded((StepSerial)parent.getStep());

    setFocusCandidateId( parent.getObjectId() );

    FQName fqn = modification.getObject().getFQName();
    if (object.getStep() instanceof StepMapping) {
      // reset warnings for deleted mapping
      String objectId = ObjectId.createStepId(object.getStep()).getObjectId();
      modification.getSession().getWFWarningsHandler(fqn).deleteAllWarnings(objectId);
    }

    // reset warnings for all children that are mappings
    Set<Step> recursiveChildren = new HashSet<>();
    WF.addChildStepsRecursively(recursiveChildren, object.getStep());
    for (Step childStep : recursiveChildren) {
      if (childStep instanceof StepMapping) {
        String objectId = ObjectId.createStepId(childStep).getObjectId();
        modification.getSession().getWFWarningsHandler(fqn).deleteAllWarnings(objectId);
      }
    }
  }

  private void deleteQuery(Step step) {
    StepMapping mapping = QueryUtils.findQueryHelperMapping(object);
    if(mapping != null) {
      deleteStep(mapping, object.getParent().getParent());
    }
    deleteStep(step, object.getParent());
  }

  public void updateParentsWhenNeeded(Step container) {
    if (object.getStep() instanceof StepRetry) {
      StepCatch stepCatch = (StepCatch)container.getParentStep();
      stepCatch.updateRetryHandlers();
    }
  }


  @Override
  protected void modifyVariable(Variable variable) throws XynaException, ModificationNotAllowedException {
    if (clipboard != null) {
      deleteFromClipboard();
      return;
    }
    GBSubObject parent = object.getParent();
    Dataflow df = modification.getObject().getDataflow();
    Step step = object.getStep();
    if (df != null) {
      if (step != null && step instanceof StepFunction) {
        df.prepareStepFunctionVarsForUpdate(variable.getIdentifiedVariables());
      } else {
        df.resetConnection(object);
      }
    }
    
    parent.getVariableListAdapter().remove(variable);
    
    if(step instanceof WFStep) {
      FQName fqName = modification.getObject().getFQName();
      ReferenceInvalidatedNotification notification = new ReferenceInvalidatedNotification(fqName, object.getRoot().getWorkflow());
      modification.getSession().getWFWarningsHandler(fqName).handleChange(object.getId(), notification);
    }
    
    setFocusCandidateId(parent.getObjectId());
  }

  @Override
  protected void modifyFormula(FormulaInfo formulaInfo) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    GBSubObject parent = object.getParent();
    parent.getExpressionListAdapter().remove(formulaInfo.getIndex());
    setFocusCandidateId( parent.getObjectId() );

    if (object.getStep() instanceof StepMapping) {
      FQName fqn = modification.getObject().getFQName();
      modification.getSession().getWFWarningsHandler(fqn).deleteAllWarnings(object.getId().getObjectId());
    }
  }

  @Override
  protected void modifyCase(Case caseInfo) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    GBSubObject parent = object.getParent();
    parent.getCaseListAdapter().remove(caseInfo.getIndex());
    setFocusCandidateId( parent.getObjectId() );
    
    if (parent.getWorkflow() != null) {
      FQName fqName = modification.getObject().getFQName();
      ReferenceInvalidatedNotification notification = new ReferenceInvalidatedNotification(fqName, object.getRoot().getWorkflow());
      modification.getSession().getWFWarningsHandler(fqName).handleChange(object.getId(), notification);
    }
  }

  @Override
  protected void modifyBranch(Branch branchInfo) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException, UnsupportedOperationException {
    GBSubObject parent = object.getParent();
    if ( !(parent.getStep() instanceof StepParallel) ) {
      // Deleting branches from parallelism is currently not supported.
      // TODO: support (let StepParallel implement interface Distinction and adapt parent.getBranchListAdapter())

      // delete constants for the branch
      ObjectId parentStepId = ObjectId.createStepId(parent.getStep());
      IdentifiedVariables identifiedVariables = modification.getObject().getVariableMap().identifyVariables(parentStepId);
      List<AVariableIdentification> outputVarIdents = identifiedVariables.getVariables(VarUsageType.output);
      for (AVariableIdentification outputVarIdent : outputVarIdents) {
        GBSubObject varSubObject = modification.getObject().getObject(outputVarIdent.internalGuiId.createId());
        String branchId = ObjectId.createBranchId(parentStepId.getBaseId(), String.valueOf(branchInfo.getBranchNr()));
        modification.getObject().getDataflow().deleteConnectionForBranch(varSubObject, branchId); 
      }

      // delete branch
      parent.getBranchListAdapter().remove(branchInfo.getBranchNr()); 
      setFocusCandidateId( parent.getObjectId() );
    }
  }
  
  @Override
  protected void modifyQueryFilter(QueryFilterCriterion queryFilterCriterion)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    if(object.getStep() instanceof StepFunction) {
      StepFunction stepFunction = (StepFunction)object.getStep();
      stepFunction.getQueryFilterConditions().remove(queryFilterCriterion.getIndex());
    }
    QueryUtils.refreshQueryHelperMappingExpression(object);
  }
  
  @Override
  protected void modifyQuerySorting(QuerySortCriterion querySortCriterion)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    
    List<AVariableIdentification> variableIdentifications = object.getIdentifiedVariables().getVariables(VarUsageType.input);
    for (AVariableIdentification vi : variableIdentifications) {
      if(vi.getIdentifiedVariable().getVarName().equals(Tags.QUERY_CONST_QUERY_PARAMETER)) {
        QueryParameter queryParameter = (QueryParameter) vi.getConstantValue(object.getRoot());
        queryParameter.getSortCriterion().remove(querySortCriterion.getIndex());
        object.getRoot().getDataflow().setConstantValue(vi, null, queryParameter);
        break;
      }
    }
  }
  
  @Override
  protected void modifyQuerySelectionMask(QuerySelectionMask querySelectionMask)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    SelectionMask selectionMask = QueryUtils.getSelectionMask(GBSubObject.of(object.getRoot(), ObjectId.createStepId(object.getStep()).getObjectId()));
    if(selectionMask != null) {
      selectionMask.getColumns().remove(querySelectionMask.getIndex());
      QueryUtils.saveSelectionMask(object, object.getRoot().getDataflow(), selectionMask);
    }
  }

  @Override
  protected void modifyMemberVar(DomOrExceptionGenerationBase dtOrException, MemberVarInfo memberVarInfo) throws XynaException, ModificationNotAllowedException {
    List<AVariable> variables = dtOrException.getAllMemberVarsIncludingInherited();
    dtOrException.removeMemberVar(variables.get(memberVarInfo.getIndex()));
  }

  @Override
  protected void modifyMemberMethod(DOM dom, MemberMethodInfo memberMethodInfo) throws XynaException, ModificationNotAllowedException {
    OperationInformation operationInformations = dom.collectOperationsOfDOMHierarchy(true)[memberMethodInfo.getIndex()];
    
    int index = -1;
    List<Operation> operations = dom.getOperations();
    for (int i = 0; i < operations.size(); i++) {
      if(operations.get(i).getName().equals(operationInformations.getOperation().getName())) {
        index = i;
        break;
      }
    }

    if (index != -1) {
      dom.removeOperation(index);
    }

    object.getRoot().resetVariableMap();

    // data type might become abstract in case the method was overriding one from a super class
    updateAbstractness(object.getDtOrException());

  }

  @Override
  protected void modifyMetaTag(DOM dom) {
    int idx = ObjectId.getMetaTagIdx(object.getId());
    object.getMetaTagListAdapter().remove(idx);
  }

  @Override
  protected void modifyServiceGroupLib(DOM serviceGroup) {
    object.getLibListAdapter().remove(object.getLibInfo().getIndex());
  }

}
