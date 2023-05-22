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
package com.gip.xyna.coherence.coherencemachine.interconnect;

import org.apache.log4j.Logger;

import com.gip.xyna.coherence.coherencemachine.CoherenceAction;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCalleeAlgorithmDisconnected.QueuedRequest;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockAwaitResponse;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockRequestResponse;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.management.NodeInformation;
import com.gip.xyna.coherence.utils.debugging.Debugger;
import com.gip.xyna.coherence.utils.logging.LoggerFactory;


/**
 * Defines the interface (connection) over which a cluster nodes is reachable for other nodes.
 */
public abstract class InterconnectCallee implements InterconnectProtocol {

  private static final Logger logger = LoggerFactory.getLogger(InterconnectCallee.class);
  private static final Debugger debugger = Debugger.getDebugger();
  
  private volatile InterconnectCalleeAlgorithm calleeAlgorithm;
  private InterconnectProtocol impl;

  public InterconnectCallee(InterconnectProtocol impl) {
    this.impl = impl;
  }

  public abstract void initInternally();


  /**
   * callee ist nun zwar erreichbar, aber beantwortet alle remote anfragen mit ungef�hrlichen pseudoantworten solange bis initconnected aufgerufen worden ist.
   */
  public final void initNotConnected() {
    calleeAlgorithm = new InterconnectCalleeAlgorithmDisconnected(impl);
    initInternally();
  }

  /**
   * ab sofort k�nnen alle remote requests vom {@link InterconnectProtocol} verarbeitet werden. vorher werden alle angesammelten anfragen bearbeitet.
   */
  public final void initConnected() {
    Object lock = ((InterconnectCalleeAlgorithmDisconnected) calleeAlgorithm).lock;
    synchronized (lock) {
      for (QueuedRequest qr : ((InterconnectCalleeAlgorithmDisconnected) calleeAlgorithm).getQueuedRequests()) {
        qr.execute(impl);
      }
      // notify threads waiting in connectToCluster
      ((InterconnectCalleeAlgorithmDisconnected) calleeAlgorithm).connected = true;
      lock.notifyAll();
      calleeAlgorithm = new InterconnectCalleeAlgorithmConnected(impl);
    }
    if (debugger.isEnabled()) {
      debugger.debug("initConnected finished. new requests can be forwarded directly");
    }
  }


  public abstract void shutdown();


  public LockRequestResponse requestLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws ObjectNotInCacheException, InterruptedException {
    return calleeAlgorithm.requestLock(objectId, priority, tryLock, nanoTimeout);
  }


  public CoherencePayload executeActions(CoherenceAction actions) {
    return calleeAlgorithm.executeActions(actions);
  }


  public void releaseLock(long objectId, long priorityToRelease) throws ObjectNotInCacheException {
    calleeAlgorithm.releaseLock(objectId, priorityToRelease);
  }


  public LockAwaitResponse awaitLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws ObjectNotInCacheException, InterruptedException {
    return calleeAlgorithm.awaitLock(objectId, priority, tryLock, nanoTimeout);
  }


  public InitialConnectionData connectToClusterRemotely(NodeInformation nodeInformation) {
    return calleeAlgorithm.connectToClusterRemotely(nodeInformation);
  }


  public void waitForActiveThreads(ThreadType type) {
    calleeAlgorithm.waitForActiveThreads(type);
  }


  public void recallLockRequest(long objectId, long priority, long priorityOfLockInOldLockCircle,
                                boolean countedInLockCircle, int lockCircleSize) throws ObjectNotInCacheException {
    calleeAlgorithm.recallLockRequest(objectId, priority, priorityOfLockInOldLockCircle, countedInLockCircle, lockCircleSize);
  }

}
