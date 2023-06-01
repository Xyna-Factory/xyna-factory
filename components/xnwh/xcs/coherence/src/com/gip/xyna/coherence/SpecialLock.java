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
package com.gip.xyna.coherence;



import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.coherence.CacheControllerImpl.GlobalLockPayload;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.management.ClusterMember;
import com.gip.xyna.coherence.utils.debugging.Debugger;
import com.gip.xyna.coherence.utils.logging.LoggerFactory;



public class SpecialLock implements ThreadLockInterface {

  private static final Logger logger = LoggerFactory.getLogger(SpecialLock.class);
  private static final Debugger debugger = Debugger.getDebugger();
  
  private Semaphore lock = new Semaphore(1);
  private AtomicInteger threadCnt = new AtomicInteger(0);
  private long objectId;
  private CacheControllerImpl controller;


  SpecialLock(long objectId, CacheControllerImpl controller) {
    this.objectId = objectId;
    this.controller = controller;
  }


  public void lockLocally() {
    try {
      lock.acquire();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }


  public void unlockLocally() {
    if (!isLocked()) {
      throw new RuntimeException("can not unlock without locking first.");
    }
    lock.release();
  }


  public void decrementThreadCount() {
    if (threadCnt.decrementAndGet() < 0) {
      throw new RuntimeException("threadCnt should not be negative.");
    }
  }


  public void checkLock() {
    // ok cnt = 0
    try {
      lock.acquire();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    // ok, weil anderer thread nicht lock bekommt
    threadCnt.incrementAndGet();
    if (debugger.isEnabled()) {
      debugger.debug("checkLock: got special lock");
    }
    // ok cnt = 1
    lock.release();
    if (debugger.isEnabled()) {
      debugger.debug("checkLock: released special lock");
    }
    // ok cnt = 1
  }


  public void waitForThreads() {
    try {
      while (threadCnt.get() > 0) {
        Thread.sleep(THREADCNT_SLEEP_INTERVAL);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }


  public void lockAll() {
    //triggern
    try {
      controller.lock(objectId);
      GlobalLockPayload payload = (GlobalLockPayload) controller.read(objectId);
      controller.setAndPushAlreadyLockedObject(objectId, new GlobalLockPayload(payload.type, true), false);
    } catch (ObjectNotInCacheException e) {
      throw new RuntimeException(e);
    }
  }


  public void unlockAll(ClusterMember[] oldMembers) {
    GlobalLockPayload payload;
    try {
      payload = (GlobalLockPayload) controller.read(objectId);
    } catch (ObjectNotInCacheException e) {
      throw new RuntimeException(e);
    }
    controller.setAndPushAlreadyLockedObject(objectId, new GlobalLockPayload(payload.type, false), oldMembers, false, false);
    controller.unlock(objectId);
  }


  public boolean isLocked() {
    return lock.availablePermits() == 0;
  }


  public String toString() {
    StringBuffer sb =
        new StringBuffer("\n----- ").append(SpecialLock.class.getSimpleName()).append(" for <").append(objectId)
            .append(">\n");
    sb.append(" - threadCnt = ").append(threadCnt.get()).append("\n");
    sb.append(" - locked = ").append(isLocked()).append("\n");
    sb.append("-------------");
    return sb.toString();
  }

}
