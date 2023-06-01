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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;



public class ExceptionEntry_1_1 implements ExceptionEntry {

  private Map<String, String> messages;
  private String name;
  private String path;
  private List<ExceptionParameter> parameter;
  private String label;
  private boolean isAbstract = false;
  private String baseExceptionName;
  private String baseExceptionPath;
  private String code;


  public ExceptionEntry_1_1(Map<String, String> messages, String name, String path, String code) {
    this.messages = messages;
    this.name = name;
    this.path = path;
    this.code = code;
    parameter = new ArrayList<ExceptionParameter>();
  }


  public void addExceptionParameter(ExceptionParameter p) {
    parameter.add(p);
  }


  public String getLabel() {
    return label;
  }


  public void setLabel(String label) {
    this.label = label;
  }


  public boolean isAbstract() {
    return isAbstract;
  }


  protected void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }


  public String getBaseExceptionName() {
    return baseExceptionName;
  }


  protected void setBaseExceptionName(String baseExceptionName) {
    this.baseExceptionName = baseExceptionName;
  }


  public String getBaseExceptionPath() {
    return baseExceptionPath;
  }


  protected void setBaseExceptionPath(String baseExceptionPath) {
    this.baseExceptionPath = baseExceptionPath;
  }


  public Map<String, String> getMessages() {
    return messages;
  }


  public String getName() {
    return name;
  }


  public String getPath() {
    return path;
  }


  public List<ExceptionParameter> getParameter() {
    return parameter;
  }


  public String getCode() {
    return code;
  }


}
