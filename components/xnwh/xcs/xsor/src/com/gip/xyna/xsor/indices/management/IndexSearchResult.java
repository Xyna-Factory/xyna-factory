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
package com.gip.xyna.xsor.indices.management;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gip.xyna.xsor.indices.XSORPayloadPrimaryKeyIndex;
import com.gip.xyna.xsor.indices.tools.IntValueWrapper;


public class IndexSearchResult {
  
  public static IndexSearchResult EMPTY_INDEX_SEARCH_RESULT = new IndexSearchResult(new int[0], true, true);
  
  private final int[] internalIds;
  private final boolean sorted;
  private final boolean exhaustiveSearch;
  
  public IndexSearchResult(int[] internalIds, boolean sorted, boolean exhaustiveSearch) {
    this.internalIds = internalIds;
    this.sorted = sorted;
    this.exhaustiveSearch = exhaustiveSearch;
  }
  
  
  public static IndexSearchResult createIndexSearchResultFromIntValueWrapper(IntValueWrapper wrapper) {
    return new IndexSearchResult(wrapper.getValues(), true, true);
  }
  
  
  public static IndexSearchResult createIndexSearchResultFromInteger(Integer integer) {
    return new IndexSearchResult(new int[] {integer.intValue()}, true, true);
  }
  
  
  public static IndexSearchResult createIndexSearchResultAsFullTableScan(XSORPayloadPrimaryKeyIndex index) {
    return new IndexSearchResult(index.values(), true, true);
  }
  
  
  public int[] getInternalIds() {
    return internalIds;
  }
  
  public boolean isSorted() {
    return sorted;
  }
  
  public boolean isExhaustiveSearch() {
    return exhaustiveSearch;
  }
  
  
  // TODO improve algorithm, fast & sorted would be nice (low priority as or'ed requests are not that frequent)
  IndexSearchResult union(IndexSearchResult other) {
    if (other == null) {
      return this;
    }
    int[] a = this.internalIds;
    int[] b = other.internalIds;

   List<Integer> aValuesToAdd = new ArrayList<Integer>();
   if (!other.sorted) {
     b = Arrays.copyOf(b, b.length);
     Arrays.sort(b);
   }
   for (int i : a) {
     int bIndexOfAEntry = Arrays.binarySearch(b, i);
     if (bIndexOfAEntry < 0) {
       aValuesToAdd.add(i);
     }
   }
   int[] result = new int[b.length + aValuesToAdd.size()];
   System.arraycopy(b, 0, result, 0, b.length);
   for (int i=0; i<result.length- b.length; i++) {
     result[b.length+i] = aValuesToAdd.get(i).intValue();
   }
   boolean exhaustiveSearch = this.exhaustiveSearch && other.exhaustiveSearch;
   return new IndexSearchResult(result, false, exhaustiveSearch);
 }
  
  // TODO implement
  IndexSearchResult intersect(IndexSearchResult other) {
    if (other == null) {
      return this;
    }
    throw new UnsupportedOperationException("Needs to be inplemented before using an Determinator that actuallay returns a collection instead of a singletonList");
  }

  

}
