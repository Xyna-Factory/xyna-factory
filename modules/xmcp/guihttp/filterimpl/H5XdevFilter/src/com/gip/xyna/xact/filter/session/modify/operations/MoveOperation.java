/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.session.Clipboard;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.Clipboard.ClipboardCopyDirection;
import com.gip.xyna.xact.filter.session.exceptions.MergeConflictException;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.exceptions.UnsupportedOperationException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Case;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.CaseArea;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberMethodInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberVarInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Variable;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectId.ObjectIdPrefix;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.StepMap;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepChoice;
import com.gip.xyna.xact.filter.session.modify.Insertion;
import com.gip.xyna.xact.filter.session.modify.Insertion.QueryInsertStep;
import com.gip.xyna.xact.filter.session.workflowwarnings.ReferenceInvalidatedNotification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.DirectVarIdentification;
import com.gip.xyna.xact.filter.util.HintGeneration;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xact.filter.xmom.workflows.json.CopyJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.MoveJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.PositionJson.RelativePosition;
import com.gip.xyna.xact.filter.xmom.workflows.json.VariableJson;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.BranchInfo;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.PythonOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.ServiceVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCallInService;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCallServiceReference;

import xmcp.processmodeller.datatypes.RepairEntry;
import xmcp.xact.modeller.Hint;



public class MoveOperation extends ModifyOperationBase<MoveJson> {

  private MoveJson move;
  private final Clipboard clipboard;


  public MoveOperation() {
    this(null);
  }


  public MoveOperation(Clipboard clipboard) {
    this.clipboard = clipboard;
  }


  @Override
  public int parseRequest(String jsonRequest) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();
    move = jp.parse(jsonRequest, MoveJson.getJsonVisitor());

    return move.getRevision();
  }


  private void move(GBSubObject object)
      throws UnknownObjectIdException, MissingObjectException, XynaException, UnsupportedOperationException, MergeConflictException {
    move(object, null);
  }


  private void move(GBSubObject object, QueryInsertStep queryInsertStep)
      throws UnknownObjectIdException, MissingObjectException, XynaException, UnsupportedOperationException, MergeConflictException {
    if (clipboard != null) {
      try {
        moveFromClipboard(object);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return;
    }

    if (isQuery(object)) {
      moveQuery();
      return;
    }


    if (object.isStepInTryBlock()) {
      // step is in a try-block -> move the try-block, instead
      move(object.getParent(), queryInsertStep);
      return;
    }

    if (object.isStepInForeach()) {
      // step is in a StepForeach -> move StepForeach, instead
      move(object.getParent().getParent().getParent(), queryInsertStep);
      return;
    }

    GBSubObject relativeToObject = object.getRoot()
        .getObject(move.getRelativeTo().startsWith("wf_") ? move.getRelativeTo().replaceFirst("wf_", "step_") : move.getRelativeTo());

    Insertion insertion = new Insertion(relativeToObject, move);
    insertion.setQueryInsertStep(queryInsertStep);
    insertion.wrapWhenNeeded(modification.getObject()); // Hinzufuegen von fuer Insert evtl. benoetigten Wrapper-Schritten
    insertion.inferWhere(object); //Feststellen wohin inserted wird, dies ist leider nicht eindeutig
    insertion.inferPossibleContent(); //Überlegen was im Content stehen könnte, dies ist leider nicht eindeutig

    if (isOperationInSelf(relativeToObject)) {
      throw new UnsupportedOperationException("move", UnsupportedOperationException.MOVE_IN_YOURSELF_IS_NOT_POSSIBLE);
    }

    if (object.getType() == ObjectType.distinctionCase && relativeToObject.getCaseAreaInfo() != null) {
      prepareMoveCase(relativeToObject);
    }

    // Special Case: Move a Exception-Variable to a workflow
    if (isSpecialInsertThrow(relativeToObject)) {
      specialInsertThrow(relativeToObject, insertion);
    } else {
      insertion.checkContent(object);
      insertion.move(object);

      switch (object.getType()) {
        case step :
          // scope of step might have changed
          if (object.getStep() != null && relativeToObject.getStep() != null) {
            object.getStep().setParentScope(relativeToObject.getStep().getParentScope());
            Utils.updateScopeOfSubSteps(object.getStep()); //update scope of children
          }

          // a StepParallel with an empty lane or only one lane might be left
          Utils.cleanupStepParallels(modification.getObject().getStepMap());

          break;

        default :
          break;
      }
    }

    postprocess(relativeToObject);
  }


  private void moveVariableFromClipboard(GBSubObject object) throws Exception {
    CopyOperation copyOp = new CopyOperation(clipboard, ClipboardCopyDirection.FROM_CLIPBOARD);
    CopyJson copy = new CopyJson(move.getRevision(), move.getRelativeTo(), move.getInsideIndex());
    copyOp.setCopy(copy);
    copyOp.modify(modification, object);

    clipboard.removeEntry(object);
  }


  private void moveStepFromClipboard(GBSubObject object)
      throws UnsupportedOperationException, XynaException, MissingObjectException, UnknownObjectIdException {
    GBSubObject gbSubObject = modification.getObject().getObject(move.getRelativeTo());

    clipboard.resetHints();
    List<RepairEntry> repairEntries = Clipboard.copyStepFromClipboard(object, gbSubObject, modification, move.getInsideIndex(), move);
    for (RepairEntry entry : repairEntries) {
      Hint hint = HintGeneration.convertRepairEntryToHint(entry);
      clipboard.getHints().add(hint);
    }

    clipboard.removeEntry(object);
  }


  private void moveFromClipboard(GBSubObject object) throws Exception {
    if (move.getRelativeTo() != null && move.getRelativeTo().equals(ObjectIdPrefix.clipboardEntry.name())) {
      clipboard.moveEntry(move.getInsideIndex(), object);
      return;
    }
    if (object.getType() == ObjectType.variable) {
      moveVariableFromClipboard(object);
      return;
    } else if (object.getType() == ObjectType.step) {
      moveStepFromClipboard(object);
      return;
    }

    throw new UnsupportedOperationException("move", "unsupported object type: " + object.getType());

  }


  private void moveQuery()
      throws UnknownObjectIdException, MissingObjectException, XynaException, UnsupportedOperationException, MergeConflictException {
    StepMapping stepMapping = QueryUtils.findQueryHelperMapping(object);
    if (stepMapping != null) {
      GBSubObject mapping = modification.getObject().getObject(ObjectId.createStepId(stepMapping).getObjectId());
      move(mapping, QueryInsertStep.mapping);
      if (move.getRequestInsideIndex() != null) {
        move.setInsideIndex(move.getRequestInsideIndex());
      }
      if (RelativePosition.left == move.getRelativePosition() || RelativePosition.right == move.getRelativePosition()) {
        move.setRelativePosition(RelativePosition.bottom);
        move.setRelativeTo(mapping.getObjectId());
      }
    }
    move(object, QueryInsertStep.function);
  }


  private void prepareMoveCase(GBSubObject relativeToObject) throws MergeConflictException {
    int sourceCaseNo = ObjectId.parseCaseNumber(object.getId());
    StepChoice stepChoice = (StepChoice) object.getStep();
    int sourceBranchNo = stepChoice.getBranchNo(sourceCaseNo);
    CaseArea destCaseArea = relativeToObject.getCaseAreaInfo();
    int destBranchNo = destCaseArea.getCaseAreaNo();

    if (sourceBranchNo == destBranchNo) {
      // case is moved within branch -> no merge conflicts possible
      return;
    }

    // when case to be moved is last one of its branch, the branch will be discarded, causing the index to be shifted
    BranchInfo sourceBranch = stepChoice.getBranchesForGUI().get(sourceBranchNo);
    if (sourceBranchNo < destBranchNo && sourceBranch.getCases().size() <= 1) {
      destCaseArea.setCaseAreaNo(destCaseArea.getCaseAreaNo() - 1);
    }

    boolean mergeConflict = false;
    if (sourceBranch.getCases().size() <= 1) {
      for (Step stepInSourceBranch : sourceBranch.getMainStep().getChildSteps()) {
        if (!(stepInSourceBranch instanceof StepAssign)) {
          mergeConflict = true;
          break;
        }
      }
    }

    BranchInfo destBranch = stepChoice.getBranchesForGUI().get(destBranchNo);
    StepSerial destContainer = (StepSerial) destBranch.getMainStep();

    if (mergeConflict) {
      handleChoiceMergeConflict(stepChoice, sourceBranch, destBranch);
    }

    // when cases of type choice are merged, input of branch can't be used as casted type, anymore
    disconnectFromBranch(stepChoice, destContainer);
  }


  private void disconnectFromBranch(StepChoice stepChoice, StepSerial branchContainer) {
    if (stepChoice.getDistinctionType() != DistinctionType.TypeChoice) {
      return;
    }

    List<Step> assignSteps = branchContainer.getChildSteps().stream().filter(x -> x instanceof StepAssign).collect(Collectors.toList());
    if (assignSteps.size() > 1) {
      StepAssign firstAssign = (StepAssign) assignSteps.get(0);
      for (String idToDisconnect : firstAssign.getOutputVarIds()) {
        for (Step stepToDisconnect : getGuiSteps(branchContainer.getChildSteps())) {
          disconnectVarInput(stepToDisconnect, idToDisconnect);
        }
      }

      branchContainer.removeChild(assignSteps.get(0));
    }
  }


  private void disconnectVarInput(Step step, String varId) {
    String[] inputVarIds = step.getInputVarIds();
    for (int varIdx = 0; varIdx < inputVarIds.length; varIdx++) {
      if ((inputVarIds[varIdx] != null) && (inputVarIds[varIdx].equals(varId))) {
        inputVarIds[varIdx] = null;
      }
    }
  }


  private void handleChoiceMergeConflict(StepChoice stepChoice, BranchInfo sourceBranch, BranchInfo destBranch)
      throws MergeConflictException {
    // when force-flag is not set, merge conflicts lead to aborting the operation
    if (!move.isForce()) {
      throw new MergeConflictException();
    }

    // resolve merge conflict with method specified by parameter conflictHandling

    StepSerial sourceContainer = (StepSerial) sourceBranch.getMainStep();
    StepSerial destContainer = (StepSerial) destBranch.getMainStep();
    StepMap stepMap = modification.getObject().getStepMap();

    switch (move.getConflictHandling()) {
      case USE_SOURCE :
        for (Step guiStep : getGuiSteps(destContainer.getChildSteps())) {
          destContainer.removeChild(guiStep);
          stepMap.removeStep(guiStep);
        }

        destContainer.resetVariablesAndExceptions();

      case APPEND :
        disconnectFromBranch(stepChoice, sourceContainer); // when cases of type choice are merged, input of branch can't be used as casted type, anymore
        destContainer.addChildren(getGuiSteps(sourceContainer.getChildSteps()));

        for (AVariable varToCopy : sourceContainer.getVariablesAndExceptions()) {
          destContainer.addVar(varToCopy);
        }
        break;

      case USE_DESTINATION :
        for (Step guiStep : getGuiSteps(sourceContainer.getChildSteps())) {
          stepMap.removeStep(guiStep);
        }
    }
  }


  private void postprocess(GBSubObject relativeToObject) {
    if (object.getType() == ObjectType.distinctionCase && relativeToObject.getCaseAreaInfo() != null) {
      StepChoice stepChoice = (StepChoice) object.getStep();
      IdentifiedVariablesStepChoice identifiedVariables =
          ((IdentifiedVariablesStepChoice) modification.getObject().getVariableMap().identifyVariables(ObjectId.createStepId(stepChoice)));
      identifiedVariables.loadCreatedVariables();
    }
  }


  private List<Step> getGuiSteps(List<Step> steps) {
    List<Step> guiSteps = new ArrayList<>();
    for (Step step : steps) {
      if (!(step instanceof StepAssign)) {
        guiSteps.add(step);
      }
    }

    return guiSteps;
  }


  @Override
  protected void modifyStep(Step step)
      throws XynaException, UnknownObjectIdException, MissingObjectException, UnsupportedOperationException, MergeConflictException {
    move(object);

    FQName fqName = modification.getObject().getFQName();
    ReferenceInvalidatedNotification notification = new ReferenceInvalidatedNotification(fqName, object.getRoot().getWorkflow());
    modification.getSession().getWFWarningsHandler(fqName).handleChange(object.getId(), notification);
  }


  @Override
  protected void modifyVariable(Variable variable)
      throws XynaException, UnknownObjectIdException, MissingObjectException, UnsupportedOperationException, MergeConflictException {
    Dataflow df = modification.getObject().getDataflow();
    Step step = object.getStep();
    if (df != null) {
      if (step != null && step instanceof StepFunction) {
        df.prepareStepFunctionVarsForUpdate(variable.getIdentifiedVariables());
      }
    }
    move(object);

    FQName fqName = modification.getObject().getFQName();
    ReferenceInvalidatedNotification notification = new ReferenceInvalidatedNotification(fqName, object.getRoot().getWorkflow());
    modification.getSession().getWFWarningsHandler(fqName).handleChange(object.getId(), notification);
  }


  @Override
  protected void modifyMemberVar(DomOrExceptionGenerationBase dtOrException, MemberVarInfo memberVarInfo)
      throws UnknownObjectIdException, MissingObjectException, XynaException, UnsupportedOperationException, MergeConflictException {
    move(object);
  }


  @Override
  protected void modifyCase(Case caseInfo) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException,
      UnexpectedJSONContentException, UnsupportedOperationException, MergeConflictException {
    move(object);
  }


  @Override
  protected void modifyMemberMethod(DOM dom, MemberMethodInfo memberMethodInfo) throws XynaException, UnsupportedOperationException {
    Operation oldOperation = object.getOperation();
    Operation newOperation = null;
    if (oldOperation != null) {
      if (oldOperation instanceof JavaOperation) { // Override Method
        newOperation = new JavaOperation(dom);
        JavaOperation newJavaOperation = (JavaOperation) newOperation;
        JavaOperation oldJavaOperation = (JavaOperation) oldOperation;
        newJavaOperation.setActive(oldJavaOperation.isActive());
        newJavaOperation.setImpl(oldJavaOperation.getImpl());
        newJavaOperation.setLabel(oldJavaOperation.getLabel());
      } else if (oldOperation instanceof PythonOperation) { // Override Method
        newOperation = new PythonOperation(dom);
        PythonOperation newPythonOperation = (PythonOperation) newOperation;
        PythonOperation oldPythonOperation = (PythonOperation) oldOperation;
        newPythonOperation.setActive(oldPythonOperation.isActive());
        newPythonOperation.setImpl(oldPythonOperation.getImpl());
        newPythonOperation.setLabel(oldPythonOperation.getLabel());
      } else if (oldOperation instanceof WorkflowCallInService) {
        newOperation = new WorkflowCallInService(dom);
        WorkflowCallInService newWorkflowCallInService = (WorkflowCallInService) newOperation;
        WorkflowCallInService oldWorkflowCallInService = (WorkflowCallInService) oldOperation;
        newWorkflowCallInService.setWf(oldWorkflowCallInService.getWfFQClassName(), dom.getRevision());
      } else if (oldOperation instanceof WorkflowCallServiceReference) {
        newOperation = new WorkflowCallServiceReference(dom);
      }
      if (newOperation != null) {
        newOperation.setAbstract(oldOperation.isAbstract());
        newOperation.setDocumentation(oldOperation.getDocumentation());
        newOperation.setIsStepEventListener(oldOperation.isStepEventListener());
        newOperation.setLabel(oldOperation.getLabel());
        newOperation.setName(oldOperation.getName());
        newOperation.setStatic(oldOperation.isStatic());
        newOperation.setVersion(oldOperation.getVersion());

        dom.addOperation(dom.getOperations().size(), newOperation);

        GBSubObject gbsNewMethod =
            new GBSubObject(object.getRoot(), new ObjectId(ObjectType.operation, String.valueOf(Utils.getOperationIndex(newOperation))),
                            dom, newOperation);
        gbsNewMethod.getRoot().resetVariableMap();
        copyVars(oldOperation.getInputVars(), VarUsageType.input, gbsNewMethod);
        copyVars(oldOperation.getOutputVars(), VarUsageType.output, gbsNewMethod);
        copyVars(oldOperation.getThrownExceptions(), VarUsageType.thrown, gbsNewMethod);
      } else {
        throw new UnsupportedOperationException("overrideMethod",
                                                "Override method of type " + oldOperation.getClass().getName() + " is not supported");
      }
    }
  }


  private void copyVars(List<? extends AVariable> vars, VarUsageType varUsageType, GBSubObject gbsNewMethod) throws XynaException {
    for (AVariable var : vars) {
      copyVar(var, varUsageType, gbsNewMethod);
    }
  }


  private void copyVar(AVariable var, VarUsageType varUsageType, GBSubObject gbsNewMethod) throws XynaException {
    AVariableIdentification varIdent = null;
    DomOrExceptionGenerationBase doe = var.getDomOrExceptionObject();
    if (doe == null && var instanceof ServiceVariable && var.getJavaTypeEnum() != null) {
      AVariable clone = new ServiceVariable((ServiceVariable) var);
      clone.setId(String.valueOf(clone.getCreator().getNextXmlId()));
      clone.setVarName(var.getJavaTypeEnum().toString().toLowerCase() + clone.getId());
      varIdent = DirectVarIdentification.of(clone);
    } else {
      String type = doe instanceof ExceptionGeneration ? Tags.EXCEPTION : Tags.VARIABLE;
      VariableJson variableJson = new VariableJson(type, var.getLabel(), FQNameJson.ofPathAndName(var.getFQClassName()));
      variableJson.setList(var.isList());
      GBBaseObject newVariable = createParameter(gbsNewMethod, variableJson);
      varIdent = newVariable.getVariable().getVariable();
    }
    gbsNewMethod.getIdentifiedVariables().getListAdapter(varUsageType).add(varIdent);
  }

}
