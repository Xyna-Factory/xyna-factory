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

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * BlockingQueue-Implementierung, die sich wie ein RingBuffer verhält.
 * 
 * Selbst wenn die Queue mit der konfigurierten Kapazität gefüllt ist und andere Threads 
 * blockierend warten, dass sie Einträge hinzufügen können, kann mit der exchange-Methode
 * ein neuer Eintrag eingetragen werden indem gleichzeitig der älteste Eintrag entfernt wird.
 *
 */
public class RingbufferBlockingQueue<E> extends LinkedBlockingQueueWithAccessibleLocks<E> {
  
  public RingbufferBlockingQueue(int capacity) {
    super(capacity);
  }
  
  public RingbufferBlockingQueue(LinkedBlockingQueue<E> queue) {
    super(queue);
  }
 
  public static interface QueueFilter<E> {
    boolean filter(E e);
  }
  
  /**
   * gefiltertes Poll: gesucht wird ältester Eintrag, der zum Filter passt (Filter meldet true)
   * @param queueFilter
   * @return
   */
  public E filteredPoll( QueueFilter<E> queueFilter ) {
    putLock.lock();
    takeLock.lock();
    try {
      return internalFilteredPoll(queueFilter);
    } finally {
      takeLock.unlock();
      putLock.unlock();
    }
  }
  
  /**
   * gefiltertes Peek: gesucht wird ältester Eintrag, der zum Filter passt (Filter meldet true)
   * @param queueFilter
   * @return
   */
  public E filteredPeek( QueueFilter<E> queueFilter ) {
    putLock.lock();
    takeLock.lock();
    try {
      return internalFilteredPeek(queueFilter);
    } finally {
      takeLock.unlock();
      putLock.unlock();
    }
  }
  
 
  
  
  /**
   * Erst ein poll, dann ein offer. Abgesichert durch Locks, so dass kein anderer Thread stört
   * @param newest
   * @return eldest ältester Eintrag im RingBuffer
   */
  public E exchange( E newest )  {
    putLock.lock();
    takeLock.lock();
    try {
      return internalExchange(newest);
    } finally {
      takeLock.unlock();
      putLock.unlock();
    }
  }

  /**
   * Erst ein offer, bei Nicht-Erfolg ein Exchange (poll, dann offer)
   * @param newest
   * @return eldest null, wenn offer erfolgreich ist, ansonsten ältester Eintrag im RingBuffer
   */
  public E offerOrExchange( E newest ) {
    putLock.lock();
    takeLock.lock();
    try {
      if( offer( newest ) ) {
        return null;
      } else {
        return internalExchange(newest);
      }
    } finally {
      takeLock.unlock();
      putLock.unlock();
    }
  }
    
  /**
   * Erst ein filteredPoll, dann ein offer. Abgesichert durch Locks, so dass kein anderer Thread stört
   * @param queueFilter
   * @param newest
   * @return ältester Eintrag im RingBuffer, der Filter erfüllt oder newest, falls Filter nicht erfüllt wird
   */
  public E filteredExchange( QueueFilter<E> queueFilter, E newest )  {
    putLock.lock();
    takeLock.lock();
    try {
      return internalFilteredExchange(queueFilter,newest);
    } finally {
      takeLock.unlock();
      putLock.unlock();
    }
  }

  /**
   * Erst ein offer, bei Nicht-Erfolg ein filteredExchange (filteredPoll, dann offer)
   * @param queueFilter
   * @param newest
   * @return null, wenn offer erfolgreich ist, ansonsten ältester Eintrag im RingBuffer den Filter findet oder newest
   */
  public E offerOrFilteredExchange( QueueFilter<E> queueFilter, E newest ) {
    putLock.lock();
    takeLock.lock();
    try {
      if( offer( newest ) ) {
        return null;
      } else {
        return internalFilteredExchange(queueFilter, newest);
      }
    } finally {
      takeLock.unlock();
      putLock.unlock();
    }
  }
  
  
  private E internalFilteredPoll(QueueFilter<E> queueFilter) {
    Iterator<E> iter = wrapped.iterator();
    while (iter.hasNext()) {
      E entry = iter.next();
      if( queueFilter.filter(entry) ) {
        iter.remove();
        return entry;
      }
    }
    return null;
  }
  
  private E internalFilteredPeek(QueueFilter<E> queueFilter) {
    Iterator<E> iter = wrapped.iterator();
    while (iter.hasNext()) {
      E entry = iter.next();
      if( queueFilter.filter(entry) ) {
        return entry;
      }
    }
    return null;
  }
 
  private void silentPut(E newest) {
    //kein put, da dann InterruptedException zu behandeln wäre.
    //offer/put sollte immer erfolgreich sein, da vorher ein poll ausgeführt wurde
    if( ! offer( newest ) ) {
      throw new IllegalStateException("Could not offer after poll");
    }
  }
 
  private E internalExchange(E newest) {
    E eldest = poll();
    silentPut(newest);
    return eldest;
  }

  private E internalFilteredExchange(QueueFilter<E> pollFilter, E newest) {
    E eldest = internalFilteredPoll(pollFilter);
    if( eldest == null ) {
      return newest;
    }
    silentPut(newest);
    return eldest;
  }

}
