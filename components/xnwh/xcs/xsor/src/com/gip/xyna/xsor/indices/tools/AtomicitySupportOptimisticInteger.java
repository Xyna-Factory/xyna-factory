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
package com.gip.xyna.xsor.indices.tools;

import java.util.concurrent.atomic.AtomicInteger;


// FIXME untested!
public class AtomicitySupportOptimisticInteger implements AtomicitySupport {

  private AtomicInteger accessState = new AtomicInteger(0);
  
  public void protectAgainstNonAtomicOperations() {
    boolean success = false;
    while (!success) {
      int state = accessState.get();
      if (state >= 0) {
        if (accessState.compareAndSet(state, state+1)) {
          success = true;
        }
      } else {
        synchronized (accessState) {
          if (accessState.get() >= 0) {
            if (accessState.compareAndSet(state, state+1)) {
              success = true;
            }
          } else {
            try {
              accessState.wait();
            } catch (InterruptedException e) {
              // try again
            }
          }
        }
      }
    }
  }


  public void releaseProtectionAgainstNonAtomicOperations() {
    boolean success = false;
    while (!success) {
      int state = accessState.get();
      if (state == 1) {
        synchronized (accessState) {
          if (accessState.compareAndSet(1, 0)) {
            success = true;
            accessState.notifyAll();
          }
        }
      } else {
        if (accessState.compareAndSet(state, state-1)) {
          success = true;
        }
      }
    }

  }


  public void initiateNonAtomicOperation() {
    boolean success = false;
    while (!success) {
      int state = accessState.get();
      if (state == 0) {
        if (accessState.compareAndSet(0, -1)) {
          success = true;
        }
      } else {
        synchronized (accessState) {
          int lockedState = accessState.get();
          if (lockedState == 0) {
            if (accessState.compareAndSet(0, -1)) {
              success = true;
            }
          } else {
            try {
              accessState.wait();
            } catch (InterruptedException e) {
              // try again
            }
          }
        }
      }
    }
  }


  public void finishNonAtomicOperation() {
    synchronized (accessState) {
      accessState.set(0); // we could compare but we should be the only accessor
      accessState.notifyAll();
    }
  }

}
