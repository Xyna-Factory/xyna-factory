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
package com.gip.xyna.xsor.indices.management;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchCriterion;


public class SearchCriterionSingleIndexDefinitionDeterminator implements SearchCriterionIndexDefinitionDeterminator {

  public List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> determineAppropriateIndexDefinitions(
         List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions, SearchCriterion searchCriterion) {
    if (searchCriterion.getColumns().size() == 0) {
      return null;
    }
    List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> result = 
      new ArrayList<IndexDefinition<?,? extends IndexKey,? extends IndexSearchCriterion>>();
    IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion> bestIndex = null;
    float bestIndexCoverage = 0.0f;
    for (IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion> index : indexDefinitions) {
      float coverage = index.coverage(searchCriterion);
      if (coverage >= 1.0f) {
        result.add(index);
        return result;
      } else if (coverage > bestIndexCoverage) {
        bestIndex = index;
        bestIndexCoverage = coverage;
      }
    }
    result.add(bestIndex);
    return result;
  }

}
