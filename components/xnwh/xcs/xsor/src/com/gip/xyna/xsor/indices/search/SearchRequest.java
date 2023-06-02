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
package com.gip.xyna.xsor.indices.search;

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.management.SearchCriterionIndexDefinitionDeterminator;
import com.gip.xyna.xsor.protocol.XSORPayload;


public class SearchRequest {
  
  private final String tablename;
  private final List<? extends SearchCriterion> searchCriterion;
  
  private final static double DEFAULT_INITIAL_SEARCH_INCREASE = 1.5f;
  private final static double DEFAULT_MINIMUM_SEARCH_INCREASE = 1.1f;
  
  private final static int SUN = 201; // maxrows -1 are too hot to look at
  private final static int HOT = 100;
  private final static int COLD = 0;
  
  private final static int MAX_EXPANSION_POW_TEMPERATURE = (int) Math.pow(HOT, 2);
  private final static int MAX_RETRACTION_POW_TEMPERATURE = (int) Math.pow(HOT, 2) * 5;
  
  // totally not thread safe...and we know it...na na nana nana
  volatile int temperature;
  volatile double searchRangeIncrease;
  volatile int warmthThroughExpansion;
  
  
  public SearchRequest(String tablename, List<? extends SearchCriterion> searchCriterion) {
    this.tablename = tablename;
    this.searchCriterion = searchCriterion;
    this.temperature = HOT;
    this.searchRangeIncrease = DEFAULT_INITIAL_SEARCH_INCREASE;
  }
  
  
  public String getTablename() {
    return tablename;
  }
  
  public List<? extends SearchCriterion> getSearchCriterion() {
    return searchCriterion;
  }
   
  
  public boolean fits(XSORPayload candidate, SearchParameter parameter) {
    if (candidate == null) {
      return false;
    }
    if (searchCriterion == null || searchCriterion.size() == 0) {
      return true;
    } else {
      for (SearchCriterion subCriterion : searchCriterion) {
        if (subCriterion.fits(candidate, parameter)) {
          return true;
        }
      }
      return false;
    }
  }
  
  
  public int initialGetAdjustedMaxResults(int maxResults) {
    if (maxResults < 0) {
      temperature = SUN;
      return maxResults;
    } else {
      double initialRangeIncrease = searchRangeIncrease;
      return adjustMaxResults(maxResults, initialRangeIncrease);
    }
  }
  
  
  public void anneal() {
    if (temperature < SUN) {
      if (temperature > COLD) {
        if (warmthThroughExpansion > 0) {
          temperature--;
        } else {
          shrinkSearchExpansion();
        }
      } else {
        if (warmthThroughExpansion > HOT) {
          temperature = HOT;
        } else {
          temperature = warmthThroughExpansion;
        }
      }
      warmthThroughExpansion = 0;
    }
  }
  
  
  // not enough values could be found in the adjustedSearchRange
  public int expandSearchRange(int maxResults) {
    assert maxResults > -1 : "An Expansion should not be called with maxResults -1 as there is nothing to be gotten";
    double adjustedSearchIncrease = calculateAndSetExpandedSearchRange();
    return adjustMaxResults(maxResults, adjustedSearchIncrease);
  }
    
  
  private int adjustMaxResults(int maxResults, double increaseFactor) {
    long increasedResults = Math.round(maxResults * increaseFactor);
    if (increasedResults > Integer.MAX_VALUE) {
      return -1; // or return Integer.MAX_VALUE ?
    } else {
      return (int) increasedResults;
    }
  }
  
  
  private double calculateAndSetExpandedSearchRange() {
    double current = searchRangeIncrease;
    double temperatureFactor = (Math.pow(temperature, 2.0d)/MAX_EXPANSION_POW_TEMPERATURE) + 0.1d;
    double expansion = (current * temperatureFactor) + current;
    searchRangeIncrease = expansion;
    warmthThroughExpansion++;
    return expansion;
  }
  
  
  private void shrinkSearchExpansion() {
    double current = searchRangeIncrease;
    double temperatureFactor = (Math.pow(temperature, 2.0d)/MAX_RETRACTION_POW_TEMPERATURE) + 0.1d;
    double retraction = current - (current * temperatureFactor);
    if (retraction < DEFAULT_MINIMUM_SEARCH_INCREASE) {
      retraction = DEFAULT_MINIMUM_SEARCH_INCREASE;
    }
    searchRangeIncrease = retraction;
    temperature--;
  }
  
  
  public void establishAppropriateIndexDefinitions(List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions, SearchCriterionIndexDefinitionDeterminator determinator) {
    for (SearchCriterion criterion : searchCriterion) {
      criterion.establishAppropriateIndexDefinitions(indexDefinitions, determinator);
    }
  }
  
    
}
