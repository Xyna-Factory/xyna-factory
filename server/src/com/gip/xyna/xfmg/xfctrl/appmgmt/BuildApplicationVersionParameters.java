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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.io.Serializable;
import java.util.List;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;


public class BuildApplicationVersionParameters implements Serializable{

  private static final long serialVersionUID = 1L;
  
  private String comment;
  private List<String> excludeSubtypesOf;
  private Workspace parentWorkspace;
  private String user;
  private boolean remoteStub;
  
  public String getComment() {
    return comment;
  }
  
  public void setComment(String comment) {
    this.comment = comment;
  }
  
  public List<String> getExcludeSubtypesOf() {
    return excludeSubtypesOf;
  }
  
  public void setExcludeSubtypesOf(List<String> excludeSubtypesOf) {
    this.excludeSubtypesOf = excludeSubtypesOf;
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

  public boolean getRemoteStub() {
    return remoteStub;
  }
  
  public void setRemoteStub(boolean remoteStub) {
    this.remoteStub = remoteStub;
  }

  
}
