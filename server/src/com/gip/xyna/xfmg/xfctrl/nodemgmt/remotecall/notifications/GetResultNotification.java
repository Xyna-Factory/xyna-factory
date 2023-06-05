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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.notifications;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.FactoryNodeCaller;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.NotificationProcessor.RemoteCallNotificationStatus;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.SuccesfullOrderExecutionResponse;
import com.gip.xyna.xmcp.SynchronousSuccesfullOrderExecutionResponse;
import com.gip.xyna.xmcp.SynchronousSuccessfullRemoteOrderExecutionResponse;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.remotecallserialization.XynaXmomSerialization;

/**
 * GetResultNotification
 * Holt Response ab
 */
public class GetResultNotification extends RemoteCallNotification {
  
  private SuccesfullOrderExecutionResponse ssoer;
  private static XynaXmomSerialization serialization = XynaFactory.getInstance().getProcessing().getXmomSerialization();
  
  public GetResultNotification(Long remoteOrderId) {
    setOrderId(remoteOrderId);
  }

  public RemoteCallNotificationStatus setResponse(String nodeName, OrderExecutionResponse oer) {
    if (oer.hasExecutedSuccesfully()) {
      this.ssoer = (SuccesfullOrderExecutionResponse)oer;
      return RemoteCallNotificationStatus.Succeeded;
    } else {
      setSerializedException(((ErroneousOrderExecutionResponse)oer).getExceptionInformation());
      return RemoteCallNotificationStatus.Failed;
    }
  }

  public GeneralXynaObject getXynaObject(XynaOrderServerExtension correlatedXynaOrder, Long remoteCallAppRevision) throws XPRC_XmlParsingException, XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    if(ssoer instanceof SynchronousSuccesfullOrderExecutionResponse) {
      SynchronousSuccesfullOrderExecutionResponse response = (SynchronousSuccesfullOrderExecutionResponse)ssoer;
      if (response.getPayloadXML() == null) {
        return (XynaObject) response.getResponse();
      } else if (response.getPayloadXML().isEmpty()) {
        return new Container();
      } else {
        return XynaObject.generalFromXml(response.getPayloadXML(), correlatedXynaOrder.getRootOrder().getRevision());
      }      
    } else if (ssoer instanceof SynchronousSuccessfullRemoteOrderExecutionResponse) {
      SynchronousSuccessfullRemoteOrderExecutionResponse response = (SynchronousSuccessfullRemoteOrderExecutionResponse) ssoer;
      GeneralXynaObject result = serialization.deserialize(remoteCallAppRevision, response.getFqn(), response.getPayload());
      return result; //important: deserialize using correlatedXynaOrder - revision and not createdRevision from response!
    } else {
      throw new RuntimeException("Unexpected SuccessfullOrderExecutionResponse: " + ssoer.getClass());
    }
    

  }

  @Override
  public RemoteCallNotificationStatus execute(FactoryNodeCaller factoryNodeCaller) {
    OrderExecutionResponse oer = factoryNodeCaller.removeResponse(getRemoteOrderId());
    return setResponse(factoryNodeCaller.getNodeName(), oer);
  }

}