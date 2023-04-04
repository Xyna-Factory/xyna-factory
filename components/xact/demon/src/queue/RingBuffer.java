/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package queue;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Implementation eines RingBuffers
 *
 * @param <T>
 */
public class RingBuffer<T> extends AbstractQueue<T> {

  private T[] buffer;
  private int first;
  private int size;
  private int capacity;
  
  
  @SuppressWarnings("unchecked")
  public RingBuffer( int capacity ) {
    this.capacity = capacity;
    buffer = (T[])new Object[capacity];
    first = 0;
    size = 0; 
  }
  
  @Override
  public Iterator<T> iterator() {
    return Arrays.asList(buffer).iterator();
    //throw new UnsupportedOperationException("Schwierige Implementation...");
  }

  @Override
  public int size() {
    return size;
  }
  
  /**
   * Größe des RingBuffers
   * @return
   */
  public int capacity() {
    return capacity;
  }

  
  /* (non-Javadoc)
   * @see java.util.Queue#offer(java.lang.Object)
   */
  public boolean offer(T o) {
    if( size == capacity ) {
      return false; 
    }
    buffer[ (first+size)%capacity ] = o;
    ++size;
    return true;
  }

  /* (non-Javadoc)
   * @see java.util.Queue#peek()
   */
  public T peek() {
    if( size == 0 ) {
      return null;
    } else {
      return buffer[first];
    }
  }

  /* (non-Javadoc)
   * @see java.util.Queue#poll()
   */
  public T poll() {
    if( size == 0 ) {
      return null;
    } else {
      T t = buffer[first];
      buffer[first] = null; //keine unnötige Referenz behalten
      --size;
      first = (first + 1) % capacity; // wrap-around
      return t;
    }
  }
  
  /**
   * Entfernt den übergebenen Eintrag aus dem RingBuffer, falls dieser bei poll() ausgegeben würde.
   * Damit ist es möglich, die aktuellen Daten im Buffer zu behalten 
   * (T t = ringBuffer.peek(); process(t); ringBuffer.poll(t); )
   * @param data
   * @return
   */
  public boolean poll(T data) {
    if( data == null ) {
      return false;
    }
    if( data.equals( peek() ) ) {
      poll();
      return true;
    }
    return false;
  }
  
  /**
   * Kopie des RingBuffers in die übergebene Collection
   * @param collection
   */
  public void copyTo( Collection<T> collection ) {
    for( T t : buffer ) {
      if( t != null ) {
        collection.add(t);
      }
    }
  }
  
}
