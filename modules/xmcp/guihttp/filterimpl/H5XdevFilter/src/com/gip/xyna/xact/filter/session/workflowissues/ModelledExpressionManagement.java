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

package com.gip.xyna.xact.filter.session.workflowissues;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.XMOMLoader;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.xfractwfe.formula.AndOperator;
import com.gip.xyna.xprc.xfractwfe.formula.BaseType;
import com.gip.xyna.xprc.xfractwfe.formula.DivideOperator;
import com.gip.xyna.xprc.xfractwfe.formula.EqualsOperator;
import com.gip.xyna.xprc.xfractwfe.formula.Expression;
import com.gip.xyna.xprc.xfractwfe.formula.Expression2Args;
import com.gip.xyna.xprc.xfractwfe.formula.Function;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionParameterTypeDefinition;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression.CastExpression;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression.DynamicResultTypExpression;
import com.gip.xyna.xprc.xfractwfe.formula.Functions;
import com.gip.xyna.xprc.xfractwfe.formula.GtOperator;
import com.gip.xyna.xprc.xfractwfe.formula.GteOperator;
import com.gip.xyna.xprc.xfractwfe.formula.LiteralExpression;
import com.gip.xyna.xprc.xfractwfe.formula.LtOperator;
import com.gip.xyna.xprc.xfractwfe.formula.LteOperator;
import com.gip.xyna.xprc.xfractwfe.formula.MinusOperator;
import com.gip.xyna.xprc.xfractwfe.formula.MultiplyOperator;
import com.gip.xyna.xprc.xfractwfe.formula.Not;
import com.gip.xyna.xprc.xfractwfe.formula.NotEqualsOperator;
import com.gip.xyna.xprc.xfractwfe.formula.Operator;
import com.gip.xyna.xprc.xfractwfe.formula.OrOperator;
import com.gip.xyna.xprc.xfractwfe.formula.PlusOperator;
import com.gip.xyna.xprc.xfractwfe.formula.SingleVarExpression;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;
import com.gip.xyna.xprc.xfractwfe.formula.VariableInstanceFunctionIncovation;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.EmptyVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedVariable;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.VariableInfo;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedVariable.StepBasedType;



public class ModelledExpressionManagement {

  private static class InvalidFunctionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

  }
  
  private static class OperatorTypes {

    private TypeInfo resultType;
    private TypeInfo exp1Type;
    private TypeInfo exp2Type;


    public OperatorTypes(TypeInfo resultType, TypeInfo exp1Type, TypeInfo exp2Type) {
      this.resultType = resultType;
      this.exp1Type = exp1Type;
      this.exp2Type = exp2Type;
    }


    public TypeInfo getResultType() {
      return resultType;
    }


    public TypeInfo getExp1Type() {
      return exp1Type;
    }


    public TypeInfo getExp2Type() {
      return exp2Type;
    }
  }

  
  public boolean areFunctionsInAssignmentExpressionValid(ModelledExpression me, Step step, VariableContextIdentification vic, boolean isAssignExp) {
    FunctionTypeCheckVisitor visitor = new FunctionTypeCheckVisitor(vic, isAssignExp, step);
    return visitor.check(me);
  }

  
  
  private static class IsSingleVarExpressionVisitor extends EmptyVisitor {

    private class IsSingleVarExpression extends RuntimeException{
      private static final long serialVersionUID = 1L;
      
    }
    
    private class IsNotSingleVarExpression extends RuntimeException {
      private static final long serialVersionUID = 1L;
      
    }
    
    public boolean checkIsSingleVarTargetExpression(ModelledExpression me) {
      
      try {
        me.visitTargetExpression(this);
      } catch(IsSingleVarExpression e) {
        return true;
      } catch(IsNotSingleVarExpression e) {
        return false;
      }
      
      return false;
    }
    
    
    @Override
    public void variableStarts(Variable variable) {
      throw new IsSingleVarExpression();
    }
    
    @Override
    public void functionStarts(FunctionExpression fe) {
      //only cast is allowed
      if(fe instanceof CastExpression) {
        throw new IsSingleVarExpression();
      }
      throw new IsNotSingleVarExpression();
    }
    
    @Override
    public void expression2ArgsStarts(Expression2Args expression) {
      throw new IsNotSingleVarExpression();
    }
    
    @Override
    public void literalExpression(LiteralExpression expression) {
      throw new IsNotSingleVarExpression();
    }
    
    @Override
    public void notStarts(Not not) {
      throw new IsNotSingleVarExpression();
    }
    
  }

  private static class FunctionTypeCheckVisitor extends EmptyVisitor {

    private TypeInfo sourceTi;
    private TypeInfo targetTi;

    private boolean settingSourceExpression;
    private VariableContextIdentification vic;
    private boolean isAssignExpression;
    private List<Expression> openIndexDefs;
    private boolean nextIsIndexDef;
    private Step step;
    private Stack<Variable> currentVariable; //currently finished variable -> before instance services
    private Stack<Integer> currentPartIndex;
    
    private static final Map<Class<? extends Operator>, OperatorTypes> operatorResultTypes = initOperatorResultTypes();
    
    public FunctionTypeCheckVisitor(VariableContextIdentification vic, boolean isAssignExpression, Step step) {
      this.vic = vic;
      this.isAssignExpression = isAssignExpression;
      this.step = step;
      this.openIndexDefs = new ArrayList<Expression>();
      this.nextIsIndexDef = false;
      this.currentVariable = new Stack<Variable>();
      this.currentPartIndex = new Stack<Integer>();
    }
    
    private static Map<Class<? extends Operator>, OperatorTypes> initOperatorResultTypes() {
      Map<Class<? extends Operator>, OperatorTypes> result = new HashMap<Class<? extends Operator>, OperatorTypes>();
      TypeInfo booleanType = new TypeInfo(PrimitiveType.BOOLEAN);
      TypeInfo numberType = TypeInfo.ANYNUMBER;
      TypeInfo anyType = TypeInfo.ANY;
      
      OperatorTypes boolBoolBool = new OperatorTypes(booleanType, booleanType, booleanType);
      OperatorTypes numNumNum = new OperatorTypes(numberType, numberType, numberType);
      OperatorTypes boolNumNum = new OperatorTypes(booleanType, numberType, numberType);
      OperatorTypes boolAnyAny = new OperatorTypes(booleanType, anyType, anyType);

      result.put(AndOperator.class, boolBoolBool);
      result.put(DivideOperator.class, numNumNum);
      result.put(EqualsOperator.class, boolAnyAny);
      result.put(GteOperator.class, boolNumNum);
      result.put(GtOperator.class, boolNumNum);
      result.put(LteOperator.class, boolNumNum);
      result.put(LtOperator.class, boolNumNum);
      result.put(MinusOperator.class, numNumNum);
      result.put(MultiplyOperator.class, numNumNum);
      result.put(NotEqualsOperator.class, boolAnyAny);
      result.put(OrOperator.class, boolBoolBool);
      result.put(PlusOperator.class, numNumNum);
      
      return result;
    }
    


    public boolean check(ModelledExpression me) {
      try {
        settingSourceExpression = false;
        me.visitTargetExpression(this);

        if (isAssignExpression) {
          settingSourceExpression = true;
          me.visitSourceExpression(this);
          checkAssignment();
          //targetExpression must be SingleVariableExpression
          IsSingleVarExpressionVisitor v = new IsSingleVarExpressionVisitor();
          if(!v.checkIsSingleVarTargetExpression(me)) {
            return false;
          }
          
        } else {
          //result type  (targetTi) should be boolean
          if(targetTi == null) {
            throw new InvalidFunctionException();
          }
          BaseType bt = targetTi.getBaseType();
          if(bt == null) {
            throw new InvalidFunctionException();
          }
          if(bt != BaseType.BOOLEAN_OBJECT && bt != BaseType.BOOLEAN_PRIMITIVE) {
            throw new InvalidFunctionException();
          }
        }
        
      } catch (InvalidFunctionException e) {
        return false;
      }

      return true;
    }


    //does the right side match the left side?
    //actually we should allow a type-UpCast here
    private void checkAssignment() {
       
      if(sourceTi == null || targetTi == null) {
        throw new InvalidFunctionException();
      }
      
      compatibleTypes(sourceTi, targetTi, false, false);
    }


    @Override
    public void allPartsOfFunctionFinished(FunctionExpression fe) {

      //other checks?

      validateFunctionTypesExtended(fe);
      TypeInfo ti = determineFunctionExpressionResultType(fe);
      
      //if there are VariableAccessPart in fe.getParts()
      //we have to follow them and set SoruceOrTargetTi!
      for(int i=0; i<fe.getParts().size(); i++) {
        List<VariableInfo> members = ti.getModelledType().getAllMemberVarsIncludingInherited();
        String name = fe.getParts().get(i).getName();
        Optional<VariableInfo> next = members.stream().filter(x -> x.getVarName().equals(name)).findFirst();
        if(next.isPresent()) {
          ti = next.get().getTypeInfo(true);
        }
      }
      

      setSourceOrTargetTi(ti);
    }
    
    
    @Override
    public void allPartsOfVariableFinished(Variable variable) {
      ensureVariableInitialized(variable);
      TypeInfo ti = determineTypeOfFollowAbleVariable(variable);
      checkVarAccess(variable);
      setSourceOrTargetTi(ti);
      
      currentVariable.pop();
      currentPartIndex.pop();
    }
    
    @Override
    public void variablePartEnds(VariableAccessPart part) {
      super.variablePartEnds(part);
      if (currentPartIndex.empty()) {
        currentPartIndex.push(0);
      }
      int idx = currentPartIndex.pop();
      currentPartIndex.push(idx + 1);
    }
    
    @Override
    public void expression2ArgsEnds(Expression2Args exp) {
      TypeInfo ti = operatorResultTypes.get(exp.getOperator().getClass()).getResultType();
      setSourceOrTargetTi(ti);
      
      validateExpression2ArgsArgumentTypes(exp);
    }
    
    private void validateExpression2ArgsArgumentTypes(Expression2Args exp) {
      TypeInfo acturalArgumentType1 = determineTypeOfExpression(exp.getVar1());
      TypeInfo acturalArgumentType2 = determineTypeOfExpression(exp.getVar2());
      Class<? extends Operator> operatorClass = exp.getOperator().getClass();
      OperatorTypes expectedTypes = operatorResultTypes.get(operatorClass);
      TypeInfo expectedArgumentType1 = expectedTypes.getExp1Type();
      TypeInfo expectedArgumentType2 = expectedTypes.getExp2Type();
      
      checkTypeConforms(expectedArgumentType1, acturalArgumentType1);
      checkTypeConforms(expectedArgumentType2, acturalArgumentType2);
      
      if(operatorClass == EqualsOperator.class || operatorClass == NotEqualsOperator.class) {
        //check that arguments could be equal
        compatibleTypes(acturalArgumentType1, acturalArgumentType2, false, false);
      }
    }
    
    @Override
    public void expression2ArgsStarts(Expression2Args expression) {
      if(nextIsIndexDef) {
        nextIsIndexDef = false;
        openIndexDefs.add(expression);
      }
    }
    
    @Override
    public void notStarts(Not not) {
      if(nextIsIndexDef) {
        nextIsIndexDef = false;
        openIndexDefs.add(not);
      }  
    }
    
    
    @Override
    public void variableStarts(Variable variable) {
      if(nextIsIndexDef) {
        nextIsIndexDef = false;
        openIndexDefs.add(new SingleVarExpression(variable));
      }  
    }
    
    
    @Override
    public void functionStarts(FunctionExpression fe) {
      if(nextIsIndexDef) {
        nextIsIndexDef = false;
        openIndexDefs.add(fe);
      }
    }
    
    
    @Override
    public void indexDefStarts() {
      nextIsIndexDef = true;
    }
    
    @Override
    public void indexDefEnds() {
      if(openIndexDefs.size() == 0) {
        throw new InvalidFunctionException();
      }
      
      Expression indexEx = openIndexDefs.remove(openIndexDefs.size() - 1);
      TypeInfo expressionResultType = determineTypeOfExpression(indexEx);
      boolean isString = expressionResultType.isBaseType() && BaseType.STRING.equals(expressionResultType.getBaseType());
      if (!expressionResultType.isAnyNumber() && !isString) {
        throw new InvalidFunctionException();
      }
    }
    
    private void checkTypeConforms(TypeInfo should, TypeInfo is) {
      if(TypeInfo.ANY.equals(should)) {
        return;
      }
      compatibleTypes(is, should, false, false);
      
    }

    @Override
    public void literalExpression(LiteralExpression expression) {
      if(nextIsIndexDef) {
        nextIsIndexDef = false;
        openIndexDefs.add(expression);
      }
      TypeInfo ti = determineTypeOfLiteralExpression(expression);
      setSourceOrTargetTi(ti);
    }


    private void ensureVariableInitialized(Variable variable) {
      if(variable.getBaseVariable() != null) {
        return;
      }
      
      try {
        vic.createVariableInfo(variable, false);
        variable.validate();
      } catch (XPRC_InvalidVariableIdException | XPRC_InvalidVariableMemberNameException e) {
        throw new InvalidFunctionException();
      }
    }
    
    
    private void setSourceOrTargetTi(TypeInfo info) {
      if (settingSourceExpression) {
        sourceTi = info;
      } else {
        targetTi = info;
      }
    }


    private void checkVarAccess(Variable variable) {
      boolean isList = ((StepBasedVariable)variable.getBaseVariable()).getAVariable().isList();
      if (isList && variable.getIndexDef() == null && variable.getParts() != null && variable.getParts().size() > 0 && !isQueryOutputVar(variable)) {
        throw new InvalidFunctionException();
      }
      
      //example: [1]Text["0"].text:=test is invalid
      if (!isList && variable.getIndexDef() != null && variable.getParts() != null) {
        throw new InvalidFunctionException();
      }
      
      for (int i = 0; i < variable.getParts().size() - 1; i++) {
        VariableAccessPart part = variable.getParts().get(i);
        boolean partIsList = false;
        try {
          partIsList = variable.follow(i).getTypeInfo(false).isList();
        } catch (XPRC_InvalidVariableMemberNameException e) {
          throw new InvalidFunctionException();
        }

        if (part.getIndexDef() == null && partIsList) {
          throw new InvalidFunctionException(); //List without Index definition
        }

        if (part.getIndexDef() != null && !partIsList) {
          throw new InvalidFunctionException(); //Single has Index definition
        }
      }
    }


    private boolean isQueryOutputVar(Variable variable) {
      if (variable.getVarNum() != 0) {
        return false;
      }
      
      boolean isQueryMapping = step instanceof StepMapping && ((StepMapping)step).isConditionMapping();
      
      return isQueryMapping;
    }


    private TypeInfo determineFunctionExpressionResultType(FunctionExpression fe) {
      
      if(fe instanceof DynamicResultTypExpression) {
        return ((DynamicResultTypExpression)fe).getResultType();
      }
      
      //special cases
      switch(fe.getFunction().getName()) {
        case "concatlists": 
        case Functions.APPEND_TO_LIST_FUNCTION_NAME:
          //type is most generic argument
          return determineMostGenericArgument(fe);
      }
      
      return fe.getFunction().getResultType();
    }

    
    private TypeInfo determineTypeOfFollowAbleVariable(Variable var) {
      TypeInfo ti;
      try {
        ti = var.getFollowedVariable().getTypeInfo(true);
        if (ti.isBaseType()) {
          return ti;
        } else if (ti.isModelledType()) {
          boolean isList = var.getFollowedVariable().getTypeInfo(false).isList();
          if(isList && var.lastPartOfVariableHasListAccess()) {
            isList = false;
          }
          return new TypeInfo(ti.getModelledType(), isList);
        }
        return ti;
      } catch (XPRC_InvalidVariableMemberNameException e) {
        throw new InvalidFunctionException();
      }
    }


    private TypeInfo determineTypeOfExpression(Expression ex) {
      if (ex instanceof SingleVarExpression) {
        return determineTypeOfFollowAbleVariable(((SingleVarExpression) ex).getVar());
      } else if (ex instanceof Expression2Args) {
        return operatorResultTypes.get(((Expression2Args)ex).getOperator().getClass()).getResultType();
      } else if (ex instanceof LiteralExpression) {
        return determineTypeOfLiteralExpression((LiteralExpression)ex);
      } else if(ex instanceof Not) {
        return new TypeInfo(BaseType.BOOLEAN_PRIMITIVE);
      } else if(ex instanceof FunctionExpression) {
        return determineFunctionExpressionResultType((FunctionExpression) ex);
      }

      return ex.getTargetType();
    }


    private TypeInfo determineTypeOfLiteralExpression(LiteralExpression ex) {
      String value = ex.getValue();
      try {
        Double.parseDouble(value);
        return new TypeInfo(BaseType.LONG_PRIMITIVE); //some Number
      }catch(NumberFormatException e) {
      }
      if(value.toLowerCase().equals("false") || value.toLowerCase().equals("true")) {
        return new TypeInfo(BaseType.BOOLEAN_PRIMITIVE); //some boolean
      }
      return new TypeInfo(BaseType.STRING);
    }

    
    @Override
    public void variableEnds(Variable variable) {
      super.variableEnds(variable);
      currentVariable.push(variable);
      
      currentPartIndex.push(0);
    }
    
    
    
    @Override
    public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi) {
      super.instanceFunctionStarts(vifi);
      Variable var = currentVariable.peek();
      List<TypeInfo> inputParameterTypes = new ArrayList<TypeInfo>();
      List<AVariable> inputVars = null;
      try {
        inputVars = getOperationInputs(step, var, vifi.getName());
      } catch (Exception e) {
        throw new InvalidFunctionException();
      }
      for(AVariable inputVar: inputVars) {
        TypeInfo inputVarTypeInfo = createTypeInfoForAVar(inputVar);
        inputParameterTypes.add(inputVarTypeInfo);
      }
      
      vifi.setInputParameterTypes(inputParameterTypes);
    }
    
    
    private TypeInfo createTypeInfoForAVar(AVariable avar) {
      if (avar.isJavaBaseType()) {
        BaseType baseType = BaseType.valueOfJavaName(avar.getJavaTypeEnum().getJavaTypeName());
        return new TypeInfo(baseType, avar.isList());
      } else {
        return new TypeInfo(new StepBasedType(avar.getDomOrExceptionObject(), (StepBasedIdentification) vic), avar.isList());
      }
    }
    
    private AVariable findMemberVar(List<AVariable> memberVars, String name) {
      for(AVariable var: memberVars) {
        if(var.getVarName().equals(name)) {
          return var;
        }
      }
      return null;
    }
    
    private DomOrExceptionGenerationBase traverseDoEs(DomOrExceptionGenerationBase doe, Variable var, int upToPartIndex) {
      for(int i=0; i<upToPartIndex; i++) {
        VariableAccessPart part = var.getParts().get(i); 
        if(part.isMemberVariableAccess()) {
          String memberVarName = part.getName();
          List<AVariable> members = doe.getAllMemberVarsIncludingInherited();
          AVariable memberVar = findMemberVar(members, memberVarName);
          doe = memberVar.getDomOrExceptionObject();
        } else if(part instanceof VariableInstanceFunctionIncovation) {
          VariableInstanceFunctionIncovation invocation = (VariableInstanceFunctionIncovation) part;
          DOM dom = (DOM)doe;
          Operation operation = null;
          try {
            operation = dom.getOperationByName(invocation.getName());
          } catch (XPRC_OperationUnknownException e) {
            throw new InvalidFunctionException();
          }
          doe = operation.getOutputVars().get(0).getDomOrExceptionObject();
        }
      }

      return doe;
    }
    
    private List<AVariable> getOperationInputs(Step step, Variable var, String operationName){
      AVariable variableInStep = null;
      List<AVariable> result = null;
      List<AVariable> inputVars = step.getInputVars();
      
      int varNum = var.getVarNum();
      if(varNum < inputVars.size()) {
        variableInStep = inputVars.get(varNum);
      } else {
        varNum -= inputVars.size();
        variableInStep = step.getOutputVars().get(varNum);
      }
      
      DomOrExceptionGenerationBase doe =  variableInStep.getDomOrExceptionObject();
      int index = currentPartIndex.peek();
      doe = traverseDoEs(doe, var, index);
      
      
      if(!(doe instanceof DOM)) {
        throw new InvalidFunctionException();
      }
      
      DOM dom = (DOM)doe;
      try {
        result = dom.getOperationByName(operationName).getInputVars();
      } catch (XPRC_OperationUnknownException e) {
        throw new InvalidFunctionException();
      }
      
      return result;
    }
    
    @Override
    public void instanceFunctionEnds(VariableInstanceFunctionIncovation vifi) {
      List<Expression> parameterExpressions = vifi.getFunctionParameter();
      List<TypeInfo> inputParas = vifi.getInputParameterTypes();
      
      if(inputParas == null) {
        inputParas = new ArrayList<TypeInfo>();
      }
      
      if(parameterExpressions.size() != inputParas.size()) {
        throw new InvalidFunctionException();
      }
      
      for(int i=0; i<parameterExpressions.size(); i++) {
        TypeInfo parameterType = determineTypeOfExpression(parameterExpressions.get(i));
        TypeInfo inputType = inputParas.get(i);
        compatibleTypes(parameterType, inputType, true, false); //TODO: allow type downcast as well? ( , , false, )
      }
    }

    private void validateFunctionTypesExtended(FunctionExpression fe) {
      Function function = fe.getFunction();
      
      //loosely type parameters
      //-> should they be a list
      List<Expression> subs = fe.getSubExpressions();
      for(int i=0; i<subs.size(); i++) {
        TypeInfo is = determineTypeOfExpression(subs.get(i));
        TypeInfo should = fe.getParameterTypeDef(i);
        compatibleTypes(is, should, false, false);
      }
      
      //too few/many arguments
      FunctionParameterTypeDefinition fptd = function.getParameterTypeDef();
      if(fptd.numberOfParas() != -1 && fptd.numberOfOptionalParas() != -1 && (subs.size() < fptd.numberOfParas() || subs.size() > fptd.numberOfParas() + fptd.numberOfOptionalParas())) {
        throw new InvalidFunctionException();
      }
      
      //"new" function only supports a String literal as argument.
      if(function.getName().equals(Functions.NEW_FUNCTION_NAME)) {
        String fqn = ((LiteralExpression)fe.getSubExpressions().get(0)).getValue();
        DomOrExceptionGenerationBase doe = loadDoE(fqn);
        if(doe.isAbstract()) { //can't instantiate abstract Datatype/Exception
          throw new InvalidFunctionException();
        }
        return;
      }
      
      if (!function.getName().equals("concatlists") && !function.getName().equals(Functions.APPEND_TO_LIST_FUNCTION_NAME)) { //name should be constant
        return;
      }

      if (fe.getSubExpressions().size() != 2) {
        throw new InvalidFunctionException();
      }

      TypeInfo ti1 = determineTypeOfExpression(fe.getSubExpressions().get(0));
      TypeInfo ti2 = determineTypeOfExpression(fe.getSubExpressions().get(1));
      compatibleTypes(ti1, ti2, false, true);
    }

    
    private DomOrExceptionGenerationBase loadDoE(String fqn) {
      DomOrExceptionGenerationBase result = null;
      
      long revision = step.getCreator().getRevision();
      
      try {
        FQName fqName = new FQName(revision, fqn);
        result = (DomOrExceptionGenerationBase) XMOMLoader.loadNewGB(fqName);
      } catch (Exception e) {
        //FQN might be invalid or a workflow 
        throw new InvalidFunctionException();
      }
      
      return result;
    }
    
    private TypeInfo determineMostGenericArgument(FunctionExpression fe) {
      
      List<Expression> subExpressions = fe.getSubExpressions();
      
      if(subExpressions.size() == 0) {
        return null;
      }
      
      TypeInfo mostGenericTypeInfo = determineTypeOfExpression(subExpressions.get(0));
      if(mostGenericTypeInfo.isBaseType()) {
        return mostGenericTypeInfo;
      }
      
      for(int i=1; i<subExpressions.size(); i++) {
        TypeInfo cmp = determineTypeOfExpression(subExpressions.get(i));
        mostGenericTypeInfo = findMoreGenericType(mostGenericTypeInfo, cmp);
      }
      
      
      return mostGenericTypeInfo;
    }

    private TypeInfo findMoreGenericType(TypeInfo ti1, TypeInfo ti2) {
      DomOrExceptionGenerationBase doe1 = ((StepBasedVariable.StepBasedType) ti1.getModelledType()).getGenerationType();
      DomOrExceptionGenerationBase doe2 = ((StepBasedVariable.StepBasedType) ti2.getModelledType()).getGenerationType();
      return DomOrExceptionGenerationBase.isSuperClass(doe1, doe2)? ti1 : ti2;
    }


    private void compatibleTypes(TypeInfo source, TypeInfo target, boolean strict, boolean ignoreList) {
      
      //allows null to be assigned to primitives - like flash did
      //also check target for null (-> might be checking Equals where null may appear on both sides)
      if(source.isNull() || target.isNull()) {
        return;
      }
      
      //has to be in front of list-Check, because target any means that both list and single values are allowed
      //at least when evaluation the length function.
      if(target.isAny()) {
        return;
      }
      
      if (!ignoreList && source.isList() != target.isList()) {
        throw new InvalidFunctionException();
      }
      
      if (target.isAnyNumber()) {
        if(!source.isAnyNumber() && !source.isBaseType() && !source.getBaseType().equals(BaseType.STRING)) {
          throw new InvalidFunctionException();
        } else {
          return;
        }
      }
      
      if(target.isBaseType() && target.getBaseType().equals(BaseType.LIST)) {
        if(source.isList()) {
          return;
        } else {
          throw new InvalidFunctionException();
        }
      }

      if (source.isBaseType() != target.isBaseType()) {
        throw new InvalidFunctionException();
      }

      if (source.isBaseType()) {
        BaseType bt1 = source.getBaseType();
        BaseType bt2 = target.getBaseType();
        if (bt2.equals(BaseType.STRING) || bt1.equals(BaseType.STRING)) {
          return; //any primitive type can be a string
        } else if (bt2.isNumber() && !bt1.isNumber()) {
          throw new InvalidFunctionException();
        } else if (bt2.isBoolean() && !bt1.isBoolean()) {
          throw new InvalidFunctionException();
        }
      } else {
        DomOrExceptionGenerationBase doe1 = ((StepBasedVariable.StepBasedType) source.getModelledType()).getGenerationType();
        DomOrExceptionGenerationBase doe2 = ((StepBasedVariable.StepBasedType) target.getModelledType()).getGenerationType();

        if (strict) {
          if(!DomOrExceptionGenerationBase.isSuperClass(doe1, doe2)) {
            throw new InvalidFunctionException();
          }
        } else {
          if (!DomOrExceptionGenerationBase.isSuperClass(doe1, doe2) && !DomOrExceptionGenerationBase.isSuperClass(doe2, doe1)) {
            throw new InvalidFunctionException();
          }
        }
      }
    }
  }
}
