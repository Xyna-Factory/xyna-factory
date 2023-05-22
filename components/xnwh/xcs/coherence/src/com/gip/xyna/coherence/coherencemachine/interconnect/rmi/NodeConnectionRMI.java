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
package com.gip.xyna.coherence.coherencemachine.interconnect.rmi;



import java.rmi.RemoteException;

import com.gip.xyna.coherence.coherencemachine.CoherenceAction;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.interconnect.InitialConnectionData;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectProtocol;
import com.gip.xyna.coherence.coherencemachine.interconnect.ThreadType;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockAwaitResponse;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockRequestResponse;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.management.NodeInformation;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;



// FIXME prio2: exception handling for all methods
/**
 * "Client" - Implementierung des Interconnects f�r RMI
 */
public class NodeConnectionRMI implements InterconnectProtocol {

  private final GenericRMIAdapter<InterconnectProtocolRemoteInterface> rmiAdapter;

  private final ClassLoader responsibleClassLoader;


  public NodeConnectionRMI(RMIConnectionClientParameters rmiParameters, InterconnectRMIClassLoader classloader)
      throws RMIConnectionFailureException {

    this.responsibleClassLoader = classloader;

    // set the context classloader while setting up the RMI connection: 
    Thread t = Thread.currentThread();
    ClassLoader contextClassLoader = t.getContextClassLoader();
    t.setContextClassLoader(responsibleClassLoader);
    try {
      rmiAdapter =
          new GenericRMIAdapter<InterconnectProtocolRemoteInterface>(rmiParameters.getHostName(), rmiParameters
              .getPort(), rmiParameters.getRMIBindingName());
      rmiAdapter.reconnect();
    } finally {
      t.setContextClassLoader(contextClassLoader);
    }

  }


  private final InterconnectProtocolRemoteInterface getRmiInterface() {
    try {
      return rmiAdapter.getRmiInterface();
    } catch (RMIConnectionFailureException e) {
      throw new RuntimeException(e);
    }
  }


  public LockAwaitResponse awaitLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws ObjectNotInCacheException, InterruptedException {
    try {
      //�ber rmi kann man keine absoluten nanotimeouts �bergeben => relativ berechnen und auf gegenseite r�cktransformieren.
      //FIXME pingzeit mit einberechnen?
      return getRmiInterface().awaitLock(objectId, priority, tryLock, nanoTimeout - System.nanoTime());
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }


  public CoherencePayload executeActions(CoherenceAction actions) {
    try {
      return getRmiInterface().executeActions(actions);
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }


  public void releaseLock(long objectId, long priorityToRelease) throws ObjectNotInCacheException {
    try {
      getRmiInterface().releaseLock(objectId, priorityToRelease);
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }


  public LockRequestResponse requestLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws ObjectNotInCacheException, InterruptedException {
    try {
      //�ber rmi kann man keine absoluten nanotimeouts �bergeben => relativ berechnen und auf gegenseite r�cktransformieren.
      return getRmiInterface().requestLock(objectId, priority, tryLock, nanoTimeout - System.nanoTime());
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }


  public InitialConnectionData connectToClusterRemotely(NodeInformation nodeInformation) {
    try {
      return getRmiInterface().connectToCluster(nodeInformation);
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }


  public void waitForActiveThreads(ThreadType type) {
    try {
      getRmiInterface().waitForActiveThreads(type);
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }


  public void recallLockRequest(long objectId, long priority, long priorityOfLockInOldLockCircle,
                                boolean countedInLockCircle, int lockCircleSize) throws ObjectNotInCacheException {
    try {
      getRmiInterface().recallLockRequest(objectId, priority, priorityOfLockInOldLockCircle, countedInLockCircle,
                                          lockCircleSize);
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }

}
