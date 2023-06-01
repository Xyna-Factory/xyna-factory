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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.FactoryNodeCaller;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.NotificationProcessor.RemoteCallNotificationStatus;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.RemoteOrderExecution;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.RemoteCallXynaOrderCreationParameter;
import com.gip.xyna.xmcp.SuccesfullOrderExecutionResponse;

/**
 * StartOrderNotification
 * startet einen Auftrag auf dem FactoryNode
 */
public class StartOrderNotification extends RemoteCallNotification {
  private RemoteCallXynaOrderCreationParameter rxocp;
  
  static final Logger logger = CentralFactoryLogging.getLogger(StartOrderNotification.class);
  
  public StartOrderNotification(RemoteCallXynaOrderCreationParameter rxocp) {
    this.rxocp = rxocp;
  }

  public RemoteCallXynaOrderCreationParameter getRemoteXynaOrderCreationParameter() {
    return rxocp;
  }
  
  public RemoteCallNotificationStatus setResponse(String nodeName, OrderExecutionResponse oer) {
    RemoteCallNotificationStatus status = null;
    if (oer.hasExecutedSuccesfully()) {
      setOrderId(((SuccesfullOrderExecutionResponse)oer).getOrderId());
      status = RemoteCallNotificationStatus.Succeeded;
    } else {
      setOrderId(((ErroneousOrderExecutionResponse)oer).getOrderId());
      setSerializedException(((ErroneousOrderExecutionResponse)oer).getExceptionInformation() );
      status = RemoteCallNotificationStatus.Failed;
    }
    return status;
  }


  @Override
  public RemoteCallNotificationStatus execute(FactoryNodeCaller factoryNodeCaller) {
    RemoteOrderExecution remoteOrderExecution = factoryNodeCaller.getRemoteOrderExecution();
    if( remoteOrderExecution.isConnected() ) {
      try {
        OrderExecutionResponse oer = remoteOrderExecution.createOrder(rxocp);
        return setResponse(factoryNodeCaller.getNodeName(), oer);
      } catch (XFMG_NodeConnectException e) {
        factoryNodeCaller.checkConnectivity();
        return setNodeConnectException(e);
      }
    } else {
      return setNodeConnectException( new XFMG_NodeConnectException(remoteOrderExecution.getNodeName()) );
    }
  }


}