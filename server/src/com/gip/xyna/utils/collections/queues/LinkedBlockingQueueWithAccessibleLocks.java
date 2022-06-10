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
package com.gip.xyna.utils.collections.queues;

import java.lang.reflect.Field;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;


/**
 *
 */
public class LinkedBlockingQueueWithAccessibleLocks<E> extends WrappedBlockingQueue<E>{

  /** Lock held by take, poll, etc */
  protected final ReentrantLock takeLock;

  /** Lock held by put, offer, etc */
  protected final ReentrantLock putLock;

  public LinkedBlockingQueueWithAccessibleLocks() {
    this(Integer.MAX_VALUE);
  }
  
  public LinkedBlockingQueueWithAccessibleLocks(int capacity) {
    super(new LinkedBlockingQueue<E>(capacity));
    this.takeLock = extractLock( "takeLock" );
    this.putLock = extractLock( "putLock" );
  }
  
  public LinkedBlockingQueueWithAccessibleLocks(LinkedBlockingQueue<E> queue) {
    super(queue);
    this.takeLock = extractLock( "takeLock" );
    this.putLock = extractLock( "putLock" );
  }
  
  private ReentrantLock extractLock(String lockName) {
    try {
      Field lockField = LinkedBlockingQueue.class.getDeclaredField(lockName);
      lockField.setAccessible(true);
      return (ReentrantLock) lockField.get(wrapped);
    } catch (Exception e) {
      throw new IllegalStateException( "Could not get "+lockName, e);
    }
  }

}
