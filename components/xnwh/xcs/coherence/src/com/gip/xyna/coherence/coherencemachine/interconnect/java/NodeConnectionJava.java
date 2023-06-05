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
package com.gip.xyna.coherence.coherencemachine.interconnect.java;



import com.gip.xyna.coherence.coherencemachine.CoherenceAction;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.interconnect.InitialConnectionData;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectProtocol;
import com.gip.xyna.coherence.coherencemachine.interconnect.ThreadType;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockAwaitResponse;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockRequestResponse;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.management.NodeInformation;



public class NodeConnectionJava implements InterconnectProtocol {

  InterconnectCalleeJava targetNode;


  public NodeConnectionJava(InterconnectCalleeJava targetNode) {
    this.targetNode = targetNode;
  }


  public LockAwaitResponse awaitLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws ObjectNotInCacheException, InterruptedException {
    return targetNode.awaitLock(objectId, priority, tryLock, nanoTimeout);
  }


  public CoherencePayload executeActions(CoherenceAction actions) {
    return targetNode.executeActions(actions);
  }


  public void releaseLock(long objectId, long priorityToRelease) throws ObjectNotInCacheException {
    targetNode.releaseLock(objectId, priorityToRelease);
  }


  public LockRequestResponse requestLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws ObjectNotInCacheException, InterruptedException {
    return targetNode.requestLock(objectId, priority, tryLock, nanoTimeout);
  }


  public InitialConnectionData connectToClusterRemotely(NodeInformation nodeInformation) {
    return targetNode.connectToClusterRemotely(nodeInformation);
  }


  public void waitForActiveThreads(ThreadType type) {
    targetNode.waitForActiveThreads(type);
  }


  public void recallLockRequest(long objectId, long priority, long priorityOfLockInOldLockCircle,
                                boolean countedInLockCircle, int lockCircleSize) throws ObjectNotInCacheException {
    targetNode.recallLockRequest(objectId, priority, priorityOfLockInOldLockCircle, countedInLockCircle, lockCircleSize);
  }

}
