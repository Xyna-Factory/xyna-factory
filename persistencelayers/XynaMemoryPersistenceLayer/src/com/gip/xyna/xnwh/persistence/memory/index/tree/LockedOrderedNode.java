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
package com.gip.xyna.xnwh.persistence.memory.index.tree;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;


public abstract class LockedOrderedNode<E extends Comparable<E>> extends OrderedNode<E> {

  private int modCount;
  
  public LockedOrderedNode(Root root, E value) {
    super(root, value);
  }
  
  public abstract ReadWriteLock getLock();

  public abstract Lock readLock();
  
  protected int getModCount() {
    return modCount;
  }
  
  //sollte nur aufgerufen werden, wenn objekt gelockt ist => hier kein volatile notwendig
  protected void increaseModCount() {
    modCount++;
  }

}
