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
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.expressions.ExpressionAssigners.ExpressionAssigner;
import com.gip.xyna.xact.filter.session.expressions.ExpressionAssigners;
import com.gip.xyna.xact.filter.session.expressions.ExpressionAssigners.FunctionSubExpressionAssigner;
import com.gip.xyna.xact.filter.session.expressions.ExpressionAssigners.ConsumerExpressionAssigner;
import com.gip.xyna.xact.filter.session.expressions.ExpressionAssigners.InstanceFunctionSubExpressionAssigner;
import com.gip.xyna.xact.filter.session.expressions.ExpressionAssigners.VariableAccessPathExpressionAssigner;
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
  
  

  
 
  
  private static class ConverterVisitor implements Visitor {
    
    private xmcp.processmodeller.datatypes.expression.Expression result;
    private Stack<Object> context;
    private Map<Object, GeneralXynaObject> objects;
    
    
    public ConverterVisitor() {
      reset();
    }
    
    private void reset() {
      result = null;
      context = new Stack<Object>();
      objects = new HashMap<Object, GeneralXynaObject>();
      context.push(new ConsumerExpressionAssigner(x -> this.result = x));
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
      ExpressionAssigner assigner = ExpressionAssigners.assignerCreatorMap.get(exp.getClass()).apply(exp);
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
      ExpressionAssigner assigner = ExpressionAssigners.assignerCreatorMap.get(exp.getClass()).apply(exp);
      context.push(assigner);
      objects.put(assigner, exp);
    }

    @Override
    public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi) {
      logger.debug("instanceFunctionStarts");
      
      Object obj = context.peek();
      GeneralXynaObject xo = objects.get(obj);
      InstanceFunctionSubExpressionAssigner cur = new InstanceFunctionSubExpressionAssigner((xmcp.processmodeller.datatypes.expression.VariableInstanceFunctionIncovation)xo);
      context.push(cur); 
    }

    @Override
    public void instanceFunctionEnds(VariableInstanceFunctionIncovation vifi) {
      logger.debug("instanceFunctionEnds");
      context.pop();
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
      ExpressionAssigner assigner = ExpressionAssigners.assignerCreatorMap.get(exp.getClass()).apply(exp);
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
      Object obj = context.peek();
      GeneralXynaObject xo = objects.get(obj);
      if (xo instanceof xmcp.processmodeller.datatypes.expression.Expression2Args) {
        ((xmcp.processmodeller.datatypes.expression.Expression2Args) xo).unversionedSetOperator(operator.getOperatorAsString());
      }
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
        toPush = new VariableAccessPathExpressionAssigner(cur);
      } else {
        cur = new xmcp.processmodeller.datatypes.expression.VariableAccessPart();
        toPush = part;
      }
      
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
      }
      context.push(toPush);
      objects.put(toPush, cur);
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
      ExpressionAssigner assigner = ExpressionAssigners.assignerCreatorMap.get(xo.getClass()).apply(xo);
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
      }
    }
  }
}
