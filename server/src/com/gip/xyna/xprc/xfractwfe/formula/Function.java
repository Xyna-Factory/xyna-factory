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



public final class Function {

  private final FunctionParameterTypeDefinition parameterTypeDef;
  private final String name;
  private final String javaCode;
  private final TypeInfo resultType;
  private final FunctionVisitationPattern customVisitationPattern;


  public Function(String name, TypeInfo resultType, FunctionParameterTypeDefinition parameterTypeDef, String javaCode) {
    this(name, resultType, parameterTypeDef, javaCode, null);
  }
  
  public Function(String name, TypeInfo resultType, FunctionParameterTypeDefinition parameterTypeDef, String javaCode, FunctionVisitationPattern customVisitationPattern) {
    this.name = name;
    this.resultType = resultType;
    this.javaCode = javaCode;
    this.parameterTypeDef = parameterTypeDef;
    this.customVisitationPattern = customVisitationPattern;
  }


  public String getName() {
    return name;
  }


  public FunctionParameterTypeDefinition getParameterTypeDef() {
    return parameterTypeDef;
  }


  public String getJavaCode() {
    return javaCode;
  }


  public TypeInfo getResultType() {
    return resultType;
  }
  
  
  public boolean hasCustomVisitationPattern() {
    return customVisitationPattern != null;
  }
  
  
  public FunctionVisitationPattern getCustomVisitationPattern() {
    return customVisitationPattern;
  }

}
