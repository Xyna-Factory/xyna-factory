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

package com.gip.xyna.utils.concurrent;



import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;



public class ReentrantLockWrapper extends ObjectWithRemovalSupport {

  private ReentrantReadWriteLock lock;


  public ReentrantLockWrapper() {
    lock = new ReentrantReadWriteLock();
  }


  @Override
  protected boolean shouldBeDeleted() {
    /*
     * sicherstellen, dass das lock nicht von einem anderen thread gelockt ist:
     * man kann die lock.getWrite-/ReadLockCount-methoden nicht verwenden, weil sie nicht sicher die
     * richtige zahl liefern.
     * => testen, ob man writelock bekommen kann (falls ja, dann kann das lock entfernt werden)
     * 
     */
    if (lock.writeLock().isHeldByCurrentThread()) {
      return false;
    }

    if (lock.writeLock().tryLock()) {
      lock.writeLock().unlock();
      return true;
    }
    return false;
  }


  public ReentrantReadWriteLock getLock() {
    return lock;
  }

}

