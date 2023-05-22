/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

import java.util.List;

import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.Visitor;


public class VariableInstanceFunctionIncovation extends VariableAccessPart {

  private final List<Expression> functionParameter;
  private List<TypeInfo> inputParameterTypes;
  private boolean requiresXynaOrder;
  
  public VariableInstanceFunctionIncovation(String name, int firstIdx, int lastIdx, Expression indexDef, List<Expression> functionParameter) {
    super(name, firstIdx, lastIdx, indexDef);
    this.functionParameter = functionParameter;
  }
  
  
  @Override
  public boolean isMemberVariableAccess() {
    return false;
  }

  
  public List<Expression> getFunctionParameter() {
    return functionParameter;
  }
  
  
  public void setInputParameterTypes(List<TypeInfo> inputParameterTypes) {
    this.inputParameterTypes = inputParameterTypes;
  }
  
  
  public List<TypeInfo> getInputParameterTypes() {
    return inputParameterTypes;
  }
  
  public void setRequiresXynaOrder(boolean requiresXynaOrder) {
    this.requiresXynaOrder = requiresXynaOrder;
  }
  
  public boolean requiresXynaOrder() {
    return requiresXynaOrder;
  }
  
  @Override
  public void visit(Visitor visitor, boolean lastPart) {
    visitor.variablePartStarts(this, lastPart);
    visitor.instanceFunctionStarts(this);
    for (int i = 0; i < getFunctionParameter().size(); i++) {
      Expression subExpr = getFunctionParameter().get(i);
      visitor.instanceFunctionSubExpressionStarts(this, i);
      subExpr.visit(visitor);
      visitor.instanceFunctionSubExpressionEnds(this, i);
    }
    visitor.instanceFunctionEnds(this);
    if (getIndexDef() != null) {
      getIndexDef().visit(visitor);
    }
    visitor.variablePartEnds(this, lastPart);
  }
  
}
