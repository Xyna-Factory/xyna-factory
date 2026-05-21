/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter.session.workflowissues;



import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public class TypeToAVariableConverter {


  public static AVariable convert(DOM dom, Class<?> clazz) {
    AVariable result;

    PrimitiveType primitiveType = AVariable.PrimitiveType.createOrNull(clazz.getCanonicalName());
    if (primitiveType != null) {
      result = createPrimitiveTypeAVariable(dom, primitiveType);
    } else {
      result = createModelledTypeAvariable(dom, clazz);
    }

    return result;
  }


  private static AVariable createPrimitiveTypeAVariable(DOM dom, PrimitiveType type) {
    DatatypeVariable result = new DatatypeVariable(dom);
    result.create(type);
    return result;
  }


  private static AVariable createModelledTypeAvariable(DOM dom, Class<?> clazz) {
    DatatypeVariable result = new DatatypeVariable(dom);
    try {
      boolean isReserved = GenerationBase.isReservedServerObjectByFqClassName(clazz.getCanonicalName());
      String fqOriginalName = isReserved ? GenerationBase.getXmlNameForReservedClass(clazz) : clazz.getCanonicalName();
      result.create(fqOriginalName);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    return result;
  }
}
