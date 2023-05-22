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



import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;



public class LockedSubTree<E extends Comparable<E>> extends LockedOrderedNode<E> {

  private ReadWriteLock lock;

  public LockedSubTree(Root root, E value, ReadWriteLock lock) {
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
  
  public ReadWriteLock getLock() {
    return lock;
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
  public AbstractNode<E> getChild(int i) {
    Lock rl = lock.readLock();
    rl.lock();
    try {
      return super.getChild(i);
    } finally {
      rl.unlock();
    }
  }


  @Override
  public List<AbstractNode<E>> getChildren() {
    Lock rl = lock.readLock();
    rl.lock();
    try {
      return super.getChildren();
    } finally {
      rl.unlock();
    }
  }


  @Override
  public int getNumberOfChildren() {
    Lock rl = lock.readLock();
    rl.lock();
    try {
      return super.getNumberOfChildren();
    } finally {
      rl.unlock();
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
  public AbstractNode<E> getParent() {
    Lock rl = lock.readLock();
    rl.lock();
    try {
      return super.getParent();
    } finally {
      rl.unlock();
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
  public Lock readLock() {
    return lock.readLock();
  }


  @Override
  public int getChildIndex(AbstractNode<E> child) {
    Lock rl = lock.readLock();
    rl.lock();
    try {
      return super.getChildIndex(child);
    } finally {
      rl.unlock();
    }
  }


  @Override
  /**
   * ACHTUNG: parent muss bereits gelockt sein!
   */
  public AbstractNode<E> transform(NodeTypeTransformer<E> transformer) {    
    return super.transform(transformer);
  }
  
  
  public String toString() {
    return "L-" + super.toString();
  }


}
