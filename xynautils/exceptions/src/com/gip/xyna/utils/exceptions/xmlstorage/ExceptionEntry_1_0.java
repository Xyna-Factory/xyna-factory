/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ExceptionEntry_1_0 implements ExceptionEntry {
  
  private String code;
  private Map<String, String> messages;
  private String variableName;
  private List<ExceptionParameter> parameter;

  public ExceptionEntry_1_0(Map<String, String> messageMap, String code) {
    this.code = code;
    this.messages = messageMap;
    parameter = new ArrayList<ExceptionParameter>();
  }

  public String getCode() {
    return code;
  }

  public Map<String, String> getMessages() {
    return messages;
  }

  public void setVariableName(String varName) {
    variableName = varName;
  }

  public void addExceptionParameter(ExceptionParameter p) {
    parameter.add(p);
  }
  
  protected String getVariableName() {
    return variableName;
  }

  public List<ExceptionParameter> getParameter() {
    return parameter;
  }
  
  
}
