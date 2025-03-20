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
package com.gip.xyna.xact.filter.session.gb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.BranchListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.CaseListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.ExpressionListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.FormulaListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.LibListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.MemberMethodListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.MemberVarListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.MetaTagListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.QueryFilterListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.QuerySelectionMaskListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.QuerySortListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.StaticMethodListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.StepListAdapter;
import com.gip.xyna.xact.filter.session.gb.GBSubObjectUtils.VariableListAdapter;
import com.gip.xyna.xact.filter.session.gb.ObjectId.ObjectPart;
import com.gip.xyna.xact.filter.session.gb.references.Reference;
import com.gip.xyna.xact.filter.session.gb.references.ReferenceType;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.StepFunctionJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.VariableJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.Workflow;
import com.gip.xyna.xact.filter.xmom.workflows.json.WorkflowStepVisitor;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ForEachScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.FormulaContainer;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.Catchable;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;

import xnwh.persistence.SortCriterion;

public class GBSubObject extends GBBaseObject {
  
  private final XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();  
  private final GenerationBaseObject root;
  private final ObjectId objectId;

  public GBSubObject(GenerationBaseObject root, ObjectId objectId) {
    super();
    this.root = root;
    this.objectId = objectId;
  }

  public GBSubObject(GenerationBaseObject root, ObjectId objectId, WF workflow) {
    super(workflow);
    this.root = root;
    this.objectId = objectId;
  }
  
  public GBSubObject(GenerationBaseObject root, ObjectId objectId, Step step) {
    super(step);
    this.root = root;
    this.objectId = objectId;
  }
  
  public GBSubObject(GenerationBaseObject root, ObjectId objectId, Step step, Variable variable) {
    super(step,variable);
    this.root = root;
    this.objectId = objectId;
  }
  
  public GBSubObject(GenerationBaseObject root, ObjectId objectId, Step step, FormulaInfo formulaInfo) {
    super(step, formulaInfo);
    this.root = root;
    this.objectId = objectId;
  }

  public GBSubObject(GenerationBaseObject root, ObjectId objectId, Step step, Branch branchInfo) {
    super(step, branchInfo);
    this.root = root;
    this.objectId = objectId;
  }

  public GBSubObject(GenerationBaseObject root, ObjectId objectId, Step step, Case caseInfo) {
    super(step, caseInfo);
    this.root = root;
    this.objectId = objectId;
  }

  public GBSubObject(GenerationBaseObject root, ObjectId objectId, Step step, CaseArea caseAreaInfo) {
    super(step, caseAreaInfo);
    this.root = root;
    this.objectId = objectId;
  }
  
  public GBSubObject(GenerationBaseObject root, ObjectId objectId, Step step, QueryFilterCriterion queryFilterCriterion) {
    super(step, queryFilterCriterion);
    this.root = root;
    this.objectId = objectId;
  }
  
  public GBSubObject(GenerationBaseObject root, ObjectId objectId, Step step, QuerySortCriterion querySortCriterion) {
    super(step, querySortCriterion);
    this.root = root;
    this.objectId = objectId;
  }
  
  public GBSubObject(GenerationBaseObject root, ObjectId objectId, Step step, QuerySelectionMask querySelectionMask) {
    super(step, querySelectionMask);
    this.root = root;
    this.objectId = objectId;
  }

  public GBSubObject(GenerationBaseObject root, ObjectId objectId, DomOrExceptionGenerationBase dtOrException) {
    super(dtOrException);
    this.root = root;
    this.objectId = objectId;
  }
  
  public GBSubObject(GenerationBaseObject root, ObjectId objectId, DomOrExceptionGenerationBase dtOrException, Operation operation) {
    super(dtOrException, operation);
    this.root = root;
    this.objectId = objectId;
  }

  public GBSubObject(GenerationBaseObject root, ObjectId objectId, DomOrExceptionGenerationBase dtOrException, MemberVarInfo memberVarInfo) {
    super(dtOrException, memberVarInfo);
    this.root = root;
    this.objectId = objectId;
  }

  public GBSubObject(GenerationBaseObject root, ObjectId objectId, DomOrExceptionGenerationBase dtOrException, MemberVarInfo memberVarInfo, Variable variable) {
    super(dtOrException, memberVarInfo);
    this.root = root;
    this.objectId = objectId;
    this.variable = variable;
  }

  public GBSubObject(GenerationBaseObject root, ObjectId objectId, DomOrExceptionGenerationBase dtOrException, MemberMethodInfo memberMethodInfo) {
    super(dtOrException, memberMethodInfo);
    this.root = root;
    this.objectId = objectId;
  }
  
  public GBSubObject(GenerationBaseObject root, ObjectId objectId, DomOrExceptionGenerationBase dtOrException, MethodVarInfo methodVarInfo) {
    super(dtOrException, methodVarInfo);
    this.root = root;
    this.objectId = objectId;
  }

  public GBSubObject(GenerationBaseObject root, ObjectId objectId, DOM serviceGroup, LibInfo libInfo) {
    super(serviceGroup, libInfo);
    this.root = root;
    this.objectId = objectId;
  }

  public GBSubObject(GenerationBaseObject root, ObjectId objectId, DomOrExceptionGenerationBase dtOrException, ObjectType objectType) {
    super(dtOrException, objectType);
    this.root = root;
    this.objectId = objectId;
  }
  
  public GBSubObject(GenerationBaseObject root, ObjectId objectId, DomOrExceptionGenerationBase dtOrException, Operation operation, Variable variable) {
    super(dtOrException, operation, variable);
    this.root = root;
    this.objectId = objectId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + objectId.hashCode();
    result = prime * result + root.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    GBSubObject other = (GBSubObject) obj;
    if (!objectId.equals(other.objectId))
      return false;
    if (!root.equals(other.root))
      return false;
    return true;
  }

  public ObjectType getType() {
    return objectId.getType();
  }
  
  public ObjectPart getPart() {
    return objectId.getPart();
  }
  
  public String getObjectId() { //FIXME umbenennen
    return objectId.getObjectId();
  }
  public ObjectId getId() {
    return objectId;
  }
  

  public static GBSubObject of(GenerationBaseObject gbo, String objectId) throws MissingObjectException, UnknownObjectIdException, XynaException {
    ObjectId oi = ObjectId.parse(objectId);
    switch( oi.getType() ) {
    case step:
    case formulaArea:
    case orderInputSource:
    case remoteDestination:
    case remoteDestinationArea:
      return new GBSubObject(gbo, oi, gbo.getStep(oi.getBaseId()) );
    case labelArea:
    case documentationArea:
      switch(gbo.getType()) {
        case WORKFLOW:
          if (oi.getBaseId().length() > 0) {
            return new GBSubObject(gbo, oi, gbo.getStep(oi.getBaseId()) );
          } else {
            return new GBSubObject(gbo, oi, gbo.getWorkflow() );
          }
        case DATATYPE:
          return new GBSubObject(gbo, oi, gbo.getDOM());
        case EXCEPTION:
          return new GBSubObject(gbo, oi, gbo.getExceptionGeneration());
        default:
          throw new IllegalStateException("Unexpected type " + oi.getType());
      }
    case operationDocumentationArea:
      OperationInformation[] operationInformations = ((DOM)gbo.getGenerationBase()).collectOperationsOfDOMHierarchy(true);
      Operation operation = operationInformations[Integer.parseInt(oi.getBaseId())].getOperation();
      return new GBSubObject(gbo, oi, (DomOrExceptionGenerationBase)gbo.getGenerationBase(), operation);
    case memberDocumentationArea:
      return new GBSubObject(gbo, oi, (DomOrExceptionGenerationBase)gbo.getGenerationBase(), new MemberVarInfo(Integer.valueOf(oi.getBaseId())));
    case variable:
      switch(gbo.getType()) {
        case DATATYPE:
          IdentifiedVariables identifiedVariables = gbo.identifyVariables(oi);
          Variable variable = new Variable(identifiedVariables, ObjectId.parseVariableInfo(oi) );
          return new GBSubObject(gbo, oi, gbo.getDOM(), gbo.getDOM().getOperations().get(Integer.valueOf(oi.getBaseId())), variable);
        case WORKFLOW:
          Step step = gbo.getStep(oi.getBaseId());
          identifiedVariables = gbo.identifyVariables(oi);
          variable = new Variable(identifiedVariables, ObjectId.parseVariableInfo(oi) );
          return new GBSubObject(gbo, oi, step, variable);
        default:
          throw new IllegalStateException("Unexpected variable location " + oi.getType());
      }

    case remoteDestinationParameter:
      Step step = gbo.getStep(oi.getBaseId());
      Variable variable = new Variable(null, gbo.getDataflow().getRemoteDestinationParameterVariableIdentification(oi));
      return new GBSubObject(gbo, oi, step, variable);
    case expression:
      step = gbo.getStep(oi.getBaseId());
      FormulaInfo formulaInfo = new FormulaInfo(ObjectId.parseFormulaNumber(oi));
      return new GBSubObject(gbo, oi, step, formulaInfo);
    case distinctionBranch:
      step = gbo.getStep(oi.getBaseId());
      Branch branch = new Branch(ObjectId.parseBranchNumber(oi));
      return new GBSubObject(gbo, oi, step, branch);
    case distinctionCase:
      step = gbo.getStep(oi.getBaseId());
      Case caseInfo = new Case(ObjectId.parseCaseNumber(oi));
      return new GBSubObject(gbo, oi, step, caseInfo);
    case branchArea:
      step = gbo.getStep(oi.getBaseId());
      return new GBSubObject(gbo, oi, step);
    case caseArea:
      step = gbo.getStep(oi.getBaseId());
      CaseArea caseAreaInfo = new CaseArea(ObjectId.parseCaseAreaNumber(oi));
      return new GBSubObject(gbo, oi, step, caseAreaInfo);
    case queryFilterArea:
      return new GBSubObject(gbo, oi, gbo.getStep(oi.getBaseId()));
    case querySortingArea:
      return new GBSubObject(gbo, oi, gbo.getStep(oi.getBaseId()));
    case querySelectionMasksArea:
      return new GBSubObject(gbo, oi, gbo.getStep(oi.getBaseId()));
    case queryFilterCriterion:
      return new GBSubObject(gbo, oi, gbo.getStep(oi.getBaseId()), new QueryFilterCriterion(ObjectId.parseFormulaNumber(oi)));
    case querySortCriterion:
      return new GBSubObject(gbo, oi, gbo.getStep(oi.getBaseId()), new QuerySortCriterion(ObjectId.parseFormulaNumber(oi)));
    case querySelectionMask:
      return new GBSubObject(gbo, oi, gbo.getStep(oi.getBaseId()), new QuerySelectionMask(ObjectId.parseFormulaNumber(oi)));
    case workflow:
      return new GBSubObject(gbo, oi, gbo.getWorkflow() );
    case memberVar:
      return new GBSubObject(gbo, oi, (DomOrExceptionGenerationBase)gbo.getGenerationBase(), new MemberVarInfo(ObjectId.parseMemberVarNumber(oi)));
    case memberMethod:
      return new GBSubObject(gbo, oi, (DomOrExceptionGenerationBase)gbo.getGenerationBase(), new MemberMethodInfo(ObjectId.parseMemberMethodNumber(oi)));
    case metaTag:
    case metaTagArea:
      GBSubObject subObject = new GBSubObject(gbo, oi, (DomOrExceptionGenerationBase)gbo.getGenerationBase(), oi.getType());
      int metaTagIdx = oi.getType() == ObjectType.metaTag ? ObjectId.getMetaTagIdx(oi) : -1;
      String rawMetaTag = null;

      if (oi.getBaseId() != null && oi.getBaseId().length() > 0) {
        ObjectId subOi = ObjectId.parse(oi.getBaseId());
        if (subOi.getType() == ObjectType.memberVar) {
          // meta tag area of a member variable
          subObject.memberVarInfo = new MemberVarInfo(ObjectId.parseMemberVarNumber(subOi));

          if (oi.getType() == ObjectType.metaTag) {
            AVariable var = gbo.getDOM().getMemberVars().get(subObject.memberVarInfo.getIndex());
            rawMetaTag = var.getUnknownMetaTags().get(metaTagIdx).strip();
          }
        } else if (subOi.getType() == ObjectType.memberMethod || subOi.getType() == ObjectType.staticMethod) {
          // meta tag area of a member service
          subObject.memberMethodInfo = new MemberMethodInfo(ObjectId.parseMemberMethodNumber(subOi));
          
          if (oi.getType() == ObjectType.metaTag) {
            Operation op = gbo.getDOM().getOperations().get(subObject.memberMethodInfo.getIndex());
            rawMetaTag = op.getUnknownMetaTags().get(metaTagIdx).strip();
          }
        }
      } else if (oi.getType() == ObjectType.metaTag) {
        // global meta tag area of data type
        rawMetaTag = gbo.getDOM().getUnknownMetaTags().get(metaTagIdx).strip();
      }

      if (rawMetaTag != null) {GBSubObjectUtils.createDTMetaTag(rawMetaTag);
        subObject.metaTag = GBSubObjectUtils.createDTMetaTag(rawMetaTag);
      }

      return subObject;
    case memberVarArea:
    case memberMethodsArea:
    case typeInfoArea:
    case exceptionMessageArea:
    case libs:
    case datatype:
    case exception:
    case servicegroup:
      return new GBSubObject(gbo, oi, (DomOrExceptionGenerationBase)gbo.getGenerationBase(), oi.getType());
    case methodVarArea:
      return new GBSubObject(gbo, oi, (DomOrExceptionGenerationBase)gbo.getGenerationBase(), new MethodVarInfo(Integer.parseInt(oi.getBaseId())));
    case serviceGroupLib:
      return new GBSubObject(gbo, oi, (DOM)gbo.getGenerationBase(), new LibInfo(ObjectId.parseLibNumber(oi)));
    case serviceGroupSharedLib:
      return new GBSubObject(gbo, oi, (DOM)gbo.getGenerationBase(), new LibInfo(ObjectId.parseSharedLibNumber(oi)));
    case clipboardEntry:
      return new GBSubObject(gbo, oi);
    default:
      throw new IllegalStateException("Unexpected type "+oi.getType()); //FIXME
    }
  }
  
  public GBSubObject getParent() {
    if(step != null) {
      ObjectId stepId = ObjectId.createStepId(step);
      switch( getType() ) {
        case step:
          if( step instanceof WFStep ) {
            return new GBSubObject(root, new ObjectId(ObjectType.workflow, null), root.getWorkflow() );
          }
          Step parent = root.getStepMap().getParentStep(stepId.getBaseId());
          if (parent == null) {
            // fallback solution to determine parent (necessary, since not every step gets an xml-id in the flash-gui)
            parent = getStep().getParentStep();
          }
          
          if( parent instanceof WFStep ) {
            return new GBSubObject(root, new ObjectId(ObjectType.workflow, null), root.getWorkflow() );
          } else {
            return new GBSubObject(root, new ObjectId(ObjectType.step, parent.getStepId() ), parent);
          }
        case variable:
          ObjectPart part = ObjectPart.forUsage( variable.getUsage() );
          if( step == null ) {
            ObjectId id = new ObjectId(ObjectType.workflow, null, part );
            return new GBSubObject(root, id, root.getWorkflow() );
          } else {
            ObjectId id = ObjectId.createStepId(step, part);
            return new GBSubObject(root, id, step);
          }
        case workflow:
          return this; //gibt keinen weiteren Parent
        case expression:
          return new GBSubObject(root, new ObjectId(ObjectType.formulaArea, stepId.getBaseId() ), step);
        case distinctionBranch:
          Step stepChoice = root.getStep(stepId.getBaseId());
          ObjectId branchAreaId = new ObjectId(ObjectType.branchArea, stepId.getBaseId());
          return new GBSubObject(root, branchAreaId, stepChoice);
        case distinctionCase:
          String caseAreaIdStr = ObjectId.createCaseAreaId(stepId.getBaseId(), String.valueOf(caseInfo.getIndex()));
          try {
            ObjectId caseAreaId = ObjectId.parse(caseAreaIdStr);
            CaseArea caseAreaInfo = new CaseArea(ObjectId.parseCaseAreaNumber(caseAreaId));
            stepChoice = root.getStep(stepId.getBaseId());
            return new GBSubObject(root, caseAreaId, stepChoice, caseAreaInfo);
          } catch (UnknownObjectIdException e) {
            return null; // TODO: logging
          }
        case querySortCriterion:
          return new GBSubObject(root, new ObjectId(ObjectType.querySortingArea, stepId.getBaseId() ), step);
        case queryFilterCriterion:
          return new GBSubObject(root, new ObjectId(ObjectType.queryFilterArea, stepId.getBaseId() ), step);
        default:
          return null;
      }
    } else {
      switch( getType() ) {
        case memberMethod:
        case staticMethod:
        case memberVar:
        case typeInfoArea:
          return new GBSubObject(root, new ObjectId(ObjectType.datatype, null), getDtOrException());
        case metaTag:
          GBSubObject subObject = new GBSubObject(root, new ObjectId(ObjectType.datatype, null), getDtOrException());

          if (objectId.getBaseId() != null && objectId.getBaseId().length() > 0) {
            ObjectId subOi;
            try {
              subOi = ObjectId.parse(objectId.getBaseId());
            } catch (UnknownObjectIdException e) {
              return null;
            }

            if (subOi.getType() == ObjectType.memberVar) {
              // meta tag area of a member variable
              subObject.memberVarInfo = new MemberVarInfo(ObjectId.parseMemberVarNumber(subOi));
            } else {
              // meta tag area of a member service
              subObject.memberMethodInfo = new MemberMethodInfo(ObjectId.parseMemberMethodNumber(subOi));
            }
          }

          return subObject;
        case methodVarArea:
          return new GBSubObject(root, new ObjectId(ObjectType.operation, String.valueOf(Utils.getOperationIndex(getOperation()))), getDtOrException(), getOperation());
        case variable:
          ObjectId methodVarAreaId = new ObjectId(ObjectType.methodVarArea, getId().getBaseId(), ObjectPart.forUsage(ObjectId.parseVariableInfo(objectId).getFirst()));
          return new GBSubObject(root, methodVarAreaId, root.getDOM(), new MethodVarInfo(Integer.parseInt(objectId.getBaseId())));
        default:
          return null;
      }
    }
  }

  public GBSubObject getSibling(int index) {
    switch( getType() ) {
      case step:
        Step parentStep = step.getParentStep();
        if ( (parentStep == null) || (index < 0) || (index >= parentStep.getChildSteps().size()) ) {
          return null;
        }

        Step siblingStep = parentStep.getChildSteps().get(index);
        if (siblingStep.getXmlId() == null) {
          return new GBSubObject(root, new ObjectId(ObjectType.workflow, null), root.getWorkflow());
        } else {
          return new GBSubObject(root, new ObjectId(ObjectType.step, siblingStep.getStepId()), siblingStep);
        }
      default:
        return null;
    }
  }

  public List<Reference> getReferences() throws XynaException {
    switch( getType() ) {
      case workflow:
        return getWorkflowReferences();
      case memberMethod:
      case staticMethod:
      case operation:
        return getOperationReferences();
      case datatype:
      case typeInfoArea:
      case servicegroup:
      case exception:
        return getDatatypeReferences();
        
      default:
        throw new IllegalStateException("Unsupported type " + getType());
    }
  }

  private List<Reference> getDatatypeReferences() throws XynaException {
    List<Reference> result = new ArrayList<>();

    HashMap<String, String> filters = new HashMap<>();
    filters.put("fqname", getDtOrException().getOriginalFqName());

    result.addAll(loadReferences(ReferenceType.usedInImplOf, filters, getDtOrException().getRevision(), XMOMDatabaseType.DATATYPE));
    result.addAll(loadReferences(ReferenceType.producedBy, filters, getDtOrException().getRevision(), XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION));
    result.addAll(loadReferences(ReferenceType.neededBy, filters, getDtOrException().getRevision(), XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION));
    result.addAll(loadReferences(ReferenceType.possessedBy, filters, getDtOrException().getRevision(), XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION));
    result.addAll(loadReferences(ReferenceType.possesses, filters, getDtOrException().getRevision(), XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION));
    result.addAll(loadReferences(ReferenceType.thrownBy, filters, getDtOrException().getRevision(), XMOMDatabaseType.EXCEPTION));
    result.addAll(loadReferences(ReferenceType.extendedBy, filters, getDtOrException().getRevision(), XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION));
    result.addAll(loadReferences(ReferenceType.extend, filters, getDtOrException().getRevision(), XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION));

    return result;
  }

  private List<Reference> getOperationReferences() throws XynaException {
    List<Reference> result = new ArrayList<>();

    HashMap<String, String> filters = new HashMap<>();
    StringBuilder fqn = new StringBuilder();
    fqn.append(operation.getParent().getFqClassName());
    String serviceName = operation.getParent().getServiceName(operation);
    if(serviceName != null) {
      fqn.append(".").append(serviceName);
    }
    fqn.append(".").append(operation.getName());
    filters.put("fqname", fqn.toString());

    result.addAll(loadReferences(ReferenceType.calledBy, filters, operation.getParent().getRevision(), XMOMDatabaseType.SERVICE));
    result.addAll(loadReferences(ReferenceType.calls, filters, operation.getParent().getRevision(), XMOMDatabaseType.SERVICE));
    result.addAll(loadReferences(ReferenceType.exceptions, filters, operation.getParent().getRevision(), XMOMDatabaseType.SERVICE));
    result.addAll(loadReferences(ReferenceType.needs, filters, operation.getParent().getRevision(), XMOMDatabaseType.SERVICE));
    result.addAll(loadReferences(ReferenceType.produces, filters, operation.getParent().getRevision(), XMOMDatabaseType.SERVICE));

    return result;
  }

  private List<Reference> getWorkflowReferences() throws XynaException {
    List<Reference> result = new ArrayList<>();
    
    HashMap<String, String> filters = new HashMap<>();
    filters.put("fqname", getWorkflow().getFqClassName());
    
    result.addAll(loadReferences(ReferenceType.instanceServiceReferenceOf, filters, getWorkflow().getRevision(), XMOMDatabaseType.DATATYPE));
    result.addAll(loadReferences(ReferenceType.calledBy, filters, getWorkflow().getRevision(), XMOMDatabaseType.SERVICE));
    
    return result;
  }
  
  private List<Reference> loadReferences(ReferenceType referenceType, HashMap<String, String> filters, Long revision, XMOMDatabaseType...xmomDatabaseTypes) throws XynaException{
    final List<Reference> references = new ArrayList<>();
    
    if(!root.getSaveState()) {
      return references;
    }
    
    SearchRequestBean srb = Utils.createReferencesSearchRequestBean(referenceType.getSelection(), filters);
    XMOMDatabaseSelect select = (XMOMDatabaseSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
    select.addAllDesiredResultTypes(Arrays.asList(xmomDatabaseTypes));
    
    XMOMDatabaseSearchResult searchResult = multiChannelPortal.searchXMOMDatabase(Arrays.asList(select), -1, revision);
    List<XMOMDatabaseSearchResultEntry> results = searchResult.getResult();
    results.forEach(r -> {
      try {
        Reference ref = new Reference(new FQName(r.getRuntimeContext(), r.getFqName()), r.getLabel(), referenceType, Type.of(r.getType()));
        references.add(ref);
      } catch (XFMG_NoSuchRevision e) {
        
      }
    });
    return references;
  }
  
  public GenerationBaseObject getRoot() {
    return root;
  }

  public XMOMGuiJson getJsonSerializable() {
    switch( getType() ) {
    case step:
      if( getStep() instanceof StepFunction ) {
        return new StepFunctionJson(root.getView(), (StepFunction)getStep(), getPart() );
      } else {
        return new WorkflowStepVisitor(root.getView(), getStep() );
      }
    case workflow:
      return new Workflow(root, getPart());
    case variable:
      return new VariableJson(this);
    default:
      return null;
    }
  }

  public IdentifiedVariables getIdentifiedVariables() {
    return root.identifyVariables(objectId);
  }
  
  public void refreshIdentifiedVariables() {
    root.getVariableMap().refreshIdentifiedVariables(objectId);
  }
  
  public List<Step> getStepListAdapter() {
    if( getType() != ObjectType.step ) {
      throw new IllegalStateException("getStepListAdapter called for "+getType()+", not step");
    }
    return new StepListAdapter((StepSerial)step, getRoot());
  }

  public List<Formula> getFormulaListAdapter() {
    if( getType() != ObjectType.formulaArea) {
      throw new IllegalStateException("getFormulaListAdapter called for "+getType()+", not formulaArea");
    }
    return new FormulaListAdapter((FormulaContainer)step, getIdentifiedVariables(), getRoot());
  }

  public List<String> getExpressionListAdapter() {
    if( getType() != ObjectType.formulaArea ) {
      throw new IllegalStateException("getExpressionListAdapter called for "+getType()+", not formulaArea");
    }
    return new ExpressionListAdapter((StepMapping)step);
  }
  
  public List<Branch> getBranchListAdapter() {
    if( getType() != ObjectType.branchArea ) {
      throw new IllegalStateException("getBranchListAdapter called for "+getType()+", not branchArea");
    }

    Distinction distinction;
    if (step instanceof StepChoice) {
      distinction = (Distinction)step;
    } else if (step instanceof StepParallel) {
      // TODO: support deleting branches in parallelism
      throw new IllegalStateException("Deleting branches from parallelism is currently not supported.");
    } else {
      distinction = (Distinction)( ((Catchable)step).getProxyForCatch() );
    }

    return new BranchListAdapter(distinction, getRoot());
  }

  public List<Case> getCaseListAdapter() {
    if( getType() != ObjectType.caseArea ) {
      throw new IllegalStateException("getCaseListAdapter called for "+getType()+", not caseArea");
    }
    return new CaseListAdapter((StepChoice)step, getCaseAreaInfo().getCaseAreaNo());
  }

  public List<Variable> getVariableListAdapter() {
    if( (getType() != ObjectType.step) &&
        (getType() != ObjectType.workflow) &&
        (getType() != ObjectType.expression) &&
        (getType() != ObjectType.queryFilterCriterion) &&
        (getType() != ObjectType.distinctionCase) &&
        getType() != ObjectType.methodVarArea) {
      throw new IllegalStateException("getVariableListAdapter called for "+getType()+", not step, workflow, formula or distinctionCase");
    }

    VarUsageType usage = null;
    switch( getPart() ) {
    case input:
      usage = VarUsageType.input;
      break;
    case output:
      usage = VarUsageType.output;
      break;
    case thrown:
      usage = VarUsageType.thrown;
      break;
    default:
      throw new IllegalStateException("Input or Output?");
    }
    IdentifiedVariables identifiedVariables;
    if(getType() == ObjectType.queryFilterCriterion) {
      StepMapping queryHelperMapping = QueryUtils.findQueryHelperMapping(this);
      if(queryHelperMapping == null) {
        identifiedVariables = getIdentifiedVariables();
      } else {
        identifiedVariables = root.identifyVariables(ObjectId.createStepId(queryHelperMapping));
      }
      usage = VarUsageType.input;
    } else {
      identifiedVariables = getIdentifiedVariables();
    }
    return new VariableListAdapter(identifiedVariables, usage);
  }
  
  public List<String> getQueryFilterListAdapter(){
    if( getType() != ObjectType.queryFilterArea ) {
      throw new IllegalStateException("getQueryListAdapter called for "+getType()+", not queryFilterArea");
    }
    return new QueryFilterListAdapter((StepFunction)step, this);
  }
  
  public List<SortCriterion> getQuerySortingListAdapter(){
    if( getType() != ObjectType.querySortingArea ) {
      throw new IllegalStateException("getQuerySortingListAdapter called for "+getType()+", not querySortingArea");
    }
    return new QuerySortListAdapter(getIdentifiedVariables(), getRoot());
  }
  
  public List<String> getQuerySelectionMaskListAdapter() throws UnknownObjectIdException, MissingObjectException, XynaException{
    if( getType() != ObjectType.querySelectionMasksArea ) {
      throw new IllegalStateException("getQuerySelectionMaskListAdapter called for "+getType()+", not querySelectionMasksArea");
    }
    return new QuerySelectionMaskListAdapter(getRoot().getObject(ObjectId.createStepId(getStep()).getObjectId()), getRoot().getDataflow());
  }

  public List<MemberVar> getMemberVarListAdapter() {
    return new MemberVarListAdapter(dtOrException);
  }

  public List<MemberMethod> getMemberMethodListAdapter() {
    return new MemberMethodListAdapter((DOM)dtOrException);
  }
  
  public List<StaticMethod> getStaticMethodListAdapter() {
    return new StaticMethodListAdapter((DOM)dtOrException);
  }

  public List<DTMetaTag> getMetaTagListAdapter() {
    return new MetaTagListAdapter((DOM)dtOrException, memberVarInfo, memberMethodInfo);
  }

  public List<Lib> getLibListAdapter() {
    return new LibListAdapter(root);
  }
 
  public boolean isStepInTryBlock() {
    return (getType() == ObjectType.step && getStep() != null && getStep().getParentStep() instanceof StepCatch);
  }

  public boolean isForeach() {
    return (getType() == ObjectType.step && getStep() instanceof StepForeach);
  }

  public boolean isStepInForeach() {
    return (getType() == ObjectType.step && getStep() != null && getStep().getParentStep() instanceof StepSerial
         && getStep().getParentStep().getParentStep() instanceof ForEachScopeStep
         && getStep().getParentStep().getParentStep().getParentStep() instanceof StepForeach);
  }

}
