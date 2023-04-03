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
package snmpTrapDemon.poolUsage;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ListCounter is a sorted list which can count elements. 
 */
public class ListCounter<T extends Comparable<? super T>> extends AbstractList<T> {

  private ArrayList<T> sortedList;

  public ListCounter() {
    this.sortedList = new ArrayList<T>();
  }
  
  public ListCounter( List<T> list ) {
    this.sortedList = new ArrayList<T>(list);
    Collections.sort( this.sortedList );
  }

  /**
   * @param list
   */
  public void addAll( List<T> list) {
    this.sortedList.addAll(list);
    Collections.sort( this.sortedList );
  }

  /* (non-Javadoc)
   * @see java.util.AbstractList#get(int)
   */
  @Override
  public T get(int index) {
    return sortedList.get(index);
  }

  /* (non-Javadoc)
   * @see java.util.AbstractCollection#size()
   */
  @Override
  public int size() {
    return sortedList.size();
  }
  
  public int countLessThan(T t ) {
    return indexOfLargestElementLessThan(t)+1;
  }
  
  public int countLessThanOrEqualTo(T t ) {
    return indexOfSmallestElementGreaterThan(t);
  }
  
  public int countGreaterThan(T t ) {
    return size() - countLessThanOrEqualTo(t);
  }
  
  public int countGreaterThanOrEqualTo(T t ) {
    return size() - countLessThan(t);
  }
  
  public int countBetween(T t1, T t2) {
    return countLessThanOrEqualTo(t2)-countLessThan(t1);
  }
  
  public int countBetweenExclusive(T t1, T t2) {
    return countLessThan(t2)-countLessThanOrEqualTo(t1);
  }
  
  public int countEquals(T t) {
    return countLessThanOrEqualTo(t)-countLessThan(t);
  }
  
  /**
   * @param t
   * @return
   */
  private int indexOfLargestElementLessThan(T t) {
    int i = Collections.binarySearch(sortedList,t);
    if( i >= 0 ) {
      while( i >= 0 && sortedList.get(i).compareTo(t) == 0 ) {
        --i;
      }
      return i;
    } else {
      return -i-2;
    }
  }
  
  /**
   * @param t
   * @return
   */
  private int indexOfSmallestElementGreaterThan(T t) {
    int i = Collections.binarySearch(sortedList,t);
    if( i >= 0 ) {
      while( i < size() && sortedList.get(i).compareTo(t) == 0 ) {
        ++i;
      }
      return i;
    } else {
      return -i-1;
    }
  }

}
