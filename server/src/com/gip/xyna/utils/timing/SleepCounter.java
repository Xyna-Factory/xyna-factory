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
package com.gip.xyna.utils.timing;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;


public class SleepCounter {
  
  private final long increment;
  private final AtomicLong currentSleep;
  private final long maxSleep;
  private final int yieldThreshold;
  private final AtomicLong sleeps;
  private final TimeUnit unit;
  private final boolean parkInSubseconds;
  
  public SleepCounter(long increment) {
    this(increment, 0);
  }
  
  public SleepCounter(long increment, long maxSleepMillis) {
    this(increment, maxSleepMillis, 0);
  }
                      
  
  public SleepCounter(long increment, long maxSleepMillis, int yieldThreshold) {
    this(increment, maxSleepMillis, yieldThreshold, TimeUnit.MILLISECONDS, true);
  }
  
  
  public SleepCounter(long increment, long maxSleep, int yieldThreshold, TimeUnit timeUnit, boolean parkInSubseconds) {
    this.increment = increment;
    this.maxSleep = maxSleep;
    this.yieldThreshold = yieldThreshold;
    this.currentSleep = new AtomicLong(0);
    this.sleeps = new AtomicLong(0);
    this.unit = timeUnit;
    this.parkInSubseconds = parkInSubseconds;
  }
  
  
  public SleepCounter clone() {
    return new SleepCounter(this.increment, this.maxSleep, this.yieldThreshold);
  }
  
  public void reset() {
    sleeps.set(0);
    currentSleep.set(0);
  }


  public void sleep() throws InterruptedException {
    if (sleeps.getAndIncrement() < yieldThreshold) {
      Thread.yield();
    } else {
      long sleep = currentSleep.get();
      if (sleep > 0) {
        if (parkInSubseconds) {
          switch (unit) {
            case MICROSECONDS :
            case NANOSECONDS :
              LockSupport.parkNanos(unit.toNanos(sleep));
              break;
            case MILLISECONDS :
              Thread.sleep(sleep);
              break;
            default :
              unit.sleep(sleep);
              break;
          }
        } else {
          unit.sleep(sleep);
        }
      }
      if (maxSleep > 0) {
        currentSleep.set(Math.min(maxSleep, sleep + increment));
      } else {
        currentSleep.set(sleep + increment);
      }
    }
  }
  
  
  public long iterationCount() {
    return sleeps.get();
  }

}
