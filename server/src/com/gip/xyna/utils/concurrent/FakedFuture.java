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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class FakedFuture<V> implements Future<V> {

  public enum ExecutionState {RUNNING, FINISHED, FAILED, CANCELLED};
  
  private V result;
  private ExecutionState state = ExecutionState.RUNNING;
  private ExecutionException exception;
  
  public synchronized V get() throws InterruptedException, ExecutionException {
    while (state == ExecutionState.RUNNING) {
      this.wait();
    }
    if (state == ExecutionState.FAILED) {
      throw exception;
    }
    return this.result;
  }
  
  
  public synchronized V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    long timout = System.currentTimeMillis() + unit.toMillis(timeout);
    while (state == ExecutionState.RUNNING && System.currentTimeMillis() < timout) {
      this.wait(timout - System.currentTimeMillis());
    }
    switch (state) {
      case RUNNING :
        throw new TimeoutException();
      case FAILED :
        throw exception;
      case CANCELLED :
        throw new TimeoutException("Cancelled!");
      default :
        break;
    }
    return this.result;
  }
  
  
  public synchronized void set(V result) {
    this.result = result;
    this.state = ExecutionState.FINISHED;
    this.notifyAll();
  }
  
  
  public synchronized void injectException(Throwable exception) {
    this.exception = new ExecutionException(exception);
    this.state = ExecutionState.FAILED;
    this.notifyAll();
  }
  
  public synchronized ExecutionState getState() {
    return state;
  }


  public synchronized boolean cancel(boolean mayInterruptIfRunning) {
    if (!isDone()) {
      this.state = ExecutionState.CANCELLED;
      this.notifyAll();
      return true;
    } else {
      return false;
    }
  }


  public synchronized boolean isCancelled() {
    return state == ExecutionState.CANCELLED;
  }


  public synchronized boolean isDone() {
    return state != ExecutionState.RUNNING;
  }
  
  
}
