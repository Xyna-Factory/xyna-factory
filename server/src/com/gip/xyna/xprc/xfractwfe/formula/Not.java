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

import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.Visitor;



public class Not extends Expression1Arg {

  private final Expression innerExp;


  public Not(int firstIdx, Expression innerExp) {
    super(firstIdx);
    this.innerExp = innerExp;
  }


  public int getLastIdx() {
    return innerExp.getLastIdx();
  }


  @Override
  public void setLastIdx(int idx) {
    innerExp.setLastIdx(idx);
  }


  public Expression getInnerExpression() {
    return innerExp;
  }

  
  @Override
  public void visit(Visitor visitor) {
    visitor.notStarts(this);
    getInnerExpression().visit(visitor);
    visitor.notEnds(this);
  }
}
