/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xfmg.oas.generation.storage;

import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;

import xmcp.oas.fman.storables.OAS_ImportHistory;


public class SearchOasImportHistoryDetails implements WarehouseRetryExecutableNoException<OAS_ImportHistory> {

  private static final String QUERY = "SELECT * FROM " + OasImportHistoryStorable.TABLE_NAME + " WHERE " +
                                      OasImportHistoryStorable.COL_UNIQUE_ID + " = ?"  ;
  private static final OasImportHistoryAdapter _adapter = new OasImportHistoryAdapter();

  private final long uniqueId;
  
  
  public SearchOasImportHistoryDetails(OAS_ImportHistory row) {
    super();
    this.uniqueId = row.getUniqueIdentifier();
  }


  @Override
  public OAS_ImportHistory executeAndCommit(ODSConnection con) throws PersistenceLayerException {
    /*
    PreparedQuery<OasImportHistoryStorable> query = OasImportHistoryStorage.getQueryCache().getQueryFromCache(QUERY, con,
                                                      OasImportHistoryStorable.getOasImportHistoryDetailsReader());
                                                      */
    OasImportHistoryStorable storable = new OasImportHistoryStorable();
    storable.setUniqueIdentifier(uniqueId);
    try {
      con.queryOneRow(storable);
    } catch (Exception e) {
      throw new RuntimeException("Error querying OasImportHistory entry with id " + uniqueId + ": " + e.getMessage(), e);
    }
    return _adapter.adapt(storable);
  }

}
