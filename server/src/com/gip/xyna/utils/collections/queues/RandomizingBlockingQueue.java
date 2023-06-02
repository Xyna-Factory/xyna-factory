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
package com.gip.xyna.utils.collections.queues;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RandomizingBlockingQueue ist eine Queue, aus der Elemente in einer zufälligen Reihenfolge gezogen werden können.
 * - offer(element) trägt dabei die Elemente ein, null ist nicht erlaubt
 * - poll oder take entfernen ein zufälliges Element aus der Queue und geben dieses zurück;
 *   das Verhalten unterscheidet sich bei leerer Collection:
 *   a) poll() liefert null, wenn die Queue leer ist
 *   b) take() oder poll(timeout, unit) blockieren, bis wieder ein Element in die Queue eingetragen wurde
 * - iterator() gibt Elemente in beliebiger interner Reihefolge aus, Reihenfolge hat keinen Zusammenhang mit poll oder take
 * - peek gibt ein beliebiges Element oder null aus, kein Zusammenhang mit poll: im Allgemeinen ist q.peek() != q.poll()
 */
public class RandomizingBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>{

  private final List<E> data = new ArrayList<E>();
  
  private final ReentrantLock lock = new ReentrantLock();
  
  private final Condition notEmpty= lock.newCondition();
  
  private final Random random = new Random();
  
  public RandomizingBlockingQueue() {
  }
  
  public RandomizingBlockingQueue(Collection<? extends E> c) {
    addAll(c);
  }

  @Override
  public boolean offer(E e) {
    if( e == null ) {
      throw new IllegalArgumentException("entry must not be null");
    }
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
      data.add(e);
      notEmpty.signal();
    } finally {
      lock.unlock();
    }
    return true;
  }

  private E removeRandom() {
    switch( data.size() ) {
    case 0: 
      return null;
    case 1:
      return data.remove(0);
    default:
      int lastIdx = data.size()-1;
      int index = random.nextInt(data.size());
      E value = data.get(index);
      E last = data.remove(lastIdx);
      if( index < lastIdx ) {
        data.set(index, last);
      }
      return value;
    }
  }
  
  @Override
  public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
      return removeRandom();
    } finally {
      lock.unlock();
    }
  }

  /** 
   * gibt ein beliebiges Element oder null aus, kein Zusammenhang mit poll: im Allgemeinen ist q.peek() != q.poll()
   */
  @Override
  public E peek() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
      if( data.size() > 0 ) {
        return data.get(data.size()-1);
      } else {
        return null;
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void put(E e) throws InterruptedException {
    offer(e);
  }

  @Override
  public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
    return offer(e);
  }

  @Override
  public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
      while (data.size() == 0) {
        notEmpty.await();
      }
      return removeRandom();
    } finally {
      lock.unlock();
    }
  }
    
  @Override
  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
      while (data.size() == 0) {
        if (nanos <= 0) {
          return null;
        }
        nanos = notEmpty.awaitNanos(nanos);
      }
      return removeRandom();
    } finally {
      lock.unlock();
    }
  }


  @Override
  public int remainingCapacity() {
    return Integer.MAX_VALUE;
  }

  @Override
  public int drainTo(Collection<? super E> c) {
    return drainTo(c, Integer.MAX_VALUE);
  }

  @Override
  public int drainTo(Collection<? super E> c, int maxElements) {
    if (c == null) {
      throw new NullPointerException();
    }
    if (c == this) {
      throw new IllegalArgumentException();
    }
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
      if( maxElements > data.size() ) {
        c.addAll(data);
        int size = data.size();
        data.clear();
        return size;
      } else {
        for( int i=0; i<maxElements; ++i ) {
          c.add(data.remove(data.size()-1));
        }
        return maxElements;
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Iterator<E> iterator() {
    return data.iterator();
  }

  @Override
  public int size() {
    return data.size();
  }

}
