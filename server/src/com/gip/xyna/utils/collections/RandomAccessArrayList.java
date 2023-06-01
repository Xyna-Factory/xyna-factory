/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;


/**
 * RandomAccessArrayList bietet anders als die übliche ArrayList einen beliebigen Zugriff über den Index,
 * ohne eine Exception zu werfen, wenn der Index zu hoch ist. 
 * Stattdessen werden sinnvolle Operationen durchgeführt:
 * - set(index,E element) und add(int index, E element) vergrößern die Liste entsprechend
 * - get(index) und remove(index) geben null zurück
 * Negative Indexe führen zum gewohnten Fehler
 * 
 * FIXME vollständig implementieren! listIterator, subList, ?resize()?
 * 
 */
public class RandomAccessArrayList<E> extends WrappedList<E> implements Serializable {

  private static final long serialVersionUID = 1L;

  public RandomAccessArrayList() {
    super(new ArrayList<E>() );
  }

  public RandomAccessArrayList(int initialCapacity) {
    super(new ArrayList<E>(initialCapacity) );
  }
  
  public RandomAccessArrayList(Collection<? extends E> c) {
    super(new ArrayList<E>(c) );
  }
 
  public E get(int index) {
    if( index < size() ) {
      return wrapped.get(index);
    } else {
      return null;
    }
  }

  public E set(int index, E element) {
    if( index < size() ) {
      return wrapped.set(index, element);
    } else {
      for( int idx = size(); idx<index; ++idx ) {
        add(null);
      }
      add(element);
      return null;
    }
  }

  public void add(int index, E element) {
    if( index < size() ) {
      wrapped.add(index, element);
    } else {
      for( int idx = size(); idx<index; ++idx ) {
        add(null);
      }
      add(element);
    }
  }

  public E remove(int index) {
    if( index < size() ) {
      return wrapped.remove(index);
    } else {
      return null;
    }
  }

  public ListIterator<E> listIterator(int index) {
    //TODO
    throw new UnsupportedOperationException("listIterator("+index+") is not supported for RandomAccessArrayList");
  }

  public List<E> subList(int fromIndex, int toIndex) {
    //TODO
    throw new UnsupportedOperationException("subList("+fromIndex+","+toIndex+") is not supported for RandomAccessArrayList");
  }

}
