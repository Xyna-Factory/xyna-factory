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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.collections.Pair;

/**
 * SimpleDelayQueue ist eine Queue, bei der die Eintr�ge erst nach 
 * einer Wartezeit wieder ausgelesen werden k�nnen.
 * 
 * SimpleDelayQueue is eine Vereinfachtung von java.util.concurrent.DelayQueue:
 * Gespeicherte Eintr�ge m�ssen nicht mehr das Interface Delayed erf�llen.
 * Bei offer und add kann stattdessen direkt die Zeit angegeben werden, nach der die
 * Eintr�ge aus der Queue abgeholt werden k�nnen.
 *
 */
public class SimpleDelayQueue<E> implements BlockingQueue<E> {
 
  private DelayQueue<DelayedEntry<E>> dq = new DelayQueue<DelayedEntry<E>>();
  private long defaultDelay;
 
  /**
   * Note: this class has a natural ordering that is inconsistent with equals.
   */
  private static class DelayedEntry<E> implements Delayed {

    private E entry;
    private long absoluteTimeInMillis;
    
    public DelayedEntry(E entry, long absoluteTimeInMillis) {
      this.entry = entry;
      this.absoluteTimeInMillis = absoluteTimeInMillis;
    }
    
    @Override
    public String toString() {
      return String.valueOf(entry);
    }

    @Override
    public int compareTo(Delayed o) {
      if( o instanceof DelayedEntry ) {
        return (int)(getTimeInMillis()-((DelayedEntry<?>)o).getTimeInMillis());
      } else {
        return (int)(getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
      }
    }
    
    @Override
    public int hashCode() {
      return entry == null ? 0 : entry.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
      if( o instanceof DelayedEntry ) {
        DelayedEntry<?> other = (DelayedEntry<?>)o;
        if( entry == null ) {
          return other.entry == null;
        }
        return entry.equals(other.entry);
      } else {
        return false;
      }
    }
    
    private long getTimeInMillis() {
      return absoluteTimeInMillis;
    }
    
    public Pair<E,Long> toPair(long timeBaseInMillis) {
      return Pair.of(entry, absoluteTimeInMillis - timeBaseInMillis);
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
      long delay = absoluteTimeInMillis - System.currentTimeMillis();
      if( unit == TimeUnit.MILLISECONDS ) {
        return delay;
      } else {
        return unit.convert(delay, TimeUnit.MILLISECONDS);
      }
    }
    
    public static <E> E extract(DelayedEntry<E> de) {
      if( de == null ) {
        return null;
      } else {
        return de.entry;
      }
    }

    public static <E> DelayedEntry<E> wrap(E e, long delay) {
      return new DelayedEntry<E>(e, System.currentTimeMillis() + delay);
    }

    public static <E> DelayedEntry<E> wrapAbsolute(E e, long absoluteTime) {
      return new DelayedEntry<E>(e, absoluteTime);
    }
  }
  
  
  public SimpleDelayQueue() {
    this.defaultDelay = 0;
  }
  public SimpleDelayQueue(long defaultDelayInMillis) {
    this.defaultDelay = defaultDelayInMillis;
  }

  public void setDefaultDelay(long defaultDelay) {
    this.defaultDelay = defaultDelay;
  }
  public long getDefaultDelay() {
    return defaultDelay;
  }
  
  public boolean offer(E e, long ttl) {
    return dq.offer(DelayedEntry.wrap(e, ttl));
  }

  public boolean offerAbsolute(E e, long absoluteTime) {
    return dq.offer(DelayedEntry.wrapAbsolute(e, absoluteTime));
  }
  

  public void addAbsolute(E e, long absoluteTime) {
    dq.offer(DelayedEntry.wrapAbsolute(e, absoluteTime));
  }
  
  public void add(E e, long ttl) {
    dq.offer(DelayedEntry.wrap(e, ttl));
  }

  public List<Pair<E,Long>> listAllEntriesOrdered( long timeBaseInMillis ) {
    List<Pair<E,Long>> list = CollectionUtils.transform(dq, new ToPairTrafo<E>(timeBaseInMillis) );
    Collections.sort(list, Pair.<Long>comparatorSecond() );
    return list;
  }
  
  private static class ToPairTrafo<E> implements Transformation<DelayedEntry<E>, Pair<E,Long>> {

    private long timeBaseInMillis;

    public ToPairTrafo(long timeBaseInMillis) {
      this.timeBaseInMillis = timeBaseInMillis;
    }

    @Override
    public Pair<E, Long> transform(DelayedEntry<E> from) {
      return from.toPair(timeBaseInMillis);
    }
    
  }
  
  
  
  
  public E poll() {
    return DelayedEntry.extract(dq.poll());
  }

  public E remove() {
    return DelayedEntry.extract(dq.remove());
  }

  public E peek() {
    return DelayedEntry.extract(dq.peek());
  }

  public E element() {
    return DelayedEntry.extract(dq.element());
  }

  public int size() {
    return dq.size();
  }

  public boolean isEmpty() {
    return dq.isEmpty();
  }

  public boolean contains(Object o) {
    return dq.contains(o);
  }

  public Iterator<E> iterator() {
    return new EntryIterator<E>( dq.iterator() );
  }
  
  public static class EntryIterator<E> implements Iterator<E> {

    private Iterator<DelayedEntry<E>> iterator;

    public EntryIterator(Iterator<DelayedEntry<E>> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public E next() {
      return DelayedEntry.extract(iterator.next());
    }

    @Override
    public void remove() {
      iterator.remove();
    }
    
  }

  
  
  public Object[] toArray() {
   return dq.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return dq.toArray(a);
  }

  public boolean remove(Object o) {
    return dq.remove( DelayedEntry.wrap(o, defaultDelay) );
  }

  public boolean containsAll(Collection<?> c) {
    return dq.containsAll(c);
  }

  public boolean addAll(Collection<? extends E> c) {
    if (c == null)
      throw new NullPointerException();
    if (c == this)
      throw new IllegalArgumentException();
    boolean modified = false;
    for (E e : c) {
      if (add(e)) {
        modified = true;
      }
    }
    return modified;
  }

  public boolean removeAll(Collection<?> c) {
    return dq.removeAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return dq.retainAll(c);
  }

  public void clear() {
    dq.clear();
  }

  public boolean offer(E o) {
    return dq.offer(DelayedEntry.wrap(o, defaultDelay));
  }

  public boolean offer(E o, long timeout, TimeUnit unit) throws InterruptedException {
    return dq.offer(DelayedEntry.wrap(o, defaultDelay), timeout, unit);
  }

  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    return DelayedEntry.extract(dq.poll(timeout, unit));
  }

  public E take() throws InterruptedException {
    return DelayedEntry.extract(dq.take());
  }

  public void put(E o) throws InterruptedException {
    dq.put(DelayedEntry.wrap(o, defaultDelay));
  }

  public int remainingCapacity() {
    return dq.remainingCapacity();
  }

  public boolean add(E o) {
    return dq.add(DelayedEntry.wrap(o, defaultDelay));
  }

  public int drainTo(Collection<? super E> c) {
    if (c == null)
      throw new NullPointerException();
    if (c == this)
      throw new IllegalArgumentException();
    List<DelayedEntry<E>> drain = new ArrayList<DelayedEntry<E>>();
    int size = dq.drainTo(drain);
    for( DelayedEntry<E> de : drain ) {
      c.add( DelayedEntry.extract(de) );
    }
    return size;
  }

  public int drainTo(Collection<? super E> c, int maxElements) {
    if (c == null)
      throw new NullPointerException();
    if (c == this)
      throw new IllegalArgumentException();
    List<DelayedEntry<E>> drain = new ArrayList<DelayedEntry<E>>();
    int size = dq.drainTo(drain, maxElements);
    for( DelayedEntry<E> de : drain ) {
      c.add( DelayedEntry.extract(de) );
    }
    return size;
  }
  
  @Override
  public String toString() {
    return dq.toString();
  }

}
