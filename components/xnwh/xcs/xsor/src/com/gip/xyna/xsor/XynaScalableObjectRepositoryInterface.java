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
package com.gip.xyna.xsor;

import java.util.List;

import com.gip.xyna.xsor.common.exceptions.ActionNotAllowedInClusterStateException;
import com.gip.xyna.xsor.common.exceptions.CollisionWithRemoteRequestException;
import com.gip.xyna.xsor.common.exceptions.RemoteProcessExecutionTimeoutException;
import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchRequest;
import com.gip.xyna.xsor.persistence.PersistenceException;
import com.gip.xyna.xsor.protocol.XSORPayload;


public interface XynaScalableObjectRepositoryInterface {

  
  public void addIndices(String tablename, List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions);
  
  
  /**
   * Sucht nach den Suchkriterien unter Berücksichtigung.
   * 
   * @param searchRequest beschreibt die unparametrisierte Suche in DisjunktiverNormaleForm.
   * @param searchParameter enthält die Parameter der Suchanfrage.
   * @param maxResults maximale Menge an gefundenen Objekten, die zurückgegeben werden. Es werden nur dann weniger als maxResults
   *                   Objekte zurückgegeben, wenn nicht mehr Objekte gefunden wurden.
   * @param lockResults falls true, werden alle zurückgegebenen Objekte gelockt
   * @param strictlyCoherent 
   * @return Liste der gefundenen Objekte.
   */
  public List<XSORPayload> search(SearchRequest searchRequest, SearchParameter searchParameter, TransactionContext transactionContext, 
                                            int maxResults, boolean lockResults, boolean strictlyCoherent) throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException, ActionNotAllowedInClusterStateException;

    
  public boolean persistPayload(XSORPayload payload, TransactionContext transactionContext, boolean strictlyCoherent) throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException, ActionNotAllowedInClusterStateException;
  
  
  public void deletePayload(XSORPayload payload, TransactionContext transactionContext, boolean strictlyCoherent) throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException, ActionNotAllowedInClusterStateException;
  
  
  public TransactionContext beginTransaction(boolean strictlyCoherent);
  
  
  public void endTransaction(TransactionContext transactionContext, boolean strictlyCoherent) throws RemoteProcessExecutionTimeoutException, CollisionWithRemoteRequestException, ActionNotAllowedInClusterStateException;
  
  /**
   * initialisierung für diesen objekt-typ und laden der vorhandenen daten aus dem backingstore
   */
  public void initializeTable(String tableName, Class<? extends XSORPayload> clazz, int maxTableSize) throws PersistenceException;
  
  public void removeTable(String tableName, Class<? extends XSORPayload> clazz) throws PersistenceException;
  
}
