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


public class VariableAccessPart {

  private String name;
  private Expression indexDef; //inhalt von []
  private int lastIdx;
  private final int firstIdx;


  public VariableAccessPart(String name, int firstIdx, int lastIdx, Expression indexDef) {
    this.name = name;
    this.indexDef = indexDef;
    this.lastIdx = lastIdx;
    this.firstIdx = firstIdx;
  }
  

  public int getLastIdx() {
    return lastIdx;
  }


  public String getName() {
    return name;
  }


  public Expression getIndexDef() {
    return indexDef;
  }


  public int getFirstIdx() {
    return firstIdx;
  }
  
  
  public boolean isMemberVariableAccess() {
    return true;
  }


  public void setIndexDef(Expression indexDef) {
    this.indexDef = indexDef;    
  }


  public void visit(Visitor visitor, boolean lastPart) {
    visitor.variablePartStarts(this, lastPart);
    if (getIndexDef() != null) {
      visitor.indexDefStarts(getIndexDef());
      getIndexDef().visit(visitor);
      visitor.indexDefEnds(getIndexDef());
    }
    visitor.variablePartEnds(this, lastPart);
  }
  
}
