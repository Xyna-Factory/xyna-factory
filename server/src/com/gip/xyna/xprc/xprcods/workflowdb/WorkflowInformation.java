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
package com.gip.xyna.xprc.xprcods.workflowdb;

import java.io.Serializable;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;


public class WorkflowInformation implements Serializable {

  private static final long serialVersionUID = 1L;

  private String fqClassName;
  private RuntimeContext runtimeContext;
  private DeploymentStatus deploymentStatus;
  
  
  public WorkflowInformation(String fqClassName, RuntimeContext runtimeContext, DeploymentStatus deploymentStatus) {
    this.fqClassName = fqClassName;
    this.runtimeContext = runtimeContext;
    this.deploymentStatus = deploymentStatus;
  }

  public String getFqClassName() {
    return fqClassName;
  }
  
  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }
  
  
  public DeploymentStatus getDeploymentStatus() {
    return deploymentStatus;
  }
}
