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
package com.gip.xyna.coherence.utils.locking;



import java.util.concurrent.Semaphore;



/**
 * bietet im vergleich zu reentrantlock die möglichkeit, auch intern von einem anderen thread unlocken zu lassen
 * (deadlockerkennung und sowas)
 */

public class ReentrantLock2 {


  private Thread ownerThread;
  private int ownedCnt = 0;
  private Semaphore lock = new Semaphore(1);


  public void lock() {
    Thread currentThread = null;
    if (ownerThread != null) {
      currentThread = Thread.currentThread();
      if (currentThread == ownerThread) {
        ownedCnt++;
        return;
      }
    }
    try {
      lock.acquire();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    ownerThread = currentThread == null ? Thread.currentThread() : currentThread;
    ownedCnt++;
    return;
  }


  public void unlock() {
    Thread currentThread = null;
    if (ownerThread != null) {
      currentThread = Thread.currentThread();
      if (currentThread == ownerThread) {
        ownedCnt--;
        if (ownedCnt == 0) {
          ownerThread = null;
          lock.release();
        } else if (ownedCnt < 0) {
          throw new IllegalMonitorStateException();
        }
      } else {
        throw new IllegalMonitorStateException();
      }
    } else {
      throw new IllegalMonitorStateException();
    }
  }


  public boolean isLocked() {
    return ownerThread != null;
  }


  public boolean isHeldByCurrentThread() {
    return ownerThread != null && ownerThread == Thread.currentThread();
  }


  public int getHoldCount() {
    return isHeldByCurrentThread() ? ownedCnt : 0;
  }


  public boolean hasQueuedThreads() {
    return lock.hasQueuedThreads();
  }
}
