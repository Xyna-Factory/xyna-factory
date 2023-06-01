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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public abstract class FutureWrapper<V> implements Future<V> {

  protected final Future<V> innerFuture;
  
  public FutureWrapper(Future<V> innerFuture) {
    this.innerFuture = innerFuture;
  }
  
  public boolean cancel(boolean mayInterruptIfRunning) {
    return innerFuture.cancel(mayInterruptIfRunning);
  }

  public boolean isCancelled() {
    return innerFuture.isCancelled();
  }

  public boolean isDone() {
    return innerFuture.isDone();
  }

  public V get() throws InterruptedException, ExecutionException {
    return innerFuture.get();
  }

  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return innerFuture.get(timeout, unit);
  }

}
