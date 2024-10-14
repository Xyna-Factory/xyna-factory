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
package com.gip.xyna.xfmg.xfctrl.deploymentmarker;

import java.io.Serializable;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentificationBase;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentifier;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


public abstract class DeploymentMarker implements Serializable{
  
  private static final long serialVersionUID = 1L;
  
  private Long id;
  private String deploymentItemName;
  private XMOMType deploymentItemType;
  private RuntimeContext runtimeContext;
  
  
  public Long getId() {
    return id;
  }
  
  public void setId(Long id) {
    this.id = id;
  }
  
  public DeploymentItemIdentifier getDeploymentItem() {
    return new DeploymentItemIdentificationBase(deploymentItemType, deploymentItemName);
  }
  
  public void setDeploymentItem(DeploymentItemIdentifier deploymentItem) {
    this.deploymentItemName = deploymentItem.getName();
    this.deploymentItemType = deploymentItem.getType();
  }
  
  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }
  
  public void setRuntimeContext(RuntimeContext runtimeContext) {
    this.runtimeContext = runtimeContext;
  }
  
  public Long getRevision() {
    if (runtimeContext == null) {
      return null;
    }
    
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try {
      return revisionManagement.getRevision(runtimeContext);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException("RuntimeContext " + runtimeContext + " unkown", e);
    }
  }
}
