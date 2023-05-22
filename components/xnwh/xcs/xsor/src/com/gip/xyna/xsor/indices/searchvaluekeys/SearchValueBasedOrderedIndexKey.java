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
package com.gip.xyna.xsor.indices.searchvaluekeys;

import com.gip.xyna.xsor.indices.OrderedIndexKey;
import com.gip.xyna.xsor.indices.search.SearchValue;


public class SearchValueBasedOrderedIndexKey extends OrderedIndexKey {

  public enum KeyType {
    START, STOP, INDEX;
  }

  private SearchValue[] values;
  private final KeyType keyType;
  private final boolean inclusive;
  private final boolean partiallyOpen;
  
  public SearchValueBasedOrderedIndexKey(SearchValue[] values) {
    this(values, KeyType.INDEX, true, false);
  }
  
  public SearchValueBasedOrderedIndexKey(SearchValue[] values, KeyType keyType, boolean inclusive, boolean partiallyOpen) {
    this.values = values;
    this.keyType = keyType;
    this.inclusive = inclusive;
    this.partiallyOpen = partiallyOpen;
  }


  public int compareTo(OrderedIndexKey o) {
    SearchValueBasedOrderedIndexKey otherSearchValueBasedKey = (SearchValueBasedOrderedIndexKey)o;
    if (keyType == KeyType.INDEX &&
        otherSearchValueBasedKey.keyType != KeyType.INDEX) {
      return -1 * otherSearchValueBasedKey.compareTo(this);
    } else {
      int iteratableColumns = Math.min(values.length, otherSearchValueBasedKey.values.length);
      for (int i = 0; i < iteratableColumns; i++) {
        int comparison = values[i].getComparisionAlgorithm().compare(values[i].getValue(), otherSearchValueBasedKey.values[i].getValue());
        if (comparison != 0) {
          return comparison;
        }
      }
      // TODO nicer?
      if (values.length != otherSearchValueBasedKey.values.length) {
        if (keyType == KeyType.INDEX) {
          if (otherSearchValueBasedKey.keyType == KeyType.START) {
            if (otherSearchValueBasedKey.inclusive) {
              return 1;
            } else {
              return -1;
            }
          } else {
            if (otherSearchValueBasedKey.inclusive) {
              return -1;
            } else {
              return 1;
            }
          }
        } else if (keyType == KeyType.START) {
          if (values.length < otherSearchValueBasedKey.values.length) {
            if (inclusive) {
              return -1;
            } else {
             return 1; 
            }
          } else {
            return -1;
          }
        } else {
          if (values.length < otherSearchValueBasedKey.values.length) {
            if (inclusive) {
              return 1;
            } else {
              return -1;
            }
          } else {
            return 1;
          }
        }
      }
      return 0;
    }
  }

  @Override
  public boolean isInclusive() {
    return inclusive;
  }

  @Override
  public boolean isPartiallyOpen() {
    return partiallyOpen;
  }


}
