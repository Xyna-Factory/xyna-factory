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


class ConnectionList {

  private static java.util.concurrent.ConcurrentHashMap<String, Socket> TCPblocked = new java.util.concurrent.ConcurrentHashMap<String, Socket>();
  private static java.util.concurrent.ConcurrentHashMap<String, NetConfNotificationReceiverTriggerConnection> connectionList =
      new java.util.concurrent.ConcurrentHashMap<String, NetConfNotificationReceiverTriggerConnection>();
  private static boolean openTCPServer;

  private static void updateSizeBlocked() {
    try {
      NetConfNotificationReceiverSharedLib.setTotalNetConfConnections(sizeBlocked());
    } catch (Throwable t) {
    }
  }


  public static List<String> ListConnectionList() {
    List<String> list = new LinkedList<String>();
    connectionList.forEach((id, object) -> list.add(id));
    return list;
  }


  public static List<String> ListTCPblocked() {
    List<String> list = new LinkedList<String>();
    TCPblocked.forEach((id, object) -> list.add(id));
    return list;
  }


  public static void clearConnectionList() {
    connectionList.clear();
  }


  public static void clearTCPblocked() {
    TCPblocked.clear();
    updateSizeBlocked();
  }


  public static void TriggerOn() {
    openTCPServer = true;
  }


  public static void TriggerOff() {
    openTCPServer = false;
  }


  public static boolean isTriggerOn() {
    return openTCPServer;
  }


  public static void block(String id, Socket socket) {
    TCPblocked.put(id, socket);
    updateSizeBlocked();
  }


  public static boolean isBlocked(String id) {
    return TCPblocked.containsKey(id);
  }


  public static void release(String id) {
    TCPblocked.remove(id);
    updateSizeBlocked();
  }


  public static void addConnection(String id, NetConfNotificationReceiverTriggerConnection conn) {
    connectionList.put(id, conn);
  }


  public static void removeConnection(String id) {
    connectionList.remove(id);
  }


  public static boolean isConnected(String id) {
    return connectionList.containsKey(id);
  }


  public static NetConfNotificationReceiverTriggerConnection getConnection(String id) {
    return connectionList.get(id);
  }


  public static Socket getSocket(String id) {
    return TCPblocked.get(id);
  }


  public static long sizeConnectionList() {
    return ConnectionList.connectionList.size();
  }


  public static long sizeBlocked() {
    return ConnectionList.TCPblocked.size();
  }

}
