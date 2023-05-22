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
package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.List;

import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;


public interface VariableContextIdentification {
  
  public interface VariableInfo {

    /**
     * depth = 0 -&gt; erstes kind
     * depth = parts.size-1 = bis zum ende gefolgt
     * @throws XPRC_InvalidVariableMemberNameException 
     */
    VariableInfo follow(List<VariableAccessPart> parts, int depth) throws XPRC_InvalidVariableMemberNameException;

    /**
     * falls ignoreList auf true gesetzt ist, wird nie TypeInfo==LIST zur�ckgegeben. ansonsten wird der typ zur�ckgegeben, 
     * und der kann auch LIST sein, falls die variable (inkl pfad) listenwertig ist.
     */
    TypeInfo getTypeInfo(boolean ignoreList);

    String getJavaCodeForVariableAccess();

    String getVarName();

    void castTo(TypeInfo type);

  }
  
  public interface OperationInfo {

    String getOperationName();

    List<VariableInfo> getResultTypes();

  }

  VariableInfo createVariableInfo(TypeInfo resultType);
  
  /**
   * Gibt Typ von Root Variable zur�ck. Falls followAccessParts=true, werden die Accessparts mit validiert
   */
  VariableInfo createVariableInfo(Variable v, boolean followAccessParts) throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException;
  
  TypeInfo getTypeInfo(String originalXmlName);
  
  Long getRevision();

  
}
