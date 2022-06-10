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

package com.gip.xyna.coherence.coherencemachine.interconnect;

import com.gip.xyna.coherence.coherencemachine.CoherenceAction;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockAwaitResponse;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockRequestResponse;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.management.NodeInformation;


/**
 * This implementation simply redirects the requests to the actual implementation
 */
public class InterconnectCalleeAlgorithmConnected extends InterconnectCalleeAlgorithm {


  private final InterconnectProtocol impl;


  public InterconnectCalleeAlgorithmConnected(InterconnectProtocol impl) {
    this.impl = impl;
  }


  public LockRequestResponse requestLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws ObjectNotInCacheException, InterruptedException {
    return impl.requestLock(objectId, priority, tryLock, nanoTimeout);
  }


  public CoherencePayload executeActions(CoherenceAction actions) {
    return impl.executeActions(actions);
  }


  public void releaseLock(long objectId, long priorityToRelease) throws ObjectNotInCacheException {
    impl.releaseLock(objectId, priorityToRelease);
  }


  public LockAwaitResponse awaitLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws ObjectNotInCacheException, InterruptedException {
    return impl.awaitLock(objectId, priority, tryLock, nanoTimeout);
  }


  public InitialConnectionData connectToClusterRemotely(NodeInformation nodeInformation) {
    return impl.connectToClusterRemotely(nodeInformation);
  }


  public void waitForActiveThreads(ThreadType type) {
    impl.waitForActiveThreads(type);
  }


  public void recallLockRequest(long objectId, long priority, long priorityOfLockInOldLockCircle,
                                boolean countedInLockCircle, int lockCircleSize) throws ObjectNotInCacheException {
    impl.recallLockRequest(objectId, priority, priorityOfLockInOldLockCircle, countedInLockCircle, lockCircleSize);
  }

}
