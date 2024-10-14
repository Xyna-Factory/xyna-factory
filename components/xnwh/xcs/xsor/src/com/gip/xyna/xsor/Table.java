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
package com.gip.xyna.xsor;



import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.apache.log4j.Logger;

import com.gip.xyna.xsor.common.XSORUtil;
import com.gip.xyna.xsor.protocol.XSORMemory;
import com.gip.xyna.xsor.protocol.XSORMemory.LogLockTimeThreadLocal;



public class Table {

  private static final Logger logger = Logger.getLogger(Table.class);

  private XSORMemory xsorMemory;
  private AtomicIntegerArray transactionLock;
  private int size;


  public Table(int size, XSORMemory xcMemory) {
    this.size = size;
    this.xsorMemory = xcMemory;
    transactionLock = new AtomicIntegerArray(size);
    for (int i = 0; i < size; i++) {
      transactionLock.set(i, -1);
    }
  }


  public XSORMemory getXSORMemory() {
    return xsorMemory;
  }

  private LogLockTimeThreadLocal lastLock = new LogLockTimeThreadLocal("tx");
  private static final int timeOutInS = 60;
  private static final int maxSleepInMs = 50;
  
  /**
   * transactionId sollte positiv sein
   */
  public void lockObjectForTransaction(int internalId, int transactionId) {
    int i = 0;
    int sleepInMs = 0;
    long timeout = 0;
    long lockedBy = -1;
    while (!tryLockObjectForTransaction(internalId, transactionId)) {
      i++;
      
      long currentTime = System.currentTimeMillis();
      if (i == 1) {
        timeout = currentTime + timeOutInS * 1000;
      }
      long txId = transactionLock.get(internalId);
      int toTimeout = (int) (timeout - currentTime);
      if (toTimeout <= 0) {
        if (txId != 0) { // 0 happens if the lock was released since the lock.compareAndSet
          ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
          ThreadInfo ti = tbean.getThreadInfo(txId, 1000);
          if (ti != null) {
            logger.warn("Long waiting for Object lock :" + internalId + ", held by thread " + ti.getThreadName());
            if (logger.isTraceEnabled()) {
              Exception e = new Exception();
              e.setStackTrace(ti.getStackTrace());
              logger.warn("stack of thread holding lock: ", e);
            }
          } else {
            logger.warn("Long waiting for Object lock :" + internalId + ", held by unknown thread (tid=" + txId + ")");
          }
        }
        throw new RuntimeException("Probable deadlock detected, have waited " + timeOutInS + " seconds for lock. Abort.");
      }

      if (lockedBy != txId) {
        if (lockedBy != -1) {
          //jemand anderes hat das lock als vorher -> es geht vorwärts -> gleich wieder probieren
          sleepInMs = 0;
        }
        lockedBy = txId;
      }
      if (i % 100 == 1 && logger.isDebugEnabled()) {
        //ausführliches logging
        if (txId > 0) {
          logger.debug("Long waiting for Transaction lock: " + internalId + ", held by transaction txid=" + txId + ".");
        }
      }

      // ok, da es i.A. normalerweise nicht blockiert => TODO:Handbuch:
      if (sleepInMs > 5) {
        XSORUtil.sleep(Math.min(sleepInMs, toTimeout));
      }
      sleepInMs = Math.min(maxSleepInMs, sleepInMs + 5);
    }
    if (logger.isTraceEnabled()) {
      logger.trace("txid=" + transactionId + " tx-locked " + internalId + ".");
    }
  }
  
  
  public boolean tryLockObjectForTransaction(int internalId, int transactionId) {
    boolean locked = transactionLock.compareAndSet(internalId, -1, transactionId);
    if (locked) {
      lastLock.set();
    }
    return locked;
  }


  public void unlockObjectForTransaction(int internalId) {
    transactionLock.set(internalId, -1);
    lastLock.logAndRemove(internalId);
  }


  public boolean unlockObjectForTransaction(int internalId, int transactionId) {
    try {
      return transactionLock.compareAndSet(internalId, transactionId, -1);
    } finally {
      lastLock.logAndRemove(internalId);
    }
  }


  public int getLockTransactionId(int internalId) {
    return transactionLock.get(internalId);
  }


  public int getSize() {
    return size;
  }

}
