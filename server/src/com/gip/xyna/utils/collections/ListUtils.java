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
package com.gip.xyna.utils.collections;

import java.util.List;

/**
 *  
 * Aufruf: ListUtils.insert(value).into(list).at(index); 
 *         ListUtils.insert(value).into(list).after(value2); 
 *         ListUtils.move(value).in(list).to(index); 
 *         ListUtils.move(value1).in(list).after(value2); 
 *         ListUtils.moveIn(list).from(index1).to(index2); 
 *         ListUtils.moveIn(list).from(index).before(value2); 
 *         ListUtils.transfer(value).from(list).to(list2).at(index2); 
 *         ListUtils.transferFrom(list1).atIndex(index1).to(list2).before(value2); 
 *
 */
public class ListUtils {
  
  /**
   * Aufruf insert(value).into(list).[atEnd(),at(index),after(value2),before(value2)];
   * @param entry
   * @return
   */
  public static <T> Insert<T> insert(T entry) {
    return new Insert<T>(entry);
  }
  
  /**
   * Aufruf move(value).in(list).[atEnd(),at(index),after(value2),before(value2)]; 
   * @param entry
   * @return
   */
  public static <T> Move<T> move(T entry) {
    return new Move<T>(entry);
  }
  /**
   * Aufruf moveIn(list).from(index1).[atEnd(),at(index2),after(value),before(value)]; 
   * @param in
   * @return
   */
  public static <T> Move<T> moveIn(List<? extends T> in) {
    return new Move<T>(in);
  }
  
  /**
   * Aufruf transfer(value).from(list1).to(list2).[atEnd(),at(index),after(value2),before(value2)]; 
   * @param entry
   * @return
   */
  public static <T> Transfer<T> transfer(T entry) {
    return new Transfer<T>(entry);
  }
  /**
   * Aufruf transferFrom(list1).atIndex(index1).to(list2).[atEnd(),at(index2),after(value),before(value)]; 
   * @return
   */
  public static <T> Transfer<T> transferFrom(List<? extends T> from) {
    return new Transfer<T>(from);
  }
  
  
  
  public static abstract class Position<E> {
    
    public void at(Integer index) {
      at( index == null ? -1 : index.intValue() );
    }

    public abstract void at(int index);
    
    public void atEnd() {
      at(-1);
    }
    
    public void after(E element) {
      at( indexOf(element)+1);
    }

    public void before(E element) {
      at( indexOf(element) );
    }

    protected abstract int indexOf(E element);
    
    protected int indexOf(List<E> list, E element) {
      int idx = list.indexOf(element);
      if( idx < 0 ) {
        throw new IllegalStateException("element is no member of list");
      }
      return idx;
    }
  }
  
  

  public static class Insert<E> extends Position<E> {

    private E entry;
    private List<E> into;

    public Insert(E entry) {
      this.entry = entry;
    }
   
    @SuppressWarnings("unchecked")
    public Insert<E> into(List<? extends E> into) {
      this.into = (List<E>)into;
      return this;
    }

    @Override
    public void at(int index) {
      if( index < 0 ) {
        into.add(entry);
      } else {
        into.add(index, entry);
      }
    }
    
    protected int indexOf(E element) {
      return indexOf(into, element);
    }
  }

  public static class Move<E> extends Position<E> {

    public Move(E entry) {
      this.entry = entry;
    }
    
    public Move(List<? extends E> in) {
      in(in);
    }

    protected E entry;
    protected List<E> in;
    protected int index;
    protected boolean oldIndexUsed;
    
    @SuppressWarnings("unchecked")
    public Move<E> in(List<? extends E> in) {
      this.in = (List<E>) in;
      calcIndex();
      return this;
    }
    
    public Move<E> indexIsBeforeMove() {
      oldIndexUsed = true;
      return this;
    }
    
    public Move<E> from(int index) {
      this.index = index;
      return this;
    }

    private void calcIndex() {
      if( in == null ) {
        return; //nichts zu tun
      } 
      boolean oiu = oldIndexUsed;
      if( entry == null ) {
        index = in.indexOf(entry); //TODO NPE m�glich
      } else {
        index = indexOf(entry);
      }
      oldIndexUsed = oiu;
    }

    public void at(int index) {
      E element = in.remove(this.index);
      if( index < 0 ) {
        in.add(element);
      } else {
        if( this.index < index && oldIndexUsed ) {
          in.add(index-1,element);
        } else {
          in.add(index,element);
        }
      }
    }
    
    protected int indexOf(E element) {
      oldIndexUsed = true;
      return indexOf(in, element);
    }
    
  }
  
  public static class Transfer<E> extends Position<E> {

    public Transfer(E entry) {
      this.entry = entry;
    }
    
    public Transfer(List<? extends E> from) {
      from(from);
    }

    private E entry;
    private List<E> from;
    private List<E> to;
    private int index;
    
    @SuppressWarnings("unchecked")
    public Transfer<E> from(List<? extends E> from) {
      this.from = (List<E>) from;
      if( entry == null ) {
        index = from.indexOf(entry); //TODO NPE m�glich
      } else {
        index = indexOf(this.from, entry);
      }
      return this;
    }
    public Transfer<E> atIndex(int index) {
      this.index = index;
      return this;
    }
    @SuppressWarnings("unchecked")
    public Transfer<E> to(List<? extends E> to) {
      this.to = (List<E>) to;
      return this;
    }
    
    public void at(int index) {
      E element = from.remove(this.index);
      if( index < 0 ) {
        to.add(element);
      } else {
        to.add(index,element);
      }
    }
    
    protected int indexOf(E element) {
      return indexOf(to, element);
    }
    
  }

}
