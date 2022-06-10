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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;


/**
 * IncrementableCountDownLatch funktioniert analog zu CountDownLatch, der Counter kann allerdings 
 * noch inkrementiert werden, solange der Latch noch nicht gelöst wurde.
 * Eigenschaften:
 * <ul>
 * <li>Beim {@link #countDown()} wird der Counter um 1 verringert, der Latch wird gelöst, sobald der
 *     Counter <code>&lt;=0</code> ist.</li>
 * <li>Solange der Latch noch nicht gelöst wurde, geben <code>countDown()</code> und {@link #increment()} 
 *     <code>true</code> zurück, danach <code>false</code>.</li>
 * <li>Das Verhalten von <code>countDown()</code> und <code>increment()</code> ist unsymmetrisch: 
 *     nur <code>countDown()</code> löst den Latch aus, <code>increment()</code> löst den Latch nicht,
 *     wenn der Counter positiv wird.</li>
 * <li>Der Counter wird einfach weiter inkrementiert bzw. runtergezählt, auch wenn das Latch ausgelöst 
 *     wurde.</li>
 * <li>Mit {@link #onRelease()} steht eine Methode zu Verfügung, die in abgeleiteten Klassen beliebig
 *     überschrieben werden kann. Sie wird genau einmal aufgerufen, wenn der Latch gelöst wird.</li>
 * </ul>
 */
public class IncrementableCountDownLatch {

  private final CountDownLatch cdl;
  private final AtomicInteger counter;
  private volatile boolean released = false;
  private static final Logger logger = CentralFactoryLogging.getLogger(IncrementableCountDownLatch.class);
  
  /**
   * Initialer Count ist 0
   */
  IncrementableCountDownLatch() {
    this(0);
  }

  /**
   * Angabe des initialen Counts
   * @param initialCount
   */
  public IncrementableCountDownLatch(int initialCount) {
    cdl = new CountDownLatch(1);
    counter = new AtomicInteger(initialCount);
  }

  /**
   * Wartet blockierend auf das Lösen des Latchs
   * @throws InterruptedException
   */
  public void await() throws InterruptedException {
    increment(); //evtl. ist Counter bereits auf 0, daher mit ...
    countDown(); //... countDown evtl. den Latch auslösen
    cdl.await();
  }
  
  /**
   * Wartet blockierend auf das Lösen des Latchs, solange Timeout nicht erreicht wurde
   * @throws InterruptedException
   */
  public boolean await(long timeout, TimeUnit unit) 
      throws InterruptedException {
    increment(); //evtl. ist Counter bereits auf 0, daher mit ...
    countDown(); //... countDown evtl. den Latch auslösen
    return cdl.await(timeout,unit);
  }

  /**
   * Inkrementieren des Counters
   * @return false, wenn Latch bereits gelöst wurde 
   */
  public boolean increment() {
    int cnt = counter.incrementAndGet();
    if (logger.isTraceEnabled()) {
      logger.trace(this + " inc->" + cnt);
    }
    return ! released;
  }

  /**
   * herunterzählem des Counters, evtl. Lösen des Latch
   * @return false, wenn Latch bereits zuvor gelöst wurde
   */
  public boolean countDown() {
    int c = counter.decrementAndGet();
    if (logger.isTraceEnabled()) {
      logger.trace(this + " dec->" + c);
    }
    if( c <= 0 ) {
      return release();
    } else {
      return ! released; //CountDown war möglich, Latch kann aber zuvor bereits gelöst worden sein
    }
  }


  private boolean release() {
    if (released) {
      return false; //bereits released
    }
    synchronized (this) {
      if (released) {
        return false;
      }
      cdl.countDown();
      released = true;
    }
    onRelease();
    return true;
  }


  /**
   * Rückgabe des aktuellen Counter-Standes
   * @return
   */
  public int getCount() {
    return counter.get();
  }
  
  
  /**
   * onRelease() wird genau einmal aufgerufen, nachdem der Latch gelöst wird.
   * Kann in abgeleiteten Klassen beliebig überschrieben werden.
   * Das Werfen eines Fehlers verhindert nicht das release().
   */
  protected void onRelease() {
  }
  
}
