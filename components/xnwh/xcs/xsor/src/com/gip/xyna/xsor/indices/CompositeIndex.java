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
package com.gip.xyna.xsor.indices;

import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.management.IndexSearchResult;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.protocol.XSORPayload;


public interface CompositeIndex extends Index<XSORPayload> {

  /**
   * Evaluates the SearchCriterion and returns all found internalIds that matched the request as far as it was covered from the index
   * @param searchCriterion
   * @param parameter
   * @return all internalIds that matched the searchRequest, empty array if no match could be found
   */
  public IndexSearchResult search(SearchCriterion searchCriterion, SearchParameter parameter, int maxResults);
  
  /**
   * Updates the outdatedEntry with the outdatedId to the updatedEntry with the updatedId.
   * If outdatedEntry is null a creation is performed and if updatedEntry is null a Deletion is
   * @param outdatedEntry
   * @param updatedEntry
   * @param outdatedId
   * @param updatedId
   */
  public void update(XSORPayload outdatedEntry, XSORPayload updatedEntry, int outdatedId, int updatedId);
 
  /**
   * Returns the IndexDefinition used to create the index
   * @return the indexdefinition
   */
  public IndexDefinition getIndexDefintion();
  
  
}
