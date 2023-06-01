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

package com.gip.xyna.xfmg.xopctrl.managedsessions;

import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class SessionTimeoutRunnable implements Runnable {

  private static final Logger logger = CentralFactoryLogging.getLogger(SessionTimeoutRunnable.class);

  private final PriorityBlockingQueue<ManagedSession> timeoutQueue;
  private final SessionManagement sessionManagement;


  public SessionTimeoutRunnable(SessionManagement sessionManagement, PriorityBlockingQueue<ManagedSession> timeoutQueue) {
    this.timeoutQueue = timeoutQueue;
    this.sessionManagement = sessionManagement;
  }


  public void run() {
    synchronized (timeoutQueue) {
      try {
        while (true) {

          while (timeoutQueue.isEmpty()) {
            logger.debug("Waiting for Sessions to be registered...");
            waitForNextNotify();
          }

          ManagedSession timedoutSession = timeoutQueue.poll();
          if (timedoutSession == null) {
            continue;
          }

          long diff =
              timedoutSession.getLastInteraction() + SessionManagement.getSessionTimeout() - System.currentTimeMillis();
          if (diff <= 0) {
            try {
              tryToTimeout(timedoutSession);
            } catch (PersistenceLayerException e) {
              logger.error("Error while trying to timeout session", e);
            }
          } else {
            timeoutQueue.add(timedoutSession);
            waitForTimeout(diff);
          }

        }
      } catch (InterruptedException e) {
        // shutdown
        if (logger.isDebugEnabled()) {
          logger.debug("Thread " + Thread.currentThread().getName() + " finished");
        }
      }
    }
  }


  private void waitForTimeout(Long diff) throws InterruptedException {
    try {
      timeoutQueue.wait(diff);
    } catch (InterruptedException e) {
      if (sessionManagement.isShuttingDown()) {
        throw new InterruptedException();
      }
    }
  }


  private void waitForNextNotify() throws InterruptedException {
    try {
      timeoutQueue.wait();
    } catch (InterruptedException e) {
      if (sessionManagement.isShuttingDown()) {
        throw new InterruptedException();
      }
    }
  }


  private void tryToTimeout(ManagedSession timedoutSession) throws PersistenceLayerException {
    if (!timedoutSession.timeout()) { // this may occasionally fail due to a last second keep alive
      timeoutQueue.add(timedoutSession);
    } else {
      sessionManagement.quitSession(timedoutSession.getID());
    }
  }

}
