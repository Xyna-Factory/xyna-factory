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
package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.xfractwfe.formula.AndOperator;
import com.gip.xyna.xprc.xfractwfe.formula.Assign;
import com.gip.xyna.xprc.xfractwfe.formula.BaseType;
import com.gip.xyna.xprc.xfractwfe.formula.DivideOperator;
import com.gip.xyna.xprc.xfractwfe.formula.EqualsOperator;
import com.gip.xyna.xprc.xfractwfe.formula.Expression;
import com.gip.xyna.xprc.xfractwfe.formula.Expression2Args;
import com.gip.xyna.xprc.xfractwfe.formula.FollowableType;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression.CastExpression;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression.DynamicResultTypExpression;
import com.gip.xyna.xprc.xfractwfe.formula.Functions;
import com.gip.xyna.xprc.xfractwfe.formula.GtOperator;
import com.gip.xyna.xprc.xfractwfe.formula.GteOperator;
import com.gip.xyna.xprc.xfractwfe.formula.LiteralExpression;
import com.gip.xyna.xprc.xfractwfe.formula.LocalExpressionVariable;
import com.gip.xyna.xprc.xfractwfe.formula.LocalExpressionVariable.ExtractionReason;
import com.gip.xyna.xprc.xfractwfe.formula.LtOperator;
import com.gip.xyna.xprc.xfractwfe.formula.LteOperator;
import com.gip.xyna.xprc.xfractwfe.formula.MinusOperator;
import com.gip.xyna.xprc.xfractwfe.formula.MultiplyOperator;
import com.gip.xyna.xprc.xfractwfe.formula.Not;
import com.gip.xyna.xprc.xfractwfe.formula.NotEqualsOperator;
import com.gip.xyna.xprc.xfractwfe.formula.Operator;
import com.gip.xyna.xprc.xfractwfe.formula.OrOperator;
import com.gip.xyna.xprc.xfractwfe.formula.Parser;
import com.gip.xyna.xprc.xfractwfe.formula.PlusOperator;
import com.gip.xyna.xprc.xfractwfe.formula.SingleVarExpression;
import com.gip.xyna.xprc.xfractwfe.formula.SupportedFunctionStore;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;
import com.gip.xyna.xprc.xfractwfe.formula.VariableInstanceFunctionIncovation;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer;
import com.gip.xyna.xprc.xfractwfe.formula.XFLOperatorPrecedenceAdjuster;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedIdentification.VariableInfoPathMap;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.VariableInfo;



/**
 * Klasse für Parsing, Typisierung und Codegenerierung von Formel-Ausdrücken, wie sie in 
 * Conditional Choices und Mappings modelliert und verwendet werden können.
 * 
 * Das Parsing geschieht im Parser, die Typisierung und Codegenerierung passiert hauptsächlich über
 * ein Visitor-Pattern.
 * 
 * Formel-Ausdrücke haben folgenden Syntax
 * <pre>
 * Expr := ValueExpr | ValueExpr mit AND/OR und klammerungen verknüpft
 * ValueExpr := SingleValueExpr | Function(ValueExpr, ...) | ValueExpr Operator ValueExpr | Prefix ValueExpr
 * Prefix := - | !/not |
 * Operator := + | - | &lt; | &gt; | == | &lt;= | &gt;= | != 
 * SingleValueExpr := PathExpr | "Constant/Literal" | null
 * PathExpr := % VariableIndex % ( [ValueExpr] )? (. (MemberName | InstanceFunctionName\( ( Expr (, Expr)* )? \) ) ( [ValueExpr ] )?)*
 * VariableIndex := Index der Variable im lokalen Workflow-Context (step.getInputIds, step.getLocalIds, 
 *   step.getOutputIds werden in dieser Reihenfolge durchgezählt)
 * MemberName := Name einer Membervariable der Variable
 * Mapping := PathExpr = Expr
 * </pre>
 * 
 * Beispiel: 
 *   %13%.bool=contains("hello 3netordernumber" , concat(%5%["1"].data[%2%.containsSimpleList.data[%2%.containsSimpleList.data["1"]]], "1"+"2", "net", "order","number" ))
 * könnte true ergeben.
 */
public class ModelledExpression {

  
  public final static String TEMP_VARIABLE_PREFIX = "_mft1_v";
  private final static String TEMP_DEEPER_VARIABLE_PREFIX = "_mfv_v";
  private final static Logger logger = CentralFactoryLogging.getLogger(ModelledExpression.class);
  
  private final String expression;
  private final VariableContextIdentification variableContext;
  
  /*
   * falls gesamtausdruck "a = b" ist, dann ist target=a, source=b
   * falls nicht, ist nur target gesetzt.
   */
  private Expression parsedTargetExpression;
  private Expression parsedSourceExpression;//existiert nur, wenn der gesamtausdruck die form "a = b" hat 
  private Assign foundAssign = null;
  private List<LocalExpressionVariable> exprVars = Collections.emptyList();
  
  
  public ModelledExpression(VariableContextIdentification variableContext, String expression) {
    this.expression = expression;
    this.variableContext = variableContext;
  }



  public interface Visitor {
    
    public void expression2ArgsStarts(Expression2Args expression);

    public void functionEnds(FunctionExpression fe);

    public void functionSubExpressionEnds(FunctionExpression fe, int parameterCnt);

    public void functionSubExpressionStarts(FunctionExpression fe, int parameterCnt);

    public void functionStarts(FunctionExpression fe);
    
    public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi);
    
    public void instanceFunctionEnds(VariableInstanceFunctionIncovation vifi);
    
    @Deprecated
    public void instanceFunctionSubExpressionEnds(Expression fe, int parameterCnt);

    @Deprecated
    public void instanceFunctionSubExpressionStarts(Expression fe, int parameterCnt);
    
    
    public default void instanceFunctionSubExpressionEnds(VariableInstanceFunctionIncovation vifi, int parameterCnt) {
      instanceFunctionSubExpressionEnds(vifi.getFunctionParameter().get(parameterCnt), parameterCnt);
    }
    
    public default void instanceFunctionSubExpressionStarts(VariableInstanceFunctionIncovation vifi, int parameterCnt) {
      instanceFunctionSubExpressionStarts(vifi.getFunctionParameter().get(parameterCnt), parameterCnt);
    }
    
    public void allPartsOfVariableFinished(Variable variable);

    public void expression2ArgsEnds(Expression2Args expression);

    public void literalExpression(LiteralExpression expression);

    public void notStarts(Not not);

    public void notEnds(Not not);

    public void operator(Operator operator);

    public void variableStarts(Variable variable);

    public void variableEnds(Variable variable);
    
    default public void singleVarExpressionStarts(SingleVarExpression expression) { };
    
    default public void singleVarExpressionEnds(SingleVarExpression expression) { };
    

    /**
     * aaa.bbb[].ccc.ddd[].eee
     *     ^
     *     cursor 
     */
    public void variablePartStarts(VariableAccessPart part);
    
    default public void variablePartStarts(VariableAccessPart part, boolean isLastPart) { variablePartStarts(part); };


    /**
     * aaa.bbb[].ccc.ddd[].eee
     *         ^
     *     cursor bevor ccc beginnt
     */
    public void variablePartEnds(VariableAccessPart part);
    
    default public void variablePartEnds(VariableAccessPart part, boolean isLastPart) { variablePartEnds(part); };
    
    /**
     * aaa.bbb[].ccc.ddd[].eee
     *         ^
     *     cursor nachdem alle kind-parts fertig sind
     *     
     * also partStarts(aaa)
     *      partEnds(aaa)
     *      partStarts(bbb)
     *      partEnds(bbb)
     *      partStarts(ccc)
     *      partEnds(ccc)
     *      partStarts(ddd)
     *      partEnds(ddd)
     *      partStarts(eee)
     *      partEnds(eee)
     *      partSubContextEnds(eee)
     *      partSubContextEnds(ddd)
     *      partSubContextEnds(ccc)
     *      partSubContextEnds(bbb)
     *      partSubContextEnds(aaa)
     */
    public void variablePartSubContextEnds(VariableAccessPart p);

    public void allPartsOfFunctionFinished(FunctionExpression fe);

    @Deprecated
    public void indexDefStarts();
    
    public default void indexDefStarts(Expression exp) {
      indexDefStarts();
    }

    @Deprecated
    public void indexDefEnds();
    
    public default void indexDefEnds(Expression exp) {
      indexDefEnds();
    }
    
    public default void functionAllSubExpressionsEnds(FunctionExpression fe) {
    }
    
  }
  
  static boolean isCast(FunctionExpression fe) {
    return fe.getFunction().getName().equals(Functions.CAST_FUNCTION_NAME);
  }
  
  public static boolean isNew(FunctionExpression fe) {
    return fe.getFunction().getName().equals(Functions.NEW_FUNCTION_NAME);
  }
  
  /**
   * visitor für die erzeugung des xfl strings aus den javaobjekten
   */
  public static class IdentityCreationVisitor extends EmptyVisitor {
    
    protected final StringBuilder sb = new StringBuilder();
    protected Stack<FunctionExpression> lastFunction = new Stack<FunctionExpression>();
    protected boolean skipNextLiteral;

    public void expression2ArgsStarts(Expression2Args expression) {
      sb.append("(");
    }

    public void functionEnds(FunctionExpression fe) {
      if (isCast(fe)) {   
        sb.append("#cast(");
        literalExpression((LiteralExpression) fe.getSubExpressions().get(0));
        sb.append(")");
      }
      if (fe.getIndexDef() != null) {
        appendIndexDefEnd();
      }
    }
    
    @Override
    public void allPartsOfFunctionFinished(FunctionExpression fe) {
      lastFunction.pop();
    }


    public void functionSubExpressionEnds(FunctionExpression fe, int parameterCnt) {
      if (lastFunction.peek().getSubExpressions().size() == parameterCnt + 1) {
        if (!isCast(fe)) {
          sb.append(")");
        }
        if (lastFunction.peek().getIndexDef() != null) {
          appendIndexDefStart();
        }
      }
    }

    public void functionSubExpressionStarts(FunctionExpression fe, int parameterCnt) {
      if (parameterCnt > 0 && !(isCast(fe) && parameterCnt == 1)) {
        sb.append(", ");
      }
    }

    public void functionStarts(FunctionExpression fe) {
      lastFunction.push(fe);
      if (isCast(fe)) {
        skipNextLiteral = true;
        return;
      }
      sb.append(fe.getFunction().getName());
      if (!fe.getFunction().getName().equals("null")) {
        sb.append("(");
      }
    }

    public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi) {
      sb.append("(");
    }

    public void instanceFunctionEnds(VariableInstanceFunctionIncovation vifi) {
      sb.append(")");
      if (vifi.getIndexDef() != null) {
        appendIndexDefStart();
      }
    }

    public void instanceFunctionSubExpressionStarts(Expression fe, int parameterCnt) {
      if (parameterCnt > 0) {
        sb.append(", ");
      }
    }

    public void expression2ArgsEnds(Expression2Args expression) {
      sb.append(")");
    }

    public void literalExpression(LiteralExpression expression) {
      if (skipNextLiteral) {
        skipNextLiteral = false;
      } else {
        sb.append("\"").append(expression.getValueEscapedForJava()).append("\"");
      }
    }

    public void notStarts(Not not) {
      sb.append("!");
    }

    public void operator(Operator operator) {
      sb.append(operator.getOperatorAsString());
    }

    public void variableStarts(Variable variable) {
      sb.append("%").append(variable.getVarNum()).append("%");
      if (variable.getIndexDef() != null) {
        appendIndexDefStart();
      }
    }
    
    protected void appendIndexDefStart() {
      sb.append("[");
    }
    
    protected void appendIndexDefEnd() {
      sb.append("]");
    }

    public void variableEnds(Variable variable) {
      if (variable.getIndexDef() != null) {
        appendIndexDefEnd();
      }
    }

    public void variablePartStarts(VariableAccessPart part) {
      sb.append(".").append(part.getName());
      if (part.isMemberVariableAccess() && 
          part.getIndexDef() != null) {
        appendIndexDefStart();
      }
    }

    public void variablePartEnds(VariableAccessPart part) {
      if (part.getIndexDef() != null) {
        appendIndexDefEnd();
      }
    }

    public String getXFLExpression() {
      return sb.toString();
    }

  }

  public static class InitVariablesVisitor extends EmptyVisitor {

    private List<XPRC_InvalidVariableIdException> exceptionsInvalidIds =
        new ArrayList<XPRC_InvalidVariableIdException>();
    private List<XPRC_InvalidVariableMemberNameException> exceptionsInvalidMembers =
        new ArrayList<XPRC_InvalidVariableMemberNameException>();


    public void rethrowException() throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException {
      if (exceptionsInvalidIds.size() > 0) {
        throw exceptionsInvalidIds.get(0);
      }
      if (exceptionsInvalidMembers.size() > 0) {
        throw exceptionsInvalidMembers.get(0);
      }
    }


    public void variableStarts(Variable variable) {
      try {
        variable.validate();
      } catch (XPRC_InvalidVariableIdException e) {
        exceptionsInvalidIds.add(e);
      } catch (XPRC_InvalidVariableMemberNameException e) {
        exceptionsInvalidMembers.add(e);
      }
    }

    @Override
    public void functionStarts(FunctionExpression fe) {
      try {
        fe.validate();
      } catch (XPRC_InvalidVariableIdException e) {
        exceptionsInvalidIds.add(e);
      } catch (XPRC_InvalidVariableMemberNameException e) {
        exceptionsInvalidMembers.add(e);
      }
    }
  }
  
  
  public static class InferOriginalTypeVisitor extends EmptyVisitor {
    

    public void functionEnds(FunctionExpression fe) {
      fe.setOriginalType(fe.getResultType());
    }

    public void expression2ArgsEnds(Expression2Args expression) {
      Operator op = expression.getOperator();
      if (op instanceof GtOperator || op instanceof GteOperator || op instanceof LtOperator
          || op instanceof LteOperator) {
        expression.setOriginalType(new TypeInfo(BaseType.BOOLEAN_PRIMITIVE));
      } else if (op instanceof PlusOperator || op instanceof MinusOperator || op instanceof DivideOperator
          || op instanceof MultiplyOperator) {
        TypeInfo t;
        try {
          t = calcResultingNumberType(expression.getVar1().getOriginalType(), expression.getVar2().getOriginalType());
        } catch (XPRC_InvalidVariableMemberNameException e) {
          throw new RuntimeException(e);
        }
        expression.setOriginalType(t);
      } else if (op instanceof AndOperator || op instanceof OrOperator) {
        expression.setOriginalType(new TypeInfo(BaseType.BOOLEAN_PRIMITIVE));
      } else if (op instanceof EqualsOperator || op instanceof NotEqualsOperator) {
        expression.setOriginalType(new TypeInfo(BaseType.BOOLEAN_PRIMITIVE));
      }
    }

    public void literalExpression(LiteralExpression expression) {
      try {
        if (expression.getOriginalType().isUnknown()) {
          if (isNumber(expression.getValue())) {
            expression.setOriginalType(TypeInfo.ANYNUMBER);
          } else if (expression.getValue().equalsIgnoreCase("true") || expression.getValue().equalsIgnoreCase("false")) {
            expression.setOriginalType(new TypeInfo(BaseType.BOOLEAN_OBJECT));
          } else {
            expression.setOriginalType(new TypeInfo(BaseType.STRING));
          }
        }
      } catch (XPRC_InvalidVariableMemberNameException e) {
        throw new RuntimeException(e);
      }
    }

  }
  

  public static class MakeTypeDefinitionConsistentVisitor extends EmptyVisitor {
    
    private Stack<VariableInstanceFunctionIncovation> instanceInvocationStack = new Stack<VariableInstanceFunctionIncovation>();

    private boolean allowStringComparison = false;
    
    public MakeTypeDefinitionConsistentVisitor() {
    }
    
    public void setAllowStringComparison() {
      allowStringComparison = true;
    }

    public void expression2ArgsStarts(Expression2Args expression) {
      try {
        Operator op = expression.getOperator();
        if (op instanceof GtOperator || op instanceof GteOperator || op instanceof LtOperator || op instanceof LteOperator) {
          if (allowStringComparison) {
            if (!expression.getVar1().getOriginalType().isAnyNumber()) {
              expression.getVar1().setTargetType(new TypeInfo(PrimitiveType.STRING));
            }
            if (!expression.getVar2().getOriginalType().isAnyNumber()) {
              expression.getVar2().setTargetType(new TypeInfo(PrimitiveType.STRING));
            }
          } else {
            if (!expression.getVar1().getOriginalType().isAnyNumber()) {
              expression.getVar1().setTargetType(TypeInfo.ANYNUMBER);
            } else {
              expression.getVar1().setTargetType(expression.getVar1().getOriginalType());
            }
            if (!expression.getVar2().getOriginalType().isAnyNumber()) {
              expression.getVar2().setTargetType(TypeInfo.ANYNUMBER);
            } else {
              expression.getVar2().setTargetType(expression.getVar2().getOriginalType());
            }
          }
        } else if (op instanceof PlusOperator || op instanceof MinusOperator || op instanceof DivideOperator
            || op instanceof MultiplyOperator) {
          if (!expression.getVar1().getOriginalType().isAnyNumber()) {
            expression.getVar1().setTargetType(TypeInfo.ANYNUMBER);
          } else {
            expression.getVar1().setTargetType(expression.getVar1().getOriginalType());
          }
          if (!expression.getVar2().getOriginalType().isAnyNumber()) {
            expression.getVar2().setTargetType(TypeInfo.ANYNUMBER);
          } else {
            expression.getVar2().setTargetType(expression.getVar2().getOriginalType());
          }
        } else if (op instanceof AndOperator || op instanceof OrOperator) {
          expression.getVar1().setTargetType(new TypeInfo(BaseType.BOOLEAN_PRIMITIVE));
          expression.getVar2().setTargetType(new TypeInfo(BaseType.BOOLEAN_PRIMITIVE));
        } else if (op instanceof EqualsOperator || op instanceof NotEqualsOperator) {
          boolean nullComparison = expression.getVar1().getOriginalType().isNull() || expression.getVar2().getOriginalType().isNull();
          if (nullComparison) {
            expression.getVar1().setTargetType(expression.getVar1().getOriginalType());
            expression.getVar2().setTargetType(expression.getVar2().getOriginalType());
          } else {
            if (expression.getVar1().getOriginalType().isUnknown()) {
              if (expression.getVar2().getOriginalType().isUnknown()) {
                //beide unknown
                expression.getVar1().setTargetType(new TypeInfo(BaseType.STRING));
                expression.getVar2().setTargetType(new TypeInfo(BaseType.STRING));
              } else {
                //unknown übernimmt typ vom anderen
                expression.getVar1().setTargetType(expression.getVar2().getOriginalType());
                expression.getVar2().setTargetType(expression.getVar2().getOriginalType());
              }
            } else {
              if (expression.getVar2().getOriginalType().isUnknown()) {
                //unknown übernimmt typ vom anderen
                expression.getVar1().setTargetType(expression.getVar1().getOriginalType());
                expression.getVar2().setTargetType(expression.getVar1().getOriginalType());
              } else {
                //beide nicht unknown
                if (expression.getVar2().getOriginalType().equals(expression.getVar1().getOriginalType(), true)) {
                  //gleicher typ
                  expression.getVar1().setTargetType(expression.getVar1().getOriginalType());
                  expression.getVar2().setTargetType(expression.getVar2().getOriginalType());
                } else {
                  //TODO bessere fallunterscheidungen
                  if (expression.getVar2().getOriginalType().isAnyNumber() && expression.getVar1().getOriginalType().isAnyNumber()) {
                    TypeInfo t = calcResultingNumberType(expression.getVar1().getOriginalType(), expression.getVar2().getOriginalType());
                    expression.getVar1().setTargetType(t);
                    expression.getVar2().setTargetType(t);
                  } else {
                    expression.getVar1().setTargetType(new TypeInfo(BaseType.STRING));
                    expression.getVar2().setTargetType(new TypeInfo(BaseType.STRING));
                  }
                }
              }
            }
            op.setNeedsEquals(true);
          }
        }
      } catch (XPRC_InvalidVariableMemberNameException e) {
        throw new RuntimeException(e);
      }
    }


    public void literalExpression(LiteralExpression expression) {
      if (expression.getTargetType().isUnknown()) {
        try {
          expression.setTargetType(expression.getOriginalType());
        } catch (XPRC_InvalidVariableMemberNameException e) {
          throw new RuntimeException(e);
        }
      } else if (expression.getTargetType().equals(new TypeInfo(BaseType.LONG_OBJECT), true)) {
        if (isLong(expression.getValue())) {
          //ok, ntbd
        } else {
          throw new RuntimeException(expression.getValue() + " is not a long.");
        }
      } else if (expression.getTargetType().equals(new TypeInfo(BaseType.DOUBLE_OBJECT), true)) {
        if (isDouble(expression.getValue())) {
          //ok, ntbd
        } else {
          throw new RuntimeException(expression.getValue() + " is not a double.");
        }
      } else if (expression.getTargetType().equals(new TypeInfo(BaseType.INT_OBJECT), true)) {
        if (isInt(expression.getValue())) {
          //ok, ntbd
        } else {
          throw new RuntimeException(expression.getValue() + " is not a int.");
        }
      } else if (expression.getTargetType().isAnyNumber()) {
        if (isNumber(expression.getValue())) {
          //ok, ntbd
        } else {
          throw new RuntimeException(expression.getValue() + " is not a number.");
        }
      }
      //boolean geht immer, string auch
    }


    public void notStarts(Not not) {
      try {
        not.getInnerExpression().setTargetType(new TypeInfo(BaseType.BOOLEAN_OBJECT));
      } catch (XPRC_InvalidVariableMemberNameException e) {
        throw new RuntimeException(e);
      }
    }


    public void variableStarts(Variable variable) {
      try {
        if (variable.getIndexDef() != null) {
          //zugriff auf listenelement geht nur per int-wertigem index
          variable.getIndexDef().setTargetType(new TypeInfo(BaseType.INT_PRIMITIVE));
        }
        if (!variable.getTargetType().isUnknown()) {
          if (!variable.getTypeOfExpression().equals(variable.getTargetType(), true)) {
            //TODO inkompatible typen erkennen und fehler werfen. passiert aber ansosnten später beim compile
          }
        } else {
          variable.setTargetType(variable.getTypeOfExpression());
        }
      } catch (XPRC_InvalidVariableMemberNameException e) {
        throw new RuntimeException(e);
      }
    }


    public void variablePartStarts(VariableAccessPart part) {
      if (part.getIndexDef() != null) {
        //zugriff auf listenelement geht nur per int-wertigem index
        try {
          part.getIndexDef().setTargetType(new TypeInfo(BaseType.INT_PRIMITIVE));
        } catch (XPRC_InvalidVariableMemberNameException e) {
          throw new RuntimeException(e);
        }
      }
    }


    public void functionSubExpressionStarts(FunctionExpression fe, int parameterCnt) {
      TypeInfo type = fe.getParameterTypeDef(parameterCnt);
      if (type.isAny()) {
        return;
      }
      try {
        fe.getSubExpressions().get(parameterCnt).setTargetType(type);
      } catch (XPRC_InvalidVariableMemberNameException e) {
        throw new RuntimeException(e);
      }
    }


    public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi) {
      instanceInvocationStack.push(vifi);
    }


    public void instanceFunctionEnds(VariableInstanceFunctionIncovation vifi) {
      instanceInvocationStack.pop();
    }


    public void instanceFunctionSubExpressionStarts(Expression fe, int parameterCnt) {
      VariableInstanceFunctionIncovation callingFunction = instanceInvocationStack.peek();
      TypeInfo correspondingVariable = callingFunction.getInputParameterTypes().get(parameterCnt);
      try {
        fe.setTargetType(correspondingVariable);
      } catch (XPRC_InvalidVariableMemberNameException e) {
        throw new RuntimeException(e);
      }
    }

  }


  public class JavaCodeGeneratorVisitor extends ContextAwareVisitor {

    private StringBuilder sb = new StringBuilder();
    private boolean transformNextLiteralToClass = false;
    private boolean skipNextLiteral = false;
    private Set<Object> adjustments = new HashSet<>();
    
    
    public JavaCodeGeneratorVisitor() {
    }
    
    
    private StringBuilder getSB() {
        return sb;
    }
    
    
    public void expression2ArgsStarts(Expression2Args expression) {
      super.expression2ArgsStarts(expression);
      try {
        getSB().append(transformation("(", expression.getOriginalType(), expression.getTargetType())[0]).append(expression.getOperator().getPrefix());  
      } catch (XPRC_InvalidVariableMemberNameException e) {
        throw new RuntimeException(e);
      }
    }


    public void expression2ArgsEnds(Expression2Args expression) {
      try {
        getSB().append(expression.getOperator().needsClosingBrace() ? ")" : "").append(")").append(transformation("", expression.getOriginalType(), expression.getTargetType())[1]);
      } catch (XPRC_InvalidVariableMemberNameException e) {
        throw new RuntimeException(e);
      }
      super.expression2ArgsEnds(expression);
    }
    
    
    public void literalExpression(LiteralExpression expression) {
      super.literalExpression(expression);
      if (skipNextLiteral) {
        skipNextLiteral = false;
      } else if (transformNextLiteralToClass) {
        String fqNameOfCast = expression.getValue();
        if (GenerationBase.isReservedServerObjectByFqOriginalName(fqNameOfCast)) {
          fqNameOfCast = GenerationBase.getReservedClass(fqNameOfCast).getName();
        } else {
          try {
            fqNameOfCast = GenerationBase.transformNameForJava(fqNameOfCast);
          } catch (XPRC_InvalidPackageNameException e) {
            throw new RuntimeException("Cast to " + fqNameOfCast + " is invalid");
          }
        }
        getSB().append(fqNameOfCast).append(".class");
        transformNextLiteralToClass = false;
      } else {
        if (expression.getTargetType().isUnknown()) {
          throw new RuntimeException();
        } else if (expression.getTargetType().equals(BaseType.STRING, false)) {
          getSB().append("\"").append(expression.getValueEscapedForJava()).append("\"");
        } else if (expression.getTargetType().isList()) {
          getSB().append("new ArrayList(Arrays.asList(new Object[]{").append(expression.getValue()).append("}))");
        } else if (expression.getTargetType().equals(BaseType.LONG_OBJECT, false) || expression.getTargetType().equals(BaseType.LONG_PRIMITIVE, false)) {
          getSB().append(expression.getValue()).append("l");
        } else if (expression.getTargetType().equals(BaseType.FLOAT_OBJECT, false) || expression.getTargetType().equals(BaseType.FLOAT_PRIMITIVE, false)) {
          getSB().append(expression.getValue()).append("f");
        } else if (expression.getTargetType().equals(BaseType.DOUBLE_OBJECT, false)) {
          getSB().append(expression.getValue()).append("d");
        } else {
          getSB().append(expression.getValue());
        }
      }
    }

    
    public void notStarts(Not not) {
      super.notStarts(not);
      getSB().append("!");
    }


    public void operator(Operator operator) {
      super.operator(operator);
      getSB().append(operator.toJavaCode()); //TODO toJavaCode rauswerfen und hier ein switch/if-else
    }


    public void variableStarts(Variable variable) {
      super.variableStarts(variable);
      VariableInfo vi = variable.getBaseVariable();
      String getter = vi.getJavaCodeForVariableAccess();

      try {
        if (generateTypeResistantMappingCode.get()) {
          if (((StepBasedVariable)variable.getFollowedVariable()).getAVariable().isFunctionResult()) {
            // no adjustment for instance service results
            String[] transform = transformation(getter, variable.getTypeOfExpression(), variable.getTargetType());  
            getSB().append(transform[0]);
          } else if (isContext(SingleVarExpression.class)) {
            generateAdjustmentPrefix(getContext(SingleVarExpression.class), variable.getTargetType());
            getSB().append(getter);
          } else {
            String[] transform = transformation(getter, variable.getTypeOfExpression(), variable.getTargetType());  
            getSB().append(transform[0]).append(transform[1]);
          }
        } else {
          String[] transform = transformation(getter, variable.getTypeOfExpression(), variable.getTargetType());  
          getSB().append(transform[0]);
        }
      } catch (XPRC_InvalidVariableMemberNameException e) {
        throw new RuntimeException(e);
      }

      if (variable.isPathMap()) {
        isPathVariable.push(true);
        String valueGetter = GenerationBase.buildGetter(((VariableInfoPathMap) variable.getBaseVariable()).getVarNameOfValue());
        getSB().append(valueGetter).append("FromMap((\"");
        if (variable.getIndexDef() != null) {
          getSB().append("[\\\"\" + ");
        }
      } else {
        isPathVariable.push(false);
      }
    }


    private Object transformation_prefix(TypeInfo toType) {
      if (toType == null ||
          !toType.isBaseType()) {
        return Functions.class.getName()+"."+Functions.ADJUST_VALUE_METHOD_NAME+"(java.lang.Object.class, ";
      } else {
        return Functions.class.getName()+"."+Functions.ADJUST_VALUE_METHOD_NAME+"("+toType.getJavaName()+".class, ";
      }
    }
    
    
    /**
     * @return zwei-elementiges array. erstes element ist der start der transformation, zweites element die beendenden klammern.
     * grund für die teilung: dazwischen werden andere expressions vom visitor besucht. 
     */
    private String[] transformation(String value, TypeInfo fromType, TypeInfo toType) {
      if (fromType.equals(toType, true)) {
        return new String[] {value, ""};
      } else if (toType.isModelledType() && fromType.isModelledType() && !toType.isList() && !fromType.isList()) {
        //wenn beide listenwertig sind, wurde es obendrüber abgefangen.
        //wenn nur eines davon listenwertig ist, passt es nicht zusammen
        if (toType.getModelledType().isSuperClassOf(fromType.getModelledType())) {
          return new String[] {value, ""};
        } else if (fromType.getModelledType().isSuperClassOf(toType.getModelledType())) {
          //casting 
          return new String[] {"((" + toType.getJavaName() + ")" + value, ")"};
        } else {
          throw new RuntimeException("Type " + fromType.getJavaName() + " may not be mapped to " + toType.getJavaName()+ ".");
        }
      } else if (toType.equals(BaseType.STRING, false)) {
        return new String[] {"String.valueOf(" + value, ")"};

      } else if (toType.equals(BaseType.BOOLEAN_PRIMITIVE, false)) {
        if (fromType.equals(BaseType.STRING, false)) {
          return new String[] {"Boolean.valueOf(" + value, ")"};
        } else {
          return new String[] {"Boolean.valueOf(String.valueOf(" + value, "))"};
        }
      } else if (toType.equals(BaseType.BOOLEAN_OBJECT, false)) {
        return new String[] {"com.gip.xyna.xprc.xfractwfe.formula.Functions.fparsebooleanornull(" + value, ")"};
      } else if (toType.equals(BaseType.INT_PRIMITIVE, false)) {
        if (fromType.equals(BaseType.INT_PRIMITIVE, false)) {
          return new String[] {value, ""};
        }
        if (fromType.isAnyNumber() && fromType.isPrimitive()) {
          return new String[] {"((int)(" + value, "))"};
        }
        if (fromType.equals(BaseType.STRING, false)) {
          return new String[] {"Integer.parseInt(" + value, ")"};
        } else {
          return new String[] {"Integer.parseInt(String.valueOf(" + value, "))"};
        }
      } else if (toType.equals(BaseType.INT_OBJECT, false)) {
        if (fromType.equals(BaseType.INT_PRIMITIVE, false)) {
          return new String[] {value, ""};
        }
        if (fromType.isAnyNumber() && fromType.isPrimitive()) {
          return new String[] {"((int)(" + value, "))"};
        }
        return new String[] {"com.gip.xyna.xprc.xfractwfe.formula.Functions.fparseintegerornull(" + value, ")"};
      } else if (toType.equals(BaseType.LONG_PRIMITIVE, false)) {
        if (fromType.isAnyNumber() && fromType.isPrimitive()) {
          return new String[] {"((long)(" + value, "))"};
        }
        if (fromType.equals(BaseType.STRING, false)) {
          return new String[] {"Long.parseLong(" + value, ")"};
        } else {
          return new String[] {"Long.parseLong(String.valueOf(" + value, "))"};
        }
      } else if (toType.equals(BaseType.LONG_OBJECT, false)) {
        if (fromType.isAnyNumber() && fromType.isPrimitive()) {
          return new String[] {"((long)(" + value, "))"};
        }
        return new String[] {"com.gip.xyna.xprc.xfractwfe.formula.Functions.fparselongrnull(" + value, ")"};
      } else if (toType.equals(BaseType.DOUBLE_PRIMITIVE, false)) {
        if (fromType.isAnyNumber() && fromType.isPrimitive()) {
          return new String[] {"((double)(" + value, "))"};
        }
        if (fromType.equals(BaseType.STRING, false)) {
          return new String[] {"Double.parseDouble(" + value, ")"};
        } else {
          return new String[] {"Double.parseDouble(String.valueOf(" + value, "))"};
        }
      } else if (toType.equals(BaseType.DOUBLE_OBJECT, false)) {
        if (fromType.isAnyNumber() && fromType.isPrimitive()) {
          return new String[] {"((double)(" + value, "))"};
        }
        return new String[] {"com.gip.xyna.xprc.xfractwfe.formula.Functions.fparsedoubleornull(" + value, ")"};
      } else if (toType.equals(BaseType.FLOAT_PRIMITIVE, false)) {
        if (fromType.isAnyNumber() && fromType.isPrimitive()) {
          return new String[] {"((float)(" + value, "))"};
        }
        if (fromType.equals(BaseType.STRING, false)) {
          return new String[] {"Float.parseFloat(" + value, ")"};
        } else {
          return new String[] {"Float.parseFloat(String.valueOf(" + value, "))"};
        }
      } else if (toType.equals(BaseType.FLOAT_OBJECT, false)) {
        if (fromType.isAnyNumber() && fromType.isPrimitive()) {
          return new String[] {"((float)(" + value, "))"};
        }
        return new String[] {"com.gip.xyna.xprc.xfractwfe.formula.Functions.fparsefloatnull(" + value, ")"};
      } else if (toType.isAnyNumber()) {
        if (fromType.isAnyNumber()) {
          return new String[] {value, ""};
        }
        if (fromType.equals(BaseType.STRING, false)) {
          return new String[] {"Double.parseDouble(" + value, ")"};
        } else {
          return new String[] {"Double.parseDouble(String.valueOf(" + value, "))"};
        }
      } else if (toType.isList()) {
        //FIXME hier die information verarbeiten, welchen typ die listenelemente haben müssen.       
        return new String[] {"new ArrayList(Arrays.asList(new Object[]{" + value, "}))"};
      } else if (toType.isAny() && fromType.isModelledType()) {
        return new String[] {value, ""};
      } else if (toType.isUnknown()) {
        return new String[] {value, ""};
      } else {
        throw new RuntimeException("Unsupported type transformation: from " + fromType.getJavaName() + " to " + toType.getJavaName());
      }
    }


    public void variablePartStarts(VariableAccessPart p, boolean lastPart) {
      super.variablePartStarts(p);
      if (p.getName() == null ||
          p.getName().trim().length() == 0) {
        return;
      }
      if (p.isMemberVariableAccess()) {
        if (currentVariableIsPath()) {
          getSB().append(".").append(p.getName());
          if (p.getIndexDef() != null) {
            getSB().append("[\\\"\" + ");
          }
        } else {
          if (lastPart &&
              generateTypeResistantMappingCode.get() &&
              p.getIndexDef() == null &&
              !contextStack.empty() &&
              adjustments.contains(contextStack.peek())) {
            getSB().append(".get(\"").append(p.getName()).append("\")");
          } else {
            getSB().append(".").append(GenerationBase.buildGetter(p.getName())).append("()");
          }
        }
      }
    }

    private Stack<Boolean> isPathVariable = new Stack<Boolean>();

    private boolean currentVariableIsPath() {
      if (isPathVariable.isEmpty()) {
        return false;
      } else {
        return isPathVariable.peek();
      }
    }

    public void allPartsOfVariableFinished(Variable variable) {
      if (isPathVariable.pop()) {
        //ersten "." wieder entfernen
        getSB().append("\").substring(1), \"").append(((VariableInfoPathMap)variable.getBaseVariable()).getDataModel()).append("\")");
        
        String path = ((VariableInfoPathMap)variable.getBaseVariable()).getLocalPathOfValue();
        String[] parts = path.split("\\.");
        for (int i = 0; i<parts.length; i++) {
          getSB().append(".").append(GenerationBase.buildGetter(parts[i])).append("()");
        }
      }
      try {
        if (generateTypeResistantMappingCode.get()) {
          if (((StepBasedVariable)variable.getFollowedVariable()).getAVariable().isFunctionResult()) {
            getSB().append(transformation("", variable.getTypeOfExpression(), variable.getTargetType())[1]);
          } else if (isContext(SingleVarExpression.class)) {
            generateAdjustmentSuffix(getContext(SingleVarExpression.class), variable.getTargetType());
          }
        } else {
          getSB().append(transformation("", variable.getTypeOfExpression(), variable.getTargetType())[1]);
        }
      } catch (XPRC_InvalidVariableMemberNameException e) {
        throw new RuntimeException(e);
      }
      super.allPartsOfVariableFinished(variable);
    }

    
    public void allPartsOfFunctionFinished(FunctionExpression fe) {
      //ifs genauso wie in functionStarts
      if (isNew(fe)) {
      } else {
        if (fe.getJavaCode().equals("null")) {
        } else {
          generateAdjustmentSuffix(fe, fe.getTargetType());
        }
      }
      super.allPartsOfFunctionFinished(fe);
    }


    public void functionSubExpressionEnds(FunctionExpression fe, int parameterCnt) {
      if (generateTypeResistantMappingCode.get()) {
        Expression subExpression = fe.getSubExpressions().get(parameterCnt);
        if (!(subExpression instanceof SingleVarExpression) &&
            !(subExpression instanceof FunctionExpression)) { 
          generateAdjustmentSuffix(fe.getSubExpressions().get(parameterCnt), fe.getParameterTypeDef(parameterCnt));
        }
      }
      super.functionSubExpressionEnds(fe, parameterCnt);
    }
    
    
    @Override
    public void functionAllSubExpressionsEnds(FunctionExpression fe) {
      //ifs genauso wie in functionStarts
      if (isNew(fe)) {
      } else {
        if (fe.getJavaCode().equals("null")) {
        } else {
          getSB().append(")"); //schliessende klammer von funktionsaufruf, also z.b. replaceAll(... -> )
        }
      }
      super.functionAllSubExpressionsEnds(fe);
    }


    public void functionSubExpressionStarts(FunctionExpression fe, int parameterCnt) {
      super.functionSubExpressionStarts(fe, parameterCnt);
      if (parameterCnt > 0) {
        getSB().append(", ");
      }
      if (generateTypeResistantMappingCode.get()) {
        Expression subExpression = fe.getSubExpressions().get(parameterCnt);
        if (!(subExpression instanceof SingleVarExpression) && // SingleVars & 
            !(subExpression instanceof FunctionExpression)) { // FunctionExpressions adjust on their own
          generateAdjustmentPrefix(subExpression, fe.getParameterTypeDef(parameterCnt));
        }
      }
    }
  
    public void functionStarts(FunctionExpression fe) {
      super.functionStarts(fe);
      if (isNew(fe)) {
        String constructor;
        try {
          constructor = fe.getOriginalType().getModelledType().generateEmptyConstructor();
        } catch (XPRC_InvalidPackageNameException e) {
          throw new IllegalArgumentException(e);
        } catch (XPRC_InvalidVariableMemberNameException e) {
          throw new RuntimeException(e);
        }
        getSB().append(constructor);
        skipNextLiteral = true;
      } else {
        if (isCast(fe)) {
          transformNextLiteralToClass = true;
        } 
        if (fe.getJavaCode().equals("null")) {
          getSB().append("null");
        } else {
          generateAdjustmentPrefix(fe, fe.getTargetType());
          getSB().append(fe.getJavaCode()).append("(");
        }
      }
    }


    public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi) {
      super.instanceFunctionStarts(vifi);
      getSB().append(".").append(vifi.getName()).append("(");
      if (vifi.requiresXynaOrder()) {
        getSB().append(StepFunction.METHODNAME_GET_PROCESS + "()." + WF.METHODNAME_GET_CORRELATED_XYNA_ORDER + "()");
      }
    }

    
    public void instanceFunctionEnds(VariableInstanceFunctionIncovation vifi) {
      getSB().append(")");
      super.instanceFunctionEnds(vifi);
    }

    public void instanceFunctionSubExpressionStarts(VariableInstanceFunctionIncovation vifi, int parameterCnt) {
      super.instanceFunctionSubExpressionStarts(vifi, parameterCnt);
      if (parameterCnt > 0) {
        getSB().append(", ");
      }
      if (generateTypeResistantMappingCode.get()) {
        if (!(vifi.getFunctionParameter().get(parameterCnt) instanceof SingleVarExpression)) {
          generateAdjustmentPrefix(vifi.getFunctionParameter().get(parameterCnt), vifi.getInputParameterTypes().get(parameterCnt));
        }
      }
    }
    

    public void instanceFunctionSubExpressionEnds(VariableInstanceFunctionIncovation vifi, int parameterCnt) {
      if (generateTypeResistantMappingCode.get()) {
        if (!(vifi.getFunctionParameter().get(parameterCnt) instanceof SingleVarExpression)) {
          generateAdjustmentSuffix(vifi.getFunctionParameter().get(parameterCnt), vifi.getInputParameterTypes().get(parameterCnt));
        }
      }
      super.instanceFunctionSubExpressionEnds(vifi, parameterCnt);
    }
    
    
    public void indexDefStarts(Expression exp) {
      super.indexDefStarts(exp);
      getSB().append(".get(");
      if (generateTypeResistantMappingCode.get()) {
        if (!(exp instanceof SingleVarExpression)) {
          generateAdjustmentPrefix(exp, new TypeInfo(BaseType.INT_PRIMITIVE));
        }
      }
    }
    
    
    public void indexDefEnds(Expression exp) {
      if (currentVariableIsPath()) {
        getSB().append(" + \"\\\"]");
      } else {
        if (generateTypeResistantMappingCode.get()) {
          if (!(exp instanceof SingleVarExpression)) {
            generateAdjustmentSuffix(exp, new TypeInfo(BaseType.INT_PRIMITIVE));
          }
        }
        getSB().append(")");
      }
      super.indexDefEnds(exp);
    }


    private void generateAdjustmentPrefix(Expression exp, TypeInfo typeInfo) {
      if (!typeInfo.isBaseType() &&
          typeInfo.isAnyNumber()) {
        typeInfo = new TypeInfo(PrimitiveType.DOUBLE_OBJ);
      }
      if (typeInfo.isBaseType() ||
          typeInfo.isUnknown()) {
        if (qualifiesForDynamicAdjustment(exp)) {
          getSB().append(transformation_prefix(typeInfo));
          adjustments.add(exp);
        } else {
          try {
            getSB().append(transformation("", exp.getOriginalType(), typeInfo)[0]);
          } catch (XPRC_InvalidVariableMemberNameException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    
    
    private void generateAdjustmentSuffix(Expression exp, TypeInfo typeInfo) {
      if (!typeInfo.isBaseType() &&
          typeInfo.isAnyNumber()) {
        typeInfo = new TypeInfo(PrimitiveType.DOUBLE_OBJ);
      }
      if (typeInfo.isBaseType() || 
          typeInfo.isAnyNumber() ||
          typeInfo.isUnknown()) {
        if (qualifiesForDynamicAdjustment(exp) && adjustments.contains(exp)) {
          getSB().append(")");
        } else {
          try {
            getSB().append(transformation("", exp.getOriginalType(), typeInfo)[1]);
          } catch (XPRC_InvalidVariableMemberNameException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    
    private boolean qualifiesForDynamicAdjustment(Expression expression) {
      if (expression instanceof CastExpression) {
        return false;
      } else if (expression instanceof FunctionExpression) {
        return ((FunctionExpression)expression).getParts().size() > 0;
      } else if (expression instanceof LiteralExpression) {
        return false;
      }
      return true;
    }
    
  }


  public static ModelledExpression parse(Step step, String expression) throws XPRC_ParsingModelledExpressionException {
    return ModelledExpression.parse(new StepBasedIdentification(step), expression);
  }
  
  public static ModelledExpression parse(VariableContextIdentification vci, String expression) throws XPRC_ParsingModelledExpressionException {
    return parse(vci, expression, null);
  }
  
  public static ModelledExpression parse(VariableContextIdentification vci, String expression, SupportedFunctionStore functionStore) throws XPRC_ParsingModelledExpressionException {
    try {
      expression = XFLLexer.lexemStreamToString(XFLOperatorPrecedenceAdjuster.handleOperatorPrecedence(XFLLexer.lex(expression)));
    } catch (Exception e) {
      //fehler passiert beim parsen erneut!
      if (logger.isTraceEnabled()) {
        logger.trace("Failed to apply operation precedence adjustments, modelled expression '" + expression + "' might not perform as expected", e);
      }
    }
    ModelledExpression formula = new ModelledExpression(vci, expression);
    formula.parse(new Parser(formula, functionStore));
    return formula;
  }


  private void parse(Parser parser) throws XPRC_ParsingModelledExpressionException {
    Expression e1 = parser.parseExpression(0, false);
    Expression e2 = null;
    if (foundAssign != null) {
      e2 = e1;
      e1 = parser.parseExpression(e1.getLastIdx() + 1, false);
    }
    while (e1.getLastIdx() < expression.length() - 1) {
      int lastIdx = e1.getLastIdx();
      Expression temp = parser.parseSecondPartOfExpressionIfExisting(e1.getLastIdx() + 1, e1);
      if (temp.getLastIdx() == lastIdx) {
        //endlosschleife verhindern
        throw new XPRC_ParsingModelledExpressionException(expression, e1.getLastIdx() + 2);
      }
      e1 = temp;
    }
    if (e2 == null) {
      parsedTargetExpression = e1;
      parsedSourceExpression = null;
    } else {
      parsedTargetExpression = e2;
      wrapListAssignment(ExtractionReason.TARGET_LIST_INITIALIZATION);
      parsedSourceExpression = e1;
    }
  }

  
  private void wrapListAssignment(ExtractionReason extractionReason) {
    TargetListAccessWrappingVisitor tlawv = new TargetListAccessWrappingVisitor(extractionReason);
    visitTargetExpression(tlawv);
    exprVars = tlawv.getLocals();
  }
  
  
  private static class TargetListAccessWrappingVisitor extends EmptyVisitor {
    
    private Stack<Object> contextStack = new Stack<Object>();
    private List<LocalExpressionVariable> locals = new ArrayList<LocalExpressionVariable>();
    private ExtractionReason extractionReason;
    
    
    public TargetListAccessWrappingVisitor(ExtractionReason extractionReason) {
      this.extractionReason = extractionReason;
    }
    
    public void allPartsOfVariableFinished(Variable variable) { 
      contextStack.pop();
    }

    public void variableStarts(Variable variable) {
      if (variable.getIndexDef() != null && 
          !(variable.getIndexDef() instanceof LiteralExpression)) {
        String uniqueSuffix = String.valueOf(variable.getFirstIdx());
        LocalExpressionVariable lev = new LocalExpressionVariable(variable.getIndexDef(), uniqueSuffix, extractionReason);
        variable.setIndexDef(lev);
        locals.add(lev);
      }
      contextStack.push(variable);
    }


    public void variablePartStarts(VariableAccessPart part) {
      if (part.getIndexDef() != null && 
          !(part.getIndexDef() instanceof LiteralExpression)) {
        Object ctx = contextStack.peek();
        if (ctx instanceof Variable) {
          Variable var = (Variable) ctx;
          String uniqueSuffix = String.valueOf(var.getFirstIdx());
          int partIndex = var.getParts().indexOf(part);
          if (partIndex >= 0) {
            uniqueSuffix += "_" + var.getParts().get(partIndex).getFirstIdx();
          }
          LocalExpressionVariable lev = new LocalExpressionVariable(part.getIndexDef(), uniqueSuffix, partIndex, extractionReason);
          part.setIndexDef(lev);
          locals.add(lev);          
        } else if (ctx instanceof CastExpression) {
          //TODO offenbar sollten castexpression und variable ein gemeinsames interface implementieren, wenn die verwendung hier so ähnlich ist!
          CastExpression cast = (CastExpression) ctx;
          String uniqueSuffix = String.valueOf(cast.getFirstIdx());
          int partIndex = cast.getPartIndex(part);
          if (partIndex >= 0) {
            uniqueSuffix += "_" + cast.getParts().get(partIndex).getFirstIdx();
          }
          LocalExpressionVariable lev = new LocalExpressionVariable(part.getIndexDef(), uniqueSuffix, partIndex, extractionReason);
          part.setIndexDef(lev);
          locals.add(lev);       
        } else {
          //new könnte theoretisch auch indexdef haben, macht aber wenig sinn
          throw new RuntimeException("Unsupported index def at " + ctx);
        }
        
      }
    }


    @Override
    public void allPartsOfFunctionFinished(FunctionExpression fe) {
      contextStack.pop();
    }

    @Override
    public void functionStarts(FunctionExpression fe) {
      if (fe.getIndexDef() != null &&
          !(fe.getIndexDef() instanceof LiteralExpression)) {
        if (fe instanceof CastExpression) {
          CastExpression cast = (CastExpression) fe;
          String uniqueSuffix = String.valueOf(cast.getFirstIdx());
          LocalExpressionVariable lev = new LocalExpressionVariable(fe.getIndexDef(), uniqueSuffix, extractionReason);
          cast.setIndexDef(lev);
          locals.add(lev);      
        } else {
          throw new RuntimeException("Unsupported index def at " + fe);
        }
      }
        
      contextStack.push(fe);
    }

    public List<LocalExpressionVariable> getLocals() {
      return locals;
    }

  }

  public void writeNonAssignmentExpressionToBuffer(CodeBuffer cb) throws XPRC_ParsingModelledExpressionException {
    if (parsedSourceExpression == null) {
      JavaCodeGeneratorVisitor jv = new JavaCodeGeneratorVisitor();
      parsedTargetExpression.visit(jv);
      cb.add(jv.getSB().toString().trim());
    } else {
      throw new XPRC_ParsingModelledExpressionException(expression, parsedTargetExpression == null ? -1 : parsedTargetExpression.getLastIdx() + 1,
                                                        new RuntimeException("There is no assignment allowed in this expression."));
    }
  }


  private static List<TypeInfo> numberTypesInOrder = Arrays.asList(new TypeInfo[] {TypeInfo.ANYNUMBER,
                  new TypeInfo(BaseType.DOUBLE_PRIMITIVE), new TypeInfo(BaseType.FLOAT_PRIMITIVE),
                  new TypeInfo(BaseType.LONG_PRIMITIVE), new TypeInfo(BaseType.INT_PRIMITIVE)});


  private static TypeInfo calcResultingNumberType(TypeInfo typeInfo, TypeInfo typeInfo2) {
    if (typeInfo.equals(typeInfo2) && typeInfo.isAnyNumber()) {
      return typeInfo;
    }
    int i1 = 0;
    int i2 = 0;
    for (int i = 0; i < numberTypesInOrder.size(); i++) {
      if (numberTypesInOrder.get(i).equals(typeInfo, true)) {
        i1 = i;
      }
      if (numberTypesInOrder.get(i).equals(typeInfo2, true)) {
        i2 = i;
      }
    }
    return numberTypesInOrder.get(Math.min(i1, i2));
  }
  
  
  static boolean isNumber(String value) {
    //ohne exception zu werfen checken
    if (value == null || value.length() == 0) {
      return false;
    }
    for (int i = 0; i<value.length(); i++) {
      char c = value.charAt(i);
      if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E'  || c == '-' || c == '+') {
        continue;
      } else {
        return false;
      }
    }
    
    try {
      Double.valueOf(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }


  static boolean isLong(String value) {
    if (!fastCheckInt(value)) {
      return false;
    }
    try {
      Long.valueOf(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }


  private static boolean fastCheckInt(String value) {
    if (value == null || value.length() == 0) {
      return false;
    }
    char c = value.charAt(0);
    if ((c >= '0' && c <= '9') || c == '+' || c == '-') {
      
    } else {
      return false;
    }
    for (int i = 1; i<value.length(); i++) {
      c = value.charAt(i);
      if ((c >= '0' && c <= '9')) {
        continue;
      } else {
        return false;
      }
    }
    return true;
  }

  private static boolean isDouble(String value) {
    return isNumber(value);
  }


  private static boolean isInt(String value) {
    if (!fastCheckInt(value)) {
      return false;
    }
    try {
      Integer.valueOf(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
  
  
  public TypeInfo getTargetType() throws XPRC_InvalidVariableMemberNameException {
    if (parsedSourceExpression == null) {
      return new TypeInfo(BaseType.BOOLEAN_PRIMITIVE);
    } else if (parsedTargetExpression instanceof FollowableType) {
      FollowableType sve = (FollowableType) parsedTargetExpression;
      return sve.getTypeOfExpression();
    } else if (parsedTargetExpression instanceof DynamicResultTypExpression) {
      DynamicResultTypExpression drte = (DynamicResultTypExpression) parsedTargetExpression;
      return drte.getResultType();
    } else {
      return null;
    }
  }
  
  public TypeInfo getSourceType() throws XPRC_InvalidVariableMemberNameException {
    if (parsedSourceExpression == null) {
      return null;
    } else if (parsedSourceExpression instanceof FollowableType) {
      FollowableType sve = (FollowableType) parsedSourceExpression;
      return sve.getTypeOfExpression();
    } else if (parsedSourceExpression instanceof DynamicResultTypExpression) {
      DynamicResultTypExpression drte = (DynamicResultTypExpression) parsedSourceExpression;
      return drte.getResultType();
    } else {
      return null;
    }
  }


  /*
   * key kann sein:
   * 1. variable id
   * 2. pathname
   * 3. falls parent listenwertig, steht hier ein index. 
   *    falls der index dynamisch ist, steht hier einfach nur "d", ansonsten der index als zahl
   * 
   * 5.list.3.value 
   * entsprich dem value des 3ten listenelements der variable 5, also
   * var5.getList().get(3).getValue()
   */
  public static class MapTree {

    private Map<String, MapTree> map;
    private boolean generated = false;


    public MapTree() {
      map = new HashMap<String, MapTree>();
    }


    public Map<String, MapTree> getMap() {
      return map;
    }


    public String toString() {
      return (generated? "+" : "") + map.toString();
    }
  }


  /**
   * bestimmt die typen aller teil-ausdrücke und befüllt den maptree mit informationen zu den variablen-zugriffen des
   * ziel ausdrucks.
   * damit variablen-teile nicht überschrieben werden, muss für alle an einem mapping beteiligten einzelmappings 
   * die gleiche maptree an diese methode übergeben werden.
   * 
   * erläuterung: damit beim befüllen des ziel types keine NPEs zur laufzeit passieren, wird der code generiert,
   * der sicherstellt, dass entsprechende objekte erstellt werden. wenn man in verschiedenen einzelmappings auf
   * gleiche pfade in der variablen hierarchie zugreift, sollen die entsprechenden objekte aber nur einmal
   * erstellt werden.
   * 
   * @param importedClassNames
   * @param varPathsAlreadyCreated enthält informationen zu allen variablen-teilen des target-types, und ob diese
   *   bei der codegenerierung noch erstellt werden müssen, oder ob sie bereits vorhanden sind.
   */
  public void initTypesOfParsedFormula(Set<String> importedClassNames, MapTree varPathsAlreadyCreated)
      throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException,
      XPRC_ParsingModelledExpressionException {
    InitVariablesVisitor iv = new InitVariablesVisitor();
    parsedTargetExpression.visit(iv);
    if (parsedSourceExpression != null) {
      parsedSourceExpression.visit(iv);
    }
    iv.rethrowException();
    parsedTargetExpression.visit(new InferOriginalTypeVisitor());
    parsedTargetExpression.visit(new MakeTypeDefinitionConsistentVisitor());
    TypeInfo type = parsedTargetExpression.getOriginalType();
    if (parsedSourceExpression != null) {
      parsedSourceExpression.setTargetType(type);
      parsedSourceExpression.visit(new InferOriginalTypeVisitor());
      parsedSourceExpression.visit(new MakeTypeDefinitionConsistentVisitor());
      if (parsedTargetExpression instanceof FollowableType) {
        checkCreationOfVariable(varPathsAlreadyCreated, (FollowableType)parsedTargetExpression);
      } else {
        throw new XPRC_ParsingModelledExpressionException(expression, 1,
                                                          new RuntimeException("Target expression must be a variable."));        
      }
    }
  }

  /*
   * Beispiel:
   * cast(cast(%1%).x[1].y.z[2]).a.b[1].c
   * MapTree soll folgenden Pfad enthalten: 1->x->1->y->z->2->a->b->1->c
   */
  private void checkCreationOfVariable(MapTree varPathsAlreadyCreated, FollowableType sve) throws XPRC_ParsingModelledExpressionException {
    if (sve.isPathMap()) {
      return;
    }

    //um auf index-def von basisvariable zugreifen zu können, müssen cast-funktionen ausgepackt werden 
    FollowableType ft = sve;
    while (ft instanceof CastExpression) {
      ft = (FollowableType) ((CastExpression) ft).getWrappedAccessPath();
    }

    varPathsAlreadyCreated = getMapTreeChild(varPathsAlreadyCreated, String.valueOf(sve.getVarNum()), ft.getIndexDef());

    for (int accessPathIdx = 0; accessPathIdx < sve.getAccessPathLength(); accessPathIdx++) {
      VariableAccessPart vap = sve.getAccessPart(accessPathIdx);
      varPathsAlreadyCreated = getMapTreeChild(varPathsAlreadyCreated, vap.getName(), vap.getIndexDef());
    }
  }


  private MapTree getMapTreeChild(MapTree varPathsAlreadyCreated, String key, Expression indexDef) throws XPRC_ParsingModelledExpressionException {
    MapTree child = varPathsAlreadyCreated.map.get(key);
    if (child == null) {
      child = new MapTree();
      varPathsAlreadyCreated.map.put(key, child);
    }

    if (indexDef != null) {
      if (indexDef instanceof LiteralExpression) {
        key = ((LiteralExpression) indexDef).getValue();
      } else if (indexDef instanceof LocalExpressionVariable) {
        key = "d"; //dynamic
      } else {
        throw new XPRC_ParsingModelledExpressionException(
                                                          expression,
                                                          indexDef.getFirstIdx() + 1,
                                                          new RuntimeException(
                                                                               "Only literal or local expressions are supported in array index definition of target variable."));
      }
      varPathsAlreadyCreated = child;
      child = varPathsAlreadyCreated.map.get(key);
      if (child == null) {
        child = new MapTree();
        varPathsAlreadyCreated.map.put(key, child);
      }
    }
    return child;
  }
  

  public void writeAssignmentExpressionToCodeBuffer(CodeBuffer cb, Set<String> importedClassNames, long uniqueId,
                                                    MapTree varPathsForVariables, boolean isTemplate) throws XPRC_ParsingModelledExpressionException,
      XPRC_InvalidVariableMemberNameException {
    if (!(parsedTargetExpression instanceof FollowableType)) {
      throw new RuntimeException("Target expression must be a variable.");
    }
    FollowableType sve = (FollowableType) parsedTargetExpression;
    boolean useDeepClones = foundAssign != null && foundAssign == Assign.DEEP_CLONE;
    cb.addLine("// " + expression);
    TypeInfo type = parsedTargetExpression.getOriginalType();
    JavaCodeGeneratorVisitor jv = new JavaCodeGeneratorVisitor();
    if (parsedSourceExpression != null) {
      parsedSourceExpression.visit(jv);
    }
    String code = jv.getSB().toString();

    // local vars
    if (sve.isPathMap()) {
      wrapListAssignment(ExtractionReason.PATH_MAP_INDEX);
    }
    
    for (LocalExpressionVariable exprVar : exprVars) {
      JavaCodeGeneratorVisitor jv2 = new JavaCodeGeneratorVisitor();
      jv2.generateAdjustmentPrefix(exprVar.getExpression(), exprVar.getExpression().getTargetType());
      exprVar.getExpression().visit(jv2);
      jv2.generateAdjustmentSuffix(exprVar.getExpression(), exprVar.getExpression().getTargetType());
      String varName = exprVar.getUniqueVariableName(uniqueId);
      cb.addLine(exprVar.getExpression().getTargetType().getJavaName(), " ",  varName, " = ", jv2.getSB().toString());
      switch (exprVar.getExtractionReason()) {
        case TARGET_LIST_INITIALIZATION :
          createList(cb, sve, varName, exprVar.getPartIndex(), uniqueId);
          break;
        default :
          break;
      }
    }
    
    //lazy objekt erstellung, falls in map vorhanden (ansonsten wurde es bereits erstellt)
    String key = String.valueOf(sve.getVarNum());
    MapTree var = varPathsForVariables.map.get(key);
    if (var != null && var.map.size() > 0) {
      //create empty object for children of level -1
      createEmptyChildObjects(cb, sve, -1, var, importedClassNames, uniqueId);
      cb.addLine();
    } else {
      //default konstruktor genügt
    }

    // for typeless mappings
    boolean needSetterInvocation = true;
    
    //temporäre variable anlegen, die später über den setter dem target zugewiesen wird
    String uniqueVarName = TEMP_VARIABLE_PREFIX + uniqueId;
    if (type.isList()) {
      //FIXME bessere sonderbehandlung von null?
      if (parsedSourceExpression.getOriginalType().isNull()) {
        cb.addLine("List ", uniqueVarName, " = null");
      } else {
        TypeInfo typeInfo = sve.getFollowedVariable().getTypeInfo(true);
        boolean cloneObjects = useDeepClones && !typeInfo.isBaseType();
        cb.addLine("List ", uniqueVarName);
        cb.addLine("List ", uniqueVarName, "_1 = ", code);
        cb.addLine("if (", uniqueVarName, "_1 == null) {");
        cb.addLine(uniqueVarName, " = null");
        cb.addLine("} else {");
        cb.addLine(uniqueVarName, " = new ArrayList(", (cloneObjects ? ")" : uniqueVarName + "_1)"));
        if (cloneObjects) {
          //eigtl müsste man für o hier die typinformation der liste in code nehmen, da kommt man aber nicht gut dran. so funktioniert es auch...

          cb.addLine("for (", Object.class.getSimpleName(), " o : ", uniqueVarName, "_1) {");
          cb.addLine("if (o == null) {");
          cb.addLine(uniqueVarName, ".add(null)");
          cb.addLine("} else {");
          cb.addLine(uniqueVarName, ".add(((", GeneralXynaObject.class.getName(), ") o).clone())");
          cb.addLine("}"); //else
          cb.addLine("}"); //for
        }
        cb.addLine("}"); //else
      }
    } else {
      if (parsedSourceExpression == null) {
        if (sve.isPathMap()) {
          VariableInfoPathMap vipm = (VariableInfoPathMap) sve.getFollowedVariable();
          cb.add(vipm.getJavaCodeForVariableAccess());
          String setter = vipm.getPathSetter(); //addPath oder setPath
          cb.add(setter, "(\"", vipm.getPath(uniqueId), "\", \"", vipm.getDataModel(), "\")");
          cb.addLB();
        } else {
          throw new RuntimeException("source expression missing");
        }
      } else if (parsedSourceExpression.getOriginalType().isNull()) {
        cb.addLine(type.getJavaName(), " ", uniqueVarName, " = null");
      } else if (sve.isPathMap()) {
        VariableInfoPathMap vipm = (VariableInfoPathMap) sve.getFollowedVariable();
        cb.add(vipm.getJavaCodeForVariableAccess());
        //TODO mehr als eine value- variable unterstützen?
        String setter = GenerationBase.buildSetter(vipm.getVarNameOfValue());
        cb.add(setter, "InMap(\"", vipm.getPath(uniqueId), "\", \"", vipm.getDataModel(), "\", ", code, ")");
        cb.addLB();
      } else if (sve.getFollowedVariable().getTypeInfo(true).isBaseType()) { // native type, true ist hier richtig, weil wir wissen, dass target keine liste ist
        if (!isTemplate && 
            generateTypeResistantMappingCode.get()) {
          needSetterInvocation = false;
          VariableInfo target_vi = sve.follow(sve.getAccessPathLength() - 2);
          VariableAccessPart targetLastPart = sve.getAccessPart(sve.getAccessPathLength() - 1);
          if (targetLastPart.getIndexDef() == null) { // && notSetterOfWholeList?
            cb.addLine(sve.toJavaCodeGetter(sve.getAccessPathLength() - 2, true, uniqueId),
                       ".set(",
                         "\"", targetLastPart.getName(), "\", ",
                         Functions.class.getName(), ".", Functions.ADJUST_VALUE_METHOD_NAME, "(",
                           target_vi.getTypeInfo(true).getJavaName(), ".", XynaObjectCodeGenerator.FIELD_GETTER_METHOD_NAME, "(\"", targetLastPart.getName(), "\").getType(), ",
                           code,
                          ")",
                        ")"
                        );
          } else {
            cb.add("((List)", sve.toJavaCodeGetter(sve.getAccessPathLength() - 1, false, uniqueId), ").set(");
            if (targetLastPart.getIndexDef() instanceof LiteralExpression) {
              cb.add(((LiteralExpression) targetLastPart.getIndexDef()).getValue());
            } else if (targetLastPart.getIndexDef() instanceof LocalExpressionVariable) {
              cb.add(((LocalExpressionVariable) targetLastPart.getIndexDef()).getUniqueVariableName(uniqueId));
            } else {
              throw new RuntimeException();
            }
            cb.add(", ", code, ")");
            cb.addLB();
          }
        } else {
          cb.addLine(type.getJavaName(), " ", uniqueVarName, " = ", code);
        }
      } else {
        TypeInfo targetType = null;
        if (parsedSourceExpression instanceof SingleVarExpression) {
          Variable sourceVar = ((SingleVarExpression) parsedSourceExpression).getVar();
          targetType = sourceVar.getTargetType();
        } else if (parsedSourceExpression instanceof FunctionExpression) {
          targetType = ((FunctionExpression)parsedSourceExpression).getTargetType();
        } else {
          throw new RuntimeException("unexpected case: source expression is of complex type but not a variable");
        }
        cb.addLine(targetType.getJavaName(), " ", uniqueVarName, " = (", targetType.getJavaName(), ")", code);
        cb.addLine("if (", uniqueVarName, " != null) {");
        String className;
        if (importedClassNames.contains(targetType.getModelledType().getFqClassName())) {
          className = targetType.getModelledType().getSimpleClassName();
        } else {
          className = targetType.getModelledType().getFqClassName();
        }
        cb.addLine(uniqueVarName, " = (", className, ") ", uniqueVarName, ".clone(", String.valueOf(useDeepClones) ,")");
        cb.addLine("}");
      }
    }
    
    //FIXME visitor pattern verwenden
    if (needSetterInvocation &&
        !sve.isPathMap()) {
      int depth = sve.getAccessPathLength() - 1;
      cb.add(sve.toJavaCodeSetter(depth, !sve.lastPartOfVariableHasListAccess(), uniqueVarName, uniqueId));
    }
  }
  
  public static XynaPropertyBoolean generateTypeResistantMappingCode = new XynaPropertyBoolean("xprc.xfractwfe.different.typeresistant", true);
  
  /**
   * 
   * @param cb
   * @param sve
   * @param pathDepth
   * @param varPathsForVariables kinder davon sind kinder von der zugehörigen variable (entsprechend dem path),
   * d.h. bei depth == -1, wird hier die teilmap der basisvariable übergeben
   * bei depth == 0 wird hier die teilmap %0% bzw %0%[x] (falls listenwertig) übergeben.
   */
  private void createEmptyChildObjects(CodeBuffer cb, FollowableType sve, int pathDepth,
                                       MapTree varPathsForVariables, Set<String> importedClassNames, long unqiueId) throws XPRC_InvalidVariableMemberNameException {

    if (varPathsForVariables == null) {
      return;
    }

    VariableInfo varInfo = sve.follow(pathDepth);
    if (varInfo.getTypeInfo(false).isList()) {
      
      String listAccess = null; //gibt es auf die liste einen zugriff der art [expr]
      if (pathDepth == -1) {
        //root-indexdef ermitteln
        FollowableType ft = sve;
        while (ft instanceof CastExpression) {
          ft = (FollowableType) ((CastExpression) ft).getWrappedAccessPath();
        }
        
        Expression indexDef = ft.getIndexDef();
        if (indexDef != null) {
          if (indexDef instanceof LiteralExpression) {
            listAccess = ((LiteralExpression) indexDef).getValue();
          } else {
            listAccess = "d";
          }
        }
      } else {
        Expression indexDef = sve.getAccessPart(pathDepth).getIndexDef();
        if (indexDef != null) {
          if (indexDef instanceof LiteralExpression) {
            listAccess = ((LiteralExpression) indexDef).getValue();
          } else {
            listAccess = "d";
          }
        }
      }

      if (listAccess == null) {
        return;
      }
      //per default ist liste leer -> mit nulls (oder richtigen objekten) befüllen 
      int maxIdx = -1;
      for (Entry<String, MapTree> entry : varPathsForVariables.map.entrySet()) {
        if (entry.getKey().equals("d")) { //dynamic
          continue;
        }
        int idx = Integer.valueOf(entry.getKey());
        if (idx > maxIdx) {
          maxIdx = idx;
        }
      }
      if (!varPathsForVariables.generated && 
          !listAccess.equals("d")) { // bei dynamischen listen ist schon vorher intialisiert worden
        createList(cb, sve, maxIdx, pathDepth, unqiueId);
        varPathsForVariables.generated = true;
      }
      varPathsForVariables = varPathsForVariables.map.get(listAccess);
    }

    if (sve.getAccessPathLength() <= pathDepth + 1) {
      return;
    }

    if (!varPathsForVariables.generated) {
      //TODO falls dynamischer listenindex verwendet wird, muss hier darauf zugegriffen werden können. leider wird 
      //     hier vorausgesetzt, dass der name der gleiche ist, wie bei der späteren codeerzeugung für die eigentliche zuweisung
      createVar(cb, sve, pathDepth, (StepBasedVariable) varInfo, importedClassNames, unqiueId);
      varPathsForVariables.generated = true;
    }
    varPathsForVariables = varPathsForVariables.map.get(sve.getAccessPart(pathDepth + 1).getName());
    createEmptyChildObjects(cb, sve, pathDepth + 1, varPathsForVariables, importedClassNames, unqiueId);
  }


  private void createVar(CodeBuffer cb, FollowableType sve, int depth, StepBasedVariable varInfo, Set<String> importedClassNames,
                         long uniqueId) throws XPRC_InvalidVariableMemberNameException {
    cb.addLine("if( ", sve.toJavaCodeGetter(depth, true, uniqueId), " == null ) {");
    TypeInfo ti = sve.follow(depth).getTypeInfo(true);
    if (ti.isModelledType() && ti.getModelledType().isAbstract()) {
      cb.addLine("throw new RuntimeException(\"abstract type " + ti.getModelledType().getFqXMLName() + " is null.\")");
    } else {
      String varName = TEMP_DEEPER_VARIABLE_PREFIX + (depth + 1) + "_" + uniqueId;
      cb.add(varInfo.getAVariable().getEventuallyQualifiedClassNameNoGenerics(importedClassNames), " ", varName, " = ");
      varInfo.getAVariable().generateConstructor(cb, importedClassNames, true);
      cb.addLine();
      cb.addLine(sve.toJavaCodeSetter(depth, false, varName, uniqueId));
    }
    cb.addLine("}");
  }


  private void createList(CodeBuffer cb, FollowableType sve, int maxIdxOfList, int depth, long uniqueId) {
    createList(cb, sve, String.valueOf(maxIdxOfList), depth, uniqueId);
  }
  
  
  private void createList(CodeBuffer cb, FollowableType sve, String maxIdxOfListLocalVariable, int depth, long uniqueId) {
    String listGetter = sve.toJavaCodeGetter(depth, false, uniqueId);
    String minListSize = maxIdxOfListLocalVariable+"+1";
    String nCopies = "new ArrayList(java.util.Collections.nCopies("+minListSize+",null))";
    cb.addLine("if( ",listGetter, " == null ) {" );
    cb.addLine(sve.toJavaCodeSetter(depth, true, nCopies, uniqueId));
    cb.addLine("} else if( ",listGetter,".size() < ",minListSize," ) {" );
    cb.addLine("List list = ", listGetter );
    cb.addLine("for( int i=list.size(); i<",minListSize,"; ++i ) list.add(null)");
    cb.addLine("}");
  }
  

  @Override
  public int hashCode() {
    return expression.hashCode();
  }


  @Override
  public boolean equals(Object obj) {
    //wird in StepMapping Array.equals verwendet
    if (obj == null) {
      return false;
    }

    if (!(obj instanceof ModelledExpression)) {
      return false;
    }
    ModelledExpression other = (ModelledExpression) obj;
    return expression.equals(other.expression);
  }


  public String getExpression() {
    return expression;
  }

  
  public boolean hasPathMapTarget() {
    if (parsedTargetExpression != null &&
        parsedTargetExpression instanceof SingleVarExpression) {
      return ((SingleVarExpression)parsedTargetExpression).getVar().isPathMap();
    } else {
      return false;
    }
  }
  
  
  public boolean hasPathMapSource() {
    if (parsedSourceExpression != null &&
        parsedSourceExpression instanceof SingleVarExpression) {
      return ((SingleVarExpression)parsedSourceExpression).getVar().isPathMap();
    } else {
      return false;
    }
  }
  

  public static class DuplicateAssignException extends Exception {

    private static final long serialVersionUID = 1L;

  }


  /**
   * @throws DuplicateAssignException falls der ausdruck bereits ein assign enthielt
   */
  public void setFoundAssign(Assign assign) throws DuplicateAssignException {
    if (foundAssign != null) {
      throw new DuplicateAssignException();
    }
    foundAssign = assign;
  }

  public Assign getFoundAssign() {
    return foundAssign;
  }

  public VariableContextIdentification getVariableContextIdentification() {
    return variableContext;
  }

  
  public void visitTargetExpression(Visitor visitor) {
    if (parsedTargetExpression != null) {
      parsedTargetExpression.visit(visitor);
    }
  }
  
  public void visitSourceExpression(Visitor visitor) {
    if (parsedSourceExpression != null) {
      parsedSourceExpression.visit(visitor);
    }
  }
  
  public static class EmptyVisitor implements Visitor {

    public void expression2ArgsStarts(Expression2Args expression) {}

    public void functionEnds(FunctionExpression fe) {}

    public void functionSubExpressionEnds(FunctionExpression fe, int parameterIndex) {}

    public void functionSubExpressionStarts(FunctionExpression fe, int parameterIndex) {}

    public void functionStarts(FunctionExpression fe) {}

    public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi) {}

    public void instanceFunctionEnds(VariableInstanceFunctionIncovation vifi) {}

    public void instanceFunctionSubExpressionEnds(Expression fe, int parameterIndex) {}

    public void instanceFunctionSubExpressionStarts(Expression fe, int parameterIndex) {}

    public void allPartsOfVariableFinished(Variable variable) {}

    public void expression2ArgsEnds(Expression2Args expression) {}

    public void literalExpression(LiteralExpression expression) {}

    public void notStarts(Not not) {}

    public void notEnds(Not not) {}

    public void operator(Operator operator) {}

    public void variableStarts(Variable variable) {}

    public void variableEnds(Variable variable) {}

    public void variablePartStarts(VariableAccessPart part) {}

    public void variablePartEnds(VariableAccessPart part) {}

    public void variablePartSubContextEnds(VariableAccessPart p) {}

    public void allPartsOfFunctionFinished(FunctionExpression fe) {}

    public void indexDefStarts() {}

    public void indexDefEnds() {}
    
  }
  
  public static class ContextAwareVisitor extends EmptyVisitor {
    
    protected final Stack<Object> contextStack;
    
    public ContextAwareVisitor() {
      contextStack = new Stack<>();
    }
    
    public boolean isContext(Class<?> clazz) {
      return clazz.isInstance(contextStack.peek());
    }
    
    public <E extends Expression> E getContext(Class<E> eClazz) {
      return eClazz.cast(contextStack.peek());
    }

    public void expression2ArgsStarts(Expression2Args expression) {
      contextStack.push(expression);
    }
    
    public void expression2ArgsEnds(Expression2Args expression) {
      contextStack.pop();
    }

    public void functionStarts(FunctionExpression fe) {
      contextStack.push(fe);
    }
    
    public void functionEnds(FunctionExpression fe) {
      contextStack.pop();
    }

    public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi) {
      contextStack.push(vifi);
    }
    
    public void instanceFunctionEnds(VariableInstanceFunctionIncovation vifi) {
      contextStack.pop();
    }
    
    public void singleVarExpressionStarts(SingleVarExpression expression) {
      contextStack.push(expression);
    }
    
    public void singleVarExpressionEnds(SingleVarExpression expression) {
      contextStack.pop();
    }

    public void indexDefStarts(Expression exp) {
      contextStack.push(exp);
    }
    
    public void indexDefEnds(Expression exp) {
      contextStack.pop();
    }

  }

  public static ModelledExpression createUnparsed(Step step, String expression) {
    return new ModelledExpression( new StepBasedIdentification(step), expression );
  }

  
}
