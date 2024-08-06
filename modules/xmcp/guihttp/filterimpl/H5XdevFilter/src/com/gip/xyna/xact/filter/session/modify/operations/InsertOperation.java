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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.XMOMLoader;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.ModificationNotAllowedException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.exceptions.UnsupportedOperationException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Branch;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Case;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.FormulaInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.QueryFilterCriterion;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Variable;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectId.ObjectPart;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.modify.Insertion;
import com.gip.xyna.xact.filter.session.modify.Insertion.PossibleContent;
import com.gip.xyna.xact.filter.session.modify.Insertion.QueryInsertStep;
import com.gip.xyna.xact.filter.session.modify.operations.copy.StepCopier;
import com.gip.xyna.xact.filter.session.workflowwarnings.ReferenceInvalidatedNotification;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.DirectVarIdentification;
import com.gip.xyna.xact.filter.util.HintGeneration;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xact.filter.xmom.workflows.json.FromXmlJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.InsertJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.MappingJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.PositionJson.RelativePosition;
import com.gip.xyna.xact.filter.xmom.workflows.json.QueryJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ServiceJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ServiceUtils;
import com.gip.xyna.xact.filter.xmom.workflows.json.VariableJson;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ForEachScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.SpecialPurposeIdentifier;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.DistinctionType;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;

import xmcp.processmodeller.datatypes.RepairEntry;
import xmcp.xact.modeller.Hint;
import xnwh.persistence.FilterCondition;
import xnwh.persistence.PersistenceServices;
import xnwh.persistence.QueryParameter;
import xnwh.persistence.SelectionMask;
import xnwh.persistence.Storable;

public class InsertOperation extends ModifyOperationBase<InsertJson> {

  private InsertJson insert;

  @Override
  public int parseRequest(String jsonRequest) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();
    insert = jp.parse(jsonRequest, InsertJson.getJsonVisitor());
    
    return insert.getRevision();
  }

  @Override
  protected void modifyStep(Step step) throws UnknownObjectIdException, MissingObjectException, XynaException, InvalidJSONException, UnexpectedJSONContentException {
    Dataflow df = modification.getObject().getDataflow();
    if (df != null) {
      if (step != null && step instanceof StepFunction) {
        df.prepareStepFunctionVarsForUpdate(object.getIdentifiedVariables());
      }
    }
    insert();
    
    FQName fqName = modification.getObject().getFQName();
    ReferenceInvalidatedNotification notification = new ReferenceInvalidatedNotification(fqName, object.getRoot().getWorkflow());
    modification.getSession().getWFWarningsHandler(fqName).handleChange(object.getId(), notification);

  }

  @Override
  protected void modifyWorkflow(WF workflow) throws UnknownObjectIdException, MissingObjectException, XynaException, InvalidJSONException, UnexpectedJSONContentException, ModificationNotAllowedException {
    insert();
  }

  @Override
  protected void modifyVariable(Variable variable) throws UnknownObjectIdException, MissingObjectException, XynaException, InvalidJSONException, UnexpectedJSONContentException {
    insert();
    
    Step step = object.getStep();
    if(step != null && step instanceof WFStep) {
      FQName fqName = modification.getObject().getFQName();
      ReferenceInvalidatedNotification notification = new ReferenceInvalidatedNotification(fqName, object.getRoot().getWorkflow());
      modification.getSession().getWFWarningsHandler(fqName).handleChange(object.getId(), notification);
    }
  }

  @Override
  protected void modifyFormulaArea(Step step) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    insert();
  }

  @Override
  protected void modifyFormula(FormulaInfo formulaInfo) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    insert();
  }

  @Override
  protected void modifyCase(Case caseInfo) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    insert();

    if (object.getParent().getWorkflow() != null) {
      FQName fqName = modification.getObject().getFQName();
      ReferenceInvalidatedNotification notification = new ReferenceInvalidatedNotification(fqName, object.getRoot().getWorkflow());
      modification.getSession().getWFWarningsHandler(fqName).handleChange(object.getId(), notification);
    }
  }

  @Override
  protected void modifyBranchArea(Step step) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    //add connections for new branch in dataflow
    //otherwise it will take connections from default branch
    ObjectId parentStepId = ObjectId.createStepId(object.getStep());
    IdentifiedVariables identifiedVariables = modification.getObject().getVariableMap().identifyVariables(parentStepId);
    List<AVariableIdentification> outputVarIdents = identifiedVariables.getVariables(VarUsageType.output);
    for (AVariableIdentification outputVarIdent : outputVarIdents) {
      GBSubObject varSubObject = modification.getObject().getObject(outputVarIdent.internalGuiId.createId());
      modification.getObject().getDataflow().addConnectionForBranch(varSubObject, step); 
    }
    
    insert();
  }

  @Override
  protected void modifyCaseArea(Step step) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    insert();
  }

  @Override
  protected void modifyBranch(Branch branchInfo) throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    insert();
  }
  
  @Override
  protected void modifyQuerySelectionMaskArea()
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    insert();
  }
  
  private void insert() throws UnknownObjectIdException, MissingObjectException, XynaException, InvalidJSONException, UnexpectedJSONContentException { 
    Optional<FromXmlJson> fromXmlJson = tryParseAsFromXml();
    if(fromXmlJson.isPresent()) {
      insertFromXml((FromXmlJson) fromXmlJson.get());
      return;
    }
    
    Insertion insertion = new Insertion(object, insert);
    insertion.wrapWhenNeeded(modification.getObject()); // Hinzufuegen von fuer Insert evtl. benoetigten Wrapper-Schritten
    insertion.inferWhere(object); // Feststellen wohin inserted wird, dies ist leider nicht eindeutig - TODO: muss bei Undo auch wieder entfernt werden
    insertion.inferPossibleContent(); // Überlegen was im Content stehen könnte, dies ist leider nicht eindeutig
    Pair<PossibleContent, ? extends XMOMGuiJson> content = insertion.parseContent(insert); // Parsen der möglichen Contents


    
    if(isInsertQuery(content, modification.getObject().getGenerationBase().getRevision())) {
      insertQuery(content);
    } else {
      GBBaseObject objectToCreate = createNewObject(insertion.getParent(), content, insert);
      insertion.insert(objectToCreate);
  
      insertion.updateParentsWhenNeeded(object, modification.getObject());
      updateAbstractness(objectToCreate.getDtOrException());
    }
  }


  private Optional<FromXmlJson> tryParseAsFromXml() {
    FromXmlJson result = null;
    try {
      insert.parseContent(com.gip.xyna.xact.filter.session.modify.Insertion.PossibleContent.fromXml.getJsonVisitor());
      result = (FromXmlJson) insert.getContent();
      return Optional.of(result);
    } catch (InvalidJSONException | UnexpectedJSONContentException e) {
      return Optional.empty();
    }
  }


  private void insertStepFromXml(GenerationBaseObject gbo) {
    Step actualStep = findActualStepToInsertXmlStepAfter(gbo);
    StepSerial targetStepSerial = null;
    
    if (actualStep instanceof StepSerial && !(actualStep instanceof StepParallel)) {
      targetStepSerial = (StepSerial) actualStep;
      if (targetStepSerial.getChildSteps().size() > 0) {
        actualStep = targetStepSerial.getChildSteps().get(targetStepSerial.getChildSteps().size() - 1);
      } else {
        actualStep = null;
      }
    } else {
      targetStepSerial = (StepSerial) actualStep.getParentStep();

    }
    int index = actualStep == null ? targetStepSerial.getChildSteps().size() : targetStepSerial.getChildSteps().indexOf(actualStep) + 1;
    List<RepairEntry> repairEntries = StepCopier.copyStepInto(gbo, index, targetStepSerial, modification);
    for (RepairEntry entry : repairEntries) {
      Hint hint = HintGeneration.convertRepairEntryToHint(entry);
      modification.addHint(hint);
    }
  }
  
  
  private Step findActualStepToInsertXmlStepAfter(GenerationBaseObject gbo) {
    Step actualStep = null;
    if (object.getType() == ObjectType.workflow) {
      List<Step> steps = object.getWorkflow().getWfAsStep().getChildStep().getChildSteps();
      actualStep = steps.get(steps.size() - 1);
    } else if(object.getType() == ObjectType.distinctionBranch) {
      actualStep = determineStepFromDistinctionBranch(object.getStep(), object.getObjectId());
    } else if(object.getType() == ObjectType.variable){
      actualStep = determineStepFromVariable(object.getStep(), object.getObjectId());
    } else {
      actualStep = determineStepInsideRegularStepSerial(object.getStep());
    }
    
    
    return actualStep;
  }


  private Step determineStepFromVariable(Step step, String objectId) {

    if (step instanceof StepChoice) {
      StepChoice stepChoice = (StepChoice) step;
      String[] parts = ObjectId.split(objectId);

      if (parts.length != 2 || !parts[1].startsWith("in")) {
        return step;
      }

      if (stepChoice.getDistinctionType() == DistinctionType.TypeChoice) {
        int caseNo;
        try {
          String idPlusOneAsString = parts[1].substring(2);
          caseNo = Integer.parseInt(idPlusOneAsString) - 1; // one for typeChoice input
        } catch (NumberFormatException e) {
          return step;
        }
        int branchNo = stepChoice.getBranchNo(caseNo);
        return stepChoice.getChildSteps().get(branchNo);
      } else if (stepChoice.getDistinctionType() == DistinctionType.ConditionalChoice) {
        int childNo;
        try {
          String ChildNoAsString = parts[1].substring(2);
          childNo = Integer.parseInt(ChildNoAsString);
        } catch (NumberFormatException e) {
          return step;
        }
        return stepChoice.getChildSteps().get(childNo);
      }
    }
    
    
    return step;
  }

  private Step determineStepFromDistinctionBranch(Step step, String objectId) {
    int id = -1;
    Step result = null;
    try {
      String[] parts = ObjectId.split(objectId);
      String idString = parts[parts.length - 1];
      id = Integer.parseInt(idString);
    } catch (NumberFormatException e) {
      throw new RuntimeException(e);
    }

    if (step instanceof WFStep) {
      //find global catch StepSerial member
      step = step.getChildSteps().get(1);
    }

    if (step instanceof StepCatch) {
      id++; //skip step in try block
    }

    Step stepSerial = step.getChildSteps().get(id);
    List<Step> childSteps = stepSerial.getChildSteps();
    result = childSteps.size() > 0 ? childSteps.get(childSteps.size() - 1) : stepSerial;

    return result;
  }

  private Step determineStepInsideRegularStepSerial(Step step) {
    Step parentStep = step.getParentStep();

    if (step instanceof StepSerial && !(parentStep instanceof ForEachScopeStep) && !(step instanceof StepParallel)) {
      return step;
    }
    
    if (parentStep instanceof StepSerial) {
      Step grandParentStep = parentStep.getParentStep();
      if (grandParentStep instanceof ForEachScopeStep) {
        return determineStepInsideRegularStepSerial(grandParentStep.getParentStep());
      } else {
        return step;
      }
    }

    if (step instanceof StepFunction && parentStep instanceof StepCatch) {
      return determineStepInsideRegularStepSerial(parentStep);
    }

    throw new RuntimeException("Could not determine StepSerial to insert step into.");
  }


  private void insertVariableFromXml(GenerationBaseObject gbo) {
    AVariable avarToInsert = gbo.getWFStep().getInputVars().get(0);
    avarToInsert.setId("" + object.getRoot().getWorkflow().getNextXmlId());
    DirectVarIdentification toInsert = DirectVarIdentification.of(avarToInsert);

    List<AVariableIdentification> listAdapter = findListAdapter(object);
    AVariableIdentification varToFind = object.getVariable() == null ? null : object.getVariable().getVariable();
    int index = findIndexInListAdapter(listAdapter, varToFind);
    if (index == -1) { //variable not in list
      index = listAdapter.size() - 1; //insert at back
    }
    index++;
    listAdapter.add(index, toInsert);
  }

  
  private int findIndexInListAdapter(List<AVariableIdentification> listAdapter, AVariableIdentification varToFind) {
    try {
      int index = listAdapter.indexOf(varToFind);
      return index;
    } catch (Exception e) {
      return -1;
    }
  }

  //if obj is variable, return VariableListAdapter containing it
  //if obj is step, return InpuVariableListAdapter
  private List<AVariableIdentification> findListAdapter(GBSubObject obj) {
    ObjectType type = null;
    try {
      type = ObjectId.parse(obj.getObjectId()).getType();
    } catch (UnknownObjectIdException e) {
      throw new RuntimeException(e);
    }
    if (type == ObjectType.step || type == ObjectType.workflow) {
      return obj.getIdentifiedVariables().getListAdapter(VarUsageType.input);
    } else {
      IdentifiedVariables idVars = obj.getVariable().getIdentifiedVariables();
      if (idVars.getListAdapter(VarUsageType.output).contains(obj.getVariable().getVariable())) {
        return idVars.getListAdapter(VarUsageType.output);
      }
      return idVars.getListAdapter(VarUsageType.input);
    }

  }

  
  private void insertFromXml(FromXmlJson content) {
    String xml = content.getXml();
    GenerationBaseObject gbo = createGboFromXml(xml);
    if (xmlContainingStep(gbo)) {
      insertStepFromXml(gbo);
    } else {
      insertVariableFromXml(gbo);
    }
  }

  private GenerationBaseObject createGboFromXml(String xml) {
    XMOMLoader loader = object.getRoot().getXmomLoader();
    FQName fqName;
    try {
      fqName = new FQName(object.getRoot().getFQName().getRevision(), "tmp.cpy");
    } catch (XFMG_NoSuchRevision e1) {
      throw new RuntimeException(e1);
    }
    GenerationBaseObject gbo;
    try {
      gbo = loader.load(fqName, xml);
    } catch (XynaException e) {
      throw new RuntimeException("could not load object from clipboard.", e);
    }
    return gbo;
  }


  private boolean xmlContainingStep(GenerationBaseObject gbo) {
    return gbo.getWorkflow().getInputVars().size() == 0;
  }

  private void insertQuery(Pair<PossibleContent, ? extends XMOMGuiJson> content) throws UnknownObjectIdException, MissingObjectException, XynaException, UnexpectedJSONContentException {
    GBSubObject relativeToObject = object;
    GBBaseObject service = null;

    if (insert.getRequestInsideIndex() != null) {
      insert.setInsideIndex(insert.getRequestInsideIndex()); // reset, da inferWhere() bereits aufgerufen wurde
    }

    // ConditionMapping
    GBBaseObject mapping = createQueryMapping(relativeToObject);
    refreshDataFlow();

    if (RelativePosition.left == insert.getRelativePosition() || RelativePosition.right == insert.getRelativePosition()) {
      insert.setRelativePosition(RelativePosition.bottom);
      relativeToObject = modification.getObject().getObject(ObjectId.createStepId(mapping.getStep()).getObjectId());
    }
    
    service = createQueryStepFunction(relativeToObject, content);
    
    refreshDataFlow();
    
    // ConditionMapping output variable
    insert.setInsideIndex(0);
    insert.setRelativePosition(RelativePosition.inside);
    createQueryMappingOutput(mapping);
    refreshDataFlow();    

    // create connection between mapping output and service function input
    createQueryConnection(mapping, service);
    refreshDataFlow();
    
    // set constant QueryParameter and SelectionMask
    createQueryConstants(service);
    
  }
  
  private GBBaseObject createQueryMapping(GBSubObject relativeToObject) throws XynaException {
    Insertion insertion = new Insertion(relativeToObject, insert);
    insertion.setQueryInsertStep(QueryInsertStep.mapping);
    prepareInsertion(insertion, relativeToObject);
    Pair<PossibleContent, ? extends XMOMGuiJson> content = Pair.of(PossibleContent.mapping, MappingJson.hiddenQueryMapping());
    GBBaseObject mapping = createNewObject(insertion.getParent(), content, insert);
    executeInsertion(insertion, mapping);
    modification.getObject().getStepMap().addStep(mapping.getStep());
    return mapping;
  }
  
  private GBBaseObject createQueryStepFunction(GBSubObject relativeToObject, Pair<PossibleContent, ? extends XMOMGuiJson> orgContent) throws XynaException, UnexpectedJSONContentException {
    Insertion insertion = new Insertion(relativeToObject, insert);
    insertion.setQueryInsertStep(QueryInsertStep.function);
    prepareInsertion(insertion, relativeToObject);
    
    String label = "Query";
    
    if(orgContent.getSecond() instanceof ServiceJson) {
      label = ((ServiceJson)orgContent.getSecond()).getLabel();
    } else if(orgContent.getSecond() instanceof QueryJson) {
      label = ((QueryJson)orgContent.getSecond()).getLabel();
    }
    
    ServiceJson serviceJson = new ServiceJson(label);
    FQNameJson fqName = null;
    
    if (orgContent.getFirst() == PossibleContent.query) {
      fqName = FQNameJson.parseAttribute(null, Tags.FQN, PersistenceServices.class.getName());
      fqName = FQNameJson.parseAttribute(fqName, Tags.OPERATION, QUERY_SERVICE_OPERATION);
    } else {
      fqName = ((ServiceJson)orgContent.getSecond()).getFQName();
    }
    
    serviceJson.setFQName(fqName);
    
    Pair<PossibleContent, ? extends XMOMGuiJson> content = Pair.of(PossibleContent.service, serviceJson);
    insertion.setActualContent(PossibleContent.service);
    GBBaseObject service = createNewObject(insertion.getParent(), content, insert);
    executeInsertion(insertion, service);
    modification.getObject().getStepMap().addStep(((StepCatch)service.getStep()).getStepInTryBlock());
    return service;
  }
  
  
  private boolean isInsertQuery(Pair<PossibleContent, ? extends XMOMGuiJson> content, Long revision) {
    if(content.getFirst().equals(PossibleContent.query)) {
      return true;
    }
    
    if(!content.getFirst().equals(PossibleContent.service)) {
      return false;
    }

    ServiceJson sj = (ServiceJson) content.getSecond();
    
    if(sj.isPrototype()) {
      return false;
    }
    
    //check if operation has specialPurposeIdentifier query
    FQNameJson fqn = sj.getFQName();
    String completeName = fqn.getTypePath() + "." + fqn.getTypeName();
    
    if (fqn.getOperation() != null && fqn.getOperation().length() > 0) {
      try {
        DOM dom = DOM.getOrCreateInstance(completeName, new GenerationBaseCache(), revision);
        dom.parse(false);
        return dom.getOperationByName(fqn.getOperation()).getSpecialPurposeIdentifier() == SpecialPurposeIdentifier.QUERY_STORABLE;
      } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException
          | XPRC_MDMDeploymentException | XPRC_OperationUnknownException e) {
      }
    }
    
    try {
      WF wf = WF.getOrCreateInstance(completeName, new GenerationBaseCache(), revision);
      wf.parse(false);
      return wf.getSpecialPurposeIdentifier() == SpecialPurposeIdentifier.QUERY_STORABLE;
    } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException
        | XPRC_MDMDeploymentException e) {
      return false;
    }
  }
  
  
  private void createQueryMappingOutput(GBBaseObject mapping) throws UnknownObjectIdException, MissingObjectException, XynaException, UnexpectedJSONContentException {
    String mappingOutputId = ObjectId.createStepId(mapping.getStep(), ObjectPart.output).getObjectId();
    GBSubObject mappingOutputGbSubObject = modification.getObject().getObject(mappingOutputId);
    
    VariableJson variableJson = new VariableJson(Tags.VARIABLE, "Filter condition", FQNameJson.parseAttribute(null, Tags.FQN, FilterCondition.class.getName()));
    variableJson.setList(false);
    Pair<PossibleContent, ? extends XMOMGuiJson> content = Pair.of(PossibleContent.variable, variableJson);
    Insertion insertion = new Insertion(mappingOutputGbSubObject, insert);
    insertion.setQueryInsertStep(QueryInsertStep.mappingOutput);
    prepareInsertion(insertion, null);
    insertion.setActualContent(PossibleContent.variable);
    GBBaseObject variable = createNewObject(modification.getObject().getObject(mappingOutputId), content, insert);
    executeInsertion(insertion, variable);
  }
  
  private void createQueryConnection(GBBaseObject mapping, GBBaseObject service) throws UnknownObjectIdException, MissingObjectException, XynaException {
    String mappingOutputVarId = ObjectId.createVariableId(mapping.getStep().getStepId(), VarUsageType.output, 0);
    GBSubObject source = modification.getObject().getObject(mappingOutputVarId);
    
    GBSubObject stepFunctionInput = modification.getObject().getObject(ObjectId.createStepId(((StepCatch)service.getStep()).getStepInTryBlock(), ObjectPart.input).getObjectId());
    List<AVariableIdentification> variableIdentifications = stepFunctionInput.getIdentifiedVariables().getVariables(VarUsageType.input);
    for(int i = 0; i < variableIdentifications.size(); i++) {
      AVariableIdentification av = variableIdentifications.get(i);
      if(FilterCondition.class.getName().equals(av.getIdentifiedVariable().getFQClassName())) {
        GBSubObject target = modification.getObject().getObject(ObjectId.createVariableId(((StepCatch)service.getStep()).getStepInTryBlock().getStepId(), VarUsageType.input, i));
        modification.getObject().getDataflow().addUserConnection(source, target, null);
        
        //set source and target of target
        AVariable tvar = service.getStep().getInputVars().get(1);
        tvar.setSourceIds(mapping.getStep().getStepId());
        tvar.setTargetId(service.getStep().getStepId());
            
        break;
      }
    }
  }
  
  private void createQueryConstants(GBBaseObject service) throws UnknownObjectIdException, MissingObjectException, XynaException {
    String stepFunctionInputId = ObjectId.createStepId(((StepCatch)service.getStep()).getStepInTryBlock(), ObjectPart.input).getObjectId();
    GBSubObject stepFunctionInputGbSubObject = modification.getObject().getObject(stepFunctionInputId);
    
    QueryParameter queryParameter = new QueryParameter(-1, false, Collections.emptyList());
    SelectionMask selectionMask = new SelectionMask(Storable.class.getName(), Collections.emptyList());
    
    List<AVariableIdentification> variableIdentifications = stepFunctionInputGbSubObject.getIdentifiedVariables().getVariables(VarUsageType.input);
    for (AVariableIdentification av : variableIdentifications) {
      if(SelectionMask.class.getName().equals(av.getIdentifiedVariable().getFQClassName())) {
        modification.getObject().getDataflow().setConstantValue(av, null, selectionMask);
      }
      if(QueryParameter.class.getName().equals(av.getIdentifiedVariable().getFQClassName())) {
        modification.getObject().getDataflow().setConstantValue(av, null, queryParameter);
      }
    }
  }
  
  private void prepareInsertion(Insertion insertion, GBSubObject relativeToObject) {
    insertion.inferWhere(relativeToObject);
    insertion.inferPossibleContent();
  }
  
  private void executeInsertion(Insertion insertion, GBBaseObject newObject) {
    insertion.insert(newObject);
    insertion.updateParentsWhenNeeded(newObject, modification.getObject());
  }
  
  @Override
  protected void modifyQueryFilterArea(Step step)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    insert();
  }
  
  @Override
  protected void modifyQuerySortingArea(Step step)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    insert();
  }
  
  @Override
  protected void modifyQueryFilter(QueryFilterCriterion queryFilterCriterion)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    insert();
  }

  @Override
  protected void modifyMemberVarArea(DomOrExceptionGenerationBase dtOrException) throws UnknownObjectIdException, MissingObjectException, XynaException, InvalidJSONException, UnexpectedJSONContentException {
    insert();
  }

  @Override
  protected void modifyMemberMethodArea(DOM dom) throws UnsupportedOperationException, UnknownObjectIdException, MissingObjectException, XynaException, InvalidJSONException, UnexpectedJSONContentException {
    insert();
  }


  @Override
  public void modifyMethodVarArea(DomOrExceptionGenerationBase dtOrException) throws UnsupportedOperationException, UnknownObjectIdException, MissingObjectException, XynaException, InvalidJSONException, UnexpectedJSONContentException, ModificationNotAllowedException {
    insert();
  }

  @Override
  public void modifyLibs(DomOrExceptionGenerationBase dtOrException) throws UnknownObjectIdException, MissingObjectException, XynaException, InvalidJSONException, UnexpectedJSONContentException {
    insert();
  }
  
  @Override
  protected void modifyRemoteDestination(StepFunction step) {
    ServiceUtils.setRemoteDestination(step, insert.getName());
  }

}
