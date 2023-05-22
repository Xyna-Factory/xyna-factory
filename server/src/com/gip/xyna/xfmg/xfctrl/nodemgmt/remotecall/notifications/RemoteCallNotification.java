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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications;

import java.util.concurrent.CountDownLatch;

import com.gip.xyna.utils.concurrent.AtomicEnum;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeRemoteException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.FactoryNodeCaller;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.NotificationProcessor.RemoteCallNotificationStatus;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse.SerializableExceptionInformation;

public abstract class RemoteCallNotification {
  protected CountDownLatch cdl = new CountDownLatch(1);
  private Long remoteOrderId;
  private XFMG_NodeConnectException nodeConnectException;
  protected AtomicEnum<RemoteCallNotificationStatus> status = new AtomicEnum<RemoteCallNotificationStatus>(RemoteCallNotificationStatus.class, RemoteCallNotificationStatus.New);
  private SerializableExceptionInformation serializedException;
  private String nodeName;
  
  public RemoteCallNotificationStatus setNodeConnectException(XFMG_NodeConnectException nodeConnectException) {
    this.nodeConnectException = nodeConnectException;
    return RemoteCallNotificationStatus.Failed;
  }
  
  public XFMG_NodeConnectException getNodeConnectException() {
    return nodeConnectException;
  }

  public void setStatusAndNotify(RemoteCallNotificationStatus st) {
    status.set(st);
    cdl.countDown();
  }

  public boolean changeStatusFromExecutingAndNotify(RemoteCallNotificationStatus st) {
    if( status.compareAndSet(RemoteCallNotificationStatus.Execution, st) ) {
      cdl.countDown();
      return true;
    }
    return false;
  }

  public boolean startExecution(String nodeName) {
    this.nodeName = nodeName;
    return status.compareAndSet(RemoteCallNotificationStatus.New, RemoteCallNotificationStatus.Execution);
  }

  public RemoteCallNotificationStatus getStatus() {
    return status.get();
  }

  protected void setOrderId(Long orderId) {
    this.remoteOrderId = orderId;
  }
  
  public Long getRemoteOrderId() {
    return remoteOrderId;
  }

  public void await() throws InterruptedException {
    cdl.await();
  }

  public boolean isSucceeded() {
    return status.get() == RemoteCallNotificationStatus.Succeeded;
  }
  
  public boolean isExecuting() {
    return status.get() == RemoteCallNotificationStatus.Execution;
  }
  
  public boolean remove() {
    return status.compareAndSet(RemoteCallNotificationStatus.New, RemoteCallNotificationStatus.Removed);
  }

  public abstract RemoteCallNotificationStatus execute(FactoryNodeCaller factoryNodeCaller);
  
  
  protected void setSerializedException(SerializableExceptionInformation serializedException) {
    this.serializedException = serializedException;
  }
  
  public XynaException parseSerializedException(String orderType, long revision) {
    if( serializedException == null ) {
      return null;
    }
    Throwable cause = serializedException.recreateThrowable(revision);
    if( cause instanceof XynaException ) {
      return (XynaException)cause;
    } else {
      return new XFMG_NodeRemoteException( nodeName, orderType, cause);
    }
  }

  
}