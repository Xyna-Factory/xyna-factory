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

import java.util.List;

import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.WarehouseRetryExecutorBuilder;

import xmcp.oas.fman.storables.OAS_ImportHistory;
import xmcp.tables.datatypes.TableInfo;


public class OasImportHistoryStorage {

  private static PreparedQueryCache queryCache = new PreparedQueryCache();


  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(OasImportHistoryStorable.class);
  }
  
  public static void shutdown() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.unregisterStorable(OasImportHistoryStorable.class);
  }
  

  private WarehouseRetryExecutorBuilder buildExecutor() {
    return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY).storable(OasImportHistoryStorable.class);
  }

  
  public static PreparedQueryCache getQueryCache() {
    return queryCache;
  }

  
  public List<OAS_ImportHistory> searchOasImportHistory(TableInfo info) throws PersistenceLayerException {
    List<OAS_ImportHistory> ret = buildExecutor().execute(new SearchOasImportHistory(info));
    return ret;
  }
  
  
  public OAS_ImportHistory searchOasImportHistoryDetails(OAS_ImportHistory input) throws PersistenceLayerException {
    OAS_ImportHistory ret = buildExecutor().execute(new SearchOasImportHistoryDetails(input));
    return ret;
  }
  
  
  public void storeOasImportHistory(OAS_ImportHistory input) throws PersistenceLayerException {
    buildExecutor().execute(new StoreOasImportHistory(input));
  }
  
}
