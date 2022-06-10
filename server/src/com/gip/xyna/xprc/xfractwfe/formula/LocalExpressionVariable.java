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

import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.Visitor;




public class LocalExpressionVariable extends Expression1Arg {

  private final Expression expr;
  private final String variableName;
  private final int partIndex;
  private final ExtractionReason reason;
  
  
  public static enum ExtractionReason {
    TARGET_LIST_INITIALIZATION, PATH_MAP_INDEX;
  }
  

  public LocalExpressionVariable(Expression expr, String uniqueSuffixForVarName, ExtractionReason reason) {
    this(expr, uniqueSuffixForVarName, -1, reason);
  }
  
  public LocalExpressionVariable(Expression expr, String uniqueSuffixForVarName, int partIndex, ExtractionReason reason) {
    super(expr.getFirstIdx());
    String varName = ModelledExpression.TEMP_VARIABLE_PREFIX + "_local_" + uniqueSuffixForVarName;
    this.variableName = varName;
    this.expr = expr;
    this.partIndex = partIndex;
    this.reason = reason;
  }


  @Override
  public int getLastIdx() {
    return expr.getLastIdx();
  }


  @Override
  public void setLastIdx(int idx) {
    throw new RuntimeException();
  }


  @Override
  public void setOriginalType(TypeInfo type) {
    throw new RuntimeException();
  }


  @Override
  public void setTargetType(TypeInfo type) throws XPRC_InvalidVariableMemberNameException {
    expr.setTargetType(type);
  }


  @Override
  public TypeInfo getOriginalType() throws XPRC_InvalidVariableMemberNameException {
    return expr.getOriginalType();
  }


  @Override
  public TypeInfo getTargetType() {
    return expr.getTargetType();
  }

  
  public String getVariableName() {
    return variableName;
  }
  
  
  public String getUniqueVariableName(long uniqueId) {
    return getVariableName() + "_" + uniqueId;
  }
  

  public Expression getExpression() {
    return expr;
  }
 
  
  public int getPartIndex() {
    return partIndex;
  }
  
  
  public ExtractionReason getExtractionReason() {
    return reason;
  }


  @Override
  public void visit(Visitor visitor) {
    //  if the target expression would be generated via Visitor this might be harmfull
    getExpression().visit(visitor);
  }

}
