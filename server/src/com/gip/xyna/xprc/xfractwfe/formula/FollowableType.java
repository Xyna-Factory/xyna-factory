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
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.VariableInfo;


public interface FollowableType {
  
  boolean isPathMap();
  
  int getAccessPathLength();
  
  int getVarNum();
  
  Expression getIndexDef();
  
  VariableAccessPart getAccessPart(int index);

  TypeInfo getTypeOfExpression() throws XPRC_InvalidVariableMemberNameException;

  boolean lastPartOfVariableHasListAccess();

  String toJavaCodeSetter(int depth, boolean withListAccess, String uniqueVarName, long uniqueId);

  String toJavaCodeGetter(int depth, boolean withListAccess, long uniqueId);

  VariableInfo follow(int pathDepth) throws XPRC_InvalidVariableMemberNameException;

  VariableInfo getFollowedVariable() throws XPRC_InvalidVariableMemberNameException;

  int getPartIndex(VariableAccessPart part);

}
