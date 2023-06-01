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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class SetableFuture<V> implements Future<V> {
  
  private volatile V value;
  private volatile Throwable exception;
  private final ReentrantLock lock;
  private final Condition condition;
  
  public SetableFuture() {
    lock = new ReentrantLock();
    condition = lock.newCondition();
  }

  public boolean cancel(boolean mayInterruptIfRunning) {
    lock.lock();
    try {
      if (gotResult()) {
        return false;
      } else {
        exception = new CancellationException();
        return true;
      }
    } finally {
      lock.unlock();
    }
  }

  public boolean isCancelled() {
    lock.lock();
    try {
      return exception != null && exception instanceof CancellationException;
    } finally {
      lock.unlock();
    }
  }

  public boolean isDone() {
    lock.lock();
    try {
      return gotResult();
    } finally {
      lock.unlock();
    }
  }

  public V get() throws InterruptedException, ExecutionException {
    lock.lock();
    try {
      while (!gotResult()) {
        condition.await();
      }
      return getResult();
    } finally {
      lock.unlock();
    }
  }

  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    lock.lock();
    try {
      if (gotResult()) {
        return getResult();
      }
      long awaitTimeout = System.nanoTime() + unit.toNanos(timeout);
      while (awaitTimeout > System.nanoTime()) {
        condition.await(awaitTimeout-System.nanoTime(), TimeUnit.NANOSECONDS);
      }
      if (gotResult()) {
        return getResult();
      } else {
        throw new TimeoutException();
      }
    } finally {
      lock.unlock();
    }
  }
  
  public void set(V value) throws IllegalStateException {
    lock.lock();
    try {
      if (gotResult()) {
        throw new IllegalStateException();
      } else {
        this.value = value;
        condition.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }
  
  
  public void setException(Throwable exception) throws IllegalStateException {
    lock.lock();
    try {
      if (gotResult()) {
        throw new IllegalStateException();
      } else {
        this.exception = exception;
        condition.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }
  
  
  private boolean gotResult() {
    return value != null || exception != null;
  }
  
  
  private V getResult() throws ExecutionException {
    if (exception != null) {
      if (exception instanceof ExecutionException) {
        throw (ExecutionException)exception;
      } else if (exception instanceof RuntimeException) {
        throw (RuntimeException)exception;
      } else if (exception instanceof Error) {
        throw (Error)exception;
      } else {
        throw new ExecutionException(exception);
      }
    } else {
      return value;
    }
  }

}
