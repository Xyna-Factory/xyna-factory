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

package com.gip.xyna.xact.NetConfNotificationReceiverSharedLib;


import java.util.LinkedList;
import java.util.List;


public class NetConfNotificationReceiverSharedLib {

  private static long TotalNetConfConnections;

  private static java.util.concurrent.ConcurrentHashMap<String, String> RDIDHashMap =
      new java.util.concurrent.ConcurrentHashMap<String, String>(); //RD_ID, RD_IP
  private static java.util.concurrent.ConcurrentHashMap<String, String> RDIPHashMap =
      new java.util.concurrent.ConcurrentHashMap<String, String>(); //RD_IP, RD_ID
  private static java.util.concurrent.ConcurrentHashMap<String, String> NetConfConnectionIDHashMap =
      new java.util.concurrent.ConcurrentHashMap<String, String>(); //RD_IP, ConnectionID

  private static java.util.concurrent.ConcurrentLinkedQueue<OutputQueueNetConfOperationElement> OutputQueueNetConfOperation =
      new java.util.concurrent.ConcurrentLinkedQueue<OutputQueueNetConfOperationElement>();
  private static java.util.concurrent.ConcurrentHashMap<String, InputQueueNetConfMessageElement> InputQueueNetConfMessageHashMap =
      new java.util.concurrent.ConcurrentHashMap<String, InputQueueNetConfMessageElement>(); //MessageID, InputQueueNetConfMessageElement
  private static java.util.concurrent.ConcurrentHashMap<String, String> InputQueueMessageIDHashMap =
      new java.util.concurrent.ConcurrentHashMap<String, String>(); //MessageID, RD_ID


  public static void addOutputQueueNetConfOperation(String RDID, String MessageID, String NetConfOperation) throws Throwable {
    try {
      if (containsRDHashfromRDID(RDID)) {
        String RD_IP = getDeviceIPfromDeviceID(RDID);
        String ConnectionID = getSharedNetConfConnectionID(RD_IP);
        OutputQueueNetConfOperationElement element =
            new OutputQueueNetConfOperationElement(ConnectionID, RDID, MessageID, NetConfOperation);
        OutputQueueNetConfOperation.add(element);
      } else {
        InputQueueNetConfMessageElement element =
            new InputQueueNetConfMessageElement("", RDID, MessageID, "Interleave-Capability (RFC5277) is not supported", false);
        if (!element.getConnectionID().contains("invalid")) {
          InputQueueNetConfMessageHashMap.put(MessageID, element);
        }
      } ;
    } catch (Throwable t) {
      InputQueueNetConfMessageElement element =
          new InputQueueNetConfMessageElement("", "", MessageID, "addOutputQueueNetConfOperation failed", false);
      if (!element.getConnectionID().contains("invalid")) {
        InputQueueNetConfMessageHashMap.put(MessageID, element);
      }
    }
  }


  public static OutputQueueNetConfOperationElement pollOutputQueueNetConfOperation() throws Throwable {
    return OutputQueueNetConfOperation.poll();
  }


  public static long sizeOutputQueueNetConfOperation() throws Throwable {
    return OutputQueueNetConfOperation.size();
  }


  public static void addInputQueueNetConfMessageElement(String ConnectionID, String RDID, String MessageID, String NetConfMessage,
                                                        boolean valid)
      throws Throwable {
    InputQueueNetConfMessageElement element = new InputQueueNetConfMessageElement(ConnectionID, RDID, MessageID, NetConfMessage, valid);
    InputQueueNetConfMessageHashMap.put(MessageID, element);
  }


  public static void removeInputQueueNetConfMessageElement(String MessageID) throws Throwable {
    InputQueueNetConfMessageHashMap.remove(MessageID);
  }


  public static InputQueueNetConfMessageElement pollInputQueueNetConfMessageElement(String MessageID) throws Throwable {
    InputQueueNetConfMessageElement element = InputQueueNetConfMessageHashMap.get(MessageID);
    InputQueueNetConfMessageHashMap.remove(MessageID);
    return element;
  }


  public static boolean containsInputQueueNetConfMessageElement(String MessageID) throws Throwable {
    return InputQueueNetConfMessageHashMap.containsKey(MessageID);
  }


  public static void TimeoutNetConfOperation(String MessageID) throws Throwable {
    try {
      InputQueueMessageIDHashMap.remove(MessageID);
      InputQueueNetConfMessageHashMap.remove(MessageID);
    } catch (Throwable t) {
    }
  }


  public static List<String> listInputQueueMessageID() throws Throwable {
    List<String> list = new LinkedList<String>();
    InputQueueMessageIDHashMap.forEach((id, object) -> list.add(id));
    return list;
  }


  public static void addInputQueueMessageID(String MessageID, String RD_ID) throws Throwable {
    InputQueueMessageIDHashMap.put(MessageID, RD_ID);
  }


  public static void removeInputQueueMessageID(String MessageID) throws Throwable {
    InputQueueMessageIDHashMap.remove(MessageID);
  }


  public static boolean containsInputQueueMessageID(String MessageID) throws Throwable {
    return InputQueueMessageIDHashMap.containsKey(MessageID);
  }


  public static String getRDIDfromInputQueueMessageID(String MessageID) throws Throwable {
    return InputQueueMessageIDHashMap.get(MessageID);
  }


  public static void addRDHash(String RDIDHash, String RD_IP) throws Throwable {
    try {
      if (RDIDHashMap.containsKey(RDIDHash)) {
        RDIPHashMap.remove(RDIDHashMap.get(RDIDHash));
        RDIDHashMap.remove(RDIDHash);
      }
    } catch (Throwable t) {
    }
    RDIDHashMap.put(RDIDHash, RD_IP);
    RDIPHashMap.put(RD_IP, RDIDHash);
  }


  public static void removeRDHash(String RDIDHash) throws Throwable {
    String RD_IP = RDIDHashMap.get(RDIDHash);
    RDIPHashMap.remove(RD_IP);
    RDIDHashMap.remove(RDIDHash);
  }


  public static String getDeviceIPfromDeviceID(String RDIDHash) throws Throwable {
    return RDIDHashMap.get(RDIDHash);
  }


  public static String getDeviceIDfromDeviceIP(String RD_IP) throws Throwable {
    return RDIPHashMap.get(RD_IP);
  }


  public static boolean containsRDHashfromRDIP(String RD_IP) throws Throwable {
    return RDIPHashMap.containsKey(RD_IP);
  }


  public static boolean containsRDHashfromRDID(String RDID) throws Throwable {
    return RDIDHashMap.containsKey(RDID);
  }


  public static void addSharedNetConfConnectionID(String RD_IP, String ConnectionID) throws Throwable {
    NetConfConnectionIDHashMap.put(RD_IP, ConnectionID);
  }


  public static void removeSharedNetConfConnectionID(String IP) throws Throwable {
    NetConfConnectionIDHashMap.remove(IP);
  }


  public static String getSharedNetConfConnectionID(String IP) throws Throwable {
    return NetConfConnectionIDHashMap.get(IP);
  }


  public static boolean containsSharedNetConfConnectionID(String IP) throws Throwable {
    return NetConfConnectionIDHashMap.containsKey(IP);
  }


  public static long sizeSharedNetConfConnectionID() throws Throwable {
    return NetConfConnectionIDHashMap.size();
  }


  public static List<String> listSharedNetConfConnection() throws Throwable {
    List<String> list = new LinkedList<String>();
    NetConfConnectionIDHashMap.forEach((id, object) -> list.add(id));
    return list;
  }


  public static void setTotalNetConfConnections(long TotalConn) throws Throwable {
    TotalNetConfConnections = TotalConn;
  }


  public static long getTotalNetConfConnections() throws Throwable {
    return TotalNetConfConnections;
  }

}
