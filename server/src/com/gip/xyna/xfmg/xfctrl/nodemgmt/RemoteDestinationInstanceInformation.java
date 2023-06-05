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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.io.Serializable;
import java.util.Map;

import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationType.DispatchingParameterDescription;

public class RemoteDestinationInstanceInformation implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private String name;
  private String typename;
  private String description;
  private Duration executionTimeout;
  private Map<String, String> startparameter;
  private DispatchingParameterDescription dispatchingParams;
  
  public RemoteDestinationInstanceInformation() {
    
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getTypename() {
    return typename;
  }
  
  public void setTypename(String typename) {
    this.typename = typename;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public Duration getExecutionTimeout() {
    return executionTimeout;
  }
  
  public void setExecutionTimeout(Duration executionTimeout) {
    this.executionTimeout = executionTimeout;
  }
  
  public Map<String, String> getStartparameter() {
    return startparameter;
  }
  
  public void setStartparameter(Map<String, String> startparameter) {
    this.startparameter = startparameter;
  }
  
  public DispatchingParameterDescription getDispatchingParams() {
    return dispatchingParams;
  }
  
  public void setDispatchingParams(DispatchingParameterDescription dispatchingParams) {
    this.dispatchingParams = dispatchingParams;
  }

}