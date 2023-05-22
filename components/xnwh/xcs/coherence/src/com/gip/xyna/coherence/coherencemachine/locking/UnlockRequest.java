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

package com.gip.xyna.coherence.coherencemachine.locking;



import java.util.concurrent.CountDownLatch;

import com.gip.xyna.coherence.coherencemachine.SynchronousRequest;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectProtocol;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;



public class UnlockRequest extends SynchronousRequest {

  private long objectId;
  private long priorityToRelease;


  public UnlockRequest(InterconnectProtocol nodeConnection, long objectId, long priorityToRelease, CountDownLatch latch) {
    super(nodeConnection, latch);
    this.objectId = objectId;
    this.priorityToRelease = priorityToRelease;
  }


  public void exec() throws ObjectNotInCacheException {
    nodeConnection.releaseLock(objectId, priorityToRelease);
  }

}
