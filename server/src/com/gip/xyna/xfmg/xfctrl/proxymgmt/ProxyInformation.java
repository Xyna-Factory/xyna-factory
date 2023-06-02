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
package com.gip.xyna.xfmg.xfctrl.proxymgmt;

import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIParameter;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.storables.ProxyStorable;

public class ProxyInformation {

  private ProxyStorable proxy;
  private int numberOfRights;
  private int numberOfProxyMethods;
  private String failure;
  private RMIParameter rmiParameter;
  
  public ProxyInformation(ProxyStorable proxy) {
    this.proxy = proxy;
  }

  public String getName() {
    return proxy.getName();
  }
  
  public ProxyStorable getProxy() {
    return proxy;
  }

  public void setNumberOfRights(int numberOfRights) {
    this.numberOfRights = numberOfRights;
  }

  public void setNumberOfProxyMethods(int numberOfProxyMethods) {
    this.numberOfProxyMethods = numberOfProxyMethods;
  }
  
  public int getNumberOfProxyMethods() {
    return numberOfProxyMethods;
  }
  public int getNumberOfRights() {
    return numberOfRights;
  }

  public String getDescription() {
    if( failure != null ) {
      return failure;
    }
    return proxy.getDescription();
  }

  public void setFailure(String failure) {
    this.failure = failure;
  }

  public void setRMIParameter(RMIParameter rmiParameter) {
    this.rmiParameter = rmiParameter;
  }
  
  public RMIParameter getRMIParameter() {
    return rmiParameter;
  }
}
