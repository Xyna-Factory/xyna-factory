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
package com.gip.xyna.xsor.indices.searchvaluekeys;

import com.gip.xyna.xsor.indices.UniqueIndexKey;
import com.gip.xyna.xsor.indices.search.SearchValue;


public class SearchValueBasedUniqueIndexKey extends UniqueIndexKey {

  private SearchValue[] values;
  
  public SearchValueBasedUniqueIndexKey(SearchValue[] indexedColumnValues)  {
    values = indexedColumnValues;
  }
  
  
  @Override
  public boolean keyEquals(UniqueIndexKey otherKey) {
    for (int i=0; i<values.length; i++) {
      if (!values[i].equals(((SearchValueBasedUniqueIndexKey) otherKey).getSearchValue(i))) {
        return false;
      }
    }
    return true;
  }
  

  @Override
  public int keyHash() {
    int hash = 1;
    for (SearchValue value : values) {
      //hash-construct wie in arrays.hashcode
      hash = 31 * hash + value.hashCode();
    }
    return hash;
  }
  
  
  public SearchValue getSearchValue(int index) {
    return values[index];
  }

}
