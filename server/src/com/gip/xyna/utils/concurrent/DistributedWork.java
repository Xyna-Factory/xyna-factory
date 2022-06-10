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



import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;



/**
 * utilklasse zum angenehmen verteilten abarbeiten von mehreren aufgaben über mehrere threads hinweg.
 * 
 * falls man nicht explizit mit Tasks arbeiten will, so zu verwenden:<br>
 * <pre>
 * DistributedWork work = new DistributedWork(count);
 * int nextWorkIdx;
 * while (-1 != (nextWorkIdx = work.getAndLockNextOpenTaskIdx())) {
 *   try {
 *   //work mit entsprechendem index abarbeiten
 *   } finally {
 *     work.taskDone();
 *   } 
 * }
 * //kein freies task mehr gefunden. die anderen tasks sind entweder fertig oder werden noch bearbeitet.
 * work.waitForCompletion(); //aufruf wartet nur, falls nicht rekursiv aufgerufen, ansonsten kommt er sofort zurück. das ist dann okay.
 * </pre>
 * @see DistributedWorkWithTasks
 */
public class DistributedWork {


  protected final AtomicInteger nextOpenWork;
  protected final CountDownLatch latch;
  protected final Set<Thread> activeThreads;
  private final int workCount;


  public DistributedWork(int workCount) {
    nextOpenWork = new AtomicInteger(0);
    this.workCount = workCount;
    latch = new CountDownLatch(workCount);
    activeThreads = Collections.synchronizedSet(new HashSet<Thread>());
  }


  public void taskDone() {
    latch.countDown();
  }


  public void waitForCompletion() throws InterruptedException {
    if (!isRecursive()) {
      latch.await();
    }
  }


  protected boolean isRecursive() {
    Thread t = Thread.currentThread();

    if (activeThreads.contains(t)) {
      return true;
    }
    return false;
  }


  /**
   * gibt -1 zurück, falls kein weiteres offenes task vorhanden ist
   */
  public int getAndLockNextOpenTaskIdx() {
    Thread t = Thread.currentThread();
    activeThreads.add(t);

    int idx = nextOpenWork.getAndIncrement();
    if (idx < workCount) {
      return idx;
    } else {
      return -1;
    }
  }


}
