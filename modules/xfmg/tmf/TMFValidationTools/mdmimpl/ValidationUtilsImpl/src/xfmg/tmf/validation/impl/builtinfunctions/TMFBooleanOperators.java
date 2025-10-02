/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xfmg.tmf.validation.impl.builtinfunctions;

import xfmg.tmf.validation.impl.ConversionUtils;
import xfmg.tmf.validation.impl.OperatorPrecedence;
import xfmg.tmf.validation.impl.SyntaxTreeNode;
import xfmg.tmf.validation.impl.TMFExpressionContext;
import xfmg.tmf.validation.impl.functioninterfaces.TMFInfixFunction;

public class TMFBooleanOperators implements TMFInfixFunction {

  private enum Operators {

    AND("AND", OperatorPrecedence.AND.precedence), AND2("&&", OperatorPrecedence.AND.precedence), OR("OR",
        OperatorPrecedence.OR.precedence), OR2("||", OperatorPrecedence.OR.precedence);


    String name;
    int precedence;


    Operators(String name, int precedence) {
      this.name = name;
      this.precedence = precedence;
    }

  }


  private final Operators op;


  public TMFBooleanOperators(Operators op) {
    this.op = op;
  }
  
  @Override
  public Object evalLazy(TMFExpressionContext context, SyntaxTreeNode[] args) {
    boolean a1 = ConversionUtils.ifNull(ConversionUtils.getBoolean(args[0].eval(context)), false);
    switch (op) {
      case AND :
      case AND2 :
        return a1 && ConversionUtils.ifNull(ConversionUtils.getBoolean(args[1].eval(context)), false);
      case OR :
      case OR2 :
        return a1 || ConversionUtils.ifNull(ConversionUtils.getBoolean(args[1].eval(context)), false);
    }
    throw new RuntimeException("unexpected");
  }
  
  @Override
  public Object eval(TMFExpressionContext context, Object[] args) {
    throw new RuntimeException("unexpected");
  }

  @Override
  public String getName() {
    return op.name;
  }


  @Override
  public void validate(SyntaxTreeNode parent, SyntaxTreeNode[] args) {
    if (args.length != 2) {
      throw new RuntimeException(op.name + " needs 2 args");
    }
  }


  @Override
  public int operatorPrecedence() {
    return op.precedence;
  }


  public static TMFBooleanOperators and() {
    return new TMFBooleanOperators(Operators.AND);
  }


  public static TMFBooleanOperators and2() {
    return new TMFBooleanOperators(Operators.AND2);
  }


  public static TMFBooleanOperators or() {
    return new TMFBooleanOperators(Operators.OR);
  }


  public static TMFBooleanOperators or2() {
    return new TMFBooleanOperators(Operators.OR2);
  }
}
