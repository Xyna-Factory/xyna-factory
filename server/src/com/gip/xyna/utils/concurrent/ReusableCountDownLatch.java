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
package com.gip.xyna.utils.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ReusableCountDownLatch ist ein wiederverwendbarer CountDownLatch.
 * Er ist für Anwendungsfälle gedacht, in denen nur selten ein await(..) benötigt wird, 
 * das countDown() aber häufig gerufen wird.
 * 
 * - countDown() hat keinen Effekt, solange kein await(..) gerufen wurde
 * - await(..) legt einen inneren CountDownLatch an
 * - weitere await(..) verwenden den gleichen inneren CountDownLatch
 * - countDown() sieht inneren CountDownLatch, löst diesen aus und entfernt den inneren CountDownLatch wieder
 */
public class ReusableCountDownLatch {

  private Integer count;
  private final Object lock = new Object();
  private volatile CountDownLatch cdl;

  public ReusableCountDownLatch(int count) {
    this.count = count;
  }
  
  public void countDown() {
    CountDownLatch latch = cdl;
    if( latch != null ) {
      latch.countDown();
      cdl = null; //verbrauchten Latch entfernen
    }
  }

  public void await() throws InterruptedException {
    getOrCreateLatch().await();
  }

  public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
    return getOrCreateLatch().await(timeout, unit);
  }
  
  private CountDownLatch getOrCreateLatch() {
    CountDownLatch latch = cdl;
    if( latch != null ) {
      return latch;
    }
    //neuen Latch abgesichert anlegen
    synchronized (lock) {
      latch = cdl;
      if( latch == null ) {
        latch = new CountDownLatch(count);
        cdl = latch;
      }
    }
    return latch;
  }

  public CountDownLatch prepareLatch() {
    return getOrCreateLatch();
  }
  
  
}
