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

package com.gip.xyna.coherence.remote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.gip.xyna.coherence.CacheController;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;



public class SessionBasedLockUnlock {

  private Map<Long, LockSessionRunnable> activeLockSessions = new ConcurrentHashMap<Long, LockSessionRunnable>();


  public void lock(CacheController controller, long sessionId, long objectId) throws ObjectNotInCacheException,
      DuplicateSessionIdException {

    if (!activeLockSessions.containsKey(sessionId)) {
      CountDownLatch lockBarrier = new CountDownLatch(1);
      LockSessionRunnable r = new LockSessionRunnable(controller, objectId, lockBarrier);
      Thread t = new Thread(r);
      t.start();
      try {
        lockBarrier.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      if (r.exception != null) {
        if (r.exception instanceof ObjectNotInCacheException) {
          throw (ObjectNotInCacheException) r.exception;
        } else {
          throw new RuntimeException(r.exception);
        }
      }
      activeLockSessions.put(sessionId, r); //TODO falls != null, auch fehler werfen? evtl besser synchronisieren?
    } else {
      throw new DuplicateSessionIdException(sessionId);
    }

  }


  public void unlock(CacheController controller, long sessionId, long objectId) throws UnknownSessionIdException,
      ObjectNotInCacheException {
    LockSessionRunnable runnable = activeLockSessions.remove(sessionId);
    if (runnable == null) {
      throw new UnknownSessionIdException(sessionId);
    }
    runnable.unlock();
    if (runnable.exception != null) {
      if (runnable.exception instanceof ObjectNotInCacheException) {
        throw (ObjectNotInCacheException) runnable.exception;
      } else {
        throw new RuntimeException(runnable.exception);
      }
    }
  }


  public static class UnknownSessionIdException extends Exception {

    private static final long serialVersionUID = 5285337916472939242L;

    private final long sessionId;


    public UnknownSessionIdException(long sessionId) {
      this.sessionId = sessionId;
    }


    public String getMessage() {
      return "Session ID <" + sessionId + "> is not known";
    }

  }


  public static class DuplicateSessionIdException extends Exception {

    private static final long serialVersionUID = 5285337916472939242L;

    private final long sessionId;


    public DuplicateSessionIdException(long sessionId) {
      this.sessionId = sessionId;
    }


    public String getMessage() {
      return "Session ID <" + sessionId + "> already exists";
    }

  }


  private static class LockSessionRunnable implements Runnable {

    private final CacheController controller;
    private final long objectId;

    private Throwable exception;

    private final CountDownLatch lockedNotification;
    private final CountDownLatch unlockBarrier;


    public LockSessionRunnable(CacheController controller, long objectId, CountDownLatch cb) {
      this.controller = controller;
      this.objectId = objectId;
      this.lockedNotification = cb;
      unlockBarrier = new CountDownLatch(1);
    }


    public void run() {

      try {
        controller.lock(objectId);
      } catch (Throwable e) {
        exception = e;
        //TODO in diesem fall wirklich noch auf unlockbarrier warten? unittests für die exceptionfälle schreiben. dabei kann man cachecontroller ja entsprechend implementieren.
      }

      lockedNotification.countDown();

      try {
        unlockBarrier.await();
      } catch (InterruptedException e) {
        exception = e;
        throw new RuntimeException(e);
      }

      try {
        controller.unlock(objectId);
      } catch (ObjectNotInCacheException e) {
        exception = e;
      }

      if (exception == null) {
        return;
      }

    }


    public void unlock() {
      unlockBarrier.countDown();
    }

  }

}
