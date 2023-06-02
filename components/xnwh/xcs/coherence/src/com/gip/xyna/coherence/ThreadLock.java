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
package com.gip.xyna.coherence;



import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.coherence.CacheControllerImpl.GlobalLockPayload;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.management.ClusterMember;


/**
 * zählt anzahl threads die {@link #checkLock()} aufgerufen haben aber noch nicht {@link #decrementThreadCount()). 
 * threads werden durch {@link #checkLock()} durchgelassen, falls das lock nicht gelockt ist, ansonsten warten sie dort,
 * bis unlocked wird.
 */
public class ThreadLock implements ThreadLockInterface {

  private volatile boolean locked = false;
  private volatile int lockCnt = 0;
  private AtomicInteger threadCnt = new AtomicInteger(0);
  private long objectId;
  private long objectId_t;
  private CacheControllerImpl controller; //TODO interface nehmen, wenn push veröffentlicht wird


  public ThreadLock(long id, long id_t, CacheControllerImpl controller) {
    this.controller = controller;
    this.objectId = id;
    this.objectId_t = id_t;
  }


  public void unlockLocally() {
    locked = false;
  }


  private void getSharedLock(long id) {
    try {
      controller.lock(id);
    } catch (ObjectNotInCacheException e) {
      throw new RuntimeException(e);
    }
  }


  private void unlockSharedLock(long id) {
    controller.unlock(id);
  }


  public void lockLocally() {
    lockCnt++;
    locked = true;
  }


  public void checkLock() {
    threadCnt.incrementAndGet();
    if (locked) {
      int oldCnt = -1;
      while (lockCnt != oldCnt) {
        oldCnt = lockCnt;
        threadCnt.decrementAndGet();
        getSharedLock(objectId_t);
        unlockSharedLock(objectId_t);
        //wenn der thread hier hängt, wird er nicht mitgezählt. das ist nur dann schlimm, falls das global lock ein zweites mal vergeben wurde
        //ist das global lock nicht mehr gesetzt, ist lockCnt = oldCnt und der thread darf gefahrlos laufen.
        threadCnt.incrementAndGet();
      }
    }
  }


  public void decrementThreadCount() {
    if (threadCnt.decrementAndGet() < 0) {
      throw new RuntimeException("threadCnt should not be negative.");
    }
  }


  public void lockAll() {
    //auf alle knoten "locked" versenden. realisierung über push von boolean innerhalb von payload von objectId
    //dort wartet dann ein trigger der darauf reagiert und das lokale lock setzt.
    getSharedLock(objectId_t);
    
    //jetzt den trigger aktivieren
    getSharedLock(objectId);
    GlobalLockPayload payload;
    try {
      payload = (GlobalLockPayload) controller.read(objectId);
    } catch (ObjectNotInCacheException e) {
      throw new RuntimeException(e);
    }
    controller.setAndPushAlreadyLockedObject(objectId, new GlobalLockPayload(payload.type, true), false);
  }


  public void unlockAll(ClusterMember[] membersToResume) {
    GlobalLockPayload payload;
    try {
      payload = (GlobalLockPayload) controller.read(objectId);
    } catch (ObjectNotInCacheException e) {
      throw new RuntimeException(e);
    }
    controller.setAndPushAlreadyLockedObject(objectId, new GlobalLockPayload(payload.type, false), membersToResume, false, false);
    unlockSharedLock(objectId);
    unlockSharedLock(objectId_t);
  }


  public String toString() {
    StringBuffer sb =
        new StringBuffer("\n----- ").append(ThreadLock.class.getSimpleName()).append(" for <").append(objectId)
            .append(">\n");
    sb.append(" - threadCnt = ").append(threadCnt.get()).append("\n");
    sb.append(" - lockCnt = ").append(lockCnt).append("\n");
    sb.append(" - locked = ").append(locked).append("\n");
    sb.append("-------------");
    return sb.toString();
  }


  public boolean isLocked() {
    return locked;
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

}
