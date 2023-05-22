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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.io.Serializable;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;


public class RemoveApplicationParameters implements Serializable{

  private static final long serialVersionUID = 1L;
  
  private boolean global;
  private boolean force;
  private boolean extraForce;
  private boolean keepForAudits;
  private boolean stopIfRunning = false;
  private boolean removeIfUsed;
  private Workspace parentWorkspace;
  private String user;
  
  public boolean isGlobal() {
    return global;
  }
  
  public void setGlobal(boolean global) {
    this.global = global;
  }
  
  public boolean isForce() {
    return force;
  }
  
  public void setForce(boolean force) {
    this.force = force;
  }

  public boolean isExtraForce() {
    return extraForce;
  }
  
  public void setExtraForce(boolean extraForce) {
    this.extraForce = extraForce;
  }
  
  public boolean isKeepForAudits() {
    return keepForAudits;
  }
  
  public void setKeepForAudits(boolean keepForAudits) {
    this.keepForAudits = keepForAudits;
  }

  public void setStopIfRunning(boolean b) {
    stopIfRunning = b;
  }
  
  public boolean stopIfRunning() {
    return stopIfRunning;
  }
  
  public Workspace getParentWorkspace() {
    return parentWorkspace;
  }
  
  public void setParentWorkspace(Workspace parentWorkspace) {
    this.parentWorkspace = parentWorkspace;
  }
  
  public String getUser() {
    return user;
  }
  
  public void setUser(String user) {
    this.user = user;
  }
  
  public boolean isRemoveIfUsed() {
    return removeIfUsed;
  }
  
  public void setRemoveIfUsed(boolean removeIfUsed) {
    this.removeIfUsed = removeIfUsed;
  }
}
