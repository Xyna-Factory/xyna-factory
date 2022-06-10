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

package com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters;

import java.io.Serializable;


public class RemoveWorkspaceParameters implements Serializable{
  
  private static final long serialVersionUID = 1L;
  
  private boolean force;
  private boolean cleanupXmls;
  private String user;

  
  public boolean isForce() {
    return force;
  }
  
  public void setForce(boolean force) {
    this.force = force;
  }

  
  public boolean cleanupXmls() {
    return cleanupXmls;
  }

  
  public void setCleanupXmls(boolean cleanupXmls) {
    this.cleanupXmls = cleanupXmls;
  }
  
  
  public boolean keepForAudits() {
    return !cleanupXmls;
  }
  
  
  public String getUser() {
    return user;
  }
  
  
  public void setUser(String user) {
    this.user = user;
  }
}
