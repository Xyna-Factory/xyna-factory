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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.session.Clipboard;
import com.gip.xyna.xact.filter.session.Clipboard.ClipboardCopyDirection;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.XMOMLoader;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.exceptions.UnsupportedOperationException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberVarInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Variable;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectId.ObjectPart;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepChoice;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepForeach;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepFunction;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepMapping;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepRetry;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepThrow;
import com.gip.xyna.xact.filter.session.modify.Insertion;
import com.gip.xyna.xact.filter.session.modify.Insertion.PossibleContent;
import com.gip.xyna.xact.filter.session.modify.Insertion.QueryInsertStep;
import com.gip.xyna.xact.filter.session.modify.operations.copy.StepCopier;
import com.gip.xyna.xact.filter.session.repair.XMOMRepair;
import com.gip.xyna.xact.filter.session.workflowwarnings.ReferenceInvalidatedNotification;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.StepVariableIdProvider;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.DirectVarIdentification;
import com.gip.xyna.xact.filter.util.HintGeneration;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.ServiceVarIdentification;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xact.filter.xmom.workflows.json.CopyJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.InsertJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.MappingJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.PositionJson.RelativePosition;
import com.gip.xyna.xact.filter.xmom.workflows.json.ServiceJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.UpdateResponseJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.VariableJson;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.BranchInfo;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.CaseInfo;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.HasDocumentation;
import com.gip.xyna.xprc.xfractwfe.generation.HasMetaTags;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;
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

import xmcp.processmodeller.datatypes.RepairEntry;
import xmcp.xact.modeller.Hint;
import xnwh.persistence.FilterCondition;
import xnwh.persistence.PersistenceServices;
import xnwh.persistence.QueryParameter;
import xnwh.persistence.SelectionMask;
import xnwh.persistence.SortCriterion;
import xnwh.persistence.Storable;

public class CopyOperation extends ModifyOperationBase<CopyJson> {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(CopyOperation.class);

  private CopyJson copy;
  private Map<AVariableIdentification, AVariableIdentification> variableCloneMap; // original variable -> cloned variable
  private Clipboard clipboard;
  private ClipboardCopyDirection clipboardCopyDirection = null;

  public CopyOperation() {
    this(null, null);
  }


  public CopyOperation(Clipboard clipboard, ClipboardCopyDirection clipboardCopyDirection) {
    super();
    variableCloneMap = new HashMap<AVariableIdentification, AVariableIdentification>();
    this.clipboard = clipboard;
    this.clipboardCopyDirection = clipboardCopyDirection;
  }


  public void setVariableCloneMap(Map<AVariableIdentification, AVariableIdentification> varCloneMap) {
    variableCloneMap = varCloneMap;
  }


  @Override
  public int parseRequest(String jsonRequest) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();

    copy = jp.parse(jsonRequest, CopyJson.getJsonVisitor());

    return copy.getRevision();
  }
  
  @Override
  protected void modifyStep(Step step) throws Exception {
    copy();
    
    FQName fqName = modification.getObject().getFQName();
    ReferenceInvalidatedNotification notification = new ReferenceInvalidatedNotification(fqName, object.getRoot().getWorkflow());
    modification.getSession().getWFWarningsHandler(fqName).handleChange(object.getId(), notification);
  }

  @Override
  protected void modifyVariable(Variable variable) throws Exception {
    copy();
    
    if (object.getRoot().getGenerationBase() instanceof WF) {
      FQName fqName = modification.getObject().getFQName();
      ReferenceInvalidatedNotification notification = new ReferenceInvalidatedNotification(fqName, object.getRoot().getWorkflow());
      modification.getSession().getWFWarningsHandler(fqName).handleChange(object.getId(), notification);
    }
  }

  @Override
  protected void modifyMemberVar(DomOrExceptionGenerationBase dtOrException, MemberVarInfo memberVarInfo) throws Exception {
    copy();
  }


  public static AVariable createCopyOfVariable(AVariable orgAVar, GenerationBase creatorOfCopy) {
    AVariable cpyAVar = null;
    if (orgAVar.isPrototype()) {
      cpyAVar = new DatatypeVariable(creatorOfCopy);
      cpyAVar.createPrototype(orgAVar.getLabel());
      cpyAVar.setIsList(orgAVar.isList());
    } else if (orgAVar.getDomOrExceptionObject() == null) {
      cpyAVar = AVariable.createAnyType(creatorOfCopy, orgAVar.isList());
    } else {
      cpyAVar = AVariable.createAVariable("" + creatorOfCopy.getNextXmlId(), orgAVar.getDomOrExceptionObject(), orgAVar.isList());
    }

    cpyAVar.setLabel(orgAVar.getLabel());
    if (orgAVar.getUnknownMetaTags() != null) {
      List<String> unknownMetaTags = new ArrayList<String>(orgAVar.getUnknownMetaTags());
      cpyAVar.setUnknownMetaTags(unknownMetaTags);
    }
    if (orgAVar.getPersistenceTypes() != null) {
      Set<PersistenceTypeInformation> persistenceTypes = new HashSet<PersistenceTypeInformation>(orgAVar.getPersistenceTypes());
      cpyAVar.setPersistenceTypes(persistenceTypes);
    }

    return cpyAVar;
  }


  private GenerationBaseObject createClipboardGbo() throws XPRC_InvalidPackageNameException {
    XMOMLoader loader = object.getRoot().getXmomLoader();
    FQName fqName = new FQName(-1l, null, "tmp", "clipboardWF");
    GenerationBaseObject gbo = loader.createNewWorkflow("clipboardWF", fqName);
    return gbo;
  }


  private void copyVariableToClipboard(AVariable orgAVar) throws XPRC_InvalidPackageNameException, UnknownObjectIdException {
    GenerationBaseObject gbo = createClipboardGbo();
    WF wf = gbo.getWFStep().getWF();
    String baseId = "step";
    ObjectId objectId = ObjectId.parse(ObjectId.createVariableId(baseId, VarUsageType.input, 0));
    AVariable cpyAVar = createCopyOfVariable(orgAVar, wf);
    Variable cpyVar = new Variable(null, DirectVarIdentification.of(cpyAVar));
    GBSubObject gbsub = new GBSubObject(gbo, objectId, wf.getWfAsStep(), cpyVar);
    addToClipboard(gbsub);
  }


  private void copyStepToClipboard() throws XPRC_InvalidPackageNameException {
    GenerationBaseObject gbo = createClipboardGbo();
    WF wf = gbo.getWFStep().getWF();
    gbo.createDataflow();
    wf.getWfAsStep().getChildStep().getChildSteps().clear(); //remove stepAssign

    GBSubObject gbsub = StepCopier.copyStepIntoGenerationBaseObject(gbo, object);
    addToClipboard(gbsub);
  }


  private void addToClipboard(GBSubObject gbsub) {
    Clipboard.ClipboardEntry result = clipboard.new ClipboardEntry();
    result.setObject(gbsub);
    if (object.getRoot().getSaveState()) {
      result.setFqn(object.getRoot().getOriginalFqName());
      result.setRevision(object.getRoot().getFQName().getRevision());
    }

    clipboard.addEntry(copy.getInsideIndex(), result);
  }


  private void copyFromClipboard() throws Exception {
    if (object.getType() == ObjectType.variable) {
      copyVariableFromClipboard();
      return;
    } else if (object.getType() == ObjectType.step) {
      copyStepFromClipboard();
      return;
    }

    throw new UnsupportedOperationException("copy", "unexpected object of type " + object.getType() + " in clipboard.");
  }
  
  
  private void copyVariableFromClipboard() throws Exception {

    clipboard.resetHints();

    AVariable avar = object.getVariable().getVariable().getIdentifiedVariable();
    if (XMOMRepair.variableHasToBeConverted(avar)) {
      Hint hint = new Hint();
      hint.setDescription("Variable converted to prototype.");
      clipboard.getHints().add(hint);
    }

    GBSubObject relativeToObject = modification.getObject().getObject(copy.getRelativeTo());
    if (relativeToObject.getType() == ObjectType.memberVarArea) {
      // convert variable to member variable
      object = new GBSubObject(object.getRoot(), new ObjectId(ObjectType.memberVar, object.getId().getBaseId()), relativeToObject.getDtOrException(), new MemberVarInfo(0), object.getVariable());
    }

    executeCopy();
  }
  
  
  private void copyStepFromClipboard() throws Exception {
    GBSubObject gbSubObject = modification.getObject().getObject(copy.getRelativeTo());
    List<RepairEntry> repairEntries = Clipboard.copyStepFromClipboard(object, gbSubObject, modification, copy.getInsideIndex(), copy);

    clipboard.resetHints();
    for (RepairEntry entry : repairEntries) {
      Hint hint = HintGeneration.convertRepairEntryToHint(entry);
      clipboard.getHints().add(hint);
    }
  }
  

  private void copyToClipboard() throws Exception {
    if (object.getType() == ObjectType.variable) {
      boolean isThisVar = ( (object.getDtOrException() instanceof DOM) &&
          (modification.getObject().getViewType() == com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type.dataType) &&
          (object.getVariable().getUsage() == VarUsageType.input && object.getVariable().getIndex() == 0 ) );
      if (!modification.getObject().getSaveState() && isThisVar) {
        throw new UnsupportedOperationException(UnsupportedOperationException.COPY_TO_CLIPBOARD, UnsupportedOperationException.CANNOT_COPY_UNSAVED_THIS_VAR);
      }

      Variable orgVar = object.getVariable();object.getMemberVarInfo();
      AVariable orgAVar = orgVar.getVariable().getIdentifiedVariable();

      copyVariableToClipboard(orgAVar);
      return;
    }

    if (object.getType() == ObjectType.memberVar) {
      AVariable orgAVar = object.getDtOrException().getAllMemberVarsIncludingInherited().get(object.getMemberVarInfo().getIndex());
      if (orgAVar.getDomOrExceptionObject() == null) {
        throw new UnsupportedOperationException(UnsupportedOperationException.COPY_TO_CLIPBOARD, UnsupportedOperationException.CANNOT_COPY_PRIMITIVE_VARIABLE);
      }

      copyVariableToClipboard(orgAVar);
      return;
    }

    if (object.getType() == ObjectType.step) {
      copyStepToClipboard();
      return;
    }

    throw new RuntimeException("Can't add Object of type " + object.getType() + " to clipboard.");
  }


  private void copy() throws Exception {
    if(clipboardCopyDirection != null && clipboardCopyDirection == ClipboardCopyDirection.TO_CLIPBOARD) {
      copyToClipboard();
      return;
    }
    
    if(clipboardCopyDirection != null && clipboardCopyDirection == ClipboardCopyDirection.FROM_CLIPBOARD) {
      copyFromClipboard();
      return;
    }
    
    if(ObjectType.step == object.getType() && isQuery(object)) {
      copyQuery();
      return;
    }

    //PMOD-1016
    if ((object.getType() == ObjectType.step && object.getStep() == null) || object.getType() == ObjectType.remoteDestinationParameter) {
      throw new UnsupportedOperationException(UnsupportedOperationException.COPY_OPERATION, "Variable does not support operation");
    }

    if (object.isStepInTryBlock()) {
      // step is in a try-block -> copy the try-block, instead
      object = object.getParent();
    }

    if(object.getType() == ObjectType.step && object.getStep() instanceof StepCatch) {
      StepCatch stepCatch = (StepCatch)object.getStep();
      GBSubObject objectInTryBlock = modification.getObject().getObject(ObjectId.createStepId(stepCatch.getStepInTryBlock()).getObjectId());
      if(isQuery(objectInTryBlock)) {
        object = objectInTryBlock;
        copyQuery();
        return;
      }
    }

    if (object.isForeach()) {
      // step is a StepForeach -> copy contained step, instead
      Step iteratedStep = object.getStep().getChildSteps().get(0).getChildSteps().get(0).getChildSteps().get(0);
      object = modification.getObject().getObject(ObjectId.createStepId(iteratedStep).getObjectId());
    }

    executeCopy();
  }
  
  
  private void executeCopy() throws Exception {
    GBSubObject relativeToObject = /* object.getRoot().getObject(copy.getRelativeTo()); */ modification.getObject().getObject(copy.getRelativeTo());
    Insertion insertion = new Insertion(relativeToObject, copy);

    insertion.wrapWhenNeeded(modification.getObject()); // Hinzufuegen von fuer Insert evtl. benoetigten Wrapper-Schritten
    insertion.inferWhere(object); // Feststellen wohin inserted wird, dies ist leider nicht eindeutig
    insertion.inferPossibleContent(); // Überlegen was im Content stehen könnte, dies ist leider nicht eindeutig

    if (isOperationInSelf(relativeToObject)) {
      throw new UnsupportedOperationException(UnsupportedOperationException.COPY_OPERATION,
                                              UnsupportedOperationException.COPY_IN_YOURSELF_IS_NOT_POSSIBLE);
    }

    // Special Case: Move a Exception-Variable to a workflow
    if (isSpecialInsertThrow(relativeToObject)) {
      specialInsertThrow(relativeToObject, insertion);
      return;
    }

    insertion.checkContent(object);

    Pair<PossibleContent, ? extends XMOMGuiJson> content = insertion.copyContent(modification.getObject().getView(), object);
    GBBaseObject objectToInsert = createNewObject(insertion.getParent(), content, copy);

    insertion.insert(objectToInsert); // TODO: Response besser erst nach Kopieren von Content erzeugen

    copyContent(object, objectToInsert);
  }


  private void prepareInsertion(Insertion insertion) {
    GBSubObject insideObject = insertion.getInsideObject();
    insertion.inferWhere(insideObject);
    insertion.inferPossibleContent();
  }
  
  private UpdateResponseJson executeInsertion(Insertion insertion, GBBaseObject newObject) {
    UpdateResponseJson response = insertion.insert(newObject);
    insertion.updateParentsWhenNeeded(newObject, modification.getObject());
    return response;
  }

  private void copyContent(GBSubObject source, GBBaseObject destination) throws Exception {
    if (destination.getType() == ObjectType.step) {
      copyVars(source, destination);
      copyChildren(source, destination);
    }
  }

  private void copyVars(GBSubObject source, GBBaseObject destination) throws UnknownObjectIdException, MissingObjectException, XynaException {
    
    GBSubObject sourceSubObject = source;
    if(sourceSubObject.getStep() instanceof StepCatch) {
      StepCatch stepCatch = (StepCatch)sourceSubObject.getStep();
      if(stepCatch.getStepInTryBlock() != null) {
        sourceSubObject = object.getRoot().getObject(ObjectId.createStepId(stepCatch.getStepInTryBlock()).getObjectId());
      }
    }
    
    GBBaseObject destinationGbBaseObject = destination;
    if(destinationGbBaseObject.getStep() instanceof StepCatch) {
      StepCatch stepCatch = (StepCatch)destinationGbBaseObject.getStep();
      destinationGbBaseObject = new GBBaseObject(stepCatch.getStepInTryBlock());
    }
    
    
    //handle internal variables of ForEach
    if(destinationGbBaseObject.getStep() instanceof StepForeach && destination.getStep() instanceof StepForeach) {
      copyVarsStepForeach(source, sourceSubObject, destinationGbBaseObject);
      return;
    }
    
    List<VarUsageType> usageTypes = getRequiredVarUsageType(sourceSubObject);
    AVariableIdentification newVar;
    for (VarUsageType varUsageType : usageTypes) {
      int idx = 0;
      for (AVariableIdentification av : sourceSubObject.getIdentifiedVariables().getVariables(varUsageType)) {
        newVar = copyVar(av, destinationGbBaseObject, ObjectPart.forUsage(varUsageType));
        Optional<AVariableIdentification> opt = setIdProviderForVarCopy(newVar, av, source, destination, idx, varUsageType);
        if (opt.isPresent()) {
          newVar = opt.get();
        }
        variableCloneMap.put(av, newVar);
        source.getRoot().getDataflow().copyConnection(av, newVar, variableCloneMap, destination);
        
        idx++;
      }
    }

    //set input of StepThorow
    if (destinationGbBaseObject.getStep() instanceof StepThrow && destination.getStep() instanceof StepThrow) {
      copyVarsStepThrow(source, sourceSubObject, destinationGbBaseObject);
    }
    
    //copy input of StepFunction (does not create new variables)
    if(destinationGbBaseObject.getStep() instanceof StepFunction) {
      copyConnectionsOfStepFunction(sourceSubObject, destinationGbBaseObject);
    }
  }

  
  private void copyConnectionsOfStepFunction(GBSubObject sourceSubObject, GBBaseObject destinationGbBaseObject) {
    try {
      Dataflow dataflow = sourceSubObject.getRoot().getDataflow();
      Step newStep = destinationGbBaseObject.getStep();
      List<AVariableIdentification> oldVars = sourceSubObject.getIdentifiedVariables().getListAdapter(VarUsageType.input);
      List<AVariableIdentification> newVars = dataflow.identifyVariables(newStep).getListAdapter(VarUsageType.input);
      for (int i = 0; i < oldVars.size(); i++) {
        AVariableIdentification oldVar = oldVars.get(i);
        AVariableIdentification newVar = newVars.get(i);
        dataflow.copyConnection(oldVar, newVar, variableCloneMap, destinationGbBaseObject);
      }
    } catch (Exception e) {
      logger.warn("could not copy connections of stepFunction", e);
    }
  }


  private void copyVarsStepForeach(GBSubObject source, GBSubObject sourceSubObject, GBBaseObject destinationGbBaseObject) {
    StepForeach newStep = (StepForeach)destinationGbBaseObject.getStep();
    StepForeach oldStep = (StepForeach)source.getStep();
    
    //same Input
    List<AVariable> oldInputVars = oldStep.getInputVars();
    int index = 0;
    for(AVariable oldInputVar : oldInputVars) {
      newStep.addInput(oldInputVar);
      AVariable singleInputVar = newStep.getInputVarsSingle()[index];

      AVariableIdentification newVar = ServiceVarIdentification.of(singleInputVar);
      AVariableIdentification oldSingleVarIndet =
          sourceSubObject.getRoot().getVariableMap().identifyVariables(ObjectId.createStepId(oldStep)).getVariable(AVariableIdentification.VarUsageType.input, index);

      Optional<AVariableIdentification> opt = setIdProviderForVarCopy(newVar, oldSingleVarIndet, sourceSubObject, destinationGbBaseObject, 0, VarUsageType.input);
      if (opt.isPresent()) {
        newVar = opt.get();
      }
      
      variableCloneMap.put(oldSingleVarIndet, newVar);
      source.getRoot().getDataflow().copyConnection(oldSingleVarIndet, newVar, variableCloneMap, destinationGbBaseObject);
      index++;
    }
    
    if(newStep.getInputListRefs() == null || newStep.getInputListRefs().length == 0) {
      throw new RuntimeException("InputListRefs");
    }
    
    String[] oldListRefs = oldStep.getInputListRefs();
    for(int i=0; i<oldListRefs.length; i++) {
      newStep.getInputListRefs()[i] = oldListRefs[i];
    }
    
  }
  
  private void copyVarsStepThrow(GBSubObject source, GBSubObject sourceSubObject, GBBaseObject destinationGbBaseObject) {
    StepThrow st = (StepThrow) destinationGbBaseObject.getStep();
    StepThrow oldStep = ((StepThrow) source.getStep());
    if (oldStep.getTargetExceptionVariable() != null && oldStep.getTargetExceptionVariable().getVariable() != null) {
      String oldId = oldStep.getTargetExceptionVariable().getVariable().getId();
      st.setExceptionID(oldId);
      st.getExceptionTypeFqn(); //set variable
      IdentifiedVariablesStepThrow newVars = new IdentifiedVariablesStepThrow(ObjectId.createStepId(st), st);
      IdentifiedVariablesStepThrow oldVars = new IdentifiedVariablesStepThrow(ObjectId.createStepId(oldStep), oldStep);
      AVariableIdentification cpy = newVars.getVariable(VarUsageType.input, 0);
      AVariableIdentification org = oldVars.getVariable(VarUsageType.input, 0);
      Optional<AVariableIdentification> opt = setIdProviderForVarCopy(cpy, org, sourceSubObject, destinationGbBaseObject, 0, VarUsageType.input);
      if (opt.isPresent()) {
        cpy = opt.get();
      }

      Set<AVariableIdentification> oldKeyset = new HashSet<AVariableIdentification>(variableCloneMap.keySet());
      source.getRoot().getDataflow().copyConnection(org, cpy, variableCloneMap, destinationGbBaseObject);
      Set<AVariableIdentification> newKeySet = variableCloneMap.keySet();
      newKeySet.removeAll(oldKeyset);
      for (AVariableIdentification avar : newKeySet) {
        st.setExceptionID(variableCloneMap.get(avar).getIdentifiedVariable().getId());
      }
    }
  }

  private Optional<AVariableIdentification> setIdProviderForVarCopy(AVariableIdentification cpy, AVariableIdentification org,
                                                                    GBBaseObject src, GBBaseObject dst, int idx, VarUsageType type) {

    if (org.idprovider instanceof StepVariableIdProvider) {
      Step step = dst.getStep();
      ObjectId objectId = ObjectId.createStepId(step);
      IdentifiedVariables vars;
      if (step instanceof StepFunction) {
        vars = new IdentifiedVariablesStepFunction(objectId, (StepFunction) step);
      } else if (step instanceof StepChoice) {
        vars = new IdentifiedVariablesStepChoice(objectId, (StepChoice) step);
      } else if (step instanceof StepMapping) {
        vars = new IdentifiedVariablesStepMapping(objectId, (StepMapping) step);
      } else if (step instanceof StepThrow) {
        vars = new IdentifiedVariablesStepThrow(objectId, (StepThrow) step);
      } else if (step instanceof StepRetry) {
        vars = new IdentifiedVariablesStepRetry(objectId, (StepRetry) step);
      } else if (step instanceof StepForeach) {
        vars = new IdentifiedVariablesStepForeach(objectId, (StepForeach) step);
      } else {  
        return Optional.empty(); //stepAssign?
      }
      AVariableIdentification setVar = vars.getVariable(type, idx);
      cpy.idprovider = setVar.idprovider;
      cpy.internalGuiId = setVar.internalGuiId;
      return Optional.of(setVar);
    }
    return Optional.empty();
  }


  private List<VarUsageType> getRequiredVarUsageType(GBSubObject source){
    if(source.getStep() instanceof StepFunction) {
      StepFunction stepFunction = (StepFunction) source.getStep();
      if(!stepFunction.isPrototype()) {
        return Collections.emptyList();
      }
    } else if (source.getStep() instanceof StepChoice) {
      return Arrays.asList(VarUsageType.input);
    } else if (source.getStep() instanceof StepThrow) {
      return Collections.emptyList();
    } else if (source.getStep() instanceof StepParallel) {
      return Collections.emptyList();
    }
    return Arrays.asList(VarUsageType.input, VarUsageType.output);
  }

  private AVariableIdentification copyVar(AVariableIdentification origVar, GBBaseObject destination, ObjectPart destObjectPart) throws UnknownObjectIdException, MissingObjectException, XynaException {
    GBSubObject varObject = object.getRoot().getObject(origVar.internalGuiId.createId());
    VariableJson varJson = new VariableJson(varObject);
    String stepId = destination.getStep().getStepId();
    String destinationId = ObjectId.createId(ObjectType.step, stepId, destObjectPart);
    GenerationBaseObject gbObj = new GenerationBaseObject(new FQName(), destination.getStep().getCreator(), null);
    GBSubObject destinationSubObject = new GBSubObject(gbObj,ObjectId.parse(destinationId), destination.getStep().getParentWFObject());
    GBBaseObject newVarObject = createParameter(destinationSubObject, varJson);
    copyUnknownMetaTags((HasMetaTags)origVar.getIdentifiedVariable(),
                        (HasMetaTags)newVarObject.getVariable().getVariable().getIdentifiedVariable());

    InsertJson varInsertJson = new InsertJson(copy.getRevision(), destinationId, -1);
    Insertion varInsertion = new Insertion(destinationSubObject, varInsertJson);
    varInsertion.inferWhere(varObject);
    varInsertion.inferPossibleContent();
    varInsertion.insert(newVarObject);
    
    return newVarObject.getVariable().getVariable();
  }

  private void copyChildren(GBSubObject source, GBBaseObject destination) throws Exception {
    if (destination.getType() != ObjectType.step) {
      return;
    }
    Step sourceStep = source.getStep();
    Step targetStep = destination.getStep();
    
    if(sourceStep instanceof StepCatch) {
      sourceStep = ((StepCatch)sourceStep).getStepInTryBlock();
    }
    
    if(targetStep instanceof StepCatch) {
      targetStep = ((StepCatch)targetStep).getStepInTryBlock();
    }    
    
    if(sourceStep instanceof HasDocumentation && targetStep instanceof HasDocumentation) {
      ((HasDocumentation)targetStep).setDocumentation(((HasDocumentation)sourceStep).getDocumentation());
    }
    
    copyUnknownMetaTags((HasMetaTags)sourceStep, (HasMetaTags)targetStep);
    
    if(sourceStep instanceof StepFunction && targetStep instanceof StepFunction) {
      copyChildrenOfStepFunction((StepFunction)sourceStep, (StepFunction)targetStep);
    } else if(sourceStep instanceof StepMapping && targetStep instanceof StepMapping) {
      copyStepMapping((StepMapping)sourceStep, (StepMapping)targetStep);
    }
    
    if(sourceStep instanceof StepChoice && targetStep instanceof StepChoice) {
      copyChildrenOfStepChoice((StepChoice)sourceStep, (StepChoice)targetStep);
    }
    
    if(source.getStep() instanceof StepCatch && destination.getStep() instanceof StepCatch) {
      copyChildrenOfStepCatch((StepCatch)source.getStep(), (StepCatch)destination.getStep());
    }
    
    if(source.getStep() instanceof StepParallel && destination.getStep() instanceof StepParallel) {
      copyChildrenOfStepParallel((StepParallel)sourceStep, (StepParallel)targetStep);
    }
    
    if(source.getStep() instanceof StepForeach && destination.getStep() instanceof StepForeach) {
      copyChildrenOfStepForeach((StepForeach)sourceStep, (StepForeach)targetStep);
    }
  }

  public static void copyUnknownMetaTags(HasMetaTags source, HasMetaTags target) {
    List<String> sourceUnknownMetaTags = source.getUnknownMetaTags();
    if(sourceUnknownMetaTags != null) {
      List<String> targetUnknownMetaTags = new ArrayList<>(sourceUnknownMetaTags.size());
      targetUnknownMetaTags.addAll(sourceUnknownMetaTags);
      target.setUnknownMetaTags(targetUnknownMetaTags);
    }
  }

  private void copyChildrenOfStepParallel(StepParallel source, StepParallel target) throws Exception {
    List<Step> chields = source.getChildSteps();
    int laneIndex = 0;
    for (Step step : chields) {
      if(step instanceof StepSerial) {
        if(target.getChildSteps().size() < laneIndex + 1) {
          StepSerial newStep = new StepSerial(target.getParentScope(), target.getCreator());
          newStep.createEmpty();
          target.addChild(laneIndex, newStep);
          object.getRoot().getStepMap().addStep(newStep);

        }
        copyStepSerial((StepSerial)step, (StepSerial)target.getChildSteps().get(laneIndex));
      }
      laneIndex++;
    }
  }
  

  private void copyChildrenOfStepForeach(StepForeach source, StepForeach target) throws Exception {
    copyStepSerial(source.getChildScope().getChildStep(), target.getChildScope().getChildStep());
  }


  private void copyChildrenOfStepCatch(StepCatch source, StepCatch target) throws Exception {
    
    if(source == null) {
      return;
    }
    
    List<BranchInfo> sourceBranchInfos = source.getBranchesForGUI();
    if(sourceBranchInfos == null) {
      return;
    }
    
    for (BranchInfo sourceBranchInfo : sourceBranchInfos) {
      CaseInfo mainCase = sourceBranchInfo.getMainCase();
      String fqn = mainCase.getComplexName();
      target.addBranch(-1, fqn, mainCase.getName());
      List<BranchInfo> targetBranchInfos = target.getBranchesForGUI();
      
      for (BranchInfo targetBranchInfo : targetBranchInfos) {
        if(fqn.equals(targetBranchInfo.getMainCase().getComplexName())) {
          Step sourceExecutedStep = sourceBranchInfo.getMainStep();
          Step targetExecutedStep = targetBranchInfo.getMainStep();
          object.getRoot().getStepMap().addStep(targetExecutedStep);
          
          copyStepSerial((StepSerial)sourceExecutedStep, (StepSerial)targetExecutedStep);
          break;
        }
      }
    }    
  }
  
  private void copyChildrenOfStepChoice(StepChoice source, StepChoice target) throws Exception {
   
    target.replaceOuterCondition(source.getOuterCondition());
    List<BranchInfo> sourceBranchInfos = source.getBranchesForGUI();
    List<BranchInfo> targetBranchInfos = target.getBranchesForGUI();
    
    switch(source.getDistinctionType()) {
      case ConditionalChoice:
        int branchIndex = 0;
        for (BranchInfo sourceBranchInfo : sourceBranchInfos) {
          Step sourceExecutedStep = sourceBranchInfo.getMainStep();
          Step targetExecutedStep = targetBranchInfos.get(branchIndex).getMainStep();
          copyStepSerial((StepSerial)sourceExecutedStep, (StepSerial)targetExecutedStep);
          branchIndex++;
        }
        break;
      case ConditionalBranch:
        branchIndex = 0;
        for (BranchInfo sourceBranchInfo : sourceBranchInfos) {
          
          List<CaseInfo> sourceCaseInfos = sourceBranchInfo.getCases();
          boolean isDefault = false;
          for (CaseInfo caseInfo : sourceCaseInfos) {
            if(caseInfo.isDefault()) {
              isDefault = true;
              break;
            }
          }
          if(!isDefault) {
            CaseInfo mainCase = sourceBranchInfo.getMainCase();
            target.addBranch(branchIndex, mainCase.getComplexName(), mainCase.getName());
            targetBranchInfos = target.getBranchesForGUI();
          }
          if(sourceCaseInfos.size() > 1) {
            int caseIndex = 0;
            for (CaseInfo caseInfo : sourceCaseInfos) {
              if(caseInfo.isMainCaseOfItsBranch()) {
                target.getChildSteps().get(caseIndex).getChildSteps().clear();
              } else {
                target.addCase(branchIndex, caseIndex, caseInfo.getComplexName(), caseInfo.getName());
              }
              caseIndex++;
            }
          }
          
          Step sourceExecutedStep = sourceBranchInfo.getMainStep();
          Step targetExecutedStep = targetBranchInfos.get(branchIndex).getMainStep();
          object.getRoot().getStepMap().addStep(targetExecutedStep);
          
          copyStepSerial((StepSerial)sourceExecutedStep, (StepSerial)targetExecutedStep);
          
          branchIndex++;
        }
        break;
      case TypeChoice:
        for (BranchInfo sourceBranchInfo : sourceBranchInfos) {
          CaseInfo mainCase = sourceBranchInfo.getMainCase();
          boolean found = false;
          int targetBranchIndex = 0;
          for (BranchInfo targetBranchInfo : targetBranchInfos) {
            if(targetBranchInfo.getMainCase().getComplexName().equals(mainCase.getComplexName())) {
              found = true;
              break;
            }
            targetBranchIndex++;
          }
          if(found) {
            Step sourceExecutedStep = sourceBranchInfo.getMainStep();
            Step targetExecutedStep = targetBranchInfos.get(targetBranchIndex).getMainStep();
            object.getRoot().getStepMap().addStep(targetExecutedStep);
            
            copyStepSerial((StepSerial)sourceExecutedStep, (StepSerial)targetExecutedStep);
          }
        }        
        Iterator<BranchInfo> it = targetBranchInfos.iterator();
        int deleteIndex = 0;
        while(it.hasNext()) {
          BranchInfo targetBranchInfo = it.next();
          boolean found = false;
          for (BranchInfo sourceBranchInfo : sourceBranchInfos) {
            if(targetBranchInfo.getMainCase().getComplexName().equals(sourceBranchInfo.getMainCase().getComplexName())) {
              found = true;
              break;
            }
          }
          if(!found) {
            it.remove();
            target.removeBranch(deleteIndex);
          } else {
            deleteIndex++;
          }
        }
        break;
      case ExceptionHandling:
        
        break;
      default:
        break;
    }
    
    object.getRoot().getVariableMap().refreshIdentifiedVariables(ObjectId.createStepId(target));
  }
  
  private void copyStepSerial(StepSerial source, StepSerial target) throws Exception {

    final String targetObjectId = ObjectId.createStepId(target).getObjectId();
    
    List<Step> steps = source.getChildSteps();
    for (Step step : steps) {
      if(step instanceof StepAssign) {
        continue;
      }
      if(step instanceof StepMapping && ((StepMapping)step).isConditionMapping()){
        continue;
      }
      GBSubObject gbSubObject = object.getRoot().getObject(ObjectId.createStepId(step).getObjectId());
      if(gbSubObject != null) {
        gbSubObject.getRoot().getStepMap().addStep(step);
        
        CopyOperation copyOperation = new CopyOperation();
        copyOperation.setVariableCloneMap(variableCloneMap);
        copyOperation.copy = new CopyJson(copy.getRevision(), targetObjectId, -1);
        copyOperation.modify(modification, gbSubObject);
      }
    }
  }
  
  
  private void copyStepMapping(StepMapping source, StepMapping target) {
    target.setIsConditionMapping(source.isConditionMapping());
    target.setLabel(source.getLabel());    
    
    if(target.isTemplateMapping() && target.getFormulaCount() == 1) {
      target.removeFormula(0); // remove initial formula
    }
    
    for(int i = 0; i < source.getFormulaCount(); i++) {
      target.addFormula(i, source.getFormula(i));
    }
  }
  
  private void copyChildrenOfStepFunction(StepFunction source, StepFunction target) throws Exception {
    target.setLabel(source.getLabel());
    target.setFreesCapacities(source.isFreeCapacitiesTaggable());
    target.setOverrideCompensation(source.getCompensateStep() != null);
    
    target.setExecutionDetached(source.isExecutionDetached());
    target.setOrderInputSourceRef(source.getOrderInputSourceRef());
    
    
    if(source.getCompensateStep() instanceof StepSerial && target.getCompensateStep() instanceof StepSerial) {
      object.getRoot().getStepMap().addStep(target.getCompensateStep());
      copyStepSerial((StepSerial)source.getCompensateStep(), (StepSerial)target.getCompensateStep());
    }
  }
  
  private void copyQuery() throws Exception {
    if (clipboardCopyDirection != null) {
      throw new UnsupportedOperationException(UnsupportedOperationException.COPY_OPERATION, UnsupportedOperationException.COPY_QUERY_TO_CLIPBOARD_NOT_SUPPORTED);
    }

    GBSubObject sourceStepFunctionGBSubObject = object;
    StepFunction sourceStepFunction = (StepFunction)sourceStepFunctionGBSubObject.getStep();
    StepMapping sourceStepMapping = QueryUtils.findQueryHelperMapping(sourceStepFunctionGBSubObject);
    GBSubObject sourceMappingGBSubObject = object.getRoot().getObject(ObjectId.createStepId(sourceStepMapping).getObjectId());
    
    GBBaseObject targetMappingGBBaseObject = createQueryMapping(object.getRoot().getObject(copy.getRelativeTo()));
    refreshDataFlow();
    
    if (RelativePosition.left == copy.getRelativePosition() || RelativePosition.right == copy.getRelativePosition()) {
      copy.setRelativePosition(RelativePosition.bottom);
      copy.setRelativeTo(ObjectId.createStepId(targetMappingGBBaseObject.getStep()).getObjectId());
    }
    GBBaseObject targetStepCatchGBBaseObject = createQueryStepFunction(object.getRoot().getObject(copy.getRelativeTo()));
    StepCatch targetStepCatch = (StepCatch)targetStepCatchGBBaseObject.getStep();
    GBSubObject targetStepFunctionGBSubObject = object.getRoot().getObject(ObjectId.createStepId((StepFunction) targetStepCatch.getStepInTryBlock()).getObjectId());
    StepFunction targetStepFunction = (StepFunction) targetStepFunctionGBSubObject.getStep();
    refreshDataFlow();
    
    // Copy compensation
    if(sourceStepFunction.getCompensateStep() instanceof StepSerial) {
      targetStepFunction.setOverrideCompensation(true);
      object.getRoot().getStepMap().addStep(targetStepFunction.getCompensateStep());
      copyStepSerial((StepSerial)sourceStepFunction.getCompensateStep(), (StepSerial)targetStepFunction.getCompensateStep());
    }
    
    // Exception area
    copyChildrenOfStepCatch((StepCatch) sourceStepFunctionGBSubObject.getStep().getParentStep(), targetStepCatch);
    
    // ConditionMapping output variable
    copy.setInsideIndex(0);
    copy.setRelativePosition(RelativePosition.inside);
    createQueryMappingOutput(targetMappingGBBaseObject);
    refreshDataFlow();
    
    // create connection between mapping output and service function input
    createQueryConnection(targetMappingGBBaseObject, targetStepCatchGBBaseObject);
    refreshDataFlow();
    
    // set constant QueryParameter and SelectionMask
    createQueryConstants(targetStepCatchGBBaseObject);

    // copy only input vars
    int idx = 0;
    for (AVariableIdentification input : sourceMappingGBSubObject.getIdentifiedVariables().getVariables(VarUsageType.input)) {
      AVariableIdentification newVar = copyVar(input, targetMappingGBBaseObject, ObjectPart.forUsage(VarUsageType.input));
      Optional<AVariableIdentification> opt = setIdProviderForVarCopy(newVar, input, sourceMappingGBSubObject, targetMappingGBBaseObject, idx, VarUsageType.input);
      if (opt.isPresent()) {
        newVar = opt.get();
      }
      variableCloneMap.put(input, newVar);
      sourceMappingGBSubObject.getRoot().getDataflow().copyConnection(input, newVar, variableCloneMap, targetMappingGBBaseObject);
      idx++;
    }
    
    copyChildren(sourceMappingGBSubObject, targetMappingGBBaseObject);
    
    List<String> filterConditions = sourceStepFunction.getQueryFilterConditions();
    if(targetStepFunction.getQueryFilterConditions() == null) {
      targetStepFunction.setQueryFilterConditions(new ArrayList<>(10));
    }
    if(filterConditions != null) {
      for (String condition : filterConditions) {
        targetStepFunction.getQueryFilterConditions().add(condition);
      }
    }
    QueryUtils.refreshQueryHelperMappingExpression(targetStepFunctionGBSubObject);
    
    // QueryParameter
    QueryParameter sourceQueryParameter = QueryUtils.getQueryParameter(sourceStepFunctionGBSubObject);
    QueryParameter targetQueryParameter = QueryUtils.getQueryParameter(targetStepFunctionGBSubObject);
    targetQueryParameter.setMaxObjects(sourceQueryParameter.getMaxObjects());
    targetQueryParameter.setQueryHistory(sourceQueryParameter.getQueryHistory());
    List<? extends SortCriterion> sortCriterions = sourceQueryParameter.getSortCriterion();
    for (SortCriterion sortCriterion : sortCriterions) {
      targetQueryParameter.addToSortCriterion(new SortCriterion(sortCriterion.getCriterion(), sortCriterion.getReverse()));
    }
    QueryUtils.saveQueryParamater(targetStepFunctionGBSubObject, modification.getObject().getDataflow(), targetQueryParameter);
    
    // SelectionMask
    SelectionMask sourceSelectionMask = QueryUtils.getSelectionMask(sourceStepFunctionGBSubObject);
    SelectionMask targetSelectionMask = QueryUtils.getSelectionMask(targetStepFunctionGBSubObject);
    targetSelectionMask.setRootType(sourceSelectionMask.getRootType());
    List<String> columns = sourceSelectionMask.getColumns();
    if(columns != null) {
      for (String column : columns) {
        targetSelectionMask.addToColumns(column);
      }
    }
    
    QueryUtils.saveSelectionMask(targetStepFunctionGBSubObject, modification.getObject().getDataflow(), targetSelectionMask);
    QueryUtils.refreshRootType(targetStepFunctionGBSubObject, (DOM) modification.load(FQNameJson.ofPathAndName(targetSelectionMask.getRootType())).getGenerationBase());
  }
  
  private GBBaseObject createQueryStepFunction(GBSubObject relativeToObject) throws XynaException, UnexpectedJSONContentException {
    Insertion insertion = new Insertion(relativeToObject, copy);
    insertion.setQueryInsertStep(QueryInsertStep.function);
    prepareInsertion(insertion);
    ServiceJson serviceJson = new ServiceJson("Query");
    FQNameJson fqName = FQNameJson.parseAttribute(null, Tags.FQN, PersistenceServices.class.getName());
    fqName = FQNameJson.parseAttribute(fqName, Tags.OPERATION, QUERY_SERVICE_OPERATION);
    serviceJson.setFQName(fqName);    
    Pair<PossibleContent, ? extends XMOMGuiJson> content = Pair.of(PossibleContent.service, serviceJson);
    insertion.setActualContent(PossibleContent.service);
    GBBaseObject service = createNewObject(insertion.getParent(), content, copy);
    executeInsertion(insertion, service);
    modification.getObject().getStepMap().addStep(((StepCatch)service.getStep()).getStepInTryBlock());
    return service;
  }
  
  private GBBaseObject createQueryMapping(GBSubObject relativeToObject) throws XynaException {
    Insertion insertion = new Insertion(relativeToObject, copy);
    insertion.wrapWhenNeeded(modification.getObject());
    insertion.setQueryInsertStep(QueryInsertStep.mapping);
    prepareInsertion(insertion);
    Pair<PossibleContent, ? extends XMOMGuiJson> content = Pair.of(PossibleContent.mapping, MappingJson.hiddenQueryMapping());
    GBBaseObject mapping = createNewObject(insertion.getParent(), content, copy);
    executeInsertion(insertion, mapping);
    modification.getObject().getStepMap().addStep(mapping.getStep());
    return mapping;
  }
  
  private void createQueryMappingOutput(GBBaseObject mapping) throws UnknownObjectIdException, MissingObjectException, XynaException, UnexpectedJSONContentException {
    String mappingOutputId = ObjectId.createStepId(mapping.getStep(), ObjectPart.output).getObjectId();
    GBSubObject mappingOutputGbSubObject = modification.getObject().getObject(mappingOutputId);
    
    VariableJson variableJson = new VariableJson(Tags.VARIABLE, "Filter condition", FQNameJson.parseAttribute(null, Tags.FQN, FilterCondition.class.getName()));
    variableJson.setList(false);
    Pair<PossibleContent, ? extends XMOMGuiJson> content = Pair.of(PossibleContent.variable, variableJson);
    Insertion insertion = new Insertion(mappingOutputGbSubObject, copy);
    insertion.setQueryInsertStep(QueryInsertStep.mappingOutput);
    prepareInsertion(insertion);
    insertion.setActualContent(PossibleContent.variable);
    GBBaseObject variable = createNewObject(modification.getObject().getObject(mappingOutputId), content, copy);
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
  

  public void setCopy(CopyJson copy) {
    this.copy = copy;
  }
}
