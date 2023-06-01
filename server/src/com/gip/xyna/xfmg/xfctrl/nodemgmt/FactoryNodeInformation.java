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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.FactoryNodeCaller.FactoryNodeCallerStatus;

public class FactoryNodeInformation {

  private String name;
  private int instanceId;
  private String description;
  private String remoteAccessType;
  private FactoryNodeCallerStatus status;
  private XFMG_NodeConnectException lastException;
  
  private int waitingForConnectivity;
  private int waitingForResult;
  
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public int getInstanceId() {
    return instanceId;
  }
  public void setInstanceId(int instanceId) {
    this.instanceId = instanceId;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public String getRemoteAccessType() {
    return remoteAccessType;
  }
  public void setRemoteAccessType(String remoteAccessType) {
    this.remoteAccessType = remoteAccessType;
  }
  public FactoryNodeCallerStatus getStatus() {
    return status;
  }
  public void setStatus(FactoryNodeCallerStatus status) {
    this.status = status;
  }
  public int getWaitingForConnectivity() {
    return waitingForConnectivity;
  }
  public void setWaitingForConnectivity(int waitingForConnectivity) {
    this.waitingForConnectivity = waitingForConnectivity;
  }
  public int getWaitingForResult() {
    return waitingForResult;
  }
  public void setWaitingForResult(int waitingForResult) {
    this.waitingForResult = waitingForResult;
  }
  public void setConnectException(XFMG_NodeConnectException lastException) {
    this.lastException = lastException;
  }
  public XFMG_NodeConnectException getConnectException() {
    return lastException;
  }
}