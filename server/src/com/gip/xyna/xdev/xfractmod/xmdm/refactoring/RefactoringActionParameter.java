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
package com.gip.xyna.xdev.xfractmod.xmdm.refactoring;

import java.io.Serializable;

import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringManagement.RefactoringType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;


public abstract class RefactoringActionParameter implements Serializable {
  
  public static enum RefactoringTargetRootType {
    DATATYPE(XMOMType.DATATYPE),
    EXCEPTION(XMOMType.EXCEPTION),
    WORKFLOW(XMOMType.WORKFLOW),
    OPERATION(XMOMType.DATATYPE),
    SERVICE_GROUP(XMOMType.DATATYPE),
    PATH(null),
    MEMBER_VARIABLE(null),
    BASE_TYPE(null);
    
    private final XMOMType xmomRootType;
    
    private RefactoringTargetRootType(XMOMType xmomRootType) {
      this.xmomRootType = xmomRootType;
    }
    
    public XMOMType getXmomRootType() {
      return xmomRootType;
    }
    
    public static RefactoringTargetRootType fromXMOMType(XMOMType xmomRootType) {
      switch (xmomRootType) {
        case DATATYPE :
          return DATATYPE;
        case EXCEPTION :
          return EXCEPTION;
        case WORKFLOW :
          return WORKFLOW;
        default :
          return null;
      }
    }
    
  }
  
  private static final long serialVersionUID = 1L;

  private final RefactoringType type;
  private String sessionId;
  private String username;
  private boolean forceDeploy;
  private boolean ignoreIncompatibleStorables;
  private RuntimeContext runtimeContext;
  
  public RefactoringActionParameter(RefactoringType type) {
    this.type = type;
  }
  
  public RefactoringType getRefactoringType() {
    return type;
  }

  
  public String getSessionId() {
    return sessionId;
  }

  
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  
  public String getUsername() {
    return username;
  }

  
  public void setUsername(String username) {
    this.username = username;
  }
  
  
  public boolean doForceDeploy() {
    return forceDeploy;
  }
  
  public void setForceDeploy(boolean forceDeploy) {
    this.forceDeploy = forceDeploy;
  }
  
  
  public boolean ignoreIncompatibleStorables() {
    return ignoreIncompatibleStorables;
  }
  
  
  public void setIgnoreIncompatibleStorables(boolean ignoreIncompatibleStorables) {
    this.ignoreIncompatibleStorables = ignoreIncompatibleStorables;
  }
  
  
  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }
  
  
  public void setRuntimeContext(RuntimeContext runtimeContext) {
    this.runtimeContext = runtimeContext;
  }
}
