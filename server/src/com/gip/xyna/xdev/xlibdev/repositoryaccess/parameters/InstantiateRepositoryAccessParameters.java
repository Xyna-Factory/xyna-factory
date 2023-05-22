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
package com.gip.xyna.xdev.xlibdev.repositoryaccess.parameters;

import java.io.Serializable;
import java.util.Map;


public class InstantiateRepositoryAccessParameters implements Serializable{

  private static final long serialVersionUID = 1L;

  private String repositoryAccessInstanceName;
  private String repositoryAccessName;
  private Map<String, String> parameterMap;
  private String xmomAccessName;
  private String codeAccessName;
  
  
  public String getRepositoryAccessInstanceName() {
    return repositoryAccessInstanceName;
  }
  
  public void setRepositoryAccessInstanceName(String repositoryAccessInstanceName) {
    this.repositoryAccessInstanceName = repositoryAccessInstanceName;
  }
  
  public String getRepositoryAccessName() {
    return repositoryAccessName;
  }
  
  public void setRepositoryAccessName(String repositoryAccessName) {
    this.repositoryAccessName = repositoryAccessName;
  }
  
  public Map<String, String> getParameterMap() {
    return parameterMap;
  }
  
  public void setParameterMap(Map<String, String> parameterMap) {
    this.parameterMap = parameterMap;
  }
  
  
  public String getXmomAccessName() {
    return xmomAccessName;
  }
  
  
  public void setXmomAccessName(String xmomAccessName) {
    this.xmomAccessName = xmomAccessName;
  }
  
  
  public String getCodeAccessName() {
    return codeAccessName;
  }
  
  
  public void setCodeAccessName(String codeAccessName) {
    this.codeAccessName = codeAccessName;
  }
}
