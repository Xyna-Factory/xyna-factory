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

package com.gip.xyna.xact.trigger;


import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.NetConfNotificationReceiverSharedLib.NetConfNotificationReceiverSharedLib;
import com.gip.xyna.xact.NetConfNotificationReceiverSharedLib.OutputQueueNetConfOperationElement;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;


public class NetConfNotificationReceiverTrigger extends EventListener<NetConfNotificationReceiverTriggerConnection, NetConfNotificationReceiverStartParameter> {

  private static Logger logger = CentralFactoryLogging.getLogger(NetConfNotificationReceiverTrigger.class);

  private NetConfNotificationReceiverStartParameter startParameter;
  private Integer port;
  private String filter_targetWF;

  private long whilewait_CloseConnectionList;
  private long whilewait_NetConfOperation;
  private long queuewait;

  private TCPConnection TCPconn;


  public NetConfNotificationReceiverTrigger() {
  }


  private String getRDIPfromSocketID(String SocketID) throws Throwable {
    return SocketID.substring(0, SocketID.indexOf(":"));
  }


  private String getOldConnection(String SocketID) {
    String OldConnectionID = "";
    try {
      String RDIP = getRDIPfromSocketID(SocketID);
      if (NetConfNotificationReceiverSharedLib.containsSharedNetConfConnectionID(RDIP)) {
        OldConnectionID = NetConfNotificationReceiverSharedLib.getSharedNetConfConnectionID(RDIP);
      }
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: " + "getOldConnection failed", t);
    }
    return OldConnectionID;
  }


  private void TCPServerListener() throws Exception {

    logger.info("NetConfNotificationReceiver: " + "Listening on port " + this.port + "...");
    while ((!this.TCPconn.isClosed()) & (ConnectionList.isTriggerOn())) {
      logger.info("NetConfNotificationReceiver: " + "Waiting to accept connection...");
      String SocketID = this.TCPconn.accept();
      if (SocketID.contains("blocked")) {
        logger.info("NetConfNotificationReceiver: " + "Socket blocked connection ID: " + SocketID);
      } else {
        logger.info("NetConfNotificationReceiver: " + "Accepted connection ID: " + SocketID);

        String OldConnectionID = getOldConnection(SocketID); // Only one connection per RD_IP allowed (e.g. uncontrolled reboot of RD)

        Thread t = new Thread() {

          public void run() {
            new NetConfNotificationReceiverTriggerConnection(SocketID, filter_targetWF, OldConnectionID);
          }
        };
        t.start();

        logger.info("NetConfNotificationReceiver: " + "Again Listening ...");
      } ;
    }
    logger.info("NetConfNotificationReceiver: " + "TCP Listener closed!");
  }


  private void startServerTCP() throws Exception {
    logger.info("NetConfNotificationReceiver: " + "Start TCP Listener on port " + this.port + "...");
    this.TCPconn = new TCPConnection(this.port);

    Thread t = new Thread() {

      public void run() {
        try {
          TCPServerListener();
        } catch (Exception ex) {
          if (ConnectionList.isTriggerOn()) {
            logger.warn("NetConfNotificationReceiver: " + "TCPServerListener failed", ex);
          }
        }
      }
    };
    t.start();
  }


  public void closeServerTCP() throws Exception {
    logger.info("NetConfNotificationReceiver: " + "CLOSE TCP Listener on port " + this.port + "...");
    this.TCPconn.close();
  }


  private void OutputQueueNetConfOperationListener() throws Exception {
    while ((!this.TCPconn.isClosed()) & (ConnectionList.isTriggerOn())) {
      try {
        Thread.sleep(whilewait_NetConfOperation);
        while (NetConfNotificationReceiverSharedLib.sizeOutputQueueNetConfOperation() > 0) {
          OutputQueueNetConfOperationElement element = NetConfNotificationReceiverSharedLib.pollOutputQueueNetConfOperation();
          if (element.isValid()) {
            if (ConnectionList.isConnected(element.getConnectionID())) {
              NetConfNotificationReceiverTriggerConnection conn = ConnectionList.getConnection(element.getConnectionID());
              conn.sendNetConfOperation(element.getNetConfOperation());
              NetConfNotificationReceiverSharedLib.addInputQueueMessageID(element.getMessageID(), element.getRDID());
            } else {
              NetConfNotificationReceiverSharedLib.addInputQueueNetConfMessageElement("", element.getRDID(), element.getMessageID(),
                                                                                      "Invalid ConnectionID", false);
            }
          } else {
            NetConfNotificationReceiverSharedLib.addInputQueueNetConfMessageElement("", element.getRDID(), element.getMessageID(),
                                                                                    "Invalid OutputQueueNetConfOperationElement", false);
          }
        }
      } catch (Throwable t) {
        logger.warn("NetConfNotificationReceiver: " + "OutputQueueNetConfOperationListener failed", t);
      }

    }
  }


  private void startOutputQueueNetConfOperationListener() throws Exception {
    Thread t = new Thread() {

      public void run() {
        try {
          OutputQueueNetConfOperationListener();
        } catch (Exception ex) {
          if (ConnectionList.isTriggerOn()) {
            logger.warn("NetConfNotificationReceiver: " + "OutputQueueNetConfOperationListener failed", ex);
          }
        }
      }
    };
    t.start();
  }


  public void start(NetConfNotificationReceiverStartParameter sp) throws XACT_TriggerCouldNotBeStartedException {
    try {
      startParameter = sp;
      this.port = sp.getPort();
      NetConfNotificationReceiverCredentials.setUserame(sp.getUsername());
      NetConfNotificationReceiverCredentials.setPassword(sp.getPassword());
      NetConfNotificationReceiverCredentials.setHostKeyAuthenticationMode(sp.getHostKeyAuthenticationMode());
      NetConfNotificationReceiverCredentials.setReplayInMinutes(sp.getReplayInMinutes());
      this.filter_targetWF = sp.getFilterTargetWF();
      this.whilewait_CloseConnectionList = NetConfNotificationReceiverStartParameter.CloseConnectionList_RequestInterval;
      this.queuewait = NetConfNotificationReceiverStartParameter.Receive_RequestInterval;
      this.whilewait_NetConfOperation = NetConfNotificationReceiverStartParameter.Receive_NetConfOperation;

      ConnectionList.TriggerOn();
      this.startServerTCP();
      this.startOutputQueueNetConfOperationListener();

    } catch (Exception ex) {
      logger.warn("NetConfNotificationReceiver: " + "XACT_TriggerCouldNotBeStartedException", ex);
      throw new XACT_TriggerCouldNotBeStartedException(ex) {
        private static final long serialVersionUID = 1L;
      };
    }

  }


  public NetConfNotificationReceiverTriggerConnection receive() {

    NetConfNotificationReceiverTriggerConnection conn = null;

    try {

      logger.debug("NetConfNotificationReceiver: " + "Receive - Wait ...");

      if (ConnectionList.isTriggerOn()) {
        while (ConnectionQueue.size() == 0) {
          try {
            Thread.sleep(queuewait);
          } catch (Exception ex) {
            logger.warn("NetConfNotificationReceiver: receive - sleep failed", ex);
          }
        }
        logger.debug("NetConfNotificationReceiver: " + "Receive - GET ...");
        conn = ConnectionQueue.get();
        logger.debug("NetConfNotificationReceiver: " + "#ConnectionQueue: " + ConnectionQueue.size());
      }
    } catch (Exception ex) {
      logger.warn("NetConfNotificationReceiver: " + "receive failed", ex);
    }

    return conn;
  }


  /**
   * Called by Xyna Processing if there are not enough system capacities to process the request.
   */
  protected void onProcessingRejected(String cause, NetConfNotificationReceiverTriggerConnection con) {
  }


  /**
   * called by Xyna Processing to stop the Trigger.
   * should make sure, that start() may be called again directly afterwards. connection instances
   * returned by the method receive() should not be expected to work after stop() has been called.
   */

  public void stop() throws XACT_TriggerCouldNotBeStoppedException {


    try {
      ConnectionList.TriggerOff();
      List<String> ItemList = ConnectionList.ListConnectionList();
      for (Iterator<String> iter = ItemList.iterator(); iter.hasNext();) {
        String element = iter.next();
        Thread t = new Thread() {

          public void run() {
            NetConfNotificationReceiverTriggerConnection conn = ConnectionList.getConnection(element);
            conn.close_connection();
          }
        };
        t.start();
      }

      logger.info("NetConfNotificationReceiver: " + "Wait to CloseServerTCP ... " + ConnectionList.sizeConnectionList());
      while (ConnectionList.sizeConnectionList() > 0) {
        try {
          Thread.sleep(whilewait_CloseConnectionList);
        } catch (Exception ex) {
          logger.warn("NetConfNotificationReceiver: stop - sleep failed", ex);
        }
      }
      this.closeServerTCP();

      logger.info("NetConfNotificationReceiver: " + "Trigger stopped");

    } catch (Exception ex) {
      logger.warn("NetConfNotificationReceiver: " + "Trigger stop failed", ex);
      throw new RuntimeException("Problems stopping NetConfNotificationReceiver: " + ex);
    }

  }


  /**
   * called when a triggerconnection generated by this trigger was not accepted by any filter
   * registered to this trigger
   * @param con corresponding triggerconnection
   */
  public void onNoFilterFound(NetConfNotificationReceiverTriggerConnection con) {
    try {
      logger.debug("NetConfNotificationReceiver: " + "No Filter found ... #messages: " + con.message_size());

      String ausgabe = con.getMessage();
      logger.debug("NetConfNotificationReceiver: " + "Message: " + ausgabe);
      logger.debug("NetConfNotificationReceiver: " + "... #messages: " + con.message_size());
    } catch (Exception ex) {
      logger.warn("NetConfNotificationReceiver: " + "onNoFilterFound failed", ex);
    }
  }


  /**
   * @return description of this trigger
   */
  public String getClassDescription() {
    return "NetConf Notification Receiver";
  }

}
