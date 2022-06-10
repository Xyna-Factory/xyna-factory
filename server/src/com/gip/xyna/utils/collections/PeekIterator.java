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

import java.util.Iterator;
import java.util.ListIterator;


/**
 * PeekIterator ist eine Implemtierung des Iterator-Interfaces, mit der ein Eintrag bereits
 * gelesen werden kann, bevor die Methode next gerufen wird.
 * Dazu muss im Konstruktor der Iterator der zugrundeliegenden Collection übergeben werden.
 * Die Methode remove wird nur unterstützt, wenn ein ListIterator übergeben wurde.
 */
public class PeekIterator<E> implements java.util.Iterator<E> {

  private Iterator<E> iter;
  private ListIterator<E> listIter;
  private E peeked;
  private boolean isListIterator;
  
  public PeekIterator(java.util.Iterator<E> iter) {
    isListIterator = false;
    this.iter = iter;
    peeked = null;
  }
  public PeekIterator(ListIterator<E> iter) {
    isListIterator = true;
    this.iter = iter;
    this.listIter = iter;
    peeked = null;
  }
 
  public boolean hasNext() {
    if( isListIterator ) {
      return iter.hasNext();
    } else {
      if( peeked != null ) {
        return true;
      }
      return iter.hasNext();
    }
  }

  public E next() {
    try {
      if( isListIterator ) {
        return iter.next();
      } else {
        if( peeked != null ) {
          return peeked;
        }
        return iter.next();
      }
    } finally {
      peeked = null;
    }
  }

  public void remove() {
    if( ! isListIterator ) {
      //kann nicht funktionieren, wenn durch peek() der iterator schon weiter ist
      throw new UnsupportedOperationException();
    } else {
      if( peeked == null ) {
        listIter.remove();
      } else {
        listIter.previous();
        listIter.remove();
      }
    }
  }
  
  public E peek() {
    if( peeked != null ) {
      return peeked;
    }
    if( isListIterator ) {
      peeked = listIter.next();
      listIter.previous();
   } else {
      peeked = iter.next();
    }
    return peeked;
  }

  public Iterator<E> iterator() {
    return iter;
  }
  
  @Override
  public String toString() {
    if( hasNext() ) {
      return "PeekIterator("+peek()+")";
    } else {
      return "PeekIterator()";
    }
  }
  
}
