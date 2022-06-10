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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class FutureCollection<E> /*could: implements Collection*/{
  
  @SuppressWarnings("rawtypes")
  private final static FutureCollection EMPTY = new FutureCollection();

  protected List<Future<E>> futures;
  
  
  public FutureCollection() {
    futures = new ArrayList<Future<E>>();
  }
  
  
  public FutureCollection(Collection<Future<E>> futures) {
    this.futures = new ArrayList<Future<E>>();
    for (Future<E> future : futures) {
      add(future);
    }
  }
  
  public synchronized void add(Future<E> future) {
    futures.add(future);
  }
  
  public synchronized void addAll(FutureCollection<E> futures) {
    this.futures.addAll(futures.futures);
  }
  
  public synchronized List<E> get() throws InterruptedException, ExecutionException {
    List<E> results = new ArrayList<E>();
    for (Future<E> future : futures) {
      results.add(future.get());
    }
    return results;
  }
  
  
  public synchronized List<E> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    List<E> results = new ArrayList<E>();
    for (Future<E> future : futures) {
      results.add(future.get(timeout, unit));
    }
    return results;
  }
  
  
  
  public int size() {
    return futures.size();
  }
  
  
  @SuppressWarnings("unchecked")
  public static <E> FutureCollection<E> empty() {
    return EMPTY;
  }
  
  
  public static <E> FutureCollection<E> from(Collection<Future<E>> futures) {
    return new FutureCollection<E>(futures);
  }
  
}
