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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.memory.ColumnDeclaration;
import com.gip.xyna.xnwh.persistence.memory.PreparedQueryForMemory.EscapeForMemory;
import com.gip.xyna.xnwh.persistence.memory.TableInfo;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionParser.AndOrExpressions;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionParser.Comparison;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionParser.Expression;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionParser.InList;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionParser.IsNotNull;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionParser.IsNull;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionParser.Not;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionParser.SQLParsingException;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionTokenizer.ConditionToken;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionTokenizer.SQLTokenizerException;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ConditionTokenizer.TokenType;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;

/**
 * parst whereclause und sub-bedingungen, erstellt ein objektbaum, und kann toJava(), um daraus einen javaausdruck zu
 * bauen
 */
public class Condition {
  
  public static ArrayList<String> primitiveTypes = new ArrayList<String>(Arrays.asList(new String[] {"long", "int",
      "short", "byte", "double", "float", "boolean", "char"}));

  public static ArrayList<String> objectsOfPrimitiveTypes = new ArrayList<String>(Arrays.asList(new String[] {"Long",
      "Integer", "Short", "Byte", "Double", "Float", "Boolean", "Character"}));

  private static final Logger logger = CentralFactoryLogging.getLogger(Condition.class);


  private static final Pattern PATTERN_ANY_NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

  private static final String CONDITION_BOOL_PREFIX = "conditionBoolean";

  //member vars:
    
  private ArrayList<Condition> conditions = new ArrayList<Condition>();
  private ArrayList<Operator> operators = new ArrayList<Operator>();
  private boolean isEmpty = false;
  private String expr1;
  private String expr2;
  private ConditionOperator conditionOperator;
  private boolean isNegated = false;


  public static Condition create(String sqlString) throws PreparedQueryParsingException {
    ConditionTokenizer ct = new ConditionTokenizer();
    try {
      List<ConditionToken> tokens = ct.tokenize(sqlString);
      ConditionParser parser = new ConditionParser();
      Expression e = parser.parse(tokens);
      return convertExpressionIntoCondition2(sqlString, e);
    } catch (SQLTokenizerException e) {
      throw new PreparedQueryParsingException(e.getMessage(), e);
    } catch (SQLParsingException e) {
      throw new PreparedQueryParsingException(e.getMessage(), e);
    }
  }
  

  private static Condition convertExpressionIntoCondition2(String sqlString, Expression e) {
    if (e instanceof Comparison) {
      Comparison c = (Comparison) e;
      String op = c.operator.getAsString(sqlString);
      if (c.operator.type == TokenType.NEQ) {
        op = ConditionOperator.EQUALS.getSql();
      }
      String arg = ((Comparison) e).argument.getAsString(sqlString);
      if (((Comparison) e).argument.type == TokenType.ESCAPED) {
        arg = "'" + arg + "'";
      }
      Condition con = new Condition(((Comparison) e).token.getAsString(sqlString), op, arg);
      if (c.operator.type == TokenType.NEQ) {
        con.isNegated = true;
      }
      return con;
    } else if (e instanceof IsNull) {
      return new Condition(((IsNull) e).arg.getAsString(sqlString), ConditionOperator.NULL.getSql(), null);
    } else if (e instanceof IsNotNull) {
      return new Condition(((IsNotNull) e).arg.getAsString(sqlString), ConditionOperator.NOTNULL.getSql(), null);
    } else if (e instanceof Not) {
      Condition c = convertExpressionIntoCondition2(sqlString, ((Not) e).subExpression);
      if (!(((Not) e).subExpression instanceof AndOrExpressions)) {
        //FIXME nur aus abwärtskompatibilitätsgründen zwischenschicht
        Condition c2 = new Condition();
        c2.isEmpty = false;
        c2.conditions.add(c);
        c2.isNegated = true;
        return c2;
      }
      c.isNegated = true;
      return c;
    } else if (e instanceof AndOrExpressions) {
      Condition c = new Condition();
      c.isEmpty = false;
      for (int i = 0; i < ((AndOrExpressions) e).subExpressions.size(); i++) {
        c.conditions.add(convertExpressionIntoCondition2(sqlString, ((AndOrExpressions) e).subExpressions.get(i)));
      }
      for (int i = 0; i < ((AndOrExpressions) e).operators.size(); i++) {
        if (((AndOrExpressions) e).operators.get(i).token.type == TokenType.AND) {
          c.operators.add(Operator.JAVA_OPERATOR_AND);
        } else {
          c.operators.add(Operator.JAVA_OPERATOR_OR);
        }
      }
      return c;
    } else if (e instanceof InList) {
      StringBuilder list = new StringBuilder();
      List<ConditionToken> els = ((InList) e).elements;
      list.append("(");
      if (els.size() > 0) {
        if (els.get(0).type == TokenType.ESCAPED) {
          list.append("'");
        }
        list.append(sqlString.substring(els.get(0).startIndex, els.get(els.size() - 1).endIndex + 1));
        if (els.get(els.size() - 1).type == TokenType.ESCAPED) {
          list.append("'");
        }
      }
      list.append(")");
      return new Condition(((InList) e).arg.getAsString(sqlString), ConditionOperator.IN.getSql(), list.toString());
    }
    throw new RuntimeException("unexpected condition type: " + e.getClass().getName());
  }


  public Condition() {
    isEmpty = true;
  }
  
  private Condition(String expr1, String operator, String expr2) {
    this.expr1 = expr1;
    this.conditionOperator = ConditionOperator.getBySql(operator.toLowerCase());
    this.expr2 = expr2;
  }

  public List<Condition> getConditions() {
    return Collections.unmodifiableList(conditions);
  }

  public List<Operator> getOperators() {
    return Collections.unmodifiableList(operators);
  }

  public boolean isNegated() {
    return isNegated;
  }

  protected String getExpression1() {
    return expr1;
  }

  protected String getExpression2() {
    return expr2;
  }

  protected String getExpressionOperator() {
    return this.conditionOperator.getSql();
  }

  private class JavaGenContext {

    private int parameterIndex;
    private TableInfo t;


    public JavaGenContext(TableInfo t, int parameterStartIndex) {
      parameterIndex = parameterStartIndex;
      this.t = t;
    }


    public int getNextParameterIndex() {
      return parameterIndex++; // vorherigen index zurückgeben
    }

  }


  private String getTypeOf(String colName, JavaGenContext context) throws PreparedQueryBuildException {
    for (ColumnDeclaration cd : context.t.getColTypes()) {
      if (cd.getName().equalsIgnoreCase(colName)) {
        return cd.getJavaType();
      }
    }
    throw new PreparedQueryBuildException("did not find column with name " + colName);
  }
  
  private String getFQTypeOf(String colName, JavaGenContext context) {
    for (ColumnDeclaration cd : context.t.getColTypes()) {
      if (cd.getName().equalsIgnoreCase(colName)) {
        return cd.getFQJavaType();
      }
    }
    throw new RuntimeException("did not find column with name " + colName);
  }


  private String exprToJava(String expr, JavaGenContext context, String otherExpr)
      throws PreparedQueryBuildException {
    return exprToJava(expr, context, otherExpr, false);
  }
  
  private final static Pattern findCharsToReplaceForStringLiteral = Pattern.compile("([\\\\\"])"); //backslashes und anführungszeichen

  private String exprToJava(String expr, JavaGenContext context, String otherExpr, boolean escapeExprForLikeStatement)
      throws PreparedQueryBuildException {

    if (expr.startsWith("'")) {
      if (expr.length() < 2 || !expr.endsWith("'")) {
        throw new IllegalArgumentException("Invalid expression: " + expr);
      }
      /*
       * "."a%     -> a ist escaped, es wird praktisch nach strings, die mit .a anfangen gesucht
       *    \.a  
       * \"a%      -> es wird nach strings, die mit "a anfangen gesucht
       *    \"a (generierter javacode benötigt anführungszeichen escaped)
       * \\\"a%     -> es wird nach strings, die mit \"a anfangen gesucht
       *    \\\\\"a
       * \\\\\"a%   -> es wird nach strings, die mit \\"a anfangen gesucht
       *    \\\\\\\\\"a
       *    
       * für string müssen anführungszeichen und backslashes jeweils 1x escaped werden
       * für regexp müssen backslashes 1x escaped werden
       * 
       */
      String exprSubstring = expr.substring(1, expr.length() - 1);
      if (escapeExprForLikeStatement) {
        exprSubstring = SelectionParser.escapeParams(exprSubstring, true, new EscapeForMemory());
        exprSubstring = findCharsToReplaceForStringLiteral.matcher(exprSubstring).replaceAll("\\\\$1");
        return Pattern.class.getSimpleName() + ".compile(\"" + exprSubstring + "\")";
      } else {
        exprSubstring = findCharsToReplaceForStringLiteral.matcher(exprSubstring).replaceAll("\\\\$1");
        return "\"" + exprSubstring + "\"";
      }
    }

    if (expr.equals("?")) {
      String type = getTypeOf(otherExpr, context);
      if (escapeExprForLikeStatement) {
        //          String className = PreparedQueryCreator.class.getName();
        //          String methodNameFq = className + ".getEscapedRegexpVersionFromSQLLike";
        //          String argument = "\"\" + p.get(" + context.getNextParameterIndex() + ")";
        String parameterValuePattern = "p.getParameterAsPattern(" + context.getNextParameterIndex() + ")";
        return parameterValuePattern;
        //          return methodNameFq + "(" + argument + ")";
      } else {
        // falls type klein long ist oder sowas, dann kommt aus p nen objekt raus => casten und umwandeln.
        if (type.equals("long")) {
          return "((Number) p.get(" + context.getNextParameterIndex() + ")).longValue()";
        } else if (type.equals("int")) {
          return "((Number) p.get(" + context.getNextParameterIndex() + ")).intValue()";
        } else if (type.equals("double")) {
          return "((Number) p.get(" + context.getNextParameterIndex() + ")).doubleValue()";
        } else if (type.equals("byte")) {
          return "((Number) p.get(" + context.getNextParameterIndex() + ")).byteValue()";
        } else if (type.equals("boolean")) {
          return "((Boolean) p.get(" + context.getNextParameterIndex() + ")).booleanValue()";
        } else if (type.equals("float")) {
          return "((Number) p.get(" + context.getNextParameterIndex() + ")).floatValue()";
        } else {
          return "(" + getFQTypeOf(otherExpr, context) + ") p.get(" + context.getNextParameterIndex() + ")";
        }
      }
    }

    if (PATTERN_ANY_NUMBER.matcher(expr).matches()) {
      if (!otherExpr.equals("?")) {
        if ("long".equalsIgnoreCase(getTypeOf(otherExpr, context))) {
          return expr + "L";
        } else if ("short".equalsIgnoreCase(getTypeOf(otherExpr, context))) {
          return "(short)" + expr;
        } else if ("byte".equalsIgnoreCase(getTypeOf(otherExpr, context))) {
          return "(byte)" + expr;
        } else if ("char".equalsIgnoreCase(getTypeOf(otherExpr, context)) || "character".equalsIgnoreCase(getTypeOf(otherExpr, context))) {
          return "(char)" + expr;
        } else if ("float".equalsIgnoreCase(getTypeOf(otherExpr, context))) {
          return expr + "f";
        } else {
          return expr;
        }
      } else {
        return expr;
      }
    }

    // ((WorkflowInstanceDetails) s).getId()
    for (int colIdx = 0; colIdx < context.t.getColTypes().length; colIdx++) {
      if (context.t.getColTypes()[colIdx].getName().equalsIgnoreCase(expr)) {
        String getter = context.t.getColTypes()[colIdx].getGetter();
        if (getter == null) {
          throw new PreparedQueryBuildException("column " + expr
              + " can not be accessed, because there is no getter function with the appropriate name defined in class "
              + context.t.getBackingClass().getCanonicalName());
        }
        return "((" + context.t.getBackingClass().getCanonicalName() + ")s)." + getter + "()";
      }
    }

    logger.warn("could not build java expression for " + expr);
    throw new PreparedQueryBuildException("could not build java expression for " + expr);

  }


  /**
   * kann mit "(" enden
   */
  private String conditionOperatorToJava(String expressionType) {

    switch (conditionOperator) {
      case UNEQUAL1 :
        //fall through
      case UNEQUAL2 :
        throw new RuntimeException("not equal must be transformed to not ( .. equal .. )");
      case EQUALS :
        if (primitiveTypes.contains(expressionType)) {
          return "==";
        } else {
          return (".equals(");
        }
      case LIKE :
        return (".matcher(");
      case GREATER :
        if (objectsOfPrimitiveTypes.contains(expressionType) || primitiveTypes.contains(expressionType)) {
          return " > ";
        }
        throw new IllegalArgumentException("Cannot apply '>' to anything different from a number");
      case SMALLER :
        if (objectsOfPrimitiveTypes.contains(expressionType) || primitiveTypes.contains(expressionType)) {
          return " < ";
        }
        throw new IllegalArgumentException("Cannot apply '>' to anything different from a number");
      case NULL :
        return "== null";
      case NOTNULL :
        return "!= null";
      default :
        return conditionOperator.getSql();
    }

  }


  public Map<String, String> generateWhereConditionBooleans(TableInfo t, int parameterStartIndex)
      throws PreparedQueryBuildException {
    if (isEmpty) {
      return null;
    }
    return generateWhereConditionBooleans(new JavaGenContext(t, parameterStartIndex), null, new AtomicInteger(0));
  }


  private Map<String, String> generateWhereConditionBooleans(JavaGenContext context,
                                                             Map<String, String> existingBooleans, AtomicInteger count)
      throws PreparedQueryBuildException {

    if (existingBooleans == null) {
      existingBooleans = new HashMap<String, String>();
    }

    if (conditions.size() == 0) {

      String booleanName = CONDITION_BOOL_PREFIX + count.getAndIncrement();

      StringBuilder sb = new StringBuilder();

      String localConditionOperatorInJava = conditionOperatorToJava(getTypeOf(expr1, context));
      final boolean likeOperator = conditionOperator == ConditionOperator.LIKE;
      final boolean isNullOperator;
      if (likeOperator) {
        isNullOperator = false;
      } else {
        isNullOperator =
            conditionOperator == ConditionOperator.NULL || conditionOperator == ConditionOperator.NOTNULL;
      }

      // avoid NPE, but not for simple types (these are identified by the fact that the operator contains a ""
      // substring to cast to String
      String expressionType = getTypeOf(expr1, context);
      boolean localConditionOperatorContainsQuotationMarks = localConditionOperatorInJava.contains("\"\"");
      if (!primitiveTypes.contains(expressionType) && !isNullOperator) {
        sb.append(exprToJava(expr1, context, expr2) + " != null && ");
      }

      boolean localConditionOperatorInJavaEndsWithClosingBrace = localConditionOperatorInJava.endsWith("(");
      if (likeOperator && localConditionOperatorContainsQuotationMarks) {
        // weil der javaoperator ... + "").matches( keine öffnende klammer hat; für Strings ist der Operator aber nur
        // '.matches(', d.h. es wird doch wieder keine Klammer gebraucht
        sb.append("(");
      } else if (!localConditionOperatorInJavaEndsWithClosingBrace && isNegated) {
        //TODO wahrscheinlich nicht mehr notwendig, seit die einzel conditions in eigenen methoden liegen.
        //achtung aber unten bei klammer zu
        sb.append("(");
      }

      if (likeOperator) {
        sb.append(exprToJava(expr2, context, expr1, true));
        sb.append(localConditionOperatorInJava).append(" ");
        sb.append(exprToJava(expr1, context, expr2));
        if (objectsOfPrimitiveTypes.contains(expressionType) || primitiveTypes.contains(expressionType)) {
          sb.append(" + \"\"");
        }
        sb.append(" ).matches()");
      } else {
        sb.append(exprToJava(expr1, context, expr2)).append(" ");
        sb.append(localConditionOperatorInJava).append(" ");
        if (isNullOperator) {
          // nothing to be done, the operator contains "== null" or "!= null"
          // FIXME check that this is not used for trivial types
        } else {
          sb.append(exprToJava(expr2, context, expr1));
        }
        if (localConditionOperatorInJavaEndsWithClosingBrace || isNegated) {
          sb.append(")");
        }
      }

      existingBooleans.put(booleanName, sb.toString());

    } else if (conditions.size() == 1 && !isNegated) {
      //keine klammern notwendig
      conditions.get(0).generateWhereConditionBooleans(context, existingBooleans, count);
    } else {
      for (int i = 0; i < operators.size(); i++) {
        conditions.get(i).generateWhereConditionBooleans(context, existingBooleans, count);
      }
      conditions.get(conditions.size() - 1).generateWhereConditionBooleans(context, existingBooleans, count);
    }

    return existingBooleans;
  }


  public String generateWhereConditionExpression(TableInfo t, int parameterStartIndex) {
    return generateWhereConditionExpression(new JavaGenContext(t, parameterStartIndex), new AtomicInteger(0));
  }


  private String generateWhereConditionExpression(JavaGenContext context, AtomicInteger count) {
    if (isEmpty) {
      if (isNegated) {
        return "false";
      } else {
        return "true";
      }
    }
    StringBuffer sb = new StringBuffer();
    if (isNegated) {
      sb.append("!");
    }
    if (conditions.size() == 0) {
      sb.append(" " + CONDITION_BOOL_PREFIX + count.getAndIncrement() + "(s, p) ");
    } else if (conditions.size() == 1 && !isNegated) {
      // keine klammern notwendig
      sb.append(conditions.get(conditions.size() - 1).generateWhereConditionExpression(context, count));
    } else {
      sb.append("(");
      for (int i = 0; i < operators.size(); i++) {
        sb.append(conditions.get(i).generateWhereConditionExpression(context, count) + " "
            + operators.get(i).toJava() + " ");
      }
      sb.append(conditions.get(conditions.size() - 1).generateWhereConditionExpression(context, count));
      sb.append(")");
    }
    String result = sb.toString();
    return result;
  }


  
  public String getExpr1() {
    return expr1;
  }


  
  public String getExpr2() {
    return expr2;
  }


  
  public ConditionOperator getConditionOperator() {
    return conditionOperator;
  }

}
