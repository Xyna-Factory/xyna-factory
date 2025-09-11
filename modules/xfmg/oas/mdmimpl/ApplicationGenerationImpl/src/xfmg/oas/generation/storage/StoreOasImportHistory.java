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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;

import xmcp.oas.fman.storables.OAS_ImportHistory;


public class StoreOasImportHistory implements WarehouseRetryExecutableNoException<OAS_ImportHistory> {

  private static final OasImportHistoryAdapter _adapter = new OasImportHistoryAdapter();
  private OAS_ImportHistory _input;
  
  
  public StoreOasImportHistory(OAS_ImportHistory input) {
    super();
    this._input = input;
  }


  @Override
  public OAS_ImportHistory executeAndCommit(ODSConnection con) throws PersistenceLayerException {
    if (_input.getUniqueIdentifier() <= 0) {
      _input.setUniqueIdentifier(XynaFactory.getInstance().getIDGenerator().getUniqueId());
    }
    OasImportHistoryStorable storable = _adapter.adapt(_input);
    con.persistObject(storable);
    return _input;
  }

}
