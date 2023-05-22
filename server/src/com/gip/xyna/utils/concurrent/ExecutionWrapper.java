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
package com.gip.xyna.utils.concurrent;



import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaRunnable;



public abstract class ExecutionWrapper<E extends XynaException> extends XynaRunnable {

  private static final Logger logger = CentralFactoryLogging.getLogger(ExecutionWrapper.class);

  private Throwable exception = null;
  private Thread thread;
  private final CountDownLatch latch;
  private final String description;


  public ExecutionWrapper(String description) {
    this.latch = new CountDownLatch(1);
    this.description = description;
  }


  public abstract void execute() throws E;


  public void run() {
    try {
      thread = Thread.currentThread();
      if (logger.isDebugEnabled()) {
        logger.debug("Starting " + description + " with thread " + thread.getName());
      }
      execute();
    } catch (Throwable e) {
      Department.handleThrowable(e);
      exception = e;
    } finally {
      thread = null;
      latch.countDown();
      if (logger.isDebugEnabled()) {
        logger.debug("Finished " + description);
      }
    }
  }


  public Thread getThread() {
    return thread;
  }


  public Throwable getXynaException() {
    return exception;
  }


  public boolean await(long timeout) {
    try {
      return latch.await(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      return false;
    }
  }

}
