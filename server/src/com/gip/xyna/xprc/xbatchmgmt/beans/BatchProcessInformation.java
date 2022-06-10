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
package com.gip.xyna.xprc.xbatchmgmt.beans;

import java.io.Serializable;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessScheduling.SchedulingState;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessArchiveStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessCustomizationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRestartInformationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRuntimeInformationStorable;


public class BatchProcessInformation implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private Long batchProcessId;
  private BatchProcessArchiveStorable archiveData;
  private BatchProcessRuntimeInformationStorable runtimeInformationData;
  private BatchProcessCustomizationStorable customizationData;
  private BatchProcessRestartInformationStorable restartData;
  private SchedulingState schedulingState;
  private RemoteXynaOrderCreationParameter masterOrderCreationParameter;
  private BatchProcessStatus batchProcessStatus;
  
  public BatchProcessInformation() {
  }
  
  public BatchProcessInformation(BatchProcessArchiveStorable from, boolean fillRuntimeInformation) {
    this.archiveData = new BatchProcessArchiveStorable(from);
    this.batchProcessId = from.getOrderId();
    if( fillRuntimeInformation ) {
      this.runtimeInformationData = new BatchProcessRuntimeInformationStorable(from);
      this.batchProcessStatus = BatchProcessStatus.from( archiveData.getOrderStatus(), null );
    }
  }

  public Long getBatchProcessId() {
    return batchProcessId;
  }

  public BatchProcessRuntimeInformationStorable getRuntimeInformation() {
    return runtimeInformationData;
  }

  public void setRuntimeInformation(BatchProcessRuntimeInformationStorable runtimeInformationData) {
    this.runtimeInformationData = runtimeInformationData;
    this.archiveData.updateWithRuntimeInformation(runtimeInformationData, false);
  }
  
  public void setCustomization(BatchProcessCustomizationStorable customizationData) {
    this.customizationData = customizationData;
    this.archiveData.updateWithCustomization(customizationData);
  }

  public void setRestartInformation(BatchProcessRestartInformationStorable restartData) {
    this.restartData = restartData;
    this.archiveData.updateWithRestartInformation(restartData);
  }
  
  public BatchProcessCustomizationStorable getCustomization() {
    return customizationData;
  }

  public BatchProcessRestartInformationStorable getRestartInformation() {
    return restartData;
  }

  public BatchProcessArchiveStorable getArchive() {
    return archiveData;
  }
  
  public SchedulingState getSchedulingState() {
    return schedulingState;
  }

  public void setSchedulingState(SchedulingState schedulingState) {
    this.schedulingState = schedulingState;
  }

  public String getLabel() {
    return archiveData.getLabel();
  }

  public String getApplication() {
    return archiveData.getApplication();
  }

  public String getVersion() {
    return archiveData.getVersion();
  }

  public String getWorkspace() {
    return archiveData.getWorkspace();
  }

  public String getComponent() {
    return archiveData.getComponent();
  }

  public String getSlaveOrdertype() {
    return archiveData.getSlaveOrdertype();
  }

  public int getCanceled() {
    return archiveData.getCanceled();
  }

  public RemoteXynaOrderCreationParameter getMasterOrderCreationParameter() {
    return masterOrderCreationParameter;
  }
  
  public void setMasterOrderCreationParameter(RemoteXynaOrderCreationParameter masterOrderCreationParameter) {
    this.masterOrderCreationParameter = masterOrderCreationParameter;
  }
  
  public BatchProcessStatus getBatchProcessStatus() {
    return batchProcessStatus;
  }
  
  public void setBatchProcessStatus(BatchProcessStatus batchProcessStatus) {
    this.batchProcessStatus = batchProcessStatus;
  }

  public RuntimeContext getRuntimeContext() {
    if (getWorkspace() != null) {
      return new Workspace(getWorkspace());
    }
    return new Application(getApplication(), getVersion());
  }

}
