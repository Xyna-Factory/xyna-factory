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



import java.util.Set;



public class CoherenceLockState {

  long objectId;
  int currentLockCircleSize;
  Long currentlyLockingPriority;
  Set<Long> lockedPriorities;
  int preliminaryLockCircleSize;
  long localRequestPriorityHoldingLock;


  public long getObjectID() {
    return this.objectId;
  }


  public int getCurrentLockCircleSize() {
    return this.currentLockCircleSize;
  }


  public Long getCurrentlyLockingPriority() {
    return this.currentlyLockingPriority;
  }


  public Set<Long> getLockedPriorities() {
    return lockedPriorities;
  }


  public int getPreliminaryLockCircleSize() {
    return preliminaryLockCircleSize;
  }


  public long getLocalRequestPriorirtyHoldingLock() {
    return localRequestPriorityHoldingLock;
  }

}
