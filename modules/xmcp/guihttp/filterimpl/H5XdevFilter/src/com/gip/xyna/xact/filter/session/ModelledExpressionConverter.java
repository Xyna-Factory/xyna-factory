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
package com.gip.xyna.xact.filter.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.formula.Expression;
import com.gip.xyna.xprc.xfractwfe.formula.Expression2Args;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression;
import com.gip.xyna.xprc.xfractwfe.formula.LiteralExpression;
import com.gip.xyna.xprc.xfractwfe.formula.Not;
import com.gip.xyna.xprc.xfractwfe.formula.Operator;
import com.gip.xyna.xprc.xfractwfe.formula.SingleVarExpression;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;
import com.gip.xyna.xprc.xfractwfe.formula.VariableInstanceFunctionIncovation;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.Visitor;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;

import xmcp.processmodeller.datatypes.expression.ExpressionVariable;
import xmcp.processmodeller.datatypes.expression.NotExpression;
import xmcp.processmodeller.datatypes.response.GetModelledExpressionsResponse;

public class ModelledExpressionConverter {

  private static final Logger logger = CentralFactoryLogging.getLogger(ModelledExpressionConverter.class);
  
  public GetModelledExpressionsResponse convert(GenerationBaseObject gbo, String stepId) {
    GetModelledExpressionsResponse result = new GetModelledExpressionsResponse();
    List<xmcp.processmodeller.datatypes.expression.ModelledExpression> expressions = new ArrayList<>();
    List<ModelledExpression> backendExpressions = null;
    Step step;
    try {
      step = gbo.getStep(ObjectId.parse(stepId).getBaseId());
    } catch (UnknownObjectIdException e) {
      throw new RuntimeException("unkown id");
    }
    
    if(step instanceof StepMapping) {
      backendExpressions = ((StepMapping)step).getParsedExpressions();
    } else if(step instanceof StepChoice) {
      backendExpressions = ((StepChoice)step).getParsedFormulas();
    } else if(step instanceof StepFunction) {
      //if it is a query, return expressions of mapping
      return result;
    }
    
    for(ModelledExpression backendExpression : backendExpressions) {
      xmcp.processmodeller.datatypes.expression.ModelledExpression expression = convert(backendExpression);
      expressions.add(expression);
    }
    
    result.unversionedSetModelledExpressions(expressions);
    return result;
  }

  public xmcp.processmodeller.datatypes.expression.ModelledExpression convert(ModelledExpression backendExpression) {
    logger.debug("convert: " + backendExpression.getExpression());
    xmcp.processmodeller.datatypes.expression.ModelledExpression result = new xmcp.processmodeller.datatypes.expression.ModelledExpression();
    ConverterVisitor visitor = new ConverterVisitor();
    backendExpression.visitSourceExpression(visitor);
    result.unversionedSetSourceExpression(visitor.getAndReset());
    backendExpression.visitTargetExpression(visitor);
    result.unversionedSetTargetExpression(visitor.getAndReset());
    return result;
  }
  
  
  private static interface ExpressionAssigner {
    public void assign(xmcp.processmodeller.datatypes.expression.Expression exp);
  }
  
  private static class RootExpressionAssigner implements ExpressionAssigner {
    
    private ConverterVisitor visitor;
    
    public RootExpressionAssigner(ConverterVisitor visitor) {
      this.visitor = visitor;
    }
    

    @Override
    public void assign(xmcp.processmodeller.datatypes.expression.Expression exp) {
      visitor.result = exp;
    }
    
  }
  
  private static class VariableExpressionAssigner implements ExpressionAssigner {

    private ExpressionVariable variable;
    
    public VariableExpressionAssigner(ExpressionVariable var) {
      variable = var;
    }
    
    @Override
    public void assign(xmcp.processmodeller.datatypes.expression.Expression exp) {
      variable.unversionedSetIndexDef(exp);
    }
  }
  
  private static class Expression2ArgsExpressionAssigner implements ExpressionAssigner {

    private xmcp.processmodeller.datatypes.expression.Expression2Args toAssign;
    private boolean assignedFirstAlready;
    
    public Expression2ArgsExpressionAssigner(xmcp.processmodeller.datatypes.expression.Expression2Args toAssign) {
      this.toAssign = toAssign;
      assignedFirstAlready = false;
    }
    
    @Override
    public void assign(xmcp.processmodeller.datatypes.expression.Expression exp) {
      if(!assignedFirstAlready) {
        toAssign.unversionedSetVar1(exp);
        assignedFirstAlready = true;
      } else {
        toAssign.unversionedSetVar2(exp);
      }
    }
    
  }
  
  private static class NotExpressionAssigner implements ExpressionAssigner {

    private NotExpression toAssign;
    
    public NotExpressionAssigner(NotExpression toAssign) {
      this.toAssign = toAssign;
    }
    
    @Override
    public void assign(xmcp.processmodeller.datatypes.expression.Expression exp) {
      toAssign.unversionedSetExpression(exp);
    }
    
  }
  
  private static class VariableInstanceFunctionInvocationExpressionAssigner implements ExpressionAssigner {

    xmcp.processmodeller.datatypes.expression.VariableInstanceFunctionIncovation invocation;
    boolean assigningParameters;
    
    
    public VariableInstanceFunctionInvocationExpressionAssigner(xmcp.processmodeller.datatypes.expression.VariableInstanceFunctionIncovation invocation) {
      this.invocation = invocation;
      this.assigningParameters = true;
    }
    
    @Override
    public void assign(xmcp.processmodeller.datatypes.expression.Expression exp) {
      if(assigningParameters) {
        List<? extends xmcp.processmodeller.datatypes.expression.Expression> oldParts = invocation.getFunctionParameter();
        List<xmcp.processmodeller.datatypes.expression.Expression> parts = oldParts == null ? new ArrayList<>() : new ArrayList<>(oldParts);
        parts.add(exp);
        invocation.unversionedSetFunctionParameter(parts);
      } else {
        invocation.setIndexDef(exp);
      }
    }
    
    public void endAssigningParameters() {
      assigningParameters = false;
    }
    
  }
  
  private static class VariableAccessPathExpressionAssigner implements ExpressionAssigner {

    private xmcp.processmodeller.datatypes.expression.VariableAccessPart vap;
    
    
    public VariableAccessPathExpressionAssigner(xmcp.processmodeller.datatypes.expression.VariableAccessPart vap) {
      this.vap = vap;
    }
    
    @Override
    public void assign(xmcp.processmodeller.datatypes.expression.Expression exp) {
      vap.unversionedSetIndexDef(exp);
    }
    
  }
  
  private static class FunctionExpressionAssigner implements ExpressionAssigner {
    //only assigns index definition.
    private xmcp.processmodeller.datatypes.expression.FunctionExpression functionExpression;
    
    
    public FunctionExpressionAssigner(xmcp.processmodeller.datatypes.expression.FunctionExpression functionExpression) {
      this.functionExpression = functionExpression;

    }
    
    @Override
    public void assign(xmcp.processmodeller.datatypes.expression.Expression exp) {
        functionExpression.setIndexDef(exp);
    }
  }
  
  private static class FunctionSubExpressionAssigner implements ExpressionAssigner {
    private xmcp.processmodeller.datatypes.expression.FunctionExpression functionExpression;
    
    public FunctionSubExpressionAssigner(xmcp.processmodeller.datatypes.expression.FunctionExpression functionExpression) {
      this.functionExpression = functionExpression;

    }
    
    @Override
    public void assign(xmcp.processmodeller.datatypes.expression.Expression exp) {
      List<? extends xmcp.processmodeller.datatypes.expression.Expression> oldParts = functionExpression.getSubExpressions();
      List<xmcp.processmodeller.datatypes.expression.Expression> parts = oldParts == null ? new ArrayList<>() : new ArrayList<>(oldParts);
      parts.add(exp);
      functionExpression.setSubExpressions(parts);
    }
    
  }
  
  private static class ConverterVisitor implements Visitor {
    
    private xmcp.processmodeller.datatypes.expression.Expression result;
    private Stack<Object> context;
    private Map<Object, GeneralXynaObject> objects;
    private static Map<Class<? extends GeneralXynaObject>, Function<GeneralXynaObject, ExpressionAssigner>> assignerCreatorMap = createAssignerCreatorMap();
    
    
    private static Map<Class<? extends GeneralXynaObject>, Function<GeneralXynaObject, ExpressionAssigner>> createAssignerCreatorMap() {
      Map<Class<? extends GeneralXynaObject>, Function<GeneralXynaObject, ExpressionAssigner>> map = new HashMap<>();
      map.put(ExpressionVariable.class,  (x) -> new VariableExpressionAssigner((ExpressionVariable)x));
      map.put(xmcp.processmodeller.datatypes.expression.Expression2Args.class, (x) -> new Expression2ArgsExpressionAssigner((xmcp.processmodeller.datatypes.expression.Expression2Args)x));
      map.put(NotExpression.class, (x) -> new NotExpressionAssigner((NotExpression)x));
      map.put(xmcp.processmodeller.datatypes.expression.VariableInstanceFunctionIncovation.class, (x) -> new VariableInstanceFunctionInvocationExpressionAssigner((xmcp.processmodeller.datatypes.expression.VariableInstanceFunctionIncovation)x));
      map.put(xmcp.processmodeller.datatypes.expression.VariableAccessPart.class, (x) -> new VariableAccessPathExpressionAssigner((xmcp.processmodeller.datatypes.expression.VariableAccessPart)x));
      map.put(xmcp.processmodeller.datatypes.expression.FunctionExpression.class, (x) -> new FunctionExpressionAssigner((xmcp.processmodeller.datatypes.expression.FunctionExpression)x));
      return map;
    }
    
    
    public ConverterVisitor() {
      reset();
    }
    
    private void reset() {
      result = null;
      context = new Stack<Object>();
      objects = new HashMap<Object, GeneralXynaObject>();
      context.push(new RootExpressionAssigner(this));
    }
    
    public xmcp.processmodeller.datatypes.expression.Expression getAndReset() {
      xmcp.processmodeller.datatypes.expression.Expression localResult = result;
      reset();
      return localResult;
    }

    @Override
    public void expression2ArgsStarts(Expression2Args expression) {
      logger.debug("expression2ArgsStarts");
      xmcp.processmodeller.datatypes.expression.Expression2Args exp = new xmcp.processmodeller.datatypes.expression.Expression2Args();
      assignExpression(exp);
      ExpressionAssigner assigner = assignerCreatorMap.get(exp.getClass()).apply(exp);
      context.push(assigner);
      objects.put(assigner, exp);
    }

    @Override
    public void functionEnds(FunctionExpression fe) {
      logger.debug("functionEnds");
    }

    @Override
    public void functionSubExpressionEnds(FunctionExpression fe, int parameterCnt) {
      logger.debug("functionSubExpressionEnds");
      context.pop();
    }

    @Override
    public void functionSubExpressionStarts(FunctionExpression fe, int parameterCnt) {
      logger.debug("functionSubExpressionStarts");
      Object obj = context.peek();
      GeneralXynaObject xo = objects.get(obj);
      FunctionSubExpressionAssigner cur = new FunctionSubExpressionAssigner((xmcp.processmodeller.datatypes.expression.FunctionExpression)xo);
      context.push(cur);      
    }

    @Override
    public void functionStarts(FunctionExpression fe) {
      logger.debug("functionStarts");
      
      xmcp.processmodeller.datatypes.expression.FunctionExpression exp = new xmcp.processmodeller.datatypes.expression.FunctionExpression();
      exp.unversionedSetFunction(fe.getFunction().getName());
      assignExpression(exp);
      ExpressionAssigner assigner = assignerCreatorMap.get(exp.getClass()).apply(exp);
      context.push(assigner);
      objects.put(assigner, exp);
    }

    @Override
    public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi) {
      logger.debug("instanceFunctionStarts");
    }

    @Override
    public void instanceFunctionEnds(VariableInstanceFunctionIncovation vifi) {
      logger.debug("instanceFunctionEnds");
      Object obj = context.peek();
      if(obj instanceof VariableInstanceFunctionInvocationExpressionAssigner) {
        ((VariableInstanceFunctionInvocationExpressionAssigner)obj).endAssigningParameters();
      } else {
        logger.error("Cound not stop assigning parameters. " + obj);
      }
    }

    @Override
    public void instanceFunctionSubExpressionEnds(Expression fe, int parameterCnt) {
      logger.debug("instanceFunctionSubExpressionEnds");
    }

    @Override
    public void instanceFunctionSubExpressionStarts(Expression fe, int parameterCnt) {
      logger.debug("instanceFunctionSubExpressionStarts");
    }

    @Override
    public void allPartsOfVariableFinished(Variable variable) {
      logger.debug("allPartsOfVariableFinished");
      context.pop();
    }

    @Override
    public void expression2ArgsEnds(Expression2Args expression) {
      logger.debug("expression2ArgsEnds");
      context.pop();
    }

    @Override
    public void literalExpression(LiteralExpression expression) {
      logger.debug("literalExpression - " + expression.getValue());
      xmcp.processmodeller.datatypes.expression.LiteralExpression exp = new xmcp.processmodeller.datatypes.expression.LiteralExpression();
      exp.unversionedSetValue(expression.getValue());
      assignExpression(exp);
    }




    @Override
    public void notStarts(Not not) {
      logger.debug("notStarts");
      xmcp.processmodeller.datatypes.expression.NotExpression exp = new xmcp.processmodeller.datatypes.expression.NotExpression();
      assignExpression(exp);
      ExpressionAssigner assigner = assignerCreatorMap.get(exp.getClass()).apply(exp);
      context.push(assigner);
      objects.put(assigner, exp);
    }

    @Override
    public void notEnds(Not not) {
      logger.debug("notEnds");
      context.pop();
    }

    @Override
    public void operator(Operator operator) {
      logger.debug("operator");
      //we are visiting an expression2Args
      Object obj = context.peek();
      GeneralXynaObject xo = objects.get(obj);
      ((xmcp.processmodeller.datatypes.expression.Expression2Args)xo).unversionedSetOperator(operator.getOperatorAsString());
      
    }

    @Override
    public void variableStarts(Variable variable) {
      logger.debug("variableStarts");
      Object obj = context.peek();
      GeneralXynaObject xo = objects.get(obj);
      context.push(variable);
      ExpressionVariable cur = new ExpressionVariable();
      cur.unversionedSetVarNum(variable.getVarNum());
      objects.put(variable, cur);
      
      if(xo instanceof xmcp.processmodeller.datatypes.expression.SingleVarExpression) {
        ((xmcp.processmodeller.datatypes.expression.SingleVarExpression)xo).setVariable(cur);
      } else {
        throw new RuntimeException("Unexpected variableStarts Context: " + obj + "/"+ xo + " - expected: SingleVarExpression");
      }
    }

    @Override
    public void variableEnds(Variable variable) {
      logger.debug("variableEnds");
    }

    @Override
    public void variablePartStarts(VariableAccessPart part) {
      logger.debug("variablePartStarts");
      Object currentContext = context.peek();
      GeneralXynaObject xo = objects.get(currentContext);

      xmcp.processmodeller.datatypes.expression.VariableAccessPart cur = null;
      Object toPush = null;
      if(part instanceof VariableInstanceFunctionIncovation) { 
        cur = new xmcp.processmodeller.datatypes.expression.VariableInstanceFunctionIncovation();
        toPush = assignerCreatorMap.get(cur.getClass()).apply(cur);
      } else {
        cur = new xmcp.processmodeller.datatypes.expression.VariableAccessPart();
        toPush = part;
      }
      context.push(toPush);
      objects.put(toPush, cur);
      cur.unversionedSetName(part.getName());
      if(xo instanceof ExpressionVariable) {
        List<? extends xmcp.processmodeller.datatypes.expression.VariableAccessPart> oldParts = ((ExpressionVariable)xo).getParts();
        List<xmcp.processmodeller.datatypes.expression.VariableAccessPart> parts = oldParts == null ? new ArrayList<>() : new ArrayList<>(oldParts);
        parts.add(cur);
        ((ExpressionVariable)xo).unversionedSetParts(parts);
      } else if(xo instanceof xmcp.processmodeller.datatypes.expression.FunctionExpression) {
        List<? extends xmcp.processmodeller.datatypes.expression.VariableAccessPart> oldParts = ((xmcp.processmodeller.datatypes.expression.FunctionExpression)xo).getParts();
        List<xmcp.processmodeller.datatypes.expression.VariableAccessPart> parts = oldParts == null ? new ArrayList<>() : new ArrayList<>(oldParts);
        parts.add(cur);
        ((xmcp.processmodeller.datatypes.expression.FunctionExpression)xo).unversionedSetParts(parts);
      } else {
        throw new RuntimeException("Unexpected vairablePart Context: " + currentContext + "/"+ xo + " - expected: ExpressionVariable");
      }
      
    }

    @Override
    public void variablePartEnds(VariableAccessPart part) {
      logger.debug("variablePartEnds");
      context.pop();
    }

    @Override
    public void variablePartSubContextEnds(VariableAccessPart p) {
      logger.debug("variablePartSubContextEnds");
    }

    @Override
    public void allPartsOfFunctionFinished(FunctionExpression fe) {
      logger.debug("allPartsOfFunctionFinished");
    }

    @Override
    public void indexDefStarts() {
      logger.debug("indexDefStarts");
      Object obj = context.peek();
      GeneralXynaObject xo = objects.get(obj);
      ExpressionAssigner assigner = assignerCreatorMap.get(xo.getClass()).apply(xo);
      context.push(assigner);
    }

    @Override
    public void indexDefEnds() {
      logger.debug("indexDefEnds");
      context.pop();
    }


    @Override
    public void singleVarExpressionStarts(SingleVarExpression expression) {
      logger.debug("singleVarExpressionStarts");
      xmcp.processmodeller.datatypes.expression.SingleVarExpression exp = new xmcp.processmodeller.datatypes.expression.SingleVarExpression();
      assignExpression(exp);
      context.push(expression);
      objects.put(expression, exp);
    }

    @Override
    public void singleVarExpressionEnds(SingleVarExpression expression) { 
      logger.debug("singleVarExpressionEnds");
      context.pop();
    }
    
    private void assignExpression(xmcp.processmodeller.datatypes.expression.Expression exp) {
      Object obj = context.peek();
      if(obj instanceof ExpressionAssigner) {
        ((ExpressionAssigner)obj).assign(exp);
      } else {
        logger.error("Could not assign expression. " + obj + " is not an ExpressionAssigner");
      }
    }
  }
}
