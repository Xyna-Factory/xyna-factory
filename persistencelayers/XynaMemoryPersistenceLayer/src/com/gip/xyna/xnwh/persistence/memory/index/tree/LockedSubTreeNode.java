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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;



/**
 * holt nur writelocks. usecase:
 * 
 * <pre>
 * 
 *                A
 *               / \
 *              /   \
 *             B     C
 * 
 *</pre>
 * 
 * falls A ein {@link LockedSubTree} ist und B, C sind {@link LockedSubTreeNode}'s, dann ist zum zeitpunkt, wo
 * �nderungen an B und C durchgef�hrt werden, das readlock von A bereits geholt. die writelocks werden dann nun ggfs von
 * A geholt, wenn das readwritelock das entsprechende ist.<br>
 * ist n�tzlich, wenn locken teuer ist - zb bei coherence, wo beim locken ein netzwerkzugriff passieren muss.
 */
public class LockedSubTreeNode<E extends Comparable<E>> extends LockedOrderedNode<E> {

  private ReadWriteLock lock;

  public LockedSubTreeNode(Root root, E value, ReadWriteLock lock) {
    super(root, value);
    this.lock = lock;
  }


  @Override
  public int addChild(AbstractNode<E> child) {
    Lock wl = lock.writeLock();
    wl.lock();
    try {
      return super.addChild(child);
    } finally {
      wl.unlock();
    }
  }


  @Override
  protected void addChildInternally(int position, AbstractNode<E> child) {
    Lock wl = lock.writeLock();
    wl.lock();
    try {
      super.addChildInternally(position, child);
    } finally {
      wl.unlock();
    }
  }


  @Override
  protected AbstractNode<E> removeChildInternally(int i) {
    Lock wl = lock.writeLock();
    wl.lock();
    try {
      return super.removeChildInternally(i);
    } finally {
      wl.unlock();
    }
  }


  @Override
  public void setParent(AbstractNode<E> parent) {
    Lock wl = lock.writeLock();
    wl.lock();
    try {
      super.setParent(parent);
    } finally {
      wl.unlock();
    }
  }


  @Override
  public ReadWriteLock getLock() {
    return lock;
  }

  private static final Lock EMPTY_LOCK = new Lock() {

    public void lock() {
      
    }

    public void lockInterruptibly() throws InterruptedException {
      
    }

    public Condition newCondition() {
      return null;
    }

    public boolean tryLock() {
      return true;
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      return true;
    }

    public void unlock() {
    }
    
  };

  @Override
  public Lock readLock() {
    return EMPTY_LOCK;
  }


  @Override
  /**
   * ACHTUNG: parent muss bereits gelockt sein!
   */
  public AbstractNode<E> transform(NodeTypeTransformer<E> transformer) {    
    return super.transform(transformer);
  }

}
