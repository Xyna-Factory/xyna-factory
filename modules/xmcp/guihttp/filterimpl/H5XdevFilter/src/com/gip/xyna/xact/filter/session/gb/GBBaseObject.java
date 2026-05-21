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

import java.util.List;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

import xmcp.processmodeller.datatypes.MetaTag;

public class GBBaseObject {

  protected ObjectType type; // FIXME: better use sub-classes, instead
  protected Step step;
  protected WF workflow;
  protected Variable variable;
  protected FormulaInfo formulaInfo;
  protected Case caseInfo;
  protected CaseArea caseAreaInfo;
  protected Formula formula;
  protected String expression;
  protected Branch branchInfo;
  
  protected QueryFilterCriterion queryFilterCriterion;
  protected QuerySortCriterion querySortCriterion;
  protected QuerySelectionMask querySelectionMask;

  // Datatype Modeller
  protected DomOrExceptionGenerationBase dtOrException;
  protected MemberVarInfo memberVarInfo;
  protected MemberVar memberVar;
  protected MemberMethodInfo memberMethodInfo;
  protected MemberMethod memberMethod;
  
  // Servicegroups
  protected StaticMethod staticMethod;
  protected LibInfo libInfo;
  protected Lib lib;
  
  // Datatype
  protected MethodVarInfo methodVarInfo;
  protected Operation operation;
  protected Integer operationIndex;
  protected DTMetaTag metaTag;

  public GBBaseObject() {
    this.type = ObjectType.clipboardEntry;
  }

  public GBBaseObject(WF workflow) {
    this.type = ObjectType.workflow;
    this.workflow = workflow;
  }

  public GBBaseObject(Step step) {
    this.type = ObjectType.step;
    this.step = step;
  }

  public GBBaseObject(Step step, Variable variable) {
    this.type = ObjectType.variable;
    this.step = step;
    this.variable = variable;
  }

  public GBBaseObject(Step step, Formula formula) {
    this.type = ObjectType.formula;
    this.step = step;
    this.formula = formula;
  }

  public GBBaseObject(Step step, String expression) {
    this.type = ObjectType.expression;
    this.step = step;
    this.expression = expression;
  }

  public GBBaseObject(Step step, FormulaInfo formulaInfo) {
    this.type = ObjectType.expression;
    this.step = step;
    this.formulaInfo = formulaInfo;
  }

  public GBBaseObject(Step step, Case caseInfo) {
    this.type = ObjectType.distinctionCase;
    this.step = step;
    this.caseInfo = caseInfo;
  }

  public GBBaseObject(Step step, CaseArea caseAreaInfo) {
    this.type = ObjectType.caseArea;
    this.step = step;
    this.caseAreaInfo = caseAreaInfo;
  }
  
  public GBBaseObject(Step step, Branch distinctionBranch) {
    this.type = ObjectType.distinctionBranch;
    this.step = step;
    this.branchInfo = distinctionBranch;
  }
  
  public GBBaseObject(Step step, QueryFilterCriterion queryFilterCriterion) {
    this.type = ObjectType.queryFilterCriterion;
    this.step = step;
    this.queryFilterCriterion = queryFilterCriterion;
  }
  
  public GBBaseObject(Step step, QuerySortCriterion querySortCriterion) {
    this.type = ObjectType.querySortCriterion;
    this.step = step;
    this.querySortCriterion = querySortCriterion;
  }
  
  public GBBaseObject(Step step, QuerySelectionMask querySelectionMask) {
    this.type = ObjectType.querySelectionMask;
    this.step = step;
    this.querySelectionMask = querySelectionMask;
  }
  
  public GBBaseObject(DomOrExceptionGenerationBase dtOrException) {
    this.type = ObjectType.datatype;
    this.dtOrException = dtOrException;
  }
  
  public GBBaseObject(DomOrExceptionGenerationBase dtOrException, Operation operation) {
    this.type = ObjectType.operation;
    this.dtOrException = dtOrException;
    this.operation = operation;
    operationIndex = Utils.getOperationIndex(operation);
  }

  public GBBaseObject(DomOrExceptionGenerationBase dtOrException, MemberVarInfo memberVarInfo) {
    this.type = ObjectType.memberVar;
    this.dtOrException = dtOrException;
    this.memberVarInfo = memberVarInfo;
  }

  public GBBaseObject(DomOrExceptionGenerationBase dtOrException, MemberVar memberVar) {
    this.type = ObjectType.memberVar;
    this.dtOrException = dtOrException;
    this.memberVar = memberVar;
  }

  public GBBaseObject(DomOrExceptionGenerationBase dtOrException, MemberMethodInfo memberMethodInfo) {
    this.type = ObjectType.memberMethod;
    this.dtOrException = dtOrException;
    this.memberMethodInfo = memberMethodInfo;
    if(dtOrException instanceof DOM) {
      OperationInformation[] operationInformations = ((DOM)dtOrException).collectOperationsOfDOMHierarchy(true);
      this.operation = operationInformations[memberMethodInfo.getIndex()].getOperation();
    }
    operationIndex = memberMethodInfo.getIndex();
  }

  public GBBaseObject(DomOrExceptionGenerationBase dtOrException, MemberMethod memberMethod) {
    this.type = ObjectType.memberMethod;
    this.dtOrException = dtOrException;
    this.memberMethod = memberMethod;
    this.operation = memberMethod.getOperation();
  }

  public GBBaseObject(DomOrExceptionGenerationBase dtOrException, DTMetaTag metaTag) {
    this.type = ObjectType.metaTag;
    this.dtOrException = dtOrException;
    this.metaTag = metaTag;
  }

  public GBBaseObject(DomOrExceptionGenerationBase dtOrException, ObjectType objectType) {
    this.type = objectType;
    this.dtOrException = dtOrException;
  }
  
  public GBBaseObject(DomOrExceptionGenerationBase dtOrException, StaticMethod staticMethod) {
    this.type = ObjectType.staticMethod;
    this.dtOrException = dtOrException;
    this.staticMethod = staticMethod;
    this.operation = staticMethod.getOperation();
    operationIndex = Utils.getOperationIndex(operation);
  }
  
  public GBBaseObject(DomOrExceptionGenerationBase dtOrException, MethodVarInfo methodInfo) {
    this.type = ObjectType.methodVarArea;
    this.dtOrException = dtOrException;
    this.methodVarInfo = methodInfo;
    this.operation = Utils.getOperationByIndex((DOM)dtOrException, methodInfo.getIndex());
    this.operationIndex = methodInfo.getIndex();
  }

  public GBBaseObject(DOM serviceGroup, LibInfo libInfo) {
    this.type = ObjectType.methodVarArea;
    this.dtOrException = serviceGroup;
    this.libInfo = libInfo;
  }

  public GBBaseObject(DomOrExceptionGenerationBase dtOrException, Operation operation, Variable variable) {
    this.type = ObjectType.variable;
    this.dtOrException = dtOrException;
    this.variable = variable;
    this.operation = operation;
  }

  public GBBaseObject(DomOrExceptionGenerationBase dtOrException, Lib fileId) {
    this.type = ObjectType.serviceGroupLib;
    this.dtOrException = dtOrException;
    this.lib = fileId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((step == null) ? 0 : step.hashCode());
    result = prime * result + ((variable == null) ? 0 : variable.hashCode());
    result = prime * result + ((workflow == null) ? 0 : workflow.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    GBBaseObject other = (GBBaseObject) obj;
    if (type != other.type)
      return false;
    if (step != other.step ) {
      return false;
    }
    if (workflow != other.workflow ) {
      return false;
    }
    if (step == null) {
      if (other.step != null)
        return false;
    } else if (!step.equals(other.step))
      return false;
    if (type != other.type)
      return false;
    if (variable == null) {
      if (other.variable != null)
        return false;
    } else if (!variable.equals(other.variable))
      return false;
    if (workflow == null) {
      if (other.workflow != null)
        return false;
    } else if (!workflow.equals(other.workflow))
      return false;
    return true;
  }

  
  public QueryFilterCriterion getQueryFilterCriterion() {
    return queryFilterCriterion;
  }
  
  public ObjectType getType() {
    return type;
  }

  public Step getStep() {
    return step;
  }

  public Step getContainerStep() {
    if (getType() != ObjectType.step) {
      // when represented object is not a step (e.g. it could be a branch, instead), the step variable stores the container
      return getStep();
    } else {
      // determine container the step is in
      Step step = getStep();
      if (step instanceof StepFunction && step.getParentStep() != null && step.getParentStep() instanceof StepCatch) {
        step = step.getParentStep();
      }

      return step.getParentStep();
    }
  }

  public WF getWorkflow() {
    return workflow;
  }

  public Variable getVariable() {
    return variable;
  }

  public CaseArea getCaseAreaInfo() {
    return caseAreaInfo;
  }

  public Formula getFormula() {
    return formula;
  }

  public String getExpression() {
    return expression;
  }
  
  public Branch getBranchInfo() {
    return branchInfo;
  }

  public FormulaInfo getFormulaInfo() {
    return formulaInfo;
  }

  public Case getCaseInfo() {
    return caseInfo;
  }

  public QuerySortCriterion getQuerySortCriterion() {
    return querySortCriterion;
  }

  // data type modeller

  public DomOrExceptionGenerationBase getDtOrException() {
    return dtOrException;
  }

  public MemberVarInfo getMemberVarInfo() {
    return memberVarInfo;
  }

  public MemberVar getMemberVar() {
    return memberVar;
  }

  public LibInfo getLibInfo() {
    return libInfo;
  }

  public Lib getLib() {
    return lib;
  }

  public MemberMethodInfo getMemberMethodInfo() {
    return memberMethodInfo;
  }

  public MemberMethod getMemberMethod() {
    return memberMethod;
  }
  
  public StaticMethod getStaticMethod() {
    return staticMethod;
  }

  public Operation getOperation() {
    return operation;
  }
  
  public Integer getOperationIndex() {
    return operationIndex;
  }
  
  public DTMetaTag getMetaTag() {
    return metaTag;
  }
  
  public QuerySelectionMask getQuerySelectionMask() {
    return querySelectionMask;
  }

  public static abstract class ExpressionInfo {
    private VarUsageType usage;
    private int index;

    public ExpressionInfo(Pair<VarUsageType, Integer> expressionInfo) {
      this.usage = expressionInfo.getFirst();
      this.index = expressionInfo.getSecond();
    }

    public VarUsageType getUsage() {
      return usage;
    }

    public int getIndex() {
      return index;
    }
  }

  public static class FormulaInfo extends ExpressionInfo {
    public FormulaInfo(int formulaNumber) {
      super(new Pair<VarUsageType, Integer>(VarUsageType.input, formulaNumber));
    }
  }

//  public static class CaseInfo extends ExpressionInfo {
//    public CaseInfo(int caseNumber) {
//      super(new Pair<VarUsageType, Integer>(VarUsageType.input, caseNumber));
//    }
//  }
  public static class Case {
    private int index;
    private String expression;
    private String label;


    public Case(int index) {
      this.index = index;
    }


    public Case(String expression, String label) {
      this.expression = expression;
      this.label = label;
    }


    public int getIndex() {
      return index;
    }

    public String getExpression() {
      return expression;
    }

    public String getLabel() {
      return label;
    }
  }

  public static class CaseArea {

    private int caseAreaNo;


    public CaseArea(int caseAreaNo) {
      this.caseAreaNo = caseAreaNo;
    }

    public void setCaseAreaNo(int caseAreaNo) {
      this.caseAreaNo = caseAreaNo;
    }
    
    public int getCaseAreaNo() {
      return caseAreaNo;
    }
  }

  public static class Variable extends ExpressionInfo {
    private IdentifiedVariables identifiedVariables;
    private AVariableIdentification variable;
    
    public Variable(IdentifiedVariables identifiedVariables, Pair<VarUsageType, Integer> variableInfo) {
      super(variableInfo);
      
      this.identifiedVariables = identifiedVariables;
      this.variable = identifiedVariables.getVariable(getUsage(), getIndex());
    }
  
    public Variable(IdentifiedVariables identifiedVariables, AVariableIdentification variable) {
      super(new Pair<VarUsageType, Integer>(null, -1));
      
      this.identifiedVariables = identifiedVariables;
      this.variable = variable;
    }

    public AVariableIdentification getVariable() {
      return variable;
    }
    
    public IdentifiedVariables getIdentifiedVariables() {
      return identifiedVariables;
    }
  }


  public static class Formula {
    private String expression;
    private List<Variable> variables;


    public Formula(String expression, List<Variable> variables) {
      this.expression = expression;
      this.variables = variables;
    }


    public String getExpression() {
      return expression;
    }

    public List<Variable> getVariables() {
      return variables;
    }
  }


  public static class Branch { // TODO: split in Branch and BranchInfo
    private String expression;
    private String label;
    private int branchNr;


    public Branch(int branchNr, String expression, String label) {
      this.branchNr = branchNr;
      this.expression = expression;
      this.label = label;
    }

    public Branch(int branchNr) {
      this.branchNr = branchNr;
    }

    public String getExpression() {
      return expression;
    }

    public String getLabel() {
      return label;
    }

    public int getBranchNr() {
      return branchNr;
    }

    // TODO: Necessary for moving branches within CB? Or maybe a solution similar to how variables can be moved within input/output?
//    @Override
//    public boolean equals(Object obj) {
//      if (super.equals(obj)) {
//        return true;
//      }
//
//      if ( !(obj instanceof Branch) ) {
//        return false;
//      }
//
//      return branchNr == ((Branch)obj).getBranchNr();
//    }
  }
  
  public static class QueryFilterCriterion {
    
    private int index;
    private String expression;        
    
    public QueryFilterCriterion(String expression) {
      this.expression = expression;
      this.index = -1;
    }
    
    public QueryFilterCriterion(int index) {
      this.index = index;
    }
    
    public int getIndex() {
      return index;
    }
    
    public String getExpression() {
      return expression;
    }
  }
  
  public static class QuerySortCriterion {
    
    private int index;
    private String expression;

    public QuerySortCriterion(String expression) {
      this.expression = expression;
      this.index = -1;
    }
    
    public QuerySortCriterion(int index) {
      this.index = index;
    }
    
    public int getIndex() {
      return index;
    }
    
    public String getExpression() {
      return expression;
    }
  }
  
  public static class QuerySelectionMask {
    
    private int index;
    private String expression;

    public QuerySelectionMask(String expression) {
      this.expression = expression;
      this.index = -1;
    }
    
    public QuerySelectionMask(int index) {
      this.index = index;
    }
    
    public int getIndex() {
      return index;
    }
    
    public String getExpression() {
      return expression;
    }
  }


  public static class MemberVarInfo {

    private int index;

    public MemberVarInfo(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }
  }


  public static class MemberVar {

    private AVariable var;

    public MemberVar(AVariable var) {
      this.var = var;
    }

    public AVariable getVar() {
      return var;
    }
  }


  public static class MemberMethodInfo {

    private int index;

    public MemberMethodInfo(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }
  }
  
  public static class MethodVarInfo {

    private int index;

    public MethodVarInfo(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }
  }


  public static class MemberMethod {

    private Operation operation;

    public MemberMethod(Operation operation) {
      this.operation = operation;
    }

    public Operation getOperation() {
      return operation;
    }
  }

  public static class DTMetaTag {

    private MetaTag metaTag;
    private Integer idx;

    public DTMetaTag(MetaTag metaTag) {
      this(metaTag, null);
    }

    public DTMetaTag(MetaTag metaTag, Integer idx) {
      this.metaTag = metaTag;
      this.idx = idx;
    }

    public MetaTag getMetaTag() {
      return metaTag;
    }

    public Integer getIdx() {
      return idx;
    }

    public void setIdx(Integer idx) {
      this.idx = idx;
    }
  }
  
  public static class StaticMethod {

    private Operation operation;

    public StaticMethod(Operation operation) {
      this.operation = operation;
    }

    public Operation getOperation() {
      return operation;
    }
  }

  public static class LibInfo {
    
    private int index;

    public LibInfo(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }

  }

  public static class Lib {

    private String fileId;
    private String libName;

    private Lib() {}

    public static Lib createWithFileId(String fileId) {
      Lib lib = new Lib();
      lib.fileId = fileId;

      return lib;
    }

    public static Lib createWithLibName(String libName) {
      Lib lib = new Lib();
      lib.libName = libName;

      return lib;
    }
    
    public String getFileId() {
      return fileId;
    }

    public String getLibName() {
      return libName;
    }

  }
}
