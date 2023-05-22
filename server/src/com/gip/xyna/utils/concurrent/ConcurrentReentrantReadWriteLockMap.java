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


import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;

public class ConcurrentReentrantReadWriteLockMap {

  
  protected final ConcurrentMapWithObjectRemovalSupport<Object, ReentrantLockWrapper> locks = new ConcurrentMapWithObjectRemovalSupport<Object, ReentrantLockWrapper>() {

    private static final long serialVersionUID = 1L;

    @Override
    public ReentrantLockWrapper createValue(Object key) {
      return new ReentrantLockWrapper();
    }
    
  };


  public void readLock(Object object) {
    locks.process(object, (x) -> {
      x.getLock().readLock().lock();
      return null;
    });
  }


  public void readUnlock(Object object) {
    locks.process(object, (x) -> {
      x.getLock().readLock().unlock();
      return null;
    });
  }


  public void writeLock(Object object) {
    locks.process(object, (x) -> {
      x.getLock().writeLock().lock();
      return null;
    });
  }


  public void writeUnlock(Object object) {
    locks.process(object, (x) -> {
      x.getLock().writeLock().unlock();
      return null;
    });
  }



 
}
