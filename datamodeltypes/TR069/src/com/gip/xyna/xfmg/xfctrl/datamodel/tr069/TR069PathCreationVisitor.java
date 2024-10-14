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
package com.gip.xyna.xfmg.xfctrl.datamodel.tr069;

import java.util.List;

import com.gip.xyna.xprc.xfractwfe.formula.Expression;
import com.gip.xyna.xprc.xfractwfe.formula.Expression2Args;
import com.gip.xyna.xprc.xfractwfe.formula.FunctionExpression;
import com.gip.xyna.xprc.xfractwfe.formula.LiteralExpression;
import com.gip.xyna.xprc.xfractwfe.formula.Not;
import com.gip.xyna.xprc.xfractwfe.formula.Operator;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;
import com.gip.xyna.xprc.xfractwfe.formula.VariableInstanceFunctionIncovation;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Path.PathCreationVisitor;

public class TR069PathCreationVisitor implements PathCreationVisitor {

  //TODO nicht immer stringbuilder mitbauen, wenn man ihn nicht benötigt
  private final StringBuilder sb = new StringBuilder();
  private final DOM dom;
  private DOM currentDom;
  private AVariable currentVar;
  

  public TR069PathCreationVisitor(DOM dom) {
    this.dom = dom;
  }


  public String getPath() {
    return sb.toString();
  }


  public void expression2ArgsStarts(Expression2Args expression) {
    throw new RuntimeException();
  }


  public void functionEnds(FunctionExpression fe) {
    throw new RuntimeException();
  }


  public void functionSubExpressionEnds(FunctionExpression fe, int parameterCnt) {
    throw new RuntimeException();
  }


  public void functionSubExpressionStarts(FunctionExpression fe, int parameterCnt) {
    throw new RuntimeException();
  }


  public void functionStarts(FunctionExpression fe) {
    throw new RuntimeException();

  }


  public void instanceFunctionStarts(VariableInstanceFunctionIncovation vifi) {
    throw new RuntimeException();

  }


  public void instanceFunctionEnds(VariableInstanceFunctionIncovation vifi) {
    throw new RuntimeException();

  }


  public void instanceFunctionSubExpressionEnds(Expression fe, int parameterCnt) {
    throw new RuntimeException();

  }


  public void instanceFunctionSubExpressionStarts(Expression fe, int parameterCnt) {
    throw new RuntimeException();

  }


  public void allPartsOfVariableFinished(Variable variable) {
  }


  public void expression2ArgsEnds(Expression2Args expression) {
    throw new RuntimeException();

  }


  public void literalExpression(LiteralExpression expression) {
    sb.append(".").append(expression.getValue()); //index-definition?
  }


  public void notStarts(Not not) {
    throw new RuntimeException();

  }


  public void notEnds(Not not) {
    throw new RuntimeException();

  }


  public void operator(Operator operator) {
    throw new RuntimeException();

  }


  public void variableStarts(Variable variable) {
    //begin
    currentDom = dom;
    sb.append(currentDom.getOriginalSimpleName());
  }


  public void variableEnds(Variable variable) {
  }


  public void variablePartStarts(VariableAccessPart part) {
    String partName = part.getName();
    List<AVariable> memberVars = currentDom.getMemberVars();
    for (AVariable member : memberVars) {
      if (member.getVarName().equals(partName)) {
        currentVar = member;
        currentDom = (DOM) member.getDomOrExceptionObject();
        sb.append(".").append(member.getLabel());
        break;
      }
    }
  }


  public void variablePartEnds(VariableAccessPart part) {
  }


  public void variablePartSubContextEnds(VariableAccessPart p) {
  }


  public AVariable getCurrentDom() {
    return currentVar;
  }


  @Override
  public void allPartsOfFunctionFinished(FunctionExpression fe) {
  }


  public void indexDefStarts() {
  }


  public void indexDefEnds() {
  }
}
