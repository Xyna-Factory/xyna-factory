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



import java.rmi.Remote;
import java.rmi.RemoteException;

import com.gip.xyna.coherence.coherencemachine.CoherenceAction;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.interconnect.InitialConnectionData;
import com.gip.xyna.coherence.coherencemachine.interconnect.ThreadType;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockAwaitResponse;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockRequestResponse;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.management.NodeInformation;



public interface InterconnectProtocolRemoteInterface extends Remote {

  public LockAwaitResponse awaitLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws RemoteException, ObjectNotInCacheException, InterruptedException;


  public CoherencePayload executeActions(CoherenceAction actions) throws RemoteException;


  public void releaseLock(long objectId, long priorityToRelease) throws RemoteException, ObjectNotInCacheException;


  public LockRequestResponse requestLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws RemoteException, ObjectNotInCacheException, InterruptedException;


  public InitialConnectionData connectToCluster(NodeInformation nodeInformation) throws RemoteException;


  public void waitForActiveThreads(ThreadType type) throws RemoteException;


  public void recallLockRequest(long objectId, long priority, long priorityOfLockInOldLockCircle,
                                boolean countedInLockCircle, int lockCircleSize) throws RemoteException, ObjectNotInCacheException;
}
