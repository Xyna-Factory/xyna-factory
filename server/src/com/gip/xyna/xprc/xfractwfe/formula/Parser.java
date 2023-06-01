/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xprc.xfractwfe.formula;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.xmom.PersistenceExpressionVisitors.QueryFunctionStore;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression.InvalidNumberOfFunctionParametersException;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.DuplicateAssignException;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification;



public class Parser {

  private static final Logger logger = CentralFactoryLogging.getLogger(Parser.class);
  
  private final String expression;
  private final ModelledExpression formula;
  private final SupportedFunctionStore functionStore;
  

  public Parser(ModelledExpression formula, SupportedFunctionStore supportedFunctions) {
    this.expression = formula.getExpression();
    this.formula = formula;
    if (supportedFunctions == null) {
      functionStore = new Functions();
    } else {
      functionStore = supportedFunctions;
    }
  }

  /**
   * @param gotOpenOperator gab es zuletzt beim parsen einen operator, der nun eine expression verlangt, die den operator beendet? z.b. vorher "expression =="
   *        falls nicht, kann nun alles mögliche kommen:
   *        - assignment
   *        - komplexe expression, etc 
   */
  public Expression parseExpression(int startIdx, boolean gotOpenOperator)
      throws XPRC_ParsingModelledExpressionException {
    char c = read(startIdx);
    while (c == ' ') {
      startIdx++;
      c = read(startIdx);
    }
    switch (c) {
      case '%' : {
        Variable var = parseVar(startIdx);
        if (var.getLastIdx() + 1 < expression.length()) {
          c = read(var.getLastIdx() + 1);
        } else {
          c = '0'; //egal, hauptsache nicht #
        }
        if (!gotOpenOperator 
            || c == '#') { //cast funktion muss noch um die variable gewrapped werden
          return parseSecondPartOfExpressionIfExisting(var.getLastIdx() + 1, new SingleVarExpression(var));
        } else {
          return new SingleVarExpression(var);
        }
      }
      case '(' : {
        Expression exp = parseExpression(startIdx + 1, false);
        int idx = exp.getLastIdx() + 1;
        if (idx >= expression.length()) {
          throw new XPRC_ParsingModelledExpressionException(expression, idx + 1,
                                                            new RuntimeException("missing closing brace."));
        }
        while (true) {
          c = read(idx);
          while (c == ' ') {
            idx++;
            if (idx >= expression.length()) {
              throw new XPRC_ParsingModelledExpressionException(expression, idx + 1,
                                                                new RuntimeException("missing closing brace."));
            }
            c = read(idx);
          }
          switch (c) {
            case ')' :
              exp.setLastIdx(idx);
              return exp;
            case ',' :
              throw new XPRC_ParsingModelledExpressionException(expression, idx + 1,
                                                                new RuntimeException("unexpected comma"));
            default :
              exp = parseSecondPartOfExpressionIfExisting(idx, exp);
              idx = exp.getLastIdx() + 1;
          }
        }
      }
      case '!' : {
        Expression innerExp = parseExpression(startIdx + 1, true);
        Expression exp = new Not(startIdx, innerExp);
        return exp;
      }
      case '"' : {
        LiteralExpression literal = parseLiteralExpression(startIdx);
        if (!gotOpenOperator) {
          return parseSecondPartOfExpressionIfExisting(literal.getLastIdx() + 1, literal);
        } else {
          return literal;
        }
      }
      default :
        List<Function> functions = new ArrayList<Function>(functionStore.getSupportedFunctions());
        Collections.sort(functions, new Comparator<Function>() {
          public int compare(Function f1, Function f2) {
            return f2.getName().length() - f1.getName().length();
          }
        });
        for (Function f : functions) {
          if (expression.substring(startIdx).startsWith(f.getName())) {
            Expression ret = parseFunction(f, startIdx);
            if (!gotOpenOperator) {
              return parseSecondPartOfExpressionIfExisting(ret.getLastIdx() + 1, ret);
            } else {
              return ret;
            }
          }
        }
        throw new XPRC_ParsingModelledExpressionException(expression, startIdx + 1);
    }
  }


  private char read(int idx) throws XPRC_ParsingModelledExpressionException {
    if (idx >= expression.length()) {
      throw new XPRC_ParsingModelledExpressionException(expression, idx + 1,
                                                        new RuntimeException("unexpected end of expression"));
    }
    return expression.charAt(idx);
  }


  private enum FunctionParsingState {
    /**
     * noch nichts geparst
     */
    NOTHING,
    /**
     * start der parameter
     */
    OPEN_BRACE,
    /**
     * es wird ein parameter erwartet. oder klammer zu
     */
    EXPECTING_NEW_PARAMETER,
    /**
     * d.h. es wurde eine expression als parameter geparst, es kann entweder noch der parameter weiter gehen
     * oder es folgt ein komma oder es folgt klammer zu.
     */
    POSSIBLY_PARAMETER_END;
  }

  private FunctionExpression parseFunction(Function f, final int startIdx) throws XPRC_ParsingModelledExpressionException {
    FunctionExpression function = parseFunctionInternally(f, startIdx);
    if (!(function.getLastIdx() + 1 >= expression.length())) {
      char c = read(function.getLastIdx() + 1);
      if (c == '.') {
        List<VariableAccessPart> accessParts = new ArrayList<VariableAccessPart>();
        int idx = function.getLastIdx() + 1;
        while (idx < expression.length() && read(idx) == '.') {
          VariableAccessPart part = parseVariableAccessPart(idx + 1);
          accessParts.add(part);
          idx = part.getLastIdx() + 1;
        }
        function.setAccessParts(accessParts);
        function.setLastIdx(idx - 1);
      }
    }
    if (ModelledExpression.isNew(function)) {
      try {
        return new FunctionExpression.NewExpression(function, formula.getVariableContextIdentification());
      } catch (InvalidNumberOfFunctionParametersException e) {
        throw new XPRC_ParsingModelledExpressionException(expression, startIdx + 1, e);
      }
    } else {
      return function;
    }
  }

  private FunctionExpression parseFunctionInternally(Function f, final int startIdx) throws XPRC_ParsingModelledExpressionException {
    int idx = startIdx + f.getName().length();
    FunctionParsingState state = FunctionParsingState.NOTHING;
    List<Expression> subExpressions = new ArrayList<Expression>();
    Expression subExpression = null;
    try {
      while (true) {
        if (idx >= expression.length()) {
          if (f.getParameterTypeDef().numberOfParas() == 0) {
            //klammern sind optional
            return new FunctionExpression(startIdx, idx - 1, f, subExpressions, null);
          }
          throw new XPRC_ParsingModelledExpressionException(expression, idx + 1, new RuntimeException(
                                                                                                  "missing parameters for function "
                                                                                                      + f.getName()));
        }
        char c = read(idx);
        while (c == ' ') {
          idx++;
          c = read(idx);
        }
        switch (c) {
          case ',' :
            if (state == FunctionParsingState.OPEN_BRACE || state == FunctionParsingState.POSSIBLY_PARAMETER_END) {
              if (subExpression != null) {
                subExpressions.add(subExpression);
                subExpression = null;
              } else {
                throw new XPRC_ParsingModelledExpressionException(expression, idx + 1,
                                                                  new RuntimeException(
                                                                                       "missing parameters for function "
                                                                                           + f.getName()));
              }
              state = FunctionParsingState.EXPECTING_NEW_PARAMETER;
            } else if (f.getParameterTypeDef().numberOfParas() == 0) {
              //klammern sind optional. das komma gehört zu einer übergeordneten funktion oder sowas
              return new FunctionExpression(startIdx, idx - 1, f, subExpressions, null);
            } else {
              throw new XPRC_ParsingModelledExpressionException(expression, idx + 1,
                                                                new RuntimeException("missing parameter for function "
                                                                    + f.getName()));
            }
            break;
          case ')' :
            if (state == FunctionParsingState.OPEN_BRACE || state == FunctionParsingState.POSSIBLY_PARAMETER_END) {
              if (subExpression != null) {
                subExpressions.add(subExpression);
              }
              if (f.getResultType().isList() && (idx+1 < expression.length() && read(idx+1) == '[')) {
                  Expression indexDefExpr = parseExpression(idx + 2, false);
                  idx = indexDefExpr.getLastIdx() + 1;
                  c = read(idx);
                  while (c == ' ') {
                    idx++;
                    c = read(idx);
                  }
                  while (c != ']') {
                    indexDefExpr = parseSecondPartOfExpressionIfExisting(idx, indexDefExpr);
                    idx = indexDefExpr.getLastIdx() + 1;
                    c = read(idx);
                    while (c == ' ') {
                      idx++;
                      c = read(idx);
                    }
                  }
                  return new FunctionExpression(startIdx, idx, f, subExpressions, indexDefExpr);
              } else {
                return new FunctionExpression(startIdx, idx, f, subExpressions, null);
              }
            } else if (f.getParameterTypeDef().numberOfParas() == 0) {
              //klammern sind optional. die klammer gehört zu einer übergeordneten funktion oder sowas
              return new FunctionExpression(startIdx, idx - 1, f, subExpressions, null);
            } else {
              throw new XPRC_ParsingModelledExpressionException(expression, idx + 1);
            }
          case ']' :
            return new FunctionExpression(startIdx, idx - 1, f, subExpressions, null);
          case '(' :
            if (state == FunctionParsingState.NOTHING) {
              state = FunctionParsingState.OPEN_BRACE;
              break;
            }
            //fallthrough
          default :
            if (state == FunctionParsingState.OPEN_BRACE || state == FunctionParsingState.EXPECTING_NEW_PARAMETER) {
              subExpression = parseExpression(idx, false);
              idx = subExpression.getLastIdx();
              state = FunctionParsingState.POSSIBLY_PARAMETER_END;
            } else if (state == FunctionParsingState.POSSIBLY_PARAMETER_END) {
              subExpression = parseSecondPartOfExpressionIfExisting(idx, subExpression);
              idx = subExpression.getLastIdx();
            } else {
              if (f.getParameterTypeDef().numberOfParas() == 0) {
                //klammern sind optional
                return new FunctionExpression(startIdx, idx - 1, f, subExpressions, null);
              }
              throw new XPRC_ParsingModelledExpressionException(expression, idx + 1,
                                                                new RuntimeException("missing parameters for function "
                                                                    + f.getName()));
            }
        }
        idx++;
      }
    } catch (InvalidNumberOfFunctionParametersException e) {
      throw new XPRC_ParsingModelledExpressionException(expression, startIdx + 1, e);
    }
  }


  private LiteralExpression parseLiteralExpression(final int startIdx) throws XPRC_ParsingModelledExpressionException {
    //erster idx => "
    int idx = startIdx + 1;
    StringBuilder value = new StringBuilder();
    StringBuilder valueForJavaCodeGen = new StringBuilder();
    while (true) {
      char c = read(idx);
      if (c == '\\') { // ein backslash gefunden
        idx++;
        if (idx >= expression.length()) {
          throw new XPRC_ParsingModelledExpressionException(expression, idx + 1,
                                                            new RuntimeException("unexpected end of literal"));
        }
        c = read(idx);
        char d;
        String forJava;
        switch (c) {
          case 't' :
            d = '\t';
            forJava = "\\t";
            break;
          case 'b' :
            d = '\b';
            forJava = "\\b"; //2 Zeichen - Die durch die Kompilation in ein Zeichen resultieren
            break;
          case 'n' :
            d = '\n';
            forJava = "\\n"; // -> in string: "asd\nasd" value <asd
                             //                                    asd
            break;
          case 'r' :
            d = '\r';
            forJava = "\\r";
            break;
          case 'f' :
            d = '\f';
            forJava = "\\f";
            break;
          case '"' :
            d = '"';
            forJava = "\\\""; // -> in string: "asd\"asd" -> value <asd"asd>
            break;
          case '\\' :
            d = c;
            forJava = "\\\\"; // in string: "asd\\asd" -> value <asd\asd>
            break;
          case 'u' :
            // "\ u <zahl>" erstmal nicht korrekt unterstützt
            // fall through
          default :
            if (logger.isDebugEnabled()) {
              logger.debug("found escaped character <" + c + "> in XFL expression '" + expression + "' at position " + idx
                  + ". this has no special meaning and will be ignored.");
            }
            d = c;
            forJava = String.valueOf(c); // escape von anderen zeichen enthält im value kein backslash, sondern nur c. -> in string: "asd'c'asd" -> value <asd'c'asd>
        }
        value.append(d);
        valueForJavaCodeGen.append(forJava);
      } else {
        if (c == '"') {
          break;
        }
        value.append(c);
        valueForJavaCodeGen.append(c);
      }
      idx++;
      if (idx >= expression.length()) {
        throw new XPRC_ParsingModelledExpressionException(expression, idx + 1,
                                                          new RuntimeException("unexpected end of literal"));
      }
    }
    return new LiteralExpression(value.toString(), valueForJavaCodeGen.toString(), startIdx, idx);
  }


  public Expression parseSecondPartOfExpressionIfExisting(final int startIdx, Expression firstPart)
      throws XPRC_ParsingModelledExpressionException {
    boolean firstPartPlusSecondPartMustBeBoolean = false; //TODO das könnte man wahrscheinlich immer ganz gut checken - z.b. wissen funktionen, ob sie vom typ boolean sind
    int idx = startIdx;
    /*
     * entweder 
     * 1. ende des ausdrucks, falls variable boolean ist oder
     * 2. vergleichs-operator und zweite variable oder
     * 3. assign und dann neuer ausdruck     
     * 
     */
    if (idx >= expression.length()) {
      firstPart.setLastIdx(idx);
      if (firstPartPlusSecondPartMustBeBoolean) {
        checkBoolean(idx, firstPart);
      }
      return firstPart;
    }
    char c = read(idx);
    while (c == ' ') {
      idx++;
      if (idx >= expression.length()) {
        firstPart.setLastIdx(idx - 1);
        if (firstPartPlusSecondPartMustBeBoolean) {
          checkBoolean(idx, firstPart);
        }
        return firstPart;
      }
      c = read(idx);
    }

    switch (c) {
      case ')' :
      case ',' :
      case ']' :
        return firstPart;
      case '|' :
      case '&' :
      case '<' :
      case '>' :
      case '+' :
      case '-' :
      case '*' :
      case '/' :
      case '!' : {//!=
        Operator op = parseOperator(idx);
        //nun kann entweder eine 2te variable kommen, oder eine weitere expression (z.b. a && (b && c)

        Expression var2 =
            parseExpression(op.getLastIdx() + 1, !(op instanceof AndOperator || op instanceof OrOperator));
        return new Expression2Args(firstPart.getFirstIdx(), firstPart, var2, op);
      }
      case '#':
        if (read(idx+1) == 'c' &&
            read(idx+2) == 'a' &&
            read(idx+3) == 's' &&
            read(idx+4) == 't' &&
            read(idx+5) == '(') {
          Function castFunction = getCastFunction();
          FunctionExpression parsedCast = parseFunction(castFunction, idx+1);
          List<Expression> allParams = new ArrayList<Expression>();
          String value = ((LiteralExpression)parsedCast.getSubExpressions().get(0)).getValue();
          allParams.add(new LiteralExpression(value, value, parsedCast.getSubExpressions().get(0).getFirstIdx(), parsedCast.getSubExpressions().get(0).getLastIdx())); 
          allParams.add(firstPart);
          try {
            FunctionExpression fe;
            if (functionStore instanceof QueryFunctionStore) {
              fe = new FunctionExpression.CastExpression(parsedCast, allParams, 1, 0, formula.getVariableContextIdentification(), Boolean.TRUE, true);
            } else {
              fe = new FunctionExpression.CastExpression(parsedCast, allParams, 1, 0, formula.getVariableContextIdentification());
            }
            // TODO reset firstIdx to firstPart.getFirstIdx() ?
            return parseSecondPartOfExpressionIfExisting(fe.getLastIdx() + 1, fe);
          } catch (InvalidNumberOfFunctionParametersException e) {
            throw new XPRC_ParsingModelledExpressionException(expression, startIdx + 1, e);
          }
        }
      case '=' :
        if (read(idx + 1) == '=') {
          Operator op = parseOperator(idx);
          //nun kann entweder eine 2te variable kommen, oder eine weitere expression (z.b. a && (b && c)

          Expression var2 =
              parseExpression(op.getLastIdx() + 1, !(op instanceof AndOperator || op instanceof OrOperator));
          return new Expression2Args(firstPart.getFirstIdx(), firstPart, var2, op);
        }
        try {
          formula.setFoundAssign(Assign.DEEP_CLONE);
        } catch (DuplicateAssignException e) {
          throw new XPRC_ParsingModelledExpressionException(expression, idx + 1, e);
        }
        firstPart.setLastIdx(idx);
        return firstPart;
      case '~' :
        if (read(idx + 1) == '=') {
          try {
            formula.setFoundAssign(Assign.SHALLOW_CLONE);
          } catch (DuplicateAssignException e) {
            throw new XPRC_ParsingModelledExpressionException(expression, idx + 1, e);
          }
          firstPart.setLastIdx(idx + 1);
          return firstPart;
        } else {
          throw new XPRC_ParsingModelledExpressionException(expression, idx + 1);
        }
      default :
        throw new XPRC_ParsingModelledExpressionException(expression, idx + 1, new RuntimeException(
                                                                                                "expected second part of expression after first part ended at position "
                                                                                                    + firstPart.getLastIdx()));
    }
  }
  
  private void checkBoolean(int idx, Expression firstPart) throws XPRC_ParsingModelledExpressionException {
    try {
      if (firstPart.getOriginalType().isBaseType() && firstPart.getOriginalType().getBaseType().isBoolean()) {
        //ok
      } else {
        throw new XPRC_ParsingModelledExpressionException(expression, idx + 1,
                                                          new RuntimeException(
                                                                               "Expected expression of boolean type, got expression of type "
                                                                                   + firstPart.getOriginalType()
                                                                                       .getJavaName() + "."));
      }
    } catch (XPRC_InvalidVariableMemberNameException e) {
      throw new XPRC_ParsingModelledExpressionException(expression, idx + 1, e);
    }
  }


  private Operator parseOperator(int idx) throws XPRC_ParsingModelledExpressionException {
    char c = read(idx);
    char cnext = read(idx + 1);
    switch (c) {
      case '|' :
        if (cnext == '|') {
          return new OrOperator(idx + 1);
        } else {
          throw new XPRC_ParsingModelledExpressionException(expression, idx + 1);
        }
      case '&' :
        if (cnext == '&') {
          return new AndOperator(idx + 1);
        } else {
          throw new XPRC_ParsingModelledExpressionException(expression, idx + 1);
        }
      case '<' :
        switch (cnext) {
          case '=' :
            return new LteOperator(idx + 1);
          default :
            return new LtOperator(idx);
        }
      case '>' :
        switch (cnext) {
          case '=' :
            return new GteOperator(idx + 1);
          default :
            return new GtOperator(idx);
        }
      case '=' :
        if (cnext == '=') {
          return new EqualsOperator(idx + 1);
        } else {
          throw new XPRC_ParsingModelledExpressionException(expression, idx + 1);
        }
      case '!' :
        if (cnext == '=') {
          return new NotEqualsOperator(idx + 1);
        } else {
          throw new XPRC_ParsingModelledExpressionException(expression, idx + 1);
        }
      case '+' :
        return new PlusOperator(idx);
      case '-' :
        return new MinusOperator(idx);
      case '/' :
        return new DivideOperator(idx);
      case '*' :
        return new MultiplyOperator(idx);
      default :
        throw new XPRC_ParsingModelledExpressionException(expression, idx + 1);
    }
  }


  private Variable parseVar(final int startIdx) throws XPRC_ParsingModelledExpressionException {
    VariableContextIdentification variableContext = formula.getVariableContextIdentification();
    List<VariableAccessPart> parts = new ArrayList<VariableAccessPart>();
    //%0%.ports[5] oder %0%.ports# oder sowas
    int idx = startIdx + 1;
    while (read(idx) != '%') {
      idx++;
    }
    //c[idx] = %

    String varNum = expression.substring(startIdx + 1, idx);
    idx++;
    if (idx >= expression.length()) {
      return new Variable(variableContext, varNum, parts, startIdx, idx, null);
    }
    char c = read(idx);
    while (c == ' ') {
      idx++;
      if (idx >= expression.length()) {
        return new Variable(variableContext, varNum, parts, startIdx, idx, null);
      }
      c = read(idx);
    }

    //c[idx] != ' '

    Expression indexDefExpr = null;
    while (true) {
      if (idx >= expression.length()) {
        return new Variable(variableContext, varNum, parts, startIdx, idx - 1, indexDefExpr);
      }
      c = read(idx);
      switch (c) {
        case '[' :
          if (']' == read(idx+1)) { // skip empty brackets because the gui likes sending them so much
            idx++;
          } else {
            indexDefExpr = parseExpression(idx + 1, false);
            idx = indexDefExpr.getLastIdx() + 1;
          }
          c = read(idx);
          while (c == ' ') {
            idx++;
            c = read(idx);
          }
          if (c != ']') {
            //invalid index definition. Expected [ValueExpr]
            throw new XPRC_ParsingModelledExpressionException(expression, startIdx + 1);
          }
          idx++;
          break;
        case '.' :
          VariableAccessPart part = parseVariableAccessPart(idx + 1);
          parts.add(part);
          idx = part.getLastIdx() + 1;
          break;
        case ' ' :
          idx++;
          break;
        default :
          return new Variable(variableContext, varNum, parts, startIdx, idx - 1, indexDefExpr);
      }
    }
  }


  private VariableAccessPart parseVariableAccessPart(int idx) throws XPRC_ParsingModelledExpressionException {
    int startIdx = idx;
    Expression indexDefExpr = null;
    int startOfIndexDefExpr = -1;
    while (true) {
      if (idx >= expression.length()) {
        if (startOfIndexDefExpr == -1) {
          startOfIndexDefExpr = idx;
        }
        return new VariableAccessPart(expression.substring(startIdx, startOfIndexDefExpr), startIdx, idx - 1, indexDefExpr);
      }
      char c = read(idx);
      switch (c) {
        case '.' :
        case ']' :
        case ' ' :
        case '=' :
        case '<' :
        case '>' :
        case '+' :
        case '-' :
        case '*' :
        case '/' :
        case '!' :
        case '&' :
        case '|' :
        case ',' :
        case ')' :
        case '~' :
        case '#' :
          //ende von variable part
          if (startOfIndexDefExpr == -1) {
            startOfIndexDefExpr = idx;
          }
          int lastIdx = idx -1;
          if (idx + 1 >= expression.length() && c == '*' && startIdx == startOfIndexDefExpr) {
            // looks like SelektionMask with trailing star
            lastIdx = idx;
          }
          return new VariableAccessPart(expression.substring(startIdx, startOfIndexDefExpr), startIdx, lastIdx, indexDefExpr);
        case '[' :
          startOfIndexDefExpr = idx;
          indexDefExpr = parseExpression(idx + 1, false);
          idx = indexDefExpr.getLastIdx() + 1;
          c = read(idx);
          while (c == ' ') {
            idx++;
            c = read(idx);
          }
          while (c != ']') {
            indexDefExpr = parseSecondPartOfExpressionIfExisting(idx, indexDefExpr);
            idx = indexDefExpr.getLastIdx() + 1;
            c = read(idx);
            while (c == ' ') {
              idx++;
              c = read(idx);
            }
          }
          break;
        case '(' :
          startOfIndexDefExpr = idx;
          List<Expression> subExpressions = new ArrayList<Expression>();
          Expression subExpression;
          idx++;
          c = read(idx);
          while (c != ')') {
            if (c != ',') {
              subExpression = parseExpression(idx, false);
              idx = subExpression.getLastIdx();
              subExpressions.add(subExpression);
            }
            idx++;
            c = read(idx);
          }
          if (idx + 1 >= expression.length()) {
            return new VariableInstanceFunctionIncovation(expression.substring(startIdx, startOfIndexDefExpr), startIdx, idx, null, subExpressions);
          } else {
            c = read(idx+1);
            switch (c) {
              case '[' :
                indexDefExpr = parseExpression(idx + 2, false);
                idx = indexDefExpr.getLastIdx() + 1;
                c = read(idx);
                while (c == ' ') {
                  idx++;
                  c = read(idx);
                }
                while (c != ']') {
                  indexDefExpr = parseSecondPartOfExpressionIfExisting(idx, indexDefExpr);
                  idx = indexDefExpr.getLastIdx() + 1;
                  c = read(idx);
                  while (c == ' ') {
                    idx++;
                    c = read(idx);
                  }
                }
                return new VariableInstanceFunctionIncovation(expression.substring(startIdx, startOfIndexDefExpr), startIdx, idx, indexDefExpr, subExpressions);
              default :
                return new VariableInstanceFunctionIncovation(expression.substring(startIdx, startOfIndexDefExpr), startIdx, idx, null, subExpressions);
            }
          }
        default :
          //teil vom variablen-part namen
          break;
      }
      idx++;
    }
  }
  
  //caching
  private Function castFunction;
  private boolean castFunctionNotFound = false;

  private Function getCastFunction() {
    if (castFunction != null) {
      return castFunction;
    }
    if (!castFunctionNotFound) {
      List<Function> functions = new ArrayList<Function>(functionStore.getSupportedFunctions());
      for (Function function : functions) {
        if (function.getName().equals(Functions.CAST_FUNCTION_NAME)) {
          castFunction = function;
          return function;
        }
      }
      castFunctionNotFound = true;
    }
    throw new UnsupportedOperationException("Function " + Functions.CAST_FUNCTION_NAME + " is not supported!");
  }
  
}
