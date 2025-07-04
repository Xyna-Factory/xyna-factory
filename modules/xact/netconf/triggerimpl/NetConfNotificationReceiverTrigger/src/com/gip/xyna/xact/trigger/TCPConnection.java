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


import com.gip.xyna.CentralFactoryLogging;
import org.apache.log4j.Logger;

import java.lang.Exception;
import java.net.ServerSocket;
import java.net.Socket;


public class TCPConnection {

  private static Logger logger = CentralFactoryLogging.getLogger(NetConfNotificationReceiverTriggerConnection.class);

  private Integer port;
  private ServerSocket serverSocket;


  public TCPConnection(Integer port) throws Exception {
    this.port = port;
    this.serverSocket = null;
    try {
      this.serverSocket = new ServerSocket(this.port);
    } catch (Exception ex) {
      logger.warn("NetConfNotificationReceiver: " + "Initialization of ServerSocket failed", ex);
      throw ex;
    }
  }


  public boolean isClosed() throws Exception {
    return this.serverSocket.isClosed();
  }


  public String accept() {
    String SocketID = null;
    try {
      Socket socket = this.serverSocket.accept();

      SocketID = socket.getInetAddress().toString().replace("/", "") + ":" + socket.getPort() + "_" + socket.getLocalPort();

      if ((ConnectionList.isBlocked(SocketID)) | (!ConnectionList.isTriggerOn())) {
        socket.close();
        SocketID = SocketID+"_blocked";
      } else {
        ConnectionList.block(SocketID, socket);
      }

    } catch (Exception ex) {
      if (ConnectionList.isTriggerOn()) {
        logger.warn("NetConfNotificationReceiver: " + "Socket accept failed", ex);
      }
    }
    return SocketID;
  }


  public void close() {
    try {
      this.serverSocket.close();
    } catch (Exception ex) {
      logger.warn("NetConfNotificationReceiver: " + "Socket close failed", ex);
    }
  }

}
