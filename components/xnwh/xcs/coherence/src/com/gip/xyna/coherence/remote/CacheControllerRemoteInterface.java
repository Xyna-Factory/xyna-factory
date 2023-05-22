/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

package com.gip.xyna.coherence.remote;



import java.rmi.Remote;
import java.rmi.RemoteException;

import com.gip.xyna.coherence.coherencemachine.CoherenceObjectCompleteState;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;



public interface CacheControllerRemoteInterface extends Remote {


  // TODO prio2 configurable
  public static final int DEFAULT_REMOTE_INTERFACE_PORT = 1095;


  public long create(CoherencePayload payload) throws RemoteException;


  public CoherencePayload read(long objectId) throws RemoteException;


  public void update(long objectId, CoherencePayload payload) throws RemoteException;


  public void delete(long objectId) throws RemoteException;


  public void lock(long objectId, long sessionId) throws RemoteException;


  public void unlock(long objectId, long sessionId) throws RemoteException;


  public void pauseCluster() throws RemoteException;


  public void unpauseCluster() throws RemoteException;


  public CoherenceObjectCompleteState getCompleteObjectState(long objectId) throws RemoteException;


  public void executeRemoteRunnable(RemoteRunnable r) throws RemoteException;

}
