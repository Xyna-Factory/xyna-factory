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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.Modification;
import com.gip.xyna.xact.filter.session.exceptions.MergeConflictException;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.ModificationNotAllowedException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.exceptions.UnsupportedOperationException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Branch;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Case;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Formula;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.FormulaInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Lib;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberMethod;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberMethodInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberVar;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberVarInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.QueryFilterCriterion;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.QuerySelectionMask;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.QuerySortCriterion;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Variable;
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.modify.Insertion;
import com.gip.xyna.xact.filter.session.modify.Insertion.PossibleContent;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.DirectVarIdentification;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.datatypes.json.DatatypeMemberXo;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xact.filter.xmom.workflows.json.BranchJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.CaseJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.DistinctionJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ForeachJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.FormulaJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.LibJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.MappingJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.MemberMethodJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.MemberServiceJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.MemberVarJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ParallelismJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.PositionJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.RetryJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ServiceJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.TemplateJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.ThrowJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.UpdateResponseJson;
import com.gip.xyna.xact.filter.xmom.workflows.json.VariableJson;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.HasDocumentation;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.ServiceVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
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

public abstract class ModifyOperationBase<T extends XMOMGuiJson> {

  protected static final String QUERY_SERVICE_OPERATION = "query";
  
  private static final String THROW_LABEL_PREFIX = "Throw";
  private static final Logger logger = CentralFactoryLogging.getLogger(ModifyOperationBase.class);
  
  protected GBSubObject object;
  protected String focusCandidateId;
  protected Modification modification;


  public abstract int parseRequest(String jsonRequest) throws InvalidJSONException, UnexpectedJSONContentException;


  protected void modifyStep(Step step) throws Exception {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyWorkflow(WF workflow) throws XynaException, UnknownObjectIdException, MissingObjectException,
      InvalidJSONException, UnexpectedJSONContentException, ModificationNotAllowedException {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyVariable(Variable variable) throws Exception {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyFormulaArea(Step step)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyQueryFilterArea(Step step)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }
  
  protected void modifyQuerySelectionMaskArea()
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyQuerySortingArea(Step step)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyQueryFilter(QueryFilterCriterion queryFilterCriterion)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyQuerySorting(QuerySortCriterion querySortCriterion)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }
  
  protected void modifyQuerySelectionMask(QuerySelectionMask querySelectionMask)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }
  

  protected void modifyFormula(FormulaInfo formulaInfo)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyBranch(Branch branchInfo)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException, UnsupportedOperationException {
    throw new java.lang.UnsupportedOperationException();
  }
  

  protected void modifyCase(Case caseInfo) throws XynaException, UnknownObjectIdException, MissingObjectException,
      InvalidJSONException, UnexpectedJSONContentException, UnsupportedOperationException, MergeConflictException {
    throw new java.lang.UnsupportedOperationException();
  }
  

  protected void modifyBranchArea(Step step)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }
  

  protected void modifyCaseArea(Step step)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyDocumentation(HasDocumentation object)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }
  

  protected void modifyMemberVar(DomOrExceptionGenerationBase dtOrException, MemberVarInfo memberVarInfo)
      throws Exception {
    throw new java.lang.UnsupportedOperationException();
  }
  

  protected void modifyMemberVarArea(DomOrExceptionGenerationBase dtOrException)
      throws UnsupportedOperationException, XynaException, UnknownObjectIdException, MissingObjectException,
      InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }
  

  protected void modifyMemberMethod(DOM dom, MemberMethodInfo memberMethodInfo)
      throws UnsupportedOperationException, XynaException, ModificationNotAllowedException {
    throw new java.lang.UnsupportedOperationException();
  }  

  protected void modifyMemberMethodArea(DOM dom) throws UnsupportedOperationException,
      UnknownObjectIdException, MissingObjectException, XynaException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }
  

  protected void modifyTypeInfoArea(DomOrExceptionGenerationBase dtOrException)
      throws XynaException, ModificationNotAllowedException {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyExceptionMessageArea(DomOrExceptionGenerationBase dtOrException) {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyMethodVarArea(DomOrExceptionGenerationBase dtOrException)
      throws UnsupportedOperationException, UnknownObjectIdException, MissingObjectException, XynaException, InvalidJSONException,
      UnexpectedJSONContentException, ModificationNotAllowedException {
    throw new java.lang.UnsupportedOperationException();
  }
  

  protected void modifyLibs(DomOrExceptionGenerationBase dtOrException)
      throws UnknownObjectIdException, MissingObjectException, XynaException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyServiceGroupLib(DOM serviceGroup) {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyServiceGroupSharedLib(DOM serviceGroup) {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyOrderInputSource(Step step)
      throws XynaException, UnknownObjectIdException, MissingObjectException, InvalidJSONException, UnexpectedJSONContentException {
    throw new java.lang.UnsupportedOperationException();
  }


  protected void modifyRemoteDestination(StepFunction step) {
    throw new java.lang.UnsupportedOperationException();
  }

  
  public void modify(Modification modification, GBSubObject object) throws Exception {
    this.modification = modification;
    this.object = object;
    this.focusCandidateId = object.getObjectId();
    switch( object.getType() ) {
    case step:
      modifyStep(object.getStep());
      break;
    case labelArea:
      if (object.getStep() != null) {
        modifyStep(object.getStep());
      } else {
        modifyWorkflow(object.getWorkflow());
      }
      break;
    case documentationArea:
      if (object.getStep() != null) {
        modifyDocumentation((HasDocumentation)object.getStep());
      } else if(object.getWorkflow() != null) {
        modifyDocumentation((HasDocumentation)object.getWorkflow());
      } else if (object.getDtOrException() != null) {
        modifyDocumentation((HasDocumentation)object.getDtOrException());
      }
      break;
    case memberDocumentationArea:
      List<DatatypeMemberXo> members = com.gip.xyna.xact.filter.xmom.datatypes.json.Utils.createDtMembers(
                                               object.getDtOrException().getAllMemberVarsIncludingInherited(), 
                                               object.getRoot(), 
                                               com.gip.xyna.xact.filter.xmom.datatypes.json.Utils.getParents(object.getRoot().getParent()));
                                               
      DatatypeMemberXo member = members.get(object.getMemberVarInfo().getIndex());
      modifyDocumentation(member.getVariable());
      break;
    case operationDocumentationArea:
      if (object.getOperation() != null) {
        modifyDocumentation((HasDocumentation)object.getOperation());
      }
      break;
    case workflow:
      modifyWorkflow(object.getWorkflow());
      break;
    case variable:
    case remoteDestinationParameter:
      modifyVariable(object.getVariable());
      break;
    case formulaArea:
      modifyFormulaArea(object.getStep());
      break;
    case expression:
      modifyFormula(object.getFormulaInfo());
      break;
    case distinctionBranch:
      modifyBranch(object.getBranchInfo());
      break;
    case distinctionCase:
      modifyCase(object.getCaseInfo());
      break;
    case branchArea:
      modifyBranchArea(object.getStep());
      break;
    case caseArea:
      modifyCaseArea(object.getStep());
      break;
    case queryFilterArea:
      modifyQueryFilterArea(object.getStep());
      break;
    case querySortingArea:
      modifyQuerySortingArea(object.getStep());
      break;
    case queryFilterCriterion:
      modifyQueryFilter(object.getQueryFilterCriterion());
      break;
    case querySortCriterion:
      modifyQuerySorting(object.getQuerySortCriterion());
      break;
    case querySelectionMasksArea:
      modifyQuerySelectionMaskArea();
      break;
    case querySelectionMask:
      modifyQuerySelectionMask(object.getQuerySelectionMask());
      break;
    case memberVar: // variable in data type/exception type
      modifyMemberVar(object.getDtOrException(), object.getMemberVarInfo());
      break;
    case memberVarArea: // member variables of data type/exception type
      modifyMemberVarArea(object.getDtOrException());
      break;
    case memberMethod: // method in data type
      modifyMemberMethod((DOM)object.getDtOrException(), object.getMemberMethodInfo());
      break;
    case memberMethodsArea: // method in data type
      modifyMemberMethodArea((DOM)object.getDtOrException());
      break;
    case typeInfoArea:
      modifyTypeInfoArea(object.getDtOrException());
      break;
    case exceptionMessageArea:
      modifyExceptionMessageArea(object.getDtOrException());
      break;
    case methodVarArea:
      modifyMethodVarArea(object.getDtOrException());
      break;
    case libs:
      modifyLibs(object.getDtOrException());
      break;
    case serviceGroupLib:
      modifyServiceGroupLib((DOM)object.getDtOrException());
      break;
    case serviceGroupSharedLib:
      modifyServiceGroupSharedLib((DOM)object.getDtOrException());
      break;
    case orderInputSource:
      modifyOrderInputSource(object.getStep());
      break;
    case remoteDestination:
    case remoteDestinationArea:
      modifyRemoteDestination((StepFunction)object.getStep());
      break;
    default:
      break;
    }
  }

  protected void setFocusCandidateId(String focusCandidateId) {
    this.focusCandidateId = focusCandidateId;
  }
  
  public String getFocusCandidateId() {
    return focusCandidateId;
  }
  
  public String getObjectId() {
    return object.getObjectId();
  }

  public void removeWrapperWhenObsolete(StepSerial lane) {
    Utils.removeWrapperWhenObsolete(modification.getObject(), lane);
  }



  protected GBBaseObject createNewObject(GBSubObject parent, Pair<PossibleContent, ? extends XMOMGuiJson> content, PositionJson positionJson) throws XynaException {
    switch( content.getFirst() ) {
    case service:
      return createStepFunction(parent, (ServiceJson)content.getSecond() );
    case variable:
      return createParameter(parent, (VariableJson)content.getSecond() );
    case memberVar:
      return createMemberVar(parent, (MemberVarJson)content.getSecond() );
    case memberMethod:
      return createMemberMethod(parent, (MemberMethodJson)content.getSecond() );
    case choice:
      return createStepChoice(parent, (DistinctionJson)content.getSecond() );
    case mapping:
      return createMapping(parent, (MappingJson)content.getSecond() );
    case formula:
      return createFormula(parent, (FormulaJson)content.getSecond() );
    case expression:
      return createExpression(parent, (MappingJson)content.getSecond() );
    case distinctionBranch:
      return createBranch(parent, (BranchJson)content.getSecond(), positionJson );
    case distinctionCase:
      return createCase(parent, (CaseJson)content.getSecond() );
    case throwStep:
      return createThrowStep(parent, (ThrowJson)content.getSecond() );
    case retryStep:
      return createRetryStep(parent, (RetryJson)content.getSecond() );
    case templateStep:
      return createTemplateStep(parent, (TemplateJson)content.getSecond() );
    case queryFilterCriterion:
      return createQueryFilterCriterion((FormulaJson)content.getSecond() );
    case querySortCriterion:
      return createQuerySortCriterion((FormulaJson)content.getSecond() );
    case querySelectionMask:
      return createQuerySelectionMask((FormulaJson)content.getSecond() );
    case staticMethod:
      return createStaticMethod(parent, (MemberServiceJson)content.getSecond() );
    case lib:
      return createLib(parent, (LibJson)content.getSecond() );
    case parallelism:
      return createParalellism(parent, (ParallelismJson)content.getSecond());
    case foreach:
      return createForeach(parent, (ForeachJson)content.getSecond());
    default:
      return null; // FIXME
    }
  }
  
  private GBBaseObject createParalellism(GBSubObject parent, ParallelismJson content) {
    StepParallel stepParallel = new StepParallel(getParentScope(parent), parent.getRoot().getWorkflow());
    stepParallel.createEmpty();
    return new GBBaseObject(stepParallel);
  }
  
  private GBBaseObject createForeach(GBSubObject parent, ForeachJson content) {
    StepForeach stepForeach = new StepForeach(getParentScope(parent), object.getRoot().getWorkflow());
    stepForeach.createEmpty();
    return new GBBaseObject(stepForeach);
  }

  protected boolean isQuery(GBSubObject functionGBSubobject) {
    if(functionGBSubobject == null) {
      return false;
    }
    return QueryUtils.findQueryHelperMapping(functionGBSubobject) != null;
  }

  
  public static StepFunction createStepFunction(ScopeStep parentScope, String operationName, GenerationBase implGb) {
    GenerationBase creator = (parentScope instanceof WFStep) ? ((WFStep) parentScope).getWF() : parentScope.getParentWFObject();
    StepFunction sf = new StepFunction(parentScope, creator);
    
    if(implGb == null) {
      sf.createEmpty();
    } else if(implGb instanceof WF) {
      WF wf = (WF)implGb;
      String[] invokeVarIds = new String[wf.getInputVars().size()];
      String[] receiveVarIds = new String[wf.getOutputVars().size()];
      nullPrototypes(receiveVarIds, wf.getOutputVars());
      
      sf.createService(wf, invokeVarIds, receiveVarIds); 
    } else if(implGb instanceof DOM) {
      DOM service = (DOM)implGb;
      createServiceInStepFunction(service, sf, operationName);
    }
    
    return sf;
  }

  
  private static void createServiceInStepFunction(DOM service, StepFunction sf, String operationName) {
    try {
      Operation operation = service.getOperationByName(operationName);

      int invokeVarIdsCount;
      if (operation.isStatic()) {
        invokeVarIdsCount = operation.getInputVars().size();
      } else {
        // instance methods have an additional input parameter that takes the instance of the data type
        invokeVarIdsCount = operation.getInputVars().size() + 1;
      }

      String[] invokeVarIds = new String[invokeVarIdsCount];
      String[] receiveVarIds = new String[operation.getOutputVars().size()];

      sf.createService(service, operationName, invokeVarIds, receiveVarIds);
    } catch (XPRC_OperationUnknownException ex) {
      Utils.logError(service.getOriginalFqName() + " isn't accessible.", ex);
      sf.createEmpty();
    }
  }
  
  
  private GBBaseObject createStepFunction( GBSubObject parent, ServiceJson content) throws XynaException {
    StepFunction sf = new StepFunction(getParentScope(parent), object.getRoot().getWorkflow() );
    Step newStep = sf;
    if( content.isPrototype() ) {
      sf.createEmpty();
    } else {
      try {
        GenerationBaseObject insertGbo = modification.load(content.getFQName());
        if( insertGbo.getType() == XMOMType.WORKFLOW ) {
          WF wf = insertGbo.getWorkflow();
          String[] invokeVarIds = new String[wf.getInputVars().size()];
          String[] receiveVarIds = createVarIdArray(wf.getOutputVars().size());
          nullPrototypes(receiveVarIds, wf.getOutputVars());
          
          sf.createService(wf, invokeVarIds, receiveVarIds); 
        } else if( insertGbo.getType() == XMOMType.DATATYPE ) {
          //Service
          DOM service = insertGbo.getDOM();
          Operation operation = service.getOperationByName(content.getFQName().getOperation());
          
          int invokeVarIdsCount; 
          if (operation.isStatic()) {
            invokeVarIdsCount = operation.getInputVars().size();
          } else {
            // instance methods have an additional input parameter that takes the instance of the data type
            invokeVarIdsCount = operation.getInputVars().size() + 1;
          }
          
          String[] invokeVarIds = new String[invokeVarIdsCount];
          String[] receiveVarIds = createVarIdArray(operation.getOutputVars().size());
          
          sf.createService(service, content.getFQName().getOperation(), invokeVarIds, receiveVarIds);

          // in case the inputs/ouputs of the function have default restrictions, they are used as a default values for dynamic typing
          try {
            setDefaultCasting(sf, operation);
          } catch (Exception e) {
            Utils.logError("Could not determine/set default restrictions for input/outpus of function call.", e);
          }
        } else {
          //TODO Fehler
        }
      } catch (Ex_FileAccessException ex) {
        Utils.logError(content.getFQName() + " isn't accessible.", ex);
        sf.createEmpty();
      }

      // wrap step in try-block right from the start to make adding exception handling later easier
      StepCatch stepCatch = new StepCatch(sf.getParentScope(), sf, sf.getCreator());
      stepCatch.createEmpty();
      sf.setCatchStep(stepCatch);
      newStep = stepCatch;
    }
    
    if (content.getLabel() != null) {
      sf.setLabel(content.getLabel());
    }
    
    return new GBBaseObject(newStep);
  }


  private static void nullPrototypes(String[] ids, List<AVariable> vars) {
    for (int i = 0; i < ids.length; i++) {
      if (vars.get(i) == null || vars.get(i).isPrototype()) {
        ids[i] = null;
      }
    }
  }


  /**
   * In case the inputs/ouputs of a function have default restrictions, they are used as a default values for dynamic typing.
   */
  private void setDefaultCasting(StepFunction sf, Operation op) {
    for (int varIdx = 0; varIdx < op.getInputVars().size(); varIdx++) {
      AVariable opInputVar = op.getInputVars().get(varIdx);
      sf.getInputVarCastToType()[varIdx] = opInputVar.getDefaultTypeRestriction() != null ? opInputVar.getDefaultTypeRestriction().getOriginalFqName() : null;
    }

    for (int varIdx = 0; varIdx < op.getOutputVars().size(); varIdx++) {
      AVariable opOutputVar = op.getOutputVars().get(varIdx);
      sf.getReceiveVarCastToType()[varIdx] = opOutputVar.getDefaultTypeRestriction() != null ? opOutputVar.getDefaultTypeRestriction().getOriginalFqName() : null;
    }
  }

  private String[] createVarIdArray(int size) {
    String[] varIds = new String[size];
    for (int varNr = 0; varNr < size; varNr++) {
      varIds[varNr] = object.getRoot().getWorkflow().getNextXmlId().toString();
    }
    
    return varIds;
  }
  
  private GBBaseObject createStepChoice( GBSubObject parent, DistinctionJson content) throws XynaException {
    StepChoice sc = new StepChoice(getParentScope(parent), object.getRoot().getWorkflow() );
    String condition = content.getCondition() != null ? content.getCondition() : "";
    sc.create(condition, content.getDistinctionType());

    return new GBBaseObject(sc);
  }
  
  protected GBBaseObject createParameter(final GBSubObject parent, VariableJson content) throws XynaException {
    AVariable var;
    GenerationBase creator = parent.getRoot().getGenerationBase();

    if (Tags.EXCEPTION.equals(content.getType())) {
      var = new ExceptionVariable(creator, content.getFQName().toString());
    } else {
      if ( parent != null && (parent.getStep() instanceof StepMapping || parent.getStep() instanceof StepFunction) ) {
        var = new ServiceVariable(creator);
      } else {
        var = new DatatypeVariable(creator);
      }
    }
    var.setIsList(content.isList());

    if( content.isPrototype() ) {
      var.createPrototype(content.getLabel());
    } else {
      try {
        GenerationBaseObject insertGbo = modification.load(content.getFQName());
        if ((insertGbo.getType() == XMOMType.DATATYPE) || (insertGbo.getType() == XMOMType.EXCEPTION)) {
          if ((DomOrExceptionGenerationBase) insertGbo.getGenerationBase() != null) {
            var.createDomOrException(content.getLabel(), (DomOrExceptionGenerationBase) insertGbo.getGenerationBase());
          } else {
            var = AVariable.createAnyType(creator, var.isList());
          }
        } else {
          throw new IllegalStateException(content.getFQName() + " is not datatype or exception " + insertGbo.getType() );
        }
      } catch (Ex_FileAccessException ex) {
        Utils.logError(content.getFQName() + " isn't accessible.", ex);
        var.createPrototype(content.getLabel());
      }
    }
    
    // parent.getStep() is null for workflow input/output
    if (parent != null && parent.getStep() != null) {
      var.setSourceIds(parent.getStep().getStepId());
    }

    Variable variable = new Variable(parent != null ? parent.getIdentifiedVariables() : null, DirectVarIdentification.of(var));
    //IdentifiedVariables identifiedVariables = (parent != null || parent.getType() == ObjectType.memberVarArea) ? null : parent.getIdentifiedVariables();
    //Variable variable = new Variable(identifiedVariables, DirectVarIdentification.of(var));

    return new GBBaseObject(parent != null ? parent.getStep() : null, variable);
  }

  protected GBBaseObject createMemberVar(final GBSubObject parent, MemberVarJson content) throws XynaException {
    DatatypeVariable var = new DatatypeVariable(parent.getRoot().getGenerationBase());
    if (content.getFqn() != null && isFqnReachable(content.getFqn(), parent.getRoot().getGenerationBase().getRevision())) {
      var.create(content.getFqn());
    } else if (content.getPrimitiveType() != null) {
      var.create(content.getPrimitiveType());
    } else {
      var.create(PrimitiveType.STRING);
    }
    var.setLabel(content.getLabel());
    var.setIsList(content.isList());
    var.setDocumentation(content.getDocumentation());

    List<String> usedNames = new ArrayList<>();
    for (AVariable memberVar : parent.getDtOrException().getAllMemberVarsIncludingInherited()) {
      usedNames.add(memberVar.getVarName());
    }
    var.setVarName(com.gip.xyna.xprc.xfractwfe.generation.xml.Utils.createUniqueJavaName(usedNames, content.getLabel(), false));

    return new GBBaseObject(object.getDtOrException(), new MemberVar(var));
  }
  
  private boolean isFqnReachable(String fqn, Long revision) {
    
    try {
      String rootTag = DomOrExceptionGenerationBase.retrieveRootTag(fqn, revision, false, true);
      XMOMType type = XMOMType.getXMOMTypeByRootTag(rootTag);
      switch(type) {
        case DATATYPE:
          DOM dom = DOM.getInstance(fqn, revision);
          return dom != null && dom.exists();
        case EXCEPTION:
          ExceptionGeneration eg = ExceptionGeneration.getInstance(fqn, revision);
          return eg != null && eg.exists();
        default:
          return false;
      }
    } catch (Ex_FileAccessException | XPRC_XmlParsingException | XPRC_InvalidPackageNameException e) {
      Utils.logError(e.getMessage(), e);
    }
    
    return false;
  }

  private AVariable createMethodParameter(final GBSubObject parent, VariableJson content, List<AVariable> parameters) throws XPRC_InvalidPackageNameException {
    GenerationBase creator = parent.getRoot().getGenerationBase();
    String fqn = content.getFQName() == null ? null : content.getFQName().toString();

    AVariable variable = null;
    if (Tags.EXCEPTION.equals(content.getType())) {
      ExceptionVariable exception = new ExceptionVariable(creator);
      exception.init(content.getFQName().getTypePath(), content.getFQName().getTypeName());
      variable = exception;
    } else {
      DatatypeVariable datatypeVar = new DatatypeVariable(creator);
      datatypeVar.create(fqn);
      variable = datatypeVar;
    }

    List<String> usedNames = new ArrayList<>();
    for (AVariable memberVar : parameters) {
      usedNames.add(memberVar.getVarName());
    }
    variable.setVarName(com.gip.xyna.xprc.xfractwfe.generation.xml.Utils.createUniqueJavaName(usedNames, content.getLabel(), false));

    variable.setLabel(content.getLabel());
    variable.setIsList(content.isList());
    return variable;
  }

  private List<AVariable> createMethodParameters(final GBSubObject parent, List<VariableJson> content) throws XynaException {
    List<AVariable> parameters = new ArrayList<>();
    for (VariableJson varJson : content) {
      parameters.add(createMethodParameter(parent, varJson, parameters));
    }
    return parameters;
  }

  private GBBaseObject createMemberMethod(final GBSubObject parent, MemberMethodJson content) throws XynaException {
    DOM dataType = (DOM) object.getDtOrException();
    JavaOperation operation = new JavaOperation(dataType);
    operation.setLabel(content.getLabel());
    operation.setDocumentation(content.getDocumentation());
    operation.setImpl(content.getImplementation().strip());
    operation.setHasBeenPersisted(false);

    createMethodParameters(parent, content.getInputVars()).forEach(operation.getInputVars()::add);
    createMethodParameters(parent, content.getOutputVars()).forEach(operation.getOutputVars()::add);
    createMethodParameters(parent, content.getThrownExceptions()).stream()
      .filter(ExceptionVariable.class::isInstance)
      .map(ExceptionVariable.class::cast)
      .forEach(operation.getThrownExceptionsForMod()::add);

    List<String> usedNames = new ArrayList<String>();
    OperationInformation[] operationInformations = dataType.collectOperationsOfDOMHierarchy(true);
    for (OperationInformation curOpInfo : operationInformations) {
      usedNames.add(curOpInfo.getOperation().getName());
    }
    operation.setName(com.gip.xyna.xprc.xfractwfe.generation.xml.Utils.createUniqueJavaName(usedNames, content.getLabel(), false));

    return new GBBaseObject(dataType, new MemberMethod(operation));
  }

  private ScopeStep getParentScope(GBSubObject parent) {
    if (parent == null || parent.getStep() == null) {
      return null;
    }

    return parent.getStep().getParentScope();
  }

  private GBBaseObject createMapping(final GBSubObject parent, MappingJson content) throws XynaException {
    StepMapping sm = new StepMapping(getParentScope(parent), parent.getRoot().getWorkflow() );
    if(content.getIsCondition()) {
      sm.setIsConditionMapping(true);
    }
    sm.createEmpty(content.getLabel());

    return new GBBaseObject(sm);
  }

  private GBBaseObject createTemplateStep(final GBSubObject parent, TemplateJson content) throws XynaException {
    StepMapping st = new StepMapping(getParentScope(parent), object.getRoot().getWorkflow() );
    st.createTemplate();

    return new GBBaseObject(st);
  }
  
  private GBBaseObject createFormula(final GBSubObject parent, FormulaJson content) throws XynaException {
    List<Variable> variables = new ArrayList<Variable>();
    for (VariableJson variableJson : content.getVariables()) {
      Variable var = createParameter(parent, variableJson).getVariable();
      variables.add(var);
    }
    Formula formula = new Formula(content.getExpression(), variables);

    return new GBBaseObject(object.getStep(), formula);
  }

  private GBBaseObject createExpression(final GBSubObject parent, MappingJson content) throws XynaException {
    return new GBBaseObject(object.getStep(), content.getExpression());
  }
  
  private GBBaseObject createBranch(final GBSubObject parent, BranchJson content, PositionJson positionJson) throws XynaException {
    Branch distinctionBranch = new Branch(positionJson.getInsideIndex(), content.getExpression(), content.getLabel());
    return new GBBaseObject(object.getStep(), distinctionBranch);
  }

  private GBBaseObject createCase(final GBSubObject parent, CaseJson content) throws XynaException {
    Case distinctionCase = new Case(content.getExpression(), content.getLabel());
    return new GBBaseObject(object.getStep(), distinctionCase);
  }

  protected GBBaseObject createThrowStep(final GBSubObject parent, ThrowJson content) throws XynaException {
    StepThrow st = new StepThrow(getParentScope(parent), object.getRoot().getWorkflow());
    
    String fqn = content.getFqName().toString();
    if (fqn != null && fqn.equals("java.lang.Exception")) {
      fqn = "core.exception.Exception";
    }
    
    st.create(fqn, THROW_LABEL_PREFIX + " " + content.getLabel());
    return new GBBaseObject(st);
  }

  private GBBaseObject createRetryStep(final GBSubObject parent, RetryJson content) throws XynaException {
    StepRetry sr = new StepRetry(getParentScope(parent), object.getRoot().getWorkflow());
    sr.create(content.getLabel(), content.getDocumentation());

    return new GBBaseObject(sr);
  }
  
  private GBBaseObject createQueryFilterCriterion(FormulaJson content) throws XynaException {
    String expression = content.getExpression();
    if(expression == null || expression.length() == 0) {
      expression = "%0%";
    }
    return new GBBaseObject(object.getStep(), new QueryFilterCriterion(expression));
  }
  
  private GBBaseObject createQuerySortCriterion(FormulaJson content) throws XynaException {
    String expression = content.getExpression();
    if(expression == null || expression.length() == 0) {
      expression = "%0%";
    }
    return new GBBaseObject(object.getStep(), new QuerySortCriterion(expression));
  }
  
  private GBBaseObject createQuerySelectionMask(FormulaJson content) throws XynaException {
    String expression = content.getExpression();
    if(expression == null || expression.length() == 0) {
      expression = "%0%";
    }
    return new GBBaseObject(object.getStep(), new QuerySelectionMask(expression));
  }
  
  private GBBaseObject createStaticMethod(final GBSubObject parent, MemberServiceJson content) throws XynaException {
    DOM dataType = (DOM) object.getDtOrException();
    JavaOperation operation = new JavaOperation(dataType);
    operation.setLabel(content.getLabel());
    operation.setStatic(true);
    operation.setImpl("");

    List<String> usedNames = new ArrayList<>();
    OperationInformation[] operationInformations = dataType.collectOperationsOfDOMHierarchy(true);
    for (OperationInformation curOpInfo : operationInformations) {
      usedNames.add(curOpInfo.getOperation().getName());
    }
    operation.setName(com.gip.xyna.xprc.xfractwfe.generation.xml.Utils.createUniqueJavaName(usedNames, content.getLabel(), false));

    return new GBBaseObject(dataType, new GBBaseObject.StaticMethod(operation));
  }

  private GBBaseObject createLib(final GBSubObject parent, LibJson content) throws XynaException {
    Lib fileId = Lib.createWithFileId(content.getFileId());
    return new GBBaseObject(object.getDtOrException(), fileId);
  }

  /**
   * In case of a data type or exception type, the abstract-flag is set in case it's necessary.
   */
  protected void updateAbstractness(DomOrExceptionGenerationBase dtOrException) {
    if (dtOrException != null) {
      setAbstract(dtOrException, dtOrException.isAbstract());
    }
  }

  protected void setAbstract(DomOrExceptionGenerationBase dtOrException, Boolean isAbstract) {
    // data type has to be abstract when it contains at least one abstract operation
    if (dtOrException instanceof DOM) {
      for (OperationInformation operationInformation : ((DOM) dtOrException).collectOperationsOfDOMHierarchy(true)) {
        if (operationInformation.isAbstract()) {
          isAbstract = true;
          break;
        }
      }
    }

    if (isAbstract != null) {
      dtOrException.setIsAbstract(isAbstract);
    }
  }
  

  public boolean isSpecialInsertThrow(GBSubObject relativeToObject) {
    return object.getType() == ObjectType.variable && relativeToObject.getType() == ObjectType.step
        && object.getVariable().getVariable() != null
        && object.getVariable().getVariable().getIdentifiedVariable().getDomOrExceptionObject() instanceof ExceptionGeneration
        && relativeToObject.getStep() instanceof StepSerial;
  }
  
  public UpdateResponseJson specialInsertThrow(GBSubObject relativeToObject, Insertion insertion) throws XynaException {
    UpdateResponseJson response;
    ThrowJson throwJson = new ThrowJson();
    throwJson.setFqName(FQNameJson.ofPathAndName(object.getVariable().getVariable().getIdentifiedVariable().getFQClassName()));
    throwJson.setList(object.getVariable().getVariable().getIdentifiedVariable().isList());
    throwJson.setLabel(object.getVariable().getVariable().getIdentifiedVariable().getLabel());
    
    GBBaseObject newObject = createThrowStep(insertion.getParent(), throwJson);
    response = insertion.insert(newObject);
    
    return response;
  }
  

  public boolean isOperationInSelf(GBSubObject relativeToObject) {
    if (object.getType() == ObjectType.step) {
      Step compareStep = object.getStep();
      if (compareStep instanceof StepFunction && compareStep.getParentStep() != null && compareStep.getParentStep() instanceof StepCatch) {
        compareStep = compareStep.getParentStep();
      }

      Step containerStep = relativeToObject.getContainerStep();
      while (containerStep != null) {
        if (containerStep.equals(compareStep)) {
          return true;
        }

        containerStep = containerStep.getParentStep();
      }
    }

    return false;
  }


  protected boolean refreshDataFlow() {
    if (modification == null || modification.getObject() == null || modification.getObject().getDataflow() == null) {
      return false;
    }

    modification.getObject().getDataflow().applyDataflowToGB();
    modification.getObject().markAsModified();
    modification.getObject().refreshDataflow();

    return true;
  }

  protected void removeConstant(Variable variable, String branchId) throws UnsupportedOperationException {
    Dataflow df = modification.getObject().getDataflow();
    df.removeConstantValue(object, branchId);
  }

  protected Map<Step, List<AVariableIdentification>> getWrappingStepsVariables(Step step, VarUsageType varUsageType) {
    Map<Step, List<AVariableIdentification>> wrappingStepsVariables = new HashMap<Step, List<AVariableIdentification>>();
    if (step == null) {
      return wrappingStepsVariables;
    }

    Step parentStep = step.getParentStep();
    int levelIdx = 0;
    while (parentStep != null && !(parentStep instanceof WFStep) && levelIdx < Utils.MAX_RECURSIVE_PARENT_SEARCH) {
      if (parentStep instanceof StepChoice || parentStep instanceof StepForeach) {
        wrappingStepsVariables.put(parentStep, (modification.getObject().getVariableMap().identifyVariables(ObjectId.createStepId(parentStep)).getVariables(varUsageType)));
      }
      parentStep = parentStep.getParentStep();
      levelIdx++;
    }

    return wrappingStepsVariables;
  }
}
