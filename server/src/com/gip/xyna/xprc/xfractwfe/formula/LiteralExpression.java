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



public class LiteralExpression extends Expression1Arg {

  private final String value;
  private final String valueEscapedForJava;
  private int lastIdx;


  public LiteralExpression(String value, String valueEscapedForJava, int firstIdx, int lastIdx) {
    super(firstIdx);
    this.value = value;
    this.valueEscapedForJava = valueEscapedForJava;
    this.lastIdx = lastIdx;
  }

  @Override
  public int getLastIdx() {
    return lastIdx;
  }


  @Override
  public void setLastIdx(int idx) {
    this.lastIdx = idx;
  }


  public String getValue() {
    return value;
  }


  public String getValueEscapedForJava() {
    return valueEscapedForJava;
  }

  @Override
  public void visit(Visitor visitor) {
    visitor.literalExpression(this);    
  }

}
