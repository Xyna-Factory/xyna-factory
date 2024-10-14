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
package com.gip.xyna.xsor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.xsor.common.InternalIdAndPayloadPair;
import com.gip.xyna.xsor.common.XSORUtil;
import com.gip.xyna.xsor.indices.management.IndexManagement;
import com.gip.xyna.xsor.indices.management.IndexSearchResult;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchRequest;
import com.gip.xyna.xsor.protocol.XSORPayload;



public class SearchingIndexSearchResultIterator {
  
    private final IndexManagement indexManagement; // TODO this reference might be gotten static from another source
    private final SearchRequest searchRequest;
    private final SearchParameter searchParameter;
    private final int maxResults;
    
    private final IndesxSearchResultPreprocessing searchResultPreprocessor;
    
    int[] backingSearchResult;
    boolean[] invalidResult;
    boolean exhaustedIndices;
    int currentResultIndex;
    int unremovedResults;
    boolean lockResults;
    List<XSORPayload> fittingResults;
    
        
    public SearchingIndexSearchResultIterator(IndexManagement indexManagement, SearchRequest searchRequest, SearchParameter searchParameter, int maxResults, boolean lockResults) {
      this.indexManagement = indexManagement;
      this.searchRequest = searchRequest;
      this.searchParameter = searchParameter;
      this.maxResults = maxResults;
      
      searchResultPreprocessor = IndesxSearchResultPreprocessing.getAppropriateSearchResultPreprocessing(maxResults, lockResults);
      
      // do an initial search
      refreshBackingSearchResult(true);
      unremovedResults = backingSearchResult.length;
      this.lockResults = lockResults;
      fittingResults = new ArrayList<XSORPayload>();
    }
    
    public int next() { // TODO research early if leftover inidices < maxResults - fittingResults ?
      if (currentResultIndex + 1 >= backingSearchResult.length) {
        assert !exhaustedIndices : "There are no new ids to be gotten, hasNext should have told you";
        refreshBackingSearchResult(false);
      }
      currentResultIndex++;
      return backingSearchResult[currentResultIndex];
    }
    
    
    public void refresh() {
      refreshBackingSearchResult(false);
    }
    
    
    public boolean hasNext() {
      // false or true
      if (!exhaustedIndices || currentResultIndex + 1 < backingSearchResult.length) {
        return true;
      } else {
        return false;
      }
    }
    
    public void remove() {
      invalidResult[currentResultIndex] = true;
      unremovedResults--;
    }
    
    
    public void close() {
      searchRequest.anneal();
    }
    
    public void addFittingPayload(XSORPayload payload) {
      fittingResults.add(payload);
    }
    
    public List<XSORPayload> getFittingResults() {
      return fittingResults;
    }
    
    public int fittingResultSize() {
      return fittingResults.size();
    }
    
    public List<Integer> getFittingIds() {
      List<Integer> fittingIds = new ArrayList<Integer>();
      for (int i=0; i < backingSearchResult.length; i++) {
        if (!invalidResult[i]) {
          fittingIds.add(backingSearchResult[i]);
        }
      }
      return fittingIds;
    }
    
    
    public List<InternalIdAndPayloadPair> getFittingPairs() {
      List<InternalIdAndPayloadPair>  fittingPairs = new ArrayList<InternalIdAndPayloadPair>();
      Iterator<XSORPayload> payloadIterator = fittingResults.iterator();
      for (int i=0; i < currentResultIndex+1; i++) {
        if (!invalidResult[i]) {
          fittingPairs.add(new InternalIdAndPayloadPair(backingSearchResult[i], payloadIterator.next()));
        }
      }
      return fittingPairs;
    }
        
    
    private void refreshBackingSearchResult(boolean initial) {
      int adjustedMaxResults;
      if (initial) {
        adjustedMaxResults = searchRequest.initialGetAdjustedMaxResults(maxResults);
      } else {
        adjustedMaxResults = searchRequest.expandSearchRange(maxResults);
      }
      IndexSearchResult result = indexManagement.search(searchRequest, searchParameter, adjustedMaxResults);
      exhaustedIndices = result.isExhaustiveSearch();
      backingSearchResult = searchResultPreprocessor.preprocessIndexSearchResult(result);
      invalidResult = new boolean[backingSearchResult.length];
      currentResultIndex = -1;
      if (!initial) {
        fittingResults.clear();
      }
    }
        

  public enum IndesxSearchResultPreprocessing {
    
    SORT {
      @Override
      int[] preprocessIndexSearchResult(IndexSearchResult searchResult) {
        if (searchResult.isSorted()) {
          int[] temp = Arrays.copyOf(searchResult.getInternalIds(), searchResult.getInternalIds().length);
          Arrays.sort(temp);
          return temp;
        } else {
          return searchResult.getInternalIds();
        }
      }
    },
    SHUFFLE {
      @Override
      int[] preprocessIndexSearchResult(IndexSearchResult searchResult) {
        return XSORUtil.shuffleIntArray(searchResult.getInternalIds());
      }
    },
    UNTOUCHED {
      @Override
      int[] preprocessIndexSearchResult(IndexSearchResult searchResult) {
        return searchResult.getInternalIds();
      }
    };
    
    abstract int[] preprocessIndexSearchResult(IndexSearchResult searchResult);
    
    public static IndesxSearchResultPreprocessing getAppropriateSearchResultPreprocessing(int maxResults, boolean lockResults) {
      if (maxResults < 0) {
        if (lockResults) {
          return SORT;
        } else {
          return UNTOUCHED;
        }
      } else if (maxResults == 1) {
        if (lockResults) {
          return SHUFFLE;
        } else {
          return SHUFFLE;
        }
      } else {
        if (lockResults) {
          return SORT;
        } else {
          return SHUFFLE;
        }
      }
    }
  }
    

}
