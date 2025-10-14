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

import java.math.BigDecimal;

import xfmg.tmf.validation.impl.ConversionUtils;
import xfmg.tmf.validation.impl.OperatorPrecedence;
import xfmg.tmf.validation.impl.SyntaxTreeNode;
import xfmg.tmf.validation.impl.TMFExpressionContext;
import xfmg.tmf.validation.impl.functioninterfaces.TMFInfixFunction;

public class TMFMathOperator implements TMFInfixFunction {

    private static enum Operator {

      PLUS("+", OperatorPrecedence.MATHPLUS.precedence) {

        @Override
        Number eval(Number n1, Number n2) {
          if (n1 instanceof BigDecimal || n2 instanceof BigDecimal) {
            return ConversionUtils.asBigDecimal(n1).add(ConversionUtils.asBigDecimal(n2));
          } 
          if (n1 instanceof Double || n2 instanceof Double) {
            return n1.doubleValue() + n2.doubleValue();
          }
          return n1.longValue() + n2.longValue();
        }
      },
      MINUS("-", OperatorPrecedence.MATHPLUS.precedence) {

        @Override
        Number eval(Number n1, Number n2) {
          if (n1 instanceof BigDecimal || n2 instanceof BigDecimal) {
            return ConversionUtils.asBigDecimal(n1).subtract(ConversionUtils.asBigDecimal(n2));
          } 
          if (n1 instanceof Double || n2 instanceof Double) {
            return n1.doubleValue() - n2.doubleValue();
          }
          return n1.longValue() - n2.longValue();
        }
      },
      MULTIPLY("*", OperatorPrecedence.MATHMULTIPLY.precedence) {

        @Override
        Number eval(Number n1, Number n2) {
          if (n1 instanceof BigDecimal || n2 instanceof BigDecimal) {
            return ConversionUtils.asBigDecimal(n1).multiply(ConversionUtils.asBigDecimal(n2));
          } 
          if (n1 instanceof Double || n2 instanceof Double) {
            return n1.doubleValue() * n2.doubleValue();
          }
          return n1.longValue() * n2.longValue();
        }
      },
      DIVIDE("/", OperatorPrecedence.MATHMULTIPLY.precedence) {

        @Override
        Number eval(Number n1, Number n2) {
          if (n1 instanceof BigDecimal || n2 instanceof BigDecimal) {
            return ConversionUtils.asBigDecimal(n1).divide(ConversionUtils.asBigDecimal(n2));
          } 
          if (n1 instanceof Double || n2 instanceof Double || (n1.longValue() % n2.longValue() != 0)) {
            return n1.doubleValue() / n2.doubleValue();
          }
          return n1.longValue() / n2.longValue();
        }
      };


      final String opString;
      final int opPrecedence;


      Operator(String opString, int opPrecedence) {
        this.opString = opString;
        this.opPrecedence = opPrecedence;
      }


      abstract Number eval(Number n1, Number n2);

    }


    private final Operator op;


    private TMFMathOperator(Operator op) {
      super();
      this.op = op;
    }


    public static TMFMathOperator plus() {
      return new TMFMathOperator(Operator.PLUS);
    }


    public static TMFMathOperator minus() {
      return new TMFMathOperator(Operator.MINUS);
    }


    public static TMFMathOperator multiply() {
      return new TMFMathOperator(Operator.MULTIPLY);
    }


    public static TMFMathOperator divide() {
      return new TMFMathOperator(Operator.DIVIDE);
    }


    @Override
    public Object eval(TMFExpressionContext context, Object[] args) {
      Number n1 = ConversionUtils.ifNull(ConversionUtils.getNumber(args[0]), 0);
      Number n2 = ConversionUtils.ifNull(ConversionUtils.getNumber(args[1]), 0);
      return op.eval(n1, n2);
    }


    @Override
    public String getName() {
      return op.opString;
    }


    @Override
    public int operatorPrecedence() {
      return op.opPrecedence;
    }


    @Override
    public void validate(SyntaxTreeNode parent, SyntaxTreeNode[] args) {
      if (args.length != 2) {
        throw new RuntimeException("need 2 args");
      }
    }
}
