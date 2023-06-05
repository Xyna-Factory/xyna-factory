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

import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectProtocol;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject;

import junit.framework.TestCase;


public class LockObjectTest extends TestCase {

  public void test_01_lockUnlock() throws ObjectDeletedWhileWaitingForLockException, InterruptedException {

    LockObject lock = new LockObject(123L);

    lock.requestLock(1, true, false, -1);
    lock.releaseLock(true, false);

    lock.checkClean();

  }


  public void test_02_lockUnlockWithRemote() throws ObjectDeletedWhileWaitingForLockException, InterruptedException {

    LockObject lock = new LockObject(123L);

    lock.requestLock(10, true, false, -1);
    long winningPriority = lock.requestLock(5, false, false, -1).priority;

    assertEquals(10, winningPriority);

    lock.setPreliminaryLockCircleSize(2, false);
    lock.closeLockCircle();

    lock.releaseLock(true, true);

    lock.awaitLock(10, false, -1);

    lock.releaseLock(false, true);

    lock.checkClean();

  }


  public void test_03_lockUnlockWithCircle() throws ObjectDeletedWhileWaitingForLockException, InterruptedException {

    LockObject lock = new LockObject(123L);

    lock.requestLock(7, true, false, -1);

    long losingPrioDueToClosedCircle = lock.requestLock(10, false, false, -1).priority;

    lock.setPreliminaryLockCircleSize(2, false);
    lock.closeLockCircle();

    assertEquals(InterconnectProtocol.SUCCESSFUL_LOCK_BY_COMPARISON, losingPrioDueToClosedCircle);

    lock.releaseLock(false, true);

    lock.releaseLock(true, true);

    lock.checkClean();

  }

}
