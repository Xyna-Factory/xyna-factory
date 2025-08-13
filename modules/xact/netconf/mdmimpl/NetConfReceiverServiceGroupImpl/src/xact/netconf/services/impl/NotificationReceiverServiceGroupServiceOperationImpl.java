/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xact.netconf.services.impl;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.NetConfNotificationReceiverSharedLib.InputQueueNetConfMessageElement;
import com.gip.xyna.xact.NetConfNotificationReceiverSharedLib.NetConfNotificationReceiverSharedLib;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;

import base.Text;
import base.math.IntegerNumber;
import xact.connection.Response;
import xact.netconf.datatypes.Status;
import xact.netconf.services.NotificationReceiverServiceGroupServiceOperation;
import xact.templates.Document;


public class NotificationReceiverServiceGroupServiceOperationImpl implements ExtendedDeploymentTask, NotificationReceiverServiceGroupServiceOperation {

  private static final long RequestInterval_Receive = 100; //RequestInterval (in ms) - Receive Response
  private static final String NetConfOperationSuffix = "]]>]]>";


  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public void onUndeployment() throws XynaException {
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public Status containsRDHashfromDeviceID(Text Text_DeviceID) {
    Status ReturnStatus = new Status.Builder().instance();
    try {
      boolean IsContained = NetConfNotificationReceiverSharedLib.containsRDHashfromRDID(Text_DeviceID.getText());
      ReturnStatus.setValid(IsContained);
      ReturnStatus.setStatus("ok");
    } catch (Throwable t) {
      ReturnStatus.setValid(false);
      ReturnStatus.setStatus("containsRDHashfromDeviceID failed");
    }
    return ReturnStatus;
  }


  public Status containsRDHashfromDeviceIP(Text Text_DeviceIP) {
    Status ReturnStatus = new Status.Builder().instance();
    try {
      boolean IsContained = NetConfNotificationReceiverSharedLib.containsRDHashfromRDIP(Text_DeviceIP.getText());
      ReturnStatus.setValid(IsContained);
      ReturnStatus.setStatus("ok");
    } catch (Throwable t) {
      ReturnStatus.setValid(false);
      ReturnStatus.setStatus("containsRDHashfromDeviceID failed");
    }
    return ReturnStatus;
  }


  public Status containsSharedNetConfConnection(Text Text_RDIP) {
    Status ReturnStatus = new Status.Builder().instance();
    try {
      boolean IsContained = NetConfNotificationReceiverSharedLib.containsSharedNetConfConnectionID(Text_RDIP.getText());
      ReturnStatus.setValid(IsContained);
      ReturnStatus.setStatus("ok");
    } catch (Throwable t) {
      ReturnStatus.setValid(false);
      ReturnStatus.setStatus("containsSharedNetConfConnection failed");
    }
    return ReturnStatus;
  }


  public Text getDeviceIDfromDeviceIP(Text Text_DeviceIP) {
    Text ReturnDeviceID = new Text.Builder().instance();
    try {
      ReturnDeviceID.setText(NetConfNotificationReceiverSharedLib.getDeviceIDfromDeviceIP(Text_DeviceIP.getText()));
    } catch (Throwable t) {
    }
    return ReturnDeviceID;
  }


  public Text getDeviceIPfromDeviceID(Text Text_DeviceID) {
    Text ReturnDeviceIP = new Text.Builder().instance();
    try {
      ReturnDeviceIP.setText(NetConfNotificationReceiverSharedLib.getDeviceIPfromDeviceID(Text_DeviceID.getText()));
    } catch (Throwable t) {
    }
    return ReturnDeviceIP;
  }


  public Text getSharedNetConfConnectionID(Text Text_RDIP) {
    Text ReturnConnectionID = new Text.Builder().instance();
    try {
      ReturnConnectionID.setText(NetConfNotificationReceiverSharedLib.getSharedNetConfConnectionID(Text_RDIP.getText()));
    } catch (Throwable t) {
    }
    return ReturnConnectionID;
  }


  public IntegerNumber getTotalNetConfConnections() {
    IntegerNumber ReturnTotalNetConfConnections = new IntegerNumber.Builder().instance();
    try {
      ReturnTotalNetConfConnections.setValue(NetConfNotificationReceiverSharedLib.getTotalNetConfConnections());
    } catch (Throwable t) {
      ReturnTotalNetConfConnections.setValue(0);
    }
    return ReturnTotalNetConfConnections;
  }


  public List<? extends Text> listSharedNetConfConnections() {
    ArrayList<Text> ReturnConnectionList = new ArrayList<Text>();
    try {
      List<String> ListConnections = NetConfNotificationReceiverSharedLib.listSharedNetConfConnection();
      for (Iterator<String> iter = ListConnections.iterator(); iter.hasNext();) {
        String element = iter.next();
        Text ConnectionTextElement = new Text.Builder().instance();
        ConnectionTextElement.setText(element);
        ReturnConnectionList.add(ConnectionTextElement);
      } ;
    } catch (Throwable t) {
    }
    return ReturnConnectionList;
  }


  public Container netConfOperation(Text Text_DeviceID, Document Document_NetConfOperation, Text Text_UniqueMessageUUID, IntegerNumber IntegerNumber_TimeOutInMillis) {
    Response ReturnResponse = new Response();
    Status ReturnStatus = new Status();
    Container ReturnContainer = new Container();
    try {
      String DeviceID = Text_DeviceID.getText();
      String UniqueMessageUUID = Text_UniqueMessageUUID.getText();
      String NetConfOperation = Document_NetConfOperation.getText() + NetConfOperationSuffix;
      long TimeOutInMillis = IntegerNumber_TimeOutInMillis.getValue();

      NetConfNotificationReceiverSharedLib.addOutputQueueNetConfOperation(DeviceID, UniqueMessageUUID, NetConfOperation);

      long MilliSecondsNow = System.currentTimeMillis();
      long MilliSecondsTimeOut = MilliSecondsNow + TimeOutInMillis;
      boolean ResponseReceived = false;
      while ((MilliSecondsNow < MilliSecondsTimeOut) & (!ResponseReceived)) {
        Thread.sleep(RequestInterval_Receive);
        if (NetConfNotificationReceiverSharedLib.containsInputQueueNetConfMessageElement(UniqueMessageUUID)) {
          ResponseReceived = true;
          InputQueueNetConfMessageElement Element = NetConfNotificationReceiverSharedLib.pollInputQueueNetConfMessageElement(UniqueMessageUUID);
          if (Element.isValid() & (Element.getRDID().contains(DeviceID))) {
            ReturnStatus.setValid(true);
            ReturnStatus.setStatus("ok");
            ReturnResponse.setContent(Element.getNetConfMessage());
          } else {
            ReturnStatus.setValid(false);
            ReturnStatus.setStatus("Response from DeviceID " + Element.getRDID() + " failed: " + Element.getMessageID());
            ReturnResponse.setContent(Element.getNetConfMessage());
          }
        }
        MilliSecondsNow = System.currentTimeMillis();
      }
      if (!ResponseReceived) {
        NetConfNotificationReceiverSharedLib.TimeoutNetConfOperation(UniqueMessageUUID);
        ReturnResponse.setContent("");
        ReturnStatus.setValid(false);
        ReturnStatus.setStatus("time out");
      }
    } catch (Throwable t) {
      ReturnResponse.setContent("");
      ReturnStatus.setValid(false);
      ReturnStatus.setStatus("netConfOperation failed");
    }
    ReturnContainer.add(ReturnResponse);
    ReturnContainer.add(ReturnStatus);
    return ReturnContainer;
  }


  public IntegerNumber sizeSharedNetConfConnections() {
    IntegerNumber ReturnSizeSharedNetConfConnections = new IntegerNumber.Builder().instance();
    try {
      ReturnSizeSharedNetConfConnections.setValue(NetConfNotificationReceiverSharedLib.sizeSharedNetConfConnectionID());
    } catch (Throwable t) {
      ReturnSizeSharedNetConfConnections.setValue(0);
    }
    return ReturnSizeSharedNetConfConnections;
  }

}
