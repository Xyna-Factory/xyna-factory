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

import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.Visitor;


public abstract class Expression {

  private TypeInfo origType = TypeInfo.UNKOWN;
  private TypeInfo targetType = TypeInfo.UNKOWN;
  private final int firstIdx;
  
  public Expression(int firstIdx) {
    this.firstIdx = firstIdx;
  }

  public abstract int getLastIdx();


  public void setOriginalType(TypeInfo type) {
    this.origType = type;
  }


  public void setTargetType(TypeInfo type) throws XPRC_InvalidVariableMemberNameException {
    this.targetType = type;
  }


  public abstract void setLastIdx(int idx);


  public TypeInfo getOriginalType() throws XPRC_InvalidVariableMemberNameException {
    return origType;
  }


  public TypeInfo getTargetType() {
    return targetType;
  }


  public int getFirstIdx() {
    return firstIdx;
  }
  
  public abstract void visit(Visitor visitor);

}
