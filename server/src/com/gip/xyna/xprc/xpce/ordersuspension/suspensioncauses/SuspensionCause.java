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
package com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses;

import java.io.Serializable;

import com.gip.xyna.xprc.xpce.ordersuspension.SuspensionBackupMode;


/**
 *
 */
public abstract class SuspensionCause implements Serializable {
  private static final long serialVersionUID = 1L;

  protected String laneId;
  protected SuspensionBackupMode orderBackupMode;
  
  
  public abstract String getName();
  
  public boolean needToFreeCapacities() {
    return true;
  }
  
  public boolean needToFreeVetos() {
    return false;
  }

  @Override
  public String toString() {
    return getName();
  }
  
  public void setLaneId(String laneId) {
    this.laneId = laneId;
  }
  
  public String getLaneId() {
    return laneId;
  }
  
  public void setSuspensionOrderBackupMode(SuspensionBackupMode orderBackupMode) {
    this.orderBackupMode = orderBackupMode;
  }
  
  public void setSuspensionOrderBackupModeIfUnset(SuspensionBackupMode orderBackupMode) {
    if (this.orderBackupMode == null) {
      this.orderBackupMode = orderBackupMode;
    }
  }
  
  public SuspensionBackupMode getOrderBackupMode() {
    return orderBackupMode;
  }
  
  
}
