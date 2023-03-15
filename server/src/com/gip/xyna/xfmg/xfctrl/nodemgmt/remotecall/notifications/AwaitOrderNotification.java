/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications;

import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.FactoryNodeCaller;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.NotificationProcessor.RemoteCallNotificationStatus;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.Resumer;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;

/**
 * AwaitOrderNotification
 * wartet, bis Auftrag "remoteOrderId" auf dem FactoryNode berarbeitet wurde.
 * Resume oder Notify, falls Auftrag beendet ist oder Timeout abgelaufen ist 
 *
 */
public class AwaitOrderNotification extends RemoteCallNotification {
  
  private long absoluteTimeout;
  private ResumeTarget resumeTarget;
  private boolean aborted;

  public AwaitOrderNotification(Long remoteOrderId) {
    setOrderId(remoteOrderId);
  }
  
  public AwaitOrderNotification(Long remoteOrderId, long absoluteTimeout, ResumeTarget resumeTarget) {
    setOrderId(remoteOrderId);
    this.absoluteTimeout = absoluteTimeout;
    this.resumeTarget = resumeTarget;
  }

  public long getTimeout() {
    return absoluteTimeout;
  }
  
  public ResumeTarget getResumeTarget() {
    return resumeTarget;
  }

  public boolean isAborted() {
    return aborted;
  }

  @Override
  public RemoteCallNotificationStatus execute(FactoryNodeCaller factoryNodeCaller) {
    if (!factoryNodeCaller.addAwaitOrder(this)) {
      return RemoteCallNotificationStatus.Succeeded;
    }
    if (resumeTarget == null) {
      return RemoteCallNotificationStatus.Execution;
    } else {
      return RemoteCallNotificationStatus.Parked;
    }
  }


  public boolean isParked() {
    return getStatus() == RemoteCallNotificationStatus.Parked;
  }

  public void setResponseRetrieved(Resumer resumer) {
    if( resumeTarget != null ) {
      resumer.resume(getRemoteOrderId()); 
    } else {
      setStatusAndNotify(RemoteCallNotificationStatus.Succeeded);
    }
  }

  public void disconnect(String nodeName) {
    setNodeConnectException( new XFMG_NodeConnectException(nodeName) );
    setStatusAndNotify(RemoteCallNotificationStatus.Failed);
  }
  
  public void abort(String nodeName) {
    aborted = true;
    setNodeConnectException( new XFMG_NodeConnectException(nodeName) );
    setStatusAndNotify(RemoteCallNotificationStatus.Failed);
  }

}