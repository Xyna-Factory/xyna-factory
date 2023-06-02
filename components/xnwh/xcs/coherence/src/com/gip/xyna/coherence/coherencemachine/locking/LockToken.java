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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;


public class LockToken implements Serializable {

  static enum LockTokenType {
    CIRCLE_PROTECTION, STANDARD, PRELIMINARY

  }

  private static final long serialVersionUID = 1L;

  AtomicInteger numberOfWaitingThreads = new AtomicInteger(0);

  private boolean notified;
  private volatile boolean lockIsRecalled = false;
  private LockTokenType type;
  
  public LockToken(LockTokenType type) {
    this.type = type;
  }

  public boolean isNotified() {
    return this.notified;
  }
  
  /**
   * true, falls threads gewartet haben
   */
  public boolean notifyLock() {
    if (numberOfWaitingThreads.get() > 0) {
      synchronized (this) {
        notified = true;
        notifyAll();
      }
      return true;
    }
    return false;
  }
  
  
  public String toString() {
    return super.toString() + "-" + type;
  }
  
  
  public boolean isLockRecalled() {
    return lockIsRecalled;
  }
  
  
  public void lockIsRecalled() {
    lockIsRecalled = true; 
  }
   
}
