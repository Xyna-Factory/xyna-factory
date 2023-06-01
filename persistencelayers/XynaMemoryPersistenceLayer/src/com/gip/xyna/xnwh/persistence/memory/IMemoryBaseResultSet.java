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

package com.gip.xyna.xnwh.persistence.memory;



import java.util.List;



public abstract class IMemoryBaseResultSet extends BaseResultSet {
  
  private boolean interruptedIndexTraversal = false;
  
  public abstract void unlockReadLocks();
  public abstract int size();

  public abstract List<MemoryRowLock> getWriteLocks();


  public void unlockWriteLocks() {
    List<MemoryRowLock> locks = getWriteLocks();
    if (locks != null) {
      for (MemoryRowLock lock : locks) {
        lock.sustainedLock().unlock();
      }
    }
  }
  

  public void setInterruptedIndexTraversal(boolean b) {
    this.interruptedIndexTraversal = b;
  }

  public boolean isInterruptedIndexTraversal() {
    return interruptedIndexTraversal;
  }
}
