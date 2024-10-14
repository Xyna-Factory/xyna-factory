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
package com.gip.xyna.xfmg.xclusteringservices;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.gip.xyna.xfmg.exceptions.XFMG_ClusterConnectionException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.ClusterNodeOnlineButNotExpectingHeartbeatPingException;


public interface RMIClusterProviderInterconnectInterface extends Remote {
  
  /**
   * Knoten A signalisiert an Knoten B, dass er nun auch ins Cluster will (join, restore)
   */
  public void register(String hostname, int port, boolean restore) throws RemoteException, XFMG_ClusterConnectionException;
  
  /**
   * Knoten B signalisiert zurück an Knoten A, dass er im Cluster willkommen ist, beide sind dann CONNECTED
   */
  public void connect() throws RemoteException;
  
  /**
   * knoten, der bereits zum cluster gehört, wird disconnected 
   */
  public void disconnect(String hostname, int port) throws RemoteException;
  
  public void heartbeatPing() throws RemoteException, ClusterNodeOnlineButNotExpectingHeartbeatPingException;

  /**
   *  Knoten B signalisiert nach einem {@link #register(String, int, boolean)} zurück an Knoten A, dass er noch nicht so weit ist, um timeouts beim warten
   *  auf {@link #connect()} zu verhindern
   */
  public void waiting() throws RemoteException;

  public boolean readyForStateChange(ClusterState connected) throws RemoteException;
  
}
