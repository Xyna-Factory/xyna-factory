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

import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.expressions.ExpressionAssigners.ExpressionAssigner;
import com.gip.xyna.xact.filter.session.expressions.ExpressionAssigners;
import com.gip.xyna.xact.filter.session.expressions.ExpressionAssigners.FunctionSubExpressionAssigner;
import com.gip.xyna.xact.filter.session.expressions.ExpressionAssigners.ConsumerExpressionAssigner;
import com.gip.xyna.xact.filter.session.expressions.ExpressionAssigners.InstanceFunctionSubExpressionAssigner;
import com.gip.xyna.xact.filter.session.expressions.VarAccessPartAssigners;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.formula.Expression;
import com.gip.xyna.xprc.xfractwfe.formula.Expression2Args;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression;
import com.gip.xyna.xprc.xfractwfe.formula.Functions;
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

import xmcp.processmodeller.datatypes.expression.CastExpression;
import xmcp.processmodeller.datatypes.expression.ExpressionVariable;
import xmcp.processmodeller.datatypes.response.GetModelledExpressionsResponse;



public class ModelledExpressionConverter {


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

    if (step instanceof StepMapping) {
      backendExpressions = ((StepMapping) step).getParsedExpressions();
    } else if (step instanceof StepChoice) {
      backendExpressions = ((StepChoice) step).getParsedFormulas();
    } else if (step instanceof StepFunction) {
      backendExpressions = QueryUtils.findQueryHelperMapping((StepFunction)step).getParsedExpressions();
    }

    for (ModelledExpression backendExpression : backendExpressions) {
      if(backendExpression == null) {
        continue;
      }
      xmcp.processmodeller.datatypes.expression.ModelledExpression expression = convert(backendExpression);
      expressions.add(expression);
    }

    result.unversionedSetModelledExpressions(expressions);
    return result;
  }


  public xmcp.processmodeller.datatypes.expression.ModelledExpression convert(ModelledExpression backendExpression) {
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
      xmcp.processmodeller.datatypes.expression.Expression2Args exp = new xmcp.processmodeller.datatypes.expression.Expression2Args();
      assignExpression(exp);
      ExpressionAssigner assigner = ExpressionAssigners.createAssigner(exp);
      context.push(assigner);
      objects.put(assigner, exp);
    }


    @Override
    public void functionEnds(FunctionExpression fe) {
    }


    @Override
    public void functionSubExpressionEnds(FunctionExpression fe, int parameterCnt) {
      context.pop();
    }


    @Override
    public void functionSubExpressionStarts(FunctionExpression fe, int parameterCnt) {
      Object obj = context.peek();
      GeneralXynaObject xo = objects.get(obj);
      FunctionSubExpressionAssigner cur = new FunctionSubExpressionAssigner((xmcp.processmodeller.datatypes.expression.FunctionExpression) xo);
      context.push(cur);
    }


    @Override
    public void functionStarts(FunctionExpression fe) {
      xmcp.processmodeller.datatypes.expression.FunctionExpression exp = Functions.CAST_FUNCTION_NAME.equals(fe.getFunction().getName()) ? 
            new CastExpression(null, null, null, null): 
            new xmcp.processmodeller.datatypes.expression.FunctionExpression(null, null, null, null);
      exp.unversionedSetFunction(fe.getFunction().getName());
      assignExpression(exp);
      ExpressionAssigner assigner = ExpressionAssigners.createAssigner(exp);
      context.push(assigner);
      objects.put(assigner, exp);
    }


    @Override
    public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi) {
      Object obj = context.peek();
      GeneralXynaObject xo = objects.get(obj);
      InstanceFunctionSubExpressionAssigner cur = new InstanceFunctionSubExpressionAssigner((xmcp.processmodeller.datatypes.expression.VariableInstanceFunctionIncovation) xo);
      context.push(cur);
    }


    @Override
    public void instanceFunctionEnds(VariableInstanceFunctionIncovation vifi) {
      context.pop();
    }


    @Override
    public void instanceFunctionSubExpressionEnds(Expression fe, int parameterCnt) {
    }


    @Override
    public void instanceFunctionSubExpressionStarts(Expression fe, int parameterCnt) {
    }


    @Override
    public void allPartsOfVariableFinished(Variable variable) {
      context.pop();
    }


    @Override
    public void expression2ArgsEnds(Expression2Args expression) {
      context.pop();
    }


    @Override
    public void literalExpression(LiteralExpression expression) {
      xmcp.processmodeller.datatypes.expression.LiteralExpression exp = new xmcp.processmodeller.datatypes.expression.LiteralExpression();
      exp.unversionedSetValue(expression.getValue());
      assignExpression(exp);
    }


    @Override
    public void notStarts(Not not) {
      xmcp.processmodeller.datatypes.expression.NotExpression exp = new xmcp.processmodeller.datatypes.expression.NotExpression();
      assignExpression(exp);
      ExpressionAssigner assigner = ExpressionAssigners.createAssigner(exp);
      context.push(assigner);
      objects.put(assigner, exp);
    }


    @Override
    public void notEnds(Not not) {
      context.pop();
    }


    @Override
    public void operator(Operator operator) {
      Object obj = context.peek();
      GeneralXynaObject xo = objects.get(obj);
      ((xmcp.processmodeller.datatypes.expression.Expression2Args) xo).unversionedSetOperator(operator.getOperatorAsString());
    }


    @Override
    public void variableStarts(Variable variable) {
      Object obj = context.peek();
      GeneralXynaObject xo = objects.get(obj);
      context.push(variable);
      ExpressionVariable cur = new ExpressionVariable(variable.getVarNum(), null, null);
      objects.put(variable, cur);
      ((xmcp.processmodeller.datatypes.expression.SingleVarExpression) xo).setVariable(cur);
    }


    @Override
    public void variableEnds(Variable variable) {
    }


    @Override
    public void variablePartStarts(VariableAccessPart part) {
      Object currentContext = context.peek();
      GeneralXynaObject xo = objects.get(currentContext);
      xmcp.processmodeller.datatypes.expression.VariableAccessPart cur = null;
      Object toPush = null;
      if (part instanceof VariableInstanceFunctionIncovation) {
        cur = new xmcp.processmodeller.datatypes.expression.VariableInstanceFunctionIncovation(null, null, null);
        toPush = ExpressionAssigners.createAssigner(cur);
      } else {
        cur = new xmcp.processmodeller.datatypes.expression.VariableAccessPart();
        toPush = part;
      }

      cur.unversionedSetName(part.getName());
      VarAccessPartAssigners.assign(xo, cur);
      context.push(toPush);
      objects.put(toPush, cur);
    }


    @Override
    public void variablePartEnds(VariableAccessPart part) {
      context.pop();
    }


    @Override
    public void variablePartSubContextEnds(VariableAccessPart p) {
    }


    @Override
    public void allPartsOfFunctionFinished(FunctionExpression fe) {
    }


    @Override
    public void indexDefStarts() {
      Object obj = context.peek();
      GeneralXynaObject xo = objects.get(obj);
      ExpressionAssigner assigner = ExpressionAssigners.createAssigner(xo);
      context.push(assigner);
    }


    @Override
    public void indexDefEnds() {
      context.pop();
    }


    @Override
    public void singleVarExpressionStarts(SingleVarExpression expression) {
      xmcp.processmodeller.datatypes.expression.SingleVarExpression exp = new xmcp.processmodeller.datatypes.expression.SingleVarExpression();
      assignExpression(exp);
      context.push(expression);
      objects.put(expression, exp);
    }


    @Override
    public void singleVarExpressionEnds(SingleVarExpression expression) {
      context.pop();
    }


    private void assignExpression(xmcp.processmodeller.datatypes.expression.Expression exp) {
      Object obj = context.peek();
      ((ExpressionAssigner) obj).assign(exp);
    }
  }
}
