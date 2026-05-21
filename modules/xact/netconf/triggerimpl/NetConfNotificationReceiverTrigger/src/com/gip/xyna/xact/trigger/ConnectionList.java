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


import java.net.Socket;
import java.util.List;
import java.util.LinkedList;
import com.gip.xyna.xact.NetConfNotificationReceiverSharedLib.NetConfNotificationReceiverSharedLib;
import java.util.concurrent.ConcurrentHashMap;


public class ConnectionList {

  private ConcurrentHashMap<String, Socket> tcpBlocked = new ConcurrentHashMap<String, Socket>();
  private ConcurrentHashMap<String, NetConfNotificationReceiverTriggerConnection> connectionList =
      new ConcurrentHashMap<String, NetConfNotificationReceiverTriggerConnection>();
  private boolean openTCPServer;

  
  private void updateSizeBlocked() {
    try {
      NetConfNotificationReceiverSharedLib.setTotalNetConfConnections(sizeBlocked());
    } catch (Throwable t) {
    }
  }


  public List<String> listConnectionList() {
    List<String> list = new LinkedList<String>();
    connectionList.forEach((id, object) -> list.add(id));
    return list;
  }


  public List<String> listTCPblocked() {
    List<String> list = new LinkedList<String>();
    tcpBlocked.forEach((id, object) -> list.add(id));
    return list;
  }


  public void clearConnectionList() {
    connectionList.clear();
  }


  public void clearTCPblocked() {
    tcpBlocked.clear();
    updateSizeBlocked();
  }


  public void triggerOn() {
    openTCPServer = true;
  }


  public void triggerOff() {
    openTCPServer = false;
  }


  public boolean isTriggerOn() {
    return openTCPServer;
  }


  public void block(String id, Socket socket) {
    tcpBlocked.put(id, socket);
    updateSizeBlocked();
  }


  public boolean isBlocked(String id) {
    return tcpBlocked.containsKey(id);
  }


  public void release(String id) {
    tcpBlocked.remove(id);
    updateSizeBlocked();
  }


  public void addConnection(String id, NetConfNotificationReceiverTriggerConnection conn) {
    connectionList.put(id, conn);
  }


  public void removeConnection(String id) {
    connectionList.remove(id);
  }


  public boolean isConnected(String id) {
    return connectionList.containsKey(id);
  }


  public NetConfNotificationReceiverTriggerConnection getConnection(String id) {
    return connectionList.get(id);
  }


  public Socket getSocket(String id) {
    return tcpBlocked.get(id);
  }


  public long sizeConnectionList() {
    return connectionList.size();
  }


  public long sizeBlocked() {
    return tcpBlocked.size();
  }

}
