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
package com.gip.xyna.xnwh.persistence.memory.sqlparsing;



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionTokenizer.ConditionToken;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionTokenizer.TokenType;



public class ConditionParser {


  public static class Comparison extends Expression {

    public final ConditionToken token;
    public final ConditionToken operator;
    public final ConditionToken argument;


    private Comparison(ConditionToken token, ConditionToken operator, ConditionToken arg) {
      this.token = token;
      this.operator = operator;
      this.argument = arg;
    }
  }

  public static class IsNull extends Expression {

    public final ConditionToken arg;


    private IsNull(ConditionToken arg) {
      this.arg = arg;
    }

  }

  public static class IsNotNull extends Expression {

    public final ConditionToken arg;


    private IsNotNull(ConditionToken arg) {
      this.arg = arg;
    }

  }

  public static class Not extends Expression {

    public final Expression subExpression;


    private Not(Expression subExpression) {
      this.subExpression = subExpression;
    }

  }

  public static class AndOrExpressions extends Expression {

    public final List<Expression> subExpressions;
    public final List<Operator2> operators;


    private AndOrExpressions(List<Expression> subExpressions, List<Operator2> operators) {
      this.subExpressions = subExpressions;
      this.operators = operators;
    }

  }

  public static class InList extends Expression {

    public final List<ConditionToken> elements;
    public final ConditionToken arg;

    public InList(ConditionToken arg, List<ConditionToken> elements) {
      this.arg = arg;
      this.elements = elements;
    }

  }

  public static class Expression {

    public int lastTokenIdx;

  }

  public static class Operator2 {

    public final ConditionToken token;

    public final int lastTokenIdx;


    public Operator2(ConditionToken token, int tokenIdx) {
      this.token = token;
      this.lastTokenIdx = tokenIdx;
    }

  }


  public Expression parse(List<ConditionToken> tokens) throws SQLParsingException {
    int idx = 0;
    Expression e = expectExpressions(tokens, idx);
    if (e.lastTokenIdx != tokens.size() - 1) {
      throw new SQLParsingException("Missing Operator at position " + (tokens.get(e.lastTokenIdx).endIndex + 1));
    }
    return e;
  }


  private Operator2 expectOperator(List<ConditionToken> tokens, int idx) throws SQLParsingException {
    ConditionToken token = tokens.get(idx);
    switch (token.type) {
      case AND :
      case OR :
        return new Operator2(token, idx);
      default :
        return null;
    }
  }


  //expr ( operator expr )* 
  private Expression expectExpressions(List<ConditionToken> tokens, int idx) throws SQLParsingException {
    int len = tokens.size();
    Expression expression = expectSingleExpression(tokens, idx);
    idx = expression.lastTokenIdx + 1;
    List<Expression> exprs = new ArrayList<>();
    exprs.add(expression);
    List<Operator2> ops = new ArrayList<>();
    while (idx < len) {
      Operator2 op = expectOperator(tokens, idx);
      if (op == null) {
        break;
      }
      ops.add(op);
      Expression expr2 = expectSingleExpression(tokens, op.lastTokenIdx + 1);
      exprs.add(expr2);
      idx = expr2.lastTokenIdx + 1;
    }
    if (exprs.size() == 1) {
      return expression;
    }
    Expression e = new AndOrExpressions(exprs, ops);
    e.lastTokenIdx = idx - 1;
    return e;
  }


  public static class SQLParsingException extends Exception {

    public SQLParsingException(String msg) {
      super(msg);
    }

  }


  /*
   * möglichkeiten:
   * - ( exprs )
   * - a compare b
   * - NOT expr 
   */
  private Expression expectSingleExpression(List<ConditionToken> tokens, int idx) throws SQLParsingException {
    int len = tokens.size();
    ConditionToken token = tokens.get(idx);
    if (len - idx < 3) {
      throw new SQLParsingException("SQL incomplete after position " + token.endIndex);
    }
    switch (token.type) {
      case BRACE_OPEN :
        Expression expr = expectExpressions(tokens, idx + 1);
        if (expr.lastTokenIdx >= len - 1) {
          throw new SQLParsingException("Missing closing brace belonging to opening brace at position " + token.startIndex);
        }
        ConditionToken closingBrace = tokens.get(expr.lastTokenIdx + 1);
        if (closingBrace.type != TokenType.BRACE_CLOSE) {
          throw new SQLParsingException("Missing closing brace at position " + closingBrace.startIndex);
        }
        expr.lastTokenIdx = expr.lastTokenIdx + 1;
        return expr;
      case WORD :
        ConditionToken op = tokens.get(idx + 1);
        switch (op.type) {
          case EQ :
          case NEQ :
          case LT :
          case LTE :
          case GT :
          case GTE :
          case LIKE :
            ConditionToken arg = tokens.get(idx + 2);
            switch (arg.type) {
              case NULL : //!= null support?
              case WORD :
              case ESCAPED :
              case PARAMETER :
                break;
              default :
                throw new SQLParsingException("Invalid argument found at position " + arg.startIndex);
            }
            Expression e = new Comparison(token, op, arg);
            e.lastTokenIdx = idx + 2;
            return e;
          case IS :
            ConditionToken next = tokens.get(idx + 2);
            switch (next.type) {
              case NULL :
                e = new IsNull(token);
                e.lastTokenIdx = idx + 2;
                return e;
              case NOT :
                if (len - idx < 4) {
                  throw new SQLParsingException("SQL incomplete after position " + next.endIndex);
                }
                next = tokens.get(idx + 3);
                if (next.type != TokenType.NULL) {
                  throw new SQLParsingException("Expecting NULL at position " + next.startIndex);
                }
                e = new IsNotNull(token);
                e.lastTokenIdx = idx + 3;
                return e;
              default :
                throw new SQLParsingException("Invalid sql at position " + next.startIndex);
            }
          case IN :
            return expectInList(token, tokens, idx + 2);
          default :
            throw new SQLParsingException("Missing operator at position " + op.startIndex);
        }
      case NOT :
        Expression e = expectSingleExpression(tokens, idx + 1);
        Expression ne = new Not(e);
        ne.lastTokenIdx = e.lastTokenIdx;
        return ne;
      default :
        throw new SQLParsingException("Unexpected token " + token.type + " at position " + token.startIndex);
    }
  }


  private Expression expectInList(ConditionToken arg, List<ConditionToken> tokens, int idx) throws SQLParsingException {
    ConditionToken t = tokens.get(idx);
    if (t.type != TokenType.BRACE_OPEN) {
      throw new SQLParsingException("Missing opening brace after IN at position " + t.startIndex);
    }
    int len = tokens.size();
    idx++;
    if (idx >= len) {
      throw new SQLParsingException("Incomplete IN statement");
    }
    List<ConditionToken> list = new ArrayList<>();
    loop : while (idx < len) {
      t = tokens.get(idx);
      switch (t.type) {
        case PARAMETER :
        case ESCAPED :
        case NULL :
        case WORD : 
          list.add(t);
          break;
        case BRACE_CLOSE :
          break loop;
        default :
          throw new SQLParsingException("Unexpected IN parameter of type " + t.type.name() + " at position " + t.startIndex);
      }
      idx++;
      if (idx >= len) {
        throw new SQLParsingException("Incomplete IN statement");
      }
      t = tokens.get(idx);
      switch (t.type) {
        case COMMA :
          //ok
          break;
        case BRACE_CLOSE :
          break loop;
        default :
          throw new SQLParsingException("Missing comma in IN statement at position " + t.startIndex);
      }
      idx++;
    }
    InList il = new InList(arg, list);
    il.lastTokenIdx = idx;
    return il;
  }


}
