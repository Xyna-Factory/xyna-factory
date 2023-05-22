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


import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.gip.xyna.utils.collections.HashCodeMap;


public class HashParallelReentrantReadWriteLocks {

  private HashCodeMap<ReentrantReadWriteLock> locks;
  
  
  public HashParallelReentrantReadWriteLocks() {
    this(32);
  }
  
  
  public HashParallelReentrantReadWriteLocks(int parallel) {
    locks = new HashCodeMap<ReentrantReadWriteLock>(parallel, new HashCodeMap.Constructor<ReentrantReadWriteLock>() {
      public ReentrantReadWriteLock newInstance() {
        return new ReentrantReadWriteLock();
      }
    });
  }
  

  public void readLock(Object object) {
    locks.get(object).readLock().lock();
  }
  
  
  public void readUnlock(Object object) {
    locks.get(object).readLock().unlock();
  }
  
  
  public void writeLock(Object object) {
    locks.get(object).writeLock().lock();
  }
  
  
  public void writeUnlock(Object object) {
    locks.get(object).writeLock().unlock();
  }
 

}
