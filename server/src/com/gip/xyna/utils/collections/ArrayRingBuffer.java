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
package com.gip.xyna.utils.collections;

import java.lang.reflect.Array;

/**
 * nicht synchronisierter einfacher ringbuffer basierend auf einem array 
 */
public class ArrayRingBuffer<E> {

  private final Object[] arr;
  private int start; //Nächster zu belegender Index
  private int size; //Anzahl belegter Objekte (maximal gleich capacity)
  private final int capacity; //Maximale Anzahl
  
  public ArrayRingBuffer(int capacity) {
    if (capacity == 0) {
      throw new IllegalArgumentException();
    }
    arr = new Object[capacity];
    this.capacity = capacity;
  }
  
  public void add(E e) {
    if (size != capacity) {
      size++;
    }
    arr[start] = e;
    start = (start + 1) % capacity;
  }
  
  public int size() {
    return size;
  }
  
  /**
   * RingBuffer Inhalt wird als Array zurückgegeben. Ältestes Element ist an Stelle 0, neustes Element an Stelle (size-1).
   * 
   * Falls übergebenes Array zu klein ist, wird ein neues erzeugt, ansonsten wird es befüllt. 
   * @return
   */
  @SuppressWarnings("unchecked")
  public E[] getOrdered(E[] array) {
    E[] ret = array;
    if (array.length < size) {
      ret = (E[]) Array.newInstance(array.getClass().getComponentType(), size);
    }
    if (size == capacity && start != 0) {
      System.arraycopy(arr, start, ret, 0, capacity-start); //alten objekte fangen da an, wo der nächste einfügepunkt ist
      System.arraycopy(arr, 0, ret, capacity-start, start); //neuen objekte enden da, wo der nächste einfügepunkt ist
    } else {
      System.arraycopy(arr, 0, ret, 0, size);
    }
    return ret;
  }

  /**
   * @param relativeIndex 0 -&gt; ältestes element
   * @return
   */
  @SuppressWarnings("unchecked")
  public E get(int relativeIndex) {
    if (relativeIndex >= size) {
      throw new ArrayIndexOutOfBoundsException(relativeIndex);
    }
    return (E) arr[(start + relativeIndex) % size];
  }
  
  
}
