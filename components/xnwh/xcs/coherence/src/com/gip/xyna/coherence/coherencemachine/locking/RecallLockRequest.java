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
package com.gip.xyna.coherence.coherencemachine.locking;

import java.util.concurrent.CountDownLatch;

import com.gip.xyna.coherence.coherencemachine.SynchronousRequest;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectProtocol;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;


public class RecallLockRequest extends SynchronousRequest {

  private final long objectId;
  private final long priority;
  private final long priorityOfLockInOldLockCircle;
  private final boolean countedInLockCircle;
  private final int lockCircleSize;
  
  public RecallLockRequest(InterconnectProtocol nodeConnection, long objectId, long priority, long priorityOfLockInOldLockCircle, boolean countedInLockCircle, int lockCircleSize, CountDownLatch latch) {
    super(nodeConnection, latch);
    this.objectId = objectId;
    this.priority = priority;
    this.priorityOfLockInOldLockCircle = priorityOfLockInOldLockCircle;
    this.countedInLockCircle = countedInLockCircle;
    this.lockCircleSize = lockCircleSize;
  }

  @Override
  public void exec() throws ObjectNotInCacheException, InterruptedException {
    getNodeConnection().recallLockRequest(objectId, priority, priorityOfLockInOldLockCircle, countedInLockCircle, lockCircleSize);
  }

}
