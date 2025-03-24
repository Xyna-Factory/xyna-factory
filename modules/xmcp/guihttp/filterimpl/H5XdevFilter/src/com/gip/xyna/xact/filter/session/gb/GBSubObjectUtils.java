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
import java.util.List;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Branch;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Case;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.DTMetaTag;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Formula;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Lib;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberMethod;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberMethodInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberVar;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberVarInfo;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.StaticMethod;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Variable;
import com.gip.xyna.xact.filter.session.gb.adapter.ListAdapter;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepMapping;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepWithWfVars;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.ExpressionUtils;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.BranchInfo;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.CaseInfo;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.FormulaContainer;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;

import xmcp.processmodeller.datatypes.MetaTag;
import xnwh.persistence.QueryParameter;
import xnwh.persistence.SelectionMask;
import xnwh.persistence.SortCriterion;

public class GBSubObjectUtils {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(GBSubObjectUtils.class);
  
  public static class StepListAdapter extends ListAdapter<Step> {

    private StepSerial parent;
    private GenerationBaseObject gbo;

    public StepListAdapter(StepSerial parent, GenerationBaseObject gbo) {
      this.parent = parent;
      this.gbo = gbo;
    }

    @Override
    public Step get(int index) {
      return parent.getChildSteps().get(index);
    }

    @Override
    public int size() {
      return parent.getChildSteps().size();
    }

    @Override
    public void add(int index, Step element) {
      parent.addChild(index, element);
      gbo.getStepMap().addStep(element);

      Step stepToAdd = element;
      if (element instanceof StepCatch) {
        stepToAdd = ((StepCatch)element).getStepInTryBlock();
      }

      if(!(stepToAdd instanceof StepParallel)) {
        IdentifiedVariables vars = gbo.getVariableMap().identifyVariables(ObjectId.createStepId(stepToAdd));
        List<AVariableIdentification> outputVars = vars.getVariables(VarUsageType.output);
        for (int varNr = 0; varNr < outputVars.size(); varNr++ ) {
          AVariableIdentification var = outputVars.get(varNr);
          if (var.getIdentifiedVariable() == null || var.getIdentifiedVariable().isPrototype()) {
            continue;
          }
  
          AVariable clone = IdentifiedVariablesStepWithWfVars.createServiceOrExceptionVar(var.getIdentifiedVariable(), String.valueOf(stepToAdd.getParentWFObject().getNextXmlId()));
          stepToAdd.getOutputVarIds()[varNr] = clone.getId();
  
          //add StepId to SourceIds
          if(stepToAdd instanceof StepFunction && !clone.getSourceIds().contains(stepToAdd.getStepId())){
            clone.setSourceIds(stepToAdd.getStepId());
          }
  
          parent.addVar(clone);
        }
      }
    }

    @Override
    public Step remove(int index) {
      Step removed = parent.getChildSteps().remove(index);
      if( removed != null ) {
        gbo.getStepMap().removeStep(removed);
      }
      return removed;
    }
    
  }

  public static class VariableListAdapter extends ListAdapter<Variable> {

    private IdentifiedVariables identifiedVariables;
    private List<AVariableIdentification> varList;
    private VarUsageType usage;

    public VariableListAdapter(IdentifiedVariables identifiedVariables, VarUsageType usage) {
      this.identifiedVariables = identifiedVariables;
      this.varList = identifiedVariables.getListAdapter(usage);
      this.usage = usage;
    }

    @Override
    public Variable get(int index) {
      return new Variable(identifiedVariables, varList.get(index));
    }

    @Override
    public int size() {
      return varList.size();
    }

    @Override
    public void add(int index, Variable element) {
      varList.add(index, element.getVariable() );
    }

    @Override
    public boolean remove(Object o) {
      if( o instanceof Variable ) {
        return varList.remove(((Variable)o).getVariable());
      }
      return false;
    }
    
    @Override
    public Variable remove(int index) {
      AVariableIdentification removed = varList.remove(index);
      if( removed != null ) {
        return new Variable(identifiedVariables, removed);
      }
      return null;
    }
    
    @Override
    public int indexOf(Object o) {
      if( o instanceof Variable ) {
        return varList.indexOf(((Variable)o).getVariable());
      }
      return super.indexOf(o);
    }
    
    @Override
    public void move(int sourceIndex, int destinationIndex) {
      if(identifiedVariables instanceof IdentifiedVariablesStepMapping) {
        IdentifiedVariablesStepMapping identifiedVariablesStepMapping = (IdentifiedVariablesStepMapping)identifiedVariables;
        identifiedVariablesStepMapping.move(usage, sourceIndex, destinationIndex);
      } else {
        super.move(sourceIndex, destinationIndex);
      }
    }
    
  }


  public static class FormulaListAdapter extends ListAdapter<Formula> {

    private FormulaContainer formulaContainer;
    private IdentifiedVariables identifiedVariables;
    private GenerationBaseObject gbo;


    public FormulaListAdapter(FormulaContainer formulaContainer, IdentifiedVariables identifiedVariables, GenerationBaseObject gbo) {
      this.formulaContainer = formulaContainer;
      this.identifiedVariables = identifiedVariables;
      this.gbo = gbo;
    }


    @Override
    public Formula get(int index) {
      Formula formula = new Formula(formulaContainer.getFormula(index), null); // TODO: variable-list?
      return formula;
    }

    @Override
    public int size() {
      return formulaContainer.getFormulaCount();
    }

    @Override
    public void add(int index, Formula element) {
      formulaContainer.addFormula(index, element.getExpression());

      List<AVariableIdentification> varListAdapter = identifiedVariables.getListAdapter(VarUsageType.input);
      for (Variable var : element.getVariables()) {
        varListAdapter.add(var.getVariable()); // TODO: Could list contain a variable twice after this?
      }

      gbo.getStepMap().updateStep((Step)formulaContainer);
    }

  }


  public static class ExpressionListAdapter extends ListAdapter<String> { // TODO: remove and always use FormulaListAdapter, instead
    
    private StepMapping stepMapping;


    public ExpressionListAdapter(StepMapping stepMapping) {
      this.stepMapping = stepMapping;
    }


    @Override
    public String get(int index) {
      return stepMapping.getRawExpressions().get(index);
    }

    @Override
    public int size() {
      return stepMapping.getRawExpressions().size();
    }

    @Override
    public void add(int index, String element) {
      stepMapping.addFormula(index, element);
    }

    @Override
    public String remove(int index) {
      String removed = stepMapping.removeFormula(index);
      return removed;
    }

  }
  
  public static class QueryFilterListAdapter extends ListAdapter<String>{
    
    private StepFunction stepFuntion;
    private final GBSubObject gbSubObject;
    
    public QueryFilterListAdapter(StepFunction step, GBSubObject gbSubObject) {
      this.gbSubObject = gbSubObject;
      stepFuntion = step;
      if(stepFuntion.getQueryFilterConditions() == null) {
        stepFuntion.setQueryFilterConditions(new ArrayList<>(10));
      }
    }

    @Override
    public String get(int index) {
      return stepFuntion.getQueryFilterConditions().get(index);
    }

    @Override
    public int size() {
      return stepFuntion.getQueryFilterConditions().size();
    }
    
    @Override
    public void add(int index, String element) {
      stepFuntion.getQueryFilterConditions().add(index, element);
      QueryUtils.refreshQueryHelperMappingExpression(gbSubObject);
    }
    
    @Override
    public String remove(int index) {
      String result = stepFuntion.getQueryFilterConditions().remove(index);
      QueryUtils.refreshQueryHelperMappingExpression(gbSubObject);
      return result;
    }
  }
  
  public static class QuerySortListAdapter extends ListAdapter<SortCriterion>{
    
    private QueryParameter queryParameter;
    private AVariableIdentification variableIdentification;
    private GenerationBaseObject gbo;
    
    public QuerySortListAdapter(IdentifiedVariables identifiedVariables, GenerationBaseObject gbo) {
      this.gbo = gbo;
      List<AVariableIdentification> variableIdentifications = identifiedVariables.getVariables(VarUsageType.input);
      for (AVariableIdentification vi : variableIdentifications) {
        if(vi.getIdentifiedVariable().getVarName().equals(Tags.QUERY_CONST_QUERY_PARAMETER)) {
          this.variableIdentification = vi;
          queryParameter = (QueryParameter) vi.getConstantValue(gbo);
          break;
        }
      }
    }    
    
    @Override
    public SortCriterion get(int index) {
      return queryParameter.getSortCriterion().get(index);      
    }
    
    @Override
    public int size() {
      if(queryParameter != null && queryParameter.getSortCriterion() != null) {
        return queryParameter.getSortCriterion().size();
      }
      return 0;
    }
    
    @Override
    public void add(int index, SortCriterion element) {
      List<SortCriterion> criterions;
      if(queryParameter.getSortCriterion() != null) {
        criterions= new ArrayList<>(queryParameter.getSortCriterion().size());
        criterions.addAll(queryParameter.getSortCriterion());
      } else {
        criterions = new ArrayList<>();
      }
      criterions.add(index, element);
      queryParameter.setSortCriterion(criterions);
      saveQueryParameter(queryParameter);
    }
    
    @Override
    public SortCriterion remove(int index) {
      SortCriterion sc = queryParameter.getSortCriterion().remove(index);
      saveQueryParameter(queryParameter);
      return sc;
    }
    
    private void saveQueryParameter(QueryParameter queryParameter) {
      gbo.getDataflow().setConstantValue(variableIdentification, null, queryParameter);
    }
  }
  
  public static class QuerySelectionMaskListAdapter extends ListAdapter<String>{
    
    private final GBSubObject stepFunctionGBSubObject;
    private final Dataflow dataflow;
    private SelectionMask selectionMask;
    
    
    public QuerySelectionMaskListAdapter(GBSubObject stepFunctionGBSubObject, Dataflow dataflow) {
      this.dataflow = dataflow;
      this.stepFunctionGBSubObject = stepFunctionGBSubObject;
      this.selectionMask = QueryUtils.getSelectionMask(stepFunctionGBSubObject);
    }
    
    @Override
    public void add(int index, String element) {
      if (selectionMask.getColumns() == null) {
        selectionMask.setColumns(new ArrayList<String>());
      }
      selectionMask.getColumns().add(element);
      save();
    }
    
    @Override
    public String remove(int index) {
      if(selectionMask.getColumns() != null) {
        String result =  selectionMask.getColumns().remove(index);
        save();
        return result;
      }
      return null;
    }

    @Override
    public String get(int index) {
      if(selectionMask.getColumns() != null) {
        return selectionMask.getColumns().get(index);
      }
      return null;
    }

    @Override
    public int size() {
      if(selectionMask.getColumns() != null) {
        return selectionMask.getColumns().size();
      }
      return 0;
    }
    
    private void save() {
      QueryUtils.saveSelectionMask(stepFunctionGBSubObject, dataflow, selectionMask);
    }
    
  }


  public static class BranchListAdapter extends ListAdapter<Branch> {

    private Distinction distinctionStep;
    private GenerationBaseObject gbo;


    public BranchListAdapter(Distinction distinctionStep, GenerationBaseObject gbo) {
      this.distinctionStep = distinctionStep;
      this.gbo = gbo;
    }


    @Override
    public Branch get(int index) {
      BranchInfo branchInfo = distinctionStep.getBranchesForGUI().get(index);
      CaseInfo mainCase = branchInfo.getMainCase();
      Branch branch = new Branch(index, mainCase.getComplexName(), mainCase.getGuiName());

      return branch;
    }

    @Override
    public int size() {
      return distinctionStep.getBranchesForGUI().size();
    }

    @Override
    public void add(int index, Branch element) {
      List<AVariable> varsBefore = null;
      if(distinctionStep instanceof StepCatch) {
        varsBefore = new ArrayList<AVariable>(((StepCatch) distinctionStep).getParentWFObject().getWfAsStep().getChildStep().getVariablesAndExceptions());
      }
      distinctionStep.addBranch(index, element.getExpression(), element.getLabel());
      gbo.getStepMap().updateStep((Step)distinctionStep);
      
      if (distinctionStep instanceof StepCatch) {
        StepCatch stepCatch = (StepCatch) distinctionStep;
        gbo.getStepMap().updateStep(stepCatch.getStepInTryBlock());

        //remove children of generated variables
        List<AVariable> varsAfter = ((StepCatch) distinctionStep).getParentWFObject().getWfAsStep().getChildStep().getVariablesAndExceptions();
        varsAfter.removeAll(varsBefore);
        for (AVariable createdVariable : varsAfter) {
          List<AVariable> children = createdVariable.getChildren();
          if (children != null) {
            createdVariable.removeChildren(children);
          }
        }
      }
    }

    @Override
    public Branch remove(int index) {
      Branch removed = get(index);
      distinctionStep.removeBranch(index);
      gbo.getStepMap().updateStep((Step)distinctionStep);
      
      if(distinctionStep instanceof StepCatch) {
        StepCatch stepCatch = (StepCatch)distinctionStep;
        gbo.getStepMap().updateStep(stepCatch.getStepInTryBlock());
      }

      if (distinctionStep instanceof StepChoice) {
        ExpressionUtils.cleanUpChoiceInputsAndConditions((StepChoice) distinctionStep);
      }

      return removed;
    }

  }


  public static class CaseListAdapter extends ListAdapter<Case> {

    private StepChoice stepChoice;
    private int branchNo;


    public CaseListAdapter(StepChoice stepChoice, int branchNo) {
      this.stepChoice = stepChoice;
      this.branchNo = branchNo;
    }


    @Override
    public Case get(int index) {
      CaseInfo caseInfo = stepChoice.getBranchesForGUI().get(branchNo).getCases().get(index);
      Case distinctionCase = new Case(caseInfo.getComplexName(), caseInfo.getGuiName());
      return distinctionCase;
    }

    @Override
    public int size() {
      return stepChoice.getBranchesForGUI().get(branchNo).getCases().size();
    }

    @Override
    public void add(int index, Case element) {
      stepChoice.addCase(branchNo, index, element.getExpression(), element.getLabel());
    }

    @Override
    public Case remove(int index) {
      CaseInfo caseInfo = stepChoice.removeCase(index);
      Case removed = new Case(caseInfo.getComplexName(), caseInfo.getName());

      return removed;
    }

    @Override
    public int indexOf(Object o) {
      if (o instanceof Case) {
        return ((Case)o).getIndex();
      }

      return super.indexOf(o);
    }

  }


  public static class MemberVarListAdapter extends ListAdapter<MemberVar> {

    private DomOrExceptionGenerationBase dtOrException;

    public MemberVarListAdapter(DomOrExceptionGenerationBase dtOrException) {
      this.dtOrException = dtOrException;
    }

    @Override
    public MemberVar get(int index) {
      return new MemberVar(dtOrException.getMemberVars().get(index));
    }

    @Override
    public int size() {
      return dtOrException.getMemberVars().size();
    }

    @Override
    public void add(int index, MemberVar element) {
      dtOrException.addMemberVar(index, element.getVar());
    }

    @Override
    public boolean remove(Object o) {
      if( o instanceof MemberVar ) {
        return dtOrException.removeMemberVar(((MemberVar)o).getVar());
      }
      return false;
    }

    @Override
    public MemberVar remove(int index) {
      AVariable removed = dtOrException.removeMemberVar(index);
      if( removed != null ) {
        return new MemberVar(removed);
      }
      return null;
    }

    @Override
    public int indexOf(Object o) {
      if( o instanceof MemberVar ) {
        AVariable memberVar = ((MemberVar)o).getVar();
        return dtOrException.getMemberVars().indexOf(memberVar);
      }
      return super.indexOf(o);
    }
    
  }


  public static class MemberMethodListAdapter extends ListAdapter<MemberMethod> {
    
    private DOM dataType;
    
    public MemberMethodListAdapter(DOM dataType) {
      this.dataType = dataType;
    }
    
    @Override
    public MemberMethod get(int index) {
      return new MemberMethod(dataType.getOperations().get(index));
    }
    
    @Override
    public int size() {
      return dataType.getOperations().size();
    }
    
    @Override
    public void add(int index, MemberMethod element) {
      dataType.addOperation(index, element.getOperation());
    }
    
    @Override
    public boolean remove(Object o) {
      if( o instanceof MemberVar ) {
//        return dtOrException.removeOperation(((MemberMethod)o).getOperation()); TODO
      }
      return false;
    }
    
    // TODO: PMOD-TODO
//    @Override
//    public MemberVar remove(int index) {
//      AVariable removed = dtOrException.removeMemberVar(index);
//      if( removed != null ) {
//        return new MemberVar(removed);
//      }
//      return null;
//    }
    
//    @Override
//    public int indexOf(Object o) {
//      if( o instanceof MemberVar ) {
//        return dtOrException.indexOfMemberVar(((Variable)o).getVariable());
//      }
//      return super.indexOf(o);
//    }
    
  }
  
  public static class StaticMethodListAdapter extends ListAdapter<StaticMethod> {
    
    private DOM dataType;
    
    public StaticMethodListAdapter(DOM dataType) {
      this.dataType = dataType;
    }
    
    @Override
    public StaticMethod get(int index) {
      return new StaticMethod(dataType.getOperations().get(index));
    }
    
    @Override
    public int size() {
      return dataType.getOperations().size();
    }
    
    @Override
    public void add(int index, StaticMethod element) {
      dataType.addOperation(index, element.getOperation());
    }
    
    @Override
    public boolean remove(Object o) {
      return false;
    }
    
  }

  public static class LibListAdapter extends ListAdapter<Lib> {

    private GenerationBaseObject gbo;
    
    public LibListAdapter(GenerationBaseObject gbo) {
      this.gbo = gbo;
    }

    @Override
    public int size() {
      return gbo.getDOM().getAdditionalLibraries().size();
    }

    @Override
    public void add(int index, Lib element) {
      gbo.addSgLibToUpload(element.getFileId());

      FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
      TransientFile tFile = fm.retrieve(element.getFileId());
      gbo.getDOM().addAdditionalLibrary(index, tFile.getOriginalFilename());
    }

    @Override
    public Lib remove(int index) {
      gbo.addSgLibToDelete(get(index).getLibName());

      return Lib.createWithLibName(gbo.getDOM().deleteAdditionalLibrary(index));
    }

    @Override
    public Lib get(int index) {
      return Lib.createWithLibName(gbo.getDOM().getAdditionalLibraries().toArray(new String[size()])[index]);
    }

  }

  public static class MetaTagListAdapter extends ListAdapter<DTMetaTag> {

    private DomOrExceptionGenerationBase dtOrException;
    private MemberVarInfo memberVarInfo;
    private MemberMethodInfo memberMethodInfo;

    public MetaTagListAdapter(DomOrExceptionGenerationBase dtOrException, MemberVarInfo memberVarInfo, MemberMethodInfo memberMethodInfo) {
      this.dtOrException = dtOrException;
      this.memberVarInfo = memberVarInfo;
      this.memberMethodInfo = memberMethodInfo;
    }

    @Override
    public DTMetaTag get(int index) {
      String tagXml = getMetaTags().get(index);
      return createDTMetaTag(tagXml, index);
    }

    @Override
    public DTMetaTag set(int index, DTMetaTag element) {
      getMetaTags().set(index, element.getMetaTag().getTag());
      return get(index);
    }

    @Override
    public int size() {
      return getMetaTags().size();
    }

    @Override
    public void add(int index, DTMetaTag element) {
      List<String> metaTags = getMetaTags();
      metaTags.add(index, element.getMetaTag().getTag());
      element.setIdx(index);
    }

    @Override
    public DTMetaTag remove(int index) {
      List<String> metaTags = getMetaTags();
      DTMetaTag removedTag = createDTMetaTag(metaTags.get(index), index);
      metaTags.remove(index);

      return removedTag;
    }

    @Override
    public int indexOf(Object o) {
      DTMetaTag dtMetaTag = ((DTMetaTag)o);
      return dtMetaTag.getIdx();
    }

    private List<String> getMetaTags() {
      List<String> metaTags;
      if (memberVarInfo != null) {
        AVariable var = dtOrException.getMemberVars().get(memberVarInfo.getIndex());
        metaTags = var.getUnknownMetaTags() != null ? var.getUnknownMetaTags() : new ArrayList<String>();
        var.setUnknownMetaTags(metaTags);
      } else if (memberMethodInfo != null) {
        Operation operation = ((DOM)dtOrException).getOperations().get(memberMethodInfo.getIndex());
        metaTags = operation.getUnknownMetaTags() != null ? operation.getUnknownMetaTags() : new ArrayList<String>();
        operation.setUnknownMetaTags(metaTags);
      } else {
        metaTags = dtOrException.getUnknownMetaTags() != null ? dtOrException.getUnknownMetaTags() : new ArrayList<String>();
        dtOrException.setUnknownMetaTags(metaTags);
      }
      
      return metaTags;
    }
    
  }

  public static DTMetaTag createDTMetaTag(String xml, int idx) {
    MetaTag metaTag = new MetaTag(xml.strip());
    return new DTMetaTag(metaTag, idx);
  }

  public static abstract class ObjectAdapter<T> {

    public static ObjectAdapter<?> forType(ObjectType type) {
      switch( type ) {
      case step:
        return new StepObjectAdapter();
      case variable:
        return new VariableObjectAdapter();
      case formula:
        return new FormulaObjectAdapter();
      case expression:
        return new ExpressionObjectAdapter();
      case distinctionBranch:
        return new BranchObjectAdapter();
      case distinctionCase:
        return new CaseObjectAdapter();
      case queryFilterCriterion:
        return new QueryFilterObjectAdapter();
      case querySortCriterion:
        return new QuerySortingObjectAdapter();
      case querySelectionMask:
        return new QuerySelectionMaskgObjectAdapter();
      case memberVar:
        return new MemberVarObjectAdapter();
      case memberMethod:
        return new MemberMethodObjectAdapter();
      case staticMethod:
        return new StaticMethodObjectAdapter();
      case metaTag:
        return new MetaTagObjectAdapter();
      case serviceGroupLib:
        return new LibObjectAdapter();
      default:
        break;
      }
      return null;
    }

    public abstract List<T> getListAdapter(GBSubObject insideObject);

    public abstract T getObject(GBBaseObject object);
    
  }
  public static class StepObjectAdapter extends ObjectAdapter<Step> {

    @Override
    public List<Step> getListAdapter(GBSubObject object) {
      return object.getStepListAdapter();
    }

    @Override
    public Step getObject(GBBaseObject object) {
      return object.getStep();
    }
    
  }
  
  public static class VariableObjectAdapter extends ObjectAdapter<Variable> {

    @Override
    public List<Variable> getListAdapter(GBSubObject object) {
      return object.getVariableListAdapter();
    }

    @Override
    public Variable getObject(GBBaseObject object) {
      return object.getVariable();
    }
    
  }

  public static class FormulaObjectAdapter extends ObjectAdapter<Formula> {

    @Override
    public List<Formula> getListAdapter(GBSubObject object) {
      return object.getFormulaListAdapter();
    }

    @Override
    public Formula getObject(GBBaseObject object) {
      return object.getFormula();
    }

  }

  public static class ExpressionObjectAdapter extends ObjectAdapter<String> {
    
    @Override
    public List<String> getListAdapter(GBSubObject object) {
      return object.getExpressionListAdapter();
    }
    
    @Override
    public String getObject(GBBaseObject object) {
      return object.getExpression();
    }
    
  }
  
  public static class BranchObjectAdapter extends ObjectAdapter<Branch> {

    @Override
    public List<Branch> getListAdapter(GBSubObject object) {
      return object.getBranchListAdapter();
    }

    @Override
    public Branch getObject(GBBaseObject object) {
      return object.getBranchInfo();
    }

  }

  
  public static class CaseObjectAdapter extends ObjectAdapter<Case> {
    
    @Override
    public List<Case> getListAdapter(GBSubObject object) {
      return object.getCaseListAdapter();
    }
    
    @Override
    public Case getObject(GBBaseObject object) {
      return object.getCaseInfo();
    }
    
  }
  
  public static class QueryFilterObjectAdapter extends ObjectAdapter<String> {

    @Override
    public List<String> getListAdapter(GBSubObject object) {
      return object.getQueryFilterListAdapter();
    }

    @Override
    public String getObject(GBBaseObject object) {
      return object.getQueryFilterCriterion().getExpression();
    }

  }
  
  public static class QuerySortingObjectAdapter extends ObjectAdapter<SortCriterion> {

    @Override
    public List<SortCriterion> getListAdapter(GBSubObject object) {
      return object.getQuerySortingListAdapter();
    }

    @Override
    public SortCriterion getObject(GBBaseObject object) {
      return new SortCriterion(object.getQuerySortCriterion().getExpression(), false);
    }

  }
  
  public static class QuerySelectionMaskgObjectAdapter extends ObjectAdapter<String> {

    @Override
    public List<String> getListAdapter(GBSubObject object) {
      try {
        return object.getQuerySelectionMaskListAdapter();
      } catch (UnknownObjectIdException | MissingObjectException | XynaException e) {
        Utils.logError(e);
        return null;
      }
    }

    @Override
    public String getObject(GBBaseObject object) {
      return object.getQuerySelectionMask().getExpression();
    }

  }

  public static class MemberVarObjectAdapter extends ObjectAdapter<MemberVar> {

    @Override
    public List<MemberVar> getListAdapter(GBSubObject object) {
      return object.getMemberVarListAdapter();
    }

    @Override
    public MemberVar getObject(GBBaseObject object) {
      if (object.getMemberVar() != null) {
        return object.getMemberVar();
      }

      AVariable var = object.getDtOrException().getMemberVars().get(object.getMemberVarInfo().getIndex());
      return new MemberVar(var);
    }
    
  }

  public static class MemberMethodObjectAdapter extends ObjectAdapter<MemberMethod> {
    
    @Override
    public List<MemberMethod> getListAdapter(GBSubObject object) {
      return object.getMemberMethodListAdapter();
    }
    
    @Override
    public MemberMethod getObject(GBBaseObject object) {
      return object.getMemberMethod();
    }
    
  }
  
  public static class StaticMethodObjectAdapter extends ObjectAdapter<StaticMethod> {
    
    @Override
    public List<StaticMethod> getListAdapter(GBSubObject object) {
      return object.getStaticMethodListAdapter();
    }
    
    @Override
    public StaticMethod getObject(GBBaseObject object) {
      return object.getStaticMethod();
    }
    
  }
  
  public static class MethodVarObjectAdapter extends ObjectAdapter<MemberVar> {

    @Override
    public List<MemberVar> getListAdapter(GBSubObject object) {
      return object.getMemberVarListAdapter();
    }

    @Override
    public MemberVar getObject(GBBaseObject object) {
      return object.getMemberVar();
    }
    
  }

  public static class MetaTagObjectAdapter extends ObjectAdapter<DTMetaTag> {

    @Override
    public List<DTMetaTag> getListAdapter(GBSubObject object) {
      return object.getMetaTagListAdapter();
    }

    @Override
    public DTMetaTag getObject(GBBaseObject object) {
      return object.getMetaTag();
    }

  }

  public static class LibObjectAdapter extends ObjectAdapter<Lib> {
    
    @Override
    public List<Lib> getListAdapter(GBSubObject object) {
      return object.getLibListAdapter();
    }
    
    @Override
    public Lib getObject(GBBaseObject object) {
      return object.getLib();
    }
    
  }

}
