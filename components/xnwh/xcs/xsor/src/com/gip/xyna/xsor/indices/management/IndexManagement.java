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

import com.gip.xyna.xsor.indices.*;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchRequest;
import com.gip.xyna.xsor.protocol.XSORPayload;

public interface IndexManagement {
  
  public void createIndex(IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion> indexDefinition);
  
  public void createXSORPayloadPrimaryKeyIndex(String tableName);
  
  public XSORPayloadPrimaryKeyIndex getXSORPayloadPrimaryKeyIndex(String tableName);
  
  public IndexSearchResult search(SearchRequest searchRequest, SearchParameter searchParameter, int maxResults);
  
  public boolean put(XSORPayload obj, int internalId);
  
  public void delete(XSORPayload obj, int internalId);
  
  public void update(XSORPayload outdatedEntry, XSORPayload updatedEntry, int outdatedId, int updatedId);

  /**
   * alle objekte aus allen indizes entfernen (inkl pkindex)
   */
  public void clear(String tableName);

  public void remove(String tableName);

}
