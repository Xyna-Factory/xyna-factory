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

package com.gip.xyna.xnwh.xwarehousejobs;



import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.xnwh.xwarehousejobs.WarehouseJobScheduleFactory.WarehouseJobScheduleType;



public abstract class WarehouseJobSchedule {

  private boolean isRegistered = false;
  private final ReentrantLock registerLock = new ReentrantLock();


  public abstract WarehouseJobScheduleType getType();


  public abstract String[] getScheduleParameters();


  public final void register() {
    registerLock.lock();
    try {
      if (isRegistered)
        return;
      registerInternally();
      isRegistered = true;
    }
    finally {
      registerLock.unlock();
    }
  }


  public final void unregister() {
    registerLock.lock();
    try {
      if (!isRegistered) {
        return;
      }
      unregisterInternally();
      isRegistered = false;
    }
    finally {
      registerLock.unlock();
    }
  }


  public final boolean isRegistered() {
    return isRegistered;
  }


  /**
   * The real register method
   */
  protected abstract void registerInternally() ;


  /**
   * The real unregister method
   */
  protected abstract void unregisterInternally();


  /**
   * @return true if this job schedule is expected to run again in future
   */
  public abstract boolean needsToRunAgain();

}
