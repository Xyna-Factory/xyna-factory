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
package com.gip.xyna.utils.collections.queues;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * BlockingQueue-Implementierung, die alle Aufrufe an eine im Konstruktor �bergebene BlockingQueue weiterreicht.
 * Dies vereinfacht das Implementieren von Dekoratoren. 
 */
public class WrappedBlockingQueue<E> implements BlockingQueue<E> {
  
  protected BlockingQueue<E> wrapped;
  
  protected  WrappedBlockingQueue( BlockingQueue<E> queue ) {
    this.wrapped = queue;
  }

  public E poll() {
    return wrapped.poll();
  }

  public E remove() {
    return wrapped.remove();
  }

  public E peek() {
    return wrapped.peek();
  }

  public E element() {
    return wrapped.element();
  }

  public int size() {
    return wrapped.size();
  }

  public boolean isEmpty() {
    return wrapped.isEmpty();
  }

  public boolean contains(Object o) {
    return wrapped.contains(o);
  }

  public Iterator<E> iterator() {
    return wrapped.iterator();
  }

  public Object[] toArray() {
   return wrapped.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return wrapped.toArray(a);
  }

  public boolean remove(Object o) {
    return wrapped.remove(o);
  }

  public boolean containsAll(Collection<?> c) {
    return wrapped.containsAll(c);
  }

  public boolean addAll(Collection<? extends E> c) {
    return wrapped.addAll(c);
  }

  public boolean removeAll(Collection<?> c) {
    return wrapped.removeAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return wrapped.retainAll(c);
  }

  public void clear() {
    wrapped.clear();
  }

  public boolean offer(E o) {
    return wrapped.offer(o);
  }

  public boolean offer(E o, long timeout, TimeUnit unit) throws InterruptedException {
    return wrapped.offer(o, timeout, unit);
  }

  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    return wrapped.poll(timeout, unit);
  }

  public E take() throws InterruptedException {
    return wrapped.take();
  }

  public void put(E o) throws InterruptedException {
    wrapped.put(o);
  }

  public int remainingCapacity() {
    return wrapped.remainingCapacity();
  }

  public boolean add(E o) {
    return wrapped.add(o);
  }

  public int drainTo(Collection<? super E> c) {
    return wrapped.drainTo(c);
  }

  public int drainTo(Collection<? super E> c, int maxElements) {
    return wrapped.drainTo(c, maxElements);
  }
  
  @Override
  public String toString() {
    return wrapped.toString();
  }
  
  @Override
  public int hashCode() {
    return wrapped.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    return wrapped.equals(obj);
  }

}
