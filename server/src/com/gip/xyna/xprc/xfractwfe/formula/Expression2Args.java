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

import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.Visitor;


public class Expression2Args extends Expression {

  private final Expression var1;
  private final Expression var2;
  private final Operator op;


  public Expression2Args(int firstIdx, Expression var, Expression var2, Operator op) {
    super(firstIdx);
    this.var1 = var;
    this.var2 = var2;
    this.op = op;
  }


  @Override
  public int getLastIdx() {
    return var2.getLastIdx();
  }


  @Override
  public void setLastIdx(int idx) {
    var2.setLastIdx(idx);
  }


  public Expression getVar1() {
    return var1;
  }


  public Operator getOperator() {
    return op;
  }


  public Expression getVar2() {
    return var2;
  }


  @Override
  public void visit(Visitor visitor) {
    visitor.expression2ArgsStarts(this);
    getVar1().visit(visitor);
    visitor.operator(getOperator());
    getVar2().visit(visitor);
    visitor.expression2ArgsEnds(this);
  }


}
