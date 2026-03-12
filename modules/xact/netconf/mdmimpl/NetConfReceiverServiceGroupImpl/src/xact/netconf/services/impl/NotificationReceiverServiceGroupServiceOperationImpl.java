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


  public Status containsRDHashfromDeviceID(Text textDeviceID) {
    Status returnStatus = new Status.Builder().instance();
    try {
      boolean isContained = NetConfNotificationReceiverSharedLib.containsRDHashfromRDID(textDeviceID.getText());
      returnStatus.setValid(isContained);
      returnStatus.setStatus("ok");
    } catch (Throwable t) {
      returnStatus.setValid(false);
      returnStatus.setStatus("containsRDHashfromDeviceID failed");
    }
    return returnStatus;
  }


  public Status containsRDHashfromDeviceIP(Text textDeviceIP) {
    Status returnStatus = new Status.Builder().instance();
    try {
      boolean isContained = NetConfNotificationReceiverSharedLib.containsRDHashfromRDIP(textDeviceIP.getText());
      returnStatus.setValid(isContained);
      returnStatus.setStatus("ok");
    } catch (Throwable t) {
      returnStatus.setValid(false);
      returnStatus.setStatus("containsRDHashfromDeviceID failed");
    }
    return returnStatus;
  }


  public Status containsSharedNetConfConnection(Text textRDIP) {
    Status returnStatus = new Status.Builder().instance();
    try {
      boolean isContained = NetConfNotificationReceiverSharedLib.containsSharedNetConfConnectionID(textRDIP.getText());
      returnStatus.setValid(isContained);
      returnStatus.setStatus("ok");
    } catch (Throwable t) {
      returnStatus.setValid(false);
      returnStatus.setStatus("containsSharedNetConfConnection failed");
    }
    return returnStatus;
  }


  public Text getDeviceIDfromDeviceIP(Text textDeviceIP) {
    Text returnDeviceID = new Text.Builder().instance();
    try {
      returnDeviceID.setText(NetConfNotificationReceiverSharedLib.getDeviceIDfromDeviceIP(textDeviceIP.getText()));
    } catch (Throwable t) {
    }
    return returnDeviceID;
  }


  public Text getDeviceIPfromDeviceID(Text textDeviceID) {
    Text returnDeviceIP = new Text.Builder().instance();
    try {
      returnDeviceIP.setText(NetConfNotificationReceiverSharedLib.getDeviceIPfromDeviceID(textDeviceID.getText()));
    } catch (Throwable t) {
    }
    return returnDeviceIP;
  }


  public Text getSharedNetConfConnectionID(Text textRDIP) {
    Text returnConnectionID = new Text.Builder().instance();
    try {
      returnConnectionID.setText(NetConfNotificationReceiverSharedLib.getSharedNetConfConnectionID(textRDIP.getText()));
    } catch (Throwable t) {
    }
    return returnConnectionID;
  }


  public IntegerNumber getTotalNetConfConnections() {
    IntegerNumber returnTotalNetConfConnections = new IntegerNumber.Builder().instance();
    try {
      returnTotalNetConfConnections.setValue(NetConfNotificationReceiverSharedLib.getTotalNetConfConnections());
    } catch (Throwable t) {
      returnTotalNetConfConnections.setValue(0);
    }
    return returnTotalNetConfConnections;
  }


  public List<? extends Text> listSharedNetConfConnections() {
    ArrayList<Text> returnConnectionList = new ArrayList<Text>();
    try {
      List<String> ListConnections = NetConfNotificationReceiverSharedLib.listSharedNetConfConnection();
      for (Iterator<String> iter = ListConnections.iterator(); iter.hasNext();) {
        String element = iter.next();
        Text ConnectionTextElement = new Text.Builder().instance();
        ConnectionTextElement.setText(element);
        returnConnectionList.add(ConnectionTextElement);
      }
    } catch (Throwable t) {
    }
    return returnConnectionList;
  }


  public Container netConfOperation(Text textDeviceID, Document Document_NetConfOperation, Text textUniqueMessageUUID, 
                                    IntegerNumber IntegerNumber_TimeOutInMillis) {
    Response returnResponse = new Response();
    Status returnStatus = new Status();
    Container returnContainer = new Container();
    try {
      String deviceID = textDeviceID.getText();
      String UniqueMessageUUID = textUniqueMessageUUID.getText();
      String NetConfOperation = Document_NetConfOperation.getText() + NetConfOperationSuffix;
      long TimeOutInMillis = IntegerNumber_TimeOutInMillis.getValue();

      NetConfNotificationReceiverSharedLib.addOutputQueueNetConfOperation(deviceID, UniqueMessageUUID, NetConfOperation);

      long MilliSecondsNow = System.currentTimeMillis();
      long MilliSecondsTimeOut = MilliSecondsNow + TimeOutInMillis;
      boolean ResponseReceived = false;
      while ((MilliSecondsNow < MilliSecondsTimeOut) & (!ResponseReceived)) {
        Thread.sleep(RequestInterval_Receive);
        if (NetConfNotificationReceiverSharedLib.containsInputQueueNetConfMessageElement(UniqueMessageUUID)) {
          ResponseReceived = true;
          InputQueueNetConfMessageElement Element = NetConfNotificationReceiverSharedLib.pollInputQueueNetConfMessageElement(UniqueMessageUUID);
          if (Element.isValid() & (Element.getRDID().contains(deviceID))) {
            returnStatus.setValid(true);
            returnStatus.setStatus("ok");
            returnResponse.setContent(Element.getNetConfMessage());
          } else {
            returnStatus.setValid(false);
            returnStatus.setStatus("Response from DeviceID " + Element.getRDID() + " failed: " + Element.getMessageID());
            returnResponse.setContent(Element.getNetConfMessage());
          }
        }
        MilliSecondsNow = System.currentTimeMillis();
      }
      if (!ResponseReceived) {
        NetConfNotificationReceiverSharedLib.TimeoutNetConfOperation(UniqueMessageUUID);
        returnResponse.setContent("");
        returnStatus.setValid(false);
        returnStatus.setStatus("time out");
      }
    } catch (Throwable t) {
      returnResponse.setContent("");
      returnStatus.setValid(false);
      returnStatus.setStatus("netConfOperation failed");
    }
    returnContainer.add(returnResponse);
    returnContainer.add(returnStatus);
    return returnContainer;
  }


  public IntegerNumber sizeSharedNetConfConnections() {
    IntegerNumber returnSizeSharedNetConfConnections = new IntegerNumber.Builder().instance();
    try {
      returnSizeSharedNetConfConnections.setValue(NetConfNotificationReceiverSharedLib.sizeSharedNetConfConnectionID());
    } catch (Throwable t) {
      returnSizeSharedNetConfConnections.setValue(0);
    }
    return returnSizeSharedNetConfConnections;
  }

}
