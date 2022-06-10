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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteData;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.RemoteCallXynaOrderCreationParameter;


public interface RemoteOrderExecutionInterface extends Remote {
  
  public final static String BINDING_NAME = "RemoteOrderExecutionInterface";

  
  public OrderExecutionResponse createOrder(XynaCredentials credentials, 
                                            String identifier,
                                            RemoteCallXynaOrderCreationParameter creationParameter, 
                                            TransactionMode mode) throws RemoteException;
  
  public List<RemoteData> awaitData(XynaCredentials credentials, String identifier, long timeoutMillis ) throws RemoteException;
  
  public int getRunningCount(XynaCredentials credentials, String identifier ) throws RemoteException;

  public List<Long> checkRunningOrders(XynaCredentials credentials, String identifier, List<Long> orderIds ) throws RemoteException;
  
  public void abortCommunication(XynaCredentials credentials, String identifier)  throws RemoteException;
  
  public static enum TransactionMode {
    // return from call as soon as order is
    CREATION, // created
    START, // started
    ACK; // acknowledged
  }
  
}
