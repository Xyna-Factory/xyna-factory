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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.io.Serializable;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;


public class CopyApplicationIntoWorkspaceParameters implements Serializable{

  private static final long serialVersionUID = 1L;
  
  private Workspace targetWorkspace;
  private String comment;
  private boolean overrideChanges;
  private String user;
  
  
  public Workspace getTargetWorkspace() {
    return targetWorkspace;
  }
  
  public void setTargetWorkspace(Workspace targetWorkspace) {
    this.targetWorkspace = targetWorkspace;
  }
  
  public String getComment() {
    return comment;
  }
  
  public void setComment(String comment) {
    this.comment = comment;
  }
  
  public boolean overrideChanges() {
    return overrideChanges;
  }
  
  public void setOverrideChanges(boolean overrideChanges) {
    this.overrideChanges = overrideChanges;
  }
  
  public String getUser() {
    return user;
  }
  
  public void setUser(String user) {
    this.user = user;
  }
}
