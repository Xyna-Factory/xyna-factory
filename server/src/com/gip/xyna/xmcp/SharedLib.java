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
package com.gip.xyna.xmcp;

import java.io.Serializable;
import java.util.List;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;


public class SharedLib implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private final String name;
  private final boolean inUse;
  private final List<String> content;
  private final RuntimeContext runtimeContext;
  
  public SharedLib(String name, boolean inUse, List<String> parts, String application, String version) {
    this(name, inUse, parts, application != null ? new Application(application, version) : RevisionManagement.DEFAULT_WORKSPACE);
  }

  public SharedLib(String name, boolean inUse, List<String> parts, RuntimeContext runtimeContext) {
    this.name = name;
    this.inUse = inUse;
    this.content = parts;
    this.runtimeContext = runtimeContext;
  }

  
  public String getName() {
    return name;
  }

  
  public boolean isInUse() {
    return inUse;
  }

  
  public List<String> getContent() {
    return content;
  }
  
  
  public String getApplication() {
    if (runtimeContext instanceof Application) {
      return runtimeContext.getName();
    }
    return null;
  }
  
  
  public String getVersion() {
    if (runtimeContext instanceof Application) {
      return ((Application) runtimeContext).getVersionName();
    }
    return null;
  }

  public String getWorkspace() {
    if (runtimeContext instanceof Workspace) {
      return runtimeContext.getName();
    }
    return null;
  }
  
  
  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }
}
