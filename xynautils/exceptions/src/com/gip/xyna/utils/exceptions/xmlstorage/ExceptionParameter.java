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
package com.gip.xyna.utils.exceptions.xmlstorage;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import com.gip.xyna.utils.exceptions.utils.codegen.InvalidParameterNameException;

public class ExceptionParameter {

  private static Set<String> invalidVarNames = new HashSet<String>(Arrays.asList(new String[]{"code", "message", "args"}));
  private String label;
  private String varName;
  private boolean isReference;
  private String typeName;
  private String typePath;
  private String javaType; //nur gesetzt, falls isReference = false;
  private boolean isList = false;
  private String type; //exception/data
  private String documentation;

  public ExceptionParameter(String varName, boolean isReference) throws InvalidParameterNameException {
    if (invalidVarNames.contains(varName.toLowerCase())) {
      throw new InvalidParameterNameException(varName);
    }
    this.varName = varName;
    this.isReference = isReference;
  }

  public void setType(String type) {
    this.type = type;
  }
  
  public String getType() {
    return type;
  }

  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }

  public String getDocumentation() {
    return documentation;
  }

  public void setLabel(String label) {
    this.label = label;
  }


  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }


  public void setTypePath(String typePath) {
    this.typePath = typePath;
  }


  public void setJavaType(String javaType) {
    this.javaType = javaType;
  }


  public String getLabel() {
    return label;
  }


  public String getVarName() {
    return varName;
  }


  public boolean isReference() {
    return isReference;
  }


  public String getTypeName() {
    return typeName;
  }


  public String getTypePath() {
    return typePath;
  }


  public String getJavaType() {
    return javaType;
  }


  public void setIsList(boolean isList) {
    this.isList = isList;
  }
  
  public boolean isList() {
    return isList;
  }


}
