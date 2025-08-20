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
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;

import xmcp.oas.fman.storables.OAS_ImportHistory;


public class StoreOasImportHistory implements WarehouseRetryExecutableNoResult {

  private static final OasImportHistoryAdapter _adapter = new OasImportHistoryAdapter();
  private OAS_ImportHistory input;
  
  
  public StoreOasImportHistory(OAS_ImportHistory input) {
    super();
    this.input = input;
  }


  @Override
  public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
    OasImportHistoryStorable storable = _adapter.adapt(input);
    con.persistObject(storable);
  }
  
}
