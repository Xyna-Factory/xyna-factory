/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xsched.capacities;



import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;



/* package */ class CMSharedResourceConsistencyManagement {

  private static final Logger logger = CentralFactoryLogging.getLogger(CMSharedResourceConsistencyManagement.class);

  private final Thread consistencyThread;
  private final ConcurrentLinkedQueue<SharedResourceFreeCapacitiesRequest> queue;
  private final Function<SharedResourceFreeCapacitiesRequest, Boolean> processingFunction;
  private boolean running;


  /* package */ CMSharedResourceConsistencyManagement(Function<SharedResourceFreeCapacitiesRequest, Boolean> processingFunction) {
    this.processingFunction = processingFunction;
    queue = new ConcurrentLinkedQueue<>();
    running = true;
    consistencyThread = new Thread(this::run, "CapacytiyManagement - Shared Resource - Consistency Thread");
    consistencyThread.setDaemon(true);
    consistencyThread.start();
  }


  public void stop() {
    running = false;
    consistencyThread.interrupt();
    try {
      consistencyThread.join();
    } catch (InterruptedException e) {
    }
  }


  private void run() {
    boolean success = true;
    while (running) {
      try {
        SharedResourceFreeCapacitiesRequest request = queue.poll();
        if (request != null) {
          success = processRequest(request);
        }
        if (request == null || !success) {
          Thread.sleep(10000);
        }
      } catch (InterruptedException e) {
        continue;
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("CapacityManagement Shared resource Consistency Thread finished");
    }
  }


  private boolean processRequest(SharedResourceFreeCapacitiesRequest request) {
    if (logger.isDebugEnabled()) {
      logger.debug("reprocessing capacities request: " + request.toString());
    }
    boolean success = processingFunction.apply(request);
    if (!success) {
      queue.add(request);
      if (logger.isDebugEnabled()) {
        logger.debug("reprocessing capacities request failed: " + request.toString());
      }
    } else if (logger.isDebugEnabled()) {
      logger.debug("reprocessing capacities request succeeded: " + request.toString());
    }
    return success;
  }


  public void queueRequest(SharedResourceFreeCapacitiesRequest request) {
    if (logger.isDebugEnabled()) {
      logger.debug("capacities request queued: " + request.toString());
    }
    queue.add(request);
  }
}
