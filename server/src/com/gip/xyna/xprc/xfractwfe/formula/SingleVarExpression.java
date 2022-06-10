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
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.Visitor;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.VariableInfo;




public class SingleVarExpression extends Expression1Arg implements FollowableType {

  private final Variable var;
  private int lastIdx;


  public SingleVarExpression(Variable var) {
    super(var.getFirstIdx());
    this.var = var;
    this.lastIdx = var.getLastIdx();
  }


  @Override
  public int getLastIdx() {
    return lastIdx;
  }


  @Override
  public void setLastIdx(int idx) {
    lastIdx = idx;
  }


  @Override
  public void setOriginalType(TypeInfo type) {
    throw new RuntimeException();
  }


  @Override
  public void setTargetType(TypeInfo type) throws XPRC_InvalidVariableMemberNameException {
    var.setTargetType(type);
  }


  @Override
  public TypeInfo getOriginalType() throws XPRC_InvalidVariableMemberNameException {
    return var.getTypeOfExpression();
  }


  @Override
  public TypeInfo getTargetType() {
    return var.getTargetType();
  }


  public Variable getVar() {
    return var;
  }


  public boolean isPathMap() {
    return var.isPathMap();
  }


  public int getAccessPathLength() {
    return var.getParts().size();
  }


  public int getVarNum() {
    return var.getVarNum();
  }


  public Expression getIndexDef() {
    return var.getIndexDef();
  }


  public VariableAccessPart getAccessPart(int index) {
    return var.getParts().get(index);
  }


  public TypeInfo getTypeOfExpression() throws XPRC_InvalidVariableMemberNameException {
    return var.getTypeOfExpression();
  }


  public boolean lastPartOfVariableHasListAccess() {
    return var.lastPartOfVariableHasListAccess();
  }


  public String toJavaCodeSetter(int depth, boolean withListAccess, String uniqueVarName, long uniqueId) {
    return var.toJavaCodeSetter(depth, withListAccess, uniqueVarName, uniqueId);
  }


  public String toJavaCodeGetter(int depth, boolean withListAccess, long uniqueId) {
    return var.toJavaCodeGetter(depth, withListAccess, uniqueId);
  }


  public VariableInfo follow(int pathDepth) throws XPRC_InvalidVariableMemberNameException {
    return var.getBaseVariable().follow(var.getParts(), pathDepth);
  }


  public VariableInfo getFollowedVariable() throws XPRC_InvalidVariableMemberNameException {
    return var.getFollowedVariable();
  }


  public void visit(Visitor visitor) {
    visitor.singleVarExpressionStarts(this);
    var.visit(visitor);
    visitor.singleVarExpressionEnds(this);
  }


  public int getPartIndex(VariableAccessPart part) {
    return var.getParts().indexOf(part);
  }

}
