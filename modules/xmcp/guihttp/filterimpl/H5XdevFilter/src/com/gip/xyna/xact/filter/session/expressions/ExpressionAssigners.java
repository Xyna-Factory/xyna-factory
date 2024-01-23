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
package com.gip.xyna.xact.filter.session.expressions;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

import xmcp.processmodeller.datatypes.expression.Expression;
import xmcp.processmodeller.datatypes.expression.Expression2Args;
import xmcp.processmodeller.datatypes.expression.ExpressionVariable;
import xmcp.processmodeller.datatypes.expression.FunctionExpression;
import xmcp.processmodeller.datatypes.expression.NotExpression;
import xmcp.processmodeller.datatypes.expression.VariableAccessPart;
import xmcp.processmodeller.datatypes.expression.VariableInstanceFunctionIncovation;



public class ExpressionAssigners {

  public interface ExpressionAssigner {

    public void assign(Expression exp);
  }

  
  public static ExpressionAssigner createAssigner(GeneralXynaObject assignTo) {
    return assignerCreatorMap.get(assignTo.getClass()).apply(assignTo);
  }

  private final static Map<Class<? extends GeneralXynaObject>, Function<GeneralXynaObject, ExpressionAssigner>> assignerCreatorMap =
      createAssignerCreatorMap();


  private static Map<Class<? extends GeneralXynaObject>, Function<GeneralXynaObject, ExpressionAssigner>> createAssignerCreatorMap() {
    Map<Class<? extends GeneralXynaObject>, Function<GeneralXynaObject, ExpressionAssigner>> map = new HashMap<>();
    map.put(NotExpression.class, (x) -> new NotExpressionAssigner((NotExpression) x));
    map.put(Expression2Args.class, (x) -> new Expression2ArgsExpressionAssigner((Expression2Args) x));
    map.put(FunctionExpression.class, (x) -> new FunctionExpressionAssigner((FunctionExpression) x));
    map.put(ExpressionVariable.class, (x) -> new VariableExpressionAssigner((ExpressionVariable) x));
    map.put(VariableAccessPart.class, (x) -> new VariableAccessPathExpressionAssigner((VariableAccessPart) x));
    map.put(VariableInstanceFunctionIncovation.class, (x) -> new VariableAccessPathExpressionAssigner((VariableAccessPart) x));
    return Collections.unmodifiableMap(map);
  }


  public static class ConsumerExpressionAssigner implements ExpressionAssigner {

    private Consumer<Expression> consumer;


    public ConsumerExpressionAssigner(Consumer<Expression> consumer) {
      this.consumer = consumer;
    }


    @Override
    public void assign(Expression exp) {
      consumer.accept(exp);
    }

  }

  public static class VariableExpressionAssigner extends ConsumerExpressionAssigner {

    public VariableExpressionAssigner(ExpressionVariable var) {
      super(x -> var.unversionedSetIndexDef(x));
    }
  }

  public static class Expression2ArgsExpressionAssigner implements ExpressionAssigner {

    private Expression2Args toAssign;
    private boolean assignedFirstAlready;


    public Expression2ArgsExpressionAssigner(Expression2Args toAssign) {
      this.toAssign = toAssign;
      assignedFirstAlready = false;
    }


    @Override
    public void assign(Expression exp) {
      if (!assignedFirstAlready) {
        toAssign.unversionedSetVar1(exp);
        assignedFirstAlready = true;
      } else {
        toAssign.unversionedSetVar2(exp);
      }
    }

  }

  public static class NotExpressionAssigner extends ConsumerExpressionAssigner {

    public NotExpressionAssigner(NotExpression toAssign) {
      super(x -> toAssign.unversionedSetExpression(x));
    }

  }

  public static class VariableAccessPathExpressionAssigner extends ConsumerExpressionAssigner {


    public VariableAccessPathExpressionAssigner(VariableAccessPart vap) {
      super(x -> vap.unversionedSetIndexDef(x));
    }

  }

  public static class FunctionExpressionAssigner extends ConsumerExpressionAssigner {

    public FunctionExpressionAssigner(FunctionExpression functionExpression) {
      super(x -> functionExpression.setIndexDef(x));

    }

  }


  public static class ExpressionListAssigner implements ExpressionAssigner {

    private Supplier<List<? extends Expression>> getter;
    private Consumer<List<Expression>> setter;


    public ExpressionListAssigner(Supplier<List<? extends Expression>> getter, Consumer<List<Expression>> setter) {
      this.getter = getter;
      this.setter = setter;
    }


    @Override
    public void assign(Expression exp) {
      List<? extends Expression> oldParts = getter.get();
      List<Expression> parts = oldParts == null ? new ArrayList<>() : new ArrayList<>(oldParts);
      parts.add(exp);
      setter.accept(parts);
    }

  }


  public static class InstanceFunctionSubExpressionAssigner extends ExpressionListAssigner {

    public InstanceFunctionSubExpressionAssigner(VariableInstanceFunctionIncovation invocation) {
      super(() -> invocation.getFunctionParameter(), (exp) -> invocation.unversionedSetFunctionParameter(exp));
    }

  }

  public static class FunctionSubExpressionAssigner extends ExpressionListAssigner {

    public FunctionSubExpressionAssigner(FunctionExpression funExp) {
      super(() -> funExp.getSubExpressions(), (exp) -> funExp.unversionedSetSubExpressions(exp));
    }

  }
}
