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

package com.gip.xyna.coherence.coherencemachine.interconnect;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.coherence.coherencemachine.CoherenceAction;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockAwaitResponse;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockRequestResponse;
import com.gip.xyna.coherence.exceptions.ClusterInconsistentException;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.management.NodeInformation;
import com.gip.xyna.coherence.utils.debugging.Debugger;


/**
 * sammelt anfragen und gibt sie erst zur implementierung weiter, wenn der callee voll verbunden wird
 */
public class InterconnectCalleeAlgorithmDisconnected extends InterconnectCalleeAlgorithm {

 // private static final Logger logger = LoggerFactory.getLogger(InterconnectCalleeAlgorithmDisconnected.class);
  private static final Debugger debugger = Debugger.getDebugger();
  
  private List<QueuedRequest> queue = new ArrayList<QueuedRequest>();
  Object lock = new Object();
  private final InterconnectProtocol impl;
  boolean connected;


  public InterconnectCalleeAlgorithmDisconnected(InterconnectProtocol impl) {
    this.impl = impl;
  }


  public LockRequestResponse requestLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws ObjectNotInCacheException, InterruptedException {
    synchronized (lock) {
      if (connected) {
        return impl.requestLock(objectId, priority, tryLock, nanoTimeout);
      } else {
        queue.add(new QRRequestLock(objectId, priority, tryLock, nanoTimeout));
        return new LockRequestResponse(InterconnectProtocol.SUCCESSFUL_LOCK_NO_COMPARISON_REQ, 0);
      }
    }
  }


  public CoherencePayload executeActions(CoherenceAction actions) {
    synchronized (lock) {
      if (connected) {
        return impl.executeActions(actions);
      } else {
        queue.add(new QRExecuteActions(actions));
        return null;
      }
    }
  }


  public void releaseLock(long objectId, long priorityToRelease)
      throws ObjectNotInCacheException {
    synchronized (lock) {
      if (connected) {
        impl.releaseLock(objectId, priorityToRelease);
      } else {
        queue.add(new QRReleaseLock(objectId, priorityToRelease));
      }
    }
  }


  public void waitForActiveThreads(ThreadType type) {
    synchronized (lock) {
      if (connected) {
        impl.waitForActiveThreads(type);
      } else {
        queue.add(new QRWaitForActiveThreads(type));
      }
    }
  }


  public LockAwaitResponse awaitLock(long objectId, long priority, boolean tryLock, long nanoTimeout) throws ObjectNotInCacheException, InterruptedException {
    // nothing to be done: this should not happen anyway since all lock requests will be answered by LOCK_SUCCESSFUL
    // TODO prio4: throw a ClusterInconsistentException if this arrives when not connected?
    synchronized (lock) {
      if (connected) {
        return impl.awaitLock(objectId, priority, tryLock, nanoTimeout);
      } else {
        throw new ClusterInconsistentException("awaitLock was called while not connected");
      }
    }
  }


  public InitialConnectionData connectToClusterRemotely(NodeInformation nodeInformation) {
    //TODO wieso hier nich auch einfach zur queue hinzufügen?
    synchronized (lock) {
      // wait in a loop to make sure that the connected flag has been set in the meantime
      while (!connected) {
        try {
          lock.wait(1000);
        } catch (InterruptedException e) {
          throw new RuntimeException("Got interrupted unexpectedly", e);
        }
      }
    }
    return impl.connectToClusterRemotely(nodeInformation);
  }


  public static abstract class QueuedRequest {

    public abstract void execute(InterconnectProtocol calleeImpl);

  }


  private static class QRExecuteActions extends QueuedRequest {

    CoherenceAction actions;


    public QRExecuteActions(CoherenceAction actions) {
      this.actions = actions;
    }


    @Override
    public void execute(InterconnectProtocol calleeImpl) {
      if (debugger.isEnabled()) {
        debugger.debug(new Object() {
          public String toString() {
            return "executeActions from queue: execute " + actions.getActionType() + " on " + actions.getTargetObjectID();
          }
        });
      }
      calleeImpl.executeActions(actions);
    }
    
  }


  private static class QRReleaseLock extends QueuedRequest {

    final long objectId;
    final long priorityToRelease;


    public QRReleaseLock(long objectId, long priorityToRelease) {
      this.objectId = objectId;
      this.priorityToRelease = priorityToRelease;
    }


    @Override
    public void execute(InterconnectProtocol calleeImpl) {
      if (debugger.isEnabled()) {
        debugger.debug(new Object() {
          public String toString() {
            return "releaseLock from queue: release " + objectId;
          }
        });
      }
      try {
        calleeImpl.releaseLock(objectId, priorityToRelease);
      } catch (ObjectNotInCacheException e) {
        // can this happen if the object has already been removed?
        throw new RuntimeException("should not happen");
      }
    }

  }


  private static class QRRequestLock extends QueuedRequest {

    long objectId;
    long priority;
    boolean tryLock;
    long nanoTimeout;


    public QRRequestLock(long objectId, long priority, boolean tryLock, long nanoTimeout) {
      this.objectId = objectId;
      this.priority = priority;
      this.tryLock = tryLock;
      this.nanoTimeout = nanoTimeout;
    }


    @Override
    public void execute(InterconnectProtocol calleeImpl) {
      if (debugger.isEnabled()) {
        debugger.debug(new Object() {

          public String toString() {
            return "requestLock from queue: request " + objectId + "(p=" + priority + ")";
          }
        });
      }
      try {
        calleeImpl.requestLock(objectId, priority, tryLock, nanoTimeout);
      } catch (ObjectNotInCacheException e) {
        // FIXME: klären: can happen if the object has already been removed => dann ist runtimeexception doch nicht so gut?
        if (debugger.isEnabled()) {
          debugger.debug("object with id <" + objectId + "> wasn't locked because it has already been removed");
        }
        throw new RuntimeException("This should not happen", e);
      } catch (InterruptedException e) {
        if (debugger.isEnabled()) {
          debugger.debug("requestLock of object with id <" + objectId + "> was interrupted");          
        }
        //objekt kann nicht preliminary gelockt sein => runtimeexception
        throw new RuntimeException("requestLock was interrupted while executing queued request for objectId = " + objectId, e);
      }
    }

  }

  private static class QRWaitForActiveThreads extends QueuedRequest {

    private ThreadType type;
    
    public QRWaitForActiveThreads(ThreadType type) {
      this.type = type;
    }

    @Override
    public void execute(InterconnectProtocol calleeImpl) {
      if (debugger.isEnabled()) {
        debugger.debug(new Object() {
          public String toString() {
            return "waitForActiveThreads from queue:  " + type;
          }
        });
      }
      calleeImpl.waitForActiveThreads(type);
    }

  }

  private static class QRRecallLockRequest extends QueuedRequest {

    private final long objectId;
    private final long priority;
    private final long priorityOfLockInOldLockCircle;
    private final boolean countedInLockCircle;
    private final int lockCircleSize;


    public QRRecallLockRequest(long objectId, long priority, long priorityOfLockInOldLockCircle,
                               boolean countedInLockCircle, int lockCircleSize) {
      this.objectId = objectId;
      this.priority = priority;
      this.priorityOfLockInOldLockCircle = priorityOfLockInOldLockCircle;
      this.countedInLockCircle = countedInLockCircle;
      this.lockCircleSize = lockCircleSize;
    }


    @Override
    public void execute(InterconnectProtocol calleeImpl) {
      if (debugger.isEnabled()) {
        debugger.debug(new Object() {

          public String toString() {
            return "recallLockRequest from queue: objectId = " + objectId + ", prio = " + priority
                + ", priorityOfLockInOldLockCircle = " + priorityOfLockInOldLockCircle;
          }
        });
      }
      try {
        calleeImpl.recallLockRequest(objectId, priority, priorityOfLockInOldLockCircle, countedInLockCircle, lockCircleSize);
      } catch (ObjectNotInCacheException e) {
        if (debugger.isEnabled()) {
          debugger.debug(new Object() {

            public String toString() {
              return "Could not recall lock request: object " + objectId + " was deleted in the meanwhile.";
            }
          });
        }
        //da kann man sonst nichts machen.
      }
    }

  }


  public List<QueuedRequest> getQueuedRequests() {
    return queue;
  }


  public void recallLockRequest(long objectId, long priority, long priorityOfLockInOldLockCircle,
                                boolean countedInLockCircle, int lockCircleSize) throws ObjectNotInCacheException {
    synchronized (lock) {
      if (connected) {
        impl.recallLockRequest(objectId, priority, priorityOfLockInOldLockCircle, countedInLockCircle, lockCircleSize);
      } else {
        queue.add(new QRRecallLockRequest(objectId, priority, priorityOfLockInOldLockCircle, countedInLockCircle, lockCircleSize));
      }
    }
  }


}
