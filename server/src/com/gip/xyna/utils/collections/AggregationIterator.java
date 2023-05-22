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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class AggregationIterator<E> implements Iterator<E> {
  
  final List<Iterator<E>> iterators;
  private int iteratorIndex;
  
  public AggregationIterator(final Iterator<E>... iteratorsToAggregate) {
    if (iteratorsToAggregate == null ||
        iteratorsToAggregate.length <= 0) {
      iterators = Collections.emptyList();
    } else if (iteratorsToAggregate.length > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Too many iterators.");
    } else {
      iterators = Arrays.asList(iteratorsToAggregate);
    }
    iteratorIndex = 0;
  }
  
  
  public AggregationIterator(final List<Iterator<E>> iteratorsToAggregate) {
     if (iteratorsToAggregate == null) {
      iterators = Collections.emptyList();
    } else {
      iterators = iteratorsToAggregate;
    }
    iteratorIndex = 0;
  }
  

  public boolean hasNext() {
    if (iteratorIndex < iterators.size()) {
      Iterator<E> iter = iterators.get(iteratorIndex);
      if (iter.hasNext()) {
        return true;
      } else {
        iteratorIndex++;
        return hasNext();
      }
    } else {
      return false;
    }
  }


  public E next() {
    if (hasNext()) {
      Iterator<E> iter = iterators.get(iteratorIndex);
      return iter.next();
    } else {
      return null;
    }
  }


  public void remove() {
    Iterator<E> iter = iterators.get(iteratorIndex);
    iter.remove();
  }

}
