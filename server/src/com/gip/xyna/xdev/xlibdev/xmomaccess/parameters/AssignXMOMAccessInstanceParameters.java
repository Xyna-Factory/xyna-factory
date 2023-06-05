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
package com.gip.xyna.xdev.xlibdev.xmomaccess.parameters;

import java.io.Serializable;


public class AssignXMOMAccessInstanceParameters implements Serializable{

  private static final long serialVersionUID = 1L;

  private String xmomAccessName;
  private String repositoryAccessInstanceName;
  private boolean includeCapacities = false;
  private boolean includeXynaProperties = false;
  private boolean deploy = false;
  
  
  public String getXmomAccessName() {
    return xmomAccessName;
  }

  public void setXmomAccessName(String xmomAccessName) {
    this.xmomAccessName = xmomAccessName;
  }

  public String getRepositoryAccessInstanceName() {
    return repositoryAccessInstanceName;
  }

  public void setRepositoryAccessInstanceName(String repositoryAccessInstanceName) {
    this.repositoryAccessInstanceName = repositoryAccessInstanceName;
  }

  public boolean includeCapacities() {
    return includeCapacities;
  }
  
  public void setIncludeCapacities(boolean includeCapacities) {
    this.includeCapacities = includeCapacities;
  }
  
  public boolean includeXynaProperties() {
    return includeXynaProperties;
  }
  
  public void setIncludeXynaProperties(boolean includeXynaProperties) {
    this.includeXynaProperties = includeXynaProperties;
  }

  public boolean deploy() {
    return deploy;
  }
  
  public void setDeploy(boolean deploy) {
    this.deploy = deploy;
  }
}
