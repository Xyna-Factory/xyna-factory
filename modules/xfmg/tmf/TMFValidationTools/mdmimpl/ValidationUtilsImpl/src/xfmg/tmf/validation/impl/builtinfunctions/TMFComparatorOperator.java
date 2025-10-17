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

public class TMFComparatorOperator implements TMFInfixFunction {

  private static enum Comparator {

    GREATER(">", OperatorPrecedence.RELATIONS.precedence) {

      @Override
      boolean eval(Number n1, Number n2) {
        if (n1 instanceof BigDecimal || n2 instanceof BigDecimal) {
          return ConversionUtils.asBigDecimal(n1).compareTo(ConversionUtils.asBigDecimal(n2)) > 0;
        }
        return n1.doubleValue() > n2.doubleValue();
      }
    },
    LESSER("<", OperatorPrecedence.RELATIONS.precedence) {

      @Override
      boolean eval(Number n1, Number n2) {
        if (n1 instanceof BigDecimal || n2 instanceof BigDecimal) {
          return ConversionUtils.asBigDecimal(n1).compareTo(ConversionUtils.asBigDecimal(n2)) < 0;
        }
        return n1.doubleValue() < n2.doubleValue();
      }
    },
    GREATER_OR_EQUAL(">=", OperatorPrecedence.RELATIONS.precedence) {

      @Override
      boolean eval(Number n1, Number n2) {
        if (n1 instanceof BigDecimal || n2 instanceof BigDecimal) {
          return ConversionUtils.asBigDecimal(n1).compareTo(ConversionUtils.asBigDecimal(n2)) >= 0;
        }
        return n1.doubleValue() >= n2.doubleValue();
      }
    },
    LESSER_OR_EQUAL("<=", OperatorPrecedence.RELATIONS.precedence) {

      @Override
      boolean eval(Number n1, Number n2) {
        if (n1 instanceof BigDecimal || n2 instanceof BigDecimal) {
          return ConversionUtils.asBigDecimal(n1).compareTo(ConversionUtils.asBigDecimal(n2)) <= 0;
        }
        return n1.doubleValue() <= n2.doubleValue();
      }
    },
    EQUAL("==", OperatorPrecedence.EQUAL.precedence) {

      @Override
      boolean eval(Number n1, Number n2) {
        if (n1 instanceof BigDecimal || n2 instanceof BigDecimal) {
          return ConversionUtils.asBigDecimal(n1).compareTo(ConversionUtils.asBigDecimal(n2)) == 0;
        }
        return Double.compare(n1.doubleValue(), n2.doubleValue()) == 0;
      }
    },
    NOT_EQUAL("!=", OperatorPrecedence.EQUAL.precedence) {

      @Override
      boolean eval(Number n1, Number n2) {
        return !EQUAL.eval(n1, n2);
      }
    }, MATCH_LEFT("~=", OperatorPrecedence.EQUAL.precedence) {

      @Override
      boolean eval(Number n1, Number n2) {
        throw new RuntimeException();
      }
    },MATCH_RIGHT("=~", OperatorPrecedence.EQUAL.precedence) {

      @Override
      boolean eval(Number n1, Number n2) {
        throw new RuntimeException();
      }
    };


    final String opString;
    final int opPrecedence;


    Comparator(String opString, int opPrecedence) {
      this.opString = opString;
      this.opPrecedence = opPrecedence;
    }


    abstract boolean eval(Number n1, Number n2);

  }


  private final Comparator op;


  private TMFComparatorOperator(Comparator op) {
    super();
    this.op = op;
  }


  public static TMFComparatorOperator greater() {
    return new TMFComparatorOperator(Comparator.GREATER);
  }


  public static TMFComparatorOperator lesser() {
    return new TMFComparatorOperator(Comparator.LESSER);
  }


  public static TMFComparatorOperator greaterEqual() {
    return new TMFComparatorOperator(Comparator.GREATER_OR_EQUAL);
  }


  public static TMFComparatorOperator lesserEqual() {
    return new TMFComparatorOperator(Comparator.LESSER_OR_EQUAL);
  }


  public static TMFComparatorOperator equal() {
    return new TMFComparatorOperator(Comparator.EQUAL);
  }


  public static TMFComparatorOperator notEqual() {
    return new TMFComparatorOperator(Comparator.NOT_EQUAL);
  }
  
  public static TMFComparatorOperator matchLeft() {
    return new TMFComparatorOperator(Comparator.MATCH_LEFT);
  }
  
  public static TMFComparatorOperator matchRight() {
    return new TMFComparatorOperator(Comparator.MATCH_RIGHT);
  }


  @Override
  public Object eval(TMFExpressionContext context, Object[] args) {
    switch (op) {
      case MATCH_LEFT :
        return match(args[0], args[1]);
      case MATCH_RIGHT :
        return match(args[1], args[0]);
      case EQUAL :
      case NOT_EQUAL :
        return compareStrings(args);
      default :
        return compareNumbers(args);
    }
  }
  
  private Boolean match(Object regex, Object value) {
    String r = ConversionUtils.getString(regex);
    if (r == null) {
      throw new RuntimeException("Regular expression is null");
    }
    String v = ConversionUtils.ifNull(ConversionUtils.getString(value), "");
    return TMFExpressionContext.regexCache.getOrCreate(r).matcher(v).matches();
  }
  

  private Object compareNumbers(Object[] args) {
    Number n1 = ConversionUtils.ifNull(ConversionUtils.getNumber(args[0]), 0);
    Number n2 = ConversionUtils.ifNull(ConversionUtils.getNumber(args[1]), 0);
    return op.eval(n1, n2);
  }


  private Object compareStrings(Object[] args) {
    try {
      return compareNumbers(args);
    } catch (NumberFormatException e) {
      String s1 = ConversionUtils.ifNull(ConversionUtils.getString(args[0]), "");
      String s2 = ConversionUtils.getString(args[1]);
      switch (op) {
        case EQUAL :
          return s1.equals(s2);
        case NOT_EQUAL :
          return !s1.equals(s2);
      }
    }
    throw new RuntimeException("not reached");
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
