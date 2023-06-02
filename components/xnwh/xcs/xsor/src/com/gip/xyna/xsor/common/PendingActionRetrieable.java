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
package com.gip.xyna.xsor.common;

import org.apache.log4j.Logger;

import com.gip.xyna.xsor.common.exceptions.ActionNotAllowedInClusterStateException;
import com.gip.xyna.xsor.common.exceptions.CollisionWithRemoteRequestException;
import com.gip.xyna.xsor.common.exceptions.RemoteProcessExecutionTimeoutException;
import com.gip.xyna.xsor.common.exceptions.RetryDueToPendingCreateAction;


public abstract class PendingActionRetrieable<R> {

  
  private static final Logger logger = Logger.getLogger(PendingActionRetrieable.class.getName());
  public static final int RETRIES = 50;
  
  private int maxRetries;
  
  
  public PendingActionRetrieable() {
    this(RETRIES);
  }
  
  public PendingActionRetrieable(int retries) {
    maxRetries = retries;
  }
  
  
  public R execute() throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException, ActionNotAllowedInClusterStateException {
    int retries = 0;
    while (retries++ < maxRetries) {
      try {
       return executeInternally();
      } catch (RetryDueToPendingCreateAction e) {
        if (logger.isDebugEnabled()) {
          logger.debug("retrying request", e);
        }
        backoff(retries);
      }
    }    
    throw new RuntimeException("Got stuck retrying execution for " + maxRetries);
  }
  
  public abstract R executeInternally() throws RetryDueToPendingCreateAction, RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException, ActionNotAllowedInClusterStateException;
  
  
  private void backoff(int retries) {
    int sleeptime = retries * 10;
    try {
      Thread.sleep(Math.min(sleeptime, 500));
    } catch (InterruptedException e) {
      // TODO what's the worse problem: spurious wakeups or retrying a lot while on termination request? 
      throw new RuntimeException(e);
    }
  }
  
}
